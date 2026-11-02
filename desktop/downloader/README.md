# Downloader local MP3 e MP4 (Windows x64)

Interface local aberta pelo painel web ou pelo atalho Windows. Os arquivos ficam
no computador, sem backend de conversao ou login adicional. Videos individuais
em MP3/MP4 e playlists em MP3 192 kbps, somente de conteudo autorizado.

## Desenvolvimento

Python 3.12 x64 com Tcl/Tk e Inno Setup 6 sao ferramentas de build; o usuario final
nao precisa instala-las. Na pasta deste arquivo:

```powershell
.\build.ps1
```

Se os programas estiverem em outros caminhos, use `-Python` e `-InnoSetup`.
O build cria ambiente virtual, instala `requirements-build.txt`, baixa ferramentas
fixadas em `binaries.json`, verifica SHA256, executa testes e gera:

- `dist\MySuperAppDownloader\MySuperAppDownloader.exe` (nao mover somente o exe).
- `output\MySuperAppDownloader-Setup.exe` (instalador).
- `output\MySuperAppDownloader-<versao>-sources.zip` (fontes correspondentes; ver Publicacao).

Executar fontes, apos preparar `vendor` pelo build:

```powershell
.\.venv\Scripts\python.exe app.py
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

## Uso

Instalar por usuario (sem administrador). Abrir o atalho **My Super App Downloader**
ou o protocolo `mysuperapp-downloader://open` do painel. Colar link HTTPS de video
individual, escolher MP3 (192 kbps) ou MP4, escolher pasta, confirmar autorizacao
e baixar. MP4 permite teto de 360p, 720p ou 1080p: seleciona a melhor resolucao
H.264/AAC disponivel ate o teto e junta as faixas sem recodificar video.
Se nao houver formato compativel, informa falha; nao usa codec ou altura diferente
silenciosamente. Lives em andamento/agendadas nao sao suportadas.

No modo Playlist, colar link publico e clicar Carregar playlist. A lista nao baixa
midia: selecione todos/nenhum/itens e clique Baixar selecionados. Download em MP3,
sequencial, continuando apos falhas. A interface pagina 50 itens por vez, sem quota
do tamanho total. Listas dinamicas (Mix/radio) nao sao suportadas.

A subpasta usa titulo seguro e ID da playlist. Os arquivos usam titulo e ID do
video, SEM numeracao. Arquivos existentes sao pulados antes do download de midia
quando detectados, nunca sobrescritos. Cancelar para o item atual e a fila,
preservando concluidos. Repetir falhas/cancelados usa a selecao e destino originais.
Para outro destino, escolher uma pasta ja existente. Nao ha historico persistente:
selecao e resultados sobrevivem a refresh da aba, mas nao a encerrar o auxiliar.
Alterar link/modo/formato limpa a operacao anterior.

Nao ha limite diario do aplicativo. Playlists/MP4 usam mais internet e disco,
e ha limites externos do YouTube. Os MP3 ainda precisam de conversao de audio;
remux MP4 evita conversao pesada de video, mas tambem usa recursos locais.
O instalador 0.2.0 mantem AppId/protocolo: encerrar o auxiliar antes de atualizar
a instalacao existente. Nao executar desinstalacao para atualizar.

Fechar a janela de controle ou usar **Encerrar aplicativo** interrompe os processos
filhos e encerra o servidor. Fechar apenas a aba nao encerra o aplicativo.
O seletor de pasta aparece na janela nativa, que pode estar atras do navegador.

O auxiliar usa apenas loopback, porta dinamica e token por execucao. O site nunca
recebe acesso a arquivos nem envia credenciais Firebase ao aplicativo. A chamada
de protocolo somente abre a interface, nao recebe links/comandos externos.

## Publicacao (portal web, so para contas logadas)

Fluxo completo, a partir da raiz do repositorio:

1. `desktop\downloader\build.ps1` gera o instalador e, ao final, chama
   `collect-sources.ps1` (pule com `-SkipSources`), que produz
   `output\MySuperAppDownloader-<versao>-sources.zip`: FFmpeg no commit exato, a
   receita BtbN na tag usada, cada biblioteca habilitada em `ffmpeg -buildconf`
   no commit fixado pela receita (LAME via snapshot SVN do SourceForge), yt-dlp,
   Deno, licencas e `SOURCES-MANIFEST.txt` (origem, revisao, SHA256). O script
   falha listando qualquer fonte nao resolvida; nao publique sem o zip.
   `collect-sources.ps1 -Plan` so imprime a tabela de componentes incluidos/
   excluidos. Downloads ficam em `.cache\sources` (ignorado pelo git).
2. `npm --prefix web\functions run publish:downloader -- --dry-run` e depois sem
   `--dry-run` (ADC do projeto prd `baila-675c6`; Storage nao esta habilitado no
   dev). Envia `downloader/MySuperAppDownloader-Setup-<versao>.exe`,
   `downloader/MySuperAppDownloader-<versao>-sources.zip` e grava
   `downloader/latest.json` (versao, tamanho, SHA256, data, `--notes=a;b`).
   Objetos versionados existentes so sao sobrescritos com `--force`. Nenhum token
   de download publico e criado.
3. `firebase deploy --only storage,hosting` em `web\`: `storage.rules` libera
   leitura de `downloader/**` apenas para usuarios autenticados; o portal
   (`web\public\my-super-app\index.html`, aba Downloads) le o manifesto pela REST
   do Storage com o ID token, baixa com progresso, confere o SHA256 antes de salvar
   e abre `mysuperapp-downloader://open` com deteccao heuristica (perda de foco).

O instalador nao e assinado: o portal e `THIRD-PARTY-NOTICES.txt` explicam o aviso
do SmartScreen e mostram o hash para conferencia. Nao desative protecoes do
Windows; assinatura confiavel requer certificado separado. Ao mudar versao,
atualize `installer.iss` (`AppVersion`), `THIRD-PARTY-NOTICES.txt` e refaca os
tres passos. Teste em Windows limpo: instalar, SmartScreen, protocolo, download
autorizado e desinstalacao.

yt-dlp/YouTube mudam frequentemente. Falhas de rede, videos restritos ou bloqueios
do YouTube nao sao contornados com cookies pessoais ou proxies; atualizar versoes
fixadas e hashes e reconstruir o instalador quando necessario.
