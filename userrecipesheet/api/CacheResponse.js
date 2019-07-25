var fs = require('fs')
var path = require('path')
var sha1 = require('sha1')

function getPath(filename){
	return path.join(__dirname,'./cache/'+filename+'.json')
}

function generateCacheFilename(config){
	if(typeof config==='string'){
		config = {
			key:config
		}
	}
	if(!config){
		config = {}
	}
	config = Object.assign({
		prefix:''
	},config)

	if(config.sha){
		return config.prefix+sha1(config.key)
	} else {
		return config.prefix+config.key
	}
}

function saveFile(cacheConfig,data){
	let filename = generateCacheFilename(cacheConfig)
	let filepath = getPath(filename)
	let content = typeof data==='string' ? data : JSON.stringify(data,null,2)
	fs.writeFileSync(filepath, content, {flag:'w'})
}

function getFile(filename){
	let filepath = getPath(filename)
	if(fs.existsSync(filepath)){
		let content = fs.readFileSync(filepath, 'utf8')
		return content
	} else {
		return false
	}
}

function isCached(cacheConfig){
	let filename = generateCacheFilename(cacheConfig)
	let filepath = getPath(filename)
	return fs.existsSync(filepath)
}

function getCache(cacheConfig){
	let filename = generateCacheFilename(cacheConfig)
	return getFile(filename)
}

function setCache(cacheConfig,data){
	return saveFile(cacheConfig,data)
}

function killCache(cacheConfig){
	let filename = generateCacheFilename(cacheConfig)
	let filepath = getPath(filename)
	if(fs.existsSync(filepath)){
		fs.unlinkSync(filepath)
		return true
	}
	return false
}

module.exports = {
	isCached:isCached,
	getCache:getCache,
	setCache:setCache,
	killCache:killCache
}