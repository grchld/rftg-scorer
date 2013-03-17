package org.rftg.scorer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author gc
 */
class Settings {

    Card.GameType gameType = Card.GameType.EXP1;

    void load(ObjectInputStream ois, CardInfo cardInfo) throws IOException, ClassNotFoundException {
        gameType = (Card.GameType) ois.readObject();
    }

    void save(ObjectOutputStream oos) throws IOException {
        oos.writeObject(gameType);
    }

}
