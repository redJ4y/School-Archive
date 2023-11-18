package application;

import graphs.AdjacencyListGraph;
import graphs.Vertex;
import graphs.WeightedEdge;
import graphs.WeightedGraphADT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// @author Jared Scholz
public class BestConversionFinder {

    private final List<String> currencies;
    private final Map<String, Vertex<String>> vertexMap;
    private WeightedGraphADT<String> graph;
    private double weightPadding; // value used to eliminate negative weights

    public BestConversionFinder(List<String> currencies) {
        this.currencies = currencies;
        vertexMap = new HashMap(currencies.size());
        graph = null;
        weightPadding = 64.0;
    }

    public synchronized void setRates(double[][] exchangeRates) {
        if (exchangeRates.length != currencies.size() || exchangeRates[0].length != currencies.size()) {
            throw new IllegalArgumentException("invalid connectivity table dimensions");
        }
        graph = new AdjacencyListGraph(WeightedGraphADT.GraphType.DIRECTED); // garbage collect the old one
        List<Vertex<String>> vertices = new ArrayList(currencies.size());
        for (String currency : currencies) {
            Vertex<String> newVertex = graph.addVertex(currency);
            vertices.add(newVertex);
            vertexMap.put(currency, newVertex);
        }
        for (int i = 0; i < currencies.size(); i++) {
            for (int j = 0; j < currencies.size(); j++) {
                if (exchangeRates[i][j] > 0) { // convert conversion to edge weight (eliminate negative weights):
                    graph.addEdge(vertices.get(i), vertices.get(j), Math.log(1.0 / exchangeRates[i][j]) + weightPadding);
                }
            }
        }
    }

    public synchronized WeightedGraphADT<String> getBestConversion(String fromCurrency, String toCurrency) {
        if (graph == null) {
            throw new IllegalStateException("setRates() must precede getBestConversion()");
        }
        if (!vertexMap.containsKey(fromCurrency) || !vertexMap.containsKey(toCurrency)) {
            throw new IllegalArgumentException("invalid currency string(s)");
        }
        Map<String, WeightedEdge<String>> shortestPathsTree = getShortestPathsTree(vertexMap.get(fromCurrency));
        if (shortestPathsTree == null) {
            throw new RuntimeException("negative weight closed path discovered: increase weightPadding");
        }
        // reconstruct the path taken:
        WeightedGraphADT<String> path = new AdjacencyListGraph(WeightedGraphADT.GraphType.DIRECTED);
        Vertex<String> current = path.addVertex(toCurrency);
        while (!current.getUserObject().equals(fromCurrency)) {
            WeightedEdge<String> edge = shortestPathsTree.get(current.getUserObject());
            Vertex<String> next = path.addVertex(edge.oppositeVertex(current).getUserObject());
            double exchangeRate = 1.0 / Math.exp(edge.getWeight() - weightPadding); // convert back to exchange rates
            path.addEdge(current, next, exchangeRate);
            path.addEdge(next, current, 1.0 / exchangeRate); // also set return rates
            current = next;
        }
        return path;
    }

    /* Implementation of the Bellman-Ford single source shortest path algorithm */
    private Map<String, WeightedEdge<String>> getShortestPathsTree(Vertex<String> source) {
        Map<String, WeightedEdge<String>> leastEdges = new LinkedHashMap<>();
        Map<Vertex<String>, Double> shortestPathEstimates = new HashMap<>();
        // initialize estimates:
        for (Vertex<String> vertex : graph.vertexSet()) {
            shortestPathEstimates.put(vertex, Double.POSITIVE_INFINITY);
        }
        shortestPathEstimates.put(source, 0.0);
        // relax edges:
        for (int i = 1; i < graph.vertexSet().size(); i++) {
            for (WeightedEdge<String> edge : graph.edgeSet()) {
                Vertex<String> start = edge.endVertices()[0];
                Vertex<String> end = edge.endVertices()[1];
                Double newPathWeight = shortestPathEstimates.get(start) + edge.getWeight();
                if (newPathWeight < shortestPathEstimates.get(end)) {
                    shortestPathEstimates.put(end, newPathWeight);
                    leastEdges.put(end.getUserObject(), edge);
                }
            }
        }
        // check if any edge can still be relaxed (check for negative weight closed paths):
        for (WeightedEdge<String> edge : graph.edgeSet()) {
            Vertex<String>[] vs = edge.endVertices();
            if (shortestPathEstimates.get(vs[0]) + edge.getWeight() < shortestPathEstimates.get(vs[1])) {
                return null; // there exists a negative weight closed path
            }
        }
        return leastEdges;
    }

    /* Takes in exchangeRates to ensure they are updated with the new weightPadding */
    public synchronized void setWeightPadding(double weightPadding, double[][] exchangeRates) {
        if (weightPadding < 0.0) {
            throw new IllegalArgumentException("weightPadding must be non-negative");
        }
        this.weightPadding = weightPadding;
        setRates(exchangeRates);
    }
}
