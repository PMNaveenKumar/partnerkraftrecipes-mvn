package com.skava.builder.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.skava.builder.interfaces.ApiTokenPropertiesServiceBuilder;
import com.skava.builder.interfaces.DBSessionManagerServiceBuilder;
import com.skava.builder.interfaces.HttpClientServiceBuilder;
import com.skava.builder.interfaces.JBPMServiceBuilder;
import com.skava.builder.interfaces.MemCacheManagerServiceBuilder;
import com.skava.builder.interfaces.PimServiceBuilder;
import com.skava.builder.interfaces.PimServiceKraftBuilder;
import com.skava.builder.interfaces.SharedSecretsMapServiceBuilder;
import com.skava.builder.interfaces.SkavaLoggerServiceBuilder;
import com.skava.builder.interfaces.SkavaResourceBundleServiceBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.ZookeeperManagerServiceBuilder;
import com.skava.builder.remote.interfaces.APIAdminRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.PromotionRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.SkavaKeystoreRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamComUserRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamInventoryUserRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamSearchV2KraftRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamSearchV2RemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamUserRemoteServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.model.Tenant;
import com.skava.model.TenantThreadLocal;
import com.skava.model.pim.PimConstants;
import com.skava.pim.helper.PIMUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;

import lombok.Getter;
import lombok.Setter;

