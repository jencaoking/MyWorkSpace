// 设置仓储（user_settings 表）。单行镜像（固定 id='local'），仅 get / upsert。
// 对应 PHP App\Repository\SettingsRepository
class SettingsRepository {
  constructor(db) {
    this.db = db;
  }

  async get() {
    const [rows] = await this.db.query('SELECT * FROM user_settings WHERE id = ?', ['local']);
    if (rows.length === 0) return null;
    const row = rows[0];
    if (typeof row.module_toggles === 'string' && row.module_toggles) {
      try {
        row.module_toggles = JSON.parse(row.module_toggles);
      } catch (_) {
        /* keep as-is */
      }
    }
    return row;
  }

  async upsert(theme, moduleToggles, language) {
    const now = new Date().toISOString().slice(0, 19).replace('T', ' ');
    const togglesJson = moduleToggles === null ? null : JSON.stringify(moduleToggles);
    const sql = `INSERT INTO user_settings (id, theme, module_toggles, language, created_at, updated_at)
      VALUES ('local', ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        theme = VALUES(theme),
        module_toggles = VALUES(module_toggles),
        language = VALUES(language),
        updated_at = ?`;
    await this.db.query(sql, [theme, togglesJson, language, now, now, now]);
    return (await this.get()) ?? {};
  }
}

module.exports = { SettingsRepository };
