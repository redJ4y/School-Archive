
import java.awt.Dimension;

// @author Jared Scholz
public class Testing {

    public static void main(String[] args) {
        Dimension dimension = new Dimension(6, 3);
        int[][] landValues = {{20, 40, 100, 130, 150, 200},
        {40, 140, 250, 320, 400, 450},
        {100, 250, 350, 420, 450, 500},
        {130, 320, 420, 500, 600, 700},
        {150, 400, 450, 600, 700, 800},
        {200, 450, 500, 700, 800, 900}};

        System.out.println("Subdivision cost 0:");
        runTest(new Subdivider(landValues, 0), dimension, 750);

        System.out.println("\nSubdivision cost 10:");
        runTest(new Subdivider(landValues, 10), dimension, 690);

        System.out.println("\nSubdivision cost 50:");
        runTest(new Subdivider(landValues, 50), dimension, 550);

        System.out.println("\nSubdivision cost 100:");
        runTest(new Subdivider(landValues, 100), dimension, 500);

        System.out.println("\nEnsuring that dynamic matches brute force (6x6):");
        dimension = new Dimension(6, 6);
        for (int i = 0; i <= 64; i += 4) {
            System.out.print("Division Cost " + i + ": ");
            Subdivider subdivider = new Subdivider(landValues, i);
            int dynamicResult = subdivider.subdivide(dimension, Strategy.DYNAMIC).getValue();
            int bruteResult = subdivider.subdivide(dimension, Strategy.BRUTE_FORCE).getValue();
            if (dynamicResult == bruteResult) {
                System.out.println("Pass");
            } else {
                System.out.println("FAIL");
            }
        }
    }

    private static void runTest(Subdivider subdivider, Dimension dimension, int targetSolution) {
        int bruteForceSolution = subdivider.subdivide(dimension, Strategy.BRUTE_FORCE).getValue();
        int greedySolution = subdivider.subdivide(dimension, Strategy.GREEDY).getValue();
        int dynamicSolution = subdivider.subdivide(dimension, Strategy.DYNAMIC).getValue();
        if (bruteForceSolution == targetSolution) {
            System.out.println("Brute force passed (" + bruteForceSolution + ")");
        } else {
            System.out.println("Brute force FAILED (" + bruteForceSolution + ")");
        }
        if (greedySolution == targetSolution) {
            System.out.println("Greedy passed (" + greedySolution + ")");
        } else {
            System.out.println("Greedy is SUB-OPTIMAL (" + greedySolution + ")");
        }
        if (dynamicSolution == targetSolution) {
            System.out.println("Dynamic passed (" + dynamicSolution + ")");
        } else {
            System.out.println("Dynamic FAILED (" + dynamicSolution + ")");
        }
    }
}
