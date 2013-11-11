package org.rftg.scorer;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gc
 */
public class FastCameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public final static int AUTO_CAMERA_HEIGHT_BOUND = 800;

    private volatile Camera camera;
    private List<Size> cameraSizes;
    private Size preferredSize;
    private Size actualSize;
    private View interfaceView;

    private byte[] buffer;

    private final Object bufferLock = new Object();
    private boolean bufferReady;

    public FastCameraView(Context context) {
        super(context);
    }

    public FastCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FastCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        getHolder().addCallback(this);
    }

    public List<Size> getCameraSizes() {
        return cameraSizes;
    }

    public Size getActualSize() {
        synchronized (bufferLock) {
            return actualSize;
        }
    }

    public void setPreferredSize(Size preferredSize) {
        this.preferredSize = preferredSize;
    }

    public void setInterfaceView(View interfaceView) {
        this.interfaceView = interfaceView;
    }

    public ByteBuffer getBuffer(ByteBuffer byteBuffer, Size size) {
        if (camera == null) {
            return null;
        }
        synchronized (bufferLock) {
            while (!bufferReady) {
                try {
                    bufferLock.wait(1000);
                } catch (InterruptedException e) {
                    Rftg.w(e.getMessage());
                    return null;
                }
            }
            if (!this.actualSize.equals(size)) {
                return null;
            }
            byteBuffer.position(0);
            byteBuffer.put(buffer, 0, byteBuffer.capacity());
            camera.addCallbackBuffer(buffer);
            return byteBuffer;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Rftg.d("Surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Rftg.d("Surface changed");
        releaseCamera();

        try {
            camera = Camera.open();
            Rftg.d("Camera opened");

            Size selectedCameraSize = null;
            boolean foundPreferred = false;
            cameraSizes = new ArrayList<Size>();
            for (Camera.Size size : camera.getParameters().getSupportedPreviewSizes()) {
                Size s = new Size(size.width, size.height);
                if (s.width % 16 != 0) {
                    Rftg.d("Skip resolution with width not divisible by 16: " + s);
                }
                cameraSizes.add(s);
                if (foundPreferred) {
                    continue;
                }
                // If no user defined preferred size we will try to find the one with height close to AUTO_CAMERA_HEIGHT_BOUND
                if (s.equals(preferredSize)) {
                    selectedCameraSize = s;
                    Rftg.d("Selecting preferred size: " + s);
                    foundPreferred = true;
                } else if (selectedCameraSize == null ||
                        (selectedCameraSize.width < s.width && s.height <= AUTO_CAMERA_HEIGHT_BOUND) ||
                        (s.height <= AUTO_CAMERA_HEIGHT_BOUND && selectedCameraSize.height > AUTO_CAMERA_HEIGHT_BOUND) ||
                        ((s.height > AUTO_CAMERA_HEIGHT_BOUND && selectedCameraSize.height > AUTO_CAMERA_HEIGHT_BOUND) &&
                                (s.height < selectedCameraSize.height || (s.height == selectedCameraSize.height && s.width > selectedCameraSize.width)))) {
                    selectedCameraSize = s;
                }
            }

            if (selectedCameraSize == null) {
                throw new RuntimeException("Can't find proper camera size");
            }

            Camera.Parameters params = camera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewSize(selectedCameraSize.width, selectedCameraSize.height);
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            camera.setParameters(params);
            params = camera.getParameters();

            if (interfaceView != null) {
                // Change holder size to maintain ratio if needed
                Size interfaceSize = new Size(interfaceView.getWidth(), interfaceView.getHeight());
                Size preferredHolderSize = interfaceSize.scaleIn(this.actualSize);
                this.getHolder().setFixedSize(preferredHolderSize.width, preferredHolderSize.height);
            }

            camera.setPreviewDisplay(getHolder());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            int pictureSize = this.actualSize.width * this.actualSize.height;

            synchronized (bufferLock) {
                buffer = new byte[pictureSize * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8];

                this.actualSize = new Size(params.getPreviewSize().width, params.getPreviewSize().height);
                Rftg.d("Set preview size to " + this.actualSize);

                bufferReady = false;
            }

            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);

            camera.startPreview();
            Rftg.d("Preview started");
        } catch (IOException e) {
            Rftg.e(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Rftg.d("Surface destroyed");
        releaseCamera();
    }

    public void releaseCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                Rftg.d("Preview stopped");
            } finally {
                camera.release();
            }
            camera = null;
            Rftg.d("Camera released");
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        synchronized (bufferLock) {
            bufferReady = true;
            bufferLock.notifyAll();
        }
    }

}
