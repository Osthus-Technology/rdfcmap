package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "res-meta")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ResMeta
{
	private String title;
	private String created;
	private String modified;
	private String language;
	private String format;
	private String publisher;
	private String extent;
	private String source;

	@XmlElement(name = "title", namespace = "http://purl.org/dc/elements/1.1/")
	public String getTitle()
	{
		return title;
	}

	@XmlElement(name = "created", namespace = "http://purl.org/dc/terms/")
	public String getCreated()
	{
		return created;
	}

	@XmlElement(name = "modified", namespace = "http://purl.org/dc/terms/")
	public String getModified()
	{
		return modified;
	}

	@XmlElement(name = "language", namespace = "http://purl.org/dc/elements/1.1/")
	public String getLanguage()
	{
		return language;
	}

	@XmlElement(name = "format", namespace = "http://purl.org/dc/elements/1.1/")
	public String getFormat()
	{
		return format;
	}

	@XmlElement(name = "publisher", namespace = "http://purl.org/dc/elements/1.1/")
	public String getPublisher()
	{
		return publisher;
	}

	@XmlElement(name = "extent", namespace = "http://purl.org/dc/elements/1.1/")
	public String getExtent()
	{
		return extent;
	}

	@XmlElement(name = "source", namespace = "http://purl.org/dc/elements/1.1/")
	public String getSource()
	{
		return source;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public void setCreated(String created)
	{
		this.created = created;
	}

	public void setModified(String modified)
	{
		this.modified = modified;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	public void setPublisher(String publisher)
	{
		this.publisher = publisher;
	}

	public void setExtent(String extent)
	{
		this.extent = extent;
	}

	public void setSource(String source)
	{
		this.source = source;
	}
}
