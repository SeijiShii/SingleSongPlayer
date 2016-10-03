package net.c_kogyo.singlesongplayer;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;

import view.CollapseFileTreeView;
import view.SoundFileListCell;

public class MainActivity extends AppCompatActivity {
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initAdView();

        initDrawer();
        initFileView();

        initQueueList();

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

    private LinearLayout queueList;
    private void initQueueList() {

        queueList = (LinearLayout) findViewById(R.id.queue_list);
    }

    private void addSoundFile(File file) {

        SoundFileListCell cell = new SoundFileListCell(this, file);
        queueList.addView(cell);

    }
}
