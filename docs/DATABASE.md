# Database

## Firestore

### Collection `qrcodes`

Cada documento de QR Code usa o próprio document ID como identificador canônico do QR. O app Android gera a `staticUrl` no formato `https://baila.space/qr/?id={documentId}` e associa o documento ao usuário autenticado em `userId`.

Exemplo de documento base:

```json
{
  "identifier": "Sticker",
  "redirectUrl": "",
  "staticUrl": "https://baila.space/qr/?id=XTF9FopziwTWxRRscDkv",
  "text": "Testando o sistema, aguarde novidades ",
  "type": "TEXT",
  "userId": "U4cg9exb01axlBOAYg2xABb1k7k1"
}
```

Campos principais:

| Campo | Tipo | Observação |
| --- | --- | --- |
| `identifier` | string | Nome amigável opcional do QR Code. |
| `redirectUrl` | string | Usado quando `type = REDIRECT`. |
| `staticUrl` | string | URL pública hospedada no Firebase Hosting para leitura do QR. |
| `text` | string | Conteúdo exibido quando `type = TEXT`. |
| `type` | string | Valores atuais: `REDIRECT`, `TEXT`, `MURAL`. |
| `userId` | string | UID do usuário autenticado dono do QR Code. |

### Subcollections opcionais

Essas subcollections não precisam existir na criação do QR Code; elas surgem conforme uso do identificador:

- `qrcodes/{qrCodeId}/access_logs`
- `qrcodes/{qrCodeId}/comments`

`access_logs` armazena leituras com localização e contexto do dispositivo. `comments` é usada pelo fluxo de mural para comentários públicos associados ao QR Code.
