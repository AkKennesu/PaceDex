package com.poketch.mobile.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.graphicsLayer
import com.poketch.mobile.ui.theme.bouncyPress
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.poketch.core.domain.model.AuthState
import com.poketch.core.domain.model.AuthUser
import com.poketch.mobile.ui.theme.StitchAlertCoral
import com.poketch.mobile.ui.theme.StitchAlertTint
import com.poketch.mobile.ui.theme.StitchBorderSubtle
import com.poketch.mobile.ui.theme.StitchCanvasBackground
import com.poketch.mobile.ui.theme.StitchDarkSlate
import com.poketch.mobile.ui.theme.StitchHydration
import com.poketch.mobile.ui.theme.StitchHydrationTint
import com.poketch.mobile.ui.theme.StitchPartnerMint
import com.poketch.mobile.ui.theme.StitchPrimary
import com.poketch.mobile.ui.theme.StitchPrimaryTint
import com.poketch.mobile.ui.theme.StitchStepsAmber
import com.poketch.mobile.ui.theme.StitchStepsTint
import com.poketch.mobile.ui.theme.StitchSurfaceCard
import com.poketch.mobile.ui.theme.StitchTextMuted
import com.poketch.mobile.ui.theme.StitchTextPrimary
import com.poketch.mobile.ui.theme.StitchTextSecondary
import kotlinx.coroutines.launch

private const val FIREBASE_WEB_CLIENT_ID = "36843683760-tto98ec609fq6rqn04pq463i3ubjisp0.apps.googleusercontent.com"

/**
 * Google Stitch Redesigned Authentication & Trainer Profile Screen
 * Matches Stitch DESIGN.md and bb8fedd9157d47619efde6e5967b8a27.html:
 * - Botanical Canvas with subtle top ambient glow
 * - Botanical Poké Ball Device Emblem with soft glow
 * - Segmented Pill Switcher (Log In vs Sign Up)
 * - Pure White Bento Card with Trainer Email & Password strength gauge
 * - Primary Emerald CTA button with arrow_forward
 * - Google SSO and Guest Testing Bypass Login
 * - Trainer Perk botanical mint card (#DCFCE7)
 * - Authenticated Trainer Profile Card with Cloud Firestore & Watch pairing
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchCanvasBackground)
    ) {
        // Subtle emerald top ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StitchPrimary.copy(alpha = 0.08f),
                            StitchPrimaryTint.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = authState) {
                is AuthState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = StitchPrimary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Connecting Trainer...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StitchTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is AuthState.Authenticated -> {
                    // Authenticated Trainer Profile View
                    StitchAuthenticatedTrainerCard(
                        user = state.user,
                        uiState = uiState,
                        onSyncToCloud = { viewModel.syncToCloud() },
                        onRestoreFromCloud = { viewModel.restoreFromCloud() },
                        onSignOut = { viewModel.signOut() },
                        onScanWatchQr = {
                            try {
                                val options = GmsBarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                    .enableAutoZoom()
                                    .build()
                                val scanner = GmsBarcodeScanning.getClient(context, options)
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        val raw = barcode.rawValue
                                        if (!raw.isNullOrBlank()) {
                                            viewModel.pairWithScannedWatch(raw)
                                        } else {
                                            Toast.makeText(context, "No barcode detected", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnCanceledListener { }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Scanner error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not start scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSyncWatch = { viewModel.syncWithWatchNow() }
                    )
                }
                is AuthState.Unauthenticated -> {
                    // Unauthenticated Stitch Login & Registration View
                    StitchUnauthenticatedAuthSection(
                        uiState = uiState,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onToggleMode = viewModel::toggleMode,
                        onSubmitEmailAuth = viewModel::submitEmailAuth,
                        onGoogleIdTokenReceived = viewModel::signInWithGoogleIdToken,
                        onBypassLogin = viewModel::loginAsDummyTrainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

enum class AuthStep {
    EMAIL_ENTRY,
    OTP_VERIFICATION
}

/**
 * Stitch Login / Registration Component matching bb8fedd9157d47619efde6e5967b8a27.html
 */
