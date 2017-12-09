package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "connection-appearance")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ConnectionAppearance
{
	private String id;
	private String fromPos;
	private String toPos;
	private String type;
	private String arrowHead;
	private List<ControlPoint> controlPoints;

	@XmlAttribute(name = "id")
	public String getId()
	{
		return id;
	}

	@XmlAttribute(name = "from-pos")
	public String getFromPos()
	{
		return fromPos;
	}

	@XmlAttribute(name = "to-pos")
	public String getToPos()
	{
		return toPos;
	}

	@XmlAttribute(name = "type")
	public String getType()
	{
		return type;
	}

	@XmlAttribute(name = "arrowhead")
	public String getArrowHead()
	{
		return arrowHead;
	}

	@XmlElement(name = "control-point")
	public List<ControlPoint> getControlPoints()
	{
		return controlPoints;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setFromPos(String fromPos)
	{
		this.fromPos = fromPos;
	}

	public void setToPos(String toPos)
	{
		this.toPos = toPos;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public void setArrowHead(String arrowHead)
	{
		this.arrowHead = arrowHead;
	}

	public void setControlPoints(List<ControlPoint> controlPoints)
	{
		this.controlPoints = controlPoints;
	}
}