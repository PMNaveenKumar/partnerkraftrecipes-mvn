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
package com.skava.searchv2;

import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Level;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tartarus.snowball.ext.PorterStemmer;

import com.skava.cache.MemCacheFactory;
import com.skava.cache.MemCacheManager;
import com.skava.cache.MemCacheV2;
import com.skava.db.DBSession;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SearchResponseFormatter;
import com.skava.model.index.ProductIndexFields;
import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.dbbeans.Partner;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.PimKraftConstants;
import com.skava.model.pim.ProductCategory;
import com.skava.model.pim.SelectedFacet;
import com.skava.model.pim.SelectedKraftFacet;
import com.skava.model.searchv2.SearchConstants;
import com.skava.model.searchv2.SearchWeightConfig;
import com.skava.model.searchv2.StreamCategoryGroupResponse;
import com.skava.model.searchv2.StreamSearchConfig;
import com.skava.model.searchv2.StreamSearchGroupResult;
import com.skava.model.searchv2.StreamSearchQuery;
import com.skava.model.searchv2.StreamSearchQueryCondition;
import com.skava.model.searchv2.StreamSearchResponse;
import com.skava.model.searchv2.Synonym;
import com.skava.services.HttpClientService;
import com.skava.services.JMQService;
import com.skava.services.SearchSynonymService;
import com.skava.services.StreamCatalogService;
import com.skava.services.StreamSearchKraftService;
import com.skava.services.StreamSearchService;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.util.CampaignUtil;
import com.skava.util.CastUtil;
import com.skava.util.JSONUtils;
import com.skava.util.NWUtil;
import com.skava.util.PartnerUtil;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.SolrUtil;
import com.skava.util.StopAnalyzerUtil;
import com.skava.util.StopAnalyzerUtilKraft;
import com.skava.util.StringUtil;

import lombok.Getter;

