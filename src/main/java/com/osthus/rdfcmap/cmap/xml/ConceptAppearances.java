package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "concept-appearance-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConceptAppearances
{
	private List<ConceptAppearance> conceptAppearances = null;

	@XmlElement(name = "concept-appearance")
	public List<ConceptAppearance> getConceptAppearances()
	{
		return conceptAppearances;
	}

	public void setConceptAppearances(List<ConceptAppearance> conceptAppearances)
	{
		this.conceptAppearances = conceptAppearances;
	}
}