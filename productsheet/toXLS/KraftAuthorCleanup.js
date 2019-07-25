var nutritionitems = {
	name 	: 	[],
	quantity: 	[],
	unit 	: 	[]
};

var tags = {
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

//Arulk Starts
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

function checkDuplicate(arrObj, inputStringValue)
{
	inputStringValue = inputStringValue.trim();
	if(arrObj.indexOf(inputStringValue) == -1 && inputStringValue != "")
		arrObj.push(inputStringValue);
	return arrObj;
}

function typeCheckAndStringify(jsonObj, keyVal){
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

function cleanup(id, entry, config, cb){
	config.catMapLog.write("\n" + id);
	config.cleanUpLog.write("\n" + id);
	var result = {}
	result.id = id;
	entry.id = id;
	result.hasimage = false;
		
	if(entry.name && entry.name.length > 0)
	{
		result.name = entry.name;
		result.seoname = entry.name.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
		config.cleanUpLog.write("\t");
		config.catMapLog.write("\t" + result.name + "\t" + result.seoname);
	}
	else
	{
		config.catMapLog.write("\t");
		config.cleanUpLog.write("\tname");
	}

	if(entry.properties.iteminfo.images && entry.properties.iteminfo.images.length > 0)
	{
		result.image  = entry.properties.iteminfo.images[0].image;
		let imagesArr = [];
		config.catMapLog.write("\t" + result.image);
		for(var propName in entry.properties.iteminfo.images){
			if(propName != "0")
			{
				imagesArr.push(entry.properties.iteminfo.images[propName].image);
			}
		};
		if(imagesArr && imagesArr.length > 0)
		{
			result.altimages  = imagesArr.toString();
			config.catMapLog.write("\t" + result.altimages);
		}
		else
		{
			config.catMapLog.write("\t");
		}
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.catMapLog.write("\t\t");
		config.cleanUpLog.write("\timage");
	}

	if(entry.properties.iteminfo.images)
	{
		config.catMapLog.write("\t" + "TRUE");
	}
	else
	{
		config.catMapLog.write("\t" + "FALSE");
	}

	

	if(entry.name && entry.name.length > 0)
	{
		result.semanticId = "/author/" + id + "/" +  entry.name.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
		config.catMapLog.write("\t" + result.semanticId);
	}
	else
	{
		config.catMapLog.write("\t");
		config.cleanUpLog.write("\tsemanticId");
	}

	if(entry.properties.iteminfo.description && entry.properties.iteminfo.description.length > 0)
	{
		result.description = JSON.stringify(entry.properties.iteminfo.description);
		config.catMapLog.write("\t" + result.description);
		/*if(entry.properties.iteminfo.description[0].name)
		{
			result.authorlink = entry.properties.iteminfo.description[0].name;
			config.catMapLog.write("\t" + result.authorlink);
		}
		else
		{
			config.catMapLog.write("\t");
			config.cleanUpLog.write("\t");
		}
		if(entry.properties.iteminfo.description[0].link)
		{
			result.authorname = entry.properties.iteminfo.description[0].link;
			config.catMapLog.write("\t" + result.authorname);
		}
		else
		{
			config.catMapLog.write("\t");
			config.cleanUpLog.write("\t");
		}

		if(entry.properties.iteminfo.description[0].value)
		{
			result.authordescription = entry.properties.iteminfo.description[0].value;
			config.catMapLog.write("\t" + result.authordescription);
		}
		else
		{
			config.catMapLog.write("\t");
			config.cleanUpLog.write("\t");
		}*/
	}
	else
	{
		config.catMapLog.write("\t");
		config.cleanUpLog.write("\tdescription");
	}

	if(entry.properties.iteminfo.metainfo && entry.properties.iteminfo.metainfo.length > 0)
	{
		result.metainfo = entry.properties.iteminfo.metainfo;
		config.catMapLog.write("\t" + JSON.stringify(result.metainfo));
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.catMapLog.write("\t");
		config.cleanUpLog.write("\tmetainfo");
	}

	/*let finalKeywords = [];
	let keywordsandtag = entry.properties.iteminfo.metainfo.filter(x => x.title === "keywords");
	if(keywordsandtag && keywordsandtag.length > 0)
	{
		let oldValues = keywordsandtag[0].description.split(',');
		for(i=0; i < oldValues.length; i++)
		{
			finalKeywords.push(oldValues[i].trim());
		}
	}

	if(entry.properties.iteminfo.keywords && entry.properties.iteminfo.keywords.length > 0)
	{
		let keywords = entry.properties.iteminfo.keywords;
		for(i=0; i < keywords.length; i++)
		{
			finalKeywords.push(keywords[i].trim());
		}
	}

	if(finalKeywords && finalKeywords.length > 0)
	{
		result.tags = finalKeywords.toString();
		result.keywords = finalKeywords.toString();
		config.catMapLog.write("\t"+result.keywords);
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.catMapLog.write("\t");
		config.cleanUpLog.write("\ttags");
	}

	/*result.hasimage = false;
	result.articletype = entry.articleType;
	result.templatetype = entry.templateType;
	
	if(entry.name && entry.name.length > 0)
	{
		result.name = entry.name;
		result.productname  = entry.productname;
		result.seoname = entry.name;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tname");
	} 

	if(entry.properties.iteminfo.images && entry.properties.iteminfo.images.length > 0)
	{
		result.image  = entry.properties.iteminfo.images[0].image;
		let imagesArr = [];
		for(var propName in entry.properties.iteminfo.images){
			if(propName != "0")
			{
				imagesArr.push(entry.properties.iteminfo.images[propName].image);
			}
		};
		if(imagesArr && imagesArr.length > 0)
		{
			result.altimages  = imagesArr.toString();
		}
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\timage");
	}

	result.hasimage  = entry.properties.iteminfo.images ? 'true' : 'false';
	
	if(entry.properties.iteminfo.metainfo && entry.properties.iteminfo.metainfo.length > 0)
	{
		result.metainfo = entry.properties.iteminfo.metainfo;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tmetainfo");
	}

	let finalKeywords = [];
	let keywordsandtag = entry.properties.iteminfo.metainfo.filter(x => x.title === "keywords");
	if(keywordsandtag && keywordsandtag.length > 0)
	{
		let oldValues = keywordsandtag[0].description.split(',');
		for(i=0; i < oldValues.length; i++)
		{
			finalKeywords.push(oldValues[i].trim());
		}
	}

	if(entry.properties.iteminfo.keywords && entry.properties.iteminfo.keywords.length > 0)
	{
		let keywords = entry.properties.iteminfo.keywords;
		for(i=0; i < keywords.length; i++)
		{
			finalKeywords.push(keywords[i].trim());
		}
	}

	if(finalKeywords && finalKeywords.length > 0)
	{
		result.tags = finalKeywords.toString();
		result.keywords = finalKeywords.toString();
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\ttags");
	}

	if(entry.properties.iteminfo.description && entry.properties.iteminfo.description.length > 0)
	{
		result.description = entry.properties.iteminfo.description[0].value;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tdescription");
	}

	if(entry.properties.iteminfo.publisherdate)
	{
		result.publisheddate = entry.properties.iteminfo.publisherdate;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tpublisherdate");
	}

	if(entry.properties.iteminfo.publisherlink)
	{
		result.authorprofile = entry.properties.iteminfo.publisherlink;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tpublisherlink");
	}

	if(entry.properties.iteminfo.publisher)
	{
		result.authorname = entry.properties.iteminfo.publisher;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tpublisher");
	}

	if(entry.name && entry.name.length > 0)
	{
		result.semanticId = "/blog/" + id + "/" +  entry.name.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
	}
	else
	{
		config.cleanUpLog.write("\tsemanticId");
	}
*/
result.name = entry.name;
	entry.__properties = result

	if(cb) cb(entry)
	return entry
}

module.exports = {
	cleanup:cleanup
}