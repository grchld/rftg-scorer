package org.rftg.scorer;

import android.app.Activity;
import android.content.res.AssetManager;
import org.rftg.scorer.Card.Extra;
import org.rftg.scorer.Card.ExtraType;
import org.rftg.scorer.Card.Flag;
import org.rftg.scorer.Card.Power;
import org.rftg.scorer.Card.PowerType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.rftg.scorer.Card.Phase.*;

/**
 * @author gc
 */
class CardInfo {

    Card cards[];

    CardInfo(AssetManager assetManager) {
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

    CardInfo(Activity activity) {
        this(activity.getAssets());
    }

    static Card[] loadCards(InputStream inputStream) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            List<Card> cards = new ArrayList<Card>();
            Map<String, Card> cardByNames = new HashMap<String, Card>();
            Card card = null;
            int id = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                }
                String[] s = line.split(":");
                switch (s[0].charAt(0)) {
                    case 'N':
                        if (card != null) {
                            cards.add(card);
                            cardByNames.put(card.name, card);
                        }
                        card = new Card();
                        card.id = id++;
                        card.name = s[1];
                        if ("Gambling World".equals(card.name)) {
                            card.gamblingWorld = true;
                        }
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
                        card.count.put(Card.GameType.BASE, Integer.parseInt(s[1]));
                        card.count.put(Card.GameType.EXP1, Integer.parseInt(s[2]));
                        card.count.put(Card.GameType.EXP2, Integer.parseInt(s[3]));
                        card.count.put(Card.GameType.EXP3, Integer.parseInt(s[4]));
                        break;
                    case 'G':
                        card.goodType = Card.GoodType.valueOf(s[1]);
                        break;
                    case 'F':
                        for (String flag : s[1].split("(\\s|\\|)+")) {
                            card.flags.add(Flag.valueOf(flag));
                        }
                        break;
                    case 'P':
                        Power power = new Power();
                        for (String powerType : s[2].split("(\\s|\\|)+")) {
                            power.powers.add(PowerType.valueOf(powerType));
                        }
                        int phaseNum = Integer.parseInt(s[1]);
                        switch (phaseNum) {
                            case 1:
                                power.phase = EXPLORE;
                                break;
                            case 2:
                                power.phase = DEVELOP;
                                break;
                            case 3:
                                power.phase = SETTLE;
                                break;
                            case 4:
                                if (Collections.disjoint(Card.TRADE_POWERS, power.powers)) {
                                    power.phase = CONSUME;
                                } else {
                                    power.phase = TRADE;
                                }
                                break;
                            case 5:
                                power.phase = PRODUCE;
                                break;
                            default:
                                throw new IllegalStateException("Unknown phase: " + phaseNum);
                        }
                        power.value = Integer.parseInt(s[3]);
                        power.times = Integer.parseInt(s[4]);
                        card.powers.add(power);
                        card.phasePowers.add(power.phase);
                        break;
                    case 'V':
                        Extra extra = new Extra();
                        extra.vp = Integer.parseInt(s[1]);
                        extra.extraType = ExtraType.valueOf(s[2]);
                        extra.name = s[3];
                        card.extras.add(extra);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
            cards.add(card);
            cardByNames.put(card.name, card);

            for (Card c : cards) {
                for (Extra extra : c.extras) {
                    if (!"N/A".equals(extra.name)) {
                        extra.namedCard = cardByNames.get(extra.name);
                        if (extra.namedCard == null) {
                            throw new IllegalStateException("Card not found: " + extra.name);
                        }
                    }
                }
            }

            return cards.toArray(new Card[cards.size()]);
        } finally {
            inputStream.close();
        }
    }
}
