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
import javax.imageio.ImageIO;  
import java.io.FileNotFoundException; 
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import javax.sound.sampled.*;

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

    private static final String HIGH_SCORE_FILE = "assets/userdata/highscore.txt";

    private static final String SOUND_FILE = "assets/sounds/points.wav"; 
	
	private static final String DEATH_SOUND = "assets/sounds/death.wav";

    public static void main(String[] args) {
        JFrame frame = new JFrame("SpaceShip Cruiser 1.0");
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
        setLayout(null);

        loadTextures();
        loadHighScore();

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
            spaceshipImg = ImageIO.read(new File("assets/textures/ships/ship1.jpg"));
            File[] files = new File("assets/textures/meteorites/").listFiles();
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
        } catch (IOException | NumberFormatException ignored) {}
    }

    private void saveHighScore() {
        try (FileWriter writer = new FileWriter(HIGH_SCORE_FILE)) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getBlockSpeed() {
        return 5 + (score / 10);
    }

    private void playSound(String soundFile) {
        try {
            File sound = new File(soundFile);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(sound);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
	
	private void deathSound(String soundFilePath) {
    try {
        File soundFile = new File(soundFilePath);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        clip.start();
    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
        System.err.println("Błąd przy odtwarzaniu dźwięku: " + e.getMessage());
    }
}


    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            if (keysPressed.contains(KeyEvent.VK_LEFT) && playerX > 0) {
                playerX -= 7;
            }
            if (keysPressed.contains(KeyEvent.VK_RIGHT) && playerX < BASE_WIDTH - PLAYER_SIZE) {
                playerX += 7;
            }

            Iterator<Block> iter = blocks.iterator();
            while (iter.hasNext()) {
                Block block = iter.next();
                block.fall();

                if (block.y > BASE_HEIGHT) {
                    score++;
                    if (score > highScore) highScore = score;
                    if (score % 50 == 0) {  
                        playSound(SOUND_FILE);
                    }
                    iter.remove();
                    continue;
                }

                Rectangle playerRect = new Rectangle(playerX, BASE_HEIGHT - PLAYER_SIZE - 30, PLAYER_SIZE, PLAYER_SIZE);
                Rectangle blockRect = new Rectangle(block.x, block.y, BLOCK_SIZE, BLOCK_SIZE);

                if (playerRect.intersects(blockRect)) {
					playSound(DEATH_SOUND);
                    gameOver = true;
                    blocks.clear();
                    respawnButton.setVisible(true);
                    saveHighScore();
                    break;
                }
            }

            if (rand.nextInt(30) == 1) {
                blocks.add(new Block(rand.nextInt(BASE_WIDTH - BLOCK_SIZE), -BLOCK_SIZE, getBlockSpeed()));
            }
        }

        repaint();
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
        respawnButton.setBounds(btnX, btnY, (int) (btnWidth * scaleX), (int) (btnHeight * scaleY));
    }

    private void drawGame(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.drawImage(spaceshipImg, playerX, BASE_HEIGHT - PLAYER_SIZE - 30, PLAYER_SIZE, PLAYER_SIZE, null);

        for (Block block : blocks) {
            Graphics2D g2d = (Graphics2D) g.create();

            int centerX = block.x + BLOCK_SIZE / 2;
            int centerY = block.y + BLOCK_SIZE / 2;

            block.angle += 0.001;

            g2d.rotate(block.angle, centerX, centerY);

            Image meteorite = meteoriteImages.get(rand.nextInt(meteoriteImages.size()));
            g2d.drawImage(meteorite, block.x, block.y, BLOCK_SIZE, BLOCK_SIZE, null);

            g2d.dispose();
        }

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
		score = 0;
		gameOver = false;
		blocks.clear();
		respawnButton.setVisible(false);
	
		repaint();
	}

	class Block {
		int x, y;
		int speed;
		double angle;

		public Block(int x, int y, int speed) {
			this.x = x;
			this.y = y;
			this.speed = speed;
			this.angle = 0;
		}

			public void fall() {
			y += speed;
			angle += 0.05;
		}
	}
}
