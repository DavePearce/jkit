/*
 * @(#)MaterialNode.java	1.4 06/17/98
 *
 * MaterialNode.java
 * A linked list node of materials.
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

//import LinkNode;
//import Material;

/**
 * class MaterialNode
 */
public class MaterialNode extends LinkNode {
    private Material theMaterial;

    /**
     * MaterialNode
     */
    public
    MaterialNode() {
	super(null);
	theMaterial=null;

    }

    /**
     * MaterialNode
     * @param newMaterial
     * @param nextlink
     */
    public
    MaterialNode(Material newMaterial, LinkNode nextlink) {
	super(nextlink);
	theMaterial=newMaterial;

    }

    /**
     * Next
     * @return MaterialNode
     */
    public
    MaterialNode Next() { 

	 return ((MaterialNode) GetNext());
    }

    /**
     * GetMaterial
     * @return Material
     */
    public
    Material GetMaterial() { 

	 return (theMaterial);
    }

};
