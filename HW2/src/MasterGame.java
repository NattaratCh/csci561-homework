import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by nuning on 3/6/21.
 */
public class MasterGame {
    private static int capture = 0;
    private static int king = 0;
    private static int noCaptureTime = 0;
    private static int noKingCrownTime = 0;
    private static Map<String, Integer> boardHistory = new HashMap<>();
    private static boolean noMoves = false;
    public static void main(String[] args) {
        double blackTime = 100;
        double whiteTime = 100;
        int i = 0;
        Player[] turn = {Player.BLACK, Player.WHITE};
        String[][] board = initializeBoard();
        String boardStr = null;

        File playerFile = new File("./player/playdata.txt");
        if (playerFile.exists()) {
            playerFile.delete();
        }

        File opponentFile = new File("./opponent/playdata.txt");
        if (opponentFile.exists()) {
            opponentFile.delete();
        }

        while(!isTerminal(board, boardStr, blackTime, whiteTime)) {
            System.out.println("ROUND: " + (i + 1) + " turn: " + turn[i % 2].toString());
            capture = 0;
            king = 0;
            if (turn[i % 2].equals(Player.BLACK)) {
                writeInput(board, Player.BLACK, blackTime);
                Homework.main(new String[]{});
            } else {
                writeInput(board, Player.WHITE, whiteTime);
                HomeworkNoPruning.main(new String[]{});
            }

            List<String[]> moves = readMove();
            if (moves != null) {
                applyMove(moves, board, turn[i % 2]);
            } else {
                noMoves = true;
            }

            boardStr = getBoardString(board);
            if (boardHistory.containsKey(boardStr)) {
                boardHistory.put(boardStr, boardHistory.get(boardStr) + 1);
            } else {
                boardHistory.put(boardStr, 1);
            }

            printBoard(board);

            double usedTime = getUsedTime();
            if (turn[i % 2].equals(Player.BLACK)) {
                blackTime -= usedTime;
            } else {
                whiteTime -= usedTime;
            }

            if (capture == 0) {
                noCaptureTime++;
            } else {
                noCaptureTime = 0;
            }

            if (king == 0) {
                noKingCrownTime++;
            } else {
                noKingCrownTime = 0;
            }

            if (turn[i % 2].equals(Player.BLACK)) {
                writeResult(turn[i%2], i, board, moves, blackTime);
            } else {
                writeResult(turn[i%2], i, board, moves, whiteTime);
            }

            if (noMoves) {
                System.out.println("Terminated no moves");
                if (Player.BLACK.equals(turn[i%2])) {
                    System.out.println("WHITE WIN!");
                } else {
                    System.out.println("BLACK WIN!");
                }
                break;
            }

            i++;
        }
        System.out.println("End Game");
    }

