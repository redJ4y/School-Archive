package graphs;

/**
 * An interface that represents an undirected and weighted edge in a graph which
 * holds elements of type E in its vertices
 *
 * @see GraphADT.java
 *
 * @author Andrew Ensor
 */
public interface Edge<E> {

    // returns the two end vertices (poss same) for this edge as array
    public Vertex<E>[] endVertices();

    // returns the end vertex opposite the specified vertex
    public Vertex<E> oppositeVertex(Vertex<E> vertex);
}
