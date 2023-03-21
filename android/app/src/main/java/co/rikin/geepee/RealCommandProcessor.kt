package co.rikin.geepee

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class RealCommandProcessor(private val appContext: Context): CommandProcessor {
  override fun process(commands: List<Command>): AppAction {
    commands.forEach { command ->
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(command.deeplink)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      appContext.startActivity(intent)
      Log.i("Command Processor", command.toString())
    }

    return AppAction.ClearCommands
  }
}

fun interface CommandProcessor {
  fun process(commands: List<Command>): AppAction
}