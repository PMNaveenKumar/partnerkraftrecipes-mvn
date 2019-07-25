package com.skava.searchv2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skava.cache.MemCacheFactory;
import com.skava.cache.MemCacheManager;
import com.skava.cache.MemCacheV2;
import com.skava.dao.PartnerPropertiesDAO;
import com.skava.db.DBSession;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SearchService;
import com.skava.interfaces.SuggestResponseFormatter;
import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.dbbeans.PartnerProperties;
import com.skava.model.http.SkavaHttpRequest;
import com.skava.model.http.SkavaHttpResponse;
import com.skava.model.search.SolrSearchKraftResponse;
import com.skava.model.searchv2.SearchConstants;
import com.skava.model.searchv2.StreamSearchConfig;
import com.skava.model.searchv2.StreamSearchConfigsV2;
import com.skava.model.searchv2.StreamSearchGroupResult;
import com.skava.model.searchv2.StreamSearchQuery;
import com.skava.model.searchv2.StreamSearchQueryCondition;
import com.skava.model.searchv2.StreamSearchResponse;
import com.skava.model.searchv2.StreamSearchResponseFacetValue;
import com.skava.model.searchv2.StreamSearchResult;
import com.skava.model.searchv2.solr.SolrSearchResponseGroupItem;
import com.skava.model.searchv2.solr.SolrSearchResponseGroupModel;
import com.skava.model.searchv2.solr.SolrSearchResponseModel;
import com.skava.services.HttpClientService;
import com.skava.services.StreamSearchKraftService;
import com.skava.services.StreamSearchService;
import com.skava.util.CastUtil;
import com.skava.util.EncodeUtil;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.URLUtil;

