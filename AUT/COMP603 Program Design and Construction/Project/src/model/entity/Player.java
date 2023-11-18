package model.entity;

/*
A Player object stores all of the players data during gameplay.
It provides some methods for the driver, but often the driver will access fields directly through getters/setters.
 */
// @author Jared Scholz
import java.awt.Point;
import java.util.Random;

public class Player {

    private final String name;
    private final EntityStats stats;
    private Point position;
    private final TravelMap travelMap;
    private final Inventory inventory;
    private int coins;

    public Player(String name, int x, int y) {
        this.name = name;
        stats = new EntityStats(100, 0, 0, 0, 0);
        position = new Point(x, y);
        travelMap = new TravelMap();
        inventory = new Inventory();
        coins = 100;

        Item starterWeapon = new Weapon("Pocket Dagger", "You kept it from a past life...", 2, 5, 20, 0, 15);
        inventory.addItem(starterWeapon);
        inventory.setEquippedWeapon(0);
    }

    /* Constructor for cloning a base player (used for converting game save to Player object) */
    public Player(String name, EntityStats stats, Point position, TravelMap travelMap, Inventory inventory, int coins) {
        this.name = name;
        this.stats = stats;
        this.position = position;
        this.travelMap = travelMap;
        this.inventory = inventory;
        this.coins = coins;
    }

    public int getAttack(EntityStats enemyStats) { // returns -1 if attack was dodged
        Weapon weapon = inventory.getEquippedWeapon();
        if (weapon == null) {
            return 1; // punch does exactly 1 damage (no weapon equipped)
        }
        return weapon.getAttack(enemyStats, stats.getDamageModifier(), stats.getArmorPiercingModifier());
    }

    /* Gives the player coins according to the rarity of the enemy defeated, returns the number added */
    public int addCoins(int defeatedEnemyRarity) {
        Random randGen = new Random();
        int addition = 0;
        addition += randGen.nextInt(101) + (defeatedEnemyRarity - 1) * 100; // value between (rarity-1)*100 and rarity*100
        addition += 10; // keep the average coins gained above average merchant prices
        coins += addition;
        return addition;
    }

    public void removeCoins(int amount) {
        coins -= amount;
        if (coins < 0) { // this should never occur
            coins = 0;
        }
    }

    public void move(Point newPosition) {
        position = newPosition;
    }

    public boolean hasWeapon() {
        return inventory.getEquippedWeapon() != null;
    }

    public int getDamageMin() { // damageMin from equipped weapon
        if (!hasWeapon()) {
            return 1;
        }
        return inventory.getEquippedWeapon().getDamageMin();
    }

    public int getDamageMax() { // damageMax from equipped weapon
        if (!hasWeapon()) {
            return 1;
        }
        return inventory.getEquippedWeapon().getDamageMax();
    }

    public String getName() {
        return name;
    }

    public EntityStats getStats() {
        return stats; // stats are modified in GameDriver
    }

    public Point getPosition() {
        return position; // position is modified in GameDriver
    }

    public TravelMap getTravelMap() {
        return travelMap; // travelMap is modified in GameDriver
    }

    public Inventory getInventory() {
        return inventory; // inventory is modified in GameDriver
    }

    public int getCoins() {
        return coins;
    }
}
