package org.rftg.scorer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gc
 */
public class UserInterfaceView extends View {

    private MainContext mainContext;
    private UserInterfaceResources userInterfaceResources;

    private List<Widget> widgets = new ArrayList<Widget>();

    private Card magnifiedCard;

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

    {
        setOnTouchListener(new OnTouchListener() {

            private Rect previousDownRect;

            private Widget findWidget(MotionEvent motionEvent) {
                Point point = new Point((int)motionEvent.getX(), (int)motionEvent.getY());
                Widget result = null;
                for (Widget widget : widgets) {
                    if (widget.rect.contains(point)) {
                        result = widget;
                    }
                }
                return result;
            }

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Widget widget;
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        widget = findWidget(motionEvent);
                        if (widget != null) {
                            previousDownRect = widget.rect;
                            widget.onTouchDown();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (magnifiedCard != null) {
                            magnifiedCard = null;
                            previousDownRect = null;
                            postInvalidate();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        widget = findWidget(motionEvent);
                        boolean sameRect = widget != null && widget.rect.equals(previousDownRect);
                        if (magnifiedCard != null && (widget == null || !sameRect)) {
                            magnifiedCard = null;
                            previousDownRect = null;
                            postInvalidate();
                        }
                        if (widget != null && !sameRect) {
                            previousDownRect = widget.rect;
                            widget.onTouchDown();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mainContext == null) {
            return;
        }

        if (userInterfaceResources == null
                || userInterfaceResources.screenProperties.screenSize.width != canvas.getWidth()
                || userInterfaceResources.screenProperties.screenSize.height != canvas.getHeight()) {
            if (userInterfaceResources != null) {
                userInterfaceResources.dispose();
            }
            userInterfaceResources = new UserInterfaceResources(mainContext, new Size(canvas.getWidth(), canvas.getHeight()));
        }

        updateWidgets();

        for (Widget widget : widgets) {
            widget.draw(canvas);
        }
    }

    private void updateWidgets() {
        widgets.clear();
        final State state = mainContext.state;
        final Player player = state.player;
        widgets.add(new Widget(userInterfaceResources.getResetIcon(), null, null){
            @Override
            void onTouchDown() {
                state.player.cards.clear();
                state.player.chips = 0;
                state.player.prestige = 0;
                state.player.resetScoring();
                postInvalidate();
            }
        });
        widgets.add(new Widget(userInterfaceResources.getChipsIcon(), null, "" + player.chips){
            @Override
            void onTouchDown() {
                state.player.chips++;
                state.player.resetScoring();
                postInvalidate();
            }
        });
        if (state.settings.usePrestige) {
            widgets.add(new Widget(userInterfaceResources.getPrestigeIcon(), null, "" + player.prestige){
                @Override
                void onTouchDown() {
                    state.player.prestige++;
                    state.player.resetScoring();
                    postInvalidate();
                }
            });
        }
        widgets.add(new Widget(userInterfaceResources.getMilitaryIcon(), null, "" + player.scoring.military));
        widgets.add(new Widget(userInterfaceResources.getCardsIcon(), null, "" + player.cards.size()));
        widgets.add(new Widget(userInterfaceResources.getTotalIcon(), null, "" + player.scoring.score));

        List<Point> previewPositions = userInterfaceResources.screenProperties.getPreviewPositions(state.player.scoring.cardScores.size());
        int i = 0;
        for (final Scoring.CardScore cardScore : state.player.scoring.cardScores) {
            widgets.add(new Widget(userInterfaceResources.getCard(cardScore.card.id), previewPositions.get(i++), "" + cardScore.score){
                @Override
                void onTouchDown() {
                    magnifiedCard = cardScore.card;
                    postInvalidate();
                }
            });
        }

        if (magnifiedCard != null) {
            String score = "";
            for (Scoring.CardScore cardScore : state.player.scoring.cardScores) {
                if (cardScore.card.id == magnifiedCard.id) {
                    score += cardScore.score;
                    break;
                }
            }
            widgets.add(new Widget(userInterfaceResources.getMagnifiedCard(magnifiedCard.id), null, score));
        }
    }

    class Widget {
        final Sprite sprite;
        final Rect rect;
        final String text;

        Widget(Sprite sprite, Point position, String text) {
            this.sprite = sprite;
            this.rect = position == null ? sprite.rect : new Rect(position, sprite.rect.size);
            this.text = text;
        }

        void draw(Canvas canvas) {
            sprite.draw(canvas, rect);
            if (text != null) {
                canvas.drawText(text, rect.text.x, rect.text.y, sprite.paint);
            }
        }

        void onTouchDown(){}
    }
}
