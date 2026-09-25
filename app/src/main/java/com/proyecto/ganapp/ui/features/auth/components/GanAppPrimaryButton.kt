package com.proyecto.ganapp.ui.features.auth.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

/**
 * Botón primario de Auth.
 * El radio de 14.dp es local a este componente y no forma parte de GanAppShapes.
 * Si [isLoading] es true, el click queda deshabilitado. Sin [loadingLabel], se muestra [label].
 */
@Composable
fun GanAppPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loadingLabel: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false
) {
    val widthModifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .then(widthModifier)
            .heightIn(min = 48.dp)
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .clearAndSetSemantics {},
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
                Spacer(modifier = Modifier.width(GanAppSpacing.xs))
                Text(
                    text = loadingLabel ?: label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
