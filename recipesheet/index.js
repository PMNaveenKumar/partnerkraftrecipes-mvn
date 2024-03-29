const express = require('express')
const fs = require('fs')
const path = require('path')
const Freemarker = require('freemarker.js')
const app = express()
const openport = require('openport')
const opn = require('opn')
// var exec = require('child_process').exec;
// var KraftProduct = require('./KraftProduct')
// var KraftBrands = require('./KraftProduct/KraftBrands.js')
// const Promise = require("bluebird")
const KraftAPI = require('./api/kraft/')
// const ExportPIM = require('./ExportPIM/')
const bodyParser = require('body-parser')
var toXLS = require('./toXLS/');
const ProductCleaner = require('./toXLS/KraftProductCleanup.js')

/*

Helper

*/

var fm = new Freemarker({
	viewRoot: __dirname+'/freemarker',
	options: {}
})

app.set('port', (process.env.PORT || 5002))
app.use(express.static(__dirname + '/public'))
app.use(bodyParser.json())
app.use(bodyParser.urlencoded({ extended: true }))

//
// API JSON Responses
//

//
// log
//
var util = require('util');
var logFile = fs.createWriteStream(__dirname + '/debug.tsv', { flags: 'a+' });
var logStdout = process.stdout;

console.log = function(data, newLine) {
	logFile.write(util.format(data) + (newLine ? '\n' : ''));
	logStdout.write(util.format(data) + (newLine ? '\n' : ''));
};

app.get('/api/kraft/recipes/:recipeId', function(req, res) {
	KraftAPI.getRecipes(req.params.recipeId, function(data){
		res.end(data)
	},{nocache:req.query.nocache || false})
})

app.get('/api/kraft/recipes/search', function(req, res) {
	KraftAPI.getRecipeRelated({ProductName:req.params.query}, function(data){
		res.end(data)
	},{nocache:req.query.nocache || false})
})

app.get('/api/kraft/brands', function(req, res) {
	if(req.query.products){
		KraftAPI.getBrandsWithProducts(function(jsonResponse){
			res.end(jsonResponse)
		})
	} else {
		KraftAPI.getBrands(function(jsonResponse){
			res.end(jsonResponse)
		})
	}
})

app.get('/api/kraft/brands/:brandId', function(req, res) {
	KraftAPI.getBrandProducts(req.params.brandId, function(productsResp){
		res.end(productsResp)
	})
})

app.get('/api/kraft/product/:productId', function(req, res) {
	if(req.params.productId.indexOf(',')>-1){
		KraftAPI.getProducts(req.params.productId, function(productsResp){
			if(req.query.clean==='1'){
				const ProductCleaner = require('./toXLS/KraftProductCleanup.js')
				let data = JSON.parse(productsResp)
				let products = data.Products.map(function(product){
					return ProductCleaner.cleanup(product).__properties
				})
				productsResp = JSON.stringify({entries:products},null,2)
			}
			res.end(productsResp)
		})
	} else {
		KraftAPI.getProduct(req.params.productId, function(productsResp){
			if(req.query.clean==='1'){
				const ProductCleaner = require('./toXLS/KraftProductCleanup.js')
				productsResp = JSON.stringify(ProductCleaner.cleanup(JSON.parse(productsResp)).__properties,null,2)
			}
			res.end(productsResp)
		})
	}
})

//
// UI
//

app.get('/ui', function(req,res){
	let html = fm.renderSync('iframes.html', {})
	let pageHTML = fm.renderSync('framework.html', {title:'Kraft Brands', body:html})
	res.end(pageHTML)
})

app.get('/ui/kraft/brands', function(req, res) {
	KraftAPI.getBrands(function(brandsResp){
		let brandsHTML = fm.renderSync('brands.html', JSON.parse(brandsResp))
		let pageHTML = fm.renderSync('framework.html', {title:'Kraft Brands', body:brandsHTML})
		if(typeof pageHTML==='string'){
			res.end(pageHTML)
		} else {
			res.end(brandsResp)
		}
	})
})

app.get('/ui/kraft/brands/:brandId', function(req, res) {
	KraftAPI.getBrandProducts(req.params.brandId, function(brandsResp){
		let brandsHTML = fm.renderSync('brand_products.html', JSON.parse(brandsResp))
		let pageHTML = fm.renderSync('framework.html', {title:'Kraft Brands', body:brandsHTML})
		if(typeof pageHTML==='string'){
			res.end(pageHTML)
		} else {
			res.end(brandsResp)
		}
	})
})

