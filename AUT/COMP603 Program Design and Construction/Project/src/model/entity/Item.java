package model.entity;

/*
Item is the abstract parent class of Weapons, Armor, and Potions.
This class provides universal functionality to the items.
This class is used heavily for polymorphism.
(All items share a name and description)
 */
// @author Jared Scholz
import controller.GameDriver;
import java.io.Serializable;

public abstract class Item implements Serializable {

    private static final long serialVersionUID = 10004L;

    private final ItemType type;
    private String name;
    private final String description;
    private final int rarity;

    public Item(ItemType type, String name, String description, int rarity) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
    }

    protected void setBroken() {
        GameDriver.notifyOfBrokenItem(name);
        name = "Broken " + name;
    }

    public ItemType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getRarity() {
        return rarity;
    }

    public String toInventoryString() {
        return "<" + type.name().charAt(0) + "> " + name + "\n ~ \"" + description + "\"";
    }

    public String toInventoryString(int coins) {
        return "<" + type.name().charAt(0) + "> " + name + " for " + coins + " coins!\n ~ \"" + description + "\"";
    }

    @Override
    public String toString() {
        return "[" + type.name() + ", " + name + ", " + description + ", " + rarity;
    }
}
