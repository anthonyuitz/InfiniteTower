package edu.virginia.game;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class LevelGenerator {
	int[][] level;
	Point playerCoord;
	Point stairsUp;
	NonTileMatrix entitiesMatrix;
	ArrayList<Room> rooms;
	
	public LevelGenerator() {
		level = new int[0][0];
		playerCoord = new Point(-1, -1);
		stairsUp = new Point(-1,-1);
	}
	
	public int[][] generateLevel(int floor) {
		int size = 30+floor*2;
		int rmmean = 7;
		int rmstdev = 2;
		
		if(floor % 5 == 0) {
			entitiesMatrix = new NonTileMatrix(35, 35);
			level = new int[35][35];//the matrix to return
			this.rooms = new ArrayList<Room>(); //all generated rooms
			for(int x = 0; x < level.length; x++) { //fill the matrix with unwalkable spaces
				for(int y = 0; y < level[0].length; y++) {
					level[x][y] = MatrixValues.WALL_START;
				}
			}
			
			rooms.add(new Room(1, 1, 4, 4));
			rooms.add(new Room(10, 6, 8, 8)); //reincarnation room
			rooms.add(new Room(8, 20, 4, 4)); //potion room
			rooms.add(new Room(22, 22, 10, 10));								 //boss room
			
			for(int x = 0; x < rooms.size(); x++) {
				int startR = rooms.get(x).x;
				int endR = startR + rooms.get(x).width;
				int startC = rooms.get(x).y;
				int endC = startC + rooms.get(x).height;
				for(int r = startR; r < endR; r++) {
					for(int col = startC; col < endC; col++) {
						level[r][col] = MatrixValues.WALKABLE_SPACE_START;
					}
				}
			}

			for(int x = 0; x < rooms.size(); x++)
				level[rooms.get(x).centerX][rooms.get(x).centerY] = -1;

			for(int x = 0; x < rooms.size(); x++) {
				Room room = rooms.get(x);
				for(int r = -1; r <= room.width; r++) {
					Point top = new Point(room.x+r,room.y-1);
					Point bot = new Point(room.x+r,room.y+room.height);

					if(inBounds(top)) {
						if(inBounds(new Point(top.x, top.y+1)) && level[top.x][top.y+1] == -1) {
							level[top.x][top.y] = MatrixValues.DOOR_START;
						}
						else {
							level[top.x][top.y] = MatrixValues.WALL_ROOMS;
						}
					}

					if(inBounds(bot)) {
						if(inBounds(new Point(bot.x, bot.y-1)) && level[bot.x][bot.y-1] == -1) {
							level[bot.x][bot.y] = MatrixValues.DOOR_START;
						}
						else {
							level[bot.x][bot.y] = MatrixValues.WALL_ROOMS;
						}
					}
				}
				for(int r = -1; r <= room.height; r++) {
					Point left = new Point(room.x-1,room.y+r);
					Point right = new Point(room.x+room.width,room.y+r);

					if(inBounds(left)) {
						if(inBounds(new Point(left.x+1, left.y)) && level[left.x+1][left.y] == -1) {
							level[left.x][left.y] = MatrixValues.DOOR_START;
						}
						else {
							level[left.x][left.y] = MatrixValues.WALL_ROOMS;
						}
					}

					if(inBounds(right)) {
						if(inBounds(new Point(right.x-1, right.y)) && level[right.x-1][right.y] == -1) {
							level[right.x][right.y] = MatrixValues.DOOR_START;
						}
						else {
							level[right.x][right.y] = MatrixValues.WALL_ROOMS;
						}
					}
				}
			}

			for(int x = 0; x < rooms.size() - 1; x++) {
				connectRooms(rooms.get(x), rooms.get(x+1));
				for(int y = 0; y < rooms.size(); y++) {
					if(y != x) {
						if(Math.random() < .1) {
							connectRooms(rooms.get(x), rooms.get(y));
						}
					}
				}
			}

			for(int x = 0; x < level.length; x++) {
				for(int y = 0; y < level[0].length; y++) {
					if(level[x][y] == -1) {
						level[x][y] = 1;
					}
					else if(level[x][y] == MatrixValues.DOOR_START) {
						Point up = new Point(x, y-1);
						Point down = new Point(x, y+1);
						Point left = new Point(x-1, y);
						Point right = new Point(x+1, y);
						boolean nearPath = false;
						if(inBounds(up) && level[up.x][up.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						else if(inBounds(down) && level[down.x][down.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						else if(inBounds(left) && level[left.x][left.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						else if(inBounds(right) && level[right.x][right.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						if(!nearPath) {
							level[x][y] = MatrixValues.WALL_ROOMS;
						}
					}
					/*else if(level[x][y] == MatrixValues.PATH) { //comment in if paths are not a different tile
					level[x][y] = 1;
				}*/
				}
			}
			
			double prob = 1.0;
			while(Math.random() < prob) {
				rooms.get(2).randomizeCenter();
				//level[rooms.get(x).centerX][rooms.get(x).centerY] = MatrixValues.ENEMY_START+enemyType;
				entitiesMatrix.addVal(rooms.get(2).getCenter(), MatrixValues.ITEMS_START);
				prob -= .33;
			}
			
			rooms.get(1).randomizeCenter();
			level[rooms.get(1).centerX][rooms.get(1).centerY] = MatrixValues.ALTAR;

			//put the player or the stairs on depending on which floor it is
			rooms.get(0).randomizeCenter();
			rooms.get(rooms.size()-1).randomizeCenter();
			if(floor > 1){
				//level[rooms.get(0).centerX][rooms.get(0).centerY] = MatrixValues.STAIRS_DOWN;
			}
			entitiesMatrix.addVal(rooms.get(0).getCenter(), MatrixValues.PLAYER);
			playerCoord = new Point(rooms.get(0).centerX, rooms.get(0).centerY);
			level[rooms.get(rooms.size()-1).centerX][rooms.get(rooms.size()-1).centerY] = MatrixValues.STAIRS_UP;
			stairsUp = new Point(rooms.get(rooms.size()-1).centerX, rooms.get(rooms.size()-1).centerY);
			
			rooms.get(3).randomizeCenter();
			entitiesMatrix.addVal(rooms.get(3).getCenter(), MatrixValues.ENEMY_BOSS);
			surroundWithWalls();
			entitiesMatrix.surroundWithEmpty();
			return level;
		}
		else {
			entitiesMatrix = new NonTileMatrix(size, size);
			level = new int[size][size]; //the matrix to return
			this.rooms = new ArrayList<Room>(); //all generated rooms
			for(int x = 0; x < level.length; x++) { //fill the matrix with unwalkable spaces
				for(int y = 0; y < level[0].length; y++) {
					level[x][y] = MatrixValues.WALL_START;
				}
			}

			for(int z = 0; z < 100; z++) {
				rooms.add(generateRoom(size, size, rmmean, rmstdev));
			}

			int c = 0;
			while(c != rooms.size()) {
				if(rooms.size() <= 1)
					break;
				int t = (int)(Math.random()*(rooms.size()-c))+c;
				Collections.swap(rooms, t, c);
				c++;
				for(int x = c; x < rooms.size(); x++) {
					if(rooms.get(x).overlaps(rooms.get(c-1))) {
						rooms.remove(x);
						x--;
					}
				}
			}



			for(int x = 0; x < rooms.size(); x++) {
				int startR = rooms.get(x).x;
				int endR = startR + rooms.get(x).width;
				int startC = rooms.get(x).y;
				int endC = startC + rooms.get(x).height;
				for(int r = startR; r < endR; r++) {
					for(int col = startC; col < endC; col++) {
						level[r][col] = MatrixValues.WALKABLE_SPACE_START;
					}
				}
			}

			for(int x = 0; x < rooms.size(); x++)
				level[rooms.get(x).centerX][rooms.get(x).centerY] = -1;

			for(int x = 0; x < rooms.size(); x++) {
				Room room = rooms.get(x);
				for(int r = -1; r <= room.width; r++) {
					Point top = new Point(room.x+r,room.y-1);
					Point bot = new Point(room.x+r,room.y+room.height);

					if(inBounds(top)) {
						if(inBounds(new Point(top.x, top.y+1)) && level[top.x][top.y+1] == -1) {
							level[top.x][top.y] = MatrixValues.DOOR_START;
						}
						else {
							level[top.x][top.y] = MatrixValues.WALL_ROOMS;
						}
					}

					if(inBounds(bot)) {
						if(inBounds(new Point(bot.x, bot.y-1)) && level[bot.x][bot.y-1] == -1) {
							level[bot.x][bot.y] = MatrixValues.DOOR_START;
						}
						else {
							level[bot.x][bot.y] = MatrixValues.WALL_ROOMS;
						}
					}
				}
				for(int r = -1; r <= room.height; r++) {
					Point left = new Point(room.x-1,room.y+r);
					Point right = new Point(room.x+room.width,room.y+r);

					if(inBounds(left)) {
						if(inBounds(new Point(left.x+1, left.y)) && level[left.x+1][left.y] == -1) {
							level[left.x][left.y] = MatrixValues.DOOR_START;
						}
						else {
							level[left.x][left.y] = MatrixValues.WALL_ROOMS;
						}
					}

					if(inBounds(right)) {
						if(inBounds(new Point(right.x-1, right.y)) && level[right.x-1][right.y] == -1) {
							level[right.x][right.y] = MatrixValues.DOOR_START;
						}
						else {
							level[right.x][right.y] = MatrixValues.WALL_ROOMS;
						}
					}
				}
			}

			for(int x = 0; x < rooms.size() - 1; x++) {
				connectRooms(rooms.get(x), rooms.get(x+1));
				for(int y = 0; y < rooms.size(); y++) {
					if(y != x) {
						if(Math.random() < .1) {
							connectRooms(rooms.get(x), rooms.get(y));
						}
					}
				}
			}

			for(int x = 0; x < level.length; x++) {
				for(int y = 0; y < level[0].length; y++) {
					if(level[x][y] == -1) {
						level[x][y] = 1;
					}
					else if(level[x][y] == MatrixValues.DOOR_START) {
						Point up = new Point(x, y-1);
						Point down = new Point(x, y+1);
						Point left = new Point(x-1, y);
						Point right = new Point(x+1, y);
						boolean nearPath = false;
						if(inBounds(up) && level[up.x][up.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						else if(inBounds(down) && level[down.x][down.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						else if(inBounds(left) && level[left.x][left.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						else if(inBounds(right) && level[right.x][right.y] == MatrixValues.PATH) {
							nearPath = true;
						}
						if(!nearPath) {
							level[x][y] = MatrixValues.WALL_ROOMS;
						}
					}
					/*else if(level[x][y] == MatrixValues.PATH) { //comment in if paths are not a different tile
					level[x][y] = 1;
				}*/
				}
			}

			//TODO: populate the rooms with items, etc depending on room type/floor

			for(int x = 1; x < rooms.size()-1; x++) {
				double prob = .75;
				while(Math.random() < prob) {
					rooms.get(x).randomizeCenter();
					int enemyType = (int)(Math.random()*3);
					//level[rooms.get(x).centerX][rooms.get(x).centerY] = MatrixValues.ENEMY_START+enemyType;
					entitiesMatrix.addVal(rooms.get(x).getCenter(), MatrixValues.ENEMY_START+enemyType);
					prob -= .25;
				}
				if(Math.random() < .5) {
					rooms.get(x).randomizeCenter();
					//level[rooms.get(x).centerX][rooms.get(x).centerY] = MatrixValues.ITEMS_START;
					int itemNum = Item.getItem();
					entitiesMatrix.addVal(rooms.get(x).getCenter(), itemNum);
				}
			}

			//put the player or the stairs on depending on which floor it is
			rooms.get(0).randomizeCenter();
			rooms.get(rooms.size()-1).randomizeCenter();
			if(floor > 1){
				//level[rooms.get(0).centerX][rooms.get(0).centerY] = MatrixValues.STAIRS_DOWN;
			}
			entitiesMatrix.addVal(rooms.get(0).getCenter(), MatrixValues.PLAYER);
			playerCoord = new Point(rooms.get(0).centerX, rooms.get(0).centerY);
			level[rooms.get(rooms.size()-1).centerX][rooms.get(rooms.size()-1).centerY] = MatrixValues.STAIRS_UP;
			stairsUp = new Point(rooms.get(rooms.size()-1).centerX, rooms.get(rooms.size()-1).centerY);
			surroundWithWalls();
			entitiesMatrix.surroundWithEmpty();
			return level;
		}
	}
	
	public Point getStairsUp() {
		return stairsUp;
	}
	
	private void surroundWithWalls() {
		int[][] newLevels = new int[level.length+2][level[0].length+2];
		for(int x=0; x<newLevels[0].length;x++) {
			newLevels[0][x] = MatrixValues.WALL_ROOMS;
			newLevels[newLevels.length-1][x] = MatrixValues.WALL_ROOMS;
		}
		for(int x=0; x<newLevels.length;x++) {
			newLevels[x][0] = MatrixValues.WALL_ROOMS;
			newLevels[x][newLevels[0].length-1] = MatrixValues.WALL_ROOMS;
		}
		for(int x = 1; x < newLevels.length-1;x++) {
			for(int y = 1; y < newLevels[0].length-1; y++) {
				newLevels[x][y] = level[x-1][y-1];
			}
		}
		level = newLevels;
		for(Room r : rooms) {
			r.matrixSurroundedWithWalls();
		}
		playerCoord.x++;
		playerCoord.y++;
		stairsUp.x++;
		stairsUp.y++;
	}
	
	public NonTileMatrix getEntitiesMatrix() {
		return entitiesMatrix;
	}
	
	//generates an array list of ints, first number is x, 2nd is y, 3rd is width of room, 4th is height
	//rooms have random x, y with random gaussian width and height with mean mean and standard deviation stdev
	private Room generateRoom(int lvlwid, int lvlhgt, int mean, int stdev) {
		int x = (int)(Math.random()*lvlwid);
		int y = (int)(Math.random()*lvlhgt);
		Random rng = new Random();
		int wid = (int) (mean + stdev*rng.nextGaussian());
		if(wid < 1) {
			wid = 1;
		}
		int hgt = (int) (mean + stdev*rng.nextGaussian());
		if(hgt < 1) {
			hgt = 1;
		}
		Room room = new Room(x, y, wid, hgt);
		
		if(isValidRoom(lvlwid, lvlhgt, room)) {
			return room;
		}
		else {
			return generateRoom(lvlwid, lvlhgt, mean, stdev);
		}
	}
	
	//checks if the room is within the bounds of the level
	private boolean isValidRoom(int lvlwid, int lvlhgt, Room room) {
		if(room.x >= 0 && room.x+room.width <= lvlwid) {
			if(room.y >= 0 && room.y+room.height <= lvlhgt) {
				return true;
			}
		}
		return false;
	}
	
	public void connectRooms(Room r1, Room r2) {
		Queue<Point> queue = new LinkedList<Point>();
		ArrayList<Point> visited = new ArrayList<Point>();
		HashMap<Point, Point> pathBack = new HashMap<Point, Point>();
		
		Point curr = new Point(r1.centerX, r1.centerY);
		Point start = new Point(r1.centerX, r1.centerY);
		Point end = new Point(r2.centerX, r2.centerY);
		queue.add(curr);
		
		while(!queue.isEmpty()) {
			curr = queue.remove();
			if(curr.equals(end)) {
				curr = pathBack.get(curr);
				while(!curr.equals(start)) {
					if(level[curr.x][curr.y] != MatrixValues.DOOR_START) {
						level[curr.x][curr.y] = MatrixValues.PATH;
					}
					curr = pathBack.get(curr);
				}
				break;
			}
			if(visited.contains(curr))
				continue;
			//System.out.println(curr);
			visited.add(curr);
			Point up = new Point(curr.x, curr.y-1);
			Point down = new Point(curr.x, curr.y+1);
			Point left = new Point(curr.x-1, curr.y);
			Point right = new Point(curr.x+1, curr.y);
			if(validPoint(up, visited) || up.equals(end)) {
				queue.add(up);
				pathBack.put(up, curr);
			}
			if(validPoint(down, visited) || down.equals(end)) {
				queue.add(down);
				pathBack.put(down, curr);
			}
			if(validPoint(left, visited) || left.equals(end)) {
				queue.add(left);
				pathBack.put(left, curr);
			}
			if(validPoint(right, visited) || right.equals(end)) {
				queue.add(right);
				pathBack.put(right, curr);
			}
		}
	}
	
	//checks if a point is valid for connecting rooms
	private boolean validPoint(Point p, ArrayList<Point> visited) {
		if(inBounds(p)) {
			if(level[p.x][p.y] == MatrixValues.WALL_START || level[p.x][p.y] == MatrixValues.PATH 
					|| level[p.x][p.y] == MatrixValues.DOOR_START) {
				if(!visited.contains(p)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void connectRooms2(Room r1, Room r2) {
		if(Math.random() <.5) { //half the time do y direction first
			if(r1.x > r2.x) {
				for(int a = r2.x; a <= r1.x; a++) {
					level[a][r2.y] = 1;
				}
				if(r2.y > r1.y) {
					for(int a = r1.y; a <= r2.y; a++) {
						level[r1.x][a] = 1;
					}
				}
				else {
					for(int a = r2.y; a <= r1.y; a++) {
						level[r1.x][a] = 1;
					}
				}
			}
			else {
				for(int a = r1.x; a <= r2.x; a++) {
					level[a][r1.y] = 1;
				}
				if(r2.y > r1.y) {
					for(int a = r1.y; a <= r2.y; a++) {
						level[r2.x][a] = 1;
					}
				}
				else {
					for(int a = r2.y; a <= r1.y; a++) {
						level[r2.x][a] = 1;
					}
				}
			}
		}
		else { //half the time do x direction first
			if(r1.y > r2.y) {
				for(int a = r2.y; a <= r1.y; a++) {
					level[r2.x][a] = 1;
				}
				if(r2.x > r1.x) {
					for(int a = r1.x; a <= r2.x; a++) {
						level[a][r1.y] = 1;
					}
				}
				else {
					for(int a = r2.x; a <= r1.x; a++) {
						level[a][r1.y] = 1;
					}
				}
			}
			else {
				for(int a = r1.y; a <= r2.y; a++) {
					level[r1.x][a] = 1;
				}
				if(r2.x > r1.x) {
					for(int a = r1.x; a <= r2.x; a++) {
						level[a][r2.y] = 1;
					}
				}
				else {
					for(int a = r2.x; a <= r1.x; a++) {
						level[a][r2.y] = 1;
					}
				}
			}
		}
	}
	
	public boolean inBounds(Point p) {
		if(p.x >= 0 && p.x < level.length) {
			if(p.y >= 0 && p.y < level[0].length) {
				return true;
			}
		}
		return false;
	}
	
	public void printLevel(int[][] level) {
		for(int y = 0; y < level[0].length; y++) {
			for(int x = 0; x < level.length; x++) {
				//System.out.print(String.format("%03d", level[x][y]) + " ");
				if(level[x][y] == MatrixValues.WALL_START)
					System.out.print("XX ");
				else if(level[x][y] == MatrixValues.WALL_ROOMS)
					System.out.print("WL ");
				else if(level[x][y] == MatrixValues.DOOR_START)
					System.out.print("DR ");
				else if(level[x][y] == MatrixValues.ENEMY_START)
					System.out.print("EM ");
				else if(level[x][y] == MatrixValues.PLAYER) 
					System.out.print("PL ");
				else if(level[x][y] == MatrixValues.STAIRS_UP) 
					System.out.print("SU ");
				else if(level[x][y] == MatrixValues.STAIRS_DOWN) 
					System.out.print("SD ");
				else 
					System.out.print(String.format("%02d", level[x][y]) +" ");
			}
			System.out.println();
		}
	}
	
	public Point getPlayerCoordinates() {
		return playerCoord;
	}
	
	
	public static void main(String[] args) {
		LevelGenerator game = new LevelGenerator();
		game.printLevel(game.generateLevel(1));
	}
}
