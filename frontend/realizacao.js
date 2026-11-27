'use strict';

const realizacaoState = { detail: null, opening: false, saving: false, request: 0 };
const situationLabels = { AGENDADA: 'Agendada', DISPONIVEL: 'Disponível', EM_ANDAMENTO: 'Em andamento', ENCERRADA: 'Prazo encerrado', ENTREGUE: 'Entregue' };

function resetRealizacao() {
  realizacaoState.request++;
  realizacaoState.detail = null;
  if ($('prova-dialog').open) $('prova-dialog').close();
  $('prova-questions').replaceChildren();
  $('student-evaluations-list').replaceChildren();
}

async function loadMinhasAvaliacoes() {
  const request = ++realizacaoState.request;
  showError('student-evaluations-error');
  $('student-evaluations-list').replaceChildren(element('p', 'empty-state', 'Carregando suas avaliações…'));
  try {
    const list = await api('/api/minhas-avaliacoes');
    if (request !== realizacaoState.request) return;
    $('student-evaluations-caption').textContent = `${list.length} avaliações destinadas a você`;
    $('student-evaluations-list').replaceChildren(...list.map(item => {
      const card = element('article', 'evaluation-card');
      const top = element('div', 'card-top');
      top.append(element('span', 'question-number', `AVALIAÇÃO ${String(item.avaliacaoId).padStart(2, '0')}`),
        element('span', 'type-badge', situationLabels[item.situacao]));
      card.append(top, element('h3', '', item.titulo));
      if (item.descricao) card.append(element('p', 'muted evaluation-description', item.descricao));
      card.append(element('p', 'evaluation-period', `${dateFormat.format(new Date(item.inicioEm))} → ${dateFormat.format(new Date(item.fimEm))}`));
      card.append(element('p', 'evaluation-meta', `${item.quantidadeQuestoes} questões · ${numberFormat.format(item.valorTotal)} pontos`));
      if (item.situacao === 'ENTREGUE') {
        card.append(element('p', 'result-label', item.notaTotal !== null
          ? `Nota: ${numberFormat.format(item.notaTotal)} / ${numberFormat.format(item.valorTotal)}`
          : 'Entrega registrada. Aguardando correção.'));
      }
      const label = { DISPONIVEL: 'Começar avaliação →', EM_ANDAMENTO: 'Continuar avaliação →', ENTREGUE: 'Ver entrega →' }[item.situacao];
      const button = element('button', 'button quiet', label || situationLabels[item.situacao]);
      button.type = 'button'; button.disabled = !label;
      button.addEventListener('click', () => openProva(item.id, item.situacao !== 'ENTREGUE'));
      card.append(button);
      return card;
    }));
    if (!list.length) $('student-evaluations-list').append(element('p', 'empty-state', 'Nenhuma avaliação destinada a você por enquanto.'));
  } catch (error) {
    if (request !== realizacaoState.request) return;
    $('student-evaluations-list').replaceChildren();
    showError('student-evaluations-error', error.message);
  }
}

