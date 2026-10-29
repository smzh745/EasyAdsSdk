# EasyAdsSdk

A lightweight, clean, and decoupled Google Mobile Ads (AdMob) SDK and demo application for Android.

Inspired by modern production architectures, **EasyAds** eliminates complex boilerplate while retaining essential production rules:
- **Decoupled UI**: Activities and Fragments never interact directly with raw AdMob SDK listeners.
- **GDPR / Google UMP Ready**: Integrated Consent Management for EEA/UK users with in-app privacy revocability.
- **Multiple Native Ad Sizes & Shimmer**: Built-in templates for **BANNER**, **SMALL**, **MEDIUM**, **LARGE**, shimmer placeholders, plus support for **CUSTOM** layouts.
- **Dynamic Remote Config Control**: Toggle any ad placement on/off remotely (`isAdEnabled`) or change ad sizes dynamically via remote keys.
- **Pure Kotlin**: No heavy third-party DI framework needed. Easy to drop into any project.
- **Zero Fullscreen Collision**: Built-in concurrency gate ensures App Open, Interstitial, and Rewarded ads never overlap.
- **Lifecycle & Memory Safe**: Safe Activity context checks (`isSafeForAd`), context-wrapper resolution (`findActivity()`), banner pause/resume/destroy, and complete cache purging.
- **Frequency Capping**: Built-in thread-safe `FrequencyCounter` to show ads every N actions (e.g. 2-4 downloads, 2-3 clicks).
- **One-Line Ad-Free Handling**: Instant memory eviction when the user purchases an ad-free tier.

---

## 📁 Project Structure

```text
EasyAdsSdk/
├── easyads/                 # The reusable Ads SDK library module
│   └── src/main/java/com/easyads/sdk/
│       ├── EasyAds.kt                      # Main facade & config (init, isAdFree, isDebug, remoteConfig, destroyAll)
│       ├── consent/
│       │   └── ConsentManager.kt           # Google UMP GDPR consent gathering & privacy options
│       ├── core/
│       │   ├── AdResult.kt                 # Sealed interface callbacks (Loaded, Shown, Failed, etc.)
│       │   ├── FullscreenGate.kt           # Concurrency gate preventing overlapping fullscreen ads
│       │   ├── ActivitySafety.kt           # isSafeForAd() & findActivity() context unwrappers
│       │   ├── FrequencyCounter.kt         # Thread-safe action interval & frequency gate
│       │   ├── EasyAdsLogger.kt            # Tagged, debug-gated central logging
│       │   └── NetworkUtils.kt             # Fast-fail network connectivity checks
│       ├── config/
│       │   ├── RemoteConfigProvider.kt     # Interface for plugging in Firebase Remote Config
│       │   └── DefaultRemoteConfigManager.kt # Built-in thread-safe remote flag manager
│       ├── banner/
│       │   ├── BannerAdManager.kt          # Adaptive & Collapsible banner logic
│       │   ├── BannerType.kt               # ADAPTIVE, COLLAPSIBLE_TOP, COLLAPSIBLE_BOTTOM
│       │   └── view/EasyBannerView.kt      # Ready-to-use custom Banner View
│       ├── interstitial/
│       │   └── InterstitialAdManager.kt    # Preload & show interstitial ads with safety timeout
│       ├── nativead/
│       │   ├── NativeAdManager.kt          # Native ad loader & multi-size binder with in-flight deduplication
│       │   ├── NativeTemplate.kt           # BANNER, SMALL, MEDIUM, LARGE, SHIMMER, CUSTOM
│       │   └── view/EasyNativeView.kt      # Ready-to-use custom Native View
│       ├── appopen/
│       │   ├── AppOpenAdManager.kt         # Splash & App Resume App Open ads with activity exclusions
│       │   └── AppOpenLifecycleTracker.kt  # ProcessLifecycleOwner observer
│       ├── rewarded/
│       │   └── RewardedAdManager.kt        # Rewarded video manager
│       └── extensions/
│           ├── ActivityExtensions.kt       # Activity helper extensions
│           └── FragmentExtensions.kt       # Fragment helper extensions
└── app/                     # Demo implementation module showcasing all formats & toggles
```

---

## 🚀 Setup & Initialization

