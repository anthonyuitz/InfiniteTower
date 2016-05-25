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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.virginia.engine.display.AnimatedSprite;
import edu.virginia.engine.display.DisplayObject;
import edu.virginia.engine.display.DisplayObjectContainer;
import edu.virginia.engine.display.Tween;
import edu.virginia.engine.display.TweenTransitions;
import edu.virginia.engine.display.TweenableParams;
import edu.virginia.engine.events.Event;
import edu.virginia.engine.events.IEventListener;
import edu.virginia.engine.events.TweenEvent;

public class Player extends AnimatedSprite implements IEventListener {
	private int maxHealth = 100;
	private final int SPEED = 5;
	private int baseAttackDamage = 15;
	
	private Point coords;
	private boolean moving;
	private Point dest;
	private Tower game;
	private ArrayList<Point> currentPath;
	private int health;
	private int pathIndex;
	private int prevMatrixVal;
	
	// Stats
	private int level = 1; // Affects other stats
	private int xp = 0; // 0-10000, 10000 is level up
	private double strength = 10; // Increases damage done
	private double agility = 10; // Increases hit chance, increases chance of enemy miss
	private double luck = 10; // TODO: Implement this
	private double luckGrowth = 0.1;
	private double strGrowth = 0.1;
	private double agiGrowth = 0.1;// Determines how much stats grow per level. see onLevelUp() for usage
	private int visualField = 3; // How far player can see
	private int statPoints; //points player can use to increase stats

	//temporary buffs
	private int invincibility;
	private int luckBuff;
	private int strBuff;
	private int agiBuff;
	
	
	public Player(String id, String imageFileName, Point coords, Tower game) {
		super(id, imageFileName);
		this.setCoords(coords);
		moving = false;
		super.hasPhysics = true;
		this.game = game;
		currentPath = null;
		pathIndex = 0;
		health = maxHealth;
		prevMatrixVal = -1;
		statPoints = 1;
		invincibility = 0;
		luckBuff = 0;
		strBuff = 0;
		agiBuff = 0;
		this.addEventListener(game, TurnEndedEvent.TURN_ENDED_EVENT);
		
		if (game.getFog() != null) clearFog();
	}
	
	public void move(int x, int y) {
		Point coords = game.positionToCoords(x, y);
		boolean enemyInPath = game.entitiesMatrix.containsValInRange(coords, MatrixValues.ENEMY_START, MatrixValues.ENEMY_END);
		if (!moving && health > 0 && !enemyInPath) {
			moving = true;
			dest = new Point(x,y);
			
			double xVel = x - super.getXPos();
			double yVel = y - super.getYPos();
			double mag = Math.sqrt(xVel * xVel + yVel * yVel);
			xVel = xVel * SPEED/mag;
			yVel = yVel * SPEED/mag;
			super.setXVelocity(xVel);
			super.setYVelocity(yVel);
		}
		else if (enemyInPath) {
			currentPath = null;
			pathIndex = 0;
		}
	}
	
	public void attack(Enemy enemy) {
		// Clear fog around enemy
		enemy.clearFog(1);
		Random rand = new Random();

		int attackDamage = baseAttackDamage + rand.nextInt((int)(this.getStr()));

		//critical hit
		if(rand.nextDouble() < 0.1 + (Math.pow(this.getLuck(),0.8) / 100.0)) {
			attackDamage *= 2;
			enemy.takeDamage(attackDamage, true);
		}
		else {
			enemy.takeDamage(attackDamage, false);
		}
		
		if(this.strBuff > 0) {
			this.strBuff--;
		}
		if(this.agiBuff > 0) {
			this.agiBuff--;
		}
		endTurn();
	}
	
