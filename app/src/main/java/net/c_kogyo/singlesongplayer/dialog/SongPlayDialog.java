package net.c_kogyo.singlesongplayer.dialog;

import android.animation.Animator;
import android.animation.ValueAnimator;
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
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.c_kogyo.singlesongplayer.R;
import net.c_kogyo.singlesongplayer.services.SongPlayService;

/**
 * Created by SeijiShii on 2016/10/07.
 */

public class SongPlayDialog extends DialogFragment{

    public static final String ACTION_PLAY_PAUSE    = SongPlayDialog.class.getName() + "_action_play_pause";
    public static final String ACTION_STOP          = SongPlayDialog.class.getName() + "_action_stop";
    public static final String ACTION_FADE_IN_OUT   = SongPlayDialog.class.getName() + "action_fade_in_out";

    public static final String PROGRESS_CHANGED = SongPlayDialog.class.getName() + "progress_changed";
    public static final String CHANGED_PROGRESS = SongPlayDialog.class.getName() + "changed_progress";

    public static SongPlayDialog newInstance(String filePath, int duration) {

        Bundle args = new Bundle();

        args.putString(SongPlayService.FILE_PATH, filePath);
        args.putInt(SongPlayService.DURATION, duration);

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

        duration = getArguments().getInt(SongPlayService.DURATION);
        durationString = getTimeString(duration);

        initBroadcast();

        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        view = LayoutInflater.from(getActivity()).inflate(R.layout.song_play_dialog, null);
        builder.setView(view);

        initImage();
        initTitleText();
        initAlbumText();
        initDurationText();
        initSeekBar();
        initFadeOutButton();
        initPlayPauseButton();
        initStopButton();

        builder.setCancelable(false);

        return builder.create();
    }

    private void initBroadcast() {

        broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (action.equals(SongPlayService.ACTION_PLAY_PAUSE_STATE_CHANGED)) {

                    boolean isPlaying = intent.getBooleanExtra(SongPlayService.IS_PLAYING, false);
                    updatePlayPauseButton(isPlaying);

                } else if (action.equals(SongPlayService.ACTION_FADING_STARTED)) {

                    isFading = true;
                    flashFadeOutButton();

                } else if (action.equals(SongPlayService.ACTION_FADING_REVERTED)) {

                    isFading = false;
                } else if (action.equals(SongPlayService.ACTION_PLAY_STOPPED)) {

                    isFading = false;
                } else if (action.equals(SongPlayService.ACTION_UPDATE_PROGRESS)) {

                    int currentPosition = intent.getIntExtra(SongPlayService.CURRENT_POSITION, 0);
                    updateDurationText(currentPosition);
                    updateSeekBar(currentPosition);

                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(SongPlayService.ACTION_PLAY_PAUSE_STATE_CHANGED);
        intentFilter.addAction(SongPlayService.ACTION_FADING_STARTED);
        intentFilter.addAction(SongPlayService.ACTION_FADING_REVERTED);
        intentFilter.addAction(SongPlayService.ACTION_PLAY_COMPLETED);
        intentFilter.addAction(SongPlayService.ACTION_UPDATE_PROGRESS);
        intentFilter.addAction(SongPlayService.ACTION_PLAY_STOPPED);
        broadcastManager.registerReceiver(receiver, intentFilter);

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

    private int duration;
    private String durationString;
    private TextView durationText;
    private KillableSeekbarListener seekbarListener;

    private void initDurationText() {
        durationText = (TextView) view.findViewById(R.id.duration_text);
        updateDurationText(0);
    }

    private void updateDurationText(int currentPosition) {

        String string = getTimeString(currentPosition) + " / " + durationString;
        durationText.setText(string);

    }

    private SeekBar seekBar;
    private void initSeekBar() {

        seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        seekBar.setMax(duration);
        seekbarListener = new KillableSeekbarListener();
        seekBar.setOnSeekBarChangeListener(seekbarListener);

    }

    private void updateSeekBar(int currentPosition) {

        // リスナが相互に干渉して音切れになるため一度kill
        seekbarListener.kill(true);
        seekBar.setProgress(currentPosition);
        seekbarListener.kill(false);

    }

    class KillableSeekbarListener implements SeekBar.OnSeekBarChangeListener {

        boolean killed;

        public boolean isKilled() {
            return killed;
        }

        public void kill(boolean kill) {
            this.killed = kill;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            if (killed) return;

            Intent progressChangedIntent = new Intent(PROGRESS_CHANGED);
            progressChangedIntent.putExtra(CHANGED_PROGRESS, i);
            broadcastManager.sendBroadcast(progressChangedIntent);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    private View fadeOutButton;
    private void initFadeOutButton() {

        fadeOutButton = view.findViewById(R.id.fade_out_button);
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

    // フェードアウトが始まってから正常に戻るまでtrue
    private boolean isFading;
    private void flashFadeOutButton() {

        ValueAnimator animator0 = ValueAnimator.ofFloat(1f, 0f);
        animator0.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                fadeOutButton.setAlpha((float) valueAnimator.getAnimatedValue());
                fadeOutButton.requestLayout();
            }
        });
        animator0.setDuration(500);
        animator0.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

                ValueAnimator animator1 = ValueAnimator.ofFloat(0f, 1f);
                animator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        fadeOutButton.setAlpha((float) valueAnimator.getAnimatedValue());
                        fadeOutButton.requestLayout();
                    }
                });
                animator1.setDuration(500);
                animator1.start();

                animator1.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {

                        // 再帰的に繰り返す
                        if (isFading) {
                            flashFadeOutButton();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator0.start();

        // 新しい方法を試してみたけど古い方法のほうがアニメーションがきれい
//        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                fadeOutButton.setAlpha((float) valueAnimator.getAnimatedValue());
//                fadeOutButton.requestLayout();
//            }
//        });
//        animator.setDuration(1000);
//        animator.setRepeatCount(ValueAnimator.REVERSE);
//        animator.start();
//
//        animator.addListener(new Animator.AnimatorListener() {
//            @Override
//            public void onAnimationStart(Animator animator) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animator animator) {
//
//                if (!isFading) {
//                    animator.cancel();
//                    fadeOutButton.setAlpha(1f);
//                }
//            }
//
//            @Override
//            public void onAnimationCancel(Animator animator) {
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animator animator) {
//
//            }
//        });
    }

    private View playPauseButton;
    private ImageView playPauseIcon;
    private void initPlayPauseButton() {

        playPauseButton = view.findViewById(R.id.play_pause_button);
        playPauseIcon = (ImageView) view.findViewById(R.id.play_pause_icon);

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

            playPauseIcon.setBackgroundResource(R.drawable.pause);

        } else {

            playPauseIcon.setBackgroundResource(R.drawable.play);
        }
    }

    private void initStopButton() {

        View stopButton = view.findViewById(R.id.stop_button);
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

    private String getTimeString(int time){

        final int second = 1000;
        final int minute = second * 60;
        final int hour = minute * 60;

        StringBuilder builder = new StringBuilder();

        if (time / hour > 0) {

            builder.append(String.valueOf(time / hour)).append(":");
        }
        time = time - time / hour * hour;

        if (time / minute >= 10) {
            builder.append(String.valueOf(time / minute)).append(":");
        } else if (time / minute > 0) {
            builder.append("0").append(String.valueOf(time / minute)).append(":");
        } else {
            builder.append("0:");
        }
        time = time - time / minute * minute;
        time = time / second;

        builder.append(String.format("%02d", time));

        return builder.toString();
    }
}
