package com.osthus.rdfcmap.cmap.shape;

import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

/**
 * PropertyShape
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class PropertyShape extends AbstractShape
{
	List<Resource> path = null;
	Integer minCount = null;
	Integer maxCount = null;
	Resource dataType = null;
	String pattern = null;
	List<Resource> shapeClass = null;
	Resource nodeKind = null;
	NodeShape node = null;
	Set<Resource> inAllowedValues = null;
	Resource hasValue = null;

	public PropertyShape()
	{
		super();
	}

	public List<Resource> getPath()
	{
		return path;
	}

	public void setPath(List<Resource> path)
	{
		this.path = path;
	}

	public Integer getMinCount()
	{
		return minCount;
	}

	public void setMinCount(Integer minCount)
	{
		this.minCount = minCount;
	}

	public Integer getMaxCount()
	{
		return maxCount;
	}

	public void setMaxCount(Integer maxCount)
	{
		this.maxCount = maxCount;
	}

	public Resource getDataType()
	{
		return dataType;
	}

	public void setDataType(Resource dataType)
	{
		this.dataType = dataType;
	}

	public String getPattern()
	{
		return pattern;
	}

	public void setPattern(String pattern)
	{
		this.pattern = pattern;
	}

	public List<Resource> getShapeClass()
	{
		return shapeClass;
	}

	public void setShapeClass(List<Resource> shapeClass)
	{
		this.shapeClass = shapeClass;
	}

	public Resource getNodeKind()
	{
		return nodeKind;
	}

	public void setNodeKind(Resource nodeKind)
	{
		this.nodeKind = nodeKind;
	}

	public Resource getHasValue()
	{
		return hasValue;
	}

	public void setHasValue(Resource hasValue)
	{
		this.hasValue = hasValue;
	}

	public NodeShape getNode()
	{
		return node;
	}

	public void setNode(NodeShape node)
	{
		this.node = node;
	}

	public Set<Resource> getInAllowedValues()
	{
		return inAllowedValues;
	}

	public void setInAllowedValues(Set<Resource> inAllowedValues)
	{
		this.inAllowedValues = inAllowedValues;
	}
}