	public void takeDamage(int damage) {
		// Cancel movement path
		if (currentPath != null) {
			if (pathIndex > 0) {
				this.moving = false;
				super.setXVelocity(0);
				super.setYVelocity(0);
//				move(currentPath.get(pathIndex-1).x, currentPath.get(pathIndex-1).y);
			}
			this.moving = false;
			currentPath = null;
		}
		pathIndex = 0;
		
		double baseDodgePercent = .1;
		double dodgePercent = baseDodgePercent + baseDodgePercent * (this.getAgi()/100);
		Random rand = new Random();
		int num = rand.nextInt(100)+1;
		if (num > dodgePercent*100) {// Hit
			if(this.invincibility > 0) {
				this.invincibility--;
				this.damageString = "Invulnerable!";
				//play damage blocked sound
			}
			else {
				try {
					game.playSoundEffect("playerdamaged");
				} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				health -= damage;
	//			System.out.println("Player took " + damage + " damage");
				this.damageString = Integer.toString(damage);
			}
		}
		else {
//			System.out.println("Player dodged."); // Dodge
			this.damageString = "Dodge!";
		}
		drawDamageString = true;
		
		// Change healthbar size
		DisplayObjectContainer statusbar = game.getStatusBar();
		DisplayObjectContainer healthbar = (DisplayObjectContainer) statusbar.getChildById("healthbar");
		if (health <= 0) {
			this.setVisible(false);
			healthbar.setVisible(false);
			game.endGame();
		}
		else {
			DisplayObject greenBar = healthbar.getChildById("green_bar");
			float percentage = (float)health/(float)maxHealth;
			greenBar.setScaleX(percentage);
			healthbar.removeChild(greenBar);
			healthbar.addChild(greenBar);
			
		}
		if(this.agiBuff > 0) {
			this.agiBuff--;
		}
		//game.setStatusBar(statusbar);
	}
	
	/*
	 * Returns new growth potential for reincarnation
	 */
	public double getStrReincarnateGrowth(int level) {
		double reincarnateGrowth = (strGrowth - .1)*(.25+1.0*level/100) + .1; // Keep 25% of growth exceeding base percent of 10% 
		return reincarnateGrowth;
	}
	public double getAgiReincarnateGrowth(int level) {
		double reincarnateGrowth = (agiGrowth - .1)*.25*(4.0*level/100) + .1; // Keep 25% of growth exceeding base percent of 10% 
		return reincarnateGrowth;
	}
	
	public void reincarnate(int level) {
		if(level > 0) {
			health = 100;
			maxHealth = 100;
			this.level = 1;
			xp = 0;
			prevMatrixVal = -1;
			statPoints = 1;
			this.agility = 10;
			this.strength = 10;
			this.agiGrowth = this.getAgiReincarnateGrowth(level);
			this.strGrowth = this.getStrReincarnateGrowth(level);
		}
		currentPath = null;
		pathIndex = 0;
		moving = false;
	}
	
	/*
	 * Throws a TurnEndedEvent to let others know the turn has ended
	 */
	public void endTurn() {
		TurnEndedEvent event = new TurnEndedEvent(this);
		this.dispatchEvent(event);
	}
	
	//change size of xp bar
	public void changeXP(double percentage) {
		DisplayObjectContainer statusbar = game.getStatusBar();
		DisplayObjectContainer xpbar = (DisplayObjectContainer) statusbar.getChildById("xpbar");
		DisplayObject purpleBar = xpbar.getChildById("purplebar");
		//scaling by 0 causes errors so avoid doing that
		if(percentage == 0.0) {
			percentage = 0.0000001;
		}
		purpleBar.setScaleX(percentage);
			
	}
		
	public void increaseXP(int amount) {
		xp += amount;
		if (xp >= 10000) { // Level up
			onLevelUp();
		}
		changeXP(xp/10000.0);

	}
	
	private boolean leveledUp = false;
	
	public void onLevelUp() {
		try {
			game.playSoundEffect("lvlup");
		} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
		
		leveledUp = true;
		level++;
		xp = xp % 10000;
		xp = xp / 10;

		// Level up perks
		maxHealth += strength;
		this.health += strength;
		//add increase in max health to current health
	
		//double percent = 1.0*this.health/this.maxHealth;
	//	this.health = (int)(maxHealth*percent+.5);
		
		strength += strGrowth;
		agility += agiGrowth;
		
		// Caculate new growth per level
		//handle  growth stat with user choice
		//double newgrowth = strGrowth + (strGrowth * .1);
		//strGrowth = newgrowth;
		//newgrowth = agiGrowth + (agiGrowth * .1);
		//agiGrowth = newgrowth;
		statPoints += 2;
	}
	
	public void setMaxHealth(int hp) {
		maxHealth = hp;
	}
	
	public ArrayList<Point> getCurrentPath() {
		return currentPath;
	}
	
	public void setCurrentPath(ArrayList<Point> points) {
		if (!moving && points.size() > 0 && health > 0) {
			currentPath = points;
			Point first = points.get(0);
			Point positions = game.coordsToPosition(first.x, first.y);
			this.move(positions.x, positions.y);
		}
	}
	
