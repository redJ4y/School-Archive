package model.entity;

/*
Entities (player and enemies) each use an instance of EntityStats to store their stats.
 */
// @author Jared Scholz
import java.io.Serializable;

public class EntityStats implements Serializable {

    private static final long serialVersionUID = 10002L;

    private int health; // HP
    private int damageModifier; // DM
    private int armorPiercingModifier; // APM
    private int protection; // PROT
    private int agility; // AGIL

    public EntityStats(int health, int damageModifier, int armorPiercingModifier, int protection, int agility) {
        this.health = health;
        this.damageModifier = damageModifier;
        this.armorPiercingModifier = armorPiercingModifier;
        this.protection = protection;
        this.agility = agility;
    }

    /* Modifies the correct stat (specified by StatType enum) by the amount specified */
    public void modifyStat(StatType stat, int amount) {
        switch (stat) {
            case HP:
                health += amount;
                if (health < 0) {
                    health = 0;
                }
                break;
            case DM:
                damageModifier += amount;
                if (damageModifier < 0) {
                    damageModifier = 0;
                }
                break;
            case APM:
                armorPiercingModifier += amount;
                if (armorPiercingModifier < 0) {
                    armorPiercingModifier = 0;
                }
                break;
            case PROT:
                protection += amount;
                if (protection < 0) {
                    protection = 0;
                }
                break;
            case AGIL:
                agility += amount;
                if (agility < 0) {
                    agility = 0;
                }
                break;
        } // stats may not go below 0
    }

    public int getHealth() {
        return health;
    }

    public int getDamageModifier() {
        return damageModifier;
    }

    public int getArmorPiercingModifier() {
        return armorPiercingModifier;
    }

    public int getProtection() {
        return protection;
    }

    public int getAgility() {
        return agility;
    }

    public String toDisplayString(String name, Armor equippedArmor) {
        int protectionBonus = equippedArmor == null ? 0 : equippedArmor.getProtectionBonus();
        int agilityBonus = equippedArmor == null ? 0 : equippedArmor.getAgilityBonus();
        String displayString = name + "'s Stats:\n";
        displayString += health + " [Health]\n";
        displayString += damageModifier + " [Damage Modifier]\n";
        displayString += armorPiercingModifier + " [Armor Piercing Modifier]\n";
        displayString += String.format("%d %+d", protection, protectionBonus) + " [Protection]\n";
        displayString += String.format("%d %+d", agility, agilityBonus) + " [Agility]\n";
        return displayString;
    }

    @Override
    public String toString() {
        return String.format("(Stats:HP=%d,DM=%d,APM=%d,PROT=%d,AGIL=%d)", health, damageModifier, armorPiercingModifier, protection, agility);
    }
}
