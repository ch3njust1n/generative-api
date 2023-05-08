package co.rikin.geepee

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import co.rikin.geepee.ui.theme.Bittersweet
import co.rikin.geepee.ui.theme.Eerie
import co.rikin.geepee.ui.theme.GeePeeTheme
import co.rikin.geepee.ui.theme.Onyx
import co.rikin.geepee.ui.theme.PeachYellow
import co.rikin.geepee.ui.theme.Sage
import co.rikin.geepee.ui.theme.Space
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {

    WindowCompat.setDecorFitsSystemWindows(window, false)

    super.onCreate(savedInstanceState)
    setContent {
      App()
    }
  }
}


@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalPermissionsApi::class
)
@Composable
fun App() {

  fun Context.canLaunchIntent(intent: Intent): Boolean {
    return packageManager.queryIntentActivities(intent, 0).isNotEmpty()
  }

  val context = LocalContext.current

  val viewModel = viewModel<AppViewModel>(
    factory = AppViewModelFactory(
      speechToText = RealSpeechToText(context),
      context = context
    )
  )

  val uri = createImageUri(context)

  val permissionsState = rememberMultiplePermissionsState(
    permissions = buildList {
      add(Manifest.permission.CAMERA)
      add(Manifest.permission.RECORD_AUDIO)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        add(Manifest.permission.QUERY_ALL_PACKAGES)
      }
    },
    onPermissionsResult = {}
  )

  if (!permissionsState.allPermissionsGranted) {
    SideEffect {
      permissionsState.launchMultiplePermissionRequest()
    }
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
        val linkIntent = Intent(ACTION_VIEW)
        // try deeplink first
        if (!command.deeplink.isNullOrEmpty()) {
          Log.d("AppCommand Navigation", "Trying deeplink: ${command.deeplink}")
          linkIntent.data = Uri.parse(command.deeplink)
          try {
            appLauncher.launch(linkIntent)
            return@LaunchedEffect
          } catch (_: ActivityNotFoundException) {
            Log.d("AppCommand", "Launching ${command.deeplink} failed")
          }
        }

        // try url next
        if (!command.url.isNullOrEmpty()) {
          Log.d("AppCommand Navigation", "Trying url: ${Uri.parse(command.url)}")
          linkIntent.data = Uri.parse(command.url)
          try {
            appLauncher.launch(linkIntent)
            return@LaunchedEffect
          } catch (_: ActivityNotFoundException) {
            Log.d("AppCommand", "Launching ${Uri.parse(command.url)} failed")
          }
        }

        // try package method last
        context
          .packageManager
          .getLaunchIntentForPackage(command.appId)
          ?.apply {
            type = "image/jpg"
            setPackage(command.appId)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, command.description)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          ?.also { intent ->
            Log.d("AppCommand Navigation", "Trying package: ${command.appId}")
            try {
              appLauncher.launch(intent)
              return@LaunchedEffect
            } catch (_: ActivityNotFoundException) {
              Log.d("AppCommand", "Launching ${command.appId} failed")
            }
          }

        // if we reached here the action failed, we should still advance the queue
        viewModel.action(AppAction.Advance)
      }

      is Command.SystemCommand -> {
        when (command.peripheral) {
          Peripheral.Camera -> {
            photoLauncher.launch(uri)
          }
        }
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
        Box(
          modifier = Modifier
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


@Composable
fun SpeechBubble(display: PromptDisplay) {
  val textColor = when (display) {
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
