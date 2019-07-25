var KraftAPI = require('../api/kraft/')
var ProductSchema = require('./KraftProductsSchema.js')
var ProductCleaner = require('./KraftProductCleanup.js')
//var value = [];

// facet properties
var facets = {
				id 								: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-15' },
				GTIN							: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: true, value: '0-15' },
				kraftid							: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: true, value: '0-15' },
				createdon						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-30' },
				updatedon						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-30' },
				brand 							: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				legaldesignation				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				itemcode						: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-99' },
				manufacturename					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				subbrandname					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				shortdescription				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-99' },
				//variant 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
				name 							: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-999' },
				description 					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-999' },
				qualitystatment					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				storageinstructions				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				temperatureclassification		: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-999' },
				ecommdivision					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				ecommproductfeature				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-999' },
				markedreturnable				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: true, value: 'true-false-false' },
				prepcooksuggestions				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-999' },
				prodcutbenefit					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-999' },
				ecommproductcategory			: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				subcategory						: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				shelflife						: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				nutritioncontains				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-999' },
				nutritionservingsize			: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-999' },
				nutritioningredients 			: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-999' },
				servingsoftradeitemunit			: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				//longdescription 				: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-99' },
				productid						: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-15' },
				productname						: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-999' },				
				image 							: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				//alt_image 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				imagedamurl						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				//altimagesalsifyurl				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },				
				declarednetcontentuomcode		: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-5' },
				healthclaims 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value:  '0-999'},
				entrytype 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9' },
				producttype 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: true, value: '0-9' },
				isproduct 						: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: false, sortable: false, value: 'true-false-false' },
				unit 							: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
				quantity 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
				nutritionitemname 				: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
				//sizevariantoz 					: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
				//count 							: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
				//defaultproductgtin 				: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
				ingrediants_productid 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: false, value: [] },
				//Arul - Start
				divisioncatalystname 			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				divisioncatalyst	 			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				consumerunitsizeuom 			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				consumerunitsize 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				tags 							: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
				keywords						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
				phiddensearchterms 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				marketingdescription			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				assets 							: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				variationlabel1					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				variationlabel2					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				//primaryimageurlcode				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				//secondaryimageurlcode			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				hasimage	 					: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: true, sortable: false, value: 'true-false-false' },
				//productvariants					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				//categorylevel1					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				//categorylevel2					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				//categorylevel3					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				nutritionallergens				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				sizevariants					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				sizevariants2					: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' },
				seoname							: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				numberofratings					: 	{ fieldtype: 0, validationtype: 1, searchable: true, filterable: true, sortable: false, value:  '0-9999'},
				averagerating 					: 	{ fieldtype: 0, validationtype: 2, searchable: true, filterable: true, sortable: false, value:  '0-5-0'},
				activestatus					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
				semanticId	 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				//primaryimageassetreferenceid	: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-99' },
				//secondaryimageassetreferenceid	: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: false, sortable: false, value: '0-99' },
				//tagsfull 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				categorylevelmap				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				categorylevelmapbrandpipe			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
				pid								: 	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: true, value: '0-99' }
				//Arul - End
			};


function addProducts(productIds, workbook, cb, options){
	importProducts(productIds, function(products){
		products.map(function(product){
			addProduct(product,workbook)
		})
		if(cb) cb(products)
	},[],options)
}

function importProducts(productIds, cb, result, options){
	if(!result){
		result = []
	}
	if(!(productIds instanceof Array)){
		productIds = String(productIds).split(',')
	}
	// remove dupes
	productIds = productIds.filter(function(e,i,a){
		return a.indexOf(e)===i
	})

	if(productIds.length>0){
		let productId = productIds.pop()
		KraftAPI.getProduct(productId, function(response){
			if(response){
				console.log('product loaded',productId,productIds.length)
				ProductCleaner.cleanup(JSON.parse(response), function(cleanProduct){
					result.push(cleanProduct)
					importProducts(productIds,cb,result,options)
				})
			}
		},options)
	} else {
		if(cb) cb(result)
	}
}

