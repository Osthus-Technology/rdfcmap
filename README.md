# rdf-cmap
RDFCMap converts between CMap CXL and RDF TTL. 

Usage to convert from CXL to TTL
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar -i your-input-cmap-model.cxl

Usage to convert from TTL to CXL:
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar -i your-input-rdf-model.ttl

Note: Conversion from CXL to TTL supports vocabulary of AFO2.0, conversion from TTL to CXL needs major revision and is currently restricted to AFT1.1.5.  

Use --help to see command line options
"c:\path\to\jdk8\bin\java" -Dlog4j.configurationFile=resources/log4j2.xml -jar rdf-cmap.jar --help


How to create a cmap to be used with rdfcmap?

1) Get CMAP Tools from https://cmap.ihmc.us/
2) Create a new cmap following guidelines below on formatting 
a) All nodes of a cmap model are transformed to instances in rdf model
b) Labels of cmap nodes are transformed to titles of instances using dct:title 
c) Links between nodes in cmap are transformed to object properties in rdf model.
d) Labels of links must agree to existing labels (skos:prefLabel) of properties in RDF vocabulary.
e) Cardinalities can be added to object properties in cmap: default without specification of cardinality is "min 0". Exact cardinality is specified by adding the number, e.g. "has part =1" means there is exactly 1 link of "has part". Minimum cardinality is specified by ">1" (one or more)
3) Export cmap as CXL file

Copyright OSTHUS 2017


 