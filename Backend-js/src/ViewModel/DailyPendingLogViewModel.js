// 每日未完成作业视图模型，对应 PHP App\ViewModel\DailyPendingLogViewModel
const { DailyPendingLog } = require('../Model/DailyPendingLog');

function fromArray(a) {
  return DailyPendingLog.fromUploadArray(a);
}
function toArray(m) {
  return m.toApiArray();
}
function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
