// 笔记视图模型，对应 PHP App\ViewModel\NoteViewModel
const { ApiException } = require('../../src/Exception/ApiException');
const { Note } = require('../Model/Note');

function fromUploadArray(raw, deviceId) {
  if (!raw || !raw.id) {
    throw new ApiException('note.id is required', 400, 400);
  }
  const now = Date.now();
  return new Note({
    id: String(raw.id),
    title: raw.title !== undefined ? String(raw.title) : '',
    content: raw.content ?? null,
    isPinned: parseInt(raw.isPinned ?? raw.is_pinned ?? 0, 10),
    isFavorite: parseInt(raw.isFavorite ?? raw.is_favorite ?? 0, 10),
    createdAt: parseInt(raw.createdAt ?? raw.created_at ?? now, 10),
    updatedAt: parseInt(raw.updatedAt ?? raw.updated_at ?? now, 10),
    lastModified: now,
    isDeleted: parseInt(raw.isDeleted ?? raw.is_deleted ?? 0, 10),
    deviceId,
    needsSync: 0,
  });
}

function toApiArray(note) {
  return note.toArray();
}

module.exports = { fromUploadArray, toApiArray };
