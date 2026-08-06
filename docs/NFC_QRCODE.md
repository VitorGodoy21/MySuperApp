# NFC e QR Code dinamico

## Objetivo

Um QR Code do MySuperApp usa uma URL fixa no formato:

```text
https://baila.space/qr/?id={qrCodeId}
```

Essa URL e a identidade publica permanente do QR Code. A pagina em `baila.space` busca `qrcodes/{qrCodeId}` no Firestore e decide se deve redirecionar, mostrar texto ou abrir um mural. Alterar esse documento pelo app altera o que sera apresentado sem trocar o adesivo nem imprimir outro QR Code.

Uma tag NFC pode usar essa mesma URL. Assim, o QR impresso e o NFC no mesmo adesivo sao duas formas de abrir o mesmo identificador, com o mesmo conteudo configuravel pelo app.

```text
Camera le QR  --> https://baila.space/qr/?id={qrCodeId} --> Hosting + Firestore
Celular le NFC --> https://baila.space/qr/?id={qrCodeId} --> resolvem o conteudo atual
```

## O que gravar na tag

O app permite dois tipos de conteúdo ao gravar uma tag:

1. **URL de um QR Code existente (recomendado para a maioria dos casos).**
   Grave **um único NDEF URI Record** com a `staticUrl` do QR Code acrescida
   do parâmetro `source=nfc`. Exemplo:

   ```text
   https://baila.space/qr/?id=XTF9FopziwTWxRRscDkv&source=nfc
   ```

   NDEF (NFC Data Exchange Format) e o formato padrao de mensagens NFC. Um
   URI Record informa ao celular que o conteudo e uma URL; Android e iPhones
   modernos normalmente oferecem ou abrem o navegador ao aproximar uma tag
   com uma URL HTTPS.

2. **Valor de texto personalizado.** O app também permite gravar um valor
   de texto livre, definido pelo usuário, como um **NDEF Text Record**. Use
   este modo para conteúdos que não dependem da resolução dinâmica via
   Firestore (ex.: um texto fixo, um telefone, um Wi-Fi SSID simples). Ao
   contrário da URL de um QR Code, o valor de texto **não pode ser
   atualizado remotamente**: qualquer mudança exige regravar a tag
   fisicamente.

Nao grave:

- O `redirectUrl` final, pois ele muda e eliminaria o controle centralizado.
- O texto ou o conteudo de um mural, pois eles tambem podem mudar.
- Credenciais, tokens, IDs internos de usuarios ou dados sensiveis, mesmo no
  modo de valor personalizado — a tag e publica e pode ser lida por qualquer
  pessoa com acesso fisico a ela.

**Atualizacao de decisao de produto:** diferente da versao anterior deste
documento, QR e NFC **nao** usam mais a URL canonica identica. A tag NFC leva
o parametro extra `source=nfc` para que a pagina de redirect e as Cloud
Functions consigam registrar a origem do acesso (`qr` ou `nfc`) no
`access_log`, permitindo comparar volume/uso por canal sem alterar o
`qrCodeId` nem o restante do fluxo de resolucao (texto, redirect, mural).

## Como a atualizacao funciona

Existem dois niveis independentes de mudanca:

| O que muda | Onde muda | Exige aproximar a tag? |
| --- | --- | --- |
| Destino, texto ou tipo apresentado | Documento `qrcodes/{qrCodeId}` pelo app | Nao |
| URL/identidade gravada na tag | Memoria fisica da tag NFC | Sim |

Na operacao normal, a URL da tag nunca precisa mudar: a alteracao no Firestore ja atualiza o resultado do proximo toque NFC e da proxima leitura do QR.

Uma tag pode ser regravada fisicamente enquanto for compativel, formatada para NDEF e nao estiver bloqueada contra escrita. A regravacao exige encostar o celular ou gravador na tag; nao existe atualizacao remota da memoria NFC. A protecao contra escrita costuma ser irreversivel, portanto nao bloqueie a tag antes de validar o adesivo e a URL em campo.

## Tipos de NFC

"NFC" descreve a comunicacao sem contato; a experiencia depende do chip e do formato gravado. Para esta solucao, o requisito principal e ser uma tag **NFC Forum compativel com NDEF** e regravavel.

