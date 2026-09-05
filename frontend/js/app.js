/* ===========================================================
  Sunrise Dental Clinic - SPA shell + views
   =========================================================== */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

const STATE = {
  user: null,
  dentists: [],
  treatments: [],
  pendingParams: null,
};

const NAV = [
  { section: "Main" },
  { id: "dashboard", label: "Dashboard", icon: "dashboard", title: "Dashboard", sub: "Overview of today's clinic activity" },
  { id: "appointments", label: "Appointments", icon: "calendar", title: "Appointments", sub: "Register, search and manage bookings" },
  { id: "billing", label: "Billing", icon: "receipt", title: "Billing", sub: "Calculate and print patient bills" },
  { id: "patients", label: "Patients", icon: "users", title: "Patients", sub: "Patient records and treatment history" },
  { id: "reports", label: "Reports", icon: "chart", title: "Reports", sub: "Daily, revenue and treatment reports" },
  { id: "reminders", label: "Reminders", icon: "bell", title: "Reminders", sub: "Queued confirmations and reminders" },
  { section: "Administration", adminOnly: true },
  { id: "admin-dentists", label: "Dentists", icon: "userPlus", title: "Manage Dentists", sub: "Add, edit or deactivate dentists", adminOnly: true },
  { id: "admin-treatments", label: "Treatments", icon: "tooth", title: "Manage Treatments", sub: "Treatment types and base costs", adminOnly: true },
  { id: "admin-users", label: "Staff Users", icon: "shield", title: "Staff Users", sub: "Manage login accounts and roles", adminOnly: true },
  { id: "audit", label: "Audit Log", icon: "history", title: "Audit Log", sub: "Who did what, and when", adminOnly: true },
  { section: "Support" },
  { id: "help", label: "Help", icon: "help", title: "Help & Guide", sub: "How to use the system" },
];

/* ---------------------------------------------------------
   Boot
   --------------------------------------------------------- */
document.addEventListener("DOMContentLoaded", init);

async function init() {
  $("#year").textContent = new Date().getFullYear();
  $("#year2").textContent = new Date().getFullYear();
  startClock();
  wireHome();
  wireLogin();
  $("#logout-btn").addEventListener("click", doLogout);
  $("#menu-toggle").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
  window.addEventListener("hashchange", () => renderRoute(currentRoute()));

  try {
    const res = await API.get("/api/auth/me");
    onAuthenticated(res.user);
  } catch (e) {
    showHome();
  }
}

function startClock() {
  const tick = () => {
    const el = $("#clock");
    if (el) el.textContent = new Date().toLocaleString("en-GB", {
      weekday: "short", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit",
    });
  };
  tick();
  setInterval(tick, 15000);
}

function showHome() {
  $("#splash")?.remove();
  $("#home-screen").hidden = false;
  $("#login-screen").hidden = true;
  $("#app-shell").style.display = "none";
}

function showLogin() {
  $("#splash")?.remove();
  $("#home-screen").hidden = true;
  $("#login-screen").hidden = false;
  $("#app-shell").style.display = "none";
}

function wireHome() {
  $$("#home-login-btn, #home-contact-login, #home-footer-login").forEach((btn) => {
    btn.addEventListener("click", (e) => { e.preventDefault(); showLogin(); });
  });
  $$("[data-scroll]", $("#home-screen")).forEach((link) => {
    link.addEventListener("click", (e) => {
      e.preventDefault();
      $("#" + link.dataset.scroll)?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });
  wireBookingForm();
}

/* ---------------------------------------------------------
   Public online booking form (home page - no login required)
   --------------------------------------------------------- */
async function wireBookingForm() {
  const form = $("#public-booking-form");
  if (!form) return;

  const dentistSelect = $("#pb-dentist", form);
  const treatmentSelect = $("#pb-treatment", form);
  const dateInput = $("#pb-date", form);
  dateInput.min = todayStr();

  let publicDentists = [];
  let publicTreatments = [];

  try {
    [publicDentists, publicTreatments] = await Promise.all([
      API.get("/api/public/dentists"),
      API.get("/api/public/treatments"),
    ]);
    dentistSelect.innerHTML = '<option value="">Select a dentist</option>' +
      publicDentists.map((d) => `<option value="${d.id}">${UI.escapeHtml(d.name)}${d.specialization ? " - " + UI.escapeHtml(d.specialization) : ""}</option>`).join("");
    treatmentSelect.innerHTML = '<option value="">Select a treatment</option>' +
      publicTreatments.map((t) => `<option value="${t.id}">${UI.escapeHtml(t.treatmentType)} - ${UI.money(t.baseCost)}</option>`).join("");
  } catch (e) {
    dentistSelect.innerHTML = '<option value="">Could not load dentists</option>';
    treatmentSelect.innerHTML = '<option value="">Could not load treatments</option>';
  }

  const updateTotal = () => {
    const d = publicDentists.find((x) => x.id === Number(dentistSelect.value));
    const t = publicTreatments.find((x) => x.id === Number(treatmentSelect.value));
    const total = (d?.consultationFee || 0) + (t?.baseCost || 0);
    $("#pb-total", form).textContent = UI.money(total);
  };
  dentistSelect.addEventListener("change", updateTotal);
  treatmentSelect.addEventListener("change", updateTotal);

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const errBox = $("#booking-error", form);
    errBox.hidden = true;
    const btn = $("#pb-submit", form);
    btn.disabled = true;
    btn.textContent = "Submitting...";
    try {
      const payload = Object.fromEntries(new FormData(form).entries());
      const res = await API.post("/api/public/appointments", payload);
      form.reset();
      dateInput.min = todayStr();
      $("#pb-total", form).textContent = "Rs. 0.00";
      showBookingConfirmation(res);
    } catch (e2) {
      errBox.textContent = e2.message || "Could not submit your booking";
      errBox.hidden = false;
      errBox.scrollIntoView({ behavior: "smooth", block: "center" });
    } finally {
      btn.disabled = false;
      btn.textContent = "Request Appointment";
    }
  });
}

function showBookingConfirmation(res) {
  UI.openModal({
    title: "Appointment Requested!",
    bodyHtml: `
      <div style="text-align:center;padding:8px 0 4px;">
        <div style="font-size:40px;margin-bottom:10px;">✅</div>
        <p style="margin:0 0 6px;font-size:14px;">Your reference number is</p>
        <p style="margin:0 0 18px;font-size:24px;font-weight:800;color:var(--amber-700);">${UI.escapeHtml(res.appointmentNo)}</p>
        <p style="margin:0 0 4px;font-size:13px;color:var(--gray-600);">${UI.escapeHtml(res.dentistName)} &middot; ${UI.escapeHtml(res.treatmentType)}</p>
        <p style="margin:0 0 18px;font-size:13px;color:var(--gray-600);">${UI.dateNice(res.date)} at ${UI.timeNice(res.time)}</p>
        <p style="margin:0;font-size:12.5px;color:var(--gray-500);">Please save this reference number. Our staff will confirm your appointment shortly by phone or email.</p>
      </div>`,
    footHtml: `<button class="btn btn-primary btn-block" id="booking-confirm-done">Done</button>`,
    onMount: (overlay, close) => {
      overlay.querySelector("#booking-confirm-done").addEventListener("click", close);
    },
  });
}

function wireLogin() {
  $("#login-back-home").addEventListener("click", (e) => { e.preventDefault(); showHome(); });
  const form = $("#login-form");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = $("#login-submit");
    const err = $("#login-error");
    err.hidden = true;
    btn.disabled = true;
    btn.textContent = "Signing in...";
    try {
      const res = await API.post("/api/auth/login", {
        username: $("#login-username").value.trim(),
        password: $("#login-password").value,
      });
      onAuthenticated(res.user);
      $("#login-screen").hidden = true;
      form.reset();
    } catch (e) {
      err.textContent = e.message || "Sign in failed";
      err.hidden = false;
    } finally {
      btn.disabled = false;
      btn.textContent = "Sign in";
    }
  });
}

async function doLogout() {
  const yes = await UI.confirmDialog("You will be signed out of Sunrise Dental Clinic.", { okLabel: "Sign out" });
  if (!yes) return;
  try { await API.post("/api/auth/logout"); } catch (e) { /* ignore */ }
  STATE.user = null;
  location.hash = "";
  $("#app-shell").style.display = "none";
  showHome();
  UI.toast("Signed out successfully", "success");
}

function onAuthenticated(user) {
  STATE.user = user;
  $("#splash")?.remove();
  $("#home-screen").hidden = true;
  $("#login-screen").hidden = true;
  $("#app-shell").style.display = "flex";

  $("#user-name").textContent = user.fullName;
  $("#user-role").textContent = user.role;
  $("#user-avatar").textContent = UI.initials(user.fullName);

  renderNav();
  loadLookups();
  renderRoute(currentRoute() || "dashboard");
}

function currentRoute() {
  return (location.hash || "").replace("#/", "").replace("#", "") || null;
}

function navigate(id, params) {
  STATE.pendingParams = params || null;
  if (currentRoute() === id) { renderRoute(id); } else { location.hash = "#/" + id; }
}

function renderNav() {
  const list = $("#nav-list");
  list.innerHTML = "";
  const isAdmin = STATE.user?.isAdmin || STATE.user?.role === "ADMIN";
  NAV.forEach((item) => {
    if (item.adminOnly && !isAdmin) return;
    if (item.section) {
      list.insertAdjacentHTML("beforeend", `<div class="sidebar-section-label">${item.section}</div>`);
      return;
    }
    const a = UI.el(`<div class="nav-item" data-route="${item.id}">${ICON[item.icon] || ""}<span>${item.label}</span></div>`);
    a.addEventListener("click", () => navigate(item.id));
    list.appendChild(a);
  });
}

