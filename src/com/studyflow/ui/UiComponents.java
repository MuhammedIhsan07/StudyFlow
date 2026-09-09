package com.studyflow.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class UiComponents {
    private UiComponents() { }

    static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(size, style));
        label.setForeground(color);
        return label;
    }

    static JPanel transparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    static JProgressBar progressBar(int value, Color color) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(Math.max(0, Math.min(100, value)));
        bar.setForeground(color);
        bar.setBackground(new Color(233, 229, 221));
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setPreferredSize(new Dimension(100, 8));
        bar.setUI(new RoundedProgressUI());
        return bar;
    }

    static final class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;
        private Color stroke;

        RoundedPanel(Color fill, int radius) {
            this(fill, null, radius);
        }

        RoundedPanel(Color fill, Color stroke, int radius) {
            this.fill = fill;
            this.stroke = stroke;
            this.radius = radius;
            setOpaque(false);
        }

        void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    static class RoundedButton extends JButton {
        private Color normal;
        private Color hover;
        private Color textColor;
        private final int radius;
        private boolean over;

        RoundedButton(String text, Color normal, Color hover, Color textColor, int radius) {
            super(text);
            this.normal = normal;
            this.hover = hover;
            this.textColor = textColor;
            this.radius = radius;
            setFont(Theme.medium(13));
            setForeground(textColor);
            setBorder(Theme.padding(10, 16, 10, 16));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            Theme.clickable(this);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { over = false; repaint(); }
            });
        }

        void setPalette(Color normal, Color hover, Color textColor) {
            this.normal = normal;
            this.hover = hover;
            this.textColor = textColor;
            setForeground(textColor);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(over && isEnabled() ? hover : normal);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    static final class NavButton extends RoundedButton {
        private boolean selected;

        NavButton(String text) {
            super(text, new Color(0, 0, 0, 0), Theme.PRIMARY_SOFT, Theme.MUTED, 14);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(Theme.font(14, Font.PLAIN));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        }

        void setSelectedState(boolean selected) {
            this.selected = selected;
            if (selected) {
                setPalette(Theme.PRIMARY_SOFT, Theme.PRIMARY_SOFT, Theme.PRIMARY_DARK);
                setFont(Theme.medium(14));
            } else {
                setPalette(new Color(0, 0, 0, 0), new Color(239, 232, 222), Theme.MUTED);
                setFont(Theme.regular(14));
            }
        }
    }

    static final class Avatar extends JComponent {
        private String initials;
        private final Color color;

        Avatar(String initials, int size, Color color) {
            this.initials = initials;
            this.color = color;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        void setInitials(String initials) {
            this.initials = initials;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.medium(Math.max(11, getWidth() / 3)));
            int width = g2.getFontMetrics().stringWidth(initials);
            int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(initials, (getWidth() - width) / 2, y);
            g2.dispose();
        }
    }

    private static final class RoundedProgressUI extends BasicProgressBarUI {
        @Override
        protected void paintDeterminate(Graphics graphics, JComponent component) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = progressBar.getWidth();
            int height = progressBar.getHeight();
            g2.setColor(progressBar.getBackground());
            g2.fillRoundRect(0, 0, width, height, height, height);
            int amount = getAmountFull(progressBar.getInsets(), width, height);
            g2.setColor(progressBar.getForeground());
            g2.fillRoundRect(0, 0, amount, height, height, height);
            g2.dispose();
        }
    }

    static Border cardPadding() {
        return Theme.padding(20, 22, 20, 22);
    }
}
