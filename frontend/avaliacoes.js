'use strict';

const evaluationState = { list: [], questions: [], students: [], selected: [], participants: new Set(),
  id: null, readonly: false, saving: false, opening: false, request: 0 };
const numberFormat = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

function resetEvaluations() {
  evaluationState.request++;
  evaluationState.list = [];
  evaluationState.questions = [];
  evaluationState.students = [];
  evaluationState.selected = [];
  evaluationState.participants.clear();
  $('evaluations-list').replaceChildren();
  $('selected-questions').replaceChildren();
  $('question-options').replaceChildren();
  $('student-options').replaceChildren();
}

async function loadEvaluations() {
  const request = ++evaluationState.request;
  showError('evaluations-error');
  $('evaluations-list').replaceChildren(element('p', 'empty-state', 'Carregando avaliações…'));
  try {
    const list = await api('/api/avaliacoes');
    if (request !== evaluationState.request) return;
    evaluationState.list = list;
    renderEvaluations();
  } catch (error) {
    if (request !== evaluationState.request) return;
    $('evaluations-list').replaceChildren();
    $('evaluations-caption').textContent = '';
    showError('evaluations-error', error.message);
  }
}

function renderEvaluations() {
  const filter = $('evaluation-filter').value;
  const list = evaluationState.list.filter(item => !filter || item.status === filter);
  $('evaluations-caption').textContent = `${list.length} ${list.length === 1 ? 'avaliação' : 'avaliações'} · apenas as criadas por você`;
  $('evaluations-list').replaceChildren(...list.map(item => {
    const card = element('article', 'evaluation-card');
    const top = element('div', 'card-top');
    top.append(element('span', 'question-number', `AVALIAÇÃO ${String(item.id).padStart(2, '0')}`),
      element('span', `type-badge${item.status === 'RASCUNHO' ? ' text-type' : ''}`, item.status === 'RASCUNHO' ? 'Rascunho' : 'Publicada'));
    card.append(top, element('h3', '', item.titulo));
    if (item.descricao) card.append(element('p', 'muted evaluation-description', item.descricao));
    const meta = element('div', 'evaluation-meta');
    meta.append(element('span', '', `${item.quantidadeQuestoes} questões`),
      element('span', '', `${item.quantidadeAlunos} participantes`),
      element('span', '', `${numberFormat.format(item.valorTotal)} pontos`));
    card.append(meta, element('p', 'evaluation-period', `${dateFormat.format(new Date(item.inicioEm))} → ${dateFormat.format(new Date(item.fimEm))}`));
    const button = element('button', 'button quiet', item.status === 'RASCUNHO' ? 'Editar rascunho →' : 'Ver avaliação →');
    button.type = 'button';
    button.addEventListener('click', () => openEvaluation(item.id));
    card.append(button);
    if (item.status === 'PUBLICADA') {
      const correction = element('button', 'button quiet correction-button', 'Respostas dos alunos →');
      correction.type = 'button';
      correction.addEventListener('click', () => openEntregas(item.id, item.titulo));
      card.append(correction);
    }
    return card;
  }));
  if (!list.length) $('evaluations-list').append(element('p', 'empty-state', 'Nenhuma avaliação por aqui. Comece com um novo rascunho.'));
}

async function questionCatalog() {
  const result = [];
  for (let offset = 0; ; offset += 100) {
    const page = await api(`/api/questoes?limite=100&deslocamento=${offset}`);
    result.push(...page);
    if (page.length < 100) return result;
  }
}

