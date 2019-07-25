var path = require('path')
var KraftAPI = require('../api/kraft/')
var PIMWorkbook = require('./utils/PIMWorkbook.js')
var workbook = new PIMWorkbook()
var importRecipes = require('./importRecipes.js')
var importProducts = require('./importProducts.js')
var fs = require('fs');


function importKraftData(config,cb){
	config = Object.assign({
		brands:[],
		products:[],
		recipes:[],
		filename:'export_'+new Date().getTime()+'_'+Math.floor(10+Math.random()*89),
		options:{}
	},config)

	if(config.brands.length>0){
		// add brand products to config.products
		KraftAPI.getBrandProducts(config.brands.pop(),function(brandProductsStr){

			let brandProducts = JSON.parse(brandProductsStr)

			config.products = config.products.concat(brandProducts.Products.map(function(product){
				return product.GTIN
			}))
			// recurse this function
			importKraftData(config,cb)
		},config.options)
		return
	}

	// add recipes first because they have products embedded in them
	importRecipes.addRecipes(config.recipes, workbook, function(recipes, subproductids){
		config.products = config.products.concat(subproductids)
		// recipes contain related products - add those also
		importProducts.addProducts(config.products, workbook, function(products){

			let filepath = path.join(__dirname,'exports/',config.filename+'.xlsx')

			workbook.save(filepath,function(){
				cb({
					logs:[
						'Added '+recipes.length+'/'+config.recipes.length+' Recipes',
						'Added '+products.length+'/'+config.products.length+' Products'
					],
					filepath:filepath
				})
				console.log('import to XLS complete')
			})
			
		},config.options)
	})
}

// new code
function generateRecipeData(config, callback) {
	KraftAPI.getKraftCategories(config, function(categoryResponse) {
		categoryResponse = categoryResponse && JSON.parse(categoryResponse) || {};

		if (categoryResponse && categoryResponse.Categories && categoryResponse.Categories.length) {
			var topCategories = categoryResponse.Categories,
				recipesCount = 0,
				recipesData = [];

			topCategories = topCategories.slice(14, 15); // test...

			topCategories.filter(function(categoryObj) {
				recipesCount += parseInt(categoryObj.NumberOfRecipes);
			});
			
			console.log('Recipes Count: ' + recipesCount);

			for (var i = 0; i < topCategories.length; i++) {		
				KraftAPI.getRecipeIdsForCategory(config, topCategories[i].CategoryID, function(recipeIdResponse) {
					recipeIdResponse = recipeIdResponse && JSON.parse(recipeIdResponse) || {},
					recipeIdsArray = recipeIdResponse && recipeIdResponse.Recipes || [];

					if (recipeIdsArray.length) {
						var recipeIds = [];
						recipeIdsArray.filter(function(recipeObj) {
							recipeIds.push(recipeObj.RecipeId);
						});

						// recipeIds = recipeIds.slice(0, 1); // test...
						console.log('RecipeIds Count: ' + recipeIds.length);

						KraftAPI.getRecipes(recipeIds, function(recipesResponse) {
							console.log('Got recipe response...');
							recipesResponse = recipesResponse && JSON.parse(recipesResponse) || {};

							if (recipesResponse.Recipes) {
								recipesData = recipesData.concat(recipesResponse.Recipes);
							}
							
							console.log('recipesCount: ' + recipesCount + ' recipesData: ' + recipesData.length);

							// if (recipesCount == recipesData.length) {
								var categoriesData = {};

								recipesData.map(function(recipe, index) {
									if (recipe && recipe.RecipeId) {
										importRecipes.addRecipe(recipe, workbook);

										// console.log(recipesCount + ' : ' + index);
										categoriesData = storeCategoryData(categoriesData, recipe);

										if ((recipesData.length - 1) == index) { // chk !important
											addCategoryData(categoriesData, workbook);

											var filepath = path.join(__dirname, 'exports/', config.filename + '.xlsx');

											workbook.save(filepath, function() {
												callback({
													status 	: 	'Success',
													message : 	'Added ' + recipesCount + '/' + recipesData.length + ' Recipes',
													filepath: 	filepath
												});
											});
										}
									}
								});
							// }
						}, { nocache: true });	
					}
				});
			}	
		} else {
			callback({ status: 'Failure', message: 'Categories not found' });
		}
	});
}

