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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public static String getLabelFromDctTitle(Model model, Resource resource)
	{
		if (!resource.hasProperty(AFOUtil.DCT_TITLE))
		{
			return StringUtils.EMPTY;
		}

		String title = resource.getProperty(AFOUtil.DCT_TITLE).getString();
		if (title != null && !title.isEmpty())
		{
			return title.trim();
		}

		return StringUtils.EMPTY;
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

		if (label.contains("&#xa;"))
		{
			label = label.replaceAll("&#xa;", "").trim();
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

		if (label.toLowerCase().contains("instance:"))
		{
			Pattern pattern = Pattern.compile("(?i)(instance\\:[a-zA-Z0-9\\s]+)$");
			Matcher matcher = pattern.matcher(label);
			if (matcher.find())
			{
				String instanceString = matcher.group(1);
				label = label.replaceAll(instanceString, "");
			}
		}

		if (label.contains(",") && !label.contains("("))
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

		String aid = StringUtils.EMPTY;

		if (label.contains("("))
		{
			Pattern pattern = Pattern.compile("\\(([A-Z]+_[0-9]{5,7})\\)");
			Matcher matcher = pattern.matcher(label);
			if (matcher.find())
			{
				aid = matcher.group(1);
				label = label.replaceAll("\\(" + aid + "\\)", "");
			}
		}

		String prefixedIri = StringUtils.EMPTY;
		if (label.contains("("))
		{
			// try pattern for readable prefixed iris
			Pattern pattern = Pattern.compile("\\(([a-zA-Z\\-]+\\:[a-zA-Z]+)\\)");
			Matcher matcher = pattern.matcher(label);
			if (matcher.find())
			{
				prefixedIri = matcher.group(1);
				label = label.replaceAll("\\(" + prefixedIri + "\\)", "");
			}
			else if (label.contains(","))
			{
				// try pattern for comma-separated list of prefixed iris
				pattern = Pattern.compile("\\((([a-zA-Z\\-]+\\:[a-zA-Z]+)[\\s]*[\\,]?[\\s]*)*\\)");
				matcher = pattern.matcher(label);
				if (matcher.find())
				{
					String prefixedIris = matcher.group(0);
					label = label.replaceAll(prefixedIris, "");
					prefixedIris = prefixedIris.replaceAll("\\(", "").replaceAll("\\)", "");
					String[] parts = prefixedIris.split(",");
					for (int i = 0; i < parts.length; i++)
					{
						String part = parts[i].trim();
						if (part.toLowerCase().startsWith("af"))
						{
							// accept first occurrence of an Allotrope term
							prefixedIri = part;
							break;
						}
						if (part.toLowerCase().startsWith("owl"))
						{
							// skip general owl terms
							continue;
						}
						// otherwise accept the last term of other ontologies
						prefixedIri = part;
					}
				}
			}
		}

		String[] segments = label.split(":");
		String namespace = StringUtils.EMPTY;
		String filterLabel = StringUtils.EMPTY;
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
		else if (!prefixedIri.isEmpty())
		{
			segments = prefixedIri.split(":");
			namespace = Prefixes.nsPrefixMap.get(segments[0]);
			if (namespace == null || namespace.isEmpty())
			{
				log.info("Missing prefix for term: " + prefixedIri);
				return null;
			}
			filterLabel = segments[1];
		}
		else if (!aid.isEmpty())
		{
			if (aid.contains("_"))
			{
				String[] parts = StringUtils.split(aid, "_");
				String prefix = parts[0].toLowerCase();
				if (aid.toLowerCase().startsWith("af"))
				{
					prefix = prefix.replaceAll("af", "af-");
					namespace = Prefixes.aftPrefixMap.get(prefix);
				}
				else
				{
					namespace = Prefixes.nsPrefixMap.get(prefix);
				}
			}
			filterLabel = label;
		}
		else
		{
			namespace = AFOUtil.AFO_PREFIX;
			filterLabel = label;
		}

		filterLabel = filterLabel.trim();

		if (namespace.equals(AFOUtil.RDF_PREFIX) || namespace.equals(AFOUtil.RDFS_PREFIX) || namespace.equals(AFOUtil.XSD_PREFIX))
		{
			return model.getResource(namespace + filterLabel);
		}

		if (namespace.contains(AFOUtil.QUDT_SCHEMA_PREFIX) || namespace.contains("qudt") || namespace.contains(AFOUtil.QB_PREFIX))
		{
			if (filterLabel.trim().contains(" "))
			{
				filterLabel = filterLabel.replaceAll("\\s", "-");
				filterLabel = WordUtils.capitalizeFully(filterLabel, '-').replaceAll("\\-", "");
			}
			if (!isResourceExpected)
			{
				// property expected that should have label starting with lowercase
				String firstLetterLowercase = filterLabel.substring(0, 1).toLowerCase();
				filterLabel = firstLetterLowercase + filterLabel.substring(1, filterLabel.length());
			}
			return model.getResource(namespace + filterLabel);
		}

		if (namespace.contains(AFOUtil.ADF_DC_PREFIX))
		{
			return model.getResource(namespace + filterLabel);
		}

		Resource labelDefinedResource = null;
		String prefLabelOfLabelDefinedResource = null;
		if (!aid.isEmpty())
		{
			String iri = StringUtils.EMPTY;
			if (namespace.toLowerCase().contains("allotrope"))
			{
				if (namespace.equals(AFOUtil.AFO_PREFIX))
				{
					String[] parts = StringUtils.split(aid, "_");
					String prefix = "af-" + parts[0].substring(2).toLowerCase();
					namespace = Prefixes.aftPrefixMap.get(prefix);
				}
				iri = namespace + aid;
			}
			else if (namespace.equals(AFOUtil.CHEBI_PREFIX))
			{
				Resource chebiResource = model.createResource(namespace + aid);
				return chebiResource;
			}
			else if (aid.contains("_"))
			{
				String[] parts = StringUtils.split(aid, "_");
				if (Prefixes.oboPrefixMap.containsKey(parts[0].toLowerCase()))
				{
					iri = namespace + "_" + parts[1];
				}
			}
			else
			{
				iri = namespace + aid;
			}
			labelDefinedResource = model.getResource(iri);
			prefLabelOfLabelDefinedResource = RdfUtil.getLabelForResource(labelDefinedResource, model);

			if (!prefLabelOfLabelDefinedResource.isEmpty())
			{
				if (!prefLabelOfLabelDefinedResource.equals(filterLabel))
				{
					log.debug("Found label: " + filterLabel + " for term with id: " + aid + " but ontology enforces different preferred label: "
							+ prefLabelOfLabelDefinedResource);
				}

				return labelDefinedResource;
			}
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
			log.info("No term found for label \"" + label + "\" but found possibly matching term: " + StringUtils.join(partialHits, ", "));
		}

		log.info("No resource found for label: " + label);

		return null;
	}

	private static String getLabelForResource(Resource resource, Model model)
	{
		if (model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).hasNext())
		{
			return model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getString();
		}
		else if (model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).hasNext())
		{
			return model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getString();
		}
		else
		{
			log.info("No label found for resource with iri: " + resource.getURI());
			return StringUtils.EMPTY;
		}
	}
}
