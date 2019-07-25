/*******************************************************************************
 * Copyright Â©2002-2014 Skava. 
 * All rights reserved.The Skava system, including 
 * without limitation, all software and other elements
 * thereof, are owned or controlled exclusively by
 * Skava and protected by copyright, patent, and 
 * other laws. Use without permission is prohibited.
 * 
 *  For further information contact Skava at info@skava.com.
 ******************************************************************************/
package com.skava.search.service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.json.JSONArray;
import org.json.JSONObject;

import com.skava.interfaces.SearchService;
import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.http.SkavaHttpResponse;
import com.skava.model.searchv2.SearchConstants;
import com.skava.model.searchv2.StreamIndexDocument;
import com.skava.model.searchv2.StreamIndexRequest;
import com.skava.model.searchv2.StreamIndexResponse;
import com.skava.model.searchv2.StreamSearchConfig;
import com.skava.model.searchv2.StreamSearchGroupResult;
import com.skava.model.searchv2.StreamSearchQuery;
import com.skava.model.searchv2.StreamSearchQueryCondition;
import com.skava.model.searchv2.StreamSearchResponse;
import com.skava.model.searchv2.StreamSearchResponseFacetValue;
import com.skava.model.searchv2.StreamSearchResult;
import com.skava.model.searchv2.solr.SolrIndexResponse;
import com.skava.model.searchv2.solr.SolrSearchResponse;
import com.skava.model.searchv2.solr.SolrSearchResponseGroupItem;
import com.skava.model.searchv2.solr.SolrSearchResponseGroupModel;
import com.skava.model.searchv2.solr.SolrSearchResponseModel;
import com.skava.searchv2.StreamSearchV2ServiceImplKraft;
import com.skava.services.StreamSearchService;
import com.skava.util.CastUtil;
import com.skava.util.EncodeUtil;
import com.skava.util.JSONUtils;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.SolrUtil;
import com.skava.util.StringUtil;
import com.skava.util.URLUtil;
import com.skava.util.Utilities;

