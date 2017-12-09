@XmlSchema(//
		namespace = "http://cmap.ihmc.us/xml/cmap/", //
		elementFormDefault = XmlNsForm.QUALIFIED, //
		xmlns = { //
				@XmlNs(prefix = "", namespaceURI = "http://cmap.ihmc.us/xml/cmap/"), //
				@XmlNs(prefix = "dcterms", namespaceURI = "http://purl.org/dc/terms/"), //
				@XmlNs(prefix = "dc", namespaceURI = "http://purl.org/dc/elements/1.1/"), //
				@XmlNs(prefix = "vcard", namespaceURI = "http://www.w3.org/2001/vcard-rdf/3.0#")//
		})

package com.osthus.rdfcmap.cmap.xml;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;