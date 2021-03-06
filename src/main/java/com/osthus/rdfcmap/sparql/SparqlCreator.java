package com.osthus.rdfcmap.sparql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.cmap.Cmap2TurtleConverter;
import com.osthus.rdfcmap.helper.PreparedModels;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.Prefixes;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * Create a SPARQL query based on cxl input. 
 * Prepare path from source node to target node. 
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class SparqlCreator
{
	private static final Logger log = LogManager.getLogger("Logger");

	private Cmap2TurtleConverter cmap2TurtleConverter;

	private Set<Statement> graphStatements = new LinkedHashSet<Statement>();

	private Set<String> graphStatementsAsStrings = new LinkedHashSet<String>();

	private Map<String, String> uri2node = new HashMap<String, String>();

	public void create(Path pathToInputFile, String[] additionalFiles) throws JAXBException, IOException, ParserConfigurationException, SAXException
	{
		log.info("Creating sparql from cxl: " + pathToInputFile.toString()
				+ ((additionalFiles != null && additionalFiles.length > 0) ? " using additional files: " + StringUtils.join(additionalFiles, ", ") : ""));

		Model model = CmapUtil.extractStoredModelFromCxl(pathToInputFile);
		model = CmapUtil.addTriples(additionalFiles, model);
		if (model.isEmpty())
		{
			log.info("No RDF model found, generating from scratch.");
		}
		else
		{
			log.info(model.listStatements().toList().size() + " triples total.");
		}

		cmap2TurtleConverter = new Cmap2TurtleConverter();

		model = cmap2TurtleConverter.createOrUpdateVisualizationModel(pathToInputFile, model);
		model = cmap2TurtleConverter.updateModel(model);
		model = cmap2TurtleConverter.cleanModel(model);
		PreparedModels preparedModels = cmap2TurtleConverter.prepareSeparatedModels(model);
		Model instanceModel = preparedModels.getInstanceModel();

		String sparql = createSparql(instanceModel, model);
		sparql = cleanPrefixes(sparql);
		log.info("SPARQL: \n" + sparql);
		write(pathToInputFile, sparql, model);
	}

	private String cleanPrefixes(String sparql)
	{
		String[] lines = sparql.split("\n");
		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i];
			if (!line.toLowerCase().startsWith("prefix "))
			{
				continue;
			}
			String prefix = line.split(" ")[1];
			int numPrefix = StringUtils.countMatches(sparql, prefix);
			if (numPrefix <= 1)
			{
				sparql = sparql.replaceAll(line + "\\n", "");
			}
		}
		return sparql;
	}

	private String createSparql(Model model, Model fullModel)
	{
		log.info("Creating sparql query.");
		StringBuilder sb = new StringBuilder();

		Resource startNode = null;
		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (subject.hasLiteral(VizUtil.AFV_IS_SOURCE_NODE, true))
			{
				startNode = subject;
				break;
			}
		}

		if (startNode != null)
		{
			log.info("Found start node of sparql query: " + getResourceLabel(startNode, model, fullModel));
		}
		else
		{
			throw new IllegalStateException("Missing start node for SPARQL query. Please verify that there exists a resource node with oval, dashed border.");
		}

		Resource targetNode = null;
		stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (subject.hasLiteral(VizUtil.AFV_IS_TARGET_NODE, true))
			{
				targetNode = subject;
				break;
			}
		}

		if (targetNode != null)
		{
			log.info("Found target node of sparql query: " + getResourceLabel(targetNode, model, fullModel));
		}
		else
		{
			throw new IllegalStateException("Missing target node for SPARQL query. Please verify that there exists a resource node with oval, solid border.");
		}

		sb.append("# SPARQL created with rdfcmap V" + RdfCmap.version + " (https://github.com/Osthus-Technology/rdfcmap)\n");
		sb.append(Prefixes.getSparqlPrefixes() + "\n\n");
		sb.append("select distinct ");

		Set<String> targetPropertyLabels = new LinkedHashSet<String>();
		Set<Property> targetProperties = new LinkedHashSet<Property>();
		stmtIterator = model.listStatements(targetNode, (Property) null, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getPredicate().equals(VizUtil.AFV_IS_TARGET_NODE) || statement.getPredicate().equals(VizUtil.AFV_IS_SOURCE_NODE)
					|| statement.getPredicate().equals(AFOUtil.RDF_TYPE))
			{
				// skip for query
				continue;
			}
			String label = getPropertyLabel(statement.getPredicate(), fullModel);
			targetPropertyLabels.add(label);
			targetProperties.add(statement.getPredicate());
		}

		sb.append(StringUtils.join(targetPropertyLabels, " "));
		sb.append(" where {\n");

		Set<String> visited = new LinkedHashSet<String>();
		visited.add(startNode.getURI());
		PathList pathList = new PathList(visited, false);
		pathList = findNeighbour(model, startNode, targetNode, pathList);

		if (!pathList.isFoundTarget())
		{
			throw new IllegalStateException("Could not find path from source to target.");
		}

		log.info("Sequence of statements of graph from source to target:\n" + StringUtils.join(graphStatements, "\n") + "\n");

		sb = createGraphString(model, fullModel, sb, targetNode);

		String targetNodeString = getPrefixedString(targetNode, model, fullModel);
		int i = 0;
		for (Iterator<Property> iterator = targetProperties.iterator(); iterator.hasNext();)
		{
			sb.append("  ");
			Property property = iterator.next();
			sb.append(targetNodeString + " " + getPrefixedString(property, model, fullModel) + " " + targetPropertyLabels.toArray()[i++] + " .\n");
		}

		if (RdfCmap.includeAllNodes)
		{
			sb = createGraphStringForUnhandledNodes(model, fullModel, sb, targetNode);
		}
		sb.append("} \n");
		return sb.toString();
	}

	/**
	 * Create a graph string for all other nodes that have not yet been covered based on path from source to target
	 */
	private StringBuilder createGraphStringForUnhandledNodes(Model model, Model fullModel, StringBuilder sb, Resource targetNode)
	{
		Set<Statement> handledStatements = new LinkedHashSet<Statement>();
		handledStatements.addAll(graphStatements);
		Set<String> handledStatementsAsStrings = new LinkedHashSet<String>();
		handledStatementsAsStrings.addAll(graphStatementsAsStrings);
		for (Iterator<Statement> iterator = model.listStatements(); iterator.hasNext();)
		{
			Statement statement = iterator.next();
			if (handledStatements.contains(statement))
			{
				continue;
			}

			String statementAsString = getStatementString(model, fullModel, statement);
			if (handledStatementsAsStrings.contains(statementAsString))
			{
				continue;
			}

			Resource subject = statement.getSubject();

			if (isTargetNode(subject, targetNode))
			{
				continue;
			}

			if (statement.getPredicate().getURI().equals(VizUtil.AFV_IS_SOURCE_NODE.getURI())
					|| statement.getPredicate().getURI().equals(VizUtil.AFV_IS_TARGET_NODE.getURI()))
			{
				continue;
			}

			if (statement.getPredicate().equals(AFOUtil.RDF_TYPE) && statement.getObject().isResource()
					&& statement.getResource().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
			{
				continue;
			}

			handledStatements.add(statement);
			handledStatementsAsStrings.add(statementAsString);

			sb.append(getStatementString(model, fullModel, statement));

			if (RdfCmap.includePathProperties)
			{
				StmtIterator stmtIterator = model.listStatements(subject, (Property) null, (RDFNode) null);
				while (stmtIterator.hasNext())
				{
					Statement subjectStatement = stmtIterator.next();
					if (handledStatements.contains(subjectStatement))
					{
						continue;
					}

					String subjectStatementAsString = getStatementString(model, fullModel, subjectStatement);
					if (handledStatementsAsStrings.contains(subjectStatementAsString))
					{
						continue;
					}

					if (subjectStatement.getPredicate().equals(AFOUtil.RDF_TYPE) && subjectStatement.getObject().isResource()
							&& subjectStatement.getResource().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
					{
						continue;
					}

					handledStatements.add(subjectStatement);
					handledStatementsAsStrings.add(subjectStatementAsString);

					Property subjectProperty = subjectStatement.getPredicate();
					if (subjectProperty.getURI().equals(statement.getPredicate().getURI())
							|| subjectProperty.getURI().equals(VizUtil.AFV_IS_SOURCE_NODE.getURI())
							|| subjectProperty.getURI().equals(VizUtil.AFV_IS_TARGET_NODE.getURI()))
					{
						continue;
					}

					sb.append(getStatementString(model, fullModel, subjectStatement));
				}
			}
		}

		return sb;
	}

	private boolean isTargetNode(Resource subject, Resource targetNode)
	{
		if (subject.isURIResource())
		{
			if (targetNode.isURIResource() && subject.getURI().equals(targetNode.getURI()))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else if (targetNode.isAnon() && subject.asNode().getBlankNodeId().equals(targetNode.asNode().getBlankNodeId()))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	private StringBuilder createGraphString(Model model, Model fullModel, StringBuilder sb, Resource targetNode)
	{
		for (Iterator<Statement> iterator = graphStatements.iterator(); iterator.hasNext();)
		{
			Statement statement = iterator.next();

			graphStatementsAsStrings.add(getStatementString(model, fullModel, statement));

			if (RdfCmap.includePathProperties)
			{
				StmtIterator stmtIterator = model.listStatements(statement.getSubject(), (Property) null, (RDFNode) null);
				while (stmtIterator.hasNext())
				{
					Statement subjectStatement = stmtIterator.next();
					if (graphStatements.contains(subjectStatement))
					{
						continue;
					}
					String statementAsString = getStatementString(model, fullModel, subjectStatement);
					if (graphStatementsAsStrings.contains(statementAsString))
					{
						continue;
					}

					if (subjectStatement.getPredicate().equals(AFOUtil.RDF_TYPE) && subjectStatement.getObject().isResource()
							&& subjectStatement.getResource().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
					{
						continue;
					}

					Property subjectProperty = subjectStatement.getPredicate();
					if (subjectProperty.getURI().equals(statement.getPredicate().getURI())
							|| subjectProperty.getURI().equals(VizUtil.AFV_IS_SOURCE_NODE.getURI())
							|| subjectProperty.getURI().equals(VizUtil.AFV_IS_TARGET_NODE.getURI()))
					{
						continue;
					}

					if (isTargetNode(statement.getSubject(), targetNode) && subjectStatement.getObject().isLiteral())
					{
						// skip literal properties of target node because they are added as queried parameters
						continue;
					}

					graphStatementsAsStrings.add(getStatementString(model, fullModel, subjectStatement));
				}
			}
		}

		for (Iterator<String> iterator = graphStatementsAsStrings.iterator(); iterator.hasNext();)
		{
			String string = iterator.next();
			sb.append(string);
		}

		return sb;
	}

	private String getStatementString(Model model, Model fullModel, Statement statement)
	{
		String statementAsString = "  ";
		Resource subject = statement.getSubject();
		Property property = statement.getPredicate();
		if (!statement.getObject().isLiteral())
		{
			Resource object = statement.getResource();
			if (subject.isURIResource())
			{
				if (object.isURIResource())
				{
					statementAsString = statementAsString + (getPrefixedString(subject, model, fullModel) + " " + getPrefixedString(property, model, fullModel)
							+ " " + getPrefixedString(object, model, fullModel) + " . \n");
				}
				else
				{
					// anon object
					statementAsString = statementAsString + (getPrefixedString(subject, model, fullModel) + " " + getPrefixedString(property, model, fullModel)
							+ " " + getAnonLabel(object, model, fullModel) + " . \n");
				}
			}
			else
			{
				// anon subject
				if (object.isURIResource())
				{
					// anon subject
					statementAsString = statementAsString + (getAnonLabel(subject, model, fullModel) + " " + getPrefixedString(property, model, fullModel) + " "
							+ getPrefixedString(object, model, fullModel) + " . \n");
				}
				else
				{
					// anon subject and object
					statementAsString = statementAsString + (getAnonLabel(subject, model, fullModel) + " " + getPrefixedString(property, model, fullModel) + " "
							+ getAnonLabel(object, model, fullModel) + " . \n");
				}
			}
		}
		else
		{
			// literal object
			if (subject.isURIResource())
			{
				// literal object
				statementAsString = statementAsString + (getPrefixedString(subject, model, fullModel) + " " + getPrefixedString(property, model, fullModel)
						+ " " + getLiteralString(statement.getObject().asLiteral()) + " . \n");
			}
			else
			{
				// anon subject and literal object
				statementAsString = statementAsString + (getAnonLabel(subject, model, fullModel) + " " + getPrefixedString(property, model, fullModel) + " "
						+ getLiteralString(statement.getObject().asLiteral()) + " . \n");
			}
		}

		return statementAsString;
	}

	private String getLiteralString(Literal literal)
	{
		String string = "\"";
		string = string + literal.getLexicalForm();
		String dataTypeIri = literal.getDatatypeURI();
		dataTypeIri = dataTypeIri.replace(AFOUtil.XSD_PREFIX, "xsd:");
		string = string + "\"^^" + dataTypeIri;
		return string;
	}

	private String getPrefixedString(Resource resource, Model model, Model fullModel)
	{
		String namespace = resource.getNameSpace();
		String localName = resource.getLocalName();
		String prefix = Prefixes.getNamespaceMap().get(namespace);
		if (prefix == null && resource.isURIResource() && namespace.equals(AFOUtil.OBO_PREFIX))
		{
			return getOboPrefix(resource);
		}
		else if (prefix == null)
		{
			if (resource.isAnon())
			{
				return getAnonLabel(resource, model, fullModel);
			}
			else if (resource.getURI().startsWith(CmapUtil.URN_UUID))
			{
				if (uri2node.containsKey(resource.getURI()))
				{
					return uri2node.get(resource.getURI());
				}
				String nodeLabel = "?" + getResourceLabel(resource, model, fullModel).replaceAll("[\\-\\s\\(\\)]", "_") + "_" + uri2node.size();
				uri2node.put(resource.getURI(), nodeLabel);

				return nodeLabel;
			}
			else
			{
				return "<" + resource.getURI() + ">";
			}
		}
		else if (prefix.equals("obo"))
		{
			return getOboPrefix(resource);
		}
		else
		{
			return prefix + ":" + localName;
		}
	}

	private String getOboPrefix(Resource resource)
	{
		String[] segments = resource.getURI().split("_");
		String prefix = Prefixes.getNamespaceMap().get(segments[0]);
		return prefix + ":_" + segments[segments.length - 1];
	}

	private String getAnonLabel(Resource resource, Model model, Model fullModel)
	{
		String label = resource.asNode().getBlankNodeId().getLabelString();
		String[] segments = label.split(":");
		String id = segments[segments.length - 1];
		id = id.replaceAll("-", "");

		String typeLabel = StringUtils.EMPTY;
		StmtIterator stmtIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource type = statement.getResource();
			if (type.isAnon())
			{
				continue;
			}

			Resource typeResource = fullModel.getResource(type.getURI());
			if (typeResource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
			{
				typeLabel = typeResource.getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
			}
			else if (typeResource.hasProperty(AFOUtil.RDFS_LABEL))
			{
				typeLabel = typeResource.getProperty(AFOUtil.RDFS_LABEL).getString();
			}
			if (!typeLabel.isEmpty())
			{
				typeLabel = typeLabel.replaceAll("[\\s\\(\\)]+", "_");
				typeLabel = typeLabel + "_";
				break;
			}
		}

		return "_:" + typeLabel + id;
	}

	private PathList findNeighbour(Model model, Resource currentNode, Resource targetNode, PathList pathList)
	{
		// start at node
		// --iterate over all object properties
		// --get neighbour forward
		// -----if (not visited and not target) store as visited and get next neighbour forward
		// -----if (visited) get next neighbour forward
		// -----if (target) stop
		// --if no neighbour found forward
		// -----get neighbour based on inverse relation pointing to node
		// --------if (not visited and not target) store as visited and get next neighbour inverse forward
		// --------if (visited) get next neighbour inverse forward
		// --------if (target) stop
		// repeat until target found
		// include all other unvisited nodes and object properties (second version)

		Set<String> visited = pathList.getPathList();
		StmtIterator nodeStmtIterator = model.listStatements(currentNode, (Property) null, (RDFNode) null);
		while (nodeStmtIterator.hasNext())
		{
			Statement statement = nodeStmtIterator.next();
			if (statement.getObject().isLiteral())
			{
				continue;
			}

			if (statement.getPredicate().equals(AFOUtil.RDF_TYPE))
			{
				continue;
			}

			Resource nextNode = statement.getResource();
			String id;
			if (nextNode.isAnon())
			{
				id = nextNode.asNode().getBlankNodeId().getLabelString();
			}
			else
			{
				id = nextNode.getURI();
			}
			if (visited.contains(id))
			{
				continue;
			}

			if (nextNode.equals(targetNode))
			{
				// DONE!!
				visited.add(id);
				pathList.setPathList(visited);
				pathList.setFoundTarget(true);
				graphStatements.add(statement);
				return pathList;
			}

			visited.add(id);
			pathList.setPathList(visited);
			graphStatements.add(statement);

			if (findNeighbour(model, nextNode, targetNode, pathList).isFoundTarget())
			{
				pathList.setFoundTarget(true);
				return pathList;
			}
		}

		// no forward link leads to target now check inverse links
		if (findNeighbourInverse(model, currentNode, targetNode, pathList).isFoundTarget())
		{
			pathList.setFoundTarget(true);
			return pathList;
		}

		return pathList;
	}

	private PathList findNeighbourInverse(Model model, Resource currentNode, Resource targetNode, PathList pathList)
	{
		Set<String> visited = pathList.getPathList();
		StmtIterator reverseNodeStmtIterator = model.listStatements((Resource) null, (Property) null, currentNode);
		while (reverseNodeStmtIterator.hasNext())
		{
			Statement statement = reverseNodeStmtIterator.next();

			if (statement.getPredicate().equals(AFOUtil.RDF_TYPE))
			{
				continue;
			}

			Resource nextNode = statement.getSubject();

			String id;
			if (nextNode.isAnon())
			{
				id = nextNode.asNode().getBlankNodeId().getLabelString();
			}
			else
			{
				id = nextNode.getURI();
			}

			if (visited.contains(id))
			{
				continue;
			}

			if (nextNode.equals(targetNode))
			{
				visited.add(id);
				pathList.setPathList(visited);
				pathList.setFoundTarget(true);
				graphStatements.add(statement);
				return pathList;
			}

			visited.add(id);
			pathList.setPathList(visited);
			graphStatements.add(statement);

			if (findNeighbour(model, nextNode, targetNode, pathList).isFoundTarget())
			{
				pathList.setFoundTarget(true);
				return pathList;
			}
		}
		return pathList;
	}

	private String getPropertyLabel(Property property, Model model)
	{
		String propertyLabel = StringUtils.EMPTY;
		if (model.listStatements(property.asResource(), AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).hasNext())
		{
			propertyLabel = model.listStatements(property.asResource(), AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getString();
		}
		else if (model.listStatements(property.asResource(), AFOUtil.RDFS_LABEL, (RDFNode) null).hasNext())
		{
			propertyLabel = model.listStatements(property.asResource(), AFOUtil.RDFS_LABEL, (RDFNode) null).next().getString();
		}
		else if (model.listStatements(property.asResource(), AFOUtil.DCT_TITLE, (RDFNode) null).hasNext())
		{
			propertyLabel = model.listStatements(property.asResource(), AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
		}
		else if (model.listStatements(property.asResource(), AFOUtil.DCT_TITLE, (RDFNode) null).hasNext())
		{
			propertyLabel = model.listStatements(property.asResource(), AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
		}
		else if (model.listStatements(property.asResource(), AFOUtil.OBO_EDITOR_PREFERRED_LABEL, (RDFNode) null).hasNext())
		{
			propertyLabel = model.listStatements(property.asResource(), AFOUtil.OBO_EDITOR_PREFERRED_LABEL, (RDFNode) null).next().getString();
		}
		else if (property.asResource().getLocalName() != null && !property.asResource().getLocalName().isEmpty())
		{
			propertyLabel = property.asResource().getLocalName();
		}
		else
		{
			propertyLabel = "property";
		}

		return "?" + propertyLabel.replaceAll("\\s", "_");
	}

	private String getResourceLabel(Resource resource, Model model, Model fullModel)
	{
		String resourceLabel = StringUtils.EMPTY;

		if (resource.hasProperty(AFOUtil.DCT_TITLE))
		{
			resourceLabel = resource.getProperty(AFOUtil.DCT_TITLE).getString();
		}
		else if (resource.hasProperty(AFOUtil.RDFS_LABEL))
		{
			resourceLabel = resource.getProperty(AFOUtil.RDFS_LABEL).getString();
		}
		else if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
		{
			resourceLabel = resource.getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
		}
		else if (model.contains(resource, AFOUtil.RDF_TYPE))
		{
			StmtIterator stmtIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				Resource object = statement.getResource();
				if (object.isAnon())
				{
					continue;
				}
				if (object.getURI().startsWith(AFOUtil.OWL_PREFIX))
				{
					continue;
				}

				if (fullModel.contains(object, AFOUtil.SKOS_PREF_LABEL))
				{
					resourceLabel = fullModel.listStatements(object, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getString();
					break;
				}
				else if (fullModel.contains(object, AFOUtil.RDFS_LABEL))
				{
					resourceLabel = fullModel.listStatements(object, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getString();
					break;
				}
			}

			if (!resourceLabel.isEmpty())
			{
				if (resource.isURIResource())
				{
					return resourceLabel;
				}

				return "[ " + resourceLabel + " ]";
			}
		}

		if (resource.isAnon())
		{
			resourceLabel = resource.asNode().getBlankNodeLabel();
		}
		else if (!resource.getURI().contains(CmapUtil.URN_UUID) && resource.getLocalName() != null && !resource.getLocalName().isEmpty())
		{
			resourceLabel = resource.getLocalName();
		}
		else
		{
			resourceLabel = "unknown";
		}

		return resourceLabel;
	}

	private void write(Path path, String sparql, Model model) throws IOException, FileNotFoundException
	{
		String inputFileName = path.getFileName().toString();
		String outputFileName = inputFileName.substring(0, inputFileName.length() - 3) + "sparql";
		Path sparqlPath = Paths.get(outputFileName);
		Files.deleteIfExists(sparqlPath);
		sparqlPath = Files.createFile(sparqlPath);
		Files.write(sparqlPath, sparql.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

		List<String> lines = Files.readAllLines(sparqlPath, Charset.defaultCharset());
		lines = Cmap2TurtleConverter.addCommentsWithHumanReadableIds(lines, model);
		outputFileName = outputFileName.substring(0, outputFileName.length() - 7) + "-human-readable.sparql";
		sparqlPath = Paths.get(outputFileName);
		Files.deleteIfExists(sparqlPath);
		sparqlPath = Files.createFile(sparqlPath);
		Cmap2TurtleConverter.writeFile(sparqlPath, lines);

	}

}
