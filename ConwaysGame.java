package ConwaysGameOfLIfe;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/conway_game"; // Update your database name
    private static final String USER = "root";  // Database username
    private static final String PASS = ""; // Database password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static int createNewGame(int[][] grid) {
        String insertGameSQL = "INSERT INTO games () VALUES ()";  // No grid_state needed
        String insertMoveSQL = "INSERT INTO moves (game_id, generation) VALUES (?, ?)";
        String insertLiveCellSQL = "INSERT INTO live_cells (move_id, x, y) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement gameStmt = conn.prepareStatement(insertGameSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement moveStmt = conn.prepareStatement(insertMoveSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement cellStmt = conn.prepareStatement(insertLiveCellSQL)) {

            // Step 1: Insert a new game
            gameStmt.executeUpdate();
            ResultSet gameKeys = gameStmt.getGeneratedKeys();
            if (!gameKeys.next()) {
                throw new SQLException("Failed to create game.");
            }
            int gameId = gameKeys.getInt(1);

            // Step 2: Insert initial move (generation 0)
            moveStmt.setInt(1, gameId);
            moveStmt.setInt(2, 0); // Initial state
            moveStmt.executeUpdate();
            ResultSet moveKeys = moveStmt.getGeneratedKeys();
            if (!moveKeys.next()) {
                throw new SQLException("Failed to create initial move.");
            }
            int moveId = moveKeys.getInt(1);

            // Step 3: Insert alive cells for generation 0
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    if (grid[x][y] == 1) { // Assuming 1 means alive
                        cellStmt.setInt(1, moveId);
                        cellStmt.setInt(2, x);
                        cellStmt.setInt(3, y);
                        cellStmt.addBatch();
                    }
                }
            }

            // Execute batch insert for efficiency
            cellStmt.executeBatch();

            return gameId;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Error case
    }


    public static void saveGameState(int gameId, int[][] grid) {
        String insertMoveSQL = "INSERT INTO moves (game_id) VALUES (?)";
        String insertLiveCellSQL = "INSERT INTO live_cells (move_id, x, y) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement moveStmt = conn.prepareStatement(insertMoveSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement cellStmt = conn.prepareStatement(insertLiveCellSQL)) {

            // Step 1: Insert a new move with the given gameId
            moveStmt.setInt(1, gameId);
            moveStmt.executeUpdate();

            // Step 2: Retrieve the generated move_id
            ResultSet generatedKeys = moveStmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                throw new SQLException("Failed to retrieve move_id.");
            }
            int moveId = generatedKeys.getInt(1);

            // Step 3: Insert alive cells
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    if (grid[x][y] == 1) { // Assuming 1 means the cell is alive
                        cellStmt.setInt(1, moveId);
                        cellStmt.setInt(2, x);
                        cellStmt.setInt(3, y);
                        cellStmt.addBatch();
                    }
                }
            }

            // Execute batch insert for efficiency
            cellStmt.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static int[][] loadGameState(int gameId, int rows, int cols) {
        // Initialize an empty grid
        int[][] grid = new int[rows][cols];

        // SQL to get the latest move_id for the given game
        String getLatestMoveSQL = "SELECT move_id FROM moves WHERE game_id = ? ORDER BY generation DESC LIMIT 1";
        // SQL to get live cells for that move
        String getLiveCellsSQL = "SELECT x, y FROM live_cells WHERE move_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement moveStmt = conn.prepareStatement(getLatestMoveSQL);
             PreparedStatement cellStmt = conn.prepareStatement(getLiveCellsSQL)) {

            // Step 1: Get the latest move ID
            moveStmt.setInt(1, gameId);
            ResultSet moveResult = moveStmt.executeQuery();

            if (!moveResult.next()) {
                System.out.println("No moves found for game_id: " + gameId);
                return grid; // Return an empty grid
            }

            int moveId = moveResult.getInt("move_id");

            // Step 2: Get all live cells for that move
            cellStmt.setInt(1, moveId);
            ResultSet cellResult = cellStmt.executeQuery();

            while (cellResult.next()) {
                int x = cellResult.getInt("x");
                int y = cellResult.getInt("y");

                // Ensure x, y are within grid bounds
                if (x >= 0 && x < rows && y >= 0 && y < cols) {
                    grid[x][y] = 1; // Mark cell as alive
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return grid;
    }


    public static List<Integer> getAllGameIds() {
        List<Integer> gameIds = new ArrayList<>();
        String query = "SELECT game_id FROM games";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                gameIds.add(rs.getInt("game_id"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gameIds;  // Returns empty list if no games exist
    }

    private static String gridToString(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : grid) {
            sb.append(Arrays.toString(row)).append(";");
        }
        return sb.toString();
    }

    private static int[][] stringToGrid(String data, int rows, int cols) {
        int[][] grid = new int[rows][cols];
        String[] rowsData = data.split(";");
        for (int i = 0; i < rows; i++) {
            String[] colsData = rowsData[i].replaceAll("[\\[\\]]", "").split(", ");
            for (int j = 0; j < cols; j++) {
                grid[i][j] = Integer.parseInt(colsData[j]);
            }
        }
        return grid;
    }
}

public class ConwaysGame {
    public static void main(String[] args) {
        int rows = 9, cols = 9;
        Scanner scanner = new Scanner(System.in);
        int gameId = -1;
        GameOfLife game = new GameOfLife(rows, cols);

        // Ask user whether to start a new game or resume
        System.out.println("Start a new game (N) or resume an existing one (E)?");
        String choice = scanner.nextLine();

        if (choice.equalsIgnoreCase("N")) {
            // Start a new game
            boolean validChoice = false;

            while (!validChoice) {
                System.out.println("Start a preset game (S) or a random one (R)");
                String decision = scanner.nextLine();

                if (decision.equalsIgnoreCase("S")) {
                    // Preset game setup
                    int[][] customGrid = {
                            {0, 0, 0, 0, 0, 0, 0, 0, 0},
                            {0, 0, 0, 0, 0, 0, 0, 0, 0},
                            {0, 0, 0, 0, 0, 0, 0, 0, 0},
                            {0, 0, 0, 0, 1, 0, 0, 0, 0},
                            {0, 0, 0, 1, 1, 1, 0, 0, 0},
                            {0, 0, 0, 0, 0, 0, 0, 0, 0},
                            {0, 0, 0, 0, 0, 0, 0, 0, 0},
                            {0, 0, 0, 0, 0, 0, 0, 0, 0},
                            {0, 0, 0, 0, 0, 0, 0, 0, 0}
                    };
                    game.setGrid(customGrid);
                    gameId = DatabaseConnection.createNewGame(customGrid);
                    System.out.println("New game started with ID: " + gameId);
                    validChoice = true;  // Valid choice made, exit the loop
                } else if (decision.equalsIgnoreCase("R")) {
                    // Random grid setup
                    game.setRandomGrid();  // This method would generate a random grid
                    gameId = DatabaseConnection.createNewGame(game.getGrid());
                    System.out.println("Random game started with ID: " + gameId);
                    validChoice = true;  // Valid choice made, exit the loop
                } else {
                    System.out.println("Invalid option. Please restart the game.");
                }
            }

        } else if (choice.equalsIgnoreCase("E")) {
            // Display all available game IDs
            System.out.println("Available games to resume:");
            List<Integer> availableGameIds = DatabaseConnection.getAllGameIds();  // Assuming this method returns a list of game IDs
            if (availableGameIds.isEmpty()) {
                System.out.println("No games available to resume.");
            } else {
                for (int aGameId : availableGameIds) {
                    System.out.println("Game ID: " + aGameId);
                }
            }

            // Prompt for a game ID to resume
            while (true) {
                try {
                    gameId = Integer.parseInt(scanner.nextLine().trim());
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a valid Game ID.");
                }
            }

            // Load the game state
            int[][] loadedGrid = DatabaseConnection.loadGameState(gameId, rows, cols);
            if (loadedGrid != null) {
                game.setGrid(loadedGrid);
                System.out.println("Game resumed with ID: " + gameId);
            } else {
                System.out.println("Invalid Game ID. Starting a new game instead.");
                gameId = DatabaseConnection.createNewGame(game.getGrid());
            }
        }


        game.printGrid("Initial Grid:");

        while (true) {
            System.out.println("Press ENTER to continue, or type 'exit' to stop...");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) break;

            game.updateGrid();
            DatabaseConnection.saveGameState(gameId, game.getGrid());
            game.printGrid("Updated Grid:");
        }
        scanner.close();
    }
}

class GameOfLife {
    private final int rows, cols;
    private int[][] grid;
    private final Random random = new Random();

    public GameOfLife(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new int[rows][cols];
    }

    public void setGrid(int[][] matrix) {
        if (matrix.length != rows || matrix[0].length != cols) {
            throw new IllegalArgumentException("Grid size must match " + rows + "x" + cols);
        }
        for (int i = 0; i < rows; i++) {
            System.arraycopy(matrix[i], 0, grid[i], 0, cols);
        }
    }

    public void setRandomGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = random.nextBoolean() ? 1 : 0;
            }
        }
    }

    public int[][] getGrid() {
        return grid;
    }

    public void updateGrid() {
        int[][] newGrid = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int aliveNeighbors = countAliveNeighbors(i, j);
                newGrid[i][j] = (grid[i][j] == 1 && (aliveNeighbors == 2 || aliveNeighbors == 3)) ||
                        (grid[i][j] == 0 && aliveNeighbors == 3) ? 1 : 0;
            }
        }
        grid = newGrid;
    }

    private int countAliveNeighbors(int x, int y) {
        int count = 0;
        int[] checkX = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] checkY = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int newX = x + checkX[i], newY = y + checkY[i];
            if (newX >= 0 && newY >= 0 && newX < rows && newY < cols && grid[newX][newY] == 1) {
                count++;
            }
        }
        return count;
    }

    public void printGrid(String message) {
        System.out.println(message);
        for (int[] row : grid) {
            for (int cell : row) {
                System.out.print(cell == 1 ? "■ " : "□ ");
            }
            System.out.println();
        }
    }
}
