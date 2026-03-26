package com.vfdeginformatica.mysuperapp.domain.use_case.validator

import javax.inject.Inject

class ValidatePassword @Inject constructor() {
    operator fun invoke(password: String): Boolean {
        if (password.isNotBlank()) {
            return password.length >= 8
        }

        return false
    }
}