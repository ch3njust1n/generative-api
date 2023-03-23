package co.rikin.geepee

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Scanner

class RealAssetLoader(private val context: Context): AssetLoader {
  override suspend fun loadFile(): String {
    return withContext(Dispatchers.IO) {
      val inputStream = context.assets.open("intial.txt")
      val scanner = Scanner(inputStream)
      val contents = scanner.useDelimiter("\\A").next()
      scanner.close()
      inputStream.close()
      contents
    }
  }
}

interface AssetLoader {
  suspend fun loadFile(): String
}