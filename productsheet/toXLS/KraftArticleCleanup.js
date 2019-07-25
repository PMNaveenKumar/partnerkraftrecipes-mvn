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
	config.cleanUpLog.write("\n" + id);
	config.descCountLog.write("\n" + id);
	var result = {}
	result.id = id;
	entry.id = id;
	result.hasimage = false;
	result.articletype = entry.articleType;
	result.templatetype = entry.templateType;
	
	if(config && config.url && config.url.length > 0)
	{
		var url = config.url;		
		var replace = "-";
		var re = new RegExp(replace, 'g');
		var catArr = url.replace("http://www.kraftrecipes.com/","").replace(".aspx","").replace(re, " ").split("/");

		//catArr.splice(catArr.length-2);
		if(catArr.length > 1)
		{
			catArr = catArr.splice(0,catArr.length-1);
		}
		for(var idxval = 0; idxval < catArr.length; idxval++)
		{
			catArr[idxval] = catArr[idxval].replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
		}

		var tabcount = 6;
		var remainingtabs = tabcount - catArr.length;
		var catMapStr = "\n" + url + "\t" + catArr.join("\t");
		for(var tabidx = 1; tabidx <= remainingtabs ; tabidx++)
		{
			catMapStr = catMapStr + "\t";
		}
		catMapStr = catMapStr + id;
		config.catMapLog.write("\n" + url + "\t" + catArr.join("\t") + "\t" + id);
	}
	if(entry.name && entry.name.length > 0)
	{
		result.name = entry.name;
		result.productname  = entry.productname;
		result.seoname = entry.name.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
		config.cleanUpLog.write("\t");
		config.oldNewLog.write("\n" + config.url + "\t" + "/article/" + id + "/" + result.seoname);
	}
	else
	{
		config.cleanUpLog.write("\tname");
	}

	if(entry.templateType)
	{
		result.templatetype = entry.templateType;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\ttemplateType");
	}

	if(entry.articleType)
	{
		result.articletype = entry.articleType;
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tarticletype");
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

	if(entry.properties.iteminfo.videos && entry.properties.iteminfo.videos.length > 0)
	{
		var splitvide = entry.properties.iteminfo.videos[0].value.split('|');
		var videoObj = {};
		var videoArr = [];
		for(i=0; i < splitvide.length; i++)
		{
			var key = splitvide[i].split(':')[0].trim();
			var value = splitvide[i].split(':')[1].trim();
			if(key == "VideoId")
			{
				videoObj.videoid = value;
				videoArr.push('{"sourceName":"BRIGHTCOVE","videoID":' + value + '}');
			}
			if(key == "playerid")
				videoObj.playerid = value;
		}
		videoObj.name = "BRIGHTCOVE";
		//result.video  = JSON.stringify(videoObj);
		result.video  = '['+videoArr.toString()+']';
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\tvideo");
	}

	result.hasvideo  = entry.properties.iteminfo.videos ? 'true' : 'false';
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

	let keywordsandtag = entry.properties.iteminfo.metainfo.filter(x => x.title === "keywords");
	if(keywordsandtag && keywordsandtag.length > 0)
	{
		let newValues = [];
		let oldValues = keywordsandtag[0].description.split(',');
		for(i=0; i < oldValues.length; i++)
		{
			newValues.push(oldValues[i].trim());
		}
		result.tags = newValues.toString();
		result.keywords = newValues.toString();
		config.cleanUpLog.write("\t");
	}
	else
	{
		config.cleanUpLog.write("\ttags");
	}

	var descData = "";
	if(entry.properties.iteminfo.description && entry.properties.iteminfo.description.length > 0)
	{
		//Desc char-set count
		entry.properties.iteminfo.description.map(function(itm, idx)
		{
			descData = descData + itm.value;
		});

		if(descData.length > 32766)
		{
			config.descCountLog.write("\t" + descData.length + "\t" + descData);
		}
		else
		{
			result.description = descData;
			config.cleanUpLog.write("\t");
		}
	}
	else
	{
		//config.descCountLog.write("\t" + descData.length);
		config.cleanUpLog.write("\tdescription");
	}

	if(entry.name && entry.name.length > 0)
	{

		result.semanticId = "/article/" + id + "/" +  entry.name.replace(/[^a-zA-Z0-9 ]/g, "").replace(/\s\s+/g, ' ').replace(/ +/g, "-").toLowerCase();
	}
	else
	{
		config.cleanUpLog.write("\tsemanticId");
	}

	entry.__properties = result

	if(cb) cb(entry)
	return entry
}

module.exports = {
	cleanup:cleanup
}