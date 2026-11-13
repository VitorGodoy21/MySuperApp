'use strict';
/**
 * Motor puro do relatório financeiro mensal (sem Firebase).
 *
 * Replica as fórmulas do app Android (`FinancialTotals`, `GetTransactionsUseCase`,
 * `GetPaidInvoicesUseCase`, `RecurrenceRules`) para que os números do relatório batam com o
 * Dashboard: parcelas entram pelo `invoiceMonth`, recorrentes são materializadas mês a mês
 * (recurrenceStart/End, excludedMonths, overrides), a fatura paga de cada cartão segue
 * `paidInvoiceMonth(closingDay, invoiceDueDate, M)` e o valor pago substitui o projetado.
 *
 * Entrada: documentos crus do Firestore já convertidos (`transactionDate` pode ser Timestamp
 * Admin, Date ou millis). Saída: objeto serializável gravado em
 * `users/{uid}/financial_reports/{yyyy-MM}`.
 */

const SCHEMA_VERSION = 2;
const TZ_OFFSET_MS = 3 * 60 * 60 * 1000; // America/Sao_Paulo (UTC-3, sem horário de verão)
const CREDIT = 'CREDIT';
const INVOICE_PAYMENT = 'INVOICE_PAYMENT';
const TOP_CATEGORIES = 8;
const TOP_INSTALLMENT_GROUPS = 10;
const FUTURE_INSTALLMENT_MONTHS = 6;
const TOP_ITEMS = 12;

// ─── Meses (strings yyyy-MM) ─────────────────────────────────────────────────

