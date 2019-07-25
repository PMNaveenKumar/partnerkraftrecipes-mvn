package com.skava.transform;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.skava.interfaces.URLGenerator;
import com.skava.model.com.request.SkavaCOMRequest;
import com.skava.model.http.SkavaHttpRequest;
import com.skava.model.transform.Data;
import com.skava.model.transform.Source;
import com.skava.model.transform.Type;
import com.skava.services.HttpClientService;
import com.skava.transform.helper.TransformHelper;
import com.skava.util.ServerException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;

public class KraftRecipesURLGenerator implements URLGenerator
{
    private static final SkavaLogger logger = SkavaLoggerFactory.getLogger(KraftRecipesURLGenerator.class);

    public String generate(Type type,
                           String url,
                           Map<String, List<String>> secret_access_key,
                           Source source,
                           SkavaHttpRequest skavaRequest,
                           HttpServletRequest httpRequest,
                           SkavaCOMRequest request,
                           HttpClientService httpClientService,
                           HashMap<String, List<String>> inputParams,
                           HashMap<String, List<String>> paginationParams,
                           HashMap<String, Data> dataMap) throws ServerException
    {
        try
        {
            if (url != null)
            {
                if (type.getTypeFamily().equals("smartzone"))
                {
                    if (url.contains("/productlist/"))
                    {
                        if (inputParams != null && url != null && inputParams.containsKey("searchflag") && inputParams.get("searchflag").size() > 0 && inputParams.get("searchflag").get(0).equals("true"))
                        {
                            url = url.replace("/productlist/loadSearch", "/search/load");
                            url = url.replace("categoryId", "productId");
                            if (inputParams.containsKey("selectedFacets"))
                            {
                                url += "&selectedFacets={\"selectedFacets\":" + inputParams.get("selectedFacets").get(0) + "}";
                            }
                        }
                        else
                        {
                            if (inputParams.containsKey("selectedFacets"))
                            {
                                url += "&filter={\"selectedFacets\":" + inputParams.get("selectedFacets").get(0) + "}";
                            }
                        }
                    }
                }
                else if (type.getIdPattern().toString().equals("search"))
                {
                    JSONObject imagesObj = new JSONObject();

                    if (inputParams.containsKey("catImage") && inputParams.get("catImage").size() > 0)
                    {
                        imagesObj.put("catImage", inputParams.get("catImage").get(0));
                        inputParams.remove("catImage");
                    }
                    if (inputParams.containsKey("pdtImage") && inputParams.get("pdtImage").size() > 0)
                    {
                        imagesObj.put("pdtImage", inputParams.get("pdtImage").get(0));
                        inputParams.remove("pdtImage");
                    }
                    if (inputParams.containsKey("googleAd") && inputParams.get("googleAd").size() > 0)
                    {
                        JSONArray googleAds = new JSONArray(inputParams.get("googleAd").get(0));
                        imagesObj.put("googleAd", googleAds);
                        inputParams.remove("googleAd");
                    }
                    if (imagesObj != null && imagesObj.length() > 0)
                    {
                        dataMap.put("smartzoneAds_data", TransformHelper.getData(Source.TYPE_JSON, imagesObj.toString().getBytes(), null, null, "_", "UTF-8", null, false, null, null, false, source));
                    }
                }
                else
                {
                    if (inputParams != null && inputParams.containsKey("selectedFacets") && inputParams.get("selectedFacets").size() > 0)
                    {
                        if (url.contains("/kraft/search/"))
                        {
                            url += "&selectedFacets={\"selectedFacets\":" + inputParams.get("selectedFacets").get(0) + "}";
                        }
                        else
                        {
                            url += "&filter={\"selectedFacets\":" + inputParams.get("selectedFacets").get(0) + "}";
                        }
                        inputParams.remove("selectedFacets");
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error occurred at processing KraftRecipesURLGenerator", e);
        }
        return url;
    }
}
