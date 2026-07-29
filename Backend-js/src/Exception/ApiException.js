// 业务级预期异常：状态码 code 返回给客户端，httpCode 为 HTTP 状态
class ApiException extends Error {
  constructor(message, code = 1, httpCode = 400) {
    super(message);
    this.name = 'ApiException';
    this.code = code;
    this.httpCode = httpCode;
  }
}

module.exports = { ApiException };
