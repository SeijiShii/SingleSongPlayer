package net.c_kogyo.singlesongplayer.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import net.c_kogyo.singlesongplayer.R;
import net.c_kogyo.singlesongplayer.activities.MainActivity;
import net.c_kogyo.singlesongplayer.dialog.SongPlayDialog;

import java.io.File;

import static android.content.Intent.ACTION_DELETE;

/**
 * Created by SeijiShii on 2016/10/07.
 */

public class SongPlayService extends Service{

    public static final String ACTION_PLAY_STARTED = SongPlayService.class.getSimpleName() + "_action_play_started";
    public static final String ACTION_PLAY_COMPLETED = SongPlayService.class.getSimpleName() + "_action_play_completed";
    public static final String ACTION_PLAY_STOPPED = SongPlayService.class.getSimpleName() + "_action_play_stopped";
    public static final String ACTION_PLAY_PAUSE_STATE_CHANGED = SongPlayService.class.getSimpleName() + "_action_play_pause_state_changed";
    public static final String IS_PLAYING = SongPlayService.class.getCanonicalName() + "_is_playing";

    public static final String ACTION_FADING_STARTED = SongPlayService.class.getSimpleName() + "_action_fading_started";
    public static final String ACTION_FADING_REVERTED = SongPlayService.class.getSimpleName() + "_action_fading_reverted";
    public static final String ACTION_UPDATE_PROGRESS = SongPlayService.class.getSimpleName() + "_action_update_progress";

    public static final String FILE_PATH = SongPlayService.class.getSimpleName() + "_file_path";
    public static final String PROGRESS = SongPlayService.class.getSimpleName() + "_progress";


