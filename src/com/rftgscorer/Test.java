package com.rftgscorer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
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