### 1. In `Application.onCreate()`:
```kotlin
// Initialize EasyAds (logs only print when isDebug = true)
EasyAds.initialize(this, isDebug = BuildConfig.DEBUG)

// (Optional) Test EEA GDPR dialog in debug builds outside Europe:
if (BuildConfig.DEBUG) {
    EasyAds.consent.isDebugGeographyEEA = true
    // EasyAds.consent.testDeviceHashedId = "YOUR_TEST_DEVICE_HASHED_ID" // If testing on physical device
}

// (Optional) Enable App Resume Open Ad, excluding Splash/Onboarding activities
EasyAds.appOpen.enableAppResume(
    application = this,
    adUnitId = "ca-app-pub-3940256099942544/9257395921",
    disallowedActivityClasses = setOf(SplashActivity::class.java)
)
```

---

## 🛡️ GDPR / Google UMP Consent Management

### 1. Requesting Consent on Splash Screen
```kotlin
EasyAds.consent.requestConsent(this) { canRequestAds ->
    if (canRequestAds) {
        // Preload splash ads or proceed to main screen
        EasyAds.appOpen.loadAndShowSplash(this, adUnitId, timeoutMs = 4000L) {
            navigateToMain()
        }
    } else {
        navigateToMain()
    }
}
```

### 2. Providing Privacy Options in Settings (Google Play Policy Requirement)
Google AdMob requires EEA/UK users to be able to revoke/modify consent at any time:
```kotlin
// Check if user is in EEA / requires privacy options:
val isRequired = EasyAds.consent.isPrivacyOptionsRequired(context)

// Show privacy form when user taps "Consent Choices" or "Privacy Options":
binding.btnPrivacyOptions.setOnClickListener {
    EasyAds.consent.showPrivacyOptionsForm(this) { formError ->
        // Handle completion
    }
}
```

---

## ⏱️ Frequency & Interval Management

Easily trigger interstitial ads every N user actions (e.g. every 2-4 downloads or 2-3 clicks):

```kotlin
// Define counter with random interval (between 2 and 4 actions)
val downloadAdCounter = FrequencyCounter(minInterval = 2, maxInterval = 4)

fun onDownloadCompleted(context: Context) {
    downloadAdCounter.onAction {
        context.findActivity()?.let { activity ->
            EasyAds.interstitial.show(activity, adUnitId)
        }
    }
}
```

---

## 🎨 Native Ads with Different Sizes & Shimmer

EasyAds includes **pre-built responsive templates** and supports **custom XML layouts**:

| Template | Description | Recommended Placement |
| :--- | :--- | :--- |
| `NativeTemplate.BANNER` | Compact horizontal row (~56dp) | In-feed lists, replacing traditional banner ads |
| `NativeTemplate.SMALL` | Compact card (~90dp) | Bottom sheets, dialogs, list items |
| `NativeTemplate.MEDIUM` | Standard card with 140dp media view (~250dp) | Content feeds, mid-screen cards |
| `NativeTemplate.LARGE` | Full card with 180dp media view, star rating & full-width CTA (~360dp) | Exit dialogs, end-of-flow screens, high-engagement screens |
| `NativeTemplate.SHIMMER_BANNER` | Shimmer placeholder for Banner | Loading state |
| `NativeTemplate.SHIMMER_MEDIUM` | Shimmer placeholder for Medium | Loading state |
| `NativeTemplate.CUSTOM(R.layout.my_ad)` | 100% custom layout | Custom branded layouts |

### XML Declaration:
```xml
<com.easyads.sdk.nativead.view.EasyNativeView
    android:id="@+id/nativeAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### Loading Any Size:
```kotlin
binding.nativeAdView.loadAndShow(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/2247696110",
    template = NativeTemplate.MEDIUM,
    remoteKey = "native_home"
)
```

---

## 🎛️ Remote Config Control: Show or Hide Ads

Every format supports an optional `remoteKey` parameter. When passed, **EasyAds** validates whether the ad is enabled before making requests or displaying views.

```kotlin
// Connect Firebase Remote Config:
EasyAds.setRemoteConfigProvider { remoteKey ->
    FirebaseRemoteConfig.getInstance().getBoolean(remoteKey)
}

// Or pass a bulk map:
EasyAds.setRemoteConfigValues(
    mapOf(
        "banner_home" to true,
        "interstitial_splash" to false,
        "native_home" to true
    )
)
```

---

## 💎 Ad-Free / In-App Purchase Support

When a user purchases an Ad-Free upgrade or subscription:

```kotlin
EasyAds.isAdFree = true
```

* All existing in-memory ad caches (Banners, Interstitials, Native, App Open, Rewarded) are **purged immediately**.
* Any future ad requests fail-fast without hitting the network or initializing views.
