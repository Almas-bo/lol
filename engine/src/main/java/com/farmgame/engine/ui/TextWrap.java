package com.farmgame.engine.ui;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/** Перенос текста по словам под заданную ширину (для панелей на Java2D). */
public final class TextWrap {

    private TextWrap() {
    }

    public static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth || line.isEmpty()) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    result.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
                // Очень длинное слово режем по символам.
                while (fm.stringWidth(line.toString()) > maxWidth && line.length() > 1) {
                    int cut = line.length() - 1;
                    while (cut > 1 && fm.stringWidth(line.substring(0, cut)) > maxWidth) {
                        cut--;
                    }
                    result.add(line.substring(0, cut));
                    line.delete(0, cut);
                }
            }
            result.add(line.toString());
        }
        return result;
    }
}
