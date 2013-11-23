package org.rftg.scorer;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;

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

        userInterface.setMainContext(mainContext);

        mainContext.state.player.cards.clear();
        for (int i = 40 ; i < 60 ; i++) {
            mainContext.state.player.cards.add(cardInfo.cards[i]);
        }
        mainContext.state.player.resetScoring();
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
            mainContext.fastCamera.releaseCamera();
            mainContext.executor.stop();
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
}
