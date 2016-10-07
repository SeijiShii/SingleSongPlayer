package net.c_kogyo.singlesongplayer.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.c_kogyo.singlesongplayer.R;

import java.io.File;

/**
 * Created by SeijiShii on 2016/10/02.
 */

public class SoundFileListCell extends LinearLayout {

    private File mFile;
    private int cellHeight;
    private MediaMetadataRetriever mRetriever;
    private boolean mFadeIn;

    private PostCompressListener mCompressListener;

    public SoundFileListCell(Context context, File file, boolean fadeIn, PostCompressListener listener) {

        super(context);

        mCompressListener = listener;
        mFile = file;
        mFadeIn = fadeIn;

        init();
    }

    public SoundFileListCell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private View v;
    private void init() {

        cellHeight = getContext().getResources().getDimensionPixelSize(R.dimen.sound_cell_height);
        mRetriever = new MediaMetadataRetriever();
        mRetriever.setDataSource(mFile.getPath());

        v = inflate(getContext(), R.layout.sound_file_list_cell, this);

        initIconImage();
        initTitleText();
        initSubTitleText();
        initRemoveButton();
        initForTouch();


        if (mFadeIn) {

            this.setAlpha(0f);

            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {

                    while (SoundFileListCell.this.getHeight() <= 0) {

                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            //
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                         animateFadeIn();
                        }
                    });
                }
            }).start();

        }
    }

    private void animateFadeIn() {

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SoundFileListCell.this.setAlpha((float) valueAnimator.getAnimatedValue());
                SoundFileListCell.this.requestLayout();
            }
        });
        animator.setDuration(500);
        animator.start();

    }

    private void initIconImage() {

        ImageView iconImage = (ImageView) v.findViewById(R.id.icon_image);

        Bitmap bitmap = getBitmap();
        if (bitmap != null) {

            iconImage.setImageBitmap(bitmap);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(cellHeight * 0.8), (int) (cellHeight* 0.8));
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

            iconImage.setLayoutParams(params);

        } else {

            iconImage.setBackgroundResource(R.drawable.omp_blue_back);
        }

    }

    private void fadeOut() {

        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SoundFileListCell.this.setAlpha((float) valueAnimator.getAnimatedValue());
                SoundFileListCell.this.requestLayout();
            }
        });
        animator.setDuration(500);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

                compressView();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();

    }

    private void compressView() {

        ValueAnimator animator = ValueAnimator.ofInt(this.cellHeight, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SoundFileListCell.this.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
                SoundFileListCell.this.requestLayout();
            }
        });
        animator.setDuration(300);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

                mCompressListener.postCompress(SoundFileListCell.this);

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    private Bitmap getBitmap() {

        byte[] data = mRetriever.getEmbeddedPicture();

        if (data != null) {
            return  BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }

    private void initTitleText() {

        TextView titleText = (TextView) v.findViewById(R.id.title_text);

        String title = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title == null || title.length() <= 0) {
            title = mFile.getName();
        }
        titleText.setText(title);
    }

    private void initSubTitleText() {

        TextView subTitleText = (TextView) v.findViewById(R.id.sub_title_text);

        String subTitle = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

        subTitleText.setText(subTitle);
    }

    private void initRemoveButton() {

        final Button removeButton = (Button) v.findViewById(R.id.remove_button);
        removeButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        removeButton.setAlpha(0.3f);

                        return true;

                    case MotionEvent.ACTION_UP:

                        removeButton.setAlpha(1f);
                        fadeOut();

                        return true;

                    case MotionEvent.ACTION_CANCEL:

                        removeButton.setAlpha(1f);

                        return true;

                }

                return false;
            }
        });
    }


    private void initForTouch() {



    }




    public File getFile() {
        return mFile;
    }




    public interface PostCompressListener {
        void postCompress(SoundFileListCell cell);
    }

}
