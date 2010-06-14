/*
 * @(#)OctNode.java	1.4 06/17/98
 *
 * OctNode.java
 * Implements an octree node for speeding up the raytracing algorithm. Provides
 * a way of doing space subdivision. Holds pointers to adjacent nodes and the
 * child nodes of the octree node. Holds the linked list of objects in the
 * octree node and the number of objects.
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

//import Face;
//import ObjNode;
//import ObjectType;
//import Point;
//import Ray;
//import Scene;
//import Vector;

/**
 * class OctNode
 */
public class OctNode {
    private OctNode[] Adjacent;
    private Face[] OctFaces;
    private OctNode[] Child;
    private ObjNode ObjList;
    private int NumObj;
    private final static int MaxObj = 15;
    private final static int MaxDepth = 10;
    private final static int MAXX = 0;
    private final static int MAXY = 1;
    private final static int MAXZ = 2;
    private final static int MINX = 3;
    private final static int MINY = 4;
    private final static int MINZ = 5;

    /**
     * Initialize
     */
    private 
    void Initialize() { 

	 int i;

	 Adjacent = new OctNode[6];
	 OctFaces = new Face[6];
	 Child = new OctNode[8];
	 for (i = 0; i < 6; i++) Adjacent[i] = null;
	 for (i = 0; i < 8; i++) Child[i] = null;
	 ObjList = null;
	 NumObj = 0;
    }

    /**
     * CreateFaces
     * @param maxX
     * @param minX
     * @param maxY
     * @param minY
     * @param maxZ
     * @param minZ
     */
    private 
    void CreateFaces(float maxX, float minX, float maxY, float minY,
		float maxZ, float minZ) { 

	 Point temp;

	 for (int i = 0; i < 6; i++) OctFaces[i] = new Face();
	 temp = new Point(maxX, maxY, maxZ);
	 OctFaces[MAXX].SetVert(temp, 0);
	 temp = new Point(maxX, minY, maxZ);
	 OctFaces[MAXX].SetVert(temp, 1);
	 temp = new Point(maxX, minY, minZ);
	 OctFaces[MAXX].SetVert(temp, 2);
	 temp = new Point(maxX, maxY, minZ);
	 OctFaces[MAXX].SetVert(temp, 3);
	 temp = new Point(minX, maxY, maxZ);
	 OctFaces[MAXY].SetVert(temp, 0);
	 temp = new Point(maxX, maxY, maxZ);
	 OctFaces[MAXY].SetVert(temp, 1);
	 temp = new Point(maxX, maxY, minZ);
	 OctFaces[MAXY].SetVert(temp, 2);
	 temp = new Point(minX, maxY, minZ);
	 OctFaces[MAXY].SetVert(temp, 3);
	 temp = new Point(maxX, maxY, maxZ);
	 OctFaces[MAXZ].SetVert(temp, 0);
	 temp = new Point(minX, maxY, maxZ);
	 OctFaces[MAXZ].SetVert(temp, 1);
	 temp = new Point(minX, minY, maxZ);
	 OctFaces[MAXZ].SetVert(temp, 2);
	 temp = new Point(maxX, minY, maxZ);
	 OctFaces[MAXZ].SetVert(temp, 3);
	 temp = new Point(minX, minY, maxZ);
	 OctFaces[MINX].SetVert(temp, 0);
	 temp = new Point(minX, maxY, maxZ);
	 OctFaces[MINX].SetVert(temp, 1);
	 temp = new Point(minX, maxY, minZ);
	 OctFaces[MINX].SetVert(temp, 2);
	 temp = new Point(minX, minY, minZ);
	 OctFaces[MINX].SetVert(temp, 3);
	 temp = new Point(maxX, minY, maxZ);
	 OctFaces[MINY].SetVert(temp, 0);
	 temp = new Point(minX, minY, maxZ);
	 OctFaces[MINY].SetVert(temp, 1);
	 temp = new Point(minX, minY, minZ);
	 OctFaces[MINY].SetVert(temp, 2);
	 temp = new Point(maxX, minY, minZ);
	 OctFaces[MINY].SetVert(temp, 3);
	 temp = new Point(minX, maxY, minZ);
	 OctFaces[MINZ].SetVert(temp, 0);
	 temp = new Point(maxX, maxY, minZ);
	 OctFaces[MINZ].SetVert(temp, 1);
	 temp = new Point(maxX, minY, minZ);
	 OctFaces[MINZ].SetVert(temp, 2);
	 temp = new Point(minX, minY, minZ);
	 OctFaces[MINZ].SetVert(temp, 3);
    }

