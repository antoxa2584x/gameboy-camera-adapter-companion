package ua.retrogaming.gcac.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.ui.component.ColorSchemeCircle
import ua.retrogaming.gcac.ui.component.ColorSchemeSelector
import ua.retrogaming.gcac.ui.component.GbPaletteImage
import ua.retrogaming.gcac.ui.component.GreenButton
import ua.retrogaming.gcac.ui.theme.DarkRed

private val colorSchemes = listOf(
    ColorSchemeCircle(
        "grayscale",
        listOf(Color(0xFFFFFFFF), Color(0xFFBFBFBF), Color(0xFF7F7F7F), Color(0xFF3F3F3F))
    ),
    ColorSchemeCircle(
        "game-boy",
        listOf(Color(0xFFD0D93C), Color(0xFF78A46A), Color(0xFF545854), Color(0xFF244624))
    ),
    ColorSchemeCircle(
        "super-game-boy",
        listOf(Color(0xFFFFFFFF), Color(0xFFB5B3BD), Color(0xFF545367), Color(0xFF090713))
    ),
    ColorSchemeCircle(
        "game-boy-color-jpn",
        listOf(Color(0xFFF0F0F0), Color(0xFFDAC46A), Color(0xFF705834), Color(0xFF1E1E1E))
    ),
    ColorSchemeCircle(
        "game-boy-color-usa-gold",
        listOf(Color(0xFFF0F0F0), Color(0xFFDCA0A0), Color(0xFF884E4E), Color(0xFF1E1E1E))
    ),
    ColorSchemeCircle(
        "game-boy-color-usa-eur",
        listOf(Color(0xFFF0F0F0), Color(0xFF86C864), Color(0xFF3A6084), Color(0xFF1E1E1E))
    )
)

@Composable
fun PrintingGallery(
    photos: List<PhotoData>,
    colorScheme: String,
    isLandscape: Boolean,
    onSchemeSelected: (String) -> Unit,
    onPhotoClick: (PhotoData) -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.start_printing_hint),
                    Modifier
                        .padding(24.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = 8.sp,
                    color = Color.White
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ColorSchemeSelector(
                    schemes = colorSchemes,
                    selected = colorScheme,
                    onSchemeSelected = { scheme -> onSchemeSelected(scheme.name) }
                )

                ImageGrid(
                    isLandscape = isLandscape,
                    photos = photos,
                    colorScheme = colorScheme,
                    onClick = onPhotoClick,
                    modifier = Modifier.weight(1f),
                    bottomPadding = bottomPadding
                )
            }
        }
    }
}

@Composable
fun GalleryActionButtons(
    showGalleryActions: Boolean,
    onPrintRequest: () -> Unit,
    onSaveAll: () -> Unit,
    onDeleteAllRequest: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GreenButton(
            fillMaxWidth = false,
            text = stringResource(R.string.print_photo),
            icon = painterResource(R.drawable.ic_print),
            onClick = onPrintRequest
        )

        if (showGalleryActions) {
            GreenButton(
                fillMaxWidth = false,
                text = stringResource(R.string.save_all),
                icon = painterResource(R.drawable.ic_save),
                onClick = onSaveAll
            )

            GreenButton(
                fillMaxWidth = false,
                text = stringResource(R.string.remove_all),
                containerColor = DarkRed,
                icon = painterResource(R.drawable.ic_delete),
                onClick = onDeleteAllRequest
            )
        }
    }
}

@Composable
private fun ImageGrid(
    isLandscape: Boolean,
    photos: List<PhotoData>,
    colorScheme: String,
    modifier: Modifier = Modifier,
    onClick: (PhotoData) -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 6 else 3),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 120.dp + bottomPadding),
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = photos,
            key = { it.path } // stable key = file path
        ) { photo ->
            GbPaletteImage(
                data = photo.path,
                scheme = colorScheme,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onClick(photo) }
                    .padding(4.dp),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
