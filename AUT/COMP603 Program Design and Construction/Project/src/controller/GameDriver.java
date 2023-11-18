package controller;

/*
GameDriver handles gameplay, taking input from the view and updating the model.
- The game driver keeps track of the current game state (what is happening) and updates the view accordingly.
- The game driver updates the model immediately and enqueues updates for the view to process in time.
 */
// @author Jared Scholz
import model.data.DBManager;
import model.map.GameMap;
import model.entity.Player;
import view.ViewManager;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Point;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import model.entity.Inventory;
import model.entity.StatType;
import model.map.Enemy;
import model.map.Event;
import model.map.Merchant;
import model.map.PassiveEvent;
import model.map.Scene;
import model.map.Trap;
import view.GameplayButtons;

public class GameDriver {

    public final static int MAP_SIZE = 17; // should not be changed

    private DBManager dataKeeper;
    private static ViewManager viewManager;
    private GameMap gameMap;
    private Player player;

    /* Context variables - remember values between actions */
    // these will always be set before needed (validated just in case)
    private String lookDirectionString;
    private Point lookToPosition;
    private Merchant currentMerchant;
    private Enemy currentEnemy;
    private int enemyInitialHealth; // not validated

    public GameDriver() {
        dataKeeper = new DBManager();
        viewManager = new ViewManager(this);
        gameMap = null; // set by checkForGameSave()
        player = null; // set by checkForGameSave()

        // initialize context variables:
        lookDirectionString = null;
        lookToPosition = null;
        currentMerchant = null;
        currentEnemy = null;
        enemyInitialHealth = 1;
    }

    /* Displays the GUI */
    public void runGame() {
        viewManager.display(); // prompts the pregame menu first
        // prepare gameplay view:
        viewManager.displayTextLine("You awake in a strange place.");
        viewManager.displayTextLine("You feel ready for an adventure...");
        viewManager.enableGameplayButton(GameplayButtons.ADVENTURE);
    }

    /* Looks for a game save and initializes game data variables */
    public void checkForGameSave(String username) {
        if (dataKeeper.checkGameSave(username)) {
            // a game save with this username exists...
            gameMap = dataKeeper.getExistingMap();
            player = dataKeeper.getExistingPlayer();
        } else {
            // a game save with this username does not exist...
            gameMap = new GameMap(MAP_SIZE, dataKeeper); // generate random map
            player = new Player(username, MAP_SIZE / 2, MAP_SIZE / 2); // generate new player
        }
        viewManager.updatePlayerInfo(player);
    }

    public void resetEventContext() { // used when user does not interact with merchant/loot
        // ensures current game state validation works (not strictly necessary)
        currentMerchant = null;
        currentEnemy = null;
    }

    /* The user has selected an item to purchase from a merchant */
    public void purchaseFromMerchant(int index) {
        if (currentMerchant != null) { // validate current game state
            if (player.getInventory().hasSpace()) { // this was already validated (good measure)
                player.getInventory().addItem(currentMerchant.getItem(index));
                player.removeCoins(currentMerchant.getPrice(index));
                viewManager.updatePlayerInfoDirectly(player); // update immediately
                viewManager.displayTextLine("You take your new " + currentMerchant.getItem(index).getName() + "!");
                currentMerchant.removeItem(index);
                currentMerchant = null; // reset after using
            }
        }
    }

    /* The user has defeated an enemy and chosen to collect the loot */
    public void collectEnemyLoot() {
        if (currentEnemy != null) { // validate current game state
            if (player.getInventory().hasSpace()) { // this was already validated (good measure)
                player.getInventory().addItem(currentEnemy.getLoot());
                viewManager.updatePlayerInfoDirectly(player); // update immediately
                viewManager.displayTextLine("You take your new " + currentEnemy.getLoot().getName() + "!");
                currentEnemy = null; // reset after using
            }
        }
    }

    /* The user has selected to use an inventory item */
    public void inventoryEquipOrConsume(int index) {
        Inventory inventory = player.getInventory();
        // index was already validated - do it again for good measure:
        if (!inventory.isEmpty() && index >= 0 && index < inventory.getItems().size()) {
            inventory.equipOrConsume(index, player.getStats());
        }
        viewManager.updatePlayerInfoDirectly(player); // update immediately
    }

    /* The user has selected to drop an inventory item */
    public void inventoryDrop(int index) {
        Inventory inventory = player.getInventory();
        // index was already validated - do it again for good measure:
        if (!inventory.isEmpty() && index >= 0 && index < inventory.getItems().size()) {
            inventory.remove(index);
        }
        viewManager.updatePlayerInfoDirectly(player); // update immediately
    }

