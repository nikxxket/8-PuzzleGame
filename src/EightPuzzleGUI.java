import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

// Панель для отображения пазла
class PuzzlePanel extends JPanel {
    private int[][] board;
    private int tileSize;
    private final int margin = 2;

    public PuzzlePanel(int[][] board) {
        this.board = copyBoard(board);
        setBackground(new Color(40, 40, 40));
        setPreferredSize(new Dimension(500, 500));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        int panelSize = Math.min(width, height);
        tileSize = (panelSize - 2 * margin) / 3;
    }

    public void setBoard(int[][] newBoard) {
        this.board = copyBoard(newBoard);
        repaint();
    }

    private int[][] copyBoard(int[][] original) {
        int[][] copy = new int[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, 3);
        }
        return copy;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int panelSize = Math.min(panelWidth, panelHeight);
        tileSize = (panelSize - 2 * margin) / 3;

        int offsetX = (panelWidth - (tileSize * 3 + 2 * margin)) / 2;
        int offsetY = (panelHeight - (tileSize * 3 + 2 * margin)) / 2;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int x = offsetX + j * tileSize + margin;
                int y = offsetY + i * tileSize + margin;
                int width = tileSize - 2 * margin;
                int height = tileSize - 2 * margin;

                if (board[i][j] == 0) {
                    // Пустая клетка
                    g2d.setColor(new Color(60, 60, 60));
                    g2d.fillRect(x, y, width, height);
                    g2d.setColor(new Color(100, 100, 100));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRect(x, y, width, height);
                } else {
                    // Клетка с числом - яркий цвет
                    GradientPaint gradient = new GradientPaint(x, y, new Color(100, 150, 200),
                            x + width, y + height, new Color(50, 100, 150));
                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(x, y, width, height, 15, 15);

                    g2d.setColor(Color.WHITE);
                    int fontSize = Math.max(20, Math.min(40, tileSize / 3));
                    g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
                    String text = String.valueOf(board[i][j]);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textX = x + (width - fm.stringWidth(text)) / 2;
                    int textY = y + (height - fm.getHeight()) / 2 + fm.getAscent();
                    g2d.drawString(text, textX, textY);

                    // Яркая обводка
                    g2d.setColor(new Color(255, 215, 0));
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRoundRect(x, y, width, height, 15, 15);

                    // Внутренняя обводка
                    g2d.setColor(new Color(255, 255, 255, 100));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRoundRect(x + 2, y + 2, width - 4, height - 4, 13, 13);
                }
            }
        }
    }
}

// Главное окно приложения
public class EightPuzzleGUI extends JFrame {
    private PuzzlePanel puzzlePanel;
    private JButton solveButton;
    private JButton shuffleButton;
    private JButton resetButton;
    private JButton stopButton;
    private JLabel infoLabel;
    private JTextArea solutionArea;
    private int[][] currentBoard;
    private List<PuzzleState> solutionPath;
    private Timer animationTimer;
    private int currentStep;
    private boolean isAnimating;

    public EightPuzzleGUI() {
        initializeUI();
        setupEventHandlers();
        shufflePuzzle();
    }

    private void initializeUI() {
        setTitle("8-Puzzle Game - Пятнашки");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Центральная панель для пазла
        currentBoard = new int[3][3];
        puzzlePanel = new PuzzlePanel(currentBoard);
        add(puzzlePanel, BorderLayout.CENTER);

        // Нижняя панель с кнопками
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        bottomPanel.setBackground(new Color(50, 50, 50));

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(50, 50, 50));

        solveButton = createStyledButton("Решить", new Color(100, 150, 200), new Color(70, 130, 180));
        shuffleButton = createStyledButton("Перемешать", new Color(150, 150, 150), new Color(120, 120, 120));
        resetButton = createStyledButton("Сброс", new Color(200, 120, 120), new Color(180, 100, 100));
        stopButton = createStyledButton("Стоп", new Color(220, 120, 120), new Color(200, 100, 100));
        stopButton.setEnabled(false);

