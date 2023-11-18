package model.map;

/*
The game map class stores the entire randomly generated game map.
The game map is composed of Scenes.
 */
// @author Jared Scholz
import model.data.DBManager;
import model.entity.Item;
import java.awt.Point;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

public class GameMap implements Serializable {

    private static final long serialVersionUID = 10012L;

    private final int size;
    private Scene[][] map;

    public GameMap(int size, DBManager data) {
        this.size = size;
        initializeMap(data);
    }

    /* Randomly generate the map with (new) scenes */
    private void initializeMap(DBManager data) {
        Random randGen = new Random();
        map = new Scene[size][size];

        List<Scene> allScenes = data.getAllScenes();
        List<Enemy> allEnemies = data.getAllEnemies();
        List<Item> allItems = data.getAllItems();
        List<Merchant> allMerchants = data.getAllMerchants();
        List<PassiveEvent> allPassiveEvents = data.getAllPassiveEvents();
        List<Trap> allTraps = data.getAllTraps();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                // assign the map tile a randomly selected scene:
                map[i][j] = new Scene(allScenes.get(randGen.nextInt(allScenes.size()))); // clone scene for this tile
                // assign the scene a randomly selected battle:
                map[i][j].initializeBattle(allEnemies, allItems, randGen); // an enemy is selected based on type, random loot is assigned
                // give the scene a possible merchant (in addition to any scene-specific merchants):
                map[i][j].addMerchant(new Merchant(allMerchants.get(randGen.nextInt(allMerchants.size())))); // clone merchant for this tile
                map[i][j].initializeMerchants(allItems, randGen); // assign each merchant a random inventory
                // give the scene two possible passive events (in addition to any scene-specific passive events):
                map[i][j].addPassiveEvent(allPassiveEvents.get(randGen.nextInt(allPassiveEvents.size())));
                map[i][j].addPassiveEvent(allPassiveEvents.get(randGen.nextInt(allPassiveEvents.size()))); // no problem if identical (adding to a set)
                // give the scene two possible traps (in addition to any scene-specific traps):
                map[i][j].addTrap(allTraps.get(randGen.nextInt(allTraps.size())));
                map[i][j].addTrap(allTraps.get(randGen.nextInt(allTraps.size()))); // no problem if identical (adding to a set)
            }
        }
    }

    public Scene getScene(Point position) {
        return map[position.x][position.y];
    }
}
