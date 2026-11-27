'use strict';

const reportsState = { request: 0, userId: null, data: null, active: 'desempenho' };
const percent = value => value === null ? '—' : `${numberFormat.format(value)}%`;
const points = value => value === null ? '—' : numberFormat.format(value);
const boundedPercent = value => Math.max(0, Math.min(100, Number(value) || 0));

function resetRelatorios() {
  reportsState.request++;
  reportsState.userId = null;
  reportsState.data = null;
  $('reports-filter').reset();
  $('report-assessments').replaceChildren();
  for (const name of ['desempenho', 'questoes', 'participacao']) {
    $(`chart-${name}`).replaceChildren(); $(`table-${name}`).replaceChildren();
  }
  $('report-benchmark').textContent = '';
}

function reportParams() {
  const params = new URLSearchParams();
  for (const [key, id] of [['inicio', 'report-start'], ['fim', 'report-end'], ['busca', 'report-search'], ['notaMin', 'report-min-grade'], ['notaMax', 'report-max-grade'], ['minimoCorrigidas', 'report-min-finals'], ['minimoRespostas', 'report-min-samples'], ['limite', 'report-limit']]) {
    if ($(id).value !== '') params.set(key, $(id).value);
  }
  const ids = [...$('report-assessments').selectedOptions].map(option => option.value);
  if (ids.length) params.set('avaliacaoIds', ids.join(','));
  return params;
}

async function loadRelatorios() {
  const request = ++reportsState.request;
  const userId = state.user?.id;
  showError('reports-error');
  $('reports-loading').hidden = true;
  $('apply-reports').disabled = false;
  for (const name of ['desempenho', 'questoes', 'participacao']) {
    $(`chart-${name}`).replaceChildren(); $(`table-${name}`).replaceChildren();
  }
  if ($('report-start').value && $('report-end').value && $('report-end').value < $('report-start').value) {
    showError('reports-error', 'A data final deve ser igual ou posterior à inicial.'); return;
  }
  if (Number($('report-min-grade').value) > Number($('report-max-grade').value)) {
    showError('reports-error', 'A nota mínima não pode superar a máxima.'); return;
  }
  $('reports-loading').hidden = false;
  $('apply-reports').disabled = true;
  try {
    const list = await api('/api/avaliacoes');
    if (request !== reportsState.request) return;
    const selected = new Set([...$('report-assessments').selectedOptions].map(option => option.value));
    $('report-assessments').replaceChildren(...list.filter(item => item.status === 'PUBLICADA').map(item => {
      const option = element('option', '', item.titulo); option.value = String(item.id); option.selected = selected.has(option.value); return option;
    }));
    reportsState.userId = userId;
    const params = reportParams();
    const names = ['desempenho', 'questoes', 'participacao'];
    const result = await Promise.all(names.map(name => api(`/api/relatorios/${name}?${params}`)));
    if (request !== reportsState.request || state.user?.id !== userId) return;
    reportsState.data = Object.fromEntries(names.map((name, i) => [name, result[i]]));
    renderReports();
  } catch (error) {
    if (request !== reportsState.request) return;
    showError('reports-error', error.message);
  } finally {
    if (request === reportsState.request) { $('reports-loading').hidden = true; $('apply-reports').disabled = false; }
  }
}

function reportTable(target, caption, columns, rows) {
  const table = element('table', 'report-table');
  table.append(element('caption', 'sr-only', caption));
  const head = element('thead'); const header = element('tr');
  for (const [title] of columns) { const th = element('th', '', title); th.scope = 'col'; header.append(th); }
  head.append(header); table.append(head);
  const body = element('tbody');
  for (const row of rows) {
    const tr = element('tr');
    for (const [, value] of columns) tr.append(element('td', '', value(row)));
    body.append(tr);
  }
  if (!rows.length) {
    const tr = element('tr'); const td = element('td', 'empty-state', 'Nenhum resultado para os filtros escolhidos.');
    td.colSpan = columns.length; tr.append(td); body.append(tr);
  }
  table.append(body); $(target).replaceChildren(table);
}

function chartAxis(container) {
  const axis = element('div', 'chart-axis');
  axis.append(element('span', '', '0%'), element('span', '', '50%'), element('span', '', '100%'));
  container.append(axis);
}

