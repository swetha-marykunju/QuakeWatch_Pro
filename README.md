QuakeWatch Pro 
A Real-Time Seismic Activity Tracker for Android

QuakeWatch Pro is a proactive monitoring application designed to keep users informed about global seismic events. 
By integrating the USGS (United States Geological Survey) data feed with Google Maps SDK, the app provides a seamless visual experience and background notification system for significant earthquakes.

CORE FEATURES

  Live Map Integration: Real-time rendering of seismic events using Google Maps SDK with custom-styled markers.

  Intelligent Filtering: A 3-way toggle system allowing users to filter by "All Events," "High Magnitude (5.0+)," and "Recent (Last 24 Hours)."

  Background Monitoring: Leverages Android WorkManager to perform periodic network synchronization every 15 minutes.

  Smart Notifications: Implements logic using SharedPreferences to track earthquake IDs, ensuring users are only notified once for each unique significant event.

  Dynamic UI: Uses Material Design 3, ViewBinding, and custom animations to highlight the most recent seismic activity.

TECH STACK

  Language: Java

  Networking: Retrofit 2 & OkHttp (REST API consumption)

  JSON Parsing: GSON

  Background Tasks: WorkManager API

  Maps: Google Maps SDK for Android

  Persistence: SharedPreferences (State management)

  Security: Secrets Gradle Plugin (API Key obfuscation)

SETUP & INSTALLATION

  To run this project locally, you will need to provide your own Google Maps API Key.

  Clone the Repository:

  Add API Key:

  Open local.properties in the root directory.

  Add the following line: MAPS_API_KEY=your_google_maps_key_here

  Build: * Open the project in Android Studio.

  Sync Gradle and click Run.

SECURITY & BEST PRACTICES

  API Security: Used the Secrets Gradle Plugin to ensure API keys are never hardcoded in the AndroidManifest.xml.

  State Management: Implemented persistent storage logic to manage notification triggers, reducing unnecessary background processing and improving battery efficiency.
