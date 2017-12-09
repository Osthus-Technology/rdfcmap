package com.osthus.rdfcmap.enums;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Property;

import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class PropertyEnums
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, Property> enum2Property = new HashMap()
	{
		private static final long serialVersionUID = 4079928218691037228L;

		{
			put(MapProperty.THE_TITLE.name(), AFOUtil.DCT_TITLE);
			put(MapProperty.CREATED_DATE.name(), AFOUtil.DCT_CREATED);
			put(MapProperty.MODIFIED_DATE.name(), AFOUtil.DCT_MODIFIED);
			put(MapProperty.PUBLISHER.name(), AFOUtil.DCT_PUBLISHER);
			put(MapProperty.MAP_WIDTH.name(), VizUtil.AFV_WIDTH);
			put(MapProperty.MAP_HEIGHT.name(), VizUtil.AFV_HEIGHT);
		}
	};

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, Property> enum2StyleProperty = new HashMap()
	{
		private static final long serialVersionUID = 1384747351012850071L;

		{
			put(MapProperty.FONT_SIZE_CONCEPT.name(), VizUtil.AFV_SIZE);
			put(MapProperty.FONT_NAME_CONCEPT.name(), AFOUtil.DCT_TITLE);
			put(MapProperty.FONT_STYLE_CONCEPT.name(), VizUtil.AFV_STYLE);
			put(MapProperty.FONT_COLOR_CONCEPT.name(), VizUtil.AFV_COLOR);
			put(MapProperty.TEXT_MARGIN_CONCEPT.name(), VizUtil.AFV_TEXT_MARGIN);
			put(MapProperty.BACKGROUND_COLOR_CONCEPT.name(), VizUtil.AFV_BACKGROUND_COLOR);
			put(MapProperty.BORDER_COLOR_CONCEPT.name(), VizUtil.AFV_COLOR);
			put(MapProperty.BORDER_STYLE_CONCEPT.name(), VizUtil.AFV_STYLE);
			put(MapProperty.BORDER_THICKNESS_CONCEPT.name(), VizUtil.AFV_THICKNESS);
			put(MapProperty.BORDER_SHAPE_CONCEPT.name(), VizUtil.AFV_SHAPE);
			put(MapProperty.BORDER_SHAPE_RRARC_CONCEPT.name(), VizUtil.AFV_SHAPE_ARC);
			put(MapProperty.TEXT_ALIGNMENT_CONCEPT.name(), VizUtil.AFV_TEXT_ALIGNMENT);
			put(MapProperty.SHADOW_COLOR_CONCEPT.name(), VizUtil.AFV_SHADOW_COLOR);
			put(MapProperty.MIN_WIDTH_CONCEPT.name(), VizUtil.AFV_MINIMUM_WIDTH);
			put(MapProperty.MIN_HEIGHT_CONCEPT.name(), VizUtil.AFV_MINIMUM_HEIGHT);
			put(MapProperty.MAX_WIDTH_CONCEPT.name(), VizUtil.AFV_MAXIMUM_WIDTH);
			put(MapProperty.GROUP_CHILD_SPACING_CONCEPT.name(), VizUtil.AFV_GROUP_CHILD_SPACING);
			put(MapProperty.GROUP_PARENT_SPACING_CONCEPT.name(), VizUtil.AFV_GROUP_PARENT_SPACING);

			put(MapProperty.FONT_SIZE_LINK.name(), VizUtil.AFV_SIZE);
			put(MapProperty.FONT_NAME_LINK.name(), AFOUtil.DCT_TITLE);
			put(MapProperty.FONT_STYLE_LINK.name(), VizUtil.AFV_STYLE);
			put(MapProperty.FONT_COLOR_LINK.name(), VizUtil.AFV_COLOR);
			put(MapProperty.TEXT_MARGIN_LINK.name(), VizUtil.AFV_TEXT_MARGIN);
			put(MapProperty.BACKGROUND_COLOR_LINK.name(), VizUtil.AFV_BACKGROUND_COLOR);
			put(MapProperty.BORDER_COLOR_LINK.name(), VizUtil.AFV_COLOR);
			put(MapProperty.BORDER_STYLE_LINK.name(), VizUtil.AFV_STYLE);
			put(MapProperty.BORDER_THICKNESS_LINK.name(), VizUtil.AFV_THICKNESS);
			put(MapProperty.BORDER_SHAPE_LINK.name(), VizUtil.AFV_SHAPE);
			put(MapProperty.BORDER_SHAPE_RRARC_LINK.name(), VizUtil.AFV_SHAPE_ARC);
			put(MapProperty.TEXT_ALIGNMENT_LINK.name(), VizUtil.AFV_TEXT_ALIGNMENT);
			put(MapProperty.SHADOW_COLOR_LINK.name(), VizUtil.AFV_SHADOW_COLOR);
			put(MapProperty.MIN_WIDTH_LINK.name(), VizUtil.AFV_MINIMUM_WIDTH);
			put(MapProperty.MIN_HEIGHT_LINK.name(), VizUtil.AFV_MINIMUM_HEIGHT);
			put(MapProperty.MAX_WIDTH_LINK.name(), VizUtil.AFV_MAXIMUM_WIDTH);
			put(MapProperty.GROUP_CHILD_SPACING_LINK.name(), VizUtil.AFV_GROUP_CHILD_SPACING);
			put(MapProperty.GROUP_PARENT_SPACING_LINK.name(), VizUtil.AFV_GROUP_PARENT_SPACING);

			put(MapProperty.COLOR_CONNECTION.name(), VizUtil.AFV_COLOR);
			put(MapProperty.STYLE_CONNECTION.name(), VizUtil.AFV_STYLE);
			put(MapProperty.THICKNESS_CONNECTION.name(), VizUtil.AFV_THICKNESS);
			put(MapProperty.TYPE_CONNECTION.name(), VizUtil.AFV_CONNECTION_TYPE);
			put(MapProperty.ARROW_HEAD_CONNECTION.name(), VizUtil.AFV_ARROW_HEAD);

			put(MapProperty.FONT_SIZE_RESOURCE.name(), VizUtil.AFV_SIZE);
			put(MapProperty.FONT_NAME_RESOURCE.name(), AFOUtil.DCT_TITLE);
			put(MapProperty.FONT_STYLE_RESOURCE.name(), VizUtil.AFV_STYLE);
			put(MapProperty.FONT_COLOR_RESOURCE.name(), VizUtil.AFV_COLOR);
			put(MapProperty.BACKGROUND_COLOR_RESOURCE.name(), VizUtil.AFV_BACKGROUND_COLOR);
		}
	};
}
