package com.osthus.rdfcmap.enums;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public enum MapProperty
{
	THE_TITLE, // dct:title
	CREATED_DATE, // dct:created
	MODIFIED_DATE, // dct:modified
	PUBLISHER, // "OSTHUS" dct:publisher
	MAP_WIDTH, // "5000" afv:width
	MAP_HEIGHT, // "5000" afv:height
	BACKGROUND_COLOR_MAP, // "255,255,255,0" afv:backgroundColor
	FONT_SIZE_CONCEPT, // "12" afv:Font afv:size
	FONT_NAME_CONCEPT, // "Verdana" afv:Font dct:title
	FONT_STYLE_CONCEPT, // "plain" afv:Font afv:style
	FONT_COLOR_CONCEPT, // "0,0,0,255" afv:Font afv:color
	TEXT_MARGIN_CONCEPT, // "4" afv:textMargin
	BACKGROUND_COLOR_CONCEPT, // "237,244,246,255" afv:backgroundColor
	BORDER_COLOR_CONCEPT, // "0,0,0,255" afv:Border afv:color
	BORDER_STYLE_CONCEPT, // "solid" afv:Border afv:style
	BORDER_THICKNESS_CONCEPT, // "1" afv:Border afv:thickness
	BORDER_SHAPE_CONCEPT, // "rounded-rectangle" afv:Border afv:shape
	BORDER_SHAPE_RRARC_CONCEPT, // "15.0" afv:Border afv:shapeArc
	TEXT_ALIGNMENT_CONCEPT, // "center" afv:textAlignment
	SHADOW_COLOR_CONCEPT, // "none" afv:shadowColor
	MIN_WIDTH_CONCEPT, // "-1" afv:minimumWidth
	MIN_HEIGHT_CONCEPT, // "-1" afv:minimumHeight
	MAX_WIDTH_CONCEPT, // "-1.0" afv:maximumWidth
	GROUP_CHILD_SPACING_CONCEPT, // "10" afv:groupChildSpacing
	GROUP_PARENT_SPACING_CONCEPT, // "10" afv:groupParentSpacing
	FONT_SIZE_LINK, // "12" afv:Font afv:size
	FONT_NAME_LINK, // "Verdana" afv:Font dct:title
	FONT_STYLE_LINK, // "plain" afv:Font afv:style
	FONT_COLOR_LINK, // "0,0,0,255" afv:Font afv:color
	TEXT_MARGIN_LINK, // "4"
	BACKGROUND_COLOR_LINK, // "237,244,246,255"
	BORDER_COLOR_LINK, // "0,0,0,0"
	BORDER_STYLE_LINK, // "solid"
	BORDER_THICKNESS_LINK, // "1"
	BORDER_SHAPE_LINK, // "rectangle"
	BORDER_SHAPE_RRARC_LINK, // "15.0"
	TEXT_ALIGNMENT_LINK, // "center"
	SHADOW_COLOR_LINK, // "none"
	MIN_WIDTH_LINK, // "-1"
	MIN_HEIGHT_LINK, // "-1"
	MAX_WIDTH_LINK, // "-1.0"
	GROUP_CHILD_SPACING_LINK, // "10"
	GROUP_PARENT_SPACING_LINK, // "10"
	COLOR_CONNECTION, // "0,0,0,255" afv:Connection afv:color
	STYLE_CONNECTION, // "solid" afv:Connection afv:style
	THICKNESS_CONNECTION, // "1" afv:Connection afv:thickness
	TYPE_CONNECTION, // "straight" afv:Connection afv:connectionType
	ARROW_HEAD_CONNECTION, // "if-to-concept-and-slopes-up" afv:Connection afv:arrowHead
	FONT_SIZE_RESOURCE, // "12"
	FONT_NAME_RESOURCE, // "SanSerif"
	FONT_STYLE_RESOURCE, // "plain"
	FONT_COLOR_RESOURCE, // "0,0,0,255"
	BACKGROUND_COLOR_RESOURCE, // "192,192,192,255"
}
