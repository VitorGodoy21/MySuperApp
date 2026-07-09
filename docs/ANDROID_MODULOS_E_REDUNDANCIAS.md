# Android: módulos e redundâncias

## Visão geral

Hoje a pasta `android/` está dividida em três módulos com papéis diferentes:

| Módulo | Tipo | Papel principal |
| --- | --- | --- |
| `app` | application | App principal do projeto, com shell próprio e features além de QR Code |
| `app-qrcode` | application | App standalone focado em gestão de QR Codes |
| `feature-qrcode` | library | Módulo compartilhado com a maior parte da lógica de QR Code, autenticação e telas reutilizadas |

## O que cada diretório faz

### `android/app`

É o app principal. Ele funciona como um shell que monta a navegação principal e agrega features que não pertencem ao fluxo exclusivo de QR Code.

O que fica aqui:

- inicialização do app principal (`MySuperApplication`, `MainActivity`, `MySuperApp`)
- navegação principal com splash, login, home, financeiro, nova transação e entrada para os fluxos de QR Code
- drawer/menu do app principal
- dependências e DI do que é específico do app principal
- features próprias como:
  - `financial/`
  - `new_transaction/`
  - `home/`
  - biometria
  - repositórios de transação, cartão e categoria

Em resumo: `app` é o produto principal e usa `feature-qrcode` como feature compartilhada.

### `android/app-qrcode`

É um segundo app Android, separado do principal, voltado só para gestão de QR Codes.

O que fica aqui:

- inicialização própria (`QrCodeManagerApplication`, `QrCodeManagerActivity`)
- navegação própria (`QrCodeManagerNavGraph`, `QrCodeManagerScreen`)
- splash/sessão/logout próprios
- drawer próprio
- telas exclusivas deste app:
  - `profile/`
  - `notifications/`
  - `notification/` (serviço FCM)

Em resumo: `app-qrcode` é um shell standalone para operações de QR Code, mas reaproveita quase toda a lógica de negócio do `feature-qrcode`.

### `android/feature-qrcode`

É o módulo compartilhado entre os dois apps. Apesar do nome, ele não guarda só tela de QR Code: ele concentra boa parte da base reutilizável dos fluxos de autenticação e QR.

O que fica aqui:

- `data/`: DAOs, DTOs, repositórios remotos, storage local e mapeadores
- `domain/`: modelos, contratos de repositório, utilitários e use cases
- `presentation/`: telas Compose e view models compartilhados
- fluxos reutilizados por `app` e `app-qrcode`, como:
  - login
  - lista de QR Codes
  - detalhe/edição de QR Code
  - comentários do mural
  - mapa e estatísticas de acessos

Em resumo: `feature-qrcode` é o módulo que realmente contém a feature compartilhada; os dois apps só encaixam essa feature dentro de shells diferentes.

## Redundâncias encontradas

## Status atual

Parte da duplicação estrutural já foi reduzida:

- a inicialização de Firebase App Check foi centralizada em `feature-qrcode/common/FirebaseAppCheckInitializer.kt`
- a DI compartilhada de auth/QR/session/repositories/utilitários foi movida para:
  - `feature-qrcode/di/shared/SharedDaoModule.kt`
  - `feature-qrcode/di/shared/SharedRepositoryModule.kt`
  - `feature-qrcode/di/shared/SharedUtilModule.kt`
- as rotas compartilhadas de QR foram centralizadas em `feature-qrcode/navigation/SharedQrCodeRoutes.kt`
- `app-qrcode` deixou de manter módulos Hilt próprios para dependências que já eram totalmente compartilhadas

Com isso, a redundância mais pesada saiu dos módulos de DI e da inicialização comum.

## 1. Configuração base dos dois apps Android

Há bastante duplicação entre `app` e `app-qrcode` em:

- `build.gradle.kts`
- `AndroidManifest.xml`
- classe `Application`

O padrão repetido inclui:

- leitura de `MAPS_API_KEY` de `local.properties`
- flavors `dev` e `prd`
- `BuildConfig.IS_DEBUGGABLE`
- desativação de variantes `debug`
- dependências base de Compose, Firebase, Hilt, Coroutines, DataStore, ZXing e Maps

Diferenças reais:

- `app` adiciona financeiro, biometria e testes Compose
- `app-qrcode` adiciona `firebase-messaging`, profile e notifications

Conclusão: a maior parte da configuração dos dois apps ainda é parecida, mas a inicialização compartilhada de App Check já foi extraída para reduzir repetição de código Kotlin.

## 2. DI compartilhada centralizada no `feature-qrcode`

Antes havia duplicação clara em:

- `di/DaoModule.kt`
- `di/RepositoryModule.kt`
- `di/UtilModule.kt`
- boa parte de `di/UseCaseModule.kt`