public class SearchServiceSolrImplKraft implements SearchService
{
    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());

    private static final String ROOT_NODE_SPELL_CEHCK = "spellcheck";
    private static final String ROOT_NODE_SUGGEST = "suggest";
    private static final String SEARCH_NODE_SUGGESTIONS = "suggestions";
    private static final String SEARCH_NODE_SUGGESTION = "suggestion";
    private static final String SEARCH_NODE_SUGGESTION_TERM = "term";
    private static final String SEARCH_NODE_SUGGESTION_WEIGHT = "weight";

    private static final String FACET_SORT_TYPE_COUNT = "count";
    private static final String FACET_SORT_TYPE_INDEX = "index";

    public String getQuery(StreamSearchQuery query, Campaign campaign)
    {
        StringBuffer sb = new StringBuffer();
        if (query != null && query.getConditions() != null)
        {
            HashMap<String, Object> customBoostConfig = null;
            String customBoostConfigStr = campaign.getProperty(CampaignProperties.PROP_SEARCH_CUSTOM_BOOSTCONFIG);
            if (customBoostConfigStr != null)
            {
                try
                {
                    JSONObject jObj = new JSONObject(customBoostConfigStr);
                    customBoostConfig = CastUtil.jsonTohashMap(jObj);
                }
                catch (Exception e)
                {
                    logger.info("Invalid Boost Config format " + customBoostConfigStr);
                }
            }
            sb.append(getQuery(query.getConditions(), customBoostConfig));
        }
        return sb.toString();
    }

    private String getQuery(ArrayList<StreamSearchQueryCondition> conditions,
                           HashMap<String, Object> customBoostConfig)
    {
        StringBuffer sb = new StringBuffer();
        List<StreamSearchQueryCondition> facetCond = new ArrayList<>();
        if (conditions != null && conditions.size() > 0)
        {
            sb.append("(");
            for (int i = 0; i < conditions.size(); i++)
            {
                if (conditions.get(i) != null && conditions.get(i).getConditions() != null && conditions.get(i).getConditions().size() > 0)
                {
                    String fieldName = conditions.get(i).getConditions().get(0).getFieldName();
                    if(fieldName != null && fieldName.startsWith(StreamSearchV2ServiceImplKraft.FACET_FILTER_PREFIX))
                    {
                        facetCond.add(conditions.get(i));
                    }
                    else
                    {
                        sb.append(getQueryCondition(i, conditions.get(i), customBoostConfig));
                    }
                }
                else
                {
                    sb.append(getQueryCondition(i, conditions.get(i), customBoostConfig));
                }
            }
            sb.append(")");
            if(!facetCond.isEmpty())
            {
                StringBuffer facetCondQuery = new StringBuffer();
                for (int i = 0; i < facetCond.size(); i++)
                {
                    facetCondQuery.append(getQueryCondition(i, facetCond.get(i), customBoostConfig));
                }
                sb.append("&fq=" + facetCondQuery.toString());
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String getQueryCondition(int index,
                                    StreamSearchQueryCondition condition,
                                    HashMap<String, Object> customBoostConfig)
    {
        boolean fieldInCustomBoostConfig = (condition != null && customBoostConfig != null && customBoostConfig.containsKey(condition.getFieldName()));
        StringBuffer sb = new StringBuffer();
        if ((customBoostConfig != null && fieldInCustomBoostConfig) || customBoostConfig == null || (customBoostConfig != null && condition.getFieldName() == null) || !condition.isSearchTerm())
        {
            if (condition != null && (condition.getConditions() != null || condition.getFieldName() != null))
            {
                if (index != 0 /*&& !condition.isAnd()*/)
                {
                    sb.append(" ");
                }

                if (condition.isNot())
                {
                    sb.append("-");
                }
                else if (condition.isAnd())
                {
                    sb.append("+");
                }

                if (condition.getConditions() != null)
                {
                    sb.append(getQuery(condition.getConditions(), customBoostConfig));
                }
                else
                {
                    if (condition.getFieldName() != null)
                    {
                        if (condition.getValue() != null)
                        {
                            sb.append(SolrUtil.escapeQueryChars(condition.getFieldName()));
                            Object obj = condition.getValue();
                            if (obj instanceof ArrayList<?>)
                            {
                                sb.append(":");
                                ArrayList<Object> values = (ArrayList<Object>) obj;
                                sb.append("(");
                                for (int idx = 0; idx < values.size(); idx++)
                                {
                                    if (idx != 0)
                                    {
                                        sb.append(" ");
                                    }
                                    sb.append(getValue(SolrUtil.escapeQueryChars(values.get(idx)), "", condition.isWildcard()));
                                }
                                sb.append(")");
                            }
                            else
                            {
                                sb.append(":");
                                sb.append(getValue(SolrUtil.escapeQueryChars(condition.getValue()), "", condition.isWildcard()));
                                if (customBoostConfig != null && customBoostConfig.containsKey(condition.getFieldName()) && customBoostConfig.get(condition.getFieldName()) instanceof String && Utilities.isDouble(String.valueOf(customBoostConfig.get(condition.getFieldName()))))
                                {
                                    condition.setBoost(ReadUtil.getFloat(String.valueOf(customBoostConfig.get(condition.getFieldName())), condition.getBoost()));
                                }
                                if (condition.getBoost() > 0.0f)
                                {
                                    sb.append("^" + condition.getBoost());
                                }
                            }
                        }
                        else if (condition.getMinValue() != null || condition.getMaxValue() != null)
                        {
                            sb.append(SolrUtil.escapeQueryChars(condition.getFieldName()));
                            sb.append(":");
                            sb.append("[");
                            sb.append(getValue(condition.getMinValue(), "*", condition.isWildcard()));
                            sb.append(" TO ");
                            sb.append(getValue(condition.getMaxValue(), "*", condition.isWildcard()));
                            sb.append("]");
                        }
                        else
                        {
                            sb.append(getValue(SolrUtil.escapeQueryChars(condition.getFieldName()), "", condition.isWildcard()));
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    private Object getValue(Object value, String defaultValue, boolean wildcard)
    {
        StringBuffer sb = new StringBuffer();
        if (value == null)
        {
            sb.append(defaultValue);
        }
        else if (value instanceof Integer || value instanceof Float || wildcard)
        {
            sb.append(value);
        }
        else
        {
            sb.append("\"");
            sb.append(value);
            sb.append("\"");
        }
        return sb.toString();
    }

    public String generateSearchURL(StreamSearchConfig config,
                                    StreamSearchQuery query,
                                    String image,
                                    String[] imageField,
                                    int offset,
                                    int limit,
                                    String group,
                                    String[] facet,
                                    String sort,
                                    Campaign campaign,
                                    String searchterm,
                                    boolean isSpellCheck,
                                    boolean disableFacetLimit) throws ServerException
    {
        String[] searchUrls = config.getServiceUrlswithCollectionName();
        String searchUrl = null;
        String shardsURL = null;

        if (searchUrls.length > 0)
        {
            searchUrl = searchUrls[0];
            if (searchUrls.length > 1)
            {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < searchUrls.length; i++)
                {
                    if (i != 0)
                    {
                        sb.append(",");
                    }
                    sb.append(searchUrls[i]);
                }
                shardsURL = sb.toString();
            }
        }

        String facetField = null;
        if (facet != null && facet.length > 0)
        {

            StringBuffer facetFieldBuffer = new StringBuffer();
            boolean disableMinCountFacets = campaign.getBooleanProperty(CampaignProperties.PROP_DISABLE_MIN_COUNT_FACETS, false);
            int facetMinCount = !disableMinCountFacets ? 1 : 0;
            facetFieldBuffer.append("&facet=true&facet.mincount=" + facetMinCount);
            if(disableFacetLimit)
            {
                facetFieldBuffer.append("&facet.limit=-1");
            }
            String facetSort = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_SEARCH_FACET_SORT_TYPE), FACET_SORT_TYPE_COUNT);
            facetSort = FACET_SORT_TYPE_INDEX.equals(facetSort) ? facetSort : FACET_SORT_TYPE_COUNT;
            facetFieldBuffer.append("&facet.sort=").append(facetSort);
            JSONObject facetFieldSortJobj = JSONUtils.getJSONObjectFromString(ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_SEARCH_FACET_FIELD_SORT), null), new JSONObject());//TODO
            for (int i = 0; i < facet.length; i++)
            {
                String facetFieldValue = facet[i];
                facetFieldValue = EncodeUtil.urlEncode(facetFieldValue, false);
                facetFieldBuffer.append("&facet.field=" + facetFieldValue);
                if(facetFieldSortJobj.has(facetFieldValue))
                {
                    facetFieldBuffer.append(StringUtil.addStrings("&f.", facetFieldValue, ".facet.sort=", JSONUtils.safeGetJSONProperty(facetFieldValue, facetFieldSortJobj)));
                }
            }
            facetField = facetFieldBuffer.toString();
        }

        String groupFilterQuery = null;
        String groupFacetQuery = null;
        if (group != null)
        {
            String filterSort = "";
            if (sort != null /*&& sort.startsWith("sort_")*/) //commented because ant code doesn't contains this condition, also only getproducts has sort_. getevents doesn't has sort_.
            {
                filterSort = " sort='" + sort + "'"; // default string
                String[] sortSplit = sort.trim().split(" ");
                String sortOrder = sortSplit[sortSplit.length - 1];
                String collapseSortNumTypeKeys = campaign.getProperty(CampaignProperties.PROP_SEARCH_COLLAPSE_SORT_NUMTYPEKEYS); //customizing query for numeric field sort inside collapse https://cwiki.apache.org/confluence/display/solr/Collapse+and+Expand+Results
                if(collapseSortNumTypeKeys != null && sortSplit != null && sortSplit.length > 0)
                {
                    HashSet<String> collapseSortNumTypeKeysSet = new HashSet<String>(Arrays.asList(Utilities.getStringsWithDelims(collapseSortNumTypeKeys, ",", false)));
                    String sortKeystr = sortSplit[0]; 
                    if(collapseSortNumTypeKeysSet != null && sortKeystr != null && collapseSortNumTypeKeysSet.contains(sortKeystr))
                    {
                        Iterator<String> iter = collapseSortNumTypeKeysSet.iterator();
                        while(iter.hasNext())
                        {
                            if((iter.next()).equalsIgnoreCase(sortKeystr))
                            {
                                filterSort = (sortOrder.equals("asc") ? " min='" : " max='") + sortKeystr + "'";
                                break;
                            }
                        }
                    }
                }
            }
            boolean skavafacet = campaign.getBooleanProperty(CampaignProperties.PROP_ENABLE_SKAVA_FACET, false);
            groupFilterQuery = "{!collapse%20field=" + group + filterSort + (skavafacet ? " cost=102}" : "}");
            boolean expand = campaign.getBooleanProperty(CampaignProperties.PROP_ENABLE_EXPAND, false);
            if (expand)
            {
                groupFilterQuery += "&expand=true&expand.rows=100";
            }
            if (skavafacet)
            {
                groupFacetQuery = "{!skavacollapse%20field=" + group + " cost=101}";
                facetField = null;
            }
        }

        return "http://" + searchUrl + "/select?" + (shardsURL != null ? "&shards=" + EncodeUtil.urlDecoder(shardsURL) : "") + (groupFacetQuery != null ? "&fq=" + groupFacetQuery : "") + (groupFilterQuery != null ? "&fq=" + groupFilterQuery : "") + ((facetField != null && facetField.length() > 0) ? facetField : "") + "&wt=json&start=" + offset + "&rows=" + limit + "&spellcheck=" + (isSpellCheck && searchterm != null && searchterm.length() > 0 ? "true&spellcheck.count=20&spellcheck.q=" + searchterm : "false");
    }

    public String generateAdditionalSuggestDicturl(StreamSearchConfig config,
                                                   long campaignId,
                                                   String searchterm,
                                                   int offset,
                                                   int limit,
                                                   boolean useSuggester,
                                                   String region,
                                                   String catalogId,
                                                   long storeId) throws ServerException
    {
        String toRet = null;
        String methodName = "generateAdditionalSuggestDicturl";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), methodName, 0, null, null, "SkavaSearchService - {} :  config - {}, campaignId - {}, searchterm - {}, offset - {}, limit - {}, useSuggester - {}, region - {}, storeId - {}, catalogId - {}", null, false, null, this.getClass().getSimpleName(), config, campaignId, searchterm, offset, limit, useSuggester, region, catalogId);
        String[] searchUrls = config.getServiceUrlswithCollectionName();
        if (searchUrls.length > 0 && searchUrls[0] != null)
        {
            StringBuffer suggestUrl = new StringBuffer();
            suggestUrl.append("http://");
            suggestUrl.append(searchUrls[0]);
            suggestUrl.append(useSuggester ? "/suggestdictbysuggestor" : "/suggestdict");
            suggestUrl.append(campaignId);
            if(catalogId != null)
            {
                suggestUrl.append("_");
                suggestUrl.append(catalogId);
            }
            if(region != null)
            {
            	suggestUrl.append("_");
            	suggestUrl.append(region);
            }
            
            if(storeId > 0)
            {
                suggestUrl.append("_");
                suggestUrl.append(storeId);
            }
            suggestUrl.append(useSuggester ? "?q=" : "?spellcheck.q=");
            suggestUrl.append(URLUtil.browserEncodeEscapingUnsafeCharacters(searchterm));
            suggestUrl.append("&wt=json");
            suggestUrl.append(useSuggester ? "&suggest.count=" : "&spellcheck.count=");
            suggestUrl.append((limit > 0 ? limit : ""));
            toRet = suggestUrl.toString();
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : SuggestDicturl - {} ", null, false, null, this.getClass().getSimpleName(), toRet);
        return toRet;
    }

    public String generateGroupSuggestDicturl(StreamSearchConfig config,
                                              long campaignId,
                                              String searchterm,
                                              String typeOfSuggest,
                                              int offset,
                                              int limit,
                                              String region,
                                              String catalogId,
                                              long storeId) throws ServerException
    {
        String[] searchUrls = config.getServiceUrlswithCollectionName();
        String searchUrl = null;
        if (searchUrls.length > 0)
        {
            searchUrl = searchUrls[0];
        }
        region = ReadUtil.getString(region, null);
        catalogId = ReadUtil.getString(catalogId, null);
        return "http://" + searchUrl + "/" + typeOfSuggest + campaignId + (catalogId != null ? "_" + catalogId :  "")  + (region != null ? "_" + region : "") + (storeId >0 ? "_" + storeId : "") + "?spellcheck.q=" + URLUtil.browserEncodeEscapingUnsafeCharacters(searchterm)/*EncodeUtil.urlEncode(searchterm, false) */ + "&wt=json" + (limit > 0 ? "&spellcheck.count=" + limit : "");
    }

    public String getQueryString(StreamSearchQuery query) throws ServerException
    {
        return getQuery(query.getConditions(), null);
    }

    public String generateIndexURL(StreamSearchConfig config,
                                   StreamIndexRequest indexRequest,
                                   boolean commit)
    {
        String[] serviceURLs = config.getServiceUrlswithCollectionName();
        int serverIdx = (int) (System.currentTimeMillis() % serviceURLs.length);
        return "http://" + serviceURLs[serverIdx] + "/update" + (commit ? "?commit=true" : "");
    }

    public HashMap<String, List<String>> generateSearchParams(StreamSearchConfig config,
                                                              StreamSearchQuery query,
                                                              String image)
    {
        return null;
    }

    public HashMap<String, List<String>> generateIndexParams(StreamSearchConfig config,
                                                             StreamIndexRequest indexRequest)
    {
        return null;
    }

    public HashMap<String, List<String>> generateSearchHeaders(StreamSearchConfig config,
                                                               StreamSearchQuery query,
                                                               String image)
    {
        return null;
    }

    public HashMap<String, List<String>> generateIndexHeaders(StreamSearchConfig config,
                                                              StreamIndexRequest indexRequest)
    {
        return null;
    }

    public String getIndexContentType(StreamSearchConfig config,
                                      StreamIndexRequest indexRequest)
    {
        return "application/json";
    }

    public byte[] getSearchContent(StreamSearchConfig config,
                                   StreamSearchQuery query,
                                   String sort,
                                   String[] imageField,
                                   String image,
                                   int searchDomainType,
                                   boolean enableSKParser,
                                   Campaign campaign,
                                   int offset,
                                   int limit,
                                   String serchTerm,
                                   boolean isSpellCheck,
                                   String contextualParam) throws ServerException
    {
        String toRet = new String();
        if (contextualParam == null)
        {
            if (query != null && (query.getConditions() != null || query.getEdismax() != null) & !isSpellCheck)
            {
                String queryStr = "q=";
                if (query.getConditions() != null)
                {
                    if (enableSKParser && searchDomainType == StreamSearchService.SEARCH_DOMAIN_PRODUCT)
                    {
                        queryStr += "{!skavaparser}";
                    }
                    String[] queries = getQuery(query, campaign).split("&fq=");
                    if(queries.length == 2)
                    {
                        toRet = queryStr + EncodeUtil.urlEncode(queries[0], false) + "&fq=(" + EncodeUtil.urlEncode(queries[1], false) + ")";
                    }
                    else
                    {
                        toRet = queryStr + EncodeUtil.urlEncode(queries[0], false);
                    }
                }

                if (query.getEdismax() != null)
                {
                    toRet = toRet + EncodeUtil.urlEncode(" " + query.getEdismax(), false);
                }
            }

            if (query != null && query.getAdditionalParams() != null)
            {
                toRet = (toRet != null ? toRet : "") + "&" + query.getAdditionalParams();
            }

            if (query != null && query.getQf() != null)
            {
                toRet = (toRet != null ? toRet : "") + "&qf=" + EncodeUtil.urlEncode(query.getQf(), false);
            }

            if (query != null && query.getBq() != null)
            {
                toRet = (toRet != null ? toRet : "") + "&bq=" + EncodeUtil.urlEncode(query.getBq(), false);
            }
        }
        else
        {
            String queryStr = "q=";
            if (enableSKParser && searchDomainType == StreamSearchService.SEARCH_DOMAIN_PRODUCT)
            {
                queryStr += "{!skavaparser}";
            }

            try
            {
                toRet = queryStr + URLEncoder.encode(contextualParam, "UTF-8");
                if (query != null && query.getEdismax() != null)
                {
                    toRet = toRet + EncodeUtil.urlEncode(" " + query.getEdismax(), false);
                }
                if (query != null && query.getAdditionalParams() != null)
                {
                    toRet = (toRet != null ? toRet : "") + "&" + query.getAdditionalParams();
                }

                if (query != null && query.getQf() != null)
                {
                    toRet = (toRet != null ? toRet : "") + "&qf=" + EncodeUtil.urlEncode(query.getQf(), false);
                }

                if (query != null && query.getBq() != null)
                {
                    toRet = (toRet != null ? toRet : "") + "&bq=" + EncodeUtil.urlEncode(query.getBq(), false);
                }
            }
            catch (Exception e)
            {
                logger.error("URL DECODE ERROR", e);
            }
        }
        if (sort != null)
        {
            toRet = (toRet != null ? toRet : "") + "&sort=" + EncodeUtil.urlEncode((sort != null ? sort : "score desc") + ",id asc", false);
        }
        return toRet.getBytes();
    }

    public byte[] getIndexContent(StreamSearchConfig config,
                                  StreamIndexRequest indexRequest) throws ServerException
    {
        JSONObject toRet = null;
        String addJsonStr = "";
        boolean isDeleteProcess = false;
        try
        {
            ArrayList<StreamIndexDocument> docs = indexRequest.getDocs();
            if (docs != null && docs.size() > 0)
            {
                toRet = new JSONObject();
                JSONArray toArr = new JSONArray();

                for (int i = 0; i < docs.size(); i++)
                {
                    StreamIndexDocument temp = docs.get(i);
                    try
                    {
                        if (StreamIndexDocument.OPERATION_DELETE.equals(temp.getType()))
                        {
                            isDeleteProcess = true;
                            toArr.put(getDeleteIndexContent(docs.get(i)));
                        }
                        else if (StreamIndexDocument.OPERATION_DELETE_QUERY.equals(temp.getType()))
                        {
                            isDeleteProcess = true;
                            toArr.put(getDeleteByQueryIndexContent(docs.get(i)));
                        }
                        else
                        {
                            if (addJsonStr.length() > 0)
                            {
                                addJsonStr = addJsonStr + "," + getAddIndexContent(docs.get(i));
                            }
                            else
                            {
                                addJsonStr = getAddIndexContent(docs.get(i));
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        throw new ServerException(e);
                    }
                }
                if (!isDeleteProcess)
                {
                    addJsonStr = "{\"add\":[" + addJsonStr + "]}";
                }
                else
                {
                    toRet.put("delete", toArr);
                }
            }
        }
        catch (Exception e)
        {
            throw new ServerException(e);
        }
        if (isDeleteProcess)
        {
            return (toRet != null ? toRet.toString().getBytes() : null);
        }
        else
        {
            return (addJsonStr.length() > 0 ? addJsonStr.getBytes() : null);
        }
    }

    private String getAddIndexContent(StreamIndexDocument streamIndexDocument) throws ServerException
    {
        JSONObject toRet = null;
        String toRetStr = null;
        if (streamIndexDocument != null)
        {
            try
            {
                HashMap<String, Object> facets = streamIndexDocument.getFacets();

                if (streamIndexDocument.getProperties() != null)
                {
                    facets.putAll(streamIndexDocument.getProperties());
                }
                toRet = new JSONObject(CastUtil.toJSON(facets));

                if (facets != null && facets.get("tag") != null)
                {
                    if (facets.get("tag") instanceof String[])
                    {
                        String[] tags = (String[]) facets.get("tag");
                        boolean isDynamicTag = false;
                        for (int i = 0; i < tags.length; i++)
                        {
                            JSONObject tagJson = JSONUtils.getJSONObjectFromString(tags[i], null);
                            if (tagJson != null) // if tag is a JsonObject with boost for CJE dynamic tag 
                            {
                                toRet.put("tag_" + tagJson.get("value"), tagJson);
                                isDynamicTag = true;
                            }
                        }
                        if (isDynamicTag)
                        {
                            JSONObject availableJson = (JSONObject) JSONUtils.getJSONObjectFromString(JSONUtils.safeGetStringValue(toRet, "available", "{'value':'true','boost':1}"), null);
                            toRet.put("available", availableJson);
                        }
                    }
                }

                if (streamIndexDocument.getId() != null)
                {
                    toRet.put("id", streamIndexDocument.getId());
                }

                toRetStr = toRet.toString();
                //TO manipulate index field manually since we need to pass the json with duplicate key index.
                JSONArray jArrSpell = null;
                if (toRet.has("index"))
                {
                    JSONArray jArr = (JSONArray) toRet.get("index");
                    if (toRet.has("index_spellcheck"))
                    {
                        jArrSpell = toRet.getJSONArray("index_spellcheck");
                        toRet.remove("index_spellcheck");
                    }
                    toRet.remove("index");
                    String jsonstr = toRet.toString();
                    toRetStr = jsonstr.substring(0, jsonstr.length() - 1);
                    for (int i = 0; i < jArr.length(); i++)
                    {
                        if (jArr.get(i) != JSONObject.NULL)
                        {
                            toRetStr = toRetStr + ",\"index\":" + jArr.getString(i);
                        }
                    }
                    toRetStr = toRetStr + "}";
                }
                if (jArrSpell != null && jArrSpell.length() > 0)
                {
                    toRetStr = toRetStr.substring(0, toRetStr.length() - 1);
                    for (int i = 0; i < jArrSpell.length(); i++)
                    {
                        if (jArrSpell.get(i) != JSONObject.NULL)
                        {
                            toRetStr = toRetStr + ",\"index_spellcheck\":" + jArrSpell.getString(i);
                        }
                    }
                    toRetStr = toRetStr + "}";
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
        }
        return toRetStr;
    }

    private JSONObject getDeleteIndexContent(StreamIndexDocument streamIndexDocument) throws ServerException
    {
        JSONObject toRet = null;
        if (streamIndexDocument != null && streamIndexDocument.getId() != null)
        {
            try
            {
                toRet = new JSONObject();
                toRet.put("id", streamIndexDocument.getId());
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
        }
        return toRet;
    }

    private JSONObject getDeleteByQueryIndexContent(StreamIndexDocument streamIndexDocument) throws ServerException
    {
        JSONObject toRet = null;
        if (streamIndexDocument != null && streamIndexDocument.getQueryString() != null)
        {
            try
            {
                toRet = new JSONObject();
                toRet.put("query", streamIndexDocument.getQueryString());
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
        }
        return toRet;
    }

    public String getSearchMethod(StreamSearchConfig config,
                                  StreamSearchQuery query,
                                  String image)
    {
        return "GET";
    }

    public String getIndexMethod(StreamSearchConfig config,
                                 StreamIndexRequest indexRequest)
    {
        return "POST";
    }

    public String getSearchRemoteHost(StreamSearchConfig config,
                                      StreamSearchQuery query,
                                      String image)
    {
        return null;
    }

    public String getIndexRemoteHost(StreamSearchConfig config,
                                     StreamIndexRequest indexRequest)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public StreamSearchResponse parseSearchResult(StreamSearchConfig config,
                                                  SkavaHttpResponse skResponse,
                                                  String group) throws ServerException
    {
        StreamSearchResponse toRet = null;
        byte[] responseData = skResponse.getContent();
        if (responseData != null)
        {
            SolrSearchResponse response = (SolrSearchResponse) CastUtil.fromJSON(new String(responseData), SolrSearchResponse.class);

            int numEntries = 0;
            LinkedHashMap<String, ArrayList<StreamSearchGroupResult>> groups = null;
            if (group != null && response.getGrouped() != null && response.getGrouped().containsKey(group))
            {
                groups = new LinkedHashMap<String, ArrayList<StreamSearchGroupResult>>();
                numEntries = response.getGrouped().get(group).getNgroups();
                HashMap<String, SolrSearchResponseGroupModel> groupsTemp = response.getGrouped();
                Iterator<Map.Entry<String, SolrSearchResponseGroupModel>> groupsTempIterator = groupsTemp.entrySet().iterator();
                while (groupsTempIterator.hasNext())
                {
                    Map.Entry<String, SolrSearchResponseGroupModel> entry = groupsTempIterator.next();
                    SolrSearchResponseGroupModel model = entry.getValue();

                    ArrayList<StreamSearchGroupResult> groupItems = null;
                    if (model.getGroups() != null)
                    {
                        groupItems = new ArrayList<StreamSearchGroupResult>();
                        ArrayList<SolrSearchResponseGroupItem> modelGroups = model.getGroups();
                        for (int i = 0; i < modelGroups.size(); i++)
                        {
                            SolrSearchResponseGroupItem item = modelGroups.get(i);
                            ArrayList<HashMap<String, Object>> docs = item.getDoclist().getDocs();
                            groupItems.add(new StreamSearchGroupResult(item.getGroupValue(), item.getDoclist().getNumFound(), getStreamSearchResults(docs)));
                        }

                    }
                    groups.put(entry.getKey(), groupItems);
                }
            }

            ArrayList<StreamSearchResult> results = null;
            if (response.getResponse() != null)
            {
                numEntries = response.getResponse().getNumFound();
                ArrayList<HashMap<String, Object>> docs = response.getResponse().getDocs();
                results = getStreamSearchResults(docs);
            }

            HashMap<String, ArrayList<StreamSearchResult>> expanded = null;
            if (response.getExpanded() != null)
            {
                expanded = new HashMap<String, ArrayList<StreamSearchResult>>();
                Iterator<Map.Entry<String, SolrSearchResponseModel>> itr = response.getExpanded().entrySet().iterator();
                while (itr.hasNext())
                {
                    Entry<String, SolrSearchResponseModel> expandKey = itr.next();
                    if (expandKey.getKey() != null && expandKey.getValue() != null)
                    {
                        ArrayList<HashMap<String, Object>> docs = expandKey.getValue().getDocs();
                        expanded.put(expandKey.getKey(), getStreamSearchResults(docs));
                    }
                }
            }

            HashMap<String, ArrayList<StreamSearchResponseFacetValue>> facets = null;
            if (response.getFacet_counts() != null && response.getFacet_counts().getFacet_fields() != null)
            {
                facets = new HashMap<String, ArrayList<StreamSearchResponseFacetValue>>();
                HashMap<String, String[]> responseFacets = response.getFacet_counts().getFacet_fields();
                Iterator<String> itr = responseFacets.keySet().iterator();
                while (itr.hasNext())
                {
                    String facetKey = itr.next();
                    String[] facetArr = responseFacets.get(facetKey);
                    ArrayList<StreamSearchResponseFacetValue> facetValues = new ArrayList<StreamSearchResponseFacetValue>();
                    for (int i = 0; i < facetArr.length; i += 2)
                    {
                        facetValues.add(new StreamSearchResponseFacetValue(facetArr[i], ReadUtil.getInt(facetArr[i + 1], 0)));
                    }
                    facets.put(facetKey, facetValues);
                }
            }
            String errorMessage = null;
            String errorCode = null;
            if (response.getError() != null && response.getError().getMsg() != null)
            {
                errorMessage = response.getError().getMsg();
                errorCode = String.valueOf(response.getError().getCode());
            }

            HashMap<String, String> spellcheck = new HashMap<String, String>();
            try
            {
                if (response.getSpellcheck() != null && response.getSpellcheck().size() > 0)
                {
                    HashMap<String, Object> spellcheckSugg = response.getSpellcheck();
                    ArrayList<Object> arrObj = (ArrayList<Object>) spellcheckSugg.get("suggestions");

                    for (int i = 0; i < arrObj.size(); i++)
                    {
                        String searchterm = (String) arrObj.get(i);
                        i++;
                        Object temp = arrObj.get(i);
                        if (temp instanceof Map)
                        {
                            HashMap<String, Object> hash = (HashMap<String, Object>) arrObj.get(i);
                            ArrayList<Object> arrObjFinal = (ArrayList<Object>) hash.get("suggestion");
                            String suggWord = (String) arrObjFinal.get(0);
                            spellcheck.put(searchterm, suggWord);
                        }
                        else if (temp instanceof String && searchterm.equalsIgnoreCase("collation"))
                        {
                            spellcheck.put(searchterm, (String) temp);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throw new ServerException("Error on generating Spellcheck object", e);
            }

            toRet = new StreamSearchResponse(numEntries, results, groups, facets, null, null, null, null, errorMessage, errorCode, spellcheck, expanded);
        }
        return toRet;
    }

    public StreamIndexResponse parseIndexResult(SkavaHttpResponse skResponse) throws ServerException
    {
        StreamIndexResponse toRet = null;
        byte[] responseData = skResponse.getContent();
        if (responseData != null)
        {
            SolrIndexResponse response = (SolrIndexResponse) CastUtil.fromJSON(new String(responseData), SolrIndexResponse.class);
            int respCode = 0;
            String respMsg = Response.RESPONSE_MSG_SUCCESS;
            if (response.getError() != null)
            {
                respCode = response.getError().getCode();
                respMsg = response.getError().getMsg();
            }
            toRet = new StreamIndexResponse(respCode, respMsg, 0, 0, 0);
        }

        return toRet;
    }

    private ArrayList<StreamSearchResult> getStreamSearchResults(ArrayList<HashMap<String, Object>> docs)
    {
        ArrayList<StreamSearchResult> toRet = null;
        if (docs != null && docs.size() > 0)
        {
            toRet = new ArrayList<StreamSearchResult>();
            for (int i = 0; i < docs.size(); i++)
            {
                toRet.add(getStreamSearchResult(docs.get(i)));
            }
        }
        return toRet;
    }

    private StreamSearchResult getStreamSearchResult(HashMap<String, Object> props)
    {
        StreamSearchResult toRet = null;
        if (props != null && props.size() > 0)
        {
            Iterator<Map.Entry<String, Object>> value = props.entrySet().iterator();
            String id = null;
            HashMap<String, Object> properites = new HashMap<String, Object>();
            while (value.hasNext())
            {
                Map.Entry<String, Object> valueEntry = value.next();
                if (valueEntry.getKey().equals("id"))
                {
                    id = valueEntry.getValue().toString();
                }
                else
                {
                    properites.put(valueEntry.getKey(), valueEntry.getValue());
                }
            }
            toRet = new StreamSearchResult(id, properites);
        }
        return toRet;
    }

    @SuppressWarnings("unchecked")
    public List<String> parseSuggestionDictResult(SkavaHttpResponse skResponse,
                                                  String searchTerm,
                                                  int offset,
                                                  int limit,
                                                  boolean useSuggester) throws ServerException
    {
        LinkedHashMap<String, Float> toRet = new LinkedHashMap<String, Float>();
        String methodName = "parseSuggestionDictResult";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  skResponse - {}, searchTerm - {}, offset - {}, limit - {}, useSuggester - {}", null, false, null, this.getClass().getSimpleName(), skResponse, searchTerm, offset, limit, useSuggester);
        try
        {
            byte[] responseData = skResponse.getContent();
            if (responseData != null)
            {
                String resstr = new String(responseData);
                resstr = resstr.replaceAll("~", " ");
                if (resstr.startsWith("{"))
                {
                    JSONObject obj = new JSONObject(resstr);
                    if (obj.has(useSuggester ? ROOT_NODE_SUGGEST : ROOT_NODE_SPELL_CEHCK))
                    {
                        JSONObject objSpell = (JSONObject) obj.get(useSuggester ? ROOT_NODE_SUGGEST : ROOT_NODE_SPELL_CEHCK);
                        if (useSuggester)
                        {
                            Iterator<String> keyIterator = (Iterator<String>) objSpell.keys();
                            while (keyIterator.hasNext())
                            {
                                JSONObject suggestDict = objSpell.getJSONObject(keyIterator.next());
                                Iterator<String> searchKeyIterator = (Iterator<String>) suggestDict.keys();
                                while (searchKeyIterator.hasNext())
                                {
                                    JSONObject suggestDictItem = suggestDict.getJSONObject(searchKeyIterator.next());
                                    if (suggestDictItem != null && suggestDictItem.has(SEARCH_NODE_SUGGESTIONS))
                                    {
                                        JSONArray objSug = suggestDictItem.getJSONArray(SEARCH_NODE_SUGGESTIONS);
                                        LinkedHashMap<String, Float> tempMap = new LinkedHashMap<String, Float>();
                                        for (int i = 0; i < objSug.length(); i++)
                                        {
                                            JSONObject suggest = objSug.getJSONObject(i);
                                            String term = suggest.getString(SEARCH_NODE_SUGGESTION_TERM);
                                            float boost = (float) suggest.getDouble(SEARCH_NODE_SUGGESTION_WEIGHT);
                                            tempMap.put(term, boost);
                                        }
                                        toRet = mergeBoostedList(toRet, tempMap);
                                    }
                                }
                            }
                        }
                        else
                        {
                            JSONArray objSug = objSpell.getJSONArray(SEARCH_NODE_SUGGESTIONS);
                            for (int i = 0; i < objSug.length(); i++)
                            {
                                if (objSug.getString(i).equals(searchTerm.replaceAll("~", " ")))
                                {
                                    JSONObject suggestionObj = objSug.getJSONObject(i + 1);
                                    if (suggestionObj != null && suggestionObj.has(SEARCH_NODE_SUGGESTION))
                                    {
                                        JSONArray suggestions = suggestionObj.getJSONArray(SEARCH_NODE_SUGGESTION);
                                        if (suggestions != null && suggestions.length() > 0)
                                        {
                                            List<String> list = JSONUtils.getJSONArrayAsList(suggestions);
                                            for (int listIdx = 0; listIdx < list.size(); listIdx++)
                                            {
                                                toRet.put(list.get(listIdx), 0.0f);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.writeLog(Level.ERROR, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : dictResult - {} ", null, false, null, this.getClass().getSimpleName(), toRet);
        return new ArrayList<String>(toRet.keySet());
    }

    public LinkedHashMap<String, Float> mergeBoostedList(LinkedHashMap<String, Float> first,
                                                          LinkedHashMap<String, Float> second)
    {
        LinkedHashMap<String, Float> toRet = new LinkedHashMap<String, Float>();
        Iterator<Entry<String, Float>> firstIterator = null;
        Iterator<Entry<String, Float>> secondIterator = null;
        Entry<String, Float> firstEntry = null;
        Entry<String, Float> secondEntry = null;
        if (first != null)
        {
            firstIterator = first.entrySet().iterator();
        }
        if (second != null)
        {
            secondIterator = second.entrySet().iterator();
        }
        boolean cont = true;
        do
        {
            if (firstEntry == null && firstIterator.hasNext())
            {
                firstEntry = firstIterator.next();
            }
            if (secondEntry == null && secondIterator.hasNext())
            {
                secondEntry = secondIterator.next();
            }
            cont = (firstEntry != null || secondEntry != null);

            if (firstEntry != null && secondEntry != null)
            {
                float firstBoost = firstEntry.getValue();
                float secondBoost = secondEntry.getValue();
                if (firstBoost > secondBoost)
                {
                    toRet.put(firstEntry.getKey(), firstEntry.getValue());
                    firstEntry = null;
                }
                else
                {
                    toRet.put(secondEntry.getKey(), secondEntry.getValue());
                    secondEntry = null;
                }
            }
            else if (firstEntry != null)
            {
                toRet.put(firstEntry.getKey(), firstEntry.getValue());
                firstEntry = null;
            }
            else if (secondEntry != null)
            {
                toRet.put(secondEntry.getKey(), secondEntry.getValue());
                secondEntry = null;
            }
        }
        while (cont);
        return toRet;
    }

    public JSONArray parseSuggestionDictResultArr(SkavaHttpResponse skResponse,
                                                  String searchTerm,
                                                  String outputKey,
                                                  int offset,
                                                  int limit) throws ServerException
    {
        String methodName = "parseSuggestionDictResultArr";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  skResponse - {}, searchTerm - {}, outputKey - {}, offset - {}, limit - {}", null, false, null, this.getClass().getSimpleName(), skResponse, searchTerm, outputKey, offset, limit);
        JSONArray toArr = new JSONArray();
        ArrayList<String> toRet = new ArrayList<String>();
        try
        {
            byte[] responseData = skResponse.getContent();
            if (responseData != null)
            {
                String resstr = new String(responseData);
                resstr = resstr.replaceAll("~", " ");
                if (resstr.startsWith("{"))
                {
                    JSONObject obj = new JSONObject(resstr);
                    if (obj.has("spellcheck"))
                    {
                        JSONObject objSpell = (JSONObject) obj.get("spellcheck");
                        JSONArray objSug = objSpell.getJSONArray("suggestions");
                        for (int i = 0; i < objSug.length(); i++)
                        {
                            if (objSug.getString(i).equals(searchTerm.replaceAll("~", " ")))
                            {
                                JSONObject suggestionObj = objSug.getJSONObject(i + 1);
                                if (suggestionObj != null && suggestionObj.has("suggestion"))
                                {
                                    JSONArray suggestions = suggestionObj.getJSONArray("suggestion");
                                    if (suggestions != null && suggestions.length() > 0)
                                    {
                                        toRet.addAll(JSONUtils.getJSONArrayAsList(suggestions));
                                    }
                                }
                                break;
                            }
                        }
                        if (toRet != null && toRet.size() > 0)
                        {
                            toArr = JSONUtils.getListAsJSONArray(toRet);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
            SkavaLoggerFactory.getLogger(getClass()).info("Error on the Search suggestion result parser:" + e);
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : responseJSONArray - {} ", null, false, null, this.getClass().getSimpleName(), toArr);
        return toArr;
    }
}
