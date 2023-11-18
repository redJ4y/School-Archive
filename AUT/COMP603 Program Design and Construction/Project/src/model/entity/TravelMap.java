package model.entity;

/*
The TravelMap is a data structure used to keep track of player movement history.
 */
// @author Jared Scholz
import controller.GameDriver;
import java.awt.Point;
import java.io.Serializable;

public class TravelMap implements Serializable {

    private static final long serialVersionUID = 10008L;

    private final int size;
    private final char[][] history;

    public TravelMap() {
        size = GameDriver.MAP_SIZE;
        history = new char[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                history[i][j] = ' ';
            }
        }
    }

    public void setVisited(Point position) {
        if (history[position.x][position.y] != 'X') {
            history[position.x][position.y] = 'O';
        }
    }

    public void setDefeated(Point position) {
        history[position.x][position.y] = 'X';
    }

    public char[][] getArray() {
        return history;
    }

    public String toDisplayString(Point currentPosition) {
        String toReturn = getHorizontalBorder();
        for (int i = 0; i < size; i++) {
            toReturn += "| ";
            for (int j = 0; j < size; j++) {
                if (i == currentPosition.x && j == currentPosition.y) {
                    toReturn += "# ";
                } else {
                    toReturn += history[i][j] + " ";
                }
            }
            toReturn += "|";
            // print compass (doesn't need to be a fast algorithm):
            if (i == size / 2 - 1) {
                toReturn += "    N";
            } else if (i == size / 2) {
                toReturn += "  W + E";
            } else if (i == size / 2 + 1) {
                toReturn += "    S";
            }
            toReturn += "\n";
        }
        toReturn += getHorizontalBorder();
        toReturn += "X Defeated | O Visited | # Current\n";
        return toReturn;
    }

    private String getHorizontalBorder() {
        String horizontalBorder = "+";
        for (int i = 0; i < size * 2 + 1; i++) {
            horizontalBorder += "-";
        }
        return (horizontalBorder + "+\n");
    }
}
