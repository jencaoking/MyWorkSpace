// 影音书籍模型（movie_books 表），对应 PHP App\Model\MovieBook。tmdb_id 以字符串存储。
class MovieBook {
  constructor({
    id,
    type,
    title,
    tmdbId,
    status,
    rating,
    posterUrl,
    note,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.type = type;
    this.title = title;
    this.tmdbId = tmdbId;
    this.status = status;
    this.rating = rating;
    this.posterUrl = posterUrl;
    this.note = note;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  toApiArray() {
    return {
      id: this.id,
      type: this.type,
      title: this.title,
      tmdb_id: this.tmdbId,
      status: this.status,
      rating: this.rating,
      poster_url: this.posterUrl,
      note: this.note,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }

  static fromUploadArray(a) {
    const now = Date.now();
    return new MovieBook({
      id: a.id ?? null,
      type: a.type ?? 'movie',
      title: a.title ?? '',
      tmdbId: a.tmdb_id !== undefined && a.tmdb_id !== '' ? String(a.tmdb_id) : null,
      status: a.status ?? 'want',
      rating: a.rating !== undefined && a.rating !== '' ? parseFloat(a.rating) : null,
      posterUrl: a.poster_url ?? null,
      note: a.note ?? '',
      lastModified: a.last_modified !== undefined ? parseInt(a.last_modified, 10) : now,
      isDeleted: a.is_deleted !== undefined ? parseInt(a.is_deleted, 10) : 0,
      deviceId: a.device_id ?? null,
      needsSync: a.needs_sync !== undefined ? parseInt(a.needs_sync, 10) : 0,
    });
  }
}

module.exports = { MovieBook };
