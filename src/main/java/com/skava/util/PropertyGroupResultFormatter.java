/*******************************************************************************
 * Copyright ©2002-2014 Skava. 
 * All rights reserved.The Skava system, including 
 * without limitation, all software and other elements
 * thereof, are owned or controlled exclusively by
 * Skava and protected by copyright, patent, and 
 * other laws. Use without permission is prohibited.
 * 
 *  For further information contact Skava at info@skava.com.
 ******************************************************************************/
package com.skava.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SearchResponseFormatter;
import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.searchv2.StreamSearchGroupResult;
import com.skava.model.searchv2.StreamSearchResponse;
import com.skava.model.searchv2.StreamSearchResult;
import com.skava.model.userv2.UserV2;

public class PropertyGroupResultFormatter implements SearchResponseFormatter
{
    public static final String ENTRYPROPNAME_USERID = "userid";
    private static final int ERROR_CODE_NO_CONTENT = 204;
    private static final String ERROR_NO_MATCHING_DATA = "No matching data";

    public Response format(HttpServletRequest request,
                           StreamSearchResponse searchResult,
                           int limit,
                           Campaign campaign,
                           UserV2 user,
                           boolean useV2,
                           StreamSearchResponse curratedresult,
                           JSONObject multiProducts,
                           JSONObject curatedFacetConfig) throws ServerException
    {
        return format(searchResult, null, limit, user, null, campaign, null);
    }

    public Response format(StreamSearchResponse result1,
                           StreamSearchResponse result2,
                           int limit,
                           UserV2 user,
                           DBSessionManager dbSessionManager,
                           Campaign campaign) throws ServerException
    {
        return format(result1, result2, limit, user, dbSessionManager, campaign, null);
    }

    public Response format(StreamSearchResponse searchResult,
                           StreamSearchResponse result2,
                           int limit,
                           UserV2 user,
                           DBSessionManager dbSessionManager,
                           Campaign campaign,
                           String vendor) throws ServerException
    {
        Map<String, Object> productDataMap = getProductsData(result2);

        return format(searchResult, user, campaign, productDataMap);
    }