function storeCategoryData(categoriesData, data) {
	var categories = data && data.Categories || [];

	categories.map(function(obj) {
		categoriesData[obj.SubCategoryID + '_' + obj.CategoryID] = { subCategoryName: obj.SubCategoryName, parentCategoryName: obj.CategoryName, languageId: data.LanguageId, brandId: data.BrandId };
	});

	return categoriesData;
}

function addCategoryData(categoriesData, workbook) {
	var categoryProperties = {};

	for (var key in categoriesData) {
		var ids = key.split('_');

		for (var j = (ids.length - 1); j >= 0; j--) {
			importRecipes.addCategory({ categoryId: ids[j], parentCategoryId: ((j == 1) ? 'top' : ids[1]), brandId: categoriesData[key].brandId }, workbook);

			categoryProperties[ids[j]] = { 
				categoryName: ((j == 1) ? categoriesData[key].parentCategoryName : categoriesData[key].subCategoryName), 
				languageId: categoriesData[key].languageId,
				brandId: categoriesData[key].brandId
			};
		}
	}

	addCategoryMasterData(categoryProperties, workbook);
}

function addCategoryMasterData(data, workbook) {
	for (var key in data) {
		importRecipes.addCategoryProperties({ 
			categoryId: key, 
			propName: 'name', 
			propVal: data[key].categoryName, 
			languageId: data[key].languageId,
			brandId: data[key].brandId
		}, workbook);
	}
}

