var path = require('path')
var KraftAPI = require('../api/kraft/')
var PIMWorkbook = require('./utils/PIMWorkbook.js')
var workbook = new PIMWorkbook()
var importRecipes = require('./importRecipes.js')
var importProducts = require('./importProducts.js')
var importProductCleanup = require('./KraftProductCleanup.js')
var importArticleCleanup = require('./KraftArticleCleanup.js')
var importArticles = require('./importArticles.js')
var importBlogCleanup = require('./KraftBlogCleanup.js')
var importBlogs = require('./importBlogs.js')
var importAuthorsCleanup = require('./KraftAuthorCleanup.js')
var importAuthors = require('./importAuthors.js')

//Article
var Articles = require("./articles.js");

xlsxj = require('xlsx-to-json')
const fs = require('fs');



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

generatePIMSheet = {
	init 			: 	function(config, callback) {
							var self = this;

							KraftAPI.getRecipeIds(config, function(recipeIdsResponse) {
								console.log(recipeIdsResponse);
								recipeIdsResponse = recipeIdsResponse && JSON.parse(recipeIdsResponse) || {},
								addedRecipes = recipeIdsResponse.AddedRecipes && recipeIdsResponse.AddedRecipes.Recipes || [],
								deletedRecipes = recipeIdsResponse.DeletedRecipes && recipeIdsResponse.DeletedRecipes.Recipes || [],
								updatedRecipes = recipeIdsResponse.UpdatedRecipes && recipeIdsResponse.UpdatedRecipes.Recipes || [],
								recipeIds = [].concat(addedRecipes),
								recipeIds = recipeIds.concat(deletedRecipes),
								recipeIds = recipeIds.concat(updatedRecipes);

								recipeIds = [ '50441' ];
								if (recipeIds.length) {
									console.log('***** Start Time *****');
									console.log(new Date());
									console.log('***** Total Recipes Count *****');
									console.log(recipeIds.length);
									recipeIds = recipeIds.slice(config.from, config.to);
									self.loadRecipeData(recipeIds, config, function() {
										console.log('***** End Time *****');
										console.log(new Date());
										callback({ status: 'Success', message: 'Success' });
									});
								}
							});
							/*self.getRecipeIds(config.filename, function(recipeIds) {
								console.log('***** Start Time *****');
								console.log(new Date());
								console.log('***** Total Recipes Count *****');
								console.log(recipeIds.length);
								recipeIds = recipeIds.slice(config.from, config.to);
								self.loadRecipeData(recipeIds, config, function() {
									console.log('***** End Time *****');
									console.log(new Date());
									callback({ status: 'Success', message: 'Success' });
								});
							});*/							
						},
	getRecipeIds 	: 	function(filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'Recip-Taxonomy-Mapping',
								recipeIds = [];
   							
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var cellValue = row.getCell(1).value || null;
						              		if (cellValue && (recipeIds.indexOf(cellValue) == -1)) {
						              			recipeIds.push(cellValue);
						              		}
										}						              							              		
						            });
								}
								callback(recipeIds);
							});							
						},
	loadRecipeData 	: 	function(recipeIds, config, callback) {
							var self = this;

							if (recipeIds.length) {
								var subsetIds = recipeIds.splice(0, config.maxCount);

								KraftAPI.getRecipes(subsetIds, function(recipesResponse) {
									recipesResponse = recipesResponse && JSON.parse(recipesResponse) || {},
									recipeData = recipesResponse.Recipes || [];

									if (recipeData.length) {
										var facetProperties = [];
										console.log('***** Writing recipes in excel *****');
										recipeData.map(function(recipe, index) {
											if (recipe && recipe.RecipeId) {
												var newFacets = importRecipes.addRecipe(recipe, workbook);
												newFacets.map(function(facet, idx) {
													if (facetProperties.indexOf(facet) == -1) {
														facetProperties.push(facet);
													}
												});
											}
											if ((recipeData.length - 1) == index) {
												importRecipes.addFacet(facetProperties, config.languageId, workbook);
												self.writeInLocal(workbook, (config.from + '_' + config.to), function() {
													self.loadRecipeData(recipeIds, config, callback);
												});
											}
										});
									}
								}, { nocache: true, languageId: config.languageId, brandId: config.brandId });
							} else {
								callback();
							}
						},
	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						}
};

