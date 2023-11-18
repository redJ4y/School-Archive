package model.map;

/*
The Trap is a type of Event. Traps may subtract a value from a player stat.
 */
// @author Jared Scholz
import model.entity.StatType;
import java.io.Serializable;

public class Trap extends Event implements Serializable {

    private static final long serialVersionUID = 10016L;

    StatType stat;
    int modification;

    public Trap(String description, StatType stat, int modification) {
        super(EventType.TRAP, "Trap!", description); // description unused
        this.stat = stat;
        this.modification = modification;
    }

    public StatType getStat() {
        return stat;
    }

    public int getModification() {
        return modification;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + stat.name() + ", " + modification + "]";
    }
}
