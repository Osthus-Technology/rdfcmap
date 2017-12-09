package com.osthus.rdfcmap.ontology;

import org.apache.jena.rdf.model.Resource;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ClassDescriptionAsShape
{
	private Integer minCount = null;
	private Integer maxCount = null;
	private Resource nodeKind = null;
	private Resource node = null;
	private Resource dataType = null;
	private Resource propertyPath = null;
	private Resource allowedValues = null;
	private String name = null;

	public ClassDescriptionAsShape()
	{
		super();
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

	public Resource getNode()
	{
		return node;
	}

	public void setNode(Resource node)
	{
		this.node = node;
	}

	public Resource getNodeKind()
	{
		return nodeKind;
	}

	public void setNodeKind(Resource nodeKind)
	{
		this.nodeKind = nodeKind;
	}

	public Resource getDataType()
	{
		return dataType;
	}

	public void setDataType(Resource dataType)
	{
		this.dataType = dataType;
	}

	public Resource getPropertyPath()
	{
		return propertyPath;
	}

	public void setPropertyPath(Resource propertyPath)
	{
		this.propertyPath = propertyPath;
	}

	public Resource getAllowedValues()
	{
		return allowedValues;
	}

	public void setAllowedValues(Resource allowedValues)
	{
		this.allowedValues = allowedValues;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}
