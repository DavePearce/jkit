package battleships;

import java.util.*;

public class BattleShipsGame {
	private GridSquare[][] leftSquares;
	private GridSquare[][] rightSquares;
	private ArrayList<BattleShip> leftShips;
	private ArrayList<BattleShip> rightShips;	
	private int width;
	private int height;
	
	/**
     * Create an instance of the battle ships game where each side has width x
     * height squares.
     * 
     * @param width
     * @param height
     */
	public BattleShipsGame(int width, int height) {
		leftSquares = new GridSquare[width][height];
		rightSquares = new GridSquare[width][height];
		leftShips = new ArrayList<BattleShip>();
		rightShips = new ArrayList<BattleShip>();
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Check whether or not this game is finished!
	 * @return
	 */
	public boolean isOver() {
		return didPlayerWin() || didComputerWin();
	}
	
	/**
	 * Check whether player won or not.
	 * @return
	 */
	public boolean didPlayerWin() {
		for(BattleShip ship : rightShips) {
			if(!ship.isDestroyed()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check whether computer won or not.
	 * @return
	 */
	public boolean didComputerWin() {
		for(BattleShip ship : leftShips) {
			if(!ship.isDestroyed()) {
				return false;
			}
		}
		return true;
	}
	
	/**
     * Bomb a square at a particular location.
     * 
     * @param xpos
     * @param ypos
     * @param leftSide
     *            Indicate whether its on the left (i.e. player's) side, or on
     *            the right (i.e. computer's) side.
     */
	public void bombSquare(int xpos, int ypos, boolean leftSide) {
		GridSquare[][] squares = leftSide ? leftSquares : rightSquares;
		GridSquare square = squares[xpos][ypos];
		
		if(square instanceof EmptySquare) {
			squares[xpos][ypos] = new MissSquare();
		} else if(square instanceof ShipSquare) {
			ShipSquare ss = (ShipSquare) square;
			squares[xpos][ypos] = new HitSquare();
			ss.getShip().isHit();
		}
	}
	
	/**
     * Get a grid square at a given position on the left side of the board.
     * 
     * @param xpos
     * @param ypos
     * @return
     */
	public GridSquare getLeftSquare(int xpos, int ypos) {
		return leftSquares[xpos][ypos];
	}

	/**
     * Get a grid square at a given position on the right side of the board.
     * 
     * @param xpos
     * @param ypos
     * @return
     */
	public GridSquare getRightSquare(int xpos, int ypos) {
		return rightSquares[xpos][ypos];
	}
	
	/**
	 * Get width of one side of the board.
	 * @return
	 */
	public int getWidth() { return width; }
		
	/**
	 * Get height of one side of the board.
	 * @return
	 */
	public int getHeight() { return height; }
	

	/**
	 * Construct a randomly generate board
	 * @param game
	 */
	public void createRandomBoard(Random random) {
		// To start with, initialise all squares to be empty
		for(int y=0;y!=height;++y) {
			for(int x=0;x!=width;++x) {
				leftSquares[x][y] = new EmptySquare();
				rightSquares[x][y] = new EmptySquare();
			}
		}
		
		// Now add ships to left side
		
		addRandomShip(new BattleShip("Submarine",3),true,random);
		addRandomShip(new BattleShip("Submarine",3),true,random);
		addRandomShip(new BattleShip("Submarine",3),true,random);
		addRandomShip(new BattleShip("Corvette",4),true,random);
		addRandomShip(new BattleShip("Carrier",5),true,random);
		
		// Now add ships to right side
		addRandomShip(new BattleShip("Submarine",3),false,random);
		addRandomShip(new BattleShip("Submarine",3),false,random);
		addRandomShip(new BattleShip("Submarine",3),false,random);
		addRandomShip(new BattleShip("Corvette",4),false,random);
		addRandomShip(new BattleShip("Carrier",5),false,random);		
	}
	
	/**
     * Add a random ship of a given length to one side of the board. The new
     * ship cannot intersect any existing ships.
     * 
     * @param length
     * @param squares
     * @param random
     */
	private void addRandomShip(BattleShip ship, boolean leftSide, Random random) {
		boolean vertical = random.nextBoolean();
		int length = ship.getLength();
		GridSquare[][] squares = leftSide ? leftSquares : rightSquares;
		if(leftSide) {
			leftShips.add(ship);
		} else {
			rightShips.add(ship);
		}
		
		while(true) {
			int xpos = random.nextInt(vertical ? width : width-length);
			int ypos = random.nextInt(vertical ? height - length : height);
			// now, check whether this ship intersects any others
			if(vertical) {
				for(int i=0;i!=length;++i) {
					if(!(squares[xpos][ypos+i] instanceof EmptySquare)) {
						continue;
					}
				}

				// if we get here, then there is no intersection.
				
				for(int i=0;i!=length;++i) {
					if(i==0) {
						squares[xpos][ypos+i] = new ShipSquare(ShipSquare.Type.VERTICAL_TOP_END,ship);
					} else if(i == (length-1)) {
						squares[xpos][ypos+i] = new ShipSquare(ShipSquare.Type.VERTICAL_BOTTOM_END,ship);
					} else {
						squares[xpos][ypos+i] = new ShipSquare(ShipSquare.Type.VERTICAL_MIDDLE,ship);
					}
				}
				
				// done
				return;
			} else {
				for(int i=0;i!=length;++i) {
					if(!(squares[xpos+i][ypos] instanceof EmptySquare)) {
						continue;
					}
				}

				// if we get here, then there is no intersection.
				
				for(int i=0;i!=length;++i) {
					if(i==0) {
						squares[xpos+i][ypos] = new ShipSquare(ShipSquare.Type.HORIZONTAL_LEFT_END,ship);
					} else if(i == (length-1)) {
						squares[xpos+i][ypos] = new ShipSquare(ShipSquare.Type.HORIZONTAL_RIGHT_END,ship);
					} else {
						squares[xpos+i][ypos] = new ShipSquare(ShipSquare.Type.HORIZONTAL_MIDDLE,ship);
					}
				}
				
				// done
				return;
			}
		}
	}	
}