public class StreamSearchV2ServiceImplKraft implements StreamSearchV2KraftService
{
    private SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());
    private DBSessionManager dbSessionManager;
    private StreamSearchKraftService streamSearchKraftService;
    private StreamSearchService streamSearchService;

    private SearchSynonymService searchSynonymService;
    public static DecimalFormat df = new DecimalFormat("#0.00");
    private String searchQueueName;
    private JMQService jmqService;
    private HttpClientService httpClientService;
    @Getter private boolean solrCloudMode = false;
    @Getter private boolean lukeFromDb = false;
    private MemCacheManager memCacheManager;

    private TermCurator termCurator = new TermCurator();

    private StreamCatalogService streamCatalogService;
    public static final int NUM_ITEMS_PER_LIST = 50;
    public static final String DEFAULT_SORT = "createdtime desc";
    public static final String FULFILLMENT_TYPE = "fulfillment";
    public static final String NOTIFICATION_TYPE = "notification";

    public static final String[] SYNC_EVENT_TYPES = { "notification", "fulfillment" };
    public static final String DEFAULT_RESULT_FORMATTER_CLASS = "com.skava.util.PropertyResultFormatter";

    public static final java.util.List<String> VALID_SYNC_EVENT_TYPES = Arrays.asList(SYNC_EVENT_TYPES);

    @SuppressWarnings("unused") private static final Map<Integer, String> SHOP_ID_MAP = createMap();

    private static final int NUM_BASIC_PARALLEL_CALLS = 10;
    private static final int NUM_MAX_PARALLEL_CALLS = 10;
    private static final long KEEP_ALIVE_MSECS = 2000;

    private static final String CURATED_FACET_CONFIG_CACHE_KEY = "curatedfacetconfig";
    private static final String CURATED_FACET_CONFIG_CACHE_NAME = "curatedFacetConfig";

    private static final String SYNONYM = "synonym";
    private static final String PRODUCT = "product";
    public static final String SEARCH_VERSION = "v5";
    public static final String LIST_VERSION = "v5";
    public static final String SORT_VERSION = "1";

    private HashMap<String, HashMap<String, ArrayList<JSONObject>>> synPdtMap = new HashMap<String, HashMap<String, ArrayList<JSONObject>>>();

    public static final String FACET_FILTER_PREFIX = "facet_";
    public static final String DEFAULT_SOLR_CATALOGID = "0";

    Executor poolExecutor = new ThreadPoolExecutor(NUM_BASIC_PARALLEL_CALLS, NUM_MAX_PARALLEL_CALLS, KEEP_ALIVE_MSECS, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    CompletionService<StreamCategoryGroupResponse> ecs = new ExecutorCompletionService<StreamCategoryGroupResponse>(poolExecutor);

    //TODO:lak move to global campaign after checking with prasad.
    private static Map<Integer, String> createMap()
    {
        Map<Integer, String> shopIdBrandMap = new HashMap<Integer, String>();
        shopIdBrandMap.put(0, "GOL");
        shopIdBrandMap.put(1, "ONOL");
        shopIdBrandMap.put(2, "BROL");
        shopIdBrandMap.put(3, "GPSV");
        shopIdBrandMap.put(4, "ATOL");
        return Collections.unmodifiableMap(shopIdBrandMap);
    }

    public StreamSearchV2ServiceImplKraft(DBSessionManager dbSessionManager,
                                          StreamSearchKraftService streamSearchKraftService,
                                          StreamSearchService streamSearchService,
                                          SearchSynonymService searchSynonymService,
                                          StreamCatalogService streamCatalogService,
                                          JMQService jmqService,
                                          String searchQueueName,
                                          boolean solrCloudMode,
                                          HttpClientService httpClientService,
                                          MemCacheManager memCacheManager,
                                          boolean lukeFromDb) throws ServerException
    {
        this.dbSessionManager = dbSessionManager;
        this.streamSearchKraftService = streamSearchKraftService;
        this.streamSearchService = streamSearchService;
        this.searchSynonymService = searchSynonymService;
        this.streamCatalogService = streamCatalogService;
        this.jmqService = jmqService;
        this.searchQueueName = searchQueueName;
        this.solrCloudMode = solrCloudMode;
        this.httpClientService = httpClientService;
        this.memCacheManager = memCacheManager;
        this.lukeFromDb = lukeFromDb;
    }

    /**
     * Search service have a defined solr schema to maintain the Product information in solr. We've feed processor to index the Product data in to the solr as per the solr schema.
       Using this API we can retrieve the indexed products from solr. This API supports pagination, filtering and sorting. Using this API we can perform open search, search by product name, color, size, category name and price.This API supports contextual search as well.
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter for all the microservice.
     * @param skuId Skus that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated sku information are responded
     * @param productId Products that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated product information are responded
     * @param name Product name refer to name of the product which is indexed in solr needs to be fetched
     * @param brand Brand name refer to brand name of the product which is indexed in solr needs to be fetched
     * @param category Categories that are product group will have category name. Based on the mentioned category name, indexed products are responded from solr
     * @param categoryid Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded
     * @param categorylevel1 category level 1 refers the top categories.The products with indexed field categorylevel1 matches with the category name will be responded.
     * @param categorylevel2 category level 2 refers the immediate sub categories of top categories.The products with indexed field categorylevel2 matches with the category name will be responded
     * @param categorylevel3 category level 3 refers the immediate sub categories of level 2 categories.The products with indexed field categorylevel3 matches with the category name will be responded
     * @param categorylevel4 category level 4 refers the immediate sub categories of level 3 categories.The products with indexed field categorylevel4 matches with the category name will be responded
     * @param categorylevel5 category level 5 refers the immediate sub categories of level 4 categories.The products with indexed field categorylevel5 matches with the category name will be responded
     * @param division Specifies the products containing the division should be fetched.
     * @param color The products with indexed field color matches with the color parameter value will be responded
     * @param size1 The products with indexed field size1 matches with the size1 parameter value will be responded
     * @param size2 The products with indexed field size2 matches with the size2 parameter value will be responded
     * @param priceMin This parameter accepts Minimum price for the product. The products with indexed field priceMin matches with the priceMin parameter value will be responded
     * @param priceMax This parameter accepts Maximum price for the product. The products with indexed field priceMin matches with the priceMin parameter value will be responded
     * @param variant This parameter accepts the parameter which is making different from other products like special tags
     * @param searchTerm This parameter takes the search term as value which used to perform general search. The products with the search term matched with any indexed field will be responded
     * @param facets This parameter accepts the array of facets 0-th value as true and the remaining value contains facets which groups all the facets and filter the products based on the given facets to respond
     * @param selectedFacets It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.
     * @param customFacets It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be boosted to top from the response.
     * @param sort It is used to apply sorting for the products response from solr based on the particular field. Example: price|desc. This sorts the products with indexed field price in descending order
     * @param group This parameter used to group the products based on the given query parameter values
     * @param responseFormatterClass This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param storeId All the micro services functionality can be customized in store level. Campaign can have multiple stores. Each store can have different set of properties that will influence the functionality of the API. Unique identifier associated for the store will go here
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100.
     * @param usev2 This is a boolean parameter which is used to mention the whether needs to enable index based boosting or not. By default solr using query based boosting
     * @param edismax This is a boolean parameter which is used to mention the whether needs to enable the Extended DisMax query parser or not
     * @param iszeroResult the iszeroResult
     * @param spellcheck This is a boolean parameter which is used to mention whether the check spelling is needed or not
     * @param personalize In case of searching products based on personalized options. Example:- If a user searched the products based on giving search terms and product id and that particular user has red color as favorite one which will load the read color products
     * @param contextualParam Specifies a query to load the exact products
     * @param region Products will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param version This parameter accepts the publish version
     * @param curate the curate
     * @param online This is a boolean parameter which is used to mention the whether the online products will be responded or not
     * @param store This is a boolean parameter which is used to mention the whether the store based products will be responded or not
     * @param advancedSearch This parameter accepts the advanced key search in case of searching dynamic indexed fields
     * @param disableFacetMinCount This parameter used to retain the filter options even after applying the filters. The facet options won't change even after the filter applied.
     * @param includeGhostProduct It is boolean parameter to mention that mention that whether products that are invisible needs to be honored while considering the category to respond.
     * @param includeFacet  This parameter used to add additional filterable facet
     * @param disableFacetLimit The solr gives the minimum 100 filter options. This parameter used to disable the limit of solr filter option restriction
     * @param segments It refers to user segments, by this the products that are associated to the requested segments alone will be considered for the products to respond
     * @param catalogId All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @SuppressWarnings("unchecked")
    public Response getProductsForKraft(HttpServletRequest request,
                                        long storeId,
                                        String[] skuId,
                                        String[] productId,
                                        String name,
                                        String[] brand,
                                        String[] category,
                                        String[] categoryid,
                                        String categorylevel1,
                                        String categorylevel2,
                                        String categorylevel3,
                                        String categorylevel4,
                                        String categorylevel5,
                                        String[] division,
                                        String[] color,
                                        String size1,
                                        String size2,
                                        float[] priceMin,
                                        float[] priceMax,
                                        String variant,
                                        String searchTerm,
                                        String[] facets,
                                        SelectedKraftFacet[] selectedFacets,
                                        SelectedKraftFacet[] customFacets,
                                        String sort,
                                        String group,
                                        String responseFormatterClass,
                                        int offset,
                                        int limit,
                                        boolean usev2,
                                        boolean edismax,
                                        boolean iszeroResult,
                                        boolean spellcheck,
                                        boolean personalize,
                                        String contextualParam,
                                        String region,
                                        String version,
                                        SelectedFacet[] curate,
                                        boolean online,
                                        boolean store,
                                        SelectedFacet[] advancedSearch,
                                        boolean disableFacetMinCount,
                                        boolean includeGhostProduct,
                                        String includeFacet,
                                        boolean disableFacetLimit,
                                        String[] segments,
                                        String catalogId,
                                        String similarType,
                                        boolean spellCheckOnly) throws ServerException
    {
        Response toRet = null;
        String selectedSort = null;
        String sortStrTemp = null;
        String sortOrder = null;
        String sortNumTemp = null;
        List<String> prefacet = null;
        List<String> presort = null;
        String[] sortArr = null;
        String methodName = "getProducts";

        try
        {
            logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  request - {}, storeId - {}, skuId - {}, productId - {}, name - {}, brand - {}, category - {}, categoryid - {}, categorylevel1 - {}, categorylevel2 - {}, categorylevel3 - {}, categorylevel4 - {}, categorylevel5 - {}, division - {}, color - {}, size1 - {}, size2 - {}, priceMin - {}, priceMax - {}, variant - {}, searchTerm - {}, facets - {}, selectedFacets - {}, sort - {}, group - {}, responseFormatterClass - {},  offset - {}, limit - {}, usev2 - {}, edismax - {}, iszeroResult - {}, spellcheck - {32}, personalize - {}, contextualParam - {}, region - {}, version - {}, curate - {}, online - {}, store - {}, advancedSearch - {}, disableFacetMinCount - {}, includeGhostProduct - {}, includeFacet - {}, disableFacetLimit - {}, segments - {}, catalogId - {}", null, false, null, this.getClass().getSimpleName(), request, storeId, skuId, productId, name, brand, category, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, color, size1, size2, priceMin, priceMax, variant, searchTerm, facets, selectedFacets, sort, group, responseFormatterClass, offset, limit, usev2, edismax, iszeroResult, spellcheck, personalize, contextualParam, region, version, curate, online, store, advancedSearch, disableFacetMinCount, includeGhostProduct, includeFacet, disableFacetLimit, segments, catalogId);
            searchTerm = ReadUtil.getString(searchTerm, null);
            request.setAttribute("searchterm", searchTerm != null ? searchTerm.toLowerCase() : searchTerm);
            region = ReadUtil.getString(region, null);
            request.setAttribute("region", region);
            Campaign campaign = getCampaign(dbSessionManager, storeId, "pim");
            if (campaign == null || campaign.getId() <= 0) { throw new ServerException("Invalid Campaign"); }
            long campaignId = campaign.getId();
            Partner partner = PartnerUtil.loadPartner(dbSessionManager, campaign.getPartnerid());
            if (partner == null || partner.getId() <= 0) { throw new ServerException("Invalid Partner"); }
            
            if(catalogId == null)
            {
                DBSession dbSession = dbSessionManager.getReadOnlyDBSession();
                try
                {
                    catalogId = CampaignUtil.getPimCatalogId(dbSession, storeId);
                    catalogId = ReadUtil.getString(catalogId, null) == null ? DEFAULT_SOLR_CATALOGID : catalogId;
                }
                catch (Exception e)
                {
                    dbSession.endSession();
                    logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaKraftSearchService - {} :  Catalogid - {}", null, false, null, this.getClass().getSimpleName(), null);
                }
                finally
                {
                    dbSession.endSession();
                }
            }
            
            Synonym synonym = null;
            if (searchSynonymService != null && searchSynonymService.isAllowSynonom() && searchTerm != null && searchTerm.length() > 0)
            {
                synonym = searchSynonymService.getSynonym(campaignId, region, searchTerm);
            }

            if (synonym != null && Synonym.TYPE_REDIRECT == synonym.getType())
            {
                toRet = new Response();
                toRet.setResponseCode(302);
                toRet.setResponseMessage("HTTP/1.1 302 Moved Temporarily");

                if (synonym.getUrl() != null)
                {
                    toRet.setRedirectUrl(Arrays.asList(new String[] { synonym.getUrl() }));
                }
                else
                {
                    String redirectURLPrefix = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_REDIRECTURL_PREFIX), null);
                    if (redirectURLPrefix != null && synonym.getValue() != null)
                    {
                        toRet.setRedirectUrl(Arrays.asList(new String[] { request.getScheme() + "://" + request.getServerName() + redirectURLPrefix + synonym.getValue() }));
                    }
                }
                return toRet;
            }
            else
            {
                String listVersion = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_LIST_VERSION), LIST_VERSION);
                StreamSearchConfig config = streamSearchService.getPartnerConfig(request.getServerName(), campaign, 3, listVersion);
                if (searchTerm != null)
                {
                    JSONObject jPer = new JSONObject(ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_ENABLEPERSONALIZE_SEARCH), "{}"));
                    personalize = (jPer.has("enablePersonalizedSearch") && jPer.getString("enablePersonalizedSearch").equals("1") ? true : false);
                }
                JSONObject jSpell = new JSONObject(ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_ENABLEPREDICTIVE_SEARCH), "{}"));
                spellcheck = (jSpell.has("enablePredictiveSearch") && jSpell.getString("enablePredictiveSearch").equals("1") ? true : false);

                JSONObject jCurr = new JSONObject(ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_ENABBLECURRATED_SEARCH), "{}"));

                boolean enableCurrated = ((jCurr.has("enableCuratedSearch") && jCurr.getString("enableCuratedSearch").equals("1") ? true : false) && (searchTerm != null && searchTerm.length() > 0 ? true : false) && (sort != null && sort.length() > 0 ? false : true));
                request.setAttribute("enableCurrated", enableCurrated);

                int curratedPdtLimit = ReadUtil.getInt(campaign.getProperty(CampaignProperties.PROP_CURRATED_PDT_LIMIT), (limit < 20 ? limit : 20));
                if (request != null)
                {
                    request.setAttribute("curratedPdtLimit", curratedPdtLimit);
                    request.setAttribute("offset", offset);
                }

                if (iszeroResult)
                {
                    String preFacetSortstr = campaign.getProperty(CampaignProperties.PROP_CUSTOM_PRE_SORT_FACET_KEY);
                    if (preFacetSortstr != null && preFacetSortstr.length() > 0)
                    {
                        JSONObject preFacetSort = new JSONObject(preFacetSortstr);
                        HashMap<String, Object> preFacetSortMap = CastUtil.jsonTohashMap(preFacetSort);
                        if (preFacetSortMap.containsKey("facet"))
                        {
                            String[] temp = (String[]) preFacetSortMap.get("facet");
                            prefacet = Arrays.asList(temp);
                        }

                        if (preFacetSortMap.containsKey("sort"))
                        {
                            String[] temp = (String[]) preFacetSortMap.get("sort");
                            presort = Arrays.asList(temp);
                        }
                    }
                }

                sort = ReadUtil.getString(sort, null);
                boolean defaultfieldsort = campaign.getBooleanProperty(CampaignProperties.PROP_DEFAULTFIELD_SORT, false);
                if (sort != null)
                {
                    sortArr = sort.split(",");
                    if (sortArr != null && sortArr.length > 0)
                    {
                        ArrayList<String> sortList = new ArrayList<>();
                        for (int i = 0; i < sortArr.length; i++)
                        {
                            String[] sortArrTemp = sortArr[i].split("\\|");
                            if (sortArrTemp != null && sortArrTemp.length == 2)
                            {
                                if (selectedSort == null)
                                {
                                    selectedSort = sortArr[i];
                                }
                                else
                                {
                                    selectedSort += "," + sortArr[i];
                                }
                                sortList.addAll(Arrays.asList(sortArrTemp));
                            }
                            else
                            {
                                throw new ServerException("Invalid sort parameter");
                            }
                        }
                        sortArr = sortList.toArray(new String[sortList.size()]);
                    }
                }

                List<String> luceneFields = null;
                MemCacheV2<List<String>> searchLukeCache = new MemCacheFactory<List<String>>().getCache(SEARCH_LUKE_CACHE, memCacheManager);
                String baseKey = request.getServerName() != null ? request.getServerName() : null;
                String cacheKey = String.valueOf(campaignId);
                if (searchLukeCache != null)
                {
                    luceneFields = searchLukeCache.get(baseKey, cacheKey);
                }
                if (luceneFields == null || luceneFields.isEmpty())
                {
                    if (lukeFromDb)
                    {
                        StreamSearchResponse fields = streamSearchService.getLuceneFields(campaignId);
                        if (fields != null && fields.getSearchLuceneFields() != null)
                        {
                            luceneFields = fields.getSearchLuceneFields().getValue();
                        }
                    }
                    else
                    {
                        boolean skipLukeCall = ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_PRODUCT_LUCENE_FIELDS_FROM_S3), false);
                        if (skipLukeCall)
                        {
                            String catalogServiceId = String.valueOf(campaignId) + "_" + PRODUCT;
                            luceneFields = SolrUtil.getLuceneFieldsFromS3(streamCatalogService, memCacheManager, httpClientService, request, null, "v1", partner.getName(), catalogServiceId, 0, null);
                        }
                        else
                        {
                            luceneFields = SolrUtil.getLuceneFieldsAsListByCampaignId(config, campaign.getId(), httpClientService); // getting field names from luke // TODO:MERCHANDIZE luke response cache
                        }
                    }

                    if (luceneFields != null && searchLukeCache != null)
                    {
                        searchLukeCache.put(baseKey, cacheKey, luceneFields, false);
                    }
                    else
                    {
                        logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), null, 0, "Luke Call Failed!!! - possible sorting options and default facets won't be displayed", methodName, "Unknown Exception occurred while processing workitem @{}", null, false, null, methodName);
                    }
                }

                logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  luceneFields - {}", null, false, null, this.getClass().getSimpleName(), luceneFields);

                boolean applyDefaultSort = ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_APPLYDEFAULT_SORT), false);
                ArrayList<String> defaultSort = null;
                if (applyDefaultSort && sort == null)
                {
                    if (selectedFacets != null && selectedFacets.length > 0)
                    {
                        defaultSort = new ArrayList<String>();
                        for (SelectedKraftFacet facet : selectedFacets)
                        {
                            String[] facetValues = facet.getValue();
                            for (String facetValue : facetValues)
                            {
                                defaultSort.add("sort_sequence_" + facetValue + "_" + String.valueOf(campaignId) + " asc"); // need Changes
                            }
                        }
                    }
                }

                ArrayList<String> possibleSorts = new ArrayList<>();
                ArrayList<String> possibleFacets = new ArrayList<>();
                ArrayList<String> similarFacets = new ArrayList<>();
                String[] similarFields = null;
                StringBuilder similarBoost = new StringBuilder();
                StringBuilder similarFilter = new StringBuilder();

                
                if (facets != null && facets.length == 1 && facets[0].equals("false"))
                {
                    facets = null; // Setting null for facets, if facets params contains only false
                }
                if (luceneFields != null)
                {
                    Iterator<String> fieldIterator = luceneFields.iterator();
                    while (fieldIterator.hasNext())
                    {
                        String key = fieldIterator.next();
                        if (key.startsWith(FACET_FILTER_PREFIX) && key.endsWith("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)) && facets != null && facets.length == 1 && facets[0].equals("true"))
                        {
                            if (prefacet != null)
                            {
                                String keyTemp = key.substring(FACET_FILTER_PREFIX.length(), key.indexOf("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)));
                                if (prefacet.contains(keyTemp))
                                {
                                    possibleFacets.add(key);
                                }
                            }
                            else
                            {
                                possibleFacets.add(key);
                            }
                        }
                        if (key.startsWith("sort") && key.endsWith("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)) && !key.contains("sequence"))
                        {
                            if (presort != null)
                            {
                                String keyTemp = key.substring("sort_".length(), key.indexOf("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)));
                                if (presort.contains(keyTemp))
                                {
                                    possibleSorts.add(key);
                                }
                            }
                            else
                            {
                                possibleSorts.add(key);
                            }
                        }
                        if (sort == null && defaultSort != null && defaultSort.contains(key))
                        {
                            sort = defaultSort.get(defaultSort.indexOf(key));
                        }
                        if (similarType != null && !similarType.isEmpty())
                        {
                            JSONObject similarConfigObj = new JSONObject(campaign.getProperty(PimKraftConstants.PROP_SIMILARSEARCH_CONFIG));

                            if (similarConfigObj != null)
                            {
                                HashMap<String, Object> similarConfig = CastUtil.jsonTohashMap(similarConfigObj.getJSONObject(similarType));

                                similarFields = similarConfig.containsKey("fields") ? (String[]) similarConfig.get("fields") : new String[] {};
                                for (String field : similarFields)
                                {
                                    if (key.equals(FACET_FILTER_PREFIX + field + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)))
                                    {
                                        similarFacets.add(key);
                                    }
                                    else if (key.equals(field))
                                    {
                                        similarFacets.add(key);
                                    }
                                }

                                String[] similarFieldsBoost = similarConfig.containsKey("boosts") ? (String[]) similarConfig.get("boosts") : new String[] {};
                                for (String field : similarFieldsBoost)
                                {
                                    String fieldtemp[] = field.split("\\^");
                                    if (key.equals(FACET_FILTER_PREFIX + fieldtemp[0] + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)))
                                    {
                                        String fieldTemp = FACET_FILTER_PREFIX + fieldtemp[0] + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId);
                                        similarBoost.append((similarBoost.length() > 0 ? " " : "") + fieldTemp + "^" + fieldtemp[1]);
                                    }
                                    else if (key.equals(fieldtemp[0]))
                                    {
                                        similarBoost.append((similarBoost.length() > 0 ? " " : "") + field);
                                    }
                                }

                                String[] similarFilters = similarConfig.containsKey("filters") ? (String[]) similarConfig.get("filters") : new String[] {};
                                for (String field : similarFilters)
                                {
                                    String fieldtemp[] = field.split("\\:");
                                    if (key.equals(FACET_FILTER_PREFIX + fieldtemp[0] + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)))
                                    {
                                        String fieldTemp = FACET_FILTER_PREFIX + fieldtemp[0] + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId);
                                        similarFilter.append((similarFilter.length() > 0 ? " AND " : "") + fieldTemp + ":" + fieldtemp[1]);
                                    }
                                    else if (key.equals(fieldtemp[0]))
                                    {
                                        similarFilter.append((similarFilter.length() > 0 ? " AND " : "") + field);
                                    }
                                }
                            }
                        }
                    }

                    if (!possibleFacets.isEmpty())
                    {
                        String customFacetOrder = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_SEARCH_CUSTOM_FACETSORTORDER), null);
                        if (customFacetOrder != null && customFacetOrder.length() > 0)
                        {
                            List<String> sortedPossibleFacets = new ArrayList<>();
                            String[] facetSortOrder = customFacetOrder.split(",");
                            for (String facet : facetSortOrder)
                            {
                                Iterator<String> it = possibleFacets.iterator();
                                while (it.hasNext())
                                {
                                    String possibleFacet = it.next();
                                    if (possibleFacet.startsWith(facet))
                                    {
                                        sortedPossibleFacets.add(possibleFacet);
                                        possibleFacets.remove(possibleFacet);
                                        break;
                                    }
                                }
                            }
                            if (!sortedPossibleFacets.isEmpty())
                            {
                                if (!possibleFacets.isEmpty())
                                {
                                    sortedPossibleFacets.addAll(possibleFacets);
                                }
                                facets = sortedPossibleFacets.toArray(new String[sortedPossibleFacets.size()]);
                            }
                            else
                            {
                                facets = possibleFacets.toArray(new String[possibleFacets.size()]);
                            }
                        }
                        else
                        {
                            facets = possibleFacets.toArray(new String[possibleFacets.size()]);
                        }
                    }
                }
                if (facets != null && facets.length == 1 && facets[0].equals("true"))
                {
                    logger.writeLog(Level.INFO, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  Invalid Facet Parameter Value - facets[0] - {} possible facets will not be shown", null, false, null, this.getClass().getSimpleName(), facets[0]);
                    facets = null;
                }

                String defaultSortStr = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_SEARCH_DEFAULT_SORT), null);
                ArrayList<String> defaultSortFields = new ArrayList<>();
                if (defaultSortStr != null)
                {
                    defaultSortFields = new ArrayList<String>(Arrays.asList(defaultSortStr.split(",")));
                }

                if ((!defaultSortFields.isEmpty() || !possibleSorts.isEmpty()) && sortArr != null && sortArr.length > 0)
                {

                    ArrayList<String> sortTemp = new ArrayList<>();
                    String sortField = null;
                    for (int i = 0; i < sortArr.length; i += 2)
                    {
                        String sortString = sortArr[i].replaceAll(" ", "_");
                        sortNumTemp = defaultfieldsort ? (sortArr[i]) : ("sort_" + sortString + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId));
                        sortStrTemp = defaultfieldsort ? (sortArr[i]) : ("sortstr_" + sortString + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId));
                        sortOrder = sortArr[i + 1];
                        if (sortStrTemp != null && ((possibleSorts != null && possibleSorts.contains(sortStrTemp)) || defaultSortFields.contains(sortStrTemp)))
                        {
                            sortField = sortStrTemp;
                        }
                        else if (sortNumTemp != null && ((possibleSorts != null && possibleSorts.contains(sortNumTemp)) || defaultSortFields.contains(sortNumTemp)))
                        {
                            sortField = sortNumTemp;
                        }
                        else
                        {
                            sortField = sortArr[i];
                        }
                        if (sortOrder != null && sortField != null)
                        {
                            sortField += " " + sortOrder;
                        }
                        if (sortField != null)
                        {
                            sortTemp.add(sortField);
                        }
                    }

                    if (!sortTemp.isEmpty())
                    {
                        sort = StringUtil.mergeStrings(sortTemp.toArray(new String[sortTemp.size()]), ",");
                    }
                }
                if (applyDefaultSort && sort == null && luceneFields != null && luceneFields.contains("sequence"))
                {
                    sort = new String("sequence asc");
                }
                boolean allowSegmentation = ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_PIM_ALLOW_SEGMENTATION), false);
                if (allowSegmentation)
                {
                    if (segments == null)
                    {
                        segments = new String[] { ProductCategory.SEGMENTS_ALL };
                    }
                    else
                    {
                        boolean allPresent = false;
                        for (int s = 0; s < segments.length; s++)
                        {
                            if (segments[s].equals(ProductCategory.SEGMENTS_ALL))
                            {
                                allPresent = true;
                            }
                        }
                        if (!allPresent)
                        {
                            segments = Arrays.copyOf(segments, segments.length + 1);
                            segments[segments.length - 1] = ProductCategory.SEGMENTS_ALL;
                        }
                    }
                }
                else
                {
                    segments = null;
                }

                StreamSearchQuery query = new StreamSearchQuery();
                StreamSearchQuery facetQuery = null;
                StreamSearchResponse result = null;
                StreamSearchResponse curratedresult = null;
                query = addQueryParams(query, campaignId, skuId, productId, name, brand, category, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, variant, color, region, version, personalize, online, store, advancedSearch, includeGhostProduct, segments, catalogId, storeId);
                
                //Boost user preferences
                StringBuffer boostQuery = query.getBq() != null ? new StringBuffer(query.getBq()) : new StringBuffer();
                if(customFacets != null && customFacets.length > 0)
                {
                    int boostForFirstValue = ReadUtil.getInt(campaign.getProperty(PimKraftConstants.PROP_BOOST_FOR_FIRST_VALUE), 500);
                    int maxBoostOffset = 0;
                    for(SelectedKraftFacet selectedFacet : customFacets)
                    {
                        maxBoostOffset = maxBoostOffset + selectedFacet.getValue().length;
                    }
                    String facetSuffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
                    for(SelectedKraftFacet selectedFacet : customFacets)
                    {
                        boolean boostFlag = true;
                        String selectedFacetkey = selectedFacet.getKey().replaceAll(" ", "_");
                        String[] selectedFacetValues = selectedFacet.getValue();
                        String facetKeyValue = FACET_FILTER_PREFIX + selectedFacetkey + facetSuffix;
                        for(String facetValue : selectedFacetValues)
                        {
                            boostQuery = createBq(boostQuery, facetKeyValue, facetValue, (boostFlag ? boostForFirstValue : maxBoostOffset));
                            maxBoostOffset = maxBoostOffset - 1;
                            boostFlag = false;
                        }
                    }
                    if(boostQuery.length() > 0)
                    {
                        query.setBq(boostQuery.toString());
                    }
                }

                if (edismax)
                {
                    if (selectedFacets != null)
                    {
                        HashMap<String, List<String>> selectedFacetNamesValues = new HashMap<>();
                        for (int i = 0; i < selectedFacets.length; i++)
                        {
                            String selectedFacetkey = selectedFacets[i].getKey().replaceAll(" ", "_");
                            String[] selectedFacetValues = selectedFacets[i].getValue();
                            ArrayList<StreamSearchQueryCondition> facetCond = new ArrayList<>();
                            String suffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
                            selectedFacetNamesValues.put(FACET_FILTER_PREFIX + selectedFacetkey + suffix, Arrays.asList(selectedFacetValues));
                            for (int j = 0; j < selectedFacetValues.length; j++)
                            {
                                String facetSuffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
                                String facetKeyValue = FACET_FILTER_PREFIX + selectedFacetkey + facetSuffix;
                                facetCond.add(new StreamSearchQueryCondition(facetKeyValue, selectedFacetValues[j], null, null, false, false, null));
                            }
                            StreamSearchQueryCondition facetCondition = new StreamSearchQueryCondition(null, null, null, null, true, (selectedFacets[i].getOperation() == SelectedKraftFacet.FACET_EXCLUDE), facetCond);
                            query.addCondition(facetCondition);
                            if (facetQuery == null)
                            {
                                facetQuery = new StreamSearchQuery();
                            }
                            facetQuery.addCondition(facetCondition);
                        }
                        if (selectedFacetNamesValues.size() > 0)
                        {
                            query.setSelectedFacetNames(selectedFacetNamesValues);
                        }
                    }

                    String originalContextualParam = null;
                    if (contextualParam != null)
                    {
                        contextualParam = URLDecoder.decode(contextualParam, "UTF-8");
                        originalContextualParam = contextualParam;
                        if (facetQuery != null)
                        {
                            String facet = streamSearchService.getQueryString(facetQuery);
                            if (!facetQuery.getConditions().isEmpty())
                            {
                                facet = facet.substring(1, facet.length() - 1); //to remove outer bracket as it makes query (+campaignid:2295 +available:true +brand:"nike" +categorylevel2:"footwear" (+facet_gender_2295:"men" +facet_colors_2295:"multicolor" +facet_toe_shape_2295:"regular" +facet_brand_2295:"Nike")) rather than (+campaignid:2295 +available:true +brand:"nike" +categorylevel2:"footwear" +facet_gender_2295:"men" +facet_colors_2295:"multicolor" +facet_toe_shape_2295:"regular" +facet_brand_2295:"Nike") 
                            }
                            contextualParam = contextualParam.substring(0, contextualParam.length() - 1) + " " + facet + ")";
                        }
                    }

                    if (searchTerm != null)
                    {
                        List<String> searchTermArray = StopAnalyzerUtilKraft.parseKeywords(campaign.getProperty(CampaignProperties.PROP_SEARCH_STOPWORDS), searchTerm.toLowerCase(), ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_SEARCH_NONENGCHAR), false), campaign, region, true);
                        //logger.info("****SEARCH TERM: " + searchTerm + " arr: " + searchTermArray);
                        if (searchTermArray != null && !searchTermArray.isEmpty())
                        {
                            Object[] curateResults = this.termCurator.curateTermsArray(httpClientService, campaign, searchTerm.toLowerCase(), searchTermArray);
                            ArrayList<StreamSearchQueryCondition> conditions = (ArrayList<StreamSearchQueryCondition>) curateResults[0];
                            HashMap<String, ArrayList<String>> altTerms = (HashMap<String, ArrayList<String>>) curateResults[1];
                            HashMap<String, String> swapTerms = (HashMap<String, String>) curateResults[2];
                            HashMap<String, Integer> boostVals = (HashMap<String, Integer>) curateResults[3];
                            ArrayList<StreamSearchQueryCondition> curateCondition = new ArrayList<>();
                            if ((conditions != null && !conditions.isEmpty()) || (altTerms != null && !altTerms.isEmpty()))
                            {
                                if (conditions != null && !conditions.isEmpty())
                                {
                                    curateCondition.add(new StreamSearchQueryCondition(null, null, null, null, true, false, conditions));
                                }
                                if (swapTerms.get(searchTerm) != null)
                                {
                                    searchTerm = swapTerms.get(searchTerm);
                                    searchTermArray = StopAnalyzerUtilKraft.parseKeywords(campaign.getProperty(CampaignProperties.PROP_SEARCH_STOPWORDS), searchTerm.toLowerCase(), ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_SEARCH_NONENGCHAR), false));
                                }
                                ArrayList<String> alt = (altTerms == null ? null : altTerms.get(searchTerm.toLowerCase())); // the unsanitized word is the key
                                if (alt != null)
                                {
                                    ArrayList<StreamSearchQueryCondition> altConditions = new ArrayList<>();
                                    for (String term : alt)
                                    {
                                        String terms[] = term.split(" ");
                                        if (terms.length == 1)
                                        {
                                            altConditions.add(new StreamSearchQueryCondition(term, null, null, null, false, false, null));
                                        }
                                        else // for more words space issue
                                        {
                                            ArrayList<StreamSearchQueryCondition> tempConditions = new ArrayList<>();
                                            for (String t : terms)
                                            {
                                                tempConditions.add(new StreamSearchQueryCondition(t, null, null, null, true, false, null));
                                            }
                                            altConditions.add(new StreamSearchQueryCondition(null, null, null, null, false, false, tempConditions));
                                        }
                                    }
                                    curateCondition.add(new StreamSearchQueryCondition(null, null, null, null, true, false, altConditions));
                                }
                                else
                                {
                                    for (int i = 0; i < searchTermArray.size(); i++)
                                    {
                                        String word = searchTermArray.get(i);
                                        String term = word.trim();
                                        if (term.length() > 0)
                                        {
                                            alt = (altTerms == null ? null : altTerms.get(word)); // the unsanitized word is the key
                                            if (alt != null)
                                            {
                                                ArrayList<StreamSearchQueryCondition> altConditions = new ArrayList<>();
                                                for (String altTerm : alt)
                                                {
                                                    String terms[] = altTerm.split(" ");
                                                    if (terms.length == 1)
                                                    {
                                                        altConditions.add(new StreamSearchQueryCondition(altTerm, null, null, null, false, false, null));
                                                    }
                                                    else
                                                    {
                                                        ArrayList<StreamSearchQueryCondition> tempConditions = new ArrayList<>();
                                                        for (String t : terms)
                                                        {
                                                            tempConditions.add(new StreamSearchQueryCondition(t, null, null, null, true, false, null));
                                                        }
                                                        altConditions.add(new StreamSearchQueryCondition(null, null, null, null, false, false, tempConditions));
                                                    }

                                                }
                                                curateCondition.add(new StreamSearchQueryCondition(null, null, null, null, true, false, altConditions));
                                            }
                                            else
                                            {
                                                if (swapTerms.get(term) != null)
                                                {
                                                    term = swapTerms.get(term);
                                                }
                                                curateCondition.add(new StreamSearchQueryCondition(term, null, null, null, true, false, null));
                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                String searchTermTemp = searchTermArray != null && searchTermArray.size() > 0 ? StringUtil.mergeStrings(searchTermArray.toArray(new String[searchTermArray.size()]), " ", searchTerm) : searchTerm;
                                boolean disabledidumean = true;
                                if (searchTermArray != null && searchTerm != null && searchTerm.length() != searchTermTemp.length())
                                {
                                    disabledidumean = false;
                                }
                                String spellCorected = spellCheck(request, campaign, config, searchTermTemp, listVersion, disableFacetMinCount, disableFacetLimit, spellcheck);
                                if (spellcheck && spellCheckOnly && disabledidumean && !spellCorected.equalsIgnoreCase(searchTerm))
                                {
                                    JSONObject stateObj = new JSONObject();
                                    JSONObject properties = new JSONObject();
                                    JSONObject jsonObj = new JSONObject();
                                    stateObj.put("searchcorrected", spellCorected);
                                    properties.put("state", stateObj);
                                    jsonObj.put("properties", properties);
                                    toRet = new Response("application/json; charset=utf-8", jsonObj.toString().getBytes("UTF-8"));
                                    toRet.setResponseCode(0);
                                    return toRet;
                                }
                                if (disabledidumean)
                                {
                                    request.setAttribute("searchterm", searchTerm);
                                }
                                else
                                {
                                    request.setAttribute("searchterm", searchTerm);
                                    request.setAttribute("spellcheck", searchTerm);
                                }
                                if (spellCorected != null && !spellCorected.equalsIgnoreCase(searchTerm))
                                {
                                    searchTerm = spellCorected;
                                    searchTermArray = StopAnalyzerUtilKraft.parseKeywords(campaign.getProperty(CampaignProperties.PROP_SEARCH_STOPWORDS), searchTerm.toLowerCase(), ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_SEARCH_NONENGCHAR), false), campaign, region);
                                }
                                for (int i = 0; i < searchTermArray.size(); i++)
                                {
                                    String word = searchTermArray.get(i);
                                    String term = word.trim();
                                    curateCondition.add(new StreamSearchQueryCondition(term, null, null, null, true, false, null));
                                }
                            }
                            ArrayList<StreamSearchQueryCondition> idCondition = new ArrayList<>();
                            idCondition.add(new StreamSearchQueryCondition("categoryid", searchTerm, null, null, false, false, null));
                            idCondition.add(new StreamSearchQueryCondition("productid", searchTerm, null, null, false, false, null));
                            idCondition.add(new StreamSearchQueryCondition("skuid", searchTerm, null, null, false, false, null));

                            ArrayList<StreamSearchQueryCondition> searchCondition = new ArrayList<>();
                            searchCondition.add(new StreamSearchQueryCondition(null, null, null, null, false, false, curateCondition));
                            searchCondition.add(new StreamSearchQueryCondition(null, null, null, null, false, false, idCondition));

                            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, searchCondition));

                            StringBuffer bq = new StringBuffer();
                            if (boostVals != null && boostVals.size() > 0)
                            {
                                Iterator<Entry<String, Integer>> boostValIterator = boostVals.entrySet().iterator();
                                while (boostValIterator.hasNext())
                                {
                                    Entry<String, Integer> boostValEntry = boostValIterator.next();
                                    //(productid:56bb7d5be5df298921b66db7)^500 (productid:56aa60245f61910c9901179a)^490
                                    bq = createBq(bq, "productid", boostValEntry.getKey(), boostValEntry.getValue());
                                }
                            }
                            ArrayList<String> curateKeys = new ArrayList<>();
                            String curateFields = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_CURATE_WEIGHT_CONFIG), "{}");

                            JSONObject curateObj = new JSONObject(curateFields);
                            if (curate != null && curate.length > 0)
                            {
                                for (int i = 0; i < curate.length; i++)
                                {
                                    String selectedFacetkey = curate[i].getKey().replaceAll(" ", "_");
                                    curateKeys.add(selectedFacetkey);
                                    String[] selectedFacetValues = curate[i].getValue();
                                    ArrayList<StreamSearchQueryCondition> facetCond = new ArrayList<>();
                                    for (int j = 0; j < selectedFacetValues.length; j++)
                                    {
                                        facetCond.add(new StreamSearchQueryCondition(selectedFacetkey, selectedFacetValues[j], null, null, false, false, null));
                                        if (curateObj != null && curateObj.has(selectedFacetkey))
                                        {
                                            bq = createBq(bq, selectedFacetkey, selectedFacetValues[j], curateObj.getInt(selectedFacetkey));
                                        }

                                    }
                                    StreamSearchQueryCondition facetCondition = new StreamSearchQueryCondition(null, null, null, null, false, false, facetCond);
                                    query.addCondition(facetCondition);
                                }
                            }
                            if (bq.length() > 0)
                            {
                                query.setBq(bq.toString());
                            }
                        }
                        else
                        {
                            toRet = new Response(ServerException.ERR_NONE, "No Match Found.");
                            logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), null, 0, "Search Term is Invalid ", methodName, "Unknown Exception occurred while processing workitem @{}", null, false, null, searchTerm);
                            return toRet;
                        }
                    }
                    String searchFields = campaign.getProperty(CampaignProperties.PROP_SEARCH_WEIGHT_CONFIG);
                    SearchWeightConfig customSearchFields = null;
                    HashMap<String, String> searchBoostMap = null;
                    if (searchFields != null)
                    {
                        StringBuffer qf = new StringBuffer();
                        customSearchFields = (SearchWeightConfig) CastUtil.fromJSON(searchFields, SearchWeightConfig.class);
                        searchBoostMap = customSearchFields.getConfigureBoostValues();
                        boolean first = true;
                        for (Entry<String, String> entry : searchBoostMap.entrySet())
                        {
                            float boost = ReadUtil.getFloat(entry.getValue(), 0f);
                            if (boost != 0f)
                            {
                                if (!first)
                                {
                                    qf.append(" ");
                                }
                                else
                                {
                                    first = false;
                                }
                                qf.append(entry.getKey());
                                qf.append("^");
                                qf.append(boost);
                            }
                        }
                        query.setQf(qf.toString());
                    }

                    query.setAdditionalParams("defType=edismax&stopwords=true&lowercaseOperators=true&indent=true&mm=99%25");

                    if (similarFields!=null && !similarFacets.isEmpty())
                    {
                        StringBuilder tempparams = new StringBuilder();
                        for (String similarFacet : similarFacets)
                        {
                            tempparams.append((tempparams.length()>0 ? "," : "") + similarFacet);
                        }
                        if (tempparams.length()>0)
                        {
                            similarFilter.append((similarFilter.length() > 0 ? " AND " : "") + "storeid:" + storeId);
                            similarFilter.append(" AND campaignid:" + campaignId);
                            similarFilter.append(" AND available:true");
                            similarFilter.append(" AND catalogid:" + catalogId);
                            
                            query.setAdditionalParams("mlt=true&mlt.mintf=1&mlt.count=" + limit + "&mlt.mindf=2&mlt.fl=" + tempparams.toString() + "&" + (similarBoost.length() == 0 ? "" : "mlt.boost=true&mlt.qf=" + similarBoost.toString() + "&") + (similarFilter.length() == 0 ? "" : "fq=" + similarFilter.toString() + "&") + query.getAdditionalParams());
                        }
                    }

                    if (includeFacet != null)
                    {
                        if (facets != null)
                        {
                            List<String> facetList = Arrays.asList(facets);
                            facetList.add(includeFacet);
                            facets = facetList.toArray(new String[facetList.size()]);
                        }
                        else
                        {
                            facets = new String[] { includeFacet };
                        }
                    }
                    result = streamSearchKraftService.doSearch(request.getServerName(), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, listVersion, query, null, null, sort, group, facets, offset, limit, config, false, searchTerm, contextualParam, originalContextualParam, disableFacetMinCount, disableFacetLimit, (similarFields!=null && !similarFacets.isEmpty()));
                }
                else
                {
                    if (searchTerm != null)
                    {
                        String spellCorected = spellCheck(request, campaign, config, searchTerm, listVersion, disableFacetMinCount, disableFacetLimit, spellcheck);
                        if (spellCorected != null && !spellCorected.equalsIgnoreCase(searchTerm))
                        {
                            searchTerm = spellCorected;
                        }
                    }
                    StreamSearchQuery curratedquery = new StreamSearchQuery();
                    curratedquery = addQueryParams(curratedquery, campaignId, skuId, productId, name, brand, category, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, variant, color, region, version, personalize, online, store, advancedSearch, includeGhostProduct, segments, catalogId, storeId);
                    constructQuery(campaign, campaignId, curratedquery, searchTerm, enableCurrated, usev2, curratedPdtLimit, selectedFacets, priceMin, priceMax, size1, size2, catalogId, false, region);

                    query.setAdditionalParams("defType=edismax");

                    boolean makeanotherNwcall = true;
                    if (enableCurrated)
                    {
                        if (includeFacet != null)
                        {
                            if (facets != null)
                            {
                                List<String> facetList = Arrays.asList(facets);
                                facetList.add(includeFacet);
                                facets = facetList.toArray(new String[facetList.size()]);
                            }
                            else
                            {
                                facets = new String[] { includeFacet };
                            }
                        }
                        curratedresult = streamSearchKraftService.doSearch(request.getServerName(), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, listVersion, curratedquery, null, null, sort, group, facets, 0, curratedPdtLimit, config, false, searchTerm, contextualParam, null, disableFacetMinCount, disableFacetLimit, false);
                        if (curratedresult != null && curratedresult.getNumEntries() > 0 && curratedresult.getGroups() != null)
                        {
                            Iterator<Map.Entry<String, ArrayList<StreamSearchGroupResult>>> resultIterator = curratedresult.getGroups().entrySet().iterator();
                            ArrayList<StreamSearchGroupResult> resultCurr = resultIterator.next().getValue();
                            long nocurratedResult = resultCurr.size();

                            if ((curratedPdtLimit >= offset || nocurratedResult <= offset) && (offset > 0 && nocurratedResult <= offset))
                            {
                                curratedresult = null;
                            }

                            if (offset == 0)
                            {
                                if (nocurratedResult < limit)
                                {
                                    limit = limit - (int) nocurratedResult;
                                }
                                else if (nocurratedResult == limit)
                                {
                                    makeanotherNwcall = false;
                                }
                            }

                            if (nocurratedResult < curratedPdtLimit && (offset + limit) > nocurratedResult)
                            {
                                if (offset > 0 && offset >= nocurratedResult)
                                {
                                    offset = offset - (int) nocurratedResult;
                                }
                            }
                        }
                    }
                    constructQuery(campaign, campaignId, query, searchTerm, false, usev2, curratedPdtLimit, selectedFacets, priceMin, priceMax, size1, size2, catalogId, (enableCurrated ? true : false), region);

                    if (includeFacet != null)
                    {
                        if (facets != null)
                        {
                            List<String> facetList = Arrays.asList(facets);
                            facetList.add(includeFacet);
                            facets = facetList.toArray(new String[facetList.size()]);
                        }
                        else
                        {
                            facets = new String[] { includeFacet };
                        }
                    }
                    result = streamSearchKraftService.doSearch(request.getServerName(), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, listVersion, query, null, null, sort, group, facets, offset, (makeanotherNwcall ? limit : 0), config, false, searchTerm, contextualParam, null, disableFacetMinCount, disableFacetLimit, false);
                }
                if (result == null && curratedresult != null)
                {
                    result = curratedresult;
                    curratedresult = null;
                }

                if (result != null || curratedresult != null)
                {
                    if (!possibleSorts.isEmpty())
                    {
                        result.setPossibleSorts(possibleSorts.toArray(new String[possibleSorts.size()]));
                    }
                    if (!defaultSortFields.isEmpty())
                    {
                        result.setPossibleSorts(defaultSortFields.toArray(new String[defaultSortFields.size()]));
                    }

                    if (selectedSort != null)
                    {
                        result.setSelectedSort(selectedSort);
                    }
                    if (selectedFacets != null)
                    {
                        HashMap<String, String[]> selFacets = new HashMap<String, String[]>();
                        for (SelectedKraftFacet facet : selectedFacets)
                        {
                            selFacets.put(facet.getKey(), facet.getValue());
                        }
                        if (selFacets.size() > 0)
                        {
                            result.setSelectedFacets(selFacets);
                        }
                    }
                    if (categoryid != null)
                    {
                        request.setAttribute("categoryid", categoryid);
                    }

                    String curatedFacetconfigJobjStr = null;
                    String curatedFacetconfigUrl = campaign.getProperty(CampaignProperties.PROP_SEARCH_CURATED_FACETS_CONFIG_URL);
                    if (curatedFacetconfigUrl != null && curatedFacetconfigUrl.trim().length() > 0)
                    {
                        MemCacheV2<String> curatedFacetConfigCache = new MemCacheFactory<String>().getCache(CURATED_FACET_CONFIG_CACHE_NAME, memCacheManager);
                        if (curatedFacetConfigCache != null)
                        {
                            curatedFacetconfigJobjStr = curatedFacetConfigCache.get(String.valueOf(campaignId), CURATED_FACET_CONFIG_CACHE_KEY);
                            if (curatedFacetconfigJobjStr == null)
                            {
                                curatedFacetconfigJobjStr = new String(NWUtil.getDataFromURL(curatedFacetconfigUrl, httpClientService));
                                curatedFacetConfigCache.put(String.valueOf(campaignId), CURATED_FACET_CONFIG_CACHE_KEY, curatedFacetconfigJobjStr, false);
                            }
                        }
                    }

                    SearchResponseFormatter searchResponseFormatter = (SearchResponseFormatter) Class.forName(responseFormatterClass).newInstance();
                    toRet = searchResponseFormatter.format(request, result, limit, campaign, null, usev2, curratedresult, null, JSONUtils.getJSONObjectFromString(curatedFacetconfigJobjStr, null));
                    if (toRet == null) { throw new ServerException(ServerException.ERR_NONE, "No match found"); }
                }
            }
        }
        catch (Exception e)
        {
            logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
            toRet = new Response((e instanceof ServerException) ? ((ServerException) e).getErrorCode() : ServerException.ERR_UNKNOWN, e.getMessage());
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : response - {} ", null, false, null, this.getClass().getSimpleName(), CastUtil.toJSON(toRet));
        return toRet;
    }
    
    private void constructQuery(Campaign campaign,
                                long campaignId,
                                StreamSearchQuery query,
                                String searchTerm,
                                boolean enableCurrated,
                                boolean usev2,
                                int curratedPdtLimit,
                                SelectedKraftFacet[] selectedFacets,
                                float[] priceMin,
                                float[] priceMax,
                                String size1,
                                String size2,
                                String catalogId,
                                boolean includeCurrated,
                                String region) throws ServerException
    {
        String methodName = "constructQuery";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  campaign - {}, campaignId - {}, query - {}, searchTerm - {}, enableCurrated - {}, usev2 - {}, curratedPdtLimit - {}, selectedFacets - {}, priceMin - {}, priceMax - {}, size1 - {} size2 - {}, catalogId - {}, includeCurrated - {}, region - {}", null, false, null, this.getClass().getSimpleName(), campaign, campaignId, query, searchTerm, enableCurrated, usev2, curratedPdtLimit, selectedFacets, priceMin, priceMax, size1, size2, catalogId, includeCurrated, region);
        if (searchTerm != null)
        {
            searchTerm = searchTerm.trim();
            String searchTermOrginal = searchTerm;
            searchTerm = searchTerm.replaceAll("[^\\w]\\s+", " ");//fix for SKCP-3349
            List<String> searchTermArray = StopAnalyzerUtil.parseKeywords(campaign.getProperty(CampaignProperties.PROP_SEARCH_STOPWORDS), searchTerm, ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_SEARCH_NONENGCHAR), false));
            //String[] searchTermArray = searchTerm.split(" ");
            if (searchTermArray != null)
            {
                ArrayList<StreamSearchQueryCondition> searchterm = new ArrayList<StreamSearchQueryCondition>();
                ArrayList<StreamSearchQueryCondition> newwordSearchCond = new ArrayList<StreamSearchQueryCondition>();
                String searchFields = campaign.getProperty(CampaignProperties.PROP_SEARCH_WEIGHT_CONFIG);
                SearchWeightConfig customSearchFields = null;
                HashMap<String, String> searchBoostMap = null;
                if (searchFields != null)
                {
                    customSearchFields = (SearchWeightConfig) CastUtil.fromJSON(searchFields, SearchWeightConfig.class);
                    searchBoostMap = customSearchFields.getConfigureBoostValues();
                }
                if (!usev2)
                {
                    for (int i = 0; i < searchTermArray.size(); i++)
                    {
                        String term = searchTermArray.get(i);
                        if (term.length() > 0)
                        {
                            ArrayList<StreamSearchQueryCondition> wordSearchCond = new ArrayList<StreamSearchQueryCondition>();
                            if (customSearchFields != null)
                            {
                                for (Entry<String, String> entry : searchBoostMap.entrySet())
                                {
                                    wordSearchCond.add(new StreamSearchQueryCondition(entry.getKey(), term, null, null, false, false, null, Float.parseFloat(entry.getValue()), true));
                                }
                            }
                            else
                            {
                                wordSearchCond.add(new StreamSearchQueryCondition("productid", term, null, null, false, false, null, 0, true, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("skuid", term, null, null, false, false, null, 0, true, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("division", term, null, null, false, false, null, 0.9f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("category", term, null, null, false, false, null, 0.8f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("brand", term, null, null, false, false, null, 0.7f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("name", term, null, null, false, false, null, 0.6f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("variant", term, null, null, false, false, null, 0.5f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("description", term, null, null, false, false, null, 0.3f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("additionalkeyword", term, null, null, false, false, null, 0.2f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("color", term, null, null, false, false, null, 0.1f, true));
                                wordSearchCond.add(new StreamSearchQueryCondition("storeid", term, null, null, false, false, null, 0.1f, true));
                            }
                            if (searchTermArray.size() > 1)
                            {
                                searchterm.add(new StreamSearchQueryCondition(null, null, null, null, true, false, wordSearchCond));
                            }
                            else
                            {
                                searchterm.add(new StreamSearchQueryCondition(null, null, null, null, false, false, wordSearchCond));
                            }
                        }
                    }
                    newwordSearchCond.add(new StreamSearchQueryCondition(null, null, null, null, false, false, searchterm));
                }
                else
                {
                    for (int i = 0; i < searchTermArray.size(); i++)
                    {
                        String term = searchTermArray.get(i);
                        if (term.length() > 0)
                        {
                            newwordSearchCond.add(new StreamSearchQueryCondition("index", term, null, null, true, false, null));
                        }
                    }
                }

                if (enableCurrated)
                {
                    for (int i = 0; i < searchTermArray.size(); i++)
                    {
                        String term = searchTermArray.get(i);
                        if (term.length() > 0)
                        {
                            //  String tempcurrateStr = "[0 TO "+curratedPdtLimit+"]";
                            newwordSearchCond.add(new StreamSearchQueryCondition("sortorder_" + term, null, String.valueOf(0), String.valueOf(curratedPdtLimit), true, false, null));
                        }
                    }
                }
                if (includeCurrated && !enableCurrated)
                {
                    for (int i = 0; i < searchTermArray.size(); i++)
                    {
                        newwordSearchCond.add(new StreamSearchQueryCondition("sortorder_" + searchTermArray.get(i), null, String.valueOf(0), String.valueOf(curratedPdtLimit), true, true, null));
                    }
                }
                if (searchTermOrginal != null && searchTermOrginal.length() > 0)
                {
                    newwordSearchCond.add(new StreamSearchQueryCondition("suggestion", searchTermOrginal, null, null, false, false, null));
                    query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, newwordSearchCond));
                }
            }
        }
        if (selectedFacets != null)
        {
            HashMap<String, List<String>> selectedFacetNamesValues = new HashMap<String, List<String>>();
            String facetSuffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
            for (int i = 0; i < selectedFacets.length; i++)
            {
                String selectedFacetkey = selectedFacets[i].getKey() != null ? selectedFacets[i].getKey().replaceAll(" ", "_") : null;
                String[] selectedFacetValues = selectedFacets[i].getValue() != null ? selectedFacets[i].getValue() : null;
                if(selectedFacetkey == null || selectedFacetValues == null)
                {
                    continue;
                }
                ArrayList<StreamSearchQueryCondition> facetCond = new ArrayList<StreamSearchQueryCondition>();
                //ArrayList<String> list = new ArrayList<String>();
                String suffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
                selectedFacetNamesValues.put(FACET_FILTER_PREFIX + selectedFacetkey + suffix, Arrays.asList(selectedFacetValues));
                String facetKeyValue = FACET_FILTER_PREFIX + selectedFacetkey + facetSuffix;
                for (int j = 0; j < selectedFacetValues.length; j++)
                {
                    facetCond.add(new StreamSearchQueryCondition(facetKeyValue, selectedFacetValues[j], null, null, false, false, null));
                }
                query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, (selectedFacets[i].getOperation() == SelectedKraftFacet.FACET_EXCLUDE), facetCond));
            }
            if (selectedFacetNamesValues.size() > 0)
            {
                query.setSelectedFacetNames(selectedFacetNamesValues);
            }
        }

        if (size1 != null || size2 != null)
        {
            ArrayList<StreamSearchQueryCondition> conditions = new ArrayList<StreamSearchQueryCondition>();
            if (size1 != null)
            {
                conditions.add(new StreamSearchQueryCondition("size1value", size1, null, null, false, false, null));
            }

            if (size2 != null)
            {
                conditions.add(new StreamSearchQueryCondition("size2value", size2, null, null, false, false, null));
            }
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, conditions));
        }
        ArrayList<StreamSearchQueryCondition> searchPriceTemp = new ArrayList<StreamSearchQueryCondition>();
        if (priceMin != null)
        {
            for (int i = 0; i < priceMin.length; i++)
            {

                float priceMinTemp = priceMin[i];
                float priceMaxTemp = priceMax[i];
                if (priceMinTemp >= 0.0f && priceMaxTemp > 0.0f)
                {
                    ArrayList<StreamSearchQueryCondition> priceSearchCond = new ArrayList<StreamSearchQueryCondition>();
                    priceSearchCond.add(new StreamSearchQueryCondition("salemin", null, String.valueOf((priceMinTemp * 100)), null, true, false, null));
                    priceSearchCond.add(new StreamSearchQueryCondition("salemax", null, null, String.valueOf((priceMaxTemp * 100)), true, false, null));
                    searchPriceTemp.add(new StreamSearchQueryCondition(null, null, null, null, false, false, priceSearchCond));
                }
            }
        }
        if (searchPriceTemp.size() > 0)
        {
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, searchPriceTemp));
        }
    }

    static class TermCurator
    {
        private HashMap<String, CuratedField> curatedFields = new HashMap<String, CuratedField>();

        public final HashMap<String, ArrayList<String>> sortOrderConfig = new HashMap<String, ArrayList<String>>();
        public String sortOrderVersion = null;

        //TODO:don't hardcore here 
        public static final int MAX_SORTORDER_ITEM = 50;
        public static final int MAX_SORTORDER_BOOST_FACTOR = 50;
        public static final int MAX_SORTORDER_BOOST_OFFSET = 5000;

        //public static final String FIELD_CATEGORYLEVEL1 = "categorylevel1";
        //public static final String FIELD_CATEGORYLEVEL2 = "categorylevel2";
        //public static final String FIELD_CATEGORYLEVEL3 = "categorylevel3";
        //public static final String FIELD_BRAND = "brand";
        //public static final String FIELD_COLOR = "color";

        private SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());

        static ThreadLocal<PorterStemmer> porterStemmer = new ThreadLocal<PorterStemmer>();

        @SuppressWarnings({ "unchecked" })
        private void loadMaps(HttpClientService httpClientService,
                              Campaign campaign) throws ServerException
        {
            CuratedField[] fields = CuratedField.loadFields(campaign);
            if (fields != null)
            {
                HashMap<String, CuratedField> curatedFields = new HashMap<String, CuratedField>();
                for (CuratedField cf : fields)
                {
                    curatedFields.put(cf.fieldName, cf);
                }
                this.curatedFields = curatedFields;
            }
            String strDbVersion = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_CAMPAIGN_SORTORDER_VERSION), SORT_VERSION);
            if (strDbVersion != null && (sortOrderVersion == null || !strDbVersion.equals(sortOrderVersion)))
            {
                String sortOrderConfigFile = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_CAMPAIGN_SORTORDER_CONFIG_FILE), null);
                if (sortOrderConfigFile != null)
                {
                    try
                    {
                        byte[] data = NWUtil.getDataFromURL(sortOrderConfigFile, httpClientService);
                        if (data != null && data.length > 0)
                        {
                            JSONObject jOSortConfig = new JSONObject(new String(data));
                            @SuppressWarnings("unchecked")
                            Iterator<String> sortOrderIterator = (Iterator<String>) jOSortConfig.keys();
                            while (sortOrderIterator.hasNext())
                            {
                                String key = sortOrderIterator.next();
                                if (key != null)
                                {
                                    ArrayList<String> sortOrder = new ArrayList<String>();
                                    JSONArray values = jOSortConfig.getJSONArray(key);
                                    for (int i = 0; i < values.length(); i++)
                                    {
                                        sortOrder.add(values.getString(i));
                                    }
                                    StringBuffer keyWord = new StringBuffer();
                                    List<String> searchTermArray = StopAnalyzerUtilKraft.parseKeywords(campaign.getProperty(CampaignProperties.PROP_SEARCH_STOPWORDS), key, ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_SEARCH_NONENGCHAR), false));
                                    for (int i = 0; i < searchTermArray.size(); i++)
                                    {
                                        if (i != 0)
                                        {
                                            keyWord.append(" ");
                                        }
                                        keyWord.append(sanitizeTerm(searchTermArray.get(i)));
                                    }
                                    if (sortOrder != null && sortOrder.size() > 0)
                                    {
                                        sortOrderConfig.put(keyWord.toString(), sortOrder);
                                    }
                                }
                            }
                            sortOrderVersion = strDbVersion;
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
            }
        }

        public String swapTerm(CuratedField cf, String term)
        {
            HashMap<String, String> swapMap = cf.swapMap;
            String swapString = (swapMap == null ? null : swapMap.get(term));

            if (logger.isDebugEnabled())
            {
                logger.debug("Swap Term for " + term + " in  " + cf + " is  " + swapString + " from " + swapMap);
            }
            return swapString;
        }

        public HashSet<String> alternateTerms(CuratedField cf, String term)
        {
            HashMap<String, HashSet<String>> synMap = cf.synMap;
            return (synMap == null ? null : synMap.get(term));
        }

        static String sanitizeTerm(String input) throws ServerException
        {
            PorterStemmer stemmer = porterStemmer.get();
            if (stemmer == null)
            {
                stemmer = new PorterStemmer();
                porterStemmer.set(stemmer);
            }
            String toRet = input.trim().toLowerCase();
            int idx = toRet.indexOf("'");
            if (idx >= 0)
            {
                toRet = toRet.substring(0, idx);
            }
            if (toRet != null)
            {
                stemmer.setCurrent(toRet);
                if (stemmer.stem())
                {
                    toRet = stemmer.getCurrent();
                }
            }
            return toRet;
        }

        public Object[] curateTermsArray(HttpClientService httpClientService,
                                         Campaign campaign,
                                         String searchTerm,
                                         List<String> searchTermArray) throws ServerException
        {
            HashMap<String, ArrayList<String>> altTermsMap = new HashMap<>();
            ArrayList<StreamSearchQueryCondition> listConditions = new ArrayList<>();
            HashMap<String, Integer> boostVals = new HashMap<>();
            HashMap<String, String> swapTermsMap = new HashMap<>();

            StringBuffer keyWord = new StringBuffer();
            for (int i = 0; i < searchTermArray.size(); i++)
            {
                if (i != 0)
                {
                    keyWord.append(" ");
                }
                keyWord.append(sanitizeTerm(searchTermArray.get(i)));
            }
            /* first check with full term */
            boolean isCombinedCurate = updateCurate(httpClientService, listConditions, altTermsMap, swapTermsMap, campaign, searchTerm);
            updateSortOrder(boostVals, keyWord.toString());
            //logger.info("CURATE TERMS ARRAY keyword=" + keyWord + " searchtermsarray: " + searchTermArray);

            if (!isCombinedCurate)
            {
                for (String word : searchTermArray)
                {
                    updateCurate(httpClientService, listConditions, altTermsMap, swapTermsMap, campaign, word);
                }
            }

            return new Object[] { listConditions, altTermsMap, swapTermsMap, boostVals };
        }

        private boolean updateCurate(HttpClientService httpClientService,
                                     List<StreamSearchQueryCondition> listConditions,
                                     Map<String, ArrayList<String>> altTermsMap,
                                     Map<String, String> swapTermsMap,
                                     Campaign campaign,
                                     String word) throws ServerException
        {
            boolean toRet = false;
            String term = sanitizeTerm(word);
            Map<String, CuratedField> listFieldMap = this.curateTerm(httpClientService, campaign, term);
            //logger.info("Search Term: " + term + " Word: " + word + " List: " + listFields);
            if (listFieldMap != null && !listFieldMap.isEmpty())
            {
                if (listFieldMap.size() > 1 && listFieldMap.containsKey(SYNONYM))
                {
                    listFieldMap.remove(SYNONYM);
                }
                Iterator<Entry<String, CuratedField>> iterator = listFieldMap.entrySet().iterator();
                while (iterator.hasNext())
                {
                    Entry<String, CuratedField> entry = iterator.next();
                    CuratedField field = entry.getValue();
                    String origTerm = field.mapTerms.get(term);
                    HashSet<String> altTerms = this.alternateTerms(field, term);
                    if (altTerms != null && !altTerms.isEmpty())
                    {
                        ArrayList<String> terms = new ArrayList<>();
                        terms.add(origTerm);
                        terms.addAll(altTerms);
                        //logger.info("Term: " + term + " Alt Terms: " + altTerms + " field: " + field);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Term: " + term + " Alt Terms: " + altTerms + " field: " + field);
                        }
                        if (!field.fieldName.equalsIgnoreCase(SYNONYM))
                        {
                            listConditions.add(new StreamSearchQueryCondition(field.fieldName, terms, null, null, field.isMandatoryMatch, false, null));
                        }
                        altTermsMap.put(word, terms); // need the original unsanitized word as the key
                    }
                    else
                    {
                        String swapTerm = this.swapTerm(field, term);
                        //logger.info("Term: " + term + " Swap Term: " + swapTerm + " field: " + field);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Term: " + term + " Swap Term: " + swapTerm + " field: " + field);
                        }
                        swapTerm = (swapTerm == null ? (origTerm != null ? origTerm : word) : swapTerm);
                        swapTermsMap.put(word, swapTerm);
                        if (field.fieldName.equalsIgnoreCase(SYNONYM))
                        {
                            String[] terms = swapTerm.split(" ");
                            ArrayList<StreamSearchQueryCondition> altConditions = new ArrayList<>();
                            if (terms.length == 1)
                            {
                                altConditions.add(new StreamSearchQueryCondition(swapTerm, null, null, null, false, false, null));
                            }
                            else // for more words space issue
                            {
                                ArrayList<StreamSearchQueryCondition> tempConditions = new ArrayList<>();
                                for (String t : terms)
                                {
                                    tempConditions.add(new StreamSearchQueryCondition(t, null, null, null, true, false, null));
                                }
                                altConditions.add(new StreamSearchQueryCondition(null, null, null, null, false, false, tempConditions));
                            }
                            listConditions.add(new StreamSearchQueryCondition(null, null, null, null, field.isMandatoryMatch, false, altConditions));
                        }
                        else
                        {
                            listConditions.add(new StreamSearchQueryCondition(field.fieldName, swapTerm, null, null, field.isMandatoryMatch, false, null));
                        }
                    }

                }
                toRet = true;
            }
            return toRet;
        }

        private boolean updateSortOrder(HashMap<String, Integer> boostVals,
                                        String keyWord)
        {
            boolean toRet = false;
            if (sortOrderConfig.containsKey(keyWord))
            {
                ArrayList<String> values = sortOrderConfig.get(keyWord);
                if (values != null)
                {
                    for (int i = 0; i < values.size() && i < MAX_SORTORDER_ITEM; i++)
                    {
                        String key = values.get(i);
                        if (!boostVals.containsKey(key))
                        {
                            boostVals.put(key, ((MAX_SORTORDER_ITEM - i) * MAX_SORTORDER_BOOST_FACTOR + MAX_SORTORDER_BOOST_OFFSET));
                        }
                    }
                }
                toRet = true;
            }
            return toRet;

        }

        public Map<String, CuratedField> curateTerm(HttpClientService httpClientService,
                                                    Campaign campaign,
                                                    String term) throws ServerException
        {
            Map<String, CuratedField> termsList = new HashMap<>();
            //String term = sanitizeTerm(word); // curateTermsArray already sanitizes Do not sanitize here again as it does it twice and words like dress get affected and both 's' get removed.

            //logger.info("Curating term:" + term + " this.curatedFields=" + this.curatedFields);
            if (logger.isDebugEnabled())
            {
                logger.debug("Curating term:" + term + " this.curatedFields=" + this.curatedFields);
            }
            if (term.length() > 0)
            {
                loadMaps(httpClientService, campaign);
                //logger.info("Curating term2:" + term + " this.curatedFields=" + this.curatedFields);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Curating term2:" + term + " this.curatedFields=" + this.curatedFields);
                }
                Iterator<Map.Entry<String, CuratedField>> iter = this.curatedFields.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry<String, CuratedField> entry = iter.next();
                    CuratedField cf = entry.getValue();
                    if (cf.mapTerms.containsKey(term))
                    {
                        termsList.put(cf.fieldName, cf);
                        //logger.info("Found term:" + term + " in " + cf);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Found term:" + term + " in " + cf);
                        }
                    }
                    else
                    {
                        //logger.info("Skipped term:" + term + " in " + cf);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Skipped term:" + term + " in " + cf);
                        }
                    }
                }
            }
            return (termsList.size() > 0 ? termsList : null);
        }

    }

    static class CuratedField
    {
        static SkavaLogger logger = SkavaLoggerFactory.getLogger(CuratedField.class);

        public final String campaignPropertyName;
        public final String fieldName;
        public final boolean isMandatoryMatch;
        public final CuratedField synonymField;
        public final HashMap<String, HashSet<String>> synMap = new HashMap<String, HashSet<String>>();
        public final HashMap<String, String> swapMap = new HashMap<String, String>();
        public final HashMap<String, String> mapTerms = new HashMap<String, String>();

        // TODO: get from Campaign Properties
        public static final String PROP_CAMPAIGN_CURATELIST_VERSION = "search.curatelist.version";
        public static final String PROP_CAMPAIGN_CURATELIST_SETTINGS = "search.curatelist.settings";

        private static HashMap<Long, String> mapCurateListVersion = new HashMap<Long, String>();
        //private static final String propVersion = "version";
        private static final String propCampaignProperty = "propname";
        private static final String propFieldName = "field";
        private static final String propIsMandatory = "required";
        private static final String propFields = "fields";

        public CuratedField(Campaign campaign, String campaignPropertyName,
                            String fieldName, boolean isMandatoryMatch,
                            CuratedField synonymField) throws ServerException
        {
            this.campaignPropertyName = campaignPropertyName;
            this.fieldName = fieldName;
            this.isMandatoryMatch = isMandatoryMatch;
            this.synonymField = synonymField;
            this.loadFromProperty(campaign);
        }

        public static CuratedField[] loadFields(Campaign campaign) throws ServerException
        {
            CuratedField[] cfields = null;
            try
            {
                JSONObject jobj = null;

                String strDbVersion = ReadUtil.getString(campaign.getProperty(/*CampaignProperties.*/PROP_CAMPAIGN_CURATELIST_VERSION), null);
                String curVersion = mapCurateListVersion.get(campaign.getId());

                if (logger.isDebugEnabled())
                {
                    logger.debug("Curate Version DB: " + strDbVersion + " Myversion: " + curVersion);
                }

                if (strDbVersion != null && (curVersion == null || !curVersion.equals(strDbVersion)))
                {
                    String strFieldSettings = ReadUtil.getString(campaign.getProperty(/*CampaignProperties.*/PROP_CAMPAIGN_CURATELIST_SETTINGS), null);

                    if (strFieldSettings != null)
                    {
                        jobj = new JSONObject(strFieldSettings);
                    }
                    if (jobj != null)
                    {
                        JSONArray jarrFields = jobj.getJSONArray(propFields);
                        int numFields = jarrFields.length();
                        cfields = new CuratedField[numFields];
                        CuratedField synoymField = null;
                        for (int i = 0; i < numFields; i++)
                        {
                            JSONObject field = jarrFields.getJSONObject(i);
                            if (SYNONYM.equalsIgnoreCase(field.getString(propFieldName)))
                            {
                                synoymField = new CuratedField(campaign, field.getString(propCampaignProperty), field.getString(propFieldName), field.getBoolean(propIsMandatory), null);
                                break;
                            }
                        }
                        for (int i = 0; i < numFields; i++)
                        {
                            JSONObject field = jarrFields.getJSONObject(i);
                            if (!SYNONYM.equalsIgnoreCase(field.getString(propFieldName)))
                            {
                                cfields[i] = new CuratedField(campaign, field.getString(propCampaignProperty), field.getString(propFieldName), field.getBoolean(propIsMandatory), synoymField);
                            }
                            else
                            {
                                cfields[i] = synoymField;
                            }
                        }

                        mapCurateListVersion.put(campaign.getId(), strDbVersion);
                        logger.info("Loaded curate list version: " + strDbVersion);
                    }
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
            return cfields;
        }

        private void loadFromProperty(Campaign campaign) throws ServerException
        {
            String strProp = ReadUtil.getString(campaign.getProperty(this.campaignPropertyName), null);
            if (strProp != null)
            {
                String[] strings = strProp.split("\\s*,\\s*");

                if (strings != null)
                {
                    for (int i = 0; i < strings.length; i++)
                    {
                        // leads to double sanitization of certain strings that are at the end.
                        //strings[i] = TermCurator.sanitizeTerm(strings[i]);

                        String[] synstrings = strings[i].split("\\s*~\\s*");
                        if (synstrings != null && synstrings.length > 1)
                        {
                            String[] originalTerms = new String[synstrings.length];

                            for (int j = 0; j < synstrings.length; j++)
                            {
                                originalTerms[j] = synstrings[j];
                                synstrings[j] = TermCurator.sanitizeTerm(synstrings[j]);
                            }

                            for (int j = 0; j < synstrings.length; j++)
                            {
                                boolean flag = false;
                                mapTerms.put(synstrings[j], originalTerms[j]);
                                HashSet<String> list = synMap.get(synstrings[j]);
                                if (list == null)
                                {
                                    list = new HashSet<String>();
                                    synMap.put(synstrings[j], list);
                                }

                                for (int k = 0; k < synstrings.length; k++)
                                {
                                    if (synonymField != null && synonymField.synMap.containsKey(synstrings[k]))
                                    {
                                        list.addAll(synonymField.synMap.get(synstrings[k]));
                                        flag = true;
                                    }
                                    if (j == k) continue;
                                    list.add(originalTerms[k]);
                                }
                                if (flag)
                                {
                                    for (String value : list)
                                    {
                                        Iterator<Entry<String, String>> iterator = synonymField.mapTerms.entrySet().iterator();
                                        while (iterator.hasNext())
                                        {
                                            Entry<String, String> entry = iterator.next();
                                            if (entry.getValue().equalsIgnoreCase(value))
                                            {
                                                if (synonymField.synMap.containsKey(entry.getKey()))
                                                {
                                                    HashSet<String> temp = new HashSet<String>(list);
                                                    temp.add(originalTerms[j]);
                                                    temp.remove(entry.getValue());
                                                    synMap.put(entry.getKey(), temp);
                                                    mapTerms.put(entry.getKey(), entry.getValue());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            String[] swapstrings = strings[i].split("\\s*\\|\\s*");
                            if (swapstrings != null && swapstrings.length == 2)
                            {
                                String origTerm = swapstrings[0];
                                swapstrings[0] = TermCurator.sanitizeTerm(swapstrings[0]);
                                swapstrings[1] = /*TermCurator.sanitizeTerm*/(swapstrings[1]); // we intentionally do not sanitize here because we want to use dress instead of dres to query solr as it barfs if we use dres for the dress category
                                swapMap.put(swapstrings[0], swapstrings[1]);
                                logger.info("SWAP STRINGS: [" + fieldName + "] " + swapstrings[0] + " " + swapstrings[1]);
                                mapTerms.put(swapstrings[0], origTerm);
                            }
                            else
                            {
                                mapTerms.put(TermCurator.sanitizeTerm(strings[i]), strings[i]);
                            }
                        }
                    }
                }
            }
        }

        public String toString()
        {
            return "CuratedField[" + this.campaignPropertyName + ", " + this.fieldName + ", " + this.isMandatoryMatch + "]";
        }
    }
    
    private StreamSearchQuery addQueryParams(StreamSearchQuery query,
                                             long campaignId,
                                             String[] skuId,
                                             String[] productId,
                                             String name,
                                             String[] brand,
                                             String[] category,
                                             String[] categoryid,
                                             String categorylevel1,
                                             String categorylevel2,
                                             String categorylevel3,
                                             String categorylevel4,
                                             String categorylevel5,
                                             String variant,
                                             String[] color,
                                             String region,
                                             String version,
                                             boolean personalize,
                                             boolean online,
                                             boolean store,
                                             SelectedFacet[] advancedSearch,
                                             boolean includeGhostProduct,
                                             String[] segments,
                                             String catalogId,
                                             long storeId)
    {
        String methodName = "addQueryParams";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  query - {}, campaignId - {}, skuId - {}, productId - {}, name - {}, brand - {}, category - {}, categoryid - {}, categorylevel1 - {}, categorylevel2 - {}, categorylevel3 - {}, categorylevel4 - {}, categorylevel5 - {}, variant - {}, color - {}, region - {}, version - {},  personalize - {}, online - {}, store - {}, advancedSearch - {}, includeGhostProduct - {}, segments - {}, catalogId - {}", null, false, null, this.getClass().getSimpleName(), query, campaignId, skuId, productId, name, brand, category, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, variant, color, region, version,  personalize, online, store, advancedSearch, includeGhostProduct, segments, catalogId);
        query.addCondition(new StreamSearchQueryCondition("campaignid", campaignId, null, null, true, false, null));
        query.addCondition(new StreamSearchQueryCondition("available", "true", null, null, true, false, null));
        if(!includeGhostProduct)
        {
            query.addCondition(new StreamSearchQueryCondition("isvisible", "true", null, null, true, false, null));
        }
        if (online && store)
        {
            ArrayList<StreamSearchQueryCondition> conditions = new ArrayList<StreamSearchQueryCondition>();
            conditions.add(new StreamSearchQueryCondition("online", "true", null, null, false, false, null));
            conditions.add(new StreamSearchQueryCondition("store", "true", null, null, false, false, null));
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, conditions));
        }
        else if (online)
        {
            query.addCondition(new StreamSearchQueryCondition("online", "true", null, null, true, false, null));
        }
        else if (store)
        {
            query.addCondition(new StreamSearchQueryCondition("store", "true", null, null, true, false, null));
        }
        if (skuId != null)
        {
            ArrayList<StreamSearchQueryCondition> skuidCond = new ArrayList<StreamSearchQueryCondition>();
            for (int i = 0; i < skuId.length; i++)
            {
                skuidCond.add(new StreamSearchQueryCondition("skuid", skuId[i], null, null, false, false, null));
            }
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, skuidCond));
        }
        if (advancedSearch != null)
        {
            for (int i = 0; i < advancedSearch.length; i++)
            {
                String selectedkey = advancedSearch[i].getKey().replaceAll(" ", "_");
                String[] selectedKeyValues = advancedSearch[i].getValue();
                ArrayList<StreamSearchQueryCondition> keyCond = new ArrayList<StreamSearchQueryCondition>();
                for (int j = 0; j < selectedKeyValues.length; j++)
                {
                    String facetKeyValue = "search_" + selectedkey;
                    keyCond.add(new StreamSearchQueryCondition(facetKeyValue, selectedKeyValues[j], null, null, false, false, null));
                }
                StreamSearchQueryCondition searchKeyCondition = new StreamSearchQueryCondition(null, null, null, null, true, false, keyCond);
                query.addCondition(searchKeyCondition);
            }
        }
        if (productId != null && productId.length > 0)
        {
            ArrayList<StreamSearchQueryCondition> ProdIdCond = new ArrayList<StreamSearchQueryCondition>();
            for (int i = 0; i < productId.length; i++)
            {
                String prodid = ReadUtil.getString(productId[i], null);
                if (prodid != null)
                {
                    ProdIdCond.add(new StreamSearchQueryCondition("productid", prodid, null, null, false, false, null));
                }
            }
            if (ProdIdCond.size() > 0)
            {
                query.addCondition(new StreamSearchQueryCondition(null, null, null, null, (personalize ? false : true), false, ProdIdCond));
            }
        }
        if (name != null)
        {
            query.addCondition(new StreamSearchQueryCondition("name", name, null, null, (personalize ? false : true), false, null));
        }
        if (brand != null)
        {
            ArrayList<StreamSearchQueryCondition> cond = arrayofQuery("brand", brand);
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, (personalize ? false : true), false, cond));
        }
        if (category != null)
        {
            ArrayList<StreamSearchQueryCondition> catCond = arrayofQuery("category", category);
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, (personalize ? false : true), false, catCond));
        }
        if (categoryid != null)
        {
            ArrayList<StreamSearchQueryCondition> catCond = arrayofQuery("categoryid", categoryid);
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, (personalize ? false : true), false, catCond));
        }
        if (categorylevel1 != null)
        {
            query.addCondition(new StreamSearchQueryCondition("categorylevel1", categorylevel1, null, null, (personalize ? false : true), false, null));
        }
        if (categorylevel2 != null)
        {
            query.addCondition(new StreamSearchQueryCondition("categorylevel2", categorylevel2, null, null, (personalize ? false : true), false, null));
        }
        if (categorylevel3 != null)
        {
            query.addCondition(new StreamSearchQueryCondition("categorylevel3", categorylevel3, null, null, (personalize ? false : true), false, null));
        }
        if (categorylevel4 != null)
        {
            query.addCondition(new StreamSearchQueryCondition("categorylevel4", categorylevel4, null, null, (personalize ? false : true), false, null));
        }
        if (categorylevel5 != null)
        {
            query.addCondition(new StreamSearchQueryCondition("categorylevel5", categorylevel5, null, null, (personalize ? false : true), false, null));
        }
        if (variant != null)
        {
            query.addCondition(new StreamSearchQueryCondition("variant", variant, null, null, (personalize ? false : true), false, null));
        }
        if (region != null)
        {
            query.addCondition(new StreamSearchQueryCondition("region", region, null, null, true, false, null));
        }
        if (version != null)
        {
            query.addCondition(new StreamSearchQueryCondition(ProductIndexFields.PUBLISH_VERSION, version, null, null, true, false, null));
        }
        if (color != null)
        {
            ArrayList<StreamSearchQueryCondition> cond = arrayofQuery("color", color);
            query.addCondition(new StreamSearchQueryCondition(null, null, null, null, (personalize ? false : true), false, cond));
        }
        if(segments != null)
        {
            ArrayList<StreamSearchQueryCondition> segmentCond = arrayofQuery("segment", segments);
            StreamSearchQueryCondition segmentQuery = new StreamSearchQueryCondition(null, null, null, null, true, false, segmentCond);
            segmentQuery.setAnd(true);
            query.addCondition(segmentQuery);
        }
        if (catalogId != null)
        {
            query.addCondition(new StreamSearchQueryCondition("catalogid", catalogId, null, null, true, false, null));
        }
        if (storeId > 0)
        {
            query.addCondition(new StreamSearchQueryCondition("storeid", String.valueOf(storeId), null, null, true, false, null));
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : query - {} ", null, false, null, this.getClass().getSimpleName(), query);
        return query;
    }
    
    private String spellCheck(HttpServletRequest request, Campaign campaign, StreamSearchConfig config, String searchTerm, String listVersion, boolean disableFacetMinCount, boolean disableFacetLimit, boolean spellcheck) throws ServerException
    {
        String methodName = "spellCheck";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  request - {}, campaign - {}, config - {}, searchTerm - {}, listVersion - {}, disableFacetMinCount - {}, disableFacetLimit - {}, spellcheck - {}", null, false, null, this.getClass().getSimpleName(), request, campaign, config, searchTerm, listVersion, disableFacetMinCount, disableFacetLimit, spellcheck);
        if (searchTerm != null && searchTerm.length() > 0)
        {
            StreamSearchQuery spellquery = new StreamSearchQuery();
            
            StreamSearchResponse spellresult = streamSearchKraftService.doSearch(request.getServerName(), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, listVersion, spellquery, null, null, null, null, null, 0, 0, config, spellcheck, (searchTerm != null && searchTerm.length() > 0 ? searchTerm.toLowerCase() : searchTerm), null, null, disableFacetMinCount, disableFacetLimit, false);
            HashMap<String, String> spell = spellresult.getSpellCheck();
            String[] searchArr = searchTerm.split(" ");
            ArrayList<String> wordBreak = new ArrayList<String>();
            for (int indxi = 0; indxi < searchArr.length; indxi++)
            {
                String strToappend = searchArr[indxi];
                if (indxi + 1 < searchArr.length)
                {
                    String mergedStr = strToappend;
                    for (int indxj = indxi + 1; indxj < searchArr.length; indxj++)
                    {
                        mergedStr = (mergedStr + " " + searchArr[indxj]).toLowerCase();
                        if (spell.containsKey(mergedStr))
                        {
                            strToappend = spell.get(mergedStr);
                            indxi = indxj;
                        }
                    }
                }
                wordBreak.add(strToappend);
            }
            String[] searchArrs = wordBreak.toArray(new String[wordBreak.size()]);
            StringBuffer strBuffer = new StringBuffer();
            for (int i = 0; i < searchArrs.length; i++)
            {
                if (spell.containsKey(searchArrs[i].toLowerCase()))
                {
                    searchArrs[i] = spell.get(searchArrs[i].toLowerCase());
                }
                strBuffer.append(searchArrs[i]);
                if (i + 1 < searchArrs.length)
                {
                    strBuffer.append(" ");
                }
            }
            searchTerm = strBuffer.toString();
            request.setAttribute("spellcheck", searchTerm);
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : searchTerm - {} ", null, false, null, this.getClass().getSimpleName(), searchTerm);
        return searchTerm;
    }
    
    public ArrayList<StreamSearchQueryCondition> arrayofQuery(String field,
                                                              String[] terms)
    {
        ArrayList<StreamSearchQueryCondition> toRet = new ArrayList<StreamSearchQueryCondition>();
        for (int i = 0; i < terms.length; i++)
        {
            toRet.add(new StreamSearchQueryCondition(field, terms[i], null, null, false, false, null));
        }
        return toRet;
    }
    
    private StringBuffer createBq(StringBuffer bq,
                                  String key,
                                  String value,
                                  int boost)
    {
        String methodName = "createBq";
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  bq - {}, key - {}, value - {}, boost - {}", null, false, null, this.getClass().getSimpleName(), bq, key, value, boost);
        if (bq != null && key != null && value != null && boost > 0)
        {
            if (bq.length() > 0)
            {
                bq.append(" ");
            }
            bq.append("(" + key + ":");
            bq.append("\"" + value + "\"");
            bq.append(")^");
            bq.append(boost);
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : createBq - {} ", null, false, null, this.getClass().getSimpleName(), bq);
        return bq;
    }
    
    private Campaign getCampaign(DBSessionManager dbSesManager,
                                 long storeId,
                                 String service) throws ServerException
    {
        Campaign campaign = null;
        long pimCampaignId = CampaignUtil.getCampaignIdByStoreId(dbSesManager.getReadOnlyDBSession(), storeId, service);
        if (pimCampaignId > 0)
        {
            campaign = CampaignUtil.loadCampaign(dbSesManager, pimCampaignId);
        }
        else
        {
            campaign = CampaignUtil.loadCampaign(dbSesManager, storeId);
        }
        return campaign;
    }

    /**
     * Search service have a defined solr schema to maintain the Product information in solr. We've feed processor to index the Product data in to the solr as per the solr schema.
       Using this API we can retrieve the indexed products from solr. This API filtering and sorting. Using this API we can perform open search, search by product name, color, size, category name and price.
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter for all the microservice.
     * @param searchTerm This parameter takes the search term as value which used to perform general search. The products with the search term matched with any indexed field will be responded
     * @param selectedFacets It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.
     * @param sort It is used to apply sorting for the products response from solr based on the particular field. Example: price|desc. This sorts the products with indexed field price in descending order
     * @param group This parameter used to group the products based on the given query parameter values
     * @param responseFormatterClass This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100.
     * @param edismax This is a boolean parameter which is used to mention the whether needs to enable the Extended DisMax query parser or not
     * @param region Products will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param version This parameter accepts the publish version
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @Override
    public Response getGroupSuggestion(HttpServletRequest request,
                                       long storeId,
                                       String searchTerm,
                                       String responseFormatterClass,
                                       String region,
                                       MultiFacetsKraft userPreferences,
                                       boolean edismax,
                                       int groupLimit,
                                       String catalogId,
                                       SelectedKraftFacet[] selectedFacets) throws ServerException
    {
        Response toRet = null;
        String selectedSort = null;
        String sortStrTemp = null;
        String sortOrder = null;
        String sortNumTemp = null;
        List<String> prefacet = null;
        List<String> presort = null;
        String[] sortArr = null;
        String methodName = "getProducts";
        boolean personalize = false;
        try
        {
            /* logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  request - {}, storeId - {}, skuId - {}, productId - {}, name - {}, brand - {}, category - {}, categoryid - {}, categorylevel1 - {}, categorylevel2 - {}, categorylevel3 - {}, categorylevel4 - {}, categorylevel5 - {}, division - {}, color - {}, size1 - {}, size2 - {}, priceMin - {}, priceMax - {}, variant - {}, searchTerm - {}, facets - {}, selectedFacets - {}, sort - {}, group - {}, responseFormatterClass - {},  offset - {}, limit - {}, usev2 - {}, edismax - {}, iszeroResult - {}, spellcheck - {32}, personalize - {}, contextualParam - {}, region - {}, version - {}, curate - {}, online - {}, store - {}, advancedSearch - {}, disableFacetMinCount - {}, includeGhostProduct - {}, includeFacet - {}, disableFacetLimit - {}, segments - {}, catalogId - {}", null, false, null, this.getClass().getSimpleName(), request, storeId, skuId, productId, name, brand, category, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, color, size1, size2, priceMin, priceMax, variant, searchTerm, facets, selectedFacets, sort, group, responseFormatterClass, offset, limit, usev2, edismax, iszeroResult, spellcheck, personalize, contextualParam, region, version, curate, online, store, advancedSearch, disableFacetMinCount, includeGhostProduct, includeFacet, disableFacetLimit, segments, catalogId); */
            request.setAttribute("searchterm", searchTerm);
            region = ReadUtil.getString(region, null);
            request.setAttribute("region", region);

            Campaign campaign = getCampaign(dbSessionManager, storeId, "pim");
            if (campaign == null || campaign.getId() <= 0) { throw new ServerException("Invalid Campaign"); }
            long campaignId = campaign.getId();

            Partner partner = PartnerUtil.loadPartner(dbSessionManager, campaign.getPartnerid());
            if (partner == null || partner.getId() <= 0) { throw new ServerException("Invalid Partner"); }

            if (catalogId == null)
            {
                DBSession dbSession = dbSessionManager.getReadOnlyDBSession();
                try
                {
                    catalogId = CampaignUtil.getPimCatalogId(dbSession, storeId);
                    catalogId = ReadUtil.getString(catalogId, null) == null ? DEFAULT_SOLR_CATALOGID : catalogId;
                }
                catch (Exception e)
                {
                    dbSession.endSession();
                    logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaKraftSearchService - {} :  Catalogid - {}", null, false, null, this.getClass().getSimpleName(), null);
                }
                finally
                {
                    dbSession.endSession();
                }
            }
            String listVersion = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_LIST_VERSION), LIST_VERSION);
            StreamSearchConfig config = streamSearchService.getPartnerConfig(request.getServerName(), campaign, 3, listVersion);
            if (searchTerm != null)
            {
                JSONObject jPer = new JSONObject(ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_ENABLEPERSONALIZE_SEARCH), "{}"));
                personalize = (jPer.has("enablePersonalizedSearch") && jPer.getString("enablePersonalizedSearch").equals("1") ? true : false);
            }

            String defaultGroup = ReadUtil.getString(campaign.getProperty(PimKraftConstants.PROP_SEARCH_DEFAULT_GROUP_FIELD), null);

            defaultGroup = defaultGroup != null ? "sort_" + defaultGroup + "_" + region + "_" + campaignId : null;

            String sort = campaign.getProperty(PimKraftConstants.PROP_TYPEAHEAD_DEFAULTFIELD_SORT);
            if (sort != null)
            {
                sortArr = sort.split(",");
                if (sortArr != null && sortArr.length > 0)
                {
                    ArrayList<String> sortList = new ArrayList<>();
                    for (int i = 0; i < sortArr.length; i++)
                    {
                        String[] sortArrTemp = sortArr[i].split("\\|");
                        if (sortArrTemp != null && sortArrTemp.length == 2)
                        {
                            if (selectedSort == null)
                            {
                                selectedSort = sortArr[i];
                            }
                            else
                            {
                                selectedSort += "," + sortArr[i];
                            }
                            sortList.addAll(Arrays.asList(sortArrTemp));
                        }
                        else
                        {
                            throw new ServerException("Invalid sort parameter");
                        }
                    }
                    sortArr = sortList.toArray(new String[sortList.size()]);
                }
            }

            List<String> luceneFields = null;
            MemCacheV2<List<String>> searchLukeCache = new MemCacheFactory<List<String>>().getCache(SEARCH_LUKE_CACHE, memCacheManager);
            String baseKey = request.getServerName() != null ? request.getServerName() : null;
            String cacheKey = String.valueOf(campaignId);
            if (searchLukeCache != null)
            {
                luceneFields = searchLukeCache.get(baseKey, cacheKey);
            }
            if (luceneFields == null || luceneFields.isEmpty())
            {
                if (lukeFromDb)
                {
                    StreamSearchResponse fields = streamSearchService.getLuceneFields(campaignId);
                    if (fields != null && fields.getSearchLuceneFields() != null)
                    {
                        luceneFields = fields.getSearchLuceneFields().getValue();
                    }
                }
                else
                {
                    boolean skipLukeCall = ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_PRODUCT_LUCENE_FIELDS_FROM_S3), false);
                    if (skipLukeCall)
                    {
                        String catalogServiceId = String.valueOf(campaignId) + "_" + PRODUCT;
                        luceneFields = SolrUtil.getLuceneFieldsFromS3(streamCatalogService, memCacheManager, httpClientService, request, null, "v1", partner.getName(), catalogServiceId, 0, null);
                    }
                    else
                    {
                        luceneFields = SolrUtil.getLuceneFieldsAsListByCampaignId(config, campaign.getId(), httpClientService); // getting field names from luke // TODO:MERCHANDIZE luke response cache
                    }
                }

                if (luceneFields != null && searchLukeCache != null)
                {
                    searchLukeCache.put(baseKey, cacheKey, luceneFields, false);
                }
                else
                {
                    logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), null, 0, "Luke Call Failed!!! - possible sorting options and default facets won't be displayed", methodName, "Unknown Exception occurred while processing workitem @{}", null, false, null, methodName);
                }
            }

            logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  luceneFields - {}", null, false, null, this.getClass().getSimpleName(), luceneFields);

            ArrayList<String> possibleSorts = new ArrayList<>();
            ArrayList<String> possibleFacets = new ArrayList<>();

            if (luceneFields != null)
            {
                Iterator<String> fieldIterator = luceneFields.iterator();
                while (fieldIterator.hasNext())
                {
                    String key = fieldIterator.next();
                    if (key.startsWith(FACET_FILTER_PREFIX) && key.endsWith("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)))
                    {
                        if (prefacet != null)
                        {
                            String keyTemp = key.substring(FACET_FILTER_PREFIX.length(), key.indexOf("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)));
                            if (prefacet.contains(keyTemp))
                            {
                                possibleFacets.add(key);
                            }
                        }
                        else
                        {
                            possibleFacets.add(key);
                        }
                    }
                    if (key.startsWith("sort") && key.endsWith("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)) && !key.contains("sequence"))
                    {
                        if (presort != null)
                        {
                            String keyTemp = key.substring("sort_".length(), key.indexOf("_" + (region != null ? region + "_" : "") + String.valueOf(campaignId)));
                            if (presort.contains(keyTemp))
                            {
                                possibleSorts.add(key);
                            }
                        }
                        else
                        {
                            possibleSorts.add(key);
                        }
                    }
                }

            }

            String defaultSortStr = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_SEARCH_DEFAULT_SORT), null);
            ArrayList<String> defaultSortFields = new ArrayList<>();
            if (defaultSortStr != null)
            {
                defaultSortFields = new ArrayList<String>(Arrays.asList(defaultSortStr.split(",")));
            }

            if ((!defaultSortFields.isEmpty() || !possibleSorts.isEmpty()) && sortArr != null && sortArr.length > 0)
            {

                ArrayList<String> sortTemp = new ArrayList<>();
                String sortField = null;
                for (int i = 0; i < sortArr.length; i += 2)
                {
                    String sortString = sortArr[i].replaceAll(" ", "_");
                    sortNumTemp = luceneFields.contains(sortArr[i]) ? (sortArr[i]) : ("sort_" + sortString + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId));
                    sortStrTemp = luceneFields.contains(sortArr[i]) ? (sortArr[i]) : ("sortstr_" + sortString + "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId));
                    sortOrder = sortArr[i + 1];
                    if (sortStrTemp != null && ((possibleSorts != null && possibleSorts.contains(sortStrTemp)) || defaultSortFields.contains(sortStrTemp)))
                    {
                        sortField = sortStrTemp;
                    }
                    else if (sortNumTemp != null && ((possibleSorts != null && possibleSorts.contains(sortNumTemp)) || defaultSortFields.contains(sortNumTemp)))
                    {
                        sortField = sortNumTemp;
                    }
                    else
                    {
                        sortField = sortArr[i];
                    }
                    if (sortOrder != null && sortField != null)
                    {
                        sortField += " " + sortOrder;
                    }
                    if (sortField != null)
                    {
                        sortTemp.add(sortField);
                    }
                }

                if (!sortTemp.isEmpty())
                {
                    sort = StringUtil.mergeStrings(sortTemp.toArray(new String[sortTemp.size()]), ",");
                }
            }

            StreamSearchQuery query = new StreamSearchQuery();
            StreamSearchQuery facetQuery = null;
            StreamSearchResponse result = null;
            StreamSearchResponse curratedresult = null;
            query = addQueryParams(query, campaignId, null, null, null, null, null, null, null, null, null, null, null, null, null, region, null, personalize, false, false, null, false, null, catalogId, storeId);

            if (edismax)
            {
                if (userPreferences != null && userPreferences.getSelectedFacets() != null && userPreferences.getSelectedFacets().length > 0)
                {
                    ArrayList<SelectedKraftFacet> selectedFacetList = new ArrayList<SelectedKraftFacet>()
                    {};
                    for (SelectedKraftFacet selectedFacet : userPreferences.getSelectedFacets())
                    {
                        SelectedKraftFacet facet = new SelectedKraftFacet();
                        facet.setKey(selectedFacet.getKey());
                        facet.setValue(selectedFacet.getValue());
                        facet.setOperation(selectedFacet.getOperation());
                        selectedFacetList.add(facet);
                    }
                    selectedFacetList.addAll(Arrays.asList(selectedFacets));
                    selectedFacets = (!selectedFacetList.isEmpty()) ? selectedFacetList.toArray(new SelectedKraftFacet[selectedFacetList.size()]) : null;
                }
                if (selectedFacets != null)
                {
                    HashMap<String, List<String>> selectedFacetNamesValues = new HashMap<>();
                    for (int i = 0; i < selectedFacets.length; i++)
                    {
                        String selectedFacetkey = selectedFacets[i].getKey().replaceAll(" ", "_");
                        String[] selectedFacetValues = selectedFacets[i].getValue();
                        ArrayList<StreamSearchQueryCondition> facetCond = new ArrayList<>();
                        String suffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
                        selectedFacetNamesValues.put(FACET_FILTER_PREFIX + selectedFacetkey + suffix, Arrays.asList(selectedFacetValues));
                        for (int j = 0; j < selectedFacetValues.length; j++)
                        {
                            String facetSuffix = (campaign.getBooleanProperty(CampaignProperties.PROP_CAMPAIGNBASEDINDEXING, true)) ? "_" + (region != null ? region + "_" : "") + String.valueOf(campaignId) : "";
                            String facetKeyValue = FACET_FILTER_PREFIX + selectedFacetkey + facetSuffix;
                            facetCond.add(new StreamSearchQueryCondition(facetKeyValue, selectedFacetValues[j], null, null, false, false, null));
                        }
                        StreamSearchQueryCondition facetCondition = new StreamSearchQueryCondition(null, null, null, null, true, (selectedFacets[i].getOperation() == SelectedKraftFacet.FACET_EXCLUDE), facetCond);
                        query.addCondition(facetCondition);
                        if (facetQuery == null)
                        {
                            facetQuery = new StreamSearchQuery();
                        }
                        facetQuery.addCondition(facetCondition);
                    }
                    if (selectedFacetNamesValues.size() > 0)
                    {
                        query.setSelectedFacetNames(selectedFacetNamesValues);
                    }
                }

                ArrayList<StreamSearchQueryCondition> searchCondition = new ArrayList<>();
                ArrayList<StreamSearchQueryCondition> curateCondition = new ArrayList<>();

                curateCondition.add(new StreamSearchQueryCondition(searchTerm, null, null, null, true, false, null));
                searchCondition.add(new StreamSearchQueryCondition(null, null, null, null, false, false, curateCondition));
                query.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, searchCondition));
                
                String searchFields = campaign.getProperty(CampaignProperties.PROP_SEARCH_WEIGHT_CONFIG);
                SearchWeightConfig customSearchFields = null;
                HashMap<String, String> searchBoostMap = null;
                if (searchFields != null)
                {
                    StringBuilder qf = new StringBuilder();
                    customSearchFields = (SearchWeightConfig) CastUtil.fromJSON(searchFields, SearchWeightConfig.class);
                    searchBoostMap = customSearchFields.getConfigureBoostValues();
                    boolean first = true;
                    for (Entry<String, String> entry : searchBoostMap.entrySet())
                    {
                        float boost = ReadUtil.getFloat(entry.getValue(), 0f);
                        if (boost != 0f)
                        {
                            if (!first)
                            {
                                qf.append(" ");
                            }
                            else
                            {
                                first = false;
                            }
                            qf.append(entry.getKey());
                            qf.append("^");
                            qf.append(boost);
                        }
                    }
                    query.setQf(qf.toString());
                }
                query.setAdditionalParams("group=true&group.field=" + defaultGroup + "&group.limit=" + groupLimit + "&defType=edismax&stopwords=true&lowercaseOperators=true&indent=true&mm=99%25");

                result = streamSearchKraftService.doSearch(request.getServerName(), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, listVersion, query, null, null, sort, defaultGroup, null, 0, 4, config, false, searchTerm, null, null, false, false, false);
            }
            else
            {
                if (searchTerm != null)
                {
                    query.setAdditionalParams("group=true&group.field=" + defaultGroup + "&group.limit=" + groupLimit);
                    constructQuery(campaign, campaignId, query, searchTerm, false, true, 0, null, null, null, null, null, catalogId, false, region);
                    result = streamSearchKraftService.doSearch(request.getServerName(), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, listVersion, query, null, null, sort, defaultGroup, null, 0, 4, config, false, searchTerm, null, null, false, false, false);
                }
            }

            if (result != null || curratedresult != null)
            {
                if (selectedSort != null)
                {
                    result.setSelectedSort(selectedSort);
                }

                SearchResponseFormatter searchResponseFormatter = (SearchResponseFormatter) Class.forName(responseFormatterClass).newInstance();
                toRet = searchResponseFormatter.format(request, result, 4, campaign, null, true, curratedresult, null, null);
                if (toRet == null) { throw new ServerException(ServerException.ERR_NONE, "No match found"); }
            }
        }
        catch (Exception e)
        {
            logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
            toRet = new Response((e instanceof ServerException) ? ((ServerException) e).getErrorCode() : ServerException.ERR_UNKNOWN, e.getMessage());
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETGROUPSUGGESTION, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : response - {} ", null, false, null, this.getClass().getSimpleName(), CastUtil.toJSON(toRet));
        return toRet;
    }

    @Override
    public Response suggestDict(HttpServletRequest request,
                                long storeId,
                                String searchTerm,
                                String responseFormatterClass,
                                int offset,
                                int limit,
                                String region) throws ServerException
    { Response toRet = null;
    String methodName = "suggestDict";
    try
    {
        Campaign campaign = getCampaign(dbSessionManager, storeId, "pim");
        String version = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_LIST_VERSION), LIST_VERSION);
        
        String catalogId = null;
        DBSession dbSession = dbSessionManager.getReadOnlyDBSession();
        try
        {
            catalogId = CampaignUtil.getPimCatalogId(dbSession, storeId);
        }
        catch (Exception e)
        {
            dbSession.endSession();
            logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  catalogid - {}", null, false, null, this.getClass().getSimpleName(), null);

        }
        finally
        {
            dbSession.endSession();
        }

        toRet = streamSearchKraftService.doSuggestDict((request != null ? request.getServerName() : null), campaign, StreamSearchService.SEARCH_DOMAIN_PRODUCT, version, searchTerm, responseFormatterClass, offset, limit, region, catalogId, storeId);
        if (toRet == null) { throw new ServerException(ServerException.ERR_NONE, "No match found"); }
    }
    catch (Exception e)
    {
        toRet = new Response((e instanceof ServerException) ? ((ServerException) e).getErrorCode() : ServerException.ERR_UNKNOWN, e.getMessage());
    }
    return toRet;}
}
