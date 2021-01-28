import java.io.*;
import java.util.*;

public class Homework {
    public static void main(String[] args) {
        Solution solution = new Solution();
        solution.solve();
    }
}

class Solution {
    private int[][] directions = { {0,-1}, {1,-1}, {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}};

    public Solution() {}

    public void solve() {
        SearchModel model = null;
        try {
             model = getInput();
            System.out.println(model.getMethod());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (model != null) {
            int n = model.getNumberOfSettlings();
            String[] results = new String[n];
            for (int i=0; i<n; i++) {
                if ("BFS".equalsIgnoreCase(model.getMethod())) {
                    List<Integer[]> path = bfs(model.getMap(), model.getStartPosition(), model.getSettlingsPosition()[i], model.getMaxRockHeight());
                    results[i] = getResult(path);
                } else if ("UCS".equalsIgnoreCase(model.getMethod())) {
                    List<Integer[]> path = ucs(model.getMap(), model.getStartPosition(), model.getSettlingsPosition()[i], model.getMaxRockHeight());
                    results[i] = getResult(path);
                }
            }


            writeOutput(results);
        }
    }

    public void writeOutput(String[] results) {
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

    public String getResult(List<Integer[]> path) {
        if (path == null) {
            System.out.println("FAIL");
            return "FAIL";
        }

        StringBuilder sb = new StringBuilder();
        for(Integer[] p: path) {
            System.out.print(p[0]+ "," + p[1] + " ");
            sb.append(p[0]+ "," + p[1] + " ");
        }
        System.out.println();
        return sb.toString().trim();
    }

    public List<Integer[]> bfs(Integer[][] map, Integer[] startPosition, Integer[] settlingPostition, Integer maxRockHeight) {
        if (map.length == 0 || map[0].length == 0) return null;

        System.out.println("goal position: " + settlingPostition[0] + " " + settlingPostition[1]);
        Queue<Node> queue = new LinkedList<>(); // keep paths
        Integer width = map[0].length;
        Integer height = map.length;
        Set<String> visited = new HashSet<>();

        Node startNode = new Node(null, startPosition, 0);
        queue.add(startNode);

        while(!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Integer[] pos = currentNode.getPosition(); // get last position
            System.out.println("current position: " + pos[0] + " " + pos[1]);
            if (visited.contains(pos[0] + "_" + pos[1])) continue;
            visited.add(pos[0] + "_" + pos[1]);

            //System.out.println("current position: " + pos[0] + " " + pos[1]);
            if (pos[0] == settlingPostition[0] && pos[1] == settlingPostition[1]) {
                return getPath(currentNode);
            }

            List<Node> children = expandChildren(currentNode, width, height, map, maxRockHeight);
            for(Node child: children) {
                queue.add(child);
            }
        }
        return null;
    }

    public List<Integer[]> ucs(Integer[][] map, Integer[] startPosition, Integer[] settlingPostition, Integer maxRockHeight) {
        if (map.length == 0 || map[0].length == 0) return null;

        System.out.println("goal position: " + settlingPostition[0] + " " + settlingPostition[1]);
        PriorityQueue<Node> pq = new PriorityQueue(new NodeComparator());
        Node startNode = new Node(null, startPosition, 0);
        pq.add(startNode);

        Map<String, Node> openNodeMap = new HashMap<>();
        openNodeMap.put(startPosition[0] + "_" + startPosition[1], startNode);

        Map<String, Node> visited = new HashMap<>();
        Integer width = map[0].length;
        Integer height = map.length;

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            Integer[] currentPos = currentNode.getPosition();
            System.out.println("current position: " + currentPos[0] + " " + currentPos[1]);
            if (currentPos[0] == settlingPostition[0] && currentPos[1] == settlingPostition[1]) {
                return getPath(currentNode);
            }

            List<Node> children = expandChildren(currentNode, width, height, map, maxRockHeight);
            for(Node child: children) {
                String childKey = child.getPosition()[0] + "_" + child.getPosition()[1];
                if (!openNodeMap.containsKey(childKey) && !visited.containsKey(childKey)) {
                    pq.add(child);
                    openNodeMap.put(childKey, child);
                } else if (openNodeMap.containsKey(childKey)) {
                    Node node = openNodeMap.get(childKey);
                    if (child.getPathCost() < node.getPathCost()) {
                        pq.remove(node);
                        pq.add(child);
                        openNodeMap.put(childKey, child);
                    }
                } else if (visited.containsKey(childKey)) {
                    Node node = visited.get(childKey);
                    if (child.getPathCost() < currentNode.getPathCost()) {
                        visited.remove(node);
                        pq.add(child);
                        openNodeMap.put(childKey, child);
                    }
                }
            }

            visited.put(currentPos[0] + "_" + currentPos[1], currentNode);
        }
        return null;
    }

    private List<Integer[]> getPath(Node node) {
        List<Integer[]> path = new ArrayList<>();
        while (node != null) {
            path.add(0,node.getPosition());
            node = node.getParent();
        }
        return path;
    }

    public int getUnitPathCost(Integer[] currentPos, Integer[] nextPos) {
        int diffX = Math.abs(currentPos[0] - nextPos[0]);
        int diffY = Math.abs(currentPos[1] - nextPos[1]);
        return diffX * diffY == 1 ? 14 : 10;
    }

    public List<Node> expandChildren(Node currentNode, Integer width, Integer height, Integer[][] map, int maxRockHeight) {
        List<Node> children = new ArrayList<>();
        for(int[] dir: directions) {
            int newX = currentNode.getPosition()[0] + dir[0];
            int newY = currentNode.getPosition()[1] + dir[1];
            if (newX < 0 || newX >= width || newY < 0 || newY >= height ||
                    !passSteepCondition(map[currentNode.getPosition()[1]][currentNode.getPosition()[0]], map[newY][newX], maxRockHeight)) {
                continue;
            };
            Integer[] newPos = new Integer[]{ newX, newY};
            children.add(new Node(currentNode, newPos, currentNode.getPathCost() + getUnitPathCost(currentNode.getPosition(), newPos)));
        }
        return children;
    }

    public boolean passSteepCondition(Integer currentCellHeight, Integer nextCellHeight, Integer maxRockHeight) {
        if (currentCellHeight >= 0) currentCellHeight = 0;
        if (nextCellHeight >= 0) nextCellHeight = 0;
        return Math.abs(currentCellHeight - nextCellHeight) <= maxRockHeight;
    }

    public SearchModel getInput() throws Exception {
        // TODO edit file path
        File file = new File("./src/input.txt");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            int lineNumber = 1;
            String method = null;
            Integer[] mapSize = new Integer[2];
            Integer[] startPosition = new Integer[2];
            Integer maxRockHeight = null;
            Integer numberOfSettlingSites = null;
            Integer[][] settlingPosition = null;
            Integer[][] map = null;

            while (line != null) {
                line = line.trim();
                if (lineNumber == 1) {
                    method = line;
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 2) {
                    int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                    mapSize[0] = temp[0];
                    mapSize[1] = temp[1];
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 3) {
                    int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                    startPosition[0] = temp[0];
                    startPosition[1] = temp[1];
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 4) {
                    maxRockHeight = Integer.valueOf(line);
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 5) {
                    numberOfSettlingSites = Integer.valueOf(line);
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 6) {
                    settlingPosition = new Integer[numberOfSettlingSites][2];
                    for (int i=0; i<numberOfSettlingSites; i++) {
                        int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                        settlingPosition[i][0] = temp[0];
                        settlingPosition[i][1] = temp[1];
                        line = reader.readLine().trim();
                    }
                    lineNumber += numberOfSettlingSites;
                } else {
                    int w = mapSize[0];
                    int h = mapSize[1];
                    map = new Integer[h][w];
                    for(int y=0; y<h; y++) {
                        System.out.println(line);
                        int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                        for(int x=0; x<w; x++) {
                            map[y][x] = temp[x];
                        }
                        line = reader.readLine();
                        if (line != null) line = line.trim();
                    }

                }
            }
            reader.close();

            SearchModel model = new SearchModel(method, mapSize[0], mapSize[1], startPosition, maxRockHeight, numberOfSettlingSites, settlingPosition, map);
            return model;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new FileNotFoundException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }
}

class Node {
    private Node parent;
    private Integer[] position;
    private Integer pathCost;

