package net.c_kogyo.singlesongplayer;

import android.animation.ValueAnimator;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDrawer();

    }

    private int drawerStride;
    private LinearLayout drawer;
    private void initDrawer() {

        drawer = (LinearLayout) findViewById(R.id.drawer);

        initDirTab();

        // 画面幅を検出し初期状態はタブのみが出ているようにする
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        drawerStride = point.x - tabWidth;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.rightMargin = drawerStride;
        drawer.setLayoutParams(params);
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

                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        params.rightMargin = (int) valueAnimator.getAnimatedValue();
                        drawer.setLayoutParams(params);

                        drawer.requestLayout();
                    }
                });
                animator.setDuration(500);
                animator.start();

                isDrawerOpen = !isDrawerOpen;

            }
        });


    }
}
