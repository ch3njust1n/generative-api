package co.rikin.geepee

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraAccessException
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import co.rikin.geepee.ui.theme.Bittersweet
import co.rikin.geepee.ui.theme.Eerie
import co.rikin.geepee.ui.theme.GeePeeTheme
import co.rikin.geepee.ui.theme.Onyx
import co.rikin.geepee.ui.theme.PeachYellow
import co.rikin.geepee.ui.theme.Sage
import co.rikin.geepee.ui.theme.Space
import co.rikin.geepee.Logger

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {

    WindowCompat.setDecorFitsSystemWindows(window, false)

    super.onCreate(savedInstanceState)
    setContent {
      App()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
  val context = LocalContext.current
  val logger = remember { Logger(context) }

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

  val uri = createImageUri(context)

  val viewModel = viewModel<AppViewModel>(
    factory = AppViewModelFactory(
      speechToText = RealSpeechToText(context),
      context = context
    )
  )

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) {
    viewModel.action(AppAction.Advance)
  }


  val appLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    viewModel.action(AppAction.Advance)
  }

  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    if (success) {
      Log.d("GeePee", uri.toString())
    }
    viewModel.action(AppAction.Advance)
  }

  LaunchedEffect(viewModel.state.commandQueue) {
    if (viewModel.state.commandQueue.isEmpty()) return@LaunchedEffect
    when (val command = viewModel.state.commandQueue.first()) {
      is Command.AppCommand -> {
        if (!command.deeplink.isNullOrEmpty()) {
          val intent = Intent(ACTION_VIEW, Uri.parse(command.deeplink))
          appLauncher.launch(intent)
        } else {
          val intent = Intent().apply {
            type = "image/jpg"
            setPackage(command.appId)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, command.description)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          appLauncher.launch(intent)
        }
      }

      is Command.SystemCommand -> {
        when (command.peripheral) {
          Peripheral.Camera, Peripheral.FrontCamera, Peripheral.BackCamera -> {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val useFrontCamera = command.peripheral == Peripheral.FrontCamera
                logger.logToFile("Camera", useFrontCamera.toString())
                val cameraIntent = getCameraIntent(context, uri, useFrontCamera)
                photoLauncher.launch(uri)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
          }

          Peripheral.ScreenRecorder -> {

          }
        }
      }

      is Command.AskCommand -> {
          // Add your logic here to handle the AskCommand
          logger.logToFile("GeePee", "AskCommand")
          viewModel.action(AppAction.Advance)
      }

      Command.UnsupportedCommand -> {
        viewModel.action(AppAction.Advance)
      }
    }
  }

  GeePeeTheme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(color = Eerie)
        .windowInsetsPadding(insets = WindowInsets.systemBars)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      ) {
        items(viewModel.state.promptDisplay) { message ->
          SpeechBubble(
            display = message
          )
        }

      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextField(
          modifier = Modifier.weight(1f),
          value = viewModel.state.currentPrompt,
          shape = RoundedCornerShape(8.dp),
          colors = TextFieldDefaults.textFieldColors(
            textColor = PeachYellow,
            cursorColor = PeachYellow,
            containerColor = Onyx,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          onValueChange = { viewModel.action(AppAction.UpdatePrompt(it)) }
        )
        Box(modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .pointerInput(Unit) {
            detectTapGestures(onPress = {
              viewModel.action(AppAction.StartRecording)
              awaitRelease()
              viewModel.action(AppAction.StopRecording)
            })
          }, contentAlignment = Alignment.Center
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_microphone),
            tint = Bittersweet,
            contentDescription = "Speak"
          )
        }
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable {
              viewModel.action(AppAction.Submit(viewModel.state.currentPrompt))
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_chat),
            tint = Sage,
            contentDescription = "Send"
          )
        }
      }
    }
  }
}

fun getCameraId(context: Context, useFrontCamera: Boolean): String? {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraFacing = if (useFrontCamera) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK

    try {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                return cameraId
            }
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }
    return null
}

fun getCameraIntent(context: Context, uri: Uri?, useFrontCamera: Boolean): Intent {
    val cameraId = getCameraId(context, useFrontCamera)
    return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        if (cameraId != null) {
            putExtra("android.intent.extras.CAMERA_FACING", cameraId)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

@Preview
@Composable
fun AppPlayground() {
  App()
}

@Composable
fun SpeechBubble(display: PromptDisplay) {
  val textColor = when(display) {
    is PromptDisplay.System -> {
      Sage
    }
    is PromptDisplay.User -> {
      PeachYellow
    }
  }

  Box(
    modifier = Modifier
      .wrapContentSize()
      .background(
        color = Space,
        shape = RoundedCornerShape(16.dp)
      )
      .padding(16.dp)
  ) {
    Text(
      text = display.content,
      color = textColor
    )
  }
}

@Preview
@Composable
fun SpeechBubblePreview() {
  GeePeeTheme {
    Column {
      SpeechBubble(display = PromptDisplay.User("Take a picture"))
    }
  }
}