var generatePIMSheet = {
	facets 			: 	{
							tags 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							alternatelanguagerecipes 	: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							assets 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							averagerating 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
							brandid 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
							classficationid 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							classificationname 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							complimentaryrecipes 		: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							courses 					: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: false, value: [] },
							dietexchange 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							hasvideo 					: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: true, sortable: false, value: 'true-false-false' },
							ingredients 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							ingredientname 	 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							ishealthy 					: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: false, sortable: false, value: 'true-false-false' },
							keyingredients 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							keywords 					: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							lifestyles 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							numberofratings 			: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: true, value: [] },
							numberofratingswithcomments : 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-99' },
							numberofservings 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
							displayorder 				: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							document 					: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							nutritionitemid 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							nutritionitemname 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							predriscriptor 				: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							quantity 					: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							unit 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							preparationdescription 		: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							preparationpretext 			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							preparationsteps 			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							description 			 	: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
							preparationtime 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							name 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
							recipetype 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: false, value: '0-999' },
							riseingredients 			: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							ingredienttype 		 		: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							ingrediants_productid		: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							grin 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
							risetaxonomy 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							taxonomytypename 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							romancetext 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							seoname 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							tips 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							totaltime 					: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: true, value: [] },
							trademarkinfo 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							video 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							yield 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							producttype 				: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: true, value: '0-9' },
							hasimage 					: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: true, sortable: false, value: 'true-false-false' },
							image 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							author 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },		
							isrecipe 					: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: false, sortable: false, value: 'true-false-true' },
							semanticId 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999' },
							altimages 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
							dishdetails_text			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
							dishdetails_url 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] }
						},

	init 			: 	function(config, callback) {
							var self = this,
								getRecipeDetails = function(recipeIds, filter) {
									if (recipeIds.length) {
										if (filter) {
											recipeIds = recipeIds.splice(((parseInt(config.pageNo) - 1) * parseInt(config.pageSize)), parseInt(config.pageSize));
										}

										self.loadRecipeData(recipeIds, config, (responseData) => {
											console.log('\nEndtime\t' + (new Date()).toLocaleString(), true);
											callback(responseData);
										});										
									} else {
										callback({ status: 'Failure', message: 'No Recipe Ids Found' });
									}
								};

							// clear facet
							for (var facet in self.facets) {
								if (self.facets[facet].fieldtype == 2) {
									self.facets[facet].value = [];
								}
							}

							// new workbook
							workbook = new PIMWorkbook();

							if (config.filename != null) {
								self.getRecipeIds(config.filename, (recipeIds) => {
									getRecipeDetails(recipeIds, true);
								});
							} else {
								KraftAPI.getRecipeIds(config, function(recipeIdsResponse) {
									recipeIdsResponse = recipeIdsResponse && JSON.parse(recipeIdsResponse) || {},
									addedRecipes = recipeIdsResponse.AddedRecipes && recipeIdsResponse.AddedRecipes.Recipes || [],
									deletedRecipes = recipeIdsResponse.DeletedRecipes && recipeIdsResponse.DeletedRecipes.Recipes || [],
									updatedRecipes = recipeIdsResponse.UpdatedRecipes && recipeIdsResponse.UpdatedRecipes.Recipes || [],
									recipeIds = addedRecipes.concat(deletedRecipes).concat(updatedRecipes);

									getRecipeDetails(recipeIds, false);							
								});
							}							
						},
	getRecipeIds 	: 	function(filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'recipeids',
								recipeIds = [];
   							
							workbook.read(filepath, sheetName, (sheetData) => {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, (row, rowNumber) => {
										if (rowNumber > 1) {
											var cellValue = row.getCell(1).value || null;
						              		if (cellValue && (recipeIds.indexOf(cellValue) == -1)) {
						              			recipeIds.push(cellValue);
						              		}

						              		if (sheetData.rowCount == rowNumber) {
						              			callback(recipeIds);
						              		}
										}						              							              		
						            });
								} else {
									callback(recipeIds);
								}
							});							
						},
	loadRecipeData 	: 	function(recipeIds, config, callback) {
							var self = this;

							KraftAPI.getRecipes(recipeIds, (recipesResponse) => {
								recipesResponse = recipesResponse && JSON.parse(recipesResponse) || {},
								recipeData = recipesResponse.Recipes || [];

								if (recipeData.length) {									
									var iterateRecipeData = function(index) {
										if (index < recipeData.length) {
											var recipe = recipeData[index];

											if (config.altDetails) {
												if (recipe.AlternateLanguageRecipes && recipe.AlternateLanguageRecipes.length) {
													var altRecipe = recipe.AlternateLanguageRecipes.filter(function(altObj) {
														return (altObj.LanguageId == config.altDetails.languageId) && (altObj.SiteId == config.altDetails.siteId);
													});

													if (altRecipe.length) {
														recipe.RecipeId = altRecipe[0].RecipeId;
														importRecipes.addRecipe(recipe, workbook, config, self.facets, (facetData) => {
															self.facets = facetData;
															setTimeout(() => {
																iterateRecipeData(index + 1);
															}, 150);															
														});
													} else {
														console.log(recipe.RecipeId + '\tNO_ALT_RECIPE_MATCHING', true);
														iterateRecipeData(index + 1);
													}
												} else {
													console.log(recipe.RecipeId + '\tNO_ALT_RECIPE_FOUND', true);
													iterateRecipeData(index + 1);
												}												
											} else if (recipe && recipe.RecipeId) {
												importRecipes.addRecipe(recipe, workbook, config, self.facets, (facetData) => {
													self.facets = facetData;
													setTimeout(() => {
														iterateRecipeData(index + 1);
													}, 150);
												});
											}											
									  	} else {
											importRecipes.addFacet(self.facets, config.languageId, workbook, () => {
												var filename = ((parseInt(config.pageNo) - 1) * parseInt(config.pageSize)) + '_' + (parseInt(config.pageNo) * parseInt(config.pageSize));
												self.writeInLocal(workbook, filename, () => {
													callback({ status: 'Success', message: 'Success' });
												});
											});
									  	}
									};
									iterateRecipeData(0);
								} else {
									callback({ status: 'Failure', message: 'No Data Found' });
								}
							}, { nocache: true, languageId: config.languageId, brandId: config.brandId });
						},
	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						}
};

