package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "image-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Images
{
	private List<Image> images = null;

	@XmlElement(name = "image")
	public List<Image> getImages()
	{
		return images;
	}

	public void setImages(List<Image> images)
	{
		this.images = images;
	}
}