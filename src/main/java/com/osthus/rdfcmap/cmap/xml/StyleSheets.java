package com.osthus.rdfcmap.cmap.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "style-sheet-list")
/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class StyleSheets
{
	private List<StyleSheet> styleSheets = null;

	@XmlElement(name = "style-sheet")
	public List<StyleSheet> getStyleSheets()
	{
		return styleSheets;
	}

	public void setStyleSheets(List<StyleSheet> styleSheets)
	{
		this.styleSheets = styleSheets;
	}
}