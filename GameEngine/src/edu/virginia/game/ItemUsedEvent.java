package edu.virginia.game;

import edu.virginia.engine.events.Event;
import edu.virginia.engine.events.IEventDispatcher;

public class ItemUsedEvent extends Event {
	public static final String ITEM_USED_EVENT = "ITEM_USED_EVENT";
	private int id;
	public ItemUsedEvent(IEventDispatcher source, int id) {
		
		super(source, ITEM_USED_EVENT);
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
}
