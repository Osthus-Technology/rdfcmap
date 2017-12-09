package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "connection")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Connection
{
	private String id;
	private String fromId;
	private String toId;

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

	@XmlAttribute(name = "from-id")
	public String getFromId()
	{
		return fromId;
	}

	@XmlAttribute(name = "to-id")
	public String getToId()
	{
		return toId;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setFromId(String fromId)
	{
		this.fromId = fromId;
	}

	public void setToId(String toId)
	{
		this.toId = toId;
	}
}