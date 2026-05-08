import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

/**
 * KikiK — Horror Flappy Bird  (Enhanced Edition)
 *
 * NEW FEATURES:
 *  - Lives / hearts system (upgradeable 1–3)
 *  - Blood coins floating between pipes
 *  - Power-ups: Shield, Slow-Mo, Magnet
 *  - Weather effects: Rain, Fog, Lightning
 *  - Upgrade shop accessible from the dead / start screen
 *  - Combo multiplier for consecutive gap passes
 *
 * Controls: SPACE / Click = Flap  |  P = Pause  |  R = Restart  |  ESC = Menu  |  U = Shop
 */
public class KikiK extends JPanel implements ActionListener {

    // ── Board ────────────────────────────────────────────────────────────────
    static final int W = 800, H = 600;

    // ── Bird ─────────────────────────────────────────────────────────────────
    int birdX = W / 4;
    int birdY = H / 2;
    int birdW = 72, birdH = 72;
    double velY = 0;
    double gravity = 0.45;
    double flapPower = -9.5;
    double maxFallSpeed = 12;

    int flapFrame = 0;
    int flapTick  = 0;
    static final int FLAP_FRAMES = 4;

    // ── Lives ─────────────────────────────────────────────────────────────────
    int lives = 1;          // current lives (set from upgrade)
    int maxLives = 1;       // max lives (upgradeable)
    int invincibleTimer = 0; // frames of invincibility after taking a hit
    static final int INVINCIBLE_FRAMES = 90;

    // ── Coins ─────────────────────────────────────────────────────────────────
    static class Coin {
        float x, y;
        float bobOffset;
        boolean collected = false;
        boolean magnetTarget = false;
        float collectAnim = 0; // 0=alive, >0=disappearing
        Coin(float x, float y) {
            this.x = x; this.y = y;
            this.bobOffset = (float)(Math.random() * Math.PI * 2);
        }
    }
    ArrayList<Coin> coins = new ArrayList<>();
    int sessionCoins = 0;   // coins earned this run
    int totalCoins  = 0;    // persistent bank

    // ── Power-ups ─────────────────────────────────────────────────────────────
    enum PowerType { SHIELD, SLOWMO, MAGNET }
    static class PowerUp {
        float x, y;
        PowerType type;
        float bobOffset;
        boolean collected = false;
        float collectAnim = 0;
        PowerUp(float x, float y, PowerType t) {
            this.x = x; this.y = y; this.type = t;
            this.bobOffset = (float)(Math.random() * Math.PI * 2);
        }
    }
    ArrayList<PowerUp> powerUps = new ArrayList<>();

    // Active power-up timers (frames)
    int shieldTimer  = 0;
    int slowMoTimer  = 0;
    int magnetTimer  = 0;
    static final int POWERUP_DURATION = 300; // 5 seconds at 60fps

    // Shield hit flash
    int shieldHitFlash = 0;

    // ── Combo ─────────────────────────────────────────────────────────────────
    int combo = 0;
    int comboDisplayTimer = 0;

    // ── Weather ───────────────────────────────────────────────────────────────
    enum Weather { CLEAR, RAIN, FOG, LIGHTNING }
    Weather currentWeather = Weather.CLEAR;
    int weatherTimer = 0;
    static final int WEATHER_CHANGE_INTERVAL = 600; // 10s

    // Rain drops
    static class RainDrop {
        float x, y, speed, len;
        RainDrop() { reset(); y = (float)(Math.random() * H); }
        void reset() { x = (float)(Math.random() * W); y = -20; speed = 8 + (float)(Math.random()*5); len = 10 + (float)(Math.random()*10); }
    }
    ArrayList<RainDrop> rainDrops = new ArrayList<>();

    // Lightning
    int lightningTimer = 0;
    int lightningFlash = 0;
    ArrayList<int[]> lightningBolts = new ArrayList<>(); // each bolt: x1,y1,x2,y2,...

    // Fog alpha target
    float fogAlpha = 0, fogAlphaTarget = 0;

    // ── Pipes ─────────────────────────────────────────────────────────────────
    static class Pipe {
        int x, gapY, width = 60, gapH = 230;
        boolean scored = false;
        Pipe(int x, int gapY) { this.x = x; this.gapY = gapY; }
    }
    ArrayList<Pipe> pipes = new ArrayList<>();
    Random rng = new Random();
    int pipeSpeed = 2;
    int frameCount = 0;

