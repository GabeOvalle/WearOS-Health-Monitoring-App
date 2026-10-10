# 2-gafas-wearos-swe2710s26

## Description
This repository contains the work for team gafas in the spring 26 section of SWE 2170.
The design involves a smartwatch-based heart rate monitoring application intended for trade workers performing physically demanding tasks. The will app continuously tracks heart rate data and presents key pieces of information, such as current BPM, hourly average BPM, daily average BPM, and high-impact activity duration. These pieces of information will be displayed in a simple, readable interface designed for use during work.

### Design
The application follows a MVC architecture, currently organized into presentation and data layers.
**Presentation Layer (presentation package)**
This layer is responsible for the user interface and handling user interactions.
**Data Layer (data package)**
This layer is responsible for abstracting the data source from the rest of the application.

# Version Log - MeasureDataCompose App

## v2.0.0 (Latest)

### New Features
- **Safety Alert System**: Real-time heart rate monitoring with automated alerts for anomalies (±20 BPM from average).
- **Haptic Feedback**: Watch vibrates (500ms) when a safety alert is triggered.
- **High-Impact Activity Alerts**: Triggers a notification if high-impact movement is detected for more than 30 seconds.
- **Automated Permissions**: Body sensor permissions are requested automatically on app startup, removing the need for manual permission buttons.
- **Continuous Monitoring**: App starts sensor readings automatically once permissions are granted.
- **Grid Navigation**: 2D grid UI using nested pagers to navigate between live data, stats, and settings.
- **Reactive UI**: High-impact duration stats update in real-time on the dashboard.

### Improvements
- Refactored `HealthServicesRepository` to use a shared hot flow for data processing and alerting.
- Moved sensor reading logic into a background-ready flow in the Repository layer.
- Simplified `MeasureDataScreen` UI for better focus on data and alerts.

---

## v1.0.0 (Base Version)
*Original release*

### Features
- Basic heart rate monitoring using Health Services API.
- Heart rate display with connection status icons (Acquiring/Available).
- Persistence of heart rate data to local CSV storage.
- Manual start/stop controls for sensor recording.
- Basic high-impact activity tracking (saved to disk).

## Installation Instructions

To build and run this application on a Wear OS device, you'll need Android Studio and a connected Wear OS device (either a physical watch or an emulator).
### Prerequisites
* Android Studio
* A Wear OS device or emulator
### Using Android Studio
This is the simplest method to get the app running
Set Up Your Device:
* Emulator:
  * In Android Studio, open the Device Manager (View > Tool Windows > Device Manager).
  * Click Create Device, select the Wear OS category, and choose a suitable hardware profile.
  * Select a system image for Wear OS 3 or higher. Download it if prompted.
  *  Finish the setup and launch the emulator from the Device Manager.
* Physical Watch:
  * Enable developer options on your watch by going to Settings > System > About and tapping the Build number seven times.
  * Go to Settings > Developer options and enable ADB debugging. If you are connecting over Wi-Fi, also enable Wi-Fi debugging.
  * Connect the watch to your computer. You can pair it via Wi-Fi from the Device Manager or connect it via USB
Build and Run:
* Ensure your watch or emulator is selected in the device dropdown menu at the top of the Android Studio window.
* Click the Run 'app' button
* Android Studio will automatically build the project with Gradle, install the app on your selected device, and launch it.

**[https://developer.android.com/studio/run/device#connect](see for more details)**


## Adding files

* [Create](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#create-a-file) or [upload](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#upload-a-file) files
* [Add files using the command line](https://docs.gitlab.com/topics/git/add_files/#add-files-to-a-git-repository) or push an existing Git repository with the following command:

```
cd existing_repo
git remote add origin https://gitlab.com/msoe.edu/sdl/y26swe2710-hasker/2-gafas-wearos-swe2710s26.git
git branch -M main
git push -uf origin main
```

#### Unit Testing
 The business logic for safety alerts and heart rate analysis is verified using JUnit and MockK for vibration and flow mocking.
​
 - **Location**: `app/src/test/java/com/example/measuredatacompose/data/HealthServicesRepositoryTest.kt`
 - **How to Run**: Right-click the `HealthServicesRepositoryTest` file in the project view and select **Run 'HealthServicesRepositoryTest'**.
 - **Coverage**: 
    - heart rate range calculations
    - haptic feedback triggers

#### Integration Testing
  Currently, integration testing exists for the connection to the manager portal. This test simulates the watch's heart rate sensor and validates that data is correctly serialized and published to the MQTT broker using the manager portal's schema. To run the integration tests and verify the MQTT connection to the manager portal, follow these instructions:

  ##### 1. Start the MQTT Broker
  Ensure you have a broker running on your host machine. Using Docker is the easiest method: ``bash docker run -it -p 1883:1883 eclipse-mosquitto``

  *Note: If authentication is required, you may need a custom `mosquitto.conf`.*

  ##### 2. Configure the IP Address
  The test in `MessagingSimulationTest.kt` is currently set to `10.0.2.2`, which is the special alias for your host's `localhost` when using the **Android Emulator**. 
  - **Emulator**: No changes needed.
  - **Physical Device**: Change `brokerIp` in `MessagingSimulationTest.kt` to your computer's local network IP (e.g., `192.168.1.5`).

  ##### 3. Execute the Simulation
  You can trigger the test in two ways:

  - **Android Studio**: Open `app/src/androidTest/java/com/example/measuredatacompose/MessagingSimulationTest.kt` and click the **Run** icon (green arrow) next to the `runHeartRateAlertSimulation` function.

  ##### 4. Verify the Output
  - **Logcat**: Filter for `Messaging` or `MessageTest` to see real-time connection status and JSON payloads.
    
  


## Known Bugs & Issues

- Delayed Sensor Initialization Or Not Reading: There is a ~10-second delay on startup before BPM data displays. This was orginally by design, but the code for that delay has now been removed. The current issue may be partially due to the WhileSubscribed(5000) strategy in the Repository, which only starts the sensor after the UI is fully loaded. However, there is still a delay between when the UI is loaded and the sensor starts reading. Other users have reported issues with the sensor not reading at all.
- UI Blocking on Startup: The uiState check in MeasureDataApp is a blocking check (if (uiState == UiState.Supported)). If hasHeartRateCapability() takes long to resolve, the user sees a blank screen or a "Startup" state before the Pagers are even initialized.
- Continuous Alert Triggering: Once a heart rate is out of range, the app triggers a new haptic vibration and UI alert for every single data point received, rather than once per "event." This results in a non-stop stream of vibrations and popups until the heart rate returns to the normal range.

## Authors 

* Gabe Ovalle: ovalleg@msoe.edu
* Jamie Martig: martigj@msoe.edu
* John Eckberg: eckbergj@msoe.edu
* Logan Collins: collinsl@msoe.edu

## Credits/acknowledgements

The starting point for this lab is based on the [Open Source Health Services Sample](https://github.com/android/health-samples) written by the Android Orginization 