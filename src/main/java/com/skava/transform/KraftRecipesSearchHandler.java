package com.skava.transform;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.remote.interfaces.StreamComUserRemoteServiceBuilder;
import com.skava.interfaces.TransformAuthenticationHandler;
import com.skava.model.TenantThreadLocal;
import com.skava.model.com.request.SkavaCOMRequest;
import com.skava.model.com.request.SkavaCOMSmartZoneModel;
import com.skava.model.com.request.SkavaCOMUserModel;
import com.skava.model.com.request.SkavaCOMUserRequest;
import com.skava.model.http.SkavaHttpRequest;
import com.skava.model.http.SkavaHttpResponse;
import com.skava.model.transform.Type;
import com.skava.model.userv2.ComUserResponse;
import com.skava.services.SkavaTransformer;
import com.skava.services.StreamComUserService;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.CookieUtil;
import com.skava.util.JSONUtils;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.URLUtil;

import lombok.Getter;
import lombok.Setter;

public class KraftRecipesSearchHandler implements TransformAuthenticationHandler
{
    static SkavaLogger logger = SkavaLoggerFactory.getLogger(KraftRecipesSearchHandler.class);
    private static SkavaTenantContextFactory skavaTenantContextFactory;
    private static UserPrefAndDevices userPrefdata;

    public KraftRecipesSearchHandler()
    {

    }

    public KraftRecipesSearchHandler(SkavaTenantContextFactory skavaTenantContextFactory)
    {
        this.skavaTenantContextFactory = skavaTenantContextFactory;
        try
        {
            JSONObject userPrefData = ConfigManagerInstance.getJSON("detailsmap", new JSONObject());
            if (userPrefData != null && userPrefData.length() > 0)
            {
                userPrefdata = new UserPrefAndDevices();
                JSONObject userpref = (JSONObject) JSONUtils.safeGetJSONObject("userpreferences", userPrefData);
                JSONObject deviceTypesObj = (JSONObject) JSONUtils.safeGetJSONObject("deviceTypes", userPrefData);
                String adzerkUrl = (String) JSONUtils.safeGetStringValue(userPrefData, "adzerkUrl", null);
                if (adzerkUrl != null)
                {
                    userPrefdata.setAdzerlUrl(adzerkUrl);
                }
                if (userpref != null)
                {
                    JSONArray include = JSONUtils.safeGetJSONArray("include", userpref);
                    JSONArray exculde = JSONUtils.safeGetJSONArray("exclude", userpref);
                    TreeMap<String, String> userPreferences = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

                    if (include != null && include.length() > 0)
                    {
                        for (int i = 0; i < include.length(); i++)
                        {
                            userPreferences.put(include.getString(i), "include");
                        }
                    }
                    if (exculde != null && exculde.length() > 0)
                    {
                        for (int i = 0; i < exculde.length(); i++)
                        {
                            userPreferences.put(exculde.getString(i), "exculde");
                        }
                    }
                    if (userPreferences != null && userPreferences.size() > 0)
                    {
                        userPrefdata.setUserPref(userPreferences);
                    }
                }
                if (deviceTypesObj != null)
                {
                    HashMap<String, String> deviceOSTypes = new HashMap<String, String>();
                    if (deviceTypesObj != null)
                    {
                        deviceOSTypes = JSONUtils.getHashMapFromJSONObject(deviceTypesObj);
                    }

                    if (deviceOSTypes != null && deviceOSTypes.size() > 0)
                    {
                        userPrefdata.setDeviceTypes(deviceOSTypes);
                    }
                }
            }
        }

        catch (Exception e)
        {
            logger.error("Error occurred at processing userpreferencesmap", e);
        }
    }

