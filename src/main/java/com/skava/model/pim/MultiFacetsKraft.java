package com.skava.model.pim;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class MultiFacetsKraft implements Serializable
{
    private static final long serialVersionUID = 8544894685483520790L;
    private @Getter @Setter SelectedKraftFacet[] selectedFacets;
    private @Getter @Setter SelectedKraftFacet[] recommendationCurates;
}