        buttonPanel.add(solveButton);
        buttonPanel.add(shuffleButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(stopButton);

        // Информационная панель
        infoLabel = new JLabel("Нажмите 'Перемешать' чтобы начать", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setBackground(new Color(70, 70, 70));
        infoLabel.setOpaque(true);

        // Область решения
        solutionArea = new JTextArea(5, 40);
        solutionArea.setEditable(false);
        solutionArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        solutionArea.setBackground(new Color(60, 60, 60));
        solutionArea.setForeground(new Color(220, 220, 220));
        solutionArea.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 200), 1));

        JScrollPane scrollPane = new JScrollPane(solutionArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 200), 2),
                "Путь решения",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                new Color(100, 150, 200)
        ));
        scrollPane.setPreferredSize(new Dimension(400, 120));
        scrollPane.setBackground(new Color(50, 50, 50));

        // Собираем нижнюю панель
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(50, 50, 50));
        infoPanel.add(infoLabel, BorderLayout.NORTH);
        infoPanel.add(scrollPane, BorderLayout.CENTER);

        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(infoPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(600, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(500, 600));
        getContentPane().setBackground(new Color(40, 40, 40));
    }

    private JButton createStyledButton(String text, Color gradientStart, Color gradientEnd) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Рисуем градиентный фон
                GradientPaint gradient = new GradientPaint(0, 0, gradientStart,
                        getWidth(), getHeight(), gradientEnd);
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Рисуем обводку
                g2d.setColor(new Color(255, 215, 0));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);

                // Внутренняя обводка
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 13, 13);

                // Рисуем текст
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(getText(), textX, textY);

                g2d.dispose();
            }
        };

        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 45));

        // Эффект наведения
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(255, 215, 0));
                button.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE);
                button.repaint();
            }
        });

        return button;
    }

    private void setupEventHandlers() {
        shuffleButton.addActionListener(e -> shufflePuzzle());
        solveButton.addActionListener(e -> solvePuzzle());
        resetButton.addActionListener(e -> resetPuzzle());
        stopButton.addActionListener(e -> stopAnimation());
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            isAnimating = false;
        }
        stopButton.setEnabled(false);
        solveButton.setEnabled(true);
        shuffleButton.setEnabled(true);
        infoLabel.setText("Анимация остановлена");
        infoLabel.setForeground(new Color(255, 215, 0));
    }

    private void resetPuzzle() {
        stopAnimation();
        solutionPath = null;
        currentStep = 0;
        isAnimating = false;
        solutionArea.setText("");
        infoLabel.setText("Пазл сброшен. Нажмите 'Перемешать' для новой игры");
        infoLabel.setForeground(Color.WHITE);
    }

    private void shufflePuzzle() {
        stopAnimation();
        currentBoard = PuzzleSolver.generateRandomState();
        puzzlePanel.setBoard(currentBoard);
        solutionPath = null;
        currentStep = 0;
        solutionArea.setText("");
        infoLabel.setText("Пазл перемешан! Нажмите 'Решить' для поиска решения");
        infoLabel.setForeground(Color.WHITE);
    }

    private void solvePuzzle() {
        PuzzleState start = new PuzzleState(currentBoard);
        infoLabel.setText("Идет поиск оптимального решения... Пожалуйста, подождите");
        infoLabel.setForeground(new Color(255, 215, 0));
        solveButton.setEnabled(false);
        shuffleButton.setEnabled(false);

        SwingWorker<List<PuzzleState>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<PuzzleState> doInBackground() {
                return PuzzleSolver.solveAStar(start);
            }

            @Override
            protected void done() {
                try {
                    solutionPath = get();
                    if (solutionPath.isEmpty()) {
                        infoLabel.setText("Решение не найдено! Попробуйте перемешать пазл еще раз");
                        infoLabel.setForeground(Color.RED);
                        solveButton.setEnabled(true);
                        shuffleButton.setEnabled(true);
                    } else {
                        currentStep = 0;
                        infoLabel.setText(String.format("Решение найдено! Оптимальный путь: %d шагов. Начинаю анимацию...",
                                solutionPath.size() - 1));
                        infoLabel.setForeground(new Color(100, 255, 100));
                        displaySolutionSteps();
                        startAnimation();
                    }
                } catch (Exception ex) {
                    infoLabel.setText("Ошибка при поиске решения: " + ex.getMessage());
                    infoLabel.setForeground(Color.RED);
                    ex.printStackTrace();
                    solveButton.setEnabled(true);
                    shuffleButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void startAnimation() {
        if (solutionPath == null || solutionPath.isEmpty()) {
            return;
        }

        isAnimating = true;
        stopButton.setEnabled(true);
        currentStep = 1;

        animationTimer = new Timer(800, e -> {
            if (!isAnimating) {
                return;
            }

            if (currentStep < solutionPath.size()) {
                PuzzleState state = solutionPath.get(currentStep);
                puzzlePanel.setBoard(state.getBoard());

                String action = state.getAction();
                String actionRu = "";
                switch (action) {
                    case "Up": actionRu = "Вверх"; break;
                    case "Down": actionRu = "Вниз"; break;
                    case "Left": actionRu = "Влево"; break;
                    case "Right": actionRu = "Вправо"; break;
                }
                infoLabel.setText(String.format("Шаг %d из %d: %s", currentStep, solutionPath.size() - 1, actionRu));
                infoLabel.setForeground(new Color(255, 215, 0));
                currentStep++;
            } else {
                animationTimer.stop();
                isAnimating = false;
                stopButton.setEnabled(false);
                solveButton.setEnabled(true);
                shuffleButton.setEnabled(true);
                infoLabel.setText("Поздравляем! Пазл собран!");
                infoLabel.setForeground(new Color(100, 255, 100));
            }
        });

        animationTimer.start();
    }

    private void displaySolutionSteps() {
        StringBuilder sb = new StringBuilder();
        sb.append("Оптимальный путь решения:\n");
        sb.append("------------------------------------------------\n");
        for (int i = 1; i < solutionPath.size(); i++) {
            PuzzleState state = solutionPath.get(i);
            String action = state.getAction();
            String actionRu = "";
            switch (action) {
                case "Up": actionRu = "Вверх"; break;
                case "Down": actionRu = "Вниз"; break;
                case "Left": actionRu = "Влево"; break;
                case "Right": actionRu = "Вправо"; break;
            }
            sb.append(String.format("%2d. %s\n", i, actionRu));
        }
        sb.append("------------------------------------------------\n");
        sb.append(String.format("Всего шагов: %d\n", solutionPath.size() - 1));
        solutionArea.setText(sb.toString());
        solutionArea.setCaretPosition(0);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            new EightPuzzleGUI().setVisible(true);
        });
    }
}