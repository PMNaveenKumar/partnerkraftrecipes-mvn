var path = require('path')
var PIMWorkbook = require('./utils/PIMWorkbook.js')
var workbook = new PIMWorkbook()
var importUserRecipes = require('./importUserRecipes.js')
const csv = require('csvtojson');
var csvya = require('ya-csv');
var utf8 = require('utf8');
var KraftAPI = require('../api/kraft/')
const requestMod = require('request');
const fs = require('fs');
const HttpsProxyAgent = require('http-proxy-agent');
var ingredientsdata = [];

facets = {};
generatePIMSheet = {
	
	init 			: 	function(config, callback) {
							var outputLogFile = "/" + config.startIndex + "_"+config.endIndex + "_userrecipe_"+ new Date().getTime() + ".log"
							var logger = fs.createWriteStream( path.join(__dirname,'MissedFailureLog/',outputLogFile), { flags: 'a+' });
							var self = this;
							self.readIngrediants(config);
							workbook = new PIMWorkbook();
							var originalrecipeIDs = [];
							var recipeIDs = [];
							csv({
								delimiter:'\t'
							}).fromFile("D:/nodejs_pgms/userrecipesheet/toXLS/exports/input/Users-Recipes.csv").then((jsonArr)=>{
								
								//console.log(jsonArr);
								var i = -1;
								var userrecipeArr = [];
								var averageRating = [];
								var numberOfRating = [];
								jsonArr.forEach(function(jsonObj) {
									// var siteId = jsonObj.SiteID;
									// var approved = jsonObj.Approved;
									// var public = jsonObj.Pubilc;
									
									// if(((siteId == 1) || (siteId == 90)) && (approved == 1) && (public == 1))
									// {
										i = i+1;
										originalrecipeIDs[i] = jsonObj.RecipeID;
										if(jsonObj.AverageRating || jsonObj.AverageRating >= 0)
										{
											averageRating[i] = jsonObj.AverageRating;
										}
										else
										{
											averageRating[i] = 0;
										}
										if(jsonObj.numberOfRating || jsonObj.numberOfRating >= 0)
										{
											numberOfRating[i] = jsonObj.numberOfRating;
										}
										else
										{
											numberOfRating[i] = 0;
										}
									//}
								});

								recipeIDs = originalrecipeIDs.slice(parseInt(config.startIndex), parseInt(config.endIndex));
								originalrecipeIDs = [];
								var count = 0;
								var recipesCount = recipeIDs.length;
								var index = 0;
								var wellioReferenceIdSet = []
								var count1 = -1;
								if(recipeIDs.length > 0)
								{
									getkraftuserrecipeData(config,count,index, recipeIDs,recipesCount, wellioReferenceIdSet,userrecipeArr,averageRating,numberOfRating,callback,logger, count1)
								}
								
								
							})	
																																
						},
						readIngrediants 	: 	function(config) {
							var filepath = path.join(__dirname, '/exports/input/', 'ingredients.csv');
							var reader = csvya.createCsvFileReader(filepath, { 'encoding': 'utf-8','separator': '|' });
							reader.setColumnNames([ 'INGREDIENT_ID','INGREDIENT_NAME','CREATED_ON','UPDATED_ON','AISLE_ID', 'AISLE_NAME' ]);
							reader.addListener('data', function(data) {
							    var ingredientsAisleData ={
												id		:		data.INGREDIENT_ID,
												ingname : 		data.INGREDIENT_NAME,
												createddon:		data.CREATED_ON,
												updatedon:		data.UPDATED_ON,
												aisle_id : 		data.AISLE_ID,
												aisle_name : 	data.AISLE_NAME
											}
							        ingredientsdata[ingredientsAisleData.ingname] = ingredientsAisleData; 
							});
							config.ingredientsdata = ingredientsdata;							
						}
						
};

