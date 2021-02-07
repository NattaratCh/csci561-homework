package practice;

import java.util.*;

/**
 * Created by nuning on 2/5/21.
 */
public class Minimax {
    private static final String[][] UTILITY_LIST =  {{"E", "10"}, {"F", "11"}, {"H", "9"}, {"I", "12"}, {"L", "12"}, {"M", "15"}, {"O", "13"}, {"P", "14"},
            {"T", "15"}, {"U", "2"}, {"W", "4"}, {"X", "1"}, {"Z2", "3"}, {"Z3", "22"}, {"Z5", "24"}, {"Z6", "25"}};
    public static Map<String, Integer> map;

    public Minimax() {
        initializeUtility();
    }

    public void initializeUtility() {
        map = new HashMap<>();
        for(String[] u: UTILITY_LIST) {
            map.put(u[0], Integer.valueOf(u[1]));
        }
    }

    public Integer alphaBetaSearch(Node node) {
        Integer v = maxValue(node, Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println("alphaBetaSearch v:" + v);
        return v;
    }

    public int maxValue(Node node, Integer alpha, Integer beta) {
        System.out.println("MAX : " + node.getName() + ", " + alpha + ", " + beta);
        if (terminalTest(node)) return utility(node);
        Integer v = Integer.MIN_VALUE;
        for(Node child: node.getChildren()) {
            v = Math.max(v, minValue(child, alpha, beta));
            if (v >= beta) return v;
            alpha = Math.max(v, alpha);
            System.out.println("MAX: " + node.getName() + ", " + alpha + ", " + beta);
        }
        return v;
    }

    public int minValue(Node node, Integer alpha, Integer beta) {
        System.out.println("MIN: " + node.getName() + ", " + alpha + ", " + beta);
        if (terminalTest(node)) return utility(node);
        Integer v = Integer.MAX_VALUE;
        for(Node child: node.getChildren()) {
            v = Math.min(v, maxValue(child, alpha, beta));
            if (v <= alpha) return v;
            beta = Math.min(v, beta);
            System.out.println("MIN: " + node.getName() + ", " + alpha + ", " + beta);
        }
        return v;
    }

    public boolean terminalTest(Node node) {
        return map.containsKey(node.getName());
    }

    public int utility(Node node) {
        return map.get(node.getName());
    }
}