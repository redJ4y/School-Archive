package model.entity;

/*
Armor is a type of Item. Armor provides the player with bonus protection and agility.
 */
// @author Jared Scholz
import java.io.Serializable;

public class Armor extends Item implements Serializable {

    private static final long serialVersionUID = 10001L;

    private int protection;
    private int agility;
    private int durability;

    public Armor(String name, String description, int rarity, int protection, int agility, int durability) {
        super(ItemType.ARMOR, name, description, rarity);
        this.protection = protection;
        this.agility = agility;
        this.durability = durability;
    }

    /* Constructor for cloning a base Armor */
    public Armor(Armor baseArmor) {
        this(baseArmor.getName(), baseArmor.getDescription(), baseArmor.getRarity(), baseArmor.getProtectionBonus(), baseArmor.getAgilityBonus(), baseArmor.getDurability());
    }

    public int getProtectionBonus() {
        return protection;
    }

    public int getAgilityBonus() {
        return agility;
    }

    public void decreaseDurability() {
        durability--;
        if (durability < 1) {
            durability = 0;

            if (!getName().split(" ")[0].equals("Broken")) { // only break once
                setBroken();
                protection /= 10; // greatly reduce protection
                agility /= 10; // greatly reduce agility
            }
        }
    }

    public int getDurability() {
        return durability;
    }

    public String getSpecsString() {
        return "+" + protection + " Protection | +" + agility + " Agility | " + durability + " Durability";
    }

    @Override
    public String toInventoryString() {
        return super.toInventoryString() + "\n +" + protection + " Protection | +" + agility + " Agility | " + durability + " Durability";
    }

    @Override
    public String toInventoryString(int coins) {
        return super.toInventoryString(coins) + "\n +" + protection + " Protection | +" + agility + " Agility | " + durability + " Durability";
    }

    @Override
    public String toString() {
        return super.toString() + ", " + protection + ", " + agility + ", " + durability + "]";
    }
}
