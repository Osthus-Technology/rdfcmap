package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "concept-appearance")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConceptAppearance
{
	private String id;

	private String x;

	private String y;

	private String width;

	private String height;

	private String fontStyle;

	private String fontSize;

	private String backgroundColor;

	private String backgroundImage;

	private String backgroundImageStyle;

	private String backgroundImageLayout;

	private String borderShape;

	private String borderStyle;

	private String expanded;

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

	@XmlAttribute(name = "font-style")
	public String getFontStyle()
	{
		return fontStyle;
	}

	@XmlAttribute(name = "font-size")
	public String getFontSize()
	{
		return fontSize;
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

	@XmlAttribute(name = "border-shape")
	public String getBorderShape()
	{
		return borderShape;
	}

	@XmlAttribute(name = "border-style")
	public String getBorderStyle()
	{
		return borderStyle;
	}

	@XmlAttribute(name = "expanded")
	public String getExpanded()
	{
		return expanded;
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

	public void setBorderShape(String borderShape)
	{
		this.borderShape = borderShape;
	}

	public void setBorderStyle(String borderStyle)
	{
		this.borderStyle = borderStyle;
	}

	public void setExpanded(String expanded)
	{
		this.expanded = expanded;
	}

	public void setHeight(String height)
	{
		this.height = height;
	}

	public void setFontStyle(String fontStyle)
	{
		this.fontStyle = fontStyle;
	}

	public void setFontSize(String fontSize)
	{
		this.fontSize = fontSize;
	}
}
