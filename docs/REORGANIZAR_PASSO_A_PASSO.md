# 📦 Guia Prático: Reorganizar seu Projeto

## 🎯 Objetivo

Transformar sua estrutura atual em um monorepo profissional.

---

## ✅ Pré-Requisitos

- [ ] Projeto commit no Git (segurança)
- [ ] Espaço livre em disco (~1-2GB)
- [ ] Terminal aberto (PowerShell/CMD no Windows)

---

## 🚀 Passo a Passo

### Passo 1: Backup (IMPORTANTE!)

```bash
# Criar branch de segurança
git checkout -b backup-reorganizar
git push origin backup-reorganizar

# Voltar para main/master
git checkout main
```

### Passo 2: Criar Pasta `android`

```powershell
# Windows PowerShell
New-Item -ItemType Directory -Path "android" -Force

# Ou via terminal
mkdir android
```

### Passo 3: Mover Arquivos Android para `android/`

```powershell
# Windows PowerShell

# Mover pasta
Move-Item app android/
Move-Item gradle android/

# Mover arquivos
Move-Item build.gradle.kts android/
Move-Item settings.gradle.kts android/
Move-Item gradle.properties android/
Move-Item gradlew android/
Move-Item gradlew.bat android/
Move-Item local.properties android/ -ErrorAction SilentlyContinue
Move-Item .idea android/ -ErrorAction SilentlyContinue
Move-Item *.iml android/ -ErrorAction SilentlyContinue
```

Ou em **Linux/Mac**:
```bash
mkdir -p android

mv app android/
mv gradle android/
mv build.gradle.kts android/
mv settings.gradle.kts android/
mv gradle.properties android/
mv gradlew android/
mv gradlew.bat android/
mv local.properties android/ 2>/dev/null
mv .idea android/ 2>/dev/null
mv *.iml android/ 2>/dev/null
```

### Passo 4: Criar Pastas para Web

```powershell
# Windows
New-Item -ItemType Directory -Path "web/qr" -Force
New-Item -ItemType Directory -Path "web/dashboard" -Force
New-Item -ItemType Directory -Path "docs" -Force
```

Ou **Linux/Mac**:
```bash
mkdir -p web/qr
mkdir -p web/dashboard
mkdir -p docs
```

### Passo 5: Mover/Criar Arquivos Web

```bash
# Se o arquivo web/qr/index.html já existe, deixa onde está
# (ele já deve estar em web/)

# Se não existir:
# Copie qualquer HTML para web/qr/index.html
```

### Passo 6: Criar Documentação Raiz

Criar arquivo `README.md` na raiz:

```markdown
# MySuperApp - Monorepo

Projeto que combina app Android e web em um repositório único.

## 📁 Estrutura

- **android/** - App Android (abrir em Android Studio)
- **web/** - Frontend web (abrir em VS Code)
- **docs/** - Documentação compartilhada

## 🚀 Como Começar

### Android
```bash
cd android
./gradlew build
```

### Web
```bash
cd web
python -m http.server 8000
# Acessa: http://localhost:8000
```

## 📖 Documentação

- [Organização](docs/ORGANIZACAO_MONOREPO.md)
- [IDEs](docs/GUIA_IDES.md)
- [Setup](docs/SETUP.md)
```

### Passo 7: Atualizar `.gitignore` na Raiz

Criar/atualizar `.gitignore`:

```
# Android
android/.gradle/
android/build/
android/.idea/
android/local.properties
android/*.iml
android/*.apk
android/*.aab

# Web
web/node_modules/
web/dist/
web/.env.local

# Docs
docs/.DS_Store

# OS
.DS_Store
Thumbs.db

# IDE
.idea/
.vscode/settings.json
*.swp
*.swo
```

### Passo 8: Verificar Estrutura

```bash
# Ver resultado final
tree -L 2 -a

# Deve mostrar:
# .
# ├── android/
# │   ├── app/
# │   ├── gradle/
# │   ├── build.gradle.kts
# │   └── ...
# ├── web/
# │   ├── qr/
# │   ├── dashboard/
# │   └── ...
# ├── docs/
# │   ├── ORGANIZACAO_MONOREPO.md
# │   ├── GUIA_IDES.md
# │   └── ...
# ├── README.md
# └── .gitignore
```

### Passo 9: Commit das Mudanças

```bash
# Verificar o que mudou
git status

# Adicionar tudo
git add .

# Commit
git commit -m "refactor: reorganizar projeto em monorepo

- Mover app Android para pasta android/
- Organizar web em pasta web/
- Centralizar docs em docs/
- Atualizar .gitignore
"

# Enviar para repositório
git push origin main
```

---

## 🔧 Após Reorganizar: Configurar IDEs

### Android Studio

```
1. File → Open
2. Navegue para: MySuperApp/android
3. Clique OK
4. Deixe fazer sync dos gradle files

✅ Pronto! Android Studio agora aponta para android/
```

### VS Code

```
1. File → Open Folder
2. Navegue para: MySuperApp/web
3. Clique Select Folder

✅ Pronto! VS Code abre web/

# (Opcional) Abrir Workspace
1. File → Add Folder to Workspace
2. Adicione: android/, docs/, web/
3. Salve workspace: File → Save Workspace As...
   Nome: MySuperApp.code-workspace
```

---

## ✅ Checklist Pós-Reorganização

- [ ] Estrutura de pastas criada
- [ ] Arquivos movidos para lugar certo
- [ ] `.gitignore` atualizado
- [ ] `README.md` raiz criado
- [ ] Commit enviado ao Git
- [ ] Android Studio aponta para `android/`
- [ ] VS Code abre `web/`
- [ ] Compilação Android funciona: `cd android && ./gradlew build`
- [ ] Web abre no navegador: `cd web && python -m http.server 8000`

---

## 🚀 Problema? Desfaça!

Se algo deu errado, é simples:

```bash
# Voltar para branch de backup
git checkout backup-reorganizar

# Ou reverter commit
git reset --hard HEAD~1

# Ou restaurar do Git
git checkout HEAD -- .
```

---

## 🎯 Resultado Final

Depois de tudo:

```
Você terá:
✅ Android Studio: abre android/ (IDE ideal)
✅ VS Code: abre web/ (IDE ideal)
✅ Documentação: centralizada em docs/
✅ Git: estrutura clara e profissional
✅ Colaboração: fácil separar trabalhos
```

---

## 💡 Dicas

1. **Não force Android Studio abrir a raiz**
   - Sempre abra a pasta `android/` específica

2. **Use múltiplos workspaces**
   - VS Code: workspace individual para web/
   - Android Studio: project individual para android/

3. **Gitignore importante**
   - Sem isso, commitará arquivos desnecessários
   - Afeta performance do Git

4. **Documentação é essencial**
   - README em cada pasta principal
   - Onboard rápido para novos devs

---

**Pronto para reorganizar?** Comece pelo Passo 1! 🚀

