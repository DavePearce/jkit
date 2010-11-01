// Rotating wireframe cube demo
// The wireframe square rotates in the applet and it appears to be in 3D!
// The wireframe square changes color to demonstrating creating your own colors!
// Written by WolfCoder 1-03-2005
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
public class CubePanel extends JPanel
{
	private double x_verts[] = {-48,48,48,-48}; // Four vertices for a cube
	private double y_verts[] = {48,48,-48,-48};
	private double shape_save[] = {-48,48,48,-48,48,48,-48,-48}; // Original shape before transformation
	private double rotation_pos = 0; // Rotation position
	private double chroma_pos = 64; // Color animation position
	
	private double cube_height;
	
	private File FPS_LOG;
	private File VERTEX_LOG;
	
	private PrintStream fpsout;
	private PrintStream verout;
	
	public CubePanel(int width, int height)
	{
		cube_height = height;
		// Does nothing here
		double[] x_verts = {-width,width,width,-width};
		this.x_verts = x_verts;
		double[] y_verts = {-height,height,height,-height};
		this.y_verts = y_verts;
	}
	private void drawCube(Graphics page, int xpos, int ypos)
	{
		// Reset to original shape
		for(int index = 0;index < 4;index++)
			x_verts[index] = shape_save[index]; // Copy X's
		for(int index = 4;index < 8;index++)
			y_verts[index-4] = shape_save[index]; // Copy Y's
		// Rotate all the points like so...
		double temp_x,temp_y;
		for(int index = 0;index < 4;index++)
		{
			// Rotate the square normally over the y axis in the xyz plane (I think)
			temp_x = x_verts[index]*Math.cos(rotation_pos)-y_verts[index]*Math.sin(rotation_pos);
			temp_y = x_verts[index]*Math.sin(rotation_pos)+y_verts[index]*Math.cos(rotation_pos);
			x_verts[index] = temp_x;
			y_verts[index] = temp_y;
			// And the square rotates to look 3D 2x along the x axis in the xyz plane (I think)
			y_verts[index] = y_verts[index]*Math.sin(rotation_pos/2);
			if(verout != null){
				verout.println(x_verts[index] + " \t" + y_verts[index]);
			}
		}
		// Add a degree of rotation
		rotation_pos += 0.001;
		chroma_pos++;
		if(chroma_pos > 255)
			chroma_pos = 64; // Reset to flash color
		// Draws the cube in question
		// Selects the color
		Color tempcolor = new Color(0,0,255);
		page.setColor(tempcolor);
		for(int index = 0;index < 4;index++)
		{
			if(index < 3)
			{
				page.drawLine((int)x_verts[index]+xpos,(int)(y_verts[index]+ypos-cube_height/2),(int)x_verts[index+1]+xpos,(int)(y_verts[index+1]+ypos-cube_height/2));
			}
			else
			{
				page.drawLine((int)x_verts[index]+xpos,(int)(y_verts[index]+ypos-cube_height/2),(int)x_verts[0]+xpos,(int)(y_verts[0]+ypos-cube_height/2));
			}
		}
		for(int index = 0;index < 4;index++)
		{
			if(index < 3)
			{
				page.drawLine((int)x_verts[index]+xpos,(int)(y_verts[index]+ypos+cube_height/2),(int)x_verts[index+1]+xpos,(int)(y_verts[index+1]+ypos+cube_height/2));
			}
			else
			{
				page.drawLine((int)x_verts[index]+xpos,(int)(y_verts[index]+ypos+cube_height/2),(int)x_verts[0]+xpos,(int)(y_verts[0]+ypos+cube_height/2));
			}
		}
		for(int index = 0;index < 4;index++)
		{
			if(index < 3)
			{
				page.drawLine((int)x_verts[index]+xpos,(int)(y_verts[index]+ypos-cube_height/2),(int)x_verts[index]+xpos,(int)(y_verts[index]+ypos+cube_height/2));
			}
			else
			{
				page.drawLine((int)x_verts[index]+xpos,(int)(y_verts[index]+ypos-cube_height/2),(int)x_verts[index]+xpos,(int)(y_verts[index]+ypos+cube_height/2));
			}
		}
	}
	
	private int currentFrame = 0;
	
	public void paintComponent(Graphics page)
	{
		super.paintComponent(page);
		calc.startTiming();
		// Use a black background
		setBackground(Color.black);
		// Draw the cube
		drawCube(page,128,128);
		float fps = calc.endTiming()/10;
		if(++currentFrame > 20000){
			System.exit(0);
		}
		if(fpsout != null){
			fpsout.println(fps);
		}
		page.setColor(Color.white);
		page.drawString(fps + " FPS", 1, 10);
		repaint();
	}
	
	private CalculationsPerSecond calc = new CalculationsPerSecond();
	
	public void setLogs(String fps, String vertex){
		File f = new File(fps);
		File f2 = new File(vertex);
		try {
			fpsout = new PrintStream(f);
			verout = new PrintStream(f2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(256, 256);
		CubePanel c = new CubePanel(64, 92);
		c.setLogs("FPS.log", "VERTEX.log");
		frame.getContentPane().add(c);
		frame.setResizable(false);
		frame.setVisible(true);
	}
	
}