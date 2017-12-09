package com.osthus.rdfcmap.helper;

import java.util.List;

import org.apache.jena.rdf.model.Statement;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class StatementDelta
{
	private List<Statement> statementsToAdd;
	private List<Statement> statementsToRemove;

	StatementDelta(List<Statement> statementsToAdd, List<Statement> statementsToRemove)
	{
		super();
		this.statementsToAdd = statementsToAdd;
		this.statementsToRemove = statementsToRemove;
	}

	public List<Statement> getStatementsToAdd()
	{
		return statementsToAdd;
	}

	public void setStatementsToAdd(List<Statement> statementsToAdd)
	{
		this.statementsToAdd = statementsToAdd;
	}

	public List<Statement> getStatementsToRemove()
	{
		return statementsToRemove;
	}

	public void setStatementsToRemove(List<Statement> statementsToRemove)
	{
		this.statementsToRemove = statementsToRemove;
	}

}
