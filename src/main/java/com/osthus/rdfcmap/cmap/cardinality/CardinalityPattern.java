package com.osthus.rdfcmap.cmap.cardinality;

import java.util.HashMap;
import java.util.Map;

/**
 * CardinalityPattern
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CardinalityPattern
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, String> cardinality2pattern = new HashMap()
	{
		private static final long serialVersionUID = 5275721024215574083L;

		{
			put(CardinalityEnum.MIN_0.name(), "");
			put(CardinalityEnum.EXACTLY.name(), "^([0-9]+)$");
			put(CardinalityEnum.MAX.name(), "^<([0-9]+)$");
			put(CardinalityEnum.MAX_EXCL.name(), "^<([0-9]+)\\[$");
			put(CardinalityEnum.MAX_INCL.name(), "^<([0-9]+)\\]$");
			put(CardinalityEnum.MIN.name(), "^>([0-9]+)$");
			put(CardinalityEnum.MIN_EXCL.name(), "^>([0-9]+)\\[$");
			put(CardinalityEnum.MIN_EXCL_MAX_EXCL.name(), "^\\]([0-9]+)[\\s\\,]+([0-9]+)\\[$");
			put(CardinalityEnum.MIN_EXCL_MAX_INCL.name(), "^\\]([0-9]+)[\\s\\,]+([0-9]+)\\]$");
			put(CardinalityEnum.MIN_INCL.name(), "^>([0-9]+)\\]$");
			put(CardinalityEnum.MIN_INCL_MAX_EXCL.name(), "^\\[([0-9]+)[\\s\\,]+([0-9]+)\\[$");
			put(CardinalityEnum.MIN_INCL_MAX_INCL.name(), "^\\[([0-9]+)[\\s\\,]+([0-9]+)\\]$");
			put(CardinalityEnum.MIN_MAX.name(), "^(([0-9]+)[\\s\\,]+([0-9]+))$");
		}
	};
}
