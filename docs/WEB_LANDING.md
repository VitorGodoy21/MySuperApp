# Landing page web (`https://baila.space/`)

## O que e

A raiz do Firebase Hosting (`web/public/index.html`) e uma landing page
institucional em pt-BR que explica o QR Code dinamico do Baila QR, os tres
tipos de conteudo (redirecionar, texto, mural), exemplos de uso em comercio
(restaurante, loja de roupas, embalagem/produto e variacoes) e os recursos do
app (metricas, notificacoes, NFC, anti-spam).

Logo apos o hero ha a secao **Produtos** (`#produtos`): a placa de divulgacao
para estabelecimentos com varios QR Codes dinamicos (Instagram, Pix, cardapio,
WhatsApp...) e chip NFC embutido, personalizada com a marca do cliente. Sao
apresentados tres modelos (placa de mesa A6/A5, quadro de parede/balcao A4/A3,
adesivo/etiqueta), todos "sob orcamento", com CTA de orcamento pelo WhatsApp.

A pagina de resolucao dos QR Codes continua em `web/public/qr/index.html`
(`/qr/?id=...`) e nao e afetada pela landing.

## Caracteristicas tecnicas

- HTML + CSS + um `<script>` inline minimo (ano do rodape). Sem framework, sem
  build, sem Firebase SDK.
- Respeita o CSP definido em `web/firebase.json`: nenhuma fonte externa, icones
  em emoji/SVG inline, QR ilustrativo como `<symbol id="qr">` SVG reutilizado.
  Ao editar a pagina, nao adicione `<link>`/`<img>`/`<script>` de dominios
  externos sem ajustar o CSP.
- Identidade visual igual a `/qr`: gradiente `#667eea -> #764ba2`, cards
  brancos com `border-radius: 20px`, fonte do sistema.
- Responsiva (breakpoints em 900px e 480px) e respeita
  `prefers-reduced-motion`.

## Link da Play Store

O botao "Baixar na Play Store" aparece duas vezes (hero e CTA final) com o
placeholder:

```text
https://play.google.com/store/apps/details?id=REPLACE_ME
```

Procure por `REPLACE_ME` em `web/public/index.html` e troque pelo ID real do
app publicado. Ha um comentario `TODO` ao lado de cada ocorrencia.

## Numero do WhatsApp (orcamento das placas)

Os botoes "Pedir orcamento no WhatsApp" (card destaque e rodape da secao
Produtos) usam o placeholder:

```text
https://wa.me/55REPLACE_ME?text=...
```

Troque `55REPLACE_ME` pelo numero completo com DDI e DDD, sem espacos ou
simbolos (ex.: `5511999999999`). O texto pre-preenchido esta URL-encoded no
parametro `text`. Tambem ha comentarios `TODO` ao lado de cada ocorrencia.

## Como testar localmente

```powershell
cd web
firebase emulators:start --only hosting --project baila-675c6
```

Abra `http://127.0.0.1:5000/`. Observacao: o emulador de Hosting nao aplica os
`headers` do `firebase.json`; para validar o CSP use a pagina publicada ou
injete o header manualmente no navegador/ferramenta de teste.

## Deploy

```powershell
cd web
firebase deploy --only hosting
```

### Erro "a execução de scripts foi desabilitada neste sistema"

O `firebase` instalado via npm cria um shim `firebase.ps1` em
`%AppData%\npm`. Se a Execution Policy do PowerShell estiver `Restricted`, ele
nao carrega. Alternativas:

1. Chamar o shim `.cmd` no lugar do `.ps1` (nao exige mudar politica):

   ```powershell
   firebase.cmd deploy --only hosting
   ```

2. Liberar scripts locais apenas para o usuario atual (uma vez):

   ```powershell
   Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
   ```

   Depois disso `firebase deploy --only hosting` funciona normalmente.

3. Usar `npx firebase-tools deploy --only hosting`.
