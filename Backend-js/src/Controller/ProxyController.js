// 第三方 API 代理：密钥仅服务端持有（后台管理填写），客户端调用 /api/proxy/* 避免密钥下发。
// 对应 PHP App\Controller\ProxyController。支持有道翻译、TMDB 搜索、和风天气。
const https = require('https');
const http = require('http');
const crypto = require('crypto');
const ApiResponse = require('../../lib/ApiResponse');
const { ConfigRepository } = require('../Repository/ConfigRepository');

const YOUDAO_QWEATHER = {
  key: process.env.YOUDAO_APP_KEY || '',
  secret: process.env.YOUDAO_APP_SECRET || '',
};

function httpGet(url, headers = {}) {
  return new Promise((resolve) => {
    const lib = url.startsWith('https') ? https : http;
    const req = lib.get(
      url,
      { headers: Object.assign({ 'User-Agent': 'MyWorkProxy/1.0' }, headers) },
      (res) => {
        let data = '';
        res.on('data', (c) => (data += c));
        res.on('end', () => resolve(data));
      }
    );
    req.on('error', () => resolve(false));
    req.setTimeout(8000, () => {
      req.destroy();
      resolve(false);
    });
  });
}

class ProxyController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.config = new ConfigRepository(db);
  }

  getQuery() {
    return this.req.query;
  }

  async translate() {
    const q = this.getQuery();
    const text = String(q.text ?? '').trim();
    const from = String(q.from ?? 'auto');
    const to = String(q.to ?? 'zh-CHS');
    if (text === '') {
      ApiResponse.json(this.res, { code: 1, message: '缺少参数 text', data: null });
      return;
    }
    const data = await this.callYoudao(text, from, to);
    if (data === null) {
      ApiResponse.json(this.res, { code: 2, message: '未配置有道 API 密钥（请在后台管理填写）或请求失败', data: null });
      return;
    }
    if (!empty(data.errorCode) && String(data.errorCode) !== '0') {
      ApiResponse.json(this.res, { code: 3, message: '有道 API 返回错误：' + data.errorCode, data });
      return;
    }
    const translation = Array.isArray(data.translation) ? data.translation.join('') : '';
    ApiResponse.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        query: text,
        from,
        to,
        translation,
        speak_url: data.speakUrl ?? '',
        t_speak_url: data.tSpeakUrl ?? '',
      },
    });
  }

  async word() {
    const q = this.getQuery();
    const text = String(q.text ?? '').trim();
    if (text === '') {
      ApiResponse.json(this.res, { code: 1, message: '缺少参数 text', data: null });
      return;
    }
    const data = await this.callYoudao(text, 'auto', 'auto');
    if (data === null) {
      ApiResponse.json(this.res, { code: 2, message: '未配置有道 API 密钥（请在后台管理填写）或请求失败', data: null });
      return;
    }
    if (!empty(data.errorCode) && String(data.errorCode) !== '0') {
      ApiResponse.json(this.res, { code: 3, message: '有道 API 返回错误：' + data.errorCode, data });
      return;
    }
    const basic = data.basic ?? {};
    const explains = basic.explains ?? [];
    const examples = [];
    if (Array.isArray(data.web)) {
      for (const w of data.web) {
        examples.push({ source: w.key ?? '', target: (w.value ?? []).join('；') });
      }
    }
    ApiResponse.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        word: text,
        phonetic: basic.phonetic ?? '',
        phonetic_us: basic['uk-phonetic'] ?? '',
        phonetic_uk: basic['us-phonetic'] ?? '',
        explains,
        translation: data.translation ?? [],
        speak_url: data.speakUrl ?? (basic['uk-speech'] ?? ''),
        t_speak_url: data.tSpeakUrl ?? (basic['us-speech'] ?? ''),
        examples,
      },
    });
  }

  async searchTmdb() {
    const q = this.getQuery();
    const query = String(q.query ?? '').trim();
    const page = Math.max(1, parseInt(q.page ?? 1, 10));
    if (query === '') {
      ApiResponse.json(this.res, { code: 1, message: '缺少 query 参数', data: null }, 400);
      return;
    }
    const apiKey = await this.tmdbKey();
    if (apiKey === '') {
      ApiResponse.json(this.res, { code: 1, message: '服务端未配置 TMDB API Key（请在后台管理填写）', data: null }, 412);
      return;
    }
    const url =
      'https://api.themoviedb.org/3/search/multi?api_key=' + encodeURIComponent(apiKey) +
      '&language=zh-CN&include_adult=false&page=' + page +
      '&query=' + encodeURIComponent(query);
    const raw = await httpGet(url);
    if (raw === false || raw === '') {
      ApiResponse.json(this.res, { code: 1, message: 'TMDB 请求失败（网络或密钥无效）', data: null }, 502);
      return;
    }
    let json;
    try {
      json = JSON.parse(raw);
    } catch (_) {
      json = null;
    }
    if (!json || !json.results) {
      ApiResponse.json(this.res, { code: 1, message: 'TMDB 返回异常', data: null }, 502);
      return;
    }
    const results = [];
    for (const r of json.results) {
      const type = String(r.media_type ?? '');
      if (type === 'person') continue;
      const poster = r.poster_path ? 'https://image.tmdb.org/t/p/w342' + r.poster_path : '';
      let title;
      let original;
      let release;
      if (type === 'tv') {
        title = String(r.name ?? r.original_name ?? '');
        original = String(r.original_name ?? r.name ?? '');
        release = String(r.first_air_date ?? '');
      } else {
        title = String(r.title ?? r.original_title ?? '');
        original = String(r.original_title ?? r.title ?? '');
        release = String(r.release_date ?? '');
      }
      results.push({
        tmdb_id: parseInt(r.id ?? 0, 10),
        media_type: type,
        title,
        original_title: original,
        overview: String(r.overview ?? ''),
        poster_url: poster,
        release_date: release,
        vote_average: r.vote_average !== undefined ? parseFloat(r.vote_average) : 0.0,
      });
    }
    ApiResponse.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        query,
        page: parseInt(json.page ?? page, 10),
        total_results: parseInt(json.total_results ?? results.length, 10),
        total_pages: parseInt(json.total_pages ?? 1, 10),
        results,
      },
    });
  }

  async tmdbKey() {
    const cfg = await this.config.get('tmdb_key', '');
    if (cfg !== '') return cfg;
    return process.env.TMDB_KEY || '';
  }

  async keys() {
    let key = await this.config.get('youdao_app_key', '');
    let sec = await this.config.get('youdao_app_secret', '');
    if ((key === '' || sec === '') && (YOUDAO_QWEATHER.key || YOUDAO_QWEATHER.secret)) {
      if (key === '' && YOUDAO_QWEATHER.key) key = YOUDAO_QWEATHER.key;
      if (sec === '' && YOUDAO_QWEATHER.secret) sec = YOUDAO_QWEATHER.secret;
    }
    return [key, sec];
  }

  async callYoudao(q, from, to) {
    const [appKey, secret] = await this.keys();
    if (appKey === '' || secret === '') return null;
    const salt = uniqid();
    const curtime = String(Math.floor(Date.now() / 1000));
    const len = [...q].length;
    const input = len > 20 ? [...q].slice(0, 10).join('') + len + [...q].slice(-10).join('') : q;
    const sign = crypto.createHash('sha256').update(appKey + input + salt + curtime + secret).digest('hex');
    const params = new URLSearchParams({
      q,
      from,
      to,
      appKey,
      salt,
      sign,
      signType: 'v3',
      curtime,
    });
    const url = 'https://openapi.youdao.com/api?' + params.toString();
    const json = await httpGet(url);
    if (json === false) return null;
    try {
      const decoded = JSON.parse(json);
      return typeof decoded === 'object' ? decoded : null;
    } catch (_) {
      return null;
    }
  }

  async weatherNow() {
    const q = this.getQuery();
    const location = String(q.location ?? '').trim();
    if (location === '') {
      ApiResponse.json(this.res, { code: 1, message: '缺少 location 参数', data: null }, 400);
      return;
    }
    const [key, token, host] = await this.qweatherConfig();
    if (key === '' && token === '') {
      ApiResponse.json(this.res, { code: 1, message: '服务端未配置和风天气密钥（请在后台管理填写 qweather_key 或 qweather_token）', data: null }, 412);
      return;
    }
    const data = await this.callQweather(host, '/v7/weather/now', { location, lang: 'zh' }, key, token);
    this.respondQweather(data);
  }

  async weather7d() {
    const q = this.getQuery();
    const location = String(q.location ?? '').trim();
    if (location === '') {
      ApiResponse.json(this.res, { code: 1, message: '缺少 location 参数', data: null }, 400);
      return;
    }
    const [key, token, host] = await this.qweatherConfig();
    if (key === '' && token === '') {
      ApiResponse.json(this.res, { code: 1, message: '服务端未配置和风天气密钥', data: null }, 412);
      return;
    }
    const data = await this.callQweather(host, '/v7/weather/7d', { location, lang: 'zh' }, key, token);
    this.respondQweather(data);
  }

  async cityLookup() {
    const q = this.getQuery();
    const keyword = String(q.keyword ?? '').trim();
    if (keyword === '') {
      ApiResponse.json(this.res, { code: 1, message: '缺少 keyword 参数', data: null }, 400);
      return;
    }
    const [key, token, host] = await this.qweatherConfig();
    if (key === '' && token === '') {
      ApiResponse.json(this.res, { code: 1, message: '服务端未配置和风天气密钥', data: null }, 412);
      return;
    }
    const data = await this.callQweather('geoapi.qweather.com', '/geo/v2/city/lookup', {
      location: keyword, range: 'cn', number: '20', lang: 'zh',
    }, key, token);
    this.respondQweather(data);
  }

  async qweatherConfig() {
    const key = await this.config.get('qweather_key', '');
    const token = await this.config.get('qweather_token', '');
    const host = await this.config.get('qweather_host', 'devapi.qweather.com');
    return [key, token, host];
  }

  async callQweather(host, path, params, key, token) {
    const url = 'https://' + host + path + '?' + new URLSearchParams(params).toString();
    let finalUrl = url;
    if (token === '' && key !== '') {
      finalUrl = url + '&key=' + encodeURIComponent(key);
    }
    const headers = {};
    if (token !== '') headers.Authorization = 'Bearer ' + token;
    const raw = await httpGet(finalUrl, headers);
    if (raw === false || raw === '') return null;
    try {
      return JSON.parse(raw);
    } catch (_) {
      return null;
    }
  }

  respondQweather(data) {
    if (data === null) {
      ApiResponse.json(this.res, { code: 1, message: '和风天气请求失败（网络或密钥无效）', data: null }, 502);
      return;
    }
    if (String(data.code ?? '') !== '200') {
      ApiResponse.json(this.res, { code: 1, message: '和风天气返回错误：' + (data.code ?? 'unknown'), data }, 502);
      return;
    }
    ApiResponse.json(this.res, { code: 0, message: 'ok', data });
  }
}

function empty(v) {
  return v === undefined || v === null || v === '' || v === 0 || (Array.isArray(v) && v.length === 0);
}

function uniqid() {
  return crypto.randomBytes(8).toString('hex') + Date.now().toString(16);
}

module.exports = { ProxyController };
