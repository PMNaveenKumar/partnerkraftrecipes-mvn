package com.skava.pim;

import com.skava.util.ServerException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.skava.util.Utilities;

import org.springframework.context.MessageSource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.skava.annotation.AuditParam;
import com.skava.annotation.Auditable;
import com.skava.cache.MemCacheManager;
import com.skava.db.DBSession;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SkavaLogService;
import com.skava.model.pim.CategoryMaster;
import com.skava.model.pim.FacetMaster;
import com.skava.model.pim.MultiFacets;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.PimConstants;
import com.skava.model.pim.PimKraftConstants;
import com.skava.model.pim.PimResponse;
import com.skava.model.pim.ProductMaster;
import com.skava.pim.helper.PIMUtil;
import com.skava.services.BpmService;
import com.skava.services.PimServiceKraft;
import com.skava.services.PromotionService;
import com.skava.services.StreamComUserService;
import com.skava.services.StreamInventoryUserService;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.services.StreamSearchV2Service;
import com.skava.util.CampaignUtil;
import com.skava.util.CryptoUtil;
import com.skava.util.ReadUtil;

import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
* <h1> PimServicesImpl </h1>
* <p>This class handles the implementation of PIM services.</p>
* @author: <u>Skava Platform Team</u>
* @version 7.5
* @since 6.0
*/
/**
 * This class handles the implementation of PIM services.
 * @jbpm <code>CategoryService</code> - BPMN diagram has the flow to get the
 *       information about the requested category and available sub categories.
 * @jbpm <code>CategoryServiceLoadAll</code> - BPMN diagram has the flow to get
 *       the information about the all category and available sub categories of
 *       requested storeId.
 * @jbpm <code>ProductListSolrService</code> - BPMN diagram has the flow to get
 *       the products that associated with the requested categoryId from solr
 * @jbpm <code>ProductListDBService</code> - BPMN diagram has the flow to get
 *       the products that associated with the requested categoryId from
 *       database.
 * @jbpm <code>ProductService</code> - BPMN diagram has the flow to get products
 *       information can be retrieved of requested productId.
 * @jbpm <code>ProductServiceLoadAll</code> - BPMN diagram has the flow to get
 *       the information about the all products of requested storeId.
 * @jbpm <code>SkuService</code> - BPMN diagram has the flow to get skus
 *       information can be retrieved of requested skuId.
 * @jbpm <code>PimSearchService</code> - BPMN diagram has the flow to we can
 *       directly get the products by CategoryId, ProductId & SkuId.
 * @jbpm <code>CacheClear</code> - BPMN diagram has the flow to clear the values
 *       based on storeId, categoryId, productId, skuId that are cached in
 *       Memcache.
 * @jbpm <code>FacetService</code> - BPMN diagram has the flow to get the
 *       information about the all facets of requested storeId.
 */
public class PimServicesImplKraft implements PimServiceKraft
{
    private static final String PIMSERVICESIMPL = "PIMSERVICESIMPL";
    private String serviceName;

    /**
     * dbSessionManager The object to access database. cryptoUtil Provides the
     * functionalities of encrypt and decrypt methods. streamSearchV2Service
     * This service provides the functionalities of get products from solr.
     * promotionService This service provides the functionalities of get offers
     * for products. bpmService BPM service to get corresponding bpmn diagram.
     * streamComUserService This service provides the functionalities of get
     * user details. inventoryService This service is used to check the
     * availability of products. messageSource It has the message for each
     * response code cacheManager It is used to manage the cache
     * functionalities. allowPreview It is used to manage the preview.
     */
    private DBSessionManager dbSessionManager;
    private CryptoUtil cryptoUtil;
    @Setter private StreamSearchV2KraftService streamSearchV2Service;
    @Setter private StreamSearchV2Service streamSearchV2CoreService;
    @Setter private PromotionService promotionService;
    @Setter private BpmService bpmService;
    @Setter private StreamComUserService streamComUserService;
    private StreamInventoryUserService inventoryService;
    private MessageSource messageSource;
    private MemCacheManager cacheManager;
    private boolean allowPreview = false;

