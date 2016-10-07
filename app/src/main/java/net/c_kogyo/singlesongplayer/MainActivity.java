package net.c_kogyo.singlesongplayer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.util.ArrayList;

import view.CollapseFileTreeView;
import view.SoundFileListCell;

public class MainActivity extends AppCompatActivity {

    private ArrayList<File> queueList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        queueList = new ArrayList<>();

        initAdView();

        initDrawer();
        initFileView();

        initQueueListView();

    }


    @Override
    protected void onResume() {
        super.onResume();
        loadQueueList();
        setQueueListView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveQueueList();
    }

    private final String SSP_PREF_TAG = "ssp_pref_tag";
    private final String QUEUE_LIST_TAG = "queue_list_tag";
    private void saveQueueList() {

        StringBuilder builder = new StringBuilder();

        for (File file : queueList) {

            builder.append(file.getAbsolutePath()).append(",");
        }

        String data = builder.toString();

        SharedPreferences prefs = getSharedPreferences(SSP_PREF_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(QUEUE_LIST_TAG, data);
        editor.apply();
    }

    private void loadQueueList() {

        SharedPreferences prefs = getSharedPreferences(SSP_PREF_TAG, MODE_PRIVATE);

        String data = prefs.getString(QUEUE_LIST_TAG, null);

        queueList = new ArrayList<>();
        if (data == null || data.equals("")) return;

        String[] ss = data.split(",");

        for (String s : ss) {
            queueList.add(new File(s));
        }

    }

    private void initAdView() {

        MobileAds.initialize(getApplicationContext(), getString(R.string.banner_ad_unit_id));

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

    }

    private int drawerStride;
    private LinearLayout drawer;
    private ViewGroup.MarginLayoutParams drawerParams;
    private void initDrawer() {

        drawer = (LinearLayout) findViewById(R.id.drawer);

        initDirTab();

        // 画面幅を検出し初期状態はタブのみが出ているようにする
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        drawerStride = tabWidth - point.x;

        drawerParams = new FrameLayout.LayoutParams(point.x, ViewGroup.LayoutParams.MATCH_PARENT);
        drawerParams.leftMargin = drawerStride;
        drawer.setLayoutParams(drawerParams);
        drawer.requestLayout();

    }

    private ImageView dirTab;
    private int tabWidth;
    private boolean isDrawerOpen = false;
    private void initDirTab() {

        dirTab = (ImageView) findViewById(R.id.dir_tab);
        tabWidth = getResources().getDimensionPixelSize(R.dimen.dir_tab_width);

        dirTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                animateDrawer();
            }
        });
    }

    private void animateDrawer() {

        int origin, target;
        if (isDrawerOpen) {
            origin = 0;
            target = drawerStride;
        } else {
            origin = drawerStride;
            target = 0;
        }

        ValueAnimator animator = ValueAnimator.ofInt(origin, target);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                drawerParams.leftMargin =  (int) valueAnimator.getAnimatedValue();
                drawer.setLayoutParams(drawerParams);

                drawer.requestLayout();
            }
        });
        animator.setDuration(300);
        animator.start();

        isDrawerOpen = !isDrawerOpen;

    }

    private final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;
    private CollapseFileTreeView fileView;
    private void initFileView(){

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            updateFileView();
        }

    }

    private void updateFileView() {

        fileView = (CollapseFileTreeView) findViewById(R.id.file_view);
        File exDir = Environment.getExternalStorageDirectory();
        fileView.setFileAndListeners(exDir, new CollapseFileTreeView.OnAnimationUpdateListener() {
            @Override
            public void onUpdateContainer() {

//                findViewById(R.id.scroll_view).requestLayout();
            }

        }, new CollapseFileTreeView.OnFileClickListener() {
            @Override
            public void onClick(File file) {
                // TODO ファイルクリック時の処理
                animateDrawer();
                addSoundFile(file);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
//                    updateFileView();


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private LinearLayout queueListView;
    private FrameLayout screenFrame;
    private ScrollView scrollView;
    private void initQueueListView() {

        queueListView = (LinearLayout) findViewById(R.id.queue_list);
        screenFrame = (FrameLayout) findViewById(R.id.screen_frame);
        screenFrame.setVisibility(View.INVISIBLE);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);

    }

    private void addSoundFile(File file) {

        queueList.add(file);

        SoundFileListCell cell = new SoundFileListCell(this, file, true, new SoundFileListCell.PostCompressListener() {
            @Override
            public void postCompress(SoundFileListCell cell) {
                postCompressCell(cell);
            }
        });
        queueListView.addView(addDragListener(cell));

    }

    private void setQueueListView() {

        queueListView.removeAllViews();

        for (File file : queueList) {
            SoundFileListCell cell = new SoundFileListCell(this, file, false, new SoundFileListCell.PostCompressListener() {
                @Override
                public void postCompress(SoundFileListCell cell) {
                    postCompressCell(cell);
                }
            });
            queueListView.addView(addDragListener(cell));        }
    }

    private void postCompressCell(SoundFileListCell cell) {

        for ( int i = 0 ; i < queueListView.getChildCount() ; i++ ) {

            if (cell.equals(queueListView.getChildAt(i))) {

                queueListView.removeViewAt(i);
                queueList.remove(i);

            }
        }
    }

    private SoundFileListCell cellDragged;
    private SoundFileListCell addDragListener(SoundFileListCell cell) {

        final int cellHeight = getResources().getDimensionPixelSize(R.dimen.sound_cell_height);

        cell.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                // ドラッグスタート時にcellを仕込む
                cellDragged = (SoundFileListCell) view;
                screenFrame.setVisibility(View.VISIBLE);
                reflectOnScreen(getIndexOfCell(cellDragged));

                view.startDrag(null, new View.DragShadowBuilder(view), null, 0);

                Log.i(SoundFileListCell.class.getSimpleName(), "Start drag! index: " + getIndexOfCell(cellDragged));

                return true;
            }
        });

        cell.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {

                if (view.equals(cellDragged)) {
                    return true;
                }

                SoundFileListCell currentCell = (SoundFileListCell) view;
                int currentIndex = getIndexOfCell(currentCell);
                int fromIndex = getIndexOfCell(cellDragged);

                //ドラッグが入ってきたとき上下どちらに退避するか

                switch (dragEvent.getAction()) {

                    case DragEvent.ACTION_DRAG_ENTERED:

                        if (fromIndex == currentIndex) return true;

                        // ドラッグが入ってきました。

//                        ImageView cellShadow = new ImageView(MainActivity.this);
//                        cellShadow.setImageBitmap(getBitmapFromView(toCell));
//
//                        ObjectAnimator animator0 = ObjectAnimator.ofFloat(cellShadow, "translationY", 0, target);
//                        animator0.setDuration(300);
//                        animator0.start();
//
//                        toCell.setAlpha(0.2f);

                        Log.i(SoundFileListCell.class.getSimpleName(), "Drag in! to " + currentIndex);

                        if (fromIndex > currentIndex) {
                            //上に向かってドラッグしたということ
                            // 下に向かって退避する

                            for (int i = fromIndex - 1  ; i >= currentIndex; i-- ) {

                                CellShadow cellShadow = getCellShadowByIndex(i);

                                if (cellShadow != null) {
                                    cellShadow.move = CellShadowMove.DOWN_MOVE;
                                    ObjectAnimator animator0 = ObjectAnimator.ofFloat(cellShadow, "translationY", 0, cellHeight);
                                    animator0.setDuration(300);
                                    animator0.start();
                                }

                            }

                        } else {
                            // 下に向かってドラッグしたということ
                            // 上に向かって退避する

                            for (int i = fromIndex + 1  ; i <= currentIndex; i++ ) {

                                CellShadow cellShadow = getCellShadowByIndex(i);

                                if (cellShadow != null) {
                                    cellShadow.move = CellShadowMove.UP_MOVE;
                                    ObjectAnimator animator0 = ObjectAnimator.ofFloat(cellShadow, "translationY", 0, -cellHeight);
                                    animator0.setDuration(300);
                                    animator0.start();
                                }

                            }
                        }



                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:

                        Log.i(SoundFileListCell.class.getSimpleName(), "Drag exited! of " + currentIndex);
                        // 元位置に復帰するアニメーション

                        CellShadow shadow = getCellShadowByIndex(currentIndex);

                        if (shadow != null) {
                            int origin = cellHeight;
                            if (shadow.move == CellShadowMove.UP_MOVE) {
                                origin = -cellHeight;
                            }

                            ObjectAnimator animator0 = ObjectAnimator.ofFloat(shadow, "translationY", origin, 0);
                            animator0.setDuration(300);
                            animator0.start();
                        }


                        return false;

                    case DragEvent.ACTION_DROP:

                        screenFrame.removeAllViews();
                        screenFrame.setVisibility(View.INVISIBLE);

                        if (fromIndex == currentIndex) return true;

//                        // viewがよけてしまうのでこれは呼ばれない
//
//                        cellDragged.setAlpha(1f);
//
//
//
//                        queueListView.removeView(cellDragged);
//                        queueListView.addView(cellDragged, toIndex);
//                        queueListView.requestLayout();
//
//                        File file = queueList.get(fromIndex);
//                        queueList.remove(fromIndex);
//                        queueList.add(toIndex, file);

                        Log.i(SoundFileListCell.class.getSimpleName(), "Drag dropped! into " + currentIndex);

                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:

                        screenFrame.removeAllViews();
                        screenFrame.setVisibility(View.INVISIBLE);

//                        ObjectAnimator animator1 = ObjectAnimator.ofFloat(toCell, "translationY", target, 0);
//                        animator1.setDuration(300);
//                        animator1.start();




                        Log.i(SoundFileListCell.class.getSimpleName(), "Drag ended!");

                        return true;
                }
                return true;
            }
        });

        return cell;
    }

    private int getIndexOfCell(SoundFileListCell cell) {

        for ( int i = 0 ; i < queueListView.getChildCount() ; i++ ) {

            if (queueListView.getChildAt(i).equals(cell)) {
                return i;
            }
        }
        return -1;
    }

    private Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight() ,Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable =view.getBackground();
