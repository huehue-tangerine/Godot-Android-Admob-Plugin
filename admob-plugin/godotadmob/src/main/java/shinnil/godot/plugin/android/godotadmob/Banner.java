package shinnil.godot.plugin.android.godotadmob;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.libraries.ads.mobile.sdk.banner.*;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;

interface BannerListener {
    void onBannerLoaded();
    void onBannerFailedToLoad(int code);
}

public class Banner {

    private final Activity activity;
    private final FrameLayout layout;
    private final BannerListener listener;
    private final String sizeName;
    private final Bundle extras;
    private final String adUnitId;
    
    private AdView view;
    private BannerAd ad;
    private FrameLayout.LayoutParams params;
    private int currentGravity;

    public Banner(String id, AdRequest request, Bundle extras, Activity activity, BannerListener listener, boolean top, FrameLayout layout, String sizeName) {
        this.activity = activity;
        this.layout = layout;
        this.listener = listener;
        this.sizeName = sizeName;
        this.extras = extras;
        this.adUnitId = id;
        this.currentGravity = top ? Gravity.TOP : Gravity.BOTTOM;
        
        add(this.currentGravity);
    }

private void add(int gravity) {

        activity.runOnUiThread(() -> {
            AdSize size = getSize(sizeName);
            params = new FrameLayout.LayoutParams(-1, -2);
            params.gravity = gravity;
            
            BannerAdRequest.Builder builder = new BannerAdRequest.Builder(adUnitId, size);
            if (extras != null) {
                builder.setGoogleExtrasBundle(extras);
            }
            
            BannerAd.load(builder.build(), new AdLoadCallback<BannerAd>() {
                @Override
                public void onAdLoaded(BannerAd value) {

                    activity.runOnUiThread(() -> {
                        ad = value;
                        view = new AdView(activity);
                        view.setLayoutParams(params);
                        view.registerBannerAd(value, activity);
                        layout.addView(view);
                        listener.onBannerLoaded();
                    });
                }

                @Override
                public void onAdFailedToLoad(LoadAdError e) {
                    android.util.Log.e("godot", "AdMob: banner failed to load: " + e);
                    activity.runOnUiThread(() -> {
                        listener.onBannerFailedToLoad(e.getCode().getValue());
                    });
                }
            });
        });
    }

    public void show() {
        activity.runOnUiThread(() -> {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hide() {
        activity.runOnUiThread(() -> {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        });
    }

    public void move(boolean top) {
        activity.runOnUiThread(() -> {
            currentGravity = top ? Gravity.TOP : Gravity.BOTTOM;
            if (view != null && params != null) {

                params.gravity = currentGravity;
                view.setLayoutParams(params);
            }
        });
    }

    public void resize() {
        activity.runOnUiThread(() -> {
            remove(); 
            add(currentGravity);
        });
    }

    public void remove() {
        activity.runOnUiThread(() -> {
            if (view != null) {
                layout.removeView(view);
                view.destroy();
                view = null;
            }
            if (ad != null) {
                ad.destroy();
                ad = null;
            }
        });
    }

    private AdSize getSize(String n) {
        switch (n) {
            case "BANNER": return AdSize.BANNER;
            case "LARGE_BANNER": return AdSize.LARGE_BANNER;
            case "MEDIUM_RECTANGLE": return AdSize.MEDIUM_RECTANGLE;
            case "FULL_BANNER": return AdSize.FULL_BANNER;
            case "LEADERBOARD": return AdSize.LEADERBOARD;
            default:
                DisplayMetrics m = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(m);
                return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, (int) (m.widthPixels / m.density));
        }
    }

    public int getWidth() {
        return ad == null ? 0 : ad.getAdSize().getWidthInPixels(activity);
    }

    public int getHeight() {
        return ad == null ? 0 : ad.getAdSize().getHeightInPixels(activity);
    }
}