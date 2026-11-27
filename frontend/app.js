'use strict';

const $ = (id) => document.getElementById(id);
const state = { user: null, csrf: null, page: 0, pageSize: 8, loading: false, request: 0, saving: false };
let toastTimer;

function showError(id, message = '') {
  $(id).textContent = message;
  $(id).hidden = !message;
}

async function csrf() {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  if (!response.ok) throw new Error('Não foi possível iniciar sua sessão. Tente novamente.');
  state.csrf = await response.json();
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers);
  const method = options.method || 'GET';
  if (method !== 'GET') {
    if (!state.csrf) await csrf();
    headers.set(state.csrf.headerName, state.csrf.token);
  }
  let response;
  try {
    response = await fetch(path, { ...options, headers, credentials: 'same-origin' });
  } catch {
    throw new Error('Não foi possível conectar. Confira sua conexão e tente novamente.');
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && path !== '/api/auth/login') {
      showLogin();
      if (path !== '/api/auth/me') showError('login-error', 'Sua sessão terminou. Entre novamente para continuar.');
    }
    if (response.status === 403) state.csrf = null;
    const error = new Error(data?.detail || 'Não foi possível concluir. Tente novamente.');
    error.status = response.status;
    throw error;
  }
  return data;
}

function showLogin() {
  state.user = null;
  state.request++;
  state.loading = false;
  $('boot').hidden = true;
  $('workspace').hidden = true;
  $('login-view').hidden = false;
  if ($('question-dialog').open) $('question-dialog').close();
  if ($('evaluation-dialog').open) $('evaluation-dialog').close();
  if (typeof resetEvaluations === 'function') resetEvaluations();
  if (typeof resetRealizacao === 'function') resetRealizacao();
  if (typeof resetCorrecao === 'function') resetCorrecao();
  if (typeof resetRelatorios === 'function') resetRelatorios();
  $('questions-list').replaceChildren();
  $('senha').value = '';
}

function showWorkspace(user) {
  state.user = user;
  state.page = 0;
  $('boot').hidden = true;
  $('login-view').hidden = true;
  $('workspace').hidden = false;
  $('account-name').textContent = user.nome;
  $('account-role').textContent = user.perfil === 'PROFESSOR' ? 'Professor' : 'Aluno';
  $('avatar').textContent = user.nome.split(/\s+/).map(word => word[0]).slice(0, 2).join('');
  const teacher = user.perfil === 'PROFESSOR';
  $('teacher-view').hidden = !teacher;
  $('student-view').hidden = teacher;
  $('questions-nav').hidden = !teacher;
  $('evaluations-nav').hidden = !teacher;
  $('reports-nav').hidden = !teacher;
  $('evaluations-view').hidden = true;
  $('reports-view').hidden = true;
  $('student-nav').hidden = teacher;
  $('student-name').textContent = user.nome.split(' ')[0];
  if (teacher) navigate(viewFromHash());
  else if (typeof loadMinhasAvaliacoes === 'function') loadMinhasAvaliacoes();
}

function navigate(view) {
  if (state.user?.perfil !== 'PROFESSOR') return;
  state.view = view;
  for (const [name, section, link] of [['questoes', 'teacher-view', 'questions-nav'], ['avaliacoes', 'evaluations-view', 'evaluations-nav'], ['relatorios', 'reports-view', 'reports-nav']]) {
    $(section).hidden = view !== name;
    $(link).classList.toggle('active', view === name);
  }
  if (view === 'avaliacoes') {
    if (typeof loadEvaluations === 'function') loadEvaluations();
  } else if (view === 'relatorios') {
    if (typeof loadRelatorios === 'function') loadRelatorios();
  } else loadQuestions();
}

