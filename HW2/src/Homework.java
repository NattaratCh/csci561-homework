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
        GameState gameState = readInput("./test-cases/input6.txt");
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
        System.out.println("minimax depth limit: " + depthLimit);
        Integer value = maxValue(gameState, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        System.out.println("value: " + value);
        bestMove.print();
    }

    // Player
    public Integer maxValue(GameState state, Integer alpha, Integer beta, Integer depth) {
        System.out.println("#############################################################");
        System.out.println("maxValue depth " + depth);
        if (isTerminal(state)) {
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
        for (Move move: possibleMoves) {
            GameState nextState = move.getState();
            nextState.printBoard();
            if (canOpponentContinue(nextState)) {
                System.out.println("opponent moves");
                nextState.setIsPlayerTurn(false);
                next = minValue(nextState, alpha, beta, depth+1);
            } else {
                System.out.println("player moves");
                nextState.setIsPlayerTurn(true);
                next = maxValue(nextState, alpha, beta, depth+1);
            }

            System.out.println("maxValue value: " + next);
            System.out.println("--");

            if (next > value) {
                value = next;
                if (depth == 0) bestMove = move;
                System.out.println("## depth: " + depth);
                System.out.println("## best move: " + move.getFrom() + " => " + move.getTo());
            }

            if (value >= beta) return value;
            alpha = Math.max(alpha, value);
        }
        return value;
    }

    // Opponent
    public Integer minValue(GameState state, Integer alpha, Integer beta, Integer depth) {
        System.out.println("#############################################################");
        System.out.println("minValue depth " + depth);
        if (isTerminal(state)) {
            return state.utility();
        }

        if (depth == depthLimit) {
            return state.evaluation();
        }

        Integer value = Integer.MAX_VALUE;
        List<Move> possibleMoves = getAllPossibleMoves(state);
        for (Move move: possibleMoves) {
            GameState nextState = move.getState();
            nextState.printBoard();
            if (canPlayerContinue(nextState)) {
                nextState.setIsPlayerTurn(true);
                value = Math.min(value, maxValue(nextState, alpha, beta, depth+1));
            } else {
                nextState.setIsPlayerTurn(false);
                value = Math.min(value, minValue(nextState, alpha, beta, depth+1));
            }
            System.out.println("minValue value: " + value);

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
        Move current = new Move(MoveType.JUMP, null, Utility.getLabel(currentRow, currentCol), null, board[currentRow][currentCol], board[currentRow][currentCol], gameState);
        loopJump(current, captureDirs, queue, moves, true);

        while(!queue.isEmpty()) {
            Move move = queue.poll();
            String fromLabel = move.getTo();
            int[] fromPosition = Utility.getPosition(fromLabel);
            board = move.getState().getBoard();
            if (isKingChecker(board[fromPosition[1]][fromPosition[0]])) {
                loopJump(move, kingCaptureDirs, queue, moves, false);
            } else {
                loopJump(move, captureDirs, queue, moves, false);
            }
        }
        return moves;
    }

    public void loopJump(Move prevMove, int[][] captureDirs, Queue<Move> queue, List<Move> moves, boolean isFirstJump) {
        if (prevMove != null && prevMove.isBecomeKing()) {
            // First time becoming a king should stop jumping
            moves.add(prevMove);
            return;
        }
        boolean isValid = false;
        String[][] board;
        for(int[] dir: captureDirs) {
            GameState gameState = GameState.newInstance(prevMove.getState());
            String fromLabel = prevMove.getTo();
            int[] position = Utility.getPosition(fromLabel);
            int oldRow = position[1];
            int oldCol = position[0];
            int row = oldRow + dir[1];
            int col = oldCol + dir[0];
            if (isValidMove(gameState, oldRow, oldCol, row, col, gameState.isPlayerTurn())) {
                isValid = true;
                Move nextMove = new Move(MoveType.JUMP, prevMove.getTo(), Utility.getLabel(row, col), prevMove, prevMove.getToChecker(), null, null);
                applyMove(gameState, nextMove);
                nextMove.setState(gameState);
                board = gameState.getBoard();
                String toChecker =  board[row][col];
                nextMove.setToChecker(toChecker);
                queue.add(nextMove);
            }
        }

        if (!isValid && !isFirstJump) {
            moves.add(prevMove);
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
            cloneState = GameState.newInstance(gameState);

            //System.out.println("checker " + checker + " " + cloneState.getBoard()[currentRow][currentCol]);

            List<Move> jumps = getPossibleJump(cloneState, currentRow, currentCol, captureDirs);
            System.out.println("getAllPossibleMoves | jump size " + jumps.size());

            if (jumps.isEmpty()) {
                System.out.println("regular move");
                for (int[] dir : regularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    cloneState = GameState.newInstance(gameState);
                    if (isValidMove(cloneState, currentRow, currentCol, row, col, cloneState.isPlayerTurn())) {
                        String[][] board = cloneState.getBoard();
                        System.out.println("Add possible move " + checker + " -> " + Utility.getLabel(row, col));
                        Move newMove = new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null, board[currentRow][currentCol], board[row][col], null);
                        applyMove(cloneState, newMove);
                        newMove.setState(cloneState);
                        regularMoves.add(newMove);
                        cloneState.printBoard();
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
            cloneState = GameState.newInstance(gameState);

            List<Move> jumps = getPossibleJump(cloneState, currentRow, currentCol, kingCaptureDirs);
            System.out.println("getAllPossibleMoves | jump size " + jumps.size());

            if (jumps.isEmpty()) {
                for(int[] dir: kingRegularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    cloneState = GameState.newInstance(gameState);
                    if (isValidMove(cloneState, currentRow, currentCol, row, col, cloneState.isPlayerTurn())) {
                        String[][] board = cloneState.getBoard();
                        Move newMove = new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null, board[currentRow][currentCol], board[row][col], null);
                        applyMove(cloneState, newMove);
                        newMove.setState(cloneState);
                        regularMoves.add(newMove);
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

    public boolean isValidMove(GameState gameState, int oldRow, int oldCol, int row, int col, boolean isPlayerTurn) {
        String[][] board = gameState.getBoard();
        // Invalid index
        if (row < 0 || row >= board.length || col < 0 || col >= board[row].length ||
                oldRow < 0 || oldRow >= board.length || oldCol < 0 || oldCol >= board[row].length) {
            return false;
        }

        //System.out.println("isValidMove : " + Utility.getLabel(oldRow,oldCol) + " -> " + Utility.getLabel(row,col));

        // No checker
        if (board[oldRow][oldCol].equals(".")) {
            return false;
        }

        Player currentPlayer = isPlayerTurn ? gameState.getPlayer() : gameState.getOpponent();
        // Another checker exists at new position
        if (!board[row][col].equals(".")) {
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
//            System.out.println("6");
//            System.out.println(!isKing && isMoveDown(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1);
            if (!isKing && isMoveDown(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (isKing && Math.abs(oldRow-row) == 2) {
                // King captures
                if (isMoveUp(oldRow, row)) {
                    //System.out.println("Black King move up + left " + board[row+1][col+1]);
                    return ((isMoveRight(oldCol, col) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row+1][col+1].equalsIgnoreCase("w")));
                } else if (isMoveDown(oldRow, row)) {
                    return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                            (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("w")));
                }
            } else if (!isKing && isMoveDown(oldRow, row) && Math.abs(row - oldRow) == 2) {
                // Man captures move
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((isMoveRight(oldCol, col) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                        (isMoveLeft(oldCol, col) && board[row-1][col+1].equalsIgnoreCase("w")));
            }
        }
        //System.out.println("7");
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

    public boolean canPlayerContinue(GameState gameState) {
        int[][] dirs;
        int[][] kingDirs = new int[][]{{1, -1}, {-1, -1}, {1, 1}, {-1, 1}, {2, -2}, {-2, -2}, {2, 2}, {-2, 2}};
        if (Player.WHITE.equals(gameState.getPlayer())) {
            dirs = new int[][]{{1, -1}, {-1, -1}, {2, -2}, {-2, -2}};
        } else {
            dirs = new int[][]{{1, 1}, {-1, 1}, {2, 2}, {-2, 2}};
        }

        for(String checker: gameState.getPlayerManPosition()) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: dirs) {
                if (isValidMove(gameState, position[1], position[0], position[1] + dir[1], position[0] + dir[0], true)) {
                    return true;
                }
            }
        }

        for(String checker: gameState.getPlayerKingPosition()) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(gameState, position[1], position[0], position[1] + dir[1], position[0] + dir[0], true)) {
                    return true;
                }
            }
        }

        //System.out.println("canPlayerContinue false");
        return false;
    }

    public boolean canOpponentContinue(GameState gameState) {
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//        gameState.printBoard();
        int[][] dirs;
        int[][] kingDirs = new int[][]{{1, -1}, {-1, -1}, {1, 1}, {-1, 1}, {2, -2}, {-2, -2}, {2, 2}, {-2, 2}};
        if (Player.WHITE.equals(gameState.getOpponent())) {
            dirs = new int[][]{{1, -1}, {-1, -1}, {2, -2}, {-2, -2}};
        } else {
            dirs = new int[][]{{1, 1}, {-1, 1}, {2, 2}, {-2, 2}};
        }



        for(String checker: gameState.getOpponentManPosition()) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: dirs) {
                if (isValidMove(gameState, position[1], position[0], position[1] + dir[1], position[0] + dir[0], false)) {
                    return true;
                }
            }
        }

        for(String checker: gameState.getOpponentKingPosition()) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(gameState, position[1], position[0], position[1] + dir[1], position[0] + dir[0], false)) {
                    return true;
                }
            }
        }
        //System.out.println("canOpponentContinue false");
        return false;
    }

    public boolean isTerminal(GameState gameState) {
        // One player loses all checkers
        if (gameState.getPlayerCheckerSize() == 0 || gameState.getOpponentCheckerSize() == 0) {
            return true;
        }

        // No possible move
        if (!canOpponentContinue(gameState) && !canPlayerContinue(gameState)) {
            return true;
        }

        return false;
    }
}

