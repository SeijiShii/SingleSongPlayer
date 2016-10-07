package net.c_kogyo.singlesongplayer.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import net.c_kogyo.singlesongplayer.R;
import net.c_kogyo.singlesongplayer.activities.MainActivity;

import java.io.File;

import static android.content.Intent.ACTION_DELETE;

/**
 * Created by SeijiShii on 2016/10/07.
 */

public class SongPlayService extends Service {

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

        mContext = getApplicationContext();

        mPlayer = new MediaPlayer();

        broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        broadcastManager.registerReceiver(receiver, new IntentFilter(MainActivity.PLAY_OR_PAUSE));
        broadcastManager.registerReceiver(receiver, new IntentFilter(MainActivity.STOP_PLAY));
        broadcastManager.registerReceiver(receiver, new IntentFilter(MainActivity.FADE_IN_OUT));
        broadcastManager.registerReceiver(receiver, new IntentFilter(MainActivity.PROGRESS_CHANGED));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


            }
        };
    }

    private File mFile;
    private MediaPlayer mPlayer;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String filePath = intent.getStringExtra(MainActivity.SONG_FILE_PATH);

        if (filePath == null) return START_NOT_STICKY;

        mFile = new File(filePath);




        return START_STICKY;
    }

    NotificationCompat.Builder mBuilder;
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

    }
}
