/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 PixelOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package net.pixelos.ota.ui

import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import net.pixelos.ota.R
import net.pixelos.ota.data.ChangelogState
import net.pixelos.ota.data.source.network.MaintainerInfo
import net.pixelos.ota.deviceinfo.DeviceInfoUtils
import net.pixelos.ota.ui.theme.LocalAppFonts
import net.pixelos.ota.updates.action.UpdateAction
import net.pixelos.ota.updates.action.UpdateActionType
import net.pixelos.ota.updates.state.ProgressState
import net.pixelos.ota.updates.state.UpdateItemState
import net.pixelos.ota.updates.state.UpdateOperationPhase
import net.pixelos.ota.util.StringUtil
import java.util.Date

private val ButtonHeight = 56.dp
private val SegmentedGap = 4.dp

private fun horizontalSegmentedShape(
    index: Int,
    count: Int,
    outerCornerRadius: Dp = 14.dp,
    innerCornerRadius: Dp = 4.dp,
): Shape {
    return when {
        count <= 1 -> RoundedCornerShape(outerCornerRadius)
        index == 0 -> RoundedCornerShape(
            topStart = outerCornerRadius,
            bottomStart = outerCornerRadius,
            topEnd = innerCornerRadius,
            bottomEnd = innerCornerRadius,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = innerCornerRadius,
            bottomStart = innerCornerRadius,
            topEnd = outerCornerRadius,
            bottomEnd = outerCornerRadius,
        )
        else -> RoundedCornerShape(innerCornerRadius)
    }
}

private fun verticalSegmentedShape(
    index: Int,
    count: Int,
    outerCornerRadius: Dp = 14.dp,
    innerCornerRadius: Dp = 4.dp,
): Shape {
    return when {
        count <= 1 -> RoundedCornerShape(outerCornerRadius)
        index == 0 -> RoundedCornerShape(
            topStart = outerCornerRadius,
            topEnd = outerCornerRadius,
            bottomStart = innerCornerRadius,
            bottomEnd = innerCornerRadius,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = innerCornerRadius,
            topEnd = innerCornerRadius,
            bottomStart = outerCornerRadius,
            bottomEnd = outerCornerRadius,
        )
        else -> RoundedCornerShape(innerCornerRadius)
    }
}

@Composable
fun SystemUpdateScreen(
    supportingText: String? = null,
    supportingTextIsError: Boolean = false,
    isBusy: Boolean,
    hasCheckedInSession: Boolean = false,
    canCheckForUpdates: Boolean = true,
    onBackClick: () -> Unit,
    onCheckClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier,
    updateItem: UpdateItemState? = null,
    deviceStatus: String? = null,
    maintainerInfo: MaintainerInfo? = null,
    changelogState: ChangelogState = ChangelogState.Idle,
    onUpdateAction: (UpdateAction) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hideSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
            }
        }
    }

    var isCardFlipped by rememberSaveable { mutableStateOf(false) }
    val cardRotation by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing,
        ),
        label = "cardFlipAnimation",
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.top_bar_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LocalAppFonts.current.topBarTitle,
                        ),
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 12.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onPreferencesClick,
                        modifier = Modifier.padding(end = 12.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            BottomActionArea(
                updateItem = updateItem,
                isBusy = isBusy,
                hasCheckedInSession = hasCheckedInSession,
                canCheckForUpdates = canCheckForUpdates,
                supportingText = supportingText,
                supportingTextIsError = supportingTextIsError,
                onCheckClick = onCheckClick,
                onViewNotesClick = { showBottomSheet = true },
                onUpdateAction = onUpdateAction,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // 3D Flippable Card
            val outlineVariant = MaterialTheme.colorScheme.outlineVariant
            val cardBorder = remember(outlineVariant) {
                BorderStroke(1.dp, outlineVariant.copy(alpha = 0.2f))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center,
            ) {
                // Front Card
                Card(
                    onClick = { isCardFlipped = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isCardFlipped) 0f else 1f)
                        .graphicsLayer {
                            rotationY = cardRotation
                            cameraDistance = 16f * density
                            alpha = if (cardRotation <= 90f) 1f else 0f
                        },
                    shape = MaterialTheme.shapes.extraLarge,
                    border = cardBorder,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    CardFrontFace(
                        imageRes = R.drawable.updater,
                    )
                }

                // Back Card
                Card(
                    onClick = { isCardFlipped = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isCardFlipped) 1f else 0f)
                        .graphicsLayer {
                            rotationY = cardRotation + 180f
                            cameraDistance = 16f * density
                            alpha = if (cardRotation > 90f) 1f else 0f
                        },
                    shape = MaterialTheme.shapes.extraLarge,
                    border = cardBorder,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    CardBackFace(
                        isCardFlipped = isCardFlipped,
                        updateItem = updateItem,
                        deviceStatus = deviceStatus,
                        maintainerInfo = maintainerInfo,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            UpdateNotesSheetContent(
                updateItem = updateItem,
                changelogState = changelogState,
                onClose = hideSheet,
            )
        }
    }
}

