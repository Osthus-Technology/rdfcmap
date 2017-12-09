package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "properties-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Properties
{
	private List<Property> properties = null;

	private String id;

	@XmlElement(name = "property")
	public List<Property> getProperties()
	{
		return properties;
	}

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setProperties(List<Property> properties)
	{
		this.properties = properties;
	}
}