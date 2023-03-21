package co.rikin.geepee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder

class AppViewModel : ViewModel() {
  var state by mutableStateOf(AppState())

  fun action(action: AppAction) {
    when (action) {
      is AppAction.Submit -> {
        viewModelScope.launch(Dispatchers.IO) {
          val response = GptClient.service.chat(
            ChatRequest(
              messages = listOf(
                ChatMessage(
                  role = "user",
                  content = action.prompt
                )
              )
            )
          )

          val content = response.choices.first().message.content

          val commands = listOf(
            Command(
              appId = "com.twitter.android",
              deeplink = "https://twitter.com/intent/tweet?text=${
                URLEncoder.encode(
                  content,
                  "UTF-8"
                )
              }"
            )
          )

          state = state.copy(
            display = content,
            commands = commands
          )
        }
      }
      is AppAction.UpdatePrompt -> {
        state = state.copy(
          prompt = action.text
        )
      }

      AppAction.ClearCommands -> {
        state = state.copy(
          commands = emptyList()
        )
      }
    }
  }
}

data class AppState(
  val display: String = "Sup?",
  val prompt: String = "",
  val commands: List<Command> = emptyList()
)

sealed class AppAction {
  class UpdatePrompt(val text: String) : AppAction()
  class Submit(val prompt: String) : AppAction()
  object ClearCommands : AppAction()
}

data class ApiAction(
  val component: String,
  val action: String,
  val subcomponent: String? = null,
  val parameters: String? = null,
  val appId: String? = null
)

data class Command(
  val appId: String,
  val deeplink: String,
  val extra: String? = null
)
