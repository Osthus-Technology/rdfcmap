package com.osthus.adf;

import java.io.IOException;
import java.nio.file.Path;

import org.allotrope.adf.dd.model.DataDescription;
import org.allotrope.adf.model.AdfFile;
import org.allotrope.adf.service.AdfService;
import org.allotrope.adf.service.AdfServiceFactory;
import org.apache.logging.log4j.Logger;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import de.osthus.ambeth.exception.RuntimeExceptionUtil;

/**
 * AdfCreator - Write instances to ADF data description
 * 
 * requires shaded dependencies of ADF (adf.repackaged-1.4.0-shaded.jar)
 * due to incompatibility between Jena 2 (required by ADF1.4) and Jena 3 (required by rdfcmap)
 * if shaded jar is unavailable then remove class AdfCreator
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class AdfCreator
{
	public static void create(Path adfPath, org.apache.jena.rdf.model.Model model, Logger log)
	{
		log.info("Writing ADF to " + adfPath.toAbsolutePath().toString());
		AdfService adfService = AdfServiceFactory.create();
		try (AdfFile adfFile = adfService.createFile(adfPath))
		{
			DataDescription dataDescription = adfFile.getDataDescription();
			Model newModel = dumpModel(model);
			dataDescription.add(newModel);
		}
		catch (IOException e)
		{
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	/**
	 * Currently ADF depends on Jena 2.13.0 which has different model compared to jena 3.1.0 as used by rdfcmap. This method transforms jena 3.1.0 model to jena
	 * 2.13.0 model based on conversion of single triples.
	 *
	 * @param model
	 * @return
	 */
	private static Model dumpModel(org.apache.jena.rdf.model.Model model)
	{
		Model newModel = ModelFactory.createDefaultModel();
		org.apache.jena.rdf.model.StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			org.apache.jena.rdf.model.Statement statement = stmtIterator.next();
			org.apache.jena.rdf.model.Resource subject = statement.getSubject();
			org.apache.jena.rdf.model.Property property = statement.getPredicate();
			org.apache.jena.rdf.model.RDFNode object = statement.getObject();

			Resource newSubject;
			Property newProperty;
			RDFNode newObject;

			if (subject.isURIResource())
			{
				newSubject = newModel.createResource(subject.getURI());
			}
			else
			{
				newSubject = newModel.createResource(new AnonId(subject.asNode().getBlankNodeId().getLabelString()));
			}

			newProperty = newModel.createProperty(property.getURI());

			if (object.isURIResource())
			{
				newObject = newModel.createResource(object.asResource().getURI());
			}
			else if (object.isAnon())
			{
				newObject = newModel.createResource(new AnonId(object.asNode().getBlankNodeId().getLabelString()));
			}
			else
			{
				String datatypeUri = object.asLiteral().getDatatypeURI();
				if (datatypeUri.contains("string"))
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getString());
				}
				else if (datatypeUri.contains("integer"))
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getInt());
				}
				else if (datatypeUri.contains("double"))
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getDouble());
				}
				else if (datatypeUri.contains("float"))
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getFloat());
				}
				else if (datatypeUri.contains("long"))
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getLong());
				}
				else if (datatypeUri.contains("boolean"))
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getBoolean());
				}
				else if (datatypeUri.contains("dateTime"))
				{
					String literalValue = object.asLiteral().getString();
					if (literalValue.contains("\"^^"))
					{
						String[] segments = literalValue.split("\\^\\^");
						literalValue = segments[0].substring(1, segments[0].length() - 1); // cut quotes
					}
					Object literalValueAsObject = javax.xml.bind.DatatypeConverter.parseDateTime(literalValue);
					newObject = newModel.createTypedLiteral(literalValueAsObject);
				}
				else
				{
					newObject = newModel.createTypedLiteral(object.asLiteral().getString());
				}
			}

			newModel.add(newSubject, newProperty, newObject);
		}
		return newModel;
	}
}
