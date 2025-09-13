package ua.retrogaming.gcac.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ColorSchemeCircle(
    val name: String,
    val colors: List<Color>
)

@Composable
fun ColorSchemeSelector(
    schemes: List<ColorSchemeCircle>,
    circleSize: Dp = 40.dp,
    spacing: Dp = 12.dp,
    onSchemeSelected: (ColorSchemeCircle) -> Unit
) {
    var selected by remember { mutableStateOf(schemes.firstOrNull()?.name) }

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically

    ) {
        schemes.forEach { scheme ->
            Canvas(
                modifier = Modifier
                    .size(if (selected == scheme.name) circleSize + 4.dp else circleSize)
                    .clip(CircleShape)
                    .clickable {
                        selected = scheme.name
                        onSchemeSelected(scheme)
                    }
                    .border(2.dp, Color.White, CircleShape)
            ) {
                // Linear gradient with 4 segments
                val brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to scheme.colors[0],
                        0.25f to scheme.colors[0],
                        0.25f to scheme.colors[1],
                        0.5f to scheme.colors[1],
                        0.5f to scheme.colors[2],
                        0.75f to scheme.colors[2],
                        0.75f to scheme.colors[3],
                        1.0f to scheme.colors[3]
                    )
                )
                drawRect(brush = brush, size = Size(size.width, size.height))
            }
        }
    }
}