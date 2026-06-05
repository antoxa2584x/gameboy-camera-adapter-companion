package ua.retrogaming.gcac.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor

@Composable
fun GreenButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SecondaryBackgroundColor,
    contentColor: Color = Color.White,
    fillMaxWidth: Boolean = true,
    enabled: Boolean = true,
    icon: Painter? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = if (fillMaxWidth) modifier.fillMaxWidth().height(48.dp) else modifier.height(48.dp),
        colors = ButtonColors(
            containerColor,
            contentColor,
            containerColor,
            contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text,
            fontSize = 8.sp,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = PressStart2P
            )
        )
    }
}
