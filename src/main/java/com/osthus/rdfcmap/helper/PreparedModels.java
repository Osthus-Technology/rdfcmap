package com.osthus.rdfcmap.helper;

import org.apache.jena.rdf.model.Model;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class PreparedModels
{
	Model instanceModel;
	Model visualizationModel;
	Model otherModel;

	public PreparedModels(Model instanceModel, Model visualizationModel, Model otherModel)
	{
		this.instanceModel = instanceModel;
		this.visualizationModel = visualizationModel;
		this.otherModel = otherModel;
	}

	public Model getInstanceModel()
	{
		return instanceModel;
	}

	public void setInstanceModel(Model instanceModel)
	{
		this.instanceModel = instanceModel;
	}

	public Model getVisualizationModel()
	{
		return visualizationModel;
	}

	public void setVisualizationModel(Model visualizationModel)
	{
		this.visualizationModel = visualizationModel;
	}

	public Model getOtherModel()
	{
		return otherModel;
	}

	public void setOtherModel(Model otherModel)
	{
		this.otherModel = otherModel;
	}
}
