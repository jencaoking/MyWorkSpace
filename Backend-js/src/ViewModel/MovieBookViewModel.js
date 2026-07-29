// 影音书籍视图模型，对应 PHP App\ViewModel\MovieBookViewModel
const { MovieBook } = require('../Model/MovieBook');

function fromArray(a) {
  return MovieBook.fromUploadArray(a);
}
function toArray(m) {
  return m.toApiArray();
}
function listToArray(list) {
  return list.map((m) => m.toApiArray());
}

module.exports = { fromArray, toArray, listToArray };
