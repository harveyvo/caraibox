package men.harveyvo.caraibox;

import android.annotation.SuppressLint;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import java.util.concurrent.TimeUnit;

import men.harveyvo.caraibox.databinding.ActivityFullscreenBinding;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    // Audio manager instance to manage or
    // handle the audio interruptions
    AudioManager audioManager;

    // Audio attributes instance to set the playback
    // attributes for the media player instance
    // these attributes specify what type of media is
    // to be played and used to callback the audioFocusChangeListener
    AudioAttributes playbackAttributes;

    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Runnable focusAudioRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            try {
                audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
//                audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                // initiate the audio playback attributes
                playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();

                // set the playback attributes for the focus requester
                AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build();

                // request the audio focus and
                // store it in the int variable
                final int audioFocusRequest = audioManager.requestAudioFocus(focusRequest);

            } catch (Exception exception) {
                Log.e("AUDIO FOCUS", exception.getMessage());
            }
        }
    };

    private final Runnable abandonAudioRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                int focusGainResult = audioManager.abandonAudioFocus(null);
            } catch (Exception exception) {
                Log.e("AUDIO FOCUS", exception.getMessage());
            }
        }
    };

    private final Runnable exitAppRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                finishAndRemoveTask();
            } catch (Exception exception) {

            }
        }
    };

    private ActivityFullscreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mContentView = binding.fullscreenContent;
        delayedHide(100);
        doAction();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void doAction() {
        mHideHandler.postDelayed(focusAudioRunnable, 0);
        mHideHandler.postDelayed(abandonAudioRunnable, 1000);
        mHideHandler.postDelayed(this::openDefaultApp, 2000);
    }


    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void openDefaultApp() {
        Context ctx = this; // or you can replace **'this'** with your **ActivityName.this**
        String firstApp = "com.vietmap.S2OBU";
        String secondApp = "com.vietmap.s1OBU";
        String thirdApp = "com.google.android.apps.maps";
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(firstApp);
            ctx.startActivity(i);
        } catch (Exception e) {
            Log.e("Start App", e.getMessage());
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(secondApp);
                ctx.startActivity(i);
            } catch (Exception e2) {
                try {
                    Intent i = ctx.getPackageManager().getLaunchIntentForPackage(thirdApp);
                    ctx.startActivity(i);
                } catch (Exception e3) {
                    Log.e("Start App", e.getMessage());
                }
            }
        }
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // media player is handled according to the
    // change in the focus which Android system grants for
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.i("AUDIO FOCUS", "AUDIO FOCUS_GAIN");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                Log.i("AUDIO FOCUS", "AUDIOFOCUS_LOSS_TRANSIENTN");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.i("AUDIO FOCUS", "AUDIO FOCUS LOSS");
            }
        }
    };

}