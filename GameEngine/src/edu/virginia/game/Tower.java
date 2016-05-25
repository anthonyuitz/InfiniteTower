package edu.virginia.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.virginia.engine.display.AnimatedSprite;
import edu.virginia.engine.display.DisplayObject;
import edu.virginia.engine.display.DisplayObjectContainer;
import edu.virginia.engine.display.Game;
import edu.virginia.engine.display.Sprite;
import edu.virginia.engine.display.TweenJuggler;
import edu.virginia.engine.events.Event;
import edu.virginia.engine.events.IEventListener;
import edu.virginia.engine.sound.SoundManager;

public class Tower extends Game implements MouseListener, IEventListener {
	private final static int VIEW_WIDTH = 1920;
	private final static int VIEW_HEIGHT = 1080;

	private DisplayObjectContainer backgroundCon; // black background
	private DisplayObjectContainer UI; // Put UI elements here
	private DisplayObjectContainer tileContainer; // Put tiles here
	private DisplayObjectContainer entityContainer; // Put enemies, character,
													// items, etc. here
	private DisplayObjectContainer levelContainer; // Container for tile and
													// entity containers
	private DisplayObjectContainer fogContainer; // Only for fog
	
	private int tileSize;
	private int[][] level;
	private ArrayList<Item> inventory;
	private DisplayObjectContainer minimap; //minimap of dungeon around player

	public NonTileMatrix entitiesMatrix;

	public LevelGenerator levelGenerator; // generates the levels

	private DisplayObject[][] tiles;
	private DisplayObject[][] fog;
	
	private Point altar;
	private boolean reincarnated = false;

	// private Point startPos; // In pixels. Where to start drawing level s.t.
	// player is in center
	private Point playerStartPos; // In pixels. x,y position where player
									// starts. Used to move view

	private boolean bossAlive = false;
	
	// Sprites
	private Player player;
	private DisplayObjectContainer statusBar;
	private Sprite gameOverSprite;
	private Sprite background = new Sprite("background", "background.png");

	private HashMap<String, Enemy> enemies;
	private int currLevel;
	private int enemyID;
	private int numTurns;

	TweenJuggler juggler;
	private SoundManager soundManager;

	/**
	 * Constructor. See constructor in Game.java for details on the parameters
	 * given
	 */
	public Tower() {
		super("Infinite Dungeon", VIEW_WIDTH, VIEW_HEIGHT);
		super.getMainFrame().addMouseListener(this);
		this.inventory = new ArrayList<Item>();
		this.addEventListener(this, ItemUsedEvent.ITEM_USED_EVENT);
		this.addEventListener(this, EnemyDiedEvent.ENEMY_DIED_EVENT);
		mainMenu();
		
		soundManager = new SoundManager();
	}

	public void mainMenu() {
		// Instantiation
		this.removeAll();
		this.setXPos(0);
		this.setYPos(0);
		backgroundCon = new DisplayObjectContainer("background");
		backgroundCon.addChild(background);
		this.addChild(backgroundCon);
		UI = new DisplayObjectContainer("UI");
		tileContainer = new DisplayObjectContainer("tiles");
		entityContainer = new DisplayObjectContainer("entities");
		fogContainer = new DisplayObjectContainer("fog");
		levelContainer = new DisplayObjectContainer("level");
		levelContainer.addChild(tileContainer);
		levelContainer.addChild(entityContainer);
		levelContainer.addChild(fogContainer);
		levelGenerator = new LevelGenerator();
		this.addChild(levelContainer);
		this.addChild(UI);
		enemies = new HashMap<String, Enemy>();

		// create the start screen
		numTurns = 0;
		currLevel = 0;
		DisplayObject startButton = new DisplayObject("startButton", "startButton.png");
		startButton.setXPos(VIEW_WIDTH / 2 - startButton.getUnscaledWidth() / 2);
		startButton.setYPos(3 * VIEW_HEIGHT / 4 - startButton.getUnscaledHeight() / 2);
		UI.addChild(startButton);
		DisplayObject start = new DisplayObject("start", "start.png");
		start.setXPos(VIEW_WIDTH / 2 - start.getUnscaledWidth() / 2);
		start.setYPos(VIEW_HEIGHT / 4 - start.getUnscaledHeight() / 2);
		UI.addChild(start);

	}

