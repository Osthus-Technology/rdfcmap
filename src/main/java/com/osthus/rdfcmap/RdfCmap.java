package com.osthus.rdfcmap;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.xml.sax.SAXException;

import com.osthus.rdfcmap.cmap.Cmap2TurtleConverter;
import com.osthus.rdfcmap.cmap.Turtle2CmapConverter;
import com.osthus.rdfcmap.cmap.shape.ShapesCreator2;
import com.osthus.rdfcmap.ontology.OntologyCreator;
import com.osthus.rdfcmap.path.PathFinder;
import com.osthus.rdfcmap.sparql.SparqlCreator;
import com.osthus.rdfcmap.util.CmapUtil;
import com.osthus.rdfcmap.util.Prefixes;

/**
 * Main class RDF CMap conversion tool check command line argument --help for available options
 *
 * @author Helge Krieg, OSTHUS GmbH
 */
public class RdfCmap
{
	private static Logger log;

	public static RDFNode rootNode = null;

	public static Resource root = null;

	public static boolean writeSeparateFiles = false;

	public static boolean writeFiles = false;

	public static boolean isAftColorScheme = false;

	public static boolean createSparql = false;

	public static boolean listPaths = false;

	public static boolean roundtrip = false;

	public static boolean createShapes = false;

	public static boolean createOntology = false;

	public static boolean writeTurtleToCxl = true;

	public static boolean optimizeLayout = false;

	public static int layoutDuration = 10;

	public static boolean isAutoLayout = true;

	private static String inputFileToConvert = StringUtils.EMPTY;

	private static String[] additionalInputFiles = null;

	public static boolean isRadialLayout = false;

	public static boolean isCircleLayout = false;

	public static boolean isGraphVizLayout = false;

	public static boolean adjustLabels = false;

	public static String dotBinary = "dot.exe";

	public static String graphVizAlgoName = "dot";

	public static boolean breakCycles = false;

	public static List<String> userSpecifiedInstanceNamespaces;

	public static boolean layoutLinks = true;

	public static float nodeSize = 0.1f;

	public static String initialLayout = "diagonal";

	public static String overlap = "scalexy";

	public static boolean avoidLinkLinkOverlap = false;

	public static int startseed = 1;

	public static boolean removeBnodes = false;

	public static String namespace = "http://www.example.com#";

	public static String prefix = "ex";

	public static boolean addSpecificProperties = false;

	public static boolean ignoreLongComments = false;

	public static boolean usePrefixes = true;

	public static boolean useBlankNodes = true;

	public static boolean humanReadable = true;

	public static boolean addDctTitles = true;

	public static boolean includePathProperties = true;

	public static boolean includeAllNodes = true;

	public static boolean createAdf = false;

	public static boolean useNamedShapes = true;

	public static boolean addRdfTypeShapeBasedOnShaclClass = true;

	public static boolean useNetworkShapeGraph = true;

	public static boolean includeVocabulary = false;

	public static boolean visualizeLiterals = true;

	public static String version = StringUtils.EMPTY;

