package ua.retrogaming.gcac.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.helper.LedSerialClient
import ua.retrogaming.gcac.prefs.DevicePrefs
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor
import ua.retrogaming.gcac.view.component.CloseButton
import ua.retrogaming.gcac.view.component.GreenButton


class LedPopup : KoinComponent {

    private val ledSerialClient: LedSerialClient by inject()
    val ledStatus = DevicePrefs.ledStatus

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Render(onCloseClick: () -> Unit) {
        if (ledStatus == null)
            return

        var selectedColor by remember { mutableStateOf(Color(ledStatus.hex.toColorInt())) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = AbsoluteAlignment.CenterRight
                ) {
                    CloseButton().Render(onCloseClick)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = BackgroundColor,
                    )
                ) {
                    LedSettingsView(
                        selectedColor, ledStatus.useRgb,
                        { color, useRgb ->
                            ledSerialClient.setLedColor(color, useRgb)
                        })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Adapter version: v"+DevicePrefs.deviceVersion,
                    Modifier.padding(10.dp),
                    fontSize = 12.sp,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center
                )

            }
        }
    }

    @Composable
    fun LedSettingsView(
        initialColor: Color = Color(0xFF00FF00),
        initialUseRgb: Boolean = true,
        onSetColor: (Color, Boolean) -> Unit
    ) {
        var selectedColor by remember { mutableStateOf(initialColor) }
        var useRgb by remember { mutableStateOf(initialUseRgb) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val controller = rememberColorPickerController()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose LED color",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(10.dp),
                controller = controller,
                onColorChanged = { colorEnvelope: ColorEnvelope ->
                    selectedColor = colorEnvelope.color
                }

            )

            Spacer(modifier = Modifier.height(16.dp))

            // Switch row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(1.dp), // square corners
                    color = Color.Transparent
                ) {
                    Switch(
                        modifier = Modifier.height(48.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundColor,   // knob
                            checkedTrackColor = SecondaryBackgroundColor, // track (a Material green)
                            checkedBorderColor = BackgroundColor,
                            uncheckedThumbColor = BackgroundColor,
                            uncheckedTrackColor = Color("#cccccc".toColorInt())
                        ),
                        checked = useRgb,
                        onCheckedChange = { useRgb = it }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    fontSize = 12.sp,
                    text = "If color not as expected, switch this",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GreenButton().Render("Set LED Color", {
                onSetColor(selectedColor, useRgb)
            })
        }
    }

}