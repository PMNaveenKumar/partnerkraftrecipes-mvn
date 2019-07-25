var request = require('request')

function ApplePie(config){

	if(!config.post){
		config.post = {}
	}

	for(var key in config.post){
		if(!(config.post[key] instanceof Object)){
			config.post[key] = {
				defaultValue:config.post[key],
				process:function(data, defaultValue){
					return data || defaultValue
				}
			}
		}
	}

	if(!config.get){
		config.get = {}
	}

	for(var key in config.get){
		if(!(config.get[key] instanceof Object)){
			config.get[key] = {
				defaultValue:config.get[key],
				process:function(data, defaultValue){
					return data || defaultValue
				}
			}
		}
	}

	if(!config.rest){
		config.rest = []
	}

	for(let i=0; i<config.rest.length; i++){
		if(!(config.rest[i] instanceof Object)){
			config.rest[i] = {
				key:config.rest[i],
				process:function(data, defaultValue){
					return data || defaultValue
				}
			}
		}
	}

	function send(data, cb){

		let requestObject = {
			method:data.method || config.method || 'GET',
			url:data.url ? data.url : `${config.url}`
		}
		
		if(config.headers){
			requestObject.headers = config.headers
		}

		let prop,key,val

		// process rest props
		if(config.rest){
			let restArray = []
			for(var i=0; i<config.rest.length; i++){
				prop = config.rest[i]
				if(data.rest[prop.key]){
					restArray.push(data.rest[prop.key])
				} else {
					break
				}
			}

			// new code
			if (restArray.length) {
				requestObject.url += '/'+restArray.join('/')
			}			
			//requestObject.url = requestObject.url.replace(/\/\//g,'/')
		}

		// process POST props
		if(config.post && Object.keys(config.post).length>0){
			requestObject.form = {}
			for(key in config.post){
				prop = config.post[key]
				val = data.post && data.post[key] ? data.post[key] : prop.defaultValue
				requestObject.form[key] = prop.process(val, prop.defaultValue)
			}
		}

		// process GET props
		if(config.get){
			requestObject.qs = Object.assign({},data.get)
			for(key in config.get){
				prop = config.get[key]
				val = data.get && data.get[key] ? data.get[key] : prop.defaultValue
				requestObject.qs[key] = prop.process(val, prop.defaultValue)
			}
		}

		// handle situation where GET/POST exist concurrently
		if(requestObject.qs && requestObject.method==='POST'){
			let qs = Object.keys(requestObject.qs).map(function(key) {
				return encodeURIComponent(key) + "=" + encodeURIComponent(requestObject.qs[key])
			}).join('&')
			requestObject.url = `${config.url}?${qs}`
			delete requestObject.qs
		}

		/*console.log('ApplePie',requestObject)
		console.log('***** Request *****');*/
		//console.log(JSON.stringify(requestObject));

		request(requestObject, function (error, response, body) {
			if(error){
				cb(config.defaultResponse || {error:true})
			} else {
				if(config.process){
					body = config.process(body, requestObject, function(body){
						cb(body)
					})
				} else {
					cb(body)
				}
			}
	    })
	}

	return {
		send:send
	}
}

module.exports = ApplePie