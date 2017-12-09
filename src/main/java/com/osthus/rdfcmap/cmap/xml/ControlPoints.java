package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ControlPoints
{
	private List<ControlPoint> controlPoints = null;

	@XmlElement(name = "control-point")
	public List<ControlPoint> getControlPoints()
	{
		return controlPoints;
	}

	public void setControlPoints(List<ControlPoint> controlPoints)
	{
		this.controlPoints = controlPoints;
	}
}