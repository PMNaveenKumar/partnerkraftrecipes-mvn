var UTC_DAY = 24*60*60*1000
// var starttime = 0//new Date().getTime()
// var endtime = 0//new Date(new Date().getTime()+(UTC_DAY*365*10)).getTime()
// const starttime = 0
// const endtime = 0//new Date(2030,1,1).getTime()

const starttime = String(new Date().getTime())
const endtime = String(new Date(2038,1,1).getTime()) //new Date(2030,1,1).getTime()

var custom_schema = {
	categorymaster: {
		categoryid: '',
		parentcategoryid: 'products',
		status: 1,
		hasproducts: true,
		defaultparentcategory: true,
		visible: true,
		locked: false,
		starttime: starttime,
		endtime: endtime,
		type: 0
	},
	categorymasterproperties: {
		categoryid: '',
		name: '',
		value: '',
		locale: 'en_US',
		locked: false,
		starttime: starttime,
		endtime: endtime
	},
	productmaster: {
		productid: function(entry){
			return entry.id
		},
		status: 1,
		collection: '',
		subproductids: '',
		upsellproductids: '',
		crosssellproductids: '',
		bundle: false,
		bundlemainproductid: '',
		bundlemandatoryproductids: '',
		bundleoptionalproductids: '',
		defaultparentcategoryid: '',
		groupid: '',
		facetgroup: '',
		starttime: starttime,
		endtime: endtime,
		visible: true,
		locked: false
		//ismatch: true
	},
	productmasterproperties: {
		productid: function(entry){
			return entry.productId
		},
		name: '',
		value: '',
		locked: false,
		locale: 'en_US',
		/*locale: function(entry){
				return entry.languageId
			},*/
		overridebysku: false,
		starttime: starttime,
		endtime: endtime
	},
	skumaster: {
		skuid: function(entry){
			return entry.id
		},
		productids: function(entry){
			return entry.id
		},
		upcids: function(entry){
			return entry.id
		},
		status: 1,
		locked: false,
	    starttime: starttime,
	    endtime: endtime
	},
	skumasterproperties: {
		skuid: function(entry){
			return entry.id
		},
		name: '',
		value: '',
		locked: false,
		locale: 'en_US',
	    starttime: starttime,
	    endtime: endtime
	},
	productcategory: {
		categoryid: function(entry){
			return entry.GTIN
		},
		productid: function(entry){
			return entry.productid
		},
		status: 1,
		starttime: starttime,
		endtime: endtime
	},
	facetmaster: {
		name: function(entry){
			return entry.name;
		},
		locale: 'en_US',
		type: function(entry){
			return 0;
		},
		fieldtype: function(entry){
			return entry.fieldtype;
		},
		validationtype: function(entry){
			return entry.validationtype;
		},
		displaytype: 0,
		filterable: function(entry){
			return entry.filterable;
		},
		searchable: function(entry){
			return entry.searchable;
		},
		sortable: function(entry){
			return entry.sortable;
		},
		required: false,
		status: 1,
	    starttime: starttime,
		endtime: endtime,
		showintab: 0
	},
	facetmasterproperties: {
		facetname: function(entry){
			return entry.facetname
		},
		facetlocale:'en_US',
		name: '',
		value: '',
		locale:'en_US',	
		locked:false,
	    starttime: starttime,
		endtime: endtime
	},
	storemaster: {
		storeId: 1,
		parentstoreid: 2,
		status: 3,
		type: 4,
		zip: 5,
		latitude: 6,
		longitude: 7,
		starttime: starttime,
		endtime: endtime
	},
	storemasterproperties: {
		storeId: 1,
		name: 2,
		value: 3,
		locale: 4,
		locked: 5,
		starttime: starttime,
		endtime: endtime
	},
	storesku: {
		storeid: 1,
		skuid: 2,
		status: 3,
		locked: 4
	},
	storeskuproperties: {
		storeid: 1,
		skuid: 2,
		name: 3,
		value: 4,
		locked: 5,
		locale: 6,
		starttime: starttime,
		endtime: endtime
	},
	storecategory: {
		storeid: 1,
		categoryid: 2,
		status: 3,
		locked: 4
	},
	storecategoryproperties: {
		storeid: 1,
		categoryid: 2,
		name: 3,
		value: 4,
		locale: 5,
		locked: 6,
		starttime: starttime,
		endtime: endtime
	},
	storeproduct: {
		storeid: 1,
		productid: 2,
		status: 3,
		locked: 4
	},
	storeproductproperties: {
		storeid: 1,
		productid: 2,
		name: 3,
		value: 4,
		locked: 5,
		locale: 6,
		starttime: 7,
		endtime: 8
	}
}

module.exports = custom_schema