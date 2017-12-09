package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "image")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Image
{
	private String id;
	private String bytes;

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

	@XmlAttribute(name = "bytes")
	public String getBytes()
	{
		return bytes;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setBytes(String bytes)
	{
		this.bytes = bytes;
	}
}