	/*
	 * Clears fog from around player
	 */
	public void clearFog() {
		DisplayObject[][] fog = game.getFog();
		int level[][] = game.getLevel();
		int x = this.coords.x;
		int y = this.coords.y;
		
		// Check to see if player is in a room, if so, clear all fog from it
		ArrayList<Room> rooms = game.levelGenerator.rooms;
		Room curr = new Room();
		boolean found = false;
		for (Room room : rooms) {
			if (room.contains(x, y)) {
				curr = room;
				found = true;
				break;
			}
		}
		if (found) {
			int roomX = curr.x - 1;
			int roomY = curr.y - 1;
			int roomWidth = curr.width + 1;
			int roomHeight = curr.height + 1;
			
			for (int i = roomX; i <= roomX + roomWidth; i++) {
				for (int j = roomY; j <= roomY + roomHeight; j++) {
					fog[i][j].setVisible(false);
				}
			}
		}
		
		// Clear fog from visual field
		ArrayList<Point> points = getVisibleTilesInRadius();
		for (Point p : points) {
			fog[p.x][p.y].setVisible(false);
		}
		game.setFog(fog);
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
	
	/*
	 * Uses a breadth-first technique to determine the tiles that should be visible to the player
	 * with their visual radius, accounting for walls and other barriers
	 */
	public ArrayList<Point> getVisibleTilesInRadius() {
		Point origin = new Point(this.coords.x, this.coords.y);
		int[][] level = game.getLevel();
		ArrayList<Point> points = new ArrayList<Point>();
		Queue<Point> Q = new LinkedList<Point>();
		Q.add(origin);
		while (!Q.isEmpty()) {
			//System.out.println("Polling queue");
			Point p = Q.poll();
			points.add(p);
			if (MatrixValues.canSeePast(level[p.x][p.y]) || p.equals(origin)) {
				Point n1 = new Point(p.x + 1, p.y);
				Point n2 = new Point(p.x, p.y + 1);
				Point n3 = new Point(p.x - 1, p.y);
				Point n4 = new Point(p.x, p.y - 1);
				Point[] neighbors = { n1, n2, n3, n4 };
				for (int i = 0; i < neighbors.length; i++) {
					Point n = neighbors[i];
					if (!points.contains(n) && game.levelGenerator.inBounds(n) && isWithinVisualField(n.x, n.y)) {
						Q.add(n);
					}
				}
			}
		}
		return points;
	}
	
	public boolean isWithinVisualField(int i, int j) {
		return i >= this.coords.x - visualField && i <= this.coords.x + visualField
				&& j >= this.coords.y - visualField && j <= this.coords.y + visualField;
	}
	
	/*
	 * Change matrix values after moving from one tile to another
	 */
	public void changeMatrixValues() {
		game.entitiesMatrix.removeVal(coords, 0);
		this.coords = game.positionToCoords(dest.x, dest.y);
		game.entitiesMatrix.addVal(coords, 0);
		
		clearFog();
	}
	
	public int getPrevMatrixVal() {
		return prevMatrixVal;
	}
	
	public void setPrevMatrixVal(int i) {
		prevMatrixVal = i;
	}
	
	long lastTimeInMillis = System.nanoTime()/1000000;
	@Override
	public void update(ArrayList<String> pressedKeys) {
		super.update(pressedKeys);
		
		if (moving) {
			double xVel = super.getXVelocity();
			double yVel = super.getYVelocity();
			double x = super.getXPos();
			double y = super.getYPos();
			// Check to see if player has passed destination and if so, correct it
			if (xVel > 0 && x > dest.x) super.setXPos(dest.x);
			if (xVel < 0 && x < dest.x) super.setXPos(dest.x);
			if (yVel > 0 && y > dest.y) super.setYPos(dest.y);
			if (yVel < 0 && y < dest.y) super.setYPos(dest.y);
			
			// If player has reached destination, stop
			if (super.getXPos() == dest.x && super.getYPos() == dest.y) {
				moving = false;
				super.setXVelocity(0);
				super.setYVelocity(0);
				changeMatrixValues();
				endTurn();
				if (currentPath != null && pathIndex < currentPath.size() - 1) {
					pathIndex++;
					Point next = currentPath.get(pathIndex);
					Point positions = game.coordsToPosition(next.x, next.y);
					this.move(positions.x, positions.y);
				}
				else if (currentPath != null && pathIndex >= currentPath.size() - 1) {
					currentPath = null;
					pathIndex = 0;
				}
			}
//			lastTimeInMillis = timeInMillis;
		}
	}
	
	public void drawString(Graphics2D g2d, String s, Color c) {
		int i = (int)(Math.random()*100);
		DisplayObject stringSprite = new DisplayObject("string" + i);
		
		// Create damage string
		Font f = new Font(Font.SANS_SERIF, Font.BOLD, 13);
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(s);
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
        
        g2d.setColor(c);
        g2d.drawString(s, 0, fm.getAscent());
        g2d.dispose();
		
		// Add sprite in correct location (above player)
        stringSprite.setImage(img);
		stringSprite.setXPos(game.getXPos() + this.getXPos() + this.getUnscaledWidth() / 2 - stringSprite.getUnscaledWidth() / 2);
		stringSprite.setYPos(game.getYPos() + this.getYPos() + -1 * stringSprite.getUnscaledHeight() - 10);
		stringSprite.addEventListener(this, TweenEvent.TWEEN_COMPLETE_EVENT);
		
        // Tween damage sprite
        Tween alphaTween = new Tween(stringSprite);
        String param = TweenableParams.ALPHA;
        TweenTransitions transition = new TweenTransitions(TweenTransitions.STANDARD);
        double startVal = 1.0;
        double endVal = 0.0;
        double time = 1000;
        alphaTween.animate(param, transition, startVal, endVal, time);
        param = TweenableParams.Y_POS;
        Tween yTween = new Tween(stringSprite);
        startVal = stringSprite.getYPos();
        endVal = stringSprite.getYPos() - 50;
        yTween.animate(param, transition, startVal, endVal, time);
        game.juggler.addTween(alphaTween);
        game.juggler.addTween(yTween);
        
        DisplayObjectContainer UI = game.getUI();
		UI.addChild(stringSprite);
		game.setFogContainer(UI);
	}
	
	private boolean drawDamageString = false;
	private String damageString = "";
	
	@Override
	public void draw(Graphics g) {
		super.draw(g);
		Graphics2D g2d = (Graphics2D) g;
		
		if (drawDamageString) { // Draw damage string if necessary
			drawDamageString = false;
			Color c;
			
			if (damageString.equals("Dodge!") || damageString.equals("Miss!")) c = Color.GREEN;
	        else c = Color.RED;
			
			drawString(g2d, damageString, c);
			
		}
		
		if(leveledUp) {
			leveledUp = false;
			
			drawString(g2d, "Lvl Up!", Color.YELLOW);
		}
	}

	public Point getCoords() {
		return coords;
	}

	public void setCoords(Point coords) {
		this.coords = coords;
	}

	public boolean isMoving() {
		return moving;
	}

	public void setMoving(boolean moving) {
		this.moving = moving;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
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
	
	public int getMaxHealth() {
		return this.maxHealth;
	}
	
	public int getXP() {
		return this.xp;
	}
	
	//str variable will hold base str. getStr() will return base + bonus from potions
	public double getStr() {
		if(this.strBuff > 0) {
			return 1.5*this.strength;
		}
		else {
			return this.strength;
		}
	}
	public double getAgi() {
		if(this.agiBuff > 0) {
			return 1.5*this.agility;
		}
		else {
			return this.agility;
		}
	}
	public double getStrGrowth() {
		return this.strGrowth;
	}
	public double getAgiGrowth() {
		return this.agiGrowth;
	}
	public int getStatPoints() {
		return this.statPoints;
	}
	
	public void increaseStr() {
		strength += 2;
		statPoints--;
	}
	public void increaseStrGrowth() {
		strGrowth += .2;
		statPoints--;
	}
	public void increaseAgi(){
		agility += 2;
		statPoints--;
	}
	public void increaseAgiGrowth() {
		agiGrowth += .2;
		statPoints--;
	}
	public void addInv(int uses) {
		this.invincibility += uses;
	}
	public void addLuck(int uses) {
		this.luckBuff += uses;
	}
	public void addStr(int uses) {
		this.strBuff += uses;
	}
	public void addAgi(int uses) {
		this.agiBuff += uses;
	}
	public int getLuckBuff() {
		return this.luckBuff;
	}
	public int getStrBuff() {
		return this.strBuff;
	}
	public int getAgiBuff() {
		return this.agiBuff;
	}
	public int getInv() {
		return this.invincibility;
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

	public double getLuck() {
		if(this.getLuckBuff() > 0) {
			return (this.luck*1.5);
		}
		return this.luck;
	}

	public double getLuckGrowth() {
		// TODO Auto-generated method stub
		return this.luckGrowth;
	}

	public void increaseLuck() {
		// TODO Auto-generated method stub
		luck += 2;
		statPoints--;
	}

	public void increaseLuckGrowth() {
		// TODO Auto-generated method stub
		this.luckGrowth += .2;
		statPoints--;
	}

}
