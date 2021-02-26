import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Homework {
    public static void main(String[] args) {
        Solution solution = new Solution();
        long start = System.nanoTime();
        solution.solve("./src/input.txt");
        long end = System.nanoTime();
        long durationInMilliseconds = (end - start) / 1000000;
        System.out.println("Time executed in seconds: " + (durationInMilliseconds/1000));
    }

    /******************************
     * Submission *
     *******************************/
//    public static void main(String[] args) {
//        Solution solution = new Solution();
//        solution.solve("input.txt");
//    }

    /******************************
     * For testing *
     *******************************/
//    public static void main(String[] args) {
//        test();
//    }

    /***
     *  Testing code
     */

    private static void test() {
        for(int i=1; i<=50; i++) {
            File dest = new File("./src/input.txt");
            if (dest.exists()) {
                dest.delete();
            }

            File src = new File("./test-cases/input" + i + ".txt");
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error while copy file");
                e.printStackTrace();
            }

            Solution solution = new Solution();
            solution.solve("./src/input.txt");
            try {
                compareOutput(i, solution.searchModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void compareOutput(int caseNumber, SearchModel input) throws Exception {
        String outputFileName = "./test-cases/output" + caseNumber + ".txt";
        try {
            BufferedReader expectOutputReader = new BufferedReader(new FileReader(outputFileName));
            BufferedReader myOutputReader = new BufferedReader(new FileReader("./src/output.txt"));
            String expectLine = expectOutputReader.readLine();
            String myLine = myOutputReader.readLine();

            boolean success = true;
            int line = 1;
            while (expectLine != null && myLine != null) {
                expectLine = expectLine.trim();
                myLine = myLine.trim();

                //if (expectLine.split("\\s").length != myLine.split("\\s").length) {
                    Integer[] goal = input.getSettlingsPosition()[line-1];
                    System.out.println("#######################");
                    System.out.println("Incorrect case: " + line);
                    System.out.println("Expected: " + expectLine);
                    Integer expectedCost = calculateOutputCost(expectLine, input.getMethod(), input.getMap(), goal);
                    System.out.println("Output: " + myLine);
                    Integer myCost = calculateOutputCost(myLine, input.getMethod(), input.getMap(), goal);
                    if (expectedCost != null && myCost != null) {
                        System.out.println("Cost compare: " + (expectedCost.equals(myCost)));
                    }
                    if ((myCost != null && expectedCost != null && myCost <= expectedCost) || (myCost == null && expectedCost == null)) {
                        System.out.println("My cost is lower or equal ");
                        success = true;
                    } else {
                        success = false;
                    }
                    System.out.println("#######################");
                //}
                expectLine = expectOutputReader.readLine();
                myLine = myOutputReader.readLine();
                line++;
            }

            expectOutputReader.close();
            myOutputReader.close();

            System.out.println("###########################################");
            if (success) {
                System.out.println("#### Case " + caseNumber + " PASSED");
            } else {
                System.out.println("#### Case " + caseNumber + " FAILED");
            }
            System.out.println("###########################################");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Integer calculateOutputCost(String line, SearchMethod method, Integer[][] map, Integer[] goal) {
        line = line.trim();
        if (line.equalsIgnoreCase("FAIL")) return null;
        String[] positions = line.split("\\s");
        Integer cost = 0;
        String[] lastPosStr = positions[0].split(",");
        Integer[] lastPos = new Integer[]{ Integer.valueOf(lastPosStr[0]), Integer.valueOf(lastPosStr[1])};
        for(int i=1; i<positions.length; i++) {
            String[] currentPosStr = positions[i].split(",");
            Integer[] currentPos = new Integer[]{ Integer.valueOf(currentPosStr[0]), Integer.valueOf(currentPosStr[1])};

            if (SearchMethod.ASTAR.equals(method)) {
                cost += getAStartUnitPathCost(lastPos, currentPos, map);
            } else {
                cost += getUnitPathCost(method, lastPos, currentPos);
            }

            lastPos = currentPos;
        }

        System.out.println("Cost: " + cost);
        return cost;
    }

    public static int getUnitPathCost(SearchMethod method, Integer[] currentPos, Integer[] nextPos) {
        if (SearchMethod.BFS.equals(method)) return 1;
        int diffX = Math.abs(currentPos[0] - nextPos[0]);
        int diffY = Math.abs(currentPos[1] - nextPos[1]);
        if (diffX != 0 && diffY != 0) {
            //diagonal move
            return 14;
        } else if ((diffX != 0 && diffY == 0) || (diffX == 0 && diffY !=0)) {
            // vertical or horizontal move
            return 10;
        } else {
            return 0;
        }
    }

    public static int getAStartUnitPathCost(Integer[] currentPos, Integer[] nextPos, Integer[][] map) {
        int ucsPathCost = getUnitPathCost(SearchMethod.UCS, currentPos, nextPos);
        int nextMuddyLevel = Math.max(map[nextPos[1]][nextPos[0]], 0);
        int currentHeight = Math.min(map[currentPos[1]][currentPos[0]], 0);
        int nextHeight = Math.min(map[nextPos[1]][nextPos[0]], 0);
        return ucsPathCost + Math.abs(map[currentPos[1]][currentPos[0]]-map[nextPos[1]][nextPos[0]]);
        //return ucsPathCost + nextMuddyLevel + Math.abs(currentHeight - nextHeight);
    }

    public static int heuristic(Integer[] pos, Integer[] goal, Integer[][] map) {
        int dx = Math.abs(pos[0] - goal[0]);
        int dy = Math.abs(pos[1] - goal[1]);
        int muddy = Math.max(0, map[pos[1]][pos[0]]);
        int height = Math.min(0, map[pos[1]][pos[0]]);
        int goalHeight = Math.min(0, map[goal[1]][goal[0]]);

        return 10 * (dx+dy) + (14 - 2*10) * Math.min(dx, dy) + Math.abs(map[pos[1]][pos[0]] - map[goal[1]][goal[0]]);
        //return 10 * (dx+dy) + (14 - 2*10) * Math.min(dx, dy) + muddy + Math.abs(height - goalHeight);
    }
}

class Solution {
    private int[][] directions = { {0,-1}, {1,-1}, {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}};
    public SearchModel searchModel;

    public Solution() {}

    public void solve(String input) {
        SearchModel model = null;
        try {
             model = getInput(input);
            System.out.println(model.getMethod());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (model != null) {
            int n = model.getNumberOfSettlings();
            String[] results = new String[n];
            for (int i=0; i<n; i++) {
                System.out.println("Test case #" + (i+1));
                switch (model.getMethod()) {
                    case BFS:
                        List<Integer[]> path = bfs(model.getMap(), model.getStartPosition(), model.getSettlingsPosition()[i], model.getMaxRockHeight());
                        results[i] = getResult(path);
                        break;

                    case UCS:
                        path = ucs(model.getMap(), model.getStartPosition(), model.getSettlingsPosition()[i], model.getMaxRockHeight());
                        results[i] = getResult(path);
                        break;

                    case ASTAR:
                        path = astar(model.getMap(), model.getStartPosition(), model.getSettlingsPosition()[i], model.getMaxRockHeight());
                        results[i] = getResult(path);
                        break;

                    default:
                        System.out.println("Invalid search method");
                }
            }


            writeOutput(results);
        }
    }

    /******************************
     * Search functions *
     *******************************/

//    public List<Integer[]> bfs(Integer[][] map, Integer[] startPosition, Integer[] goal, Integer maxRockHeight) {
//        if (map.length == 0 || map[0].length == 0) return null;
//
//        System.out.println("goal position: " + goal[0] + " " + goal[1]);
//        Queue<Node> queue = new LinkedList<>(); // keep paths
//        Integer width = map[0].length;
//        Integer height = map.length;
//        Set<String> visited = new HashSet<>();
//        Set<String> open = new HashSet<>();
//
//        Node startNode = new Node(null, startPosition, 0, null);
//        if (startNode.getPosition()[0].equals(goal[0]) && startNode.getPosition()[1].equals(goal[1])) {
//            return getPath(startNode);
//        }
//
//        queue.add(startNode);
//
//        while(!queue.isEmpty()) {
//            Node currentNode = queue.poll();
//            Integer[] pos = currentNode.getPosition(); // get last position
//            visited.add(pos[0] + "_" + pos[1]);
//            //System.out.println("current position: " + pos[0] + " " + pos[1]);
//
//            List<Node> children = expandChildren(currentNode, width, height, map, maxRockHeight, SearchMethod.BFS);
//            for(Node child: children) {
//                String key = child.getPosition()[0] + "_" + child.getPosition()[1];
//                if (!visited.contains(key) && !open.contains(key)) {
//                    if (child.getPosition()[0].equals(goal[0]) && child.getPosition()[1].equals(goal[1])) {
//                        System.out.println("current position: " + child.getPosition()[0] + " " + child.getPosition()[1]);
//                        System.out.println("Total cost: " + child.getPathCost());
//                        return getPath(child);
//                    }
//                    queue.add(child);
//                    open.add(key);
//                }
//
//            }
//        }
//
//        return null;
//    }

    public List<Integer[]> bfs(Integer[][] map, Integer[] startPosition, Integer[] goal, Integer maxRockHeight) {
        if (map.length == 0 || map[0].length == 0) return null;

        System.out.println("goal position: " + goal[0] + " " + goal[1]);
        Queue<Node> queue = new LinkedList<>(); // keep paths
        Integer width = map[0].length;
        Integer height = map.length;
        Set<String> visited = new HashSet<>();

        Node startNode = new Node(null, startPosition, 0, null);
        queue.add(startNode);

        while(!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Integer[] pos = currentNode.getPosition(); // get last position
            if (visited.contains(pos[0] + "_" + pos[1])) continue;
            visited.add(pos[0] + "_" + pos[1]);
            // System.out.println("current position: " + pos[0] + " " + pos[1]);

            if (pos[0].equals(goal[0]) && pos[1].equals(goal[1])) {
//                System.out.println("current position: " + pos[0] + " " + pos[1]);
//                System.out.println("Total cost: " + currentNode.getPathCost());
                return getPath(currentNode);
            }

            List<Node> children = expandChildren(currentNode, width, height, map, maxRockHeight, SearchMethod.BFS);
            for(Node child: children) {
                queue.add(child);
            }
        }

        return null;
    }

    public List<Integer[]> ucs(Integer[][] map, Integer[] startPosition, Integer[] goal, Integer maxRockHeight) {
        if (map.length == 0 || map[0].length == 0) return null;

        System.out.println("goal position: " + goal[0] + " " + goal[1]);
        PriorityQueue<Node> pq = new PriorityQueue(new UCSNodeComparator());
        Node startNode = new Node(null, startPosition, 0, null);
        pq.add(startNode);

        Map<String, Node> openNodeMap = new HashMap<>();
        openNodeMap.put(startPosition[0] + "_" + startPosition[1], startNode);

        Map<String, Node> visited = new HashMap<>();
        Integer width = map[0].length;
        Integer height = map.length;

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            Integer[] currentPos = currentNode.getPosition();
            //System.out.println("current position: " + currentPos[0] + " " + currentPos[1]);
            if (currentPos[0].equals(goal[0]) && currentPos[1].equals(goal[1])) {
                System.out.println("Total cost: " + currentNode.getPathCost());
                return getPath(currentNode);
            }

            List<Node> children = expandChildren(currentNode, width, height, map, maxRockHeight, SearchMethod.UCS);
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

    /******************************
     * A* Functions  *
     *******************************/

    public List<Integer[]> astar(Integer[][] map, Integer[] startPosition, Integer[] goal, Integer maxRockHeight) {
        if (map.length == 0 || map[0].length == 0) return null;

        System.out.println("goal position: " + goal[0] + " " + goal[1]);
        PriorityQueue<Node> pq = new PriorityQueue(new AStarNodeComparator());
        Node startNode = new Node(null, startPosition, 0, 0);
        pq.add(startNode);

        Map<String, Node> openNodeMap = new HashMap<>();
        openNodeMap.put(startPosition[0] + "_" + startPosition[1], startNode);

        Map<String, Node> visited = new HashMap<>();
        Integer width = map[0].length;
        Integer height = map.length;

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            Integer[] currentPos = currentNode.getPosition();
            //System.out.println("#### current position: " + currentPos[0] + " " + currentPos[1] + " Cost: " + currentNode.getPathCost());
            if (currentPos[0].equals(goal[0]) && currentPos[1].equals(goal[1])) {
                System.out.println("Total cost: " + currentNode.getPathCost());
                return getPath(currentNode);
            }

            List<Node> children = expandAStarChildren(currentNode, width, height, map, maxRockHeight, goal);
            for(Node child: children) {
                //System.out.println("child position: " + child.getPosition()[0] + " " + child.getPosition()[1] + " Cost: " + child.getPathCost());
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

    public int getAStartUnitPathCost(Integer[] currentPos, Integer[] nextPos, Integer[][] map) {
        int ucsPathCost = getUnitPathCost(SearchMethod.UCS, currentPos, nextPos);
        int nextMuddyLevel = Math.max(map[nextPos[1]][nextPos[0]], 0);
        int currentHeight = Math.min(map[currentPos[1]][currentPos[0]], 0);
        int nextHeight = Math.min(map[nextPos[1]][nextPos[0]], 0);
        return ucsPathCost + nextMuddyLevel + Math.abs(currentHeight - nextHeight);

        /******************************
         * For testing
         *******************************/
//        int ucsPathCost = getUnitPathCost(SearchMethod.UCS, currentPos, nextPos);
//        int currentHeight = map[currentPos[1]][currentPos[0]];
//        int nextHeight = map[nextPos[1]][nextPos[0]];
//        return ucsPathCost + Math.abs(currentHeight - nextHeight);

    }

    public List<Node> expandAStarChildren(Node currentNode, Integer width, Integer height, Integer[][] map, int maxRockHeight, Integer[] goal) {
        List<Node> children = new ArrayList<>();
        for(int[] dir: directions) {
            int newX = currentNode.getPosition()[0] + dir[0];
            int newY = currentNode.getPosition()[1] + dir[1];
            if (newX < 0 || newX >= width || newY < 0 || newY >= height ||
                    !passSteepCondition(map[currentNode.getPosition()[1]][currentNode.getPosition()[0]], map[newY][newX], maxRockHeight)) {
                continue;
            };
            Integer[] newPos = new Integer[]{ newX, newY};
            Integer newPathCost = currentNode.getPathCost() + getAStartUnitPathCost(currentNode.getPosition(), newPos, map);
            children.add(new Node(currentNode, newPos, newPathCost, newPathCost + heuristic(newPos, goal, map)));
        }
        return children;
    }

    public int heuristic(Integer[] pos, Integer[] goal, Integer[][] map) {
        int dx = Math.abs(pos[0] - goal[0]);
        int dy = Math.abs(pos[1] - goal[1]);
        int muddy = Math.max(0, map[pos[1]][pos[0]]);
        int height = Math.min(0, map[pos[1]][pos[0]]);
        int goalHeight = Math.min(0, map[goal[1]][goal[0]]);
        return 10 * (dx+dy) + (14 - 2*10) * Math.min(dx, dy) + muddy + Math.abs(height - goalHeight);

        /******************************
         * For testing
         *******************************/
//        int dx = Math.abs(pos[0] - goal[0]);
//        int dy = Math.abs(pos[1] - goal[1]);
//        int height = map[pos[1]][pos[0]];
//        int goalHeight = map[goal[1]][goal[0]];
//        return 10 * (dx+dy) + (14 - 2*10) * Math.min(dx, dy) + Math.abs(height - goalHeight);
    }

    /******************************
     * Utilities  *
     *******************************/

    private List<Integer[]> getPath(Node node) {
        List<Integer[]> path = new ArrayList<>();
        while (node != null) {
            path.add(0,node.getPosition());
            node = node.getParent();
        }
        return path;
    }

    public int getUnitPathCost(SearchMethod method, Integer[] currentPos, Integer[] nextPos) {
        if (SearchMethod.BFS.equals(method)) return 1;
        int diffX = Math.abs(currentPos[0] - nextPos[0]);
        int diffY = Math.abs(currentPos[1] - nextPos[1]);
        if (diffX != 0 && diffY != 0) {
            //diagonal move
            return 14;
        } else if ((diffX != 0 && diffY == 0) || (diffX == 0 && diffY !=0)) {
            // vertical or horizontal move
            return 10;
        } else {
            return 0;
        }
    }

    public List<Node> expandChildren(Node currentNode, Integer width, Integer height, Integer[][] map, int maxRockHeight, SearchMethod method) {
        List<Node> children = new ArrayList<>();
        for(int[] dir: directions) {
            int newX = currentNode.getPosition()[0] + dir[0];
            int newY = currentNode.getPosition()[1] + dir[1];
            if (newX < 0 || newX >= width || newY < 0 || newY >= height ||
                    !passSteepCondition(map[currentNode.getPosition()[1]][currentNode.getPosition()[0]], map[newY][newX], maxRockHeight)) {
                continue;
            };
            Integer[] newPos = new Integer[]{ newX, newY};
            children.add(new Node(currentNode, newPos, currentNode.getPathCost() + getUnitPathCost(method, currentNode.getPosition(), newPos), null));
        }
        return children;
    }

    public boolean passSteepCondition(Integer currentCellHeight, Integer nextCellHeight, Integer maxRockHeight) {
        currentCellHeight = Math.min(currentCellHeight, 0);
        nextCellHeight = Math.min(nextCellHeight, 0);
        return Math.abs(currentCellHeight - nextCellHeight) <= maxRockHeight;


        /******************************
         * For testing
         *******************************/
//        return Math.abs(currentCellHeight - nextCellHeight) <= maxRockHeight;
    }

    public SearchModel getInput(String filename) throws Exception {
        File file = new File(filename);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            int lineNumber = 1;
            SearchMethod method = null;
            Integer[] mapSize = new Integer[2];
            Integer[] startPosition = new Integer[2];
            Integer maxRockHeight = null;
            Integer numberOfSettlingSites = null;
            Integer[][] settlingPosition = null;
            Integer[][] map = null;

            while (line != null) {
                line = line.trim();
                if (lineNumber == 1) {
                    // line 1 - search method
                    method = SearchMethod.getSearchMethod(line.toUpperCase());
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 2) {
                    // line 2 - W H
                    int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                    mapSize[0] = temp[0];
                    mapSize[1] = temp[1];
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 3) {
                    // line 3 - starting position X Y
                    int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                    startPosition[0] = temp[0];
                    startPosition[1] = temp[1];
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 4) {
                    // line 4 - maximum rock height
                    maxRockHeight = Integer.valueOf(line);
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 5) {
                    // line 5 - number of settling sites
                    numberOfSettlingSites = Integer.valueOf(line);
                    lineNumber++;
                    line = reader.readLine();
                } else if (lineNumber == 6) {
                    // Next N lines - coordinates (in cells) of each target settling site X Y
                    settlingPosition = new Integer[numberOfSettlingSites][2];
                    for (int i=0; i<numberOfSettlingSites; i++) {
                        int[] temp = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                        settlingPosition[i][0] = temp[0];
                        settlingPosition[i][1] = temp[1];
                        line = reader.readLine().trim();
                    }
                    lineNumber += numberOfSettlingSites;
                } else {
                    // Next H lines - map
                    int w = mapSize[0];
                    int h = mapSize[1];
                    map = new Integer[h][w];
                    for(int y=0; y<h; y++) {
                        //System.out.println(line);
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
            searchModel = model;
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
}

class Node {
    private Node parent;
    private Integer[] position;
    private Integer pathCost;
    private Integer estimatedCost;

    public Node(Node parent, Integer[] position, Integer pathCost, Integer estimatedCost) {
        this.parent = parent;
        this.position = position;
        this.pathCost = pathCost;
        this.estimatedCost = estimatedCost;
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

    public Integer getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Integer estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;
        return node.position[0].equals(this.position[0]) && node.position[1].equals(this.position[1]);
    }
}

class UCSNodeComparator implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) {
        return o1.getPathCost().compareTo(o2.getPathCost());
    }
}

class AStarNodeComparator implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) {
        return o1.getEstimatedCost().compareTo(o2.getEstimatedCost());
    }
}

class SearchModel {
    private SearchMethod method;
    private Integer width;
    public  Integer height;
    private Integer[] startPosition;
    private Integer maxRockHeight;
    private Integer numberOfSettlings;
    private Integer[][] settlingsPosition;
    private Integer[][] map;

    public SearchModel(SearchMethod method, Integer width, Integer height, Integer[] startPosition, Integer maxRockHeight, Integer numberOfSettlings, Integer[][] settlingsPosition, Integer[][] map) {
        this.method = method;
        this.width = width;
        this.height = height;
        this.startPosition = startPosition;
        this.maxRockHeight = maxRockHeight;
        this.numberOfSettlings = numberOfSettlings;
        this.settlingsPosition = settlingsPosition;
        this.map = map;
    }

    public SearchMethod getMethod() {
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

enum SearchMethod {
    BFS("BFS"),
    UCS("UCS"),
    ASTAR("A*");

    private String name;

    SearchMethod(String name) {
        this.name = name;
    }

    public static Map<String, SearchMethod> map = new HashMap<>();
    static {
        map.put(BFS.getName(), BFS);
        map.put(UCS.getName(), UCS);
        map.put(ASTAR.getName(), ASTAR);
    }

    public String getName() {
        return this.name;
    }

    public static SearchMethod getSearchMethod(String name) {
        if (map.get(name) == null) {
            throw new RuntimeException(String.format("No search method with name (%s)", name));
        }
        return map.get(name);
    }
}