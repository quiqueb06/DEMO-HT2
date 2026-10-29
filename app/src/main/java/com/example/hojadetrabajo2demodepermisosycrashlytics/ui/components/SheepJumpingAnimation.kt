package com.example.hojadetrabajo2demodepermisosycrashlytics.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Draws a sheep that performs a parabolic arc OVER a fence while an alarm is active.
 *
 * When [isJumping] is `true` the sheep travels from the left of the fence, arcs
 * over the top (peak at the fence center), lands on the right, then teleports back
 * to repeat. When `false` the sheep rests on the left side.
 *
 * The fence sign always displays [timeLabel] — so the sheep literally jumps over
 * the current time.
 *
 * @param timeLabel  Text shown on the fence sign (e.g. "11:25:03").
 * @param isJumping  Whether the sheep should loop its jump animation.
 */
@Composable
fun SheepJumpingAnimation(
    timeLabel: String,
    isJumping: Boolean,
    modifier: Modifier = Modifier
) {
    // progress: 0f = left of fence, 0.5f = peak over fence, 1f = right of fence
    val jumpProgress = remember { Animatable(0f) }

    LaunchedEffect(isJumping) {
        if (isJumping) {
            while (isActive) {
                // Parabolic arc: left → over fence → right
                jumpProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = LinearEasing)
                )
                delay(80L)
                jumpProgress.snapTo(0f)  // teleport back to left
                delay(350L)              // brief pause before next jump
            }
        } else {
            jumpProgress.snapTo(0f)
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
        ) {
            // A fixed design space keeps the arc and fence proportional on every density.
            val scaleX = size.width / 360f
            val scaleY = size.height / 220f
            withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {
            val w = 360f
            val h = 220f
            val groundY = h * 0.78f

            // ── Ground ─────────────────────────────────────────────
            drawRect(
                color = Color(0xFF388E3C),
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
            // Grass top strip
            drawRect(
                color = Color(0xFF66BB6A),
                topLeft = Offset(0f, groundY - 6f),
                size = Size(w, 10f)
            )

            // ── Fence (center) ──────────────────────────────────────
            val fenceCenterX = w * 0.5f
            drawFenceWithSign(
                centerX = fenceCenterX,
                groundY = groundY,
                timeLabel = timeLabel,
                textMeasurer = textMeasurer
            )

            // ── Sheep position via parabola ─────────────────────────
            val p = jumpProgress.value
            val startX = w * 0.13f
            val endX   = w * 0.87f
            val sheepX = startX + (endX - startX) * p

            // Parabola: y = -4 * maxHeight * p * (p - 1)  (peak at p=0.5)
            val maxJumpPx = h * 0.55f
            val sheepLiftY = -4f * maxJumpPx * p * (p - 1f)
            val sheepBaseY = groundY - sheepLiftY

            withTransform({
                translate(sheepX, sheepBaseY)
                scale(0.65f, 0.65f, pivot = Offset.Zero)
            }) { drawSheep(centerX = 0f, baseY = 0f, facingRight = true) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private drawing helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawFenceWithSign(
    centerX: Float,
    groundY: Float,
    timeLabel: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val fenceColor = Color(0xFF6D4C41)
    val postW  = 8f
    val postH  = 55f
    val railW  = 160f
    val railH  = 6f
    val postTop = groundY - postH

    val left  = centerX - railW / 2f
    val right = centerX + railW / 2f

    // Posts
    drawRect(fenceColor, Offset(left - postW / 2, postTop), Size(postW, postH))
    drawRect(fenceColor, Offset(right - postW / 2, postTop), Size(postW, postH))

    // Rails
    drawRect(fenceColor, Offset(left, postTop + 8f), Size(railW, railH))
    drawRect(fenceColor, Offset(left, postTop + 35f), Size(railW, railH))

    // Sign board
    val signW = 140f
    val signH = 30f
    val signLeft = centerX - signW / 2f
    val signTop  = postTop - signH - 6f

    drawRect(Color(0xFFFFF9C4), Offset(signLeft, signTop), Size(signW, signH))
    drawRect(
        color = Color(0xFFF9A825),
        topLeft = Offset(signLeft, signTop),
        size = Size(signW, signH),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )

    // Time label on sign
    val measured = textMeasurer.measure(
        text = timeLabel,
        style = TextStyle(color = Color(0xFF1A237E), fontSize = (20f / density / fontScale).sp)
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            centerX - measured.size.width / 2f,
            signTop + signH / 2f - measured.size.height / 2f
        )
    )
}

private fun DrawScope.drawSheep(centerX: Float, baseY: Float, facingRight: Boolean) {
    val dir = if (facingRight) 1f else -1f

    // ── Body (fluffy cloud shape) ──────────────────────────────────
    drawOval(
        color = Color(0xFFF5F5F5),
        topLeft = Offset(centerX - 50f, baseY - 50f),
        size = Size(100f, 56f)
    )
    // Fluff bumps
    listOf(-28f, -10f, 10f, 28f).forEach { xOff ->
        drawCircle(Color(0xFFEEEEEE), radius = 22f, center = Offset(centerX + xOff, baseY - 53f))
    }

    // ── Head ──────────────────────────────────────────────────────
    val headX = centerX + dir * 50f
    drawCircle(Color(0xFF616161), radius = 21f, center = Offset(headX, baseY - 44f))

    // Eye
    drawCircle(Color.White, radius = 5f, center = Offset(headX + dir * 6f, baseY - 48f))
    drawCircle(Color.Black, radius = 2.5f, center = Offset(headX + dir * 7f, baseY - 48f))

    // Ear
    val earPath = Path().apply {
        moveTo(headX + dir * 5f, baseY - 63f)
        lineTo(headX - dir * 6f, baseY - 75f)
        lineTo(headX + dir * 16f, baseY - 72f)
        close()
    }
    drawPath(earPath, color = Color(0xFF757575))

    // ── Legs ──────────────────────────────────────────────────────
    val legColor = Color(0xFF616161)
    val legW = 10f
    val legH = 28f
    listOf(-28f, -10f, 10f, 28f).forEach { xOff ->
        drawRect(legColor, Offset(centerX + xOff - legW / 2, baseY - 6f), Size(legW, legH))
        // Hoof
        drawRect(Color(0xFF212121), Offset(centerX + xOff - legW / 2, baseY + legH - 6f), Size(legW, 8f))
    }

    // ── Tail ──────────────────────────────────────────────────────
    drawCircle(Color(0xFFEEEEEE), radius = 13f, center = Offset(centerX - dir * 54f, baseY - 32f))
}
