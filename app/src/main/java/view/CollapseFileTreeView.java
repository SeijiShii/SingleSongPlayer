package view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.c_kogyo.singlesongplayer.R;

import java.io.File;

/**
 * Created by SeijiShii on 2016/09/30.
 */

public class CollapseFileTreeView extends LinearLayout {

    private File mFile;
    private View v;

    public CollapseFileTreeView(Context context, File file) {
        super(context);

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

        v = inflate(getContext(), R.layout.collapse_file_tree_view, this);

        mainCell = (LinearLayout) v.findViewById(R.id.main_cell);
        iconImage = (ImageView) v.findViewById(R.id.icon_image);
        textView = (TextView) v.findViewById(R.id.text);
        childContainer = (LinearLayout) v.findViewById(R.id.child_container);

    }

    private void updateIconImageView() {

        if (mFile == null) return;

        if (mFile.isDirectory()) {
            iconImage.setBackgroundResource(R.drawable.dir_icon);
        } else {

        }
    }

    private void updateTextView() {

        if (mFile == null) return;

        String titleText = mFile.getName();

        textView.setText(titleText);
    }

    private void updateContainer() {

        if (mFile == null || !mFile.isDirectory()) return;

        for (File file : mFile.listFiles()) {

            if (file.isDirectory()) {

                CollapseFileTreeView treeView = new CollapseFileTreeView(getContext(), file);
                treeView.setOnContainerHeightUpdateListener(mListener);
                childContainer.addView(treeView);

            } else {

                // 拡張子を取得
                String[] ss = file.getName().split(".");

                if (ss.length > 2) {
                    String extension = ss[ss.length - 1];


                    if (extension.equals("mp3")) {

                    }
                }
            }
        }

        childContainer.getLayoutParams().height = 0;

        mainCell.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                animateChildContainer();
            }
        });

    }

    private OnContainerHeightUpdateListener mListener;
    public void setFile(File file, OnContainerHeightUpdateListener listener) {
        this.mFile = file;

        mListener = listener;
        updateViews();
    }


    private void updateViews() {
        updateIconImageView();
        updateTextView();
        updateContainer();
    }

    private boolean isChildOpen = false;
    private void animateChildContainer() {

        int origin, target;

        int openHeight = (int) getContext().getResources().getDimension(R.dimen.file_height) * childContainer.getChildCount();

        if (isChildOpen) {
            origin = openHeight;
            target = 0;
        } else {
            origin = 0;
            target = openHeight;
        }

        ValueAnimator animator = ValueAnimator.ofInt(origin, target);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                childContainer.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
                childContainer.requestLayout();
                CollapseFileTreeView.this.requestLayout();
                mListener.onContainerHeightUpdate();

            }
        });
        animator.setDuration(300);
        animator.start();

        isChildOpen = !isChildOpen;
    }

    public void setOnContainerHeightUpdateListener(OnContainerHeightUpdateListener listener) {
        mListener = listener;
    }

    public interface OnContainerHeightUpdateListener {

        void onContainerHeightUpdate();
    }


}
