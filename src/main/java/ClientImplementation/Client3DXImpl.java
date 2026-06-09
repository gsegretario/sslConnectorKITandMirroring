package ClientImplementation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Base64;
import javax.net.ssl.SSLContext;

import org.json.JSONObject;
import org.slf4j.Logger;

public class Client3DXImpl {
	private final String STANDARD_PATH_TO_HOME_DIR =  "C:\\Comau\\Schedulatori\\SSLReaderMBOM\\conf";
	//private final String STANDARD_PATH_TO_HOME_DIR = "C:\\Users\\mpiperni\\eclipse-workspace\\sslConnectorClassifyReleaseMBOM\\home_3dExp"; 
	private final String STANDARD_PROPERTIES_FILENAME = "Client3dExperience.properties";
	
	public final String PATH_TO_HOME_DIR = "pathToHomeDir";
	public final String PROPERTIES_FILENAME = "propertiesFileName";
	public final String SECURITY_CONTEXT = "securityContext";
	public final String URL_3DPASSPORT = "url_3dPassport";
	public final String URL_3DSPACE = "url_3dSpace";
	public final String URL_CLM = "url_clm";
	public final String TENANT = "tenant";
	public final String CLM_AGENTID ="clm_agent_id";
	public final String CLM_INFO="clm_pwd";
	public final String CLM_INFO_ENCODED="clm_pwd_encoded";
	public final String URL_CREATEMBOM = "url_createMBOM";
	public final String USERNAME = "username";
	public final String PASSWORD = "password";

	private File homeDir;
	private Properties properties;
	private String propertiesFileName;
	private String url_createMBOM;
	private String clm_agent_id;
	private String clm_agent_pwd_urlencoded;
	private String clm_pwd;
	private String username;
	private String password;
	private String securityContext;
	
	@SuppressWarnings("unused")
	private String url_3dPassport;
	@SuppressWarnings("unused")
    private String url_3dSpace;
    private String url_clm;
    private String tenant;
  
    private String accessToken;
	
    private HttpClient client3dExperience;
    private String loginTicket;
    private String csfrToken;
    private Logger logger;
	
	public Client3DXImpl(final Logger _logger) {
		super();
		logger = _logger;
	}
	
	public void init(HashMap<String,String> hmParams) throws Exception {
		logger.debug("Client3dExperienceImpl init...");
		String pathToHomeDir = "";
		if(hmParams.get(PATH_TO_HOME_DIR)!=null && !hmParams.get(PATH_TO_HOME_DIR).isEmpty()) {
			pathToHomeDir = hmParams.get(PATH_TO_HOME_DIR);
		} else {
			pathToHomeDir = STANDARD_PATH_TO_HOME_DIR;
		}
		if(hmParams.get(PROPERTIES_FILENAME)!=null && !hmParams.get(PROPERTIES_FILENAME).isEmpty()) {
			this.propertiesFileName = hmParams.get(PROPERTIES_FILENAME);
		} else {
			this.propertiesFileName = STANDARD_PROPERTIES_FILENAME;
		}
		securityContext = hmParams.get(SECURITY_CONTEXT);
		logger.debug("PathToHomeDir: "+pathToHomeDir);
		homeDir = new File(pathToHomeDir);
		try {
			loadPropertiesFromClasses();
		} catch (Exception e) {
			loadPropertiesFromDir();
		}

		initialize(hmParams);
		
		logger.debug("Client3dExperienceImpl init Ok");
	}
	
	private void initialize(HashMap<String,String> hmParams) throws Exception {
		logger.debug("Client3dExperienceImpl initialize..");
		get3dExpURLFromProperties();
		if(properties.getProperty("usePropertiesForLogin")!=null && !properties.getProperty("usePropertiesForLogin").isEmpty() && Boolean.parseBoolean(properties.getProperty("usePropertiesForLogin"))) {
			//lettura file di properties
			getLoginParamsFromProperties();
		} else {
			throw new Exception(String.format("Properties file not found - %s", propertiesFileName));
		}
		checkParams();
		initClient();
		//commentato 20260303
		//createAccessToken();
		//commentato 20260303
		//getLoginTicket();
		//casAuthentication();
		getCSFRToken();
		logger.debug("Client3dExperienceImpl initialize OK");
	}
	
