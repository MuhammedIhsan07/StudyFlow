package com.studyflow.ui;

import com.studyflow.model.StudentProfile;
import com.studyflow.model.StudyTask;
import com.studyflow.model.Subject;
import com.studyflow.model.TaskFactory;
import com.studyflow.model.TaskPriority;
import com.studyflow.model.TaskStatus;
import com.studyflow.model.TaskType;
import com.studyflow.service.AdaptivePlannerService;
import com.studyflow.ui.UiComponents.Avatar;
import com.studyflow.ui.UiComponents.NavButton;
import com.studyflow.ui.UiComponents.RoundedButton;
import com.studyflow.ui.UiComponents.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Main professional desktop interface for the adaptive planner. */
public final class MainFrame extends JFrame {
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private enum Page {
        OVERVIEW("Overview", "Your personalized study command center"),
        SCHEDULE("My schedule", "A balanced view of your study week"),
        SUBJECTS("Subjects", "Manage courses and weekly learning goals"),
        TASKS("All tasks", "Prioritize work and stay ahead of deadlines"),
        PROGRESS("Progress", "Understand your performance and consistency"),
        PROFILE("Student profile", "Personalize how StudyFlow plans your day");

        final String title;
        final String subtitle;
        Page(String title, String subtitle) { this.title = title; this.subtitle = subtitle; }
    }

    private final AdaptivePlannerService planner;
    private final Runnable logoutAction;
    private final JPanel pageHost = new JPanel(new BorderLayout());
    private final JLabel pageTitle = UiComponents.label("", 24, Theme.TEXT, Font.BOLD);
    private final JLabel pageSubtitle = UiComponents.label("", 12, Theme.MUTED, Font.PLAIN);
    private final JLabel profileName = UiComponents.label("", 13, Theme.TEXT, Font.BOLD);
    private final JLabel profileProgram = UiComponents.label("", 11, Theme.MUTED, Font.PLAIN);
    private final Avatar sidebarAvatar;
    private final Avatar headerAvatar;
    private final Map<Page, NavButton> navButtons = new EnumMap<>(Page.class);
    private JTextField searchField;
    private Page activePage = Page.OVERVIEW;
    private int weekOffset;
    private String taskFilter = "All";
    private String taskQuery = "";

    public MainFrame(AdaptivePlannerService planner) {
        this(planner, null);
    }

    public MainFrame(AdaptivePlannerService planner, Runnable logoutAction) {
        this.planner = planner;
        this.logoutAction = logoutAction;
        sidebarAvatar = new Avatar(planner.getProfile().getInitials(), 42, Theme.PRIMARY);
        headerAvatar = new Avatar(planner.getProfile().getInitials(), 38, Theme.PRIMARY);
        configureFrame();
        buildInterface();
        installKeyboardShortcuts();
        showPage(Page.OVERVIEW);
    }

