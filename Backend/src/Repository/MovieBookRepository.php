<?php
namespace App\Repository;

use App\Model\MovieBook;
use PDO;

/** 影音书籍仓库。 */
class MovieBookRepository {
    public function __construct(private PDO $pdo) {}

    public function list(): array {
        $stmt = $this->pdo->query("SELECT * FROM movie_books WHERE is_deleted=0 ORDER BY id DESC");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function byId(string $id): ?MovieBook {
        $stmt = $this->pdo->prepare("SELECT * FROM movie_books WHERE id=?");
        $stmt->execute([$id]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        return $r ? self::hydrate($r) : null;
    }

    public function upsert(MovieBook $m): void {
        $now = time() * 1000;
        $sql = "INSERT INTO movie_books
            (id,type,title,tmdb_id,status,rating,poster_url,note,last_modified,is_deleted,device_id,needs_sync)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,0)
            ON DUPLICATE KEY UPDATE
            type=VALUES(type),title=VALUES(title),tmdb_id=VALUES(tmdb_id),status=VALUES(status),
            rating=VALUES(rating),poster_url=VALUES(poster_url),note=VALUES(note),
            last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $m->id, $m->type, $m->title, $m->tmdbId, $m->status, $m->rating,
            $m->posterUrl, $m->note, $m->lastModified ?? $now, $m->isDeleted ?? 0, $m->deviceId
        ]);
    }

    public function softDelete(string $id): void {
        $this->pdo->prepare("UPDATE movie_books SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?")
            ->execute([time() * 1000, $id]);
    }

    public function findDirty(): array {
        $stmt = $this->pdo->query("SELECT * FROM movie_books WHERE needs_sync=1 AND is_deleted=0");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function findDeletedIds(): array {
        $stmt = $this->pdo->query("SELECT id FROM movie_books WHERE is_deleted=1 AND needs_sync=1");
        return array_map(static fn($r) => $r['id'], $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function clearDirty(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("UPDATE movie_books SET needs_sync=0 WHERE id IN ($ph)")->execute($ids);
        }
    }

    public function clearDeleted(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("DELETE FROM movie_books WHERE id IN ($ph)")->execute($ids);
        }
    }

    private static function hydrate(array $r): MovieBook {
        $m = new MovieBook();
        $m->id = $r['id'];
        $m->type = $r['type'];
        $m->title = $r['title'];
        $m->tmdbId = $r['tmdb_id'] === null ? null : (string)$r['tmdb_id'];
        $m->status = $r['status'];
        $m->rating = $r['rating'] === null ? null : (float)$r['rating'];
        $m->posterUrl = $r['poster_url'];
        $m->note = $r['note'];
        $m->lastModified = (int)$r['last_modified'];
        $m->isDeleted = (int)$r['is_deleted'];
        $m->deviceId = $r['device_id'];
        $m->needsSync = (int)$r['needs_sync'];
        return $m;
    }
}
