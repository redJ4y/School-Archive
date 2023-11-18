package application;

import graphs.AdjacencyListGraph;
import graphs.Vertex;
import graphs.WeightedGraphADT;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

// @author Jared Scholz
public class ArbitrageFinder {

    private static final double INFINITY = Double.MAX_VALUE;
    private static final int NO_VERTEX = -1;

    private final List<String> currencies;
    private double[][] exchangeRates;
    private double[][] weights;
    // result of Floyd-Warshall's all pairs algorithm:
    private double[][] weightsUsed;
    private int[][] verticesUsed;

    public ArbitrageFinder(List<String> currencies) {
        this.currencies = currencies;
        exchangeRates = null;
        weights = null;
    }

    public synchronized void setRates(double[][] exchangeRates) {
        if (exchangeRates.length != currencies.size() || exchangeRates[0].length != currencies.size()) {
            throw new IllegalArgumentException("invalid connectivity table dimensions");
        }
        this.exchangeRates = exchangeRates;
        if (weights == null) {
            weights = new double[exchangeRates.length][exchangeRates.length];
        }
        // convert exchange rates to edge weights:
        for (int i = 0; i < exchangeRates.length; i++) {
            for (int j = 0; j < exchangeRates.length; j++) {
                if (exchangeRates[i][j] > 0) {
                    weights[i][j] = Math.log(1.0 / exchangeRates[i][j]);
                } else {
                    weights[i][j] = 0;
                }
            }
        }
    }

    public synchronized Queue<WeightedGraphADT<String>> findArbitrages() {
        if (exchangeRates == null) {
            throw new IllegalStateException("setRates() must precede findArbitrages()");
        }
        RunAllPairsFloydWarshall(); // initialize weightsUsed and verticesUsed
        PriorityQueue<ArbitrageValueWrapper> arbitrageSorter = new PriorityQueue<>();
        boolean[] usedVertices = new boolean[currencies.size()];
        for (int i = 0; i < weightsUsed.length; i++) { // go along the main diagonal of weightsUsed:
            if (weightsUsed[i][i] < 0) { // negative weight used indicates there is an arbitrage somewhere from here...
                // find a vertex (startingIndex) within the arbitrage:
                boolean[] visitedVertices = new boolean[verticesUsed.length];
                int startingIndex = i; // to be determined by traversing:
                while (!visitedVertices[startingIndex] && !usedVertices[startingIndex]) { // find the first vertex that is visited a second time
                    visitedVertices[startingIndex] = true;
                    startingIndex = verticesUsed[startingIndex][startingIndex];
                } // startingIndex is now known to be within the arbitrage, or it has already been used
                if (!usedVertices[startingIndex]) { // ensure this arbitrage is unique
                    // construct a new graph of the arbitrage:
                    WeightedGraphADT<String> arbitrage = new AdjacencyListGraph(WeightedGraphADT.GraphType.DIRECTED);
                    ArbitrageValueWrapper valueWrapper = new ArbitrageValueWrapper(arbitrage);
                    Vertex<String> startingVertex = arbitrage.addVertex(currencies.get(startingIndex));
                    Vertex<String> previousVertex = startingVertex;
                    int previousIndex = startingIndex;
                    int currentIndex = verticesUsed[startingIndex][startingIndex];
                    while (currentIndex != startingIndex) {
                        Vertex<String> currentVertex = arbitrage.addVertex(currencies.get(currentIndex));
                        arbitrage.addEdge(currentVertex, previousVertex, exchangeRates[currentIndex][previousIndex]);
                        valueWrapper.addMultiplier(exchangeRates[currentIndex][previousIndex]);
                        usedVertices[currentIndex] = true;
                        previousVertex = currentVertex;
                        previousIndex = currentIndex;
                        currentIndex = verticesUsed[currentIndex][currentIndex];
                    }
                    // connect back to the (already existing) starting vertex:
                    arbitrage.addEdge(startingVertex, previousVertex, exchangeRates[startingIndex][previousIndex]);
                    valueWrapper.addMultiplier(exchangeRates[startingIndex][previousIndex]);
                    arbitrageSorter.add(valueWrapper); // place into priority queue
                }
            }
        }
        // convert priority queue of value wrappers to a queue of graphs:
        Queue<WeightedGraphADT<String>> arbitrages = new LinkedList<>();
        for (ArbitrageValueWrapper valueWrapper : arbitrageSorter) {
            arbitrages.add(valueWrapper.arbitrage);
        }
        return arbitrages;
    }

    /* Wraps an arbitrage graph with a comparable value for priority ordering */
    private class ArbitrageValueWrapper implements Comparable<ArbitrageValueWrapper> {

        public WeightedGraphADT<String> arbitrage;
        protected int length;
        protected double multiplier; // per iteration
        protected double value; // (multiplier / length)

        public ArbitrageValueWrapper(WeightedGraphADT<String> arbitrage) {
            this.arbitrage = arbitrage;
            length = 0;
            multiplier = 1.0;
            value = -1.0;
        }

        public void addMultiplier(double multiplier) {
            length++;
            this.multiplier *= multiplier;
        }

        @Override
        public int compareTo(ArbitrageValueWrapper o) { // calculate value when compared
            if (o.value < 0) {
                o.value = o.multiplier / o.length;
            }
            if (value < 0) {
                value = multiplier / length;
            }
            if (value == o.value) {
                return 0;
            } else {
                return value < o.value ? -1 : 1;
            }
        }
    }

    /* Floyd-Warshall's all pairs algorithm implemented by Andrew Ensor */
    private void RunAllPairsFloydWarshall() {
        int n = weights.length;
        double[][][] d = new double[n + 1][][];
        d[0] = weights;
        // create p[0]:
        int[][][] p = new int[n + 1][][];
        p[0] = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (weights[i][j] < INFINITY) {
                    p[0][i][j] = i;
                } else {
                    p[0][i][j] = NO_VERTEX;
                }
            }
        }
        // build d[1],...,d[n] and p[1],...,p[n] dynamically:
        for (int k = 1; k <= n; k++) {
            d[k] = new double[n][n];
            p[k] = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double s;
                    if (d[k - 1][i][k - 1] != INFINITY && d[k - 1][k - 1][j] != INFINITY) {
                        s = d[k - 1][i][k - 1] + d[k - 1][k - 1][j];
                    } else {
                        s = INFINITY;
                    }
                    if (d[k - 1][i][j] <= s) {
                        d[k][i][j] = d[k - 1][i][j];
                        p[k][i][j] = p[k - 1][i][j];
                    } else {
                        d[k][i][j] = s;
                        p[k][i][j] = p[k - 1][k - 1][j];
                    }
                }
            }
        }
        weightsUsed = d[d.length - 1];
        verticesUsed = p[p.length - 1];
    }
}
