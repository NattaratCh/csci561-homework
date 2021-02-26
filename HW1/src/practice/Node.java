package practice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nuning on 2/5/21.
 */
public class Node {
    private String name;
    private Node parent;
    private Node left;
    private Node right;
    private List<Node> children;

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public Node getRight() {
        return right;
    }

    public void setRight(Node right) {
        this.right = right;
    }

    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(left);
        children.add(right);
        return children;
    }
}
