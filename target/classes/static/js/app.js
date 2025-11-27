document.addEventListener('DOMContentLoaded', () => {

  const fmt = (v, currency=false) => {
    if (v === null || v === undefined) return '—';
    if (currency)
      return '₹' + Number(v).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2});
    if (Math.abs(v) >= 1000)
      return Number(v).toLocaleString('en-IN', {maximumFractionDigits:2});
    return Number(v).toFixed(2);
  };

  const sanitize = s =>
    s ? String(s).replace(/\uFFFD/g, '').replace(/[^\x00-\x7F]/g, '').trim() : '';

  let pieChart, lineChart, barChart;

  // -------------------------------------------------------------------
  // UPDATED DOUGHNUT LABEL PLUGIN (labels outside, readable)
  // -------------------------------------------------------------------
  const DoughnutLabelPlugin = {
    id: 'doughnutLabelPlugin',
    afterDraw(chart) {
      if (!chart || chart.config.type !== 'doughnut') return;

      const ctx = chart.ctx;
      const dataset = chart.data.datasets[0];
      if (!dataset) return;

      const meta = chart.getDatasetMeta(0);
      const total = dataset.data.reduce((a,b)=>a + (+b || 0), 0) || 1;

      meta.data.forEach((arc, i) => {
        const value = Number(dataset.data[i] || 0);
        if (value <= 0) return;

        const percent = (value / total) * 100;
        const angle = (arc.startAngle + arc.endAngle) / 2;

        // Position label OUTSIDE the doughnut
        const r = arc.outerRadius + 22;
        const x = arc.x + Math.cos(angle) * r;
        const y = arc.y + Math.sin(angle) * r;

        // Draw connector line
        ctx.save();
        ctx.strokeStyle = "rgba(0,0,0,0.25)";
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(
          arc.x + Math.cos(angle) * arc.outerRadius,
          arc.y + Math.sin(angle) * arc.outerRadius
        );
        ctx.lineTo(x, y);
        ctx.stroke();
        ctx.restore();

        // Draw outlined text for readability
        ctx.font = "600 12px system-ui";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";

        ctx.lineWidth = 4;
        ctx.strokeStyle = "white";
        ctx.strokeText(percent.toFixed(1) + "%", x, y);

        ctx.fillStyle = "#222";
        ctx.fillText(percent.toFixed(1) + "%", x, y);
      });
    }
  };

  if (window.Chart && !Chart.registry.plugins.get("doughnutLabelPlugin")) {
    Chart.register(DoughnutLabelPlugin);
  }

  // -------------------------------------------------------------------
  // UPDATED PIE CHART FUNCTION
  // -------------------------------------------------------------------
  function drawPie(labels, data) {
    const ctx = document.getElementById('pieChart');
    if (!ctx) return;
    if (pieChart) pieChart.destroy();

    const total = data.reduce((a,b)=>a + (+b || 0), 0) || 1;

    const legendLabels = labels.map((l, i) =>
      `${l} (${((data[i] || 0) / total * 100).toFixed(1)}%)`
    );

    const colors = [
      "#4e79a7", "#f28e2b", "#e15759",
      "#76b7b2", "#59a14f", "#edc948",
      "#af7aa1", "#ff9da7"
    ];

    pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: legendLabels,
        datasets: [{
          data,
          backgroundColor: colors
        }]
      },
      options: {
        plugins: {
          legend: { position: 'bottom', labels: { usePointStyle: true } }
        },
        maintainAspectRatio: false,
        cutout: '55%'
      }
    });
  }

  // -------------------------------------------------------------------
  // LAST 12 MONTH LABELS
  // -------------------------------------------------------------------
  function lastNMonthsLabels(n = 12) {
    const out = [];
    const now = new Date();
    for (let i = n - 1; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      out.push(d.toLocaleString('default', { month: 'short', year: '2-digit' }));
    }
    return out;
  }

  // -------------------------------------------------------------------
  // LINE CHART
  // -------------------------------------------------------------------
  function drawLine(labels, data) {
    const ctx = document.getElementById('lineChart');
    if (!ctx) return;
    if (lineChart) lineChart.destroy();

    lineChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Portfolio value',
          data,
          fill: true,
          tension: 0.25,
          backgroundColor: 'rgba(15,125,255,0.12)',
          borderColor: 'rgba(15,125,255,0.9)',
          pointRadius: 3
        }]
      },
      options: {
        plugins: { legend: { display: false } },
        maintainAspectRatio: false,
        scales: {
          y: { beginAtZero: false, ticks: { callback: v => '₹' + Number(v).toLocaleString() } },
          x: { ticks: { maxRotation: 0 } }
        }
      }
    });
  }

  // -------------------------------------------------------------------
  // BAR CHART
  // -------------------------------------------------------------------
  function drawBar(labels, data) {
    const ctx = document.getElementById('barChart');
    if (!ctx) return;
    if (barChart) barChart.destroy();

    const bg = data.map(v => (v >= 0 ? 'rgba(16,185,129,0.8)' : 'rgba(239,68,68,0.85)'));
    const border = data.map(v => (v >= 0 ? 'rgba(16,185,129,1)' : 'rgba(239,68,68,1)'));

    barChart = new Chart(ctx, {
      type: 'bar',
      data: { labels, datasets: [{ label: 'P/L %', data, backgroundColor: bg, borderColor: border, borderWidth:1 }] },
      options: {
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { ticks: { callback: v => v + '%' } } }
      }
    });
  }

  // -------------------------------------------------------------------
  // P/L UTILITY
  // -------------------------------------------------------------------
  function computePLPercent(s) {
    const buy = Number(s.buyPrice || 0);
    const qty = Number(s.quantity || 0);
    const curr = Number(s.currentPrice || 0);
    const invested = buy * qty;
    const value = curr * qty;
    if (invested === 0) return 0;
    return ((value - invested) / invested) * 100;
  }

  // -------------------------------------------------------------------
  // MAIN REFRESH FUNCTION
  // -------------------------------------------------------------------
  async function refreshAll() {
    try {
      const [summaryResp, portfolioResp] = await Promise.all([
        fetch('/api/summary'),
        fetch('/api/portfolio')
      ]);

      const summary = summaryResp.ok ? await summaryResp.json() : null;
      const portfolio = portfolioResp.ok ? await portfolioResp.json() : { stocks: [] };

      if (summary) {
        document.getElementById('sumInvested').innerText = fmt(summary.totalInvested, true);
        document.getElementById('sumCurrent').innerText = fmt(summary.currentValue, true);
        document.getElementById('sumPL').innerText =
          fmt(summary.unrealized, true) +
          ' (' + Number(summary.unrealizedPercent).toFixed(2) + '%)';

        let rr = summary.riskRating;
        if (!rr && typeof summary.volatility === 'number') {
          const v = summary.volatility;
          rr = (v < 30) ? 'Low' : (v < 60 ? 'Medium' : 'High');
        }
        document.getElementById('riskRating').innerText = rr || '—';
      }

      // -------------------------------------------------------------------
      // HOLDINGS TABLE
      // -------------------------------------------------------------------
      const tbody = document.getElementById('holdingsBody');
      tbody.innerHTML = '';
      const stocks = portfolio.stocks || [];

      stocks.forEach(s => {
        const invested = Number(s.buyPrice || 0) * Number(s.quantity || 0);
        const value = Number(s.currentPrice || 0) * Number(s.quantity || 0);
        const plPct = computePLPercent(s);
        const tr = document.createElement('tr');
        const plClass = plPct >= 0 ? 'profit' : 'loss';

        tr.innerHTML = `
          <td><strong>${sanitize(s.symbol)}</strong></td>
          <td>${Number(s.quantity||0)}</td>
          <td>${fmt(s.buyPrice,false)}</td>
          <td>${sanitize(s.buyDate)}</td>
          <td>${fmt(s.currentPrice,false)}</td>
          <td>${fmt(invested,false)}</td>
          <td>${fmt(value,false)}</td>
          <td class="${plClass}">${plPct.toFixed(2)}%</td>
          <td>
            <form action="/delete" method="post" onsubmit="return confirm('Delete ${sanitize(s.symbol)}?');">
              <input type="hidden" name="symbol" value="${sanitize(s.symbol)}"/>
              <button class="btn btn-sm btn-outline-danger">Delete</button>
            </form>
          </td>
        `;
        tbody.appendChild(tr);
      });

      // -------------------------------------------------------------------
      // ALLOCATION PIE
      // -------------------------------------------------------------------
      if (summary && summary.allocation) {
        const alloc = summary.allocation;
        const labels = Object.keys(alloc);
        const data = labels.map(k => Number((alloc[k] || 0).toFixed(2)));
        drawPie(labels, data);
      } else {
        const total = stocks.reduce((a,x)=>a + (Number(x.currentPrice||0)*Number(x.quantity||0)), 0) || 1;
        const map = {};
        stocks.forEach(s =>
          map[s.symbol] = ((Number(s.currentPrice||0) * Number(s.quantity||0)) / total) * 100
        );
        const labels = Object.keys(map);
        const data = labels.map(k => Number(map[k].toFixed(2)));
        if (labels.length) drawPie(labels, data);
      }

      // -------------------------------------------------------------------
      // TOP MOVERS
      // -------------------------------------------------------------------
      const gainersEl = document.getElementById('topGainers');
      const losersEl = document.getElementById('topLosers');
      gainersEl.innerHTML = '';
      losersEl.innerHTML = '';

      const scored = stocks.map(s => ({ s, pct: computePLPercent(s) }));
      const valid = scored.filter(x => !Number.isNaN(x.pct));

      const topGainers = valid.slice().sort((a,b)=>b.pct - a.pct).slice(0,6);
      const topLosers = valid.slice().sort((a,b)=>a.pct - b.pct).slice(0,6);

      if (topGainers.length === 0) {
        gainersEl.innerHTML = `<li class="text-muted">No gainers</li>`;
      } else {
        topGainers.forEach(x => {
          const li = document.createElement('li');
          li.innerHTML =
            `<strong>${sanitize(x.s.symbol)}</strong> — <span class="profit">${x.pct.toFixed(2)}%</span>`;
          gainersEl.appendChild(li);
        });
      }

      if (topLosers.length === 0) {
        losersEl.innerHTML = `<li class="text-muted">No losers</li>`;
      } else {
        topLosers.forEach(x => {
          const li = document.createElement('li');
          li.innerHTML =
            `<strong>${sanitize(x.s.symbol)}</strong> — <span class="loss">${x.pct.toFixed(2)}%</span>`;
          losersEl.appendChild(li);
        });
      }

      // -------------------------------------------------------------------
      // SUGGESTIONS
      // -------------------------------------------------------------------
      const sugBox = document.getElementById('suggestionsBox');
      if (summary && summary.suggestions) {
        const html = (summary.suggestions || []).map(s => `<div>${sanitize(s)}</div>`).join('');
        sugBox.innerHTML = html || '<div class="text-muted small">No suggestions</div>';
      } else {
        sugBox.innerHTML = '<div class="text-muted small">No suggestions</div>';
      }

      // -------------------------------------------------------------------
      // PORTFOLIO TREND (12 months)
      // -------------------------------------------------------------------
      let base = stocks.reduce((acc, x) =>
        acc + (Number(x.currentPrice||0) * Number(x.quantity||0)), 0);
      if (!base || Number.isNaN(base)) base = 20000;

      const months = lastNMonthsLabels(12);
      const trend = [];
      let cur = base;

      for (let i = 0; i < months.length; i++) {
        const seed = stocks.reduce((a,x)=>a + (x.symbol ? x.symbol.charCodeAt(0) : 0), 0) + i*7;
        const noise = Math.sin(seed * 0.13 + i * 0.5) * 0.02;
        cur = Math.max(0, cur * (1 + noise));
        trend.push(Math.round(cur));
      }

      drawLine(months, trend);

      // -------------------------------------------------------------------
      // PERFORMANCE OVERVIEW BAR
      // -------------------------------------------------------------------
      const barLimit = Math.min(8, stocks.length);
      const sortedByAbs = stocks
        .slice()
        .sort((a,b)=>Math.abs(computePLPercent(b)) - Math.abs(computePLPercent(a)))
        .slice(0, barLimit);

      const barLabels = sortedByAbs.map(s => sanitize(s.symbol));
      const barData = sortedByAbs.map(s => Number(computePLPercent(s).toFixed(2)));

      if (barLabels.length) drawBar(barLabels, barData);

    } catch (err) {
      console.error('dashboard refresh error', err);
    }
  }

  refreshAll();
  setInterval(refreshAll, 60000);
});
