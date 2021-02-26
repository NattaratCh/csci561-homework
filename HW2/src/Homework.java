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
    private String[] bestMove;
    private Integer depth;
    private Integer depthLimit;

    public CheckerGame(Integer depthLimit) {
        this.gameState = readInput("./test-cases/input.txt");
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

            GameState gameState = new GameState(board, player, mode, timeRemaining);
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
        System.out.println("Best move: from" + bestMove[0] + " to " + bestMove[1]);
        System.out.println("value: " + value);
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
        List<String[]> possibleMoves = state.getAllPossibleMoves(true);
        for (String[] move: possibleMoves) {
            String[] captured = state.applyMove(move);
            if (state.canOpponentContinue()) {
                next = minValue(state, alpha, beta);
            } else {
                next = maxValue(state, alpha, beta);
            }

            System.out.println("maxValue value: " + value);

            if (next > value) {
                value = next;
                if (depth == depthLimit) bestMove = move;
            }
            state.resetMove(move, captured);

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
        List<String[]> possibleMoves = state.getAllPossibleMoves(false);
        for (String[] move: possibleMoves) {
            String[] captured = state.applyMove(move);
            if (state.canPlayerContinue()) {
                value = Math.min(value, maxValue(state, alpha, beta));
            } else {
                value = Math.min(value, minValue(state, alpha, beta));
            }
            System.out.println("minValue value: " + value);
            state.resetMove(move, captured);

            if (value <= alpha) return alpha;
            beta = Math.min(beta, value);
        }
        return value;
    }
}

class GameState {
    private String[][] board;
    private Player player;
    private Mode mode;
    private float timeRemaining;
    private Set<String> playerManPosition;
    private Set<String> playerKingPosition;
    private Set<String> opponentManPosition;
    private Set<String> opponentKingPosition;


    public GameState(String[][] board, Player player, Mode mode, float timeRemaining) {
        this.board = board;
        this.player = player;
        this.mode = mode;
        this.timeRemaining = timeRemaining;
        playerManPosition = new HashSet<>();
        playerKingPosition = new HashSet<>();
        opponentManPosition = new HashSet<>();
        opponentKingPosition = new HashSet<>();
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

    public List<String[]> getAllPossibleMoves(boolean playerTurn) {
        System.out.println("getAllPossibleMoves playerTurn: " + playerTurn);
        int[][] regularDirs;
        int[][] captureDirs;
        int[][] kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};;
        int[][] kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};
        Set<String> manCheckerPosition = playerTurn ? playerManPosition : opponentManPosition;
        Set<String> kingCheckerPosition = playerTurn ? playerKingPosition : opponentKingPosition;

        Player currentPlayer = playerTurn ? player : (Player.WHITE.equals(player) ? Player.BLACK : Player.WHITE);

        if (Player.WHITE.equals(currentPlayer)) {
            regularDirs = new int[][]{{1, -1}, {-1, -1}};
            captureDirs = new int[][]{{2, -2}, {-2, -2}};
        } else {
            regularDirs = new int[][]{{1, 1}, {-1, 1}};
            captureDirs = new int[][]{{2, 2}, {-2, 2}};
        }

        List<String[]> regularMoves = new ArrayList<>();
        List<String[]> captureMoves = new ArrayList<>();

