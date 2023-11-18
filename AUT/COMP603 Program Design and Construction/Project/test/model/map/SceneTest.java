package model.map;

import java.util.Random;
import model.data.DBManager;
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
public class SceneTest {

    private static DBManager dataKeeper;

    public SceneTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        dataKeeper = new DBManager();
        dataKeeper.initializeData();
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
     * Test of pickEvent method, of class Scene.
     */
    @Test
    public void testPickEvent() {
        System.out.println("pickEvent");
        Scene scene = new Scene("test", "test", EnemyType.DRAGON);
        scene.initializeBattle(dataKeeper.getAllEnemies(), dataKeeper.getAllItems(), new Random());
        scene.addTrap(dataKeeper.getAllTraps().get(0));
        System.out.println("ensure that battle is favored over other events (2x or more)");
        int battleCount = 0;
        int otherCount = 0;
        for (int i = 0; i < 100000; i++) {
            Event event = scene.pickEvent();
            if (event instanceof Enemy) {
                battleCount++;
            } else {
                otherCount++;
            }
        }
        System.out.println("(battles: " + battleCount + ", other: " + otherCount + ")");
        assertTrue(battleCount >= otherCount * 2);
    }

    /**
     * Test of initializeBattle method, of class Scene.
     */
    @Test
    public void testInitializeBattle() {
        System.out.println("initializeBattle");
        Scene scene = new Scene("test", "test", EnemyType.DRAGON);
        scene.initializeBattle(dataKeeper.getAllEnemies(), dataKeeper.getAllItems(), new Random());
        Event event = scene.pickEvent();
        assertTrue(event instanceof Enemy);
        Enemy enemy = (Enemy) event;
        // ensure enemy is of the correct type
        assertSame(EnemyType.DRAGON, enemy.getEnemyType());
        // ensure ints are correct
        assertTrue(enemy.getDamageMin() >= 0);
        assertTrue(enemy.getDamageMax() > 0);
        assertTrue(enemy.getRarity() > 0);
        // ensure objects were initialized
        assertNotNull(enemy.getName());
        assertNotNull(enemy.getDescription());
        assertNotNull(enemy.getLoot());
        assertNotNull(enemy.getStats());
    }

    /**
     * Test of initializeMerchants method, of class Scene.
     */
    @Test
    public void testInitializeMerchants() {
        System.out.println("initializeMerchants");
        Scene scene = new Scene("test", "test", EnemyType.DRAGON);
        scene.addMerchant(dataKeeper.getAllMerchants().get(0));
        scene.initializeMerchants(dataKeeper.getAllItems(), new Random());
        assertTrue(!scene.getPossibleMerchants().iterator().next().getItems().isEmpty());
    }

}
