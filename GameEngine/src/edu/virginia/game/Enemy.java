package edu.virginia.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.virginia.engine.display.AnimatedSprite;
import edu.virginia.engine.display.DisplayObject;
import edu.virginia.engine.display.DisplayObjectContainer;
import edu.virginia.engine.display.Sprite;
import edu.virginia.engine.display.Tween;
import edu.virginia.engine.display.TweenTransitions;
import edu.virginia.engine.display.TweenableParams;
import edu.virginia.engine.events.Event;
import edu.virginia.engine.events.IEventListener;
import edu.virginia.engine.events.TweenEvent;

public class Enemy extends AnimatedSprite implements IEventListener {
	
	private Point coords;
	private Tower game;

	private DisplayObjectContainer healthbar;
	private int health;
	private boolean moving;
	private Point dest; // x,y coords 
	private int pathIndex;
	private ArrayList<Point> currentPath;
	private int matrixValue;
	private boolean isBoss = false;
	private Room room;
	private boolean charging = false;
	private ArrayList<DisplayObject> fire;
	
	// Stats
	private int agility;
	private int level;
	private int strength;
	private int maxHealth = 40;
	private static final double SPEED = 5;
	private int baseAttackDamage;
	private int visualField = 4; // Distance at which the enemy can search for the player

	public Enemy(String id, String imageFileName, Point coords, Tower game, Stats stats, int matrixValue) {
		super(id, imageFileName);
		this.coords = coords;
		this.game = game;
		Point positions = game.coordsToPosition(coords.x, coords.y);
		super.setXPos(positions.x);
		super.setYPos(positions.y);
		this.healthbar = new DisplayObjectContainer("enemy_healthbar");
		this.addChild(healthbar);
		this.healthbar.addChild(new Sprite("red_bar", "small_healthbar_red.png"));
		this.healthbar.addChild(new Sprite("green_bar", "small_healthbar_green.png"));
		this.healthbar.setXPos(this.getUnscaledWidth()/2 - healthbar.getChildById("red_bar").getUnscaledWidth()/2);
		this.healthbar.setYPos(-healthbar.getChildById("red_bar").getUnscaledHeight() - 5);
		this.healthbar.setVisible(false);
		this.moving = false;
		this.pathIndex = -1;
		this.hasPhysics = true;
		this.matrixValue = matrixValue;
		room = null;
		fire = new ArrayList<DisplayObject>();
		
		// Stats
		this.maxHealth = stats.maxHealth;
		this.agility = stats.agility;
		this.strength = stats.strength;
		this.baseAttackDamage = stats.baseAttackDamage;
		this.health = this.maxHealth;
		this.level = stats.level;
		
		//scaling graphics
		this.setScaleY(1.0*game.getTileSize()/this.getUnscaledHeight());
		this.setScaleX(1.0*game.getTileSize()/this.getUnscaledWidth());
	}
	
	public Enemy(String id, String imageFileName, Point coords, Tower game, Stats stats, int matrixValue, Room room) {
		super(id, imageFileName);
		this.coords = coords;
		this.game = game;
		Point positions = game.coordsToPosition(coords.x, coords.y);
		super.setXPos(positions.x);
		super.setYPos(positions.y);
		this.healthbar = new DisplayObjectContainer("enemy_healthbar");
		this.addChild(healthbar);
		this.healthbar.addChild(new Sprite("red_bar", "small_healthbar_red.png"));
		this.healthbar.addChild(new Sprite("green_bar", "small_healthbar_green.png"));
		this.healthbar.setXPos(this.getUnscaledWidth()/2 - healthbar.getChildById("red_bar").getUnscaledWidth()/2);
		this.healthbar.setYPos(-healthbar.getChildById("red_bar").getUnscaledHeight() - 5);
		this.healthbar.setVisible(false);
		this.moving = false;
		this.pathIndex = -1;
		this.hasPhysics = true;
		this.matrixValue = matrixValue;
		this.room = room;
		isBoss = true;
		fire = new ArrayList<DisplayObject>();
		
		// Stats
		this.maxHealth = stats.maxHealth;
		this.agility = stats.agility;
		this.strength = stats.strength;
		this.baseAttackDamage = stats.baseAttackDamage;
		this.health = this.maxHealth;
		this.level = stats.level;
		
		//scaling graphics
		this.setScaleY(1.0*game.getTileSize()/this.getUnscaledHeight());
		this.setScaleX(1.0*game.getTileSize()/this.getUnscaledWidth());
	}
	
