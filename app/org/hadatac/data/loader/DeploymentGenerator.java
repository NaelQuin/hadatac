package org.hadatac.data.loader;

import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.RDFNode;
import org.hadatac.console.http.SPARQLUtils;
import org.hadatac.entity.pojo.DataFile;
import org.hadatac.metadata.loader.URIUtils;
import org.hadatac.utils.CollectionUtil;
import org.hadatac.utils.ConfigProp;
import org.hadatac.utils.NameSpaces;
import org.hadatac.utils.Templates;

public class DeploymentGenerator extends BaseGenerator {
	final String kbPrefix = ConfigProp.getKbPrefix();
	String startTime = "";

	public DeploymentGenerator(DataFile dataFile) {
		super(dataFile);
	}

	public DeploymentGenerator(DataFile dataFile, String startTime) {
		super(dataFile);
		this.startTime = startTime;
	}

	@Override
	public void initMapping() {
		mapCol.clear();
		mapCol.put("DataAcquisitionName", Templates.ACQ_DATAACQUISITIONNAME);
		mapCol.put("Method", Templates.ACQ_METHOD);
		mapCol.put("Study", Templates.ACQ_DASTUDYID);
		mapCol.put("Epi/Lab", Templates.ACQ_EPILAB);
	}

	private String getCohortAsPlatform(Record rec) {
		String cohort = "";
		String strQuery = NameSpaces.getInstance().printSparqlNameSpaceList() 
				+ " SELECT ?cohort WHERE { "
				+ " ?cohort hasco:isCohortOf " + getStudy(rec) + " . "
				+ " }";

		ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), strQuery);
		
		if (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			RDFNode node = soln.get("cohort");
			if (null != node) {
				cohort = URIUtils.replaceNameSpaceEx(node.toString());
			}
		}

		return cohort;
	}

	private String getSampleCollectionAsPlatform(Record rec) {
		String sampleCollection = "";
		String strQuery = NameSpaces.getInstance().printSparqlNameSpaceList() 
				+ " SELECT ?sampleCollection WHERE { "
				+ " ?sampleCollection hasco:isMemberOf " + getStudy(rec) + " . "
				+ " }";
		
		ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), strQuery);
		
		if (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			RDFNode node = soln.get("sampleCollection");
			if (null != node) {
				sampleCollection = URIUtils.replaceNameSpaceEx(node.toString());
			}
		}

		return sampleCollection;
	}

	private String getPlatform(Record rec) {
		String tempStr = "";
		if (isEpiData(rec)) {
			tempStr = getCohortAsPlatform(rec);
			if (tempStr == null || tempStr.equals("")) {
				tempStr = getSampleCollectionAsPlatform(rec);
			} 
			return tempStr;
		}
		else if (isLabData(rec)) {
			return getSampleCollectionAsPlatform(rec);
		}

		return "";
	}

	private String getInstrument(Record rec) {
		if (isEpiData(rec)) {
			// “generic questionnaire” is the instrument
			return kbPrefix + "INS-GENERIC-QUESTIONNAIRE";
		}
		else if (isLabData(rec)) {
			return kbPrefix + "INS-GENERIC-PHYSICAL-INSTRUMENT";
		}
		return "";
	}

	private String getDataAcquisitionName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("DataAcquisitionName"));
	}

	private String getMethod(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Method"));
	}

	private String getStudy(Record rec) {
		return kbPrefix + "STD-" + rec.getValueByColumnName(mapCol.get("Study"));
	}

	private String getDataDictionaryName(Record rec) {
		return isEpiData(rec)? rec.getValueByColumnName(mapCol.get("DataDictionaryName")) : "";
	}

	private Boolean isEpiData(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Epi/Lab")).equalsIgnoreCase("EPI");
	}

	private Boolean isLabData(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Epi/Lab")).equalsIgnoreCase("LAB");
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
		//System.out.println("inside DeploymentGenerator.createRow ");
		Map<String, Object> row = new HashMap<String, Object>();

		String tempDA = getDataAcquisitionName(rec);
		if (tempDA == null || tempDA.equals("")) {
			System.out.println("[ERROR] Deployment not created because of empty DA info."); 
			return null;
		}

		String tempDeployment = getPlatform(rec);
		if (tempDeployment == null || tempDeployment.equals("")) {
			System.out.println("[ERROR] Platform for " + tempDA + " is empty. Deployment was not created."); 
			return null;
		}

		String tempInstrument = getInstrument(rec);
		if (tempInstrument == null || tempInstrument.equals("")) {
			System.out.println("[ERROR] Instrument for " + tempDA + " is empty. Deployment was not created."); 
			return null;
		}

		row.put("hasURI", kbPrefix + "DPL-" + tempDA);
		row.put("a", "vstoi:Deployment");
		row.put("hasco:hascoType", "vstoi:Deployment");
		row.put("vstoi:hasPlatform", tempDeployment);
		row.put("hasco:hasInstrument", tempInstrument);
		if (startTime.isEmpty()) {
			row.put("prov:startedAtTime", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).format(new Date()));
		}
		else {
			row.put("prov:startedAtTime", startTime);
		}
		
		setStudyUri(URIUtils.replacePrefixEx(getStudy(rec)));

		return row;
	}

	@Override
	public String getTableName() {
		return "Deployment";
	}

	@Override
	public String getErrorMsg(Exception e) {
		return "Error in DeploymentGenerator: " + e.getMessage();
	}
}