function viewFromHash() { return ['#avaliacoes', '#relatorios'].includes(location.hash) ? location.hash.slice(1) : 'questoes'; }
window.addEventListener('hashchange', () => navigate(viewFromHash()));

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = $('login-submit');
  if (button.disabled) return;
  showError('login-error');
  button.disabled = true;
  button.textContent = 'Entrando…';
  try {
    const body = new URLSearchParams({ email: $('email').value.trim().toLowerCase(), senha: $('senha').value });
    await api('/api/auth/login', { method: 'POST', body });
    $('senha').value = '';
    await csrf();
    showWorkspace(await api('/api/auth/me'));
  } catch (error) {
    showError('login-error', error.message);
  } finally {
    button.disabled = false;
    button.textContent = 'Entrar ↗';
  }
});

$('logout').addEventListener('click', async () => {
  $('logout').disabled = true;
  try {
    await api('/api/auth/logout', { method: 'POST' });
    state.csrf = null;
    showLogin();
    await csrf();
  } catch (error) {
    notify(error.message);
  } finally {
    $('logout').disabled = false;
  }
});

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function questionCard(question) {
  const card = element('article', 'question-card');
  const top = element('div', 'card-top');
  top.append(element('span', 'question-number', `QUESTÃO ${String(question.id).padStart(2, '0')}`));
  const text = question.tipo === 'TEXTO_LIVRE';
  top.append(element('span', `type-badge${text ? ' text-type' : ''}`, text ? '✎  Texto livre' : '◉  Múltipla escolha'));
  card.append(top, element('h3', '', question.enunciado));
  if (!text) {
    const details = element('details');
    details.append(element('summary', '', 'Ver alternativas e gabarito'));
    const list = element('ol', 'option-list');
    question.alternativas.forEach((alternative, i) => {
      const row = element('li', alternative.correta ? 'correct' : '');
      row.append(element('span', 'option-marker', `${String.fromCharCode(65 + i)}.`), element('span', '', alternative.texto));
      if (alternative.correta) row.append(element('span', 'correct-label', '✓ Correta'));
      list.append(row);
    });
    details.append(list);
    card.append(details);
  }
  return card;
}

async function loadQuestions() {
  const request = ++state.request;
  state.loading = true;
  showError('list-error');
  $('previous-page').disabled = true;
  $('next-page').disabled = true;
  $('questions-list').replaceChildren(element('p', 'empty-state', 'Carregando questões…'));
  const params = new URLSearchParams({ limite: String(state.pageSize), deslocamento: String(state.page * state.pageSize) });
  if ($('type-filter').value) params.set('tipo', $('type-filter').value);
  try {
    const countParams = new URLSearchParams();
    if ($('type-filter').value) countParams.set('tipo', $('type-filter').value);
    const [questions, { total }] = await Promise.all([
      api(`/api/questoes?${params}`), api(`/api/questoes/contagem?${countParams}`)
    ]);
    if (request !== state.request) return;
    $('questions-list').replaceChildren(...questions.map(questionCard));
    if (!questions.length) $('questions-list').append(element('p', 'empty-state', 'Nenhuma questão por aqui. Que tal criar a primeira?'));
    $('list-caption').textContent = `${total} ${total === 1 ? 'questão cadastrada' : 'questões cadastradas'} · ${questions.length} nesta página`;
    $('page-indicator').textContent = `Página ${state.page + 1} de ${Math.max(1, Math.ceil(total / state.pageSize))}`;
    $('previous-page').disabled = state.page === 0;
    $('next-page').disabled = (state.page + 1) * state.pageSize >= total;
  } catch (error) {
    if (request !== state.request) return;
    $('questions-list').replaceChildren();
    showError('list-error', error.message);
    $('list-caption').textContent = '';
    $('previous-page').disabled = state.page === 0;
  } finally {
    if (request === state.request) state.loading = false;
  }
}

$('type-filter').addEventListener('change', () => { state.page = 0; loadQuestions(); });
$('previous-page').addEventListener('click', () => { if (!state.loading && state.page > 0) { state.page--; loadQuestions(); } });
$('next-page').addEventListener('click', () => { if (!state.loading) { state.page++; loadQuestions(); } });

