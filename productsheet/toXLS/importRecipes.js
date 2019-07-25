var KraftAPI = require('../api/kraft/')
var RecipeSchema = require('./KraftRecipesSchema.js')
var facetProperties = [];

function addRecipes(recipeIds, workbook, cb){
	let subproductids = []
	importRecipes(recipeIds, function(recipes){
		recipes.map(function(recipe){
			if(recipe && recipe.RecipeId){
				let productids = addRecipe(recipe,workbook)
				// we need to collect the related products for import 
				subproductids = subproductids.concat(productids)
			}
		})
		if(cb) cb(recipes,subproductids)
	})
}

function importRecipes(recipeIds, cb, result){
	KraftAPI.getRecipes(recipeIds, function(response){
		cb(JSON.parse(response).Recipes)
	})
}

function addRecipe(recipe, workbook){

	console.log(recipe.RecipeId);

	//
	// Product Master
	//
	workbook.insertRows('productmaster', recipe, RecipeSchema);

	//
	// CATEGORIES
	//

	/*let primaryCategories = recipe.Categories.filter(function(category){
		return category.CategoryName==='Primary'
	})
	let PrimaryCategory = ''

	if(primaryCategories.length===0 && recipe.Categories.length>0){
		//primaryCategories = [recipe.Categories[0]]
		PrimaryCategory = recipe.Categories[0].CategoryName
	} else {
		PrimaryCategory = primaryCategories[0].SubCategoryName
	}*/

	//
	// Product Properties
	//
	var properties = [
		{
			name 	: 	'product_type',
			value 	: 	0
		},
		// ex: 'some content'
		{
			name 	: 	'ad_serving_keywords',
			value 	: 	recipe.AdServingKeywords
		},
		{
			name 	: 	'AverageRating',
			value 	: 	recipe.AverageRating
		},
		{
			name 	: 	'brandid',
			value 	: 	recipe.BrandId
		},
		{
			name 	: 	'diet_exchange',
			value 	: 	recipe.DietExchange
		},
		{
			name 	: 	'end_date',
			value 	: 	recipe.EndDate ? new Date(parseInt(recipe.EndDate.substr(6))).getTime() : ''
		},
		{
			name 	: 	'hasVideo',
			value 	: 	recipe.HasVideo
		},
		{
			name 	: 	'is_healthy_living',
			value 	: 	recipe.IsHealthyLiving
		},
		{
			name 	: 	'keywords',
			value 	: 	recipe.Keywords
		},
		{
			name 	: 	'krl_id',
			value 	: 	recipe.KRLId
		},
		{
			name 	: 	'language_id',
			value 	: 	recipe.LanguageId
		},
		{
			name 	:  	'number_of_ingredients',
			value 	: 	recipe.NumberOfIngredients
		},
		{
			name 	: 	'number_of_ratings',
			value 	: 	recipe.NumberOfRatings
		},
		{
			name 	: 	'number_of_ratings_with_comments',
			value 	: 	recipe.NumberOfRatingsWithComments
		},
		{
			name 	: 	'number_of_servings',
			value 	: 	recipe.NumberOfServings
		},
		{
			name 	: 	'nutrition_bonus',
			value 	: 	recipe.NutritionBonus
		},
		{
			name 	: 	'nutrition_exchange_item_id',
			value 	: 	recipe.NutritionExchangeItemId
		},
		{
			name 	: 	'preparation_description',
			value 	: 	recipe.PreparationDescription
		},
		{
			name 	: 	'preparation_pre_text',
			value 	: 	recipe.PreparationPreText
		},
		{
			name 	: 	'PreparationTime',
			value 	: 	recipe.PreparationTime
		},
		{
			name 	: 	'recipe_display_format_id',
			value 	: 	recipe.RecipeDisplayFormatId
		},
		{
			name 	: 	'recipeid',
			value 	: 	recipe.RecipeId
		},
		{
			name 	: 	'Name',
			value 	: 	recipe.RecipeName
		},
		{
			name 	: 	'recipe_type',
			value 	: 	recipe.RecipeType
		},
		{
			name 	: 	'romance_text',
			value 	: 	recipe.RomanceText
		},
		{
			name 	: 	'seo_name',
			value 	: 	recipe.SEOName
		},
		{
			name 	: 	'TotalTime',
			value 	: 	recipe.TotalTime
		},
		{
			name 	: 	'yield',
			value 	: 	recipe.Yield
		},
		// ex: [ 'message1', 'message2' ]
		{
			name 	: 	'courses',
			value 	: 	(recipe.Courses && recipe.Courses.length) ? recipe.Courses.join(',') : ''
		},
		{
			name 	: 	'key_ingredients',
			value 	: 	(recipe.KeyIngredients && recipe.KeyIngredients.length) ? recipe.KeyIngredients.join(',') : ''
		},
		{
			name 	: 	'life_styles',
			value 	: 	(recipe.LifeStyles && recipe.LifeStyles.length) ? recipe.LifeStyles.join(',') : ''
		},
		// json string
		{
			name 	: 	'assets',
			value 	: 	(recipe.Assets && recipe.Assets.length) ? JSON.stringify(recipe.Assets) : ''
		},
		{
			name 	: 	'preparationsteps',
			value 	: 	(recipe.PreparationSteps && recipe.PreparationSteps.length) ? JSON.stringify(recipe.PreparationSteps) : ''
		},
		{
			name 	: 	'ratings',
			value 	: 	recipe.Ratings ? JSON.stringify(recipe.Ratings) : ''
		},
		{
			name 	: 	'relateddata',
			value 	: 	recipe.RelatedData ? JSON.stringify(recipe.RelatedData) : ''
		},
		{
			name 	: 	'video',
			value 	: 	(recipe.Video && recipe.Video.length) ? JSON.stringify(recipe.Video) : ''
		}
		// name:'Image', value:((recipe.Assets.filter(asset=>{return asset.imageType==='Primary'}).pop() || {PictureFormats:[{rendition:'640x428', url:''}]}).PictureFormats.filter(pic=>{return pic.rendition==='640x428'}).pop() || {url:''}).url
	];

	
	properties.map(function(props) {
		if (!props.value) {
			console.log('name: ' + props.name);
		}
	});

	// ex: [ { key1: value1, key2: value2 }, { key1: value1, key2: value2 }... ]
	properties = properties.concat(getProperties('classifications', recipe.Classifications));
	properties = properties.concat(getProperties('complimentaryrecipes', recipe.ComplimentaryRecipes));
	properties = properties.concat(getProperties('cssskins', recipe.CSSSkins));
	properties = properties.concat(getProperties('dishdetails', recipe.DishDetails));  // tst
	properties = properties.concat(getProperties('ingredients', recipe.Ingredients));
	properties = properties.concat(getProperties('nutritionitems', recipe.NutritionItems));
	properties = properties.concat(getProperties('riseingredients', recipe.RiseIngredients));
	properties = properties.concat(getProperties('riserecipe', recipe.Riserecipe));
	properties = properties.concat(getProperties('tips', recipe.Tips));

	// alt recipe id
	if (recipe.AlternateLanguageRecipes && recipe.AlternateLanguageRecipes.length) {
		var altRecipes = recipe.AlternateLanguageRecipes.map(function(altRecipeObj) {
			return {
				name 	: 	'alternaterecipe_' + altRecipeObj.LanguageId + '_' + altRecipeObj.SiteId,
				value 	: 	altRecipeObj.RecipeId
			};
		});

		properties = properties.concat(altRecipes);
	}

	properties.map((prop) => {
		return Object.assign(prop, { 
			productId 	: 	recipe.RecipeId,
			languageId 	: 	recipe.LanguageId
		});
	});

	workbook.insertRows('productmasterproperties', properties, RecipeSchema);

	//
	// product category mapping
	//
	workbook.insertRows('productcategory', (recipe.RiseTaxonomy || []).map(function(obj) {
		return Object.assign(obj, { 
			productId 	: 	recipe.RecipeId
		});
	}), RecipeSchema);

	//
	// sku
	//	
	workbook.insertRows('skumaster', recipe, RecipeSchema);

	//
	// get collecton product gtins
	//
	let subproductids = recipe.RiseIngredients
	.filter(function(ingredient){
		return ingredient.ProductId
	}).map(function(ingredient){
		return ingredient.ProductId
	})

	// return subproductids
	return facetProperties;
}

