package model.map;

/*
The EnemyType enum is used to distinguish between Enemies.
This is used when generating the map - a Scene wants a specific type of enemy.
 */
// @author Jared Scholz
import java.io.Serializable;

public enum EnemyType implements Serializable {
    WILD_ANIMAL, GOBLIN, UNDEAD, TROLL, DRAGON, GHOST;

    private static final long serialVersionUID = 10011L;
}