app.get('/ui/kraft/product/:productId', function(req, res) {
	KraftAPI.getProduct(req.params.productId, function(productsResp){
		let data = ProductCleaner.cleanup(JSON.parse(productsResp))
		let productHTML = fm.renderSync('product-nutrition.html', data.__properties)

		if(typeof productHTML!=='string'){
			return res.end('Error: product-nutrition.html'+JSON.stringify(data.__properties,null,2))
		}

		let pageHTML = fm.renderSync('framework.html', {title:'Kraft Product', body:productHTML || 'hello'})
		
		if(typeof pageHTML==='string'){
			res.end(pageHTML)
		} else {
			res.end('Error')
		}
	})
})

//
// product import form
//

app.all('/import', function(req, res) {
	let html = fm.renderSync('import-form.html', {})
	let pageHTML = fm.renderSync('framework.html', {title:'Kraft Import', body:html})
	res.end(pageHTML)
})

app.all('/import/submit', function(req, res) {
	let config = {
		brands:req.body.brands ? req.body.brands.split(',') : [],
		products:req.body.products ? req.body.products.split(',') : [],
		recipes:req.body.recipes ? req.body.recipes.split(',') : [],
		filename:req.body.filename || null,
		options:{
			cache:Boolean(req.body.cache)
		}
	}
	toXLS.importKraftData(config,function(resp){
		let html = fm.renderSync('import-form.html', resp)
		let pageHTML = fm.renderSync('framework.html', {title:'Kraft Import', body:html})
		res.end(pageHTML)
	})
})

// new code
app.post('/export/recipes', function(request, response) {
	var params = [ { name: 'brandId', required: true }, { name: 'languageId', required: true }, { name: 'filename', required: false } ],
		paramsNotExist = params.filter(function(obj) {
			if (obj.required && (request.body[obj.name] == null)) {
				return obj;
			}
		});

	if (paramsNotExist.length == 0) {
		var options = {
			brandId 	: 	request.body.brandId,
			languageId 	: 	request.body.languageId,
			filename 	: 	request.body.filename || new Date().getTime().toString()
		};

		toXLS.generateRecipeData(options, function(result) {
			response.json(result);
		});
	} else {
		response.json({ status: 'Failure', message: `Missing required parameter ${paramsNotExist[0].name}` });
	}
});

app.get('/tst', function(request, response) {
	console.log('waiting...');
	setTimeout(function() {
		console.log('sending...');
		response.json({});
	}, (60000 * 2.75));
});

app.get('/generate_pim_sheet', function(request, response) {
	var config = {
		brandId 		: 	request.query.brandId || 1,
		languageId 		: 	request.query.languageId || 1,
		pageNo 			: 	request.query.pageNo || 1,
		pageSize 		: 	request.query.pageSize || 1,
		lastUpdatedDate : 	request.query.lastUpdatedDate || '0401970',
		domainName 		: 	request.query.domainName || 'kraftrecipes.com',
		filename 		: 	request.query.filename || null
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
});

app.all('/facet_mapping', function(request, response) {
	facetMapping.init(function(result) {
		response.send(result);
	});
});

app.get('/generate_category_mapping_sheet', function(request, response) {
	var config = {
		languageId 	: 	request.query.languageId ? request.query.languageId.split(',') : [ '1' ]
	};

	try {
		toXLS.categoryMapping.init(config, function(data) {
			response.send(data);
		});
	} catch(exception) {
		response.send({ status: 'Failure', message: exception.message });
	}
});

app.get('/logtest', function(request, response) {
	for (var i = 1; i <= 10; i++) {
		console.log('12345 https://stackoverflow.com/questions/8393636/node-log-in-a-file-instead-of-the-console 200 NO_RECIPE_FOUND');
		if (i == 10) {
			response.send({ status: 'success' });
		}
	}
});

openport.find({ startingPort: 5000 }, function(err, port) {
    if (err) {
        console.log(err)
    } else {
        var server = app.listen(port, function() {
            // console.log('Running on port', port)
            // opn(`http://localhost:${port}/`, { app: ['google chrome'] })
        });
        server.timeout = 1000 * 60 * 60 * 3;
    }
})