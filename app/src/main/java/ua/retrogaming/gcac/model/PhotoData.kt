package ua.retrogaming.gcac.model
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoData (
    val path: String = "",
    val created: Long = System.currentTimeMillis(),
    val filter: String = "",
) : Parcelable