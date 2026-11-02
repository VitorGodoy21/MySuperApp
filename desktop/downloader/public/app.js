'use strict';
const $ = id => document.getElementById(id);
const token = location.hash.slice(1) || sessionStorage.getItem('local-token');
history.replaceState(null, '', '/');
if (token) sessionStorage.setItem('local-token', token);
let stopped = false;
let sending = false;
let state = null;
let page = 0;
let pageKey = '';
let epoch = 0;
let listRequest = 0;
const rows = new Map();
const labels = {
    idle: 'Pronto', loading_playlist: 'Carregando playlist', ready: 'Selecione os itens',
    processing: 'Processando', completed: 'Finalizado · confira os resultados', error: 'Falha', cancelled: 'Cancelado',
    pending: 'Na fila', preparing: 'Verificando', downloading: 'Baixando', converting: 'Convertendo MP3',
    merging: 'Juntando MP4', done: 'Concluído', failed: 'Falhou', skipped: 'Já existe · pulado', unavailable: 'Indisponível'
};

async function api(path, body) {
    const response = await fetch('/api/' + path, {
        method: body === undefined ? 'GET' : 'POST',
        headers: { 'X-Local-Token': token || '', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
        ...(body === undefined ? {} : { body: JSON.stringify(body) })
    });
    const result = await response.json();
    if (!response.ok) {
        const failure = new Error(result.error || 'Não foi possível executar a operação.');
        failure.status = response.status;
        throw failure;
    }
    return result;
}
function error(message) { $('error').textContent = message; $('error').hidden = !message; }
function options() {
    const playlist = $('mode').value === 'playlist';
    $('format-field').hidden = playlist;
    $('height-field').hidden = playlist || $('format').value !== 'mp4';
    $('format-hint').textContent = playlist
        ? 'MP3 de 192 kbps em subpasta da playlist, sem numeração. Arquivos existentes serão pulados.'
        : $('format').value === 'mp4'
            ? 'Melhor H.264/AAC disponível até a resolução escolhida. Sem conversão de vídeo; pode não haver formato compatível.'
            : 'MP3 de 192 kbps. Um arquivo de cada vez, sem quotas diárias do aplicativo.';
}
function render() {
    if (!state) return;
    const busy = state.busy || sending;
    const playlist = $('mode').value === 'playlist';
    const ready = state.phase === 'ready' && state.mode === 'playlist' && playlist;
    $('phase').textContent = labels[state.phase] || state.phase;
    $('message').textContent = state.message;
    $('folder').value = state.folder;
    $('destination').textContent = state.destination ? 'Destino: ' + state.destination : '';
    const counts = state.counts;
    $('summary').textContent = state.total
        ? `${state.total} itens · ${counts.done} concluídos · ${counts.failed} falhas · ${counts.skipped} pulados · ${counts.cancelled} cancelados · ${counts.unavailable} indisponíveis`
        : '';
    $('selection').textContent = `${state.selected} de ${state.total} selecionados`;
    const current = state.current;
    $('title').textContent = current ? `${current.index + 1}. ${current.title}` : state.title || '';
    $('item-message').textContent = current ? `${labels[current.phase]} · ${current.message}${current.height ? ' · ' + current.height + 'p' : ''}` : '';
    $('progress').hidden = !current || !state.busy;
    if (typeof current?.percent === 'number') $('progress').value = current.percent;
    else $('progress').removeAttribute('value');
    $('progress-hint').textContent = current?.phase === 'downloading' && state.format === 'mp4' ? 'Progresso da faixa atual (vídeo ou áudio), não do arquivo completo.' : '';
    for (const id of ['mode', 'format', 'height', 'url', 'choose', 'authorized']) $(id).disabled = busy || stopped;
    $('start').disabled = busy || stopped || (ready && !state.selected);
    $('start').textContent = playlist ? (ready ? 'Baixar selecionados' : 'Carregar playlist') : 'Baixar ' + $('format').value.toUpperCase();
    $('cancel').hidden = !state.busy;
    $('cancel').disabled = sending;
    $('retry').hidden = state.busy || !state.retryable;
    $('retry').disabled = busy;
    $('list-section').hidden = !state.total;
    $('select-all').hidden = $('select-none').hidden = !ready;
    $('select-all').disabled = $('select-none').disabled = busy;
    $('previous').disabled = busy && sending || page === 0;
    $('next').disabled = busy && sending || (page + 1) * 50 >= state.total;
    $('page-label').textContent = `Página ${page + 1} de ${Math.max(1, Math.ceil(state.total / 50))}`;
    options();
}

async function list() {
    if (!state?.total) { $('items').replaceChildren(); rows.clear(); pageKey = ''; return; }
    const operation = state.operation, requestedPage = page, request = ++listRequest, requestedEpoch = epoch;
    let result;
    try { result = await api(`items?operation=${encodeURIComponent(operation)}&page=${requestedPage}`); }
    catch (err) {
        // Another tab or a newer action can replace the preview during this request.
        if (requestedEpoch !== epoch || request !== listRequest || err.status === 400) return;
        throw err;
    }
    if (requestedEpoch !== epoch || state.operation !== operation || page !== requestedPage || request !== listRequest) return;
    const key = `${operation}:${page}`;
    if (pageKey !== key) { $('items').replaceChildren(); rows.clear(); pageKey = key; }
    for (const item of result.items) {
        let row = rows.get(item.index);
        if (!row) {
            const li = document.createElement('li');
            const input = document.createElement('input');
            input.type = 'checkbox';
            input.setAttribute('aria-label', 'Selecionar item ' + (item.index + 1));
            const details = document.createElement('div');
            const title = document.createElement('strong');
            const message = document.createElement('p');
            details.append(title, message);
            li.append(input, details);
            $('items').append(li);
            row = { li, input, title, message };
            rows.set(item.index, row);
            input.addEventListener('change', () => action('select', {
                operation, action: 'items', indices: [item.index], selected: input.checked
            }));
        }
        row.input.checked = item.selected;
        row.input.disabled = state.phase !== 'ready' || state.busy || sending || item.phase === 'unavailable';
        row.title.textContent = `${item.index + 1}. ${item.title}`;
        const phase = item.phase === 'pending' && !item.selected ? 'Não selecionado' : labels[item.phase] || item.phase;
        row.message.textContent = `${phase}${item.message ? ' · ' + item.message : ''}${item.height ? ' · ' + item.height + 'p' : ''}${item.filename ? ' · ' + item.filename : ''}`;
        row.li.dataset.phase = item.phase;
    }
}
async function refresh() {
    const requestedEpoch = epoch;
    const next = await api('status');
    if (requestedEpoch !== epoch || stopped) return;
    if (!state || state.operation !== next.operation) {
        page = 0;
        if (next.phase !== 'idle') {
            $('mode').value = next.mode;
            $('format').value = next.format;
            $('height').value = String(next.height);
            $('url').value = next.url || '';
        }
    }
    state = next;
    render();
    await list();
}
async function action(endpoint, body) {
    if (sending || stopped) return;
    sending = true;
    epoch++;
    error('');
    render();
    try { await api(endpoint, body); }
    catch (err) { error(err.message); }
    finally {
        // Keep polling and actions serialized so a late status cannot revert a selection.
        try { await refresh(); } catch (err) { error(err.message); }
        sending = false;
        render();
        await list().catch(() => {});
    }
}
async function poll() {
    if (stopped) return;
    if (!sending) {
        const requestedEpoch = epoch;
        try { await refresh(); }
        catch (err) {
            if (requestedEpoch === epoch && !sending && !stopped) {
                stopped = true;
                error('Auxiliar encerrado ou sessão expirada. Abra novamente pelo site ou atalho.');
                render();
            }
        }
    }
    if (!stopped) setTimeout(poll, 800);
}
for (const id of ['mode', 'format', 'height', 'url']) {
    $(id).addEventListener(id === 'url' ? 'input' : 'change', async () => {
        options();
        if (state?.phase !== 'idle') await action('reset', { operation: state.operation });
        else render();
    });
}
$('download-form').addEventListener('submit', event => {
    event.preventDefault();
    if (!state || sending || state.busy) return;
    const mode = $('mode').value;
    if (mode === 'playlist' && state.phase !== 'ready') {
        action('preview', { url: $('url').value });
    } else {
        action('start', { mode, url: $('url').value, format: mode === 'playlist' ? 'mp3' : $('format').value,
            height: Number($('height').value), operation: state.operation, authorized: $('authorized').checked });
    }
});
for (const [id, endpoint] of [['choose', 'folder'], ['cancel', 'cancel'], ['open', 'open-folder']]) {
    $(id).addEventListener('click', () => action(endpoint, {}));
}
for (const [id, selection] of [['select-all', 'all'], ['select-none', 'none']]) {
    $(id).addEventListener('click', () => action('select', { operation: state.operation, action: selection }));
}
$('retry').addEventListener('click', () => action('retry', { operation: state.operation, authorized: $('authorized').checked }));
for (const [id, delta] of [['previous', -1], ['next', 1]]) {
    $(id).addEventListener('click', async () => { page += delta; render(); await list().catch(err => error(err.message)); });
}
$('exit').addEventListener('click', async () => {
    if (!confirm('Encerrar o aplicativo? A operação atual e a fila serão canceladas.')) return;
    try {
        await api('exit', {});
        stopped = true;
        document.querySelectorAll('button, input, select').forEach(el => { el.disabled = true; });
        $('message').textContent = 'Aplicativo encerrado. Você pode fechar esta aba.';
    } catch (err) { error(err.message); }
});
options();
poll();
