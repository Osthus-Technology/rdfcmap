package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "linking-phrase-appearance")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class LinkingPhraseAppearance
{
	private String id;
	private String x;
	private String y;
	private String width;
	private String height;
	private String minWidth;
	private String minHeight;
	private String fontSize;
	private String fontColor;
	private String backgroundColor;
	private String backgroundImage;
	private String backgroundImageStyle;
	private String backgroundImageLayout;
	private String borderColor;
	private String shadowColor;

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

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

	@XmlAttribute(name = "width")
	public String getWidth()
	{
		return width;
	}

	@XmlAttribute(name = "height")
	public String getHeight()
	{
		return height;
	}

	@XmlAttribute(name = "min-width")
	public String getMinWidth()
	{
		return minWidth;
	}

	@XmlAttribute(name = "min-height")
	public String getMinHeight()
	{
		return minHeight;
	}

	@XmlAttribute(name = "border-color")
	public String getBorderColor()
	{
		return borderColor;
	}

	@XmlAttribute(name = "background-color")
	public String getBackgroundColor()
	{
		return backgroundColor;
	}

	@XmlAttribute(name = "background-image")
	public String getBackgroundImage()
	{
		return backgroundImage;
	}

	@XmlAttribute(name = "background-image-style")
	public String getBackgroundImageStyle()
	{
		return backgroundImageStyle;
	}

	@XmlAttribute(name = "background-image-layout")
	public String getBackgroundImageLayout()
	{
		return backgroundImageLayout;
	}

	@XmlAttribute(name = "shadow-color")
	public String getShadowColor()
	{
		return shadowColor;
	}

	@XmlAttribute(name = "font-size")
	public String getFontSize()
	{
		return fontSize;
	}

	@XmlAttribute(name = "font-color")
	public String getFontColor()
	{
		return fontSize;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setX(String x)
	{
		this.x = x;
	}

	public void setY(String y)
	{
		this.y = y;
	}

	public void setWidth(String width)
	{
		this.width = width;
	}

	public void setHeight(String height)
	{
		this.height = height;
	}

	public void setMinWidth(String minWidth)
	{
		this.minWidth = minWidth;
	}

	public void setMinHeight(String minHeight)
	{
		this.minHeight = minHeight;
	}

	public void setBorderColor(String borderColor)
	{
		this.borderColor = borderColor;
	}

	public void setBackgroundColor(String backgroundColor)
	{
		this.backgroundColor = backgroundColor;
	}

	public void setBackgroundImage(String backgroundImage)
	{
		this.backgroundImage = backgroundImage;
	}

	public void setBackgroundImageStyle(String backgroundImageStyle)
	{
		this.backgroundImageStyle = backgroundImageStyle;
	}

	public void setBackgroundImageLayout(String backgroundImageLayout)
	{
		this.backgroundImageLayout = backgroundImageLayout;
	}

	public void setShadowColor(String shadowColor)
	{
		this.shadowColor = shadowColor;
	}

	public void setFontSize(String fontSize)
	{
		this.fontSize = fontSize;
	}

	public void setFontColor(String fontColor)
	{
		fontSize = fontColor;
	}
}