package com.osthus.rdfcmap.cmap.shape;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.cmap.Cmap2TurtleConverter;
import com.osthus.rdfcmap.cmap.Turtle2CmapConverter;
import com.osthus.rdfcmap.enums.ConceptProperty;
import com.osthus.rdfcmap.helper.ConceptRelation;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * ShapesCreator based on AFT1.1.5
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class ShapesCreator1_1_5
{
	private static final Logger log = LogManager.getLogger("Logger");

	private static File outputFolder = new File("shapes");

	public static Map<Resource, ConceptRelation> link2conceptRelations = new HashMap<Resource, ConceptRelation>();

	public static Model modelWithShapes = null;

	public static Model mappingModel = null;

	private Map<String, NodeShape> uuid2Shape = new HashMap<String, NodeShape>();

	private Turtle2CmapConverter turtle2CmapConverter = new Turtle2CmapConverter();
	private Cmap2TurtleConverter cmap2TurtleConverter = new Cmap2TurtleConverter();

	public void create(Path pathToInputFile, String[] additionalFiles) throws JAXBException, IOException, ParserConfigurationException, SAXException
	{
		log.info("Creating shapes from TTL: " + pathToInputFile.toString()
				+ ((additionalFiles != null && additionalFiles.length > 0) ? " using additional files: " + StringUtils.join(additionalFiles, ", ") : ""));

		Model model = ModelFactory.createDefaultModel();
		mappingModel = ModelFactory.createDefaultModel();
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

		createShapes(model);

		exportShapes(model, pathToInputFile);

		exportMapping(model);
	}

	private void createShapes(Model model)
	{
		log.info("Creating shapes.");

		collectNodeShapes(model);

		link2conceptRelations = RdfUtil.determineConceptRelations(model);

		determineShapes(model);
	}

	private void exportShapes(Model model, Path pathToInputFile) throws IOException
	{
		if (Files.notExists(outputFolder.toPath()))
		{
			Files.createDirectory(outputFolder.toPath());
		}
		else
		{
			FileUtils.cleanDirectory(outputFolder);
		}

		determineShapeTitles();

		Model allShapesModel = ModelFactory.createDefaultModel();
		for (Entry<String, NodeShape> entry : uuid2Shape.entrySet())
		{
			NodeShape nodeShape = entry.getValue();

			Model shapeModel = createModelFromNodeShape(nodeShape, AFOUtil.AFS_PREFIX + nodeShape.getTitle(), model);

			String fileName = nodeShape.getTitle() + ".ttl";
			Path path = Paths.get(outputFolder.getAbsolutePath() + "\\" + fileName);
			path = Files.createFile(path);
			shapeModel.write(new FileOutputStream(path.toFile()), "TTL");

			allShapesModel.add(shapeModel);
		}

		model.add(allShapesModel);
		modelWithShapes = model;
	}

	private void exportMapping(Model model) throws IOException, FileNotFoundException
	{
		Path path = Paths.get(outputFolder.getAbsolutePath() + "\\" + "mapping.ttl");
		path = Files.createFile(path);
		mappingModel.setNsPrefixes(model.getNsPrefixMap());
		mappingModel.write(new FileOutputStream(path.toFile()), "TTL");
	}

	private void determineShapeTitles()
	{
		Map<String, NodeShape> tempMap = new HashMap<String, NodeShape>();
		List<String> titles = new ArrayList<String>();
		for (Entry<String, NodeShape> entry : uuid2Shape.entrySet())
		{
			NodeShape nodeShape = entry.getValue();

			String title = cmap2TurtleConverter.unbreakString(nodeShape.getName());
			title = title.replaceAll("\\W+", " ").trim();
			title = title.toLowerCase().replaceAll("\\s", "-");
			if (title.isEmpty() || title.equals("-"))
			{
				title = "unlabeled";
			}

			titles.add(title);
			int numTitles = 0;
			for (Iterator<String> iterator = titles.iterator(); iterator.hasNext();)
			{
				String string = iterator.next();
				if (string.equals(title))
				{
					numTitles++;
				}
			}

			title = WordUtils.capitalizeFully(title, '-').replaceAll("\\-", "");

			title = title + "Shape";
			if (numTitles > 1)
			{
				title = title + "-" + numTitles;
			}

			nodeShape.setTitle(title);
			tempMap.put(entry.getKey(), nodeShape);
		}
		for (Entry<String, NodeShape> entry : tempMap.entrySet())
		{
			uuid2Shape.put(entry.getKey(), entry.getValue());
		}

		updateNestedShapes();
	}

	private Model createModelFromNodeShape(NodeShape nodeShape, String iri, Model model)
	{
		Model shapeModel = ModelFactory.createDefaultModel();
		shapeModel.setNsPrefixes(RdfUtil.prefixMap);
		Resource shapeResource = shapeModel.createResource(iri);

		shapeResource.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);

		if (nodeShape.getName() != null)
		{
			shapeResource.addLiteral(AFOUtil.SHACL_NAME, nodeShape.getName());
		}

		if (nodeShape.getDescription() != null)
		{
			shapeResource.addLiteral(AFOUtil.SHACL_DESCRIPTION, nodeShape.getDescription());
		}

		if (nodeShape.getIsClosed() != null)
		{
			shapeResource.addLiteral(AFOUtil.SHACL_CLOSED, true);
		}

		if (nodeShape.getIgnoredProperties() != null && !nodeShape.getIgnoredProperties().isEmpty())
		{
			RDFNode[] ignoredProperties = new RDFNode[nodeShape.getIgnoredProperties().size()];
			int i = 0;
			for (Iterator<Resource> iterator = nodeShape.getIgnoredProperties().iterator(); iterator.hasNext();)
			{
				Resource resource = iterator.next();
				ignoredProperties[i] = resource;
				i++;
			}
			RDFList list = shapeModel.createList(ignoredProperties);
			shapeResource.addProperty(AFOUtil.SHACL_IGNORED_PROPERTIES, list);
		}

		addMappings(nodeShape, shapeResource);

		if (nodeShape.getTargetNode() != null && !nodeShape.getTargetNode().isEmpty())
		{
			for (Iterator<Resource> iterator = nodeShape.getTargetNode().iterator(); iterator.hasNext();)
			{
				Resource resource = iterator.next();
				shapeResource.addProperty(AFOUtil.SHACL_TARGET_NODE, resource);
			}
		}

		if (nodeShape.getProperty() != null && !nodeShape.getProperty().isEmpty())
		{
			for (PropertyShape propertyShape : nodeShape.getProperty())
			{
				Resource blankNode = shapeModel.createResource();
				blankNode.addProperty(AFOUtil.RDF_TYPE, AFOUtil.SHACL_PROPERTY_SHAPE);
				if (propertyShape.getMinCount() != null)
				{
					blankNode.addLiteral(AFOUtil.SHACL_MIN_COUNT, propertyShape.getMinCount());
				}
				if (propertyShape.getMaxCount() != null)
				{
					blankNode.addLiteral(AFOUtil.SHACL_MAX_COUNT, propertyShape.getMaxCount());
				}

				if (propertyShape.getPath() != null && !propertyShape.getPath().isEmpty())
				{
					if (propertyShape.getPath().size() == 1)
					{
						Resource property = propertyShape.getPath().get(0);
						if (!property.isAnon() && property.getURI().startsWith(VizUtil.AFV_PREFIX))
						{
							Resource propertyInstance = model.createResource(property.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
							property = getPropertyForPropertyInstance(model, propertyInstance);
							blankNode.addProperty(AFOUtil.SHACL_PATH, property);
						}
						else if (property.isAnon())
						{
							Resource blankNodeProperty = model.createResource();
							List<Statement> blankNodeStatements = model.listStatements(property, (Property) null, (RDFNode) null).toList();
							for (Iterator iterator = blankNodeStatements.iterator(); iterator.hasNext();)
							{
								Statement statement = (Statement) iterator.next();
								blankNodeProperty.addProperty(statement.getPredicate(), statement.getObject());
							}
							blankNode.addProperty(AFOUtil.SHACL_PATH, blankNodeProperty);
						}
						else
						{
							blankNode.addProperty(AFOUtil.SHACL_PATH, property);
						}
					}
					else
					{
						Resource blankNodeList = shapeModel.createResource();
						Resource blankNodeRest = null;
						int i = 0;
						for (Iterator<Resource> iterator = propertyShape.getPath().iterator(); iterator.hasNext();)
						{
							Resource resource = iterator.next();

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
								blankNodeRest = shapeModel.createResource();
							}
							else
							{
								blankNodeRest = AFOUtil.RDF_NIL;
							}

							if (resource.isAnon())
							{
								List<Statement> blankNodeStatements = model.listStatements(resource, (Property) null, (RDFNode) null).toList();
								Resource blankListMember = shapeModel.createResource();
								for (Iterator iterator2 = blankNodeStatements.iterator(); iterator2.hasNext();)
								{
									Statement statement = (Statement) iterator2.next();
									blankListMember.addProperty(statement.getPredicate(), statement.getObject());
								}

								member.addProperty(AFOUtil.RDF_FIRST, blankListMember);
								member.addProperty(AFOUtil.RDF_REST, blankNodeRest);
							}
							else if (resource.getURI().startsWith(VizUtil.AFV_PREFIX))
							{
								Resource propertyInstance = shapeModel.createResource(resource.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
								resource = getPropertyForPropertyInstance(model, propertyInstance);
								member.addProperty(AFOUtil.RDF_FIRST, resource);
								member.addProperty(AFOUtil.RDF_REST, blankNodeRest);
							}
							else
							{
								member.addProperty(AFOUtil.RDF_FIRST, resource);
								member.addProperty(AFOUtil.RDF_REST, blankNodeRest);
							}

							i++;
						}
						blankNode.addProperty(AFOUtil.SHACL_PATH, blankNodeList);
					}
				}

				if (propertyShape.getNodeKind() != null)
				{
					blankNode.addProperty(AFOUtil.SHACL_NODEKIND, propertyShape.getNodeKind());
				}

				if (propertyShape.getDataType() != null)
				{
					blankNode.addProperty(AFOUtil.SHACL_DATATYPE, propertyShape.getDataType());
				}

				if (propertyShape.getNode() != null)
				{
					blankNode.addProperty(AFOUtil.SHACL_NODE, shapeModel.createResource(AFOUtil.AFS_PREFIX + propertyShape.getNode().getTitle()));
				}

				if (propertyShape.getShapeClass() != null && !propertyShape.getShapeClass().isEmpty())
				{
					for (Iterator<Resource> iterator = propertyShape.getShapeClass().iterator(); iterator.hasNext();)
					{
						Resource resource = iterator.next();
						blankNode.addProperty(AFOUtil.SHACL_CLASS, resource);
					}
				}

				if (propertyShape.getInAllowedValues() != null && !propertyShape.getInAllowedValues().isEmpty())
				{
					RDFNode[] allowedValues = new RDFNode[propertyShape.getInAllowedValues().size()];
					int i = 0;
					for (Iterator<Resource> iterator = propertyShape.getInAllowedValues().iterator(); iterator.hasNext();)
					{
						Resource resource = iterator.next();
						allowedValues[i] = resource;
						i++;
					}
					RDFList list = shapeModel.createList(allowedValues);
					blankNode.addProperty(AFOUtil.SHACL_IN, list);
				}

				if (propertyShape.getHasValue() != null)
				{
					blankNode.addProperty(AFOUtil.SHACL_HAS_VALUE, propertyShape.getHasValue());
				}

				if (propertyShape.getPattern() != null)
				{
					blankNode.addProperty(AFOUtil.SHACL_PATTERN, propertyShape.getPattern());
				}

				shapeResource.addProperty(AFOUtil.SHACL_PROPERTY, blankNode);
			}
		}

		return shapeModel;
	}

	private void addMappings(NodeShape nodeShape, Resource shapeResource)
	{
		if (nodeShape.getTargetClass() != null && !nodeShape.getTargetClass().isEmpty())
		{
			Resource mappingShapeResource = mappingModel.createResource(shapeResource.getURI());
			for (Iterator<Resource> iterator = nodeShape.getTargetClass().iterator(); iterator.hasNext();)
			{
				Resource resource = iterator.next();
				mappingShapeResource.addProperty(AFOUtil.SHACL_TARGET_CLASS, resource);
			}
		}

		if (nodeShape.getTargetNode() != null && !nodeShape.getTargetNode().isEmpty())
		{
			Resource mappingShapeResource = mappingModel.createResource(shapeResource.getURI());
			for (Iterator<Resource> iterator = nodeShape.getTargetNode().iterator(); iterator.hasNext();)
			{
				Resource resource = iterator.next();
				mappingShapeResource.addProperty(AFOUtil.SHACL_TARGET_NODE, resource);
			}
		}
	}

	private void determineShapes(Model model)
	{
		Map<String, NodeShape> tempMap = new HashMap<String, NodeShape>();
		for (Entry<String, NodeShape> entry : uuid2Shape.entrySet())
		{
			Resource instance = model.getResource(entry.getKey());
			NodeShape nodeShape = entry.getValue();
			nodeShape.setName(determineName(model, instance));
			nodeShape.setTargetNode(Arrays.asList(instance));

			Resource uiConcept = model.getResource(entry.getKey().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			StmtIterator hiddenPropertyIterator = model.listStatements(uiConcept, VizUtil.AFV_HAS_HIDDEN_PROPERTY, (RDFNode) null);
			while (hiddenPropertyIterator.hasNext())
			{
				Statement hiddenPropertyStatement = hiddenPropertyIterator.next();
				Resource hiddenProperty = hiddenPropertyStatement.getObject().asResource();
				PropertyShape propertyShape = new PropertyShape();
				Resource propertyAsResource = model.listStatements(hiddenProperty, AFOUtil.AFX_HAS_PROPERTY, (RDFNode) null).next().getObject().asResource();
				propertyShape.setPath(Arrays.asList(propertyAsResource));
				String propertyName = model.listStatements(hiddenProperty, AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
				propertyShape.setName(propertyName);
				Resource cardinality = model.listStatements(hiddenProperty, VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).next().getObject().asResource();

				if (model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).hasNext()
						&& !model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).next().getString().isEmpty())
				{
					propertyShape.setMinCount(model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).next().getInt());
				}

				if (model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).hasNext()
						&& !model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).next().getString().isEmpty())
				{
					propertyShape.setMaxCount(model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).next().getInt());
				}

				propertyShape = determineNodeAndDataType(model, instance, propertyShape, model.getProperty(propertyAsResource.getURI()), null);

				nodeShape = createOrUpdatePropertyShapes(nodeShape, propertyShape);
			}

			for (Entry<Resource, ConceptRelation> linkEntry : link2conceptRelations.entrySet())
			{
				Resource link = linkEntry.getKey();
				ConceptRelation conceptRelation = linkEntry.getValue();
				if (!conceptRelation.from.getURI().equals(uiConcept.getURI()))
				{
					continue;
				}

				PropertyShape propertyShape = new PropertyShape();
				propertyShape.setPath(Arrays.asList(getPropertyForLink(model, model.getProperty(link.getURI()))));
				String propertyName = model.listStatements(link, AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
				propertyShape.setName(propertyName);

				StmtIterator cardinalityOfLinkIterator = model.listStatements(link, VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null);

				if (!cardinalityOfLinkIterator.hasNext())
				{
					propertyShape.setMinCount(0);
				}

				while (cardinalityOfLinkIterator.hasNext())
				{
					Statement cardinalityStatement = cardinalityOfLinkIterator.next();
					Resource cardinality = cardinalityStatement.getObject().asResource();

					if (model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).hasNext()
							&& !model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).next().getString().isEmpty())
					{
						propertyShape.setMinCount(model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).next().getInt());
					}

					if (model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).hasNext()
							&& !model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).next().getString().isEmpty())
					{
						propertyShape.setMaxCount(model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).next().getInt());
					}

					propertyShape = determineNodeAndDataType(model, instance, propertyShape, model.getProperty(link.getURI()), conceptRelation.to);

					nodeShape = createOrUpdatePropertyShapes(nodeShape, propertyShape);
				}
			}

			Set<Resource> rdfTypes = new HashSet<>();
			StmtIterator stmtIterator = model.listStatements(instance, (Property) null, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				Resource propertyAsResource = statement.getPredicate().asResource();
				if (isPropertyIncluded(nodeShape, propertyAsResource))
				{
					continue;
				}

				if (propertyAsResource.equals(AFOUtil.RDF_TYPE))
				{
					rdfTypes.add(statement.getResource());
					continue;
				}
				PropertyShape propertyShape = new PropertyShape();
				propertyShape.setPath(Arrays.asList(propertyAsResource));
				propertyShape.setName(determineName(model, propertyAsResource));
				propertyShape.setMinCount(0);
				propertyShape = determineNodeAndDataType(model, instance, propertyShape, model.getProperty(propertyAsResource.getURI()), null);

				nodeShape = createOrUpdatePropertyShapes(nodeShape, propertyShape);
			}

			// handle rdf:type separately
			if (!rdfTypes.isEmpty())
			{
				// rdf:type is modeled as a shape using rdf:type/rdfs:subClassOf* <the-required-type>
				// sh:property [
				// sh:path ( rdf:type [sh:zeroOrMorePath rdfs:subClassOf ] );
				// sh:hasValue <the-required-type> ;
				// ]
				for (Iterator<Resource> iterator = rdfTypes.iterator(); iterator.hasNext();)
				{
					Resource rdfType = iterator.next();

					PropertyShape propertyShape = new PropertyShape();
					Resource zeroOrMoreSubClassOf = model.createResource().addProperty(AFOUtil.SHACL_ZERO_OR_MORE_PATH, AFOUtil.RDFS_SUBCLASS_OF);
					propertyShape.setPath(Arrays.asList(AFOUtil.RDF_TYPE, zeroOrMoreSubClassOf));
					propertyShape.setName("rdf:type/rdfs:subClassOf*");
					propertyShape.setMinCount(1);
					propertyShape.setNodeKind(AFOUtil.SHACL_IRI);
					propertyShape.setHasValue(rdfType);

					nodeShape = createOrUpdatePropertyShapes(nodeShape, propertyShape);
				}
			}
			else
			{
				log.info("Found resource without rdf:type specified: " + instance.getURI());
			}

			tempMap.put(entry.getKey(), nodeShape);

		}

		for (Entry<String, NodeShape> entry : tempMap.entrySet())
		{
			uuid2Shape.put(entry.getKey(), entry.getValue());
		}

		updateNestedShapes();
	}

	private NodeShape createOrUpdatePropertyShapes(NodeShape nodeShape, PropertyShape propertyShape)
	{
		List<PropertyShape> propertyShapes = nodeShape.getProperty();
		if (propertyShapes == null)
		{
			propertyShapes = new ArrayList<PropertyShape>();
		}
		propertyShapes.add(propertyShape);

		nodeShape.setProperty(propertyShapes);

		return nodeShape;
	}

	private void updateNestedShapes()
	{
		Map<String, NodeShape> tempMap = new HashMap<String, NodeShape>();
		for (Entry<String, NodeShape> entry : uuid2Shape.entrySet())
		{
			NodeShape nodeShape = entry.getValue();
			List<PropertyShape> updatedPropertyShapes = new ArrayList<>();
			for (PropertyShape propertyShape : nodeShape.getProperty())
			{
				if (propertyShape.getNode() == null)
				{
					updatedPropertyShapes.add(propertyShape);
					continue;
				}

				NodeShape nestedNodeShape = propertyShape.getNode();
				propertyShape.setNode(uuid2Shape.get(nestedNodeShape.getTargetNode().get(0).getURI()));
				updatedPropertyShapes.add(propertyShape);
			}
			nodeShape.setProperty(updatedPropertyShapes);

			tempMap.put(entry.getKey(), nodeShape);
		}

		for (Entry<String, NodeShape> entry : tempMap.entrySet())
		{
			uuid2Shape.put(entry.getKey(), entry.getValue());
		}
	}

	private String determineName(Model model, Resource resource)
	{
		Map<String, String> properties = new HashMap<>();
		properties = turtle2CmapConverter.createTitle(model, resource, properties);
		String propertyName = properties.get(ConceptProperty.TITLE.name());
		propertyName = cmap2TurtleConverter.unbreakString(propertyName);
		return propertyName.trim();
	}

	private boolean isPropertyIncluded(NodeShape nodeShape, Resource propertyAsResource)
	{
		if (nodeShape.getProperty() == null)
		{
			return false;
		}

		for (PropertyShape propertyShape : nodeShape.getProperty())
		{
			if (propertyShape.getPath() == null)
			{
				continue;
			}

			if (propertyShape.getPath().contains(propertyAsResource))
			{
				return true;
			}
		}

		return false;
	}

	private PropertyShape determineNodeAndDataType(Model model, Resource instance, PropertyShape propertyShape, Property property, Resource targetNode)
	{
		property = getPropertyForLink(model, property);

		RDFNode objectNode = model.listStatements(instance, property, (RDFNode) null).next().getObject();
		if (objectNode.isLiteral())
		{
			Integer propertyValueAsInteger = null;
			Float propertyValueAsFloat = null;
			String propertyValueAsString = null;

			try
			{
				propertyValueAsInteger = model.listStatements(instance, property, (RDFNode) null).next().getInt();
			}
			catch (Exception e)
			{
				// expected exception
			}

			if (propertyValueAsInteger == null)
			{
				try
				{
					propertyValueAsFloat = model.listStatements(instance, property, (RDFNode) null).next().getFloat();
				}
				catch (Exception e)
				{
					// expected exception
				}
			}

			if (propertyValueAsInteger == null && propertyValueAsFloat == null)
			{
				try
				{
					propertyValueAsString = model.listStatements(instance, property, (RDFNode) null).next().getString();
				}
				catch (Exception e)
				{
					// expected exception
				}
			}

			if (propertyValueAsInteger == null && propertyValueAsFloat == null && propertyValueAsString == null)
			{
				throw new IllegalStateException("Could not parse property value of statement: " + instance.getURI() + " " + property.getURI() + " "
						+ model.listStatements(instance, model.getProperty(property.getURI()), (RDFNode) null).next().getObject().toString());
			}

			if (propertyValueAsInteger != null)
			{
				propertyShape.setDataType(AFOUtil.XSD_INTEGER);
			}
			else if (propertyValueAsFloat != null)
			{
				propertyShape.setDataType(AFOUtil.XSD_DOUBLE);
			}
			else
			{
				propertyShape.setDataType(AFOUtil.XSD_STRING);
			}

			if (propertyValueAsInteger != null || propertyValueAsFloat != null || propertyValueAsString != null)
			{
				propertyShape.setNodeKind(AFOUtil.SHACL_LITERAL);
			}
		}
		else if (objectNode.isURIResource() && !objectNode.asResource().getURI().startsWith(CmapUtil.URN_UUID))
		{
			propertyShape.setNodeKind(AFOUtil.SHACL_IRI);
		}
		else if (objectNode.isURIResource() && objectNode.asResource().getURI().startsWith(CmapUtil.URN_UUID))
		{
			propertyShape.setNodeKind(AFOUtil.SHACL_IRI);
			if (targetNode == null)
			{
				propertyShape.setNode(uuid2Shape.get(objectNode.asResource().getURI()));
			}
			else
			{
				propertyShape.setNode(uuid2Shape.get(targetNode.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID)));
			}
		}
		else if (objectNode.isAnon())
		{
			propertyShape.setNodeKind(AFOUtil.SHACL_BLANK_NODE);
		}
		return propertyShape;
	}

	private Property getPropertyForLink(Model model, Property property)
	{
		if (property.getURI().startsWith(VizUtil.AFV_PREFIX))
		{
			Resource propertyInstance = model.createResource(property.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));

			property = getPropertyForPropertyInstance(model, propertyInstance);
		}
		return property;
	}

	private Property getPropertyForPropertyInstance(Model model, Resource propertyInstance)
	{
		Property property;
		if (model.listStatements(propertyInstance, AFOUtil.SKOS_RELATED, (RDFNode) null).hasNext())
		{
			property = model
					.createProperty(model.listStatements(propertyInstance, AFOUtil.SKOS_RELATED, (RDFNode) null).next().getObject().asResource().getURI());
		}
		else
		{
			String propertyString = model.listStatements(propertyInstance, AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
			propertyString = CmapUtil.replacePrefixesWithNamespaces(model, Arrays.asList(propertyString)).get(0);
			propertyString = propertyString.substring(1, propertyString.length() - 1); // cut < >
			property = model.createProperty(propertyString);
		}
		return property;
	}

	private void collectNodeShapes(Model model)
	{
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (subject.isAnon())
			{
				continue;
			}

			Resource instance = model.listStatements(subject, AFOUtil.AFX_HAS_OBJECT, (RDFNode) null).next().getResource();

			uuid2Shape.put(instance.getURI(), new NodeShape());
		}
	}
}
