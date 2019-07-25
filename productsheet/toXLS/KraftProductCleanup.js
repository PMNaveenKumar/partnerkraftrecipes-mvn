var nutritionitems = {
	name 	: 	[],
	quantity: 	[],
	unit 	: 	[]
};

var tags = {
	name 	: 	[]
};

var keywords = {
	name 	: 	[]
};

function hasContent(val){
	return !(val===null || val==='')
	return val!==null && String(val).toUpperCase()!=='NULL' && val!=='' && String(val).replace(/\s/g,'')!==''
}

function fixPercent(context){
	let n = parseInt(String(context).replace(/[^0-9\.\-]/g,'') || 0)
	if(isNaN(n)) n = 0
    return n
}

function fixMass(context) {
	if(!context) context = ''
	str = String(context).toLowerCase()
	.replace(/ gr/i,'g')
	.replace(/ gm/i,'g')
	.replace(/ mg/i,'mg')
	.replace(/ g/i,'g')
	return str
}

function fixInteger(context) {
	return parseInt(String(context).replace(/[^0-9\.\-]/g,'') || 0)
}

function fixIngredients(context) {
	var str = context ? String(context) : ''
	return str.replace(/^ingredients:/gi,'').replace(/\n/g,' ').replace(/^\s+|\s+$/g,'')
}

function intValue(context){
	let n = parseInt(String(context).replace(/[^0-9\.\-]/g,'') || 0)
	if(isNaN(n)) n = 0
    return n
}

function productIsOfBrand(product,brandid){
	let brands = product.ProductBrands || []
	let i = brands.length
	while(i--){
		if(String(brands[i].BrandID)===String(brandid)){
			return true
		}
	}
	return false
}

function pushNutrition(name, quantity) {
		nutritionitems.name.push(name);
		nutritionitems.quantity.push(quantity.split(' ')[0]);
		nutritionitems.unit.push(quantity.split(' ')[1]? quantity.split(' ')[1].toLowerCase(): '%DV');
}

function pushNutritionV2(name, quantity) {
	nutritionitems.name.push(name.replace('F_NUTRSRV1_','').replace('_','').toLowerCase());
	nutritionitems.quantity.push(quantity.split(' ')[0]);
	nutritionitems.unit.push(quantity.split(' ')[1]? quantity.split(' ')[1].toLowerCase(): '%DV');
}
function isNodeExists(nodeObj) {
	return (typeof nodeObj != "undefined");
}

function splitStringBySymbolAndTrim(stringValue, symbolKey)
{
	var retArr = [];
	tempArr = stringValue.split(symbolKey);
	tempArr.map(function(itm, idx)
	{
		if(itm.trim() != "")
		{
			retArr = checkDuplicate(retArr, itm);
		}
	});
	return retArr;
}

function removeStringItemByCharSize(stringValue, symbolKey, size)
{
	var retArr = [];
	tempArr = stringValue.split(symbolKey);
	tempArr.map(function(itm, idx)
	{
		if(itm.trim() != "" && itm.length <= size)
		{
			retArr = checkDuplicate(retArr, itm);
		}
	});
	return retArr;
}

function checkDuplicate(arrObj, inputStringValue)
{
	inputStringValue = inputStringValue.trim();
	if(arrObj.indexOf(inputStringValue) == -1 && inputStringValue != "")
		arrObj.push(inputStringValue);
	return arrObj;
}

