package cn.ucai.fulicenter.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import cn.ucai.fulicenter.R;
import cn.ucai.fulicenter.ui.view.MFGT;

/**
 * Created by clawpo on 2017/3/14.
 */

public class SplashActivity extends AppCompatActivity {
    int time = 2000;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MFGT.gotoMain(SplashActivity.this);
                SplashActivity.this.finish();
            }
        },time);
    }
}
