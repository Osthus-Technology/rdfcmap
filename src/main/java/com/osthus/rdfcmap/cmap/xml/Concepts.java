package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "concept-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Concepts
{
	private List<Concept> conceptList = null;

	@XmlElement(name = "concept")
	public List<Concept> getConceptList()
	{
		return conceptList;
	}

	public void setConceptList(List<Concept> concepts)
	{
		conceptList = concepts;
	}
}