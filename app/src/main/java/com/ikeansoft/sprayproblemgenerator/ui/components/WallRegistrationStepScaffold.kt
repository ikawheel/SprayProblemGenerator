package com.ikeansoft.sprayproblemgenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSurfaceColor

@Composable
fun WallRegistrationStepScaffold(
    modifier: Modifier = Modifier,
    headerText: String? = null,
    imageAspectRatio: Float? = null,
    useFullImageViewport: Boolean = false,
    applyStatusBarsPadding: Boolean = false,
    applyImePadding: Boolean = false,
    imageContainerPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = 20.dp
    ),
    bodyCardPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        bottom = 20.dp
    ),
    bodyCardContentPadding: PaddingValues = PaddingValues(16.dp),
    imageContent: @Composable BoxScope.() -> Unit,
    bodyContent: (@Composable ColumnScope.() -> Unit)? = null,
    footerContent: @Composable ColumnScope.() -> Unit
) {
    val resolvedImageTopPadding = if (headerText.isNullOrBlank()) {
        imageContainerPadding.calculateTopPadding()
    } else {
        12.dp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (applyStatusBarsPadding) {
                    Modifier.statusBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (applyImePadding) {
                        Modifier.imePadding()
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!headerText.isNullOrBlank()) {
                    Text(
                        text = headerText,
                        color = AppTextColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(
                            start = 16.dp,
                            top = resolvedImageTopPadding,
                            end = 16.dp,
                            bottom = imageContainerPadding.calculateBottomPadding()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageContainerModifier = if (useFullImageViewport) {
                            Modifier.fillMaxSize()
                        } else if (
                            imageAspectRatio != null &&
                            maxWidth > 0.dp &&
                            maxHeight > 0.dp
                        ) {
                            val availableAspectRatio = maxWidth.value / maxHeight.value
                            if (availableAspectRatio > imageAspectRatio) {
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(imageAspectRatio)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(imageAspectRatio)
                            }
                        } else {
                            Modifier.fillMaxSize()
                        }

                        Box(
                            modifier = imageContainerModifier
                                .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                                .clipToBounds(),
                            content = imageContent
                        )
                    }
                }

                if (bodyContent != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bodyCardPadding),
                        color = AppSurfaceColor,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(bodyCardContentPadding),
                            content = bodyContent
                        )
                    }
                }
            }
        }

        BottomActionBar(content = footerContent)
    }
}
