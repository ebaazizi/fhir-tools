package org.sitenv.spring;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sitenv.spring.model.BulkDataOutput;
import org.sitenv.spring.model.BulkDataOutputInfo;
import org.sitenv.spring.model.DafBulkDataRequest;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.service.AsyncService;
import org.sitenv.spring.service.BulkDataRequestService;
import org.sitenv.spring.service.GroupService;
import org.sitenv.spring.util.CommonUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.qos.logback.classic.Logger;

@Controller
@RequestMapping("/bulkdata")
public class BulkDataRequestProvider {

	Logger log = (Logger) LoggerFactory.getLogger(BulkDataRequestProvider.class);

	private static int a=0;

	@Autowired
	private BulkDataRequestService bdrService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private AsyncService service;

	public void seta()
	{
		a+=1;
	}

	@RequestMapping(value = "/{requestId}", method = RequestMethod.GET)
	@ResponseBody
	public String getContentLocationResponse(@PathVariable Integer requestId, HttpServletRequest request,
			HttpServletResponse response) {

		String body = "";
		DafBulkDataRequest bdr = bdrService.getBulkDataRequestById(requestId);
		if (bdr != null) {

			if (bdr.getStatus().equalsIgnoreCase("In Progress")) {
				response.setHeader("X-Progress", "In Progress");
				response.setStatus(202);
			}

			if (bdr.getStatus().equalsIgnoreCase("Accepted")) {
				response.setHeader("X-Progress", "Accepted");
				response.setStatus(202);
			}
			if (bdr.getStatus().equalsIgnoreCase("Completed")) {

				BulkDataOutput bdo = new BulkDataOutput();

				GregorianCalendar cal = new GregorianCalendar();
				cal.setTime(new Date());
				cal.setTimeZone(TimeZone.getTimeZone("GMT"));
				cal.add(Calendar.DATE, 10);
				String dt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz").format(cal.getTime());
				bdo.setTransactionStartTime(dt);

				bdo.setRequest(bdr.getRequestResource());
				bdo.setSecure("false");

				String[] links = bdr.getFiles().split(",");
				StringBuilder linksHeader = new StringBuilder();
				String uri = request.getScheme() + "://" + request.getServerName()
				+ ("http".equals(request.getScheme()) && request.getServerPort() == 80
				|| "https".equals(request.getScheme()) && request.getServerPort() == 443 ? ""
						: ":" + request.getServerPort())
				+ request.getContextPath();

				for (int i = 0; i < links.length; i++) {

					if (links[i] != null && !links[i].equals("null")) {

						BulkDataOutputInfo bdoi = new BulkDataOutputInfo();

						String linkForBody = uri + "/bulkdata/download/" + bdr.getRequestId() + "/" + links[i];
						String l = "<" + uri + "/bulkdata/download/" + bdr.getRequestId() + "/" + links[i] + ">";
						bdoi.setUrl(linkForBody);
						bdo.add(bdoi);

						linksHeader.append(l);

						if (i < links.length - 1) {
							linksHeader.append(",");
						}
					}
				}
				Gson g = new Gson();
				body = g.toJson(bdo);

				response.setHeader("Link", linksHeader.toString());
			}
		} else {
			response.setStatus(404);
			throw new ResourceNotFoundException(
					"The requested Content-Location was not found. Please contact the Admin.");
		}

		return body;
	}

	@RequestMapping(value = "/download/{id}/{fileName:.+}", method = RequestMethod.GET)
	@ResponseBody
	public int downloadFile(@PathVariable Integer id, @PathVariable String fileName, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String contextPath = System.getProperty("catalina.base");
		String destDir = contextPath + "/bulk data/" + id + "/";
		return CommonUtil.downloadFIleByName(new File(destDir + fileName), response);

	}

	// delete content-location request
	@RequestMapping(value = "/{requestId}", method = RequestMethod.DELETE)
	@ResponseBody
	public void deleteContentRequest(@PathVariable Integer requestId, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Integer res = bdrService.deleteRequestById(requestId);

		// delete folder with files
		if (res > 0) {
			String contextPath = System.getProperty("catalina.base");
			String destDir = contextPath + "/bulk data/" + requestId + "/";
			File directory = new File(destDir);
			if (directory.exists()) {
				FileUtils.deleteDirectory(directory);
			}
		}
		log.info("Numer of records effected due to content-location delete : " + res +" for request id : "+requestId);

		response.setStatus(202);

	}

