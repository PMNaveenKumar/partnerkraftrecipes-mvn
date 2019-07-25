package com.skava.builder.interfaces;

import com.skava.model.Tenant;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.services.PimServiceKraft;
import com.skava.util.ServerException;

public interface PimServiceKraftBuilder extends SkavaBuilder
{
    String PIMSERVICEKRAFT = "PimServiceKraft";
    PimServiceKraft getPimServiceKraft(Tenant tenant, SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException;
}
