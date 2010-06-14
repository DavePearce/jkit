/*
 * @(#)Color.java	1.5 06/17/98
 *
 * Color.java
 * The class which holds color values. Provides operations for mixing and
 * combining colors.
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

/**
 * class Color
 */
public class Color  {
    private float red;
    private float green;
    private float blue;

    /**
     * Color
     */
    public
    Color() {
	red=0.0f;
	green=0.0f;
	blue=0.0f;
	

    }

    /**
     * Color
     * @param redComp
     * @param greenComp
     * @param blueComp
     */
    public
    Color(float redComp, float greenComp, float blueComp) { 
	red=redComp;
	green=greenComp;
	blue=blueComp;
	

    }

    /**
     * Color
     * @param newcolor
     */
    public
    Color(Color newcolor) { 

	 red = newcolor.GetRed();
	 green = newcolor.GetGreen();
	 blue = newcolor.GetBlue();
    }

    /**
     * Scale
     * @param factor
     */
    public
    void Scale(float factor) { 

	 red *= factor;
	 green *= factor;
	 blue *= factor;
    }

    /**
     * Scale
     * @param factor
     * @param color
     */
    public
    void Scale(float factor, Color color) { 

	 red *= factor * color.GetRed();
	 green *= factor * color.GetGreen();
	 blue *= factor * color.GetBlue();
    }

    /**
     * FindMax
     * @return float
     */
    public
    float FindMax() { 

	 if (red >= green && red >= blue) return (red);
	 if (green >= blue) return (green);
	 return (blue);
    }

    /**
     * Set
     * @param redComp
     * @param greenComp
     * @param blueComp
     */
    public
    void Set(float redComp, float greenComp, float blueComp) { 

	 red = redComp;
	 green = greenComp;
	 blue = blueComp;
    }

    /**
     * GetRed
     * @return float
     */
    public
    float GetRed() { 

	 return (red);
    }

    /**
     * GetGreen
     * @return float
     */
    public
    float GetGreen() { 

	 return (green);
    }

    /**
     * GetBlue
     * @return float
     */
    public
    float GetBlue() { 

	 return (blue);
    }

    /**
     * Mix
     * @param factor
     * @param color1
     * @param color2
     */
    public
    void Mix(float factor, Color color1, Color color2) { 

	 red += factor * color1.GetRed() * color2.GetRed();
	 green += factor * color1.GetGreen() * color2.GetGreen();
	 blue += factor * color1.GetBlue() * color2.GetBlue();
    }

    /**
     * Combine
     * @param color1
     * @param color2
     * @param color2factor
     * @param color3
     * @param color4
     * @param color5
     */
    public
    void Combine(Color color1, Color color2, float color2factor, Color color3,
	Color color4, Color color5) { 

	 red = color1.GetRed() + color2.GetRed() * color2factor +
		color3.GetRed()	+ color4.GetRed() + color5.GetRed();
	 green = color1.GetGreen() + color2.GetGreen() * color2factor +
		color3.GetGreen() + color4.GetGreen() + color5.GetGreen();
	 blue = color1.GetBlue() + color2.GetBlue() * color2factor +
		color3.GetBlue() + color4.GetBlue() + color5.GetBlue();
    }

    /**
     * hashcode. Very dangerous because this object is not immutable, but okay for BM validation. **NS**
     */
    int hashcode() {
	return new Float(red).hashCode() ^ new Float(green).hashCode() ^ new Float(blue).hashCode();
    }

};
