# Generative APIs with Large Language Models

Join us on [Discord](https://discord.com/invite/y934qSHE).

[Model README](backend/model/README.md)

[Backend README](backend/README.md)

Check out our blog on [Generative APIs with Large Language Models](https://medium.com/@ch3njust1n/generative-apis-with-large-language-models-987108f52d1f) for more details on how we integrated LLM to create this LUI.


### Initial LLM-powered LUI with Text demo:
https://user-images.githubusercontent.com/3211697/228732388-f92f3da0-3e1a-4d8b-ac9d-c8ac9fd2e514.mp4

### Speech-to-text LLM-powered LUI demo:
https://user-images.githubusercontent.com/3211697/229305091-a363b114-dd13-4d8b-8713-b09c000b3997.mp4


### API 
* Initially generated with the help of GPT-3.5 and further refined by GPT-4.


### LLM-to-Mobile Control API

Command syntax: 

```
<component>-<subcomponent>-<action>-<parameters>
```

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

### Example prompt to OpenAI API
Note that the object should be stringified before being sent.

```json
{
  "command": "take a selfie and send it to X and upload it to Instagram and Twitter",
  "state": {
    "device": {
      "model": "Phone Model",
      "os_version": "OS Version",
      "hardware_specs": "Hardware Specifications"
    },
    "battery": {
      "level": "Battery Level",
      "charging_status": "Charging Status"
    },
    "connectivity": {
      "wifi_status": "Wi-Fi Status",
      "cellular_status": "Cellular Status",
      "signal_strength": "Signal Strength",
      "network_type": "Network Type"
    },
    "bluetooth": {
      "status": "Bluetooth Status",
      "paired_devices": "List of Paired Devices"
    },
    "active_apps": [
      "List of Currently Running Apps"
    ],
    "contacts": [
      "List of Contacts"
    ],
    "permissions": [
      "List of App Permissions"
    ],
    "files": [
      "List of File Paths and Metadata"
    ],
    "notifications": [
      "List of Recent Notifications"
    ]
  }
}

```

### Example JSON response to mobile interface

```json
{
  "actions": [
    {
      "component": "camera",
      "subcomponent": "cam-fr",
      "action": "photo",
      "parameters": "<photo-params>"
    },
    {
      "component": "text",
      "action": "send",
      "parameters": "<phone-number>-<photo>"
    },
    {
      "component": "app",
      "app_id": "com.instagram.android",
      "action": "upload-photo",
      "parameters": "<photo>"
    },
    {
      "component": "app",
      "app_id": "com.twitter.android",
      "action": "upload-photo",
      "parameters": "<photo>"
    }
  ]
}

```
