import java.io.*;
import java.util.*;

/**
 * Created by nuning on 3/12/21.
 */
public class CustomMasterGame {
    private static int capture = 0;
    private static int king = 0;
    private static int noCaptureTime = 0;
    private static int noKingCrownTime = 0;
    private static Map<String, Integer> boardHistory = new HashMap<>();
    private static boolean noMoves = false;
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("First move (Agent/Player):");
        String firstMove = in.nextLine();

        Player agent = Player.WHITE;
        Player player = Player.BLACK;
        Player[] turn = new Player[2];
        if (firstMove.equals("Agent")) {
            turn[0] = agent;
            turn[1] = player;
        } else {
            turn[0] = player;
            turn[1] = agent;
        }

        File file = new File("./game/result/final_result.txt");
        if (file.exists()) {
            file.delete();
        }

        double blackTime = 100;
        double whiteTime = 100;
        double givenTime = 100;
        StringBuilder sb1 = startGame(blackTime, whiteTime, turn, agent, player, in);
        writeFinalResult(sb1.toString(), givenTime);
    }

    public static StringBuilder startGame(double agentTime, double playerTime, Player[] turn, Player agent, Player player, Scanner in) {
        StringBuilder sb = new StringBuilder();
        sb.append("**********************\n");
        sb.append("Agent is " + agent + "\n");
        sb.append("Player is " + player + "\n");
        System.out.println("**********************");
        System.out.println("Agent is " + agent);
        System.out.println("Player is " + player);
        int i = 0;
        String[][] board = initializeBoard();
        String boardStr = null;

        File agentFile = new File("./game/agent/playdata.txt");
        if (agentFile.exists()) {
            agentFile.delete();
        }

        File playerFile = new File("./game/player/playdata.txt");
        if (playerFile.exists()) {
            playerFile.delete();
        }

        boardHistory.clear();
        noKingCrownTime = 0;
        noCaptureTime = 0;
        noMoves = false;

        while(!isTerminal(board, boardStr, agentTime, playerTime, agent, player, sb)) {
            System.out.println("ROUND: " + (i + 1) + " turn: " + turn[i % 2].toString());
            capture = 0;
            king = 0;
            if (turn[i % 2].equals(player)) {
                writeInput(board, player, playerTime);
                System.out.println("Your moves (Type end to end your move): ");
                List<String> playerMove = new ArrayList<>();
                while (in.hasNext()) {
                    String input = in.nextLine().trim();
                    if (input.equals("end")) break;
                    playerMove.add(input);
                }

                writeMove(playerMove);
            } else {
                writeInput(board, agent, agentTime);
                Homework.main(new String[]{});
            }

            List<String[]> moves = readMove();
            if (moves != null) {
                applyMove(moves, board, turn[i % 2]);
                noMoves = false;
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

            if (turn[i%2].equals(agent)) {
                double usedTime = getUsedTime();
                agentTime -= usedTime;
            } else {
                playerTime -= 0.1;
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

            if (turn[i % 2].equals(agent)) {
                writeResult(agent, i, board, moves, agentTime);
            } else {
                writeResult(player, i, board, moves, playerTime);
            }

            if (noMoves) {
                sb.append("Terminated no moves\n");
                System.out.println("Terminated no moves");
                if (agent.equals(turn[i%2])) {
                    sb.append("Player WIN!\n");
                    System.out.println("Player WIN!");
                } else {
                    sb.append("Agent WIN!\n");
                    System.out.println("Agent WIN!");
                }
                break;
            }

            i++;
        }
        System.out.println("End Game");
        sb.append("Agent time remaining: " + agentTime + "\n");
        sb.append("Player time remaining: " + playerTime + "\n");
        return sb;
    }

    public static void writeFinalResult(String result, double time) {
        File file = new File("./result/final_result.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write("Given time: " + time);
            br.write(System.lineSeparator());
            br.write(result);
            br.write("===============================");
            br.write(System.lineSeparator());

            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeResult(Player turn, int i, String[][] board, List<String[]> moves, double time) {
        File file = new File("./game/result/result.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists() && i == 0) {
            file.delete();
        }
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write("Round: " + (i+1));
            br.write(System.lineSeparator());
            br.write("Turn: " + turn.toString() + System.lineSeparator());
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

    public static boolean isTerminal(String[][] board, String boardStr, double agentTime, double playerTime, Player agent, Player player, StringBuilder sb) {
        if (playerTime <= 0) {
            System.out.println("Terminated player runs out of time");
            sb.append("Terminated player runs out of time\n");
            sb.append("Agent WIN!");
            return true;
        }
        if (agentTime <= 0) {
            System.out.println("Terminated agent runs out of time");
            sb.append("Terminated agent runs out of time\n");
            sb.append("Player WIN!");
            return true;
        }
        // no capture for 50 moves or no king for 50 moves (1 move = 2 piles)
        // (50 plies for your agent + 50 by your opponent)
        if (noCaptureTime >= 100) {
            System.out.println("Terminated no capture for 50 times");
            sb.append("Terminated no capture for 50 times\n");
            if (agentTime > playerTime) {
                System.out.println("Agent WIN!");
                sb.append("Agent WIN!\n");
            } else if (playerTime > agentTime) {
                System.out.println("Player WIN!");
                sb.append("Player WIN!\n");
            } else {
                System.out.println("DRAW!");
                sb.append("DRAW!\n");
            }
            return true;
        }
        if (noKingCrownTime >= 100) {
            System.out.println("Terminated no king crown for 50 times");
            sb.append("Terminated no king crown for 50 times\n");
            if (agentTime > playerTime) {
                System.out.println("Agent WIN!");
                sb.append("Agent WIN!\n");
            } else if (playerTime > agentTime) {
                System.out.println("Player WIN!");
                sb.append("Player WIN!\n");
            } else {
                System.out.println("DRAW!");
                sb.append("DRAW!\n");
            }
            return true;
        }
        // same board position 3 times
        int count = boardHistory.getOrDefault(boardStr, 0);
        if (count >= 3) {
            System.out.println("Terminated same board position 3 times");
            sb.append("Terminated same board position 3 times\n");
            if (agentTime > playerTime) {
                System.out.println("Agent WIN!");
                sb.append("Agent WIN!\n");
            } else if (playerTime > agentTime) {
                System.out.println("Player WIN!");
                sb.append("Player WIN!\n");
            } else {
                System.out.println("DRAW!");
                sb.append("DRAW!\n");
            }
            return true;
        }

        int agentSize = getPlayerSize(board, agent);
        int playerSize = getPlayerSize(board, player);
        if (agentSize == 0) {
            System.out.println("Player WIN!");
            sb.append("No agent checkers left\n");
            sb.append("Player WIN!\n");
            return true;
        }
        if (playerSize == 0) {
            System.out.println("Agent WIN!");
            sb.append("No player checkers left\n");
            sb.append("Agent WIN!\n");
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
            System.out.println("readMove: " + e.getMessage());
            return null;
        }
    }

    public static void writeMove(List<String> moves) {
        try {
            FileWriter writer = new FileWriter("./src/output.txt");
            for(String move: moves) {
                writer.write(move + System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    public static int getPlayerSize(String[][] board, Player player) {
        String color = Player.BLACK.equals(player) ? "b" : "w";
        int count = 0;
        for(int r=0; r<board.length; r++) {
            for(int c=0; c<board[r].length; c++) {
                if(board[r][c].equalsIgnoreCase(color)) count++;
            }
        }
        return count;
    }
}
