package redhat.com.openshiftapisusage.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ApplicationController {
	
	// RETURN JSON VARIABLES
	private final String CART_ID_STR = "CART_ID";
	private final String DEPLOYMENT_STATUS_STR = "DEPLOYMENT_STATUS";
	private final String DEPLOYMENT_REMARKS_STR = "DEPLOYMENT_REMARKS";
	private final String DEPLOYMENT_DATA_STR = "DEPLOYMENT_DATA";

	//private String OPENSHIFT_URL_STR = "https://192.168.99.101:8443";
	//private String URL_STR = "http://localhost/printjson";
	private String OPENSHIFT_URL_STR;
	private String MKT_URL_STR;
	private String TOKEN_STR;
	
	// JSON INPUT PARAMETERS
	private final String MEMORY_STR = "MEMORY";
	private final String INSTANCE_DATA_STR = "INSTANCE_DATA";
	private final String CPU_STR = "CPU";

	

	@RequestMapping(value = "/createproject", method = RequestMethod.POST)
	public ResponseEntity<String> createProject(@RequestBody Map<String, Object> payload) {
		OPENSHIFT_URL_STR = System.getenv("OPENSHIFT_URL_STR");
		MKT_URL_STR = System.getenv("MKT_PLACE_URL");
		TOKEN_STR = System.getenv("TOKEN");
		
		if (OPENSHIFT_URL_STR == null) {
			// throw error
			System.out.println("Openshift API Url is not set");
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Openshift API Url is not set as an env variable");
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}
		
		if (MKT_URL_STR == null) {
			// throw error
			System.out.println("Marketplace Url is not set");
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Marketplace Url is not set as an env variable");
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}
		
		if (TOKEN_STR == null) {
			// throw error
			System.out.println("TOKEN is not set");
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Openshift authentication token is not set");
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}
		
		// retrieve variables from payload
		
		//get CPU
		String cpulimit = (String) ((Map)payload.get(INSTANCE_DATA_STR)).get(CPU_STR);
		if (cpulimit == null) 
			cpulimit = "4";
		
		// get Memory
		String memorylimit = (String) ((Map)payload.get(INSTANCE_DATA_STR)).get(MEMORY_STR);
		if (memorylimit == null)
			memorylimit = "4G";
		
		// get APP CODE
		String appcode = (String) ((Map)((Map)payload.get("ADDITIONAL_DATA")).get("AppCode")).get("TRUE_VAL");
		
		// get LOB
		String lob = (String) ((Map)((Map)payload.get("ADDITIONAL_DATA")).get("LOB")).get("TRUE_VAL");
		
		// get CART_ID
		String cartid = (String) payload.get(CART_ID_STR);
		
		String projectname = appcode.concat("-").concat(lob);
		String quotaname = projectname.concat("quota");
		
		System.out.println("cpulimit == "+cpulimit);
		System.out.println("memorylimit == "+memorylimit);
		System.out.println("appcode == "+appcode);
		System.out.println("lob == "+lob);
		System.out.println("cartid == "+cartid);
		
		String unzipCommand = "unzip -o /deployments/openshift-apis-usage-0.0.1-SNAPSHOT.jar -d /tmp/openshift-apis-usage";
		try {
			callShellScript(unzipCommand);
		} catch (Exception e) {
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Unable to unzip contents in /tmp/openshift-apis-usage directory. ".concat(e.getMessage()));
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}

        //1. create projectrequest
		String projectRequestCommand = createProjectRequestString(TOKEN_STR, projectname, OPENSHIFT_URL_STR);
		
		try {
			callShellScript(projectRequestCommand);
		} catch (Exception e) {
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Unable to create project. ".concat(e.getMessage()));
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}
		
		//2. create resourcequota
		String quotaCommand = createQuotaString(TOKEN_STR, quotaname, projectname, cpulimit, memorylimit, OPENSHIFT_URL_STR);
		try {
			callShellScript(quotaCommand);
		} catch (Exception e) {
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Project has been created successfully. However, unable to create ResourceQuota. ".concat(e.getMessage()));
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}
		
		//3. update mktplace api
		try {
			updateMktplace(MKT_URL_STR, cartid, "Completed", "1st deployment", TOKEN_STR, projectname, quotaname);
		} catch (Exception e) {
			MultiValueMap<String, String> headers = new HttpHeaders();
			headers.add("Result", "Error");
			headers.add("msg", "Project and Quota has been created successfully. However, unable to update progress using MktPlace APIs. ".concat(e.getMessage()));
			ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return re;
		}	
		
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add("Result", "Success");
		headers.add("msg", "Project (".concat(projectname).concat(") and ResourceQuota (").concat(quotaname).concat(") have been created successfully"));
		ResponseEntity <String> re = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
		return re;
	}
	
	private String createProjectRequestString(String token, String projectname, String endpointurl) {
		StringBuilder sb = new StringBuilder();
		String command = sb.append("sh /tmp/openshift-apis-usage/BOOT-INF/classes/createProjectRequest.sh ").
				append(token).append(" ").
				append(projectname).append(" ").
				append(endpointurl).toString();
		return command.toString();
	}

	private String createQuotaString(String token, String quotaname, String projectname, String cpulimit,
			String memorylimit, String endpointurl) {
		StringBuilder sb = new StringBuilder();
		String command = sb.append("sh /tmp/openshift-apis-usage/BOOT-INF/classes/createResourceQuota.sh ").
				append(token).append(" ").
				append(quotaname).append(" ").
				append(projectname).append(" ").
				append(cpulimit).append(" ").
				append(memorylimit).append(" ").
				append(endpointurl).toString();
		return command;
	}
	
	@RequestMapping(value = "/printjson", method = RequestMethod.POST)
	public void printJson(@RequestBody Map<String, Object> payload) {
		System.out.println("CartId = " +payload.get(CART_ID_STR));
		System.out.println("Deployment Status = " + payload.get(DEPLOYMENT_STATUS_STR));
		System.out.println("Deployment Data: projectName = "+ ((Map)payload.get("DEPLOYMENT_DATA")).get("PROJECT_NAME"));
		System.out.println("Deployment Data: quotaName = "+ ((Map)payload.get("DEPLOYMENT_DATA")).get("QUOTA_NAME"));
	}

	
	private void callShellScript(String command) throws Exception {
		StringBuffer output = new StringBuffer();
		
		System.out.println("==Running following command ==\n"+command);
		
		Process p;
		p = Runtime.getRuntime().exec(command);
		p.waitFor();
		BufferedReader reader = 
                            new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line = "";			
		while ((line = reader.readLine())!= null) {
			output.append(line + "\n");
		}
		System.out.println("=======OUTPUT=====\n"+output.toString());
	}
	
	private void unzipFiles() {
		String command = "unzip -o /deployments/openshift-apis-usage-0.0.1-SNAPSHOT.jar -d /tmp/openshift-apis-usage";
		
		StringBuffer output = new StringBuffer();
		
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = 
                            new BufferedReader(new InputStreamReader(p.getInputStream()));

                        String line = "";			
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("=======OUTPUT=====\n"+output.toString());
	}

    
	private void updateMktplace (String url, String cartId, String deploymentStatus, 
				String deploymentRemarks, String token, String projectname, String quotaname) throws Exception{
		
		/*
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		RestTemplate restTemplate = new RestTemplate();
		
		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put(CART_ID_STR, cartId);
			requestJson.put(DEPLOYMENT_STATUS_STR, deploymentStatus);
			requestJson.put(DEPLOYMENT_REMARKS_STR, deploymentRemarks);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		HttpEntity<String> entity = new HttpEntity<String>(requestJson.toString(), headers);
		restTemplate.postForObject(url, entity, String.class);
		*/
		
		StringBuilder sb = new StringBuilder();
		String command = sb.append("sh /tmp/openshift-apis-usage/BOOT-INF/classes/callMktPlaceURL.sh ").
				append(token).append(" ").
				append(deploymentStatus).append(" ").
				append("\"".concat(deploymentRemarks).concat("\"")).append(" ").
				append(cartId).append(" ").
				append(projectname).append(" ").
				append(quotaname).append(" ").
				append(url).toString();
		callShellScript(command);
			
	}
	
	
	/*
	String command = "curl -k -X POST "+
            "-H \"Authorization: Bearer ikf2sTDGVSsQZFa2ZbwinQYbyjwCjAodJpLW4kxClmo\" "+
            "-H 'Accept: application/json' "+
            "-H 'Content-Type: application/json' " +
	        "-d '{ \"apiVersion\": \"v1\", \"kind\": \"ResourceQuota\", \"metadata\": { \"name\": \""+quotaname+"\", \"namespace\": \""+projectname+"\" }, \"spec\": {\"hard\": {\"cpu\": \""+cpucount+"\", \"memory\": "+memory+"}} }' " +
	        "https://192.168.99.101:8443/api/v1/namespaces/"+projectname+"/resourcequotas ";
	
	*/
	
	//String command = "sh /Users/kashukla/Downloads/software/openshift-apis-usage/createResourceQuota.sh "
	/*
	String command = new StringBuilder().append("curl -k -X POST -H Authorization: Bearer 1q87Ln0_0g3uEQrJKZEqXKZ4vWo6Ft8GJvUXXclUDY0 "
    		+ "-H Accept: application/json "
    		+ "-H Content-Type: application/json "
    		+ "-d { \"apiVersion\": \"v1\", "
    		     + "\"kind\": \"ResourceQuota\", "
    		     + "\"metadata\": "
    		            + "{ \"name\": \"teskapil2\", "
    		            + "\"namespace\": \"test1\" }, "
    		     + "\"spec\": "
    		            + "{ \"hard\": "
    		                + "{ \"cpu\": \"2\", "
    		                + "\"memory\": \"2G\" } "
    		            + "} "
    		     + "} "
    		+ "https://192.168.99.101:8443/api/v1/namespaces/test1/resourcequotas").toString();
    		*/
	
	//String command = "sh /deployments/run.sh";
	
	/*
	String command = "curl -k -X GET "+
	                   "-H 'Authorization: Bearer GUfgymyGU90mGok0KXG67oMSOqQsiuaTwc-EEnJMFq0' "+
			           "-H 'Accept: application/json' "+
	                   "-H 'Content-Type: application/json' " +
			           "https://192.168.99.101:8443/oapi/v1/";
	*/
	
	/*
	String command = "curl -k -X POST "+
               "-H \"Authorization: Bearer iGJUjqX80gtnDcyGh0C9NmWiruWyM747BxQxbW-VBIM\" "+
	           "-H 'Accept: application/json' "+
               "-H 'Content-Type: application/json' " +
	           "-d '{ \"apiVersion\": \"v1\", \"kind\": \"ProjectRequest\", \"metadata\": { \"name\": \""+projectname+"\" } }' " +
	           "https://192.168.99.101:8443/oapi/v1/projectrequests ";
	  */
	 
	/*
	String command = "curl -k -X POST "+
                     "-H \"Authorization: Bearer iGJUjqX80gtnDcyGh0C9NmWiruWyM747BxQxbW-VBIM\" "+
	                 "-H 'Accept: application/json' "+
                     "-H 'Content-Type: application/json' " +
	                 "-d '{ \"apiVersion\": \"v1\", \"kind\": \"ProjectRequest\", \"metadata\": { \"name\": \""+projectname+"\" } }' " +
	                 "https://192.168.99.101:8443/apis/project.openshift.io/v1/projectrequests ";
	*/
}
