/*
 * @(#)Scene.java	1.13 06/17/98
 *
 * Scene.java
 * Contains all the elements of the scene. Has functions to load the scene from
 * a file and render it. Traverses the octree data structure to find the
 * color contributions to each pixel of the various lights and objects in the
 * scene, using reflection and reflection.
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

import java.io.*;


/**
 * class Scene
 */
public class Scene  {
    private static String syncObject = "dummy"; // **NS**
//    private static int numberOfObjects = -1; //**NS**
    private Camera camera;
    private LightNode lights;
    private ObjNode objects;
    private MaterialNode materials;
    private OctNode octree;
    private float MaxX;
    private float MinX;
    private float MaxY;
    private float MinY;
    private float MaxZ;
    private float MinZ;
    private int RayID;
    private final static float AmbLightIntensity = 0.5f;
    private final static float Brightness = 1.0f;
    private final static float MinFactor = 0.1f;
    private final static int MaxLevel = 3;


    /**
     * LoadScene **NS**
     *
     * I tried to make this method load the static data once in to static variables so the MT version
     * could have the data only read once, but this did not work. For some reason the scene defination
     * must be changed when the RT does its picture so as a compromise I get this routine to sync on
     * an object to be sure the MT version does not confuse spec.io.FileInputStream which I suspect
     * might break with several readers at once. **NS**
     *
     * @param filename
     * @return int
     */
    
    private int LoadScene(String filename) {
	synchronized(syncObject) { 
	    return LoadSceneOrig(filename);    
	}
    }
/*         
    private static synchronized int LoadScene(String filename) {
	if (numberOfObjects == -1) {
	    numberOfObjects = LoadSceneOrig(filename);    
System.out.println( numberOfObjects );  
	}
System.out.println( numberOfObjects );	
	return numberOfObjects;
    }
*/  

    /**
     * LoadSceneOrig
     * @param filename
     * @return int
     */
    private
    int LoadSceneOrig(String filename) { 

	 DataInputStream infile;
	 int numObj = 0, ObjID = 0;

	 try {
		infile = new DataInputStream(new BufferedInputStream( new FileInputStream(filename) ));
		
		camera = null;
		lights = null;
		objects = null;
		materials = null;
		MaxX = MinX = MaxY = MinY = MaxZ = MinZ = 0.0f;
		String input;

		input = infile.readLine();
		
		if (!(input.substring(20, 25).equals("ascii"))) {
			System.err.print("Unrecognized file!" + "\n");
			//System.exit(-1);
		}
		while ((input = infile.readLine()) != null) {
			if (input.equals("camera {"))
				ReadCamera(infile);
			else if (input.equals("point_light {"))
				ReadLight(infile);
			else if (input.equals("sphere {"))
				numObj += ReadSphere(infile, ObjID);
			else if (input.equals("poly_set {"))
				numObj += ReadPoly(infile, ObjID);
			else {
			    System.err.print("Unrecognized keyword!" + "\n");
			    //System.exit(-1);
			}
		}
		infile.close();
	 } catch (java.io.FileNotFoundException f) {
		System.err.print("Could not open file: "  +  filename + "\n");
		//System.exit(-1);
	 } catch (java.io.IOException i) {
		System.err.print("IO Error!\n");
		//System.exit(-1);
	 }

	 return (numObj);
    }

    /**
     * ReadCamera
     * @param infile
     */
    private
    void ReadCamera(DataInputStream infile) { 

	 String temp;
	 float[] input = new float[3];
	 int i;

	 try {
		temp = infile.readLine();
		temp = temp.substring(11);
		for (i = 0; i < 2; i++) {
			input[i] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
			temp = temp.substring(temp.indexOf(' ') + 1);
		}
		input[2] = Float.valueOf(temp).floatValue();
		Point position = new Point(input[0], input[1], input[2]);
		MaxX = MinX = input[0];
		MaxY = MinY = input[1];
		MaxZ = MinZ = input[2];
		temp = infile.readLine();
		temp = temp.substring(16);
		for (i = 0; i < 2; i++) {
			input[i] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
			temp = temp.substring(temp.indexOf(' ') + 1);
		}
		input[2] = Float.valueOf(temp).floatValue();
		Vector viewDir = new Vector(input[0], input[1], input[2]);
		temp = infile.readLine();
		temp = temp.substring(16);
		float focalDist = Float.valueOf(temp).floatValue();
		temp = infile.readLine();
		temp = temp.substring(10);
		for (i = 0; i < 2; i++) {
			input[i] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
			temp = temp.substring(temp.indexOf(' ') + 1);
		}
		input[2] = Float.valueOf(temp).floatValue();
		Vector orthoUp = new Vector(input[0], input[1], input[2]);
		temp = infile.readLine();
		temp = temp.substring(14);
		float FOV = Float.valueOf(temp).floatValue();
		temp = infile.readLine();
		camera = new
			Camera(position, viewDir, focalDist, orthoUp, FOV);
	 } catch (java.io.IOException e) {
		System.err.print("IO Error!\n");
		//System.exit(-1);
	 }

    }

