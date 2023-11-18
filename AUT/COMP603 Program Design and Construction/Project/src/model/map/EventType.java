package model.map;

/*
The EventType enum is used to distinguish between Events.
 */
// @author Jared Scholz
import java.io.Serializable;

public enum EventType implements Serializable {
    BATTLE, MERCHANT, PASSIVE_EVENT, TRAP; // hard-coded types, do not extend!

    private static final long serialVersionUID = 10018L;
}
