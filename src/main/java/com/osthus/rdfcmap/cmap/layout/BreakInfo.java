package com.osthus.rdfcmap.cmap.layout;

import java.util.LinkedHashSet;
import java.util.Set;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;

/**
 * BreakInfo
 *
 * @author Helge Krieg, OSTHUS GmbH
 */
public class BreakInfo
{
	LinkedHashSet<Node> visited;
	Set<Edge> edgesToBreak;

	public BreakInfo(LinkedHashSet<Node> visited, Set<Edge> edgesToBreak)
	{
		super();
		this.visited = visited;
		this.edgesToBreak = edgesToBreak;
	}

	public LinkedHashSet<Node> getVisited()
	{
		return visited;
	}

	public void setVisitedNodes(LinkedHashSet<Node> visited)
	{
		this.visited = visited;
	}

	public Set<Edge> getEdgesToBreak()
	{
		return edgesToBreak;
	}

	public void setEdgesToBreak(Set<Edge> edgesToBreak)
	{
		this.edgesToBreak = edgesToBreak;
	}

}
