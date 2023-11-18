
import java.awt.Dimension;

// @author Jared Scholz
public class Subdivider {

    private final int[][] landValues;
    private final int divisionCost;

    public Subdivider(int[][] landValues, int divisionCost) {
        this.landValues = landValues;
        this.divisionCost = divisionCost;
    }

    public SubdivisionNode subdivide(Dimension startingLand, Strategy strategy) {
        switch (strategy) {
            case BRUTE_FORCE:
                SubdivisionNode resultRootBF = new SubdivisionNode(startingLand);
                bruteForceSolve(resultRootBF);
                return resultRootBF;
            case GREEDY:
                SubdivisionNode resultRootG = new SubdivisionNode(startingLand);
                greedySolve(resultRootG);
                return resultRootG;
            case DYNAMIC:
                return dynamicSolve(startingLand);
            default:
                return null;
        }
    }

    private void bruteForceSolve(SubdivisionNode node) {
        Dimension currentDimension = node.getDimension();
        int maxValue = landValues[currentDimension.width - 1][currentDimension.height - 1]; // value with no split
        for (int x = 1; x < currentDimension.width; x++) {
            SubdivisionNode tempNode = node.makeCopy(); // test each possible vertical split with a temp node
            tempNode.splitVertical(x); // initializes children
            bruteForceSolve(tempNode.getChildOne());
            bruteForceSolve(tempNode.getChildTwo());
            int value = tempNode.getChildrensValue() - (divisionCost * currentDimension.height);
            if (value > maxValue) {
                maxValue = value;
                node.assumeSplit(tempNode); // keep track of the solution used
            }
        }
        for (int y = 1; y < currentDimension.height; y++) { // same as before but for splitting horizontally
            SubdivisionNode tempNode = node.makeCopy();
            tempNode.splitHorizontal(y);
            bruteForceSolve(tempNode.getChildOne());
            bruteForceSolve(tempNode.getChildTwo());
            int value = tempNode.getChildrensValue() - (divisionCost * currentDimension.width);
            if (value > maxValue) {
                maxValue = value;
                node.assumeSplit(tempNode);
            }
        }
        node.setValue(maxValue);
    }

    private void greedySolve(SubdivisionNode node) { // goes with the best immediate subdivision for each plot
        Dimension currentDimension = node.getDimension();
        int maxValue = landValues[currentDimension.width - 1][currentDimension.height - 1]; // value with no split
        for (int x = 1; x < currentDimension.width; x++) {
            SubdivisionNode tempNode = node.makeCopy(); // test each possible vertical split with a temp node
            tempNode.splitVertical(x); // initializes children
            Dimension cOneD = tempNode.getChildOne().getDimension();
            Dimension cTwoD = tempNode.getChildTwo().getDimension();
            int value = landValues[cOneD.width - 1][cOneD.height - 1] + landValues[cTwoD.width - 1][cTwoD.height - 1];
            value -= divisionCost * currentDimension.height;
            // pick the best immediate value (without looking at future possible subdivisions)
            if (value > maxValue) {
                maxValue = value;
                node.assumeSplit(tempNode); // keep track of the solution used
            }
        }
        for (int y = 1; y < currentDimension.height; y++) { // same as before but for splitting horizontally
            SubdivisionNode tempNode = node.makeCopy();
            tempNode.splitHorizontal(y);
            Dimension cOneD = tempNode.getChildOne().getDimension();
            Dimension cTwoD = tempNode.getChildTwo().getDimension();
            int value = landValues[cOneD.width - 1][cOneD.height - 1] + landValues[cTwoD.width - 1][cTwoD.height - 1];
            value -= divisionCost * currentDimension.width;
            if (value > maxValue) {
                maxValue = value;
                node.assumeSplit(tempNode);
            }
        }
        node.setValue(maxValue);
        if (!node.isLeaf()) { // do the same with each side...
            greedySolve(node.getChildOne());
            greedySolve(node.getChildTwo());
        }
    }

    private SubdivisionNode dynamicSolve(Dimension dimension) {
        // use a SubdivisionNode array to improve readability (though technically a bit slower)...
        SubdivisionNode[][] nodes = new SubdivisionNode[dimension.width + 1][dimension.height + 1];
        // initialize the 0 height row and the 0 width column:
        for (int i = 0; i < dimension.width; i++) {
            SubdivisionNode newZeroNode = new SubdivisionNode(new Dimension(i, 0));
            newZeroNode.setValue(0);
            nodes[i][0] = newZeroNode;
        }
        for (int j = 0; j < dimension.height; j++) {
            SubdivisionNode newZeroNode = new SubdivisionNode(new Dimension(0, j));
            newZeroNode.setValue(0);
            nodes[0][j] = newZeroNode;
        }
        // fill out the table:
        for (int col = 1; col <= dimension.width; col++) { // col corresponds with subdivision width
            for (int row = 1; row <= dimension.height; row++) { // row corresponds with subdivision height
                SubdivisionNode currentNode = new SubdivisionNode(new Dimension(col, row));
                int maxValue = landValues[col - 1][row - 1]; // value with no split
                // each possible split index (vertical or horizontal) would result in two subdivisions...
                // as the table is starting with the simplest cases, the maximum values of these two subdivisions are already known
                // by looking up or to the left in the table, you can find the two resulting sides (both already optimally subdivided)
                // (e.g. vertical split index 1 = subdivision immediately left one + one in from the far left side)
                for (int i = col - 1; i > 0; i--) { // check the max value of each possible vertical split (looking left)
                    SubdivisionNode childOne = nodes[i][row];
                    SubdivisionNode childTwo = nodes[col - i][row];
                    int possibleValue = childOne.getValue() + childTwo.getValue() - (divisionCost * row);
                    if (possibleValue > maxValue) {
                        maxValue = possibleValue;
                        currentNode.adoptChildren(childOne, childTwo, i); // maintain node references in table
                    }
                }
                for (int j = row - 1; j > 0; j--) { // same as before but for horizontal splits (looking up)
                    SubdivisionNode childOne = nodes[col][j];
                    SubdivisionNode childTwo = nodes[col][row - j];
                    int possibleValue = childOne.getValue() + childTwo.getValue() - (divisionCost * col);
                    if (possibleValue > maxValue) {
                        maxValue = possibleValue;
                        currentNode.adoptChildren(childOne, childTwo, -j);
                    }
                }
                currentNode.setValue(maxValue);
                nodes[col][row] = currentNode; // add solution to the table
            }
        }
        // the table already contains complete trees...
        return nodes[dimension.width][dimension.height];
    }
}
