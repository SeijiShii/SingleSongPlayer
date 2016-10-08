package net.c_kogyo.singlesongplayer.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.c_kogyo.singlesongplayer.R;
import net.c_kogyo.singlesongplayer.services.SongPlayService;

import java.io.File;

/**
 * Created by SeijiShii on 2016/10/07.
 */

public class SongPlayDialog extends DialogFragment {

    public static final String TRACK_DURATION = SongPlayService.class.getName() + "track_duration";
    public static final String TRACK_PROGRESS = SongPlayService.class.getName() + "track_progress";

    public static final String ACTION_PLAY_OR_PAUSE = SongPlayService.class.getName() + "_action_play_or_pause";
    public static final String ACTION_STOP          = SongPlayService.class.getName() + "_action_stop";
    public static final String ACTION_FADE_IN_OUT   = SongPlayService.class.getName() + "action_fade_in_out";

    public static final String PROGRESS_CHANGED = SongPlayService.class.getName() + "progress_changed";

    public static SongPlayDialog newInstance(String filePath) {

        Bundle args = new Bundle();

        args.putString(SongPlayService.FILE_PATH, filePath);

        SongPlayDialog fragment = new SongPlayDialog();
        fragment.setArguments(args);
        return fragment;
    }

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver receiver;
    private MediaMetadataRetriever retriever;
    private View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String filePath = getArguments().getString(SongPlayService.FILE_PATH);
        if (filePath == null) return null;

        broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };

        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        view = LayoutInflater.from(getActivity()).inflate(R.layout.song_play_dialog, null);
        builder.setView(view);

        initImage();
        initTitleText();
        initAlbumText();
        initSeekBar();
        initFadeOutButton();
        initPlayPauseButton();
        initStopButton();

        builder.setCancelable(false);

        return builder.create();
    }

    private void initImage() {

        ImageView image = (ImageView) view.findViewById(R.id.image);
        image.setImageBitmap(getBitmap());

    }

    private Bitmap getBitmap() {

        byte[] data = retriever.getEmbeddedPicture();

        if (data != null) {
            return  BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }

    private void initTitleText() {

        TextView titleText = (TextView) view.findViewById(R.id.title_text);
        String titleString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

        titleText.setText(titleString);
    }

    private void initAlbumText() {

        TextView albumText = (TextView) view.findViewById(R.id.album_text);
        String albumString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

        albumText.setText(albumString);
    }

    private SeekBar seekBar;
    private void initSeekBar() {

        seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void initFadeOutButton() {

        Button fadeOutButton = (Button) view.findViewById(R.id.fade_out_button);
        fadeOutButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        view.setAlpha(0.3f);

                        return true;
                    case MotionEvent.ACTION_UP:

                        view.setAlpha(1f);

                        return true;
                }

                return false;
            }
        });
    }

    private void initPlayPauseButton() {

        Button playPauseButton = (Button) view.findViewById(R.id.play_pause_button);
        playPauseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        view.setAlpha(0.3f);

                        return true;
                    case MotionEvent.ACTION_UP:

                        view.setAlpha(1f);

                        return true;
                }

                return false;
            }
        });
    }

    private void initStopButton() {

        Button stopButton = (Button) view.findViewById(R.id.stop_button);
        stopButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        view.setAlpha(0.3f);

                        return true;
                    case MotionEvent.ACTION_UP:

                        broadcastManager.sendBroadcast(new Intent(ACTION_STOP));
                        view.setAlpha(1f);

                        dismiss();

                        return true;
                }

                return false;
            }
        });
    }
}
