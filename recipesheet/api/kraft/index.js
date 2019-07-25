var ApplePie = require('../ApplePie')
var Cacher = require('../CacheResponse.js')

var KraftBrands = new ApplePie({
	url:'http://api.kraftapps.com/v4/ecomm/api/brands/',
	defaultResponse:{
		error:1
	}
})

var KraftBrandProducts = new ApplePie({
	url:'http://api.kraftapps.com/v4/ecomm/api/products/filter/',
	get:{
    	ccode:'USA',
    	lcode:'en',
    	sord:'asc',
    	sidx:'GTIN',
    	bid:0,
    	startNum:1,
    	endNum:500
    },
	defaultResponse:{
		error:1
	}
})

var KraftProducts = new ApplePie({
	url:'http://api.kraftapps.com/v4/ecomm/api/products/gtin/eng',
	method:'POST',
	get:{
		sort:'Ascending',
		orderBy:'eCommProductName',
		pageSize:100,
		pageNumber:1
	},
	post:{
		list:{
			process:function(data, query){
				if(data instanceof Array){
					return data.join(',')
				}
				return data
			}
		}
	},
	defaultResponse:{
		error:1
	}
})

var KraftRecipe = new ApplePie({
	url:'http://api.kraftapps.com/v4/Recipes',
	headers : {
        "Authorization" : "Basic a3JhZnQ6a3JhZnQ="
    },
	get:{
    	recipeIds:{
			process:function(data, query){
				// console.log('KraftRecipe')
				if(data instanceof Array){
					return data.join(',')
				}
				return data
			}
		},
		languageId: '',
		brandId: ''
    },
	defaultResponse:{
		error:1
	}
})

var KraftRecipeRelated = new ApplePie({
	url:'http://stgapi.kraftapps.com/v4/recipes/RelatedRecipes',
	headers : {
        "Authorization" : "Basic a3JhZnQ6a3JhZnQ="
    },
	get:{
		PageSize:20,
		PageNo:1,
		ProductName:'',
		VariantName:''
    },
	defaultResponse:{
		error:1
	}
})

var KraftProduct = new ApplePie({
	url:'http://api.kraftapps.com/v4/ecomm/api/products/gtin/eng',
	rest:['productID']
})

// new code
var kraftCategories = new ApplePie({
	url 	: 	'http://api.kraftapps.com/v4/recipes/categories',
	headers : 	{ Authorization: 'Basic a3JhZnQ6a3JhZnQ=' },
	get 	: 	{
					brandId 	: 	'',
					languageId 	: 	'',
					pageSize 	: 	100
				}
});

var kraftRecipeIdsForCategories = new ApplePie({
	url 	: 	'http://api.kraftapps.com/v4/recipes/RecipesByCategory',
	headers : 	{ Authorization: 'Basic a3JhZnQ6a3JhZnQ=' },
	get 	: 	{
					brandId 	: 	'',
					languageId 	: 	'',
					catid 		: 	'',
					pageSize 	: 	5000
				}
});

var kraftRecipeIds = new ApplePie({
	url 	: 	'http://api.kraftapps.com/v4/Recipes/UpdatedKraftRecipes/',
	headers : 	{ Authorization: 'Basic a3JhZnQ6a3JhZnQ=' },
	get 	: 	{
					LastUpdatedDate : 	'',
					languageid 		: 	'',
					siteids 		: 	'',
					pagesize 		: 	'',
					pageno 			: 	''
				}
});

function getRecipeIds(config, callback) {
	kraftRecipeIds.send({
		get : 	{
					LastUpdatedDate : 	config.lastUpdatedDate,
					languageid 		: 	config.languageId,
					siteids 		: 	config.brandId,
					pagesize 		: 	config.pageSize,
					pageno 			: 	config.pageNo
				}
	}, callback);
}

function getKraftCategories(config, callback) {
	kraftCategories.send({
		get: { brandId: config.brandId, languageId: config.languageId }
	}, function(data) {
		callback(data);
	});
}

function getRecipeIdsForCategory(config, categoryId, callback) {
	kraftRecipeIdsForCategories.send({
		get: { brandId: config.brandId, languageId: config.languageId, catid: categoryId }
	}, function(data) {
		callback(data);
	});
}

function getRecipeRelated(productName,cb){
	let cacheKey = 'getRecipeRelated_'+productName
	if(Cacher.isCached(cacheKey)){
		cb(Cacher.getCache(cacheKey))
	} else {
		KraftRecipeRelated.send({
			get:{
				ProductName:productName,
				VariantName:productName
			}
		}, function(data){
			Cacher.setCache(cacheKey,JSON.parse(data))
			cb(data)
		})
	}
}

