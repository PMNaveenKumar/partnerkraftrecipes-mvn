/*

Note about primaryKeys: 
If a sheet has primaryKeys set, that will indicate when to overwrite a row.
Multiple keys can be used such that when a row is being added,
if there already exists a row with matching primary key values,
the latest entry row is discarded
*/

const defaultStartTime = new Date(2018, 1, 1).getTime()
const defaultEndTime = new Date(2030,1,1).getTime() //new Date(2030,1,1).getTime()

var pim_schema = {
    "sheets": [{
            "name": "categorymaster",
            "primaryKeys": ["categoryid"],
            "columns": [{
                    "header": "categoryid",
                    "key": "categoryid"
                },
                {
                    "header": "parentcategoryid",
                    "key": "parentcategoryid"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "hasproducts",
                    "key": "hasproducts"
                },
                {
                    "header": "defaultparentcategory",
                    "key": "defaultparentcategory"
                },
                {
                    "header": "visible",
                    "key": "visible"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                },
                {
                    "header": "type",
                    "key": "type"
                },
                {
                    header  :   'segments',
                    key     :   'segments'
                }
            ]
        },
        {
            "name": "categorymasterproperties",
            "primaryKeys": ["categoryid", "name", 'locale'],
            "columns": [{
                    "header": "categoryid",
                    "key": "categoryid"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "locked",
                    "key": "locked"
                }
            ]
        },
        {
            "name": "productmaster",
            "primaryKeys": ["productid"],
            "columns": [{
                    "header": "productid",
                    "key": "productid"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "collection",
                    "key": "collection"
                },
                {
                    "header": "subproductids",
                    "key": "subproductids"
                },
                {
                    "header": "upsellproductids",
                    "key": "upsellproductids"
                },
                {
                    "header": "crosssellproductids",
                    "key": "crosssellproductids"
                },
                {
                    "header": "bundle",
                    "key": "bundle"
                },
                {
                    "header": "bundlemainproductid",
                    "key": "bundlemainproductid"
                },
                {
                    "header": "bundlemandatoryproductids",
                    "key": "bundlemandatoryproductids"
                },
                {
                    "header": "bundleoptionalproductids",
                    "key": "bundleoptionalproductids"
                },
                {
                    "header": "defaultparentcategoryid",
                    "key": "defaultparentcategoryid"
                },
                {
                    "header": "groupid",
                    "key": "groupid"
                },
                {
                    "header": "facetgroup",
                    "key": "facetgroup"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                },
                {
                    "header": "visible",
                    "key": "visible"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    header  :   'segments',
                    key     :   'segments'        
                },
                {
                    'header'  :   'ismatch',
                    'key'     :   'ismatch'        
                }
            ]
        },
        {
            "name": "productmasterproperties",
            "primaryKeys": ["productid", "name"],
            "columns": [{
                    "header": "productid",
                    "key": "productid"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value",
                    defaultValue: ""
                },
                {
                    "header": "locked",
                    "key": "locked",
                    defaultValue: false
                },
                {
                    "header": "locale",
                    "key": "locale",
                    defaultValue: "en_US"
                },
                {
                    "header": "overridebysku",
                    "key": "overridebysku",
                    defaultValue: true
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "skumaster",
            "primaryKeys": ["skuid"],
            "columns": [{
                    "header": "skuid",
                    "key": "skuid"
                },
                {
                    "header": "productids",
                    "key": "productids"
                },
                {
                    "header": "upcids",
                    "key": "upcids"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "skumasterproperties",
            "primaryKeys": ["skuid", "name"],
            "columns": [{
                    "header": "skuid",
                    "key": "skuid"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "productcategory",
            "columns": [{
                    "header": "categoryid",
                    "key": "categoryid"
                },
                {
                    "header": "productid",
                    "key": "productid"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "facetmaster",
            "primaryKeys": ["name"],
            "columns": [{
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "type",
                    "key": "type"
                },
                {
                    "header": "fieldtype",
                    "key": "fieldtype"
                },
                {
                    "header": "validationtype",
                    "key": "validationtype"
                },
                {
                    "header": "displaytype",
                    "key": "displaytype"
                },
                {
                    "header": "filterable",
                    "key": "filterable"
                },
                {
                    "header": "searchable",
                    "key": "searchable"
                },
                {
                    "header": "sortable",
                    "key": "sortable"
                },
                {
                    "header": "required",
                    "key": "required"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                },
                {
                    header  :   'showintab',
                    key     :   'showintab'
                }
            ]
        },
        {
            "name": "facetmasterproperties",
            "primaryKeys": ["facetname", "name"],
            "columns": [{
                    "header": "facetname",
                    "key": "facetname"
                },
                {
                    "header": "facetlocale",
                    "key": "facetlocale"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "locked",
                    "key": "locked"
                }
            ]
        },
        {
            "name": "storemaster",
            "columns": [{
                    "header": "storeId",
                    "key": "storeId"
                },
                {
                    "header": "parentstoreid",
                    "key": "parentstoreid"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "type",
                    "key": "type"
                },
                {
                    "header": "zip",
                    "key": "zip"
                },
                {
                    "header": "latitude",
                    "key": "latitude"
                },
                {
                    "header": "longitude",
                    "key": "longitude"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "storemasterproperties",
            "columns": [{
                    "header": "storeId",
                    "key": "storeId"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "catalogsku",
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "skuid",
                    "key": "skuid"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "catalogskuproperties",
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "skuid",
                    "key": "skuid"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "catalogcategory",
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "categoryid",
                    "key": "categoryid"
                },
                {
                    header: "parentcategoryid",
                    key: "parentcategoryid"
                },
                {
                    "header": "status",
                    "key": "status"
                },
                {
                    header: "hasproducts",
                    key: "hasproducts",
                    defaultValue: true
                },
                {
                    header: "defaultparentcategory",
                    key: "defaultparentcategory"
                },
                {
                    header: "visible",
                    key: "visible",
                    defaultValue: true
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                },
                {
                    "header": "type",
                    "key": "type"
                },
                {
                    "header": "segments",
                    "key": "segments"
                }
            ]
        },
        {
            "name": "catalogcategoryproperties",
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "categoryid",
                    "key": "categoryid"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "locked",
                    "key": "locked"
                }
            ]
        },
        {
            "name": "catalogproduct",
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "productid",
                    "key": "productid"
                },
                {
                    "header": "status",
                    "key": "status",
                    defaultValue: 1
                },
                {
                    "header": "locked",
                    "key": "locked",
                    defaultValue: false
                },
                {
                    "header": "visible",
                    "key": "visible",
                    defaultValue: true
                },
                {
                    "header": "defaultparentcategoryid",
                    "key": "defaultparentcategoryid"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                },
                {
                    header  :   'segments',
                    key     :   'segments'
                }
            ]
        },
        {
            "name": "catalogproductproperties",
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "productid",
                    "key": "productid"
                },
                {
                    "header": "name",
                    "key": "name"
                },
                {
                    "header": "value",
                    "key": "value"
                },
                {
                    "header": "locked",
                    "key": "locked"
                },
                {
                    "header": "locale",
                    "key": "locale"
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        },
        {
            "name": "catalogproductcategory",
            "primaryKeys": ["catalogid", "categoryid", "productid"],
            "columns": [{
                    "header": "catalogid",
                    "key": "catalogid"
                },
                {
                    "header": "categoryid",
                    "key": "categoryid"
                },
                {
                    "header": "productid",
                    "key": "productid"
                },
                {
                    "header": "status",
                    "key": "status",
                    defaultValue: 1
                },
                {
                    "header": "starttime",
                    "key": "starttime",
                    defaultValue: defaultStartTime
                },
                {
                    "header": "endtime",
                    "key": "endtime",
                    defaultValue: defaultEndTime
                }
            ]
        }
    ]
}

module.exports = pim_schema