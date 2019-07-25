package com.skava.builder.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.skava.builder.interfaces.ActiveMQConnectionFactoryServiceBuilder;
import com.skava.builder.interfaces.AnalyticsListnerServiceBuilder;
import com.skava.builder.interfaces.ApiTokenPropertiesServiceBuilder;
import com.skava.builder.interfaces.DBSessionManagerServiceBuilder;
import com.skava.builder.interfaces.HttpClientServiceBuilder;
import com.skava.builder.interfaces.IndexListenerServiceBuilder;
import com.skava.builder.interfaces.JMQServiceBuilder;
import com.skava.builder.interfaces.MemCacheManagerServiceBuilder;
import com.skava.builder.interfaces.PartnerListenerServiceBuilder;
import com.skava.builder.interfaces.ScaleApiDriverForListenerServiceBuilder;
import com.skava.builder.interfaces.ScaleMessageListenerServiceBuilder;
import com.skava.builder.interfaces.SearchIndexListenerServiceBuilder;
import com.skava.builder.interfaces.SearchServiceKraftBuilder;
import com.skava.builder.interfaces.SearchServiceBuilder;
import com.skava.builder.interfaces.SearchSynonymServiceBuilder;
import com.skava.builder.interfaces.SkavaLoggerServiceBuilder;
import com.skava.builder.interfaces.SkavaResourceBundleServiceBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.StreamJMQFactoryServiceBuilder;
import com.skava.builder.interfaces.StreamSearchKraftServiceBuilder;
import com.skava.builder.interfaces.StreamSearchServiceBuilder;
import com.skava.builder.interfaces.StreamSearchKraftServiceBuilder;
import com.skava.builder.interfaces.StreamSearchV2ServiceBuilder;
import com.skava.builder.interfaces.StreamSearchV2KraftServiceBuilder;
import com.skava.builder.interfaces.ZookeeperManagerServiceBuilder;
import com.skava.builder.remote.interfaces.APIAdminRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.SkavaKeystoreRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamCatalogRemoteServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.model.Tenant;
import com.skava.model.TenantThreadLocal;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;

import lombok.Getter;
import lombok.Setter;

