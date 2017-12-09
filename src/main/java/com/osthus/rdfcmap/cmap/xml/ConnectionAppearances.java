package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "connection-appearance-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConnectionAppearances
{
	private List<ConnectionAppearance> connectionAppearances = null;

	@XmlElement(name = "connection-appearance")
	public List<ConnectionAppearance> getConnectionAppearances()
	{
		return connectionAppearances;
	}

	public void setConnectionAppearances(List<ConnectionAppearance> connectionAppearances)
	{
		this.connectionAppearances = connectionAppearances;
	}
}