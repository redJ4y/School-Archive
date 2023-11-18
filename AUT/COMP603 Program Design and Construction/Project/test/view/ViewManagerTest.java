package view;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
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
public class ViewManagerTest {

    private ViewManager viewManager;

    public ViewManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        viewManager = new ViewManager(null);
        viewManager.display(); // starts task runner thread
    }

    @After
    public void tearDown() {
        viewManager.stopTaskRunner();
        ((JFrame) SwingUtilities.getWindowAncestor(viewManager)).dispose();
        viewManager = null;
    }

    /**
     * Test of testTaskRunnerFunctionality method, of class ViewManager.
     *
     * Extra time is given to account for random variation
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testTaskRunnerFunctionality() throws InterruptedException {
        System.out.println("testTaskRunnerFunctionality count test");
        viewManager.addDelay(200); // start delay while more tasks are added to queue
        viewManager.displayText("test"); // 1
        viewManager.displayText("test"); // 2
        viewManager.displayText("test"); // 3
        viewManager.displayText("test"); // 4
        viewManager.displayText("test"); // 5
        viewManager.displayText("test"); // 6
        viewManager.displayText("test"); // 7
        Thread.sleep(75); // give enough time for the first delay to be dequeued
        assertSame(7, viewManager.getNumTasks()); // ensure that all 7 tasks are in the queue

        System.out.println("testTaskRunnerFunctionality deque test");
        Thread.sleep(300); // ensure that the first delay AND (at least) the first display text are completed
        if (viewManager.getNumTasks() > 6) { // expect one OR MORE tasks to be complete
            fail("task runner is not dequeuing");
        }
        Thread.sleep(700);
        assertSame(0, viewManager.getNumTasks()); // make sure ALL tasks finish

        // test that the count goes down when something is dequed but in progress:
        viewManager.addDelay(200);
        Thread.sleep(75); // give enough time for the delay to be dequeued
        assertSame(0, viewManager.getNumTasks()); // ensure that the currently running task is not counted
    }

    /**
     * Test of testUpdatePlayerInfoDirectly method, of class ViewManager.
     *
     * Extra time is given to account for random variation
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testUpdatePlayerInfoDirectly() throws InterruptedException {
        System.out.println("testUpdatePlayerInfoDirectly");
        viewManager.addDelay(200); // start delay while more tasks are added to queue
        viewManager.addDelay(1000);
        viewManager.updatePlayerInfoDirectly(new Player("test", 0, 0));

        Thread.sleep(75); // give enough time for the first delay to be dequeued
        assertSame(3, viewManager.getNumTasks()); // update player task should be placed at the beginning AND end of queue
        // should look like: delay 200 (in progress) -> update player (1) -> delay 1000 (2) -> update player (3)

        Thread.sleep(300);
        assertSame(1, viewManager.getNumTasks()); // the first delay and update player should be done
        // should look like: delay 1000 (in progress) -> update player (1)

        Thread.sleep(1200);
        assertSame(0, viewManager.getNumTasks()); // make sure tasks finish
    }

}
