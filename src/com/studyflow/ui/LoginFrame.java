package com.studyflow.ui;

import com.studyflow.model.AuthenticatedUser;
import com.studyflow.model.UserRole;
import com.studyflow.service.AccountManagementService;
import com.studyflow.service.AdaptivePlannerService;
import com.studyflow.service.MentorDashboardService;
import com.studyflow.ui.UiComponents.RoundedButton;
import com.studyflow.ui.UiComponents.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Optional;

/** Role-based sign-in window for students and Mentor/Admin users. */
public final class LoginFrame extends JFrame {
    private final AccountManagementService accountService = new AccountManagementService();
    private UserRole selectedRole = UserRole.STUDENT;
    private final RoundedButton studentRole = roleButton("Student");
    private final RoundedButton mentorRole = roleButton("Mentor / Admin");
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel roleDescription = text("Plan tasks, manage subjects, and track your learning.", 11, Theme.MUTED, Font.PLAIN);
    private final JLabel demoCredentials = text("", 11, Theme.PRIMARY_DARK, Font.BOLD);
    private final JLabel errorMessage = text(" ", 11, Theme.DANGER, Font.BOLD);

    public LoginFrame() {
        configureFrame();
        setContentPane(createInterface());
        updateSelectedRole(UserRole.STUDENT);
        getRootPane().setDefaultButton(findSignInButton(getContentPane()));
    }

    private void configureFrame() {
        setTitle("StudyFlow - Sign in");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }

    private JPanel createInterface() {
        BookLoginCanvas root = new BookLoginCanvas();
        root.setLayout(new BorderLayout());
        root.setBorder(Theme.padding(62, 74, 62, 74));

        JPanel content = transparent();
        content.setLayout(new GridLayout(1, 2, 42, 0));
        content.add(createWelcomePanel());
        content.add(createLoginCard());
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = transparent();
        panel.setBorder(Theme.padding(34, 26, 28, 20));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel brand = transparent();
        brand.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        RoundedPanel mark = new RoundedPanel(Theme.PRIMARY, 15);
        mark.setPreferredSize(new Dimension(46, 46));
        mark.setLayout(new BorderLayout());
        JLabel initial = text("S", 23, Color.WHITE, Font.BOLD);
        initial.setHorizontalAlignment(SwingConstants.CENTER);
        mark.add(initial);
        JPanel brandCopy = transparent();
        brandCopy.setLayout(new BoxLayout(brandCopy, BoxLayout.Y_AXIS));
        brandCopy.add(text("StudyFlow", 21, Theme.TEXT, Font.BOLD));
        brandCopy.add(text("ADAPTIVE STUDY PLANNER", 9, Theme.MUTED, Font.BOLD));
        brand.add(mark);
        brand.add(brandCopy);
        panel.add(brand);
        panel.add(Box.createVerticalStrut(64));
        panel.add(text("One planner.", 33, Theme.TEXT, Font.BOLD));
        panel.add(text("Two role-based workspaces.", 25, Theme.PRIMARY, Font.BOLD));
        panel.add(Box.createVerticalStrut(18));

        JTextArea summary = new JTextArea("Students build adaptive study plans while mentors and administrators monitor progress, identify learning gaps, and offer timely support.");
        summary.setFont(Theme.regular(14));
        summary.setForeground(Theme.MUTED);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setOpaque(false);
        summary.setEditable(false);
        summary.setFocusable(false);
        summary.setMinimumSize(new Dimension(360, 74));
        summary.setPreferredSize(new Dimension(420, 74));
        summary.setMaximumSize(new Dimension(420, 82));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(summary);
        panel.add(Box.createVerticalStrut(34));

        panel.add(feature("01", "Adaptive schedules", "Plans reorganize as study routines change."));
        panel.add(Box.createVerticalStrut(15));
        panel.add(feature("02", "Mentor visibility", "Progress signals guide timely student support."));
        panel.add(Box.createVerticalGlue());
        panel.add(text("A thoughtful space for better learning decisions.", 10, Theme.MUTED, Font.PLAIN));
        return panel;
    }

