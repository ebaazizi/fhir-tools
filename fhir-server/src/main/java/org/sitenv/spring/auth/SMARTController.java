package org.sitenv.spring.auth;

import com.google.inject.internal.util.Lists;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
/*
 * Provides list of metadata supported by Smart Launch app and
 * openid connect
 * 
 */
@Controller
@RequestMapping("/.well-known")
public class SMARTController extends HttpServlet {
	
	public static final String SMART_CONFIGURATION_URL = "smart-configuration";
	public static final String OPENID_CONFIGURATION_URL = "openid-configuration";
	public static final String AUTHORIZE_URL = "authorize";
	public static final String TOKEN_URL = "token";
	public static final String INTROSPECTION_URL = "introspect";
	

	private static final long serialVersionUID = 936659617930557226L;
	
	/** Provides metadata about SMART APP launch
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	
	@RequestMapping(value = "/" + SMART_CONFIGURATION_URL, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getWellKnowllSMATConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> wellKnownConfig= new HashMap<String, Object>();

        String baseUrl = Common.getBaseUrl(request);
        
        wellKnownConfig.put("authorization_endpoint", baseUrl + AUTHORIZE_URL);
        wellKnownConfig.put("token_endpoint", baseUrl + TOKEN_URL);
        wellKnownConfig.put("introspection_endpoint", baseUrl + INTROSPECTION_URL);

        String[] stringArrayMethods = { "client_secret_basic","client_secret_post" };
        String[] stringArrayScopes = { "openid", "profile","fhirUser","launch","launch/patient","launch/encounter","patient/*.*","user/*.*","offline_access"};
        String[] stringArrayResponce = {"code"};
        String[] stringArrayCapabilities = {"launch-ehr",
                "launch-standalone",
                "client-public",
                "client-confidential-symmetric",
                "context-passthrough-banner",
                "context-passthrough-style",
                "context-ehr-patient",
                "context-ehr-encounter",
                "context-standalone-patient",
                "context-standalone-encounter",
                "permission-offline",
                "permission-patient",
                "permission-user"};

        wellKnownConfig.put("token_endpoint_auth_methods_supported", stringArrayMethods);
        wellKnownConfig.put("scopes_supported", stringArrayScopes);
        wellKnownConfig.put("response_types_supported", stringArrayResponce);

        wellKnownConfig.put("capabilities", stringArrayCapabilities);

        return wellKnownConfig;

    }
    
	/** Provides matadata about openid configuration
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	
    @RequestMapping(value = "/" + OPENID_CONFIGURATION_URL, method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getAuthorization(HttpServletRequest request, HttpServletResponse response){
	
		String baseUrl = Common.getBaseUrl(request);

		Map<String, Object> m = new HashMap<>();
		m.put("issuer", baseUrl);
		m.put("authorization_endpoint", baseUrl + AUTHORIZE_URL);
		m.put("token_endpoint", baseUrl + TOKEN_URL);
		m.put("introspection_endpoint", baseUrl + INTROSPECTION_URL);
		
	    //m.put("userinfo_endpoint", baseUrl + UserInfoEndpoint.URL);
		m.put("jwks_uri", baseUrl+ "jwk" );
		m.put("response_types_supported", Lists.newArrayList("code")); 
		m.put("subject_types_supported", Lists.newArrayList("public"));
		m.put("claims_supported", Lists.newArrayList(
				"sub",
				"name",
				"profile",
				"email"
				));
		
		m.put("claims_parameter_supported", false);
		m.put("request_parameter_supported", false);
		m.put("request_uri_parameter_supported", true);
		m.put("require_request_uri_registration", false);
		return m;
	}
}
