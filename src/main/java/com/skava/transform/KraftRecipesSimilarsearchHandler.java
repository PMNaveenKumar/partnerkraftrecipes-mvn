package com.skava.transform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.interfaces.TransformAuthenticationHandler;
import com.skava.model.com.request.SkavaCOMRequest;
import com.skava.model.com.request.SkavaCOMSmartZoneModel;
import com.skava.model.com.request.SkavaCOMUserModel;
import com.skava.model.com.request.SkavaCOMUserRequest;
import com.skava.model.http.SkavaHttpRequest;
import com.skava.model.http.SkavaHttpResponse;
import com.skava.model.transform.Type;
import com.skava.services.SkavaTransformer;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.JSONUtils;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.URLUtil;

import lombok.Getter;
import lombok.Setter;

public class KraftRecipesSimilarsearchHandler implements TransformAuthenticationHandler
{

    static SkavaLogger logger = SkavaLoggerFactory.getLogger(KraftRecipesSmartzoneHandler.class);
    private static UserPrefAndDevices userPrefdata;

    public KraftRecipesSimilarsearchHandler()
    {

    }

    public KraftRecipesSimilarsearchHandler(SkavaTenantContextFactory skavaTenantContextFactory)
    {
        try
        {
            JSONObject userPrefData = ConfigManagerInstance.getJSON("detailsmap", new JSONObject());
            if (userPrefData != null && userPrefData.length() > 0)
            {
                userPrefdata = new UserPrefAndDevices();
                JSONObject deviceTypesObj = (JSONObject) JSONUtils.safeGetJSONObject("deviceTypes", userPrefData);
                String adzerkUrl = (String) JSONUtils.safeGetStringValue(userPrefData, "adzerkUrl", null);
                if (adzerkUrl != null)
                {
                    userPrefdata.setAdzerlUrl(adzerkUrl);
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
                String divName = "";
                String locale = getInputparamValue(queryParametersMap, "locale");
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
                    JSONObject placementsObj = new JSONObject();
                    JSONArray placementsArr = new JSONArray();
                    if (userRequestData.getAddressModel() != null && userRequestData.getAddressModel().length > 0)
                    {
                        SkavaCOMUserModel item = userRequestData.getAddressModel()[0];
                        if (item.getAdsDetails().length > 0)
                        {
                            SkavaCOMSmartZoneModel adsDetails = item.getAdsDetails()[0];
                            if (adsDetails != null)
                            {
                                if (adsDetails.getName() != null)
                                {
                                    placementsObj.put("divName", adsDetails.getName());
                                    divName = adsDetails.getName();
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
                                postBodyObj.put("placements", placementsArr);
                            }
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
                                    queryParametersMap.put("issmartzone", Arrays.asList("false"));
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
                                queryParametersMap.put("issmartzone", Arrays.asList("false"));
                                String resp = new String(skResponse.getContent());
                                JSONObject transformResponse = new JSONObject(resp);

                                if (transformResponse != null)
                                {
                                    JSONObject decisions = (JSONObject) JSONUtils.safeGetJSONObject("decisions", transformResponse);
                                    Object div2Obj = JSONUtils.safeGetJSONObject(divName, decisions);
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
                                                    if (JSONUtils.safeGetJSONObject("category", customData) != null)
                                                    {
                                                        String categoryId = JSONUtils.safeGetStringValue(customData, "category", "");
                                                        if (categoryId != null)
                                                        {
                                                            queryParametersMap.put("issmartzone", Arrays.asList("productlist"));
                                                            queryParametersMap.put("categoryid", Arrays.asList(new String[] { categoryId }));
                                                        }
                                                    }
                                                    else
                                                    {
                                                        int customDatatype = (int) JSONUtils.safeGetIntValue(customData, "type", 0);
                                                        if (customDatatype == 4 && contentsObj.has("body"))
                                                        {
                                                            String pdtData = (String) contentsObj.get("body");
                                                            String firstChar = "";
                                                            if (pdtData != null && pdtData != "")
                                                            {
                                                                firstChar = String.valueOf(pdtData.charAt(0));
                                                            }
                                                            if (!firstChar.equalsIgnoreCase("["))
                                                            {
                                                                JSONObject productDetails = new JSONObject((String) pdtData);
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
                                                                        queryParametersMap.put("categoryid", Arrays.asList(productIds));
                                                                    }
                                                                }
                                                                queryParametersMap.put("issmartzone", Arrays.asList("search"));
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
                        catch (Exception e)
                        {
                            logger.error("Error While Processing KraftRecipesSimilarsearchHandler ", e);
                        }
                    }
                }
                else
                {
                    queryParametersMap.put("issmartzone", Arrays.asList("false"));
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error While Processing KraftRecipesSimilarsearchHandler ", e);
            throw new ServerException("Error While Processing KraftRecipesSimilarsearchHandler ", e);
        }
        return toRet;
    }
    private String getInputparamValue(HashMap<String, List<String>> inputParams,
                                      String paramName)
    {
        if (inputParams != null && inputParams.containsKey(paramName) && inputParams.get(paramName).size() > 0) { return inputParams.get(paramName).get(0); }
        return null;
    }
    static class UserPrefAndDevices
    {
        public @Setter @Getter HashMap<String, String> deviceTypes;
        public @Setter @Getter String adzerlUrl;
    }
}
