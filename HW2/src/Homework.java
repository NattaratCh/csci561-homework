import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by nuning on 2/25/21.
 */

public class Homework {
    public static void main(String[] args) {
        CheckerGame checkerGame = new CheckerGame(5);
        checkerGame.start();
    }

}

class CheckerGame {
    private GameState gameState;
    private MoveLog bestMove;
    private Integer depth;
    private Integer depthLimit;

    public CheckerGame(Integer depthLimit) {
        this.gameState = readInput("./test-cases/input1.txt");
        this.depthLimit = depthLimit;
        this.depth = 0;
    }

    public void start() {
        gameState.printState();
        nextMove();
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

    public void nextMove() {
        minimax();
    }

    public void minimax() {
        Integer value = maxValue(gameState, Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println("value: " + value);
        bestMove.print();
    }

    // Player
    public Integer maxValue(GameState state, Integer alpha, Integer beta) {
        if (state.isTerminal()) {
            return state.utility();
        }

        if (depth == depthLimit) {
            return state.evaluation();
        }

        Integer value = Integer.MIN_VALUE;
        Integer next = null;
        depth += 1;
        List<Move> possibleMoves = state.getAllPossibleMoves();
        for (Move move: possibleMoves) {
            MoveLog moveLog = state.applyMove(move); // TODO handle null
            if (state.canOpponentContinue()) {
                state.setIsPlayerTurn(false);
                next = minValue(state, alpha, beta);
            } else {
                state.setIsPlayerTurn(true);
                next = maxValue(state, alpha, beta);
            }

            System.out.println("maxValue value: " + value);

            if (next > value) {
                value = next;
                if (depth == depthLimit) bestMove = moveLog;
            }
            state.setIsPlayerTurn(true);
            state.resetMove(moveLog);

            if (value >= beta) return value;
            alpha = Math.max(alpha, value);
        }
        return value;
    }

    // Opponent
    public Integer minValue(GameState state, Integer alpha, Integer beta) {
        if (state.isTerminal()) {
            return state.utility();
        }

        if (depth == depthLimit) {
            return state.evaluation();
        }

        Integer value = Integer.MAX_VALUE;
        depth += 1;
        List<Move> possibleMoves = state.getAllPossibleMoves();
        for (Move move: possibleMoves) {
            MoveLog moveLog = state.applyMove(move);
            if (state.canPlayerContinue()) {
                state.setIsPlayerTurn(true);
                value = Math.min(value, maxValue(state, alpha, beta));
            } else {
                state.setIsPlayerTurn(false);
                value = Math.min(value, minValue(state, alpha, beta));
            }
            System.out.println("minValue value: " + value);
            state.setIsPlayerTurn(false);
            state.resetMove(moveLog);

            if (value <= alpha) return alpha;
            beta = Math.min(beta, value);
        }
        return value;
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

    public void getPossibleJumps(int currentRow, int currentCol, int[][] captureDirs, Move move, int jumpCount) {
        boolean isValid = false;
        for(int[] dir: captureDirs) {
            int row = currentRow + dir[1];
            int col = currentCol + dir[0];
            if (isValidMove(currentRow, currentCol, row, col, jumpCount == 0)) {
                isValid = true;
                System.out.println("valid getPossibleJumps " + currentCol + " " + currentRow + " -> " + col + " " + row);
                Move newMove = new Move(MoveType.JUMP, Utility.getLabel(currentRow, currentCol), Utility.getLabel(row, col), move);

                if (isKingChecker(Utility.getLabel(row, col))) {
                    getPossibleJumps(row, col, kingCaptureDirs, newMove, jumpCount+1);
                } else {
                    getPossibleJumps(row, col, captureDirs, newMove, jumpCount+1);
                }
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
            int currentRow = currentPostion[0];
            int currentCol = currentPostion[1];

            getPossibleJumps(currentRow, currentCol, captureDirs, null, 0);
            System.out.println("getAllPossibleMoves | jump size " + jumpList.size());

            if (jumpList == null || jumpList.isEmpty()) {
                for(int[] dir: regularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    if (isValidMove(currentRow, currentCol, row, col, true)) {
                        regularMoves.add(new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null));
                    }
                }
            } else {
                captureMoves = jumpList;
            }

//            for(int[] dir: captureDirs) {
//                int row = currentRow + dir[1];
//                int col = currentCol + dir[0];
//                if (isValidMove(currentRow, currentCol, row, col)) {
//                    captureMoves.add(new String[] {checker, Utility.getLabel(row, col)});
//                }
//            }
        }

        // Get all possible moves of king checkers
        for(String checker: kingCheckerPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[0];
            int currentCol = currentPostion[1];

            getPossibleJumps(currentRow, currentCol, kingCaptureDirs, null, 0);
            System.out.println("getAllPossibleMoves | jump size " + jumpList.size());

            if (jumpList == null || jumpList.isEmpty()) {
                for(int[] dir: kingRegularDirs) {
                    int row = currentRow + dir[1];
                    int col = currentCol + dir[0];
                    if (isValidMove(currentRow, currentCol, row, col, true)) {
                        regularMoves.add(new Move(MoveType.ONE, checker, Utility.getLabel(row, col), null));
                    }
                }
            } else {
                captureMoves = jumpList;
            }

//            for(int[] dir: kingCaptureDirs) {
//                int row = currentRow + dir[1];
//                int col = currentCol + dir[0];
//                if (isValidMove(currentRow, currentCol, row, col)) {
//                    captureMoves.add(new String[] {checker, Utility.getLabel(row, col)});
//                }
//            }
        }

        jumpList = new ArrayList<>();

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        } else {
            return regularMoves;
        }

    }

    public boolean isValidMove(int oldRow, int oldCol, int row, int col, boolean isFirstTime) {
        //System.out.println("isValidMove : " + oldRow + " " + oldCol + " -> " + row + " " + col);
        // Invalid index
        if (row < 0 || row >= board.length || col < 0 || col >= board[row].length ||
                oldRow < 0 || oldRow >= board.length || oldCol < 0 || oldCol >= board[row].length) {
            return false;
        }

        // No checker
        if (board[oldRow][oldCol].equals(".") && isFirstTime) {
            return false;
        }

        Player currentPlayer = isPlayerTurn ? player : opponent;
        // Checker of opponent exists at new position
        if ((Player.WHITE.equals(currentPlayer) && board[row][col].equalsIgnoreCase("b")) || (Player.BLACK.equals(currentPlayer) && board[row][col].equalsIgnoreCase("w"))) {
            return false;
        }

        if (Player.WHITE.equals(currentPlayer)) {
            if (oldRow - row == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (oldRow - row == 2) {
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((col - oldCol == 2 && board[row+1][col-1].equalsIgnoreCase("b")) ||
                        ((col - oldCol == -2 && board[row+1][col+1].equalsIgnoreCase("b"))));
            }
        } else {
            if (row - oldRow == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (row - oldRow == 2) {
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((col - oldCol == 2 && board[row-1][col-1].equalsIgnoreCase("w")) ||
                        ((col - oldCol == -2 && board[row-1][col+1].equalsIgnoreCase("w"))));
            }
        }
        return false;
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
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true)) {
                    return true;
                }
            }
        }

        for(String checker: playerKingPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canOpponentContinue() {
        int[][] dirs;
        int[][] kingDirs = new int[][]{{1, -1}, {-1, -1}, {1, 1}, {-1, 1}, {2, -2}, {-2, -2}, {2, 2}, {-2, 2}};
        Player opponent = Player.WHITE.equals(player) ? Player.BLACK : Player.WHITE;
        if (Player.WHITE.equals(opponent)) {
            dirs = new int[][]{{1, -1}, {-1, -1}, {2, -2}, {-2, -2}};
        } else {
            dirs = new int[][]{{1, 1}, {-1, 1}, {2, 2}, {-2, 2}};
        }

        for(String checker: opponentManPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: dirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true)) {
                    return true;
                }
            }
        }

