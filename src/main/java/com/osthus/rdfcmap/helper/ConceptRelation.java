package com.osthus.rdfcmap.helper;

import org.apache.jena.rdf.model.Resource;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConceptRelation
{
	public Resource from;
	public Resource to;
	public Resource link;

	public ConceptRelation(Resource from, Resource to, Resource link)
	{
		this.from = from;
		this.to = to;
		this.link = link;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null)
		{
			return false;
		}

		ConceptRelation otherConceptRelation = (ConceptRelation) other;

		if (!otherConceptRelation.from.equals(from) && !otherConceptRelation.from.equals(to) && !otherConceptRelation.link.equals(link))
		{
			return false;
		}
		if (!otherConceptRelation.to.equals(from) && !otherConceptRelation.to.equals(to) && !otherConceptRelation.link.equals(link))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		return (from.hashCode() + to.hashCode() + link.hashCode());
	}
}