	public void processTurn() {
		if (health > 0) {
			if(charging) {
				int i = game.getPlayer().getCoords().x;
				int j = game.getPlayer().getCoords().y;
				if (Math.abs(coords.x-i) <= 1 && Math.abs(coords.y-j) <= 1) { // If player is adjacent, attack
					pathIndex = -1;
					currentPath = null;
					moving = false;
					super.setXVelocity(0);
					super.setYVelocity(0);
					attack(game.getPlayer());
				}
				else {
					this.setDamageString("Missed");
					this.setDrawDamageString(true);
				}
				this.removeChild(fire.get(0));
				fire.remove(0);
				charging = false;
				endTurn();
			}
			else {
				// Determine if enemy should move or change paths
				// Search 7x7 matrix which the enemy is the center of for player
				Point destCoords = new Point();
				if (currentPath != null && currentPath.size() > 0){
					destCoords = new Point(currentPath.get(currentPath.size()-1).x, currentPath.get(currentPath.size()-1).y);
				}
				boolean playerFound = false;
				if(isBoss) {
					int i = game.getPlayer().getCoords().x;
					int j = game.getPlayer().getCoords().y;
					if(room.contains(i,j)) {
						if (game.levelGenerator.inBounds(new Point(i, j)) && game.entitiesMatrix.containsVal(new Point(i, j), MatrixValues.PLAYER)) { // Player is near, move toward
							playerFound = true;
							ArrayList<Point> path = game.computePath(this.getCoords().x, this.getCoords().y, i, j);
							if (path.size() > 0) path.remove(path.size()-1); // Remove the final dest (player tile)
							if (Math.abs(coords.x-i) <= 1 && Math.abs(coords.y-j) <= 1) { // If player is adjacent, attack
								pathIndex = -1;
								currentPath = null;
								moving = false;
								super.setXVelocity(0);
								super.setYVelocity(0);
								attack(game.getPlayer());
								endTurn();
							}
							else if (currentPath == null // Enemy is not heading anywhere
									|| (path.size() > 0 && destCoords.x != path.get(path.size()-1).x 
									&& destCoords.y != path.get(path.size()-1).y)) { // player has moved, change path
								moving = false;
								setCurrentPath(path);
							}
							else { // Move to next position in path
								Point next = currentPath.get(pathIndex);
								Point positions = game.coordsToPosition(next.x, next.y);
								int nextI = next.x;
								int nextJ = next.y;
								int nextVal = game.getLevel()[nextI][nextJ];
								if (nextVal >= MatrixValues.ENEMY_START && nextVal <= MatrixValues.ENEMY_END) { // Another enemy stands in path
									ArrayList<Point> newPath = game.computePath(this.coords.x, this.coords.y, destCoords.x, destCoords.y);
									setCurrentPath(newPath);
								}
								else this.move(positions.x, positions.y);
							}
						}
					}
				}
				else {
					for (int i = this.coords.x - visualField; i <= this.coords.x + visualField; i++) {
						for (int j = this.coords.y - visualField; j <= this.coords.y + visualField; j++) {
							if (game.levelGenerator.inBounds(new Point(i, j)) && game.entitiesMatrix.containsVal(new Point(i, j), MatrixValues.PLAYER)) { // Player is near, move toward
								playerFound = true;
								ArrayList<Point> path = game.computePath(this.getCoords().x, this.getCoords().y, i, j);
								if (path.size() > 0) path.remove(path.size()-1); // Remove the final dest (player tile)
								if (Math.abs(coords.x-i) <= 1 && Math.abs(coords.y-j) <= 1) { // If player is adjacent, attack
									pathIndex = -1;
									currentPath = null;
									moving = false;
									super.setXVelocity(0);
									super.setYVelocity(0);
									attack(game.getPlayer());
									endTurn();
								}
								else if (currentPath == null // Enemy is not heading anywhere
										|| (path.size() > 0 && destCoords.x != path.get(path.size()-1).x 
										&& destCoords.y != path.get(path.size()-1).y)) { // player has moved, change path
									moving = false;
									setCurrentPath(path);
								}
								else { // Move to next position in path
									Point next = currentPath.get(pathIndex);
									Point positions = game.coordsToPosition(next.x, next.y);
									int nextI = next.x;
									int nextJ = next.y;
									int nextVal = game.getLevel()[nextI][nextJ];
									if (nextVal >= MatrixValues.ENEMY_START && nextVal <= MatrixValues.ENEMY_END) { // Another enemy stands in path
										ArrayList<Point> newPath = game.computePath(this.coords.x, this.coords.y, destCoords.x, destCoords.y);
										setCurrentPath(newPath);
									}
									else this.move(positions.x, positions.y);
								}
							}
						}
					}
				}
				if (!playerFound && currentPath != null) {
					Point next = currentPath.get(pathIndex);
					Point positions = game.coordsToPosition(next.x, next.y);
					int nextI = next.x;
					int nextJ = next.y;
					if (game.entitiesMatrix.containsValInRange(new Point(nextI, nextJ), MatrixValues.ENEMY_START, MatrixValues.ENEMY_END)) { // Another enemy stands in path
						ArrayList<Point> newPath = game.computePath(this.coords.x, this.coords.y, destCoords.x, destCoords.y);
						if (newPath != null && newPath.size() > 0) setCurrentPath(newPath);
						else {
							this.setXVelocity(0);
							this.setYVelocity(0);
							moving = false;
						}
					}
					else this.move(positions.x, positions.y);
				}
				else if (!playerFound) endTurn();
			}
		}
	}
	
