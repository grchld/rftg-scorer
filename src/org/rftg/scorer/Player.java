package org.rftg.scorer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gc
 */
class Player {

    List<Card> cards = new ArrayList<Card>();
    int chips;

    void load(ObjectInputStream ois, CardInfo cardInfo) throws IOException, ClassNotFoundException {
        chips = ois.readInt();
        int[] cardIds = (int[]) ois.readObject();
        for (int id : cardIds) {
            cards.add(cardInfo.cards[id]);
        }
    }

    void save(ObjectOutputStream oos) throws IOException {
        oos.writeInt(chips);
        int[] cardIds = new int[cards.size()];
        int i = 0;
        for (Card card : cards) {
            cardIds[i++] = card.id;
        }
        oos.writeObject(cardIds);
    }
}
