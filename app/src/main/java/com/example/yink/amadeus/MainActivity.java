package com.example.yink.amadeus;

/*
 * Big thanks to https://github.com/RIP95 aka Emojikage
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Random;

@SuppressLint("StaticFieldLeak")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_PERMISSION_RECORD_AUDIO = 11302;
    private static String recogLang;
    private static SpeechRecognizer speechRecognizer;
    private final VoiceLine[] voiceLines = VoiceLine.Line.getLines();
    private String[] contextLang;
    private final Random randomGen = new Random();
    private Runnable loop, listeningRunnable, answerRunnable;
    private Handler handler;
    private ImageView settingsImg, logoSmall;
    private static Context context;
    private SharedPreferences settings;
    private ImageView subtitlesBackground;
    private TextView subtitles;
    private int permissionCheck;
    private int i = 36;

    public static void promptSpeechInput() {
        speechRecognizerBeepDisabled(context, true);
        speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, recogLang));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LangContext.wrap(newBase));
    }

    public static void speechRecognizerBeepDisabled(Context context, boolean mute) {
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int dndMode = context.getSystemService(NotificationManager.class)
                    .getCurrentInterruptionFilter();
            switch (dndMode) {
                default:
                case NotificationManager.INTERRUPTION_FILTER_UNKNOWN:
                    Log.e(TAG, "DND Mode Checking Result: FILTER_UNKNOWN");
                    break;

                case NotificationManager.INTERRUPTION_FILTER_ALL:
                    Log.e(TAG, "DND Mode Checking Result: FILTER_ALL");
                    int flag = mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE;
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, flag, 0);
                    break;

                case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                    Log.e(TAG, "DND Mode Checking Result: FILTER_PRIORITY");
                    break;

                case NotificationManager.INTERRUPTION_FILTER_NONE:
                    Log.e(TAG, "DND Mode Checking Result: FILTER_NONE");
                    flag = mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE;
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, flag, 0);
                    break;

                case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                    Log.e(TAG, "DND Mode Checking Result: FILTER_ALARMS");
                    break;
            }
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (context == null) context = this.getApplicationContext();

        speechRecognizerBeepDisabled(context, true);

        ImageView kurisu = findViewById(R.id.imageView_kurisu);
        subtitlesBackground = findViewById(R.id.imageView_subtitles);
        subtitles = findViewById(R.id.textView_subtitles);
        settingsImg = findViewById(R.id.imageView_settings);
        logoSmall = findViewById(R.id.imageView_logo_small);

        handler = handler != null ? handler : new Handler(Looper.getMainLooper());

        listeningRunnable = () -> {
            int DURATION = 100;
            i++;
            if (i > 39) i = 37;
            String imgName = "logo" + i;
            int id = getResources().getIdentifier(imgName, "drawable", getPackageName());
            logoSmall.setImageDrawable((ContextCompat.getDrawable(this, id)));
            handler.postDelayed(listeningRunnable, DURATION);
        };

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        recogLang = settings.getString("recognition_lang", "ja-JP");

        contextLang = recogLang.split("-");

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new SpeechListener());

        if (!settings.getBoolean("show_subtitles", false)) {
            subtitlesBackground.setVisibility(View.INVISIBLE);
            subtitles.setVisibility(View.INVISIBLE);
        } else {
            subtitlesBackground.setVisibility(View.VISIBLE);
            subtitles.setVisibility(View.VISIBLE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_RECORD_AUDIO);
        }

        permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        loop = () -> {
            if (Amadeus.isLoop) {
                Amadeus.speak(voiceLines[randomGen.nextInt(voiceLines.length)], MainActivity.this);
                handler.postDelayed(loop, 5000 + randomGen.nextInt(5) * 1000);
            }
        };

        settingsImg.setOnClickListener(v -> startActivity(new Intent(this,
                SettingsActivity.class)));

        kurisu.setOnClickListener(v -> {
            if (!Amadeus.isLoop && !Amadeus.isSpeaking) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        promptSpeechInput();
                        Log.e(TAG, "Permission granted");
                    } else {
                        Log.e(TAG, "Permission not granted");
                        Amadeus.speak(voiceLines[VoiceLine.Line.DAGA_KOTOWARU], MainActivity.this);
                    }
                } else {
                    Log.e(TAG, "No need permission");
                    promptSpeechInput();
                }
            }
        });

        kurisu.setOnLongClickListener(v -> {
            if (!Amadeus.isLoop && !Amadeus.isSpeaking) {
                handler.removeCallbacks(listeningRunnable);
                logoSmall.setImageResource(R.drawable.amadeus_icon_smaller);
                logoSmall.setPadding(dp(10), 0, dp(10), 0);
                handler.post(loop);
                Amadeus.isLoop = true;
                speechRecognizer.cancel();
            } else {
                handler.removeCallbacks(loop);
                Amadeus.isLoop = false;
                if (!Amadeus.isSpeaking) {
                    handler.removeCallbacks(listeningRunnable);
                    handler.post(listeningRunnable);
                    logoSmall.setPadding(0, 0, 0, 0);
                }
            }
            return true;
        });
        if (answerRunnable == null) answerRunnable = () ->
                Amadeus.speak(voiceLines[VoiceLine.Line.HELLO], MainActivity.this);
        handler.postDelayed(answerRunnable, 500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Amadeus.isLoop = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null)
            speechRecognizer.destroy();
        if (Amadeus.m != null)
            Amadeus.m.release();
        if (handler != null) {
            if (loop != null) handler.removeCallbacks(loop);
            if (listeningRunnable != null) handler.removeCallbacks(listeningRunnable);
            if (answerRunnable != null) handler.removeCallbacks(answerRunnable);
        }
        speechRecognizerBeepDisabled(context, false);
        context = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!settings.getBoolean("show_subtitles", false)) {
            subtitlesBackground.setVisibility(View.INVISIBLE);
            subtitles.setVisibility(View.INVISIBLE);
        } else {
            subtitlesBackground.setVisibility(View.VISIBLE);
            subtitles.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Amadeus.isLoop = false;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK && null != data) {

                /* Switch language within current context for voice recognition */
                Context context = LangContext.load(getApplicationContext(), contextLang[0]);

                ArrayList<String> input = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Amadeus.responseToInput(input.get(0), context, MainActivity.this);
            }
        }
    }

    public int dp(float sizeInDp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (sizeInDp * scale + 0.5f);
    }

    private class SpeechListener implements RecognitionListener {

        private final String TAG = SpeechListener.class.getName();

        public void onReadyForSpeech(Bundle params) {
            Log.e(TAG, "Speech recognition start");
            logoSmall.setPadding(0, 0, 0, 0);
            if (handler != null && listeningRunnable != null) {
                handler.removeCallbacks(listeningRunnable);
                handler.post(listeningRunnable);
            }
        }

        public void onBeginningOfSpeech() {
            Log.e(TAG, "Listening speech");
        }

        public void onRmsChanged(float rmsdB) {
//            Log.e(TAG, "onRmsChanged");
        }

        public void onBufferReceived(byte[] buffer) {
            Log.e(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.e(TAG, "Speech recognition end");
        }

        public void onError(int error) {
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                Log.e(TAG, "didn't recognize anything");
                // keep going
                if (!Amadeus.isLoop && !Amadeus.isSpeaking) promptSpeechInput();
            } else {
                // drop voice input but it will automatically trigger again by Amadeus :3
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    Log.e(TAG, "busy...");
                } else {
                    Log.e(TAG, "error " + error);
                    speechRecognizer.cancel();
                    Amadeus.speak(voiceLines[VoiceLine.Line.SORRY], MainActivity.this);
                }
                if (handler != null && listeningRunnable != null)
                    handler.removeCallbacks(listeningRunnable);
                logoSmall.setImageResource(R.drawable.amadeus_icon_smaller);
                logoSmall.setPadding(dp(10), 0, dp(10), 0);
            }
        }

        public void onResults(Bundle results) {
            String input = "";
            StringBuilder debug = new StringBuilder();
            Log.e(TAG, "Received results");
            ArrayList<?> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (Object word : data) {
                debug.append(word).append("\n");
            }
            Log.e(TAG, debug.toString());

            input += data.get(0);
            /* TODO: Japanese doesn't split the words. Sigh. */
            String[] splitInput = input.split(" ");

            /* Really, google? */
            if (splitInput[0].equalsIgnoreCase("Асистент")) {
                splitInput[0] = "Ассистент";
            }

            /* Switch language within current context for voice recognition */
            Context context = LangContext.load(getApplicationContext(), contextLang[0]);

            if (splitInput.length > 2 && splitInput[0].equalsIgnoreCase(context.getString(R.string.assistant))) {
                String cmd = splitInput[1].toLowerCase();
                String[] args = new String[splitInput.length - 2];
                System.arraycopy(splitInput, 2, args, 0, splitInput.length - 2);

                if (cmd.contains(context.getString(R.string.open))) {
                    Amadeus.openApp(args, MainActivity.this);
                }

            } else {
                Amadeus.responseToInput(input, context, MainActivity.this);
            }
            if (handler != null && listeningRunnable != null)
                handler.removeCallbacks(listeningRunnable);
            logoSmall.setImageResource(R.drawable.amadeus_icon_smaller);
            logoSmall.setPadding(dp(10), 0, dp(10), 0);
        }

        public void onPartialResults(Bundle partialResults) {
            Log.e(TAG, "onPartialResults");
        }

        public void onEvent(int eventType, Bundle params) {
            Log.e(TAG, "onEvent " + eventType);
        }

    }
}