public class PimServiceKraftTenantContextFactoryImpl extends SkavaTenantContextFactory
{
    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());

    private static final String SERVEREXCEPTION_NULLPOINTER = "Null Pointer Exception Occurred While Processing on SkavaTenantContextFactoryImpl.";

	private static final String PIMSERVICETENANTCONTEXTFACTORYIMPL = "PIMSERVICETENANTCONTEXTFACTORYIMPL";

    @Setter @Getter public List<Object> skavaTenantContextFactoryList;

    /* for code readability please put all *ServiceBuilder instance objects below */
    static ZookeeperManagerServiceBuilder zookeeperManagerServiceBuilder;
    SkavaResourceBundleServiceBuilder skavaResourceBundleServiceBuilder;
    SkavaLoggerServiceBuilder skavaLoggerServiceBuilder;
    MemCacheManagerServiceBuilder memCacheManagerServiceBuilder;
    DBSessionManagerServiceBuilder dbSessionManagerServiceBuilder;
    HttpClientServiceBuilder httpClientServiceBuilder;
    JBPMServiceBuilder jbpmServiceBuilder;
    ApiTokenPropertiesServiceBuilder apiTokenPropertiesServiceBuilder;
    PimServiceBuilder pimServiceBuilder;
    PimServiceKraftBuilder pimServiceKraftBuilder;
    StreamSearchV2KraftRemoteServiceBuilder streamSearchV2KraftRemoteServiceBuilder;
    SharedSecretsMapServiceBuilder sharedSecretsMapServiceBuilder;

    /* for code readability please put all *RemoteServiceBuilder instance objects below */
    SkavaKeystoreRemoteServiceBuilder skavaKeystoreRemoteServiceBuilder;
    APIAdminRemoteServiceBuilder apiAdminRemoteServiceBuilder;
    StreamInventoryUserRemoteServiceBuilder streamInventoryUserRemoteServiceBuilder;
    StreamSearchV2RemoteServiceBuilder streamSearchV2RemoteServiceBuilder;
    PromotionRemoteServiceBuilder promotionRemoteServiceBuilder;
    StreamUserRemoteServiceBuilder streamUserRemoteServiceBuilder;
    StreamComUserRemoteServiceBuilder streamComUserRemoteServiceBuilder;

    private static Set<String> execBuilderMethod = new LinkedHashSet<>();

    public PimServiceKraftTenantContextFactoryImpl(PimServiceTenantContextFactoryImpl skavaTenantContextFactoryList, PimServiceKraftBuilderImpl pimServiceKraftBuilder, Object streamSearchV2KraftRemoteServiceBuilder) throws Exception
    {
        super(zookeeperManagerServiceBuilder);
        this.skavaTenantContextFactoryList = skavaTenantContextFactoryList.getSkavaTenantContextFactoryList();
        this.skavaTenantContextFactoryList.add(pimServiceKraftBuilder);
        this.skavaTenantContextFactoryList.add(streamSearchV2KraftRemoteServiceBuilder);
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
                else if (object instanceof JBPMServiceBuilder)
                {
                    jbpmServiceBuilder = (JBPMServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.BPMSERVICE);
                }
                else if (object instanceof ApiTokenPropertiesServiceBuilder)
                {
                    apiTokenPropertiesServiceBuilder = (ApiTokenPropertiesServiceBuilder) object;
                }
                else if (object instanceof SharedSecretsMapServiceBuilder)
                {
                    sharedSecretsMapServiceBuilder = (SharedSecretsMapServiceBuilder) object;
                }
                else if (object instanceof PimServiceBuilder)
                {
                    pimServiceBuilder = (PimServiceBuilder) object;
                    execBuilderMethod.add(PimServiceBuilder.PIMSERVICE);
                }
                else if (object instanceof PimServiceKraftBuilder)
                {
                    pimServiceKraftBuilder = (PimServiceKraftBuilder) object;
                    execBuilderMethod.add(PimServiceKraftBuilder.PIMSERVICEKRAFT);
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
            Class<? extends PimServiceKraftTenantContextFactoryImpl> currentClass = this.getClass();
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
                    String errorMsg = " Exception @putExecBuilderIntoMap tenant Id : " + tenant.getId() + ", execBuilderMethod : " + execMethodTemp;
                    PIMUtil.writeLog(Level.ERROR, PIMUtil.getCallingMethodName(2), PIMSERVICETENANTCONTEXTFACTORYIMPL, e, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_EXCEPTION+errorMsg, null, PIMSERVICETENANTCONTEXTFACTORYIMPL);
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
            execBuilderMethod.add(SkavaTenantContextFactory.SKAVAKEYSTORESERVICE);
        }
        else if (object instanceof StreamInventoryUserRemoteServiceBuilder)
        {
            streamInventoryUserRemoteServiceBuilder = (StreamInventoryUserRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamInventoryUserRemoteServiceBuilder.STREAM_INVENTORY_USER_REMOTE_SERVICE);
        }
        else if (object instanceof StreamSearchV2RemoteServiceBuilder)
        {
            streamSearchV2RemoteServiceBuilder = (StreamSearchV2RemoteServiceBuilder) object;
            execBuilderMethod.add(StreamSearchV2RemoteServiceBuilder.STREAM_SEARCH_V2_REMOTE_SERVICE);
        }
        else if (object instanceof PromotionRemoteServiceBuilder)
        {
            promotionRemoteServiceBuilder = (PromotionRemoteServiceBuilder) object;
            execBuilderMethod.add(PromotionRemoteServiceBuilder.PROMOTION_REMOTE_SERVICE);
        }
        else if (object instanceof StreamUserRemoteServiceBuilder)
        {
            streamUserRemoteServiceBuilder = (StreamUserRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamUserRemoteServiceBuilder.STREAM_USER_REMOTE_SERVICE);
        }
        else if (object instanceof StreamComUserRemoteServiceBuilder)
        {
            streamComUserRemoteServiceBuilder = (StreamComUserRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamComUserRemoteServiceBuilder.STREAM_COM_USER_REMOTE_SERVICE);
        }
        else if (object instanceof StreamSearchV2KraftRemoteServiceBuilder)
        {
            streamSearchV2KraftRemoteServiceBuilder = (StreamSearchV2KraftRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamSearchV2KraftRemoteServiceBuilder.STREAM_SEARCH_V2_KRAFT_REMOTE_SERVICE);
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
            else if (builderName.equals(StreamSearchV2RemoteServiceBuilder.STREAM_SEARCH_V2_REMOTE_SERVICE))
            {
                nullCheck(this.streamSearchV2RemoteServiceBuilder, tenant);
                return this.streamSearchV2RemoteServiceBuilder.getStreamSearchV2RemoteService(tenant, this);
            }
            else if (builderName.equals(StreamInventoryUserRemoteServiceBuilder.STREAM_INVENTORY_USER_REMOTE_SERVICE))
            {
                nullCheck(this.streamInventoryUserRemoteServiceBuilder, tenant);
                return this.streamInventoryUserRemoteServiceBuilder.getStreamInventoryUserRemoteService(tenant, this);
            }
            else if (builderName.equals(SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE))
            {
                nullCheck(memCacheManagerServiceBuilder, tenant);
                toRet = memCacheManagerServiceBuilder.getMemCacheManagerService(tenant);
            }
            else if (builderName.equals(SkavaTenantContextFactory.MessageSource))
            {
                nullCheck(this.skavaResourceBundleServiceBuilder, tenant);
                return this.skavaResourceBundleServiceBuilder.get(tenant);
            }
            else if (builderName.equals(SkavaTenantContextFactory.BPMSERVICE))
            {
                nullCheck(this.jbpmServiceBuilder, tenant);
                return this.jbpmServiceBuilder.getBpmService(tenant);
            }
            else if (builderName.equals(PromotionRemoteServiceBuilder.PROMOTION_REMOTE_SERVICE))
            {
                nullCheck(this.promotionRemoteServiceBuilder, tenant);
                return this.promotionRemoteServiceBuilder.getPromotionRemoteService(tenant, this);
            }
            else if (builderName.equals(StreamComUserRemoteServiceBuilder.STREAM_COM_USER_REMOTE_SERVICE))
            {
                nullCheck(this.streamComUserRemoteServiceBuilder, tenant);
                return this.streamComUserRemoteServiceBuilder.getStreamComUserRemoteService(tenant, this);
            }
            else if (builderName.equals(HttpClientServiceBuilder.HTTPCLIENTSERVICE))
            {
                nullCheck(httpClientServiceBuilder, tenant);
                toRet = httpClientServiceBuilder.getHttpClientService(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
            }
            else if (builderName.equals(ApiTokenPropertiesServiceBuilder.API_TOKEN_PROPERTIES))
            {
                nullCheck(apiTokenPropertiesServiceBuilder, tenant);
                toRet = apiTokenPropertiesServiceBuilder.getAPITokenProperties(tenant);
            }
            else if (builderName.equals(PimServiceBuilder.PIMSERVICE))
            {
                nullCheck(pimServiceBuilder, tenant);
                toRet = pimServiceBuilder.getPimService(tenant, this);
            }
            else if (builderName.equals(PimServiceKraftBuilder.PIMSERVICEKRAFT))
            {
                nullCheck(pimServiceKraftBuilder, tenant);
                toRet = pimServiceKraftBuilder.getPimServiceKraft(tenant, this);
            }
            else if (builderName.equals(SharedSecretsMapServiceBuilder.SHAREDSECRETSHASHMAP))
            {
                nullCheck(sharedSecretsMapServiceBuilder, tenant);
                toRet = sharedSecretsMapServiceBuilder.getSharedSecretsHashMap(tenant);
            }
            else if (builderName.equals(SkavaTenantContextFactory.SKAVA_KEYSTORE_REMOTE_SERVICE))
            {
                nullCheck(skavaKeystoreRemoteServiceBuilder, tenant);
                toRet = skavaKeystoreRemoteServiceBuilder.getSkavaKeystoreRemoteService(tenant, this);
            }
            else if (builderName.equals(APIAdminRemoteServiceBuilder.API_ADMIN_REMOTE_SERVICE))
            {
                nullCheck(apiAdminRemoteServiceBuilder, tenant);
                toRet = apiAdminRemoteServiceBuilder.getAPIAdminRemoteService(tenant, this);
            }
            else if (builderName.equals(StreamUserRemoteServiceBuilder.STREAM_USER_REMOTE_SERVICE))
            {
                nullCheck(this.streamUserRemoteServiceBuilder, tenant);
                toRet = streamUserRemoteServiceBuilder.getStreamUserRemoteService(tenant, this);
            }
            else if (builderName.equals(StreamSearchV2KraftRemoteServiceBuilder.STREAM_SEARCH_V2_KRAFT_REMOTE_SERVICE))
            {
                nullCheck(this.streamSearchV2KraftRemoteServiceBuilder, tenant);
                toRet = streamSearchV2KraftRemoteServiceBuilder.getStreamSearchV2RemoteServiceKraft(tenant, this);
            }
        }
        return toRet;
    }
}