facetMapping = {
	inputpath 		: 	__dirname + '/exports/input/facet/',
	facetname 		: 	[ 'tags', 'classficationid', 'classificationname', 'courses', 'ingredientname', 'keywords', 'numberofratings', 'numberofservings', 'displayorder', 'document', 'nutritionitemid', 'nutritionitemname', 'predriscriptor', 'quantity', 'unit', 'description', 'preparationtime', 'name', 'ingredienttype', 'ingrediants_productid', 'grin', 'taxonomytypename', 'totaltime', 'yield', 'author', 'dishdetails_text', 'dishdetails_url' ],

	init 			: 	function(cbk) {
							var self = this;

							fs.readdir(self.inputpath, (error, files) => {
								if (files && files.length) {
									var sheets = [];

									files.map((file, index) => {
										self.readSheet(file, sheetData => {
											if (sheetData) {
												sheets.push(sheetData);
											}

											if ((files.length - 1) == index) {
												self.processData(sheets, results => {
													for (var key in results) {
														console.log(key + ': ' + results[key].length + ' : ' + results[key].join(',').length, true);
													}
													cbk({ status: 'Success', data: results });
												});
											}
										});
									});
								} else {
									cbk({ status: 'Failure', data: (error || 'No Files Found') });
								}
							});
						},
	readSheet		: 	function(filename, cbk) {
							var filepath = path.join(__dirname, 'exports/input/facet/', filename);

							workbook.read(filepath, 'productmasterproperties', function(sheetData) {
								console.log('Reading...', true);
								cbk(sheetData);
							});					
						},
	processData		: 	function(sheets, cbk) {
							var self = this,
								resultObj = {};

							sheets.map((sheet, sheetIndex) => {
								console.log('Processing Sheet ' + sheetIndex, true);
								sheet.eachRow({ includeEmpty: true }, function(row, rowNumber) {
									if (rowNumber > 1) {
										var propName = row.getCell(2).value;

					              		if (self.facetname.indexOf(propName) != -1) {
					              			var propValue = row.getCell(3).value,
					              				propArray = propValue.split(',');

					              			if (!(propName in resultObj)) {
					              				resultObj[propName] = propArray;
					              			} else {
					              				propArray.map(prop => {
				              						if (resultObj[propName].indexOf(prop) == -1) {
				              							resultObj[propName].push(prop);
				              						}
					              				});
					              			}
					              		}

					              		if (((sheets.length - 1) == sheetIndex) && (sheet.rowCount == rowNumber)) {
											cbk(resultObj);
										}
									}						              							              		
					            });										
							});													
						}
};

categoryMapping = {
	init 			: 	function(config, callback) {
							var self = this;

							self.getCategoryData(config.languageId, function(categoryData) {
								self.getCategories(Object.keys(categoryData), function(categories) {
									importRecipes.addCategory(categories, workbook, function() {
										self.getProperties(categoryData, function(propertiesData) {
											importRecipes.addCategoryProperties(propertiesData, workbook, function() {
												generatePIMSheet.writeInLocal(workbook, 'category_mapping', function() {
													callback({ status: 'Success', message: 'Success' });
												});
											});
										});
									});
								});
							});
						},
	getCategoryData	: 	function(languageIds, callback) {
							var filename = 'Brain-Recipe-Taxonomy.xlsx',
								filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'Brain-Taxonomy',
								categoryData = {};
   							
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {						              		
						              		var type = row.getCell(1).value || null,
						              			id = row.getCell(2).value || null,
						              			name = row.getCell(3).value || null,
						              			parentId = row.getCell(4).value != 'NULL' ? row.getCell(4).value : 'top',
						              			languageId = row.getCell(5).value || '';

						              		if (languageIds.indexOf(languageId.toString()) != -1) {
						              			categoryData[id + '_' + parentId + '_' + languageId] = {
						              				type 		: 	type,
						              				name 		: 	name
						              			};
						              		}
										}						              							              		
						            });
								}
								callback(categoryData);
							});
						},
	getCategories 	: 	function(data, callback) {
							var result = {};

							data.map(function(key, index) {
								var ids = key.split('_');
								result[ids[0] + '_' + ids[1]] = {};
							});

							callback(result);
						},
	getProperties 	: 	function(data, callback) {
							var result = [];

							for (var key in data) {
								var ids = key.split('_'),
									categoryData = data[key];

								categoryData.categoryId = ids[0],
								categoryData.languageId = ids[2];

								result.push(categoryData);
							}

							callback(result);
						}
};

//
// example request call
//

// importKraftData({
// 	brands:["50"],
// 	products:["00021000006687","00029000015623"],
// 	recipes:["200835", "200489", "200493", "200487", "200486"]
// },function(info){
// 	console.log(JSON.stringify(info,null,2))
// })

module.exports = {
	importKraftData:importKraftData,
	generateRecipeData 	: 	generateRecipeData,
	generatePIMSheet 	: 	generatePIMSheet,
	categoryMapping 	: 	categoryMapping,
	facetMapping 		: 	facetMapping
}