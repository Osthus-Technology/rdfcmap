package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "map-style")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class MapStyle
{
	private String backgroundColor;

	private String imageStyle;

	private String imageTopLeft;

	@XmlAttribute(name = "background-color")
	public String getBackgroundColor()
	{
		return backgroundColor;
	}

	@XmlAttribute(name = "image-style")
	public String getImageStyle()
	{
		return imageStyle;
	}

	@XmlAttribute(name = "image-top-left")
	public String getImageTopLeft()
	{
		return imageTopLeft;
	}

	public void setBackgroundColor(String backgroundColor)
	{
		this.backgroundColor = backgroundColor;
	}

	public void setImageStyle(String imageStyle)
	{
		this.imageStyle = imageStyle;
	}

	public void setImageTopLeft(String imageTopLeft)
	{
		this.imageTopLeft = imageTopLeft;
	}
}