// FrutiMarket BD - comportamiento comun
const FM_PALETA = ['#1F7A4D', '#F28C28', '#B0306A', '#E8B200', '#2A9D8F', '#6C4AB6', '#8AB833', '#D1495B', '#3A6EA5', '#9C6644'];
const fmNum = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });

function fmFormato(v, formato) {
  if (formato === 'MONEDA') return '$ ' + fmNum.format(v);
  if (formato === 'KG') return fmNum.format(v) + ' kg';
  if (formato === 'PORCENTAJE') return v.toLocaleString('es-CO', { maximumFractionDigits: 1 }) + ' %';
  return fmNum.format(v);
}

/** Dibuja un grafico de Chart.js con los colores y formatos de la aplicacion. */
function fmGrafico(id, cfg) {
  const el = document.getElementById(id);
  if (!el || !window.Chart) return;
  const circular = cfg.tipo === 'pie' || cfg.tipo === 'doughnut';
  const horizontal = cfg.horizontal === true;
  Chart.defaults.font.family = '"Plus Jakarta Sans", system-ui, sans-serif';
  Chart.defaults.color = '#5b6a60';
  const datasets = cfg.series.map((s, i) => ({
    label: s.nombre,
    data: s.datos,
    backgroundColor: circular ? FM_PALETA.slice(0, s.datos.length) : FM_PALETA[i],
    borderColor: circular ? '#fff' : FM_PALETA[i],
    borderWidth: circular ? 2 : (cfg.tipo === 'line' ? 2.5 : 0),
    borderRadius: cfg.tipo === 'bar' ? 5 : 0,
    tension: .3, pointRadius: cfg.tipo === 'line' ? 3 : 0, fill: false,
    maxBarThickness: 46
  }));
  new Chart(el, {
    type: cfg.tipo,
    data: { labels: cfg.etiquetas, datasets },
    options: {
      responsive: true, maintainAspectRatio: false, indexAxis: horizontal ? 'y' : 'x',
      cutout: cfg.tipo === 'doughnut' ? '58%' : undefined,
      plugins: {
        legend: { display: circular || datasets.length > 1, position: circular ? 'right' : 'bottom', labels: { usePointStyle: true, boxWidth: 8 } },
        tooltip: { callbacks: { label: c => {
          const v = c.parsed !== null && typeof c.parsed === 'object' ? (horizontal ? c.parsed.x : c.parsed.y) : c.parsed;
          if (circular) {
            const t = c.dataset.data.reduce((a, b) => a + b, 0);
            return ` ${c.label}: ${fmFormato(v, cfg.formato)} (${(100 * v / t).toFixed(1)} %)`;
          }
          return ` ${c.dataset.label}: ${fmFormato(v, cfg.formato)}`;
        } } }
      },
      scales: circular ? {} : {
        [horizontal ? 'x' : 'y']: { beginAtZero: true, grid: { color: '#edf0eb' }, border: { display: false },
          ticks: { callback: v => fmFormato(v, cfg.formato).replace('$ ', '$') } },
        [horizontal ? 'y' : 'x']: { grid: { display: false }, border: { display: false } }
      }
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  const lat = document.querySelector('.fm-lateral');
  document.querySelectorAll('[data-menu]').forEach(b => b.addEventListener('click', () => lat.classList.toggle('abierto')));
  document.querySelectorAll('tr[data-href]').forEach(tr => tr.addEventListener('click', e => {
    if (!e.target.closest('a, button, form')) window.location = tr.dataset.href;
  }));
  document.querySelectorAll('form[data-confirmar]').forEach(f => f.addEventListener('submit', e => {
    if (!window.confirm(f.dataset.confirmar)) e.preventDefault();
  }));
});
