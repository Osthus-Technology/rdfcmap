package com.osthus.rdfcmap.cmap.shape;

import java.util.List;

import org.apache.jena.rdf.model.Resource;

/**
 * NodeShape
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class NodeShape extends AbstractShape
{
	List<Resource> targetClass = null;
	List<Resource> targetNode = null;
	List<NodeShape> andShapes = null;
	List<NodeShape> orShapes = null;
	List<PropertyShape> property = null;
	Boolean isClosed = null;
	List<Resource> ignoredProperties = null;
	String title = null;

	public NodeShape()
	{
		super();
	}

	public List<Resource> getTargetClass()
	{
		return targetClass;
	}

	public void setTargetClass(List<Resource> targetClass)
	{
		this.targetClass = targetClass;
	}

	public List<Resource> getTargetNode()
	{
		return targetNode;
	}

	public void setTargetNode(List<Resource> targetNode)
	{
		this.targetNode = targetNode;
	}

	public List<NodeShape> getAndShapes()
	{
		return andShapes;
	}

	public void setAndShapes(List<NodeShape> andShapes)
	{
		this.andShapes = andShapes;
	}

	public List<NodeShape> getOrShapes()
	{
		return orShapes;
	}

	public void setOrShapes(List<NodeShape> orShapes)
	{
		this.orShapes = orShapes;
	}

	public List<PropertyShape> getProperty()
	{
		return property;
	}

	public void setProperty(List<PropertyShape> property)
	{
		this.property = property;
	}

	public Boolean getIsClosed()
	{
		return isClosed;
	}

	public void setIsClosed(Boolean isClosed)
	{
		this.isClosed = isClosed;
	}

	public List<Resource> getIgnoredProperties()
	{
		return ignoredProperties;
	}

	public void setIgnoredProperties(List<Resource> ignoredProperties)
	{
		this.ignoredProperties = ignoredProperties;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
}
