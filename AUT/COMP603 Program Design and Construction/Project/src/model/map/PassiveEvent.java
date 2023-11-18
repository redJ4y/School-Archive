package model.map;

/*
A PassiveEvent is a type of Event. PassiveEvents give the player a chance to regain HP.
 */
// @author Jared Scholz
import java.io.Serializable;

public class PassiveEvent extends Event implements Serializable {

    private static final long serialVersionUID = 10014L;

    int hpBonus;

    public PassiveEvent(String description, int hpBonus) {
        super(EventType.PASSIVE_EVENT, "...Nothing...", description);
        this.hpBonus = hpBonus;
    }

    public int getHpBonus() {
        return hpBonus;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + hpBonus + "]";
    }
}
