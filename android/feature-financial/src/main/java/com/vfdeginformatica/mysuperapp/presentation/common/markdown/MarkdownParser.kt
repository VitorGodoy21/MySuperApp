package com.vfdeginformatica.mysuperapp.presentation.common.markdown

/**
 * Parser de um subconjunto de Markdown — o que o template `financialNarrative.js` e o prompt da
 * IA produzem: títulos `#`, listas `-`/`*`/`1.`, tabelas `|`, blocos de código ``` , citações `>`
 * e parágrafos. Negrito `**` e código inline `` ` `` são tratados em [MarkdownParser.parseInline].
 * Não cobre links, imagens nem HTML.
 */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class ListBlock(val items: List<String>, val ordered: Boolean) : MarkdownBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

/** Trecho de texto inline já separado em estilos. */
data class InlineSpan(val text: String, val bold: Boolean = false, val code: Boolean = false)

object MarkdownParser {

    private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
    private val bulletRegex = Regex("""^\s*[-*]\s+(.*)$""")
    private val orderedRegex = Regex("""^\s*\d+[.)]\s+(.*)$""")
    private val tableSeparatorRegex = Regex("""^\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?$""")

    fun parse(markdown: String): List<MarkdownBlock> {
        val lines = markdown.replace("\r\n", "\n").split('\n')
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(paragraph.toString().trim())
                paragraph.clear()
            }
        }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                trimmed.isEmpty() -> { flushParagraph(); i++ }

                trimmed.startsWith("```") -> {
                    flushParagraph()
                    val code = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) { code += lines[i]; i++ }
                    i++ // fecha ```
                    blocks += MarkdownBlock.CodeBlock(code.joinToString("\n"))
                }

                headingRegex.matches(trimmed) -> {
                    flushParagraph()
                    val m = headingRegex.find(trimmed)!!
                    blocks += MarkdownBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim())
                    i++
                }

                trimmed.startsWith(">") -> {
                    flushParagraph()
                    val quote = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith(">")) {
                        quote += lines[i].trim().removePrefix(">").trim(); i++
                    }
                    blocks += MarkdownBlock.Quote(quote.joinToString(" "))
                }

                trimmed.startsWith("|") && i + 1 < lines.size && tableSeparatorRegex.matches(lines[i + 1].trim()) -> {
                    flushParagraph()
                    val header = splitTableRow(trimmed)
                    i += 2
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) { rows += splitTableRow(lines[i].trim()); i++ }
                    blocks += MarkdownBlock.Table(header, rows)
                }

                bulletRegex.matches(line) || orderedRegex.matches(line) -> {
                    flushParagraph()
                    val ordered = orderedRegex.matches(line)
                    val regex = if (ordered) orderedRegex else bulletRegex
                    val items = mutableListOf<String>()
                    while (i < lines.size && regex.matches(lines[i])) {
                        items += regex.find(lines[i])!!.groupValues[1].trim(); i++
                        // continuação indentada do item
                        while (i < lines.size && lines[i].startsWith("  ") && lines[i].isNotBlank() &&
                            !bulletRegex.matches(lines[i]) && !orderedRegex.matches(lines[i])
                        ) {
                            items[items.lastIndex] = items.last() + " " + lines[i].trim(); i++
                        }
                    }
                    blocks += MarkdownBlock.ListBlock(items, ordered)
                }

                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                    i++
                }
            }
        }
        flushParagraph()
        return blocks
    }

    private fun splitTableRow(row: String): List<String> =
        row.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

    /** Separa `**negrito**` e `` `código` `` em spans. Marcadores não fechados ficam literais. */
    fun parseInline(text: String): List<InlineSpan> {
        val spans = mutableListOf<InlineSpan>()
        val buffer = StringBuilder()
        var i = 0
        fun flush() { if (buffer.isNotEmpty()) { spans += InlineSpan(buffer.toString()); buffer.clear() } }
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        flush(); spans += InlineSpan(text.substring(i + 2, end), bold = true); i = end + 2
                    } else { buffer.append("**"); i += 2 }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        flush(); spans += InlineSpan(text.substring(i + 1, end), code = true); i = end + 1
                    } else { buffer.append('`'); i++ }
                }
                else -> { buffer.append(text[i]); i++ }
            }
        }
        flush()
        return spans
    }
}
