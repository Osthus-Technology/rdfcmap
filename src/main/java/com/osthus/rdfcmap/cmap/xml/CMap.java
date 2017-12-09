package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cmap")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CMap
{
	private ResMeta resMeta;

	private Map map;

	private String rdfModel;

	@XmlElement(name = "res-meta")
	public ResMeta getResMeta()
	{
		return resMeta;
	}

	@XmlElement(name = "map")
	public Map getMap()
	{
		return map;
	}

	@XmlElement(name = "rdf-model")
	public String getRdfModel()
	{
		return rdfModel;
	}

	public void setResMeta(ResMeta resMeta)
	{
		this.resMeta = resMeta;
	}

	public void setMap(Map map)
	{
		this.map = map;
	}

	public void setRdfModel(String rdfModel)
	{
		this.rdfModel = rdfModel;
	}
}