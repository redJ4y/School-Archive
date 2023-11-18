package graphs;

// @author Jared Scholz
public interface WeightedEdge<E> extends Edge<E> {

    public double getWeight();

    public void setWeight(double weight);
}
