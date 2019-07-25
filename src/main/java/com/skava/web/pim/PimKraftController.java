package com.skava.web.pim;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.skava.builder.interfaces.PimServiceBuilder;
import com.skava.builder.interfaces.PimServiceKraftBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.model.Response;
import com.skava.model.TenantThreadLocal;
import com.skava.model.pim.MultiFacets;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.PimConstants;
import com.skava.model.pim.PimKraftConstants;
import com.skava.model.pim.PimResponse;
import com.skava.model.pim.SkuRequest;
import com.skava.services.PimService;
import com.skava.services.PimServiceKraft;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.helpers.CustomWebEditor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Setter;

/**
 * PIM Service end points are available in this class.
 * <h1> PimController </h1>
 *
 * @author : Skava Platform Team
 * @version 7.5
 * @since 6.0
 */
@Controller
@Api(tags = { "Pim" }, value = "Pim", description = "Pim Microservices - API", protocols = "https")

public class PimKraftController
{

    @Autowired @Setter private SkavaTenantContextFactory skavaKraftContextFactory;

    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());
    
    /**
     * <h1>getProductlist</h1>
     * <p>This method will get the products that associated with the requested categoryId from solr</p>
     * 
     * @param request {@link javax.servlet.http.HttpServletRequest} It contains the information of request for HTTP servlets.
     * @param response {@link javax.servlet.http.HttpServletResponse} To manipulate the HTTP protocol specified header information and return data for client.
     * @param version {@link java.lang.String} It hold the value of version number of API. Using this parameter we can access the different version of the API.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter  for all the microservice.
     * @param categoryId {@link java.lang.String} Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded.
     * @param catalogId {@link java.lang.String} All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here.
     * @param selectedFacets {@link com.skava.model.pim.MultiFacets} It is used to mention the filter for the products that are available in the requested category. Provided as String with json data encoded. List of available facets for the request will be available in the productlist response with the key \"facets\". Ex:\n"+"\n"+"\n<pre>{\"color\":[{\"name\":\"blue\",\"count\":2}\n"+"\n"+",{\"name\":\"green\",\"count\":1}],\"price\":[{\"name\":\"250\",\"count\":1}],\"Delivery Method\":[{\"name\":\"Courier\",\"count\":1}]}</pre>"+"\n"+". We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response. Ex:\n"+"\n"+"<pre>{\"selectedFacets\":[{\"key\":\"Color\",\"value\":[\"blue\",\"green\"]},\n"+"\n"+"\"{\"key\":\"Delivery Method\",\"value\":[\"Courier\"]}]}</pre>\n"+"\n"+" Here both the Color and Delivery method with the respective value must be available for a product to be listed. If more than one value provided in facet value for a particular key, either one value must be available with the facet key. Ex: Sku/Product should have facet \"Color\" with value as \"blue\" or \"green\". For range filter, values can be provided as \"200~300\"
     * @param responseFormatterClass TODO Deprecated not used
     * @param sort {@link java.lang.String} It is used to apply sorting in the product response. Based on the sortable facets configured in the PIM Admin, sortable facet value will be available in the response, it needs to be set as value for this parameter to apply the sort.
     * @param locale {@link java.lang.String} API Response and error messages will be responded in the locale mentioned in this parameter. Locale needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100.
     * @param previewTime {@link java.lang.String} By this parameter we can simulate the API to respond the Future or Past data. In this parameter we've to send the time in milliseconds format. For Ex: "1514764800000" for 01-Jan-2018 00:00:00..
     * @param contextualParam TODO Deprecated not used
     * @param disableFacetMinCount TODO Deprecated not used
     * @param includeOutOfStock This is a boolean parameter which is used to mention the whether the product that are out of stock needs to be responded or not.
     * @param includeGhostProduct It is boolean parameter to mention that mention that whether products that are invisible needs to be honored while considering the category to respond.
     * @param skipPromo It is boolean parameter to mention that whether the promotion offer can apply to this product needs to be responded or not.
     * @param skipInventory It is boolean parameter to mention that whether the inventory can check the availability of this product needs to be responded or not.
     * @param segments {@link java.lang.String} This parameter represents the segments identifiers for which the API is requested for. User segments are logical group of end users who are grouped together using certain attributes i.e. demographics/membership level/Purchase history. When the user segment(s) is passed the API will fetch the product only if it falls under the subjected segment(s).
     * @return {@link com.skava.model.pim.PimResponse} It returns the Products with PIM response.
     */

    @ApiOperation(value = "Get Products from Solr", notes = "This API fetches all products that are associated with the requested category identifier. This service supports pagination, filtering and sorting.This service will get the products that associated with the requested categoryId from solr(http://lucene.apache.org/solr/). In order to index the data to the solr, we need to configure indexer feed(URL).", response = PimResponse.class, protocols = "https")
    @RequestMapping(value = "/{version}/kraft/productlist/loadSearch", method = RequestMethod.GET)
    @ResponseBody
    public PimResponse getProductlistForKraft(@ApiParam(value = PimConstants.DEF_REQUEST_PARAM, required = true) HttpServletRequest request,
                                      @ApiParam(value = PimConstants.DEF_RESPONSE_PARAM, required = true) HttpServletResponse response,
                                      @ApiParam(value = PimConstants.DEF_VERSION_PARAM, required = true) @Deprecated @PathVariable String version,
                                      @ApiParam(value = PimConstants.DEF_STOREID_PARAM_REQUIRED, required = true) @RequestParam(value = "storeId", required = true) long storeId,
                                      @ApiParam(value = PimConstants.DEF_CATEGORYID_PARAM_REQUIRED, required = true) @RequestParam(value = "categoryId", required = true) String categoryId,
                                      @ApiParam(value = PimConstants.DEF_CATALOGID_PARAM, required = false) @RequestParam(value = "catalogId", required = false) String catalogId,
                                      @ApiParam(value = PimConstants.DEF_MULTIFACETS_PARAM, required = false) @RequestParam(value = "filter", required = false) MultiFacets selectedFacets,
                                      @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "responseFormatterClass", required = false, defaultValue = "com.skava.searchv2.resultformatter.ComSolrResultFormatterKraft") String responseFormatterClass,
                                      @ApiParam(value = PimConstants.DEF_SORT_PARAM, required = false) @RequestParam(value = "sort", required = false) String sort,
                                      @ApiParam(value = PimConstants.DEF_LOCALE_PARAM, required = false) @RequestParam(value = "locale", required = false) String locale,
                                      @ApiParam(value = PimConstants.DEF_OFFSET_PARAM, required = true) @RequestParam(value = "offset", required = true) int offset,
                                      @ApiParam(value = PimConstants.DEF_LIMIT_PARAM, required = true) @RequestParam(value = "limit", required = true) int limit,
                                      @ApiParam(value = PimConstants.DEF_PREVIEWTIME_PARAM, required = false) @RequestParam(value = "previewTime", required = false) String previewTime,
                                      @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "contextualParam", required = false) String contextualParam,
                                      @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "disableFacetMinCount", required = false) boolean disableFacetMinCount,
                                      @ApiParam(value = PimConstants.DEF_INCLUDEOUTOFSTOCK_PARAM, required = false) @RequestParam(value = "includeOutOfStock", required = false) boolean includeOutOfStock,
                                      @ApiParam(value = PimConstants.DEF_INCLUDEGHOST_PARAM, required = false) @RequestParam(value = "includeGhostProduct", required = false) boolean includeGhostProduct,
                                      @ApiParam(value = PimConstants.DEF_SKIPPROMO_PARAM, required = false) @RequestParam(value = "skipPromo", required = false) boolean skipPromo,
                                      @ApiParam(value = PimConstants.DEF_SKIPINVENTORY_PARAM, required = false) @RequestParam(value = "skipInventory", required = false) boolean skipInventory,
                                      @ApiParam(value = PimConstants.DEF_SEGMENTS_PARAM, required = false) @RequestParam(value = "segment", required = false) String[] segments,
                                      @ApiParam(value = PimKraftConstants.DEF_USERPREFERENCES, required = false) @RequestParam(value = "userPreferences", required = false) MultiFacetsKraft userPreferences,
                                      @ApiParam(value = PimKraftConstants.DEF_DISABLEDEFAULTSORT_PARAM, defaultValue="false", required = false) @RequestParam(value = "disableDefaultSort", defaultValue="false", required = false) boolean disableDefaultSort)

    {
        PimResponse toRet = new PimResponse(Response.RESPONSE_FAILED, Response.RESPONSE_MSG_FAILURE);
        try
        {
            toRet = ((PimServiceKraft) skavaKraftContextFactory.get(TenantThreadLocal.get(), PimServiceKraftBuilder.PIMSERVICEKRAFT)).getProductListFromSolrKraft(request, response, storeId, categoryId, locale, selectedFacets, responseFormatterClass, sort, previewTime, offset, limit, contextualParam, catalogId, disableFacetMinCount, includeOutOfStock, includeGhostProduct, skipPromo, skipInventory, segments, userPreferences, disableDefaultSort);
        }
        catch (ServerException se)
        {
            logger.info("sending error response", se);
            toRet.setResponseCode(Response.RESPONSE_FAILED);
            toRet.setResponseMessage(se.getMessage());
        }
        return toRet;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(MultiFacets.class, new CustomWebEditor<MultiFacets>(MultiFacets.class));
        binder.registerCustomEditor(MultiFacetsKraft.class, new CustomWebEditor<MultiFacetsKraft>(MultiFacetsKraft.class));
        binder.registerCustomEditor(SkuRequest.class, new CustomWebEditor<SkuRequest>(SkuRequest.class));
    }

    /**
     * <h1>getProducts</h1>
     * <p>Using this method we can directly get the products by CategoryId, ProductId and SkuId.</p>
     * 
     * @param request {@link javax.servlet.http.HttpServletRequest} It contains the information of request for HTTP servlets.
     * @param response {@link javax.servlet.http.HttpServletResponse} To manipulate the HTTP protocol specified header information and return data for client.
     * @param version {@link java.lang.String} It hold the value of version number of API. Using this parameter we can access the different version of the API.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter  for all the microservice.
     * @param skuId {@link java.lang.String} Product are the sku group that are configured in PIMAdmin.We can get product available in the requested skuId.
     * @param productId {@link java.lang.String} Products that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated product information are responded.
     * @param name {@link java.lang.String} Specifies name containing the product name to fetch.
     * @param brand {@link java.lang.String} Specifies the products containing the brand should be fetched.
     * @param categories {@link java.lang.String} Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded.
     * @param categoryid {@link java.lang.String} Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded.
     * @param categorylevel1 TODO Deprecated not used
     * @param categorylevel2 TODO Deprecated not used
     * @param categorylevel3 TODO Deprecated not used
     * @param categorylevel4 TODO Deprecated not used
     * @param categorylevel5 TODO Deprecated not used
     * @param division TODO Deprecated not used
     * @param color TODO Deprecated not used
     * @param size1 TODO Deprecated not used
     * @param size2 TODO Deprecated not used
     * @param priceMin TODO Deprecated not used
     * @param priceMax TODO Deprecated not used
     * @param variant TODO Deprecated not used
     * @param searchTerm {@link java.lang.String} It is used to get the products by search with CategoryId or ProductId or SkuId.
     * @param facets TODO Deprecated not used
     * @param selectedFacets {@link com.skava.model.pim.MultiFacets} It is used to mention the filter for the products that are available in the requested category. Provided as String with json data encoded. List of available facets for the request will be available in the productlist response with the key \"facets\". Ex:\n"+"\n"+"\n<pre>{\"color\":[{\"name\":\"blue\",\"count\":2}\n"+"\n"+",{\"name\":\"green\",\"count\":1}],\"price\":[{\"name\":\"250\",\"count\":1}],\"Delivery Method\":[{\"name\":\"Courier\",\"count\":1}]}</pre>"+"\n"+". We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response. Ex:\n"+"\n"+"<pre>{\"selectedFacets\":[{\"key\":\"Color\",\"value\":[\"blue\",\"green\"]},\n"+"\n"+"\"{\"key\":\"Delivery Method\",\"value\":[\"Courier\"]}]}</pre>\n"+"\n"+" Here both the Color and Delivery method with the respective value must be available for a product to be listed. If more than one value provided in facet value for a particular key, either one value must be available with the facet key. Ex: Sku/Product should have facet \"Color\" with value as \"blue\" or \"green\". For range filter, values can be provided as \"200~300\"
     * @param curateTerms TODO Deprecated not used
     * @param sort {@link java.lang.String} It is used to apply sorting in the product response. Based on the sortable facets configured in the PIM Admin, sortable facet value will be available in the response, it needs to be set as value for this parameter to apply the sort.
     * @param group TODO Deprecated not used
     * @param responseFormatterClass TODO Deprecated not used
     * @param catalogId {@link java.lang.String} All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here.
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100.
     * @param usev2 TODO Deprecated not used
     * @param edismax TODO Deprecated not used
     * @param iszeroResult TODO Deprecated not used
     * @param spellcheck TODO Deprecated not used
     * @param personalize TODO Deprecated not used
     * @param locale {@link java.lang.String} API Response and error messages will be responded in the locale mentioned in this parameter. Locale needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param previewTime {@link java.lang.String} By this parameter we can simulate the API to respond the Future or Past data. In this parameter we've to send the time in milliseconds format. For Ex: "1514764800000" for 01-Jan-2018 00:00:00..
     * @param contextualParam TODO Deprecated not used
     * @param includeOutOfStock This is a boolean parameter which is used to mention the whether the product that are out of stock needs to be responded or not.
     * @param includeGhostProduct It is boolean parameter to mention that mention that whether products that are invisible needs to be honored while considering the category to respond.
     * @param disableFacetMinCount TODO Deprecated not used
     * @param segments {@link java.lang.String} This parameter represents the segments identifiers for which the API is requested for. User segments are logical group of end users who are grouped together using certain attributes i.e. demographics/membership level/Purchase history. When the user segment(s) is passed the API will fetch the product only if it falls under the subjected segment(s).
     * @return {@link com.skava.model.pim.PimResponse} It returns the Products with PIM response.
     */
     
    @ApiOperation(value = "Get Products", notes = "This service is used to get the products from solr by the given search term. As the product list service this service will also supports pagination, filtering and sorting.Using this API we can directly get the products by CategoryId, ProductId & SkuId.", response = PimResponse.class, protocols = "https")
    @RequestMapping(value = "/{version}/kraft/search/load", method = RequestMethod.GET)
    @ResponseBody
    public PimResponse getProductsForKraft(@ApiParam(value = PimConstants.DEF_REQUEST_PARAM, required = true) HttpServletRequest request,
                                   @ApiParam(value = PimConstants.DEF_RESPONSE_PARAM, required = true) HttpServletResponse response,
                                   @ApiParam(value = PimConstants.DEF_VERSION_PARAM, required = true) @Deprecated @PathVariable String version,
                                   @ApiParam(value = PimConstants.DEF_STOREID_PARAM_REQUIRED, required = true) @RequestParam(value = "storeId", required = true) long storeId,
                                   @ApiParam(value = PimConstants.DEF_SKUID_PARAM, required = false) @RequestParam(value = "skuId", required = false) String[] skuId,
                                   @ApiParam(value = PimConstants.DEF_PRODUCTID_PARAM, required = false) @RequestParam(value = "productId", required = false) String[] productId,
                                   @ApiParam(value = PimConstants.DEF_PRODUCTNAME_PARAM, required = false) @RequestParam(value = "name", required = false) String name,
                                   @ApiParam(value = PimConstants.DEF_PRODUCTBRAND_PARAM, required = false) @RequestParam(value = "brand", required = false) String[] brand,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "category", required = false) String[] categories,
                                   @ApiParam(value = PimConstants.DEF_CATEGORYID_PARAM, required = false) @RequestParam(value = "categoryid", required = false) String[] categoryid,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "categorylevel1", required = false) String categorylevel1,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "categorylevel2", required = false) String categorylevel2,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "categorylevel3", required = false) String categorylevel3,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "categorylevel4", required = false) String categorylevel4,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "categorylevel5", required = false) String categorylevel5,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "division", required = false) String[] division,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "color", required = false) String[] color,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "size1", required = false) String size1,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "size2", required = false) String size2,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "priceMin", required = false, defaultValue = "0.0f") float[] priceMin,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "priceMax", required = false, defaultValue = "0.0f") float[] priceMax,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "variant", required = false) String variant,
                                   @ApiParam(value = PimConstants.DEF_SEARCHTERM_PARAM, required = false) @RequestParam(value = "search", required = false) String searchTerm,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "facets", required = false) String[] facets,
                                   @ApiParam(value = PimConstants.DEF_MULTIFACETS_PARAM, required = false) @RequestParam(value = "selectedFacets", required = false) MultiFacets selectedFacets,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "recCurate", required = false) MultiFacets curateTerms,
                                   @ApiParam(value = PimConstants.DEF_SORT_PARAM, required = false) @RequestParam(value = "sort", required = false) String sort,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "group", required = false) String group,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "responseFormatterClass", required = false, defaultValue = "com.skava.searchv2.resultformatter.ComSolrResultFormatterKraft") String responseFormatterClass,
                                   @ApiParam(value = PimConstants.DEF_CATALOGID_PARAM, required = false) @RequestParam(value = "catalogId", required = false) String catalogId,
                                   @ApiParam(value = PimConstants.DEF_OFFSET_PARAM, required = true) @RequestParam(value = "offset", required = true) int offset,
                                   @ApiParam(value = PimConstants.DEF_LIMIT_PARAM, required = true) @RequestParam(value = "limit", required = true) int limit,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "usev2", required = false) boolean usev2,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "edismax", required = false, defaultValue = "true") boolean edismax,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "iszeroResult", required = false) boolean iszeroResult,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "spellcheck", required = false) boolean spellcheck,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "personalize", required = false) boolean personalize,
                                   @ApiParam(value = PimConstants.DEF_LOCALE_PARAM, required = false) @RequestParam(value = "locale", required = false) String locale,
                                   @ApiParam(value = PimConstants.DEF_PREVIEWTIME_PARAM, required = false) @RequestParam(value = "previewTime", required = false) String previewTime,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "contextualParam", required = false) String contextualParam,
                                   @ApiParam(value = PimConstants.DEF_INCLUDEOUTOFSTOCK_PARAM, required = false) @RequestParam(value = "includeOutOfStock", required = false) boolean includeOutOfStock,
                                   @ApiParam(value = PimConstants.DEF_INCLUDEGHOST_PARAM, required = false) @RequestParam(value = "includeGhostProduct", required = false) boolean includeGhostProduct,
                                   @ApiParam(value = PimConstants.DEF_DEPRECATED, required = false) @RequestParam(value = "disableFacetMinCount", required = false) boolean disableFacetMinCount,
                                   @ApiParam(value = PimConstants.DEF_SEGMENTS_PARAM, required = false) @RequestParam(value = "segment", required = false) String[] segments,
                                   @ApiParam(value = PimKraftConstants.DEF_USERPREFERENCES, required = false) @RequestParam(value = "userPreferences", required = false) MultiFacetsKraft userPreferences,
                                   @ApiParam(value = PimKraftConstants.DEF_SIMILAR_SEARCH, required = false) @RequestParam(value = "similarType", required = false) String similarType,
                                   @ApiParam(value = PimKraftConstants.DEF_DISABLEDEFAULTSORT_PARAM, defaultValue="false", required = false) @RequestParam(value = "disableDefaultSort", defaultValue="false", required = false) boolean disableDefaultSort,
                                   @ApiParam(value = PimKraftConstants.DEF_SPELLCHECK_ONLY_PARAM, defaultValue="false", required = false) @RequestParam(value = "spellCheckOnly", defaultValue="false", required = false) boolean spellCheckOnly)

    {
        PimResponse toRet = new PimResponse(Response.RESPONSE_FAILED, Response.RESPONSE_MSG_FAILURE);
        try
        {
            toRet = ((PimServiceKraft) skavaKraftContextFactory.get(TenantThreadLocal.get(), PimServiceKraftBuilder.PIMSERVICEKRAFT)).getProductsKraft(request, response, storeId, skuId, productId, name, brand, categories, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, color, size1, size2, priceMin, priceMax, variant, searchTerm, facets, selectedFacets, sort, group, responseFormatterClass, catalogId, offset, limit, usev2, edismax, iszeroResult, spellcheck, personalize, locale, previewTime, contextualParam, includeOutOfStock, includeGhostProduct, disableFacetMinCount, segments, userPreferences, similarType, disableDefaultSort, spellCheckOnly);
        }
        catch (ServerException se)
        {
            logger.info("sending error response", se);
            toRet.setResponseCode(Response.RESPONSE_FAILED);
            toRet.setResponseMessage(se.getMessage());
        }
        return toRet;
    }
}
