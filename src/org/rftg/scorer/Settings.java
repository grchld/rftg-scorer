package org.rftg.scorer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author gc
 */
class Settings {

    Card.GameType gameType = Card.GameType.EXP3;
    boolean usePrestige;
    Size preferredCameraSize;

    void load(ObjectInputStream ois, CardInfo cardInfo) throws IOException, ClassNotFoundException {
        gameType = (Card.GameType) ois.readObject();
        usePrestige = ois.readBoolean();
        int preferredWidth = ois.readInt();
        int preferredHeight = ois.readInt();
        preferredCameraSize = preferredWidth == 0 ? null : new Size(preferredWidth, preferredHeight);
    }

    void save(ObjectOutputStream oos) throws IOException {
        oos.writeObject(gameType);
        oos.writeBoolean(usePrestige);
        oos.writeInt(preferredCameraSize == null ? 0 : preferredCameraSize.width);
        oos.writeInt(preferredCameraSize == null ? 0 : preferredCameraSize.height);
    }

}
