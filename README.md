This project is a fork of librespot-android on Github.
This is a demo application to demonstrate that it is possible to run [librespot-java](https://github.com/librespot-org/librespot-java) on an Android device.
The app provides basic functionalities to login to XShore Dev account, pause/resume, skip next and previous, volume control.

The following modifications has been added as initial base commit:
* Update Gradle build to latest version.
* Spotify Session and Player are created as background service instead of main activity.
* Prepare boot event receiver to start the background MainService at boot time.
* Disable TremoloVorbisDecoder as it caused crashes on the ARM Targets including my own Samsung S21Ultra.

Future feature to be completed:
* ZeroConf Spotify Connect to be added.
* Handle login with user account from UI side.
* Tweak the player features in terms of bitrate, mixing and gapless playback.