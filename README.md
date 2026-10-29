# EasyAdsSdk

[![Release](https://jitpack.io/v/smzh745/EasyAdsSdk.svg)](https://jitpack.io/#smzh745/EasyAdsSdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A lightweight, clean, and highly customizable Google Mobile Ads (AdMob) SDK and demo application for Android.

Inspired by modern production architectures, **EasyAds** eliminates boilerplate while retaining essential production requirements:
- **Decoupled Architecture**: Screens never interact directly with raw AdMob SDK objects.
- **GDPR / Google UMP Ready**: Integrated Consent Management for EEA/UK users with in-app privacy revocability.
- **Customizable Styling**: Brand your Native Ads with custom colors, typography, and corner radii via `NativeAdStyle`.
- **Pre-built Templates & Shimmer**: Built-in responsive layouts for **BANNER**, **SMALL**, **MEDIUM**, **LARGE**, shimmer loading states, plus 100% **CUSTOM** layout support.
- **Built-in Test Ad Unit IDs**: Access official AdMob test IDs with `TestAdUnits.*` (no more hunting for test IDs).
- **Dynamic Remote Config**: Toggle any ad on/off remotely (`isAdEnabled`) or change sizes dynamically via remote keys.
- **Zero Fullscreen Collision**: Concurrency gate (`FullscreenGate`) ensures App Open, Interstitial, and Rewarded ads never overlap.
- **Lifecycle & Memory Safe**: `isSafeForAd()` checks, `findActivity()` context unwrappers, and complete cache purging.
- **Action Frequency Capping**: Thread-safe `FrequencyCounter` to trigger ads every N actions (e.g. every 2–4 downloads or 2–3 clicks).
- **One-Line Ad-Free Mode**: `EasyAds.isAdFree = true` instantly purges all in-memory ad caches.

---

## 📦 Installation

### Step 1: Add JitPack repository
In your root `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

*(Or in root `build.gradle` for older Gradle projects):*
```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependency
Because this is a multi-module library, reference the `:easyads` module:

In your app's `build.gradle.kts` (Kotlin DSL):
```kotlin
dependencies {
    implementation("com.github.smzh745.EasyAdsSdk:easyads:v1.0.0")
}
```
*(Or in Groovy `build.gradle`):*
```groovy
dependencies {
    implementation 'com.github.smzh745.EasyAdsSdk:easyads:v1.0.0'
}
```

---

## 🚀 Initialization

Initialize EasyAds in your `Application` class using the modern fluent builder:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        EasyAds.init(this) {
            isDebug = BuildConfig.DEBUG
            
            // Simulates European Economic Area (EEA) in debug builds to test GDPR:
            isDebugGeographyEEA = BuildConfig.DEBUG
            // testDeviceHashedId = "YOUR_TEST_DEVICE_HASHED_ID" // If testing on a physical phone

            // Enable automatic App-Resume App Open ad (excluding Splash screen):
            enableAppResume(
                application = this@MyApplication,
                adUnitId = if (BuildConfig.DEBUG) TestAdUnits.APP_OPEN else "YOUR_REAL_APP_OPEN_ID",
                disallowedActivityClasses = setOf(SplashActivity::class.java)
            )

            // Connect Firebase Remote Config (optional):
            remoteConfig { key -> FirebaseRemoteConfig.getInstance().getBoolean(key) }
        }
    }
}
```

---

## 🛡️ GDPR / Google UMP Consent Flow

### 1. Requesting Consent on Splash Screen
```kotlin
EasyAds.consent.requestConsent(this) { canRequestAds ->
    if (canRequestAds) {
        // Pre-warm or show splash ads
        EasyAds.appOpen.loadAndShowSplash(
            activity = this,
            adUnitId = TestAdUnits.APP_OPEN,
            timeoutMs = 4000L
        ) {
            navigateToMain()
        }
    } else {
        navigateToMain()
    }
}
```

### 2. Providing Privacy Options in Settings (Google Play Policy)
Google requires apps serving EEA/UK users to provide an option in Settings to revoke or modify consent:
```kotlin
// Check if user is in EEA / requires privacy options:
val isRequired = EasyAds.consent.isPrivacyOptionsRequired(this)

