// 后台管理鉴权配置（对应 PHP config/admin.php）。
//
// 密码来源优先级（从高到低）：
//   1) 环境变量 SELFWORK_ADMIN_PASSWORD（推荐，支持明文或 bcrypt 哈希）
//   2) .env 中的同名变量
//   3) 内置默认哈希（对应明文 admin123，仅本地开发用）
const bcrypt = require('bcryptjs');
const { envOr } = require('../lib/env');

const DEFAULT_HASH = '$2y$12$taxM81UV.TSSHkT8f8yWC..6nL6nZLQEJ8U7Wwa3pZLiMNwmZ4xoi';

const env = envOr('SELFWORK_ADMIN_PASSWORD', '');

let passwordHash = null;
let passwordPlain = null;
let source = 'default';

if (env) {
  if (env.startsWith('$2y$') || env.startsWith('$2a$') || env.startsWith('$2b$')) {
    passwordHash = env;
  } else {
    passwordPlain = env;
  }
  source = 'env';
} else {
  passwordHash = DEFAULT_HASH;
  source = 'default';
}

async function verify(plain) {
  if (passwordPlain !== null) return plain === passwordPlain;
  if (!passwordHash) return false;
  // PHP 的 $2y$ 与 bcryptjs 的 $2a$ 等价，仅版本标记不同
  const h = passwordHash.startsWith('$2y$') ? '$2a$' + passwordHash.slice(4) : passwordHash;
  return bcrypt.compare(plain, h);
}

module.exports = {
  passwordHash,
  passwordPlain,
  source,
  defaultHash: DEFAULT_HASH,
  verify,
};
