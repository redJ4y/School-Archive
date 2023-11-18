package model.map;

/*
The Enemy is a type of Event. There are multiple types of enemies specified by the EnemyType enum.
Enemies are entities (as well as Map objects) so they have EntityStats. Enemies are to be battled by the player.
 */
// @author Jared Scholz
import model.entity.Armor;
import model.entity.Item;
import model.entity.EntityStats;
import model.entity.Potion;
import model.entity.Weapon;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Enemy extends Event implements Serializable {

    private static final long serialVersionUID = 10010L;

    private EnemyType enemyType;
    private int rarity;
    private int damageMin;
    private int damageMax;
    private EntityStats stats;
    private Item loot;

    public Enemy(String name, String description, EnemyType enemyType, int rarity, int damageMin, int damageMax, int hp, int apm, int prot, int agil) {
        super(EventType.BATTLE, name, description);
        this.enemyType = enemyType;
        this.rarity = rarity;
        this.damageMin = damageMin;
        this.damageMax = damageMax;
        stats = new EntityStats(hp, 0, apm, prot, agil);
    }

    /* Constructor for cloning a base enemy */
    public Enemy(Enemy baseEnemy) {
        this(baseEnemy.getName(), baseEnemy.getDescription(), baseEnemy.getEnemyType(), baseEnemy.getRarity(), baseEnemy.getDamageMin(),
                baseEnemy.getDamageMax(), baseEnemy.getStats().getHealth(), baseEnemy.getStats().getArmorPiercingModifier(),
                baseEnemy.getStats().getProtection(), baseEnemy.getStats().getAgility());
    }

    public int getAttack(EntityStats playerStats, Armor playerArmor) { // returns -1 if attack was dodged
        Random randGen = new Random();

        int targetProtection = playerStats.getProtection();
        int targetAgility = playerStats.getAgility();
        if (playerArmor != null) {
            targetProtection += playerArmor.getProtectionBonus();
            targetAgility += playerArmor.getAgilityBonus();
            playerArmor.decreaseDurability();
        }
        int damageModifier = stats.getDamageModifier();
        int armorPiercingModifier = stats.getArmorPiercingModifier();

        int baseDamage = randGen.nextInt(damageMax - damageMin + 1) + damageMin;
        int calculatedDamage = baseDamage + damageModifier - Math.max(targetProtection - armorPiercingModifier, 0);
        int actualDamage = Math.max(calculatedDamage, baseDamage / 2); // cannot deal less than half of baseDamage

        if (randGen.nextInt(100) < targetAgility) { // agility effectively maxes out at 100
            if (randGen.nextInt(2) == 0) { // probability becomes (agility / 100) * 0.5
                return -1;                 // e.g. agility >= 100 results in a 50% chance of dodging
            }
        }
        return actualDamage;
    }

    /* Gives the enemy a random loot item (new clone) of the correct rarity */
    public void initializeLoot(List<Item> allItems, Random randGen) {
        List<Item> filteredItems = new ArrayList<>(allItems);
        filteredItems.removeIf(n -> (n.getRarity() != rarity)); // filteredItems now only contains items of correct rarity
        if (!filteredItems.isEmpty()) {
            Item baseItem = filteredItems.get(randGen.nextInt(filteredItems.size()));
            // clone the base item to make a new instance:
            Item newItem = null;
            switch (baseItem.getType()) {
                case ARMOR:
                    newItem = new Armor((Armor) baseItem);
                    break;
                case WEAPON:
                    newItem = new Weapon((Weapon) baseItem);
                    break;
                case POTION:
                    newItem = new Potion((Potion) baseItem);
                    break;
            }
            loot = newItem;
        } else { // this should never occur when finished
            // this is only for Jared when adding to the game files
            System.out.println("DEBUGGING: No items of rarity " + rarity + " in file!");
        }
    }

    public boolean isDead() {
        return stats.getHealth() <= 0;
    }

    public EnemyType getEnemyType() {
        return enemyType;
    }

    public int getRarity() {
        return rarity;
    }

    public int getDamageMin() {
        return damageMin;
    }

    public int getDamageMax() {
        return damageMax;
    }

    public EntityStats getStats() {
        return stats;
    }

    public Item getLoot() {
        return loot;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + enemyType.name() + ", " + rarity + ", " + damageMin + "-" + damageMax + ", " + stats.toString() + "]";
    }
}
