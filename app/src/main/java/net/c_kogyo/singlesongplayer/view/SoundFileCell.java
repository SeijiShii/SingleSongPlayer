package net.c_kogyo.singlesongplayer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.c_kogyo.singlesongplayer.R;

import java.io.File;

/**
 * Created by SeijiShii on 2016/10/09.
 */

public class SoundFileCell extends LinearLayout {

    private File nFile;
    private MediaMetadataRetriever mRetriever;
    private OnFileClickListener mListener;
    private int fileHeight;

    public SoundFileCell(Context context, File file, OnFileClickListener listener) {
        super(context);

        nFile = file;
        mListener = listener;

        fileHeight = context.getResources().getDimensionPixelSize(R.dimen.file_height);

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

    public interface OnFileClickListener {
        void onClick(File file);
    }

}
