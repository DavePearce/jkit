/*
 * Camera.java
 * Stores the camera characteristics.
 * @(#)Camera.java	1.4 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

//import Point;
//import Vector;

/**
 * class Camera
 */
public class Camera  {
    private Point position;
    private Vector viewDirection;
    private float focalDistance;
    private Vector orthoUp;
    private float verticalFOV;

    /**
     * Camera
     * @param newpos
     * @param newview
     * @param newfdist
     * @param newortho
     * @param newFOV
     */
    public 
    Camera(Point newpos, Vector newview, float newfdist, Vector newortho,
	float newFOV) { 
	position=newpos;
	viewDirection=newview;
	focalDistance=newfdist;
	orthoUp=newortho;
	verticalFOV=newFOV;
	

    }

    /**
     * GetViewDir
     * @return Vector
     */
    public
    Vector GetViewDir() { 

	 return (viewDirection);
    }

    /**
     * GetOrthoUp
     * @return Vector
     */
    public
    Vector GetOrthoUp() { 

	 return (orthoUp);
    }

    /**
     * GetFocalDist
     * @return float
     */
    public
    float GetFocalDist() { 

	 return (focalDistance);
    }

    /**
     * GetFOV
     * @return float
     */
    public
    float GetFOV() { 

	 return (verticalFOV);
    }

    /**
     * GetPosition
     * @return Point
     */
    public
    Point GetPosition() { 

	 return (position);
    }

};