	public void enterDungeon(int currLevel) {
		// clear the screen
		this.removeAll();
		
		// for testing purposes add 1 health pot
		inventory.add(new Item("pot1", "pot.png", MatrixValues.ITEMS_START));

		// Instantiation
		backgroundCon = new DisplayObjectContainer("background");
		backgroundCon.addChild(background);
		this.addChild(backgroundCon);
		UI = new DisplayObjectContainer("UI");
		tileContainer = new DisplayObjectContainer("tiles");
		entityContainer = new DisplayObjectContainer("entities");
		fogContainer = new DisplayObjectContainer("fog");
		levelContainer = new DisplayObjectContainer("level");
		levelContainer.addChild(tileContainer);
		levelContainer.addChild(entityContainer);
		levelContainer.addChild(fogContainer);
		this.addChild(levelContainer);
		this.addChild(UI);
		enemies = new HashMap<String, Enemy>();

		minimap = new DisplayObjectContainer("minimap");
		minimap.setVisible(false);
		UI.addChild(minimap);
		
		// status bar (hp,xp, stats)
		statusBar = new DisplayObjectContainer("statusbar");
		altar = new Point(-1, -1);
		
		DisplayObjectContainer healthbar = new DisplayObjectContainer("healthbar");
		healthbar.setXPos(50);
		healthbar.setYPos(20);
		healthbar.addChild(new Sprite("red_bar", "large_healthbar_red.png"));
		healthbar.addChild(new Sprite("green_bar", "large_healthbar_green.png"));
		
		if(currLevel > 1) {
			float percentage = Math.min((float) player.getHealth() / (float) player.getMaxHealth(), 1.0f);
			healthbar.getChildById("green_bar").setScaleX(percentage);
		}
		
		statusBar.addChild(healthbar);

		DisplayObjectContainer xpbar = new DisplayObjectContainer("xpbar");
		xpbar.setXPos(50);
		xpbar.setYPos(0);
		xpbar.addChild(new Sprite("blackbar", "blackXPbar.png"));
		xpbar.addChild(new Sprite("purplebar", "purpleXPbar.png"));
		statusBar.addChild(xpbar);

		DisplayObjectContainer levelCircle = new DisplayObjectContainer("levelCircle", "levelCircle.png");
		statusBar.addChild(levelCircle);
		
		DisplayObjectContainer statBar = new DisplayObjectContainer("statBar");
		DisplayObjectContainer strBox = new DisplayObjectContainer("strDisplay", "statBox.png");
		DisplayObject strIcon = new DisplayObject("strIcon", "str.png");
		strIcon.setScaleX(1/1.5);
		strBox.addChild(strIcon);
		//strBox.setScaleX(1.5);
		strBox.setXPos(50);
		strBox.setYPos(70);
		statBar.addChild(strBox);
		DisplayObjectContainer agiBox = new DisplayObjectContainer("agiDisplay", "statBox.png");
		DisplayObject agiIcon = new DisplayObject("agiIcon", "agi.png");
		agiIcon.setScaleX(1/1.5);
		agiBox.addChild(agiIcon);
		//agiBox.setScaleX(1.5);
		agiBox.setXPos(125);
		agiBox.setYPos(70);
		statBar.addChild(agiBox);
		
		DisplayObjectContainer luckBox = new DisplayObjectContainer("luckDisplay", "statBox.png");
		//agiBox.setScaleX(1.5);
		luckBox.setXPos(200);
		luckBox.setYPos(70);
		statBar.addChild(luckBox);

		statusBar.addChild(statBar);
		
		UI.addChild(statusBar);
		
		enemyID = 0;

		juggler = new TweenJuggler();

		// Get level matrix
		level = levelGenerator.generateLevel(currLevel);
		levelGenerator.printLevel(level);

		entitiesMatrix = levelGenerator.getEntitiesMatrix();

		Point playerCoords = levelGenerator.getPlayerCoordinates();
		if (playerCoords != null) {
			if (currLevel == 1 && !reincarnated)
				player = new Player("player", "player.gif", playerCoords, this);
			else
				player.setCoords(playerCoords);
		}
		entityContainer.addChild(player);

		// Create tiles from matrix
		tileSize = (new DisplayObject("tile", "floor_tile1.gif")).getUnscaledWidth();
		tiles = new DisplayObject[level.length][level[0].length];
		for (int i = 0; i < level.length; i++) {
			int y = (i * tileSize);
			for (int j = 0; j < level[0].length; j++) {
				int x = (j * tileSize);
				int value = level[i][j];
				DisplayObject tile = new DisplayObject("tile");
				//TODO: fix this to just add an item child
				if (entitiesMatrix.containsValInRange(new Point(i, j), MatrixValues.ITEMS_START, MatrixValues.ITEMS_END))
					tile = new DisplayObject("item_tile:" + i + "," + j, "item.png");
				else if (value >= MatrixValues.WALKABLE_SPACE_START && value <= MatrixValues.WALKABLE_SPACE_END)
					tile = new DisplayObject("floor:" + i + "," + j, "floor_tile1.gif");
				else if (value == MatrixValues.ALTAR) {
					tile = new DisplayObject("altar:" + i + "," + j, "altar.gif");
					altar = new Point(i, j);
				}
				else if (value == MatrixValues.WALL_ROOMS)
					tile = new DisplayObject("wall:" + i + "," + j, "wall_tile1.gif");
				else if (value >= MatrixValues.WALL_START && value <= MatrixValues.WALL_END)
					tile = new DisplayObject("wall:" + i + "," + j, "black_tile.png");
				else if (value >= MatrixValues.DOOR_START && value <= MatrixValues.DOOR_END)
					tile = new DisplayObject("door:" + i + "," + j, "door1.gif");
				else if (value == MatrixValues.FOG_OF_WAR)
					tile = new DisplayObject("black:" + i + "," + j, "black_tile.png");
				else if (value == MatrixValues.STAIRS_DOWN)
					tile = new DisplayObject("stair_down:" + i + "," + j, "stairs_down.png");
				else if (value == MatrixValues.STAIRS_UP)
					tile = new DisplayObject("stair_up:" + i + "," + j, "stairs_up.png");
				else
					tile = new DisplayObject("floor:" + i + "," + j, "floor_tile1.gif");
				tile.setScaleY(1.0*tileSize/tile.getUnscaledHeight());
				tile.setScaleX(1.0*tileSize/tile.getUnscaledWidth());
				if (entitiesMatrix.containsValInRange(new Point(i, j), MatrixValues.ENEMY_START,
						MatrixValues.ENEMY_END)) {
					for (int z = 0; z < entitiesMatrix.get(i, j).size(); z++) {
						value = entitiesMatrix.getVal(i, j, z);
						if (value >= MatrixValues.ENEMY_START && value <= MatrixValues.ENEMY_END) {
							Enemy enemy;
							Point p = new Point(i, j);
							Stats stats;
							int maxHealth;
							int baseAttackDamage;
							int agility;
							int strength;
							String id = "enemy" + enemyID;
							String img;
							switch (value) {
							case MatrixValues.ENEMY_START: // Zombie
								maxHealth = 40;
								baseAttackDamage = 5;
								agility = 5;
								strength = 5;
								img = "zombie.gif";
								break;
							case MatrixValues.ENEMY_START + 1: // Mummy
								maxHealth = 45;
								baseAttackDamage = 5;
								agility = 6;
								strength = 5;
								img = "mummy.gif";
								break;
							case MatrixValues.ENEMY_START + 2: // Goblin
								maxHealth = 50;
								baseAttackDamage = 10;
								agility = 7;
								strength = 7;
								img = "goblin.gif";
								break;
							case MatrixValues.ENEMY_BOSS: // boss
								maxHealth = 100;
								bossAlive = true;
								baseAttackDamage = 15;
								agility = 1;
								strength = 15;
								img = "skeleboss.gif";
								break;
							default: // Default to zombie
								maxHealth = 40;
								baseAttackDamage = 5;
								agility = 5;
								strength = 5;
								img = "zombie.gif";
							}
							stats = new Stats(currLevel, maxHealth, baseAttackDamage, strength, agility);
							if(value == MatrixValues.ENEMY_BOSS) {
								enemy = new Enemy("boss", img, p, this, stats, value, new Room(22, 22, 10, 10));
							}
							else {
								enemy = new Enemy(id, img, p, this, stats, value);
							}
							enemyID++;
							enemies.put(enemy.getId(), enemy);
							entityContainer.addChild(enemy);
						}
					}
				}
				tile.setXPos(x);
				tile.setYPos(y);
				tiles[i][j] = tile;
				tileContainer.addChild(tile);
			}
		}
		
		// Fog matrix instantiation
		fog = new DisplayObject[level.length][level[0].length];
		for (int i = 0; i < fog.length; i++) {
			for (int j = 0; j < fog[0].length; j++) {
				fog[i][j] = new DisplayObject("fog", "fog.png");
				DisplayObject tile = fog[i][j];
				tile.setXPos(j * tileSize);
				tile.setYPos(i * tileSize);
				tile.setScaleY(1.0*tileSize/tile.getUnscaledHeight());
				tile.setScaleX(1.0*tileSize/tile.getUnscaledWidth());
				fogContainer.addChild(tile);
			}
		}
		player.clearFog();

		// Start view off such that player is in center of screen
		playerCoords = player.getCoords();
		Point playerPos = coordsToPosition(playerCoords.x, playerCoords.y);
		int playerX = playerPos.x;
		int playerY = playerPos.y;
		playerStartPos = new Point(playerX, playerY);
		this.setXPos((VIEW_WIDTH / 2) - playerStartPos.x);
		this.setYPos((VIEW_HEIGHT / 2) - playerStartPos.y);
		UI.setXPos((VIEW_WIDTH / 2) - playerStartPos.x);
		UI.setYPos((VIEW_HEIGHT / 2) - playerStartPos.y);

		// Update player position
		player.setXPos(playerX);
		player.setYPos(playerY);

		// set player status bar
		player.changeXP(player.getXP() / 10000.0);
		
		try {
			soundManager.play("ambience", true);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void playSoundEffect(String id) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		soundManager.play(id, false);
	}

	// create a 40x40 minimap at the top right of the screen
	private void minimap() {
		if (minimap.isVisible()) {
			minimap.setVisible(false);
		} else {
			minimap.removeAll();
			Point pos = player.getCoords();
			double scaleFactor = 0.25;
			int size = 40;
			int startx = Math.max(0, pos.x - size/2);
			int starty = Math.max(0, pos.y - size/2);
			
			
			minimap.setScaleX(scaleFactor);
			minimap.setScaleY(scaleFactor);
			
			
		
			for (int i = startx; i < startx + size && i < level.length; i++) {
				for (int j = starty; j < starty + size && j < level[i].length; j++) {
					DisplayObject tile = ((DisplayObjectContainer) (levelContainer.getChildById("tiles"))).getChildByIndex(i*level[0].length+j);
					minimap.addChild(tile);
					DisplayObject fog = ((DisplayObjectContainer) (levelContainer.getChildById("fog"))).getChildByIndex(i*level[0].length+j);
					minimap.addChild(fog);
				}
			}
			for(DisplayObject entity : entityContainer.getChildren()) {
				if(entity instanceof Player) {
					Player ent = (Player) entity;
					Point coords = ent.getCoords();
					if(coords.x >= startx && coords.x <startx+size && coords.x < level.length && coords.y >= starty && coords.y <starty+30 && coords.y < level.length) {
						minimap.addChild(entity);
					}
				}
				else if(entity instanceof Enemy) {
					entity = (Enemy) entity;
					Enemy ent = (Enemy) entity;
					Point coords = ent.getCoords();
					if(coords.x >= startx && coords.x <startx+size && coords.x < level.length && coords.y >= starty && coords.y <starty+30 && coords.y < level.length) {
						//check if enemy is in fog
						if(!fog[coords.x][coords.y].isVisible()) {
							minimap.addChild(entity);
						}
					}
				}
			}
			minimap.setXPos((int) (VIEW_WIDTH - size * tileSize * scaleFactor));
			minimap.setYPos(0);
			minimap.setVisible(true);
		}
	}

	@Override
	public void update(ArrayList<String> pressedKeys) {
		super.update(pressedKeys);
		
		// UI Controls
		if (currLevel > 0) {
			if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_I))) {
				openInv();
				pressedKeys.remove(KeyEvent.getKeyText(KeyEvent.VK_I));
			}
			if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_M))) {
				minimap();
				pressedKeys.remove(KeyEvent.getKeyText(KeyEvent.VK_M));
			}
			if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_P))) {
				pause(true);
				pressedKeys.remove(KeyEvent.getKeyText(KeyEvent.VK_P));
			}
			
			//update invincibility
			
			player.removeAll();
			if(player.getInv() > 0) {
				DisplayObject invincibility = new DisplayObject("invincibility", "invincibility.png");
				invincibility.setXPos(-5);
				invincibility.setYPos(-5);
				player.addChild(invincibility);
			}
		}
		
		// Arrow key movement
		if (player != null && !player.isMoving()) {
			int moveI = 0;
			int moveJ = 0;
			if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_W)) || pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_UP))) { // Move up
				moveI -= 1;
			}
			else if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_A)) || pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_LEFT))) { // Move left
				moveJ -= 1;
			}
			else if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_S)) || pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_DOWN))) { // Move down
				moveI += 1;
			}
			else if (pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_D)) || pressedKeys.contains(KeyEvent.getKeyText(KeyEvent.VK_RIGHT))) { // Move right
				moveJ += 1;
			}
			if (moveI != 0 || moveJ != 0) { // Move
				int i = player.getCoords().x + moveI;
				int j = player.getCoords().y + moveJ;
				sendPlayerToDest(i, j);
			}
		}
