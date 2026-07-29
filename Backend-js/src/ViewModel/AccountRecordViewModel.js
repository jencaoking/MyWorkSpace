// 记账记录视图模型，对应 PHP App\ViewModel\AccountRecordViewModel
const { AccountRecord } = require('../Model/AccountRecord');

function fromArray(a) {
  return AccountRecord.fromUploadArray(a);
}

function toArray(m) {
  return m.toApiArray();
}

function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
