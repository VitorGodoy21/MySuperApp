package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.home.SmartTagsMenu
import com.vfdeginformatica.mysuperapp.presentation.screen.home.SmartTagsOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTagsPickerBottomSheet(
    options: List<SmartTagsOption> = SmartTagsMenu.options,
    onOptionSelected: (route: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = SmartTagsMenu.TITLE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            options.forEach { option ->
                ListItem(
                    headlineContent = { Text(option.title) },
                    leadingContent = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.title
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option.route) }
                )
            }
        }
    }
}
