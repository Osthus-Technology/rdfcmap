package com.osthus.rdfcmap.cmap.shape;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.cmap.Cmap2TurtleConverter;
import com.osthus.rdfcmap.cmap.cardinality.CardinalityEnum;
import com.osthus.rdfcmap.helper.ConceptRelation;
import com.osthus.rdfcmap.sparql.PathList;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.Prefixes;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * ShapesCreator based on AFO2.0
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ShapesCreator2
{
	private static final Logger log = LogManager.getLogger("Logger");

	Model shapesModel = ModelFactory.createDefaultModel();
	private static File outputFolder = new File("shapes");
	private static String rootShapeTitle = "rootShapeTitle";
	private Map<String, Integer> shapelabel2counter = new HashMap<String, Integer>();
	private static Map<Resource, ConceptRelation> link2conceptRelations = new HashMap<Resource, ConceptRelation>();

	public void create(Resource rootType, Path pathToInputFile, String[] additionalFiles)
			throws JAXBException, IOException, ParserConfigurationException, SAXException
	{
		if (RdfCmap.useNetworkShapeGraph)
		{
			log.info("Creating shapes from TTL: " + pathToInputFile.toString()
					+ ((additionalFiles != null && additionalFiles.length > 0) ? " using additional files: " + StringUtils.join(additionalFiles, ", ") : ""));

			Model model = ModelFactory.createDefaultModel();
			log.info("Reading model from file: " + pathToInputFile.toString());
			model.read(pathToInputFile.toUri().toString(), null, "TTL");
			log.info(model.listStatements().toList().size() + " triples total.");

			model = CmapUtil.addTriples(additionalFiles, model);
			if (model.isEmpty())
			{
				log.info("No RDF model found. Check input file.");
				System.exit(1);
			}
			else if (additionalFiles != null && additionalFiles.length > 0)
			{
				log.info(model.listStatements().toList().size() + " triples total.");
			}

			if (rootType == null)
			{
				throw new IllegalStateException("Missing root node for shapes creation. Use option --root");
			}

			link2conceptRelations = RdfUtil.determineConceptRelations(model); // model.write(System.out, "TTL")

			Resource root = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, rootType).next().getSubject();
			log.info("Found root: " + root.getURI());

			Set<String> visited = new LinkedHashSet<String>();
			visited.add(root.getURI());

			PathList pathList = new PathList(visited, false);

			rootShapeTitle = getShapesLabel(root, model, true);
			Resource rootShape = shapesModel.createResource(AFOUtil.AFS_PREFIX + rootShapeTitle);
			rootShape.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);

			PathAndModel pathAndModel = new PathAndModel(pathList, shapesModel, rootShape, 0);
			pathAndModel = findNeighbour(model, root, null, pathAndModel, true);

			rootShape = pathAndModel.getNodeShape();

			// add target classes
			Set<Resource> types = getTypes(root, model);
			for (Iterator<Resource> iterator = types.iterator(); iterator.hasNext();)
			{
				Resource type = iterator.next();
				rootShape.addProperty(AFOUtil.SHACL_TARGET_CLASS, type);
			}

			shapesModel.setNsPrefixes(Prefixes.nsPrefixMap);
			shapesModel.write(System.out, "TTL");

			writeShapes(model, pathToInputFile);
		}
		else
		{
			ShapesCreatorPath shapesCreatorPath = new ShapesCreatorPath();
			shapesCreatorPath.create(rootType, pathToInputFile, additionalFiles);
		}

	}

	private void writeShapes(Model model, Path pathToInputFile) throws IOException
	{
		if (Files.notExists(outputFolder.toPath()))
		{
			Files.createDirectory(outputFolder.toPath());
		}
		else
		{
			FileUtils.cleanDirectory(outputFolder);
		}

		String fileName = rootShapeTitle + ".ttl";
		Path path = Paths.get(outputFolder.getAbsolutePath() + "\\" + fileName);
		path = Files.createFile(path);
		shapesModel.write(new FileOutputStream(path.toFile()), "TTL");

		if (RdfCmap.humanReadable)
		{
			List<String> lines = Files.readAllLines(path, Charset.defaultCharset());

			lines = Cmap2TurtleConverter.addCommentsWithHumanReadableIds(lines, model);

			fileName = fileName.substring(0, fileName.length() - 4) + "-human-readable.ttl";
			path = Paths.get(outputFolder.getAbsolutePath() + "\\" + fileName);
			path = Files.createFile(path);

			Cmap2TurtleConverter.writeFile(path, lines);
		}

	}

	private Resource addTypeShapes(Resource resource, Resource resourceShape, Model model, Model shapesModel)
	{
		Set<Resource> types = getTypes(resource, model);
		for (Iterator<Resource> typeIterator = types.iterator(); typeIterator.hasNext();)
		{
			Resource type = typeIterator.next();

			if (RdfCmap.addRdfTypeShapeBasedOnShaclClass)
			{
				resourceShape.addProperty(AFOUtil.SHACL_CLASS, type);
			}
			else
			{
				resourceShape = addTypeShapesBasedOnRdfSubClassOfStar(resourceShape, model, shapesModel, type);
			}
		}

		return resourceShape;
	}

	private Resource addTypeShapesBasedOnRdfSubClassOfStar(Resource resourceShape, Model model, Model shapesModel, Resource type)
	{
		Resource propertyShape = shapesModel.createResource();
		propertyShape.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_PROPERTY_SHAPE);
		Resource zeroOrMoreSubClassOf = model.createResource().addProperty(AFOUtil.SHACL_ZERO_OR_MORE_PATH, AFOUtil.RDFS_SUBCLASS_OF);
		List<Resource> path = Arrays.asList(AFOUtil.RDF_TYPE, zeroOrMoreSubClassOf);
		Resource blankNodeList = shapesModel.createResource();
		Resource blankNodeRest = null;
		int i = 0;
		for (Iterator<Resource> iterator = path.iterator(); iterator.hasNext();)
		{
			Resource pathResource = iterator.next();

			Resource member = null;
			if (i == 0)
			{
				member = blankNodeList;
			}
			else
			{
				member = blankNodeRest;
			}

			if (iterator.hasNext())
			{
				blankNodeRest = shapesModel.createResource();
			}
			else
			{
				blankNodeRest = AFOUtil.RDF_NIL;
			}

			if (pathResource.isAnon())
			{
				List<Statement> blankNodeStatements = model.listStatements(pathResource, (Property) null, (RDFNode) null).toList();
				Resource blankListMember = shapesModel.createResource();
				for (Iterator<Statement> iterator2 = blankNodeStatements.iterator(); iterator2.hasNext();)
				{
					Statement statement = iterator2.next();
					blankListMember.addProperty(statement.getPredicate(), statement.getObject());
				}

				member.addProperty(AFOUtil.RDF_FIRST, blankListMember);
				member.addProperty(AFOUtil.RDF_REST, blankNodeRest);
			}
			else
			{
				member.addProperty(AFOUtil.RDF_FIRST, pathResource);
				member.addProperty(AFOUtil.RDF_REST, blankNodeRest);
			}

			i++;
		}

		propertyShape.addProperty(AFOUtil.SHACL_PATH, blankNodeList);

		// rdf types are required properties for shapes of entities
		propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT, ResourceFactory.createTypedLiteral("1", XSDDatatype.XSDinteger));

		propertyShape.addProperty(AFOUtil.SHACL_HAS_VALUE, type);
		resourceShape.addProperty(AFOUtil.SHACL_PROPERTY, propertyShape);
		return resourceShape;
	}

	private Set<Resource> getTypes(Resource resource, Model model)
	{
		Set<Resource> types = new HashSet<Resource>();
		StmtIterator stmtIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getObject().isAnon())
			{
				continue;
			}

			Resource type = statement.getObject().asResource();

			if (type.equals(AFOUtil.OWL_CLASS))
			{
				continue;
			}

			if (type.equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
			{
				continue;
			}

			types.add(type);
		}

		Set<Resource> parentTypes = new HashSet<Resource>();
		Set<Resource> typesToRemove = new HashSet<Resource>();
		for (Iterator<Resource> iterator = types.iterator(); iterator.hasNext();)
		{
			Resource type = iterator.next();
			if (type.getURI().startsWith(AFOUtil.QUDT_UNIT_PREFIX) || type.getURI().startsWith(AFOUtil.QUDT_UNIT_EXT_PREFIX))
			{
				// use parent class for external classes of units such as unit-ext:Milliliter -> qudt:VolumeUnit
				Resource parentType;
				StmtIterator typeIterator = model.listStatements(type, AFOUtil.RDF_TYPE, (RDFNode) null); // model.write(System.out,"TTL")
				while (typeIterator.hasNext())
				{
					Statement statement = typeIterator.next();
					if (statement.getObject().isAnon())
					{
						continue;
					}

					if (statement.getObject().asResource().equals(AFOUtil.OWL_CLASS))
					{
						continue;
					}

					if (statement.getObject().asResource().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
					{
						continue;
					}

					parentType = statement.getObject().asResource();
					parentTypes.add(parentType);
					typesToRemove.add(type);
				}
			}
		}

		if (!typesToRemove.isEmpty() && !parentTypes.isEmpty())
		{
			types.removeAll(typesToRemove);
			types.addAll(parentTypes);
		}

		return types;
	}

	/**
	 * Wlak along graph in forward direction and trace the path.
	 *
	 * @param model
	 * @param currentNode
	 * @param link
	 * @param pathAndModel
	 * @param isForwardLinked
	 *            Property is true if it is a step in forward direction based on a normal link (default). It is false if it is a step in forward direction based
	 *            on an inversely connected link (if false then we currently handle a node that is linked via inverse property to the graph. In this case we
	 *            only go one step backwards for shapes creation) .
	 * @return
	 */
	private PathAndModel findNeighbour(Model model, Resource currentNode, Property link, PathAndModel pathAndModel, boolean isForwardLinked)
	{
		// start at node
		// --iterate over all object properties
		// --get neighbour forward
		// -----if (not visited and not target) store as visited and get next neighbour forward
		// -----if (visited) get next neighbour forward
		// --if no neighbour found forward
		// -----get neighbour based on inverse relation pointing to node
		// --------if (not visited and not target) store as visited and get next neighbour inverse forward
		// --------if (visited) get next neighbour inverse forward
		// repeat until done with all connected nodes
		// include all other unvisited nodes and object properties (second version)
		int counter = pathAndModel.getCounter();
		counter += 2;
		String label = getResourceLabel(currentNode, model);
		String propertyLabel = StringUtils.EMPTY;
		if (link != null)
		{
			propertyLabel = getResourceLabel(link, model);
		}
		log.info(indent(counter) + propertyLabel + " " + label);

		Resource currentNodeShape = pathAndModel.getNodeShape();

		Set<String> visited = pathAndModel.getPathList().getPathList();

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

			if (statement.getPredicate().getURI().startsWith(VizUtil.AFV_PREFIX))
			{
				continue;
			}

			if (statement.getObject().isURIResource() && statement.getObject().asResource().getURI().startsWith(VizUtil.AFV_PREFIX))
			{
				continue;
			}

			if (statement.getObject().isResource() && statement.getObject().asResource().hasProperty(VizUtil.AFV_IS_LITERAL_NODE)
					&& statement.getObject().asResource().getProperty(VizUtil.AFV_IS_LITERAL_NODE).getBoolean())
			{
				// skip literal nodes
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

			if (isForwardLinked)
			{
				if (visited.contains(id))
				{
					continue;
				}
			}
			else if (visited.contains(id) && link.getURI().equals(statement.getPredicate().getURI()))
			{
				// current node is inversely linked and next node was already visited and iterated link of statement is equal to inverse link
				// this is first node after inverse link, now add shape for backwards direction for one single step
				// SHACL does not allow links from nested shapes to outer shapes, so create a new backwards shape instead
				isForwardLinked = true; // do this only once for an inverse relation

				Resource propertyShape = shapesModel.createResource();
				propertyShape.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_PROPERTY_SHAPE);
				propertyShape.addProperty(AFOUtil.SHACL_PATH, statement.getPredicate().asResource());

				propertyShape = addShapeCardinality(propertyShape, currentNode, statement.getPredicate(), nextNode, model, false);

				String shapeLabel = getShapesLabel(nextNode, model, false);
				Resource shapeResource;
				if (RdfCmap.useNamedShapes)
				{
					shapeResource = shapesModel.createResource(AFOUtil.AFS_PREFIX + shapeLabel);
				}
				else
				{
					shapeResource = shapesModel.createResource();
				}

				if (log.isDebugEnabled())
				{
					shapeResource.addProperty(AFOUtil.SHACL_DESCRIPTION, getShapesDescription(nextNode, id, model, false));
					shapeResource.addProperty(AFOUtil.SHACL_NAME, shapeLabel);
				}

				shapeResource.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);
				shapeResource = addTypeShapes(nextNode, shapeResource, model, shapesModel);

				propertyShape.addProperty(AFOUtil.SHACL_QUALIFIED_VALUE_SHAPE, shapeResource);
				currentNodeShape.addProperty(AFOUtil.SHACL_PROPERTY, propertyShape);
				continue;
			}

			visited.add(id);

			PathList pathList = pathAndModel.getPathList();
			pathList.setPathList(visited);
			pathAndModel.setPathList(pathList);
			pathAndModel.setCounter(counter);

			// now update shapesmodel for currentnode linking to nextnode
			Resource propertyShape = shapesModel.createResource();
			propertyShape.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_PROPERTY_SHAPE);
			propertyShape.addProperty(AFOUtil.SHACL_PATH, statement.getPredicate().asResource()); // model.write(System.out,"TTL")

			propertyShape = addShapeCardinality(propertyShape, currentNode, statement.getPredicate(), nextNode, model, false);

			String shapeLabel = getShapesLabel(nextNode, model, isForwardLinked);
			Resource shapeResource;
			if (RdfCmap.useNamedShapes)
			{
				shapeResource = shapesModel.createResource(AFOUtil.AFS_PREFIX + shapeLabel);
			}
			else
			{
				shapeResource = shapesModel.createResource();
			}

			if (log.isDebugEnabled())
			{
				shapeResource.addProperty(AFOUtil.SHACL_DESCRIPTION, getShapesDescription(nextNode, id, model, isForwardLinked));
				shapeResource.addProperty(AFOUtil.SHACL_NAME, shapeLabel);
			}
			shapeResource.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);

			pathAndModel.setNodeShape(shapeResource);
			pathAndModel = findNeighbour(model, nextNode, statement.getPredicate(), pathAndModel, true);
			Resource nextNodeShape = pathAndModel.getNodeShape();

			propertyShape.addProperty(AFOUtil.SHACL_QUALIFIED_VALUE_SHAPE, nextNodeShape);
			currentNodeShape.addProperty(AFOUtil.SHACL_PROPERTY, propertyShape);
		}
		// shapesModel.write(System.out,"TTL")
		// no forward link leads to target now check inverse links
		pathAndModel.setNodeShape(currentNodeShape);
		pathAndModel = findNeighbourInverse(model, currentNode, null, pathAndModel);

		currentNodeShape = addTypeShapes(currentNode, currentNodeShape, model, shapesModel);
		currentNodeShape = addLiteralShapes(model, currentNode, currentNodeShape);

		pathAndModel.setNodeShape(currentNodeShape);
		counter -= 2;
		pathAndModel.setCounter(counter);
		return pathAndModel;
	}

	private String getShapesDescription(Resource nextNode, String id, Model model, boolean isForwardLinked)
	{
		if (nextNode.isAnon())
		{
			if (isForwardLinked)
			{
				return id;
			}

			return id + "(inverse)";
		}

		if (nextNode.getURI().startsWith(CmapUtil.URN_UUID))
		{
			Resource uiNode = model.getResource(nextNode.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			if (model.listStatements(uiNode, VizUtil.AFV_IS_BLANK_NODE, (RDFNode) null).hasNext())
			{
				if (isForwardLinked)
				{
					return "[" + id + "]";
				}

				return "[" + id + "]" + "(inverse)";

			}
		}

		if (isForwardLinked)
		{
			return id;
		}

		return id + "(inverse)";
	}

	private Resource addShapeCardinality(Resource propertyShape, Resource resource, Property predicate, Resource neighbour, Model model,
			boolean isInverseRelation)
	{
		Resource cardinality = null;
		if (!isInverseRelation)
		{
			cardinality = determineCardinality(resource, predicate, neighbour, model);
		}
		else
		{
			cardinality = determineCardinality(neighbour, predicate, resource, model);
		}

		if (cardinality == null)
		{
			log.error("Cardinality not found for property: " + propertyShape.getProperty(AFOUtil.SHACL_PATH).getResource().toString() + " of resource "
					+ resource.getURI());
		}

		CardinalityEnum cardinalityEnum = getCardinalityType(cardinality, model);
		int minCardinality = 0;
		int maxCardinality = 0;
		if (cardinality != null)
		{
			if (cardinality.hasProperty(AFOUtil.AFX_MINIMUM_VALUE) && !cardinality.getProperty(AFOUtil.AFX_MINIMUM_VALUE).getString().isEmpty())
			{
				minCardinality = cardinality.getProperty(AFOUtil.AFX_MINIMUM_VALUE).getInt();
			}
			else
			{
				log.error("Missing min cardinality for " + resource.getURI() + " " + predicate.getURI() + " Assuming optional min 0.");
				cardinalityEnum = CardinalityEnum.MIN_0;
				minCardinality = 0;
			}
			if (cardinality.hasProperty(AFOUtil.AFX_MAXIMUM_VALUE) && !cardinality.getProperty(AFOUtil.AFX_MAXIMUM_VALUE).getString().isEmpty())
			{
				maxCardinality = cardinality.getProperty(AFOUtil.AFX_MAXIMUM_VALUE).getInt();
			}
			else
			{
				log.error("Missing max cardinality for " + resource.getURI() + " " + predicate.getURI() + " Assuming optional min 0.");
				cardinalityEnum = CardinalityEnum.MIN_0;
			}
		}

		if (!isInverseRelation)
		{
			switch (cardinalityEnum)
			{
				case EXACTLY:
					// means for literal properties: min 1 max 1
					// means for object properties: min 1, qualifiedMin 1, qualifiedMax 1 (but not max 1 because other links are allowed to exist)
					// means for object properties with single links such as qudt:unit: min 1, max 1, qualifiedMin 1, qualifiedMax 1 (because nothing else
					// allowed other than a Unit)
					if (isSingleLinkObjectProperty(predicate))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
						propertyShape.addLiteral(AFOUtil.SHACL_MAX_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(maxCardinality), XSDDatatype.XSDinteger));
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MAX_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(maxCardinality), XSDDatatype.XSDinteger));
					}
					else if (isObjectProperty(predicate, model))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MAX_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(maxCardinality), XSDDatatype.XSDinteger));
					}
					else
					{
						propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
						propertyShape.addLiteral(AFOUtil.SHACL_MAX_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(maxCardinality), XSDDatatype.XSDinteger));
					}
					break;
				case MIN:
					// means for literal properties: min X
					// means for object properties: min X, qualifiedMin X
					propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT,
							ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
					if (isObjectProperty(predicate, model))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
					}
					break;
				case MAX:
					// means for literal properties: max X
					// means for object properties: max X, qualifiedMax X
					propertyShape.addLiteral(AFOUtil.SHACL_MAX_COUNT,
							ResourceFactory.createTypedLiteral(String.valueOf(maxCardinality), XSDDatatype.XSDinteger));
					if (isObjectProperty(predicate, model))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MAX_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(maxCardinality), XSDDatatype.XSDinteger));
					}
					break;
				case MIN_0:
				default:
					// optional properties
					// means for literal properties: min 0
					// means for object properties: min 0, qualifiedMin 0
					propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT, ResourceFactory.createTypedLiteral(String.valueOf(0), XSDDatatype.XSDinteger));
					if (isObjectProperty(predicate, model))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(0), XSDDatatype.XSDinteger));
					}
					break;
			}
		}
		else
		{
			switch (cardinalityEnum)
			{
				case EXACTLY:
				case MIN:
				case MAX:
					propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT,
							ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
					if (isObjectProperty(predicate, model))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(minCardinality), XSDDatatype.XSDinteger));
					}
					break;
				case MIN_0:
				default:
					propertyShape.addLiteral(AFOUtil.SHACL_MIN_COUNT, ResourceFactory.createTypedLiteral(String.valueOf(0), XSDDatatype.XSDinteger));
					if (isObjectProperty(predicate, model))
					{
						propertyShape.addLiteral(AFOUtil.SHACL_QUALIFIED_MIN_COUNT,
								ResourceFactory.createTypedLiteral(String.valueOf(0), XSDDatatype.XSDinteger));
					}
					break;
			}
		}
		return propertyShape;
	}

	private boolean isSingleLinkObjectProperty(Property predicate)
	{
		if (AFOUtil.QUDT_UNIT.getURI().equals(predicate.getURI()))
		{
			return true;
		}

		// Todo: add further single link properties

		return false;
	}

	private boolean isObjectProperty(Property predicate, Model model)
	{
		return model.listStatements(predicate.asResource(), AFOUtil.RDF_TYPE, AFOUtil.OWL_OBJECT_PROPERTY).hasNext();
	}

	private CardinalityEnum getCardinalityType(Resource cardinality, Model model)
	{
		Resource cardinalityType = null;
		StmtIterator stmtIterator = model.listStatements(cardinality, AFOUtil.RDF_TYPE, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getObject().isAnon())
			{
				continue;
			}
			else if (statement.getObject().asResource().equals(AFOUtil.OWL_CLASS) || statement.getObject().asResource().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
			{
				continue;
			}

			cardinalityType = statement.getResource();
		}

		if (cardinalityType == null)
		{
			log.error("Cardinality undefined.");
			return CardinalityEnum.MIN_0;
		}

		if (VizUtil.AFV_MIN_CARDINALITY.equals(cardinalityType))
		{
			return CardinalityEnum.MIN;
		}

		if (VizUtil.AFV_EXACT_CARDINALITY.equals(cardinalityType))
		{
			return CardinalityEnum.EXACTLY;
		}

		if (VizUtil.AFV_MAX_CARDINALITY.equals(cardinalityType))
		{
			return CardinalityEnum.MAX;
		}

		log.error("Unhandled type of cardinality: " + cardinalityType.getURI() + " Assuming optional property.");
		return CardinalityEnum.MIN_0;
	}

	private Resource determineCardinality(Resource from, Property predicate, Resource to, Model model)
	{
		for (Entry<Resource, ConceptRelation> entry : link2conceptRelations.entrySet())
		{
			Resource link = entry.getKey();
			if (!isSameProperty(predicate, link, model))
			{
				continue;
			}
			ConceptRelation conceptRelation = entry.getValue();
			if (!from.getURI().equals(conceptRelation.from.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID)))
			{
				continue;
			}
			if (!to.getURI().equals(conceptRelation.to.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID)))
			{
				continue;
			}

			Resource cardinality = link.getProperty(VizUtil.AFV_HAS_CARDINALITY).getResource();
			return cardinality;
		}

		return null;
	}

	private boolean isSameProperty(Property predicate, Resource link, Model model)
	{
		Resource linkObject = model.getResource(link.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
		if (linkObject.hasProperty(AFOUtil.SKOS_RELATED) && predicate.getURI().equals(linkObject.getProperty(AFOUtil.SKOS_RELATED).getResource().getURI()))
		{
			return true;
		}

		return false;
	}

	private Resource addLiteralShapes(Model model, Resource node, Resource nodeShape)
	{
		StmtIterator nodeStmtIterator = model.listStatements(node, (Property) null, (RDFNode) null);
		while (nodeStmtIterator.hasNext())
		{
			Statement statement = nodeStmtIterator.next();
			if (!statement.getObject().isLiteral()
					&& !(statement.getObject().isResource() && statement.getObject().asResource().hasProperty(VizUtil.AFV_IS_LITERAL_NODE)
							&& statement.getObject().asResource().getProperty(VizUtil.AFV_IS_LITERAL_NODE).getBoolean()))
			{
				continue;
			}

			if (statement.getPredicate().getURI().startsWith(VizUtil.AFV_PREFIX))
			{
				continue;
			}

			Resource literalShape = shapesModel.createResource();
			literalShape.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_PROPERTY_SHAPE);
			literalShape.addProperty(AFOUtil.SHACL_PATH, statement.getPredicate().asResource());
			literalShape.addProperty(AFOUtil.SHACL_NODEKIND, AFOUtil.SHACL_LITERAL);
			String datatypeString = StringUtils.EMPTY;

			if (statement.getObject().isLiteral())
			{
				// literals that were directly attached to nodes (not via literal nodes in cmap)
				datatypeString = statement.getLiteral().getDatatypeURI();
				if (!statement.getPredicate().equals(AFOUtil.DCT_TITLE))
				{
					// directly attached literal properties are required with exact cardinality (if not modeled explicitly in cmap)
					literalShape.addLiteral(AFOUtil.SHACL_MIN_COUNT, ResourceFactory.createTypedLiteral("1", XSDDatatype.XSDinteger));
					literalShape.addLiteral(AFOUtil.SHACL_MAX_COUNT, ResourceFactory.createTypedLiteral("1", XSDDatatype.XSDinteger));
				}
				else
				{
					// take titles as optional properties for shapes of entities
					literalShape.addLiteral(AFOUtil.SHACL_MIN_COUNT, ResourceFactory.createTypedLiteral("0", XSDDatatype.XSDinteger));
				}
			}
			else
			{
				// literals nodes (explicitly modeled in cmap)
				datatypeString = statement.getObject().asResource().getProperty(AFOUtil.DCT_TITLE).getString();
				literalShape = addShapeCardinality(literalShape, node, statement.getPredicate(), statement.getObject().asResource(), model, false);
			}

			if (datatypeString.contains("string"))
			{
				literalShape.addProperty(AFOUtil.SHACL_DATATYPE, AFOUtil.XSD_STRING);
			}
			else if (datatypeString.contains("integer"))
			{
				literalShape.addProperty(AFOUtil.SHACL_DATATYPE, AFOUtil.XSD_INTEGER);
			}
			else if (datatypeString.contains("double"))
			{
				literalShape.addProperty(AFOUtil.SHACL_DATATYPE, AFOUtil.XSD_DOUBLE);
			}
			else if (datatypeString.contains("boolean"))
			{
				literalShape.addProperty(AFOUtil.SHACL_DATATYPE, AFOUtil.XSD_BOOLEAN);
			}
			else if (datatypeString.contains("dateTime"))
			{
				literalShape.addProperty(AFOUtil.SHACL_DATATYPE, AFOUtil.XSD_DATETIME);
			}
			else
			{
				log.error("Unknown literal datatype: " + datatypeString + " of resource: " + node.toString() + " Assuming string.");
				literalShape.addProperty(AFOUtil.SHACL_DATATYPE, AFOUtil.XSD_STRING);
			}

			nodeShape.addProperty(AFOUtil.SHACL_PROPERTY, literalShape);
		}

		return nodeShape;
	}

	private String getShapesLabel(Resource resource, Model model, boolean isForwardLinked)
	{
		String label = getResourceLabel(resource, model);
		label = label.split(":")[1];
		label = label.replaceAll("\\s", "-").replaceAll("\\(", "-").replaceAll("\\)", "-").replaceAll("\\[", "").replaceAll("\\]", "");
		label = WordUtils.capitalizeFully(label, '-').replaceAll("\\-", "");
		label = label + "Shape";
		if (!isForwardLinked)
		{
			label = label + "Inverse";
		}

		if (RdfCmap.useNamedShapes)
		{
			int counter = 0;
			if (shapelabel2counter.containsKey(label))
			{
				counter = shapelabel2counter.get(label);
			}
			if (counter > 0)
			{
				label = label + "_" + counter;
			}
			shapelabel2counter.put(label, counter++);
		}

		return label;
	}

	private PathAndModel findNeighbourInverse(Model model, Resource currentNode, Property link, PathAndModel pathAndModel)
	{
		int counter = pathAndModel.getCounter();
		counter += 2;
		String label = getResourceLabel(currentNode, model);
		String propertyLabel = StringUtils.EMPTY;
		if (link != null)
		{
			propertyLabel = getResourceLabel(link, model);
		}
		log.info(indent(counter) + "^" + propertyLabel + " " + label);

		Resource currentNodeShape = pathAndModel.getNodeShape();

		Set<String> visited = pathAndModel.getPathList().getPathList();
		StmtIterator reverseNodeStmtIterator = model.listStatements((Resource) null, (Property) null, currentNode);
		while (reverseNodeStmtIterator.hasNext())
		{
			Statement statement = reverseNodeStmtIterator.next();

			if (statement.getPredicate().equals(AFOUtil.RDF_TYPE))
			{
				continue;
			}

			if (statement.getPredicate().getURI().startsWith(VizUtil.AFV_PREFIX))
			{
				continue;
			}

			if (statement.getSubject().isURIResource() && statement.getSubject().asResource().getURI().startsWith(VizUtil.AFV_PREFIX))
			{
				continue;
			}

			if (statement.getSubject().hasProperty(VizUtil.AFV_IS_LITERAL_NODE) && statement.getSubject().getProperty(VizUtil.AFV_IS_LITERAL_NODE).getBoolean())
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

			visited.add(id);
			PathList pathList = pathAndModel.getPathList();
			pathList.setPathList(visited);
			pathAndModel.setPathList(pathList);
			pathAndModel.setCounter(counter);

			// now update shapesmodel for currentnode ^linking to nextnode

			Resource propertyShape = shapesModel.createResource();
			propertyShape.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_PROPERTY_SHAPE);

			// inversepath is modeled like: ex:propertyshape sh:path [ sh:inversePath ex:property ] .
			Resource inversePathResource = shapesModel.createResource();
			inversePathResource.addProperty(AFOUtil.SHACL_INVERSE_PATH, statement.getPredicate());
			propertyShape.addProperty(AFOUtil.SHACL_PATH, inversePathResource);

			propertyShape = addShapeCardinality(propertyShape, currentNode, statement.getPredicate(), nextNode, model, true);

			String shapeLabel = getShapesLabel(nextNode, model, false);
			Resource shapeResource;
			if (RdfCmap.useNamedShapes)
			{
				shapeResource = shapesModel.createResource(AFOUtil.AFS_PREFIX + shapeLabel);
			}
			else
			{
				shapeResource = shapesModel.createResource();
			}

			if (log.isDebugEnabled())
			{
				shapeResource.addProperty(AFOUtil.SHACL_DESCRIPTION, getShapesDescription(nextNode, id, model, false));
				shapeResource.addProperty(AFOUtil.SHACL_NAME, shapeLabel);
			}
			shapeResource.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);

			pathAndModel.setNodeShape(shapeResource);
			pathAndModel = findNeighbour(model, nextNode, statement.getPredicate(), pathAndModel, false);
			Resource nextNodeShape = pathAndModel.getNodeShape();

			propertyShape.addProperty(AFOUtil.SHACL_QUALIFIED_VALUE_SHAPE, nextNodeShape);
			currentNodeShape.addProperty(AFOUtil.SHACL_PROPERTY, propertyShape);
		}

		pathAndModel.setNodeShape(currentNodeShape);
		counter -= 2;
		pathAndModel.setCounter(counter);
		return pathAndModel;
	}

	private String indent(int counter)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= counter; i++)
		{
			sb.append(" ");
		}

		return sb.toString();
	}

	private String getResourceLabel(Resource resource, Model model)
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

				if (model.contains(object, AFOUtil.SKOS_PREF_LABEL))
				{
					resourceLabel = model.listStatements(object, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getString();
					break;
				}
				else if (model.contains(object, AFOUtil.RDFS_LABEL))
				{
					resourceLabel = model.listStatements(object, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getString();
					break;
				}
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
		else
		{
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
		}

		return resourceLabel;
	}

}