generateProductSheet = {
	init 			: 	function(config, callback) {
							var self = this;
							var starttimeval = new Date().getTime().toString();
							var outputLogFile = "/exports/logs/products/fullLog_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var fullLog = fs.createWriteStream(__dirname + outputLogFile, { flags: 'a+' });
							config.fullLog = fullLog;
							var catMapLogFile = "/exports/logs/products/catmap_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var catMapLog = fs.createWriteStream(__dirname + catMapLogFile, { flags: 'a+' });
							config.catMapLog = catMapLog;
							config.sheetData = [];
							var self = this;
							productIds = [];
							self.getProductIds(config.filename, function(productIdsResponse) {
								productIds = productIdsResponse;
								productIds = productIds.slice(config.from, config.to);
								console.log(" After Total length =="+productIds.length);
								if (productIds.length)
								{
									productIds.push(productIds[productIds.length-1]);
									//console.log(" After hot fix =="+productIds.length);
									console.log('***** Start Time *****');
									console.log(new Date());
									console.log('***** Total Products Count *****');
									console.log(productIds.length - 1);
									fullLog.write("***** Start time: " + new Date().getTime().toString() + "\nTotal Products Count: " + (productIds.length - 1));
									self.getReviewData(config, config.reviewFileName, function(config) {
										self.loadProductData(productIds, config, function() {
											console.log('***** End Time *****');
											console.log(new Date());
											fullLog.write("\nEnd time: " + new Date().getTime().toString());
											callback({ status: 'Success', message: 'Success' });
										});
									});
								}
								
							});
						},
	getProductIds 	: 	function(filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'KraftRecipes Products',
								//sheetName = 'KR-Product- Taxanomy-Current',
								productIds = [];
   							
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var productId = (row.getCell(1).value+"").substring(0, 14) || null;
											// console.log(productId + "//");
											//var status = row.getCell(5).value || null;
						              		if (productId && (productIds.indexOf(productId) == -1) )
						              		{
						              			// if(status == 'Active')
						              			// {
													productIds.push(productId.toString().length == 14 ? productId : '000'+productId);
						              			// }
						              			// else
						              			// {
						              			// 	console.log('Not Active : '+productId);
						              			// }
						              		}
										}						              							              		
						            });
								}
								callback(productIds);
							});							
						},
	loadProductData : 	function(productIds, config, callback) {
							var self = this, index = 0;
							if (productIds.length) {
								function apiProductsHandler(config, index, productIds, callback)
								{
									var productId = productIds[index];
									KraftAPI.getProduct(productId, function(productsResponse) {
										productsResponse = productsResponse && JSON.parse(productsResponse) || null;
										if(productsResponse != null && productsResponse.GTIN)
										{
											//if(self.checkIsValid(productsResponse))
											//{
												config.fullLog.write("\n" + productId);
												console.log(productId);
												result = importProductCleanup.cleanup(config, productsResponse, null);
												//console.log("Cleanup Rsponse : "+result.toString());
												productId = result.GTIN;
												//self.getReviewData(config, productId, config.reviewFileName, function(config) {
													//console.log(">>"+productId);
													importProducts.addProduct(config.topcategoryid, config, result, workbook);
												//});
												//importProducts.addProduct(10001, result, workbook);
												//importProducts.addProduct('10000', result, workbook);
											/*}
											else
											{
												config.fullLog.write("\n" + productId + ' Ignored division catalyst 70');
												console.log(productId + ' Ignored division catalyst 70');
											}*/
										}
										else
										{
											config.fullLog.write("\n" + productId + ' No Response from API');
											console.log(productId + ' No Response from API');
										}	

										// write
										if ((productIds.length - 1) == index) {
											console.log(">>"+productId);
											importProducts.addFacet(workbook, function() {
												self.writeInLocal(workbook, 'product-'+config.from + '_' + config.to, function() {
													//setTimeout(function(){
														console.log(">>"+productId);
														callback({ status: 'Success', message: 'Success' })
													//},30000);
												});
											});
										}
										index = index +1;
										if(index < productIds.length) 
										{
											apiProductsHandler(config, index, productIds, callback);
										}
									});
								}
								apiProductsHandler(config, index, productIds, callback);

							} else {
								callback();
							}
						},
	getReviewData 	: 	function(config, filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
							sheetName = 'products-KraftRecipes-en';
							console.log("Rating & Review Path: " + __dirname, 'exports/input/', filename);
							console.log("sheetName: " + sheetName + "\n...Reading...");
							config.sheetDataArr = [];
							workbook.read(filepath, sheetName, function(sheetData) {
								colNumGtin = 1;
								colNumAverRat = 2;
								colNumNofRat = 3;
								colNumPid = 4;
								if (sheetData)
								{
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber)
									{
										if (rowNumber > 1)
										{
											var gtin = "";
											var avrating = "";
											var nfrating = "";
											var pid = "";
											var rowObj = {};
											if(typeof row.getCell(colNumGtin).value != "undefiend" && (row.getCell(colNumGtin).value + "") != "")
											{
												gtin = row.getCell(colNumGtin).value + "";
												rowObj.gtin = gtin;
												//sheetDataArr.push({"gtin": gtin});
												if(typeof row.getCell(colNumAverRat).value != "undefiend" && (row.getCell(colNumAverRat).value + "") != "")
												{
													avrating = row.getCell(colNumAverRat).value + "";
													//sheetDataArr.push({"ar": avrating});
													rowObj.ar = avrating;
												}
												else
												{
													//sheetDataArr.push({"ar": "0"});
													rowObj.ar = "0";
												}
												
												if(typeof row.getCell(colNumNofRat).value != "undefiend" && (row.getCell(colNumNofRat).value + "") != "")
												{
													nfrating = row.getCell(colNumNofRat).value + "";
													//sheetDataArr.push({"nr": nfrating});
													rowObj.nr = nfrating;
												}
												else
												{
													//sheetDataArr.push({"nr": "0"});
													rowObj.nr = "0";
												}

												if(typeof row.getCell(colNumPid).value != "undefiend" && (row.getCell(colNumPid).value + "") != "")
												{
													pid = row.getCell(colNumPid).value + "";
													//sheetDataArr.push({"nr": pid});
													rowObj.pid = pid;
												}
												config.sheetDataArr[gtin] = rowObj;
												//console.log(">>"+gtin);
											}
										}
									});
									console.log("...Done...\nRead row count: " + Object.keys(config.sheetDataArr).length);
									callback(config);
								}
							});						
						},

	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						},
	checkIsValid	:	function(responseObj) {
							return ((typeof responseObj.F_DIVISION_CATALYST == "undefined") || (typeof responseObj.F_DIVISION_CATALYST != "undefined" && responseObj.F_DIVISION_CATALYST != "70"));
	}
};

