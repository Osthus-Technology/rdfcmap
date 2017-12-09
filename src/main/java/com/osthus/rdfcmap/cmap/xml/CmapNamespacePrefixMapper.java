package com.osthus.rdfcmap.cmap.xml;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CmapNamespacePrefixMapper extends NamespacePrefixMapper
{

	private static final String DCT_PREFIX = "dct"; // DEFAULT NAMESPACE
	private static final String DCT_URI = "http://purl.org/dc/terms/";

	private static final String DC_PREFIX = "dc"; // DEFAULT NAMESPACE
	private static final String DC_URI = "http://purl.org/dc/elements/1.1/";

	private static final String VCARD_PREFIX = "vcard";
	private static final String VCARD_URI = "http://www.w3.org/2001/vcard-rdf/3.0#";

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix)
	{
		if (DCT_URI.equals(namespaceUri))
		{
			return DCT_PREFIX;
		}
		else if (DC_URI.equals(namespaceUri))
		{
			return DC_PREFIX;
		}
		else if (VCARD_URI.equals(namespaceUri))
		{
			return VCARD_PREFIX;
		}

		return suggestion;
	}

	@Override
	public String[] getPreDeclaredNamespaceUris()
	{
		return new String[] { DCT_URI, DC_URI, VCARD_URI };
	}

}