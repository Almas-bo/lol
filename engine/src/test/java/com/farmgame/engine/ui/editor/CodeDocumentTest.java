package com.farmgame.engine.ui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeDocumentTest {

    private final CodeDocument doc = new CodeDocument();

    private void typeAll(String s) {
        for (char c : s.toCharArray()) {
            doc.type(c);
        }
    }

    @Test
    void typingAndCyrillic() {
        typeAll("robot.say(\"Привет\");");
        assertEquals("robot.say(\"Привет\");", doc.text());
        assertEquals(20, doc.caretCol());
    }

    @Test
    void enterAfterBraceIndentsAndClosingBraceDedents() {
        typeAll("for (;;) {");
        doc.newline();
        typeAll("robot.water();");
        doc.newline();
        typeAll("}");
        assertEquals("for (;;) {\n    robot.water();\n}", doc.text());
    }

    @Test
    void enterBetweenBracesOpensBlock() {
        typeAll("{}");
        doc.moveLeft(false, false);
        doc.newline();
        assertEquals("{\n    \n}", doc.text());
        assertEquals(1, doc.caretLine());
        assertEquals(4, doc.caretCol());
    }

    @Test
    void backspaceRemovesWholeIndentLevel() {
        doc.load("        x");
        doc.setCaret(0, 8, false);
        doc.backspace();
        assertEquals("    x", doc.text());
    }

    @Test
    void backspaceAtLineStartJoinsLines() {
        doc.load("ab\ncd");
        doc.setCaret(1, 0, false);
        doc.backspace();
        assertEquals("abcd", doc.text());
        assertEquals(2, doc.caretCol());
    }

    @Test
    void selectionReplaceAndCopy() {
        doc.load("hello world");
        doc.setCaret(0, 6, false);
        doc.end(true);
        assertEquals("world", doc.selectedText());
        typeAll("farm");
        assertEquals("hello farm", doc.text());
        assertFalse(doc.hasSelection());
    }

    @Test
    void multiLineInsertAndSelection() {
        doc.load("A\nB");
        doc.setCaret(0, 1, false);
        doc.insert("1\n2\n3");
        assertEquals("A1\n2\n3\nB", doc.text());
        doc.selectAll();
        assertEquals("A1\n2\n3\nB", doc.selectedText());
        doc.deleteSelection();
        assertEquals("", doc.text());
    }

    @Test
    void undoRedo() {
        doc.load("x");
        doc.end(false);
        typeAll("yz");
        assertTrue(doc.undo());
        assertEquals("xy", doc.text());
        assertTrue(doc.undo());
        assertEquals("x", doc.text());
        assertFalse(doc.undo());
        assertTrue(doc.redo());
        assertEquals("xy", doc.text());
    }

    @Test
    void toggleCommentKeepsIndent() {
        doc.load("    robot.water();");
        doc.toggleComment();
        assertEquals("    // robot.water();", doc.text());
        doc.toggleComment();
        assertEquals("    robot.water();", doc.text());
    }

    @Test
    void tabAndUntabSelectedLines() {
        doc.load("a\nb");
        doc.selectAll();
        doc.tab();
        assertEquals("    a\n    b", doc.text());
        doc.untab();
        assertEquals("a\nb", doc.text());
    }

    @Test
    void verticalMovementRemembersColumn() {
        doc.load("long line here\nab\nanother long line");
        doc.setCaret(0, 10, false);
        doc.moveVertical(1, false);
        assertEquals(2, doc.caretCol());
        doc.moveVertical(1, false);
        assertEquals(10, doc.caretCol());
    }

    @Test
    void smartHome() {
        doc.load("    code");
        doc.end(false);
        doc.home(false);
        assertEquals(4, doc.caretCol());
        doc.home(false);
        assertEquals(0, doc.caretCol());
    }
}
