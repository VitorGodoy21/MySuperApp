package com.vfdeginformatica.mysuperapp.domain.use_case.validator

import android.util.Patterns
import javax.inject.Inject

class ValidateEmail @Inject constructor() {
    operator fun invoke(email: String): Boolean {
        if (email.isNotBlank()) {
            return Patterns.EMAIL_ADDRESS.matcher(email)
                .matches()
        }

        return false
    }
}