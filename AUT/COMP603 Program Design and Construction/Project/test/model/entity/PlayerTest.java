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
public class PlayerTest {

    public PlayerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAttack method, of class Player.
     *
     * Tests using the default weapon (Pocket Dagger)
     */
    @Test
    public void testGetAttack() {
        System.out.println("getAttack (default weapon: Pocket Dagger)");
        Player player = new Player("test", 0, 0);
        EntityStats enemyStats = new EntityStats(0, 0, 0, 0, 0);
        for (int i = 0; i < 100000; i++) { // try 100000 attacks
            int damage = player.getAttack(enemyStats);
            if (damage < 5 || damage > 20) { // ensure damage is expected
                fail("damage is not within range of [damageMin, damageMax]");
            }
            if (i % 10 == 0) { // make a new player for more weapon durability every 10 iterations
                player = new Player("test", 0, 0);
            }
        }

        System.out.println("getAttack with enemy protection (default weapon: Pocket Dagger)");
        player = new Player("test", 0, 0);
        enemyStats = new EntityStats(0, 0, 0, 10, 0);
        for (int i = 0; i < 100000; i++) { // try 100000 attacks
            int damage = player.getAttack(enemyStats);
            // 5 / 2 = 2 (new damageMin), 20 - 10 = 10 (new damageMax)
            if (damage < 2 || damage > 10) { // ensure damage is expected
                fail("damage is not within range of [damageMin, damageMax] accounting for enemy protection");
            }
            if (i % 10 == 0) { // make a new player for more weapon durability every 10 iterations
                player = new Player("test", 0, 0);
            }
        }
    }

    /**
     * Test of addCoins method, of class Player.
     */
    @Test
    public void testAddCoins() {
        System.out.println("testAddCoins enemy rarity 1");
        Player player = new Player("test", 0, 0);
        for (int i = 0; i < 100000; i++) { // try 100000 values
            int coins = player.addCoins(1);
            if (coins < 10 || coins > 110) {
                fail("coins given is not within expected range");
            }
            if (i % 100 == 0) { // make a new player occasionally to keep coin total down
                player = new Player("test", 0, 0);
            }
        }

        System.out.println("testAddCoins enemy rarity 2");
        for (int i = 0; i < 100000; i++) { // try 100000 values
            int coins = player.addCoins(2);
            if (coins < 110 || coins > 210) {
                fail("coins given is not within expected range");
            }
            if (i % 100 == 0) { // make a new player occasionally to keep coin total down
                player = new Player("test", 0, 0);
            }
        }

        System.out.println("testAddCoins enemy rarity 3");
        for (int i = 0; i < 100000; i++) { // try 100000 values
            int coins = player.addCoins(3);
            if (coins < 210 || coins > 310) {
                fail("coins given is not within expected range");
            }
            if (i % 100 == 0) { // make a new player occasionally to keep coin total down
                player = new Player("test", 0, 0);
            }
        }

        System.out.println("testAddCoins enemy rarity 4");
        for (int i = 0; i < 100000; i++) { // try 100000 values
            int coins = player.addCoins(4);
            if (coins < 310 || coins > 410) {
                fail("coins given is not within expected range");
            }
            if (i % 100 == 0) { // make a new player occasionally to keep coin total down
                player = new Player("test", 0, 0);
            }
        }
    }

}