	private void getLoginTicket() throws Exception {
 		logger.debug("getLoginTicket...");		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dPassport+"/login?action=get_auth_params"))
				.header("Content-Type", "application/json")
				.build();
		HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
		logger.debug("getLoginTicket response: "+response);
		
		if (response.statusCode() != 200) {
			logger.debug("Error retrieving login ticket. Status "+response.statusCode()+" - message: "+response.body());
			logger.error("Error retrieving login ticket. Status "+response.statusCode()+" - message: "+response.body());
			throw new Exception("Error retrieving login ticket. Status "+response.statusCode()+" - message: "+response.body());
		} else {
			JSONObject jsonResponse = new JSONObject(response.body());
			loginTicket = jsonResponse.getString("lt");
			logger.debug("loginTicket: "+loginTicket);
		}
	}
	
	private HttpRequest.BodyPublisher ofFormData(Map<Object, Object> data) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Object, Object> entry : data.entrySet()) {
			if (builder.length() > 0) {
				builder.append("&");
			}
			if(entry.getKey().toString().equals("lt")) {
				builder.append(entry.getKey().toString());
				builder.append("=");
				builder.append(entry.getValue());
			} else {
				builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
				builder.append("=");
				builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
			}
		}
		return HttpRequest.BodyPublishers.ofString(builder.toString());
	}

	private void casAuthentication() throws Exception {
		logger.debug("casAuthentication...");		
		Map<Object, Object> data = new HashMap<>();
		data.put("lt", loginTicket);
		data.put("username", username);
		data.put("password", password);
		
		HttpRequest request = HttpRequest.newBuilder()
				.POST(ofFormData(data))
				.uri(URI.create(url_3dPassport+"/login?service="+URLEncoder.encode(url_3dSpace,StandardCharsets.UTF_8)+((tenant!=null && !tenant.isEmpty())?"?tenant="+tenant:"")))
				.header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.build();
		HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
		logger.debug("casAuthentication response: "+response);		
		if (response.statusCode() != 200) {
			logger.error("An error occurred calling Autenthicate. Response status: " + response.statusCode() + " - Body: " + response.body());
			throw new Exception("An error occurred calling Autenthicate. Response status: " + response.statusCode() + " - Body: " + response.body());
		} else {
			if(response.body().contains("error.authentication.credentials.bad")) {
				logger.error("An error occurred calling Autenthicate. Response status: " + response.statusCode() + " - Body: " + response.body());
				throw new Exception("An error occurred calling Autenthicate. Response status: " + response.statusCode() + " - Body: " + response.body());
			}
			logger.debug("casAuthentication END");
		}
	}
	
	private void initClient() throws Exception {
		
		//per gestire handshake
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, null, null);
		//instead of follow commented line
		//System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
		//.version(HttpClient.Version.HTTP_1_1)
		CookieHandler.setDefault(new CookieManager());
		client3dExperience = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMinutes(2))
				.cookieHandler(CookieHandler.getDefault())
				.followRedirects(HttpClient.Redirect.NORMAL)

				.sslContext(ctx)
    			//.version(HttpClient.Version.HTTP_1_1)
    			//.version(HttpClient.Version.HTTP_2)
				.build();
	}
	
	private void checkParams() throws Exception {
		checkLoginParams();
		checkURLParams();
	}
	
	public void setSecurityContext(String userSecurityContext) throws Exception {
		securityContext = userSecurityContext;
	}
	
	private void checkLoginParams() throws Exception {
		if(clm_agent_id==null || clm_agent_id.isEmpty()) {
			throw new Exception("Client initialization error: clm_agent_id cannot be null or empty");
		} else if(clm_pwd==null || clm_pwd.isEmpty()) {
			throw new Exception("Client initialization error: password cannot be null or empty");
		} else if(clm_agent_pwd_urlencoded==null || clm_agent_pwd_urlencoded.isEmpty()) {
			throw new Exception("Client initialization error: securityContext cannot be null or empty");
		} 	
		//url_createMBOM
	}
	
	private void checkURLParams() throws Exception {
		if(url_3dPassport==null || url_3dPassport.isEmpty()) {
			throw new Exception("Client initialization error: url_3dPassport cannot be null or empty");
		} else if(url_3dSpace==null || url_3dSpace.isEmpty()) {
			throw new Exception("Client initialization error: url_3dSpace cannot be null or empty");
		} else if(url_clm==null || url_clm.isEmpty()) {
			throw new Exception("Client initialization error: url_clm cannot be null or empty");
		}else if(url_createMBOM==null || url_createMBOM.isEmpty()) {
			throw new Exception("Client initialization error: url_createMBOM cannot be null or empty");
		}
	}
	
	private void loadPropertiesFromDir() throws Exception {
		File propertiesFile = new File(this.homeDir, this.propertiesFileName);
		if (!propertiesFile.exists()) {
			throw new Exception(String.format("Properties file not found - %s", propertiesFile));
		}
		properties = new Properties();
		properties.load(new FileReader(propertiesFile));
	}
	

	protected void loadPropertiesFromClasses() throws Exception {
		logger.debug("loadProperties Properties FileName: "+this.propertiesFileName);
		InputStream input = Client3DXImpl.class.getClassLoader().getResourceAsStream(this.propertiesFileName);
		properties = new Properties();
		properties.load(input);
	}
	
	private void get3dExpURLFromProperties() {
		//lettura file di properties
		url_3dPassport = properties.getProperty(URL_3DPASSPORT);
	    url_3dSpace = properties.getProperty(URL_3DSPACE);
	    url_clm = properties.getProperty(URL_CLM);
	    url_createMBOM = properties.getProperty(URL_CREATEMBOM);
	    tenant = properties.getProperty(TENANT);
	}
	
	private void getLoginParamsFromProperties() {
		//lettura file di properties
		clm_agent_id = properties.getProperty(CLM_AGENTID);
		clm_pwd = properties.getProperty(CLM_INFO);
		clm_agent_pwd_urlencoded = properties.getProperty(CLM_INFO_ENCODED);
		username = properties.getProperty(USERNAME);
		password = properties.getProperty(PASSWORD);
		//securityContext = properties.getProperty(SECURITY_CONTEXT);
	}
	
	public JSONObject createAccessToken() throws Exception {
		JSONObject requestBody = new JSONObject();
	    logger.info("create Access Token: "+requestBody+ "; url clm: "+url_clm);
	    HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_clm+"/clm/oauth2/token?client_id="+clm_agent_id+"&client_secret="+clm_agent_pwd_urlencoded+"&grant_type=client_credentials"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.build();
		HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
		logger.info("create Access Token response: "+response);
		if (response.statusCode() != 200) {
			logger.error("Error creating Token. Status "+response.statusCode()+" - message: "+response.body());
			throw new Exception("Error creating Token. Status "+response.statusCode()+" - message: "+response.body());
		} else {
			System.out.println("create Access Token responseBody: "+response.body());
			logger.debug("create Access Token responseBody: "+response.body());
			JSONObject jsonResponse = new JSONObject(response.body());
			accessToken = jsonResponse.getString("access_token");
			return jsonResponse;
		}
	}
	

