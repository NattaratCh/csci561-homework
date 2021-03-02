import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by nuning on 2/25/21.
 */

public class Homework {
    public static void main(String[] args) {
        CheckerGame checkerGame = new CheckerGame();
        checkerGame.start();
    }

}

class CheckerGame {
    private Move bestMove;
    private Integer depthLimit;
    private int[][] kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};;
    private int[][] kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};

    public CheckerGame() {
        this.depthLimit = 1;
    }

    public void start() {
        GameState gameState = readInput("./test-cases/input3.txt");
        gameState.printState();
        nextMove(gameState);
    }

    public GameState readInput(String filename) {
        File file = new File(filename);
        BufferedReader reader;
        int boardSize = 8;
        try {
            reader = new BufferedReader(new FileReader(file));

            // Line 1 - Game mode (SINGLE, GAME)
            String line = reader.readLine().trim();
            Mode mode = Mode.valueOf(line);

            // Line 2 - Color (WHITE, BLACK)
            line = reader.readLine().trim();
            Player player = Player.valueOf(line);

            // Line 3 - time remaining
            line = reader.readLine().trim();
            float timeRemaining = Float.valueOf(line);

            // Line 4 - board state
            String[][] board = new String[boardSize][boardSize];
            for(int r=0; r<boardSize; r++) {
                line = reader.readLine().trim();
                board[r] = line.split("");
            }

            GameState gameState = new GameState(board, player, mode, timeRemaining, true);
            System.out.println(gameState.getBoard().length + " " + gameState.getBoard()[0].length );
            return gameState;
        } catch (Exception e) {
            System.out.println("readInput: " + e.getMessage());
            return null;
        }
    }

    public void nextMove(GameState gameState) {
        minimax(gameState);
    }

    public void minimax(GameState gameState) {
        if (gameState.isInitialState()) {
            depthLimit = 1;
        } else {
            depthLimit = 5;
        }
        Integer value = maxValue(gameState, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        System.out.println("value: " + value);
        bestMove.print();
    }

    // Player
    public Integer maxValue(GameState state, Integer alpha, Integer beta, Integer depth) {
        System.out.println("maxValue depth " + depth);
        if (state.isTerminal()) {
            System.out.println("terminal");
            return state.utility();
        }

        if (depth == depthLimit) {
            System.out.println("reach depth limit");
            return state.evaluation();
        }

        Integer value = Integer.MIN_VALUE;
        Integer next = null;
        List<Move> possibleMoves = getAllPossibleMoves(state);
        GameState cloneState;
        for (Move move: possibleMoves) {
            try {
                cloneState = (GameState) state.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                continue;
            }
            applyMove(cloneState, move); // TODO handle null
            cloneState.printBoard();
            if (cloneState.canOpponentContinue()) {
                cloneState.setIsPlayerTurn(false);
                next = minValue(cloneState, alpha, beta, depth+1);
            } else {
                cloneState.setIsPlayerTurn(true);
                next = maxValue(cloneState, alpha, beta, depth+1);
            }

            System.out.println("maxValue value: " + next);
            System.out.println("--");

            if (next > value) {
                value = next;
                if (depth == 0) bestMove = move;
                System.out.println("## depth: " + depth);
                System.out.println("## best move: " + move.getFrom() + " => " + move.getTo());
            }
            cloneState.setIsPlayerTurn(true);

            if (value >= beta) return value;
            alpha = Math.max(alpha, value);
        }
        return value;
    }

    // Opponent
    public Integer minValue(GameState state, Integer alpha, Integer beta, Integer depth) {
        System.out.println("minValue depth " + depth);
        if (state.isTerminal()) {
            return state.utility();
        }

        if (depth == depthLimit) {
            return state.evaluation();
        }

        Integer value = Integer.MAX_VALUE;
        GameState cloneState;
        List<Move> possibleMoves = getAllPossibleMoves(state);
        for (Move move: possibleMoves) {
            try {
                cloneState = (GameState) state.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                continue;
            }
            applyMove(cloneState, move);
            cloneState.printBoard();
            if (cloneState.canPlayerContinue()) {
                cloneState.setIsPlayerTurn(true);
                value = Math.min(value, maxValue(cloneState, alpha, beta, depth+1));
            } else {
                cloneState.setIsPlayerTurn(false);
                value = Math.min(value, minValue(cloneState, alpha, beta, depth+1));
            }
            System.out.println("minValue value: " + value);
            cloneState.setIsPlayerTurn(false);

            if (value <= alpha) return alpha;
            beta = Math.min(beta, value);
        }
        return value;
    }

    public List<Move> getPossibleJump(GameState gameState, int currentRow, int currentCol, int[][] captureDirs) {
        Queue<Move> queue = new LinkedList<>();
        List<Move> moves = new ArrayList<>();
        String[][] board = gameState.getBoard();
        // First jump
        Move current = new Move(MoveType.JUMP, null, Utility.getLabel(currentRow, currentCol), null, board[currentRow][currentCol], board[currentRow][currentCol]);
        loopJump(gameState, current, captureDirs, queue, moves, true);

        while(!queue.isEmpty()) {
            Move move = queue.poll();
            String fromLabel = move.getTo();
            int[] fromPosition = Utility.getPosition(fromLabel);
            board = gameState.getBoard();
            if (isKingChecker(board[fromPosition[1]][fromPosition[0]])) {
                loopJump(gameState, move, kingCaptureDirs, queue, moves, false);
            } else {
                loopJump(gameState, move, captureDirs, queue, moves, false);
            }
        }
        return moves;
    }

    public void loopJump(GameState gameState, Move move, int[][] captureDirs, Queue<Move> queue, List<Move> moves, boolean isFirstJump) {
        if (move != null && move.isBecomeKing()) {
            moves.add(move);
            return;
        }
        boolean isValid = false;
        String[][] board;
        for(int[] dir: captureDirs) {
            String fromLabel = move.getTo();
            int[] position = Utility.getPosition(fromLabel);
            int row = position[1] + dir[1];
            int col = position[0] + dir[0];
            if (isValidMove(gameState, position[1], position[0], row, col)) {
                isValid = true;
                Move nextMove = new Move(MoveType.JUMP, move.getTo(), Utility.getLabel(row, col), move, move.getToChecker(), null);
                applyMove(gameState, nextMove);
                board = gameState.getBoard();
                String toChecker =  board[row][col];
                nextMove.setToChecker(toChecker);
                queue.add(nextMove);
            }
        }

        if (!isValid && !isFirstJump) {
            moves.add(move);
        }
    }

    public void applyNewMove(GameState gameState, Move move) {
        int[] oldPosition = Utility.getPosition(move.getFrom());
        int[] newPosition = Utility.getPosition(move.getTo());
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];
        String[][] board = gameState.getBoard();

        Player currentPlayer = gameState.isPlayerTurn() ? gameState.getPlayer() : gameState.getOpponent();
        boolean isMoveToKingArea = Utility.isKingArea(currentPlayer, newRow, newCol);

        if (isMoveToKingArea) {
            System.out.println("applyNewMove move to king " + move.getFrom() + " " + move.getTo() );
            board[newRow][newCol] = board[oldRow][oldCol].toUpperCase();
            if (currentPlayer.equals(gameState.getPlayer())) {
                gameState.getPlayerKingPosition().add(move.getTo());
                gameState.getPlayerManPosition().remove(move.getFrom());
            } else {
                gameState.getOpponentKingPosition().add(move.getTo());
                gameState.getOpponentManPosition().remove(move.getFrom());
            }
        } else {
            board[newRow][newCol] = board[oldRow][oldCol];
            if (currentPlayer.equals(gameState.getPlayer())) {
                gameState.getPlayerManPosition().add(move.getTo());
                gameState.getPlayerManPosition().remove(move.getFrom());
            } else {
                gameState.getOpponentManPosition().add(move.getTo());
                gameState.getOpponentManPosition().remove(move.getFrom());
            }
        }
        board[oldRow][oldCol] = ".";
    }

    public MoveLog applySingleMove(GameState gameState, Move move) {
        int[] oldPosition = Utility.getPosition(move.getFrom());
        int[] newPosition = Utility.getPosition(move.getTo());
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];
        String[][] board = gameState.getBoard();

        String fromChecker = board[oldRow][oldCol];

        applyNewMove(gameState, move);

        return new MoveLog(MoveType.JUMP.ONE, fromChecker, board[newRow][newCol], move.getFrom(), move.getTo(), null, null, null);
    }

    public void applyJumpMoves(GameState gameState, Move move) {

        Player currentPlayer = gameState.isPlayerTurn() ? gameState.getPlayer() : gameState.getOpponent();

        int[] oldPosition = Utility.getPosition(move.getFrom());
        int[] newPosition = Utility.getPosition(move.getTo());
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];
        String[][] board = gameState.getBoard();
        System.out.println("applyJumpMoves from " + oldCol + " " + oldRow + " to " + newCol + " " + newRow);
        System.out.println("applyJumpMoves from " + move.getFrom() + " to " + move.getTo());

        String fromChecker = board[oldRow][oldCol];

        applyNewMove(gameState, move);

        // Handle capture
        int[] capturedPosition = new int[] { (oldCol+newCol) / 2 , (oldRow+newRow) / 2}; // (x,y)
        String capturedLabel = Utility.getLabel(capturedPosition[1], capturedPosition[0]);
        String capturedChecker = board[capturedPosition[1]][capturedPosition[0]];
        if (gameState.getOpponent().equals(currentPlayer)) {
            gameState.getPlayerManPosition().remove(capturedLabel);
            gameState.getPlayerKingPosition().remove(capturedLabel);
        } else {
            gameState.getOpponentManPosition().remove(capturedLabel);
            gameState.getOpponentKingPosition().remove(capturedLabel);
        }
        board[capturedPosition[1]][capturedPosition[0]] = ".";
    }

    public void applyMove(GameState gameState, Move move) {
        System.out.println("applyMove " + move.getMoveType() + " from " + move.getFrom() + " to " + move.getTo());
        if (MoveType.ONE.equals(move.getMoveType())) {
            applySingleMove(gameState, move);
        } else if (MoveType.JUMP.equals(move.getMoveType())) {
            applyJumpMoves(gameState, move);
        }
    }

    public List<Move> getAllPossibleMoves(GameState gameState) {
        int[][] regularDirs;
        int[][] captureDirs;

        Set<String> manCheckerPosition = gameState.isPlayerTurn() ? new HashSet<>(gameState.getPlayerManPosition()) : new HashSet<>(gameState.getOpponentManPosition());
        Set<String> kingCheckerPosition = gameState.isPlayerTurn() ? new HashSet<>(gameState.getPlayerKingPosition()) : new HashSet<>(gameState.getOpponentKingPosition());

        Player currentPlayer = gameState.isPlayerTurn() ? gameState.getPlayer() : gameState.getOpponent();

        if (Player.WHITE.equals(currentPlayer)) {
            regularDirs = new int[][]{{1, -1}, {-1, -1}};
            captureDirs = new int[][]{{2, -2}, {-2, -2}};
        } else {
            regularDirs = new int[][]{{1, 1}, {-1, 1}};
            captureDirs = new int[][]{{2, 2}, {-2, 2}};
        }

        List<Move> regularMoves = new ArrayList<>();
        List<Move> captureMoves = new ArrayList<>();
        GameState cloneState;

        // Get all possible moves of man checkers
        for(String checker: manCheckerPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[1];
            int currentCol = currentPostion[0];
            try {
                cloneState = (GameState) gameState.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                continue;
            }

            System.out.println("checker " + checker + " " + cloneState.getBoard()[currentRow][currentCol]);

            List<Move> jumps = getPossibleJump(cloneState, currentRow, currentCol, captureDirs);
            System.out.println("getAllPossibleMoves | jump size " + jumps.size());

            if (jumps.isEmpty()) {
                String[][] board = gameState.getBoard();
                for (int[] dir : regularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    if (isValidMove(cloneState, currentRow, currentCol, row, col)) {
                        System.out.println("Add possible move " + checker + " -> " + Utility.getLabel(row, col));
                        regularMoves.add(new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null, board[currentRow][currentCol], board[row][col]));
                    }
                }
            } else {
                captureMoves = jumps;
            }
        }

        // Get all possible moves of king checkers
        for(String checker: kingCheckerPosition) {
            System.out.println("king " + checker);
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[1];
            int currentCol = currentPostion[0];
            try {
                cloneState = (GameState) gameState.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                continue;
            }

            List<Move> jumps = getPossibleJump(cloneState, currentRow, currentCol, kingCaptureDirs);
            System.out.println("getAllPossibleMoves | jump size " + jumps.size());

            if (jumps.isEmpty()) {
                String[][] board = gameState.getBoard();
                for(int[] dir: kingRegularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    if (isValidMove(cloneState, currentRow, currentCol, row, col)) {
                        regularMoves.add(new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null, board[currentRow][currentCol], board[row][col]));
                    }
                }
            } else {
                captureMoves = jumps;
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        } else {
            return regularMoves;
        }

    }

    public boolean isValidMove(GameState gameState, int oldRow, int oldCol, int row, int col) {
        String[][] board = gameState.getBoard();
        gameState.printBoard();
        // Invalid index
        if (row < 0 || row >= board.length || col < 0 || col >= board[row].length ||
                oldRow < 0 || oldRow >= board.length || oldCol < 0 || oldCol >= board[row].length) {
            return false;
        }

        System.out.println("isValidMove : " + Utility.getLabel(oldRow,oldCol) + " -> " + Utility.getLabel(row,col));

        // No checker
        if (board[oldRow][oldCol].equals(".")) {
            System.out.println("1");
            return false;
        }

        Player currentPlayer = gameState.isPlayerTurn() ? gameState.getPlayer() : gameState.getOpponent();
        // Another checker exists at new position
        if (!board[row][col].equals(".")) {
            System.out.println("2");
            return false;
        }

        boolean isKing = isKingChecker(board[oldRow][oldCol]);
        if (isKing && Math.abs(oldRow-row) == 1 && Math.abs(col-oldCol) == 1) {
            // King normal move
            return true;
        }

        if (Player.WHITE.equals(currentPlayer)) {
            if (!isKing && isMoveUp(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (isKing && Math.abs(oldRow-row) == 2) {
                // King captures
                if (isMoveUp(oldRow, row)) {
                    return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("b")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("b")));
                } else if (isMoveDown(oldRow, row)) {
                    return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("b")) ||
                            (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("b")));
                }
            } else if (!isKing && isMoveUp(oldRow, row) && Math.abs(row - oldRow) == 2) {
                // Man captures move
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("b")) ||
                        (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("b")));
            }
        } else {
            if (!isKing && isMoveDown(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1) {
                System.out.println("3");
                return true;
            } else if (isKing && Math.abs(oldRow-row) == 2) {
                // King captures
                if (isMoveUp(oldRow, row)) {
                    System.out.println("4" + (((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("w")))));
                    //System.out.println("Black King move up + left " + board[row+1][col+1]);
                    return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("w")));
                } else if (isMoveDown(oldRow, row)) {
                    System.out.println("5");
                    return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("w")));
                }
            } else if (!isKing && isMoveDown(oldRow, row) && Math.abs(row - oldRow) == 2) {
                // Man captures move
                // capture: check \ and / directions (must have opponent checker between two positions)
                System.out.println("6");
                return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                        (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("w")));
            }
        }
        System.out.println("7");
        return false;
    }

    private boolean isMoveLeft(int oldCol, int col) {
        return col < oldCol;
    }

    private boolean isMoveRight(int oldCol, int col) {
        return col > oldCol;
    }

    private boolean isMoveUp(int oldRow, int row) {
        return row < oldRow;
    }

    private boolean isMoveDown(int oldRow, int row) {
        return row > oldRow;
    }

    public boolean isKingChecker(String checker) {
        return checker.equals("W") || checker.equals("B");
    }
}

