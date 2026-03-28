import java.util.*;

// Класс состояния пазла
class PuzzleState {
    private final int[][] board;
    private final int blankRow;
    private final int blankCol;
    private final String action;
    private final PuzzleState parent;
    private final int cost;
    public static final int SIZE = 3;
    public static final int[][] GOAL = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 0}
    };

    public PuzzleState(int[][] board) {
        this.board = copyBoard(board);
        this.action = "Initial";
        this.parent = null;
        this.cost = 0;
        int[] blankPos = findBlank();
        this.blankRow = blankPos[0];
        this.blankCol = blankPos[1];
    }

    private PuzzleState(int[][] board, String action, PuzzleState parent, int newBlankRow, int newBlankCol) {
        this.board = board;
        this.action = action;
        this.parent = parent;
        this.cost = parent.cost + 1;
        this.blankRow = newBlankRow;
        this.blankCol = newBlankCol;
    }

    private int[] findBlank() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    return new int[]{i, j};
                }
            }
        }
        throw new IllegalArgumentException("Board must contain a blank cell (0)");
    }

    private int[][] copyBoard(int[][] original) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    public boolean isGoal() {
        return Arrays.deepEquals(board, GOAL);
    }

    public List<String> getPossibleActions() {
        List<String> actions = new ArrayList<>();
        if (blankRow > 0) actions.add("Up");
        if (blankRow < SIZE - 1) actions.add("Down");
        if (blankCol > 0) actions.add("Left");
        if (blankCol < SIZE - 1) actions.add("Right");
        return actions;
    }

    public PuzzleState applyAction(String action) {
        int newRow = blankRow;
        int newCol = blankCol;
        switch (action) {
            case "Up": newRow = blankRow - 1; break;
            case "Down": newRow = blankRow + 1; break;
            case "Left": newCol = blankCol - 1; break;
            case "Right": newCol = blankCol + 1; break;
            default: throw new IllegalArgumentException("Invalid action: " + action);
        }
        int[][] newBoard = copyBoard(this.board);
        newBoard[blankRow][blankCol] = newBoard[newRow][newCol];
        newBoard[newRow][newCol] = 0;
        return new PuzzleState(newBoard, action, this, newRow, newCol);
    }

    public int getManhattanDistance() {
        int distance = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int value = board[i][j];
                if (value != 0) {
                    int targetRow = (value - 1) / SIZE;
                    int targetCol = (value - 1) % SIZE;
                    distance += Math.abs(i - targetRow) + Math.abs(j - targetCol);
                }
            }
        }
        return distance;
    }

    public int getTotalCost() {
        return cost + getManhattanDistance();
    }

    public int[][] getBoard() {
        return copyBoard(board);
    }

    public String getAction() {
        return action;
    }

    public PuzzleState getParent() {
        return parent;
    }

    public int getCost() {
        return cost;
    }

    public int getBlankRow() {
        return blankRow;
    }

    public int getBlankCol() {
        return blankCol;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PuzzleState that = (PuzzleState) obj;
        return Arrays.deepEquals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }
}

// Класс для решения пазла
class PuzzleSolver {
    public static List<PuzzleState> solveAStar(PuzzleState start) {
        PriorityQueue<PuzzleState> openSet = new PriorityQueue<>(
                Comparator.comparingInt(PuzzleState::getTotalCost)
        );

        Map<PuzzleState, Integer> gScore = new HashMap<>();
        Map<PuzzleState, PuzzleState> cameFrom = new HashMap<>();

        openSet.offer(start);
        gScore.put(start, 0);

        while (!openSet.isEmpty()) {
            PuzzleState current = openSet.poll();

            if (current.isGoal()) {
                return reconstructPath(current);
            }

            for (String action : current.getPossibleActions()) {
                PuzzleState neighbor = current.applyAction(action);
                int tentativeGScore = gScore.get(current) + 1;

                if (tentativeGScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    if (!openSet.contains(neighbor)) {
                        openSet.offer(neighbor);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<PuzzleState> reconstructPath(PuzzleState goal) {
        List<PuzzleState> path = new ArrayList<>();
        PuzzleState current = goal;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path;
    }

    public static boolean isSolvable(int[][] board) {
        int inversions = 0;
        List<Integer> flattened = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] != 0) {
                    flattened.add(board[i][j]);
                }
            }
        }
        for (int i = 0; i < flattened.size(); i++) {
            for (int j = i + 1; j < flattened.size(); j++) {
                if (flattened.get(i) > flattened.get(j)) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0;
    }

    public static int[][] generateRandomState() {
        List<Integer> numbers = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        Collections.shuffle(numbers);
        int[][] board = new int[3][3];
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = numbers.get(index++);
            }
        }
        if (!isSolvable(board)) {
            return generateRandomState();
        }
        return board;
    }
}