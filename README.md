# 📱 FyneCast

<div align="center">

<p>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="UI">
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-6DB33F?style=flat" alt="Architecture">
  <img src="https://img.shields.io/badge/Network-Retrofit-FF5722?style=flat" alt="Retrofit">
  <img src="https://img.shields.io/badge/Database-Room%20%26%20DataStore-FF8800?style=flat" alt="Database">
</p>

**FyneCast** is a native Android weather app, built with **Kotlin**, **Jetpack Compose**, **Retrofit**, **MVVM** and **Clean Architecture**.<br>
Hourly/daily forecasts, weather alerts tracking, favorite cities, a map location picker and full English & Arabic (RTL) support.

</div>

---

## 🚀 About the Project

**FyneCast** fetches location-based forecasts from WeatherAPI through Retrofit, along with stats such as humidity, wind speed, pressure & UV index. Weather loads for your device location (GPS), for a city found with auto-complete search, or for a spot picked on the map. Users sign up with email, Google or Facebook, save favorite cities, and track severe weather alerts. When a tracked alert expires or its conditions lift, the app sends a notification with an alarm-style sound and vibration. Units can be switched between Metric (°C) & Imperial (°F), the theme between light & dark, and the language between English & Arabic.

---

## ✨ Key Features

- **Weather Forecasts**
  - Current conditions, a 24-hour hourly forecast and a 7-day daily forecast.
  - Live local clock for the selected city.
  - Automatic refresh every 15 minutes while the app is open.
  - Refresh when you return to the app.
  - Metric/Imperial units.

- **Location, Search & Map Picker**
  - Device location through Play Services Location.
  - City auto-complete suggestions through the Photon geocoding API.
  - Interactive map picker (MapLibre with OpenFreeMap tiles); map labels follow the app language.
  - City names shown in the app language through Android's built-in `Geocoder`, with Photon as a fallback.

- **Favorites**
  - Save cities with live temperatures, stored with Room and displayed with Paging 3.
  - Temperatures refresh when the tab is opened and whenever the city is viewed.
  - Swipe-to-delete with Undo.

- **Severe Weather Alerts & Notifications**
  - Track any active alert; one periodic `WorkManager` worker per tracked alert checks it in the background.
  - A notification (alarm-style sound and vibration) tells you when an alert expires or its conditions lift.
  - A periodic refresh worker (15 minutes, network required) keeps data current; tracking is restored after a reboot.

- **Authentication & Profile**
  - Email/password sign-up and login + password reset
  - Google and Facebook sign-in.
  - Profile editing with an image picker that downsamples, corrects EXIF orientation and compresses to WebP.
  - Changing the email requires verifying the new address.

- **Localization & Theming**
  - English and Arabic, including right-to-left layouts.
  - The language defaults to the phone's language on first launch.
  - Light and dark themes.

---

