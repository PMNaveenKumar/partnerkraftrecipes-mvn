package com.skava.pim.jbpm.workitems;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Level;
import org.drools.core.process.instance.WorkItemHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;

import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.pim.CatalogMaster;
import com.skava.model.pim.CategoryMaster;
import com.skava.model.pim.MultiFacets;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.PIMItemProperty;
import com.skava.model.pim.PimConstants;
import com.skava.model.pim.PimKraftConstants;
import com.skava.model.pim.PimResponse;
import com.skava.model.pim.PimVersion;
import com.skava.model.pim.SelectedFacet;
import com.skava.model.pim.SelectedKraftFacet;
import com.skava.model.pim.Sort;
import com.skava.pim.helper.PIMUtil;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.util.CastUtil;
import com.skava.util.JSONUtils;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.StringUtil;

/**
 * <h1> GetSearchResults </h1>
 * <p>This class is used to we can directly get the products by CategoryId, ProductId and SkuId.</p>
 * @author: <u>Skava Platform Team</u>
 * @version 7.5
 * @since 6.0
 * @jbpm GetSearchResults
 */
public class GetSearchResultsForKraft implements WorkItemHandler
{

    private String SERVICE_NAME;

    private final String GETSEARCHRESULTS = "GETSEARCHRESULTS";
    public static final int SEARCH_RESPONSE_CODE = 204;
    
    public static final String DEFAULT_SORT_PROP = "prop.pimdefaultsort";

