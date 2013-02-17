package org.rftg.scorer;

import java.io.Serializable;

/**
 * @author gc
 */
class Player implements Serializable {

    String name;
    int chips;

    int getTotalScores() {
        return 12;
    }
}
