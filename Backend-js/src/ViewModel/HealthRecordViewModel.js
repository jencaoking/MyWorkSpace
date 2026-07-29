// 健康记录视图模型，对应 PHP App\ViewModel\HealthRecordViewModel
const { HealthRecord } = require('../Model/HealthRecord');

function fromArray(a) {
  return HealthRecord.fromUploadArray(a);
}
function toArray(m) {
  return m.toApiArray();
}
function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