generateCategorySheet = {
	init 			: 	function(config, callback) {
							var self = this,
						  		categoryids = [
								{
									name 	: 	'Sauces and Condiments',
									value 	: 	'10001'
								},
								{
									name 	: 	'Packaged Meals',
									value 	: 	'10002'
								},
								{
									name 	: 	'Meats',
									value 	: 	'10003'
								},
								{
									name 	: 	'Baking and Desserts',
									value 	: 	'10004'
								},
								{
									name 	: 	'Cheese and Dairy',
									value 	: 	'10005'
								},
								{
									name 	: 	'Snacks',
									value 	: 	'10006'
								},
								{
									name 	: 	'Drinks',
									value 	: 	'10007'
								}
							];
							self.getCategories(config.filename, categoryids, function(categoryids) {
								//console.log(categoryids);
								self.getCategoryData(config.filename, config, categoryids, function(categoryResponse) {
									//console.log(categoryResponse);

										//console.log('length :'+categoryResponse.length);
										if (categoryResponse.length) {
											categoryResponse.map(function(category, index) {
												if (category) {
													//console.log(category.top);
													importProducts.addCategory(category, workbook);
													if(category.isValid)
													{
														var productId = category.GTIN;
														productId = productId.toString().length == 14 ? productId : '000'+productId;
														KraftAPI.getProduct(productId, function(productsResponse) {
															productsResponse = productsResponse && JSON.parse(productsResponse) || null;
															if(productsResponse != null && !productsResponse.ErrorFlag)
															{
																result = importProductCleanup.cleanup(productsResponse);
																importProducts.addProduct(category.top, result, workbook);
															}
															else
															{
																console.log('Erorr Product ID :'+productsResponse.GTIN);
															}	

															// write
															if ((categoryResponse.length - 1) == index) {
																importProducts.addFacet(workbook, function() {
																	self.writeInLocal(workbook, 'categorydetails-'+config.from + '_' + config.to, function() {
																		callback({ status: 'Success', message: 'Success' });
																	});
																});
															}
														}, null);	
													}
												}
											});
										}
								});
							});
						},
	getCategories 	: 	function(filename, categoryids,callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'KR-Product- Taxanomy-Current'
   							
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									size = 10007;
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if(rowNumber > 1)
										{
											flag = true;
											for(var i=0; i < categoryids.length; i++){
					              				if(row.getCell(4).value === categoryids[i].name)
					              				{
					              					flag = false;
					              					break;
					              				}
				              				}
				              				if(flag)
				              				{
				              					size++;
				              					//console.log(row.getCell(4).value);
				              					var  newCat = {
					              						name : row.getCell(4).value,
					              						value : size
					              					}
					              				categoryids.push(newCat);
				              				}
										}
						            });
								}
								callback(categoryids);
							});							
						},

	getCategoryData	: 	function(filename, config, categoryids, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'KR-Product- Taxanomy-Current',
								productFilePath = path.join(__dirname, 'exports/input/', config.productfile),
								productSheetName = 'KraftRecipes Products',
								productSheetData = [];
								categoryData = [{
						              				top 		: 	'top',
						              				topName 	: 	'Products',
						              				subCat      :   '10000'
					              				}];
   							
   							// New Product Sheet
   							workbook.read(productFilePath, productSheetName, function(sheetData) {
   								if (sheetData) {
   									productSheetData = sheetData;
   								}
   							});	

   							//console.log(categoryids);
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1 && config.from < rowNumber && rowNumber <= config.to) {						              		
						              		var top = row.getCell(1).value || null,
					              			topSeoName = row.getCell(2).value || null,
					              			topName = row.getCell(3).value || null,
					              			subCat = row.getCell(4).value || null,
					              			subSeoCatName = row.getCell(5).value || null,
					              			subCatName = row.getCell(6).value || null,
					              			gtnId = row.getCell(13).value || null;

					              			flag = false;
					              			if(productSheetData)
					              			{
					              				productSheetData.eachRow({ includeEmpty: true }, function(prow, prowNumber) {
					              					if(prowNumber > 1 && prow.getCell(1).value.substring(1, 15) == gtnId)
					              					{
					              						flag = true;
					              						//console.log(flag);
							              			}
					              				});
					              			}
					              			
					              			for(var i=0; i < categoryids.length; i++) {
					              				// need to validate for category
					              				//console.log(categoryids[i].name)
					              				if(top == categoryids[i].name)
					              				{
					              					top = categoryids[i].value;
					              					topName = categoryids[i].name;
					              				}
					              				if(subCat == categoryids[i].name)
					              				{
					              					subCat = categoryids[i].value
					              					subCatName = categoryids[i].name;
					              				}
					              			}

											if(flag)
											{
												category = {
						              				top 		: 	top,
						              				topSeo 		: 	topSeoName,
						              				topName 	: 	topName,
						              				subCat 		: 	subCat,
						              				subCatSeo 	: 	subSeoCatName,
						              				subCatName 	: 	subCatName,
						              				GTIN 		: 	gtnId,
						              				isValid		:  true
					              				};
											}
											else
											{
												category = {
						              				top 		: 	top,
						              				topSeo 		: 	topSeoName,
						              				topName 	: 	topName,
						              				subCat 		: 	subCat,
						              				subCatSeo 	: 	subSeoCatName,
						              				subCatName 	: 	subCatName,
						              				GTIN 		: 	gtnId,
						              				isValid		:  	false
						              			};
											}
					              			categoryData.push(category);						              			
										}						              							              		
						            });
								}
								callback(categoryData);
							});
						},	

	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						}
};

