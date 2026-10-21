package com.vfdeginformatica.mysuperapp.notification

import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFilterTest {

    @Test
    fun allDisabledHidesEverything() {
        val settings = NotificationSettings(enabledAll = false, enabledAccess = true, enabledMuralComments = true)
        assertFalse(NotificationFilter.shouldShow(settings, NotificationFilter.TYPE_ACCESS))
        assertFalse(NotificationFilter.shouldShow(settings, NotificationFilter.TYPE_MURAL_COMMENT))
        assertFalse(NotificationFilter.shouldShow(settings, "other"))
    }

    @Test
    fun accessTypeFollowsAccessToggle() {
        assertTrue(NotificationFilter.shouldShow(NotificationSettings(enabledAccess = true), NotificationFilter.TYPE_ACCESS))
        assertFalse(NotificationFilter.shouldShow(NotificationSettings(enabledAccess = false), NotificationFilter.TYPE_ACCESS))
    }

    @Test
    fun muralCommentTypeFollowsMuralToggle() {
        assertTrue(NotificationFilter.shouldShow(NotificationSettings(enabledMuralComments = true), NotificationFilter.TYPE_MURAL_COMMENT))
        assertFalse(NotificationFilter.shouldShow(NotificationSettings(enabledMuralComments = false), NotificationFilter.TYPE_MURAL_COMMENT))
    }

    @Test
    fun unknownTypeIsShownWhenAllEnabled() {
        val settings = NotificationSettings(enabledAccess = false, enabledMuralComments = false)
        assertTrue(NotificationFilter.shouldShow(settings, "unknown"))
    }
}