    private boolean isFadingOut;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Context mContext;

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver receiver;
    @Override
    public void onCreate() {
        super.onCreate();

        isFadingOut = false;

        mContext = getApplicationContext();

        broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if (action.equals(SongPlayDialog.ACTION_STOP)) {

                    if (mPlayer.isPlaying()) {
                        mPlayer.stop();
                        notificationManager.cancel(notifyId);
                        stopSelf();
                    }
                } else if (action.equals(SongPlayDialog.ACTION_PLAY_PAUSE)) {

                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                    } else {
                        mPlayer.start();
                    }

                    Intent playStateChangeIntent = new Intent(ACTION_PLAY_PAUSE_STATE_CHANGED);
                    playStateChangeIntent.putExtra(IS_PLAYING, mPlayer.isPlaying());
                    broadcastManager.sendBroadcast(playStateChangeIntent);

                }


//                switch (intent.getAction()) {
//                    case MainActivity.PLAY_OR_PAUSE:
//
//                        if (mPlayer.isPlaying()) {
//                            mPlayer.stop();
//                        } else {
//                            mPlayer.start();
//                        }
//
//                        break;
//                    case MainActivity.STOP_PLAY:
//
//                        if (mPlayer.isPlaying()) {
//                            mPlayer.stop();
//                        }
//
//                        break;
//                    case MainActivity.FADE_IN_OUT:
//
//                        isFadingOut = !isFadingOut;
//
//
//                        break;
//                    case MainActivity.PROGRESS_CHANGED:
//
//                        break;
//                }
            }
        };
    }

    private File mFile;
    private MediaPlayer mPlayer;
    private float currentVolume;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        broadcastManager.registerReceiver(receiver, new IntentFilter(SongPlayDialog.ACTION_STOP));
        broadcastManager.registerReceiver(receiver, new IntentFilter(SongPlayDialog.ACTION_PLAY_PAUSE));

        // 現在のボリュームを追い続けるスレッド
        new Thread(new Runnable() {
            @Override
            public void run() {

                do {
                    AudioManager manager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);
                    int currentVolumeInt = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolumeInt = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    currentVolume = (float) currentVolumeInt / (float) maxVolumeInt;

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }while (true);
            }
        }).start();

        String filePath = intent.getStringExtra(MainActivity.SONG_FILE_PATH);

        if (filePath == null) return START_NOT_STICKY;

        mFile = new File(filePath);

        if (mPlayer == null) {
            mPlayer = MediaPlayer.create(mContext, Uri.fromFile(mFile));
            mPlayer.setVolume(currentVolume, currentVolume);
            mPlayer.start();
            muteOtherStream(true);

            if (mPlayer.isPlaying()) {

                Intent playStartIntent = new Intent(ACTION_PLAY_STARTED);
                playStartIntent.putExtra(FILE_PATH, mFile.getAbsolutePath());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(playStartIntent);
                createNotification();
            }

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_PLAY_COMPLETED));
                    stopSelf();

                }
            });
        }




        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        muteOtherStream(false);
    }

    private NotificationManager notificationManager;
    private NotificationCompat.Builder mBuilder;
    private static int notifyId = 100;
    void createNotification() {

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setSmallIcon(R.drawable.logo_024);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mContext, Uri.fromFile(mFile));
        String trackTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        mBuilder.setContentText(trackTitle);

        Intent dummyIntent = new Intent(mContext, IntentCatcherDummyService.class);
        PendingIntent relaunchPendingIntent = PendingIntent.getService(mContext, 0, dummyIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setContentIntent(relaunchPendingIntent);

        Intent deleteIntent = new Intent(mContext, SongPlayService.class);
        deleteIntent.setAction(ACTION_DELETE);
        PendingIntent deletePendingIntent = PendingIntent.getService(mContext, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setDeleteIntent(deletePendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId, mBuilder.build());

    }

    int alarmVolume, systemVolume, notificationVolume, ringVolume, dtmfVolune;
    private static final String ALARM_VOLUME = "alarm_volume";
    private static final String SYSTEM_VOLUME = "system_volume";
    private static final String NOTIFICATION_VOLUME = "notification_volume";
    private static final String RING_VOLUME = "ring_volume";
    private static final String DTMF_VOLUME = "dtmf_volume";

    private void muteOtherStream(boolean mute) {

        SharedPreferences prefs = getSharedPreferences(MainActivity.SSP_PREF_TAG, MODE_PRIVATE);
        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if (mute) {
            alarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM);
            manager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);

            systemVolume = manager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);

            notificationVolume = manager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);

            ringVolume = manager.getStreamVolume(AudioManager.STREAM_RING);
            manager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);

            dtmfVolune = manager.getStreamVolume(AudioManager.STREAM_DTMF);
            manager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(ALARM_VOLUME, alarmVolume);
            editor.putInt(SYSTEM_VOLUME, systemVolume);
            editor.putInt(NOTIFICATION_VOLUME, notificationVolume);
            editor.putInt(RING_VOLUME, ringVolume);
            editor.putInt(DTMF_VOLUME, dtmfVolune);
            editor.apply();

        }  else {
            prefs.getInt(ALARM_VOLUME, alarmVolume);
            manager.setStreamVolume(AudioManager.STREAM_ALARM, alarmVolume, 0);

            prefs.getInt(SYSTEM_VOLUME, systemVolume);
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolume, 0);

            prefs.getInt(NOTIFICATION_VOLUME, notificationVolume);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificationVolume, 0);

            prefs.getInt(RING_VOLUME, ringVolume);
            manager.setStreamVolume(AudioManager.STREAM_RING, ringVolume, 0);

            prefs.getInt(DTMF_VOLUME, dtmfVolune);
            manager.setStreamVolume(AudioManager.STREAM_DTMF, dtmfVolune, 0);
        }

    }

    private FadeOutRunnable fadeOutRunnable;
    private void fadeOutIn() {

        if (isFadingOut) {

            fadeOutRunnable = new FadeOutRunnable();
            new Thread(fadeOutRunnable).start();

            Intent fadeStartIntent = new Intent(ACTION_FADING_STARTED);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(fadeStartIntent);

        } else {

            Intent fadeRevertIntent = new Intent(ACTION_FADING_REVERTED);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(fadeRevertIntent);
        }

    }

    class FadeOutRunnable implements Runnable {

        @Override
        public void run() {

            int secCounter = 0;
            AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
            int volumeStep = (int)(currentVolume / 50);

            while (secCounter > 0 || secCounter < 5000) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //
                }

                if (isFadingOut) {
                    secCounter += 100;
                    manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, -volumeStep, 0);
                } else {
                    secCounter -= 100;
                    manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, volumeStep, 0);
                }

            }

        }
    }

}