    /**
     * Constructor - Initialize the required services and properties when the
     * deployment of PIM service.
     * 
     * @param dbSessionManager
     *            This service provides the functionalities of database
     *            management.
     * @param streamSearchV2Service
     *            This service provides the functionalities of get products from
     *            solr.
     * @param inventoryService
     *            This service is used to check the availability of products.
     * @param cacheManager
     *            It is used to manage the cache functionalities.
     * @param resourceBundle
     *            It has the message for each response code
     * @param prefix
     *            Used for the functionalities of encrypt and decrypt methods.
     * @param promotionService
     *            This service provides the functionalities of get offers for
     *            products.
     * @param bpmService
     *            This service is used to get corresponding bpmn diagram for API
     * @param streamComUserService
     *            This service provides the functionalities of get user details.
     * @param allowPreview
     *            It is used to manage the preview.
     * @throws ServerException
     *             It will be triggered for all the server exception thrown with
     *             in this function.
     */

    public PimServicesImplKraft(DBSessionManager dbSessionManager,
                                StreamSearchV2KraftService streamSearchV2Service,
                                StreamSearchV2Service streamSearchV2CoreService,
                                StreamInventoryUserService inventoryService,
                                MemCacheManager cacheManager,
                                MessageSource resourceBundle, String prefix,
                                PromotionService promotionService,
                                BpmService bpmService,
                                StreamComUserService streamComUserService,
                                boolean allowPreview) throws ServerException
    {
        this.dbSessionManager = dbSessionManager;
        this.cryptoUtil = new CryptoUtil(prefix);
        this.streamSearchV2Service = streamSearchV2Service;
        this.streamSearchV2CoreService = streamSearchV2CoreService;
        this.inventoryService = inventoryService;
        this.cacheManager = cacheManager;
        this.messageSource = resourceBundle;
        this.promotionService = promotionService;
        this.bpmService = bpmService;
        this.streamComUserService = streamComUserService;
        this.allowPreview = allowPreview;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Auditable(eventType = SkavaLogService.AUDIT_EVENT_LOAD)
    public PimResponse getProductListFromSolrKraft(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   @AuditParam(field = "storeId") long storeId,
                                                   @AuditParam(field = "categoryId") String categoryId,
                                                   @AuditParam(field = "locale") String locale,
                                                   @AuditParam(field = "selectedFacets") MultiFacets selectedFacets,
                                                   @AuditParam(field = "responseFormatterClass") String responseFormatterClass,
                                                   @AuditParam(field = "sort") String sort,
                                                   @AuditParam(field = "previewTime") String previewTime,
                                                   @AuditParam(field = "offset") int offset,
                                                   @AuditParam(field = "limit") int limit,
                                                   @AuditParam(field = "contextualParam") String contextualParam,
                                                   @AuditParam(field = "catalogId") String catalogId,
                                                   @AuditParam(field = "disableFacetMinCount") boolean disableFacetMinCount,
                                                   @AuditParam(field = "includeOutOfStock") boolean includeOutOfStock,
                                                   @AuditParam(field = "includeGhostProduct") boolean includeGhostProduct,
                                                   @AuditParam(field = "skipPromo") boolean skipPromo,
                                                   @AuditParam(field = "skipInventory") boolean skipInventory,
                                                   @AuditParam(field = "segments") String[] segments,
                                                   @AuditParam(field = "userPreferences") MultiFacetsKraft userPreferences,
                                                   @AuditParam(field = "disableDefaultSort") boolean disableDefaultSort) throws ServerException
    {
        PimResponse toRet = new PimResponse();
        DBSession dbSession = null;
        Locale localeObj = null;
        int responseCode = 0;
        try
        {
            dbSession = dbSessionManager.getReadOnlyDBSession();
            if (catalogId == null)
            {
                catalogId = CampaignUtil.getPimCatalogId(dbSession, storeId);
            }
            this.serviceName = PIMUtil.getCallingMethodName(2);
            localeObj = Utilities.getLocale(locale);
            Map<String, Object> processParams = new HashMap<>();
            processParams.put(PimConstants.PARAM_HTTPSERVLET_REQUEST, request);
            processParams.put(PimConstants.PARAM_HTTPSERVLET_RESPONSE, response);
            processParams.put(PimConstants.PARAM_SEARCHV2_SERVICE, streamSearchV2Service);
            processParams.put(PimConstants.PARAM_STORE_ID, storeId);
            processParams.put(PimConstants.PARAM_CATEGORY_ID, categoryId);
            processParams.put(PimConstants.PARAM_SELECTED_FACETS, selectedFacets);
            processParams.put(PimConstants.PARAM_RESPONSEFORMATTER_CLASS, responseFormatterClass);
            processParams.put(PimConstants.PARAM_FACETS, new String[] { "true" });
            processParams.put(PimConstants.PARAM_SORT, sort);
            processParams.put(PimConstants.PARAM_OFFSET, offset);
            processParams.put(PimConstants.PARAM_LIMIT, limit);
            processParams.put(PimConstants.PARAM_CONTEXTUALPARAM, contextualParam);
            processParams.put(PimConstants.PARAM_CATALOG_ID, catalogId);
            processParams.put(PimConstants.PARAM_LOCALE, locale);
            processParams.put(PimConstants.PARAM_PROMOTION_SERVICE, promotionService);
            processParams.put(PimConstants.PARAM_STREAM_COM_USER_SERVICE, streamComUserService);
            processParams.put(PimConstants.PARAM_INVENTORY_SERVICE, inventoryService);
            processParams.put(previewTime == null ? PimConstants.PARAM_CATALOGTIME : PimConstants.PARAM_PREVIEWTIME, previewTime == null ? System.currentTimeMillis() : previewTime);
            processParams.put(PimConstants.PARAM_DBSESSION_MANAGER, dbSessionManager);
            processParams.put(PimConstants.PARAM_MEMCACHE_MANAGER, cacheManager);
            processParams.put(PimConstants.PARAM_CRYPTOUTIL, cryptoUtil);
            processParams.put(PimConstants.PARAM_DISABLE_FACET_MIN_COUNT, disableFacetMinCount);
            processParams.put(PimConstants.PARAM_INCLUDE_OUT_OF_STOCK, includeOutOfStock);
            processParams.put(PimConstants.PARAM_INCLUDE_GHOST_PRODUCT, includeGhostProduct);
            processParams.put(PimConstants.PARAM_SKIP_PROMOTION, skipPromo);
            processParams.put(PimConstants.PARAM_SKIP_INVENTORY, skipInventory);
            processParams.put(PimConstants.PARAM_SEGMENTS, segments);
            processParams.put(PimKraftConstants.PARAM_USER_PREFERENCES, userPreferences);
            processParams.put(PimKraftConstants.PARAM_DISABLE_DEFAULT_SORT, disableDefaultSort);
            processParams.put(PimConstants.SERVICE_NAME, this.serviceName);

            processParams.put(PimConstants.PARAM_DBSESSION, dbSession);

            Map<String, Object> processResponse = this.bpmService.startProcess("ProductListSolrServiceKraft", processParams);

            responseCode = ReadUtil.getInt(processResponse.get(PimConstants.PARAM_RESP_CODE), 0);
            if (responseCode != 0 || !processResponse.containsKey(PimConstants.PARAM_PIM_RESPONSE)) { throw new ServerException(responseCode); }
            toRet = (PimResponse) processResponse.get(PimConstants.PARAM_PIM_RESPONSE);
            toRet.setCategory((CategoryMaster) processResponse.get(PimConstants.PARAM_CATEGORY_MASTER));
            if (processResponse.containsKey(PimConstants.PARAM_PRODUCT_MASTERS))
            {
                ProductMaster[] productMasters = (ProductMaster[]) processResponse.get(PimConstants.PARAM_PRODUCT_MASTERS);
                if (productMasters != null && productMasters.length > 0)
                {
                    toRet.setCurrencySign(ReadUtil.getString(processResponse.get(PimConstants.PARAM_CURRENCY_SIGN), null));
                    toRet.setProducts(new LinkedHashSet<ProductMaster>(Arrays.asList(productMasters)));
                    Set<FacetMaster> facetInfo = (Set<FacetMaster>) processResponse.get(PimConstants.PARAM_FACET_INFO);
                    if (facetInfo != null && !facetInfo.isEmpty())
                    {
                        toRet.setFacetMasters(facetInfo);
                    }
                }
                else
                {
                    throw new ServerException(PimConstants.CODE_RESP_NO_PRODUCTS);
                }
            }
            else
            {
                throw new ServerException(PimConstants.CODE_RESP_NO_PRODUCTS);
            }
        }
        catch (ServerException se)
        {
            if (dbSession != null)
            {
                dbSession.endSession(se);
            }
            responseCode = se.getErrorCode();
            if (!PimConstants.knownError(responseCode))
            {
                PIMUtil.writeLog(Level.ERROR, this.serviceName, PIMSERVICESIMPL, se, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_SERVER_EXCEPTION, null, PIMSERVICESIMPL);
            }
            toRet = PIMUtil.getPimResponse(messageSource, toRet, localeObj, responseCode, null);
        }
        finally
        {
            if (dbSession != null)
            {
                dbSession.endSession();
                dbSession = null;
            }
        }

        return toRet;
    }

    /**
     * <p>Using this method we can directly get the products by CategoryId,
     * ProductId and SkuId.</p>
     * 
     * @param request
     *            It contains the information of request for HTTP servlets.
     * @param response
     *            To manipulate the HTTP protocol specified header information
     *            and return data for client.
     * @param storeId
     *            In all the microservice the configuration parameters of the
     *            APIs are maintained in an entity called Campaign. We can
     *            customize the functionality of the API using the campaign
     *            properties. Campaign Id is a mandatory parameter for all the
     *            microservice.
     * @param skuId
     *            Product are the sku group that are configured in PIMAdmin.We
     *            can get product available in the requested skuId.
     * @param productId
     *            Products that are configured in the PIM Admin will have unique
     *            identifier. Based on the mentioned identifier, associated
     *            product information are responded.
     * @param name
     *            Specifies name containing the product name to fetch.
     * @param brand
     *            Specifies the products containing the brand should be fetched.
     * @param categories
     *            Categories that are configured in the PIM Admin will have
     *            unique identifier. Based on the mentioned identifier,
     *            associated category information and its sub categories that
     *            have products are responded.
     * @param categoryids
     *            Categories that are configured in the PIM Admin will have
     *            unique identifier. Based on the mentioned identifier,
     *            associated category information and its sub categories that
     *            have products are responded.
     * @param categorylevel1
     *            TODO Deprecated not used
     * @param categorylevel2
     *            TODO Deprecated not used
     * @param categorylevel3
     *            TODO Deprecated not used
     * @param categorylevel4
     *            TODO Deprecated not used
     * @param categorylevel5
     *            TODO Deprecated not used
     * @param division
     *            TODO Deprecated not used
     * @param color
     *            TODO Deprecated not used
     * @param size1
     *            TODO Deprecated not used
     * @param size2
     *            TODO Deprecated not used
     * @param priceMin
     *            TODO Deprecated not used
     * @param priceMax
     *            TODO Deprecated not used
     * @param variant
     *            TODO Deprecated not used
     * @param searchTerm
     *            It is used to get the products by search with CategoryId or
     *            ProductId or SkuId.
     * @param facets
     *            TODO Deprecated not used
     * @param selectedFacets
     *            It is used to mention the filter for the products that are
     *            available in the requested category. It have a json format.
     *            {"selectedFacets":[{"key":"color","value":["blue"]}]}. We can
     *            add more filter by adding further objects to the
     *            selectedFacets array. Applicable filter can be retrieved from
     *            the response.
     * @param sort
     *            It is used to apply sorting in the product response. Based on
     *            the sortable facets configured in the PIM Admin, sortable
     *            facet value will be available in the response, it needs to be
     *            set as value for this parameter to apply the sort.
     * @param group
     *            TODO Deprecated not used
     * @param responseFormatterClass
     *            TODO Deprecated not used
     * @param catalogId
     *            All the micro services functionality can be customized in
     *            catalog level. Campaign can have multiple catalogs. Each
     *            catalog can have different set of properties that will
     *            influence the functionality of the API. Unique identifier
     *            associated for the catalog will go here.
     * @param offset
     *            This parameter will be available for all the services that
     *            supports pagination. This parameter is used to mention the
     *            starting index of the items which going to responded by the
     *            API.
     * @param limit
     *            This parameter will be available for all the services that
     *            supports pagination. This parameter is used to mention the
     *            number of maximum items that needs to be responded for the
     *            request. This parameter have a internal max limit of 100.
     * @param usev2
     *            TODO Deprecated not used
     * @param edismax
     *            TODO Deprecated not used
     * @param iszeroResult
     *            TODO Deprecated not used
     * @param spellcheck
     *            TODO Deprecated not used
     * @param personalize
     *            TODO Deprecated not used
     * @param locale
     *            API Response and error messages will be responded in the
     *            locale mentioned in this parameter. Locale needs to be
     *            mentioned in Java standard locale
     *            format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param previewTime
     *            By this parameter we can simulate the API to respond the
     *            Future or Past data. In this parameter we've to send the time
     *            in milliseconds format. For Ex: "1514764800000" for 1-Jan-2018
     *            0Hr:0Min:0Sec.
     * @param contextualParam
     *            TODO Deprecated not used
     * @param includeOutOfStock
     *            This is a boolean parameter which is used to mention the
     *            whether the product that are out of stock needs to be
     *            responded or not.
     * @param includeGhostProduct
     *            It is boolean parameter to mention that mention that whether
     *            products that are invisible needs to be honored while
     *            considering the category to respond.
     * @param disableFacetMinCount
     *            TODO Deprecated not used
     * @param segments
     *            It refers to user segments, by this the products that are
     *            associated to the requested segments alone will be considered
     *            for the products to respond.
     * @return {@link com.skava.model.pim.PimResponse} It returns the Products in <code>PIM response</code>.
     */
    @SuppressWarnings("unchecked")
    @Override
    @Auditable(eventType = SkavaLogService.AUDIT_EVENT_LOAD)
    public PimResponse getProductsKraft(HttpServletRequest request,
                                        HttpServletResponse response,
                                        @AuditParam(field = "storeId") long storeId,
                                        @AuditParam(field = "skuId") String[] skuId,
                                        @AuditParam(field = "productId") String[] productId,
                                        @AuditParam(field = "name") String name,
                                        @AuditParam(field = "brand") String[] brand,
                                        @AuditParam(field = "categories") String[] categories,
                                        @AuditParam(field = "categoryids") String[] categoryids,
                                        @AuditParam(field = "categorylevel1") String categorylevel1,
                                        @AuditParam(field = "categorylevel2") String categorylevel2,
                                        @AuditParam(field = "categorylevel3") String categorylevel3,
                                        @AuditParam(field = "categorylevel4") String categorylevel4,
                                        @AuditParam(field = "categorylevel5") String categorylevel5,
                                        @AuditParam(field = "division") String[] division,
                                        @AuditParam(field = "color") String[] color,
                                        @AuditParam(field = "size1") String size1,
                                        @AuditParam(field = "size2") String size2,
                                        @AuditParam(field = "priceMin") float[] priceMin,
                                        @AuditParam(field = "priceMax") float[] priceMax,
                                        @AuditParam(field = "variant") String variant,
                                        @AuditParam(field = "searchTerm") String searchTerm,
                                        @AuditParam(field = "facets") String[] facets,
                                        @AuditParam(field = "selectedFacets") MultiFacets selectedFacets,
                                        @AuditParam(field = "sort") String sort,
                                        @AuditParam(field = "group") String group,
                                        @AuditParam(field = "responseFormatterClass") String responseFormatterClass,
                                        @AuditParam(field = "catalogId") String catalogId,
                                        @AuditParam(field = "offset") int offset,
                                        @AuditParam(field = "limit") int limit,
                                        @AuditParam(field = "usev2") boolean usev2,
                                        @AuditParam(field = "edismax") boolean edismax,
                                        @AuditParam(field = "iszeroResult") boolean iszeroResult,
                                        @AuditParam(field = "spellcheck") boolean spellcheck,
                                        @AuditParam(field = "personalize") boolean personalize,
                                        @AuditParam(field = "locale") String locale,
                                        @AuditParam(field = "previewTime") String previewTime,
                                        @AuditParam(field = "contextualParam") String contextualParam,
                                        @AuditParam(field = "includeOutOfStock") boolean includeOutOfStock,
                                        @AuditParam(field = "includeGhostProduct") boolean includeGhostProduct,
                                        @AuditParam(field = "disableFacetMinCount") boolean disableFacetMinCount,
                                        @AuditParam(field = "segments") String[] segments,
                                        @AuditParam(field = "userPreferences") MultiFacetsKraft userPreferences,
                                        @AuditParam(field = "similarType") String similarType,
                                        @AuditParam(field = "disableDefaultSort") boolean disableDefaultSort,
                                        @AuditParam(field = "spellCheckOnly") boolean spellCheckOnly) throws ServerException

    {

        PimResponse toRet = new PimResponse();
        DBSession dbSession = null;
        Locale localeObj = null;
        int responseCode = 0;
        try
        {
            dbSession = dbSessionManager.getReadOnlyDBSession();
            if (catalogId == null)
            {
                catalogId = CampaignUtil.getPimCatalogId(dbSession, storeId);
            }
            this.serviceName = PIMUtil.getCallingMethodName(2);
            localeObj = Utilities.getLocale(locale);
            Map<String, Object> processParams = new HashMap<>();
            processParams.put(PimConstants.PARAM_HTTPSERVLET_REQUEST, request);
            processParams.put(PimConstants.PARAM_HTTPSERVLET_RESPONSE, response);
            processParams.put(PimConstants.PARAM_SEARCHV2_SERVICE, streamSearchV2Service);
            processParams.put(PimKraftConstants.PARAM_SEARCHV2_CORE_SERVICE, streamSearchV2CoreService);
            processParams.put(PimConstants.PARAM_STORE_ID, storeId);
            processParams.put(PimConstants.PARAM_PRODUCT_IDS, productId);
            processParams.put(PimConstants.PARAM_SKU_IDS, skuId);
            processParams.put(PimConstants.PARAM_NAME, name);
            processParams.put(PimConstants.PARAM_BRAND, brand);
            processParams.put(PimConstants.PARAM_CATEGORY, categories);
            processParams.put(PimConstants.PARAM_CATEGORY_IDS, categoryids);
            processParams.put(PimConstants.PARAM_CATEGORY_LEVEL1, categorylevel1);
            processParams.put(PimConstants.PARAM_CATEGORY_LEVEL2, categorylevel2);
            processParams.put(PimConstants.PARAM_CATEGORY_LEVEL3, categorylevel3);
            processParams.put(PimConstants.PARAM_CATEGORY_LEVEL4, categorylevel4);
            processParams.put(PimConstants.PARAM_CATEGORY_LEVEL5, categorylevel5);
            processParams.put(PimConstants.PARAM_DIVISION, division);
            processParams.put(PimConstants.PARAM_COLOR, color);
            processParams.put(PimConstants.PARAM_SIZE1, size1);
            processParams.put(PimConstants.PARAM_SIZE2, size2);
            processParams.put(PimConstants.PARAM_PRICE_MIN, priceMin);
            processParams.put(PimConstants.PARAM_PRICE_MAX, priceMax);
            processParams.put(PimConstants.PARAM_VARIANT, variant);
            processParams.put(PimConstants.PARAM_SEARCH_TERM, searchTerm);
            processParams.put(PimConstants.PARAM_FACETS, facets);
            processParams.put(PimConstants.PARAM_SELECTED_FACETS, selectedFacets);
            processParams.put(PimKraftConstants.PARAM_USER_PREFERENCES, userPreferences);
            processParams.put(PimConstants.PARAM_SORT, sort);
            processParams.put(PimConstants.PARAM_GROUP, group);
            processParams.put(PimConstants.PARAM_RESPONSEFORMATTER_CLASS, responseFormatterClass);
            processParams.put(PimConstants.PARAM_CATALOG_ID, catalogId);
            processParams.put(PimConstants.PARAM_OFFSET, offset);
            processParams.put(PimConstants.PARAM_LIMIT, limit);
            processParams.put(PimConstants.PARAM_USEV2, usev2);
            processParams.put(PimConstants.PARAM_EDISMAX, edismax);
            processParams.put(PimConstants.PARAM_ISZERORESULT, iszeroResult);
            processParams.put(PimConstants.PARAM_SPELLCHECK, spellcheck);
            processParams.put(PimConstants.PARAM_PERSONALIZE, personalize);
            processParams.put(PimConstants.PARAM_LOCALE, locale);
            processParams.put(PimConstants.PARAM_CONTEXTUALPARAM, contextualParam);
            processParams.put(PimConstants.PARAM_PROMOTION_SERVICE, promotionService);
            processParams.put(PimConstants.PARAM_STREAM_COM_USER_SERVICE, streamComUserService);
            processParams.put(PimConstants.PARAM_INVENTORY_SERVICE, inventoryService);
            processParams.put(previewTime == null ? PimConstants.PARAM_CATALOGTIME : PimConstants.PARAM_PREVIEWTIME, previewTime == null ? System.currentTimeMillis() : previewTime);
            processParams.put(PimConstants.PARAM_DBSESSION_MANAGER, dbSessionManager);
            processParams.put(PimConstants.PARAM_MEMCACHE_MANAGER, cacheManager);
            processParams.put(PimConstants.PARAM_CRYPTOUTIL, cryptoUtil);
            processParams.put(PimConstants.PARAM_INCLUDE_OUT_OF_STOCK, includeOutOfStock);
            processParams.put(PimConstants.PARAM_INCLUDE_GHOST_PRODUCT, includeGhostProduct);
            processParams.put(PimConstants.PARAM_DISABLE_FACET_MIN_COUNT, disableFacetMinCount);
            processParams.put(PimConstants.PARAM_SEGMENTS, segments);
            processParams.put(PimConstants.SERVICE_NAME, this.serviceName);

            processParams.put(PimKraftConstants.PARAM_SIMILAR_TYPE, similarType);
            processParams.put(PimKraftConstants.PARAM_DISABLE_DEFAULT_SORT, disableDefaultSort);
            processParams.put(PimKraftConstants.PARAM_SPELLCHECK_ONLY, spellCheckOnly);

            processParams.put(PimConstants.PARAM_DBSESSION, dbSession);

            Map<String, Object> processResponse = this.bpmService.startProcess("PimSearchServiceKraft", processParams);

            responseCode = ReadUtil.getInt(processResponse.get(PimConstants.PARAM_RESP_CODE), 0);
            if ((responseCode != 0 && responseCode != PimConstants.CODE_RESP_REDIRECT) || !processResponse.containsKey(PimConstants.PARAM_PIM_RESPONSE)) { throw new ServerException(responseCode); }

            toRet = (PimResponse) processResponse.get(PimConstants.PARAM_PIM_RESPONSE);
            boolean skipProductLoad = ReadUtil.getBoolean(processResponse.get(PimKraftConstants.PARAM_SKIP_PRODUCTLOAD), false);
            if(responseCode == 0 && skipProductLoad)
            {
                toRet.setResponseCode(responseCode);
                toRet.setResponseMessage("Success");
                return toRet;
            }
            if (responseCode == PimConstants.CODE_RESP_REDIRECT)
            {
                return toRet;
            }
            else if (processResponse.containsKey(PimConstants.PARAM_PRODUCT_MASTERS))
            {
                ProductMaster[] productMasters = (ProductMaster[]) processResponse.get(PimConstants.PARAM_PRODUCT_MASTERS);
                if (productMasters != null && productMasters.length > 0)
                {
                    toRet.setCurrencySign(ReadUtil.getString(processResponse.get(PimConstants.PARAM_CURRENCY_SIGN), null));
                    toRet.setProducts(new LinkedHashSet<ProductMaster>(Arrays.asList(productMasters)));
                    Set<FacetMaster> facetInfo = (Set<FacetMaster>) processResponse.get(PimConstants.PARAM_FACET_INFO);
                    if (facetInfo != null && !facetInfo.isEmpty())
                    {
                        toRet.setFacetMasters(facetInfo);
                    }
                }
                else
                {
                    throw new ServerException(PimConstants.CODE_RESP_NO_PRODUCTS);
                }
            }
            else
            {
                throw new ServerException(PimConstants.CODE_RESP_NO_PRODUCTS);
            }
        }
        catch (ServerException se)
        {
            if (dbSession != null)
            {
                dbSession.endSession(se);
            }
            responseCode = se.getErrorCode();
            if (!PimConstants.knownError(responseCode))
            {
                PIMUtil.writeLog(Level.ERROR, this.serviceName, PIMSERVICESIMPL, se, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_SERVER_EXCEPTION, null, PIMSERVICESIMPL);
            }
            toRet = PIMUtil.getPimResponse(messageSource, toRet, localeObj, responseCode, null);
        }
        finally
        {
            if (dbSession != null)
            {
                dbSession.endSession();
                dbSession = null;
            }
        }
        return toRet;
    }
}
