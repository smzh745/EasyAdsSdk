# EasyAdsSdk

A lightweight, clean, and decoupled Google Mobile Ads (AdMob) SDK and demo application for Android.

Inspired by modern production architectures, **EasyAds** eliminates complex boilerplate while retaining essential production rules:
- **Decoupled UI**: Activities and Fragments never interact directly with raw AdMob SDK listeners.
- **Multiple Native Ad Sizes**: Built-in templates for **BANNER**, **SMALL**, **MEDIUM**, **LARGE**, plus support for **CUSTOM** layouts.
- **Dynamic Remote Config Control**: Toggle any ad placement on/off remotely (`isAdEnabled`) or change ad sizes dynamically via remote keys.
- **Pure Kotlin**: No heavy third-party DI framework needed. Easy to drop into any project.
- **Zero Fullscreen Collision**: Built-in concurrency gate ensures App Open, Interstitial, and Rewarded ads never overlap.
- **Lifecycle & Memory Safe**: Safe Activity context checks (`isSafeForAd`), banner pause/resume/destroy, and complete cache purging.
- **One-Line Ad-Free Handling**: Instant memory eviction when the user purchases an ad-free tier.

---

## 📁 Project Structure

```text
EasyAdsSdk/
├── easyads/                 # The reusable Ads SDK library module
│   └── src/main/java/com/easyads/sdk/
│       ├── EasyAds.kt                      # Main entrypoint & config (init, isAdFree, destroyAll, remoteConfig)
│       ├── core/
│       │   ├── AdResult.kt                 # Sealed interface callbacks (Loaded, Shown, Failed, etc.)
│       │   ├── FullscreenGate.kt           # Concurrency gate preventing overlapping fullscreen ads
│       │   ├── ActivitySafety.kt           # isSafeForAd() lifecycle safety checks
│       │   └── NetworkUtils.kt             # Fast-fail network connectivity checks
│       ├── config/
│       │   ├── RemoteConfigProvider.kt     # Interface for plugging in Firebase Remote Config
│       │   └── DefaultRemoteConfigManager.kt # Built-in thread-safe remote flag manager
│       ├── banner/
│       │   ├── BannerAdManager.kt          # Adaptive & Collapsible banner logic
│       │   ├── BannerType.kt               # ADAPTIVE, COLLAPSIBLE_TOP, COLLAPSIBLE_BOTTOM
│       │   └── view/EasyBannerView.kt      # Ready-to-use custom Banner View
│       ├── interstitial/
│       │   └── InterstitialAdManager.kt    # Preload & show interstitial ads with remote keys
│       ├── nativead/
│       │   ├── NativeAdManager.kt          # Native ad loader & multi-size binder
│       │   ├── NativeTemplate.kt           # BANNER, SMALL, MEDIUM, LARGE, CUSTOM
│       │   └── view/EasyNativeView.kt      # Ready-to-use custom Native View
│       ├── appopen/
│       │   ├── AppOpenAdManager.kt         # Splash & App Resume App Open ads
│       │   └── AppOpenLifecycleTracker.kt  # ProcessLifecycleOwner observer
│       ├── rewarded/
│       │   └── RewardedAdManager.kt        # Rewarded video manager
│       └── extensions/
│           ├── ActivityExtensions.kt       # Activity helper extensions
│           └── FragmentExtensions.kt       # Fragment helper extensions
└── app/                     # Demo implementation module showcasing all formats & toggles
```

---

## 🎛️ Remote Config Control: Show or Hide Ads

Every format supports an optional `remoteKey` parameter. When passed, **EasyAds** validates whether the ad is enabled before making requests or displaying views.

### 1. Connecting Firebase Remote Config (1 line of code)
```kotlin
// In Application class:
EasyAds.setRemoteConfigProvider { remoteKey ->
    FirebaseRemoteConfig.getInstance().getBoolean(remoteKey)
}
```

Or pass a bulk map (e.g. from JSON or local SharedPreferences):
```kotlin
EasyAds.setRemoteConfigValues(
    mapOf(
        "banner_home" to 1,          // 1 or true = enabled
        "interstitial_splash" to 0,  // 0 or false = disabled
        "native_home" to true,
        "native_size" to "large"     // Can even control native size dynamically!
    )
)
```

### 2. Using Remote Keys with Ads

```kotlin
// Interstitial: If disabled via Remote Config, callback runs immediately with 0 delay!
showInterstitialAd(
    adUnitId = "ca-app-pub-3940256099942544/1033173712",
    remoteKey = "interstitial_home"
) {
    // Continues user flow / navigation seamlessly
}

// Banner: If disabled via Remote Config, container hides and doesn't load
binding.bannerAdView.load(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/6300978111",
    remoteKey = "banner_home"
)
```

---

## 🎨 Native Ads with Different Sizes

EasyAds includes **4 pre-built responsive templates** and supports **custom XML layouts**:

| Template | Description | Recommended Placement |
| :--- | :--- | :--- |
| `NativeTemplate.BANNER` | Compact horizontal row (~56dp) | In-feed lists, replacing traditional banner ads |
| `NativeTemplate.SMALL` | Compact card (~90dp) | Bottom sheets, dialogs, list items |
| `NativeTemplate.MEDIUM` | Standard card with 140dp media view (~250dp) | Content feeds, mid-screen cards |
| `NativeTemplate.LARGE` | Full card with 180dp media view, star rating & full-width CTA (~360dp) | Exit dialogs, end-of-flow screens, high-engagement screens |
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
// Load Large Native Ad
binding.nativeAdView.loadAndShow(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/2247696110",
    template = NativeTemplate.LARGE,
    remoteKey = "native_home"
)

// Or dynamically pick size from Remote Config:
binding.nativeAdView.loadAndShow(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/2247696110",
    remoteKey = "native_home",
    remoteTemplateKey = "native_home_size" // Remote Config value: "banner", "small", "medium", or "large"
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
