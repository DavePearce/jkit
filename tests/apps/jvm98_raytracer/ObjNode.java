/*
 * @(#)ObjNode.java	1.4 06/17/98
 *
 * ObjNode.java
 * A linked list node of objects.
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

//import LinkNode;
//import ObjectType;

/**
 * class ObjNode
 */
public class ObjNode extends LinkNode {
    private ObjectType theObject;

    /**
     * ObjNode
     */
    public
    ObjNode() {
	super(null);
	theObject=null;

    }

    /**
     * ObjNode
     * @param newObj
     * @param nextlink
     */
    public
    ObjNode(ObjectType newObj, LinkNode nextlink) {
	super(nextlink);
	theObject=newObj;

    }

    /**
     * Next
     * @return ObjNode
     */
    public
    ObjNode Next() { 

	 return ((ObjNode) GetNext());
    }

    /**
     * GetObj
     * @return ObjectType
     */
    public
    ObjectType GetObj() { 

	 return (theObject);
    }

};