    @Override
    public void postAuthenticate(HttpServletRequest arg0,
                                 HttpServletResponse arg1,
                                 HashMap<String, List<String>> arg2,
                                 String arg3,
                                 String arg4,
                                 Type arg5,
                                 SkavaCOMRequest arg6,
                                 SkavaTransformer arg7,
                                 String arg8,
                                 long arg9) throws ServerException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String preAuthenticate(HttpServletRequest request,
                                  HttpServletResponse response,
                                  HashMap<String, List<String>> queryParametersMap,
                                  String typeFamily,
                                  String pathIdPattern,
                                  Type type,
                                  SkavaCOMRequest comRequest,
                                  SkavaTransformer transform,
                                  long campaignId) throws ServerException
    {

        String toRet = null;
        try
        {
            if (queryParametersMap != null)
            {
                String smartZoneFlag = getInputparamValue(queryParametersMap, "smartzone");
                String withuserpreferenceFlag = getInputparamValue(queryParametersMap, "withuserpreference");
                String locale = getInputparamValue(queryParametersMap, "locale");
                long storeId = ReadUtil.getLong(getInputparamValue(queryParametersMap, "storeId"), 0);

                if (Boolean.valueOf(smartZoneFlag))
                {
                    String contentType = "application/json";
                    String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "";

                    String os = "";
                    HashMap<String, String> deviceOSTypes = userPrefdata.getDeviceTypes();
                    if (userAgent != "")
                    {
                        for (String key : deviceOSTypes.keySet())
                        {
                            if (userAgent.toLowerCase().indexOf(key) >= 0)
                            {
                                os = deviceOSTypes.get(key);
                            }
                        }
                    }
                    SkavaCOMUserRequest userRequestData = (SkavaCOMUserRequest) comRequest;
                    JSONObject postBodyObj = new JSONObject();
                    JSONArray placementsArr = new JSONArray();
                    if (userRequestData.getAddressModel() != null && userRequestData.getAddressModel().length > 0)
                    {
                        SkavaCOMUserModel item = userRequestData.getAddressModel()[0];
                        if (item.getAdsDetails().length > 0)
                        {
                            for (int itr = 0; itr < item.getAdsDetails().length; itr++)
                            {
                                SkavaCOMSmartZoneModel adsDetails = item.getAdsDetails()[itr];
                                JSONObject placementsObj = new JSONObject();
                                if (adsDetails != null)
                                {
                                    if (adsDetails.getName() != null)
                                    {
                                        placementsObj.put("divName", adsDetails.getName());
                                    }
                                    if (adsDetails.getNetworkId() != null)
                                    {
                                        placementsObj.put("networkId", adsDetails.getNetworkId());
                                    }
                                    if (adsDetails.getSiteId() != null)
                                    {
                                        placementsObj.put("siteId", adsDetails.getSiteId());
                                    }

                                    JSONArray adTypes = new JSONArray();
                                    if (adsDetails.getTypes() != null)
                                    {
                                        String types = adsDetails.getTypes();
                                        String[] splitTypes = types.split(",");
                                        for (int i = 0; i < splitTypes.length; i++)
                                        {
                                            adTypes.put(Integer.parseInt(splitTypes[i]));
                                        }
                                        placementsObj.put("adTypes", adTypes);
                                    }
                                    JSONArray zones = new JSONArray();
                                    if (adsDetails.getZones() != null)
                                    {
                                        zones.put(Integer.parseInt(adsDetails.getZones()));
                                        placementsObj.put("zoneIds", zones);
                                    }
                                    JSONObject properties = new JSONObject();
                                    if (adsDetails.getCustomParams() != null)
                                    {
                                        properties = JSONUtils.getJSONObjectFromMap(adsDetails.getCustomParams(), false);
                                    }
                                    if (os != "")
                                    {
                                        properties.put("devicetype", os);
                                    }
                                    placementsObj.put("properties", properties);
                                    placementsArr.put(placementsObj);
                                }
                            }
                            postBodyObj.put("placements", placementsArr);
                        }
                        if (item.getCustomParams() != null)
                        {
                            HashMap<String, List<String>> map = item.getCustomParams();
                            for (Map.Entry<String, List<String>> entry : map.entrySet())
                            {
                                String key = entry.getKey();
                                if (entry.getValue() != null && entry.getValue().size() > 0)
                                {
                                    if (key.equals("keywords"))
                                    {
                                        JSONArray value = new JSONArray();
                                        for (String val : entry.getValue())
                                        {
                                            value.put(val);
                                        }
                                        postBodyObj.put(entry.getKey(), value);
                                    }
                                    else
                                    {
                                        postBodyObj.put(entry.getKey(), entry.getValue().get(0));
                                    }
                                }
                            }
                        }
                    }
                    String url = userPrefdata.getAdzerlUrl();

                    HashMap<String, List<String>> headers = (HashMap<String, List<String>>) URLUtil.readHeaders(request);
                    if (headers == null)
                    {
                        headers = new HashMap<String, List<String>>();
                    }
                    if (headers.containsKey("Host"))
                    {
                        headers.remove("Host");
                    }
                    headers.put("Content-Type", Arrays.asList(contentType));
                    headers.put("Accept-Language", Arrays.asList(locale));
                    if (url != null)
                    {
                        SkavaHttpRequest skavaRequest = new SkavaHttpRequest(url, null, headers, null, postBodyObj.toString().getBytes(), "POST", request.getRemoteHost());
                        try
                        {
                            SkavaHttpResponse skResponse = null;
                            try
                            {
                                skResponse = transform.getHttpclientservice().makeRequest(skavaRequest, -1, true);
                            }
                            catch (Exception e)
                            {
                                if (queryParametersMap.containsKey("categoryid") && queryParametersMap.get("categoryid") != null && queryParametersMap.get("categoryid").size() > 0)
                                {
                                    queryParametersMap.put("pdpflag", Arrays.asList("true"));
                                    return null;
                                }
                                else
                                {
                                    JSONObject root = new JSONObject();
                                    root.put("responseMessage", "Adzerk failed :" + e.getMessage());
                                    return root.toString();
                                }
                            }
                            if (skResponse != null)
                            {
                                queryParametersMap.put("plpflag", Arrays.asList("false"));
                                queryParametersMap.put("searchflag", Arrays.asList("false"));
                                String resp = new String(skResponse.getContent());
                                JSONObject transformResponse = new JSONObject(resp);

                                if (transformResponse != null)
                                {
                                    JSONObject decisions = (JSONObject) JSONUtils.safeGetJSONObject("decisions", transformResponse);
                                    Iterator<?> keys = decisions.keys();
                                    JSONArray googleAds = new JSONArray();
                                    while (keys.hasNext())
                                    {
                                        String name = keys.next().toString();
                                        Object div2Obj = JSONUtils.safeGetJSONObject(name, decisions);
                                        if (div2Obj instanceof JSONObject)
                                        {
                                            JSONObject div1 = (JSONObject) div2Obj;
                                            JSONArray contentsArray = JSONUtils.safeGetJSONArray("contents", div1);
                                            if ((contentsArray != null) && (contentsArray.length() > 0))
                                            {
                                                JSONObject contentsObj = contentsArray.getJSONObject(0);
                                                JSONObject data = (JSONObject) JSONUtils.safeGetJSONObject("data", contentsObj);
                                                if (data != null)
                                                {
                                                    JSONObject customData = (JSONObject) JSONUtils.safeGetJSONObject("customData", data);
                                                    if (customData != null)
                                                    {
                                                        if (customData.has("category"))
                                                        {
                                                            String categoryId = JSONUtils.safeGetStringValue(customData, "category", "");
                                                            if (categoryId != null)
                                                            {
                                                                queryParametersMap.put("plpflag", Arrays.asList("true"));
                                                                queryParametersMap.put("categoryid", Arrays.asList(new String[] { categoryId }));
                                                            }
                                                        }
                                                        else if (customData.has("product"))
                                                        {
                                                            String productId = JSONUtils.safeGetStringValue(customData, "product", "");
                                                            if (productId != null)
                                                            {
                                                                queryParametersMap.put("searchflag", Arrays.asList("true"));
                                                                queryParametersMap.put("productids", Arrays.asList(new String[] { productId }));
                                                            }
                                                        }
                                                        else
                                                        {
                                                            JSONObject adsData = new JSONObject();
                                                            int customDatatype = (int) JSONUtils.safeGetIntValue(customData, "type", 0);
                                                            if (customDatatype == 0 || customDatatype == 2)
                                                            {
                                                                if (contentsObj.get("body") != null)
                                                                {
                                                                    if (name.contains("categoryImage"))
                                                                    {
                                                                        queryParametersMap.put("catImage", Arrays.asList(new String[] { (String) contentsObj.get("body") }));
                                                                    }
                                                                    else if (name.contains("productImage"))
                                                                    {
                                                                        queryParametersMap.put("pdtImage", Arrays.asList(new String[] { (String) contentsObj.get("body") }));
                                                                    }
                                                                    else
                                                                    {
                                                                        if (contentsObj.get("body") != null)
                                                                        {
                                                                            adsData.put("body", contentsObj.get("body"));
                                                                        }
                                                                        if (contentsObj.get("customTemplate") != null)
                                                                        {
                                                                            adsData.put("customTemplate", contentsObj.get("customTemplate"));
                                                                        }
                                                                        if (adsData != null)
                                                                        {
                                                                            googleAds.put(adsData);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            else if (customDatatype == 4)
                                                            {
                                                                if (contentsObj.has("body"))
                                                                {
                                                                    JSONObject productDetails = new JSONObject(contentsObj.getString("body"));
                                                                    if (productDetails != null)
                                                                    {
                                                                        JSONArray productIdsArr = JSONUtils.safeGetJSONArray("Id", productDetails);
                                                                        String productIds = "";
                                                                        if (productIdsArr != null && productIdsArr.length() > 0)
                                                                        {
                                                                            for (int itr = 0; itr < productIdsArr.length(); itr++)
                                                                            {
                                                                                productIds += productIdsArr.get(itr) + ",";
                                                                            }
                                                                            productIds = productIds.substring(0, productIds.length() - 1);
                                                                            queryParametersMap.put("searchflag", Arrays.asList("true"));
                                                                            queryParametersMap.put("productids", Arrays.asList(productIds));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (googleAds != null && googleAds.length() > 0)
                                    {
                                        queryParametersMap.put("googleAd", Arrays.asList(googleAds.toString()));
                                    }
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            logger.error("Error While Processing KraftRecipesSmartzoneHandler ", e);
                        }
                    }
                }
                else
                {
                    queryParametersMap.put("plpflag", Arrays.asList("false"));
                    queryParametersMap.put("searchflag", Arrays.asList("false"));
                }
                String userPrefParam = getUserPreferences(request, withuserpreferenceFlag, locale, storeId, queryParametersMap);
                if (userPrefParam != null)
                {
                    queryParametersMap.put("userpreferences", Arrays.asList(userPrefParam));
                }
            }
        }
        catch (

        Exception e)
        {
            logger.error("Error While Processing KraftRecipesSmartzoneHandler ", e);
            throw new ServerException("Error While Processing KraftRecipesSmartzoneHandler ", e);
        }
        return toRet;
    }

    private String getInputparamValue(HashMap<String, List<String>> inputParams,
                                      String paramName)
    {
        if (inputParams != null && inputParams.containsKey(paramName) && inputParams.get(paramName).size() > 0) { return inputParams.get(paramName).get(0); }
        return null;
    }

    private String getUserPreferences(HttpServletRequest request,
                                      String withuserpreferenceFlag,
                                      String locale,
                                      long storeId,
                                      HashMap<String, List<String>> inputParams) throws Exception
    {
        if (Boolean.valueOf(withuserpreferenceFlag))
        {
            Cookie[] cookie = request.getCookies();
            boolean isLogInCookieAvailable = false;
            String userPrefStr = null;
            String USERPREF_COOKIE_NAME = "userpreferences";
            if (cookie != null && cookie.length > 0)
            {
                for (int cidx = 0; cidx < cookie.length; cidx++)
                {
                    if (cookie[cidx].getName() != null)
                    {
                        if (cookie[cidx].getName().contains("ckcjeu"))
                        {
                            isLogInCookieAvailable = true;
                        }
                        else if (cookie[cidx].getName().contains(USERPREF_COOKIE_NAME))
                        {
                            String userPrefStrEncoded = CookieUtil.getCookieValue(request, USERPREF_COOKIE_NAME);
                            userPrefStr = URLDecoder.decode(userPrefStrEncoded, "utf-8");
                        }
                    }
                }
                if (isLogInCookieAvailable)
                {
                    TreeMap<String, String> userPrefMap = userPrefdata.getUserPref();
                    if (userPrefStr != null && userPrefStr.length() > 0)
                    {
                        String userPrefToRet = getFormattedUserPreference(userPrefStr, userPrefMap, inputParams);
                        return userPrefToRet;
                    }
                    else
                    {
                        ComUserResponse comUserResponse = null;
                        try
                        {
                            StreamComUserService StreamComUserService = (StreamComUserService) skavaTenantContextFactory.get(TenantThreadLocal.get(), StreamComUserRemoteServiceBuilder.STREAM_COM_USER_REMOTE_SERVICE);
                            comUserResponse = StreamComUserService.getProfileBpm(request, null, locale, false, storeId);
                        }
                        catch (Exception e)
                        {
                            logger.error("Error While Processing User profile in KraftRecipesSmartzoneHandler ", e);
                            throw new ServerException("Error While Processing User profile in KraftRecipesSmartzoneHandler ", e);
                        }

                        HashMap<String, String> userProfileResp = new HashMap<String, String>();
                        if (comUserResponse != null)
                        {
                            if (comUserResponse.getUserProperties() != null && comUserResponse.getUserProperties().getCustomProperties() != null)
                            {
                                userProfileResp = comUserResponse.getUserProperties().getCustomProperties();
                            }
                        }
                        String prefStr = "";
                        if (userProfileResp != null && userProfileResp.size() > 0 && userProfileResp.containsKey("preferenceJSON"))
                        {
                            prefStr = userProfileResp.get("preferenceJSON");
                            String userPrefToRet = getFormattedUserPreference(prefStr, userPrefMap, inputParams);
                            return userPrefToRet;
                        }
                    }
                }
            }
        }
        return null;
    }

    static class UserPrefAndDevices
    {
        public @Setter @Getter TreeMap<String, String> userPref;
        public @Setter @Getter HashMap<String, String> deviceTypes;
        public @Setter @Getter String adzerlUrl;
    }

    private String getFormattedUserPreference(String prefStr,
                                              TreeMap<String, String> userPrefMap,
                                              HashMap<String, List<String>> inputParams) throws JSONException
    {
        try
        {
            if (prefStr != null && prefStr.length() > 0)
            {
                JSONObject preferenceJson = new JSONObject(prefStr);
                JSONArray selectedFacets = new JSONArray();
                if (preferenceJson != null)
                {
                    JSONObject preferencesObj = (JSONObject) JSONUtils.safeGetJSONObject("preferences", preferenceJson);
                    if (preferencesObj != null && preferencesObj.length() > 0)
                    {
                        Iterator<String> prefItr = preferencesObj.keys();
                        while (prefItr.hasNext())
                        {
                            String category = (String) prefItr.next();
                            if (userPrefMap.containsKey(category))
                            {
                                if (preferencesObj.get(category) != null)
                                {
                                    JSONObject currentFacet = new JSONObject();
                                    currentFacet.put("key", category);
                                    currentFacet.put("value", preferencesObj.get(category));
                                    currentFacet.put("operation", userPrefMap.get(category) == "include" ? 0 : 1);
                                    selectedFacets.put(currentFacet);
                                }
                            }
                        }
                        if (inputParams != null && inputParams.size() > 0)
                        {
                            String userPreferenceParam = getInputparamValue(inputParams, "userpreferences");
                            if (userPreferenceParam != null && userPreferenceParam.length() > 0)
                            {
                                JSONArray userPreferenceParamArr = new JSONArray(userPreferenceParam);
                                if (userPreferenceParamArr != null && userPreferenceParamArr.length() > 0)
                                {
                                    for (int itr = 0; itr < userPreferenceParamArr.length(); itr++)
                                    {
                                        selectedFacets.put(userPreferenceParamArr.get(itr));
                                    }
                                }
                            }
                        }
                        return selectedFacets.toString();
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error While Processing KraftRecipesSmartzoneHandler ", e);
        }
        return null;
    }
}