    private void configureFrame() {
        setTitle("StudyFlow - Adaptive Study Planner");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1380, 860);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(createSidebar(), BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(Theme.BACKGROUND);
        workspace.add(createHeader(), BorderLayout.NORTH);
        pageHost.setBackground(Theme.BACKGROUND);
        workspace.add(pageHost, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                Theme.padding(24, 18, 18, 18)));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel brand = UiComponents.transparentPanel();
        brand.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        RoundedPanel mark = new RoundedPanel(Theme.PRIMARY, 14);
        mark.setPreferredSize(new Dimension(42, 42));
        mark.setLayout(new BorderLayout());
        JLabel markText = UiComponents.label("S", 22, Color.WHITE, Font.BOLD);
        markText.setHorizontalAlignment(SwingConstants.CENTER);
        mark.add(markText);
        JPanel brandText = UiComponents.transparentPanel();
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.add(UiComponents.label("StudyFlow", 19, Theme.TEXT, Font.BOLD));
        brandText.add(UiComponents.label("ADAPTIVE PLANNER", 9, Theme.MUTED, Font.BOLD));
        brand.add(mark);
        brand.add(brandText);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(34));
        sidebar.add(sectionLabel("WORKSPACE"));
        sidebar.add(Box.createVerticalStrut(8));

        addNav(sidebar, Page.OVERVIEW, "   Overview");
        addNav(sidebar, Page.SCHEDULE, "   My schedule");
        addNav(sidebar, Page.SUBJECTS, "   Subjects");
        addNav(sidebar, Page.TASKS, "   All tasks");
        addNav(sidebar, Page.PROGRESS, "   Progress");
        sidebar.add(Box.createVerticalStrut(22));
        sidebar.add(sectionLabel("ACCOUNT"));
        sidebar.add(Box.createVerticalStrut(8));
        addNav(sidebar, Page.PROFILE, "   Student profile");
        sidebar.add(Box.createVerticalStrut(20));

        RoundedButton addTask = primaryButton("+  Add study task");
        addTask.setAlignmentX(Component.LEFT_ALIGNMENT);
        addTask.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addTask.addActionListener(event -> showTaskDialog());
        sidebar.add(addTask);
        sidebar.add(Box.createVerticalGlue());

        RoundedPanel tip = new RoundedPanel(new Color(238, 231, 220), 18);
        tip.setLayout(new BorderLayout(10, 0));
        tip.setBorder(Theme.padding(14, 14, 14, 14));
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip.setPreferredSize(new Dimension(210, 72));
        tip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        JLabel bulb = UiComponents.label("AI", 12, Theme.PRIMARY, Font.BOLD);
        tip.add(bulb, BorderLayout.WEST);
        JPanel tipText = UiComponents.transparentPanel();
        tipText.setLayout(new BoxLayout(tipText, BoxLayout.Y_AXIS));
        tipText.add(UiComponents.label("Smart planning", 12, Theme.PRIMARY_DARK, Font.BOLD));
        JLabel note = UiComponents.label("Adapts when plans change.", 10, Theme.MUTED, Font.PLAIN);
        tipText.add(note);
        tip.add(tipText, BorderLayout.CENTER);
        sidebar.add(tip);
        sidebar.add(Box.createVerticalStrut(18));
        sidebar.add(new JSeparator());
        sidebar.add(Box.createVerticalStrut(16));

        JPanel account = UiComponents.transparentPanel();
        account.setAlignmentX(Component.LEFT_ALIGNMENT);
        account.setLayout(new BorderLayout(10, 0));
        account.setPreferredSize(new Dimension(210, 48));
        account.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JPanel avatarWrap = UiComponents.transparentPanel();
        avatarWrap.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 3));
        avatarWrap.add(sidebarAvatar);
        account.add(avatarWrap, BorderLayout.WEST);
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(profileName);
        copy.add(profileProgram);
        account.add(copy, BorderLayout.CENTER);
        account.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        account.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) { showPage(Page.PROFILE); }
        });
        sidebar.add(account);
        if (logoutAction != null) {
            sidebar.add(Box.createVerticalStrut(9));
            RoundedButton signOut = new RoundedButton("Sign out", new Color(235, 224, 209),
                    new Color(221, 206, 187), Theme.PRIMARY_DARK, 11);
            signOut.setAlignmentX(Component.LEFT_ALIGNMENT);
            signOut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            signOut.addActionListener(event -> {
                dispose();
                logoutAction.run();
            });
            sidebar.add(signOut);
        }
        updateProfileChrome();
        return sidebar;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = UiComponents.label(text, 10, new Color(142, 132, 121), Font.BOLD);
        label.setBorder(Theme.padding(0, 10, 0, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void addNav(JPanel sidebar, Page page, String label) {
        NavButton button = new NavButton(label);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> showPage(page));
        navButtons.put(page, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setPreferredSize(new Dimension(0, 88));
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                Theme.padding(15, 28, 14, 28)));

        JPanel titleBlock = UiComponents.transparentPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(pageTitle);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(pageSubtitle);
        header.add(titleBlock, BorderLayout.WEST);

        JPanel actions = UiComponents.transparentPanel();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        searchField = new PlaceholderTextField("Search tasks or subjects", 20);
        searchField.setFont(Theme.regular(13));
        searchField.setForeground(Theme.TEXT);
        searchField.setBackground(new Color(250, 248, 243));
        searchField.setToolTipText("Search tasks or subjects");
        searchField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        searchField.addActionListener(event -> {
            taskQuery = searchField.getText().trim();
            taskFilter = "All";
            showPage(Page.TASKS);
        });
        actions.add(searchField);

        RoundedButton reminder = softButton("●  Reminders");
        reminder.addActionListener(event -> showReminders());
        actions.add(reminder);
        actions.add(headerAvatar);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private void installKeyboardShortcuts() {
        getRootPane().registerKeyboardAction(event -> {
            searchField.requestFocusInWindow();
            searchField.selectAll();
        }, javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(event -> showTaskDialog(),
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void showPage(Page page) {
        activePage = page;
        pageTitle.setText(page.title);
        pageSubtitle.setText(page.subtitle);
        navButtons.forEach((key, button) -> button.setSelectedState(key == page));
        pageHost.removeAll();
        JPanel content;
        switch (page) {
            case SCHEDULE: content = createSchedulePage(); break;
            case SUBJECTS: content = createSubjectsPage(); break;
            case TASKS: content = createTasksPage(); break;
            case PROGRESS: content = createProgressPage(); break;
            case PROFILE: content = createProfilePage(); break;
            default: content = createOverviewPage();
        }
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        pageHost.add(scroll, BorderLayout.CENTER);
        pageHost.revalidate();
        pageHost.repaint();
        updateProfileChrome();
    }

    private JPanel pageCanvas() {
        JPanel page = new ResponsivePage();
        page.setBackground(Theme.BACKGROUND);
        page.setBorder(Theme.padding(28, 30, 34, 30));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        return page;
    }

    private JPanel createOverviewPage() {
        JPanel page = pageCanvas();
        StudentProfile profile = planner.getProfile();
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";

        JPanel intro = UiComponents.transparentPanel();
        intro.setLayout(new BorderLayout());
        intro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JPanel introText = UiComponents.transparentPanel();
        introText.setLayout(new BoxLayout(introText, BoxLayout.Y_AXIS));
        introText.add(UiComponents.label(greeting + ", " + firstName(profile.getName()), 23, Theme.TEXT, Font.BOLD));
        introText.add(Box.createVerticalStrut(5));
        introText.add(UiComponents.label(LocalDate.now().format(LONG_DATE) + "  •  Let's make today count.", 13, Theme.MUTED, Font.PLAIN));
        intro.add(introText, BorderLayout.WEST);
        RoundedButton planButton = primaryButton("+  Plan a session");
        planButton.addActionListener(event -> showTaskDialog());
        intro.add(planButton, BorderLayout.EAST);
        page.add(intro);
        page.add(Box.createVerticalStrut(20));

        page.add(createFocusBanner());
        page.add(Box.createVerticalStrut(20));
        page.add(createMetricRow());
        page.add(Box.createVerticalStrut(20));

        JPanel lower = UiComponents.transparentPanel();
        lower.setLayout(new GridLayout(1, 2, 18, 0));
        lower.setAlignmentX(Component.CENTER_ALIGNMENT);
        lower.setPreferredSize(new Dimension(1000, 650));
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        lower.add(createTodayCard());
        JPanel right = UiComponents.transparentPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(createUpcomingCard());
        right.add(Box.createVerticalStrut(18));
        right.add(createSubjectSnapshot());
        lower.add(right);
        page.add(lower);
        return page;
    }

    private JPanel createFocusBanner() {
        GradientPanel banner = new GradientPanel();
        banner.setLayout(new BorderLayout(22, 0));
        banner.setBorder(Theme.padding(22, 26, 22, 26));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        RoundedPanel sparkle = new RoundedPanel(new Color(255, 255, 255, 48), 18);
        sparkle.setPreferredSize(new Dimension(74, 74));
        sparkle.setLayout(new BorderLayout());
        JLabel icon = UiComponents.label("AI", 21, Color.WHITE, Font.BOLD);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        sparkle.add(icon);
        banner.add(sparkle, BorderLayout.WEST);

        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label("SMART FOCUS", 10, new Color(246, 239, 228), Font.BOLD));
        StudyTask recommendation = planner.getRecommendedTask().orElse(null);
        String title = recommendation == null ? "Your plan is clear" : recommendation.getTitle();
        String detail = recommendation == null
                ? "Add a task and StudyFlow will prioritize it for you."
                : planner.getSubject(recommendation.getSubjectId()).getName() + "  •  "
                + recommendation.getDurationMinutes() + " min  •  " + recommendation.getPriority() + " priority";
        copy.add(Box.createVerticalStrut(6));
        copy.add(UiComponents.label(title, 20, Color.WHITE, Font.BOLD));
        copy.add(Box.createVerticalStrut(7));
        copy.add(UiComponents.label(detail, 12, new Color(246, 239, 228), Font.PLAIN));
        banner.add(copy, BorderLayout.CENTER);
        if (recommendation != null) {
            RoundedButton start = new RoundedButton("Mark complete", Color.WHITE, new Color(244, 238, 229),
                    Theme.PRIMARY_DARK, 14);
            start.addActionListener(event -> {
                planner.toggleCompleted(recommendation.getId());
                toast("Great work — task completed.", Theme.SUCCESS);
                showPage(activePage);
            });
            JPanel actionWrap = UiComponents.transparentPanel();
            actionWrap.setLayout(new GridBagLayout());
            actionWrap.add(start);
            banner.add(actionWrap, BorderLayout.EAST);
        }
        return banner;
    }

    private JPanel createMetricRow() {
        JPanel row = UiComponents.transparentPanel();
        row.setLayout(new GridLayout(1, 4, 14, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        int planned = planner.getTodayPlannedMinutes();
        int complete = planner.getTodayCompletedMinutes();
        row.add(metricCard("TODAY'S PLAN", formatDuration(planned), planner.getTasksForDate(LocalDate.now()).size() + " sessions", Theme.PRIMARY, "PL"));
        row.add(metricCard("COMPLETED", String.valueOf(planner.getCompletedCount()), "tasks in total", Theme.SUCCESS, "OK"));
        row.add(metricCard("THIS WEEK", formatDuration(planner.getWeekCompletedMinutes()), "focused time", Theme.WARNING, "WK"));
        row.add(metricCard("OVERALL", planner.getOverallProgress() + "%", "goal progress", Theme.SAGE, "%"));
        return row;
    }

    private JPanel metricCard(String label, String value, String caption, Color color, String iconText) {
        RoundedPanel card = new RoundedPanel(Theme.SURFACE, Theme.BORDER, 18);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(Theme.padding(16, 17, 16, 17));
        RoundedPanel icon = new RoundedPanel(tint(color, .12f), 14);
        icon.setPreferredSize(new Dimension(45, 45));
        icon.setLayout(new BorderLayout());
        JLabel symbol = UiComponents.label(iconText, 20, color, Font.BOLD);
        symbol.setHorizontalAlignment(SwingConstants.CENTER);
        icon.add(symbol);
        card.add(icon, BorderLayout.WEST);
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label(label, 9, Theme.MUTED, Font.BOLD));
        copy.add(Box.createVerticalStrut(3));
        copy.add(UiComponents.label(value, 21, Theme.TEXT, Font.BOLD));
        copy.add(UiComponents.label(caption, 10, Theme.MUTED, Font.PLAIN));
        card.add(copy, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTodayCard() {
        RoundedPanel card = sectionCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(sectionHeader("Today's study plan", "Your sessions in priority order", "View all", event -> showPage(Page.TASKS)));
        card.add(Box.createVerticalStrut(13));
        List<StudyTask> today = planner.getTasksForDate(LocalDate.now());
        if (today.isEmpty()) {
            card.add(emptyState("No sessions planned", "Add a study task to build today's plan."));
        } else {
            today.sort(Comparator.comparingInt((StudyTask task) -> task.getUrgencyScore(LocalDate.now())).reversed());
            for (StudyTask task : today) {
                card.add(taskRow(task, true));
                card.add(Box.createVerticalStrut(9));
            }
        }
        return card;
    }

    private JPanel createUpcomingCard() {
        RoundedPanel card = sectionCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(sectionHeader("Upcoming deadlines", "Next 7 days", null, null));
        card.add(Box.createVerticalStrut(12));
        List<StudyTask> upcoming = planner.getUpcomingTasks(7).stream().limit(3).collect(Collectors.toList());
        if (upcoming.isEmpty()) {
            card.add(emptyState("You're all caught up", "No deadlines in the next seven days."));
        } else {
            for (StudyTask task : upcoming) {
                Subject subject = planner.getSubject(task.getSubjectId());
                JPanel line = UiComponents.transparentPanel();
                line.setLayout(new BorderLayout(10, 0));
                line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                JLabel dot = UiComponents.label("●", 15, parseColor(subject.getColorHex()), Font.PLAIN);
                line.add(dot, BorderLayout.WEST);
                JPanel copy = UiComponents.transparentPanel();
                copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
                copy.add(UiComponents.label(task.getTitle(), 12, Theme.TEXT, Font.BOLD));
                copy.add(UiComponents.label(subject.getCode() + "  •  " + relativeDate(task.getDueDate()), 10, Theme.MUTED, Font.PLAIN));
                line.add(copy, BorderLayout.CENTER);
                JLabel badge = pill(task.getPriority().toString(), priorityColor(task.getPriority()));
                line.add(badge, BorderLayout.EAST);
                card.add(line);
                card.add(Box.createVerticalStrut(9));
            }
        }
        return card;
    }

    private JPanel createSubjectSnapshot() {
        RoundedPanel card = sectionCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(sectionHeader("Subject momentum", "Weekly target progress", "Subjects", event -> showPage(Page.SUBJECTS)));
        card.add(Box.createVerticalStrut(12));
        for (Subject subject : planner.getSubjects().stream().limit(3).collect(Collectors.toList())) {
            JPanel title = UiComponents.transparentPanel();
            title.setLayout(new BorderLayout());
            title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            title.add(UiComponents.label(subject.getName(), 11, Theme.TEXT, Font.BOLD), BorderLayout.WEST);
            title.add(UiComponents.label(subject.getProgressPercent() + "%", 11, Theme.MUTED, Font.BOLD), BorderLayout.EAST);
            card.add(title);
            card.add(Box.createVerticalStrut(6));
            card.add(UiComponents.progressBar(subject.getProgressPercent(), parseColor(subject.getColorHex())));
            card.add(Box.createVerticalStrut(12));
        }
        return card;
    }

    private JPanel createSchedulePage() {
        JPanel page = pageCanvas();
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset);
        LocalDate sunday = monday.plusDays(6);
        JPanel toolbar = UiComponents.transparentPanel();
        toolbar.setLayout(new BorderLayout());
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label("Weekly study plan", 21, Theme.TEXT, Font.BOLD));
        copy.add(UiComponents.label(monday.format(SHORT_DATE) + " — " + sunday.format(SHORT_DATE), 12, Theme.MUTED, Font.PLAIN));
        toolbar.add(copy, BorderLayout.WEST);
        JPanel controls = UiComponents.transparentPanel();
        controls.setLayout(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        RoundedButton previous = softButton("<");
        previous.addActionListener(event -> { weekOffset--; showPage(Page.SCHEDULE); });
        RoundedButton current = softButton("Today");
        current.addActionListener(event -> { weekOffset = 0; showPage(Page.SCHEDULE); });
        RoundedButton next = softButton(">");
        next.addActionListener(event -> { weekOffset++; showPage(Page.SCHEDULE); });
        RoundedButton add = primaryButton("+  Add task");
        add.addActionListener(event -> showTaskDialog());
        controls.add(previous); controls.add(current); controls.add(next); controls.add(add);
        toolbar.add(controls, BorderLayout.EAST);
        page.add(toolbar);
        page.add(Box.createVerticalStrut(20));

        JPanel week = UiComponents.transparentPanel();
        week.setLayout(new GridLayout(1, 7, 10, 0));
        week.setPreferredSize(new Dimension(1180, 440));
        week.setMaximumSize(new Dimension(Integer.MAX_VALUE, 440));
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            LocalDate date = monday.plusDays(dayIndex);
            week.add(dayColumn(date));
        }
        page.add(week);
        page.add(Box.createVerticalStrut(20));
        page.add(createAdaptiveInfoCard());
        return page;
    }

    private JPanel dayColumn(LocalDate date) {
        boolean today = date.equals(LocalDate.now());
        RoundedPanel column = new RoundedPanel(today ? new Color(239, 235, 225) : Theme.SURFACE,
                today ? new Color(184, 157, 133) : Theme.BORDER, 18);
        column.setBorder(Theme.padding(14, 12, 14, 12));
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        JLabel name = UiComponents.label(date.getDayOfWeek().toString().substring(0, 3), 10,
                today ? Theme.PRIMARY : Theme.MUTED, Font.BOLD);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setMinimumSize(new Dimension(60, 16));
        name.setPreferredSize(new Dimension(60, 16));
        name.setMaximumSize(new Dimension(60, 16));
        name.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel number = UiComponents.label(String.valueOf(date.getDayOfMonth()), 24,
                today ? Theme.PRIMARY_DARK : Theme.TEXT, Font.BOLD);
        number.setAlignmentX(Component.CENTER_ALIGNMENT);
        number.setMinimumSize(new Dimension(60, 34));
        number.setPreferredSize(new Dimension(60, 34));
        number.setMaximumSize(new Dimension(60, 34));
        number.setHorizontalAlignment(SwingConstants.CENTER);
        column.add(name); column.add(Box.createVerticalStrut(3)); column.add(number);
        column.add(Box.createVerticalStrut(16));
        List<StudyTask> dayTasks = planner.getTasksForDate(date);
        if (dayTasks.isEmpty()) {
            JLabel open = UiComponents.label("Open day", 10, new Color(154, 146, 136), Font.PLAIN);
            open.setAlignmentX(Component.CENTER_ALIGNMENT);
            column.add(open);
        } else {
            for (StudyTask task : dayTasks) {
                Subject subject = planner.getSubject(task.getSubjectId());
                RoundedPanel mini = new RoundedPanel(tint(parseColor(subject.getColorHex()), .10f), 12);
                mini.setBorder(Theme.padding(10, 9, 10, 9));
                mini.setLayout(new BoxLayout(mini, BoxLayout.Y_AXIS));
                mini.setAlignmentX(Component.LEFT_ALIGNMENT);
                mini.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
                JLabel code = UiComponents.label(subject.getCode(), 9, parseColor(subject.getColorHex()), Font.BOLD);
                mini.add(code);
                mini.add(Box.createVerticalStrut(4));
                JLabel title = UiComponents.label(ellipsize(task.getTitle(), 20), 10,
                        task.getStatus() == TaskStatus.COMPLETED ? Theme.MUTED : Theme.TEXT, Font.BOLD);
                mini.add(title);
                mini.add(Box.createVerticalStrut(5));
                mini.add(UiComponents.label(task.getStartTime().format(TIME) + " • " + task.getDurationMinutes() + "m",
                        9, Theme.MUTED, Font.PLAIN));
                column.add(mini);
                column.add(Box.createVerticalStrut(8));
            }
        }
        return column;
    }

    private JPanel createAdaptiveInfoCard() {
        RoundedPanel info = new RoundedPanel(new Color(232, 241, 231), 18);
        info.setBorder(Theme.padding(17, 20, 17, 20));
        info.setLayout(new BorderLayout(14, 0));
        JLabel icon = UiComponents.label("AI", 12, Theme.SUCCESS, Font.BOLD);
        info.add(icon, BorderLayout.WEST);
        JPanel text = UiComponents.transparentPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UiComponents.label("Adaptive scheduling is active", 13, new Color(62, 104, 73), Font.BOLD));
        text.add(UiComponents.label("If a task is missed, StudyFlow finds a balanced future day without overloading your daily goal.",
                11, new Color(91, 125, 98), Font.PLAIN));
        info.add(text, BorderLayout.CENTER);
        return info;
    }

    private JPanel createSubjectsPage() {
        JPanel page = pageCanvas();
        JPanel intro = pageToolbar("Your subjects", planner.getSubjects().size() + " active courses",
                "+  Add subject", event -> showSubjectDialog());
        page.add(intro);
        page.add(Box.createVerticalStrut(20));
        JPanel grid = UiComponents.transparentPanel();
        grid.setLayout(new GridLayout(0, 2, 16, 16));
        for (Subject subject : planner.getSubjects()) grid.add(subjectCard(subject));
        page.add(grid);
        return page;
    }

    private JPanel subjectCard(Subject subject) {
        Color color = parseColor(subject.getColorHex());
        RoundedPanel card = sectionCard();
        card.setLayout(new BorderLayout(18, 0));
        card.setPreferredSize(new Dimension(440, 190));
        RoundedPanel monogram = new RoundedPanel(tint(color, .13f), 17);
        monogram.setPreferredSize(new Dimension(64, 64));
        monogram.setLayout(new BorderLayout());
        JLabel initial = UiComponents.label(subject.getName().substring(0, 1).toUpperCase(), 25, color, Font.BOLD);
        initial.setHorizontalAlignment(SwingConstants.CENTER);
        monogram.add(initial);
        card.add(monogram, BorderLayout.WEST);

        JPanel body = UiComponents.transparentPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(UiComponents.label(subject.getCode(), 10, color, Font.BOLD));
        body.add(Box.createVerticalStrut(3));
        body.add(UiComponents.label(subject.getName(), 16, Theme.TEXT, Font.BOLD));
        int open = (int) planner.getOpenTasks().stream().filter(task -> task.getSubjectId().equals(subject.getId())).count();
        body.add(UiComponents.label(open + " open tasks  •  " + formatDuration(subject.getWeeklyGoalMinutes()) + " weekly goal",
                10, Theme.MUTED, Font.PLAIN));
        body.add(Box.createVerticalStrut(16));
        JPanel progressHeader = UiComponents.transparentPanel();
        progressHeader.setLayout(new BorderLayout());
        progressHeader.add(UiComponents.label("Weekly progress", 10, Theme.MUTED, Font.PLAIN), BorderLayout.WEST);
        progressHeader.add(UiComponents.label(subject.getProgressPercent() + "%", 11, Theme.TEXT, Font.BOLD), BorderLayout.EAST);
        body.add(progressHeader);
        body.add(Box.createVerticalStrut(6));
        body.add(UiComponents.progressBar(subject.getProgressPercent(), color));
        body.add(Box.createVerticalStrut(14));
        RoundedButton plan = new RoundedButton("Plan a session", tint(color, .11f), tint(color, .18f), color, 12);
        plan.setAlignmentX(Component.LEFT_ALIGNMENT);
        plan.addActionListener(event -> showTaskDialog(subject));
        body.add(plan);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTasksPage() {
        JPanel page = pageCanvas();
        JPanel toolbar = UiComponents.transparentPanel();
        toolbar.setLayout(new BorderLayout());
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label("Task manager", 21, Theme.TEXT, Font.BOLD));
        copy.add(UiComponents.label(planner.getOpenTasks().size() + " tasks still need attention", 12, Theme.MUTED, Font.PLAIN));
        toolbar.add(copy, BorderLayout.WEST);
        JPanel controls = UiComponents.transparentPanel();
        controls.setLayout(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        JComboBox<String> filters = new JComboBox<>(new String[]{"All", "Today", "Upcoming", "Completed", "Overdue"});
        filters.setSelectedItem(taskFilter);
        styleInput(filters);
        filters.addActionListener(event -> {
            taskFilter = String.valueOf(filters.getSelectedItem());
            showPage(Page.TASKS);
        });
        RoundedButton add = primaryButton("+  Add task");
        add.addActionListener(event -> showTaskDialog());
        controls.add(filters); controls.add(add);
        toolbar.add(controls, BorderLayout.EAST);
        page.add(toolbar);
        page.add(Box.createVerticalStrut(18));

        List<StudyTask> tasks = filteredTasks();
        RoundedPanel list = sectionCard();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JPanel listHeader = UiComponents.transparentPanel();
        listHeader.setLayout(new BorderLayout());
        listHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        listHeader.add(UiComponents.label(taskQuery.isEmpty() ? taskFilter + " tasks" : "Search results for “" + taskQuery + "”",
                14, Theme.TEXT, Font.BOLD), BorderLayout.WEST);
        listHeader.add(UiComponents.label(tasks.size() + " results", 11, Theme.MUTED, Font.PLAIN), BorderLayout.EAST);
        list.add(listHeader);
        list.add(Box.createVerticalStrut(13));
        if (tasks.isEmpty()) {
            list.add(emptyState("No matching tasks", "Try another filter or create a new study task."));
        } else {
            for (StudyTask task : tasks) {
                list.add(taskRow(task, false));
                list.add(Box.createVerticalStrut(10));
            }
        }
        page.add(list);
        return page;
    }

    private List<StudyTask> filteredTasks() {
        LocalDate today = LocalDate.now();
        return planner.getTasks().stream().filter(task -> {
            switch (taskFilter) {
                case "Today": return task.getDueDate().equals(today);
                case "Upcoming": return task.getStatus() == TaskStatus.SCHEDULED && !task.getDueDate().isBefore(today);
                case "Completed": return task.getStatus() == TaskStatus.COMPLETED;
                case "Overdue": return task.isOverdue(today);
                default: return true;
            }
        }).filter(task -> {
            if (taskQuery.isEmpty()) return true;
            String query = taskQuery.toLowerCase(Locale.ENGLISH);
            Subject subject = planner.getSubject(task.getSubjectId());
            return task.getTitle().toLowerCase(Locale.ENGLISH).contains(query)
                    || subject.getName().toLowerCase(Locale.ENGLISH).contains(query)
                    || subject.getCode().toLowerCase(Locale.ENGLISH).contains(query);
        }).collect(Collectors.toList());
    }

    private JPanel taskRow(StudyTask task, boolean compact) {
        Subject subject = planner.getSubject(task.getSubjectId());
        Color color = parseColor(subject.getColorHex());
        RoundedPanel row = new RoundedPanel(new Color(250, 248, 244), Theme.BORDER, 14);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(Theme.padding(11, 13, 11, 13));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, compact ? 72 : 80));

        JButton check = new JButton(task.getStatus() == TaskStatus.COMPLETED ? "OK" : "");
        check.setFont(Theme.medium(8));
        check.setForeground(Color.WHITE);
        check.setBackground(task.getStatus() == TaskStatus.COMPLETED ? Theme.SUCCESS : Color.WHITE);
        check.setPreferredSize(new Dimension(28, 28));
        check.setFocusPainted(false);
        check.setBorder(BorderFactory.createLineBorder(task.getStatus() == TaskStatus.COMPLETED ? Theme.SUCCESS : Theme.BORDER, 1, true));
        check.setToolTipText(task.getStatus() == TaskStatus.COMPLETED ? "Mark as open" : "Mark complete");
        check.addActionListener(event -> {
            boolean wasCompleted = task.getStatus() == TaskStatus.COMPLETED;
            planner.toggleCompleted(task.getId());
            toast(wasCompleted ? "Task reopened." : "Task completed — nice work!", Theme.SUCCESS);
            showPage(activePage);
        });
        JPanel checkWrap = UiComponents.transparentPanel();
        checkWrap.setLayout(new GridBagLayout());
        checkWrap.add(check);
        row.add(checkWrap, BorderLayout.WEST);

        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = UiComponents.label(task.getTitle(), 13,
                task.getStatus() == TaskStatus.COMPLETED ? Theme.MUTED : Theme.TEXT, Font.BOLD);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        String meta = subject.getCode() + "  •  " + relativeDate(task.getDueDate()) + " at "
                + task.getStartTime().format(TIME) + "  •  " + task.getDurationMinutes() + " min";
        copy.add(UiComponents.label(meta, 10, Theme.MUTED, Font.PLAIN));
        row.add(copy, BorderLayout.CENTER);

        JPanel actions = UiComponents.transparentPanel();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 7, 3));
        actions.add(pill(task.getType().toString(), color));
        if (task.isOverdue(LocalDate.now()) && task.isAdaptive()) {
            RoundedButton adapt = new RoundedButton("Adapt", Theme.WARNING_SOFT, new Color(241, 225, 207), Theme.WARNING, 11);
            adapt.addActionListener(event -> {
                LocalDate newDate = planner.adaptTask(task.getId());
                toast("Rescheduled to " + newDate.format(LONG_DATE) + ".", Theme.PRIMARY);
                showPage(activePage);
            });
            actions.add(adapt);
        }
        if (!compact) {
            RoundedButton delete = new RoundedButton("Delete", Theme.DANGER_SOFT, new Color(238, 216, 211), Theme.DANGER, 11);
            delete.addActionListener(event -> deleteTask(task));
            actions.add(delete);
        }
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JPanel createProgressPage() {
        JPanel page = pageCanvas();
        page.add(pageToolbar("Learning analytics", "A clear view of your study performance",
                "View schedule", event -> showPage(Page.SCHEDULE)));
        page.add(Box.createVerticalStrut(20));
        page.add(createMetricRow());
        page.add(Box.createVerticalStrut(20));

        JPanel split = UiComponents.transparentPanel();
        split.setLayout(new GridLayout(1, 2, 18, 0));
        RoundedPanel chartCard = sectionCard();
        chartCard.setLayout(new BorderLayout());
        chartCard.add(sectionHeader("Weekly focus", "Completed study minutes", null, null), BorderLayout.NORTH);
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        chartCard.add(new WeeklyChart(planner.getWeeklyMinutes(monday)), BorderLayout.CENTER);
        split.add(chartCard);

        RoundedPanel performance = sectionCard();
        performance.setLayout(new BoxLayout(performance, BoxLayout.Y_AXIS));
        performance.add(sectionHeader("Subject performance", "Progress against weekly goals", null, null));
        performance.add(Box.createVerticalStrut(17));
        List<Subject> sorted = new ArrayList<>(planner.getSubjects());
        sorted.sort(Comparator.comparingInt(Subject::getProgressPercent).reversed());
        for (Subject subject : sorted) {
            Color color = parseColor(subject.getColorHex());
            JPanel subjectLine = UiComponents.transparentPanel();
            subjectLine.setLayout(new BorderLayout());
            subjectLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            subjectLine.add(UiComponents.label(subject.getName(), 11, Theme.TEXT, Font.BOLD), BorderLayout.WEST);
            subjectLine.add(UiComponents.label(formatDuration(subject.getStudiedMinutes()) + " / "
                    + formatDuration(subject.getWeeklyGoalMinutes()), 10, Theme.MUTED, Font.PLAIN), BorderLayout.EAST);
            performance.add(subjectLine);
            performance.add(Box.createVerticalStrut(6));
            performance.add(UiComponents.progressBar(subject.getProgressPercent(), color));
            performance.add(Box.createVerticalStrut(15));
        }
        split.add(performance);
        page.add(split);
        page.add(Box.createVerticalStrut(18));
        page.add(insightCard());
        return page;
    }

    private JPanel insightCard() {
        RoundedPanel card = new RoundedPanel(new Color(244, 236, 223), 18);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(Theme.padding(18, 20, 18, 20));
        JLabel icon = UiComponents.label("i", 20, Theme.WARNING, Font.BOLD);
        card.add(icon, BorderLayout.WEST);
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        Subject focus = planner.getSubjects().stream().min(Comparator.comparingInt(Subject::getProgressPercent)).orElse(null);
        String message = focus == null ? "Add a subject to receive personalized progress insights."
                : focus.getName() + " is behind its target. A focused 45-minute session will improve balance.";
        copy.add(UiComponents.label("Planner insight", 13, new Color(111, 76, 49), Font.BOLD));
        copy.add(UiComponents.label(message, 11, new Color(126, 95, 69), Font.PLAIN));
        card.add(copy, BorderLayout.CENTER);
        if (focus != null) {
            RoundedButton action = new RoundedButton("Plan 45 min", Color.WHITE, new Color(235, 223, 207),
                    new Color(111, 76, 49), 12);
            action.addActionListener(event -> showTaskDialog(focus));
            card.add(action, BorderLayout.EAST);
        }
        return card;
    }

    private JPanel createProfilePage() {
        JPanel page = pageCanvas();
        StudentProfile profile = planner.getProfile();
        page.add(pageToolbar("Profile & preferences", "Used to personalize your adaptive study plan", null, null));
        page.add(Box.createVerticalStrut(20));

        RoundedPanel card = sectionCard();
        card.setLayout(new BorderLayout(28, 0));
        JPanel identity = UiComponents.transparentPanel();
        identity.setPreferredSize(new Dimension(230, 330));
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        Avatar largeAvatar = new Avatar(profile.getInitials(), 88, Theme.PRIMARY);
        largeAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(Box.createVerticalStrut(8));
        identity.add(largeAvatar);
        identity.add(Box.createVerticalStrut(15));
        JLabel name = UiComponents.label(profile.getName(), 18, Theme.TEXT, Font.BOLD);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(name);
        JLabel program = UiComponents.label(profile.getProgram(), 11, Theme.MUTED, Font.PLAIN);
        program.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(program);
        identity.add(Box.createVerticalStrut(22));
        RoundedPanel status = new RoundedPanel(Theme.SUCCESS_SOFT, 13);
        status.setBorder(Theme.padding(10, 13, 10, 13));
        status.setMaximumSize(new Dimension(205, 52));
        status.setLayout(new BorderLayout(8, 0));
        status.add(UiComponents.label("●", 12, Theme.SUCCESS, Font.PLAIN), BorderLayout.WEST);
        status.add(UiComponents.label("Adaptive planning active", 10, new Color(62, 104, 73), Font.BOLD), BorderLayout.CENTER);
        identity.add(status);
        card.add(identity, BorderLayout.WEST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JTextField nameField = input(profile.getName());
        JTextField emailField = input(profile.getEmail());
        JTextField programField = input(profile.getProgram());
        JSpinner goalField = new JSpinner(new SpinnerNumberModel(profile.getDailyGoalMinutes(), 30, 720, 15));
        styleInput(goalField);
        JComboBox<String> timeField = new JComboBox<>(new String[]{
                "Morning (6 AM - 10 AM)", "Afternoon (12 PM - 4 PM)",
                "Evening (5 PM - 9 PM)", "Late evening (8 PM - 11 PM)"});
        timeField.setSelectedItem(profile.getPreferredStudyTime());
        styleInput(timeField);
        addFormRow(form, 0, "Full name", nameField);
        addFormRow(form, 1, "Email address", emailField);
        addFormRow(form, 2, "Program / course", programField);
        addFormRow(form, 3, "Daily goal (minutes)", goalField);
        addFormRow(form, 4, "Preferred study time", timeField);
        RoundedButton save = primaryButton("Save changes");
        save.addActionListener(event -> {
            if (nameField.getText().trim().isEmpty()) {
                showError("Please enter your name.");
                return;
            }
            planner.updateProfile(nameField.getText().trim(), emailField.getText().trim(),
                    programField.getText().trim(), (Integer) goalField.getValue(),
                    String.valueOf(timeField.getSelectedItem()));
            updateProfileChrome();
            toast("Profile preferences saved.", Theme.SUCCESS);
            showPage(Page.PROFILE);
        });
        GridBagConstraints saveConstraints = new GridBagConstraints();
        saveConstraints.gridx = 1; saveConstraints.gridy = 5; saveConstraints.anchor = GridBagConstraints.EAST;
        saveConstraints.insets = new Insets(14, 10, 0, 0);
        form.add(save, saveConstraints);
        JPanel formArea = UiComponents.transparentPanel();
        formArea.setLayout(new BorderLayout(26, 0));
        formArea.add(new JSeparator(SwingConstants.VERTICAL), BorderLayout.WEST);
        formArea.add(form, BorderLayout.CENTER);
        card.add(formArea, BorderLayout.CENTER);
        page.add(card);
        return page;
    }

    private JPanel pageToolbar(String title, String subtitle, String actionText,
                               java.awt.event.ActionListener listener) {
        JPanel toolbar = UiComponents.transparentPanel();
        toolbar.setLayout(new BorderLayout());
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label(title, 21, Theme.TEXT, Font.BOLD));
        copy.add(UiComponents.label(subtitle, 12, Theme.MUTED, Font.PLAIN));
        toolbar.add(copy, BorderLayout.WEST);
        if (actionText != null) {
            RoundedButton action = primaryButton(actionText);
            action.addActionListener(listener);
            toolbar.add(action, BorderLayout.EAST);
        }
        return toolbar;
    }

    private JPanel sectionHeader(String title, String subtitle, String action,
                                 java.awt.event.ActionListener listener) {
        JPanel header = UiComponents.transparentPanel();
        header.setLayout(new BorderLayout());
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label(title, 15, Theme.TEXT, Font.BOLD));
        copy.add(UiComponents.label(subtitle, 10, Theme.MUTED, Font.PLAIN));
        header.add(copy, BorderLayout.WEST);
        if (action != null) {
            JButton link = new JButton(action + "  >");
            link.setFont(Theme.medium(11));
            link.setForeground(Theme.PRIMARY);
            link.setBorderPainted(false);
            link.setContentAreaFilled(false);
            link.setFocusPainted(false);
            link.addActionListener(listener);
            Theme.clickable(link);
            header.add(link, BorderLayout.EAST);
        }
        return header;
    }

    private RoundedPanel sectionCard() {
        RoundedPanel card = new RoundedPanel(Theme.SURFACE, Theme.BORDER, 18);
        card.setBorder(UiComponents.cardPadding());
        return card;
    }

    private JPanel emptyState(String title, String message) {
        JPanel panel = UiComponents.transparentPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(Theme.padding(24, 10, 24, 10));
        JLabel icon = UiComponents.label("o", 24, new Color(160, 151, 141), Font.PLAIN);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel heading = UiComponents.label(title, 13, Theme.TEXT, Font.BOLD);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel copy = UiComponents.label(message, 10, Theme.MUTED, Font.PLAIN);
        copy.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(icon); panel.add(Box.createVerticalStrut(7)); panel.add(heading);
        panel.add(Box.createVerticalStrut(3)); panel.add(copy);
        return panel;
    }

    private void showTaskDialog() { showTaskDialog(null); }

    private void showTaskDialog(Subject selectedSubject) {
        if (planner.getSubjects().isEmpty()) {
            showError("Add a subject before creating a study task.");
            return;
        }
        JDialog dialog = createDialog("Plan a study task", 560, 560);
        JPanel form = dialogForm();
        JTextField title = input("");
        JComboBox<Subject> subject = new JComboBox<>(planner.getSubjects().toArray(new Subject[0]));
        if (selectedSubject != null) subject.setSelectedItem(selectedSubject);
        JComboBox<TaskType> type = new JComboBox<>(TaskType.values());
        JTextField date = input(LocalDate.now().toString());
        JTextField time = input("18:00");
        JSpinner duration = new JSpinner(new SpinnerNumberModel(60, 15, 360, 15));
        JComboBox<TaskPriority> priority = new JComboBox<>(TaskPriority.values());
        priority.setSelectedItem(TaskPriority.MEDIUM);
        JCheckBox adaptive = new JCheckBox("Allow intelligent rescheduling", true);
        adaptive.setOpaque(false);
        adaptive.setFont(Theme.regular(12));
        adaptive.setForeground(Theme.TEXT);
        styleInput(subject); styleInput(type); styleInput(duration); styleInput(priority);
        addDialogRow(form, 0, "Task title", title);
        addDialogRow(form, 1, "Subject", subject);
        addDialogRow(form, 2, "Task type", type);
        addDialogRow(form, 3, "Date (YYYY-MM-DD)", date);
        addDialogRow(form, 4, "Start time (HH:MM)", time);
        addDialogRow(form, 5, "Duration (minutes)", duration);
        addDialogRow(form, 6, "Priority", priority);
        GridBagConstraints check = dialogConstraints(1, 7);
        check.insets = new Insets(8, 8, 8, 8);
        form.add(adaptive, check);

        JPanel actions = dialogActions();
        RoundedButton cancel = softButton("Cancel");
        RoundedButton save = primaryButton("Add to plan");
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            try {
                if (title.getText().trim().isEmpty()) throw new IllegalArgumentException("Enter a task title.");
                LocalDate dueDate = LocalDate.parse(date.getText().trim());
                LocalTime startTime = LocalTime.parse(time.getText().trim());
                StudyTask task = TaskFactory.create((TaskType) type.getSelectedItem(), title.getText().trim(),
                        ((Subject) subject.getSelectedItem()).getId(), dueDate, startTime,
                        (Integer) duration.getValue(), (TaskPriority) priority.getSelectedItem(), adaptive.isSelected());
                planner.addTask(task);
                dialog.dispose();
                toast("Study task added to your plan.", Theme.SUCCESS);
                showPage(activePage);
            } catch (DateTimeParseException exception) {
                showError("Use YYYY-MM-DD for the date and HH:MM for the time.");
            } catch (IllegalArgumentException exception) {
                showError(exception.getMessage());
            }
        });
        actions.add(cancel); actions.add(save);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showSubjectDialog() {
        JDialog dialog = createDialog("Add a subject", 520, 420);
        JPanel form = dialogForm();
        JTextField name = input("");
        JTextField code = input("");
        JSpinner goal = new JSpinner(new SpinnerNumberModel(240, 30, 1200, 30));
        JComboBox<String> color = new JComboBox<>(new String[]{"Brown", "Tan", "Sage", "Charcoal", "Olive"});
        styleInput(goal); styleInput(color);
        addDialogRow(form, 0, "Subject name", name);
        addDialogRow(form, 1, "Short code", code);
        addDialogRow(form, 2, "Weekly goal (minutes)", goal);
        addDialogRow(form, 3, "Accent color", color);
        JPanel actions = dialogActions();
        RoundedButton cancel = softButton("Cancel");
        RoundedButton save = primaryButton("Add subject");
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            if (name.getText().trim().isEmpty() || code.getText().trim().isEmpty()) {
                showError("Enter both a subject name and short code.");
                return;
            }
            String[] colors = {"#7A5C43", "#B8875C", "#5F846C", "#55534E", "#829B72"};
            planner.addSubject(new Subject(name.getText().trim(), code.getText().trim().toUpperCase(Locale.ENGLISH),
                    colors[color.getSelectedIndex()], (Integer) goal.getValue()));
            dialog.dispose();
            toast("Subject added successfully.", Theme.SUCCESS);
            showPage(activePage);
        });
        actions.add(cancel); actions.add(save);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showReminders() {
        List<StudyTask> reminders = planner.getUpcomingTasks(3);
        StringBuilder message = new StringBuilder();
        if (reminders.isEmpty()) {
            message.append("No urgent reminders. Your next three days are clear.");
        } else {
            for (StudyTask task : reminders) {
                message.append("• ").append(task.getTitle()).append(" — ")
                        .append(relativeDate(task.getDueDate())).append(" at ")
                        .append(task.getStartTime().format(TIME)).append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, message.toString(), "Upcoming reminders", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteTask(StudyTask task) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete “" + task.getTitle() + "”?", "Delete task",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            planner.deleteTask(task.getId());
            toast("Task deleted.", Theme.DANGER);
            showPage(activePage);
        }
    }

    private JDialog createDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setMinimumSize(new Dimension(width, height));
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Theme.SURFACE);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Theme.SURFACE);
        heading.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                Theme.padding(20, 24, 18, 24)));
        JPanel copy = UiComponents.transparentPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UiComponents.label("STUDYFLOW", 9, Theme.PRIMARY, Font.BOLD));
        copy.add(UiComponents.label(title, 20, Theme.TEXT, Font.BOLD));
        heading.add(copy, BorderLayout.WEST);
        dialog.add(heading, BorderLayout.NORTH);
        return dialog;
    }

    private JPanel dialogForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.SURFACE);
        form.setBorder(Theme.padding(16, 24, 10, 24));
        return form;
    }

    private JPanel dialogActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        actions.setBackground(new Color(248, 245, 239));
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        return actions;
    }

    private void addDialogRow(JPanel form, int row, String label, JComponent input) {
        GridBagConstraints labelConstraints = dialogConstraints(0, row);
        labelConstraints.weightx = 0;
        labelConstraints.insets = new Insets(7, 0, 7, 12);
        form.add(UiComponents.label(label, 11, Theme.MUTED, Font.BOLD), labelConstraints);
        GridBagConstraints inputConstraints = dialogConstraints(1, row);
        inputConstraints.insets = new Insets(5, 0, 5, 0);
        form.add(input, inputConstraints);
    }

    private GridBagConstraints dialogConstraints(int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x; constraints.gridy = y;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = x == 1 ? 1 : 0;
        return constraints;
    }

    private void addFormRow(JPanel form, int row, String label, JComponent input) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(8, 0, 8, 18);
        form.add(UiComponents.label(label, 11, Theme.MUTED, Font.BOLD), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.fill = GridBagConstraints.HORIZONTAL;
        right.weightx = 1; right.insets = new Insets(6, 0, 6, 0);
        form.add(input, right);
    }

    private JTextField input(String value) {
        JTextField field = new JTextField(value, 26);
        styleInput(field);
        return field;
    }

    private void styleInput(JComponent input) {
        input.setFont(Theme.regular(12));
        input.setForeground(Theme.TEXT);
        input.setBackground(Color.WHITE);
        input.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                Theme.padding(8, 10, 8, 10)));
        input.setPreferredSize(new Dimension(Math.max(160, input.getPreferredSize().width), 38));
    }

    private JLabel pill(String text, Color color) {
        JLabel label = UiComponents.label(text, 9, color, Font.BOLD);
        label.setOpaque(true);
        label.setBackground(tint(color, .12f));
        label.setBorder(Theme.padding(5, 9, 5, 9));
        return label;
    }

    private RoundedButton primaryButton(String text) {
        return new RoundedButton(text, Theme.PRIMARY, Theme.PRIMARY_DARK, Color.WHITE, 13);
    }

    private RoundedButton softButton(String text) {
        return new RoundedButton(text, Theme.PRIMARY_SOFT, new Color(221, 206, 187), Theme.PRIMARY_DARK, 12);
    }

    private void updateProfileChrome() {
        StudentProfile profile = planner.getProfile();
        profileName.setText(profile.getName());
        profileProgram.setText(ellipsize(profile.getProgram(), 20));
        sidebarAvatar.setInitials(profile.getInitials());
        headerAvatar.setInitials(profile.getInitials());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Please check the details", JOptionPane.ERROR_MESSAGE);
    }

    private void toast(String message, Color accent) {
        JDialog toast = new JDialog(this, false);
        toast.setUndecorated(true);
        RoundedPanel panel = new RoundedPanel(Theme.TEXT, 15);
        panel.setBorder(Theme.padding(13, 16, 13, 16));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 9, 0));
        panel.add(UiComponents.label("●", 12, accent, Font.BOLD));
        panel.add(UiComponents.label(message, 12, Color.WHITE, Font.BOLD));
        toast.setContentPane(panel);
        toast.pack();
        int x = getX() + getWidth() - toast.getWidth() - 28;
        int y = getY() + getHeight() - toast.getHeight() - 42;
        toast.setLocation(x, y);
        toast.setVisible(true);
        Timer timer = new Timer(2400, event -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private static String firstName(String name) {
        if (name == null || name.trim().isEmpty()) return "Student";
        return name.trim().split("\\s+")[0];
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + "m";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return remainder == 0 ? hours + "h" : hours + "h " + remainder + "m";
    }

    private static String relativeDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "Today";
        if (date.equals(today.plusDays(1))) return "Tomorrow";
        if (date.equals(today.minusDays(1))) return "Yesterday";
        return date.format(SHORT_DATE);
    }

    private static Color priorityColor(TaskPriority priority) {
        if (priority == TaskPriority.HIGH) return Theme.DANGER;
        if (priority == TaskPriority.MEDIUM) return Theme.WARNING;
        return Theme.SUCCESS;
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

    private static String ellipsize(String text, int max) {
        return text.length() <= max ? text : text.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static final class GradientPanel extends JPanel {
        GradientPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(140, 106, 80), getWidth(), getHeight(), new Color(69, 51, 41)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(getWidth() - 180, -90, 260, 260);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class ResponsivePage extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ResponsivePage() {
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int outer = 9;
            int top = 7;
            int bottom = Math.max(top + 80, height - 7);
            int center = width / 2;

            g2.setColor(Theme.BACKGROUND);
            g2.fillRect(0, 0, width, height);

            // Soft the open book slightly above the beige desk surface.
            g2.setColor(Theme.BOOK_SHADOW);
            g2.fillRoundRect(outer + 3, top + 7, Math.max(20, width - outer * 2 - 2),
                    Math.max(20, bottom - top), 30, 30);

            if (width < 520) {
                g2.setColor(Theme.BOOK_PAPER);
                g2.fillRoundRect(outer, top, Math.max(20, width - outer * 2),
                        Math.max(20, bottom - top), 26, 26);
                g2.setColor(Theme.BOOK_EDGE);
                g2.drawRoundRect(outer, top, Math.max(20, width - outer * 2),
                        Math.max(20, bottom - top), 26, 26);
            } else {
                Path2D leftPage = new Path2D.Double();
                leftPage.moveTo(center, top + 13);
                leftPage.curveTo(center - 18, top + 5, center - 34, top + 3, center - 55, top + 3);
                leftPage.lineTo(outer + 22, top + 3);
                leftPage.curveTo(outer + 8, top + 3, outer + 3, top + 14, outer + 3, top + 30);
                leftPage.lineTo(outer + 3, bottom - 24);
                leftPage.curveTo(outer + 8, bottom - 8, outer + 22, bottom - 4, outer + 42, bottom - 4);
                leftPage.lineTo(center - 46, bottom - 4);
                leftPage.curveTo(center - 27, bottom - 4, center - 11, bottom + 1, center, bottom + 8);
                leftPage.closePath();

                Path2D rightPage = new Path2D.Double();
                rightPage.moveTo(center, top + 13);
                rightPage.curveTo(center + 18, top + 5, center + 34, top + 3, center + 55, top + 3);
                rightPage.lineTo(width - outer - 22, top + 3);
                rightPage.curveTo(width - outer - 8, top + 3, width - outer - 3, top + 14,
                        width - outer - 3, top + 30);
                rightPage.lineTo(width - outer - 3, bottom - 24);
                rightPage.curveTo(width - outer - 8, bottom - 8, width - outer - 22, bottom - 4,
                        width - outer - 42, bottom - 4);
                rightPage.lineTo(center + 46, bottom - 4);
                rightPage.curveTo(center + 27, bottom - 4, center + 11, bottom + 1, center, bottom + 8);
                rightPage.closePath();

                g2.setPaint(new GradientPaint(outer, 0, new Color(247, 239, 226),
                        center, 0, Theme.BOOK_PAPER));
                g2.fill(leftPage);
                g2.setPaint(new GradientPaint(center, 0, Theme.BOOK_PAPER,
                        width - outer, 0, new Color(247, 239, 226)));
                g2.fill(rightPage);

                g2.setColor(Theme.BOOK_EDGE);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(leftPage);
                g2.draw(rightPage);

                // Soft center crease gives the pages their open-book depth.
                g2.setPaint(new GradientPaint(center - 13, 0, new Color(83, 59, 42, 0),
                        center, 0, new Color(83, 59, 42, 32)));
                g2.fillRect(center - 13, top + 8, 13, Math.max(20, bottom - top - 7));
                g2.setPaint(new GradientPaint(center, 0, new Color(83, 59, 42, 32),
                        center + 13, 0, new Color(83, 59, 42, 0)));
                g2.fillRect(center, top + 8, 13, Math.max(20, bottom - top - 7));

                // Fine page layers at the lower edge keep the illustration subtle.
                g2.setColor(new Color(188, 169, 145, 90));
                g2.drawLine(outer + 25, bottom - 1, center - 49, bottom - 1);
                g2.drawLine(center + 49, bottom - 1, width - outer - 25, bottom - 1);

                Path2D bookmark = new Path2D.Double();
                bookmark.moveTo(center + 18, top + 1);
                bookmark.lineTo(center + 31, top + 1);
                bookmark.lineTo(center + 31, top + 28);
                bookmark.lineTo(center + 24.5, top + 22);
                bookmark.lineTo(center + 18, top + 28);
                bookmark.closePath();
                g2.setColor(new Color(82, 124, 93, 145));
                g2.fill(bookmark);
            }

            g2.dispose();
            super.paintComponent(graphics);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(80, visibleRect.height - 60);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class PlaceholderTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || isFocusOwner()) return;
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setFont(getFont());
            g2.setColor(new Color(145, 136, 126));
            Insets insets = getInsets();
            int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(placeholder, insets.left, y);
            g2.dispose();
        }
    }

    private static final class WeeklyChart extends JComponent {
        private final Map<LocalDate, Integer> values;
        WeeklyChart(Map<LocalDate, Integer> values) {
            this.values = values;
            setPreferredSize(new Dimension(420, 260));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 30, right = 18, top = 35, bottom = 40;
            int chartWidth = getWidth() - left - right;
            int chartHeight = getHeight() - top - bottom;
            int max = Math.max(120, values.values().stream().mapToInt(Integer::intValue).max().orElse(0));
            g2.setFont(Theme.regular(9));
            g2.setColor(new Color(226, 219, 209));
            g2.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 3; i++) {
                int y = top + chartHeight * i / 3;
                g2.drawLine(left, y, left + chartWidth, y);
            }
            int count = Math.max(1, values.size());
            int cell = chartWidth / count;
            int barWidth = Math.min(28, cell / 2);
            int index = 0;
            for (Map.Entry<LocalDate, Integer> entry : values.entrySet()) {
                int height = (int) (chartHeight * (entry.getValue() / (double) max));
                int x = left + index * cell + (cell - barWidth) / 2;
                int y = top + chartHeight - height;
                g2.setColor(entry.getKey().equals(LocalDate.now()) ? Theme.PRIMARY : new Color(194, 179, 160));
                g2.fillRoundRect(x, y, barWidth, Math.max(4, height), 9, 9);
                g2.setColor(Theme.MUTED);
                String day = entry.getKey().getDayOfWeek().toString().substring(0, 3);
                int dayWidth = g2.getFontMetrics().stringWidth(day);
                g2.drawString(day, x + (barWidth - dayWidth) / 2, getHeight() - 14);
                if (entry.getValue() > 0) {
                    String minutes = entry.getValue() + "m";
                    int labelWidth = g2.getFontMetrics().stringWidth(minutes);
                    g2.setColor(Theme.TEXT);
                    g2.drawString(minutes, x + (barWidth - labelWidth) / 2, Math.max(14, y - 6));
                }
                index++;
            }
            g2.dispose();
        }
    }
}
