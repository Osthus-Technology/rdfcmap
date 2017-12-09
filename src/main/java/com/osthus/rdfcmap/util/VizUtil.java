package com.osthus.rdfcmap.util;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class VizUtil
{
	public static final String AFV_PREFIX = "http://purl.allotrope.org/ontologies/visualization#";
	public static final Resource AFV_MAP = ResourceFactory.createResource(AFV_PREFIX + "Map");
	public static final Resource AFV_CONCEPT = ResourceFactory.createResource(AFV_PREFIX + "Concept");
	public static final Resource AFV_GRAPHIC_STYLE = ResourceFactory.createResource(AFV_PREFIX + "GraphicStyle");
	public static final Resource AFV_LINK = ResourceFactory.createResource(AFV_PREFIX + "Link");
	public static final Resource AFV_CONNECTION = ResourceFactory.createResource(AFV_PREFIX + "Connection");
	public static final Resource AFV_POINT = ResourceFactory.createResource(AFV_PREFIX + "Point");
	public static final Resource AFV_RESOURCE = ResourceFactory.createResource(AFV_PREFIX + "Resource");
	public static final Resource AFV_FONT = ResourceFactory.createResource(AFV_PREFIX + "Font");
	public static final Resource AFV_HIDDEN_PROPERTY = ResourceFactory.createResource(AFV_PREFIX + "HiddenProperty");
	public static final Resource AFV_BORDER = ResourceFactory.createResource(AFV_PREFIX + "Border");
	public static final Resource AFV_IMAGE = ResourceFactory.createResource(AFV_PREFIX + "Image");
	public static final Resource AFV_EXACT_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "ExactCardinality");
	public static final Resource AFV_MIN_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "MinCardinality");
	public static final Resource AFV_MAX_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "MaxCardinality");
	public static final Resource AFV_INTERVAL_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "IntervalCardinality");
	public static final Resource AFV_MIN_INCLUSIVE_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "MinInclusiveCardinality");
	public static final Resource AFV_MAX_INCLUSIVE_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "MaxInclusiveCardinality");
	public static final Resource AFV_MIN_EXCLUSIVE_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "MinExclusiveCardinality");
	public static final Resource AFV_MAX_EXCLUSIVE_CARDINALITY = ResourceFactory.createResource(AFV_PREFIX + "MaxExclusiveCardinality");
	public static final Resource AFV_MIN_INCLUSIVE_MAX_INCLUSIVE_CARDINALITY = ResourceFactory
			.createResource(AFV_PREFIX + "MinInclusiveMaxInclusiveCardinality");
	public static final Resource AFV_MIN_INCLUSIVE_MAX_EXCLUSIVE_CARDINALITY = ResourceFactory
			.createResource(AFV_PREFIX + "MinInclusiveMaxExclusiveCardinality");
	public static final Resource AFV_MIN_EXCLUSIVE_MAX_INCLUSIVE_CARDINALITY = ResourceFactory
			.createResource(AFV_PREFIX + "MinExclusiveMaxInclusiveCardinality");
	public static final Resource AFV_MIN_EXCLUSIVE_MAX_EXCLUSIVED_CARDINALITY = ResourceFactory
			.createResource(AFV_PREFIX + "MinExclusiveMaxExclusiveCardinality");

	public static final Property AFV_COLOR = ResourceFactory.createProperty(AFV_PREFIX + "color");
	public static final Property AFV_BACKGROUND_COLOR = ResourceFactory.createProperty(AFV_PREFIX + "backgroundColor");
	public static final Property AFV_STYLE = ResourceFactory.createProperty(AFV_PREFIX + "style");
	public static final Property AFV_BYTES = ResourceFactory.createProperty(AFV_PREFIX + "bytes");
	public static final Property AFV_LAYOUT = ResourceFactory.createProperty(AFV_PREFIX + "layout");
	public static final Property AFV_TEXT_MARGIN = ResourceFactory.createProperty(AFV_PREFIX + "textMargin");
	public static final Property AFV_TEXT_ALIGNMENT = ResourceFactory.createProperty(AFV_PREFIX + "textAlignment");
	public static final Property AFV_SHAPE = ResourceFactory.createProperty(AFV_PREFIX + "shape");
	public static final Property AFV_SHAPE_ARC = ResourceFactory.createProperty(AFV_PREFIX + "shapeArc");
	public static final Property AFV_SHADOW_COLOR = ResourceFactory.createProperty(AFV_PREFIX + "shadowColor");
	public static final Property AFV_MINIMUM_WIDTH = ResourceFactory.createProperty(AFV_PREFIX + "minimumWidth");
	public static final Property AFV_MINIMUM_HEIGHT = ResourceFactory.createProperty(AFV_PREFIX + "minimumHeight");
	public static final Property AFV_MAXIMUM_WIDTH = ResourceFactory.createProperty(AFV_PREFIX + "maximumWidth");
	public static final Property AFV_GROUP_CHILD_SPACING = ResourceFactory.createProperty(AFV_PREFIX + "groupChildSpacing");
	public static final Property AFV_GROUP_PARENT_SPACING = ResourceFactory.createProperty(AFV_PREFIX + "groupParentSpacing");
	public static final Property AFV_CONNECTION_TYPE = ResourceFactory.createProperty(AFV_PREFIX + "connectionType");
	public static final Property AFV_LINE_TYPE = ResourceFactory.createProperty(AFV_PREFIX + "lineType");
	public static final Property AFV_ARROW_HEAD = ResourceFactory.createProperty(AFV_PREFIX + "arrowHead");
	public static final Property AFV_WIDTH = ResourceFactory.createProperty(AFV_PREFIX + "width");
	public static final Property AFV_HEIGHT = ResourceFactory.createProperty(AFV_PREFIX + "height");
	public static final Property AFV_SIZE = ResourceFactory.createProperty(AFV_PREFIX + "size");
	public static final Property AFV_THICKNESS = ResourceFactory.createProperty(AFV_PREFIX + "thickness");
	public static final Property AFV_X_POSITION = ResourceFactory.createProperty(AFV_PREFIX + "xPosition");
	public static final Property AFV_Y_POSITION = ResourceFactory.createProperty(AFV_PREFIX + "yPosition");
	public static final Property AFV_ANCHOR_FROM = ResourceFactory.createProperty(AFV_PREFIX + "anchorFrom");
	public static final Property AFV_ANCHOR_TO = ResourceFactory.createProperty(AFV_PREFIX + "anchorTo");
	public static final Property AFV_CONNECTS_FROM = ResourceFactory.createProperty(AFV_PREFIX + "connectsFrom");
	public static final Property AFV_CONNECTS_TO = ResourceFactory.createProperty(AFV_PREFIX + "connectsTo");
	public static final Property AFV_HAS_CONCEPT_STYLE = ResourceFactory.createProperty(AFV_PREFIX + "hasConceptStyle");
	public static final Property AFV_HAS_LINK_STYLE = ResourceFactory.createProperty(AFV_PREFIX + "hasLinkStyle");
	public static final Property AFV_HAS_CONNECTION_STYLE = ResourceFactory.createProperty(AFV_PREFIX + "hasConnectionStyle");
	public static final Property AFV_HAS_RESOURCE_STYLE = ResourceFactory.createProperty(AFV_PREFIX + "hasResourceStyle");
	public static final Property AFV_HAS_FONT = ResourceFactory.createProperty(AFV_PREFIX + "hasFont");
	public static final Property AFV_HAS_BORDER = ResourceFactory.createProperty(AFV_PREFIX + "hasBorder");
	public static final Property AFV_HAS_IMAGE = ResourceFactory.createProperty(AFV_PREFIX + "hasImage");
	public static final Property AFV_HAS_CONTROL_POINT = ResourceFactory.createProperty(AFV_PREFIX + "hasControlPoint");
	public static final Property AFV_HAS_CONNECTION = ResourceFactory.createProperty(AFV_PREFIX + "hasConnection");
	public static final Property AFV_HAS_HIDDEN_PROPERTY = ResourceFactory.createProperty(AFV_PREFIX + "hasHiddenProperty");
	public static final Property AFV_HAS_MAP = ResourceFactory.createProperty(AFV_PREFIX + "hasMap");
	public static final Property AFV_IDENTIFIER = ResourceFactory.createProperty(AFV_PREFIX + "identifier");
	public static final Property AFV_SHORT_COMMENT = ResourceFactory.createProperty(AFV_PREFIX + "shortComment");
	public static final Property AFV_LONG_COMMENT = ResourceFactory.createProperty(AFV_PREFIX + "longComment");
	public static final Property AFV_EXPANDED = ResourceFactory.createProperty(AFV_PREFIX + "expanded");
	public static final Property AFV_IS_BLANK_NODE = ResourceFactory.createProperty(AFV_PREFIX + "isBlankNode");
	public static final Property AFV_IS_LITERAL_NODE = ResourceFactory.createProperty(AFV_PREFIX + "isLiteralNode");
	public static final Property AFV_IS_CLASS = ResourceFactory.createProperty(AFV_PREFIX + "isClass");
	public static final Property AFV_IS_TARGET_NODE = ResourceFactory.createProperty(AFV_PREFIX + "isTargetNode");
	public static final Property AFV_IS_SOURCE_NODE = ResourceFactory.createProperty(AFV_PREFIX + "isSourceNode");

	// property separates instances from named individuals belonging to vocabulary
	public static final Property AFV_IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES = ResourceFactory.createProperty(AFV_PREFIX + "isNamedIndividualOfOntologies");

	public static final Property AFV_HAS_UUID = ResourceFactory.createProperty(AFV_PREFIX + "hasUuid");
	public static final Property AFV_HAS_PARENT_ID = ResourceFactory.createProperty(AFV_PREFIX + "hasParentId");
	public static final Property AFV_HAS_PARENT = ResourceFactory.createProperty(AFV_PREFIX + "hasParent");
	public static final Property AFV_HAS_CARDINALITY = ResourceFactory.createProperty(AFV_PREFIX + "hasCardinality");
	public static final Property AFV_CLASS_HIERARCHY_LEVEL = ResourceFactory.createProperty(AFV_PREFIX + "classHierarchyLevel");

	//@formatter:off
	public static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
			"<cmap xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns=\"http://cmap.ihmc.us/xml/cmap/\" "
			+ "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:vcard=\"http://www.w3.org/2001/vcard-rdf/3.0#\">\r\n" +
			"    <res-meta>\r\n" +
			"        <dc:title>THE_TITLE</dc:title>\r\n" +
			"        <dcterms:created>CREATED_DATE</dcterms:created>\r\n" +
			"        <dcterms:modified>MODIFIED_DATE</dcterms:modified>\r\n" +
			"        <dc:language>en</dc:language>\r\n" +
			"        <dc:format>x-cmap/x-storable</dc:format>\r\n" +
			"        <dc:publisher>PUBLISHER</dc:publisher>\r\n" +
			"        <dc:source>cmap:1QP5DZZTS-1NQ9MJ-1:1QP5F01MN-1D7P3TB-Z:1QPB3LN03-6F7M65-XD7</dc:source>\r\n" +
			"    </res-meta>\r\n" +
			"    <map width=\"MAP_WIDTH\" height=\"MAP_HEIGHT\">\r\n";

	public static final String FOOTER = "<style-sheet-list>\r\n" +
			"            <style-sheet id=\"_Default_\">\r\n" +
			"                <map-style background-color=\"BACKGROUND_COLOR_MAP\" image-style=\"full\" image-top-left=\"0,0\"/>\r\n" +
			"                <concept-style font-name=\"FONT_NAME_CONCEPT\" font-size=\"FONT_SIZE_CONCEPT\" font-style=\"FONT_STYLE_CONCEPT\" "
			+ "font-color=\"FONT_COLOR_CONCEPT\" text-margin=\"TEXT_MARGIN_CONCEPT\" background-color=\"BACKGROUND_COLOR_CONCEPT\" "
			+ "background-image-style=\"full\" border-color=\"BORDER_COLOR_CONCEPT\" border-style=\"BORDER_STYLE_CONCEPT\" "
			+ "border-thickness=\"BORDER_THICKNESS_CONCEPT\"\r\n" +
			"                    border-shape=\"BORDER_SHAPE_CONCEPT\" border-shape-rrarc=\"BORDER_SHAPE_RRARC_CONCEPT\" "
			+ "text-alignment=\"TEXT_ALIGNMENT_CONCEPT\" shadow-color=\"SHADOW_COLOR_CONCEPT\" min-width=\"MIN_WIDTH_CONCEPT\" "
			+ "min-height=\"MIN_HEIGHT_CONCEPT\" max-width=\"MAX_WIDTH_CONCEPT\" group-child-spacing=\"GROUP_CHILD_SPACING_CONCEPT\" "
			+ "group-parent-spacing=\"GROUP_PARENT_SPACING_CONCEPT\"/>\r\n" +
			"                <linking-phrase-style font-name=\"FONT_NAME_LINK\" font-size=\"FONT_SIZE_LINK\" font-style=\"FONT_STYLE_LINK\" "
			+ "font-color=\"FONT_COLOR_LINK\" text-margin=\"TEXT_MARGIN_LINK\" background-color=\"BACKGROUND_COLOR_LINK\" "
			+ "background-image-style=\"full\" border-color=\"BORDER_COLOR_LINK\" border-style=\"BORDER_STYLE_LINK\" "
			+ "border-thickness=\"BORDER_THICKNESS_LINK\" border-shape=\"BORDER_SHAPE_LINK\" border-shape-rrarc=\"BORDER_SHAPE_RRARC_LINK\" "
			+ "text-alignment=\"TEXT_ALIGNMENT_LINK\" shadow-color=\"SHADOW_COLOR_LINK\" min-width=\"MIN_WIDTH_LINK\" min-height=\"MIN_HEIGHT_LINK\" "
			+ "max-width=\"MAX_WIDTH_LINK\" group-child-spacing=\"GROUP_CHILD_SPACING_LINK\" group-parent-spacing=\"GROUP_PARENT_SPACING_LINK\"/>\r\n" +
			"                <connection-style color=\"COLOR_CONNECTION\" style=\"STYLE_CONNECTION\" thickness=\"THICKNESS_CONNECTION\" "
			+ "type=\"TYPE_CONNECTION\" arrowhead=\"ARROW_HEAD_CONNECTION\"/>\r\n" +
			"                <resource-style font-name=\"FONT_NAME_RESOURCE\" font-size=\"FONT_SIZE_RESOURCE\" font-style=\"FONT_STYLE_RESOURCE\" "
			+ "font-color=\"FONT_COLOR_RESOURCE\" background-color=\"BACKGROUND_COLOR_RESOURCE\"/>\r\n" +
			"            </style-sheet>\r\n" +
			"            <style-sheet id=\"_LatestChanges_\">\r\n" +
			"                <concept-style font-style=\"plain\" font-color=\"255,204,51,255\" background-color=\"255,200,0,255\" "
			+ "shadow-color=\"none\"/>\r\n" +
			"                <connection-style arrowhead=\"no\"/>\r\n" +
			"            </style-sheet>\r\n" +
			"        </style-sheet-list>\r\n" +
			"        <extra-graphical-properties-list>\r\n" +
			"            <properties-list id=\"1QPB3LN03-6F7M65-XD7\">\r\n" +
			"                <property key=\"StyleSheetGroup_0\" value=\"//*@!#$%%^&amp;*()() No Grouped StyleSheets @\"/>\r\n" +
			"            </properties-list>\r\n" +
			"        </extra-graphical-properties-list>\r\n" +
			"    </map>\r\n" ;

	public static final String CLOSING_TAG ="</cmap>";
	//@formatter:on
}
