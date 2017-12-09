package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "concept-style")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConceptStyle
{
	private String fontName;

	private String fontSize;

	private String fontStyle;

	private String fontColor;

	private String textMargin;

	private String backgroundColor;

	private String backgroundImageStyle;

	private String borderColor;

	private String borderStyle;

	private String borderThickness;

	private String borderShape;

	private String borderShapeRrarc;

	private String textAlignment;

	private String shadowColor;

	private String minWidth;

	private String minHeight;

	private String maxWidth;

	private String groupChildSpacing;

	private String groupParentSpacing;

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

	@XmlAttribute(name = "text-margin")
	public String getTextMargin()
	{
		return textMargin;
	}

	@XmlAttribute(name = "background-color")
	public String getBackgroundColor()
	{
		return backgroundColor;
	}

	@XmlAttribute(name = "background-image-style")
	public String getBackgroundImageStyle()
	{
		return backgroundImageStyle;
	}

	@XmlAttribute(name = "border-color")
	public String getBorderColor()
	{
		return borderColor;
	}

	@XmlAttribute(name = "border-style")
	public String getBorderStyle()
	{
		return borderStyle;
	}

	@XmlAttribute(name = "border-thickness")
	public String getBorderThickness()
	{
		return borderThickness;
	}

	@XmlAttribute(name = "border-shape")
	public String getBorderShape()
	{
		return borderShape;
	}

	@XmlAttribute(name = "border-shape-rrarc")
	public String getBorderShapeRrarc()
	{
		return borderShapeRrarc;
	}

	@XmlAttribute(name = "text-alignment")
	public String getTextAlignment()
	{
		return textAlignment;
	}

	@XmlAttribute(name = "shadow-color")
	public String getShadowColor()
	{
		return shadowColor;
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

	@XmlAttribute(name = "max-width")
	public String getMaxWidth()
	{
		return maxWidth;
	}

	@XmlAttribute(name = "group-child-spacing")
	public String getGroupChildSpacing()
	{
		return groupChildSpacing;
	}

	@XmlAttribute(name = "group-parent-spacing")
	public String getGroupParentSpacing()
	{
		return groupParentSpacing;
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

	public void setTextMargin(String textMargin)
	{
		this.textMargin = textMargin;
	}

	public void setBackgroundColor(String backgroundColor)
	{
		this.backgroundColor = backgroundColor;
	}

	public void setBackgroundImageStyle(String backgroundImageStyle)
	{
		this.backgroundImageStyle = backgroundImageStyle;
	}

	public void setBorderColor(String borderColor)
	{
		this.borderColor = borderColor;
	}

	public void setBorderStyle(String borderStyle)
	{
		this.borderStyle = borderStyle;
	}

	public void setBorderThickness(String borderThickness)
	{
		this.borderThickness = borderThickness;
	}

	public void setBorderShape(String borderShape)
	{
		this.borderShape = borderShape;
	}

	public void setBorderShapeRrarc(String borderShapeRrarc)
	{
		this.borderShapeRrarc = borderShapeRrarc;
	}

	public void setTextAlignment(String textAlignment)
	{
		this.textAlignment = textAlignment;
	}

	public void setShadowColor(String shadowColor)
	{
		this.shadowColor = shadowColor;
	}

	public void setMinWidth(String minWidth)
	{
		this.minWidth = minWidth;
	}

	public void setMinHeight(String minHeight)
	{
		this.minHeight = minHeight;
	}

	public void setMaxWidth(String maxWidth)
	{
		this.maxWidth = maxWidth;
	}

	public void setGroupChildSpacing(String groupChildSpacing)
	{
		this.groupChildSpacing = groupChildSpacing;
	}

	public void setGroupParentSpacing(String groupParentSpacing)
	{
		this.groupParentSpacing = groupParentSpacing;
	}
}
