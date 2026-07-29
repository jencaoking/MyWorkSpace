// 笔记图片上传 Controller：接收 multipart 图片，落盘到 uploads/notes/ 并返回可访问 URL。
// 对应 PHP App\Controller\NoteImageController。multer 的 upload.single('file') 在 routes 中装配。
const Response = require('../../lib/Response');
const { ApiException } = require('../../src/Exception/ApiException');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

const ALLOWED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/jpg'];
const MAX_SIZE = 10 * 1024 * 1024; // 10MB

class NoteImageController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
  }

  deviceId() {
    const id = (this.req.headers['x-device-id'] || '').toString().trim();
    if (!id) throw new ApiException('X-Device-ID header required', 400, 400);
    return id;
  }

  publicBase() {
    const proto = this.req.secure ? 'https' : 'http';
    const host = this.req.headers.host || 'localhost';
    return `${proto}://${host}`;
  }

  async upload() {
    this.deviceId(); // 鉴权：必须带上设备 ID

    const file = this.req.file;
    if (!file) {
      Response.error(this.res, '缺少上传文件', 400, 400);
      return;
    }
    if (file.size > MAX_SIZE) {
      Response.error(this.res, '图片过大，最大 10MB', 400, 400);
      return;
    }
    const mime = file.mimetype;
    if (!ALLOWED.includes(mime)) {
      Response.error(this.res, '仅支持图片格式（jpg / png / gif / webp）', 400, 400);
      return;
    }

    const ext = mime === 'image/png' ? 'png' : mime === 'image/gif' ? 'gif' : mime === 'image/webp' ? 'webp' : 'jpg';
    const uploadDir = path.join(__dirname, '..', '..', 'uploads', 'notes');
    fs.mkdirSync(uploadDir, { recursive: true });

    const name = crypto.randomBytes(16).toString('hex') + '.' + ext;
    const dest = path.join(uploadDir, name);
    fs.copyFileSync(file.path, dest);
    fs.unlinkSync(file.path); // 删除 multer 临时文件

    const p = '/uploads/notes/' + name;
    const url = this.publicBase() + p;

    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        url,
        path: p,
        name,
        size: file.size,
      },
    });
  }
}

module.exports = { NoteImageController, ALLOWED, MAX_SIZE };
