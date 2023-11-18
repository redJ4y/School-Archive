package model.map;

/*
The Event class is the abstract parent of Enemys, Merchants, PassiveEvents, and Traps
This class provides universal functionality to the events.
This class is used heavily for polymorphism.
(All events share a name and description)
 */
// @author Jared Scholz
import java.io.Serializable;

public abstract class Event implements Serializable {

    private static final long serialVersionUID = 10017L;

    private final EventType type;
    private final String name;
    private final String description;

    public Event(EventType type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EventType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "[" + type.name() + ", " + name + ", " + description;
    }
}
