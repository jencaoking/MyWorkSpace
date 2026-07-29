// 设备用户视图模型，对应 PHP App\ViewModel\DeviceUserViewModel
const { DeviceUser } = require('../Model/DeviceUser');

function fromArray(a) {
  return DeviceUser.fromArray(a);
}
function toArray(m) {
  return m.toApiArray();
}
function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