        for(String checker: opponentKingPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], true)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Integer utility() {
        // TODO
        return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 500 + playerKingPosition.size() * 100 + playerManPosition.size();
    }

    public Integer evaluation() {
        // TODO
        return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 50 + playerKingPosition.size() * 10 + playerManPosition.size();
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
        return playerManPosition.size() + playerManPosition.size();
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
    private Move move;

    public Move(MoveType moveType, String from, String to, Move move) {
        this.moveType = moveType;
        this.from = from;
        this.to = to;
        this.move = move;
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
}

class Utility {
    public static String[][] boardLabel = {
            {"a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8"},
            {"a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7"},
            {"a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6"},
            {"a5", "b5", "c8", "d5", "e5", "f5", "g5", "h5"},
            {"a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4"},
            {"a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3"},
            {"a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2"},
            {"a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"}
    };

    public static Set<String> whiteKingAre;
    public static Set<String> blackKingArea;
    static {
        whiteKingAre = new HashSet<>();
        whiteKingAre.add("b8");
        whiteKingAre.add("d8");
        whiteKingAre.add("f8");
        whiteKingAre.add("h8");

        blackKingArea = new HashSet<>();
        blackKingArea.add("a1");
        blackKingArea.add("c1");
        blackKingArea.add("e1");
        blackKingArea.add("g1");
    }

    public static String[] cols = {"a", "b", "c", "d", "e", "f", "g", "h"};

    public static int[] getPosition(String label) {
        char[] c = label.toCharArray();
        return new int[] {7 - (c[1] - '1'), c[0] - 'a'};
    }

    public static String getLabel(int row, int col) {
        return cols[col] + "" + (8 - row);
    }

    public static boolean isKingArea(Player player, int row, int col) {
        String label = getLabel(row, col);
        if (Player.WHITE.equals(player)) {
            return whiteKingAre.contains(label);
        } else {
            return blackKingArea.contains(label);
        }
    }
}