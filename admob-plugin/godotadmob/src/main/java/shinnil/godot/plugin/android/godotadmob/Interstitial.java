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

    private final AdRequest request;
    private final Activity activity;
    private final InterstitialListener listener;
    private InterstitialAd ad;

    public Interstitial(String id, AdRequest request, Activity activity, InterstitialListener listener) {
        this.request = request;
        this.activity = activity;
        this.listener = listener;
        load();
    }

    public void show() {
        if (ad != null) {
            ad.show(activity);
    
        }}

    public boolean isLoaded() {
        return ad != null;
    }

    private void setAd(InterstitialAd v) {
        if (ad != null) {
            ad.setAdEventCallback(null);
        
        }ad = v;
        if (ad != null) {
            ad.setAdEventCallback(new InterstitialAdEventCallback() {
                public void onAdClicked() {
                    listener.onInterstitialClicked();
                }

                public void onAdDismissedFullScreenContent() {
                    setAd(null);
                    listener.onInterstitialClosed();
                    load();
                }

                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError e) {
                    listener.onInterstitialFailedToLoad(e.getCode().getValue());
                }

                public void onAdShowedFullScreenContent() {
                    listener.onInterstitialOpened();
                }

                public void onAdImpression() {
                    listener.onInterstitialImpression();
                }
            });
    
        }}

    private void load() {
        InterstitialAd.load(request, new AdLoadCallback<InterstitialAd>() {
            public void onAdLoaded(@NonNull InterstitialAd v) {
                setAd(v);
                listener.onInterstitialLoaded();
            }

            public void onAdFailedToLoad(@NonNull LoadAdError e) {
                setAd(null);
                listener.onInterstitialFailedToLoad(e.getCode().getValue());
            }
        });
    }
}
