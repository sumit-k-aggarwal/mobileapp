GLIDE - ANDROID TAXI RIDER DEMO
=============================

A small, native Java Android application. Android 8.0 (API 26) or newer.
Designed for the Android Pixel test bed; no account or API keys required.

FEATURES
  - Search eight predefined San Francisco destinations; change pickup.
  - Save Home and Work locations.
  - Compare Glide, Comfort, and Glide XL with seats and estimated fares.
  - View an itemized illustrative USD fare estimate before booking.
  - Choose simulated card or cash payment.
  - Book now or schedule 5 minutes to 30 days ahead.
  - Simulated matching, driver arrival, pickup, and trip completion.
  - Cancel before pickup, with no simulated cancellation fee.
  - View receipts, rate completed rides, and browse persisted ride history.
  - Review safety guidance and explicitly share a demo trip via Android.
  - Restore active rides and preferences; clear all local demo data.
  - Scrollable native UI, accessible controls, and illustrated offline map.

DEMO BOUNDARY
  This is a functional local rider prototype, not a production taxi service.
  Maps are illustrations, not live routing. Addresses are predefined.
  Drivers, vehicle availability, ETA, fares, payments, and ride progress are
  simulated. No real transport is dispatched and no money is collected.
  Scheduled rides progress only when the app is open; there is no background
  dispatch or notification service. While open, immediate rides advance after
  5 seconds (matching), 15 seconds (arrival), and 35 seconds (trip).
  The stage buttons let you advance the demo faster.

  A production service would additionally need a backend, rider authentication,
  a driver app and dispatch service, licensed maps/routing, live location,
  payment processing, push notifications, and operational support.

WINDOWS QUICK START (PowerShell, from this directory)
  First-time project-local tool installation:
    .\tools\setup-android.ps1

  Build APK, run unit tests and Android lint:
    .\tools\build.ps1

  Open the app on the Pixel emulator:
    .\tools\run-android.ps1

  Run device acceptance tests (clears this app's local demo data):
    .\tools\test-android.ps1

  Stop only this project's emulator:
    .\.tools\android-sdk\platform-tools\adb.exe -s emulator-5580 emu kill

  The JDK, SDK, Gradle cache, and AVD are project-local in .tools.
  Setup requires internet access and several GB of disk space. Running the
  installed application is offline. Android Studio can also open this folder;
  select the installed SDK and JDK, or source tools\android-env.ps1 for CLI work.

OUTPUTS
  dist\Glide-debug.apk                  Installable debug-signed APK
  artifacts\home.png                    Android screenshot
  artifacts\android-tests.txt           Device acceptance test results
  app\build\reports\tests\testDebugUnitTest\index.html
  app\build\reports\lint-results-debug.html

SOURCE MAP
  MainActivity.java      Rider UI, navigation, dialogs, scheduling
  RideEngine.java        Quotes, schedule validation, trip state machine
  RideStore.java         Device-local persistence
  CityMapView.java       Offline schematic map
  SymbolView.java        Native vector-style icons
  RideEngineTest.java   Focused fare and lifecycle unit tests
  RideInstrumentation.java  On-device user-flow and persistence checks

FEATURE REFERENCES (official sources consulted November 19, 2026)
  Uber rider request flow, ride choices, pickup confirmation, trip progress:
    https://www.uber.com/us/en/ride/how-it-works/
  Uber upfront pricing:
    https://www.uber.com/us/en/ride/how-it-works/upfront-pricing/
  Uber Reserve:
    https://www.uber.com/us/en/ride/how-it-works/reserve/
  Uber rider safety:
    https://www.uber.com/us/en/ride/safety/
  Android command-line builds:
    https://developer.android.com/build/building-cmdline
  Android emulator:
    https://developer.android.com/studio/run/emulator-commandline

  References informed the feature selection only. Glide uses its own original
  branding, interface, illustration, demo data, and illustrative pricing.