    /**
     * <p>This method execute the load products by CategoryId, ProductId and SkuId.</p>
     * 
     * @param workItem {@link org.kie.api.runtime.process.WorkItem} It contains the params which are used by the current work item.
     * @param manager {@link org.kie.api.runtime.process.WorkItemManager} It sets response of <code>GetSearchResults</code> work item.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager)
    {
        Map<String, Object> responseMap = new HashMap<>();

        try
        {

            PIMUtil.writeLog(Level.DEBUG, SERVICE_NAME, GETSEARCHRESULTS, null, null, PimConstants.DESC_START_WORKITEM, workItem.getParameters(), PimConstants.DESC_START_WORKITEM, GETSEARCHRESULTS, workItem.getParameters());

            HashMap<String, Object> params = (HashMap<String, Object>) workItem.getParameters();

            SERVICE_NAME = ReadUtil.getString(params.get(PimConstants.SERVICE_NAME), null);

            CatalogMaster catalogMaster = (CatalogMaster) params.get(PimConstants.PARAM_CATALOG_MASTER);
            CategoryMaster categoryMaster = (CategoryMaster) params.get(PimConstants.PARAM_CATEGORY_MASTER);
            HttpServletRequest request = (HttpServletRequest) params.get(PimConstants.PARAM_HTTPSERVLET_REQUEST);
            StreamSearchV2KraftService streamSearchV2Service = (StreamSearchV2KraftService) params.get(PimConstants.PARAM_SEARCHV2_SERVICE);
            PimVersion currentVersion = (PimVersion) params.get(PimConstants.PARAM_CURRENT_VERSION);
            String[] productIds = (String[]) params.get(PimConstants.PARAM_PRODUCT_IDS);
            String[] skuId = (String[]) params.get(PimConstants.PARAM_SKU_IDS);
            String name = (String) params.get(PimConstants.PARAM_NAME);
            String[] brand = (String[]) params.get(PimConstants.PARAM_BRAND);
            String[] category = (String[]) params.get(PimConstants.PARAM_CATEGORY);
            String[] categoryids = (String[]) params.get(PimConstants.PARAM_CATEGORY_IDS);
            String categoryId = (String) params.get(PimConstants.PARAM_CATEGORY_ID);
            if (categoryMaster != null && (categoryMaster.getType() != CategoryMaster.CATEGORY_TYPE_DYNAMIC))
            {
                if (categoryids == null && categoryId != null)
                {
                    categoryids = new String[] { categoryId };
                }
            }
            String categorylevel1 = (String) params.get(PimConstants.PARAM_CATEGORY_LEVEL1);
            String categorylevel2 = (String) params.get(PimConstants.PARAM_CATEGORY_LEVEL2);
            String categorylevel3 = (String) params.get(PimConstants.PARAM_CATEGORY_LEVEL3);
            String categorylevel4 = (String) params.get(PimConstants.PARAM_CATEGORY_LEVEL4);
            String categorylevel5 = (String) params.get(PimConstants.PARAM_CATEGORY_LEVEL5);
            String[] division = (String[]) params.get(PimConstants.PARAM_DIVISION);
            String[] color = (String[]) params.get(PimConstants.PARAM_COLOR);
            String size1 = (String) params.get(PimConstants.PARAM_SIZE1);
            String size2 = (String) params.get(PimConstants.PARAM_SIZE2);
            float[] priceMin = (float[]) params.get(PimConstants.PARAM_PRICE_MIN);
            float[] priceMax = (float[]) params.get(PimConstants.PARAM_PRICE_MAX);
            String[] facets = (String[]) params.get(PimConstants.PARAM_FACETS);
            String variant = (String) params.get(PimConstants.PARAM_VARIANT);
            String searchTerm = (String) params.get(PimConstants.PARAM_SEARCH_TERM);
            MultiFacets selectedFacets = (MultiFacets) params.get(PimConstants.PARAM_SELECTED_FACETS);
            MultiFacetsKraft userPreferences = (MultiFacetsKraft) params.get(PimKraftConstants.PARAM_USER_PREFERENCES);
            String sort = (String) params.get(PimConstants.PARAM_SORT);
            String group = (String) params.get(PimConstants.PARAM_GROUP);
            String responseFormatterClass = (String) params.get(PimConstants.PARAM_RESPONSEFORMATTER_CLASS);
            int offset = ReadUtil.getInt(params.get(PimConstants.PARAM_OFFSET), 0);
            int limit = ReadUtil.getInt(params.get(PimConstants.PARAM_LIMIT), 10);
            boolean usev2 = ReadUtil.getBoolean(params.get(PimConstants.PARAM_USEV2), false);
            boolean edismax = ReadUtil.getBoolean(params.get(PimConstants.PARAM_EDISMAX), false);
            boolean iszeroResult = ReadUtil.getBoolean(params.get(PimConstants.PARAM_ISZERORESULT), false);
            boolean spellcheck = ReadUtil.getBoolean(params.get(PimConstants.PARAM_SPELLCHECK), false);
            boolean personalize = ReadUtil.getBoolean(params.get(PimConstants.PARAM_PERSONALIZE), false);
            String contextualParam = (String) params.get(PimConstants.PARAM_CONTEXTUALPARAM);
            MultiFacets curateTerms = (MultiFacets) params.get(PimConstants.PARAM_CURATETERMS);
            Campaign campaign = (Campaign) params.get(PimConstants.PARAM_CAMPAIGN);
            String locale = (String) params.get(PimConstants.PARAM_LOCALE);
            Set<Sort> selectedSort = (LinkedHashSet<Sort>) params.get(PimConstants.PARAM_SELECTED_SORT);
            boolean disableFacetMinCount = ReadUtil.getBoolean(params.get(PimConstants.PARAM_DISABLE_FACET_MIN_COUNT), false);
            boolean includeGhostProduct = ReadUtil.getBoolean(params.get(PimConstants.PARAM_INCLUDE_GHOST_PRODUCT), false);
            String[] segments = (String[]) params.get(PimConstants.PARAM_SEGMENTS);
            long storeId = (long) params.get(PimConstants.PARAM_STORE_ID);
            boolean disableDefaultSort = (boolean) params.get(PimKraftConstants.PARAM_DISABLE_DEFAULT_SORT);
            
            String similarType = (String) params.get(PimKraftConstants.PARAM_SIMILAR_TYPE);
            boolean spellCheckOnly = ReadUtil.getBoolean(params.get(PimKraftConstants.PARAM_SPELLCHECK_ONLY), false);

            List<String> sortList = new ArrayList<>();
            if (selectedSort != null && !selectedSort.isEmpty())
            {
                for (Sort sortObject : selectedSort)
                {
                    if (sortObject.getValue() != null)
                    {
                        sortList.add(sortObject.getValue());
                    }
                }
                sort = StringUtil.mergeStrings(sortList.toArray(new String[sortList.size()]), ",", null);
            }
            String defaultSort = ReadUtil.getString(campaign.getProperty(DEFAULT_SORT_PROP), null);
            if(!disableDefaultSort && defaultSort != null && (sort == null || sort.isEmpty() || !sortList.contains(defaultSort)))
            {
                sort = sort == null ? defaultSort : (sort + "," + defaultSort);
            }

            PIMUtil.writeLog(Level.DEBUG, SERVICE_NAME, GETSEARCHRESULTS, null, null, " SkavPimService - {} :request - {},  streamSearchV2Service - {},  campaign - {},  skuId - {},  productIds - {},  name - {},  brand - {},  category - {},  categoryids - {},  categorylevel1 - {10},  categorylevel2 - {11},  categorylevel3 - {12},  categorylevel4 - {13},  categorylevel5 - {14},  division - {15},  color - {16},  size1 - {17},  size2 - {18},  priceMin - {19},  priceMax - {20},  variant - {21},  searchTerm - {22},  selectedFacets - {23},  sort - {24},  group - {25},  facets - {26},  responseFormatterClass - {27},  catalogMaster - {28},  categoryMaster - {29},  offset - {30},  limit - {31},  usev2 - {32},  edismax - {33},  iszeroResult - {34},  spellcheck - {35},  personalize - {36},  locale - {37},  currentVersion - {38},  contextualParam - {39},  curateTerms - {40},  disableFacetMinCount - {41},  includeGhostProduct - {42},  segments - {43}, userPreferences - {44} ", null, GETSEARCHRESULTS, request, streamSearchV2Service, campaign, skuId, productIds, name, brand, category, categoryids, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, color, size1, size2, priceMin, priceMax, variant, searchTerm, selectedFacets, sort, group, facets, responseFormatterClass, catalogMaster, categoryMaster, offset, limit, usev2, edismax, iszeroResult, spellcheck, personalize, locale, currentVersion, contextualParam, curateTerms, disableFacetMinCount, includeGhostProduct, segments, userPreferences, similarType);

            responseMap = getSearchresults(request, streamSearchV2Service, campaign, skuId, productIds, name, brand, category, categoryids, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, color, size1, size2, priceMin, priceMax, variant, searchTerm, selectedFacets, sort, group, facets, responseFormatterClass, catalogMaster, categoryMaster, offset, limit, usev2, edismax, iszeroResult, spellcheck, personalize, locale, currentVersion, contextualParam, curateTerms, disableFacetMinCount, includeGhostProduct, segments, storeId, userPreferences, similarType, spellCheckOnly);

            PIMUtil.writeLog(Level.DEBUG, SERVICE_NAME, GETSEARCHRESULTS, null, null, PimConstants.DESC_RESP_MAP_WORKITEM, null, PimConstants.DESC_RESP_MAP_WORKITEM, GETSEARCHRESULTS, responseMap);

        }
        catch (ServerException se)
        {

            if (!PimConstants.knownError(se.getErrorCode()))
            {
                PIMUtil.writeLog(Level.ERROR, SERVICE_NAME, GETSEARCHRESULTS, se, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_SERVER_EXCEPTION, null, PimConstants.DESC_SERVER_EXCEPTION, GETSEARCHRESULTS);
            }
            responseMap = this.workItemResultBuilder(se.getErrorCode(), null, null, false);
        }
        catch (Exception e)
        {

            PIMUtil.writeLog(Level.ERROR, SERVICE_NAME, GETSEARCHRESULTS, e, PimConstants.ERRORTYPE_INPUT, PimConstants.DESC_EXCEPTION, null, PimConstants.DESC_EXCEPTION, GETSEARCHRESULTS);

            responseMap = this.workItemResultBuilder(ServerException.ERR_UNKNOWN, null, null, false);
        }
        manager.completeWorkItem(workItem.getId(), responseMap);
    }

    /**
     * <p>Using this method we can directly get the products by CategoryId, ProductId and SkuId.</p>
     * 
     * @param request {@link javax.servlet.http.HttpServletRequest} It contains the information of request for HTTP servlets.
     * @param streamSearchV2Service {@link com.skava.services.StreamSearchV2KraftService} This service provides the functionalities of get products from solr.
     * @param campaign {@link com.skava.model.dbbeans.Campaign} In all the microservice the configuration parameters of the APIs are maintained in an entity called Campaign. We can customize the functionality of the API using the campaign properties.
     * @param skuIds {@link java.lang.String} Product are the sku group that are configured in PIMAdmin.We can get product available in the requested skuId.
     * @param productIds {@link java.lang.String} Products that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated product information are responded.
     * @param name {@link java.lang.String} Specifies name containing the product name to fetch.
     * @param brand {@link java.lang.String} Specifies the products containing the brand should be fetched.
     * @param categories {@link java.lang.String} Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded.
     * @param categoryIds {@link java.lang.String} Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded.
     * @param categorylevel1 {@link java.lang.String} It contains category level 1 information.
     * @param categorylevel2 {@link java.lang.String} It contains category level 2 information.
     * @param categorylevel3 {@link java.lang.String} It contains category level 3 information.
     * @param categorylevel4 {@link java.lang.String} It contains category level 4 information.
     * @param categorylevel5 {@link java.lang.String} It contains category level 5 information.
     * @param division Division.
     * @param color Product color.
     * @param size1 Product size1.
     * @param size2 product size2.
     * @param priceMin Minimum price.
     * @param priceMax maximum price.
     * @param variant {@link java.lang.String} Variant.
     * @param searchTerm {@link java.lang.String} It is used to get the products by search with CategoryId or ProductId or SkuId.
     * @param selectedFacets {@link com.skava.model.pim.SelectedKraftFacet} It is used to mention the filter for the products that are available in the requested category. It have a json format. {"selectedFacets":[{"key":"color","value":["blue"]}]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.
     * @param sort {@link com.skava.model.pim.SelectedKraftFacet} It is used to apply sorting in the product response. Based on the sortable facets configured in the PIM Admin, sortable facet value will be available in the response, it needs to be set as value for this parameter to apply the sort.
     * @param group {@link com.skava.model.pim.SelectedKraftFacet} The product group.
     * @param facets {@link com.skava.model.pim.SelectedKraftFacet} It contains facet information.
     * @param responseFormatterClass {@link com.skava.model.pim.SelectedKraftFacet} It contains specific format for response.
     * @param catalogMaster {@link com.skava.model.pim.CatalogMaster} It contains catalog master information.
     * @param categoryMaster {@link com.skava.model.pim.CategoryMaster} It contains category master information.
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100.
     * @param usev2 It contains user information.
     * @param edismax Edismax.
     * @param iszeroResult Boolean to decide zero result.
     * @param spellcheck Boolean to decide spell check.
     * @param personalize Boolean to decide personalize.
     * @param locale {@link java.lang.String} API Response and error messages will be responded in the locale mentioned in this parameter. Locale needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param currentVersion {@link com.skava.model.pim.PimVersion} The current PIM version.
     * @param contextualParam {@link java.lang.String} The contextual param.
     * @param curateTerms {@link com.skava.model.pim.MultiFacetsKraft} The curate terms.
     * @param disableFacetMinCount Boolean parameter to decide facet minimum count. 
     * @param includeGhostProduct It is boolean parameter to mention that mention that whether products that are invisible needs to be honored while considering the category to respond.
     * @param segments {@link java.lang.String} It refers to user segments, by this the products that are associated to the requested segments alone will be considered for the category to respond.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Campaign. We can customize the functionality of the API using the campaign properties. Campaign Id is a mandatory parameter  for all the microservice.
     * @param selectedFacets {@link com.skava.model.pim.SelectedKraftFacet} User facets
     * @return {@link java.util.Map} It returns constructed response map with loaded products.
     * <p>Below are the keys present in the output map</p>
     *  <ul>
     *  <li>CODE_RESP_NO_CURRENT_VERSION_FOUND - 2011, for currentVersion is equal to null</li>
     *  <li>CODE_RESP_NO_PRODUCTS - 2010, for ResponseCode is equal to 1</li>
     *  <li>CODE_RESP_INVALID_CAMPAIGN_ID - 2000, for campaign is equal to null</li>
     *  <li>CODE_RESP_SUCCESS - 0</li>
     *  </ul>
     * @throws Exception {@link java.lang.Exception} A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */

