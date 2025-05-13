import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;  // Added import for ImageIO
import java.io.FileNotFoundException; // Added import for exception handling
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

public class spaceshipcruiser extends JPanel implements ActionListener {
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;

    private int score = 0;
    private int highScore = 0;
    private boolean gameOver = false;

    private final Timer timer;
    private final ArrayList<Block> blocks = new ArrayList<>();
    private final Set<Integer> keysPressed = new HashSet<>();

    private int playerX = BASE_WIDTH / 2;
    private final int PLAYER_SIZE = 50;
    private final int BLOCK_SIZE = 50;

    private final JButton respawnButton;

    private Image spaceshipImg;
    private final ArrayList<Image> meteoriteImages = new ArrayList<>();
    private final Random rand = new Random();

    private static final String HIGH_SCORE_FILE = "userdata/highscore.txt";  // Path to high score file

    public static void main(String[] args) {
        JFrame frame = new JFrame("SpaceShip Cruiser");
        spaceshipcruiser game = new spaceshipcruiser();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(BASE_WIDTH, BASE_HEIGHT);
        frame.setResizable(true);
        frame.add(game);
        frame.setVisible(true);
    }

    public spaceshipcruiser() {
        setFocusable(true);
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setLayout(null); // Manual button placement

        loadTextures();
        loadHighScore();  // Load highscore from file

        timer = new Timer(1000 / 60, this);
        timer.start();

        respawnButton = new JButton("Respawn");
        respawnButton.setFocusable(false);
        respawnButton.setVisible(false);
        respawnButton.addActionListener(e -> restartGame());
        add(respawnButton);

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                keysPressed.add(e.getKeyCode());
            }

            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode());
            }
        });
    }

    private void loadTextures() {
        try {
            spaceshipImg = ImageIO.read(new File("textures/ships/ship1.jpg"));
            File[] files = new File("textures/meteorites/").listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".jpg")) {
                        meteoriteImages.add(ImageIO.read(file));
                    }
                }
            }

            if (spaceshipImg == null || meteoriteImages.isEmpty()) {
                throw new IOException("Missing textures!");
            }

        } catch (IOException e) {
            System.err.println("Error loading textures: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading textures: " + e.getMessage());
            System.exit(1);
        }
    }

    private void loadHighScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORE_FILE))) {
            String line = reader.readLine();
            if (line != null) {
                highScore = Integer.parseInt(line);
            }
        } catch (FileNotFoundException e) {
            // File doesn't exist, so we'll just leave highScore as 0
        } catch (IOException | NumberFormatException e) {
            // Error reading the file or invalid format, so default to 0
            e.printStackTrace();
        }
    }

    private void saveHighScore() {
        try (FileWriter writer = new FileWriter(HIGH_SCORE_FILE)) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) updateGame();
        repaint();
    }

    private void updateGame() {
        // Smooth steering
        if (keysPressed.contains(KeyEvent.VK_LEFT) && playerX > 0)
            playerX -= 7;
        if (keysPressed.contains(KeyEvent.VK_RIGHT) && playerX < BASE_WIDTH - PLAYER_SIZE)
            playerX += 7;

        // Update blocks
        Iterator<Block> iter = blocks.iterator();
        while (iter.hasNext()) {
            Block block = iter.next();
            block.fall();

            if (block.y > BASE_HEIGHT) {
                score++;
                highScore = Math.max(highScore, score);
                iter.remove();
                continue;
            }

            // Collision detection
            Rectangle playerRect = new Rectangle(playerX, BASE_HEIGHT - PLAYER_SIZE - 30, PLAYER_SIZE, PLAYER_SIZE);
            Rectangle blockRect = new Rectangle(block.x, block.y, BLOCK_SIZE, BLOCK_SIZE);
            if (playerRect.intersects(blockRect)) {
                gameOver = true;
                blocks.clear();
                respawnButton.setVisible(true);
                saveHighScore();  // Save highscore when game over
                break;
            }
        }

        // Random block spawn
        if (new Random().nextInt(30) == 1) {
            blocks.add(new Block(new Random().nextInt(BASE_WIDTH - BLOCK_SIZE), -BLOCK_SIZE));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();
        double scaleX = width / (double) BASE_WIDTH;
        double scaleY = height / (double) BASE_HEIGHT;

        Graphics2D g2 = (Graphics2D) g;
        g2.scale(scaleX, scaleY);

        if (gameOver) {
            drawGameOver(g2);
            updateRespawnButton(scaleX, scaleY);
        } else {
            drawGame(g2);
            respawnButton.setVisible(false);
        }
    }

    private void updateRespawnButton(double scaleX, double scaleY) {
        int btnWidth = 150;
        int btnHeight = 40;
        int btnX = (int) ((BASE_WIDTH / 2.0 - btnWidth / 2.0) * scaleX);
        int btnY = (int) ((BASE_HEIGHT / 2.0 + 80) * scaleY);
        respawnButton.setBounds(btnX, btnY, (int)(btnWidth * scaleX), (int)(btnHeight * scaleY));
    }

    private void drawGame(Graphics2D g) {
        // Draw spaceship with texture
        g.drawImage(spaceshipImg, playerX, BASE_HEIGHT - PLAYER_SIZE - 30, PLAYER_SIZE, PLAYER_SIZE, null);

        // Draw meteorites with random textures
        for (Block block : blocks) {
            int meteoriteIndex = rand.nextInt(meteoriteImages.size());
            g.drawImage(meteoriteImages.get(meteoriteIndex), block.x, block.y, BLOCK_SIZE, BLOCK_SIZE, null);
        }

        // Score display
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Score: " + score, 10, 30);
        g.drawString("Highscore: " + highScore, BASE_WIDTH - 160, 30);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("GAME OVER", BASE_WIDTH / 3, BASE_HEIGHT / 3);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.drawString("Score: " + score, BASE_WIDTH / 3, BASE_HEIGHT / 2);
        g.drawString("Highscore: " + highScore, BASE_WIDTH / 3, BASE_HEIGHT / 2 + 40);
    }

    private void restartGame() {
        playerX = BASE_WIDTH / 2;
        score = 0;
        blocks.clear();
        gameOver = false;
        respawnButton.setVisible(false);
    }

    private static class Block {
        int x, y;

        public Block(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void fall() {
            y += 5;
        }
    }
}
