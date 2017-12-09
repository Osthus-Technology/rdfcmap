package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "style-sheet")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class StyleSheet
{
	private String id;

	private MapStyle mapStyle;

	private ConceptStyle conceptStyle;

	private LinkingPhraseStyle linkingPhraseStyle;

	private ConnectionStyle connectionStyle;

	private ResourceStyle resourceStyle;

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

	@XmlElement(name = "map-style")
	public MapStyle getMapStyle()
	{
		return mapStyle;
	}

	@XmlElement(name = "concept-style")
	public ConceptStyle getConceptStyle()
	{
		return conceptStyle;
	}

	@XmlElement(name = "linking-phrase-style")
	public LinkingPhraseStyle getLinkingPhraseStyle()
	{
		return linkingPhraseStyle;
	}

	@XmlElement(name = "connection-style")
	public ConnectionStyle getConnectionStyle()
	{
		return connectionStyle;
	}

	@XmlElement(name = "resource-style")
	public ResourceStyle getResourceStyle()
	{
		return resourceStyle;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setMapStyle(MapStyle mapStyle)
	{
		this.mapStyle = mapStyle;
	}

	public void setConceptStyle(ConceptStyle conceptStyle)
	{
		this.conceptStyle = conceptStyle;
	}

	public void setLinkingPhraseStyle(LinkingPhraseStyle linkingPhraseStyle)
	{
		this.linkingPhraseStyle = linkingPhraseStyle;
	}

	public void setConnectionStyle(ConnectionStyle connectionStyle)
	{
		this.connectionStyle = connectionStyle;
	}

	public void setResourceStyle(ResourceStyle resourceStyle)
	{
		this.resourceStyle = resourceStyle;
	}
}
