package ua.retrogaming.gcac.view.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.theme.Red

class CloseButton {

    @Composable
    fun Render( onClick: () -> Unit){
        Button(onClick,
            modifier = Modifier
                .height(40.dp)
                .width(40.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonColors(
                Red,
                Color.White,
                Red,
                Color.White
            )
        ) {
            Text(
                "X", style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PressStart2P
                )
            )
        }
    }
}