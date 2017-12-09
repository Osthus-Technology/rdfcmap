package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "connection-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Connections
{
	private List<Connection> connections = null;

	@XmlElement(name = "connection")
	public List<Connection> getConnections()
	{
		return connections;
	}

	public void setConnections(List<Connection> connections)
	{
		this.connections = connections;
	}
}