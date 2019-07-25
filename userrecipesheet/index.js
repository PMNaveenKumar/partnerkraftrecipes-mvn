console.log("Hello World!!");
const express = require('express')
const openport = require('openport');
const app = express();
const requestMod = require('request');
const csv = require('csvtojson');
var csvya = require('ya-csv');
var PIMWorkbook = require('./toXLS/utils/PIMWorkbook.js')
var workbook = new PIMWorkbook();
const toXLS = require('./toXLS/');
const fs = require('fs');
const HttpsProxyAgent = require('http-proxy-agent');
var proxy = 'http://kavishree.s:skava%401234@10.68.248.34:80';
var agent = new HttpsProxyAgent(proxy);

var outputLogFile = "/toXLS/exports/input/FinalCompare_1" +".csv"
var logger = fs.createWriteStream(__dirname + outputLogFile, { flags: 'a+' });


app.get('/posttest', function (request, response) {
	var userRecipes ='';
	var options1 = {
        uri:"http://stgapi.kraftapps.com/v2/Community/UserRecipes/121926",
		method: "GET"
	};
	requestMod(options1, function (error, response, body) {
		if (!error && response.statusCode == 200) {
			userRecipes = JSON.parse(body);
			console.log("UserRecipeID : " + userRecipes.UserRecipe.UserRecipeID + '\n');
			var options = {
				uri:"http://kaiseki.endpoints.wellio-integration.cloud.goog/api/recipes/?key=AIzaSyB7HKCPVdsGOkn46LlzOHUpLqzo2GKcdVc",
				method: "POST",
				body:JSON.stringify({
					"header.description": "userRecipes.UserRecipe.UserRecipeID", //need to discuss
					"header.servings": 4,
					"header.social_media_score":" userRecipes.UserRecipe.UserRecipeID", //need to discuss
					"header.source.source_url": 121926,
					"header.source.source_name": "KraftMemberRecipes", //need to discuss - node not available
					"header.timing.prep_time": 0,
					"header.timing.cook_time": 0,
					"header.timing.total_time": 0,
					"header.title": "userRecipes.UserRecipe.RecipeName",
					"ingredients[].ingredients":" userRecipes.UserRecipe.Ingredients",
					"preparations[].preparation": "userRecipes.UserRecipe.Preparation"
				  }),
				headers: {
					"accept": "application/json",
					"Content-Type": "application/json"
				}
			};
			requestMod(options, function (error, response, body) {
				if (!error && response.statusCode == 200) {
					console.log("kaiseki.endpoints.wellio-integration.cloud.goog - Success"+ '\n');
				  }
				  else{
					  console.log("Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog"+ '\n');
				  }
			  });
		  }
		  else{
			console.log("Request failed!!! - stgapi.kraftapps.com"+ '\n');
		  }
	  });
    response.send("success");
});



app.get('/posttest1', function (request, response) {
	var userRecipes ='';
	var options1 = {
        uri:"http://stgapi.kraftapps.com/v2/Community/UserRecipes/121926",
        method: "GET",
        agent:agent
	};
	requestMod(options1, function (error, response, body) {
		if (!error && response.statusCode == 200) {
			userRecipes = JSON.parse(body);
			console.log("UserRecipeID : " + userRecipes.UserRecipe.UserRecipeID + '\n');
			var options = {
				uri:"http://kaiseki.endpoints.wellio-integration.cloud.goog/api/recipes/?key=AIzaSyB7HKCPVdsGOkn46LlzOHUpLqzo2GKcdVc",
				method: "POST",
				body:JSON.stringify({
					"header.description": "userRecipes.UserRecipe.UserRecipeID", //need to discuss
					"header.servings": 4,
					"header.social_media_score":" userRecipes.UserRecipe.UserRecipeID", //need to discuss
					"header.source.source_url": 121926,
					"header.source.source_name": "KraftMemberRecipes", //need to discuss - node not available
					"header.timing.prep_time": 0,
					"header.timing.cook_time": 0,
					"header.timing.total_time": 0,
					"header.title": "userRecipes.UserRecipe.RecipeName",
					"ingredients[].ingredients":" userRecipes.UserRecipe.Ingredients",
					"preparations[].preparation": "userRecipes.UserRecipe.Preparation"
				  }),
				headers: {
					"accept": "application/json",
					"Content-Type": "application/json"
				}
			};
			requestMod(options, function (error, response, body) {
				if (!error && response.statusCode == 200) {
					console.log("kaiseki.endpoints.wellio-integration.cloud.goog - Success"+ '\n');
				  }
				  else{
					  console.log("Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog"+ '\n');
				  }
			  });
		  }
		  else{
			console.log("Request failed!!! - stgapi.kraftapps.com"+ '\n');
		  }
	  });
    response.send("success");
});


