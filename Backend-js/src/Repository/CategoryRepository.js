// 分类仓储（categories 表），对应 PHP App\Repository\CategoryRepository
// 分类为共享参考数括，列表返回全部未删除项（跨设备可见）
const { Category } = require('../Model/Category');

class CategoryRepository {
  constructor(db) {
    this.db = db;
  }

  static hydrate(row) {
    return new Category({
      id: String(row.id),
      name: String(row.name),
      color: row.color ?? null,
      sortOrder: parseInt(row.sort_order, 10),
      isSystem: parseInt(row.is_system, 10),
      lastModified: parseInt(row.last_modified, 10),
      isDeleted: parseInt(row.is_deleted, 10),
      deviceId: String(row.device_id),
      needsSync: parseInt(row.needs_sync, 10),
    });
  }

  async list() {
    const [rows] = await this.db.query(
      'SELECT * FROM categories WHERE is_deleted=0 ORDER BY sort_order ASC, id ASC'
    );
    return rows.map(CategoryRepository.hydrate);
  }

  async byId(id) {
    const [rows] = await this.db.query('SELECT * FROM categories WHERE id=?', [id]);
    return rows.length ? CategoryRepository.hydrate(rows[0]) : null;
  }

  async upsertBatch(cats) {
    const sql = `INSERT INTO categories (id,name,color,sort_order,is_system,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,0,?,0)
      ON DUPLICATE KEY UPDATE
        name=VALUES(name), color=VALUES(color), sort_order=VALUES(sort_order),
        is_system=VALUES(is_system), last_modified=VALUES(last_modified),
        device_id=VALUES(device_id), needs_sync=0, is_deleted=0`;
    let n = 0;
    for (const c of cats) {
      await this.db.query(sql, [
        c.id,
        c.name,
        c.color,
        c.sortOrder,
        c.isSystem,
        c.lastModified,
        c.deviceId,
      ]);
      n++;
    }
    return n;
  }

  async deleteBatch(ids) {
    if (!ids || !ids.length) return 0;
    const ph = ids.map(() => '?').join(',');
    const [r] = await this.db.query(
      `UPDATE categories SET is_deleted=1, needs_sync=1, last_modified=? WHERE id IN (${ph})`,
      [Date.now(), ...ids]
    );
    return r.affectedRows;
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM categories WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map(CategoryRepository.hydrate);
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM categories WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }
}

module.exports = { CategoryRepository };
