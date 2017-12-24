# rdf-cmap
RDFCMap converts between CMap CXL and RDF TTL. 

Usage to convert from CXL to TTL
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar -i your-input-cmap-model.cxl

Note: Conversion from CXL to TTL supports vocabulary of AFO2.0

Usage to convert from TTL to CXL:
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar -i your-input-rdf-model.ttl

Usage to visualize data description of ADF as CXL:
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar -i your-input-adf-file.adf

Note: Creation of a useful visualizations of large graphs requires automatic layouting of nodes. Rdfcmap supports layouting based on graphviz as well as different layout algorithms provided by gephi. Please contact OSTHUS for further information (office(at)osthus(dot)com).  

Use --help to see command line options
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar --help

How to create a cmap to be used with rdfcmap?

1) Get CMAP Tools from https://cmap.ihmc.us/
2) Create a new cmap following guidelines below on formatting 
3) All nodes of a cmap model are transformed to instances in rdf model
4) Labels of cmap nodes are transformed to titles of instances using dct:title 
5) Links between nodes in cmap are transformed to object properties in rdf model.
6) Labels of links must agree to existing labels (skos:prefLabel) of properties in RDF vocabulary.
7) Cardinalities can be added to object properties in cmap: default without specification of cardinality is "min 0". Exact cardinality is specified by adding the number, e.g. "has part =1" means there is exactly 1 link of "has part". Minimum cardinality is specified by ">1" (one or more)
8) Export cmap as CXL file

Changelog:

V2.3.2
* Create visualization directly from ADF
* Note: Reading and writing ADF requires presence of ADF-API in build path. Please contact OSTHUS for further support and information (office(at)osthus(dot)com).  

V2.3.1 
* Improved conversion from ttl to cmap in order to support blank nodes and literal values
* Improved import of triples
* Note: Conversion from TTL to CXL requires layouting of concepts in order to get a useful visualization. Enabling automatic layouting with rdfcmap needs additional third-party dependencies. Currently, rdfcmap supports layouting based on graphviz as well as different layout algorithms provided by gephi. Please contact OSTHUS for further information (office(at)osthus(dot)com).  

V2.2.0 
* Updated conversion from ttl to cmap in order to support BFO aligned models
* Note: Conversion from TTL to CXL requires layouting of concepts in order to get a useful visualization. Enabling automatic layouting with rdfcmap needs additional third-party dependencies. Currently, rdfcmap supports layouting based on graphviz as well as different layout algorithms provided by gephi. Please contact OSTHUS for further information (office(at)osthus(dot)com).  
  
V2.1.4 
* Updated conversion from cmap to ttl in order to support BFO aligned models
* Added SPARQL export
* Added Shape export
* Added ADF export

Copyright OSTHUS 2016-2018


 