    /* ----- Gameplay methods below ----- */
    public void adventure() {
        viewManager.displayTextLine("Pick a direction to look...");
        viewManager.enableGameplayButtons(Arrays.asList(GameplayButtons.N, GameplayButtons.E, GameplayButtons.S, GameplayButtons.W));
    }

    /* The user has selected a direction to look */
    public void look(Direction lookDirection) {
        lookDirectionString = lookDirection.toString();
        lookToPosition = lookDirection.getChange(player.getPosition());
        viewManager.displayTextLine("You pull out your spyglass and look into the distant " + lookDirectionString + "...");
        viewManager.addDelay(250);
        if (isValidPosition(lookToPosition)) {
            viewManager.displayTextLine(gameMap.getScene(lookToPosition).getView());
            viewManager.addDelay(500);
            viewManager.displayTextLine("Venture " + lookDirectionString + "?");
            viewManager.enableGameplayButtons(Arrays.asList(GameplayButtons.YES, GameplayButtons.NO));
        } else {
            viewManager.displayTextLine("You see a mighty cliff cascading downwards into the violent sea below.");
            viewManager.displayTextLine("You cannot venture there.");
            adventure(); // try again
        }
    }

    /* The user has chosen to go to lookToPosition */
    public void goDirection() {
        if (lookToPosition != null) { // validate current game state
            player.move(lookToPosition);
            lookToPosition = null; // reset after using
            player.getTravelMap().setVisited(player.getPosition()); // record movement when player enters new tile
            viewManager.updatePlayerInfo(player);
            encounterNewScene();
        }
    }

    /* The user intends to select a different direction to look */
    public void pickNewDirection() {
        viewManager.displayTextLine("Maybe a different direction would be better.");
        adventure();
    }

    /* The user has chosen to attack the current enemy */
    public void attack() {
        doPlayerTurn();
        doEnemyTurn();
        if (currentEnemy.isDead()) { // enemy has been defeated...
            viewManager.enableGameplayButton(GameplayButtons.ADVENTURE);
        } else { // enemy is not yet defeated...
            viewManager.enableGameplayButtons(Arrays.asList(GameplayButtons.ATTACK, GameplayButtons.RUN));
        }
    }

    /* The user has chosen to run away from the current enemy */
    public void runAway() {
        if (currentEnemy != null) { // validate current game state
            viewManager.displayTextLine("You turn and run.");
            printSlowTransition();
            // give enemy one last chance to attack (33% possibility)...
            if ((new Random()).nextInt(3) == 0) {
                int damage = currentEnemy.getAttack(player.getStats(), player.getInventory().getEquippedArmor());
                if (damage > 0) { // agility increases chance of escaping without getting hit
                    viewManager.displayTextLine("The " + currentEnemy.getName() + " gets off one last hit as you begin to run!");
                    currentEnemy = null; // reset after using
                    printSlowTransition();
                    player.getStats().modifyStat(StatType.HP, -1 * damage);
                    viewManager.updatePlayerInfo(player);
                    viewManager.displayTextLine("Damage taken: " + damage + " | You have " + player.getStats().getHealth() + " [Health] left.");
                    checkForDeath(); // enemy could kill player
                }
            }
            viewManager.addDelay(500);
            viewManager.displayTextLine("Phew... You made it out alive.");
            viewManager.enableGameplayButton(GameplayButtons.ADVENTURE);
        }
    }

    /* Determines the scene, prints generic text, then calls an appropriate encounter scene method */
    private void encounterNewScene() {
        if (lookDirectionString != null) { // validate current game state
            viewManager.displayTextLine("You set off " + lookDirectionString + "ward.");
            lookDirectionString = null; // reset after using
            printSlowTransition();
            Scene scene = gameMap.getScene(player.getPosition());
            viewManager.displayTextLine(scene.getDescription());
            printSlowTransition();
            Event event = scene.pickEvent(); // randomly selected event
            switch (event.getType()) {
                case BATTLE:
                    viewManager.displayTextLine("Something is here.");
                    printSlowTransition();
                    viewManager.displayTextLine("Oh no... It's a: " + event.getName());
                    viewManager.addDelay(250);
                    viewManager.displayTextLine(event.getDescription());
                    printSlowTransition();
                    encounterBattle((Enemy) event); // enables correct UI buttons
                    break;
                case MERCHANT:
                    viewManager.displayTextLine("Something is here.");
                    printSlowTransition();
                    viewManager.displayTextLine("Phew... It's a: " + event.getName());
                    viewManager.addDelay(250);
                    viewManager.displayTextLine(event.getDescription());
                    printSlowTransition();
                    encounterMerchant((Merchant) event);
                    viewManager.enableGameplayButton(GameplayButtons.ADVENTURE);
                    break;
                case PASSIVE_EVENT:
                    viewManager.displayTextLine("There's nothing here. A future visit might uncover something more...");
                    viewManager.addDelay(250);
                    viewManager.displayTextLine(event.getDescription());
                    viewManager.addDelay(250);
                    encounterPassiveEvent((PassiveEvent) event);
                    viewManager.enableGameplayButton(GameplayButtons.ADVENTURE);
                    break;
                case TRAP:
                    viewManager.displayTextLine("An eerie silence engulfs you.");
                    printSlowTransition();
                    viewManager.displayTextLine(event.getDescription());
                    viewManager.addDelay(250);
                    encounterTrap((Trap) event);
                    viewManager.enableGameplayButton(GameplayButtons.ADVENTURE);
                    break;
            }
        }
    }

