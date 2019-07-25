package com.skava.searchv2.resultformatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.skava.db.DBSessionManager;
import com.skava.interfaces.SearchResponseFormatter;
import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.searchv2.StreamSearchGroupResult;
import com.skava.model.searchv2.StreamSearchResponse;
import com.skava.model.searchv2.StreamSearchResponseFacetValue;
import com.skava.model.searchv2.StreamSearchResult;
import com.skava.model.userv2.UserV2;
import com.skava.util.JSONUtils;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;

public class ComSolrResultFormatterKraft implements SearchResponseFormatter
{

    private static final int ERROR_CODE_NO_CONTENT = 204;
    private static final String ERROR_NO_MATCHING_DATA = "No matching data";

    @Override
    public Response format(HttpServletRequest request,
                           StreamSearchResponse searchResult,
                           int limit,
                           Campaign campaign,
                           UserV2 user,
                           boolean userV2,
                           StreamSearchResponse curratedResult,
                           JSONObject multiProducts,
                           JSONObject curatedFacetConfig) throws ServerException
    {
        Response toRet = null;
        try
        {
            long campaignId = campaign.getId();
            String region = ReadUtil.getString(request.getAttribute("region"), null);
            campaignId = ReadUtil.getLong(campaign.getProperty(CampaignProperties.PROP_SEARCHDOMAIN_SEARCHCAMPAIGNID), campaignId);
            JSONObject jsonObj = null;
            String synonym = null;
            if (request != null && request.getAttribute("synonymWord") != null)
            {
                synonym = (String) request.getAttribute("synonymWord");
            }
            if (searchResult.getErrorMessage() != null)
            {
                throw new ServerException(searchResult.getErrorMessage());
            }
            else if (searchResult.getNumEntries() == 0 || (searchResult.getGroups() != null && searchResult.getGroups().size() == 0) || (searchResult.getResults() != null && searchResult.getResults().size() == 0))
            {
                toRet = new Response(ERROR_CODE_NO_CONTENT, ERROR_NO_MATCHING_DATA);
                return toRet;
            }

            else
            {
                if (searchResult != null && searchResult.getNumEntries() > 0)
                {
                    jsonObj = new JSONObject();
                    long resultCount = 0;
                    ArrayList<StreamSearchResult> results = searchResult.getResults();
                    HashMap<String, ArrayList<StreamSearchResponseFacetValue>> facets = searchResult.getFacets();
                    if (results == null && searchResult.getGroups() != null && searchResult.getGroups().size() == 1)
                    {
                        results = new ArrayList<StreamSearchResult>();
                        Iterator<Map.Entry<String, ArrayList<StreamSearchGroupResult>>> resultIterator = searchResult.getGroups().entrySet().iterator();
                        ArrayList<StreamSearchGroupResult> result = resultIterator.next().getValue();
                        for (int i = 0; i < result.size(); i++)
                        {
                            StreamSearchGroupResult groupResult = result.get(i);
                            if (groupResult != null && groupResult.getResults().size() > 0)
                            {
                                results.addAll(groupResult.getResults());
                            }
                        }
                        resultCount = searchResult.getNumEntries();
                    }
                    else
                    {
                        resultCount = searchResult.getNumEntries();
                    }

                    if (results != null && results.size() > 0)
                    {
                        JSONObject stateObj = new JSONObject();
                        JSONObject jObjProperties = null;
                        JSONObject properties = new JSONObject();
                        JSONArray jAResponse = new JSONArray();
                        for (int j = 0; j < results.size(); j++)
                        {
                            StreamSearchResult result = results.get(j);

                            jObjProperties = result.getPropertyJSON();
                            if (jObjProperties != null)
                            {
                                Object obj = JSONUtils.safeGetJSONObject("properties", jObjProperties);
                                if (obj != null)
                                {
                                    jAResponse.put(new JSONObject(obj.toString()));
                                }
                                else
                                {
                                    jAResponse.put(jObjProperties);
                                }
                            }
                        }
                        if (searchResult.getPossibleSorts() != null && searchResult.getPossibleSorts().length > 0)
                        {

                            JSONArray sortingObj = new JSONArray();
                            String[] possibleSorts = searchResult.getPossibleSorts();
                            JSONObject sortTemp = new JSONObject();
                            JSONArray options = new JSONArray();
                            String sortOptionsSting = campaign.getProperty(CampaignProperties.PROP_SEARCH_CUSTOM_SORTOPTIONS);
                            JSONObject sortJobj = null;
                            if (sortOptionsSting != null)
                            {
                                sortJobj = new JSONObject(sortOptionsSting);
                            }
                            for (int i = 0; i < possibleSorts.length; i++)
                            {
                                String sorts = possibleSorts[i];
                                if (sorts != null)
                                {
                                    int beginLength = 0;
                                    boolean isString = false;
                                    if (sorts.startsWith("sortstr_"))
                                    {
                                        beginLength = "sortstr_".length();
                                        isString = true;
                                    }
                                    else
                                    {
                                        beginLength = "sort_".length();
                                    }

                                    sorts = sorts.substring(beginLength, sorts.indexOf("_" + (region != null ? region + "_" : "") + campaignId)).replaceAll("_", " ");
                                    if (sorts.equals("price"))
                                    {
                                        JSONObject temp = new JSONObject();
                                        temp.put("label", "Price Low - High");
                                        temp.put("value", sorts + "|asc");
                                        options.put(temp);
                                        temp = new JSONObject();
                                        temp.put("label", "Price High - Low");
                                        temp.put("value", sorts + "|desc");
                                        options.put(temp);
                                    }
                                    else if (sorts.equals("rating"))
                                    {
                                        JSONObject temp = new JSONObject();
                                        temp.put("label", "Top Rated");
                                        temp.put("value", sorts + "|desc");
                                        options.put(temp);
                                    }
                                    else
                                    {
                                        if (sortJobj != null && sortJobj.has(sorts))
                                        {
                                            JSONObject order = sortJobj.getJSONObject(sorts);
                                            if (order != null)
                                            {
                                                String orderSorttype = JSONUtils.safeGetStringValue(order, "type", null);
                                                if (orderSorttype != null && orderSorttype.equalsIgnoreCase("desc"))
                                                {
                                                    String tempStr = sorts + (isString ? " Z-A" : " High - Low");
                                                    String displayLabel = JSONUtils.safeGetStringValue(order, "label", tempStr);
                                                    JSONObject temp = new JSONObject();
                                                    temp.put("label", displayLabel);
                                                    temp.put("value", sorts + "|desc");
                                                    options.put(temp);

                                                }
                                                else if (orderSorttype != null && orderSorttype.equalsIgnoreCase("asc"))
                                                {
                                                    String tempStr = sorts + (isString ? " A-Z" : " Low - High");
                                                    String displayLabel = JSONUtils.safeGetStringValue(order, "label", tempStr);
                                                    JSONObject temp = new JSONObject();
                                                    temp.put("label", displayLabel);
                                                    temp.put("value", sorts + "|asc");
                                                    options.put(temp);
                                                }
                                            }
                                        }
                                        else
                                        {
                                            JSONObject temp = new JSONObject();
                                            temp.put("label", sorts + (isString ? " A-Z" : " Low - High"));
                                            temp.put("value", sorts + "|asc");
                                            options.put(temp);
                                            temp = new JSONObject();
                                            temp.put("label", sorts + (isString ? " Z-A" : " High - Low"));
                                            temp.put("value", sorts + "|desc");
                                            options.put(temp);
                                        }
                                    }
                                }
                            }
                            if (options.length() > 0)
                            {
                                sortTemp.put("options", options);
                            }
                            if (searchResult.getSelectedSort() != null && options != null)
                            {
                                Map<String, String> sortMap = new HashMap<>();
                                for (int i = 0; i < options.length(); i++)
                                {
                                    JSONObject jsonObject = options.getJSONObject(i);
                                    String label = JSONUtils.safeGetStringValue(jsonObject, "label", null);
                                    String value = JSONUtils.safeGetStringValue(jsonObject, "value", null);
                                    if (label != null && value != null)
                                    {
                                        sortMap.put(value, label);
                                    }
                                }
                                String[] selectedSorts = searchResult.getSelectedSort().split(",");
                                String selected = null;
                                for (String selectedSort : selectedSorts)
                                {
                                    if (sortMap.containsKey(selectedSort))
                                    {
                                        String temp = sortMap.get(selectedSort);
                                        if (temp != null)
                                        {
                                            if (selected == null)
                                            {
                                                selected = "";
                                            }
                                            else
                                            {
                                                selected += ",";
                                            }
                                            selected += temp;
                                        }
                                    }
                                }
                                if (selected != null)
                                {
                                    sortTemp.put("selectedname", selected);
                                }
                            }
                            sortingObj.put(sortTemp);
                            stateObj.put("sorting", sortingObj);
                        }

                        if (searchResult.getSelectedFacets() != null && searchResult.getSelectedFacets().size() > 0)
                        {
                            JSONArray selectedSortArr = new JSONArray();
                            HashMap<String, String[]> selectedFacets = searchResult.getSelectedFacets();
                            for (Map.Entry<String, String[]> entry : selectedFacets.entrySet())
                            {
                                String[] value = entry.getValue();
                                for (String facet : value)
                                {
                                    JSONObject temp = new JSONObject();
                                    temp.put("primaryname", entry.getKey());
                                    temp.put("name", facet);
                                    selectedSortArr.put(temp);
                                }
                            }
                            if (selectedSortArr.length() > 0)
                            {
                                stateObj.put("selectedfacets", selectedSortArr);
                            }
                        }

                        stateObj.put("productcount", resultCount);
                        stateObj.put("searchterm", request.getParameter("searchTerm"));
                        if (request != null)
                        {
                            stateObj.put("searchterm", request.getAttribute("searchterm"));
                            if (request.getAttribute("spellcheck") != null)
                            {
                                String spellcheckTerm = (String) request.getAttribute("spellcheck");
                                String searchterm = (String) request.getAttribute("searchterm");
                                if (!spellcheckTerm.equalsIgnoreCase(searchterm))
                                {
                                    stateObj.put("searchcorrected", request.getAttribute("spellcheck"));
                                }
                            }
                            if (synonym != null)
                            {
                                stateObj.put("synonym", synonym);
                            }
                        }

                        properties.put("state", stateObj);
                        jsonObj.put("properties", properties);
                        if (jAResponse.length() > 0)
                        {
                            JSONObject productObj = new JSONObject();
                            productObj.put("products", jAResponse);
                            jsonObj.put("children", productObj);
                        }
                        if (facets != null && facets.size() > 0)
                        {
                            JSONObject jArrObj = new JSONObject();
                            for (Map.Entry<String, ArrayList<StreamSearchResponseFacetValue>> entry : facets.entrySet())
                            {
                                String facetName = entry.getKey();
                                int beginLength = 0;
                                if (facetName.startsWith("facet_"))
                                {
                                    beginLength = "facet_".length();
                                }

                                if (facetName.indexOf("_") > -1)
                                {
                                    facetName = facetName.substring(beginLength, facetName.indexOf("_" + (region != null ? region + "_" : "") + campaignId)).replaceAll("_", " ");
                                }
                                //jArrObj.put("name",facetName.split("_")[1]);
                                ArrayList<StreamSearchResponseFacetValue> facetValues = entry.getValue();
                                JSONArray facetValuesArr = new JSONArray();
                                for (StreamSearchResponseFacetValue facetValue : facetValues)
                                {
                                    JSONObject jObj = new JSONObject();
                                    jObj.put("name", facetValue.getName());
                                    jObj.put("count", facetValue.getCount());
                                    facetValuesArr.put(jObj);
                                }
                                if (facetValuesArr.length() > 0)
                                {
                                    jArrObj.put(facetName, facetValuesArr);
                                }
                            }
                            jsonObj.put("facets", jArrObj);
                        }
                        jsonObj.put("responseCode", "0");
                    }
                }
                toRet = new Response("application/json; charset=utf-8", jsonObj.toString().getBytes("UTF-8"));
                toRet.setResponseCode(0);
            }
        }
        catch (Exception e)
        {
            if (e instanceof ServerException)
            {
                throw (ServerException) e;
            }
            else
            {
                throw new ServerException(e);
            }
        }
        return toRet;
    }

    @Override
    public Response format(StreamSearchResponse result1,
                           StreamSearchResponse result2,
                           int limit,
                           UserV2 user,
                           DBSessionManager dbSessionManager,
                           Campaign campaign) throws ServerException
    {
        return format(result1, result2, limit, user, dbSessionManager, campaign, null);
    }

    public Response format(StreamSearchResponse result1,
                           StreamSearchResponse result2,
                           int limit,
                           UserV2 user,
                           DBSessionManager dbSessionManager,
                           Campaign campaign,
                           String vendor) throws ServerException
    {
        return null;
    }

    @Override
    public Response format(StreamSearchResponse searchResult,
                           UserV2 user,
                           Campaign campaign,
                           Map<String, Object> dataMap)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
