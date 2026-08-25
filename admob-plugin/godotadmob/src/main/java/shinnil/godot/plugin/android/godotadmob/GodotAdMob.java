package shinnil.godot.plugin.android.godotadmob;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus;
import com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener;

import org.godotengine.godot.Godot;
import org.godotengine.godot.GodotLib;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public class GodotAdMob extends GodotPlugin {
    private final Activity activity; // The main activity of the game

    private boolean isReal = false; // Store if is real or not
    private boolean isForChildDirectedTreatment = false; // Store if is children directed treatment desired
    private boolean isPersonalized = true; // ads are personalized by default, GDPR compliance within the European Economic Area may require you to disable personalization.
    private String maxAdContentRating = ""; // Store maxAdContentRating ("G", "PG", "T" or "MA")
    private Bundle extras = null;
    private String appId = "";
    private RequestConfiguration requestConfiguration;

    private FrameLayout layout = null; // Store the layout

    private RewardedVideo rewardedVideo = null; // Rewarded Video object
    private RewardedInterstitial rewardedInterstitial = null; // Rewarded Interstitial object
    private Interstitial interstitial = null; // Interstitial object
    private Banner banner = null; // Banner object
    private CMP cmp; // Google Consent Management Platform (CMP)


    public GodotAdMob(Godot godot) {
        super(godot);
        this.activity = getActivity();
    }

    // create and add a new layout to Godot
    @Override
    public View onMainCreate(Activity activity) {
        layout = new FrameLayout(activity);
        return layout;
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "GodotAdMob";
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        signals.add(new SignalInfo("on_admob_initialized"));

        signals.add(new SignalInfo("on_admob_ad_loaded"));
        signals.add(new SignalInfo("on_admob_banner_failed_to_load", Integer.class));

        signals.add(new SignalInfo("on_interstitial_loaded"));
        signals.add(new SignalInfo("on_interstitial_failed_to_load", Integer.class));
        signals.add(new SignalInfo("on_interstitial_close"));
        signals.add(new SignalInfo("on_interstitial_opened"));
        signals.add(new SignalInfo("on_interstitial_clicked"));
        signals.add(new SignalInfo("on_interstitial_impression"));

        signals.add(new SignalInfo("on_rewarded_video_ad_closed"));
        signals.add(new SignalInfo("on_rewarded_video_ad_failed_to_load", Integer.class));
        signals.add(new SignalInfo("on_rewarded_video_ad_loaded"));
        signals.add(new SignalInfo("on_rewarded_video_ad_opened"));

        signals.add(new SignalInfo("on_rewarded_interstitial_ad_loaded"));
        signals.add(new SignalInfo("on_rewarded_interstitial_ad_opened"));
        signals.add(new SignalInfo("on_rewarded_interstitial_ad_closed"));
        signals.add(new SignalInfo("on_rewarded_interstitial_ad_failed_to_load", Integer.class));
        signals.add(new SignalInfo("on_rewarded_interstitial_ad_failed_to_show", Integer.class));

        signals.add(new SignalInfo("on_rewarded", String.class, Integer.class));
        signals.add(new SignalInfo("on_rewarded_clicked"));
        signals.add(new SignalInfo("on_rewarded_impression"));

        signals.add(new SignalInfo("on_consent_info_update_success"));
        signals.add(
                new SignalInfo("on_consent_info_update_failure",
                        Integer.class, String.class));
        signals.add(new SignalInfo("on_app_can_request_ads", Integer.class));

        return signals;
    }

    /* Init
     * ********************************************************************** */

    /**
     * Prepare for work with AdMob
     *
     * @param isReal     Tell if the environment is for real or test
     */
    @UsedByGodot
    public void init(boolean isReal) {
        this.initWithContentRating(isReal, false, true, "");
    }

    /**
     * Init with content rating additional options
     *
     * @param isReal                      Tell if the environment is for real or test
     * @param isForChildDirectedTreatment Target audience is children.
     * @param isPersonalized              If ads should be personalized or not.
     *                                    GDPR compliance within the European Economic Area requires that you
     *                                    disable ad personalization if the user does not wish to opt into
     *                                    ad personalization.
     * @param maxAdContentRating          must be "G", "PG", "T" or "MA"
     */
    @UsedByGodot
    public void initWithContentRating(
            boolean isReal,
            boolean isForChildDirectedTreatment,
            boolean isPersonalized,
            String maxAdContentRating) {
            initWithContentRating(isReal, isForChildDirectedTreatment, isPersonalized, maxAdContentRating, appId);
    }

    @UsedByGodot
    public void initWithContentRating(
            boolean isReal,
            boolean isForChildDirectedTreatment,
            boolean isPersonalized,
            String maxAdContentRating,
            String appId) {

        this.isReal = isReal;
        this.isForChildDirectedTreatment = isForChildDirectedTreatment;
        this.isPersonalized = isPersonalized;
        this.maxAdContentRating = maxAdContentRating;
        this.appId = appId == null ? "" : appId;

        this.requestConfiguration = this.createRequestConfiguration();

        if (!isPersonalized) {
            // https://developers.google.com/admob/android/eu-consent#forward_consent_to_the_google_mobile_ads_sdk
            if (extras == null) {
                extras = new Bundle();
            }
            extras.putString("npa", "1");
        }

        Log.d("godot", "AdMob: init with content rating options");
    }


    private RequestConfiguration createRequestConfiguration() {
        RequestConfiguration.Builder builder = new RequestConfiguration.Builder();
        if (!this.isReal) {
            List<String> testDeviceIds = Arrays.asList("B3EEABB8EE11C2D", getAdMobDeviceId());
            builder.setTestDeviceIds(testDeviceIds);
        }

        if (this.isForChildDirectedTreatment) {
            builder.setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD);
        }

        // StringEquality false positive
        //noinspection StringEquality
        if (this.maxAdContentRating != null && !this.maxAdContentRating.isEmpty()) {
            RequestConfiguration.MaxAdContentRating rating = RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G;
            if ("PG".equals(this.maxAdContentRating)) rating = RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG;
            if ("T".equals(this.maxAdContentRating)) rating = RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_T;
            if ("MA".equals(this.maxAdContentRating)) rating = RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_MA;
            builder.setMaxAdContentRating(rating);
        }
        return builder.build();
    }


    /**
     * Returns AdRequest object constructed considering the extras.
     *
     * @return AdRequest object
     */
    private AdRequest getAdRequest(final String id) {
        AdRequest.Builder adBuilder = new AdRequest.Builder(id);
        AdRequest adRequest;
        if (!this.isForChildDirectedTreatment && extras != null) {
            adBuilder.setGoogleExtrasBundle(extras);
        }

        adRequest = adBuilder.build();
        return adRequest;
    }

    private void emitSignalOnRenderThread(final String signal, final Object... args) {
        getGodot().runOnRenderThread(() -> emitSignal(signal, args));
    }

    /**
     * To Initializes AdMob on a background thread to improve performance.
     */
    @UsedByGodot
    public void initializeOnBackgroundThread() {
        new Thread(() -> {
            String resolvedAppId = resolveAppId();
            if (resolvedAppId.isEmpty()) {
                Log.e("godot", "AdMob: missing application ID. Set AdMob.app_id or add com.google.android.gms.ads.APPLICATION_ID to AndroidManifest.xml.");
                return;
            }

            try {
                InitializationConfig config = new InitializationConfig.Builder(resolvedAppId)
                    .setRequestConfiguration(requestConfiguration == null
                        ? createRequestConfiguration()
                        : requestConfiguration)
                    .build();
                MobileAds.initialize(activity, config, new OnAdapterInitializationCompleteListener() {
                    @Override
                    public void onAdapterInitializationComplete(InitializationStatus status) {
                        emitSignalOnRenderThread("on_admob_initialized");
                    }
                });
            } catch (RuntimeException exception) {
                Log.e("godot", "AdMob: failed to initialize GMA Next-Gen SDK", exception);
            }
        }).start();
    }

    private String resolveAppId() {
        if (appId != null && !appId.trim().isEmpty()) {
            return appId.trim();
        }

        try {
            ApplicationInfo applicationInfo = activity.getPackageManager().getApplicationInfo(
                    activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metadata = applicationInfo.metaData;
            if (metadata != null) {
                String manifestAppId = metadata.getString("com.google.android.gms.ads.APPLICATION_ID", "");
                if (manifestAppId != null) {
                    return manifestAppId.trim();
                }
            }
        } catch (PackageManager.NameNotFoundException exception) {
            Log.e("godot", "AdMob: could not read application metadata", exception);
        }

        return "";
    }

    /* Rewarded Video
     * ********************************************************************** */

    /**
     * Load a Rewarded Video
     *
     * @param id AdMod Rewarded video ID
     */
    @UsedByGodot
    public void loadRewardedVideo(final String id) {
        activity.runOnUiThread(() -> {
            rewardedVideo = new RewardedVideo(activity, new RewardedVideoListener() {
                @Override
                public void onRewardedVideoLoaded() {
                    emitSignalOnRenderThread("on_rewarded_video_ad_loaded");
                }

                @Override
                public void onRewardedVideoFailedToLoad(int errorCode) {
                    emitSignalOnRenderThread("on_rewarded_video_ad_failed_to_load", errorCode);
                }

                @Override
                public void onRewardedVideoOpened() {
                    emitSignalOnRenderThread("on_rewarded_video_ad_opened");
                }

                @Override
                public void onRewardedVideoClosed() {
                    emitSignalOnRenderThread("on_rewarded_video_ad_closed");
                }

                @Override
                public void onRewarded(String type, int amount) {
                    emitSignalOnRenderThread("on_rewarded", type, amount);
                }

                @Override
                public void onRewardedClicked() {
                    emitSignalOnRenderThread("on_rewarded_clicked");
                }

                @Override
                public void onRewardedAdImpression() {
                    emitSignalOnRenderThread("on_rewarded_impression");
                }
            });
            rewardedVideo.load(id, getAdRequest(id));
        });
    }

    /**
     * Show a Rewarded Video
     */
    @UsedByGodot
    public void showRewardedVideo() {
        activity.runOnUiThread(() -> {
            if (rewardedVideo == null) {
                return;
            }
            rewardedVideo.show();
        });
    }

    /* Rewarded Interstitial
     * ********************************************************************** */

    /**
     * Load a Rewarded Interstitial
     *
     * @param id AdMod Rewarded interstitial ID
     */
    @UsedByGodot
    public void loadRewardedInterstitial(final String id) {
        activity.runOnUiThread(() -> {
            rewardedInterstitial = new RewardedInterstitial(activity, new RewardedInterstitialListener() {
                @Override
                public void onRewardedInterstitialLoaded() {
                    emitSignalOnRenderThread("on_rewarded_interstitial_ad_loaded");
                }

                @Override
                public void onRewardedInterstitialOpened() {
                    emitSignalOnRenderThread("on_rewarded_interstitial_ad_opened");
                }

                @Override
                public void onRewardedInterstitialClosed() {
                    emitSignalOnRenderThread("on_rewarded_interstitial_ad_closed");
                }

                @Override
                public void onRewardedInterstitialFailedToLoad(int errorCode) {
                    emitSignalOnRenderThread("on_rewarded_interstitial_ad_failed_to_load", errorCode);
                }

                @Override
                public void onRewardedInterstitialFailedToShow(int errorCode) {
                    emitSignalOnRenderThread("on_rewarded_interstitial_ad_failed_to_show", errorCode);
                }

                @Override
                public void onRewarded(String type, int amount) {
                    emitSignalOnRenderThread("on_rewarded", type, amount);
                }

                @Override
                public void onRewardedClicked() {
                    emitSignalOnRenderThread("on_rewarded_clicked");
                }

                @Override
                public void onRewardedAdImpression() {
                    emitSignalOnRenderThread("on_rewarded_impression");
                }
            });
            rewardedInterstitial.load(id, getAdRequest(id));
        });
    }

    /**
     * Show a Rewarded Interstitial
     */
    @UsedByGodot
    public void showRewardedInterstitial() {
        activity.runOnUiThread(() -> {
            if (rewardedInterstitial == null) {
                return;
            }
            rewardedInterstitial.show();
        });
    }


    /* Banner
     * ********************************************************************** */

    /**
     * Load a banner
     *
     * @param id      AdMod Banner ID
     * @param isOnTop To made the banner top or bottom
     */
    @UsedByGodot
    public void loadBanner(final String id, final boolean isOnTop, final String bannerSize) {
        activity.runOnUiThread(() -> {
            if (banner != null) banner.remove();
            banner = new Banner(id, getAdRequest(id), extras, activity, new BannerListener() {
                @Override
                public void onBannerLoaded() {
                    emitSignalOnRenderThread("on_admob_ad_loaded");
                }

                @Override
                public void onBannerFailedToLoad(int errorCode) {
                    emitSignalOnRenderThread("on_admob_banner_failed_to_load", errorCode);
                }
            }, isOnTop, layout, bannerSize);
        });
    }

    /**
     * Show the banner
     */
    @UsedByGodot
    public void showBanner() {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.show();
            }
        });
    }

    /**
     * Resize the banner
     * @param isOnTop To made the banner top or bottom
     */
    @UsedByGodot
    public void move(final boolean isOnTop) {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.move(isOnTop);
            }
        });
    }

    /**
     * Resize the banner
     */
    @UsedByGodot
    public void resize() {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.resize();
            }
        });
    }


    /**
     * Hide the banner
     */
    @UsedByGodot
    public void hideBanner() {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.hide();
            }
        });
    }

    /**
     * Get the banner width
     *
     * @return int Banner width
     */
    @UsedByGodot
    public int getBannerWidth() {
        if (banner != null) {
            return banner.getWidth();
        }
        return 0;
    }

    /**
     * Get the banner height
     *
     * @return int Banner height
     */
    @UsedByGodot
    public int getBannerHeight() {
        if (banner != null) {
            return banner.getHeight();
        }
        return 0;
    }

    /* Interstitial
     * ********************************************************************** */

    /**
     * Load a interstitial
     *
     * @param id AdMod Interstitial ID
     */
    @UsedByGodot
    public void loadInterstitial(final String id) {
        activity.runOnUiThread(() -> interstitial = new Interstitial(id, getAdRequest(id), activity, new InterstitialListener() {
            @Override
            public void onInterstitialLoaded() {
                emitSignalOnRenderThread("on_interstitial_loaded");
            }

            @Override
            public void onInterstitialFailedToLoad(int errorCode) {
                emitSignalOnRenderThread("on_interstitial_failed_to_load", errorCode);
            }

            @Override
            public void onInterstitialOpened() {
                // Not Implemented
                emitSignalOnRenderThread("on_interstitial_opened");
            }

            @Override
            public void onInterstitialClosed() {
                emitSignalOnRenderThread("on_interstitial_close");
            }

            @Override
            public void onInterstitialClicked() {
                emitSignalOnRenderThread("on_interstitial_clicked");
            }

            @Override
            public void onInterstitialImpression() {
                emitSignalOnRenderThread("on_interstitial_impression");
            }
        }));
    }

    /**
     * Show the interstitial
     */
    @UsedByGodot
    public void showInterstitial() {
        activity.runOnUiThread(() -> {
            if (interstitial != null) {
                interstitial.show();
            }
        });
    }

    /* ConsentInformation
     * ********************************************************************** */
    @UsedByGodot
    public void requestConsentInfoUpdate(final boolean testingConsent){

        activity.runOnUiThread(() -> cmp = new CMP(activity,
                testingConsent,
                testingConsent ? getAdMobDeviceId() : "",
                new CMPListener() {
            @Override
            public void onConsentInfoUpdateSuccess() {
                emitSignalOnRenderThread("on_consent_info_update_success");
            }

            @Override
            public void onConsentInfoUpdateFailure(int errorCode, String errorMessage) {
                emitSignalOnRenderThread("on_consent_info_update_failure", errorCode, errorMessage);
            }

            @Override
            public void onAppCanRequestAds(int consentStatus) {
                emitSignalOnRenderThread("on_app_can_request_ads", consentStatus);
            }
        }));
    }

    @UsedByGodot
    public void resetConsentInformation(){
        Log.w("godot", "Removing consent: ");
        if(cmp != null) {
            cmp.resetConsentInformation();
        }
    }

    /* Utils
     * ********************************************************************** */

    /**
     * Generate MD5 for the deviceID
     *
     * @param s The string to generate de MD5
     * @return String The MD5 generated
     */
    private String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            //Logger.logStackTrace(TAG,e);
        }
        return "";
    }

    /**
     * Get the Device ID for AdMob
     *
     * @return String Device ID
     */
    private String getAdMobDeviceId() {
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = md5(android_id).toUpperCase(Locale.US);
        return deviceId;
    }

}
