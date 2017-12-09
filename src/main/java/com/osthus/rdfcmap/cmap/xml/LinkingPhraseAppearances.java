package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "linking-phrase-appearance-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class LinkingPhraseAppearances
{
	private List<LinkingPhraseAppearance> linkingPhraseAppearances = null;

	@XmlElement(name = "linking-phrase-appearance")
	public List<LinkingPhraseAppearance> getLinkingPhraseAppearances()
	{
		return linkingPhraseAppearances;
	}

	public void setLinkingPhraseAppearances(List<LinkingPhraseAppearance> linkingPhraseAppearances)
	{
		this.linkingPhraseAppearances = linkingPhraseAppearances;
	}
}