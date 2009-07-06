import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class GraphicalInterface extends JFrame implements WindowListener, MouseListener {
	/**
	 * The following fields cache various icons so we don't need to load them
	 * everytime.
	 */
	private static ImageIcon boardPartition = makeImageIcon("boardPartition.png");
	private static ImageIcon emptySquare = makeImageIcon("water.png");
	private static ImageIcon hitSquare = makeImageIcon("explosion.png");
	private static ImageIcon missSquare = makeImageIcon("missedWater.png");
	private static ImageIcon hShipLeftSquare = makeImageIcon("hShipLeft.png");	
	private static ImageIcon hShipRightSquare = makeImageIcon("hShipRight.png");
	private static ImageIcon hShipMiddleSquare = makeImageIcon("hShipMiddle.png");
	private static ImageIcon vShipBottomSquare = makeImageIcon("vShipBottom.png");	
	private static ImageIcon vShipTopSquare = makeImageIcon("vShipTop.png");
	private static ImageIcon vShipMiddleSquare = makeImageIcon("vShipMiddle.png");
	
	private BattleShipsGame game;
	private JPanel outerMostPanel;	
	private JLabel[][] leftDisplayGrid;
	private JLabel[][] rightDisplayGrid;
	
	public GraphicalInterface() {
		super("The Game of Battleships!");		
		
		game = new BattleShipsGame(15,15);
		game.createRandomBoard(new Random());
		
		// Construct the outermost panel. This will include the grid display, as
		// well as any buttons as needed.
		makeOutermostPanel(15,15);
		getContentPane().add(outerMostPanel);
		drawBoard();
		
		// tell frame to fire a WindowsListener event
		// but not to close when "x" button clicked.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
				
		// Pack the window. This causes the window to compute its size,
		// including the layout of its components.
		pack();
		
		// Finally, make the window visible
		setVisible(true);
	}

	/**
	 * This method is called when the user clicks on the "X" button in the
	 * right-hand corner.
	 * 
	 * @param e
	 */
	public void windowClosing(WindowEvent e) {
		// Ask the user to confirm they wanted to do this
		int r = JOptionPane.showConfirmDialog(this, new JLabel(
		"Exit BattleShips?"), "Confirm Exit",
		JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (r == JOptionPane.YES_OPTION) {
			System.exit(0);
		}
	}

	public void mouseClicked(MouseEvent e) {
		// Ok, mouse has been clicked. We need to decide whether or not it's
		// occurred on one of the grid objects.
		Component c = e.getComponent();
		if(c instanceof JLabel) {
			System.out.println("GOT HERE");
			for(int y=0;y!=game.getHeight();++y) {
				for(int x=0;x!=game.getWidth();++x) {
					JLabel l = rightDisplayGrid[x][y];
					if(l == c) {
						game.bombSquare(x,y,false);
						drawBoard();
					}
				}
			}
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	
	/**
	 * This method is called after the X button has been depressed.
	 * @param e
	 */
    public void windowClosed(WindowEvent e) {}

    // The following methods are part of the WindowListener interface,
    // but are not needed here.
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}

    private ImageIcon getSquareIcon(GridSquare gs, boolean visible) {
    	if(gs instanceof EmptySquare) {
    		return emptySquare;
    	} else if(gs instanceof MissSquare) {
    		return missSquare;
    	} else if(gs instanceof HitSquare) {
    		return hitSquare;
    	} else {
    		if(visible) {
    			ShipSquare ss = (ShipSquare) gs;
    			if(ss.getType() == ShipSquare.Type.HORIZONTAL_LEFT_END) {
    				return hShipLeftSquare;
    			} else if(ss.getType() == ShipSquare.Type.HORIZONTAL_RIGHT_END) {
    				return hShipRightSquare;
    			} else if(ss.getType() == ShipSquare.Type.HORIZONTAL_MIDDLE) {
    				return hShipMiddleSquare;	
    			} else if(ss.getType() == ShipSquare.Type.VERTICAL_BOTTOM_END) {
    				return vShipBottomSquare;
    			} else if(ss.getType() == ShipSquare.Type.VERTICAL_TOP_END) {
    				return vShipTopSquare;
    			} else if(ss.getType() == ShipSquare.Type.VERTICAL_MIDDLE) {
    				return vShipMiddleSquare;	
    			}  
    		}
    		return emptySquare;
    	}
    }
    
    /**
     * This method is used to draw the game board
     */
    private void drawBoard() {
    	// Draw the state of the board
    	for(int y=0;y!=game.getHeight();++y) {
    		for(int x=0;x!=game.getWidth();++x) {
    			leftDisplayGrid[x][y].setIcon(getSquareIcon(game.getLeftSquare(x,y),true));
    			rightDisplayGrid[x][y].setIcon(getSquareIcon(game.getRightSquare(x,y),false));
    		}    		
    	}
    }
    
    /**
     * This method is used to create the outermost panel.
     * @return
     */
    private void makeOutermostPanel(int width, int height) {    	
    	// First, construct the left display grid and its panel
    	leftDisplayGrid = new JLabel[width][height];
    	rightDisplayGrid = new JLabel[width][height];
    	JPanel displayPanel = new JPanel();
    	GridLayout displayPanelLayout = new GridLayout(height,(width*2)+1); 
    	displayPanelLayout.setHgap(1);
    	displayPanelLayout.setVgap(1);
    	displayPanel.setLayout(displayPanelLayout);    	
    	
    	for(int y = 0; y < width; ++y) {
        	for(int x = 0; x < width; ++x) {
        		leftDisplayGrid[x][y] = new JLabel(emptySquare);        		
        		displayPanel.add(leftDisplayGrid[x][y]);
        	}
        	displayPanel.add(new JLabel(boardPartition));
        	for(int x = 0; x < width; ++x) {
        		rightDisplayGrid[x][y] = new JLabel(emptySquare);
        		rightDisplayGrid[x][y].addMouseListener(this);
        		displayPanel.add(rightDisplayGrid[x][y]);
        	}
    	}    	
    	displayPanel.setBorder(new EmptyBorder(15,15,15,15));
    	
    	// Third, construct a simple panel to put buttons on.
    	JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());		
		buttonPanel.add(new JButton("New Game"));
		
    	// Finally, bring the whole thing together.
    	outerMostPanel = new JPanel();
    	outerMostPanel.setLayout(new BorderLayout());
		outerMostPanel.add(buttonPanel,BorderLayout.NORTH);
		outerMostPanel.add(displayPanel,BorderLayout.CENTER);
    }
    
	/**
	 * Helper method for loading image icons.
	 * @param filename
	 * @return
	 */
	private static ImageIcon makeImageIcon(String filename) {		
		// using the URL means the image loads when stored
		// in a jar or expanded into individual files.
		java.net.URL imageURL = BattleShip.class.getResource(filename);

		ImageIcon icon = null;
		if (imageURL != null) {
			icon = new ImageIcon(imageURL);
		}
		return icon;
	}
	
	/**
	 * Main method for the Graphical User Interface
	 */
	public static void main(String[] args) {
		new GraphicalInterface();
	}
	
	
}
