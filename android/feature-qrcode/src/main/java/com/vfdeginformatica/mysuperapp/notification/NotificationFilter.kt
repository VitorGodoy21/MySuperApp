package com.vfdeginformatica.mysuperapp.notification

import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings

object NotificationFilter {
    const val TYPE_ACCESS = "access"
    const val TYPE_MURAL_COMMENT = "mural_comment"

    fun shouldShow(settings: NotificationSettings, type: String): Boolean {
        if (!settings.enabledAll) return false

        return when (type) {
            TYPE_ACCESS -> settings.enabledAccess
            TYPE_MURAL_COMMENT -> settings.enabledMuralComments
            else -> true
        }
    }
}
