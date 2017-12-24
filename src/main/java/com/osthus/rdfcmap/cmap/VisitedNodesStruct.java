package com.osthus.rdfcmap.cmap;

import java.util.Set;

/**
 * VisitedNodesStruct
 *
 * @author Helge Krieg, OSTHUS GmbH
 */
public class VisitedNodesStruct
{
	private boolean isInstanceNode = false;
	private Set<String> visitedNodes;

	public VisitedNodesStruct(Set<String> visitedNodes)
	{
		super();
		this.visitedNodes = visitedNodes;
	}

	public boolean isInstanceNode()
	{
		return isInstanceNode;
	}

	public void setInstanceNode(boolean isInstanceNode)
	{
		this.isInstanceNode = isInstanceNode;
	}

	public Set<String> getVisitedNodes()
	{
		return visitedNodes;
	}

	public void setVisitedNodes(Set<String> visitedNodes)
	{
		this.visitedNodes = visitedNodes;
	}

}
