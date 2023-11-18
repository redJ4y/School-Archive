package application;

// @author Jared Scholz
import graphs.AdjacencyListGraph;
import graphs.Edge;
import graphs.Vertex;
import graphs.WeightedGraphADT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BridgeExchangeFinder {

    private final List<String> currencies;
    private AdjacencyListGraph<String> graph;
    private int counter;

    public BridgeExchangeFinder(List<String> currencies) {
        this.currencies = currencies;
        graph = null;
    }

    public synchronized void setRates(double[][] exchangeRates) {
        if (exchangeRates.length != currencies.size() || exchangeRates[0].length != currencies.size()) {
            throw new IllegalArgumentException("invalid connectivity table dimensions");
        }
        graph = new AdjacencyListGraph(WeightedGraphADT.GraphType.UNDIRECTED); // garbage collect the old one
        List<Vertex<String>> vertices = new ArrayList(currencies.size());
        for (String currency : currencies) {
            vertices.add(graph.addVertex(currency));
        }
        for (int i = 0; i < currencies.size(); i++) {
            for (int j = i; j < currencies.size(); j++) { // always assumes bidirectional exchanges
                if (exchangeRates[i][j] > 0) {
                    graph.addEdge(vertices.get(i), vertices.get(j));
                }
            }
        }
    }

    public synchronized List<Edge<String>> findBridges() {
        if (graph == null) {
            throw new IllegalStateException("setRates() must precede findBridges()");
        }
        List<Edge<String>> bridges = new LinkedList<>();
        Map<Vertex<String>, VertexPropertiesWrapper> propertiesMap = new HashMap(currencies.size());
        Vertex<String> start = graph.vertexSet().iterator().next(); // pick any vertex
        counter = 0;
        doDepthFirstBridgeFinder(start, null, propertiesMap, bridges);
        return bridges;
    }

    private void doDepthFirstBridgeFinder(Vertex<String> current, Vertex<String> previous,
            Map<Vertex<String>, VertexPropertiesWrapper> propertiesMap, List<Edge<String>> bridges) {
        VertexPropertiesWrapper properties = propertiesMap.get(current);
        if (properties == null) {
            properties = new VertexPropertiesWrapper();
            propertiesMap.put(current, properties); // also marks this vertex as discovered
        }
        properties.d = counter++;
        properties.m = properties.d; // default m-value
        for (Edge<String> edge : current.incidentEdges()) {
            Vertex<String> next = edge.oppositeVertex(current);
            if (next != previous) { // exclude parent
                if (propertiesMap.get(next) == null) {
                    doDepthFirstBridgeFinder(next, current, propertiesMap, bridges);
                    // calculate m values on the way back...
                    properties.m = Math.min(properties.m, propertiesMap.get(next).m);
                } else { // next has already been discovered
                    properties.m = Math.min(properties.m, propertiesMap.get(next).d);
                }
            }
        }
        // determine if current is a bridge with any children:
        for (Edge<String> edge : current.incidentEdges()) {
            Vertex<String> next = edge.oppositeVertex(current);
            if (next != previous && propertiesMap.get(next).m > properties.d) {
                bridges.add(edge);
            }
        }
    }

    private class VertexPropertiesWrapper {

        public int d; // discovery position
        public int m; // reachable position (backwards) without this vertex
    }
}
