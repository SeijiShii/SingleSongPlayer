package view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.c_kogyo.singlesongplayer.R;

import java.io.File;

/**
 * Created by SeijiShii on 2016/09/30.
 */

public class CollapseFileTreeView extends LinearLayout {

    private OnAnimationUpdateListener mUpdateListener;
    private OnFileClickListener mOnFileClickListener;

    private File mFile;
    private View v;
    private int fileHeight;

    public CollapseFileTreeView(Context context,
                                File file,
                                OnAnimationUpdateListener listener,
                                OnFileClickListener clickListener) {
        super(context);

        mUpdateListener = listener;
        mOnFileClickListener = clickListener;

        mFile = file;
        initCommon();
        updateViews();
    }

    public CollapseFileTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public CollapseFileTreeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    private LinearLayout mainCell;
    private ImageView iconImage;
    private TextView textView;
    private LinearLayout childContainer;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initCommon();
    }

    private void initCommon() {

        fileHeight = getContext().getResources().getDimensionPixelSize(R.dimen.file_height);

        v = inflate(getContext(), R.layout.collapse_file_tree_view, this);

        mainCell = (LinearLayout) v.findViewById(R.id.main_cell);

        iconImage = (ImageView) v.findViewById(R.id.icon_image);
        textView = (TextView) v.findViewById(R.id.text);
        childContainer = (LinearLayout) v.findViewById(R.id.child_container);

    }

    private void updateViews() {
        updateIconImageView();
        updateTextView();

        mainCell.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        mainCell.setAlpha(0.5f);

                        return true;
                    case MotionEvent.ACTION_UP:

                        mainCell.setAlpha(1f);

                        // ディレクトリとファイルでは挙動が違う
                        if (mFile.isDirectory()) {
                            animateChildContainer();
                        } else if (isMP3Or4(mFile)) {

                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        mainCell.setAlpha(1f);
                        return true;
                }

                return false;
            }
        });


    }

    private boolean isMP3Or4(File file) {

        // .mp3というディレクトリもある
        if (file.isDirectory()) return false;

        // 拡張子を取得
        String[] ss = file.getName().split("\\.");

        if (ss.length >= 2) {
            String extension = ss[ss.length - 1];
            if (extension.equals("mp3") || extension.equals("mp4")) {
                return true;
            }
        }
        return false;
    }

    private void updateIconImageView() {

        if (mFile == null) return;
        iconImage.setBackgroundResource(R.drawable.dir_icon);

    }

    private void updateTextView() {

        if (mFile == null) return;

        String titleText = mFile.getName();
        textView.setText(titleText);
    }

    public void addChildren() {

        if (mFile == null || !mFile.isDirectory()) return;

        for (File file : mFile.listFiles()) {

            if (file.isDirectory()) {

                CollapseFileTreeView treeView
                        = new CollapseFileTreeView(getContext(),
                        file,
                        new OnAnimationUpdateListener() {

                            @Override
                            public void onUpdateContainer() {

                                CollapseFileTreeView.this.updateContainerHeight();
                                mUpdateListener.onUpdateContainer();

                                Log.i(CollapseFileTreeView.class.getSimpleName(), mFile.getName() + ": containerHeight: " + childContainer.getHeight());
                            }

                        }, mOnFileClickListener);

                childContainer.addView(treeView);

            } else if (isMP3Or4(file)) {

                SoundFileCell cell = new SoundFileCell(getContext(), file, mOnFileClickListener);
                childContainer.addView(cell);

            }
        }

        childContainer.getLayoutParams().height = 0;

    }

    private void updateContainerHeight() {

        int height = 0 ;

        for (int i = 0 ; i < childContainer.getChildCount() ; i++ ) {

            height += ((CollapseFileTreeView)(childContainer.getChildAt(i))).getLayoutParams().height;

        }

        childContainer.getLayoutParams().height = height;

    }

    public void setFileAndListeners(File file,
                                    OnAnimationUpdateListener listener,
                                    OnFileClickListener clickListener) {

        this.mUpdateListener = listener;
        this.mOnFileClickListener = clickListener;
        this.mFile = file;

        updateViews();
        addChildren();
    }

    private boolean isChildOpen = false;
    private void animateChildContainer() {

        int origin, target;

        if (isChildOpen) {
            origin = getOpenContainerHeight();
            target = 0;
        } else {
            origin = 0;
            target = getOpenContainerHeight();
        }

        ValueAnimator animator = ValueAnimator.ofInt(origin, target);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                // この時点でchildContainerはWRAP_CONTENTではない。
                childContainer.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();

                childContainer.requestLayout();

                mUpdateListener.onUpdateContainer();

            }
        });
        animator.setDuration(300);
        animator.start();

        isChildOpen = !isChildOpen;

        // 孫階層のファイルとフォルダをセット
        addGrandChildren();
    }

    private void addGrandChildren() {

        for (int i = 0 ; i < this.childContainer.getChildCount() ; i++ ) {

            View view = childContainer.getChildAt(i);

            if (view instanceof CollapseFileTreeView) {
                CollapseFileTreeView treeView = (CollapseFileTreeView) view;

                if (treeView.childContainer.getChildCount() <= 0) {
                    treeView.addChildren();
                }
            }

        }
    }

    private int getOpenContainerHeight() {

        int sum = 0;
        for (int i = 0 ; i < childContainer.getChildCount() ; i++ ) {

            View view = childContainer.getChildAt(i);

            if (view instanceof CollapseFileTreeView) {

                sum += ((CollapseFileTreeView) view).getViewHeight();
            } else if (view instanceof SoundFileCell) {

                sum += fileHeight;
            }
        }
        return sum;
    }

    private int getViewHeight() {

        if (! isChildOpen) return fileHeight;

        return fileHeight + getOpenContainerHeight();

    }

    class SoundFileCell extends LinearLayout{

        private File nFile;
        private MediaMetadataRetriever mRetriever;
        private OnFileClickListener mListener;

        public SoundFileCell(Context context, File file, OnFileClickListener listener) {
            super(context);

            nFile = file;
            mListener = listener;

            init();
        }

        public SoundFileCell(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        private View v;
        private void init() {

            mRetriever = new MediaMetadataRetriever();
            mRetriever.setDataSource(nFile.getPath());

            v = inflate(getContext(), R.layout.sound_file_cell, this);
            initIconImage();
            initTitleText();

            this.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {

                    switch (motionEvent.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            SoundFileCell.this.setAlpha(0.5f);

                            return true;

                        case  MotionEvent.ACTION_UP:

                            SoundFileCell.this.setAlpha(1f);
                            mListener.onClick(nFile);

                            return true;

                        case MotionEvent.ACTION_CANCEL:

                            SoundFileCell.this.setAlpha(1f);

                            return true;

                    }


                    return false;
                }
            });

        }

        private void initIconImage() {

            ImageView iconImage = (ImageView) v.findViewById(R.id.icon_image);

            Bitmap bitmap = getBitmap();
            if (bitmap != null) {

                iconImage.setImageBitmap(bitmap);

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(fileHeight * 0.8), (int) (fileHeight * 0.8));
                params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

                iconImage.setLayoutParams(params);

            } else {

                iconImage.setBackgroundResource(R.drawable.ompu);
            }

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
                title = nFile.getName();
            }
            titleText.setText(title);

        }


    }

    public interface OnAnimationUpdateListener {

        void onUpdateContainer();

    }

    public interface OnFileClickListener {
        void onClick(File file);
    }

}
