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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import co.rikin.geepee.ui.theme.GeePeeTheme

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
  val appLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {}
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) {}

  val viewModel = viewModel<AppViewModel>()
  val state = viewModel.state

  SideEffect {
    state.commandQueue.forEach { command ->
      when (command) {
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
  }

  GeePeeTheme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start
      ) {
        state.commandDisplay.forEach { command ->
          SpeechBubble(content = command.description)
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(onClick = { viewModel.action(AppAction.OpenCamera) }) {
          Text("\"Take a picture\"")
        }
        Button(onClick = { viewModel.action(AppAction.SendToTwitter) }) {
          Text("\"Share that to Twitter\"")
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
        shape = RoundedCornerShape(
          topStartPercent = 50,
          topEndPercent = 50,
          bottomStartPercent = 0,
          bottomEndPercent = 50
        )
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
