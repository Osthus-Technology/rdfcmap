package com.osthus.rdfcmap.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Definition of mappings for prefixes to namespaces
 *
 * @author Helge Krieg, OSTHUS GmbH
 */
public class Prefixes
{
	private static final Logger log = LogManager.getLogger("Logger");

	public static final Map<String, String> aftPrefixMap = new HashMap<String, String>()
	{
		{
			put("af-c", AFOUtil.AFC_PREFIX);
			put("af-cq", AFOUtil.AFCQ_PREFIX);
			put("af-e", AFOUtil.AFE_PREFIX);
			put("af-m", AFOUtil.AFM_PREFIX);
			put("af-r", AFOUtil.AFR_PREFIX);
			put("af-x", AFOUtil.AFX_PREFIX);
			put("af-p", AFOUtil.AFP_PREFIX);
			put("af-rl", AFOUtil.AFRL_PREFIX);
			put("af-fn", AFOUtil.AFFN_PREFIX);
			put("af-re", AFOUtil.AFRE_PREFIX);
			put("af-s", AFOUtil.AFS_PREFIX);
			put("af-dt", AFOUtil.AFDT_PREFIX);
			put("af-q", AFOUtil.AFQ_PREFIX);
			put("af-v", VizUtil.AFV_PREFIX);
			put("af-cur", AFOUtil.AFCUR_PREFIX);
			put("af-ec-001", AFOUtil.AFEC_001_PREFIX);
			put("af-ec-002", AFOUtil.AFEC_002_PREFIX);
			put("af-ec-003", AFOUtil.AFEC_003_PREFIX);
			put("af-ec-004", AFOUtil.AFEC_004_PREFIX);
			put("af-ec-005", AFOUtil.AFEC_005_PREFIX);
			put("af-ec-006", AFOUtil.AFEC_006_PREFIX);
		}
	};

	public static final Map<String, String> oboPrefixMap = new HashMap<String, String>()
	{
		{
			// put("obo", AFTUtil.OBO_PREFIX); //not used due to possibly malformed URLs after processing with owlapi
			put("cl", AFOUtil.CL_PREFIX);
			put("go", AFOUtil.GO_PREFIX);
			put("ro", AFOUtil.RO_PREFIX);
			put("uo", AFOUtil.UO_PREFIX);
			put("iao", AFOUtil.IAO_PREFIX);
			put("bfo", AFOUtil.BFO_PREFIX);
			put("gaz", AFOUtil.GAZ_PREFIX);
			put("mop", AFOUtil.MOP_PREFIX);
			put("obi", AFOUtil.OBI_PREFIX);
			put("chmo", AFOUtil.CHMO_PREFIX);
			put("envo", AFOUtil.ENVO_PREFIX);
			put("ncbi", AFOUtil.NCBI_PREFIX);
			put("pato", AFOUtil.PATO_PREFIX);
			put("chebi", AFOUtil.CHEBI_PREFIX);
			put("uberon", AFOUtil.UBERON_PREFIX);
		}
	};
	public static Map<String, String> nsPrefixMap = new HashMap<String, String>()
	{
		{
			putAll(aftPrefixMap);
			putAll(oboPrefixMap);
			put("m", AFOUtil.MATHML_PREFIX);
			put("co", AFOUtil.COLLECTION_ONTOLOGY_PREFIX);
			put("ex", AFOUtil.EXAMPLE_PREFIX);
			put("qb", AFOUtil.QB_PREFIX);
			put("dct", AFOUtil.DCT_PREFIX);
			put("ex2", AFOUtil.EXAMPLE2_PREFIX);
			put("hdf", AFOUtil.HDF_PREFIX);
			put("ldp", AFOUtil.LDP_PREFIX);
			put("map", AFOUtil.MAP_PREFIX);
			put("ops", AFOUtil.OPS_PREFIX);
			put("ore", AFOUtil.ORE_PREFIX);
			put("org", AFOUtil.ORG_PREFIX);
			put("owl", AFOUtil.OWL_PREFIX);
			put("pav", AFOUtil.PAV_PREFIX);
			put("rdf", AFOUtil.RDF_PREFIX);
			put("xml", AFOUtil.XML_PREFIX);
			put("xsd", AFOUtil.XSD_PREFIX);
			put("foaf", AFOUtil.FOAF_PREFIX);
			put("omcd", AFOUtil.OPENMATH_PREFIX);
			put("prov", AFOUtil.PROV_PREFIX);
			put("qudt", AFOUtil.QUDT_SCHEMA_PREFIX);
			put("rdfs", AFOUtil.RDFS_PREFIX);
			put("skos", AFOUtil.SKOS_PREFIX);
			put("unit", AFOUtil.QUDT_UNIT_PREFIX);
			put("time", AFOUtil.TIME_PREFIX);
			put("void", AFOUtil.VOID_PREFIX);
			put("afs-c", AFOUtil.AFS_C_PREFIX);
			put("afs-q", AFOUtil.AFS_Q_PREFIX);
			put("shacl", AFOUtil.SHACL_PREFIX);
			put("vcard", AFOUtil.VCARD_PREFIX);
			put("adf-dc", AFOUtil.ADF_DC_PREFIX);
			put("adf-dp", AFOUtil.ADF_DP_PREFIX);
			put("af-map", AFOUtil.AFMAP_PREFIX);
			put("afs-dc", AFOUtil.AFS_DC_PREFIX);
			put("afs-hr", AFOUtil.AFS_HR_PREFIX);
			put("dctype", AFOUtil.DCTYPE_PREFIX);
			put("premis", AFOUtil.PREMIS_PREFIX);
			put("af-math", AFOUtil.AFMATH_PREFIX);
			put("qudt-ext", AFOUtil.QUDT_SCHEMA_EXT_PREFIX);
			put("unit-ext", AFOUtil.QUDT_UNIT_EXT_PREFIX);
			put("qudt-unit", AFOUtil.QUDT_UNIT_PREFIX);
			put("adf-dc-hdf", AFOUtil.ADF_DC_HDF_PREFIX);
			put("quantity-ext", AFOUtil.QUDT_QUANTITY_EXT_PREFIX);
		}
	};

