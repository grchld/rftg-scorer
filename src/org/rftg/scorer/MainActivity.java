package org.rftg.scorer;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements CvCameraViewListener {

    ExecutorService executorService;
    SamplesLoader samplesLoader;
    Recognizer recognizer;
    CustomNativeTools customNativeTools;

    private CameraBridgeViewBase openCvCameraView;

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:

                    customNativeTools = new CustomNativeTools();
                    samplesLoader = new SamplesLoader(MainActivity.this, Card.GameType.EXP1.maxCardNum);


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

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main);
        openCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause() {
        if (openCvCameraView != null)
            openCvCameraView.disableView();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, loaderCallback);
    }

    public void onDestroy() {
        if (openCvCameraView != null)
            openCvCameraView.disableView();
        if (samplesLoader != null) {
            samplesLoader.release();
            samplesLoader = null;
        }
        executorService.shutdown();
        super.onDestroy();
    }

    @Override
    public synchronized void onCameraViewStarted(int width, int height) {
        recognizer = new Recognizer(this, width, height);
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
