package edu.virginia.game;

import edu.virginia.engine.display.Sprite;

public class Item extends Sprite{
	
	public Item(String id, String imageFileName, int type) {
		super(id, imageFileName);
		this.id = type;
	}

	//id of item determines its effect
	private int id;
	
	public void setID(int id) {
		this.id = id;
	}
	
	public int getID() {
		return this.id;
	}
	//get an item object using the item number
	public static Item getItem(int itemNum) {
		if(itemNum == MatrixValues.ITEMS_START) {
			return new Item("pot1", "pot.png", itemNum);
		}
		else if(itemNum == MatrixValues.ITEMS_START+1) {
			return new Item("invpot1", "invpot.png", itemNum);
		}
		else if(itemNum == MatrixValues.ITEMS_START+2) {
			return new Item("luckpot1", "luckpot.png", itemNum);
		}
		else if(itemNum == MatrixValues.ITEMS_START+3) {
			return new Item("strpot1", "strpot.png", itemNum);
		}
		else if(itemNum == MatrixValues.ITEMS_START+4) {
			return new Item("agipot1", "agipot.png", itemNum);
		}
		return new Item("pot1", "pot.png", itemNum);

	}
//generate a random item
	public static int getItem() {
		double itemRoll = Math.random();
		//generate random items
		//make hp potions more common
		if(itemRoll < .5) {
			return MatrixValues.ITEMS_START;
		}
		else if(itemRoll < .625) {
			return MatrixValues.ITEMS_START+1;
		}
		else if(itemRoll < .75) {
			return MatrixValues.ITEMS_START+2;
		}
		else if(itemRoll < .875) {
			return MatrixValues.ITEMS_START+3;
		}
		else {
			return MatrixValues.ITEMS_START+4;
		}
	}
}
