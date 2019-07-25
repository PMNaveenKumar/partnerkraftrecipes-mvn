var KraftAPI = require('../api/kraft/')
var RecipeSchema = require('./KraftRecipesSchema.js');
var locale = { '1': 'en_US', '2': 'es_ES' };

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

function addRecipe(recipe, workbook, config, facets, cbk) {
	console.log(recipe.RecipeId + '\t', false);

	//
	// Product Master
	//
	if (!config.altDetails) {
		workbook.insertRows('productmaster', recipe, RecipeSchema);
	}

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
	var properties = [];

	if (recipe.AdServingKeywords) {
		var tagSplit = recipe.AdServingKeywords.split(','),
			newTags = [];

		tagSplit.map(function(tag) {
			newTags.push(trimPropertyValue(tag));
		});

		properties.push({ name: 'tags', value: newTags.join(',') });

		newTags.map(function(newTag) {
			if (facets.tags.value.indexOf(newTag) == -1) {
				facets.tags.value.push(newTag);
			}
		});
	} else {
		console.log('AdServingKeywords, ', false);
	}

	if (recipe.AlternateLanguageRecipes && recipe.AlternateLanguageRecipes.length) {		
		properties.push({ name: 'alternatelanguagerecipes', value: JSON.stringify(recipe.AlternateLanguageRecipes) });
	} else {
		console.log('AlternateLanguageRecipes, ', false);
	}

	if (recipe.Assets && recipe.Assets.length) {
		properties.push({ name: 'assets', value: JSON.stringify(recipe.Assets) });
	} else {
		console.log('Assets, ', false);
	}

	if (recipe.AverageRating != null) {
		properties.push({ name: 'averagerating', value: recipe.AverageRating });
	} else {
		console.log('AverageRating, ', false);
	}

	if (recipe.BrandId != null) {
		properties.push({ name: 'brandid', value: recipe.BrandId });
	} else {
		console.log('BrandId, ', false);
	}

	if (recipe.Classifications && recipe.Classifications.length) {
		properties = properties.concat(getProperties(recipe.Classifications, true, true, true, facets));
	} else {
		console.log('Classifications, ', false);
	}

	if (recipe.ComplimentaryRecipes && recipe.ComplimentaryRecipes.length) {
		properties.push({ name: 'complimentaryrecipes', value: JSON.stringify(recipe.ComplimentaryRecipes) });
	} else {
		console.log('ComplimentaryRecipes, ', false);
	}

	if (recipe.Courses && recipe.Courses.length) {
		var newCourses = [];
		
		recipe.Courses.map(function(course) {
			newCourses.push(trimPropertyValue(course));
		});

		properties.push({ name: 'courses', value: newCourses.join(',') });

		newCourses.map(function(newCourse) {
			if (facets.courses.value.indexOf(newCourse) == -1) {
				facets.courses.value.push(newCourse);
			}
		});
	} else {
		console.log('Courses, ', false);
	}

	if (recipe.DietExchange != null) {
		properties.push({ name: 'dietexchange', value: recipe.DietExchange });
	} else {
		console.log('DietExchange, ', false);
	}

	properties.push({ name: 'hasvideo', value: recipe.HasVideo });

	if (recipe.Ingredients && recipe.Ingredients.length) {
		properties.push({ name: 'ingredients', value: JSON.stringify(recipe.Ingredients) });

		properties = properties.concat(getSingleProperties('ingredientname', 'IngredientName', recipe.Ingredients, true, false, true, facets));
	} else {
		console.log('Ingredients, ', false);
	}

	properties.push({ name: 'ishealthy', value: recipe.IsHealthyLiving });

	if (recipe.KeyIngredients && recipe.KeyIngredients.length) {
		properties.push({ name: 'keyingredients', value: JSON.stringify(recipe.KeyIngredients) });
	} else {
		console.log('KeyIngredients, ', false);
	}

	if (recipe.Keywords != null) {
		var keywords = recipe.Keywords.split(','),
			newKeywords = [];

		keywords.map(function(keyword) {
			newKeywords.push(trimPropertyValue(keyword));
		});

		properties.push({ name: 'keywords', value: newKeywords.join(',') });
		
		newKeywords.map(function(newKeyword) {
			if (facets.keywords.value.indexOf(newKeyword) == -1) {
				facets.keywords.value.push(newKeyword);
			}
		});
	} else {
		console.log('Keywords, ', false);
	}

	if (recipe.LifeStyles && recipe.LifeStyles.length) {
		properties.push({ name: 'lifestyles', value: JSON.stringify(recipe.LifeStyles) });
	} else {
		console.log('LifeStyles, ', false);
	}

	if (recipe.NumberOfRatings != null) {
		var ratingsValue = trimPropertyValue(recipe.NumberOfRatings);

		properties.push({ name: 'numberofratings', value: ratingsValue });

		if (facets.numberofratings.value.indexOf(ratingsValue) == -1) {
			facets.numberofratings.value.push(ratingsValue);
		}
	} else {
		console.log('NumberOfRatings, ', false);
	}

	if (recipe.NumberOfRatingsWithComments != null) {
		properties.push({ name: 'numberofratingswithcomments', value: recipe.NumberOfRatingsWithComments });
	} else {
		console.log('NumberOfRatingsWithComments, ', false);
	}

	if (recipe.NumberOfServings != null) {
		var servingsVal = trimPropertyValue(recipe.NumberOfServings);

		properties.push({ name: 'numberofservings', value: servingsVal });

		if (facets.numberofservings.value.indexOf(servingsVal) == -1) {
			facets.numberofservings.value.push(servingsVal);
		}
	} else {
		console.log('NumberOfServings, ', false);
	}

	if (recipe.NutritionItems && recipe.NutritionItems.length) {
		properties = properties.concat(getProperties(recipe.NutritionItems, true, false, true, facets));
	} else {
		console.log('NutritionItems, ', false);
	}

	if (recipe.PreparationDescription != null) {
		properties.push({ name: 'preparationdescription', value: recipe.PreparationDescription });
	} else {
		console.log('PreparationDescription, ', false);
	}

	if (recipe.PreparationPreText) {
		properties.push({ name: 'preparationpretext', value: recipe.PreparationPreText });
	} else {
		console.log('PreparationPreText, ', false);
	}

	if (recipe.PreparationSteps && recipe.PreparationSteps.length) {
		properties.push({ name: 'preparationsteps', value: JSON.stringify(recipe.PreparationSteps) });

		properties = properties.concat(getSingleProperties('description', 'Description', recipe.PreparationSteps, true, false, false, facets));
	} else {
		console.log('PreparationSteps, ', false);
	}

	if (recipe.PreparationTime != null) {
		var preparationTimeVal = trimPropertyValue(recipe.PreparationTime);

		properties.push({ name: 'preparationtime', value: preparationTimeVal });

		if (facets.preparationtime.value.indexOf(preparationTimeVal) == -1) {
			facets.preparationtime.value.push(preparationTimeVal);
		}
	} else {
		console.log('PreparationTime, ', false);
	}

	if (recipe.RecipeName) {
		var nameVal = trimPropertyValue(recipe.RecipeName);

		properties.push({ name: 'name', value: nameVal });

		if (facets.name.value.indexOf(nameVal) == -1) {
			facets.name.value.push(nameVal);
		}
	} else {
		console.log('RecipeName, ', false);
	}

	if (recipe.RecipeType != null) {
		properties.push({ name: 'recipetype', value: recipe.RecipeType });
	} else {
		console.log('RecipeType, ', false);
	}

	if (recipe.RiseIngredients && recipe.RiseIngredients.length) {
		properties.push({ name: 'riseingredients', value: JSON.stringify(recipe.RiseIngredients) });

		properties = properties.concat(getSingleProperties('ingredienttype', 'IngredientType', recipe.RiseIngredients, true, true, true, facets));
		properties = properties.concat(getSingleProperties('ingrediants_productid', 'ProductId', recipe.RiseIngredients, true, true, true, facets));
	} else {
		console.log('RiseIngredients, ', false);
	}

	if (recipe.Riserecipe && recipe.Riserecipe.length) {
		properties = properties.concat(getSingleProperties('grin', 'GRIN', recipe.Riserecipe, true, false, false, facets));
	} else {
		console.log('Riserecipe, ', false);
	}

	if (recipe.RiseTaxonomy && recipe.RiseTaxonomy.length) {
		properties.push({ name: 'risetaxonomy', value: JSON.stringify(recipe.RiseTaxonomy) });

		properties = properties.concat(getSingleProperties('taxonomytypename', 'TaxonomyTypeName', recipe.RiseTaxonomy, true, true, true, facets));
	} else {
		console.log('RiseTaxonomy, ', false);
	}

	if (recipe.RomanceText) {
		properties.push({ name: 'romancetext', value: recipe.RomanceText });
	} else {
		console.log('RomanceText, ', false);
	}

	if (recipe.SEOName) {
		properties.push({ name: 'seoname', value: recipe.SEOName });
	} else {
		console.log('SEOName, ', false);
	}

	if (recipe.Tips && recipe.Tips.length) {
		properties.push({ name: 'tips', value: JSON.stringify(recipe.Tips) });
	} else {
		console.log('Tips, ', false);
	}

	if (recipe.TotalTime != null) {
		var totalTimeValue = trimPropertyValue(recipe.TotalTime);

		properties.push({ name: 'totaltime', value: totalTimeValue });

		if (facets.totaltime.value.indexOf(totalTimeValue) == -1) {
			facets.totaltime.value.push(totalTimeValue);
		}
	} else {
		console.log('TotalTime, ', false);
	}

	if (recipe.TradeMarkInfo != null) {
		properties.push({ name: 'trademarkinfo', value: recipe.TradeMarkInfo });
	} else {
		console.log('TradeMarkInfo, ', false);
	}

	if (recipe.Video && recipe.Video.length) {
		properties.push({ name: 'video', value: JSON.stringify(recipe.Video) });
	} else {
		console.log('Video, ', false);
	}

	if (recipe.Yield) {
		var yieldVal = trimPropertyValue(recipe.Yield);

		properties.push({ name: 'yield', value: yieldVal });

		if (facets.yield.value.indexOf(yieldVal) == -1) {
			facets.yield.value.push(yieldVal);
		}
	} else {
		console.log('Yield, ', false);
	}

	properties.push({ name: 'producttype', value: '0' });

	properties.push({ name: 'hasimage', value: (recipe.Assets && recipe.Assets.length) ? true : false });

	if (recipe.Assets && recipe.Assets.length) {
		var assets = [],
			w,
			TARGET_IMAGE_WIDTH = 1024;

		recipe.Assets.map(function(pic) {
			var q0 = (pic.imageType === 'Primary') ? 1 : 0;

			pic.PictureFormats.map(function(fmt) {
				if (fmt.url.match(/(\d*x\d*)/)) {
					// prefer to use the width/height baked into the image name
			 		w = parseInt(fmt.url.split('_').pop().split('.')[0].split('x')[0]);
			 	} else {
			 		// fall back on rendition - this is not a reliable measure of the image size
			 		// we must use resizable DAM images for all assets where possible
			 		w = parseInt(fmt.rendition.split('x')[0]);
			 	}

		 		var q1 = Math.abs(TARGET_IMAGE_WIDTH - w);
			 	assets.push({ url: fmt.url, w: w, q0: q0, q1: q1, r: fmt.rendition, author: pic.author });
		 	});
 		});

		assets.sort(function(a, b) {
			return a.q0 > b.q0 ? -1 : a.q0 < b.q0 ? 1 : (a.q1 < b.q1 ? -1 : a.q1 > b.q1 ? 1 : 0);
	 	});

	 	if (assets.length) {
	 		properties.push({ name: 'image', value: assets[0].url });

	 		if (assets[0].author != null) {
	 			var authorVal = trimPropertyValue(assets[0].author);

				properties.push({ name: 'author', value: authorVal });

				if (facets.author.value.indexOf(authorVal) == -1) {
					facets.author.value.push(authorVal);
				}
	 		}
	 	}
	}

	properties.push({ name: 'isrecipe', value: true });

	// semanticId
	if (recipe.RecipeId && recipe.SEOName) {
		var semanticUrl = config.domainName + '/';
		if (recipe.LanguageId != 1) {
			semanticUrl += locale[recipe.LanguageId] + '/';
		}
		semanticUrl += 'recipe/' + recipe.RecipeId + '/' + recipe.SEOName.replace('-' + recipe.RecipeId, '');

		properties.push({ name: 'semanticId', value: semanticUrl });
	} else {
		console.log('semanticId, ', false);
	}

	if (recipe.Assets && recipe.Assets.length) {
		var altImagesObj = {};

		recipe.Assets.map(function(asset, index) {
			asset.PictureFormats.map(function(format) {
				format.width = parseInt(format.rendition.split('x')[0]);

				if (!(index in altImagesObj)) {
					altImagesObj[index] = [ format ];
				} else {
					(altImagesObj[index]).push(format);
				}
			});
		});

		for (var altImages in altImagesObj) {
			altImagesObj[altImages].sort(function(a, b) {
				return a.width > b.width ? -1 : 1;
			});
		}

		var finalAltImages = [];
		for (var altImage in altImagesObj) {
			finalAltImages.push(altImagesObj[altImage][0].url);
		}

		properties.push({ name: 'altimages', value: finalAltImages.join(',') });
	}

	if (recipe.DishDetails && recipe.DishDetails.length) {
		properties = properties.concat(getSingleProperties('dishdetails_text', 'Text', recipe.DishDetails, true, true, true, facets));
		properties = properties.concat(getSingleProperties('dishdetails_url', 'Url', recipe.DishDetails, true, true, true, facets));
	} else {
		console.log('DishDetails, ', false);
	}

	console.log('', true);

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
	/*if (!config.altDetails) {
		workbook.insertRows('productcategory', (recipe.RiseTaxonomy || []).map(function(obj) {
			return Object.assign(obj, { 
				productId 	: 	recipe.RecipeId
			});
		}), RecipeSchema);
	}*/

	//
	// sku
	//
	if (!config.altDetails) {
		workbook.insertRows('skumaster', recipe, RecipeSchema);
	}

	//
	// get collecton product gtins
	//
	/*let subproductids = recipe.RiseIngredients
	.filter(function(ingredient){
		return ingredient.ProductId
	}).map(function(ingredient){
		return ingredient.ProductId
	})*/

	// return subproductids
	// return facets;
	cbk(facets);
}

