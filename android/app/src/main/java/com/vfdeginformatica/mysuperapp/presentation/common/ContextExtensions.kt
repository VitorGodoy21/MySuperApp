package com.vfdeginformatica.mysuperapp.presentation.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Extensão para obter a FragmentActivity a partir de um Context
 * Percorre a hierarquia de contextos até encontrar uma Activity
 */
fun Context.getFragmentActivity(): FragmentActivity? {
    var context = this

    // Primeiro, tenta fazer cast direto
    if (this is FragmentActivity) {
        return this
    }

    // Depois, percorre a hierarquia de ContextWrapper
    while (context is ContextWrapper) {
        val baseContext = context.baseContext

        if (baseContext is FragmentActivity) {
            return baseContext
        }

        context = baseContext
    }

    return null
}

/**
 * Extensão para obter qualquer Activity a partir de um Context
 */
fun Context.getActivity(): Activity? {
    var context = this

    if (this is Activity) {
        return this
    }

    while (context is ContextWrapper) {
        val baseContext = context.baseContext

        if (baseContext is Activity) {
            return baseContext
        }

        context = baseContext
    }

    return null
}

