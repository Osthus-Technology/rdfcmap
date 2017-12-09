package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "linking-phrase-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class LinkingPhrases
{
	private List<LinkingPhrase> linkingPhrases = null;

	@XmlElement(name = "linking-phrase")
	public List<LinkingPhrase> getLinkingPhrases()
	{
		return linkingPhrases;
	}

	public void setLinkingPhrases(List<LinkingPhrase> linkingPhrases)
	{
		this.linkingPhrases = linkingPhrases;
	}
}