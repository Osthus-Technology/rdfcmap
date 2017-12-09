package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "linking-phrase")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class LinkingPhrase
{
	private String id;
	private String label;
	private String shortComment;
	private String longComment;
	private String parentId;

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

	@XmlAttribute(name = "label")
	public String getLabel()
	{
		return label;
	}

	@XmlAttribute(name = "short-comment")
	public String getShortComment()
	{
		return shortComment;
	}

	@XmlAttribute(name = "long-comment")
	public String getLongComment()
	{
		return longComment;
	}

	@XmlAttribute(name = "parent-id")
	public String getParentId()
	{
		return parentId;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public void setShortComment(String shortComment)
	{
		this.shortComment = shortComment;
	}

	public void setLongComment(String longComment)
	{
		this.longComment = longComment;
	}

	public void setParentId(String parentId)
	{
		this.parentId = parentId;
	}
}