//        if (bgDrawable!=null)
        //has background drawable, then draw it on the canvas
//            bgDrawable.draw(canvas);
//        else
        //does not have background drawable, then draw white background on the canvas
//            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        view.draw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(0);
        canvas.drawLine(0, 0, canvas.getWidth(), 0, paint);
        canvas.drawLine(0, canvas.getHeight(), canvas.getWidth(), canvas.getHeight(), paint );
        //return the bitmap
        return returnedBitmap;
    }

    private ArrayList<CellShadow> cellShadows;
    private void reflectOnScreen(int draggedIndex) {

        cellShadows = new ArrayList<>();

        int[] scrollTopLeft = {0, 0};
        scrollView.getLocationOnScreen(scrollTopLeft);

        for (int i = 0 ; i < queueListView.getChildCount() ; i++ ) {

            // 自分の画像は射影しない
            if (i != draggedIndex) {

                View view = queueListView.getChildAt(i);

                int[] cellTopLeft = {0, 0};
                view.getLocationOnScreen(cellTopLeft);

                CellShadow shadow = new CellShadow(i, view);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(shadow.width, shadow.height);
                params.leftMargin = 0;
                params.topMargin = cellTopLeft[1] - scrollTopLeft[1];
                // FrameLayoutのmarginはgravityがセットされないと反応しない
                params.gravity = Gravity.TOP;

                shadow.setLayoutParams(params);

                screenFrame.addView(shadow);
                cellShadows.add(shadow);

                Log.i(SoundFileListCell.class.getSimpleName(), "index " + i + " added! x = " + params.leftMargin + ", y = " + params.topMargin );

            }

        }

    }

    private CellShadow getCellShadowByIndex(int index) {
        for (CellShadow cellShadow : cellShadows) {
            if (cellShadow.index == index) {
                return cellShadow;
            }
        }
        return null;
    }

    enum CellShadowMove{

        UP_MOVE,
        STAY,
        DOWN_MOVE,
    }

    class CellShadow  extends ImageView{

        CellShadowMove move;
        int index, width, height;

        public CellShadow(int index, View view) {
            super(MainActivity.this);
            this.index = index;
            this.move = CellShadowMove.STAY;

            Bitmap bitmap = getBitmapFromView(view);
            width = bitmap.getWidth();
            height = bitmap.getHeight();

            setImageBitmap(bitmap);
        }

        public CellShadow(Context context, AttributeSet attrs, int index) {
            super(context, attrs);
            this.index = index;
        }


    }
}
