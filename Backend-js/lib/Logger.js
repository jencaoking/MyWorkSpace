// 文件日志（对应 PHP 的 App\Logger），写入 storage/logs/app-YYYY-MM-DD.log
const fs = require('fs');
const path = require('path');

const LOG_DIR = path.join(__dirname, '..', 'storage', 'logs');
const THRESHOLD = (process.env.LOG_LEVEL || 'debug').toLowerCase();
const ORDER = { debug: 0, info: 1, warning: 2, error: 3 };

function ts() {
  return new Date().toISOString().replace('T', ' ').slice(0, 23);
}

function write(level, msg, ctx) {
  try {
    if ((ORDER[level] ?? 0) < (ORDER[THRESHOLD] ?? 0)) return;
    fs.mkdirSync(LOG_DIR, { recursive: true });
    const file = path.join(LOG_DIR, `app-${new Date().toISOString().slice(0, 10)}.log`);
    const ctxStr = ctx ? ' ' + JSON.stringify(ctx) : '';
    fs.appendFileSync(file, `[${ts()}] [${level}] ${msg}${ctxStr}\n`);
  } catch (_) {
    /* 日志失败不应影响主流程 */
  }
}

module.exports = {
  debug: (m, c) => write('debug', m, c),
  info: (m, c) => write('info', m, c),
  warning: (m, c) => write('warning', m, c),
  error: (m, c) => write('error', m, c),
  exception: (e, c) => write('error', (e && e.stack) || String(e), c),
};
