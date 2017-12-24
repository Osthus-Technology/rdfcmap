package com.osthus.rdfcmap.cmap.layout;

import org.apache.jena.rdf.model.Resource;

/**
 * LinkPosition
 *
 * @author Helge Krieg, OSTHUS GmbH
 */
public class LinkPosition
{
	Resource link = null;
	int x = 0;
	int y = 0;

	public LinkPosition(Resource link, int x, int y)
	{
		super();
		this.link = link;
		this.x = x;
		this.y = y;
	}

	public Resource getLink()
	{
		return link;
	}

	public void setLink(Resource link)
	{
		this.link = link;
	}

	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(int y)
	{
		this.y = y;
	}

}
