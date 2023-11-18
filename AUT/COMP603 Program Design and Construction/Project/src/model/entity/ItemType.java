package model.entity;

/*
This enum is used to distinguish between different Items
 */
// @author Jared Scholz
import java.io.Serializable;

public enum ItemType implements Serializable {
    WEAPON, ARMOR, POTION; // hard-coded types, not to be extended!

    private static final long serialVersionUID = 10005L;
}