generateFacetPropertiesSheet = {
	init 			: 	function(config, callback) {
							var self = this;
							self.getProductIds(config.filename, function(productIds) {
								self.loadProductData(productIds, config, function() {

								});
							});
						},
	getProductIds 	: 	function(filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'KraftRecipes Products',
								//sheetName = 'KR-Product- Taxanomy-Current',
								productIds = [];
   							
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var productId = row.getCell(1).value.substring(1, 15) || null;
											var status = row.getCell(5).value || null;
						              		if (productId && (productIds.indexOf(productId) == -1) )
						              		{
						              			if(status == 'Active')
						              			{
						              				productIds.push(productId.toString().length == 14 ? productId : '000'+productId);
						              			}
						              			else
						              			{
						              				console.log('Not Active : '+productId);
						              			}
						              		}
										}						              							              		
						            });
								}
								callback(productIds);
							});							
						},
	loadProductData : 	function(productIds, config, callback) {
							var self = this;

							if (productIds.length) {
								var pordLength = (productIds.length%config.maxCount > 0) ? (productIds.length/config.maxCount+1) : (productIds.length/config.maxCount);
								for(var i=0 ; i < pordLength ; i++)
								{
									var subsetIds = productIds.splice(config.from, config.to);

									KraftAPI.getProducts(subsetIds, function(productsResponse) {
										productsResponse = productsResponse && JSON.parse(productsResponse) || {},
										productData = productsResponse.Products || [];
										if (productData.length) {
											var facetProperties = [];
											console.log('***** Writing products in excel *****');
											productData.map(function(product, index) {
												if (product && product.GTIN) {
													result = importProductCleanup.cleanup(product);
													importProducts.addProduct(10001, result, workbook);
												}
											});
										}
									}, { nocache: true, languageId: config.languageId});

									config.from = config.to;
									config.to = config.to + config.maxCount;

									if ((pordLength - 1) == i) {
											importProducts.addFacet(workbook, function() {
											self.writeInLocal(workbook, 'Facetdetails', function() {
												callback({ status: 'Success', message: 'Success' });
											});
										});
									}
								}					
							} else {
								callback();
							}
						},
											
	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						}
};

var digitLength = 9;

