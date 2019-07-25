var UTC_DAY = 24*60*60*1000
// var starttime = 0//new Date().getTime()
// var endtime = 0//new Date(new Date().getTime()+(UTC_DAY*365*10)).getTime()
// const starttime = 0
// const endtime = 0//new Date(2030,1,1).getTime()

const starttime = String(new Date().getTime())
const endtime = String(new Date(2038,1,1).getTime()) //new Date(2030,1,1).getTime()
var locale = { '1' : 'en_US', '2': 'es_ES' };

function leadingzeros(id){
	var s = id + "";
	while (s.length < 8) s = "0" + s;
	return s;
}

var custom_schema = {
    productmaster: {
		productid: function(entry) {
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeID))
			{
				return leadingzeros(entry.UserRecipe.UserRecipeID);							
			}			
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
		defaultparentcategoryid 	:  function(entry) {
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeCategory) && (entry.UserRecipe.UserRecipeCategoryID))
			{
				return 'USR' + entry.UserRecipe.UserRecipeCategory.UserRecipeCategoryID;
			}
			return '';
		},
		groupid 					: 	'',
		facetgroup 					: 	'',
		starttime 					: 	starttime,
		endtime 					: 	endtime,
		visible 					: 	true,
		locked 						: 	false,
		segments 					: 	''
	},
	productmasterproperties: {
		productid: function(entry){
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeID))
			{
				return leadingzeros(entry.UserRecipe.UserRecipeID);	
			}
		},
		name: '',
		value: '',
		locked: false,
		locale: 'en_US',
		overridebysku: false,
		starttime: starttime,
		endtime: endtime
	},
	skumaster: {
		skuid: function(entry){
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeID))
			{
				return leadingzeros(entry.UserRecipe.UserRecipeID);	
			}
		},
		productids: function(entry){
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeID))
			{
				return leadingzeros(entry.UserRecipe.UserRecipeID);	
			}
		},
		upcids: function(entry){
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeID))
			{
				return leadingzeros(entry.UserRecipe.UserRecipeID);	
			}
		},
		status: 1,
		locked: false,
	    starttime: starttime,
	    endtime: endtime
	},
	skumasterproperties: {
		skuid: function(entry){
			if((entry.UserRecipe) && (entry.UserRecipe.UserRecipeID))
			{
				return leadingzeros(entry.UserRecipe.UserRecipeID);	
			}
		},
		name: '',
		value: '',
		locked: false,
		locale: 'en_US',
	    starttime: starttime,
	    endtime: endtime
	},
	facetmaster: {
		name 			: 	function(entry) {
								if((entry.name))
								{
									return entry.name;
								}
							},
		locale 			: 	function(entry) {
								// return locale[entry.languageId];
								return locale['1'];
							},
		type 			: 	0,
		fieldtype 		: 	function(entry) {
								if((entry.fieldtype))
								{
									return entry.fieldtype;
								}
							},
		validationtype 	: 	function(entry) {
								if((entry.validationtype))
								{
									return entry.validationtype;
								}
							},
		displaytype 	: 	0,
		filterable 		: 	function(entry) {
								if((entry.isFilterable))
								{
									return entry.isFilterable;
								}
							},
		searchable 		: 	function(entry) {
								if((entry.isSearchable))
								{
									return entry.isSearchable;
								}
							},
		sortable 		: 	function(entry) {
								if((entry.isSortable))
								{
									return entry.isSortable;
								}
							},
		required 		: 	false,
		status 			: 	1,
	    starttime 		: 	starttime,
		endtime 		: 	endtime,
		showintab 		: 	''
	},
	facetmasterproperties: {
		facetname 		: 	function(entry) {
								if((entry.facetname))
								{
									return entry.facetname;
								}
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
	}
}

module.exports = custom_schema
module.leadingzeros = leadingzeros