package com.gpetuhov.android.sneakeyes;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

public class ButtonActivity extends AppCompatActivity {

    private Button mButton;

    @Inject PhotoTaker mPhotoTaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button);

        SneakEyesApp.getAppComponent().inject(this);

        mButton = (Button) findViewById(R.id.button);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoTaker.takePhoto();
            }
        });
    }
}
