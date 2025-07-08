package com.w2sv.wifiwidget.ui.screens.widgetconfiguration.components.configuration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.w2sv.composed.extensions.thenIf
import com.w2sv.composed.extensions.thenIfNotNull
import com.w2sv.wifiwidget.R
import com.w2sv.wifiwidget.ui.designsystem.InfoIcon
import com.w2sv.wifiwidget.ui.designsystem.KeyboardArrowRightIcon
import com.w2sv.wifiwidget.ui.designsystem.SubPropertyKeyboardArrowRightIcon
import com.w2sv.wifiwidget.ui.designsystem.biggerIconSize
import com.w2sv.wifiwidget.ui.designsystem.nestedContentBackground
import com.w2sv.wifiwidget.ui.utils.alphaDecreased
import com.w2sv.wifiwidget.ui.utils.shake
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * For alignment of primary check row click elements with sub property click elements
 */
private val primaryCheckRowModifier = Modifier.padding(end = 16.dp)

@Composable
fun CheckRowColumn(elements: ImmutableList<CheckRowColumnElement.CheckRow<*>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        elements
            .forEach { data ->
                when (data.hasSubProperties) {
                    false -> {
                        CheckRow(data = data, modifier = primaryCheckRowModifier)
                    }

                    true -> {
                        CheckRowWithSubProperties(
                            data = data
                        )
                    }
                }
            }
    }
}

@Composable
fun DragAndDroppableCheckRowColumn(
    elements: ImmutableList<CheckRowColumnElement.CheckRow<*>>,
    modifier: Modifier = Modifier,
    onDrop: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val localHapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onDrop(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.heightIn(
            max = 2_000.dp // Necessary due to nesting inside scrollable column. Chosen arbitrarily.
        ),
        userScrollEnabled = false
    ) {
        items(elements, key = { it.property.labelRes }) { data ->
            ReorderableItem(reorderableLazyListState, key = data.property.labelRes) { isDragging ->
                val itemModifier = Modifier
                    .longPressDraggableHandle(
                        onDragStarted = {
                            localHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                    .thenIf(isDragging) {
                        val shadowColor = MaterialTheme.colorScheme.secondary.alphaDecreased()
                        shadow(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    }

                when (data.hasSubProperties) {
                    false -> {
                        CheckRow(
                            data = data,
                            modifier = primaryCheckRowModifier.then(itemModifier)
                        )
                    }

                    true -> {
                        CheckRowWithSubProperties(
                            data = data,
                            modifier = itemModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(data: CheckRowColumnElement.CheckRow<*>, modifier: Modifier = Modifier) {
    CheckRowBase(
        data = data,
        leadingIcon = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                KeyboardArrowRightIcon(tint = data.leadingIconAndLabelColor)
            }
        },
        modifier = modifier,
        labelColor = data.leadingIconAndLabelColor
    )
}

@Composable
private fun CheckRowWithSubProperties(data: CheckRowColumnElement.CheckRow<*>, modifier: Modifier = Modifier) {
    var expandSubProperties by rememberSaveable {
        mutableStateOf(false)
    }
    // Collapse subProperties on unchecking
    LaunchedEffect(data, data.isChecked()) {
        if (!data.isChecked()) {
            expandSubProperties = false
        }
    }

    Column(modifier = modifier) {
        CheckRowBase(
            data = data,
            leadingIcon = {
                FilledTonalIconButton(
                    onClick = { expandSubProperties = !expandSubProperties },
                    enabled = data.isChecked(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        disabledContainerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (expandSubProperties) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            },
            modifier = primaryCheckRowModifier,
            labelColor = data.leadingIconAndLabelColor
        )

        AnimatedVisibility(visible = expandSubProperties) {
            SubPropertyCheckRowColumn(elements = data.subPropertyColumnElements!!)
        }
    }
}

@Composable
private fun SubPropertyCheckRowColumn(elements: ImmutableList<CheckRowColumnElement>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(start = 24.dp) // Make background start at the indentation of CheckRowBase label
            .nestedContentBackground()
            .padding(start = subPropertyColumnPadding)
            .animateContentSize()
    ) {
        elements.forEach { element ->
            when (element) {
                is CheckRowColumnElement.CheckRow<*> -> {
                    if (element.show()) {
                        CheckRowBase(
                            data = element,
                            modifier = element.modifier,
                            fontSize = subPropertyCheckRowColumnFontSize,
                            leadingIcon = { SubPropertyKeyboardArrowRightIcon() }
                        )
                    }
                }

                is CheckRowColumnElement.Custom -> {
                    element.content()
                }
            }
        }
    }
}

@Composable
fun VersionsHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.versions),
        modifier = modifier.padding(top = subPropertyColumnPadding),
        fontSize = subPropertyCheckRowColumnFontSize,
        fontWeight = FontWeight.SemiBold
    )
}

private val subPropertyCheckRowColumnFontSize = 14.sp
private val subPropertyColumnPadding = 12.dp

@Composable
private fun CheckRowBase(
    data: CheckRowColumnElement.CheckRow<*>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val label = stringResource(id = data.property.labelRes)
    val checkBoxCD = stringResource(id = R.string.set_unset, label)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
            .then(data.modifier)
            .thenIfNotNull(data.shakeController) {
                shake(it)
            }
    ) {
        leadingIcon?.run {
            invoke()
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            fontSize = fontSize,
            color = labelColor,
            modifier = Modifier.weight(1.0f)
        )
        data.showInfoDialog?.let {
            InfoIconButton(
                onClick = { it() },
                contentDescription = stringResource(id = R.string.info_icon_cd, label)
            )
        }
        Checkbox(
            checked = data.isChecked(),
            onCheckedChange = {
                data.onCheckedChange(it)
            },
            modifier = Modifier.semantics {
                contentDescription = checkBoxCD
            }
        )
    }
}

@Composable
private fun InfoIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        InfoIcon(
            contentDescription = contentDescription,
            modifier = Modifier.size(biggerIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
