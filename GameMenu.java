import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class GameMenu extends JFrame {

    // ── Assets ──────────────────────────────────────────────────────────────
    Image bg;
    Image bgNew;
    Image logoImg;

    // ── Layout ──────────────────────────────────────────────────────────────
    CardLayout cardLayout;
    JPanel mainPanel;
    JPanel instructionsPanel;
    JPanel highScorePanel;
    JPanel settingsPanel;

    // ── Music ────────────────────────────────────────────────────────────────
    Clip bgMusicClip;
    float musicVolume = 0.7f;
    boolean musicEnabled = true;

    // ── Palette ──────────────────────────────────────────────────────────────
    static final Color BLOOD        = new Color(180,  0,   0);
    static final Color BLOOD_BRIGHT = new Color(255,  40,  40);
    static final Color EMBER        = new Color(255, 100,  20);
    static final Color PANEL_BG     = new Color(  6,   4,   4, 230);
    static final Color TEXT_DIM     = new Color(140, 110, 110);
    static final Color TEXT_GHOST   = new Color( 60,  40,  40);

    static final Random RNG = new Random();

    // =========================================================================
    public GameMenu() {
        setTitle("KikiK");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        bg      = new ImageIcon("background.jpg").getImage();
        bgNew   = new ImageIcon("background.jpg").getImage();
        logoImg = new ImageIcon("logo.jpg").getImage();

        cardLayout = new CardLayout();
        setLayout(cardLayout);

        mainPanel         = createMainMenu();
        instructionsPanel = createInstructions();
        highScorePanel    = createHighScore();
        settingsPanel     = createSettings();

        add(mainPanel,         "main");
        add(instructionsPanel, "instructions");
        add(highScorePanel,    "score");
        add(settingsPanel,     "settings");

        cardLayout.show(getContentPane(), "main");
        startBackgroundMusic("music.wav");
    }

    // =========================================================================
    //  MUSIC
    // =========================================================================
    private void startBackgroundMusic(String filename) {
        try {
            File musicFile = new File(filename);
            if (!musicFile.exists()) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(musicFile);
            bgMusicClip = AudioSystem.getClip();
            bgMusicClip.open(ais);
            bgMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            setMusicVolume(musicVolume);
            bgMusicClip.start();
        } catch (Exception e) {
            System.out.println("Music not loaded: " + e.getMessage());
        }
    }

    private void setMusicVolume(float vol) {
        if (bgMusicClip == null) return;
        FloatControl fc = (FloatControl) bgMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
        float min = fc.getMinimum(), max = fc.getMaximum();
        fc.setValue(min + (max - min) * vol);
    }

    private void toggleMusic() {
        if (bgMusicClip == null) return;
        musicEnabled = !musicEnabled;
        if (musicEnabled) bgMusicClip.start(); else bgMusicClip.stop();
    }

    // =========================================================================
    //  MAIN MENU
    // =========================================================================
    private JPanel createMainMenu() {
        AtmospherePanel panel = new AtmospherePanel(bg);
        panel.setLayout(null);

        // ── Left: logo + flavour text ────────────────────────────────────────
        JPanel logoPanel = createLogoPanel();
        panel.add(logoPanel);

        // Horizontal blood-drip line below logo
        DripBar drip = new DripBar();
        drip.setBounds(30, 165, 440, 18);
        panel.add(drip);

        GlitchLabel desc = new GlitchLabel(
            "<html>" +
            "<span style='font-family:monospace;font-size:12px;color:#cc4444'>SURVIVE THE DARKNESS</span><br><br>" +
            "<span style='font-family:monospace;font-size:11px;color:#886666'>Dodge the endless Obstacles.<br>" +
            "Stay alive as long as possible.<br>" +
            "The deeper you go — the harder it gets.</span>" +
            "</html>"
        );
        desc.setBounds(40, 185, 400, 140);
        panel.add(desc);

        // Decorative hex-grid texture block (bottom-left corner)
        HexGrid hex = new HexGrid();
        hex.setBounds(0, 420, 500, 180);
        panel.add(hex);

        // ── Divider + sidebar ────────────────────────────────────────────────
        BloodDivider divider = new BloodDivider();
        divider.setBounds(490, 0, 12, 600);
        panel.add(divider);

        SidebarPanel sidebar = new SidebarPanel();
        sidebar.setBounds(502, 0, 298, 600);
        panel.add(sidebar);

        // Sidebar header
        JLabel menuLabel = makeLabel("≡  MENU", loadFont(22f), BLOOD_BRIGHT);
        menuLabel.setBounds(30, 40, 240, 36);
        sidebar.add(menuLabel);

        // Thin accent underline
        AccentLine accentLine = new AccentLine();
        accentLine.setBounds(30, 78, 180, 2);
        sidebar.add(accentLine);

        // Buttons
        JButton start    = createButton("▶   START GAME",   28, 110, true);
        JButton instr    = createButton("?   INSTRUCTIONS", 28, 168, false);
        JButton score    = createButton("★   HIGH SCORE",   28, 220, false);
        JButton settings = createButton("⚙   SETTINGS",     28, 272, false);
        JButton mute     = createButton("♪   MUSIC: ON",    28, 330, false);
        JButton exit     = createButton("✕   EXIT",         28, 395, false);

        sidebar.add(start);
        sidebar.add(instr);
        sidebar.add(score);
        sidebar.add(settings);
        sidebar.add(mute);
        sidebar.add(exit);

        // Version watermark
        JLabel version = makeLabel("v1.0  ·  KikiK  ·  2025",
                                   new Font("Monospaced", Font.PLAIN, 9),
                                   TEXT_GHOST);
        version.setBounds(50, 572, 220, 16);
        sidebar.add(version);

        // Listeners
        start.addActionListener(e -> {
            dispose();
            if (bgMusicClip != null) bgMusicClip.stop();
            JFrame frame = new JFrame("KikiK Game");
            KikiK game = new KikiK(frame);
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
        instr.addActionListener(e -> cardLayout.show(getContentPane(), "instructions"));
        score.addActionListener(e -> { refreshHighScore(); cardLayout.show(getContentPane(), "score"); });
        settings.addActionListener(e -> cardLayout.show(getContentPane(), "settings"));
        mute.addActionListener(e -> {
            toggleMusic();
            mute.setText(musicEnabled ? "♪   MUSIC: ON" : "♪   MUSIC: OFF");
        });
        exit.addActionListener(e -> System.exit(0));

        return panel;
    }

    // =========================================================================
    //  INSTRUCTIONS
    // =========================================================================
    private JPanel createInstructions() {
        AtmospherePanel panel = new AtmospherePanel(bgNew);
        panel.setLayout(null);

        // Big header + underline
        JLabel title = makeLabel("HOW TO PLAY", loadFont(38f), BLOOD_BRIGHT);
        title.setBounds(200, 44, 420, 52);
        panel.add(title);

        AccentLine titleLine = new AccentLine();
        titleLine.setBounds(200, 98, 370, 3);
        panel.add(titleLine);

        // Key cards — now with a glow border on hover
        String[][] keys = {
            {"SPACE", "Jump"}, {"R", "Restart"}, {"P", "Pause"}, {"ESC", "Menu"}
        };
        int kx = 70;
        for (String[] k : keys) {
            KeyCard card = new KeyCard(k[0], k[1]);
            card.setBounds(kx, 135, 140, 90);
            panel.add(card);
            kx += 162;
        }

        // Flavour text
        String[] lines = {
            "Avoid all obstacles and survive as long as possible.",
            "Speed increases the longer you stay alive.",
            "Only the strongest survive the darkness."
        };
        int ty = 262;
        for (String line : lines) {
            JLabel l = makeLabel("›  " + line,
                    new Font("Monospaced", Font.PLAIN, 13),
                    new Color(180, 140, 140));
            l.setBounds(100, ty, 620, 22);
            panel.add(l);
            ty += 26;
        }

        // Decorative skull divider
        SkullDivider skull = new SkullDivider();
        skull.setBounds(100, 350, 600, 30);
        panel.add(skull);

        JButton back = createButton("◀   BACK TO MENU", 275, 430, false);
        back.addActionListener(e -> cardLayout.show(getContentPane(), "main"));
        panel.add(back);

        return panel;
    }

    // =========================================================================
    //  HIGH SCORE
    // =========================================================================
    JLabel scoreValueLabel;

    private void refreshHighScore() {
        if (scoreValueLabel != null)
            scoreValueLabel.setText(String.valueOf(GameData.highScore));
    }

    private JPanel createHighScore() {
        AtmospherePanel panel = new AtmospherePanel(bgNew);
        panel.setLayout(null);

        JLabel title = makeLabel("BLOOD SCORE", loadFont(38f), BLOOD_BRIGHT);
        title.setBounds(195, 44, 430, 52);
        panel.add(title);

        AccentLine titleLine = new AccentLine();
        titleLine.setBounds(195, 98, 380, 3);
        panel.add(titleLine);

        // Score box with pulsing outer glow
        BloodScoreBox scoreBox = new BloodScoreBox(String.valueOf(GameData.highScore));
        scoreBox.setBounds(180, 140, 440, 190);
        panel.add(scoreBox);
        scoreValueLabel = scoreBox.valueLabel;

        JLabel sub = makeLabel("— PERSONAL BEST —",
                new Font("Monospaced", Font.BOLD, 13),
                TEXT_DIM);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        sub.setBounds(150, 345, 500, 22);
        panel.add(sub);

        SkullDivider skull = new SkullDivider();
        skull.setBounds(100, 378, 600, 30);
        panel.add(skull);

        JButton back = createButton("◀   BACK TO MENU", 275, 430, false);
        back.addActionListener(e -> cardLayout.show(getContentPane(), "main"));
        panel.add(back);

        return panel;
    }

    // =========================================================================
    //  SETTINGS
    // =========================================================================
    private JPanel createSettings() {
        AtmospherePanel panel = new AtmospherePanel(bgNew);
        panel.setLayout(null);

        JLabel title = makeLabel("SETTINGS", loadFont(38f), BLOOD_BRIGHT);
        title.setBounds(260, 44, 320, 52);
        panel.add(title);

        AccentLine titleLine = new AccentLine();
        titleLine.setBounds(260, 98, 260, 3);
        panel.add(titleLine);

        // Volume section
        JLabel volLabel = makeLabel("MUSIC VOLUME",
                new Font("Monospaced", Font.BOLD, 13), TEXT_DIM);
        volLabel.setBounds(190, 148, 240, 24);
        panel.add(volLabel);

        BloodSlider volSlider = new BloodSlider(0, 100, (int)(musicVolume * 100));
        volSlider.setBounds(190, 180, 420, 38);
        volSlider.addChangeListener(e -> {
            musicVolume = volSlider.getValue() / 100f;
            setMusicVolume(musicVolume);
        });
        panel.add(volSlider);

        // Toggle button
        JButton toggleBtn = createButton(musicEnabled ? "♪   MUSIC: ON" : "♪   MUSIC: OFF",
                280, 248, false);
        toggleBtn.addActionListener(e -> {
            toggleMusic();
            toggleBtn.setText(musicEnabled ? "♪   MUSIC: ON" : "♪   MUSIC: OFF");
        });
        panel.add(toggleBtn);

        JLabel comingSoon = makeLabel("· · · more options coming soon · · ·",
                new Font("Monospaced", Font.ITALIC, 11),
                TEXT_GHOST);
        comingSoon.setHorizontalAlignment(SwingConstants.CENTER);
        comingSoon.setBounds(150, 325, 500, 22);
        panel.add(comingSoon);

        SkullDivider skull = new SkullDivider();
        skull.setBounds(100, 365, 600, 30);
        panel.add(skull);

        JButton back = createButton("◀   BACK TO MENU", 275, 430, false);
        back.addActionListener(e -> cardLayout.show(getContentPane(), "main"));
        panel.add(back);

        return panel;
    }

    // =========================================================================
    //  LOGO
    // =========================================================================
    private JPanel createLogoPanel() {
        int logoW = 430, logoH = 145;
        BufferedImage scaled = new BufferedImage(logoW, logoH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        sg.drawImage(logoImg, 0, 0, logoW, logoH, null);
        sg.dispose();

        // Remove near-black pixels, feather edge
        for (int y = 0; y < logoH; y++) {
            for (int x = 0; x < logoW; x++) {
                int rgba  = scaled.getRGB(x, y);
                int r = (rgba >> 16) & 0xFF, g2 = (rgba >> 8) & 0xFF, b = rgba & 0xFF;
                int bright = (r + g2 + b) / 3;
                if (bright < 30) scaled.setRGB(x, y, 0x00000000);
                else if (bright < 80) {
                    int alpha = (int)((bright - 30) / 50.0 * 255);
                    scaled.setRGB(x, y, (alpha << 24) | (rgba & 0x00FFFFFF));
                }
            }
        }

        final BufferedImage finalLogo = scaled;
        JPanel p = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // subtle red drop shadow
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.setColor(BLOOD);
                g2.drawImage(finalLogo, 4, 6, getWidth(), getHeight(), null);
                g2.setComposite(AlphaComposite.SrcOver);
                g2.drawImage(finalLogo, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBounds(22, 12, logoW, logoH);
        return p;
    }

    // =========================================================================
    //  BUTTON
    // =========================================================================
    private JButton createButton(String text, int x, int y, boolean primary) {
        JButton btn = new JButton(text) {
            boolean hover = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hover = false; repaint(); }
            }); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (primary && hover) {
                    // Full blood fill for primary hover
                    GradientPaint gp = new GradientPaint(0, 0, new Color(200, 10, 10),
                                                          getWidth(), 0, new Color(120, 0, 0));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                } else if (hover) {
                    g2.setColor(new Color(140, 0, 0, 45));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(new Color(200, 0, 0, 140));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                } else {
                    g2.setColor(new Color(18, 8, 8, 190));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(new Color(60, 20, 20, 100));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                }

                // Left accent pip
                if (hover) {
                    g2.setColor(primary ? Color.WHITE : BLOOD_BRIGHT);
                    g2.fillRect(0, 10, 3, getHeight() - 20);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBounds(x, y, 226, 40);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setForeground(primary ? Color.WHITE : new Color(210, 180, 180));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFont(loadFont(12.5f));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(8);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.WHITE); }
            public void mouseExited (MouseEvent e) { btn.setForeground(primary ? Color.WHITE : new Color(210,180,180)); }
        });
        return btn;
    }

    // =========================================================================
    //  FONT
    // =========================================================================
    private Font loadFont(float size) {
        try {
            File f = new File("font.ttf");
            if (f.exists()) return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(Font.BOLD, size);
        } catch (Exception ignored) {}
        return new Font("Monospaced", Font.BOLD, (int) size);
    }

    private static JLabel makeLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setOpaque(false);
        return l;
    }

    // =========================================================================
    //  MAIN
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameMenu().setVisible(true));
    }

    // =========================================================================
    //  INNER COMPONENTS
    // =========================================================================

    // ── Full-screen atmospheric background ───────────────────────────────────
    static class AtmospherePanel extends JPanel {
        final Image bg;
        final java.util.List<float[]> particles = new ArrayList<>();
        final java.util.List<float[]> sparks    = new ArrayList<>();
        static final int PCOUNT = 80, SCOUNT = 20;
        float vignetteAlpha = 0f;
        float scanOffset = 0;

        AtmospherePanel(Image bg) {
            this.bg = bg;
            setLayout(null); setOpaque(true);
            for (int i = 0; i < PCOUNT; i++) spawnParticle(true);
            for (int i = 0; i < SCOUNT;  i++) spawnSpark(true);

            Timer t = new Timer(14, e -> {
                for (float[] p : particles) { p[0]+=p[2]; p[1]+=p[3]; p[4]-=0.002f+RNG.nextFloat()*0.001f; }
                for (float[] s : sparks)    { s[0]+=s[2]; s[1]+=s[3]; s[3]+=0.012f; s[4]-=0.018f; }
                particles.removeIf(p -> p[4]<=0 || p[1]<-5 || p[1]>getHeight()+5);
                sparks.removeIf   (s -> s[4]<=0 || s[1]>getHeight()+5);
                while (particles.size()<PCOUNT) spawnParticle(false);
                while (sparks.size()   <SCOUNT) spawnSpark(false);
                scanOffset = (scanOffset + 0.3f) % 6;
                repaint();
            });
            t.start();
        }

        void spawnParticle(boolean anywhere) {
            float x  = RNG.nextFloat()*800;
            float y  = anywhere ? RNG.nextFloat()*600 : 605;
            float vx = (RNG.nextFloat()-0.5f)*0.3f;
            float vy = -(0.25f + RNG.nextFloat()*0.9f);
            float a  = 0.25f + RNG.nextFloat()*0.55f;
            float sz = 1.4f  + RNG.nextFloat()*2.8f;
            particles.add(new float[]{x, y, vx, vy, a, sz});
        }

        void spawnSpark(boolean anywhere) {
            float x  = RNG.nextFloat()*800;
            float y  = anywhere ? RNG.nextFloat()*600 : 605;
            float vx = (RNG.nextFloat()-0.5f)*1.8f;
            float vy = -(0.8f + RNG.nextFloat()*2f);
            float a  = 0.7f + RNG.nextFloat()*0.3f;
            sparks.add(new float[]{x, y, vx, vy, a});
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background image
            if (bg != null) g2.drawImage(bg, 0, 0, getWidth(), getHeight(), null);

            // Deep dark overlay — stronger than before
            g2.setColor(new Color(4, 2, 2, 185));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Vignette (radial dark corners)
            RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Float(getWidth()/2f, getHeight()/2f),
                Math.max(getWidth(), getHeight()) * 0.72f,
                new float[]{0.35f, 1f},
                new Color[]{new Color(0,0,0,0), new Color(0,0,0,200)}
            );
            g2.setPaint(vignette);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Faint red centre bloom
            RadialGradientPaint bloom = new RadialGradientPaint(
                new Point2D.Float(getWidth()/2f, getHeight()/2f),
                getWidth()*0.55f,
                new float[]{0f, 1f},
                new Color[]{new Color(120,0,0,28), new Color(0,0,0,0)}
            );
            g2.setPaint(bloom);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Scanlines
            g2.setStroke(new BasicStroke(1f));
            for (int y = (int)(scanOffset); y < getHeight(); y += 4) {
                g2.setColor(new Color(0,0,0, 18 + (y%8==0 ? 12 : 0)));
                g2.drawLine(0, y, getWidth(), y);
            }

            // Ember particles
            for (float[] p : particles) {
                float a = Math.max(0, Math.min(1, p[4]));
                float r = 0.85f + RNG.nextFloat()*0.15f;
                float gf = 0.05f + RNG.nextFloat()*0.18f;
                g2.setColor(new Color(r, gf, 0f, a * 0.9f));
                float s = p[5];
                g2.fill(new Ellipse2D.Float(p[0]-s/2, p[1]-s/2, s, s));
            }

            // Bright sparks (streaks)
            g2.setStroke(new BasicStroke(1.2f));
            for (float[] s : sparks) {
                float a = Math.max(0, Math.min(1, s[4]));
                g2.setColor(new Color(1f, 0.6f, 0.1f, a));
                g2.draw(new Line2D.Float(s[0], s[1], s[0]-s[2]*3, s[1]-s[3]*1.5f));
            }
        }
    }

    // ── Sidebar with layered dark texture ────────────────────────────────────
    static class SidebarPanel extends JPanel {
        float wave = 0;
        SidebarPanel() {
            setLayout(null); setOpaque(false);
            Timer t = new Timer(16, e -> { wave += 0.018f; repaint(); });
            t.start();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Main sidebar fill — very dark, subtle noise
            g2.setColor(PANEL_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Animated diagonal hatch
            g2.setColor(new Color(255, 0, 0, 5));
            g2.setStroke(new BasicStroke(6));
            for (float x = -(float)(wave*12 % 24); x < getWidth()+getHeight(); x += 24) {
                g2.drawLine((int)x, 0, (int)(x - getHeight()), getHeight());
            }

            // Left glow border
            for (int i = 0; i < 8; i++) {
                float alpha = (8-i) / 22f;
                g2.setColor(new Color(1f, 0f, 0f, alpha));
                g2.fillRect(0, 0, i+1, getHeight());
            }

            // Top glow
            GradientPaint top = new GradientPaint(0,0,new Color(180,0,0,40),0,80,new Color(0,0,0,0));
            g2.setPaint(top);
            g2.fillRect(0, 0, getWidth(), 80);

            // Bottom glow
            GradientPaint bottom = new GradientPaint(0,getHeight()-60,new Color(0,0,0,0),0,getHeight(),new Color(180,0,0,30));
            g2.setPaint(bottom);
            g2.fillRect(0, getHeight()-60, getWidth(), 60);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Blood divider (vertical separator) ───────────────────────────────────
    static class BloodDivider extends JPanel {
        float phase = 0;
        BloodDivider() {
            setOpaque(false);
            Timer t = new Timer(30, e -> { phase += 0.04f; repaint(); });
            t.start();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int pulse = (int)(90 + 60 * Math.sin(phase));
            // Core line
            g2.setColor(new Color(180, 0, 0, pulse));
            g2.fillRect(4, 0, 3, getHeight());
            // Glow halos
            for (int i = 1; i <= 6; i++) {
                float a = (pulse/255f) * (1f - i/7f) * 0.5f;
                g2.setColor(new Color(1f, 0f, 0f, a));
                g2.fillRect(4-i, 0, 3+i*2, getHeight());
            }
            // Random drip drops
            if (RNG.nextInt(30) == 0) {
                int dy = RNG.nextInt(getHeight());
                g2.setColor(new Color(140, 0, 0, 180));
                g2.fillOval(2, dy, 7, 5 + RNG.nextInt(12));
            }
            g2.dispose();
        }
    }

    // ── Drip bar (horizontal, below logo) ────────────────────────────────────
    static class DripBar extends JPanel {
        int[] dripY;
        float[] dripSpeed;
        DripBar() {
            setOpaque(false);
            dripY     = new int [12];
            dripSpeed = new float[12];
            for (int i = 0; i < 12; i++) { dripY[i] = RNG.nextInt(18); dripSpeed[i]=0.3f+RNG.nextFloat()*0.7f; }
            Timer t = new Timer(60, e -> {
                for (int i=0;i<12;i++) { dripY[i]+=(int)(dripSpeed[i]+0.5f); if(dripY[i]>getHeight()+20) dripY[i]=-6; }
                repaint();
            });
            t.start();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Horizontal blood line
            g2.setColor(new Color(160, 0, 0, 200));
            g2.fillRect(0, 0, getWidth(), 3);
            // Glow above line
            GradientPaint gp = new GradientPaint(0,0,new Color(200,0,0,80),0,3,new Color(0,0,0,0));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), 3);
            // Drip droplets
            int step = getWidth() / 12;
            for (int i = 0; i < 12; i++) {
                int x = i * step + step/2;
                if (dripY[i] > 0) {
                    g2.setColor(new Color(140, 0, 0, 200));
                    g2.fillRect(x-1, 3, 3, dripY[i]);
                    g2.setColor(new Color(180, 10, 10, 220));
                    g2.fillOval(x-4, 3+dripY[i]-4, 9, 9);
                }
            }
            g2.dispose();
        }
    }

    // ── Hex grid texture ──────────────────────────────────────────────────────
    static class HexGrid extends JPanel {
        HexGrid() { setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(0.6f));
            int R = 18;
            double h = R * Math.sqrt(3);
            for (int row = 0; row * h < getHeight() + h; row++) {
                for (int col = 0; col * 1.5 * R < getWidth() + R; col++) {
                    double cx = col * 1.5 * R + R;
                    double cy = row * h + (col % 2 == 0 ? 0 : h/2);
                    int alpha = (int)(8 + 10 * Math.sin(row * 0.4 + col * 0.3));
                    g2.setColor(new Color(180, 0, 0, Math.max(0,Math.min(255,alpha))));
                    drawHex(g2, (int)cx, (int)cy, R);
                }
            }
            // Fade top half
            GradientPaint fade = new GradientPaint(0, 0, new Color(0,0,0,200), 0, getHeight(), new Color(0,0,0,0));
            g2.setPaint(fade);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
        static void drawHex(Graphics2D g, int cx, int cy, int r) {
            int[] xs = new int[6], ys = new int[6];
            for (int i = 0; i < 6; i++) {
                xs[i] = (int)(cx + r * Math.cos(Math.toRadians(60*i + 30)));
                ys[i] = (int)(cy + r * Math.sin(Math.toRadians(60*i + 30)));
            }
            g.drawPolygon(xs, ys, 6);
        }
    }

    // ── Accent underline ──────────────────────────────────────────────────────
    static class AccentLine extends JPanel {
        AccentLine() { setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0,0, BLOOD_BRIGHT, getWidth(),0, new Color(80,0,0,0));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ── Skull divider ─────────────────────────────────────────────────────────
    static class SkullDivider extends JPanel {
        SkullDivider() { setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int mid = getWidth()/2, my = getHeight()/2;
            // Lines
            GradientPaint left  = new GradientPaint(0, my, new Color(0,0,0,0), mid-22, my, new Color(140,0,0,180));
            GradientPaint right = new GradientPaint(mid+22, my, new Color(140,0,0,180), getWidth(), my, new Color(0,0,0,0));
            g2.setStroke(new BasicStroke(1f));
            g2.setPaint(left);  g2.drawLine(20, my, mid-22, my);
            g2.setPaint(right); g2.drawLine(mid+22, my, getWidth()-20, my);
            // Diamond
            g2.setColor(BLOOD);
            int[] dx = {mid, mid+8, mid, mid-8};
            int[] dy = {my-8, my, my+8, my};
            g2.fillPolygon(dx, dy, 4);
            g2.setColor(new Color(255,80,80));
            g2.drawPolygon(dx, dy, 4);
            g2.dispose();
        }
    }

    // ── Blood score box ───────────────────────────────────────────────────────
    static class BloodScoreBox extends JPanel {
        JLabel valueLabel;
        float phase = 0;
        BloodScoreBox(String value) {
            setOpaque(false);
            setLayout(null);

            valueLabel = new JLabel(value, SwingConstants.CENTER) {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    // Glow layers
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    int tx = (getWidth()-fm.stringWidth(getText()))/2;
                    int ty = (getHeight()+fm.getAscent())/2-8;
                    for (int i = 8; i >= 1; i--) {
                        float a = (1f - i/9f) * 0.4f;
                        g2.setColor(new Color(1f,0f,0f,a));
                        g2.setFont(getFont());
                        g2.drawString(getText(), tx-i, ty-i);
                        g2.drawString(getText(), tx+i, ty+i);
                        g2.drawString(getText(), tx-i, ty+i);
                        g2.drawString(getText(), tx+i, ty-i);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            try {
                File f = new File("font.ttf");
                Font base = f.exists() ? Font.createFont(Font.TRUETYPE_FONT, f) : new Font("Monospaced", Font.BOLD, 1);
                valueLabel.setFont(base.deriveFont(Font.BOLD, 88f));
            } catch(Exception ex) {
                valueLabel.setFont(new Font("Monospaced", Font.BOLD, 88));
            }
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setBounds(0, 20, 440, 140);
            add(valueLabel);

            Timer t = new Timer(30, e -> { phase += 0.06f; repaint(); });
            t.start();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pulse = (int)(55 + 35*Math.sin(phase));

            // Box border
            g2.setColor(new Color(40,10,10,200));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(new Color(160,0,0, pulse));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);

            // Corner ticks
            g2.setColor(BLOOD_BRIGHT);
            g2.setStroke(new BasicStroke(2f));
            int s = 16;
            g2.drawLine(0,0, s,0); g2.drawLine(0,0, 0,s);
            g2.drawLine(getWidth()-s,0, getWidth(),0); g2.drawLine(getWidth(),0, getWidth(),s);
            g2.drawLine(0,getHeight()-s, 0,getHeight()); g2.drawLine(0,getHeight(), s,getHeight());
            g2.drawLine(getWidth()-s,getHeight(), getWidth(),getHeight()); g2.drawLine(getWidth(),getHeight()-s, getWidth(),getHeight());

            // Outer pulse glow
            for (int i = 1; i <= 4; i++) {
                float a = (pulse/255f) * (1f-i/5f) * 0.25f;
                g2.setColor(new Color(1f,0f,0f,a));
                g2.setStroke(new BasicStroke(i*1.5f));
                g2.drawRoundRect(-i, -i, getWidth()+i*2, getHeight()+i*2, 12+i*2, 12+i*2);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Custom blood-red slider ───────────────────────────────────────────────
    static class BloodSlider extends JSlider {
        BloodSlider(int min, int max, int val) {
            super(min, max, val);
            setOpaque(false);
            setUI(new javax.swing.plaf.basic.BasicSliderUI(this) {
                public void paintTrack(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g.create();
                    Rectangle t = trackRect;
                    int ty = t.y + t.height/2 - 2;
                    g2.setColor(new Color(40,10,10,220));
                    g2.fillRoundRect(t.x, ty, t.width, 5, 4, 4);
                    int filled = (int)((slider.getValue()-slider.getMinimum())/(double)(slider.getMaximum()-slider.getMinimum())*t.width);
                    GradientPaint gp = new GradientPaint(t.x,0, new Color(180,0,0), t.x+filled,0, BLOOD_BRIGHT);
                    g2.setPaint(gp);
                    g2.fillRoundRect(t.x, ty, filled, 5, 4, 4);
                    g2.dispose();
                }
                public void paintThumb(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = thumbRect.x + thumbRect.width/2;
                    int cy = thumbRect.y + thumbRect.height/2;
                    g2.setColor(new Color(20,5,5,220));
                    g2.fillOval(cx-9, cy-9, 18, 18);
                    g2.setColor(BLOOD_BRIGHT);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(cx-9, cy-9, 18, 18);
                    g2.setColor(new Color(255,120,120));
                    g2.fillOval(cx-4, cy-4, 8, 8);
                    g2.dispose();
                }
            });
        }
    }

    // ── Glitch label ──────────────────────────────────────────────────────────
    static class GlitchLabel extends JLabel {
        int counter = 0, gx = 0, gy = 0;
        GlitchLabel(String html) {
            super(html);
            setOpaque(false);
            setVerticalAlignment(SwingConstants.TOP);
            Timer t = new Timer(75, e -> {
                counter++;
                if (counter % 18 == 0) { gx = RNG.nextInt(5)-2; gy = RNG.nextInt(3)-1; }
                else { gx = gy = 0; }
                repaint();
            });
            t.start();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.translate(gx, gy);
            if (gx != 0) {
                g2.setColor(new Color(255,0,0,45));
                g2.translate(3,0);
                super.paintComponent(g2);
                g2.translate(-3,0);
            }
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // ── Key-bind card ─────────────────────────────────────────────────────────
    static class KeyCard extends JPanel {
        String key, action;
        boolean hover = false;
        float phase = RNG.nextFloat() * 6.28f;

        KeyCard(String key, String action) {
            this.key = key; this.action = action;
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hover = false; repaint(); }
            });
            Timer t = new Timer(30, e -> { phase += 0.04f; if(hover) repaint(); });
            t.start();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg   = hover ? new Color(40,10,10,220) : new Color(15,5,5,200);
            Color bord = hover ? BLOOD_BRIGHT : new Color(80,30,30,160);

            g2.setColor(bg);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
            g2.setColor(bord);
            g2.setStroke(new BasicStroke(hover ? 1.8f : 1f));
            g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,10,10);

            if (hover) {
                // Pulsing outer glow
                int pulse = (int)(60+40*Math.sin(phase));
                g2.setColor(new Color(200,0,0,pulse));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(-1,-1,getWidth()+2,getHeight()+2,12,12);
            }

            // Key name
            g2.setFont(new Font("Monospaced", Font.BOLD, 21));
            g2.setColor(hover ? BLOOD_BRIGHT : Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(key, (getWidth()-fm.stringWidth(key))/2, 34);

            // Action label
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.setColor(hover ? new Color(220,160,160) : TEXT_DIM);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(action, (getWidth()-fm2.stringWidth(action))/2, 62);

            g2.dispose();
        }
    }
}