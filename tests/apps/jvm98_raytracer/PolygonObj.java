/*
 * @(#)PolygonObj.java	1.3 06/17/98
 *
 * Polygon.java
 * The class for all planar polygons with 4 or more vertices. Does the actual
 * testing of whether a point lies in polygon or not.
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

//import IntersectPt;
//import Material;
//import Point;
//import PolyTypeObj;
//import Ray;

/**
 * class PolygonObj
 */
public class PolygonObj extends PolyTypeObj {
    private int MaxComp;

    /**
     * Check
     * @param ray
     * @param pt
     * @return boolean
     */
    protected
    boolean Check(Ray ray, IntersectPt pt) { 

	 return (InsidePolygon(GetVerts(), GetNumVerts(), pt.GetIntersection(),
				  ray));
    }

    /**
     * InsidePolygon
     * @param verts
     * @param num
     * @param pt
     * @param ray
     * @return boolean
     */
    private 
    boolean InsidePolygon(Point[] verts, int num, Point pt, Ray ray) { 

	 int cross = 0;
	 int xindex, yindex, index = 0;
	 float xtest, ytest, x0, y0, x1, y1;

	 switch (MaxComp) {
	 case 0: xindex = 1; yindex = 2;
		xtest = pt.GetY(); ytest = pt.GetZ(); break;
	 case 1: xindex = 0; yindex = 2;
		xtest = pt.GetX(); ytest = pt.GetZ(); break;
	 default: xindex = 0; yindex = 1;
		xtest = pt.GetX(); ytest = pt.GetY(); break;
	 }
	 x0 = GetCoord(verts[num - 1], xindex) - xtest;
	 y0 = GetCoord(verts[num - 1], yindex) - ytest;
	 while (num-- != 0) {
		x1 = GetCoord(verts[index], xindex) - xtest;
		y1 = GetCoord(verts[index], yindex) - ytest;
		if (y0 > 0.0f) {
		    if (y1 <= 0.0f) {
			if (x1 * y0 > y1 * x0) cross++;
		    }
		} else {
		    if (y1 > 0.0f) {
			if (x0 * y1 > y0 * x1) cross++;
		    }
		}
		x0 = x1;
		y0 = y1;
		index++;
	 }
	 return ((cross & 1) == 1);
    }

    /**
     * GetCoord
     * @param pt
     * @param index
     * @return float
     */
    private 
    float GetCoord(Point pt, int index) { 

	 switch (index) {
	 case 0: return (pt.GetX());
	 case 1: return (pt.GetY());
	 default: return (pt.GetZ());
	 }
    }

    /**
     * PolygonObj
     * @param objmaterial
     * @param newobjID
     * @param numverts
     * @param vertices
     * @param MaxX
     * @param MinX
     * @param MaxY
     * @param MinY
     * @param MaxZ
     * @param MinZ
     */
    public 
    PolygonObj(Material objmaterial, int newobjID, int numverts,
		Point[] vertices, Point max, Point min) {
	 super(objmaterial,newobjID,numverts,vertices,max,min);

	 float x = Math.abs(GetNormal().GetX());
	 float y = Math.abs(GetNormal().GetY());
	 float z = Math.abs(GetNormal().GetZ());
	 if (x >= y && x >= z) MaxComp = 0;
	 else if (y >= z) MaxComp = 1;
	 else MaxComp = 2;
    }

};