function addAlternative() {
  const row = element('div', 'alternative-row');
  const radio = element('input');
  radio.type = 'radio'; radio.name = 'correta'; radio.required = true;
  const input = element('input', 'option-input');
  input.type = 'text'; input.required = true;
  const remove = element('button', 'icon-button', '×');
  remove.type = 'button';
  remove.addEventListener('click', () => {
    if ($('alternatives').children.length > 2) { row.remove(); labelAlternatives(); }
  });
  row.append(radio, input, remove);
  $('alternatives').append(row);
  labelAlternatives();
}

function labelAlternatives() {
  const rows = [...$('alternatives').children];
  rows.forEach((row, index) => {
    row.querySelector('input[type=radio]').setAttribute('aria-label', `Alternativa ${index + 1} é a correta`);
    row.querySelector('.option-input').setAttribute('aria-label', `Texto da alternativa ${index + 1}`);
    row.querySelector('.option-input').placeholder = `Alternativa ${index + 1}`;
    const remove = row.querySelector('button');
    remove.setAttribute('aria-label', `Remover alternativa ${index + 1}`);
    remove.disabled = rows.length <= 2;
  });
}

function setQuestionType() {
  const text = $('question-type').value === 'TEXTO_LIVRE';
  $('alternatives-section').hidden = text;
  $('alternatives-section').disabled = text;
  $('text-help').hidden = !text;
}

$('new-question').addEventListener('click', () => {
  $('question-form').reset();
  $('alternatives').replaceChildren();
  addAlternative(); addAlternative();
  setQuestionType();
  showError('question-error');
  $('question-dialog').showModal();
  $('statement').focus();
});
$('add-alternative').addEventListener('click', addAlternative);
$('question-type').addEventListener('change', setQuestionType);
function closeDialog() { if (!state.saving) $('question-dialog').close(); }
$('close-dialog').addEventListener('click', closeDialog);
$('cancel-dialog').addEventListener('click', closeDialog);
$('question-dialog').addEventListener('cancel', (event) => { if (state.saving) event.preventDefault(); });

$('question-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  if (state.saving) return;
  const tipo = $('question-type').value;
  const alternativas = tipo === 'TEXTO_LIVRE' ? [] : [...$('alternatives').children].map(row => ({
    texto: row.querySelector('.option-input').value.trim(), correta: row.querySelector('input[type=radio]').checked
  }));
  const enunciado = $('statement').value.trim();
  if (!enunciado || alternativas.some(a => !a.texto)) {
    showError('question-error', 'Preencha os campos com um texto válido.'); return;
  }
  state.saving = true;
  showError('question-error');
  $('save-question').disabled = true;
  $('save-question').textContent = 'Salvando…';
  $('close-dialog').disabled = true;
  $('cancel-dialog').disabled = true;
  let saved = false;
  try {
    await api('/api/questoes', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ tipo, enunciado, alternativas }) });
    saved = true;
    $('question-dialog').close();
    notify('Questão criada. Mais uma ideia na sua coleção.');
    $('type-filter').value = tipo;
    const { total } = await api(`/api/questoes/contagem?tipo=${tipo}`);
    state.page = Math.max(0, Math.floor((total - 1) / state.pageSize));
    await loadQuestions();
  } catch (error) {
    if (saved) notify('A questão foi salva, mas não foi possível atualizar a lista.');
    else showError('question-error', error.message);
  } finally {
    state.saving = false;
    $('save-question').disabled = false;
    $('save-question').textContent = 'Salvar questão ↗';
    $('close-dialog').disabled = false;
    $('cancel-dialog').disabled = false;
  }
});

function notify(message) {
  clearTimeout(toastTimer);
  $('toast').textContent = message;
  $('toast').hidden = false;
  toastTimer = setTimeout(() => { $('toast').hidden = true; }, 4500);
}

(async () => {
  try {
    await csrf();
    showWorkspace(await api('/api/auth/me'));
  } catch (error) {
    showLogin();
    if (error.status !== 401) showError('login-error', error.message);
  }
})();
