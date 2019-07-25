var KraftAPI = require('../api/kraft/')
var ProductSchema = require('./KraftProductsSchema.js')
var ProductCleaner = require('./KraftProductCleanup.js')


function addProducts(productIds, workbook, cb, options){
	importProducts(productIds, function(products){
		products.map(function(product){
			addProduct(product,workbook)
		})
		if(cb) cb(products)
	},[],options)
}

function importProducts(productIds, cb, result, options){
	if(!result){
		result = []
	}
	if(!(productIds instanceof Array)){
		productIds = String(productIds).split(',')
	}
	// remove dupes
	productIds = productIds.filter(function(e,i,a){
		return a.indexOf(e)===i
	})

	if(productIds.length>0){
		let productId = productIds.pop()
		KraftAPI.getProduct(productId, function(response){
			if(response){
				console.log('product loaded',productId,productIds.length)
				ProductCleaner.cleanup(JSON.parse(response), function(cleanProduct){
					result.push(cleanProduct)
					importProducts(productIds,cb,result,options)
				})
			}
		},options)
	} else {
		if(cb) cb(result)
	}
}

function addProduct(product, workbook){

	// validate product
	if(!product.ID){
		console.log('addProduct: Invalid Product!',product.GTIN || 'No GTIN')
		return
	} else {
		console.log('addProduct:',product.ID)
	}

	// insert product master
	workbook.insertRows('productmaster',product,ProductSchema)

	// insert product master properties
	let props = []
	for(var propName in product.__properties){
		props.push({
			productid:product.ID,
			name:propName,
			value:product.__properties[propName]
		})
	}

	// save imported date to compare to Kraft product's "Updated" value during automated import
	// props.push({
	// 	productid:product.ID,
	// 	name:'importDate',
	// 	value:new Date().getTime()
	// })

	props.push({
		productid:product.ID,
		name:'entryType',
		value:'product'
	})

	workbook.insertRows('productmasterproperties',props,ProductSchema)

	// insert product category
	workbook.insertRows('productcategory',{
		categoryid:'products',
		productid:product.ID
	},ProductSchema)

	// insert sku master
	workbook.insertRows('skumaster',product,ProductSchema)

	// 00013000000277
}

module.exports = {
	addProducts:addProducts
}