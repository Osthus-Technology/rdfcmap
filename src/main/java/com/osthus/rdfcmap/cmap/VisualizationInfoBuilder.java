package com.osthus.rdfcmap.cmap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.cmap.cardinality.Cardinality;
import com.osthus.rdfcmap.enums.ConceptProperty;
import com.osthus.rdfcmap.helper.VisualizationInfoBuilderResult;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.RdfUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * VisualizationInfoBuilder
 * 
 * Generate visualization model based on cmap info.
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class VisualizationInfoBuilder {
	private static final Logger log = LogManager.getLogger("Logger");

	public static VisualizationInfoBuilderResult createOrUpdateVisualizationInfo(Model model, Path path,
			Map<String, Map<String, String>> conceptId2UiProperties,
			Map<String, Map<String, String>> fullLinkId2UiProperties,
			Map<String, Map<String, String>> connectionId2UiProperties,
			Map<String, Map<String, String>> controlPointId2UiProperties,
			Map<String, Map<String, String>> imageId2UiProperties, List<Resource> resources) {
		log.info("Creating or updating visualization info.");
		Long x = 10l;
		Long y = 10l;
		for (Entry<String, Map<String, String>> entry : conceptId2UiProperties.entrySet()) {
			Resource concept;
			Resource uiConcept;
			if (entry.getKey().contains(VizUtil.AFV_PREFIX)) {
				uiConcept = model.createResource(entry.getKey());
			} else if (entry.getKey().contains(CmapUtil.URN_UUID)) {
				uiConcept = model.createResource(entry.getKey().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			} else {
				// new concept, new instance
				String title = entry.getValue().get(ConceptProperty.TITLE.name());
				Resource existingConceptWithSameVizId = findExistingUiResource(model, entry.getKey(),
						VizUtil.AFV_CONCEPT);
				if (existingConceptWithSameVizId == null) {
					String id = UUID.randomUUID().toString();
					concept = model.createResource(CmapUtil.URN_UUID + id);
					uiConcept = model.createResource(VizUtil.AFV_PREFIX + id);
					model = CmapUtil.createOrUpdateRelatedResource(model, uiConcept, AFOUtil.AFX_HAS_OBJECT, concept);
					model = CmapUtil.createOrUpdateRelatedResource(model, concept, AFOUtil.RDF_TYPE,
							AFOUtil.OWL_NAMED_INDIVIDUAL);
					model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IDENTIFIER,
							entry.getKey());
					log.debug("Added new concept with title: "
							+ ((title == null || title.isEmpty()) ? "<unknown>" : "\"" + title + "\"") + " new ID: "
							+ uiConcept.getURI());
				} else {
					// reuse existing concept
					uiConcept = existingConceptWithSameVizId;
					concept = model.listStatements(uiConcept, AFOUtil.AFX_HAS_OBJECT, (RDFNode) null).next()
							.getResource();
					String existingTitle = uiConcept.getProperty(AFOUtil.DCT_TITLE).getString();
					log.debug("Reusing existing concept <" + uiConcept.getURI() + "> with title: " + "\""
							+ existingTitle + "\" for new concept with title "
							+ ((title == null || title.isEmpty()) ? "<unknown>" : "\"" + title + "\"")
							+ " and given ID: " + entry.getKey());
				}
			}

			if (!uiConcept.hasProperty(AFOUtil.AFX_HAS_OBJECT)) {
				// this can happen if input file contains RDF UI concepts but
				// not RDF model
				log.debug("Found UI concept without relation to RDF concept. Adding required relations.");
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateRelatedResource(model, uiConcept, AFOUtil.AFX_HAS_OBJECT, concept);
				model = CmapUtil.createOrUpdateRelatedResource(model, concept, AFOUtil.RDF_TYPE,
						AFOUtil.OWL_NAMED_INDIVIDUAL);
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IDENTIFIER, entry.getKey());
			}

			resources.add(uiConcept);

			model = CmapUtil.createOrUpdateRelatedResource(model, uiConcept, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT);
			model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, AFOUtil.DCT_IDENTIFIER, uiConcept.getURI());

			// resolve title against ontologies and add rdf:type
			Resource type = RdfUtil.getResourceByLabel(model, entry.getValue().get(ConceptProperty.TITLE.name()), false,
					true);
			if (type != null) {
				model = CmapUtil.createOrUpdateRelatedResource(model,
						model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID)),
						AFOUtil.RDF_TYPE, type);
			} else if (entry.getValue().get(ConceptProperty.TITLE.name()) != null
					&& !entry.getValue().get(ConceptProperty.TITLE.name()).contains("\"^^")
					&& !entry.getValue().get(ConceptProperty.TITLE.name()).toLowerCase().contains("xsd:")) {
				log.info("Found unresolved label: " + entry.getValue().get(ConceptProperty.TITLE.name()));
			}

			model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, AFOUtil.DCT_TITLE,
					entry.getValue().get(ConceptProperty.TITLE.name()));

			if (!model.contains(uiConcept, VizUtil.AFV_HAS_MAP, (RDFNode) null)) {
				Resource map = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP).next()
						.getSubject();
				uiConcept.addProperty(VizUtil.AFV_HAS_MAP, map);
			}

			if (entry.getValue().containsKey(ConceptProperty.SHORT_COMMENT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_SHORT_COMMENT,
						entry.getValue().get(ConceptProperty.SHORT_COMMENT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_SHORT_COMMENT, "");
			}

			if (entry.getValue().containsKey(ConceptProperty.LONG_COMMENT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_LONG_COMMENT,
						entry.getValue().get(ConceptProperty.LONG_COMMENT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_LONG_COMMENT, "");
			}

			if (entry.getValue().containsKey(ConceptProperty.PARENT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_HAS_PARENT_ID,
						entry.getValue().get(ConceptProperty.PARENT.name()));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_HAS_PARENT_ID, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.EXPANDED.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_EXPANDED,
						entry.getValue().get(ConceptProperty.EXPANDED.name()));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_EXPANDED, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.IS_BLANK_NODE.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IS_BLANK_NODE,
						entry.getValue().get(ConceptProperty.IS_BLANK_NODE.name()));
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateRelatedResource(model, concept, VizUtil.AFV_HAS_UUID, concept);
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_IS_BLANK_NODE, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				statementsToRemove = model.listStatements(concept, VizUtil.AFV_HAS_UUID, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.IS_LITERAL_NODE.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IS_LITERAL_NODE,
						entry.getValue().get(ConceptProperty.IS_LITERAL_NODE.name()));
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateLiteralValue(model, concept, VizUtil.AFV_IS_LITERAL_NODE,
						entry.getValue().get(ConceptProperty.IS_LITERAL_NODE.name()));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_IS_LITERAL_NODE, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				statementsToRemove = model.listStatements(concept, VizUtil.AFV_IS_LITERAL_NODE, (RDFNode) null)
						.toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept,
						VizUtil.AFV_IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES,
						entry.getValue().get(ConceptProperty.IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES.name()));
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateLiteralValue(model, concept,
						VizUtil.AFV_IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES,
						entry.getValue().get(ConceptProperty.IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES.name()));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES, (RDFNode) null)
						.toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				statementsToRemove = model
						.listStatements(concept, VizUtil.AFV_IS_NAMED_INDIVIDUAL_OF_ONTOLOGIES, (RDFNode) null)
						.toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.IS_CLASS.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IS_CLASS,
						entry.getValue().get(ConceptProperty.IS_CLASS.name()));
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateLiteralValue(model, concept, VizUtil.AFV_IS_CLASS,
						entry.getValue().get(ConceptProperty.IS_CLASS.name()));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_IS_CLASS, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				statementsToRemove = model.listStatements(concept, VizUtil.AFV_IS_CLASS, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.IS_TARGET_NODE.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IS_TARGET_NODE,
						Boolean.parseBoolean(entry.getValue().get(ConceptProperty.IS_TARGET_NODE.name())));
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateLiteralValue(model, concept, VizUtil.AFV_IS_TARGET_NODE,
						Boolean.parseBoolean(entry.getValue().get(ConceptProperty.IS_TARGET_NODE.name())));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_IS_TARGET_NODE, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				statementsToRemove = model.listStatements(concept, VizUtil.AFV_IS_TARGET_NODE, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.IS_SOURCE_NODE.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_IS_SOURCE_NODE,
						Boolean.parseBoolean(entry.getValue().get(ConceptProperty.IS_SOURCE_NODE.name())));
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateLiteralValue(model, concept, VizUtil.AFV_IS_SOURCE_NODE,
						Boolean.parseBoolean(entry.getValue().get(ConceptProperty.IS_SOURCE_NODE.name())));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_IS_SOURCE_NODE, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				concept = model.getResource(uiConcept.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				statementsToRemove = model.listStatements(concept, VizUtil.AFV_IS_SOURCE_NODE, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.X.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_X_POSITION,
						entry.getValue().get(ConceptProperty.X.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_X_POSITION, x.toString());
			}

			if (entry.getValue().containsKey(ConceptProperty.Y.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_Y_POSITION,
						entry.getValue().get(ConceptProperty.Y.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_Y_POSITION, y.toString());
			}

			if (entry.getValue().containsKey(ConceptProperty.WIDTH.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_WIDTH,
						entry.getValue().get(ConceptProperty.WIDTH.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_WIDTH, "100");
			}

			if (entry.getValue().containsKey(ConceptProperty.HEIGHT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_HEIGHT,
						entry.getValue().get(ConceptProperty.HEIGHT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_HEIGHT, "25");
			}

			if (entry.getValue().containsKey(ConceptProperty.FONT_STYLE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_STYLE, entry.getValue().get(ConceptProperty.FONT_STYLE.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_STYLE, "plain");
			}

			if (entry.getValue().containsKey(ConceptProperty.FONT_SIZE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_SIZE, entry.getValue().get(ConceptProperty.FONT_SIZE.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_SIZE, "12");
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_IMAGE.name())) {
				if (entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE.name()).toLowerCase().trim()
						.equals(CmapUtil.NO_IMAGE)) {
					// remove triples from previously used image (image was
					// removed, "none")
					List<Statement> statementsToRemove = model
							.listStatements(uiConcept, VizUtil.AFV_HAS_IMAGE, (RDFNode) null).toList();
					StmtIterator stmtIterator = model.listStatements(uiConcept, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
					while (stmtIterator.hasNext()) {
						Statement statement = stmtIterator.next();
						Resource imageResource = statement.getResource();
						statementsToRemove
								.addAll(model.listStatements(imageResource, (Property) null, (RDFNode) null).toList());
						if (stmtIterator.hasNext()) {
							log.error("Found multiple images for visualization concept: " + uiConcept.getURI());
						}
					}
					if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
						model.remove(statementsToRemove);
					}
				}

				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_IMAGE,
						VizUtil.AFV_IMAGE, VizUtil.AFV_IDENTIFIER,
						entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE.name()));

			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiConcept, VizUtil.AFV_HAS_IMAGE, (RDFNode) null).toList();
				StmtIterator stmtIterator = model.listStatements(uiConcept, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource imageResource = statement.getResource();
					statementsToRemove
							.addAll(model.listStatements(imageResource, (Property) null, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple images for visualization concept: " + uiConcept.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_IMAGE_STYLE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_IMAGE,
						VizUtil.AFV_IMAGE, VizUtil.AFV_STYLE,
						entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE_STYLE.name()));
			} else {
				List<Statement> statementsToRemove = new ArrayList<>();
				StmtIterator stmtIterator = model.listStatements(uiConcept, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource imageResource = statement.getResource();
					statementsToRemove
							.addAll(model.listStatements(imageResource, VizUtil.AFV_STYLE, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple images for visualization concept: " + uiConcept.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_IMAGE,
						VizUtil.AFV_IMAGE, VizUtil.AFV_LAYOUT,
						entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name()));
			} else {
				List<Statement> statementsToRemove = new ArrayList<>();
				StmtIterator stmtIterator = model.listStatements(uiConcept, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource imageResource = statement.getResource();
					statementsToRemove
							.addAll(model.listStatements(imageResource, VizUtil.AFV_LAYOUT, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple images for visualization concept: " + uiConcept.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.BORDER_SHAPE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_BORDER,
						VizUtil.AFV_BORDER, VizUtil.AFV_SHAPE,
						entry.getValue().get(ConceptProperty.BORDER_SHAPE.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_BORDER,
						VizUtil.AFV_BORDER, VizUtil.AFV_SHAPE, "rounded-rectangle");
			}

			if (entry.getValue().containsKey(ConceptProperty.BORDER_STYLE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_BORDER,
						VizUtil.AFV_BORDER, VizUtil.AFV_STYLE,
						entry.getValue().get(ConceptProperty.BORDER_STYLE.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_BORDER,
						VizUtil.AFV_BORDER, VizUtil.AFV_STYLE, "solid");
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_COLOR.name())) {
				if (RdfCmap.isAftColorScheme) {
					String bgColor = CmapUtil.determineBackgroundColor(model, uiConcept);
					model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_BACKGROUND_COLOR,
							bgColor);
				} else {
					model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_BACKGROUND_COLOR,
							entry.getValue().get(ConceptProperty.BACKGROUND_COLOR.name()));
				}
			} else {
				if (RdfCmap.isAftColorScheme) {
					String bgColor = CmapUtil.determineBackgroundColor(model, uiConcept);
					model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_BACKGROUND_COLOR,
							bgColor);
				} else {
					model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_BACKGROUND_COLOR,
							"237,244,246,255");
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.SHADOW_COLOR.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_SHADOW_COLOR,
						entry.getValue().get(ConceptProperty.SHADOW_COLOR.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_SHADOW_COLOR, "none");
			}

			x += 5;
			y += 5;
		}

		x = 55l;
		y = 10l;
		for (

		Entry<String, Map<String, String>> entry : fullLinkId2UiProperties.entrySet()) {
			Resource link;
			Resource uiLink;
			if (entry.getKey().contains(VizUtil.AFV_PREFIX)) {
				uiLink = model.createResource(entry.getKey());
			} else if (entry.getKey().contains(CmapUtil.URN_UUID)) {
				uiLink = model.createResource(entry.getKey().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			} else {
				// new link
				String title = entry.getValue().get(ConceptProperty.TITLE.name());
				Resource existingLinkWithSameVizId = findExistingUiResource(model, entry.getKey(), VizUtil.AFV_LINK);
				if (existingLinkWithSameVizId == null) {
					String id = UUID.randomUUID().toString();
					link = model.createResource(CmapUtil.URN_UUID + id);
					uiLink = model.createResource(VizUtil.AFV_PREFIX + id);
					model = CmapUtil.createOrUpdateRelatedResource(model, uiLink, AFOUtil.AFX_HAS_OBJECT, link);
					model = CmapUtil.createOrUpdateRelatedResource(model, link, AFOUtil.RDF_TYPE,
							AFOUtil.OWL_OBJECT_PROPERTY);
					model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_IDENTIFIER, entry.getKey());
					log.debug("Added new link with title: "
							+ ((title == null || title.isEmpty()) ? "<unknown>" : "\"" + title + "\"") + " new ID: "
							+ uiLink.getURI());
				} else {
					// reuse existing link
					uiLink = existingLinkWithSameVizId;
					link = model.listStatements(uiLink, AFOUtil.AFX_HAS_OBJECT, (RDFNode) null).next().getResource();
					String existingTitle = uiLink.getProperty(AFOUtil.DCT_TITLE).getString();
					log.debug("Reusing existing link <" + uiLink.getURI() + "> with title: " + "\"" + existingTitle
							+ "\" for new link with title "
							+ ((title == null || title.isEmpty()) ? "<unknown>" : "\"" + title + "\"")
							+ " and given ID: " + entry.getKey());
				}
			}

			if (!uiLink.hasProperty(AFOUtil.AFX_HAS_OBJECT)) {
				// this can happen if input file contains RDF UI links but not
				// RDF model
				log.debug("Found UI link without relation to RDF link. Adding required relations.");
				link = model.getResource(uiLink.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateRelatedResource(model, uiLink, AFOUtil.AFX_HAS_OBJECT, link);
				model = CmapUtil.createOrUpdateRelatedResource(model, link, AFOUtil.RDF_TYPE,
						AFOUtil.OWL_NAMED_INDIVIDUAL);
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_IDENTIFIER, entry.getKey());
			}

			resources.add(uiLink);

			model = CmapUtil.createOrUpdateRelatedResource(model, uiLink, AFOUtil.RDF_TYPE, VizUtil.AFV_LINK);
			model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, AFOUtil.DCT_IDENTIFIER, uiLink.getURI());
			model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, AFOUtil.DCT_TITLE,
					entry.getValue().get(ConceptProperty.TITLE.name()));

			if (!model.contains(uiLink, VizUtil.AFV_HAS_MAP, (RDFNode) null)) {
				Resource map = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP).next()
						.getSubject();
				uiLink.addProperty(VizUtil.AFV_HAS_MAP, map);
			}

			if (entry.getValue().containsKey(ConceptProperty.SHORT_COMMENT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_SHORT_COMMENT,
						entry.getValue().get(ConceptProperty.SHORT_COMMENT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_SHORT_COMMENT, "");
			}

			if (entry.getValue().containsKey(ConceptProperty.LONG_COMMENT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_LONG_COMMENT,
						entry.getValue().get(ConceptProperty.LONG_COMMENT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_LONG_COMMENT, "");
			}

			if (entry.getValue().containsKey(ConceptProperty.PARENT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_HAS_PARENT_ID,
						entry.getValue().get(ConceptProperty.PARENT.name()));
			} else {
				List<Statement> statementsToRemove = model
						.listStatements(uiLink, VizUtil.AFV_HAS_PARENT_ID, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
				statementsToRemove = model.listStatements(uiLink, VizUtil.AFV_HAS_PARENT, (RDFNode) null).toList();
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.X.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_X_POSITION,
						entry.getValue().get(ConceptProperty.X.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_X_POSITION, x.toString());
			}

			if (entry.getValue().containsKey(ConceptProperty.Y.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_Y_POSITION,
						entry.getValue().get(ConceptProperty.Y.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_Y_POSITION, y.toString());
			}

			if (entry.getValue().containsKey(ConceptProperty.WIDTH.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_WIDTH,
						entry.getValue().get(ConceptProperty.WIDTH.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_WIDTH, "100");
			}

			if (entry.getValue().containsKey(ConceptProperty.HEIGHT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_HEIGHT,
						entry.getValue().get(ConceptProperty.HEIGHT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_HEIGHT, "15");
			}

			if (entry.getValue().containsKey(ConceptProperty.MIN_WIDTH.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_MINIMUM_WIDTH,
						entry.getValue().get(ConceptProperty.MIN_WIDTH.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_MINIMUM_WIDTH, "2");
			}

			if (entry.getValue().containsKey(ConceptProperty.MIN_HEIGHT.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_MINIMUM_HEIGHT,
						entry.getValue().get(ConceptProperty.MIN_HEIGHT.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_MINIMUM_HEIGHT, "11");
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_COLOR.name())) {
				if (RdfCmap.isAftColorScheme) {
					String bgColor = CmapUtil.determineBackgroundColor(model, uiLink);
					model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_BACKGROUND_COLOR, bgColor);
				} else {
					model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_BACKGROUND_COLOR,
							entry.getValue().get(ConceptProperty.BACKGROUND_COLOR.name()));
				}
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_BACKGROUND_COLOR,
						"240,240,240,0");
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_IMAGE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_IMAGE,
						VizUtil.AFV_IMAGE, VizUtil.AFV_IDENTIFIER,
						entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE.name()));
			} else {
				List<Statement> statementsToRemove = model.listStatements(uiLink, VizUtil.AFV_HAS_IMAGE, (RDFNode) null)
						.toList();
				StmtIterator stmtIterator = model.listStatements(uiLink, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource imageResource = statement.getResource();
					statementsToRemove
							.addAll(model.listStatements(imageResource, (Property) null, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple images for visualization link: " + uiLink.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_IMAGE_STYLE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_IMAGE,
						VizUtil.AFV_IMAGE, VizUtil.AFV_STYLE,
						entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE_STYLE.name()));
			} else {
				List<Statement> statementsToRemove = new ArrayList<>();
				StmtIterator stmtIterator = model.listStatements(uiLink, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource imageResource = statement.getResource();
					statementsToRemove
							.addAll(model.listStatements(imageResource, VizUtil.AFV_STYLE, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple images for visualization link: " + uiLink.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_IMAGE,
						VizUtil.AFV_IMAGE, VizUtil.AFV_LAYOUT,
						entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE_LAYOUT.name()));
			} else {
				List<Statement> statementsToRemove = new ArrayList<>();
				StmtIterator stmtIterator = model.listStatements(uiLink, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource imageResource = statement.getResource();
					statementsToRemove
							.addAll(model.listStatements(imageResource, VizUtil.AFV_LAYOUT, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple images for visualization link: " + uiLink.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}
			}

			if (entry.getValue().containsKey(ConceptProperty.BORDER_COLOR.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_BORDER,
						VizUtil.AFV_BORDER, VizUtil.AFV_COLOR,
						entry.getValue().get(ConceptProperty.BORDER_COLOR.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_BORDER,
						VizUtil.AFV_BORDER, VizUtil.AFV_COLOR, "240,240,240,0");
			}

			if (entry.getValue().containsKey(ConceptProperty.FONT_SIZE.name())) {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_SIZE, entry.getValue().get(ConceptProperty.FONT_SIZE.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_SIZE, "9");
			}

			if (entry.getValue().containsKey(ConceptProperty.FONT_COLOR.name())) {
				if (RdfCmap.isAftColorScheme) {
					String fontColor = CmapUtil.determineFontColor(model, uiLink);
					model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_FONT,
							VizUtil.AFV_FONT, VizUtil.AFV_COLOR, fontColor);
				} else {
					model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_FONT,
							VizUtil.AFV_FONT, VizUtil.AFV_COLOR,
							entry.getValue().get(ConceptProperty.FONT_COLOR.name()));
				}

			} else {
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_FONT,
						VizUtil.AFV_FONT, VizUtil.AFV_COLOR, "0,0,0,255");
			}

			if (entry.getValue().containsKey(ConceptProperty.SHADOW_COLOR.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_SHADOW_COLOR,
						entry.getValue().get(ConceptProperty.SHADOW_COLOR.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiLink, VizUtil.AFV_SHADOW_COLOR, "none");
			}

			if (entry.getValue().containsKey(ConceptProperty.CARDINALITY.name())) {
				String cardinalityString = entry.getValue().get(ConceptProperty.CARDINALITY.name());
				Cardinality cardinality = CmapUtil.determineCardinality(cardinalityString);

				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_CARDINALITY,
						cardinality.getType(), AFOUtil.AFX_MINIMUM_VALUE, cardinality.getMinimumValue());
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_CARDINALITY,
						cardinality.getType(), AFOUtil.AFX_MAXIMUM_VALUE, cardinality.getMaximumValue());
			} else {
				List<Statement> statementsToRemove = new ArrayList<>();
				StmtIterator stmtIterator = model.listStatements(uiLink, VizUtil.AFV_HAS_CARDINALITY, (RDFNode) null);
				while (stmtIterator.hasNext()) {
					Statement statement = stmtIterator.next();
					Resource cardinalityResource = statement.getResource();
					statementsToRemove.addAll(
							model.listStatements(cardinalityResource, (Property) null, (RDFNode) null).toList());
					if (stmtIterator.hasNext()) {
						log.error("Found multiple cardinalities for visualization link: " + uiLink.getURI());
					}
				}
				if (statementsToRemove != null && !statementsToRemove.isEmpty()) {
					model.remove(statementsToRemove);
				}

				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_CARDINALITY,
						VizUtil.AFV_MIN_CARDINALITY, AFOUtil.AFX_MINIMUM_VALUE, "0");
				model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiLink, VizUtil.AFV_HAS_CARDINALITY,
						VizUtil.AFV_MIN_CARDINALITY, AFOUtil.AFX_MAXIMUM_VALUE, "");
			}

			x += 15;
			y += 15;
		}

		for (Entry<String, Map<String, String>> entry : connectionId2UiProperties.entrySet()) {
			Resource connection;
			Resource uiConnection;
			if (entry.getKey().contains(VizUtil.AFV_PREFIX)) {
				uiConnection = model.createResource(entry.getKey());
			} else if (entry.getKey().contains(CmapUtil.URN_UUID)) {
				uiConnection = model.createResource(entry.getKey().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			} else {
				// new connection
				Resource existingConnectionWithSameVizId = findExistingUiResource(model, entry.getKey(),
						VizUtil.AFV_CONNECTION);
				if (existingConnectionWithSameVizId == null) {
					String id = UUID.randomUUID().toString();
					connection = model.createResource(CmapUtil.URN_UUID + id);
					uiConnection = model.createResource(VizUtil.AFV_PREFIX + id);
					model = CmapUtil.createOrUpdateRelatedResource(model, uiConnection, AFOUtil.AFX_HAS_OBJECT,
							connection);
					model = CmapUtil.createOrUpdateRelatedResource(model, connection, AFOUtil.RDF_TYPE,
							AFOUtil.OWL_OBJECT_PROPERTY);
					model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_IDENTIFIER,
							entry.getKey());
					log.debug("Added new connection for new ID: " + id);
				} else {
					// reuse existing connection
					uiConnection = existingConnectionWithSameVizId;
					connection = model.listStatements(uiConnection, AFOUtil.AFX_HAS_OBJECT, (RDFNode) null).next()
							.getResource();
					String existingTitle = connection.getProperty(AFOUtil.DCT_TITLE).getString();
					log.debug("Reusing existing connection <" + uiConnection.getURI() + "> with title: " + "\""
							+ existingTitle + "\" for new connection and given ID: " + entry.getKey());
				}
			}

			if (!uiConnection.hasProperty(AFOUtil.AFX_HAS_OBJECT)) {
				// this can happen if input file contains RDF UI links but not
				// RDF model
				log.debug("Found UI connection without relation to RDF connection. Adding required relations.");
				connection = model.getResource(uiConnection.getURI().replace(VizUtil.AFV_PREFIX, CmapUtil.URN_UUID));
				model = CmapUtil.createOrUpdateRelatedResource(model, uiConnection, AFOUtil.AFX_HAS_OBJECT, connection);
				model = CmapUtil.createOrUpdateRelatedResource(model, connection, AFOUtil.RDF_TYPE,
						AFOUtil.OWL_NAMED_INDIVIDUAL);
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_IDENTIFIER,
						entry.getKey());
			}

			resources.add(uiConnection);

			model = CmapUtil.createOrUpdateRelatedResource(model, uiConnection, AFOUtil.RDF_TYPE,
					VizUtil.AFV_CONNECTION);
			model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, AFOUtil.DCT_IDENTIFIER,
					uiConnection.getURI());

			if (!model.contains(uiConnection, VizUtil.AFV_HAS_MAP, (RDFNode) null)) {
				Resource map = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP).next()
						.getSubject();
				uiConnection.addProperty(VizUtil.AFV_HAS_MAP, map);
			}

			Resource from = model.getResource(entry.getValue().get(ConceptProperty.CONNECTS_FROM.name()));

			Resource to = model.getResource(entry.getValue().get(ConceptProperty.CONNECTS_TO.name()));

			model = CmapUtil.createOrUpdateRelatedResource(model, uiConnection, VizUtil.AFV_CONNECTS_FROM, from);
			model = CmapUtil.createOrUpdateRelatedResource(model, uiConnection, VizUtil.AFV_CONNECTS_TO, to);

			if (entry.getValue().containsKey(ConceptProperty.ANCHOR_FROM.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_ANCHOR_FROM,
						entry.getValue().get(ConceptProperty.ANCHOR_FROM.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_ANCHOR_FROM, "center");
			}

			if (entry.getValue().containsKey(ConceptProperty.ANCHOR_TO.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_ANCHOR_TO,
						entry.getValue().get(ConceptProperty.ANCHOR_TO.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_ANCHOR_TO, "center");
			}

			if (entry.getValue().containsKey(ConceptProperty.LINE_TYPE.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_LINE_TYPE,
						entry.getValue().get(ConceptProperty.LINE_TYPE.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_LINE_TYPE, "straight");
			}

			if (entry.getValue().containsKey(ConceptProperty.ARROW_HEAD.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_ARROW_HEAD,
						entry.getValue().get(ConceptProperty.ARROW_HEAD.name()));
			} else {
				model = CmapUtil.createOrUpdateLiteralValue(model, uiConnection, VizUtil.AFV_ARROW_HEAD,
						"if-to-concept");
			}
		}

		removeAllControlPoints(model);

		for (Entry<String, Map<String, String>> entry : controlPointId2UiProperties.entrySet()) {
			Resource controlPoint;
			if (entry.getKey().contains(VizUtil.AFV_PREFIX)) {
				controlPoint = model.createResource(entry.getKey());
			} else if (entry.getKey().contains(CmapUtil.URN_UUID)) {
				controlPoint = model.createResource(entry.getKey().replace(CmapUtil.URN_UUID, VizUtil.AFV_PREFIX));
			} else {
				throw new IllegalStateException("Unhandled control point ID: " + entry.getKey());
			}

			model = CmapUtil.createOrUpdateRelatedResource(model, controlPoint, AFOUtil.RDF_TYPE, VizUtil.AFV_POINT);
			model = CmapUtil.createOrUpdateLiteralValue(model, controlPoint, AFOUtil.DCT_IDENTIFIER, entry.getKey());

			String parentConnectionId = entry.getValue().get(ConceptProperty.CONNECTION_ID.name());
			Resource connection = null;
			if (parentConnectionId.contains(VizUtil.AFV_PREFIX)) {
				// point already assigned to connection in RDF model
				connection = model.getResource(parentConnectionId);
			} else {
				// new point needs to be assigned to parent connection via
				// visualization id
				StmtIterator connectionIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE,
						VizUtil.AFV_CONNECTION);
				while (connectionIterator.hasNext()) {
					Statement statement = connectionIterator.next();
					String connectionId = statement.getSubject().getProperty(VizUtil.AFV_IDENTIFIER).getString();
					if (parentConnectionId.equals(connectionId)) {
						connection = statement.getSubject();
						break;
					}
				}
			}

			if (!connection.hasProperty(AFOUtil.DCT_IDENTIFIER)) {
				throw new IllegalStateException(
						"Control Point with id: " + entry.getKey() + " has no parent connection specified.");
			}

			model = CmapUtil.createOrUpdateRelatedResource(model, controlPoint, VizUtil.AFV_HAS_CONNECTION, connection);
			model = CmapUtil.createOrUpdateRelatedResource(model, connection, VizUtil.AFV_HAS_CONTROL_POINT,
					controlPoint);

			if (entry.getValue().containsKey(ConceptProperty.X.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, controlPoint, VizUtil.AFV_X_POSITION,
						entry.getValue().get(ConceptProperty.X.name()));
			} else {
				System.out
						.println("Control Point with id: " + entry.getKey() + " has no X specified. Set to default 0.");
				model = CmapUtil.createOrUpdateLiteralValue(model, connection, VizUtil.AFV_X_POSITION, "0");
			}

			if (entry.getValue().containsKey(ConceptProperty.Y.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, controlPoint, VizUtil.AFV_Y_POSITION,
						entry.getValue().get(ConceptProperty.Y.name()));
			} else {
				System.out
						.println("Control Point with id: " + entry.getKey() + " has no Y specified. Set to default 0.");
				model = CmapUtil.createOrUpdateLiteralValue(model, connection, VizUtil.AFV_Y_POSITION, "0");
			}

			if (entry.getValue().containsKey(ConceptProperty.INDEX.name())) {
				model = CmapUtil.createOrUpdateLiteralValue(model, controlPoint, AFOUtil.AFX_INDEX,
						entry.getValue().get(ConceptProperty.INDEX.name()));
			} else {
				System.out.println(
						"Control Point with id: " + entry.getKey() + " has no index specified. Set to last position.");
				Integer index = model.listStatements(connection, VizUtil.AFV_HAS_CONTROL_POINT, (RDFNode) null).toList()
						.size();
				model = CmapUtil.createOrUpdateLiteralValue(model, controlPoint, AFOUtil.AFX_INDEX, index.toString());
			}
		}

		for (Entry<String, Map<String, String>> entry : imageId2UiProperties.entrySet()) {
			String imageId = entry.getKey();

			String originalId = entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE.name());
			String bytes = entry.getValue().get(ConceptProperty.BACKGROUND_IMAGE_BYTES.name());

			Resource imageResource = null;
			StmtIterator stmtIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
			while (stmtIterator.hasNext()) {
				Statement statement = stmtIterator.next();
				if (!statement.getResource().hasProperty(VizUtil.AFV_IDENTIFIER)) {
					continue;
				}

				String vizId = statement.getResource().getProperty(VizUtil.AFV_IDENTIFIER).getString();
				if (imageId.equals(vizId)) {
					imageResource = statement.getResource();
					break;
				}
			}
			if (imageResource == null) {
				log.error("Found image without any related visualization concepts or links. Image ID: " + imageId);
				continue;
			}

			model = CmapUtil.createOrUpdateLiteralValue(model, imageResource, VizUtil.AFV_BYTES, bytes);

			if (originalId != null && !originalId.isEmpty()) {
				model = CmapUtil.createOrUpdateLiteralValue(model, imageResource, VizUtil.AFV_IDENTIFIER, originalId);
			}

		}

		model = replaceVisualizationIdentifiersForIris(model);

		model = determineClassHierarchyLevel(model);

		return new VisualizationInfoBuilderResult(model, resources);
	}

	private static Model determineClassHierarchyLevel(Model model) {
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT);
		while (stmtIterator.hasNext()) {
			Statement statement = stmtIterator.next();
			Resource instance = model.listStatements(statement.getSubject(), AFOUtil.AFX_HAS_OBJECT, (RDFNode) null)
					.next().getResource();
			List<Resource> classes = new ArrayList<>();
			if (instance.getURI().matches(CmapUtil.URN_UUID + AFOUtil.AFO_ID_PATTERN)) {
				// this is the case when urn:uuid: is added to class names in
				// order to visualize the taxonomy with cmap
				// classes are disguised as instances in order to apply rdfcmap
				// for visualization
				classes.add(instance);
			} else {
				StmtIterator instanceIterator = model.listStatements(instance, AFOUtil.RDF_TYPE, (RDFNode) null);
				while (instanceIterator.hasNext()) {
					Statement instanceStatement = instanceIterator.next();
					classes.add(instanceStatement.getResource());
				}
			}

			if (!belongsToAfoClassHierarchy(classes)) {
				model = CmapUtil.createOrUpdateLiteralValue(model, statement.getSubject(),
						VizUtil.AFV_CLASS_HIERARCHY_LEVEL, "-1");
				log.debug("Level -1 " + statement.getSubject().getURI());
				continue;
			}

			for (Iterator<Resource> iterator = classes.iterator(); iterator.hasNext();) {
				Resource resource = iterator.next();
				if (!RdfUtil.isAFTNamespace(resource.getNameSpace())
						&& !resource.getURI().matches(CmapUtil.URN_UUID + AFOUtil.AFO_ID_PATTERN)) {
					continue;
				}

				Set<Resource> parents = new HashSet<>();
				parents = identifyParents(model, parents, resource);

				model = CmapUtil.createOrUpdateLiteralValue(model, statement.getSubject(),
						VizUtil.AFV_CLASS_HIERARCHY_LEVEL, String.valueOf(parents.size()));
				log.debug("Level " + parents.size() + " " + statement.getSubject().getURI());
			}
		}

		return model;
	}

	private static Model adjustLabelSize(Model model, Resource uiConcept) {
		if (!model.listStatements(uiConcept, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null).hasNext()) {
			return model;
		}

		int level = Integer.valueOf((model.listStatements(uiConcept, VizUtil.AFV_CLASS_HIERARCHY_LEVEL, (RDFNode) null)
				.next().getString()));
		if (level < 0) {
			return model;
		}

		if (level == 0) {
			level = 1; // avoid division by 0
		}

		String width = String.valueOf(Math.min(Math.max(Math.round(10000 / level), 100), 2000));
		String height = String.valueOf(Math.min(Math.max(Math.round(2500 / level), 25), 500));

		// String fontSize = String.valueOf(Math.min(Math.max(Math.round(500 /
		// level), 12), 200));
		String fontSize = "12";
		switch (level) {
		case 0:
		case 1:
		case 2:
			fontSize = "800";
			break;
		case 3:
			fontSize = "600";
			break;
		case 4:
			fontSize = "500";
			break;
		case 5:
			fontSize = "300";
			break;
		case 6:
			fontSize = "200";
			break;
		case 7:
		case 8:
		case 9:
			fontSize = "100";
			break;
		case 10:
			fontSize = "80";
			break;
		case 11:
			fontSize = "50";
			break;
		case 12:
			fontSize = "25";
			break;
		default:
			fontSize = "12";
		}

		model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_WIDTH, width);
		model = CmapUtil.createOrUpdateLiteralValue(model, uiConcept, VizUtil.AFV_HEIGHT, height);
		model = CmapUtil.createOrUpdateLiteralValueOfRelatedResource(model, uiConcept, VizUtil.AFV_HAS_FONT,
				VizUtil.AFV_FONT, VizUtil.AFV_SIZE, fontSize);

		return model;
	}

	private static Set<Resource> identifyParents(Model model, Set<Resource> parents, Resource resource) {
		StmtIterator parentsIterator = model.listStatements(resource, AFOUtil.RDFS_SUBCLASS_OF, (RDFNode) null);
		while (parentsIterator.hasNext()) {
			Statement parentStatement = parentsIterator.next();
			Resource parent = parentStatement.getResource();
			if (parent.isAnon()) {
				continue;
			}
			if ((RdfUtil.isAFTNamespace(parent.getNameSpace())
					|| parent.getURI().matches(CmapUtil.URN_UUID + AFOUtil.AFO_ID_PATTERN))
					&& !parents.contains(parent)) {
				parents.add(parent);
				parents = identifyParents(model, parents, parent);
			}
		}
		return parents;
	}

	private static boolean belongsToAfoClassHierarchy(List<Resource> classes) {
		for (Iterator<Resource> iterator = classes.iterator(); iterator.hasNext();) {
			Resource resource = iterator.next();
			if (RdfUtil.isAFTNamespace(resource.getNameSpace())
					|| (resource.getURI().matches(CmapUtil.URN_UUID + AFOUtil.AFO_ID_PATTERN))) {
				return true;
			}
		}

		return false;
	}

	private static Resource findExistingUiResource(Model model, String key, Resource rdfType) {
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, rdfType);
		while (stmtIterator.hasNext()) {
			Statement statement = stmtIterator.next();

			if (!statement.getSubject().isURIResource()) {
				continue;
			}

			Resource subject = statement.getSubject();

			String id = model.listStatements(subject, VizUtil.AFV_IDENTIFIER, (RDFNode) null).next().getString();
			if (id != null && !id.isEmpty() && id.equals(key)) {
				return subject;
			}
		}

		return null;
	}

	private static Model replaceVisualizationIdentifiersForIris(Model model) {
		List<List<Statement>> statementsToReplace = new ArrayList<>();

		StmtIterator stmtIterator = model.listStatements();
		while (stmtIterator.hasNext()) {
			Statement statement = stmtIterator.next();
			Resource subject = statement.getSubject();

			if (!subject.toString().startsWith(CmapUtil.URN_UUID)
					&& !subject.toString().startsWith(VizUtil.AFV_PREFIX)) {
				continue;
			}

			if (!subject.hasProperty(VizUtil.AFV_IDENTIFIER)) {
				continue;
			}

			String vizId = subject.getProperty(VizUtil.AFV_IDENTIFIER).getLiteral().getLexicalForm();

			if (vizId.startsWith(CmapUtil.URN_UUID) || vizId.startsWith(VizUtil.AFV_PREFIX)) {
				continue;
			}

			StmtIterator replaceStmtIterator = model.listStatements((Resource) null, (Property) null,
					ResourceFactory.createResource(vizId));
			while (replaceStmtIterator.hasNext()) {
				Statement statementToReplace = replaceStmtIterator.next();

				if (statementToReplace.getPredicate().equals(VizUtil.AFV_IDENTIFIER)) {
					continue;
				}

				List<Statement> pairOfStatementsToReplace = new ArrayList<>(2);
				pairOfStatementsToReplace.add(statementToReplace);
				pairOfStatementsToReplace.add(ResourceFactory.createStatement(statementToReplace.getSubject(),
						statementToReplace.getPredicate(), subject));
				statementsToReplace.add(pairOfStatementsToReplace);
			}
		}

		for (Iterator<List<Statement>> iterator = statementsToReplace.iterator(); iterator.hasNext();) {
			List<Statement> statements = iterator.next();
			model.remove(statements.get(0));
			model.add(statements.get(1));
		}

		// replace parent Ids of nested nodes
		List<Statement> statementsToRemove = new ArrayList<>();
		List<Statement> statementsToAdd = new ArrayList<>();
		StmtIterator parentStmtIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_PARENT_ID,
				(RDFNode) null);
		while (parentStmtIterator.hasNext()) {
			Statement parentStatement = parentStmtIterator.next();
			statementsToRemove.add(parentStatement);
			statementsToRemove
					.addAll(model.listStatements((Resource) null, VizUtil.AFV_HAS_PARENT, (RDFNode) null).toList());

			String parentId = parentStatement.getObject().asLiteral().getString();
			Resource parent = null;
			if (parentId.contains(VizUtil.AFV_PREFIX) || parentId.contains(CmapUtil.URN_UUID)) {
				parent = model.getResource(parentStatement.getObject().asLiteral().getString());
			} else {
				parent = model.listStatements((Resource) null, VizUtil.AFV_IDENTIFIER,
						ResourceFactory.createPlainLiteral(parentId)).next().getSubject();
			}
			statementsToAdd
					.add(ResourceFactory.createStatement(parentStatement.getSubject(), VizUtil.AFV_HAS_PARENT, parent));
		}

		if (!statementsToRemove.isEmpty()) {
			model.remove(statementsToRemove);
			model.add(statementsToAdd);
		}
		return model;
	}

	/**
	 * Control points do not have identifiers in XML, so it is not easily
	 * possible to attach information to a list of control points. Pragmatic
	 * solution: delete all control points, create new control points.
	 *
	 * @param model
	 */
	private static void removeAllControlPoints(Model model) {
		List<Statement> statementsToRemove = new ArrayList<>();
		StmtIterator stmtIterator = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_POINT);
		while (stmtIterator.hasNext()) {
			Statement statement = stmtIterator.next();
			statementsToRemove.add(statement);
			Resource subject = statement.getSubject();
			StmtIterator subjectStatementIterator = model.listStatements(subject, (Property) null, (RDFNode) null);
			while (subjectStatementIterator.hasNext()) {
				Statement statement2 = subjectStatementIterator.next();
				statementsToRemove.add(statement2);
			}

			StmtIterator objectStatementIterator = model.listStatements((Resource) null, (Property) null, subject);
			while (objectStatementIterator.hasNext()) {
				Statement statement2 = objectStatementIterator.next();
				statementsToRemove.add(statement2);
			}
		}

		model.remove(statementsToRemove);
	}
}
