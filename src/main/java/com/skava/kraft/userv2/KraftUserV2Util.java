package com.skava.kraft.userv2;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.skava.model.http.SkavaHttpRequest;
import com.skava.model.http.SkavaHttpResponse;
import com.skava.services.HttpClientService;
import com.skava.util.Constants;
import com.skava.util.ServerException;

public class KraftUserV2Util
{
    public static JSONObject getDataFromUrl(HttpServletRequest request,
                                      HttpClientService httpClientService,
                                      String url,
                                      HashMap<String, List<String>> headers,
                                      HashMap<String, List<String>> params,
                                      String method,
                                      String content) throws ServerException, UnsupportedEncodingException
    {
        JSONObject respObj = null;
        String strResponse = null;
        try
        {
            SkavaHttpRequest httpRequest = new SkavaHttpRequest(url, params, headers, Constants.CTYPE_JSON, (content != null ? content.getBytes() : null), method, null);
            SkavaHttpResponse httpResponse = httpClientService.makeRequest(request, httpRequest, -1, true, 0L, false, null);
            strResponse = new String(httpResponse.getContent(), "UTF-8");
            respObj = new JSONObject(strResponse);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return respObj;
    }

    public static JSONArray getDataFromUrlAsArray(HttpServletRequest request,
                                            HttpClientService httpClientService,
                                            String url,
                                            HashMap<String, List<String>> headers,
                                            HashMap<String, List<String>> params,
                                            String method,
                                            String content) throws ServerException, UnsupportedEncodingException
    {
        JSONArray respObj = null;
        String strResponse = null;
        try
        {
            SkavaHttpRequest httpRequest = new SkavaHttpRequest(url, params, headers, Constants.CTYPE_JSON, (content != null ? content.getBytes() : null), method, null);
            SkavaHttpResponse httpResponse = httpClientService.makeRequest(request, httpRequest, -1, true, 0L, false, null);
            strResponse = new String(httpResponse.getContent(), "UTF-8");
            respObj = new JSONArray(strResponse);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return respObj;
    }
    
}
