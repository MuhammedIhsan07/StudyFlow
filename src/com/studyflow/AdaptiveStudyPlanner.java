package com.studyflow;

import com.studyflow.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. */
public final class AdaptiveStudyPlanner {
    private AdaptiveStudyPlanner() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Custom-painted components also work with Swing's default theme.
            }
            new LoginFrame().setVisible(true);
        });
    }
}
