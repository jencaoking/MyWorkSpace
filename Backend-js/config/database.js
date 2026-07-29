// 数据库连接（mysql2/promise 连接池，对应 PHP 的 PDO 配置）。
// 由 server.js 通过 require('./config/database') 取得连接池。
const mysql = require('mysql2/promise');
const { envOr } = require('../lib/env');

const pool = mysql.createPool({
  host: envOr('DB_HOST', 'localhost'),
  user: envOr('DB_USER', 'mywork'),
  password: envOr('DB_PASS', 'mywork_pass'),
  database: envOr('DB_NAME', 'mywork'),
  charset: 'utf8mb4',
  waitForConnections: true,
  connectionLimit: 10,
  timezone: 'Z',
});

module.exports = pool;
