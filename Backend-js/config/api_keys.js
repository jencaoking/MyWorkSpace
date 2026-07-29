// 第三方 API Key（对应 PHP config/api_keys.php）。仅服务端持有，不下发客户端。
// 优先从环境变量读取（生产推荐），回退占位符。
const { envOr } = require('../lib/env');

module.exports = {
  tmdb: envOr('TMDB_KEY', 'YOUR_TMDB_KEY'),
  qweather: envOr('QWEATHER_KEY', 'YOUR_QWEATHER_KEY'),
  youdao: {
    app_key: envOr('YOUDAO_APP_KEY', ''),
    app_secret: envOr('YOUDAO_APP_SECRET', ''),
  },
};
