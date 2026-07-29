<?php
namespace App\Router;

/** 极简路由器：把 HTTP 请求（method + path）映射到 Controller 动作（MVVM 中的调度器） */
final class Router
{
    /** @var array<string, callable(): void> */
    private array $routes = [];

    public function add(string $method, string $path, callable $handler): void
    {
        $this->routes[$method . ' ' . $path] = $handler;
    }

    public function dispatch(string $method, string $path): void
    {
        $key = $method . ' ' . $path;
        if (!isset($this->routes[$key])) {
            \Response::error("Not Found: $key", 404, 404);
        }
        ($this->routes[$key])();
    }
}