async function openProva(id, iniciar) {
  if (realizacaoState.opening || realizacaoState.saving) return;
  realizacaoState.opening = true;
  const userId = state.user?.id;
  try {
    if (iniciar) await api(`/api/minhas-avaliacoes/${id}/iniciar`, { method: 'POST' });
    const detail = await api(`/api/minhas-avaliacoes/${id}`);
    if (state.user?.id !== userId || state.user?.perfil !== 'ALUNO') return;
    realizacaoState.detail = detail;
    $('prova-title').textContent = detail.avaliacao.titulo;
    $('prova-description').textContent = detail.avaliacao.descricao;
    $('prova-period').textContent = `Prazo: ${dateFormat.format(new Date(detail.avaliacao.fimEm))}`;
    const readonly = detail.avaliacao.entregueEm !== null;
    $('prova-fields').disabled = readonly;
    $('save-prova').hidden = readonly;
    $('submit-prova').hidden = readonly;
    $('prova-final-help').hidden = readonly;
    $('prova-message').hidden = true;
    showError('prova-error');
    const saved = new Map(detail.respostas.map(item => [item.avaliacaoQuestaoId, item]));
    $('prova-questions').replaceChildren(...detail.questoes.map((question, index) => {
      const field = element('fieldset', 'prova-question');
      field.append(element('legend', '', `${String(index + 1).padStart(2, '0')} · ${numberFormat.format(question.valor)} pontos`), element('h3', '', question.enunciado));
      if (question.tipo === 'MULTIPLA_ESCOLHA') {
        for (const alternative of question.alternativas) {
          const label = element('label', 'answer-option');
          const input = element('input'); input.type = 'radio'; input.name = `resposta-${question.id}`; input.value = String(alternative.id);
          input.checked = saved.get(question.id)?.alternativaId === alternative.id;
          label.append(input, element('span', '', alternative.texto)); field.append(label);
        }
        if (!readonly) {
          const clear = element('button', 'text-button', 'Limpar resposta'); clear.type = 'button';
          clear.addEventListener('click', () => { field.querySelectorAll('input').forEach(input => { input.checked = false; }); updateProvaProgress(); });
          field.append(clear);
        }
      } else {
        const textarea = element('textarea'); textarea.rows = 5; textarea.dataset.textItem = String(question.id);
        textarea.setAttribute('aria-label', `Resposta da questão ${index + 1}`);
        textarea.value = saved.get(question.id)?.texto || '';
        textarea.placeholder = 'Escreva sua resposta com suas palavras.';
        field.append(textarea);
      }
      return field;
    }));
    updateProvaProgress();
    $('prova-dialog').showModal();
  } catch (error) { notify(error.message); }
  finally { realizacaoState.opening = false; }
}

function respostasDaProva() {
  if (!realizacaoState.detail) return [];
  const respostas = [];
  for (const question of realizacaoState.detail.questoes) {
    if (question.tipo === 'MULTIPLA_ESCOLHA') {
      const selected = $('prova-form').querySelector(`input[name="resposta-${question.id}"]:checked`);
      if (selected) respostas.push({ avaliacaoQuestaoId: question.id, alternativaId: Number(selected.value) });
    } else {
      const texto = $('prova-form').querySelector(`[data-text-item="${question.id}"]`).value.trim();
      if (texto) respostas.push({ avaliacaoQuestaoId: question.id, texto });
    }
  }
  return respostas;
}

function updateProvaProgress() {
  const detail = realizacaoState.detail;
  if (!detail) return;
  $('prova-progress').textContent = detail.avaliacao.entregueEm
    ? 'Entrega registrada. Suas respostas estão disponíveis para consulta.'
    : `${respostasDaProva().length} de ${detail.questoes.length} questões respondidas. Você pode salvar e continuar dentro do prazo.`;
}

$('refresh-student-evaluations').addEventListener('click', loadMinhasAvaliacoes);
$('prova-form').addEventListener('input', updateProvaProgress);
function closeProva() { if (!realizacaoState.saving) $('prova-dialog').close(); }
$('close-prova').addEventListener('click', closeProva);
$('cancel-prova').addEventListener('click', closeProva);
$('prova-dialog').addEventListener('cancel', event => { if (realizacaoState.saving) event.preventDefault(); });
$('prova-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (realizacaoState.saving || !realizacaoState.detail || realizacaoState.detail.avaliacao.entregueEm) return;
  const deliver = event.submitter?.dataset.action === 'deliver';
  const id = realizacaoState.detail.avaliacao.id;
  const body = JSON.stringify({ respostas: respostasDaProva() });
  realizacaoState.saving = true;
  showError('prova-error');
  $('prova-message').hidden = true;
  $('prova-fields').disabled = true;
  for (const control of ['save-prova', 'submit-prova', 'cancel-prova', 'close-prova']) $(control).disabled = true;
  try {
    await api(`/api/minhas-avaliacoes/${id}/${deliver ? 'entregar' : 'respostas'}`, {
      method: deliver ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body
    });
    if (deliver) { $('prova-dialog').close(); notify('Avaliação entregue. Obrigado por compartilhar seu aprendizado.'); }
    else { $('prova-message').textContent = 'Respostas salvas.'; $('prova-message').hidden = false; }
    await loadMinhasAvaliacoes();
  } catch (error) { showError('prova-error', error.message); }
  finally {
    realizacaoState.saving = false;
    $('prova-fields').disabled = false;
    for (const control of ['save-prova', 'submit-prova', 'cancel-prova', 'close-prova']) $(control).disabled = false;
  }
});

if (state.user?.perfil === 'ALUNO') loadMinhasAvaliacoes();
