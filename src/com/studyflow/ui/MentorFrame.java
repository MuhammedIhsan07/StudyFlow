package com.studyflow.ui;

import com.studyflow.model.AuthenticatedUser;
import com.studyflow.model.StudentProgressSummary;
import com.studyflow.model.StudentProgressSummary.ProgressStatus;
import com.studyflow.model.StudentProgressSummary.SubjectProgress;
import com.studyflow.service.MentorDashboardService;
import com.studyflow.ui.UiComponents.Avatar;
import com.studyflow.ui.UiComponents.RoundedButton;
import com.studyflow.ui.UiComponents.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Separate progress-monitoring workspace for mentors and administrators. */
public final class MentorFrame extends JFrame {
    private final MentorDashboardService mentorService;
    private final AuthenticatedUser mentor;
    private final Runnable logoutAction;
    private final JPanel pageHost = new JPanel(new BorderLayout());
    private final Map<String, RoundedButton> studentButtons = new LinkedHashMap<>();
    private StudentProgressSummary selectedStudent;

    public MentorFrame(MentorDashboardService mentorService, AuthenticatedUser mentor, Runnable logoutAction) {
        this.mentorService = mentorService;
        this.mentor = mentor;
        this.logoutAction = logoutAction;
        List<StudentProgressSummary> students = mentorService.getStudents();
        selectedStudent = students.isEmpty() ? null : students.get(0);
        configureFrame();
        buildInterface(students);
        renderDashboard();
    }

    private void configureFrame() {
        setTitle("StudyFlow - Mentor/Admin Dashboard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 710));
        setSize(1400, 870);
        setLocationRelativeTo(null);
    }

    private void buildInterface(List<StudentProgressSummary> students) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(createSidebar(students), BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(Theme.BACKGROUND);
        workspace.add(createHeader(), BorderLayout.NORTH);
        pageHost.setBackground(Theme.BACKGROUND);
        workspace.add(pageHost, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createSidebar(List<StudentProgressSummary> students) {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(270, 0));
        sidebar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                Theme.padding(24, 18, 18, 18)));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel brand = transparent();
        brand.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        RoundedPanel mark = new RoundedPanel(Theme.PRIMARY_DARK, 14);
        mark.setPreferredSize(new Dimension(42, 42));
        mark.setLayout(new BorderLayout());
        JLabel markText = label("S", 21, Color.WHITE, Font.BOLD);
        markText.setHorizontalAlignment(SwingConstants.CENTER);
        mark.add(markText);
        JPanel brandCopy = transparent();
        brandCopy.setLayout(new BoxLayout(brandCopy, BoxLayout.Y_AXIS));
        brandCopy.add(label("StudyFlow", 19, Theme.TEXT, Font.BOLD));
        brandCopy.add(label("MENTOR PORTAL", 9, Theme.SUCCESS, Font.BOLD));
        brand.add(mark);
        brand.add(brandCopy);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(32));

        sidebar.add(sidebarLabel("COHORT OVERVIEW"));
        sidebar.add(Box.createVerticalStrut(9));
        RoundedPanel cohort = new RoundedPanel(Theme.PRIMARY_SOFT, 14);
        cohort.setAlignmentX(Component.LEFT_ALIGNMENT);
        cohort.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        cohort.setBorder(Theme.padding(11, 13, 11, 13));
        cohort.setLayout(new BorderLayout());
        JPanel cohortText = transparent();
        cohortText.setLayout(new BoxLayout(cohortText, BoxLayout.Y_AXIS));
        cohortText.add(label("Computer Science", 12, Theme.PRIMARY_DARK, Font.BOLD));
        cohortText.add(label(students.size() + " active students", 10, Theme.MUTED, Font.PLAIN));
        cohort.add(cohortText, BorderLayout.CENTER);
        cohort.add(label(String.valueOf(mentorService.getAverageProgress()) + "%", 14,
                Theme.PRIMARY, Font.BOLD), BorderLayout.EAST);
        sidebar.add(cohort);
        sidebar.add(Box.createVerticalStrut(25));

