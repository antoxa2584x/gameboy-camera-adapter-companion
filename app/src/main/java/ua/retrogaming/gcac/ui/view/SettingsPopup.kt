package ua.retrogaming.gcac.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.data.prefs.DeviceData
import ua.retrogaming.gcac.data.serial.LedSerialClient
import ua.retrogaming.gcac.ui.component.CloseButton
import ua.retrogaming.gcac.ui.component.GreenButton
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor


class SettingsPopup : KoinComponent {

    private val ledSerialClient: LedSerialClient by inject()
    val ledStatus = DeviceData.ledStatus

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Render(onCloseClick: () -> Unit) {
        if (ledStatus == null)
            return

        var selectedColor by remember { mutableStateOf(try { Color(ledStatus.hex.toColorInt()) } catch (_: Exception) { Color.Green }) }

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = AbsoluteAlignment.CenterRight
                ) {
                    CloseButton().Render(onCloseClick)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    colors = CardDefaults.cardColors(
                        containerColor = BackgroundColor,
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        LedSettingsView(
                            selectedColor, ledStatus.useRgb,
                            { color, useRgb ->
                                ledSerialClient.setLedColor(color, useRgb)
                            })

                        Spacer(modifier = Modifier.height(16.dp))

                        LanguageSwitch()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    stringResource(R.string.adapter_version, DeviceData.deviceVersion.toString()),
                    Modifier.padding(4.dp),
                    fontSize = 8.sp,
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
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val controller = rememberColorPickerController()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.choose_led_color),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(4.dp),
                controller = controller,
                onColorChanged = { colorEnvelope: ColorEnvelope ->
                    selectedColor = colorEnvelope.color
                }

            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(1.dp), // square corners
                        color = Color.Transparent
                    ) {
                        Switch(
                            modifier = Modifier.height(40.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,   // knob
                                checkedTrackColor = SecondaryBackgroundColor, // track (a Material green)
                                checkedBorderColor = BackgroundColor,
                                uncheckedThumbColor = BackgroundColor,
                                uncheckedTrackColor = Color("#cccccc".toColorInt())
                            ),
                            checked = useRgb,
                            onCheckedChange = { useRgb = it }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.led_color_hint),
                            color = Color.White,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GreenButton().Render(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    text = stringResource(R.string.set_led_color),
                    onClick = {
                        onSetColor(selectedColor, useRgb)
                    }
                )
            }
        }
    }

    @Composable
    fun LanguageSwitch() {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.language_settings),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            val currentLang = DeviceData.language

            Row(
                modifier = Modifier
                    .widthIn(min = 180.dp)
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageOption(
                    text = stringResource(R.string.lang_en),
                    isSelected = currentLang == "en",
                    onClick = {
                        if (DeviceData.language != "en") {
                            DeviceData.language = "en"
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.White)
                )
                LanguageOption(
                    text = stringResource(R.string.lang_uk),
                    isSelected = currentLang == "uk",
                    onClick = {
                        if (DeviceData.language != "uk") {
                            DeviceData.language = "uk"
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun LanguageOption(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(if (isSelected) SecondaryBackgroundColor else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Gray,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }

}