	@RequestMapping(value = "/load/request/{id}", method = RequestMethod.GET)
	@ResponseBody
	public void loadRequestById(@PathVariable Integer id, HttpServletRequest request, HttpServletResponse response)
			throws IOException, InterruptedException {

		DafBulkDataRequest bdr = bdrService.getBulkDataRequestById(id);

		processBulkDataRequest(bdr);

	}

	@Scheduled(cron = "*/5 * * * * ?")
	public void processBulkDataRequestSchedular() {

		System.out.println("Schedular checking for pending requests...!");

		List<DafBulkDataRequest> requests = bdrService.getBulkDataRequestsByProcessedFlag(false);
		try {
			for (DafBulkDataRequest bdr : requests) {

				long startTime = System.nanoTime();
				log.info("request with id : "+bdr.getRequestId() +" is processing...  start time: "+startTime);

				processBulkDataRequest(bdr);


				long endTime   = System.nanoTime();
				long totalTime = endTime - startTime;
				log.info("request with id : "+bdr.getRequestId() +" - processing completed. The total time is : '"+totalTime+"' in nano seconds");

			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void processBulkDataRequest(DafBulkDataRequest bdr) throws IOException, InterruptedException {



		List<Integer> patientList = null;
		Date start = null;
		if (bdr.getStart() != null) {
			DateDt dateDt = new DateDt();
			dateDt.setValueAsString(bdr.getStart());
			start = dateDt.getValue();
		}

		FhirContext ctx = new FhirContext().forDstu2();

		String contextPath = System.getProperty("catalina.base");
		File destDir = new File(contextPath + "/bulk data/" + bdr.getRequestId() + "/");
		bdr.setStatus("In Progress");
		bdr.setProcessedFlag(true);
		bdrService.saveBulkDataRequest(bdr);

		List<String> files = new ArrayList<String>();

		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		if (bdr.getResourceName() != null && bdr.getResourceName().equalsIgnoreCase("GROUP")) {

			DafGroup dafGroup = groupService.getGroupById(bdr.getResourceId());

			if (dafGroup != null) {
				patientList = new ArrayList<Integer>();
				JSONArray jsonArr = new JSONArray(dafGroup.getMember());
				for (int i = 0; i < jsonArr.length(); i++) {
					JSONObject jsonObj = jsonArr.getJSONObject(i);
					if (jsonObj.getJSONObject("entity") != null) {

						String referenceId = jsonObj.getJSONObject("entity").getString("reference").split("/")[1];
						Integer patientId = Integer.parseInt(referenceId);
						patientList.add(patientId);
					}
				}
			}

		}
		// Process Patient Bulk data request
		String type = bdr.getType();
		/*		String[] nameEx = {};
		if(type !=null)
		{
			nameEx = type.split(",");  // output is all necessart files
		}*/


		a = 0 ;
		if (type == null || Arrays.asList(type.split(",")).contains("Patient")) {

			Future<Long> patient = service.processPatientData(bdr, destDir, ctx, patientList, start);
			files.add("Patient.ndjson");
		}

		// Process AllergyIntolerance Bulk data request
		Future<Long> allergyintolerance = null;
		if (type == null || Arrays.asList(type.split(",")).contains("AllergyIntolerance")) {

			allergyintolerance = service.processAllergyIntoleranceData(bdr, destDir, ctx, patientList, start);
			files.add("AllergyIntolerance.ndjson");
		}

		// Process CarePlan Bulk data request
		Future<Long> careplan = null;
		if (type == null || Arrays.asList(type.split(",")).contains("CarePlan")) {

			careplan = service.processCarePlanData(bdr, destDir, ctx, patientList, start);
			files.add("CarePlan.ndjson");
		}

		// Process Condition Bulk data request
		Future<Long> condition = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Condition")) {

			condition = service.processConditionData(bdr, destDir, ctx, patientList, start);
			files.add("Condition.ndjson");
		}

		// Process Device Bulk data request
		Future<Long> device = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Device")) {

			device = service.processDeviceData(bdr, destDir, ctx, patientList, start);
			files.add("Device.ndjson");
		}

		// Process DiagnosticReport Bulk data request
		Future<Long> diagnosticreport = null;
		if (type == null || Arrays.asList(type.split(",")).contains("DiagnosticReport")) {

			diagnosticreport = service.processDiagnosticReportData(bdr, destDir, ctx, patientList, start);
			files.add("DiagnosticReport.ndjson");
		}

		// Process DocumentReference Bulk data request
		Future<Long> documentreference = null;
		if (type == null || Arrays.asList(type.split(",")).contains("DocumentReference")) {

			documentreference = service.processDocumentReferenceData(bdr, destDir, ctx, patientList, start);
			files.add("DocumentReference.ndjson");
		}

		// Process Goal Bulk data request
		Future<Long> goal = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Goal")) {

			goal = service.processGoalsData(bdr, destDir, ctx, patientList, start);
			files.add("Goal.ndjson");
		}

		// Process Immunization Bulk data request
		Future<Long> immunization = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Immunization")) {

			immunization = service.processImmunizationData(bdr, destDir, ctx, patientList, start);
			files.add("Immunization.ndjson");
		}

