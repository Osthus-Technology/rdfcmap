package com.osthus.rdfcmap.enums;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public enum ConceptProperty
{
	TITLE, // "label" dct:title (generated)
	SHORT_COMMENT, // "popup" (generated)
	LONG_COMMENT, // "hidden text" (generated)
	X, // 200 afv:xPosition
	Y, // 234 afv:yPosition
	WIDTH, // 200 afv:width
	HEIGHT, // 25 afv:height
	MIN_WIDTH, // 2 afv:minimumWidth
	MIN_HEIGHT, // 2 afv:minimumHeight
	CONNECTS_FROM, // source-id afv:connectsFrom
	CONNECTS_TO, // target-id afv:connectsTo
	ANCHOR_FROM, // "center" afv:anchorFrom
	ANCHOR_TO, // "center" afv:anchorTo
	FONT_STYLE, // "plain" afv:Font afv:style
	BACKGROUND_COLOR, // "255,200,0,255" afv:backgroundColor
	BACKGROUND_IMAGE, // id of afv:Image
	BACKGROUND_IMAGE_STYLE, // "full" afv:Image afv:style
	BACKGROUND_IMAGE_LAYOUT, // "top" afv:Image afv:layout
	BACKGROUND_IMAGE_BYTES, // bytes afv:Image afv:bytes
	BORDER_COLOR, // "255,200,0,255" afv:color
	BORDER_SHAPE, // "oval" afv:border afv:shape
	BORDER_STYLE, // "solid" afv:border afv:shape
	SHADOW_COLOR, // "null" afv:shadowColor
	FONT_SIZE, // "9" afv:Font afv:size
	FONT_COLOR, // "9" afv:Font afv:color
	LINE_TYPE, // "straight" afv:lineType
	ARROW_HEAD, // "no" afv:arrowHead
	CONNECTION_ID, // id of afv:Connection
	INDEX, // counter
	PARENT, // id of parent of nested concept
	EXPANDED, // "true" afv:expanded
	CARDINALITY, // "" afv:Cardinality af-x:minimum value / af-x:maximum value
	IS_BLANK_NODE, // "true" afv:isBlankNode
	IS_LITERAL_NODE, // "true" afv:isLiteralNode
	// required to separate named individual belonging to vocabulary from named individuals belonging to instances
	IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES, // "true" afv:isNamedIndividualOfOntologies
	IS_CLASS, // "true" afv:isClass
	IS_SOURCE_NODE, // "true" afv:isSourceNode
	IS_TARGET_NODE // "true" afv:isTargetNode
}