async function renderRoute(id) {
  const isAdmin = STATE.user?.role === "ADMIN";
  let entry = NAV.find((n) => n.id === id);
  if (!entry || (entry.adminOnly && !isAdmin)) {
    entry = NAV.find((n) => n.id === "dashboard");
    id = "dashboard";
  }
  $$(".nav-item").forEach((n) => n.classList.toggle("active", n.dataset.route === id));
  $("#page-title").textContent = entry.title;
  $("#page-sub").textContent = entry.sub;
  $("#sidebar").classList.remove("open");

  const view = $("#view");
  view.innerHTML = `<div class="spinner"></div>`;
  try {
    await Views[toCamel(id)](view);
  } catch (e) {
    console.error(e);
    view.innerHTML = `<div class="empty-state">${ICON.info}<div>${UI.escapeHtml(e.message || "Something went wrong")}</div></div>`;
  }
}

function toCamel(id) {
  return id.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
}

/* ---------------------------------------------------------
   Shared lookups (dentists / treatments)
   --------------------------------------------------------- */
async function loadLookups() {
  try {
    STATE.dentists = await API.get("/api/dentists?activeOnly=true");
    STATE.treatments = await API.get("/api/treatments?activeOnly=true");
  } catch (e) { /* handled per-view */ }
}

function dentistOptions(selectedId) {
  return STATE.dentists.map((d) =>
    `<option value="${d.id}" ${d.id === selectedId ? "selected" : ""}>${UI.escapeHtml(d.name)} - ${UI.money(d.consultationFee)}</option>`
  ).join("");
}

function treatmentOptions(selectedId) {
  return STATE.treatments.map((t) =>
    `<option value="${t.id}" ${t.id === selectedId ? "selected" : ""}>${UI.escapeHtml(t.treatmentType)} - ${UI.money(t.baseCost)}</option>`
  ).join("");
}

function dentistFee(id) { return STATE.dentists.find((d) => d.id === Number(id))?.consultationFee || 0; }
function treatmentCost(id) { return STATE.treatments.find((t) => t.id === Number(id))?.baseCost || 0; }

function todayStr() {
  const d = new Date();
  return d.toISOString().slice(0, 10);
}

function renderBars(rows, { labelKey = "label", valueKey = "value", fmt = (v) => v } = {}) {
  if (!rows.length) return `<div class="empty-state">${ICON.chart}<div>No data yet</div></div>`;
  const max = Math.max(...rows.map((r) => Number(r[valueKey]) || 0), 1);
  return rows.map((r) => `
    <div class="bar-row">
      <div class="label">${UI.escapeHtml(String(r[labelKey]))}</div>
      <div class="bar-track"><div class="bar-fill" style="width:${Math.max(4, (Number(r[valueKey]) / max) * 100)}%"></div></div>
      <div class="val">${fmt(r[valueKey])}</div>
    </div>`).join("");
}

/* ===========================================================
   VIEWS
   =========================================================== */
const Views = {};

/* ----------------------------- Dashboard ----------------------------- */
Views.dashboard = async (view) => {
  const [summary, apptTrend, revTrend, statusRows, topTreat, workload] = await Promise.all([
    API.get("/api/reports/summary"),
    API.get("/api/reports/appointments-trend?days=7"),
    API.get("/api/reports/revenue-trend?days=7"),
    API.get("/api/reports/status-breakdown"),
    API.get("/api/reports/top-treatments?limit=5"),
    API.get("/api/reports/dentist-workload"),
  ]);

  const statusTotal = statusRows.reduce((s, r) => s + Number(r.value), 0) || 1;
  const statusColor = { PENDING: "var(--amber-700)", CONFIRMED: "var(--blue-600)", COMPLETED: "var(--green-600)", CANCELLED: "var(--red-600)" };

  view.innerHTML = `
    <div class="page-head">
      <div>
        <h2 class="section-title">Welcome back, ${UI.escapeHtml(STATE.user.fullName.split(" ")[0])} 👋</h2>
        <p class="section-sub">Here's what's happening at the clinic today.</p>
      </div>
      <div class="toolbar">
        ${summary.unbilledCompleted > 0 ? `<span class="badge badge-pending">${summary.unbilledCompleted} completed visit(s) awaiting billing</span>` : ""}
        <button class="btn btn-primary" id="dash-new-appt">${ICON.plus} New Appointment</button>
      </div>
    </div>

    <div class="grid grid-4" style="margin-bottom:20px;">
      <div class="card stat-tile tile-yellow">
        <div class="stat-icon">${ICON.calendar}</div>
        <div class="stat-value">${summary.appointmentsToday}</div>
        <div class="stat-label">Appointments Today</div>
      </div>
      <div class="card stat-tile tile-blue">
        <div class="stat-icon">${ICON.clock}</div>
        <div class="stat-value">${summary.pending}</div>
        <div class="stat-label">Pending Confirmation</div>
      </div>
      <div class="card stat-tile tile-green">
        <div class="stat-icon">${ICON.dollar}</div>
        <div class="stat-value">${UI.money(summary.revenueMonth)}</div>
        <div class="stat-label">Revenue This Month</div>
      </div>
      <div class="card stat-tile tile-red">
        <div class="stat-icon">${ICON.users}</div>
        <div class="stat-value">${summary.patients}</div>
        <div class="stat-label">Registered Patients</div>
      </div>
    </div>

    <div class="grid grid-2-1" style="margin-bottom:20px;">
      <div class="card">
        <div class="card-head"><div><h3>Appointments - last 7 days</h3><div class="sub">Bookings created per day</div></div></div>
        <div class="card-pad">${renderBars(apptTrend, { labelKey: "label", valueKey: "value", fmt: (v) => v })}</div>
      </div>
      <div class="card">
        <div class="card-head"><div><h3>Appointment Status</h3><div class="sub">${summary.appointmentsTotal} total</div></div></div>
        <div class="card-pad">
          ${statusRows.map((r) => `
            <div class="bar-row">
              <div class="label" style="color:${statusColor[r.label] || "var(--ink-700)"}">${UI.escapeHtml(r.label)}</div>
              <div class="bar-track"><div class="bar-fill" style="width:${(r.value / statusTotal) * 100}%; background:${statusColor[r.label] || "var(--yellow-500)"}"></div></div>
              <div class="val">${r.value}</div>
            </div>`).join("")}
        </div>
      </div>
    </div>

    <div class="grid grid-3">
      <div class="card">
        <div class="card-head"><div><h3>Revenue - last 7 days</h3></div></div>
        <div class="card-pad">${renderBars(revTrend, { fmt: (v) => UI.money(v) })}</div>
      </div>
      <div class="card">
        <div class="card-head"><div><h3>Most Common Treatments</h3></div></div>
        <div class="card-pad">${renderBars(topTreat, { fmt: (v) => v })}</div>
      </div>
      <div class="card">
        <div class="card-head"><div><h3>Dentist Workload</h3></div></div>
        <div class="card-pad">${renderBars(workload, { fmt: (v) => v })}</div>
      </div>
    </div>
  `;

  $("#dash-new-appt").addEventListener("click", () => openAppointmentForm());
};

/* ----------------------------- Appointments ----------------------------- */
Views.appointments = async (view) => {
  const params = STATE.pendingParams; STATE.pendingParams = null;
  const activeTab = params?.tab || "list";

  view.innerHTML = `
    <div class="tabs">
      <button class="tab-btn ${activeTab === "list" ? "active" : ""}" data-tab="list">All Appointments</button>
      <button class="tab-btn ${activeTab === "lookup" ? "active" : ""}" data-tab="lookup">Find by Reference No.</button>
    </div>
    <div id="appt-tab-body"></div>
  `;

  const body = $("#appt-tab-body");
  const showTab = (tab) => tab === "lookup" ? renderApptLookup(body) : renderApptList(body);
  $$(".tab-btn", view).forEach((b) => b.addEventListener("click", () => {
    $$(".tab-btn", view).forEach((x) => x.classList.remove("active"));
    b.classList.add("active");
    showTab(b.dataset.tab);
  }));
  showTab(activeTab);
  if (params?.lookupNo) {
    $('.tab-btn[data-tab="lookup"]', view)?.click();
  }
};

async function renderApptList(body) {
  body.innerHTML = `<div class="spinner"></div>`;
  const state = { q: "", status: "", dentistId: "" };

  const draw = async () => {
    const qs = new URLSearchParams();
    if (state.q) qs.set("q", state.q);
    if (state.status) qs.set("status", state.status);
    if (state.dentistId) qs.set("dentistId", state.dentistId);
    const rows = await API.get("/api/appointments?" + qs.toString());

    body.innerHTML = `
      <div class="page-head">
        <div class="toolbar">
          <div class="search-box">${ICON.search}<input id="appt-q" placeholder="Search patient, contact or reference..." value="${UI.escapeHtml(state.q)}" /></div>
          <select class="filter-select" id="appt-status-filter">
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <select class="filter-select" id="appt-dentist-filter">
            <option value="">All Dentists</option>
            ${STATE.dentists.map((d) => `<option value="${d.id}">${UI.escapeHtml(d.name)}</option>`).join("")}
          </select>
        </div>
        <button class="btn btn-primary" id="appt-new">${ICON.plus} Register Appointment</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="data">
            <thead><tr>
              <th>Reference</th><th>Patient</th><th>Dentist</th><th>Treatment</th><th>Date &amp; Time</th><th>Status</th><th></th>
            </tr></thead>
            <tbody id="appt-tbody"></tbody>
          </table>
        </div>
      </div>
    `;

    const tbody = $("#appt-tbody");
    if (!rows.length) {
      tbody.innerHTML = `<tr class="empty-row"><td colspan="7">${ICON.calendar}<div>No appointments found</div></td></tr>`;
    } else {
      tbody.innerHTML = rows.map((a) => apptRow(a)).join("");
    }

    $("#appt-new").addEventListener("click", () => openAppointmentForm(draw));
    $("#appt-q").addEventListener("input", UI.debounce((e) => { state.q = e.target.value; draw(); }, 350));
    $("#appt-status-filter").value = state.status;
    $("#appt-status-filter").addEventListener("change", (e) => { state.status = e.target.value; draw(); });
    $("#appt-dentist-filter").value = state.dentistId;
    $("#appt-dentist-filter").addEventListener("change", (e) => { state.dentistId = e.target.value; draw(); });

    wireApptRowActions(body, draw);
  };

  await draw();
}

