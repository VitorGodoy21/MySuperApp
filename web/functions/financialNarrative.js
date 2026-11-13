'use strict';
/**
 * Gera o texto (Markdown, pt-BR) do relatório financeiro mensal a partir do objeto produzido por
 * `financialReport.buildReport`. Puro e determinístico: nenhuma chamada externa.
 *
 * O formato segue a análise manual feita originalmente: resposta curta → conta do mês → para onde
 * vai o dinheiro (fixo / parcelas / variável, com os maiores itens) → comparação → alertas →
 * ações sugeridas → limitações. O app renderiza um subconjunto de Markdown: títulos `#`, listas
 * `-`/`1.`, tabelas `|`, blocos de código ``` e negrito `**`.
 */

const MONTHS_PT = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
const MONTHS_SHORT = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

function brl(value) {
  const v = Number.isFinite(value) ? value : 0;
  return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function num(value) {
  const v = Number.isFinite(value) ? value : 0;
  return v.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function pct(value, digits = 0) {
  return value == null || !Number.isFinite(value) ? '—' : `${value.toLocaleString('pt-BR', { maximumFractionDigits: digits })}%`;
}

function shareOf(part, whole) {
  return whole > 0 ? `${((part / whole) * 100).toLocaleString('pt-BR', { maximumFractionDigits: 0 })}%` : '—';
}

function monthLabel(key) {
  const [y, m] = key.split('-').map(Number);
  return `${MONTHS_PT[m - 1]}/${y}`;
}

function monthShort(key) {
  const [y, m] = key.split('-').map(Number);
  return `${MONTHS_SHORT[m - 1]}/${String(y).slice(2)}`;
}

function deltaText(d) {
  if (!d || d.amount == null) return 'sem referência';
  if (d.amount === 0) return 'igual';
  const arrow = d.amount > 0 ? '▲' : '▼';
  const p = d.percent == null ? '' : ` (${d.percent > 0 ? '+' : ''}${d.percent.toLocaleString('pt-BR', { maximumFractionDigits: 0 })}%)`;
  return `${arrow} ${brl(Math.abs(d.amount))}${p}`;
}

function table(headers, rows) {
  const line = (cells) => `| ${cells.join(' | ')} |`;
  return [line(headers), line(headers.map(() => '---')), ...rows.map(line)].join('\n');
}

function itemLabel(item) {
  const cat = item.categoryName ? ` · ${item.categoryName}` : '';
  return `${item.title || '(sem título)'}${cat}`;
}

// ─── Seções ──────────────────────────────────────────────────────────────────

function sectionShortAnswer(r) {
  const s = r.summary;
  const label = monthLabel(r.month);
  const rows = [];
  rows.push(['Quanto entrou?', s.income > 0 ? `**${brl(s.income)}** (${s.incomeCount} receita(s))` : '**Nenhuma receita registrada**']);
  rows.push(['Quanto saiu da conta?', `**${brl(s.cashExpense)}** = débito ${brl(s.debitExpense)} + faturas pagas ${brl(s.paidInvoiceTotal)}`]);
  if (s.income > 0) {
    const verb = s.balance >= 0 ? 'Sobrou' : 'Faltou';
    rows.push(['Quanto sobrou?', `**${verb} ${brl(Math.abs(s.balance))}** (${pct(s.savingsRate)} da renda)`]);
    rows.push(['Para onde foi?', `Fixo ${pct(s.recurringPercentOfIncome)} + parcelas ${pct(s.installmentPercentOfIncome)} + variável ${pct(s.variablePercentOfIncome)} da renda (compras do mês)`]);
  } else {
    rows.push(['Quanto sobrou?', 'Não dá para calcular sem receita no mês']);
  }
  const invoices = r.invoices.filter((i) => i.effectiveTotal > 0);
  if (invoices.length > 0) {
    const total = invoices.reduce((a, i) => a + i.effectiveTotal, 0);
    const projected = invoices.reduce((a, i) => a + i.projectedTotal, 0);
    const rec = invoices.reduce((a, i) => a + i.recurringTotal, 0);
    const inst = invoices.reduce((a, i) => a + i.installmentTotal, 0);
    const single = invoices.reduce((a, i) => a + i.singleTotal, 0);
    const comp = `${shareOf(rec, projected)} recorrente · ${shareOf(inst, projected)} parcelas · ${shareOf(single, projected)} compras avulsas`;
    const share = s.income > 0 ? ` (${shareOf(total, s.income)} da renda)` : '';
    rows.push(['É a fatura?', `A(s) fatura(s) somou(aram) **${brl(total)}**${share}. Composição: ${comp}. A fatura não é a causa — é onde o variável e as parcelas ficam invisíveis.`]);
  }
  return `## 1. Resposta curta — ${label}\n\n${table(['Pergunta', 'Resposta'], rows)}`;
}

function sectionAccount(r) {
  const s = r.summary;
  const w = 12;
  const line = (label, value, indent = 0) => `${' '.repeat(indent)}${label.padEnd(44 - indent)}${num(value).padStart(w)}`;
  const lines = [];
  lines.push(line('Receitas', s.income));
  lines.push(line('− Débito do mês', s.debitExpense));
  if (s.recurringDebitExpense != null) {
    lines.push(line('recorrentes no débito', s.recurringDebitExpense, 4));
    lines.push(line('avulsos/parcelas no débito', Math.max(0, s.debitExpense - s.recurringDebitExpense), 4));
  }
  for (const inv of r.invoices.filter((i) => i.effectiveTotal > 0)) {
    const status = inv.isPaid ? `paga${inv.paidOn ? ` em ${inv.paidOn.slice(8, 10)}/${inv.paidOn.slice(5, 7)}` : ''}` : 'em aberto, projetada';
    lines.push(line(`− Fatura ${inv.bank} ${monthShort(inv.invoiceMonth)} (${status})`, inv.effectiveTotal));
    if (inv.singleTotal > 0) lines.push(line('compras avulsas', inv.singleTotal, 4));
    if (inv.recurringTotal > 0) lines.push(line('recorrentes no crédito', inv.recurringTotal, 4));
    if (inv.installmentTotal > 0) lines.push(line(`parcelas (${inv.installmentCount})`, inv.installmentTotal, 4));
  }
  lines.push(line('= Saldo', s.balance) + (s.savingsRate != null ? `   (${pct(s.savingsRate, 1)})` : ''));
  let note = 'Esses números batem com Dashboard → Mês (Receitas / Despesas / Saldo). Compras no crédito só saem da conta quando a fatura é paga.';
  const mismatch = r.invoices.filter((i) => i.isPaid && Math.abs(i.paidAmount - i.projectedTotal) >= 1);
  if (mismatch.length > 0) {
    note += ' ' + mismatch.map((i) => `A fatura ${i.bank} ficou ${brl(Math.abs(i.paidAmount - i.projectedTotal))} ${i.paidAmount > i.projectedTotal ? 'acima' : 'abaixo'} do lançado — vale conferir.`).join(' ');
  }
  return `## 2. A conta do mês\n\n\`\`\`\n${lines.join('\n')}\n\`\`\`\n\n${note}`;
}

function sectionWhereItGoes(r) {
  const s = r.summary;
  const items = s.items || { topRecurring: [], topVariable: [], installmentGroups: [] };
  const parts = [];
  parts.push('## 3. Para onde vai o dinheiro (compras do mês)');

  // 3.1 fixo
  const fixedHeader = `### 3.1 Custo fixo — ${brl(s.recurringExpense)}/mês${s.income > 0 ? ` (${pct(s.recurringPercentOfIncome)} da renda)` : ''}`;
  if (items.topRecurring.length > 0) {
    const rows = items.topRecurring.map((i) => [itemLabel(i), num(i.amount), s.income > 0 ? shareOf(i.amount, s.income) : '—', i.paymentMethod === 'CREDIT' ? 'crédito' : 'débito']);
    parts.push(`${fixedHeader}\n\n${table(['Item', 'R$/mês', '% renda', 'Onde'], rows)}`);
    if (s.recurringDebitExpense != null) parts.push(`Débito: ${brl(s.recurringDebitExpense)} · Crédito (vai para a fatura): ${brl(s.recurringCreditExpense)}.`);
  } else {
    parts.push(`${fixedHeader}\n\nNenhuma despesa recorrente neste mês.`);
  }

  // 3.2 parcelas
  const f = r.futureInstallments || { months: [], topGroups: [], totalCommitted: 0, groupCount: 0 };
  const instHeader = `### 3.2 Parcelas — ${brl(s.installmentExpense)} neste mês${s.income > 0 ? ` (${pct(s.installmentPercentOfIncome)} da renda)` : ''}`;
  const instParts = [instHeader];
  if (items.installmentGroups.length > 0) {
    const rows = items.installmentGroups.map((g) => [
      itemLabel(g),
      g.installmentNumber && g.installments ? `${g.installmentNumber}/${g.installments}` : '—',
      num(g.amount),
      g.purchaseTotal != null ? num(g.purchaseTotal) : '—'
    ]);
    instParts.push(table(['Compra', 'Parcela', 'R$ no mês', 'Total compra'], rows));
  }
  if (f.months.length > 0) {
    instParts.push(`**Já comprometido nas próximas faturas: ${brl(f.totalCommitted)}** em ${f.groupCount} compra(s).`);
    instParts.push(table(['Fatura', 'Parcelas', 'R$'], f.months.map((m) => [monthShort(m.month), String(m.count), num(m.total)])));
    if (s.income > 0) {
      const next = f.months[0];
      const committed = s.recurringExpense + next.total;
      instParts.push(`Fixo + parcelas de ${monthShort(next.month)} ≈ **${brl(committed)} = ${shareOf(committed, s.income)} da renda** antes de qualquer gasto do dia a dia.`);
    }
  } else if (items.installmentGroups.length === 0) {
    instParts.push('Nenhuma parcela neste mês nem nas próximas faturas.');
  }
  parts.push(instParts.join('\n\n'));

  // 3.3 variável
  const varHeader = `### 3.3 Variável (avulsos) — ${brl(s.variableExpense)}${s.income > 0 ? ` (${pct(s.variablePercentOfIncome)} da renda)` : ''}`;
  const varParts = [varHeader];
  if (items.topVariable.length > 0) {
    const rows = items.topVariable.map((i) => [i.day ? String(i.day).padStart(2, '0') : '—', itemLabel(i), num(i.amount), i.paymentMethod === 'CREDIT' ? 'crédito' : 'débito']);
    varParts.push(`Maiores lançamentos:\n\n${table(['Dia', 'Item', 'R$', 'Onde'], rows)}`);
  }
  const cats = (r.categories || []).filter((c) => c.total > 0);
  if (cats.length > 0) {
    const rows = cats.map((c) => [c.name || 'Sem categoria', num(c.total), pct(c.percent), c.previousTotal == null ? '—' : deltaText(c.delta)]);
    varParts.push(`Por categoria (todas as compras do mês):\n\n${table(['Categoria', 'R$', '% gastos', 'vs mês anterior'], rows)}`);
  }
  if (s.uncategorizedCount > 0) {
    varParts.push(`**${s.uncategorizedCount} lançamento(s) sem categoria somam ${brl(s.uncategorizedTotal)}** — ${shareOf(s.uncategorizedTotal, s.totalExpense)} dos gastos está "cego" para a análise por categoria.`);
  }
  parts.push(varParts.join('\n\n'));
  return parts.join('\n\n');
}

const METRIC_LABELS = [
  ['income', 'Receitas'],
  ['cashExpense', 'Saiu da conta'],
  ['balance', 'Saldo'],
  ['recurringExpense', 'Fixo'],
  ['installmentExpense', 'Parcelas'],
  ['variableExpense', 'Variável'],
  ['creditExpense', 'Compras no crédito'],
  ['paidInvoiceTotal', 'Faturas pagas']
];

function sectionComparison(r) {
  const c = r.comparison || {};
  const prev = c.previousMonth;
  const avg = c.average3Months;
  if (!prev && !avg) return '## 4. Comparação\n\nAinda não há meses anteriores com dados para comparar.';
  const headers = ['Métrica', monthShort(r.month)];
  if (prev) headers.push(`vs ${monthShort(prev.month)}`);
  if (avg) headers.push(`vs média ${avg.monthsWithData}m`);
  const rows = METRIC_LABELS.map(([key, label]) => {
    const row = [label, num(r.summary[key])];
    if (prev) row.push(deltaText(prev.delta[key]));
    if (avg) row.push(deltaText(avg.delta[key]));
    return row;
  });
  const notes = [];
  if (prev) {
    const d = prev.delta.balance;
    if (d && d.amount != null && d.amount !== 0) {
      notes.push(`O saldo ${d.amount > 0 ? 'melhorou' : 'piorou'} ${brl(Math.abs(d.amount))} em relação a ${monthLabel(prev.month)}.`);
    }
    const v = prev.delta.variableExpense;
    if (v && v.amount != null && Math.abs(v.amount) >= 100) {
      notes.push(`O gasto variável ${v.amount > 0 ? 'subiu' : 'caiu'} ${brl(Math.abs(v.amount))}${v.percent != null ? ` (${pct(Math.abs(v.percent))})` : ''}.`);
    }
  }
  if (prev && prev.figures.income === 0) notes.push('O mês anterior não tem receita registrada, então as variações de saldo não são confiáveis.');
  return `## 4. Comparação\n\n${table(headers, rows)}${notes.length ? `\n\n${notes.join(' ')}` : ''}`;
}

function sectionAlerts(r) {
  const alerts = (r.alerts || []).filter((a) => a.code !== 'EMPTY_MONTH');
  if (alerts.length === 0) return '## 5. Alertas\n\nNenhum alerta neste mês.';
  const icon = { warning: '⚠️', info: 'ℹ️' };
  return `## 5. Alertas\n\n${alerts.map((a) => `- ${icon[a.severity] || '•'} ${a.message}`).join('\n')}`;
}

function sectionActions(r) {
  const s = r.summary;
  const f = r.futureInstallments || { months: [], totalCommitted: 0, topGroups: [] };
  const avg = r.comparison && r.comparison.average3Months ? r.comparison.average3Months.figures : null;
  const actions = [];

  if (s.income > 0 && s.variableExpense > 0) {
    const reference = avg && avg.variableExpense > 0 ? Math.min(s.variableExpense, avg.variableExpense) : s.variableExpense;
    const cap = Math.max(0, Math.round((reference * 0.8) / 100) * 100);
    if (cap > 0 && cap < s.variableExpense) {
      actions.push(`**Teto de variável: ${brl(cap)}/mês** (20% abaixo do que você vem gastando). Acompanhe na aba Filtros → "Só não recorrentes". Impacto: até ${brl(s.variableExpense - cap)}/mês.`);
    }
  }
  if (s.income > 0 && f.totalCommitted > s.income * 0.1 && f.months.length > 0) {
    const last = f.months[f.months.length - 1];
    const top = (f.topGroups || []).slice(0, 3).map((g) => `${g.title} (${brl(g.remainingTotal)})`).join(', ');
    actions.push(`**Congelar novos parcelamentos até ${monthShort(last.month)}** — há ${brl(f.totalCommitted)} em parcelas abertas${top ? `: ${top}` : ''}. Cada compra parcelada vira "fixo temporário".`);
  }
  if (s.income > 0 && s.recurringPercentOfIncome != null && s.recurringPercentOfIncome > 40) {
    const top = ((s.items && s.items.topRecurring) || []).slice(0, 3).map((i) => `${i.title} ${brl(i.amount)}`).join(' · ');
    actions.push(`**Revisar o custo fixo (${pct(s.recurringPercentOfIncome)} da renda)**${top ? `: maiores itens ${top}` : ''}. Assinaturas e serviços que não usa toda semana são os primeiros candidatos.`);
  }
  if (s.uncategorizedCount > 0) {
    actions.push(`**Categorizar os ${s.uncategorizedCount} lançamentos sem categoria** (${brl(s.uncategorizedTotal)}) para a análise por categoria fazer sentido no próximo mês.`);
  }
  const mismatch = r.invoices.filter((i) => i.isPaid && Math.abs(i.paidAmount - i.projectedTotal) >= 1);
  if (mismatch.length > 0) {
    actions.push(`**Conferir a fatura ${mismatch.map((i) => i.bank).join(', ')}**: o valor pago difere do lançado em ${mismatch.map((i) => brl(Math.abs(i.paidAmount - i.projectedTotal))).join(', ')} — pode ser estorno, item faltando ou duplicado.`);
  }
  if (s.income > 0 && s.balance > 0) {
    const save = Math.round((s.income * 0.15) / 100) * 100;
    actions.push(`**Pagar a si mesmo primeiro:** no dia em que a receita cair, transfira ${brl(save)} (15% da renda) para reserva/investimento. Com as ações acima isso cabe no orçamento.`);
  } else if (s.income > 0 && s.balance < 0) {
    actions.push(`**Fechar o buraco de ${brl(Math.abs(s.balance))}**: priorize o teto de variável e evite novas compras no crédito até a próxima fatura.`);
  }
  if (actions.length === 0) return '## 6. Ações sugeridas\n\nNada urgente: o mês fechou dentro do esperado. Mantenha o acompanhamento semanal.';
  return `## 6. Ações sugeridas (ordem de impacto)\n\n${actions.map((a, i) => `${i + 1}. ${a}`).join('\n')}`;
}

function sectionLimits(r) {
  const s = r.summary;
  const notes = [];
  if (r.partial) notes.push('Relatório **parcial**: o mês ainda não terminou, então receitas, faturas e variável vão mudar até o fechamento.');
  if (s.income === 0) notes.push('Sem receita registrada no mês — saldo e porcentagens da renda não são representativos.');
  const prev = r.comparison && r.comparison.previousMonth;
  if (!prev) notes.push('Sem mês anterior com dados: as comparações aparecem só a partir do segundo mês de uso.');
  else if (prev.figures.income === 0) notes.push('Mês anterior sem receita registrada: comparações de saldo distorcidas.');
  const open = r.invoices.filter((i) => !i.isPaid && i.effectiveTotal > 0);
  if (open.length > 0) notes.push(`Fatura(s) ${open.map((i) => i.bank).join(', ')} ainda em aberto: entraram pelo valor projetado.`);
  notes.push('Fixo / parcelas / variável e categorias usam as compras **datadas** no mês (regime de competência); "saiu da conta" usa débito + faturas pagas (caixa).');
  return `## 7. Limitações\n\n${notes.map((n) => `- ${n}`).join('\n')}`;
}

/** Markdown completo do relatório. */
function buildNarrative(report) {
  const s = report.summary;
  const title = `# Relatório financeiro — ${monthLabel(report.month)}${report.partial ? ' (parcial)' : ''}`;
  if (!s || s.transactionCount === 0) {
    return `${title}\n\nNenhuma transação registrada em ${monthLabel(report.month)}. Lance receitas e despesas no app para o relatório ser gerado.`;
  }
  return [
    title,
    `> Gerado automaticamente a partir dos seus lançamentos (${s.transactionCount} transações). Regras de cálculo idênticas ao Dashboard.`,
    sectionShortAnswer(report),
    sectionAccount(report),
    sectionWhereItGoes(report),
    sectionComparison(report),
    sectionAlerts(report),
    sectionActions(report),
    sectionLimits(report)
  ].join('\n\n');
}

module.exports = { buildNarrative, _internal: { deltaText, table, monthLabel, monthShort } };
