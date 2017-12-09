package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "extra-graphical-properties-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ExtraProperties
{
	private Properties properties = null;

	@XmlElement(name = "properties-list")
	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
}