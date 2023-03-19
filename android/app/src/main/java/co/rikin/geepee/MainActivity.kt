package co.rikin.geepee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import co.rikin.geepee.ui.theme.GeePeeTheme
import co.rikin.geepee.ui.theme.PurpleGrey80

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
  val viewModel = viewModel<AppViewModel>()
  val state = viewModel.state

  GeePeeTheme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(state.response, fontSize = 20.sp)
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextField(modifier = Modifier.weight(1f), value = state.prompt, onValueChange = { viewModel.action(AppAction.UpdatePrompt(it)) })
        Box(
          modifier = Modifier
            .size(60.dp)
            .background(color = PurpleGrey80, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable {
               viewModel.action(AppAction.SubmitPrompt(state.prompt))
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = Icons.Rounded.Send, contentDescription = "Send")
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

class AppViewModel : ViewModel() {
  var state by mutableStateOf(AppState())

  fun action(action: AppAction) {
    when (action) {
      is AppAction.SubmitPrompt -> {
        state = state.copy(
          response = "GPT: ${action.prompt}"
        )
      }

      is AppAction.UpdatePrompt -> {
        state = state.copy(
          prompt = action.text
        )
      }
    }
  }
}

data class AppState(
  val response: String = "Go to camera",
  val prompt: String = ""
)

sealed class AppAction {
  class UpdatePrompt(val text: String) : AppAction()
  class SubmitPrompt(val prompt: String) : AppAction()
}