function apptRow(a) {
  const canConfirm = a.status === "PENDING";
  const canComplete = a.status === "CONFIRMED";
  const canEdit = a.status === "PENDING" || a.status === "CONFIRMED";
  const canCancel = a.status !== "CANCELLED" && a.status !== "COMPLETED";
  const canRemind = a.status !== "CANCELLED" && a.status !== "COMPLETED";

  return `
    <tr data-id="${a.id}" data-no="${a.appointmentNo}">
      <td class="cell-strong">${UI.escapeHtml(a.appointmentNo)}</td>
      <td>${UI.escapeHtml(a.patientName)}<div class="cell-muted">${UI.escapeHtml(a.patientContact || "")}</div></td>
      <td>${UI.escapeHtml(a.dentistName)}</td>
      <td>${UI.escapeHtml(a.treatmentType)}</td>
      <td>${UI.dateNice(a.date)}<div class="cell-muted">${UI.timeNice(a.time)}</div></td>
      <td>${UI.statusBadge(a.status)}</td>
      <td>
        <div class="row-actions">
          <button class="btn btn-sm btn-outline btn-icon" data-act="view" title="View details">${ICON.info}</button>
          ${canConfirm ? `<button class="btn btn-sm btn-outline btn-icon" data-act="confirm" title="Confirm">${ICON.check}</button>` : ""}
          ${canComplete ? `<button class="btn btn-sm btn-outline btn-icon" data-act="complete" title="Mark completed">${ICON.checkCircle}</button>` : ""}
          ${canEdit ? `<button class="btn btn-sm btn-outline btn-icon" data-act="edit" title="Reschedule / edit">${ICON.edit}</button>` : ""}
          ${canRemind ? `<button class="btn btn-sm btn-outline btn-icon" data-act="remind" title="Send reminder">${ICON.bell}</button>` : ""}
          ${a.status === "COMPLETED" ? `<button class="btn btn-sm btn-outline btn-icon" data-act="bill" title="${a.billed ? "View bill" : "Bill this visit"}">${ICON.receipt}</button>` : ""}
          ${canCancel ? `<button class="btn btn-sm btn-danger btn-icon" data-act="cancel" title="Cancel">${ICON.x}</button>` : ""}
        </div>
      </td>
    </tr>`;
}

function wireApptRowActions(root, refresh) {
  $$("tr[data-id]", root).forEach((tr) => {
    const id = Number(tr.dataset.id);
    const no = tr.dataset.no;
    $$("button[data-act]", tr).forEach((btn) => {
      btn.addEventListener("click", async () => {
        const act = btn.dataset.act;
        try {
          if (act === "view") { const a = await API.get(`/api/appointments/${id}`); openAppointmentDetail(a); }
          else if (act === "confirm") { await API.put(`/api/appointments/${id}/status`, { status: "CONFIRMED" }); UI.toast("Appointment confirmed", "success"); refresh(); }
          else if (act === "complete") { await API.put(`/api/appointments/${id}/status`, { status: "COMPLETED" }); UI.toast("Marked as completed", "success"); refresh(); }
          else if (act === "edit") { const a = await API.get(`/api/appointments/${id}`); openAppointmentForm(refresh, a); }
          else if (act === "remind") { await API.post(`/api/appointments/${id}/remind`); UI.toast("Reminder queued for the patient", "success"); }
          else if (act === "bill") { navigate("billing", { appointmentNo: no }); }
          else if (act === "cancel") {
            const ok = await UI.confirmDialog(`Cancel appointment ${no}? The patient will be notified.`, { okLabel: "Cancel appointment", danger: true });
            if (ok) { await API.del(`/api/appointments/${id}`); UI.toast("Appointment cancelled", "success"); refresh(); }
          }
        } catch (e) { UI.toast(e.message, "error"); }
      });
    });
  });
}

async function renderApptLookup(body) {
  const params = STATE.pendingParams;
  body.innerHTML = `
    <div class="card" style="max-width:520px;margin-bottom:20px;">
      <div class="card-pad">
        <div class="field" style="margin-bottom:12px;">
          <label>Appointment Reference Number</label>
          <div class="toolbar">
            <input id="lookup-no" placeholder="e.g. APT-1001" style="flex:1;min-width:200px;padding:11px 13px;border-radius:8px;border:1.5px solid var(--border);" />
            <button class="btn btn-primary" id="lookup-btn">${ICON.search} Find</button>
          </div>
        </div>
        <p class="section-sub" style="margin:0;">Displays full patient, dentist and treatment details for the appointment.</p>
      </div>
    </div>
    <div id="lookup-result"></div>
  `;

  const doLookup = async () => {
    const no = $("#lookup-no").value.trim();
    if (!no) return;
    const resultEl = $("#lookup-result");
    resultEl.innerHTML = `<div class="spinner"></div>`;
    try {
      const a = await API.get(`/api/appointments/lookup/${encodeURIComponent(no)}`);
      resultEl.innerHTML = apptDetailCard(a);
      wireApptDetailCard(resultEl, a, doLookup);
    } catch (e) {
      resultEl.innerHTML = `<div class="empty-state">${ICON.info}<div>${UI.escapeHtml(e.message)}</div></div>`;
    }
  };

  $("#lookup-btn").addEventListener("click", doLookup);
  $("#lookup-no").addEventListener("keydown", (e) => { if (e.key === "Enter") doLookup(); });

  if (params?.lookupNo) { $("#lookup-no").value = params.lookupNo; doLookup(); }
}

function apptDetailCard(a) {
  return `
    <div class="card">
      <div class="card-head">
        <div><h3>${UI.escapeHtml(a.appointmentNo)}</h3><div class="sub">Booked ${UI.dateNice(a.createdAt)}</div></div>
        ${UI.statusBadge(a.status)}
      </div>
      <div class="card-pad">
        <div class="grid grid-2">
          <div>
            <div class="section-sub" style="margin-bottom:10px;font-weight:800;color:var(--ink-700);">PATIENT</div>
            <p style="margin:0 0 4px;font-weight:700;">${UI.escapeHtml(a.patientName)}</p>
            <p class="cell-muted" style="margin:0 0 2px;">${ICON.phone} ${UI.escapeHtml(a.patientContact || "-")}</p>
            <p class="cell-muted" style="margin:0 0 2px;">${ICON.map} ${UI.escapeHtml(a.patientAddress || "-")}</p>
            <p class="cell-muted" style="margin:0;">${ICON.mail} ${UI.escapeHtml(a.patientEmail || "-")}</p>
          </div>
          <div>
            <div class="section-sub" style="margin-bottom:10px;font-weight:800;color:var(--ink-700);">VISIT</div>
            <p style="margin:0 0 4px;"><b>${UI.escapeHtml(a.dentistName)}</b></p>
            <p class="cell-muted" style="margin:0 0 2px;">${UI.escapeHtml(a.treatmentType)}</p>
            <p class="cell-muted" style="margin:0 0 2px;">${UI.dateNice(a.date)} at ${UI.timeNice(a.time)}</p>
            <p class="cell-muted" style="margin:0;">Estimated total: <b style="color:var(--ink-900);">${UI.money(a.estimatedTotal)}</b></p>
          </div>
        </div>
        ${a.notes ? `<hr class="hr"/><p class="section-sub" style="font-weight:800;color:var(--ink-700);margin-bottom:6px;">NOTES</p><p style="margin:0;font-size:13px;">${UI.escapeHtml(a.notes)}</p>` : ""}
        <hr class="hr"/>
        <div class="toolbar">
          ${a.status !== "CANCELLED" && a.status !== "COMPLETED" ? `<button class="btn btn-outline" data-act="edit">${ICON.edit} Reschedule</button>` : ""}
          ${a.status !== "CANCELLED" && a.status !== "COMPLETED" ? `<button class="btn btn-outline" data-act="remind">${ICON.bell} Send Reminder</button>` : ""}
          ${a.status !== "CANCELLED" && a.status !== "COMPLETED" ? `<button class="btn btn-danger" data-act="cancel">${ICON.x} Cancel</button>` : ""}
          ${a.status === "COMPLETED" ? `<button class="btn btn-primary" data-act="bill">${ICON.receipt} ${a.billed ? "View Bill" : "Bill This Visit"}</button>` : ""}
          <button class="btn btn-ghost" data-act="print">${ICON.print} Print</button>
        </div>
      </div>
    </div>`;
}

function wireApptDetailCard(root, a, refresh) {
  const btn = (act) => root.querySelector(`[data-act="${act}"]`);
  btn("edit")?.addEventListener("click", () => openAppointmentForm(refresh, a));
  btn("remind")?.addEventListener("click", async () => {
    try { await API.post(`/api/appointments/${a.id}/remind`); UI.toast("Reminder queued", "success"); } catch (e) { UI.toast(e.message, "error"); }
  });
  btn("cancel")?.addEventListener("click", async () => {
    const ok = await UI.confirmDialog(`Cancel appointment ${a.appointmentNo}?`, { okLabel: "Cancel appointment", danger: true });
    if (ok) { try { await API.del(`/api/appointments/${a.id}`); UI.toast("Appointment cancelled", "success"); refresh(); } catch (e) { UI.toast(e.message, "error"); } }
  });
  btn("bill")?.addEventListener("click", () => navigate("billing", { appointmentNo: a.appointmentNo }));
  btn("print")?.addEventListener("click", () => window.print());
}

