package view;

/*
The ViewManager manages all view components and all interaction with the controller.
It creates the frame and populates it. All JPanel Forms interact only with the ViewManager.
The ViewManager processes tasks given by the controller (stored in a BlockingDeque).
 */
// @author Jared Scholz
import controller.Direction;
import controller.GameDriver;
import model.entity.Item;
import model.entity.Player;
import model.map.Merchant;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class ViewManager extends JPanel {

    private final GameDriver gameDriver;

    private JPanel gameArea; // to use CardLayout
    private JTabbedPane playerArea;

    private final PregameMenuView pregameMenu;
    // gameArea panels:
    private GameplayView gameplayView;
    private MerchantView merchantView;
    private LootView lootView;
    // playerArea panels:
    private MapView mapView;
    private InventoryView inventoryView;
    private StatsView statsView;

    private final BlockingDeque<Runnable> tasks; // queue of tasks given by the driver
    // a BlockingDeque blocks the task runner until more tasks are added
    private final TaskRunner taskRunner; // private class below

    public ViewManager(GameDriver gameDriver) {
        super(new BorderLayout());
        this.gameDriver = gameDriver;
        tasks = new LinkedBlockingDeque<>(); // use a deque so that tasks can be added to the front
        taskRunner = new TaskRunner(); // started (new thread) in display()
        initializePanels();

        pregameMenu = new PregameMenuView(this);
        super.add(pregameMenu, BorderLayout.NORTH);
    }

    /* Private class TaskRunner:
    * A TaskRunner waits in a separate thread for tasks to be enqueued.
    * It then runs them in order.
     */
    private class TaskRunner implements Runnable {

        private boolean stop;

        public void requestStop() { // called when the window is closed
            stop = true;
        }

        @Override
        public void run() {
            stop = false;
            while (!stop) { // wait for tasks to show up
                try {
                    tasks.takeFirst().run();
                    Thread.sleep(50); // reduce CPU time and add default delay
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        }
    }

    public void stopTaskRunner() {
        taskRunner.requestStop();
    }

    public int getNumTasks() {
        return tasks.size();
    }

    /* Displays the GUI and starts the task running thread */
    public void display() {
        JFrame frame = new JFrame("RPG Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.setMinimumSize(new Dimension(800, 488));
        frame.pack();
        frame.setLocationRelativeTo(null);
        // notify gameDriver when window is closed:
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameDriver.applicationClosing();
            }
        });
        frame.setVisible(true);
        // start the task running thread:
        Thread thread = new Thread(taskRunner);
        thread.start();
    }

    /* Prepares all elements of the GUI */
    private void initializePanels() {
        // set up the two sides/areas (gameArea and playerArea):
        gameArea = new JPanel(new CardLayout());
        playerArea = new JTabbedPane();
        playerArea.setBorder(new EmptyBorder(0, 6, 12, 6));
        playerArea.setFocusable(false);
        super.add(gameArea, BorderLayout.CENTER); // center adjusts both height and width
        super.add(playerArea, BorderLayout.EAST); // east only adjusts height

        // initialize gameArea:
        gameplayView = new GameplayView(this);
        gameArea.add(gameplayView, GameAreaOptions.GAMEPLAY.name());
        merchantView = new MerchantView(this);
        Box merchantViewHolder = new Box(BoxLayout.Y_AXIS); // holder centers merchant view
        merchantViewHolder.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        merchantViewHolder.add(merchantView);
        gameArea.add(merchantViewHolder, GameAreaOptions.MERCHANT.name());
        lootView = new LootView(this);
        Box lootViewHolder = new Box(BoxLayout.Y_AXIS); // holder centers loot view
        lootViewHolder.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        lootViewHolder.add(lootView);
        gameArea.add(lootViewHolder, GameAreaOptions.LOOT.name());

        // initialize playerArea:
        int scaleMode = Image.SCALE_SMOOTH; // set the scale mode for icon scaling
        inventoryView = new InventoryView(this);
        ImageIcon inventoryIcon = new ImageIcon(new ImageIcon("icons/pinventory.png").getImage().getScaledInstance(20, 20, scaleMode));
        playerArea.addTab("Inventory", inventoryIcon, inventoryView);
        mapView = new MapView();
        ImageIcon mapIcon = new ImageIcon(new ImageIcon("icons/pmap.png").getImage().getScaledInstance(20, 20, scaleMode));
        playerArea.addTab("Map", mapIcon, mapView);
        statsView = new StatsView();
        ImageIcon statsIcon = new ImageIcon(new ImageIcon("icons/pstats.png").getImage().getScaledInstance(20, 20, scaleMode));
        playerArea.addTab("Stats", statsIcon, statsView);

        // hide areas until the pregame menu is complete:
        gameArea.setVisible(false);
        playerArea.setVisible(false);
    }

    /* Replaces the pre-game menu with the game */
    private void hidePregameMenu() {
        pregameMenu.setVisible(false);
        super.remove(pregameMenu);
        gameArea.setVisible(true);
        playerArea.setVisible(true);
    }

    /* Switches the game area to a selected game area option */
    private void setGameArea(GameAreaOptions card) {
        ((CardLayout) gameArea.getLayout()).show(gameArea, card.name());
    }

    /* ----- Methods to be called by GameDriver below ----- */
    // AKA "Tasks"
    public void updatePlayerInfo(Player player) {
        tasks.addLast((Runnable) () -> {
            inventoryView.updateInventory(player);
            statsView.updateStats(player);
            mapView.updateMap(player.getTravelMap(), player.getPosition());
        });
    }

    public void updatePlayerInfoDirectly(Player player) { // direct
        // does not wait for other tasks to complete (addFirst)
        if (!tasks.isEmpty()) {
            // only necessary when there are other tasks
            tasks.addFirst((Runnable) () -> {
                inventoryView.updateInventory(player);
                statsView.updateStats(player);
                mapView.updateMap(player.getTravelMap(), player.getPosition());
            });
        }
        // the user may modify their inventory after a call to updatePlayerInfo is enqueued
        // to avoid this edge case the newest player will be used again at the end of the queue
        tasks.addLast((Runnable) () -> {
            inventoryView.updateInventory(player);
            statsView.updateStats(player);
            mapView.updateMap(player.getTravelMap(), player.getPosition());
        });
    }

    public void disableUserInteraction() { // direct
        // does not wait for other tasks to complete (addFirst)
        tasks.addFirst((Runnable) () -> {
            gameplayView.disableUserInteraction();
        });
        tasks.addFirst((Runnable) () -> {
            inventoryView.setEnabled(false);
            statsView.setEnabled(false);
            mapView.setEnabled(false);
        });
    }

    /* Printing Methods Below */
    public void displayTextLine(String text) {
        tasks.addLast((Runnable) () -> {
            gameplayView.addText(text + "\n");
        });
    }

    public void displayTextLine() {
        tasks.addLast((Runnable) () -> {
            gameplayView.addText("\n");
        });
    }

    public void displayText(String text) {
        tasks.addLast((Runnable) () -> {
            gameplayView.addText(text);
        });
    } // End Printing Methods

    public void enableGameplayButtons(List<GameplayButtons> buttons) {
        tasks.addLast((Runnable) () -> {
            gameplayView.enableButtons(buttons);
        });
    } // only the selected buttons will be enabled

    public void enableGameplayButton(GameplayButtons button) { // for a single button
        tasks.addLast((Runnable) () -> {
            List<GameplayButtons> wrapperList = new ArrayList<>(1);
            wrapperList.add(button);
            gameplayView.enableButtons(wrapperList);
        });
    } // only the selected button will be enabled

    /* Adds a task to switch to the merchant game area option */
    public void setMerchant(Merchant merchant, int coins, boolean invFull) {
        tasks.addLast((Runnable) () -> {
            merchantView.prepPanel(merchant, coins, invFull);
            setGameArea(GameAreaOptions.MERCHANT); // to be undone when finished
        });
    }

    /* Adds a task to switch to the loot game area option */
    public void setLoot(Item loot, int numCoins, boolean invFull) {
        tasks.addLast((Runnable) () -> {
            lootView.prepPanel(loot, numCoins, invFull);
            setGameArea(GameAreaOptions.LOOT); // to be undone when finished
        });
    }

    /* Adds a waiting time to the task queue */
    public void addDelay(int ms) {
        tasks.addLast((Runnable) () -> {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ex) {
                // IGNORE
            }
        });
    }

    /* ----- End methods to be called by GameDriver ----- 
    *
    *
    *  ----- Methods to be called by view components below ----- */
    public void gameplayButtonPressed(GameplayButtons button) { // used by gameplay panel
        // convert button press into action method call:
        switch (button) {
            case N:
                gameDriver.look(Direction.charToDirection('n'));
                break;
            case S:
                gameDriver.look(Direction.charToDirection('s'));
                break;
            case E:
                gameDriver.look(Direction.charToDirection('e'));
                break;
            case W:
                gameDriver.look(Direction.charToDirection('w'));
                break;
            case YES:
                gameDriver.goDirection();
                break;
            case NO:
                gameDriver.pickNewDirection();
                break;
            case ADVENTURE:
                gameDriver.adventure();
                break;
            case ATTACK:
                gameDriver.attack();
                break;
            case RUN:
                gameDriver.runAway();
                break;
            case QUIT:
                gameDriver.quitGame();
                break;
        }
    }

    public void purchaseItem(int index) { // used by merchant panel
        // index is already validated
        gameDriver.purchaseFromMerchant(index);
        setGameArea(GameAreaOptions.GAMEPLAY);
    }

    public void collectLoot() { // used by loot panel
        gameDriver.collectEnemyLoot();
        setGameArea(GameAreaOptions.GAMEPLAY);
    }

    public void leavePressed(GameAreaOptions source) { // used by merchant and loot panels
        // the gameplay area is already prepared
        gameDriver.resetEventContext(); // end merchant/loot period
        setGameArea(GameAreaOptions.GAMEPLAY);
    }

    public void equipPressed(int index) { // used by inventory panel
        // index is already validated
        gameDriver.inventoryEquipOrConsume(index);
    }

    public void consumePressed(int index) { // used by inventory panel
        // index is already validated
        gameDriver.inventoryEquipOrConsume(index);
        lootView.invNotFull(); // make sure the user can pick up a waiting item
    }

    public void dropPressed(int index) { // used by inventory panel
        // index is already validated
        gameDriver.inventoryDrop(index);
        lootView.invNotFull(); // make sure the user can pick up a waiting item
    }

    public void usernameSubmitted(String username) { // used by pregame menu
        // username is already validated
        gameDriver.checkForGameSave(username);
        hidePregameMenu(); // switch to playing the game
    }
    /* ----- End methods to be called by view components ----- */
}
