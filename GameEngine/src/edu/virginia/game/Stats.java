package edu.virginia.game;

/*
 * This class can be used to instantiate different types of enemies with Enemy objects
 */
public class Stats {
	public int maxHealth;
	public int baseAttackDamage;
	public int strength;
	public int agility;
	public int level;
	
	public Stats(int level, int maxHealth, int baseAttackDamage, int strength, int agility) {
		double scale = (level-1)/10;
		this.level = level;
		this.maxHealth = maxHealth + (strength * (level-1));
		this.baseAttackDamage = baseAttackDamage + (int)(baseAttackDamage*scale);
		this.strength = strength + (int)(strength*scale);
		this.agility = agility + (int)(agility*scale);
	}
}