| Tecnologia | Caracteristicas | Recomendacao |
| --- | --- | --- |
| NFC Forum Type 1 | Legada, pouca capacidade e menos comum no mercado atual. | Nao usar em novas compras. |
| NFC Forum Type 2 | Baseada em ISO/IEC 14443A; barata, muito comum em adesivos e amplamente compativel. Inclui a familia NTAG. | **Recomendada.** |
| NFC Forum Type 3 | Baseada em FeliCa; mais comum em ecossistemas especificos. | Nao necessaria para URLs em adesivos. |
| NFC Forum Type 4 | Baseada em ISO/IEC 14443A/B; pode oferecer mais memoria e recursos de seguranca. | Usar apenas se houver requisito real de seguranca/capacidade. |
| NFC Forum Type 5 | Baseada em ISO/IEC 15693; maior alcance relativo, mas experiencia e disponibilidade variam. | Nao e a primeira escolha para adesivos de URL. |
| MIFARE Classic | Muito difundida em controle de acesso antigo, mas nao e a melhor opcao para interoperabilidade NFC/NDEF moderna. | Evitar para este caso. |

## Tags recomendadas

Compre adesivos originais, regravaveis, compativeis com NFC Forum Type 2 e ja formatados para NDEF. A familia NTAG e a opcao pratica para este projeto:

| Tag | Memoria de usuario aproximada | Quando usar |
| --- | --- | --- |
| NTAG213 | 144 bytes | **Opcao minima recomendada.** A URL canonica atual cabe com folga. |
| NTAG215 | 504 bytes | Boa margem para payloads futuros, sem necessidade de recursos especiais. |
| NTAG216 | 888 bytes | Quando houver necessidade de mais espaco, mantendo o mesmo modelo simples. |

Escolha adesivos feitos para NFC, com material adequado ao ambiente. Metal proximo a antena reduz ou impede a leitura; para aplicacao em metal, use uma tag "on-metal" com camada de ferrite. Confirme tambem resistencia a agua, calor, dobra, cola e abrasao conforme o local de uso.

Type 4 e apropriada somente se um requisito futuro justificar chip mais caro, maior capacidade ou mecanismos de seguranca. Para uma URL publica e curta, ela nao traz beneficio funcional sobre uma NTAG Type 2.

## Provisionamento inicial

Antes de uma integracao de escrita no Android, e possivel gravar a tag com um aplicativo ou gravador NFC que suporte NDEF:

1. No detalhe do QR Code, copie a `staticUrl`; confira que ela contem o ID correto.
2. Escolha a opcao de escrever uma URL/URI ou criar uma mensagem NDEF URI.
3. Cole a `staticUrl` completa como unico registro da mensagem e aproxime a tag.
4. Leia a tag novamente com o mesmo dispositivo e confirme que a URL devolvida e identica.
5. Teste a abertura em pelo menos um Android e um iPhone compativel, conectados a internet.
6. Altere o destino, texto ou tipo do QR pelo app e toque a tag novamente; ela deve refletir o novo comportamento sem regravacao.
7. Mantenha a tag desbloqueada durante os testes. So considere bloquea-la se houver uma necessidade operacional forte e uma amostra validada.

Use a URL HTTPS completa. Embora o NDEF possa codificar prefixos de URL para economizar bytes, a ferramenta de gravacao deve receber a URL normal; ela produz a codificacao NDEF apropriada.

## Limites e cuidados

- NFC passivo nao tem bateria e nao recebe alteracoes pela rede.
- Tags clonadas ou de baixa qualidade podem ter memoria, alcance ou durabilidade inferiores ao anunciado. Valide um lote pequeno antes da compra em escala.
- A tag e publica: qualquer pessoa com acesso fisico pode le-la. A URL deve permanecer publica e nao deve autorizar acoes privilegiadas.
- Um adesivo pode conter QR e NFC, mas materiais metalicos, agua, dobra e a posicao da antena podem degradar a leitura NFC mesmo quando o QR continua visivel.
- O redirecionamento depende da disponibilidade do dominio, Firebase Hosting e Firestore, exatamente como o QR atual.

## Integracao no Android (modulo `feature-nfc`)

