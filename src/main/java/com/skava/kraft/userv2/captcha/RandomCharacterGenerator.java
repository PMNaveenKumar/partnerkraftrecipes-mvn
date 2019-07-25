package com.skava.kraft.userv2.captcha;

import java.util.Random;

public class RandomCharacterGenerator {

    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private Random random;

    public RandomCharacterGenerator() {
        random = new Random();
    }

    public String[] generateCharacters(Integer characterSize) {
        String[] characters = new String[characterSize];

        for (int i = 0; i < characterSize; i++) {
            characters[i] = String.valueOf(chars.charAt(random.nextInt(chars.length())));
        }

        return characters;
    }

    public Random getRandom() {
        return random;
    }
}
