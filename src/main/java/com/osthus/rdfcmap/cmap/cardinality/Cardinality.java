package com.osthus.rdfcmap.cmap.cardinality;

import org.apache.jena.rdf.model.Resource;

import com.osthus.rdfcmap.util.VizUtil;

/**
 * Cardinality
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Cardinality
{
	Resource type;
	String minimumValue;
	String maximumValue;

	public Cardinality(Resource type, String minimumValue, String maximumValue)
	{
		super();
		if (type == null || minimumValue == null || maximumValue == null)
		{
			throw new IllegalStateException("Invalid cardinality.");
		}
		this.type = type;
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
	}

	public Resource getType()
	{
		return type;
	}

	public void setType(Resource type)
	{
		this.type = type;
	}

	public String getMinimumValue()
	{
		return minimumValue;
	}

	public void setMinimumValue(String minimumValue)
	{
		this.minimumValue = minimumValue;
	}

	public String getMaximumValue()
	{
		return maximumValue;
	}

	public void setMaximumValue(String maximumValue)
	{
		this.maximumValue = maximumValue;
	}

	@Override
	public String toString()
	{
		if (VizUtil.AFV_EXACT_CARDINALITY.getURI().equals(type.getURI()))
		{
			return minimumValue;
		}

		if (VizUtil.AFV_MIN_CARDINALITY.getURI().equals(type.getURI()))
		{
			return ">" + minimumValue;
		}

		if (VizUtil.AFV_MAX_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "<" + maximumValue;
		}

		if (VizUtil.AFV_INTERVAL_CARDINALITY.getURI().equals(type.getURI()))
		{
			return minimumValue + " " + maximumValue;
		}

		if (VizUtil.AFV_MAX_EXCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "<" + maximumValue + "[";
		}

		if (VizUtil.AFV_MAX_INCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "<" + maximumValue + "]";
		}

		if (VizUtil.AFV_MIN_EXCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return ">" + minimumValue + "[";
		}

		if (VizUtil.AFV_MIN_EXCLUSIVE_MAX_EXCLUSIVED_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "]" + minimumValue + " " + maximumValue + "[";
		}

		if (VizUtil.AFV_MIN_EXCLUSIVE_MAX_INCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "]" + minimumValue + " " + maximumValue + "]";
		}

		if (VizUtil.AFV_MIN_INCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return ">" + minimumValue + "]";
		}

		if (VizUtil.AFV_MIN_INCLUSIVE_MAX_EXCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "[" + minimumValue + " " + maximumValue + "[";
		}

		if (VizUtil.AFV_MIN_INCLUSIVE_MAX_INCLUSIVE_CARDINALITY.getURI().equals(type.getURI()))
		{
			return "[" + minimumValue + " " + maximumValue + "]";
		}

		throw new IllegalStateException("Unknown cardinality type: " + type.getURI());
	}
}