function getRecipes(ids, cb, config){
	config = config || {};
	// config.nocache = Boolean(config.nocache)
	// need a way to set a cache key based on the list of ids
	// so sum them and count them - this should make the names unique most of the time
	// not bullet proof for sure but this is just the cache
	/*let arr = ids.toString().split(',')
	let sum = arr.reduce(function(n,id){
		return n + parseInt(id)
	},0)
	let cacheKey = 'getRecipes_'+arr.length+'_'+sum*/

	/*if(config.nocache!==true && Cacher.isCached(cacheKey)){
		cb(Cacher.getCache(cacheKey))
	} else {*/
		// get individual cached recipes from request to shorten request
		ids = ids.toString().split(',');
		loadRecipes(ids, config, function(result){
			let fullResponse = {
			  "Metadata": {
			    "PageCount": 1,
			    "PageNo": 0,
			    "TotalCount": 0
			  },
			  "Recipes": result
			}
			// Cacher.setCache(cacheKey,fullResponse)
			cb(JSON.stringify(fullResponse))
		}, [])
	// }
}

// we need to split up calls if recipe request count is > 20??
function loadRecipes(ids, config, cb, result){
	/*if(!result){
		result = []
	}*/
	if(ids.length>0){
		console.log('Loading... ' + ids.length + ' Recipes', true);
		let subset = ids.splice(0,50)
		// console.log(subset.join());
		// console.log('***** Remaining Recipes Count *****', true);
		// console.log(ids.length, true);
		KraftRecipe.send({
			get:{
				recipeIds:subset,
				languageId: config.languageId,
				brandId: config.brandId
			}
		}, function(data){
			try {
				let e = JSON.parse(data)
				if(e.Recipes){
					// log
					e.Recipes.map(function(recipe) {
						subset.splice(subset.indexOf(recipe.RecipeId.toString()), 1);
					});
					if (subset.length) {
						subset.map(function(missingIds) {
							console.log(missingIds + '\tNO_RECIPE_FOUND', true);
						});
					}						
					// log
					result = result.concat(e.Recipes)
				} else {
					console.log(subset + '\tNO_RECIPE_FOUND', true);
				}
			} catch(err) {
				console.log(subset + '\tNO_RECIPE_FOUND', true);
			}
			loadRecipes(ids, config, cb, result);
		})
	} else {
		cb(result)
	}
}

// getting multiple products does not seem to work!!

function getProducts(ids,cb){
	let cacheKey = 'getProducts_'+ids.toString()
	let cacheConfig = {key:ids.toString(), prefix:'getProducts_', sha:true}
	if(false && Cacher.isCached(cacheConfig)){
		cb(Cacher.getCache(cacheConfig))
	} else {
		// get individual cached recipes from request to shorten request
		ids = ids.toString().split(',')
		loadProducts(ids, function(result){
			let fullResponse = {
			  "Metadata": {
			    "PageCount": 1,
			    "PageNo": 0,
			    "TotalCount": ids.length
			  },
			  "Products": result
			}
			Cacher.setCache(cacheConfig,fullResponse)
			cb(JSON.stringify(fullResponse))
		})
	}
}

function loadProducts(ids, cb, result){
	// console.log('loadProducts')
	if(!result){
		result = []
	}
	if(ids.length>0){
		let id = ids.pop()
		let cacheKey = id
		let cacheConfig = {key:id, prefix:'getProduct_', sha:false}
		let cacheConfigErr = {key:id, prefix:'ERR_getProduct_', sha:false}
		// console.log('cacheConfig',cacheConfig)

		if(Cacher.isCached(cacheConfig)){
			//console.log('loadProducts (local)',id)
			let data = Cacher.getCache(cacheConfig)
			if(Cacher.killCache(cacheConfigErr)){
				console.log('error cache killed',id)
			}
			let e = JSON.parse(data)
			result.push(e)
			loadProducts(ids, cb, result)
		} else if(Cacher.isCached(cacheConfigErr)){
			//console.log('loadProducts (local:err)',id)
			loadProducts(ids, cb, result)
		} else {
			console.log('loadProducts (remote)',id)
			KraftProduct.send({
				rest:{
					productID:id
				}
			}, function(data){
				try {
					let e = JSON.parse(data)
					result.push(e)
					Cacher.setCache(cacheConfig,e)
				} catch(err) {
					console.error('Product failed to load',id)
					//console.error(data)
					Cacher.setCache(cacheConfigErr,{error:data})
				}
				loadProducts(ids, cb, result)
			})
		}
	} else {
		cb(result)
	}
}