function cleanup(config, entry, cb){
	nutritionitems = {
		name 	: 	[],
		quantity: 	[],
		unit 	: 	[]
	};
	
	tags = {
		name 	: 	[]
	};
	
	keywords = {
		name 	: 	[]
	};

	var result = {}
	result.hasimage = false;
	if(isNodeExists(entry['salsify:id']))
	{
		entry.ID = entry['salsify:id'];
		result.id = entry['salsify:id'];
		result.kraftid = entry['salsify:id'];
	}

	if(isNodeExists(entry.GTIN))
	result.GTIN = entry.GTIN;

	if(isNodeExists(entry['salsify:id']))
	result.ingrediants_productid = entry['salsify:id'];

	if(isNodeExists(entry['salsify:created_at']))
	result.createdon = entry['salsify:created_at'];

	if(isNodeExists(entry['salsify:updated_at']))
	result.updatedon = entry['salsify:updated_at'];

	// new nodes
	if(isNodeExists(entry.F_LEGAL_DESIGNATION))
	{
		if (typeof entry.F_LEGAL_DESIGNATION == "object")
		{
			result.legaldesignation = [];
			(entry.F_LEGAL_DESIGNATION).map(function(itm, idx){
				tags.name = checkDuplicate(tags.name, itm);
			});
			result.legaldesignation = result.legaldesignation.join(',');
		}
		else if(typeof entry.F_LEGAL_DESIGNATION == "string")
		{
			(entry.F_LEGAL_DESIGNATION.split(",")).map(function(itm, idx){
				tags.name = checkDuplicate(tags.name, itm);
			});
			result.legaldesignation = entry.F_LEGAL_DESIGNATION;
		}
	}

	if(isNodeExists(entry.F_KRAFT_ITEM_CODE))
	result.itemcode = entry.F_KRAFT_ITEM_CODE;

	if(isNodeExists(entry.P_PRODUCT_MANUFACTURE_NAME))
	result.manufacturename = entry.P_PRODUCT_MANUFACTURE_NAME;

	if(isNodeExists(entry.F_PRODUCT_DESCRIPTION_SHORT))
	result.shortdescription = entry.F_PRODUCT_DESCRIPTION_SHORT;

	if(isNodeExists(entry.F_SUB_BRAND_NAME))
	result.subbrandname = entry.F_SUB_BRAND_NAME;
	
	if(isNodeExists(entry.F_SHELF_QUALITY_STATEMENT))
	result.qualitystatment = entry.F_SHELF_QUALITY_STATEMENT;
	
	if(isNodeExists(entry.F_STORAGE_INSTRUCTIONS))
	result.storageinstructions = entry.F_STORAGE_INSTRUCTIONS;

	if(isNodeExists(entry['Temperature Classification']))
	result.temperatureclassification = entry['Temperature Classification'];

	if(isNodeExists(entry.P_ECOMM_DIVISION))
	result.ecommdivision = entry.P_ECOMM_DIVISION;

	if(isNodeExists(entry.F_IS_PACKAGING_MARKED_RETURNABLE))
	result.markedreturnable = entry.F_IS_PACKAGING_MARKED_RETURNABLE;

	if(isNodeExists(entry.F_PREP_AND_COOK_SUGGESTIONS))
	result.prepcooksuggestions = entry.F_PREP_AND_COOK_SUGGESTIONS;

	if(isNodeExists(entry.P_PRODUCT_BENEFIT_2))
	result.prodcutbenefit = entry.P_PRODUCT_BENEFIT_2;

	if(isNodeExists(entry.F_ECOMM_PRODUCT_CATEGORY))
	result.ecommproductcategory = entry.F_ECOMM_PRODUCT_CATEGORY;

	if(isNodeExists(entry.P_SUBCATEGORY))
	result.subcategory = entry.P_SUBCATEGORY;

	if(isNodeExists(entry.F_TOTAL_SHELF_LIFE))
	result.shelflife  = entry.F_TOTAL_SHELF_LIFE;

	if(isNodeExists(entry.productid))
	result.productid = entry.productid;
	//result.longdescription = entry.F_LONG_DESCRIPTION;
	
	if(isNodeExists(entry.productname))
	result.productname  = entry.productname;
	
	//result.primaryimageurlcode = entry['<Primary Image.|Node|.Ecommerce Image URL>'];
	//result.secondaryimageurlcode = entry['<Secondary Image.|Node|.Ecommerce Image URL>'];
	/*if(result.secondaryimageurlcode && result.secondaryimageurlcode.length > 0)
	{
		result.secondaryimageurlcode = result.secondaryimageurlcode.toString();
	}*/
	//result.primaryimageassetreferenceid = entry['Primary Image Asset Reference ID'];
	//result.secondaryimageassetreferenceid = entry['Secondary Image Asset Reference ID'];

	//console.log(entry);

	// brand
	/*if(entry.ProductBrands && entry.ProductBrands.length>0){
		result.brandid = entry.ProductBrands[0].BrandID
		result.brand = entry.ProductBrands[0].Name
	}*/
	if(entry.P_ECOMM_BRAND)
	{
		result.brand = entry.P_ECOMM_BRAND;
	}

	// name
	if(entry.P_ECOMMERCE_PRODUCT_NAME_R1 && entry.P_ECOMMERCE_PRODUCT_NAME_R1.length > 0){
		result.name = entry.P_ECOMMERCE_PRODUCT_NAME_R1;
	}

	if(typeof result.name == 'undefined' && entry.P_ECOMMERCE_PRODUCT_NAME_R5 && entry.P_ECOMMERCE_PRODUCT_NAME_R5.length > 0)
	{
		result.name = entry.P_ECOMMERCE_PRODUCT_NAME_R5;
	}
	//semanticId
	if(typeof result.name != 'undefined' && entry['salsify:id'] && entry['salsify:id'].length > 0 ){
		result.semanticId = "/product/" + entry['salsify:id'] + "/" +  result.name.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
	}
	/*if(productIsOfBrand(entry,130)){
		// 130 is Heinz brand
		// they have a special product name property "P_ECOMMERCE_PRODUCT_NAME_R5" - boo
		result.name = entry.P_ECOMMERCE_PRODUCT_NAME_R5 || entry.eCommProductName
	} else {
		result.name = entry.eCommProductName
	}*/

	// description THIS WAS CHANGED ON 2018-02-27 - Recent Kraft update switch the priority of the fields
	// result.description = entry.eCommProductFeature || entry.GeneralMktDescription || ''

	if(entry.F_LONG_DESCRIPTION && entry.F_LONG_DESCRIPTION.length > 0)
	{
		result.description = entry.F_LONG_DESCRIPTION;
	}

	if(entry.F_DECLARED_NET_CONTENT_UOM_CODE && entry.F_DECLARED_NET_CONTENT_UOM_CODE.length > 0)
	{
		result.declarednetcontentuomcode = entry.F_DECLARED_NET_CONTENT_UOM_CODE;
	}
	if((entry.NUTRIENT_HEALTH_CLAIMS && entry.NUTRIENT_HEALTH_CLAIMS.length > 0) || (entry.F_NUTRIENT_HEALTH_CLAIMS && entry.F_NUTRIENT_HEALTH_CLAIMS.length > 0)) 
	{
		result.healthclaims = entry.NUTRIENT_HEALTH_CLAIMS || entry.F_NUTRIENT_HEALTH_CLAIMS;
	}
	if(entry.F_ECOMMERCE_PRODUCT_FEATURE && entry.F_ECOMMERCE_PRODUCT_FEATURE.length > 0)
	{
		result.ecommproductfeature = entry.F_ECOMMERCE_PRODUCT_FEATURE;
	}
	/*if(hasContent(entry.ProductVariants))
	{
		if (entry.ProductVariants && entry.ProductVariants.length) {*/
			/*var newVariantObj = entry.ProductVariants.filter(function(variantObj) {
				return variantObj.DefaultProductGTIN == entry.GTIN;
			});

			if(newVariantObj.length)
			{
				result.productvariants = JSON.stringify(newVariantObj);
			}*/
			/*result.productvariants = JSON.stringify(entry.ProductVariants);
		}
	}*/

	// alt_image
	// KRAFTREC-3517  --- 
	//let allowImageTypes = ["A1C1", "A1L1", "A1R1", "A1N1", "A2N1", "A3N1", "A7N1", "A8N1", "A9N1"]//, "BC", "IN", "NF", "Z1C1", "PV", "SHELFMAN"]
	//let allowImageTypes = ["A1C1", "A1L1", "A1R1", "A1N1", "A2N1", "A3N1", "A7N1", "A8N1", "A9N1", "BC", "IN", "NF", "Z1C1", "PV", "SHELFMAN"]
	//let primaryImageType = "A1C1"

	/*primaryId = '';
	secondaryIds = [];

	if(entry['<Primary Image.|Node|.Ecommerce Image URL>'])
	{
		primaryId = entry['<Primary Image.|Node|.Ecommerce Image URL>']; 
	}
	if(entry['<Secondary Image.|Node|.Ecommerce Image URL>'])
	{
		secondaryIds = entry['<Secondary Image.|Node|.Ecommerce Image URL>'];
	}

	if(entry['salsify:digital_assets']){
		result.alt_image = entry['salsify:digital_assets'].map(function(asset){
			if(asset['salsify:source_url'] ){
				let SourceURL = asset['salsify:source_url'] ;
				if(primaryId == asset['salsify:id'])
				{
					result.image = SourceURL;
					result.hasimage = true;
				}
				if (secondaryIds.indexOf(asset['salsify:id']) > -1)	
				{
					return SourceURL;
				}
			}
		}).filter(e=>{return e});

		result.altimagesalsifyurl = entry['salsify:digital_assets'].map(function(asset){
			if(asset['salsify:url'] ){
				let SourceURL = asset['salsify:url'] ;
				if(primaryId == asset['salsify:id'])
				{
					result.imagesalsifyurl = SourceURL;
				}
				if (secondaryIds.indexOf(asset['salsify:id']) > -1)	
				{
					return SourceURL;
				}
			}
		}).filter(e=>{return e})
	}

	// Image: primary image wasn't found - choose from alt_image
	if(!result.image){
		if(result.alt_image && result.alt_image.length){
			result.image = result.alt_image[0]
		}
	}

	// change array of altimages to string with comma separated.  KRAFTREC-3428
	result.alt_image = secondaryIds;
	if(secondaryIds.length > 0)
	{
		//result.alt_image = JSON.stringify(secondaryIds);
		result.alt_image = secondaryIds.toString();
	}
	else
	{
		delete result.alt_image;
	}

	// salsifyaltimageurls
	if(result.altimagesalsifyurl && result.altimagesalsifyurl.length > 0)
	{
		result.altimagesalsifyurl = result.altimagesalsifyurl.toString();    
	}else
	{
		delete result.altimagesalsifyurl;
	}*/



	// nutrition_allergens
	/*if(entry.ProductAllergens && entry.ProductAllergens.length>0){
		result.nutritionallergens = entry.ProductAllergens.map(function(a){
			return a.Allergen
		}).join(', ')
	}*/
	
	

	if(entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT && entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT.length > 0){
		result.nutritionallergens = entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT
		result.nutritioncontains = entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT
	}

	// SizeVariants
	//result.sizevariants = JSON.stringify(entry.SizeVariants || [])

	/*if(entry.F_VARIANT_NAME && entry.F_VARIANT_NAME.length > 0){
		result.variant = entry.F_VARIANT_NAME
	}*/

	// nutrition_servings
	if(entry.F_SERVINGS_OF_TRADE_ITEM_UNIT && entry.F_SERVINGS_OF_TRADE_ITEM_UNIT.length > 0){
		result.nutritionservings = entry.F_SERVINGS_OF_TRADE_ITEM_UNIT
	}
	// nutrition_servingsize
	if(entry.F_INGREDIENT_INFORMATION_HH_SERVING_SIZE && entry.F_INGREDIENT_INFORMATION_HH_SERVING_SIZE.length > 0){
		result.nutritionservingsize = entry.F_INGREDIENT_INFORMATION_HH_SERVING_SIZE
	}

	// nutrition_ingredients
	if(entry.F_INGREDIENT_INFORMATION_STATEMENT && entry.F_INGREDIENT_INFORMATION_STATEMENT.length > 0)
	{
		result.nutritioningredients = fixIngredients(entry.F_INGREDIENT_INFORMATION_STATEMENT)
	}

	//Static naming
	/*if(entry.F_NUTRSRV1_CA_AMT && hasContent(entry.F_NUTRSRV1_CA_AMT)){
		//result.nutritionvitamincdv = fixPercent(entry.NutrSrv1VitCRDA)
		pushNutrition('Vitamin C', entry.F_NUTRSRV1_CA_AMT);
	} else
	{
		//pushNutrition('Vitamin C', '0');
	}*/

	//Dynamic naming
	/*for (var keyName in entry){
		if(keyName.search(/F_NUTRSRV1_/i) == 0)
			pushNutritionV2(keyName, entry[keyName]);
	}*/

	//Image
	var assetsDataObjsArr = [];
	var assetsDataObj = {};
	var arr = ["Marketing View - Color Front","Marketing View - Color Left","Marketing View - Color Right","Package View Bundle - A7N1","Ingredient Statement","Nutrition Fact Panel","Barcode","Package View Bundle - A2N1","Package View Bundle - A8N1","Package View Bundle - A3N1","Package View Bundle - A9N1"];
	arr.forEach(function(value, index) {
		var entryKey = value;
		if(typeof entry[entryKey] != "undefined")
		{
			if(typeof entry['salsify:id'] != "undefined" && typeof entry['salsify:digital_assets'] != "undefined" && entry['salsify:digital_assets'].length > 0)
			{
				var selecteNode = "", selecteNodeName = "", selecteNodeId = "";
				selecteNodeName = entryKey;
				if(typeof entry[entryKey] == "object")
				{
					arr.forEach(function(value, index) {
						if(typeof entry[selecteNodeName] != "undefined" && entry[selecteNodeName].length > 0)
						{
							selecteNode = entry[selecteNodeName];
							selecteNodeId = selecteNode[0];
						}
					});
				}else if(typeof entry[entryKey] == "string")
				{
					selecteNodeId = entry[selecteNodeName];
				}
				var assetsNode = entry['salsify:digital_assets'];
				for (var assetKey in assetsNode)
				{
					var assetObj = assetsNode[assetKey];
					if(assetObj['salsify:id'] == selecteNodeId)
					{
						var objName = selecteNodeName.toLowerCase().replace(/\s/g,'').replace(/-/g,'');
						var objVal = assetObj['salsify:url'];
						var objDamVal = assetObj['salsify:source_url'];
						assetsDataObj[objName] = {};
						var assetObj = assetsDataObj[objName];
						assetObj['asseturl'] = objVal;
						assetObj['damurl'] = objDamVal;
						//assetsDataObjsArr.push({name : objName , value : objVal, damurl: objDamVal});
					}
				}
			}
		}
	});
	
	if(typeof assetsDataObj.marketingviewcolorfront != "undefined")
	{
		if(typeof assetsDataObj.marketingviewcolorfront.asseturl != "undefined")
		{
			result.image = assetsDataObj.marketingviewcolorfront.asseturl;
			result.hasimage = true;
			delete assetsDataObj.marketingviewcolorfront.asseturl;
		}
		if(typeof assetsDataObj.marketingviewcolorfront.damurl != "undefined")
		{
			result.imagedamurl = assetsDataObj.marketingviewcolorfront.damurl;
			delete assetsDataObj.marketingviewcolorfront.damurl;
		}
		delete assetsDataObj.marketingviewcolorfront
	}
	
	if(JSON.stringify(assetsDataObj) != "{}")
		result.assets = JSON.stringify(assetsDataObj);

	if(entry.F_NUTRSRV1_ENERPF_AMT && hasContent(entry.F_NUTRSRV1_ENERPF_AMT)){
		pushNutrition('Caloriesfromfat Amt', entry.F_NUTRSRV1_ENERPF_AMT);
	}
	
	if(entry.F_NUTRSRV1_VITAA_RDA && hasContent(entry.F_NUTRSRV1_VITAA_RDA)){
		pushNutrition('Vitamin A', entry.F_NUTRSRV1_VITAA_RDA);
	}

	if(entry.F_NUTRSRV1_VITC_RDA && hasContent(entry.F_NUTRSRV1_VITC_RDA)){
		pushNutrition('Vitamin C', entry.F_NUTRSRV1_VITC_RDA);
	}

	if(entry.F_NUTRSRV1_VITC_AMT && hasContent(entry.F_NUTRSRV1_VITC_AMT)){
		pushNutrition('Vitamin C Amt', entry.F_NUTRSRV1_VITC_AMT);
	}

	if(entry.F_NUTRSRV1_CA_RDA && hasContent(entry.F_NUTRSRV1_CA_RDA)){
		pushNutrition('Calcium', entry.F_NUTRSRV1_CA_RDA);
	}

	if(entry['F_NUTRSRV1_CA_AMT'] && hasContent(entry['F_NUTRSRV1_CA_AMT'])){
		pushNutrition('Calcium Amt', entry['F_NUTRSRV1_CA_AMT']);
	}

	if(entry.F_NUTRSRV1_CAL_AMT && hasContent(entry.F_NUTRSRV1_CAL_AMT)){
		pushNutrition('Nutrition Calories Amt', entry.F_NUTRSRV1_CAL_AMT);
	}
	
	if(entry.F_NUTRSRV1_FE_RDA && hasContent(entry.F_NUTRSRV1_FE_RDA)){
		pushNutrition('Iron', entry.F_NUTRSRV1_FE_RDA);
	}

	if(entry['F_NUTRSRV1_FE_AMT'] && hasContent(entry['F_NUTRSRV1_FE_AMT'])){
		pushNutrition('Iron Amt', entry['F_NUTRSRV1_FE_AMT']);
	}

	if(entry['F_NUTRSRV1_VITB6-_RDA'] && hasContent(entry['F_NUTRSRV1_VITB6-_RDA'])){
		pushNutrition('Vitamin B6', entry['F_NUTRSRV1_VITB6-_RDA']);
	}

	if(entry.F_NUTRSRV1_VITD_RDA && hasContent(entry.F_NUTRSRV1_VITD_RDA)){
		pushNutrition('Vitamin D', entry.F_NUTRSRV1_VITD_RDA);
	}
	
	if(entry.F_NUTRSRV1_VITD_AMT && hasContent(entry.F_NUTRSRV1_VITD_AMT)){
		pushNutrition('Vitamin D Amt', entry.F_NUTRSRV1_VITD_AMT);
	}

	if(entry.F_NUTRSRV1_VITE_RDA && hasContent(entry.F_NUTRSRV1_VITE_RDA)){
		pushNutrition('Vitamin E', entry.F_NUTRSRV1_VITE_RDA);
	}
	
	if(entry.F_NUTRSRV1_VITE_AMT && hasContent(entry.F_NUTRSRV1_VITE_AMT)){
		pushNutrition('Vitamin E Amt', entry.F_NUTRSRV1_VITE_AMT);
	}
	
	if(entry.F_NUTRSRV1_VITK_RDA && hasContent(entry.F_NUTRSRV1_VITK_RDA)){
		pushNutrition('Vitamin K', entry.F_NUTRSRV1_VITK_RDA);
	}
	
	if(entry.F_NUTRSRV1_VITK_AMT && hasContent(entry.F_NUTRSRV1_VITK_AMT)){
		pushNutrition('Vitamin K Amt', entry.F_NUTRSRV1_VITK_AMT);
	}
	
	if(entry.F_NUTRSRV1_THIA_RDA && hasContent(entry.F_NUTRSRV1_THIA_RDA)){
		pushNutrition('Thiamin', entry.F_NUTRSRV1_THIA_RDA);
	}
	
	if(entry.F_NUTRSRV1_THAI_AMT && hasContent(entry.F_NUTRSRV1_THAI_AMT)){
		pushNutrition('Thiamin Amt', entry.F_NUTRSRV1_THAI_AMT);
	}
	
	if(entry.F_NUTRSRV1_RIBF_RDA && hasContent(entry.F_NUTRSRV1_RIBF_RDA)){
		pushNutrition('Riboflavin', entry.F_NUTRSRV1_RIBF_RDA);
	}
	
	if(entry.F_NUTRSRV1_RIBF_AMT && hasContent(entry.F_NUTRSRV1_RIBF_AMT)){
		pushNutrition('Riboflavin Amt', entry.F_NUTRSRV1_RIBF_AMT);
	}

	if(entry.F_NUTRSRV1_NIA_RDA && hasContent(entry.F_NUTRSRV1_NIA_RDA)){
		pushNutrition('Niacin', entry.F_NUTRSRV1_NIA_RDA);
	}
	
	if(entry.F_NUTRSRV1_NIA_AMT && hasContent(entry.F_NUTRSRV1_NIA_AMT)){
		pushNutrition('Niacin Amt', entry.F_NUTRSRV1_NIA_AMT);
	}

	if(entry.F_NUTRSRV1_P_RDA && hasContent(entry.F_NUTRSRV1_P_RDA)){
		pushNutrition('Phosphorus', entry.F_NUTRSRV1_P_RDA);
	}

	if(entry.F_NUTRSRV1_MG_RDA && hasContent(entry.F_NUTRSRV1_MG_RDA)){
		pushNutrition('Magnesium', entry.F_NUTRSRV1_MG_RDA);
	}

	if(entry.F_NUTRSRV1_MN_RDA && hasContent(entry.F_NUTRSRV1_MN_RDA)){
		pushNutrition('Manganese', entry.F_NUTRSRV1_MN_RDA);
	}

	if(entry.F_NUTRSRV1_FAT_AMT && hasContent(entry.F_NUTRSRV1_FAT_AMT)){
		pushNutrition('Fat Amt', entry.F_NUTRSRV1_FAT_AMT);
	}

	if(entry.F_NUTRSRV1_FAT_RDA && hasContent(entry.F_NUTRSRV1_FAT_RDA)){
		pushNutrition('Fat', entry.F_NUTRSRV1_FAT_RDA);
	}

	if(entry.F_NUTRSRV1_FASAT_AMT && hasContent(entry.F_NUTRSRV1_FASAT_AMT)){
		pushNutrition('Saturated Fat Amt', entry.F_NUTRSRV1_FASAT_AMT);
	}

	if(entry.F_NUTRSRV1_FASAT_RDA && hasContent(entry.F_NUTRSRV1_FASAT_RDA)){
		pushNutrition('Saturated Fat', entry.F_NUTRSRV1_FASAT_RDA);
	}

	if(entry.F_NUTRSRV1_FATRN_AMT && hasContent(entry.F_NUTRSRV1_FATRN_AMT)){
		pushNutrition('Trans Fat', entry.F_NUTRSRV1_FATRN_AMT);
	}

	if(entry.F_NUTRSRV1_FAPU_AMT && hasContent(entry.F_NUTRSRV1_FAPU_AMT)){
		pushNutrition('Polyunsaturated Fat Amt', entry.F_NUTRSRV1_FAPU_AMT);
	}

	if(entry.F_NUTRSRV1_FAMS_AMT && hasContent(entry.F_NUTRSRV1_FAMS_AMT)){
		pushNutrition('Monounsaturated Fat Amt', entry.F_NUTRSRV1_FAMS_AMT);
	}

	if(entry['F_NUTRSRV1_CHOL-_AMT'] && hasContent(entry['F_NUTRSRV1_CHOL-_AMT'])){
		pushNutrition('Cholesterol Amt', entry['F_NUTRSRV1_CHOL-_AMT']);
	}

	if(entry['F_NUTRSRV1_CHOL-_RDA'] && hasContent(entry['F_NUTRSRV1_CHOL-_RDA'])){
		pushNutrition('Cholesterol', entry['F_NUTRSRV1_CHOL-_RDA']);
	}

	if(entry.F_NUTRSRV1_NA_AMT && hasContent(entry.F_NUTRSRV1_NA_AMT)){
		pushNutrition('Sodium Amt', entry.F_NUTRSRV1_NA_AMT);
	}

	if(entry.F_NUTRSRV1_NA_RDA && hasContent(entry.F_NUTRSRV1_NA_RDA)){
		pushNutrition('Sodium', entry.F_NUTRSRV1_NA_RDA);
	}

	if(entry.F_NUTRSRV1_K_AMT && hasContent(entry.F_NUTRSRV1_K_AMT)){
		pushNutrition('Potassium Amt', entry.F_NUTRSRV1_K_AMT);
	}

	if(entry.F_NUTRSRV1_K_RDA && hasContent(entry.F_NUTRSRV1_K_RDA)){
		pushNutrition('Potassium', entry.F_NUTRSRV1_K_RDA);
	}
	
	if(entry['F_NUTRSRV1_CHO-_AMT'] && hasContent(entry['F_NUTRSRV1_CHO-_AMT'])){
		pushNutrition('Carbohydrates Amt', entry['F_NUTRSRV1_CHO-_AMT']);
	}
	
	if(entry['F_NUTRSRV1_CHO-_RDA'] && hasContent(entry['F_NUTRSRV1_CHO-_RDA'])){
		pushNutrition('Carbohydrates', entry['F_NUTRSRV1_CHO-_RDA']);
	}

	if(entry.F_NUTRSRV1_FIBTSW_AMT && hasContent(entry.F_NUTRSRV1_FIBTSW_AMT)){
		pushNutrition('Fiber Amt', entry.F_NUTRSRV1_FIBTSW_AMT);
	}

	if(entry.F_NUTRSRV1_FIBTSW_RDA && hasContent(entry.F_NUTRSRV1_FIBTSW_RDA)){
		pushNutrition('Fiber', entry.F_NUTRSRV1_FIBTSW_RDA);
	}

	if(entry.F_NUTRSRV1_SUGAR_AMT && hasContent(entry.F_NUTRSRV1_SUGAR_AMT)){
		pushNutrition('Sugars Amt', entry.F_NUTRSRV1_SUGAR_AMT);
	}

	if(entry['F_NUTRSRV1_PRO-_RDA'] && hasContent(entry['F_NUTRSRV1_PRO-_RDA'])){
		pushNutrition('Protein', entry['F_NUTRSRV1_PRO-_RDA']);
	}

	if(entry['F_NUTRSRV1_PRO-_AMT'] && hasContent(entry['F_NUTRSRV1_PRO-_AMT'])){
		pushNutrition('Protein Amt', entry['F_NUTRSRV1_PRO-_AMT']);
	}

	if(entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT && hasContent(entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT)){
		//result.nutritioncontains = entry.F_INGREDIENT_INFO_DOES_CONTAIN_STATEMENT
	}

	// seo friendly name
	if(typeof result.name != 'undefined'){
		result.seoname = result.name
	}

	if(entry.F_GEN_MKT_DESCRIPTION && entry.F_GEN_MKT_DESCRIPTION.length > 0)
	{
		result.marketingdescription = entry.F_GEN_MKT_DESCRIPTION;
	}
	if(typeof(entry['Active Status']) != "undefined")
	{
		result.activestatus = entry['Active Status'];
	}
	if(typeof(entry['Division Catalyst Name']) != "undefined")
	{
		result.divisioncatalystname = entry['Division Catalyst Name'];
	}
	if(typeof(entry['F_CONSUMER_UNIT_SIZE_UOM']) != "undefined")
	{
		result.consumerunitsizeuom = entry['F_CONSUMER_UNIT_SIZE_UOM'];
	}
	if(typeof(entry['F_DIVISION_CATALYST']) != "undefined")
	{
		result.divisioncatalyst = entry['F_DIVISION_CATALYST'];
	}
	if(typeof(entry['F_CONSUMER_UNIT_SIZE']) != "undefined")
	{
		result.consumerunitsize = entry['F_CONSUMER_UNIT_SIZE'];
	}
	if(typeof(entry['P_KEY_SEARCH_TERMS']) != "undefined")
	{
		var keywordArr = splitStringBySymbolAndTrim(entry['P_KEY_SEARCH_TERMS'], ',');
		result.keywords = keywordArr.join(",");
		//result.keywords = keywordArr.join(",");
		(keywordArr).map(function(itm, idx)
		{
			//keywords.name = checkDuplicate(tags.name, itm);
			//tags.name = checkDuplicate(tags.name, itm);
		});
		keywordArr = removeStringItemByCharSize(entry['P_KEY_SEARCH_TERMS'], ',', 50);
		(keywordArr).map(function(itm, idx)
		{
			//keywords.name = checkDuplicate(tags.name, itm);
			tags.name = checkDuplicate(tags.name, itm);
		});
	}
	if(typeof(entry['P_HIDDEN_SEARCH_TERMS']) != "undefined")
	{
		result.phiddensearchterms = entry['P_HIDDEN_SEARCH_TERMS'];
	}
	/*if(typeof(entry['Primary Image Asset Reference ID']) != "undefined")
	{
		result.primaryimageassetreferenceid = entry['Primary Image Asset Reference ID'];
	}
	if(typeof(entry['Secondary Image Asset Reference ID']) != "undefined")
	{
		result.secondaryimageassetreferenceid = entry['Secondary Image Asset Reference ID'];
	}*/

	function typeCheckAndStringify(jsonObj, keyVal)
	{
		var retStr = "";
		if(typeof(jsonObj[keyVal]) != "undefined")
		{
			if(typeof jsonObj[keyVal] == "object"){
				retStr = JSON.stringify(jsonObj[keyVal]);
			}
			else
			{
				retStr = jsonObj[keyVal].toString();
			}
		}
		return retStr;
	}

	var categorylevelmap = "\t";
	var categorylevelmappipe = "||";
	
	if(typeof(entry['CRM - Category LVL 1']) != "undefined")
	{
		var categorylevel1 = typeCheckAndStringify(entry, "CRM - Category LVL 1");
		categorylevelmap = categorylevelmap + categorylevel1 + "\t";
		categorylevelmappipe = categorylevelmappipe + categorylevel1;
	}
	categorylevelmappipe = categorylevelmappipe  + "||";

	if(typeof(entry['CRM - Category LVL2']) != "undefined")
	{
		var categorylevel2 = typeCheckAndStringify(entry, "CRM - Category LVL2");
		categorylevelmap = categorylevelmap + categorylevel2 + "\t";
		categorylevelmappipe = categorylevelmappipe + categorylevel2
	}
	categorylevelmappipe = categorylevelmappipe  + "||";
	
	if(typeof(entry['CRM - Category LVL 3']) != "undefined")
	{
		var categorylevel3 = typeCheckAndStringify(entry, "CRM - Category LVL 3");
		categorylevelmap = categorylevelmap + categorylevel3 + "\t";
		categorylevelmappipe = categorylevelmappipe + categorylevel3;
	}
	categorylevelmappipe = categorylevelmappipe  + "||";
	
	if(typeof(entry['CRM - Category LVL 4']) != "undefined")
	{
		var categorylevel4 = typeCheckAndStringify(entry, "CRM - Category LVL 4");
		categorylevelmap = categorylevelmap + categorylevel4 + "\t";
		categorylevelmappipe = categorylevelmappipe + categorylevel4;
	}
	categorylevelmappipe = categorylevelmappipe  + "||";

	if(result.id  != "undefined")
		config.catMapLog.write("\n" + result.id + categorylevelmap);
	
	//var categorylevelMapBrand = "\t";
	var categorylevelmapbrandpipe = "||";

	if(typeof(entry['Brand Site - Category LVL1']) != "undefined")
	{
		var brandcategory1 = typeCheckAndStringify(entry, "Brand Site - Category LVL1");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory1 + "||";
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";

	if(typeof(entry['Brand Site - Category LVL 1']) != "undefined")
	{
		var brandcategory1 = typeCheckAndStringify(entry, "Brand Site - Category LVL 1");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory1;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";

	if(typeof(entry['Brand Site - Category LVL2']) != "undefined")
	{
		var brandcategory2 = typeCheckAndStringify(entry, "Brand Site - Category LVL2");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory2;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";
	
	if(typeof(entry['Brand Site - Category LVL 2']) != "undefined")
	{
		var brandcategory2 = typeCheckAndStringify(entry, "Brand Site - Category LVL 2");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory2;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";

	if(typeof(entry['Brand Site - Category LVL3']) != "undefined")
	{
		var brandcategory3 = typeCheckAndStringify(entry, "Brand Site - Category LVL3");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory3;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";

	if(typeof(entry['Brand Site - Category LVL 3']) != "undefined")
	{
		var brandcategory3 = typeCheckAndStringify(entry, "Brand Site - Category LVL 3");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory3;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";

	if(typeof(entry['Brand Site - Category LVL4']) != "undefined")
	{
		var brandcategory4 = typeCheckAndStringify(entry, "Brand Site - Category LVL4");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory4;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";

	if(typeof(entry['Brand Site - Category LVL 4']) != "undefined")
	{
		var brandcategory4 = typeCheckAndStringify(entry, "Brand Site - Category LVL 4");
		categorylevelmapbrandpipe = categorylevelmapbrandpipe + brandcategory4;
	}
	categorylevelmapbrandpipe = categorylevelmapbrandpipe + "||";
	

	//logFileMissing.write(util.format(categorylevelMap));
	//if(config.skipcategorymap != 'true')
	result.categorylevelmap = categorylevelmappipe;
	result.categorylevelmapbrand = categorylevelmapbrandpipe;
	
	if(typeof(entry['CRM-Variation-Label-1']) != "undefined")
	{
		result.variationlabel1 = typeCheckAndStringify(entry, "CRM-Variation-Label-1");
	}

	if(typeof(entry['CRM-Variation-Label-2']) != "undefined")
	{
		result.variationlabel2 = typeCheckAndStringify(entry, "CRM-Variation-Label-2");
	}

	if(typeof(entry['CRM-Variation-LVL-1']) != "undefined")
	{
		result.sizevariants = typeCheckAndStringify(entry, "CRM-Variation-LVL-1");
		tags.name = checkDuplicate(tags.name, entry['CRM-Variation-LVL-1']);
	}

	if(typeof(entry['CRM-Variation-LVL-2']) != "undefined")
	{
		result.sizevariants2 = typeCheckAndStringify(entry, "CRM-Variation-LVL-2");
	}

	/*if(typeof(entry['CRM-Variation-LVL-1']) != "undefined" || typeof(entry['CRM-Variation-LVL-2']) != "undefined")
	{
		var isDataSet = false;
		var jsonObj = {};
		if(entry['CRM-Variation-LVL-1'])
		{
			isDataSet = true;
			jsonObj.sizevariants = typeCheckAndStringify(entry, "CRM-Variation-LVL-1");
		}
		if(entry['CRM-Variation-LVL-2'])
		{
			isDataSet = true;
			jsonObj.sizevariants2 = typeCheckAndStringify(entry, "CRM-Variation-LVL-2");
		}
		if(isDataSet)
		{
			result.productvariants = jsonObj.toString();
		}
	}*/

	if(nutritionitems.name.length > 0)
	{
		result.nutritionitemname = nutritionitems.name.join(',');
		result.unit = nutritionitems.unit.join(',');
		result.quantity = nutritionitems.quantity.join(',');
	}

	if(tags.name.length > 0)
	{
		/*result.tagsfull = tags.name.join(',');
		var rettags = [];
		var curSize = 0;
		tags.name.map(function(itm, idx)
		{
			curSize = curSize + itm.length;
			if(itm.length <=50 && itm.trim() != "" && curSize <= 50)
			{
				rettags.push(itm);
			}
		});
		//tags.name = rettags.join(',');
		result.tags = rettags.join(',');*/
		result.tags = tags.name.join(',');
	}
	//console.log(result);

	entry.__properties = result

	if(cb) cb(entry)
	return entry
}

module.exports = {
	cleanup:cleanup
}