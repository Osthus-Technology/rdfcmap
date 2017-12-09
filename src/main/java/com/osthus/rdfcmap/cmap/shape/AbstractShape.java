package com.osthus.rdfcmap.cmap.shape;

/**
 * AbstractShape
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public abstract class AbstractShape
{
	String name = null;
	String description = null;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