## 🛠️ Tech Stack & Libraries

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/), Navigation Compose
- **Architecture:** MVVM with Clean Architecture (feature packages: `auth`, `home`, `favorites`, `alerts`, `map`, `settings`, `core`)
- **Dependency Injection:** manual, through an `AppContainer`. Deliberately to keep the wiring explicit; Hilt is on the roadmap.
- **Concurrency:** [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-guide.html)
- **Local Persistence:** [Room](https://developer.android.com/training/data-storage/room) with [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview), and [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Background Processing:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Networking:** [Retrofit](https://square.github.io/retrofit/) & [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Maps:** [MapLibre Native](https://maplibre.org/) with [OpenFreeMap](https://openfreemap.org/) tiles
- **Location:** Google Play Services Location, Android `Geocoder`
- **Authentication:** [Firebase Auth](https://firebase.google.com/docs/auth), [Credential Manager](https://developer.android.com/training/sign-in/credential-manager) (Google) and the Facebook Login SDK
- **Image Loading:** [Coil 3](https://coil-kt.github.io/coil/)

---

## 🏛️ Architecture & Design Pattern

Each feature is split into layers:

- **Presentation (Compose UI & ViewModels):**<br>
  Compose screens observe state exposed by ViewModels as `StateFlow`.<br>
  One-off events (navigation, messages) travel over channels.
- **Domain (Use Cases, Models & Repository interfaces):**<br>
  Business logic such as loading weather, tracking alerts and updating a profile.<br>
  The favorites feature uses Paging types in its domain contract.
- **Data (Repository implementations, Room DAOs, DTOs & mappers):**<br>
  Wraps the WeatherAPI, Photon, Room, DataStore, Firebase and Android platform services.

An architecture refactor toward stricter layer separation is in progress (see the roadmap below).

---

## 📡 Data Sources & APIs

- **Weather Data:** [WeatherAPI](https://www.weatherapi.com/)
- **City Search (auto-complete):** [Photon Komoot](https://photon.komoot.io/), which only supports English, German and French names
- **Reverse Geocoding & Localized Place Names:** Android `Geocoder` first, Photon as a fallback
- **Map Tiles:** [OpenFreeMap](https://openfreemap.org/)

---

## 📸 Screenshots

<div align="center">

| Home Dashboard | Favorites List | Weather Alerts | App Settings |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/user-attachments/assets/47c0aaa7-aea7-4160-a015-e84db9b7204d" width="200" alt="Home Dashboard" /> | <img src="https://github.com/user-attachments/assets/5ad7d848-6dcb-4413-9c11-6571298bb8b2" width="200" alt="Favorites List" /> | <img src="https://github.com/user-attachments/assets/6ff1d066-a445-4c51-8ee5-b8e5121a605c" width="200" alt="Weather Alerts" /> | <img src="https://github.com/user-attachments/assets/42671196-3e59-497b-97cf-eff3cfcee14a" width="200" alt="App Settings" /> |
| <sub><i>Figure 1:<br>Main Dashboard (Dark Mode/English)</i></sub> | <sub><i>Figure 2:<br>Favorites List (Dark Mode/English)</i></sub> | <sub><i>Figure 3:<br>Weather Alerts (Dark Mode/English)</i></sub> | <sub><i>Figure 4:<br>Settings (Light Mode/Arabic)</i></sub> |

</div>

---

## ⚙️ Getting Started

The app needs a few keys and a Firebase project before it builds and runs.<br>
They are not in the repository, so follow the steps below.

### 📋 Prerequisites

- A recent **Android Studio** that supports the project's Android Gradle Plugin (9.2.x)
- **JDK 17** or newer
- **Android SDK Platform 37** installed (the project compiles against API 37; **minSdk is 26**, Android 8.0)
- A device or emulator **with Google Play services** (needed for Google sign-in and for Arabic city names)
- A free **[WeatherAPI](https://www.weatherapi.com/)** key
- A **Firebase** project
- *Optional:* a **Facebook Developer** app, only if you want Facebook login

### 🚀 Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/mhq93/FyneCast.git
   ```

2. **Open the project** in Android Studio (**File > Open**) and let Gradle sync.

3. **Add your keys to `local.properties`** (project root; Android Studio creates the file with `sdk.dir`).<br>
Add these lines with **no quotation marks**:
   ```properties
   WEATHER_API_KEY=your_weatherapi_key
   FACEBOOK_APP_ID=your_facebook_app_id
   FACEBOOK_CLIENT_TOKEN=your_facebook_client_token
   ```
   - The Facebook lines are optional; without them, Facebook login will not work.
   - The Facebook redirect scheme is built from the app ID automatically, and no Facebook app *secret* is needed.
   - A template is available in `local.properties.example`. The real file is git-ignored.

4. **Set up Firebase**
   - Create a Firebase project and add an Android app with the package name `com.mhq.fynecast`.
   - Add the SHA-1 fingerprint of your debug key (run `./gradlew signingReport`).
   - In **Authentication**, enable **Email/Password** and **Google**.
   - Download `google-services.json` **after** enabling Google (so it contains the web client ID) and place it in the `app/` folder. The file is git-ignored.

5. **Facebook login (optional)**
   - Create a Facebook app with the Facebook Login product and add the Android platform (package `com.mhq.fynecast` and your key hash). Debug builds print the key hash to Logcat.
   - Enable Facebook as a sign-in provider in Firebase Authentication.

6. **Run** the `app` configuration on a device or emulator.

---

## 🧪 Testing

There are no automated tests yet. Unit tests for the validators, mappers and use cases are the first item on the roadmap.

---

## 🐞 Known Issues & Roadmap

**Known issues**
- Arabic place names come from Google's Geocoder, so they need Google Play services.<br>
  Without it, names fall back to the weather service's English names.
- Search suggestions are always in English, German or French, because Photon has no Arabic.<br>
  After you pick a city, its name is rewritten in the app language.
- The map picker needs a network connection and shows no message if the map tiles fail to load.
- Some user-facing messages in ViewModels are still hardcoded in English instead of string resources.

**Roadmap**
- Unit tests (ViewModels and use cases with JUnit, MockK and Turbine).
- Move Android types out of ViewModels and introduce presentation models.<br>
  To follow the Clean Architecture layer rules more strictly.
- Consider Hilt for dependency injection.
