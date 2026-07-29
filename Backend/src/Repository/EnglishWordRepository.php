<?php
namespace App\Repository;

use App\Model\EnglishWord;
use PDO;

/** 英语单词仓库。 */
class EnglishWordRepository {
    public function __construct(private PDO $pdo) {}

    public function list(): array {
        $stmt = $this->pdo->query("SELECT * FROM english_words WHERE is_deleted=0 ORDER BY next_review ASC");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function byId(string $id): ?EnglishWord {
        $stmt = $this->pdo->prepare("SELECT * FROM english_words WHERE id=?");
        $stmt->execute([$id]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        return $r ? self::hydrate($r) : null;
    }

    public function upsert(EnglishWord $m): void {
        $now = time() * 1000;
        $sql = "INSERT INTO english_words
            (id,word,phonetic,meaning,example,familiarity,next_review,last_modified,is_deleted,device_id,needs_sync)
            VALUES (?,?,?,?,?,?,?,?,?,?,0)
            ON DUPLICATE KEY UPDATE
            word=VALUES(word),phonetic=VALUES(phonetic),meaning=VALUES(meaning),example=VALUES(example),
            familiarity=VALUES(familiarity),next_review=VALUES(next_review),last_modified=VALUES(last_modified),
            is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $m->id, $m->word, $m->phonetic, $m->meaning, $m->example,
            $m->familiarity, $m->nextReview, $m->lastModified ?? $now, $m->isDeleted ?? 0, $m->deviceId
        ]);
    }

    public function softDelete(string $id): void {
        $this->pdo->prepare("UPDATE english_words SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?")
            ->execute([time() * 1000, $id]);
    }

    public function findDirty(): array {
        $stmt = $this->pdo->query("SELECT * FROM english_words WHERE needs_sync=1 AND is_deleted=0");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function findDeletedIds(): array {
        $stmt = $this->pdo->query("SELECT id FROM english_words WHERE is_deleted=1 AND needs_sync=1");
        return array_map(static fn($r) => $r['id'], $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function clearDirty(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("UPDATE english_words SET needs_sync=0 WHERE id IN ($ph)")->execute($ids);
        }
    }

    public function clearDeleted(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("DELETE FROM english_words WHERE id IN ($ph)")->execute($ids);
        }
    }

    private static function hydrate(array $r): EnglishWord {
        $m = new EnglishWord();
        $m->id = $r['id'];
        $m->word = $r['word'];
        $m->phonetic = $r['phonetic'];
        $m->meaning = $r['meaning'];
        $m->example = $r['example'];
        $m->familiarity = (int)$r['familiarity'];
        $m->nextReview = (int)$r['next_review'];
        $m->lastModified = (int)$r['last_modified'];
        $m->isDeleted = (int)$r['is_deleted'];
        $m->deviceId = $r['device_id'];
        $m->needsSync = (int)$r['needs_sync'];
        return $m;
    }
}
