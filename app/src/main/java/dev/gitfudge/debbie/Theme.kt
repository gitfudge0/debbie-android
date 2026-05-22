package dev.gitfudge.debbie

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

object DebbieColors {
    val Paper = Color(0xFFF5F3EF)
    val Bone = Color(0xFFFFFCF6)
    val Ink = Color(0xFF191713)
    val Char = Color(0xFF2A2721)
    val Mute = Color(0xFF8A8783)
    val Faint = Color(0xFFD9D6D0)
    val Dust = Color(0xFFECEAE5)
    val Signal = Color(0xFFD94A2B)
    val Sap = Color(0xFF3A6B3F)
    val Amber = Color(0xFFC68A1E)
    val DarkPaper = Color(0xFF12140F)
    val DarkBone = Color(0xFF1A1D17)
    val DarkInk = Color(0xFFF2F1EB)
    val DarkMute = Color(0xFF8F948D)
    val DarkFaint = Color(0xFF2B302B)
    val DarkSignal = Color(0xFFFF8C66)
    val DarkSap = Color(0xFF8ACB87)
    val DarkAmber = Color(0xFFE2B65A)
}

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

@Composable
fun DebbieTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            background = DebbieColors.DarkPaper,
            surface = DebbieColors.DarkBone,
            primary = DebbieColors.DarkSignal,
            onPrimary = DebbieColors.DarkPaper,
            onBackground = DebbieColors.DarkInk,
            onSurface = DebbieColors.DarkInk,
            outline = DebbieColors.DarkFaint,
        )
    } else {
        lightColorScheme(
            background = DebbieColors.Paper,
            surface = DebbieColors.Bone,
            primary = DebbieColors.Signal,
            onPrimary = DebbieColors.Bone,
            onBackground = DebbieColors.Ink,
            onSurface = DebbieColors.Ink,
            outline = DebbieColors.Faint,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.copy(
            bodyLarge = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, lineHeight = 20.sp),
            bodyMedium = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontFamily = JetBrainsMono, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontFamily = JetBrainsMono, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
        ),
        content = {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                Surface(color = colors.background, content = content)
            }
        },
    )
}

// Flat pressed tint + 2dp Signal focus ring, the universal interaction treatment.
// Material's round ripple is off-brand on square surfaces, so every Debbie clickable
// drives this Indication instead. `pressTint` may be transparent for surfaces (like the
// solid-Ink primary button) that signal press by swapping their fill instead of overlaying.
private class DebbieIndication(private val pressTint: Color, private val ring: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        DebbieIndicationNode(interactionSource, pressTint, ring)

    override fun equals(other: Any?) =
        other is DebbieIndication && other.pressTint == pressTint && other.ring == ring

    override fun hashCode() = 31 * pressTint.hashCode() + ring.hashCode()
}

private class DebbieIndicationNode(
    private val interactionSource: InteractionSource,
    private val pressTint: Color,
    private val ring: Color,
) : Modifier.Node(), DrawModifierNode {
    private val pressAlpha = Animatable(0f)
    private val focusAlpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            var focused = false
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }
                launch {
                    pressAlpha.animateTo(if (presses.isNotEmpty()) 1f else 0f, tween(120, easing = LinearOutSlowInEasing))
                }
                launch {
                    focusAlpha.animateTo(if (focused) 1f else 0f, tween(180, easing = LinearOutSlowInEasing))
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (pressTint.alpha > 0f && pressAlpha.value > 0f) {
            drawRect(color = pressTint, alpha = pressAlpha.value)
        }
        drawContent()
        if (focusAlpha.value > 0f) {
            val w = 2.dp.toPx()
            drawRect(
                color = ring,
                alpha = focusAlpha.value,
                topLeft = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
                style = Stroke(width = w),
            )
        }
    }
}

@Composable
fun DebbieButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dark = MaterialTheme.colorScheme.background == DebbieColors.DarkPaper
    val ink = MaterialTheme.colorScheme.onSurface
    val signal = MaterialTheme.colorScheme.primary
    val fill = when {
        !enabled -> ink.copy(alpha = .18f)
        pressed -> if (dark) lerp(ink, MaterialTheme.colorScheme.background, .14f) else DebbieColors.Char
        else -> ink
    }
    val content = if (enabled) MaterialTheme.colorScheme.background else ink.copy(alpha = .4f)
    Row(
        modifier
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = remember(signal) { DebbieIndication(Color.Transparent, signal) },
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(text.uppercase(), color = content, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) }
}

