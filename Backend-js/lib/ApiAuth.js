// 可选的 App 接口共享令牌鉴权（对应 PHP 的 App\ApiAuth）。
// 仅当配置了 SELFWORK_API_TOKEN 时才强制校验；否则不拦截（保持开箱即用）。
const { ApiException } = require('../src/Exception/ApiException');
const apiConfig = require('../config/api');

function appApiTokenRequired(req) {
  const token = apiConfig.appApiToken;
  if (!token) return; // 未配置则不强制

  const header = String(req.headers['authorization'] || req.headers['x-api-token'] || '').trim();
  let provided = header;
  if (header.toLowerCase().startsWith('bearer ')) {
    provided = header.slice(7).trim();
  }
  if (provided === '' || provided !== token) {
    throw new ApiException('Unauthorized', 401, 401);
  }
}

module.exports = { appApiTokenRequired };
