package edu.virginia.game;

import edu.virginia.engine.events.Event;
import edu.virginia.engine.events.IEventDispatcher;

public class EnemyDiedEvent extends Event {
public static final String ENEMY_DIED_EVENT = "ENEMY_DIED_EVENT";
	
	public EnemyDiedEvent(IEventDispatcher source) {
		super(source, ENEMY_DIED_EVENT);
	}
}
