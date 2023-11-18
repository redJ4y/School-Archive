package model.map;

/*
The Scene is the building block of the game map. Each scene has a possible Enemy (battle) and a variety
of possible Traps/Merchants/PassiveEvents that may be selected at random. These may even be scene-specific.
Each scene has a view (what is seen from a distance) and a description (what is seen on approach).
 */
// @author Jared Scholz
import model.entity.Item;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Scene implements Serializable {

    private static final long serialVersionUID = 10015L;

    private String view;
    private String description;
    private EnemyType enemyType;

    private Enemy battle;
    private boolean battleCompleted;
    private Set<Trap> possibleTraps;
    private Set<Merchant> possibleMerchants;
    private Set<PassiveEvent> possiblePassiveEvents;

    public Scene(String view, String description, EnemyType enemyType) {
        this.view = view;
        this.description = description;
        this.enemyType = enemyType;

        battleCompleted = true; // default until battle set
        possibleTraps = new LinkedHashSet<>();
        possibleMerchants = new LinkedHashSet<>();
        possiblePassiveEvents = new LinkedHashSet<>();
    }

    /* Constructor for cloning a base scene */
    public Scene(Scene baseScene) {
        this(baseScene.getView(), baseScene.getDescription(), baseScene.getEnemyType());
        // there may be a maximum of 1 existing trap/merchant/passiveevent in a baseScene:
        if (!baseScene.getPossibleTraps().isEmpty()) {
            addTrap(baseScene.getPossibleTraps().iterator().next());
        }
        if (!baseScene.getPossibleMerchants().isEmpty()) {
            addMerchant(baseScene.getPossibleMerchants().iterator().next());
        }
        if (!baseScene.getPossiblePassiveEvents().isEmpty()) {
            addPassiveEvent(baseScene.getPossiblePassiveEvents().iterator().next());
        }
    }

    /* Select an Event at random out of the scene's possibilities */
    public Event pickEvent() {
        Random randGen = new Random();
        List<Event> eventPool = new ArrayList<>();
        if (!battleCompleted) {
            eventPool.add(battle);
        } // exclude battle if already completed
        eventPool.addAll(possibleMerchants);
        eventPool.addAll(possibleTraps);
        eventPool.addAll(possiblePassiveEvents);
        int selection = randGen.nextInt(eventPool.size());
        // re-roll selection twice to favor battle event:
        if (selection > 0) {
            selection = randGen.nextInt(eventPool.size());
        }
        if (selection > 0) {
            selection = randGen.nextInt(eventPool.size());
        }
        // selection is given 3 chances to be a battle
        // if the battle is already completed, this favors merchant events
        return eventPool.get(selection); // return random selection
    }

    public void setBattleCompleted() {
        battleCompleted = true;
    }

    /* Give the scene a random enemy of the correct EnemyType */
    public void initializeBattle(List<Enemy> allEnemies, List<Item> allItems, Random randGen) {
        List<Enemy> filteredEnemies = new ArrayList<>(allEnemies);
        filteredEnemies.removeIf(n -> (n.getEnemyType() != enemyType)); // filteredEnemies now only contains enemies of type enemyType
        if (!filteredEnemies.isEmpty()) {
            battle = new Enemy(filteredEnemies.get(randGen.nextInt(filteredEnemies.size())));
            battle.initializeLoot(allItems, randGen); // give the enemy random loot
            battleCompleted = false;
        } else { // this should never occur when finished
            // this is only for Jared when adding to the game files
            System.out.println("DEBUGGING: No " + enemyType.name() + "s in file!");
        }
    }

    /* Make sure that all of the scene's merchants are initialized (have inventories) */
    public void initializeMerchants(List<Item> allItems, Random randGen) {
        for (Merchant current : possibleMerchants) {
            current.initializeInventory(allItems, randGen); // assign each merchant a random inventory
        }
    }

    public void addTrap(Trap trap) {
        possibleTraps.add(trap);
    }

    public void addMerchant(Merchant merchant) {
        possibleMerchants.add(merchant);
    }

    public void addPassiveEvent(PassiveEvent passiveEvent) {
        possiblePassiveEvents.add(passiveEvent);
    }

    public String getView() {
        return view;
    }

    public String getDescription() {
        return description;
    }

    public EnemyType getEnemyType() {
        return enemyType;
    }

    public Set<Trap> getPossibleTraps() {
        return possibleTraps;
    }

    public Set<Merchant> getPossibleMerchants() {
        return possibleMerchants;
    }

    public Set<PassiveEvent> getPossiblePassiveEvents() {
        return possiblePassiveEvents;
    }

    @Override
    public String toString() {
        return "[" + view + ", " + description + ", " + enemyType.name() + "]";
    }
}
