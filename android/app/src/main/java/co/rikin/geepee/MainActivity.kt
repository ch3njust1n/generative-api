package co.rikin.geepee

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import co.rikin.geepee.ui.theme.GeePeeTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
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
  val viewModel = viewModel<AppViewModel>()
  val state = viewModel.state


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

  LaunchedEffect(state.commandQueue) {
    if (state.commandQueue.isEmpty()) return@LaunchedEffect
    delay(1000)
    when (val command = state.commandQueue.first()) {
      is Command.AppCommand -> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(command.deeplink))
        appLauncher.launch(intent)
      }

      is Command.SystemCommand -> {
        when (command.peripheral) {
          Peripheral.Camera -> {
            if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
              ) == PackageManager.PERMISSION_GRANTED
            ) {
              appLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            } else {
              permissionLauncher.launch(Manifest.permission.CAMERA)
            }
          }

          Peripheral.ScreenRecorder -> {

          }
        }
      }
    }
  }

  GeePeeTheme {
    if (state.initializing) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
      ) {
        Text("Initializing")
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
          horizontalAlignment = Alignment.Start
        ) {
          items(state.promptQueue) { message ->
            SpeechBubble(content = message.content)
          }

        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextField(modifier = Modifier.weight(1f), value = state.currentPrompt, onValueChange = { viewModel.action(AppAction.UpdatePrompt(it)) })
          IconButton(onClick = { viewModel.action(AppAction.Submit(state.currentPrompt)) }) {
            Icon(imageVector = Icons.Rounded.Send, contentDescription = "Send")
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun AppPlayground() {
  App()
}

@Composable
fun SpeechBubble(content: String) {
  Box(
    modifier = Modifier
      .wrapContentSize()
      .background(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp)
      )
      .padding(16.dp)
  ) {
    Text(text = content, color = MaterialTheme.colorScheme.onPrimary)
  }
}

@Preview
@Composable
fun SpeechBubblePreview() {
  GeePeeTheme {
    SpeechBubble("Take a picture")
  }
}
