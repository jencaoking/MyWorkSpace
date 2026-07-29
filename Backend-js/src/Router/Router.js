// 路由：与原 PHP 版一致，采用「方法 + 精确路径」匹配（无路径参数）。
// 通过内部 express.Router 实现，dispatch() 返回可被 app.use 挂载的路由器。
const express = require('express');

class Router {
  constructor() {
    this.router = express.Router();
  }

  add(method, path, handler) {
    this.router[method.toLowerCase()](path, handler);
  }

  dispatch() {
    return this.router;
  }
}

module.exports = { Router };
