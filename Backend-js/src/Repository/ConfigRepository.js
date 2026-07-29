// 通用键值配置存储（app_config 表），对应 PHP App\Repository\ConfigRepository
class ConfigRepository {
  constructor(db) {
    this.db = db;
  }

  async get(key, def = null) {
    const [rows] = await this.db.query('SELECT cfg_value FROM app_config WHERE cfg_key = ?', [key]);
    return rows.length ? rows[0].cfg_value : def;
  }

  async getAll(keys) {
    const out = {};
    for (const k of keys) out[k] = await this.get(k, '');
    return out;
  }

  async set(key, value) {
    await this.db.query(
      'INSERT INTO app_config (cfg_key, cfg_value) VALUES (?, ?) ' +
        'ON DUPLICATE KEY UPDATE cfg_value = VALUES(cfg_value)',
      [key, value]
    );
  }

  async setMany(kv) {
    for (const [k, v] of Object.entries(kv)) {
      if (typeof v === 'string') await this.set(k, v);
    }
  }
}

module.exports = { ConfigRepository };
