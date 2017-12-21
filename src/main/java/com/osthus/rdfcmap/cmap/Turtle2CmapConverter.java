package com.osthus.rdfcmap.cmap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.NumericEntityEscaper;
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
import com.osthus.rdfcmap.cmap.cardinality.Cardinality;
import com.osthus.rdfcmap.cmap.cardinality.StatementWithCardinality;
import com.osthus.rdfcmap.enums.ConceptProperty;
import com.osthus.rdfcmap.helper.ConceptRelation;
import com.osthus.rdfcmap.helper.LinkedConcept;
import com.osthus.rdfcmap.helper.VisualizationInfoBuilderResult;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

@SuppressWarnings("deprecation")
/**
 * Turtle2CmapConverter
 * 
 * rdf -> cmap 
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Turtle2CmapConverter
{
	private static final int MAX_CHARS = 15;

	List<Resource> resources = new ArrayList<>();

	private static Map<Resource, ConceptRelation> link2conceptRelations = new HashMap<Resource, ConceptRelation>();

	private static final Logger log = LogManager.getLogger("Logger");

	public void convert(Path pathToInputFile) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, JAXBException
	{
		convert(pathToInputFile, null);
	}

	public void convert(Path pathToInputFile, String[] additionalFiles) throws JAXBException, IOException, ParserConfigurationException, SAXException
	{
		log.info("Converting to cmap: " + pathToInputFile.toString()
				+ ((additionalFiles != null && additionalFiles.length > 0) ? " using additional files: " + StringUtils.join(additionalFiles, ", ") : ""));

		Model model = ModelFactory.createDefaultModel();
		log.info("Reading model from file: " + pathToInputFile.toString());

		if (pathToInputFile.toFile().getName().toLowerCase().endsWith("adf"))
		{
			model = CmapUtil.extractModelFromAdf(pathToInputFile, model);
		}
		else if (pathToInputFile.toFile().getName().toLowerCase().endsWith("owl") || pathToInputFile.toFile().getName().toLowerCase().endsWith("xml")
				|| pathToInputFile.toFile().getName().toLowerCase().endsWith("rdfxml"))
		{
			model = CmapUtil.extractModelFromOntologyFile(pathToInputFile, model, "RDF/XML");
		}
		else if (pathToInputFile.toFile().getName().toLowerCase().endsWith("n3"))
		{
			model = CmapUtil.extractModelFromOntologyFile(pathToInputFile, model, "N3");
		}
		else if (pathToInputFile.toFile().getName().toLowerCase().endsWith("ttl") && RdfCmap.userSpecifiedInstanceNamespaces != null
				&& !RdfCmap.userSpecifiedInstanceNamespaces.isEmpty())
		{
			model = CmapUtil.extractModelFromOntologyFile(pathToInputFile, model, "TTL");
		}
		else
		{
			model.read(pathToInputFile.toUri().toString(), null, "TTL");
		}

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

		link2conceptRelations = RdfUtil.determineConceptRelations(model);

		Map<String, Map<String, String>> conceptId2UiProperties = new HashMap<>();
		Map<String, Map<String, String>> linkId2UiProperties = new HashMap<>();
		Map<String, Map<String, String>> fullLinkId2UiProperties = new HashMap<>();
		Map<String, List<String>> connectionId2LinkAndConcept = new HashMap<>();
		Map<String, Map<String, String>> connectionId2UiProperties = new HashMap<>();
		Map<String, Map<String, String>> controlPointId2UiProperties = new HashMap<>();
		Map<String, Map<String, String>> imageId2UiProperties = new HashMap<>();
		Map<String, Set<LinkedConcept>> linkId2LinkedConcepts = new HashMap<>();

		if (!model.contains((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP))
		{
			// create completely new CMap
			StmtIterator stmtIterator = model.listStatements();
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				if (!statement.getSubject().toString().startsWith(CmapUtil.URN_UUID))
				{
					continue;
				}

				String subjectKey = statement.getSubject().toString();

				String vizKey = subjectKey.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX);
				if (model.containsResource(ResourceFactory.createResource(vizKey)))
				{
					Resource uiSubject = model.getResource(vizKey);
					if (!uiSubject.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT))
					{
						continue;
					}
				}

				Map<String, String> conceptProperties = CmapUtil.createOrRetrieveMapOfUiProperties(conceptId2UiProperties, subjectKey);
				conceptProperties = createTitle(model, statement.getSubject(), conceptProperties);
				conceptProperties = createShortComment(model, statement.getSubject(), conceptProperties);

				conceptId2UiProperties.put(subjectKey, conceptProperties);

				Model singleConceptModel = ModelFactory.createDefaultModel();
				StmtIterator instanceStmtIterator = model.listStatements(statement.getSubject(), (Property) null, (RDFNode) null);
				while (instanceStmtIterator.hasNext())
				{
					Statement instanceStatement = instanceStmtIterator.next();
					if (instanceStatement.getObject().isURIResource() && instanceStatement.getObject().toString().startsWith(CmapUtil.URN_UUID))
					{
						String linkKey = instanceStatement.getPredicate().asResource().toString();

						Set<LinkedConcept> linkedConcepts = createOrRetrieveSetOfLinkedConcepts(linkId2LinkedConcepts, linkKey);
						LinkedConcept linkedConcept = new LinkedConcept(statement.getSubject().toString(),
								instanceStatement.getObject().asResource().toString());
						linkedConcepts.add(linkedConcept);
						linkId2LinkedConcepts.put(linkKey, linkedConcepts);

						Map<String, String> linkProperties = CmapUtil.createOrRetrieveMapOfUiProperties(linkId2UiProperties, linkKey);
						linkProperties = createLinkTitle(model, instanceStatement.getPredicate().asResource(), linkProperties);
						linkProperties = createLinkShortComment(model, instanceStatement.getPredicate().asResource(), linkProperties);

						linkId2UiProperties.put(linkKey, linkProperties);

						String targetSubjectKey = instanceStatement.getObject().asResource().toString();
						Map<String, String> targetConceptProperties = CmapUtil.createOrRetrieveMapOfUiProperties(conceptId2UiProperties, targetSubjectKey);
						targetConceptProperties = createTitle(model, instanceStatement.getObject().asResource(), targetConceptProperties);
						targetConceptProperties = createShortComment(model, instanceStatement.getObject().asResource(), targetConceptProperties);

						conceptId2UiProperties.put(targetSubjectKey, targetConceptProperties);

						singleConceptModel.add(instanceStatement);
					}
					else if (instanceStatement.getObject().isURIResource() || instanceStatement.getObject().isLiteral())
					{
						singleConceptModel.add(instanceStatement);
					}
					else if (instanceStatement.getObject().isAnon())
					{
						String blankIri = instanceStatement.getObject().toString();
						singleConceptModel = addBlankNodes(model, singleConceptModel, instanceStatement, blankIri);
					}
					else
					{
						System.out.println("Warning! Unhandled state.");
					}
				}

				if (!singleConceptModel.isEmpty())
				{
					ByteArrayOutputStream output = new ByteArrayOutputStream()
					{
						private StringBuilder string = new StringBuilder();

						@Override
						public void write(int b)
						{
							string.append((char) b);
						}
					};

					singleConceptModel.write(output, "TTL");
					conceptProperties = CmapUtil.createOrRetrieveMapOfUiProperties(conceptId2UiProperties, subjectKey);
					conceptProperties.put(ConceptProperty.LONG_COMMENT.name(),
							NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(createHumanReadableRdf(model, output.toString()))));

					conceptId2UiProperties.put(subjectKey, conceptProperties);
				}
			}

			for (Entry<String, Set<LinkedConcept>> link : linkId2LinkedConcepts.entrySet())
			{
				for (LinkedConcept linkedConcept : link.getValue())
				{
					String linkKey = VizUtil.AFV_PREFIX + UUID.randomUUID().toString();
					String connectionFromId = VizUtil.AFV_PREFIX + UUID.randomUUID().toString();
					String connectionToId = VizUtil.AFV_PREFIX + UUID.randomUUID().toString();
					String from = linkedConcept.from.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX);
					String to = linkedConcept.to.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX);
					connectionId2LinkAndConcept.put(connectionFromId, Arrays.asList(from, linkKey));
					connectionId2LinkAndConcept.put(connectionToId, Arrays.asList(linkKey, to));

					Map<String, String> connectionFromProperties = CmapUtil.createOrRetrieveMapOfUiProperties(connectionId2UiProperties, connectionFromId);
					connectionFromProperties.put(ConceptProperty.CONNECTS_FROM.name(), from);
					connectionFromProperties.put(ConceptProperty.CONNECTS_TO.name(), linkKey);
					connectionId2UiProperties.put(connectionFromId, connectionFromProperties);

					Map<String, String> connectionToProperties = CmapUtil.createOrRetrieveMapOfUiProperties(connectionId2UiProperties, connectionToId);
					connectionToProperties.put(ConceptProperty.CONNECTS_FROM.name(), linkKey);
					connectionToProperties.put(ConceptProperty.CONNECTS_TO.name(), to);
					connectionId2UiProperties.put(connectionToId, connectionToProperties);

					Map<String, String> linkProperties = linkId2UiProperties.get(link.getKey());
					fullLinkId2UiProperties.put(linkKey, linkProperties);
				}
			}

			CmapUtil.createMap(model);
		}
		else
		{
			StmtIterator stmtIterator = model.listStatements();
			Set<String> handledUris = new HashSet<>();
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				Resource subject = statement.getSubject();
				if (!subject.isURIResource())
				{
					continue;
				}

				if (!subject.getURI().contains(CmapUtil.URN_UUID))
				{
					continue;
				}

				if (handledUris.contains(subject.getURI()))
				{
					continue;
				}

				Resource uiSubject = model.getResource(subject.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));

				if (uiSubject.getProperty(AFOUtil.RDF_TYPE) == null)
				{
					log.debug("Found instance without visualization: " + subject.getURI());
				}
				else if (VizUtil.AFV_CONCEPT.getURI().equals(uiSubject.getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
				{
					Map<String, String> conceptProperties = CmapUtil.createOrRetrieveMapOfUiProperties(conceptId2UiProperties, subject.getURI());
					conceptProperties = createTitle(model, subject, conceptProperties);
					conceptProperties = createShortComment(model, subject, conceptProperties);
					conceptProperties = createLongComment(model, subject, conceptProperties);
					conceptProperties = createNestedParentRelation(model, uiSubject, conceptProperties);
					conceptProperties = createAppearanceInfo(model, uiSubject, conceptProperties);

					conceptId2UiProperties.put(subject.getURI(), conceptProperties);
					handledUris.add(subject.getURI());

					imageId2UiProperties = handleImage(model, imageId2UiProperties, uiSubject);

				}
				else if (VizUtil.AFV_LINK.getURI().equals(uiSubject.getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
				{
					Map<String, String> linkProperties = CmapUtil.createOrRetrieveMapOfUiProperties(fullLinkId2UiProperties, subject.getURI());
					linkProperties = createLinkTitle(model, subject, linkProperties);
					linkProperties = createLinkCardinality(model, uiSubject, linkProperties);
					linkProperties = createLinkShortComment(model, subject, linkProperties);
					linkProperties = createNestedParentRelation(model, uiSubject, linkProperties);
					linkProperties = createLinkAppearanceInfo(model, uiSubject, linkProperties);

					fullLinkId2UiProperties.put(subject.getURI(), linkProperties);
					handledUris.add(subject.getURI());

					imageId2UiProperties = handleImage(model, imageId2UiProperties, uiSubject);
				}
				else if (VizUtil.AFV_CONNECTION.getURI().equals(uiSubject.getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
				{
					String connectionId = subject.getURI();
					String connectsFromId = uiSubject.getProperty(VizUtil.AFV_CONNECTS_FROM).getResource().getURI();
					connectsFromId = connectsFromId.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX);
					String connectsToId = uiSubject.getProperty(VizUtil.AFV_CONNECTS_TO).getResource().getURI();
					connectsToId = connectsToId.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX);
					connectionId2LinkAndConcept.put(connectionId, Arrays.asList(connectsFromId, connectsToId));

					Map<String, String> connectionProperties = CmapUtil.createOrRetrieveMapOfUiProperties(connectionId2UiProperties, connectionId);
					connectionProperties.put(ConceptProperty.CONNECTS_FROM.name(), connectsFromId);
					connectionProperties.put(ConceptProperty.CONNECTS_TO.name(), connectsToId);
					connectionProperties = createConnectionAppearanceInfo(model, uiSubject, connectionProperties);

					connectionId2UiProperties.put(connectionId, connectionProperties);

					if (!uiSubject.hasProperty(VizUtil.AFV_HAS_CONTROL_POINT))
					{
						continue;
					}

					StmtIterator controlPointIterator = model.listStatements(uiSubject, VizUtil.AFV_HAS_CONTROL_POINT, (RDFNode) null);
					while (controlPointIterator.hasNext())
					{
						Statement controlPointStatement = controlPointIterator.next();
						Resource controlPoint = controlPointStatement.getResource();
						String controlPointId = controlPoint.getURI();
						String x = controlPoint.getProperty(VizUtil.AFV_X_POSITION).getString();
						String y = controlPoint.getProperty(VizUtil.AFV_Y_POSITION).getString();
						String index = controlPoint.getProperty(AFOUtil.AFX_INDEX).getString();
						String parentConnectionId = uiSubject.getURI();
						Map<String, String> controlPointProperties = CmapUtil.createOrRetrieveMapOfUiProperties(controlPointId2UiProperties, controlPointId);
						controlPointProperties.put(ConceptProperty.X.name(), x);
						controlPointProperties.put(ConceptProperty.Y.name(), y);
						controlPointProperties.put(ConceptProperty.CONNECTION_ID.name(), parentConnectionId);
						controlPointProperties.put(ConceptProperty.INDEX.name(), index);
						controlPointId2UiProperties.put(controlPointId, controlPointProperties);
					}
				}
			}
		}

		VisualizationInfoBuilderResult visualizationInfoBuilderResult = VisualizationInfoBuilder.createOrUpdateVisualizationInfo(model, pathToInputFile,
				conceptId2UiProperties, fullLinkId2UiProperties, connectionId2UiProperties, controlPointId2UiProperties, imageId2UiProperties, resources);

		model = visualizationInfoBuilderResult.getModel();

		CxlWriter.generateCxlFromRdfModel(pathToInputFile, model);

		log.info(model.listStatements().toList().size() + " triples total after processing.");
	}

	private Map<String, String> createLinkCardinality(Model model, Resource subject, Map<String, String> linkProperties)
	{
		if (subject.hasProperty(VizUtil.AFV_HAS_CARDINALITY))
		{
			linkProperties.put(ConceptProperty.CARDINALITY.name(), CmapUtil.addCardinality(model, subject, "").trim());
		}
		else
		{
			linkProperties.put(ConceptProperty.CARDINALITY.name(), "");
		}
		return linkProperties;
	}

	private Map<String, Map<String, String>> handleImage(Model model, Map<String, Map<String, String>> imageId2UiProperties, Resource uiSubject)
	{
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE))
		{
			StmtIterator stmtIterator = model.listStatements(uiSubject, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				Resource imageResource = statement.getResource();
				Map<String, String> imageProperties = CmapUtil.createOrRetrieveMapOfUiProperties(imageId2UiProperties, imageResource.getURI());
				imageProperties = createImageInfo(model, imageResource, imageProperties);
				imageId2UiProperties.put(imageResource.getURI(), imageProperties);

				if (stmtIterator.hasNext())
				{
					log.error("Found resource with multiple related images: " + uiSubject.getURI());
					break;
				}
			}
		}

		return imageId2UiProperties;
	}

	private Map<String, String> createConnectionAppearanceInfo(Model model, Resource uiSubject, Map<String, String> connectionProperties)
	{
		String fromPos = uiSubject.getProperty(VizUtil.AFV_ANCHOR_FROM).getString();
		if (fromPos != null && !fromPos.isEmpty())
		{
			connectionProperties.put(ConceptProperty.ANCHOR_FROM.name(), fromPos);
		}

		String toPos = uiSubject.getProperty(VizUtil.AFV_ANCHOR_TO).getString();
		if (toPos != null && !toPos.isEmpty())
		{
			connectionProperties.put(ConceptProperty.ANCHOR_TO.name(), toPos);
		}

		String arrowHead = uiSubject.getProperty(VizUtil.AFV_ARROW_HEAD).getString();
		if (arrowHead != null && !arrowHead.isEmpty())
		{
			connectionProperties.put(ConceptProperty.ARROW_HEAD.name(), arrowHead);
		}

		String type = uiSubject.getProperty(VizUtil.AFV_LINE_TYPE).getString();
		if (type != null && !type.isEmpty())
		{
			connectionProperties.put(ConceptProperty.LINE_TYPE.name(), type);
		}

		return connectionProperties;
	}

	private Map<String, String> createLinkAppearanceInfo(Model model, Resource uiSubject, Map<String, String> linkProperties)
	{
		String x = uiSubject.getProperty(VizUtil.AFV_X_POSITION).getString();
		String y = uiSubject.getProperty(VizUtil.AFV_Y_POSITION).getString();
		String width = uiSubject.getProperty(VizUtil.AFV_WIDTH).getString();
		String height = uiSubject.getProperty(VizUtil.AFV_HEIGHT).getString();
		String minWidth = uiSubject.getProperty(VizUtil.AFV_MINIMUM_WIDTH).getString();
		String minHeight = uiSubject.getProperty(VizUtil.AFV_MINIMUM_HEIGHT).getString();
		String fontSize = uiSubject.getProperty(VizUtil.AFV_HAS_FONT).getProperty(VizUtil.AFV_SIZE).getString();
		String fontColor = uiSubject.getProperty(VizUtil.AFV_HAS_FONT).getProperty(VizUtil.AFV_COLOR).getString();
		String borderColor = uiSubject.getProperty(VizUtil.AFV_HAS_BORDER).getProperty(VizUtil.AFV_COLOR).getString();
		String backgroundColor = uiSubject.getProperty(VizUtil.AFV_BACKGROUND_COLOR).getString();
		String shadowColor = uiSubject.getProperty(VizUtil.AFV_SHADOW_COLOR).getString();

		if (x != null && !x.isEmpty())
		{
			linkProperties.put(ConceptProperty.X.name(), x);
		}

		if (y != null && !y.isEmpty())
		{
			linkProperties.put(ConceptProperty.Y.name(), y);
		}

		if (width != null && !width.isEmpty())
		{
			linkProperties.put(ConceptProperty.WIDTH.name(), width);
		}

		if (height != null && !height.isEmpty())
		{
			linkProperties.put(ConceptProperty.HEIGHT.name(), height);
		}

		if (minWidth != null && !minWidth.isEmpty())
		{
			linkProperties.put(ConceptProperty.MIN_WIDTH.name(), minWidth);
		}

		if (minHeight != null && !minHeight.isEmpty())
		{
			linkProperties.put(ConceptProperty.MIN_HEIGHT.name(), minHeight);
		}

		if (fontSize != null && !fontSize.isEmpty())
		{
			linkProperties.put(ConceptProperty.FONT_SIZE.name(), fontSize);
		}

		if (fontColor != null && !fontColor.isEmpty())
		{
			linkProperties.put(ConceptProperty.FONT_COLOR.name(), fontColor);
		}

		if (borderColor != null && !borderColor.isEmpty())
		{
			linkProperties.put(ConceptProperty.BORDER_COLOR.name(), borderColor);
		}

		if (backgroundColor != null && !backgroundColor.isEmpty())
		{
			linkProperties.put(ConceptProperty.BACKGROUND_COLOR.name(), backgroundColor);
		}

		if (shadowColor != null && !shadowColor.isEmpty())
		{
			linkProperties.put(ConceptProperty.SHADOW_COLOR.name(), shadowColor);
		}

		String backgroundImage = null;
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE))
		{
			backgroundImage = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getURI();
		}
		if (backgroundImage != null && !backgroundImage.isEmpty())
		{
			linkProperties.put(ConceptProperty.BACKGROUND_IMAGE.name(), backgroundImage);
		}

		String backgroundImageStyle = null;
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE) && uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_STYLE))
		{
			backgroundImageStyle = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_STYLE).getString();
		}
		if (backgroundImageStyle != null && !backgroundImageStyle.isEmpty())
		{
			linkProperties.put(ConceptProperty.BACKGROUND_IMAGE_STYLE.name(), backgroundImageStyle);
		}

		String backgroundImageLayout = null;
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE) && uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_LAYOUT))
		{
			backgroundImageLayout = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_LAYOUT).getString();
		}
		if (backgroundImageLayout != null && !backgroundImageLayout.isEmpty())
		{
			linkProperties.put(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name(), backgroundImageLayout);
		}

		return linkProperties;
	}

	private Map<String, String> createAppearanceInfo(Model model, Resource uiSubject, Map<String, String> conceptProperties)
	{
		String x = uiSubject.getProperty(VizUtil.AFV_X_POSITION).getString();
		String y = uiSubject.getProperty(VizUtil.AFV_Y_POSITION).getString();
		String width = uiSubject.getProperty(VizUtil.AFV_WIDTH).getString();
		String height = uiSubject.getProperty(VizUtil.AFV_HEIGHT).getString();
		String fontStyle = uiSubject.getProperty(VizUtil.AFV_HAS_FONT).getProperty(VizUtil.AFV_STYLE).getString();
		String fontSize = uiSubject.getProperty(VizUtil.AFV_HAS_FONT).getProperty(VizUtil.AFV_SIZE).getString();
		String backgroundColor = uiSubject.getProperty(VizUtil.AFV_BACKGROUND_COLOR).getString();
		String expanded = StringUtils.EMPTY;
		if (uiSubject.hasProperty(VizUtil.AFV_EXPANDED))
		{
			expanded = uiSubject.getProperty(VizUtil.AFV_EXPANDED).getString();
		}

		if (x != null && !x.isEmpty())
		{
			conceptProperties.put(ConceptProperty.X.name(), x);
		}

		if (y != null && !y.isEmpty())
		{
			conceptProperties.put(ConceptProperty.Y.name(), y);
		}

		if (width != null && !width.isEmpty())
		{
			conceptProperties.put(ConceptProperty.WIDTH.name(), width);
		}

		if (height != null && !height.isEmpty())
		{
			conceptProperties.put(ConceptProperty.HEIGHT.name(), height);
		}

		if (fontStyle != null && !fontStyle.isEmpty())
		{
			conceptProperties.put(ConceptProperty.FONT_STYLE.name(), fontStyle);
		}

		if (fontSize != null && !fontSize.isEmpty())
		{
			conceptProperties.put(ConceptProperty.FONT_SIZE.name(), fontSize);
		}

		if (backgroundColor != null && !backgroundColor.isEmpty())
		{
			conceptProperties.put(ConceptProperty.BACKGROUND_COLOR.name(), backgroundColor);
		}

		if (expanded != null && !expanded.isEmpty())
		{
			conceptProperties.put(ConceptProperty.EXPANDED.name(), expanded);
		}

		String backgroundImage = null;
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE))
		{
			backgroundImage = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getURI();
		}
		if (backgroundImage != null && !backgroundImage.isEmpty())
		{
			conceptProperties.put(ConceptProperty.BACKGROUND_IMAGE.name(), backgroundImage);
		}

		String backgroundImageStyle = null;
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE) && uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_STYLE))
		{
			backgroundImageStyle = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_STYLE).getString();
		}
		if (backgroundImageStyle != null && !backgroundImageStyle.isEmpty())
		{
			conceptProperties.put(ConceptProperty.BACKGROUND_IMAGE_STYLE.name(), backgroundImageStyle);
		}

		String backgroundImageLayout = null;
		if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE) && uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_LAYOUT))
		{
			backgroundImageLayout = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_LAYOUT).getString();
		}
		if (backgroundImageLayout != null && !backgroundImageLayout.isEmpty())
		{
			conceptProperties.put(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name(), backgroundImageLayout);
		}

		return conceptProperties;
	}

	private Map<String, String> createLinkShortComment(Model model, Resource resource, Map<String, String> linkProperties)
	{
		StringBuilder linkPopup = new StringBuilder();
		resource = Cmap2TurtleConverter.tryToExtractLinkFromAfxAndObo(model, resource);
		StmtIterator linkDefinitionIterator = model.listStatements(resource, AFOUtil.SKOS_DEFINITION, (RDFNode) null);
		String linkDefinition = StringUtils.EMPTY;
		while (linkDefinitionIterator.hasNext())
		{
			linkDefinition = linkDefinitionIterator.next().getLiteral().getString();
		}
		if (!linkDefinition.isEmpty())
		{
			String id = RdfUtil.getNamespaceMap().get(resource.getNameSpace()) + ":" + resource.getLocalName() + "\n\n";
			linkPopup.append(id);
			linkPopup.append("definition: \n" + linkDefinition);
		}

		if (linkPopup.length() > 0)
		{
			linkProperties.put(ConceptProperty.SHORT_COMMENT.name(),
					NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(linkPopup.toString())));
		}

		return linkProperties;
	}

	private Map<String, String> createLinkTitle(Model model, Resource resource, Map<String, String> linkProperties)
	{
		if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
		{
			String title = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getLiteral().toString();
			linkProperties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else if (resource.hasProperty(AFOUtil.DCT_TITLE))
		{
			String title = model.listStatements(resource, AFOUtil.DCT_TITLE, (RDFNode) null).next().getLiteral().toString();
			linkProperties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else if (resource.hasProperty(AFOUtil.RDFS_LABEL))
		{
			String title = model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getLiteral().toString();
			linkProperties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else
		{
			String prefix = RdfUtil.getNamespaceMap().get(resource.getNameSpace());
			if (prefix != null && !prefix.isEmpty())
			{
				linkProperties.put(ConceptProperty.TITLE.name(), breakString(prefix + ":" + resource.getLocalName(), MAX_CHARS));
			}
			else
			{
				linkProperties.put(ConceptProperty.TITLE.name(), breakString(resource.getURI(), MAX_CHARS));
			}
		}
		return linkProperties;
	}

	private Map<String, String> createImageInfo(Model model, Resource resource, Map<String, String> imageProperties)
	{
		if (resource.hasProperty(VizUtil.AFV_IDENTIFIER))
		{
			String id = model.listStatements(resource, VizUtil.AFV_IDENTIFIER, (RDFNode) null).next().getLiteral().toString();
			imageProperties.put(ConceptProperty.BACKGROUND_IMAGE.name(), id);
		}
		else
		{
			log.debug("Found image without visualization identifier. Image URI: " + resource.getURI());
		}

		if (resource.hasProperty(VizUtil.AFV_BYTES))
		{
			String bytes = model.listStatements(resource, VizUtil.AFV_BYTES, (RDFNode) null).next().getLiteral().toString();
			imageProperties.put(ConceptProperty.BACKGROUND_IMAGE_BYTES.name(), bytes);
		}

		return imageProperties;
	}

	private Map<String, String> createLongComment(Model model, Resource resource, Map<String, String> properties)
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream()
		{
			private StringBuilder string = new StringBuilder();

			@Override
			public void write(int b)
			{
				string.append((char) b);
			}
		};

		Model singleConceptModel = ModelFactory.createDefaultModel();
		List<Statement> statements = model.listStatements(resource, (Property) null, (RDFNode) null).toList();

		StmtIterator anonStmtIterator = model.listStatements(resource, (Property) null, (RDFNode) null);
		while (anonStmtIterator.hasNext())
		{
			statements = CmapUtil.addStatementsWithBlankNodes(model, anonStmtIterator.next(), statements);
		}
		singleConceptModel.add(statements);

		singleConceptModel.write(output, "TTL");
		String singleConceptModelString = output.toString();
		singleConceptModelString = createHumanReadableRdf(model, singleConceptModelString);
		singleConceptModelString = addCardinalitiesToLongComment(model, singleConceptModel, resource, singleConceptModelString);
		properties.put(ConceptProperty.LONG_COMMENT.name(),
				NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(singleConceptModelString)));

		return properties;
	}

	private String addCardinalitiesToLongComment(Model model, Model singleConceptModel, Resource resource, String singleConceptModelString)
	{
		Resource uiResource = model.getResource(resource.getURI().replaceAll(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));

		if (!model.listStatements(uiResource, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT).hasNext())
		{
			return singleConceptModelString;
		}

		List<StatementWithCardinality> statementsWithCardinality = new ArrayList<>();
		for (ConceptRelation conceptRelation : link2conceptRelations.values())
		{
			if (conceptRelation.from.equals(uiResource))
			{
				if (model.listStatements(conceptRelation.link, VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).hasNext())
				{
					Resource cardinality = model.listStatements(conceptRelation.link, VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).next().getResource();
					Resource cardinalityType = model.listStatements(cardinality, AFOUtil.RDF_TYPE, (RDFNode) null).next().getResource();
					String minCardinality = model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).next().getString();
					String maxCardinality = model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).next().getString();
					String cardinalityString = (new Cardinality(cardinalityType, minCardinality, maxCardinality)).toString();

					if (cardinalityString.equals(CmapUtil.MIN_ZERO))
					{
						// this is the default for absence of cardinality
						continue;
					}

					String propertyTitle = model.listStatements(conceptRelation.link, AFOUtil.AFX_HAS_OBJECT, (RDFNode) null).next().getResource()
							.getProperty(AFOUtil.DCT_TITLE).getString();
					if (!propertyTitle.contains(":"))
					{
						propertyTitle = "af-x:" + propertyTitle;
					}

					statementsWithCardinality
							.add(new StatementWithCardinality(uiResource.getURI(), propertyTitle, conceptRelation.to.getURI(), cardinalityString));
				}
			}
		}

		StmtIterator stmtIterator = model.listStatements(uiResource, VizUtil.AFV_HAS_HIDDEN_PROPERTY, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (model.listStatements(statement.getResource(), VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).hasNext())
			{
				Resource cardinality = model.listStatements(statement.getResource(), VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).next().getResource();
				Resource cardinalityType = model.listStatements(cardinality, AFOUtil.RDF_TYPE, (RDFNode) null).next().getResource();
				String minCardinality = model.listStatements(cardinality, AFOUtil.AFX_MINIMUM_VALUE, (RDFNode) null).next().getString();
				String maxCardinality = model.listStatements(cardinality, AFOUtil.AFX_MAXIMUM_VALUE, (RDFNode) null).next().getString();
				String cardinalityString = (new Cardinality(cardinalityType, minCardinality, maxCardinality)).toString();

				if (cardinalityString.equals(CmapUtil.MIN_ZERO))
				{
					// this is the default for absence of cardinality
					continue;
				}

				String propertyTitle = model.listStatements(statement.getResource(), AFOUtil.DCT_TITLE, (RDFNode) null).next().getString();
				if (!propertyTitle.contains(":"))
				{
					propertyTitle = "af-x:" + propertyTitle;
				}

				String objectAsString = model.listStatements(statement.getResource(), AFOUtil.AFX_HAS_VALUE, (RDFNode) null).next().getString();

				statementsWithCardinality.add(new StatementWithCardinality(uiResource.getURI(), propertyTitle, objectAsString, cardinalityString));
			}

		}

		singleConceptModelString = singleConceptModelString.replaceAll("\r\n", " ");
		singleConceptModelString = singleConceptModelString.replaceAll("\n", " ");
		List<String> lines = Arrays.asList(singleConceptModelString.split(";"));

		List<String> processedLines = new ArrayList<String>(lines.size());

		String regex = "(^|[\\s]+)([<]{0,2}([a-z\\-]+):([a-z\\-\\s/\\(\\)0-9]+)[>]{0,2})([\"\\:\\w\\s<]+|$)";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

		String commentRegex = ".*#(?![\\w]*>.*)";
		Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);

		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);

			Matcher m = p.matcher(line);
			Matcher commentMatcher = commentPattern.matcher(line);
			boolean foundId = false;
			boolean commentedOut = false;
			while (m.find())
			{
				foundId = true;
				if (line.contains("#") && line.indexOf("#") < m.start(1))
				{
					if (commentMatcher.matches() && commentMatcher.end(1) < m.end(3))
					{
						commentedOut = true;
						processedLines.add(line);
						continue;
					}
				}

				String prefix = m.group(3);
				String label = m.group(4);
				String object = m.group(5);

				if (prefix == null || prefix.trim().isEmpty() || label == null || label.trim().isEmpty() || object == null || object.trim().isEmpty())
				{
					continue;
				}

				object = object.trim().replaceAll("\"", "").replaceAll("<", "").replaceAll(CmapUtil.URN_UUID, "");

				String labelWithoutCardinality = prefix.trim() + ":" + label.trim();

				String labelWithCardinality = StringUtils.EMPTY;
				for (StatementWithCardinality statementWithCardinality : statementsWithCardinality)
				{
					if (labelWithoutCardinality.equals(statementWithCardinality.getProperty())
							&& (statementWithCardinality.getObject().contains(object) || object.contains(statementWithCardinality.getObject())))
					{
						labelWithCardinality = labelWithoutCardinality + " " + statementWithCardinality.getCardinality();
						break;
					}
				}

				if (labelWithCardinality.isEmpty())
				{
					continue;
				}

				if (labelWithoutCardinality.toLowerCase().contains("af-x") || labelWithoutCardinality.toLowerCase().contains("afx"))
				{
					// af-x property replace witch <<af-x:property>>
					line = line.replaceAll("<<" + Pattern.quote(labelWithoutCardinality) + ">>", "<<" + labelWithCardinality + ">>");
				}
				else
				{
					// other properties replace with other:property
					line = line.replaceAll(Pattern.quote(labelWithoutCardinality), "<<" + labelWithCardinality + ">>");
				}
			}

			if (!(foundId && commentedOut))
			{
				processedLines.add(line);
			}
			log.debug("--> " + line);
		}

		return StringUtils.join(processedLines.toArray(), ";\n");
	}

	private Map<String, String> createNestedParentRelation(Model model, Resource resource, Map<String, String> properties)
	{
		if (!resource.hasProperty(VizUtil.AFV_HAS_PARENT))
		{
			return properties;
		}

		properties.put(ConceptProperty.PARENT.name(), model.listStatements(resource, VizUtil.AFV_HAS_PARENT, (RDFNode) null).next().getResource().getURI());

		return properties;
	}

	public String createHumanReadableRdf(Model model, String singleConceptModelString)
	{
		List<String> lines = Arrays.asList(singleConceptModelString.split("\n"));
		List<String> processedLines = new ArrayList<String>(lines.size());
		String regex = "(^|[\\w\\s]+)<(http\\:\\/\\/purl\\.allotrope\\.org\\/ontologies\\/[a-z]*#)([A-Z]{3}_[0-9]{7})>[\\w\\s.]*";
		String regexUri = "(<((http[s]?|ftp):\\/)?\\/?([^:\\/\\s]+)((\\/\\w+)*\\/)([\\w\\-\\.]+[^#?\\s]+)(.*)>)";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Pattern patternUri = Pattern.compile(regexUri, Pattern.CASE_INSENSITIVE);
		String commentRegex = ".*#(?![\\w]*>.*)";
		Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);
			Matcher m = p.matcher(line);
			Matcher commentMatcher = commentPattern.matcher(line);
			boolean foundId = false;
			boolean commentedOut = false;
			while (m.find())
			{
				foundId = true;
				if (line.contains("#") && line.indexOf("#") < m.start(2))
				{
					if (commentMatcher.matches() && commentMatcher.end(1) < m.end(3))
					{
						commentedOut = true;
						processedLines.add(line);
						continue;
					}
				}

				String resourceIri = m.group(2) + m.group(3);
				Resource resource = model.getResource(resourceIri);
				if (!resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
				{
					log.error("Error during replacement as human readable identifier: " + m.group(2) + m.group(3));
				}

				String label = resource.getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
				String prefix = RdfUtil.getNamespaceMap().get(resource.getNameSpace());
				line = line.replaceAll("<" + m.group(2), "<<" + prefix + ":");
				line = line.replaceAll(m.group(3) + ">", label + ">>");
			}

			if (!(foundId && commentedOut))
			{
				processedLines.add(line);
			}
			log.debug("--> " + line);
		}

		processedLines = replaceNamespacesWithPrefixes(model, processedLines);
		return StringUtils.join(processedLines.toArray(), "\n");
	}

	private List<String> replaceNamespacesWithPrefixes(Model model, List<String> lines)
	{
		List<String> processedLines = new ArrayList<String>(lines.size());
		String regexUri = "(<((http[s]?|ftp):\\/)?\\/?([^:\\/\\s]+)((\\/\\w+)*\\/)([\\w\\-\\.]+[^#?\\s]+)>)";
		Pattern patternUri = Pattern.compile(regexUri, Pattern.CASE_INSENSITIVE);
		String commentRegex = ".*#(?![\\w]*>.*)";
		Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);
			Matcher m = patternUri.matcher(line);
			Matcher commentMatcher = commentPattern.matcher(line);
			boolean foundUri = false;
			boolean commentedOut = false;
			while (m.find())
			{
				foundUri = true;
				if (line.contains("#") && line.indexOf("#") < m.start(1))
				{
					if (commentMatcher.matches() && commentMatcher.end(1) < m.end(2))
					{
						commentedOut = true;
						processedLines.add(line);
						continue;
					}
				}
				boolean isNamespaceKnown = false;
				for (String namespace : RdfUtil.getNamespaceMap().keySet())
				{
					if (m.group(1).contains(namespace))
					{
						isNamespaceKnown = true;
						String replacement = m.group(1).replaceAll("<", "").replaceAll(">", "").replace(namespace,
								RdfUtil.getNamespaceMap().get(namespace) + ":");
						line = line.replace(m.group(1), replacement);
						break;
					}
				}

				if (!isNamespaceKnown)
				{
					log.info("Unknown namespace in URI: " + m.group(1));
				}
			}

			if (!(foundUri && commentedOut))
			{
				processedLines.add(line);
			}
			log.debug("--> " + line);
		}
		return processedLines;
	}

	private Map<String, String> createShortComment(Model model, Resource resource, Map<String, String> properties)
	{
		if (resource.getURI().startsWith(CmapUtil.URN_UUID + "AF") || resource.getURI().startsWith(CmapUtil.URN_UUID + "BFO")
				|| resource.getURI().startsWith(CmapUtil.URN_UUID + "IAO"))
		{
			// this is a disguised class
			StringBuilder popupText = new StringBuilder();
			StmtIterator labelIterator = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null);
			String label = StringUtils.EMPTY;
			boolean hasDefinition = false;
			while (labelIterator.hasNext())
			{
				label = labelIterator.next().getLiteral().getString();
			}
			if (label.isEmpty())
			{
				labelIterator = model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null);
				while (labelIterator.hasNext())
				{
					label = labelIterator.next().getLiteral().getString();
				}
			}

			StmtIterator definitionIterator = model.listStatements(resource, AFOUtil.SKOS_DEFINITION, (RDFNode) null);
			String definition = StringUtils.EMPTY;
			while (definitionIterator.hasNext())
			{
				definition = definitionIterator.next().getLiteral().getString();
				hasDefinition = true;
			}
			if (definition.isEmpty())
			{
				definitionIterator = model.listStatements(resource, AFOUtil.OBO_DEFINITION, (RDFNode) null);
				while (definitionIterator.hasNext())
				{
					definition = definitionIterator.next().getLiteral().getString();
				}
			}
			if (definition.isEmpty())
			{
				definitionIterator = model.listStatements(resource, AFOUtil.OBO_DEFINITION_DISGUISED, (RDFNode) null);
				while (definitionIterator.hasNext())
				{
					definition = definitionIterator.next().getLiteral().getString();
				}
			}

			if (!label.isEmpty())
			{
				popupText.append("prefLabel: \n" + label + "\n\n");
			}
			if (!definition.isEmpty())
			{
				popupText.append("definition: \n" + definition + "\n\n");
			}

			if (popupText.length() == 0 && !hasDefinition && model.listStatements(resource, AFOUtil.RDF_TYPE, AFOUtil.OWL_NAMED_INDIVIDUAL).hasNext())
			{
				popupText.append("Concept is a named individual.");
			}

			if (resource.hasProperty(AFOUtil.DCT_DESCRIPTION))
			{
				popupText.append(
						"description: \n" + model.listStatements(resource, AFOUtil.DCT_DESCRIPTION, (RDFNode) null).next().getLiteral().toString() + "\n\n");
			}

			if (resource.hasProperty(AFOUtil.OBO_ELUCIDATION))
			{
				StmtIterator exampleIterator = model.listStatements(resource, AFOUtil.OBO_ELUCIDATION, (RDFNode) null);
				while (exampleIterator.hasNext())
				{
					Statement statement = exampleIterator.next();
					popupText.append("elucidation: \n" + statement.getLiteral().toString() + "\n\n");
				}
			}

			if (resource.hasProperty(AFOUtil.OBO_ELUCIDATION_DISGUISED))
			{
				StmtIterator exampleIterator = model.listStatements(resource, AFOUtil.OBO_ELUCIDATION_DISGUISED, (RDFNode) null);
				while (exampleIterator.hasNext())
				{
					Statement statement = exampleIterator.next();
					popupText.append("elucidation: \n" + statement.getLiteral().toString() + "\n\n");
				}
			}

			if (resource.hasProperty(AFOUtil.OBO_EXAMPLE))
			{
				StmtIterator exampleIterator = model.listStatements(resource, AFOUtil.OBO_EXAMPLE, (RDFNode) null);
				while (exampleIterator.hasNext())
				{
					Statement statement = exampleIterator.next();
					popupText.append("example: \n" + statement.getLiteral().toString() + "\n\n");
				}
			}

			if (resource.hasProperty(AFOUtil.OBO_EXAMPLE_DISGUISED))
			{
				StmtIterator exampleIterator = model.listStatements(resource, AFOUtil.OBO_EXAMPLE_DISGUISED, (RDFNode) null);
				while (exampleIterator.hasNext())
				{
					Statement statement = exampleIterator.next();
					popupText.append("example: \n" + statement.getLiteral().toString() + "\n\n");
				}
			}

			if (popupText.length() > 0)
			{
				String namespacePrefix = RdfUtil.getNamespaceMap().get(resource.getNameSpace());
				String id = StringUtils.EMPTY;
				if (namespacePrefix != null && !namespacePrefix.isEmpty())
				{
					id = namespacePrefix + ":";
				}
				id = id + resource.getLocalName() + "\n\n";
				popupText.insert(0, id);
				properties.put(ConceptProperty.SHORT_COMMENT.name(),
						NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(popupText.toString())));
			}
		}
		else if (resource.hasProperty(AFOUtil.RDF_TYPE))
		{
			StringBuilder popupText = new StringBuilder();
			StmtIterator parentTypeIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
			boolean hasParentTypeWithDefinition = false;
			while (parentTypeIterator.hasNext())
			{
				Resource parentType = parentTypeIterator.next().getObject().asResource();
				StmtIterator parentTypeLabelIterator = model.listStatements(parentType, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null);
				String parentTypeLabel = StringUtils.EMPTY;
				while (parentTypeLabelIterator.hasNext())
				{
					parentTypeLabel = parentTypeLabelIterator.next().getLiteral().getString();
				}
				StmtIterator parentTypeDefinitionIterator = model.listStatements(parentType, AFOUtil.SKOS_DEFINITION, (RDFNode) null);
				String parentTypeDefinition = StringUtils.EMPTY;
				while (parentTypeDefinitionIterator.hasNext())
				{
					parentTypeDefinition = parentTypeDefinitionIterator.next().getLiteral().getString();
					hasParentTypeWithDefinition = true;
					String id = RdfUtil.getNamespaceMap().get(parentType.getNameSpace()) + ":" + parentType.getLocalName() + "\n\n";
					popupText.append(id);
				}
				if (!parentTypeLabel.isEmpty())
				{
					popupText.append("class prefLabel: \n" + parentTypeLabel + "\n\n");
				}
				if (!parentTypeDefinition.isEmpty())
				{
					popupText.append("class definition: \n" + parentTypeDefinition + "\n\n");
				}
			}

			if (resource.hasProperty(AFOUtil.DCT_DESCRIPTION))
			{
				popupText.append("description: \n" + model.listStatements(resource, AFOUtil.DCT_DESCRIPTION, (RDFNode) null).next().getLiteral().toString());
			}

			if (popupText.length() == 0 && !hasParentTypeWithDefinition
					&& model.listStatements(resource, AFOUtil.RDF_TYPE, AFOUtil.OWL_NAMED_INDIVIDUAL).hasNext())
			{
				popupText.append("Concept is a named individual.");
			}

			if (popupText.length() == 0 && !hasParentTypeWithDefinition
					&& model.listStatements(resource, AFOUtil.OBO_DEFINITION_DISGUISED, (RDFNode) null).hasNext())
			{
				popupText.append(model.listStatements(resource, AFOUtil.OBO_DEFINITION_DISGUISED, (RDFNode) null).next().getString());
			}

			if (popupText.length() > 0)
			{
				properties.put(ConceptProperty.SHORT_COMMENT.name(),
						NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(popupText.toString())));
			}
		}
		else if (resource.hasProperty(AFOUtil.SKOS_DEFINITION))
		{
			properties.put(ConceptProperty.SHORT_COMMENT.name(),
					"definition: \n" + model.listStatements(resource, AFOUtil.SKOS_DEFINITION, (RDFNode) null).next().getLiteral().toString());
		}
		else if (resource.hasProperty(AFOUtil.DCT_DESCRIPTION))
		{
			properties.put(ConceptProperty.SHORT_COMMENT.name(),
					"description: \n" + model.listStatements(resource, AFOUtil.DCT_DESCRIPTION, (RDFNode) null).next().getLiteral().toString());
		}
		else if (resource.hasProperty(AFOUtil.OBO_DEFINITION))
		{
			properties.put(ConceptProperty.SHORT_COMMENT.name(),
					"obo:definition: \n" + model.listStatements(resource, AFOUtil.OBO_DEFINITION, (RDFNode) null).next().getLiteral().toString());
		}
		else if (resource.hasProperty(AFOUtil.OBO_DEFINITION_DISGUISED))
		{
			properties.put(ConceptProperty.SHORT_COMMENT.name(),
					"obo:definition: \n" + model.listStatements(resource, AFOUtil.OBO_DEFINITION_DISGUISED, (RDFNode) null).next().getLiteral().toString());
		}
		else if (resource.hasProperty(AFOUtil.OBO_ELUCIDATION))
		{
			properties.put(ConceptProperty.SHORT_COMMENT.name(),
					"obo:elucidation: \n" + model.listStatements(resource, AFOUtil.OBO_ELUCIDATION, (RDFNode) null).next().getLiteral().toString());
		}
		else if (resource.hasProperty(AFOUtil.OBO_ELUCIDATION_DISGUISED))
		{
			properties.put(ConceptProperty.SHORT_COMMENT.name(),
					"obo:elucidation: \n" + model.listStatements(resource, AFOUtil.OBO_ELUCIDATION_DISGUISED, (RDFNode) null).next().getLiteral().toString());
		}

		return properties;
	}

	public Map<String, String> createTitle(Model model, Resource resource, Map<String, String> properties)
	{
		if (resource.hasProperty(AFOUtil.DCT_TITLE))
		{
			String title = model.listStatements(resource, AFOUtil.DCT_TITLE, (RDFNode) null).next().getLiteral().toString();
			properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
		{
			String title = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getLiteral().toString();
			properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else if (resource.hasProperty(AFOUtil.DCT_IDENTIFIER))
		{
			String title = model.listStatements(resource, AFOUtil.DCT_IDENTIFIER, (RDFNode) null).next().getLiteral().toString();
			properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else if (resource.hasProperty(AFOUtil.RDFS_LABEL))
		{
			String title = model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getLiteral().toString();
			properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else if (resource.hasProperty(AFOUtil.RDF_TYPE))
		{
			StmtIterator stmtIterator = model.listStatements(resource, AFOUtil.RDF_TYPE, (RDFNode) null);
			boolean hasTypeLabel = false;
			String title = "instance of";
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				Resource typeResource = statement.getResource();
				if (typeResource.hasProperty(AFOUtil.DCT_TITLE))
				{
					title = title + " " + model.listStatements(typeResource, AFOUtil.DCT_TITLE, (RDFNode) null).next().getLiteral().toString();
					hasTypeLabel = true;
				}
				else if (typeResource.hasProperty(AFOUtil.DCT_IDENTIFIER))
				{
					title = title + " " + model.listStatements(typeResource, AFOUtil.DCT_IDENTIFIER, (RDFNode) null).next().getLiteral().toString();
					hasTypeLabel = true;
				}
				else if (typeResource.hasProperty(AFOUtil.RDFS_LABEL))
				{
					title = title + " " + model.listStatements(typeResource, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getLiteral().toString();
					hasTypeLabel = true;
				}
				else if (typeResource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
				{
					title = title + " " + model.listStatements(typeResource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getLiteral().toString();
					hasTypeLabel = true;
				}
				else if (typeResource.equals(AFOUtil.OWL_CLASS))
				{
					// class disguised as instance for viz
					if (resource.hasProperty(AFOUtil.DCT_TITLE))
					{
						title = model.listStatements(resource, AFOUtil.DCT_TITLE, (RDFNode) null).next().getLiteral().toString();
						hasTypeLabel = true;
					}
					else if (resource.hasProperty(AFOUtil.DCT_IDENTIFIER))
					{
						title = model.listStatements(resource, AFOUtil.DCT_IDENTIFIER, (RDFNode) null).next().getLiteral().toString();
						hasTypeLabel = true;
					}
					else if (resource.hasProperty(AFOUtil.RDFS_LABEL))
					{
						title = model.listStatements(resource, AFOUtil.RDFS_LABEL, (RDFNode) null).next().getLiteral().toString();
						hasTypeLabel = true;
					}
					else if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
					{
						title = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getLiteral().toString();
						hasTypeLabel = true;
					}
					else
					{
						title = resource.getLocalName();
						hasTypeLabel = true;
					}
				}
				else
				{
					title = title + " " + typeResource.getLocalName();
					hasTypeLabel = true;
				}
			}

			if (hasTypeLabel)
			{
				properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
			}
			else if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
			{
				title = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getLiteral().toString();
				properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
			}
			else
			{
				if (resource.getURI().startsWith(CmapUtil.URN_UUID)
						&& resource.getURI().toLowerCase().matches(CmapUtil.URN_UUID + "[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
				{
					properties.put(ConceptProperty.TITLE.name(), "instance");
				}
				else if (resource.getURI().startsWith(CmapUtil.URN_UUID)
						&& resource.getURI().toLowerCase().matches(CmapUtil.URN_UUID + "bnode:[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
				{
					properties.put(ConceptProperty.TITLE.name(), "[]");
				}
				else if (resource.getURI().startsWith(CmapUtil.URN_UUID))
				{
					properties.put(ConceptProperty.TITLE.name(), breakString(resource.getURI().replaceAll(CmapUtil.URN_UUID, "instance of "), MAX_CHARS));
				}
				else
				{
					properties.put(ConceptProperty.TITLE.name(), breakString(resource.getURI(), MAX_CHARS));
				}
			}
		}
		else if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
		{
			String title = model.listStatements(resource, AFOUtil.SKOS_PREF_LABEL, (RDFNode) null).next().getLiteral().toString();
			properties.put(ConceptProperty.TITLE.name(), breakString(title, MAX_CHARS));
		}
		else
		{
			if (resource.getURI().startsWith(CmapUtil.URN_UUID)
					&& resource.getURI().toLowerCase().matches(CmapUtil.URN_UUID + "[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
			{
				properties.put(ConceptProperty.TITLE.name(), "instance");
			}
			else if (resource.getURI().startsWith(CmapUtil.URN_UUID)
					&& resource.getURI().toLowerCase().matches(CmapUtil.URN_UUID + "bnode:[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
			{
				properties.put(ConceptProperty.TITLE.name(), "[]");
			}
			else if (resource.getURI().startsWith(CmapUtil.URN_UUID))
			{
				properties.put(ConceptProperty.TITLE.name(), breakString(resource.getURI().replaceAll(CmapUtil.URN_UUID, "instance of "), MAX_CHARS));
			}
			else
			{
				properties.put(ConceptProperty.TITLE.name(), breakString(resource.getURI(), MAX_CHARS));
			}
		}

		return properties;
	}

	private Model addBlankNodes(Model model, Model singleConceptModel, Statement statementWithBlankObject, String iri)
	{
		StmtIterator stmtIterator = model.listStatements(statementWithBlankObject.getObject().asResource(), (Property) null, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			singleConceptModel.add(statement);
			if (statement.getObject().isAnon() && !statement.getObject().toString().equals(iri))
			{
				singleConceptModel = addBlankNodes(model, singleConceptModel, statement, iri);
			}
		}
		stmtIterator = model.listStatements((Resource) null, (Property) null, statementWithBlankObject.getObject());
		while (stmtIterator.hasNext())
		{
			singleConceptModel.add(stmtIterator.next());
		}

		return singleConceptModel;
	}

	private Set<LinkedConcept> createOrRetrieveSetOfLinkedConcepts(Map<String, Set<LinkedConcept>> linkId2LinkedConcepts, String key)
	{
		Set<LinkedConcept> listOfLinkedConcepts = linkId2LinkedConcepts.get(key);

		if (listOfLinkedConcepts != null)
		{
			return listOfLinkedConcepts;
		}

		return new HashSet<>();
	}

	private String breakString(String str, int size)
	{
		StringBuilder sb = new StringBuilder(str);
		int pos = 0;
		if (str.contains(" "))
		{
			while ((pos = sb.indexOf(" ", pos + size)) >= 0)
			{
				sb.insert(pos, "&#10;");
			}
		}
		else
		{
			while ((pos + size) < sb.length())
			{
				sb.insert(pos + size, "&#10;");
				pos += size;
			}
		}

		// make sure there are no linebreaks inbetween escaped quotes
		String stringWithLinebreaks = sb.toString();
		if (stringWithLinebreaks.contains("\\&#10;\""))
		{
			stringWithLinebreaks = stringWithLinebreaks.replaceAll("\\&#10;\"", "\\\"&#10;");
		}
		return stringWithLinebreaks;
	}
}
