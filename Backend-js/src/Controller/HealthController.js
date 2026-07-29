// 健康检查 Controller：探活数据库连通性。对应 PHP App\Controller\HealthController
const Response = require('../../lib/Response');

class HealthController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
  }

  async index() {
    let dbOk = true;
    let dbError = '';
    if (!this.db) {
      dbOk = false;
      dbError = 'database not configured or unreachable';
    } else {
      try {
        await this.db.query('SELECT 1');
      } catch (e) {
        dbOk = false;
        dbError = e.message;
      }
    }

    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        server_time: Date.now(),
        app_version: '1.0',
        runtime: process.version,
        db_connected: dbOk,
        db_error: dbError,
      },
    });
  }
}

module.exports = { HealthController };