    /**
     * ReadLight
     * @param infile
     */
    private  
    void ReadLight(DataInputStream infile) { 

	 String temp;
	 float[] input = new float[3];
	 int i;

	 try {
		temp = infile.readLine();
		temp = temp.substring(11);
		for (i = 0; i < 2; i++) {
			input[i] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
			temp = temp.substring(temp.indexOf(' ') + 1);
		}
		input[2] = Float.valueOf(temp).floatValue();
		Point position = new Point(input[0], input[1], input[2]);
		temp = infile.readLine();
		temp = temp.substring(8);
		for (i = 0; i < 2; i++) {
			input[i] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
			temp = temp.substring(temp.indexOf(' ') + 1);
		}
		input[2] = Float.valueOf(temp).floatValue();
		Color color = new Color(input[0], input[1], input[2]);
		temp = infile.readLine();
		Light newlight = new Light(position, color);
		LightNode newnode = new LightNode(newlight, lights);
		lights = newnode;
	} catch (java.io.IOException e) {
		System.err.print("IO Error!\n");
		//System.exit(-1);
	 }

    }

    /**
     * ReadSphere
     * @param infile
     * @param ObjID
     * @return int
     */
    private  
    int ReadSphere(DataInputStream infile, int ObjID) { 

	 String temp;
	 float[] input = new float[3];
	 int i;
	 float radius;
	 Point max = new Point(MaxX, MaxY, MaxZ);
	 Point min = new Point(MinX, MinY, MinZ);

	 try {
		temp = infile.readLine();
		temp = infile.readLine();
		Material theMaterial = ReadMaterial(infile);
		temp = infile.readLine();
		temp = temp.substring(9);
		for (i = 0; i < 2; i++) {
			input[i] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
			temp = temp.substring(temp.indexOf(' ') + 1);
		}
		input[2] = Float.valueOf(temp).floatValue();
		Point origin = new Point(input[0], input[1], input[2]);
		temp = infile.readLine();
		temp = temp.substring(9);
		radius = Float.valueOf(temp).floatValue();
		for (i = 0; i < 7; i++) temp = infile.readLine();
		SphereObj newsphere = new SphereObj(theMaterial, ++ObjID,
						origin,	radius, max, min);
		ObjNode newnode = new ObjNode(newsphere, objects);
		objects = newnode;
		MaxX = max.GetX();
		MaxY = max.GetY();
		MaxZ = max.GetZ();
		MinX = min.GetX();
		MinY = min.GetY();
		MinZ = min.GetZ();
	 } catch (java.io.IOException e) {
		System.err.print("IO Error!\n");
		//System.exit(-1);
	 }

	 return (1);
    }