	boolean waiting = false;
	public void move(int x, int y) {
		Point coords = game.positionToCoords(x, y);
		boolean enemyInPath = game.entitiesMatrix.containsValInRange(coords, MatrixValues.ENEMY_START, MatrixValues.ENEMY_END);
		if (!moving && health > 0 && !enemyInPath) {
			waiting = false;
			moving = true;
			dest = new Point(x,y);
			changeMatrixValues();
			
			double xVel = x - super.getXPos();
			double yVel = y - super.getYPos();
			double mag = Math.sqrt(xVel * xVel + yVel * yVel);
			xVel = xVel * SPEED/mag;
			yVel = yVel * SPEED/mag;
			super.setXVelocity(xVel);
			super.setYVelocity(yVel);
		}
		else if (enemyInPath) waiting = true;
	}
	
	public void attack(Player player) {
		this.healthbar.setVisible(true);
		if(Math.random() < .5 || !isBoss) {
//			double baseHitPercent = .75;
//			double hitPercent = baseHitPercent + baseHitPercent * (agility/100);
			Random rand = new Random();
//			int num = rand.nextInt(100)+1;
//			if (num < hitPercent*100) {
				int attackDamage = baseAttackDamage + rand.nextInt(strength);
				player.takeDamage(attackDamage);
//			}
//			else {
				//			System.out.println("Enemy missed");
//				DisplayObject damageSprite = new DisplayObject("damage");
//				player.setDamageString("Miss!");
//				player.setDrawDamageString(true);
//			}
		}
		else {
			if(charging) {
				this.setDamageString("Fire Storm!");
				this.setDrawDamageString(true);
				Random rand = new Random();
				int attackDamage = baseAttackDamage + (rand.nextInt(strength) + 1)*3;
				player.takeDamage(attackDamage);
			}
			else {
				charging = true;
				this.setDamageString("Charging!");
				this.setDrawDamageString(true);
				DisplayObject fireball = new DisplayObject("fire", "fireball1.gif");
				fireball.setXPos(this.getXPos());
				fireball.setYPos(this.getYPos());
				fireball.setVisible(true);
				this.addChild(fireball);
				fire.add(fireball);
			}
		}
	}
	
	public void takeDamage(int damage, boolean crit) {
		this.healthbar.setVisible(true);
		double baseDodgePercent = .1;
		double dodgePercent = baseDodgePercent + baseDodgePercent * (agility/100);
		Random rand = new Random();
		int num = rand.nextInt(100)+1;
		if (num > dodgePercent*100) {// Hit
			try {
				game.playSoundEffect("attack");
			} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			health -= damage;
//			System.out.println("Enemy took " + damage + " damage");
			if(crit) {
				this.damageString = "CRIT! " + Integer.toString(damage);
			}
			else {
				this.damageString = Integer.toString(damage);
			}
		}
		else {
//			System.out.println("Enemy dodged"); // dodge
			try {
				game.playSoundEffect("dodge");
			} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.damageString = "Dodge!";
		}
		drawDamageString = true;
		
		if (health <= 0) {
			super.setVisible(false);
			healthbar.setVisible(false);
			game.entitiesMatrix.removeVal(this.coords, this.matrixValue);
			game.dispatchEvent(new EnemyDiedEvent(this));
		}
		else {
			DisplayObject greenBar = healthbar.getChildById("green_bar");
			float percentage = (float)health/(float)maxHealth;
			greenBar.setScaleX(percentage);
			healthbar.removeChild(greenBar);
			healthbar.addChild(greenBar);
		}
	}
	
