import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import br.ufba.dcc.wiser.fot.semantic.schema.FiestaIoT;
import br.ufba.dcc.wiser.fot.semantic.schema.SSN;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelFactoryBase;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class TripleStore {
	
	private String serverHost;
	private String cloudHost;
	private List<String> sensors;
	private HashMap<String, Literal> lastUploadedData;
	private String baseURI;
	private int minutesAggregation;
	
	public TripleStore(String serverHost, String cloudHost, String baseURI) {
		this.serverHost = serverHost;
		this.baseURI = baseURI;
		this.cloudHost = cloudHost;
		this.lastUploadedData = new HashMap<String, Literal>();
		loadSensors();
		startlastUploadedData();
	}
	
	
	private void loadSensors(){
		System.out.print("loading sensors...");
		this.sensors = new ArrayList<String>();
		String str = "SELECT DISTINCT ?sensor "
				+ "WHERE { "
				+ "?sensor <http://purl.oclc.org/NET/ssnx/ssn#madeObservation> ?obs . "
				+ "}";
		
		Query query = QueryFactory.create(str);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(this.serverHost, query);
		ResultSet result = qExe.execSelect();
		while(result.hasNext()){
	        QuerySolution row = result.nextSolution();
	        Resource resource = row.getResource("sensor");
	        this.sensors.add(resource.getURI());
	    }
		System.out.println("number of sensors: " + sensors.size());
	}
	
	private void startlastUploadedData(){
		
		System.out.print("loading time of last data of cloud...");
		for(String sensorURI : this.sensors ){
			String str = "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#> \n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ "SELECT ?endDateTime\n"
					+ "WHERE {  \n"
					+ "  <" + sensorURI + "> ssn:madeObservation ?obs .\n"
					+ "  ?obs ssn:observationResult ?sOutput .\n"
					+ "  ?sOutput ssn:hasValue ?obsValue .\n"
					+ "  ?obsValue <http://www.loa-cnr.it/ontologies/DUL.owl#hasDataValue> ?value .\n"
					+ "  ?obs ssn:startTime ?startTime .\n"
					+ "  ?obs ssn:endTime ?endTime .\n"
					+ "  ?startTime <http://www.loa-cnr.it/ontologies/DUL.owl#hasIntervalDate> ?startDateTime .\n"
					+ "  ?endTime <http://www.loa-cnr.it/ontologies/DUL.owl#hasIntervalDate> ?endDateTime .\n"
					+ "} ORDER BY DESC(?dateTime)\n"
					+ "LIMIT 1";
			Query query = QueryFactory.create(str);
			QueryExecution qExe = QueryExecutionFactory.sparqlService(this.cloudHost, query);
			ResultSet result = qExe.execSelect();
			if(result.hasNext()){
				Literal endDate = result.nextSolution().getLiteral("endDateTime");
				this.lastUploadedData.put(sensorURI, endDate);
			}
		}
		System.out.println("done");
	}

	public void buildAggregation(){
		System.out.println("Starting aggregation...");
		for(String sensorURI : this.sensors ){
			Literal referenceDateTime;
			if (this.lastUploadedData.containsKey(sensorURI)){
				referenceDateTime = this.lastUploadedData.get(sensorURI);
			}else{
				Model model = ModelFactory.createDefaultModel();
				Calendar calendar = new GregorianCalendar(1900,01,01);
				referenceDateTime = model.createTypedLiteral(new XSDDateTime(calendar),
						XSDDatatype.XSDdateTime);
			}
			
			String str = "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#> \n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ "SELECT ?startDateTime ?endDateTime ?value \n"
					+ "WHERE {  \n"
					+ "  <" + sensorURI + "> ssn:madeObservation ?obs .\n"
					+ "  ?obs ssn:observationResult ?sOutput .\n"
					+ "  ?sOutput ssn:hasValue ?obsValue .\n"
					+ "  ?obsValue <http://www.loa-cnr.it/ontologies/DUL.owl#hasDataValue> ?value .\n"
					+ "  ?obs ssn:startTime ?startTime .\n"
					+ "  ?obs ssn:endTime ?endTime .\n"
					+ "  ?startTime <http://www.loa-cnr.it/ontologies/DUL.owl#hasIntervalDate> ?startDateTime .\n"
					+ "  ?endTime <http://www.loa-cnr.it/ontologies/DUL.owl#hasIntervalDate> ?endDateTime .\n"
					+ "  FILTER ( ?startDateTime > \"" + referenceDateTime.getValue() +  "\"^^xsd:dateTime)\n"
					+ "} ORDER BY ?dateTime\n";
			System.out.println(str);
			Query query = QueryFactory.create(str);
			QueryExecution qExe = QueryExecutionFactory.sparqlService(this.serverHost, query);
			ResultSet result = qExe.execSelect();
			//ResultSetFormatter.outputAsTSV(System.out, result);
			System.out.println("getting results...");
			
			if(result.hasNext()){
				//Model model = ModelFactoryBase.createOntologyModel();
				Model model = ModelFactory.createDefaultModel();
				QuerySolution row = result.nextSolution();
				Literal beginDate = row.getLiteral("startDateTime");
				Literal endDate = row.getLiteral("endDateTime");
				Literal value = row.getLiteral("value");
				int count = 1;
				
				while(result.hasNext()){
					row = result.nextSolution();
					endDate = row.getLiteral("endDateTime");
					int newValue = value.getInt() + row.getLiteral("value").getInt();
					value = model.createTypedLiteral(newValue);
					count++;
				}
				if(sensorURI.contains("temperatureSensor")){
					value = model.createTypedLiteral(value.getInt()/count);
				}
				String sensorFullName = sensorURI.split("#")[1];
				model = buildFiestaIoTTriples(sensorFullName, beginDate, endDate, value);
				//updateTripleStore(model, this.cloudHost);
				model.write(System.out, "RDF/XML");
			}
		}
	}
	
	private Model buildFiestaIoTTriples(String sensorFullName, Literal lBeginDate, Literal lEndDate, Literal lValue){
		Model model = ModelFactory.createOntologyModel();
		
	
		long beginTimestamp = ((XSDDateTime) lBeginDate.getValue()).asCalendar().getTimeInMillis();
		long endTimestamp = ((XSDDateTime) lEndDate.getValue()).asCalendar().getTimeInMillis();
		
		Resource observationValue = model.createResource(this.baseURI
				+ "obsValue_" + beginTimestamp + endTimestamp, SSN.ObservationValue);
		
		observationValue.addLiteral(FiestaIoT.hasDataValue, lValue);
		
		Resource sensorOutput = model.createResource(this.baseURI
				+ "sensorOutput_" + beginTimestamp + endTimestamp, SSN.SensorOutput);
		sensorOutput.addProperty(SSN.hasValue, observationValue);
		
		Resource startTimeInterval = model.createResource(this.baseURI
				+ "startTimeInterval" + beginTimestamp + endTimestamp, FiestaIoT.classTimeInterval);
		Resource endTimeInterval = model.createResource(this.baseURI
				+ "endTimeInterval" + beginTimestamp + endTimestamp, FiestaIoT.classTimeInterval);
		
		startTimeInterval.addLiteral(FiestaIoT.hasIntervalDate, lBeginDate);
		
		endTimeInterval.addLiteral(FiestaIoT.hasIntervalDate, lEndDate);
		
		Resource observation = model.createResource(this.baseURI + "obs_" + beginTimestamp + endTimestamp, SSN.Observation);
		observation.addProperty(SSN.startTime, startTimeInterval);
		observation.addProperty(SSN.endTime, endTimeInterval);
		observation.addProperty(SSN.observationResult, sensorOutput);

		Resource sensor = model.createResource(SSN.NS + sensorFullName);
		sensor.addProperty(SSN.madeObservation, observation);
		
		return model;
	}
	
	private synchronized void updateTripleStore(Model model,
			String tripleStoreURI) {
		DatasetAccessor accessor = DatasetAccessorFactory
				.createHTTP(tripleStoreURI);
		accessor.add(model);

	}
}
