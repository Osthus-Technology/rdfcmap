package com.osthus.rdfcmap.sparql;

import java.util.Set;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class PathList
{
	private Set<String> pathList;
	private boolean foundTarget = false;

	public PathList(Set<String> pathList, boolean foundTarget)
	{
		super();
		this.pathList = pathList;
		this.foundTarget = foundTarget;
	}

	public Set<String> getPathList()
	{
		return pathList;
	}

	public void setPathList(Set<String> pathList)
	{
		this.pathList = pathList;
	}

	public boolean isFoundTarget()
	{
		return foundTarget;
	}

	public void setFoundTarget(boolean foundTarget)
	{
		this.foundTarget = foundTarget;
	}
}
