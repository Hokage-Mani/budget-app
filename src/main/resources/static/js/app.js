// ---------- State ----------
const state = {
  token: localStorage.getItem('ledger_token') || null,
  userName: localStorage.getItem('ledger_user') || null,
  accounts: [],
  debts: [],
  snapshot: null,
  calendarMonth: new Date().getMonth(),
  calendarYear: new Date().getFullYear(),
  chart: null,
  stompClient: null,
};

const $ = (id) => document.getElementById(id);
const money = (n) => (n < 0 ? '-$' : '$') + Math.abs(n).toFixed(2);

// ---------- Auth ----------
async function api(path, options = {}) {
  const headers = options.headers || {};
  if (state.token) headers['Authorization'] = 'Bearer ' + state.token;
  if (options.body) headers['Content-Type'] = 'application/json';
  const res = await fetch(path, { ...options, headers });
  if (res.status === 401) { logout(); throw new Error('Unauthorized'); }
  if (!res.ok) throw new Error(await res.text());
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

document.getElementById('auth-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const mode = e.submitter.dataset.mode;
  const email = $('auth-email').value;
  const password = $('auth-password').value;
  const displayName = $('auth-name').value;
  try {
    const body = mode === 'register'
      ? JSON.stringify({ email, password, displayName })
      : JSON.stringify({ email, password });
    const res = await fetch(`/api/auth/${mode}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body
    });
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    state.token = data.token;
    state.userName = data.displayName || data.email;
    localStorage.setItem('ledger_token', state.token);
    localStorage.setItem('ledger_user', state.userName);
    boot();
  } catch (err) {
    $('auth-error').textContent = 'Could not sign in: ' + err.message;
  }
});

function logout() {
  state.token = null;
  localStorage.removeItem('ledger_token');
  localStorage.removeItem('ledger_user');
  if (state.stompClient) state.stompClient.deactivate();
  $('app').classList.add('hidden');
  $('auth-screen').classList.remove('hidden');
}
$('logout-btn').addEventListener('click', logout);

// ---------- Boot ----------
async function boot() {
  $('auth-screen').classList.add('hidden');
  $('app').classList.remove('hidden');
  $('user-name').textContent = state.userName;

  const [accounts, debts, snapshot] = await Promise.all([
    api('/api/accounts'), api('/api/debts'), api('/api/dashboard')
  ]);
  state.accounts = accounts;
  state.debts = debts;
  state.snapshot = snapshot;

  render();
  connectLive();
}

// ---------- Live updates ----------
function connectLive() {
  // Decode userId out of the JWT payload (base64 middle segment) so we know which topic to join.
  const payload = JSON.parse(atob(state.token.split('.')[1]));
  const userId = payload.userId;

  const socket = new SockJS('/ws');
  const client = new StompJs.Client({
    webSocketFactory: () => socket,
    reconnectDelay: 3000,
    onConnect: () => {
      $('connection-dot').classList.replace('dot-off', 'dot-on');
      client.subscribe(`/topic/dashboard/${userId}`, (msg) => {
        state.snapshot = JSON.parse(msg.body);
        render();
      });
    },
    onWebSocketClose: () => $('connection-dot').classList.replace('dot-on', 'dot-off'),
  });
  client.activate();
  state.stompClient = client;
}

// ---------- Render ----------
function render() {
  const s = state.snapshot;
  if (!s) return;

  $('total-balance').textContent = money(s.totalBalance);
  $('total-debt').textContent = money(s.totalDebt);

  $('account-list').innerHTML = state.accounts.map(a => `
    <li class="${a.balance >= 0 ? 'positive' : 'negative'}">
      <span class="stripe-name">${a.name}</span>
      <span class="stripe-amount">${money(a.balance)}</span>
    </li>`).join('') || '<li class="stripe-name">No accounts yet</li>';

  $('debt-list').innerHTML = s.activeDebts.map(d => `
    <li class="negative">
      <span class="stripe-name">${d.name} — ${d.percentPaidOff}% paid</span>
      <span class="stripe-amount">${money(d.currentBalance)}</span>
    </li>`).join('') || '<li class="stripe-name">No active debts 🎉</li>';

  $('paid-off-list').innerHTML = s.paidOffDebts.map(d => `
    <li class="positive">
      <span class="stripe-name">${d.name}</span>
      <span class="stripe-amount">Paid off</span>
    </li>`).join('') || '<li class="stripe-name">None yet</li>';

  $('recommendation-list').innerHTML = s.recommendations.map(r => `
    <li>
      <div class="rec-top">
        <span>${r.debtName}</span>
        <span class="rec-amount">${money(r.recommendedAmount)} (${r.recommendedPercent}%)</span>
      </div>
      <div class="rec-note">${r.rationale}</div>
    </li>`).join('') || '<li class="rec-note">Add an active debt and a savings goal to see recommendations.</li>';

  renderChart(s.chartSeries);
  renderCalendar(s.chartSeries, s.activeDebts);
  populateTxSelectors();
}

function renderChart(series) {
  const ctx = document.getElementById('flow-chart');
  const labels = series.map(p => p.date);
  const data = series.map(p => p.amount);
  const colors = data.map(v => v >= 0 ? '#4FBE8E' : '#E1685C');

  if (state.chart) {
    state.chart.data.labels = labels;
    state.chart.data.datasets[0].data = data;
    state.chart.data.datasets[0].backgroundColor = colors;
    state.chart.update();
    return;
  }
  state.chart = new Chart(ctx, {
    type: 'bar',
    data: { labels, datasets: [{ data, backgroundColor: colors, borderRadius: 2 }] },
    options: {
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { color: '#8B93A1', maxTicksLimit: 8 }, grid: { display: false } },
        y: { ticks: { color: '#8B93A1' }, grid: { color: '#2A3340' } }
      }
    }
  });
}

function renderCalendar(series, activeDebts) {
  const year = state.calendarYear, month = state.calendarMonth;
  $('calendar-title').textContent = 'Calendar — ' +
    new Date(year, month, 1).toLocaleString('default', { month: 'long', year: 'numeric' });

  const byDay = {};
  series.forEach(p => {
    const d = new Date(p.date);
    if (d.getFullYear() === year && d.getMonth() === month) {
      const key = d.getDate();
      byDay[key] = byDay[key] || { total: 0 };
      byDay[key].total += p.amount;
    }
  });
  const dueDays = {};
  activeDebts.forEach(d => {
    if (!d.dueDate) return;
    const dd = new Date(d.dueDate);
    if (dd.getFullYear() === year && dd.getMonth() === month) {
      dueDays[dd.getDate()] = dueDays[dd.getDate()] || [];
      dueDays[dd.getDate()].push(d.name);
    }
  });

  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  let html = '';
  for (let i = 0; i < firstDay; i++) html += '<div class="cal-cell empty"></div>';
  for (let day = 1; day <= daysInMonth; day++) {
    const entry = byDay[day];
    const dues = dueDays[day];
    html += `<div class="cal-cell">
      <span class="cal-day-num">${day}</span>
      ${entry ? `<span class="cal-amount ${entry.total >= 0 ? 'positive' : 'negative'}">${money(entry.total)}</span>` : ''}
      ${dues ? dues.map(n => `<span class="cal-due">Due: ${n}</span>`).join('') : ''}
    </div>`;
  }
  $('calendar-grid').innerHTML = html;
}

$('cal-prev').addEventListener('click', () => {
  state.calendarMonth--;
  if (state.calendarMonth < 0) { state.calendarMonth = 11; state.calendarYear--; }
  render();
});
$('cal-next').addEventListener('click', () => {
  state.calendarMonth++;
  if (state.calendarMonth > 11) { state.calendarMonth = 0; state.calendarYear++; }
  render();
});

// ---------- Transaction modal ----------
function populateTxSelectors() {
  $('tx-account').innerHTML = state.accounts.map(a => `<option value="${a.id}">${a.name}</option>`).join('');
  $('tx-debt').innerHTML = '<option value="">None</option>' +
    state.debts.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
}

$('add-transaction-btn').addEventListener('click', () => {
  if (state.accounts.length === 0) {
    alert('Add an account first — a transaction has to belong to one.');
    return;
  }
  $('tx-error').textContent = '';
  $('tx-date').value = new Date().toISOString().slice(0, 10);
  $('tx-modal').showModal();
});
$('tx-cancel').addEventListener('click', () => $('tx-modal').close());
$('tx-category').addEventListener('change', (e) => {
  $('tx-debt-wrap').classList.toggle('hidden', e.target.value !== 'DEBT_PAYMENT');
});

$('tx-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/api/transactions', {
      method: 'POST',
      body: JSON.stringify({
        accountId: Number($('tx-account').value),
        debtId: $('tx-debt').value ? Number($('tx-debt').value) : null,
        description: $('tx-description').value,
        amount: Number($('tx-amount').value),
        category: $('tx-category').value,
        date: $('tx-date').value,
      })
    });
    $('tx-modal').close();
    $('tx-form').reset();
    // No manual refresh needed — the WebSocket broadcast triggered by the server will update the UI.
  } catch (err) {
    $('tx-error').textContent = 'Could not save transaction: ' + err.message;
  }
});

// ---------- Savings goal modal ----------
$('edit-goal-btn').addEventListener('click', () => {
  $('goal-error').textContent = '';
  $('goal-modal').showModal();
});
$('goal-cancel').addEventListener('click', () => $('goal-modal').close());
$('goal-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/api/goals', {
      method: 'POST',
      body: JSON.stringify({
        name: $('goal-name').value,
        targetAmount: Number($('goal-target').value),
        targetDate: $('goal-date').value || null,
        monthlyAllocation: Number($('goal-allocation').value),
        strategy: $('goal-strategy').value,
      })
    });
    $('goal-modal').close();
    $('goal-form').reset();
  } catch (err) {
    $('goal-error').textContent = 'Could not save goal: ' + err.message;
  }
});

// ---------- Add account modal ----------
$('add-account-btn').addEventListener('click', () => {
  $('account-error').textContent = '';
  $('account-modal').showModal();
});
$('account-cancel').addEventListener('click', () => $('account-modal').close());
$('account-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/api/accounts', {
      method: 'POST',
      body: JSON.stringify({
        name: $('account-name').value,
        type: $('account-type').value,
        openingBalance: Number($('account-balance').value),
      })
    });
    $('account-modal').close();
    $('account-form').reset();
    // Account creation only broadcasts over WebSocket when a transaction touches it,
    // so pull both the account list and a fresh dashboard snapshot here directly.
    [state.accounts, state.snapshot] = await Promise.all([api('/api/accounts'), api('/api/dashboard')]);
    render();
  } catch (err) {
    $('account-error').textContent = 'Could not add account: ' + err.message;
  }
});

// ---------- Add debt modal ----------
$('add-debt-btn').addEventListener('click', () => {
  $('debt-error').textContent = '';
  $('debt-modal').showModal();
});
$('debt-cancel').addEventListener('click', () => $('debt-modal').close());
$('debt-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/api/debts', {
      method: 'POST',
      body: JSON.stringify({
        name: $('debt-name').value,
        balance: Number($('debt-balance').value),
        interestRateApr: Number($('debt-apr').value),
        minimumPayment: Number($('debt-min-payment').value),
        dueDate: $('debt-due-date').value || null,
      })
    });
    $('debt-modal').close();
    $('debt-form').reset();
    [state.debts, state.snapshot] = await Promise.all([api('/api/debts'), api('/api/dashboard')]);
    render();
  } catch (err) {
    $('debt-error').textContent = 'Could not add debt: ' + err.message;
  }
});

// ---------- Init ----------
if (state.token) boot();