function monthKey(year, month) {
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}`;
}

function isMonthKey(value) {
  return typeof value === 'string' && /^\d{4}-(0[1-9]|1[0-2])$/.test(value);
}

function addMonths(key, n) {
  const [y, m] = key.split('-').map(Number);
  const total = y * 12 + (m - 1) + n;
  return monthKey(Math.floor(total / 12), (total % 12) + 1);
}

function toMillis(value) {
  if (value == null) return null;
  if (typeof value === 'number') return value;
  if (value instanceof Date) return value.getTime();
  if (typeof value.toMillis === 'function') return value.toMillis();
  if (typeof value.seconds === 'number') return value.seconds * 1000;
  if (typeof value._seconds === 'number') return value._seconds * 1000;
  return null;
}

function localDate(value) {
  const ms = toMillis(value);
  return ms == null ? null : new Date(ms - TZ_OFFSET_MS);
}

function monthOf(value) {
  const d = localDate(value);
  return d ? monthKey(d.getUTCFullYear(), d.getUTCMonth() + 1) : null;
}

function dayOf(value) {
  const d = localDate(value);
  return d ? d.getUTCDate() : null;
}

function currentMonth(now = Date.now()) {
  return monthOf(now);
}

// ─── Regras de domínio ───────────────────────────────────────────────────────

const amountOf = (t) => (typeof t.amount === 'number' && Number.isFinite(t.amount) ? t.amount : 0);
const isExpense = (t) => (t.transactionType || 'EXPENSE') === 'EXPENSE';
const isIncome = (t) => t.transactionType === 'INCOME';
const isInvoicePayment = (t) => t.paymentMethod === INVOICE_PAYMENT;
const isPurchase = (t) => isExpense(t) && !isInvoicePayment(t);
const isCredit = (t) => t.paymentMethod === CREDIT;
const isInstallment = (t) => !t.isRecurring && t.installmentGroupId != null;

function recurrenceStart(t) {
  return isMonthKey(t.recurrenceStart) ? t.recurrenceStart : monthOf(t.transactionDate);
}

function occursIn(t, month) {
  if (!t.isRecurring) return false;
  const start = recurrenceStart(t);
  if (!start || month < start) return false;
  if (isMonthKey(t.recurrenceEnd) && month > t.recurrenceEnd) return false;
  return !(Array.isArray(t.excludedMonths) && t.excludedMonths.includes(month));
}

function materialize(t, month) {
  const override = t.overrides && typeof t.overrides === 'object' ? t.overrides[month] : null;
  return {
    ...t,
    title: override && typeof override.title === 'string' ? override.title : t.title,
    amount: override && typeof override.amount === 'number' ? override.amount : amountOf(t),
    occurrenceMonth: month
  };
}

function dueAfterClosingSameMonth(card) {
  const c = card.closingDay, d = card.invoiceDueDate;
  return Number.isInteger(c) && Number.isInteger(d) && c >= 1 && c <= 31 && d >= 1 && d <= 31 && d > c;
}

function paidInvoiceMonth(card, month) {
  return dueAfterClosingSameMonth(card) ? month : addMonths(month, -1);
}

const sum = (items, fn = amountOf) => round2(items.reduce((acc, item) => acc + (fn(item) || 0), 0));

function round2(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

// ─── Seleção de transações ───────────────────────────────────────────────────

/** Mesmo conjunto que a aba "Mês" do Dashboard (GetTransactionsUseCase.mergeWithRecurring). */
function monthTransactions(all, month) {
  const dated = all.filter(
    (t) => !t.isRecurring && t.installmentGroupId == null && monthOf(t.transactionDate) === month
  );
  const installments = all.filter((t) => isInstallment(t) && t.invoiceMonth === month);
  const occurrences = all.filter((t) => occursIn(t, month)).map((t) => materialize(t, month));
  return [...dated, ...installments, ...occurrences];
}

/** Itens da fatura `invoiceMonth` (FinancialTotals.invoiceItems com dedup das recorrentes). */
function invoiceItems(all, invoiceMonth) {
  const byInvoice = all.filter((t) => t.invoiceMonth === invoiceMonth && !isInvoicePayment(t));
  const recurring = all
    .filter((t) => occursIn(t, invoiceMonth))
    .map((t) => materialize(t, invoiceMonth))
    .filter((t) => isExpense(t) && isCredit(t) && t.invoiceMonth !== invoiceMonth);
  return [...byInvoice, ...recurring];
}

/** Faturas que saem da conta em `month` (GetPaidInvoicesUseCase.build). */
function paidInvoices(all, cards, month) {
  const creditCards = cards.filter((c) => Array.isArray(c.paymentMethods) && c.paymentMethods.includes(CREDIT));
  const knownIds = new Set(creditCards.map((c) => c.id));
  const payments = all.filter(isInvoicePayment);

  const invoices = creditCards.map((card) => {
    const target = paidInvoiceMonth(card, month);
    const items = invoiceItems(all, target).filter((t) => t.cardId === card.id);
    const payment = payments.find((p) => p.invoicePaymentMonth === target && p.cardId === card.id) || null;
    return buildInvoice({ cardId: card.id, bank: card.bank || 'Cartão', finalCode: card.finalCode || null, invoiceMonth: target, items, payment });
  });

  const fallback = addMonths(month, -1);
  const orphanItems = invoiceItems(all, fallback).filter((t) => !knownIds.has(t.cardId));
  if (orphanItems.length > 0) {
    const payment = payments.find((p) => p.invoicePaymentMonth === fallback && !knownIds.has(p.cardId)) || null;
    invoices.push(buildInvoice({ cardId: '', bank: 'Outros cartões', finalCode: null, invoiceMonth: fallback, items: orphanItems, payment }));
  }

  return invoices.filter((i) => i.itemCount > 0 || i.isPaid);
}

function buildInvoice({ cardId, bank, finalCode, invoiceMonth, items, payment }) {
  const installments = items.filter(isInstallment);
  const recurring = items.filter((t) => t.isRecurring);
  const single = items.filter((t) => !isInstallment(t) && !t.isRecurring);
  const projectedTotal = sum(items);
  const paidAmount = payment ? round2(amountOf(payment)) : null;
  return {
    cardId,
    bank,
    finalCode,
    invoiceMonth,
    itemCount: items.length,
    projectedTotal,
    isPaid: payment != null,
    paidAmount,
    paidOn: payment ? localIsoDate(payment.transactionDate) : null,
    effectiveTotal: paidAmount != null ? paidAmount : projectedTotal,
    installmentTotal: sum(installments),
    recurringTotal: sum(recurring),
    singleTotal: sum(single),
    installmentCount: installments.length,
    recurringCount: recurring.length,
    singleCount: single.length
  };
}

function localIsoDate(value) {
  const d = localDate(value);
  return d ? `${monthKey(d.getUTCFullYear(), d.getUTCMonth() + 1)}-${String(d.getUTCDate()).padStart(2, '0')}` : null;
}

// ─── Números de um mês ───────────────────────────────────────────────────────

function categoryNames(t, categoryById) {
  const ids = Array.isArray(t.category) ? t.category.filter((id) => typeof id === 'string' && id) : [];
  if (ids.length === 0) return [{ id: null, name: null }];
  return ids.map((id) => ({ id, name: categoryById.get(id) || null }));
}

/**
 * Todos os números do mês `month`. `cards`/`categories` são listas de documentos com `id`.
 */
function buildMonthFigures({ transactions, cards, categories, month }) {
  const categoryById = new Map((categories || []).map((c) => [c.id, c.title || '']));
  const all = transactions || [];

  const tx = monthTransactions(all, month);
  const incomes = tx.filter(isIncome);
  const purchases = tx.filter(isPurchase);
  const income = sum(incomes);
  const debitExpense = sum(purchases.filter((t) => !isCredit(t)));
  const creditExpense = sum(purchases.filter(isCredit));
  const invoices = paidInvoices(all, cards || [], month);
  const paidInvoiceTotal = sum(invoices, (i) => i.effectiveTotal);
  const cashExpense = round2(debitExpense + paidInvoiceTotal);
  const balance = round2(income - cashExpense);

  // Fixo × parcelas × variável sobre as compras do mês (regime de competência)
  const recurringExpense = sum(purchases.filter((t) => t.isRecurring));
  const installmentExpense = sum(purchases.filter(isInstallment));
  const variableExpense = sum(purchases.filter((t) => !t.isRecurring && !isInstallment(t)));
  const totalExpense = round2(recurringExpense + installmentExpense + variableExpense);

  const categoryTotals = new Map();
  for (const t of purchases) {
    for (const { id, name } of categoryNames(t, categoryById)) {
      const key = id || '';
      const entry = categoryTotals.get(key) || { categoryId: id, name: name || (id ? id : 'Sem categoria'), total: 0, count: 0 };
      entry.total += amountOf(t);
      entry.count += 1;
      categoryTotals.set(key, entry);
    }
  }
  const categories_ = [...categoryTotals.values()]
    .map((c) => ({ ...c, total: round2(c.total), percent: percent(c.total, totalExpense) }))
    .sort((a, b) => b.total - a.total);

  const uncategorized = purchases.filter((t) => !Array.isArray(t.category) || t.category.length === 0);

  const recurringPurchases = purchases.filter((t) => t.isRecurring);
  const items = {
    topRecurring: topItems(recurringPurchases, categoryById),
    topVariable: topItems(purchases.filter((t) => !t.isRecurring && !isInstallment(t)), categoryById),
    installmentGroups: monthInstallmentGroups(purchases.filter(isInstallment), categoryById)
  };

  return {
    month,
    transactionCount: tx.length,
    income,
    incomeCount: incomes.length,
    debitExpense,
    creditExpense,
    totalExpense,
    paidInvoiceTotal,
    cashExpense,
    balance,
    savingsRate: income > 0 ? round2((balance / income) * 100) : null,
    recurringExpense,
    recurringDebitExpense: sum(recurringPurchases.filter((t) => !isCredit(t))),
    recurringCreditExpense: sum(recurringPurchases.filter(isCredit)),
    installmentExpense,
    variableExpense,
    recurringPercentOfIncome: percent(recurringExpense, income),
    installmentPercentOfIncome: percent(installmentExpense, income),
    variablePercentOfIncome: percent(variableExpense, income),
    invoices,
    categories: categories_,
    items,
    uncategorizedCount: uncategorized.length,
    uncategorizedTotal: sum(uncategorized)
  };
}

function toItem(t, categoryById) {
  return {
    title: t.title || '',
    amount: round2(amountOf(t)),
    day: dayOf(t.transactionDate),
    paymentMethod: isCredit(t) ? CREDIT : 'DEBIT',
    categoryName: categoryNames(t, categoryById)[0].name || null
  };
}

function topItems(list, categoryById) {
  return [...list]
    .sort((a, b) => amountOf(b) - amountOf(a))
    .slice(0, TOP_ITEMS)
    .map((t) => toItem(t, categoryById));
}

/** Parcelas que caem no mês, agrupadas por compra (uma linha por grupo). */
function monthInstallmentGroups(installments, categoryById) {
  const groups = new Map();
  for (const t of installments) {
    const key = t.installmentGroupId;
    const g = groups.get(key) || {
      title: t.title || '',
      installmentNumber: t.installmentNumber || null,
      installments: t.installments || null,
      amount: 0,
      purchaseTotal: t.installments ? round2(amountOf(t) * t.installments) : null,
      paymentMethod: isCredit(t) ? CREDIT : 'DEBIT',
      categoryName: categoryNames(t, categoryById)[0].name || null
    };
    g.amount += amountOf(t);
    groups.set(key, g);
  }
  return [...groups.values()]
    .map((g) => ({ ...g, amount: round2(g.amount) }))
    .sort((a, b) => b.amount - a.amount)
    .slice(0, TOP_ITEMS);
}

function percent(part, whole) {
  return whole > 0 ? round2((part / whole) * 100) : null;
}

// ─── Parcelas futuras ────────────────────────────────────────────────────────

function futureInstallments(transactions, afterMonth, categoryById) {
  const future = (transactions || []).filter(
    (t) => isInstallment(t) && isPurchase(t) && isMonthKey(t.invoiceMonth) && t.invoiceMonth > afterMonth
  );
  const byMonth = new Map();
  for (const t of future) {
    const entry = byMonth.get(t.invoiceMonth) || { month: t.invoiceMonth, count: 0, total: 0 };
    entry.count += 1;
    entry.total += amountOf(t);
    byMonth.set(t.invoiceMonth, entry);
  }
  const months = [...byMonth.values()]
    .sort((a, b) => (a.month < b.month ? -1 : 1))
    .slice(0, FUTURE_INSTALLMENT_MONTHS)
    .map((m) => ({ ...m, total: round2(m.total) }));

  const groups = new Map();
  for (const t of future) {
    const g = groups.get(t.installmentGroupId) || {
      installmentGroupId: t.installmentGroupId,
      title: t.title || '',
      installments: t.installments || null,
      installmentAmount: round2(amountOf(t)),
      remainingCount: 0,
      remainingTotal: 0,
      lastMonth: t.invoiceMonth,
      categoryName: categoryNames(t, categoryById)[0].name
    };
    g.remainingCount += 1;
    g.remainingTotal += amountOf(t);
    if (t.invoiceMonth > g.lastMonth) g.lastMonth = t.invoiceMonth;
    groups.set(t.installmentGroupId, g);
  }
  const topGroups = [...groups.values()]
    .map((g) => ({ ...g, remainingTotal: round2(g.remainingTotal) }))
    .sort((a, b) => b.remainingTotal - a.remainingTotal)
    .slice(0, TOP_INSTALLMENT_GROUPS);

  return {
    months,
    totalCommitted: round2(future.reduce((acc, t) => acc + amountOf(t), 0)),
    groupCount: groups.size,
    topGroups
  };
}

// ─── Comparação ──────────────────────────────────────────────────────────────

const COMPARED_METRICS = [
  'income', 'debitExpense', 'creditExpense', 'totalExpense', 'paidInvoiceTotal', 'cashExpense', 'balance',
  'recurringExpense', 'installmentExpense', 'variableExpense'
];

function delta(current, reference) {
  if (reference == null) return { amount: null, percent: null };
  const amount = round2(current - reference);
  return { amount, percent: reference !== 0 ? round2((amount / Math.abs(reference)) * 100) : null };
}

function averageFigures(figuresList) {
  const withData = figuresList.filter((f) => f.transactionCount > 0);
  if (withData.length === 0) return null;
  const avg = { monthsWithData: withData.length };
  for (const metric of COMPARED_METRICS) {
    avg[metric] = round2(withData.reduce((acc, f) => acc + (f[metric] || 0), 0) / withData.length);
  }
  return avg;
}

function compareFigures(current, reference) {
  if (!reference) return null;
  const out = {};
  for (const metric of COMPARED_METRICS) out[metric] = delta(current[metric], reference[metric]);
  return out;
}

function compareCategories(current, previous) {
  const prevByKey = new Map((previous ? previous.categories : []).map((c) => [c.categoryId || '', c]));
  const curByKey = new Map(current.categories.map((c) => [c.categoryId || '', c]));
  const keys = new Set([...prevByKey.keys(), ...curByKey.keys()]);
  const rows = [...keys].map((key) => {
    const cur = curByKey.get(key);
    const prev = prevByKey.get(key);
    const total = cur ? cur.total : 0;
    const previousTotal = prev ? prev.total : null;
    return {
      categoryId: (cur || prev).categoryId,
      name: (cur || prev).name,
      total,
      count: cur ? cur.count : 0,
      percent: cur ? cur.percent : null,
      previousTotal,
      delta: previous ? delta(total, previousTotal == null ? 0 : previousTotal) : { amount: null, percent: null }
    };
  });
  rows.sort((a, b) => b.total - a.total);
  return rows.slice(0, TOP_CATEGORIES + 1);
}

// ─── Alertas ─────────────────────────────────────────────────────────────────

function buildAlerts({ current, previous, categories, future }) {
  const alerts = [];
  const push = (code, severity, message, data) => alerts.push({ code, severity, message, ...(data || {}) });

  if (current.transactionCount === 0) {
    push('EMPTY_MONTH', 'info', 'Nenhuma transação registrada neste mês.');
    return alerts;
  }
  if (current.income === 0) {
    push('NO_INCOME', 'warning', 'Nenhuma receita registrada no mês — o saldo e as porcentagens ficam incompletos.');
  }
  if (previous && previous.transactionCount > 0 && previous.income === 0) {
    push('PREVIOUS_INCOMPLETE', 'info', 'O mês anterior não tem receita registrada; as comparações podem estar distorcidas.');
  }
  if (current.balance < 0) {
    push('NEGATIVE_BALANCE', 'warning', `Saiu mais dinheiro do que entrou: saldo de ${formatBrl(current.balance)}.`, { amount: current.balance });
  }
  if (current.uncategorizedCount > 0) {
    push('UNCATEGORIZED', 'info', `${current.uncategorizedCount} gasto(s) sem categoria somam ${formatBrl(current.uncategorizedTotal)}.`, {
      count: current.uncategorizedCount, amount: current.uncategorizedTotal
    });
  }
  for (const inv of current.invoices) {
    if (inv.isPaid && Math.abs(inv.paidAmount - inv.projectedTotal) >= 1) {
      push('INVOICE_MISMATCH', 'info',
        `Fatura ${inv.bank} de ${inv.invoiceMonth}: pago ${formatBrl(inv.paidAmount)} vs. lançado ${formatBrl(inv.projectedTotal)}.`,
        { cardId: inv.cardId, invoiceMonth: inv.invoiceMonth, amount: round2(inv.paidAmount - inv.projectedTotal) });
    }
  }
  const grown = categories
    .filter((c) => c.previousTotal != null && c.previousTotal > 0 && c.delta.amount != null && c.delta.amount >= 100 && c.delta.percent >= 30)
    .sort((a, b) => b.delta.amount - a.delta.amount)[0];
  if (grown) {
    push('CATEGORY_GROWTH', 'info',
      `"${grown.name}" cresceu ${formatBrl(grown.delta.amount)} (${grown.delta.percent.toFixed(0)}%) em relação ao mês anterior.`,
      { categoryId: grown.categoryId, amount: grown.delta.amount, percent: grown.delta.percent });
  }
  if (current.income > 0 && current.recurringExpense + current.installmentExpense > current.income * 0.6) {
    push('COMMITTED_HIGH', 'warning',
      `Fixo + parcelas comprometem ${percent(current.recurringExpense + current.installmentExpense, current.income).toFixed(0)}% da renda.`);
  }
  if (future.months.length > 0 && current.income > 0 && future.months[0].total > current.income * 0.15) {
    push('INSTALLMENTS_AHEAD', 'info',
      `Já há ${formatBrl(future.months[0].total)} em parcelas para ${future.months[0].month}.`,
      { month: future.months[0].month, amount: future.months[0].total });
  }
  return alerts;
}

function formatBrl(value) {
  return (value || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

// ─── Relatório completo ──────────────────────────────────────────────────────

/**
 * Monta o relatório de `month` comparando com `month-1` e com a média de `month-1..month-3`.
 * `partial` indica mês ainda em andamento (gerado sob demanda).
 */
function buildReport({ transactions, cards, categories, month, partial = false, generatedAt = Date.now() }) {
  if (!isMonthKey(month)) throw new Error(`Mês inválido: ${month}`);
  const input = { transactions, cards, categories };
  const current = buildMonthFigures({ ...input, month });
  const previousMonths = [1, 2, 3].map((n) => buildMonthFigures({ ...input, month: addMonths(month, -n) }));
  const previous = previousMonths[0].transactionCount > 0 ? previousMonths[0] : null;
  const average3 = averageFigures(previousMonths);
  const categoryById = new Map((categories || []).map((c) => [c.id, c.title || '']));
  const future = futureInstallments(transactions, month, categoryById);
  const comparedCategories = compareCategories(current, previous);

  return {
    schemaVersion: SCHEMA_VERSION,
    month,
    partial,
    generatedAt: new Date(generatedAt).toISOString(),
    summary: stripLists(current),
    comparison: {
      previousMonth: previous ? { month: previous.month, figures: stripLists(previous, true), delta: compareFigures(current, previous) } : null,
      average3Months: average3 ? { monthsWithData: average3.monthsWithData, figures: average3, delta: compareFigures(current, average3) } : null
    },
    invoices: current.invoices,
    categories: comparedCategories,
    futureInstallments: future,
    alerts: buildAlerts({ current, previous, categories: comparedCategories, future })
  };
}

/** Remove as listas pesadas; `items` (top lançamentos) só é mantido no `summary` do mês corrente. */
function stripLists(figures, dropItems = false) {
  const { invoices, categories, items, ...rest } = figures;
  return dropItems ? rest : { ...rest, items };
}

module.exports = {
  SCHEMA_VERSION,
  buildReport,
  buildMonthFigures,
  addMonths,
  isMonthKey,
  currentMonth,
  monthOf,
  dayOf,
  _internal: { monthTransactions, invoiceItems, paidInvoices, occursIn, materialize, paidInvoiceMonth, futureInstallments, delta }
};
