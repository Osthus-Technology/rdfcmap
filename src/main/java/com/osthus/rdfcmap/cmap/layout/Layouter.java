package com.osthus.rdfcmap.cmap.layout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.helper.ConceptRelation;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * Layouter
 *
 * Layouter transforms RDF model into graphviz graph and executes layouting algorithm.
 *
 * This class requires additional third-party dependencies. Please contact office@osthus.com for further support and information.
 *
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Layouter
{
	private static final Logger log = LogManager.getLogger("Logger");
	private static Map<Resource, ConceptRelation> link2conceptRelations = new HashMap<Resource, ConceptRelation>();

	public static Model optimizeLayout(Model model)
	{
		// Init a project - and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();

		// Generate a new random graph into a container
		Container container = Lookup.getDefault().lookup(Container.Factory.class).newContainer();
		// Append container to graph structure
		ImportController importController = Lookup.getDefault().lookup(ImportController.class);
		importController.process(container, new DefaultProcessor(), workspace);

		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
		DirectedGraph directedGraph = graphModel.getDirectedGraph();

		Map<Node, Resource> node2UiConcept = null;

		if (RdfCmap.layoutLinks)
		{
			node2UiConcept = createGraphFromModel(model, graphModel, directedGraph);
		}
		else
		{
			node2UiConcept = createGraphFromConceptsInModel(model, graphModel, directedGraph);
		}

		log.info("Nodes: " + directedGraph.getNodeCount());
		log.info("Edges: " + directedGraph.getEdgeCount());

		if (RdfCmap.breakCycles)
		{
			graphModel = determineEdgesToBreakCycles(directedGraph, graphModel);
			directedGraph = graphModel.getDirectedGraph();
		}

		log.debug("Graph coordinates before layout:");
		for (Node n : directedGraph.getNodes())
		{
			log.debug(String.format(" X Y %14.6f %14.6f %14.6f %10s", n.x(), n.y(), n.size(), n.getLabel()));
		}

		if (RdfCmap.isAutoLayout)
		{
			graphModel = doAutoLayout(graphModel);
		}
		else if (RdfCmap.isRadialLayout)
		{
			graphModel = doRadialAxisLayout(graphModel);
		}
		else if (RdfCmap.isCircleLayout)
		{
			graphModel = doCircleLayout(graphModel);
		}
		else if (RdfCmap.isGraphVizLayout)
		{
			graphModel = doGraphVizLayout(graphModel);
		}

		log.info("Layout finished. Optimizing coordinates for cmap.");

		float minX = 0.0f;
		float minY = 0.0f;
		log.debug("Graph coordinates after layout:");
		for (Node n : directedGraph.getNodes())
		{
			log.debug(String.format(" X Y SIZE %14.6f %14.6f %14.6f %10s", n.x(), n.y(), n.size(), n.getLabel()));
			if (n.x() < minX)
			{
				minX = n.x();
			}
			if (n.y() < minY)
			{
				minY = n.y();
			}
		}
		log.debug("min x: " + minX + " min y: " + minY);

		log.debug("Coordinates after transformation:");
		for (Node n : directedGraph.getNodes())
		{
			log.debug(String.format(" X Y %14.6f %14.6f %14.6f %10s", n.x() - minX + 100, n.y() - minY + 100, n.size(), n.getLabel()));
			Resource uiConcept = node2UiConcept.get(n);
			model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_X_POSITION, String.format("%.0f", n.x() - minX + 100));
			model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_Y_POSITION, String.format("%.0f", n.y() - minY + 100));
		}

		if (!RdfCmap.layoutLinks)
		{
			model = updateLinkLocations(model);
		}

		log.info("Layout finished. Exporting snapshot to autolayout.pdf");
		PreviewModel previewModel = Lookup.getDefault().lookup(PreviewController.class).getModel();
		previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
		previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT,
				previewModel.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));
		previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		try
		{
			ec.exportFile(new File("autolayout.pdf"));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		log.info("Export done.");

		return model;
	}

	private static Model updateLinkLocations(Model model)
	{
		log.info("updating link locations.");
		List<Integer> xcoord = new ArrayList<>();
		List<Integer> ycoord = new ArrayList<>();

		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			xcoord.add(Integer.valueOf(model.listStatements(statement.getSubject(), VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString()));
			ycoord.add(Integer.valueOf(model.listStatements(statement.getSubject(), VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString()));
		}

		Collections.sort(xcoord);
		Collections.sort(ycoord);

		log.debug("link positions:");
		List<LinkPosition> linkPositions = new ArrayList<LinkPosition>();
		for (Entry<Resource, ConceptRelation> entry : link2conceptRelations.entrySet())
		{
			Resource link = entry.getKey();
			Resource from = entry.getValue().from;
			Resource to = entry.getValue().to;
			int size = Integer.valueOf(model.listStatements(link, VizUtil.AFV_WIDTH, (RDFNode) null).next().getString());
			int x1 = Integer.valueOf(model.listStatements(from, VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString());
			int y1 = Integer.valueOf(model.listStatements(from, VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString());
			int x2 = Integer.valueOf(model.listStatements(to, VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString());
			int y2 = Integer.valueOf(model.listStatements(to, VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString());
			int x = (int) (Math.round(0.5 * (x1 + x2)));
			int y = (int) (Math.round(0.5 * (y1 + y2)));
			log.debug(String.format(" X Y %14d %14d %14d %10s", x, y, size, determineNodeLabel(link.getURI(), model)));

			x = removeOverlap(xcoord, x);
			y = removeOverlap(ycoord, y);

			log.debug(String.format(" X Y %14d %14d %14d %10s", x, y, size, determineNodeLabel(link.getURI(), model)));
			model = CmapUtil.createOrUpdateLiteralValue(model, link, VizUtil.AFV_X_POSITION, String.valueOf(x));
			model = CmapUtil.createOrUpdateLiteralValue(model, link, VizUtil.AFV_Y_POSITION, String.valueOf(y));
			linkPositions.add(new LinkPosition(link, x, y));
		}

		if (RdfCmap.avoidLinkLinkOverlap)
		{
			log.debug("Avoiding overlap between links.");
			List<Resource> handledLinks = new ArrayList<>();
			int counter = 1;
			for (LinkPosition linkPosition : linkPositions)
			{
				Resource link = linkPosition.getLink();
				int size = Integer.valueOf(model.listStatements(link, VizUtil.AFV_WIDTH, (RDFNode) null).next().getString());
				int x = linkPosition.getX();
				int y = linkPosition.getY();

				List<Integer> linkXcoord = new ArrayList<>();
				List<Integer> linkYcoord = new ArrayList<>();
				for (LinkPosition otherLinkPosition : linkPositions)
				{
					if (linkPosition.getLink().equals(otherLinkPosition.getLink()) || handledLinks.contains(otherLinkPosition.getLink()))
					{
						continue;
					}
					linkXcoord.add(otherLinkPosition.getX());
					linkYcoord.add(otherLinkPosition.getY());
				}

				x = removeOverlap(linkXcoord, x);
				y = removeOverlap(linkYcoord, y);

				log.debug(String.format(" X Y %14d %14d %14d %10s %6d/%d", x, y, size, determineNodeLabel(link.getURI(), model), counter++,
						linkPositions.size()));
				model = CmapUtil.createOrUpdateLiteralValue(model, link, VizUtil.AFV_X_POSITION, String.valueOf(x));
				model = CmapUtil.createOrUpdateLiteralValue(model, link, VizUtil.AFV_Y_POSITION, String.valueOf(y));
				handledLinks.add(link);
			}
		}
		return model;
	}

	private static int removeOverlap(List<Integer> coord, int pos)
	{
		int hit = -1;
		for (int i = 0; i < coord.size(); i++)
		{
			if (coord.get(i) >= pos)
			{
				hit = coord.get(i);
				break;
			}
		}

		if (hit > 0)
		{
			int overlap = 5;
			for (int j = 0; j <= 20; j = j + 1)
			{
				if (hit > 0 && Math.abs(hit - pos) <= overlap)
				{
					pos = pos + 10;
				}
				hit = -1;
				for (int i = 0; i < coord.size(); i++)
				{
					if (coord.get(i) >= pos)
					{
						hit = coord.get(i);
						break;
					}
				}

				if (hit < 0)
				{
					break;
				}

			}
		}
		return pos;
	}

	private static Map<Node, Resource> createGraphFromModel(Model model, GraphModel graphModel, DirectedGraph directedGraph)
	{
		Map<Resource, Node> uiConcept2Node = new HashMap<>();
		Map<Node, Resource> node2UiConcept = new HashMap<>();
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_CONNECTION);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource connection = statement.getSubject();
			Resource from = model.listStatements(connection, VizUtil.AFV_CONNECTS_FROM, (RDFNode) null).next().getResource();
			Resource to = model.listStatements(connection, VizUtil.AFV_CONNECTS_TO, (RDFNode) null).next().getResource();

			String fromX = model.listStatements(from, VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString();
			String fromY = model.listStatements(from, VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString();
			String toX = model.listStatements(to, VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString();
			String toY = model.listStatements(to, VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString();

			Float fromSize = 0.0f;
			Float toSize = 0.0f;
			if (model.listStatements(from, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).hasNext())
			{
				fromSize = Float.valueOf(model.listStatements(from, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).next().getString());
			}
			if (model.listStatements(to, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).hasNext())
			{
				toSize = Float.valueOf(model.listStatements(to, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).next().getString());
			}

			Node fromNode = null;
			if (!uiConcept2Node.containsKey(from))
			{
				fromNode = graphModel.factory().newNode(String.valueOf(uiConcept2Node.size()));
				fromNode.setLabel(determineNodeLabel(from.getURI(), model));
				if (fromSize > 0.0f && RdfCmap.isAutoLayout)
				{
					fromNode.setSize(Math.max(Math.min(500.0f / fromSize, 100.0f), 500.0f));
				}
				else
				{
					fromNode.setSize(RdfCmap.nodeSize);
				}
				fromNode.setX(Float.valueOf(fromX));
				fromNode.setY(Float.valueOf(fromY));
				fromNode.setZ(0.0f);
				uiConcept2Node.put(from, fromNode);
				node2UiConcept.put(fromNode, from);
				directedGraph.addNode(fromNode);
			}
			else
			{
				fromNode = uiConcept2Node.get(from);
			}

			Node toNode = null;
			if (!uiConcept2Node.containsKey(to))
			{
				toNode = graphModel.factory().newNode(String.valueOf(uiConcept2Node.size()));
				toNode.setLabel(determineNodeLabel(to.getURI(), model));
				if (toSize > 0.0f && RdfCmap.isAutoLayout)
				{
					toNode.setSize(Math.max(Math.min(500.0f / toSize, 100.0f), 500.0f));
				}
				else
				{
					toNode.setSize(RdfCmap.nodeSize);
				}
				toNode.setX(Float.valueOf(toX));
				toNode.setY(Float.valueOf(toY));
				toNode.setZ(0.0f);
				uiConcept2Node.put(to, toNode);
				node2UiConcept.put(toNode, to);
				directedGraph.addNode(toNode);
			}
			else
			{
				toNode = uiConcept2Node.get(to);
			}

			Edge edge = graphModel.factory().newEdge(fromNode, toNode, 0, true);
			edge.setWeight(0.1);
			edge.setLabel(from.getURI() + " --> " + to.getURI());
			directedGraph.addEdge(edge);
		}
		return node2UiConcept;
	}

	private static Map<Node, Resource> createGraphFromConceptsInModel(Model model, GraphModel graphModel, DirectedGraph directedGraph)
	{
		link2conceptRelations = RdfUtil.determineConceptRelations(model);

		Map<Resource, Node> uiConcept2Node = new HashMap<>();
		Map<Node, Resource> node2UiConcept = new HashMap<>();

		for (Entry<Resource, ConceptRelation> entry : link2conceptRelations.entrySet())
		{
			Resource from = entry.getValue().from;
			Resource to = entry.getValue().to;
			String fromX = model.listStatements(from, VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString();
			String fromY = model.listStatements(from, VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString();
			String toX = model.listStatements(to, VizUtil.AFV_X_POSITION, (RDFNode) null).next().getString();
			String toY = model.listStatements(to, VizUtil.AFV_Y_POSITION, (RDFNode) null).next().getString();

			Float fromSize = 0.0f;
			Float toSize = 0.0f;
			if (model.listStatements(from, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).hasNext())
			{
				fromSize = Float.valueOf(model.listStatements(from, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).next().getString());
			}
			if (model.listStatements(to, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).hasNext())
			{
				toSize = Float.valueOf(model.listStatements(to, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).next().getString());
			}

			Node fromNode = null;
			if (!uiConcept2Node.containsKey(from))
			{
				fromNode = graphModel.factory().newNode(String.valueOf(uiConcept2Node.size()));
				fromNode.setLabel(determineNodeLabel(from.getURI(), model));
				if (fromSize > 0.0f && RdfCmap.isAutoLayout)
				{
					fromNode.setSize(Math.max(Math.min(500.0f / fromSize, 100.0f), 500.0f));
				}
				else
				{
					fromNode.setSize(RdfCmap.nodeSize);
				}
				fromNode.setX(Float.valueOf(fromX));
				fromNode.setY(Float.valueOf(fromY));
				fromNode.setZ(0.0f);
				uiConcept2Node.put(from, fromNode);
				node2UiConcept.put(fromNode, from);
				directedGraph.addNode(fromNode);
			}
			else
			{
				fromNode = uiConcept2Node.get(from);
			}

			Node toNode = null;
			if (!uiConcept2Node.containsKey(to))
			{
				toNode = graphModel.factory().newNode(String.valueOf(uiConcept2Node.size()));
				if (to.isURIResource())
				{
					toNode.setLabel(determineNodeLabel(to.getURI(), model));
				}
				if (toSize > 0.0f && RdfCmap.isAutoLayout)
				{
					toNode.setSize(Math.max(Math.min(500.0f / toSize, 100.0f), 500.0f));
				}
				else
				{
					toNode.setSize(RdfCmap.nodeSize);
				}
				toNode.setX(Float.valueOf(toX));
				toNode.setY(Float.valueOf(toY));
				toNode.setZ(0.0f);
				uiConcept2Node.put(to, toNode);
				node2UiConcept.put(toNode, to);
				directedGraph.addNode(toNode);
			}
			else
			{
				toNode = uiConcept2Node.get(to);
			}

			Edge edge = graphModel.factory().newEdge(fromNode, toNode, 0, true);
			edge.setWeight(0.1);
			edge.setLabel(from.getURI() + " --> " + to.getURI());
			directedGraph.addEdge(edge);
		}
		return node2UiConcept;
	}

	private static String determineNodeLabel(String uri, Model model)
	{
		Resource resource = model.getResource(uri);
		String label = StringUtils.EMPTY;
		if (model.listStatements(resource, AFOUtil.DCT_TITLE, (RDFNode) null).hasNext())
		{
			label = model.listStatements(resource, AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
		}

		if (model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).hasNext())
		{
			label = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getString();
		}

		if (model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).hasNext())
		{
			label = model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getString();
		}

		if (!label.isEmpty())
		{
			label = label.replaceAll("\"", "\\\\\"");
			return label;
		}

		return uri;
	}

	private static GraphModel determineEdgesToBreakCycles(DirectedGraph directedGraph, GraphModel graphModel)
	{
		Set<Edge> edgesToBreak = new HashSet<>();
		LinkedHashSet<Node> visited = new LinkedHashSet<>();
		BreakInfo breakInfo = new BreakInfo(visited, edgesToBreak);
		int numNodes = directedGraph.getNodes().toCollection().size();
		int nodeCounter = 1;
		for (Node startNode : directedGraph.getNodes())
		{
			log.info("start node (" + nodeCounter++ + "/" + numNodes + "): " + startNode.getLabel());
			visited = breakInfo.getVisited();
			visited.add(startNode);
			breakInfo.setVisitedNodes(visited);

			for (Edge outEdge : directedGraph.getOutEdges(startNode))
			{
				if (breakInfo.getEdgesToBreak().contains(outEdge))
				{
					log.info("skip broken edge: " + outEdge.getSource().getLabel() + "/" + outEdge.getSource().getId() + "->" + outEdge.getTarget().getLabel()
							+ "/" + outEdge.getTarget().getId());
					continue;
				}

				if (visited.contains(outEdge.getTarget()))
				{
					log.info("skip start from visited node: " + outEdge.getTarget().getLabel() + "/" + outEdge.getTarget().getId());
					continue;
				}

				breakInfo = follow(startNode, outEdge.getTarget(), directedGraph, breakInfo);
			}

			for (Edge inEdge : directedGraph.getInEdges(startNode))
			{
				if (breakInfo.getEdgesToBreak().contains(inEdge))
				{
					log.info("skip broken edge: " + inEdge.getSource().getLabel() + "/" + inEdge.getSource().getId() + "->" + inEdge.getTarget().getLabel()
							+ "/" + inEdge.getTarget().getId());
					continue;
				}

				if (visited.contains(inEdge.getSource()))
				{
					log.info("skip start from visited node: " + inEdge.getSource().getLabel() + "/" + inEdge.getSource().getId());
					continue;
				}

				breakInfo = follow(startNode, inEdge.getTarget(), directedGraph, breakInfo);
			}
		}

		graphModel = createGraphModelWithUpdatedDirectedGraph(directedGraph, edgesToBreak, graphModel);
		log.info("Found " + edgesToBreak.size() + " cycles.");
		return graphModel;
	}

	private static GraphModel createGraphModelWithUpdatedDirectedGraph(DirectedGraph directedGraph, Set<Edge> edgesToBreak, GraphModel graphModel)
	{
		if (edgesToBreak.size() > 0)
		{
			ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
			pc.newProject();
			Workspace workspace = pc.getCurrentWorkspace();
			Container container = Lookup.getDefault().lookup(Container.Factory.class).newContainer();
			ImportController importController = Lookup.getDefault().lookup(ImportController.class);
			importController.process(container, new DefaultProcessor(), workspace);
			GraphModel newGraphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
			DirectedGraph newDirectedGraph = newGraphModel.getDirectedGraph();
			for (Node node : directedGraph.getNodes())
			{
				Node newNode = newGraphModel.factory().newNode(node.getId());
				newNode.setLabel(node.getLabel());
				newNode.setSize(node.size());
				newNode.setX(node.x());
				newNode.setY(node.y());
				newNode.setZ(node.z());
				newDirectedGraph.addNode(newNode);
			}
			for (Edge edge : directedGraph.getEdges())
			{
				if (edgesToBreak.contains(edge))
				{
					continue;
				}

				Node fromNode = newDirectedGraph.getNode(edge.getSource().getId());
				Node toNode = newDirectedGraph.getNode(edge.getTarget().getId());
				Edge newEdge = newGraphModel.factory().newEdge(fromNode, toNode, 0, true);
				newEdge.setWeight(0.1);
				newEdge.setLabel(edge.getLabel());
				newDirectedGraph.addEdge(newEdge);
			}

			graphModel = newGraphModel;
		}
		return graphModel;
	}

	private static BreakInfo follow(Node startNode, Node currentNode, DirectedGraph directedGraph, BreakInfo breakInfo)
	{
		if (currentNode.equals(startNode))
		{
			return breakInfo;
		}

		LinkedHashSet<Node> visitedNodes = breakInfo.getVisited();
		Set<Edge> edgesToBreak = breakInfo.getEdgesToBreak();

		StringBuilder sb = new StringBuilder();
		if (log.isDebugEnabled())
		{
			sb.append("-> ");
			for (int i = 0; i < visitedNodes.size(); i++)
			{
				sb.append("    ");
			}
		}
		String indent = sb.toString();

		log.debug(indent + "(" + currentNode.getLabel() + "/" + currentNode.getId() + ") (" + printVisitedNodes(visitedNodes) + ")");
		for (Edge outEdge : directedGraph.getOutEdges(currentNode))
		{
			if (edgesToBreak.contains(outEdge))
			{
				log.info(indent + "skip broken edge: " + outEdge.getSource().getLabel() + "/" + outEdge.getSource().getId() + "->"
						+ outEdge.getTarget().getLabel() + "/" + outEdge.getTarget().getId());
				continue;
			}
			Node nextNode = outEdge.getTarget();
			log.debug(indent + "(" + currentNode.getLabel() + "/" + currentNode.getId() + ") OUT: " + nextNode.getLabel() + " ("
					+ printVisitedNodes(visitedNodes) + ")");

			if (startNode.equals(nextNode) && visitedNodes.size() >= 2 && !isFirstNeighbour(currentNode, visitedNodes))
			{
				log.info(indent + "Found cycle! Edge to break: " + startNode.getLabel() + "/" + startNode.getId() + " --> " + currentNode.getLabel() + "/"
						+ currentNode.getId());
				edgesToBreak.add(outEdge);
				breakInfo.setEdgesToBreak(edgesToBreak);
				continue;
			}

			if (visitedNodes.contains(nextNode) && isCycleToNodeOnPath(nextNode, visitedNodes))
			{
				log.info(indent + "Found cycle! Edge to break: " + currentNode.getLabel() + "/" + currentNode.getId() + " --> " + nextNode.getLabel() + "/"
						+ nextNode.getId());
				edgesToBreak.add(outEdge);
				breakInfo.setEdgesToBreak(edgesToBreak);
				continue;
			}

			if (visitedNodes.contains(nextNode))
			{
				continue;
			}

			visitedNodes.add(currentNode);
			breakInfo.setVisitedNodes(visitedNodes);

			breakInfo = follow(startNode, nextNode, directedGraph, breakInfo);
		}

		visitedNodes = breakInfo.getVisited();
		edgesToBreak = breakInfo.getEdgesToBreak();

		for (Edge inEdge : directedGraph.getInEdges(currentNode))
		{
			if (edgesToBreak.contains(inEdge))
			{
				log.info(indent + "skip broken edge: " + inEdge.getSource().getLabel() + "/" + inEdge.getSource().getId() + "->" + inEdge.getTarget().getLabel()
						+ "/" + inEdge.getTarget().getId());
				continue;
			}
			Node nextNode = inEdge.getSource();
			log.debug(indent + "(" + currentNode.getLabel() + "/" + currentNode.getId() + ") IN : " + nextNode.getLabel() + " ("
					+ printVisitedNodes(visitedNodes) + ")");

			if (startNode.equals(nextNode) && visitedNodes.size() >= 2 && !isFirstNeighbour(currentNode, visitedNodes))
			{
				log.info(indent + "Found cycle! Edge to break: " + startNode.getLabel() + "/" + startNode.getId() + " --> " + currentNode.getLabel() + "/"
						+ currentNode.getId());
				edgesToBreak.add(inEdge);
				breakInfo.setEdgesToBreak(edgesToBreak);
				continue;
			}

			if (visitedNodes.contains(nextNode) && isCycleToNodeOnPath(nextNode, visitedNodes))
			{
				log.info(indent + "Found cycle! Edge to break: " + currentNode.getLabel() + "/" + currentNode.getId() + " --> " + nextNode.getLabel() + "/"
						+ nextNode.getId());
				edgesToBreak.add(inEdge);
				breakInfo.setEdgesToBreak(edgesToBreak);
				continue;
			}

			if (visitedNodes.contains(nextNode))
			{
				continue;
			}

			visitedNodes.add(currentNode);
			breakInfo.setVisitedNodes(visitedNodes);

			breakInfo = follow(startNode, nextNode, directedGraph, breakInfo);
		}

		return breakInfo;
	}

	private static boolean isCycleToNodeOnPath(Node node, LinkedHashSet<Node> visitedNodes)
	{
		List<Node> nodesAsList = new ArrayList<>(visitedNodes);
		int currentPosition = visitedNodes.size();
		int nodePosition = nodesAsList.indexOf(node);
		if (currentPosition - nodePosition >= 4)
		{
			return true;
		}
		return false;
	}

	private static boolean isFirstNeighbour(Node currentNode, LinkedHashSet<Node> visitedNodes)
	{
		Iterator<Node> iterator = visitedNodes.iterator();
		if (!iterator.hasNext())
		{
			return false;
		}

		Node node = iterator.next();
		if (!iterator.hasNext())
		{
			return false;
		}

		node = iterator.next();
		if (node.equals(currentNode))
		{
			return true;
		}

		return false;
	}

	private static String printVisitedNodes(LinkedHashSet<Node> visitedNodes)
	{
		List<String> visitedNodeLabels = new ArrayList<>();
		for (Iterator<Node> iterator = visitedNodes.iterator(); iterator.hasNext();)
		{
			Node node = iterator.next();
			visitedNodeLabels.add(node.getLabel() + "/" + node.getId());
		}

		return StringUtils.join(visitedNodeLabels, " - ");
	}

	private static GraphModel doGraphVizLayout(GraphModel graphModel)
	{
		log.info(
				"SKIPPED AUTOMATIC LAYOUT. Layouting requires additional third-party dependencies. Please contact office@osthus.com for further support and information.");
		// log.info("Computing layout with graphviz: " + RdfCmap.graphVizAlgoName + ". Path to dot.exe: " + RdfCmap.dotBinary);
		// RdfCmapGraphvizLayout graphvizLayout = new RdfCmapGraphvizLayout(null, RdfCmap.graphVizAlgoName);
		// graphvizLayout.setGraphModel(graphModel);
		// graphvizLayout.setDotBinary(RdfCmap.dotBinary);
		//
		// graphvizLayout.initAlgo();
		// for (int i = 0; i < 100 && graphvizLayout.canAlgo(); i++)
		// {
		// graphvizLayout.goAlgo();
		// }
		// graphvizLayout.endAlgo();
		return graphModel;
	}

	private static GraphModel doAutoLayout(GraphModel graphModel)
	{
		log.info("Autolayout with YifanHu and force atlas for " + RdfCmap.layoutDuration + " seconds.");
		AutoLayout autoLayout = new AutoLayout(RdfCmap.layoutDuration, TimeUnit.SECONDS);
		autoLayout.setGraphModel(graphModel);
		YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
		ForceAtlasLayout secondLayout = new ForceAtlasLayout(null);
		ForceAtlasLayout thirdLayout = new ForceAtlasLayout(null);

		AutoLayout.DynamicProperty optimalDistanceProperty = AutoLayout.createDynamicProperty("YifanHu.optimalDistance.name", 1000.0f, 0f);
		AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.2f);
		AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", 500.0, 0f);
		AutoLayout.DynamicProperty strongerAdjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.9f);
		AutoLayout.DynamicProperty strongerRepulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", 10000.0, 0f);
		AutoLayout.DynamicProperty strongerAutoStabProperty = AutoLayout.createDynamicProperty("forceAtlas.freezeStrength.name", 1000.0, 0f);
		autoLayout.addLayout(firstLayout, 0.7f, new AutoLayout.DynamicProperty[] { optimalDistanceProperty });
		autoLayout.addLayout(secondLayout, 0.25f, new AutoLayout.DynamicProperty[] { adjustBySizeProperty, repulsionProperty });
		autoLayout.addLayout(thirdLayout, 0.05f,
				new AutoLayout.DynamicProperty[] { strongerAdjustBySizeProperty, strongerRepulsionProperty, strongerAutoStabProperty });
		autoLayout.execute();
		return graphModel;
	}

	private static GraphModel doRadialAxisLayout(GraphModel graphModel)
	{
		log.info("Computing radial axis layout.");
		// RadialAxisLayout radialAxisLayout = new RadialAxisLayout(new RadialAxisLayoutBuilder(), 100.0, false);
		// radialAxisLayout.resetPropertiesValues();
		// radialAxisLayout.setGraphModel(graphModel);
		// radialAxisLayout.setResizeNode(true);
		// radialAxisLayout.setSparSpiral(false);
		// radialAxisLayout.setSparNodePlacement("NodeId"); // Random
		// radialAxisLayout.setNodePlacementNoOverlap(true);
		// radialAxisLayout.setSparCount(100);
		//
		// radialAxisLayout.initAlgo();
		// for (int i = 0; i < 100 && radialAxisLayout.canAlgo(); i++)
		// {
		// radialAxisLayout.goAlgo();
		// }
		// radialAxisLayout.endAlgo();
		return graphModel;
	}

	private static GraphModel doCircleLayout(GraphModel graphModel)
	{
		log.info("Computing circle layout.");
		// CircleLayout circleLayout = new CircleLayout(new CircleLayoutBuilder(), 100.0, false);
		// circleLayout.resetPropertiesValues();
		// circleLayout.setGraphModel(graphModel);
		// circleLayout.initAlgo();
		// for (int i = 0; i < 100 && circleLayout.canAlgo(); i++)
		// {
		// circleLayout.goAlgo();
		// }
		// circleLayout.endAlgo();
		return graphModel;
	}
}
