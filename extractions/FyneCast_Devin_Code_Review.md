# FyneCast `dev` code review: Clean Architecture / clean MVVM

**Repo:** `mhq93/FyneCast` @ `dev` (`c0e3a42`) · single `:app` module · about 10.7k LOC Kotlin
**Yardstick:** *Clean Architecture for Android, 2nd ed.* (Eran Boudjnah), Chapter 2 rules as summarised in `Clean_Architecture_for_Android_CHAPTER_02.md`.
**Method:** static review of every non-trivial source file (domain, data, view models, containers, workers, service, receiver, DI, navigation, manifest, Gradle). Pure Compose leaf components were checked only for layer imports. Nothing was built or run, so runtime claims are reasoned from the code.

Severity: **[C]** critical (feature broken or crash) · **[H]** high (wrong behaviour users will hit) · **[M]** medium · **[L]** low / hygiene

---

## 1. Executive summary

| Area | Verdict |
|---|---|
| Domain layer | Mostly pure Kotlin and use-case driven (good), but it leaks Android (`Context`, `ComponentActivity`, `PagingData`, `@RequiresPermission`). Some models carry presentation formatting, and features are tightly coupled to each other. |
| Presentation layer | **Not a separate layer.** ViewModels sit in `ui/` next to composables. There are no presentation models or mappers, so domain models go straight to the UI. ViewModels hold Android objects, hard-coded English strings and navigation lambdas, and they act as the source of truth for form state (which the book explicitly argues against). |
| UI layer | Composables depend directly on domain models in ~15 files, against the book's rule that "*a UI module should never depend on Domain/Data*". |
| Data / DataSource | There is no DataSource layer: Retrofit services, DTOs, DAOs and entities all live in `data/`, and repositories call them directly. Data imports UI/app classes (`MainActivity`, `ui.editprofile.ImageStorageManager`, the foreground service). |
| DI / app module | Manual container (fine), but ViewModel factories reach into `FyneCastApplication`, repositories are exposed publicly, and workers, the service and the boot receiver bypass use cases. |
| Biggest functional bugs | Alert tracking never schedules its workers, so "alert lifted / expired" alarms never fire unless the phone is rebooted. Home can get stuck on the error screen. Favorites lose their country (duplicate rows, wrong heart state). A failed profile save deletes the current avatar. Location stops working when the user grants only *approximate* location. |

---

## 2. Bugs and logic flaws

### 2.1 Alerts / notifications