    public static void writeResult(Player player, int i, String[][] board, List<String[]> moves, double time) {
        File file = new File("./src/result.txt");
        if (file.exists() && i == 0) {
            file.delete();
        }
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write("Round: " + (i+1));
            br.write(System.lineSeparator());
            br.write("Turn: " + player.toString() + System.lineSeparator());
            br.write("Time remain: " + time + System.lineSeparator());

            if (moves == null) {
                br.write("LOSE Cannot move" + System.lineSeparator());
            } else {
                for (String[] move: moves) {
                    br.write(move[0] + " " + move[1] + " " + move[2] + System.lineSeparator());
                }
            }


            for(int r=0; r<board.length; r++) {
                for(int c=0; c<board[r].length; c++) {
                    br.write(board[r][c]);
                }
                br.write(System.lineSeparator());
            }

            br.write("===============================");
            br.write(System.lineSeparator());

            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean isTerminal(String[][] board, String boardStr, double blackTime, double whiteTime) {
        if (whiteTime <= 0) {
            System.out.println("Terminated white runs out of time");
            return true;
        }
        if (blackTime <= 0) {
            System.out.println("Terminated black runs out of time");
            return true;
        }
        // no capture for 50 moves or no king for 50 moves (1 move = 2 piles)
        // (50 plies for your agent + 50 by your opponent)
        if (noCaptureTime >= 100) {
            System.out.println("Terminated no capture for 50 times");
            if (blackTime > whiteTime) {
                System.out.println("BLACK WIN!");
            } else if (whiteTime > blackTime) {
                System.out.println("WHITE WIN!");
            } else {
                System.out.println("DRAW!");
            }
            return true;
        }
        if (noKingCrownTime >= 100) {
            System.out.println("Terminated no king crown for 50 times");
            if (blackTime > whiteTime) {
                System.out.println("BLACK WIN!");
            } else if (whiteTime > blackTime) {
                System.out.println("WHITE WIN!");
            } else {
                System.out.println("DRAW!");
            }
            return true;
        }
        // same board position 3 times
        int count = boardHistory.getOrDefault(boardStr, 0);
        if (count >= 3) {
            System.out.println("Terminated same board position 3 times");
            if (blackTime > whiteTime) {
                System.out.println("BLACK WIN!");
            } else if (whiteTime > blackTime) {
                System.out.println("WHITE WIN!");
            } else {
                System.out.println("DRAW!");
            }
            return true;
        }

        // TODO Win conditions: one side cannot move
        int whitePlayers = getPlayerSize(board, "w");
        int blackPlayer = getPlayerSize(board, "b");
        if (whitePlayers == 0) {
            System.out.println("BLACK WIN!");
            return true;
        }
        if (blackPlayer == 0) {
            System.out.println("WHITE WIN!");
            return true;
        }
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
        System.out.println("MasterGame applyMove");
        Set<String> kingArea = Player.BLACK.equals(player) ? Utility.blackKingArea : Utility.whiteKingArea;
        for(String[] move: moves) {
            System.out.println("MasterGame applyMove " + move[0] + " " + move[1] + " " + move[2]);
            String action = move[0];
            String from = move[1];
            String to = move[2];
            System.out.println(action + " " + from + " " + to);
            int[] fromPosition = Utility.getPosition(from);
            int[] toPosition = Utility.getPosition(to);

            if (kingArea.contains(to)) king++;

            if(action.equals("J")) {
                board[toPosition[1]][toPosition[0]] = kingArea.contains(to) ? board[fromPosition[1]][fromPosition[0]].toUpperCase() : board[fromPosition[1]][fromPosition[0]];
                board[fromPosition[1]][fromPosition[0]] = ".";

                int x = fromPosition[0] < toPosition[0] ? 1 : -1;
                int y = fromPosition[1] < toPosition[1] ? 1 : -1;
                if (x == 1 && y == 1) {
                    // Right-Down
                    for(int r=fromPosition[1]+1; r<toPosition[1]; r++) {
                        for(int c=fromPosition[0]+1; c<toPosition[0]; c++) {
                            if (!board[r][c].equals(".")) capture++;
                            board[r][c] = ".";
                        }
                    }
                } else if (x==1 && y==-1) {
                    // Right-Up
                    for(int r=fromPosition[1]-1; r>toPosition[1]; r--) {
                        for(int c=fromPosition[0]+1; c<toPosition[0]; c++) {
                            if (!board[r][c].equals(".")) capture++;
                            board[r][c] = ".";
                        }
                    }
                } else if (x==-1 && y==-1) {
                    // Left-Up
                    for(int r=fromPosition[1]-1; r>toPosition[1]; r--) {
                        for(int c=fromPosition[0]-1; c>toPosition[0]; c--) {
                            if (!board[r][c].equals(".")) capture++;
                            board[r][c] = ".";
                        }
                    }
                } else if (x==-1 && y==1) {
                    // Left-Down
                    for(int r=fromPosition[1]+1; r<toPosition[1]; r++) {
                        for(int c=fromPosition[0]-1; c>toPosition[0]; c--) {
                            if (!board[r][c].equals(".")) capture++;
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
        if (!file.exists()) {
            return null;
        }
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
            file.delete();
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

    public static String getBoardString(String[][] board) {
        StringBuilder sb = new StringBuilder();
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                if(!board[r][c].equals(".")) {
                    sb.append(board[r][c] + "" + Utility.getLabel(r, c) + "|");
                }
            }
        }
        return sb.toString();
    }

    public static int getPlayerSize(String[][] board, String color) {
        int count = 0;
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                if(board[r][c].equalsIgnoreCase(color)) count++;
            }
        }
        return count;
    }
}
