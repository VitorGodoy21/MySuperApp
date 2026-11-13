package com.vfdeginformatica.mysuperapp.presentation.common.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parsesHeadingsParagraphsAndLists() {
        val blocks = MarkdownParser.parse(
            """
            # Relatório de novembro
            Linha um
            continua aqui.

            ## 1. Resposta curta
            - item a
            - item b

            1. primeiro
            2. segundo
            """.trimIndent()
        )

        assertEquals(MarkdownBlock.Heading(1, "Relatório de novembro"), blocks[0])
        assertEquals(MarkdownBlock.Paragraph("Linha um continua aqui."), blocks[1])
        assertEquals(MarkdownBlock.Heading(2, "1. Resposta curta"), blocks[2])
        assertEquals(MarkdownBlock.ListBlock(listOf("item a", "item b"), ordered = false), blocks[3])
        assertEquals(MarkdownBlock.ListBlock(listOf("primeiro", "segundo"), ordered = true), blocks[4])
        assertEquals(5, blocks.size)
    }

    @Test
    fun parsesTablesWithAlignmentSeparators() {
        val blocks = MarkdownParser.parse(
            """
            | Item | Valor |
            | :--- | ---: |
            | Receita | R$ 1.000,00 |
            | Saldo | **R$ 200,00** |
            """.trimIndent()
        )

        val table = blocks.single() as MarkdownBlock.Table
        assertEquals(listOf("Item", "Valor"), table.header)
        assertEquals(listOf(listOf("Receita", "R$ 1.000,00"), listOf("Saldo", "**R$ 200,00**")), table.rows)
    }

    @Test
    fun parsesCodeBlocksAndQuotesVerbatim() {
        val blocks = MarkdownParser.parse(
            """
            ```
            receitas   10.000
            - débito    2.000
            ```
            > Mês parcial.
            """.trimIndent()
        )

        assertEquals(MarkdownBlock.CodeBlock("receitas   10.000\n- débito    2.000"), blocks[0])
        assertEquals(MarkdownBlock.Quote("Mês parcial."), blocks[1])
    }

    @Test
    fun parseInlineSplitsBoldAndCode() {
        val spans = MarkdownParser.parseInline("Sobrou **R$ 200** em `novembro`.")

        assertEquals(
            listOf(
                InlineSpan("Sobrou "),
                InlineSpan("R$ 200", bold = true),
                InlineSpan(" em "),
                InlineSpan("novembro", code = true),
                InlineSpan("."),
            ),
            spans,
        )
        assertTrue(spans.none { it.text.contains("**") || it.text.contains("`") })
    }
}
