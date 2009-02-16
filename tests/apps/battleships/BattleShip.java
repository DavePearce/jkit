package battleships;

/**
 * This class represents a battle ship!
 * @author djp
 *
 */
public class BattleShip {
	private String name;
	private int length;
	private int nhits; // number of hits so far
	
	/**
	 * Construct new battle ship.
	 * @param name Name of battle ship
	 * @param length Length of battle ship (in squares)
	 */
	public BattleShip(String name, int length) {
		this.name = name;
		this.length = length;
		this.nhits = 0; // no hits so far
	}
	
	/**
	 * Signal that this battle ship has been hit.	 
	 */
	public void isHit() { nhits++; }
	
	/**
	 * Check whether or not this battle ship has been destroyed 
	 */
	public boolean isDestroyed() { return nhits == length; }
	
	/**
	 * Check the length of this ship
	 * @return
	 */
	public int getLength() { return length; }
}
