package ua.retrogaming.gcac.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetSystemBars() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = false // true if your background is light

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = SecondaryBackgroundColor, // your color
            darkIcons = useDarkIcons
        )
        // You can also set them separately:
        // systemUiController.setStatusBarColor(Color.Black)
        // systemUiController.setNavigationBarColor(Color.Black)
    }
}