function getProduct(id,cb,options){
	// console.log('getProduct',options)
	options = options || {}
	let cacheKey = 'getProduct_'+id
	if(options.cache!==false && Cacher.isCached(cacheKey)){
		cb(Cacher.getCache(cacheKey))
	} else {
		KraftProduct.send({
			rest:{
				productID:id
			}
		}, function(data){
			try {
				parsedData = JSON.parse(data)
			} catch(err) {
				parsedData = {}
				data = '{}'
			}
			Cacher.setCache(cacheKey,parsedData)
			cb(data)
		})
	}
}

function getBrands(cb){
	let cacheKey = 'getBrands'
	//if(Cacher.isCached(cacheKey)){
	//	cb(Cacher.getCache(cacheKey))
	//} else {
		KraftBrands.send({}, function(data){
			try {
				parsedData = JSON.parse(data)
			} catch(err) {
				parsedData = {}
				data = '{}'
			}
			Cacher.setCache(cacheKey,parsedData)
			cb(data)
		})
	//}
}

/*

START: HYDRATED/OPTIMIZED BRANDS AND PRODUCTS REQUEST

*/
function loadBrandProducts(brands,response,cb){
	if(brands.length>0){
		let brand = brands.pop()
		getBrandProducts(brand.BrandID, function(productsJSON){
			brand.products = JSON.parse(productsJSON).Products || []
			response.Brands.push(brand)
			loadBrandProducts(brands,response,cb)
		})
	} else {
		cb(response)
	}
}

function cleanBrandProducts(data){
	return {
		Brands:data.Brands
		.filter(function(brand){
			return brand.products.length>0
		})
		.map(function(brand){
			return {
				ID:brand.BrandID,
				Name:brand.Name,
				Products:brand.products.map(function(product){
					return {
						ID:product.GTIN,
						Name:product.Name
					}
				})
			}
		})
	}
}

function getBrandsWithProducts(cb){
	console.log('getBrandsWithProducts')
	let cacheKey = 'getBrandsWithProducts'
	if(Cacher.isCached(cacheKey)){
		cb(Cacher.getCache(cacheKey))
	} else {
		getBrands(function(data){
			let brands = JSON.parse(data)
			let response = Object.assign({},brands,{Brands:[]})
			loadBrandProducts(brands.Brands,response,function(hydratedBrands){
				hydratedBrands = cleanBrandProducts(hydratedBrands)
				Cacher.setCache(cacheKey,hydratedBrands)
				cb(JSON.stringify(hydratedBrands))
			})
		})
	}
}
/*

END: HYDRATED/OPTIMIZED BRANDS AND PRODUCTS REQUEST

*/

function getBrandProducts(brandId, cb, options){
	options = options || {}
	let cacheKey = 'getBrandProducts_'+brandId
	//if(options.cache!==false && Cacher.isCached(cacheKey)){
	//	cb(Cacher.getCache(cacheKey))
	//} else {
		KraftBrandProducts.send({
			get:{
				bid:brandId
			}
		}, function(data){
			Cacher.setCache(cacheKey,data)
			cb(data)
		})
	//}
}

function getAllProducts(cb){
	getBrands(function(brands){
		brands = JSON.parse(brands).Brands.map(function(brand){
			return {
				ID:brand.BrandID,
				products:[],
				name:brand.Name
			}
		})
		let loadedBrands = brands.length
		
		brands.map(function(brand){
			getBrandProducts(brand.ID, function(products){
				console.log('Loaded',brand.name)
				brand.products = JSON.parse(products).Products
				loadedBrands--
				if(loadedBrands===0){
					brands = brands.filter(function(brand){
						return brand.products.length>0
					})
					cb(brands)
				}
			})
		})
	})
}

module.exports = {
	getBrandsWithProducts:getBrandsWithProducts,
	getRecipes:getRecipes,
	getProduct:getProduct,
	getProducts:getProducts,
	getBrands:getBrands,
	getBrandProducts:getBrandProducts,
	getAllProducts:getAllProducts,
	getRecipeRelated:getRecipeRelated,

	getKraftCategories 		: 	getKraftCategories,
	getRecipeIdsForCategory : 	getRecipeIdsForCategory,
	getRecipeIds 			: 	getRecipeIds
}