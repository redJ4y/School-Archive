package model.entity;

/*
The player uses an Inventory to store items and keep track of equipped items.
 */
// @author Jared Scholz
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Inventory implements Serializable {

    private static final long serialVersionUID = 10003L;

    public final int MAX_SIZE = 10;

    private final List<Item> items;
    private int equippedWeapon;
    private int equippedArmor;

    public Inventory() {
        items = new ArrayList<>(MAX_SIZE);
        equippedWeapon = -1;
        equippedArmor = -1;
    }

    /* If index specifies a weapon or armor, equip it. If index specifies a potion, apply it to targetStats */
    public boolean equipOrConsume(int index, EntityStats targetStats) { // index must already be validated
        Item selectedItem = items.get(index);
        if (selectedItem instanceof Weapon) {
            if (equippedWeapon == index) {
                return false;
            }
            equippedWeapon = index;
        } else if (selectedItem instanceof Armor) {
            if (equippedArmor == index) {
                return false;
            }
            equippedArmor = index;
        } else if (selectedItem instanceof Potion) {
            ((Potion) selectedItem).applyPotion(targetStats);
            remove(index); // potion has been consumed
        }
        return true;
    } // returns boolean: false indicates item already equipped

    public Item get(int index) { // index must already be validated
        return items.get(index);
    }

    public void remove(int index) { // index must already be validated
        if (index == equippedWeapon) {
            equippedWeapon = -1;
        } else if (index == equippedArmor) {
            equippedArmor = -1;
        }
        items.remove(index);
        // move equipped item indexes to account for ArrayList shifting:
        if (index < equippedWeapon) {
            equippedWeapon--;
        }
        if (index < equippedArmor) {
            equippedArmor--;
        }
    }

    public boolean addItem(Item item) {
        if (items.size() < MAX_SIZE) {
            items.add(item);
            return true;
        } else {
            return false;
        }
    }

    public boolean setEquippedWeapon(int index) {
        if (items.get(index).getType() == ItemType.WEAPON) {
            equippedWeapon = index;
            return true;
        } else {
            return false;
        }
    }

    public boolean setEquippedArmor(int index) {
        if (items.get(index).getType() == ItemType.ARMOR) {
            equippedArmor = index;
            return true;
        } else {
            return false;
        }
    }

    public Weapon getEquippedWeapon() {
        if (equippedWeapon < 0) {
            return null;
        }
        return (Weapon) items.get(equippedWeapon);
    }

    public Armor getEquippedArmor() {
        if (equippedArmor < 0) {
            return null;
        }
        return (Armor) items.get(equippedArmor);
    }

    public int getEquippedWeaponIndex() {
        return equippedWeapon;
    }

    public int getEquippedArmorIndex() {
        return equippedArmor;
    }

    public List<Item> getItems() {
        return items;
    }

    public boolean hasSpace() {
        return items.size() < MAX_SIZE;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
