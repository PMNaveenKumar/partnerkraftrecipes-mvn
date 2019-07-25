package com.skava.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.skava.model.pim.MultiFacets;
import com.skava.model.pim.MultiFacetsKraft;
import com.skava.model.pim.PimResponse;
import com.skava.util.ServerException;
import com.skava.util.helpers.MethodInfo;

public interface PimServiceKraft
{
    @MethodInfo(params = { "request", "response", "storeId", "categories", "locale", "selectedFacets", "responseFormatterClass", "sort", "previewTime", "offset", "limit", "contextualParam", "catalogId", "disableFacetMinCount", "includeOutOfStock", "includeGhostProduct", "skipPromo", "skipInventory", "segments", "userPreferences", "disableDefaultSort" })
    public PimResponse getProductListFromSolrKraft(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   long storeId,
                                                   String categoryId,
                                                   String locale,
                                                   MultiFacets selectedFacets,
                                                   String responseFormatterClass,
                                                   String sort,
                                                   String previewTime,
                                                   int offset,
                                                   int limit,
                                                   String contextualParam,
                                                   String catalogId,
                                                   boolean disableFacetMinCount,
                                                   boolean includeOutOfStock,
                                                   boolean includeGhostProduct,
                                                   boolean skipPromo,
                                                   boolean skipInventory,
                                                   String[] segments,
                                                   MultiFacetsKraft userPreferences,
                                                   boolean disableDefaultSort) throws ServerException;

    @MethodInfo(params = { "request", "response", "storeId", "skuId", "productId", "name", "brand", "categories", "categoryids", "categorylevel1", "categorylevel2", "categorylevel3", "categorylevel4", "categorylevel5", "division", "color", "size1", "size2", "priceMin", "priceMax", "variant", "searchTerm", "facets", "selectedFacets", "sort", "group", "responseFormatterClass", "catalogId", "offset", "limit", "usev2", "edismax", "iszeroResult", "spellcheck", "personalize", "locale", "previewTime", "contextualParam", "includeOutOfStock", "includeGhostProduct", "disableFacetMinCount", "segments", "userPreferences", "similarFields", "disableDefaultSort", "spellCheckOnly" })
    public PimResponse getProductsKraft(HttpServletRequest request,
                                        HttpServletResponse response,
                                        long storeId,
                                        String[] skuId,
                                        String[] productId,
                                        String name,
                                        String[] brand,
                                        String[] categories,
                                        String[] categoryids,
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
                                        MultiFacets selectedFacets,
                                        String sort,
                                        String group,
                                        String responseFormatterClass,
                                        String catalogId,
                                        int offset,
                                        int limit,
                                        boolean usev2,
                                        boolean edismax,
                                        boolean iszeroResult,
                                        boolean spellcheck,
                                        boolean personalize,
                                        String locale,
                                        String previewTime,
                                        String contextualParam,
                                        boolean includeOutOfStock,
                                        boolean includeGhostProduct,
                                        boolean disableFacetMinCount,
                                        String[] segments,
                                        MultiFacetsKraft userPreferences,
                                        String similarType,
                                        boolean disableDefaultSort,
                                        boolean spellCheckOnly) throws ServerException;

}