        sidebar.add(sidebarLabel("STUDENTS"));
        sidebar.add(Box.createVerticalStrut(8));
        for (StudentProgressSummary student : students) {
            RoundedButton button = new RoundedButton(student.getInitials() + "   " + student.getName()
                    + "   " + student.getOverallProgress() + "%", new Color(0, 0, 0, 0),
                    new Color(235, 224, 209), Theme.MUTED, 13);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(Theme.regular(12));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            button.addActionListener(event -> selectStudent(student));
            studentButtons.put(student.getId(), button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }
        sidebar.add(Box.createVerticalGlue());

        RoundedPanel account = new RoundedPanel(new Color(232, 241, 231), 15);
        account.setAlignmentX(Component.LEFT_ALIGNMENT);
        account.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        account.setBorder(Theme.padding(10, 11, 10, 11));
        account.setLayout(new BorderLayout(10, 0));
        account.add(new Avatar(initials(mentor.getName()), 40, Theme.SUCCESS), BorderLayout.WEST);
        JPanel accountText = transparent();
        accountText.setLayout(new BoxLayout(accountText, BoxLayout.Y_AXIS));
        accountText.add(label(mentor.getName(), 11, Theme.TEXT, Font.BOLD));
        accountText.add(label("Mentor / Administrator", 9, Theme.MUTED, Font.PLAIN));
        account.add(accountText, BorderLayout.CENTER);
        sidebar.add(account);
        sidebar.add(Box.createVerticalStrut(10));
        RoundedButton signOut = new RoundedButton("Sign out", Theme.PRIMARY_SOFT,
                new Color(221, 206, 187), Theme.PRIMARY_DARK, 11);
        signOut.setAlignmentX(Component.LEFT_ALIGNMENT);
        signOut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        signOut.addActionListener(event -> {
            dispose();
            logoutAction.run();
        });
        sidebar.add(signOut);
        return sidebar;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setPreferredSize(new Dimension(0, 88));
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                Theme.padding(15, 28, 14, 28)));
        JPanel title = transparent();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(label("Mentor dashboard", 24, Theme.TEXT, Font.BOLD));
        title.add(label("Monitor progress and support every learner", 12, Theme.MUTED, Font.PLAIN));
        header.add(title, BorderLayout.WEST);

        JPanel right = transparent();
        right.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        RoundedPanel roleBadge = new RoundedPanel(Theme.SUCCESS_SOFT, 12);
        roleBadge.setBorder(Theme.padding(9, 13, 9, 13));
        roleBadge.add(label("Verified Mentor/Admin", 10, Theme.SUCCESS, Font.BOLD));
        right.add(roleBadge);
        right.add(new Avatar(initials(mentor.getName()), 38, Theme.PRIMARY_DARK));
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void selectStudent(StudentProgressSummary student) {
        selectedStudent = student;
        renderDashboard();
    }

    private void renderDashboard() {
        studentButtons.forEach((id, button) -> {
            boolean selected = selectedStudent != null && selectedStudent.getId().equals(id);
            button.setPalette(selected ? Theme.PRIMARY : new Color(0, 0, 0, 0),
                    selected ? Theme.PRIMARY_DARK : Theme.PRIMARY_SOFT,
                    selected ? Color.WHITE : Theme.MUTED);
            button.setFont(selected ? Theme.medium(12) : Theme.regular(12));
        });

        MentorBookPage page = new MentorBookPage();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(Theme.padding(28, 30, 34, 30));

        JPanel intro = transparent();
        intro.setLayout(new BorderLayout());
        intro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        JPanel introText = transparent();
        introText.setLayout(new BoxLayout(introText, BoxLayout.Y_AXIS));
        introText.add(label("Student progress center", 21, Theme.TEXT, Font.BOLD));
        introText.add(label("Select a student to review their latest learning signals.", 12, Theme.MUTED, Font.PLAIN));
        intro.add(introText, BorderLayout.WEST);
        RoundedButton export = new RoundedButton("Progress report", Theme.PRIMARY,
                Theme.PRIMARY_DARK, Color.WHITE, 12);
        export.addActionListener(event -> showReport());
        intro.add(export, BorderLayout.EAST);
        page.add(intro);
        page.add(Box.createVerticalStrut(20));
        page.add(metricRow());
        page.add(Box.createVerticalStrut(20));

        if (selectedStudent != null) {
            JPanel details = transparent();
            details.setLayout(new GridLayout(1, 2, 18, 0));
            details.setAlignmentX(Component.CENTER_ALIGNMENT);
            details.setPreferredSize(new Dimension(1000, 520));
            details.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            details.add(progressCard(selectedStudent));
            details.add(activityCard(selectedStudent));
            page.add(details);
        }

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        pageHost.removeAll();
        pageHost.add(scroll, BorderLayout.CENTER);
        pageHost.revalidate();
        pageHost.repaint();
    }

    private JPanel metricRow() {
        JPanel row = transparent();
        row.setLayout(new GridLayout(1, 4, 14, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
        int total = mentorService.getStudents().size();
        row.add(metric("TOTAL STUDENTS", String.valueOf(total), "active learners", Theme.PRIMARY));
        row.add(metric("ON TRACK", String.valueOf(mentorService.getOnTrackCount()), "meeting goals", Theme.SUCCESS));
        row.add(metric("NEEDS SUPPORT", String.valueOf(mentorService.getNeedsAttentionCount()), "priority review", Theme.WARNING));
        row.add(metric("AVERAGE", mentorService.getAverageProgress() + "%", "cohort progress", Theme.SAGE));
        return row;
    }

    private JPanel metric(String title, String value, String caption, Color accent) {
        RoundedPanel card = new RoundedPanel(Theme.SURFACE, Theme.BORDER, 17);
        card.setBorder(Theme.padding(16, 18, 16, 18));
        card.setLayout(new BorderLayout(12, 0));
        RoundedPanel marker = new RoundedPanel(tint(accent, .13f), 13);
        marker.setPreferredSize(new Dimension(43, 43));
        marker.setLayout(new BorderLayout());
        JLabel dot = label("●", 14, accent, Font.BOLD);
        dot.setHorizontalAlignment(SwingConstants.CENTER);
        marker.add(dot);
        card.add(marker, BorderLayout.WEST);
        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(label(title, 9, Theme.MUTED, Font.BOLD));
        copy.add(label(value, 20, Theme.TEXT, Font.BOLD));
        copy.add(label(caption, 9, Theme.MUTED, Font.PLAIN));
        card.add(copy, BorderLayout.CENTER);
        return card;
    }

    private JPanel progressCard(StudentProgressSummary student) {
        RoundedPanel card = sectionCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JPanel identity = transparent();
        identity.setLayout(new BorderLayout(14, 0));
        identity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        Color statusColor = statusColor(student.getStatus());
        identity.add(new Avatar(student.getInitials(), 54, statusColor), BorderLayout.WEST);
        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(label(student.getName(), 17, Theme.TEXT, Font.BOLD));
        copy.add(label(student.getId() + "  •  " + student.getProgram(), 10, Theme.MUTED, Font.PLAIN));
        copy.add(label(student.getEmail(), 9, Theme.MUTED, Font.PLAIN));
        identity.add(copy, BorderLayout.CENTER);
        identity.add(pill(student.getStatus().toString(), statusColor), BorderLayout.EAST);
        card.add(identity);
        card.add(Box.createVerticalStrut(17));

        JPanel overallHeader = transparent();
        overallHeader.setLayout(new BorderLayout());
        overallHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        overallHeader.add(label("Overall learning progress", 11, Theme.TEXT, Font.BOLD), BorderLayout.WEST);
        overallHeader.add(label(student.getOverallProgress() + "%", 18, statusColor, Font.BOLD), BorderLayout.EAST);
        card.add(overallHeader);
        card.add(Box.createVerticalStrut(7));
        card.add(UiComponents.progressBar(student.getOverallProgress(), statusColor));
        card.add(Box.createVerticalStrut(21));
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(17));
        card.add(label("SUBJECT BREAKDOWN", 9, Theme.MUTED, Font.BOLD));
        card.add(Box.createVerticalStrut(13));
        for (SubjectProgress subject : student.getSubjects()) {
            Color color = parseColor(subject.getColorHex());
            JPanel row = transparent();
            row.setLayout(new BorderLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
            row.add(label(subject.getName(), 11, Theme.TEXT, Font.BOLD), BorderLayout.WEST);
            row.add(label(subject.getCode() + "  " + subject.getProgress() + "%", 10, Theme.MUTED, Font.BOLD), BorderLayout.EAST);
            card.add(row);
            card.add(Box.createVerticalStrut(6));
            card.add(UiComponents.progressBar(subject.getProgress(), color));
            card.add(Box.createVerticalStrut(13));
        }
        return card;
    }

    private JPanel activityCard(StudentProgressSummary student) {
        RoundedPanel card = sectionCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(label("Engagement summary", 16, Theme.TEXT, Font.BOLD));
        card.add(label("Current-week signals for mentor review", 10, Theme.MUTED, Font.PLAIN));
        card.add(Box.createVerticalStrut(17));

        JPanel stats = transparent();
        stats.setLayout(new GridLayout(1, 2, 10, 0));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        stats.add(smallStat(formatDuration(student.getWeeklyMinutes()), "Study time"));
        stats.add(smallStat(String.valueOf(student.getCompletedTasks()), "Tasks completed"));
        card.add(stats);
        card.add(Box.createVerticalStrut(20));
        card.add(label("RECENT ACTIVITY", 9, Theme.MUTED, Font.BOLD));
        card.add(Box.createVerticalStrut(11));
        for (String activity : student.getRecentActivity()) {
            RoundedPanel item = new RoundedPanel(new Color(248, 245, 239), 12);
            item.setBorder(Theme.padding(10, 12, 10, 12));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            item.setLayout(new BorderLayout(10, 0));
            item.add(label("●", 10, Theme.SUCCESS, Font.BOLD), BorderLayout.WEST);
            item.add(label(activity, 10, Theme.TEXT, Font.PLAIN), BorderLayout.CENTER);
            card.add(item);
            card.add(Box.createVerticalStrut(8));
        }
        card.add(Box.createVerticalGlue());

        RoundedPanel insight = new RoundedPanel(statusBackground(student.getStatus()), 14);
        insight.setBorder(Theme.padding(12, 13, 12, 13));
        insight.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        insight.setLayout(new BoxLayout(insight, BoxLayout.Y_AXIS));
        insight.add(label("MENTOR INSIGHT", 8, statusColor(student.getStatus()), Font.BOLD));
        insight.add(label(insightText(student), 10, Theme.TEXT, Font.PLAIN));
        card.add(insight);
        card.add(Box.createVerticalStrut(13));
        RoundedButton note = new RoundedButton("Add mentor note", Theme.PRIMARY,
                Theme.PRIMARY_DARK, Color.WHITE, 12);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        note.addActionListener(event -> addNote(student));
        card.add(note);
        return card;
    }

    private JPanel smallStat(String value, String caption) {
        RoundedPanel stat = new RoundedPanel(Theme.PRIMARY_SOFT, 13);
        stat.setBorder(Theme.padding(11, 13, 11, 13));
        stat.setLayout(new BoxLayout(stat, BoxLayout.Y_AXIS));
        stat.add(label(value, 18, Theme.PRIMARY_DARK, Font.BOLD));
        stat.add(label(caption, 9, Theme.MUTED, Font.PLAIN));
        return stat;
    }

    private RoundedPanel sectionCard() {
        RoundedPanel card = new RoundedPanel(Theme.SURFACE, Theme.BORDER, 18);
        card.setBorder(Theme.padding(21, 22, 21, 22));
        return card;
    }

    private void showReport() {
        if (selectedStudent == null) return;
        String message = selectedStudent.getName() + "\n\nOverall progress: "
                + selectedStudent.getOverallProgress() + "%\nWeekly study time: "
                + formatDuration(selectedStudent.getWeeklyMinutes()) + "\nCompleted tasks: "
                + selectedStudent.getCompletedTasks() + "\nStatus: " + selectedStudent.getStatus();
        JOptionPane.showMessageDialog(this, message, "Student progress report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addNote(StudentProgressSummary student) {
        String note = JOptionPane.showInputDialog(this,
                "Write a private mentor note for " + student.getName() + ":",
                "Add mentor note", JOptionPane.PLAIN_MESSAGE);
        if (note != null && !note.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mentor note saved for this session.",
                    "Note added", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static JLabel pill(String text, Color color) {
        JLabel label = label(text, 9, color, Font.BOLD);
        label.setOpaque(true);
        label.setBackground(tint(color, .12f));
        label.setBorder(Theme.padding(5, 9, 5, 9));
        return label;
    }

    private static JLabel sidebarLabel(String text) {
        JLabel label = label(text, 9, new Color(142, 132, 121), Font.BOLD);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(Theme.padding(0, 9, 0, 0));
        return label;
    }

    private static String insightText(StudentProgressSummary student) {
        if (student.getStatus() == ProgressStatus.NEEDS_ATTENTION) {
            return "Progress is below 50%. Review missed tasks and arrange a short check-in.";
        }
        if (student.getStatus() == ProgressStatus.ON_TRACK) {
            return "The student is meeting targets and maintaining healthy study consistency.";
        }
        return "Progress is steady. One additional focused session could improve weekly balance.";
    }

    private static Color statusColor(ProgressStatus status) {
        if (status == ProgressStatus.ON_TRACK) return Theme.SUCCESS;
        if (status == ProgressStatus.NEEDS_ATTENTION) return Theme.DANGER;
        return Theme.WARNING;
    }

    private static Color statusBackground(ProgressStatus status) {
        if (status == ProgressStatus.ON_TRACK) return Theme.SUCCESS_SOFT;
        if (status == ProgressStatus.NEEDS_ATTENTION) return Theme.DANGER_SOFT;
        return Theme.WARNING_SOFT;
    }

    private static Color parseColor(String value) {
        try { return Color.decode(value); } catch (NumberFormatException exception) { return Theme.PRIMARY; }
    }

    private static Color tint(Color color, float amount) {
        int red = Math.round(255 + (color.getRed() - 255) * amount);
        int green = Math.round(255 + (color.getGreen() - 255) * amount);
        int blue = Math.round(255 + (color.getBlue() - 255) * amount);
        return new Color(Math.min(255, red), Math.min(255, green), Math.min(255, blue));
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + "m";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return remainder == 0 ? hours + "h" : hours + "h " + remainder + "m";
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1)
                + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "")).toUpperCase();
    }

    private static JPanel transparent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(size, style));
        label.setForeground(color);
        return label;
    }

    private static final class MentorBookPage extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;
        MentorBookPage() { setOpaque(false); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int center = width / 2;
            g2.setColor(Theme.BACKGROUND);
            g2.fillRect(0, 0, width, height);
            g2.setColor(Theme.BOOK_SHADOW);
            g2.fillRoundRect(12, 14, Math.max(20, width - 22), Math.max(20, height - 20), 28, 28);
            g2.setPaint(new GradientPaint(10, 0, new Color(247, 239, 226), center, 0, Theme.BOOK_PAPER));
            g2.fillRoundRect(9, 7, Math.max(20, width / 2), Math.max(20, height - 16), 25, 25);
            g2.setPaint(new GradientPaint(center, 0, Theme.BOOK_PAPER, width - 9, 0, new Color(247, 239, 226)));
            g2.fillRoundRect(center, 7, Math.max(20, width / 2 - 9), Math.max(20, height - 16), 25, 25);
            g2.setPaint(new GradientPaint(center - 12, 0, new Color(80, 56, 40, 0),
                    center, 0, new Color(80, 56, 40, 28)));
            g2.fillRect(center - 12, 12, 12, Math.max(20, height - 24));
            g2.setPaint(new GradientPaint(center, 0, new Color(80, 56, 40, 28),
                    center + 12, 0, new Color(80, 56, 40, 0)));
            g2.fillRect(center, 12, 12, Math.max(20, height - 24));
            g2.dispose();
            super.paintComponent(graphics);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return Math.max(80, visibleRect.height - 60);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
