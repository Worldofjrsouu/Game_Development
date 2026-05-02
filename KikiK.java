import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class KikiK extends JPanel {

    int boardWidth = 800;
    int boardHeight = 600;

    int birdX = boardWidth / 4;
    int birdY = boardHeight / 2;
    int birdSize = 30;

    int velocityY = 0;
    int gravity = 1;

    boolean gameOver = false;
    boolean paused = false;

    int score = 0;

    class Pipe {
        int x = boardWidth;
        int gapY;
        int width = 80;
        int gapHeight = 180;
        boolean passed = false;

        Pipe(int gapY) {
            this.gapY = gapY;
        }
    }

    ArrayList<Pipe> pipes = new ArrayList<>();
    Random random = new Random();

    Timer timer;
    JFrame parentFrame;

    public KikiK(JFrame frame) {

        this.parentFrame = frame;

        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.cyan);
        setFocusable(true);

        setupControls();

        timer = new Timer(16, e -> update());
        timer.start();

        spawnPipe();
    }

    // ================= CONTROLS =================
    public void setupControls() {

        getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "jump");
        getActionMap().put("jump", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!gameOver && !paused) velocityY = -12;
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke("R"), "restart");
        getActionMap().put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver) restart();
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "menu");
        getActionMap().put("menu", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.dispose();
                new GameMenu().setVisible(true);
            }
        });

        // ⏸ PAUSE
        getInputMap().put(KeyStroke.getKeyStroke("P"), "pause");
        getActionMap().put("pause", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                paused = !paused;
            }
        });
    }

  
    public void update() {

        if (!gameOver && !paused) {

            velocityY += gravity;
            birdY += velocityY;

            movePipes();
            checkCollision();
        }

        repaint();
    }

   
    public void movePipes() {

        for (Pipe pipe : pipes) {
            pipe.x -= SettingsMenu.gameSpeed;

            if (!pipe.passed && pipe.x < birdX) {
                score++;
                pipe.passed = true;
            }
        }

        if (!pipes.isEmpty() && pipes.get(0).x < -80) {
            pipes.remove(0);
        }

        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < 400) {
            spawnPipe();
        }
    }

    public void spawnPipe() {
        int gapY = 100 + random.nextInt(300);
        pipes.add(new Pipe(gapY));
    }

   
    public void checkCollision() {

        if (birdY < 0 || birdY > boardHeight) {
            endGame();
        }

        Rectangle bird = new Rectangle(birdX, birdY, birdSize, birdSize);

        for (Pipe pipe : pipes) {

            Rectangle top = new Rectangle(pipe.x, 0, pipe.width, pipe.gapY);
            Rectangle bottom = new Rectangle(pipe.x, pipe.gapY + pipe.gapHeight,
                    pipe.width, boardHeight);

            if (bird.intersects(top) || bird.intersects(bottom)) {
                endGame();
            }
        }
    }

    // ================= GAME OVER =================
    public void endGame() {
        gameOver = true;
        GameData.updateScore(score);
    }

    // ================= RESTART =================
    public void restart() {
        birdY = boardHeight / 2;
        velocityY = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
        paused = false;
        spawnPipe();
    }

  
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // bird
        g.setColor(Color.yellow);
        g.fillOval(birdX, birdY, birdSize, birdSize);

        // pipes
        g.setColor(Color.green);
        for (Pipe p : pipes) {
            g.fillRect(p.x, 0, p.width, p.gapY);
            g.fillRect(p.x, p.gapY + p.gapHeight,
                    p.width, boardHeight);
        }

        // score
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 20, 30);

        // pause
        if (paused) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("PAUSED", 320, 300);
        }

        // game over
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("GAME OVER", 230, 300);

            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("R = Restart | ESC = Menu", 260, 340);
        }
    }
}