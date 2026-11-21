# FreeMusic

An ad-free Android music player that streams online from [Jamendo](https://www.jamendo.com),
a catalog of independent artists that is free and legal to stream.

- Search songs, artists and genres, or browse this week's popular tracks
- Background playback with notification, lock-screen and Bluetooth controls (Media3)
- Seek bar, previous/next, album art, light/dark theme
- No ads, no tracking, no account in the app

Not compiled yet: the source was written without access to the Android SDK, so the first
build is the real test. If Gradle reports an error, send it to me and I'll fix it.

## 1. Get a free Jamendo Client ID (2 minutes)

1. Create an account at https://devportal.jamendo.com
2. Create an application there and copy its **Client ID**
3. You paste it into the app the first time you open it (nothing to edit in the code)

## 2. Build the APK

### Option A: no installs, using GitHub (easiest)

1. Create a free GitHub account and a new empty repository.
2. Upload the contents of this folder to it (drag and drop in the browser works,
   including the hidden `.github` folder).
3. Open the repository's **Actions** tab. The "Build APK" workflow runs on every upload
   (about 5 minutes). You can also start it with **Run workflow**.
4. Open the finished run and download the **FreeMusic-apk** artifact. Unzip it to get `app-debug.apk`.

### Option B: Android Studio

1. Install Android Studio and choose **Open** on this folder. Accept the prompts to
   install the SDK and sync Gradle.
2. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. The file is at `app/build/outputs/apk/debug/app-debug.apk`.

### Option C: command line

With JDK 17 and the Android SDK installed: `./gradlew assembleDebug`

## 3. Install on your phone

1. Copy `app-debug.apk` to the phone (USB, cloud drive or messaging app).
2. Open it. When asked, allow **Install unknown apps** for the app you opened it from.
3. Open FreeMusic, paste your Client ID, and play.

## Notes

- The APK is a debug build signed with the standard debug key. It installs and works fine for
  personal use. Publishing on Google Play needs a release build with your own signing key.
- Music comes from Jamendo only. Each track has its own license (mostly Creative Commons).
- Possible next features: playlists and favorites (Room), offline downloads (Media3 download
  manager), shuffle/repeat, a sleep timer.