function openAppointmentDetail(a) {
  const { overlay } = UI.openModal({ title: "Appointment " + a.appointmentNo, wide: true, bodyHtml: `<div class="spinner"></div>` });
  const body = overlay.querySelector(".modal-body");
  body.innerHTML = apptDetailCard(a);
  const cardInner = body.querySelector(".card");
  if (cardInner) { cardInner.style.border = "none"; cardInner.style.boxShadow = "none"; }
  wireApptDetailCard(body, a, () => renderRoute(currentRoute() || "appointments"));
}

function openAppointmentForm(onSaved, existing) {
  const isEdit = !!existing;
  const bodyHtml = `
    <form id="appt-form">
      ${!isEdit ? `
      <div class="grid grid-2">
        <div class="field"><label>Patient Name *</label><input name="patientName" required /></div>
        <div class="field"><label>Contact No *</label><input name="contactNo" required placeholder="07XXXXXXXX" /></div>
      </div>
      <div class="field"><label>Address</label><input name="address" /></div>
      <div class="field"><label>Email (optional, for confirmations)</label><input name="email" type="email" /></div>
      <hr class="hr"/>` : `<p class="section-sub">Editing visit for <b style="color:var(--ink-900)">${UI.escapeHtml(existing.patientName)}</b></p>`}

      <div class="grid grid-2">
        <div class="field">
          <label>Dentist *</label>
          <select name="dentistId" id="f-dentist" required><option value="">Select dentist</option>${dentistOptions(existing?.dentistId)}</select>
        </div>
        <div class="field">
          <label>Treatment Type *</label>
          <select name="treatmentId" id="f-treatment" required><option value="">Select treatment</option>${treatmentOptions(existing?.treatmentId)}</select>
        </div>
      </div>
      <div class="grid grid-2">
        <div class="field"><label>Date *</label><input name="date" type="date" required min="${isEdit ? "" : todayStr()}" value="${existing?.date || ""}" /></div>
        <div class="field"><label>Time *</label><input name="time" type="time" required value="${existing?.time || ""}" /></div>
      </div>
      <div class="field"><label>Notes</label><textarea name="notes" rows="2" style="resize:vertical;">${UI.escapeHtml(existing?.notes || "")}</textarea></div>
      <div class="field" style="margin-bottom:0;">
        <div class="card" style="background:var(--yellow-50);border-color:var(--yellow-200);padding:12px 16px;">
          <div style="display:flex;justify-content:space-between;font-size:13px;font-weight:700;">
            <span>Estimated Total</span><span id="f-total">${UI.money((dentistFee(existing?.dentistId) + treatmentCost(existing?.treatmentId)))}</span>
          </div>
        </div>
      </div>
    </form>
  `;
  const { close } = UI.openModal({
    title: isEdit ? "Reschedule Appointment" : "Register New Appointment",
    wide: true,
    bodyHtml,
    footHtml: `<button class="btn btn-outline" data-cancel>Cancel</button><button class="btn btn-primary" id="appt-save">${ICON.check} ${isEdit ? "Save Changes" : "Register Appointment"}</button>`,
    onMount: (overlay) => {
      const updateTotal = () => {
        const d = $("#f-dentist", overlay).value, t = $("#f-treatment", overlay).value;
        $("#f-total", overlay).textContent = UI.money(dentistFee(d) + treatmentCost(t));
      };
      $("#f-dentist", overlay).addEventListener("change", updateTotal);
      $("#f-treatment", overlay).addEventListener("change", updateTotal);
      overlay.querySelector("[data-cancel]").addEventListener("click", () => close());
      overlay.querySelector("#appt-save").addEventListener("click", async () => {
        const form = $("#appt-form", overlay);
        if (!form.reportValidity()) return;
        const fd = new FormData(form);
        const payload = Object.fromEntries(fd.entries());
        const saveBtn = $("#appt-save", overlay);
        saveBtn.disabled = true;
        try {
          if (isEdit) {
            await API.put(`/api/appointments/${existing.id}`, payload);
            UI.toast("Appointment updated", "success");
          } else {
            await API.post("/api/appointments", payload);
            UI.toast("Appointment registered successfully", "success");
          }
          close();
          onSaved && onSaved();
        } catch (e) {
          UI.toast(e.message, "error");
          saveBtn.disabled = false;
        }
      });
    },
  });
}