    /**
     * ReadPoly
     * @param infile
     * @param ObjID
     * @return int
     */
    private  
    int ReadPoly(DataInputStream infile, int ObjID) { 

	 String temp;
	 float[] input = new float[3];
	 int i, j, k;
	 int numpolys = 0;
	 int numverts;
	 boolean trimesh, vertnormal;
	 Point max = new Point(MaxX, MaxY, MaxZ);
	 Point min = new Point(MinX, MinY, MinZ);

	 try {
		temp = infile.readLine();
		temp = infile.readLine();
		Material theMaterial = ReadMaterial(infile);
		temp = infile.readLine();
		if (temp.substring(7).equals("POLYSET_TRI_MESH"))
			trimesh = true;
		else trimesh = false;
		temp = infile.readLine();
		if (temp.substring(11).equals("PER_VERTEX_NORMAL"))
			vertnormal = true;
		else vertnormal = false;
		for (i = 0; i < 4; i++) temp = infile.readLine();
		temp = temp.substring(11);
		numpolys = Integer.parseInt(temp);
		ObjID++;
		for (i = 0; i < numpolys; i++) {
			temp = infile.readLine();
			temp = infile.readLine();
			temp = temp.substring(16);
			numverts = Integer.parseInt(temp);
			Point[] vertices = new Point[numverts];
			for (j = 0; j < numverts; j++) {
			    temp = infile.readLine();
			    temp = temp.substring(8);
			    for (k = 0; k < 2; k++) {
				input[k] = Float.valueOf(temp.substring(0,
					temp.indexOf(' '))).floatValue();
				temp = temp.substring(temp.indexOf(' ') + 1);
			    }
			    input[2] = Float.valueOf(temp).floatValue();
			    vertices[j] = new Point(input[0], input[1],
							input[2]);
			    if (vertnormal) temp = infile.readLine();
			}
			temp = infile.readLine();
			TriangleObj newtriangle;
			PolygonObj newpoly;
			ObjNode newnode;
			if (trimesh) {
			    newtriangle = new TriangleObj(theMaterial, ObjID,
							numverts, vertices,
							max, min);
			    newnode = new ObjNode(newtriangle, objects);
			} else {
			    newpoly = new PolygonObj(theMaterial, ObjID,
							numverts, vertices,
							max, min);
			    newnode = new ObjNode(newpoly, objects);
			}
			objects = newnode;
		}
		temp = infile.readLine();
		MaxX = max.GetX();
		MaxY = max.GetY();
		MaxZ = max.GetZ();
		MinX = min.GetX();
		MinY = min.GetY();
		MinZ = min.GetZ();
	 } catch (java.io.IOException e) {
		System.err.print("IO Error!\n");
		//System.exit(-1);
	 }

	 return (numpolys);
    }

    /**
     * ReadMaterial
     * @param infile
     * @return Material
     */
    private  
    Material ReadMaterial(DataInputStream infile) { 

	 String temp;
	 float[] input = new float[3];
	 Color[] colors = new Color[4];
	 int i, j;
	 float shininess, ktran;

	 try {
		temp = infile.readLine();
		for (i = 0; i < 4; i++) {
			temp = infile.readLine();
			if (i != 1) temp = temp.substring(14);
			else temp = temp.substring(13);
			for (j = 0; j < 2; j++) {
				input[j] = Float.valueOf(temp.substring(0,
						temp.indexOf(' '))).
						floatValue();
				temp = temp.substring(temp.indexOf(' ') + 1);
			}
			input[2] = Float.valueOf(temp).floatValue();
			colors[i] = new Color(input[0], input[1], input[2]);
		}
		temp = infile.readLine();
		shininess = Float.valueOf(temp.substring(14)).floatValue();
		temp = infile.readLine();
		ktran = Float.valueOf(temp.substring(10)).floatValue();
		temp = infile.readLine();
		Material newmaterial = new Material(colors[0], colors[1],
						colors[2], colors[3],
						shininess, ktran);
		MaterialNode newnode = new MaterialNode(newmaterial,
							materials);
		materials = newnode;
		return (newmaterial);
	 } catch (java.io.IOException e) {
		System.err.print("IO Error!\n");
		//System.exit(-1);
	 }

	 return (null);
    }

