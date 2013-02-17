package org.rftg.scorer;

import android.app.Activity;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author gc
 */
public class CardsLoader {

    List<Card> cards;

    CardsLoader(AssetManager assetManager) {
        try {
            InputStream inputStream = assetManager.open("cards.txt");
            try {
                cards = loadCards(inputStream);
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    CardsLoader(Activity activity) {
        this(activity.getAssets());
    }

    static List<Card> loadCards(InputStream inputStream) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            List<Card> cards = new ArrayList<Card>();
            Card card = null;
            int id = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                String[] s = line.split(":");
                switch (s[0].charAt(0)) {
                    case 'N':
                        if (card != null) {
                            cards.add(card);
                        }
                        card = new Card();
                        card.id = id++;
                        card.name = s[1];
                        break;
                    case 'T':
                        switch (s[1].charAt(0)) {
                            case '1':
                                card.cardType = Card.CardType.WORLD;
                                break;
                            case '2':
                                card.cardType = Card.CardType.DEVELOPMENT;
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        card.cost = Integer.parseInt(s[2]);
                        card.vp = Integer.parseInt(s[3]);
                        break;
                    case 'E':
                        card.count = new EnumMap<Card.GameType, Integer>(Card.GameType.class);
                        card.count.put(Card.GameType.BASE, Integer.parseInt(s[1]));
                        card.count.put(Card.GameType.EXP1, Integer.parseInt(s[2]));
                        card.count.put(Card.GameType.EXP2, Integer.parseInt(s[3]));
                        card.count.put(Card.GameType.EXP3, Integer.parseInt(s[4]));
                        break;
                    case 'G':
                        card.goodType = Card.GoodType.valueOf(s[1]);
                        break;
                    case 'F':
                        card.flags = new HashSet<Card.Flags>();
                        for (String flag : s[1].split("(\\s|\\|)+")) {
                            card.flags.add(Card.Flags.valueOf(flag));
                        }
                        break;
                    case 'P':
                        break;
                    case 'V':
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
            cards.add(card);

            return cards;
        } finally {
            inputStream.close();
        }
    }
}