generateArticleProductSheet = {
	init 			: 	function(config, callback) {
							var self = this;
							self.productCount = 0;
							self.productIds = [];
							var starttimeval = new Date().getTime().toString();
							var outputLogFile = "/exports/logs/articles/scrap_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var callLog = fs.createWriteStream(__dirname + outputLogFile, { flags: 'a+' });
							var cleanUpLogFile = "/exports/logs/articles/cleanup_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var cleanUpLog = fs.createWriteStream(__dirname + cleanUpLogFile, { flags: 'a+' });
							var catMapLogFile = "/exports/logs/articles/catmap_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var catMapLog = fs.createWriteStream(__dirname + catMapLogFile, { flags: 'a+' });
							var oldNewLogFile = "/exports/logs/articles/oldnew_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var oldNewLog = fs.createWriteStream(__dirname + oldNewLogFile, { flags: 'a+' });
							var descCountLogFile = "/exports/logs/articles/desccount_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var descCountLog = fs.createWriteStream(__dirname + descCountLogFile, { flags: 'a+' });
							config.callLog = callLog;
							config.cleanUpLog = cleanUpLog;
							config.catMapLog = catMapLog;
							config.oldNewLog = oldNewLog;
							config.descCountLog = descCountLog;
							
							var descImgD3LogFile = "/exports/logs/articles/desc_images_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var descImgD3MapLog = fs.createWriteStream(__dirname + descImgD3LogFile, { flags: 'a+' });
							var imgD3LogFile = "/exports/logs/articles/images_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var imgD3MapLog = fs.createWriteStream(__dirname + imgD3LogFile, { flags: 'a+' });
							config.callLog = callLog;
							config.cleanUpLog = cleanUpLog;
							config.catMapLog = catMapLog;
							config.imgD3MapLog = imgD3MapLog;
							config.descImgD3MapLog = descImgD3MapLog;
							
							self.getDataJsonFromSheet(config.filename, config.from, config.to, function(artDataJson){
								console.log(" After Total length =="+ self.productIds.length);
								var productIds = self.productIds;
								if (productIds.length) {
									//artDataJson['hotfix'] = {"id" : "", "url" : ""}
									//productIds.push(productIds[productIds.length - 1]);
									//console.log(" After Hot fix ==" + productIds.length);
									console.log('***** Start Time *****');
									console.log(new Date());
									console.log('***** Total Products Count *****');
									console.log(productIds.length);
									callLog.write("***** Start time: " + new Date().getTime().toString() + "\nTotal Products Count: " + (productIds.length));

									self.loadProductData(productIds, config, function() {
										console.log('***** End Time *****');
										console.log(new Date());
										callLog.write("\nEnd time: " + new Date().getTime().toString());
										callback({ status: 'Success', message: 'Success' });
									});
								}
							});
						},
	getDataJsonFromSheet : 	function(filename, readRowFrom, readRowSize, callback) {
							var self = this, filepath = path.join(__dirname, 'exports/input/', filename),
							sheetName = 'ArticlesUrlsIds',
							retJsonObj = {};
							console.log("filepath: " + filepath + "\nfrom    : " + readRowFrom + "\nto      : " + (readRowFrom + readRowSize));
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if ( (rowNumber > 1) && (rowNumber > readRowFrom) && (rowNumber < (readRowFrom + readRowSize)) )
										{
											var articleType = row.getCell(1).value || null;
											var url = row.getCell(2).value || null;
											var productId = row.getCell(4).value || null;
											var urlType = row.getCell(3).value || null;
											if(url != null && productId != null)
											{
												self.productCount++;
												zeorCount = digitLength - productId.toString().length;
												for(var perfixidx= 0; perfixidx < zeorCount; perfixidx++)
												{	
													productId = "0" + productId;
												}
												self.productIds.push(productId);
												retJsonObj[productId] = {"id" : productId, "url" : url, "type" : urlType, "articleType": articleType};
											}
											if(sheetData.rowCount == rowNumber)
											{
												self.artDataJson = retJsonObj;
												//console.log(JSON.stringify(self.artDataJson));
												callback(retJsonObj);
											}
										}						              							              		
									});
								}
							});
						},
	loadProductData : 	function(productIds, config, callback) {
							var self = this, index = 0;
							if (productIds.length) {
								function apiProductsHandler(config, index, productIds, callback)
								{
									var productId = productIds[index];
									var artDataJson = self.artDataJson;
									var url = artDataJson[productId].url;
									var templateType = artDataJson[productId].type;
									var articleType = artDataJson[productId].articleType;
									//console.log("artDataJson[productId].url " + artDataJson[productId].url);
									KraftAPI.getArticle(url, function(productsResponse) {
										productsResponse = productsResponse && JSON.parse(productsResponse) || null;
										if(productsResponse != null)
										{
											if(self.checkIsValid(productsResponse))
											{
												config.callLog.write("\n" + url);
												console.log(url);
												//console.log(JSON.stringify(productsResponse));
												productsResponse.templateType = templateType;
												productsResponse.articleType = articleType;
												config.url = url;
												result = importArticleCleanup.cleanup(productId, productsResponse, config);
												//image logging for d3 convertion start
												if(productsResponse && productsResponse.images && productsResponse.images.length)
												{
													productsResponse.images.forEach(function(element) {
														var imageSpliter = element.value.split(" ");
														var found = false;
														imageSpliter.forEach(function(imageFinder) {
															if(imageFinder.indexOf("src=") != -1)
															{
																config.descImgD3MapLog.write(imageFinder.slice(5).slice(0, -1) + '\n');
																found = true;
															}
														});
														if(!found)
														{
															console.log("Image not scraped in link :" + element.value);
														}
													});
												}
												if(productsResponse && productsResponse.properties && productsResponse.properties.iteminfo && productsResponse.properties.iteminfo.images && productsResponse.properties.iteminfo.images.length)
												{
													productsResponse.properties.iteminfo.images.forEach(function(element) {
														config.imgD3MapLog.write(productId + '\t' +element.image + '\n');
													});
												}
												//image logging for d3 convertionend

												//console.log("Cleanup Rsponse : "+result.name);
												/*productId = result.GTIN;
												self.getReviewData(productId, config.reviewFileName, function(reviewsResponse) {
													//console.log(">>"+productId);
													*/
													importArticles.addArticle(10001, result, workbook);
												//});
												//importProducts.addProduct(10001, result, workbook);
												//importProducts.addProduct('10000', result, workbook);
											}
											else
											{
												config.callLog.write("\n" + url + ' Invalid Response Code');
												console.log(url + ' Invalid Response Code');
											}
										}
										else
										{
											config.callLog.write("\n" + url + ' No Response from API');
											console.log(url + ' No Response from API');
											//console.log(JSON.stringify(productsResponse));
										}	

										// write
										if ((productIds.length - 1) == index) {
											importArticles.addFacet(workbook, function() {
												self.writeInLocal(workbook, 'product-'+config.from + '_' + config.to + '_' + new Date().getTime().toString(), function() {
													//setTimeout(function(){
														callback({ status: 'Success', message: 'Success' })
													//},30000);
												});
											});
										}
										index = index +1;
										if(index < productIds.length) 
										{
											apiProductsHandler(config, index, productIds, callback);
										}
									});
								}
								apiProductsHandler(config, index, productIds, callback);

							} else {
								callback();
							}
						},
	getReviewData 	: 	function(productId, filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'products-KraftRecipes-en';
								
								//console.log('productId :' + productId);
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									reviews = [];
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var sheetprodId = row.getCell(2).value;
											if (sheetprodId && productId == sheetprodId )
											{
												//console.log('sheetprodId === '+sheetprodId);
												var averagerating = {
														name : 'averagerating',
														value : row.getCell(3).value
													};
												if(averagerating.value >= 0)
												reviews.push(averagerating);	
												var numberofratings = {
														name : 'numberofratings',
														value : row.getCell(4).value
													};
												if(numberofratings.value >= 0)
												reviews.push(numberofratings);	
											}
										}						              							              		
									});
									callback(reviews);
								}
							});
							//callback(review);							
						},

	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						},
	checkIsValid 	:	function(responseObj) {
							if(responseObj.hasOwnProperty("redirectResponse"))
								return false;
							else if(responseObj.hasOwnProperty("responseCode"))
								return false;
							else
								return true;
	}
};

