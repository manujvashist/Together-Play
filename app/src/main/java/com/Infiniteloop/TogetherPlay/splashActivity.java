package com.Infiniteloop.TogetherPlay;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.Infiniteloop.TogetherPlay.R;

public class splashActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        final Animation animation_1 = AnimationUtils.loadAnimation(getBaseContext(),R.anim.rotate);
        final Animation animation_2 = AnimationUtils.loadAnimation(getBaseContext(),R.anim.antirotate);
        final Animation animation_3 = AnimationUtils.loadAnimation(getBaseContext(),R.anim.abc_fade_out);

        imageView.startAnimation(animation_2);
        animation_2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.startAnimation(animation_1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animation_1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.startAnimation(animation_3);
                finish();
                Intent i = new Intent(getBaseContext(),MainActivity.class);
                startActivity(i);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
