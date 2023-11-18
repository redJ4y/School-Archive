package model.map;

/*
The Merchant is a type of Event. Merchants have an inventory (with prices) that the player may purchase from.
 */
// @author Jared Scholz
import model.entity.Armor;
import model.entity.Item;
import model.entity.Potion;
import model.entity.Weapon;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Merchant extends Event implements Serializable {

    private static final long serialVersionUID = 10013L;

    private int invrarity; // rarity of inventory
    private List<Item> inventory;
    private List<Integer> prices; // store prices parallel to inventory

    public Merchant(String name, String description, int invrarity) {
        super(EventType.MERCHANT, name, description);
        this.invrarity = invrarity;
    }

    /* Constructor to clone a base merchant */
    public Merchant(Merchant baseMerchant) {
        this(baseMerchant.getName(), baseMerchant.getDescription(), baseMerchant.getInvrarity());
    }

    /* Fill inventory with a random number of random items */
    public void initializeInventory(List<Item> allItems, Random randGen) {
        inventory = new ArrayList<>(4);
        prices = new ArrayList<>(4);

        List<Item> filteredItems = new ArrayList<>(allItems);
        filteredItems.removeIf(n -> (n.getRarity() != invrarity)); // filteredItems now only contains items of correct rarity
        for (int invSize = randGen.nextInt(4) + 1; invSize > 0; invSize--) { // loop between 1 and 4 times (random)
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
            inventory.add(newItem); // add random items (duplicates allowed)
            prices.add(randGen.nextInt(101) + (invrarity - 1) * 100); // assign each item a price between (invrarity-1)*100 and invrarity*100
        }
    }

    public String toDisplayString() {
        String displayString = "The " + super.getName() + " offers you a selection of items:\n";
        displayString += ">---------------------------------<";
        int index = 1;
        for (Item current : inventory) {
            displayString += "\n" + index + ": " + current.toInventoryString(prices.get(index - 1)) + "\n";
            index++;
        }
        displayString += ">---------------------------------<\n";
        displayString += "<W> Weapon | <A> Armor | <P> Potion\n";
        return displayString;
    }

    public void removeItem(int index) { // index must already be validated
        inventory.remove(index);
        prices.remove(index);
    }

    public Item getItem(int index) { // index must already be validated
        return inventory.get(index);
    }

    public int getPrice(int index) { // index must already be validated
        return prices.get(index);
    }

    public int inventorySize() {
        return inventory.size();
    }

    public boolean hasItems() {
        return !inventory.isEmpty();
    }

    public int getInvrarity() {
        return invrarity;
    }

    public List<Item> getItems() {
        return inventory;
    }

    public List<Integer> getPrices() {
        return prices;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + invrarity + "]";
    }
}
