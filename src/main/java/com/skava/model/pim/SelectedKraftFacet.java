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
package com.skava.model.pim;

import java.io.Serializable;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <h1> SelectedFacet </h1>
 * <p>In PIM Product List Page API and Search Product API, user can filter the products based on the available filter. This model is used to hold that filter configuration</p>
 * @author: <u>Skava Platform Team</u>
 * @version 7.5
 * @since 6.0
 */
@JsonInclude(Include.NON_DEFAULT)
@ApiModel(value = "SelectedFacetModel", description = "In PIM Product List Page API and Search Product API, user can filter the products based on the available filter. This model is used to hold that filter configuration .")
public class SelectedKraftFacet implements Serializable
{
    private static final long serialVersionUID = 3187011539723300095L;
    public static final int STATUS_PROP_NOTAVILABLE=0;
    public static final int STATUS_PROP_AVILABLE=1;
    public static final int STATUS_VALUE_AVILBLE=2;

    //Default operation is include
    public static final int FACET_INCLUDE = 0;
    public static final int FACET_EXCLUDE = 1;
    
    @ApiModelProperty(value = PimConstants.DEF_ITEMKEY, readOnly = true, required = false)
    private @Getter @Setter String key;
    
    @ApiModelProperty(value = PimConstants.DEF_ITEMVALUE, readOnly = true, required = false)
    private @Getter @Setter String[] value;
    
    @ApiModelProperty(value = PimConstants.DEF_SELECTEDFACET_SELECTED, readOnly = true, required = false)
    private @Getter @Setter int selected;
    
    private @Getter @Setter int operation;
}