    public Map<String, Object> getSearchresults(HttpServletRequest request,
                                                StreamSearchV2KraftService streamSearchV2Service,
                                                Campaign campaign,
                                                String[] skuIds,
                                                String[] productIds,
                                                String name,
                                                String[] brand,
                                                String[] categories,
                                                String[] categoryIds,
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
                                                MultiFacets selectedFacets,
                                                String sort,
                                                String group,
                                                String[] facets,
                                                String responseFormatterClass,
                                                CatalogMaster catalogMaster,
                                                CategoryMaster categoryMaster,
                                                int offset,
                                                int limit,
                                                boolean usev2,
                                                boolean edismax,
                                                boolean iszeroResult,
                                                boolean spellcheck,
                                                boolean personalize,
                                                String locale,
                                                PimVersion currentVersion,
                                                String contextualParam,
                                                MultiFacets curateTerms,
                                                boolean disableFacetMinCount,
                                                boolean includeGhostProduct,
                                                String[] segments,
                                                long storeId,
                                                MultiFacetsKraft userPreferences,
                                                String similarType,
                                                boolean spellCheckOnly) throws Exception
    {
        Map<String, Object> responseMap = this.workItemResultBuilder(PimConstants.CODE_RESP_SUCCESS, null, null, false);
        List<String> categoryIdsList = new ArrayList<String>();
        if (categoryIds != null && categoryIds.length > 0)
        {
            categoryIdsList.addAll(Arrays.asList(categoryIds));
        }
        PimResponse toRet;
        if (campaign != null)
        {
            Set<String> userFacetList = new HashSet<>();
            if (currentVersion == null || currentVersion.getVersion() == null) { return this.workItemResultBuilder(PimConstants.CODE_RESP_NO_CURRENT_VERSION_FOUND, null, null, false); }
            String catalogId;
            if (catalogMaster != null)
            {
                catalogId = catalogMaster.getCatalogId();
            }
            else
            {
                catalogId = PimConstants.DEFAULT_CATALOG_ID;
            }
            long campaignId = campaign.getId();
            boolean allowSegmentation = ReadUtil.getBoolean(campaign.getProperty(CampaignProperties.PROP_PIM_ALLOW_SEGMENTATION), false);
            if (allowSegmentation)
            {
                segments = PIMUtil.processSegments(segments);
            }

            List<SelectedKraftFacet> searchfacets = null;
            if (selectedFacets != null && selectedFacets.getSelectedFacets() != null)
            {
                SelectedFacet[] selectedFacetsArray = selectedFacets.getSelectedFacets();
                searchfacets = new ArrayList<SelectedKraftFacet>();
                for (SelectedFacet selectedFacet : selectedFacetsArray)
                {
                    SelectedKraftFacet facet = new SelectedKraftFacet();
                    facet.setKey(selectedFacet.getKey());
                    facet.setValue(selectedFacet.getValue());
                    searchfacets.add(facet);
                }
            }

            List<SelectedKraftFacet> customfacets = null;
            if(userPreferences != null && userPreferences.getSelectedFacets() != null && userPreferences.getSelectedFacets().length > 0)
            {
                customfacets = new ArrayList<>();
                searchfacets = searchfacets == null ? new ArrayList<SelectedKraftFacet>() : searchfacets;
                for (SelectedKraftFacet selectedFacet : userPreferences.getSelectedFacets())
                {
                    if(selectedFacet.getOperation() == selectedFacet.FACET_EXCLUDE)
                    {
                        SelectedKraftFacet facet = new SelectedKraftFacet();
                        userFacetList.add(selectedFacet.getKey());
                        facet.setKey(selectedFacet.getKey());
                        facet.setValue(selectedFacet.getValue());
                        facet.setOperation(selectedFacet.getOperation());
                        searchfacets.add(facet);
                    }
                    else
                    {
                        customfacets.add(selectedFacet);
                    }
                }
            }

            if (categoryMaster != null && (ReadUtil.getInt(categoryMaster.getType(), CategoryMaster.CATEGORY_TYPE_STATIC) == CategoryMaster.CATEGORY_TYPE_DYNAMIC))
            {
                PIMItemProperty categoryProperty = categoryMaster.getProperty(CategoryMaster.CATEGORY_FACET_PROPERTY, null);
                String categoryFacet = ReadUtil.getString((categoryProperty != null ? categoryProperty.getValue() : null), null);
                if (categoryFacet != null)
                {
                    MultiFacetsKraft categoryFacetObj = (MultiFacetsKraft) CastUtil.fromJSON(categoryFacet, MultiFacetsKraft.class);
                    SelectedKraftFacet[] categorySelectedFacetsArray = categoryFacetObj.getSelectedFacets();
                    List<SelectedKraftFacet> categorySelectedFacets = null;
                    if (categorySelectedFacetsArray != null && categorySelectedFacetsArray.length > 0)
                    {
                        categorySelectedFacets = Arrays.asList(categorySelectedFacetsArray);
                        for (SelectedKraftFacet categorySelectedFacet : categorySelectedFacets)
                        {
                            List<String> categoryFacetValList = Arrays.asList(categorySelectedFacet.getValue());
                            if (categoryFacetValList.size() > 0)
                            {
                                for (int catIdx = 0; catIdx < categoryFacetValList.size(); catIdx++)
                                {
                                    categoryFacetValList.set(catIdx, categoryFacetValList.get(catIdx).toLowerCase());
                                }
                                categorySelectedFacet.setValue(categoryFacetValList.toArray(new String[categoryFacetValList.size()]));
                            }
                        }
                    }

                    if (searchfacets != null)
                    {
                        for (SelectedKraftFacet categorySelectedFacet : categorySelectedFacets)
                        {
                            boolean missingFacet = true;
                            for (SelectedKraftFacet searchfacet : searchfacets)
                            {
                                if (categorySelectedFacet.getKey().equals(searchfacet.getKey()))
                                {
                                    missingFacet = false;
                                    Set<String> valueSet = new HashSet<String>();
                                    valueSet.addAll(Arrays.asList(categorySelectedFacet.getValue()));
                                    valueSet.addAll(Arrays.asList(searchfacet.getValue()));
                                    searchfacet.setValue(valueSet.toArray(new String[valueSet.size()]));
                                }
                            }

                            if (missingFacet)
                            {
                                searchfacets.add(categorySelectedFacet);
                            }
                        }
                    }
                    else
                    {
                        searchfacets = categorySelectedFacets;
                    }
                }
                categoryIdsList.add(categoryMaster.getCategoryId());
            }

            SelectedFacet[] curate = null;
            if (curateTerms != null && curateTerms.getRecommendationCurates() != null)
            {
                SelectedFacet[] selectedCurate = curateTerms.getRecommendationCurates();
                curate = new SelectedFacet[selectedCurate.length];
                for (int facetIdx = 0; facetIdx < selectedCurate.length; facetIdx++)
                {
                    SelectedFacet facet = new SelectedFacet();
                    facet.setKey(selectedCurate[facetIdx].getKey());
                    facet.setValue(selectedCurate[facetIdx].getValue());
                    curate[facetIdx] = facet;
                }
            }
            float[] priceMinTemp = priceMin;
            float[] priceMaxTemp = priceMax;
            if (priceMin == null)
            {
                priceMinTemp = new float[] { 0f };
            }
            if (priceMax == null)
            {
                priceMaxTemp = new float[] { 0f };
            }
            Response searchResponse = streamSearchV2Service.getProductsForKraft(request, storeId, skuIds, // skuId,
                    productIds, // productId,
                    name, // name,
                    brand, // brand,
                    categories, ((categoryIdsList != null && !categoryIdsList.isEmpty()) ? categoryIdsList.toArray(new String[categoryIdsList.size()]) : null), categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, // division,
                    color, // color,
                    size1, // size1,
                    size2, // size2,
                    priceMinTemp, // priceMin,
                    priceMaxTemp, // priceMax,
                    variant, // variant,
                    searchTerm, // searchTerm,
                    facets, // facets,
                    (searchfacets != null && !searchfacets.isEmpty()) ? searchfacets.toArray(new SelectedKraftFacet[searchfacets.size()]) : null, // selectedFacets,
                    (customfacets != null && customfacets.size() > 0 ? customfacets.toArray(new SelectedKraftFacet[customfacets.size()]) : null),
                    sort, // sort,
                    group, // group,
                    responseFormatterClass, // responseFormatterClass,
                    offset, // offset,
                    limit, // limit,
                    usev2, // usev2,
                    edismax, iszeroResult, // iszeroResult,
                    spellcheck, // spellcheck,
                    personalize, // personalize
                    contextualParam, locale, currentVersion.getVersion(), curate, false, false, null, disableFacetMinCount, includeGhostProduct, null, false, segments, catalogId, similarType, spellCheckOnly);
            JSONObject searchResponseObj = null;
            if (searchResponse != null)
            {
                if (searchResponse.getResponseCode() == PimConstants.CODE_RESP_REDIRECT)
                {
                    toRet = new PimResponse();
                    toRet.setResponseCode(searchResponse.getResponseCode());
                    toRet.setResponseMessage(searchResponse.getResponseMessage());
                    toRet.setRedirectUrl(searchResponse.getRedirectUrl());
                    return this.workItemResultBuilder(searchResponse.getResponseCode(), toRet, null, false);
                }
                if (searchResponse.getResponseCode() == 1 || searchResponse.getResponseCode() == SEARCH_RESPONSE_CODE || searchResponse.getByteContents() == null)
                {
                    return this.workItemResultBuilder(PimConstants.CODE_RESP_NO_PRODUCTS, null, null, false);
                }
                else
                {
                    searchResponseObj = new JSONObject(new String(searchResponse.getByteContents()));
                }
            }
            if (searchResponseObj != null)
            {
                toRet = new PimResponse();
                JSONObject facetObj = (JSONObject) JSONUtils.safeGetJSONObject("facets", searchResponseObj);
                if (facetObj != null)
                {
                    toRet.setFacets(facetObj.toString());
                }
                JSONObject propObj = (JSONObject) JSONUtils.safeGetJSONObject("properties", searchResponseObj);
                if (propObj != null)
                {
                    JSONObject stateObj = (JSONObject) JSONUtils.safeGetJSONObject("state", propObj);
                    if(spellCheckOnly && JSONUtils.safeGetLongValue(stateObj, "productcount", 0) == 0)
                    {
                        String searchCorrected = JSONUtils.safeGetStringValue(stateObj, "searchcorrected", null);
                        if(searchCorrected != null && !searchCorrected.equalsIgnoreCase(searchTerm))
                        {
                            toRet.setSearchcorrected(searchCorrected);
                            toRet.setSearchterm(searchTerm);
                            return responseMap = this.workItemResultBuilder(PimConstants.CODE_RESP_SUCCESS, toRet, null, true);
                        }
                    }
                    if (stateObj != null)
                    {
                        toRet.setProductCount(JSONUtils.safeGetLongValue(stateObj, "productcount", 0));
                        toRet.setSearchterm(JSONUtils.safeGetStringValue(stateObj, "searchterm", null));
                        toRet.setSearchcorrected(JSONUtils.safeGetStringValue(stateObj, "searchcorrected", null));
                        toRet.setSynonym(JSONUtils.safeGetStringValue(stateObj, "synonym", null));
                        JSONArray sortingObjs = JSONUtils.safeGetJSONArray("sorting", stateObj);
                        if (sortingObjs != null)
                        {
                            Set<Sort> selectedSortSet = new LinkedHashSet<>();
                            Set<Sort> sortOptionSet = new LinkedHashSet<>();
                            for (int sortIdx = 0; sortIdx < sortingObjs.length(); sortIdx++)
                            {
                                JSONObject sortingObj = (JSONObject) sortingObjs.get(sortIdx);
                                String selectedSortString = JSONUtils.safeGetJSONProperty("selectedname", sortingObj);
                                if (selectedSortString != null)
                                {
                                    String[] selectedSorts = selectedSortString.split(",");
                                    for (String selectedSort : selectedSorts)
                                    {
                                        selectedSortSet.add(new Sort(selectedSort, null, null, 0, null));
                                    }
                                }
                                JSONArray optionsObjs = JSONUtils.safeGetJSONArray("options", sortingObj);
                                HashSet<String> checkDup = new HashSet<>();
                                for (int optIdx = 0; optIdx < optionsObjs.length(); optIdx++)
                                {
                                    JSONObject optionsObj = (JSONObject) optionsObjs.get(optIdx);
                                    String sortName = JSONUtils.safeGetJSONProperty("name", optionsObj);
                                    String sortLabel = JSONUtils.safeGetJSONProperty("label", optionsObj);
                                    String sortValue = JSONUtils.safeGetJSONProperty("value", optionsObj);
                                    if (!checkDup.contains(sortLabel + sortValue))
                                    {
                                        sortOptionSet.add(new Sort(sortName, sortLabel, sortValue, 0, null));
                                        checkDup.add(sortLabel + sortValue);
                                    }
                                }
                            }
                            if (!selectedSortSet.isEmpty())
                            {
                                toRet.setSelectedSort(selectedSortSet);
                            }
                            if (!sortOptionSet.isEmpty())
                            {
                                toRet.setSorts(sortOptionSet);
                            }
                        }
                        JSONArray selectedFacetObjs = JSONUtils.safeGetJSONArray("selectedfacets", stateObj);
                        if (selectedFacetObjs != null)
                        {
                            Set<SelectedFacet> selectedFacetsSet = new LinkedHashSet<>();
                            for (int facetIdx = 0; facetIdx < selectedFacetObjs.length(); facetIdx++)
                            {
                                JSONObject selectedFacetObj = (JSONObject) selectedFacetObjs.get(facetIdx);
                                String key = JSONUtils.safeGetJSONProperty("primaryname", selectedFacetObj);
                                if(!userFacetList.contains(key))
                                {
                                    SelectedFacet selectedFacet = new SelectedFacet();
                                    selectedFacet.setKey(key);
                                    selectedFacet.setValue(new String[] { JSONUtils.safeGetJSONProperty("name", selectedFacetObj) });
                                    selectedFacetsSet.add(selectedFacet);
                                }

                            }
                            if (!selectedFacetsSet.isEmpty())
                            {
                                toRet.setSelectedFacets(selectedFacetsSet);
                            }
                        }
                    }
                }

                JSONArray productsObj = JSONUtils.safeGetJSONArray("products", (JSONObject) JSONUtils.safeGetJSONObject("children", searchResponseObj));
                if (productsObj != null && productsObj.length() > 0)
                {
                    String[] pIds = new String[productsObj.length()];
                    for (int i = 0; i < productsObj.length(); i++)
                    {
                        JSONObject productObj = (JSONObject) productsObj.get(i);
                        pIds[i] = productObj.getString("productid");
                    }
                    responseMap = this.workItemResultBuilder(PimConstants.CODE_RESP_SUCCESS, toRet, pIds, false);
                }
            }

        }
        else
        {
            responseMap = this.workItemResultBuilder(PimConstants.CODE_RESP_INVALID_CAMPAIGN_ID, null, null, false);
        }
        return responseMap;
    }