function getProperties(value, isSearchable, isFilterable, isSortable, facets) {
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
				facets[key] = { fieldtype: 2, validationtype: 4, searchable: isSearchable, filterable: isFilterable, sortable: isSortable, value: [ val ] };
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

function getSingleProperties(facetname, keyName, data, isSearchable, isFilterable, isSortable, facets) {
	var result = [],
		resultObj = {};

	data.map(function(obj, index) {
		for (var property in obj) {
			if (property == keyName) {
				var key = facetname, // property.toLowerCase()
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

				if (val != 'EMPTY') {
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
						facets[key] = { fieldtype: 2, validationtype: 4, searchable: isSearchable, filterable: isFilterable, sortable: isSortable, value: [ val ] };
					} else {
						if (facets[key].value.indexOf(val) == -1) {
							facets[key].value.push(val);
						}
					}						
				}					
			}
		}
	});

	for (var properties in resultObj) {
		result.push({ name: properties, value: resultObj[properties] });
	}

	return result;
}

function trimPropertyValue(val) {
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

	return val;
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

function addFacet(facets, languageId, workbook, cbk) {
	var facetkeys = Object.keys(facets),
		iterateFacet = function(index) {
			if (index < facetkeys.length) {
				var facet = facetkeys[index];

				workbook.insertRows('facetmaster', { 
					name 			: 	facet,
					fieldtype		: 	facets[facet].fieldtype,
					validationtype 	: 	facets[facet].validationtype,
					isSearchable 	: 	facets[facet].searchable,
					isFilterable 	: 	facets[facet].filterable,
					isSortable 		: 	facets[facet].sortable,
					languageId 		: 	languageId
				}, RecipeSchema);

				if (facets[facet].value && facets[facet].value.length) {
					if (facets[facet].fieldtype == 2) {
						console.log('Facet - ' + facet + ': ' + facets[facet].value.join(',').length, true);
					}

					workbook.insertRows('facetmasterproperties', [
						{
							name 		: 	'name',
							value 		: 	facet
						},
						{
							name 		: 	'validationData',
							value 		: 	(facets[facet].fieldtype == 2) ? facets[facet].value.join(',') : facets[facet].value
						}
					].map((prop) => {
						return Object.assign(prop, { 
							facetname 	: 	facet, 
							languageId 	: 	languageId
						});
					}), RecipeSchema);
				}
				setTimeout(() => {
					iterateFacet(index + 1);
				}, 250);
		  	} else {
		  		cbk();
		  	}
		};

	iterateFacet(0);
}

module.exports = {
	addRecipes:addRecipes,
	addRecipe 	: 	addRecipe,
	addCategory : 	addCategory,
	addCategoryProperties: 	addCategoryProperties,
	addFacet 	: 	addFacet
}