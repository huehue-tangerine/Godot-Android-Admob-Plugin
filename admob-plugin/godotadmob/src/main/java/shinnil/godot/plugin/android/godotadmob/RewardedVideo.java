package shinnil.godot.plugin.android.godotadmob;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.google.android.libraries.ads.mobile.sdk.common.*;
import com.google.android.libraries.ads.mobile.sdk.rewarded.*;

interface RewardedVideoListener {
    void onRewardedVideoLoaded();
    void onRewardedVideoFailedToLoad(int code);
    void onRewardedVideoOpened();
    void onRewardedVideoClosed();
    void onRewarded(String type, int amount);
    void onRewardedClicked();
    void onRewardedAdImpression();
}

public class RewardedVideo {

    private RewardedAd ad;
    private final Activity activity;
    private final RewardedVideoListener listener;

    public RewardedVideo(Activity activity, RewardedVideoListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public boolean isLoaded() {
        return ad != null;
    }

    public void load(String id, AdRequest request) {
        activity.runOnUiThread(() -> {
            RewardedAd.load(request, new AdLoadCallback<RewardedAd>() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd v) {
                    activity.runOnUiThread(() -> {
                        setAd(v);
                        listener.onRewardedVideoLoaded();
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError e) {
                    activity.runOnUiThread(() -> {
                        setAd(null);
                        listener.onRewardedVideoFailedToLoad(e.getCode().getValue());
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
                android.util.Log.w("godot", "AdMob: Attempt to show RewardedVideo before loading or after it has already been consumed.");
            }
        });
    }

    private void setAd(RewardedAd v) {
        if (ad != null) {
            ad.setAdEventCallback(null);
        }
        
        ad = v;
        
        if (ad != null) {
            ad.setAdEventCallback(new RewardedAdEventCallback() {
                @Override
                public void onAdClicked() {
                    listener.onRewardedClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    setAd(null); 
                    listener.onRewardedVideoClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError e) {
                    listener.onRewardedVideoFailedToLoad(e.getCode().getValue());
                }

                @Override
                public void onAdImpression() {
                    listener.onRewardedAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    listener.onRewardedVideoOpened();
                }
            });
        }
    }
}