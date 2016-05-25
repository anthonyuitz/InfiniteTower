package edu.virginia.game;

import edu.virginia.engine.events.Event;
import edu.virginia.engine.events.IEventDispatcher;

public class TurnEndedEvent extends Event {
	public static final String TURN_ENDED_EVENT = "TURN_ENDED_EVENT";
	
	public TurnEndedEvent(IEventDispatcher source) {
		super(source, TURN_ENDED_EVENT);
	}
}