A leitura, gravacao e bloqueio de tags NFC estao centralizadas no modulo
Android `android/feature-nfc`, consumido tanto por `:app` quanto por
`:app-qrcode`. O fluxo implementado:

1. Verifica se o aparelho possui NFC e se ele esta ativado.
2. Solicita a aproximacao de uma tag NDEF (foreground dispatch).
3. **Leitura:** decodifica o primeiro `NdefRecord` da tag — seja um URI
   Record ou um Text Record — e mostra o conteudo, seu tipo (URL ou Texto)
   e se a tag esta bloqueada (somente leitura).
4. **Gravacao:** o usuario escolhe entre duas fontes de conteudo:
   - **QR Code existente:** o app monta `"${qrCode.staticUrl}&source=nfc"` e
     grava um unico URI Record.
   - **Valor personalizado:** o usuario digita um texto livre, gravado como
     um unico Text Record (`NdefRecord.createTextRecord`).

   Em ambos os casos, antes de gravar o app valida se a mensagem NDEF
   resultante cabe na capacidade real da tag (`Ndef.maxSize`), reportando o
   erro `TagTooSmall` (com os bytes necessarios e disponiveis) caso nao
   caiba. Para o valor personalizado, a tela tambem exibe, em tempo real
   enquanto o usuario digita, um contador de bytes comparado a capacidade da
   tag minima recomendada (NTAG213, ~144 bytes de memoria de usuario) e um
   aviso caso o conteudo provavelmente nao caiba nela — sem bloquear a
   digitacao, ja que tags maiores (NTAG215/216) podem comportar mais dados.
5. **Bloqueio:** apos gravar o conteudo, o usuario escolhe se quer bloquear
   a tag definitivamente (`Ndef.makeReadOnly()`). O app exige aproximar a
   mesma tag novamente e deixa explicito, antes de confirmar, que a acao e
   **irreversivel**: uma vez bloqueada, nenhum app ou aparelho consegue
   regravar a tag. Nao existe uma opcao de bloqueio "temporario": qualquer
   marcacao que nao altere a memoria fisica da tag seria apenas uma
   preferencia local deste app/aparelho, sem nenhum efeito real sobre a tag
   em si, por isso essa opcao nao e oferecida.
6. **Ler/editar:** ao ler uma tag regravavel (nao bloqueada), a tela de
   leitura oferece os botoes **"Editar conteudo"** (leva o usuario direto ao
   fluxo de gravacao) e **"Bloquear definitivamente"** (aplica o bloqueio de
   hardware apos aproximar a tag novamente). Tags ja bloqueadas continuam
   mostrando apenas o aviso "Tag bloqueada (somente leitura)", sem acoes de
   edicao ou bloqueio.
7. Erros claros sao reportados para tags sem NDEF, sem espaco, bloqueadas,
   incompativeis, perdidas durante a operacao ou ausentes.

Na tela inicial do modulo, os botoes de acesso a cada fluxo se chamam
**"Ler/editar tag NFC"** (leitura, com as acoes de edicao/bloqueio acima) e
**"Gravar nova tag NFC"** (gravacao de uma tag ainda sem conteudo ou que sera
substituido).

Nenhuma mudanca no modelo Firestore ou no fluxo de resolucao de conteudo
(texto, redirect, mural) foi necessaria; apenas o novo campo `source` no
`access_log`, conforme descrito acima.

## Notificacao e detalhes do log por origem

O campo `source` (`qr` ou `nfc`) gravado no `access_log` tambem e usado em
dois pontos visiveis ao dono do QR Code:

- **Notificacao push:** a Cloud Function `notifyQrCodeAccess` (gatilho de
  criacao em `qrcodes/{qrCodeId}/access_logs/{logId}`) monta o corpo da
  mensagem indicando a origem do acesso, por exemplo `Acesso via NFC seu
  (Cidade, Pais)` ou `Acesso via QR Code seu (Cidade, Pais)`. O `source`
  tambem viaja no payload `data` da notificacao FCM para uso futuro no app.
- **Detalhes do log no app:** a tela `AccessLogMapScreen` (secao
  "Identificacao" do bottom sheet de detalhes) exibe a linha `Origem` com o
  rotulo amigavel `QR Code` ou `NFC`, obtido do `AccessLog.source` mapeado a
  partir do `QrCodeAccessLogDto.source`.