    /**
     * <p>This method construct the result map.</p>
     *
     * @param errorCode Represents the response code to return.
     * @param pimResponse {@link com.skava.model.pim.PimResponse} Represents the product loaded and response to return.
     * @param productIds {@link java.lang.String} Represents the product ids to return.
     * @return {@link java.util.Map}Returns the response map of <code>GetSearchResults</code> class.
     * <p>Below are the keys present in the output map</p>
     *  <ul>
     *  <li>PARAM_RESP_CODE - responseCode</li>
     *  <li>PARAM_PIM_RESPONSE - pimResponse </li>
     *  <li>PARAM_PRODUCT_IDS - productIds</li>
     *  </ul>
     */
    private Map<String, Object> workItemResultBuilder(int errorCode,
                                                      PimResponse pimResponse,
                                                      String[] productIds,
                                                      boolean skipProductLoad)
    {
        Map<String, Object> workItemResults = new HashMap<>();
        workItemResults.put(PimConstants.PARAM_RESP_CODE, errorCode);
        workItemResults.put(PimConstants.PARAM_PIM_RESPONSE, pimResponse);
        workItemResults.put(PimConstants.PARAM_PRODUCT_IDS, productIds);
        workItemResults.put(PimKraftConstants.PARAM_SKIP_PRODUCTLOAD, skipProductLoad);
        return workItemResults;
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager)
    {
        //nothing to be done while aborting
    }
}
