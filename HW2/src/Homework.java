import javafx.util.Pair;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Created by nuning on 2/25/21.
 */

public class Homework {
    public static void main(String[] args) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long startTime = threadMXBean.getCurrentThreadCpuTime() + threadMXBean.getCurrentThreadUserTime();
        CheckerGame checkerGame = new CheckerGame(threadMXBean, startTime);
        checkerGame.start();
        writeTime(checkerGame.getUseTime());
    }

    private static void writeTime(double time) {
        try {
            FileWriter writer = new FileWriter("./src/time.txt");
            writer.write(String.valueOf(time));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CheckerGame {
    private ThreadMXBean threadMXBean;
    private long startTime;
    private Map<String, Integer> playHistory;
    private Integer depthLimit;
    private int[][] kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};;
    private int[][] kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};

    public CheckerGame(ThreadMXBean threadMXBean, long startTime) {
        this.depthLimit = 1;
        this.startTime = startTime;
        this.threadMXBean = threadMXBean;
        this.playHistory = new HashMap<>();
    }

    public void start() {

        GameState gameState = readInput("./src/input.txt");
        // TODO change input path
        // GameState gameState = readInput("./test-cases/input7.txt");
        if (Mode.GAME.equals(gameState.getMode())) {
            setPlayHistory();
        }
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

            // Line 2 - Color (WHITE,BLACK)
            line = reader.readLine().trim();
            Player player = Player.valueOf(line);

            // Line 3 - time remaining
            line = reader.readLine().trim();
            double timeRemaining = Double.valueOf(line);

            // Line 4 - board state
            String[][] board = new String[boardSize][boardSize];
            for(int r=0; r<boardSize; r++) {
                line = reader.readLine().trim();
                board[r] = line.split("");
            }

            GameState gameState = new GameState(board, player, mode, timeRemaining, true);
            return gameState;
        } catch (Exception e) {
            System.out.println("readInput: " + e.getMessage());
            return null;
        }
    }

    public void setPlayHistory() {
        File file = new File("./player/playdata.txt");
        if (!file.exists()) return;

        BufferedReader reader;
        int i=0;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                playHistory.put(line.trim(), i++);
                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getUseTime() {
        long currentTime = threadMXBean.getCurrentThreadCpuTime() + threadMXBean.getCurrentThreadUserTime();
        System.out.println("usedTime " + (currentTime - startTime)/1000000000.0);
        return (currentTime - startTime)/1000000000.0;
    }

    public void nextMove(GameState gameState) {
        Pair<Double, Move> best = minimax(gameState);
        best.getValue().getState().printBoard();
        writeBoard(best);
        writeOutput(best);
        if (Mode.GAME.equals(gameState.getMode())) {
            writePlayData(best);
        }
    }

    private void writeBoard(Pair<Double, Move> best) {
        if (best == null) return;
        File file = new File("./player/result_agent.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        GameState gameState = best.getValue().getState();
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(System.lineSeparator());
            br.write("Time remain: " + gameState.getTimeRemaining() + System.lineSeparator());

            String[][] board = gameState.getBoard();
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

    public Pair<Double, Move> minimax(GameState gameState) {
        if (Mode.GAME.equals(gameState.getMode())) {
            if (gameState.getTimeRemaining() < 1.0) {
                depthLimit = 1;
            } else if (gameState.getTimeRemaining() < 50) {
                depthLimit = 2;
            } else {
                depthLimit = 3;
            }
        }

        if (gameState.isInitialState()) {
            depthLimit = 1;
        }


        System.out.println("agent minimax depth limit: " + depthLimit);
        Pair<Double, Move> value = maxValue(gameState, -Double.MAX_VALUE, Double.MAX_VALUE, 0);
        return value;
    }

    private void writeOutput(Pair<Double, Move> best) {
        if (best == null || best.getValue() == null) {
            System.out.println("Smart Agent: Cannot move");
            return;
        }
        List<String> results = best.getValue().print();

        try {
            // TODO change output path
            FileWriter writer = new FileWriter("./src/output.txt");
            for(String line: results) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writePlayData(Pair<Double, Move> best) {
        if (best == null || best.getValue() == null || MoveType.JUMP.equals(best.getValue().getMoveType())) return;
        File file = new File("./player/playdata.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        FileWriter fr = null;
        Move bestMove = best.getValue();
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(bestMove.getFrom() + " " + bestMove.getTo() + System.lineSeparator());
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Player
    public Pair<Double, Move> maxValue(GameState state, Double alpha, Double beta, Integer depth) {
        System.out.println("#############################################################");
        System.out.println("maxValue depth " + depth);
        System.out.println("maxValue player " + state.getPlayer());
        state.setIsPlayerTurn(true);
        if (isTerminal(state)) {
            return new Pair(utility(state), null);
        }

        if (depth == depthLimit) {
            return new Pair(evaluation(state), null);
        }

        Pair<Double, Move> value = new Pair<>(-Double.MAX_VALUE, null);
        PriorityQueue<Move> possibleMoves = getAllPossibleMoves(state);
        while (!possibleMoves.isEmpty()) {
            Move move = possibleMoves.poll();
            GameState nextState = move.getState();
            nextState.printBoard();
            nextState.setIsPlayerTurn(false);
            Pair<Double, Move> fromMinPlayer = minValue(nextState, alpha, beta, depth+1);
            System.out.println("fromMinPlayer value: " + fromMinPlayer.getKey());
            if (fromMinPlayer.getValue() != null) System.out.println("fromMinPlayer move: " + fromMinPlayer.getValue().getFrom() + " -> " + fromMinPlayer.getValue().getTo());

//            System.out.println("moves " + move.getFrom() + " -> " + move.getTo());
//            System.out.println("maxValue value: " + value.getKey());
//            System.out.println("maxValue fromMinPlayer: " + fromMinPlayer.getKey());
//            System.out.println("----------------------------");

            if (fromMinPlayer.getKey() > value.getKey()) {
                value = new Pair<>(fromMinPlayer.getKey(), move);
            }

            System.out.println("maxValue value: " + value.getKey());
            System.out.println("maxValue move: " + value.getValue().getFrom() + " -> " + value.getValue().getTo());

            if (value.getKey() >= beta) return value;
            alpha = Math.max(alpha, value.getKey());

            if (Mode.SINGLE.equals(state.getMode()) && 0.95 * state.getTimeRemaining() < getUseTime()) {
                return value;
            } else if (Mode.GAME.equals(state.getMode())) {
                //TODO
                if (getUseTime() >= 0.10 * state.getTimeRemaining()) return value;
            }
        }
        return value;
    }

    // Opponent
    public Pair<Double, Move> minValue(GameState state, Double alpha, Double beta, Integer depth) {
        System.out.println("#############################################################");
        System.out.println("minValue depth " + depth);
        System.out.println("minValue player " + state.getOpponent());
        state.setIsPlayerTurn(false);
        if (isTerminal(state)) {
            return new Pair(utility(state), null);
        }

        if (depth == depthLimit) {
            return new Pair(evaluation(state), null);
        }

        Pair<Double, Move> value = new Pair<>(Double.MAX_VALUE, null);
        PriorityQueue<Move> possibleMoves = getAllPossibleMoves(state);
        while (!possibleMoves.isEmpty()) {
            Move move = possibleMoves.poll();
            GameState nextState = move.getState();
            nextState.printBoard();
            nextState.setIsPlayerTurn(true);
            Pair<Double, Move> fromMaxPlayer = maxValue(nextState, alpha, beta, depth+1);
            if (fromMaxPlayer.getKey() < value.getKey()) {
                value = new Pair<>(fromMaxPlayer.getKey(), move);
            }
            System.out.println("minValue value: " + value.getKey());
            System.out.println("minValue move: " + value.getValue().getFrom() + " -> " + value.getValue().getTo());

            if (value.getKey() <= alpha) return value;
            beta = Math.min(beta, value.getKey());

            if (Mode.SINGLE.equals(state.getMode()) && 0.95 * state.getTimeRemaining() < getUseTime()) {
                return value;
            } else if (Mode.GAME.equals(state.getMode())) {
                // TODO
                if (getUseTime() >= 0.10 * state.getTimeRemaining()) return value;
            }
        }
        return value;
    }

    public Double utility(GameState gameState) {
        if (!canOpponentContinue(gameState) || gameState.getOpponentCheckerSize() == 0) {
            // Player wins
            return 1000000.0;
        } else if (!canPlayerContinue(gameState) || gameState.getPlayerCheckerSize() == 0) {
            // Opponent wins
            return -1000000.0;
        }
        return 0.0;
    }

    public double evaluation(GameState gameState) {
        // wf: man, king, back row, middle box (row 3,4 col 2,3,4,5), middle rows (row 3,4), next be captured, safe
        double[] wf = new double[] {5,7,4,2,0.5,-3,3};
        int[] whiteValue = new int[7];
        int[] blackValue = new int[7];
        int whiteBackRow = 7;
        int blackBackRow = 0;
        Set<String> blackKing = Player.BLACK.equals(gameState.getPlayer()) ? gameState.getPlayerKingPosition() : gameState.getOpponentKingPosition();
        Set<String> whiteKing = Player.WHITE.equals(gameState.getPlayer()) ? gameState.getPlayerKingPosition() : gameState.getOpponentKingPosition();
        Set<String> blackMan = Player.BLACK.equals(gameState.getPlayer()) ? gameState.getPlayerManPosition() : gameState.getOpponentManPosition();
        Set<String> whiteMan = Player.WHITE.equals(gameState.getPlayer()) ? gameState.getPlayerManPosition() : gameState.getOpponentManPosition();
        Set<String> blackPieces = Player.BLACK.equals(gameState.getPlayer()) ? gameState.getPlayerPosition() : gameState.getOpponentPosition();
        Set<String> whitePieces = Player.WHITE.equals(gameState.getPlayer()) ? gameState.getPlayerPosition() : gameState.getOpponentPosition();

        String[][] board = gameState.getBoard();
        whiteValue[0] = whiteMan.size();
        whiteValue[1] = whiteKing.size();
        for(String whiteChecker: whitePieces) {
            int[] position = Utility.getPosition(whiteChecker);
            int row = position[1];
            int col = position[0];

            if (row == 0 || row == 7 || col == 0 || col == 7) {
                whiteValue[6]++;
            }

            if (row == whiteBackRow && !isKingChecker(board[row][col])) {
                whiteValue[2]++;
            }

            if (row == 3 || row == 4) {
                if (col >= 2 && col <= 5) {
                    whiteValue[3]++;
                } else {
                    whiteValue[4]++;
                }
            }

            if (row > 0 && row < 7 && col > 0 && col < 7) {
                if (canBeCaptured(board, row, col, Player.WHITE)) {
                    whiteValue[5]++;
                } else if (isProtected(board, row, col, Player.WHITE)) {
                    whiteValue[6]++;
                }
            }
        }

        System.out.println("***** evaluation *****");
        gameState.printBoard();
        System.out.println("******************");
        System.out.println("Evaluation white");
        System.out.println("man: " + whiteValue[0]);
        System.out.println("king: " + whiteValue[1]);
        System.out.println("back row: " + whiteValue[2]);
        System.out.println("middle box: " + whiteValue[3]);
        System.out.println("middle not box: " + whiteValue[4]);
        System.out.println("be captured next: " + whiteValue[5]);
        System.out.println("safe: " + whiteValue[6]);


        blackValue[0] = blackMan.size();
        blackValue[1] = blackKing.size();
        for(String blackChecker: blackPieces) {
            int[] position = Utility.getPosition(blackChecker);
            int row = position[1];
            int col = position[0];

            if (row == 0 || row == 7 || col == 0 || col == 7) {
                blackValue[6]++;
            }

            if (row == blackBackRow && !isKingChecker(board[row][col])) {
                blackValue[2]++;
            }

            if (row == 3 || row == 4) {
                if (col >= 2 && col <= 5) {
                    blackValue[3]++;
                } else {
                    blackValue[4]++;
                }
            }

            if (row > 0 && row < 7 && col > 0 && col < 7) {
                if (canBeCaptured(board, row, col, Player.BLACK)) {
                    blackValue[5]++;
                } else if (isProtected(board, row, col, Player.BLACK)) {
                    blackValue[6]++;
                }
            }
        }

        System.out.println("******************");
        System.out.println("Evaluation black");
        System.out.println("man: " + blackValue[0]);
        System.out.println("king: " + blackValue[1]);
        System.out.println("back row: " + blackValue[2]);
        System.out.println("middle box: " + blackValue[3]);
        System.out.println("middle not box: " + blackValue[4]);
        System.out.println("be captured next: " + blackValue[5]);
        System.out.println("safe: " + blackValue[6]);
        System.out.println("******************");

        double value = 0;
        for (int i=0; i<whiteValue.length; i++) {
            if (Player.BLACK.equals(gameState.getPlayer())) {
                value += wf[i] * (blackValue[i] - whiteValue[i]);
            } else {
                value += wf[i] * (whiteValue[i] - blackValue[i]);
            }
        }
        return value;
    }

    private boolean isProtected(String[][] board, int row, int col, Player player) {
        // check tandem form: always one piece behind other
        if (player.equals(Player.WHITE)) {
            if (isKingChecker(board[row][col])) {
                return (!isBlank(board, row-1, col-1) && board[row-1][col-1].equalsIgnoreCase("w")) ||
                        (!isBlank(board, row+1, col-1) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                        (!isBlank(board, row-1, col+1) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                        (!isBlank(board, row+1, col+1) && board[row+1][col+1].equalsIgnoreCase("w"));
            } else {
                return (!isBlank(board, row+1, col-1) && board[row+1][col-1].equalsIgnoreCase("w")) ||
                        (!isBlank(board, row+1, col+1) && board[row+1][col+1].equalsIgnoreCase("w"));
            }
        } else {
            if (isKingChecker(board[row][col])) {
                return (!isBlank(board, row-1, col-1) && board[row-1][col-1].equalsIgnoreCase("b")) ||
                        (!isBlank(board, row+1, col-1) && board[row+1][col-1].equalsIgnoreCase("b")) ||
                        (!isBlank(board, row-1, col+1) && board[row+1][col-1].equalsIgnoreCase("b")) ||
                        (!isBlank(board, row+1, col+1) && board[row+1][col+1].equalsIgnoreCase("b"));
            } else {
                return (!isBlank(board, row-1, col-1) && board[row-1][col-1].equalsIgnoreCase("b")) ||
                        (!isBlank(board, row-1, col+1) && board[row-1][col+1].equalsIgnoreCase("b"));
            }
        }
    }

    private boolean canBeCaptured(String[][] board, int row, int col, Player player) {
        if (player.equals(Player.WHITE)) {
            return (!isBlank(board, row-1, col-1) && board[row-1][col-1].equalsIgnoreCase("b") && isBlank(board, row+1, col+1)) ||
                    (!isBlank(board, row-1, col+1) && board[row-1][col+1].equalsIgnoreCase("b") && isBlank(board, row+1, col-1)) ||
                    (!isBlank(board, row+1, col+1) && board[row+1][col+1].equals("B") && isBlank(board, row-1, col-1)) ||
                    (!isBlank(board, row+1, col-1) && board[row+1][col-1].equals("B") && isBlank(board, row-1, col+1));

        } else {
            return (!isBlank(board, row+1, col-1) && board[row+1][col-1].equalsIgnoreCase("w") && isBlank(board, row-1, col+1)) ||
                    (!isBlank(board, row+1, col+1) && board[row+1][col+1].equalsIgnoreCase("w") && isBlank(board, row-1, col-1)) ||
                    (!isBlank(board, row-1, col-1) && board[row-1][col-1].equals("W") && isBlank(board, row+1, col+1)) ||
                    (!isBlank(board, row-1, col+1) && board[row-1][col+1].equals("W") && isBlank(board, row+1, col-1));
        }
    }

    private boolean isBlank(String[][] board, int row, int col) {
        return board[row][col].equals(".");
    }

    public Integer evaluation_1(GameState gameState) {
        // TODO rewrite evaluation function
        int kingWeight = 30;
        int manWeight = 1;

        Integer blackValue = 0;
        Integer whiteValue = 0;

        Set<String> blackKing = Player.BLACK.equals(gameState.getPlayer()) ? gameState.getPlayerKingPosition() : gameState.getOpponentKingPosition();
        Set<String> whiteKing = Player.WHITE.equals(gameState.getPlayer()) ? gameState.getPlayerKingPosition() : gameState.getOpponentKingPosition();
        Set<String> blackMan = Player.BLACK.equals(gameState.getPlayer()) ? gameState.getPlayerManPosition() : gameState.getOpponentManPosition();
        Set<String> whiteMan = Player.WHITE.equals(gameState.getPlayer()) ? gameState.getPlayerManPosition() : gameState.getOpponentManPosition();


        whiteValue += whiteKing.size() * kingWeight;
        // Advance man are more value than man in the back of board
        for(String whiteChecker: whiteMan) {
            int[] position = Utility.getPosition(whiteChecker);
            if (position[0] == 0 || position[0] == 7) {
                // At border of board, cannot be captured
                manWeight = 3;
            } else {
                manWeight = 1;
            }
            whiteValue += (manWeight * (7-position[1]));
        }

        System.out.println("whiteValue " + whiteValue);

        // Calculate distance between all white kings and all black pieces (man+king)
        // The distance should be small if we want to attack
        if (whiteKing.size() > blackKing.size()) {
            int whiteDist = calculateDistance(whiteKing, blackKing, blackMan);
            whiteDist = whiteDist / whiteKing.size();
            whiteValue += whiteDist;
            System.out.println("whiteDist " + whiteDist);
        }


        blackValue += blackKing.size() * kingWeight;
        for(String blackChecker: blackMan) {
            int[] position = Utility.getPosition(blackChecker);
            if (position[0] == 0 || position[0] == 7) {
                // At border of board, cannot be captured
                manWeight = 3;
            } else {
                manWeight = 1;
            }
            blackValue += (manWeight * position[1]);
        }
        System.out.println("blackValue " + blackValue);

        // Calculate distance between all black kings and all white pieces (man+king)
        if (blackKing.size() > whiteKing.size()) {
            int blackDist = calculateDistance(blackKing, whiteKing, whiteMan);
            blackDist = blackDist / blackKing.size();
            blackValue += blackDist;
            System.out.println("blackDist " + blackDist);
        }

        System.out.println("evaluation: black " + blackValue + " white " + whiteValue);

        if (Player.BLACK.equals(gameState.getPlayer())) {
            return blackValue - whiteValue;
        } else {
            return whiteValue - blackValue;
        }
    }

    private Integer calculateDistance(Set<String> playerKing, Set<String> opponentKing, Set<String> opponentMan) {
        int dist = 0;
        for(String playerChecker: playerKing) {
            int[] player = Utility.getPosition(playerChecker);
            for(String opponentChecker: opponentMan) {
                int[] opponent = Utility.getPosition(opponentChecker);
                dist += (7 - distance(player[0], opponent[0])) + (7 - distance(player[1], opponent[1]));
            }
            for(String opponentChecker: opponentMan) {
                int[] opponent = Utility.getPosition(opponentChecker);
                dist +=  (7 - distance(player[0], opponent[0])) + (7 - distance(player[1], opponent[1]));
            }
        }

        return dist;
    }

    private Integer distance(int player, int opponent) {
        return Math.abs(player - opponent);
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
                nextMove.setCapture(prevMove.getCapture() + 1);
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
        boolean isKing = isKingChecker(board[oldRow][oldCol]);
        boolean isNewKingCrowned = isMoveToKingArea && !isKing;

        if (isNewKingCrowned) {
            // Man moves to king area and becomes king
            //System.out.println("applyNewMove move to king " + move.getFrom() + " " + move.getTo() );
            board[newRow][newCol] = board[oldRow][oldCol].toUpperCase();
            if (currentPlayer.equals(gameState.getPlayer())) {
                gameState.addPlayerKingPosition(move.getTo());
                gameState.removePlayerManPosition(move.getFrom());
            } else {
                gameState.addOpponentKingPosition(move.getTo());
                gameState.removeOpponentManPosition(move.getFrom());
            }
        } else if (isKing) {
            // King moves
            //System.out.println("applyNewMove king move " + move.getFrom() + " " + move.getTo() );
            board[newRow][newCol] = board[oldRow][oldCol].toUpperCase();
            if (currentPlayer.equals(gameState.getPlayer())) {
                gameState.addPlayerKingPosition(move.getTo());
                gameState.removePlayerKingPosition(move.getFrom());
            } else {
                gameState.addOpponentKingPosition(move.getTo());
                gameState.removeOpponentKingPosition(move.getFrom());
            }
        } else {
            // Man moves
            //System.out.println("applyNewMove man move " + move.getFrom() + " " + move.getTo() );
            board[newRow][newCol] = board[oldRow][oldCol];
            if (currentPlayer.equals(gameState.getPlayer())) {
                gameState.addPlayerManPosition(move.getTo());
                gameState.removePlayerManPosition(move.getFrom());
            } else {
                gameState.addOpponentManPosition(move.getTo());
                gameState.removeOpponentManPosition(move.getFrom());
            }
        }
        board[oldRow][oldCol] = ".";
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
        //System.out.println("applyJumpMoves from " + oldCol + " " + oldRow + " to " + newCol + " " + newRow);
        //System.out.println("applyJumpMoves from " + move.getFrom() + " to " + move.getTo());

        applyNewMove(gameState, move);

        // Handle capture
        int[] capturedPosition = new int[] { (oldCol+newCol) / 2 , (oldRow+newRow) / 2}; // (x,y)
        String capturedLabel = Utility.getLabel(capturedPosition[1], capturedPosition[0]);
        if (gameState.getOpponent().equals(currentPlayer)) {
            gameState.removePlayerKingPosition(capturedLabel);
            gameState.removePlayerManPosition(capturedLabel);
        } else {
            gameState.removeOpponentKingPosition(capturedLabel);
            gameState.removeOpponentManPosition(capturedLabel);
        }
        board[capturedPosition[1]][capturedPosition[0]] = ".";
    }

    public void applyMove(GameState gameState, Move move) {
        //System.out.println("applyMove " + move.getMoveType() + " from " + move.getFrom() + " to " + move.getTo());
        if (MoveType.ONE.equals(move.getMoveType())) {
            applyNewMove(gameState, move);
        } else if (MoveType.JUMP.equals(move.getMoveType())) {
            applyJumpMoves(gameState, move);
        }
    }

    public PriorityQueue<Move> getAllPossibleMoves(GameState gameState) {
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

        // Prioritize regular moves by index in play history
        PriorityQueue<Move> regularMoves = new PriorityQueue<Move>((a, b) -> {
            // Move from single corner first
            String aTo = a.getFrom();
            String bTo = b.getFrom();

            return Player.BLACK.equals(currentPlayer) ? bTo.compareTo(aTo) : aTo.compareTo(bTo);

// consider play history
//            if (a.getSeenIndex(playHistory) == b.getSeenIndex(playHistory)) {
//                return 1;
//            } else {
//                // return position that pass earlier
//                return a.getSeenIndex(playHistory).compareTo(b.getSeenIndex(playHistory));
//            }
        });

        // Prioritize jump moves by number of captured checkers
        PriorityQueue<Move> captureMoves = new PriorityQueue<>((a, b) -> b.getCapture().compareTo(a.getCapture()));

        GameState cloneState;

        // Get all possible moves of man checkers
        for(String checker: manCheckerPosition) {
            System.out.println(checker);
            int[] currentPostion = Utility.getPosition(checker);
            int currentRow = currentPostion[1];
            int currentCol = currentPostion[0];
            cloneState = GameState.newInstance(gameState);

            //System.out.println("checker " + checker + " " + cloneState.getBoard()[currentRow][currentCol]);

            List<Move> jumps = getPossibleJump(cloneState, currentRow, currentCol, captureDirs);
            System.out.println("getAllPossibleMoves | jump size " + jumps.size());

            if (jumps.isEmpty()) {
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
                captureMoves.addAll(jumps);
            }
        }

        // Get all possible moves of king checkers
        for(String checker: kingCheckerPosition) {
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
                captureMoves.addAll(jumps);
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        } else {
            System.out.println("All possible moves order");
            for (Move m: regularMoves) {
                System.out.println(m.getFrom() + " -> " + m.getTo());
            }
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
            if (!isKing && isMoveDown(oldRow, row) && Math.abs(row-oldRow) == 1 && Math.abs(col - oldCol) == 1) {
                return true;
            } else if (isKing && Math.abs(oldRow-row) == 2) {
                // King captures
                if (isMoveUp(oldRow, row)) {
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
        if (gameState.getPlayerCheckerSize() == 0) {
            return false;
        }

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
        return false;
    }

    public boolean canOpponentContinue(GameState gameState) {
        if (gameState.getOpponentCheckerSize() == 0) {
            return false;
        }

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
        return false;
    }

    public boolean isTerminal(GameState gameState) {
        // One player loses all checkers
        if (gameState.getPlayerCheckerSize() == 0 || gameState.getOpponentCheckerSize() == 0) {
            return true;
        }

        // No possible move
        if (gameState.isPlayerTurn() && !canPlayerContinue(gameState)) {
            return true;
        }

        if (!gameState.isPlayerTurn() && !canOpponentContinue(gameState)) {
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
    private double timeRemaining;
    private Set<String> playerManPosition;
    private Set<String> playerKingPosition;
    private Set<String> playerPosition;
    private Set<String> opponentManPosition;
    private Set<String> opponentKingPosition;
    private Set<String> opponentPosition;
    private boolean isPlayerTurn;

    public GameState(String[][] board, Player player, Mode mode, double timeRemaining, boolean isPlayerTurn) {
        this.board = board;
        this.player = player;
        this.opponent = Player.WHITE.equals(player) ? Player.BLACK : Player.WHITE;
        this.mode = mode;
        this.timeRemaining = timeRemaining;
        playerManPosition = new HashSet<>();
        playerKingPosition = new HashSet<>();
        playerPosition = new HashSet<>();
        opponentManPosition = new HashSet<>();
        opponentKingPosition = new HashSet<>();
        opponentPosition = new HashSet<>();
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
        this.playerPosition = new HashSet<>(gameState.playerPosition);
        this.opponentManPosition = new HashSet<>(gameState.opponentManPosition);
        this.opponentKingPosition = new HashSet<>(gameState.opponentKingPosition);
        this.opponentPosition = new HashSet<>(gameState.opponentPosition);
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

        playerPosition.addAll(playerManPosition);
        playerPosition.addAll(playerKingPosition);
        opponentPosition.addAll(opponentManPosition);
        opponentPosition.addAll(opponentKingPosition);
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

    public double getTimeRemaining() {
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

    public Set<String> getPlayerPosition() {
        return playerPosition;
    }

    public Set<String> getOpponentPosition() {
        return opponentPosition;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public void addPlayerManPosition(String label) {
        playerManPosition.add(label);
        playerPosition.add(label);
    }

    public void removePlayerManPosition(String label) {
        playerManPosition.remove(label);
        playerPosition.remove(label);
    }

    public void addPlayerKingPosition(String label) {
        playerKingPosition.add(label);
        playerPosition.add(label);
    }

    public void removePlayerKingPosition(String label) {
        playerKingPosition.remove(label);
        playerPosition.remove(label);
    }

    public void addOpponentManPosition(String label) {
        opponentManPosition.add(label);
        opponentPosition.add(label);
    }

    public void removeOpponentManPosition(String label) {
        opponentManPosition.remove(label);
        opponentPosition.remove(label);
    }

    public void addOpponentKingPosition(String label) {
        opponentKingPosition.add(label);
        opponentPosition.add(label);
    }

    public void removeOpponentKingPosition(String label) {
        opponentKingPosition.remove(label);
        opponentPosition.remove(label);
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

class Move {
    private MoveType moveType;
    private String from;
    private String to;
    private String fromChecker;
    private String toChecker;
    private GameState state;
    private Move move;
    private Integer capture;

    public Move(MoveType moveType, String from, String to, Move move, String fromChecker, String toChecker, GameState state) {
        this.moveType = moveType;
        this.from = from;
        this.to = to;
        this.move = move;
        this.fromChecker = fromChecker;
        this.toChecker = toChecker;
        this.state = state;
        this.capture = 0;
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

    public String getToChecker() {
        return toChecker;
    }

    public void setToChecker(String toChecker) {
        this.toChecker = toChecker;
    }

    public Integer getSeenIndex(Map<String, Integer> playerHistory) {
        String move = this.getFrom() + " " + this.getTo();
        String reverseMove = this.getTo() + " " + this.getFrom();

        if (playerHistory.containsKey(move)) {
            return playerHistory.get(move);
        }
        if (playerHistory.containsKey(reverseMove)) {
            return playerHistory.get(reverseMove);
        }
        return 0;
    }

    public Integer getCapture() {
        return capture;
    }

    public void setCapture(Integer capture) {
        this.capture = capture;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public List<String> print() {
        Stack<Move> stack = new Stack<>();
        Move m = this;
        while(m != null) {
            stack.push(m);
            m = m.getMove();
        }

        List<String> results = new ArrayList<>();
        while(!stack.isEmpty()) {
            m = stack.pop();
            if (m.getFrom() == null) continue;
            results.add(m.getMoveType().getName() + " " + m.getFrom() + " " + m.getTo());
            System.out.println(m.getMoveType().getName() + " " + m.getFrom() + " " + m.getTo());
        }
        return results;
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