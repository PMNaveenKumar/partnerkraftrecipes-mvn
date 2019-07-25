package com.skava.kraft.userv2.auth.firebase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.skava.kraft.userv2.KraftUserV2Util;
import com.skava.model.Response;
import com.skava.model.userv2.ComUserResponse;
import com.skava.model.userv2.FirebaseGetAccountInfoResponse;
import com.skava.model.userv2.FirebaseUserLoginResponse;
import com.skava.model.userv2.KraftUserConstants;
import com.skava.services.HttpClientService;
import com.skava.util.CastUtil;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.JSONUtils;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;

public class KraftFirebaseUtil {
	private static SkavaLogger logger = SkavaLoggerFactory.getLogger(KraftUserV2Util.class);
	private static final String FirebaseConfigProp = "firebase.configProperties";
	public static FirebaseGetAccountInfoResponse getAccountInfo(HttpServletRequest request,HttpClientService httpClientService, String idToken) {
		
		// Setting default to failure
		FirebaseGetAccountInfoResponse fireBaseResp = new FirebaseGetAccountInfoResponse();
		fireBaseResp.setResponseCode(Response.RESPONSE_FAILED);
		fireBaseResp.setResponseMessage(Response.RESPONSE_MSG_FAILURE);
		
		ObjectMapper objectMapper = getObjectMapper();

		try {
			// URL Params
			String getProfileURL = getFirebaseZkProperty("getProfileURL");
			String apikey = getFirebaseZkProperty("apiKey");
			if(getProfileURL!=null && apikey!=null)
			{
				HashMap<String, List<String>> params = new HashMap<String, List<String>>();
				params.put(KraftUserConstants.PARAM_API_KEY, (List) CastUtil.getArrayListForString(apikey));
				params.put(KraftUserConstants.PARAM_ID_TOKEN_VALUE, (List) CastUtil.getArrayListForString(idToken));
				
				JSONObject resp = KraftUserV2Util.getDataFromUrl(request, httpClientService, getProfileURL, null, params,"POST", null);
				
				if(resp != null && resp.has(KraftUserConstants.FIREBASE_ERROR))
				{
					JSONObject error = (JSONObject) resp.get(KraftUserConstants.FIREBASE_ERROR);
					fireBaseResp.setResponseCode(error.getInt(KraftUserConstants.FIREBASE_ERROR_CODE));
					fireBaseResp.setResponseMessage(error.getString(KraftUserConstants.FIREBASE_ERROR_MESSAGE));
				}
				else
				{
					if(resp != null)
					{
						fireBaseResp = objectMapper.readValue(resp.toString(), FirebaseGetAccountInfoResponse.class);
						fireBaseResp.setResponseCode(Response.RESPONSE_SUCCESS);
						fireBaseResp.setResponseMessage(Response.RESPONSE_MSG_SUCCESS);
					}
				}
				
				logger.info("Firebase response : " + resp.toString());
			}
			else 
			{
				logger.info("Zk property missing");
			}
		}
		/*
		 * catch(JSONException e) {
		 * logger.info("JSONException Error while parsing firebase response.",
		 * e.getMessage()); }
		 */
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return fireBaseResp;
	}

	public static FirebaseUserLoginResponse getUserLoginInfo(HttpServletRequest request,HttpClientService httpClientService, String emailId, String password) {
		
		FirebaseUserLoginResponse fireBaseResponse = null;
		ComUserResponse toRet = new ComUserResponse();
		ObjectMapper objectMapper = getObjectMapper();

		try {
			String userLoginURL = getFirebaseZkProperty("userLoginURL");
			String apikey = getFirebaseZkProperty("apiKey");
			if(userLoginURL!=null && apikey!=null)
			{
				//URL Params
				HashMap<String, List<String>> params = new HashMap<String, List<String>>();
				params.put(KraftUserConstants.PARAM_API_KEY, (List) CastUtil.getArrayListForString(apikey));
				params.put(KraftUserConstants.PARAM_EMAIL_ID, (List) CastUtil.getArrayListForString(emailId));
				params.put(KraftUserConstants.PARAM_PASSWORD, (List) CastUtil.getArrayListForString(password));
				params.put(KraftUserConstants.PARAM_RETURN_SECURE_TOKEN, (List) CastUtil.getArrayListForString("true"));
				
				JSONObject resp = KraftUserV2Util.getDataFromUrl(request, httpClientService, userLoginURL, null, params,"POST", null);
				if(resp != null && resp.has(KraftUserConstants.FIREBASE_ERROR))
				{
					JSONObject error = (JSONObject) resp.get(KraftUserConstants.FIREBASE_ERROR);
					fireBaseResponse.setResponseCode(error.getInt(KraftUserConstants.FIREBASE_ERROR_CODE));
					fireBaseResponse.setResponseMessage(error.getString(KraftUserConstants.FIREBASE_ERROR_MESSAGE));
				}
				else
				{
					if(resp != null)
					{
						fireBaseResponse = objectMapper.readValue(resp.toString(), FirebaseUserLoginResponse.class);
						fireBaseResponse.setResponseCode(Response.RESPONSE_SUCCESS);
						fireBaseResponse.setResponseMessage(Response.RESPONSE_MSG_SUCCESS);
					}
				}
				logger.info("Firebase response : " + resp.toString());
			}
			else 
			{
				logger.info("Zk property missing");
			}
		}	catch(Exception e) {
			  logger.info("JSONException Error while parsing firebase response.", e.getMessage()); 
		}
		return fireBaseResponse;
	}
	
	public static String getFirebaseZkProperty(String key)
	{
		JSONObject jsonObj = JSONUtils.getJSONObjectFromString(ReadUtil.getString(ConfigManagerInstance.get(FirebaseConfigProp), null), null);
		return  JSONUtils.safeGetStringValue(jsonObj, key, null);
	}
	
	private static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
		return objectMapper;
	}

}
