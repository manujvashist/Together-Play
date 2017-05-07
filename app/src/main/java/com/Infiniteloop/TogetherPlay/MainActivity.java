package com.Infiniteloop.TogetherPlay;

import com.Infiniteloop.TogetherPlay.dj.DJActivity;
import com.Infiniteloop.TogetherPlay.speaker.SpeakerActivity;
import hw.infloop.tp1.Main2Activity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity
{
    Button button;
    // Public key for other activities to access to figure out the mode
    public final static String MODE = "MODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        button = (Button) findViewById(R.id.btn_Mic);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent micIntent = new Intent (MainActivity.this,Main2Activity.class);
                startActivity(micIntent);


            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onBtnDJ(View view) {
        Intent intent = new Intent(this, DJActivity.class);
        intent.putExtra(MODE, DJActivity.DJ_MODE);
        startActivity(intent);
    }

    public void onBtnSp(View view) {
        Intent intent = new Intent(this, SpeakerActivity.class);
        intent.putExtra(MODE, SpeakerActivity.SPEAKER_MODE);
        startActivity(intent);
    }

}

