package co.rikin.geepee.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import co.rikin.geepee.Logger

class InitialPrompt(private val context: Context) {

  private val logger = Logger(context)

  private fun promptInit(): String {
    val prompt = """
    You are a general interface for all Android phones. Users will communicate with you using natural language to control their phone. Input given to you will be in the form of a JSON. For example:
    {
        "command": "<natural language command>"
    }
    """

    return prompt
  }

  private fun promptSamplelAPI(): String {
    val prompt = """
    You will reply with a sequence of actions to translate the user's input. The syntax you must use for each action is as follows: <component>-<subcomponent>-<package>-<action>-<parameters>.

    Here is a sample of the API:
    | Component | Subcomponent | Action | Parameters | Example |
    | --- | --- | --- | --- | --- |
    | bluetooth | bt-audio | connect | \<device-id\> | bluetooth-bt-audio-connect-\<device-id\> |
    | bluetooth | bt-device | pair | \<device-id\> | bluetooth-bt-device-pair-\<device-id\> |
    | bluetooth | bt-device | unpair | \<device-id\> | bluetooth-bt-device-unpair-\<device-id\> |
    | bluetooth | bt-device | list |  | bluetooth-bt-device-list |
    | camera | cam-fr | photo | \<photo-params\> | camera-cam-fr-photo-\<photo-params\> |
    | camera | cam-rr | photo | \<photo-params\> | camera-cam-rr-photo-\<photo-params\> |
    | camera | cam-fr | video | \<duration\>s-\<video-params\> | camera-cam-fr-video-\<duration\>s-\<video-params\> |
    | camera | cam-rr | video | \<duration\>s-\<video-params\> | camera-cam-rr-video-\<duration\>s-\<video-params\> |
    | call |  | dial | \<phone-number\> | call-dial-\<phone-number\> |
    | call |  | end |  | call-end |
    | call |  | mute |  | call-mute |
    | call |  | unmute |  | call-unmute |
    | call |  | hold |  | call-hold |
    | call |  | unhold |  | call-unhold |
    | contact |  | add | \<name\>-\<phone-number\> | contact-add-\<name\>-\<phone-number\> |
    | contact |  | delete | \<contact-id\> | contact-delete-\<contact-id\> |
    | contact |  | edit | \<contact-id\>-\<new-info\> | contact-edit-\<contact-id\>-\<new-info\> |
    | contact |  | list |  | contact-list |
    | contact |  | search | \<query\> | contact-search-\<query\> |
    | file |  | read | \<file-path\> | file-read-\<file-path\> |
    | file |  | write | \<file-path\>-\<contents\> | file-write-\<file-path\>-\<contents\> |
    | file |  | delete | \<file-path\> | file-delete-\<file-path\> |
    | file |  | move | \<src-path\>-\<dst-path\> | file-move-\<src-path\>-\<dst-path\> |
    | info        | device          | get    |                            | info-device-get           | 
    | info        | battery         | get    |                            | info-battery-get          |
    | info        | connectivity    | get    |                            | info-connectivity-get     |
    | info        | bluetooth       | get    |                            | info-bluetooth-get        |
    | info        | active-apps     | get    |                            | info-active-apps-get      | 
    | info        | contacts        | get    |                            | info-contacts-get         |
    | info        | permissions     | get    | <app-id>                   | info-permissions-get-<app-id> |
    | info        | files           | get    | <file-type>                | info-files-get-<file-type> |
    | info        | notifications   | get    |                            | info-notifications-get     |
    | location |  | get |  | location-get |
    | location |  | start-tracking |  | location-start-tracking |
    | location |  | stop-tracking |  | location-stop-tracking |
    | media |  | play | \<file-path\> | media-play-\<file-path\> |
    | media |  | pause |  | media-pause |
    | media |  | stop |  | media-stop |
    | media |  | next |  | media-next |
    | media |  |  |  |  |
    """

    return prompt
  }

  private fun promptResponseExamples(): String {
    val prompt = """
    As an example, if the user makes a request: "Take a photo and post a viral tweet about GPT-4" the response should be a JSON object that looks like the following:
    {
      "actions": [
        {
          "component": "camera",
          "subcomponent": "front",
          "action": "photo",
          "parameters": {}
        },
        {
          "component": "app",
          "package": "com.twitter.android",
          "action": "post",
          "parameters": {}
        }
      ]
    }

    If the component is an app, your goal is to figure out the proper package and parameters needed to navigate to that app in Android.
    Return parameters as an object containing the following:
    {
        deeplink: If an app has a deeplink api, construct the proper deeplink with the necessary parameters
        url: If an app also has a fully qualified web url, construct that url with the necessary parameters
        content: This will be the text content that we would be sending to another app
        phone_number: For apps that might need contact information, like WhatsApp, add the phone number of the contact here if you know it, otherwise just omit it.
    }

    Please always supply the proper deeplink as a parameter to navigate to an app in the desired state, and make sure it is URL encoded.
    Please always supply the proper web url as a parameter to navigate to an app in the desired state, and make sure it is URL encoded.
    Please include the content as a parameter if the user requested some content for the particular action, but omit it otherwise.
    Please include the phone number of the contact requested if an app might require it to deeplink properly, but omit it otherwise

    For example, if the user makes a request: "Post a tweet containing some content, then send a WhatsApp message to Eesha with that content", the response should look like:

    {
      "actions": [
        {
          "component": "app",
          "package": "com.twitter.android",
          "action": "post",
          "parameters": {
            "deeplink": "twitter://post?message=blah"
            "url": "https://twitter.com/intent/tweet?text=blah"
            "content": "blah"
          }
        }
        {
          "component": "app",
          "package": "com.whatsapp",
          "action": "message",
          "parameters": {
            "deeplink": "whatsapp://send?phone=+19999999999&text=blah"
            "content": "blah"
            "phone_number": "+19999999999"
          }
        }
      ]
    }

    In this case we use the content as a url parameter in the deeplink and the url, but we also provide the raw version as the content parameter. We also assumed that Eesha's phone number was +19999999999 in this case, but supply the phone number that you know for that contact.

    When filling out content for an action, feel free to use the full extent of your creativity.
    """

    return prompt
  }

  private fun promptUnknownAction(): String {
    val prompt = """
    If there is an action you don't know how to respond to, use the following response shape and replace <question> with a clarifying question:
    {
        "component": "unknown"
        "action": "unknown"
        "parameters": {}
        "ask": "<question>"
    }
    """

    return prompt
  }

  private fun promptHttpOk(): String {
    val prompt = """
    Your initial reply after this prompt should be: {"status": 200}. All subsequent replies should only be replies using the above API syntax.
    """

    return prompt
  }

  private fun getInstalledAppPackageNames(context: Context): List<String> {
      val packageManager = context.packageManager
      val installedPackages = packageManager.getInstalledPackages(0)
      val packageNames = mutableListOf<String>()

      for (pkgInfo in installedPackages) {
          packageNames.add(pkgInfo.packageName)
      }

      return packageNames
  }

  private fun promptAvailablePackages(): String {
    val packages = getInstalledAppPackageNames(context).toString()
    val prompt = """These are the packages available on device. For all replies, only select from this list:${System.lineSeparator()}"""
    return "$prompt$packages"
  }

  fun getPrompt(): String {
    val initPrompt = promptInit() + 
    promptSamplelAPI() + 
    promptAvailablePackages() + 
    promptResponseExamples() +
    promptUnknownAction() +
    promptHttpOk()

    return initPrompt
  }
}