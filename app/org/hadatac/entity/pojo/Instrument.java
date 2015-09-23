package org.hadatac.entity.pojo;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hadatac.data.loader.util.Sparql;

import play.Play;

public class Instrument {
	private String uri;
	private String localName;
	private String label;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getLocalName() {
		return localName;
	}
	public void setLocalName(String localName) {
		this.localName = localName;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public static Instrument find(String uri) {
		Instrument instrument = null;
		Model model;
		Statement statement;
		RDFNode object;
		
		String queryString = "DESCRIBE <" + uri + ">";
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(
				Play.application().configuration().getString("hadatac.solr.triplestore") + "/store/sparql", query);
		model = qexec.execDescribe();
		
		instrument = new Instrument();
		StmtIterator stmtIterator = model.listStatements();
		
		while (stmtIterator.hasNext()) {
			statement = stmtIterator.next();
			object = statement.getObject();
			if (statement.getPredicate().getURI().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
				instrument.setLabel(object.asLiteral().getString());
			}
		}
		
		instrument.setUri(uri);
		
		return instrument;
	}
	
	public static Instrument find(HADataC hadatac) {
		Instrument instrument = null;
		
		String queryString = Sparql.prefix
				+ "SELECT ?instrument ?label WHERE {\n"
				+ "  <" + hadatac.getDeploymentUri() + "> hasneto:hasInstrument ?instrument .\n"
				+ "  OPTIONAL { ?instrument rdfs:label ?label . }\n"
				+ "}";
		
		Query query = QueryFactory.create(queryString);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(hadatac.getStaticMetadataSparqlURL(), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
		
		if (resultsrw.size() == 1) {
			QuerySolution soln = resultsrw.next();
			instrument = new Instrument();
			instrument.setLocalName(soln.getResource("instrument").getLocalName());
			instrument.setUri(soln.getResource("instrument").getURI());
			if (soln.getLiteral("label") != null) { instrument.setLabel(soln.getLiteral("label").getString()); }
			else { instrument.setLabel(soln.getResource("instrument").getLocalName()); }
		}
		
		return instrument;
	}
}