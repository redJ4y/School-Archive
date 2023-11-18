package graphs;

/**
 * An interface that represents a vertex in a graph which holds elements of type
 * E in its vertices
 *
 * @see GraphADT.java
 *
 * @author Andrew Ensor
 */
import java.util.Set;

public interface Vertex<E> {

    // returns the element held in the vertex
    public E getUserObject();

    // sets the element held in the vertex
    public void setUserObject(E element);

    // returns the edges connecting with this vertex as a Set
    public Set<WeightedEdge<E>> incidentEdges();

    // returns the vertices that are adjacent to this vertex as a Set
    public Set<Vertex<E>> adjacentVertices();

    // returns whether specified vertex is adjacent to this vertex
    public boolean isAdjacent(Vertex<?> vertex);
}
