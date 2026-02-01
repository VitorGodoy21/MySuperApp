package com.vfdeginformatica.mysuperapp.domain.model

data class UserSession(
    val id: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val lastSignInAt: Long = 0L,
    val name: String = "",
)
