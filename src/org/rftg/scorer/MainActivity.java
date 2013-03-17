package org.rftg.scorer;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class MainActivity extends Activity implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;

    private volatile RecognizerResources recognizerResources;

    private volatile Recognizer recognizer;

    private State state;

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
                        recognizerResources = new RecognizerResources(MainActivity.this, cardInfo, state.settings);
                    }

                    openCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (recognizerResources != null && state != null && recognizer != null) {
                    return recognizerResources.userControls.onTouch(view, motionEvent, recognizer, state);
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public void onPause() {
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        if (state != null) {
            state.saveState(this);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, loaderCallback);
    }

    public void onDestroy() {
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        if (recognizerResources != null) {
            recognizerResources.release();
            recognizerResources = null;
        }
        super.onDestroy();
    }

    @Override
    public synchronized void onCameraViewStarted(int width, int height) {
        recognizer = new Recognizer(recognizerResources, state, width, height);
    }

    @Override
    public synchronized void onCameraViewStopped() {
        if (recognizer != null) {
            recognizer.release();
            recognizer = null;
        }
    }

    @Override
    public synchronized Mat onCameraFrame(Mat inputFrame) {
        if (recognizer != null) {
            return recognizer.onFrame(inputFrame);
        } else {
            return inputFrame;
        }
    }
}
