package org.rftg.scorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author gc
 */
public class UserInterfaceView extends View {
/*
    final static Paint PAINT_LOADING_PERCENT = new Paint();
    {
        PAINT_LOADING_PERCENT.setARGB(255, 255, 255, 255);
        PAINT_LOADING_PERCENT.setTextSize(50);
        PAINT_LOADING_PERCENT.setTextAlign(Paint.Align.CENTER);
    }
  */
    private MainContext mainContext;
    private UserInterfaceResources userInterfaceResources;

    public UserInterfaceView(Context context) {
        super(context);
    }

    public UserInterfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserInterfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMainContext(MainContext mainContext) {
        this.mainContext = mainContext;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mainContext == null) {
            return;
        }

        if (userInterfaceResources == null
                || userInterfaceResources.screenSize.width != canvas.getWidth()
                || userInterfaceResources.screenSize.height != canvas.getHeight()) {
            if (userInterfaceResources != null) {
                userInterfaceResources.dispose();
            }
            userInterfaceResources = new UserInterfaceResources(mainContext, new Size(canvas.getWidth(), canvas.getHeight()));
        }

        State state = mainContext.state;
        Player player = state.player;
        userInterfaceResources.getCardsIcon().draw(canvas, "" + player.cards.size());
        userInterfaceResources.getChipsIcon().draw(canvas, "" + player.chips);
        if (state.settings.usePrestige) {
            userInterfaceResources.getPrestigeIcon().draw(canvas, "" + player.prestige);
        }
        userInterfaceResources.getMilitaryIcon().draw(canvas, "" + player.scoring.military);
        userInterfaceResources.getResetIcon().draw(canvas, null);
        userInterfaceResources.getTotalIcon().draw(canvas, "" + player.scoring.score);


    }
}