    /**
     * Shade
     * @param tree
     * @param eyeRay
     * @param color
     * @param factor
     * @param level
     * @param originID
     */
    private 
    void Shade(OctNode tree, Ray eyeRay, Color color, float factor, int level,
		int originID) { 

	 Color lightColor = new Color(0.0f, 0.0f, 0.0f);
	 Color reflectColor = new Color(0.0f, 0.0f, 0.0f);
	 Color refractColor = new Color(0.0f, 0.0f, 0.0f);
	 IntersectPt intersect = new IntersectPt();
	 OctNode base = new OctNode();
	 Vector normal = new Vector();
	 Ray reflect = new Ray();
	 Ray refract = new Ray();
	 float mu;
	 int current;

	 if (intersect.FindNearestIsect(tree, eyeRay, originID, level, base)) {
		intersect.GetIntersectObj().FindNormal(intersect.
							GetIntersection(),
							normal);
		GetLightColor(base, intersect.GetIntersection(), normal,
			      intersect.GetIntersectObj(), lightColor);
		if (level < MaxLevel) {
		    float check = factor *
			(1.0f - intersect.GetIntersectObj().GetMaterial().
			GetKTran()) * intersect.GetIntersectObj().
			GetMaterial().GetShininess();
		    if (check > MinFactor) {
			reflect.SetOrigin(intersect.GetIntersection());
			reflect.GetDirection().Combine(eyeRay.GetDirection(),
							normal, 1.0f, -2.0f *
							normal.Dot(eyeRay.
							GetDirection()));
			reflect.SetID(RayID++);
			Shade(base, reflect, reflectColor, check, level + 1,
				originID);
			reflectColor.Scale((1.0f - intersect.GetIntersectObj().
					   GetMaterial().GetKTran()) *
					   intersect.GetIntersectObj().
					   GetMaterial().GetShininess(),
					   intersect.GetIntersectObj().
					   GetMaterial().GetSpecColor());
		    }
		    check = factor *
			intersect.GetIntersectObj().GetMaterial().GetKTran();
		    if (check > MinFactor) {
			if (intersect.GetEnter()) {
			    mu = 1.0f / intersect.GetIntersectObj().
				GetMaterial().GetRefIndex();
			    current = intersect.GetIntersectObj().GetObjID();
			} else {
			    mu = intersect.GetIntersectObj().GetMaterial().
				GetRefIndex();
			    normal.Negate();
			    current = 0;
			}
			float IdotN = normal.Dot(eyeRay.GetDirection());
			float TotIntReflect = 1.0f - mu * mu * (1.0f - IdotN *
								IdotN);
			if (TotIntReflect >= 0.0f) {
			    float gamma = -mu * IdotN - (float)
						Math.sqrt(TotIntReflect);
			    refract.SetOrigin(intersect.GetIntersection());
			    refract.GetDirection().Combine(eyeRay.
							GetDirection(),
							normal, mu, gamma);
			    refract.SetID(RayID++);
			    Shade(base, refract, refractColor, check,
					level + 1, current);
			    refractColor.Scale(intersect.GetIntersectObj().
						GetMaterial().GetKTran(),
						intersect.GetIntersectObj().
						GetMaterial().GetSpecColor());
			}
		    }
		}
		color.Combine(intersect.GetIntersectObj().GetMaterial().
			      GetEmissColor(), intersect.GetIntersectObj().
			      GetMaterial().GetAmbColor(), AmbLightIntensity,
			      lightColor, reflectColor, refractColor);
	 }
    }

    /**
     * GetLightColor
     * @param tree
     * @param point
     * @param normal
     * @param currentObj
     * @param color
     */
    private 
    void GetLightColor(OctNode tree, Point point, Vector normal,
			ObjectType currentObj, Color color) { 

	 Ray shadow = new Ray();
	 LightNode current = lights;
	 float maxt;

	 while (current != null) {
		shadow.SetOrigin(point);
		shadow.GetDirection().Sub(current.GetLight().GetPosition(),
						point);
		maxt = shadow.GetDirection().Length();
		shadow.GetDirection().Normalize();
		shadow.SetID(RayID++);
		if (!FindLightBlock(tree, shadow, maxt)) {
		    float factor = Math.max(0.0f, normal.Dot(shadow.
						GetDirection()));
		    if (factor != 0.0f)
			color.Mix(factor, current.GetLight().GetColor(),
				  currentObj.GetMaterial().GetDiffColor());
		}
		current = current.Next();
	 }
    }

    /**
     * FindLightBlock
     * @param tree
     * @param ray
     * @param maxt
     * @return boolean
     */
    private 
    boolean FindLightBlock(OctNode tree, Ray ray, float maxt) { 

	 OctNode current = tree.FindTreeNode(ray.GetOrigin());
	 IntersectPt test = new IntersectPt();
	 Point testpt = new Point();

	 while (current != null) {
		ObjNode currentnode = current.GetList();
		while (currentnode != null) {
		    boolean found = false;
		    if (currentnode.GetObj().GetCachePt().GetID() ==
			ray.GetID()) found = true;
		    if (!found) {
			test.SetOrigID(0);
			if (currentnode.GetObj().Intersect(ray, test)) {
			    if (test.GetT() < maxt) return (true);
			}
		    }
		    currentnode = currentnode.Next();
		}
		OctNode adjacent =
		    current.Intersect(ray, testpt, test.GetThreshold());
		if (adjacent == null) current = null;
		else current = adjacent.FindTreeNode(testpt);
	 }
	 return (false);
    }