app.get('/excelread', function (request, response) {
    csv({
        delimiter:'\t'
    }).fromFile("Users-Recipes.csv").then((jsonArr)=>{
        console.log(jsonArr);
        jsonArr.forEach(function(jsonObj) {
            var siteId = jsonObj.SiteID;
            var approved = jsonObj.Approved;
            var public = jsonObj.Pubilc;
            if(((siteId == 1) || (siteId == 90)) && (approved == 1) && (public == 1))
            {
                var recipeId = jsonObj.RecipeID;
                console.log("\nrecipeId: " + recipeId);
                var userRecipes ='';
                var options1 = {
                    uri:"http://stgapi.kraftapps.com/v2/Community/UserRecipes/" + recipeId,
                    method: "GET",
                    agent:agent
                };
               requestMod(options1, function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        userRecipes = JSON.parse(body);
                        console.log("UserRecipeID : " + userRecipes.UserRecipe.UserRecipeID + '\n');
                        
                        var options = {
                            uri:"http://kaiseki.endpoints.wellio-integration.cloud.goog/api/recipes/?key=AIzaSyB7HKCPVdsGOkn46LlzOHUpLqzo2GKcdVc",
                            method: "POST",
                            body:JSON.stringify({
                                
                                    "header": {
                                      "description": "Got 4 ingredients and 10 minutes? Then youâ€™re on your way to an apple coleslaw bursting with flavor. Try this Sweet & Tangy Apple Coleslaw as a smart side. ",
                                      "servings": 4,
                                      "social_media_score": 30,
                                      "source": {
                                        "source_url": "387339", 
                                        "source_name": "KraftRecipes"
                                      },
                                      "timing": {
                                        "prep_time": 20,
                                        "cook_time": 22,
                                        "total_time": 42
                                      },
                                      "title": "Sweet & Tangy Apple Coleslaw"
                                    },
                                    "ingredients": [
                                      {
                                        "ingredients": "3/4 cup MIRACLE WHIP Light Dressing"
                                      },
                                      {
                                        "ingredients": "1 Tbsp. honey"
                                      }
                                    ],
                                    "preparations": [
                                      {
                                        "preparation": "Mix dressing and honey in large bowl until blended."
                                      },
                                      {
                                        "preparation": "Add remaining ingredients; mix lightly."
                                      },
                                      {
                                        "preparation": "Refrigerate 1 hour.."
                                      }
                                    ]
                                   
                            }),
                            headers: {
                                "accept": "application/json",
                                "Content-Type": "application/json"
                            },
                            agent:agent
                        };
                        requestMod(options, function (error, response, body) {
                            console.log("response: " + response.statusCode);
                            console.log("error: " + error);
                            console.log("response: " + response.statusMessage);
                            if (!error && (response.statusCode == 200 || response.statusCode == 201)) {
                                console.log(body);
                                console.log("kaiseki.endpoints.wellio-integration.cloud.goog - Success"+ '\n');
                            }
                            else{
                                console.log("Request failed!!! - kaiseki.endpoints.wellio-integration.cloud.goog"+ '\n');
                            }
                        });
                     }
                     else{
                         console.log("Request failed!!! - stgapi.kraftapps.com"+ '\n');
                     }
                 });
            }
           
        });
    
});
    response.send("success");
});

app.get('/writetoexcel', function (request, response) {

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
    response.send("success");
});



app.get('/generate_pim_sheet', function(request, response) {
	var config = {
		brandId 		: 	request.query.brandId || 1,
		languageId 		: 	request.query.languageId || 1,
		pageNo 			: 	request.query.pageNo || 1,
        pageSize 		: 	request.query.pageSize || 1,
        startIndex 		: 	request.query.startIndex || 0,
        endIndex        :   request.query.endIndex || 0,
		lastUpdatedDate : 	request.query.lastUpdatedDate || '0401970',
		domainName 		: 	request.query.domainName
	};

	if (request.query.altLanguageId != null && request.query.altSiteId != null) {
		config.altDetails = {
			languageId 	: 	request.query.altLanguageId,
			siteId 		: 	request.query.altSiteId
		};
	}

	console.log('Starttime\t' + (new Date()).toLocaleString(), true);

	try {
		toXLS.generatePIMSheet.init(config, function(data) {
			response.send(data);
		});
	} catch(exception) {
		response.send({ status: 'Failure', message: exception.message });
    }
    response.send("success"); 
});

app.get('/readjson', function(request, response) {


let rawdata = fs.readFileSync('wellio_212.json');  
let student = {};
var wellioJSON = JSON.parse(rawdata)
console.log("wellioJSON: " + JSON.stringify(wellioJSON));
let kraft =  fs.readFileSync('wellio_387339.json');
let kraftJSON = JSON.parse(kraft);
console.log("kraftJSON:  "+ kraftJSON);
console.log(student); 
response.send("success"); 
});


app.get('/findmissingids', function(request, response) {
    var config = {
        startIndex 		: 	request.query.startIndex || 0,
        endIndex        :   request.query.endIndex || 0
	};
    var originalrecipeIDs = [];
    var recipeIDs=[];
    var sheetRecipeIDs=[];
    var i = -1;
    csv({
        delimiter:'\t'
        
    }).fromFile("new_20_09.csv").then((jsonArr)=>{
        jsonArr.forEach(function(jsonObj) {
                i = i+1;
                originalrecipeIDs[i] = jsonObj.RecipeID;
        });
        recipeIDs = originalrecipeIDs.slice(parseInt(config.startIndex), parseInt(config.endIndex));
        i = -1;
        csv({
            delimiter:'\t'
            
        }).fromFile("new-recipes.csv").then((jsonArr)=>{
            jsonArr.forEach(function(jsonObj) {
                    i = i+1;
                    sheetRecipeIDs[i] = jsonObj.RecipeID;
            });
            
            let difference = recipeIDs.filter(x => !sheetRecipeIDs.includes(x));
            console.log("difference: " + difference + " count = " + difference.length);
            difference.forEach(element => {
                logger.write(element + "\n");
            });
        });
    });
    
    
    response.send("success"); 
});

openport.find({ startingPort: 5000 }, function(err, port) {
    if (err) {
        console.log(err)
    } else {
        var server = app.listen(port, function() {
            // console.log('Running on port', port)
            // opn(`http://localhost:${port}/`, { app: ['google chrome'] })
        });
        server.timeout = 1000 * 60 * 60 * 10;
    }
})