package com.osthus.rdfcmap.helper;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class LinkedConcept
{
	public String from;
	public String to;

	public LinkedConcept(String from, String to)
	{
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null)
		{
			return false;
		}

		LinkedConcept otherLinkedConcept = (LinkedConcept) other;

		if (!otherLinkedConcept.from.equals(from) && !otherLinkedConcept.from.equals(to))
		{
			return false;
		}
		if (!otherLinkedConcept.to.equals(from) && !otherLinkedConcept.to.equals(to))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		return (from.hashCode() + to.hashCode());
	}
}