    /* Alternate between the players turn and the enemies turn until one dies (or runs) */
    private void encounterBattle(Enemy enemy) {
        currentEnemy = enemy;
        enemyInitialHealth = enemy.getStats().getHealth();
        viewManager.enableGameplayButtons(Arrays.asList(GameplayButtons.ATTACK, GameplayButtons.RUN));
    }

    /* The player attacks the current enemy */
    private void doPlayerTurn() {
        if (currentEnemy != null) { // validate current game state
            int damage = player.getAttack(currentEnemy.getStats());
            if (!player.hasWeapon()) {
                viewManager.displayTextLine("You punch hard with your bare fist...");
                viewManager.displayTextLine("Perhaps you should equip a weapon.");
            } else {
                if (damage < 0) { // enemy dodged (damage == -1)
                    viewManager.displayTextLine("You attempt to strike, but the " + currentEnemy.getName() + " dodges.");
                    damage = 0;
                } else {
                    viewManager.displayTextLine("You strike the " + currentEnemy.getName() + " with your " + player.getInventory().getEquippedWeapon().getName() + "!");
                    printSlowTransition();
                    // print a special message if the weapon does more or less than expected:
                    if (damage > (int) Math.ceil(player.getDamageMax() * 0.75)) { // within 25% of max damage or above
                        viewManager.displayTextLine("Your hit pierces straight through the enemy's protection!");
                    } else if (damage < player.getDamageMin()) {
                        viewManager.displayTextLine("Your hit nearly bounces off of the enemy! It must be well protected...");
                    } else {
                        viewManager.displayTextLine("Your hit connects well.");
                    }
                }
            }
            viewManager.addDelay(500);
            currentEnemy.getStats().modifyStat(StatType.HP, -1 * damage);
            viewManager.displayTextLine("Damage dealt: " + damage + " | Enemy health: " + (int) (100 * ((double) currentEnemy.getStats().getHealth() / enemyInitialHealth)) + "%");
        }
    }

    /* The current enemy attacks the player */
    private void doEnemyTurn() {
        if (currentEnemy != null) { // validate current game state
            if (!currentEnemy.isDead()) {
                printSlowTransition();
                viewManager.displayTextLine("The " + currentEnemy.getName() + " responds with a vicious attack of its own!");
                printSlowTransition();

                int damage = currentEnemy.getAttack(player.getStats(), player.getInventory().getEquippedArmor());
                if (damage < 0) {
                    viewManager.displayTextLine("You dodge the enemy's strike!");
                    damage = 0;
                } else {
                    // print a special message if the enemy does more or less damage than expected:
                    if (damage > (int) Math.ceil(currentEnemy.getDamageMax() * 0.75)) { // within 25% of max damage or above
                        viewManager.displayTextLine("It pierces straight through your armor!");
                    } else if (damage < currentEnemy.getDamageMin()) {
                        viewManager.displayTextLine("It struggles to pierce your armor!");
                    } else {
                        viewManager.displayTextLine("It hurts... ");
                    }
                }
                viewManager.addDelay(500);
                player.getStats().modifyStat(StatType.HP, -1 * damage);
                viewManager.updatePlayerInfo(player);
                viewManager.displayTextLine("Damage taken: " + damage + " | You have " + player.getStats().getHealth() + " [Health] left.");
                checkForDeath(); // enemy could kill player
            } else { // enemy is dead - collect loot!
                gameMap.getScene(player.getPosition()).setBattleCompleted();
                player.getTravelMap().setDefeated(player.getPosition());

                printSlowTransition();
                viewManager.displayTextLine("The " + currentEnemy.getName() + " falls to the ground...");
                printSlowTransition();
                int coinsFound = player.addCoins(currentEnemy.getRarity());
                viewManager.updatePlayerInfo(player); // show updated coins
                viewManager.displayTextLine("You search the corpse and find " + coinsFound + " coins!");
                viewManager.addDelay(500);
                viewManager.displayTextLine("And, what's this?");
                printSlowTransition();
                viewManager.addDelay(1000);
                viewManager.setLoot(currentEnemy.getLoot(), coinsFound, !player.getInventory().hasSpace());
                viewManager.displayTextLine("...You have conqered another beast!"); // visible after loot panel
            }
        }
    }

