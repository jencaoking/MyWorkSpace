// 英语单词视图模型，对应 PHP App\ViewModel\EnglishWordViewModel
const { EnglishWord } = require('../Model/EnglishWord');

function fromArray(a) {
  return EnglishWord.fromUploadArray(a);
}
function toArray(m) {
  return m.toApiArray();
}
function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
