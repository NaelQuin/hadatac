package org.hadatac.entity.pojo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.riot.RiotNotFoundException;
import org.apache.jena.shared.NotFoundException;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hadatac.console.http.SPARQLUtils;
import org.hadatac.utils.CollectionUtil;
import org.hadatac.vocabularies.HASCO;
import org.hadatac.utils.NameSpaces;

public class NameSpace extends HADatAcThing {

    static String className = HASCO.MANAGED_ONTOLOGY;

    @Field("abbreviation")
    private String nsAbbrev = "";

    @Field("name_str")
    private String nsName = "";

    @Field("mime_type_str")
    private String nsType = "";

    @Field("url_str")
    private String nsURL = "";

    @Field("comment_str")
    private String nsComment = "";

    @Field("version_str")
    private String version = "";

    @Field("number_of_loaded_triples_int")
    private int numberOfLoadedTriples = 0;

    @Field("priority_int")
    private int priority = -1;

    public NameSpace () {
    }

    public NameSpace (String abbrev, String name, String type, String url, String comment, String version, int priority) {
        this.nsAbbrev = abbrev;
        this.nsName = name;
        this.nsType = type;
        this.nsURL = url;
        this.nsComment = comment;
        this.version = version;
        this.priority = priority;
    }

    public String getAbbreviation() {
        return nsAbbrev;
    }

    public void setAbbreviation(String abbrev) {
        nsAbbrev = abbrev;
    }

    @Override
    public String getLabel() {
        return nsAbbrev;
    }

    @Override
    public void setLabel(String abbrev) {
        nsAbbrev = abbrev;
    }

    public String getName() {
        return nsName;
    }

    public void setName(String name) {
        nsName = name;
        uri = name;
    }

    @Override
    public String getUri() {
        return nsName;
    }

    @Override
    public void setUri(String name) {
        nsName = name;
    }

    public String getMimeType() {
        return nsType;
    }

    public void setMimeType(String type) {
        nsType = type;
    }

    @Override
    public String getComment() {
        return nsComment;
    }