public class StreamSearchKraftServiceImpl implements StreamSearchKraftService
{
    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());

    private ObjectMapper _mapper = new ObjectMapper(); // can reuse, share globally

    private DBSessionManager dbSessionManager;
    private MemCacheManager memCacheManager;
    private HttpClientService httpClientService;

    private String searchConfigPathV2;
    private SearchService searchService;

    public static final String SEARCH_CONFIG_CACHE = "searchconfigV2";
    public static final String SEARCH_COLL_NAME_PRODUCT_CATALOG = "product_skavacommerce";
    public static final String SEARCH_DOMAIN_PRODUCT_CATALOG = "solr-product-";
    public static final String SEARCH_DOMAIN_LIST_CATALOG = "solr-list-";
    public static final String SEARCH_DOMAIN_LIST_ITEM_CATALOG = "solr-listitem-";
    public static final String SEARCH_DOMAIN_BASIC_SEARCH_CATALOG = "solr-basicsearch-";
    public static final String SEARCH_DOMAIN_EVENT_CATALOG = "solr-event-";
    public static final String SEARCH_VERSION = "v5";

    private static final int NUM_BASIC_PARALLEL_CALLS = 10;

    private static final int NUM_MAX_PARALLEL_CALLS = 10;

    private static final long KEEP_ALIVE_MSECS = 2000;
    Executor poolExecutor = new ThreadPoolExecutor(NUM_BASIC_PARALLEL_CALLS, NUM_MAX_PARALLEL_CALLS, KEEP_ALIVE_MSECS, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    CompletionService<StreamSearchResponse> ecs = new ExecutorCompletionService<StreamSearchResponse>(poolExecutor);

    public StreamSearchKraftServiceImpl(DBSessionManager dbSessionManager,
                                        MemCacheManager memCacheManager,
                                        HttpClientService httpClientService,
                                        String searchConfigPathV2,
                                        SearchService searchService)
    {
        this.dbSessionManager = dbSessionManager;
        this.memCacheManager = memCacheManager;
        this.httpClientService = httpClientService;
        this.searchConfigPathV2 = searchConfigPathV2;
        this.searchService = searchService;
    }

    public StreamSearchConfig getPartnerConfig(String serverName,
                                               Campaign campaign,
                                               int searchDomainType,
                                               String searchDomainVersion) throws ServerException
    {
        String methodName = "getPartnerConfig";
        logger.writeLog(Level.DEBUG, "StreamSearchService", "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  serverName - {}, campaign - {}, searchDomainType - {}, searchDomainVersion - {}", null, false, null, this.getClass().getSimpleName(), serverName, campaign, searchDomainType, searchDomainVersion);
        return getPartnerConfig(serverName, campaign, searchDomainType, searchDomainVersion, false);
    }

    public StreamSearchConfig getPartnerConfig(String serverName,
                                               Campaign campaign,
                                               int searchDomainType,
                                               String searchDomainVersion,
                                               boolean configFromString) throws ServerException
    {
        StreamSearchConfig toRet = null;
        String methodName = "getPartnerConfig";
        logger.writeLog(Level.DEBUG, "StreamSearchService", "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  serverName - {}, campaign - {}, searchDomainType - {}, searchDomainVersion - {}, configFromString - {}", null, false, null, this.getClass().getSimpleName(), serverName, campaign, searchDomainType, searchDomainVersion, configFromString);
        MemCacheV2<StreamSearchConfig> searchConfigCache = new MemCacheFactory<StreamSearchConfig>().getCache(SEARCH_CONFIG_CACHE, memCacheManager);
        String baseKey = serverName != null ? serverName : "FromFeed";
        String cacheKey = campaign.getId() + "~" + searchDomainType + "~" + searchDomainVersion;

        if (toRet == null)
        {
            String searchDomain = null;
            String domainPropName = getCampaignPropertyName(searchDomainType);
            if (campaign != null && domainPropName != null)
            {
                searchDomain = campaign.getProperty(domainPropName);
                if (searchDomain == null)
                {
                    switch (domainPropName)
                    {
                    case CampaignProperties.PROP_SEARCH_DOMAIN_BASIC_SEARCH:
                        searchDomain = SEARCH_DOMAIN_BASIC_SEARCH_CATALOG;
                        break;
                    case CampaignProperties.PROP_SEARCH_DOMAIN_EVENT:
                        searchDomain = SEARCH_DOMAIN_EVENT_CATALOG;
                        break;
                    case CampaignProperties.PROP_SEARCH_DOMAIN_PRODUCT_CATALOG:
                        searchDomain = SEARCH_DOMAIN_PRODUCT_CATALOG;
                        break;
                    case CampaignProperties.PROP_SEARCH_DOMAIN_LIST:
                        searchDomain = SEARCH_DOMAIN_LIST_CATALOG;
                        break;
                    case CampaignProperties.PROP_SEARCH_DOMAIN_LIST_ITEM:
                        searchDomain = SEARCH_DOMAIN_LIST_ITEM_CATALOG;
                        break;
                    default:
                        break;
                    }
                }
            }

            DBSession session = dbSessionManager.getReadOnlyDBSession();
            try
            {
                if (searchDomain != null)
                {
                    cacheKey += "~V2";
                    if (searchConfigCache != null)
                    {
                        toRet = searchConfigCache.get(baseKey, cacheKey);
                    }
                    if (toRet == null && (searchConfigPathV2 == null || searchConfigPathV2.length() == 0)) { throw new ServerException("searchConfigPathV2 is empty"); }
                    if (toRet == null)
                    {
                        String propertyName = getPartnerPropertyName(searchDomainType);
                        PartnerProperties partnerCollectionName = (new PartnerPropertiesDAO()).loadPropertyByPartnerIdAndPropName(session, campaign.getPartnerid(), propertyName);
                        if (propertyName.equals(PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_PRODUCT_CATALOG) && (partnerCollectionName == null || (partnerCollectionName != null && partnerCollectionName.getId() == 0)))
                        {
                            partnerCollectionName = new PartnerProperties();
                            partnerCollectionName.setId(1);
                            partnerCollectionName.setValue(SEARCH_COLL_NAME_PRODUCT_CATALOG);
                        }
                        byte[] fileByte = null;
                        if (configFromString)
                        {
                            fileByte = searchConfigPathV2.getBytes();
                        }
                        else
                        {
                            fileByte = FileUtils.readFileToByteArray(new File(searchConfigPathV2));
                        }

                        StreamSearchConfigsV2 configs = _mapper.readValue(fileByte, 0, fileByte.length, StreamSearchConfigsV2.class);
                        String[] serviceUrl = (configs.hasConfigAccess(searchDomain, searchDomainVersion) ? configs.getServiceURL() : null);
                        toRet = new StreamSearchConfig(serviceUrl, partnerCollectionName.getValue(), configs.getSourceCoreName(searchDomain, searchDomainVersion), configs.getNumShard(), configs.getReplicas());
                        searchConfigCache.put(baseKey, cacheKey, toRet, false);
                    }
                }
            }
            catch (Exception e)
            {
                logger.writeLog(Level.ERROR, "StreamSearchService", "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
                if (session != null)
                {
                    session.rollbackSession(e);
                }
                if (e instanceof ServerException)
                {
                    throw (ServerException) e;
                }
                else
                {
                    throw new ServerException(e);
                }
            }
            finally
            {
                if (session != null)
                {
                    session.endSession();
                }
            }
        }
        logger.writeLog(Level.DEBUG, "StreamSearchService", "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : StreamSearchConfig - {} ", null, false, null, this.getClass().getSimpleName(), CastUtil.toJSON(toRet));
        return toRet;
    }

    public static String getCampaignPropertyName(int searchDomainType)
    {
        String toRet = null;
        switch (searchDomainType)
        {
        case StreamSearchService.SEARCH_DOMAIN_BASIC_SEARCH:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_BASIC_SEARCH;
            break;
        case StreamSearchService.SEARCH_DOMAIN_EVENT:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_EVENT;
            break;
        case StreamSearchService.SEARCH_DOMAIN_PRODUCT:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_PRODUCT_CATALOG;
            break;
        case StreamSearchService.SEARCH_DOMAIN_ANALYTICS:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_ANALYTICS;
            break;
        case StreamSearchService.SEARCH_DOMAIN_LIST:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_LIST;
            break;
        case StreamSearchService.SEARCH_DOMAIN_LISTITEM:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_LIST_ITEM;
            break;
        case StreamSearchService.SEARCH_DOMAIN_BANK:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_BANK;
            break;
        case StreamSearchService.SEARCH_DOMAIN_PINCODE:
            toRet = CampaignProperties.PROP_SEARCH_DOMAIN_PINCODE;
            break;
        default:
            break;
        }
        return toRet;
    }

    public static String getPartnerPropertyName(int searchDomainType)
    {
        String toRet = null;
        switch (searchDomainType)
        {
        case StreamSearchService.SEARCH_DOMAIN_BASIC_SEARCH:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_BASIC_SEARCH;
            break;
        case StreamSearchService.SEARCH_DOMAIN_EVENT:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_EVENT;
            break;
        case StreamSearchService.SEARCH_DOMAIN_PRODUCT:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_PRODUCT_CATALOG;
            break;
        case StreamSearchService.SEARCH_DOMAIN_ANALYTICS:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_ANALYTICS;
            break;
        case StreamSearchService.SEARCH_DOMAIN_LIST:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_LIST;
            break;
        case StreamSearchService.SEARCH_DOMAIN_LISTITEM:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_LIST_ITEM;
            break;
        case StreamSearchService.SEARCH_DOMAIN_BANK:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_BANK;
            break;
        case StreamSearchService.SEARCH_DOMAIN_PINCODE:
            toRet = PartnerProperties.PROP_SEARCH_DOMAIN_COLL_NAME_PINCODE;
            break;
        default:
            break;
        }
        return toRet;
    }

    public StreamSearchResponse doSearch(String serverName,
                                         Campaign campaign,
                                         int searchDomainType,
                                         String searchDomainVersion,
                                         StreamSearchQuery query,
                                         String image,
                                         String[] imageField,
                                         String sort,
                                         String group,
                                         String[] facet,
                                         int offset,
                                         int limit,
                                         StreamSearchConfig config,
                                         boolean isSpellCheck,
                                         String searchTerm,
                                         String contextualParam,
                                         String origContextualParam,
                                         boolean disableFacetMinCount,
                                         boolean disableFacetLimit,
                                         boolean isMlt) throws ServerException
    {
        StreamSearchResponse toRet = null;
        StreamSearchResponse resWithOutCond = null;
        String methodName = "doSearch";

        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  serverName - {}, campaign - {}, searchDomainType - {}, searchDomainVersion - {}, query - {}, image - {}, imageField - {}, sort - {}, group - {}, facet - {}, offset - {}, limit - {}, config - {}, isSpellCheck - {}, searchTerm - {}, contextualParam - {}, origContextualParam - {}, disableFacetMinCount - {}, disableFacetLimit - {}", null, false, null, this.getClass().getSimpleName(), serverName, campaign, searchDomainType, searchDomainVersion, query, image, imageField, sort, group, facet, offset, limit, config, isSpellCheck, searchTerm, contextualParam, origContextualParam, disableFacetMinCount, disableFacetLimit);
        if (isSearchAllowed(campaign, searchDomainType, searchDomainVersion))
        {
            try
            {
                if (config == null)
                {
                    config = getPartnerConfig(serverName, campaign, searchDomainType, searchDomainVersion);
                }

                if (config != null)
                {
                    HashMap<String, ArrayList<StreamSearchResponseFacetValue>> facets = null;

                    if (disableFacetMinCount)
                    {
                        //For zero facet validation
                        ArrayList<StreamSearchQueryCondition> oldFacetCondition = query.getConditions();
                        ArrayList<StreamSearchQueryCondition> newFacetCondition = removeFilter(oldFacetCondition);
                        query.setConditions(newFacetCondition);
                        SkavaHttpRequest skRequestWithOutCond = new SkavaHttpRequest(searchService.generateSearchURL(config, query, image, imageField, offset, limit, null, facet, sort, campaign, searchTerm, isSpellCheck, disableFacetLimit), searchService.generateSearchParams(config, query, image), searchService.generateSearchHeaders(config, query, image), "application/x-www-form-urlencoded", searchService.getSearchContent(config, query, sort, imageField, image, searchDomainType, campaign.getBooleanProperty(CampaignProperties.PROP_SEARCH_ENABLE_SK_PARSER, false), campaign, offset, limit, searchTerm, isSpellCheck, contextualParam), "POST", searchService.getSearchRemoteHost(config, query, image));
                        SkavaHttpResponse skResponseWithOutCond = httpClientService.makeRequest(skRequestWithOutCond, -1, true);
                        resWithOutCond = searchService.parseSearchResult(config, skResponseWithOutCond, group);
                        facets = resWithOutCond.getFacets();
                        query.setConditions(oldFacetCondition);
                    }

                    SkavaHttpRequest skRequest = new SkavaHttpRequest(isMlt ? generateSearchURL(config, query, offset, limit, campaign) : searchService.generateSearchURL(config, query, image, imageField, offset, limit, null, facet, sort, campaign, searchTerm, isSpellCheck, disableFacetLimit), searchService.generateSearchParams(config, query, image), searchService.generateSearchHeaders(config, query, image), "application/x-www-form-urlencoded", searchService.getSearchContent(config, query, sort, imageField, image, searchDomainType, campaign.getBooleanProperty(CampaignProperties.PROP_SEARCH_ENABLE_SK_PARSER, false), campaign, offset, limit, searchTerm, isSpellCheck, contextualParam), "POST", searchService.getSearchRemoteHost(config, query, image));
                    SkavaHttpResponse skResponse = httpClientService.makeRequest(skRequest, -1, true);
                    toRet = parseSearchResult(config, skResponse, group);
                    boolean removegroup = false;
                    if (group != null)
                    {

                        boolean addSkuFacets = campaign.getBooleanProperty(CampaignProperties.PROP_SEARCH_ADD_SKU_GROUP_FACETS, false);
                        if (addSkuFacets)
                        {
                            removegroup = true;
                            SkavaHttpRequest skRequestWithoutGtoup = new SkavaHttpRequest(searchService.generateSearchURL(config, query, image, imageField, offset, limit, null, facet, sort, campaign, searchTerm, isSpellCheck, disableFacetLimit), searchService.generateSearchParams(config, query, image), searchService.generateSearchHeaders(config, query, image), "application/x-www-form-urlencoded", searchService.getSearchContent(config, query, sort, imageField, image, searchDomainType, campaign.getBooleanProperty(CampaignProperties.PROP_SEARCH_ENABLE_SK_PARSER, false), campaign, offset, limit, searchTerm, isSpellCheck, contextualParam), "POST", searchService.getSearchRemoteHost(config, query, image));
                            SkavaHttpResponse skResponseWithoutGtoup = httpClientService.makeRequest(skRequestWithoutGtoup, -1, true);
                            StreamSearchResponse withoutGtoupresult = searchService.parseSearchResult(config, skResponseWithoutGtoup, group);
                            if (withoutGtoupresult != null && withoutGtoupresult.getFacets() != null && withoutGtoupresult.getFacets().size() > 0)
                            {
                                toRet.setFacets(withoutGtoupresult.getFacets());

                            }
                        }
                    }
                    if (toRet != null && toRet.getNumEntries() > 0 && campaign.getBooleanProperty(CampaignProperties.PROP_ENABLE_MULTIFACETS, false) && query.getSelectedFacetNames() != null && query.getSelectedFacetNames().size() > 0)
                    {
                        Iterator<Map.Entry<String, List<String>>> facetsIterator = query.getSelectedFacetNames().entrySet().iterator();
                        ArrayList<Future<StreamSearchResponse>> futureFacetResponseList = new ArrayList<Future<StreamSearchResponse>>();
                        while (facetsIterator.hasNext())
                        {
                            Entry<String, List<String>> entry = facetsIterator.next();
                            String facetKey = entry.getKey();
                            Iterator<Map.Entry<String, List<String>>> internalFacetsIterator = query.getSelectedFacetNames().entrySet().iterator();

                            StreamSearchQuery newQueries = null;
                            if (contextualParam == null)
                            {
                                newQueries = getConditionsExceptFacets(query);
                            }

                            while (internalFacetsIterator.hasNext())
                            {
                                Entry<String, List<String>> internalEntry = internalFacetsIterator.next();
                                String internalFacetKey = internalEntry.getKey();
                                List<String> internalFacetValues = internalEntry.getValue();
                                if (!internalFacetKey.equals(facetKey))
                                {
                                    ArrayList<StreamSearchQueryCondition> facetCond = new ArrayList<StreamSearchQueryCondition>();
                                    for (int i = 0; i < internalFacetValues.size(); i++)
                                    {
                                        facetCond.add(new StreamSearchQueryCondition(internalFacetKey, internalFacetValues.get(i), null, null, false, false, null));
                                    }
                                    if (newQueries == null)
                                    {
                                        newQueries = new StreamSearchQuery();
                                    }
                                    newQueries.addCondition(new StreamSearchQueryCondition(null, null, null, null, true, false, facetCond));
                                }
                            }

                            if (origContextualParam != null)
                            {
                                if (newQueries != null)
                                {
                                    String facetStr = getQueryString(newQueries);
                                    if (newQueries.getConditions() != null && !newQueries.getConditions().isEmpty())
                                    {
                                        facetStr = facetStr.substring(1, facetStr.length() - 1); //to remove outer bracket as it makes query (+campaignid:2295 +available:true +brand:"nike" +categorylevel2:"footwear" (+facet_gender_2295:"men" +facet_colors_2295:"multicolor" +facet_toe_shape_2295:"regular" +facet_brand_2295:"Nike")) rather than (+campaignid:2295 +available:true +brand:"nike" +categorylevel2:"footwear" +facet_gender_2295:"men" +facet_colors_2295:"multicolor" +facet_toe_shape_2295:"regular" +facet_brand_2295:"Nike") 
                                    }
                                    contextualParam = origContextualParam.substring(0, origContextualParam.length() - 1) + " " + facetStr + ")";
                                }
                            }
                            if ((newQueries != null && newQueries.getConditions() != null) || (contextualParam != null && contextualParam.length() > 0))
                            {
                                if ((newQueries == null || newQueries.getConditions() == null))
                                {
                                    contextualParam = origContextualParam;
                                }
                                AsynchronousFacetTask asyncFacetRequest = new AsynchronousFacetTask(httpClientService, searchService, config, campaign, newQueries, sort, imageField, image, searchDomainType, facetKey, removegroup ? null : group, searchTerm, contextualParam, disableFacetLimit);
                                futureFacetResponseList.add(ecs.submit(asyncFacetRequest));
                            }
                        }
                        for (int i = 0; i < futureFacetResponseList.size(); i++)
                        {
                            StreamSearchResponse searchResponse = ecs.take().get(KEEP_ALIVE_MSECS, TimeUnit.MILLISECONDS);
                            appendResponse(toRet, searchResponse, searchResponse.getSelectedFacetKey());
                        }
                    }
                    updateFacetInfo(toRet, facets);
                }
                else
                {
                    throw new ServerException(ServerException.ERR_ILLEGAL_DATA, "Search Domain Config is not set for type: " + searchDomainType + ", version : " + searchDomainVersion + ", campaignId : " + campaign.getId());
                }
            }
            catch (Exception e)
            {
                logger.writeLog(Level.ERROR, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
                throw new ServerException(Response.RESPONSE_FAILED, e);
            }
        }
        else
        {
            throw new ServerException(ServerException.ERR_ACCESS_DENIED, "Search is not allowed for this campaign");
        }
        logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} : StreamSearchResponse - {} ", null, false, null, this.getClass().getSimpleName(), CastUtil.toJSON(toRet));
        return toRet;
    }

    private String generateSearchURL(StreamSearchConfig config,
                                     StreamSearchQuery query,
                                     int offset,
                                     int limit,
                                     Campaign campaign)
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
        return "http://" + searchUrl + "/mlt?" + (shardsURL != null ? "&shards=" + EncodeUtil.urlDecoder(shardsURL) : "") + "&wt=json&start=" + offset + "&rows=" + limit;
    }

    private void updateFacetInfo(StreamSearchResponse toRet,
                                 HashMap<String, ArrayList<StreamSearchResponseFacetValue>> facetsWithOutFilter)
    {
        if (facetsWithOutFilter != null && toRet != null && toRet.getFacets() != null)
        {
            HashMap<String, ArrayList<StreamSearchResponseFacetValue>> newFacets = toRet.getFacets();
            boolean isFacetValueAvailable = false;
            for (String key : facetsWithOutFilter.keySet())
            {
                if (!newFacets.containsKey(key))
                {
                    ArrayList<StreamSearchResponseFacetValue> facet = facetsWithOutFilter.get(key);
                    if (facet != null)
                    {
                        ArrayList<StreamSearchResponseFacetValue> zeroFacetList = new ArrayList<>();
                        for (StreamSearchResponseFacetValue streamSearchResponseFacetValue : facet)
                        {
                            StreamSearchResponseFacetValue newFacetValue = new StreamSearchResponseFacetValue(streamSearchResponseFacetValue.getName(), 0);
                            zeroFacetList.add(newFacetValue);
                        }
                        newFacets.put(key, zeroFacetList);
                    }
                }
                else
                {
                    ArrayList<StreamSearchResponseFacetValue> oldFacet = facetsWithOutFilter.get(key);
                    ArrayList<StreamSearchResponseFacetValue> newFacet = newFacets.get(key);
                    ArrayList<StreamSearchResponseFacetValue> facetList = new ArrayList<>();
                    for (StreamSearchResponseFacetValue streamSearchResponseFacetValue : oldFacet)
                    {
                        isFacetValueAvailable = false;
                        for (StreamSearchResponseFacetValue newstreamSearchResponseFacetValue : newFacet)
                        {
                            if (streamSearchResponseFacetValue.getName().equals(newstreamSearchResponseFacetValue.getName()))
                            {
                                facetList.add(newstreamSearchResponseFacetValue);
                                isFacetValueAvailable = true;
                                break;
                            }
                        }
                        if (!isFacetValueAvailable)
                        {
                            facetList.add((new StreamSearchResponseFacetValue(streamSearchResponseFacetValue.getName(), 0)));
                        }
                    }
                    newFacets.put(key, facetList);
                }
            }
            toRet.setFacets(newFacets);
        }
    }

    private ArrayList<StreamSearchQueryCondition> removeFilter(ArrayList<StreamSearchQueryCondition> oldFacetCondition)
    {
        ArrayList<StreamSearchQueryCondition> newFacetCondition = null;
        boolean isFilterAvailable = false;
        if (oldFacetCondition != null)
        {
            newFacetCondition = new ArrayList<>();
            for (StreamSearchQueryCondition queryCond : oldFacetCondition)
            {
                if (queryCond != null)
                {
                    ArrayList<StreamSearchQueryCondition> conditionList = queryCond.getConditions();
                    isFilterAvailable = false;
                    if (conditionList != null)
                    {
                        for (StreamSearchQueryCondition condition : conditionList)
                        {
                            if (condition != null)
                            {
                                String filedName = ReadUtil.getString(condition.getFieldName(), null);
                                if (filedName != null && filedName.startsWith(StreamSearchV2ServiceImpl.FACET_FILTER_PREFIX))
                                {
                                    isFilterAvailable = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!isFilterAvailable)
                    {
                        newFacetCondition.add(queryCond);
                    }
                }
            }
        }
        return newFacetCondition;
    }

    private void appendResponse(StreamSearchResponse dest,
                                StreamSearchResponse src,
                                String facetKey)
    {
        if (dest != null && dest.getFacets() != null && src != null && src.getFacets() != null && src.getFacets().containsKey(facetKey))
        {
            ArrayList<StreamSearchResponseFacetValue> facetResults = src.getFacets().get(facetKey);
            dest.getFacets().put(facetKey, facetResults);
        }
    }

    private StreamSearchQuery getConditionsExceptFacets(StreamSearchQuery query)
    {
        StreamSearchQuery newQueries = getAllExceptConditions(query);
        if (query.getConditions() != null)
        {
            getConditionsExceptFacets(query.getConditions(), newQueries);
        }
        return newQueries;
    }

    public StreamSearchQuery getAllExceptConditions(StreamSearchQuery query)
    {
        StreamSearchQuery newQuery = new StreamSearchQuery();
        newQuery.setAdditionalParams(query.getAdditionalParams());
        newQuery.setSelectedFacetNames(query.getSelectedFacetNames());
        newQuery.setQf(query.getQf());
        newQuery.setBq(query.getBq());
        newQuery.setEdismax(query.getEdismax());
        return newQuery;
    }

    private void getConditionsExceptFacets(ArrayList<StreamSearchQueryCondition> query,
                                           StreamSearchQuery newQuery)
    {
        if (query != null && query.size() > 0)
        {
            Iterator<StreamSearchQueryCondition> queryConditionIterator = query.iterator();
            while (queryConditionIterator.hasNext())
            {
                StreamSearchQueryCondition queryCondition = queryConditionIterator.next();
                if (queryCondition.getFieldName() != null && !queryCondition.getFieldName().contains("facet_"))
                {
                    newQuery.addCondition(queryCondition);
                }
                else if (queryCondition.getConditions() != null)
                {
                    if (isContainsNestedFacets(queryCondition.getConditions()))
                    {
                        newQuery.addCondition(queryCondition);
                    }
                }
            }
        }
    }

    private boolean isContainsNestedFacets(ArrayList<StreamSearchQueryCondition> queryConditions)
    {
        // TODO Auto-generated method stub
        for (int i = 0; i < queryConditions.size(); i++)
        {
            StreamSearchQueryCondition query = queryConditions.get(i);
            if (query.getFieldName() != null && query.getFieldName().contains("facet_"))
            {
                return false;
            }
            else if (query.getConditions() != null) { return isContainsNestedFacets(query.getConditions()); }
        }
        return true;
    }

    public String getQueryString(StreamSearchQuery query) throws ServerException
    {
        return searchService.getQueryString(query);
    }

    private boolean isSearchAllowed(Campaign campaign,
                                    int searchDomainType,
                                    String searchDomainVersion)
    {
        boolean doAllowSearch = false;
        String searchDomain = null;
        String domainPropName = getCampaignPropertyName(searchDomainType);
        if (campaign != null && domainPropName != null)
        {
            searchDomain = campaign.getProperty(domainPropName);
            if (searchDomain == null)
            {
                switch (domainPropName)
                {
                case CampaignProperties.PROP_SEARCH_DOMAIN_BASIC_SEARCH:
                    searchDomain = SEARCH_DOMAIN_BASIC_SEARCH_CATALOG;
                    break;
                case CampaignProperties.PROP_SEARCH_DOMAIN_EVENT:
                    searchDomain = SEARCH_DOMAIN_EVENT_CATALOG;
                    break;
                case CampaignProperties.PROP_SEARCH_DOMAIN_PRODUCT_CATALOG:
                    searchDomain = SEARCH_DOMAIN_PRODUCT_CATALOG;
                    break;
                case CampaignProperties.PROP_SEARCH_DOMAIN_LIST:
                    searchDomain = SEARCH_DOMAIN_LIST_CATALOG;
                    break;
                case CampaignProperties.PROP_SEARCH_DOMAIN_LIST_ITEM:
                    searchDomain = SEARCH_DOMAIN_LIST_ITEM_CATALOG;
                    break;
                default:
                    break;
                }
            }
        }

        if (searchDomain != null)
        {
            boolean allowSearch = campaign.getBooleanProperty(CampaignProperties.PROP_ALLOW_A9_SEARCH, true);
            String allowedDomains = campaign.getProperty(CampaignProperties.PROP_ALLOW_A9_SEARCH_ON_DOMAINS);
            if (allowSearch && allowedDomains == null)
            {
                allowedDomains = searchDomain + SEARCH_VERSION;
            }
            doAllowSearch = allowSearch && allowedDomains.contains(searchDomain + searchDomainVersion);

            if (!doAllowSearch)
            {
                logger.info("@isSearchAllowed, allowSearch:" + allowSearch + ", allowedDomains:" + allowedDomains + ", searchDomain:" + (searchDomain + searchDomainVersion));
            }
        }
        else
        {
            logger.info("@isSearchAllowed, allowSearch:" + searchDomain + ", campaign: " + (campaign != null ? campaign.toString() : campaign) + ", domainPropName:" + domainPropName);
        }
        return doAllowSearch;
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
            SolrSearchKraftResponse response = (SolrSearchKraftResponse) CastUtil.fromJSON(new String(responseData), SolrSearchKraftResponse.class);

            int numEntries = 0;
            LinkedHashMap<String, ArrayList<StreamSearchGroupResult>> groups = null;

            if (response.getMoreLikeThis() != null)
            {
                Iterator<Map.Entry<String, SolrSearchResponseModel>> itr = response.getMoreLikeThis().entrySet().iterator();
                if (itr.hasNext())
                {
                    Entry<String, SolrSearchResponseModel> expandKey = itr.next();
                    response.setResponse(expandKey.getValue());
                }
            }
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

    @Override
    public Response doSuggestDict(String serverName,
                                  Campaign campaign,
                                  int searchDomainType,
                                  String searchDomainVersion,
                                  String searchTerm,
                                  String responseFormatterClass,
                                  int offset,
                                  int limit,
                                  String region,
                                  String catalogId,
                                  long storeId)
    {
        Response toRet = null;
        String methodName = "doSuggestDict";
        try
        {
            if (isSearchAllowed(campaign, searchDomainType, searchDomainVersion))
            {
                try
                {
                    StreamSearchConfig config = null;
                    String campaignConfig = ReadUtil.getString(campaign.getProperty(CampaignProperties.PROP_STREAM_SEARCH_CONFIG), null);
                    if (campaignConfig != null)
                    {
                        config = (StreamSearchConfig) CastUtil.fromJSON(campaignConfig, StreamSearchConfig.class);
                    }
                    else
                    {
                        config = getPartnerConfig(serverName, campaign, searchDomainType, searchDomainVersion);
                    }
                    if (config != null)
                    {
                        SkavaHttpRequest skRequestsuggest = new SkavaHttpRequest(generateAdditionalSuggestDicturl(config, campaign.getId(), EncodeUtil.urlEncode(searchTerm, false), offset, limit, null, null, 0), null, null, null, searchService.getSearchContent(config, null, null, null, null, searchDomainType, campaign.getBooleanProperty(CampaignProperties.PROP_SEARCH_ENABLE_SK_PARSER, false), campaign, 0, 0, null, false, null), searchService.getSearchMethod(config, null, null), searchService.getSearchRemoteHost(config, null, null));
                        SkavaHttpResponse skResponsesuggest = httpClientService.makeRequest(skRequestsuggest, -1, true);
                        List<String> resultList = searchService.parseSuggestionDictResult(skResponsesuggest, searchTerm, offset, limit, true);
                        if (resultList != null && resultList.size() > 0)
                        {
                            if (resultList.size() > limit)
                            {
                                resultList = resultList.subList(0, limit);
                            }
                            if ((responseFormatterClass != null) && (responseFormatterClass.length() > 0))
                            {
                                SuggestResponseFormatter suggestResponseFormatter = (SuggestResponseFormatter) Class.forName(responseFormatterClass).newInstance();
                                String result = (String) suggestResponseFormatter.format(resultList, limit);
                                toRet = new Response(result.getBytes("UTF-8"));
                            }
                        }
                    }
                    else
                    {
                        throw new ServerException(ServerException.ERR_ILLEGAL_DATA, "Search Domain Config is not set for type: " + searchDomainType + ", version : " + searchDomainVersion + ", campaignId : " + campaign.getId());
                    }
                }
                catch (Exception e)
                {
                    logger.writeLog(Level.ERROR, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
                    throw new ServerException(Response.RESPONSE_FAILED, e);
                }
            }
            else
            {
                throw new ServerException(ServerException.ERR_ACCESS_DENIED, "Search is not allowed for this campaign");
            }
        }
        catch (Exception e)
        {
            logger.writeLog(Level.ERROR, SearchConstants.EVENTID_SUGGESTDICT, "", this.getClass().getSimpleName(), null, 0, e.getMessage(), methodName, "Unknown Exception occurred while processing workitem @{}", null, true, e, methodName);
            toRet = new Response();
            toRet.setResponseCode(Response.RESPONSE_FAILED);
            toRet.setResponseMessage(e.getMessage());
        }
        return toRet;
    }

    private String generateAdditionalSuggestDicturl(StreamSearchConfig config,
                                                    long campaignId,
                                                    String searchterm,
                                                    int offset,
                                                    int limit,
                                                    String region,
                                                    String catalogId,
                                                    long storeId) throws ServerException
    {
        String toRet = null;
        String methodName = "generateAdditionalSuggestDicturl";
        String[] searchUrls = config.getServiceUrlswithCollectionName();
        if (searchUrls.length > 0 && searchUrls[0] != null)
        {
            StringBuffer suggestUrl = new StringBuffer();
            suggestUrl.append("http://");
            suggestUrl.append(searchUrls[0]);
            suggestUrl.append("/suggestdict");
            suggestUrl.append(campaignId);
            if (catalogId != null)
            {
                suggestUrl.append("_");
                suggestUrl.append(catalogId);
            }
            if (region != null)
            {
                suggestUrl.append("_");
                suggestUrl.append(region);
            }

            if (storeId > 0)
            {
                suggestUrl.append("_");
                suggestUrl.append(storeId);
            }
            suggestUrl.append("?suggest.q=");
            suggestUrl.append(URLUtil.browserEncodeEscapingUnsafeCharacters(searchterm));
            suggestUrl.append("&suggest=true");
            suggestUrl.append("&suggest.dictionary=keywordSuggester&suggest.dictionary=nameSuggester");
            suggestUrl.append("&wt=json");
            suggestUrl.append("&suggest.count=");
            suggestUrl.append((limit > 0 ? limit : ""));
            toRet = suggestUrl.toString();
        }
        return toRet;
    }
}

class AsynchronousFacetTask implements Callable<StreamSearchResponse>
{
    private HttpClientService httpClientService;
    private SearchService searchService;
    private StreamSearchConfig config;
    private Campaign campaign;
    private StreamSearchQuery query;
    private String sort;
    private String[] imageField;
    private String image;
    private int searchDomainType;
    private String facetKey;
    private String group;
    private String searchTerm;
    private String contextualParam;
    private boolean isVisible;
    private boolean disableFacetLimit;

    public AsynchronousFacetTask(HttpClientService httpClientService,
                                 SearchService searchService,
                                 StreamSearchConfig config, Campaign campaign,
                                 StreamSearchQuery query, String sort,
                                 String[] imageField, String image,
                                 int searchDomainType, String facetKey,
                                 String group, String searchTerm,
                                 String contextualParam,
                                 boolean disableFacetLimit)
    {
        super();
        this.httpClientService = httpClientService;
        this.searchService = searchService;
        this.config = config;
        this.campaign = campaign;
        this.query = query;
        this.sort = sort;
        this.imageField = imageField;
        this.image = image;
        this.searchDomainType = searchDomainType;
        this.facetKey = facetKey;
        this.group = group;
        this.searchTerm = searchTerm;
        this.contextualParam = contextualParam;
        this.disableFacetLimit = disableFacetLimit;
    }

    public StreamSearchResponse call() throws Exception
    {
        byte[] contentInternal = searchService.getSearchContent(config, query, sort, imageField, image, searchDomainType, campaign.getBooleanProperty(CampaignProperties.PROP_SEARCH_ENABLE_SK_PARSER, false), campaign, 0, 0, searchTerm, false, contextualParam);
        String searchURLInternal = searchService.generateSearchURL(config, query, image, imageField, 0, 0, group, new String[] { facetKey }, sort, campaign, null, false, disableFacetLimit);
        HashMap<String, List<String>> paramsInternal = searchService.generateSearchParams(config, query, image);
        HashMap<String, List<String>> headersInternal = searchService.generateSearchHeaders(config, query, image);
        String remoteHostInternal = searchService.getSearchRemoteHost(config, query, image);
        SkavaHttpRequest skRequestInternal = new SkavaHttpRequest(searchURLInternal, paramsInternal, headersInternal, "application/x-www-form-urlencoded", contentInternal, "POST", remoteHostInternal);
        SkavaHttpResponse skResponseInternal = httpClientService.makeRequest(skRequestInternal, -1, true);
        StreamSearchResponse facetResult = searchService.parseSearchResult(config, skResponseInternal, group);
        facetResult.setSelectedFacetKey(this.facetKey);
        return facetResult;
    }
}
