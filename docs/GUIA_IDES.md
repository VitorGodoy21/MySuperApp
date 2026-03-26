# 🛠️ Guia de IDEs - Qual usar para cada parte

## 📊 Comparação de IDEs

### Para Android

| IDE | Recomendação | Pros | Contras |
|-----|--------------|------|---------|
| **Android Studio** | ⭐⭐⭐⭐⭐ | Oficial Google, melhor para Android, Emulador integrado | Pesado, não ideal para web |
| **IntelliJ IDEA** | ⭐⭐⭐⭐ | Poderosa, suporta tudo | Cara, overkill para Android puro |
| **VS Code** | ⭐⭐ | Leve | Faltam features Android específicas |

**Conclusão**: **Android Studio** é a melhor escolha

---

### Para Web

| IDE | Recomendação | Pros | Contras |
|-----|--------------|------|---------|
| **VS Code** | ⭐⭐⭐⭐⭐ | Leve, extensões web, Live Server | Nenhum (para web) |
| **WebStorm** | ⭐⭐⭐⭐ | Poderosa, JetBrains | Cara |
| **Sublime** | ⭐⭐⭐ | Leve, rápido | Menos features |
| **Vim/Neovim** | ⭐⭐⭐ | Ultraleve, profissional | Curva de aprendizado |

**Conclusão**: **VS Code** é a melhor escolha

---

### Para Ambos (um único IDE)

| IDE | Recomendação | Pros | Contras |
|-----|--------------|------|---------|
| **VS Code** | ⭐⭐⭐ | Extensões para Kotlin, Android Explorer | Não é ótimo para Android |
| **IntelliJ IDEA** | ⭐⭐⭐⭐ | Suporta tudo muito bem | Pesado |
| **Android Studio** | ⭐⭐ | Bom para Android | Péssimo para web |

**Conclusão**: Se quer um IDE só, use **IntelliJ IDEA Ultimate** (pago)

---

## 🎯 Recomendação Final

### ✅ Melhor Combinação

```
┌─────────────────────────────────────┐
│  Para Máxima Produtividade:         │
│                                     │
│  Android: Android Studio            │
│  Web:     VS Code                   │
│  Docs:    Markdown + VS Code        │
│                                     │
│  Abertas simultaneamente em         │
│  monitor duplo ou abas diferentes   │
└─────────────────────────────────────┘
```

---

## 📥 Setup Prático

### 1️⃣ Android Studio (Android)

**Download**: https://developer.android.com/studio

**Instalação**:
```bash
# Windows
# Baixe o instalador e execute

# Mac
brew install --cask android-studio

# Linux
sudo snap install android-studio --classic
```

**Abrir Projeto**:
```
File → Open → seu-caminho/MySuperApp/android
```

**Importante**: Abra `android/`, NÃO a raiz!

---

### 2️⃣ VS Code (Web + Docs)

**Download**: https://code.visualstudio.com

**Instalação**:
```bash
# Mac
brew install --cask visual-studio-code

# Windows
choco install vscode
# Ou: https://code.visualstudio.com/download

# Linux
sudo snap install code --classic
```

**Extensões Essenciais**:
```
1. Live Server (Ritwick Dey)
   └─ Para preview HTML em tempo real

2. Prettier (Code Formatter)
   └─ Formatar JavaScript/CSS

3. Thunder Client (Thunder Client)
   └─ Testar APIs (alternativa Postman)

4. Firebase Explorer (Rob Loureiro)
   └─ Ver dados do Firestore direto do VS Code

5. Markdown All in One (Yu Zhang)
   └─ Para editar documentação
```

**Instalar Extensões**:
```bash
# Via CLI
code --install-extension ritwickdey.LiveServer
code --install-extension esbenp.prettier-vscode
code --install-extension rangav.vscode-thunder-client
code --install-extension thaipham.firebase-explorer
code --install-extension yzhang.markdown-all-in-one
```

**Abrir Projeto**:
```bash
# Opção 1: Pasta individual
code ~/MySuperApp/web

# Opção 2: Workspace (recomendado)
code ~/MySuperApp
# Depois: File → Add Folder to Workspace
# Adicione: web/, docs/, android/
```

---

## 🎮 Fluxo de Desenvolvimento Ideal

### Configuração de Tela

