package model.map;

import controller.GameDriver;
import java.awt.Point;
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
public class GameMapTest {

    private GameMap map;

    public GameMapTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        DBManager dataKeeper = new DBManager();
        dataKeeper.initializeData();
        map = new GameMap(GameDriver.MAP_SIZE, dataKeeper);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test that map was initialized correctly.
     */
    @Test
    public void testMapInitialized() {
        System.out.println("testMapInitialized");
        // go through every cell...
        for (int i = 0; i < GameDriver.MAP_SIZE; i++) {
            for (int j = 0; j < GameDriver.MAP_SIZE; j++) {
                Scene scene = map.getScene(new Point(i, j));

                // make sure the scene can select an event
                assertNotSame(null, scene.pickEvent());
                // make sure the scene was given view text
                assertNotSame(null, scene.getView());
                // make sure the scene was given description text
                assertNotSame(null, scene.getDescription());
                // make sure the EnemyType was read correctly
                assertTrue(scene.getEnemyType() instanceof EnemyType);
                // make sure there is at least 1 possible trap
                assertTrue(!scene.getPossibleTraps().isEmpty());
                // make sure there is at least 1 possible merchant
                assertTrue(!scene.getPossibleMerchants().isEmpty());
                // make sure there is at least 1 possible passive event
                assertTrue(!scene.getPossiblePassiveEvents().isEmpty());
            }
        }
    }

}
