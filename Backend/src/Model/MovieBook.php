<?php
namespace App\Model;

/** 影音书籍模型（对应 movie_books 表）。tmdb_id 以字符串存储。 */
class MovieBook {
    public ?string $id;
    public ?string $type;
    public ?string $title;
    public ?string $tmdbId;
    public ?string $status;
    public ?float $rating;
    public ?string $posterUrl;
    public ?string $note;
    public ?int $lastModified;
    public ?int $isDeleted;
    public ?string $deviceId;
    public ?int $needsSync;

    public static function fromUploadArray(array $a): self {
        $m = new self();
        $m->id = $a['id'] ?? null;
        $m->type = $a['type'] ?? 'movie';
        $m->title = $a['title'] ?? '';
        $m->tmdbId = isset($a['tmdb_id']) && $a['tmdb_id'] !== '' ? (string)$a['tmdb_id'] : null;
        $m->status = $a['status'] ?? 'want';
        $m->rating = isset($a['rating']) && $a['rating'] !== '' ? (float)$a['rating'] : null;
        $m->posterUrl = $a['poster_url'] ?? null;
        $m->note = $a['note'] ?? '';
        $m->lastModified = isset($a['last_modified']) ? (int)$a['last_modified'] : (int)(microtime(true) * 1000);
        $m->isDeleted = isset($a['is_deleted']) ? (int)$a['is_deleted'] : 0;
        $m->deviceId = $a['device_id'] ?? null;
        $m->needsSync = isset($a['needs_sync']) ? (int)$a['needs_sync'] : 0;
        return $m;
    }

    public function toApiArray(): array {
        return [
            'id' => $this->id,
            'type' => $this->type,
            'title' => $this->title,
            'tmdb_id' => $this->tmdbId,
            'status' => $this->status,
            'rating' => $this->rating,
            'poster_url' => $this->posterUrl,
            'note' => $this->note,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
