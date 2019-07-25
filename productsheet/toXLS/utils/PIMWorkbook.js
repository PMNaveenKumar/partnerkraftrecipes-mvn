var Excel = require('exceljs')
var PIMSchema = require('./PIMSchema.js')

// create a convenience object for accessing the props of the sheets
var PimSchemaLookup = {}

PIMSchema.sheets.map(function(sheet){
    PimSchemaLookup[sheet.name] = sheet
    sheet.usedPrimaryKeys = {}
})

var WorkbookInstance = function(globalSchema){

    let workbookSchema = globalSchema || getDefaultSchema()
    let workbook

    function getDefaultSchema(){
        var defaultSchema = {}
        PIMSchema.sheets.map(function(sheet){
            defaultSchema[sheet.name] = {}
            sheet.columns.map(function(col){
                defaultSchema[sheet.name][col.key] = col.defaultValue!==undefined ? col.defaultValue : ''
            })
        })
        return defaultSchema
    }

    function getWorkbook(){
        let workbook = new Excel.Workbook()
        PIMSchema.sheets.map(function(sheet){
            let worksheet = workbook.addWorksheet(sheet.name)
            worksheet.columns = sheet.columns
        })
        return workbook
    }

    function save(filepath,cb){
        workbook.xlsx.writeFile(filepath)
        .then(function() {
            if(cb) cb(filepath)
        })
        .catch(function(err){
            console.log('PIMWorkbook.save() ERROR',filepath,arguments)
            if(cb) cb({error:1, errorMessage:err})
        })
    }

    function insertRows(sheetName, entries, customSchema){
        if(!(entries instanceof Array)){
            entries = [entries]
        }
        let schema = customSchema || workbookSchema
        let val,cellValue
        let row = {}
        let sheetSchema = schema[sheetName] || {}
        let sheetConfig = PimSchemaLookup[sheetName]

        entries.map(function(entry){

            // resolve all values 
            // console.log(sheetName,'--------------------------')
            
            for(col in sheetSchema){
                val = sheetSchema[col]

                // new code
                var defaultValue = undefined;
                sheetConfig.columns.map(function(column) {
                    if (column.key == col && column.defaultValue) {
                        defaultValue = column.defaultValue;
                    }
                });
                // new code                

                if(col in entry){
                    cellValue = entry[col]!==undefined ? entry[col] : val
                } else if(typeof val === 'function'){
                    cellValue = val(entry)
                } else if (defaultValue != undefined) {
                    cellValue = defaultValue;
                } else {
                    cellValue = val
                }
                row[col] = cellValue
            }

            // check values against primary keys in table
            if(sheetConfig.primaryKeys){
                
                let primaryKey = sheetConfig.primaryKeys.map(function(key){
                    return row[key]
                }).join('~')

                if(sheetConfig.usedPrimaryKeys[primaryKey]){
                    // this key is used - need to void this row
                } else {
                    sheetConfig.usedPrimaryKeys[primaryKey] = true
                    workbook.getWorksheet(sheetName).addRow(row)
                }
            } else {
                workbook.getWorksheet(sheetName).addRow(row)
            }

            //console.log(sheetName,'Added')
        })
    }

    function read(path, sheetName, callback) {
        var workBook = new Excel.Workbook(); 
        workBook.xlsx.readFile(path).then(function() {
            var sheet = workBook.getWorksheet(sheetName) || null;
            callback(sheet);
        });
    }

    workbook = getWorkbook()

    return {
        insertRows:insertRows,
        save:save,
        read: read
    }
}

module.exports = WorkbookInstance
