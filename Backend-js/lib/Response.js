// JSON 响应（API 的输出即「视图」），对应 PHP 的 App\Response
const { ApiException } = require('../src/Exception/ApiException');

function json(res, data, httpCode = 200) {
  res.status(httpCode).json(data);
}

function error(res, msg, code = 500, http = 500, extra = {}) {
  json(res, { code, message: msg, data: extra }, http);
}

function errorException(res, e) {
  const isApi = e instanceof ApiException;
  json(
    res,
    {
      code: isApi ? e.code : 500,
      message: e && e.message ? e.message : 'Internal Server Error',
      data: {},
    },
    isApi ? e.httpCode : 500
  );
}

module.exports = { json, error, errorException };