**Se tiver 2 monitores** (ideal):
```
Monitor Esquerdo:          Monitor Direito:
┌─────────────────────┐   ┌─────────────────────┐
│  Android Studio     │   │  VS Code            │
│  (android/)         │   │  (web/ + docs/)     │
│                     │   │                     │
│  • Código Kotlin    │   │  • HTML/JS          │
│  • XML Resources    │   │  • CSS              │
│  • Emulador         │   │  • Markdown         │
│                     │   │  • Live Server      │
└─────────────────────┘   └─────────────────────┘
```

**Se tiver 1 monitor**:
```
Workspace: Abas vs Minimizar
├─ Tab: Android Studio (Alt+1)
├─ Tab: VS Code (Alt+2)
└─ Usar Alt+Tab para alternar
```

**Se trabalha mobile/laptop**:
```
Seu Workflow:
1. VS Code (web) no workspace principal
2. Android Studio em segundo plano (minimize)
3. Terminal compartilhado
```

---

## 🔄 Workflows Específicos

### Workflow 1: Mudança no QR Code HTML

```
1. Abra VS Code
   └─ Pasta: web/qr/
   
2. Edite: index.html
   └─ Live Server mostra preview automaticamente

3. Teste em navegador
   └─ http://localhost:5500/qr/index.html
   
4. Commit: git add web/ && git commit
```

### Workflow 2: Mudança no App Android

```
1. Abra Android Studio
   └─ Pasta: android/
   
2. Edite: kotlin files
   └─ Sync Gradle automaticamente
   
3. Rode no Emulador
   └─ Run → Run 'app'
   
4. Commit: git add android/ && git commit
```

### Workflow 3: Atualizar Documentação

```
1. Abra VS Code
   └─ Pasta: docs/
   
2. Edite: *.md files
   └─ Markdown Preview mostra automático
   
3. Commit: git add docs/ && git commit
```

---

## ⌨️ Atalhos Úteis

### Android Studio

```
Ctrl+N              Nova classe
Ctrl+Shift+N        Novo arquivo
Ctrl+Alt+L          Formatar código
Ctrl+/              Comentar linha
Ctrl+F              Buscar
Ctrl+H              Buscar e substituir
Ctrl+Shift+Up/Down  Mover linha
```

### VS Code

```
Ctrl+N              Novo arquivo
Ctrl+O              Abrir pasta
Ctrl+`              Terminal
Ctrl+/              Comentar linha
Ctrl+F              Buscar
Ctrl+H              Buscar e substituir
Alt+Up/Down         Mover linha
```

---

## 🚀 Setup Rápido (5 min)

### Windows/Mac/Linux

```bash
# 1. Instalar Android Studio
# Download em: https://developer.android.com/studio
# Ou: brew install --cask android-studio (Mac)

# 2. Instalar VS Code
# Download em: https://code.visualstudio.com
# Ou: brew install --cask visual-studio-code (Mac)

# 3. Instalar extensões VS Code
code --install-extension ritwickdey.LiveServer
code --install-extension esbenp.prettier-vscode
code --install-extension thaipham.firebase-explorer

# 4. Abrir projeto
cd MySuperApp
code .  # VS Code

# Depois abra android/ em Android Studio
```

---

## 📊 Tabela de Decisão

**Usar Android Studio quando**:
- ✅ Trabalhar em código Kotlin
- ✅ Fazer testes no emulador
- ✅ Modificar resources (strings.xml, colors.xml)
- ✅ Debug de app

**Usar VS Code quando**:
- ✅ Trabalhar em HTML/JavaScript
- ✅ Editar documentação
- ✅ Visualizar estrutura de pastas
- ✅ Usar Git
- ✅ Escrever CSS/JavaScript puro

**Usar ambos quando**:
- ✅ Trabalho full-stack (app + web simultaneamente)
- ✅ Fazer testes end-to-end
- ✅ Integração app com web

---

## ✨ Dicas Profissionais

1. **Use GitHub Desktop ou Git CLI**
   ```bash
   # Ao invés de fazer commits em cada IDE,
   # use terminal compartilhado:
   git status
   git add .
   git commit -m "feat: QR code logging"
   git push
   ```

2. **Terminal no VS Code**
   ```
   Ctrl+` para abrir terminal integrado
   ```

3. **Android Studio Terminal**
   ```
   View → Tool Windows → Terminal
   ```

4. **Live Reload**
   - Web: Live Server do VS Code (automático)
   - Android: Hot Reload (recarrega sem recompilar)

5. **Teste Local Simultâneo**
   ```
   Terminal 1: python -m http.server 8000 (web)
   Terminal 2: ./gradlew build (android)
   Emulador: Roda app
   ```

---

**Conclusão**: Combine **Android Studio + VS Code** para máxima produtividade! 🚀