function getProperties(propName, value) {
	value = value || [],
	result = [],
	resultObj = {};

	value.map(function(obj, index) {
		for (var property in obj) {
			if (!(propName + '_' + property.toLowerCase() in resultObj)) {
				resultObj[propName + '_' + property.toLowerCase()] = obj[property] ? escape(obj[property].toString()) : null;

				if (facetProperties.indexOf(propName + '_' + property.toLowerCase()) == -1) {
					facetProperties.push(propName + '_' + property.toLowerCase());
				}
			} else {
				resultObj[propName + '_' + property.toLowerCase()] = resultObj[propName + '_' + property.toLowerCase()] + ',' + (obj[property] ? escape(obj[property].toString()) : null);
			}
		}
	});

	for (var properties in resultObj) {
		result.push({ name: properties, value: resultObj[properties] });
	}

	return result;
}

function addCategory(data, workbook, callback) {
	data = data || {};

	for (var category in data) {
		var splits = category.split('_');

		workbook.insertRows('categorymaster', {
			categoryId 		: 	splits[0],
			parentCategoryId: 	splits[1]
		}, RecipeSchema);
	}

	callback();
}

function addCategoryProperties(data, workbook, callback) {
	data = data || [];

	data.map(function(obj, index) {
		var properties = [
			{ 
				name 	: 	'name',
				value 	: 	obj.name
			},
			{
				name 	: 	'type',
				value 	: 	obj.type
			}
		].map((prop) => {
			return Object.assign(prop, { 
				categoryId 	: 	obj.categoryId, 
				languageId 	: 	obj.languageId
			});
		});
		
		workbook.insertRows('categorymasterproperties', properties, RecipeSchema);
	});

	callback();
}

function addFacet(facets, languageId, workbook) {
	facets = facets || [];

	facets.map(function(facet, index) {
		workbook.insertRows('facetmaster', { name: facet, languageId: languageId }, RecipeSchema);
		workbook.insertRows('facetmasterproperties', [
			{
				name 		: 	'name',
				value 		: 	facet
			},
			{
				name 		: 	'validationData',
				value 		: 	'0-9999'
			}
		].map((prop) => {
			return Object.assign(prop, { 
				facetname 	: 	facet, 
				languageId 	: 	languageId
			});
		}), RecipeSchema);
	});
}

module.exports = {
	addRecipes:addRecipes,
	addRecipe 	: 	addRecipe,
	addCategory : 	addCategory,
	addCategoryProperties: 	addCategoryProperties,
	addFacet 	: 	addFacet
}