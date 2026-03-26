# ⚙️ VS Code Settings para Web Development

## 📁 Criar arquivo `.vscode/settings.json` na pasta `web/`

Crie a pasta `.vscode` dentro de `web/`:

```bash
mkdir web/.vscode
```

Depois crie o arquivo `web/.vscode/settings.json`:

```json
{
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true,
  "editor.insertSpaces": true,
  "editor.tabSize": 2,
  "editor.wordWrap": "on",
  "files.autoSave": "afterDelay",
  "files.autoSaveDelay": 1000,
  "files.exclude": {
    "**/node_modules": true,
    "**/.git": true,
    "**/.DS_Store": true
  },
  "html.format.enable": true,
  "html.format.indentHandlebars": true,
  "html.format.indentInnerHtml": true,
  "html.format.wrapAttributes": "auto",
  "html.validate.styles": true,
  "javascript.format.enable": true,
  "javascript.validate.enable": true,
  "css.validate": true,
  "liveServer.settings.port": 5500,
  "liveServer.settings.root": "/",
  "liveServer.settings.useWebExt": false
}
```

## 📋 Extensões Recomendadas

Crie arquivo `web/.vscode/extensions.json`:

```json
{
  "recommendations": [
    "ritwickdey.LiveServer",
    "esbenp.prettier-vscode",
    "ecmel.vscode-html-css",
    "bradlc.vscode-tailwindcss",
    "dbaeumer.vscode-eslint",
    "thaipham.firebase-explorer",
    "yzhang.markdown-all-in-one"
  ]
}
```

## 🎯 Launch Configuration

Crie arquivo `web/.vscode/launch.json` para debug:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Live Server",
      "type": "chrome",
      "request": "launch",
      "url": "http://localhost:5500",
      "webRoot": "${workspaceFolder}",
      "sourceMaps": true
    }
  ]
}
```

## 📝 Recomendações VS Code

Arquivo `web/.vscode/recommendations.json`:

```json
{
  "recommendations": [
    {
      "name": "Live Server",
      "extension": "ritwickdey.LiveServer",
      "description": "Preview HTML em tempo real"
    },
    {
      "name": "Prettier",
      "extension": "esbenp.prettier-vscode",
      "description": "Formatar código automaticamente"
    },
    {
      "name": "Firebase Explorer",
      "extension": "thaipham.firebase-explorer",
      "description": "Gerenciar Firestore direto do VS Code"
    }
  ]
}
```

## 🚀 Tasks do VS Code

Crie arquivo `web/.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Start Live Server",
      "type": "shell",
      "command": "python -m http.server 8000",
      "isBackground": true,
      "problemMatcher": {
        "pattern": {
          "regexp": ".*",
          "file": 1,
          "location": 2,
          "message": 3
        },
        "background": {
          "activeOnStart": true,
          "beginsPattern": "^.*Server started.*",
          "endsPattern": "^.*Serving HTTP.*"
        }
      }
    }
  ]
}
```

## 💻 .gitignore para Web

Arquivo `web/.gitignore`:

```
# Dependências
node_modules/
package-lock.json
yarn.lock

# Build
dist/
build/

# Ambiente
.env.local
.env.*.local

# IDE
.vscode/settings.json
.vscode/launch.json
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Logs
*.log
npm-debug.log*
```

## 📦 package.json para Web

Arquivo `web/package.json`:

```json
{
  "name": "mysuperapp-web",
  "version": "1.0.0",
  "description": "Frontend web do MySuperApp",
  "main": "index.js",
  "scripts": {
    "start": "python -m http.server 8000",
    "dev": "python -m http.server 8000",
    "build": "echo 'No build needed for static files'",
    "deploy": "firebase deploy --only hosting"
  },
  "keywords": ["qr-code", "firebase"],
  "author": "Seu Nome",
  "license": "MIT",
  "devDependencies": {
    "prettier": "^2.8.0"
  }
}
```

Execute para criar o arquivo:

```bash
cd web
npm init -y
```

Ou copie o JSON acima para `web/package.json`.

## 🔐 .env.example

Arquivo `web/.env.example`:

```env
# Firebase Configuration
FIREBASE_API_KEY=sua-api-key-aqui
FIREBASE_AUTH_DOMAIN=seu-projeto.firebaseapp.com
FIREBASE_PROJECT_ID=seu-projeto
FIREBASE_STORAGE_BUCKET=seu-projeto.appspot.com
FIREBASE_MESSAGING_SENDER_ID=seu-sender-id
FIREBASE_APP_ID=seu-app-id

# App Configuration
REACT_APP_NAME=MySuperApp
REACT_APP_VERSION=1.0.0
REACT_APP_ENVIRONMENT=development
```

Copiar para `.env.local` para usar (não commit).

## 📚 README para Web

Arquivo `web/README.md`:

```markdown
# Web Frontend - MySuperApp

Frontend da aplicação com página de QR Code.

## Estrutura

```
web/
├── qr/              QR Code page
│   └── index.html
├── dashboard/       Dashboard (futuro)
├── .vscode/         Configurações VS Code
├── package.json     Dependências
└── README.md
```

## Setup

```bash
cd web

# Opção 1: Servidor Python
python -m http.server 8000
# Acessa: http://localhost:8000

# Opção 2: Live Server (VS Code)
# Instale extensão: Live Server
# Clique botão "Go Live"
```

## Estrutura de Pastas

- `qr/` - Página de QR Code (acesso aos usuários)
- `dashboard/` - Dashboard de estatísticas (futuro)

## Desenvolvimento

```bash
# Com Live Server do VS Code
# Arquivo salvo = página recarregada automaticamente

# Ou com terminal
python -m http.server 8000
```

## Deploy

```bash
# Firebase Hosting
firebase deploy --only hosting:qr
```
```

Salve como `web/README.md`.

