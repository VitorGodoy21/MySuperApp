package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

/**
 * Máscara de moeda pt-BR alimentada por centavos: o texto original contém apenas
 * dígitos (ex.: "12480") e é exibido como "R$ 124,80", preenchendo da direita
 * para a esquerda. O cursor fica sempre no fim.
 */
class CurrencyVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatCents(text.text)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formatted.length
            override fun transformedToOriginal(offset: Int): Int = text.length
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    companion object {
        private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

        /** Converte uma string de dígitos (centavos) para "R$ 0,00". */
        fun formatCents(digits: String): String {
            val cents = digits.filter { it.isDigit() }.toLongOrNull() ?: 0L
            return NumberFormat.getCurrencyInstance(ptBr).format(cents / 100.0)
        }

        /** Valor em reais (Double, compatível com Firestore) a partir dos centavos. */
        fun centsToDouble(digits: String): Double =
            (digits.filter { it.isDigit() }.toLongOrNull() ?: 0L) / 100.0
    }
}
