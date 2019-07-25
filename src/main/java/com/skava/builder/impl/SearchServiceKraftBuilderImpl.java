
package com.skava.builder.impl;

import com.skava.builder.interfaces.SearchServiceKraftBuilder;
import com.skava.interfaces.SearchService;
import com.skava.model.Tenant;
import com.skava.search.service.SearchServiceSolrImpl;
import com.skava.search.service.SearchServiceSolrImplKraft;
import com.skava.util.ServerException;

public class SearchServiceKraftBuilderImpl implements SearchServiceKraftBuilder
{
    @Override
    public SearchService getSearchKraftService(Tenant tenant) throws ServerException
    {
        SearchService searchService = null;
        try
        {
            searchService = new SearchServiceSolrImplKraft();
        }
        catch (Exception e)
        {
            throw new ServerException(e);
        }

        return searchService;
    }
}
