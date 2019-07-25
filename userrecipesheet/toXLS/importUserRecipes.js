var KraftUserRecipesSchema = require('./KraftUserRecipesSchema.js')
var KraftUserRecipesCleanup = require('./KraftUserRecipesCleanup.js')
var facets = {
	author 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
	moderationcomment			:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	numberofingredients			:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	notes						:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	preparationtime				:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
	totaltime					:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: true, value: [] },
	name 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
	seoname 					: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
	assets 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
	hasimage 					: 	{ fieldtype: 3, validationtype: 3, searchable: false, filterable: true, sortable: false, value: 'true-false-false' },
	image 						: 	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-9999' },
	tags 						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
	dishdetails_text			:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: false, value: [] },
	diet     					:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },		
	priceperserving				:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritioncalsperserving		:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritioncarbsperserving	:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritionconfidence			:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritionfatperserving		:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritionnrfindex			:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritionproteinperserving	:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },		
	nutritionitemid 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
	nutritionitemname 			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
	nutritionaggvalue			: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	nutritionunit				: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	nutritiondisplay			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: true, value: [] },
	createddatetime				: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	updateddatetime				: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	makes						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
	preparationsteps			: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: []},
	updatedbyuser				: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: false, sortable: false, value: [] },
	serves						: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: false, value: [] },
	usernbr						: 	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: true, value: [] },
	recipeid					: 	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: false, value: [] },
	producttype					:	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: true, value: 2 },
	semanticid 					:	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: false, sortable: false, value: '0-999'},
	hasvideo					:	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: false, value: false },
	ismemberrecipe				:	{ fieldtype: 0, validationtype: 0, searchable: false, filterable: true, sortable: false, value: true },
	ingredientname 				:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: true, sortable: false, value: [] },
	ingredients_quantitynum		:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
	ingredients_quantityunit	:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
	averagerating				:	{ fieldtype: 2, validationtype: 4, searchable: true, filterable: false, sortable: false, value: [] },
	numberofratings				:	{ fieldtype: 2, validationtype: 4, searchable: false, filterable: true, sortable: true, value: [] },
	brandid						:	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: 1 },
	languageid					:	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: 1 },
	recipeindustrysector		:	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: "Retail"},
	allergens_tags				:	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-9999'},
	taste_tags					:	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-9999'},		
	diet_tags					:	{ fieldtype: 0, validationtype: 0, searchable: true, filterable: true, sortable: false, value: '0-9999'}
};

function addProduct(userrecipeJSONObj, reviews, config, workbook){

	var result = [];
	//console.log("userrecipeJSONObj: " + userrecipeJSONObj);
	//userrecipeJSON = JSON.parse(userrecipeJSON);
	var kraftJson;
	if(userrecipeJSONObj.kraft)
	{
		kraftJson =  userrecipeJSONObj.kraft;
	}
	
	//var wellioJson = JSON.parse(userrecipeJSON.wellio);
	if(typeof kraftJson != 'undefined' && (kraftJson.UserRecipe) && !kraftJson.UserRecipe.UserRecipeID){
		console.log('addProduct: Invalid Product!',product.GTIN || 'No GTIN')
		return
	} 
	else if(typeof kraftJson != 'undefined' && (kraftJson.UserRecipe) && (kraftJson.UserRecipe.UserRecipeID)) {
		console.log('addProduct:',kraftJson.UserRecipe.UserRecipeID)
	}
	else
	{
		return
	}

	// insert product master
	workbook.insertRows('productmaster',kraftJson,KraftUserRecipesSchema);
	
	KraftUserRecipesCleanup.cleanup(userrecipeJSONObj, config, function(cleanProduct){
		result.push(cleanProduct)
	});

	// insert product master properties
	let props = []
	// reviews
	for(var i=0 ; i<reviews.length && reviews.length == 2 ; i++)
	{
		//console.log(reviews[i].name +' == '+ reviews[i].value);
		//props.push(reviews[i]);
		userrecipeJSONObj.__properties["averagerating"] = reviews[0].value + "";
		userrecipeJSONObj.__properties["numberofratings"] = reviews[1].value + "";
	}
	for(var propName in userrecipeJSONObj.__properties){

		var rec_id = kraftJson.UserRecipe.UserRecipeID+"";
		while (rec_id.length < 8) rec_id = "0" + rec_id;

		props.push({
			productid:rec_id,
			name:propName,
			value:userrecipeJSONObj.__properties[propName]
		})
	}

	workbook.insertRows('productmasterproperties',props,KraftUserRecipesSchema);

	workbook.insertRows('skumaster',kraftJson,KraftUserRecipesSchema);

}