    /**
     * CreateTree
     * @param objects
     * @param numObjects
     */
    private 
    void CreateTree(ObjNode objects, int numObjects) { 

	 if (objects != null) {
		if (numObjects > MaxObj) CreateChildren(objects, 1);
		else {
		    ObjNode currentObj = objects;
		    while (currentObj != null) {
			ObjNode newnode = new ObjNode(currentObj.GetObj(),
							ObjList);
			ObjList = newnode;
			currentObj = currentObj.Next();
		    }
		    NumObj = numObjects;
		}
	 }
    }

    /**
     * CreateChildren
     * @param objects
     * @param depth
     */
    private 
    void CreateChildren(ObjNode objects, int depth) { 

	 float maxX = OctFaces[MAXX].GetVert(0).GetX();
	 float minX = OctFaces[MINX].GetVert(0).GetX();
	 float maxY = OctFaces[MAXY].GetVert(0).GetY();
	 float minY = OctFaces[MINY].GetVert(0).GetY();
	 float maxZ = OctFaces[MAXZ].GetVert(0).GetZ();
	 float minZ = OctFaces[MINZ].GetVert(0).GetZ();
	 Point midpt = new Point((maxX + minX) / 2.0f, (maxY + minY) / 2.0f,
				(maxZ + minZ) / 2.0f);
	 Point max = new Point();
	 Point min = new Point();
	 ObjNode currentnode;
	 int i;

	 max.Set(maxX, maxY, maxZ);
	 min.Set(midpt.GetX(), midpt.GetY(), midpt.GetZ());
	 Child[0] = new OctNode(max, min);
	 max.Set(maxX, midpt.GetY(), maxZ);
	 min.Set(midpt.GetX(), minY, midpt.GetZ());
	 Child[1] = new OctNode(max, min);
	 max.Set(maxX, midpt.GetY(), midpt.GetZ());
	 min.Set(midpt.GetX(), minY, minZ);
	 Child[2] = new OctNode(max, min);
	 max.Set(maxX, maxY, midpt.GetZ());
	 min.Set(midpt.GetX(), midpt.GetY(), minZ);
	 Child[3] = new OctNode(max, min);
	 max.Set(midpt.GetX(), maxY, maxZ);
	 min.Set(minX, midpt.GetY(), midpt.GetZ());
	 Child[4] = new OctNode(max, min);
	 max.Set(midpt.GetX(), midpt.GetY(), maxZ);
	 min.Set(minX, minY, midpt.GetZ());
	 Child[5] = new OctNode(max, min);
	 max.Set(midpt.GetX(), midpt.GetY(), midpt.GetZ());
	 min.Set(minX, minY, minZ);
	 Child[6] = new OctNode(max, min);
	 max.Set(midpt.GetX(), maxY, midpt.GetZ());
	 min.Set(minX, midpt.GetY(), minZ);
	 Child[7] = new OctNode(max, min);
	 Child[0].FormAdjacent(Adjacent[0], Adjacent[1], Adjacent[2], Child[4],
				   Child[1], Child[3]);
	 Child[1].FormAdjacent(Adjacent[0], Child[0], Adjacent[2], Child[5],
				   Adjacent[4], Child[2]);
	 Child[2].FormAdjacent(Adjacent[0], Child[3], Child[1], Child[6],
				   Adjacent[4], Adjacent[5]);
	 Child[3].FormAdjacent(Adjacent[0], Adjacent[1], Child[0], Child[7],
				   Child[2], Adjacent[5]);
	 Child[4].FormAdjacent(Child[0], Adjacent[1], Adjacent[2], Adjacent[3],
				   Child[5], Child[7]);
	 Child[5].FormAdjacent(Child[1], Child[4], Adjacent[2], Adjacent[3],
				   Adjacent[4], Child[6]);
	 Child[6].FormAdjacent(Child[2], Child[7], Child[5], Adjacent[3],
				   Adjacent[4], Adjacent[5]);
	 Child[7].FormAdjacent(Child[3], Adjacent[1], Child[4], Adjacent[3],
				   Child[6], Adjacent[5]);
	 if (objects != null) currentnode = objects;
	 else currentnode = ObjList;
	 while (currentnode != null) {
		ObjectType currentobj = currentnode.GetObj();
		for (i = 0; i < 8; i++) {
		    max = Child[i].GetFace(0).GetVert(0);
		    min = Child[i].GetFace(5).GetVert(3);
		    if(!((currentobj.GetMin().GetX() > max.GetX()) ||
			 (currentobj.GetMax().GetX() < min.GetX()))) {
			if (!((currentobj.GetMin().GetY() > max.GetY()) ||
			      (currentobj.GetMax().GetY() < min.GetY()))) {
			    if (!((currentobj.GetMin().GetZ() > max.GetZ()) ||
				  (currentobj.GetMax().GetZ() < min.GetZ()))) {
				ObjNode newnode = new ObjNode(currentobj,
							Child[i].GetList());
				Child[i].SetList(newnode);
				Child[i].IncNumObj();
			    }
			}
		    }
		}
		currentnode = currentnode.Next();
	 }
	 if (objects == null) {
		NumObj = 0;
		ObjList = null;
	 }
	 if (depth < MaxDepth) {
		for (i = 0; i < 8; i++) {
		    if (Child[i].GetNumObj() > MaxObj)
			Child[i].CreateChildren(null, depth + 1);
		}
	 }
    }