// Show Google's privacy options dialog when tapped:
binding.btnPrivacyOptions.setOnClickListener {
    EasyAds.consent.showPrivacyOptionsForm(this) { formError ->
        // Consent choices updated
    }
}
```

---

## 🎨 Customizing Native Ads

### 1. Custom Brand Styling (`NativeAdStyle`)
Customize CTA buttons, background cards, typography, and badges to match your app:

```kotlin
val myBrandStyle = NativeAdStyle(
    ctaBackgroundColor = Color.parseColor("#4CAF50"),
    ctaTextColor = Color.WHITE,
    ctaCornerRadiusDp = 10f,
    cardBackgroundColor = Color.parseColor("#F5F5F5"),
    cardCornerRadiusDp = 16f,
    primaryTextColor = Color.BLACK,
    secondaryTextColor = Color.GRAY
)

// Set globally for all Native Ads:
EasyAds.nativeAd.defaultStyle = myBrandStyle

// Or set per view:
binding.nativeAdView.style = myBrandStyle
```

### 2. Built-in Templates & Shimmer

| Template | Description | Recommended Placement |
| :--- | :--- | :--- |
| `NativeTemplate.BANNER` | Compact horizontal row (~56dp) | In-feed lists, banner replacements |
| `NativeTemplate.SMALL` | Compact card (~90dp) | Dialogs, bottom sheets, item headers |
| `NativeTemplate.MEDIUM` | Standard card with 140dp media view (~250dp) | Feeds, article pages |
| `NativeTemplate.LARGE` | Full card with 180dp media view & rating (~360dp) | Exit dialogs, end-of-flow screens |
| `NativeTemplate.SHIMMER_BANNER` | Shimmer loading placeholder for Banner | Loading state |
| `NativeTemplate.SHIMMER_MEDIUM` | Shimmer loading placeholder for Medium | Loading state |
| `NativeTemplate.CUSTOM(R.layout.my_ad)` | 100% custom layout | Custom branded layouts |

### 3. Loading in XML
```xml
<com.easyads.sdk.nativead.view.EasyNativeView
    android:id="@+id/nativeAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```
```kotlin
binding.nativeAdView.loadAndShow(
    activity = this,
    adUnitId = TestAdUnits.NATIVE_ADVANCED,
    template = NativeTemplate.MEDIUM,
    showShimmer = true
)
```

---

## ⏱️ Frequency Capping (`FrequencyCounter`)

Trigger interstitial ads every N actions (e.g. every 2–4 downloads or 2–3 clicks):

```kotlin
val downloadCounter = FrequencyCounter(minInterval = 2, maxInterval = 4)

fun onStatusDownloaded(context: Context) {
    downloadCounter.onAction {
        // Safe context unwrapper resolves Activity without lifecycle callbacks
        context.findActivity()?.let { activity ->
            EasyAds.interstitial.show(activity, TestAdUnits.INTERSTITIAL)
        }
    }
}
```

---

## 🧪 Built-in Test Ad Unit IDs (`TestAdUnits`)

Official Google AdMob test IDs are built-in:
```kotlin
TestAdUnits.APP_OPEN
TestAdUnits.BANNER
TestAdUnits.ADAPTIVE_BANNER
TestAdUnits.INTERSTITIAL
TestAdUnits.INTERSTITIAL_VIDEO
TestAdUnits.REWARDED
TestAdUnits.REWARDED_INTERSTITIAL
TestAdUnits.NATIVE_ADVANCED
TestAdUnits.NATIVE_VIDEO
```

---

## 💎 Ad-Free / In-App Purchase Support

When a user purchases an Ad-Free tier:
```kotlin
EasyAds.isAdFree = true
```
* Instantly evicts and destroys all in-memory ad caches.
* All future ad requests return immediately without loading or network activity.

---
