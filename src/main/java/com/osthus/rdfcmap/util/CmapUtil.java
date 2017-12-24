package com.osthus.rdfcmap.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.cmap.cardinality.Cardinality;
import com.osthus.rdfcmap.cmap.cardinality.CardinalityEnum;
import com.osthus.rdfcmap.cmap.cardinality.CardinalityPattern;
import com.osthus.rdfcmap.enums.ColorScheme;
import com.osthus.rdfcmap.enums.DomainEnum;

/**
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CmapUtil
{
	private static final String COLOR_RED = "255,0,0,255";

	private static final String COLOR_BLACK = "0,0,0,255";

	public static final String URN_UUID = "urn:uuid:";

	public static final String NO_IMAGE = "none";

	public static final String CARDINALITY_PATTERN = "^\\s*([\\w\\s\\(\\)\\:\\-]+)\\s+((([\\[\\]]?)([\\<\\>\\=]*)\\s*([0-9]+)([\\[\\]]?))\\s*[\\,]?([0-9]*)\\s*([\\[\\]]?))\\s*$";

	private static final Logger log = LogManager.getLogger("Logger");

	public static final String MIN_ZERO = ">0";

	public static Map<String, String> createOrRetrieveMapOfUiProperties(Map<String, Map<String, String>> mapOfId2Properties, String key)
	{
		Map<String, String> mapOfUiProperties = mapOfId2Properties.get(key);

		if (mapOfUiProperties != null)
		{
			return mapOfUiProperties;
		}

		return new java.util.HashMap<>();
	}

	public static void writeVisualizationModel(Path path, Model model) throws IOException
	{
		String inputPath = path.getParent().toString();
		String outputFileName = inputPath + "/vizmodel.ttl";
		Path outPath = Paths.get(outputFileName);
		Files.deleteIfExists(outPath);
		outPath = Files.createFile(outPath);

		Resource map = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP).next().getSubject();

		Set<Statement> statements = new HashSet<>();
		Set<Resource> relatedResources = new HashSet<>();

		StmtIterator stmtIterator = model.listStatements(map, (Property) null, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			RDFNode object = statement.getObject();
			statements.add(statement);
			if (object.isLiteral())
			{
				continue;
			}
			relatedResources.add((Resource) object);
		}

		relatedResources.addAll(collectRelatedResources(model, relatedResources));

		stmtIterator = model.listStatements((Resource) null, (Property) null, map);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (subject.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT) || subject.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_LINK)
					|| subject.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONNECTION) || subject.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_RESOURCE))
			{
				continue;
			}
			relatedResources.add(subject);
		}

		relatedResources.addAll(collectRelatedResources(model, relatedResources));

		Model vizModel = ModelFactory.createDefaultModel();
		vizModel.add(new ArrayList<Statement>(statements));
		for (Iterator<Resource> iterator = relatedResources.iterator(); iterator.hasNext();)
		{
			Resource resource = iterator.next();
			vizModel.add(model.listStatements(resource, (Property) null, (RDFNode) null).toList());
			vizModel.add(model.listStatements((Resource) null, (Property) null, resource).toList());
		}

		vizModel.write(new FileOutputStream(outPath.toFile()), "TTL");
	}

	private static Set<Resource> collectRelatedResources(Model model, Set<Resource> resources)
	{
		Set<Resource> relatedResources = new HashSet<>(resources);
		for (Iterator<Resource> iterator = resources.iterator(); iterator.hasNext();)
		{
			Resource resource = iterator.next();
			StmtIterator stmtIterator = model.listStatements(resource, (Property) null, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				RDFNode object = statement.getObject();
				if (object.isLiteral() || !object.asResource().toString().startsWith(VizUtil.AFV_PREFIX))
				{
					continue;
				}
				if (object.asResource().hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT)
						|| object.asResource().hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_LINK)
						|| object.asResource().hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONNECTION)
						|| object.asResource().hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_RESOURCE))
				{
					continue;
				}

				relatedResources.add((Resource) object);
			}
		}
		return relatedResources;
	}

	/**
	 * create a CMap with default parameters
	 *
	 * @param model
	 */
	public static void createMap(Model model)
	{
		Resource map = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		map.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_MAP);
		map.addLiteral(AFOUtil.DCT_TITLE, "TTL2CMap");

		Calendar cal = Calendar.getInstance();
		Literal now = model.createTypedLiteral(cal);
		map.addLiteral(AFOUtil.DCT_CREATED, now);

		map.addLiteral(AFOUtil.DCT_MODIFIED, now);
		map.addLiteral(AFOUtil.DCT_PUBLISHER, "OSTHUS GmbH");
		map.addLiteral(VizUtil.AFV_WIDTH, "9542");
		map.addLiteral(VizUtil.AFV_HEIGHT, "1656");

		map.addLiteral(VizUtil.AFV_BACKGROUND_COLOR, "255,255,255,0");

		Resource conceptStyle = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		conceptStyle.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_GRAPHIC_STYLE);

		Resource conceptFont = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		conceptFont.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_FONT);
		conceptFont.addLiteral(AFOUtil.DCT_TITLE, "Verdana");
		conceptFont.addLiteral(VizUtil.AFV_SIZE, "12");
		conceptFont.addLiteral(VizUtil.AFV_STYLE, "plain");
		conceptFont.addLiteral(VizUtil.AFV_COLOR, COLOR_BLACK);
		conceptStyle.addProperty(VizUtil.AFV_HAS_FONT, conceptFont);

		Resource conceptBorder = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		conceptBorder.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_BORDER);
		conceptBorder.addLiteral(VizUtil.AFV_COLOR, COLOR_BLACK);
		conceptBorder.addLiteral(VizUtil.AFV_STYLE, "solid");
		conceptBorder.addLiteral(VizUtil.AFV_THICKNESS, "1");
		conceptBorder.addLiteral(VizUtil.AFV_SHAPE, "rounded-rectangle");
		conceptBorder.addLiteral(VizUtil.AFV_SHAPE_ARC, "15.0");
		conceptStyle.addProperty(VizUtil.AFV_HAS_BORDER, conceptBorder);

		conceptStyle.addLiteral(VizUtil.AFV_TEXT_MARGIN, "4");
		conceptStyle.addLiteral(VizUtil.AFV_TEXT_ALIGNMENT, "center");
		conceptStyle.addLiteral(VizUtil.AFV_BACKGROUND_COLOR, "237,244,246,255");
		conceptStyle.addLiteral(VizUtil.AFV_SHADOW_COLOR, "none");
		conceptStyle.addLiteral(VizUtil.AFV_MINIMUM_WIDTH, "-1");
		conceptStyle.addLiteral(VizUtil.AFV_MINIMUM_HEIGHT, "-1");
		conceptStyle.addLiteral(VizUtil.AFV_MAXIMUM_WIDTH, "-1.0");
		conceptStyle.addLiteral(VizUtil.AFV_GROUP_CHILD_SPACING, "10");
		conceptStyle.addLiteral(VizUtil.AFV_GROUP_PARENT_SPACING, "10");

		map.addProperty(VizUtil.AFV_HAS_CONCEPT_STYLE, conceptStyle);

		Resource linkStyle = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		linkStyle.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_GRAPHIC_STYLE);

		Resource linkFont = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		linkFont.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_FONT);
		linkFont.addLiteral(AFOUtil.DCT_TITLE, "Verdana");
		linkFont.addLiteral(VizUtil.AFV_SIZE, "12");
		linkFont.addLiteral(VizUtil.AFV_STYLE, "plain");
		linkFont.addLiteral(VizUtil.AFV_COLOR, COLOR_BLACK);
		linkStyle.addProperty(VizUtil.AFV_HAS_FONT, linkFont);

		Resource linkBorder = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		linkBorder.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_BORDER);
		linkBorder.addLiteral(VizUtil.AFV_COLOR, COLOR_BLACK);
		linkBorder.addLiteral(VizUtil.AFV_STYLE, "solid");
		linkBorder.addLiteral(VizUtil.AFV_THICKNESS, "1");
		linkBorder.addLiteral(VizUtil.AFV_SHAPE, "rectangle");
		linkBorder.addLiteral(VizUtil.AFV_SHAPE_ARC, "15.0");
		linkStyle.addProperty(VizUtil.AFV_HAS_BORDER, linkBorder);

		linkStyle.addLiteral(VizUtil.AFV_TEXT_MARGIN, "4");
		linkStyle.addLiteral(VizUtil.AFV_TEXT_ALIGNMENT, "center");
		linkStyle.addLiteral(VizUtil.AFV_BACKGROUND_COLOR, "237,244,246,255");
		linkStyle.addLiteral(VizUtil.AFV_SHADOW_COLOR, "none");
		linkStyle.addLiteral(VizUtil.AFV_MINIMUM_WIDTH, "-1");
		linkStyle.addLiteral(VizUtil.AFV_MINIMUM_HEIGHT, "-1");
		linkStyle.addLiteral(VizUtil.AFV_MAXIMUM_WIDTH, "-1.0");
		linkStyle.addLiteral(VizUtil.AFV_GROUP_CHILD_SPACING, "10");
		linkStyle.addLiteral(VizUtil.AFV_GROUP_PARENT_SPACING, "10");

		map.addProperty(VizUtil.AFV_HAS_LINK_STYLE, linkStyle);

		Resource connectionStyle = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		connectionStyle.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_GRAPHIC_STYLE);
		connectionStyle.addLiteral(VizUtil.AFV_COLOR, COLOR_BLACK);
		connectionStyle.addLiteral(VizUtil.AFV_STYLE, "solid");
		connectionStyle.addLiteral(VizUtil.AFV_THICKNESS, "1");
		connectionStyle.addLiteral(VizUtil.AFV_CONNECTION_TYPE, "straight");
		connectionStyle.addLiteral(VizUtil.AFV_ARROW_HEAD, "if-to-concept-and-slopes-up");

		map.addProperty(VizUtil.AFV_HAS_CONNECTION_STYLE, connectionStyle);

		Resource resourceStyle = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		resourceStyle.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_GRAPHIC_STYLE);

		Resource resourceFont = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
		resourceFont.addProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_FONT);
		resourceFont.addLiteral(AFOUtil.DCT_TITLE, "SanSerif");
		resourceFont.addLiteral(VizUtil.AFV_SIZE, "12");
		resourceFont.addLiteral(VizUtil.AFV_STYLE, "plain");
		resourceFont.addLiteral(VizUtil.AFV_COLOR, COLOR_BLACK);

		resourceStyle.addProperty(VizUtil.AFV_HAS_FONT, resourceFont);
		resourceStyle.addLiteral(VizUtil.AFV_BACKGROUND_COLOR, "192,192,192,255");

		map.addProperty(VizUtil.AFV_HAS_RESOURCE_STYLE, resourceStyle);
	}

	public static Model createOrUpdateLiteralValue(Model model, Resource resource, Property property, Object object)
	{
		if (object == null)
		{
			return model;
		}

		resource.removeAll(property);

		resource.addLiteral(property, object);

		return model;
	}

	public static Model createOrUpdateLiteralValueOfRelatedResource(Model model, Resource resource, Property objectProperty, Resource objectType,
			Property datatypeProperty, String newLiteralValue)
	{
		Resource relatedResource;
		if (model.contains(resource, objectProperty, (RDFNode) null))
		{
			if (model.listStatements(resource, objectProperty, (RDFNode) null).toList().size() > 1)
			{
				System.out.println("Concept " + resource.getURI() + " has multiple related resources via property of " + objectProperty.getLocalName()
						+ " ? Resetting related resources.");
				StmtIterator objectIterator = model.listStatements(resource, objectProperty, (RDFNode) null);
				while (objectIterator.hasNext())
				{
					Statement statement = objectIterator.next();
					Resource relatedObject = statement.getObject().asResource();
					model.removeAll(relatedObject, (Property) null, (RDFNode) null);
				}
				resource.removeAll(objectProperty);
				relatedResource = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
				relatedResource.addProperty(AFOUtil.RDF_TYPE, objectType);
				resource.addProperty(objectProperty, relatedResource);
			}
			else
			{
				relatedResource = resource.getPropertyResourceValue(objectProperty);
			}
		}
		else
		{
			relatedResource = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
			relatedResource.addProperty(AFOUtil.RDF_TYPE, objectType);
			resource.addProperty(objectProperty, relatedResource);
		}

		model = CmapUtil.createOrUpdateLiteralValue(model, relatedResource, datatypeProperty, newLiteralValue);
		return model;
	}

	public static Set<Statement> addStatementsWithBlankNodes(Model model, Statement statement, Set<Statement> statements)
	{
		if (statement.getObject().isAnon())
		{
			StmtIterator anonIterator = model.listStatements(statement.getObject().asResource(), (Property) null, (RDFNode) null);
			while (anonIterator.hasNext())
			{
				Statement anonStatement = anonIterator.next();
				if (!statements.contains(anonStatement))
				{
				statements.add(anonStatement);
					statements = addStatementsWithBlankNodes(model, anonStatement, statements);
				}
			}
		}

		return statements;
	}

	/**
	 * Extract RDf model from additional &lt;rdf-model&gt;-tag within CXL. The visualization may contain changes that are not yet synchronized to the stored
	 * model. So, we first extract the stored model.
	 *
	 * @param path
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static Model extractStoredModelFromCxl(Path path) throws ParserConfigurationException, SAXException, IOException, FileNotFoundException
	{
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new FileInputStream(path.toFile()));

		Model model = ModelFactory.createDefaultModel();

		Node rdfModelNode = document.getElementsByTagName("rdf-model").item(0);
		String rdfModelString = StringUtils.EMPTY;
		if (rdfModelNode != null && !rdfModelNode.getTextContent().isEmpty())
		{
			rdfModelString = rdfModelNode.getTextContent();
			String inputPath = StringUtils.EMPTY;
			String tempFileName = StringUtils.EMPTY;
			if (path.getParent() != null)
			{
				inputPath = path.getParent().toString();
				tempFileName = inputPath + "/temp.ttl";
			}
			else
			{
				tempFileName = "temp.ttl";
			}

			Path tempInput = Paths.get(tempFileName);
			Files.deleteIfExists(tempInput);
			tempInput = Files.createFile(tempInput);
			FileUtils.writeStringToFile(tempInput.toFile(), rdfModelString);

			model.read(tempInput.toUri().toString(), null, "TTL");
			Files.deleteIfExists(tempInput);
		}
		return model;
	}

	/**
	 * Add triples from the files in the given array of filenames to the given model.
	 *
	 * @param additionalFiles
	 * @param model
	 * @throws FileNotFoundException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Model addTriples(String[] additionalFiles, Model model) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException
	{
		if (additionalFiles != null && additionalFiles.length > 0)
		{
			Model tempModel = ModelFactory.createDefaultModel();
			tempModel.add(model);

			Model visualizationModel = extractVisualizationModel(tempModel);
			int numTriplesBeforeAddingOtherModels = tempModel.listStatements().toList().size();
			tempModel.removeAll();

			for (int i = 0; i < additionalFiles.length; i++)
			{
				Path pathToAdditionalFile = Paths.get(additionalFiles[i]);
				log.info("Reading triples from file: " + pathToAdditionalFile.toString());
				Model additionalModel = null;
				if (pathToAdditionalFile.toString().toUpperCase().endsWith("TTL"))
				{
					additionalModel = ModelFactory.createDefaultModel();
					additionalModel.read(new FileInputStream(pathToAdditionalFile.toFile()), null, "TTL");
				}
				else if (pathToAdditionalFile.toString().toUpperCase().endsWith("CXL"))
				{
					additionalModel = CmapUtil.extractStoredModelFromCxl(pathToAdditionalFile);
				}
				else if (pathToAdditionalFile.toString().toUpperCase().endsWith("OWL"))
				{
					additionalModel = ModelFactory.createDefaultModel();
					additionalModel.read(new FileInputStream(pathToAdditionalFile.toFile()), null, "RDF/XML");
				}
				else
				{
					throw new IllegalStateException("Unsupported input file: " + pathToAdditionalFile.toString());
				}

				if (additionalModel != null && !additionalModel.isEmpty())
				{
					int numTriplesToAdd = additionalModel.listStatements().toList().size();
					int numTriples = tempModel.listStatements().toList().size();
					tempModel = tempModel.add(additionalModel);
					int numTriplesNew = tempModel.listStatements().toList().size();
					log.info(numTriplesToAdd + " triples found. " + (numTriplesNew - numTriples) + " triples added.");
				}
				else
				{
					log.info("No triples added from file: " + pathToAdditionalFile.toString());
				}
			}

			tempModel.add(visualizationModel);
			model.add(tempModel);
			int numTriplesAfterAddingOtherModels = model.listStatements().toList().size();
			int change = numTriplesAfterAddingOtherModels - numTriplesBeforeAddingOtherModels;
			log.info("Model " + (change > 0 ? "increased " : (change == 0 ? "changed " : "reduced ")) + "by " + change + " triples. ");
		}

		return model;
	}

	public static Model addTriples(List<String> additionalFileNames, Model model)
			throws FileNotFoundException, ParserConfigurationException, SAXException, IOException
	{
		String[] additionalFiles = new String[additionalFileNames.size()];
		for (int i = 0; i < additionalFiles.length; i++)
		{
			additionalFiles[i] = additionalFileNames.get(i);
		}
		return addTriples(additionalFiles, model);
	}

	private static Model extractVisualizationModel(Model model)
	{
		Model tempModel = ModelFactory.createDefaultModel();
		Set<Statement> statementsToAdd = new HashSet<Statement>();
		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getSubject().isAnon())
			{
				continue;
			}

			if (statement.getSubject().getURI().contains(VizUtil.AFV_PREFIX))
			{
				statementsToAdd.add(statement);
				statementsToAdd = addStatementsWithBlankNodes(model, statement, statementsToAdd);
			}
			else if (statement.getSubject().getURI().contains(CmapUtil.URN_UUID))
			{
				statementsToAdd.add(statement);
				statementsToAdd = addStatementsWithBlankNodes(model, statement, statementsToAdd);
			}
		}

		tempModel.add(new ArrayList<Statement>(statementsToAdd));
		return tempModel;
	}

	public static String determineBackgroundColor(Model model, Resource uiResource)
	{
		DomainEnum domain = DomainEnum.OTHER;
		Resource resource = uiResource.getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();
		boolean isConcept = VizUtil.AFV_CONCEPT.getURI().equals(uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI());
		boolean isLink = VizUtil.AFV_LINK.getURI().equals(uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI());

		if (isConcept || isLink)
		{
			StmtIterator typeIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
			while (typeIterator.hasNext())
			{
				Statement statement = typeIterator.next();
				Resource rdfType = statement.getResource();
				if (rdfType.getURI().contains(AFOUtil.AFC_PREFIX) || (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFC_")))
				{
					domain = DomainEnum.COMMON;
				}
				else if (rdfType.getURI().contains(AFOUtil.AFE_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFE_")))
				{
					domain = DomainEnum.EQUIPMENT;
				}
				else if (rdfType.getURI().contains(AFOUtil.AFM_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFM_")))
				{
					domain = DomainEnum.MATERIAL;
				}
				else if (rdfType.getURI().contains(AFOUtil.AFP_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFP_")))
				{
					domain = DomainEnum.PROCESS;
				}
				else if (rdfType.getURI().contains(AFOUtil.AFR_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFR_")))
				{
					domain = DomainEnum.RESULT;
				}
				else if (rdfType.getURI().contains(AFOUtil.AFQ_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFQ_")))
				{
					domain = DomainEnum.QUALITY;
				}
				else if (rdfType.getURI().contains(AFOUtil.AFRL_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFRL_")))
				{
					if (isSubClassOf(rdfType, AFOUtil.AFRL_CONTEXTUAL_ROLE, model, null))
					{
						domain = DomainEnum.CONTEXTUAL_ROLE;
					}
					else
					{
						domain = DomainEnum.ROLE;
					}
				}
				else if (rdfType.getURI().contains(AFOUtil.AFX_PREFIX)
						|| (resource.getURI().startsWith(CmapUtil.URN_UUID) && resource.getURI().contains("AFX_")))
				{
					domain = DomainEnum.PROPERTY;
				}
				else if (rdfType.getURI().contains(AFOUtil.QUDT_QUANTITY_EXT_PREFIX) || rdfType.getURI().contains(AFOUtil.QUDT_SCHEMA_EXT_PREFIX)
						|| rdfType.getURI().contains(AFOUtil.QUDT_SCHEMA_PREFIX) || rdfType.getURI().contains(AFOUtil.QUDT_UNIT_PREFIX)
						|| rdfType.getURI().contains(AFOUtil.QUDT_UNIT_EXT_PREFIX))
				{
					if (isSubClassOf(rdfType, AFOUtil.OWL_OBJECT_PROPERTY, model, null) || isSubClassOf(rdfType, AFOUtil.OWL_DATATYPE_PROPERTY, model, null)
							|| isSubClassOf(rdfType, AFOUtil.OWL_ANNOTATION_PROPERTY, model, null))
					{
						domain = DomainEnum.PROPERTY;
					}
					else
					{
						domain = DomainEnum.INFORMATION;
					}
				}
				else if (rdfType.getURI().contains(AFOUtil.IAO_PREFIX))
				{
					if (isSubClassOf(rdfType, AFOUtil.OWL_OBJECT_PROPERTY, model, null) || isSubClassOf(rdfType, AFOUtil.OWL_DATATYPE_PROPERTY, model, null)
							|| isSubClassOf(rdfType, AFOUtil.OWL_ANNOTATION_PROPERTY, model, null))
					{
						domain = DomainEnum.PROPERTY;
					}
					else
					{
						domain = DomainEnum.INFORMATION;
					}
				}
				else if (rdfType.getURI().equals(AFOUtil.OWL_OBJECT_PROPERTY.getURI()))
				{
					if (resource.hasProperty(AFOUtil.SKOS_RELATED))
					{
						Resource property = model.listStatements(resource, AFOUtil.SKOS_RELATED, (RDFNode) null).next().getResource();
						if (property.getNameSpace().equals(AFOUtil.AFX_PREFIX))
						{
							domain = DomainEnum.PROPERTY;
						}
					}
				}
				else
				{
					if (isSubClassOf(rdfType, AFOUtil.OWL_OBJECT_PROPERTY, model, null) || isSubClassOf(rdfType, AFOUtil.OWL_DATATYPE_PROPERTY, model, null)
							|| isSubClassOf(rdfType, AFOUtil.OWL_ANNOTATION_PROPERTY, model, null))
					{
						domain = DomainEnum.PROPERTY;
					}
					else if (isSubClassOf(rdfType, AFOUtil.BFO_OCCURRENT, model, null))
					{
						domain = DomainEnum.PROCESS;
					}
					else if (isSubClassOf(rdfType, AFOUtil.AFE_DEVICE, model, null) || isSubClassOf(rdfType, AFOUtil.OBI_DEVICE, model, null))
					{
						domain = DomainEnum.PROCESS;
					}
					else if (isSubClassOf(rdfType, AFOUtil.BFO_MATERIAL, model, null) && !isSubClassOf(rdfType, AFOUtil.OBI_DEVICE, model, null))
					{
						domain = DomainEnum.MATERIAL;
					}
					else if (isSubClassOf(rdfType, AFOUtil.BFO_QUALITY, model, null))
					{
						domain = DomainEnum.QUALITY;
					}
					else if (isSubClassOf(rdfType, AFOUtil.BFO_ROLE, model, null) && !isSubClassOf(rdfType, AFOUtil.AFRL_CONTEXTUAL_ROLE, model, null))
					{
						domain = DomainEnum.ROLE;
					}
					else if (isSubClassOf(rdfType, AFOUtil.BFO_FUNCTION, model, null))
					{
						domain = DomainEnum.ROLE;
					}
					else if (isSubClassOf(rdfType, AFOUtil.IAO_INFORMATION_CONTENT_ENTITY, model, null))
					{
						domain = DomainEnum.INFORMATION;
					}
				}

				if (domain != DomainEnum.COMMON && domain != DomainEnum.OTHER)
				{
					break;
				}
			}
		}

		String bgColor = ColorScheme.domain2scheme.get(domain.name());
		return bgColor;
	}

	private static boolean isSubClassOf(Resource resource, Resource classType, Model model, List<Resource> visited)
	{
		StmtIterator stmtIterator = model.listStatements(resource, AFOUtil.RDFS_SUBCLASS_OF, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			RDFNode parentClass = statement.getObject();
			if (parentClass.isAnon())
			{
				continue;
			}

			if (visited == null)
			{
				visited = new ArrayList<Resource>();
				visited.add(resource);
			}

			if (visited.contains(parentClass.asResource()))
			{
				continue;
			}

			visited.add(parentClass.asResource());

			if (classType.getURI().equals(parentClass.asResource().getURI()))
			{
				return true;
			}

			if (isSubClassOf(parentClass.asResource(), classType, model, visited))
			{
				return true;
			}
		}
		return false;
	}

	public static String determineFontColor(Model model, Resource uiResource)
	{
		Resource resource = uiResource.getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();
		boolean isLink = VizUtil.AFV_LINK.getURI().equals(uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI());

		if (!isLink)
		{
			return COLOR_BLACK;
		}

		if (resource.hasProperty(AFOUtil.SKOS_RELATED))
		{
			Resource property = model.listStatements(resource, AFOUtil.SKOS_RELATED, (RDFNode) null).next().getResource();
			if (property.getNameSpace().equals(AFOUtil.AFX_PREFIX))
			{
				return COLOR_BLACK;
			}

			// todo: add other colors for different namespaces here
		}
		else
		{
			if (!resource.hasProperty(AFOUtil.DCT_TITLE))
			{
				return COLOR_BLACK;
			}

			String title = model.listStatements(resource, AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
			title = title.replaceAll(" ", "");
			if (!title.toLowerCase().contains(":") || title.toLowerCase().contains("afx:") || title.toLowerCase().contains("af-x:"))
			{
				return COLOR_RED;
			}
		}

		return COLOR_BLACK;
	}

	public static String addCardinality(Model model, Resource link, String title)
	{
		if (!link.hasProperty(VizUtil.AFV_HAS_CARDINALITY))
		{
			return title;
		}

		Resource cardinalityResource = model.listStatements(link, VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).next().getResource();
		Resource cardinalityType = model.listStatements(cardinalityResource, AFOUtil.RDF_TYPE, (RDFNode) null).next().getResource();
		String minimumValue = cardinalityResource.hasProperty(AFOUtil.AFX_MINIMUM_VALUE)
				? cardinalityResource.getProperty(AFOUtil.AFX_MINIMUM_VALUE).getString() : "";
		String maximumValue = cardinalityResource.hasProperty(AFOUtil.AFX_MAXIMUM_VALUE)
				? cardinalityResource.getProperty(AFOUtil.AFX_MAXIMUM_VALUE).getString() : "";

		String cardinalityString = StringUtils.EMPTY;
		if (cardinalityType.equals(VizUtil.AFV_MIN_CARDINALITY))
		{
			if (minimumValue.equals("0"))
			{
				return title;
			}

			cardinalityString = ">" + minimumValue;
		}
		else if (cardinalityType.equals(VizUtil.AFV_EXACT_CARDINALITY))
		{
			cardinalityString = minimumValue;
		}
		else if (cardinalityType.equals(VizUtil.AFV_MAX_CARDINALITY))
		{
			cardinalityString = "<" + maximumValue;
		}
		else if (cardinalityType.equals(VizUtil.AFV_INTERVAL_CARDINALITY))
		{
			cardinalityString = minimumValue + " " + maximumValue;
		}
		else if (cardinalityType.equals(VizUtil.AFV_MAX_EXCLUSIVE_CARDINALITY))
		{
			cardinalityString = "<" + maximumValue + "[";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MAX_INCLUSIVE_CARDINALITY))
		{
			cardinalityString = "<" + maximumValue + "]";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MIN_EXCLUSIVE_CARDINALITY))
		{
			cardinalityString = ">" + maximumValue + "[";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MIN_EXCLUSIVE_MAX_EXCLUSIVED_CARDINALITY))
		{
			cardinalityString = "]" + minimumValue + " " + maximumValue + "[";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MIN_EXCLUSIVE_MAX_INCLUSIVE_CARDINALITY))
		{
			cardinalityString = "]" + minimumValue + " " + maximumValue + "]";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MIN_INCLUSIVE_CARDINALITY))
		{
			cardinalityString = ">" + minimumValue + "]";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MIN_INCLUSIVE_MAX_EXCLUSIVE_CARDINALITY))
		{
			cardinalityString = "[" + minimumValue + " " + maximumValue + "[";
		}
		else if (cardinalityType.equals(VizUtil.AFV_MIN_INCLUSIVE_MAX_INCLUSIVE_CARDINALITY))
		{
			cardinalityString = "[" + minimumValue + " " + maximumValue + "]";
		}
		else
		{
			throw new IllegalStateException("Unknown CardinalityType for resource: " + cardinalityResource.getURI());
		}

		return title + " " + cardinalityString;
	}

	public static Cardinality determineCardinality(String cardinalityString)
	{
		String minimumValue = StringUtils.EMPTY;
		String maximumValue = StringUtils.EMPTY;

		cardinalityString = cardinalityString.replaceAll("=", "");

		if (cardinalityString.isEmpty())
		{
			// standard min 0
			return new Cardinality(VizUtil.AFV_MIN_CARDINALITY, "0", "");
		}
		else if (cardinalityString.contains("[") || cardinalityString.contains("]"))
		{
			// qualified cardinality
			throw new IllegalStateException("Qualified cardinality is not yet supported.");
		}
		else if (cardinalityString.contains(">"))
		{
			// min cardinality
			Pattern p = Pattern.compile(CardinalityPattern.cardinality2pattern.get(CardinalityEnum.MIN.name()), Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(cardinalityString);
			if (m.find())
			{
				minimumValue = m.group(1).trim();
				return new Cardinality(VizUtil.AFV_MIN_CARDINALITY, minimumValue, "");
			}
		}
		else if (cardinalityString.contains("<"))
		{
			// max cardinality
			Pattern p = Pattern.compile(CardinalityPattern.cardinality2pattern.get(CardinalityEnum.MAX.name()), Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(cardinalityString);
			if (m.find())
			{
				maximumValue = m.group(1).trim();
				return new Cardinality(VizUtil.AFV_MAX_CARDINALITY, "", maximumValue);
			}
		}
		else
		{
			// exact or interval
			Pattern p = Pattern.compile(CardinalityPattern.cardinality2pattern.get(CardinalityEnum.EXACTLY.name()), Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(cardinalityString);
			if (m.find())
			{
				// exact
				minimumValue = m.group(1).trim();
				maximumValue = minimumValue;
				return new Cardinality(VizUtil.AFV_EXACT_CARDINALITY, minimumValue, maximumValue);
			}
			else
			{
				// interval
				p = Pattern.compile(CardinalityPattern.cardinality2pattern.get(CardinalityEnum.MIN_MAX.name()), Pattern.CASE_INSENSITIVE);
				m = p.matcher(cardinalityString);
				if (m.find())
				{
					minimumValue = m.group(1).trim();
					maximumValue = m.group(2).trim();
					return new Cardinality(VizUtil.AFV_INTERVAL_CARDINALITY, minimumValue, maximumValue);
				}
			}

		}

		throw new IllegalStateException("Unknown cardinality string: \"" + cardinalityString + "\"");
	}

	public static Model createOrUpdateRelatedResource(Model model, Resource resource, Property property, Resource relatedResource)
	{
		if (model.contains(resource, property, relatedResource))
		{
			return model;
		}

		// resource.removeAll(property);

		resource.addProperty(property, relatedResource);

		return model;
	}

	public static List<String> replacePrefixesWithNamespaces(Model model, List<String> lines)
	{
		List<String> processedLines = new ArrayList<String>(lines.size());
		String regex = "(^|[\\s]+|\\^\\^)([a-z\\-]+):([a-zA-Z_\\-0-9#]+)";
		String commentRegex = ".*#(?![\\w]*>.*)";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);
			Matcher m = p.matcher(line);
			Matcher commentMatcher = commentPattern.matcher(line);
			boolean foundUri = false;
			boolean commentedOut = false;
			while (m.find())
			{
				foundUri = true;
				if (line.contains("#") && line.indexOf("#") < m.start(2))
				{
					if (commentMatcher.matches() && commentMatcher.end(1) < m.end(3))
					{
						commentedOut = true;
						processedLines.add(line);
						continue;
					}
				}

				String foundPrefix = m.group(2);
				String label = m.group(3).trim();

				String namespace = RdfUtil.prefixMap.get(foundPrefix);
				if (namespace == null || namespace.isEmpty())
				{
					log.info("Found URI with unknown prefix: " + m.group(2) + ":" + m.group(3));
					continue;
				}

				line = line.replaceAll(m.group(2) + ":" + m.group(3), "<" + namespace + label + ">");

			}

			if (!(foundUri && commentedOut))
			{
				processedLines.add(line);
			}
			log.debug("--> " + line);
		}
		return processedLines;
	}

	public static Model extractModelFromAdf(Path pathToInputFile, Model model)
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream()
		{
			private StringBuilder string = new StringBuilder();

			@Override
			public void write(int b)
			{
				string.append((char) b);
			}
		};
		String dataDescriptionAsString = StringUtils.EMPTY;

		// AdfService adfService = AdfServiceFactory.create();
		//
		// try (AdfFile adfFile = adfService.openFile(pathToInputFile, true))
		// {
		//
		// ((Model) adfFile.getDataDescription()).write(os, "NTriples");
		// }
		// catch (IOException e)
		// {
		// throw RuntimeExceptionUtil.mask(e);
		// }

		dataDescriptionAsString = os.toString(Charset.forName("UTF-8"));

		if (RdfCmap.removeBnodes)
		{
			Model tempModel = ModelFactory.createDefaultModel();

			log.info("removing blank nodes from data description");
			tempModel.read(new ByteArrayInputStream(dataDescriptionAsString.getBytes(StandardCharsets.UTF_8)), null, "NTriples");
			tempModel = RdfUtil.convertBlankNodesToNamedResources(tempModel);

			ByteArrayOutputStream output = new ByteArrayOutputStream()
			{
				private StringBuilder string = new StringBuilder();

				@Override
				public void write(int b)
				{
					string.append((char) b);
				}
			};

			tempModel.write(output, "NTriples");
			dataDescriptionAsString = output.toString(StandardCharsets.UTF_8);
		}

		dataDescriptionAsString = disguiseConceptsForVisualizationAsInstancesWithUrnUuid(dataDescriptionAsString);

		ByteArrayInputStream is = new ByteArrayInputStream(dataDescriptionAsString.getBytes(StandardCharsets.UTF_8));
		model.read(is, "NTriples");
		return model;
	}

	public static Model extractModelFromOntologyFile(Path pathToInputFile, Model model, String serialization) throws IOException
	{
		String modelAsString = StringUtils.EMPTY;

		if (RdfCmap.removeBnodes)
		{
			Model tempModel = ModelFactory.createDefaultModel();

			log.info("removing blank nodes from file: " + pathToInputFile.toString());
			tempModel.read(new FileInputStream(pathToInputFile.toFile()), null, serialization);
			tempModel = RdfUtil.convertBlankNodesToNamedResources(tempModel);

			ByteArrayOutputStream output = new ByteArrayOutputStream()
			{
				private StringBuilder string = new StringBuilder();

				@Override
				public void write(int b)
				{
					string.append((char) b);
				}
			};

			tempModel.write(output, serialization);
			modelAsString = output.toString(StandardCharsets.UTF_8);
		}
		else
		{
			List<String> lines = Files.readAllLines(pathToInputFile);
			modelAsString = StringUtils.join(lines, "\n");
		}

		modelAsString = disguiseConceptsForVisualizationAsInstancesWithUrnUuid(modelAsString);

		ByteArrayInputStream is = new ByteArrayInputStream(modelAsString.getBytes(StandardCharsets.UTF_8));
		model.read(is, null, serialization);
		return model;
	}

	private static String disguiseConceptsForVisualizationAsInstancesWithUrnUuid(String modelAsString)
	{
		if (RdfCmap.userSpecifiedInstanceNamespaces != null && !RdfCmap.userSpecifiedInstanceNamespaces.isEmpty())
		{
			for (Iterator<String> iterator = RdfCmap.userSpecifiedInstanceNamespaces.iterator(); iterator.hasNext();)
			{
				String namespace = iterator.next();
				if (namespace.equals("hdf:/data-package"))
				{
					log.info("Visualizing instances of " + namespace + " as urn:uuid:data-package.");
					modelAsString = modelAsString.replaceAll(Pattern.quote(namespace), CmapUtil.URN_UUID + "data-package");
				}
				else
				{
					String localTypeName = namespace.substring(namespace.lastIndexOf("/") + 1, namespace.length());
					log.info("Visualizing instances of IRI pattern " + namespace + "instance as urn:uuid:" + localTypeName + "instance.");
					modelAsString = modelAsString.replaceAll(Pattern.quote(namespace), CmapUtil.URN_UUID + localTypeName);
					String base = namespace.substring(0, namespace.length() - 1);
					modelAsString = modelAsString.replaceAll(Pattern.quote(base), CmapUtil.URN_UUID);
				}
			}
		}

		return modelAsString;
	}
}