@Composable
private fun StitchUnauthenticatedAuthSection(
    uiState: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmitEmailAuth: () -> Unit,
    onGoogleIdTokenReceived: (String) -> Unit,
    onBypassLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }

    var authStep by rememberSaveable { mutableStateOf(AuthStep.EMAIL_ENTRY) }
    var otpDigits by rememberSaveable { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null && authStep == AuthStep.OTP_VERIFICATION) {
            shakeOffset.animateTo(16f, tween(50))
            shakeOffset.animateTo(-16f, tween(50))
            shakeOffset.animateTo(10f, tween(50))
            shakeOffset.animateTo(-10f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    val resId = remember { context.resources.getIdentifier("default_web_client_id", "string", context.packageName) }
    val activeClientId = if (resId != 0) context.getString(resId) else FIREBASE_WEB_CLIENT_ID

    fun launchGoogleSignIn(clientId: String) {
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId.trim())
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onGoogleIdTokenReceived(googleIdTokenCredential.idToken)
                }
            } catch (e: NoCredentialException) {
                Toast.makeText(
                    context,
                    "No Google accounts found. Ensure a Google account is logged in on this device.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: GetCredentialCancellationException) {
                // User dismissed account picker
            } catch (e: GetCredentialException) {
                Toast.makeText(context, "Google Sign-In: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Google Sign-In error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 1. Brand Header & Emblem with Soft Glow
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Poké Ball Device Emblem with Soft Glow
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(StitchPrimaryTint.copy(alpha = 0.8f))
            )

            // Pure White Card Circle with Pokétch Device Emblem
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = StitchSurfaceCard,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PoketchDeviceEmblem(modifier = Modifier.size(46.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "POKÉTCH VITALITY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = StitchPrimary,
            letterSpacing = 1.2.sp
        )

        Text(
            text = "Welcome, Trainer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StitchDarkSlate,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Sync your Wear OS workouts, calorie balance, and collectible companion team.",
            style = MaterialTheme.typography.bodySmall,
            color = StitchTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark

    // 2. Segmented Pill Tab Switcher: Log In vs Sign Up
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = if (isDark) Color(0xFF090F0D) else Color(0xFFEBF0ED),
        border = if (isDark) BorderStroke(1.dp, StitchBorderSubtle) else null
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Log In Tab
            val isLoginSelected = !uiState.isSignUp
            val activeTabBg = if (isDark) Color(0xFF34D399) else StitchDarkSlate
            val activeTabText = if (isDark) Color(0xFF022C22) else Color.White

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { if (uiState.isSignUp) onToggleMode() },
                shape = RoundedCornerShape(26.dp),
                color = if (isLoginSelected) activeTabBg else Color.Transparent,
                shadowElevation = if (isLoginSelected && !isDark) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log In",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLoginSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isLoginSelected) activeTabText else StitchTextSecondary
                    )
                }
            }

            // Sign Up Tab
            val isSignUpSelected = uiState.isSignUp
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { if (!uiState.isSignUp) onToggleMode() },
                shape = RoundedCornerShape(26.dp),
                color = if (isSignUpSelected) activeTabBg else Color.Transparent,
                shadowElevation = if (isSignUpSelected && !isDark) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSignUpSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSignUpSelected) activeTabText else StitchTextSecondary
                    )
                }
            }
        }
    }

    // 3. Animated Step Switcher: EMAIL_ENTRY <-> OTP_VERIFICATION with directional horizontal slide
    AnimatedContent(
        targetState = authStep,
        transitionSpec = {
            if (targetState == AuthStep.OTP_VERIFICATION) {
                (slideInHorizontally(tween(260)) { it } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally(tween(260)) { -it } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { it } + fadeOut(tween(200)))
            }
        },
        label = "auth_step_transition"
    ) { currentStep ->
        when (currentStep) {
            AuthStep.EMAIL_ENTRY -> {
                // Bento Primary Card: Email & Password
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = StitchDarkSlate.copy(alpha = 0.04f),
                            spotColor = StitchDarkSlate.copy(alpha = 0.08f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = StitchSurfaceCard,
                    border = BorderStroke(1.dp, StitchBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Email Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (uiState.isSignUp) "Choose Trainer Email" else "Trainer Email",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchDarkSlate
                                )
                            }

                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = onEmailChanged,
                                placeholder = { Text("trainer@poketch.app", color = StitchTextMuted) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Mail,
                                        contentDescription = null,
                                        tint = StitchTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = StitchSurfaceCard,
                                    unfocusedContainerColor = if (isDark) Color(0xFF131E19) else Color(0xFFF1F4F2),
                                    focusedBorderColor = StitchPrimary,
                                    unfocusedBorderColor = if (isDark) Color(0xFF1E3328) else Color.Transparent
                                )
                            )
                        }

                        // Password Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchDarkSlate
                                )
                            }

                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = onPasswordChanged,
                                placeholder = { Text(if (uiState.isSignUp) "Create a password" else "Enter password", color = StitchTextMuted) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = StitchTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            tint = StitchTextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = StitchSurfaceCard,
                                    unfocusedContainerColor = if (isDark) Color(0xFF131E19) else Color(0xFFF1F4F2),
                                    focusedBorderColor = StitchPrimary,
                                    unfocusedBorderColor = if (isDark) Color(0xFF1E3328) else Color.Transparent
                                )
                            )
                        }

                        // Password Strength Indicator (during Sign Up)
                        if (uiState.isSignUp) {
                            StitchPasswordStrengthIndicator(password = uiState.password)
                        }

                        // Error Message (if any)
                        uiState.errorMessage?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchAlertCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Primary Emerald Action Button with bouncyPress
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(
                                    elevation = if (isDark) 0.dp else 6.dp,
                                    shape = RoundedCornerShape(25.dp),
                                    ambientColor = StitchPrimary.copy(alpha = 0.2f),
                                    spotColor = StitchPrimary.copy(alpha = 0.35f)
                                )
                                .bouncyPress(onClick = onSubmitEmailAuth),
                            shape = RoundedCornerShape(25.dp),
                            color = StitchPrimary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = if (isDark) Color(0xFF022C22) else Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (uiState.isSignUp) "Create Account" else "Sign In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF022C22) else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF022C22) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // 6-Digit Email OTP Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = StitchPrimaryTint,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .bouncyPress {
                                    authStep = AuthStep.OTP_VERIFICATION
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Verified",
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Or Sign In with 6-Digit Email OTP →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF005320),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            AuthStep.OTP_VERIFICATION -> {
                // 6-Digit OTP Verification Card with spring scale pop on digits and shake on error
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeOffset.value }
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = StitchDarkSlate.copy(alpha = 0.04f),
                            spotColor = StitchDarkSlate.copy(alpha = 0.08f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = StitchSurfaceCard,
                    border = BorderStroke(1.dp, StitchBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "6-Digit Security Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )
                        Text(
                            text = "Enter the 6-digit verification code sent to ${uiState.email.ifBlank { "trainer@poketch.app" }}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        // 6 Animated Digit Boxes
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 6) {
                                    val digit = otpDigits.getOrNull(i)?.toString() ?: ""
                                    val isFilled = digit.isNotBlank()
                                    val digitScale by animateFloatAsState(
                                        targetValue = if (isFilled) 1.08f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "otp_digit_scale_$i"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .graphicsLayer {
                                                scaleX = digitScale
                                                scaleY = digitScale
                                            }
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF131E19) else Color(0xFFF1F4F2))
                                            .border(
                                                width = if (isFilled) 2.dp else 1.dp,
                                                color = if (isFilled) StitchPrimary else StitchBorderSubtle,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchDarkSlate
                                        )
                                    }
                                }
                            }

                            // Transparent overlay capturing numeric input
                            BasicTextField(
                                value = otpDigits,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                        otpDigits = it
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = 0.01f }
                            )
                        }

                        // Submit OTP Button with bouncyPress
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(
                                    elevation = if (isDark) 0.dp else 6.dp,
                                    shape = RoundedCornerShape(25.dp),
                                    ambientColor = StitchPrimary.copy(alpha = 0.2f),
                                    spotColor = StitchPrimary.copy(alpha = 0.35f)
                                )
                                .bouncyPress {
                                    if (otpDigits.length == 6) {
                                        onSubmitEmailAuth()
                                    } else {
                                        coroutineScope.launch {
                                            shakeOffset.animateTo(16f, tween(50))
                                            shakeOffset.animateTo(-16f, tween(50))
                                            shakeOffset.animateTo(10f, tween(50))
                                            shakeOffset.animateTo(-10f, tween(50))
                                            shakeOffset.animateTo(0f, tween(50))
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(25.dp),
                            color = StitchPrimary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Verify & Access Pokétch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF022C22) else Color.White
                                )
                            }
                        }

                        // Return to Password Entry
                        Text(
                            text = "← Return to Password Entry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchPrimary,
                            modifier = Modifier
                                .clickable { authStep = AuthStep.EMAIL_ENTRY }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // 4. Subtle Divider: OR CONTINUE WITH
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = StitchBorderSubtle)
        Text(
            text = "  OR CONTINUE WITH  ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = StitchTextMuted,
            letterSpacing = 1.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = StitchBorderSubtle)
    }

    // 5. Social & SSO Stack
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Continue with Google Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = StitchDarkSlate.copy(alpha = 0.03f),
                    spotColor = StitchDarkSlate.copy(alpha = 0.06f)
                )
                .clip(RoundedCornerShape(24.dp))
                .clickable { launchGoogleSignIn(activeClientId) },
            shape = RoundedCornerShape(24.dp),
            color = StitchSurfaceCard,
            border = BorderStroke(1.dp, StitchBorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoogleGlyph(modifier = Modifier.size(20.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = StitchTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Guest Testing Bypass Login Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = StitchDarkSlate.copy(alpha = 0.03f),
                    spotColor = StitchDarkSlate.copy(alpha = 0.06f)
                )
                .clip(RoundedCornerShape(24.dp))
                .clickable { onBypassLogin() },
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF131E19) else Color(0xFFF1F5F2),
            border = BorderStroke(1.dp, StitchBorderSubtle)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Continue as Guest (Bypass Login)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )
            }
        }
    }

    // 6. Trainer Perk Banner (Botanical Mint Card #DCFCE7 / Dark Tactical #0A1711)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = StitchPrimary.copy(alpha = 0.05f),
                spotColor = StitchPrimary.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = StitchPrimaryTint,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF163826) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = StitchSurfaceCard,
                shadowElevation = if (isDark) 0.dp else 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🌱", fontSize = 22.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Trainer Perk",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                }
                Text(
                    text = "New to Pokétch? Create an account to claim your starter companion and auto-sync with Wear OS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E3A2F),
                    lineHeight = 16.sp
                )
            }
        }
    }

    // 7. Terms & Legal Footer
    Text(
        text = "By continuing, you agree to Pokétch Terms of Service and Privacy Policy.",
        style = MaterialTheme.typography.labelSmall,
        color = StitchTextMuted,
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

/**
 * Authenticated Trainer Profile View
 */
@Composable
private fun StitchAuthenticatedTrainerCard(
    user: AuthUser,
    uiState: AuthUiState,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onSignOut: () -> Unit,
    onScanWatchQr: () -> Unit,
    onSyncWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = StitchDarkSlate.copy(alpha = 0.04f),
                spotColor = StitchDarkSlate.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(26.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trainer Identity Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(StitchPrimaryTint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user.displayName?.take(1) ?: "T").uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = StitchPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName ?: "Pokémon Trainer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                    Text(
                        text = user.email ?: "trainer@poketch.app",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextSecondary
                    )
                    Text(
                        text = "Cloud ID: ${user.uid.take(12)}...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = StitchTextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchPrimaryTint
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = StitchBorderSubtle)

            // Cloud Firestore Backup Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CLOUD FIRESTORE BACKUP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchHydration,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (uiState.lastSyncTime != null) {
                        "Last backed up to cloud: ${uiState.lastSyncTime}"
                    } else {
                        "Keep your calories, water, steps and companion progression safely backed up."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSyncToCloud,
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchHydration),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestoreFromCloud,
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StitchBorderSubtle)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StitchDarkSlate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", color = StitchDarkSlate, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = StitchBorderSubtle)

            // Watch Link & Pairing Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "POKÉTCH WATCH LINK & PAIRING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isWatchConnected) StitchPrimary else StitchStepsAmber)
                    )
                    Text(
                        text = if (uiState.isWatchConnected) {
                            "Linked: ${uiState.connectedWatchName ?: "Pokétch Watch"}"
                        } else {
                            "Standalone Mode (No Watch Paired)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                }

                Text(
                    text = if (uiState.isWatchConnected) {
                        "Your watch is actively paired with Wearable Data Layer sync."
                    } else {
                        "Open 'Sync & Diagnostics' on your watch to view its pairing QR code, then tap below to scan."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanWatchQr,
                        enabled = !uiState.isPairingWatch,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Watch QR", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (uiState.isWatchConnected) {
                        OutlinedButton(
                            onClick = onSyncWatch,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, StitchBorderSubtle)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = StitchDarkSlate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Watch", color = StitchDarkSlate, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = StitchBorderSubtle)

            // Sign Out Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onSignOut() },
                shape = RoundedCornerShape(22.dp),
                color = StitchAlertTint,
                border = BorderStroke(1.dp, StitchAlertCoral.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchAlertCoral
                    )
                }
            }
        }
    }
}

