'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { buildReport } = require('../financialReport');
const { buildNarrative, _internal } = require('../financialNarrative');

const at = (y, m, d) => Date.UTC(y, m - 1, d, 15, 0, 0);
const CARD = { id: 'itau', bank: 'Itau', paymentMethods: ['CREDIT', 'DEBIT'], closingDay: 6, invoiceDueDate: 12 };
const CATS = [{ id: 'c1', title: 'Casa' }, { id: 'c2', title: 'Rolê' }];
const income = (amount, date) => ({ transactionType: 'INCOME', amount, transactionDate: date, cardId: 'itau' });
const debit = (amount, date, extra = {}) => ({ transactionType: 'EXPENSE', paymentMethod: 'DEBIT', amount, transactionDate: date, cardId: 'itau', ...extra });
const credit = (amount, date, invoiceMonth, extra = {}) => ({ transactionType: 'EXPENSE', paymentMethod: 'CREDIT', amount, transactionDate: date, cardId: 'itau', invoiceMonth, ...extra });

function sampleReport() {
  const tx = [
    income(10000, at(2026, 10, 5)), debit(3000, at(2026, 10, 10), { title: 'Antigo' }),
    income(10000, at(2026, 11, 5)),
    debit(900, at(2026, 11, 3), { title: 'Aluguel', isRecurring: true, recurrenceStart: '2026-10', category: ['c1'] }),
    credit(120, at(2026, 11, 7), '2026-11', { title: 'Mercado', category: ['c1'] }),
    debit(300, at(2026, 11, 9), { title: 'Bar', category: ['c2'] }),
    credit(250, at(2026, 10, 20), '2026-11', { title: 'Sofá', installmentGroupId: 'g', installmentNumber: 2, installments: 4 }),
    credit(250, at(2026, 10, 20), '2026-12', { title: 'Sofá', installmentGroupId: 'g', installmentNumber: 3, installments: 4 }),
    { transactionType: 'EXPENSE', paymentMethod: 'INVOICE_PAYMENT', amount: 400, transactionDate: at(2026, 11, 11), cardId: 'itau', invoicePaymentMonth: '2026-11' }
  ];
  return buildReport({ transactions: tx, cards: [CARD], categories: CATS, month: '2026-11', generatedAt: at(2026, 12, 1) });
}

test('narrative has every section and the key figures', () => {
  const md = buildNarrative(sampleReport());
  for (const h of ['# Relatório financeiro — novembro/2026', '## 1. Resposta curta', '## 2. A conta do mês', '## 3. Para onde vai', '### 3.1', '### 3.2', '### 3.3', '## 4. Comparação', '## 5. Alertas', '## 6. Ações sugeridas', '## 7. Limitações']) {
    assert.ok(md.includes(h), `missing ${h}`);
  }
  assert.ok(md.includes('Aluguel · Casa'));
  assert.ok(md.includes('| Sofá | 2/4 |'));
  assert.ok(md.includes('Fatura Itau nov/26 (paga em 11/11)'));
  assert.ok(md.includes('1 lançamento(s) sem categoria'));
  assert.ok(md.includes('Categorizar os 1 lançamentos'));
  assert.ok(md.includes('vs out/26'));
  assert.ok(!md.includes('undefined') && !md.includes('NaN'));
});

test('empty month produces a short narrative', () => {
  const md = buildNarrative(buildReport({ transactions: [], cards: [], categories: [], month: '2026-10' }));
  assert.ok(md.startsWith('# Relatório financeiro — outubro/2026'));
  assert.ok(md.includes('Nenhuma transação registrada'));
  assert.ok(!md.includes('## 2.'));
});

test('partial month is flagged in title and limitations', () => {
  const md = buildNarrative(buildReport({ transactions: [income(100, at(2026, 11, 2))], cards: [], categories: [], month: '2026-11', partial: true, generatedAt: at(2026, 11, 15) }));
  assert.ok(md.includes('(parcial)'));
  assert.ok(md.includes('Relatório **parcial**'));
});

test('deltaText formats direction, amount and percent', () => {
  const plain = (s) => s.replace(/\u00a0/g, ' ');
  assert.equal(plain(_internal.deltaText({ amount: 150.5, percent: 12.3 })), '▲ R$ 150,50 (+12%)');
  assert.equal(plain(_internal.deltaText({ amount: -20, percent: -5 })), '▼ R$ 20,00 (-5%)');
  assert.equal(_internal.deltaText({ amount: 0, percent: 0 }), 'igual');
  assert.equal(_internal.deltaText({ amount: null, percent: null }), 'sem referência');
});
