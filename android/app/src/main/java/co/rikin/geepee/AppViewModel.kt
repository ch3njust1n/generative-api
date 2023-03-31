package co.rikin.geepee

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.rikin.geepee.PromptDisplay.System
import co.rikin.geepee.PromptDisplay.User
import co.rikin.geepee.ui.InitialPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AppViewModel(private val speechToText: SpeechToText) : ViewModel() {
  var state by mutableStateOf(AppState(initializing = true))

  init {
    action(AppAction.InitialSetup)
    viewModelScope.launch {
      with(speechToText) {
        speech.collect { text ->
          state = state.copy(
            currentPrompt = state.currentPrompt + text
          )
        }
      }
    }
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

        state = state.copy(
          initializing = true,
          promptQueue = initialList,
          promptDisplay = listOf(
            System(
              "✨ Getting things ready..."
            )
          )
        )

        viewModelScope.launch(Dispatchers.IO) {
          val response = GptClient.service.chat(
            ChatRequest(
              messages = initialList
            )
          )

          val message = response.choices.first().message

          state =
            state.copy(
              initializing = false,
              promptQueue = state.promptQueue.toMutableList().apply {
                add(message)
                toList()
              },
              promptDisplay = listOf(
                System(
                  "👋🏽 How can I help?"
                )
              )
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
          promptDisplay = state.promptDisplay.toMutableList().apply {
            add(
              User(action.prompt),
            )
            add(
              System("💬 Working on it..."),
            )
            toList()
          },
          currentPrompt = ""
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
            when (action.component) {
              "camera" -> {
                Command.SystemCommand(
                  peripheral = Peripheral.Camera,
                  description = action.action
                )
              }

              "app" -> {
                if (action.appPackage != null) {
                  Command.AppCommand(
                    appId = action.appPackage,
                    deeplink = action.parameters?.deeplink,
                    url = action.parameters?.url,
                    description = action.action
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
            promptDisplay = state.promptDisplay.dropLast(1),
            commandQueue = commands
          )
        }
      }

      is AppAction.UpdatePrompt -> {
        state = state.copy(
          currentPrompt = action.text
        )
      }

      AppAction.Advance -> {
        state = state.copy(
          commandQueue = state.commandQueue.drop(1)
        )
      }

      is AppAction.Recording -> {
        state = state.copy(
          currentPrompt = state.currentPrompt + action.text
        )
      }

      AppAction.StartRecording -> {
        speechToText.start()
      }

      AppAction.StopRecording -> {
        speechToText.stop()
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(private val speechToText: SpeechToText) :
  ViewModelProvider.NewInstanceFactory() {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return AppViewModel(speechToText) as T
  }
}

data class AppState(
  val initializing: Boolean = false,
  val promptQueue: List<ChatMessage> = emptyList(),
  val promptDisplay: List<PromptDisplay> = emptyList(),
  val commandDisplay: List<Command> = emptyList(),
  val commandQueue: List<Command> = emptyList(),
  val currentPrompt: String = ""
)

sealed class PromptDisplay(val content: String) {
  class User(content: String): PromptDisplay(content)
  class System(content: String): PromptDisplay(content)
}

sealed class AppAction {
  object InitialSetup : AppAction()
  class UpdatePrompt(val text: String) : AppAction()
  class Submit(val prompt: String) : AppAction()
  object Advance : AppAction()
  object StartRecording : AppAction()
  object StopRecording : AppAction()
  class Recording(val text: String) : AppAction()
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
  @SerialName("package") val appPackage: String? = null,
  @SerialName("parameters") val parameters: ActionParameters? = null,
)

@Serializable
data class ActionParameters(
  val deeplink: String? = null,
  val url: String? = null,
  val content: String? = null,
  @SerialName("phone_number") val phoneNumber: String? = null
)

sealed class Command(
  open val description: String,
) {
  data class AppCommand(
    val appId: String,
    val deeplink: String?,
    val url: String?,
    override val description: String,
  ) : Command(description)

  data class SystemCommand(
    val peripheral: Peripheral,
    override val description: String,
  ) : Command(description)

  object UnsupportedCommand : Command("This command is currently not supported")
}

enum class Peripheral {
  Camera, ScreenRecorder
}
