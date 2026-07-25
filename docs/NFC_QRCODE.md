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

Grave **um unico NDEF URI Record** com a `staticUrl` do QR Code. Exemplo:

```text
https://baila.space/qr/?id=XTF9FopziwTWxRRscDkv
```

NDEF (NFC Data Exchange Format) e o formato padrao de mensagens NFC. Um URI Record informa ao celular que o conteudo e uma URL; Android e iPhones modernos normalmente oferecem ou abrem o navegador ao aproximar uma tag com uma URL HTTPS.

Nao grave:

- O `redirectUrl` final, pois ele muda e eliminaria o controle centralizado.
- O texto ou o conteudo de um mural, pois eles tambem podem mudar.
- Credenciais, tokens, IDs internos de usuarios ou dados sensiveis.
- Uma URL diferente com `source=nfc`.

QR e NFC devem conter a mesma URL canonica. Por decisao de produto, os acessos nao serao distinguidos nos logs: ambos resultarao no mesmo `qrCodeId` e no mesmo fluxo de monitoramento.

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

## Possivel integracao futura no Android

Uma fase posterior pode adicionar ao `android/feature-qrcode` um fluxo compartilhado para:

1. Verificar se o aparelho possui NFC e se ele esta ativado.
2. Solicitar a aproximacao de uma tag NDEF.
3. Gravar um unico URI Record com `QrCode.staticUrl`.
4. Ler a tag em seguida e comparar a URL gravada com a URL esperada.
5. Informar erros claros para tags sem NDEF, sem espaco, bloqueadas, incompativeis ou ausentes.

As duas aplicacoes Android devem apenas integrar esse fluxo compartilhado. Nenhuma mudanca no modelo Firestore, no redirect ou na URL seria necessaria.
