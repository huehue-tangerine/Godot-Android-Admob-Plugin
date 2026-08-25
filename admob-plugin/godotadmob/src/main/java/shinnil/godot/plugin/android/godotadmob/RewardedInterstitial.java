package shinnil.godot.plugin.android.godotadmob;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.google.android.libraries.ads.mobile.sdk.common.*;
import com.google.android.libraries.ads.mobile.sdk.rewarded.*;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.*;

interface RewardedInterstitialListener {

    void onRewardedInterstitialLoaded();

    void onRewardedInterstitialOpened();

    void onRewardedInterstitialClosed();

    void onRewardedInterstitialFailedToLoad(int code);

    void onRewardedInterstitialFailedToShow(int code);

    void onRewarded(String type, int amount);

    void onRewardedClicked();

    void onRewardedAdImpression();
}

public class RewardedInterstitial {

    private RewardedInterstitialAd ad;
    private final Activity activity;
    private final RewardedInterstitialListener listener;

    public RewardedInterstitial(Activity activity, RewardedInterstitialListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public boolean isLoaded() {
        return ad != null;
    }

    public void load(String id, AdRequest request) {
        RewardedInterstitialAd.load(request, new AdLoadCallback<RewardedInterstitialAd>() {
            public void onAdLoaded(@NonNull RewardedInterstitialAd v) {
                setAd(v);
                listener.onRewardedInterstitialLoaded();
            }

            public void onAdFailedToLoad(@NonNull LoadAdError e) {
                setAd(null);
                listener.onRewardedInterstitialFailedToLoad(e.getCode().getValue());
            }
        });
    }

    public void show() {
        if (ad != null) {
            ad.show(activity, r -> listener.onRewarded(r.getType(), r.getAmount()));
    
        }}

    private void setAd(RewardedInterstitialAd v) {
        if (ad != null) {
            ad.setAdEventCallback(null);
        
        }ad = v;
        if (ad != null) {
            ad.setAdEventCallback(new RewardedInterstitialAdEventCallback() {
                public void onAdClicked() {
                    listener.onRewardedClicked();
                }

                public void onAdDismissedFullScreenContent() {
                    listener.onRewardedInterstitialClosed();
                }

                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError e) {
                    listener.onRewardedInterstitialFailedToShow(e.getCode().getValue());
                }

                public void onAdImpression() {
                    listener.onRewardedAdImpression();
                }

                public void onAdShowedFullScreenContent() {
                    listener.onRewardedInterstitialOpened();
                }
            });
    
        }}
}
