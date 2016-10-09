package net.c_kogyo.singlesongplayer.dialog;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SeijiShii on 2016/10/07.
 */

public class SongPlayDialog extends DialogFragment {

    public static final String TRACK_DURATION = SongPlayDialog.class.getName() + "track_duration";
    public static final String TRACK_PROGRESS = SongPlayDialog.class.getName() + "track_progress";

    public static final String ACTION_PLAY_PAUSE    = SongPlayDialog.class.getName() + "_action_play_pause";
    public static final String ACTION_STOP          = SongPlayDialog.class.getName() + "_action_stop";
    public static final String ACTION_FADE_IN_OUT   = SongPlayDialog.class.getName() + "action_fade_in_out";

    public static final String PROGRESS_CHANGED = SongPlayDialog.class.getName() + "progress_changed";

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

        final String filePath = getArguments().getString(SongPlayService.FILE_PATH);
        if (filePath == null) return null;

        broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (action.equals(SongPlayService.ACTION_PLAY_PAUSE_STATE_CHANGED)) {

                    boolean isPlaying = intent.getBooleanExtra(SongPlayService.IS_PLAYING, false);
                    updatePlayPauseButton(isPlaying);

                } else if (action.equals(SongPlayService.ACTION_FADING_STARTED)) {

                    isFadingOut = true;
                    flashingFadeOutButton();

                } else if (action.equals(SongPlayService.ACTION_FADING_REVERTED)) {

                    isFadingOut = false;

                } else if (action.equals(SongPlayService.ACTION_PLAY_STOPPED)) {

                    dismiss();

                }

            }
        };

        IntentFilter intentFilter = new IntentFilter(SongPlayService.ACTION_PLAY_PAUSE_STATE_CHANGED);
        intentFilter.addAction(SongPlayService.ACTION_FADING_STARTED);
        intentFilter.addAction(SongPlayService.ACTION_FADING_REVERTED);
        intentFilter.addAction(SongPlayService.ACTION_PLAY_STOPPED);
        intentFilter.addAction(SongPlayService.ACTION_PLAY_COMPLETED);
        intentFilter.addAction(SongPlayService.ACTION_UPDATE_PROGRESS);
        broadcastManager.registerReceiver(receiver, intentFilter);

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

    private Button fadeOutButton;
    private void initFadeOutButton() {

        fadeOutButton = (Button) view.findViewById(R.id.fade_out_button);
        fadeOutButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        view.setAlpha(0.3f);

                        return true;
                    case MotionEvent.ACTION_UP:

                        view.setAlpha(1f);

                        broadcastManager.sendBroadcast(new Intent(ACTION_FADE_IN_OUT));

                        return true;
                }

                return false;
            }
        });
    }

    private boolean isFadingOut;
    private void flashingFadeOutButton() {

        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        while (isFadingOut) {

                            flashFadeOut();
                        }
                        fadeOutButton.setAlpha(1f);
                    }
                });
            }
        }).start();

    }

    private void flashFadeOut() {
        List<Animator> animatorList = new ArrayList<>();

        ObjectAnimator animator0 = ObjectAnimator.ofFloat(fadeOutButton, "alpha", 1f, 0f);
        animator0.setDuration(500);

        ObjectAnimator animator1 = ObjectAnimator.ofFloat(fadeOutButton, "alpha", 0f, 1f);
        animator0.setDuration(500);

        animatorList.add(animator0);
        animatorList.add(animator1);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(animatorList);
    }

    private Button playPauseButton;
    private void initPlayPauseButton() {

        playPauseButton = (Button) view.findViewById(R.id.play_pause_button);
        playPauseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        view.setAlpha(0.3f);

                        return true;
                    case MotionEvent.ACTION_UP:

                        view.setAlpha(1f);

                        broadcastManager.sendBroadcast(new Intent(ACTION_PLAY_PAUSE));

                        return true;
                }

                return false;
            }
        });
    }

    private void updatePlayPauseButton(boolean isPlaying) {

        if (isPlaying) {

            playPauseButton.setBackgroundResource(R.drawable.pause);

        } else {

            playPauseButton.setBackgroundResource(R.drawable.play);
        }
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