	@Override
	public void update(ArrayList<String> pressedKeys) {
		super.update(pressedKeys);
		
		if (moving) {
			double xVel = super.getXVelocity();
			double yVel = super.getYVelocity();
			double x = super.getXPos();
			double y = super.getYPos();
			// Check to see if enemy has passed destination and if so, correct it
			if (xVel > 0 && x > this.getDest().x) super.setXPos(this.getDest().x);
			if (xVel < 0 && x < this.getDest().x) super.setXPos(this.getDest().x);
			if (yVel > 0 && y > this.getDest().y) super.setYPos(this.getDest().y);
			if (yVel < 0 && y < this.getDest().y) super.setYPos(this.getDest().y);
			
			// If enemy has reached destination, stop
			if (super.getXPos() == this.getDest().x && super.getYPos() == this.getDest().y) {
				moving = false;
				super.setXVelocity(0);
				super.setYVelocity(0);
				endTurn();
			}
		}
	}
	
	public Point scanSurroundingsFor(int item) {
		int[][] level = game.getLevel();
		int x = this.coords.x;
		int y = this.coords.y;
		for (int i = x - visualField; i <= x + visualField; i++) {
			for (int j = y - visualField; j <= y + visualField; j++) {
				Point p = new Point(i,j);
				if (game.levelGenerator.inBounds(p) && level[i][j] == item) {
					return p;
				}
			}
		}
		return null;
	}
	
	public void endTurn() {
		TurnEndedEvent event = new TurnEndedEvent(this);
		this.dispatchEvent(event);
		
		if (currentPath != null && pathIndex < currentPath.size() - 1 && !waiting) {
			pathIndex++;
		}
		else if (currentPath != null && pathIndex >= currentPath.size() - 1) {
			currentPath = null;
			pathIndex = 0;
		}
	}
	
	public void setCurrentPath(ArrayList<Point> points) {
		if(points == null) {
			currentPath = null;
		}
		else {
			if (!moving && points.size() > 0 && health > 0) {
				pathIndex = 0;
				currentPath = points;
				Point first = points.get(0);
				Point positions = game.coordsToPosition(first.x, first.y);
				this.move(positions.x, positions.y);
			}
			else {
				moving = false;
				pathIndex = 0;
				currentPath = null;
				this.setXVelocity(0);
				this.setYVelocity(0);
			}
		}
	}
	
	int prevMatrixVal = -1;
	/*
	 * Change matrix values after moving from one tile to another
	 */
	public void changeMatrixValues() {
		game.entitiesMatrix.removeVal(coords, matrixValue);
		this.coords = game.positionToCoords(dest.x, dest.y);
		game.entitiesMatrix.addVal(coords, matrixValue);
	}
	
	private boolean drawDamageString = false;
	private String damageString = "";

