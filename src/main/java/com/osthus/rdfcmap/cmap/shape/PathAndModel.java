package com.osthus.rdfcmap.cmap.shape;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import com.osthus.rdfcmap.sparql.PathList;

/**
 * PathAndModel
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class PathAndModel
{
	PathList pathList;
	Model model;
	int counter = 0;
	Resource nodeShape;

	public PathAndModel(PathList pathList, Model model, Resource nodeShape, int counter)
	{
		super();
		this.pathList = pathList;
		this.model = model;
		this.counter = counter;
		this.nodeShape = nodeShape;
	}

	public PathList getPathList()
	{
		return pathList;
	}

	public void setPathList(PathList pathList)
	{
		this.pathList = pathList;
	}

	public int getCounter()
	{
		return counter;
	}

	public void setCounter(int counter)
	{
		this.counter = counter;
	}

	public Resource getNodeShape()
	{
		return nodeShape;
	}

	public void setNodeShape(Resource nodeShape)
	{
		this.nodeShape = nodeShape;
	}
}