//		else System.out.println("Player is still moving");

		if (currLevel > 0) {
			// Update view based on player position (relative to start player
			// position)
			if (player != null && playerStartPos != null) {
				int currentPlayerX = player.getXPos();
				int currentPlayerY = player.getYPos();
				int viewX = (VIEW_WIDTH / 2) - currentPlayerX;
				int viewY = (VIEW_HEIGHT / 2) - currentPlayerY;
				this.setXPos(viewX);
				this.setYPos(viewY);
				UI.setXPos(-viewX);
				UI.setYPos(-viewY);
				backgroundCon.setXPos(-viewX);
				backgroundCon.setYPos(-viewY);

			}
			if (enemies != null) {
				recycleEnemies();
			}
			if (juggler != null) {
				juggler.nextFrame();
			}

		}
	}

	public DisplayObject drawString(String s, Color c, int fontSize, Graphics2D g2d, String name) {

		// draw current level string
		DisplayObject stringSprite = new DisplayObject(name);

		// Create damage string
		Font f = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		g2d.setFont(f);
		FontMetrics fm = g2d.getFontMetrics();
//		int width = fm.stringWidth(s);
//		int height = fm.getHeight();
		g2d.dispose();

		img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);

		g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(f);
		fm = g2d.getFontMetrics();

		g2d.setColor(c);
		g2d.drawString(s, 0, fm.getAscent());
		g2d.dispose();

		// Add sprite in correct location (above player)
		stringSprite.setImage(img);
		return stringSprite;

	}

	@Override
	public void draw(Graphics g) {
		super.draw(g);
		if (currLevel > 0) {
			//draw level and stats of player
			Graphics2D g2d = (Graphics2D) g;
			DisplayObject levelString = drawString(Integer.toString(player.getLevel()), Color.black, 24, g2d,"levelString");
			if(Integer.toString(player.getLevel()).length() == 1) {
				levelString.setXPos(18);
			}
			else {
				levelString.setXPos(10);
			}
			levelString.setYPos(10);

			((DisplayObjectContainer) statusBar.getChildById("levelCircle")).removeAll();
			((DisplayObjectContainer) statusBar.getChildById("levelCircle")).addChild(levelString);
			
			DisplayObject strString;
			if(player.getStrBuff() > 0) {
				strString = drawString(Integer.toString((int)(player.getStr())), Color.red, 24, g2d, "strString");
			}
			else {
				strString = drawString(Integer.toString((int)(player.getStr())), Color.black, 24, g2d, "strString");
			}
			strString.setXPos(20);
			strString.setYPos(10);

			DisplayObject strGrowthString = drawString("+" + String.format("%.2f", player.getStrGrowth()), Color.black,14,g2d,"strGrowthString");
			strGrowthString.setXPos(10);
			strGrowthString.setYPos(50);
			DisplayObject strIcon = new DisplayObject("strIcon", "str.png");
			strIcon.setXPos(5);
			strIcon.setScaleX(1/1.5);
			
			DisplayObjectContainer strDisplay = ((DisplayObjectContainer) ((DisplayObjectContainer) statusBar.getChildById("statBar"))
					.getChildById("strDisplay"));
			strDisplay.removeAll();
			strDisplay.addChild(strIcon);
			strDisplay.addChild(strString);
			strDisplay.addChild(strGrowthString);
		
			DisplayObject agiString;
			if(player.getAgiBuff() > 0) { 
				agiString = drawString(Integer.toString((int)(player.getAgi())), Color.red, 24, g2d,"agiString");
			}
			else  {
				agiString = drawString(Integer.toString((int)(player.getAgi())), Color.black, 24, g2d,"agiString");
			}
			agiString.setXPos(20);
			agiString.setYPos(10);
			DisplayObject agiGrowthString = drawString("+" + String.format("%.2f",player.getAgiGrowth()), Color.black, 14, g2d,"agiGrowthString");
			agiGrowthString.setXPos(10);
			agiGrowthString.setYPos(50);
			DisplayObject agiIcon = new DisplayObject("agiIcon", "agi.png");
			agiIcon.setXPos(5);
			agiIcon.setScaleX(1/1.5);
			
				
			DisplayObjectContainer agiDisplay = ((DisplayObjectContainer) ((DisplayObjectContainer) statusBar.getChildById("statBar"))
					.getChildById("agiDisplay"));
			agiDisplay.removeAll();
			agiDisplay.addChild(agiIcon);
			agiDisplay.addChild(agiString);			
			agiDisplay.addChild(agiGrowthString);
			
			DisplayObject luckString;
			if(player.getLuckBuff() > 0) {
				luckString = drawString(Integer.toString((int)(player.getLuck())), Color.red, 24, g2d, "luckString");
			}
			else {
				luckString = drawString(Integer.toString((int)(player.getLuck())), Color.black, 24, g2d, "luckString");
			}
			luckString.setXPos(20);
			luckString.setYPos(10);

			DisplayObject luckGrowthString = drawString("+" + String.format("%.2f", player.getLuckGrowth()), Color.black,14,g2d,"luckGrowthString");
			luckGrowthString.setXPos(10);
			luckGrowthString.setYPos(50);
			DisplayObject luckIcon = new DisplayObject("luckIcon", "luck.png");
			luckIcon.setXPos(5);
			luckIcon.setScaleX(1/1.5);
			
			DisplayObjectContainer luckDisplay = ((DisplayObjectContainer) ((DisplayObjectContainer) statusBar.getChildById("statBar"))
					.getChildById("luckDisplay"));
			luckDisplay.removeAll();
			luckDisplay.addChild(luckIcon);
			luckDisplay.addChild(luckString);
			luckDisplay.addChild(luckGrowthString);

			
			//increase stats if points available
			if(player.getStatPoints() > 0) {
				DisplayObject incStr = new DisplayObject("incStr", "plus.png");
				incStr.setXPos(50);
				incStr.setYPos(10);
				strDisplay.addChild(incStr);
				DisplayObject incStrGrowth = new DisplayObject("incStrGrowth", "plus.png");
				incStrGrowth.setXPos(50);
				incStrGrowth.setYPos(50);
				strDisplay.addChild(incStrGrowth);
				DisplayObject incAgi = new DisplayObject("incAgi", "plus.png");
				incAgi.setXPos(50);
				incAgi.setYPos(10);
				agiDisplay.addChild(incAgi);
				DisplayObject incAgiGrowth = new DisplayObject("incAgiGrowth", "plus.png");
				incAgiGrowth.setXPos(50);
				incAgiGrowth.setYPos(50);
				agiDisplay.addChild(incAgiGrowth);
				DisplayObject incLuck = new DisplayObject("incLuck", "plus.png");
				incLuck.setXPos(50);
				incLuck.setYPos(10);
				luckDisplay.addChild(incLuck);
				DisplayObject incLuckGrowth = new DisplayObject("incLuckGrowth", "plus.png");
				incLuckGrowth.setXPos(50);
				incLuckGrowth.setYPos(50);
				luckDisplay.addChild(incLuckGrowth);

				
			}
		}
	}

	public static void main(String[] args) {
		Tower game = new Tower();
		game.start();
	}

	// open or close inventory
	private void openInv() {
		
		if (UI.getChildById("inv") == null) {
			DisplayObjectContainer inv = new DisplayObjectContainer("inv");
			inv.setYPos(VIEW_HEIGHT - 5 * 85);

			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					DisplayObjectContainer invBox = new DisplayObjectContainer("itemContainer", "invBox.png");
					if (i * 5 + j < inventory.size()) {
						DisplayObject item = inventory.get(i * 5 + j);
						invBox.addChild(item);
					}
					invBox.setXPos(i * 75);
					invBox.setYPos(j * 75);
					inv.addChild(invBox);
				}
			}

			UI.addChild(inv);

		} else {
			UI.removeChild(UI.getChildById("inv"));
		}

	}

	// true to pause, false to unpause
	public void pause(boolean pause) {
		if (pause) {
			soundManager.stop("ambience");
			Sprite grayScreen = new Sprite("greyScreen", "background.png");
			grayScreen.setAlpha(0.5f);
			Sprite cont = new Sprite("continue", "continue.png");
			cont.setXPos((VIEW_WIDTH / 2) - (cont.getUnscaledWidth() / 2));
			cont.setYPos((VIEW_HEIGHT / 4) - (cont.getUnscaledHeight() / 2));
			Sprite mainMenu = new Sprite("mainMenu", "mainMenu.png");
			mainMenu.setXPos((VIEW_WIDTH / 2) - (mainMenu.getUnscaledWidth() / 2));
			mainMenu.setYPos((3 * VIEW_HEIGHT / 4) - (mainMenu.getUnscaledHeight() / 2));

			UI.addChild(grayScreen);
			UI.addChild(cont);
			UI.addChild(mainMenu);

			currLevel = -2;
		} else {
			UI.removeChild(UI.getChildById("greyScreen"));
			UI.removeChild(UI.getChildById("continue"));
			UI.removeChild(UI.getChildById("mainMenu"));
			currLevel = 1; // this will have to update with level later on
			try {
				soundManager.play("ambience", true);
			} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void endGame() {
		soundManager.stop("ambience");
		try {
			playSoundEffect("gameover");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Sprite grayScreen = new Sprite("greyScreen", "background.png");
		grayScreen.setAlpha(0.5f);

		Sprite gameOverText = new Sprite("gameOverText", "gameover.png");
		gameOverText.setXPos((VIEW_WIDTH / 2) - (gameOverText.getUnscaledWidth() / 2));
		gameOverText.setYPos((VIEW_HEIGHT / 4) - (gameOverText.getUnscaledHeight() / 2));
		Sprite tryAgain = new Sprite("tryAgain", "tryAgain.png");
		tryAgain.setXPos((VIEW_WIDTH / 2) - (tryAgain.getUnscaledWidth() / 2));
		tryAgain.setYPos((3 * VIEW_HEIGHT / 4) - (tryAgain.getUnscaledHeight() / 2));

		UI.addChild(grayScreen);
		UI.addChild(gameOverText);
		UI.addChild(tryAgain);

		currLevel = -1;
		bossAlive = false;
	}

	/*
	 * Call this when the player has done something (move, attack, etc.) to end
	 * their turn.
	 */
	public void nextTurn() {
		Set<String> keys = enemies.keySet();
		for (String key : keys) {
			enemies.get(key).processTurn();
		}
		//update minimap
		minimap();
		minimap();
	}

	/*
	 * Checks each enemy and removes it if it is dead, also updating the value
	 * in the local copy of the matrix
	 */
	public void recycleEnemies() {
		int i = 0;
		Set<String> keys = enemies.keySet();
		for (String key : keys) {
			Enemy enemy = enemies.get(key);
			if (enemy != null && enemy.getHealth() <= 0) {
				enemies.remove(i);
			}
		}

	}

	// --------------- Player Movement ------------------------

	/*
	 * Uses A* Breadth First Search Algorithm as described here:
	 * http://www.redblobgames.com/pathfinding/a-star/introduction.html to
	 * calculate the best path to a destination tile.
	 */
	public ArrayList<Point> computePath(int startI, int startJ, int destI, int destJ) {

		Queue<Point> frontier = new LinkedList<Point>();
		HashMap<String, Point> cameFrom = new HashMap<String, Point>();
		cameFrom.put(startI + "," + startJ, null);
		frontier.add(new Point(startI, startJ));

		while (!frontier.isEmpty()) {
			Point current = frontier.poll();
			if (current.x == destI && current.y == destJ)
				break;
			int i = current.x;
			int j = current.y;
			Point n1 = new Point(i + 1, j);
			Point n2 = new Point(i - 1, j);
			Point n3 = new Point(i, j + 1);
			Point n4 = new Point(i, j - 1);
			Point[] neighbors = { n1, n2, n3, n4 };
			for (int k = 0; k < neighbors.length; k++) {
				Point p = neighbors[k];
				if (levelGenerator.inBounds(p) && !cameFrom.containsKey(p.x + "," + p.y)
						&& MatrixValues.isWalkable(level[p.x][p.y])) {
					frontier.add(p);
					cameFrom.put(p.x + "," + p.y, current);
				}
			}
		}

		Point current = new Point(destI, destJ);
		ArrayList<Point> path = new ArrayList<Point>();
		path.add(current);
		while (current != null && !(current.x == startI && current.y == startJ)) {
			current = cameFrom.get(current.x + "," + current.y);
			path.add(current);
			// System.out.println(current.x + "," + current.y);
		}
		path.remove(path.size() - 1);
		Collections.reverse(path);

		return path;
	}

	/*
	 * Makes player move to a certain i,j coordinate in the matrix
	 */
	public void sendPlayerToDest(int destI, int destJ) {
		Point closest = findClosestDest(player.getCoords().x, player.getCoords().y, destI, destJ);

		ArrayList<Point> path = computePath(player.getCoords().x, player.getCoords().y, closest.x, closest.y);
		player.setCurrentPath(path);
		// System.out.println("Path:");
		// for (int i = 0; i < path.size(); i++) {
		// System.out.print("(" + path.get(i).x + "," + path.get(i).y + ") ");
		// }
		// System.out.println();
	}

	/*
	 * Finds closest walkable tile if non-walkable tile is clicked
	 */
	public Point findClosestDest(int startI, int startJ, int destI, int destJ) {
		if (destI >= 0 && destJ >= 0 && destI < level.length && destJ < level[0].length
				&& MatrixValues.isWalkable(level[destI][destJ])) {
			return new Point(destI, destJ);
		} else {
			int iDiff = startI - destI;
			int jDiff = startJ - destJ;
			int incrI, incrJ;
			if (iDiff > 0)
				incrI = 1;
			else
				incrI = -1;
			if (jDiff > 0)
				incrJ = 1;
			else
				incrJ = -1;

			int i = destI;
			int j = destJ;
			while ((iDiff > 0 && i <= startI) || (iDiff <= 0 && i >= startI)) {
				while ((jDiff > 0 && j <= startJ) || (jDiff <= 0 && j >= startJ)) {
					if (i >= 0 && j >= 0 && i < level.length && j < level[0].length
							&& MatrixValues.isWalkable(level[i][j]))
						return new Point(i, j);

					j += incrJ;
				}
				j = destJ;
				i += incrI;
			}
		}
		return null;
	}

	// ----------- Coordinate/Position conversion methods ---------------

	/*
	 * Converts x,y position to matrix coordinates
	 */
	public Point positionToCoords(int x, int y) {
		int i = y / tileSize;
		int j = x / tileSize;
		// System.out.println("positionToCoords: " + "input:(" + x + "," + y +
		// ") output:(" + i + "," + j + ")");
		return new Point(i, j);
	}

	/*
	 * Converts mouse click to x,y position
	 */
	public Point mousePositionToCoords(int x, int y) {
		int i = (int) (((double) (y - this.getYPos()) / (double) tileSize));
		int j = (int) (((double) (x - this.getXPos()) / (double) tileSize));
		// System.out.println("mousePositionToCoords: " + "input:(" + x + "," +
		// y + ") output:(" + i + "," + j + ")");
		return new Point(i, j);
	}

	/*
	 * Converts matrix coordinates to x,y position
	 */
	public Point coordsToPosition(int i, int j) {
		int x = (j * tileSize);
		int y = (i * tileSize);
		// System.out.println("coordsToPosition: " + "input:(" + i + "," + j +
		// ") output:(" + x + "," + y + ")");
		return new Point(x, y);
	}

	// ------------------- Getters/Setters --------------------

	public Player getPlayer() {
		return player;
	}

	public DisplayObjectContainer getUI() {
		return UI;
	}

	public void setUI(DisplayObjectContainer uI) {
		UI = uI;
	}

	public DisplayObjectContainer getFogContainer() {
		return fogContainer;
	}

	public void setFogContainer(DisplayObjectContainer fogContainer) {
		this.fogContainer = fogContainer;
	}

	public DisplayObjectContainer getTileContainer() {
		return tileContainer;
	}

	public DisplayObjectContainer getEntityContainer() {
		return entityContainer;
	}

	public DisplayObjectContainer getLevelContainer() {
		return levelContainer;
	}

	public DisplayObjectContainer getStatusBar() {
		return statusBar;
	}

	public void setStatusBar(DisplayObjectContainer statusBar) {
		this.statusBar = statusBar;
	}

	public int[][] getLevel() {
		return level;
	}

	public void setLevel(int[][] level) {
		this.level = level;
	}
	
	public DisplayObject[][] getFog() {
		return fog;
	}

	public void setFog(DisplayObject[][] fog) {
		this.fog = fog;
	}
	//check if click was in hitbox
	public boolean clickedIn(Rectangle hitBox, int x, int y) {
		if (x >= hitBox.x && x <= hitBox.x + hitBox.width && y >= hitBox.y
				&& y <= hitBox.y + hitBox.height) {
			return true;
		}
		else {
			return false;
		}
	}
	// ----------- Mouse Listener Methods --------------
	
	int topInset = this.getMainFrame().getInsets().top;
	int leftInset = this.getMainFrame().getInsets().left;

	public void mouseClicked(MouseEvent arg0) {
		int x = arg0.getX() - leftInset;
		// mouse coordinates include the status bar which has a height of 25 on
		// my computer
		int y = arg0.getY() - topInset;
		System.out.println("Mouse coords: x=" + x + " y=" + y);

		if (currLevel > 0) {

			if (UI.getChildById("inv") != null) {
				// check for inventory use
				int usedItem = -1;
				// for now check if inventory was clicked. If it was, don't move
				// character
				boolean invClicked = false;
				for (int a = 0; a < inventory.size(); a++) {
					DisplayObjectContainer inv = (DisplayObjectContainer) UI.getChildById("inv");
					Rectangle hitBox = inv.getChildByIndex(a).getHitbox();
					if (clickedIn(hitBox,x,y)) {
						invClicked = true;
						Item item = (Item) (((DisplayObjectContainer) inv.getChildByIndex(a)).getChildByIndex(0));
						this.dispatchEvent(new ItemUsedEvent(this, item.getID()));
						usedItem = a;

					}
				}
				if (usedItem != -1) {
					// remove item
					((DisplayObjectContainer) ((DisplayObjectContainer) UI.getChildById("inv"))
							.getChildByIndex(usedItem)).removeChildByIndex(0);

					inventory.remove(usedItem);
					//update inventory screen
					openInv();
					openInv();
				}
				if (invClicked) {
					return;
				}
			}

			if(player.getStatPoints() > 0) {
				System.out.println(x + "," + y);
				DisplayObjectContainer statBar =  (DisplayObjectContainer)  (getStatusBar().getChildById("statBar"));
				DisplayObjectContainer strDisplay = (DisplayObjectContainer) statBar.getChildById("strDisplay");
				Rectangle incStrHitBox = strDisplay.getChildById("incStr").getHitbox();
				Rectangle incStrGrowthHitBox = strDisplay.getChildById("incStrGrowth").getHitbox();
				
				DisplayObjectContainer agiDisplay = (DisplayObjectContainer) statBar.getChildById("agiDisplay");
				Rectangle incAgiHitBox = agiDisplay.getChildById("incAgi").getHitbox();
				Rectangle incAgiGrowthHitBox = agiDisplay.getChildById("incAgiGrowth").getHitbox();
				
				DisplayObjectContainer luckDisplay = (DisplayObjectContainer) statBar.getChildById("luckDisplay");
				Rectangle incLuckHitBox = luckDisplay.getChildById("incLuck").getHitbox();
				Rectangle incLuckGrowthHitBox = luckDisplay.getChildById("incLuckGrowth").getHitbox();
				
				if(clickedIn(incStrHitBox,x,y)) {
					player.increaseStr();
					return;
				}
				else if(clickedIn(incAgiHitBox,x,y)) {
					player.increaseAgi();
					return;
				}
				else if(clickedIn(incAgiGrowthHitBox,x,y)) {
					player.increaseAgiGrowth();
					return;
				}
				else if(clickedIn(incStrGrowthHitBox,x,y)) {
					player.increaseStrGrowth();
					return;
				}
				else if(clickedIn(incLuckHitBox,x,y)) {
					player.increaseLuck();
					return;
				}
				else if(clickedIn(incLuckGrowthHitBox,x,y)) {
					player.increaseLuckGrowth();
					return;
				}
			}
			
			Point coords = mousePositionToCoords(x, y);
			int i = coords.x;
			int j = coords.y;
			Point closest = findClosestDest(player.getCoords().x, player.getCoords().y, i, j);
			if (closest != null) {
				i = closest.x;
				j = closest.y;
			}
			//System.out.println(i + "," + j);
			if (entitiesMatrix.containsValInRange(new Point(i, j), MatrixValues.ENEMY_START, MatrixValues.ENEMY_END)) { // Attack
				// enemy
				Point playerCoords = player.getCoords();
				int playerI = playerCoords.x;
				int playerJ = playerCoords.y;
				// If player is adjacent to enemy, don't move
				if (Math.abs(playerI - i) <= 1 && Math.abs(playerJ - j) <= 1) {

					Set<String> keys = enemies.keySet();
					for (String key : keys) {
						Enemy enemy = enemies.get(key);
						if (enemy.getCoords().x == i && enemy.getCoords().y == j && enemy.isVisible()) {
							player.attack(enemy);
							break;
						}
					}
				} else { // Move to tile adjacent to enemy
//					sendPlayerToDest(i, j);
				}
			} else if (closest != null) {
//				sendPlayerToDest(i, j);
			}

		}
		// start screen
		else if (currLevel == 0) {
			// start the game
			Rectangle startHitBox = UI.getChildById("startButton").getHitbox();

			if (clickedIn(startHitBox,x,y)) {
				System.out.println("entering dungeon");
				currLevel = 1;
				enterDungeon(currLevel);
			}
		}
		// game over
		else if (currLevel == -1) {
			// game over screen
			Rectangle tryAgainHitBox = UI.getChildById("tryAgain").getHitbox();

			if (clickedIn(tryAgainHitBox,x,y)) {
				System.out.println("entering dungeon");
				currLevel = 1;
				numTurns = 0;
				enterDungeon(currLevel);
			}
		}
		// pause screen
		else if (currLevel == -2) {

			Rectangle unpause = UI.getChildById("continue").getHitbox();
			Rectangle quit = UI.getChildById("mainMenu").getHitbox();

			if (clickedIn(unpause,x,y)) {

				pause(false);
			}

			if (clickedIn(quit,x,y)) {

				mainMenu();
			}

		}
		//System.out.println("Tile clicked: " + x + "," + y);
	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}
	
	public int getTileSize() {
		return this.tileSize;
	}

	// -------------- Event Listener -----------------

	public void handleEvent(Event event) {
		if (event.getEventType().equals(TurnEndedEvent.TURN_ENDED_EVENT)) {
			if (player.getCoords().equals(levelGenerator.stairsUp) && !bossAlive) {
				try {
					playSoundEffect("stairs");
				} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				player.reincarnate(-1);
				currLevel++;
				this.enterDungeon(currLevel);
				player.increaseXP(10000);
			}
			else if(player.getCoords().equals(altar)) {
				player.reincarnate(currLevel);
				reincarnated = true;
				currLevel = 1;
				bossAlive = false;
				this.enterDungeon(currLevel);
			}
			int i = player.getCoords().x;
			int j = player.getCoords().y;
			while (entitiesMatrix.containsValInRange(new Point(i, j), MatrixValues.ITEMS_START,
					MatrixValues.ITEMS_END)) {
				try {
					playSoundEffect("itempickedup");
				} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//assume one entity per tile for the moment, will fix if this is an issue
				int itemNum = entitiesMatrix.getVal(i, j, 0);
				if(itemNum == MatrixValues.ITEMS_START) {
					inventory.add(new Item("pot1", "pot.png", itemNum));
				}
				else if(itemNum == MatrixValues.ITEMS_START+1) {
					inventory.add(new Item("invpot", "invpot.png", itemNum));
				}
				if(itemNum == MatrixValues.ITEMS_START+2) {
					inventory.add(new Item("luckpot", "luckpot.png", itemNum));
				}
				if(itemNum == MatrixValues.ITEMS_START+3) {
					inventory.add(new Item("strpot", "strpot.png", itemNum));
				}
				if(itemNum == MatrixValues.ITEMS_START+4) {
					inventory.add(new Item("agipot", "agipot.png", itemNum));
				}
				player.setPrevMatrixVal(MatrixValues.WALKABLE_SPACE_START);
				entitiesMatrix.removeVal(new Point(i, j), itemNum);
				DisplayObject tile = new DisplayObject("floor:" + i + "," + j, "floor_tile1.gif");
				tiles[i][j] = tile;
				tileContainer.getChildById("item_tile:" + i + "," + j).setImage("floor_tile1.gif");
				tileContainer.getChildById("item_tile:" + i + "," + j).setId("floor:" + i + "," + j);
				openInv();
				openInv();
			}
			numTurns++;
			nextTurn();
		}
		// item usage
		if (event.getEventType().equals(ItemUsedEvent.ITEM_USED_EVENT)) {
			int id = ((ItemUsedEvent) event).getId();
			//health potion
			if (id == MatrixValues.ITEMS_START) {
				player.setHealth(player.getHealth() + player.getMaxHealth()/4);
				DisplayObjectContainer statusbar = getStatusBar();
				DisplayObjectContainer healthbar = (DisplayObjectContainer) statusbar.getChildById("healthbar");
				DisplayObject greenBar = healthbar.getChildById("green_bar");
				float percentage = Math.min((float) player.getHealth() / (float) player.getMaxHealth(), 1.0f);
				greenBar.setScaleX(percentage);
			}
			if (id == MatrixValues.ITEMS_START+1) {
				player.addInv(5);
			}
			if (id == MatrixValues.ITEMS_START+2) {
				player.addLuck(10);
			}
			if (id == MatrixValues.ITEMS_START+3) {
				player.addStr(10);
			}
			if (id == MatrixValues.ITEMS_START+4) {
				player.addAgi(10);
			}
			try {
				playSoundEffect("gulp");
			} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		if (event.getEventType().equals(EnemyDiedEvent.ENEMY_DIED_EVENT)) {
			Enemy temp = (Enemy) event.getSource();
			if(temp.getId().equals("boss")) {
				bossAlive = false;
				player.increaseXP(10000);
			}
			else {
				player.increaseXP((int) (1000 / (Math.pow(10, player.getLevel() - temp.getLevel()))));
				//chance of getting an item from an enemy
				if(Math.random() < 0.1 + Math.pow(player.getLuck(), .8) / 100.0) {
					int itemNum = Item.getItem();
					inventory.add(Item.getItem(itemNum));
					openInv();
					openInv();
				}
			}
		}
	}

	// ---------------------------------------------
}
