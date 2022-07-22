package org.hadatac.entity.pojo;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.hadatac.utils.CollectionUtil;
import org.hadatac.utils.FirstLabel;
import org.hadatac.utils.NameSpaces;
import org.hadatac.vocabularies.HASCO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VariableSpec extends HADatAcThing {

	private static final String className = "hasco:VariableSpec";

	private static final Logger log = LoggerFactory.getLogger(VariableSpec.class);
	public static final String LABEL_SEPARATOR = "|";
	public static final String VARIABLE_SEPARATOR = ";";
	public static final String VARIABLE_EMPTY_LABEL = "**";
	public static final String HIERARCHICAL_FACET_SEPARATOR = ",";
	public static final String EMPTY_CONTENT = "n/a";

	// patch for multi-valued attributes
	private static final String[] multiAttributeTag = { "Z-Score", "T-Score", "standard score", "Age Equivalent" };

	// Mandatory properties for Variable
	private String name = null;
	private Entity ent;
    private String role;
    private List<Attribute> attrList;

    // Optional properties for Variables
    private Entity inRelationTo;
    private Unit unit;
    private Attribute timeAttr;
    private List<PossibleValue> codebook;
    private boolean isCategorical;

	// study_uri_str,role_str,entity_uri_str,dasa_uri_str,in_relation_to_uri_str,named_time_str
    public enum SolrPivotFacet {
    	STUDY_URI_STR(0),
		ROLE_STR(1),
		ENTITY_URI_STR(2),
		DASA_URI_STR(3),
		IN_RELATION_TO_URI_STR(4),
		NAMED_TIME_STR(5);

    	private final int value;

		SolrPivotFacet(int value) {
			this.value = value;
		}
	}

	public VariableSpec() {
    	this.typeUri = HASCO.VARIABLE_SPEC;
    	this.hascoTypeUri = HASCO.VARIABLE_SPEC;
	}

    public VariableSpec(AlignmentEntityRole entRole, AttributeInRelationTo attrInRel) {
    	this(entRole, attrInRel, null, null);
    }

    public VariableSpec(AlignmentEntityRole entRole, AttributeInRelationTo attrInRel, Unit unit) {
    	this(entRole, attrInRel, unit, null);
    }

    public VariableSpec(AlignmentEntityRole entRole, AttributeInRelationTo attrInRel, Unit unit, Attribute timeAttr) {
		this.typeUri = HASCO.VARIABLE_SPEC;
		this.hascoTypeUri = HASCO.VARIABLE_SPEC;
    	this.ent = entRole.getEntity();
    	this.role = entRole.getRole();
    	this.attrList = attrInRel.getAttributeList();
    	this.inRelationTo = attrInRel.getInRelationTo();
    	this.unit = unit;
    	this.timeAttr = timeAttr;
    	this.isCategorical = false;
    }

	public VariableSpec(List<VariableSpec> sourceList) {
		if (sourceList != null && sourceList.get(0) != null) {
			this.setEntity(sourceList.get(0).getEntity());
			this.setRole(sourceList.get(0).getRole());
			this.setAttributeList(sourceList.get(0).getAttributeList());
			this.setInRelationTo(sourceList.get(0).getInRelationTo());
			this.setUnit(sourceList.get(0).getUnit());
			this.setTime(sourceList.get(0).getTime());
		}
	}

	public VariableSpec(VariableSpec variableSpec) {
		this.ent = variableSpec.getEntity();
		this.role = variableSpec.getRole();
		this.attrList = variableSpec.getAttributeList();
		this.inRelationTo = variableSpec.getInRelationTo();
		this.unit = variableSpec.getUnit();
		this.timeAttr = variableSpec.getTime();
		this.isCategorical = variableSpec.isCategorical();
	}

	public String getKey() {
    	return getRole() + getEntityStr() + getAttributeListStr() + getInRelationToStr() + getUnitStr() + getTimeStr();
    }

	public String getName() {
		if (name == null) {
			return toString();
		}
		return name;
	}

	public void setName(String name) {
    	this.name = name;
	}

	public String getLabel() {
    	return this.name;
	}

	public Entity getEntity() {
    	return ent;
    }

    public String getEntityStr() {
        if (ent == null || ent.getUri() == null || ent.getUri().isEmpty()) { 
        	return "";
        }
    	return ent.getUri();
    }

    public void setEntity(Entity ent) {
    	this.ent = ent;
	}

    public String getRole() {
    	if (role == null) {
    		return "";
    	}
    	return role;
    }

	public void setRole(String role) {
		this.role = role;
	}

    public List<Attribute> getAttributeList() {
    	return attrList;
    }

	public void setAttributeList(List<Attribute> attrList) {
    	this.attrList = attrList;
	}

    public String getAttributeListStr() {
    	if (attrList == null || attrList.isEmpty()) {
    		return "";
    	}
    	String resp = "";
    	for (Attribute attr : attrList) {
    		if (attr != null && attr.getUri() != null && !attr.getUri().isEmpty()) { 
        		resp = resp + attr.getUri();
    		}
    	}
        return resp;
    }

    public Entity getInRelationTo() {
    	return inRelationTo;
    }

	public void setInRelationTo(Entity inRelationTo) {
    	this.inRelationTo = inRelationTo;
	}

	public String getInRelationToStr() {
        if (inRelationTo == null || inRelationTo.getUri() == null ||inRelationTo.getUri().isEmpty()) { 
            return "";
        }
    	return inRelationTo.getUri();
    }

    public Unit getUnit() {
    	return unit;
    }

	public void setUnit(Unit unit) {
    	this.unit = unit;
	}

	public String getUnitStr() {
        if (unit == null || unit.getUri() == null || unit.getUri().isEmpty()) { 
            return "";
        }
    	return unit.getUri();
    }

    public Attribute getTime() {
    	return timeAttr;
    }

	public void setTime(Attribute timeAttr) {
		this.timeAttr = timeAttr;
	}

	public String getTimeStr() {
        if (timeAttr == null || timeAttr.getUri() == null || timeAttr.getUri().isEmpty()) { 
            return "";
        }
    	return timeAttr.getUri();
    }

	public List<PossibleValue> getCodebook() {
		return codebook;
	}

	public void addPossibleValue(PossibleValue possibleValue) {
		if (possibleValue == null) {
			return;
		}
		if (codebook == null) {
			codebook = new ArrayList<PossibleValue>();
		}
		codebook.add(possibleValue);
		return;
	}

	public void setCodebook(List<PossibleValue> codebook) {
		this.codebook = codebook;
	}

	public boolean isCategorical() {
		return isCategorical;
	}

	public void setIsCategorical(boolean isCategorical) {
    	this.isCategorical = isCategorical;
	}

	public static String upperCase(String orig) {
    	String[] words = orig.split(" ");
    	StringBuffer sb = new StringBuffer();

    	for (int i = 0; i < words.length; i++) {
    		sb.append(Character.toUpperCase(words[i].charAt(0)))
    		.append(words[i].substring(1)).append(" ");
    	}          
    	return sb.toString().trim();
    }      

    public static String prep(String orig) {
    	String aux = upperCase(orig);
    	return aux.replaceAll(" ","-").replaceAll("[()]","");
    }

	public static List<String> retrieveStudySearchFacetResult(String studyUri) {

		// default the search to all studies
		String queryString = "*:*";
		// but update it if a specific study is given
		if ( studyUri != null && studyUri.length() > 0 ) {
			queryString = "study_uri_str:\"" + studyUri + "\"";
		}
		SolrQuery query = new SolrQuery();

		query.setQuery(queryString);
		query.setRows(0);
		query.setFacet(true);
		query.set("facet.pivot", "study_uri_str,role_str,entity_uri_str,dasa_uri_str,in_relation_to_uri_str,named_time_str");

		try {
			SolrClient solr = new HttpSolrClient.Builder(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();
			QueryResponse queryResponse = solr.query(query, SolrRequest.METHOD.POST);
			solr.close();
			NamedList<List<PivotField>> facetPivot = queryResponse.getFacetPivot();
			List<String> parsedPivotResult = parsePivotResult(facetPivot);
			log.info("\n\n\nStudy Search Update result:");
			parsedPivotResult.forEach((s) -> {
				log.info(s);
			});
			return parsedPivotResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
	}


	private static List<String> parsePivotResult(final NamedList<List<PivotField>> pivotEntryList) {
		final Set<String> outputItems = new HashSet<>();
		for (final Map.Entry<String, List<PivotField>> pivotEntry : pivotEntryList) {
			//log.debug("Key: " + pivotEntry.getKey());
			pivotEntry.getValue().forEach((pivotField) -> {
				renderOutput(new StringBuilder(), pivotField, outputItems);
			});
		}
		final List<String> output = new ArrayList<>(outputItems);
		Collections.sort(output);
		return output;
	}

	private static void renderOutput(final StringBuilder sb, final PivotField field, final Set<String> outputItems) {

		final String fieldValue = field.getValue() != null ? ((String) field.getValue()).trim() : null;
		final StringBuilder outputBuilder = new StringBuilder(sb);
		if (field.getPivot() != null) {
			if (outputBuilder.length() > 0) {
				outputBuilder.append(HIERARCHICAL_FACET_SEPARATOR);
			}
			outputBuilder.append(fieldValue != null && fieldValue.length() > 0 ? fieldValue : EMPTY_CONTENT);
			// outputItems.add(new StringBuilder(outputBuilder).append(" (").append(field.getCount()).append(")").toString());
			// outputItems.add(new StringBuilder(outputBuilder).toString());
			field.getPivot().forEach((subField) -> {
				renderOutput(outputBuilder, subField, outputItems);
			});
		} else {
			if (outputBuilder.length() > 0) {
				outputBuilder.append(HIERARCHICAL_FACET_SEPARATOR);
			}
			outputBuilder.append(fieldValue != null && fieldValue.length() > 0 ? fieldValue : EMPTY_CONTENT);
			//outputItems.add(outputBuilder.append(" (").append(field.getCount()).append(") END").toString());
			outputItems.add(outputBuilder.toString());
		}
	}

	static public boolean constainsStandardTestScore(List<Pair<String, String>> indicators) {
    	for ( Pair<String, String> pair: indicators ) {
			for ( int i = 0; i < multiAttributeTag.length; i++ ) {
				if ( multiAttributeTag[i].equalsIgnoreCase(FirstLabel.getPrettyLabel(pair.getLeft())) ) return true;
			}
		}
		return false;
	}

	static public String retrieveTestScoreLabel(List<Pair<String, String>> indicators) {
    	for ( Pair<String, String> pair: indicators ) {
			for ( int i = 0; i < multiAttributeTag.length; i++ ) {
				if ( multiAttributeTag[i].equalsIgnoreCase(FirstLabel.getPrettyLabel(pair.getLeft())) ) {
					return FirstLabel.getPrettyLabel(pair.getLeft());
				}
			}
		}
		return null;
	}

	/*
		Pair<String, String> would be something like this: <attributeUri, indicatorUri>
		for example, <http://purl.obolibrary.org/obo/CMO_0000105, http://purl.org/twc/HHEAR_00029>
	 */
	public static List<Pair<String, String>> computeIndicatorList(String variableUri) {

		String studyQueryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				"SELECT DISTINCT  ?attributeUri ?indicator " +
				"WHERE { \n" +
				"   <" + variableUri + "> (hasco:hasAttribute/rdf:rest*/rdf:first | hasco:hasEntity) | rdfs:subClassOf  | \n" +
				"   (<http://semanticscience.org/resource/SIO_000668> / hasco:hasEntity) ?attributeUri . \n" +
				"   ?attributeUri rdfs:subClassOf* ?indicator . \n" +
				"   { ?indicator rdfs:subClassOf hasco:SampleIndicator } UNION { ?indicator rdfs:subClassOf hasco:StudyIndicator } . \n" +
				"} \n";

		Set<String> seen = new HashSet<>();
		List<Pair<String, String>> labelPairs = new ArrayList<>();  // <attributeUri, indicatorUri> is the pair

		try {

			ResultSetRewindable resultsrw = SPARQLUtilsFacetSearch.select(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), studyQueryString);

			while (resultsrw.hasNext()) {

				QuerySolution soln = resultsrw.next();

				if (soln.contains("indicator") == false ) {
					log.warn("Study Search: this variable [" + variableUri + "], does not have a indicator URI");
					continue;
				}

				if ( soln.contains("attributeUri") == false ) {
					log.warn("Study Search: this variable [" + variableUri + "], does not have a attribute URI");
					continue;
				}

				final String attributeUri = soln.get("attributeUri").toString();
				final String indicatorUri = soln.get("indicator").toString();

				if ( !seen.contains(attributeUri.toLowerCase() + "-" + indicatorUri.toLowerCase()) ) {
					labelPairs.add(new Pair<String, String>(attributeUri, indicatorUri));
					seen.add(attributeUri.toLowerCase() + "-" + indicatorUri.toLowerCase().toLowerCase());
				}
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		return labelPairs;
	}

	public static String toString(String role, Entity ent, List<Attribute> attrList, Entity inRelationTo, Unit unit, Attribute timeAttr) {
		//System.out.println("[" + attr.getLabel() + "]");
		String str = "";
		if (role != null && !role.isEmpty()) {
			str += prep(role) + "-";
		}
		if (ent != null && ent.getLabel() != null && !ent.getLabel().isEmpty()) {
			str += prep(ent.getLabel());
		}
		if (attrList != null && attrList.size() > 0) {
			for (Attribute attr : attrList) {
				if (attr != null && attr.getLabel() != null && !attr.getLabel().isEmpty()) {
					str += "-" + prep(attr.getLabel());
				}
			}
		}
		if (inRelationTo != null && !inRelationTo.getLabel().isEmpty()) {
			str += "-" + prep(inRelationTo.getLabel());
		}
		if (unit != null && unit.getLabel() != null && !unit.getLabel().isEmpty()) {
			str += "-" + prep(unit.getLabel());
		}
		if (timeAttr != null && timeAttr.getLabel() != null && !timeAttr.getLabel().isEmpty()) {
			str += "-" + prep(timeAttr.getLabel());
		}
		return str;
	}

	public String toString() {
		return VariableSpec.toString(role, ent, attrList, inRelationTo, unit, timeAttr);
    }

	// getStudyVariables()
	// getStudyVariablesWithLabels(Study studyUri)

	@Override
	public boolean saveToSolr() {
		return true;
	}

	@Override
	public boolean saveToTripleStore() {
		return true;
	}

	@Override
	public int deleteFromSolr() {
		return 0;
	}

	@Override
	public void deleteFromTripleStore() {

	}

}
