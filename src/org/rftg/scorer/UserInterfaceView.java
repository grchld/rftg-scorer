package org.rftg.scorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author gc
 */
public class UserInterfaceView extends View {

    private MainContext mainContext;
    private UserInterfaceResources userInterfaceResources;

    private List<Widget> widgets = new ArrayList<Widget>();

    private Card magnifiedCard;

    private long[] cardAddTimes = new long[Card.GameType.EXP3.totalCardNum];
    private List<CardMatch> collectedCardMatches = new ArrayList<CardMatch>(Card.GameType.EXP3.totalCardNum);

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

    public Size getViewSize() {
        return new Size(getWidth(), getHeight());
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

    private Bitmap bitmap;

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
         /*
        if (mainContext.recognizer.debugPicture != null) {
            Paint p = new Paint();
            p.setARGB(255, 0, 0, 0);
            canvas.drawRect(600, 0, 800, 200, p);
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ALPHA_8);
            }

            if (mainContext.recognizer.debugPicture != null) {
                synchronized (mainContext.recognizer.debugPicture) {
                    mainContext.recognizer.debugPicture.position(0);
                    bitmap.copyPixelsFromBuffer(mainContext.recognizer.debugPicture);
                }
            }

            p.setARGB(255, 255, 255, 255);
            canvas.drawBitmap(bitmap, new android.graphics.Rect(0, 0, 64, 64), new android.graphics.Rect(600, 0, 800, 200), p);
        }
           */
        /*
        if (mainContext.recognizer.calcHoughLeft != null) {
            for (Line line : mainContext.recognizer.calcHoughLeft.lines) {
                canvas.drawLine(line.x1, line.y1, line.x2, line.y2, userInterfaceResources.PAINT_RED);
            }
        }
        if (mainContext.recognizer.calcHoughRight != null) {
            for (Line line : mainContext.recognizer.calcHoughRight.lines) {
                canvas.drawLine(line.x1, line.y1, line.x2, line.y2, userInterfaceResources.PAINT_GREEN);
            }
        }
        if (mainContext.recognizer.calcHoughTop != null) {
            for (Line line : mainContext.recognizer.calcHoughTop.lines) {
                canvas.drawLine(line.x1, line.y1, line.x2, line.y2, userInterfaceResources.PAINT_YELLOW);
            }
        }
        if (mainContext.recognizer.calcHoughBottom != null) {
            for (Line line : mainContext.recognizer.calcHoughBottom.lines) {
                canvas.drawLine(line.x1, line.y1, line.x2, line.y2, userInterfaceResources.PAINT_MAGENTA);
            }
        }
        */
/*
        if (mainContext.recognizer.debugRectangles != null) {
            for (Point[] points : mainContext.recognizer.debugRectangles) {
                Point p0 = translate(points[0]);
                Point p1 = translate(points[1]);
                Point p2 = translate(points[2]);
                Point p3 = translate(points[3]);

                canvas.drawLine(p0.x, p0.y, p1.x, p1.y, userInterfaceResources.PAINT_GREEN);
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, userInterfaceResources.PAINT_GREEN);
                canvas.drawLine(p2.x, p2.y, p3.x, p3.y, userInterfaceResources.PAINT_GREEN);
                canvas.drawLine(p3.x, p3.y, p0.x, p0.y, userInterfaceResources.PAINT_GREEN);
            }
        }
        */

        long now = System.currentTimeMillis();
        /*
        List<Card> cards = mainContext.state.player.cards;
        for (int i = 0 ; i < cardAddTimes.length ; i++) {
            if (!cards.contains(mainContext.cardInfo.cards[i])) {
                cardAddTimes[i] = 0;
            }
        } */

        collectedCardMatches.clear();
        synchronized (mainContext.recognizer.collectedCardMatches) {
            collectedCardMatches.addAll(mainContext.recognizer.collectedCardMatches);
        }

        for (CardMatch match : collectedCardMatches) {
            if (cardAddTimes[match.cardNumber] == 0) {
                cardAddTimes[match.cardNumber] = now;
            }

            long old = now - 200;

            Point[] points = match.rect;
            Point p0 = translate(points[0]);
            Point p1 = translate(points[1]);
            Point p2 = translate(points[2]);
            Point p3 = translate(points[3]);

            Paint paint = cardAddTimes[match.cardNumber] >= old
                    ? userInterfaceResources.PAINT_BORDER_NEW
                    : userInterfaceResources.PAINT_BORDER_OLD;
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
            canvas.drawLine(p2.x, p2.y, p3.x, p3.y, paint);
            canvas.drawLine(p3.x, p3.y, p0.x, p0.y, paint);
        }

        for (CardMatch match : collectedCardMatches) {
            Point[] points = match.rect;
            Point p0 = translate(points[0]);

            canvas.drawText(mainContext.cardInfo.cards[match.cardNumber].name,
                    p0.x + userInterfaceResources.screenProperties.cardNameOffsetX,
                    p0.y + userInterfaceResources.screenProperties.cardNameOffsetY,
                    userInterfaceResources.PAINT_CARD_NAME);
/*
            canvas.drawText(" " + match.score +" > " + match.secondScore,
                    p0.x + userInterfaceResources.screenProperties.cardNameOffsetX,
                    p0.y+40 + userInterfaceResources.screenProperties.cardNameOffsetY,
                    userInterfaceResources.PAINT_CARD_NAME);
            canvas.drawText(mainContext.cardInfo.cards[match.secondCardNumber].name,
                    p0.x + userInterfaceResources.screenProperties.cardNameOffsetX,
                    p0.y+80 + userInterfaceResources.screenProperties.cardNameOffsetY,
                    userInterfaceResources.PAINT_CARD_NAME);
                    */
        }

        updateWidgets();

        for (Widget widget : widgets) {
            widget.draw(canvas);
        }
    }

    private Point translate(Point point) {
        return new Point(
                mainContext.fastCamera.getLeft() + point.x * mainContext.fastCamera.getWidth() / mainContext.recognizer.frameSize.width,
                mainContext.fastCamera.getTop() + point.y * mainContext.fastCamera.getHeight() / mainContext.recognizer.frameSize.height);
    }

    private void updateWidgets() {
        widgets.clear();
        final State state = mainContext.state;
        final Player player = state.player;
        widgets.add(new Widget(userInterfaceResources.getResetIcon(), null, null){
            @Override
            void onTouchDown() {
                Arrays.fill(cardAddTimes, 0);
                state.player.cards = new ArrayList<Card>();
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
