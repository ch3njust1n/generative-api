package co.rikin.geepee

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun createImageUri(context: Context): Uri? {
  val timestamp = System.currentTimeMillis()
  val contentValues = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, "test_photo_$timestamp.jpg")
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
  }
  return context.contentResolver.insert(
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    contentValues
  )
}