    @Override
    public void setComment(String comment) {
        nsComment = comment;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getURL() {
        return nsURL;
    }

    public void setURL(String url) {
        nsURL = url;
    }

    public int getNumberOfLoadedTriples() {
        return numberOfLoadedTriples;
    }

    public void setNumberOfLoadedTriples(int numberOfLoadedTriples) {
        this.numberOfLoadedTriples = numberOfLoadedTriples;
    }

    public String toString() {
        if (nsAbbrev == null) {
            return "null";
        }
        String showType = "null";
        if (nsType != null)
            showType = nsType;
        if (nsURL == null)
            return "<" + nsAbbrev + ":> " + nsName + " (" + showType + ", NO URL)";
        else
            return "<" + nsAbbrev + ":> " + nsName + " (" + showType + ", " + nsURL + ")";
    }

    public List<String> getOntologyURIs() {
        List<String> uris = new ArrayList<String>();
        try {
            String queryString = "SELECT ?uri \n"
                    + "FROM <" + getName() + "> \n"
                    + "WHERE { \n"
                    + " ?ont <http://www.w3.org/2000/01/rdf-schema#subClassOf>* <http://www.w3.org/2002/07/owl#Ontology> . \n"
                    + " ?uri a ?ont . \n"
                    + "} ";

            ResultSetRewindable resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(
                    CollectionUtil.Collection.METADATA_SPARQL), queryString);

            while(resultsrw.hasNext()){
                QuerySolution soln = resultsrw.next();
                uris.add(soln.getResource("uri").getURI());
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return uris;
    }


    public static List<NameSpace> findWithPages(int pageSize, int offset) {
        List<NameSpace> listOntologies = NameSpaces.getInstance().getOntologyList();
        if (listOntologies == null || pageSize < 1 || offset < 0) {
            return new ArrayList<NameSpace>();
        }
        return listOntologies.subList(offset, offset + pageSize - 1);
    }

    public static int getNumberOntologies() {
        List<NameSpace> listOntologies = NameSpaces.getInstance().getOntologyList();
        if (listOntologies == null) {
            return 0;
        }
        return listOntologies.size();
    }

    public static NameSpace find(String uri) {
        SolrQuery query = new SolrQuery();
        query.set("q", "name_str:\"" + uri + "\"");
        query.set("rows", "10");
        List<NameSpace> namespaces = findByQuery(query);
        if (namespaces.isEmpty()) {
            return null;
        }
        return namespaces.get(0);
    }

    public static NameSpace findByAbbreviation(String abbreviation) {
        SolrQuery query = new SolrQuery();
        query.set("q", "abbreviation:\"" + abbreviation + "\"");
        query.set("rows", "10");
        List<NameSpace> namespaces = findByQuery(query);
        if (namespaces.isEmpty()) {
            return null;
        }

        return namespaces.get(0);
    }

    public static List<NameSpace> findAll() {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("rows", "10000000");

        return findByQuery(query);
    }

    public static List<NameSpace> findByQuery(SolrQuery query) {
        List<NameSpace> list = new ArrayList<NameSpace>();

        SolrClient solr = new HttpSolrClient.Builder(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.NAMESPACE)).build();

        try {
            QueryResponse response = solr.query(query);
            solr.close();
            SolrDocumentList results = response.getResults();
            Iterator<SolrDocument> i = results.iterator();
            while (i.hasNext()) {
                list.add(convertFromSolr(i.next()));
            }
        } catch (Exception e) {
            list.clear();
            System.out.println("[ERROR] NameSpace.findByQuery(SolrQuery) - Exception message: " + e.getMessage());
        }

        return list;
    }

    private static NameSpace convertFromSolr(SolrDocument doc) {
        NameSpace object = new NameSpace();
        if (doc != null) {
            if (doc.getFieldValue("abbreviation") != null) {
                object.setAbbreviation(doc.getFieldValue("abbreviation").toString());
            }
            if (doc.getFieldValue("name_str") != null) {
                object.setName(doc.getFieldValue("name_str").toString());
            }
            if (doc.getFieldValue("mime_type_str") != null) {
                object.setMimeType(doc.getFieldValue("mime_type_str").toString());
            }
            if (doc.getFieldValue("url_str") != null) {
                object.setURL(doc.getFieldValue("url_str").toString());
            }
            if (doc.getFieldValue("comment_str") != null) {
                object.setComment(doc.getFieldValue("comment_str").toString());
            }
            if (doc.getFieldValue("number_of_loaded_triples_int") != null) {
                object.setNumberOfLoadedTriples(Integer.valueOf(doc.getFieldValue("number_of_loaded_triples_int").toString()).intValue());
            }
            if (doc.getFieldValue("priority_int") != null) {
                object.setPriority(Integer.valueOf(doc.getFieldValue("priority_int").toString()).intValue());
            }
            object.setTypeUri(HASCO.ONTOLOGY);
            object.setHascoTypeUri(HASCO.ONTOLOGY);
        }
        return object;
    }

    public void updateFromTripleStore() {
        OntologyTripleStore ont = OntologyTripleStore.find(this.getUri());
        this.setComment(ont.getComment());
        this.setVersion(ont.getVersion());
        this.save();
    }

    public void updateNumberOfLoadedTriples() {
        try {
            String queryString = "SELECT (COUNT(*) as ?tot) \n"
                    + "FROM <" + getName() + "> \n"
                    + "WHERE { ?s ?p ?o . } \n";

            ResultSetRewindable resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(
                    CollectionUtil.Collection.METADATA_SPARQL), queryString);
            QuerySolution soln = resultsrw.next();

            this.setNumberOfLoadedTriples(Integer.valueOf(soln.getLiteral("tot").getValue().toString()).intValue());
            this.save();
        } catch (Exception e) {
            System.out.println("NameSpace.updateLoadedTripleSize()");
 	        System.out.println("  - Value of CollectionUtil.Collection.METADATA_SPARQL=[" + CollectionUtil.Collection.METADATA_SPARQL + "]");
 	        System.out.println("  - Value of CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL)=[" + CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL) + "]");
            e.printStackTrace();
        }
    }

    public void loadTriples(String address, boolean fromRemote) {
        try {
            Repository repo = new SPARQLRepository(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_UPDATE));
            repo.init();
            RepositoryConnection con = repo.getConnection();
            ValueFactory factory = repo.getValueFactory();

            //System.out.println("Loading triples from " + address);
            if (fromRemote) {
                con.add(new URL(address), "", getRioFormat(getMimeType()), (Resource)factory.createIRI(getName()));
            } else {
                con.add(new File(address), "", getRioFormat(getMimeType()), (Resource)factory.createIRI(getName()));
            }
            //System.out.println("Loaded triples from " + address + " \n");
        } catch (NotFoundException e) {
            System.out.println("NotFoundException: address " + address);
            System.out.println("NotFoundException: " + e.getMessage());
        } catch (RiotNotFoundException e) {
            System.out.println("RiotNotFoundException: address " + address);
            System.out.println("RiotNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception: address " + address);
            System.out.println("Exception: " + e.getMessage());
        }
    }

    public void deleteTriples() {
        deleteTriplesByNamedGraph(getName());
    }

    public static void deleteTriplesByNamedGraph(String namedGraphUri) {
        if (!namedGraphUri.isEmpty()) {
            String queryString = "";
            queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
            queryString += "WITH <" + namedGraphUri + "> ";
            queryString += "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o . } ";

            UpdateRequest req = UpdateFactory.create(queryString);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(req,
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_UPDATE));
            try {
                processor.execute();
                DataAcquisitionSchema.resetCache();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void save() {
        this.saveToSolr();
    }

    @Override
    public boolean saveToSolr() {
        if (priority == -1) {
            System.out.println("Warning priority was never initialized through the NameSpace file");
        }
        try {
            SolrClient client = new HttpSolrClient.Builder(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.NAMESPACE)).build();

            int status = client.addBean(this).getStatus();
            client.commit();
            client.close();
            return (status == 0);
        } catch (IOException | SolrServerException e) {
            System.out.println("[ERROR] Namespace.save() - e.Message: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void delete() {
        this.deleteFromSolr();
    }

    @Override
    public int deleteFromSolr() {
        try {
            SolrClient client = new HttpSolrClient.Builder(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.NAMESPACE)).build();
            UpdateResponse response = client.deleteById(getAbbreviation());
            client.commit();
            client.close();
            return response.getStatus();
        } catch (SolrServerException e) {
            System.out.println("[ERROR] NameSpace.delete() - SolrServerException message: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[ERROR] NameSpace.delete() - IOException message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] NameSpace.delete() - Exception message: " + e.getMessage());
        }

        return -1;
    }

    public static int deleteAll() {
        try {
            SolrClient client = new HttpSolrClient.Builder(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.NAMESPACE)).build();
            UpdateResponse response = client.deleteByQuery("*:*");
            client.commit();
            client.close();
            return response.getStatus();
        } catch (SolrServerException e) {
            System.out.println("[ERROR] NameSpace.delete() - SolrServerException message: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[ERROR] NameSpace.delete() - IOException message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] NameSpace.delete() - Exception message: " + e.getMessage());
        }

        return -1;
    }
    public static RDFFormat getRioFormat(String contentType) {
        if (contentType.contains("turtle")) {
            return RDFFormat.TURTLE;
        } else if (contentType.contains("rdf+xml")) {
            return RDFFormat.RDFXML;
        } else {
            return RDFFormat.NTRIPLES;
        }
    }
}