	@Override
	public void draw(Graphics g) {
		super.draw(g);
		Graphics2D g2d = (Graphics2D) g;
		
		if (drawDamageString) { // Draw damage string if necessary
			drawDamageString = false;
			DisplayObject damageSprite = new DisplayObject("damage");
			
			// Create damage string
			Font f = new Font(Font.MONOSPACED, Font.BOLD, 13);
			BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	        g2d.setFont(f);
	        FontMetrics fm = g2d.getFontMetrics();
	        int width = fm.stringWidth(damageString);
	        int height = fm.getHeight();

	        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

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
	        g2d.setColor(Color.RED);
	        g2d.drawString(damageString, 0, fm.getAscent());
	        g2d.dispose();
			
			// Add sprite in correct location (above player)
			damageSprite.setImage(img);
			damageSprite.setXPos(game.getXPos() + this.getXPos() + this.getUnscaledWidth() / 2 - damageSprite.getUnscaledWidth() / 2);
			damageSprite.setYPos(game.getYPos() + this.getYPos() + -1 * damageSprite.getUnscaledHeight() - 10);
			damageSprite.addEventListener(this, TweenEvent.TWEEN_COMPLETE_EVENT);
			
	        // Tween damage sprite
	        Tween alphaTween = new Tween(damageSprite);
	        String param = TweenableParams.ALPHA;
	        TweenTransitions transition = new TweenTransitions(TweenTransitions.STANDARD);
	        double startVal = 1.0;
	        double endVal = 0.0;
	        double time = 1000;
	        alphaTween.animate(param, transition, startVal, endVal, time);
	        param = TweenableParams.Y_POS;
	        Tween yTween = new Tween(damageSprite);
	        startVal = damageSprite.getYPos();
	        endVal = damageSprite.getYPos() - 50;
	        yTween.animate(param, transition, startVal, endVal, time);
	        game.juggler.addTween(alphaTween);
	        game.juggler.addTween(yTween);
	        
	        DisplayObjectContainer UI = game.getUI();
			UI.addChild(damageSprite);
			game.setFogContainer(UI);
		}
	}
	
	/*
	 * Clears fog from around enemy
	 */
	public void clearFog(int radius) {
		DisplayObject[][] fog = game.getFog();
		int x = this.coords.x;
		int y = this.coords.y;
		
		for (int i = x-radius; i <= x+radius; i++) {
			for (int j = y-radius; j <= y+radius; j++) {
				if (game.levelGenerator.inBounds(new Point(i,j))) {
					fog[i][j].setVisible(false);
				}
			}
		}
		
		game.setFog(fog);
	}

	public Point getCoords() {
		return coords;
	}

	public void setCoords(Point coords) {
		this.coords = coords;
	}
	
	public int getHealth() {
		return health;
	}
	
	public boolean isMoving() {
		return moving;
	}

	public void setMoving(boolean moving) {
		this.moving = moving;
	}
	
	public boolean isDrawDamageString() {
		return drawDamageString;
	}

	public void setDrawDamageString(boolean drawDamageString) {
		this.drawDamageString = drawDamageString;
	}

	public String getDamageString() {
		return damageString;
	}

	public void setDamageString(String damageString) {
		this.damageString = damageString;
	}
	
	public int getLevel() {
		return level;
	}
	// --------- Tween Listener --------- 
	
	public void handleEvent(Event e) {
		// Recycle damage sprites when complete
		if (e.getEventType().equals(TweenEvent.TWEEN_COMPLETE_EVENT)) {
			System.out.println("Handling event");
			TweenEvent tEvent = (TweenEvent) e;
			Tween tween = tEvent.getTween();
			if (tween.getParam().getParam().equals(TweenableParams.ALPHA)) {
				DisplayObject obj = (DisplayObject)e.getSource();
				this.removeChild(obj);
				System.out.println("Child removed");
				System.out.println("Enemy Children: " + this.getChildren().size());
			}
		}
	}

	public Point getDest() {
		return dest;
	}

	public void setDest(Point dest) {
		this.dest = dest;
	}

	public int getPathIndex() {
		return pathIndex;
	}

	public void setPathIndex(int pathIndex) {
		this.pathIndex = pathIndex;
	}

	public int getMatrixValue() {
		return matrixValue;
	}

	public DisplayObjectContainer getHealthbar() {
		return healthbar;
	}

	public void setHealthbar(DisplayObjectContainer healthbar) {
		this.healthbar = healthbar;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = strength;
	}

	public void setMatrixValue(int matrixValue) {
		this.matrixValue = matrixValue;
	}

	public int getAgility() {
		return agility;
	}

	public void setAgility(int agility) {
		this.agility = agility;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}

	public int getBaseAttackDamage() {
		return baseAttackDamage;
	}

	public void setBaseAttackDamage(int baseAttackDamage) {
		this.baseAttackDamage = baseAttackDamage;
	}

	public boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}

	public int getPrevMatrixVal() {
		return prevMatrixVal;
	}

	public void setPrevMatrixVal(int prevMatrixVal) {
		this.prevMatrixVal = prevMatrixVal;
	}

	public ArrayList<Point> getCurrentPath() {
		return currentPath;
	}

	public static double getSpeed() {
		return SPEED;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public void setLevel(int level) {
		this.level = level;
	}
	public Tower getGame() {
		return game;
	}
	
}
