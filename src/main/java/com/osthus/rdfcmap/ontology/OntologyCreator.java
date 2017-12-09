package com.osthus.rdfcmap.ontology;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

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
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.cmap.Cmap2TurtleConverter;
import com.osthus.rdfcmap.cmap.shape.ShapesCreator1_1_5;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class OntologyCreator
{
	private static final Logger log = LogManager.getLogger("Logger");

	public static Map<String, Resource> shapeName2class = new HashMap<String, Resource>();

	public static Map<Resource, Resource> property2domainProperty = new HashMap<Resource, Resource>();

	private static Map<Resource, Integer> property2numApplied = new HashMap<>();

	public void create(String inputFileToConvert, String[] additionalInputFiles) throws JAXBException, IOException, ParserConfigurationException, SAXException
	{
		log.info("Creating ontology from: " + inputFileToConvert + ((additionalInputFiles != null && additionalInputFiles.length > 0)
				? " using additional files: " + StringUtils.join(additionalInputFiles, ", ") : ""));

		String pathToTtlFile = StringUtils.EMPTY;
		if (inputFileToConvert.toLowerCase().endsWith("cxl"))
		{
			log.info("Creating shapes.");
			Cmap2TurtleConverter cmap2TurtleConverter = new Cmap2TurtleConverter();
			if (additionalInputFiles != null && additionalInputFiles.length > 0)
			{
				cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), additionalInputFiles);
			}
			else
			{
				cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), null);
			}
			pathToTtlFile = inputFileToConvert.toString().substring(0, inputFileToConvert.toString().length() - 3) + "ttl";
			inputFileToConvert = pathToTtlFile; // created TTL from CXL, now take new TTL as input for shapes creation
		}
		else if (!inputFileToConvert.toLowerCase().endsWith("ttl"))
		{
			log.error("No .cxl or .ttl as input file found.");
			System.exit(1);
		}

		ShapesCreator1_1_5 shapesCreator = new ShapesCreator1_1_5();
		if (additionalInputFiles != null && additionalInputFiles.length > 0)
		{
			shapesCreator.create(Paths.get(inputFileToConvert), additionalInputFiles);
		}
		else
		{
			shapesCreator.create(Paths.get(inputFileToConvert), null);
		}

		if (pathToTtlFile != null && !pathToTtlFile.isEmpty())
		{
			Files.deleteIfExists(Paths.get(pathToTtlFile));
		}

		Model model = createOntologyFromShapes(ShapesCreator1_1_5.modelWithShapes);

		model = addVocabulary(model, ShapesCreator1_1_5.modelWithShapes);

		Path outPath = Paths.get(inputFileToConvert.toString().substring(0, inputFileToConvert.toString().length() - 4) + "-ontology.ttl");
		Files.deleteIfExists(outPath);
		model.write(new FileOutputStream(outPath.toFile()), "TTL");
	}

	private Model createOntologyFromShapes(Model modelWithShapes)
	{
		log.info("Now creating ontology from shapes.");
		Model model = ModelFactory.createDefaultModel();
		Map<String, String> prefixes = modelWithShapes.getNsPrefixMap();
		prefixes.remove("");
		prefixes.put(RdfCmap.prefix, RdfCmap.namespace);
		model.setNsPrefixes(prefixes);

		StmtIterator shapesIterator = modelWithShapes.listStatements((Resource) null, AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);
		while (shapesIterator.hasNext())
		{
			Statement shapeTypeStatement = shapesIterator.next();
			Resource shapeResource = shapeTypeStatement.getSubject();
			String shapeName = determineShapeName(shapeResource);
			Resource classResource = shapeName2class.get(shapeName);
			if (classResource == null)
			{
				classResource = model.createResource(RdfCmap.namespace + shapeName);
				classResource.addProperty(AFOUtil.RDF_TYPE, AFOUtil.OWL_CLASS);
				classResource.addProperty(AFOUtil.SKOS_PREF_LABEL, shapeName.replaceAll("-", " ").replaceAll("([A-Z])", " $1").trim().toLowerCase());
				shapeName2class.put(shapeName, classResource);
			}
		}

		shapesIterator = modelWithShapes.listStatements((Resource) null, AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);
		while (shapesIterator.hasNext())
		{
			Statement shapeTypeStatement = shapesIterator.next();
			Resource shapeResource = shapeTypeStatement.getSubject();
			String shapeName = determineShapeName(shapeResource);
			Resource classResource = shapeName2class.get(shapeName);

			StmtIterator shapePropertyStmtIterator = modelWithShapes.listStatements(shapeTypeStatement.getSubject(), AFOUtil.SHACL_PROPERTY, (RDFNode) null);
			while (shapePropertyStmtIterator.hasNext())
			{
				Statement shapePropertyStatement = shapePropertyStmtIterator.next();
				StmtIterator propertyShapeStmtIterator = modelWithShapes.listStatements(shapePropertyStatement.getResource(), (Property) null, (RDFNode) null);

				ClassDescriptionAsShape classDescriptionAsShape = determineClassDescription(shapeName, propertyShapeStmtIterator);

				model = addClassDescription(model, classResource.getURI(), modelWithShapes, classDescriptionAsShape);
			}
		}

		shapesIterator = modelWithShapes.listStatements((Resource) null, AFOUtil.RDF_TYPE, AFOUtil.SHACL_NODE_SHAPE);
		while (shapesIterator.hasNext())
		{
			Statement shapeTypeStatement = shapesIterator.next();
			Resource shapeResource = shapeTypeStatement.getSubject();
			String shapeName = determineShapeName(shapeResource);
			Resource classResource = shapeName2class.get(shapeName);

			StmtIterator shapePropertyStmtIterator = modelWithShapes.listStatements(shapeTypeStatement.getSubject(), AFOUtil.SHACL_PROPERTY, (RDFNode) null);
			while (shapePropertyStmtIterator.hasNext())
			{
				Statement shapePropertyStatement = shapePropertyStmtIterator.next();
				StmtIterator propertyShapeStmtIterator = modelWithShapes.listStatements(shapePropertyStatement.getResource(), (Property) null, (RDFNode) null);

				ClassDescriptionAsShape classDescriptionAsShape = determineClassDescription(shapeName, propertyShapeStmtIterator);

				addPropertyRestriction(modelWithShapes, model, classResource, classDescriptionAsShape);
			}
		}

		return model;
	}

	private void addPropertyRestriction(Model modelWithShapes, Model model, Resource classResource, ClassDescriptionAsShape classDescriptionAsShape)
	{
		Resource propertyPath = classDescriptionAsShape.getPropertyPath();

		if (propertyPath.isURIResource() && (classDescriptionAsShape.getNode() != null || classDescriptionAsShape.getDataType() != null
				|| classDescriptionAsShape.getNodeKind() != null))
		{
			// this is a single property
			Model restrictionModel = ModelFactory.createDefaultModel();
			Resource restriction = restrictionModel.createResource();
			restriction.addProperty(AFOUtil.RDF_TYPE, AFOUtil.OWL_RESTRICTION);
			if (classDescriptionAsShape.getMinCount() != null || classDescriptionAsShape.getMaxCount() != null)
			{
				if (classDescriptionAsShape.getMinCount() != null)
				{
					if (classDescriptionAsShape.getMaxCount() != null)
					{
						if (classDescriptionAsShape.getMinCount().intValue() == classDescriptionAsShape.getMaxCount().intValue())
						{
							restriction.addLiteral(AFOUtil.OWL_CARDINALITY, classDescriptionAsShape.getMinCount().longValue());
						}
						else
						{
							restriction.addLiteral(AFOUtil.OWL_MIN_CARDINALITY, classDescriptionAsShape.getMinCount().longValue());
							restriction.addLiteral(AFOUtil.OWL_MAX_CARDINALITY, classDescriptionAsShape.getMaxCount().longValue());
						}
					}
					else
					{
						restriction.addLiteral(AFOUtil.OWL_MIN_CARDINALITY, classDescriptionAsShape.getMinCount().longValue());
					}
				}
				else
				{
					restriction.addLiteral(AFOUtil.OWL_MAX_CARDINALITY, classDescriptionAsShape.getMaxCount().longValue());
				}
			}

			Resource onClass = determineClassFromShape(classDescriptionAsShape);
			restriction.addProperty(AFOUtil.OWL_ALL_VALUES_FROM, onClass);

			if (RdfCmap.addSpecificProperties //
					&& property2numApplied.get(propertyPath).intValue() == 1 //
					&& isDomainProperty(propertyPath, modelWithShapes) //
					&& !modelWithShapes.listStatements(propertyPath, AFOUtil.RDFS_DOMAIN, (RDFNode) null).hasNext() //
					&& !modelWithShapes.listStatements(propertyPath, AFOUtil.RDFS_RANGE, (RDFNode) null).hasNext())
			{
				Resource domainProperty = property2domainProperty.get(propertyPath);
				if (domainProperty == null)
				{
					domainProperty = model.createResource(RdfCmap.namespace + propertyPath.getLocalName());
					StmtIterator propertyStatementIterator = modelWithShapes.listStatements(propertyPath, (Property) null, (RDFNode) null);
					while (propertyStatementIterator.hasNext())
					{
						Statement propertyStatement = propertyStatementIterator.next();
						if (propertyStatement.getPredicate().equals(AFOUtil.RDFS_SUBPROPERTY_OF))
						{
							continue;
						}
						domainProperty.addProperty(propertyStatement.getPredicate(), propertyStatement.getObject());
						if (propertyStatement.getObject().isAnon())
						{
							List<Statement> blankNodeStatements = new ArrayList<>();
							blankNodeStatements = CmapUtil.addStatementsWithBlankNodes(modelWithShapes, propertyStatement, blankNodeStatements);
							model.add(blankNodeStatements);
						}
					}

					domainProperty.addProperty(AFOUtil.RDFS_SUBPROPERTY_OF, propertyPath);

					domainProperty.addProperty(AFOUtil.RDFS_DOMAIN, classResource);

					Resource classRange = determineClassFromShape(classDescriptionAsShape);
					domainProperty.addProperty(AFOUtil.RDFS_RANGE, classRange);

					property2domainProperty.put(propertyPath, domainProperty);
				}
				else
				{
					throw new IllegalStateException("Found specific domain property with multiple occurrence.");
				}

				propertyPath = domainProperty;
			}
			else if (!model.containsResource(propertyPath))
			{
				StmtIterator propertyStmtIterator = modelWithShapes.listStatements(propertyPath, (Property) null, (RDFNode) null);
				while (propertyStmtIterator.hasNext())
				{
					Statement propertyStatement = propertyStmtIterator.next();
					model.add(propertyStatement);
					if (propertyStatement.getObject().isAnon())
					{
						List<Statement> blankNodeStatements = new ArrayList<>();
						blankNodeStatements = CmapUtil.addStatementsWithBlankNodes(modelWithShapes, propertyStatement, blankNodeStatements);
						model.add(blankNodeStatements);
					}
				}
			}

			restriction.addProperty(AFOUtil.OWL_ON_PROPERTY, propertyPath);

			if (!hasPropertyRestriction(classResource, restriction, model, restrictionModel))
			{
				model.add(restrictionModel);
				classResource.addProperty(AFOUtil.RDFS_SUBCLASS_OF, restriction);
			}
			else
			{
				log.debug("Skipping already existing property restriction of property: " + propertyPath.getURI() + " for shape: "
						+ classDescriptionAsShape.getName());
			}
		}
	}

	private ClassDescriptionAsShape determineClassDescription(String shapeName, StmtIterator propertyShapeStmtIterator)
	{
		ClassDescriptionAsShape classDescriptionAsShape = new ClassDescriptionAsShape();
		while (propertyShapeStmtIterator.hasNext())
		{
			Statement propertyShapeStatement = propertyShapeStmtIterator.next();
			Property property = propertyShapeStatement.getPredicate();

			if (property.equals(AFOUtil.SHACL_MIN_COUNT))
			{
				classDescriptionAsShape.setMinCount(propertyShapeStatement.getInt());
			}
			else if (property.equals(AFOUtil.SHACL_MAX_COUNT))
			{
				classDescriptionAsShape.setMaxCount(propertyShapeStatement.getInt());
			}
			else if (property.equals(AFOUtil.SHACL_NODE))
			{
				classDescriptionAsShape.setNode(propertyShapeStatement.getResource());
			}
			else if (property.equals(AFOUtil.SHACL_NODEKIND))
			{
				classDescriptionAsShape.setNodeKind(propertyShapeStatement.getResource());
			}
			else if (property.equals(AFOUtil.SHACL_DATATYPE))
			{
				classDescriptionAsShape.setDataType(propertyShapeStatement.getResource());
			}
			else if (property.equals(AFOUtil.SHACL_PATH))
			{
				classDescriptionAsShape.setPropertyPath(propertyShapeStatement.getResource());
			}
			else if (property.equals(AFOUtil.SHACL_IN))
			{
				classDescriptionAsShape.setAllowedValues(propertyShapeStatement.getResource());
			}
			else
			{
				continue;
			}
		}
		classDescriptionAsShape.setName(shapeName);
		return classDescriptionAsShape;
	}

	private Model addClassDescription(Model model, String classResourceUri, Model modelWithShapes, ClassDescriptionAsShape classDescriptionAsShape)
	{
		Resource classResource = model.getResource(classResourceUri);

		Resource propertyPath = classDescriptionAsShape.getPropertyPath();

		if (propertyPath.isAnon() && classDescriptionAsShape.getAllowedValues() != null)
		{
			// this is rdf:type/rdfs:subClassOf*
			RDFList parentClasses = classDescriptionAsShape.getAllowedValues().as(RDFList.class);
			ExtendedIterator<RDFNode> parentClassesIterator = parentClasses.iterator();
			while (parentClassesIterator.hasNext())
			{
				Resource parentClass = parentClassesIterator.next().asResource();
				if (!classResource.hasProperty(AFOUtil.RDFS_SUBCLASS_OF, parentClass))
				{
					classResource.addProperty(AFOUtil.RDFS_SUBCLASS_OF, parentClass);
				}
				else
				{
					log.debug("Skipping already existing parent class: " + parentClass.getURI() + " for shape: " + classDescriptionAsShape.getName());
				}
			}
		}
		else if (propertyPath.isURIResource() && (classDescriptionAsShape.getNode() != null || classDescriptionAsShape.getDataType() != null
				|| classDescriptionAsShape.getNodeKind() != null))
		{
			// this is a single property
			// in this iteration simply count occurrences (in order to decide about specificity of properties, e.g. one occurrence can be handled with specific
			// domain property (domain and range specified) but multiple occurrence is described by more generic AFX property without domain and range
			// specified)
			Integer numPropertyPath = property2numApplied.get(propertyPath);
			if (numPropertyPath == null)
			{
				numPropertyPath = 1;
				property2numApplied.put(propertyPath, numPropertyPath);
			}
			else
			{
				numPropertyPath++;
				property2numApplied.put(propertyPath, numPropertyPath);
			}
		}
		else
		{
			throw new IllegalStateException("Unknown property path in shape. Only single properties or rdf:type/rdfs:subClassOf* allowed.");
		}

		return model;
	}

	private Model addVocabulary(Model model, Model modelWithShapes)
	{
		List<Statement> statementsToAdd = new ArrayList<>();

		// add parent classes
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDFS_SUBCLASS_OF, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getSubject().isAnon())
			{
				continue;
			}
			statementsToAdd = handleNextParent(model, modelWithShapes, statementsToAdd, statement, AFOUtil.RDFS_SUBCLASS_OF);
		}

		model.add(statementsToAdd);
		statementsToAdd.clear();

		// add parent properties
		stmtIterator = model.listStatements((Resource) null, AFOUtil.RDFS_SUBPROPERTY_OF, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getSubject().isAnon())
			{
				continue;
			}
			statementsToAdd = handleNextParent(model, modelWithShapes, statementsToAdd, statement, AFOUtil.RDFS_SUBPROPERTY_OF);
		}

		model.add(statementsToAdd);
		statementsToAdd.clear();

		int maxLoop = 10;
		int i = 0;
		statementsToAdd = determineStillMissingVocabulary(model, modelWithShapes, statementsToAdd);
		while (!statementsToAdd.isEmpty())
		{
			model.add(statementsToAdd);
			statementsToAdd.clear();
			statementsToAdd = determineStillMissingVocabulary(model, modelWithShapes, statementsToAdd);
			if (i >= maxLoop)
			{
				log.error("Could not resolve all missing external vocabulary.");
				break;
			}
			i++;
		}

		return model;
	}

	private List<Statement> determineStillMissingVocabulary(Model model, Model modelWithShapes, List<Statement> statementsToAdd)
	{
		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource propertyAsResource = statement.getPredicate().asResource();
			if (!model.listStatements(propertyAsResource, (Property) null, (RDFNode) null).hasNext())
			{
				StmtIterator propertyStatementIterator = modelWithShapes.listStatements(propertyAsResource, (Property) null, (RDFNode) null);
				while (propertyStatementIterator.hasNext())
				{
					Statement propertyStatement = propertyStatementIterator.next();
					statementsToAdd.add(propertyStatement);
					if (propertyStatement.getObject().isAnon())
					{
						statementsToAdd = CmapUtil.addStatementsWithBlankNodes(modelWithShapes, propertyStatement, statementsToAdd);
					}
				}
			}

			RDFNode object = statement.getObject();

			if (!object.isResource())
			{
				continue;
			}

			if (!model.listStatements(object.asResource(), (Property) null, (RDFNode) null).hasNext())
			{
				StmtIterator objectStatementIterator = modelWithShapes.listStatements(object.asResource(), (Property) null, (RDFNode) null);
				while (objectStatementIterator.hasNext())
				{
					Statement objectStatement = objectStatementIterator.next();
					statementsToAdd.add(objectStatement);
					if (objectStatement.getObject().isAnon())
					{
						statementsToAdd = CmapUtil.addStatementsWithBlankNodes(modelWithShapes, objectStatement, statementsToAdd);
					}
				}
			}
		}
		return statementsToAdd;
	}

	private List<Statement> handleNextParent(Model model, Model modelWithShapes, List<Statement> statementsToAdd, Statement nestedStatement,
			Property linkToParent)
	{
		RDFNode nestedObject = nestedStatement.getObject();
		if (!model.listStatements(nestedObject.asResource(), (Property) null, (RDFNode) null).hasNext())
		{
			StmtIterator nestedObjectStmtIterator = modelWithShapes.listStatements(nestedObject.asResource(), (Property) null, (RDFNode) null);
			while (nestedObjectStmtIterator.hasNext())
			{
				Statement nestedObjectStatement = nestedObjectStmtIterator.next();
				statementsToAdd.add(nestedObjectStatement);
				if (nestedObjectStatement.getObject().isAnon())
				{
					List<Statement> blankNodeStatements = new ArrayList<>();
					blankNodeStatements = CmapUtil.addStatementsWithBlankNodes(modelWithShapes, nestedObjectStatement, blankNodeStatements);
					statementsToAdd.addAll(blankNodeStatements);
				}
			}
			if (modelWithShapes.listStatements(nestedObject.asResource(), linkToParent, (RDFNode) null).hasNext())
			{
				StmtIterator nestedStmtIterator = modelWithShapes.listStatements(nestedObject.asResource(), linkToParent, (RDFNode) null);
				while (nestedStmtIterator.hasNext())
				{
					Statement statement = nestedStmtIterator.next();
					statementsToAdd = handleNextParent(model, modelWithShapes, statementsToAdd, statement, linkToParent);
				}
			}
		}
		return statementsToAdd;
	}

	private Resource determineClassFromShape(ClassDescriptionAsShape classDescriptionAsShape)
	{
		if (classDescriptionAsShape.getNodeKind().equals(AFOUtil.SHACL_LITERAL))
		{
			return classDescriptionAsShape.getDataType();
		}
		else if (classDescriptionAsShape.getNodeKind().equals(AFOUtil.SHACL_IRI))
		{
			if (classDescriptionAsShape.getNode() != null)
			{
				Resource nodeClass = classDescriptionAsShape.getNode();
				if (nodeClass.getNameSpace().equals(AFOUtil.AFS_R_PREFIX))
				{
					String shapeName = determineShapeName(nodeClass);
					nodeClass = shapeName2class.get(shapeName);
				}

				return nodeClass;
			}
			else
			{
				return AFOUtil.OWL_THING;
			}
		}
		else
		{
			throw new IllegalStateException("Unhandled nodekind");
		}
	}

	private boolean isDomainProperty(Resource propertyPath, Model modelWithShapes)
	{
		if (isLiteralDomainProperty(propertyPath, modelWithShapes))
		{
			return true;
		}

		if (isObjectDomainProperty(propertyPath, modelWithShapes))
		{
			return true;
		}

		return false;
	}

	private boolean isLiteralDomainProperty(Resource propertyPath, Model modelWithShapes)
	{
		if (!modelWithShapes.listStatements(propertyPath, AFOUtil.RDF_TYPE, AFOUtil.OWL_DATATYPE_PROPERTY).hasNext())
		{
			return false;
		}
		if (propertyPath.equals(AFOUtil.DCT_TITLE))
		{
			return false;
		}
		if (propertyPath.equals(AFOUtil.DCT_DESCRIPTION))
		{
			return false;
		}
		if (propertyPath.equals(AFOUtil.DCT_CREATED))
		{
			return false;
		}

		return true;
	}

	private boolean isObjectDomainProperty(Resource propertyPath, Model modelWithShapes)
	{
		if (!modelWithShapes.listStatements(propertyPath, AFOUtil.RDF_TYPE, AFOUtil.OWL_OBJECT_PROPERTY).hasNext())
		{
			return false;
		}

		if (!propertyPath.getNameSpace().equals(AFOUtil.AFX_PREFIX))
		{
			return false;
		}

		return true;
	}

	private boolean hasPropertyRestriction(Resource classResource, Resource restriction, Model model, Model restrictionModel)
	{
		StmtIterator parentClassIterator = model.listStatements(classResource, AFOUtil.RDFS_SUBCLASS_OF, (RDFNode) null);
		while (parentClassIterator.hasNext())
		{
			Statement statement = parentClassIterator.next();
			Resource existingRestriction = statement.getResource();
			if (!existingRestriction.isAnon())
			{
				continue;
			}

			if (!existingRestriction.hasProperty(AFOUtil.RDF_TYPE, AFOUtil.OWL_RESTRICTION))
			{
				log.warn("Found anonymous parent class that is not a restriction. Check " + classResource.getURI());
				continue;
			}

			if (areSameRestrictions(existingRestriction, restriction, model, restrictionModel))
			{
				return true;
			}
		}

		return false;
	}

	private boolean areSameRestrictions(Resource existingRestriction, Resource newRestriction, Model model, Model restrictionModel)
	{
		StmtIterator newRestrictionStmtIterator = restrictionModel.listStatements(newRestriction, (Property) null, (RDFNode) null);
		while (newRestrictionStmtIterator.hasNext())
		{
			Statement statement = newRestrictionStmtIterator.next();
			Property property = statement.getPredicate();
			RDFNode object = statement.getObject();
			if (!existingRestriction.hasProperty(property, object))
			{
				return false;
			}
		}
		return true;
	}

	private String determineShapeName(Resource shapeResource)
	{
		String shapeName = shapeResource.getLocalName().substring(0, shapeResource.getLocalName().lastIndexOf("-"));
		if (shapeName.endsWith("-") || shapeName.endsWith("-shape"))
		{
			shapeName = shapeName.substring(0, shapeName.lastIndexOf("-"));
		}
		shapeName = shapeName.replaceAll("-", " ");
		shapeName = WordUtils.capitalizeFully(shapeName);
		shapeName = StringUtils.deleteWhitespace(shapeName);
		return shapeName;
	}
}