    public Node(Node parent, Integer[] position, Integer pathCost) {
        this.parent = parent;
        this.position = position;
        this.pathCost = pathCost;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Integer[] getPosition() {
        return position;
    }

    public void setPosition(Integer[] position) {
        this.position = position;
    }

    public Integer getPathCost() {
        return pathCost;
    }

    public void setPathCost(Integer pathCost) {
        this.pathCost = pathCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;
        return node.position[0] == this.position[0] && node.position[1] == this.position[1];
    }
}

class NodeComparator implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) {
        return o1.getPathCost().compareTo(o2.getPathCost());
    }
}

//
//class Path {
//    //private List<Integer[]> paths;
//    private Integer[] position;
//    private Integer pathCost;
//
//    public Path(Integer[] position, Integer pathCost) {
//        this.position = position;
//        this.pathCost = pathCost;
//    }
//
//    public Integer[] getPosition() {
//        return position;
//    }
//
//
//    public Integer getPathCost() {
//        return pathCost;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Path)) return false;
//
//        Path p = (Path) o;
//        return p.position[0] == this.position[0] && p.position[1] == this.position[1];
////        if (p.paths.size() == 0 && this.paths.size() == 0) return true;
////        if (p.paths.size() == 0 || this.paths.size() == 0) return false;
////        return this.paths.get(this.paths.size()-1) == p.paths.get(p.paths.size()-1);
//    }
//}
//
//class PathComparator implements Comparator<Path> {
//
//    @Override
//    public int compare(Path o1, Path o2) {
//        return o1.getPathCost().compareTo(o2.getPathCost());
//    }
//}

class SearchModel {
    private String method;
    private Integer width;
    public  Integer height;
    private Integer[] startPosition;
    private Integer maxRockHeight;
    private Integer numberOfSettlings;
    private Integer[][] settlingsPosition;
    private Integer[][] map;

    public SearchModel(String method, Integer width, Integer height, Integer[] startPosition, Integer maxRockHeight, Integer numberOfSettlings, Integer[][] settlingsPosition, Integer[][] map) {
        this.method = method;
        this.width = width;
        this.height = height;
        this.startPosition = startPosition;
        this.maxRockHeight = maxRockHeight;
        this.numberOfSettlings = numberOfSettlings;
        this.settlingsPosition = settlingsPosition;
        this.map = map;
    }

    public String getMethod() {
        return method;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer[] getStartPosition() {
        return startPosition;
    }

    public Integer getMaxRockHeight() {
        return maxRockHeight;
    }

    public Integer getNumberOfSettlings() {
        return numberOfSettlings;
    }

    public Integer[][] getSettlingsPosition() {
        return settlingsPosition;
    }

    public Integer[][] getMap() {
        return map;
    }
}