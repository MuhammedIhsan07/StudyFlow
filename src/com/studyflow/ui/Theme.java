package com.studyflow.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

/** Shared visual system for a consistent, product-style interface. */
final class Theme {
    static final Color BACKGROUND = new Color(246, 242, 234);
    static final Color SURFACE = new Color(255, 254, 250);
    static final Color SIDEBAR = new Color(243, 237, 228);
    static final Color PRIMARY = new Color(122, 92, 67);
    static final Color PRIMARY_DARK = new Color(62, 46, 37);
    static final Color PRIMARY_SOFT = new Color(235, 224, 209);
    static final Color TEXT = new Color(29, 29, 25);
    static final Color MUTED = new Color(112, 103, 94);
    static final Color BORDER = new Color(221, 212, 200);
    static final Color SUCCESS = new Color(82, 124, 93);
    static final Color SUCCESS_SOFT = new Color(229, 241, 230);
    static final Color WARNING = new Color(174, 125, 73);
    static final Color WARNING_SOFT = new Color(247, 235, 220);
    static final Color DANGER = new Color(160, 83, 70);
    static final Color DANGER_SOFT = new Color(246, 228, 223);
    static final Color SAGE = new Color(112, 139, 109);
    static final Color BOOK_PAPER = new Color(255, 252, 244);
    static final Color BOOK_EDGE = new Color(211, 197, 178);
    static final Color BOOK_SHADOW = new Color(62, 46, 37, 34);

    private Theme() { }

    static Font font(int size, int style) {
        return new Font("Segoe UI", style, size);
    }

    static Font regular(int size) { return font(size, Font.PLAIN); }
    static Font medium(int size) { return font(size, Font.BOLD); }

    static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    static void clickable(JComponent component) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