checkIdsFromSite = {
	init 			: 	function(config, callback) {
							var self = this;
							var starttimeval = new Date().getTime().toString();
							var outputLogFile = "/exports/logs/articles/url_" + config.from + "_"+config.to + "_" + starttimeval + ".csv"
							var idLog = fs.createWriteStream(__dirname + outputLogFile, { flags: 'a+' });
							config.idLog = idLog;
							
							self.getDataJsonFromSheet(config.filename, config.from, config.to, function(urls){
								console.log(" After Total URL length =="+ urls.length);
								if (urls.length) {
									console.log('***** Start Time *****');
									console.log(new Date());
									console.log('***** Total URL Count *****');
									console.log(urls.length);

									self.loadProductData(urls, config, function() {
										console.log('***** End Time *****');
										console.log(new Date());
										callback({ status: 'Success', message: 'Success' });
									});
								}
							});
						},
	getDataJsonFromSheet : 	function(filename, readRowFrom, readRowSize, callback) {
							var self = this, filepath = path.join(__dirname, 'exports/input/', filename),
							sheetName = 'url',
							retArray = [];
							console.log("filepath: " + filepath + "\nfrom    : " + readRowFrom + "\nto      : " + (readRowFrom + readRowSize));
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if ( (rowNumber > 1) && (rowNumber > readRowFrom) && (rowNumber < (readRowFrom + readRowSize)) )
										{
											var urlOrgin = row.getCell(1).value || null;
											var urlNew = row.getCell(2).value || null;
											
											if(urlOrgin != null && urlOrgin != '' && urlNew != null && urlNew != '')
											{
												retArray.push(urlOrgin + "~" + urlNew);
											}
											if(sheetData.rowCount == rowNumber)
											{
												callback(retArray);
											}
										}						              							              		
									});
								}
							});
						},
	loadProductData : 	function(urls, config, callback) {
							var self = this, index = 0;
							var logJson=[];
							var maxHit = config.maxHit ? config.maxHit : 5;
							var masterArray = [];
							
							/*for(var temp = 0; temp < urls.length; temp += maxHit){
								self.hitProduct(url.splice(temp, temp + (maxHit-1)));
							}*/

							if (urls.length) {
								urls.forEach(function(url) {
									var urlSplitor = url.split('~');
									var tempcatIdSplitor = urlSplitor[1].split("/");
									var catId = '';
									var temU = '';
									tempcatIdSplitor.forEach(function(splitedUrl){
										if(!isNaN(splitedUrl))
										{
											catId = splitedUrl;
											temU = "http://origin-new.kraftrecipes.com" + urlSplitor[0];
										}
									});
									orginUrl = "http://origin-new.kraftrecipes.com" + urlSplitor[0];
									var retArray = {};
									retArray.orginkey = url;
									retArray.newkey = url;

									KraftAPI.getRecipeIdsFromCatOrgin(orginUrl, function(response){
										var orginUrlTemp = orginUrl;
										var orginIds = [];
										var idPrinterOrgin = '';
										response = JSON.parse(response);
										if(response && response.ids && response.ids.length)
										{
											response.ids.forEach(function(ids) {
												if(ids.value.length != 6){
													var temLen = 6 - ids.value.length;
													for(lenInc = 0; lenInc < temLen; lenInc++)
													{
														ids.value = '0' + ids.value;
													}
												}
												orginIds.push(ids.value);
												idPrinterOrgin += ids.value + '~';
											});
										}
										else{
											console.log('not processed orgin' + temU);
										}
										retArray.orginvalue = orginIds;
										
										KraftAPI.getRecipeIdsFromCatNew("https://www.kraftrecipes.com/skavastream/core/v5/skavastore/productlist/" + catId + "?storeId=12&offset=0&limit=1000&sort=&locale=en_US", function(response){
											newUrl = "https://www.kraftrecipes.com/skavastream/core/v5/skavastore/productlist/" + catId + "?storeId=12&offset=0&limit=1000&sort=&locale=en_US";
											response = JSON.parse(response);
											var newIds = [];
											var missingId = '';
											var idPrinterNew = '';
											if(response && response.children && response.children.products && response.children.products.length)
											{
												console.log(response.children.products[0].identifier);
												response.children.products.forEach(function(product) {
													newIds.push(product.identifier);
													idPrinterNew += product.identifier + '~';
												});
											}
											else{
												console.log('not processed new ' + newUrl + JSON.stringify(response));
											}
											retArray.newvalue = newIds;
											
											orginIds.forEach(function(ids) {
												if(newIds.indexOf(ids) == -1)
												{
													missingId += ids + "~";
												}
											});
											console.log("Processed - " + (++index));
											config.idLog.write(temU + '\t' + newUrl + '\t' + idPrinterOrgin + '\t' + idPrinterNew + '\t' + missingId + '\n');
										});
									});
								});
							} else {
								callback();
							}
						},
	getReviewData 	: 	function(productId, filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'products-KraftRecipes-en';
								
								//console.log('productId :' + productId);
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									reviews = [];
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var sheetprodId = row.getCell(2).value;
											if (sheetprodId && productId == sheetprodId )
											{
												//console.log('sheetprodId === '+sheetprodId);
												var averagerating = {
														name : 'averagerating',
														value : row.getCell(3).value
													};
												if(averagerating.value >= 0)
												reviews.push(averagerating);	
												var numberofratings = {
														name : 'numberofratings',
														value : row.getCell(4).value
													};
												if(numberofratings.value >= 0)
												reviews.push(numberofratings);	
											}
										}						              							              		
									});
									callback(reviews);
								}
							});
							//callback(review);							
						},

	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						},
	checkIsValid 	:	function(responseObj) {
							if(responseObj.hasOwnProperty("redirectResponse"))
								return false;
							else if(responseObj.hasOwnProperty("responseCode"))
								return false;
							else
								return true;
	}
};

