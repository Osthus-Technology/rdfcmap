package com.osthus.rdfcmap.helper;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Point implements Comparable<Point>
{
	public String id;
	public String x;
	public String y;
	public Integer index;

	public Point(String id, String x, String y, Integer index)
	{
		this.id = id;
		this.x = x;
		this.y = y;
		this.index = index;
	}

	public int compareTo(Point o)
	{
		if (index < o.index)
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}
}
