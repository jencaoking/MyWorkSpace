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
  { key: 'audit',           label: '审计日志', ico: '✦' },
];
const LABEL = Object.fromEntries(NAV.map(n => [n.key, n.label]));

/* ---------- 全局状态 ---------- */
const S = { view: 'overview', table: '', rows: [], columns: [], types: {}, deletable: true };

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
  else showTable(key);
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

async function showTable(table) {
  S.table = table; S.rows = []; S.columns = []; S.types = {};
  contentEl.innerHTML = '<div class="empty">加载中…</div>';
  try {
    const d = await api('/admin/browse?table=' + encodeURIComponent(table) + '&limit=100');
    S.rows = d.rows; S.columns = d.columns; S.types = d.types || {};
    S.deletable = (d.deletable !== false);
    renderTable();
  } catch (e) {
    contentEl.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
  }
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
  meta.textContent = `共 ${S.rows.length} 行`;
  tb.append(search, meta);

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
  else showTable(S.view);
});
$('menuBtn').addEventListener('click', () => appEl.classList.toggle('nav-open'));

/* ---------- 启动 ---------- */
boot();
