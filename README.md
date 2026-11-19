# Glide Android Taxi App

Native Android taxi-booking demo with destination search, fare estimates,
three ride options, scheduling, simulated trip progress, cancellation,
receipts, ratings, and locally saved ride history.

Maps, drivers, and payments are simulated. No real rides or charges occur.

## Build and run

From the repository directory in PowerShell:

```powershell
# First-time setup; skip when the project-local tools are already installed.
.\tools\setup-android.ps1

.\tools\build.ps1
.\tools\run-android.ps1
```

The installable APK is generated at `dist\Glide-debug.apk`.

## Android test bed

```powershell
.\tools\test-android.ps1
```

This runs unit tests, Android lint, and device acceptance checks on the
`Glide_Pixel` emulator. Device tests clear this app's local demo data.

See `README.txt` for the full feature list, demo limitations, source layout,
and official feature references. Downloaded tools, build outputs, APKs,
and test artifacts are excluded from Git.