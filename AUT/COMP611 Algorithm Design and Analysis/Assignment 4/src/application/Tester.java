package application;

// @author Jared Scholz
import graphs.Edge;
import graphs.Vertex;
import graphs.WeightedEdge;
import graphs.WeightedGraphADT;
import java.util.Arrays;
import java.util.List;

public class Tester {

    public static void main(String[] args) {
        List<String> countries = Arrays.asList("AUD", "EUR", "MXN", "NZD", "USD");
        double[][] rates = {
            {1, 0.61, 0, 1.08, 0.72},
            {1.64, 1, 0, 1.77, 1.18},
            {0, 0, 1, 0, 0.047},
            {0.92, 0.56, 0, 1, 0.67},
            {1.39, 0.85, 21.19, 1.5, 1}
        };
        /*List<String> countries = Arrays.asList("USD", "CAD", "EUR", "GBP", "HKD", "CHF", "JPY", "AUD", "INR", "CNY");
        double[][] rates = {
            {1, 1.36, 1.01, 0.88, 7.85, 1, 147.7, 1.57, 82.54, 7.24},
            {0.73, 1, 0.74, 0.65, 5.75, 0.73, 108.24, 1.15, 60.49, 5.31},
            {0.99, 1.35, 1, 0.87, 7.74, 0.98, 145.6, 1.55, 81.37, 7.14},
            {1.13, 1.54, 1.15, 1, 8.87, 1.13, 166.96, 1.77, 93.31, 8.19},
            {0.13, 0.17, 0.13, 0.11, 1, 0.13, 18.82, 0.2, 10.51, 0.92},
            {1, 1.37, 1.02, 0.89, 7.87, 1, 148, 1.57, 82.71, 7.26},
            {0.01, 0.01, 0.01, 0.01, 0.05, 0.01, 1, 0.01, 0.56, 0.05},
            {0.64, 0.87, 0.65, 0.56, 5.01, 0.64, 94.2, 1, 52.64, 4.62},
            {0.01, 0.02, 0.01, 0.01, 0.1, 0.01, 1.79, 0.02, 1, 0.09},
            {0.14, 0.19, 0.14, 0.12, 1.08, 0.14, 20.39, 0.22, 11.39, 1}
        };
        List<String> countries = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M");
        double[][] rates = {
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0}
        };*/

        String fromCurrency = "NZD";
        String toCurrency = "MXN";

        BestConversionFinder conversionFinder = new BestConversionFinder(countries);
        conversionFinder.setRates(rates);
        WeightedGraphADT<String> bestConversion = conversionFinder.getBestConversion(fromCurrency, toCurrency);
        System.out.println("Best conversion from " + fromCurrency + " to " + toCurrency + ":");
        System.out.println("–––––––––––––––––––––––––––––––––––––");
        displayConversion(bestConversion, fromCurrency);
        System.out.println("–––––––––––––––––––––––––––––––––––––");

        ArbitrageFinder arbitrageFinder = new ArbitrageFinder(countries);
        arbitrageFinder.setRates(rates);
        System.out.println("\nArbitrages:");
        System.out.println("–––––––––––––––––––––––––––––––––––––");
        for (WeightedGraphADT<String> arbitrage : arbitrageFinder.findArbitrages()) {
            displayArbitrage(arbitrage);
            System.out.println("–––––––––––––––––––––––––––––––––––––");
        }

        BridgeExchangeFinder bridgeFinder = new BridgeExchangeFinder(countries);
        bridgeFinder.setRates(rates);
        System.out.println("\nBridge Edges:");
        System.out.println("–––––––––––––––––––––––––––––––––––––");
        for (Edge<String> edge : bridgeFinder.findBridges()) {
            System.out.println(edge);
        }
        System.out.println("–––––––––––––––––––––––––––––––––––––");
    }

    private static void displayConversion(WeightedGraphADT<String> graph, String fromCurrency) {
        Vertex<String> current = null;
        for (Vertex<String> vertex : graph.vertexSet()) {
            if (vertex.getUserObject().equals(fromCurrency)) {
                current = vertex;
                break;
            }
        }
        System.out.print(current.getUserObject());
        Vertex<String> previous = null;
        double multiplier = 1.0;
        boolean hasNext = true;
        while (hasNext) {
            hasNext = false;
            for (WeightedEdge<String> edge : current.incidentEdges()) {
                Vertex<String> oppositeVertex = edge.oppositeVertex(current);
                if (oppositeVertex != previous) {
                    previous = current;
                    current = oppositeVertex;
                    multiplier *= edge.getWeight();
                    System.out.print(" --(" + String.format("%.4f", edge.getWeight()) + ")-> ");
                    System.out.print(current.getUserObject());
                    hasNext = true;
                    break;
                }
            }
        }
        System.out.println("\nMultiplier: x" + String.format("%.4f", multiplier));
        System.out.print(current.getUserObject());
        previous = null;
        multiplier = 1.0;
        hasNext = true;
        while (hasNext) {
            hasNext = false;
            for (WeightedEdge<String> edge : current.incidentEdges()) {
                Vertex<String> oppositeVertex = edge.oppositeVertex(current);
                if (oppositeVertex != previous) {
                    previous = current;
                    current = oppositeVertex;
                    multiplier *= edge.getWeight();
                    System.out.print(" <-(" + String.format("%.4f", edge.getWeight()) + ")-- ");
                    System.out.print(current.getUserObject());
                    hasNext = true;
                    break;
                }
            }
        }
        System.out.println("\nReturn Multiplier: x" + String.format("%.4f", multiplier));
    }

    public static void displayArbitrage(WeightedGraphADT<String> graph) {
        Vertex<String> start = graph.vertexSet().iterator().next();
        Vertex<String> current = start;
        Vertex<String> previous = null;
        WeightedEdge<String> returningEdge = null;
        String firstRow = "┌-> " + start.getUserObject();
        double multiplier = 1.0;
        do {
            for (WeightedEdge<String> edge : current.incidentEdges()) {
                Vertex<String> oppositeVertex = edge.oppositeVertex(current);
                if (oppositeVertex != previous || oppositeVertex == start) {
                    previous = current;
                    current = oppositeVertex;
                    returningEdge = edge;
                    if (current != start) {
                        multiplier *= edge.getWeight();
                        firstRow += " --(" + String.format("%.4f", edge.getWeight()) + ")-> ";
                        firstRow += current.getUserObject();
                    }
                    break;
                }
            }
        } while (current != start);
        firstRow += " --┐";
        System.out.println(firstRow);
        String secondRowStart = "└-- " + start.getUserObject();
        secondRowStart += " <-(" + String.format("%.4f", returningEdge.getWeight()) + ")-- ";
        secondRowStart += previous.getUserObject();
        secondRowStart += " <-";
        System.out.print(secondRowStart);
        for (int i = 1; i < firstRow.length() - secondRowStart.length(); i++) {
            System.out.print("-");
        }
        System.out.println("┘");
        System.out.println("Per-loop Multiplier: x" + multiplier * returningEdge.getWeight());
    }
}
