package org.rftg.scorer;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;

public class MainActivity extends Activity {

//    private volatile RecognizerResources recognizerResources;

//    private volatile Recognizer recognizer;

    private FastCameraView fastCamera;
    private UserInterfaceView userInterface;

    private State state;
  /*
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:

                    if (recognizerResources == null) {
                        CardInfo cardInfo = new CardInfo(getAssets());
                        state = State.loadState(MainActivity.this, cardInfo);
                        if (state == null) {
                            state = new State();
                        }
                        recognizerResources = new RecognizerResources(MainActivity.this, cardInfo, state.settings, new ScreenProperties(openCvCameraView.getWidth(), openCvCameraView.getHeight()));
                    }

                    openCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };
    */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        fastCamera = (FastCameraView) this.findViewById(R.id.fastCamera);
        userInterface = (UserInterfaceView) this.findViewById(R.id.userInterface);
        fastCamera.setInterfaceView(userInterface);

        CardInfo cardInfo = new CardInfo(getAssets());
        state = State.loadState(MainActivity.this, cardInfo);
        if (state == null) {
            state = new State();
        }

/*
        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (recognizerResources != null && state != null && recognizer != null && recognizerResources.isLoaded()) {
                    return recognizerResources.userControls.onTouch(view, motionEvent, recognizer, state);
                } else {
                    return false;
                }
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.prestige:
                state.settings.usePrestige = !state.settings.usePrestige;
                if (!state.settings.usePrestige) {
                    state.player.prestige = 0;
                }
                item.setTitle(getResources().getString(
                        state.settings.usePrestige ? R.string.prestige_disable : R.string.prestige_enable));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.prestige).setTitle(getResources().getString(
                state.settings.usePrestige ? R.string.prestige_disable : R.string.prestige_enable));
        return true;
    }

    @Override
    public void onPause() {
        try {
            if (state != null) {
                state.saveState(this);
            }
            fastCamera.releaseCamera();
            Rftg.d("Pause");
        } finally {
            super.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Rftg.d("Resume");
    }
}