    public Response format(StreamSearchResponse searchResult,
                           UserV2 user,
                           Campaign campaign,
                           Map<String, Object> dataMap)
    {
        Response toRet = null;
        //;
        //JSONArray toRet = null;
        try
        {
            JSONObject toret_final = null;
            boolean isowner = false;

            if (searchResult != null)
            {
                // Check whether the user is the owner of the list
                if (user != null && searchResult.getNumEntries() > 0)
                {
                    long userId = Long.parseLong(searchResult.getResults().get(0).getPropertyJSON().get(ENTRYPROPNAME_USERID).toString());
                    if (userId == user.getId())
                    {
                        isowner = true;
                    }
                }
                toret_final = new JSONObject();
                JSONObject groupAll = new JSONObject();
                ArrayList<StreamSearchResult> results = searchResult.getResults();
                int count = 0;
                if (results == null && searchResult.getGroups() != null && searchResult.getGroups().size() == 1)
                {
                    ArrayList<StreamSearchResult> groupResults = new ArrayList<StreamSearchResult>();
                    Iterator<Map.Entry<String, ArrayList<StreamSearchGroupResult>>> resultIterator = searchResult.getGroups().entrySet().iterator();
                    ArrayList<StreamSearchGroupResult> result = resultIterator.next().getValue();
                    for (int i = 0; i < result.size(); i++)
                    {
                        JSONArray arrayObj = new JSONArray();

                        StreamSearchGroupResult groupResult = result.get(i);
                        if (groupResult != null && groupResult.getResults().size() > 0 && groupResult.getGroupName() != null)
                        {
                            groupResults = groupResult.getResults();
                            count = groupResult.getMatches();

                            if (groupResults != null && groupResults.size() > 0)
                            {
                                for (int j = 0; j < groupResults.size(); j++)
                                {
                                    StreamSearchResult resultGroup = groupResults.get(j);
                                    if (resultGroup != null)
                                    {
                                        String piiProperties = null;
                                        JSONObject obj = resultGroup.getPropertyJSON();
                                        if (!isowner)
                                        {
                                            obj.remove("propertiesJson");
                                            piiProperties = campaign.getProperty(CampaignProperties.PROP_LIST_PII_PROPERTIES);
                                            if (piiProperties != null)
                                            {
                                                String[] piiPropArr = piiProperties.split(",");
                                                for (String piiProp : piiPropArr)
                                                {
                                                    obj.remove(piiProp.trim());
                                                    obj.remove("property_" + piiProp.trim());
                                                }
                                            }
                                        }
                                        Object productProps = dataMap != null ? dataMap.get(resultGroup.getProperty("productid")) : null;
                                        obj.put("productProperties", productProps);
                                        arrayObj.put(obj);
                                    }
                                }
                            }
                            groupAll.put(groupResult.getGroupName(), arrayObj);
                        }
                    }
                    toret_final.put("groupResult", groupAll);
                    toret_final.put("resultsCount", count);
                }

                if (results != null && results.size() > 0)
                {
                    for (int j = 0; j < results.size(); j++)
                    {
                        StreamSearchResult result = results.get(j);
                        if (result != null)
                        {
                            JSONObject obj = result.getPropertyJSON();
                            Object productProps = dataMap != null ? dataMap.get(result.getProperty("productid")) : null;
                            obj.put("productProperties", productProps);
                        }
                    }
                }

                if (results != null && results.size() > 0)
                {
                    JSONArray jAResults = new JSONArray();
                    for (int j = 0; j < results.size(); j++)
                    {
                        StreamSearchResult result = results.get(j);
                        if (result != null)
                        {
                            String piiProperties = null;
                            JSONObject obj = result.getPropertyJSON();
                            if (!isowner)
                            {
                                obj.remove("propertiesJson");
                                piiProperties = campaign.getProperty(CampaignProperties.PROP_LIST_PII_PROPERTIES);
                                if (piiProperties != null)
                                {
                                    String[] piiPropArr = piiProperties.split(",");
                                    for (String piiProp : piiPropArr)
                                    {
                                        obj.remove(piiProp.trim());
                                        obj.remove("property_" + piiProp.trim());
                                    }
                                }
                            }
                            Object productProps = dataMap != null ? dataMap.get(result.getProperty("productid")) : null;
                            obj.put("productProperties", productProps);
                            jAResults.put(obj);
                        }
                    }
                    toret_final.put("resultsCount", searchResult.getNumEntries());
                    toret_final.put("results", jAResults);
                    toret_final.put("isowner", isowner);
                    if (searchResult.getFacets() != null)
                    {
                        toret_final.put("facets", new JSONObject(CastUtil.toJSON(searchResult.getFacets())));
                    }
                    if (searchResult.getSelectedFacets() != null)
                    {
                        toret_final.put("selectedFacets", new JSONObject(CastUtil.toJSON(searchResult.getSelectedFacets())));
                    }
                }

                if (toret_final != null)
                {
                    if (searchResult.getNumEntries() > 0 || (searchResult.getGroups() != null && searchResult.getGroups().size() > 0 && count > 0))
                    {
                        toret_final.put("responseCode", Response.RESPONSE_SUCCESS);
                        toret_final.put("responseMessage", Response.RESPONSE_MSG_SUCCESS);
                    }
                    else
                    {
                        toret_final.put("responseCode", ERROR_CODE_NO_CONTENT);
                        toret_final.put("responseMessage", ERROR_NO_MATCHING_DATA);
                    }

                    toRet = new Response(toret_final.toString().getBytes("UTF-8"));
                }
            }
        }
        catch (Exception e)
        {
            toRet = new Response();
            toRet.setResponseCode(Response.RESPONSE_FAILED);
            toRet.setResponseMessage(e.getMessage());
        }
        return toRet;
    }

    private Map<String, Object> getProductsData(StreamSearchResponse solrProductsSearchResponse) throws ServerException
    {
        Map<String, Object> result2Map = new HashMap<String, Object>();

        if (solrProductsSearchResponse != null && solrProductsSearchResponse.getResults() != null)
        {
            for (StreamSearchResult searchResult2 : solrProductsSearchResponse.getResults())
            {
                if (searchResult2.getProperty("productid") != null)
                {
                    String productId = (String) searchResult2.getProperty("productid");
                    result2Map.put(productId, searchResult2.getProperties());
                }
            }
        }
        return result2Map;
    }
}
