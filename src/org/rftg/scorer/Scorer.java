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

public class Scorer extends Activity implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
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
        super.onDestroy();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onCameraViewStopped() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return inputFrame;
    }
}
