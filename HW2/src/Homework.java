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
    private GameState gameState;

    public CheckerGame() {
        this.gameState = readInput("./test-cases/input.txt");
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
            return gameState;
        } catch (Exception e) {
            System.out.println("readInput: " + e.getMessage());
            return null;
        }
    }

    public void nextMove() {
        alphaBetaPruning();
    }

    public void alphaBetaPruning() {
        // return moves
    }

    // Player
    public void maxValue() {

    }

    // Opponent
    public void minValue() {

    }
}

class GameState {
    private String[][] board;
    private Player player;
    private Mode mode;
    private float timeRemaining;
    private List<String> playerManPosition;
    private List<String> playerKingPosition;
    private List<String> opponentManPosition;
    private List<String> opponentKingPosition;


    public GameState(String[][] board, Player player, Mode mode, float timeRemaining) {
        this.board = board;
        this.player = player;
        this.mode = mode;
        this.timeRemaining = timeRemaining;
        playerManPosition = new ArrayList<>();
        playerKingPosition = new ArrayList<>();
        opponentManPosition = new ArrayList<>();
        opponentKingPosition = new ArrayList<>();
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

    private List<String> getAllPossibleMoves() {
        int[][] regularDirs;
        int[][] captureDirs;
        int[][] kingRegularDirs;
        int[][] kingCaptureDirs;
        if (Player.WHITE.equals(player)) {
            regularDirs = new int[][]{{1, -1}, {-1, -1}};
            captureDirs = new int[][]{{2, -2}, {-2, -2}};
            kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};
            kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};
        } else {
            regularDirs = new int[][]{{1, 1}, {-1, 1}};
            captureDirs = new int[][]{{2, 2}, {-2, 2}};
            kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};
            kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};
        }

        List<String> regularMoves = new ArrayList<>();
        List<String> captureMoves = new ArrayList<>();

        // Get all possible moves of man checkers
        for(String checker: playerManPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[0];
            int currentCol = currentPostion[1];

            for(int[] dir: regularDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col)) {
                    regularMoves.add(Utility.getLabel(row, col));
                }
            }

            for(int[] dir: captureDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col)) {
                    captureMoves.add(Utility.getLabel(row, col));
                }
            }
        }

        // Get all possible moves of king checkers
        for(String checker: playerKingPosition) {
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[0];
            int currentCol = currentPostion[1];

            for(int[] dir: kingRegularDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col)) {
                    regularMoves.add(Utility.getLabel(row, col));
                }
            }

            for(int[] dir: kingCaptureDirs) {
                int row = currentRow + dir[1];
                int col = currentCol + dir[0];
                if (isValidMove(currentRow, currentCol, row, col)) {
                    captureMoves.add(Utility.getLabel(row, col));
                }
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        } else {
            return regularMoves;
        }

    }

    private boolean isValidMove(int oldRow, int oldCol, int row, int col) {
        // Invalid index
        if (row < 0 || row > board.length || col < 0 || col > board[row].length) {
            return false;
        }

        // No checker
        if (board[oldRow][oldCol].equals(".")) {
            return false;
        }

        // Checker of opponent exists at new position
        if ((Player.WHITE.equals(player) && board[row][col].equalsIgnoreCase("b")) || (Player.BLACK.equals(player) && board[row][col].equalsIgnoreCase("w"))) {
            return false;
        }

        if (Player.WHITE.equals(player)) {
            if (oldRow - row == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (oldRow - row == 2) {
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((col - oldCol == 2 && board[row-1][col+1].equalsIgnoreCase("b")) ||
                        ((col - oldCol == -2 && board[row-1][col-1].equalsIgnoreCase("b"))));
            }
        } else {
            if (row - oldRow == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (row - oldRow == 2) {
                // capture: check \ and / directions (must have opponent checker between two positions)
                return ((col - oldCol == 2 && board[row+1][col+1].equalsIgnoreCase("w")) ||
                        ((col - oldCol == -2 && board[row+1][col-1].equalsIgnoreCase("w"))));
            }
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