package com.proyecto.ganapp.ui.features.animals

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

private val AnimalPhotoMaxHeight = GanAppSpacing.xxl * 4

private val selectableSpecies = listOf(
    EspecieAnimal.VACA to "Vaca",
    EspecieAnimal.CERDO to "Cerdo",
    EspecieAnimal.OVEJA to "Oveja",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimalFormPhoto(
    fotoUri: String?,
    fallbackSpecies: EspecieAnimal?,
    enabled: Boolean,
    pickLabel: String,
    showRemove: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    emptyFallbackText: String? = null,
) {
    var photoFailed by remember(fotoUri) { mutableStateOf(false) }
    val showPhoto = !fotoUri.isNullOrBlank() && !photoFailed
    val frameModifier = Modifier
        .widthIn(max = AnimalPhotoMaxHeight * (4f / 3f))
        .aspectRatio(4f / 3f)
        .clip(MaterialTheme.shapes.large)

    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (showPhoto) {
                AsyncImage(
                    model = fotoUri,
                    contentDescription = "Preview foto",
                    modifier = frameModifier,
                    contentScale = ContentScale.Crop,
                    onError = { photoFailed = true },
                )
            } else {
                AnimalFormPhotoFallback(
                    species = fallbackSpecies,
                    emptyFallbackText = emptyFallbackText,
                    modifier = frameModifier,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        ) {
            OutlinedButton(
                onClick = onPickPhoto,
                enabled = enabled,
                modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
            ) {
                Text(text = pickLabel, textAlign = TextAlign.Center)
            }
            if (showRemove) {
                TextButton(
                    onClick = onRemovePhoto,
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
                ) {
                    Text(text = "Quitar foto", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AnimalFormPhotoFallback(
    species: EspecieAnimal?,
    emptyFallbackText: String?,
    modifier: Modifier = Modifier,
) {
    val imageRes = species?.let(::getAnimalImageRes)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                imageRes != null -> {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(GanAppSpacing.md),
                        contentScale = ContentScale.Fit,
                    )
                }
                emptyFallbackText != null -> {
                    Text(
                        text = emptyFallbackText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(GanAppSpacing.md),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(GanAppSpacing.xxl),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AnimalSpeciesSelector(
    selected: EspecieAnimal?,
    enabled: Boolean,
    onSelected: (EspecieAnimal) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
    ) {
        selectableSpecies.forEach { (option, label) ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                enabled = enabled,
                label = { Text(label) },
                modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
            )
        }
    }
}

@Composable
internal fun AnimalFormSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun AnimalFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = error != null,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        suffix = suffix?.let { text -> { Text(text) } },
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

internal val AnimalFormPrimaryButtonMinHeight = 56.dp
