package shinnil.godot.plugin.android.godotadmob;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.google.android.libraries.ads.mobile.sdk.common.*;
import com.google.android.libraries.ads.mobile.sdk.interstitial.*;

interface InterstitialListener {
    void onInterstitialLoaded();
    void onInterstitialFailedToLoad(int code);
    void onInterstitialOpened();
    void onInterstitialClosed();
    void onInterstitialClicked();
    void onInterstitialImpression();
}

public class Interstitial {

    private final String adUnitId; 
    private final AdRequest request;
    private final Activity activity;
    private final InterstitialListener listener;
    private InterstitialAd ad;

    public Interstitial(String id, AdRequest request, Activity activity, InterstitialListener listener) {
        this.adUnitId = id; 
        this.request = request;
        this.activity = activity;
        this.listener = listener;
        load();
    }

    public void show() {
        activity.runOnUiThread(() -> {
            if (ad != null) {
                ad.show(activity);
            } else {
                android.util.Log.w("godot", "AdMob: Attempt to display interstitial before loading.");
            }
        });
    }

    public boolean isLoaded() {
        return ad != null;
    }

    private void setAd(InterstitialAd v) {
        if (ad != null) {
            ad.setAdEventCallback(null);
        }
        
        ad = v;
        
        if (ad != null) {
            ad.setAdEventCallback(new InterstitialAdEventCallback() {
                @Override
                public void onAdClicked() {
                    listener.onInterstitialClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    setAd(null);
                    listener.onInterstitialClosed();
                    load();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError e) {
                    listener.onInterstitialFailedToLoad(e.getCode().getValue());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    listener.onInterstitialOpened();
                }

                @Override
                public void onAdImpression() {
                    listener.onInterstitialImpression();
                }
            });
        }
    }

    private void load() {
        activity.runOnUiThread(() -> {
            InterstitialAd.load(request, new AdLoadCallback<InterstitialAd>() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd v) {
                    activity.runOnUiThread(() -> {
                        setAd(v);
                        listener.onInterstitialLoaded();
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError e) {
                     activity.runOnUiThread(() -> {
                        setAd(null);
                        listener.onInterstitialFailedToLoad(e.getCode().getValue());
                    });
                }
            });
        });
    }
}