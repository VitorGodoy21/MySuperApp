package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.DietDto
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import org.junit.Assert.assertEquals
import org.junit.Test

class DietMapperTest {
    @Test
    fun `round trip preserves manual order ids optional fields and free quantities`() {
        val diet = Diet(
            meals = listOf(
                DietMeal("later", "Jantar", "20:00", "Sem pressa", listOf(
                    DietFood("banana", "Banana", "entre 120 e 160 g"),
                    DietFood("cheese", "Mussarela", "2 fatias"),
                    DietFood("papaya", "Mamão", "meia unidade"),
                )),
                DietMeal("earlier", "Refeição livre"),
            ),
            notes = "Todos os dias",
        )
        assertEquals(diet, diet.toDto().toModel())
        assertEquals(Diet(), DietDto().toModel())
    }
}
