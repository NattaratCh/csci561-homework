import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by nuning on 3/6/21.
 */
public class MasterGame {
    public static void main(String[] args) {
        double blackTime = 300;
        double whiteTime = 300;
        int i = 0;
        Player[] turn = {Player.BLACK, Player.WHITE};
        String[][] board = initializeBoard();

        while(i != 10) {
            if (turn[i % 2].equals(Player.BLACK)) {
                writeInput(board, Player.BLACK, blackTime);
            } else {
                writeInput(board, Player.WHITE, whiteTime);
            }

            Homework.main(new String[]{});
            List<String[]> moves = readMove();
            applyMove(moves, board, turn[i % 2]);

            System.out.println("ROUND: " + (i + 1) + " turn: " + turn[i % 2].toString());
            printBoard(board);

            double usedTime = getUsedTime();
            if (turn[i % 2].equals(Player.BLACK)) {
                blackTime -= usedTime;
            } else {
                whiteTime -= usedTime;
            }

            i++;
        }
        System.out.println("End Game");
    }

    public static boolean isTerminal() {
        return false;
    }

    public static void printBoard(String[][] board) {
        System.out.println("==============================");
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                System.out.print(board[r][c]);
            }
            System.out.println("");
        }
    }

    public static void applyMove(List<String[]> moves, String[][] board, Player player) {
        System.out.println("applyMove");
        Set<String> kingArea = Player.BLACK.equals(player) ? Utility.blackKingArea : Utility.whiteKingArea;
        for(String[] move: moves) {
            String action = move[0];
            String from = move[1];
            String to = move[2];
            System.out.println(action + " " + from + " " + to);
            int[] fromPosition = Utility.getPosition(from);
            int[] toPosition = Utility.getPosition(to);

            if(action.equals("J")) {
                board[toPosition[1]][toPosition[0]] = kingArea.contains(to) ? board[fromPosition[1]][fromPosition[0]].toUpperCase() : board[fromPosition[1]][fromPosition[0]];
                board[fromPosition[1]][fromPosition[0]] = ".";

                int x = fromPosition[0] < toPosition[0] ? 1 : -1;
                int y = fromPosition[1] < toPosition[1] ? 1 : -1;
                if (x == 1 && y == 1) {
                    // Right-Down
                    for(int r=fromPosition[1]+1; r<toPosition[1]; r++) {
                        for(int c=fromPosition[0]+1; c<toPosition[0]; c++) {
                            board[r][c] = ".";
                        }
                    }
                } else if (x==1 && y==-1) {
                    // Right-Up
                    for(int r=fromPosition[1]-1; r>toPosition[1]; r--) {
                        for(int c=fromPosition[0]+1; c<toPosition[0]; c++) {
                            board[r][c] = ".";
                        }
                    }
                } else if (x==-1 && y==-1) {
                    // Left-Up
                    for(int r=fromPosition[1]-1; r>toPosition[1]; r--) {
                        for(int c=fromPosition[0]-1; c>toPosition[0]; c--) {
                            board[r][c] = ".";
                        }
                    }
                } else if (x==-1 && y==1) {
                    // Left-Down
                    for(int r=fromPosition[1]+1; r<toPosition[1]; r++) {
                        for(int c=fromPosition[0]-1; c>toPosition[0]; c--) {
                            board[r][c] = ".";
                        }
                    }
                }
            } else if(action.equals("E")) {
                board[toPosition[1]][toPosition[0]] = kingArea.contains(to) ? board[fromPosition[1]][fromPosition[0]].toUpperCase() : board[fromPosition[1]][fromPosition[0]];
                board[fromPosition[1]][fromPosition[0]] = ".";
            }
        }
    }

    public static List<String[]> readMove() {
        File file = new File("./src/output.txt");
        BufferedReader reader;
        List<String[]> moves = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                String[] value = line.split("\\s");
                moves.add(value);
                line = reader.readLine();
            }
            return moves;
        } catch (Exception e) {
            System.out.println("initialzeBoard: " + e.getMessage());
            return null;
        }
    }

    public static String[][] initializeBoard() {
        File file = new File("./src/initialBoard.txt");
        BufferedReader reader;
        int boardSize = 8;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = "";

            String[][] board = new String[boardSize][boardSize];
            for(int r=0; r<boardSize; r++) {
                line = reader.readLine().trim();
                board[r] = line.split("");
            }

            return board;
        } catch (Exception e) {
            System.out.println("initialzeBoard: " + e.getMessage());
            return null;
        }
    }

    public static void writeInput(String[][] board, Player player, double time) {
        try {
            // TODO change output path
            FileWriter writer = new FileWriter("./src/input.txt");
            writer.write(Mode.GAME.toString() + System.lineSeparator());
            writer.write(player.toString() + System.lineSeparator());
            writer.write(String.valueOf(time) + System.lineSeparator());
            for(int r=0; r<board.length; r++) {
                for (int c=0; c<board[r].length; c++) {
                    writer.write(board[r][c]);
                }
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getUsedTime() {
        File file = new File("./src/time.txt");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            return Double.valueOf(line);
        } catch (Exception e) {
            System.out.println("initialzeBoard: " + e.getMessage());
            return 0;
        }
    }
}
