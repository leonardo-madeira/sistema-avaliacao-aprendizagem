'use strict';

const correcaoState = { avaliacaoId: null, title: '', detail: null, opening: false, saving: false, request: 0 };

function resetCorrecao() {
  correcaoState.request++;
  correcaoState.detail = null;
  for (const id of ['entregas-dialog', 'correcao-dialog']) if ($(id).open) $(id).close();
  $('entregas-list').replaceChildren(); $('correcao-questions').replaceChildren();
}

async function loadEntregas() {
  const request = ++correcaoState.request;
  showError('entregas-error');
  $('entregas-list').replaceChildren(element('p', 'empty-state', 'Carregando entregas…'));
  try {
    const list = await api(`/api/avaliacoes/${correcaoState.avaliacaoId}/entregas`);
    if (request !== correcaoState.request) return;
    $('entregas-list').replaceChildren(...list.map(item => {
      const row = element('article', 'entrega-row');
      const info = element('div');
      info.append(element('h3', '', item.alunoNome));
      const description = !item.entregueEm ? 'Ainda não entregue' : item.notaTotal !== null
        ? `Nota: ${numberFormat.format(item.notaTotal)} / ${numberFormat.format(item.valorTotal)}`
        : `${item.correcoesPendentes} respostas aguardando correção`;
      info.append(element('p', 'muted', description));
      const button = element('button', 'button quiet', item.correcoesPendentes ? 'Corrigir textos →' : 'Ver respostas →');
      button.type = 'button'; button.disabled = !item.entregueEm;
      button.addEventListener('click', () => openCorrecao(item.id));
      row.append(info, button); return row;
    }));
    if (!list.length) $('entregas-list').append(element('p', 'empty-state', 'Nenhum participante nesta avaliação.'));
  } catch (error) {
    if (request !== correcaoState.request) return;
    $('entregas-list').replaceChildren(); showError('entregas-error', error.message);
  }
}

async function openEntregas(id, title) {
  correcaoState.avaliacaoId = id; correcaoState.title = title;
  $('entregas-title').textContent = title;
  $('entregas-dialog').showModal();
  await loadEntregas();
}

async function openCorrecao(id) {
  if (correcaoState.opening || correcaoState.saving) return;
  correcaoState.opening = true;
  const userId = state.user?.id;
  try {
    const detail = await api(`/api/correcoes/${id}`);
    if (state.user?.id !== userId || state.user?.perfil !== 'PROFESSOR') return;
    correcaoState.detail = detail;
    $('correcao-title').textContent = detail.alunoNome;
    $('correcao-subtitle').textContent = detail.titulo;
    showError('correcao-error');
    $('correcao-questions').replaceChildren(...detail.respostas.map((answer, index) => {
      const card = element('section', 'correcao-question');
      card.append(element('p', 'question-number', `QUESTÃO ${index + 1} · ${numberFormat.format(answer.valor)} pontos`), element('h3', '', answer.enunciado));
      card.append(element('p', 'submitted-answer', answer.resposta || 'Sem resposta. Zero pontos.'));
      if (answer.tipo === 'TEXTO_LIVRE' && answer.respostaId !== null) {
        const label = element('label', 'grade-label', 'Nota atribuída');
        const input = element('input'); input.type = 'number'; input.min = '0'; input.max = String(answer.valor); input.step = '0.01';
        input.dataset.respostaId = String(answer.respostaId);
        input.value = answer.notaObtida === null ? '' : String(answer.notaObtida);
        input.setAttribute('aria-label', `Nota da questão ${index + 1}`);
        label.append(input); card.append(label);
      } else card.append(element('p', 'automatic-grade', `Correção automática: ${numberFormat.format(answer.notaObtida ?? 0)} pontos`));
      return card;
    }));
    $('save-correcao').hidden = !$('correcao-questions').querySelector('input');
    $('correcao-dialog').showModal();
  } catch (error) { notify(error.message); }
  finally { correcaoState.opening = false; }
}

$('close-entregas').addEventListener('click', () => $('entregas-dialog').close());
function closeCorrecao() { if (!correcaoState.saving) $('correcao-dialog').close(); }
$('close-correcao').addEventListener('click', closeCorrecao);
$('cancel-correcao').addEventListener('click', closeCorrecao);
$('correcao-dialog').addEventListener('cancel', event => { if (correcaoState.saving) event.preventDefault(); });
$('correcao-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (correcaoState.saving || !correcaoState.detail) return;
  const notas = [...$('correcao-questions').querySelectorAll('input')].filter(input => input.value !== '')
    .map(input => ({ respostaId: Number(input.dataset.respostaId), nota: Number(input.value) }));
  if (!notas.length) { showError('correcao-error', 'Preencha ao menos uma nota.'); return; }
  correcaoState.saving = true;
  showError('correcao-error'); $('correcao-fields').disabled = true;
  for (const id of ['save-correcao', 'cancel-correcao', 'close-correcao']) $(id).disabled = true;
  try {
    await api(`/api/correcoes/${correcaoState.detail.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ notas }) });
    $('correcao-dialog').close(); notify('Notas salvas. O resultado foi atualizado.');
    await loadEntregas();
  } catch (error) { showError('correcao-error', error.message); }
  finally {
    correcaoState.saving = false; $('correcao-fields').disabled = false;
    for (const id of ['save-correcao', 'cancel-correcao', 'close-correcao']) $(id).disabled = false;
  }
});
