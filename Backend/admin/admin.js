'use strict';

/* ---------- 导航定义（概览 + 9 张白名单表） ---------- */
const NAV = [
  { key: 'overview',        label: '概览',     ico: '◎' },
  { key: 'tasks',           label: '任务',     ico: '✓' },
  { key: 'notes',           label: '笔记',     ico: '✎' },
  { key: 'sport_records',   label: '运动',     ico: '♥' },
  { key: 'english_words',   label: '单词',     ico: 'A' },
  { key: 'movie_books',     label: '影音书籍', ico: '▦' },
  { key: 'health_records',  label: '健康',     ico: '+' },
  { key: 'account_records', label: '记账',     ico: '¥' },
  { key: 'categories',      label: '分类',     ico: '#' },
  { key: 'user_settings',   label: '设置',     ico: '⚙' },
  { key: 'users',           label: '用户',     ico: '◉' },
  { key: 'audit',           label: '审计日志', ico: '✦' },
];
const LABEL = Object.fromEntries(NAV.map(n => [n.key, n.label]));

/* ---------- 全局状态 ---------- */
const S = { view: 'overview', table: '', rows: [], columns: [], types: {}, deletable: true, limit: 50, offset: 0, total: 0, userQ: '' };

/* ---------- DOM 引用 ---------- */
const $ = id => document.getElementById(id);
const loginScreen = $('loginScreen'), appEl = $('app'), navEl = $('nav'), contentEl = $('content');

/* ---------- 工具函数 ---------- */
function esc(s) {
  return String(s).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));
}
function isTs(c) {
  return ['created_at', 'updated_at', 'completed_at', 'last_modified', 'last_sync_at', 'timestamp', 'synced_at'].includes(c) || c.endsWith('_at');
}
function isText(c) {
  return ['content', 'note', 'notes', 'description', 'remark', 'synopsis', 'meaning', 'detail', 'summary', 'url', 'cover'].includes(c);
}
function labelOf(c) {
  return ({
    id: 'ID', title: '标题', content: '内容', note: '笔记', notes: '备注', description: '描述',
    created_at: '创建时间', updated_at: '更新时间', completed_at: '完成时间', last_modified: '最后修改',
    last_sync_at: '同步时间', timestamp: '时间戳', is_deleted: '已删除', device_id: '设备', priority: '优先级',
    status: '状态', task_type: '类型', needs_sync: '待同步', category_id: '分类', amount: '金额',
    record_date: '日期', word: '单词', book_name: '书名', name: '名称', type: '类型', synopsis: '简介',
  })[c] || c;
}
function fmtTs(v) {
  if (v === null || v === '' || v === undefined) return '—';
  const n = Number(v);
  if (isNaN(n) || n <= 0) return esc(String(v));
  const ms = n > 1e11 ? n : n * 1000;
  return new Date(ms).toLocaleString('zh-CN', { hour12: false });
}

async function api(path, opts = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...opts,
  });
  let json = {};
  try { json = await res.json(); } catch (e) { /* ignore */ }
  if (!res.ok || (json.code !== 0 && json.code !== undefined)) {
    const err = new Error(json.message || ('请求失败 (' + res.status + ')'));
    err.status = res.status;
    throw err;
  }
  return json.data !== undefined ? json.data : json;
}

let toastTimer = null;
function toast(msg, isErr = false) {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'toast' + (isErr ? ' err' : '');
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.hidden = true; }, 2600);
}

function openModal(id) { $(id).hidden = false; }
function closeModal(id) { $(id).hidden = true; }

/* ---------- 鉴权流程 ---------- */
function showLogin() { loginScreen.hidden = false; appEl.hidden = true; }
function enterApp() {
  loginScreen.hidden = true;
  appEl.hidden = false;
  renderSidebar();
  navigate(S.view || 'overview');
}

async function boot() {
  try {
    await api('/admin/overview');
    enterApp();
  } catch (e) {
    showLogin();
    if (e.status && e.status !== 401) toast('无法连接后台服务：' + e.message, true);
  }
}

