// App 接口共享令牌（对应 PHP config/api.php）。
// 通过 .env 的 SELFWORK_API_TOKEN 注入；未配置则为空（不强制鉴权）。
const { envOr } = require('../lib/env');

const token = envOr('SELFWORK_API_TOKEN', '');
module.exports = { appApiToken: token };