Trechos repetidos:

- `AuthDao`
- `UserRemoteDao`
- `UserSessionSecureStorage`
- `QrCodeDao`
- `QrCodeAccessLogDao`
- `UserRepository`
- `QrCodeRepository`
- `QrCodeAccessLogRepository`
- `QrCodeGenerator`
- use cases de login e QR Code

Agora a parte compartilhada foi movida para `feature-qrcode`, e os shells ficaram assim:

- `app` mantém apenas o que é específico do app principal, como financeiro e biometria
- `app-qrcode` não precisa mais duplicar os módulos Hilt de auth/QR compartilhados
- os use cases com `@Inject constructor` passam a ser resolvidos diretamente pelo Hilt a partir dos bindings compartilhados

Conclusão: esta era a duplicação estrutural mais clara e foi removida sem mexer na lógica funcional do app.

## 3. Fluxo de sessão/splash duplicado

Existe duplicação quase direta entre:

- `presentation/my_super_app/SessionViewModel.kt`
- `QrCodeManagerSessionViewModel.kt`
- `presentation/my_super_app/SplashRoute.kt`
- `QrCodeManagerSplashRoute.kt`

Os dois fazem praticamente a mesma coisa:

- consultar `IsLoggedUseCase`
- exibir loading inicial
- redirecionar para login ou tela inicial

A única diferença real é o destino de navegação final:

- `app` vai para `HomeScreen`
- `app-qrcode` vai para `QrCodeList`

Conclusão: este fluxo está duplicado e poderia virar um componente compartilhado com destino parametrizável.

## 4. Rotas de QR parcialmente centralizadas

Ainda existem duas sealed classes de navegação:

- `feature-qrcode/.../Screen.kt`
- `app-qrcode/.../QrCodeManagerScreen.kt`

Mas os valores e builders compartilhados de QR agora saem de:

- `feature-qrcode/navigation/SharedQrCodeRoutes.kt`

O que já foi centralizado:

- lista de QR Codes
- detalhe de QR Code
- mapa de acessos
- comentários do mural

O que continua específico em cada shell:

- splash
- login
- profile
- notifications

- home
- financeiro
- nova transação

Conclusão: a duplicação de strings e builders de QR caiu, mas os shells ainda mantêm definições próprias de navegação para seus fluxos exclusivos.

## 5. Wrapper da lista de QR Codes parcialmente duplicado

Existe similaridade forte entre:

- `app/.../presentation/screen/qrcode_list/QrCodeListRoute.kt`
- `app-qrcode/.../QrCodeManagerListRoute.kt`

Trechos repetidos:

- coleta de `uiState`
- `SnackbarHostState`
- recarga ao voltar para a tela
- coleta de `QrCodeListEffect`
- uso de `AppScaffold`
- renderização de `QrCodeListScreen`

Diferença real:

- `app-qrcode` adiciona drawer próprio e fluxo de logout/profile/notifications
- `app` usa o drawer principal

Conclusão: a tela base é a mesma, mas cada app está mantendo um wrapper próprio com muita lógica repetida.

## O que parece intencional vs. o que parece redundante

### Faz sentido existir separado

- `app` e `app-qrcode` como dois apps distintos
- `MainActivity` vs `QrCodeManagerActivity`
- telas exclusivas de financeiro, home, profile e notifications
- manifests separados

### Parece redundância técnica

- configuração Gradle quase idêntica
- sessão/splash duplicados
- wrappers de navegação muito parecidos para a lista de QR

### Redundância já reduzida

- inicialização de Firebase App Check
- DI compartilhada de auth/QR/session/repositories/utilitários
- builders e constantes das rotas de QR

## Leitura prática da arquitetura atual

Se a dúvida for "onde mexer", a leitura mais segura hoje é:

| Se a mudança for em... | Lugar mais provável |
| --- | --- |
| fluxo financeiro, home, biometria, menu principal | `app` |
| profile, notifications, FCM do app standalone | `app-qrcode` |
| login, QR Code, mural, mapa, access log, repositórios Firebase compartilhados | `feature-qrcode` |
| bootstrapping compartilhado de App Check e bindings comuns de QR/auth | `feature-qrcode` |

## Direção recomendada

Sem mudar comportamento, a oportunidade mais clara de simplificação é:

1. compartilhar o fluxo de splash/sessão
2. reduzir a duplicação dos wrappers de navegação
3. avaliar se a configuração Gradle comum dos dois apps merece abstração adicional
4. reavaliar o nome `feature-qrcode`, porque hoje ele concentra mais do que apenas uma feature visual de QR Code

Hoje a arquitetura continua funcionando como dois shells sobre uma base compartilhada grande, mas a duplicação estrutural mais arriscada já foi reduzida sem mudar a lógica central dos fluxos.
