// 影音书籍仓库（movie_books 表），对应 PHP App\Repository\MovieBookRepository
const { MovieBook } = require('../Model/MovieBook');

class MovieBookRepository {
  constructor(db) {
    this.db = db;
  }

  static hydrate(r) {
    return new MovieBook({
      id: r.id,
      type: r.type,
      title: r.title,
      tmdbId: r.tmdb_id === null ? null : String(r.tmdb_id),
      status: r.status,
      rating: r.rating === null ? null : parseFloat(r.rating),
      posterUrl: r.poster_url,
      note: r.note,
      lastModified: parseInt(r.last_modified, 10),
      isDeleted: parseInt(r.is_deleted, 10),
      deviceId: r.device_id,
      needsSync: parseInt(r.needs_sync, 10),
    });
  }

  async list() {
    const [rows] = await this.db.query(
      'SELECT * FROM movie_books WHERE is_deleted=0 ORDER BY id DESC'
    );
    return rows.map(MovieBookRepository.hydrate);
  }

  async byId(id) {
    const [rows] = await this.db.query('SELECT * FROM movie_books WHERE id=?', [id]);
    return rows.length ? MovieBookRepository.hydrate(rows[0]) : null;
  }

  async upsert(m) {
    const now = Date.now();
    const sql = `INSERT INTO movie_books
      (id,type,title,tmdb_id,status,rating,poster_url,note,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,0)
      ON DUPLICATE KEY UPDATE
        type=VALUES(type),title=VALUES(title),tmdb_id=VALUES(tmdb_id),status=VALUES(status),
        rating=VALUES(rating),poster_url=VALUES(poster_url),note=VALUES(note),
        last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0`;
    await this.db.query(sql, [
      m.id,
      m.type,
      m.title,
      m.tmdbId,
      m.status,
      m.rating,
      m.posterUrl,
      m.note,
      m.lastModified ?? now,
      m.isDeleted ?? 0,
      m.deviceId,
    ]);
  }

  async softDelete(id) {
    await this.db.query(
      'UPDATE movie_books SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?',
      [Date.now(), id]
    );
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM movie_books WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map(MovieBookRepository.hydrate);
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM movie_books WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }

  async clearDirty(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`UPDATE movie_books SET needs_sync=0 WHERE id IN (${ph})`, ids);
    }
  }

  async clearDeleted(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`DELETE FROM movie_books WHERE id IN (${ph})`, ids);
    }
  }
}

module.exports = { MovieBookRepository };
