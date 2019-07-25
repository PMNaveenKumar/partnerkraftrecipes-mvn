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
package com.skava.services;

import com.skava.model.Response;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.searchv2.StreamSearchConfig;
import com.skava.model.searchv2.StreamSearchQuery;
import com.skava.model.searchv2.StreamSearchResponse;
import com.skava.util.ServerException;
import com.skava.util.helpers.MethodInfo;

public interface StreamSearchKraftService
{
    public static final int SEARCH_DOMAIN_BASIC_SEARCH = 1;
    public static final int SEARCH_DOMAIN_EVENT = 2;
    public static final int SEARCH_DOMAIN_PRODUCT = 3;
    public static final int SEARCH_DOMAIN_ANALYTICS = 4;
    public static final int SEARCH_DOMAIN_LIST = 5;
    public static final int SEARCH_DOMAIN_LISTITEM = 6;
    public static final int SEARCH_DOMAIN_LISTITEM_COUNT = 7;
    public static final int SEARCH_DOMAIN_LISTITEMTAG_COUNT = 8;
    public static final int SEARCH_DOMAIN_LISTTAG_COUNT = 9;
    public static final int SEARCH_DOMAIN_UPDATE_LASTVIWEDTIME = 10;
    public static final int SEARCH_DOMAIN_BANK = 11;
    public static final int SEARCH_DOMAIN_PINCODE = 12;

    @MethodInfo(params = { "serverName", "campaign", "searchDomainType", "searchDomainVerison", "query", "image", "imageField", "sort", "group", "facets", "offset", "limit", "config", "isSpellCheck", "searchTerm", "contextualParam", "origContextualParam", "disableFacetMinCount", "disableFacetLimit", "mlt" })
    public StreamSearchResponse doSearch(String serverName,
                                         Campaign campaign,
                                         int searchDomainType,
                                         String searchDomainVerison,
                                         StreamSearchQuery query,
                                         String image,
                                         String[] imageField,
                                         String sort,
                                         String group,
                                         String[] facets,
                                         int offset,
                                         int limit,
                                         StreamSearchConfig config,
                                         boolean isSpellCheck,
                                         String searchTerm,
                                         String contextualParam,
                                         String origContextualParam,
                                         boolean disableFacetMinCount,
                                         boolean disableFacetLimit,
                                         boolean mlt) throws ServerException;

    @MethodInfo(params = {"serverName", "campaign", "searchDomainType", "searchDomainVerison", "searchTerm", "responseFormatterClass", "offset", "limit", "region", "catalogId", "storeId"})
    public Response doSuggestDict(String serverName,
                                  Campaign campaign,
                                  int searchDomainType,
                                  String searchDomainVerison,
                                  String searchTerm,
                                  String responseFormatterClass,
                                  int offset,
                                  int limit,
                                  String region,
                                  String catalogId,
                                  long storeId);
}