/**
 * Botanical Pokétch Device Emblem SVG
 */
@Composable
private fun PoketchDeviceEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        val center = Offset(r, r)

        // Outer Dark Slate Circle
        drawCircle(
            color = Color(0xFF0F172A),
            radius = r,
            center = center
        )

        // Top Red Half
        drawArc(
            color = Color(0xFFEF4444),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.05f, h * 0.05f),
            size = Size(w * 0.9f, h * 0.9f)
        )

        // Bottom White Half
        drawArc(
            color = Color(0xFFFFFFFF),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.05f, h * 0.05f),
            size = Size(w * 0.9f, h * 0.9f)
        )

        // Middle Divider Bar
        drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(0f, h * 0.46f),
            size = Size(w, h * 0.08f)
        )

        // Center Button Circles
        drawCircle(
            color = Color(0xFF0F172A),
            radius = r * 0.34f,
            center = center
        )
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = r * 0.22f,
            center = center
        )
        drawCircle(
            color = Color(0xFF16A34A),
            radius = r * 0.12f,
            center = center
        )
    }
}

/**
 * Google 4-Color Glyph
 */
@Composable
private fun GoogleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Simple stylized 4-color circular ring
        val sw = w * 0.22f
        drawArc(
            color = Color(0xFFEA4335), // Red
            startAngle = -45f,
            sweepAngle = -90f,
            useCenter = false,
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(w - sw, h - sw),
            style = Stroke(width = sw)
        )
        drawArc(
            color = Color(0xFFFBBC05), // Yellow
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(w - sw, h - sw),
            style = Stroke(width = sw)
        )
        drawArc(
            color = Color(0xFF34A853), // Green
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(w - sw, h - sw),
            style = Stroke(width = sw)
        )
        drawArc(
            color = Color(0xFF4285F4), // Blue
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(w - sw, h - sw),
            style = Stroke(width = sw)
        )
        // Center horizontal bar
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(w * 0.45f, h * 0.42f),
            size = Size(w * 0.5f, h * 0.18f)
        )
    }
}

