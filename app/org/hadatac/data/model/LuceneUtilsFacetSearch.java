package org.hadatac.data.model;

import com.typesafe.config.ConfigFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.hadatac.utils.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class LuceneUtilsFacetSearch {

    private static final Logger log = LoggerFactory.getLogger(LuceneUtilsFacetSearch.class);
    private static Set<String> visited = new HashSet<>();

    private static Model kgModel = null;
    private static Dataset dataset = null;
    public static int totalQueries = 0;
    public static int cachedQueries = 0;
    public static int freshCalls = 0;
    public static long totalTimeTaken = 0;

    private static ConcurrentHashMap<String, ResultSetRewindable> selectCache = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Model> describeCache = new ConcurrentHashMap<>();

    public static void clearCache() {
        selectCache.clear();
        describeCache.clear();
        visited.clear();
        totalQueries = 0;
        cachedQueries = 0;
        freshCalls = 0;
        totalTimeTaken = 0;
    }

    public static ResultSetRewindable select(String sparqlService, String queryString) {
        // System.out.println("\nqueryString: " + queryString + "\n");

        /*log.info("in SPARQLUtilsFacetSearch.select");
        if ( visited.contains(queryString) ) {
            log.info("encountered!!!!!!");
        } else {
            visited.add(queryString);
        }*/

        totalQueries ++;

        if ( "ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.inMemoryModel")) ) {

            // System.out.println("select: using in-memory model");

            if ( "ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.readOnlyMode")) ) {
                if (selectCache.containsKey(queryString)) {
                    cachedQueries++;
                    ResultSetRewindable resultSetRewindable = selectCache.get(queryString);
                    resultSetRewindable.reset(); // reset pointer back to the beginning of the result set
                    return resultSetRewindable;
                }
            }

            freshCalls++;
            long startTime = System.currentTimeMillis();
            Query query = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
            ResultSet results = qexec.execSelect();
            ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
            qexec.close();
            totalTimeTaken += (System.currentTimeMillis()-startTime);

            if ( "ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.readOnlyMode")) ) {
                selectCache.put(queryString,resultsrw);
            }
            return resultsrw;

        } else {

            if ("ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.readOnlyMode"))) {
                if (selectCache.containsKey(queryString)) {
                    ResultSetRewindable resultSetRewindable = selectCache.get(queryString);
                    resultSetRewindable.reset(); // reset pointer back to the beginning of the result set
                    return resultSetRewindable;
                }
            }

            try {
                // System.out.println("NOT using in-memory model");
                freshCalls++;
                long startTime = System.currentTimeMillis();
                Query query = QueryFactory.create(queryString);
                QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, query);
                ResultSet results = qexec.execSelect();
                ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
                qexec.close();
                totalTimeTaken += (System.currentTimeMillis()-startTime);

                if ("ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.readOnlyMode"))) {
                    selectCache.put(queryString, resultsrw);
                }
                return resultsrw;
            } catch (QueryParseException e) {
                System.out.println("[ERROR] queryString: " + queryString);
                throw e;
            }
        }
    }

    public static void reportStats() {

        if ( "ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.inMemoryModel")) ) {
            log.info("in-memory model. # of calls (fresh/total) = " + freshCalls + "/" + totalQueries + ", total time taken: " + totalTimeTaken
                    + ", ave. time taken: " + totalTimeTaken/freshCalls);
        } else {
            log.info("remote DB. # of calls (fresh/total) = " + freshCalls + "/" + totalQueries + ", total time taken: " + totalTimeTaken
                    + ", ave. time taken: " + totalTimeTaken/freshCalls);
        }
    }

    public static Model createInMemoryModel() {

        /*if ( kgModel != null && kgModel.size() > 0 ) {
            System.out.println("we have the in-memory model already, by-pass this!");
            return kgModel;
        }*/

        // final String queryString = "select * where { ?subject ?predicaet ?object .  } ";
        final String sparqlService = CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL);
        final String queryString = "CONSTRUCT { ?s ?p ?o  } where { ?s ?p ?o. }";

        try {
            Query query = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, query);
            kgModel = qexec.execConstruct();
        } catch (QueryParseException e) {
            System.out.println("[ERROR] queryString: " + queryString);
            throw e;
        }

        dataset = DatasetFactory.createTxnMem();
        // dataset = DatasetFactory.createGeneral();
        dataset.setDefaultModel(kgModel);
        // dataset = DatasetFactory.assemble(kgModel);
        return kgModel;
    }
}
