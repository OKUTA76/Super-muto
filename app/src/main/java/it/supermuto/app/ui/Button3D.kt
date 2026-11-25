package it.supermuto.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colori di un pulsante 3D: faccia superiore e bordo inferiore (lo "spessore"). */
data class Colori3D(val faccia: Color, val bordo: Color)

val Rosso = Colori3D(Color(0xFFE53935), Color(0xFF8E1B18))
val Verde = Colori3D(Color(0xFF2E9E4F), Color(0xFF145A2A))
val Blu = Colori3D(Color(0xFF1E7BE0), Color(0xFF0B3F80))
val Arancio = Colori3D(Color(0xFFF08A00), Color(0xFF9A5200))
val Viola = Colori3D(Color(0xFF7B4FD6), Color(0xFF432985))
val Turchese = Colori3D(Color(0xFF0E9AA7), Color(0xFF075961))
val Grigio = Colori3D(Color(0xFFB9BEC7), Color(0xFF7A808A))

/**
 * Pulsante "a rilievo": quando lo premi scende davvero verso il basso,
 * con una piccola vibrazione, come un tasto fisico.
 * Il modifier deve dare una dimensione (es. size, height + fillMaxWidth).
 */
@Composable
fun Button3D(
    onClick: () -> Unit,
    colori: Colori3D,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    spessore: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val premuto by interaction.collectIsPressedAsState()
    val discesa by animateDpAsState(
        targetValue = if (premuto) spessore - 2.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "discesa",
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    ) {
        // Spessore / lato del pulsante, con ombra a terra
        Box(
            Modifier
                .matchParentSize()
                .padding(top = spessore)
                .shadow(if (premuto) 2.dp else 10.dp, shape)
                .background(colori.bordo, shape),
        )
        // Faccia superiore, lucida, che si abbassa quando premuta
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = spessore)
                .offset(y = discesa)
                .background(
                    Brush.verticalGradient(
                        listOf(lerp(colori.faccia, Color.White, 0.30f), colori.faccia, lerp(colori.faccia, Color.Black, 0.08f))
                    ),
                    shape,
                )
                .border(3.dp, lerp(colori.faccia, Color.White, 0.45f), shape),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
