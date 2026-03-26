# 🚀 Como Executar o Script de Reorganização

## 🖥️ Windows (PowerShell)

### **Opção 1: Script PowerShell (MAIS FÁCIL) ⭐**

Criei um arquivo `reorganize.ps1` especialmente para Windows!

```powershell
# 1. Abra PowerShell como Administrador
#    Clique direito no PowerShell → "Run as Administrator"

# 2. Navegue até o projeto
cd C:\Users\Public\AndroidStudioProjects\MySuperApp

# 3. Permita execução de scripts (primeira vez)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 4. Execute o script
.\reorganize.ps1
```

**Pronto!** O script fará tudo automaticamente.

---

### **Opção 2: WSL (Windows Subsystem for Linux)**

Se tiver WSL instalado:

```powershell
# Na raiz do projeto
wsl bash reorganize.sh
```

---

### **Opção 3: Git Bash**

Se tiver Git instalado:

```bash
# 1. Clique direito na pasta do projeto
# 2. Selecione: "Git Bash Here"

# 3. Execute
bash reorganize.sh
```

---

## 🍎 Mac / Linux

### Execute Direto

```bash
# 1. Abra Terminal
# 2. Navegue até o projeto
cd ~/caminho/para/MySuperApp

# 3. Dê permissão de execução (primeira vez)
chmod +x reorganize.sh

# 4. Execute
./reorganize.sh
```

---

## 📋 O que o Script Faz

Quando você executa qualquer um dos scripts, ele:

```
1. ✅ Cria pasta 'android/'
2. ✅ Move app/ para android/
3. ✅ Move gradle/ para android/
4. ✅ Move arquivos gradle para android/
5. ✅ Cria pasta 'docs/'
6. ✅ Organiza pasta 'web/'
7. ✅ Cria README.md
8. ✅ Exibe resultado final
```

---

## ✅ Verificar se Funcionou

Após executar, você deve ver:

```
📦 Reorganizando MySuperApp para Monorepo...
✅ Pasta 'android' criada
✅ app/ movido
✅ gradle/ movido
✅ build.gradle.kts movido
✅ settings.gradle.kts movido
✅ gradle.properties movido
✅ gradlew movido
✅ gradlew.bat movido
✅ local.properties movido
✅ Pasta 'docs' criada
✅ Estrutura 'web' melhorada
✅ README.md criado

✨ Monorepo reorganizado com sucesso!
```

Se vir isso, significa que funcionou! 🎉

---

## 📁 Verificar Estrutura

Após o script, verifique se a estrutura ficou correta:

```powershell
# Windows PowerShell
ls -Recurse -Depth 2

# Deve mostrar:
# android/
#   app/
#   gradle/
#   build.gradle.kts
#   ...
# web/
#   qr/
#   dashboard/
# docs/
```

---

## 🔧 Se Algo Deu Errado

### Desfazer via Git

```bash
# Voltar ao estado anterior
git reset --hard HEAD

# Ou volta para backup
git checkout backup-reorganizar
```

### Manual (Se preferir fazer na mão)

```powershell
# Windows PowerShell

# Criar pastas
New-Item -ItemType Directory -Path "android" -Force
New-Item -ItemType Directory -Path "docs" -Force

# Mover arquivos (um por um)
Move-Item app android/
Move-Item gradle android/
Move-Item build.gradle.kts android/
Move-Item settings.gradle.kts android/
Move-Item gradle.properties android/
Move-Item gradlew android/
Move-Item gradlew.bat android/
```

---

## 🎯 Resumo das Opções

| Sistema | Recomendado | Comando |
|---------|-------------|---------|
| **Windows** | `reorganize.ps1` | `.\reorganize.ps1` |
| **Windows (Git)** | `reorganize.sh` | `bash reorganize.sh` |
| **Mac** | `reorganize.sh` | `./reorganize.sh` |
| **Linux** | `reorganize.sh` | `./reorganize.sh` |

---

## ⚡ Próximas Ações Após Script

Depois que o script terminar:

```bash
# 1. Verificar estrutura
ls -la

# 2. Adicionar ao Git
git add .

# 3. Commit
git commit -m "refactor: reorganizar em monorepo"

# 4. Enviar
git push origin main

# 5. Abrir no Android Studio
#    File → Open → android/

# 6. Abrir no VS Code
#    code web/
```

---

**Recomendação**: Use a **Opção 1** (PowerShell Script) se estiver no Windows! É mais fácil e foi feita especialmente para Windows. ✅

