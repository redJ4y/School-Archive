package model.entity;

/*
A Weapon is a type of Item. The weapon is used to deal damage to enemies.
 */
// @author Jared Scholz
import java.io.Serializable;
import java.util.Random;

public class Weapon extends Item implements Serializable {

    private static final long serialVersionUID = 10009L;

    private int damageMin, damageMax;
    private int armorPiercing;
    private int durability;

    public Weapon(String name, String description, int rarity, int damageMin, int damageMax, int armorPiercing, int durability) {
        super(ItemType.WEAPON, name, description, rarity);
        this.damageMin = damageMin;
        this.damageMax = damageMax;
        this.armorPiercing = armorPiercing;
        this.durability = durability;
    }

    /* Constructor to clone a base weapon */
    public Weapon(Weapon baseWeapon) {
        this(baseWeapon.getName(), baseWeapon.getDescription(), baseWeapon.getRarity(), baseWeapon.getDamageMin(), baseWeapon.getDamageMax(), baseWeapon.getArmorPiercing(), baseWeapon.getDurability());
    }

    public int getAttack(EntityStats targetStats, int damageModifier, int armorPiercingModifier) { // returns -1 if attack was dodged
        Random randGen = new Random();

        int targetProtection = targetStats.getProtection();
        int targetAgility = targetStats.getAgility();
        armorPiercingModifier += armorPiercing; // include weapon armor piercing

        int baseDamage = randGen.nextInt(damageMax - damageMin + 1) + damageMin;
        int calculatedDamage = baseDamage + damageModifier - Math.max(targetProtection - armorPiercingModifier, 0);
        int actualDamage = Math.max(calculatedDamage, baseDamage / 2); // cannot deal less than half of baseDamage

        if (randGen.nextInt(100) < targetAgility) { // agility effectively maxes out at 100
            if (randGen.nextInt(2) == 0) { // probability becomes (agility / 100) * 0.5
                return -1;                 // e.g. agility >= 100 results in a 50% chance of dodging
            }
        }

        decreaseDurability(); // do not decrease durability if dodged
        return actualDamage;
    }

    private void decreaseDurability() {
        durability--;
        if (durability < 1) {
            durability = 0;

            if (!getName().split(" ")[0].equals("Broken")) { // only break once
                setBroken();
                damageMin = 0;
                damageMax /= 10; // greatly reduce damageMax
                armorPiercing /= 10; // greatly reduce armorPiercing
            }
        }
    }

    public int getDamageMin() {
        return damageMin;
    }

    public int getDamageMax() {
        return damageMax;
    }

    public int getArmorPiercing() {
        return armorPiercing;
    }

    public int getDurability() {
        return durability;
    }

    public String getSpecsString() {
        return damageMin + "-" + damageMax + " Damage | " + armorPiercing + " AP | " + durability + " Durability";
    }

    @Override
    public String toInventoryString() {
        return super.toInventoryString() + "\n " + damageMin + "-" + damageMax + " Damage | " + armorPiercing + " AP | " + durability + " Durability";
    }

    @Override
    public String toInventoryString(int coins) {
        return super.toInventoryString(coins) + "\n " + damageMin + "-" + damageMax + " Damage | " + armorPiercing + " AP | " + durability + " Durability";
    }

    @Override
    public String toString() {
        return super.toString() + ", " + damageMin + "-" + damageMax + ", " + armorPiercing + ", " + durability + "]";
    }
}