function addProduct(categoryid, config, product, workbook){

	//console.log(product.GTIN);

	//
	// Product Master
	//
	workbook.insertRows('productmaster',{
		defaultparentcategoryid :  categoryid,
		productid : product.GTIN
	}, ProductSchema);

	// insert product master properties
	//var props = [];
	let props = [];
	for(var propName in product.__properties){
		if( product.__properties[propName])
		{
			props.push({
				name:propName,
				value:product.__properties[propName]
			})
		}
	};

	// reviews
	if(config && config.sheetDataArr && config.sheetDataArr[product.GTIN])
	{
		//console.log(reviews[i].name +' == '+ reviews[i].value);
		//props.push(reviews[i]);
		if(config.sheetDataArr[product.GTIN].ar)
		{
			props.push({"name":"averagerating","value":config.sheetDataArr[product.GTIN].ar});
			//console.log(product.GTIN + " " + config.sheetDataArr[product.GTIN].ar);
		}
		if(config.sheetDataArr[product.GTIN].nr)
		{
			props.push({"name":"numberofratings","value":config.sheetDataArr[product.GTIN].nr});
			//console.log(product.GTIN + " " + config.sheetDataArr[product.GTIN].nr);
		}
		if(config.sheetDataArr[product.GTIN].pid)
		{
			props.push({"name":"pid","value":config.sheetDataArr[product.GTIN].pid});
			//console.log(product.GTIN + " " + config.sheetDataArr[product.GTIN].pid);
		}
	}

	props.push({
		name:'entrytype',
		value:'product'
	});
	props.push({
		name:'producttype',
		value:'1'
	});
	props.push({
		name:'isproduct',
		value:'true'
	});

	// process Facetmaster and master properties

	if (product.SizeVariants && product.SizeVariants.length) {
		var newVariantObj = product.SizeVariants.filter(function(variantObj) {
			return variantObj.DefaultProductGTIN == product.GTIN;
		});

		props = props.concat(getProperties(newVariantObj));
	}

	props.map((prop) => {
		return Object.assign(prop, { 
			productid 	: 	product.ID
		});
	});

	if (typeof facets.variant != "undefined" && typeof facets.variant.value != "undefined" && facets.variant.value.indexOf(product.__properties.variant) == -1) {
		facets.variant.value.push(product.__properties.variant);
	}

	if (typeof facets.ingrediants_productid != "undefined" && typeof facets.ingrediants_productid.value != "undefined" && facets.ingrediants_productid.value.indexOf(product.__properties.ingrediants_productid) == -1) {
		facets.ingrediants_productid.value.push(product.__properties.ingrediants_productid);
	}
	
	if(typeof product.__properties.unit != "undefined")
	addUniqueInFacet('unit', product.__properties.unit);
	if(typeof product.__properties.quantity != "undefined")
	addUniqueInFacet('quantity', product.__properties.quantity);
	if(typeof product.__properties.nutritionitemname != "undefined")
	addUniqueInFacet('nutritionitemname', product.__properties.nutritionitemname);
	if(typeof product.__properties.tags != "undefined")
	addUniqueInFacet('tags', product.__properties.tags);
	if(typeof product.__properties.keywords != "undefined")
	addUniqueInFacet('keywords', product.__properties.keywords);
	
	workbook.insertRows('productmasterproperties', props, ProductSchema);

	//
	// sku
	//	
	workbook.insertRows('skumaster', product, ProductSchema);

	//console.log(props);

	
	// 00013000000277
}

function addFacet(workbook, callback) {
	for (var facet in facets) {
		facets[facet].name = facet;

		workbook.insertRows('facetmaster', facets[facet], ProductSchema);
		
		//console.log(facets[facet].value);

		/*if(facets[facet].fieldtype == 2)
		{
			console.log(facet+" == "+facets[facet].value);
		}*/
		
		workbook.insertRows('facetmasterproperties', [
			{ 
				name: 'name',
				value: facet
			},
			{ 
				name: 'validationData',
				value: (facets[facet].fieldtype == 2) ? facets[facet].value.join(',') : facets[facet].value
			}
		].map((prop) => {
			return Object.assign(prop, { 
				facetname 	: 	facet
			});
		}), ProductSchema);			
	}

	callback();
}