$('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const pwd = $('loginPwd').value;
  const btn = $('loginBtn'), err = $('loginErr');
  btn.disabled = true; err.hidden = true;
  try {
    await api('/admin/login', { method: 'POST', body: JSON.stringify({ password: pwd }) });
    $('loginPwd').value = '';
    enterApp();
  } catch (e2) {
    err.textContent = e2.message;
    err.hidden = false;
  } finally { btn.disabled = false; }
});

$('logoutBtn').addEventListener('click', async () => {
  try { await api('/admin/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
  S.view = 'overview';
  showLogin();
});

/* ---------- 导航与渲染 ---------- */
function renderSidebar() {
  navEl.innerHTML = '';
  NAV.forEach(item => {
    const b = document.createElement('button');
    b.className = 'nav-item' + (item.key === S.view ? ' active' : '');
    b.dataset.key = item.key;
    b.innerHTML = `<span class="nav-ico">${item.ico}</span><span>${item.label}</span>`;
    b.onclick = () => { navigate(item.key); appEl.classList.remove('nav-open'); };
    navEl.appendChild(b);
  });
}

function navigate(key) {
  S.view = key;
  document.querySelectorAll('.nav-item').forEach(el => el.classList.toggle('active', el.dataset.key === key));
  $('viewTitle').textContent = LABEL[key] || '概览';
  if (key === 'overview') showOverview();
  else if (key === 'audit') showAudit();
  else if (key === 'apikeys') showApiKeys();
  else if (key === 'users') showUsers();
  else showTable(key);
}

async function showApiKeys() {
  contentEl.innerHTML = '<div class="empty">加载中…</div>';
  try {
    const d = await api('/admin/apikeys');
    const keys = (d.keys) || {};
    const wrap = document.createElement('div');
    wrap.className = 'form-wrap glass';
    wrap.innerHTML = `
      <h3>第三方 API 密钥</h3>
      <p class="hint">密钥仅保存在服务端，App 通过后台代理（/api/proxy/*）调用，不会下发到客户端。当前支持有道智云翻译与 TMDB 影视检索。</p>
      <div class="field"><label>有道 App Key</label><input id="yk" type="text" placeholder="youdao_app_key" /></div>
      <div class="field"><label>有道 App Secret</label><input id="ys" type="password" placeholder="youdao_app_secret" /></div>
      <div class="field"><label>TMDB API Key（v3 auth）</label><input id="tk" type="text" placeholder="tmdb_key" /></div>
      <div class="row-actions">
        <button id="saveKeys" class="btn-primary">保存密钥</button>
        <button id="testKeys" class="btn">测试有道</button>
        <button id="testTmdb" class="btn">测试 TMDB</button>
        <span id="keyState" class="hint"></span>
      </div>`;
    contentEl.innerHTML = '';
    contentEl.appendChild(wrap);
    const states = [];
    if (keys.youdao_app_key) states.push('有道 ' + keys.youdao_app_key);
    if (keys.tmdb_key) states.push('TMDB ' + keys.tmdb_key);
    if (states.length) $('keyState').textContent = '已配置：' + states.join(' ｜ ');
    $('saveKeys').onclick = async () => {
      const btn = $('saveKeys'); btn.disabled = true;
      try {
        await api('/admin/apikeys', {
          method: 'POST',
          body: JSON.stringify({
            youdao_app_key: $('yk').value.trim(),
            youdao_app_secret: $('ys').value.trim(),
            tmdb_key: $('tk').value.trim(),
          }),
        });
        toast('已保存');
        showApiKeys();
      } catch (e) {
        toast('保存失败：' + e.message, true);
      } finally { btn.disabled = false; }
    };
    $('testKeys').onclick = async () => {
      const btn = $('testKeys'); btn.disabled = true;
      try {
        const r = await api('/api/proxy/translate?text=hello&to=en');
        toast('连接成功：' + (r.translation || ''));
      } catch (e) {
        toast('测试失败：' + e.message, true);
      } finally { btn.disabled = false; }
    };
    $('testTmdb').onclick = async () => {
      const btn = $('testTmdb'); btn.disabled = true;
      try {
        const r = await api('/api/proxy/tmdb/search?query=batman');
        toast('连接成功：共 ' + (r.total_results || 0) + ' 条结果');
      } catch (e) {
        toast('TMDB 测试失败：' + e.message, true);
      } finally { btn.disabled = false; }
    };
  } catch (e) {
    contentEl.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
  }
}

function setConn(ok, ver) {
  const dot = $('connStatus');
  dot.className = 'status-dot ' + (ok ? 'ok' : 'bad');
  $('connText').textContent = ok ? ('已连接 · PHP ' + ver) : '连接失败';
}

async function showOverview() {
  contentEl.innerHTML = '<div class="empty">加载中…</div>';
  try {
    const d = await api('/admin/overview');
    renderOverview(d);
    setConn(d.db_connected, d.php_version);
  } catch (e) {
    contentEl.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
  }
}

function renderOverview(d) {
  const max = Math.max(1, ...Object.values(d.tables));
  const grid = document.createElement('div');
  grid.className = 'stat-grid';
  Object.keys(d.tables).forEach(t => {
    const val = d.tables[t];
    const card = document.createElement('div');
    card.className = 'stat-card glass';
    card.innerHTML = `<h3>${LABEL[t] || t}</h3><div class="num">${val}</div>` +
      `<div class="bar"><span style="width:${Math.round(val / max * 100)}%"></span></div>`;
    grid.appendChild(card);
  });
  const sys = document.createElement('div');
  sys.className = 'sys-row';
  sys.innerHTML =
    `<span class="sys-chip">PHP <b>${esc(d.php_version)}</b></span>` +
    `<span class="sys-chip">数据库 <b>${d.db_connected ? '已连接' : '异常'}</b></span>` +
    `<span class="sys-chip">设备数 <b>${d.device_count}</b></span>` +
    `<span class="sys-chip">服务器时间 <b>${fmtTs(d.server_time)}</b></span>`;
  contentEl.innerHTML = '';
  contentEl.append(sys, grid);
}

async function showTable(table, resetOffset = true) {
  S.view = table; S.table = table; S.rows = []; S.columns = []; S.types = {};
  if (resetOffset) S.offset = 0;
  contentEl.innerHTML = '<div class="empty">加载中…</div>';
  try {
    const d = await api('/admin/browse?table=' + encodeURIComponent(table) +
      '&limit=' + S.limit + '&offset=' + S.offset);
    S.rows = d.rows; S.columns = d.columns; S.types = d.types || {};
    S.deletable = (d.deletable !== false);
    S.total = (d.total != null) ? d.total : d.rows.length;
    renderTable();
  } catch (e) {
    contentEl.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
  }
}

/* 翻页：保留当前表与筛选，按页大小前进/后退 */
function gotoPage(delta) {
  const target = S.offset + delta * S.limit;
  if (target < 0 || target >= S.total) return;
  S.offset = target;
  showTable(S.table, false);
}

function cellHTML(col, v) {
  if (v === null || v === '') return '<span style="color:var(--ink-faint)">—</span>';
  if (col === 'is_deleted') return `<span class="badge deleted-${v}">${v == 1 ? '已删除' : '正常'}</span>`;
  if (col === 'priority') return `<span class="badge prio-${v}">${v == 3 ? '高' : v == 2 ? '中' : '低'}</span>`;
  if (col === 'status' && S.table === 'tasks') return `<span class="badge status-${v}">${v == 2 ? '已完成' : v == 1 ? '进行中' : '待办'}</span>`;
  if (isTs(col)) return esc(fmtTs(v));
  if (isText(col)) return `<div class="cell-text">${esc(v)}</div>`;
  return esc(v);
}

function renderTable() {
  const cols = S.columns;
  const wrap = document.createElement('div');
  wrap.className = 'table-wrap';

  const tb = document.createElement('div');
  tb.className = 'table-toolbar';
  const search = document.createElement('input');
  search.placeholder = '在当前结果中筛选…';
  const meta = document.createElement('span');
  meta.className = 'table-meta';
  const from = S.total === 0 ? 0 : S.offset + 1;
  const to = Math.min(S.offset + S.rows.length, S.total);
  meta.textContent = `共 ${S.total} 行 · 显示 ${from}–${to}`;
  const prev = document.createElement('button');
  prev.className = 'btn-ghost'; prev.textContent = '上一页';
  prev.disabled = S.offset <= 0;
  prev.onclick = () => gotoPage(-1);
  const next = document.createElement('button');
  next.className = 'btn-ghost'; next.textContent = '下一页';
  next.disabled = S.offset + S.rows.length >= S.total;
  next.onclick = () => gotoPage(1);
  tb.append(search, meta, prev, next);

  const scroll = document.createElement('div');
  scroll.className = 'table-scroll';
  const table = document.createElement('table');
  table.className = 'data';
  const thead = document.createElement('thead');
  const htr = document.createElement('tr');
  cols.forEach(c => {
    const th = document.createElement('th');
    th.textContent = labelOf(c);
    htr.appendChild(th);
  });
  const actionTh = document.createElement('th');
  actionTh.textContent = '操作';
  actionTh.className = 'col-actions';
  htr.appendChild(actionTh);
  thead.appendChild(htr);

  const tbody = document.createElement('tbody');
  S.rows.forEach(row => {
    const tr = document.createElement('tr');
    cols.forEach(c => {
      const td = document.createElement('td');
      td.className = (isTs(c) ? 'num' : '') + (isText(c) ? ' cell-text' : '');
      td.innerHTML = cellHTML(c, row[c]);
      tr.appendChild(td);
    });
    const tdA = document.createElement('td');
    tdA.className = 'col-actions';
    const act = document.createElement('div');
    act.className = 'row-actions';
    const editBtn = document.createElement('button');
    editBtn.className = 'btn-ghost'; editBtn.textContent = '编辑';
    editBtn.onclick = () => openEdit(row);
    act.append(editBtn);
    if (S.deletable) {
      const delBtn = document.createElement('button');
      delBtn.className = 'btn-danger'; delBtn.textContent = '删除';
      delBtn.onclick = () => openDelete(row);
      act.append(delBtn);
    }
    tdA.appendChild(act);
    tr.appendChild(tdA);
    tbody.appendChild(tr);
  });
  table.append(thead, tbody);
  scroll.appendChild(table);
  wrap.append(tb, scroll);
  contentEl.innerHTML = '';
  contentEl.appendChild(wrap);

  search.addEventListener('input', () => {
    const q = search.value.trim().toLowerCase();
    Array.from(tbody.children).forEach((tr, i) => {
      const row = S.rows[i];
      const hay = cols.map(c => String(row[c] ?? '')).join(' ').toLowerCase();
      tr.style.display = (!q || hay.includes(q)) ? '' : 'none';
    });
  });
}

/* ---------- 编辑弹窗 ---------- */
let editContext = null;
function openEdit(row) {
  editContext = row;
  $('editTitle').textContent = `编辑 · ${LABEL[S.table] || S.table}`;
  const form = $('editForm');
  form.innerHTML = '';
  S.columns.forEach(c => {
    if (c === 'id') return; // 主键不允许编辑
    const type = (S.types[c] || '').toLowerCase();
    const field = document.createElement('div');
    if (c === 'is_deleted') {
      field.className = 'field check';
      const id = 'f_' + c;
      const inp = document.createElement('input');
      inp.type = 'checkbox'; inp.id = id; inp.dataset.col = c; inp.dataset.kind = 'bool';
      inp.checked = (row[c] == 1 || row[c] === '1');
      const lbl = document.createElement('label');
      lbl.textContent = labelOf(c); lbl.setAttribute('for', id);
      field.append(inp, lbl);
    } else if (isText(c) || type.includes('text')) {
      field.className = 'field';
      const lbl = document.createElement('label'); lbl.textContent = labelOf(c);
      const inp = document.createElement('textarea');
      inp.id = 'f_' + c; inp.value = row[c] ?? ''; inp.dataset.col = c; inp.dataset.kind = 'text';
      field.append(lbl, inp);
    } else if (type.includes('int') || type.includes('bigint') || type.includes('float') || type.includes('double') || type.includes('decimal') || type.includes('numeric')) {
      field.className = 'field';
      const lbl = document.createElement('label'); lbl.textContent = labelOf(c);
      const inp = document.createElement('input');
      inp.type = 'number'; inp.id = 'f_' + c; inp.value = row[c] ?? ''; inp.dataset.col = c; inp.dataset.kind = 'num';
      if (type.includes('float') || type.includes('double') || type.includes('decimal') || type.includes('numeric')) inp.step = 'any';
      field.append(lbl, inp);
    } else {
      field.className = 'field';
      const lbl = document.createElement('label'); lbl.textContent = labelOf(c);
      const inp = document.createElement('input');
      inp.type = 'text'; inp.id = 'f_' + c; inp.value = row[c] ?? ''; inp.dataset.col = c; inp.dataset.kind = 'text';
      field.append(lbl, inp);
      if (isTs(c)) {
        const hint = document.createElement('div');
        hint.className = 'hint';
        hint.textContent = '时间戳(毫秒)：' + (Number(row[c]) ? fmtTs(row[c]) : '未设置');
        field.appendChild(hint);
      }
    }
    form.appendChild(field);
  });
  openModal('editModal');
}

$('saveEditBtn').addEventListener('click', async () => {
  const fields = {};
  document.querySelectorAll('#editForm [data-col]').forEach(inp => {
    const col = inp.dataset.col;
    if (inp.dataset.kind === 'bool') fields[col] = inp.checked ? 1 : 0;
    else if (inp.dataset.kind === 'num') fields[col] = inp.value === '' ? '' : inp.value;
    else fields[col] = inp.value;
  });
  const btn = $('saveEditBtn');
  btn.disabled = true;
  try {
    await api('/admin/update', { method: 'POST', body: JSON.stringify({ table: S.table, id: editContext.id, fields }) });
    closeModal('editModal');
    toast('已保存');
    showTable(S.table);
  } catch (e) {
    toast('保存失败：' + e.message, true);
  } finally { btn.disabled = false; }
});

/* ---------- 删除确认 ---------- */
let delContext = null;
function openDelete(row) {
  delContext = row;
  const label = row.title || row.word || row.book_name || row.name || row.id;
  $('delText').textContent = `确定删除「${String(label).slice(0, 40)}」(ID: ${row.id})？删除后将同步到各设备。`;
  openModal('delModal');
}

$('confirmDelBtn').addEventListener('click', async () => {
  const btn = $('confirmDelBtn');
  btn.disabled = true;
  try {
    await api('/admin/delete', { method: 'POST', body: JSON.stringify({ table: S.table, id: delContext.id }) });
    closeModal('delModal');
    toast('已删除');
    showTable(S.table);
  } catch (e) {
    toast('删除失败：' + e.message, true);
  } finally { btn.disabled = false; }
});

/* ---------- 审计日志视图 ---------- */
async function showAudit() {
  S.table = ''; S.rows = [];
  contentEl.innerHTML = '<div class="empty">加载中…</div>';
  try {
    const d = await api('/admin/audit?limit=200');
    renderAudit(d.rows);
  } catch (e) {
    contentEl.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
  }
}

function renderAudit(rows) {
  if (!rows || !rows.length) {
    contentEl.innerHTML = '<div class="empty">暂无操作记录</div>';
    return;
  }
  const wrap = document.createElement('div');
  wrap.className = 'table-wrap';
  const scroll = document.createElement('div');
  scroll.className = 'table-scroll';
  const table = document.createElement('table');
  table.className = 'data';
  const thead = document.createElement('thead');
  thead.innerHTML = '<tr><th>时间</th><th>操作</th><th>数据表</th><th>行 ID</th><th>变更内容</th><th>IP</th></tr>';
  const tbody = document.createElement('tbody');
  rows.forEach(r => {
    const tr = document.createElement('tr');
    let changes = null;
    if (r.changes) {
      try { changes = JSON.parse(r.changes); } catch (e) { changes = r.changes; }
    }
    const changeText = changes && typeof changes === 'object'
      ? Object.entries(changes).map(([k, v]) => `${labelOf(k) || k}: ${v}`).join('，')
      : '—';
    const actionBadge = r.action === 'delete'
      ? '<span class="badge deleted-1">删除</span>'
      : '<span class="badge prio-1">编辑</span>';
    tr.innerHTML =
      `<td class="num">${esc(fmtTs(r.created_at))}</td>` +
      `<td>${actionBadge}</td>` +
      `<td>${esc(LABEL[r.table_name] || r.table_name)}</td>` +
      `<td class="mono">${esc(r.row_id)}</td>` +
      `<td class="cell-text">${esc(String(changeText))}</td>` +
      `<td>${esc(r.ip || '')}</td>`;
    tbody.appendChild(tr);
  });
  table.append(thead, tbody);
  scroll.appendChild(table);
  wrap.appendChild(scroll);
  contentEl.innerHTML = '';
  contentEl.appendChild(wrap);
}

/* ---------- 用户管理视图（以设备 ID 聚合的「用户」） ---------- */
function shortId(id) {
  if (!id) return '—';
  return id.length > 16 ? id.slice(0, 8) + '…' + id.slice(-4) : id;
}

let userCtx = null;

async function showUsers(resetOffset = true) {
  S.view = 'users'; S.table = '';
  if (resetOffset) S.offset = 0;
  contentEl.innerHTML = '<div class="empty">加载中…</div>';
  try {
    const q = (S.userQ || '').trim();
    const url = '/admin/users?limit=' + S.limit + '&offset=' + S.offset + (q ? '&q=' + encodeURIComponent(q) : '');
    const d = await api(url);
    S.rows = d.rows || [];
    S.total = d.total || 0;
    renderUsers();
  } catch (e) {
    contentEl.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
  }
}

function renderUsers() {
  const wrap = document.createElement('div');
  wrap.className = 'table-wrap';

  const tb = document.createElement('div');
  tb.className = 'table-toolbar';
  const search = document.createElement('input');
  search.placeholder = '按设备 ID 筛选…';
  search.value = S.userQ || '';
  const meta = document.createElement('span');
  meta.className = 'table-meta';
  const from = S.total === 0 ? 0 : S.offset + 1;
  const to = Math.min(S.offset + S.rows.length, S.total);
  meta.textContent = `共 ${S.total} 个设备 · 显示 ${from}–${to}`;
  const prev = document.createElement('button');
  prev.className = 'btn-ghost'; prev.textContent = '上一页';
  prev.disabled = S.offset <= 0;
  prev.onclick = () => { S.offset = Math.max(0, S.offset - S.limit); showUsers(false); };
  const next = document.createElement('button');
  next.className = 'btn-ghost'; next.textContent = '下一页';
  next.disabled = S.offset + S.rows.length >= S.total;
  next.onclick = () => { S.offset += S.limit; showUsers(false); };
  tb.append(search, meta, prev, next);

  const scroll = document.createElement('div');
  scroll.className = 'table-scroll';
  const table = document.createElement('table');
  table.className = 'data';
  const thead = document.createElement('thead');
  thead.innerHTML = '<tr><th>设备 ID</th><th>首次活跃</th><th>最后活跃</th><th>记录数</th><th>状态</th><th>备注</th><th class="col-actions">操作</th></tr>';
  const tbody = document.createElement('tbody');
  (S.rows || []).forEach(row => {
    const banned = row.status === 'banned';
    const tr = document.createElement('tr');
    tr.innerHTML =
      `<td class="mono" title="${esc(row.device_id)}">${esc(shortId(row.device_id))}</td>` +
      `<td class="num">${esc(fmtTs(row.first_seen))}</td>` +
      `<td class="num">${esc(fmtTs(row.last_seen))}</td>` +
      `<td class="num">${row.total_records}</td>` +
      `<td>${banned ? '<span class="badge deleted-1">已封禁</span>' : '<span class="badge prio-1">正常</span>'}</td>` +
      `<td>${esc(row.note || '')}</td>`;
    const tdA = document.createElement('td');
    tdA.className = 'col-actions';
    const act = document.createElement('div');
    act.className = 'row-actions';
    const banBtn = document.createElement('button');
    banBtn.className = banned ? 'btn-ghost' : 'btn-danger';
    banBtn.textContent = banned ? '解封' : '封禁';
    banBtn.onclick = () => userConfirm(row.device_id, banned ? 'unban' : 'ban');
    const noteBtn = document.createElement('button');
    noteBtn.className = 'btn-ghost'; noteBtn.textContent = '备注';
    noteBtn.onclick = () => userNote(row);
    const delBtn = document.createElement('button');
    delBtn.className = 'btn-danger'; delBtn.textContent = '清除数据';
    delBtn.onclick = () => userConfirm(row.device_id, 'delete');
    act.append(banBtn, noteBtn, delBtn);
    tdA.appendChild(act);
    tr.appendChild(tdA);
    tbody.appendChild(tr);
  });
  table.append(thead, tbody);
  scroll.appendChild(table);
  wrap.append(tb, scroll);
  contentEl.innerHTML = '';
  contentEl.appendChild(wrap);

  search.addEventListener('input', () => {
    clearTimeout(search._t);
    search._t = setTimeout(() => {
      S.userQ = search.value;
      S.offset = 0;
      showUsers(false);
    }, 300);
  });
}

function userConfirm(deviceId, mode) {
  userCtx = { deviceId, mode };
  const inp = $('userModalInput');
  inp.hidden = true; inp.value = '';
  $('userModalOk').className = 'btn-danger';
  if (mode === 'ban') {
    $('userModalTitle').textContent = '封禁设备';
    $('userModalText').textContent = '确认封禁设备「' + shortId(deviceId) + '」？封禁仅作标记，不影响其已同步数据。';
  } else if (mode === 'unban') {
    $('userModalTitle').textContent = '解封设备';
    $('userModalText').textContent = '确认解封设备「' + shortId(deviceId) + '」？';
  } else {
    $('userModalTitle').textContent = '清除数据';
    $('userModalText').textContent = '确认清除设备「' + shortId(deviceId) + '」的全部数据？该操作不可撤销，且会同步到其设备。';
  }
  openModal('userModal');
}

function userNote(row) {
  userCtx = { deviceId: row.device_id, mode: 'note' };
  $('userModalTitle').textContent = '编辑备注';
  $('userModalText').textContent = '设备 ' + shortId(row.device_id);
  const inp = $('userModalInput');
  inp.hidden = false; inp.value = row.note || '';
  $('userModalOk').className = 'btn-primary';
  openModal('userModal');
}

$('userModalOk').addEventListener('click', async () => {
  if (!userCtx) return;
  const { deviceId, mode } = userCtx;
  const btn = $('userModalOk');
  btn.disabled = true;
  try {
    if (mode === 'ban') {
      await api('/admin/users/set', { method: 'POST', body: JSON.stringify({ device_id: deviceId, status: 'banned' }) });
    } else if (mode === 'unban') {
      await api('/admin/users/set', { method: 'POST', body: JSON.stringify({ device_id: deviceId, status: 'active' }) });
    } else if (mode === 'note') {
      await api('/admin/users/set', { method: 'POST', body: JSON.stringify({ device_id: deviceId, note: $('userModalInput').value }) });
    } else if (mode === 'delete') {
      await api('/admin/users/delete', { method: 'POST', body: JSON.stringify({ device_id: deviceId }) });
    }
    closeModal('userModal');
    $('userModalInput').hidden = true;
    toast('操作成功');
    showUsers(false);
  } catch (e) {
    toast('操作失败：' + e.message, true);
  } finally {
    btn.disabled = false;
  }
});

/* ---------- 通用事件 ---------- */
document.querySelectorAll('[data-close]').forEach(btn => {
  btn.addEventListener('click', () => closeModal(btn.dataset.close));
});
document.querySelectorAll('.modal-overlay').forEach(ov => {
  ov.addEventListener('click', e => { if (e.target === ov) ov.hidden = true; });
});
$('refreshBtn').addEventListener('click', () => {
  if (S.view === 'overview') showOverview();
  else if (S.view === 'audit') showAudit();
  else if (S.view === 'users') showUsers(false);
  else showTable(S.view);
});
$('menuBtn').addEventListener('click', () => appEl.classList.toggle('nav-open'));

/* ---------- 启动 ---------- */
boot();