function getkraftuserrecipeData (config,count, index,recipeIDs, recipesCount,wellioReferenceIdSet, userrecipeArr,averageRating,numberOfRating, callback,logger, count1)
{
	
	config.userRecipeId =recipeIDs[index];
	var avgRating = averageRating[index];
	var numRating = numberOfRating[index];
	var flag = 0;
	KraftAPI.getKraftUserRecipes(config,function(responseData)
	{
		
		
		var userRecipes;
		if(parseAndCheckIsJson(responseData))
		{
			try
			{
				userRecipes = JSON.parse(responseData);
			}
			catch(Exception)
			{
				userRecipes = responseData;
			}
			//console.log("UserRecipeID : " + JSON.stringify(responseData) + '\n');
			var jsonBody = {};
			var headerObj = {};
			if((userRecipes.UserRecipe) && ((userRecipes.UserRecipe.Serves) || userRecipes.UserRecipe.Serves >= 0) && hasContent(userRecipes.UserRecipe.Serves))
			{
				headerObj.servings = userRecipes.UserRecipe.Serves;
			}
			else
			{
				flag = 1
				console.log("userRecipes.UserRecipe.Serves does not exist: " )
				logger.write(config.userRecipeId + " userRecipes.UserRecipe.Serves does not exist: ");
			}
			if((userRecipes.UserRecipe) &&(userRecipes.UserRecipe.RecipeName) && hasContent(userRecipes.UserRecipe.RecipeName))
			{
				headerObj.title = userRecipes.UserRecipe.RecipeName;
			}
			else
			{
				flag = 1
				console.log("userRecipes.UserRecipe.RecipeName does not exist: " )
				logger.write(config.userRecipeId + " userRecipes.UserRecipe.RecipeName does not exist:  " );
			}
			headerObj.description = "Got 4 ingredients and 10 minutes? Then you’re on your way to an apple coleslaw bursting with flavor. Try this Sweet & Tangy Apple Coleslaw as a smart side. ";
			headerObj.social_media_score = 30
			var sourceObj = {};
			if((userRecipes.UserRecipe) && (userRecipes.UserRecipe.UserRecipeID) && hasContent(userRecipes.UserRecipe.UserRecipeID))
			{
				sourceObj.source_url = String(userRecipes.UserRecipe.UserRecipeID);
			}
			else
			{
				flag = 1
				console.log("userRecipes.UserRecipe.UserRecipeID does not exist: " )
				logger.write(config.userRecipeId + " userRecipes.UserRecipe.UserRecipeID does not exist: " )
			}
			
			sourceObj.source_name = "KraftMemberRecipes";
			headerObj.source = sourceObj;
			var timingObj = {};
			if((userRecipes.UserRecipe) && ((userRecipes.UserRecipe.PrepTime) || userRecipes.UserRecipe.PrepTime >= 0) && hasContent(userRecipes.UserRecipe.PrepTime))
			{
				timingObj.prep_time = userRecipes.UserRecipe.PrepTime;
			}
			else
			{
				flag = 1
				console.log("userRecipes.UserRecipe.PrepTime does not exist: " )
				logger.write(config.userRecipeId + " userRecipes.UserRecipe.PrepTime does not exist:  " )
			}
			timingObj.cook_time = 0;
			timingObj.total_time = 0;
			headerObj.timing = timingObj;
			var ingredientsArr = [];
			var ingredientsarr =[];
			if((userRecipes.UserRecipe) && (userRecipes.UserRecipe.Ingredients) && hasContent(userRecipes.UserRecipe.Ingredients))
			{
				ingredientsarr =  (userRecipes.UserRecipe.Ingredients).trim().split('\r\n');
			}
			else
			{
				flag = 1
				console.log("userRecipes.UserRecipe.Ingredients does not exist: " )
				logger.write(config.userRecipeId + " userRecipes.UserRecipe.Ingredients does not exist:  " )
			}
			
			ingredientsarr.forEach(index =>
				{
				index = index.replace('-','');
					if(index.length > 0)
					{
					var ingredientsObj = {};
					ingredientsObj.ingredients = index;
					ingredientsArr.push(ingredientsObj);
					}
				
				})
			
			var preparationsArr = [];
			
			var preparationsarr = [];
			if((userRecipes.UserRecipe) && (userRecipes.UserRecipe.Preparation) && hasContent(userRecipes.UserRecipe.Preparation))
			{
				preparationsarr = (userRecipes.UserRecipe.Preparation).split('\r\n');
			}
			else
			{
				flag = 1
				console.log("userRecipes.UserRecipe.Preparation does not exist: " )
				logger.write(config.userRecipeId + " userRecipes.UserRecipe.Preparation does not exist:  " )
			}
			preparationsarr.forEach(index =>
			{
				
				if(index.length > 0)
				{
					var preparationObj = {};
					preparationObj.preparation = index;
					preparationsArr.push(preparationObj);
				}
				
			})
				
			jsonBody.header = headerObj;
			jsonBody.ingredients = ingredientsArr;
			jsonBody.preparations = preparationsArr;
			var postbody = JSON.stringify(jsonBody);
			
			var userrecipeJSON = {};
			userrecipeJSON.id = config.userRecipeId;
			userrecipeJSON.kraft = responseData;
			userrecipeJSON.numberOfRating = numRating;
			userrecipeJSON.averageRating = avgRating;
	
			userrecipeArr.push(JSON.stringify(userrecipeJSON));
			//console.log("userrecipeArr: " + userrecipeArr)
			if(flag == 0)
			{
				wellioPostCall(config,index,postbody, count,recipeIDs,recipesCount,wellioReferenceIdSet,userrecipeJSON,userrecipeArr,userrecipeArr,averageRating,numberOfRating,callback,logger,count1);
			}
			else
			{
				count = count +1;
				if(count < recipesCount) 
				{
					getkraftuserrecipeData(config,count, index+1,recipeIDs, recipesCount,wellioReferenceIdSet,userrecipeArr,averageRating,numberOfRating, callback,logger, count1);
				}
				else if(count1 < 0 && count == recipesCount)
				{
					count1 = 0;
					//console.log("wellioReferenceIdSet: " + wellioReferenceIdSet)
					var wellioIndex =0;
					wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
				}
			}
			
		}
		else
		{
			console.log("Invalid response from kraft userrecipe api for : " + config.userRecipeId);
			logger.write("\nInvalid response from kraft userrecipe api for : " + config.userRecipeId)
			count = count +1;
			if(count < recipesCount) 
			{
				getkraftuserrecipeData(config,count, index+1,recipeIDs, recipesCount,wellioReferenceIdSet,userrecipeArr,averageRating,numberOfRating, callback,logger,count1);
			}
			else if(count1 < 0 && count == recipesCount)
			{
				count1 = 0;
				//console.log("wellioReferenceIdSet: " + wellioReferenceIdSet)
				var wellioIndex =0;
				wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
			}
		}
	});
}
function wellioPostCall(config,index,postbody, count,recipeIDs,recipesCount,wellioReferenceIdSet,userrecipeJSON,userrecipeArr,userrecipeArr,averageRating,numberOfRating, callback, logger,count1)
{
	var proxy = 'http://kavishree.s:skava%4012345@10.68.248.34:80';
	var agent = new HttpsProxyAgent(proxy);
	var options = {
		uri:"http://kaiseki.endpoints.wellio-integration.cloud.goog/api/recipes/?key=AIzaSyB7HKCPVdsGOkn46LlzOHUpLqzo2GKcdVc",
		method: "POST",
		body:postbody,
		headers: {
			"Content-Type": "application/json"
			}
			//agent:agent
		};
		requestMod(options, function (error, response, body, callback) {
		if(parseAndCheckIsJson(body))
		{
			count = count + 1;
			//console.log("response Code: " + response.statusCode);
			//console.log("response statusMessage: " + response.statusMessage);
			if (!error && (response.statusCode == 200 || response.statusCode == 201)) {
				
				//console.log("wellio response : " + body + "\n");
				var wellioResponse;
				
				try
				{
					wellioResponse = JSON.parse(body);
				}
				catch(Exception)
				{
					wellioResponse = body;
				}
				if((wellioResponse.header) && (wellioResponse.header.id) && hasContent(wellioResponse.header.id))
				{
					wellioReferenceIdSet.push(wellioResponse.header.id);
				}
				
				//console.log("wellioReferenceId: " +  wellioResponse.header.id);
				if(recipeIDs.length  == count)
				{
					count1 = 0;
					//console.log("wellioReferenceIdSet: " + wellioReferenceIdSet)
					var wellioIndex =0;
					wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
				}
				if(count < recipesCount) 
				{
					getkraftuserrecipeData(config,count, index+1,recipeIDs, recipesCount,wellioReferenceIdSet,userrecipeArr,averageRating,numberOfRating, callback, logger, count1);
				}
				else if(count1 < 0 && count == recipesCount)
				{
					count1 = 0;
					//console.log("wellioReferenceIdSet: " + wellioReferenceIdSet)
					var wellioIndex =0;
					wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
				}
			}
			else{
				console.log("error: " + error);
				//console.log("response Code: " + response.statusCode);
				//console.log("response statusMessage: " + response.statusMessage);
				console.log("Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog postCall");
				logger.write(config.userRecipeId + " Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog postCall \n" )
				if(count < recipesCount) 
				{
					getkraftuserrecipeData(config,count, index+1,recipeIDs, recipesCount,wellioReferenceIdSet,userrecipeArr,averageRating,numberOfRating, callback, logger,count1);
				}
				else if(count1 < 0 && count == recipesCount)
				{
					count1 = 0;
					//console.log("wellioReferenceIdSet: " + wellioReferenceIdSet)
					var wellioIndex =0;
					wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
				}
			}
		}
		else
		{
			count = count + 1;
			console.log("error: " + error);
			//console.log("response Code: " + response.statusCode);
			//console.log("response statusMessage: " + response.statusMessage);
			console.log("Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog postCall");
			logger.write(config.userRecipeId + " Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog postCall \n" )
			if(count < recipesCount) 
			{
				getkraftuserrecipeData(config,count, index+1,recipeIDs, recipesCount,wellioReferenceIdSet,userrecipeArr,averageRating,numberOfRating, callback, logger,count1);
			}
			else if(count1 < 0 && count == recipesCount)
			{
				count1 = 0;
				//console.log("wellioReferenceIdSet: " + wellioReferenceIdSet)
				var wellioIndex =0;
				wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
			}
		}
	});
	
}
function writeInLocal(workbook, filename, callback) {
	filename = filename || new Date().getTime().toString(),
	filepath = path.join(filename + '.xlsx');
	console.log("filepath: " + filepath);

	console.log(path.join(__dirname, 'exports/output/', filename + '.xlsx'));
	workbook.save(path.join(__dirname, 'exports/output/', filename + '.xlsx'), function() {
		console.log("writing complete");
		callback();
	});
}

function wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger)
{
	if(wellioReferenceIdSet.length > 0)
	{
		count1 = count1 + 1;
		var userrecipeJSONObj = {};
		config.wellioReferenceId = wellioReferenceIdSet[wellioIndex];
		KraftAPI.getWellioGetUserRecipes(config,function(wellioResponseData)
		{
			//console.log("wellioResponseData: " + JSON.stringify(wellioResponseData));

			//userrecipeJSON.wellio = wellioResponseData;
			if(parseAndCheckIsJson(wellioResponseData))
			{
				var wellioJsonObj;
				try
				{
					wellioJsonObj = JSON.parse(wellioResponseData);
				}
				catch(Exception)
				{
					wellioJsonObj = wellioResponseData;
				}
				var userrecipeID;
				if((wellioJsonObj.header) && (wellioJsonObj.header.source) && (wellioJsonObj.header.source.source_url) && hasContent(wellioJsonObj.header.source.source_url))
				{
					userrecipeID = wellioJsonObj.header.source.source_url;
				}
				
				//console.log("userrecipeArr.length: " +  userrecipeArr.length)
				userrecipeArr.forEach(index=>
				{
					var resultJson;
					if(parseAndCheckIsJson(index))
					{
						try
						{
							resultJson = JSON.parse(index);
						}
						catch(Exception)
						{
							resultJson = index;
						}
					
						
						if(userrecipeID != null && resultJson.id == userrecipeID)
						{
							var kraftJSON;
							if(parseAndCheckIsJson(resultJson.kraft))
							{
								try
								{
									kraftJSON = JSON.parse(resultJson.kraft);
								}
								catch(Exception)
								{
									kraftJSON = resultJson.kraft;
								}
								userrecipeJSONObj.kraft = kraftJSON;
							}
							
							var averageRating = resultJson.averageRating
							userrecipeJSONObj.averageRating = averageRating;
							var numberOfRating = resultJson.numberOfRating;
							userrecipeJSONObj.numberOfRating=numberOfRating;
						}
					}
				})
				userrecipeJSONObj.wellio = wellioResponseData;
				//console.log("userrecipeJSONObj: " + JSON.stringify(userrecipeJSONObj))

				getReviewData(userrecipeID, null, function(reviewsResponse) {
					//console.log(">>"+productId);
					//importProducts.addProduct(10001, reviewsResponse, result, workbook);
					importUserRecipes.addProduct(userrecipeJSONObj, reviewsResponse, config, workbook);
					facets = importUserRecipes.addFacets(userrecipeJSONObj);
					if(wellioReferenceIdSet.length == count1)
					{
						importUserRecipes.addFacet(facets, config.languageId, workbook, function() {

							var filename = parseInt(config.startIndex) + "_" +parseInt(config.endIndex);
							console.log("filename: " + filename);
							writeInLocal(workbook, filename, function(){
								callback;
							}) ;
						});
					}
					else if(count1 < wellioReferenceIdSet.length)
					{
						wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex+1,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
					}
				});
			}
			else if(count1 < wellioReferenceIdSet.length)
			{
				console.log("Invalid response from wellioGetCall for " + config.wellioReferenceId);
				wellioGetCall(config, wellioReferenceIdSet,userrecipeJSON,wellioIndex+1,count1,workbook,userrecipeArr,averageRating,numberOfRating, callback, logger);
			}
			else if(count1 == wellioReferenceIdSet.length)
			{
				importUserRecipes.addFacet(facets, config.languageId, workbook, function() {

					var filename = parseInt(config.startIndex) + "_" +parseInt(config.endIndex);
					console.log("filename: " + filename);
					writeInLocal(workbook, filename, function(){
						callback;
					}) ;
				});
			}
		});
	}
	else
	{
		console.log("wellioReferenceIdSet is null: " + wellioReferenceIdSet);
		console.log("No data to write in excel");
		
	}
}