    // ── Particles ─────────────────────────────────────────────────────────────
    static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
        Particle(float x, float y, float vx, float vy, float life, float size, Color c) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.life=this.maxLife=life; this.size=size; this.color=c;
        }
    }
    ArrayList<Particle> particles = new ArrayList<>();

    // ── Scrolling bg ──────────────────────────────────────────────────────────
    float bgScrollX = 0;

    // ── State ─────────────────────────────────────────────────────────────────
    enum State { START, PLAYING, PAUSED, DEAD, SHOP }
    State state = State.START;
    State prevState = State.START; // for shop return

    int score = 0;
    int highScore = 0;
    static final String PREF_HS   = "kikiK_highScore";
    static final String PREF_COIN = "kikiK_totalCoins";
    static final String PREF_UPG  = "kikiK_upgrades"; // CSV: lives,flapPwr,gapW

    // ── Upgrades ──────────────────────────────────────────────────────────────
    // levels: 0,1,2 each
    int upgLives     = 0; // +1 life per level
    int upgFlapPower = 0; // -0.5 flap per level
    int upgGapWidth  = 0; // +20 gap per level
    static final int[] UPG_COST = {50, 120, 250};
    static final String[] UPG_NAMES = {"Extra Life", "Wing Strength", "Wider Gap"};

    // ── Flash / shake ─────────────────────────────────────────────────────────
    int flashAlpha = 0;
    Color flashColor = new Color(180, 0, 0);
    boolean shaking = false;
    int shakeTimer = 0;
    int shakeX = 0, shakeY = 0;

    // ── Assets ────────────────────────────────────────────────────────────────
    BufferedImage bgImg;
    BufferedImage charImg;
    BufferedImage[] flapFrameImgs;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    Font fontTitle, fontScore, fontSmall, fontUI, fontTiny;

    // ── Sound ─────────────────────────────────────────────────────────────────
    Clip bgMusic;
    float musicVol = 0.75f;

    // ── Timer ─────────────────────────────────────────────────────────────────
    Timer timer;
    JFrame parentFrame;

    // ─────────────────────────────────────────────────────────────────────────
    public KikiK(JFrame frame) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        loadPrefs();
        loadAssets();
        setupFonts();
        applyUpgrades();
        initRain();
        setupControls();
        startMusic("music.wav");

        timer = new Timer(16, this);
        timer.start();
    }

    // ── Preferences ───────────────────────────────────────────────────────────
    void loadPrefs() {
        Preferences p = Preferences.userNodeForPackage(KikiK.class);
        highScore  = p.getInt(PREF_HS, 0);
        totalCoins = p.getInt(PREF_COIN, 0);
        String upg = p.get(PREF_UPG, "0,0,0");
        String[] parts = upg.split(",");
        if (parts.length == 3) {
            upgLives     = clamp(parseInt(parts[0]), 0, 2);
            upgFlapPower = clamp(parseInt(parts[1]), 0, 2);
            upgGapWidth  = clamp(parseInt(parts[2]), 0, 2);
        }
    }
    void savePrefs() {
        Preferences p = Preferences.userNodeForPackage(KikiK.class);
        p.putInt(PREF_HS, highScore);
        p.putInt(PREF_COIN, totalCoins);
        p.put(PREF_UPG, upgLives + "," + upgFlapPower + "," + upgGapWidth);
    }
    int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e) { return 0; } }
    int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    void applyUpgrades() {
        maxLives  = 1 + upgLives;
        flapPower = -9.5 - upgFlapPower * 0.5;
    }
    int getGapWidth() { return 230 + upgGapWidth * 20; }

    // ── Asset loading ─────────────────────────────────────────────────────────
    void loadAssets() {
        bgImg  = tryLoad("background.png");
        if (bgImg == null) bgImg = tryLoad("background.jpg");
        charImg = tryLoad("kikik.jpg");
        if (charImg == null) charImg = tryLoad("character.jpg");
        if (charImg != null) {
            charImg = makeTransparentBlack(charImg);
            flapFrameImgs = buildFlapFrames(charImg, FLAP_FRAMES);
        }
    }
    BufferedImage tryLoad(String name) {
        try { return ImageIO.read(new File(name)); }
        catch (Exception e) {
            try { InputStream is = getClass().getResourceAsStream("/" + name);
                  if (is != null) return ImageIO.read(is); } catch (Exception ignored) {}
            return null;
        }
    }
    BufferedImage makeTransparentBlack(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int c = src.getRGB(x, y);
                int r=(c>>16)&0xFF, g=(c>>8)&0xFF, b=c&0xFF;
                int lum = (r+g+b)/3;
                int alpha = lum<25?0:(lum<55?(int)((lum-25)/30.0*255):255);
                out.setRGB(x, y, (alpha<<24)|(c&0x00FFFFFF));
            }
        return out;
    }
    BufferedImage[] buildFlapFrames(BufferedImage src, int count) {
        BufferedImage[] frames = new BufferedImage[count];
        int fw=birdW, fh=birdH;
        double[] squeezes = {1.0,0.85,0.70,0.85};
        for (int i=0; i<count; i++) {
            frames[i] = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[i].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int sh=(int)(fh*squeezes[i]), sy=(fh-sh)/2;
            g.drawImage(src, 0, sy, fw, sy+sh, 0, 0, src.getWidth(), src.getHeight(), null);
            g.dispose();
        }
        return frames;
    }

    // ── Font setup ────────────────────────────────────────────────────────────
    void setupFonts() {
        fontTitle = new Font("Serif",      Font.BOLD|Font.ITALIC, 52);
        fontScore = new Font("Serif",      Font.BOLD,             38);
        fontSmall = new Font("Monospaced", Font.PLAIN,            14);
        fontUI    = new Font("Serif",      Font.BOLD,             22);
        fontTiny  = new Font("Monospaced", Font.PLAIN,            11);
    }

    // ── Weather init ──────────────────────────────────────────────────────────
    void initRain() {
        rainDrops.clear();
        for (int i = 0; i < 120; i++) rainDrops.add(new RainDrop());
    }

    // ── Controls ──────────────────────────────────────────────────────────────
    void setupControls() {
        bind("flap",    KeyEvent.VK_SPACE,  e -> handleFlap());
        bind("pause",   KeyEvent.VK_P,      e -> handlePause());
        bind("restart", KeyEvent.VK_R,      e -> { if (state==State.DEAD) restart(); });
        bind("shop",    KeyEvent.VK_U,      e -> toggleShop());
        bind("menu",    KeyEvent.VK_ESCAPE, e -> {
            if (bgMusic!=null) bgMusic.stop();
            parentFrame.dispose();
            new GameMenu().setVisible(true);
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (state == State.SHOP) handleShopClick(e.getX(), e.getY());
                else handleFlap();
            }
        });
    }
    void bind(String name, int key, java.util.function.Consumer<ActionEvent> action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.accept(e); }
        });
    }

    void handleFlap() {
        if (state == State.START) {
            lives = maxLives;
            state = State.PLAYING;
            spawnPipe();
        } else if (state == State.PLAYING) {
            velY = flapPower;
            flapFrame = 0;
            playSound("flap");
            spawnFlapParticles();
        } else if (state == State.DEAD) {
            restart();
        }
    }

    void handlePause() {
        if (state == State.PLAYING) state = State.PAUSED;
        else if (state == State.PAUSED) state = State.PLAYING;
    }

    void toggleShop() {
        if (state == State.SHOP) {
            state = prevState;
        } else if (state == State.START || state == State.DEAD || state == State.PAUSED) {
            prevState = state;
            state = State.SHOP;
        }
    }

    // ── Shop click handling ───────────────────────────────────────────────────
    // Upgrade button rects (set during draw, used for click detection)
    Rectangle[] shopBtns = new Rectangle[3];

    void handleShopClick(int mx, int my) {
        if (shopBtns == null) return;
        int[] levels = {upgLives, upgFlapPower, upgGapWidth};
        for (int i = 0; i < 3; i++) {
            if (shopBtns[i] != null && shopBtns[i].contains(mx, my)) {
                int lv = levels[i];
                if (lv < 3) {
                    int cost = UPG_COST[lv];
                    if (totalCoins >= cost) {
                        totalCoins -= cost;
                        if (i == 0) upgLives++;
                        if (i == 1) upgFlapPower++;
                        if (i == 2) upgGapWidth++;
                        applyUpgrades();
                        savePrefs();
                        playSound("score");
                        spawnScoreParticles();
                    } else {
                        playSound("death"); // error beep
                    }
                }
            }
        }
        // Close button
        Rectangle closeBtn = new Rectangle(W/2+150, H/2-160, 30, 30);
        if (closeBtn.contains(mx, my)) toggleShop();
    }

    // ── Game logic ────────────────────────────────────────────────────────────
    @Override public void actionPerformed(ActionEvent e) { update(); repaint(); }

    void update() {
        frameCount++;
        bgScrollX -= 0.4f;
        if (bgImg != null && bgScrollX <= -bgImg.getWidth()) bgScrollX = 0;
        updateParticles();
        updateWeather();

        if (state == State.PLAYING) {
            // Physics
            double effectiveGravity = slowMoTimer > 0 ? gravity * 0.45 : gravity;
            double effectiveMaxFall = slowMoTimer > 0 ? maxFallSpeed * 0.45 : maxFallSpeed;
            velY = Math.min(velY + effectiveGravity, effectiveMaxFall);
            birdY += velY;

            // Wing animation
            if (++flapTick > 4) { flapTick=0; flapFrame=(flapFrame+1)%FLAP_FRAMES; }

            // Power-up timers
            if (shieldTimer  > 0) shieldTimer--;
            if (slowMoTimer  > 0) slowMoTimer--;
            if (magnetTimer  > 0) magnetTimer--;
            if (shieldHitFlash > 0) shieldHitFlash--;
            if (invincibleTimer > 0) invincibleTimer--;
            if (comboDisplayTimer > 0) comboDisplayTimer--;

            // Pipes & coins
            int effectiveSpeed = slowMoTimer > 0 ? Math.max(1, pipeSpeed/2) : pipeSpeed;
            movePipes(effectiveSpeed);
            moveCoins(effectiveSpeed);
            movePowerUps(effectiveSpeed);
            checkCollision();
            checkCoinPickup();
            checkPowerUpPickup();

            pipeSpeed = 2 + Math.min(score/15, 4);

            // Shake / flash
            if (shaking) {
                shakeTimer--;
                shakeX = (int)((rng.nextInt(9)-4)*(shakeTimer/15.0));
                shakeY = (int)((rng.nextInt(9)-4)*(shakeTimer/15.0));
                if (shakeTimer<=0) { shaking=false; shakeX=shakeY=0; }
            }
            if (flashAlpha>0) flashAlpha=Math.max(0, flashAlpha-12);
        } else if (state == State.START) {
            birdY = H/2 + (int)(Math.sin(frameCount*0.04)*15);
        }
    }

    // ── Weather ───────────────────────────────────────────────────────────────
    void updateWeather() {
        weatherTimer++;
        if (weatherTimer >= WEATHER_CHANGE_INTERVAL && state == State.PLAYING) {
            weatherTimer = 0;
            // Cycle or randomize weather based on score
            if (score < 5) {
                currentWeather = Weather.CLEAR;
            } else {
                Weather[] options = Weather.values();
                currentWeather = options[rng.nextInt(options.length)];
            }
        }

        // Rain update
        if (currentWeather == Weather.RAIN || currentWeather == Weather.LIGHTNING) {
            for (RainDrop d : rainDrops) {
                d.y += d.speed * (currentWeather == Weather.LIGHTNING ? 1.5f : 1f);
                d.x -= 1.5f;
                if (d.y > H+20) d.reset();
            }
        }

        // Fog fade
        fogAlphaTarget = currentWeather == Weather.FOG ? 0.35f : 0f;
        fogAlpha += (fogAlphaTarget - fogAlpha) * 0.02f;

        // Lightning
        if (currentWeather == Weather.LIGHTNING) {
            if (lightningTimer > 0) { lightningTimer--; lightningFlash = Math.max(0, lightningFlash-15); }
            else if (rng.nextInt(120) == 0) {
                triggerLightning();
            }
        } else {
            lightningFlash = 0;
            lightningTimer = 0;
        }
    }

    void triggerLightning() {
        lightningFlash = 180;
        lightningTimer = 12;
        lightningBolts.clear();
        // Generate a jagged bolt
        int boltX = 100 + rng.nextInt(W - 200);
        int[] bolt = new int[20];
        bolt[0] = boltX; bolt[1] = 0;
        for (int i = 2; i < 20; i += 2) {
            bolt[i]   = bolt[i-2] + (rng.nextInt(41)-20);
            bolt[i+1] = bolt[i-1] + 30;
        }
        lightningBolts.add(bolt);
        playSound("lightning");
    }

    // ── Pipes ─────────────────────────────────────────────────────────────────
    void movePipes(int speed) {
        for (Pipe p : pipes) {
            p.x -= speed;
            if (!p.scored && p.x + p.width < birdX) {
                p.scored = true;
                score++;
                combo++;
                comboDisplayTimer = 90;
                if (score > highScore) { highScore = score; savePrefs(); }
                playSound("score");
                spawnScoreParticles();
                // Spawn coins in gap
                spawnCoinsInGap(p);
                // Chance to spawn power-up
                if (rng.nextInt(5) == 0) spawnPowerUp(p);
            }
        }
        pipes.removeIf(p -> p.x + p.width < -10);

        boolean needSpawn = pipes.isEmpty();
        if (!needSpawn) {
            Pipe last = pipes.get(pipes.size()-1);
            int gap = 380 - Math.min(score*2, 80);
            needSpawn = last.x < W - gap;
        }
        if (needSpawn) spawnPipe();
    }

    void spawnPipe() {
        int gapH = getGapWidth();
        int minGap=80, maxGap=H-80-gapH;
        int gapY = minGap + rng.nextInt(Math.max(1, maxGap-minGap));
        Pipe p = new Pipe(W+20, gapY);
        p.gapH = gapH;
        pipes.add(p);
    }

    // ── Coins ─────────────────────────────────────────────────────────────────
    void spawnCoinsInGap(Pipe p) {
        int count = 1 + rng.nextInt(3);
        for (int i = 0; i < count; i++) {
            float cx = p.x + p.width + 30 + rng.nextInt(60);
            float cy = p.gapY + 20 + rng.nextInt(p.gapH - 40);
            coins.add(new Coin(cx, cy));
        }
    }

    void moveCoins(int speed) {
        for (Coin c : coins) {
            c.x -= speed;
            if (c.collectAnim > 0) c.collectAnim += 0.05f;
        }
        coins.removeIf(c -> c.x + 20 < 0 || c.collectAnim >= 1f);
    }

    void checkCoinPickup() {
        Rectangle bird = new Rectangle(birdX+10, birdY+10, birdW-20, birdH-20);
        float magnetRange = magnetTimer > 0 ? 150f : 30f;
        for (Coin c : coins) {
            if (c.collected) continue;
            float dx = c.x - (birdX + birdW/2);
            float dy = c.y - (birdY + birdH/2);
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if (magnetTimer > 0 && dist < magnetRange) {
                // Attract toward bird
                float pull = 0.15f;
                c.x -= dx * pull;
                c.y -= dy * pull;
            }
            if (dist < 36 || bird.contains((int)c.x, (int)c.y)) {
                c.collected = true;
                c.collectAnim = 0.01f;
                sessionCoins++;
                totalCoins++;
                savePrefs();
                playSound("coin");
                spawnCoinParticle(c.x, c.y);
            }
        }
    }

    // ── Power-ups ─────────────────────────────────────────────────────────────
    void spawnPowerUp(Pipe p) {
        PowerType[] types = PowerType.values();
        PowerType t = types[rng.nextInt(types.length)];
        float cx = p.x + p.width + 80 + rng.nextInt(40);
        float cy = p.gapY + p.gapH/2 + (rng.nextInt(61)-30);
        powerUps.add(new PowerUp(cx, cy, t));
    }

    void movePowerUps(int speed) {
        for (PowerUp pu : powerUps) {
            pu.x -= speed;
            if (pu.collectAnim > 0) pu.collectAnim += 0.05f;
        }
        powerUps.removeIf(pu -> pu.x + 20 < 0 || pu.collectAnim >= 1f);
    }

    void checkPowerUpPickup() {
        Rectangle bird = new Rectangle(birdX+8, birdY+8, birdW-16, birdH-16);
        for (PowerUp pu : powerUps) {
            if (pu.collected) continue;
            if (bird.contains((int)pu.x, (int)pu.y)) {
                pu.collected = true;
                pu.collectAnim = 0.01f;
                activatePowerUp(pu.type);
            }
        }
    }

    void activatePowerUp(PowerType t) {
        switch (t) {
            case SHIELD:  shieldTimer  = POWERUP_DURATION; break;
            case SLOWMO:  slowMoTimer  = POWERUP_DURATION; break;
            case MAGNET:  magnetTimer  = POWERUP_DURATION; break;
        }
        playSound("powerup");
        flashColor = getPowerUpColor(t);
        flashAlpha = 80;
    }

    Color getPowerUpColor(PowerType t) {
        switch (t) {
            case SHIELD:  return new Color(50, 200, 255);
            case SLOWMO:  return new Color(200, 100, 255);
            case MAGNET:  return new Color(255, 200, 0);
            default:      return new Color(255, 255, 255);
        }
    }

    // ── Collision ─────────────────────────────────────────────────────────────
    void checkCollision() {
        if (invincibleTimer > 0) return;
        if (birdY + birdH > H || birdY < -20) { takeHit(); return; }
        Rectangle bird = new Rectangle(birdX+18, birdY+18, birdW-36, birdH-36);
        for (Pipe p : pipes) {
            Rectangle top = new Rectangle(p.x, 0, p.width, p.gapY);
            Rectangle bot = new Rectangle(p.x, p.gapY+p.gapH, p.width, H-p.gapY-p.gapH);
            if (bird.intersects(top) || bird.intersects(bot)) { takeHit(); return; }
        }
    }

    void takeHit() {
        if (shieldTimer > 0) {
            shieldTimer = 0;
            shieldHitFlash = 30;
            invincibleTimer = INVINCIBLE_FRAMES;
            flashColor = new Color(50, 200, 255);
            flashAlpha = 120;
            shaking = true; shakeTimer = 12;
            playSound("shieldbreak");
            combo = 0;
            // push bird back to center
            velY = flapPower * 0.5;
            return;
        }
        lives--;
        combo = 0;
        if (lives <= 0) {
            die();
        } else {
            // Lose a life — brief invincibility
            invincibleTimer = INVINCIBLE_FRAMES;
            flashColor = new Color(255, 50, 50);
            flashAlpha = 160;
            shaking = true; shakeTimer = 15;
            playSound("hurt");
            velY = flapPower * 0.6;
        }
    }

    void die() {
        state = State.DEAD;
        if (score > highScore) { highScore = score; savePrefs(); }
        flashColor = new Color(180, 0, 0);
        flashAlpha = 200;
        shaking = true; shakeTimer = 20;
        playSound("death");
        spawnDeathParticles();
        // Bonus coins for high combo
        if (combo >= 5) { totalCoins += combo; sessionCoins += combo; savePrefs(); }
    }

    void restart() {
        birdY = H/2; velY = 0;
        pipes.clear();
        coins.clear();
        powerUps.clear();
        score = 0;
        sessionCoins = 0;
        combo = 0;
        pipeSpeed = 3;
        frameCount = 0;
        flashAlpha = 0;
        shaking = false;
        particles.clear();
        shieldTimer = slowMoTimer = magnetTimer = 0;
        invincibleTimer = 0;
        lives = maxLives;
        currentWeather = Weather.CLEAR;
        weatherTimer = 0;
        state = State.PLAYING;
        spawnPipe();
    }

    // ── Particles ─────────────────────────────────────────────────────────────
    void updateParticles() {
        for (Particle p : particles) {
            p.x+=p.vx; p.y+=p.vy; p.vy+=0.08f; p.life-=0.02f;
        }
        particles.removeIf(p -> p.life<=0);
    }
    void spawnFlapParticles() {
        for (int i=0; i<8; i++) {
            float vx=(rng.nextFloat()-0.5f)*3, vy=rng.nextFloat()*-2+1;
            particles.add(new Particle(birdX+birdW/2, birdY+birdH/2, vx, vy,
                0.4f+rng.nextFloat()*0.4f, 3+rng.nextFloat()*3, new Color(180,0,0,200)));
        }
    }
    void spawnScoreParticles() {
        for (int i=0; i<15; i++) {
            float angle=(float)(Math.PI*2*rng.nextFloat()), spd=2+rng.nextFloat()*3;
            Color c=new Color(255,(int)(180+rng.nextInt(75)),0,230);
            particles.add(new Particle(birdX+birdW/2, birdY+birdH/2,
                (float)Math.cos(angle)*spd, (float)Math.sin(angle)*spd-2,
                0.6f+rng.nextFloat()*0.4f, 4+rng.nextFloat()*4, c));
        }
    }
    void spawnDeathParticles() {
        for (int i=0; i<40; i++) {
            float angle=(float)(Math.PI*2*rng.nextFloat()), spd=1+rng.nextFloat()*5;
            int r=180+rng.nextInt(75), g2=rng.nextInt(40);
            particles.add(new Particle(birdX+birdW/2, birdY+birdH/2,
                (float)Math.cos(angle)*spd, (float)Math.sin(angle)*spd-1,
                0.8f+rng.nextFloat()*0.6f, 3+rng.nextFloat()*6, new Color(r,g2,0,220)));
        }
    }
    void spawnCoinParticle(float cx, float cy) {
        for (int i=0; i<6; i++) {
            float angle=(float)(Math.PI*2*rng.nextFloat()), spd=1+rng.nextFloat()*2;
            particles.add(new Particle(cx, cy, (float)Math.cos(angle)*spd, (float)Math.sin(angle)*spd-1,
                0.5f+rng.nextFloat()*0.3f, 3+rng.nextFloat()*3, new Color(255,200,50,220)));
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D)g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,   RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.translate(shakeX, shakeY);

        drawBackground(g);
        drawWeatherBG(g);
        for (Pipe p : pipes) drawPipe(g, p);
        drawCoins(g);
        drawPowerUps(g);
        drawParticles(g);
        drawBird(g);
        drawWeatherFG(g);
        drawHUD(g);

        if (state == State.START)  drawStartScreen(g);
        if (state == State.PAUSED) drawPauseScreen(g);
        if (state == State.DEAD)   drawDeadScreen(g);
        if (state == State.SHOP)   drawShopScreen(g);

        if (flashAlpha > 0) {
            g.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), flashAlpha));
            g.fillRect(-shakeX, -shakeY, W, H);
        }

        g.dispose();
    }

    // ── Background ────────────────────────────────────────────────────────────
    void drawBackground(Graphics2D g) {
        if (bgImg != null) {
            int bw=bgImg.getWidth(), bh=bgImg.getHeight();
            int drawH=H, drawW=(int)((double)bw/bh*drawH);
            int sx=(int)bgScrollX;
            g.drawImage(bgImg, sx,0, sx+drawW,drawH, 0,0,bw,bh, null);
            g.drawImage(bgImg, sx+drawW,0, sx+drawW*2,drawH, 0,0,bw,bh, null);
            if (sx+drawW < W) g.drawImage(bgImg, sx+drawW*2,0, sx+drawW*3,drawH, 0,0,bw,bh, null);
        } else {
            GradientPaint gp = new GradientPaint(0,0,new Color(5,5,20),0,H,new Color(15,5,30));
            g.setPaint(gp); g.fillRect(0,0,W,H);
        }
        RadialGradientPaint vignette = new RadialGradientPaint(new Point(W/2,H/2), Math.max(W,H)*0.7f,
            new float[]{0f,1f}, new Color[]{new Color(0,0,0,0),new Color(0,0,0,140)});
        g.setPaint(vignette); g.fillRect(0,0,W,H);
    }

    // ── Weather layers ────────────────────────────────────────────────────────
    void drawWeatherBG(Graphics2D g) {
        // Lightning flash
        if (lightningFlash > 0) {
            g.setColor(new Color(220, 220, 255, Math.min(lightningFlash, 120)));
            g.fillRect(0, 0, W, H);
            // Draw bolts
            g.setColor(new Color(240, 240, 255, 200));
            g.setStroke(new BasicStroke(2.5f));
            for (int[] bolt : lightningBolts) {
                for (int i=0; i<bolt.length-3; i+=2)
                    g.drawLine(bolt[i], bolt[i+1], bolt[i+2], bolt[i+3]);
            }
            g.setStroke(new BasicStroke(1f));
        }
        // Fog overlay
        if (fogAlpha > 0.01f) {
            g.setColor(new Color(0.5f, 0.5f, 0.6f, fogAlpha));
            g.fillRect(0, 0, W, H);
        }
    }

    void drawWeatherFG(Graphics2D g) {
        if (currentWeather == Weather.RAIN || currentWeather == Weather.LIGHTNING) {
            g.setColor(new Color(180, 200, 255, 100));
            g.setStroke(new BasicStroke(1f));
            for (RainDrop d : rainDrops)
                g.drawLine((int)d.x, (int)d.y, (int)(d.x+2), (int)(d.y+d.len));
            g.setStroke(new BasicStroke(1f));
        }
        // Weather label
        if (state == State.PLAYING && currentWeather != Weather.CLEAR) {
            g.setFont(fontTiny);
            String wLabel = "⚡ " + currentWeather.name();
            if (currentWeather == Weather.RAIN) wLabel = "🌧 RAIN";
            if (currentWeather == Weather.FOG)  wLabel = "🌫 FOG";
            g.setColor(new Color(200, 200, 255, 180));
            g.drawString(wLabel, W/2 - 30, H - 12);
        }
    }

    // ── Pipe drawing ──────────────────────────────────────────────────────────
    void drawPipe(Graphics2D g, Pipe p) {
        drawPoleSegment(g, p.x, 0, p.width, p.gapY, true);
        drawPoleSegment(g, p.x, p.gapY+p.gapH, p.width, H-p.gapY-p.gapH, false);
    }
    void drawPoleSegment(Graphics2D g, int x, int y, int w, int h, boolean isTop) {
        if (h<=0) return;
        GradientPaint woodGrad = new GradientPaint(x,0,new Color(25,18,10),x+w,0,new Color(45,32,18));
        g.setPaint(woodGrad); g.fillRect(x,y,w,h);
        g.setColor(new Color(15,10,5,100));
        for (int i=x+12; i<x+w; i+=14) g.drawLine(i,y,i,y+h);
        g.setColor(new Color(70,50,25,120)); g.fillRect(x,y,5,h);
        g.setColor(new Color(0,0,0,100));    g.fillRect(x+w-4,y,4,h);
        int capH=18, capY=isTop?y+h-capH:y;
        g.setColor(new Color(35,25,12));
        int toothCount=5, tw=w/toothCount;
        int[] tx=new int[14], ty=new int[14];
        for (int t=0; t<toothCount; t++) {
            int tx1=x+t*tw, tx2=tx1+tw, txM=(tx1+tx2)/2;
            if (isTop) { tx[t*2]=tx1; ty[t*2]=capY; tx[t*2+1]=txM; ty[t*2+1]=capY+capH; }
            else       { tx[t*2]=tx1; ty[t*2]=capY+capH; tx[t*2+1]=txM; ty[t*2+1]=capY; }
        }
        tx[toothCount*2]=x+w; ty[toothCount*2]=isTop?capY:capY+capH;
        tx[toothCount*2+1]=x; ty[toothCount*2+1]=isTop?capY:capY+capH;
        g.fillPolygon(tx,ty,toothCount*2+2);
        g.setColor(new Color(180,120,30,60));
        if (isTop) g.drawLine(x,y+h-1,x+w-1,y+h-1);
        else       g.drawLine(x,y,x+w-1,y);
    }

    // ── Coins drawing ─────────────────────────────────────────────────────────
    void drawCoins(Graphics2D g) {
        for (Coin c : coins) {
            if (c.collectAnim > 0) continue; // handled by particle
            float bob = (float)(Math.sin(frameCount*0.1 + c.bobOffset) * 4);
            float x = c.x, y = c.y + bob;
            int r = 10;
            // Glow
            g.setColor(new Color(255, 180, 0, 60));
            g.fillOval((int)(x-r-4), (int)(y-r-4), (r+4)*2, (r+4)*2);
            // Body
            g.setColor(new Color(220, 50, 50)); // blood coin color
            g.fillOval((int)(x-r), (int)(y-r), r*2, r*2);
            g.setColor(new Color(255, 100, 100));
            g.fillOval((int)(x-r+2), (int)(y-r+2), r*2-5, r*2-5);
            // Symbol
            g.setFont(fontTiny);
            g.setColor(new Color(80,0,0,200));
            g.drawString("✦", (int)(x-5), (int)(y+4));
        }
    }

    // ── Power-up drawing ──────────────────────────────────────────────────────
    void drawPowerUps(Graphics2D g) {
        for (PowerUp pu : powerUps) {
            if (pu.collectAnim > 0) continue;
            float bob = (float)(Math.sin(frameCount*0.08 + pu.bobOffset) * 5);
            int px = (int)pu.x, py = (int)(pu.y + bob);
            Color c = getPowerUpColor(pu.type);
            // Glow
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.fillRoundRect(px-18, py-18, 36, 36, 18, 18);
            // Body
            g.setColor(c);
            g.fillRoundRect(px-12, py-12, 24, 24, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Serif", Font.BOLD, 14));
            String sym = pu.type==PowerType.SHIELD?"⛨":pu.type==PowerType.SLOWMO?"⧖":"⊕";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(sym, px - fm.stringWidth(sym)/2, py + fm.getAscent()/2 - 2);
            // Label
            g.setFont(fontTiny);
            g.setColor(c);
            String label = pu.type.name();
            g.drawString(label, px - g.getFontMetrics().stringWidth(label)/2, py+22);
        }
    }

    // ── Bird drawing ──────────────────────────────────────────────────────────
    void drawBird(Graphics2D g) {
        // Invincible flicker
        if (invincibleTimer > 0 && (invincibleTimer/5)%2==0) return;

        // Shield ring
        if (shieldTimer > 0 || shieldHitFlash > 0) {
            float pulse = (float)(0.5+0.5*Math.sin(frameCount*0.2));
            Color sc = shieldHitFlash>0 ? new Color(255,50,50,180) : new Color(50,200,255,(int)(100+pulse*80));
            g.setColor(sc);
            g.setStroke(new BasicStroke(3f));
            g.drawOval(birdX-10, birdY-10, birdW+20, birdH+20);
            g.setColor(new Color(sc.getRed(),sc.getGreen(),sc.getBlue(),40));
            g.fillOval(birdX-10, birdY-10, birdW+20, birdH+20);
            g.setStroke(new BasicStroke(1f));
        }

        // Slow-mo purple trail
        if (slowMoTimer > 0) {
            g.setColor(new Color(180,50,255, 40));
            g.fillOval(birdX-5, birdY-5, birdW+10, birdH+10);
        }

        // Magnet field
        if (magnetTimer > 0) {
            float pulse = (float)(0.5+0.5*Math.sin(frameCount*0.3));
            g.setColor(new Color(255,200,0,(int)(40+pulse*30)));
            g.drawOval(birdX-50, birdY-50, birdW+100, birdH+100);
        }

        float p = (float)(0.7+0.3*Math.sin(frameCount*0.08));
        for (int i=5; i>=1; i--) {
            float alpha = p*(6-i)/30f;
            g.setColor(new Color(1f,0f,0f,alpha));
            int exp=i*5;
            g.fillOval(birdX-exp, birdY-exp, birdW+exp*2, birdH+exp*2);
        }

        if (flapFrameImgs != null) {
            double tilt = Math.max(-30, Math.min(25, velY*3.5));
            Graphics2D g2 = (Graphics2D)g.create();
            g2.translate(birdX+birdW/2, birdY+birdH/2);
            g2.rotate(Math.toRadians(tilt));
            g2.drawImage(flapFrameImgs[flapFrame], -birdW/2,-birdH/2,birdW,birdH,null);
            g2.dispose();
        } else {
            g.setColor(new Color(200,0,0,180)); g.fillOval(birdX,birdY,birdW,birdH);
            g.setColor(Color.RED); g.drawOval(birdX+2,birdY+2,birdW-4,birdH-4);
        }
    }

    void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            float a = Math.max(0, Math.min(1, p.life));
            Color c = new Color(p.color.getRed()/255f, p.color.getGreen()/255f,
                p.color.getBlue()/255f, a*(p.color.getAlpha()/255f));
            g.setColor(c);
            float s = p.size*a+1;
            g.fill(new Ellipse2D.Float(p.x-s/2, p.y-s/2, s, s));
        }
    }

    // ── HUD ───────────────────────────────────────────────────────────────────
    void drawHUD(Graphics2D g) {
        // Score
        drawGoldBox(g, 15, 15, 120, 55);
        g.setFont(fontSmall); g.setColor(new Color(180,130,40));
        g.drawString("SCORE", 28, 33);
        drawGoldText(g, String.valueOf(score), 28, 58, fontScore);

        // High score
        drawGoldBox(g, W-165, 15, 150, 55);
        g.setFont(fontSmall); g.setColor(new Color(180,130,40));
        g.drawString("BEST", W-148, 33);
        drawGoldText(g, String.valueOf(highScore), W-148, 58, fontScore);

        // Lives (hearts)
        drawLives(g);

        // Coins
        drawGoldBox(g, W/2-60, 15, 120, 40);
        g.setFont(fontSmall); g.setColor(new Color(200,160,50));
        String coinStr = "✦ " + totalCoins;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(coinStr, W/2 - fm.stringWidth(coinStr)/2, 40);

        // Active power-up bars
        drawPowerUpBars(g);

        // Combo display
        if (comboDisplayTimer > 0 && combo >= 3) {
            float alpha = Math.min(1f, comboDisplayTimer / 30f);
            g.setColor(new Color(1f, 0.8f, 0f, alpha));
            g.setFont(fontUI);
            String cs = combo + "x COMBO!";
            FontMetrics fm2 = g.getFontMetrics();
            g.drawString(cs, W/2 - fm2.stringWidth(cs)/2, H/2 - 80);
        }

        // Bottom hint
        if (state == State.PLAYING) {
            g.setFont(fontTiny);
            g.setColor(new Color(140,100,30,160));
            g.drawString("P=Pause  U=Shop  ESC=Menu", W/2-70, H-12);
        }
    }

    void drawLives(Graphics2D g) {
        int hx = W/2 - (maxLives * 28)/2;
        for (int i = 0; i < maxLives; i++) {
            boolean filled = i < lives;
            g.setColor(filled ? new Color(220,30,30) : new Color(80,20,20,150));
            drawHeart(g, hx + i*28, 65, 22, filled);
        }
    }

    void drawHeart(Graphics2D g, int x, int y, int size, boolean filled) {
        // Simple heart shape
        int s = size;
        int[] hx = {x+s/2, x+s, x+s, x+s/2, x, x, x+s/2};
        int[] hy = {y+s, y+s/2, y, y+s/4, y, y+s/2, y+s};
        if (filled) {
            g.fillPolygon(hx, hy, 7);
            g.setColor(new Color(255,100,100,100));
            g.drawPolygon(hx, hy, 7);
        } else {
            g.setColor(new Color(80,20,20,150));
            g.drawPolygon(hx, hy, 7);
        }
    }

    void drawPowerUpBars(Graphics2D g) {
        int barX = 15, barY = 80;
        if (shieldTimer > 0)  drawPowerBar(g, barX, barY,    "SHIELD",  shieldTimer,  new Color(50,200,255));
        if (slowMoTimer > 0)  drawPowerBar(g, barX, barY+28, "SLOW-MO", slowMoTimer,  new Color(180,80,255));
        if (magnetTimer > 0)  drawPowerBar(g, barX, barY+56, "MAGNET",  magnetTimer,  new Color(255,200,0));
    }

    void drawPowerBar(Graphics2D g, int x, int y, String label, int timer, Color c) {
        int w = 110, h = 18;
        g.setColor(new Color(0,0,0,150));
        g.fillRoundRect(x, y, w, h, 4, 4);
        float frac = timer / (float)POWERUP_DURATION;
        g.setColor(c);
        g.fillRoundRect(x, y, (int)(w*frac), h, 4, 4);
        g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),150));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w, h, 4, 4);
        g.setFont(fontTiny);
        g.setColor(Color.WHITE);
        g.drawString(label, x+4, y+h-4);
    }

    // ── Shop screen ───────────────────────────────────────────────────────────
    void drawShopScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,190));
        g.fillRect(0,0,W,H);

        int panelW=420, panelH=360;
        int px=(W-panelW)/2, py=(H-panelH)/2;
        g.setColor(new Color(20,10,5,230));
        g.fillRoundRect(px, py, panelW, panelH, 16, 16);
        g.setColor(new Color(180,120,30));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(px, py, panelW, panelH, 16, 16);
        g.setStroke(new BasicStroke(1f));

        drawCenteredGoldText(g, "UPGRADE SHOP", py+40, fontUI);

        // Coins
        g.setFont(fontSmall); g.setColor(new Color(255,200,50));
        String coinLabel = "✦ " + totalCoins + " Blood Coins";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(coinLabel, W/2 - fm.stringWidth(coinLabel)/2, py+65);

        int[] levels = {upgLives, upgFlapPower, upgGapWidth};
        String[] descs = {
            "Lives: " + (1+upgLives) + " → " + (1+Math.min(upgLives+1,3)),
            "Flap: " + (upgFlapPower<3?"Lv"+(upgFlapPower+1):"MAX"),
            "Gap: " + (upgGapWidth<3?"Lv"+(upgGapWidth+1):"MAX")
        };
        String[] icons = {"♥", "↑", "↔"};
        Color[] colors = {new Color(220,50,50), new Color(80,200,80), new Color(80,150,255)};

        for (int i=0; i<3; i++) {
            int row = py + 90 + i*75;
            // Row box
            g.setColor(new Color(40,25,10,150));
            g.fillRoundRect(px+15, row, panelW-30, 60, 8, 8);
            g.setColor(new Color(120,80,30,150));
            g.drawRoundRect(px+15, row, panelW-30, 60, 8, 8);

            // Icon
            g.setFont(new Font("Serif", Font.BOLD, 26));
            g.setColor(colors[i]);
            g.drawString(icons[i], px+30, row+38);

            // Name & desc
            g.setFont(fontUI); g.setColor(new Color(220,180,80));
            g.drawString(UPG_NAMES[i], px+60, row+26);
            g.setFont(fontSmall); g.setColor(new Color(180,140,60));
            g.drawString(descs[i], px+60, row+46);

            // Buy button or MAX
            boolean maxed = levels[i] >= 3;
            int btnX = px+panelW-110, btnY = row+12, btnW=90, btnH=36;
            if (maxed) {
                g.setColor(new Color(80,60,20));
                g.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
                g.setFont(fontSmall); g.setColor(new Color(130,100,40));
                g.drawString("MAX", btnX+28, btnY+22);
                shopBtns[i] = null;
            } else {
                int cost = UPG_COST[levels[i]];
                boolean canAfford = totalCoins >= cost;
                g.setColor(canAfford ? new Color(60,40,10) : new Color(40,20,10));
                g.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
                g.setColor(canAfford ? new Color(180,130,40) : new Color(100,60,30));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(btnX, btnY, btnW, btnH, 8, 8);
                g.setStroke(new BasicStroke(1f));
                g.setFont(fontSmall);
                g.setColor(canAfford ? new Color(255,200,50) : new Color(120,80,40));
                String costStr = "✦" + cost;
                FontMetrics fm2 = g.getFontMetrics();
                g.drawString(costStr, btnX + (btnW - fm2.stringWidth(costStr))/2, btnY+22);
                shopBtns[i] = new Rectangle(btnX, btnY, btnW, btnH);
            }
        }

        // Close button
        g.setFont(fontSmall); g.setColor(new Color(200,150,50));
        String closeStr = "[ U or Click ✕ to Close ]";
        FontMetrics fm3 = g.getFontMetrics();
        g.drawString(closeStr, W/2-fm3.stringWidth(closeStr)/2, py+panelH-18);

        // X button top-right
        int cx=px+panelW-30, cy=py+10;
        g.setColor(new Color(180,80,30));
        g.fillRoundRect(cx, cy, 25, 25, 6, 6);
        g.setColor(Color.WHITE); g.setFont(fontSmall);
        g.drawString("✕", cx+6, cy+17);
    }

    // ── Overlay screens ───────────────────────────────────────────────────────
    void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,130)); g.fillRect(0,0,W,H);
        drawCenteredGoldText(g, "KikiK", H/2-100, fontTitle);
        g.setFont(fontUI); g.setColor(new Color(200,150,50));
        drawCenteredString(g, "SURVIVE THE NIGHT", H/2-45, fontUI);
        float pulse=(float)(0.5+0.5*Math.sin(frameCount*0.08));
        g.setColor(new Color(1f,0.85f,0.3f,pulse));
        drawCenteredString(g, "CLICK or SPACE to Begin", H/2+30, fontUI);
        g.setColor(new Color(140,100,30,200));
        drawCenteredString(g, "SPACE/Click=Flap  P=Pause  U=Shop  ESC=Menu", H/2+80, fontSmall);
        // Power-up guide
        g.setColor(new Color(120,100,60,180));
        drawCenteredString(g, "⛨ Shield  ⧖ Slow-Mo  ⊕ Magnet  |  ✦ = Blood Coins", H/2+108, fontTiny);
        if (highScore>0) { g.setFont(fontSmall); g.setColor(new Color(200,160,50));
            drawCenteredString(g, "Personal Best: "+highScore, H/2+135, fontSmall); }
        g.setColor(new Color(200,200,50,180));
        drawCenteredString(g, "✦ "+totalCoins+" coins  |  [U] to spend them", H/2+155, fontTiny);
    }

    void drawPauseScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,160)); g.fillRect(0,0,W,H);
        drawCenteredGoldText(g, "PAUSED", H/2-30, fontTitle);
        g.setColor(new Color(200,150,50));
        drawCenteredString(g, "P = Resume  |  U = Shop", H/2+40, fontUI);
        drawCenteredString(g, "ESC = Menu", H/2+75, fontSmall);
    }

    void drawDeadScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,170)); g.fillRect(0,0,W,H);
        g.setFont(fontTitle); g.setColor(new Color(200,0,0));
        drawCenteredString(g, "YOU PERISHED", H/2-110, fontTitle);

        drawGoldBox(g, W/2-110, H/2-70, 220, 110);
        g.setFont(fontSmall); g.setColor(new Color(180,130,40));
        drawCenteredString(g, "SCORE", H/2-45, fontSmall);
        drawCenteredGoldText(g, String.valueOf(score), H/2+12, fontScore);

        if (sessionCoins > 0) {
            g.setFont(fontSmall); g.setColor(new Color(255,200,50));
            drawCenteredString(g, "+ ✦" + sessionCoins + " coins earned", H/2+48, fontSmall);
        }
        if (score>=highScore && score>0) {
            float p=(float)(0.5+0.5*Math.sin(frameCount*0.12));
            g.setColor(new Color(1f,0.9f,0.2f,p));
            drawCenteredString(g, "✦  NEW BLOOD RECORD  ✦", H/2+72, fontUI);
        }
        if (combo >= 5) {
            g.setFont(fontSmall); g.setColor(new Color(255,180,0,200));
            drawCenteredString(g, "Best combo: " + combo + "x  +✦" + combo + " bonus!", H/2+95, fontSmall);
        }
        g.setColor(new Color(200,150,50,200));
        drawCenteredString(g, "R=Rise Again  U=Shop  ESC=Menu", H/2+120, fontUI);
        float pulse=(float)(0.5+0.5*Math.sin(frameCount*0.07));
        g.setColor(new Color(1f,0.85f,0.3f,pulse));
        drawCenteredString(g, "or Click to Restart", H/2+150, fontSmall);
    }

    void drawGoldBox(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0,0,0,160)); g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(180,130,40,200));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x,y,w,h,8,8);
        g.setStroke(new BasicStroke(1f));
    }
    void drawGoldText(Graphics2D g, String text, int x, int y, Font font) {
        g.setFont(font);
        g.setColor(new Color(0,0,0,150)); g.drawString(text,x+2,y+2);
        g.setColor(new Color(255,200,50,80)); g.drawString(text,x-1,y-1);
        g.setColor(new Color(255,210,60)); g.drawString(text,x,y);
    }
    void drawCenteredGoldText(Graphics2D g, String text, int y, Font font) {
        FontMetrics fm = g.getFontMetrics(font);
        int x = (W-fm.stringWidth(text))/2;
        g.setFont(font);
        for (int i=6; i>=1; i--) {
            float a=(7-i)/30f;
            g.setColor(new Color(1f,0.6f,0f,a));
            g.drawString(text,x-i,y-i); g.drawString(text,x+i,y+i);
        }
        g.setColor(new Color(0,0,0,150)); g.drawString(text,x+2,y+2);
        g.setColor(new Color(255,215,50)); g.drawString(text,x,y);
    }
    void drawCenteredString(Graphics2D g, String text, int y, Font font) {
        FontMetrics fm = g.getFontMetrics(font);
        g.setFont(font);
        g.drawString(text, (W-fm.stringWidth(text))/2, y);
    }

    // ── Sound ─────────────────────────────────────────────────────────────────
    void startMusic(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            bgMusic = AudioSystem.getClip();
            bgMusic.open(ais);
            bgMusic.loop(Clip.LOOP_CONTINUOUSLY);
            setClipVolume(bgMusic, musicVol);
            bgMusic.start();
        } catch (Exception e) { System.out.println("Music not loaded: "+e.getMessage()); }
    }
    void setClipVolume(Clip clip, float vol) {
        try {
            FloatControl fc=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
            fc.setValue(fc.getMinimum()+(fc.getMaximum()-fc.getMinimum())*vol);
        } catch (Exception ignored) {}
    }

    void playSound(String name) {
        try {
            AudioFormat fmt = new AudioFormat(44100,16,1,true,false);
            int frames; byte[] buf;
            switch(name) {
                case "flap": {
                    frames=2200; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=180+60*(1-i/(double)frames);
                        short s=(short)(Math.sin(2*Math.PI*freq*i/44100)*5000*(1-i/(double)frames));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "score": {
                    frames=3300; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=440+220*(i/(double)frames);
                        short s=(short)(Math.sin(2*Math.PI*freq*i/44100)*8000*Math.max(0,1-i/(double)frames*2));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "death": {
                    frames=11025; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=220-180*(i/(double)frames), noise=(Math.random()-0.5)*0.3;
                        short s=(short)((Math.sin(2*Math.PI*freq*i/44100)+noise)*9000*(1-i/(double)frames));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "coin": {
                    frames=1800; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=880+440*(i/(double)frames);
                        short s=(short)(Math.sin(2*Math.PI*freq*i/44100)*6000*Math.max(0,1-i/(double)frames*2.5));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "powerup": {
                    frames=5512; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=330+330*Math.sin(i/(double)frames*Math.PI);
                        short s=(short)(Math.sin(2*Math.PI*freq*i/44100)*10000*Math.max(0,1-i/(double)frames));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "shieldbreak": {
                    frames=6615; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=660-440*(i/(double)frames), noise=(Math.random()-0.5)*0.5;
                        short s=(short)((Math.sin(2*Math.PI*freq*i/44100)+noise)*8000*(1-i/(double)frames));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "hurt": {
                    frames=3307; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double freq=300-200*(i/(double)frames);
                        short s=(short)(Math.sin(2*Math.PI*freq*i/44100)*7000*(1-i/(double)frames));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                case "lightning": {
                    frames=4410; buf=new byte[frames*2];
                    for(int i=0;i<frames;i++) {
                        double noise=(Math.random()-0.5);
                        short s=(short)(noise*12000*(1-i/(double)frames));
                        buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
                    } break;
                }
                default: return;
            }
            DataLine.Info info=new DataLine.Info(Clip.class,fmt);
            Clip clip=(Clip)AudioSystem.getLine(info);
            clip.open(fmt,buf,0,buf.length);
            clip.start();
            clip.addLineListener(ev->{ if(ev.getType()==LineEvent.Type.STOP) clip.close(); });
        } catch(Exception ignored) {}
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("KikiK — Survive the Night");
            KikiK game = new KikiK(frame);
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}