function addFacets(userrecipeJSONObj)
{
	// if (clearFacet) {
	// 	for (var fct in facets) {
	// 		if (facets[fct].fieldtype == 2) {
	// 			facets[fct].value = [];
	// 		}
	// 	}
	// }



	if(userrecipeJSONObj.averageRating || userrecipeJSONObj.averageRating >= 0)
	{
		facets.averagerating.value.push(userrecipeJSONObj.averageRating);
	}

	if(userrecipeJSONObj.numberOfRating || userrecipeJSONObj.numberOfRating >= 0)
	{
		facets.numberofratings.value.push(userrecipeJSONObj.numberOfRating);
	}

	if((userrecipeJSONObj.kraft) && (userrecipeJSONObj.kraft.UserRecipe))
	{

		if((userrecipeJSONObj.kraft.UserRecipe.CreateByUser))
		{
			facets.author.value.push(userrecipeJSONObj.kraft.UserRecipe.CreateByUser);
		}

		if(userrecipeJSONObj.kraft.UserRecipe.ModerationComment != null && (userrecipeJSONObj.kraft.UserRecipe.ModerationComment))
		{
			facets.moderationcomment.value.push(userrecipeJSONObj.kraft.UserRecipe.ModerationComment);
			
		}

		if((userrecipeJSONObj.kraft.UserRecipe.Notes))
		{
			facets.notes.value.push(userrecipeJSONObj.kraft.UserRecipe.Notes);
		}


		if((userrecipeJSONObj.kraft.UserRecipe.PrepTime))
		{
			facets.preparationtime.value.push(userrecipeJSONObj.kraft.UserRecipe.PrepTime);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.RecipeName))
		{
			facets.name.value.push(userrecipeJSONObj.kraft.UserRecipe.RecipeName);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.SEOPageName))
		{
			facets.seoname.value=userrecipeJSONObj.kraft.UserRecipe.SEOPageName;
		}

		if((userrecipeJSONObj.kraft.UserRecipe.RecipeImage))
		{
			facets.image.value=userrecipeJSONObj.kraft.UserRecipe.RecipeImage;
		}

		if((userrecipeJSONObj.kraft.UserRecipe.RecipeImage))
		{
			facets.hasimage.value=true;
		}
		else
		{
			facets.hasimage.value=false;
		}

		if((userrecipeJSONObj.kraft.UserRecipe.ReadyIn))
		{
			facets.totaltime.value.push(userrecipeJSONObj.kraft.UserRecipe.ReadyIn);
		}


		if((userrecipeJSONObj.kraft.UserRecipe.DateCreated))
		{
			var createdDatetime = userrecipeJSONObj.kraft.UserRecipe.DateCreated;
			var pattern = /Date\W(.*)-.*/;
			var match = pattern.exec(createdDatetime);
			var text = match[1];
			facets.createddatetime.value.push(text)
		}

		if((userrecipeJSONObj.kraft.UserRecipe.DateUpdated))
		{
			var updatedDateTime = userrecipeJSONObj.kraft.UserRecipe.DateUpdated;
			var pattern = /Date\W(.*)-.*/;
			var match = pattern.exec(createdDatetime);
			var text = match[1];
			facets.updateddatetime.value.push(text)
		}

		if((userrecipeJSONObj.kraft.UserRecipe.Makes))
		{
			facets.makes.value.push(userrecipeJSONObj.kraft.UserRecipe.Makes);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.Preparation))
		{
			var preparationsArr = [];
			
			var preparationsarr =  (userrecipeJSONObj.kraft.UserRecipe.Preparation).split('\r\n');
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
			facets.preparationsteps.value.push(preparationsArr.toString);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.Serves >=0))
		{
			facets.serves.value.push(userrecipeJSONObj.kraft.UserRecipe.Serves);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.UpdatedByUser))
		{
			facets.updatedbyuser.value.push(userrecipeJSONObj.kraft.UserRecipe.UpdatedByUser);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.UserNBR))
		{
			facets.usernbr.value.push(userrecipeJSONObj.kraft.UserRecipe.UserNBR);
		}

		if((userrecipeJSONObj.kraft.UserRecipe.UserRecipeID))
		{
			var recipeid = userrecipeJSONObj.kraft.UserRecipe.UserRecipeID;
			facets.recipeid.value.push(recipeid);
		}
	}
		


	var wellioJsonObj = JSON.parse(userrecipeJSONObj.wellio);


	if((wellioJsonObj.header))
	{
		if((wellioJsonObj.header.derived_labels))
		{
			if((wellioJsonObj.header.derived_labels).length > 0)
			{
				facets.dishdetails_text.value.push(wellioJsonObj.header.derived_labels);
			}
		
		}


		if((wellioJsonObj.header.diet_tags))
		{
			if((wellioJsonObj.header.diet_tags).length > 0)
			{
				facets.diet.value.push(wellioJsonObj.header.diet_tags);
			}
			
		}

		var tags = [];
		if((wellioJsonObj.header.derived_labels))
		{
			if((wellioJsonObj.header.derived_labels).length > 0) 
			{
				var derived_labels = wellioJsonObj.header.derived_labels;
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
				facets.tags.value.push(tags);
			}
			
		}
		if((wellioJsonObj.header.diet_tags))
		{
			if((wellioJsonObj.header.diet_tags).length > 0) 
			{
				var diet_tags = wellioJsonObj.header.diet_tags;
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
				facets.tags.value.push(tags);
			}
		}
		
		
		if((wellioJsonObj.header.nutrition_summary.cals_per_serving))
		{
			facets.nutritioncalsperserving.value.push(wellioJsonObj.header.nutrition_summary.cals_per_serving);
		}

		if((wellioJsonObj.header.nutrition_summary.carbs_per_serving))
		{
			facets.nutritioncarbsperserving.value.push(wellioJsonObj.header.nutrition_summary.carbs_per_serving);
		}

		if((wellioJsonObj.header.nutrition_summary.confidence))
		{
			facets.nutritionconfidence.value.push(wellioJsonObj.header.nutrition_summary.confidence);
		}

		if((wellioJsonObj.header.nutrition_summary.fat_per_serving))
		{
			facets.nutritionfatperserving.value.push(wellioJsonObj.header.nutrition_summary.fat_per_serving);
		}

		if((wellioJsonObj.header.nutrition_summary.nrf_index))
		{
			facets.nutritionnrfindex.value.push(wellioJsonObj.header.nutrition_summary.nrf_index);
		}

		if((wellioJsonObj.header.nutrition_summary.protein_per_serving))
		{
			facets.nutritionproteinperserving.value.push(wellioJsonObj.header.nutrition_summary.protein_per_serving);
		}

		if((wellioJsonObj.header.price_per_serving))
		{
			facets.priceperserving.value.push(wellioJsonObj.header.price_per_serving);
		}
	}
	
	if((wellioJsonObj.nutrition))
	{
		if((wellioJsonObj.nutrition.derived_nutrition))
		{
			var derivednutrition =[];
			var display ="";
			var nutritionitemid = "";
			var nutritionitemname = "";
			var unit = "";
			var quantity = "";
			derivednutrition =  wellioJsonObj.nutrition.derived_nutrition;
			if(derivednutrition.length > 0)
			{
				for(var i = 0;i<derivednutrition.length;i++)
				{
					//console.log("derivednutrition : " + JSON.stringify(derivednutrition));
					var derivednutritionObj = derivednutrition[i];
					if((derivednutritionObj.display != null))
					{
						display = display + derivednutritionObj.display + ",";
					}
					if((derivednutritionObj.details != null))
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

						facets.nutritiondisplay.value.push(display);
						facets.nutritionitemid.value.push(nutritionitemid);
						facets.nutritionitemname.value.push(nutritionitemname);
						facets.nutritionunit.value.push(unit);
						facets.nutritionaggvalue.value.push(quantity);
					}
				}
			}
			
		}
		
	}


	if((wellioJsonObj.ingredients) && (wellioJsonObj.ingredients.alternatives) && ((wellioJsonObj.ingredients.alternatives).length > 0))
	{
		var ingredients = wellioJsonObj.ingredients.length;
		facets.numberofingredients.value.push(ingredients);
		var alternatives = wellioJsonObj.ingredients.alternatives;
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
				facets.tags.value.push(tags);
			}
			
		}
	}


	if((wellioJsonObj.ingredients) && (wellioJsonObj.ingredients).length > 0)
	{
		var ingredientsArr = wellioJsonObj.ingredients;
		var entity = "";
		var amount = "";
		var uom = "";
		for(var i= 0;i<(ingredientsArr).length;i++)
		{
			var ingredientsJSON = ingredientsArr[i]
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
		} 
		facets.ingredientname.value.push(entity.slice(0, -1));
		facets.ingredients_quantitynum.value.push(amount.slice(0, -1));
		facets.ingredients_quantityunit.value.push(uom.slice(0, -1));
	}

	return facets;
}

function addFacet(facets, languageId, workbook, cbk) {
	facets = facets || {};

	for (var facet in facets) {
		workbook.insertRows('facetmaster', { 
			name 			: 	facet,
			fieldtype		: 	facets[facet].fieldtype,
			validationtype 	: 	facets[facet].validationtype,
			isSearchable 	: 	facets[facet].searchable,
			isFilterable 	: 	facets[facet].filterable,
			isSortable 		: 	facets[facet].sortable,
			languageId 		: 	languageId
		}, KraftUserRecipesSchema);

		if (facets[facet].value && facets[facet].value.length) {
			
			if (facets[facet].fieldtype == 2) {
				console.log('Facet--- '+facet)
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
			}), KraftUserRecipesSchema);
		}
	}

	cbk();
}

module.exports = {
	addProduct:addProduct,
	addFacets:addFacets,
	addFacet:addFacet
}