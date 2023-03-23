package co.rikin.geepee

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.rikin.geepee.ui.InitialPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AppViewModel : ViewModel() {
  var state by mutableStateOf(AppState(initializing = true))

  init {
    action(AppAction.InitialSetup())
  }

  fun action(action: AppAction) {
    Log.d("Actions", action.toString())
    when (action) {
      is AppAction.InitialSetup -> {
        val initialMessage = ChatMessage(
          role = "system",
          content = InitialPrompt
        )
        val initialList = listOf(initialMessage)

        state = state.copy(initializing = false, promptQueue = initialList)

        viewModelScope.launch(Dispatchers.IO) {
          val response = GptClient.service.chat(
            ChatRequest(
              messages = initialList
            )
          )

          val message = response.choices.first().message

          state = state.copy(
            promptQueue = state.promptQueue.toMutableList().apply {
              add(message)
              toList()
            }
          )
        }
      }

      is AppAction.Submit -> {
        state = state.copy(
          promptQueue = state.promptQueue.toMutableList().apply {
            add(
              ChatMessage(
                role = "user",
                content = action.prompt
              )
            )
            toList()
          },
        )

        viewModelScope.launch(Dispatchers.IO) {
          val response = GptClient.service.chat(
            ChatRequest(
              messages = state.promptQueue
            )
          )

          val message = response.choices.first().message
          Log.d("GeePee", message.content)
          val apiActions = Json.decodeFromString<ApiActions>(message.content)
          val commands = apiActions.actions.map { action ->
            when(action.component) {
              "camera" -> {
                Command.SystemCommand(
                  peripheral = Peripheral.Camera,
                  description = action.action
                )
              }
              "app" -> {
                if(action.appId != null) {
                  Command.AppCommand(
                    appId = action.appId,
                    deeplink = "",
                    description = action.parameters ?: ""
                  )
                } else {
                  Command.UnsupportedCommand
                }
              }
              else -> {
                Command.UnsupportedCommand
              }
            }
          }

          state = state.copy(
            promptQueue = state.promptQueue.toMutableList().apply {
              add(message)
              toList()
            },
            commandQueue = commands
          )
        }
      }

      is AppAction.UpdatePrompt -> {
        state = state.copy(
          currentPrompt = action.text
        )
      }

      AppAction.ClearCommands -> {
        state = state.copy(
          commandDisplay = emptyList()
        )
      }

      AppAction.OpenCamera -> {

      }

      AppAction.SendToTwitter -> {

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
  val initializing: Boolean = false,
  val promptQueue: List<ChatMessage> = emptyList(),
  val commandDisplay: List<Command> = emptyList(),
  val commandQueue: List<Command> = emptyList(),
  val currentPrompt: String = ""
)

sealed class AppAction {
  class InitialSetup() : AppAction()
  class UpdatePrompt(val text: String) : AppAction()
  class Submit(val prompt: String) : AppAction()
  object ClearCommands : AppAction()
  object OpenCamera : AppAction()
  object SendToTwitter : AppAction()
  object Advance : AppAction()
}

@Serializable
data class ApiActions(
  val actions: List<ApiAction>
)

@Serializable
data class ApiAction(
  val component: String,
  val action: String,
  val subcomponent: String? = null,
  val parameters: String? = null,
  @SerialName("app_id")
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

  object UnsupportedCommand: Command("This command is currently not supported")
}

enum class Peripheral {
  Camera,
  ScreenRecorder
}