/**
 * Front face of the flippable card displaying the banner image.
 */
@Composable
private fun CardFrontFace(
    imageRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = stringResource(R.string.cd_update_banner),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // "More info" pill overlay at bottom-center
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.label_more_info),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Back face of the flippable card displaying dynamic system and update specifications.
 */
@Composable
private fun CardBackFace(
    isCardFlipped: Boolean,
    updateItem: UpdateItemState?,
    deviceStatus: String? = null,
    maintainerInfo: MaintainerInfo? = null,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    val segmentBg = MaterialTheme.colorScheme.surfaceContainerLow
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val segmentBorder = remember(outlineVariant) {
        BorderStroke(1.dp, outlineVariant.copy(alpha = 0.25f))
    }
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary

    val iconBgColor = MaterialTheme.colorScheme.primaryContainer
    val iconTintColor = MaterialTheme.colorScheme.onPrimaryContainer

    val shapeRowStart = remember { horizontalSegmentedShape(0, 2) }
    val shapeRowEnd = remember { horizontalSegmentedShape(1, 2) }
    val shapeListTop = remember { verticalSegmentedShape(0, 3) }
    val shapeListMid = remember { verticalSegmentedShape(1, 3) }
    val shapeListEnd = remember { verticalSegmentedShape(2, 3) }

    val displayVersion = updateItem?.buildVersion?.ifBlank { null }
        ?: maintainerInfo?.version?.ifBlank { null }
        ?: ""
    val hasOtaUpdate = !updateItem?.fileSize.isNullOrBlank()
    val displaySize = if (hasOtaUpdate) {
        updateItem!!.fileSize
    } else {
        DeviceInfoUtils.androidVersion
    }
    val sizeSubtitle = if (hasOtaUpdate) {
        stringResource(R.string.label_size)
    } else {
        stringResource(R.string.label_android)
    }
    val sizeIcon = if (hasOtaUpdate) {
        painterResource(R.drawable.ic_download)
    } else {
        painterResource(R.drawable.android_24)
    }
    val sizeContentDesc = if (hasOtaUpdate) {
        stringResource(R.string.cd_size)
    } else {
        stringResource(R.string.cd_android)
    }
    val displayCodename = updateItem?.device?.ifBlank { null }
        ?: maintainerInfo?.codename?.ifBlank { null }
        ?: DeviceInfoUtils.device.ifBlank { "ASCP" }
    val displayStatus = (maintainerInfo?.officialStatus
        ?: DeviceInfoUtils.buildType.ifBlank { null }
        ?: stringResource(R.string.status_official))
        .uppercase()

    val maintainerName = updateItem?.maintainer?.ifBlank { null }
        ?: maintainerInfo?.maintainer?.ifBlank { null }
        ?: stringResource(R.string.maintainer_name)
    val supportUrl = updateItem?.forumUrl?.ifBlank { null }
        ?: maintainerInfo?.supportUrl?.ifBlank { null }
        ?: stringResource(R.string.support_group_url)
    val donationUrl = updateItem?.donationUrl?.ifBlank { null }
        ?: maintainerInfo?.donationUrl?.ifBlank { null }
        ?: stringResource(R.string.donation_url)
    val githubUrl = updateItem?.githubUrl?.ifBlank { null }
        ?: maintainerInfo?.github?.ifBlank { null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top area: Project logo + Project name
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_project_logo),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = stringResource(R.string.project_name),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFonts.current.topBarTitle,
                    ),
                    color = titleColor,
                )
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        // Middle Content: Spec Cards & Links
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentedGap),
        ) {
            // Row 0: Version & Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SegmentedGap),
            ) {
                InfoTile(
                    title = displayVersion,
                    subtitle = stringResource(R.string.label_version),
                    iconPainter = painterResource(R.drawable.ic_version),
                    iconContentDescription = stringResource(R.string.cd_version),
                    iconTint = iconTintColor,
                    iconBgColor = iconBgColor,
                    shape = shapeRowStart,
                    segmentBg = segmentBg,
                    segmentBorder = segmentBorder,
                    titleColor = titleColor,
                    subtitleColor = subtitleColor,
                    modifier = Modifier.weight(1f),
                )

                InfoTile(
                    title = displaySize,
                    subtitle = sizeSubtitle,
                    iconPainter = sizeIcon,
                    iconContentDescription = sizeContentDesc,
                    iconTint = iconTintColor,
                    iconBgColor = iconBgColor,
                    shape = shapeRowEnd,
                    segmentBg = segmentBg,
                    segmentBorder = segmentBorder,
                    titleColor = titleColor,
                    subtitleColor = subtitleColor,
                    modifier = Modifier.weight(1f),
                )
            }

            // Row 1: Codename & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SegmentedGap),
            ) {
                InfoTile(
                    title = displayCodename,
                    subtitle = stringResource(R.string.label_codename),
                    iconPainter = painterResource(R.drawable.ic_phone),
                    iconContentDescription = stringResource(R.string.cd_codename),
                    iconTint = iconTintColor,
                    iconBgColor = iconBgColor,
                    shape = shapeRowStart,
                    segmentBg = segmentBg,
                    segmentBorder = segmentBorder,
                    titleColor = titleColor,
                    subtitleColor = subtitleColor,
                    modifier = Modifier.weight(1f),
                )

                InfoTile(
                    title = displayStatus,
                    subtitle = stringResource(R.string.cd_status),
                    iconPainter = painterResource(R.drawable.ic_check),
                    iconContentDescription = stringResource(R.string.cd_status),
                    iconTint = iconTintColor,
                    iconBgColor = iconBgColor,
                    shape = shapeRowEnd,
                    segmentBg = segmentBg,
                    segmentBorder = segmentBorder,
                    titleColor = titleColor,
                    subtitleColor = subtitleColor,
                    modifier = Modifier.weight(1f),
                )
            }

            // Item 2: Maintainer
            SegmentedActionItem(
                title = maintainerName,
                subtitle = stringResource(R.string.label_maintainer),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = stringResource(R.string.cd_maintainer),
                        tint = iconTintColor,
                        modifier = Modifier.size(20.dp),
                    )
                },
                badgeShape = CircleShape,
                badgeBgColor = iconBgColor,
                itemShape = shapeListTop,
                segmentBg = segmentBg,
                segmentBorder = segmentBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                enabled = isCardFlipped && !githubUrl.isNullOrBlank(),
                onClick = githubUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    { uriHandler.openUri(url) }
                },
            )

            // Item 3: Support group
            SegmentedActionItem(
                title = stringResource(R.string.support_group_title),
                subtitle = supportUrl.ifBlank { "—" },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram),
                        contentDescription = stringResource(R.string.cd_support_group),
                        tint = iconTintColor,
                        modifier = Modifier.size(20.dp),
                    )
                },
                badgeShape = CircleShape,
                badgeBgColor = iconBgColor,
                itemShape = shapeListMid,
                segmentBg = segmentBg,
                segmentBorder = segmentBorder,
                titleColor = titleColor,
                subtitleColor = if (supportUrl.isNotBlank()) linkColor else subtitleColor,
                enabled = isCardFlipped && supportUrl.isNotBlank(),
                onClick = { if (supportUrl.isNotBlank()) uriHandler.openUri(supportUrl) },
            )

            // Item 4: Donation
            SegmentedActionItem(
                title = stringResource(R.string.donation_title),
                subtitle = donationUrl.ifBlank { "—" },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.VolunteerActivism,
                        contentDescription = stringResource(R.string.cd_donation),
                        tint = iconTintColor,
                        modifier = Modifier.size(20.dp),
                    )
                },
                badgeShape = CircleShape,
                badgeBgColor = iconBgColor,
                itemShape = shapeListEnd,
                segmentBg = segmentBg,
                segmentBorder = segmentBorder,
                titleColor = titleColor,
                subtitleColor = if (donationUrl.isNotBlank()) linkColor else subtitleColor,
                enabled = isCardFlipped && donationUrl.isNotBlank(),
                onClick = { if (donationUrl.isNotBlank()) uriHandler.openUri(donationUrl) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoTile(
    title: String,
    subtitle: String?,
    iconPainter: Painter,
    iconContentDescription: String,
    iconTint: Color,
    iconBgColor: Color,
    shape: Shape,
    segmentBg: Color,
    segmentBorder: BorderStroke,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = shape,
        color = segmentBg,
        border = segmentBorder,
        modifier = modifier.height(58.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = iconContentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFonts.current.topBarTitle,
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedActionItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    badgeShape: Shape,
    badgeBgColor: Color,
    itemShape: Shape,
    segmentBg: Color,
    segmentBorder: BorderStroke,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        shape = itemShape,
        color = segmentBg,
        border = segmentBorder,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        ListItem(
            modifier = Modifier.padding(vertical = 2.dp),
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(badgeShape)
                        .background(badgeBgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            },
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun BottomActionArea(
    updateItem: UpdateItemState?,
    isBusy: Boolean,
    hasCheckedInSession: Boolean,
    canCheckForUpdates: Boolean,
    supportingText: String?,
    supportingTextIsError: Boolean,
    onCheckClick: () -> Unit,
    onViewNotesClick: () -> Unit,
    onUpdateAction: (UpdateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (supportingTextIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        if (updateItem == null) {
            if (isBusy) {
                // Checking state: Pill with animated ellipsis dots
                Surface(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(ButtonHeight / 2),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonHeight),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.checking_for_update_base),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        AnimatedEllipsisDots(
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                val buttonText = if (hasCheckedInSession) {
                    stringResource(R.string.latest_version)
                } else {
                    stringResource(R.string.check_for_update)
                }
                val isEnabled = !hasCheckedInSession && canCheckForUpdates

                Button(
                    onClick = onCheckClick,
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonHeight),
                    shape = RoundedCornerShape(ButtonHeight / 2),
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else {
            IntegratedActionButton(
                updateItem = updateItem,
                onViewNotesClick = onViewNotesClick,
                onUpdateAction = onUpdateAction,
            )
        }
    }
}

@Composable
private fun IntegratedActionButton(
    updateItem: UpdateItemState,
    onViewNotesClick: () -> Unit,
    onUpdateAction: (UpdateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primary = updateItem.actions.primary
    val secondary = updateItem.actions.secondary
    val progress = updateItem.progress
    val phase = updateItem.phase

    val isReboot = phase == UpdateOperationPhase.WAITING_FOR_REBOOT ||
            primary.type == UpdateActionType.REBOOT
    val isDownloading = phase == UpdateOperationPhase.DOWNLOADING ||
            primary.type == UpdateActionType.PAUSE_DOWNLOAD
    val isPaused = phase == UpdateOperationPhase.DOWNLOAD_PAUSED ||
            phase == UpdateOperationPhase.DOWNLOAD_ERROR ||
            primary.type == UpdateActionType.RESUME_DOWNLOAD
    val isVerifying = phase == UpdateOperationPhase.VERIFYING ||
            (primary.type == UpdateActionType.START_INSTALL && !primary.enabled && progress is ProgressState.Indeterminate)
    val isInstalling = phase == UpdateOperationPhase.INSTALLING ||
            phase == UpdateOperationPhase.INSTALLING_RECOVERY ||
            phase == UpdateOperationPhase.FINALIZING ||
            phase == UpdateOperationPhase.INSTALLATION_SUSPENDED ||
            primary.type == UpdateActionType.PAUSE_INSTALL ||
            primary.type == UpdateActionType.RESUME_INSTALL

    val isProgressState = (isDownloading || isInstalling || isPaused || isVerifying) && !isReboot
    val isIdleReady = !isProgressState && !isReboot

    val percent: Float? = when (progress) {
        is ProgressState.Determinate -> progress.percent
        else -> null
    }

    val buttonText = when {
        isReboot -> stringResource(R.string.reboot_now)
        isDownloading -> {
            if (percent != null) {
                stringResource(R.string.action_downloading_pct, percent.toInt())
            } else {
                stringResource(R.string.downloading_notification)
            }
        }
        isPaused -> {
            if (percent != null) {
                stringResource(R.string.action_paused_pct, percent.toInt())
            } else {
                stringResource(R.string.download_paused_notification)
            }
        }
        isInstalling -> {
            if (percent != null) {
                stringResource(R.string.action_installing_pct, percent.toInt())
            } else {
                stringResource(R.string.installing_update_title)
            }
        }
        isVerifying -> stringResource(R.string.action_verifying_update)
        isIdleReady -> stringResource(R.string.action_download_install)
        else -> primary.type.title(context)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = ((percent ?: 0f) / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "integratedButtonProgress",
    )

    val buttonShape = RoundedCornerShape(ButtonHeight / 2)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top button: Turns into "Cancel" state when in progress; otherwise "View Update Notes"
        if (isProgressState) {
            val cancelAction = secondary ?: if (isInstalling) {
                UpdateAction(UpdateActionType.CANCEL_INSTALL)
            } else {
                UpdateAction(UpdateActionType.CANCEL_DOWNLOAD)
            }

            OutlinedButton(
                onClick = { onUpdateAction(cancelAction) },
                enabled = cancelAction.enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                shape = buttonShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            // Update available or waiting for reboot: "View Update Notes"
            OutlinedButton(
                onClick = onViewNotesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                shape = buttonShape,
            ) {
                Text(
                    text = stringResource(R.string.action_view_update_notes),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        // Subhead caption: Download size & ETA if downloading
        if (progress is ProgressState.Determinate && progress.downloadedSize.isNotEmpty()) {
            val caption = listOf(progress.downloadedSize, progress.eta)
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
            if (caption.isNotEmpty()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val buttonBgColor = if (isReboot) {
            MaterialTheme.colorScheme.primary
        } else if (isProgressState) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.primary
        }

        val textColor = if (isReboot || !isProgressState) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Surface(
            onClick = { onUpdateAction(primary) },
            enabled = primary.enabled,
            shape = buttonShape,
            color = buttonBgColor,
            border = if (isProgressState) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(buttonShape),
                contentAlignment = Alignment.Center,
            ) {
                // Background progress fill if active
                if (isProgressState && percent != null && percent > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedProgress)
                            .align(Alignment.CenterStart)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                            )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    if (isReboot) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isVerifying || ((isDownloading || isInstalling) && percent == null)) {
                        AnimatedEllipsisDots(
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEllipsisDots(
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ellipsis_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0f at 0
                0f at 300
                1f at 350
                1f at 1450
                0f at 1500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "dot1Alpha",
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0f at 0
                0f at 650
                1f at 700
                1f at 1450
                0f at 1500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "dot2Alpha",
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0f at 0
                0f at 1000
                1f at 1050
                1f at 1450
                0f at 1500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "dot3Alpha",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ".",
            style = style,
            color = color,
            modifier = Modifier.graphicsLayer { alpha = dot1Alpha },
        )
        Text(
            text = ".",
            style = style,
            color = color,
            modifier = Modifier.graphicsLayer { alpha = dot2Alpha },
        )
        Text(
            text = ".",
            style = style,
            color = color,
            modifier = Modifier.graphicsLayer { alpha = dot3Alpha },
        )
    }
}

@Composable
private fun UpdateNotesSheetContent(
    updateItem: UpdateItemState?,
    changelogState: ChangelogState,
    onClose: () -> Unit,
) {
    val subtitle = remember(updateItem) {
        listOfNotNull(
            updateItem?.buildVersion?.takeIf { it.isNotBlank() },
            updateItem?.fileSize?.takeIf { it.isNotBlank() },
        ).joinToString(" • ")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.bottom_sheet_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = LocalAppFonts.current.topBarTitle,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (changelogState) {
                    ChangelogState.Idle -> {
                        Text(
                            text = stringResource(R.string.changelog_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChangelogState.Loading -> {
                        Text(
                            text = stringResource(R.string.changelog_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChangelogState.Error -> {
                        Text(
                            text = stringResource(R.string.changelog_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is ChangelogState.Loaded -> {
                        if (changelogState.markdown.isBlank()) {
                            Text(
                                text = stringResource(R.string.changelog_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownText(
                                markdown = changelogState.markdown,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(ButtonHeight),
            shape = RoundedCornerShape(ButtonHeight / 2),
        ) {
            Text(
                text = stringResource(R.string.action_got_it),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