public class StreamSearchKraftTenantContextFactoryImpl extends SkavaTenantContextFactory
{

    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());

    private static final String SERVEREXCEPTION_NULLPOINTER = "Null Pointer Exception Occurred While Processing on SkavaTenantContextFactoryImpl.";

    @Setter @Getter public List<Object> skavaTenantContextFactoryList;

    /* for code readability please put all *ServiceBuilder instance objects below */
    static ZookeeperManagerServiceBuilder zookeeperManagerServiceBuilder;
    SkavaLoggerServiceBuilder skavaLoggerServiceBuilder;
    SkavaResourceBundleServiceBuilder skavaResourceBundleServiceBuilder;
    MemCacheManagerServiceBuilder memCacheManagerServiceBuilder;
    DBSessionManagerServiceBuilder dbSessionManagerServiceBuilder;
    HttpClientServiceBuilder httpClientServiceBuilder;
    ApiTokenPropertiesServiceBuilder apiTokenPropertiesServiceBuilder;
    ScaleApiDriverForListenerServiceBuilder scaleApiDriverForListenerServiceBuilder;
    ScaleMessageListenerServiceBuilder scaleMessageListenerServiceBuilder;
    AnalyticsListnerServiceBuilder analyticsListnerServiceBuilder;
    SearchIndexListenerServiceBuilder searchIndexListenerServiceBuilder;
    IndexListenerServiceBuilder indexListenerServiceBuilder;
    PartnerListenerServiceBuilder partnerListenerServiceBuilder;
    ActiveMQConnectionFactoryServiceBuilder activeMQConnectionFactoryServiceBuilder;
    JMQServiceBuilder jmqServiceBuilder;
    StreamSearchV2ServiceBuilder streamSearchV2ServiceBuilder;

    StreamSearchKraftServiceBuilder streamSearchKraftServiceBuilder;
    StreamSearchV2KraftServiceBuilder streamSearchV2ServiceBuilderKraft;
    StreamJMQFactoryServiceBuilder streamJMQFactoryServiceBuilder;
    SearchServiceBuilder searchServiceBuilder;
    SearchServiceKraftBuilder searchServiceKraftBuilder;
    StreamSearchServiceBuilder streamSearchServiceBuilder;
    SearchSynonymServiceBuilder searchSynonymServiceBuilder;

    /* for code readability please put all *RemoteServiceBuilder instance objects below */
    SkavaKeystoreRemoteServiceBuilder skavaKeystoreRemoteServiceBuilder;
    APIAdminRemoteServiceBuilder apiAdminRemoteServiceBuilder;
    StreamCatalogRemoteServiceBuilder streamCatalogRemoteServiceBuilder;

    private static Set<String> execBuilderMethod = new LinkedHashSet<>();

    public StreamSearchKraftTenantContextFactoryImpl(SearchTenantContextFactoryImpl searchTenantContextFactoryImpl,
                                                     StreamSearchV2KraftServiceBuilderImpl streamSearchV2KraftServiceBuilder,
                                                     StreamSearchKraftServiceBuilderImpl streamSearchKraftServiceBuilder,
                                                     SearchServiceKraftBuilderImpl searchServiceKraftBuilder) throws Exception
    {
        super(zookeeperManagerServiceBuilder);
        this.skavaTenantContextFactoryList = searchTenantContextFactoryImpl.getSkavaTenantContextFactoryList();
        this.skavaTenantContextFactoryList.add(streamSearchV2KraftServiceBuilder);
        this.skavaTenantContextFactoryList.add(streamSearchKraftServiceBuilder);
        this.skavaTenantContextFactoryList.add(searchServiceKraftBuilder);
        initAllSpringBuilders();
    }

    void initAllSpringBuilders() throws ServerException
    {
        if (skavaTenantContextFactoryList != null && !skavaTenantContextFactoryList.isEmpty())
        {
            for (Object object : skavaTenantContextFactoryList)
            {
                initRemoteBuilder(object); // Note: Don't if check *RemoteServiceBuilder builders below. for code readability we moved to initRemoteBuilder method.

                if (object instanceof ZookeeperManagerServiceBuilder)
                {
                    zookeeperManagerServiceBuilder = (ZookeeperManagerServiceBuilder) object;
                }
                else if (object instanceof SkavaLoggerServiceBuilder)
                {
                    skavaLoggerServiceBuilder = (SkavaLoggerServiceBuilder) object;
                    skavaLoggerServiceBuilder.reInitializeLog4j2(zookeeperManagerServiceBuilder);
                }
                else if (object instanceof SkavaResourceBundleServiceBuilder)
                {
                    skavaResourceBundleServiceBuilder = (SkavaResourceBundleServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.MessageSource);
                }
                else if (object instanceof MemCacheManagerServiceBuilder)
                {
                    memCacheManagerServiceBuilder = (MemCacheManagerServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                }
                else if (object instanceof DBSessionManagerServiceBuilder)
                {
                    dbSessionManagerServiceBuilder = (DBSessionManagerServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.DBSESSIONMANAGER);
                }
                else if (object instanceof HttpClientServiceBuilder)
                {
                    httpClientServiceBuilder = (HttpClientServiceBuilder) object;
                    execBuilderMethod.add(HttpClientServiceBuilder.HTTPCLIENTSERVICE);
                }
                else if (object instanceof ApiTokenPropertiesServiceBuilder)
                {
                    apiTokenPropertiesServiceBuilder = (ApiTokenPropertiesServiceBuilder) object;
                    execBuilderMethod.add(ApiTokenPropertiesServiceBuilder.API_TOKEN_PROPERTIES);
                }
                else if (object instanceof ScaleApiDriverForListenerServiceBuilder)
                {
                    scaleApiDriverForListenerServiceBuilder = (ScaleApiDriverForListenerServiceBuilder) object;
                    execBuilderMethod.add(ScaleApiDriverForListenerServiceBuilder.SCALEAPIDRIVER);
                }
                else if (object instanceof ScaleMessageListenerServiceBuilder)
                {
                    scaleMessageListenerServiceBuilder = (ScaleMessageListenerServiceBuilder) object;
                    execBuilderMethod.add(ScaleMessageListenerServiceBuilder.MESSAGELISTENER);
                }
                else if (object instanceof AnalyticsListnerServiceBuilder)
                {
                    analyticsListnerServiceBuilder = (AnalyticsListnerServiceBuilder) object;
                    execBuilderMethod.add(AnalyticsListnerServiceBuilder.ANALYTICSLISTNER);
                }
                else if (object instanceof SearchIndexListenerServiceBuilder)
                {
                    searchIndexListenerServiceBuilder = (SearchIndexListenerServiceBuilder) object;
                    execBuilderMethod.add(SearchIndexListenerServiceBuilder.SEARCHINDEXLISTENER);
                }
                else if (object instanceof IndexListenerServiceBuilder)
                {
                    indexListenerServiceBuilder = (IndexListenerServiceBuilder) object;
                    execBuilderMethod.add(IndexListenerServiceBuilder.INDEXLISTENER);
                }
                else if (object instanceof PartnerListenerServiceBuilder)
                {
                    partnerListenerServiceBuilder = (PartnerListenerServiceBuilder) object;
                    execBuilderMethod.add(PartnerListenerServiceBuilder.PARTNERLISTENER);
                }
                else if (object instanceof ActiveMQConnectionFactoryServiceBuilder)
                {
                    activeMQConnectionFactoryServiceBuilder = (ActiveMQConnectionFactoryServiceBuilder) object;
                    execBuilderMethod.add(ActiveMQConnectionFactoryServiceBuilder.ACTIVEMQCONNECTIONFACTORY);
                }
                else if (object instanceof JMQServiceBuilder)
                {
                    jmqServiceBuilder = (JMQServiceBuilder) object;
                    execBuilderMethod.add(JMQServiceBuilder.JMQSERVICE);
                }
                else if (object instanceof StreamSearchV2ServiceBuilder)
                {
                    streamSearchV2ServiceBuilder = (StreamSearchV2ServiceBuilder) object;
                    execBuilderMethod.add(StreamSearchV2ServiceBuilder.STREAMSEARCHV2SERVICE);
                }
                else if (object instanceof StreamSearchV2KraftServiceBuilder)
                {
                    streamSearchV2ServiceBuilderKraft = (StreamSearchV2KraftServiceBuilder) object;
                    execBuilderMethod.add(StreamSearchV2KraftServiceBuilder.STREAMSEARCHV2KRAFTSERVICE);
                }
                else if (object instanceof StreamSearchKraftServiceBuilder)
                {
                    streamSearchKraftServiceBuilder = (StreamSearchKraftServiceBuilder) object;
                    execBuilderMethod.add(StreamSearchKraftServiceBuilder.STREAMSEARCHKRAFTSERVICE);
                }
                else if (object instanceof StreamJMQFactoryServiceBuilder)
                {
                    streamJMQFactoryServiceBuilder = (StreamJMQFactoryServiceBuilder) object;
                    execBuilderMethod.add(StreamJMQFactoryServiceBuilder.STREAM_JMQ_FACTORY);
                }
                else if (object instanceof SearchServiceBuilder)
                {
                    searchServiceBuilder = (SearchServiceBuilder) object;
                    execBuilderMethod.add(SearchServiceBuilder.SEARCHSERVICE);
                }
                else if (object instanceof SearchServiceKraftBuilder)
                {
                    searchServiceKraftBuilder = (SearchServiceKraftBuilder) object;
                    execBuilderMethod.add(SearchServiceKraftBuilder.SEARCHKRAFTSERVICE);
                }
                else if (object instanceof StreamSearchServiceBuilder)
                {
                    streamSearchServiceBuilder = (StreamSearchServiceBuilder) object;
                    execBuilderMethod.add(StreamSearchServiceBuilder.STREAMSEARCHSERVICE);
                }
                else if (object instanceof SearchSynonymServiceBuilder)
                {
                    searchSynonymServiceBuilder = (SearchSynonymServiceBuilder) object;
                    execBuilderMethod.add(SearchSynonymServiceBuilder.SEARCH_SYNONYM_SERVICE);
                }

            }
            putExecBuilderIntoMap();
        }
    }

    private void putExecBuilderIntoMap() throws ServerException
    {
        Set<Tenant> tenants = zookeeperManagerServiceBuilder.getTenants();
        if (!execBuilderMethod.isEmpty() && tenants != null && !tenants.isEmpty())
        {
            Class<? extends StreamSearchKraftTenantContextFactoryImpl> currentClass = this.getClass();
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            Map<String, Method> currentClassMethods = new HashMap<>();
            for (Method invokeMethod : declaredMethods)
            {
                currentClassMethods.put(invokeMethod.getName(), invokeMethod);
            }
            for (Tenant tenant : tenants)
            {
                String execMethodTemp = null;
                try
                {
                    TenantThreadLocal.THREAD_TENANT.set(tenant);
                    for (String execMethod : execBuilderMethod)
                    {
                        execMethodTemp = execMethod;
                        Method invokeMethod = currentClassMethods.get("get");
                        if (invokeMethod != null)
                        {
                            invokeMethod.invoke(this, tenant, execMethod);
                        }
                        else
                        {
                            logger.info("No such builder method {} found ", execMethod);
                        }
                    }
                }
                catch (Exception e)
                {
                    String errorMsg = "Exception @putExecBuilderIntoMap tenant Id : " + tenant.getId() + ", execBuilderMethod : " + execMethodTemp;
                    logger.error(errorMsg, e);
                    System.err.println(errorMsg + e.getMessage());
                    throw new ServerException(e);
                }
                finally
                {
                    TenantThreadLocal.THREAD_TENANT.remove();
                }
            }
        }
    }

    private void initRemoteBuilder(Object object)
    {
        if (object instanceof APIAdminRemoteServiceBuilder)
        {
            apiAdminRemoteServiceBuilder = (APIAdminRemoteServiceBuilder) object;
            execBuilderMethod.add(APIAdminRemoteServiceBuilder.API_ADMIN_REMOTE_SERVICE);
        }
        else if (object instanceof SkavaKeystoreRemoteServiceBuilder)
        {
            skavaKeystoreRemoteServiceBuilder = (SkavaKeystoreRemoteServiceBuilder) object;
            execBuilderMethod.add(SkavaTenantContextFactory.SKAVA_KEYSTORE_REMOTE_SERVICE);
        }
        else if (object instanceof StreamCatalogRemoteServiceBuilder)
        {
            streamCatalogRemoteServiceBuilder = (StreamCatalogRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamCatalogRemoteServiceBuilder.STREAM_CATALOG_REMOTE_SERVICE);
        }
    }

    void nullCheck(Object obj, Tenant tenant) throws ServerException
    {
        if (obj == null) { throw new ServerException(SERVEREXCEPTION_NULLPOINTER); }
        if (tenant == null) { throw new ServerException(SERVEREXCEPTION_NULLPOINTER); }
    }

    @Override
    public Object get(Tenant tenant, String builderName) throws ServerException
    {
        Object toRet = null;
        if (builderName != null)
        {
            if (builderName.equals(SkavaTenantContextFactory.DBSESSIONMANAGER))
            {
                nullCheck(dbSessionManagerServiceBuilder, tenant);
                MemCacheManager memCacheManager = (MemCacheManager) get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                toRet = dbSessionManagerServiceBuilder.getDBSessionManager(tenant, memCacheManager);
            }
            else if (builderName.equals(SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE))
            {
                nullCheck(memCacheManagerServiceBuilder, tenant);
                toRet = memCacheManagerServiceBuilder.getMemCacheManagerService(tenant);
            }
            else if (builderName.equals(HttpClientServiceBuilder.HTTPCLIENTSERVICE))
            {
                nullCheck(httpClientServiceBuilder, tenant);
                toRet = httpClientServiceBuilder.getHttpClientService(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
            }
            else if (builderName.equals(StreamSearchServiceBuilder.STREAMSEARCHSERVICE))
            {
                nullCheck(streamSearchServiceBuilder, tenant);
                toRet = streamSearchServiceBuilder.getStreamSearchService(tenant, this);
            }
            else if (builderName.equals(SearchServiceBuilder.SEARCHSERVICE))
            {
                nullCheck(searchServiceBuilder, tenant);
                toRet = searchServiceBuilder.getSearchService(tenant);
            }
            else if (builderName.equals(SearchServiceKraftBuilder.SEARCHKRAFTSERVICE))
            {
                nullCheck(searchServiceKraftBuilder, tenant);
                toRet = searchServiceKraftBuilder.getSearchKraftService(tenant);
            }
            else if (builderName.equals(SearchSynonymServiceBuilder.SEARCH_SYNONYM_SERVICE))
            {
                nullCheck(searchSynonymServiceBuilder, tenant);
                toRet = searchSynonymServiceBuilder.getSearchSynonymService(tenant, this);
            }
            else if (builderName.equals(StreamCatalogRemoteServiceBuilder.STREAM_CATALOG_REMOTE_SERVICE))
            {
                nullCheck(streamCatalogRemoteServiceBuilder, tenant);
                toRet = streamCatalogRemoteServiceBuilder.getStreamCatalogRemoteService(tenant, this);
            }
            else if (builderName.equals(JMQServiceBuilder.JMQSERVICE))
            {
                nullCheck(jmqServiceBuilder, tenant);
                toRet = jmqServiceBuilder.getJMQService(tenant, this);
            }
            else if (builderName.equals(SkavaTenantContextFactory.MessageSource))
            {
                nullCheck(skavaResourceBundleServiceBuilder, tenant);
                toRet = skavaResourceBundleServiceBuilder.get(tenant);
            }
            else if (builderName.equals(ApiTokenPropertiesServiceBuilder.API_TOKEN_PROPERTIES))
            {
                nullCheck(apiTokenPropertiesServiceBuilder, tenant);
                toRet = apiTokenPropertiesServiceBuilder.getAPITokenProperties(tenant);
            }
            else if (builderName.equals(ScaleApiDriverForListenerServiceBuilder.SCALEAPIDRIVER))
            {
                nullCheck(scaleApiDriverForListenerServiceBuilder, tenant);
                toRet = scaleApiDriverForListenerServiceBuilder.getScaleApiDriver(tenant, this, ScaleApiDriverForListenerServiceBuilder.SCALEAPIDRIVER);
            }
            else if (builderName.equals(ScaleApiDriverForListenerServiceBuilder.SCALEAPIDRIVERFORLISTENER))
            {
                nullCheck(scaleApiDriverForListenerServiceBuilder, tenant);
                toRet = scaleApiDriverForListenerServiceBuilder.getScaleApiDriver(tenant, this, ScaleApiDriverForListenerServiceBuilder.SCALEAPIDRIVERFORLISTENER);
            }
            else if (builderName.equals(ScaleMessageListenerServiceBuilder.MESSAGELISTENER))
            {
                nullCheck(scaleMessageListenerServiceBuilder, tenant);
                toRet = scaleMessageListenerServiceBuilder.getScaleMessageListener(tenant, this);
            }
            else if (builderName.equals(AnalyticsListnerServiceBuilder.ANALYTICSLISTNER))
            {
                nullCheck(analyticsListnerServiceBuilder, tenant);
                toRet = analyticsListnerServiceBuilder.getAnalyticsListner(tenant, this);
            }
            else if (builderName.equals(SearchIndexListenerServiceBuilder.SEARCHINDEXLISTENER))
            {
                nullCheck(searchIndexListenerServiceBuilder, tenant);
                toRet = searchIndexListenerServiceBuilder.getSearchIndexListener(tenant, this);
            }
            else if (builderName.equals(IndexListenerServiceBuilder.INDEXLISTENER))
            {
                nullCheck(indexListenerServiceBuilder, tenant);
                toRet = indexListenerServiceBuilder.getIndexListener(tenant, this);
            }
            else if (builderName.equals(PartnerListenerServiceBuilder.PARTNERLISTENER))
            {
                nullCheck(partnerListenerServiceBuilder, tenant);
                toRet = partnerListenerServiceBuilder.getPartnerListener(tenant, this);
            }
            else if (builderName.equals(ActiveMQConnectionFactoryServiceBuilder.ACTIVEMQCONNECTIONFACTORY))
            {
                nullCheck(activeMQConnectionFactoryServiceBuilder, tenant);
                toRet = activeMQConnectionFactoryServiceBuilder.getActiveMQConnectionFactory(tenant);
            }
            else if (builderName.equals(StreamSearchV2ServiceBuilder.STREAMSEARCHV2SERVICE))
            {
                nullCheck(streamSearchV2ServiceBuilder, tenant);
                toRet = streamSearchV2ServiceBuilder.getStreamSearchV2Service(tenant, this);
            }
            else if (builderName.equals(StreamSearchV2KraftServiceBuilder.STREAMSEARCHV2KRAFTSERVICE))
            {
                nullCheck(streamSearchV2ServiceBuilderKraft, tenant);
                toRet = streamSearchV2ServiceBuilderKraft.getStreamSearchV2KraftService(tenant, this);
            }
            else if (builderName.equals(StreamSearchKraftServiceBuilder.STREAMSEARCHKRAFTSERVICE))
            {
                nullCheck(streamSearchKraftServiceBuilder, tenant);
                toRet = streamSearchKraftServiceBuilder.getStreamSearchKraftService(tenant, this);
            }
            else if (builderName.equals(StreamJMQFactoryServiceBuilder.STREAM_JMQ_FACTORY))
            {
                nullCheck(streamJMQFactoryServiceBuilder, tenant);
                toRet = streamJMQFactoryServiceBuilder.getStreamJMQFactoryService(tenant, this);
            }
            else if (builderName.equals(SkavaTenantContextFactory.SKAVA_KEYSTORE_REMOTE_SERVICE))
            {
                nullCheck(skavaKeystoreRemoteServiceBuilder, tenant);
                toRet = skavaKeystoreRemoteServiceBuilder.getSkavaKeystoreRemoteService(tenant, this);
            }
        }
        return toRet;
    }
}