var digitLengthBlog = 12;
generateBlogsProductSheet = {
	init 			: 	function(config, callback) {
							var self = this;
							self.productCount = 0;
							self.productIds = [];
							var starttimeval = new Date().getTime().toString();
							var outputLogFile = "/exports/logs/blogs/scrap_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var callLog = fs.createWriteStream(__dirname + outputLogFile, { flags: 'a+' });
							var cleanUpLogFile = "/exports/logs/blogs/cleanup_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var cleanUpLog = fs.createWriteStream(__dirname + cleanUpLogFile, { flags: 'a+' });
							var catMapLogFile = "/exports/logs/blogs/images_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var catMapLog = fs.createWriteStream(__dirname + catMapLogFile, { flags: 'a+' });
							config.callLog = callLog;
							config.cleanUpLog = cleanUpLog;
							config.catMapLog = catMapLog;
							
							self.getDataJsonFromSheet(config.filename, config.from, config.to, function(artDataJson){
								console.log(" After Total length =="+ self.productIds.length);
								var productIds = self.productIds;
								if (productIds.length) {
									//artDataJson['hotfix'] = {"id" : "", "url" : ""}
									//productIds.push(productIds[productIds.length - 1]);
									//console.log(" After Hot fix ==" + productIds.length);
									console.log('***** Start Time *****');
									console.log(new Date());
									console.log('***** Total Products Count *****');
									console.log(productIds.length);
									callLog.write("***** Start time: " + new Date().getTime().toString() + "\nTotal Products Count: " + (productIds.length));

									self.loadProductData(productIds, config, function() {
										console.log('***** End Time *****');
										console.log(new Date());
										callLog.write("\nEnd time: " + new Date().getTime().toString());
										callback({ status: 'Success', message: 'Success' });
									});
								}
							});
						},
	getDataJsonFromSheet : 	function(filename, readRowFrom, readRowSize, callback) {
							var self = this, filepath = path.join(__dirname, 'exports/input/', filename),
							sheetName = 'KraftBlogs',
							retJsonObj = {};
							console.log("filepath: " + filepath + "\nfrom    : " + readRowFrom + "\nto      : " + (readRowFrom + readRowSize));
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if ( (rowNumber > 1) && (rowNumber > readRowFrom) && (rowNumber < (readRowFrom + readRowSize)) )
										{
											var url = row.getCell(1).value || null;
											var productId = row.getCell(2).value || null;
											if(url != null && productId != null)
											{
												self.productCount++;
												zeorCount = digitLengthBlog - productId.toString().length;
												for(var perfixidx= 0; perfixidx < zeorCount; perfixidx++)
												{	
													productId = "0" + productId;
												}
												self.productIds.push(productId);
												retJsonObj[productId] = {"id" : productId, "url" : url};
											}
											if(sheetData.rowCount == rowNumber)
											{
												self.artDataJson = retJsonObj;
												//console.log(JSON.stringify(self.artDataJson));
												callback(retJsonObj);
											}
										}						              							              		
									});
								}
							});
						},
	loadProductData : 	function(productIds, config, callback) {
							var self = this, index = 0;
							if (productIds.length) {
								function apiProductsHandler(config, index, productIds, callback)
								{
									var productId = productIds[index];
									var artDataJson = self.artDataJson;
									var url = artDataJson[productId].url;
									//console.log("artDataJson[productId].url " + artDataJson[productId].url);
									KraftAPI.getBlog(url, function(productsResponse) {
										productsResponse = productsResponse && JSON.parse(productsResponse) || null;
										if(productsResponse != null)
										{
											if(self.checkIsValid(productsResponse))
											{
												config.callLog.write("\n" + url);
												console.log(url);
												result = importBlogCleanup.cleanup(productId, productsResponse, config);
												importBlogs.addBlog(10001, result, workbook);
											}
											else
											{
												config.callLog.write("\n" + url + ' Invalid Response Code');
												console.log(url + ' Invalid Response Code');
											}
										}
										else
										{
											config.callLog.write("\n" + url + ' No Response from API');
											console.log(url + ' No Response from API');
										}	

										// write
										if ((productIds.length - 1) == index) {
											importBlogs.addFacet(workbook, function() {
												self.writeInLocal(workbook, 'product-'+config.from + '_' + config.to + '_' + new Date().getTime().toString(), function() {
														callback({ status: 'Success', message: 'Success' })
												});
											});
										}
										index = index +1;
										if(index < productIds.length) 
										{
											apiProductsHandler(config, index, productIds, callback);
										}
									});
								}
								apiProductsHandler(config, index, productIds, callback);

							} else {
								callback();
							}
						},
	getReviewData 	: 	function(productId, filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'products-KraftRecipes-en';
								
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									reviews = [];
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var sheetprodId = row.getCell(2).value;
											if (sheetprodId && productId == sheetprodId )
											{
												var averagerating = {
														name : 'averagerating',
														value : row.getCell(3).value
													};
												if(averagerating.value >= 0)
												reviews.push(averagerating);	
												var numberofratings = {
														name : 'numberofratings',
														value : row.getCell(4).value
													};
												if(numberofratings.value >= 0)
												reviews.push(numberofratings);	
											}
										}						              							              		
									});
									callback(reviews);
								}
							});					
						},

	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						},
	checkIsValid 	:	function(responseObj) {
							if(responseObj.hasOwnProperty("redirectResponse"))
								return false;
							else if(responseObj.hasOwnProperty("responseCode"))
								return false;
							else
								return true;
	}
};

