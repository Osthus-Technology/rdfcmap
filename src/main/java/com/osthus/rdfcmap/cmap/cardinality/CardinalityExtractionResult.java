package com.osthus.rdfcmap.cmap.cardinality;

import java.util.List;

/**
 * CardinalityExtractionResult
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CardinalityExtractionResult
{
	private String singleRdf;
	private List<StatementWithCardinality> statementsWithCardinality;

	public CardinalityExtractionResult(List<StatementWithCardinality> statementsWithCardinality, String singleRdf)
	{
		super();
		this.singleRdf = singleRdf;
		this.statementsWithCardinality = statementsWithCardinality;
	}

	public String getSingleRdf()
	{
		return singleRdf;
	}

	public void setSingleRdf(String singleRdf)
	{
		this.singleRdf = singleRdf;
	}

	public List<StatementWithCardinality> getStatementsWithCardinality()
	{
		return statementsWithCardinality;
	}

	public void setStatementsWithCardinality(List<StatementWithCardinality> statementsWithCardinality)
	{
		this.statementsWithCardinality = statementsWithCardinality;
	}

}
