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
        activity.runOnUiThread(() -> {
            RewardedInterstitialAd.load(request, new AdLoadCallback<RewardedInterstitialAd>() {
                @Override
                public void onAdLoaded(@NonNull RewardedInterstitialAd v) {
                    activity.runOnUiThread(() -> {
                        setAd(v);
                        listener.onRewardedInterstitialLoaded();
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError e) {
                    activity.runOnUiThread(() -> {
                        setAd(null);
                        listener.onRewardedInterstitialFailedToLoad(e.getCode().getValue());
                    });
                }
            });
        });
    }

    public void show() {
        activity.runOnUiThread(() -> {
            if (ad != null) {
                ad.show(activity, r -> listener.onRewarded(r.getType(), r.getAmount()));
            } else {
                android.util.Log.w("godot", "AdMob: Attempt to show RewardedInterstitial before loading or after it has already been consumed.");
            }
        });
    }

    private void setAd(RewardedInterstitialAd v) {
        if (ad != null) {
            ad.setAdEventCallback(null);
        }
        
        ad = v;
        
        if (ad != null) {
            ad.setAdEventCallback(new RewardedInterstitialAdEventCallback() {
                @Override
                public void onAdClicked() {
                    listener.onRewardedClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    setAd(null);
                    listener.onRewardedInterstitialClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError e) {
                    listener.onRewardedInterstitialFailedToShow(e.getCode().getValue());
                }

                @Override
                public void onAdImpression() {
                    listener.onRewardedAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    listener.onRewardedInterstitialOpened();
                }
            });
        }
    }
}