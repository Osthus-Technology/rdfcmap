package com.osthus.rdfcmap.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ColorScheme
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, String> domain2scheme = new HashMap()
	{
		private static final long serialVersionUID = 4079928218691037228L;

		{
			put(DomainEnum.COMMON.name(), "255,255,255,255");
			put(DomainEnum.MATERIAL.name(), "194,231,194,255");
			put(DomainEnum.PROCESS.name(), "202,163,241,255");
			put(DomainEnum.RESULT.name(), "255,204,0,255");
			put(DomainEnum.INFORMATION.name(), "255,204,0,255");
			put(DomainEnum.EQUIPMENT.name(), "170,213,255,255");
			put(DomainEnum.PROPERTY.name(), "255,255,255,255");
			put(DomainEnum.QUALITY.name(), "255,255,153,255");
			put(DomainEnum.CONTEXTUAL_ROLE.name(), "255,200,150,255");
			put(DomainEnum.ROLE.name(), "255,153,204,255");
			put(DomainEnum.OTHER.name(), "255,255,255,255");
		}
	};
}
