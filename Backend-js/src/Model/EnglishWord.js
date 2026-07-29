// 英语单词模型（english_words 表），对应 PHP App\Model\EnglishWord
class EnglishWord {
  constructor({
    id,
    word,
    phonetic,
    meaning,
    example,
    familiarity,
    nextReview,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.word = word;
    this.phonetic = phonetic;
    this.meaning = meaning;
    this.example = example;
    this.familiarity = familiarity;
    this.nextReview = nextReview;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  toApiArray() {
    return {
      id: this.id,
      word: this.word,
      phonetic: this.phonetic,
      meaning: this.meaning,
      example: this.example,
      familiarity: this.familiarity,
      next_review: this.nextReview,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }

  static fromUploadArray(a) {
    const now = Date.now();
    return new EnglishWord({
      id: a.id ?? null,
      word: a.word ?? '',
      phonetic: a.phonetic ?? '',
      meaning: a.meaning ?? '',
      example: a.example ?? '',
      familiarity: a.familiarity !== undefined ? parseInt(a.familiarity, 10) : 0,
      nextReview: a.next_review !== undefined ? parseInt(a.next_review, 10) : 0,
      lastModified: a.last_modified !== undefined ? parseInt(a.last_modified, 10) : now,
      isDeleted: a.is_deleted !== undefined ? parseInt(a.is_deleted, 10) : 0,
      deviceId: a.device_id ?? null,
      needsSync: a.needs_sync !== undefined ? parseInt(a.needs_sync, 10) : 0,
    });
  }
}

module.exports = { EnglishWord };