	private static String prefixes = "";

	private static Map<String, String> namespaceMap = new HashMap<String, String>();

	public static String getSparqlPrefixes()
	{
		if (!prefixes.equals(""))
		{
			return prefixes;
		}

		createSparqlPrefixesString();

		return prefixes;
	}

	public static Map<String, String> getNamespaceMap()
	{
		if (!namespaceMap.isEmpty())
		{
			return namespaceMap;
		}

		createNamespaceMap();

		return namespaceMap;
	}

	private static void createSparqlPrefixesString()
	{
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : nsPrefixMap.entrySet())
		{
			sb.append("PREFIX ");
			sb.append(entry.getKey());
			sb.append(": <");
			sb.append(entry.getValue());
			sb.append(">\n");
		}

		prefixes = sb.toString();
		List<String> lines = Arrays.asList(prefixes.split("\\n"));
		Collections.sort(lines);
		prefixes = StringUtils.join(lines, "\n");
	}

	private static void createNamespaceMap()
	{
		for (Entry<String, String> entry : nsPrefixMap.entrySet())
		{
			namespaceMap.put(entry.getValue(), entry.getKey());
		}
	}

	public static void updatePrefixes(String[] newPrefixes)
	{
		for (int i = 0; i < newPrefixes.length; i = i + 2)
		{
			nsPrefixMap.put(newPrefixes[i], newPrefixes[i + 1]);
		}

		createNamespaceMap();
	}

	public static void listPrefixes()
	{
		log.info("Prefixes: ");
		List<String> prefixes = new ArrayList(nsPrefixMap.keySet());
		Collections.sort(prefixes);
		for (Iterator<String> iterator = prefixes.iterator(); iterator.hasNext();)
		{
			String prefix = iterator.next();
			log.info(String.format("%1$15s %2$s", prefix, nsPrefixMap.get(prefix)));
		}
		log.info("");
	}
}
