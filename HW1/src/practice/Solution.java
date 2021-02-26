package practice;

/**
 * Created by nuning on 2/5/21.
 */
public class Solution {
    private static Node root;
    public static void main(String[] args) {
        initializeTree();
        Minimax minimax = new Minimax();
        minimax.alphaBetaSearch(root);
    }

    private static void initializeTree() {
        String[] array = {"A", "B", "Q", "C", "J", "R", "Z", "D", "G", "K", "N", "S", "V", "Z1", "Z4", "E", "F", "H", "I", "L", "M", "O", "P", "T", "U", "W", "X", "Z2", "Z3", "Z5", "Z6"};
        root = createTree(array, 0, null);
    }

    private static Node createTree(String[] array, int index, Node parent) {
        int left = 2*index + 1;
        int right = 2*index + 2;

        //System.out.println(left + " " + right);

        Node node = new Node(array[index]);
        node.setParent(parent);
        if (left >= array.length || right >= array.length) {
            return node;
        }
        Node leftNode = createTree(array, left, node);
        node.setLeft(leftNode);
        Node rightNode = createTree(array, right, node);
        node.setRight(rightNode);
//        if (leftNode != null && rightNode != null ) {
//            System.out.println(node.getName() + " L: " + leftNode.getName() + " R: " + rightNode.getName());
//        } else {
//            System.out.println(node.getName() + " L: null R: null");
//        }
        return node;
    }
}
