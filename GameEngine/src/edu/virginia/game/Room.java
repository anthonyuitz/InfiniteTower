package edu.virginia.game;

import java.awt.Point;
import java.awt.Rectangle;

public class Room {
	public int x;
	public int y;
	public int width;
	public int height;
	
	public int centerX; //random point in the room which is used when connecting the rooms together
	public int centerY; //random point in the room which is used when connecting the rooms together
	
	
	public Room() {
		this.x = -1;
		this.y = -1;
		this.width = 0;
		this.height = 0;
		this.centerX = 0;
		this.centerY = 0;
	}
	
	public Room(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		if(x <= 1 && y <= 1) {
			if(Math.random() < .5) {
				this.centerX = x+width - 1;
				this.centerY = (int)(Math.random()*height) + y;
			}
			else {
				this.centerX = (int)(Math.random()*width) + x;
				this.centerY = y+height - 1;
			}
		}
		else if(x <= 1 && y > 1) {
			this.centerX = x+width - 1;
			this.centerY = (int)(Math.random()*height) + y;
		}
		else if(x > 0 && y <= 1) {
			this.centerX = (int)(Math.random()*width) + x;
			this.centerY = y+height - 1;
		}
		else {
			if(Math.random() < .5) {
				this.centerX = x;
				this.centerY = (int)(Math.random()*height) + y;
			}
			else {
				this.centerX = (int)(Math.random()*width) + x;
				this.centerY = y;
			}
		}
		
	}
	
	public void randomizeCenter() {
		this.centerX = (int)(Math.random()*width) + x;
		this.centerY = (int)(Math.random()*height) + y;
	}
	
	public Point getCenter() {
		return new Point(this.centerX, this.centerY);
	}
	
	public void matrixSurroundedWithWalls() {
		this.x++;
		this.y++;
		this.centerX++;
		this.centerY++;
	}
	
	public boolean overlaps(Room r) {
		Rectangle c = new Rectangle(this.x-3, this.y-3, this.width+6, this.height+6);
		Rectangle d = new Rectangle(r.x, r.y, r.width, r.height);
		return c.intersects(d);
	}
	
	@Override
	public String toString() {
		return "("+this.x+"," +this.y+")"+ " w/ wid: " + this.width + " & hgt: " + this.height;
	}
	
	public boolean contains(int x, int y) {
		return x >= this.x - 1 && x <= this.x + this.width && y >= this.y - 1 && y <= this.y + this.height;
	}
	
}
