/* 自律工作台 · 后台管理 — 前端逻辑（原生 JS，无构建步骤） */
(() => {
  "use strict";

  const view = document.getElementById("view");
  const connEl = document.getElementById("conn");
  const toastEl = document.getElementById("toast");

  const TABLE_META = {
    tasks:          { label: "任务" },
    notes:          { label: "笔记" },
    sport_records:  { label: "运动" },
    english_words:  { label: "英语" },
    movie_books:    { label: "影音书籍" },
    health_records: { label: "健康" },
    account_records:{ label: "记账" },
    categories:     { label: "分类" },
    user_settings:  { label: "设置" },
  };

  let currentTable = null;
  let currentRows = [];

  /* ---------- 工具 ---------- */
  const api = async (path) => {
    const res = await fetch(path, { headers: { Accept: "application/json" } });
    if (!res.ok) throw new Error("HTTP " + res.status);
    const j = await res.json();
    if (j.code !== 0) throw new Error(j.message || "接口错误");
    return j.data;
  };

  const esc = (s) =>
    String(s).replace(/[&<>"]/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[m]));

  const humanize = (c) => c.replace(/_/g, " ").replace(/\b\w/g, (m) => m.toUpperCase());

  const isTs = (c) =>
    /_(at|time)$/.test(c) || ["last_modified", "due_date", "reminder_time", "next_review", "synced_at"].includes(c);

  const isTextCol = (c) =>
    ["content", "title", "note", "meaning", "example", "rule", "module_toggles", "summary", "definition", "remark", "repeat_rule"].includes(c);

  const fmtTs = (ms) => {
    const d = new Date(ms);
    if (isNaN(d)) return String(ms);
    const p = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
  };

  const badge = (cls, text) => `<span class="badge ${cls}">${esc(text)}</span>`;
  const taskStatusBadge = (v) => badge(v == 1 ? "green" : v == 2 ? "blue" : "amber", v == 1 ? "已完成" : v == 2 ? "进行中" : "待办");
  const priorityBadge = (v) => badge(v == 1 ? "rose" : v == 3 ? "gray" : "amber", v == 1 ? "高" : v == 3 ? "低" : "中");
  const taskTypeBadge = (v) => badge("blue", v == 1 ? "重复" : v == 2 ? "目标" : "普通");

  const cellHtml = (table, col, val) => {
    if (val === null || val === "") return '<span class="badge gray">—</span>';
    if (col === "is_deleted") return badge(val == 1 ? "rose" : "green", val == 1 ? "已删除" : "正常");
    if (table === "tasks") {
      if (col === "status") return taskStatusBadge(val);
      if (col === "priority") return priorityBadge(val);
      if (col === "task_type") return taskTypeBadge(val);
    }
    if (isTs(col) && /^\d+$/.test(String(val))) return `<span class="mono">${fmtTs(Number(val))}</span>`;
    let s = typeof val === "object" ? JSON.stringify(val) : String(val);
    if (s.length > 60) return `<span class="cell-text" title="${esc(s)}">${esc(s.slice(0, 60))}…</span>`;
    return esc(s);
  };

  const toast = (msg, isErr) => {
    toastEl.textContent = msg;
    toastEl.classList.toggle("err", !!isErr);
    toastEl.hidden = false;
    requestAnimationFrame(() => toastEl.classList.add("show"));
    clearTimeout(toast._t);
    toast._t = setTimeout(() => {
      toastEl.classList.remove("show");
      setTimeout(() => (toastEl.hidden = true), 250);
    }, 2800);
  };

  /* ---------- 连接状态 ---------- */
  const checkConn = async () => {
    try {
      const d = await api("/api/health");
      connEl.className = "conn online";
      connEl.querySelector(".conn-text").textContent = "已连接 · PHP " + d.php_version;
    } catch {
      connEl.className = "conn offline";
      connEl.querySelector(".conn-text").textContent = "连接失败";
    }
  };

  /* ---------- 概览 ---------- */
  const showOverview = async () => {
    const skeleton = `<div class="view-head"><div><h2>概览</h2><p class="sub">加载中…</p></div></div>
      <div class="grid cols-auto">${'<div class="card skeleton"><div class="sk-bar"></div></div>'.repeat(6)}</div>`;
    view.innerHTML = skeleton;
    try {
      const d = await api("/admin/overview");
      renderOverview(d);
    } catch (e) {
      toast("加载概览失败：" + e.message, true);
      view.innerHTML = `<div class="panel"><div class="empty">加载失败</div></div>`;
    }
  };

  const renderOverview = (d) => {
    const t = d.tables || {};
    const cards = Object.keys(TABLE_META)
      .map((k) => {
        const v = t[k] ?? 0;
        return `<div class="card stat">
          <div class="label">${esc(TABLE_META[k].label)}</div>
          <div class="value">${v}</div>
          <div class="foot">${esc(TABLE_META[k].label)}记录数</div>
        </div>`;
      })
      .join("");

    const max = Math.max(1, ...Object.keys(TABLE_META).map((k) => t[k] ?? 0));
    const bars = Object.keys(TABLE_META)
      .map((k) => {
        const v = t[k] ?? 0;
        const pct = Math.max(6, Math.round((v / max) * 100));
        return `<div class="sysrow">
          <span class="k">${esc(TABLE_META[k].label)}</span>
          <span style="display:flex;align-items:center;gap:10px;flex:1;justify-content:flex-end">
            <span style="height:6px;width:${pct}px;border-radius:6px;background:linear-gradient(90deg,var(--accent),var(--cyan))"></span>
            <span class="v">${v}</span>
          </span></div>`;
      })
      .join("");

    view.innerHTML = `
      <div class="view-head"><div><h2>概览</h2><p class="sub">全局数据与服务状态 · 实时快照</p></div></div>
      <div class="grid cols-auto">${cards}</div>
      <div class="grid cols-2" style="margin-top:2px">
        <div class="card">
          <div class="stat"><div class="label">系统状态</div></div>
          <div class="sysrow"><span class="k">数据库</span><span class="v">${d.db_connected ? "已连接" : "异常"}</span></div>
          <div class="sysrow"><span class="k">PHP 版本</span><span class="v">${esc(d.php_version)}</span></div>
          <div class="sysrow"><span class="k">接入设备</span><span class="v">${d.device_count}</span></div>
          <div class="sysrow"><span class="k">服务器时间</span><span class="v">${fmtTs(d.server_time)}</span></div>
          ${d.db_error ? `<div class="sysrow"><span class="k">错误</span><span class="v" style="color:var(--rose)">${esc(d.db_error)}</span></div>` : ""}
        </div>
        <div class="card">
          <div class="stat"><div class="label">模块分布</div></div>
          ${bars}
        </div>
      </div>`;
  };

  /* ---------- 数据浏览 ---------- */
  const showTable = async (table) => {
    currentTable = table;
    const meta = TABLE_META[table] || { label: table };
    view.innerHTML = `
      <div class="view-head">
        <div><h2>${esc(meta.label)}</h2><p class="sub">数据浏览（只读）· 加载中…</p></div>
        <label class="search">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
          <input id="q" type="search" placeholder="在当前结果中筛选…" autocomplete="off" />
        </label>
      </div>
      <div class="panel"><div class="skeleton"><div class="sk-bar"></div><div class="sk-bar"></div><div class="sk-bar"></div></div></div>`;
    document.getElementById("q").addEventListener("input", (e) => filterRows(e.target.value));
    try {
      const d = await api("/admin/browse?table=" + encodeURIComponent(table) + "&limit=200");
      currentRows = d.rows || [];
      renderBody(d.columns || [], currentRows, meta.label);
    } catch (e) {
      toast("加载失败：" + e.message, true);
    }
  };

  const renderBody = (columns, rows, label) => {
    const panel = view.querySelector(".panel");
    const sub = view.querySelector(".sub");
    if (!rows.length) {
      panel.innerHTML = `<div class="empty">暂无 ${esc(label)} 数据</div>`;
      if (sub) sub.textContent = `数据浏览（只读）· 0 条`;
      return;
    }
    const head = columns.map((c) => `<th>${esc(humanize(c))}</th>`).join("");
    const body = rows
      .map((r) => {
        const tds = columns
          .map((c) => {
            const cls = isTextCol(c) ? ' class="cell-text"' : "";
            return `<td${cls}>${cellHtml(currentTable, c, r[c])}</td>`;
          })
          .join("");
        return `<tr>${tds}</tr>`;
      })
      .join("");
    panel.innerHTML = `<div class="table-wrap"><table class="data"><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
    if (sub) sub.textContent = `数据浏览（只读）· 显示 ${rows.length} 条`;
  };

  const filterRows = (q) => {
    q = q.trim().toLowerCase();
    const rows = q
      ? currentRows.filter((r) => Object.values(r).some((v) => String(v).toLowerCase().includes(q)))
      : currentRows;
    const label = (TABLE_META[currentTable] || {}).label || currentTable;
    if (!currentRows.length) return;
    const columns = Object.keys(currentRows[0]);
    renderBody(columns, rows, label);
  };

  /* ---------- 导航 ---------- */
  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      const v = btn.dataset.view;
      if (v === "overview") showOverview();
      else showTable(v);
    });
  });

  document.getElementById("refresh").addEventListener("click", () => {
    checkConn();
    const active = document.querySelector(".nav-item.active");
    if (active && active.dataset.view === "overview") showOverview();
    else if (active) showTable(active.dataset.view);
  });

  /* ---------- 启动 ---------- */
  checkConn();
  showOverview();
})();
