package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract

import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.model.UiMenuItem

interface AppDrawerMenuEvent {
    data object Open : AppDrawerMenuEvent
    data object Close : AppDrawerMenuEvent
    data class ClickItem(val item: UiMenuItem) : AppDrawerMenuEvent
    data object OnResume : AppDrawerMenuEvent
}