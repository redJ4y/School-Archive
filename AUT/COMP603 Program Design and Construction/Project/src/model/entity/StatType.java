package model.entity;

/*
The StatType enum specifies a type of stat and stores its string equivalent.
 */
// @author Jared Scholz
import java.io.Serializable;

public enum StatType implements Serializable {
    HP("Health"),
    DM("Damage Modifier"),
    APM("Armor Piercing Modifier"),
    PROT("Protection"),
    AGIL("Agility");

    private static final long serialVersionUID = 10007L;

    private final String text;

    private StatType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
