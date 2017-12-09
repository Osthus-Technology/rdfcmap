package com.osthus.rdfcmap.cmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.NumericEntityEscaper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.osthus.rdfcmap.RdfCmap;
import com.osthus.rdfcmap.enums.ConceptProperty;
import com.osthus.rdfcmap.enums.MapProperty;
import com.osthus.rdfcmap.enums.PropertyEnums;
import com.osthus.rdfcmap.helper.Point;
import com.osthus.rdfcmap.util.AFOUtil;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.VizUtil;

/**
 * CxlWriter
 * 
 * Write cxl output, intentionally not applying an xml library because of somewhat proprietary setup of cmap cxl.
 * 
 * @author Helge Krieg, OSTHUS GmbH
 */
public class CxlWriter
{
	private static final Logger log = LogManager.getLogger("Logger");

	public static void write(Path path, Map<String, Map<String, String>> conceptId2UiProperties, Map<String, Map<String, String>> fullLinkId2UiProperties,
			Map<String, List<String>> connectionId2LinkAndConcept) throws IOException
	{
		String inputFileName = path.getFileName().toString();
		String outputFileName = inputFileName.substring(0, inputFileName.length() - 3) + "cxl";
		Path cxlPath = Paths.get("src/main/resources/" + outputFileName);
		Files.deleteIfExists(cxlPath);
		cxlPath = Files.createFile(cxlPath);
		log.info("writing to: " + cxlPath.toString());
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<cmap xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns=\"http://cmap.ihmc.us/xml/cmap/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:vcard=\"http://www.w3.org/2001/vcard-rdf/3.0#\">\r\n"
				+ "    <res-meta>\r\n" + "        <dc:title>2016-10-20 HK excerpt of MS model</dc:title>\r\n" + "        <dc:creator>\r\n"
				+ "            <vcard:FN>HK</vcard:FN>\r\n" + "        </dc:creator>\r\n"
				+ "        <dcterms:created>2016-10-20T21:25:41+02:00</dcterms:created>\r\n"
				+ "        <dcterms:modified>2016-10-20T21:25:41+02:00</dcterms:modified>\r\n" + "        <dc:language>en</dc:language>\r\n"
				+ "        <dc:format>x-cmap/x-storable</dc:format>\r\n" + "        <dc:publisher>FIHMC CmapTools 6.02</dc:publisher>\r\n"
				+ "        <dc:extent>21064 bytes</dc:extent>\r\n"
				+ "        <dc:source>cmap:1QP5DZZTS-1NQ9MJ-1:1QP6GRX6V-1KDCWT3-TR:1QP6GRX7B-10MGM0B-ZF</dc:source>\r\n" + "    </res-meta>\r\n"
				+ "    <map width=\"6000\" height=\"6000\">\r\n");
		sb.append("    <concept-list>\r\n");
		for (Entry<String, Map<String, String>> entry : conceptId2UiProperties.entrySet())
		{
			sb.append(" <concept id=\"" + entry.getKey() + "\" label=\"" + entry.getValue().get(ConceptProperty.TITLE.name()) + "\" short-comment=\""
					+ entry.getValue().get(ConceptProperty.SHORT_COMMENT.name()) + "\" long-comment=\""
					+ entry.getValue().get(ConceptProperty.LONG_COMMENT.name()) + "\"/>\r\n");
		}
		sb.append("    </concept-list>\r\n");
		sb.append("    <concept-appearance-list>\r\n");
		Integer x = 10;
		Integer y = 10;
		for (Entry<String, Map<String, String>> entry : conceptId2UiProperties.entrySet())
		{
			sb.append("        <concept-appearance id=\"" + entry.getKey() + "\" x=\"" + x + "\" y=\"" + y
					+ "\" width=\"100\" height=\"25\" font-style=\"plain\"/>\r\n");
			x += 10;
			y += 10;
		}
		sb.append("    </concept-appearance-list>\r\n");
		sb.append("    <linking-phrase-list>\r\n");
		for (Entry<String, Map<String, String>> entry : fullLinkId2UiProperties.entrySet())
		{
			sb.append("        <linking-phrase id=\"" + entry.getKey() + "\" label=\"" + entry.getValue().get(ConceptProperty.TITLE.name())
					+ "\" short-comment=\"" + entry.getValue().get(ConceptProperty.SHORT_COMMENT.name()) + "\" long-comment=\""
					+ entry.getValue().get(ConceptProperty.LONG_COMMENT.name()) + "\"/>\r\n");
		}
		sb.append("    </linking-phrase-list>\r\n");
		sb.append("    <linking-phrase-appearance-list>\r\n");
		x = 15;
		y = 15;
		for (Entry<String, Map<String, String>> entry : fullLinkId2UiProperties.entrySet())
		{
			sb.append("        <linking-phrase-appearance id=\"" + entry.getKey() + "\" x=\"" + x + "\" y=\"" + y
					+ "\" width=\"100\" height=\"11\" min-width=\"2\" min-height=\"11\"/>\r\n");
		}
		sb.append("    </linking-phrase-appearance-list>\r\n");
		sb.append("    <connection-list>\r\n");
		for (Entry<String, List<String>> entry : connectionId2LinkAndConcept.entrySet())
		{
			sb.append("        <connection id=\"" + entry.getKey() + "\" from-id=\"" + entry.getValue().get(0) + "\" to-id=\"" + entry.getValue().get(1)
					+ "\"/>\r\n");
		}
		sb.append("    </connection-list>\r\n");
		sb.append("    <connection-appearance-list>\r\n");
		for (Entry<String, List<String>> entry : connectionId2LinkAndConcept.entrySet())
		{
			sb.append("        <connection-appearance id=\"" + entry.getKey()
					+ "\" from-pos=\"center\" to-pos=\"center\" type=\"straight\" arrowhead=\"no\"/>\r\n");
		}
		sb.append("    </connection-appearance-list>\r\n");
		sb.append("    <style-sheet-list>\r\n" + "        <style-sheet id=\"_Default_\">\r\n"
				+ "                <map-style background-color=\"255,255,255,0\" image-style=\"full\" image-top-left=\"0,0\"/>\r\n"
				+ "                <concept-style font-name=\"Verdana\" font-size=\"12\" font-style=\"plain\" font-color=\"0,0,0,255\" text-margin=\"4\" background-color=\"237,244,246,255\" background-image-style=\"full\" border-color=\"0,0,0,255\" border-style=\"solid\" border-thickness=\"1\"\r\n"
				+ "                    border-shape=\"rounded-rectangle\" border-shape-rrarc=\"15.0\" text-alignment=\"center\" shadow-color=\"none\" min-width=\"-1\" min-height=\"-1\" max-width=\"-1.0\" group-child-spacing=\"10\" group-parent-spacing=\"10\"/>\r\n"
				+ "                <linking-phrase-style font-name=\"Verdana\" font-size=\"12\" font-style=\"plain\" font-color=\"0,0,0,255\" text-margin=\"1\" background-color=\"0,0,255,0\" background-image-style=\"full\" border-color=\"0,0,0,0\" border-style=\"solid\" border-thickness=\"1\"\r\n"
				+ "                    border-shape=\"rectangle\" border-shape-rrarc=\"15.0\" text-alignment=\"center\" shadow-color=\"none\" min-width=\"-1\" min-height=\"-1\" max-width=\"-1.0\" group-child-spacing=\"10\" group-parent-spacing=\"10\"/>\r\n"
				+ "                <connection-style color=\"0,0,0,255\" style=\"solid\" thickness=\"1\" type=\"straight\" arrowhead=\"if-to-concept-and-slopes-up\"/>\r\n"
				+ "                <resource-style font-name=\"SanSerif\" font-size=\"12\" font-style=\"plain\" font-color=\"0,0,0,255\" background-color=\"192,192,192,255\"/>\r\n"
				+ "            </style-sheet>\r\n" + "            <style-sheet id=\"_LatestChanges_\">\r\n"
				+ "                <concept-style font-style=\"plain\"/>\r\n" + "                <connection-style arrowhead=\"no\"/>\r\n"
				+ "            </style-sheet>\r\n" + "        </style-sheet-list>\r\n" + "        <extra-graphical-properties-list>\r\n"
				+ "            <properties-list id=\"1QP6GRX7B-10MGM0B-ZF\">\r\n"
				+ "                <property key=\"StyleSheetGroup_0\" value=\"//*@!#$%%^&amp;*()() No Grouped StyleSheets @\"/>\r\n"
				+ "            </properties-list>\r\n" + "        </extra-graphical-properties-list>\r\n" + "    </map>\r\n" + "</cmap>");
		Files.write(cxlPath, sb.toString().getBytes());
	}

	@SuppressWarnings("deprecation")
	public static void generateCxlFromRdfModel(Path path, Model model) throws IOException
	{
		String inputFileName = path.getFileName().toString();
		String outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "_new.cxl";
		Path cxlPath = Paths.get(outputFileName);
		Files.deleteIfExists(cxlPath);
		cxlPath = Files.createFile(cxlPath);

		int numberOfMaps = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP).toList().size();
		if (numberOfMaps <= 0 || numberOfMaps > 1)
		{
			throw new IllegalStateException("There must be exactly 1 map for visualization, but found number of maps: " + numberOfMaps);
		}
		Resource map = model.listStatements((Resource) null, AFOUtil.RDF_TYPE, VizUtil.AFV_MAP).next().getSubject();

		// create header with values from RDF model
		String header = VizUtil.HEADER;
		List<String> mapProperties = Arrays.asList(MapProperty.THE_TITLE.name(), MapProperty.CREATED_DATE.name(), MapProperty.MODIFIED_DATE.name(),
				MapProperty.PUBLISHER.name(), MapProperty.MAP_WIDTH.name(), MapProperty.MAP_HEIGHT.name());

		for (Iterator<String> iterator = mapProperties.iterator(); iterator.hasNext();)
		{
			String mapProperty = iterator.next();
			String value = extractAsString(map, PropertyEnums.enum2Property.get(mapProperty));
			header = header.replace(mapProperty, value);
		}

		StringBuilder sb = new StringBuilder();

		sb.append(header);

		sb.append("    <concept-list>\r\n");
		StmtIterator statementIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_MAP, map);
		while (statementIterator.hasNext())
		{
			Statement statement = statementIterator.next();
			Resource concept = statement.getSubject();
			if (!concept.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT))
			{
				continue;
			}

			String id = concept.getProperty(AFOUtil.DCT_IDENTIFIER).getString();

			String title = StringUtils.EMPTY;
			if (concept.getProperty(AFOUtil.DCT_TITLE) != null)
			{
				title = concept.getProperty(AFOUtil.DCT_TITLE).getString();
				title = title.replaceAll("\\\\\"", "\"");
				title = title.replaceAll("\"", "&quot;");
				// title = NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(title));
			}

			if (title.isEmpty())
			{
				title = "no title";
			}

			title = title.replaceAll("<", "").replaceAll(">", "");

			String parentId = StringUtils.EMPTY;
			if (concept.getProperty(VizUtil.AFV_HAS_PARENT) != null)
			{
				parentId = concept.getProperty(VizUtil.AFV_HAS_PARENT).getResource().getURI();
			}

			String shortComment = concept.getProperty(VizUtil.AFV_SHORT_COMMENT).getString();
			String longComment = concept.getProperty(VizUtil.AFV_LONG_COMMENT).getString();
			if (parentId != null && !parentId.isEmpty())
			{
				sb.append("        <concept id=\"" + id + "\" label=\"" + title + "\" parent-id=\"" + parentId + "\" short-comment=\"" + shortComment
						+ "\" long-comment=\"" + longComment + "\"/>\r\n");
			}
			else
			{
				sb.append("        <concept id=\"" + id + "\" label=\"" + title + "\" short-comment=\"" + shortComment + "\" long-comment=\"" + longComment
						+ "\"/>\r\n");
			}
		}
		sb.append("    </concept-list>\r\n");

		sb.append("    <concept-appearance-list>\r\n");
		statementIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_MAP, map);
		while (statementIterator.hasNext())
		{
			Statement statement = statementIterator.next();
			Resource concept = statement.getSubject();
			if (!concept.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONCEPT))
			{
				continue;
			}

			String id = concept.getProperty(AFOUtil.DCT_IDENTIFIER).getString();
			String x = concept.getProperty(VizUtil.AFV_X_POSITION).getString();
			String y = concept.getProperty(VizUtil.AFV_Y_POSITION).getString();
			String width = concept.getProperty(VizUtil.AFV_WIDTH).getString();
			String height = concept.getProperty(VizUtil.AFV_HEIGHT).getString();

			String expanded = StringUtils.EMPTY;
			if (concept.getProperty(VizUtil.AFV_EXPANDED) != null)
			{
				expanded = concept.getProperty(VizUtil.AFV_EXPANDED).getString();
			}

			if (expanded != null && !expanded.isEmpty())
			{
				sb.append("        <concept-appearance id=\"" + id + "\" x=\"" + x + "\" y=\"" + y + "\" expanded=\"" + expanded + "\" width=\"" + width
						+ "\" height=\"" + height);
			}
			else
			{
				sb.append("        <concept-appearance id=\"" + id + "\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + width + "\" height=\"" + height);
			}

			String fontStyle = StringUtils.EMPTY;
			if (concept.hasProperty(VizUtil.AFV_HAS_FONT))
			{
				Resource conceptFont = concept.getProperty(VizUtil.AFV_HAS_FONT).getResource();
				fontStyle = conceptFont.getProperty(VizUtil.AFV_STYLE).getString();
			}

			if (!fontStyle.isEmpty())
			{
				sb.append("\" font-style=\"" + fontStyle);
			}

			String fontSize = StringUtils.EMPTY;
			if (concept.hasProperty(VizUtil.AFV_HAS_FONT))
			{
				Resource conceptFont = concept.getProperty(VizUtil.AFV_HAS_FONT).getResource();
				fontSize = conceptFont.getProperty(VizUtil.AFV_SIZE).getString();
			}

			if (!fontSize.isEmpty())
			{
				sb.append("\" font-size=\"" + fontSize);
			}

			String backgroundColor = StringUtils.EMPTY;
			if (concept.hasProperty(VizUtil.AFV_BACKGROUND_COLOR))
			{
				backgroundColor = concept.getProperty(VizUtil.AFV_BACKGROUND_COLOR).getString();
			}

			if (!backgroundColor.isEmpty())
			{
				sb.append("\" background-color=\"" + backgroundColor);
			}

			String backgroundImage = StringUtils.EMPTY;
			if (concept.hasProperty(VizUtil.AFV_HAS_IMAGE))
			{
				Resource imageResource = concept.getProperty(VizUtil.AFV_HAS_IMAGE).getResource();
				backgroundImage = imageResource.getURI();
				if (imageResource.hasProperty(VizUtil.AFV_IDENTIFIER)
						&& imageResource.getProperty(VizUtil.AFV_IDENTIFIER).getString().toLowerCase().trim().equals(CmapUtil.NO_IMAGE))
				{
					backgroundImage = CmapUtil.NO_IMAGE;
				}
			}
			if (!backgroundColor.isEmpty())
			{
				sb.append("\" background-image=\"" + backgroundImage);
			}

			String backgroundImageStyle = StringUtils.EMPTY;
			if (concept.hasProperty(VizUtil.AFV_HAS_IMAGE) && concept.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_STYLE))
			{
				backgroundImageStyle = concept.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_STYLE).getString();
			}
			if (!backgroundImageStyle.isEmpty())
			{
				sb.append("\" background-image-style=\"" + backgroundImageStyle);
			}

			String backgroundImageLayout = StringUtils.EMPTY;
			if (concept.hasProperty(VizUtil.AFV_HAS_IMAGE) && concept.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_LAYOUT))
			{
				backgroundImageLayout = concept.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_LAYOUT).getString();
			}
			if (!backgroundImageLayout.isEmpty())
			{
				sb.append("\" background-image-layout=\"" + backgroundImageLayout);
			}

			sb.append("\"/>\r\n");
		}
		sb.append("    </concept-appearance-list>\r\n");

		sb.append("    <linking-phrase-list>\r\n");
		statementIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_MAP, map);
		while (statementIterator.hasNext())
		{
			Statement statement = statementIterator.next();
			Resource link = statement.getSubject();
			if (!link.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_LINK))
			{
				continue;
			}

			String id = link.getProperty(AFOUtil.DCT_IDENTIFIER).getString();

			String title = StringUtils.EMPTY;
			if (link.getProperty(AFOUtil.DCT_TITLE) != null)
			{
				title = link.getProperty(AFOUtil.DCT_TITLE).getString();
				title = title.replaceAll("\\\"", "\"");
				title = title.replaceAll("\"", "&quot;");
				title = CmapUtil.addCardinality(model, link, title);
				// title = NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(title));
			}

			String parentId = StringUtils.EMPTY;
			if (link.getProperty(VizUtil.AFV_HAS_PARENT) != null)
			{
				parentId = link.getProperty(VizUtil.AFV_HAS_PARENT).getResource().getURI();
			}

			String shortComment = link.getProperty(VizUtil.AFV_SHORT_COMMENT).getString();
			String longComment = link.getProperty(VizUtil.AFV_LONG_COMMENT).getString();

			if (parentId != null && !parentId.isEmpty())
			{
				sb.append("        <linking-phrase id=\"" + id + "\" label=\"" + title + "\" parent-id=\"" + parentId + "\" short-comment=\"" + shortComment
						+ "\" long-comment=\"" + longComment + "\"/>\r\n");
			}
			else
			{
				sb.append("        <linking-phrase id=\"" + id + "\" label=\"" + title + "\" short-comment=\"" + shortComment + "\" long-comment=\""
						+ longComment + "\"/>\r\n");
			}
		}
		sb.append("    </linking-phrase-list>\r\n");

		sb.append("    <linking-phrase-appearance-list>\r\n");
		statementIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_MAP, map);
		while (statementIterator.hasNext())
		{
			Statement statement = statementIterator.next();
			Resource link = statement.getSubject();
			if (!link.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_LINK))
			{
				continue;
			}

			String id = link.getProperty(AFOUtil.DCT_IDENTIFIER).getString();
			String x = link.getProperty(VizUtil.AFV_X_POSITION).getString();
			String y = link.getProperty(VizUtil.AFV_Y_POSITION).getString();
			String width = link.getProperty(VizUtil.AFV_WIDTH).getString();
			String height = link.getProperty(VizUtil.AFV_HEIGHT).getString();
			String minWidth = link.getProperty(VizUtil.AFV_MINIMUM_WIDTH).getString();
			String shadowColor = link.getProperty(VizUtil.AFV_SHADOW_COLOR).getString();
			String backgroundColor = link.getProperty(VizUtil.AFV_BACKGROUND_COLOR).getString();
			String minHeight = link.getProperty(VizUtil.AFV_MINIMUM_HEIGHT).getString();
			Resource linkBorder = link.getProperty(VizUtil.AFV_HAS_BORDER).getObject().asResource();
			String borderColor = linkBorder.getProperty(VizUtil.AFV_COLOR).getString();
			Resource linkFont = link.getProperty(VizUtil.AFV_HAS_FONT).getObject().asResource();
			String fontSize = linkFont.getProperty(VizUtil.AFV_SIZE).getString();
			String fontColor = linkFont.getProperty(VizUtil.AFV_COLOR).getString();
			sb.append("        <linking-phrase-appearance id=\"" + id + "\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + width + "\" height=\"" + height
					+ "\" min-width=\"" + minWidth + "\" min-height=\"" + minHeight + "\" border-color=\"" + borderColor + "\" font-size=\"" + fontSize
					+ "\" font-color=\"" + fontColor + "\" shadow-color=\"" + shadowColor + "\" background-color=\"" + backgroundColor);

			String backgroundImage = StringUtils.EMPTY;
			if (link.hasProperty(VizUtil.AFV_HAS_IMAGE))
			{
				Resource imageResource = link.getProperty(VizUtil.AFV_HAS_IMAGE).getResource();
				backgroundImage = imageResource.getURI();
				if (imageResource.hasProperty(VizUtil.AFV_IDENTIFIER)
						&& imageResource.getProperty(VizUtil.AFV_IDENTIFIER).getString().toLowerCase().trim().equals(CmapUtil.NO_IMAGE))
				{
					backgroundImage = CmapUtil.NO_IMAGE;
				}
			}
			if (!backgroundColor.isEmpty())
			{
				sb.append("\" background-image=\"" + backgroundImage);
			}

			String backgroundImageStyle = StringUtils.EMPTY;
			if (link.hasProperty(VizUtil.AFV_HAS_IMAGE) && link.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_STYLE))
			{
				backgroundImageStyle = link.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_STYLE).getString();
			}
			if (!backgroundImageStyle.isEmpty())
			{
				sb.append("\" background-image-style=\"" + backgroundImageStyle);
			}

			String backgroundImageLayout = StringUtils.EMPTY;
			if (link.hasProperty(VizUtil.AFV_HAS_IMAGE) && link.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().hasProperty(VizUtil.AFV_LAYOUT))
			{
				backgroundImageLayout = link.getProperty(VizUtil.AFV_HAS_IMAGE).getResource().getProperty(VizUtil.AFV_LAYOUT).getString();
			}
			if (!backgroundImageLayout.isEmpty())
			{
				sb.append("\" background-image-layout=\"" + backgroundImageLayout);
			}

			sb.append("\"/>\r\n");
		}

		sb.append("    </linking-phrase-appearance-list>\r\n");

		sb.append("    <connection-list>\r\n");
		statementIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_MAP, map);
		while (statementIterator.hasNext())
		{
			Statement statement = statementIterator.next();
			Resource connection = statement.getSubject();
			if (!connection.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONNECTION))
			{
				continue;
			}

			String id = connection.getProperty(AFOUtil.DCT_IDENTIFIER).getString();
			Resource from = connection.getProperty(VizUtil.AFV_CONNECTS_FROM).getObject().asResource();
			String fromId = from.toString();
			Resource to = connection.getProperty(VizUtil.AFV_CONNECTS_TO).getObject().asResource();
			String toId = to.toString();
			sb.append("        <connection id=\"" + id + "\" from-id=\"" + fromId + "\" to-id=\"" + toId + "\"/>\r\n");
		}
		sb.append("    </connection-list>\r\n");

		sb.append("    <connection-appearance-list>\r\n");
		statementIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_MAP, map);
		while (statementIterator.hasNext())
		{
			Statement statement = statementIterator.next();
			Resource connection = statement.getSubject();
			if (!connection.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_CONNECTION))
			{
				continue;
			}

			String id = connection.getProperty(AFOUtil.DCT_IDENTIFIER).getString();
			String fromPos = connection.getProperty(VizUtil.AFV_ANCHOR_FROM).getString();
			String toPos = connection.getProperty(VizUtil.AFV_ANCHOR_TO).getString();
			String lineType = connection.getProperty(VizUtil.AFV_LINE_TYPE).getString();
			String arrowHead = connection.getProperty(VizUtil.AFV_ARROW_HEAD).getString();

			if (connection.hasProperty(VizUtil.AFV_HAS_CONTROL_POINT))
			{
				sb.append("        <connection-appearance id=\"" + id + "\" from-pos=\"" + fromPos + "\" to-pos=\"" + toPos + "\" type=\"" + lineType
						+ "\" arrowhead=\"" + arrowHead + "\">\r\n");

				List<Point> controlPoints = new ArrayList<>();
				StmtIterator controlPointIterator = model.listStatements(connection, VizUtil.AFV_HAS_CONTROL_POINT, (RDFNode) null);
				while (controlPointIterator.hasNext())
				{
					Statement controlPointStatement = controlPointIterator.next();
					Resource controlPoint = controlPointStatement.getResource();
					if (!controlPoint.hasProperty(AFOUtil.RDF_TYPE, VizUtil.AFV_POINT))
					{
						continue;
					}
					String x = controlPoint.getProperty(VizUtil.AFV_X_POSITION).getString();
					String y = controlPoint.getProperty(VizUtil.AFV_Y_POSITION).getString();
					Integer index = Integer.valueOf(controlPoint.getProperty(AFOUtil.AFX_INDEX).getString());
					Point cp = new Point(id, x, y, index);
					controlPoints.add(cp);
				}
				Collections.sort(controlPoints);
				for (Iterator<Point> iterator = controlPoints.iterator(); iterator.hasNext();)
				{
					Point controlPoint = iterator.next();
					sb.append("            <control-point x=\"" + controlPoint.x + "\" y=\"" + controlPoint.y + "\"/>\r\n");
				}
				sb.append("        </connection-appearance>\r\n");
			}
			else
			{
				sb.append("        <connection-appearance id=\"" + id + "\" from-pos=\"" + fromPos + "\" to-pos=\"" + toPos + "\" type=\"" + lineType
						+ "\" arrowhead=\"" + arrowHead + "\"/>\r\n");
			}
		}
		sb.append("    </connection-appearance-list>\r\n");

		// check for images that are not set to "none"
		StmtIterator imageIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
		int numImages = 0;
		while (imageIterator.hasNext())
		{
			Statement imageStatement = imageIterator.next();
			Resource imageResource = imageStatement.getResource();
			if (imageResource.getProperty(VizUtil.AFV_IDENTIFIER).getString().toLowerCase().trim().equals(CmapUtil.NO_IMAGE)
					|| !imageResource.hasProperty(VizUtil.AFV_BYTES) || imageResource.getProperty(VizUtil.AFV_BYTES).getString() == null
					|| imageResource.getProperty(VizUtil.AFV_BYTES).getString().isEmpty())
			{
				continue;
			}

			numImages++;
		}

		if (numImages > 0)
		{
			// add images
			sb.append("    <image-list>\r\n");
			imageIterator = model.listStatements((Resource) null, VizUtil.AFV_HAS_IMAGE, (RDFNode) null);
			while (imageIterator.hasNext())
			{
				Statement imageStatement = imageIterator.next();
				Resource imageResource = imageStatement.getResource();
				if (imageResource.getProperty(VizUtil.AFV_IDENTIFIER).getString().toLowerCase().trim().equals(CmapUtil.NO_IMAGE)
						|| !imageResource.hasProperty(VizUtil.AFV_BYTES))
				{
					continue;
				}
				String bytes = imageResource.getProperty(VizUtil.AFV_BYTES).getString();
				sb.append("        <image id=\"" + imageResource.getURI() + "\" bytes=\"" + bytes + "\"/>\r\n");
			}
			sb.append("    </image-list>\r\n");
		}

		// create footer with values from RDF model
		String footer = VizUtil.FOOTER;
		String mapBackgroundColor = extractAsString(map, VizUtil.AFV_BACKGROUND_COLOR);
		footer = footer.replace(MapProperty.BACKGROUND_COLOR_MAP.name(), mapBackgroundColor);

		Resource conceptStyle = model.listStatements(map, VizUtil.AFV_HAS_CONCEPT_STYLE, (RDFNode) null).next().getResource();
		Resource conceptStyleFont = model.listStatements(conceptStyle, VizUtil.AFV_HAS_FONT, (RDFNode) null).next().getResource();
		Resource conceptStyleBorder = model.listStatements(conceptStyle, VizUtil.AFV_HAS_BORDER, (RDFNode) null).next().getResource();
		Resource linkStyle = model.listStatements(map, VizUtil.AFV_HAS_LINK_STYLE, (RDFNode) null).next().getResource();
		Resource linkStyleFont = model.listStatements(linkStyle, VizUtil.AFV_HAS_FONT, (RDFNode) null).next().getResource();
		Resource linkStyleBorder = model.listStatements(linkStyle, VizUtil.AFV_HAS_BORDER, (RDFNode) null).next().getResource();
		Resource connectionStyle = model.listStatements(map, VizUtil.AFV_HAS_CONNECTION_STYLE, (RDFNode) null).next().getResource();
		Resource resourceStyle = model.listStatements(map, VizUtil.AFV_HAS_RESOURCE_STYLE, (RDFNode) null).next().getResource();
		Resource resourceStyleFont = model.listStatements(resourceStyle, VizUtil.AFV_HAS_FONT, (RDFNode) null).next().getResource();

		for (Iterator<MapProperty> iterator = Arrays.asList(MapProperty.values()).iterator(); iterator.hasNext();)
		{
			String mapProperty = iterator.next().name();

			if (!(mapProperty.contains("CONCEPT") || mapProperty.contains("LINK") || mapProperty.contains("CONNECTION") || mapProperty.contains("RESOURCE")))
			{
				continue;
			}

			String value = StringUtils.EMPTY;
			Resource resource = null;
			if (mapProperty.contains("CONCEPT"))
			{
				if (mapProperty.startsWith("FONT"))
				{
					resource = conceptStyleFont;
				}
				else if (mapProperty.startsWith("BORDER"))
				{
					resource = conceptStyleBorder;
				}
				else
				{
					resource = conceptStyle;
				}
			}
			else if (mapProperty.contains("LINK"))
			{
				if (mapProperty.startsWith("FONT"))
				{
					resource = linkStyleFont;
				}
				else if (mapProperty.startsWith("BORDER"))
				{
					resource = linkStyleBorder;
				}
				else
				{
					resource = linkStyle;
				}
			}
			else if (mapProperty.contains("CONNECTION"))
			{
				resource = connectionStyle;
			}
			else if (mapProperty.contains("RESOURCE"))
			{
				if (mapProperty.startsWith("FONT"))
				{
					resource = resourceStyleFont;
				}
				else
				{
					resource = resourceStyle;
				}
			}

			value = extractAsString(resource, PropertyEnums.enum2StyleProperty.get(mapProperty));
			footer = footer.replace(mapProperty, value);
		}

		sb.append(footer);

		if (RdfCmap.writeTurtleToCxl)
		{
			sb.append("    <rdf-model>\r\n");
			ByteArrayOutputStream rdfModelOutputStream = new ByteArrayOutputStream()
			{
				private StringBuilder string = new StringBuilder();

				@Override
				public void write(int b)
				{
					string.append((char) b);
				}
			};
			model.write(rdfModelOutputStream, "TTL");
			String rdfModel = NumericEntityEscaper.below(0x20).translate(StringEscapeUtils.escapeXml11(rdfModelOutputStream.toString()));
			sb.append(rdfModel);
			sb.append("\r\n    </rdf-model>\r\n");
		}

		sb.append(VizUtil.CLOSING_TAG);

		Files.write(cxlPath, sb.toString().getBytes());
	}

	private static String extractAsString(Resource resource, Property dataTypeProperty)
	{
		return resource.listProperties(dataTypeProperty).next().getString();
	}

}
