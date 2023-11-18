package controller;

/*
The Direction enum stores a direction (N/S/E/W) and provides direction functionality.
 */
// @author Jared Scholz
import java.awt.Point;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    public static Direction charToDirection(char input) {
        switch (input) {
            case 'n':
                return NORTH;
            case 's':
                return SOUTH;
            case 'e':
                return EAST;
            case 'w':
                return WEST;
            default:
                return null;
        }
    }

    /* Convert a movement direction into an actual Point modification */
    public Point getChange(Point start) {
        Point result = new Point(start);
        switch (this) {
            case NORTH:
                result.x--;
                break;
            case SOUTH:
                result.x++;
                break;
            case EAST:
                result.y++;
                break;
            case WEST:
                result.y--;
                break;
        }
        return result; // x and y are arbitrary
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
