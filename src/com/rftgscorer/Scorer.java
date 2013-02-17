package com.rftgscorer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class Scorer extends Activity {
    private CardsLoader cardsLoader;
    private State state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cardsLoader = new CardsLoader(this);

        state = State.loadState(this);
        if (state == null) {
            state = new State();
        }

        setContentView(R.layout.main);
/*
        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        int i = 0;
        for (Player player : state.players) {
            TabHost.TabSpec tabSpec = tabHost.newTabSpec("" + (i++));
            tabSpec.setIndicator(player.name);

            tabSpec.setContent(new PlayerContext(player));
            tabHost.addTab(tabSpec);
        }
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        state.saveState(this);
    }



    class PlayerContext implements TabHost.TabContentFactory {

        private Player player;

        PlayerContext(Player player) {
            this.player = player;
        }

        @Override
        public View createTabContent(String s) {
            View view = getLayoutInflater().inflate(R.layout.scorer, null);
            final EditText chipsCount = (EditText) view.findViewById(R.id.chipsCount);
            chipsCount.setText("" + player.chips);
            chipsCount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                    try {
                        player.chips = Integer.parseInt(charSequence.toString());
                    } catch (NumberFormatException e) {
                        player.chips = 0;
                        chipsCount.setText("" + player.chips);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            view.findViewById(R.id.chipsMinus).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (--player.chips < 0) {
                        player.chips = 0;
                    }
                    chipsCount.setText("" + player.chips);
                }
            });

            view.findViewById(R.id.chipsPlus).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    player.chips++;
                    chipsCount.setText("" + player.chips);
                }
            });

            final GridView cardGrid = (GridView) view.findViewById(R.id.cardGrid);

            cardGrid.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return 190;
                }

                @Override
                public Object getItem(int i) {
                    return null;
                }

                @Override
                public long getItemId(int i) {
                    return 0;
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {

                    ImageView imageView = (ImageView)(view != null?view:getLayoutInflater().inflate(R.layout.grid_card, null));

                    int resourceId = Scorer.this.getResources().getIdentifier("card_" + i, "drawable", getPackageName());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    options.inScaled = true;

                    Bitmap bitmap = BitmapFactory.decodeResource(Scorer.this.getResources(), resourceId, options);

                    imageView.setImageBitmap(bitmap);
                    return imageView;
                }
            });

//            ArrayAdapter<Card> adapter = new ArrayAdapter<Card>(this, R.layout.item, R.id.tvText, data);
//            gvMain.setAdapter(adapter);

//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Scorer.this, android.R.layout.simple_spinner_item, new String[]{"2","3","2","3","2","3","2","3","2","3","2","3","2","3","2","3","2","3","d"});
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//
//            Spinner cardSelector = (Spinner) view.findViewById(R.id.cardSelector);
//            cardSelector.setAdapter(adapter);
//            cardSelector.setPrompt("Card:");

//        cardSelector.setAdapter(new ArrayAdapter<Card>(this, R.layout.selector_item, R.layout.selector_text, cardsLoader.cards.toArray(new Card[cardsLoader.cards.size()])));

            return view;
        }
    }

}
