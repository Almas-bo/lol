package com.farmgame.engine.ui.editor;

import com.farmgame.engine.ui.editor.SyntaxHighlighter.Kind;
import com.farmgame.engine.ui.editor.SyntaxHighlighter.LineTokens;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntaxHighlighterTest {

    private static Kind kindAt(LineTokens tokens, int col) {
        return tokens.spans().stream().filter(s -> s.start() <= col && col < s.end())
                .findFirst().orElseThrow().kind();
    }

    @Test
    void recognizesMainTokenKinds() {
        String line = "for (int x = 10; x < 8; x++) robot.say(\"for\"); // for";
        LineTokens t = SyntaxHighlighter.tokenize(line, false);
        assertEquals(Kind.KEYWORD, kindAt(t, 0));
        assertEquals(Kind.KEYWORD, kindAt(t, 5));
        assertEquals(Kind.NUMBER, kindAt(t, 13));
        assertEquals(Kind.METHOD, kindAt(t, line.indexOf("say")));
        assertEquals(Kind.STRING, kindAt(t, line.indexOf("\"for\"") + 1));
        assertEquals(Kind.COMMENT, kindAt(t, line.indexOf("//") + 3));
    }

    @Test
    void typesAndAnnotations() {
        LineTokens t = SyntaxHighlighter.tokenize("@Override Crop c = Crop.of(CropType.CORN);", false);
        assertEquals(Kind.ANNOTATION, kindAt(t, 0));
        assertEquals(Kind.TYPE, kindAt(t, 10));
    }

    @Test
    void blockCommentsSpanLines() {
        LineTokens first = SyntaxHighlighter.tokenize("int a; /* start", false);
        assertTrue(first.endsInBlockComment());
        LineTokens second = SyntaxHighlighter.tokenize("still comment */ int b;", true);
        assertEquals(Kind.COMMENT, kindAt(second, 0));
        assertEquals(Kind.KEYWORD, kindAt(second, 17));
        assertFalse(second.endsInBlockComment());
    }

    @Test
    void spansCoverWholeLine() {
        String line = "  robot.moveTo(3, 2);  ";
        LineTokens t = SyntaxHighlighter.tokenize(line, false);
        int pos = 0;
        for (var span : t.spans()) {
            assertEquals(pos, span.start());
            pos = span.end();
        }
        assertEquals(line.length(), pos);
    }
}
