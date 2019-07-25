function cleanup(entry,config, cb){
	var result = {}

	entry.ID = entry.GTIN
	
	var kraftJson = entry.kraft;
	var wellioJson = JSON.parse(entry.wellio);

	if(entry.averageRating || entry.averageRating >= 0)
	{
		result.averagerating = entry.averageRating;
	}

	if(entry.numberOfRating || entry.numberOfRating >= 0)
	{
		result.numberofratings = entry.numberOfRating;
	}
	
	if((kraftJson.UserRecipe))
	{

		if((kraftJson.UserRecipe.CreateByUser))
		{
			result.author = kraftJson.UserRecipe.CreateByUser;
		}

		if((kraftJson.UserRecipe.Ingredients))
		{
			result.ingredients = kraftJson.UserRecipe.Ingredients;
		}
		
		if (kraftJson.UserRecipe.Assets && kraftJson.UserRecipe.Assets.length) {
			var altImagesObj = {};
			kraftJson.UserRecipe.Assets.map(function(asset, index) {
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
				altImagesObj[altImage][0].url = altImagesObj[altImage][0].url.replace(/(^\w+:|^)/, '');
				if(altImagesObj[altImage][0].url.match('/ugc/recipeimages/'))
				{
					var recipeimg= altImagesObj[altImage][0].url.match(/[\w-]+\.jpg/g);
					altImagesObj[altImage][0].url = "https://d22r0r521qk72y.cloudfront.net/khassets/ugc/recipeimages/"+recipeimg;
					finalAltImages.push(altImagesObj[altImage][0].url);
				}
				else if (altImagesObj[altImage][0].url.match('/user-recipe-image/'))
				{
					var userrecipeimg= altImagesObj[altImage][0].url.match(/[\w-]+\.jpg/g);
					altImagesObj[altImage][0].url = "https://d22r0r521qk72y.cloudfront.net/khassets/ugc/kraft-recipes/user-recipe-image/640x428/"+userrecipeimg;
					finalAltImages.push(altImagesObj[altImage][0].url);
				}
			}
			result.altimages = finalAltImages.join(',').toString();
		}

		if((kraftJson.UserRecipe.Ingredients))
		{
			var ingredientsArr = [];			
			var ingredientsArr1 = [];			
			var ingredientsArr =  (kraftJson.UserRecipe.Ingredients).split('\r\n');			
			ingredientsArr.forEach(index =>
			{
				if(index.length > 0)
				{
					var ingredientsObj = {};
					ingredientsObj.IngredientFullName = index;
					//const regex = /(([0-9\/\-\s\.]+)(\s*(oz|tsp|cups|cup|in|Tbsp|pkt|lb|bar|pounds|tub|pkg|cans|can|mL|L|package|pound|boxes|box|large|small|Tablespoons|tablespoons|tablespoon|jars|sticks|stick|bag|jar|qt|teaspoon|medium|tablespoons|teaspoons|ounces|med|loaf|bushel|container|heads|slices|slice|cloves|clove|squares|Dash|dash|dashes|square|lb. medium|pkg. oz|pint|pkt|envelope|pt|bottle|packet|tbs|sq.|lg.|sleeve|cubes|cube|env|whole|pouches|jumbo|scoops|pieces|head|stalk|bunches|bunch|small loaf|sheets|sheet|drops|drop|stalks|quart|strips|sm can|1b|Tablespoon)[\.]*)[\D+])(.*)/gm;
					const regex = /((^\d+[\/\s*-.\d*]*)(\s*oz|tsp|cups|cup|in|Tbsp|pkts|lb|bars|bar|pounds|tubs|tub|pkgs|pkg|cans|can|mL|L|package|pounds|pound|boxes|box|large|small|Tablespoons|tablespoons|tablespoon|jars|sticks|stick|bag|jar|qt|teaspoon|medium|tablespoons|teaspoons|ounces|med|loaf|bushel|container|heads|slices|slice|cloves|clove|squares|Dash|dash|dashes|square|lb. medium|pkg|oz|pint|pkt|envelope|pt|bottle|packet|tbs|sq.|lg.|sleeve|cubes|cube|env|whole|pouches|jumbo|scoops|pieces|head|stalk|bunches|bunch|small loaf|sheets|sheet|drops|drop|stalks|quart|strips)[\.]*)\s*(.*)/gm;
					const to_regex = /((^\d+[\/\s*-.\d*]*to\s*\d+[\/\s*-.\d*]*)(\s*oz|tsp|cups|cup|in|Tbsp|pkts|lb|bars|bar|pounds|tubs|tub|pkgs|pkg|cans|can|mL|L|package|pounds|pound|boxes|box|large|small|Tablespoons|tablespoons|tablespoon|jars|sticks|stick|bag|jar|qt|teaspoon|medium|tablespoons|teaspoons|ounces|med|loaf|bushel|container|heads|slices|slice|cloves|clove|squares|Dash|dash|dashes|square|lb. medium|pkg|oz|pint|pkt|envelope|pt|bottle|packet|tbs|sq.|lg.|sleeve|cubes|cube|env|whole|pouches|jumbo|scoops|pieces|head|stalk|bunches|bunch|small loaf|sheets|sheet|drops|drop|stalks|quart|strips)[\.]*)(.*)/gm;  
					const or_regex = /((^\d+[\/\s*-.\d*]*or\s*\d+[\/\s*-.\d*]*)(\s*oz|tsp|cups|cup|in|Tbsp|pkts|lb|bars|bar|pounds|tubs|tub|pkgs|pkg|cans|can|mL|L|package|pounds|pound|boxes|box|large|small|Tablespoons|tablespoons|tablespoon|jars|sticks|stick|bag|jar|qt|teaspoon|medium|tablespoons|teaspoons|ounces|med|loaf|bushel|container|heads|slices|slice|cloves|clove|squares|Dash|dash|dashes|square|lb. medium|pkg|oz|pint|pkt|envelope|pt|bottle|packet|tbs|sq.|lg.|sleeve|cubes|cube|env|whole|pouches|jumbo|scoops|pieces|head|stalk|bunches|bunch|small loaf|sheets|sheet|drops|drop|stalks|quart|strips)[\.]*)(.*)/gm;  
					const Others_regex = /(^\d+[\/\s*-.\d*]*)(.*)/gm;
					var data1 = regex.exec(index);
					var data2 = to_regex.exec(index);
					var data3 = or_regex.exec(index);
					var data4 = Others_regex.exec(index);
					if( data1 != null && data1.length > 0 )
					{
						if(data1[0] != null)
						{
							ingredientsObj.FullMeasure = data1[1];
						}
						if(data1[1] != null)
						{
							ingredientsObj.QuantityText = data1[2];
						}					
						if(data1[2] != null)
						{
							ingredientsObj.QuantityUnit = data1[3];
						}
						if(data1[4] != null)
						{
							ingredientsObj.IngredientName = data1[4];
						}						
					}
					else if(data2 != null && data2.length > 0 )
					{
						if(data2[1] != null)
						{
							ingredientsObj.FullMeasure = data2[1];
						}
						if(data2[2] != null)
						{	
							ingredientsObj.QuantityUnit = data2[2];
						}					
						if(data2[3] != null)
						{
							ingredientsObj.QuantityText = data2[3];
						}
						if(data2[4] != null)
						{
							ingredientsObj.IngredientName = data2[4];
						}						
					}
					else if(data3 != null && data3.length > 0 )
					{
						if(data3[1] != null)
						{
							ingredientsObj.FullMeasure = data3[1];
						}
						if(data3[2] != null)
						{
							ingredientsObj.QuantityUnit = data3[2];
						}
						if(data3[3] != null)
						{	
							ingredientsObj.QuantityText = data3[3];
						}
						if(data3[4] != null)
						{
							ingredientsObj.IngredientName = data3[4];
						}						
					}
					else if(data4 != null && data4.length > 0 )
					{						
						if(data4[1] != null)
						{
							ingredientsObj.QuantityText = data4[1];
						}
						if(data4[2] != null)
						{
							ingredientsObj.IngredientName = data4[2];
						}						
					}
					else
					{
						ingredientsObj.IngredientName = index;
						console.log("index--" + index);
					}
					ingredientsArr1.push(ingredientsObj);
				}
			})
			result.ingredients = ingredientsArr1;
		}

		if((kraftJson.UserRecipe.UserRecipeCategory) && (kraftJson.UserRecipe.UserRecipeCategory.UserRecipeCategoryID))
		{
			result.kraftparentcategoryid = kraftJson.UserRecipe.UserRecipeCategory.UserRecipeCategoryID;
		}

		if((kraftJson.UserRecipe.UserRecipeCategory) && (kraftJson.UserRecipe.UserRecipeCategory.Name))
		{
			result.kraftparentcategoryname = kraftJson.UserRecipe.UserRecipeCategory.Name;
		}

		if((kraftJson.UserRecipe.DateCreated))
		{
			var createdDatetime = kraftJson.UserRecipe.DateCreated;
			var pattern = /Date\W(.*)-.*/;
			var match = pattern.exec(createdDatetime);
			var text = match[1];
			result.createddatetime = text
		}

		if((kraftJson.UserRecipe.DateUpdated))
		{
			var updatedDateTime = kraftJson.UserRecipe.DateUpdated;
			var pattern = /Date\W(.*)-.*/;
			var match = pattern.exec(createdDatetime);
			var text = match[1];
			result.updateddatetime = text
		}

		if((kraftJson.UserRecipe.ModerationComment))
		{
			if(kraftJson.UserRecipe.ModerationComment != null && (JSON.stringify(kraftJson.UserRecipe.ModerationComment)).length > 0)
			{
				result.moderationcomment = kraftJson.UserRecipe.ModerationComment;
			}
		}

		if((kraftJson.UserRecipe.Notes))
		{
			result.notes = kraftJson.UserRecipe.Notes;
		}

		if((kraftJson.UserRecipe.Makes))
		{
			result.makes = kraftJson.UserRecipe.Makes;
		}

		if((kraftJson.UserRecipe.PrepTime))
		{
			result.preparationtime = kraftJson.UserRecipe.PrepTime;
		}

		if((kraftJson.UserRecipe.RecipeName))
		{
			result.name = kraftJson.UserRecipe.RecipeName;
		}

		if((kraftJson.UserRecipe.SEOPageName))
		{
			result.seoname = kraftJson.UserRecipe.SEOPageName;
		}

		if((kraftJson.UserRecipe.Serves >=0))
		{
			result.numberofservings = kraftJson.UserRecipe.Serves;
		}
		if((kraftJson.UserRecipe.UpdatedByUser))
		{
			result.updatedbyuser = kraftJson.UserRecipe.UpdatedByUser;
		}

		if((kraftJson.UserRecipe.UserNBR))
		{
			result.usernbr = kraftJson.UserRecipe.UserNBR;
		}

		if((kraftJson.UserRecipe.UserRecipeID))
		{
			var recipeid = kraftJson.UserRecipe.UserRecipeID;
			//result.recipeid = recipeid;
			result.kraftid = recipeid;

			var recipeidzeroappend = recipeid + "";
			while (recipeidzeroappend.length < 8) recipeidzeroappend = "0" + recipeidzeroappend;
			

			if((kraftJson.UserRecipe.RecipeName))
			{
				var RecipeName = kraftJson.UserRecipe.RecipeName;
				//result.semanticid = "/recipe/" + recipeid + "/" + RecipeName;
				result.semanticId = "/member-recipe/" + recipeidzeroappend + "/" + RecipeName.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
			}			
		}

		if((kraftJson.UserRecipe.Assets))
		{
			if((kraftJson.UserRecipe.Assets).length > 0)
			{
				result.assets = kraftJson.UserRecipe.Assets;
			}
		}
		
		if((kraftJson.UserRecipe.RecipeImage))
		{
			if(kraftJson.UserRecipe.RecipeImage != null && (JSON.stringify(kraftJson.UserRecipe.RecipeImage)).length > 0)
			{
				if(kraftJson.UserRecipe.RecipeImage.match('/ugc/recipeimages/'))
				{
					var recipeimg= kraftJson.UserRecipe.RecipeImage.match(/[\w-]+\.jpg/g);
					result.image = "https://d22r0r521qk72y.cloudfront.net/khassets/ugc/recipeimages/"+recipeimg;
				}
				else if (kraftJson.UserRecipe.RecipeImage.match('/user-recipe-image/'))
				{
					var userrecipeimg= kraftJson.UserRecipe.RecipeImage.match(/.*\/user-recipe-image\/(.*)/);
					result.image = "https://d22r0r521qk72y.cloudfront.net/khassets/ugc/kraft-recipes/user-recipe-image/"+userrecipeimg;				
				}				
			}			
		}

		if((kraftJson.UserRecipe.Preparation))
		{
			var preparationsArr = [];
			
			var preparationsarr =  (kraftJson.UserRecipe.Preparation).split('\r\n');
			var sequence = 0;
			preparationsarr.forEach(index =>
			{
				if(index.length > 0)
				{
					sequence = sequence + 1;
					var preparationObj = {};
					preparationObj.Description = index;
					preparationObj.Sequence = sequence;
					preparationsArr.push(preparationObj);
				}
			})
			result.preparationsteps = preparationsArr;
		}

		if((kraftJson.UserRecipe.Preparation))
		{
			result.preparationdescription =  (kraftJson.UserRecipe.Preparation).replace(/[\r\n]+/g, '$');
		}
	
		if((kraftJson.UserRecipe.RecipeImage))
		{
			result.hasimage = "true"
		}
		else
		{
			result.hasimage = "false"
		}

		if((kraftJson.UserRecipe.ReadyIn))
		{
			result.totaltime = kraftJson.UserRecipe.ReadyIn
		}
		
		
	}
	
	if((wellioJson.header))
	{

		if((wellioJson.header.derived_labels))
		{
			if((wellioJson.header.derived_labels).length > 0)
			{
				result.dishdetails_text = wellioJson.header.derived_labels.toString();
			}
		}


		if((wellioJson.header.diet_tags))
		{
			if((wellioJson.header.diet_tags).length > 0)
			{
				result.diet = wellioJson.header.diet_tags.toString();
			}
			
		}

		var tags = [];
		if((wellioJson.header.derived_labels) && ((wellioJson.header.derived_labels).length > 0))
		{
			var derived_labels = wellioJson.header.derived_labels;
			for(var i = 0;i<derived_labels.length;i++)
			{
				if(tags.length > 0)
				{
					if(!tags.includes(derived_labels[i]))
					{
						tags.push(derived_labels[i]);
					}
				}
				else
				{
					tags.push(derived_labels[i]);
				}
			}
		}
		if((wellioJson.header.diet_tags) && ((wellioJson.header.diet_tags).length > 0))
		{
			var diet_tags = wellioJson.header.diet_tags;
			for(var i = 0;i<diet_tags.length;i++)
			{
				if(tags.length > 0)
				{
					if(!tags.includes(diet_tags[i]))
					{
						tags.push(diet_tags[i]);
					}
				}
				else
				{
					tags.push(diet_tags[i]);
				}
			}
		}

		if((wellioJson.ingredients) && (wellioJson.ingredients.alternatives) && ((wellioJson.ingredients.alternatives).length > 0))
		{
			var ingredients = wellioJson.ingredients.length;
			result.number_of_ingredients = ingredients;
			var alternatives = wellioJson.ingredients.alternatives;
			for(var i = 0;i<alternatives.length;i++)
			{
				var alternativeJson = alternatives[i];

				if(alternativeJson.entity)
				{
					if(tags.length > 0)
					{
						if(!tags.includes(alternativeJson.entity))
						{
							tags.push(alternativeJson.entity);
						}
					}
					else
					{
						tags.push(alternativeJson.entity);
					}
				}
				
			}
		}

		if(tags.length > 0)
		{
			result.tags = tags.toString();
		}
		

		if((wellioJson.ingredients) && (wellioJson.ingredients).length > 0)
		{
			var ingredientsArr = wellioJson.ingredients;
			var entity = "";
			var amount = "";
			var uom = "";
			var aisle_name = "";
			var aisle_id = "";
			for(var i= 0;i<ingredientsArr.length;i++)
			{
				var ingredientsJSON = ingredientsArr[i];
				if(ingredientsJSON.entity != null)
				{
					entity = entity + ingredientsJSON.entity + ",";
				}
				if(ingredientsJSON.amount != null)
				{
					amount = amount + ingredientsJSON.amount + ",";
				}
				if(ingredientsJSON.uom != null)
				{
					uom = uom + ingredientsJSON.uom + ",";
				}
				var a_name = "EMPTY";
				var a_id = "EMPTY";
			
				if(typeof config.ingredientsdata[ingredientsJSON.entity] != "undefined")
				{
					a_name = ingredientsJSON.entity;
					a_id = config.ingredientsdata[ingredientsJSON.entity].id;
				}
				aisle_name = aisle_name + "," + a_name;
				aisle_id = aisle_id + "," + a_id;
			} 		
			entity =  entity.slice(0, -1);
			result.aisle_id = aisle_id.substr(1).slice(0, -1);
			result.aisle_name = aisle_name.substr(1).slice(0, -1);
			result.ingredientname = entity.toString();
			result.ingredients_quantitynum = amount.slice(0, -1);
			result.ingredients_quantityunit = uom.slice(0, -1);
		}
		
		if((wellioJson.header.nutrition_summary))
		{
			if((wellioJson.header.nutrition_summary.cals_per_serving))
			{
				result.nutritioncalsperserving = wellioJson.header.nutrition_summary.cals_per_serving;
			}

			if((wellioJson.header.nutrition_summary.carbs_per_serving))
			{
				result.nutritioncarbsperserving = wellioJson.header.nutrition_summary.carbs_per_serving;
			}

			if((wellioJson.header.nutrition_summary.confidence))
			{
				result.nutritionconfidence = wellioJson.header.nutrition_summary.confidence;
			}

			if((wellioJson.header.nutrition_summary.fat_per_serving))
			{
				result.nutritionfatperserving = wellioJson.header.nutrition_summary.fat_per_serving;
			}

			if((wellioJson.header.nutrition_summary.nrf_index))
			{
				result.nutritionnrfindex = wellioJson.header.nutrition_summary.nrf_index;
			}

			if((wellioJson.header.nutrition_summary.protein_per_serving))
			{
				result.nutritionproteinperserving = wellioJson.header.nutrition_summary.protein_per_serving;
			}
		}
		

		if((wellioJson.header.price_per_serving != null))
		{
			result.priceperserving = wellioJson.header.price_per_serving;
		}

	}

	
	if((wellioJson.nutrition))
	{
		if((wellioJson.nutrition.derived_nutrition))
		{
			var derivednutrition =[];
			var display ="";
			var nutritionitemid = "";
			var nutritionitemname = "";
			var unit = "";
			var quantity = "";
			derivednutrition =  wellioJson.nutrition.derived_nutrition;
			if(derivednutrition.length > 0)
			{
				for(var i = 0;i<derivednutrition.length;i++)
				{
					//console.log("derivednutrition : " + JSON.stringify(derivednutrition));
					var derivednutritionObj = derivednutrition[i];
					if((derivednutritionObj.display))
					{
						display = display + derivednutritionObj.display + ",";
					}
					else
					{
						display = display + ","
					}
					if((derivednutritionObj.details))
					{
						if((derivednutritionObj.details.name != null))
						{
							nutritionitemname = nutritionitemname + derivednutritionObj.details.name + ",";
						}

						if((derivednutritionObj.details.nutrient_id != null))
						{
							nutritionitemid = nutritionitemid + derivednutritionObj.details.nutrient_id + ",";
						}

						if((derivednutritionObj.details.unit != null))
						{
							unit = unit + derivednutritionObj.details.unit + ",";
						}

						if((derivednutritionObj.details.agg_value != null))
						{
							quantity = quantity + derivednutritionObj.details.agg_value + ",";
						}

						result.nutritiondisplay = display.slice(0, -1);
						result.nutritionitemid = nutritionitemid.slice(0, -1);
						result.nutritionitemname = nutritionitemname.slice(0, -1);
						result.nutritionunit = unit.slice(0, -1);
						result.nutritionaggvalue = quantity.slice(0, -1);
					}
				}
			}
			
		}
	}

	result.producttype = 2;
	result.isrecipe = true;
	
	result.hasvideo=false;
	result.ismemberrecipe=true;
	result.brandid = 1;
	result.languageid = 1;
	result.recipeindustrysector = "Retail";
	entry.__properties = result

	if(cb) cb(entry)
	return entry
}

module.exports = {
	cleanup:cleanup
}