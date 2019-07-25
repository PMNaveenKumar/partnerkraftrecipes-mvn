'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _promise = require('babel-runtime/core-js/promise');

var _promise2 = _interopRequireDefault(_promise);

exports.getFileByStream = getFileByStream;
exports.getDirInfo = getDirInfo;
exports.getFileStat = getFileStat;
exports.readFile = readFile;
exports.readStream = readStream;
exports.writeFile = writeFile;
exports.writeUnExistsFile = writeUnExistsFile;
exports.delFile = delFile;

var _fs = require('fs');

var _fs2 = _interopRequireDefault(_fs);

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getFileByStream(path) {
  return _fs2.default.ReadStream(path);
}

function getDirInfo(dir) {
  return new _promise2.default(function (resolve, reject) {
    _fs2.default.readdir(dir, function (err, files) {
      if (err) {
        return reject(err);
      }
      resolve(files);
    });
  });
}

function getFileStat(file) {
  return new _promise2.default(function (resolve, reject) {
    _fs2.default.lstat(file, function (err, stat) {
      if (err) {
        return reject(err);
      }
      resolve(stat);
    });
  });
}

function readFile(file) {
  return new _promise2.default(function (resolve, reject) {
    _fs2.default.readFile(file, 'utf-8', function (err, data) {
      if (err) {
        return reject(err);
      }
      resolve(data);
    });
  });
}

function readStream(file) {
  return _fs2.default.createReadStream(file);
}

function writeFile(filename, text) {
  return new _promise2.default(function (resolve, reject) {
    _fs2.default.writeFile(filename, text, function (err) {
      if (err) {
        return reject(err);
      }
      resolve(text);
    });
  });
}

function writeUnExistsFile(file, text) {
  var needCreateStack = [file];

  return new _promise2.default(function () {
    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    var search = function search() {
      file = _path2.default.resolve(file, '../');
      _fs2.default.stat(file, function (err) {
        if (err) {
          needCreateStack.push(file);
          search();
        } else {
          create();
        }
      });
    };
    var create = function create() {
      var _writeFile;

      var file = needCreateStack.pop();
      if (needCreateStack.length != 0) {
        return _fs2.default.mkdir(file, create);
      }
      (_writeFile = writeFile(file, text)).then.apply(_writeFile, args);
    };
    search();
  });
}

function delFile(file) {
  try {
    if (_fs2.default.statSync(file).isFile()) {
      _fs2.default.unlinkSync(file);
    } else {
      var children = _fs2.default.readdirSync(file);
      if (children && children.length != 0) {
        children.forEach(function (item) {
          delDir(_path2.default.join(file, item));
        });
      }
      _fs2.default.rmdirSync(file);
    }
  } catch (err) {
    return -1;
  }
}

exports.default = {
  getFileByStream: getFileByStream,
  getDirInfo: getDirInfo,
  getFileStat: getFileStat,
  readFile: readFile,
  writeFile: writeFile,
  writeUnExistsFile: writeUnExistsFile,
  delFile: delFile,
  readStream: readStream
};