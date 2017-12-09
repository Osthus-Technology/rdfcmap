package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "map")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Map
{
	private String width;

	private String height;

	private Concepts concepts;

	private LinkingPhrases linkingPhrases;

	private Connections connections;

	private ConceptAppearances conceptAppearances;

	private LinkingPhraseAppearances linkingPhraseAppearances;

	private ConnectionAppearances connectionAppearances;

	private StyleSheets styleSheets;

	private ExtraProperties extraProperties;

	private Images images;

	@XmlAttribute(name = "width")
	public String getWidth()
	{
		return width;
	}

	@XmlAttribute(name = "height")
	public String getHeight()
	{
		return height;
	}

	@XmlElement(name = "concept-list")
	public Concepts getConcepts()
	{
		return concepts;
	}

	@XmlElement(name = "linking-phrase-list")
	public LinkingPhrases getLinkingPhrases()
	{
		return linkingPhrases;
	}

	@XmlElement(name = "connection-list")
	public Connections getConnections()
	{
		return connections;
	}

	@XmlElement(name = "concept-appearance-list")
	public ConceptAppearances getConceptAppearances()
	{
		return conceptAppearances;
	}

	@XmlElement(name = "linking-phrase-appearance-list")
	public LinkingPhraseAppearances getLinkingPhraseAppearances()
	{
		return linkingPhraseAppearances;
	}

	@XmlElement(name = "connection-appearance-list")
	public ConnectionAppearances getConnectionAppearances()
	{
		return connectionAppearances;
	}

	@XmlElement(name = "style-sheet-list")
	public StyleSheets getStyleSheets()
	{
		return styleSheets;
	}

	@XmlElement(name = "extra-graphical-properties-list")
	public ExtraProperties getExtraProperties()
	{
		return extraProperties;
	}

	@XmlElement(name = "image-list")
	public Images getImages()
	{
		return images;
	}

	public void setWidth(String width)
	{
		this.width = width;
	}

	public void setHeight(String height)
	{
		this.height = height;
	}

	public void setConcepts(Concepts concepts)
	{
		this.concepts = concepts;
	}

	public void setLinkingPhrases(LinkingPhrases linkingPhrases)
	{
		this.linkingPhrases = linkingPhrases;
	}

	public void setConnections(Connections connections)
	{
		this.connections = connections;
	}

	public void setConceptAppearances(ConceptAppearances conceptAppearances)
	{
		this.conceptAppearances = conceptAppearances;
	}

	public void setLinkingPhraseAppearances(LinkingPhraseAppearances linkingPhraseAppearances)
	{
		this.linkingPhraseAppearances = linkingPhraseAppearances;
	}

	public void setConnectionAppearances(ConnectionAppearances connectionAppearances)
	{
		this.connectionAppearances = connectionAppearances;
	}

	public void setStyleSheets(StyleSheets styleSheets)
	{
		this.styleSheets = styleSheets;
	}

	public void setExtraProperties(ExtraProperties extraProperties)
	{
		this.extraProperties = extraProperties;
	}

	public void setImages(Images images)
	{
		this.images = images;
	}
}
