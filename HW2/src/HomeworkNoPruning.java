import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by nuning on 2/25/21.
 */

public class HomeworkNoPruning {
    public static void main(String[] args) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long startTime = threadMXBean.getCurrentThreadCpuTime() + threadMXBean.getCurrentThreadUserTime();
        System.out.println("startTime " + startTime);
        CheckerGameNoPruning checkerGame = new CheckerGameNoPruning(threadMXBean, startTime);
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

class CheckerGameNoPruning {
    private ThreadMXBean threadMXBean;
    private long startTime;
    private Set<String> playHistory;
    private Move bestMove;
    private Integer bestValue;
    private Integer depthLimit;
    private int[][] kingRegularDirs = new int[][]{{1, -1}, {-1, -1}, {-1,1}, {1,1}};;
    private int[][] kingCaptureDirs = new int[][]{{2, -2}, {-2, -2}, {-2,2}, {2,2}};

    public CheckerGameNoPruning(ThreadMXBean threadMXBean, long startTime) {
        this.depthLimit = 1;
        this.startTime = startTime;
        this.threadMXBean = threadMXBean;
        playHistory = new HashSet<>();
    }

    public void start() {

        GameState gameState = readInput("./src/input.txt");
        setPlayHistory();
        // TODO change input path
        // GameState gameState = readInput("./test-cases/input8.txt");
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
            double timeRemaining = Double.valueOf(line);

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

    public void setPlayHistory() {
        File file = new File("./opponent/playdata.txt");
        if (!file.exists()) return;

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                playHistory.add(line.trim());
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

    public double getGameRemainingTime(GameState state) {
        return state.getTimeRemaining() - getUseTime();
    }

    public void nextMove(GameState gameState) {
        minimax(gameState);
        System.out.println("============= next move pruning ============");
        gameState.printState();
        writeBoard(gameState);
        writeOutput();
        writePlayData();
    }

    private void writeBoard(GameState gameState) {
        File file = new File("./opponent/result_agent.txt");

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

    public void minimax(GameState gameState) {
        if (Mode.GAME.equals(gameState.getMode())) {
            if (gameState.getTimeRemaining() < 1.0) {
                depthLimit = 1;
            } else if (gameState.getTimeRemaining() < 50) {
                depthLimit = 2;
            } else {
                depthLimit = 5;
            }
        }

        if (gameState.isInitialState()) {
            System.out.println("Initial state");
            depthLimit = 1;
        }


        System.out.println("minimax depth limit: " + depthLimit);
        Integer value = maxValue(gameState, 0);
        System.out.println("value: " + value);
    }

    private void writeOutput() {
        if (bestMove == null) {
            System.out.println("No pruning Agent: Cannot move");
            return;
        }
        List<String> results = bestMove.print();

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

    private void writePlayData() {
        if (bestMove == null) return;
        File file = new File("./opponent/playdata.txt");
        FileWriter fr = null;
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
    public Integer maxValue(GameState state, Integer depth) {
        System.out.println("#############################################################");
        System.out.println("maxValue depth " + depth);
        if (isTerminal(state)) {
            return utility(state);
        }

        if (depth == depthLimit) {
            return evaluation(state);
        }

        Integer value = Integer.MIN_VALUE;
        Integer next = null;
        List<Move> possibleMoves = getAllPossibleMoves(state);
        for (Move move: possibleMoves) {

            GameState nextState = move.getState();
            nextState.printBoard();
            System.out.println("opponent moves");
            nextState.setIsPlayerTurn(false);
            next = minValue(nextState, depth+1);

            System.out.println("moves " + move.getFrom() + " -> " + move.getTo());
            System.out.println("maxValue value: " + value);
            System.out.println("maxValue next: " + next);
            System.out.println("----------------------------");

            if (next > value) {
                System.out.println("maxValue next > value bestMove " + bestMove);
                value = next;
                if (depth == 0) {
                    //if ((bestMove == null && bestValue == null) || next > bestValue) {
                    bestMove = move;
                    bestValue = value;
                    System.out.println("## best move: " + move.getFrom() + " => " + move.getTo());
                    System.out.println("## best value: " + bestValue);
                    //}
                }
            }

            if (Mode.SINGLE.equals(state.getMode()) && 0.95 * state.getTimeRemaining() < getUseTime()) {
                return value;
            } else if (Mode.GAME.equals(state.getMode())) {
                // TODO
            }
        }
        return value;
    }

    // Opponent
    public Integer minValue(GameState state, Integer depth) {
        System.out.println("#############################################################");
        System.out.println("minValue depth " + depth);
        if (isTerminal(state)) {
            return utility(state);
        }

        if (depth == depthLimit) {
            int eval = evaluation(state);
            return eval;
        }

        Integer value = Integer.MAX_VALUE;
        List<Move> possibleMoves = getAllPossibleMoves(state);
        for (Move move: possibleMoves) {
            GameState nextState = move.getState();
            nextState.printBoard();
            nextState.setIsPlayerTurn(true);
            value = Math.min(value, maxValue(nextState, depth+1));
            System.out.println("minValue value: " + value);
            System.out.println("----------------------------");

            if (Mode.SINGLE.equals(state.getMode()) && 0.95 * state.getTimeRemaining() < getUseTime()) {
                return value;
            } else if (Mode.GAME.equals(state.getMode())) {
                // TODO
            }
        }
        return value;
    }

    public Integer utility(GameState gameState) {
        if (!canOpponentContinue(gameState) || gameState.getOpponentCheckerSize() == 0) {
            // Player wins
            return 1000000;
        } else if (!canPlayerContinue(gameState) || gameState.getPlayerCheckerSize() == 0) {
            // Opponent wins
            return -1000000;
        }
        return 0;
    }

    public Integer evaluation(GameState gameState) {
        // TODO rewrite evaluation function
        int kingWeight = 20;
        int manWeight = 5;

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
            whiteValue += (manWeight + (7-position[1]));
        }

        // Calculate distance between all white kings and all black pieces (man+king)
        // The distance should be small if we want to attack
        if (whiteKing.size() > blackKing.size()) {
            int whiteDist = calculateDistance(whiteKing, blackKing, blackMan);
            whiteDist = whiteDist / whiteKing.size();
            whiteValue += whiteDist;
        }


        blackValue += blackKing.size() * kingWeight;
        for(String blackChecker: blackMan) {
            int[] position = Utility.getPosition(blackChecker);
            blackValue += (manWeight + position[1]);
        }

        // Calculate distance between all black kings and all white pieces (man+king)
        if (blackKing.size() > whiteKing.size()) {
            int blackDist = calculateDistance(blackKing, whiteKing, whiteMan);
            blackDist = blackDist / blackKing.size();
            blackValue += blackDist;
        }

        System.out.println("evaluation: black " + blackValue + " white " + whiteValue);

        if (Player.BLACK.equals(gameState.getPlayer())) {
            return blackValue - whiteValue;
        } else {
            return whiteValue - blackValue;
        }

//        Set<String> opponentKing = gameState.getOpponentKingPosition();
//        Set<String> playerKing = gameState.getPlayerKingPosition();
//        int playerCheckerSize = gameState.getPlayerCheckerSize();
//        int opponentCheckerSize = gameState.getPlayerCheckerSize();
//        if (opponentKing.isEmpty()) {
//            return (playerCheckerSize - opponentCheckerSize) * 50 + gameState.getPlayerSafeCheckers() * 10 + playerKing.size() * 20 + gameState.getPlayerKingAreaChecker() + gameState.getPlayerManPosition().size();
//        } else {
//            return (playerCheckerSize - opponentCheckerSize) * 50 + playerKing.size() * 20 + gameState.getPlayerKingAreaChecker() + gameState.getPlayerManPosition().size();
//        }
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
            System.out.println("applyNewMove move to king " + move.getFrom() + " " + move.getTo() );
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
            System.out.println("applyNewMove king move " + move.getFrom() + " " + move.getTo() );
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
            System.out.println("applyNewMove man move " + move.getFrom() + " " + move.getTo() );
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

        //System.out.println("canPlayerContinue false");
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
        //System.out.println("canOpponentContinue false");
        return false;
    }

    public boolean isTerminal(GameState gameState) {
        // One player loses all checkers
        if (gameState.getPlayerCheckerSize() == 0 || gameState.getOpponentCheckerSize() == 0) {
            return true;
        }

        // No possible move
        if (!canOpponentContinue(gameState) || !canPlayerContinue(gameState)) {
            return true;
        }

        return false;
    }
}