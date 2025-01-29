package ConwaysGameOfLIfe;

import java.util.Random;
import java.util.Scanner;

public class ConwaysGame {
    public static void main(String[] args) {
        int rows = 9, cols = 9;
        GameOfLife game = new GameOfLife(rows, cols);


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

        //choose weather to preset the grid or have it randomised
        game.setGrid(customGrid);
        //game.initializeGrid();
        game.printGrid("Initial Grid:");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Press ENTER to continue, or type 'exit' to stop...");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) break;

            game.updateGrid();
            game.printGrid("Updated Grid:");
        }
        scanner.close();
    }
}

class GameOfLife {
    private final int rows, cols;
    private int[][] grid;

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
            System.arraycopy(matrix[i], 0, grid[i], 0, cols); // Copies each row
        }
    }


    public void initializeGrid() {
        Random rand = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = rand.nextInt(2); // Randomly set to 0 or 1
            }
        }
    }

    public void updateGrid() {
        int[][] newGrid = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int aliveNeighbors = countAliveNeighbors(i, j);

                if (grid[i][j] == 1) {
                    newGrid[i][j] = (aliveNeighbors == 2 || aliveNeighbors == 3) ? 1 : 0;
                } else {
                    newGrid[i][j] = (aliveNeighbors == 3) ? 1 : 0;
                }
            }
        }
        grid = newGrid; // Swap new grid with the current grid
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
                System.out.print(cell == 1 ? "■ " : "□ "); // Represent alive as '■' and dead as '□'
            }
            System.out.println();
        }
    }
}