        // Get all possible moves of man checkers
        for(String checker: manCheckerPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[0];
            int currentCol = currentPostion[1];

            for(int[] dir: regularDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col, playerTurn)) {
                    regularMoves.add(new String[] {checker, Utility.getLabel(row, col)});
                }
            }

            for(int[] dir: captureDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col, playerTurn)) {
                    captureMoves.add(new String[] {checker, Utility.getLabel(row, col)});
                }
            }
        }

        // Get all possible moves of king checkers
        for(String checker: kingCheckerPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[0];
            int currentCol = currentPostion[1];

            for(int[] dir: kingRegularDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col, playerTurn)) {
                    regularMoves.add(new String[] {checker, Utility.getLabel(row, col)});
                }
            }

            for(int[] dir: kingCaptureDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col, playerTurn)) {
                    captureMoves.add(new String[] {checker, Utility.getLabel(row, col)});
                }
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        } else {
            return regularMoves;
        }

    }

    public boolean isValidMove(int oldRow, int oldCol, int row, int col, boolean playerTurn) {
        //System.out.println("isValidMove : " + oldRow + " " + oldCol + " -> " + row + " " + col);
        // Invalid index
        if (row < 0 || row >= board.length || col < 0 || col >= board[row].length ||
                oldRow < 0 || oldRow >= board.length || oldCol < 0 || oldCol >= board[row].length) {
            return false;
        }

        // No checker
        if (board[oldRow][oldCol].equals(".")) {
            return false;
        }

        Player currentPlayer = playerTurn ? player : (Player.WHITE.equals(player) ? Player.BLACK : Player.WHITE);
        // Checker of opponent exists at new position
        if ((Player.WHITE.equals(currentPlayer) && board[row][col].equalsIgnoreCase("b")) || (Player.BLACK.equals(currentPlayer) && board[row][col].equalsIgnoreCase("w"))) {
            return false;
        }

        if (Player.WHITE.equals(currentPlayer)) {
            if (oldRow - row == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (oldRow - row == 2) {
                // capture: check \ and / directions (must have opponent checker between two positions)
//                if (col - oldCol == 2) System.out.println("## consider " + (row+1) + " " + (col-1));
//                if (col - oldCol == -2) System.out.println("## consider " + (row+1) + " " + (col+1));
                return ((col - oldCol == 2 && board[row+1][col-1].equalsIgnoreCase("b")) ||
                        ((col - oldCol == -2 && board[row+1][col+1].equalsIgnoreCase("b"))));
            }
        } else {
            if (row - oldRow == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (row - oldRow == 2) {
                // capture: check \ and / directions (must have opponent checker between two positions)
//                if (col - oldCol == 2) System.out.println("## consider " + (row-1) + " " + (col-1));
//                if (col - oldCol == -2) System.out.println("## consider " + (row-1) + " " + (col+1));
                return ((col - oldCol == 2 && board[row-1][col-1].equalsIgnoreCase("w")) ||
                        ((col - oldCol == -2 && board[row-1][col+1].equalsIgnoreCase("w"))));
            }
        }
        return false;
    }

    public String[] applyMove(String[] move) {
        // TODO change to king if enter king area
        int[] oldPosition = Utility.getPosition(move[0]);
        int[] newPosition = Utility.getPosition(move[1]);
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];

        board[newRow][newCol] = board[oldRow][oldCol];
        board[oldRow][oldCol] = ".";

        if (Math.abs(newRow - newCol) == 2) {
            // capture move
            int[] capturedPosition = new int[] { (oldCol+newCol) / 2 , (oldRow+newRow) / 2}; // (x,y)
            String capturedLabel = Utility.getLabel(capturedPosition[1], capturedPosition[0]);
            String color = board[capturedPosition[1]][capturedPosition[0]];
            Player capturedPlayer = color.equalsIgnoreCase("w") ? Player.WHITE : Player.BLACK;
            if (player.equals(capturedPlayer)) {
                playerManPosition.remove(capturedLabel);
                playerKingPosition.remove(capturedLabel);
            } else {
                opponentManPosition.remove(capturedLabel);
                opponentKingPosition.remove(capturedLabel);
            }
            board[capturedPosition[1]][capturedPosition[0]] = ".";
            return new String[] {capturedLabel, color}; // e.g. ("a1", "W")
        }
        return null;
    }

    public void resetMove(String[] move, String[] captured) {
        int[] oldPosition = Utility.getPosition(move[0]);
        int[] newPosition = Utility.getPosition(move[1]);
        int oldRow = oldPosition[1];
        int oldCol = oldPosition[0];
        int newRow = newPosition[1];
        int newCol = newPosition[0];

        board[oldRow][oldCol] = board[newRow][newCol];
        board[newRow][newCol] = ".";

        if (Math.abs(newRow - newCol) == 2) {
            int[] capturedPosition = new int[] { (oldCol+newCol) / 2 , (oldRow+newRow) / 2}; // (x,y)
            String color = captured[1];
            String capturedLabel = captured[0];
            Player capturedPlayer = color.equalsIgnoreCase("w") ? Player.WHITE : Player.BLACK;
            if (player.equals(capturedPlayer)) {
                if (color.equals("w")) playerManPosition.add(capturedLabel);
                else playerKingPosition.add(capturedLabel);
            } else {
                if (color.equals("b")) opponentManPosition.add(capturedLabel);
                else opponentKingPosition.add(capturedLabel);
            }
            board[capturedPosition[1]][capturedPosition[0]] = color;
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
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], false)) {
                    return true;
                }
            }
        }

        for(String checker: opponentKingPosition) {
            int[] position = Utility.getPosition(checker);
            for(int[] dir: kingDirs) {
                if (isValidMove(position[1], position[0], position[1] + dir[1], position[0] + dir[0], false)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Integer utility() {
        // TODO adjust utility function
        return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 500 + getPlayerCheckerSize() * 50;
    }

    public Integer evaluation() {
        // TODOD
        return (getPlayerCheckerSize() - getOpponentCheckerSize()) * 50 + getPlayerCheckerSize();
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
}

enum Mode {
    SINGLE,
    GAME
}

enum Player {
    WHITE,
    BLACK
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

    public static String[] cols = {"a", "b", "c", "d", "e", "f", "g", "h"};

    public static int[] getPosition(String label) {
        char[] c = label.toCharArray();
        return new int[] {7 - (c[1] - '1'), c[0] - 'a'};
    }

    public static String getLabel(int row, int col) {
        return cols[col] + "" + (8 - row);
    }
}