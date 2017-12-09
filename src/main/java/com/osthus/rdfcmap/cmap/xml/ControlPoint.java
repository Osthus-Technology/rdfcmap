package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ControlPoint
{
	private String x;

	private String y;

	@XmlAttribute(name = "x")
	public String getX()
	{
		return x;
	}

	@XmlAttribute(name = "y")
	public String getY()
	{
		return y;
	}

	public void setX(String x)
	{
		this.x = x;
	}

	public void setY(String y)
	{
		this.y = y;
	}
}