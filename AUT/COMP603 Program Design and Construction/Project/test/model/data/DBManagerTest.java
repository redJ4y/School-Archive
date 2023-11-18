package model.data;

import model.entity.Player;
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
public class DBManagerTest {

    private static final String UNIQUE_USERNAME = "DBManagerTest_95154166";

    public DBManagerTest() {
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
     * Test of checkGameSave method, of class DBManager.
     */
    @Test
    public void testCheckGameSave() {
        System.out.println("checkGameSave");
        DBManager db = new DBManager();

        boolean firstCheck = db.checkGameSave(UNIQUE_USERNAME);
        db.saveGame(null, new Player(UNIQUE_USERNAME, 0, 0));
        boolean secondCheck = db.checkGameSave(UNIQUE_USERNAME); // after game saved
        // ensure that data was initialized when a new user was submitted:
        boolean itemsInitialized = !db.getAllItems().isEmpty();
        // clean up (before assertions):
        db.deleteGameSave();

        assertFalse(firstCheck);
        assertTrue(secondCheck); // after game saved
        assertTrue(itemsInitialized);
    }

    /**
     * Test of deleteGameSave method, of class DBManager.
     */
    @Test
    public void testDeleteGameSave() {
        System.out.println("deleteGameSave");
        DBManager db = new DBManager();

        db.checkGameSave(UNIQUE_USERNAME); // give db the username
        db.saveGame(null, new Player(UNIQUE_USERNAME, 0, 0)); // save
        db.deleteGameSave(); // delete save

        assertFalse(db.checkGameSave(UNIQUE_USERNAME)); // game save should not be found
    }

    /**
     * Test of getAllItems method, of class DBManager.
     */
    @Test
    public void testGetAllItems() {
        System.out.println("getAllItems");
        DBManager db = new DBManager();
        db.initializeData();
        assertFalse(db.getAllItems().isEmpty());
    }

    /**
     * Test of getAllEnemies method, of class DBManager.
     */
    @Test
    public void testGetAllEnemies() {
        System.out.println("getAllEnemies");
        DBManager db = new DBManager();
        db.initializeData();
        assertFalse(db.getAllEnemies().isEmpty());
    }

    /**
     * Test of getAllMerchants method, of class DBManager.
     */
    @Test
    public void testGetAllMerchants() {
        System.out.println("getAllMerchants");
        DBManager db = new DBManager();
        db.initializeData();
        assertFalse(db.getAllMerchants().isEmpty());
    }

    /**
     * Test of getAllPassiveEvents method, of class DBManager.
     */
    @Test
    public void testGetAllPassiveEvents() {
        System.out.println("getAllPassiveEvents");
        DBManager db = new DBManager();
        db.initializeData();
        assertFalse(db.getAllPassiveEvents().isEmpty());
    }

    /**
     * Test of getAllTraps method, of class DBManager.
     */
    @Test
    public void testGetAllTraps() {
        System.out.println("getAllTraps");
        DBManager db = new DBManager();
        db.initializeData();
        assertFalse(db.getAllTraps().isEmpty());
    }

    /**
     * Test of getAllScenes method, of class DBManager.
     */
    @Test
    public void testGetAllScenes() {
        System.out.println("getAllScenes");
        DBManager db = new DBManager();
        db.initializeData();
        assertFalse(db.getAllScenes().isEmpty());
    }

}
