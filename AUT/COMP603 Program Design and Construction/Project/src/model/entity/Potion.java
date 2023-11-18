package model.entity;

/*
A Potion is a type of Item. Potions may be consumed to modify player stats.
 */
// @author Jared Scholz
import java.io.Serializable;

public class Potion extends Item implements Serializable {

    private static final long serialVersionUID = 10006L;

    private StatType stat;
    private int modification;

    public Potion(String name, String description, int rarity, StatType stat, int modification) {
        super(ItemType.POTION, name, description, rarity);
        this.stat = stat;
        this.modification = modification;
    }

    /* Constructor to clone a base potion */
    public Potion(Potion basePotion) {
        this(basePotion.getName(), basePotion.getDescription(), basePotion.getRarity(), basePotion.getStat(), basePotion.getModification());
    }

    public void applyPotion(EntityStats targetStats) {
        targetStats.modifyStat(stat, modification);
    }

    public StatType getStat() {
        return stat;
    }

    public int getModification() {
        return modification;
    }

    public String getSpecsString() {
        String plusOrMinus = modification < 0 ? "-" : "+";
        return plusOrMinus + modification + " " + stat.toString();
    }

    @Override
    public String toInventoryString() {
        String plusOrMinus = modification < 0 ? "-" : "+";
        return super.toInventoryString() + "\n " + plusOrMinus + modification + " " + stat.toString();
    }

    @Override
    public String toInventoryString(int coins) {
        String plusOrMinus = modification < 0 ? "-" : "+";
        return super.toInventoryString(coins) + "\n " + plusOrMinus + modification + " " + stat.toString();
    }

    @Override
    public String toString() {
        return super.toString() + ", " + stat.name() + ", " + modification + "]";
    }
}
