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
        RewardedAd.load(request, new AdLoadCallback<RewardedAd>() {
            public void onAdLoaded(@NonNull RewardedAd v) {
                setAd(v);
                listener.onRewardedVideoLoaded();
            }

            public void onAdFailedToLoad(@NonNull LoadAdError e) {
                setAd(null);
                listener.onRewardedVideoFailedToLoad(e.getCode().getValue());
            }
        });
    }

    public void show() {
        if (ad != null) {
            ad.show(activity, r -> listener.onRewarded(r.getType(), r.getAmount()));
    
        }}

    private void setAd(RewardedAd v) {
        if (ad != null) {
            ad.setAdEventCallback(null);
        
        }ad = v;
        if (ad != null) {
            ad.setAdEventCallback(new RewardedAdEventCallback() {
                public void onAdClicked() {
                    listener.onRewardedClicked();
                }

                public void onAdDismissedFullScreenContent() {
                    listener.onRewardedVideoClosed();
                }

                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError e) {
                    listener.onRewardedVideoFailedToLoad(e.getCode().getValue());
                }

                public void onAdImpression() {
                    listener.onRewardedAdImpression();
                }

                public void onAdShowedFullScreenContent() {
                    listener.onRewardedVideoOpened();
                }
            });
    
        }}
}
