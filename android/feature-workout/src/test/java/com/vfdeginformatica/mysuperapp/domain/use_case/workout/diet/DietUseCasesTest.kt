package com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.repository.FakeDietRepository
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietUseCasesTest {
    @Test
    fun `missing document is success but read and auth failures remain errors`() = runTest {
        val repo = FakeDietRepository()
        val get = GetDietUseCase(repo)
        val absent = get().take(2).toList().last()
        assertTrue(absent is Resource.Success)
        assertNull(absent.data?.diet)
        repo.readError = "Sem conexão"
        assertEquals("Sem conexão", get().take(2).toList().last().message)
        repo.readError = null
        repo.owner.value = null
        assertTrue(get().take(2).toList().last() is Resource.Error)
    }

    @Test
    fun `save accepts empty diet and empty meals and overwrites only the same owner document`() = runTest {
        val repo = FakeDietRepository()
        val save = SaveDietUseCase(repo)
        assertTrue(save("owner", Diet()).last() is Resource.Success)
        assertTrue(save("owner", Diet(listOf(DietMeal("m", "Refeição")))).last() is Resource.Success)
        assertEquals(1, repo.diets.size)
        repo.owner.value = "other"
        assertTrue(save("owner", Diet()).last() is Resource.Error)
        assertEquals(1, repo.diets["owner"]?.meals?.size)
        assertNull(repo.diets["other"])
    }

    @Test
    fun `required fields and times are validated while free text is trimmed not parsed`() = runTest {
        val repo = FakeDietRepository()
        val save = SaveDietUseCase(repo)
        val valid = DietMeal("meal", " Café ", " 07:30 ", foods = listOf(DietFood("f", " Banana ", " entre 120 e 160 g ")))
        val invalid = listOf(
            valid.copy(name = " "),
            valid.copy(time = "24:00"),
            valid.copy(time = "12:60"),
            valid.copy(time = "7:30"),
            valid.copy(foods = listOf(DietFood("f", "", "2"))),
            valid.copy(foods = listOf(DietFood("f", "Ovos", " "))),
        )
        invalid.forEach { assertTrue(save("owner", Diet(listOf(it))).last() is Resource.Error) }
        assertEquals(0, repo.writes)
        assertTrue(save("owner", Diet(listOf(valid))).last() is Resource.Success)
        val meal = repo.diets.getValue("owner").meals.single()
        assertEquals("07:30", meal.time)
        assertEquals("Café", meal.name)
        assertEquals("entre 120 e 160 g", meal.foods.single().quantity)
    }
}
