'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { buildReport, buildMonthFigures, addMonths, _internal } = require('../financialReport');

// 12:00 local (UTC-3) do dia informado
const at = (y, m, d) => Date.UTC(y, m - 1, d, 15, 0, 0);

const CARD = { id: 'itau', bank: 'Itau', finalCode: '8934', paymentMethods: ['CREDIT', 'DEBIT'], closingDay: 6, invoiceDueDate: 12 };
const CARD_NEXT_MONTH = { id: 'nu', bank: 'Nubank', paymentMethods: ['CREDIT'], closingDay: 25, invoiceDueDate: 2 };
const CATS = [{ id: 'c1', title: 'Casa' }, { id: 'c2', title: 'Rolê' }];

const income = (amount, date, extra = {}) => ({ transactionType: 'INCOME', amount, transactionDate: date, cardId: 'itau', ...extra });
const debit = (amount, date, extra = {}) => ({ transactionType: 'EXPENSE', paymentMethod: 'DEBIT', amount, transactionDate: date, cardId: 'itau', ...extra });
const credit = (amount, date, invoiceMonth, extra = {}) => ({ transactionType: 'EXPENSE', paymentMethod: 'CREDIT', amount, transactionDate: date, cardId: 'itau', invoiceMonth, ...extra });

test('addMonths crosses year boundaries', () => {
  assert.equal(addMonths('2026-11', 1), '2026-12');
  assert.equal(addMonths('2026-11', 2), '2027-01');
  assert.equal(addMonths('2026-01', -1), '2025-12');
});

test('empty month yields zeros and EMPTY_MONTH alert', () => {
  const report = buildReport({ transactions: [], cards: [], categories: [], month: '2026-10' });
  assert.equal(report.summary.income, 0);
  assert.equal(report.summary.balance, 0);
  assert.equal(report.comparison.previousMonth, null);
  assert.deepEqual(report.alerts.map((a) => a.code), ['EMPTY_MONTH']);
});

test('installments count only in their invoiceMonth, not in purchase month', () => {
  const tx = [
    credit(100, at(2026, 10, 20), '2026-11', { installmentGroupId: 'g', installmentNumber: 1, installments: 2 }),
    credit(100, at(2026, 10, 20), '2026-12', { installmentGroupId: 'g', installmentNumber: 2, installments: 2 })
  ];
  const oct = buildMonthFigures({ transactions: tx, cards: [CARD], categories: [], month: '2026-10' });
  const nov = buildMonthFigures({ transactions: tx, cards: [CARD], categories: [], month: '2026-11' });
  assert.equal(oct.creditExpense, 0);
  assert.equal(nov.creditExpense, 100);
  assert.equal(nov.installmentExpense, 100);
});

test('recurring template materializes each month with override and exclusion', () => {
  const tx = [debit(500, at(2026, 9, 5), { isRecurring: true, recurrenceStart: '2026-09', excludedMonths: ['2026-10'], overrides: { '2026-11': { amount: 650, title: 'Ajustado' } } })];
  const f = (m) => buildMonthFigures({ transactions: tx, cards: [CARD], categories: [], month: m });
  assert.equal(f('2026-09').recurringExpense, 500);
  assert.equal(f('2026-10').recurringExpense, 0);
  assert.equal(f('2026-11').recurringExpense, 650);
  assert.equal(f('2026-08').recurringExpense, 0);
});

test('card due after closing pays invoice M in M; otherwise M-1', () => {
  assert.equal(_internal.paidInvoiceMonth(CARD, '2026-11'), '2026-11');
  assert.equal(_internal.paidInvoiceMonth(CARD_NEXT_MONTH, '2026-11'), '2026-10');
  assert.equal(_internal.paidInvoiceMonth({ closingDay: 0, invoiceDueDate: 0 }, '2026-11'), '2026-10');
});

test('paid invoice amount replaces projected total in cash expense', () => {
  const tx = [
    income(10000, at(2026, 11, 5)),
    credit(1000, at(2026, 10, 20), '2026-11'),
    debit(200, at(2026, 11, 8)),
    { transactionType: 'EXPENSE', paymentMethod: 'INVOICE_PAYMENT', amount: 950, transactionDate: at(2026, 11, 11), cardId: 'itau', invoicePaymentMonth: '2026-11' }
  ];
  const nov = buildMonthFigures({ transactions: tx, cards: [CARD], categories: [], month: '2026-11' });
  assert.equal(nov.invoices.length, 1);
  assert.equal(nov.invoices[0].projectedTotal, 1000);
  assert.equal(nov.invoices[0].paidAmount, 950);
  assert.equal(nov.paidInvoiceTotal, 950);
  assert.equal(nov.cashExpense, 1150);
  assert.equal(nov.balance, 8850);
  assert.equal(nov.totalExpense, 200); // o pagamento de fatura não é compra
});

test('recurring credit joins the invoice without duplicating its own invoiceMonth doc', () => {
  const tx = [credit(50, at(2026, 10, 10), '2026-11', { isRecurring: true, recurrenceStart: '2026-10' })];
  const items11 = _internal.invoiceItems(tx, '2026-11');
  const items12 = _internal.invoiceItems(tx, '2026-12');
  assert.equal(items11.length, 1);
  assert.equal(items12.length, 1);
  assert.equal(items12[0].isRecurring, true);
});

