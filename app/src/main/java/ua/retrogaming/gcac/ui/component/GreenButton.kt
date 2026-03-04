package ua.retrogaming.gcac.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor

class GreenButton {
    @Composable
    fun Render(
        modifier: Modifier = Modifier,
        text: String = "",
        containerColor: Color = SecondaryBackgroundColor,
        contentColor: Color = Color.White,
        fillMaxWidth: Boolean = true,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = if (fillMaxWidth) modifier.fillMaxWidth().height(48.dp) else modifier.height(48.dp),
            colors = ButtonColors(
                containerColor,
                contentColor,
                containerColor,
                contentColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
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
}