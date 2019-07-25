var UTC_DAY = 24*60*60*1000
var starttime = new Date(2018, 1, 1).getTime()//new Date().getTime()
var endtime = new Date(2030, 1, 1).getTime()//new Date(new Date().getTime()+(UTC_DAY*365*10)).getTime()
var locale = { '1' : 'en_US', '2': 'es_ES' };
var stores = { '1' : 'jello' };

var custom_schema = {
	categorymaster: {
		categoryid 				: 	function(data) {
										return data.categoryId;
									},
		parentcategoryid 		: 	function(data) {
										return data.parentCategoryId;
									},
		status 					: 	1,
		hasproducts 			: 	true,
		defaultparentcategory 	: 	false,
		visible 				: 	true,
		locked 					: 	false,
		starttime 				: 	starttime,
		endtime 				: 	endtime,
		type 					: 	0,
		segments 				: 	''
	},
	categorymasterproperties: {
		categoryid 	: 	function(data) {
							return data.categoryId;
						},
		name 		: 	function(data) {
							return data.name;
						},
		value 		: 	function(data) {
							return data.value;
						},
		locale 		: 	function(data) {
							return locale[data.languageId];
						},
		locked 		: 	false
	},
	productmaster: {
		productid 					: 	function(entry) {
											return entry.RecipeId;
										},
		status 						: 	1,
		collection 					: 	false,
		subproductids 				: 	'',
		upsellproductids 			: 	'',
		crosssellproductids 		: 	'',
		bundle 						: 	false,
		bundlemainproductid 		: 	'',
		bundlemandatoryproductids 	: 	'',
		bundleoptionalproductids 	: 	'',
		defaultparentcategoryid 	: 	'',
		groupid 					: 	'',
		facetgroup 					: 	'',
		starttime 					: 	starttime,
		endtime 					: 	endtime,
		visible 					: 	true,
		locked 						: 	false,
		segments 					: 	''
	},
	productmasterproperties: {
		productid 		: 	function(entry) {
								return entry.productId;
							},
		name 			: 	'',
		value 			: 	'',
		locked 			: 	false,
		locale 			: 	function(entry) {
								return locale[entry.languageId];
							},
		overridebysku 	: 	false,
	    starttime 		: 	starttime,
	    endtime 		: 	endtime
	},
	skumaster: {
		skuid 		: 	function(entry) {
							return entry.RecipeId;
						},
		productids 	: 	function(entry) {
							return entry.RecipeId;
						},
		upcids 		: 	function(entry) {
							return entry.RecipeId;
						},
		status 		: 	1,
		locked 		: 	false,
	    starttime 	: 	starttime,
	    endtime 	: 	endtime
	},
	skumasterproperties: {
		skuid: function(entry) {
			return entry.RecipeId;
		},
		name: '',
		value: '',
		locked: false,
		locale: function(entry) {
			return locale[entry.LanguageId];
		},
	    starttime: starttime,
	    endtime: endtime
	},
	productcategory: {
		categoryid 	: 	function(entry) {
							return entry.TaxonomyId;
						},
		productid 	: 	function(entry) {
							return entry.productId;
						},
		status 		: 	1,
		starttime 	: 	starttime,
		endtime 	: 	endtime
	},
	facetmaster: {
		name 			: 	function(entry) {
								return entry.name;
							},
		locale 			: 	function(entry) {
								// return locale[entry.languageId];
								return locale['1'];
							},
		type 			: 	0,
		fieldtype 		: 	function(entry) {
								return entry.fieldtype;
							},
		validationtype 	: 	function(entry) {
								return entry.validationtype;
							},
		displaytype 	: 	0,
		filterable 		: 	function(entry) {
								return entry.isFilterable;
							},
		searchable 		: 	function(entry) {
								return entry.isSearchable;
							},
		sortable 		: 	function(entry) {
								return entry.isSortable;
							},
		required 		: 	false,
		status 			: 	1,
	    starttime 		: 	starttime,
		endtime 		: 	endtime,
		showintab 		: 	''
	},
	facetmasterproperties: {
		facetname 		: 	function(entry) {
								return entry.facetname;
							},
		facetlocale 	: 	function(entry) {
								// return locale[entry.languageId];
								return locale['1'];
							},
		name 			: 	'',
		value 			: 	'',
		locale 			: 	function(entry) {
								return locale[entry.languageId];
							},	
		locked 			: 	false
	},
	storemaster: {
		storeId: 1,
		parentstoreid: 2,
		status: 3,
		type: 4,
		zip: 5,
		latitude: 6,
		longitude: 7,
		startTime: 8,
		endTime: 9
	},
	storemasterproperties: {
		storeId: 1,
		name: 2,
		value: 3,
		locale: 4,
		locked: 5,
		starttime: 6,
		endtime: 7
	},
	catalogsku: {
		catalogid 	: 	function(entry) {
							return stores[entry.BrandId] || '';
						},
		skuid 		: 	function(entry) {
							return entry.RecipeId;
						},
		status 		: 	1,
		locked 		: 	false,
		starttime 	: 	starttime,
		endtime 	: 	endtime
	},
	catalogskuproperties: {
		catalogid 	: 	function(entry) {
							return stores[entry.BrandId] || '';
						},
		skuid 		: 	function(entry) {
							return entry.RecipeId;
						},
		name 		: 	'',
		value 		: 	'',
		locked 		: 	false,
		locale 		: 	function(entry) {
							return locale[entry.LanguageId];
						},
		starttime 	: 	starttime,
		endtime 	: 	endtime
	},
	catalogcategory: {
		catalogid 				: 	function(entry) {
										return stores[entry.brandId] || '';
									},
		categoryid 				: 	function(entry) {
										return entry.categoryId;
									},
		parentcategoryid 		: 	function(entry) {
										return entry.parentCategoryId;
									},
		status 					: 	1,
		hasproducts 			: 	true,
		defaultparentcategory 	: 	false,
		visible 				: 	true,
		locked 					: 	false,
		starttime 				: 	starttime,
		endtime 				: 	endtime,
		type 					: 	0,
		segments 				: 	''
	},
	catalogcategoryproperties: {
		catalogid 	: 	function(entry) {
							return stores[entry.brandId] || '';
						},
		categoryid 	: 	function(entry) {
							return entry.categoryId;
						},
		name 		: 	function(entry) {
							return entry.propName;
						},
		value 		: 	function(entry) {
							return entry.propVal;
						},
		locale 		: 	function(entry) {
							return locale[entry.languageId];
						},
		locked 		: 	false
	},
	catalogproduct: {
		catalogid 				: 	function(entry) {
										return stores[entry.BrandId] || '';
									},
		productid 				: 	function(entry) {
										return entry.RecipeId;
									},
		status 					: 	1,
		locked 					: 	false,
		visible 				: 	true,
		defaultparentcategoryid : 	'recipes',
		starttime 				: 	starttime,
		endtime 				: 	endtime,
		segments 				: 	''
	},
	catalogproductproperties: {
		catalogid 	:  	function(entry) {
							return stores[entry.brandId] || '';
						},
		productid 	:  	function(entry) {
							return entry.productid;
						},
		name 		: 	'',
		value 		: 	'',
		locked 		:  	false,
		locale 		: 	function(entry) {
							return locale[entry.languageId];
						},
		starttime 	: 	starttime,
		endtime 	: 	endtime
	},
	catalogproductcategory: {
		catalogid 	: 	function(entry) {
							return stores[entry.brandId] || '';
						},
		categoryid 	: 	function(entry) {
							return entry.categoryId;
						},
		productid 	: 	function(entry) {
							return entry.recipeId;
						},
		status 		: 	1,
		starttime 	: 	starttime,
		endtime 	: 	endtime
	}
}

module.exports = custom_schema