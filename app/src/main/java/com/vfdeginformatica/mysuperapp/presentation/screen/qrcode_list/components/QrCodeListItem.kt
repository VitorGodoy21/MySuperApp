package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

@Composable
fun QrCodeListItem(
    qrCode: QrCodeDto,
    modifier: Modifier = Modifier,
    onItemClick: (QrCodeDto) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onItemClick(qrCode) }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // QR Code Icon/Image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Code",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Title and Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            // Title
            Text(
                text = "QR Code",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // URL/Content with truncation
            Text(
                text = qrCode.redirectUrl.ifEmpty { "Sem URL configurada" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            // ID
            Text(
                text = "ID: ${qrCode.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun QrCodeListItemPreview() {
    MaterialTheme {
        QrCodeListItem(
            qrCode = QrCodeDto(
                id = "qr_001",
                redirectUrl = "https://www.example.com/qrcode/details?id=12345&type=campaign"
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Preview
@Composable
fun QrCodeListItemPreviewEmpty() {
    MaterialTheme {
        QrCodeListItem(
            qrCode = QrCodeDto(
                id = "qr_002",
                redirectUrl = ""
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