class GameState implements Cloneable {
    private String[][] board;
    private Player player;
    private Player opponent;
    private Mode mode;
    private float timeRemaining;
    private Set<String> playerManPosition;
    private Set<String> playerKingPosition;
    private Set<String> opponentManPosition;
    private Set<String> opponentKingPosition;
    private boolean isPlayerTurn;
    private List<Move> jumpList;

    private int[][] kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};;
    private int[][] kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};

    public GameState(String[][] board, Player player, Mode mode, float timeRemaining, boolean isPlayerTurn) {
        this.board = board;
        this.player = player;
        this.opponent = Player.WHITE.equals(player) ? Player.BLACK : Player.WHITE;
        this.mode = mode;
        this.timeRemaining = timeRemaining;
        playerManPosition = new HashSet<>();
        playerKingPosition = new HashSet<>();
        opponentManPosition = new HashSet<>();
        opponentKingPosition = new HashSet<>();
        this.isPlayerTurn = isPlayerTurn;
        this.jumpList = new ArrayList<>();
        setupPosition();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private void setupPosition() {
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                if(board[r][c].equals("b")) {
                    if (Player.BLACK.equals(player)) playerManPosition.add(Utility.boardLabel[r][c]);
                    else opponentManPosition.add(Utility.boardLabel[r][c]);
                } else if(board[r][c].equals("B")) {
                    if (Player.BLACK.equals(player)) playerKingPosition.add(Utility.boardLabel[r][c]);
                    else opponentKingPosition.add(Utility.boardLabel[r][c]);
                } else if (board[r][c].equals("w")) {
                    if (Player.WHITE.equals(player)) playerManPosition.add(Utility.boardLabel[r][c]);
                    else opponentManPosition.add(Utility.boardLabel[r][c]);
                } else if (board[r][c].equals("W")) {
                    if (Player.WHITE.equals(player)) playerKingPosition.add(Utility.boardLabel[r][c]);
                    else opponentKingPosition.add(Utility.boardLabel[r][c]);
                }
            }
        }
    }

    // Once a piece is crowned king for the first time, your turn ends even if jumping backwards is possible.
    // First time becoming a king will end the turn. No need to handle this case.
    public void getPossibleJumps(int currentRow, int currentCol, int[][] captureDirs, Move move, int jumpCount, boolean isKing, Set<String> visited) {
        System.out.println("getPossibleJumps " + Utility.getLabel(currentRow, currentCol));
        boolean isValid = false;
        for(int[] dir: captureDirs) {
            int row = currentRow + dir[1];
            int col = currentCol + dir[0];
            if (isValidMove(currentRow, currentCol, row, col, jumpCount == 0, isPlayerTurn, isKing)) {
                System.out.println("valid move");
                String newLabel = Utility.getLabel(row, col);
                if (move != null && move.getFrom().equals(newLabel)) continue;
                visited.add(Utility.getLabel(row, col));
                isValid = true;
                System.out.println("valid getPossibleJumps " + Utility.getLabel(currentRow, currentCol) + " -> " + newLabel);
                Move newMove = new Move(MoveType.JUMP, Utility.getLabel(currentRow, currentCol), newLabel, move, null, null);
                getPossibleJumps(row, col, captureDirs, newMove, jumpCount+1, isKing, visited);
                visited.remove(newLabel);
            }
        }

        if (!isValid && move != null) {
            jumpList.add(move);
        }
    }

    public List<Move> getAllPossibleMoves() {
        System.out.println("getAllPossibleMoves playerTurn: " + isPlayerTurn);
        int[][] regularDirs;
        int[][] captureDirs;
        Set<String> manCheckerPosition = isPlayerTurn ? playerManPosition : opponentManPosition;
        Set<String> kingCheckerPosition = isPlayerTurn ? playerKingPosition : opponentKingPosition;

        Player currentPlayer = isPlayerTurn ? player : opponent;

        if (Player.WHITE.equals(currentPlayer)) {
            regularDirs = new int[][]{{1, -1}, {-1, -1}};
            captureDirs = new int[][]{{2, -2}, {-2, -2}};
        } else {
            regularDirs = new int[][]{{1, 1}, {-1, 1}};
            captureDirs = new int[][]{{2, 2}, {-2, 2}};
        }

        List<Move> regularMoves = new ArrayList<>();
        List<Move> captureMoves = new ArrayList<>();

        // Get all possible moves of man checkers
        for(String checker: manCheckerPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[1];
            int currentCol = currentPostion[0];

            System.out.println("checker " + checker + " " + board[currentRow][currentCol]);

            getPossibleJumps(currentRow, currentCol, captureDirs, null, 0, false, new HashSet<>());
            // System.out.println("getAllPossibleMoves | jump size " + jumpList.size());

            if (jumpList == null || jumpList.isEmpty()) {
                for (int[] dir : regularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    if (isValidMove(currentRow, currentCol, row, col, true, isPlayerTurn, false)) {
                        System.out.println("Add possible move " + checker + " -> " + Utility.getLabel(row, col));
                        regularMoves.add(new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null, null, null));
                    }
                }
            } else {
                captureMoves = jumpList;
            }
        }

        // Get all possible moves of king checkers
        for(String checker: kingCheckerPosition) {
            System.out.println("king " + checker);
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[1];
            int currentCol = currentPostion[0];

            getPossibleJumps(currentRow, currentCol, kingCaptureDirs, null, 0, true, new HashSet<>());
            System.out.println("getAllPossibleMoves | jump size " + jumpList.size());

            if (jumpList == null || jumpList.isEmpty()) {
                for(int[] dir: kingRegularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    if (isValidMove(currentRow, currentCol, row, col, true, isPlayerTurn, true)) {
                        regularMoves.add(new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null, null, null));
                    }
                }
            } else {
                captureMoves = jumpList;
            }
        }

        jumpList = new ArrayList<>();

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        } else {
            return regularMoves;
        }

    }

    public boolean isValidMove(int oldRow, int oldCol, int row, int col, boolean isFirstTime, boolean isPlayerTurn, boolean isKing) {
//        System.out.println("isValidMove : " + oldRow + " " + oldCol + " -> " + row + " " + col);
//        System.out.println("isValidMove : turn " + isPlayerTurn);
        //System.out.println("isValidMove " + board[oldRow][oldCol] + " firstTime " + isFirstTime );
        // Invalid index
        if (row < 0 || row >= board.length || col < 0 || col >= board[row].length ||
                oldRow < 0 || oldRow >= board.length || oldCol < 0 || oldCol >= board[row].length) {
            return false;
        }

        System.out.println("isValidMove : " + Utility.getLabel(oldRow,oldCol) + " -> " + Utility.getLabel(row,col));

        // No checker
        if (board[oldRow][oldCol].equals(".") && isFirstTime) {
            System.out.println("1");
            return false;
        }

        Player currentPlayer = isPlayerTurn ? player : opponent;
        // Another checker exists at new position
        if (!board[row][col].equals(".")) {
            System.out.println("2");
            return false;
        }
        // Checker of opponent exists at new position
//        if ((Player.WHITE.equals(currentPlayer) && board[row][col].equalsIgnoreCase("b")) || (Player.BLACK.equals(currentPlayer) && board[row][col].equalsIgnoreCase("w"))) {
//            return false;
//        }

        if (isKing && Math.abs(oldRow-row) == 1 && Math.abs(col-oldCol) == 1) {
            // King normal move
            return true;
        }

        if (Player.WHITE.equals(currentPlayer)) {
            if (!isKing && isMoveUp(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (isKing && Math.abs(oldRow-row) == 2) {
                // King captures
                if (isMoveUp(oldRow, row)) {
                    return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("b")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("b")));
                } else if (isMoveDown(oldRow, row)) {
                    return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("b")) ||
                            (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("b")));
                }
            } else if (!isKing && isMoveUp(oldRow, row) && Math.abs(row - oldRow) == 2) {
                // Man captures move
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("b")) ||
                        (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("b")));
            }
        } else {
            if (!isKing && isMoveDown(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1) {
                System.out.println("3");
                return true;
            } else if (isKing && Math.abs(oldRow-row) == 2) {
                // King captures
                if (isMoveUp(oldRow, row)) {
                    System.out.println("4" + (((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("w")))));
                    //System.out.println("Black King move up + left " + board[row+1][col+1]);
                    return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("w")));
                } else if (isMoveDown(oldRow, row)) {
                    System.out.println("5");
                    return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("w")));
                }
            } else if (!isKing && isMoveDown(oldRow, row) && Math.abs(row - oldRow) == 2) {
                // Man captures move
                // capture: check \ and / directions (must have opponent checker between two positions)
                System.out.println("6");
                return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                        (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("w")));
            }
        }
        System.out.println("7");
        return false;
    }

    private boolean isMoveLeft(int oldCol, int col) {
        return col < oldCol;
    }

    private boolean isMoveRight(int oldCol, int col) {
        return col > oldCol;
    }

    private boolean isMoveUp(int oldRow, int row) {
        return row < oldRow;
    }

    private boolean isMoveDown(int oldRow, int row) {
        return row > oldRow;
    }

    public void applyNewMove(Move move) {
        int[] oldPosition = Utility.getPosition(move.getFrom());
        int[] newPosition = Utility.getPosition(move.getTo());
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];

        Player currentPlayer = isPlayerTurn ? player : opponent;
        boolean isMoveToKingArea = Utility.isKingArea(currentPlayer, newRow, newCol);

        if (isMoveToKingArea) {
            System.out.println("applyNewMove move to king " + move.getFrom() + " " + move.getTo() );
            board[newRow][newCol] = board[oldRow][oldCol].toUpperCase();
            if (currentPlayer.equals(player)) {
                playerKingPosition.add(move.getTo());
                playerManPosition.remove(move.getFrom());
            } else {
                opponentKingPosition.add(move.getTo());
                opponentManPosition.remove(move.getFrom());
            }
        } else {
            board[newRow][newCol] = board[oldRow][oldCol];
            if (currentPlayer.equals(player)) {
                playerManPosition.add(move.getTo());
                playerManPosition.remove(move.getFrom());
            } else {
                opponentManPosition.add(move.getTo());
                opponentManPosition.remove(move.getFrom());
            }
        }
        board[oldRow][oldCol] = ".";
    }

    public MoveLog applySingleMove(Move move) {
        int[] oldPosition = Utility.getPosition(move.getFrom());
        int[] newPosition = Utility.getPosition(move.getTo());
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];

        String fromChecker = board[oldRow][oldCol];

        applyNewMove(move);

        return new MoveLog(MoveType.JUMP.ONE, fromChecker, board[newRow][newCol], move.getFrom(), move.getTo(), null, null, null);
    }

    public MoveLog applyJumpMoves(Move move) {
        int[] oldPosition;
        int[] newPosition;
        int oldRow;
        int oldCol;
        int newRow;
        int newCol;

        Player currentPlayer = isPlayerTurn ? player : opponent;
        String fromChecker = null;
        Move currentMove = move;
        MoveLog prevMoveLog = null;
        MoveLog headMoveLog = null;
        while(currentMove != null) {
            oldPosition = Utility.getPosition(currentMove.getFrom());
            newPosition = Utility.getPosition(currentMove.getTo());
            oldRow = oldPosition[1];
            oldCol = oldPosition[0];
            newRow = newPosition[1];
            newCol = newPosition[0];
            System.out.println("applyJumpMoves from " + oldCol + " " + oldRow + " to " + newCol + " " + newRow);
            System.out.println("applyJumpMoves from " + currentMove.getFrom() + " to " + currentMove.getTo());

            fromChecker = board[oldRow][oldCol];

            applyNewMove(move);

            // Handle capture
            int[] capturedPosition = new int[] { (oldCol+newCol) / 2 , (oldRow+newRow) / 2}; // (x,y)
            String capturedLabel = Utility.getLabel(capturedPosition[1], capturedPosition[0]);
            String capturedChecker = board[capturedPosition[1]][capturedPosition[0]];
            if (opponent.equals(currentPlayer)) {
                playerManPosition.remove(capturedLabel);
                playerKingPosition.remove(capturedLabel);
            } else {
                opponentManPosition.remove(capturedLabel);
                opponentKingPosition.remove(capturedLabel);
            }
            board[capturedPosition[1]][capturedPosition[0]] = ".";

            // Move: latest -> old
            MoveLog moveLog = new MoveLog(MoveType.JUMP, fromChecker, board[newRow][newCol], currentMove.getFrom(), currentMove.getTo(), capturedLabel, capturedChecker, null);
            if (headMoveLog == null) headMoveLog = moveLog;
            if (prevMoveLog != null) prevMoveLog.setMoveLog(moveLog);
            prevMoveLog = moveLog;
            currentMove = currentMove.getMove();
        }

        return headMoveLog;
    }

    public MoveLog applyMove(Move move) {
        System.out.println("applyMove " + move.getMoveType() + " from " + move.getFrom() + " to " + move.getTo());
        if (MoveType.ONE.equals(move.getMoveType())) {
            return applySingleMove(move);
        } else if (MoveType.JUMP.equals(move.getMoveType())) {
            return applyJumpMoves(move);
        }
        return null;
    }

    public void resetNewMove(MoveLog moveLog) {
        int[] oldPosition = Utility.getPosition(moveLog.getFrom());
        int[] newPosition = Utility.getPosition(moveLog.getTo());
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];

        // Reverse move
        Player currentPlayer = isPlayerTurn ? player : opponent;
        if (isKingChecker(moveLog.getToChecker())) {
            if (player.equals(currentPlayer)) {
                playerKingPosition.remove(moveLog.getTo());
                playerManPosition.add(moveLog.getFrom());
            } else {
                opponentKingPosition.remove(moveLog.getTo());
                opponentManPosition.add(moveLog.getFrom());
            }
        } else {
            if (player.equals(currentPlayer)) {
                playerManPosition.remove(moveLog.getTo());
                playerManPosition.add(moveLog.getFrom());
            } else {
                opponentManPosition.remove(moveLog.getTo());
                opponentManPosition.add(moveLog.getFrom());
            }
        }
        board[oldRow][oldCol] = moveLog.getFromChecker();
        board[newRow][newCol] = ".";
    }

    public void resetJumpMoves(MoveLog moveLog) {
        int[] oldPosition;
        int[] newPosition;
        int oldRow;
        int oldCol;
        int newRow;
        int newCol;

        while(moveLog != null) {
            resetNewMove(moveLog);

            oldPosition = Utility.getPosition(moveLog.getFrom());
            newPosition = Utility.getPosition(moveLog.getTo());
            oldRow = oldPosition[1];
            oldCol = oldPosition[0];
            newRow = newPosition[1];
            newCol = newPosition[0];

            int[] capturedPosition = new int[] { (oldCol+newCol) / 2 , (oldRow+newRow) / 2}; // (x,y)
            String capturedChecker = moveLog.getCapturedChecker();
            String capturedLabel = moveLog.getCaptured();
            Player capturedPlayer = isPlayerTurn ? opponent : player;
            if (player.equals(capturedPlayer)) {
                if (isKingChecker(capturedChecker)) playerKingPosition.add(capturedLabel);
                else playerManPosition.add(capturedLabel);
            } else {
                if (isKingChecker(capturedChecker)) opponentKingPosition.add(capturedLabel);
                else opponentManPosition.add(capturedLabel);
            }
            board[capturedPosition[1]][capturedPosition[0]] = capturedChecker;

            moveLog = moveLog.getMoveLog();
        }
    }

    public void resetMove(MoveLog moveLog) {
        System.out.println("Reset from " + moveLog.getFrom() + " to " + moveLog.getTo() );

        if (MoveType.ONE.equals(moveLog.getMoveType())) {
            resetNewMove(moveLog);
        } else if(MoveType.JUMP.equals(moveLog.getMoveType())) {
            resetJumpMoves(moveLog);
        }
    }

    // Check if player is able to move any checkers
    public boolean canPlayerContinue() {
        int[][] dirs;
        int[][] kingDirs = new int[][]{{1, -1}, {-1, -1}, {1, 1}, {-1, 1}, {2, -2}, {-2, -2}, {2, 2}, {-2, 2}};
        if (Player.WHITE.equals(player)) {
            dirs = new int[][]{{1, -1}, {-1, -1}, {2, -2}, {-2, -2}};
        } else {
            dirs = new int[][]{{1, 1}, {-1, 1}, {2, 2}, {-2, 2}};
        }

        for(String checker: playerManPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: dirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true, true, false)) {
                    return true;
                }
            }
        }

        for(String checker: playerKingPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true, true, true)) {
                    return true;
                }
            }
        }

        System.out.println("canPlayerContinue false");
        return false;
    }

    public boolean canOpponentContinue() {
        int[][] dirs;
        int[][] kingDirs = new int[][]{{1, -1}, {-1, -1}, {1, 1}, {-1, 1}, {2, -2}, {-2, -2}, {2, 2}, {-2, 2}};
        if (Player.WHITE.equals(opponent)) {
            dirs = new int[][]{{1, -1}, {-1, -1}, {2, -2}, {-2, -2}};
        } else {
            dirs = new int[][]{{1, 1}, {-1, 1}, {2, 2}, {-2, 2}};
        }

        for(String checker: opponentManPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: dirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true, false, false)) {
                    return true;
                }
            }
        }

        for(String checker: opponentKingPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true, false, true)) {
                    return true;
                }
            }
        }
        System.out.println("canOpponentContinue false");
        return false;
    }

    public Integer utility() {
        // TODO
        return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 500;
    }

    public Integer evaluation() {
        // TODO
        System.out.println("evaluation king: " + playerKingPosition.size());
        if (opponentKingPosition.isEmpty()) {
            return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 50 + getPlayerSafeCheckers() * 10 + playerKingPosition.size() * 20 + getPlayerKingAreaChecker() + playerManPosition.size();
        } else {
            return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 50 + playerKingPosition.size() * 20 + getPlayerKingAreaChecker() + playerManPosition.size();
        }
    }

    public int getPlayerSafeCheckers() {
        int count = 0;
        for (String checker: playerManPosition) {
            int[] playerPos = Utility.getPosition(checker);
            int playerRow = playerPos[1];
            int playerCol = playerPos[0];
            boolean safe = true;

            int safeRow = Player.WHITE.equals(player) ? board.length-1 : 0;
            if (playerCol != 0 && playerCol != board.length-1 && playerRow != safeRow) {
                for(String opponent: opponentManPosition) {
                    int[] opponentPos = Utility.getPosition(opponent);
                    int opponentRow = opponentPos[1];

                    if ((Player.WHITE.equals(player) && playerRow > opponentRow) || (Player.BLACK.equals(player) && playerRow < opponentRow)){
                        safe = false;
                        break;
                    }
                }
            }
            if (safe) count++;
        }
        return count;
    }

    // Return number of player's checkers that do not move from king area (prevent opponent to enter king area)
    public int getPlayerKingAreaChecker() {
        int count = 0;
        for (String checker: playerManPosition) {
            if (Player.WHITE.equals(player) && Utility.whiteKingArea.contains(checker)) {
                count++;
            } else if (Player.BLACK.equals(player) && Utility.blackKingArea.contains(checker)) {
                count++;
            }
        }
        return count;
    }

    public boolean isTerminal() {
        // One player loses all checkers
        if (getPlayerCheckerSize() == 0 || getOpponentCheckerSize() == 0) {
            return true;
        }

        // No possible move
        if (!canOpponentContinue() && !canPlayerContinue()) {
            return true;
        }

        return false;
    }

    public void printState() {
        System.out.println("Player: " + player.toString());
        System.out.println("Mode: " + mode.toString());
        System.out.println("timeRemaining: " + timeRemaining);
        printBoard();
    }

    public void printBoard() {
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                System.out.print(board[r][c]);
            }
            System.out.println("");
        }
    }

    private Integer getPlayerCheckerSize() {
        return playerManPosition.size() + playerKingPosition.size();
    }

    private Integer getOpponentCheckerSize() {
        return opponentManPosition.size() + opponentKingPosition.size();
    }

    public String[][] getBoard() {
        return board;
    }

    public void setIsPlayerTurn(boolean isPlayerTurn) {
        this.isPlayerTurn = isPlayerTurn;
    }

    public boolean isKingChecker(String checker) {
        return checker.equals("W") || checker.equals("B");
    }

    public boolean isInitialState() {
        for(String checker: playerManPosition) {
            if (Player.WHITE.equals(player) && !Utility.blackKingArea.contains(checker)) {
                return false;
            } else if (Player.BLACK.equals(player) && !Utility.whiteKingArea.contains(checker)) {
                return false;
            }
        }

        for(String checker: opponentManPosition) {
            if (Player.WHITE.equals(opponent) && !Utility.blackKingArea.contains(checker)) {
                return false;
            } else if (Player.BLACK.equals(opponent) && !Utility.whiteKingArea.contains(checker)) {
                return false;
            }
        }
        return getPlayerCheckerSize() == 8 && getOpponentCheckerSize() == 8;
    }

    public Player getPlayer() {
        return player;
    }

    public Player getOpponent() {
        return opponent;
    }

    public Mode getMode() {
        return mode;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public Set<String> getPlayerManPosition() {
        return playerManPosition;
    }

    public Set<String> getPlayerKingPosition() {
        return playerKingPosition;
    }

    public Set<String> getOpponentManPosition() {
        return opponentManPosition;
    }

    public Set<String> getOpponentKingPosition() {
        return opponentKingPosition;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public List<Move> getJumpList() {
        return jumpList;
    }

    public int[][] getKingRegularDirs() {
        return kingRegularDirs;
    }

    public int[][] getKingCaptureDirs() {
        return kingCaptureDirs;
    }
}

enum Mode {
    SINGLE,
    GAME
}

enum Player {
    WHITE,
    BLACK
}

enum MoveType {
    ONE("E"), JUMP("J");

    private String name;
    MoveType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class MoveLog {
    private MoveType moveType;
    private String fromChecker;
    private String toChecker;
    private String from;
    private String to;
    private String captured;
    private String capturedChecker;
    private MoveLog moveLog;

    public MoveLog(MoveType moveType, String fromChecker, String toChecker, String from, String to, String captured, String capturedChecker, MoveLog moveLog) {
        this.moveType = moveType;
        this.fromChecker = fromChecker;
        this.toChecker = toChecker;
        this.from = from;
        this.to = to;
        this.captured = captured;
        this.capturedChecker = capturedChecker;
        this.moveLog = moveLog;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public String getFromChecker() {
        return fromChecker;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getCaptured() {
        return captured;
    }

    public String getCapturedChecker() {
        return capturedChecker;
    }

    public String getToChecker() {
        return toChecker;
    }

    public MoveLog getMoveLog() {
        return moveLog;
    }

    public void setMoveLog(MoveLog moveLog) {
        this.moveLog = moveLog;
    }

    public void print() {
        Stack<MoveLog> stack = new Stack<>();
        MoveLog m = this;
        while(m != null) {
            stack.push(m);
            m = m.getMoveLog();
        }

        while(!stack.isEmpty()) {
            m = stack.pop();
            System.out.println(m.getMoveType().getName() + " " + m.getFrom() + " " + m.getTo());
        }
    }
}

class Move {
    private MoveType moveType;
    private String from;
    private String to;
    private String fromChecker;
    private String toChecker;
    private Move move;

    public Move(MoveType moveType, String from, String to, Move move, String fromChecker, String toChecker) {
        this.moveType = moveType;
        this.from = from;
        this.to = to;
        this.move = move;
        this.fromChecker = fromChecker;
        this.toChecker = toChecker;
    }

    public boolean isBecomeKing() {
        return this.toChecker != null && this.fromChecker != null && !this.toChecker.equals(this.fromChecker);
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Move getMove() {
        return move;
    }

    public void setMove(Move move) {
        this.move = move;
    }

    public String getFromChecker() {
        return fromChecker;
    }

    public String getToChecker() {
        return toChecker;
    }

    public void setToChecker(String toChecker) {
        this.toChecker = toChecker;
    }

    public void print() {
        Stack<Move> stack = new Stack<>();
        Move m = this;
        while(m != null) {
            stack.push(m);
            m = m.getMove();
        }

        while(!stack.isEmpty()) {
            m = stack.pop();
            System.out.println(m.getMoveType().getName() + " " + m.getFrom() + " " + m.getTo());
        }
    }
}

class Utility {
    public static String[][] boardLabel = {
            {"a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8"},
            {"a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7"},
            {"a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6"},
            {"a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5"},
            {"a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4"},
            {"a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3"},
            {"a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2"},
            {"a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"}
    };

    public static Set<String> whiteKingArea;
    public static Set<String> blackKingArea;
    static {
        whiteKingArea = new HashSet<>();
        whiteKingArea.add("b8");
        whiteKingArea.add("d8");
        whiteKingArea.add("f8");
        whiteKingArea.add("h8");

        blackKingArea = new HashSet<>();
        blackKingArea.add("a1");
        blackKingArea.add("c1");
        blackKingArea.add("e1");
        blackKingArea.add("g1");
    }

    public static String[] cols = {"a", "b", "c", "d", "e", "f", "g", "h"};

    public static int[] getPosition(String label) {
        char[] c = label.toCharArray();
        return new int[] {c[0] - 'a', 7 - (c[1] - '1')};
    }

    public static String getLabel(int row, int col) {
        return cols[col] + "" + (8 - row);
    }

    public static boolean isKingArea(Player player, int row, int col) {
        String label = getLabel(row, col);
        if (Player.WHITE.equals(player)) {
            return whiteKingArea.contains(label);
        } else {
            return blackKingArea.contains(label);
        }
    }
}