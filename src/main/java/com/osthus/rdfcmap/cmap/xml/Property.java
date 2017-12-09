package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "property")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Property
{
	private String key;
	private String value;

	@XmlAttribute(name = "key")
	public String getKey()
	{
		return key;
	}

	@XmlAttribute(name = "value")
	public String getValue()
	{
		return value;
	}

	public void setKey(String key)
	{
		this.key = key;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}