/*public JSONObject createAccessToken() throws Exception {
    // Costruisci il body form-url-encoded
    String form = "grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8)
            + "&client_id=" + URLEncoder.encode(clm_agent_id, StandardCharsets.UTF_8)
            + "&client_secret=" + clm_agent_pwd_urlencoded;
            // Se hai già client_secret URL-encoded, NON ri-encodarlo

    // NIENTE parametri in query string; l’endpoint vuole il body form
    URI uri = URI.create(url_clm + "/clm/oauth2/token"); // es: https://OI000000677-eu1-clm.3dexperience.3ds.com

    HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            // .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((clm_agent_id + ":" + clm_agent_pwd).getBytes(StandardCharsets.UTF_8)))
            .build();

    HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());

    logger.debug("create Access Token response status: " + response.statusCode());
    logger.debug("create Access Token response body: " + response.body());

    if (response.statusCode() != 200) {
        throw new Exception("Error creating Token. Status " + response.statusCode() + " - message: " + response.body());
    }

    JSONObject json = new JSONObject(response.body());
    accessToken = json.getString("access_token");
    return json;
}*/

	
	private JSONObject generateBodyRequestMBOM(String EngItemId, String ManItemId) {

		JSONObject json = new JSONObject();
       	json.put("rootProductID", EngItemId);
       	json.put("productOccPath", "");
       	json.put("rootMfgItemID", ManItemId);
        json.put("engConfigFilter", "");
        json.put("mfgInstanceID", "");
        json.put("scopeCreation", "AllLevel");
        json.put("prefix", "");
        json.put("computeResultReport", false);
        json.put("isOpennessComputationRequired", true);
        json.put("topRootMfgItemPID", ManItemId);
        json.put("copyModelProductToItemUponSL", "DoNotCopyModel");
        json.put("copyModelParentToChild", false);
        json.put("xmlPhantomNodeConfig", JSONObject.NULL);
        
        StringBuilder xml = new StringBuilder();

        xml.append("<!--  Copyright DASSAULT SYSTEMES 2013  -->");
        xml.append("<LinkAuthoringConfig xmlns=\"DS_DELPPWConfiguration\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("version=\"2.0\" xsi:schemaLocation=\"DS_DELPPWConfiguration ../xsd/DELWebMfgAssetsLinkAuthoring.xsd\">");

        xml.append("<Link name=\"ProductImplementLinkOcc\">");
        xml.append("<Event name=\"Create\">");
        xml.append("<Action>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_ItemLevel\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_ItemLevel\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_ItemType\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_ItemType\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_COMAUCode\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Code\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Revision\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Revision\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_SparePartType\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_SparePartType\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_SparePartPriority\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_SparePartPriority\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_CustomerDrawingNumber\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_CustomerDrawingNumber\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_ItalianDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_ItalianDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_FrenchDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_FrenchDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_ChineseDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_ChineseDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_GermanDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_GermanDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_SpanishDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_SpanishDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_RomanianDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_RomanianDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_PortogueseDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_PortogueseDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("<CopyAttribute>");
        xml.append("<targetAttribute name=\"COMAU_Mfg_EnglishDescription\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>");
        xml.append("<sourceAttribute name=\"COMAU_Eng_EnglishDescription\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>");
        xml.append("</CopyAttribute>");

        xml.append("</Action>");
        xml.append("</Event>");
        xml.append("</Link>");
        xml.append("</LinkAuthoringConfig>");

        json.put("xmlLinkAuthoringConfig", xml.toString());
        //json.put("xmlLinkAuthoringConfig", "<!--  Copyright DASSAULT SYSTEMES 2013  -->"
        //    + "<LinkAuthoringConfig xmlns=\"DS_DELPPWConfiguration\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        //   + "version=\"2.0\" xsi:schemaLocation=\"DS_DELPPWConfiguration ../xsd/DELWebMfgAssetsLinkAuthoring.xsd\">"
        //   + "<Link name=\"ProductImplementLinkOcc\">"
        //   + "<Event name=\"Create\">"
        //   + "<Action>"
        //   + "<CopyAttribute>"
        //   + "<targetAttribute name=\"COMAU_Mfg_ItemLevel\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>"
        //   + "<sourceAttribute name=\"COMAU_Eng_ItemLevel\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>"
        //   + "</CopyAttribute>"
        //   + "<CopyAttribute>"
        //   + "<targetAttribute name=\"COMAU_Mfg_COMAUCode\" dicoType=\"XP_DELFmiFunctionPPRReference_Ext\" entity=\"reference\"/>"
        //   + "<sourceAttribute name=\"COMAU_Code\" dicoType=\"XP_VPMReference_Ext\" entity=\"reference\"/>"
        //   + "</CopyAttribute>"
        //   + "</Action>"
        //   + "</Event>"
        //  + "</Link>"
        //  + "</LinkAuthoringConfig>");       
        return json;
	}
	
	//Engineering
	public JSONObject expandEngineeringItem(String sPhysicalProductId, JSONObject requestBody) throws Exception {
	    logger.debug("expand Engineering Item request: " + requestBody);

	    // Costruzione della richiesta HTTP
	    HttpRequest request = HttpRequest.newBuilder()
	            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
	            .uri(URI.create(url_3dSpace + "/resources/v1/modeler/dseng/dseng:EngItem/" + sPhysicalProductId + "/expand"))
	            //.timeout(Duration.ofSeconds(30)) // timeout specifico per la singola chiamata
	            .header("Content-Type", "application/json")
	            .header("Accept", "application/json;charset=UTF-8")
	            .header("SecurityContext", URLEncoder.encode(securityContext, StandardCharsets.UTF_8))
	            .header("ENO_CSRF_TOKEN", csfrToken)
	            .build();

	    int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	            HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	            logger.debug("expand Engineering Item response (attempt " + (attempt + 1) + "): status=" + response.statusCode());

	            if (response.statusCode() == 200) {
	                logger.debug("expand Engineering Item responseBody: " + response.body());
	                return new JSONObject(response.body());
	            } else {
	                logger.error("Errore expand Engineering Item. Status " + response.statusCode() + " - message: " + response.body());
	                throw new RuntimeException("Errore expand Engineering Item. Status " + response.statusCode() + " - message: " + response.body());
	            }
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare expandEngineeringItem dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject searchEngineeringItem(String stringToSearch) throws Exception {
		logger.debug("searchEngineeringItem START on "+stringToSearch);		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dseng/dseng:EngItem/search?$searchStr="+stringToSearch+"&$mask=dsmveng:EngItemMask.Details"))
				.header("Accept", "application/json;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.build();
		
		int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("getEngineeringItem response: "+response);

	    		if (response.statusCode() != 200) {
	    			logger.error("Error retrieving the EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error retrieving the EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("searchEngineeringItem responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare searchEngineeringItem dopo " + maxRetries + " tentativi", lastException);
	}
	//Classification
	public JSONObject getWhereClassified(String engId) throws Exception {
		logger.debug("getWhereClassified START on "+engId);		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslib/dslib:ClassifiedItem/"+engId+"?$mask=dslib:ReverseClassificationMask"))
				.header("Accept", "application/json;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.build();
		int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("getWhereClassified response: "+response);

	    		if (response.statusCode() != 200) {
	    			logger.error("Error retrieving the WhereClassified. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error retrieving the WhereClassified. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("getWhereClassified responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare getWhereClassified dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject classifyObject(JSONObject requestBody)throws Exception{
		logger.debug("classify Object: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslib/dslib:ClassifiedItem"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.info("classify Object response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error classifing Object. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error classifing Object. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.info("classifing Object responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare classifing Object dopo " + maxRetries + " tentativi", lastException);
	}
	
	//Issue
	public JSONObject createIssue(JSONObject requestBody)throws Exception{
		logger.debug("create Issue: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsiss/issue"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("create Issue response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error creating Issue. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error creating Issue. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("creating Issue responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare createIssue dopo " + maxRetries + " tentativi", lastException);
	}
	

public JSONObject patchIssue(String issueId, JSONObject requestBody) throws Exception {
        logger.debug("Patch Issue [" + issueId + "]: " + requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .uri(URI.create(url_3dSpace + "/resources/v1/modeler/dsiss/issue/" + issueId))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json;charset=UTF-8")
                .header("SecurityContext", URLEncoder.encode(securityContext, StandardCharsets.UTF_8))
                .header("ENO_CSRF_TOKEN", csfrToken)
                .build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	            logger.debug("Patch Issue response: " + response);

	            if (response.statusCode() != 200) {
	                logger.error("Error patching Issue. Status " + response.statusCode() + " - message: " + response.body());
	                throw new Exception("Error patching Issue. Status " + response.statusCode() + " - message: " + response.body());
	            } else {
	                logger.debug("Patch Issue responseBody: " + response.body());
	                return new JSONObject(response.body());
	            }
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare patchIssue dopo " + maxRetries + " tentativi", lastException);
	}

	
	public JSONObject getIssueById(String issueId) throws Exception {
		logger.debug("getIssueById START on "+issueId);		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsiss/issue/"+issueId))
				.header("Accept", "application/json;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.build();
		int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("getIssueById response: "+response);

	    		if (response.statusCode() != 200) {
	    			logger.error("Error retrieving Issue by id. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error retrieving  Issue by id. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("get Issue by id responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare getIssueById dopo " + maxRetries + " tentativi", lastException);
	}
	
	//lifecycle
	public JSONObject promoteObject(JSONObject requestBody)throws Exception{
		logger.debug("promote Object: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslc/maturity/changeState"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("promote Object response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error promoting Object. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error promoting Object. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("promoting Object response: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare promoteObject dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject changeOwner(JSONObject requestBody)throws Exception{
		logger.debug("change Owner Object: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslc/ownership/transfer"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("change Owner Object response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error changing Owner Object. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error changing Owne Object. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("changing Owne Object response: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare changeOwner dopo " + maxRetries + " tentativi", lastException);
	}
	
	//change
	public JSONObject getChangeImpact(String engId) throws Exception {
		logger.debug("getChangeImpact START on "+engId);		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dseng/dseng:EngItem/"+engId+"/dslc:changeImpact"))
				.header("Accept", "application/json;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.build();
		int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("getChangeImpact response: "+response);

	    		if (response.statusCode() != 200) {
	    			logger.error("Error retrieving the getChangeImpact. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error retrieving the getChangeImpact. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("getChangeImpact responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare getChangeImpact dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject createChangeAction(JSONObject requestBody)throws Exception{
		logger.debug("create Change Action: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslc/changeaction"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("create Change Action response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error creating Change Action. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error creating Change Action. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("create Change Action responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare createChangeAction dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject patchChange(String changeId, JSONObject requestBody) throws Exception {
        logger.debug("Patch Change [" + changeId + "]: " + requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .uri(URI.create(url_3dSpace + "/resources/v1/modeler/dslc/changeaction/" + changeId))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json;charset=UTF-8")
                .header("SecurityContext", URLEncoder.encode(securityContext, StandardCharsets.UTF_8))
                .header("ENO_CSRF_TOKEN", csfrToken)
                .build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	            logger.debug("Patch Change response: " + response);

	            if (response.statusCode() != 200) {
	                logger.error("Error patching Change. Status " + response.statusCode() + " - message: " + response.body());
	                throw new Exception("Error patching Change. Status " + response.statusCode() + " - message: " + response.body());
	            } else {
	                logger.debug("Patch Change responseBody: " + response.body());
	                return new JSONObject(response.body());
	            }
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare patchChange dopo " + maxRetries + " tentativi", lastException);
	}
	
	 //Manufacturing
	public JSONObject expandManufacturingItem(String mfgId, JSONObject requestBody)throws Exception{
		logger.debug("expand Manufacturing Item: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+mfgId+"/expand"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("expand Manufacturing Item response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error Manufacturing Item. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error Manufacturing Item. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("expand Manufacturing Item responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare expandManufacturingItem dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject searchManItem(String stringToSearch) throws Exception {
		logger.debug("searchEngineeringItem START on "+stringToSearch);		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/search?$searchStr="+stringToSearch+"&$mask=dsmfg:MfgItemInstanceMask.Details"))
				.header("Accept", "application/json;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.build();
		int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("getEngineeringItem response: "+response);

	    		if (response.statusCode() != 200) {
	    			logger.error("Error retrieving the EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error retrieving the EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("searchEngineeringItem responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare searchManItem dopo " + maxRetries + " tentativi", lastException);
	}
	
	//private
	public JSONObject createMFGItemStructure(String EngItemId, String ManItemId)throws Exception{
		JSONObject requestBody = new JSONObject();
		requestBody = generateBodyRequestMBOM(EngItemId,ManItemId );
		logger.debug("creating MFG Item Structure: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_createMBOM))
				.header("Content-Type", "application/json")
				//.header("Authorization", "Bearer "+accessToken) 
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("create MFG Item Structure response: "+response);
	    		if (response.statusCode() != 200 && response.statusCode() != 202 && response.statusCode() != 405) {
	    			logger.error("Error create MFG Item Structure. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error create MFG Item Structure. Status "+response.statusCode()+" - message: "+response.body());
	    		}
	    		else if(response.statusCode() == 405) {
	    			JSONObject jsonResponse = createMFGItemStructure(EngItemId,ManItemId);		
	    			return jsonResponse;
	    		}
	    		else {
	    			logger.debug("create MFG Item Structure responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare createMFGItemStructure dopo " + maxRetries + " tentativi", lastException);
	}
	
	public JSONObject getEngineeringItemById(String engItemPhysicalId) throws Exception {
		logger.debug("getEngineeringItemById START on "+engItemPhysicalId);		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dseng/dseng:EngItem/"+engItemPhysicalId+"?$mask=dsmveng:EngItemMask.Details"))
				.header("Accept", "application/json;charset=UTF-8")
				//.header("Authorization", "Bearer "+accessToken)
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.build();
		int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("getEngineeringItem response: "+response);
	    		
	    		if (response.statusCode() != 200) {
	    			logger.error("Error retrieving the EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error retrieving the EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("getEngineeringItemById responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare getEngineeringItemById dopo " + maxRetries + " tentativi", lastException);
	}
	
    public JSONObject createNewManufacturingItem(JSONObject requestBody)throws Exception{
		logger.debug("creating bookmark: "+requestBody);
        HttpRequest request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json;charset=UTF-8")
				.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
				.header("ENO_CSRF_TOKEN",csfrToken)
				.build();
        int maxRetries = 3;
	    int attempt = 0;
	    Exception lastException = null;

	    while (attempt < maxRetries) {
	        try {
	        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	    		logger.debug("create manufacturing item response: "+response);
	    		if (response.statusCode() != 200) {
	    			logger.error("Error creating manufacturing item. Status "+response.statusCode()+" - message: "+response.body());
	    			throw new Exception("Error creating manufacturing item. Status "+response.statusCode()+" - message: "+response.body());
	    		} else {
	    			logger.debug("create manufacturing item responseBody: "+response.body());
	    			JSONObject jsonResponse = new JSONObject(response.body());
	    			return jsonResponse;
	    		}
	        } catch (IOException  e) {
	            lastException = e;
	            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
	            // backoff esponenziale: 1s, 2s, 4s...
	            long waitTime = (long) Math.pow(2, attempt) * 1000;
	            Thread.sleep(waitTime);
	        }
	        attempt++;
	    }

	    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
	    throw new Exception("Impossibile completare createNewManufacturingItem dopo " + maxRetries + " tentativi", lastException);
	}
    
	
	 public JSONObject createNewManufacturingItemInstance(String MFNfhaterID, JSONObject requestBody)throws Exception{
			logger.debug("creating bookmark: "+requestBody);
	        HttpRequest request = HttpRequest.newBuilder()
					.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+MFNfhaterID+"/dsmfg:MfgItemInstance"))
					.header("Content-Type", "application/json")
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.header("ENO_CSRF_TOKEN",csfrToken)
					.build();
	        int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("create manufacturing item response: "+response);
					if (response.statusCode() != 200) {
						logger.error("Error creating manufacturing item. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error creating manufacturing item. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("create manufacturing item responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare createNewManufacturingItemInstance dopo " + maxRetries + " tentativi", lastException);
		}
	    
	 
	 	
	 public JSONObject createNewManufacturingItemInstanceUnderChange(String MFNfhaterID, JSONObject requestBody, String changeId)throws Exception{
			logger.debug("creating bookmark: "+requestBody);
	        HttpRequest request = HttpRequest.newBuilder()
					.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+MFNfhaterID+"/dsmfg:MfgItemInstance"))
					.header("Content-Type", "application/json")
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.header("ENO_CSRF_TOKEN",csfrToken)
					.header("DS-Change-Authoring-Context", "pid:"+changeId)
					.build();
	        int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("create manufacturing item response: "+response);
					if (response.statusCode() != 200) {
						logger.error("Error creating manufacturing item. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error creating manufacturing item. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("create manufacturing item responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare createNewManufacturingItemInstance dopo " + maxRetries + " tentativi", lastException);
		}
	 
	 public JSONObject patchModifyManufacturingUnderChange(String mfnId, JSONObject requestBody, String changeId) throws Exception {
	        logger.debug("Patch Issue [" + mfnId + "]: " + requestBody);

	        HttpRequest request = HttpRequest.newBuilder()
	                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
	                .uri(URI.create(url_3dSpace + "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/" + mfnId))
	                .header("Content-Type", "application/json")
	                .header("Accept", "application/json;charset=UTF-8")
	                .header("SecurityContext", URLEncoder.encode(securityContext, StandardCharsets.UTF_8))
	                .header("ENO_CSRF_TOKEN", csfrToken)
	                .header("DS-Change-Authoring-Context", "pid:"+changeId)
	                .build();
	        int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
		            logger.debug("Patch Issue response: " + response);

		            if (response.statusCode() != 200) {
		                logger.error("Error patching Issue. Status " + response.statusCode() + " - message: " + response.body());
		                throw new Exception("Error patching Issue. Status " + response.statusCode() + " - message: " + response.body());
		            } else {
		                logger.debug("Patch Issue responseBody: " + response.body());
		                return new JSONObject(response.body());
		            }
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare patchIssue dopo " + maxRetries + " tentativi", lastException);
		}
	 
	    public JSONObject getManufacturingItem(String id) throws Exception {
			// TODO Auto-generated method stub	
			logger.debug("getManufacturingItem...");		
			HttpRequest request = HttpRequest.newBuilder()
					.GET()
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+id+"?$mask=dsmfg:MfgItemMask.Details&$fields=dsmveno:CustomerAttributes"))
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.build();
			int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("getManufacturingItem response: "+response);
					
					if (response.statusCode() != 200) {
						logger.error("Error retrieving the ManufacturingItem. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error retrieving the ManufacturingItem. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("getManufacturingItem responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare getManufacturingItem dopo " + maxRetries + " tentativi", lastException);
		}
	    
	    private void getCSFRToken() throws Exception {
			logger.debug("getCSFRToken...");		
			HttpRequest request = HttpRequest.newBuilder()
					.GET()
					.uri(URI.create(url_3dSpace+"/resources/v1/application/CSRF"))
					//commentato 20260303 38ce7f09-a759-4d81-b410-b2f12b7dccf5:F9z73*k@y_J2~qePTx!zX1Ag
					//.header("Authorization", "Bearer "+accessToken)
					.header("Authorization", "Basic "+Base64.getEncoder().encodeToString((clm_agent_id + ":" + clm_pwd).getBytes(StandardCharsets.UTF_8)))
					//.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((clm_agent_id + ":" + clm_pwd).getBytes(StandardCharsets.UTF_8)))
					.build();
			HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
			logger.debug("getCSFRToken response: "+response);		
			if (response.statusCode() != 200) {
				logger.error("Error retrieving CSFR token. Status "+response.statusCode()+" - message: "+response.body());
				throw new Exception("Error retrieving CSFR token. Status "+response.statusCode()+" - message: "+response.body());
			} else {			
				JSONObject jsonResponse = new JSONObject(response.body());
				csfrToken = jsonResponse.getJSONObject("csrf").getString("value");
				logger.debug("CSFRToken: "+csfrToken);
			}
		}
	    
	    public JSONObject getManufacturingItemInstance(String id) throws Exception {
			// TODO Auto-generated method stub	
			logger.debug("getManufacturingItemInstance...");		
			HttpRequest request = HttpRequest.newBuilder()
					.GET()
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+id+"/dsmfg:MfgItemInstance?$mask=dsmfg:MfgItemInstanceMask.Details"))
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.build();
			int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("getManufacturingItemInstance response: "+response);
					
					if (response.statusCode() != 200) {
						logger.error("Error retrieving the getManufacturingItemInstance. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error retrieving the getManufacturingItemInstance. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("getManufacturingItemInstance responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare getManufacturingItemInstance dopo " + maxRetries + " tentativi", lastException);
		}
			
	    
	    public JSONObject getScopedManufacturingItem(List<String> listPhysicalProductID)throws Exception{
			logger.debug("get scoped manufacturing Item: "+listPhysicalProductID);
	        HttpRequest request = HttpRequest.newBuilder()
					.POST(HttpRequest.BodyPublishers.ofString(listPhysicalProductID.toString()))
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/invoke/dsmfg:getMfgItemsFromEngItem"))
					.header("Content-Type", "application/json")
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.header("ENO_CSRF_TOKEN",csfrToken)
					.build();
	        int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("get scoped manufacturing Item: "+response);
					if (response.statusCode() != 200) {
						logger.error("Error get scoped manufacturing Item. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error get scoped manufacturing Item. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("get scoped manufacturing Item responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare getScopedManufacturingItem dopo " + maxRetries + " tentativi", lastException);
		}
	    
	    public JSONObject getScopedEngItem(String mfnId) throws Exception {
			logger.debug("get scoped EngineeringItem START on "+mfnId);		
			HttpRequest request = HttpRequest.newBuilder()
					.GET()
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+mfnId+"/dsmfg:ScopeEngItem?&mask=dsmfg:ScopeEngItemMask.Default"))
					.header("Accept", "application/json;charset=UTF-8")
					//.header("Authorization", "Bearer "+accessToken)
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.build();
			int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.info("get scoped EngineeringItem response: "+response);

					if (response.statusCode() != 200) {
						logger.error("Error retrieving the scoped EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error retrieving the scoped EngineeringItem. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("get scoped EngineeringItem responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + "getScopedEngItem fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare getScopedEngItem dopo " + maxRetries + " tentativi", lastException);
		}
			
	    
	    public JSONObject createScopeLink(String manufacturingItemID, JSONObject requestBody)throws Exception{
			logger.debug("get scoped manufacturing Item: "+requestBody);
	        HttpRequest request = HttpRequest.newBuilder()
					.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dsmfg/dsmfg:MfgItem/"+manufacturingItemID+"/dsmfg:ScopeEngItem/attach"))
					.header("Content-Type", "application/json")
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.header("ENO_CSRF_TOKEN",csfrToken)
					.build();
	        int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("get scoped manufacturing Item: "+response);
					if (response.statusCode() != 200) {
						logger.error("Error get scoped manufacturing Item. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error get scoped manufacturing Item. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("get scoped manufacturing Item responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare createScopeLink dopo " + maxRetries + " tentativi", lastException);
		}
	    
	    public JSONObject getChangeActionById(String changeActionId)throws Exception{
			logger.debug("getChangeActionById START on "+changeActionId);
			HttpRequest request = HttpRequest.newBuilder()
					.GET()
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslc/changeaction/"+changeActionId+"?$fields=members,proposedChanges,realizedChanges,referentials,contexts,flowDown,customerAttributes"))
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.build();
			int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
					logger.debug("getChangeActionById response: "+response);
					if (response.statusCode() != 200) {
						logger.error("Error retrieving the ChangeAction. Status "+response.statusCode()+" - message: "+response.body());
						throw new Exception("Error retrieving the ChangeAction. Status "+response.statusCode()+" - message: "+response.body());
					} else {
						logger.debug("getChangeActionById responseBody: "+response.body());
						JSONObject jsonResponse = new JSONObject(response.body());
						return jsonResponse;
					}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare getChangeActionById dopo " + maxRetries + " tentativi", lastException);
		}
	    
	    
	    
	    public JSONObject approveChangeActionById(String changeActionId, JSONObject requestBody) throws Exception {
	        logger.debug("approveChangeActionById START on {}", changeActionId);

	        HttpRequest request = HttpRequest.newBuilder()
	                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString())) // metodo PUT con body JSON
	                .uri(URI.create(url_3dSpace + "/resources/v1/modeler/dslc/changeaction/" + changeActionId+"/approve"))
	                //.timeout(Duration.ofSeconds(30)) // timeout per la singola chiamata
	                .header("Content-Type", "application/json")
	                .header("Accept", "application/json;charset=UTF-8")
	                .header("SecurityContext", URLEncoder.encode(securityContext, StandardCharsets.UTF_8))
	                .header("ENO_CSRF_TOKEN", csfrToken) // se richiesto dal server
	                .build();

	        int maxRetries = 3;
	        int attempt = 0;
	        Exception lastException = null;

	        while (attempt < maxRetries) {
	            try {
	                HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
	                logger.debug("approveChangeActionById response (attempt {}): status={}", attempt + 1, response.statusCode());

	                if (response.statusCode() == 200 || response.statusCode() == 204) {
	                    // 200 OK con body oppure 204 No Content
	                    if (response.body() != null && !response.body().isEmpty()) {
	                        logger.debug("approveChangeActionById response body: {}", response.body());
	                        return new JSONObject(response.body());
	                    } else {
	                        // Se non c’è body ma la chiamata è OK
	                        return new JSONObject();
	                    }
	                } else {
	                    String errorMsg = String.format("Errore approveChangeActionById. Status %d - message: %s",
	                                                    response.statusCode(), response.body());
	                    logger.error(errorMsg);
	                    throw new RuntimeException(errorMsg);
	                }
	            } catch (IOException e) {
	                lastException = e;
	                logger.warn("Tentativo {} fallito: {}", attempt + 1, e.getMessage());
	                long waitTime = (long) Math.pow(2, attempt) * 1000; // backoff esponenziale
	                Thread.sleep(waitTime);
	            }
	            attempt++;
	        }

	        throw new Exception("Impossibile completare approveChangeActionById dopo " + maxRetries + " tentativi", lastException);
	    }
		
	    
	    public JSONObject getRevisionsPath(JSONObject requestBody)throws Exception{
			logger.debug("get Revisions Path: "+requestBody);
	        HttpRequest request = HttpRequest.newBuilder()
					.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
					.uri(URI.create(url_3dSpace+"/resources/v1/modeler/dslc/version/getGraph"))
					.header("Content-Type", "application/json")
					.header("Accept", "application/json;charset=UTF-8")
					.header("SecurityContext", URLEncoder.encode(securityContext,StandardCharsets.UTF_8))
					.header("ENO_CSRF_TOKEN",csfrToken)
					.build();
	        int maxRetries = 3;
		    int attempt = 0;
		    Exception lastException = null;

		    while (attempt < maxRetries) {
		        try {
		        	HttpResponse<String> response = client3dExperience.send(request, HttpResponse.BodyHandlers.ofString());
		    		logger.debug("get Revisions Path response: "+response);
		    		if (response.statusCode() != 200) {
		    			logger.error("get Revisions Path. Status "+response.statusCode()+" - message: "+response.body());
		    			throw new Exception("get Revisions Path. Status "+response.statusCode()+" - message: "+response.body());
		    		} else {
		    			logger.debug("get Revisions Path responseBody: "+response.body());
		    			JSONObject jsonResponse = new JSONObject(response.body());
		    			return jsonResponse;
		    		}
		        } catch (IOException  e) {
		            lastException = e;
		            logger.warn("Tentativo " + (attempt + 1) + " fallito: " + e.getMessage());
		            // backoff esponenziale: 1s, 2s, 4s...
		            long waitTime = (long) Math.pow(2, attempt) * 1000;
		            Thread.sleep(waitTime);
		        }
		        attempt++;
		    }

		    // Se tutti i tentativi falliscono, rilancio l'ultima eccezione
		    throw new Exception("Impossibile completare get Revisions Path dopo " + maxRetries + " tentativi", lastException);
		}
}
