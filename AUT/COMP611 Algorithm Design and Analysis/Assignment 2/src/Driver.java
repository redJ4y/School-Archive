
import java.awt.Dimension;
import java.util.Random;

// @author Jared Scholz
public class Driver {

    public static void main(String[] args) {
        /*--- Subdivision conditions below ---*/
        int divisionCost = 10; // per meter
        Dimension startingLand = new Dimension(20, 20);
        Strategy strategy = Strategy.DYNAMIC;

        // if landValues is null, random values will be generated:
        int[][] landValues = null;
        /*--- End subdivision conditions ---*/

        if (landValues == null) {
            landValues = generateRandomValues(startingLand);
        }
        Subdivider subdivider = new Subdivider(landValues, divisionCost);
        SubdivisionDisplay display = new SubdivisionDisplay();
        display.display();
        SubdivisionNode resultRoot = subdivider.subdivide(startingLand, strategy);
        display.setRootNode(resultRoot);
    }

    private static int[][] generateRandomValues(Dimension dimension) {
        Random randGen = new Random();
        int[][] values = new int[dimension.width][dimension.height];
        // fill out the shared square (mirror values over main diagonal):
        int squareSize = Math.min(dimension.width, dimension.height);
        for (int i = 0; i < squareSize; i++) {
            for (int j = i; j < squareSize; j++) {
                int scale = (i + 1) * (j + 1);
                int value = randGen.nextInt(scale * 10) + scale;
                values[i][j] = value;
                values[j][i] = value; // mirror values over main diagonal
            }
        }
        // fill out the remaining values (no need to mirror anything):
        if (dimension.width != dimension.height) {
            int iStart = 0;
            int jStart = 0;
            if (dimension.width > dimension.height) {
                iStart = squareSize;
            } else {
                jStart = squareSize;
            }
            for (int i = iStart; i < dimension.width; i++) {
                for (int j = jStart; j < dimension.height; j++) {
                    int scale = (i + 1) * (j + 1);
                    values[i][j] = randGen.nextInt(scale * 10) + scale;
                }
            }
        }
        // print out the randomly generated values if not too large:
        if (dimension.width < 31 && dimension.height < 31) {
            System.out.print("Randomly generated land values:\n{");
            for (int j = 0; j < dimension.height; j++) {
                System.out.print("{");
                for (int i = 0; i < dimension.width - 1; i++) {
                    System.out.printf("%d, ", values[i][j]);
                }
                if (j == dimension.height - 1) {
                    System.out.printf("%d}};\n", values[dimension.width - 1][j]);
                } else {
                    System.out.printf("%d},\n", values[dimension.width - 1][j]);
                }
            }
        }
        return values;
    }
}
