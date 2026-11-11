package com.poketch.wear.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poketch.wear.R

/**
 * Authentic Generation III GBA Pairing QR Code Screen.
 * Displays the 2D QR Code on the 192x192 virtual canvas so the phone app can easily scan and pair.
 */
@Composable
fun GbaWatchPairingQrScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val qrBitmap by viewModel.qrBitmapFlow.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnectedFlow.collectAsStateWithLifecycle()
    val connectedPhoneName by viewModel.connectedPhoneNameFlow.collectAsStateWithLifecycle()

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    GbaWatchCanvas(
        modifier = modifier.background(Color(0xFF0A0F1D))
    ) { scale ->
        // 1. Background Shell (GBA Bag Round Base)
        GbaPixelImage(
            resId = R.drawable.gba_bag_watch_round_base,
            contentDescription = "GBA Pairing Shell",
            modifier = Modifier.gbaVirtualBounds(0, 0, 192, 192, scale)
        )

        // 2. Banner Header (x = 20, y = 14, w = 152, h = 16)
        GbaText(
            text = "PAIR WITH PHONE",
            scale = scale,
            color = Color(0xFFFFB74D),
            fontSizeVirtualPx = 11.0f,
            lineHeightVirtualPx = 12.0f,
            letterSpacingVirtualPx = 0.2f,
            scaleX = 1.0f,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.gbaVirtualBounds(20, 14, 152, 16, scale)
        )

        // 3. QR Code White Card (x = 44, y = 32, w = 104, h = 104)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(44, 32, 104, 104, scale)
                .clip(RoundedCornerShape((8 * scale).dp))
                .background(Color.White)
                .padding((5 * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "Pokétch Pairing QR Code",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GbaText(
                    text = "GENERATING...",
                    scale = scale,
                    color = Color(0xFF64748B),
                    fontSizeVirtualPx = 9.0f,
                    lineHeightVirtualPx = 10.0f,
                    letterSpacingVirtualPx = 0.1f,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Connection Status / Instructions (x = 20, y = 139, w = 152, h = 14)
        GbaText(
            text = if (isConnected) {
                "● LINK ACTIVE (${(connectedPhoneName ?: "PHONE").take(10).uppercase()})"
            } else {
                "SCAN IN PHONE PROFILE"
            },
            scale = scale,
            color = if (isConnected) Color(0xFF78F890) else Color(0xFFB4CDDC),
            fontSizeVirtualPx = 8.5f,
            lineHeightVirtualPx = 9.0f,
            letterSpacingVirtualPx = 0.1f,
            scaleX = 1.0f,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.gbaVirtualBounds(20, 139, 152, 14, scale)
        )

        // 5. Bottom Return Button (x = 44, y = 156, w = 104, h = 22)
        val backInteractionSource = remember { MutableInteractionSource() }
        val isBackPressed by backInteractionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .gbaVirtualBounds(44, 156, 104, 22, scale)
                .graphicsLayer {
                    if (isBackPressed) {
                        translationY = 1f * scale
                    }
                }
                .clickable(
                    interactionSource = backInteractionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateBack()
                },
            contentAlignment = Alignment.Center
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_bag_btn_back_arena,
                contentDescription = "Return Button",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "< RETURN",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 11.0f,
                lineHeightVirtualPx = 11.5f,
                letterSpacingVirtualPx = 0.1f,
                scaleX = 1.0f,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
