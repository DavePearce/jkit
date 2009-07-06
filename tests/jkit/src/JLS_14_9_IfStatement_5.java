import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class JLS_14_9_IfStatement_5 {	
    private static ImageIcon emptySquare = null;
    private static ImageIcon hShipLeftSquare = null;
    private static ImageIcon hShipRightSquare = null;

    public static class GridSquare {}
    public static class EmptySquare extends GridSquare {}
  
    public static class ShipSquare extends GridSquare {	
	public enum Type {
		HORIZONTAL_LEFT_END,
		HORIZONTAL_RIGHT_END,		
		};

	public Type getType() {
	    return null;
	}
    }
    
    private ImageIcon getSquareIcon(GridSquare gs, boolean visible) {
    	if(gs instanceof EmptySquare) {
    		return emptySquare;
    	} else {
    		if(visible) {
    			ShipSquare ss = (ShipSquare) gs;
    			if(ss.getType() == ShipSquare.Type.HORIZONTAL_LEFT_END) {
    				return hShipLeftSquare;
    			} else if(ss.getType() == ShipSquare.Type.HORIZONTAL_RIGHT_END) {
    				return hShipRightSquare;
    			} 
    		}
    		return emptySquare;
    	}
    }


    public static void main(String[] args) {
	ImageIcon img1 = new JLS_14_9_IfStatement_5().getSquareIcon(new ShipSquare(),true);
	System.out.println(img1);
	ImageIcon img2 = new JLS_14_9_IfStatement_5().getSquareIcon(new EmptySquare(),false);
	System.out.println(img2);
	ImageIcon img3 = new JLS_14_9_IfStatement_5().getSquareIcon(new ShipSquare(),false);
	System.out.println(img3);
    }
}