		// Process Location Bulk data request
		Future<Long> location = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Location")) {

			location = service.processLocationData(bdr, destDir, ctx, patientList, start);
			files.add("Location.ndjson");
		}

		// Process MedicationAdministration Bulk data request
		Future<Long> medicationadministration = null;
		if (type == null || Arrays.asList(type.split(",")).contains("MedicationAdministration")) {

			medicationadministration = service.processMedicationAdministrationData(bdr, destDir, ctx, patientList, start);
			files.add("MedicationAdministration.ndjson");
		}

		// Process MedicationDispense Bulk data request
		Future<Long> medicationdispense = null;
		if (type == null || Arrays.asList(type.split(",")).contains("MedicationDispense")) {

			medicationdispense = service.processMedicationDispenseData(bdr, destDir, ctx, patientList, start);
			files.add("MedicationDispense.ndjson");
		}

		// Process MedicationOrder Bulk data request
		Future<Long> medicationorder = null;
		if (type == null || Arrays.asList(type.split(",")).contains("MedicationOrder")) {

			medicationorder = service.processMedicationOrderData(bdr, destDir, ctx, patientList, start);
			files.add("MedicationOrder.ndjson");
		}

		// Process Medication Bulk data request
		Future<Long> medication = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Medication")) {

			medication = service.processMedicationData(bdr, destDir, ctx, patientList, start);
			files.add("Medication.ndjson");
		}

		// Process MedicationStatement Bulk data request
		Future<Long> medicationstatement = null;
		if (type == null || Arrays.asList(type.split(",")).contains("MedicationStatement")) {

			medicationstatement = service.processMedicationStatementData(bdr, destDir, ctx, patientList, start);
			files.add("MedicationStatement.ndjson");
		}

		// Process Observation Bulk data request
		Future<Long> observation = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Observation")) {

			observation = service.processObservationData(bdr, destDir, ctx, patientList, start);
			files.add("Observation.ndjson");
		}

		// Process Organization Bulk data request
		Future<Long> organization = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Organization")) {

			organization = service.processOrganizationData(bdr, destDir, ctx, patientList, start);
			files.add("Organization.ndjson");
		}

		// Process Procedure Bulk data request
		Future<Long> procedure = null;
		if (type == null || Arrays.asList(type.split(",")).contains("Procedure")) {

			procedure = service.processProcedureData(bdr, destDir, ctx, patientList, start);
			files.add("Procedure.ndjson");
		}

		//wait until async processes gets complete
		while(true)
		{
			if(a == files.size())
			{
				String strFiles = StringUtils.join(files, ',');
				bdr.setStatus("Completed");
				bdr.setFiles(strFiles);
				bdrService.saveBulkDataRequest(bdr);
				System.out.println("\n\n Task complete!.................. \n\n");
				log.info("request with id : " + bdr.getRequestId() + " - is processed. The  time is : '"
						+ System.nanoTime() + "' in nano seconds");
				break;

			}
			Thread.sleep(100);
		}
	}
}