    private JPanel feature(String number, String title, String detail) {
        JPanel row = transparent();
        row.setLayout(new BorderLayout(13, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        RoundedPanel badge = new RoundedPanel(Theme.SUCCESS_SOFT, 12);
        badge.setPreferredSize(new Dimension(42, 42));
        badge.setLayout(new BorderLayout());
        JLabel numberLabel = text(number, 11, Theme.SUCCESS, Font.BOLD);
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badge.add(numberLabel);
        row.add(badge, BorderLayout.WEST);
        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(text(title, 13, Theme.TEXT, Font.BOLD));
        copy.add(text(detail, 10, Theme.MUTED, Font.PLAIN));
        row.add(copy, BorderLayout.CENTER);
        return row;
    }

    private JPanel createLoginCard() {
        RoundedPanel card = new RoundedPanel(new Color(255, 254, 250, 245), Theme.BORDER, 24);
        card.setBorder(Theme.padding(30, 32, 28, 32));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(text("SECURE WORKSPACE", 9, Theme.SUCCESS, Font.BOLD));
        card.add(Box.createVerticalStrut(7));
        card.add(text("Welcome back", 27, Theme.TEXT, Font.BOLD));
        card.add(Box.createVerticalStrut(5));
        roleDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(roleDescription);
        card.add(Box.createVerticalStrut(24));

        JPanel roleSelector = transparent();
        roleSelector.setLayout(new GridLayout(1, 2, 8, 0));
        roleSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        studentRole.addActionListener(event -> updateSelectedRole(UserRole.STUDENT));
        mentorRole.addActionListener(event -> updateSelectedRole(UserRole.MENTOR));
        roleSelector.add(studentRole);
        roleSelector.add(mentorRole);
        card.add(roleSelector);
        card.add(Box.createVerticalStrut(22));

        card.add(fieldLabel("Email address"));
        card.add(Box.createVerticalStrut(7));
        styleInput(emailField);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(emailField);
        card.add(Box.createVerticalStrut(15));
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(7));
        styleInput(passwordField);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(passwordField);

        JCheckBox showPassword = new JCheckBox("Show password");
        showPassword.setOpaque(false);
        showPassword.setFont(Theme.regular(10));
        showPassword.setForeground(Theme.MUTED);
        char echoCharacter = passwordField.getEchoChar();
        showPassword.addActionListener(event -> passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : echoCharacter));
        showPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(7));
        card.add(showPassword);

        errorMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(5));
        card.add(errorMessage);
        card.add(Box.createVerticalStrut(5));

        RoundedButton signIn = new RoundedButton("Sign in to StudyFlow", Theme.PRIMARY,
                Theme.PRIMARY_DARK, Color.WHITE, 13);
        signIn.setName("sign-in-button");
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        signIn.addActionListener(event -> signIn());
        card.add(signIn);
        card.add(Box.createVerticalStrut(18));

        RoundedPanel demo = new RoundedPanel(Theme.PRIMARY_SOFT, 14);
        demo.setBorder(Theme.padding(11, 13, 11, 13));
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        demo.setLayout(new BoxLayout(demo, BoxLayout.Y_AXIS));
        demo.add(text("DEMO CREDENTIALS", 8, Theme.MUTED, Font.BOLD));
        demo.add(Box.createVerticalStrut(3));
        demo.add(demoCredentials);
        card.add(demo);
        card.add(Box.createVerticalGlue());
        JLabel note = text("Role permissions are separated for this academic prototype.", 9, Theme.MUTED, Font.PLAIN);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(note);
        return card;
    }

    private void updateSelectedRole(UserRole role) {
        selectedRole = role;
        boolean student = role == UserRole.STUDENT;
        studentRole.setPalette(student ? Theme.PRIMARY : Theme.PRIMARY_SOFT,
                student ? Theme.PRIMARY_DARK : new Color(221, 206, 187),
                student ? Color.WHITE : Theme.PRIMARY_DARK);
        mentorRole.setPalette(student ? Theme.PRIMARY_SOFT : Theme.PRIMARY,
                student ? new Color(221, 206, 187) : Theme.PRIMARY_DARK,
                student ? Theme.PRIMARY_DARK : Color.WHITE);
        roleDescription.setText(student
                ? "Plan tasks and track personal progress."
                : "Review progress and support students.");
        emailField.setText(student ? "student@studyflow.com" : "mentor@studyflow.com");
        passwordField.setText(student ? "student123" : "mentor123");
        demoCredentials.setText(student
                ? "student@studyflow.com  /  student123"
                : "<html>Mentor: mentor@studyflow.com / mentor123<br>Admin: admin@studyflow.com / admin123</html>");
        errorMessage.setText(" ");
    }

    private void signIn() {
        char[] password = passwordField.getPassword();
        try {
            Optional<AuthenticatedUser> authenticated = accountService.authenticate(
                    emailField.getText(), password);
            if (!authenticated.isPresent()) {
                errorMessage.setText("Incorrect email or password.");
                passwordField.requestFocusInWindow();
                return;
            }

            AuthenticatedUser user = authenticated.get();
            boolean correctWorkspace = selectedRole == UserRole.STUDENT
                    ? user.getRole() == UserRole.STUDENT
                    : user.getRole() == UserRole.MENTOR || user.getRole() == UserRole.ADMIN;
            if (!correctWorkspace) {
                errorMessage.setText("Select the workspace assigned to this account.");
                return;
            }

            Runnable logout = () -> new LoginFrame().setVisible(true);
            if (user.getRole() == UserRole.STUDENT) {
                AdaptivePlannerService planner = new AdaptivePlannerService(user.getId(), user.getName(),
                        user.getEmail(), user.getProgram(), "student@studyflow.com".equals(user.getEmail()));
                new MainFrame(planner, logout).setVisible(true);
            } else {
                new MentorFrame(new MentorDashboardService(accountService), accountService,
                        user, logout).setVisible(true);
            }
            dispose();
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private RoundedButton roleButton(String label) {
        RoundedButton button = new RoundedButton(label, Theme.PRIMARY_SOFT,
                new Color(221, 206, 187), Theme.PRIMARY_DARK, 12);
        button.setFont(Theme.medium(12));
        return button;
    }

    private static JLabel fieldLabel(String value) {
        JLabel label = text(value, 10, Theme.MUTED, Font.BOLD);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void styleInput(JComponent input) {
        input.setFont(Theme.regular(12));
        input.setForeground(Theme.TEXT);
        input.setBackground(Color.WHITE);
        input.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                Theme.padding(9, 11, 9, 11)));
    }

    private static RoundedButton findSignInButton(Component component) {
        if (component instanceof RoundedButton && "sign-in-button".equals(component.getName())) {
            return (RoundedButton) component;
        }
        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                RoundedButton result = findSignInButton(child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static JPanel transparent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static JLabel text(String value, int size, Color color, int style) {
        JLabel label = new JLabel(value);
        label.setFont(Theme.font(size, style));
        label.setForeground(color);
        return label;
    }

    private static final class BookLoginCanvas extends JPanel {
        private static final long serialVersionUID = 1L;

        BookLoginCanvas() { setOpaque(false); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(53, 43, 36), getWidth(), getHeight(), new Color(28, 27, 23)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(184, 151, 114, 28));
            g2.fillOval(-130, -180, 500, 500);
            g2.setColor(new Color(92, 132, 102, 22));
            g2.fillOval(getWidth() - 360, getHeight() - 310, 520, 520);

            int left = 42;
            int right = getWidth() - 42;
            int top = 34;
            int bottom = getHeight() - 34;
            int center = getWidth() / 2;
            g2.setColor(new Color(0, 0, 0, 48));
            g2.fillRoundRect(left + 5, top + 9, right - left - 4, bottom - top, 34, 34);

            Path2D book = new Path2D.Double();
            book.moveTo(center, top + 12);
            book.curveTo(center - 35, top, center - 74, top, left + 25, top);
            book.curveTo(left + 6, top, left, top + 15, left, top + 34);
            book.lineTo(left, bottom - 28);
            book.curveTo(left + 9, bottom - 6, left + 27, bottom, left + 53, bottom);
            book.lineTo(center - 50, bottom);
            book.curveTo(center - 28, bottom, center - 10, bottom + 4, center, bottom + 12);
            book.curveTo(center + 10, bottom + 4, center + 28, bottom, center + 50, bottom);
            book.lineTo(right - 53, bottom);
            book.curveTo(right - 27, bottom, right - 9, bottom - 6, right, bottom - 28);
            book.lineTo(right, top + 34);
            book.curveTo(right, top + 15, right - 6, top, right - 25, top);
            book.curveTo(center + 74, top, center + 35, top, center, top + 12);
            book.closePath();
            g2.setColor(Theme.BOOK_PAPER);
            g2.fill(book);
            g2.setColor(Theme.BOOK_EDGE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(book);

            g2.setPaint(new GradientPaint(center - 16, 0, new Color(80, 56, 40, 0),
                    center, 0, new Color(80, 56, 40, 34)));
            g2.fillRect(center - 16, top + 8, 16, bottom - top);
            g2.setPaint(new GradientPaint(center, 0, new Color(80, 56, 40, 34),
                    center + 16, 0, new Color(80, 56, 40, 0)));
            g2.fillRect(center, top + 8, 16, bottom - top);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
