package model.entity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jared
 */
public class InventoryTest {

    private Inventory inventory;
    // set up inventory state:
    private Item weapon; // index 0 (equipped)
    private Item weapon2; // index 1
    private Item armor; // index 2 (equipped)
    private Item armor2; // index 3
    private Item potion; // index 4

    public InventoryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        inventory = new Inventory();

        weapon = new Weapon("weapon", "test", 0, 0, 0, 0, 0);
        inventory.addItem(weapon);
        inventory.setEquippedWeapon(0);

        weapon2 = new Weapon("weapon2", "test", 0, 0, 0, 0, 0);
        inventory.addItem(weapon2);

        armor = new Armor("armor", "test", 0, 0, 0, 0);
        inventory.addItem(armor);
        inventory.setEquippedArmor(2);

        armor2 = new Armor("armor2", "test", 0, 0, 0, 0);
        inventory.addItem(armor2);

        potion = new Potion("potion", "test", 0, StatType.HP, 0);
        inventory.addItem(potion);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of equipOrConsume method, of class Inventory.
     */
    @Test
    public void testEquipOrConsume() {
        System.out.println("equipOrConsume");
        EntityStats targetStats = new EntityStats(0, 0, 0, 0, 0);

        System.out.println("test already equipped");
        assertFalse(inventory.equipOrConsume(0, targetStats));
        assertFalse(inventory.equipOrConsume(2, targetStats));

        System.out.println("test equip");
        assertTrue(inventory.equipOrConsume(1, targetStats));
        assertEquals(weapon2, inventory.getEquippedWeapon());
        assertTrue(inventory.equipOrConsume(3, targetStats));
        assertEquals(armor2, inventory.getEquippedArmor());

        System.out.println("test consume");
        assertTrue(inventory.equipOrConsume(4, targetStats));
        assertSame(4, inventory.getItems().size());
    }

    /**
     * Test of remove method, of class Inventory.
     */
    @Test
    public void testRemove() {
        System.out.println("remove");

        System.out.println("test equipped item index shifting");
        inventory.remove(1);
        assertEquals(armor, inventory.get(1)); // ensure that it is in place
        assertEquals(armor, inventory.getEquippedArmor()); // ensure that the index shifted

        System.out.println("test remove equipped item");
        inventory.remove(0);
        assertSame(null, inventory.getEquippedWeapon());
    }

    /**
     * Test of addItem method, of class Inventory.
     */
    @Test
    public void testAddItem() {
        System.out.println("addItem (ensure that inventory fills up)");
        assertTrue(inventory.addItem(new Weapon("test", "test", 0, 0, 0, 0, 0)));
        assertTrue(inventory.addItem(new Weapon("test", "test", 0, 0, 0, 0, 0)));
        assertTrue(inventory.addItem(new Weapon("test", "test", 0, 0, 0, 0, 0)));
        assertTrue(inventory.addItem(new Weapon("test", "test", 0, 0, 0, 0, 0)));
        assertTrue(inventory.addItem(new Weapon("test", "test", 0, 0, 0, 0, 0)));
        // INVENTORY FULL
        assertFalse(inventory.addItem(new Weapon("test", "test", 0, 0, 0, 0, 0)));
    }

    /**
     * Test of setEquippedWeapon method, of class Inventory.
     */
    @Test
    public void testSetEquippedWeapon() {
        System.out.println("setEquippedWeapon");
        assertFalse(inventory.setEquippedWeapon(4)); // can't equip potion
        assertTrue(inventory.setEquippedWeapon(1));
        assertEquals(weapon2, inventory.getEquippedWeapon());
    }

    /**
     * Test of setEquippedArmor method, of class Inventory.
     */
    @Test
    public void testSetEquippedArmor() {
        System.out.println("setEquippedArmor");
        assertFalse(inventory.setEquippedArmor(4)); // can't equip potion
        assertTrue(inventory.setEquippedArmor(3));
        assertEquals(armor2, inventory.getEquippedArmor());
    }

}
