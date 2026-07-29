<?php
echo "=== 删除保护白名单（静态，无需 DB） ===\n";
require __DIR__ . '/src/Exception/ApiException.php';
require __DIR__ . '/src/Repository/AdminRepository.php';
use App\Repository\AdminRepository;
echo "user_settings canDelete=" . var_export(AdminRepository::canDelete('user_settings'), true) . "\n";
echo "tasks canDelete=" . var_export(AdminRepository::canDelete('tasks'), true) . "\n";
echo "categories canDelete=" . var_export(AdminRepository::canDelete('categories'), true) . "\n";

echo "\n=== HTTP 鉴权 + 删除保护守卫（无需 DB） ===\n";
function http($method, $path, $body, &$cookie) {
    $ch = curl_init('http://127.0.0.1:8899' . $path);
    $h = [];
    if ($cookie) $h[] = 'Cookie: ' . $cookie;
    if ($body !== null) {
        $h[] = 'Content-Type: application/json';
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
    }
    curl_setopt_array($ch, [CURLOPT_RETURNTRANSFER => true, CURLOPT_HEADER => true, CURLOPT_HTTPHEADER => $h]);
    $r = curl_exec($ch);
    $hs = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
    $head = substr($r, 0, $hs);
    if (preg_match('/Set-Cookie:\s*([^;]+)/i', $head, $m)) $cookie = $m[1];
    curl_close($ch);
    return [$head, substr($r, $hs)];
}
$cookie = '';
[$h, $b] = http('POST', '/admin/login', json_encode(['password' => 'wrong']), $cookie);
echo "wrong password -> " . trim($b) . "\n";
[$h, $b] = http('POST', '/admin/login', json_encode(['password' => 'admin123']), $cookie);
echo "right password -> " . trim($b) . " | cookie=" . ($cookie ? 'set' : 'none') . "\n";
[$h, $b] = http('POST', '/admin/delete', json_encode(['table' => 'user_settings', 'id' => 'x']), $cookie);
echo "delete user_settings (protected) -> " . trim($b) . "\n";
[$h, $b] = http('POST', '/admin/delete', json_encode(['table' => 'tasks', 'id' => 'x']), $cookie);
echo "delete tasks (allowed, no such id) -> " . trim($b) . "\n";
