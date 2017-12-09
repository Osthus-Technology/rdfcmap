package com.osthus.rdfcmap.cmap.cardinality;

/**
 * StatementWithCardinality
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class StatementWithCardinality
{
	private String subject;
	private String property;
	private String object;
	private String cardinality;
	private boolean mapped = false;

	public StatementWithCardinality(String subject, String property, String object, String cardinality)
	{
		super();
		this.subject = subject;
		this.property = property;
		this.object = object;
		this.cardinality = cardinality;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}

	public String getObject()
	{
		return object;
	}

	public void setObject(String object)
	{
		this.object = object;
	}

	public String getCardinality()
	{
		return cardinality;
	}

	public void setCardinality(String cardinality)
	{
		this.cardinality = cardinality;
	}

	public boolean isMapped()
	{
		return mapped;
	}

	public void setMapped(boolean mapped)
	{
		this.mapped = mapped;
	}

}