    /* Method to be called by Item when broken (let player know of broken item) */
    public static void notifyOfBrokenItem(String itemName) {
        viewManager.displayTextLine("...Your " + itemName + " is nearly broken!");
    }

    /* Give the player a chance to purchase a single item from a merchant */
    private void encounterMerchant(Merchant merchant) {
        currentMerchant = merchant;
        viewManager.displayTextLine("You check your pockets and find that you have " + player.getCoins() + " coins.");
        viewManager.addDelay(250);
        if (!currentMerchant.hasItems()) {
            viewManager.displayTextLine("...But the merchant has already sold all of their items...");
            viewManager.displayTextLine("Too bad.");
            currentMerchant = null; // reset after using
        } else {
            viewManager.displayTextLine("The merchant begins to unpack.");
            printSlowTransition();
            viewManager.addDelay(1000);
            viewManager.setMerchant(currentMerchant, player.getCoins(), !player.getInventory().hasSpace());
            viewManager.displayTextLine("The merchant hurriedly packs up and sets off..."); // visible after merchant panel
        }
    }

    private void encounterPassiveEvent(PassiveEvent passiveEvent) {
        viewManager.displayTextLine("You gain " + passiveEvent.getHpBonus() + " [Health].");
        player.getStats().modifyStat(StatType.HP, passiveEvent.getHpBonus());
        viewManager.updatePlayerInfo(player);
    }

    private void encounterTrap(Trap trap) {
        String gainOrLoss = trap.getModification() < 0 ? "lose " : "gain ";
        viewManager.displayTextLine("You " + gainOrLoss + Math.abs(trap.getModification()) + " [" + trap.getStat() + "].");
        player.getStats().modifyStat(trap.getStat(), trap.getModification());
        viewManager.updatePlayerInfo(player);
        checkForDeath(); // health could go down
    }

    /* Ensures that a Point is within the game map */
    private boolean isValidPosition(Point moveTo) {
        return (moveTo.x >= 0 && moveTo.x < MAP_SIZE && moveTo.y >= 0 && moveTo.y < MAP_SIZE);
    }

    private void printSlowTransition() {
        viewManager.addDelay(250);
        viewManager.displayText(". ");
        viewManager.addDelay(500);
        viewManager.displayText(". ");
        viewManager.addDelay(500);
        viewManager.displayText(".");
        viewManager.displayTextLine();
        viewManager.addDelay(500);
    }

    /* Checks for player death - delete game save on death */
    private void checkForDeath() {
        if (player.getStats().getHealth() <= 0) {
            printSlowTransition();
            viewManager.displayTextLine("You fall slowly to the ground.");
            printSlowTransition();
            viewManager.displayTextLine("Game over!");
            printSlowTransition();
            viewManager.displayTextLine("Progress lost!");
            viewManager.disableUserInteraction(); // BLOCK FURTHER INTERACTION
            dataKeeper.deleteGameSave();
            // block the player from saving on close:
            gameMap = null; // to be re-initialized
            player = null; // to be re-initialized
            printSlowTransition();
            viewManager.displayTextLine("> Returning to pre-game menu...");

            // wait until all text is displayed...
            GameDriver thisReference = this; // for use within the timer task
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // restart game (new pregame menu):
                    viewManager.stopTaskRunner(); // best practice
                    ((JFrame) SwingUtilities.getWindowAncestor(viewManager)).dispose(); // close old window
                    dataKeeper = new DBManager();
                    viewManager = new ViewManager(thisReference);
                    runGame();
                }
            }, viewManager.getNumTasks() * 300 + 250); // delay time (approximate)
        }
    }

    public void quitGame() {
        if (gameMap != null && player != null) {
            dataKeeper.saveGame(gameMap, player); // save game on quit
            // block a second save from occuring:
            gameMap = null;
            player = null;
        }
        System.exit(0);
    } // ----- End gameplay methods -----

    public void applicationClosing() {
        if (gameMap != null && player != null) {
            dataKeeper.saveGame(gameMap, player); // save game on close
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.out.println("ERROR SETTING LOOK AND FEEL");
            System.out.println("PLEASE ENSURE THAT THE FLATLAF LIBRARY EXISTS");
            System.out.println("THE UI IS DESIGNED ONLY FOR FLATLAF DARK");
        }
        GameDriver driver = new GameDriver();
        driver.runGame();
    }
}