function getReviewData(productId, filename, callback) {
	var filepath = path.join(__dirname, 'exports/input/members-KraftRecipes-en-Rating.xlsx'),//, filename),
		sheetName = 'Sheet1';
		
		//console.log('productId :' + productId);
	workbook.read(filepath, sheetName, function(sheetData) {
		if (sheetData) {
			reviews = [];
			sheetData.eachRow({ includeEmpty: true }, function(row, rowNumber) {
				if (rowNumber > 1) {
					var sheetprodId = row.getCell(1).value;
					if (sheetprodId && productId == sheetprodId )
					{
						//console.log('sheetprodId === '+sheetprodId);
						var averagerating = {
								name : 'averagerating',
								value : row.getCell(3).value
							};
						if(averagerating.value >= 0)
						reviews.push(averagerating);	
						var reviewcount = {
								name : 'numberofratings',
								value : row.getCell(2).value
							};
						if(reviewcount.value >= 0)
						reviews.push(reviewcount);	
					}
				}						              							              		
			});
			callback(reviews);
		}
	});
	//callback(review);							
}

function hasContent(val){
	return !(val===null || val==='')
	return val!==null && String(val).toUpperCase()!=='NULL' && val!=='' && String(val).replace(/\s/g,'')!==''
}

function parseAndCheckIsJson(obj)
{
	var flag = true;
	try{
		JSON.parse(obj)
	}
	catch(Exception){
		flag = false
	}
	return flag;
}

module.exports = {
	generatePIMSheet:generatePIMSheet,
	getkraftuserrecipeData:getkraftuserrecipeData,
	wellioPostCall:wellioPostCall
}