function localDateInput(value) {
  const date = new Date(value);
  const part = value => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${part(date.getMonth() + 1)}-${part(date.getDate())}T${part(date.getHours())}:${part(date.getMinutes())}`;
}

async function openEvaluation(id = null) {
  if (evaluationState.opening || evaluationState.saving) return;
  evaluationState.opening = true;
  $('new-evaluation').disabled = true;
  const userId = state.user?.id;
  try {
    const [questions, students, evaluation] = await Promise.all([
      questionCatalog(), api('/api/alunos'), id === null ? Promise.resolve(null) : api(`/api/avaliacoes/${id}`)
    ]);
    if (state.user?.id !== userId || state.user?.perfil !== 'PROFESSOR') return;
    evaluationState.questions = questions;
    evaluationState.students = students;
    evaluationState.id = evaluation?.id ?? null;
    evaluationState.readonly = evaluation?.status === 'PUBLICADA';
    evaluationState.selected = (evaluation?.questoes || []).map(item => ({ questaoId: item.questaoId, valor: String(item.valor) }));
    evaluationState.participants = new Set(evaluation?.alunos || []);
    $('evaluation-form').reset();
    $('evaluation-name').value = evaluation?.titulo || '';
    $('evaluation-description').value = evaluation?.descricao || '';
    const start = new Date(Date.now() + 3600000);
    $('evaluation-start').value = localDateInput(evaluation?.inicioEm || start);
    $('evaluation-end').value = localDateInput(evaluation?.fimEm || new Date(start.getTime() + 3600000));
    const readonly = evaluationState.readonly;
    $('evaluation-dialog-title').textContent = readonly ? 'Sua avaliação' : id ? 'Editar avaliação' : 'Montar avaliação';
    $('evaluation-fields').disabled = readonly;
    $('question-library').hidden = readonly;
    $('student-tools').hidden = readonly;
    $('save-evaluation').hidden = readonly;
    $('publish-evaluation').hidden = readonly;
    $('evaluation-readonly').hidden = !readonly;
    $('evaluation-publish-help').hidden = readonly;
    $('cancel-evaluation').textContent = readonly ? 'Fechar' : 'Cancelar';
    showError('evaluation-error');
    renderSelectedQuestions(); renderQuestionPicker(); renderStudents();
    $('evaluation-dialog').showModal();
    if (!readonly) $('evaluation-name').focus();
  } catch (error) { notify(error.message); }
  finally { evaluationState.opening = false; $('new-evaluation').disabled = false; }
}

function updateEvaluationTotal() {
  const total = evaluationState.selected.reduce((sum, item) => sum + (Number(item.valor) || 0), 0);
  $('evaluation-total').textContent = `${evaluationState.selected.length} questões · ${numberFormat.format(total)} pontos`;
}

function renderSelectedQuestions() {
  $('selected-questions').replaceChildren(...evaluationState.selected.map((item, index) => {
    const question = evaluationState.questions.find(question => question.id === item.questaoId);
    const row = element('div', 'selected-question');
    row.dataset.questionId = String(item.questaoId);
    row.append(element('span', 'selected-order', String(index + 1).padStart(2, '0')),
      element('p', '', question?.enunciado || `Questão ${item.questaoId}`));
    const controls = element('div', 'selected-controls');
    const value = element('input', 'question-value');
    value.type = 'number'; value.min = '0.01'; value.max = '99999.99'; value.step = '0.01'; value.required = true;
    value.value = item.valor;
    value.setAttribute('aria-label', `Pontuação da questão ${index + 1}`);
    value.addEventListener('input', () => { item.valor = value.value; updateEvaluationTotal(); });
    controls.append(value, element('span', 'points-label', 'pts'));
    if (!evaluationState.readonly) {
      for (const [label, delta] of [['↑', -1], ['↓', 1]]) {
        const move = element('button', 'icon-button', label); move.type = 'button';
        move.setAttribute('aria-label', `${delta < 0 ? 'Subir' : 'Descer'} questão ${index + 1}`);
        move.disabled = index + delta < 0 || index + delta >= evaluationState.selected.length;
        move.addEventListener('click', () => {
          const list = evaluationState.selected;
          [list[index], list[index + delta]] = [list[index + delta], list[index]];
          renderSelectedQuestions();
        });
        controls.append(move);
      }
      const remove = element('button', 'icon-button', '×'); remove.type = 'button';
      remove.setAttribute('aria-label', `Remover questão ${index + 1}`);
      remove.addEventListener('click', () => {
        evaluationState.selected.splice(index, 1); renderSelectedQuestions(); renderQuestionPicker();
      });
      controls.append(remove);
    }
    row.append(controls);
    return row;
  }));
  if (!evaluationState.selected.length) $('selected-questions').append(element('p', 'picker-empty', 'Selecione questões do banco abaixo.'));
  updateEvaluationTotal();
}

function renderQuestionPicker() {
  const search = $('question-search').value.toLocaleLowerCase('pt-BR');
  const questions = evaluationState.questions.filter(item => item.enunciado.toLocaleLowerCase('pt-BR').includes(search));
  $('question-options').replaceChildren(...questions.map(question => {
    const label = element('label', 'picker-option');
    const input = element('input'); input.type = 'checkbox'; input.dataset.questionId = String(question.id);
    input.checked = evaluationState.selected.some(item => item.questaoId === question.id);
    input.addEventListener('change', () => {
      if (input.checked) evaluationState.selected.push({ questaoId: question.id, valor: '1' });
      else evaluationState.selected = evaluationState.selected.filter(item => item.questaoId !== question.id);
      renderSelectedQuestions();
    });
    const content = element('span');
    content.append(element('strong', '', question.enunciado),
      element('small', '', question.tipo === 'TEXTO_LIVRE' ? 'Texto livre' : 'Múltipla escolha'));
    label.append(input, content);
    return label;
  }));
  if (!questions.length) $('question-options').append(element('p', 'picker-empty', 'Nenhuma questão encontrada.'));
}

function filteredStudents() {
  const search = $('student-search').value.toLocaleLowerCase('pt-BR');
  return evaluationState.students.filter(student => evaluationState.readonly
    ? evaluationState.participants.has(student.id)
    : `${student.nome} ${student.email}`.toLocaleLowerCase('pt-BR').includes(search));
}

function renderStudents() {
  $('student-counter').textContent = `${evaluationState.participants.size} selecionados`;
  const students = filteredStudents();
  $('student-options').replaceChildren(...students.map(student => {
    const label = element('label', 'picker-option');
    const input = element('input'); input.type = 'checkbox'; input.dataset.studentId = String(student.id);
    input.checked = evaluationState.participants.has(student.id);
    input.addEventListener('change', () => {
      if (input.checked) evaluationState.participants.add(student.id);
      else evaluationState.participants.delete(student.id);
      $('student-counter').textContent = `${evaluationState.participants.size} selecionados`;
    });
    const content = element('span');
    content.append(element('strong', '', student.nome), element('small', '', student.email));
    label.append(input, content);
    return label;
  }));
  if (!students.length) $('student-options').append(element('p', 'picker-empty', 'Nenhum aluno encontrado.'));
}

function closeEvaluation() { if (!evaluationState.saving) $('evaluation-dialog').close(); }
$('new-evaluation').addEventListener('click', () => openEvaluation());
$('evaluation-filter').addEventListener('change', renderEvaluations);
$('question-search').addEventListener('input', renderQuestionPicker);
$('student-search').addEventListener('input', renderStudents);
$('select-students').addEventListener('click', () => { filteredStudents().forEach(item => evaluationState.participants.add(item.id)); renderStudents(); });
$('clear-students').addEventListener('click', () => { evaluationState.participants.clear(); renderStudents(); });
$('close-evaluation').addEventListener('click', closeEvaluation);
$('cancel-evaluation').addEventListener('click', closeEvaluation);
$('evaluation-dialog').addEventListener('cancel', event => { if (evaluationState.saving) event.preventDefault(); });

$('evaluation-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (evaluationState.saving || evaluationState.readonly) return;
  const publish = event.submitter?.dataset.action === 'publish';
  showError('evaluation-error');
  const inicio = new Date($('evaluation-start').value);
  const fim = new Date($('evaluation-end').value);
  if (!Number.isFinite(inicio.getTime()) || !Number.isFinite(fim.getTime()) || fim <= inicio) {
    showError('evaluation-error', 'O fim da avaliação deve ser posterior ao início.'); return;
  }
  if (!$('evaluation-name').value.trim()) { showError('evaluation-error', 'Informe o título da avaliação.'); return; }
  if (publish && (!evaluationState.selected.length || !evaluationState.participants.size)) {
    showError('evaluation-error', 'Para publicar, selecione pelo menos uma questão e um aluno.'); return;
  }
  const body = { titulo: $('evaluation-name').value.trim(), descricao: $('evaluation-description').value.trim(),
    inicioEm: inicio.toISOString(), fimEm: fim.toISOString(),
    questoes: evaluationState.selected.map(item => ({ questaoId: item.questaoId, valor: Number(item.valor) })),
    alunos: [...evaluationState.participants] };
  evaluationState.saving = true;
  for (const id of ['save-evaluation', 'publish-evaluation', 'close-evaluation', 'cancel-evaluation']) $(id).disabled = true;
  $('evaluation-fields').disabled = true;
  try {
    const id = evaluationState.id;
    const saved = await api(id === null ? '/api/avaliacoes' : `/api/avaliacoes/${id}`, {
      method: id === null ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });
    evaluationState.id = saved.id;
    if (publish) await api(`/api/avaliacoes/${saved.id}/publicar`, { method: 'POST' });
    $('evaluation-dialog').close();
    $('evaluation-filter').value = '';
    notify(publish ? 'Avaliação publicada. Tudo pronto para a próxima etapa.' : 'Rascunho salvo. Continue quando quiser.');
    await loadEvaluations();
  } catch (error) { showError('evaluation-error', error.message); }
  finally {
    evaluationState.saving = false;
    $('evaluation-fields').disabled = evaluationState.readonly;
    for (const id of ['save-evaluation', 'publish-evaluation', 'close-evaluation', 'cancel-evaluation']) $(id).disabled = false;
  }
});

if (state.user?.perfil === 'PROFESSOR' && state.view === 'avaliacoes') loadEvaluations();
