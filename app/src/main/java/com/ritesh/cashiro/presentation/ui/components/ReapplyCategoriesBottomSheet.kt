package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.domain.service.AutomaticCategoryChange
import com.ritesh.cashiro.domain.service.AutomaticCategoryPreview
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Sync
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import java.time.format.DateTimeFormatter

/**
 * Review prompt before writing any category change. Every proposed change is
 * checked by default; unchecking a row keeps that transaction untouched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReapplyCategoriesBottomSheet(
    preview: AutomaticCategoryPreview?,
    onDismiss: () -> Unit,
    onApply: (List<AutomaticCategoryChange>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lazyListState = rememberLazyListState()

    val changes = preview?.changes.orEmpty()
    val checkedIds = remember(preview) { changes.map { it.transaction.id }.toMutableStateList() }

    val groupedByMerchant = remember(preview) { changes.groupBy { it.transaction.merchantName } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Iconax.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reapply_categories),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (preview != null) {
                            Text(
                                text = stringResource(
                                    R.string.reapply_categories_scanned_format,
                                    preview.scannedTransactions
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                DashedLine(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            when {
                preview == null -> item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            text = stringResource(R.string.reapply_categories_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                changes.isEmpty() -> item {
                    Text(
                        text = stringResource(R.string.reapply_categories_no_changes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    item {
                        Text(
                            text = stringResource(R.string.reapply_categories_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    groupedByMerchant.forEach { (merchant, merchantChanges) ->
                        item(key = "header-$merchant") {
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        items(
                            count = merchantChanges.size,
                            key = { index -> "row-${merchantChanges[index].transaction.id}" }
                        ) { index ->
                            ReapplyCategoryRow(
                                change = merchantChanges[index],
                                checked = merchantChanges[index].transaction.id in checkedIds,
                                onCheckedChange = { checked ->
                                    val id = merchantChanges[index].transaction.id
                                    if (checked) checkedIds.add(id) else checkedIds.remove(id)
                                }
                            )
                        }
                    }

                    if (checkedIds.isNotEmpty()) {
                        item {
                            Button(
                                onClick = {
                                    onApply(
                                        changes.filter { it.transaction.id in checkedIds }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.md),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.reapply_categories_apply_count,
                                        checkedIds.size
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReapplyCategoryRow(
    change: AutomaticCategoryChange,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val newCategoryInfo = CategoryMapping.categories[change.newCategory]
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = newCategoryInfo?.color ?: MaterialTheme.colorScheme.primary
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = change.transaction.merchantName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = change.transaction.dateTime.format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = if (change.transaction.category.isBlank()) "—" else change.transaction.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        newCategoryInfo?.color ?: MaterialTheme.colorScheme.primary
                    )
            )
            Text(
                text = change.newCategory,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = newCategoryInfo?.color ?: MaterialTheme.colorScheme.primary
            )
        }
    }
}
