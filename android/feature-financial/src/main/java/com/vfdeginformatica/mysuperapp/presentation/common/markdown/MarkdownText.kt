package com.vfdeginformatica.mysuperapp.presentation.common.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renderiza o subconjunto de Markdown entendido por [MarkdownParser]. Pensado para o texto do
 * relatório financeiro; sem dependência externa.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block -> MarkdownBlockView(block) }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    when (block) {
        is MarkdownBlock.Heading -> Text(
            text = inline(block.text),
            style = when (block.level) {
                1 -> typography.headlineSmall
                2 -> typography.titleLarge
                3 -> typography.titleMedium
                else -> typography.titleSmall
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = if (block.level <= 2) 8.dp else 4.dp),
        )

        is MarkdownBlock.Paragraph -> Text(inline(block.text), style = typography.bodyMedium)

        is MarkdownBlock.Quote -> Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(colors.primary, RoundedCornerShape(2.dp)))
            Text(
                inline(block.text),
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        is MarkdownBlock.CodeBlock -> Text(
            text = block.code,
            style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )

        is MarkdownBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEachIndexed { index, item ->
                Row {
                    Text(
                        text = if (block.ordered) "${index + 1}." else "•",
                        style = typography.bodyMedium,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(inline(item), style = typography.bodyMedium)
                }
            }
        }

        is MarkdownBlock.Table -> MarkdownTable(block, typography.bodySmall)
    }
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table, style: TextStyle) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
    ) {
        TableRow(table.header, style.copy(fontWeight = FontWeight.SemiBold), columnCount = table.header.size)
        HorizontalDivider(color = colors.outlineVariant)
        table.rows.forEach { row -> TableRow(row, style, columnCount = table.header.size) }
    }
}

@Composable
private fun TableRow(cells: List<String>, style: TextStyle, columnCount: Int) {
    val padded = List(columnCount) { cells.getOrElse(it) { "" } }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        padded.forEachIndexed { index, cell ->
            // Primeira coluna (rótulo) ganha mais espaço quando há várias colunas.
            val weight = if (index == 0 && columnCount > 2) 1.6f else 1f
            Text(inline(cell), style = style, modifier = Modifier.weight(weight).padding(end = 6.dp))
        }
    }
}

@Composable
private fun inline(text: String): AnnotatedString {
    val spans = remember(text) { MarkdownParser.parseInline(text) }
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    return buildAnnotatedString {
        spans.forEach { span ->
            when {
                span.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                span.code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(span.text) }
                else -> append(span.text)
            }
        }
    }
}