    /**
     * FormAdjacent
     * @param maxX
     * @param maxY
     * @param maxZ
     * @param minX
     * @param minY
     * @param minZ
     */
    private 
    void FormAdjacent(OctNode maxX, OctNode maxY, OctNode maxZ, OctNode minX,
			OctNode minY, OctNode minZ) { 

	 Adjacent[0] = maxX;
	 Adjacent[1] = maxY;
	 Adjacent[2] = maxZ;
	 Adjacent[3] = minX;
	 Adjacent[4] = minY;
	 Adjacent[5] = minZ;
    }

    /**
     * OctNode
     */
    public
    OctNode() {
	 Initialize();
    }

    /**
     * OctNode
     * @param scene
     * @param numObj
     */
    public 
    OctNode(Scene scene, int numObj) { 

	 Initialize();
	 CreateFaces(scene.GetMaxX(), scene.GetMinX(), scene.GetMaxY(),
			scene.GetMinY(), scene.GetMaxZ(), scene.GetMinZ());
	 CreateTree(scene.GetObjects(), numObj);
    }

    /**
     * OctNode
     * @param max
     * @param min
     */
    public 
    OctNode(Point max, Point min) { 

	 Initialize();
	 CreateFaces(max.GetX(), min.GetX(), max.GetY(), min.GetY(),
			max.GetZ(), min.GetZ());
    }


    /**
     * Copy
     * @param original
     */
    public
    void Copy(OctNode original) {
	 int i;

	 for (i = 0; i < 6; i++) {
		Adjacent[i] = original.GetAdjacent(i);
		OctFaces[i] = original.GetFace(i);
	 }
	 for (i = 0; i < 8; i++) Child[i] = original.GetChild(i);
	 ObjList = original.GetList();
	 NumObj = original.GetNumObj();
    }

    /**
     * GetFace
     * @param index
     * @return Face
     */
    public
    Face GetFace(int index) { 

	 return (OctFaces[index]);
    }

    /**
     * GetList
     * @return ObjNode
     */
    public
    ObjNode GetList() { 

	 return (ObjList);
    }

    /**
     * GetNumObj
     * @return int
     */
    public
    int GetNumObj() { 

	 return (NumObj);
    }

    /**
     * GetAdjacent
     * @param index
     * @return OctNode
     */
    public
    OctNode GetAdjacent(int index) { 

	 return (Adjacent[index]);
    }

