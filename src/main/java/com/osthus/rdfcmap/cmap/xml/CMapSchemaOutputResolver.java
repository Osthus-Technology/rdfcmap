package com.osthus.rdfcmap.cmap.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CMapSchemaOutputResolver extends SchemaOutputResolver
{

	protected File baseDir;

	public CMapSchemaOutputResolver(File dir)
	{
		super();
		baseDir = dir;
	}

	@Override
	public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException
	{
		return new StreamResult(new File(baseDir, suggestedFileName));
	}
}