<?php
namespace App\Exception;

/** 业务异常：携带 HTTP 状态码，由前端控制器统一转成错误响应 */
final class ApiException extends \Exception
{
    public function __construct(string $message, public int $httpCode = 400, int $code = 1)
    {
        parent::__construct($message, $code);
    }
}
