package com.skava.userv2.builder;

import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.StreamComServiceKraftBuilder;
import com.skava.model.Tenant;
import com.skava.services.userv2.KraftUserService;
import com.skava.util.ServerException;

public abstract interface StreamComKraftUserServiceBuilder
  extends StreamComServiceKraftBuilder
{
  public static final String STREAMCOMKRAFTUSERSERVICE = "StreamComKraftUserService";
  
  public abstract KraftUserService getStreamComKraftService(Tenant paramTenant, SkavaTenantContextFactory paramSkavaTenantContextFactory)
    throws ServerException;
}

/* Location:           C:\Users\ADHITHYAKRISHNA.S.ITLINFOSYS\.m2\repository\com\skava\remote\skavauser\7.5.1-SNAPSHOT\skavauser-7.5.1-SNAPSHOT-remote.jar
 * Qualified Name:     com.skava.builder.interfaces.StreamComUserServiceBuilder
 * Java Class Version: 8 (52.0)
 * JD-Core Version:    0.7.1
 */