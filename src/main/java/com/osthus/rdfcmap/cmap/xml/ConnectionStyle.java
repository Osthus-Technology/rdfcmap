package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "connection-style")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConnectionStyle
{
	private String color;

	private String style;

	private String thickness;

	private String type;

	private String arrowhead;

	@XmlAttribute(name = "color")
	public String getColor()
	{
		return color;
	}

	@XmlAttribute(name = "style")
	public String getStyle()
	{
		return style;
	}

	@XmlAttribute(name = "thickness")
	public String getThickness()
	{
		return thickness;
	}

	@XmlAttribute(name = "type")
	public String getType()
	{
		return type;
	}

	@XmlAttribute(name = "arrowhead")
	public String getArrowhead()
	{
		return arrowhead;
	}

	public void setColor(String color)
	{
		this.color = color;
	}

	public void setStyle(String style)
	{
		this.style = style;
	}

	public void setThickness(String thickness)
	{
		this.thickness = thickness;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public void setArrowhead(String arrowhead)
	{
		this.arrowhead = arrowhead;
	}
}
