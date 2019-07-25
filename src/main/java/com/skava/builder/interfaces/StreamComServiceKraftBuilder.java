package com.skava.builder.interfaces;

import com.skava.model.Tenant;
import com.skava.services.userv2.KraftUserService;
import com.skava.util.ServerException;

public interface StreamComServiceKraftBuilder extends SkavaBuilder
{
    String KRAFTUSERSERVICE = "KraftUserService";
    KraftUserService getStreamComKraftService(Tenant tenant, SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException;
}
