package com.skava.web.search;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.model.Response;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.SelectedFacet;
import com.skava.model.pim.SelectedKraftFacet;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.helpers.CustomWebEditor;
import lombok.Getter;

@Api(tags = { "Skava Search Micro Services" }, description = "Skava Search Services - API", protocols = "https")
@Controller
public class StreamSearchV2KraftController
{
    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());
    @Autowired @Getter private SkavaTenantContextFactory skavaKraftContextFactory;

    /**
     * This search API loads suggestions list from solr(http://lucene.apache.org/solr/) based on the given query parameters. This API supports pagination. Using this API we can perform load search suggestions by region and store Id. Suggestions can be rertived from single suggestion file or multiple suggestion files
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param partner Partner is an entity which refer actuall customer. A customer can have multiple Campaigns. Partner name associated with the partner entity goes here.
     * @param version It hold the value of version number of API. Using this parameter we can access the different version of the API. It is currently unused.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter  for all the microservice.
     * @param searchTerm This parameter takes the search term as value which used to perform general search. Based on this search term value the suggestions list will be displayed
     * @param view This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 10.
     * @param region Suggestions will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param catalogId All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here.
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @ApiOperation(value = "Suggest Group", notes = "This search API loads suggestions list from solr(http://lucene.apache.org/solr/) based on the given query parameters. This API supports pagination. Using this API we can perform load search suggestions by region and store Id. Suggestions can be rertived from single suggestion file or multiple suggestion files", protocols = "https", response = Response.class)
    @RequestMapping(value = "/search/{version}/kraft/getGroupSuggestion", method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody Response getGroupSuggestion(HttpServletRequest request,
                                                     @ApiParam(value = "It hold the value of version number of API. Using this parameter we can access the different version of the API. It is currently unused.", required = true) @PathVariable String version,
                                                     @ApiParam(value = "In all the microservice the configuration parameters of the APIs are maintained in an entity called Campaign. We can customize the functionality of the API using the campaign properties. Campaign Id is a mandatory parameter  for all the microservice.", required = true) @RequestParam(value = "storeId", required = true) long storeId,
                                                     @ApiParam(value = "This parameter takes the search term as value which used to perform general search. Based on this search term value the suggestions list will be displayed", required = false) @RequestParam(value = "searchterm", required = true) String searchTerm,
                                                     @ApiParam(value = "This parameter accepts the result formatter java class name which is used to format the solr response based on the java model", required = false) @RequestParam(value = "view", required = false, defaultValue = "com.skava.util.PropertyGroupResultFormatter") String view,
                                                     @ApiParam(value = "This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 10.", required = true) @RequestParam(value = "grouplimit", required = false) int groupLimit,
                                                     @ApiParam(value = "Suggestions will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).", required = false) @RequestParam(value = "region", required = false) String region,
                                                     @ApiParam(value = "This is a boolean parameter which is used to mention the whether needs to enable the Extended DisMax query parser or not", required = false) @RequestParam(value = "edismax", required = false, defaultValue = "true") boolean edismax, //Refer web: its common solr feature. Please refer web. 
                                                     @ApiParam(value = "All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here.", required = false) @RequestParam(value = "catalogId", required = false) String catalogId,
                                                     @ApiParam(value = "User facets which are available in user profile", required = false) @RequestParam(value = "userPreferences", required = false) MultiFacetsKraft userPreferences,
                                                     @ApiParam(value = "It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.", required = false) @RequestParam(value = "selectedFacets", required = false) SelectedKraftFacet[] selectedFacets) throws ServerException //Refer PIM services
    {
        return ((com.skava.services.StreamSearchV2KraftService) skavaKraftContextFactory.get(com.skava.model.TenantThreadLocal.get(), com.skava.builder.interfaces.StreamSearchV2KraftServiceBuilder.STREAMSEARCHV2KRAFTSERVICE)).getGroupSuggestion(request, storeId, searchTerm, view, region, userPreferences, edismax, groupLimit, catalogId, selectedFacets);
    }
    
    
    /**
     * This search API loads suggestions list from solr(http://lucene.apache.org/solr/) based on the given query parameters. This API supports pagination. Using this API we can perform load search suggestions by region and store Id. Suggestions can be rertived from single suggestion file or multiple suggestion files
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param version It hold the value of version number of API. Using this parameter we can access the different version of the API. It is currently unused.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter  for all the microservice.
     * @param searchTerm This parameter takes the search term as value which used to perform general search. Based on this search term value the suggestions list will be displayed
     * @param view This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 10.
     * @param region Suggestions will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @ApiOperation(value = "Suggest dict", notes = "This search API loads suggestions list from solr(http://lucene.apache.org/solr/) based on the given query parameters. This API supports pagination. Using this API we can perform load search suggestions by region and store Id. Suggestions can be rertived from single suggestion file or multiple suggestion files", protocols = "https", response = Response.class)
    @RequestMapping(value = "/search/{version}/kraft/suggestdict" , method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody Response suggestDict(HttpServletRequest request,
                                              @ApiParam(value="It hold the value of version number of API. Using this parameter we can access the different version of the API. It is currently unused.", required=true) @PathVariable String version,
                                              @ApiParam(value="In all the microservice the configuration parameters of the APIs are maintained in an entity called Campaign. We can customize the functionality of the API using the campaign properties. Campaign Id is a mandatory parameter  for all the microservice.", required=true) @RequestParam(value = "storeId", required = true) long storeId,
                                              @ApiParam(value="This parameter takes the search term as value which used to perform general search. Based on this search term value the suggestions list will be displayed", required=false) @RequestParam(value = "searchterm", required = true) String searchTerm,
                                              @ApiParam(value="This parameter accepts the result formatter java class name which is used to format the solr response based on the java model", required=false) @RequestParam(value = "view", required = false, defaultValue = "com.skava.util.PropertySuggestFormatter") String view,
                                              @ApiParam(value="This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.", required=false) @RequestParam(value = "offset", required = false) int offset,
                                              @ApiParam(value="This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 10.", required=false) @RequestParam(value = "limit", required = false) int limit,
                                              @ApiParam(value="Suggestions will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).", required=false) @RequestParam(value = "region", required = false) String region) throws ServerException //Refer PIM services
    {
        return ((com.skava.services.StreamSearchV2KraftService) skavaKraftContextFactory.get(com.skava.model.TenantThreadLocal.get(), com.skava.builder.interfaces.StreamSearchV2KraftServiceBuilder.STREAMSEARCHV2KRAFTSERVICE)).suggestDict(request, storeId, searchTerm, view, offset, limit, region);
    }

    
    //Product Catalog
    /**
     * Search service have a defined solr schema to maintain the Product information in solr. We've feed processor to index the Product data in to the solr as per the solr schema.
     * Using this API we can retrieve the indexed products from solr. This API supports pagination, filtering and sorting. Using this API we can perform open search, search by product name, color, size, category name and price.This API supports contextual search as well.
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param response To manipulate the HTTP protocol specified header information and return data for client.
     * @param partner Partner is an entity which refer actuall customer. A customer can have multiple Campaigns. Partner name associated with the partner entity goes here.
     * @param version It hold the value of version number of API. Using this parameter we can access the different version of the API. It is currently unused.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter for all the microservice.
     * @param skuId Skus that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated sku information are responded
     * @param productid Products that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated product information are responded.
     * @param name Product name refer to name of the product which is indexed in solr needs to be fetched
     * @param brand Brand name refer to brand name of the product which is indexed in solr needs to be fetched
     * @param category Categories that are product group will have category name. Based on the mentioned category name, indexed products are responded from solr
     * @param categoryid Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded
     * @param variant This parameter accepts the parameter which is making different from other products like special tags
     * @param facets This parameter accepts the array of facets 0-th value as true and the remaining value contains facets which groups all the facets and filter the products based on the given facets to respond
     * @param selectedFacets It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.
     * @param curateTerms This parameter used to give boost for particular key in dynamic query time
     * @param multiFacets This parameter used to pass the selectedFacets as a single jsonarray instead of multiple parameters  Sample : selectedFacetsv2={\"selectedFacets\":[{\"key\":\"key\",\"value\":[\"value\"]},{\"key\":\"key\",\"value\":[\"value\"]}]}
     * @param sort It is used to apply sorting for the products response from solr based on the particular field. Example: price|desc. This sorts the products with indexed field price in descending order
     * @param group This parameter used to group the products based on the given query parameter values
     * @param searchTerm This parameter takes the search term as value which used to perform general search. The products with the search term matched with any indexed field will be responded
     * @param view the This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100
     * @param idxboost This is a boolean parameter which is used to mention the whether needs to enable index based boosting or not. By default solr using query based boosting
     * @param edismax This is a boolean parameter which is used to mention the whether needs to enable the Extended DisMax query parser or not
     * @param spellcheck This is a boolean parameter which is used to mention whether the check spelling is needed or not
     * @param personalize In case of searching products based on personalized options. Example:- If a user searched the products based on giving search terms and product id and that particular user has red color as favorite one which will load the read color products
     * @param contextualParam Specifies a query to load the exact products
     * @param region Products will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param publishVersion This parameter accepts the publish version
     * @param online This is a boolean parameter which is used to mention the whether the online products will be responded or not
     * @param store This is a boolean parameter which is used to mention the whether the store based products will be responded or not
     * @param advancedSearch This parameter accepts the advanced key search in case of searching dynamic indexed fields
     * @param disableMinCountFacet This parameter used to retain the filter options even after applying the filters. The facet options won't change even after the filter applied.
     * @param isVisible This is a boolean parameter which is used to mention the whether the invisible products will be responded or not
     * @param includeFacet This parameter used to add additional filterable facet
     * @param disableFacetLimit The solr gives the minimum 100 filter options. This parameter used to disable the limit of solr filter option restriction
     * @param segments It refers to user segments, by this the products that are associated to the requested segments alone will be considered for the products to respond
     * @param catalogId All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @ApiOperation(value = "Get products", notes = "Search service have a defined solr schema to maintain the Product information in solr. We've feed processor to index the Product data in to the solr as per the solr schema.Using this API we can retrieve the indexed products from solr. This API supports pagination, filtering and sorting. Using this API we can perform open search, search by product name, color, size, category name and price.This API supports contextual search as well.", protocols = "https", response = Response.class)
    @RequestMapping(value = "/search/{version}/kraft/getProductsforKraft" , method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody Response getProducts(HttpServletRequest request,
                                              HttpServletResponse response,
                                              @ApiParam(value="It hold the value of version number of API. Using this parameter we can access the different version of the API. It is currently unused.", required=true) @PathVariable String version,
                                              @ApiParam(value="In all the microservice the configuration parameters of the APIs are maintained in an entity called Campaign. We can customize the functionality of the API using the campaign properties. Campaign Id is a mandatory parameter for all the microservice.", required=true) @RequestParam(value = "storeId", required = true) long storeId,
                                              @ApiParam(value="Skus that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated sku information are responded", required=false) @RequestParam(value = "skuid", required = false) String[] skuId,
                                              @ApiParam(value="Products that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated product information are responded.", required=false) @RequestParam(value = "productid", required = false) String[] productid,
                                              @ApiParam(value="Product name refer to name of the product which is indexed in solr needs to be fetched", required=false) @RequestParam(value = "name", required = false) String name,
                                              @ApiParam(value="Brand name refer to brand name of the product which is indexed in solr needs to be fetched", required=false) @RequestParam(value = "brand", required = false) String[] brand,
                                              @ApiParam(value="Categories that are product group will have category name. Based on the mentioned category name, indexed products are responded from solr", required=false) @RequestParam(value = "category", required = false) String[] category,
                                              @ApiParam(value="Categories that are configured in the PIM Admin will have unique identifier. Based on the mentioned identifier, associated category information and its sub categories that have products are responded", required=false) @RequestParam(value = "categoryid", required = false) String[] categoryid,
                                              @ApiParam(value="This parameter accepts the array of facets 0-th value as true and the remaining value contains facets which groups all the facets and filter the products based on the given facets to respond", required=false) @RequestParam(value = "facets", required = false) String[] facets,
                                              @ApiParam(value="It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.", required=false) @RequestParam(value = "selectedFacets", required = false) SelectedKraftFacet[] selectedFacets, //in use
                                              @ApiParam(value="It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.", required=false) @RequestParam(value = "customFacets", required = false) SelectedKraftFacet[] customFacets, //in use
                                              @ApiParam(value="This parameter used to pass the selectedFacets as a single jsonarray instead of multiple parameters  Sample : selectedFacetsv2={\"selectedFacets\":[{\"key\":\"key\",\"value\":[\"value\"]},{\"key\":\"key\",\"value\":[\"value\"]}]}", required=false) @RequestParam(value = "selectedFacetsv2", required = false) MultiFacetsKraft multiFacets, // it is added for Merchandize and finder admin project. to pass the selectedFacets as a single jsonarray instead of multiple parameters  Sample : selectedFacetsv2={"selectedFacets":[{"key":"key","value":["value"]},{"key":"key","value":["value"]}]}  TODO:MERCHANDIZEDONE
                                              @ApiParam(value="It is used to apply sorting for the products response from solr based on the particular field. Example: price|desc. This sorts the products with indexed field price in descending order", required=false) @RequestParam(value = "sort", required = false) String sort,//price|desc in use
                                              @ApiParam(value="This parameter used to group the products based on the given query parameter values", required=false) @RequestParam(value = "group", required = false) String group, //default product id, category, based indexed field
                                              @ApiParam(value="This parameter takes the search term as value which used to perform general search. The products with the search term matched with any indexed field will be responded", required=false) @RequestParam(value = "search", required = false) String searchTerm, // matched general search matched all params  in use
                                              @ApiParam(value="This parameter accepts the result formatter java class name which is used to format the solr response based on the java model", required=false) @RequestParam(value = "view", required = false, defaultValue = "com.skava.util.PropertyResultFormatter") String view,// result formatter
                                              @ApiParam(value="This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.", required=false) @RequestParam(value = "offset", required = false) int offset,
                                              @ApiParam(value="This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request.", required=false) @RequestParam(value = "limit", required = false) int limit,
                                              @ApiParam(value="This is a boolean parameter which is used to mention the whether needs to enable index based boosting or not. By default solr using query based boosting", required=false) @RequestParam(value = "idxboost", required = false) boolean idxboost, // index based boost..query based boost
                                              @ApiParam(value="This is a boolean parameter which is used to mention the whether needs to enable the Extended DisMax query parser or not", required=false) @RequestParam(value = "edismax", required = false, defaultValue = "true") boolean edismax, //Refer web: its common solr feature. Please refer web. 
                                              @ApiParam(value="This is a boolean parameter which is used to mention whether the check spelling is needed or not", required=false) @RequestParam(value = "spellcheck", required = false, defaultValue = "false") boolean spellcheck,
                                              @ApiParam(value="In case of searching products based on personalized options. Example:- If a user searched the products based on giving search terms and product id and that particular user has red color as favorite one which will load the read color products", required=false) @RequestParam(value = "personalize", required = false) boolean personalize,//TODO:/*If search term is given like additas addtionally product id is given. Example: favorite color red user specific means we are searching red additas pdt*/ 
                                              @ApiParam(value="Products will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).", required=false) @RequestParam(value = "region", required = false) String region, // like locale
                                              @ApiParam(value="This parameter accepts the publish version", required=false) @RequestParam(value = "publishVersion", required = false) String publishVersion, // Same as PIM publish version
                                              @ApiParam(value="This is a boolean parameter which is used to mention the whether the online products will be responded or not", required=false) @RequestParam(value = "online", required = false) boolean online, //whether onli pdt or store prdts 
                                              @ApiParam(value="This is a boolean parameter which is used to mention the whether the store based products will be responded or not", required=false) @RequestParam(value = "store", required = false) boolean store, ///whether onli pdt or store prdts
                                              @ApiParam(value="This is a boolean parameter which is used to mention the whether the invisible products will be responded or not", required=false) @RequestParam(value = "isVisible", required = false, defaultValue = "true") boolean isVisible,//Refer PIM services
                                              @ApiParam(value="All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here.", required=false) @RequestParam(value = "catalogId", required = false) String catalogId,
                                              @ApiParam(value="Key to fetch similar recipes of the given productid", required=false) @RequestParam(value = "similarType", required = false) String similarType) throws ServerException //Refer PIM services
    {
        SelectedFacet[] curate = null;
        String methodName = "getProducts";
        //logger.writeLog(Level.DEBUG, SearchConstants.EVENTID_GETPRODUCTS, "", this.getClass().getSimpleName(), methodName, 0, null, null, " SkavaSearchService - {} :  request - {}, storeId - {}, skuId - {}, productId - {}, name - {}, brand - {}, category - {}, categoryid - {}, categorylevel1 - {}, categorylevel2 - {}, categorylevel3 - {}, categorylevel4 - {}, categorylevel5 - {}, division - {}, color - {}, size1 - {}, size2 - {}, priceMin - {}, priceMax - {}, variant - {}, searchTerm - {}, facets - {}, selectedFacets - {}, sort - {}, group - {}, responseFormatterClass - {}, offset - {}, limit - {}, usev2 - {}, edismax - {}, iszeroResult - {}, spellcheck - {}, personalize - {}, contextualParam - {}, region - {}, version - {}, curate - {}, online - {}, store - {}, advancedSearch - {}, disableFacetMinCount - {}, includeGhostProduct - {}, includeFacet - {}, disableFacetLimit - {}, segments - {}, catalogId - {}", null, false, null,  this.getClass().getSimpleName(), request, storeId, skuId, productid, name, brand, category, categoryid, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, null, color, size1, size2, priceMin, priceMax, variant, searchTerm, facets, selectedFacets, sort, group, view, offset, limit, idxboost, edismax, false, spellcheck, personalize, contextualParam, region, version, curate, online, store, advancedSearch, disableMinCountFacet, isVisible, includeFacet, disableFacetLimit, segments, catalogId);
        if (multiFacets != null && selectedFacets == null)
        {
            selectedFacets = multiFacets.getSelectedFacets();
        }
       
        return ((com.skava.services.StreamSearchV2KraftService) skavaKraftContextFactory.get(com.skava.model.TenantThreadLocal.get(), com.skava.builder.interfaces.StreamSearchV2KraftServiceBuilder.STREAMSEARCHV2KRAFTSERVICE)).getProductsForKraft(request, storeId, skuId, productid, name, brand, category, categoryid, null, null, null, null, null, null, null, null, null, null, null, null, searchTerm, facets, selectedFacets, customFacets, sort, group, view, offset, limit, idxboost, edismax, false, spellcheck, personalize, null, region, publishVersion, null, online, store, null, false, isVisible, null, false, null, catalogId, similarType, false);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(SelectedKraftFacet.class, new com.skava.util.helpers.CustomWebEditor<SelectedKraftFacet>(SelectedKraftFacet.class));
        binder.registerCustomEditor(MultiFacetsKraft.class, new CustomWebEditor<MultiFacetsKraft>(MultiFacetsKraft.class));
        binder.registerCustomEditor(SelectedFacet.class, new CustomWebEditor<SelectedFacet>(SelectedFacet.class));
    }
}