/* ----------------------------- Billing ----------------------------- */
Views.billing = async (view) => {
  const params = STATE.pendingParams; STATE.pendingParams = null;

  view.innerHTML = `
    <div class="grid grid-2-1">
      <div>
        <div class="card" style="margin-bottom:20px;">
          <div class="card-pad">
            <div class="field" style="margin-bottom:12px;">
              <label>Appointment Reference Number</label>
              <div class="toolbar">
                <input id="bill-no" placeholder="e.g. APT-1001" style="flex:1;min-width:200px;padding:11px 13px;border-radius:8px;border:1.5px solid var(--border);" />
                <button class="btn btn-primary" id="bill-find">${ICON.search} Find</button>
              </div>
            </div>
            <p class="section-sub" style="margin:0;">Look up a visit to calculate and print its bill, or view a receipt already issued.</p>
          </div>
        </div>
        <div id="bill-result"></div>
      </div>

      <div class="card">
        <div class="card-head"><div><h3>Awaiting Billing</h3><div class="sub">Completed visits without a bill</div></div></div>
        <div id="unbilled-list" class="card-pad"><div class="spinner"></div></div>
      </div>
    </div>

    <div class="card" style="margin-top:20px;">
      <div class="card-head"><div><h3>Recent Bills</h3></div></div>
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>Bill No</th><th>Patient</th><th>Treatment</th><th>Total</th><th>Issued</th><th></th></tr></thead>
          <tbody id="recent-bills"><tr><td colspan="6" style="text-align:center;padding:24px;"><div class="spinner" style="margin:0 auto;"></div></td></tr></tbody>
        </table>
      </div>
    </div>
  `;

  const findBill = async (no) => {
    const el = $("#bill-result");
    el.innerHTML = `<div class="spinner"></div>`;
    try {
      const appt = await API.get(`/api/appointments/lookup/${encodeURIComponent(no)}`);
      if (appt.billed) {
        const bill = await API.get(`/api/bills/by-appointment/${encodeURIComponent(no)}`);
        el.innerHTML = receiptCard(bill);
        $("#bill-print", el)?.addEventListener("click", () => window.print());
      } else if (appt.status === "CANCELLED") {
        el.innerHTML = `<div class="empty-state">${ICON.xCircle}<div>This appointment was cancelled and cannot be billed.</div></div>`;
      } else {
        el.innerHTML = billingForm(appt);
        wireBillingForm(el, appt, () => { loadUnbilled(); loadRecentBills(); });
      }
    } catch (e) {
      el.innerHTML = `<div class="empty-state">${ICON.info}<div>${UI.escapeHtml(e.message)}</div></div>`;
    }
  };

  $("#bill-find").addEventListener("click", () => { const v = $("#bill-no").value.trim(); if (v) findBill(v); });
  $("#bill-no").addEventListener("keydown", (e) => { if (e.key === "Enter") { const v = e.target.value.trim(); if (v) findBill(v); } });

  const loadUnbilled = async () => {
    const box = $("#unbilled-list");
    try {
      const rows = await API.get("/api/appointments?status=COMPLETED&limit=100");
      const pending = rows.filter((r) => !r.billed);
      box.innerHTML = pending.length ? pending.slice(0, 8).map((a) => `
        <div style="display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-bottom:1px solid var(--border);">
          <div>
            <div style="font-weight:700;font-size:12.5px;">${UI.escapeHtml(a.patientName)}</div>
            <div class="cell-muted">${UI.escapeHtml(a.appointmentNo)} &middot; ${UI.escapeHtml(a.treatmentType)}</div>
          </div>
          <button class="btn btn-sm btn-primary" data-no="${a.appointmentNo}">Bill</button>
        </div>`).join("") : `<div class="empty-state" style="padding:20px;">${ICON.checkCircle}<div>All caught up!</div></div>`;
      $$("button[data-no]", box).forEach((b) => b.addEventListener("click", () => { $("#bill-no").value = b.dataset.no; findBill(b.dataset.no); }));
    } catch (e) { box.innerHTML = `<div class="empty-state">${ICON.info}<div>${UI.escapeHtml(e.message)}</div></div>`; }
  };

  const loadRecentBills = async () => {
    const tbody = $("#recent-bills");
    try {
      const bills = await API.get("/api/bills");
      tbody.innerHTML = bills.length ? bills.slice(0, 12).map((b) => `
        <tr>
          <td class="cell-strong">${UI.escapeHtml(b.billNo)}</td>
          <td>${UI.escapeHtml(b.patientName)}</td>
          <td>${UI.escapeHtml(b.treatmentType)}</td>
          <td class="cell-strong">${UI.money(b.total)}</td>
          <td class="cell-muted">${UI.escapeHtml(b.issuedAt || "")}</td>
          <td><button class="btn btn-sm btn-outline" data-no="${b.appointmentNo}">View</button></td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="6">No bills issued yet</td></tr>`;
      $$("button[data-no]", tbody).forEach((b) => b.addEventListener("click", () => { $("#bill-no").value = b.dataset.no; findBill(b.dataset.no); }));
    } catch (e) { tbody.innerHTML = `<tr class="empty-row"><td colspan="6">${UI.escapeHtml(e.message)}</td></tr>`; }
  };

  loadUnbilled();
  loadRecentBills();

  if (params?.appointmentNo) { $("#bill-no").value = params.appointmentNo; findBill(params.appointmentNo); }
};

function billingForm(appt) {
  return `
    <div class="card">
      <div class="card-head">
        <div><h3>${UI.escapeHtml(appt.appointmentNo)} - Calculate Bill</h3><div class="sub">${UI.escapeHtml(appt.patientName)} &middot; ${UI.escapeHtml(appt.treatmentType)}</div></div>
        ${UI.statusBadge(appt.status)}
      </div>
      <div class="card-pad">
        <div class="grid grid-2" style="margin-bottom:16px;">
          <div><div class="section-sub" style="font-weight:800;color:var(--ink-700);">Treatment Cost</div><div style="font-weight:700;">${UI.money(appt.treatmentCost)}</div></div>
          <div><div class="section-sub" style="font-weight:800;color:var(--ink-700);">Consultation Fee</div><div style="font-weight:700;">${UI.money(appt.consultationFee)}</div></div>
        </div>
        <div class="field">
          <label>Pricing / Concession</label>
          <select id="pricing-select">${appt._strategies || ""}</select>
        </div>
        <div class="field">
          <label>Payment Method</label>
          <select id="payment-method">
            <option value="CASH">Cash</option>
            <option value="CARD">Card</option>
            <option value="ONLINE">Online Transfer</option>
            <option value="INSURANCE">Insurance</option>
          </select>
        </div>
        <div class="card" style="background:var(--yellow-50);border-color:var(--yellow-200);padding:14px 16px;margin-bottom:16px;" id="preview-box">
          <div class="spinner" style="margin:8px auto;"></div>
        </div>
        <button class="btn btn-primary btn-block" id="issue-bill-btn">${ICON.receipt} Calculate &amp; Issue Bill</button>
      </div>
    </div>
  `;
}

async function wireBillingForm(root, appt, onIssued) {
  const strategies = await API.get("/api/bills/strategies");
  const select = $("#pricing-select", root);
  select.innerHTML = strategies.map((s) => `<option value="${s.code}">${UI.escapeHtml(s.label)}</option>`).join("");

  const updatePreview = async () => {
    const box = $("#preview-box", root);
    box.innerHTML = `<div class="spinner" style="margin:8px auto;"></div>`;
    try {
      const bill = await API.post("/api/bills/preview", { appointmentId: appt.id, pricingCode: select.value });
      box.innerHTML = `
        <div class="receipt-row"><span class="k">Sub Total</span><span>${UI.money(bill.subTotal)}</span></div>
        <div class="receipt-row"><span class="k">Discount</span><span>- ${UI.money(bill.discount)}</span></div>
        ${bill.tax > 0 ? `<div class="receipt-row"><span class="k">Tax</span><span>${UI.money(bill.tax)}</span></div>` : ""}
        <div class="receipt-row total"><span>Total Payable</span><span>${UI.money(bill.total)}</span></div>
      `;
    } catch (e) { box.innerHTML = `<div class="cell-muted">${UI.escapeHtml(e.message)}</div>`; }
  };
  select.addEventListener("change", updatePreview);
  updatePreview();

  $("#issue-bill-btn", root).addEventListener("click", async () => {
    const btn = $("#issue-bill-btn", root);
    btn.disabled = true;
    try {
      const bill = await API.post("/api/bills", {
        appointmentId: appt.id, pricingCode: select.value, paymentMethod: $("#payment-method", root).value,
      });
      UI.toast("Bill issued successfully", "success");
      root.innerHTML = receiptCard(bill);
      $("#bill-print", root)?.addEventListener("click", () => window.print());
      onIssued && onIssued();
    } catch (e) {
      UI.toast(e.message, "error");
      btn.disabled = false;
    }
  });
}

function receiptCard(b) {
  const clinic = "Sunrise Dental Clinic";
  return `
    <div class="card">
      <div class="card-pad" id="print-area">
        <div class="receipt">
          <div class="receipt-head">
            <div class="clinic-name">🦷 ${clinic}</div>
            <div class="clinic-meta">No. 45, Hospital Road, Colombo 05 &middot; 011-2 555 777<br/>Official Receipt</div>
          </div>
          <div class="receipt-row"><span class="k">Bill No.</span><span class="cell-strong">${UI.escapeHtml(b.billNo)}</span></div>
          <div class="receipt-row"><span class="k">Appointment No.</span><span>${UI.escapeHtml(b.appointmentNo)}</span></div>
          <div class="receipt-row"><span class="k">Patient</span><span>${UI.escapeHtml(b.patientName)}</span></div>
          <div class="receipt-row"><span class="k">Dentist</span><span>${UI.escapeHtml(b.dentistName)}</span></div>
          <div class="receipt-row"><span class="k">Treatment</span><span>${UI.escapeHtml(b.treatmentType)}</span></div>
          <div class="receipt-row"><span class="k">Visit Date</span><span>${UI.dateNice(b.date)} ${UI.timeNice(b.time)}</span></div>
          <div class="receipt-row"><span class="k">Issued</span><span>${UI.escapeHtml(b.issuedAt || "")}</span></div>
          <hr class="hr"/>
          <div class="receipt-row"><span class="k">Treatment Cost</span><span>${UI.money(b.treatmentCost)}</span></div>
          <div class="receipt-row"><span class="k">Consultation Fee</span><span>${UI.money(b.consultationFee)}</span></div>
          <div class="receipt-row"><span class="k">Concession (${UI.escapeHtml(b.pricingStrategy)})</span><span>- ${UI.money(b.discount)}</span></div>
          ${b.tax > 0 ? `<div class="receipt-row"><span class="k">Tax</span><span>${UI.money(b.tax)}</span></div>` : ""}
          <div class="receipt-row total"><span>Total Paid</span><span>${UI.money(b.total)}</span></div>
          <div style="text-align:center;">
            <span class="badge badge-completed receipt-badge">Payment via ${UI.escapeHtml(b.paymentMethod)}</span>
          </div>
        </div>
      </div>
      <div class="modal-foot" style="border-top:1px solid var(--border);">
        <button class="btn btn-primary" id="bill-print">${ICON.print} Print Receipt</button>
      </div>
    </div>
  `;
}

/* ----------------------------- Patients ----------------------------- */
Views.patients = async (view) => {
  const state = { q: "" };
  const draw = async () => {
    const rows = await API.get("/api/patients" + (state.q ? `?q=${encodeURIComponent(state.q)}` : ""));
    view.innerHTML = `
      <div class="page-head">
        <div class="search-box">${ICON.search}<input id="pat-q" placeholder="Search by name, contact or address..." value="${UI.escapeHtml(state.q)}" /></div>
        <button class="btn btn-primary" id="pat-new">${ICON.plus} Add Patient</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="data">
            <thead><tr><th>Name</th><th>Contact</th><th>Address</th><th>Visits</th><th></th></tr></thead>
            <tbody id="pat-tbody"></tbody>
          </table>
        </div>
      </div>
    `;
    const tbody = $("#pat-tbody");
    tbody.innerHTML = rows.length ? rows.map((p) => `
      <tr data-id="${p.id}">
        <td class="cell-strong">${UI.escapeHtml(p.name)}</td>
        <td>${UI.escapeHtml(p.contactNo || "-")}</td>
        <td class="cell-muted">${UI.escapeHtml(p.address || "-")}</td>
        <td>${p.visitCount}</td>
        <td>
          <div class="row-actions">
            <button class="btn btn-sm btn-outline btn-icon" data-act="history" title="Treatment history">${ICON.history}</button>
            <button class="btn btn-sm btn-outline btn-icon" data-act="edit" title="Edit">${ICON.edit}</button>
            <button class="btn btn-sm btn-danger btn-icon" data-act="delete" title="Delete">${ICON.trash}</button>
          </div>
        </td>
      </tr>`).join("") : `<tr class="empty-row"><td colspan="5">${ICON.users}<div>No patients found</div></td></tr>`;

    $("#pat-new").addEventListener("click", () => openPatientForm(draw));
    $("#pat-q").addEventListener("input", UI.debounce((e) => { state.q = e.target.value; draw(); }, 350));

    $$("tr[data-id]", tbody).forEach((tr) => {
      const id = Number(tr.dataset.id);
      const p = rows.find((r) => r.id === id);
      tr.querySelector('[data-act="edit"]').addEventListener("click", () => openPatientForm(draw, p));
      tr.querySelector('[data-act="history"]').addEventListener("click", () => openPatientHistory(p));
      tr.querySelector('[data-act="delete"]').addEventListener("click", async () => {
        const ok = await UI.confirmDialog(`Delete patient record for ${p.name}? This cannot be undone.`, { okLabel: "Delete", danger: true });
        if (!ok) return;
        try { await API.del(`/api/patients/${id}`); UI.toast("Patient deleted", "success"); draw(); }
        catch (e) { UI.toast(e.message, "error"); }
      });
    });
  };
  await draw();
};

function openPatientForm(onSaved, existing) {
  const isEdit = !!existing;
  const bodyHtml = `
    <form id="pat-form">
      <div class="field"><label>Full Name *</label><input name="name" required value="${UI.escapeHtml(existing?.name || "")}" /></div>
      <div class="field"><label>Contact No *</label><input name="contactNo" required value="${UI.escapeHtml(existing?.contactNo || "")}" placeholder="07XXXXXXXX" /></div>
      <div class="field"><label>Address</label><input name="address" value="${UI.escapeHtml(existing?.address || "")}" /></div>
      <div class="field" style="margin-bottom:0;"><label>Email</label><input name="email" type="email" value="${UI.escapeHtml(existing?.email || "")}" /></div>
    </form>
  `;
  const { close } = UI.openModal({
    title: isEdit ? "Edit Patient" : "Add Patient",
    bodyHtml,
    footHtml: `<button class="btn btn-outline" data-cancel>Cancel</button><button class="btn btn-primary" id="pat-save">${ICON.check} Save</button>`,
    onMount: (overlay) => {
      overlay.querySelector("[data-cancel]").addEventListener("click", () => close());
      overlay.querySelector("#pat-save").addEventListener("click", async () => {
        const form = $("#pat-form", overlay);
        if (!form.reportValidity()) return;
        const payload = Object.fromEntries(new FormData(form).entries());
        try {
          if (isEdit) await API.put(`/api/patients/${existing.id}`, payload);
          else await API.post("/api/patients", payload);
          UI.toast(isEdit ? "Patient updated" : "Patient added", "success");
          close();
          onSaved && onSaved();
        } catch (e) { UI.toast(e.message, "error"); }
      });
    },
  });
}

async function openPatientHistory(p) {
  const { overlay } = UI.openModal({
    title: `${p.name} - Treatment History`,
    wide: true,
    bodyHtml: `<div class="spinner"></div>`,
  });
  try {
    const rows = await API.get(`/api/patients/${p.id}/history`);
    const body = overlay.querySelector(".modal-body");
    body.innerHTML = rows.length ? `
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>Reference</th><th>Dentist</th><th>Treatment</th><th>Date</th><th>Status</th><th>Billed</th></tr></thead>
          <tbody>
            ${rows.map((a) => `
              <tr>
                <td class="cell-strong">${UI.escapeHtml(a.appointmentNo)}</td>
                <td>${UI.escapeHtml(a.dentistName)}</td>
                <td>${UI.escapeHtml(a.treatmentType)}</td>
                <td>${UI.dateNice(a.date)}</td>
                <td>${UI.statusBadge(a.status)}</td>
                <td>${a.billed ? `<span class="badge badge-completed">Billed</span>` : `<span class="cell-muted">-</span>`}</td>
              </tr>`).join("")}
          </tbody>
        </table>
      </div>` : `<div class="empty-state">${ICON.history}<div>No previous visits on record</div></div>`;
  } catch (e) {
    overlay.querySelector(".modal-body").innerHTML = `<div class="empty-state">${ICON.info}<div>${UI.escapeHtml(e.message)}</div></div>`;
  }
}

/* ----------------------------- Reports ----------------------------- */
Views.reports = async (view) => {
  view.innerHTML = `
    <div class="tabs">
      <button class="tab-btn active" data-tab="daily">Daily Appointments</button>
      <button class="tab-btn" data-tab="revenue">Revenue Report</button>
      <button class="tab-btn" data-tab="treatments">Most Common Treatments</button>
      <button class="tab-btn" data-tab="dentists">Dentist Workload</button>
      <button class="tab-btn" data-tab="patients">Top Patients</button>
    </div>
    <div id="report-body"><div class="spinner"></div></div>
  `;
  const body = $("#report-body");
  const tabs = { daily: reportDaily, revenue: reportRevenue, treatments: reportTreatments, dentists: reportDentists, patients: reportPatients };
  $$(".tab-btn", view).forEach((b) => b.addEventListener("click", () => {
    $$(".tab-btn", view).forEach((x) => x.classList.remove("active"));
    b.classList.add("active");
    tabs[b.dataset.tab](body);
  }));
  reportDaily(body);
};

async function reportDaily(body) {
  body.innerHTML = `<div class="spinner"></div>`;
  const daysSel = `<select class="filter-select" id="days-sel"><option value="7">Last 7 days</option><option value="14" selected>Last 14 days</option><option value="30">Last 30 days</option><option value="60">Last 60 days</option></select>`;
  const load = async (days) => {
    const rows = await API.get(`/api/reports/appointments-trend?days=${days}`);
    body.innerHTML = `
      <div class="card">
        <div class="card-head"><div><h3>Daily Appointments Report</h3><div class="sub">Number of appointments booked per day</div></div>${daysSel}</div>
        <div class="card-pad">
          ${rows.length ? renderBars(rows, { fmt: (v) => v }) : `<div class="empty-state">${ICON.calendar}<div>No appointments in this range</div></div>`}
        </div>
        <div class="table-wrap">
          <table class="data"><thead><tr><th>Date</th><th>Appointments</th></tr></thead>
          <tbody>${rows.map((r) => `<tr><td>${UI.dateNice(r.label)}</td><td class="cell-strong">${r.value}</td></tr>`).join("") || `<tr class="empty-row"><td colspan="2">No data</td></tr>`}</tbody></table>
        </div>
      </div>`;
    $("#days-sel").value = String(days);
    $("#days-sel").addEventListener("change", (e) => load(e.target.value));
  };
  load(14);
}

async function reportRevenue(body) {
  const today = todayStr();
  const monthStart = today.slice(0, 8) + "01";
  body.innerHTML = `<div class="spinner"></div>`;
  const load = async (from, to) => {
    const rows = await API.get(`/api/reports/revenue?from=${from}&to=${to}`);
    const total = rows.reduce((s, r) => s + Number(r.total), 0);
    body.innerHTML = `
      <div class="card">
        <div class="card-head">
          <div><h3>Revenue Report</h3><div class="sub">${rows.length} bill(s) &middot; total ${UI.money(total)}</div></div>
          <div class="toolbar">
            <input type="date" id="rev-from" value="${from}" style="padding:8px 10px;border-radius:8px;border:1.5px solid var(--border);" />
            <input type="date" id="rev-to" value="${to}" style="padding:8px 10px;border-radius:8px;border:1.5px solid var(--border);" />
            <button class="btn btn-outline btn-sm" id="rev-apply">Apply</button>
            <button class="btn btn-primary btn-sm" id="rev-print">${ICON.print} Print</button>
          </div>
        </div>
        <div class="table-wrap" id="print-area">
          <table class="data">
            <thead><tr><th>Bill No</th><th>Appt No</th><th>Patient</th><th>Treatment</th><th>Dentist</th><th>Discount</th><th>Total</th><th>Issued</th></tr></thead>
            <tbody>${rows.length ? rows.map((r) => `
              <tr>
                <td class="cell-strong">${UI.escapeHtml(r.bill_no)}</td>
                <td>${UI.escapeHtml(r.appointment_no)}</td>
                <td>${UI.escapeHtml(r.patient_name)}</td>
                <td>${UI.escapeHtml(r.treatment_type)}</td>
                <td>${UI.escapeHtml(r.dentist_name)}</td>
                <td>${UI.money(r.discount)}</td>
                <td class="cell-strong">${UI.money(r.total)}</td>
                <td class="cell-muted">${UI.escapeHtml(r.issued_at)}</td>
              </tr>`).join("") : `<tr class="empty-row"><td colspan="8">No bills in this date range</td></tr>`}
            </tbody>
          </table>
        </div>
      </div>`;
    $("#rev-apply").addEventListener("click", () => load($("#rev-from").value, $("#rev-to").value));
    $("#rev-print").addEventListener("click", () => window.print());
  };
  load(monthStart, today);
}

async function reportTreatments(body) {
  body.innerHTML = `<div class="spinner"></div>`;
  const rows = await API.get("/api/reports/top-treatments?limit=10");
  body.innerHTML = `
    <div class="card">
      <div class="card-head"><div><h3>Most Common Treatments</h3><div class="sub">Ranked by number of bookings</div></div></div>
      <div class="card-pad">${renderBars(rows, { fmt: (v) => v })}</div>
      <div class="table-wrap">
        <table class="data"><thead><tr><th>Treatment</th><th>Bookings</th><th>Revenue</th></tr></thead>
        <tbody>${rows.map((r) => `<tr><td class="cell-strong">${UI.escapeHtml(r.label)}</td><td>${r.value}</td><td>${UI.money(r.revenue)}</td></tr>`).join("") || `<tr class="empty-row"><td colspan="3">No data</td></tr>`}</tbody></table>
      </div>
    </div>`;
}

async function reportDentists(body) {
  body.innerHTML = `<div class="spinner"></div>`;
  const rows = await API.get("/api/reports/dentist-workload");
  body.innerHTML = `
    <div class="card">
      <div class="card-head"><div><h3>Dentist Workload</h3><div class="sub">Appointments handled and revenue generated</div></div></div>
      <div class="card-pad">${renderBars(rows, { fmt: (v) => v })}</div>
      <div class="table-wrap">
        <table class="data"><thead><tr><th>Dentist</th><th>Appointments</th><th>Revenue</th></tr></thead>
        <tbody>${rows.map((r) => `<tr><td class="cell-strong">${UI.escapeHtml(r.label)}</td><td>${r.value}</td><td>${UI.money(r.revenue)}</td></tr>`).join("") || `<tr class="empty-row"><td colspan="3">No data</td></tr>`}</tbody></table>
      </div>
    </div>`;
}

async function reportPatients(body) {
  body.innerHTML = `<div class="spinner"></div>`;
  const rows = await API.get("/api/reports/top-patients?limit=10");
  body.innerHTML = `
    <div class="card">
      <div class="card-head"><div><h3>Top Patients</h3><div class="sub">Most frequent visitors</div></div></div>
      <div class="table-wrap">
        <table class="data"><thead><tr><th>Patient</th><th>Contact</th><th>Visits</th><th>Revenue</th></tr></thead>
        <tbody>${rows.map((r) => `<tr><td class="cell-strong">${UI.escapeHtml(r.label)}</td><td>${UI.escapeHtml(r.contact_no || "-")}</td><td>${r.value}</td><td>${UI.money(r.revenue)}</td></tr>`).join("") || `<tr class="empty-row"><td colspan="4">No data</td></tr>`}</tbody></table>
      </div>
    </div>`;
}

/* ----------------------------- Admin: Dentists ----------------------------- */
Views.adminDentists = async (view) => {
  const draw = async () => {
    const rows = await API.get("/api/dentists?activeOnly=false");
    view.innerHTML = `
      <div class="page-head">
        <p class="section-sub" style="margin:0;">Dentists appear in the appointment booking form when marked active.</p>
        <button class="btn btn-primary" id="d-new">${ICON.plus} Add Dentist</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="data"><thead><tr><th>Name</th><th>Specialization</th><th>Consultation Fee</th><th>Status</th><th></th></tr></thead>
          <tbody id="d-tbody"></tbody></table>
        </div>
      </div>`;
    $("#d-tbody").innerHTML = rows.length ? rows.map((d) => `
      <tr data-id="${d.id}">
        <td class="cell-strong">${UI.escapeHtml(d.name)}</td>
        <td>${UI.escapeHtml(d.specialization || "-")}</td>
        <td>${UI.money(d.consultationFee)}</td>
        <td>${d.active ? `<span class="badge badge-completed">Active</span>` : `<span class="badge badge-cancelled">Inactive</span>`}</td>
        <td><div class="row-actions">
          <button class="btn btn-sm btn-outline btn-icon" data-act="edit">${ICON.edit}</button>
          <button class="btn btn-sm btn-danger btn-icon" data-act="delete">${ICON.trash}</button>
        </div></td>
      </tr>`).join("") : `<tr class="empty-row"><td colspan="5">No dentists yet</td></tr>`;

    $("#d-new").addEventListener("click", () => openDentistForm(draw));
    $$("tr[data-id]", view).forEach((tr) => {
      const d = rows.find((r) => r.id === Number(tr.dataset.id));
      tr.querySelector('[data-act="edit"]').addEventListener("click", () => openDentistForm(draw, d));
      tr.querySelector('[data-act="delete"]').addEventListener("click", async () => {
        const ok = await UI.confirmDialog(`Delete Dr. ${d.name}? This is only possible if they have no appointments.`, { okLabel: "Delete", danger: true });
        if (!ok) return;
        try { await API.del(`/api/dentists/${d.id}`); UI.toast("Dentist deleted", "success"); loadLookups(); draw(); }
        catch (e) { UI.toast(e.message, "error"); }
      });
    });
  };
  await draw();
};

function openDentistForm(onSaved, existing) {
  const isEdit = !!existing;
  const bodyHtml = `
    <form id="d-form">
      <div class="field"><label>Full Name *</label><input name="name" required value="${UI.escapeHtml(existing?.name || "")}" placeholder="Dr. ..." /></div>
      <div class="field"><label>Specialization</label><input name="specialization" value="${UI.escapeHtml(existing?.specialization || "")}" /></div>
      <div class="field"><label>Consultation Fee (Rs.) *</label><input name="consultationFee" type="number" min="0" step="0.01" required value="${existing?.consultationFee ?? ""}" /></div>
      <div class="field" style="margin-bottom:0;">
        <label><input type="checkbox" name="active" ${existing?.active !== false ? "checked" : ""} style="width:auto;margin-right:8px;" />Active (visible when booking)</label>
      </div>
    </form>`;
  const { close } = UI.openModal({
    title: isEdit ? "Edit Dentist" : "Add Dentist",
    bodyHtml,
    footHtml: `<button class="btn btn-outline" data-cancel>Cancel</button><button class="btn btn-primary" id="d-save">${ICON.check} Save</button>`,
    onMount: (overlay) => {
      overlay.querySelector("[data-cancel]").addEventListener("click", () => close());
      overlay.querySelector("#d-save").addEventListener("click", async () => {
        const form = $("#d-form", overlay);
        if (!form.reportValidity()) return;
        const fd = new FormData(form);
        const payload = { name: fd.get("name"), specialization: fd.get("specialization"), consultationFee: Number(fd.get("consultationFee")), active: fd.get("active") === "on" };
        try {
          if (isEdit) await API.put(`/api/dentists/${existing.id}`, payload);
          else await API.post("/api/dentists", payload);
          UI.toast("Saved successfully", "success");
          close(); loadLookups(); onSaved && onSaved();
        } catch (e) { UI.toast(e.message, "error"); }
      });
    },
  });
}

/* ----------------------------- Admin: Treatments ----------------------------- */
Views.adminTreatments = async (view) => {
  const draw = async () => {
    const rows = await API.get("/api/treatments?activeOnly=false");
    view.innerHTML = `
      <div class="page-head">
        <p class="section-sub" style="margin:0;">Treatment types and their base cost, used to auto-calculate bills.</p>
        <button class="btn btn-primary" id="t-new">${ICON.plus} Add Treatment</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="data"><thead><tr><th>Treatment</th><th>Base Cost</th><th>Duration</th><th>Status</th><th></th></tr></thead>
          <tbody id="t-tbody"></tbody></table>
        </div>
      </div>`;
    $("#t-tbody").innerHTML = rows.length ? rows.map((t) => `
      <tr data-id="${t.id}">
        <td class="cell-strong">${UI.escapeHtml(t.treatmentType)}</td>
        <td>${UI.money(t.baseCost)}</td>
        <td>${t.durationMin} min</td>
        <td>${t.active ? `<span class="badge badge-completed">Active</span>` : `<span class="badge badge-cancelled">Inactive</span>`}</td>
        <td><div class="row-actions">
          <button class="btn btn-sm btn-outline btn-icon" data-act="edit">${ICON.edit}</button>
          <button class="btn btn-sm btn-danger btn-icon" data-act="delete">${ICON.trash}</button>
        </div></td>
      </tr>`).join("") : `<tr class="empty-row"><td colspan="5">No treatments yet</td></tr>`;

    $("#t-new").addEventListener("click", () => openTreatmentForm(draw));
    $$("tr[data-id]", view).forEach((tr) => {
      const t = rows.find((r) => r.id === Number(tr.dataset.id));
      tr.querySelector('[data-act="edit"]').addEventListener("click", () => openTreatmentForm(draw, t));
      tr.querySelector('[data-act="delete"]').addEventListener("click", async () => {
        const ok = await UI.confirmDialog(`Delete "${t.treatmentType}"? This is only possible if it has no appointments.`, { okLabel: "Delete", danger: true });
        if (!ok) return;
        try { await API.del(`/api/treatments/${t.id}`); UI.toast("Treatment deleted", "success"); loadLookups(); draw(); }
        catch (e) { UI.toast(e.message, "error"); }
      });
    });
  };
  await draw();
};

function openTreatmentForm(onSaved, existing) {
  const isEdit = !!existing;
  const bodyHtml = `
    <form id="t-form">
      <div class="field"><label>Treatment Type *</label><input name="treatmentType" required value="${UI.escapeHtml(existing?.treatmentType || "")}" /></div>
      <div class="grid grid-2">
        <div class="field"><label>Base Cost (Rs.) *</label><input name="baseCost" type="number" min="0" step="0.01" required value="${existing?.baseCost ?? ""}" /></div>
        <div class="field"><label>Duration (minutes)</label><input name="durationMin" type="number" min="5" step="5" value="${existing?.durationMin ?? 30}" /></div>
      </div>
      <div class="field" style="margin-bottom:0;">
        <label><input type="checkbox" name="active" ${existing?.active !== false ? "checked" : ""} style="width:auto;margin-right:8px;" />Active (available when booking)</label>
      </div>
    </form>`;
  const { close } = UI.openModal({
    title: isEdit ? "Edit Treatment" : "Add Treatment",
    bodyHtml,
    footHtml: `<button class="btn btn-outline" data-cancel>Cancel</button><button class="btn btn-primary" id="t-save">${ICON.check} Save</button>`,
    onMount: (overlay) => {
      overlay.querySelector("[data-cancel]").addEventListener("click", () => close());
      overlay.querySelector("#t-save").addEventListener("click", async () => {
        const form = $("#t-form", overlay);
        if (!form.reportValidity()) return;
        const fd = new FormData(form);
        const payload = { treatmentType: fd.get("treatmentType"), baseCost: Number(fd.get("baseCost")), durationMin: Number(fd.get("durationMin") || 30), active: fd.get("active") === "on" };
        try {
          if (isEdit) await API.put(`/api/treatments/${existing.id}`, payload);
          else await API.post("/api/treatments", payload);
          UI.toast("Saved successfully", "success");
          close(); loadLookups(); onSaved && onSaved();
        } catch (e) { UI.toast(e.message, "error"); }
      });
    },
  });
}

/* ----------------------------- Admin: Users ----------------------------- */
Views.adminUsers = async (view) => {
  const draw = async () => {
    const rows = await API.get("/api/users");
    view.innerHTML = `
      <div class="page-head">
        <p class="section-sub" style="margin:0;">Staff accounts that can sign in to this system.</p>
        <button class="btn btn-primary" id="u-new">${ICON.plus} Add Staff User</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="data"><thead><tr><th>Username</th><th>Full Name</th><th>Role</th><th>Status</th><th></th></tr></thead>
          <tbody id="u-tbody"></tbody></table>
        </div>
      </div>`;
    $("#u-tbody").innerHTML = rows.map((u) => `
      <tr data-id="${u.id}">
        <td class="cell-strong">${UI.escapeHtml(u.username)}</td>
        <td>${UI.escapeHtml(u.fullName)}</td>
        <td>${u.role === "ADMIN" ? `<span class="badge badge-admin">Admin</span>` : `<span class="badge badge-staff">Staff</span>`}</td>
        <td>${u.active ? `<span class="badge badge-completed">Active</span>` : `<span class="badge badge-cancelled">Inactive</span>`}</td>
        <td><div class="row-actions">
          <button class="btn btn-sm btn-outline btn-icon" data-act="edit">${ICON.edit}</button>
          <button class="btn btn-sm btn-danger btn-icon" data-act="delete">${ICON.trash}</button>
        </div></td>
      </tr>`).join("");

    $("#u-new").addEventListener("click", () => openUserForm(draw));
    $$("tr[data-id]", view).forEach((tr) => {
      const u = rows.find((r) => r.id === Number(tr.dataset.id));
      tr.querySelector('[data-act="edit"]').addEventListener("click", () => openUserForm(draw, u));
      tr.querySelector('[data-act="delete"]').addEventListener("click", async () => {
        const ok = await UI.confirmDialog(`Delete staff account "${u.username}"?`, { okLabel: "Delete", danger: true });
        if (!ok) return;
        try { await API.del(`/api/users/${u.id}`); UI.toast("User deleted", "success"); draw(); }
        catch (e) { UI.toast(e.message, "error"); }
      });
    });
  };
  await draw();
};

function openUserForm(onSaved, existing) {
  const isEdit = !!existing;
  const bodyHtml = `
    <form id="u-form">
      <div class="grid grid-2">
        <div class="field"><label>Username *</label><input name="username" required value="${UI.escapeHtml(existing?.username || "")}" /></div>
        <div class="field"><label>Full Name *</label><input name="fullName" required value="${UI.escapeHtml(existing?.fullName || "")}" /></div>
      </div>
      <div class="grid grid-2">
        <div class="field">
          <label>Role *</label>
          <select name="role">
            <option value="STAFF" ${existing?.role !== "ADMIN" ? "selected" : ""}>Staff</option>
            <option value="ADMIN" ${existing?.role === "ADMIN" ? "selected" : ""}>Admin</option>
          </select>
        </div>
        <div class="field"><label>${isEdit ? "New Password (optional)" : "Password *"}</label><input name="password" type="password" ${isEdit ? "" : "required"} placeholder="${isEdit ? "Leave blank to keep current" : "Min. 6 characters"}" /></div>
      </div>
      <div class="field" style="margin-bottom:0;">
        <label><input type="checkbox" name="active" ${existing?.active !== false ? "checked" : ""} style="width:auto;margin-right:8px;" />Active (can sign in)</label>
      </div>
    </form>`;
  const { close } = UI.openModal({
    title: isEdit ? "Edit Staff User" : "Add Staff User",
    bodyHtml,
    footHtml: `<button class="btn btn-outline" data-cancel>Cancel</button><button class="btn btn-primary" id="u-save">${ICON.check} Save</button>`,
    onMount: (overlay) => {
      overlay.querySelector("[data-cancel]").addEventListener("click", () => close());
      overlay.querySelector("#u-save").addEventListener("click", async () => {
        const form = $("#u-form", overlay);
        if (!form.reportValidity()) return;
        const fd = new FormData(form);
        const payload = { username: fd.get("username"), fullName: fd.get("fullName"), role: fd.get("role"), active: fd.get("active") === "on", password: fd.get("password") };
        try {
          if (isEdit) await API.put(`/api/users/${existing.id}`, payload);
          else await API.post("/api/users", payload);
          UI.toast("Saved successfully", "success");
          close(); onSaved && onSaved();
        } catch (e) { UI.toast(e.message, "error"); }
      });
    },
  });
}

/* ----------------------------- Reminders ----------------------------- */
Views.reminders = async (view) => {
  const rows = await API.get("/api/notifications");
  view.innerHTML = `
    <div class="card" style="margin-bottom:20px;background:var(--yellow-50);border-color:var(--yellow-200);">
      <div class="card-pad" style="display:flex;gap:12px;align-items:flex-start;">
        <div style="color:var(--amber-700);">${ICON.bell}</div>
        <p style="margin:0;font-size:13px;color:var(--ink-700);line-height:1.6;">
          Reminders and confirmations are generated automatically whenever an appointment is booked, rescheduled, cancelled, completed, or when you press &ldquo;Send Reminder&rdquo;. This list shows every message that has been queued.
        </p>
      </div>
    </div>
    <div class="card">
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>Recipient</th><th>Subject</th><th>Status</th><th>Queued At</th></tr></thead>
          <tbody>
            ${rows.length ? rows.map((n) => `
              <tr data-id="${n.id}">
                <td>${UI.escapeHtml(n.recipient)}</td>
                <td class="cell-strong">${UI.escapeHtml(n.subject)}</td>
                <td>${n.status === "SENT" ? `<span class="badge badge-sent">Sent</span>` : `<span class="badge badge-queued">Queued</span>`}</td>
                <td class="cell-muted">${UI.escapeHtml(n.createdAt || "")}</td>
              </tr>`).join("") : `<tr class="empty-row"><td colspan="4">${ICON.mail}<div>No reminders yet</div></td></tr>`}
          </tbody>
        </table>
      </div>
    </div>
  `;
  $$("tr[data-id]", view).forEach((tr) => {
    const n = rows.find((r) => r.id === Number(tr.dataset.id));
    tr.style.cursor = "pointer";
    tr.addEventListener("click", () => {
      UI.openModal({ title: n.subject, bodyHtml: `<p style="white-space:pre-wrap;margin:0;font-size:13.5px;line-height:1.7;">${UI.escapeHtml(n.message)}</p>` });
    });
  });
};

/* ----------------------------- Audit ----------------------------- */
Views.audit = async (view) => {
  const state = { entity: "" };
  const draw = async () => {
    const rows = await API.get("/api/audit" + (state.entity ? `?entity=${state.entity}` : ""));
    view.innerHTML = `
      <div class="page-head">
        <select class="filter-select" id="audit-filter">
          <option value="">All Activity</option>
          <option value="APPOINTMENT">Appointments</option>
          <option value="USER">Login / Logout</option>
        </select>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="data">
            <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Reference</th><th>Details</th></tr></thead>
            <tbody>
              ${rows.length ? rows.map((a) => `
                <tr>
                  <td class="cell-muted">${UI.escapeHtml(a.createdAt || "")}</td>
                  <td class="cell-strong">${UI.escapeHtml(a.username || "system")}</td>
                  <td><span class="badge badge-staff">${UI.escapeHtml(a.action)}</span></td>
                  <td>${UI.escapeHtml(a.entity)}</td>
                  <td>${UI.escapeHtml(a.entityRef || "-")}</td>
                  <td class="cell-muted">${UI.escapeHtml(a.details || "-")}</td>
                </tr>`).join("") : `<tr class="empty-row"><td colspan="6">${ICON.history}<div>No activity recorded yet</div></td></tr>`}
            </tbody>
          </table>
        </div>
      </div>`;
    $("#audit-filter").value = state.entity;
    $("#audit-filter").addEventListener("change", (e) => { state.entity = e.target.value; draw(); });
  };
  await draw();
};

/* ----------------------------- Help ----------------------------- */
Views.help = async (view) => {
  view.innerHTML = `
    <div class="grid grid-2-1">
      <div>
        <div class="card" style="margin-bottom:20px;">
          <div class="card-head"><div><h3>Getting Started</h3><div class="sub">A quick walkthrough of the core workflow</div></div></div>
          <div class="card-pad">
            <div class="help-step">
              <div class="num">1</div>
              <div><h4>Register a new appointment</h4><p>Go to <b>Appointments &rarr; Register Appointment</b>. Fill in the patient's details, pick a dentist and treatment, and choose a date &amp; time. A unique reference number (e.g. APT-1001) is generated automatically.</p></div>
            </div>
            <div class="help-step">
              <div class="num">2</div>
              <div><h4>Find appointment details</h4><p>Use <b>Appointments &rarr; Find by Reference No.</b> and type in the appointment number to view full patient, dentist and treatment information at any time.</p></div>
            </div>
            <div class="help-step">
              <div class="num">3</div>
              <div><h4>Manage the visit</h4><p>Confirm, reschedule, send a reminder, or cancel an appointment directly from the appointments list using the action icons on each row.</p></div>
            </div>
            <div class="help-step">
              <div class="num">4</div>
              <div><h4>Calculate &amp; print the bill</h4><p>Once a visit is <b>Completed</b>, open <b>Billing</b>, search the appointment reference, choose a pricing option (standard, senior citizen, insurance or loyalty), and click <b>Calculate &amp; Issue Bill</b>. Print the receipt directly from the browser.</p></div>
            </div>
            <div class="help-step">
              <div class="num">5</div>
              <div><h4>Review reports</h4><p>The <b>Reports</b> section shows daily appointment counts, revenue over a date range, the most common treatments, and dentist workload.</p></div>
            </div>
            <div class="help-step">
              <div class="num">6</div>
              <div><h4>Exit the system</h4><p>Click <b>Sign out</b> at the bottom of the sidebar (or the button below) to safely close your session.</p></div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-head"><div><h3>Frequently Asked Questions</h3></div></div>
          <div class="card-pad">
            <div class="faq"><b>Can two patients book the same dentist at the same time?</b><p>No - the system checks the dentist's schedule and blocks double-booking automatically.</p></div>
            <div class="faq"><b>What happens when I cancel an appointment?</b><p>Its status changes to Cancelled, the slot frees up, and a cancellation notice is queued for the patient (see the Reminders page).</p></div>
            <div class="faq"><b>Can I edit a bill after it's issued?</b><p>Bills are final once issued to keep financial records accurate. Contact an administrator if a correction is required.</p></div>
            <div class="faq"><b>Who can manage dentists, treatments and staff accounts?</b><p>Only users with the <b>Admin</b> role can access the Administration section.</p></div>
          </div>
        </div>
      </div>

      <div>
        <div class="card" style="margin-bottom:20px;">
          <div class="card-head"><div><h3>Need to step away?</h3></div></div>
          <div class="card-pad">
            <p class="section-sub" style="margin-top:0;">Signing out ends your session securely on this device.</p>
            <button class="btn btn-danger btn-block" id="help-logout">${ICON.logout} Sign Out Now</button>
          </div>
        </div>
        <div class="card">
          <div class="card-head"><div><h3>Support</h3></div></div>
          <div class="card-pad">
            <p style="margin:0 0 10px;font-size:13px;">${ICON.phone} <b>011-2 555 777</b></p>
            <p style="margin:0 0 10px;font-size:13px;">${ICON.mail} noreply@brightsmile.lk</p>
            <p style="margin:0;font-size:13px;">${ICON.map} No. 45, Hospital Road, Colombo 05</p>
          </div>
        </div>
      </div>
    </div>
  `;
  $("#help-logout").addEventListener("click", doLogout);
};
