
package com.skava.builder.impl;

import java.util.HashMap;
import java.util.Map;

import com.skava.builder.interfaces.HttpClientServiceBuilder;
import com.skava.builder.interfaces.JMQServiceBuilder;
import com.skava.builder.interfaces.SearchSynonymServiceBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.StreamSearchKraftServiceBuilder;
import com.skava.builder.interfaces.StreamSearchServiceBuilder;
import com.skava.builder.interfaces.StreamSearchV2KraftServiceBuilder;
import com.skava.builder.remote.interfaces.StreamCatalogRemoteServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.db.DBSessionManager;
import com.skava.model.Tenant;
import com.skava.searchv2.StreamSearchV2ServiceImplKraft;
import com.skava.services.HttpClientService;
import com.skava.services.JMQService;
import com.skava.services.SearchSynonymService;
import com.skava.services.StreamCatalogService;
import com.skava.services.StreamSearchKraftService;
import com.skava.services.StreamSearchService;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;

public class StreamSearchV2KraftServiceBuilderImpl implements StreamSearchV2KraftServiceBuilder
{
    private Map<String, StreamSearchV2KraftService> streamSearchV2ServiceMap = new HashMap<>();

    private String searchQueueName;
    private String solrCloudMode;
    private String lukeFromDb;

    StreamSearchV2KraftServiceBuilderImpl(String searchQueueName,
                                          String solrCloudMode,
                                          String lukeFromDb)
    {
        this.searchQueueName = searchQueueName;
        this.solrCloudMode = solrCloudMode;
        this.lukeFromDb = lukeFromDb;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StreamSearchV2KraftService getStreamSearchV2KraftService(Tenant tenant,
                                                                    SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException
    {
        StreamSearchV2KraftService streamSearchV2Service = getStreamSearchV2ServiceFromMap(tenant);
        if (streamSearchV2Service == null)
        {
            synchronized (this)
            {
                streamSearchV2Service = getStreamSearchV2ServiceFromMap(tenant);
                if (streamSearchV2Service == null)
                {
                    try
                    {
                        DBSessionManager dbSessionManager = (DBSessionManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.DBSESSIONMANAGER);
                        StreamSearchKraftService streamSearchKraftService = (StreamSearchKraftService) skavaTenantContextFactory.get(tenant, StreamSearchKraftServiceBuilder.STREAMSEARCHKRAFTSERVICE);
                        StreamSearchService streamSearchService = (StreamSearchService) skavaTenantContextFactory.get(tenant, StreamSearchServiceBuilder.STREAMSEARCHSERVICE);
                        SearchSynonymService searchSynonymService = (SearchSynonymService) skavaTenantContextFactory.get(tenant, SearchSynonymServiceBuilder.SEARCH_SYNONYM_SERVICE);
                        StreamCatalogService streamCatalogService = (StreamCatalogService) skavaTenantContextFactory.get(tenant, StreamCatalogRemoteServiceBuilder.STREAM_CATALOG_REMOTE_SERVICE);
                        JMQService jmqService = (JMQService) skavaTenantContextFactory.get(tenant, JMQServiceBuilder.JMQSERVICE);
                        String searchQueueNameValue = ConfigManagerInstance.get(tenant, this.searchQueueName);
                        boolean lukeFromDbValue = ReadUtil.getBoolean(ConfigManagerInstance.get(tenant, this.lukeFromDb), false);
                        boolean solrCloudModeValue = ReadUtil.getBoolean(ConfigManagerInstance.get(tenant, this.solrCloudMode), false);
                        HttpClientService httpClientService = (HttpClientService) skavaTenantContextFactory.get(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
                        MemCacheManager memCacheManager = (MemCacheManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                        streamSearchV2Service = new StreamSearchV2ServiceImplKraft(dbSessionManager, streamSearchKraftService, streamSearchService, searchSynonymService, streamCatalogService, jmqService, searchQueueNameValue, solrCloudModeValue, httpClientService, memCacheManager, lukeFromDbValue);
                        streamSearchV2ServiceMap.put(tenant.getId(), streamSearchV2Service);
                    }
                    catch (ServerException se)
                    {
                        throw (ServerException) se;
                    }
                    catch (Exception e)
                    {
                        throw new ServerException(e);
                    }
                }
            }
        }

        return streamSearchV2Service;
    }

    private StreamSearchV2KraftService getStreamSearchV2ServiceFromMap(Tenant tenant)
    {
        return streamSearchV2ServiceMap.get(tenant.getId());
    }
}
