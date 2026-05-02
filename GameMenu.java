import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class GameMenu extends JFrame {

    Image bg;
    Image bgNew;
    Image logoImg;

    CardLayout cardLayout;

    JPanel mainPanel;
    JPanel instructionsPanel;
    JPanel highScorePanel;
    JPanel settingsPanel;

    public GameMenu() {

        setTitle("KikiK");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        bg = new ImageIcon("background.jpg").getImage();
        bgNew = new ImageIcon("background.jpg").getImage();
        logoImg = new ImageIcon("logo.jpg").getImage();

        cardLayout = new CardLayout();
        setLayout(cardLayout);

        mainPanel = createMainMenu();
        instructionsPanel = createInstructions();
        highScorePanel = createHighScore();
        settingsPanel = createSettings();

        add(mainPanel, "main");
        add(instructionsPanel, "instructions");
        add(highScorePanel, "score");
        add(settingsPanel, "settings");

        cardLayout.show(getContentPane(), "main");
    }

   
    private JPanel createDarkPanel() {

        return new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

   
    private JPanel createNewBgPanel() {

        return new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bgNew, 0, 0, getWidth(), getHeight(), null);
                g.setColor(new Color(0, 0, 0, 60));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    // ================= LOGO PANEL =================
    private JPanel createLogoPanel() {

        int logoW = 420;
        int logoH = 140;

        BufferedImage scaled = new BufferedImage(logoW, logoH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        sg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        sg.drawImage(logoImg, 0, 0, logoW, logoH, null);
        sg.dispose();

        for (int y = 0; y < logoH; y++) {
            for (int x = 0; x < logoW; x++) {
                int rgba = scaled.getRGB(x, y);
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8)  & 0xFF;
                int b =  rgba        & 0xFF;
                int brightness = (r + g + b) / 3;

                if (brightness < 30) {
                    scaled.setRGB(x, y, 0x00000000);
                } else if (brightness < 80) {
                    int alpha = (int) ((brightness - 30) / 50.0 * 255);
                    scaled.setRGB(x, y, (alpha << 24) | (rgba & 0x00FFFFFF));
                }
            }
        }

        final BufferedImage finalLogo = scaled;

        JPanel logoPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                g.drawImage(finalLogo, 0, 0, getWidth(), getHeight(), null);
            }
        };

        logoPanel.setOpaque(false);
        logoPanel.setBounds(20, 15, logoW, logoH);
        return logoPanel;
    }

    // ================= MAIN MENU =================
    private JPanel createMainMenu() {

        JPanel panel = createDarkPanel();
        panel.setLayout(null);

        JPanel logoPanel = createLogoPanel();

        JTextArea desc = new JTextArea(
                "SURVIVE THE DARKNESS...\n\n" +
                "Dodge the endless Obstacles.\n" +
                "Stay alive as long as possible.\n\n" +
                "The deeper you go, the harder it gets."
        );

        desc.setFont(new Font("Arial", Font.PLAIN, 16));
        desc.setForeground(Color.LIGHT_GRAY);
        desc.setOpaque(false);
        desc.setEditable(false);
        desc.setBounds(60, 170, 350, 150);

        // ================= SIDEBAR =================
        JPanel sideBar = new JPanel();
        sideBar.setLayout(null);
        sideBar.setBounds(520, 0, 280, 600);
        sideBar.setBackground(new Color(10, 10, 10, 210));

        JLabel menu = new JLabel("MENU");
        menu.setFont(new Font("Arial", Font.BOLD, 30));
        menu.setForeground(Color.RED);
        menu.setBounds(100, 60, 200, 40);

        JButton start    = createButton("START GAME",    40, 130);
        JButton instr    = createButton("INSTRUCTIONS",  40, 190);
        JButton score    = createButton("HIGH SCORE",    40, 250);
        JButton settings = createButton("SETTINGS",      40, 310);
        JButton exit     = createButton("EXIT",          40, 370);

        sideBar.add(menu);
        sideBar.add(start);
        sideBar.add(instr);
        sideBar.add(score);
        sideBar.add(settings);
        sideBar.add(exit);

        panel.add(logoPanel);
        panel.add(desc);
        panel.add(sideBar);

        start.addActionListener(e -> {
            dispose();

            JFrame frame = new JFrame("KikiK Game");
            KikiK game = new KikiK(frame);

            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        instr.addActionListener(e -> cardLayout.show(getContentPane(), "instructions"));
        score.addActionListener(e -> cardLayout.show(getContentPane(), "score"));
        settings.addActionListener(e -> cardLayout.show(getContentPane(), "settings"));
        exit.addActionListener(e -> System.exit(0));

        return panel;
    }

    // ================= BUTTON DESIGN =================
    private JButton createButton(String text, int x, int y) {

        JButton btn = new JButton(text);

        btn.setBounds(x, y, 200, 40);
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(Color.DARK_GRAY);
                btn.setForeground(Color.RED);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(Color.BLACK);
                btn.setForeground(Color.WHITE);
            }
        });

        return btn;
    }

    // ================= INSTRUCTIONS =================
    private JPanel createInstructions() {

        JPanel panel = createNewBgPanel();
        panel.setLayout(null);

        JLabel title = new JLabel("HOW TO PLAY");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.RED);
        title.setBounds(240, 60, 400, 50);

        JTextArea text = new JTextArea(
                "SPACE = Jump\n" +
                "R = Restart\n" +
                "P = Pause\n" +
                "ESC = Menu\n\n" +
                "Avoid Obstacles and survive.\n" +
                "The longer you stay alive,\n" +
                "the harder it becomes."
        );

        text.setFont(new Font("Arial", Font.PLAIN, 18));
        text.setForeground(Color.WHITE);
        text.setOpaque(false);
        text.setEditable(false);
        text.setBounds(200, 150, 400, 200);

        JButton back = createButton("BACK", 300, 400);
        back.addActionListener(e -> cardLayout.show(getContentPane(), "main"));

        panel.add(title);
        panel.add(text);
        panel.add(back);

        return panel;
    }

    // ================= HIGH SCORE =================
    private JPanel createHighScore() {

        JPanel panel = createNewBgPanel();
        panel.setLayout(null);

        JLabel title = new JLabel("BLOOD SCORE");
        title.setFont(new Font("Arial", Font.BOLD, 35));
        title.setForeground(Color.RED);
        title.setBounds(170, 80, 500, 50);

        JLabel score = new JLabel(String.valueOf(GameData.highScore));
        score.setFont(new Font("Arial", Font.BOLD, 80));
        score.setForeground(Color.WHITE);
        score.setBounds(360, 220, 200, 100);

        JButton back = createButton("BACK", 300, 400);
        back.addActionListener(e -> cardLayout.show(getContentPane(), "main"));

        panel.add(title);
        panel.add(score);
        panel.add(back);

        return panel;
    }

    // ================= SETTINGS =================
    private JPanel createSettings() {

        JPanel panel = createNewBgPanel();
        panel.setLayout(null);

        JLabel title = new JLabel("SETTINGS");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.RED);
        title.setBounds(290, 60, 300, 50);

        JLabel comingSoon = new JLabel("More options coming soon...");
        comingSoon.setFont(new Font("Arial", Font.ITALIC, 16));
        comingSoon.setForeground(Color.LIGHT_GRAY);
        comingSoon.setBounds(230, 200, 350, 30);

        JButton back = createButton("BACK", 300, 400);
        back.addActionListener(e -> cardLayout.show(getContentPane(), "main"));

        panel.add(title);
        panel.add(comingSoon);
        panel.add(back);

        return panel;
    }

    public static void main(String[] args) {
        new GameMenu().setVisible(true);
    }
}