package edu.virginia.game;

public class MatrixValues {
	public final static int PLAYER = 0;
	public final static int WALKABLE_SPACE_START = 1;
	public final static int PATH = 2;
	public final static int WALKABLE_SPACE_END = 99;
	public final static int WALL_START = 100;
	public final static int WALL_ROOMS = 101;
	public final static int WALL_END = 199;
	public final static int ENEMY_START = 200;
	public final static int ENEMY_BOSS = 298;
	public final static int ENEMY_END = 299;
	public final static int ITEMS_START = 300;
	public final static int ITEMS_END = 399;
	public final static int DOOR_START = 400;
	public final static int DOOR_END = 499;
	public final static int STAIRS_DOWN = 999;
	public final static int STAIRS_UP = 1000;
	public final static int ALTAR = 1001;
	public final static int FOG_OF_WAR = 2000;
	
	public static boolean isWalkable(int val) {
		return (val >= WALKABLE_SPACE_START && val <= WALKABLE_SPACE_END) 
				|| (val >= ITEMS_START && val <= ITEMS_END)
				|| (val == ALTAR)
				|| (val >= DOOR_START && val <= DOOR_END)
				|| (val == STAIRS_UP || val == STAIRS_DOWN)
				|| (val == PLAYER)
				|| (val >= ENEMY_START && val <= ENEMY_END);
	}
	
	public static boolean canSeePast(int val) {
		return (val >= WALKABLE_SPACE_START && val <= WALKABLE_SPACE_END) 
				|| (val >= ITEMS_START && val <= ITEMS_END)
				|| (val == STAIRS_UP || val == STAIRS_DOWN)
				|| (val == PLAYER)
				|| (val >= ENEMY_START && val <= ENEMY_END)
				|| val == FOG_OF_WAR;
	}
}