/***** Category added **************/
function addCategory(category,workbook){

	//console.log('category : '+ category.toString());
	

	// insert category master
	if(category.top !== 'top')
	{
		workbook.insertRows('categorymaster', {
			categoryid :  category.top
		}, ProductSchema);

		var properties = [
			{
				name 	: 	'seo_friendly_name',
				value 	: 	category.topSeo
			},
			{
				name 	: 	'name',
				value 	: 	category.topName
			},
			{
				name 	: 	'image',
				value 	: 	'[{"image":"//cdn-ap-ec.yottaa.net/5637a31d312e585f7100086c/www.kraftrecipes.com/v~22.156/-/media/assets/2017-summer/ultimate-grilled-steak-92238-600x250.jpg?h=250&w=600&la=en&hash=256C70B789629FB0D18F8795F3EE784DD0268C85&yocs=1V_1Y_2b_&yoloc=ap","name":"Summer BBQ Recipes","label":"Get the party going with BBQ recipes for ribs, chicken, burgers, steak and more!"},{"image":"//cdn-ap-ec.yottaa.net/5637a31d312e585f7100086c/www.kraftrecipes.com/v~22.156/-/media/images/kr/imagerepository/apr18/hp30/rr2-580x250-043018.jpg?h=250&w=580&la=en&hash=C6076EF4BF09655D1C190E538912508CA7166124&yocs=1V_1Y_2b_&yoloc=ap","name":"Cold Appetizer Recipes","label":"For TV snacking or party time, these cold appetizer recipes are hot stuff!"},{"image":"//cdn-ap-ec.yottaa.net/5637a31d312e585f7100086c/www.kraftrecipes.com/v~22.156/-/media/assets/recipe_images/pay-glaseado-de-fresas-124503-600x250.jpg?h=250&w=600&la=en&hash=FEAA6F14697F7CA43B9046B4EE7E724B48B211A0&yocs=1V_1Y_2b_&yoloc=ap","name":"Strawberry Recipes","label":"Throw your own strawberry festival at home with these terrific strawberry recipes."},{"image":"//cdn-ap-ec.yottaa.net/5637a31d312e585f7100086c/www.kraftrecipes.com/v~22.156/-/media/assets/2018-summer/slow-cooker-shredded-chicken-recipe-211804-600x250.jpg?h=250&w=600&la=en&hash=6FBB6E5246C2C7168D736DD609EAE4DB3150DB4F&yocs=1V_1Y_2b_&yoloc=ap","name":"Easy Slow-Cooker Recipes","label":"Busy days ahead? Dust off that slow-cooker and put it to work!"}]'
			},
			{
				name 	: 	'description',
				value 	: 	'[{"value":"Welcome to the Kraft recipes hub! Our aim is to bring you the very best recipes for everything you\u2019re looking for\u2014whether it\u2019s inspiration for aÂ holiday menuÂ or something great forÂ dinner tonight. You will be able to find recipes for tried-and-true favorites, as well as for trying your hand at new and interesting ideas! Expand your talents with our cooking school videos. Our recipe box is your recipe box. Enjoy!Â "}]'
			},
			{
				name 	: 	'metainfo',
				value 	: 	'[{"description":"HTML Tidy for Java (vers. 2009-12-01), see jtidy.sourceforge.net","title":"generator"},{"description":"IE=edge"},{"description":"Recipes: Browse All of Our Recipes - Kraft Recipes","title":"title"},{"description":"Browse recipes for any time of day with help from Kraft Recipes. Explore our recipes for breakfast, lunch, dinner, snacks, holidays and more.","title":"description"},{"description":"/-/media/assets/recipe_images/bacon-omelet-roll-115324-640x428.jpg","title":"pageimage"},{"description":"kids","title":"metacategory"},{"description":"dinner","title":"metacategory"},{"description":"130791333621603","title":"fb:app_id"},{"description":"kraftrecipes.com","title":"fb:site_name"},{"description":"Recipes: Browse All of Our Recipes","title":"og:title"},{"description":"http://www.kraftrecipes.com/-/media/assets/recipe_images/bacon-omelet-roll-115324-640x428.jpg","title":"og:image"},{"description":"http://www.kraftrecipes.com/recipes.aspx","title":"og:url"},{"description":"640","title":"og:image:width"},{"description":"428","title":"og:image:height"},{"description":"Browse recipes for any time of day with help from Kraft Recipes. Explore our recipes for breakfast, lunch, dinner, snacks, holidays and more.","title":"og:description"},{"description":"Recipes","title":"og:site_name"},{"description":"summary","title":"twitter:card"},{"description":"@kraftfoods","title":"twitter:site"},{"description":"Recipes: Browse All of Our Recipes","title":"twitter:title"},{"description":"Browse recipes for any time of day with help from Kraft Recipes. Explore our recipes for breakfast, lunch, dinner, snacks, holidays and more.","title":"twitter:description"},{"description":"http://www.kraftrecipes.com/-/media/assets/recipe_images/bacon-omelet-roll-115324-640x428.jpg","title":"twitter:image"},{"description":"http://www.kraftrecipes.com/recipes.aspx","title":"twitter:url"},{"description":"width=device-width,user-scalable=no,minimum-scale=1,maximum-scale=1","title":"viewport"},{"description":"none","title":"msapplication-config"}]'
			}

		];

		properties.map((prop) => {
			return Object.assign(prop, { 
				categoryid 	: 	category.top
			});
		});

		// category properties
		workbook.insertRows('categorymasterproperties', properties, ProductSchema);

		var propertiesCat = [
			{
				name 	: 	'seo_friendly_name',
				value 	: 	category.subCatSeo
			},
			{
				name 	: 	'name',
				value 	: 	category.subCatName
			},
			{
				name 	: 	'image',
				value 	: 	'[{"image":"//cdn-ap-ec.yottaa.net/5637a31d312e585f7100086c/www.kraftrecipes.com/v~22.156/-/media/assets/festive15_heroes/monster-claws-dipping-sauce-114127-580x250.jpg?yocs=1V_1Y_2b_&yoloc=ap","name":"Monster Claws with Dipping Sauce"}]'
			},
			{
				name 	: 	'description',
				value 	: 	'[{"value":"Lets face it, some Halloween costumes are simply not meant for sit-down dining. So unless you\u2019re planning an elegant Halloween dinner menu for grown-ups, you\u2019ll want to be sure to have lots of Halloween finger foods on your Halloween party menu! These Halloween finger foods may include Halloween appetizers like bat wings (chicken wings in disguise!) and Halloween snack recipes. And it can also include Halloween treats: spooky Halloween cookies, mini cupcakes and more. For ideas beyond fun Halloween finger foods,, check out our Halloween Party Planning Tips."}]'
			},
			{
				name 	: 	'metainfo',
				value 	: 	'[{"description":"HTML Tidy for Java (vers. 2009-12-01), see jtidy.sourceforge.net","title":"generator"},{"description":"IE=edge"},{"description":"Halloween Finger Foods - Kraft Recipes","title":"title"},{"description":"Please your party guests with a variety of Halloween finger foods. From themed chicken wings to cookie balls, find the perfect Halloween finger foods here.","title":"description"},{"description":"/-/media/assets/festive15_heroes/monster-claws-dipping-sauce-114127-642x428.jpg","title":"pageimage"},{"description":"kids","title":"metacategory"},{"description":"dinner","title":"metacategory"},{"description":"1","title":"VIDEO_LANGUAGEID"},{"description":"130791333621603","title":"fb:app_id"},{"description":"kraftrecipes.com","title":"fb:site_name"},{"description":"Halloween Finger Foods","title":"og:title"},{"description":"http://www.kraftrecipes.com/-/media/assets/festive15_heroes/monster-claws-dipping-sauce-114127-642x428.jpg","title":"og:image"},{"description":"http://www.kraftrecipes.com/recipes/holidays-and-entertaining/holidays/halloween/halloween-finger-foods.aspx","title":"og:url"},{"description":"642","title":"og:image:width"},{"description":"428","title":"og:image:height"},{"description":"Please your party guests with a variety of Halloween finger foods. From themed chicken wings to cookie balls, find the perfect Halloween finger foods here.","title":"og:description"},{"description":"Recipes","title":"og:site_name"},{"description":"summary","title":"twitter:card"},{"description":"@kraftfoods","title":"twitter:site"},{"description":"Halloween Finger Foods","title":"twitter:title"},{"description":"Please your party guests with a variety of Halloween finger foods. From themed chicken wings to cookie balls, find the perfect Halloween finger foods here.","title":"twitter:description"},{"description":"http://www.kraftrecipes.com/-/media/assets/festive15_heroes/monster-claws-dipping-sauce-114127-642x428.jpg","title":"twitter:image"},{"description":"http://www.kraftrecipes.com/recipes/holidays-and-entertaining/holidays/halloween/halloween-finger-foods.aspx","title":"twitter:url"},{"description":"width=device-width,user-scalable=no,minimum-scale=1,maximum-scale=1","title":"viewport"},{"description":"none","title":"msapplication-config"}]'
			}
		];

		propertiesCat.map((prop) => {
			return Object.assign(prop, { 
				categoryid 	: 	category.subCat
			});
		});

		// category properties
		workbook.insertRows('categorymasterproperties', propertiesCat, ProductSchema);


		// product category mapping
		workbook.insertRows('productcategory', {
			categoryid :  category.top,
			productid : category.GTIN.toString().length == 14 ? category.GTIN : '000'+category.GTIN,
		}, ProductSchema);
	}
	else
	{
		// category master
		workbook.insertRows('categorymaster', {
			categoryid :  category.subCat,
			parentcategoryid : category.top,
		}, ProductSchema);

		var properties = [
			{
				name 	: 	'name',
				value 	: 	category.topName
			}
		];

		properties.map((prop) => {
			return Object.assign(prop, { 
				categoryid 	: 	category.subCat
			});
		});

		// category properties
		workbook.insertRows('categorymasterproperties', properties, ProductSchema);
	}
}



