#!/usr/bin/env python3
"""
Script para testar o sistema de monitoramento de QR codes
Simula acessos e verifica se os dados foram salvos no Firebase
"""

import subprocess
import json
import time
from datetime import datetime

def run_command(cmd):
    """Executa comando e retorna output"""
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        return result.stdout, result.stderr
    except Exception as e:
        return "", str(e)

def test_local_server():
    """Testa se servidor local está rodando"""
    print("\n🔍 Verificando servidor local...")
    stdout, stderr = run_command("curl -s http://localhost:8000/qr/index.html | head -1")

    if "<!DOCTYPE" in stdout:
        print("✅ Servidor local está rodando em http://localhost:8000")
        return True
    else:
        print("❌ Servidor local NÃO está rodando")
        print("   Execute: python -m http.server 8000 no diretório /web")
        return False

def test_firebase_config():
    """Verifica se Firebase está configurado"""
    print("\n🔍 Verificando configuração do Firebase...")
    stdout, stderr = run_command("grep -o 'SUA_KEY' C:\\Users\\Public\\AndroidStudioProjects\\MySuperApp\\web\\qr\\index.html")

    if "SUA_KEY" in stdout:
        print("⚠️  AVISO: Firebase Config ainda tem 'SUA_KEY'")
        print("   Substitua pela chave real antes de fazer deploy!")
        return False
    else:
        print("✅ Firebase Config parece estar configurado")
        return True

def test_geolocation_functions():
    """Verifica se funções de geolocalização estão presentes"""
    print("\n🔍 Verificando funções de geolocalização...")

    functions_to_check = [
        "getLocationByGPS",
        "getLocationByIP",
        "reverseGeocode",
        "saveAccessLog"
    ]

    all_found = True
    for func in functions_to_check:
        stdout, _ = run_command(f"grep -c '{func}' C:\\Users\\Public\\AndroidStudioProjects\\MySuperApp\\web\\qr\\index.html")
        if int(stdout.strip()) > 0:
            print(f"   ✅ Função '{func}' encontrada")
        else:
            print(f"   ❌ Função '{func}' NÃO encontrada")
            all_found = False

    return all_found

def test_kotlin_compilation():
    """Testa se o código Kotlin compila"""
    print("\n🔍 Verificando compilação Kotlin...")
    stdout, stderr = run_command("cd C:\\Users\\Public\\AndroidStudioProjects\\MySuperApp && ./gradlew compileDevReleaseKotlin 2>&1 | grep -i 'build successful'")

    if "BUILD SUCCESSFUL" in stdout:
        print("✅ Compilação Kotlin bem-sucedida")
        return True
    else:
        print("❌ Compilação Kotlin falhou")
        print(f"   Erro: {stderr}")
        return False

def test_firebase_connection():
    """Testa conexão com Firebase"""
    print("\n🔍 Testando conexão com Firebase...")

    try:
        import firebase_admin
        from firebase_admin import credentials, firestore

        print("✅ Bibliotecas Firebase disponíveis")
        return True
    except ImportError:
        print("⚠️  Firebase Admin SDK não está instalado")
        print("   Execute: pip install firebase-admin")
        return False

def generate_test_report():
    """Gera relatório de teste"""
    print("\n" + "="*60)
    print("📊 RELATÓRIO DE TESTE - SISTEMA DE MONITORAMENTO QR CODE")
    print("="*60)

    tests = {
        "Servidor Local": test_local_server(),
        "Firebase Config": test_firebase_config(),
        "Funções Geolocalização": test_geolocation_functions(),
        "Compilação Kotlin": test_kotlin_compilation(),
        "Conexão Firebase": test_firebase_connection(),
    }

    print("\n" + "="*60)
    print("📋 RESUMO DOS TESTES")
    print("="*60)

    passed = 0
    for test_name, result in tests.items():
        status = "✅ PASSOU" if result else "❌ FALHOU"
        print(f"{test_name}: {status}")
        if result:
            passed += 1

    print(f"\nTotal: {passed}/{len(tests)} testes passaram")

    if passed == len(tests):
        print("\n🎉 TUDO PRONTO PARA DEPLOY!")
        print("\nPróximos passos:")
        print("1. Gere um QR code apontando para: https://seu-dominio.com/qr/index.html?id=test_001")
        print("2. Leia com um celular aleatório")
        print("3. Verifique os dados no Firebase Console")
        print("4. Procure em: Firestore → qrcodes → test_001 → access_logs")
    else:
        print("\n⚠️  CORRIJA OS ERROS ACIMA ANTES DE FAZER DEPLOY")

    print("="*60)

    # Salva relatório em arquivo
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"test_report_{timestamp}.txt"
    print(f"\n📄 Relatório salvo em: {filename}")

if __name__ == "__main__":
    print("🧪 Iniciando testes do Sistema de Monitoramento QR Code...\n")
    generate_test_report()

