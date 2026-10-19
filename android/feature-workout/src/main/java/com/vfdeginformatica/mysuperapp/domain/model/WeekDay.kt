package com.vfdeginformatica.mysuperapp.domain.model

enum class WeekDay(val label: String, val shortLabel: String) {
    MONDAY("Segunda-feira", "Seg"),
    TUESDAY("Terça-feira", "Ter"),
    WEDNESDAY("Quarta-feira", "Qua"),
    THURSDAY("Quinta-feira", "Qui"),
    FRIDAY("Sexta-feira", "Sex"),
    SATURDAY("Sábado", "Sáb"),
    SUNDAY("Domingo", "Dom");

    companion object {
        fun fromName(name: String?): WeekDay? =
            entries.firstOrNull { it.name == name }
    }
}
