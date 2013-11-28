package org.rftg.scorer;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private MainContext mainContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        FastCameraView fastCamera = (FastCameraView) this.findViewById(R.id.fastCamera);
        UserInterfaceView userInterface = (UserInterfaceView) this.findViewById(R.id.userInterface);

        CardInfo cardInfo = new CardInfo(getAssets());
        State state = State.loadState(MainActivity.this, cardInfo);
        if (state == null) {
            state = new State();
        }

        mainContext = new MainContext(this, fastCamera, userInterface, cardInfo, state);

        fastCamera.setInterfaceView(userInterface);
        fastCamera.setPreferredSize(mainContext.state.settings.preferredCameraSize);

        userInterface.setMainContext(mainContext);
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
                mainContext.state.settings.usePrestige = !mainContext.state.settings.usePrestige;
                if (!mainContext.state.settings.usePrestige) {
                    mainContext.state.player.prestige = 0;
                }
                item.setTitle(getResources().getString(
                        mainContext.state.settings.usePrestige ? R.string.prestige_disable : R.string.prestige_enable));
                mainContext.userInterface.postInvalidate();
                return true;
            case R.id.cameraResolution:
                List<Size> sizes = new ArrayList<Size>(mainContext.fastCamera.getCameraSizes());
                Collections.sort(sizes);

                Size preferredCameraSize = mainContext.state.settings.preferredCameraSize;

                Menu menu = item.getSubMenu();
                menu.clear();
                menu.add("Auto")./*setCheckable(true).setChecked(preferredCameraSize == null).*/setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        changeCameraPreferredSize(null);
                        return true;
                    }
                });

                for (final Size size : sizes) {
                    menu.add(size.toString())./*setCheckable(true).setChecked(size.equals(preferredCameraSize)).*/setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            changeCameraPreferredSize(size);
                            return true;
                        }
                    });
                }
                return true;
            default:

                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.prestige).setTitle(getResources().getString(
                mainContext.state.settings.usePrestige ? R.string.prestige_disable : R.string.prestige_enable));
        return true;
    }

    @Override
    public void onPause() {
        try {
            if (mainContext.state != null) {
                mainContext.state.saveState(this);
            }
            mainContext.executor.stop();
            mainContext.fastCamera.releaseCamera();
            Rftg.d("Pause");
        } finally {
            super.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mainContext.executor.start();
        mainContext.recognizer.startFrameRecognition();
        Rftg.d("Resume");
    }

    private void changeCameraPreferredSize(Size size) {
        mainContext.state.settings.preferredCameraSize = size;
        mainContext.fastCamera.setPreferredSize(size);

        Size interfaceSize = mainContext.userInterface.getViewSize();
        Size fixedSize = size == null ? interfaceSize : interfaceSize.scaleIn(size);

        mainContext.fastCamera.getHolder().setFixedSize(fixedSize.width, fixedSize.height);
    }
}