function chartBar(container, label, value, formatted, options = {}) {
  const row = element('div', 'chart-row');
  row.setAttribute('role', 'group'); row.setAttribute('aria-label', `${label}: ${formatted}`);
  const name = element('span', 'chart-label', label); name.title = label;
  const track = element('div', `chart-track${value === null ? ' no-data' : ''}`);
  const fill = element('span', `chart-fill ${options.kind || ''}`); fill.style.width = `${boundedPercent(value)}%`;
  track.append(fill);
  if (options.pending !== undefined) {
    const pending = element('span', 'chart-fill pending'); pending.style.width = `${boundedPercent(options.pending)}%`;
    track.append(pending);
  }
  if (options.benchmark !== null && options.benchmark !== undefined) {
    const marker = element('span', 'chart-benchmark'); marker.style.left = `${boundedPercent(options.benchmark)}%`; track.append(marker);
  }
  row.append(name, track, element('span', 'chart-value', formatted)); container.append(row);
}

function prepareChart(name, rows) {
  const chart = $(`chart-${name}`); chart.replaceChildren();
  if (!rows.length) { chart.append(element('p', 'empty-state', 'Nenhum resultado para os filtros escolhidos.')); return null; }
  chartAxis(chart); return chart;
}

function renderReports() {
  const data = reportsState.data;
  const mean = data.desempenho[0]?.mediaGeralPercentual ?? null;
  $('report-benchmark').textContent = `Barras: média da avaliação. Traço: média geral das notas filtradas (${percent(mean)}).`;
  let chart = prepareChart('desempenho', data.desempenho);
  if (chart) data.desempenho.forEach(row => chartBar(chart, row.titulo, row.mediaPercentual,
    row.mediaPercentual === null ? 'Sem notas' : percent(row.mediaPercentual), { benchmark: mean }));
  reportTable('table-desempenho', 'Desempenho por avaliação', [
    ['Avaliação', r => r.titulo], ['Convidados', r => r.convidados], ['Entregues', r => r.entregues],
    ['Notas consideradas', r => r.notasConsideradas], ['Aguardando correção', r => r.aguardandoCorrecao],
    ['Média / valor', r => `${points(r.mediaNota)} / ${points(r.valorTotal)}`], ['Média (%)', r => percent(r.mediaPercentual)],
    ['Menor nota', r => points(r.menorNota)], ['Maior nota', r => points(r.maiorNota)]
  ], data.desempenho);
  chart = prepareChart('questoes', data.questoes);
  if (chart) data.questoes.forEach(row => chartBar(chart, `Q${row.questaoId}. ${row.enunciado}`, row.percentualErros,
    percent(row.percentualErros), { kind: 'errors' }));
  reportTable('table-questoes', 'Ranking de questões objetivas por erro', [
    ['Posição', r => r.posicao], ['Questão', r => `Q${r.questaoId}. ${r.enunciado}`],
    ['Oportunidades', r => r.oportunidades], ['Respondidas', r => r.respondidas], ['Em branco', r => r.emBranco],
    ['Erros', r => r.erros], ['Erros (%)', r => percent(r.percentualErros)]
  ], data.questoes);
  chart = prepareChart('participacao', data.participacao);
  if (chart) data.participacao.forEach(row => chartBar(chart, row.titulo, row.percentualEntregas,
    row.convidados ? `${row.entregues}/${row.convidados}` : 'Sem alunos',
    { kind: 'delivered', pending: row.convidados ? 100 - (row.percentualEntregas || 0) : 0 }));
  reportTable('table-participacao', 'Participação por avaliação', [
    ['Avaliação', r => r.titulo], ['Convidados', r => r.convidados], ['Entregues', r => r.entregues],
    ['Pendentes', r => r.pendentes], ['Em andamento', r => r.emAndamento], ['Entregas (%)', r => percent(r.percentualEntregas)]
  ], data.participacao);
}

function selectReport(name, focus = false) {
  reportsState.active = name;
  for (const button of document.querySelectorAll('[data-report]')) {
    const active = button.dataset.report === name;
    button.setAttribute('aria-selected', String(active)); button.tabIndex = active ? 0 : -1;
    $(`report-${button.dataset.report}`).hidden = !active;
    if (active && focus) button.focus();
  }
}
for (const button of document.querySelectorAll('[data-report]')) {
  button.addEventListener('click', () => selectReport(button.dataset.report));
  button.addEventListener('keydown', event => {
    const names = ['desempenho', 'questoes', 'participacao'];
    const delta = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
    if (delta) { event.preventDefault(); selectReport(names[(names.indexOf(button.dataset.report) + delta + 3) % 3], true); }
  });
}
$('reports-filter').addEventListener('submit', event => { event.preventDefault(); loadRelatorios(); });
$('reset-reports').addEventListener('click', () => { $('reports-filter').reset(); $('report-limit').value = '10'; loadRelatorios(); });
$('report-limit').addEventListener('change', loadRelatorios);
if (state.user?.perfil === 'PROFESSOR' && state.view === 'relatorios') loadRelatorios();