@Composable
fun DebbieOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val dark = MaterialTheme.colorScheme.background == DebbieColors.DarkPaper
    val ink = MaterialTheme.colorScheme.onSurface
    val signal = MaterialTheme.colorScheme.primary
    val accent = if (destructive) {
        if (dark) DebbieColors.DarkSignal else DebbieColors.Signal
    } else {
        ink
    }
    val borderColor = if (enabled) accent.copy(alpha = .8f) else ink.copy(alpha = .35f)
    val content = if (enabled) accent else ink.copy(alpha = .35f)
    val pressTint = ink.copy(alpha = .08f)
    Row(
        modifier
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(0.dp))
            .clickable(
                interactionSource = interaction,
                indication = remember(pressTint, signal) { DebbieIndication(pressTint, signal) },
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(text.uppercase(), color = content, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) }
}

@Composable
fun DebbieInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(0.dp),
        textStyle = LocalTextStyle.current,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
fun DebbieCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(0.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)), RoundedCornerShape(0.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
fun Pill(text: String, tone: StatusTone = StatusTone.Mute, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val dark = MaterialTheme.colorScheme.background == DebbieColors.DarkPaper
    val color = when (tone) {
        StatusTone.Sap -> if (dark) DebbieColors.DarkSap else DebbieColors.Sap
        StatusTone.Amber -> if (dark) DebbieColors.DarkAmber else DebbieColors.Amber
        StatusTone.Signal -> if (dark) DebbieColors.DarkSignal else DebbieColors.Signal
        StatusTone.Mute -> MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
    }
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        tone == StatusTone.Mute -> MaterialTheme.colorScheme.surface
        else -> color.copy(alpha = .11f)
    }
    val animatedBg by animateColorAsState(bg, tween(180, easing = LinearOutSlowInEasing), label = "pill-bg")
    val fg by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else color, tween(180, easing = LinearOutSlowInEasing), label = "pill-fg")
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .8f),
        tween(180, easing = LinearOutSlowInEasing),
        label = "pill-border",
    )
    val pill = Modifier
        .border(1.dp, borderColor, RoundedCornerShape(999.dp))
        .background(animatedBg, RoundedCornerShape(999.dp))
        .padding(horizontal = 12.dp, vertical = 6.dp)
    val label = @Composable {
        Text(text.uppercase(), color = fg, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
    if (onClick == null) {
        Box(pill) { label() }
    } else {
        // Interactive (filter) pill: keep the visual chip small but expand the touch target to 48dp.
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .sizeIn(minHeight = 48.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Box(pill) { label() } }
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    DebbieCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    DebbieCard(modifier) {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontFamily = JetBrainsMono, fontSize = 11.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, maxLines = 1)
    }
}

@Composable
fun ProgressBar(progress: Double, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(180, easing = LinearOutSlowInEasing),
        label = "progress",
    )
    Box(modifier.fillMaxWidth().height(7.dp).clip(shape).background(MaterialTheme.colorScheme.outline.copy(alpha = .7f))) {
        Box(
            Modifier
                .fillMaxWidth(animatedProgress)
                .height(7.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 13.sp)
        Text(value, modifier = Modifier.padding(start = 16.dp), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
    }
}

// Square, flat replacement for Material Switch. Off: empty track, ink thumb left.
// On: Signal track fill, paper thumb right. The Field Instrument has no rounded toggles.
@Composable
fun DebbieToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 2.dp, tween(180), label = "thumb")
    Box(
        modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .width(46.dp)
                .height(26.dp)
                .border(1.dp, ink, RoundedCornerShape(0.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent),
        ) {
            Box(
                Modifier
                    .padding(start = thumbOffset)
                    .align(Alignment.CenterStart)
                    .size(18.dp)
                    .background(if (checked) MaterialTheme.colorScheme.onPrimary else ink),
            )
        }
    }
}

// Square, flat replacement for Material Checkbox. Checked fills with the supplied accent.
@Composable
fun DebbieCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .border(1.dp, if (checked) accent else MaterialTheme.colorScheme.onSurface, RoundedCornerShape(0.dp))
                .background(if (checked) accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text("✓", color = MaterialTheme.colorScheme.background, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
