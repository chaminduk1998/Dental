/* Reusable UI helpers: toasts, modals, formatting. */
const UI = (() => {

  function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
  }

  function money(n) {
    const v = Number(n) || 0;
    return "Rs. " + v.toLocaleString("en-LK", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function dateNice(iso) {
    if (!iso) return "-";
    const d = new Date(iso.length === 10 ? iso + "T00:00:00" : iso.replace(" ", "T"));
    if (isNaN(d)) return iso;
    return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
  }

  function timeNice(hhmm) {
    if (!hhmm) return "-";
    const [h, m] = hhmm.split(":").map(Number);
    const ap = h >= 12 ? "PM" : "AM";
    const h12 = ((h + 11) % 12) + 1;
    return `${h12}:${String(m).padStart(2, "0")} ${ap}`;
  }

  function initials(name) {
    if (!name) return "?";
    const parts = name.trim().split(/\s+/);
    return ((parts[0]?.[0] || "") + (parts[1]?.[0] || "")).toUpperCase() || name[0].toUpperCase();
  }

  function debounce(fn, wait) {
    let t;
    return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), wait); };
  }

  function statusBadge(status) {
    const s = (status || "").toUpperCase();
    const map = { PENDING: "pending", CONFIRMED: "confirmed", COMPLETED: "completed", CANCELLED: "cancelled" };
    const cls = map[s] || "pending";
    return `<span class="badge badge-${cls}"><span class="badge-dot"></span>${escapeHtml(s)}</span>`;
  }

  // ---------------- toast ----------------
  function toast(message, type = "default") {
    const stack = document.getElementById("toast-stack");
    const el = document.createElement("div");
    el.className = "toast" + (type === "success" ? " success" : type === "error" ? " error" : "");
    const icon = type === "success" ? ICON.checkCircle : type === "error" ? ICON.xCircle : ICON.info;
    el.innerHTML = `${icon}<span>${escapeHtml(message)}</span>`;
    stack.appendChild(el);
    setTimeout(() => {
      el.style.transition = "opacity .25s, transform .25s";
      el.style.opacity = "0";
      el.style.transform = "translateX(16px)";
      setTimeout(() => el.remove(), 260);
    }, 3600);
  }

  // ---------------- modal ----------------
  function openModal({ title, bodyHtml, wide = false, footHtml = "", onMount }) {
    const root = document.getElementById("modal-root");
    const overlay = document.createElement("div");
    overlay.className = "modal-overlay";
    overlay.innerHTML = `
      <div class="modal ${wide ? "modal-wide" : ""}">
        <div class="modal-head">
          <h3>${escapeHtml(title)}</h3>
          <button class="modal-close" data-close>${ICON.x}</button>
        </div>
        <div class="modal-body">${bodyHtml}</div>
        ${footHtml ? `<div class="modal-foot">${footHtml}</div>` : ""}
      </div>`;
    root.appendChild(overlay);

    const close = () => overlay.remove();
    overlay.addEventListener("click", (e) => { if (e.target === overlay) close(); });
    overlay.querySelector("[data-close]").addEventListener("click", close);
    document.addEventListener("keydown", function esc(e) {
      if (e.key === "Escape") { close(); document.removeEventListener("keydown", esc); }
    });

    if (onMount) onMount(overlay, close);
    return { overlay, close };
  }

  function confirmDialog(message, opts = {}) {
    return new Promise((resolve) => {
      openModal({
        title: opts.title || "Please confirm",
        bodyHtml: `<p style="margin:0;color:var(--ink-700);font-size:13.5px;line-height:1.6;">${escapeHtml(message)}</p>`,
        footHtml: `
          <button class="btn btn-outline" data-cancel>Cancel</button>
          <button class="btn ${opts.danger ? "btn-danger" : "btn-primary"}" data-ok>${escapeHtml(opts.okLabel || "Confirm")}</button>`,
        onMount: (overlay, close) => {
          overlay.querySelector("[data-cancel]").addEventListener("click", () => { close(); resolve(false); });
          overlay.querySelector("[data-ok]").addEventListener("click", () => { close(); resolve(true); });
        },
      });
    });
  }

  function el(html) {
    const t = document.createElement("template");
    t.innerHTML = html.trim();
    return t.content.firstElementChild;
  }

  return { escapeHtml, money, dateNice, timeNice, initials, debounce, statusBadge, toast, openModal, confirmDialog, el };
})();
