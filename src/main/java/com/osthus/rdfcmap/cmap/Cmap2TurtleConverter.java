package com.osthus.rdfcmap.cmap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.text.translate.NumericEntityEscaper;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.AnonId;
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

import com.osthus.adf.AdfCreator;
import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.cmap.cardinality.Cardinality;
import com.osthus.rdfcmap.cmap.cardinality.CardinalityExtractionResult;
import com.osthus.rdfcmap.cmap.cardinality.StatementWithCardinality;
import com.osthus.rdfcmap.cmap.xml.CMap;
import com.osthus.rdfcmap.cmap.xml.CmapNamespacePrefixMapper;
import com.osthus.rdfcmap.cmap.xml.Concept;
import com.osthus.rdfcmap.cmap.xml.ConceptAppearance;
import com.osthus.rdfcmap.cmap.xml.Connection;
import com.osthus.rdfcmap.cmap.xml.ConnectionAppearance;
import com.osthus.rdfcmap.cmap.xml.ControlPoint;
import com.osthus.rdfcmap.cmap.xml.Image;
import com.osthus.rdfcmap.cmap.xml.LinkingPhrase;
import com.osthus.rdfcmap.cmap.xml.LinkingPhraseAppearance;
import com.osthus.rdfcmap.enums.ConceptProperty;
import com.osthus.rdfcmap.helper.LinkedConcept;
import com.osthus.rdfcmap.helper.PreparedModels;
import com.osthus.rdfcmap.helper.VisualizationInfoBuilderResult;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * Cmap2TurtleConverter
 * 
 * cmap -> rdf
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Cmap2TurtleConverter
{
	private static final Logger log = LogManager.getLogger("Logger");

	public static File outputFolder = new File("separate files");

	private Map<String, Map<String, String>> conceptId2UiProperties = new HashMap<>();
	private Map<String, Map<String, String>> connectionId2UiProperties = new HashMap<>();
	private Map<String, Map<String, String>> fullLinkId2UiProperties = new HashMap<>();
	private Map<String, Map<String, String>> controlPointId2UiProperties = new HashMap<>();
	private Map<String, Map<String, String>> imageId2UiProperties = new HashMap<>();

	private List<Resource> resources = new ArrayList<>();

	public void convert(Path pathToInputFile) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, JAXBException
	{
		convert(pathToInputFile, null);
	}

	/**
	 * The previous version is a CXL that also contains an RDF model. The current version of CXL does not yet contain a synchronized RDF model.
	 *
	 * @param pathToCurrentVersion
	 * @param pathToPreviousVersion
	 *
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws JAXBException
	 */
	public void convert(Path pathToInputFile, String[] additionalFiles)
			throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, JAXBException
	{
		log.info("Converting to turtle: " + pathToInputFile.toString()
				+ ((additionalFiles != null && additionalFiles.length > 0) ? " using additional files: " + StringUtils.join(additionalFiles, ", ") : ""));

		// The visualization from CXL may contain changes that are not yet synchronized to the stored model.
		// First, we extract the stored model.

		// We must determine if CXL contains an additional RDF model. CXL directly generated by CMap do not include the RDF model, so we have to extract it from
		// a previous version or provide it additionally.

		Model model = CmapUtil.extractStoredModelFromCxl(pathToInputFile);
		log.info(model.listStatements().toList().size() + " triples total.");
		model = CmapUtil.addTriples(additionalFiles, model);
		if (model.isEmpty())
		{
			log.info("No RDF model found, generating from scratch.");
		}
		else if (additionalFiles != null && additionalFiles.length > 0)
		{
			log.info(model.listStatements().toList().size() + " triples total.");
		}

		// Second, we create or update the visualization description of the RDF model.
		model = createOrUpdateVisualizationModel(pathToInputFile, model);
		// model.write(System.out, "TTL");
		// Third, we walk through all long comments and update the RDF model (creating or updating resources)
		model = updateModel(model);

		// Fourth, we walk through all concepts of CXL, compare to resources of RDF model and remove deleted concepts as well as all their relations.
		model = cleanModel(model);

		prepareOutput(pathToInputFile, model);

		log.info(model.listStatements().toList().size() + " triples total after processing.");
	}

	public Model updateModel(Model model)
	{
		Model singleRdfModel = ModelFactory.createDefaultModel();

		// Extract connections between concepts via links
		Set<LinkedConcept> connectionsFromConceptToLink = new HashSet<>();
		Set<LinkedConcept> connectionsFromLinkToConcept = new HashSet<>();
		for (Resource resource : resources)
		{
			Resource singleUiConnection = model.getResource(resource.getURI());
			if (!VizUtil.AFV_CONNECTION.getURI().equals(singleUiConnection.getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
			{
				continue;
			}

			Resource fromResource = singleUiConnection.getProperty(VizUtil.AFV_CONNECTS_FROM).getResource().getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();
			String from = fromResource.getURI();
			Resource toResource = singleUiConnection.getProperty(VizUtil.AFV_CONNECTS_TO).getResource().getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();
			String to = toResource.getURI();
			if (VizUtil.AFV_CONCEPT.getURI()
					.equals(singleUiConnection.getProperty(VizUtil.AFV_CONNECTS_FROM).getResource().getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
			{
				connectionsFromConceptToLink.add(new LinkedConcept(from, to));
			}
			else
			{
				connectionsFromLinkToConcept.add(new LinkedConcept(from, to));
			}
		}

		HashMap<Resource, List<StatementWithCardinality>> resources2cardinalities = new HashMap<>();
		for (Resource resource : resources)
		{
			Resource singleUiResource = model.getResource(resource.getURI());
			Resource singleResource = singleUiResource.getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();

			String singleRdf = StringUtils.EMPTY;
			if (singleUiResource.hasProperty(VizUtil.AFV_LONG_COMMENT))
			{
				singleRdf = singleUiResource.getProperty(VizUtil.AFV_LONG_COMMENT).getString();
			}
			if (singleRdf == null || singleRdf.isEmpty())
			{
				boolean isConcept = VizUtil.AFV_CONCEPT.getURI().equals(singleUiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI());
				boolean isLink = VizUtil.AFV_LINK.getURI().equals(singleUiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI());
				if (isConcept)
				{
					model.removeAll(singleResource, AFOUtil.RDF_TYPE, AFOUtil.OWL_NAMED_INDIVIDUAL);
					model.add(singleResource, AFOUtil.RDF_TYPE, AFOUtil.OWL_NAMED_INDIVIDUAL);
				}
				else if (isLink)
				{
					model.removeAll(singleResource, AFOUtil.RDF_TYPE, AFOUtil.OWL_OBJECT_PROPERTY);
					model.add(singleResource, AFOUtil.RDF_TYPE, AFOUtil.OWL_OBJECT_PROPERTY);
				}

				String currentTitle = StringUtils.EMPTY;
				if (singleUiResource.hasProperty(AFOUtil.DCT_TITLE))
				{
					currentTitle = singleUiResource.getProperty(AFOUtil.DCT_TITLE).getString();
				}

				if (currentTitle == null || currentTitle.isEmpty())
				{
					if (VizUtil.AFV_LINK.getURI().equals(singleUiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
					{
						currentTitle = "link";
					}
					else if (VizUtil.AFV_CONNECTION.getURI().equals(singleUiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI()))
					{
						String from = singleUiResource.getProperty(VizUtil.AFV_CONNECTS_FROM).getResource().getProperty(AFOUtil.DCT_TITLE).getString();
						String to = singleUiResource.getProperty(VizUtil.AFV_CONNECTS_TO).getResource().getProperty(AFOUtil.DCT_TITLE).getString();
						boolean fromConcept = VizUtil.AFV_CONCEPT.getURI().equals(
								singleUiResource.getProperty(VizUtil.AFV_CONNECTS_FROM).getResource().getProperty(AFOUtil.RDF_TYPE).getResource().getURI());
						boolean toConcept = VizUtil.AFV_CONCEPT.getURI().equals(
								singleUiResource.getProperty(VizUtil.AFV_CONNECTS_TO).getResource().getProperty(AFOUtil.RDF_TYPE).getResource().getURI());
						currentTitle = "connection from " + (fromConcept ? "concept: " : "link: ") + from + " to " + (toConcept ? "concept: " : "link: ") + to;
					}
					else
					{
						continue;
					}
				}

				model.removeAll(singleResource, AFOUtil.DCT_TITLE, (RDFNode) null);
				model.add(singleResource, AFOUtil.DCT_TITLE, currentTitle);
				continue;
			}

			singleRdf = StringEscapeUtils.unescapeXml(singleRdf);
			CardinalityExtractionResult cardinalityExtractionResult = extractAndReplacePropertiesWithCardinality(model, singleRdf, singleResource.getURI());
			resources2cardinalities.put(singleUiResource, cardinalityExtractionResult.getStatementsWithCardinality());
			singleRdf = cardinalityExtractionResult.getSingleRdf();

			singleRdf = createMachineReadableRdf(model, singleRdf);
			InputStream rdfModelInputStream = new ByteArrayInputStream(singleRdf.getBytes());
			try
			{
				singleRdfModel.read(rdfModelInputStream, null, "TTL");
			}
			catch (Exception e)
			{
				log.error("Exception while processing TTL from long comment:\n\n" + singleRdf + "\n");
				log.error("Message: " + e.getLocalizedMessage());
				log.error("Stacktrace : \n" + StringUtils.join(e.getStackTrace(), "\n"));
				System.exit(1);
			}
			if (!singleRdfModel.isEmpty())
			{
				Resource oldResource = null;
				StmtIterator singleRdfModelIterator = singleRdfModel.listStatements();
				while (singleRdfModelIterator.hasNext())
				{
					Statement statement = singleRdfModelIterator.next();
					if (statement.getSubject().isURIResource())
					{
						oldResource = statement.getSubject();
						break;
					}
				}

				if (oldResource == null)
				{
					throw new IllegalStateException("No resource found as subject in single RDF model of concept: " + resource.getURI());
				}

				if (!singleResource.getURI().equals(oldResource.getURI()))
				{
					// there is a different UUID in the long comment --> long comment was manually created and determines the current state but UUIDs come from
					// this app
					// consolidate UUIDs: use UUID of RDF model, overwrite UUID in model of long comment
					log.debug(
							"Found difference in UUIDs of long comment and RDF model. Most likely resulting from manual editing. Overwriting UUID of long comment: "
									+ oldResource.getURI() + " overwritten by " + singleResource.getURI());

					// update statements of single RDF model
					List<Statement> newStatements = new ArrayList<>();
					List<Statement> oldStatements = new ArrayList<>();
					StmtIterator stmtIterator = singleRdfModel.listStatements();
					while (stmtIterator.hasNext())
					{
						Statement statementToUpdate = stmtIterator.next();
						if (!statementToUpdate.getSubject().isURIResource())
						{
							continue;
						}
						oldStatements.add(statementToUpdate);
						newStatements.add(ResourceFactory.createStatement(singleResource, statementToUpdate.getPredicate(), statementToUpdate.getObject()));
					}
					singleRdfModel.remove(oldStatements);
					singleRdfModel.add(newStatements);

					// update statements of main model (oldRsource as object is replaced by new resource as object
					newStatements.clear();
					oldStatements.clear();
					stmtIterator = model.listStatements((Resource) null, (Property) null, oldResource);
					while (stmtIterator.hasNext())
					{
						Statement statementToUpdate = stmtIterator.next();
						oldStatements.add(statementToUpdate);
						newStatements.add(ResourceFactory.createStatement(statementToUpdate.getSubject(), statementToUpdate.getPredicate(), singleResource));
					}
					model.remove(oldStatements);
					model.add(newStatements);
				}

				List<Statement> statementsToRemove = extractStatementsForResource(singleResource, model);
				model.remove(statementsToRemove);
				model.add(singleRdfModel);

				// if the title of the concept in CMap differs from the title in RDF long comment
				// then update the title with the one from the label

				String currentTitle = StringUtils.EMPTY;
				if (singleUiResource.hasProperty(AFOUtil.DCT_TITLE))
				{
					currentTitle = singleUiResource.getProperty(AFOUtil.DCT_TITLE).getString();
				}

				String titleFromLongComment = StringUtils.EMPTY;
				StmtIterator stmtIterator = singleRdfModel.listStatements(singleUiResource, AFOUtil.DCT_TITLE, (RDFNode) null);
				if (stmtIterator.hasNext())
				{
					titleFromLongComment = stmtIterator.next().getString();
					if (titleFromLongComment != null && !titleFromLongComment.isEmpty())
					{
						if (currentTitle != null && !currentTitle.isEmpty() && !currentTitle.equals(titleFromLongComment))
						{
							model.removeAll(singleResource, AFOUtil.DCT_TITLE, (RDFNode) null);
							model.add(singleResource, AFOUtil.DCT_TITLE, currentTitle);
						}
					}
				}

				if (titleFromLongComment == null || titleFromLongComment.isEmpty())
				{
					if (currentTitle != null && !currentTitle.isEmpty())
					{
						model.removeAll(singleResource, AFOUtil.DCT_TITLE, (RDFNode) null);
						model.add(singleResource, AFOUtil.DCT_TITLE, currentTitle);
					}
				}

				// consolidate object properties that link to instances with current state of visualization:
				// // walk through all object properties of a long comment and filter those that link to instances <urn:uuid...>
				// // check if there exists a corresponding link in the visualization
				// // if visualization contains no link then delete the triple

				// filter links to instances
				List<Statement> statementsToSynchronizeWithViz = new ArrayList<>();
				singleRdfModelIterator = singleRdfModel.listStatements();
				while (singleRdfModelIterator.hasNext())
				{
					Statement statement = singleRdfModelIterator.next();
					if (statement.getObject().isLiteral())
					{
						continue;
					}

					if (statement.getObject().isAnon())
					{
						continue;
					}

					if (statement.getResource().isURIResource())
					{
						Resource object = statement.getResource();
						if (!object.getURI().contains(CmapUtil.URN_UUID))
						{
							continue;
						}

						statementsToSynchronizeWithViz.add(statement);
					}
				}

				if (statementsToSynchronizeWithViz != null && !statementsToSynchronizeWithViz.isEmpty())
				{
					List<Statement> outdatedStatements = new ArrayList<>();
					for (Iterator<Statement> statementsToSynchronizeWithVizIterator = statementsToSynchronizeWithViz
							.iterator(); statementsToSynchronizeWithVizIterator.hasNext();)
					{
						Statement statement = statementsToSynchronizeWithVizIterator.next();
						Resource fromConcept = statement.getSubject();

						Resource viaLink = statement.getPredicate().asResource();
						String viaLinkLabel = getShortNameForPropertyFromLink(model, viaLink);

						Resource toConcept = statement.getResource();

						boolean vizContainsTriple = false;

						// compare to visualization, check if exists connection from resource to link
						// look for agreement of all three parts: start concept, link, target concept
						// link needs special handling because we have AFX and external properties
						// link needs special handling because each link is its own instance of AFV_LINK and is related to a proxy instance of an object
						// property
						// the instance's title is equal to prefLabel for AFX properties or equal to prefix:localname for properties of other ontologies
						for (Iterator<LinkedConcept> linkedConceptIterator = connectionsFromConceptToLink.iterator(); linkedConceptIterator.hasNext();)
						{
							LinkedConcept linkedConcept = linkedConceptIterator.next();
							if (!linkedConcept.from.equals(fromConcept.getURI()))
							{
								continue;
							}
							// start concepts agree

							Resource vizLink = model.getResource(linkedConcept.to.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
							String vizLinkLabel = vizLink.getProperty(AFOUtil.DCT_TITLE).getString();
							if (!vizLinkLabel.contains(viaLinkLabel))
							{
								// check if label of link in visualization disagrees from title of AFV_Link in visualization model
								continue;
							}
							// link labels agree

							for (Iterator<LinkedConcept> innerLinkedConceptIterator = connectionsFromLinkToConcept.iterator(); innerLinkedConceptIterator
									.hasNext();)
							{
								LinkedConcept innerLinkedConcept = innerLinkedConceptIterator.next();
								if (!innerLinkedConcept.from.equals(linkedConcept.to))
								{
									continue;
								}

								if (toConcept.getURI().equals(innerLinkedConcept.to))
								{
									// target concepts agree
									vizContainsTriple = true;
									break;
								}
							}

							if (vizContainsTriple)
							{
								break;
							}
						}

						if (!vizContainsTriple)
						{
							outdatedStatements.add(statement);
						}
					}

					model.remove(outdatedStatements);
				}
			}

			singleRdfModel.removeAll();
		}

		// replace relations by af-x properties or imported properties
		List<Statement> statementsToRemove = new ArrayList<>();
		List<Statement> statementsToAdd = new ArrayList<>();
		for (Iterator<LinkedConcept> iterator = connectionsFromConceptToLink.iterator(); iterator.hasNext();)
		{
			LinkedConcept linkedConcept = iterator.next();
			Resource fromConcept = model.getResource(linkedConcept.from);
			String linkId = linkedConcept.to;
			Resource link = model.getResource(linkId);
			link = tryToExtractLinkFromAfxAndObo(model, link);

			statementsToRemove.addAll(model.listStatements(fromConcept, ResourceFactory.createProperty(linkId), (RDFNode) null).toList());
			for (Iterator<LinkedConcept> iterator2 = connectionsFromLinkToConcept.iterator(); iterator2.hasNext();)
			{
				LinkedConcept linkedConcept2 = iterator2.next();
				if (!linkId.equals(linkedConcept2.from))
				{
					continue;
				}

				Resource toConcept = model.getResource(linkedConcept2.to);

				Property property = null;
				if (link.getURI().contains(AFOUtil.AFX_PREFIX) || link.getURI().contains(AFOUtil.OBO_PREFIX))
				{
					property = ResourceFactory.createProperty(link.getURI());
				}
				else
				{
					// handle object properties of imported ontologies
					Resource vizLink = model.getResource(link.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
					String vizLinkLabel = unbreakString(vizLink.getProperty(AFOUtil.DCT_TITLE).getString());
					String[] segments = vizLinkLabel.split(":");
					if (segments[1].contains(" "))
					{
						String vizLinkLabelCapitalized = WordUtils.capitalizeFully(segments[1], ' ').replaceAll(" ", "");
						String firstLetterLowerCase = vizLinkLabelCapitalized.substring(0, 1).toLowerCase();
						vizLinkLabel = segments[0] + ":" + firstLetterLowerCase + vizLinkLabelCapitalized.substring(1, vizLinkLabelCapitalized.length());
					}
					String vizLinkUri = CmapUtil.replacePrefixesWithNamespaces(model, Arrays.asList(vizLinkLabel)).get(0);
					vizLinkUri = vizLinkUri.replaceAll("<", "").replaceAll(">", "");
					property = ResourceFactory.createProperty(vizLinkUri);
				}

				statementsToAdd.add(ResourceFactory.createStatement(fromConcept, property, toConcept));
			}
		}
		model.remove(statementsToRemove);
		model.add(statementsToAdd);

		// add relations between instances of cmap-links and AFX
		statementsToRemove.clear();
		statementsToAdd.clear();
		for (Resource resource : resources)
		{
			Resource singleUiResource = model.getResource(resource.getURI());
			Resource singleResource = singleUiResource.getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();
			boolean isLink = VizUtil.AFV_LINK.getURI().equals(singleUiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI());
			if (isLink)
			{
				StmtIterator relatedIterator = model.listStatements(singleResource, AFOUtil.SKOS_RELATED, (RDFNode) null);
				while (relatedIterator.hasNext())
				{
					statementsToRemove.add(relatedIterator.next());
				}

				Resource link = tryToExtractLinkFromAfxAndObo(model, singleResource);
				if (link.getNameSpace().equals(AFOUtil.AFX_PREFIX) || link.getNameSpace().equals(AFOUtil.OBO_PREFIX))
				{
					statementsToAdd.add(ResourceFactory.createStatement(singleResource, AFOUtil.SKOS_RELATED, link));
				}
				else
				{
					Resource externalLink = RdfUtil.getResourceByLabel(model, link.getProperty(AFOUtil.DCT_TITLE).getString(), true, false);
					if (externalLink != null)
					{
						statementsToAdd.add(ResourceFactory.createStatement(singleResource, AFOUtil.SKOS_RELATED, externalLink));
					}
					else
					{
						log.error("Could not resolve property: " + link.getURI()
								+ (link.hasProperty(AFOUtil.DCT_TITLE) ? " " + link.getProperty(AFOUtil.DCT_TITLE).getString() : ""));
					}
				}
			}
		}
		model.remove(statementsToRemove);
		model.add(statementsToAdd);

		// handle cardinalities

		statementsToRemove.clear();
		statementsToAdd.clear();

		for (Entry<Resource, List<StatementWithCardinality>> entry : resources2cardinalities.entrySet())
		{
			Resource uiResource = model.getResource(entry.getKey().getURI());
			Resource resource = uiResource.getProperty(AFOUtil.AFX_HAS_OBJECT).getResource();
			if (entry.getValue().isEmpty())
			{
				model = removeExistingHiddenProperties(model, uiResource);
				continue;
			}

			if (!resource.getURI().equals(entry.getValue().get(0).getSubject()))
			{
				throw new IllegalStateException(
						"Error with cardinality extraction. Different resources found: " + resource.getURI() + " <-> " + entry.getValue().get(0).getSubject());
			}

			model = removeExistingHiddenProperties(model, uiResource);

			// determine cardinalities based on extracted cardinalities from hidden info
			int numDeterminedCardinalities = 0;
			StmtIterator stmtIterator = model.listStatements(resource, (Property) null, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				if (statement.getObject().isURIResource() && statement.getObject().asResource().getURI().startsWith(CmapUtil.URN_UUID))
				{
					// skip links to instances
					continue;
				}

				String label = getShortNameForPropertyFromLink(model, statement.getPredicate().asResource());

				String objectAsString = StringUtils.EMPTY;
				if (statement.getObject().isURIResource())
				{
					objectAsString = "<" + statement.getResource().getURI() + ">";
				}
				else if (statement.getObject().isAnon())
				{
					objectAsString = "[";
					// TODO: better mapping of blank node to string representation
				}
				else
				{
					objectAsString = statement.getString().trim().replaceAll("\"", "");
				}

				for (StatementWithCardinality statementWithCardinality : entry.getValue())
				{
					if (statementWithCardinality.getObject().contains(CmapUtil.URN_UUID))
					{
						continue;
					}

					if (statementWithCardinality.getProperty().contains(label) && (statementWithCardinality.getObject().contains(objectAsString)
							|| (objectAsString.contains(statementWithCardinality.getObject()))))
					{
						numDeterminedCardinalities++;
						statementWithCardinality.setMapped(true);
						String cardinalityString = statementWithCardinality.getCardinality();
						String labelWithoutCardinality = statementWithCardinality.getProperty();

						String propertyIri = statement.getPredicate().getURI();

						// add triples for cardinality of property in long comment

						Cardinality cardinality = CmapUtil.determineCardinality(cardinalityString);

						Resource hiddenProperty = model.createResource(VizUtil.AFV_PREFIX + UUID.randomUUID());
						model = CmapUtil.createOrUpdateRelatedResource(model, hiddenProperty, AFOUtil.RDF_TYPE, VizUtil.AFV_HIDDEN_PROPERTY);
						model = CmapUtil.createOrUpdateLiteralValue(model, hiddenProperty, AFOUtil.DCT_TITLE, labelWithoutCardinality);
						model = CmapUtil.createOrUpdateRelatedResource(model, hiddenProperty, AFOUtil.AFX_HAS_PROPERTY, model.getResource(propertyIri));
						model = CmapUtil.createOrUpdateLiteralValue(model, hiddenProperty, AFOUtil.AFX_HAS_VALUE, objectAsString);

						model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, hiddenProperty, VizUtil.AFV_HAS_CARDINALITY, cardinality.getType(),
								AFOUtil.AFX_HAS_VALUE, cardinalityString);
						model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, hiddenProperty, VizUtil.AFV_HAS_CARDINALITY, cardinality.getType(),
								AFOUtil.AFX_MINIMUM_VALUE, cardinality.getMinimumValue());
						model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, hiddenProperty, VizUtil.AFV_HAS_CARDINALITY, cardinality.getType(),
								AFOUtil.AFX_MAXIMUM_VALUE, cardinality.getMaximumValue());

						model = CmapUtil.createOrUpdateRelatedResource(model, uiResource, VizUtil.AFV_HAS_HIDDEN_PROPERTY, hiddenProperty);
						break;
					}
				}

			}
			if (numDeterminedCardinalities != entry.getValue().size())
			{
				for (StatementWithCardinality statementWithCardinality : entry.getValue())
				{
					if (!statementWithCardinality.isMapped() && !statementWithCardinality.getObject().contains(CmapUtil.URN_UUID))
					{
						log.error("Error during cardinality extraction. Could not map statement: " + statementWithCardinality.getSubject() + " <<"
								+ statementWithCardinality.getProperty() + " " + statementWithCardinality.getCardinality() + ">> "
								+ statementWithCardinality.getObject());
					}
				}
			}
		}

		// use background colors of AFT

		if (!RdfCmap.isAftColorScheme)
		{
			return model;
		}

		statementsToRemove.clear();
		statementsToAdd.clear();
		for (Resource resource : resources)
		{
			Resource singleUiResource = model.getResource(resource.getURI());
			String bgColor = CmapUtil.determineBackgroundColor(model, singleUiResource);

			statementsToRemove.addAll(model.listStatements(singleUiResource, VizUtil.AFV_BACKGROUND_COLOR, (RDFNode) null).toList());
			statementsToAdd.add(ResourceFactory.createStatement(singleUiResource, VizUtil.AFV_BACKGROUND_COLOR, ResourceFactory.createPlainLiteral(bgColor)));
		}
		model.remove(statementsToRemove);
		model.add(statementsToAdd);

		// add hierarchy level

		return model;
	}

	private Model removeExistingHiddenProperties(Model model, Resource uiResource)
	{
		// remove existing hidden properties and cardinalities
		List<Statement> hiddenPropertyStatementsToRemove = new ArrayList<>();
		StmtIterator hiddenPropertyIterator = model.listStatements(uiResource, VizUtil.AFV_HAS_HIDDEN_PROPERTY, (RDFNode) null);
		while (hiddenPropertyIterator.hasNext())
		{
			Statement statement = hiddenPropertyIterator.next();
			hiddenPropertyStatementsToRemove.add(statement);
			StmtIterator propertiesOfHiddenPropertyIterator = model.listStatements(statement.getResource(), (Property) null, (RDFNode) null);
			while (propertiesOfHiddenPropertyIterator.hasNext())
			{
				Statement statement2 = propertiesOfHiddenPropertyIterator.next();
				hiddenPropertyStatementsToRemove.add(statement2);
			}

			Resource cardinalityOfHiddenProperty = model.listStatements(statement.getResource(), VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null).next()
					.getResource();
			StmtIterator propertiesOfCardinalityOfHiddenPropertyIterator = model.listStatements(cardinalityOfHiddenProperty, (Property) null, (RDFNode) null);
			while (propertiesOfCardinalityOfHiddenPropertyIterator.hasNext())
			{
				Statement statement2 = propertiesOfCardinalityOfHiddenPropertyIterator.next();
				hiddenPropertyStatementsToRemove.add(statement2);
			}
		}
		model.remove(hiddenPropertyStatementsToRemove);
		return model;
	}

	/**
	 * Method scans long comment for e.g. <<some:property <1>> or <<af-x:index 1>> and extracts title with cardinality plus related object. The property is
	 * replaced for some:property and <<af-x:index>>
	 *
	 * @param model
	 * @param singleRdf
	 * @return
	 */
	private CardinalityExtractionResult extractAndReplacePropertiesWithCardinality(Model model, String singleRdf, String subject)
	{
		List<StatementWithCardinality> statementsWithCardinality = new ArrayList<StatementWithCardinality>();

		singleRdf = singleRdf.replaceAll("\r\n", " ");
		singleRdf = singleRdf.replaceAll("\n", " ");
		List<String> lines = Arrays.asList(singleRdf.split(";"));

		List<String> processedLines = new ArrayList<String>(lines.size());

		String regex = "((^|[\\w\\s]*)<<((([a-z\\-]+):([a-z\\-\\s/\\(\\)0-9]+))\\s+((([\\[\\]]?)([\\<\\>\\=]*)\\s*([0-9]+)([\\[\\]]?))\\s*[\\,]?([0-9]*)\\s*([\\[\\]]?))\\s*)>>)([\"\\:\\w\\s<]*|$)";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

		String commentRegex = ".*#(?![\\w]*>.*)";
		Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);

		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);

			// replace french quotes for double brackets
			line = line.replaceAll("«", "<<");
			line = line.replaceAll("»", ">>");

			// replace human readable identifiers for artificial IDs
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

				String label = m.group(3);
				String labelWithoutCardinality = m.group(4);
				String cardinality = m.group(7);
				String object = m.group(15).trim().replaceAll("\"", "");
				statementsWithCardinality.add(new StatementWithCardinality(subject, labelWithoutCardinality, object, cardinality));

				if (label.toLowerCase().contains("af-x") || label.toLowerCase().contains("afx"))
				{
					// af-x property replace witch <<af-x:property>>
					line = line.replaceAll("<<" + Pattern.quote(label) + ">>", "<<" + labelWithoutCardinality + ">>");
				}
				else
				{
					// other properties replace with other:property
					line = line.replaceAll("<<" + Pattern.quote(label) + ">>", labelWithoutCardinality);
				}
			}

			if (!(foundId && commentedOut))
			{
				processedLines.add(line);
			}
			log.debug("--> " + line);
		}

		return new CardinalityExtractionResult(statementsWithCardinality, StringUtils.join(processedLines.toArray(), ";\n"));
	}

	private String getShortNameForPropertyFromLink(Model model, Resource propertyAsResource)
	{
		if (propertyAsResource.getURI().contains(AFOUtil.AFX_PREFIX))
		{
			return model.getResource(propertyAsResource.getURI()).getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
		}

		String prefix = RdfUtil.getNamespaceMap().get(propertyAsResource.getNameSpace());
		if (isOboProperty(prefix))
		{
			String name = propertyAsResource.getLocalName().toUpperCase();
			prefix = getPrefixForOboTermLabel(name);
		}
		return prefix + ":" + propertyAsResource.getLocalName();
	}

	public static String getPrefixForOboTermLabel(String name)
	{
		String prefix;
		if (name.startsWith("BFO"))
		{
			prefix = "bfo";
		}
		else if (name.startsWith("IAO"))
		{
			prefix = "iao";
		}
		else if (name.startsWith("OBI"))
		{
			prefix = "obi";
		}
		else if (name.startsWith("RO"))
		{
			prefix = "ro";
		}
		else if (name.startsWith("CHEBI"))
		{
			prefix = "chebi";
		}
		else if (name.startsWith("PATO"))
		{
			prefix = "pato";
		}
		else if (name.contains("_"))
		{
			prefix = name.split("_")[0].toLowerCase();
		}
		else
		{
			prefix = "obo";
		}
		return prefix;
	}

	private List<Statement> extractStatementsForResource(Resource singleResource, Model model)
	{
		List<Statement> statementsToRemove = new ArrayList<>();
		StmtIterator singleResourceStmtIterator = model.listStatements(singleResource, (Property) null, (RDFNode) null);
		while (singleResourceStmtIterator.hasNext())
		{
			Statement statement = singleResourceStmtIterator.next();
			statementsToRemove.add(statement);
			if (!statement.getObject().isAnon())
			{
				continue;
			}

			statementsToRemove = handleBlankNodes(statement, model, statementsToRemove);
		}
		return statementsToRemove;
	}

	private List<Statement> handleBlankNodes(Statement statement, Model model, List<Statement> statementsToRemove)
	{
		Resource object = statement.getObject().asResource();
		StmtIterator blankIterator = model.listStatements(object, (Property) null, (RDFNode) null);
		while (blankIterator.hasNext())
		{
			Statement statementOfBlank = blankIterator.next();
			statementsToRemove.add(statementOfBlank);
			if (!statementOfBlank.getObject().isAnon())
			{
				if (statementOfBlank.getObject().isURIResource() && statementOfBlank.getObject().asResource().getURI().contains(CmapUtil.URN_UUID))
				{
					log.warn("Found blank node with relation to an instance. Check. Instance ID: " + statementOfBlank.getObject().asResource().getURI());
				}
				continue;
			}

			statementsToRemove = handleBlankNodes(statementOfBlank, model, statementsToRemove);
		}

		return statementsToRemove;
	}

	private String createMachineReadableRdf(Model model, String singleRdf)
	{
		List<String> lines = Arrays.asList(singleRdf.split("\n"));
		List<String> processedLines = new ArrayList<String>(lines.size());
		String regex = "((^|[\\w\\s]*)<<([a-z\\-]+):([a-z\\-\\s/\\(\\)0-9]+)>>)([\\w\\s]*|$)";
		String placeHolderRegex = "(^|[\\w\\s]+)(\\?[a-zA-Z0-9\\-]+)([\\w\\s]+|$)";
		String commentRegex = ".*#(?![\\w]*>.*)";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Pattern placeHolderPattern = Pattern.compile(placeHolderRegex, Pattern.CASE_INSENSITIVE);
		Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);

			// replace placeholders like ?variable-placeholder-name for UUIDs
			Matcher placeHolderMatcher = placeHolderPattern.matcher(line);
			Matcher commentMatcher = commentPattern.matcher(line);
			while (placeHolderMatcher.find())
			{
				if (line.contains("#") && line.indexOf("#") < placeHolderMatcher.start(1))
				{
					if (commentMatcher.matches() && commentMatcher.end(1) < placeHolderMatcher.end(2))
					{
						continue;
					}
				}

				String placeHolder = placeHolderMatcher.group(2);
				String uuid = "<" + CmapUtil.URN_UUID + UUID.randomUUID() + ">";
				line = line.replaceAll(Pattern.quote(placeHolder), uuid);
			}

			// replace french quotes for double brackets
			line = line.replaceAll("«", "<<");
			line = line.replaceAll("»", ">>");

			// replace human readable identifiers for artificial IDs
			Matcher m = p.matcher(line);
			commentMatcher = commentPattern.matcher(line);
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

				String label = m.group(4);
				String escapedRegexForSparql = label.replaceAll("\\(", "\\\\\\\\(").replaceAll("\\)", "\\\\\\\\)");
				String escapedRegexForJava = label.replaceAll("\\(", "\\\\\\(").replaceAll("\\)", "\\\\\\)");
				// @formatter:off
					String queryString = RdfUtil.getPrefixes()
				             + "select ?s where { \n"
				             + "  { \n"
				             + "    OPTIONAL { ?s skos:prefLabel ?label . } \n"
				             + "  } UNION { \n"
				             + "    OPTIONAL { ?s rdfs:label ?label . } \n"
				             + "  } \n"
				             + "  FILTER regex(?label, \"^" + escapedRegexForSparql + "$\", \"i\" )\n"
				             + "  FILTER (lang(?label) = \"\" || langMatches(lang(?label), \"EN\") )\n"
				             + "}" ;
					// @formatter:on

				Query query = QueryFactory.create(queryString);
				QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
				ResultSet results = queryExecution.execSelect();
				boolean foundPrefix = false;
				while (results.hasNext())
				{
					QuerySolution qs = results.next();
					String namespace = qs.get("s").asResource().getNameSpace();
					String prefix = RdfUtil.getNamespaceMap().get(namespace);
					String localname = "";
					if (prefix != null && ((prefix.equals(m.group(3))) || prefix.replaceAll("-", "").equals(m.group(3)) || prefix.toLowerCase().equals("obo")))
					{
						if (qs.get("s").asResource().hasProperty(AFOUtil.OWL_DEPRECATED)
								&& qs.get("s").asResource().getProperty(AFOUtil.OWL_DEPRECATED).getBoolean())
						{
							// term is deprecated, look for replacement
							if (!qs.get("s").asResource().hasProperty(AFOUtil.DCT_IS_REPLACED_BY))
							{
								log.error("Deprecated term without replacement: " + m.group(3) + ":" + m.group(4));
								localname = qs.get("s").asResource().getLocalName();
							}
							else
							{
								namespace = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource().getNameSpace();
								prefix = RdfUtil.getNamespaceMap().get(namespace);
								localname = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource().getLocalName();
								String replaceLabel = qs.get("s").asResource().getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
								log.debug("Deprecated term: <<" + m.group(3) + ":" + label + ">> is replaced by <<" + prefix + ":" + replaceLabel + ">> ("
										+ prefix + ":" + localname + ")");
							}
						}
						else
						{
							// term is not deprecated
							localname = qs.get("s").asResource().getLocalName();
						}
						foundPrefix = true;
						line = line.replaceAll("<<" + Pattern.quote(m.group(3)) + ":" + Pattern.quote(m.group(4)) + ">>", "<" + namespace + localname + ">");
						break;
					}
				}
				queryExecution.close();

				if (!foundPrefix)
				{
					log.error("Missing term: " + m.group(3) + ":" + m.group(4));
				}
			}

			if (!(foundId && commentedOut))
			{
				processedLines.add(line);
			}
			log.debug("--> " + line);
		}

		processedLines = CmapUtil.replacePrefixesWithNamespaces(model, processedLines);

		return StringUtils.join(processedLines.toArray(), "\n");
	}

	public static Resource tryToExtractLinkFromAfxAndObo(Model model, Resource link)
	{
		if (link.hasProperty(AFOUtil.DCT_TITLE))
		{
			// try to extract property from title
			String title = link.getProperty(AFOUtil.DCT_TITLE).getString();
			if (title != null && !title.isEmpty())
			{
				title = title.toLowerCase();
				title = title.trim();
				// skip links that are properties from other ontologies than afx or obo
				if (title.contains(":"))
				{
					if (!title.startsWith("afx") && !title.startsWith("af-x") && !isOboProperty(title))
					{
						return link;
					}
				}
				else
				{
					title = "af-x:" + title;
				}

				if ((title.startsWith("afx") || title.startsWith("af-x")) && !isOboProperty(title))
				{
					title = title.replace("afx", "");
					title = title.replace("af-x", "");
					title = title.replace(":", "");
					title = title.replaceAll("\n", "");
					title = title.trim();

					String escapedRegexForSparql = title.replaceAll("\\(", "\\\\\\\\(").replaceAll("\\)", "\\\\\\\\)");
				// @formatter:off
						String queryString = RdfUtil.getPrefixes()
					             + "select ?s where { \n"
					             + "  ?s skos:prefLabel ?label .\n"
					             + "FILTER regex(?label, \"^" + escapedRegexForSparql + "$\", \"i\" )\n"
					             + "}" ;
						// @formatter:on

					Query query = QueryFactory.create(queryString);
					QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
					ResultSet results = queryExecution.execSelect();
					boolean foundPrefix = false;
					while (results.hasNext())
					{
						QuerySolution qs = results.next();
						String namespace = qs.get("s").asResource().getNameSpace();
						if (namespace != null && (AFOUtil.AFX_PREFIX.equals(namespace)))
						{
							if (qs.get("s").asResource().hasProperty(AFOUtil.OWL_DEPRECATED)
									&& qs.get("s").asResource().getProperty(AFOUtil.OWL_DEPRECATED).getBoolean())
							{
								// term is deprecated, look for replacement
								if (!qs.get("s").asResource().hasProperty(AFOUtil.DCT_IS_REPLACED_BY))
								{
									log.error("Deprecated term without replacement: af-x:" + title);
									link = qs.get("s").asResource();
								}
								else
								{
									link = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource();
									namespace = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource().getNameSpace();
									String prefix = RdfUtil.getNamespaceMap().get(namespace);
									String localname = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource().getLocalName();
									String replaceLabel = qs.get("s").asResource().getProperty(AFOUtil.SKOS_PREF_LABEL).getString();
									log.debug("Deprecated term: <<af-x:" + title + ">> is replaced by <<" + prefix + ":" + replaceLabel + ">> (" + prefix + ":"
											+ localname + ")");
								}
							}
							else
							{
								// term is not deprecated
								link = qs.get("s").asResource();
							}
							foundPrefix = true;
							break;
						}
					}
					queryExecution.close();

					if (!foundPrefix)
					{
						log.error("Missing term: af-x:" + title);
					}
				}
				else
				{
					// obo properties
					title = title.replace("obo:", "");
					title = title.replace("obi:", "");
					title = title.replace("bfo:", "");
					title = title.replace("iao:", "");
					title = title.replace("ro:", "");
					title = title.replaceAll("\n", "");
					title = title.trim();

					String escapedRegexForSparql = title.replaceAll("\\(", "\\\\\\\\(").replaceAll("\\)", "\\\\\\\\)");
					// @formatter:off
						String queryString = RdfUtil.getPrefixes()
					             + "select ?s where { \n"
					             + "  { ?s rdfs:label ?label . } UNION { ?s skos:prefLabel ?label . } \n"
					             + "FILTER regex(?label, \"^" + escapedRegexForSparql + "$\", \"i\" )\n"
					             + "}" ;
						// @formatter:on

					Query query = QueryFactory.create(queryString);
					QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
					ResultSet results = queryExecution.execSelect();
					boolean foundPrefix = false;
					while (results.hasNext())
					{
						QuerySolution qs = results.next();
						String namespace = qs.get("s").asResource().getNameSpace();
						if (namespace != null && (AFOUtil.OBO_PREFIX.equals(namespace)))
						{
							if (qs.get("s").asResource().hasProperty(AFOUtil.OWL_DEPRECATED)
									&& qs.get("s").asResource().getProperty(AFOUtil.OWL_DEPRECATED).getBoolean())
							{
								// term is deprecated, look for replacement
								if (!qs.get("s").asResource().hasProperty(AFOUtil.DCT_IS_REPLACED_BY))
								{
									log.error("Deprecated term without replacement: obo:" + title);
									link = qs.get("s").asResource();
								}
								else
								{
									link = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource();
									namespace = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource().getNameSpace();
									String localname = qs.get("s").asResource().getProperty(AFOUtil.DCT_IS_REPLACED_BY).getObject().asResource().getLocalName();
									String prefix = getPrefixForOboTermLabel(localname);
									String replaceLabel = qs.get("s").asResource().getProperty(AFOUtil.RDFS_LABEL).getString();
									log.debug("Deprecated term: <<obo:" + title + ">> is replaced by <<" + prefix + ":" + replaceLabel + ">> (" + prefix + ":"
											+ localname + ")");
								}
							}
							else
							{
								// term is not deprecated
								link = qs.get("s").asResource();
							}
							foundPrefix = true;
							break;
						}
					}
					queryExecution.close();

					if (!foundPrefix)
					{
						log.error("Missing term obo:" + title);
					}
				}
			}

			boolean isObjectProperty = false;
			StmtIterator stmtIterator = model.listStatements(link, AFOUtil.RDF_TYPE, (RDFNode) null);
			while (stmtIterator.hasNext())
			{
				Statement statement = stmtIterator.next();
				if (statement.getObject().isURIResource() && AFOUtil.OWL_OBJECT_PROPERTY.getURI().equals(statement.getResource().getURI()))
				{
					isObjectProperty = true;
					break;
				}
			}

			if (!isObjectProperty)
			{
				log.debug("Found link that is not an object property: " + link.getURI());
			}
		}
		return link;
	}

	public Model cleanModel(Model model)
	{
		if (RdfCmap.ignoreLongComments)
		{
			log.debug("Dropped all long comments. Clean up not needed.");
			return model;
		}
		List<Statement> statementsToRemove = new ArrayList<>();
		List<String> idsToRemove = new ArrayList<>();
		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (subject.isAnon())
			{
				continue;
			}

			String subjectId = subject.getURI();

			if (idsToRemove.contains(subjectId))
			{
				// already handled
				continue;
			}

			if (!subjectId.contains(CmapUtil.URN_UUID))
			{
				continue;
			}

			if (!subject.hasProperty(AFOUtil.RDF_TYPE))
			{
				log.debug("Found resource without type: " + subjectId);
				continue;
			}

			String uiSubjectId = subjectId.replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX);
			Resource uiSubject = model.getResource(uiSubjectId);

			if (!conceptId2UiProperties.containsKey(subjectId) && !fullLinkId2UiProperties.containsKey(subjectId)
					&& !connectionId2UiProperties.containsKey(subjectId) && !controlPointId2UiProperties.containsKey(subjectId)
					&& !conceptId2UiProperties.containsKey(uiSubjectId) && !fullLinkId2UiProperties.containsKey(uiSubjectId)
					&& !connectionId2UiProperties.containsKey(uiSubjectId) && !controlPointId2UiProperties.containsKey(uiSubjectId))
			{
				String vizKey = uiSubject.getProperty(VizUtil.AFV_IDENTIFIER).getString();
				if (!conceptId2UiProperties.containsKey(vizKey) && !fullLinkId2UiProperties.containsKey(vizKey)
						&& !connectionId2UiProperties.containsKey(vizKey) && !controlPointId2UiProperties.containsKey(vizKey))
				{
					log.debug("RDF resource with id: " + subjectId + " was not found in vizmodel. Deleting from RDF model.");
					statementsToRemove.addAll(extractStatementsForResource(subject, model));
					statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, subject).toList());
					idsToRemove.add(subjectId);

					statementsToRemove.addAll(extractStatementsForResource(uiSubject, model));
					statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, uiSubject).toList());
					idsToRemove.add(uiSubjectId);

					if (uiSubject.hasProperty(VizUtil.AFV_HAS_FONT))
					{
						Resource fontResource = uiSubject.getProperty(VizUtil.AFV_HAS_FONT).getResource();
						statementsToRemove.addAll(extractStatementsForResource(fontResource, model));
						statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, fontResource).toList());
					}

					if (uiSubject.hasProperty(VizUtil.AFV_HAS_BORDER))
					{
						Resource borderResource = uiSubject.getProperty(VizUtil.AFV_HAS_BORDER).getResource();
						statementsToRemove.addAll(extractStatementsForResource(borderResource, model));
						statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, borderResource).toList());
					}

					if (uiSubject.hasProperty(VizUtil.AFV_HAS_CONTROL_POINT))
					{
						Resource controlPointResource = uiSubject.getProperty(VizUtil.AFV_HAS_CONTROL_POINT).getResource();
						statementsToRemove.addAll(extractStatementsForResource(controlPointResource, model));
						statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, controlPointResource).toList());
					}

					if (uiSubject.hasProperty(VizUtil.AFV_HAS_IMAGE))
					{
						Resource imageResource = uiSubject.getProperty(VizUtil.AFV_HAS_IMAGE).getResource();
						statementsToRemove.addAll(extractStatementsForResource(imageResource, model));
						statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, imageResource).toList());
					}

					if (uiSubject.hasProperty(VizUtil.AFV_HAS_CARDINALITY))
					{
						Resource cardinalityResource = uiSubject.getProperty(VizUtil.AFV_HAS_CARDINALITY).getResource();
						statementsToRemove.addAll(extractStatementsForResource(cardinalityResource, model));
						statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, cardinalityResource).toList());
					}

					if (uiSubject.hasProperty(VizUtil.AFV_HAS_HIDDEN_PROPERTY))
					{
						StmtIterator hiddenPropertyIterator = model.listStatements(uiSubject, VizUtil.AFV_HAS_HIDDEN_PROPERTY, (RDFNode) null);
						while (hiddenPropertyIterator.hasNext())
						{
							Statement statement2 = hiddenPropertyIterator.next();
							Resource hiddenPropertyResource = statement2.getResource();
							statementsToRemove.addAll(extractStatementsForResource(hiddenPropertyResource, model));
							statementsToRemove.addAll(model.listStatements((Resource) null, (Property) null, hiddenPropertyResource).toList());
						}
					}
				}
			}
		}

		if (!idsToRemove.isEmpty())
		{
			log.info("Cleaning model.");
			log.debug("Deleting resources and relations for Ids: " + StringUtils.join(idsToRemove.iterator(), ", \n"));
		}

		model.remove(statementsToRemove);

		return model;
	}

	@SuppressWarnings("deprecation")
	public Model createOrUpdateVisualizationModel(Path path, Model model) throws JAXBException, IOException
	{
		JAXBContext jc = JAXBContext.newInstance(CMap.class);
		// jc.generateSchema(new CMapSchemaOutputResolver(new File("src\\main\\resources")));

		Unmarshaller unmarshaller = jc.createUnmarshaller();
		CMap cmap = (CMap) unmarshaller.unmarshal(path.toFile());

		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new CmapNamespacePrefixMapper());

		ByteArrayOutputStream output = new ByteArrayOutputStream()
		{
			private StringBuilder string = new StringBuilder();

			@Override
			public void write(int b)
			{
				string.append((char) b);
			}
		};
		marshaller.marshal(cmap, output);
		log.debug(output.toString());

		List<Concept> concepts = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getConcepts() != null && cmap.getMap().getConcepts().getConceptList() != null)
		{
			concepts = cmap.getMap().getConcepts().getConceptList();
		}
		for (Iterator<Concept> iterator = concepts.iterator(); iterator.hasNext();)
		{
			Concept concept = iterator.next();
			String conceptId = concept.getId();
			Map<String, String> conceptProperties = CmapUtil.createOrRetrieveMapOfUiProperties(conceptId2UiProperties, conceptId);
			String label = concept.getLabel();
			if (label != null && !label.isEmpty())
			{
				label = unbreakString(label).trim();
				if (label.startsWith("[") && label.endsWith("]"))
				{
					conceptProperties.put(ConceptProperty.IS_BLANK_NODE.name(), "true");
					label = label.substring(1, label.length() - 1);
				}
				else if ((label.startsWith("\"") && label.contains("\"^^")) || label.contains("xsd:"))
				{
					conceptProperties.put(ConceptProperty.IS_LITERAL_NODE.name(), "true");
				}

				conceptProperties.put(ConceptProperty.TITLE.name(), label);

				Resource type = RdfUtil.getResourceByLabel(model, label, false, true);
				if (type != null)
				{
					if (type.hasProperty(AFOUtil.RDF_TYPE, AFOUtil.OWL_NAMED_INDIVIDUAL) && !type.getURI().startsWith(AFOUtil.AFDT_PREFIX))
					{
						conceptProperties.put(ConceptProperty.IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES.name(), "true");
					}
				}

			}

			String shortComment = concept.getShortComment();
			if (shortComment != null && !shortComment.isEmpty())
			{
				conceptProperties.put(ConceptProperty.SHORT_COMMENT.name(), shortComment);
			}

			String longComment = concept.getLongComment();
			if (longComment != null && !longComment.isEmpty() && !RdfCmap.ignoreLongComments)
			{
				conceptProperties.put(ConceptProperty.LONG_COMMENT.name(),
						NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(longComment)));
			}

			String parentId = concept.getParentId();
			if (parentId != null && !parentId.isEmpty())
			{
				conceptProperties.put(ConceptProperty.PARENT.name(), parentId);
			}

			conceptId2UiProperties.put(conceptId, conceptProperties);
		}

		List<ConceptAppearance> conceptAppearances = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getConceptAppearances() != null && cmap.getMap().getConceptAppearances().getConceptAppearances() != null)
		{
			conceptAppearances = cmap.getMap().getConceptAppearances().getConceptAppearances();
		}
		for (Iterator<ConceptAppearance> iterator = conceptAppearances.iterator(); iterator.hasNext();)
		{
			ConceptAppearance conceptAppearance = iterator.next();
			String conceptAppearanceId = conceptAppearance.getId();
			Map<String, String> conceptProperties = CmapUtil.createOrRetrieveMapOfUiProperties(conceptId2UiProperties, conceptAppearanceId);
			String x = conceptAppearance.getX();
			String y = conceptAppearance.getY();
			String width = conceptAppearance.getWidth();
			String height = conceptAppearance.getHeight();
			String fontStyle = conceptAppearance.getFontStyle();
			String fontSize = conceptAppearance.getFontSize();
			String backgroundColor = conceptAppearance.getBackgroundColor();
			String borderShape = conceptAppearance.getBorderShape();
			String borderStyle = conceptAppearance.getBorderStyle();
			String expanded = conceptAppearance.getExpanded();
			String backgroundImage = conceptAppearance.getBackgroundImage();
			String backgroundImageStyle = conceptAppearance.getBackgroundImageStyle();
			String backgroundImageLayout = conceptAppearance.getBackgroundImageLayout();

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

			if (backgroundImage != null && !backgroundImage.isEmpty())
			{
				conceptProperties.put(ConceptProperty.BACKGROUND_IMAGE.name(), backgroundImage);
			}

			if (backgroundImageStyle != null && !backgroundImageStyle.isEmpty())
			{
				conceptProperties.put(ConceptProperty.BACKGROUND_IMAGE_STYLE.name(), backgroundImageStyle);
			}

			if (backgroundImageLayout != null && !backgroundImageLayout.isEmpty())
			{
				conceptProperties.put(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name(), backgroundImageLayout);
			}

			if (borderShape != null && !borderShape.isEmpty())
			{
				conceptProperties.put(ConceptProperty.BORDER_SHAPE.name(), borderShape);
				if (borderShape.equals("rounded-rectangle"))
				{
					log.debug("Found class node. Check for punning.");
					conceptProperties.put(ConceptProperty.IS_CLASS.name(), "true");
				}
			}

			if (borderStyle != null && !borderStyle.isEmpty())
			{
				conceptProperties.put(ConceptProperty.BORDER_STYLE.name(), borderStyle);

				if (borderShape != null && borderShape.equals("oval"))
				{
					if (borderStyle.equals("dashed"))
					{
						log.debug("Found source node.");
						conceptProperties.put(ConceptProperty.IS_SOURCE_NODE.name(), "true");
					}
					else if (borderStyle.equals("solid"))
					{
						log.debug("Found target node.");
						conceptProperties.put(ConceptProperty.IS_TARGET_NODE.name(), "true");
					}
				}
			}

			if (expanded != null && !expanded.isEmpty())
			{
				conceptProperties.put(ConceptProperty.EXPANDED.name(), expanded);
			}

			conceptId2UiProperties.put(conceptAppearanceId, conceptProperties);
		}

		List<LinkingPhrase> links = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getLinkingPhrases() != null && cmap.getMap().getLinkingPhrases().getLinkingPhrases() != null)
		{
			links = cmap.getMap().getLinkingPhrases().getLinkingPhrases();
		}
		for (Iterator<LinkingPhrase> iterator = links.iterator(); iterator.hasNext();)
		{
			LinkingPhrase linkingPhrase = iterator.next();
			String linkingPhraseId = linkingPhrase.getId();
			Map<String, String> linkProperties = CmapUtil.createOrRetrieveMapOfUiProperties(fullLinkId2UiProperties, linkingPhraseId);
			String label = linkingPhrase.getLabel();
			if (label != null && !label.isEmpty())
			{
				label = unbreakString(label);

				Pattern p = Pattern.compile(CmapUtil.CARDINALITY_PATTERN, Pattern.CASE_INSENSITIVE);
				log.debug("Matching property label: " + label);
				Matcher m = p.matcher(label);
				String cardinalityString = StringUtils.EMPTY;
				if (m.find())
				{
					label = m.group(1).trim();
					cardinalityString = m.group(2).trim();
				}

				linkProperties.put(ConceptProperty.CARDINALITY.name(), cardinalityString);

				if (label.contains(":") && !label.toLowerCase().contains("af-x") && !label.toLowerCase().contains("afx") && !isOboProperty(label)
						&& !isQudtProperty(label))
				{
					// for properties of imported ontologies other than af-x/obo/qudt, spaces are not allowed and removed (must be wrong in label)
					label = label.replaceAll(" ", "");
				}

				linkProperties.put(ConceptProperty.TITLE.name(), label);
			}

			String shortComment = linkingPhrase.getShortComment();
			if (shortComment != null && !shortComment.isEmpty())
			{
				linkProperties.put(ConceptProperty.SHORT_COMMENT.name(), shortComment);
			}

			String longComment = linkingPhrase.getLongComment();
			if (longComment != null && !longComment.isEmpty() && !RdfCmap.ignoreLongComments)
			{
				linkProperties.put(ConceptProperty.LONG_COMMENT.name(), NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml(longComment)));
			}

			String parentId = linkingPhrase.getParentId();
			if (parentId != null && !parentId.isEmpty())
			{
				linkProperties.put(ConceptProperty.PARENT.name(), parentId);
			}

			fullLinkId2UiProperties.put(linkingPhraseId, linkProperties);
		}

		List<LinkingPhraseAppearance> linkAppearances = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getLinkingPhraseAppearances() != null
				&& cmap.getMap().getLinkingPhraseAppearances().getLinkingPhraseAppearances() != null)
		{
			linkAppearances = cmap.getMap().getLinkingPhraseAppearances().getLinkingPhraseAppearances();
		}
		for (Iterator<LinkingPhraseAppearance> iterator = linkAppearances.iterator(); iterator.hasNext();)
		{
			LinkingPhraseAppearance linkingPhraseAppearance = iterator.next();
			String linkingPhraseAppearanceId = linkingPhraseAppearance.getId();
			Map<String, String> linkProperties = CmapUtil.createOrRetrieveMapOfUiProperties(fullLinkId2UiProperties, linkingPhraseAppearanceId);

			String x = linkingPhraseAppearance.getX();
			String y = linkingPhraseAppearance.getY();
			String width = linkingPhraseAppearance.getWidth();
			String height = linkingPhraseAppearance.getHeight();
			String minWidth = linkingPhraseAppearance.getMinWidth();
			String minHeight = linkingPhraseAppearance.getMinHeight();
			String fontSize = linkingPhraseAppearance.getFontSize();
			String fontColor = linkingPhraseAppearance.getFontColor();
			String borderColor = linkingPhraseAppearance.getBorderColor();
			String backgroundColor = linkingPhraseAppearance.getBackgroundColor();
			String shadowColor = linkingPhraseAppearance.getShadowColor();
			String backgroundImage = linkingPhraseAppearance.getBackgroundImage();
			String backgroundImageStyle = linkingPhraseAppearance.getBackgroundImageStyle();
			String backgroundImageLayout = linkingPhraseAppearance.getBackgroundImageLayout();

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

			if (backgroundImage != null && !backgroundImage.isEmpty())
			{
				linkProperties.put(ConceptProperty.BACKGROUND_IMAGE.name(), backgroundImage);
			}

			if (backgroundImageStyle != null && !backgroundImageStyle.isEmpty())
			{
				linkProperties.put(ConceptProperty.BACKGROUND_IMAGE_STYLE.name(), backgroundImageStyle);
			}

			if (backgroundImageLayout != null && !backgroundImageLayout.isEmpty())
			{
				linkProperties.put(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name(), backgroundImageLayout);
			}

			if (shadowColor != null && !shadowColor.isEmpty())
			{
				linkProperties.put(ConceptProperty.SHADOW_COLOR.name(), shadowColor);
			}

			fullLinkId2UiProperties.put(linkingPhraseAppearanceId, linkProperties);
		}

		List<Connection> connections = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getConnections() != null && cmap.getMap().getConnections().getConnections() != null)
		{
			connections = cmap.getMap().getConnections().getConnections();
		}
		for (Iterator<Connection> iterator = connections.iterator(); iterator.hasNext();)
		{
			Connection connection = iterator.next();
			String connectionId = connection.getId();
			Map<String, String> connectionProperties = CmapUtil.createOrRetrieveMapOfUiProperties(connectionId2UiProperties, connectionId);
			String fromId = connection.getFromId();
			if (fromId != null && !fromId.isEmpty())
			{
				connectionProperties.put(ConceptProperty.CONNECTS_FROM.name(), fromId);
			}
			else
			{
				throw new IllegalStateException("Connection with id: " + connectionId + " has no source specified.");
			}

			String toId = connection.getToId();
			if (toId != null && !toId.isEmpty())
			{
				connectionProperties.put(ConceptProperty.CONNECTS_TO.name(), toId);
			}
			else
			{
				throw new IllegalStateException("Connection with id: " + connectionId + " has no target specified.");
			}

			connectionId2UiProperties.put(connectionId, connectionProperties);
		}

		List<ConnectionAppearance> connectionAppearances = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getConnectionAppearances() != null
				&& cmap.getMap().getConnectionAppearances().getConnectionAppearances() != null)
		{
			connectionAppearances = cmap.getMap().getConnectionAppearances().getConnectionAppearances();
		}
		for (Iterator<ConnectionAppearance> iterator = connectionAppearances.iterator(); iterator.hasNext();)
		{
			ConnectionAppearance connectionAppearance = iterator.next();
			String connectionAppearanceId = connectionAppearance.getId();
			Map<String, String> connectionProperties = CmapUtil.createOrRetrieveMapOfUiProperties(connectionId2UiProperties, connectionAppearanceId);
			String fromPos = connectionAppearance.getFromPos();
			if (fromPos != null && !fromPos.isEmpty())
			{
				connectionProperties.put(ConceptProperty.ANCHOR_FROM.name(), fromPos);
			}

			String toPos = connectionAppearance.getToPos();
			if (toPos != null && !toPos.isEmpty())
			{
				connectionProperties.put(ConceptProperty.ANCHOR_TO.name(), toPos);
			}

			String arrowHead = connectionAppearance.getArrowHead();
			if (arrowHead != null && !arrowHead.isEmpty())
			{
				connectionProperties.put(ConceptProperty.ARROW_HEAD.name(), arrowHead);
			}

			String type = connectionAppearance.getType();
			if (type != null && !type.isEmpty())
			{
				connectionProperties.put(ConceptProperty.LINE_TYPE.name(), type);
			}

			connectionId2UiProperties.put(connectionAppearanceId, connectionProperties);

			List<ControlPoint> controlPoints = new ArrayList<>();
			if (connectionAppearance.getControlPoints() != null)
			{
				controlPoints = connectionAppearance.getControlPoints();
			}
			int pointCount = 0;
			for (Iterator<ControlPoint> controlPointIterator = controlPoints.iterator(); controlPointIterator.hasNext();)
			{
				ControlPoint controlPoint = controlPointIterator.next();
				Map<String, String> controlPointProperties = CmapUtil.createOrRetrieveMapOfUiProperties(controlPointId2UiProperties, "");

				String x = controlPoint.getX();
				String y = controlPoint.getY();

				if (x != null && !x.isEmpty())
				{
					controlPointProperties.put(ConceptProperty.X.name(), x);
				}

				if (y != null && !y.isEmpty())
				{
					controlPointProperties.put(ConceptProperty.Y.name(), y);
				}

				controlPointProperties.put(ConceptProperty.CONNECTION_ID.name(), connectionAppearanceId);
				controlPointProperties.put(ConceptProperty.INDEX.name(), String.valueOf(pointCount));

				controlPointId2UiProperties.put(CmapUtil.URN_UUID + UUID.randomUUID().toString(), controlPointProperties); // control points must be newly
																															// created because they have no id
																															// in xml. Do not forget to replace
																															// existing control points.
				pointCount++;
			}
		}

		List<Image> images = new ArrayList<>();
		if (cmap.getMap() != null && cmap.getMap().getImages() != null && cmap.getMap().getImages().getImages() != null)
		{
			images = cmap.getMap().getImages().getImages();
		}
		for (Iterator<Image> iterator = images.iterator(); iterator.hasNext();)
		{
			Image image = iterator.next();
			String imageId = image.getId();
			Map<String, String> imageProperties = CmapUtil.createOrRetrieveMapOfUiProperties(imageId2UiProperties, imageId);
			String bytes = image.getBytes();

			if (bytes != null && !bytes.isEmpty())
			{
				imageProperties.put(ConceptProperty.BACKGROUND_IMAGE_BYTES.name(), bytes);
			}

			imageId2UiProperties.put(imageId, imageProperties);
		}

		if (!model.contains((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP))
		{
			CmapUtil.createMap(model);
		}

		VisualizationInfoBuilderResult visualizationInfoBuilderResult = VisualizationInfoBuilder.createOrUpdateVisualizationInfo(model, path,
				conceptId2UiProperties, fullLinkId2UiProperties, connectionId2UiProperties, controlPointId2UiProperties, imageId2UiProperties, resources);

		model = visualizationInfoBuilderResult.getModel();

		resources = visualizationInfoBuilderResult.getResources();

		return model;
	}

	public String unbreakString(String label)
	{
		label = label.replaceAll("&#10;", Matcher.quoteReplacement("\\n"));
		label = label.replaceAll("\\s+\\n\\s+", " ");
		label = label.replaceAll(Matcher.quoteReplacement("\\n"), "");
		label = label.replaceAll("\\s+", " ");
		return label;
	}

	private void prepareOutput(Path path, Model model) throws IOException, FileNotFoundException
	{
		String inputFileName = path.getFileName().toString();
		String outputFileName = inputFileName.substring(0, inputFileName.length() - 3) + "ttl";
		Path ttlPath = Paths.get(outputFileName);
		Files.deleteIfExists(ttlPath);
		ttlPath = Files.createFile(ttlPath);
		model.write(new FileOutputStream(ttlPath.toFile()), "TTL");

		if (RdfCmap.writeSeparateFiles)
		{
			writeSeparateTurtleFiles(model, inputFileName);
		}

		Model instanceModel = null;
		if (RdfCmap.writeFiles)
		{
			instanceModel = prepareTurtleOutput(model, inputFileName);// model.write(System.out,"TTL")
		}

		if (RdfCmap.createAdf)
		{
			if (RdfCmap.includeVocabulary)
			{
				Path adfPath = Paths.get(inputFileName.substring(0, inputFileName.length() - 4) + "-withAfo.adf");
				Files.deleteIfExists(adfPath);
				AdfCreator.create(adfPath, model, log);
			}
			else if (instanceModel != null)
			{
				Path adfPath = Paths.get(inputFileName.substring(0, inputFileName.length() - 4) + "-instances.adf");
				Files.deleteIfExists(adfPath);
				AdfCreator.create(adfPath, instanceModel, log);
			}
			else
			{
				log.error("Empty instance model. No ADF created.");
			}
		}
	}

	public PreparedModels prepareSeparatedModels(Model model)
	{
		log.info("Preparing instance model.");

		Model vizModel = ModelFactory.createDefaultModel();
		Model instanceModel = ModelFactory.createDefaultModel();
		Model otherTriplesModel = ModelFactory.createDefaultModel();
		if (RdfCmap.usePrefixes)
		{
			vizModel.setNsPrefixes(AFOUtil.nsPrefixMap);
			instanceModel.setNsPrefixes(AFOUtil.nsPrefixMap);
			otherTriplesModel.setNsPrefixes(AFOUtil.nsPrefixMap);
		}

		List<Statement> vizStatements = new ArrayList<>();
		List<Statement> instanceStatements = new ArrayList<>();
		List<Statement> otherStatements = new ArrayList<>();

		StmtIterator stmtIterator = model.listStatements();
		long blanks = 0;
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getSubject().isAnon() || (statement.getObject().isResource() && statement.getObject().isAnon()))
			{
				blanks++;
				otherStatements.add(statement);
				continue;
			}

			Resource subject = statement.getSubject();
			Resource subjectType = null;
			StmtIterator typeIterator = model.listStatements(subject, AFOUtil.RDF_TYPE, (RDFNode) null);
			if (typeIterator.hasNext())
			{
				subjectType = typeIterator.next().getResource();
			}

			boolean isConnection = false;
			boolean isLink = false;
			boolean isParent = false;
			if (subject.getURI().contains(CmapUtil.URN_UUID))
			{
				Resource uiResource = model.getResource(subject.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
				if (uiResource.hasProperty(AFOUtil.RDF_TYPE)
						&& uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI().equals(VizUtil.AFV_CONNECTION.getURI()))
				{
					isConnection = true;
				}
				if (uiResource.hasProperty(AFOUtil.RDF_TYPE)
						&& uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI().equals(VizUtil.AFV_LINK.getURI()))
				{
					isLink = true;
				}
				isParent = model.listStatements((Resource) null, VizUtil.AFV_HAS_PARENT, uiResource).hasNext();
			}

			if (subject.getURI().startsWith(CmapUtil.URN_UUID) && !(subjectType.getURI().contains(VizUtil.AFV_PREFIX)) && !isConnection && !isLink && !isParent
					&& !subjectType.getURI().equals(AFOUtil.OWL_OBJECT_PROPERTY.getURI()))
			{
				instanceStatements.add(statement);
			}
			else if (subject.getURI().startsWith(VizUtil.AFV_PREFIX)
					|| (subject.getURI().startsWith(CmapUtil.URN_UUID) && subjectType.getURI().contains(VizUtil.AFV_PREFIX)) || isConnection || isLink
					|| (subject.getURI().startsWith(CmapUtil.URN_UUID) && subjectType.getURI().equals(AFOUtil.OWL_OBJECT_PROPERTY)) || isParent)
			{
				vizStatements.add(statement);
			}
			else
			{
				otherStatements.add(statement);
			}
		}

		if (blanks > 0)
		{
			log.debug("Found " + blanks + " statements with blank nodes that were treated always as other statements.");
		}

		vizModel.add(vizStatements);

		otherTriplesModel.add(otherStatements);

		instanceModel.add(instanceStatements);

		if (instanceModel.isEmpty())
		{
			return new PreparedModels(instanceModel, vizModel, otherTriplesModel);
		}

		StmtIterator instanceIterator = instanceModel.listStatements(); // instanceModel.write(System.out, "TTL")
		Set<String> handledInstances = new HashSet<>();
		List<Statement> statementsToRemoveFromOtherTriplesModel = new ArrayList<>();
		Model tempModel = ModelFactory.createDefaultModel();
		Model singleInstanceModel = ModelFactory.createDefaultModel();
		while (instanceIterator.hasNext())
		{
			Statement statement = instanceIterator.next();
			String instanceId = statement.getSubject().getURI();
			if (handledInstances.contains(instanceId))
			{
				continue;
			}

			String title = StringUtils.EMPTY;
			Set<Statement> singleInstanceStatements = new HashSet<Statement>();
			StmtIterator singleInstanceIterator = model.listStatements(statement.getSubject(), (Property) null, (RDFNode) null);
			while (singleInstanceIterator.hasNext())
			{
				Statement singleInstanceStatement = singleInstanceIterator.next();
				singleInstanceStatements.add(singleInstanceStatement);
				singleInstanceStatements = CmapUtil.addStatementsWithBlankNodes(otherTriplesModel, singleInstanceStatement, singleInstanceStatements);

				if (AFOUtil.DCT_TITLE.getURI().equals(singleInstanceStatement.getPredicate().getURI()))
				{
					title = singleInstanceStatement.getString();
				}
			}

			singleInstanceModel.add(new ArrayList<Statement>(singleInstanceStatements));
			statementsToRemoveFromOtherTriplesModel.addAll(singleInstanceStatements);
			if (singleInstanceModel.isEmpty())
			{
				continue;
			}

			tempModel.add(singleInstanceModel);
			singleInstanceModel.removeAll();

			handledInstances.add(instanceId);
		}

		if (!otherTriplesModel.isEmpty())
		{
			// remove all statements of single instance from model of other triples -> no duplicates
			otherTriplesModel.remove(statementsToRemoveFromOtherTriplesModel);
		}

		if (!tempModel.isEmpty())
		{
			// remove all statements of single instance from model of other triples -> no duplicates
			instanceModel.add(tempModel);
		}

		if (!instanceModel.isEmpty())
		{
			// remove all statements of single instance from model of other triples -> no duplicates
			instanceModel.add(tempModel);

			if (RdfCmap.useBlankNodes)
			{
				instanceModel = transformToAnonymousInstances(instanceModel);
			}

			instanceModel = replaceLiteralNodes(instanceModel);

			instanceModel = replaceNamedIndividualsOfOntologies(instanceModel, model);

			instanceModel = addClassNodes(instanceModel, model);

			if (!RdfCmap.addDctTitles)
			{
				instanceModel = removeTitles(instanceModel);
			}
		}

		return new PreparedModels(instanceModel, vizModel, otherTriplesModel);
	}

	/**
	 * Write TTL files with entities belonging together
	 *
	 * @param model
	 * @return instance model
	 * @param inputFileName
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private Model prepareTurtleOutput(Model model, String inputFileName) throws IOException, FileNotFoundException
	{
		PreparedModels preparedModels = prepareSeparatedModels(model);

		log.info("Writing turtle files.");

		if (Files.notExists(outputFolder.toPath()))
		{
			Files.createDirectory(outputFolder.toPath());
		}
		else
		{
			FileUtils.cleanDirectory(outputFolder);
		}

		Model vizModel = preparedModels.getVisualizationModel();
		if (!vizModel.isEmpty())
		{
			String vizModelOutputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "-visualization-model.ttl";
			Path vizTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + vizModelOutputFileName);
			vizTtlPath = Files.createFile(vizTtlPath);
			vizModel.write(new FileOutputStream(vizTtlPath.toFile()), "TTL");
		}

		Model instanceModel = preparedModels.getInstanceModel();
		if (!instanceModel.isEmpty())
		{
			String instanceModelOutputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "-instance-model.ttl";
			Path instanceTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + instanceModelOutputFileName);
			instanceTtlPath = Files.createFile(instanceTtlPath);
			instanceModel.write(new FileOutputStream(instanceTtlPath.toFile()), "TTL");

			if (RdfCmap.humanReadable)
			{
				List<String> lines = Files.readAllLines(instanceTtlPath, Charset.defaultCharset());

				lines = addCommentsWithHumanReadableIds(lines, model);

				instanceModelOutputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "-instance-model-human-readable.ttl";
				instanceTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + instanceModelOutputFileName);
				instanceTtlPath = Files.createFile(instanceTtlPath);

				writeFile(instanceTtlPath, lines);
			}
		}

		Model otherTriplesModel = preparedModels.getOtherModel();
		if (!otherTriplesModel.isEmpty())
		{
			writeOtherTriples(inputFileName, otherTriplesModel);
		}

		return instanceModel;
	}

	private Model addClassNodes(Model instanceModel, Model model)
	{
		log.debug("Replacing named individuals that are not instance data but classes (e.g. chebi:17790).");

		Set<String> allClassNodes = new HashSet<String>();

		StmtIterator stmtIterator = instanceModel.listStatements((Resource) null, VizUtil.AFV_IS_CLASS, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (allClassNodes.contains(subject.getURI()))
			{
				continue;
			}

			allClassNodes.add(subject.getURI());
		}

		Iterator<String> namedNodesIterator = allClassNodes.iterator();
		while (namedNodesIterator.hasNext())
		{
			String uri = namedNodesIterator.next();
			Resource namedNode = instanceModel.getResource(uri);

			String label = namedNode.getProperty(AFOUtil.DCT_TITLE).getString();

			Resource correspondingTermOfOntologies = RdfUtil.getResourceByLabel(model, label, false, true);
			if (correspondingTermOfOntologies == null)
			{
				String[] segments = label.split(":");
				String namespace = StringUtils.EMPTY;
				String filterLabel;
				if (segments.length > 1)
				{
					namespace = AFOUtil.nsPrefixMap.get(segments[0]);
					if (namespace == null || namespace.isEmpty())
					{
						log.info("Missing prefix for term: " + label);
					}
					filterLabel = segments[1];
				}
				else
				{
					namespace = AFOUtil.AFO_PREFIX;
					filterLabel = label;
				}

				correspondingTermOfOntologies = ResourceFactory.createResource(namespace + filterLabel);
			}

			Set<Statement> statementsToRemove = new HashSet<Statement>();
			Set<Statement> statementsToAdd = new HashSet<Statement>();

			StmtIterator namedNodeIterator = instanceModel.listStatements(namedNode, (Property) null, (RDFNode) null);
			while (namedNodeIterator.hasNext())
			{
				Statement statement = namedNodeIterator.next();
				statementsToRemove.add(statement);
			}

			namedNodeIterator = instanceModel.listStatements((Resource) null, (Property) null, namedNode);
			while (namedNodeIterator.hasNext())
			{
				Statement statement = namedNodeIterator.next();
				statementsToRemove.add(statement);
				Statement newStatement = instanceModel.createStatement(statement.getSubject(), statement.getPredicate(), correspondingTermOfOntologies);
				statementsToAdd.add(newStatement);
			}

			instanceModel.remove(new ArrayList<Statement>(statementsToRemove));
			instanceModel.add(new ArrayList<Statement>(statementsToAdd));
		}

		return instanceModel;
	}

	private Model removeTitles(Model instanceModel)
	{
		log.debug("Remove all dct:titles.");

		Set<Statement> statementsToRemove = new HashSet<Statement>();
		statementsToRemove.addAll(instanceModel.listStatements((Resource) null, AFOUtil.DCT_TITLE, (RDFNode) null).toSet());
		instanceModel.remove(new ArrayList<Statement>(statementsToRemove));

		return instanceModel;
	}

	public static void writeFile(Path file, List<String> lines) throws IOException
	{
		FileWriter fileWriter = new FileWriter(file.toAbsolutePath().toString());
		String newLine = System.getProperty("line.separator");
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			fileWriter.write(line + newLine);
		}
		fileWriter.close();
	}

	public static List<String> addCommentsWithHumanReadableIds(List<String> lines, Model model)
	{
		List<String> processedLines = new ArrayList<String>(lines.size());

		String regex = "(?:^|\\s)([a-z\\-]+):([A-Z]*\\_[0-9]{7,}(?:$|\\s))";
		Pattern p = Pattern.compile(regex);
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();)
		{
			String line = iterator.next();
			log.debug("<-- " + line);

			Matcher m = p.matcher(line);

			List<String> labels = new ArrayList<String>();
			while (m.find())
			{
				if (line.contains("#") && line.indexOf("#") < m.start(1))
				{
					continue;
				}

				String iri = AFOUtil.nsPrefixMap.get(m.group(1).trim());
				String localName = m.group(2).trim();
				if (iri.equals(AFOUtil.OBO_PREFIX))
				{
					iri = iri + localName;
				}
				else if (iri.contains(AFOUtil.OBO_PREFIX))
				{
					String[] segments = localName.split("_");
					iri = iri + "_" + segments[1];
				}
				else
				{
					iri = iri + localName;
				}
				Resource resource = model.getResource(iri);
				if (resource.hasProperty(AFOUtil.SKOS_PREF_LABEL))
				{
					labels.add(m.group(1).trim() + ":" + resource.getProperty(AFOUtil.SKOS_PREF_LABEL).getString());
				}
				else if (resource.hasProperty(AFOUtil.RDFS_LABEL))
				{
					labels.add(m.group(1).trim() + ":" + resource.getProperty(AFOUtil.RDFS_LABEL).getString());
				}
				else
				{
					labels.add("unknown");
				}
			}

			if (!labels.isEmpty())
			{
				String fillSpace = StringUtils.EMPTY;
				if (line.length() < 120)
				{
					for (int i = line.length(); i < 120; i++)
					{
						fillSpace += " ";
					}
				}

				line = line + fillSpace + " # " + StringUtils.join(labels, ", ");
			}

			processedLines.add(line);
			log.debug("--> " + line);
		}
		return processedLines;
	}

	private Model replaceNamedIndividualsOfOntologies(Model instanceModel, Model model)
	{
		log.debug("Replacing named individuals that are not instance data but belong to ontologies (e.g. units).");

		Set<String> allNodes = new HashSet<String>();

		StmtIterator stmtIterator = instanceModel.listStatements((Resource) null, VizUtil.AFV_IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (allNodes.contains(subject.getURI()))
			{
				continue;
			}

			allNodes.add(subject.getURI());
		}

		Iterator<String> namedNodesIterator = allNodes.iterator();
		while (namedNodesIterator.hasNext())
		{
			String uri = namedNodesIterator.next();
			Resource namedNode = instanceModel.getResource(uri);

			String label = namedNode.getProperty(AFOUtil.DCT_TITLE).getString();

			Resource correspondingIndividualOfOntologies = RdfUtil.getResourceByLabel(model, label, false, true);

			Set<Statement> statementsToRemove = new HashSet<Statement>();
			Set<Statement> statementsToAdd = new HashSet<Statement>();

			StmtIterator namedNodeIterator = instanceModel.listStatements(namedNode, (Property) null, (RDFNode) null);
			while (namedNodeIterator.hasNext())
			{
				Statement statement = namedNodeIterator.next();
				statementsToRemove.add(statement);
			}

			namedNodeIterator = instanceModel.listStatements((Resource) null, (Property) null, namedNode);
			while (namedNodeIterator.hasNext())
			{
				Statement statement = namedNodeIterator.next();
				statementsToRemove.add(statement);
				Statement newStatement = instanceModel.createStatement(statement.getSubject(), statement.getPredicate(), correspondingIndividualOfOntologies);
				statementsToAdd.add(newStatement);
			}

			instanceModel.remove(new ArrayList<Statement>(statementsToRemove));
			instanceModel.add(new ArrayList<Statement>(statementsToAdd));
		}

		return instanceModel;
	}

	private Model replaceLiteralNodes(Model model)
	{
		log.debug("Replacing literal nodes for literal values.");

		Set<String> allLiteralNodes = new HashSet<String>();

		StmtIterator stmtIterator = model.listStatements((Resource) null, VizUtil.AFV_IS_LITERAL_NODE, (RDFNode) null);
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (allLiteralNodes.contains(subject.getURI()))
			{
				continue;
			}

			allLiteralNodes.add(subject.getURI());
		}

		Iterator<String> literalNodesIterator = allLiteralNodes.iterator();
		while (literalNodesIterator.hasNext())
		{
			String uri = literalNodesIterator.next();
			Resource literalNode = model.getResource(uri);

			Set<Statement> statementsToRemove = new HashSet<Statement>();
			Set<Statement> statementsToAdd = new HashSet<Statement>();

			String literalValue = StringUtils.EMPTY;
			StmtIterator literalNodeIterator = model.listStatements(literalNode, (Property) null, (RDFNode) null);
			while (literalNodeIterator.hasNext())
			{
				Statement statement = literalNodeIterator.next();
				statementsToRemove.add(statement);

				if (statement.getPredicate().equals(AFOUtil.DCT_TITLE))
				{
					literalValue = statement.getString();
				}
			}
			String literalValueString = StringUtils.EMPTY;
			Object literalValueAsObject = null;
			if (literalValue.contains("\"^^"))
			{
				String[] segments = literalValue.split("\\^\\^");
				literalValueString = segments[0].substring(1, segments[0].length() - 1); // cut quotes
				String[] dataTypeSegments = segments[1].split(":");
				String dataTypeIri = AFOUtil.nsPrefixMap.get(dataTypeSegments[0]) + dataTypeSegments[1];

				if (AFOUtil.XSD_STRING.getURI().equals(dataTypeIri))
				{
					literalValueAsObject = literalValueString;
				}
				else if (AFOUtil.XSD_DOUBLE.getURI().equals(dataTypeIri))
				{
					literalValueAsObject = Double.parseDouble(literalValueString);
				}
				else if (AFOUtil.XSD_INTEGER.getURI().equals(dataTypeIri))
				{
					literalValueAsObject = Integer.parseInt(literalValueString);
				}
				else if (AFOUtil.XSD_DATETIME.getURI().equals(dataTypeIri))
				{
					// e.g. "2017-05-13T15:25:00Z"^^xsd:dateTime
					literalValueAsObject = javax.xml.bind.DatatypeConverter.parseDateTime(literalValueString);
				}
				else if (AFOUtil.XSD_BOOLEAN.getURI().equals(dataTypeIri))
				{
					literalValueAsObject = Boolean.parseBoolean(literalValueString);
				}
				else
				{
					log.info("Unknown datatype found: \"" + dataTypeIri + "\". Assuming xsd:string.");
					literalValueAsObject = literalValueString;
				}
			}
			else if (literalValue.contains("xsd:"))
			{
				literalValueAsObject = literalValue;
			}
			else
			{
				log.error("Unhandled literal datatype: " + literalValue + ". Assuming xsd:string.");
				literalValueAsObject = literalValue;
			}

			literalNodeIterator = model.listStatements((Resource) null, (Property) null, literalNode);
			while (literalNodeIterator.hasNext())
			{
				Statement statement = literalNodeIterator.next();
				statementsToRemove.add(statement);
				Statement newStatement = model.createLiteralStatement(statement.getSubject(), statement.getPredicate(),
						ResourceFactory.createTypedLiteral(literalValueAsObject));
				statementsToAdd.add(newStatement);
			}

			model.remove(new ArrayList<Statement>(statementsToRemove));
			model.add(new ArrayList<Statement>(statementsToAdd));
		}

		return model;
	}

	private Model transformToAnonymousInstances(Model model)
	{
		log.debug("Adding blank nodes.");

		Set<String> allBlanks = new HashSet<String>();

		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();
			if (allBlanks.contains(subject.getURI()))
			{
				continue;
			}

			if (!subject.hasProperty(VizUtil.AFV_HAS_UUID))
			{
				continue;
			}

			allBlanks.add(subject.getURI());
		}

		Iterator<String> blanksIterator = allBlanks.iterator();
		while (blanksIterator.hasNext())
		{
			String uri = blanksIterator.next();

			Set<Statement> statementsToRemove = new HashSet<Statement>();
			Set<Statement> statementsToAdd = new HashSet<Statement>();

			Resource namedResourceToReplacebyBlank = model.getResource(uri);
			Resource blankNode = model.createResource();

			StmtIterator blankStmtIterator = model.listStatements(namedResourceToReplacebyBlank, (Property) null, (RDFNode) null);
			while (blankStmtIterator.hasNext())
			{
				Statement statement = blankStmtIterator.next();
				statementsToRemove.add(statement);
				if (statement.getPredicate().equals(AFOUtil.RDF_TYPE) && statement.getObject().isURIResource()
						&& statement.getObject().equals(AFOUtil.OWL_NAMED_INDIVIDUAL))
				{
					continue;
				}
				if (statement.getPredicate().equals(VizUtil.AFV_HAS_UUID) && !log.isDebugEnabled())
				{
					// enable debug mode to keep uuids for debugging
					continue;
				}

				Statement newStatement;
				if (statement.getObject().isAnon())
				{
					Resource blankNodeObject = model.createResource(new AnonId(statement.getObject().asNode().getBlankNodeId()));
					newStatement = ResourceFactory.createStatement(blankNode, statement.getPredicate(), blankNodeObject);
				}
				else
				{
					newStatement = ResourceFactory.createStatement(blankNode, statement.getPredicate(), statement.getObject());
				}
				statementsToAdd.add(newStatement);
			}

			blankStmtIterator = model.listStatements((Resource) null, (Property) null, namedResourceToReplacebyBlank);
			while (blankStmtIterator.hasNext())
			{
				Statement statement = blankStmtIterator.next();
				if (statement.getPredicate().equals(VizUtil.AFV_HAS_UUID) && log.isDebugEnabled())
				{
					// enable debug mode to keep uuids for debugging
					continue;
				}

				statementsToRemove.add(statement);

				if (statement.getPredicate().equals(VizUtil.AFV_HAS_UUID) && !log.isDebugEnabled())
				{
					// enable debug mode to keep uuids for debugging
					continue;
				}

				Statement newStatement;
				if (statement.getSubject().isAnon())
				{
					Resource blankNodeSubject = model.createResource(new AnonId(statement.getSubject().asNode().getBlankNodeId()));
					newStatement = ResourceFactory.createStatement(blankNodeSubject, statement.getPredicate(), blankNode);
				}
				else
				{
					newStatement = ResourceFactory.createStatement(statement.getSubject(), statement.getPredicate(), blankNode);
				}

				statementsToAdd.add(newStatement);
			}

			model.remove(new ArrayList<Statement>(statementsToRemove));
			model.add(new ArrayList<Statement>(statementsToAdd));
		}

		return model;
	}

	private void writeSeparateTurtleFiles(Model model, String inputFileName) throws IOException, FileNotFoundException
	{
		log.info("Writing separate turtle files.");
		if (Files.notExists(outputFolder.toPath()))
		{
			Files.createDirectory(outputFolder.toPath());
		}
		else
		{
			FileUtils.cleanDirectory(outputFolder);
		}

		Model vizModel = ModelFactory.createDefaultModel();
		Model instanceModel = ModelFactory.createDefaultModel();
		Model otherTriplesModel = ModelFactory.createDefaultModel();
		if (RdfCmap.usePrefixes)
		{
			vizModel.setNsPrefixes(AFOUtil.nsPrefixMap);
			instanceModel.setNsPrefixes(AFOUtil.nsPrefixMap);
			otherTriplesModel.setNsPrefixes(AFOUtil.nsPrefixMap);
		}

		List<Statement> vizStatements = new ArrayList<>();
		List<Statement> instanceStatements = new ArrayList<>();
		List<Statement> otherStatements = new ArrayList<>();
		StmtIterator stmtIterator = model.listStatements();
		long blanks = 0;
		while (stmtIterator.hasNext())
		{
			Statement statement = stmtIterator.next();
			if (statement.getSubject().isAnon() || (statement.getObject().isResource() && statement.getObject().isAnon()))
			{
				blanks++;
				otherStatements.add(statement);
				continue;
			}

			Resource subject = statement.getSubject();
			Resource subjectType = null;
			StmtIterator typeIterator = model.listStatements(subject, AFOUtil.RDF_TYPE, (RDFNode) null);
			if (typeIterator.hasNext())
			{
				subjectType = typeIterator.next().getResource();
			}

			boolean isConnection = false;
			boolean isLink = false;
			boolean isParent = false;
			if (subject.getURI().contains(CmapUtil.URN_UUID))
			{
				Resource uiResource = model.getResource(subject.getURI().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
				if (uiResource.hasProperty(AFOUtil.RDF_TYPE)
						&& uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI().equals(VizUtil.AFV_CONNECTION.getURI()))
				{
					isConnection = true;
				}
				if (uiResource.hasProperty(AFOUtil.RDF_TYPE)
						&& uiResource.getProperty(AFOUtil.RDF_TYPE).getResource().getURI().equals(VizUtil.AFV_LINK.getURI()))
				{
					isLink = true;
				}
				isParent = model.listStatements((Resource) null, VizUtil.AFV_HAS_PARENT, uiResource).hasNext();
			}

			if (subject.getURI().startsWith(CmapUtil.URN_UUID) && !(subjectType.getURI().contains(VizUtil.AFV_PREFIX)) && !isConnection && !isLink && !isParent
					&& !subjectType.getURI().equals(AFOUtil.OWL_OBJECT_PROPERTY.getURI()))
			{
				instanceStatements.add(statement);
			}
			else if (subject.getURI().startsWith(VizUtil.AFV_PREFIX)
					|| (subject.getURI().startsWith(CmapUtil.URN_UUID) && subjectType.getURI().contains(VizUtil.AFV_PREFIX)) || isConnection || isLink
					|| (subject.getURI().startsWith(CmapUtil.URN_UUID) && subjectType.getURI().equals(AFOUtil.OWL_OBJECT_PROPERTY)) || isParent)
			{
				vizStatements.add(statement);
			}
			else
			{
				otherStatements.add(statement);
			}
		}

		if (blanks > 0)
		{
			log.debug("Found " + blanks + " statements with blank nodes that were treated always as other statements.");
		}

		vizModel.add(vizStatements);
		if (!vizModel.isEmpty())
		{
			String vizModelOutputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "-visualization-model.ttl";
			Path vizTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + vizModelOutputFileName);
			vizTtlPath = Files.createFile(vizTtlPath);
			vizModel.write(new FileOutputStream(vizTtlPath.toFile()), "TTL");
		}

		otherTriplesModel.add(otherStatements);

		instanceModel.add(instanceStatements);
		if (instanceModel.isEmpty())
		{
			return;
		}

		StmtIterator instanceIterator = instanceModel.listStatements();
		Set<String> handledInstances = new HashSet<>();
		List<Statement> statementsToRemoveFromOtherTriplesModel = new ArrayList<>();
		Model singleInstanceModel = ModelFactory.createDefaultModel();
		if (RdfCmap.usePrefixes)
		{
			singleInstanceModel.setNsPrefixes(AFOUtil.nsPrefixMap);
		}

		while (instanceIterator.hasNext())
		{
			Statement statement = instanceIterator.next();
			String instanceId = statement.getSubject().getURI();
			if (handledInstances.contains(instanceId))
			{
				continue;
			}

			String title = StringUtils.EMPTY;
			Set<Statement> singleInstanceStatements = new HashSet<Statement>();
			StmtIterator singleInstanceIterator = model.listStatements(statement.getSubject(), (Property) null, (RDFNode) null);
			while (singleInstanceIterator.hasNext())
			{
				Statement singleInstanceStatement = singleInstanceIterator.next();
				singleInstanceStatements.add(singleInstanceStatement);
				singleInstanceStatements = CmapUtil.addStatementsWithBlankNodes(otherTriplesModel, singleInstanceStatement, singleInstanceStatements);

				if (AFOUtil.DCT_TITLE.getURI().equals(singleInstanceStatement.getPredicate().getURI()))
				{
					title = singleInstanceStatement.getString();
				}
			}

			singleInstanceModel.add(new ArrayList<Statement>(singleInstanceStatements));
			statementsToRemoveFromOtherTriplesModel.addAll(singleInstanceStatements);
			if (singleInstanceModel.isEmpty())
			{
				continue;
			}

			if (title.isEmpty())
			{
				title = instanceId;
			}
			title = title.replaceAll("\\W+", " ");
			title = title.toLowerCase().replaceAll("\\s", "-");
			if (title.isEmpty() || title.equals("-"))
			{
				title = "unlabeled";
			}
			if (title.startsWith("-") && title.endsWith("-"))
			{
				title = "[" + title.substring(1, title.length() - 1) + "]";
			}
			String singleInstanceModelOutputFileName = title + ".ttl";
			Path singleInstanceTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + singleInstanceModelOutputFileName);
			int index = 1;
			while (Files.exists(singleInstanceTtlPath))
			{
				singleInstanceTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + title + "_" + index + ".ttl");
				index++;
			}
			singleInstanceTtlPath = Files.createFile(singleInstanceTtlPath);
			singleInstanceModel.write(new FileOutputStream(singleInstanceTtlPath.toFile()), "TTL");
			singleInstanceModel.removeAll();

			handledInstances.add(instanceId);
		}

		if (!otherTriplesModel.isEmpty())
		{
			// remove all statements of single instance from model of other triples -> no duplicates
			otherTriplesModel.remove(statementsToRemoveFromOtherTriplesModel);
		}

		if (!otherTriplesModel.isEmpty())
		{
			writeOtherTriples(inputFileName, otherTriplesModel);
		}

	}

	private void writeOtherTriples(String inputFileName, Model otherTriplesModel) throws IOException, FileNotFoundException
	{
		String otherModelOutputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "-other-triples.ttl";
		Path otherTtlPath = Paths.get(outputFolder.getAbsolutePath() + "\\" + otherModelOutputFileName);
		Files.deleteIfExists(otherTtlPath);
		otherTtlPath = Files.createFile(otherTtlPath);
		otherTriplesModel.write(new FileOutputStream(otherTtlPath.toFile()), "TTL");
	}

	private static boolean isOboProperty(String label)
	{
		if (label.toLowerCase().startsWith("obo"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("obi"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("iao"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("bfo"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("ro"))
		{
			return true;
		}
		return false;
	}

	private static boolean isQudtProperty(String label)
	{
		if (label.toLowerCase().startsWith("qudt"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("unit"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("qudt-ext"))
		{
			return true;
		}
		if (label.toLowerCase().startsWith("unit-ext"))
		{
			return true;
		}
		return false;
	}
}