class GameState {
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
        setupPosition();
    }

    public GameState(GameState gameState) {
        this.board = new String[gameState.getBoard().length][gameState.getBoard().length];
        for (int r=0; r<gameState.getBoard().length; r++) {
            this.board[r] = Arrays.copyOf(gameState.getBoard()[r], gameState.getBoard().length);
        }
        this.player = gameState.player;
        this.opponent = gameState.opponent;
        this.mode = gameState.mode;
        this.timeRemaining = gameState.timeRemaining;
        this.playerManPosition = new HashSet<>(gameState.playerManPosition);
        this.playerKingPosition = new HashSet<>(gameState.playerKingPosition);
        this.opponentManPosition = new HashSet<>(gameState.opponentManPosition);
        this.opponentKingPosition = new HashSet<>(gameState.opponentKingPosition);
        this.isPlayerTurn = gameState.isPlayerTurn;
    }

    public static GameState newInstance(GameState gameState) {
        return new GameState(gameState);
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

    public void printState() {
        System.out.println("Player: " + player.toString());
        System.out.println("Mode: " + mode.toString());
        System.out.println("timeRemaining: " + timeRemaining);
        printBoard();
    }

    public void printBoard() {
        System.out.println("==============================");
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                System.out.print(board[r][c]);
            }
            System.out.println("");
        }
    }

    public Integer getPlayerCheckerSize() {
        return playerManPosition.size() + playerKingPosition.size();
    }

    public Integer getOpponentCheckerSize() {
        return opponentManPosition.size() + opponentKingPosition.size();
    }

    public String[][] getBoard() {
        return board;
    }

    public void setIsPlayerTurn(boolean isPlayerTurn) {
        this.isPlayerTurn = isPlayerTurn;
    }

    public boolean isInitialState() {
        for(String label: Utility.whiteInitialLabels) {
            int[] position = Utility.getPosition(label);
            if (!board[position[1]][position[0]].equals("w")) return false;
        }

        for(String label: Utility.blackInitialLabels) {
            int[] position = Utility.getPosition(label);
            if (!board[position[1]][position[0]].equals("b")) return false;
        }
        return getPlayerCheckerSize() == 12 && getOpponentCheckerSize() == 12;
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
    private GameState state;
    private Move move;

    public Move(MoveType moveType, String from, String to, Move move, String fromChecker, String toChecker, GameState state) {
        this.moveType = moveType;
        this.from = from;
        this.to = to;
        this.move = move;
        this.fromChecker = fromChecker;
        this.toChecker = toChecker;
        this.state = state;
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

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
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
            if (m.getFrom() == null) continue;
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

    public static String[] blackInitialLabels = {
            "b8","d8","f8","h8",
            "a7","c7","e7","g7",
            "b6","d6","f6","h6"
    };

    public static String[] whiteInitialLabels = {
            "a3","c3","e3","g3",
            "b2","d2","f2","h2",
            "a1","c1","e1","g1"
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