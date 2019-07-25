
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


function cleanup(entry, cb){
	var result = {}

	entry.ID = entry.GTIN

	result.id = entry.GTIN

	// brand
	if(entry.ProductBrands && entry.ProductBrands.length>0){
		result.brandid = entry.ProductBrands[0].BrandID
		result.brand = entry.ProductBrands[0].Name
	}

	// name
	if(productIsOfBrand(entry,130)){
		// 130 is Heinz brand
		// they have a special product name property "P_ECOMMERCE_PRODUCT_NAME_R5" - boo
		result.Name = entry.P_ECOMMERCE_PRODUCT_NAME_R5 || entry.eCommProductName || 'No Name'
	} else {
		result.Name = entry.eCommProductName || 'No Name'
	}

	// description THIS WAS CHANGED ON 2018-02-27 - Recent Kraft update switch the priority of the fields
	// result.description = entry.eCommProductFeature || entry.GeneralMktDescription || ''


	result.description = entry.GeneralMktDescription || entry.eCommProductFeature || ''


	// alt_image
	let allowImageTypes = ["A1C1", "A1L1", "A1R1", "A1N1", "A2N1", "A3N1", "A7N1", "A8N1", "A9N1"]//, "BC", "IN", "NF", "Z1C1", "PV", "SHELFMAN"]
	let primaryImageType = "A1C1"

	if(entry.ProductAssets && hasContent(entry.ProductAssets)){
		result.alt_image = entry.ProductAssets.map(function(asset){
			if(hasContent(asset.SourceURL) && asset.SourceURL.indexOf('ht=null&wd=null')===-1){
				let imgType = asset.SourceURL.split('?')[0].split('_').pop().split('.')[0]
				let imageAllowed = allowImageTypes.indexOf(imgType)>-1
				let SourceURL = asset.SourceURL.replace(/^\s+|\s+$/g,'')

				if(imageAllowed){
					if(imgType===primaryImageType){
						result.Image = SourceURL
					}
					return SourceURL
				} else {
					return false
				}
			}
		}).filter(e=>{return e})
	} else {
		result.alt_image = []
	}

	// Image: primary image wasn't found - choose from alt_image
	if(!result.Image){
		if(result.alt_image.length>0){
			result.Image = result.alt_image[0]
		}
	}

	// nutrition_allergens
	if(entry.ProductAllergens && entry.ProductAllergens.length>0){
		result.nutrition_allergens = entry.ProductAllergens.map(function(a){
			return a.Allergen
		}).join(', ')
	}

	// SizeVariants
	result.SizeVariants = JSON.stringify(entry.SizeVariants || [])

	// nutrition_servings
	if(entry.ServingsOfTradeItemUnit){
		result.nutrition_servings = entry.ServingsOfTradeItemUnit
	} else {
		result.nutrition_servings = 1
	}

	// nutrition_servingsize
	if(hasContent(entry.IngredientInfoHHServingSize)){
		result.nutrition_servingsize = entry.IngredientInfoHHServingSize
	} else if(hasContent(entry.NutrSrv1ServSize)){
		// alternate source
		result.nutrition_servingsize = entry.NutrSrv1ServSize
	}

	// nutrition_ingredients
	result.nutrition_ingredients = fixIngredients(entry.IngredientInformationStatement)

	// nutrition_calories
	if(hasContent(entry.NutrSrv1CalAmt)){
		result.nutrition_calories = fixInteger(entry.NutrSrv1CalAmt)
	}

	// nutrition_caloriesfromfat
	if(intValue(entry.NutrSrv1EnerpfAmt)){
		result.nutrition_caloriesfromfat = fixInteger(entry.NutrSrv1EnerpfAmt)
	} else {
		result.nutrition_caloriesfromfat = 0
	}

	// nutrition_vitamina_dv
	if(intValue(entry.F_NUTRSRV1_VITAA_RDA)){
		result.nutrition_vitamina_dv = fixPercent(entry.F_NUTRSRV1_VITAA_RDA)
	} else if(hasContent(entry.NutrSrv1VitARDA)){
		// alternate source
		result.nutrition_vitamina_dv = fixPercent(entry.NutrSrv1VitARDA)
	} else {
		// required vitamin
		result.nutrition_vitamina_dv = 0
	}

	// nutrition_vitaminc_dv
	if(intValue(entry.NutrSrv1VitCRDA)){
		result.nutrition_vitaminc_dv = fixPercent(entry.NutrSrv1VitCRDA)
	} else {
		// required vitamin
		result.nutrition_vitaminc_dv = 0
	}

	// nutrition_calcium_dv
	if(intValue(entry.NutrSrv1CaRDA)){
		result.nutrition_calcium_dv = fixPercent(entry.NutrSrv1CaRDA)
	} else {
		// required vitamin
		result.nutrition_calcium_dv = 0
	}

	// nutrition_iron_dv
	if(intValue(entry.NutrSrv1HaemRDA)){
		result.nutrition_iron_dv = fixPercent(entry.NutrSrv1HaemRDA)
	} else {
		// required vitamin
		result.nutrition_iron_dv = 0
	}

	// nutrition_vitamind_dv
	if(intValue(entry.F_NUTRSRV1_VITD_RDA)){
		result.nutrition_vitamind_dv = fixPercent(entry.F_NUTRSRV1_VITD_RDA)
	}

	// nutrition_vitamine_dv
	if(intValue(entry.F_NUTRSRV1_VITE_RDA)){
		result.nutrition_vitamine_dv = fixPercent(entry.F_NUTRSRV1_VITE_RDA)
	}

	// nutrition_niacin_dv
	if(intValue(entry.F_NUTRSRV1_NIA_RDA)){
		result.nutrition_niacin_dv = fixPercent(entry.F_NUTRSRV1_NIA_RDA)
	}

	// nutrition_phosphorus_dv
	if(intValue(entry.NutrSrv1PRDA)){
		result.nutrition_phosphorus_dv = fixPercent(entry.NutrSrv1PRDA)
	}

	// nutrition_magnesium_dv
	if(intValue(entry.F_NUTRSRV1_MG_RDA)){
		result.nutrition_magnesium_dv = fixPercent(entry.F_NUTRSRV1_MG_RDA)
	}

	// nutrition_manganese_dv
	if(intValue(entry.F_NUTRSRV1_MN_RDA)){
		result.nutrition_manganese_dv = fixPercent(entry.F_NUTRSRV1_MN_RDA)
	}

	// nutrition_fat_amt
	if(hasContent(entry.NutrSrv1FatAmt)){
		result.nutrition_fat_amt = fixMass(entry.NutrSrv1FatAmt)
	}

	// nutrition_fat_dv
	if(hasContent(entry.NutrSrv1FatRDA)){
		result.nutrition_fat_dv = fixPercent(entry.NutrSrv1FatRDA)
	}

	// nutrition_saturatedfat_amt
	if(hasContent(entry.NutrSrv1FastatAmt)){
		result.nutrition_saturatedfat_amt = fixMass(entry.NutrSrv1FastatAmt)
	}

	// nutrition_saturatedfat_dv
	if(hasContent(entry.NutrSrv1FastatRDA)){
		result.nutrition_saturatedfat_dv = fixPercent(entry.NutrSrv1FastatRDA)
	}

	// nutrition_transfat_amt
	if(hasContent(entry.NutrSrv1FatrnAmt)){
		result.nutrition_transfat_amt = fixMass(entry.NutrSrv1FatrnAmt)
	}

	// nutrition_polyunsaturatedfat_amt
	if(intValue(entry.F_NUTRSRV1_FAPU_AMT)){
		result.nutrition_polyunsaturatedfat_amt = fixMass(entry.F_NUTRSRV1_FAPU_AMT)
	}

	// nutrition_monounsaturatedfat_amt
	if(intValue(entry.F_NUTRSRV1_FAMS_AMT)){
		result.nutrition_monounsaturatedfat_amt = fixMass(entry.F_NUTRSRV1_FAMS_AMT)
	}

	// nutrition_cholesterol_amt
	if(intValue(entry.F_NUTRSRV1_CHOL_AMT)){
		result.nutrition_cholesterol_amt = fixMass(entry.F_NUTRSRV1_CHOL_AMT)
	}

	// nutrition_cholesterol_dv
	if(intValue(entry.NutrSrv1CholRDA)){
		result.nutrition_cholesterol_dv = fixPercent(entry.NutrSrv1CholRDA)
	}

	// nutrition_sodium_amt
	if(intValue(entry.NutrSrv1NaAmt)){
		result.nutrition_sodium_amt = fixMass(entry.NutrSrv1NaAmt)
	}

	// nutrition_sodium_dv
	if(intValue(entry.NutrSrv1NaRDA)){
		result.nutrition_sodium_dv = fixPercent(entry.NutrSrv1NaRDA)
	}

	// nutrition_potassium_amt
	if(intValue(entry.F_NUTRSRV1_K_AMT)){
		result.nutrition_potassium_amt = fixMass(entry.F_NUTRSRV1_K_AMT)
	}

	// nutrition_potassium_dv
	if(intValue(entry.F_NUTRSRV1_K_RDA)){
		result.nutrition_potassium_dv = fixPercent(entry.F_NUTRSRV1_K_RDA)
	}
	// nutrition_carbohydrates_amt
	if(intValue(entry.F_NUTRSRV1_CHO_AMT)){
		result.nutrition_carbohydrates_amt = fixMass(entry.F_NUTRSRV1_CHO_AMT)
	}
	// nutrition_carbohydrates_dv
	if(intValue(entry.NutrSrv1ChoRDA)){
		result.nutrition_carbohydrates_dv = fixPercent(entry.NutrSrv1ChoRDA)
	}

	// nutrition_fiber_amt
	if(intValue(entry.NutrSrv1FibtswAmt)){
		result.nutrition_fiber_amt = fixMass(entry.NutrSrv1FibtswAmt)
	}

	// nutrition_fiber_dv
	if(intValue(entry.NutrSrv1FibtswRDA)){
		result.nutrition_fiber_dv = fixPercent(entry.NutrSrv1FibtswRDA)
	}

	// nutrition_sugars_amt
	if(intValue(entry.NutrSrv1SugarAmt)){
		result.nutrition_sugars_amt = fixMass(entry.NutrSrv1SugarAmt)
	}

	// nutrition_protein_amt
	if(intValue(entry.NutrSrv1ProAmt)){
		result.nutrition_protein_amt = fixMass(entry.NutrSrv1ProAmt)
	}

	// nutrition_contains
	if(intValue(entry.IngredientInfoDoesContainStatement)){
		result.nutrition_contains = entry.IngredientInfoDoesContainStatement
	}

	entry.__properties = result

	if(cb) cb(entry)
	return entry
}

module.exports = {
	cleanup:cleanup
}