test('comparison against previous month and 3-month average with deltas', () => {
  const tx = [
    income(1000, at(2026, 8, 1)), debit(400, at(2026, 8, 2)),
    income(1000, at(2026, 9, 1)), debit(600, at(2026, 9, 2)),
    income(1000, at(2026, 10, 1)), debit(800, at(2026, 10, 2)),
    income(1200, at(2026, 11, 1)), debit(900, at(2026, 11, 2), { category: ['c2'] })
  ];
  const report = buildReport({ transactions: tx, cards: [CARD], categories: CATS, month: '2026-11' });
  assert.equal(report.comparison.previousMonth.month, '2026-10');
  assert.deepEqual(report.comparison.previousMonth.delta.income, { amount: 200, percent: 20 });
  assert.deepEqual(report.comparison.previousMonth.delta.debitExpense, { amount: 100, percent: 12.5 });
  assert.equal(report.comparison.average3Months.monthsWithData, 3);
  assert.equal(report.comparison.average3Months.figures.debitExpense, 600);
  assert.deepEqual(report.comparison.average3Months.delta.debitExpense, { amount: 300, percent: 50 });
  const role = report.categories.find((c) => c.name === 'Rolê');
  assert.equal(role.total, 900);
  assert.equal(role.previousTotal, null);
  assert.equal(role.delta.amount, 900);
});

test('future installments are summarized by month and group', () => {
  const tx = [
    credit(100, at(2026, 11, 29), '2026-12', { installmentGroupId: 'nb', installments: 3, title: 'Notebook' }),
    credit(100, at(2026, 11, 29), '2027-01', { installmentGroupId: 'nb', installments: 3, title: 'Notebook' }),
    credit(100, at(2026, 11, 29), '2027-02', { installmentGroupId: 'nb', installments: 3, title: 'Notebook' }),
    credit(30, at(2026, 11, 1), '2026-11', { installmentGroupId: 'old', installments: 1 })
  ];
  const report = buildReport({ transactions: tx, cards: [CARD], categories: [], month: '2026-11' });
  assert.equal(report.futureInstallments.totalCommitted, 300);
  assert.deepEqual(report.futureInstallments.months.map((m) => m.month), ['2026-12', '2027-01', '2027-02']);
  assert.equal(report.futureInstallments.topGroups[0].remainingCount, 3);
  assert.equal(report.futureInstallments.topGroups[0].lastMonth, '2027-02');
});

test('alerts: uncategorized, invoice mismatch, committed high', () => {
  const tx = [
    income(1000, at(2026, 11, 1)),
    debit(700, at(2026, 11, 2), { isRecurring: true, recurrenceStart: '2026-11' }),
    debit(50, at(2026, 11, 3)),
    credit(300, at(2026, 10, 20), '2026-11', { category: ['c1'] }),
    { transactionType: 'EXPENSE', paymentMethod: 'INVOICE_PAYMENT', amount: 280, transactionDate: at(2026, 11, 11), cardId: 'itau', invoicePaymentMonth: '2026-11' }
  ];
  const report = buildReport({ transactions: tx, cards: [CARD], categories: CATS, month: '2026-11' });
  const codes = report.alerts.map((a) => a.code);
  assert.ok(codes.includes('UNCATEGORIZED'));
  assert.ok(codes.includes('INVOICE_MISMATCH'));
  assert.ok(codes.includes('COMMITTED_HIGH'));
  assert.ok(!codes.includes('NO_INCOME'));
  assert.equal(report.summary.savingsRate, -3); // 1000 − (750 + 280) = −30
  assert.equal(report.partial, false);
});

test('report is JSON-serializable (no Dates/undefined)', () => {
  const report = buildReport({ transactions: [income(1, at(2026, 11, 1))], cards: [CARD], categories: [], month: '2026-11', partial: true });
  const json = JSON.parse(JSON.stringify(report));
  assert.equal(json.partial, true);
  assert.equal(typeof json.generatedAt, 'string');
});

test('summary exposes top items, installment groups and recurring split by method', () => {
  const tx = [
    income(10000, at(2026, 11, 5)),
    debit(900, at(2026, 11, 3), { title: 'Aluguel', isRecurring: true, recurrenceStart: '2026-10', category: ['c1'] }),
    credit(40, at(2026, 10, 10), '2026-11', { title: 'Streaming', isRecurring: true, recurrenceStart: '2026-10' }),
    credit(120, at(2026, 11, 7), '2026-11', { title: 'Mercado', category: ['c1'] }),
    debit(300, at(2026, 11, 9), { title: 'Bar', category: ['c2'] }),
    credit(250, at(2026, 10, 20), '2026-11', { title: 'Sofá', installmentGroupId: 'g', installmentNumber: 2, installments: 4 }),
    credit(250, at(2026, 10, 20), '2026-12', { title: 'Sofá', installmentGroupId: 'g', installmentNumber: 3, installments: 4 })
  ];
  const report = buildReport({ transactions: tx, cards: [CARD], categories: CATS, month: '2026-11', generatedAt: at(2026, 12, 1) });
  const s = report.summary;
  assert.equal(report.schemaVersion, 2);
  assert.equal(s.recurringDebitExpense, 900);
  assert.equal(s.recurringCreditExpense, 40);
  assert.deepEqual(s.items.topRecurring.map((i) => [i.title, i.amount, i.paymentMethod, i.categoryName]), [['Aluguel', 900, 'DEBIT', 'Casa'], ['Streaming', 40, 'CREDIT', null]]);
  assert.deepEqual(s.items.topVariable.map((i) => [i.title, i.amount, i.day, i.paymentMethod]), [['Bar', 300, 9, 'DEBIT'], ['Mercado', 120, 7, 'CREDIT']]);
  assert.deepEqual(s.items.installmentGroups, [{ title: 'Sofá', installmentNumber: 2, installments: 4, amount: 250, purchaseTotal: 1000, paymentMethod: 'CREDIT', categoryName: null }]);
  assert.equal(report.comparison.previousMonth.figures.items, undefined);
});