function addProduct1(categoryid, product, workbook){

	//console.log(product.GTIN);

	//
	// Product Master
	//
	workbook.insertRows('productmaster',  {
		defaultparentcategoryid :  categoryid,
		productid : product.GTIN
	}, ProductSchema);

	// insert product master properties
	let props = [];
	for(var propName in product.__properties){
		props.push({
			productid:product.ID,
			name:propName,
			value:product.__properties[propName]
		})
	};


	props.push({
		productid:product.ID,
		name:'entrytype',
		value:'product'
	});
	props.push({
		productid:product.ID,
		name:'producttype',
		value:'1'
	});
	props.push({
		productid:product.ID,
		name:'isproduct',
		value:'true'
	});

	workbook.insertRows('productmasterproperties', props, ProductSchema);

	//
	// product category mapping
	//
	//workbook.insertRows('productcategory', product, ProductSchema);
	// product category mapping
	workbook.insertRows('productcategory', {
		categoryid :  categoryid,
		productid : product.GTIN
	}, ProductSchema);

	//
	// sku
	//	
	workbook.insertRows('skumaster', product, ProductSchema);

	//
	// get collecton product gtins
	//
	/*let subproductids = product.RiseIngredients
	.filter(function(ingredient){
		return ingredient.ProductId
	}).map(function(ingredient){
		return ingredient.ProductId
	})*/

	// return subproductids
	//return facetProperties;
}

