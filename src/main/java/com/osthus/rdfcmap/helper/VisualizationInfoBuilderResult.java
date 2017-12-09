package com.osthus.rdfcmap.helper;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class VisualizationInfoBuilderResult
{
	private Model model;
	private List<Resource> resources;

	public VisualizationInfoBuilderResult(Model model, List<Resource> resources)
	{
		super();
		this.model = model;
		this.resources = resources;
	}

	public Model getModel()
	{
		return model;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public List<Resource> getResources()
	{
		return resources;
	}

	public void setResources(List<Resource> resources)
	{
		this.resources = resources;
	}
}