    /**
     * GetChild
     * @param index
     */
     public
     OctNode GetChild(int index) {
	 return (Child[index]);
     }

    /**
     * SetList
     * @param newlist
     */
    public
    void SetList(ObjNode newlist) { 

	 ObjList = newlist;
    }

    /**
     * IncNumObj
     */
    public
    void IncNumObj() { 

	 NumObj++;
    }

    /**
     * FindTreeNode
     * @param point
     * @return OctNode
     */
    public 
    OctNode FindTreeNode(Point point) { 

	 OctNode found;

	 if (point.GetX() < OctFaces[MINX].GetVert(0).GetX() || point.GetX() >=
		OctFaces[MAXX].GetVert(0).GetX()) return (null);
	 if (point.GetY() < OctFaces[MINY].GetVert(0).GetY() || point.GetY() >=
		OctFaces[MAXY].GetVert(0).GetY()) return (null);
	 if (point.GetZ() < OctFaces[MINZ].GetVert(0).GetZ() || point.GetZ() >=
		OctFaces[MAXZ].GetVert(0).GetZ()) return (null);
	 if (Child[0] != null) {
		for (int i = 0; i < 8; i++) {
		    found = Child[i].FindTreeNode(point);
		    if (found != null) return (found);
		}
	 }
	 return (this);
    }

