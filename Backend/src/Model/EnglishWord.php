<?php
namespace App\Model;

/** 英语单词模型（对应 english_words 表）。 */
class EnglishWord {
    public ?string $id;
    public ?string $word;
    public ?string $phonetic;
    public ?string $meaning;
    public ?string $example;
    public ?int $familiarity;
    public ?int $nextReview;
    public ?int $lastModified;
    public ?int $isDeleted;
    public ?string $deviceId;
    public ?int $needsSync;

    public static function fromUploadArray(array $a): self {
        $m = new self();
        $m->id = $a['id'] ?? null;
        $m->word = $a['word'] ?? '';
        $m->phonetic = $a['phonetic'] ?? '';
        $m->meaning = $a['meaning'] ?? '';
        $m->example = $a['example'] ?? '';
        $m->familiarity = isset($a['familiarity']) ? (int)$a['familiarity'] : 0;
        $m->nextReview = isset($a['next_review']) ? (int)$a['next_review'] : 0;
        $m->lastModified = isset($a['last_modified']) ? (int)$a['last_modified'] : (int)(microtime(true) * 1000);
        $m->isDeleted = isset($a['is_deleted']) ? (int)$a['is_deleted'] : 0;
        $m->deviceId = $a['device_id'] ?? null;
        $m->needsSync = isset($a['needs_sync']) ? (int)$a['needs_sync'] : 0;
        return $m;
    }

    public function toApiArray(): array {
        return [
            'id' => $this->id,
            'word' => $this->word,
            'phonetic' => $this->phonetic,
            'meaning' => $this->meaning,
            'example' => $this->example,
            'familiarity' => $this->familiarity,
            'next_review' => $this->nextReview,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
