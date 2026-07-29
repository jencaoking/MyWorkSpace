<?php
namespace App\Controller;

use App\Exception\ApiException;
use PDO;

/** 笔记图片上传 Controller：接收 multipart 图片，落盘到 uploads/notes/ 并返回可访问 URL */
final class NoteImageController
{
    private const MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private const ALLOWED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/jpg'];

    public function __construct(private PDO $pdo) {}

    private function deviceId(): string
    {
        $deviceId = $_SERVER['HTTP_X_DEVICE_ID'] ?? '';
        if ($deviceId === '') {
            throw new ApiException('X-Device-ID header required', 400, 400);
        }
        return $deviceId;
    }

    /** 根据请求脚本路径推导站点公共基础 URL（兼容子目录部署，如 /MyWorkApi/server） */
    private function publicBase(): string
    {
        $proto = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        $script = $_SERVER['SCRIPT_NAME'] ?? '/index.php';
        $prefix = rtrim(dirname($script), '/');
        return $proto . '://' . $host . $prefix;
    }

    /** POST /api/notes/image —— 图片上传，返回 { url, path, name, size } */
    public function upload(): void
    {
        $this->deviceId(); // 鉴权：必须带上设备 ID

        if (empty($_FILES['file'])) {
            \Response::error('缺少上传文件', 400, 400);
        }

        $file = $_FILES['file'];
        if ($file['error'] !== UPLOAD_ERR_OK) {
            \Response::error('文件上传失败，错误码：' . $file['error'], 400, 400);
        }

        $tmp = $file['tmp_name'];
        if (!is_uploaded_file($tmp)) {
            \Response::error('非法上传', 400, 400);
        }

        $finfo = new \finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($tmp);
        if (!in_array($mime, self::ALLOWED, true)) {
            \Response::error('仅支持图片格式（jpg / png / gif / webp）', 400, 400);
        }

        if ($file['size'] > self::MAX_SIZE) {
            \Response::error('图片过大，最大 10MB', 400, 400);
        }

        $ext = match ($mime) {
            'image/png' => 'png',
            'image/gif' => 'gif',
            'image/webp' => 'webp',
            default => 'jpg',
        };

        $uploadDir = __DIR__ . '/../../uploads/notes';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0755, true);
        }

        $name = bin2hex(random_bytes(16)) . '.' . $ext;
        $dest = $uploadDir . '/' . $name;
        if (!move_uploaded_file($tmp, $dest)) {
            \Response::error('保存失败', 500, 500);
        }

        $path = '/uploads/notes/' . $name;
        $url = $this->publicBase() . $path;

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'url' => $url,
                'path' => $path,
                'name' => $name,
                'size' => $file['size'],
            ],
        ]);
    }
}
