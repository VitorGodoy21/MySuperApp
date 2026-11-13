'use strict';
/**
 * Análise do relatório financeiro com IA (Gemini via Vertex AI, no próprio projeto GCP).
 *
 * Sem API key de terceiros: usa as credenciais padrão (ADC) da service account das Functions e a
 * Vertex não usa prompts para treino. O prompt recebe apenas o JSON do relatório (números,
 * títulos de lançamentos e categorias) — nunca uid/e-mail. Pré-requisitos no projeto:
 * `aiplatform.googleapis.com` habilitada e `roles/aiplatform.user` para a service account.
 */

const AI_MODEL = 'gemini-3.5-flash';

const AI_SYSTEM_PROMPT = `Você é um analista de finanças pessoais que escreve em português do Brasil, direto e sem jargão.
Receberá um JSON com o relatório financeiro mensal de uma pessoa (valores em reais). Regras:
- Use SOMENTE os números, títulos e categorias presentes no JSON. Não invente itens, valores nem percentuais que não possam ser calculados a partir dele.
- "summary" tem os totais do mês (income = receitas; cashExpense = o que saiu da conta = debitExpense + paidInvoiceTotal; balance = saldo; recurringExpense/installmentExpense/variableExpense = compras do mês por natureza; items.topRecurring/topVariable/installmentGroups = maiores lançamentos).
- "comparison.previousMonth" e "comparison.average3Months" trazem deltas (amount/percent) por métrica; "futureInstallments" são parcelas já comprometidas em faturas futuras; "alerts" são sinais calculados; "partial" = mês em andamento.
- Responda em Markdown usando apenas: títulos (#, ##, ###), listas (-, 1.), tabelas simples (| a | b |), negrito (**) e blocos de código (\`\`\`). Sem imagens, links ou HTML.
- Estrutura obrigatória: "# Análise de {mês}" · "## Resposta curta" (3 a 5 frases: quanto entrou, quanto saiu, quanto sobrou, principal causa) · "## Para onde foi o dinheiro" (fixo × parcelas × variável, citando os maiores itens) · "## O que mudou" (vs mês anterior/média, só se houver dados) · "## O que eu faria agora" (3 a 5 ações concretas e numéricas, em ordem de impacto) · "## Atenção" (limitações: mês parcial, sem receita, itens sem categoria etc.).
- Tom: consultor amigo, frases curtas, sem moralizar. Máximo de 600 palavras.`;

/** Remove campos que não ajudam o modelo (timestamps, texto já gerado, metadados). */
function stripForAi(doc) {
  const { narrative, aiNarrative, aiGeneratedAt, aiModel, generatedAt, trigger, schemaVersion, ...rest } = doc;
  return rest;
}

/**
 * Gera o Markdown da análise. `project` é o projeto GCP; `httpOptions` é repassado ao SDK
 * (útil fora das Functions para informar o quota project do ADC de usuário).
 */
async function generateAiNarrative(reportDoc, { project, httpOptions } = {}) {
  const { GoogleGenAI } = require('@google/genai');
  const ai = new GoogleGenAI({ vertexai: true, project, location: 'global', httpOptions });
  const response = await ai.models.generateContent({
    model: AI_MODEL,
    contents: [{ role: 'user', parts: [{ text: `Relatório (JSON):\n${JSON.stringify(stripForAi(reportDoc))}` }] }],
    config: {
      systemInstruction: AI_SYSTEM_PROMPT,
      temperature: 0.4,
      maxOutputTokens: 8192
    }
  });
  const text = (response.text || '').trim();
  if (!text) {
    const reason = response.candidates && response.candidates[0] && response.candidates[0].finishReason;
    throw new Error(`empty response (${reason || 'no candidates'})`);
  }
  return text;
}

module.exports = { AI_MODEL, generateAiNarrative, _internal: { stripForAi, AI_SYSTEM_PROMPT } };
