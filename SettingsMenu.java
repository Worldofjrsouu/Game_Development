import java.awt.*;
import javax.swing.*;

public class SettingsMenu extends JFrame {

    public static int gameSpeed = 4;

    Image bg;

    public SettingsMenu() {
        setTitle("KikiK Settings");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 🖼️ LOAD SAME BACKGROUND
        bg = new ImageIcon("menu_bg.jpeg").getImage();

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
            }
        };

        panel.setLayout(null);

        JLabel title = new JLabel("SETTINGS");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        title.setBounds(320, 100, 300, 50);

        JLabel label = new JLabel("Game Speed:");
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        label.setForeground(Color.WHITE);
        label.setBounds(330, 200, 200, 30);

        JButton slow = new JButton("Slow");
        JButton normal = new JButton("Normal");
        JButton fast = new JButton("Fast");

        slow.setBounds(250, 260, 100, 40);
        normal.setBounds(350, 260, 100, 40);
        fast.setBounds(450, 260, 100, 40);

        JButton back = new JButton("BACK");
        back.setBounds(350, 350, 100, 40);

        panel.add(title);
        panel.add(label);
        panel.add(slow);
        panel.add(normal);
        panel.add(fast);
        panel.add(back);

        add(panel);

        slow.addActionListener(e -> gameSpeed = 2);
        normal.addActionListener(e -> gameSpeed = 4);
        fast.addActionListener(e -> gameSpeed = 7);

        back.addActionListener(e -> dispose());

        setVisible(true);
    }
}