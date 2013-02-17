package org.rftg.scorer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author gc
 */
public class Test {

    public static void main(String[] args) throws Exception {
        InputStream is = new FileInputStream("assets/cards.txt");
        try {
            List<Card> cards = CardsLoader.loadCards(is);
        } finally {
            is.close();
        }
    }
}