var digitLengthAuthor = 9;
generateAuthorsProductSheet = {
	init 			: 	function(config, callback) {
							var self = this;
							self.productCount = 0;
							self.productIds = [];
							var starttimeval = new Date().getTime().toString();
							var outputLogFile = "/exports/logs/authors/scrap_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var callLog = fs.createWriteStream(__dirname + outputLogFile, { flags: 'a+' });
							var cleanUpLogFile = "/exports/logs/authors/cleanup_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var cleanUpLog = fs.createWriteStream(__dirname + cleanUpLogFile, { flags: 'a+' });
							var catMapLogFile = "/exports/logs/authors/catmap_" + config.from + "_"+config.to + "_" + starttimeval + ".log"
							var catMapLog = fs.createWriteStream(__dirname + catMapLogFile, { flags: 'a+' });
							config.callLog = callLog;
							config.cleanUpLog = cleanUpLog;
							config.catMapLog = catMapLog;
							
							self.getDataJsonFromSheet(config.filename, config.from, config.to, function(artDataJson){
								console.log(" After Total length =="+ self.productIds.length);
								var productIds = self.productIds;
								if (productIds.length) {
									//artDataJson['hotfix'] = {"id" : "", "url" : ""}
									//productIds.push(productIds[productIds.length - 1]);
									//console.log(" After Hot fix ==" + productIds.length);
									console.log('***** Start Time *****');
									console.log(new Date());
									console.log('***** Total Products Count *****');
									console.log(productIds.length);
									callLog.write("***** Start time: " + new Date().getTime().toString() + "\nTotal Products Count: " + (productIds.length));

									self.loadProductData(productIds, config, function() {
										console.log('***** End Time *****');
										console.log(new Date());
										callLog.write("\nEnd time: " + new Date().getTime().toString());
										callback({ status: 'Success', message: 'Success' });
									});
								}
							});
						},
	getDataJsonFromSheet : 	function(filename, readRowFrom, readRowSize, callback) {
							var self = this, filepath = path.join(__dirname, 'exports/input/', filename),
							sheetName = 'KraftBlogs',
							retJsonObj = {};
							console.log("filepath: " + filepath + "\nfrom    : " + readRowFrom + "\nto      : " + (readRowFrom + readRowSize));
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if ( (rowNumber > 1) && (rowNumber > readRowFrom) && (rowNumber < (readRowFrom + readRowSize)) )
										{
											var url = row.getCell(1).value || null;
											var productId = row.getCell(2).value || null;
											if(url != null && productId != null)
											{
												self.productCount++;
												zeorCount = digitLengthAuthor - productId.toString().length;
												for(var perfixidx= 0; perfixidx < zeorCount; perfixidx++)
												{	
													productId = "0" + productId;
												}
												self.productIds.push(productId);
												retJsonObj[productId] = {"id" : productId, "url" : url};
											}
											if(sheetData.rowCount == rowNumber)
											{
												self.artDataJson = retJsonObj;
												//console.log(JSON.stringify(self.artDataJson));
												callback(retJsonObj);
											}
										}						              							              		
									});
								}
							});
						},
	loadProductData : 	function(productIds, config, callback) {
							var self = this, index = 0;
							if (productIds.length) {
								function apiProductsHandler(config, index, productIds, callback)
								{
									var productId = productIds[index];
									var artDataJson = self.artDataJson;
									var url = artDataJson[productId].url;
									//console.log("artDataJson[productId].url " + artDataJson[productId].url);
									KraftAPI.getAuthors(url, function(productsResponse) {
										productsResponse = productsResponse && JSON.parse(productsResponse) || null;
										if(productsResponse != null)
										{
											if(self.checkIsValid(productsResponse))
											{
												config.callLog.write("\n" + url);
												console.log(url);
												result = importAuthorsCleanup.cleanup(productId, productsResponse, config);
												importAuthors.addBlog(10001, result, workbook);
											}
											else
											{
												config.callLog.write("\n" + url + ' Invalid Response Code');
												console.log(url + ' Invalid Response Code');
											}
										}
										else
										{
											config.callLog.write("\n" + url + ' No Response from API');
											console.log(url + ' No Response from API');
										}	

										// write
										if ((productIds.length - 1) == index) {
											importBlogs.addFacet(workbook, function() {
												self.writeInLocal(workbook, 'product-'+config.from + '_' + config.to + '_' + new Date().getTime().toString(), function() {
														callback({ status: 'Success', message: 'Success' })
												});
											});
										}
										index = index +1;
										if(index < productIds.length) 
										{
											apiProductsHandler(config, index, productIds, callback);
										}
									});
								}
								apiProductsHandler(config, index, productIds, callback);

							} else {
								callback();
							}
						},
	getReviewData 	: 	function(productId, filename, callback) {
							var filepath = path.join(__dirname, 'exports/input/', filename),
								sheetName = 'products-KraftRecipes-en';
								
							workbook.read(filepath, sheetName, function(sheetData) {
								if (sheetData) {
									reviews = [];
									sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
										if (rowNumber > 1) {
											var sheetprodId = row.getCell(2).value;
											if (sheetprodId && productId == sheetprodId )
											{
												var averagerating = {
														name : 'averagerating',
														value : row.getCell(3).value
													};
												if(averagerating.value >= 0)
												reviews.push(averagerating);	
												var numberofratings = {
														name : 'numberofratings',
														value : row.getCell(4).value
													};
												if(numberofratings.value >= 0)
												reviews.push(numberofratings);	
											}
										}						              							              		
									});
									callback(reviews);
								}
							});					
						},

	writeInLocal 	: 	function(workbook, filename, callback) {
							filename = filename || new Date().getTime().toString(),
							filepath = path.join(__dirname, 'exports/output/', filename + '.xlsx');

							workbook.save(filepath, function() {
								callback();
							});
						},
	checkIsValid 	:	function(responseObj) {
							if(responseObj.hasOwnProperty("redirectResponse"))
								return false;
							else if(responseObj.hasOwnProperty("responseCode"))
								return false;
							else
								return true;
	}
};

module.exports = {
	importKraftData:  	importKraftData,
	generateRecipeData 	: 	generateRecipeData,
	generatePIMSheet 	: 	generatePIMSheet,
	generateProductSheet:   generateProductSheet,
	generateCategorySheet : generateCategorySheet,
	generateFacetPropertiesSheet : generateFacetPropertiesSheet,
	generateArticleProductSheet:   generateArticleProductSheet,
	generateBlogsProductSheet : generateBlogsProductSheet,
	generateAuthorsProductSheet : generateAuthorsProductSheet,
	checkIdsFromSite : checkIdsFromSite
}