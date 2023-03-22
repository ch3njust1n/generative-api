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

          val command = Command.AppCommand(
            appId = "com.twitter.android",
            deeplink = "https://twitter.com/intent/tweet?text=${
              URLEncoder.encode(
                content,
                "UTF-8"
              )
            }",
            description = "Posting to Twitter",
          )

          state = state.copy(
            commandDisplay = state.commandDisplay.toMutableList().apply {
              add(command)
              toList()
            }
          )
        }
      }

      is AppAction.UpdatePrompt -> {
//        state = state.copy(
//          prompt = action.text
//        )
      }

      AppAction.ClearCommands -> {
        state = state.copy(
          commandDisplay = emptyList()
        )
      }

      AppAction.OpenCamera -> {
        val commands = listOf(
          Command.SystemCommand(
            peripheral = Peripheral.Camera,
            description = "Taking a picture",
          ),
          Command.AppCommand(
            appId = "com.twitter.android",
            deeplink = "https://twitter.com/intent/tweet?text=${
              URLEncoder.encode(
                "Check this out!",
                "UTF-8"
              )
            }",
            description = "Posting to Twitter",
          ),
          Command.SystemCommand(
            peripheral = Peripheral.Camera,
            description = "Taking a picture",
          ),
          Command.AppCommand(
            appId = "com.twitter.android",
            deeplink = "https://twitter.com/intent/tweet?text=${
              URLEncoder.encode(
                "Check this out!",
                "UTF-8"
              )
            }",
            description = "Posting to Twitter",
          )
        )

        state = state.copy(
          commandDisplay = state.commandDisplay.toMutableList().apply {
            addAll(commands)
            toList()
          },
          commandQueue = commands
        )
      }

      AppAction.SendToTwitter -> {
        val command = Command.AppCommand(
          appId = "com.twitter.android",
          deeplink = "https://twitter.com/intent/tweet?text=${
            URLEncoder.encode(
              "Check this out!",
              "UTF-8"
            )
          }",
          description = "Posting to Twitter",
        )
        state = state.copy(
          commandDisplay = state.commandDisplay.toMutableList().apply {
            add(command)
          },
          commandQueue = listOf(command)
        )
      }

      AppAction.Advance -> {
        state = state.copy(
          commandQueue = state.commandQueue.drop(1)
        )
      }
    }
  }
}

data class AppState(
  val commandDisplay: List<Command> = emptyList(),
  val commandQueue: List<Command> = emptyList()
)

sealed class AppAction {
  class UpdatePrompt(val text: String) : AppAction()
  class Submit(val prompt: String) : AppAction()
  object ClearCommands : AppAction()
  object OpenCamera : AppAction()
  object SendToTwitter : AppAction()
  object Advance: AppAction()
}

data class ApiAction(
  val component: String,
  val action: String,
  val subcomponent: String? = null,
  val parameters: String? = null,
  val appId: String? = null
)

sealed class Command(
  open val description: String,
) {
  data class AppCommand(
    val appId: String,
    val deeplink: String,
    override val description: String,
    val extra: String? = null,
  ) : Command(description)

  data class SystemCommand(
    val peripheral: Peripheral,
    override val description: String,
  ) : Command(description)
}

enum class Peripheral {
  Camera,
  ScreenRecorder
}
