package com.osthus.rdfcmap.path;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntTools;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.Filter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.cmap.Cmap2TurtleConverter;
import com.osthus.rdfcmap.cmap.Turtle2CmapConverter;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.Prefixes;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class PathFinder
{
	private static final Logger log = LogManager.getLogger("Logger");

	private Cmap2TurtleConverter cmap2TurtleConverter;
	private Turtle2CmapConverter turtle2CmapConverter;

	public void find(Path pathToInputFile, String[] additionalFiles) throws JAXBException, IOException, ParserConfigurationException, SAXException
	{
		log.info("Listing graphs from cxl: " + pathToInputFile.toString()
				+ ((additionalFiles != null && additionalFiles.length > 0) ? " using additional files: " + StringUtils.join(additionalFiles, ", ") : ""));

		Model model = CmapUtil.extractStoredModelFromCxl(pathToInputFile);
		CmapUtil.addTriples(additionalFiles, model);
		if (model.isEmpty())
		{
			log.info("No RDF model found, generating from scratch.");
		}
		else
		{
			log.info(model.listStatements().toList().size() + " triples total.");
		}

		cmap2TurtleConverter = new Cmap2TurtleConverter();
		turtle2CmapConverter = new Turtle2CmapConverter();

		model = cmap2TurtleConverter.createOrUpdateVisualizationModel(pathToInputFile, model);
		model = cmap2TurtleConverter.updateModel(model);
		model = cmap2TurtleConverter.cleanModel(model);

		String graphs = listGraphs(model);
		log.info(graphs);
		write(pathToInputFile, graphs);
	}

	private String listGraphs(Model model)
	{
		log.info("Prepare to find graphs.");
		Resource rootNode = extractRootNode(model);
		log.info("Root node: \"" + getResourceLabel(rootNode) + "\" <" + rootNode.getURI() + ">");

		Set<Resource> instances = extractInstances(model, rootNode);
		log.info("Finding shortest paths to " + instances.size() + " target nodes.");

		List<ShortestPath> paths = new ArrayList<>();
		for (Iterator<Resource> iterator = instances.iterator(); iterator.hasNext();)
		{
			Resource instance = iterator.next();
			log.debug("Finding path from root node to \"" + getResourceLabel(instance) + "\" <" + instance.getURI() + ">");
			OntTools.Path path = OntTools.findShortestPath(model, rootNode, instance, Filter.any);

			int hops = 0;
			if (path != null)
			{
				hops = path.size();
			}

			paths.add(new ShortestPath(hops, rootNode, instance, path));
		}

		StringBuilder sb = new StringBuilder();

		sb.append("Instance graphs:\n");
		sb.append(createInstanceGraphs(paths));

		sb.append("Type graphs:\n");
		sb.append(createTypeGraphs(paths, model));

		return sb.toString();
	}

	private String createTypeGraphs(List<ShortestPath> paths, Model model)
	{
		List<String> graphs = new ArrayList<>();
		for (Iterator<ShortestPath> iterator = paths.iterator(); iterator.hasNext();)
		{
			StringBuilder sb = new StringBuilder();
			ShortestPath shortestPath = iterator.next();

			OntTools.Path path = shortestPath.getPath();

			if (path != null)
			{
				Iterator<Statement> pathIterator = path.iterator();

				while (pathIterator.hasNext())
				{
					Statement statement = pathIterator.next();
					String subjectLabel = getResourceLabel(statement.getSubject());
					String predicateLabel = getPropertyLabel(statement.getPredicate());

					sb.append(createTypeLabels(model, statement.getSubject()));
					sb.append("(" + subjectLabel + ")");

					sb.append("\t" + predicateLabel + "\t");

					if (!pathIterator.hasNext())
					{
						sb.append(createTypeLabels(model, statement.getResource()));
						String objectLabel = getResourceLabel(statement.getResource());
						sb.append("(" + objectLabel + ")");
					}
				}
			}
			else
			{
				sb.append("No path found from " + getResourceLabel(shortestPath.getStart()) + " to " + getResourceLabel(shortestPath.getEnd()) + " <"
						+ shortestPath.getEnd() + ">");
			}

			sb.append("#hops = " + shortestPath.getHops());
			sb.append("\n");
			graphs.add(sb.toString());
		}

		List<String> graphs2 = sortAndReorder(graphs);
		return StringUtils.join(graphs2, "");
	}

	private List<String> sortAndReorder(List<String> graphs)
	{
		Collections.sort(graphs);
		List<String> graphs2 = new ArrayList<>(graphs.size());
		for (Iterator<String> iterator = graphs.iterator(); iterator.hasNext();)
		{
			String graph = iterator.next();
			int hopsIndex = graph.indexOf("#hops = ");
			String hops = graph.substring(hopsIndex, graph.length() - 1);
			graph = graph.substring(0, hopsIndex) + "\n";
			graph = hops + "\t" + graph;
			graphs2.add(graph);
		}
		return graphs2;
	}

	private String createTypeLabels(Model model, Resource resource)
	{
		StringBuilder sb = new StringBuilder();
		if (resource.hasProperty(AFOUtil.RDF_TYPE))
		{
			StmtIterator stmtIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
			Set<String> singleHopTypeLabels = new HashSet<>();
			while (stmtIterator.hasNext())
			{
				Statement hopStatement = stmtIterator.next();
				if (hopStatement.getResource().getURI().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
				{
					continue;
				}

				Resource hopType = hopStatement.getResource();
				String hopTypeLabel = getResourceLabel(hopType);
				if (!hopTypeLabel.contains(":"))
				{
					hopTypeLabel = Prefixes.getNamespaceMap().get(hopType.getNameSpace()) + ":" + hopType.getLocalName();
				}

				if (RdfUtil.isAFTNamespace(hopType.getNameSpace()))
				{
					String prefLabel = model.listStatements(hopType, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getString();
					hopTypeLabel = hopTypeLabel + "(" + prefLabel + ")";
				}
				singleHopTypeLabels.add(hopTypeLabel);
			}

			if (!singleHopTypeLabels.isEmpty())
			{
				List<String> hopTypeLabels = new ArrayList<>(singleHopTypeLabels);
				Collections.sort(hopTypeLabels);
				sb.append(StringUtils.join(hopTypeLabels, ","));
			}
		}
		return sb.toString();
	}

	private String createInstanceGraphs(List<ShortestPath> paths)
	{
		List<String> graphs = new ArrayList<>();
		for (Iterator<ShortestPath> iterator = paths.iterator(); iterator.hasNext();)
		{
			StringBuilder sb = new StringBuilder();
			ShortestPath shortestPath = iterator.next();

			OntTools.Path path = shortestPath.getPath();

			if (path != null)
			{
				Iterator<Statement> pathIterator = path.iterator();

				while (pathIterator.hasNext())
				{
					Statement statement = pathIterator.next();
					String subjectLabel = getResourceLabel(statement.getSubject());
					String predicateLabel = getPropertyLabel(statement.getPredicate());

					sb.append(subjectLabel);
					sb.append("\t");
					sb.append(predicateLabel);
					sb.append("\t");
					if (!pathIterator.hasNext())
					{
						String objectLabel = getResourceLabel(statement.getResource());
						sb.append(objectLabel);
					}
				}
			}
			else
			{
				sb.append("No path found from " + getResourceLabel(shortestPath.getStart()) + " to " + getResourceLabel(shortestPath.getEnd()) + " <"
						+ shortestPath.getEnd() + ">");

			}
			sb.append("#hops = " + shortestPath.getHops());
			sb.append("\n");
			graphs.add(sb.toString());
		}

		List<String> graphs2 = sortAndReorder(graphs);
		return StringUtils.join(graphs2, "");
	}

	private Resource extractRootNode(Model model)
	{
		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();

			Resource concept = statement.getSubject();
			if (!concept.isURIResource())
			{
				continue;
			}

			if (!concept.getURI().contains(CmapUtil.URN_UUID))
			{
				continue;
			}

			Resource uiConcept = model.getResource(concept.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));

			// detect root node based on format of border: oval box
			String borderShape = StringUtils.EMPTY;
			if (uiConcept.hasProperty(VizUtil.AFV_HAS_BORDER) && uiConcept.getProperty(VizUtil.AFV_HAS_BORDER).getResource().hasProperty(VizUtil.AFV_SHAPE))
			{
				borderShape = uiConcept.getProperty(VizUtil.AFV_HAS_BORDER).getResource().getProperty(VizUtil.AFV_SHAPE).getString();
			}

			if (borderShape.isEmpty() || !borderShape.equals("oval"))
			{
				continue;
			}

			return concept;
		}

		throw new IllegalStateException("No root node found (with oval border).");
	}

	private Set<Resource> extractInstances(Model model, Resource rootNode)
	{
		Set<Resource> instances = new HashSet<>();
		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();

			Resource concept = statement.getSubject();
			if (!concept.isURIResource())
			{
				continue;
			}

			if (!concept.getURI().contains(CmapUtil.URN_UUID))
			{
				continue;
			}

			if (concept.getURI().equals(rootNode.getURI()))
			{
				continue;
			}

			Resource uiConcept = ResourceFactory.createResource(concept.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			if (!model.containsResource(uiConcept))
			{
				continue;
			}

			if (model.listStatements(uiConcept, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT).hasNext())
			{
				instances.add(concept);
			}
		}

		return instances;
	}

	private String getPropertyLabel(Property property)
	{
		String propertyLabel = StringUtils.EMPTY;
		if (property.asResource().hasProperty(AFOUtil.SKOS_PREF_LABEL))
		{
			propertyLabel = property.asResource().getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
		}
		else if (property.asResource().hasProperty(AFOUtil.RDFS_LABEL))
		{
			ExtendedIterator<Statement> stmtIterator = property.asResource().listProperties(AFOUtil.RDFS_LABEL).filterKeep(t -> t.getLanguage().equals("en"));
			if (stmtIterator.hasNext())
			{
				propertyLabel = stmtIterator.next().getString();
			}
			else
			{
				propertyLabel = property.asResource().getProperty(AFOUtil.RDFS_LABEL).getString();
			}
		}
		else if (property.asResource().hasProperty(AFOUtil.DCT_TITLE))
		{
			propertyLabel = cmap2TurtleConverter.unbreakString(property.asResource().getProperty(AFOUtil.DCT_TITLE).getString());
		}
		else
		{
			propertyLabel = property.asResource().getLocalName();
		}

		return Prefixes.getNamespaceMap().get(property.asResource().getNameSpace()) + ":" + propertyLabel;
	}

	private String getResourceLabel(Resource resource)
	{
		String resourceLabel = StringUtils.EMPTY;

		if (resource.hasProperty(AFOUtil.DCT_TITLE))
		{
			resourceLabel = cmap2TurtleConverter.unbreakString(resource.getProperty(AFOUtil.DCT_TITLE).getString());
		}
		else if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
		{
			resourceLabel = resource.getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
		}
		else if (resource.hasProperty(AFOUtil.RDFS_LABEL))
		{
			resourceLabel = resource.getProperty(AFOUtil.RDFS_LABEL).getString();
		}
		else
		{
			resourceLabel = Prefixes.getNamespaceMap().get(resource.getNameSpace()) + ":" + resource.getLocalName();
		}

		return resourceLabel;
	}

	private void write(Path path, String graphs) throws IOException, FileNotFoundException
	{
		String inputFileName = path.getFileName().toString();
		String outputFileName = inputFileName.substring(0, inputFileName.length() - 3) + "txt";
		Path graphsPath = Paths.get(outputFileName);
		Files.deleteIfExists(graphsPath);
		graphsPath = Files.createFile(graphsPath);
		Files.write(graphsPath, graphs.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
	}

}
