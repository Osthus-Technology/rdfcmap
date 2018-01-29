package com.osthus.rdfcmap.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.osthus.rdfcmap.helper.ConceptRelation;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class RdfUtil
{
	private static final Logger log = LogManager.getLogger("Logger");

	public static boolean isAFTNamespace(String namespace)
	{
		if (Prefixes.aftPrefixMap.containsValue(namespace))
		{
			return true;
		}

		return false;
	}

	public static Model convertBlankNodesToNamedResources(Model model)
	{
		log.info("removing blank nodes from data description");
		Map<String, Resource> id2blankNode = new HashMap<String, Resource>();
		Map<Resource, String> blankNode2id = new HashMap<Resource, String>();
		StmtIterator blankStatementIterator = model.listStatements();
		while (blankStatementIterator.hasNext())
		{
			Statement statement = blankStatementIterator.next();
			Resource subject = statement.getSubject();
			if (!subject.isAnon())
			{
				continue;
			}
			String uuid = CmapUtil.URN_UUID + "bnode:" + UUID.randomUUID();
			id2blankNode.put(uuid, subject);
			blankNode2id.put(subject, uuid);
		}

		if (id2blankNode.isEmpty())
		{
			return model;
		}

		List<Statement> statementsToRemove = new ArrayList<Statement>();
		List<Statement> statementsToAdd = new ArrayList<Statement>();

		for (Entry<Resource, String> entry : blankNode2id.entrySet())
		{
			Resource bnode = entry.getKey();
			String uuid = entry.getValue();
			Resource newResource = model.createResource(uuid);
			StmtIterator stmtIterator = model.listStatements(bnode, (Property) null, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				statementsToRemove.add(statement);

				Statement newStatement = null;
				if (!statement.getObject().isAnon())
				{
					newStatement = ResourceFactory.createStatement(newResource, statement.getPredicate(), statement.getObject());
				}
				else
				{
					Resource newResourceForBlankObject = model.createResource(blankNode2id.get(statement.getObject().asResource()));
					newStatement = ResourceFactory.createStatement(newResource, statement.getPredicate(), newResourceForBlankObject);
				}
				statementsToAdd.add(newStatement);
			}

			stmtIterator = model.listStatements((Resource) null, (Property) null, bnode);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				statementsToRemove.add(statement);

				Statement newStatement = null;
				if (!statement.getSubject().isAnon())
				{
					newStatement = ResourceFactory.createStatement(statement.getSubject(), statement.getPredicate(), newResource);
				}
				else
				{
					Resource newResourceForBlankSubject = model.createResource(blankNode2id.get(statement.getSubject()));
					newStatement = ResourceFactory.createStatement(newResourceForBlankSubject, statement.getPredicate(), newResource);
				}
				statementsToAdd.add(newStatement);
			}
		}

		model.remove(statementsToRemove);
		model.add(statementsToAdd);

		return model;
	}

	public static Map<Resource, ConceptRelation> determineConceptRelations(Model model)
	{
		Map<Resource, ConceptRelation> link2conceptRelations = new HashMap<Resource, ConceptRelation>();
		Set<Resource> handledConnections = new HashSet<>();
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_CONNECTION);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource connection = statement.getSubject();

			if (handledConnections.contains(connection))
			{
				continue;
			}

			Resource from = model.listStatements(connection, VizUtil.AFV_CONNECTS_FROM, (RDFNode) null).next().getResource();
			Resource to = model.listStatements(connection, VizUtil.AFV_CONNECTS_TO, (RDFNode) null).next().getResource();

			Resource fromNode = null;
			Resource toNode = null;
			Resource link = null;
			if (model.listStatements(from, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT).hasNext())
			{
				fromNode = from;
			}
			else
			{
				link = from;
			}
			if (model.listStatements(to, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT).hasNext())
			{
				toNode = to;
			}
			else
			{
				link = to;
			}

			if (fromNode == null)
			{
				Resource sourceConnection = model.listStatements((Resource) null, VizUtil.AFV_CONNECTS_TO, link).next().getSubject();
				fromNode = model.listStatements(sourceConnection, VizUtil.AFV_CONNECTS_FROM, (RDFNode) null).next().getResource();
				handledConnections.add(sourceConnection);
			}
			else
			{
				Resource targetConnection = model.listStatements((Resource) null, VizUtil.AFV_CONNECTS_FROM, link).next().getSubject();
				toNode = model.listStatements(targetConnection, VizUtil.AFV_CONNECTS_TO, (RDFNode) null).next().getResource();
				handledConnections.add(targetConnection);
			}

			link2conceptRelations.put(link, new ConceptRelation(fromNode, toNode, link));

			handledConnections.add(connection);
		}

		return link2conceptRelations;
	}

	public static Resource getResourceByLabel(Model model, String label, boolean includeProperties, boolean isResourceExpected)
	{
		if (label == null || label.isEmpty())
		{
			return null;
		}

		if (label.contains("&#10;"))
		{
			label = label.replaceAll("&#10;", "").trim();
		}

		if (label.contains("["))
		{
			label = label.replaceAll("\\[", "").trim();
		}

		if (label.contains("]"))
		{
			label = label.replaceAll("\\]", "").trim();
		}

		if (label.contains("instance of"))
		{
			label = label.replaceAll("instance of", "").trim();
		}

		if (label.contains("NamedIndividual"))
		{
			label = label.replaceAll("NamedIndividual", "").trim();
		}

		if (label.contains("^^"))
		{
			return null;
		}

		if (label.contains(","))
		{
			String[] types = label.split(",");
			for (int i = 0; i < types.length; i++)
			{
				label = types[i].trim();
				if (label.toLowerCase().startsWith("af"))
				{
					break;
				}
			}
		}

		String[] segments = label.split(":");
		String namespace;
		String filterLabel;
		if (segments.length > 1)
		{
			namespace = Prefixes.nsPrefixMap.get(segments[0]);
			if (namespace == null || namespace.isEmpty())
			{
				log.info("Missing prefix for term: " + label);
				return null;
			}
			filterLabel = segments[1];
		}
		else
		{
			namespace = AFOUtil.AFO_PREFIX;
			filterLabel = label;
		}

		if (namespace.equals(AFOUtil.RDF_PREFIX))
		{
			return model.getResource(namespace + filterLabel);
		}

		if (namespace.contains(AFOUtil.QUDT_SCHEMA_PREFIX))
		{
			filterLabel = filterLabel.replaceAll("\\s", "-");
			filterLabel = WordUtils.capitalizeFully(filterLabel, '-').replaceAll("\\-", "");
			if (!isResourceExpected)
			{
				// property expected that should have label starting with lowercase
				String firstLetterLowercase = filterLabel.substring(0, 1).toLowerCase();
				filterLabel = firstLetterLowercase + filterLabel.substring(1, filterLabel.length());
			}
			return model.getResource(namespace + filterLabel);
		}

		Set<String> partialHits = new HashSet<String>();

		Set<Resource> allClassesAndIndividuals = new HashSet<Resource>();
		StmtIterator classIterator = model.listStatements((Resource) null, (Property) null, AFOUtil.OWL_CLASS);
		while (classIterator.hasNext())
		{
			Statement statement = classIterator.next();
			Resource subject = statement.getSubject();
			allClassesAndIndividuals.add(subject);
		}

		StmtIterator individualIterator = model.listStatements((Resource) null, (Property) null, AFOUtil.OWL_NAMED_INDIVIDUAL);
		while (individualIterator.hasNext())
		{
			Statement statement = individualIterator.next();
			Resource subject = statement.getSubject();
			allClassesAndIndividuals.add(subject);
		}

		if (includeProperties)
		{
			StmtIterator propertiesIterator = model.listStatements((Resource) null, (Property) null, AFOUtil.OWL_OBJECT_PROPERTY);
			while (propertiesIterator.hasNext())
			{
				Statement statement = propertiesIterator.next();
				Resource subject = statement.getSubject();
				allClassesAndIndividuals.add(subject);
			}

			propertiesIterator = model.listStatements((Resource) null, (Property) null, AFOUtil.OWL_DATATYPE_PROPERTY);
			while (propertiesIterator.hasNext())
			{
				Statement statement = propertiesIterator.next();
				Resource subject = statement.getSubject();
				allClassesAndIndividuals.add(subject);
			}

			propertiesIterator = model.listStatements((Resource) null, (Property) null, AFOUtil.OWL_ANNOTATION_PROPERTY);
			while (propertiesIterator.hasNext())
			{
				Statement statement = propertiesIterator.next();
				Resource subject = statement.getSubject();
				allClassesAndIndividuals.add(subject);
			}

		}

		Iterator<Resource> nodeIterator = allClassesAndIndividuals.iterator();
		while (nodeIterator.hasNext())
		{
			Resource subject = nodeIterator.next();

			StmtIterator subjectStmtIterator = model.listStatements(subject, (Property) null, (RDFNode) null);
			while (subjectStmtIterator.hasNext())
			{
				Statement subjectStatement = subjectStmtIterator.next();
				if (subjectStatement.getPredicate().equals(AFOUtil.SKOS_PREF_LABEL) || subjectStatement.getPredicate().equals(AFOUtil.RDFS_LABEL))
				{
					String currentLabel = subjectStatement.getString();
					if (!filterLabel.equals(currentLabel))
					{
						continue;
					}

					if (subject.getURI().startsWith(namespace))
					{
						return subject;
					}
					else
					{
						String currentNamespace = Prefixes.getNamespaceMap().get(subject.getNameSpace());
						if (currentNamespace != null)
						{
							partialHits.add(currentNamespace + ":" + currentLabel);
						}
						else
						{
							partialHits.add(subject.getNameSpace() + currentLabel);
						}
					}
				}
			}
		}

		if (!partialHits.isEmpty())
		{
			log.info("No term found for label \"" + label + "\" but found possible hit: " + StringUtils.join(partialHits, ", "));
		}

		return null;
	}
}
