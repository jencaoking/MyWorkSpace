// 共享的 .env 加载与读取助手（每个 config 文件 require 本模块即可）
require('dotenv').config();

function envOr(name, fallback) {
  const v = process.env[name];
  return v === undefined || v === '' ? fallback : v;
}

module.exports = { envOr };