	/**
	 * Main method of the CMap to RDF application.
	 *
	 * @throws ParseException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, JAXBException, ParseException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException
	{
		configureLog(Level.INFO);
		log = LogManager.getLogger("Logger");
		init(args);

		if (createSparql)
		{
			if (!inputFileToConvert.toLowerCase().endsWith("cxl"))
			{
				log.error("No .cxl input file found.");
				System.exit(1);
			}
			SparqlCreator sparqlCreator = new SparqlCreator();
			if (additionalInputFiles != null && additionalInputFiles.length > 0)
			{
				sparqlCreator.create(Paths.get(inputFileToConvert), additionalInputFiles);
			}
			else
			{
				sparqlCreator.create(Paths.get(inputFileToConvert), null);
			}
		}
		else if (createShapes)
		{
			writeFiles = true; // always create an instance model to be transformed to shapes
			String pathToTtlFile = StringUtils.EMPTY;
			if (inputFileToConvert.toLowerCase().endsWith("cxl"))
			{
				log.info("Creating shapes from CXL: " + inputFileToConvert.toString() + ((additionalInputFiles != null && additionalInputFiles.length > 0)
						? " using additional files: " + StringUtils.join(additionalInputFiles, ", ") : ""));
				Cmap2TurtleConverter cmap2TurtleConverter = new Cmap2TurtleConverter();
				if (additionalInputFiles != null && additionalInputFiles.length > 0)
				{
					cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), additionalInputFiles);
				}
				else
				{
					cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), null);
				}
				String localName = Paths.get(inputFileToConvert).toFile().getName();
				pathToTtlFile = localName.substring(0, localName.toString().length() - 3) + "ttl";
				inputFileToConvert = pathToTtlFile; // created TTL from CXL, now take new TTL as input for shapes creation
			}
			else if (!inputFileToConvert.toLowerCase().endsWith("ttl"))
			{
				log.error("No .cxl or .ttl as input file found.");
				System.exit(1);
			}

			ShapesCreator2 shapesCreator = new ShapesCreator2();
			if (additionalInputFiles != null && additionalInputFiles.length > 0)
			{
				shapesCreator.create(root, Paths.get(inputFileToConvert), additionalInputFiles);
			}
			else
			{
				shapesCreator.create(root, Paths.get(inputFileToConvert), null);
			}

			if (pathToTtlFile != null && !pathToTtlFile.isEmpty())
			{
				// Files.deleteIfExists(Paths.get(pathToTtlFile));
			}
		}
		else if (createOntology)
		{
			OntologyCreator ontologyCreator = new OntologyCreator();
			if (additionalInputFiles != null && additionalInputFiles.length > 0)
			{
				ontologyCreator.create(Paths.get(inputFileToConvert).toFile().getAbsolutePath(), additionalInputFiles);
			}
			else
			{
				ontologyCreator.create(Paths.get(inputFileToConvert).toFile().getAbsolutePath(), null);
			}
		}
		else if (listPaths)
		{
			if (!inputFileToConvert.toLowerCase().endsWith("cxl"))
			{
				log.error("No .cxl input file found.");
				System.exit(1);
			}
			PathFinder pathFinder = new PathFinder();
			if (additionalInputFiles != null && additionalInputFiles.length > 0)
			{
				pathFinder.find(Paths.get(inputFileToConvert), additionalInputFiles);
			}
			else
			{
				pathFinder.find(Paths.get(inputFileToConvert), null);
			}
		}
		else if (inputFileToConvert.toLowerCase().endsWith("cxl"))
		{
			Cmap2TurtleConverter cmap2TurtleConverter = new Cmap2TurtleConverter();
			if (roundtrip)
			{
				log.info("Updating CXL: " + inputFileToConvert.toString() + ((additionalInputFiles != null && additionalInputFiles.length > 0)
						? " using additional files: " + StringUtils.join(additionalInputFiles, ", ") : ""));
				if (additionalInputFiles != null && additionalInputFiles.length > 0)
				{
					cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), additionalInputFiles);
				}
				else
				{
					cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), null);
				}
				String pathToTtlFile = inputFileToConvert.toString().substring(0, inputFileToConvert.toString().length() - 3) + "ttl";
				Turtle2CmapConverter turtle2CmapConverter = new Turtle2CmapConverter();
				turtle2CmapConverter.convert(Paths.get(pathToTtlFile));
				Files.deleteIfExists(Paths.get(pathToTtlFile));
			}
			else
			{
				if (additionalInputFiles != null && additionalInputFiles.length > 0)
				{
					cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), additionalInputFiles);
				}
				else
				{
					cmap2TurtleConverter.convert(Paths.get(inputFileToConvert), null);
				}
			}
		}
		else if (inputFileToConvert.toLowerCase().endsWith("ttl") || inputFileToConvert.toLowerCase().endsWith("owl")
				|| inputFileToConvert.toLowerCase().endsWith("xml") || inputFileToConvert.toLowerCase().endsWith("adf"))
		{
			if (!roundtrip)
			{
				Turtle2CmapConverter turtle2CmapConverter = new Turtle2CmapConverter();
				if (additionalInputFiles != null && additionalInputFiles.length > 0)
				{
					turtle2CmapConverter.convert(Paths.get(inputFileToConvert), additionalInputFiles);
				}
				else
				{
					turtle2CmapConverter.convert(Paths.get(inputFileToConvert), null);
				}
			}
			else
			{
				log.error("Found commandline option to update CXL but no .cxl input file was specified.");
				System.exit(1);
			}
		}
		else
		{
			log.info("No input file for conversion found. Use command line option -i<filename> to specify input file.");
			System.exit(1);
		}

		log.info("done");
	}

	private static void init(String[] args) throws ParseException, NoSuchFieldException, IllegalAccessException, IOException
	{
		printOsthus();
		printVersion();

		Option help = Option.builder("h").longOpt("help").required(false).desc("Show list of command line arguments.").build();
		Option input = Option.builder("i").longOpt("input").required(true).hasArg()
				.desc("Specifies an input file for conversion (either TTL or CXL). Specifying a TTL file will convert to CXL, specifying a CXL file will convert to TTL. Mandatory argument.")
				.build();
		Option externalFiles = Option.builder("r").longOpt("read").required(false).argName("file-1> <file-2> <...").hasArgs()
				.desc("Specifies additional files (either TTL or CXL) with triples to be used during conversion. Optional argument. Up to 10 external files.")
				.build();
		Option info = Option.builder("?").longOpt("info").required(false).desc("Write out program info.").build();
		Option separate = Option.builder("s").longOpt("separate").required(false)
				.desc("For CXL to TTL conversion, write all instances and visualization model to separate TTL files in folder \"separate files\".").build();
		Option writefiles = Option.builder("wf").longOpt("writefiles").required(false)
				.desc("For CXL to TTL conversion, write instance and visualization model to TTL files in folder \"separate files\".").build();
		Option donotuseprefixes = Option.builder("np").longOpt("noprefixes").required(false)
				.desc("For CXL to TTL conversion, write TTL files without applying prefixes.").build();
		Option skipPathProperties = Option.builder("spp").longOpt("nopathproperties").required(false)
				.desc("For SPARQL queries, skip properties of nodes on path.").build();
		Option skipNodesOutsidePath = Option.builder("snp").longOpt("skipnodesoutsidepath").required(false)
				.desc("For SPARQL queries, skip nodes that are not located on path from source to target. Path results from simple search algorithm and does not have to be shortest path but may include detours and dead ends.")
				.build();
		Option donotuseblanknodes = Option.builder("nb").longOpt("noblanknodes").required(false)
				.desc("For CXL to TTL conversion, write TTL files without blank nodes but only named resources based on uuids.").build();
		Option donotusenamedshapes = Option.builder("np").longOpt("nonamedshapes").required(false)
				.desc("For shape conversion, write shapes nested as blank nodes.").build();
		Option donotusenetworkshapegraph = Option.builder("nns").longOpt("nonetworkshape").required(false)
				.desc("For shape conversion, do not write shapes as simple network but create shapes in a concentric fashion following property paths originating from root node.")
				.build();
		Option createadf = Option.builder("adf").longOpt("createadf").required(false).desc("For CXL to TTL conversion, write instance data to ADF.").build();
		Option noTurtle = Option.builder("n").longOpt("noturtle").required(false).desc("Do not store RDf model within CXL.").build();
		Option noTitles = Option.builder("nt").longOpt("notitles").required(false).desc("Do not add dct:title based on labels of CXL.").build();
		Option machine = Option.builder("ma").longOpt("machine").required(false).desc("Write machine-readable TTL without human readable comments.").build();
		Option debug = Option.builder("de").longOpt("debug").required(false).desc("Show extended output for debugging purposes.").build();
		Option color = Option.builder("c").longOpt("color").required(false).desc("Use color scheme of AFT.").build();
		Option sparql = Option.builder("q").longOpt("sparql").required(false)
				.desc("Create a sparql query for properties of a target node. Target node must have oval shape. Source node must have oval shape with dashed border.")
				.build();
		Option updateCxl = Option.builder("u").longOpt("update").required(false).desc("Update visualization directly (roundtrip cxl -> ttl -> cxl).").build();
		Option version = Option.builder("v").longOpt("version").required(false).desc("Show version information.").build();
		Option prefix = Option.builder("p").longOpt("prefix").required(false).argName("prefixes=namespaces").valueSeparator().hasArg()
				.desc("Define mapping of prefixes to namespaces. Specify mapping as comma-separated list of \"prefix1=namespace1, prefix2=namespace2, ...\". List must be surrounded by quotes in order to separate this command-line argument from other arguments.")
				.build();
		Option listPrefixes = Option.builder("l").longOpt("listprefix").required(false).desc("List prefixes for namespaces.").build();
		Option pathfinder = Option.builder("f").longOpt("pathfinder").required(false).desc("List all paths starting from root node (with oval border).")
				.build();
		Option shapes = Option.builder("sh").longOpt("shapes").required(false).desc("Create SHACL shapes from model.").build();
		Option ontology = Option.builder("owl").longOpt("ontology").required(false).desc("Create an ontology based on SHACL shapes derived from the model.")
				.build();
		Option optimize = Option.builder("o").longOpt("optimize").required(false).desc("Optimize layout using gephi layout algorithm.").build();
		Option optimizeTime = Option.builder("t").longOpt("time").required(false).hasArg().desc("Optimize layout for the given number of seconds.").build();
		Option layouter = Option.builder("a").longOpt("layout").required(false).hasArg()
				.desc("Use the specified layout as gephi layout algorithm. Possible values: auto, radial, circle, graphviz").build();
		Option dotBinary = Option.builder("d").longOpt("dot").required(false).hasArg().desc("Specify absolute path to dot.exe for graphviz layouting.").build();
		Option graphVizAlgo = Option.builder("g").longOpt("graphvizalgo").required(false).hasArg()
				.desc("Specify rendering algorithm of graphviz e.g. dot, neato, fdp").build();
		Option adjustLabels = Option.builder("b").longOpt("label").required(false)
				.desc("Adjust labels according to level in class hierarchy. Top level terms with low level number get printed out with larger size and larger font size.")
				.build();
		Option breakcycles = Option.builder("x").longOpt("break").required(false).desc("Break cycles in a cyclic graph for better layout.").build();
		Option instanceNamespace = Option.builder("n").longOpt("namespace").required(false).hasArgs()
				.desc("Specify namespaces of instances to visualize. Default is \"urn:uuid:\"").build();
		Option noLinkLayout = Option.builder("e").longOpt("nolinklayout").required(false)
				.desc("Exclude links during layouting. Layout is based on concept positions only. Link labels are positioned afterwards at the centers of connected concepts.")
				.build();
		Option nodeSize = Option.builder("z").longOpt("nodesize").required(false).hasArg()
				.desc("Use the given node size for optimizing layout with graphviz. Default: 0.1").build();
		Option shapeRoot = Option.builder("rt").longOpt("root").required(false).hasArg()
				.desc("Use the given IRI of instance or class as root node for shapes creation.").build();
		Option initLayout = Option.builder("y").longOpt("initlayout").required(false).hasArg()
				.desc("Initial layout as starting point for automatic layout. diagonal, square, blob. Default: diagonal").build();
		Option overlap = Option.builder("w").longOpt("overlap").required(false).hasArg()
				.desc("Overlap option for graphviz. true, false, scale. default:scalexy").build();
		Option linkoverlap = Option.builder("u").longOpt("linkoverlap").required(false)
				.desc("Avoid overlap between link labels, option for graphviz. Default is only avoiding overlap between links and nodes.").build();
		Option startseed = Option.builder("m").longOpt("startseed").required(false).hasArg()
				.desc("Start seeding parameter as option for graphviz. Influences force-directed layout. Integer value, default 1").build();
		Option removeBNodes = Option.builder().longOpt("removebnodes").required(false)
				.desc("Replace all blank nodes of the model as named resources with UUIDs.").build();
		Option ontoNamespace = Option.builder("nsp").longOpt("ontonamespace").required(false).hasArg()
				.desc("Namespace for ontology creation. All classes will be created with a namespace as defined by --ontonamespace.").build();
		Option ontoPrefix = Option.builder("oprf").longOpt("ontoprefix").required(false).hasArg().desc("Prefix for namespace of ontology creation.").build();
		Option specificProperties = Option.builder("specprop").longOpt("specificproperties").required(false)
				.desc("During ontology creation add specific domain properties as subproperties of AFX if possible.").build();
		Option dropLongComments = Option.builder("dlc").longOpt("droplongcomments").required(false)
				.desc("Ignore all existing long comments and create new ones.").build();
		Option hideLiteralValues = Option.builder("hlv").longOpt("hideliterals").required(false)
				.desc("Do not show literal values as explicit nodes in visualization.").build();

		Options infoOptions = new Options();
		infoOptions.addOption(help);
		infoOptions.addOption(info);
		infoOptions.addOption(version);

		Options configOptions = new Options();
		configOptions.addOption(prefix);
		configOptions.addOption(listPrefixes);

		Options appOptions = new Options();
		appOptions.addOption(input);
		appOptions.addOption(debug);
		appOptions.addOption(externalFiles);
		appOptions.addOption(separate);
		appOptions.addOption(writefiles);
		appOptions.addOption(prefix);
		appOptions.addOption(donotuseprefixes);
		appOptions.addOption(donotuseblanknodes);
		appOptions.addOption(donotusenamedshapes);
		appOptions.addOption(donotusenetworkshapegraph);
		appOptions.addOption(skipNodesOutsidePath);
		appOptions.addOption(createadf);
		appOptions.addOption(skipPathProperties);
		appOptions.addOption(noTitles);
		appOptions.addOption(machine);
		appOptions.addOption(color);
		appOptions.addOption(sparql);
		appOptions.addOption(pathfinder);
		appOptions.addOption(updateCxl);
		appOptions.addOption(shapes);
		appOptions.addOption(noTurtle);
		appOptions.addOption(optimize);
		appOptions.addOption(optimizeTime);
		appOptions.addOption(layouter);
		appOptions.addOption(adjustLabels);
		appOptions.addOption(dotBinary);
		appOptions.addOption(graphVizAlgo);
		appOptions.addOption(breakcycles);
		appOptions.addOption(instanceNamespace);
		appOptions.addOption(noLinkLayout);
		appOptions.addOption(nodeSize);
		appOptions.addOption(shapeRoot);
		appOptions.addOption(initLayout);
		appOptions.addOption(overlap);
		appOptions.addOption(linkoverlap);
		appOptions.addOption(startseed);
		appOptions.addOption(removeBNodes);
		appOptions.addOption(ontology);
		appOptions.addOption(ontoNamespace);
		appOptions.addOption(ontoPrefix);
		appOptions.addOption(specificProperties);
		appOptions.addOption(dropLongComments);
		appOptions.addOption(hideLiteralValues);

		Options allOptions = new Options();
		allOptions.addOption(help);
		allOptions.addOption(info);
		allOptions.addOption(input);
		allOptions.addOption(debug);
		allOptions.addOption(externalFiles);
		allOptions.addOption(separate);
		allOptions.addOption(writefiles);
		allOptions.addOption(donotuseprefixes);
		allOptions.addOption(donotuseblanknodes);
		allOptions.addOption(donotusenamedshapes);
		allOptions.addOption(donotusenetworkshapegraph);
		allOptions.addOption(skipPathProperties);
		allOptions.addOption(skipNodesOutsidePath);
		allOptions.addOption(createadf);
		allOptions.addOption(noTitles);
		allOptions.addOption(machine);
		allOptions.addOption(color);
		allOptions.addOption(sparql);
		allOptions.addOption(pathfinder);
		allOptions.addOption(prefix);
		allOptions.addOption(listPrefixes);
		allOptions.addOption(updateCxl);
		allOptions.addOption(version);
		allOptions.addOption(shapes);
		allOptions.addOption(noTurtle);
		allOptions.addOption(optimize);
		allOptions.addOption(optimizeTime);
		allOptions.addOption(layouter);
		allOptions.addOption(adjustLabels);
		allOptions.addOption(dotBinary);
		allOptions.addOption(graphVizAlgo);
		allOptions.addOption(breakcycles);
		allOptions.addOption(instanceNamespace);
		allOptions.addOption(noLinkLayout);
		allOptions.addOption(nodeSize);
		allOptions.addOption(shapeRoot);
		allOptions.addOption(initLayout);
		allOptions.addOption(overlap);
		allOptions.addOption(linkoverlap);
		allOptions.addOption(startseed);
		allOptions.addOption(removeBNodes);
		allOptions.addOption(ontology);
		allOptions.addOption(ontoNamespace);
		allOptions.addOption(ontoPrefix);
		allOptions.addOption(specificProperties);
		allOptions.addOption(dropLongComments);
		allOptions.addOption(hideLiteralValues);

		CommandLine cmd = new DefaultParser().parse(infoOptions, args, true);

		HelpFormatter formatter = new HelpFormatter();

		if (cmd.hasOption("help"))
		{
			formatter.printHelp("rdfcmap", allOptions, true);
		}

		if (cmd.hasOption("info"))
		{
			System.out.println("Rdfcmap is a tool for conversion between RDF instance model and visualization based on CMap.\n©OSTHUS 2017");
		}

		if (cmd.hasOption("version"))
		{
			printVersion();

		}

		if (cmd.hasOption("help") || cmd.hasOption("version") || cmd.hasOption("info") && cmd.getOptions().length < 2)
		{
			printVersion();
			System.exit(0);
		}

		cmd = new DefaultParser().parse(configOptions, args, true);
		if (cmd.hasOption("prefix"))
		{
			String prefixString = cmd.getOptionValue("prefix");
			String[] prefixList = prefixString.split(",");
			String[] prefixes = new String[prefixList.length * 2];
			for (int i = 0; i < prefixList.length; i++)
			{
				String[] singlePrefix = prefixList[i].trim().split("=");
				prefixes[2 * i] = singlePrefix[0];
				prefixes[2 * i + 1] = singlePrefix[1];
			}

			Prefixes.updatePrefixes(prefixes);
		}

		if (cmd.hasOption("listprefix"))
		{
			Prefixes.listPrefixes();
			System.exit(0);
		}

		CommandLineParser parser = new DefaultParser();
		try
		{
			cmd = parser.parse(appOptions, args);
		}
		catch (Exception e)
		{
			log.info("Exception while parsing command line arguments.");
			if (!cmd.hasOption("i") && !cmd.hasOption("input"))
			{
				log.info("No input file specified!");
			}
			log.info("Message: " + e.getLocalizedMessage());
			log.info("Stacktrace : \n" + StringUtils.join(e.getStackTrace(), "\n"));
			System.exit(1);
		}

		inputFileToConvert = cmd.getOptionValue("input");

		additionalInputFiles = cmd.getOptionValues("r");

		if (cmd.hasOption("separate"))
		{
			writeSeparateFiles = true;
		}

		if (cmd.hasOption("debug"))
		{
			configureLog(Level.DEBUG);
			log.debug("Writing debug output.");
		}

		if (cmd.hasOption("writefiles"))
		{
			writeFiles = true;
		}

		if (cmd.hasOption("color"))
		{
			isAftColorScheme = true;
		}

		if (cmd.hasOption("sparql"))
		{
			createSparql = true;
		}

		if (cmd.hasOption("droplongcomments"))
		{
			ignoreLongComments = true;
		}

		if (cmd.hasOption("nopathproperties"))
		{
			includePathProperties = false;
		}

		if (cmd.hasOption("nonetworkshape"))
		{
			useNetworkShapeGraph = false;
		}

		if (cmd.hasOption("skipnodesoutsidepath"))
		{
			includeAllNodes = false;
		}

		if (cmd.hasOption("createadf"))
		{
			createAdf = true;
		}

		if (cmd.hasOption("noprefixes"))
		{
			usePrefixes = false;
		}

		if (cmd.hasOption("noblanknodes"))
		{
			useBlankNodes = false;
		}

		if (cmd.hasOption("nonamedshapes"))
		{
			useNamedShapes = false;
		}

		if (cmd.hasOption("machine"))
		{
			humanReadable = false;
		}

		if (cmd.hasOption("notitles"))
		{
			addDctTitles = false;
		}

		if (cmd.hasOption("pathfinder"))
		{
			listPaths = true;
		}

		if (cmd.hasOption("update"))
		{
			roundtrip = true;
		}

		if (cmd.hasOption("shapes"))
		{
			createShapes = true;
		}

		if (cmd.hasOption("ontology"))
		{
			createOntology = true;
		}

		if (cmd.hasOption("noturtle"))
		{
			writeTurtleToCxl = false;
		}

		if (cmd.hasOption("optimize"))
		{
			optimizeLayout = true;
		}

		if (cmd.hasOption("hideliterals"))
		{
			visualizeLiterals = false;
		}

		if (cmd.hasOption("label"))
		{
			RdfCmap.adjustLabels = true;
		}

		if (cmd.hasOption("time"))
		{
			layoutDuration = Integer.valueOf(cmd.getOptionValue("time"));
		}

		if (cmd.hasOption("nodesize"))
		{
			RdfCmap.nodeSize = Float.valueOf(cmd.getOptionValue("nodesize"));
		}

		if (cmd.hasOption("root"))
		{
			RdfCmap.root = null;
			String uri = cmd.getOptionValue("root");
			if (uri.startsWith(CmapUtil.URN_UUID))
			{
				RdfCmap.root = ResourceFactory.createResource(uri);
			}
			else if (uri.contains(":"))
			{
				String[] segments = uri.split(":");
				String namespace = Prefixes.nsPrefixMap.get(segments[0]);
				RdfCmap.root = ResourceFactory.createResource(namespace + segments[1]);
			}
			else
			{
				RdfCmap.root = ResourceFactory.createResource(uri);
			}
		}

		if (cmd.hasOption("layout"))
		{
			String value = cmd.getOptionValue("layout").trim().toLowerCase();
			if (value != null & !value.isEmpty())
			{
				if (value.equals("radial"))
				{
					RdfCmap.isAutoLayout = false;
					RdfCmap.isRadialLayout = true;
				}
				else if (value.equals("circle"))
				{
					RdfCmap.isAutoLayout = false;
					RdfCmap.isCircleLayout = true;
				}
				else if (value.equals("graphviz"))
				{
					RdfCmap.isAutoLayout = false;
					RdfCmap.isGraphVizLayout = true;
					if (cmd.hasOption("dot"))
					{
						RdfCmap.dotBinary = cmd.getOptionValue("dot");
					}
					if (cmd.hasOption("graphvizalgo"))
					{
						RdfCmap.graphVizAlgoName = cmd.getOptionValue("graphvizalgo").trim().toLowerCase();
					}
				}
			}
		}

		if (cmd.hasOption("break"))
		{
			RdfCmap.breakCycles = true;
		}

		if (cmd.hasOption("nolinklayout"))
		{
			RdfCmap.layoutLinks = false;
		}

		if (cmd.hasOption("namespace"))
		{
			RdfCmap.userSpecifiedInstanceNamespaces = Arrays.asList(cmd.getOptionValues("namespace"));
		}

		if (cmd.hasOption("initlayout"))
		{
			RdfCmap.initialLayout = cmd.getOptionValue("initlayout").trim().toLowerCase();
		}

		if (cmd.hasOption("overlap"))
		{
			RdfCmap.overlap = cmd.getOptionValue("overlap").trim().toLowerCase();
		}

		if (cmd.hasOption("ontonamespace"))
		{
			RdfCmap.namespace = cmd.getOptionValue("ontonamespace").trim().toLowerCase();
		}

		if (cmd.hasOption("ontoprefix"))
		{
			RdfCmap.prefix = cmd.getOptionValue("ontoprefix").trim().toLowerCase();
		}

		if (cmd.hasOption("specificproperties"))
		{
			RdfCmap.addSpecificProperties = true;
		}

		if (cmd.hasOption("startseed"))
		{
			RdfCmap.startseed = Integer.valueOf(cmd.getOptionValue("startseed").trim().toLowerCase());
		}

		if (cmd.hasOption("linkoverlap"))
		{
			RdfCmap.avoidLinkLinkOverlap = true;
		}

		if (cmd.hasOption("removebnodes"))
		{
			RdfCmap.removeBnodes = true;
		}

		System.setProperty("file.encoding", "UTF-8");
		Field charset = Charset.class.getDeclaredField("defaultCharset");
		charset.setAccessible(true);
		charset.set(null, null);
		if (!Charset.defaultCharset().displayName().equals("UTF-8"))
		{
			log.info("Current character set: " + Charset.defaultCharset().displayName());
		}
	}

	private static void printVersion() throws IOException
	{
		final Properties properties = new Properties();
		properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("rdfcmap.properties"));
		version = properties.getProperty("version");
		log.info("Rdfcmap V" + version + " ©OSTHUS 2017-2018");
	}

	public static void printOsthus()
	{
		//@formatter:off
		log.info("\r\n"
				+ "  ____  ____________ ____  ______      \r\n" +
				" / __ \\/ __/_  __/ // / / / / __/      \r\n" +
				"/ /_/ /\\ \\  / / / _  / /_/ /\\ \\        \r\n" +
				"\\____/___/_/_/_/_//_/\\____/___/_   ___ \r\n" +
				"  / _ \\/ _ \\/ __/ ___/  |/  / _ | / _ \\\r\n" +
				" / , _/ // / _// /__/ /|_/ / __ |/ ___/\r\n" +
				"/_/|_/____/_/  \\___/_/  /_/_/ |_/_/  \r\n");
		//@formatter:on
	}

	private static void configureLog(Level level)
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "OFF");
		System.setProperty("org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY", "OFF");
		System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");

		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setStatusLevel(level);
		builder.setConfigurationName("Config");
		AppenderComponentBuilder console = builder.newAppender("Stdout", "Console").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
				.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%d %-5level"));
		builder.add(console);
		builder.add(builder.newRootLogger(level).add(builder.newAppenderRef("Stdout")));
		Configurator.setRootLevel(level);
		LoggerContext loggerContext = Configurator.initialize(builder.build());
		loggerContext.updateLoggers();

		// filter out spurious logging from slf4j
		PrintStream filterOut = new PrintStream(System.err)
		{
			@Override
			public void println(String l)
			{
				if (!l.startsWith("SLF4J"))
				{
					super.println(l);
				}
			}
		};
		System.setErr(filterOut);
	}
}
