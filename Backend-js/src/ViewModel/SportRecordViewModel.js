// 运动记录视图模型，对应 PHP App\ViewModel\SportRecordViewModel
const { SportRecord } = require('../Model/SportRecord');

function fromArray(a) {
  return SportRecord.fromUploadArray(a);
}
function toArray(m) {
  return m.toApiArray();
}
function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
