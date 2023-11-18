
import java.awt.Dimension;

// @author Jared Scholz
public class SubdivisionNode {

    private SubdivisionNode childOne; // left or top
    private SubdivisionNode childTwo; // right or bottom
    private int splitIndex; // - for horizontal, + for vertical, 0 for leaf node

    private final Dimension dimension;
    private int value;

    public SubdivisionNode(Dimension dimension) {
        childOne = null;
        childTwo = null;
        splitIndex = 0;
        this.dimension = dimension;
        value = 0;
    }

    public SubdivisionNode makeCopy() { // used for temp nodes
        return new SubdivisionNode(dimension);
    }

    public void assumeSplit(SubdivisionNode tempNode) { // use the same split as a temp node
        childOne = tempNode.childOne;
        childTwo = tempNode.childTwo;
        splitIndex = tempNode.getSplitIndex();
    }

    public void adoptChildren(SubdivisionNode childOne, SubdivisionNode childTwo, int splitIndex) { // adopt existing nodes
        this.childOne = childOne;
        this.childTwo = childTwo;
        this.splitIndex = splitIndex;
    }

    public void splitVertical(int index) { // initializes children
        childOne = new SubdivisionNode(new Dimension(index, dimension.height));
        childTwo = new SubdivisionNode(new Dimension(dimension.width - index, dimension.height));
        splitIndex = index;
    }

    public void splitHorizontal(int index) { // initializes children
        childOne = new SubdivisionNode(new Dimension(dimension.width, index));
        childTwo = new SubdivisionNode(new Dimension(dimension.width, dimension.height - index));
        splitIndex = -index; // store horizontal indexes as negative
    }

    public boolean isLeaf() {
        return splitIndex == 0;
    }

    public SubdivisionNode getChildOne() {
        return childOne;
    }

    public SubdivisionNode getChildTwo() {
        return childTwo;
    }

    public int getChildrensValue() {
        return childOne.getValue() + childTwo.getValue();
    }

    public int getSplitIndex() {
        return splitIndex;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
