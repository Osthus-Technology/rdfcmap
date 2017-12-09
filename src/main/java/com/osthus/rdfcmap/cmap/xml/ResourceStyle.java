package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resource-style")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ResourceStyle
{
	private String fontName;

	private String fontSize;

	private String fontStyle;

	private String fontColor;

	private String backgroundColor;

	@XmlAttribute(name = "font-name")
	public String getFontName()
	{
		return fontName;
	}

	@XmlAttribute(name = "font-size")
	public String getFontSize()
	{
		return fontSize;
	}

	@XmlAttribute(name = "font-style")
	public String getFontStyle()
	{
		return fontStyle;
	}

	@XmlAttribute(name = "font-color")
	public String getFontColor()
	{
		return fontColor;
	}

	@XmlAttribute(name = "background-color")
	public String getBackgroundColor()
	{
		return backgroundColor;
	}

	public void setFontName(String fontName)
	{
		this.fontName = fontName;
	}

	public void setFontSize(String fontSize)
	{
		this.fontSize = fontSize;
	}

	public void setFontStyle(String fontStyle)
	{
		this.fontStyle = fontStyle;
	}

	public void setFontColor(String fontColor)
	{
		this.fontColor = fontColor;
	}

	public void setBackgroundColor(String backgroundColor)
	{
		this.backgroundColor = backgroundColor;
	}
}