function getProperties(value) {
	var result = [],
		resultObj = {};
	value.map(function(obj, index) {
		
		for (var property in obj) {
			var key = property.toLowerCase(),
				val = obj[property];

			if (val == null) {
				val = 'EMPTY';
			} else if (Number.isInteger(val)) {
				val = val.toString();
			} else if (val == '') {
				val = 'EMPTY';
			} else if (val && (val.toString().trim() == '')) {
				val = 'EMPTY';
			} else if (val) {
				val = val.toString().trim();
			}
			val = val.replace(/,/g, '|'); // replace , with |
			if (!(key in resultObj)) {
				resultObj[key] = val; // escape('')
			} else {
				if (resultObj[key]) {
					resultObj[key] = resultObj[key] + ',' + val;
				} else {
					resultObj[key] = val;
				}
			}
			
			if (!(key in facets)) {
				facets[key] = { fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [ val ] };
			} else {
				if (facets[key].value.indexOf(val) == -1) {
					facets[key].value.push(val);
				}
			}
		}
	});
	for (var properties in resultObj) {
		result.push({ name: properties, value: resultObj[properties] });
	}
	return result;
}


function addUniqueInFacet(propName, propValue) {
	var splitValues = propValue.split(',');
	splitValues.map(function(property) {
		if (facets[propName].value.indexOf(property) == -1) {
			facets[propName].value.push(property);
		}
	});
}

module.exports = {
	addProducts:addProducts,
	addProduct : addProduct,
	addProduct1 : addProduct1,
	addFacet 	: 	addFacet,
	addCategory : addCategory
}