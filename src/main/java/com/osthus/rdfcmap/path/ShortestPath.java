package com.osthus.rdfcmap.path;

import org.apache.jena.ontology.OntTools.Path;
import org.apache.jena.rdf.model.Resource;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ShortestPath
{
	int hops = 0;
	Resource start = null;
	Resource end = null;
	Path path = null;

	public ShortestPath(int hops, Resource start, Resource end, Path path)
	{
		super();
		this.hops = hops;
		this.start = start;
		this.end = end;
		this.path = path;
	}

	public int getHops()
	{
		return hops;
	}

	public void setHops(int hops)
	{
		this.hops = hops;
	}

	public Resource getStart()
	{
		return start;
	}

	public void setStart(Resource start)
	{
		this.start = start;
	}

	public Resource getEnd()
	{
		return end;
	}

	public void setEnd(Resource end)
	{
		this.end = end;
	}

	public Path getPath()
	{
		return path;
	}

	public void setPath(Path path)
	{
		this.path = path;
	}

}