    /**
     * Intersect
     * @param ray
     * @param intersect
     * @param Threshold
     * @return OctNode
     */
    public 
    OctNode Intersect(Ray ray, Point intersect, float Threshold) { 

	 Vector delta = new Vector(0.0f, 0.0f, 0.0f);
	 float current = 0.0f;
	 float t;
	 int facehits[] = { -1, -1, -1 };
	 OctNode adjacent = null;

	 if (ray.GetDirection().GetX() != 0.0f) {
		t = -(ray.GetOrigin().GetX() -
			OctFaces[MAXX].GetVert(0).GetX()) /
			ray.GetDirection().GetX();
		if (t > Threshold && t > current) {
		    intersect.Combine(ray.GetOrigin(), ray.GetDirection(),
					1.0f, t);
		    if ((intersect.GetY() <= OctFaces[MAXY].GetVert(0).GetY())
			&& (intersect.GetY() >=
			OctFaces[MINY].GetVert(0).GetY()) && (intersect.GetZ()
			<= OctFaces[MAXZ].GetVert(0).GetZ()) &&
			(intersect.GetZ() >= OctFaces[MINZ].GetVert(0).GetZ()))
		    {
			current = t;
			facehits[0] = MAXX;
			delta.SetX(Threshold);
		    }
		}
		t = -(ray.GetOrigin().GetX() -
			OctFaces[MINX].GetVert(0).GetX()) /
			ray.GetDirection().GetX();
		if (t > Threshold && t > current) {
		    intersect.Combine(ray.GetOrigin(), ray.GetDirection(),
					1.0f, t);
		    if ((intersect.GetY() <= OctFaces[MAXY].GetVert(0).GetY())
			&& (intersect.GetY() >=
			OctFaces[MINY].GetVert(0).GetY()) && (intersect.GetZ()
			<= OctFaces[MAXZ].GetVert(0).GetZ()) &&
			(intersect.GetZ() >= OctFaces[MINZ].GetVert(0).GetZ()))
		    {
			current = t;
			facehits[0] = MINX;
			delta.SetX(-Threshold);
		    }
		}
	 }
	 if (ray.GetDirection().GetY() != 0.0f) {
		t = -(ray.GetOrigin().GetY() -
			OctFaces[MAXY].GetVert(0).GetY()) /
			ray.GetDirection().GetY();
		if (t > Threshold) {
		    if (t > current) {
			intersect.Combine(ray.GetOrigin(), ray.GetDirection(),
						1.0f, t);
			if ((intersect.GetX() <=
				OctFaces[MAXX].GetVert(0).GetX()) &&
				(intersect.GetX() >=
				OctFaces[MINX].GetVert(0).GetX()) &&
				(intersect.GetZ() <=
				OctFaces[MAXZ].GetVert(0).GetZ()) &&
				(intersect.GetZ() >=
				OctFaces[MINZ].GetVert(0).GetZ())) {
			    current = t;
			    facehits[0] = MAXY;
			    delta.Set(0.0f, Threshold, 0.0f);
			}
		    } else if (t == current) {
			facehits[1] = MAXY;
			delta.SetY(Threshold);
		    }
		}
		t = -(ray.GetOrigin().GetY() -
			OctFaces[MINY].GetVert(0).GetY()) /
			ray.GetDirection().GetY();
		if (t > Threshold) {
		    if (t > current) {
			intersect.Combine(ray.GetOrigin(), ray.GetDirection(),
						1.0f, t);
			if ((intersect.GetX() <=
				OctFaces[MAXX].GetVert(0).GetX()) &&
				(intersect.GetX() >=
				OctFaces[MINX].GetVert(0).GetX()) &&
				(intersect.GetZ() <=
				OctFaces[MAXZ].GetVert(0).GetZ()) &&
				(intersect.GetZ() >=
				OctFaces[MINZ].GetVert(0).GetZ())) {
			    current = t;
			    facehits[0] = MINY;
			    delta.Set(0.0f, -Threshold, 0.0f);
			}
		    } else if (t == current) {
			facehits[1] = MINY;
			delta.SetY(-Threshold);
		    }
		}
	 }
	 if (ray.GetDirection().GetZ() != 0.0f) {
		t = -(ray.GetOrigin().GetZ() -
			OctFaces[MAXZ].GetVert(0).GetZ()) /
			ray.GetDirection().GetZ();
		if (t > Threshold) {
		    if (t > current) {
			intersect.Combine(ray.GetOrigin(), ray.GetDirection(),
						1.0f, t);
			if ((intersect.GetX() <=
				OctFaces[MAXX].GetVert(0).GetX()) &&
				(intersect.GetX() >=
				OctFaces[MINX].GetVert(0).GetX()) &&
				(intersect.GetY() <=
				OctFaces[MAXY].GetVert(0).GetY()) &&
				(intersect.GetY() >=
				OctFaces[MINY].GetVert(0).GetY())) {
			    current = t;
			    facehits[0] = MAXZ;
			    delta.Set(0.0f, 0.0f, Threshold);
			}
		    } else if (t == current) {
			if (facehits[1] < 0) facehits[1] = MAXZ;
			else facehits[2] = MAXZ;
			delta.SetZ(Threshold);
		    }
		}
		t = -(ray.GetOrigin().GetZ() -
			OctFaces[MINZ].GetVert(0).GetZ()) /
			ray.GetDirection().GetZ();
		if (t > Threshold) {
		    if (t > current) {
			intersect.Combine(ray.GetOrigin(), ray.GetDirection(),
						1.0f, t);
			if ((intersect.GetX() <=
				OctFaces[MAXX].GetVert(0).GetX()) &&
				(intersect.GetX() >=
				OctFaces[MINX].GetVert(0).GetX()) &&
				(intersect.GetY() <=
				OctFaces[MAXY].GetVert(0).GetY()) &&
				(intersect.GetY() >=
				OctFaces[MINY].GetVert(0).GetY())) {
			    current = t;
			    facehits[0] = MINZ;
			    delta.Set(0.0f, 0.0f, -Threshold);
			}
		    } else if (t == current) {
			if (facehits[1] < 0) facehits[1] = MINZ;
			else facehits[2] = MINZ;
			delta.SetZ(-Threshold);
		    }
		}
	 }
	 if (facehits[0] >= MAXX) {
		intersect.Combine(ray.GetOrigin(), ray.GetDirection(), 1.0f,
					current);
		intersect.Add(delta);
		adjacent = Adjacent[facehits[0]];
		if (facehits[1] >= MAXX) {
		    if (adjacent != null) {
			adjacent = adjacent.GetAdjacent(facehits[1]);
			if (facehits[2] >= MAXX) {
			    if (adjacent != null) {
				adjacent = adjacent.GetAdjacent(facehits[2]);
			    } else adjacent = null;
			}
		    } else adjacent = null;
		}
	 }
	 return (adjacent);
    }

};
