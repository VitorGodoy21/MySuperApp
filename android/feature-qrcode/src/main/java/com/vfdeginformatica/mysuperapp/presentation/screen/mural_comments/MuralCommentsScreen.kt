package com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MuralCommentsScreen(
    qrCodeId: String,
    uiState: MuralCommentsUiState,
    onEvent: (MuralCommentsEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    var commentToDelete by remember { mutableStateOf<MuralComment?>(null) }
    val listState = rememberLazyListState()
    // Scroll to top when a new comment arrives
    LaunchedEffect(uiState.comments.size) {
        if (uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    // Delete confirmation dialog
    commentToDelete?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Excluir comentário") },
            text = {
                Text("Deseja excluir o comentário de \"${comment.author.ifEmpty { "Anônimo" }}\"? Esta ação não pode ser desfeita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(MuralCommentsEvent.OnDeleteComment(qrCodeId, comment.id))
                        commentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding()
    ) {
        // Comments area
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage.isNotEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                uiState.comments.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum comentário ainda",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Seja o primeiro a comentar!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }

                else -> {
                    val sections = splitCommentsByTuesdayCutoff(uiState.comments)

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "${uiState.comments.size} comentário(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        item {
                            SectionMarker(
                                title = "Exibidos no mural (desde terça-feira)",
                                count = sections.recentComments.size,
                                highlighted = true
                            )
                        }

                        items(
                            items = sections.recentComments,
                            key = { it.id }
                        ) { comment ->
                            MuralCommentItem(
                                comment = comment,
                                onDeleteClick = { commentToDelete = comment }
                            )
                        }

                        if (sections.olderComments.isNotEmpty()) {
                            item {
                                SectionMarker(
                                    title = "Comentários anteriores",
                                    count = sections.olderComments.size,
                                    highlighted = false
                                )
                            }

                            items(
                                items = sections.olderComments,
                                key = { "old-${it.id}" }
                            ) { comment ->
                                MuralCommentItem(
                                    comment = comment,
                                    onDeleteClick = { commentToDelete = comment }
                                )
                            }
                        }
                    }
                }
            }
        }
        // Input bar — always visible at the bottom
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.newCommentText,
                onValueChange = { onEvent(MuralCommentsEvent.OnCommentTextChanged(it)) },
                placeholder = { Text("Escreva um comentário...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4,
                enabled = !uiState.isSendingComment
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onEvent(MuralCommentsEvent.OnSendComment(qrCodeId)) },
                enabled = uiState.newCommentText.isNotBlank() && !uiState.isSendingComment,
                modifier = Modifier.size(48.dp)
            ) {
                if (uiState.isSendingComment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar comentário",
                        tint = if (uiState.newCommentText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionMarker(
    title: String,
    count: Int,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun MuralCommentItem(
    comment: MuralComment,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (comment.highlighted)
        Color(0xFFFFF3CD)   // amarelo-dourado suave
    else
        MaterialTheme.colorScheme.surfaceVariant

    val iconTint = if (comment.highlighted)
        Color(0xFFB8860B)   // dourado escuro (DarkGoldenrod)
    else
        MaterialTheme.colorScheme.primary

    val avatarIcon = if (comment.highlighted)
        Icons.Default.Stars
    else
        Icons.Default.Person

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (comment.highlighted) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = avatarIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(top = 2.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = comment.author.ifEmpty { "Anônimo" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (comment.highlighted) {
                            Text(
                                text = "Destaque",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7A5C00),
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFFFD700).copy(alpha = 0.30f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (comment.timestamp != null) {
                        Text(
                            text = formatTimestamp(comment.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir comentário",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        sdf.format(timestamp.toDate())
    } catch (e: Exception) {
        ""
    }
}

private data class MuralCommentSections(
    val recentComments: List<MuralComment>,
    val olderComments: List<MuralComment>
)

private fun splitCommentsByTuesdayCutoff(comments: List<MuralComment>): MuralCommentSections {
    val cutoff = getLastTuesdayStart()

    val recent = comments.filter { comment ->
        val commentDate = comment.timestamp?.toDate() ?: return@filter false
        !commentDate.before(cutoff)
    }

    val older = comments.filterNot { comment ->
        val commentDate = comment.timestamp?.toDate() ?: return@filterNot false
        !commentDate.before(cutoff)
    }

    return MuralCommentSections(
        recentComments = recent,
        olderComments = older
    )
}

private fun getLastTuesdayStart(): java.util.Date {
    val calendar = Calendar.getInstance()
    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}