    /**
     * Scene
     * @param width
     * @param height
     * @param filename
     */
    public
    Scene(String filename) { 
    
	 int numObj = LoadScene(filename);
	 octree = new OctNode(this, numObj);
    }
    

    /**
     * RenderScene
     */
    public
    void RenderScene( Canvas canvas, int width, int section, int nsections ) {
	Vector view = camera.GetViewDir();
	Vector up = camera.GetOrthoUp();
	Vector plane = new Vector();
	Vector horIncr = new Vector();
	Vector vertIncr = new Vector();
	float ylen = camera.GetFocalDist() *
			(float) Math.tan(0.5f * camera.GetFOV());
	float xlen = ylen * canvas.GetWidth() / canvas.GetHeight();
	Point upleft = new Point();
	Point upright = new Point();
	Point lowleft = new Point();
	Point base = new Point();
	Point current;
	Ray eyeRay = new Ray();
	int ypixel, xpixel;

	RayID = 1;
	plane.Cross(view, up);
	view.Scale(camera.GetFocalDist());
	up.Scale(ylen);
	plane.Scale(-xlen);
	upleft.FindCorner(view, up, plane, camera.GetPosition());
	plane.Negate();
	upright.FindCorner(view, up, plane, camera.GetPosition());
	up.Negate();
	plane.Negate();
	lowleft.FindCorner(view, up, plane, camera.GetPosition());
	horIncr.Sub(upright, upleft);
	horIncr.Scale(horIncr.Length() / ((float) canvas.GetWidth()));
	vertIncr.Sub(lowleft, upleft);
	vertIncr.Scale(vertIncr.Length() / ((float) canvas.GetHeight()));
	base.Set(upleft.GetX() + 0.5f * (horIncr.GetX() + vertIncr.GetX()),
		upleft.GetY() + 0.5f * (horIncr.GetY() + vertIncr.GetY()),
		upleft.GetZ() + 0.5f * (horIncr.GetZ() + vertIncr.GetZ()));
	eyeRay.SetOrigin(camera.GetPosition());

        int xstart = section * width/nsections;
        int xend   = xstart  + width/nsections;

	//spec.harness.Context.out.println( "+"+xstart + " to " + (xend-1) + " by " + canvas.GetHeight()  );	
	
	for (ypixel = 0 ; ypixel < canvas.GetHeight(); ypixel++) {
		current = new Point(base);
		for (xpixel = 0; xpixel < canvas.GetWidth(); xpixel++) {
		    if (xpixel >= xstart && xpixel < xend) {
			Color color = new Color(0.0f, 0.0f, 0.0f);
			eyeRay.GetDirection().Sub(current, eyeRay.GetOrigin());
			eyeRay.GetDirection().Normalize();
			eyeRay.SetID(RayID++);
			Shade(octree, eyeRay, color, 1.0f, 0, 0);
			canvas.Write(Brightness, xpixel, ypixel, color);			
		    }
		    current.Add(horIncr);
		}
		base.Add(vertIncr);
        }
	//spec.harness.Context.out.println( "-"+xstart + " to " + (xend-1) + " by " + canvas.GetHeight() );            
//	}
	
    }

    /**
     * GetObjects
     * @return ObjNode
     */
    public
    ObjNode GetObjects() { 
	return (objects);
    }

    /**
     * GetCanvas
     * @return Canvas
     */
//    public
//    Canvas GetCanvas() { 
//	return (canvas);
//    }

    /**
     * GetMaxX
     * @return float
     */
    public
    float GetMaxX() { 
	return (MaxX);
    }

    /**
     * GetMinX
     * @return float
     */
    public
    float GetMinX() { 
	return (MinX);
    }

    /**
     * GetMaxY
     * @return float
     */
    public
    float GetMaxY() { 
	return (MaxY);
    }

    /**
     * GetMinY
     * @return float
     */
    public
    float GetMinY() { 
	return (MinY);
    }

    /**
     * GetMaxZ
     * @return float
     */
    public
    float GetMaxZ() { 
	return (MaxZ);
    }

    /**
     * GetMinZ
     * @return float
     */
    public
    float GetMinZ() { 
	return (MinZ);
    }

};
