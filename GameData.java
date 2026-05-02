import java.io.*;

public class GameData {

    public static int highScore = 0;
    private static final String FILE_NAME = "highscore.txt";

    // Load score when game starts
    public static void loadScore() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            highScore = Integer.parseInt(br.readLine());
        } catch (Exception e) {
            highScore = 0;
        }
    }

    // Save score
    public static void saveScore() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            pw.println(highScore);
        } catch (Exception e) {
            System.out.println("Error saving score");
        }
    }

    // Update high score
    public static void updateScore(int score) {
        if (score > highScore) {
            highScore = score;
            saveScore();
        }
    }
}