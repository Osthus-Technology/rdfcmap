package com.osthus.rdfcmap.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

	private static final Map<String, String> aftPrefixMap = new HashMap<String, String>()
	{
		{
			put("af-c", AFOUtil.AFC_PREFIX);
			put("af-cq", AFOUtil.AFCQ_PREFIX);
			put("af-dt", AFOUtil.AFDT_PREFIX);
			put("af-e", AFOUtil.AFE_PREFIX);
			put("af-m", AFOUtil.AFM_PREFIX);
			put("af-r", AFOUtil.AFR_PREFIX);
			put("af-x", AFOUtil.AFX_PREFIX);
			put("af-p", AFOUtil.AFP_PREFIX);
			put("af-rl", AFOUtil.AFRL_PREFIX);
			put("af-fn", AFOUtil.AFFN_PREFIX);
			put("af-re", AFOUtil.AFRE_PREFIX);
			put("af-q", AFOUtil.AFQ_PREFIX);
			put("af-cur", AFOUtil.AFCUR_PREFIX);
			put("af-ec-001", AFOUtil.AFEC_001_PREFIX);
			put("af-ec-002", AFOUtil.AFEC_002_PREFIX);
			put("af-ec-003", AFOUtil.AFEC_003_PREFIX);
			put("af-ec-004", AFOUtil.AFEC_004_PREFIX);
			put("af-ec-005", AFOUtil.AFEC_005_PREFIX);
			put("af-ec-006", AFOUtil.AFEC_006_PREFIX);
		}
	};

	private static final Map<String, String> oboPrefixMap = new HashMap<String, String>()
	{
		{
			put("obo", AFOUtil.OBO_PREFIX);
			put("cl", AFOUtil.CL_PREFIX);
			put("go", AFOUtil.GO_PREFIX);
			put("ro", AFOUtil.RO_PREFIX);
			put("uo", AFOUtil.UO_PREFIX);
			put("iao", AFOUtil.IAO_PREFIX);
			put("bfo", AFOUtil.BFO_PREFIX);
			put("gaz", AFOUtil.GAZ_PREFIX);
			put("mop", AFOUtil.MOP_PREFIX);
			put("obi", AFOUtil.OBI_PREFIX);
			put("chmo", AFOUtil.CHMO_PREFIX);
			put("envo", AFOUtil.ENVO_PREFIX);
			put("ncbi", AFOUtil.NCBI_PREFIX);
			put("pato", AFOUtil.PATO_PREFIX);
			put("chebi", AFOUtil.CHEBI_PREFIX);
			put("uberon", AFOUtil.UBERON_PREFIX);
		}
	};

	public static Map<String, String> prefixMap = new HashMap<String, String>()
	{
		{
			putAll(aftPrefixMap);
			putAll(oboPrefixMap);
			put("m", AFOUtil.MATHML_PREFIX);
			put("co", AFOUtil.COLLECTION_ONTOLOGY_PREFIX);
			put("ex", AFOUtil.EXAMPLE_PREFIX);
			put("qb", AFOUtil.QB_PREFIX);
			put("dct", AFOUtil.DCT_PREFIX);
			put("ex2", AFOUtil.EXAMPLE2_PREFIX);
			put("hdf", AFOUtil.HDF_PREFIX);
			put("ldp", AFOUtil.LDP_PREFIX);
			put("map", AFOUtil.MAP_PREFIX);
			put("ops", AFOUtil.OPS_PREFIX);
			put("ore", AFOUtil.ORE_PREFIX);
			put("org", AFOUtil.ORG_PREFIX);
			put("owl", AFOUtil.OWL_PREFIX);
			put("pav", AFOUtil.PAV_PREFIX);
			put("rdf", AFOUtil.RDF_PREFIX);
			put("xml", AFOUtil.XML_PREFIX);
			put("xsd", AFOUtil.XSD_PREFIX);
			put("foaf", AFOUtil.FOAF_PREFIX);
			put("omcd", AFOUtil.OPENMATH_PREFIX);
			put("prov", AFOUtil.PROV_PREFIX);
			put("qudt", AFOUtil.QUDT_SCHEMA_PREFIX);
			put("rdfs", AFOUtil.RDFS_PREFIX);
			put("skos", AFOUtil.SKOS_PREFIX);
			put("time", AFOUtil.TIME_PREFIX);
			put("unit", AFOUtil.QUDT_UNIT_PREFIX);
			put("void", AFOUtil.VOID_PREFIX);
			put("afs-c", AFOUtil.AFS_C_PREFIX);
			put("afs-q", AFOUtil.AFS_Q_PREFIX);
			put("shacl", AFOUtil.SHACL_PREFIX);
			put("vcard", AFOUtil.VCARD_PREFIX);
			put("adf-dc", AFOUtil.ADF_DC_PREFIX);
			put("adf-dp", AFOUtil.ADF_DP_PREFIX);
			put("af-map", AFOUtil.AFMAP_PREFIX);
			put("afs-dc", AFOUtil.AFS_DC_PREFIX);
			put("afs-hr", AFOUtil.AFS_HR_PREFIX);
			put("dctype", AFOUtil.DCTYPE_PREFIX);
			put("premis", AFOUtil.PREMIS_PREFIX);
			put("af-math", AFOUtil.AFMATH_PREFIX);
			put("qudt-ext", AFOUtil.QUDT_SCHEMA_EXT_PREFIX);
			put("unit-ext", AFOUtil.QUDT_UNIT_EXT_PREFIX);
			put("adf-dc-hdf", AFOUtil.ADF_DC_HDF_PREFIX);
			put("quantity-ext", AFOUtil.QUDT_QUANTITY_EXT_PREFIX);
		}
	};

	private static String prefixes = "";

	private static Map<String, String> namespaceMap = new HashMap<String, String>();

	public static String getPrefixes()
	{
		if (!prefixes.equals(""))
		{
			return prefixes;
		}

		createPrefixes();

		return prefixes;
	}

	public static Map<String, String> getNamespaceMap()
	{
		if (!namespaceMap.isEmpty())
		{
			return namespaceMap;
		}

		createNamespaceMap();

		return namespaceMap;
	}

	private static void createPrefixes()
	{
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : prefixMap.entrySet())
		{
			sb.append("PREFIX ");
			sb.append(entry.getKey());
			sb.append(": <");
			sb.append(entry.getValue());
			sb.append(">\n");
		}

		prefixes = sb.toString();
		List<String> lines = Arrays.asList(prefixes.split("\\n"));
		Collections.sort(lines);
		prefixes = StringUtils.join(lines, "\n");
	}

	private static void createNamespaceMap()
	{
		for (Entry<String, String> entry : prefixMap.entrySet())
		{
			namespaceMap.put(entry.getValue(), entry.getKey());
		}
	}

	public static void updatePrefixes(String[] newPrefixes)
	{
		for (int i = 0; i < newPrefixes.length; i = i + 2)
		{
			prefixMap.put(newPrefixes[i], newPrefixes[i + 1]);
		}

		createNamespaceMap();
	}

	public static void listPrefixes()
	{
		log.info("Prefixes: ");
		List<String> prefixes = new ArrayList(prefixMap.keySet());
		Collections.sort(prefixes);
		for (Iterator iterator = prefixes.iterator(); iterator.hasNext();)
		{
			String prefix = (String) iterator.next();
			log.info(String.format("%1$15s %2$s", prefix, prefixMap.get(prefix)));
		}
		log.info("");
	}

	public static boolean isAFTNamespace(String namespace)
	{
		if (aftPrefixMap.containsValue(namespace))
		{
			return true;
		}

		return false;
	}

	public static Model convertBlankNodesToNamedResources(Model model)
	{
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
		if (label == null || label.isEmpty() || label.contains("^^"))
		{
			return null;
		}

		String[] segments = label.split(":");
		String namespace;
		String filterLabel;
		if (segments.length > 1)
		{
			namespace = AFOUtil.nsPrefixMap.get(segments[0]);
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
						String currentNamespace = RdfUtil.getNamespaceMap().get(subject.getNameSpace());
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
