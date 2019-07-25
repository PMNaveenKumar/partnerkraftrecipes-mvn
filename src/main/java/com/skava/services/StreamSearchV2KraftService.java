package com.skava.services;

import javax.servlet.http.HttpServletRequest;

import com.skava.model.Response;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.SelectedFacet;
import com.skava.model.pim.SelectedKraftFacet;
import com.skava.util.ServerException;
import com.skava.util.helpers.MethodInfo;

public interface StreamSearchV2KraftService
{
    public static final int MAX_LISTNAME_LENGTH = 1024;
    public static final int MAX_DEFAULT_UPDATE_LASTVIEWEDTIME = 300000; //5 Secs
    public static final int NUM_ITEMS_PER_LIST = 50;
    public static final String DEFAULT_SORT = "createdtime desc";
    public static final String FULFILLMENT_TYPE = "fulfillment";
    public static final String NOTIFICATION_TYPE = "notification";
    public static final String SEARCH_LUKE_CACHE = "SearchLukeCache";
    public static final String LIST_LUKE_CACHE = "ListLukeCache";
    public static final String DEFAULT_RESULT_FORMATTER_CLASS = "com.skava.util.PropertyResultFormatter";

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
    @MethodInfo(params = { "request", "storeId", "skuId", "productId", "name", "brand", "category", "categoryid", "categorylevel1", "categorylevel2", "categorylevel3", "categorylevel4", "categorylevel5", "division", "color", "size1", "size2", "priceMin", "priceMax", "variant", "searchTerm", "facets", "selectedFacets", "customFacets", "sort", "group", "responseFormatterClass", "offset", "limit", "usev2", "edismax", "iszeroResult", "spellcheck", "personalize", "contextualParam", "region", "version", "curate", "online", "store", "advancedSearch", "disableFacetMinCount", "includeGhostProduct", "includeFacet", "disableFacetLimit", "segments", "catalogId", "similarType", "spellCheckOnly" })
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
                                        boolean spellCheckOnly) throws ServerException;

    /**
     * Search service have a defined solr schema to maintain the Product information in solr. We've feed processor to index the Product data in to the solr as per the solr schema.
       Using this API we can retrieve the indexed products from solr and data grouped. This API supports filtering and sorting. Using this API we can perform open search, search by product name, color, size, category name and price.
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter for all the microservice.
     * @param searchTerm This parameter takes the search term as value which used to perform general search. The products with the search term matched with any indexed field will be responded
     * @param selectedFacets It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.
     * @param responseFormatterClass This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param group limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 100.
     * @param edismax This is a boolean parameter which is used to mention the whether needs to enable the Extended DisMax query parser or not
     * @param region Products will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @param selectedFacets It is used to mention the filter for the products that are available in the requested category. It have a json format.  selectedFacets ={ key : color , value :[ blue ]}. We can add more filter by adding further objects to the selectedFacets array. Applicable filter can be retrieved from the response.
     * @param catalogId All the micro services functionality can be customized in catalog level. Campaign can have multiple catalogs. Each catalog can have different set of properties that will influence the functionality of the API. Unique identifier associated for the catalog will go here
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @MethodInfo(params = { "request", "storeId", "searchTerm", "responseFormatterClass", "region", "userPreferences", "edismax", "groupLimit", "catalogId", "selectedFacets" })
    Response getGroupSuggestion(HttpServletRequest request,
                                long storeId,
                                String searchTerm,
                                String responseFormatterClass,
                                String region,
                                MultiFacetsKraft userPreferences,
                                boolean edismax,
                                int groupLimit,
                                String catalogId,
                                SelectedKraftFacet[] selectedFacets) throws ServerException;

    /**
     * This search API loads suggestions list from solr(http://lucene.apache.org/solr/) based on the given query parameters. This API supports pagination. Using this API we can perform load search suggestions by region and store Id. Suggestions can be rertived from single suggestion file or multiple suggestion files
     *
     * @param request It contains the information of request for HTTP servlets.
     * @param storeId In all the microservice the configuration parameters of the APIs are maintained in an entity called Store. We can customize the functionality of the API using the store properties. Store Id is a mandatory parameter  for all the microservice.
     * @param searchTerm This parameter takes the search term as value which used to perform general search. Based on this search term value the suggestions list will be displayed
     * @param responseFormatterClass This parameter accepts the result formatter java class name which is used to format the solr response based on the java model
     * @param offset This parameter will be available for all the services that supports pagination. This parameter is used to mention the starting index of the items which going to responded by the API.
     * @param limit This parameter will be available for all the services that supports pagination. This parameter is used to mention the number of maximum items that needs to be responded for the request. This parameter have a internal max limit of 10.
     * @param region Suggestions will be responded in the region mentioned in this parameter. Region needs to be mentioned in Java standard locale format(http://www.oracle.com/technetwork/java/javase/java8locales-2095355.html).
     * @return It is a response model class for Skava Search APIs which delivered as a json. It will be responded with requested item informations.
     * @throws ServerException A platform wrapper class for java Exception. It will be triggered for all the exception thrown with in this function.
     */
    @MethodInfo(params = { "request", "storeId", "searchTerm", "responseFormatterClass", "offset", "limit", "region" })
    public Response suggestDict(HttpServletRequest request,
                                long storeId,
                                String searchTerm,
                                String responseFormatterClass,
                                int offset,
                                int limit,
                                String region) throws ServerException;
}