| # | Sev | Where | Finding |
|---|---|---|---|
| B1 | **C** | `alerts/data/repoimpl/AlertsRepoImpl.trackAlert`, `AlertServiceControllerImpl`, `notifications/WeatherRefreshWorker`, `notifications/WeatherAlertTrackingWorker` | **Tracked alerts are never monitored.** `trackAlert()` only inserts a row and starts the foreground service. `WeatherAlertTrackingWorker` is only enqueued from `BootReceiver`, and `WeatherRefreshWorker.schedule()` is never called anywhere. Until the device reboots, nothing ever checks expiry or "lifted" status. As a result `CheckAlertConditionsUseCase` and `showAlarmNotification` are effectively dead code in normal use. |
| B2 | **C** | `AlertsForegroundService.Companion.removeAlert` (called from both workers) | Uses `context.startService()` from a background worker. If the service isn't already running, this throws `IllegalStateException` on API 26+. If it does start, `onCreate()` calls `startForeground()` on a background-started service, which throws `ForegroundServiceStartNotAllowedException` on API 31+. The worker's `catch (e: Exception)` turns this into `Result.retry()`, so it retries forever. |
| B3 | **C** | `BootReceiver` and the manifest's `foregroundServiceType="dataSync"` | With `targetSdk = 36`, Android 15+ forbids starting a `dataSync` foreground service from `BOOT_COMPLETED`, so `AlertsForegroundService.start()` throws. The exception is swallowed by an empty `catch`. On Android 15 `dataSync` services also have a 6 h/day limit, so the service type doesn't fit "track until the alert expires". |
| B4 | H | `CheckAlertConditionsUseCase` | `isClearedFromApi = activeAlerts.isNotEmpty() && tracked.id !in activeAlertIds`. When the API returns **no** alerts, which is exactly the "all lifted" case, nothing counts as cleared. The guard is inverted for the most important case. |
| B5 | H | `AlertsMapper.calculateId()` | The ID hashes `headline/description/instruction`. Providers often update alert wording, which changes the ID, so the use case reports the alert as "lifted" while it's still active. Use the provider's identity fields (event + effective + area), not the content. |
| B6 | H | `WeatherRefreshWorker` | Fetches alerts for **one** city, then compares them against **all** tracked alerts. Alerts tracked for other cities are reported as lifted. |
| B7 | H | `AlertsForegroundService` (`START_STICKY`) | `activeAlerts` lives only in memory. After the process is killed, the system restarts the service with a `null` intent. `onCreate()` → `postSummary()` then shows a "Tracking 0 Weather Alerts" ongoing notification that never goes away, because no action ever clears it. The service should rebuild its state from the DB or return `START_NOT_STICKY`. |
| B8 | M | `BootReceiver` | Passes `headline = entity.event` (twice) because `TrackedAlertEntity` doesn't persist the headline. After a reboot, notifications show the wrong text. |
| B9 | M | `WeatherAlertTrackingWorker`, `BootReceiver` | The `1L` minute debug interval is silently raised to WorkManager's 15-minute minimum for periodic work. `CHECK_INTERVAL_*` and `remainingMinutes` are unused. The interval logic is duplicated in two places. |
| B10 | M | `AlertsViewModel.uiState` | An empty alert list maps to `Loading`, so "no alerts" and "still loading" can't be told apart, and the blank screen is shown for both. Add an explicit `Empty` state. |
| B11 | M | Settings: notifications off | Turning notifications off stops the service, but the DB rows and workers stay. `BootReceiver` brings everything back on the next boot. Disabling should call `cancelAllTracking()`. |
| B12 | L | `AlertsNotificationManager.showAlarmNotification` | `System.currentTimeMillis().toInt()` as the notification ID can collide or overflow. Use a stable per-alert ID. |
| B13 | L | `AlertsMapper.parseToEpochMillis` | Parse failures fall back to `0L`. The alert then looks expired (it can't be tracked) and the worker rejects it as `expiresMillis == 0L`. Nobody is told why. |

### 2.2 Home / location / map

| # | Sev | Where | Finding |
|---|---|---|---|
| B14 | **H** | `HomeViewModel.fetchWeatherData` + `HomeContainer` retry | After one successful load, `isInitialLaunchPerformed = true`. If a later city fetch fails, the Retry button calls `fetchWeatherData(queryCity = null)` and `onRequestPermission()`, and both return early on `queryCity == null && isInitialLaunchPerformed`. **The user is stuck on the error screen.** |
| B15 | **H** | `HomeViewModel.onCitySelected` / `fetchWeatherData` | Each call launches a new, uncancelled coroutine, and `selectedCityName` is set before the fetch. Fast successive selections (search → favorite → map) can finish out of order and show city A's weather under city B's name. Keep a `fetchJob` and cancel it. |
| B16 | **H** | `home/data/infra/AndroidLocationStatusChecker.hasLocationPermissions` | Requires fine **and** coarse location, but `MainActivity` treats fine **or** coarse as granted. On Android 12+, a user who picks "Approximate" never gets device location. |
| B17 | H | `DefaultLocationRepoImpl.getCurrentLocation` | There's no timeout and no `lastLocation` fallback. If the fused provider never delivers a fix (indoors, emulator), the coroutine hangs and Home stays on `Loading` forever. Wrap it in `withTimeoutOrNull` and check `continuation.isActive` before resuming. |
| B18 | H | `FyneCastApp` → `onCityRowSelected` | Passes `suggestionName = favoriteCity.cityName` (no country). `ObserveFavoriteStatusUseCase` then splits it into country `""`, so the heart shows "not favorite", and tapping it **inserts a duplicate row** `(city, "")`. |
| B19 | M | `HomeViewModel.refreshCurrentCity` | Re-queries by the localised display name (`"القاهرة, مصر"`, `"Paris, France"`) instead of the stored coordinates, which can resolve to a different place. `lastRefreshTime` is set even when the call fails, and errors are swallowed. |
| B20 | M | `HomeViewModel` error branch | Sets `selectedCityName = "Cairo, Egypt"` even though no Cairo data was loaded. The default city is a business rule hard-coded in the ViewModel. |
| B21 | M | `HomeContainer` → `HomeError.retryAction` | When the initial launch hasn't completed, Retry fires both the permission flow and a direct fetch, which gives two concurrent fetches. |
| B22 | M | `WeatherMapper` (hourly) | Uses `forecastDay[0].hour`, i.e. 00:00–23:00 of *today*, so the carousel starts with past hours. Filter from `location.localtime` and spill into tomorrow. |
| B23 | M | `TimestampFormatter` via `WeatherMapper` | Formatting happens at map time using `Locale.getDefault()` for day names and `Locale.US` for hours. Switching the in-app language doesn't re-format cached data, and Arabic users see English hours. |
| B24 | M | `FyneCastApp.shouldShowBar` | `currentRoute in rootRoutes \|\| (currentRoute == Home && Success)`: the second clause is redundant, so the bar also shows on Home Loading and Error. The intent was presumably `Home → only on Success`. |
| B25 | L | Language mapping | `if (currentLanguage == "Arabic") "ar" else "en"` appears in three places (`SettingsViewModel`, `FyneCastApp` twice). When the language changes, Home isn't re-fetched in the new language. |
| B26 | L | `MapPickerScreen` | Hard-coded Cairo camera and `setMaxZoomPreference(10.0)` (which blocks precise picking). `collectAsState` isn't lifecycle-aware. |

### 2.3 Auth / profile

| # | Sev | Where | Finding |
|---|---|---|---|
| B27 | **H** | `UpdateUserProfileUseCase` + `ImageStorageManager.clearOldProfileImages` | The image is sandboxed first, which **deletes every existing profile picture**, and only then is `updateRemoteProfile()` called. If the remote update fails (no network, `recent-login-required`), DataStore still points at the deleted file and the avatar breaks. Delete old images only after a successful save. |
| B28 | **H** | `UpdateUserProfileUseCase` / `UserProfileRepositoryImpl.getProfile` | When the email changes, `verifyBeforeUpdateEmail()` only *sends a verification* email, but the new email is saved locally right away. `getProfile()` prefers the DataStore email over Firebase's, so the app shows an unverified email indefinitely. The `verificationSent` result is never surfaced to the user. |
| B29 | H | `EditProfileViewModel.init` | Collects `profileState` indefinitely and overwrites `_username`, `_userEmail` and `_profileImageUri` on every emission, which can wipe the user's in-progress edits. Take only the first value (`first()`) to seed the form. |
| B30 | H | `LoginViewModel` / `SignupViewModel` social auth | The VM receives a `Context` / `ComponentActivity` and keeps it in `viewModelScope` while the Credential Manager / Facebook UI is open, which leaks the Activity across configuration changes. On cancel, `continuation.cancel()` throws `CancellationException`, which `runCatching` swallows, and the user sees "Facebook sign-in failed" for a deliberate cancel. Google cancel behaves the same. |
| B31 | M | `LoginViewModel`, `ForgotPasswordViewModel` | There's no re-entrancy guard (Signup has one), so double taps send duplicate requests. `_loginUiState` and `_forgotPasswordUiState` are private and never exposed, so the UI can't show progress (dead state). |
| B32 | M | Navigation callbacks passed into VMs (`validateThenLogin(onSuccess)`, `handlePasswordReset(onSuccess)`, …) | The VM invokes a Composable-captured lambda after `suspend` work. After a configuration change it calls a stale lambda / NavController. Emit a one-off effect or state instead. |
| B33 | M | Email validation | Three different regexes. Login and Signup use `[a-z]+\.+[a-z]+` for the domain, which rejects `user@mail.co.uk`, `user@my-company.com`, `user@Gmail.com` and digits in domains. Edit-profile uses a different, looser regex. Signup forbids spaces in usernames (3–15 chars) while edit-profile allows spaces (3–30), so a user can save a name they couldn't have signed up with. The password special-character set excludes `!`, `?`, `*`, etc. Signup doesn't `trim()` the email; Login does. |
| B34 | M | Logout (`SettingsContainer` → `logOutUserSession()` then `onLogoutClick()`) | Fire-and-forget: navigation happens before `clearSession()` finishes. Logout doesn't cancel alert tracking, clear favorites or `lastSearchedCity`, or sign out of Facebook (`LoginManager.logOut()`) or Credential Manager (`clearCredentialState`). The activity-scoped `HomeViewModel` keeps the previous user's state. Errors from `LogoutUserUseCase` are ignored. |
| B35 | M | `AuthRepositoryImpl` and `UserProfileRepositoryImpl` | Both define their own `SessionKeys` over the **same** DataStore keys, so two repositories own one source of truth and can drift. |
| B36 | M | `UserProfileRepositoryImpl.init` | `CoroutineScope(Dispatchers.IO).launch { … }`: unstructured, never cancelled, races with the first `profileState` read. Inject an application scope. |
| B37 | L | Auth navigation | `ForgotPassword → onBackToLoginClick` and `Login ↔ Signup` use `navigate(...)` instead of `popBackStack()`, so the back stack keeps growing (Signup→Login→Forgot→Login→…). |
| B38 | L | `AppInitializerImpl` | `printFacebookKeyHash()` computes a hash and throws it away, uses the deprecated `GET_SIGNATURES` with `!!`, and has an empty `catch`. `initializeSdkComponents()` is empty and never called. |

### 2.4 Favorites / settings

| # | Sev | Where | Finding |
|---|---|---|---|
| B39 | **H** | `FavoritesViewModel.deleteFavorite` | The delete runs after a 4 s delay inside `viewModelScope`. If the ViewModel is cleared first (user leaves the tab, logs out, app is killed), the job is cancelled and the item **comes back** even though the user saw "removed". `FavoritesContainer` also shows the snackbar even when the VM ignores a second delete (`pendingDeletion != null → return`). Persist a "pending delete" flag, or delete immediately and undo with a re-insert. |
| B40 | H | Favorite identity | The primary key is `(cityName, countryName)`, derived by splitting the display string on `","` in **three** places (`HomeViewModel`, `ObserveFavoriteStatusUseCase`, the dead `InsertFavoriteUseCase`). This breaks for `"Washington, D.C., United States"` and for Arabic `"،"`. Meanwhile `stableKey` uses `lat,lon`. One identity (an ID or rounded coordinates) should come from the domain. |
| B41 | M | `FavoriteEntity` | Temperature and icon are snapshots taken when the city was favorited and are never refreshed, so the list shows stale weather as if it were current. |
| B42 | M | `ToggleFavoriteUseCase` | Check-then-act across two DAO calls isn't atomic (double tap). Use a `@Transaction` DAO method. |
| B43 | M | `SettingsViewModel.init` locale flow | `currentLanguage` starts as `"English"` (the `stateIn` initial value), so the activity first applies `en` and then `ar`, causing a double recreation / flicker for Arabic users. `localeEvent` is a replay-0 `SharedFlow` emitted with `tryEmit`, so events before `MainActivity` subscribes are dropped. |
| B44 | L | `UserPreferencesRepositoryImpl` defaults | `notificationsEnabled = true` before permission is granted (corrected later by the sync use case). `isDarkMode = true` ignores the system theme. The language is stored as a display name ("English") instead of a code. |
| B45 | L | `InsertFavoriteUseCase` | Never used. Its first overload duplicates the split logic, and it imports `home.domain.WeatherDomainModel`. |

### 2.5 Build / manifest / config

| # | Sev | Where | Finding |
|---|---|---|---|
| B46 | H | `app/build.gradle.kts` | `localProperties.load(File(rootDir,"local.properties").inputStream())` fails the build on any machine or CI without `local.properties`, and the stream is never closed. A missing `WEATHER_API_KEY` becomes the literal string `"null"`. Unused `kotlinx.coroutines.flow.*` imports in the build script. |
| B47 | M | `app/build.gradle.kts` | `JavaVersion.VERSION_12` (non-LTS, no matching Kotlin `jvmTarget`). `compileSdk = 37` vs `targetSdk = 36`. `paging-compose` declared twice (catalog version + `3.5.0-rc01`). Unused `converter-gson` next to kotlinx-serialization. `androidx.compose.remote.creation.core` has no visible use. Release has `isMinifyEnabled = false`. Versions are a mix of hard-coded strings and the catalog. |
| B48 | M | `AndroidManifest.xml` | `android:exported` on `<application>` is meaningless. `SCHEDULE_EXACT_ALARM` and `FOREGROUND_SERVICE_SPECIAL_USE` are declared but unused. The service type doesn't match the use (see B3). The Facebook `ApplicationSignature` is hard-coded. |
| B49 | M | `local.properties.example` | Lists `FACEBOOK_APP_SECRET`. An app secret must never be shipped in or used by a client app. Remove it from the template. |
| B50 | L | `.gitignore` | Ignores `.idea/`, but `.idea/*.xml` files are committed. |
| B51 | L | Tests | Only `ExampleUnitTest` / `ExampleInstrumentedTest`. The domain is almost pure Kotlin and easy to unit test; validation, alert conditions and toggle logic should be covered first. |

---

## 3. Architecture violations (against Chapter 2)

### 3.1 Domain layer: "pure Kotlin, no Android SDK references"

| # | Sev | Where | Violation / fix |
|---|---|---|---|
| A1 | **H** | `auth/domain/repository/auth/SocialAuthCredentialProvider` (`Context`, `Intent`, `ComponentActivity`) and `auth/domain/usecases/auth/AuthenticateWithSocialProviderUseCase` (`Context`, `ComponentActivity`) | Android types in domain signatures. Fetching the social token is a **UI concern** (it needs an Activity), so do it in the UI layer, or behind a UI-owned launcher. Domain should only receive the token: `AuthenticateWithSocialTokenUseCase(token, provider: SocialProvider)`. |
| A2 | M | `favorites/domain/repository/FavoritesRepository`, `GetFavoritesUseCase` → `androidx.paging.PagingData` | A Jetpack type in the domain contract. Either accept it as a pragmatic, documented exception (paging-common is JVM-only), or expose `Flow<List<FavoriteCity>>` and page in Data/Presentation. |
| A3 | L | `home/domain/usecases/GetCurrentLocationUseCase` → `androidx.annotation.RequiresPermission`; `home/domain/models/CitySuggestionModel` → `@Serializable` | Framework / serialization annotations on domain types. Serialization belongs to Data/DataSource models. |
| A4 | M | `home/domain/models/DailyForecastDomainModel` (`dayName` "Mon", `monthDayStr` "Jun 18"), `HourlyForecastDomainModel.formattedHour`, `ObserveCurrentCityNameUseCase` ("Unknown Location"), `UserPreferences.currentLanguage` ("English") | **Presentation and UI concerns in domain.** Domain should carry `LocalDate` / epoch / enums (`Language.EN/AR`). Formatting belongs in Presentation→UI mappers, which also fixes B23. |
| A5 | M | `ToggleAlertsUseCase`, `CheckAlertConditionsUseCase` → `System.currentTimeMillis()` | A hidden time dependency makes the business rules hard to test. Inject a `Clock` / `TimeProvider`. |
| A6 | M | Cross-feature coupling: `alerts.GetAlertsUseCase → home.WeatherRepository`; `home.WeatherDomainModel → alerts.AlertDomainModel`; `home.ObserveFavoriteStatusUseCase → favorites.FavoritesRepository`; `home.GetLastSearchedCityUseCase → settings.UserPreferencesRepository`; `map.ReverseGeocodeLocationUseCase → home.LocationRepository/CitySuggestionModel`; `alerts.notifications.* → settings.UpdatePreferenceUseCase/ObserveUserPreferencesUseCase`; `favorites.InsertFavoriteUseCase → home.WeatherDomainModel` | Features import each other's domain internals, which creates a dependency web. Move shared concepts (weather, location, preferences) into a `core/domain` (or a shared "weather" feature), and have use cases depend on **repositories**, not on other features' use cases. |
| A7 | L | `FetchWeatherDataUseCase` | Copies a `WeatherDomainModel` field-by-field into an identical `WeatherDomainModel`. The comment says this "protects downstream boundaries from API JSON drift", but it doesn't: it's a no-op. The real protection is the DTO→domain mapper that already exists. Either remove the copy or make the use case do real work (caching or a staleness policy). |
| A8 | L | `UpdatePreferenceUseCase` | A bag of five setters is a repository facade, not a use case ("what the user requires"). Split it into intent-based use cases (`ChangeUnitSystem`, `ChangeLanguage`, `SaveLastSearchedCity`, …) or call the repository through focused ones. |
| A9 | L | `SendPasswordResetUseCase → ValidateLoginFormUseCase.validateEmailFormat` | A use case depends on another use case just to reuse a regex. Extract a shared domain `EmailValidator` (this also fixes the three-regex drift in B33). |
| A10 | L | Mixed `Result<T>` and exceptions | Auth returns `Result<T>`, weather throws, location returns `null`, alerts swallow errors. Define one domain error model (e.g. `sealed DomainError`) so Presentation can map errors consistently. |

### 3.2 Presentation layer: "no Android classes, no context, no resources; tell the VM *what happened*; Presentation is not the source of truth"

| # | Sev | Where | Violation / fix |
|---|---|---|---|
| A11 | **H** | Structure | There's no `presentation` package. ViewModels, UI state classes and composables are mixed under `*/ui/`, so Presentation and UI are merged and the Dependency Rule between them can't be enforced. Proposed layout per feature: `presentation/` (ViewModel, `*ViewState`, `*PresentationModel`, domain→presentation mappers) and `ui/` (composables, presentation→UI mappers, string resources). |
| A12 | **H** | No presentation models | `HomeUiState.Success(weather: WeatherDomainModel)`, `HomeStateContainer.citySuggestions: List<CitySuggestionModel>`, `AlertsUiState.Success(List<AlertDomainModel>)`, `MapPickerUiState.selectedLocation: CitySuggestionModel`, `FavoritesViewModel: PagingData<FavoriteCityDomainModel>`. Every ViewModel publishes **domain models**, so the book's *Domain-to-Presentation mappers* are missing entirely. |
| A13 | **H** | `LoginViewModel.onGoogleAuthenticate(context)`, `onFacebookAuthenticate(activity)`; same in `SignupViewModel` | `Context` / `ComponentActivity` in Presentation (and a leak, B30). |
| A14 | H | Hard-coded user-facing strings in ViewModels: "Google sign-in failed", "Account created successfully.", "Processing image and saving changes...", "Profile changes saved successfully!", "Reset link successfully sent to …", "Unknown Error", "No location data found here…", "Username", "username@gmail.com", "Unknown Location" | Presentation must not own strings or resources. Emit typed events or errors (`LoginError.InvalidCredentials`, `ProfileEvent.Saved`), and let the UI map them to `R.string.*`. Today none of these strings are localised to Arabic, despite `values-ar`. Validation messages are produced in **domain** use cases as English strings, which has the same problem: return `ValidationError` enums instead. |
| A15 | H | ViewModels are the source of truth for form input (`LoginViewModel._email/_password`, `SignupViewModel` four fields, `EditProfileViewModel`, `ForgotPasswordViewModel`) and read their own state on submit (`validateThenLogin()` reads `_email.value`) | The chapter says Presentation is *not* the source of truth (UI and Data are) and gives the pattern `onSendEmailAction(address) { sendEmail(address) }`. Let the UI own the text-field state and pass values in the action: `onLoginAction(email, password)`. |
| A16 | M | Imperative, UI-flavoured method names | The chapter asks for "tell the VM what happened", with no UI terms. Current names and suggested ones: `validateThenLogin` → `onLoginAction`; `validateThenRegister` → `onSignupAction`; `handlePasswordReset` → `onPasswordResetAction`; `handleSavingProfileChanges` → `onSaveProfileAction`; `togglePasswordVisibility` → `onPasswordVisibilityToggled`; `toggleUnitSystem`/`toggleDarkMode`/`toggleLanguage` → `onUnitSystemChanged`/`onThemeChanged`/`onLanguageChanged`; `logOutUserSession` → `onLogoutAction`; `deleteFavorite`/`undoDelete` → `onFavoriteSwiped`/`onUndoAction`; `fetchWeatherData`/`refreshCurrentCity`/`fetchInitialLocationWeather` → `onRetryAction`/`onAppResumed`/`onScreenStarted`; `syncNotificationPermissionState` → `onAppResumed`. `onMapClicked` and `onAlertTrackToggled` are borderline UI terms. |
| A17 | M | VM factories (`companion object factory { this[APPLICATION_KEY] as FyneCastApplication … app.container… }`) in every ViewModel | Presentation knows about the Android `Application` and the DI container. Move the factories into the app/DI module (e.g. `core/di/ViewModelFactories.kt`) so ViewModels only declare constructor dependencies. `SessionViewModel.factory` even constructs `CheckUserLoggedInUseCase(app.container.authRepository)` itself instead of using `container.checkUserLoggedInUseCase`. |
| A18 | M | Business logic in ViewModels | Examples: `HomeViewModel` decides the fallback chain (GPS → last city → "Cairo"), splits the city name to build `FavoriteCityDomainModel`, and runs a 60 s refresh cooldown. `SettingsViewModel` maps "Arabic"→"ar". These are domain rules: create e.g. `ResolveStartupLocationUseCase`, `ToggleFavoriteCityUseCase(weather)`, `Language` enum. |
| A19 | M | `SettingsViewModel` emits `localeEvent` that `MainActivity` applies, and `requestNotificationPermissionEvent` | Fine as an effect pattern, but it relies on activity-scoped ViewModels (see A20). Model effects as a `Channel`/`SharedFlow` per screen and consume them in the screen's UI. |
| A20 | M | `MainActivity` creates `HomeViewModel`, `AlertsViewModel` and `SettingsViewModel` at activity scope and passes them through `FyneCastApp`; the Favorites and Map destinations call `homeViewModel.onCitySelected(...)` directly | Screens are coupled through a shared ViewModel instead of navigation results or shared Data state (e.g. a `SelectedCityRepository` that Home observes). The activity-scoped VMs also outlive logout (B34). |
| A21 | L | `collectAsState()` (Home, EditProfile, Map, `FyneCastApp`) vs `collectAsStateWithLifecycle()` elsewhere | Inconsistent. Use the lifecycle-aware version everywhere. |

### 3.3 UI layer: "relies only on Presentation; never depends on Domain/Data"

| # | Sev | Where | Violation / fix |
|---|---|---|---|
| A22 | **H** | Composables importing domain models: `alerts/ui/components/AlertCard.kt`, `alerts/ui/screens/AlertsContent.kt`, `favorites/ui/components/FavoriteCityItem.kt`, `favorites/ui/screens/FavoritesContent.kt`, `favorites/ui/screens/FavoritesContainer.kt`, `home/ui/components/WeatherDailyCarousel.kt`, `WeatherHourlyCarousel.kt`, `WeatherSearchBar.kt`, `WeatherStatsGrid.kt`, `home/ui/screens/HomeContent.kt`, `map/ui/components/LocationConfirmCard.kt`, `map/ui/screens/MapPickerScreen.kt` | A direct UI→Domain dependency. Once A12's presentation models exist, the UI should only import those. |
| A23 | M | `FyneCastApplication.createNotificationChannels`, `AlertsNotificationManager` | Channel names, descriptions and notification texts are hard-coded English (with emoji). They belong in string resources (UI layer) so Arabic works. |
| A24 | L | Presentation→UI mapping done inline (`"${weather.currentTempC}°"` vs F) | Acceptable in UI per the chapter, but extract a `TemperatureFormatter` UI mapper so rounding and units are consistent (raw doubles like `23.4°` show today). |

### 3.4 Data and DataSource layers

| # | Sev | Where | Violation / fix |
|---|---|---|---|
| A25 | **H** | No DataSource layer | `WeatherApiService` and `LocationApiService` with their DTOs (`home/data/api/**`), and the Room DAOs and entities (`*/data/database/**`), live in Data, and repositories call them directly. The chapter defines a separate DataSource layer (network, DB, platform) with its own models and mappers, behind interfaces owned by Data: `WeatherRemoteDataSource`, `FavoritesLocalDataSource`, `AlertsLocalDataSource`, `SessionLocalDataSource`, `PreferencesLocalDataSource`, `DeviceLocationDataSource`. Data would then hold only repository implementations, Data models and Data↔Domain mappers. |
| A26 | **H** | Data → UI/app dependencies: `alerts/notifications/data/infra/AlertsNotificationManager` imports `core.ui.MainActivity`; `alerts/data/repoimpl/AlertServiceControllerImpl` drives `notifications.AlertsForegroundService`; `auth/data/repoimpl/profile/ImageStorageRepositoryImpl` imports `auth.ui.editprofile.ImageStorageManager` | Inner layers point outwards. `ImageStorageManager` does file and bitmap I/O, so it's a **DataSource** and should move to `auth/datasource/`. Service and notification control is an app/platform concern behind a domain port. The port (`AlertServiceController`) is a good start but is named and placed as a *repository*. Notification `PendingIntent`s should come from a `launchIntentProvider` injected by the app module. |
| A27 | M | `AlertsRepoImpl.trackAlert/cancelTracking` | The repository starts and stops foreground services and cancels WorkManager jobs, so orchestration of side effects lives in Data. Move it to use cases (`TrackAlertUseCase` = save + start tracking + schedule check). That also fixes B1, because one use case would own all three steps. |
| A28 | M | `AlertsRepository.getAlerts()` returns `flowOf(emptyList())` | A dead, misleading API (alerts actually come from `WeatherRepository`). Remove it. |
| A29 | M | `WeatherRepoImpl` reads `BuildConfig.WEATHER_API_KEY` | Inject the key (or add it with an OkHttp interceptor) from DI so Data is free of build-config coupling and can be tested. |
| A30 | M | `WeatherRepoImpl.weatherDataCache` | A single global in-memory `StateFlow` shared by the UI and the background `WeatherRefreshWorker`. A worker refresh for city X overwrites the Home and Alerts state the user is viewing for city Y. Key the cache by location or keep it in a proper local data source. |
| A31 | L | `home/data/api/weather/dto/WeatherApiResponse` imports `alerts.data.dto.AlertsDto` | Cross-feature DTO coupling. Keep the Weather API DTOs together in the weather data source. |
| A32 | L | Two separate Room DBs (`favorites_db`, `alerts_db`), `exportSchema` default/false, no migrations | Prefer one `AppDatabase` in the DataSource layer with exported schemas and migrations before the first release. |

### 3.5 App module, DI and navigation

| # | Sev | Where | Violation / fix |
|---|---|---|---|
| A33 | H | Single `:app` module | The Dependency Rule is only a convention. The book's structure is modules per layer (or per feature + layer), e.g. `:core:domain`, `:feature:home:domain/data/presentation/ui`, `:datasource:weather-api`, `:datasource:database`, `:app`. Even one Gradle module per layer (`:domain` as a pure JVM module) would make A1–A4 compile errors instead of review findings. |
| A34 | M | `AppContainer` exposes repositories publicly (`authRepository`, `weatherRepository`, `alertsRepository`, `notificationPermissionChecker`, …) | ViewModels, workers, the service and the boot receiver use them directly and bypass use cases (`WeatherRefreshWorker` calls `weatherRepository.provideWeatherData` and even builds `CheckAlertConditionsUseCase` itself; `AlertsForegroundService` calls `alertsRepository.cancelAllTracking()`). Expose only use cases (and ViewModel factories) from the container. |
| A35 | M | Duplicated alert business logic in `BootReceiver`, `WeatherAlertTrackingWorker`, `ToggleAlertsUseCase` and `CheckAlertConditionsUseCase` (expiry checks, scheduling, intervals) | One `ScheduleAlertCheckUseCase` / `ReconcileTrackedAlertsUseCase` should be the single source. The Android entry points become thin adapters. |
| A36 | L | `FyneCastApplication.instance` (global static) | Unused service locator. Remove it. Retrofit instances are built eagerly with no OkHttp client, timeouts or logging. |
| A37 | L | Routes as `BottomNavigationItem` objects plus the magic string `"map_picker"`, and auth routes modelled as bottom-nav items | Split them into `AppDestination` (all routes) and a `BottomNavTab` list. Consider type-safe navigation (Navigation 2.8+ `@Serializable` routes). |
| A38 | L | Dead or commented code | Old `BottomNavigationItem`, old `UserPreferences`, `themeMode`, the `osm_tag` comment, `InsertFavoriteUseCase`, `AlertsRepository.getAlerts`, `AppInitializer.initializeSdkComponents`. |

---

## 4. What's already done well
- Use cases with `operator fun invoke` throughout, and most are `suspend`, matching the book's convention.
- Repository **interfaces** live in domain, with implementations in data (dependency inversion is in place).
- DTO→Domain and Entity↔Domain mappers exist and are kept separate (`WeatherMapper`, `AlertsMapper`, `FavoritesMapper`).
- API DTOs and DB entities are separate models (the chapter's "no dependency between API and storage").
- Platform checks sit behind domain ports (`NotificationPermissionChecker`, `LocationProviderStatusChecker`).
- Screens use a Container/Content split, which is a good seam for the Presentation→UI boundary.
- `DefaultAppContainer` is readable, lazy, and wires the composition root in one place.

---

## 5. Suggested remediation order
1. **Fix the functional criticals:** B1 + A27 (one `TrackAlertUseCase` that schedules the worker), B2/B3 (service start rules on API 31+/35+), B4, B14, B16, B18, B27, B39.
2. **Make the layers real:** add `presentation/` packages with presentation models and mappers (A11, A12, A22). Move factories to DI (A17). Remove `Context` from ViewModels and domain (A1, A13).
3. **Move strings out of Presentation and Domain** into typed errors/events plus `R.string` (A14, A23). This also completes the Arabic localisation.
4. **Add a DataSource layer** (A25) and move `ImageStorageManager` into it (A26). Fold the two session-key owners into one `SessionLocalDataSource` (B35).
5. **Decouple features** through `core/domain` (A6), then split into Gradle modules (A33) so the compiler enforces the Dependency Rule.
6. **Add unit tests** for the domain (validators, alert conditions, toggles) with an injected `Clock` (A5, B51).