/**
 * Password Strength Indicator with 4-bar gauge and rules
 */
@Composable
private fun StitchPasswordStrengthIndicator(password: String) {
    val hasLength = password.length >= 8
    val hasNumber = password.any { it.isDigit() }
    val hasMix = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }

    var score = 0
    if (password.length >= 6) score++
    if (hasLength) score++
    if (hasNumber) score++
    if (hasMix) score++

    val (label, color, filledBars) = when {
        password.isEmpty() -> Triple("Enter password", StitchTextMuted, 0)
        score >= 4 -> Triple("Strong", StitchPrimary, 4)
        score >= 3 -> Triple("Good", StitchPrimary, 3)
        score >= 2 -> Triple("Fair", StitchStepsAmber, 2)
        else -> Triple("Weak", StitchAlertCoral, 1)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength",
                style = MaterialTheme.typography.labelSmall,
                color = StitchTextSecondary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        // 4 Strength Bars
        val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i <= filledBars) color else (if (isDark) Color(0xFF1F2E26) else Color(0xFFE2E8F0))
                        )
                )
            }
        }

        // Rules chips: 8+ chars, 1 number, Mix uppercase
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PasswordRuleChip(label = "8+ chars", isMet = hasLength)
            PasswordRuleChip(label = "1 number", isMet = hasNumber)
            PasswordRuleChip(label = "Mix uppercase", isMet = hasMix)
        }
    }
}

@Composable
private fun PasswordRuleChip(label: String, isMet: Boolean) {
    val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
    val unmetBg = if (isDark) Color(0xFF131E19) else Color(0xFFF1F4F2)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMet) StitchPrimaryTint else unmetBg,
        border = BorderStroke(1.dp, if (isMet) StitchPrimary.copy(alpha = 0.4f) else (if (isDark) Color(0xFF1F2E26) else Color.Transparent))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isMet) Icons.Default.Check else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isMet) StitchPrimary else StitchTextMuted,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal,
                color = if (isMet) StitchPrimary else StitchTextMuted,
                fontSize = 10.sp
            )
        }
    }
}
