package pacman;

//external resources used to develop player movement, sprites etc.
//such as stackoverflow, github, etc.

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.*;

class CE203_1906027_Ass2  extends JPanel implements ActionListener {

    private Dimension d; //sets height and width of game
    private final Font font = new Font("Arial", Font.BOLD, 14); //assigns in-game text to be displayed
    private boolean running = false; //checks if the game is running
    private boolean dying = false; //checks if Pacman is alive

    private final int BLOCK_SIZE = 24; //describes size of blocks in-game
    private final int N_OF_BLOCKS = 15; //the number of blocks wide and high the game is
    private final int WINDOW_SIZE = N_OF_BLOCKS * BLOCK_SIZE;
    private final int MAX_GHOSTS = 12; //max number of enemies
    private final int PACMAN_SPEED = 6;

    private int N_GHOSTS = 6; //number of enemies in the beginning
    private int hearts, score;
    private int[] dx, dy; //determines position of enemies
    private int[] ghost_x, ghost_y, ghost_dx, ghost_dy; //determines number and positions of ghosts
    private int[] ghost_speed;

    private Image heart, ghost, dot;
    private Image up, down, left, right; //images for pacman animation

    private int pacman_x, pacman_y, pacman_dx, pacman_dy; //sprite coordinates for movement
    private int req_dx, req_dy; //determined in TAdapter inner class, controlled by arrow keys

    private final int validSpeed[] = {1, 2, 3, 4, 6, 8};
    private final int maxSpeed = 6;
    private int currentSpeed = 3;

    private short[] screenData; //array takes data of current level to redraw the game
    private Timer timer; //allows animation

    //Score IO
    private static String scoreFile = "src/pacman/images/scores.txt";
    private BufferedReader br = null;
    private BufferedWriter bw = null;
    private ArrayList<Scorer> scores = new ArrayList<Scorer>();

    //level construction:
    //0 = obstacle, 1 = left border, 2 = top border,
    //4 = right border, 8 = bottom border, 16 = dot, 64 = big dot
    //the level is created by adding the values together to create each element
    private final short levelData[] = {
            19, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 22,
            17, 16, 16, 16, 16, 24, 16, 16, 16, 16, 16, 16, 16, 16, 20,
            25, 24, 24, 24, 28, 0, 17, 16, 16, 16, 16, 16, 16, 16, 20,
            0, 0, 0, 0, 0, 0, 17, 16, 16, 16, 16, 16, 16, 16, 20,
            19, 18, 18, 18, 18, 18, 16, 16, 16, 16, 24, 24, 24, 24, 20,
            17, 16, 16, 16, 16, 16, 16, 16, 16, 20, 0, 0, 0, 0, 21,
            17, 16, 16, 16, 16, 16, 16, 16, 16, 20, 0, 0, 0, 0, 21,
            17, 16, 16, 16, 24, 16, 16, 16, 16, 20, 0, 0, 0, 0, 21,
            17, 16, 16, 20, 0, 17, 16, 16, 16, 16, 18, 18, 18, 18, 20,
            17, 24, 24, 28, 0, 25, 24, 24, 16, 16, 16, 16, 16, 16, 20,
            21, 0, 0, 0, 0, 0, 0, 0, 17, 16, 16, 16, 16, 16, 20,
            17, 18, 18, 22, 0, 19, 18, 18, 16, 16, 16, 16, 16, 16, 20,
            17, 16, 16, 20, 0, 17, 16, 16, 16, 16, 16, 16, 16, 16, 20,
            17, 16, 16, 20, 0, 17, 16, 16, 16, 16, 16, 16, 16, 64, 20,
            25, 24, 24, 24, 26, 24, 24, 24, 24, 24, 24, 24, 24, 24, 28
    };

    public CE203_1906027_Ass2 () {
        loadImages();
        initVariables();
        addKeyListener(new TAdapter());
        setFocusable(true); //focuses window
        initGame(); //starts the game
    }

    //method used to load sprites
    private void loadImages() {
        down = new ImageIcon("src/pacman/images/down.gif").getImage();
        up = new ImageIcon("src/pacman/images/up.gif").getImage();
        left = new ImageIcon("src/pacman/images/left.gif").getImage();
        right = new ImageIcon("src/pacman/images/right.gif").getImage();
        ghost = new ImageIcon("src/pacman/images/ghost.png").getImage();
        heart = new ImageIcon("src/pacman/images/heart.png").getImage();
        dot = new ImageIcon("src/pacman/images/dot.png").getImage();
    }

    //method used to initialise variables
    private void initVariables() {
        screenData = new short[N_OF_BLOCKS * N_OF_BLOCKS];
        d = new Dimension(400, 400);
        ghost_x = new int[MAX_GHOSTS];
        ghost_dx = new int[MAX_GHOSTS];
        ghost_y = new int[MAX_GHOSTS];
        ghost_dy = new int[MAX_GHOSTS];
        ghost_speed = new int[MAX_GHOSTS];
        dx = new int[4];
        dy = new int[4];

        timer = new Timer(40, this); //game redrawn every 40ms
        timer.start(); //starts timer
    }

    //method containing graphics functions that are called and displayed
    private void playGame(Graphics2D g2d) {
        if (dying) {
            death();
        } else {
            movePacman();
            drawPacman(g2d);
            moveGhosts(g2d);
            checkMaze();
        }
    }

    //method used to show intro screen
    private void showIntro(Graphics2D g2d) {
        String start = "Press SPACE to start";
        g2d.setColor(Color.yellow);
        g2d.drawString(start, (WINDOW_SIZE) / 4, 150);
    }

    //similar method used to display the score and hearts
    private void drawScore(Graphics2D g) {
        g.setFont(font);
        g.setColor(new Color(5, 181, 79));
        String s = "Score: " + score + " High score: "; // + displayScores("src/pacman/images/scores.txt");
        //displaying the high score makes the game not run :(
        g.drawString(s, WINDOW_SIZE / 2 + 96, WINDOW_SIZE + 16);

        for (int i = 0; i < hearts; i++) {
            g.drawImage(heart, i * 28 + 8, WINDOW_SIZE + 1, this);
        }
    }

    //method used to add and sort scores in a file according to player name
    public void addScore() {
        String name = JOptionPane.showInputDialog("Input your name to save score:");
        Scorer temp = new Scorer(name, score); //game.getScore());
        scores.add(temp);
        Collections.sort(scores, new ScoreSorter());
        try {
            if (scoreFile == null) {
                throw new IllegalArgumentException();
            }
            this.bw = new BufferedWriter(new FileWriter(scoreFile));
            for (Scorer x: scores) {
                bw.write(x.getName().replaceAll("-", "") + "-" + x.getScore());
                bw.newLine();
            }
            bw.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException();
        } catch (IOException e) {
            System.out.print("Error adding score to score data");
            e.printStackTrace();
        }
    }

    //Displays sorted scores in descending order
    public String displayScores(String scoreFile) {
        running = false;
        String toPrint = "";
        try {
            if (scoreFile == null) {
                throw new IllegalArgumentException();
            }
            this.br = new BufferedReader(new FileReader(scoreFile));
            while (br.ready()) {
                String temp = br.readLine();
                if (temp != null && temp.contains("-")) {
                    int separator = temp.indexOf("-");
                    int score = Integer.parseInt(temp.substring(separator + 1));
                    toPrint = toPrint + temp.substring(0,separator) + "-" + score + "\n";
                }
            }
        } catch (FileNotFoundException e) {
            toPrint = "Error accessing score data";
            throw new IllegalArgumentException();
        } catch (IOException e) {
            toPrint = "Error accessing score data";
            System.out.print("Error reading score data to score data");
            e.printStackTrace();
        }

        if (toPrint.isEmpty()) {
            toPrint = "No Scores Saved";
        }
        requestFocusInWindow();
        repaint();
        return toPrint;
    }

    public void scoreButton() {
        //Button that displays sorted scores
        final JButton display = new JButton("Scores");
        display.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String toPrint = displayScores("src/pacman/images/scores.txt");
                JOptionPane.showMessageDialog(null, toPrint);
            }
        });
        final JPanel control_panel = new JPanel();
        this.add(control_panel, BorderLayout.NORTH);
        control_panel.add(display);
    }

    //method used to check if there are any points left to collect
    public void checkMaze() {
        int i = 0;
        boolean finished = true;

        while (i < N_OF_BLOCKS * N_OF_BLOCKS && finished) {
            if ((screenData[i]) != 0) {
                finished = false;
            }
            i++;
        }

        if (finished) { //if all points are consumed the level restarts
            score += 50; //score increases by 50
            addScore();
            if (N_GHOSTS < MAX_GHOSTS) {
                N_GHOSTS++; //enemies increase by 1
            }

            if (currentSpeed < maxSpeed) {
                currentSpeed++; //speed increases by 1
            }
            initLevel();
        }
    }

    //method that manages player death
    private void death() {
        hearts--; //hearts decrease every death
        if (hearts == 0) {
            running = false; //if hearts equal 0 the game is lost
            addScore();
        }
        continueLevel();
    }

    //method used to decide enemies' movement
    public void moveGhosts(Graphics2D g2d) {
        int pos;
        int count;
        //set position again using block size and number of enemies:
        for (int i = 0; i < N_GHOSTS; i++) {
            if (ghost_x[i] % BLOCK_SIZE == 0 && ghost_y[i] % BLOCK_SIZE == 0) { //enemies move on one square and continue only if finished
                pos = ghost_x[i] / BLOCK_SIZE + N_OF_BLOCKS * (ghost_y[i] / BLOCK_SIZE);
                count = 0;

                //determine how enemies can move using border information (1, 2, 4, 8):
                if ((screenData[pos] & 1) == 0 && ghost_dx[i] != 1) {
                    dx[count] = -1;
                    dy[count] = 0;
                    count++;
                }
                if ((screenData[pos] & 2) == 0 && ghost_dy[i] != 1) {
                    dx[count] = 0;
                    dy[count] = -1;
                    count++;
                }
                if ((screenData[pos] & 4) == 0 && ghost_dx[i] != -1) {
                    dx[count] = 1;
                    dy[count] = 0;
                    count++;
                }
                if ((screenData[pos] & 8) == 0 && ghost_dy[i] != -1) {
                    dx[count] = 0;
                    dy[count] = 1;
                    count++;
                }

                //determine where an enemy is located (out of 225 squares):
                if (count == 0) {
                    if ((screenData[pos] & 15) == 15) {
                        ghost_dy[i] = 0;
                        ghost_dx[i] = 0;
                    } else {
                        ghost_dy[i] = -ghost_dy[i];
                        ghost_dx[i] = -ghost_dx[i];
                    }
                } else { //cannot move over obstacles
                    count = (int) (Math.random() * count);

                    if (count > 3) {
                        count = 3;
                    }
                    ghost_dy[i] = dy[count];
                    ghost_dx[i] = dx[count];
                }
            }
            ghost_x[i] = ghost_x[i] + (ghost_dx[i] * ghost_speed[i]);
            ghost_y[i] = ghost_y[i] + (ghost_dy[i] * ghost_speed[i]);
            drawGhost(g2d, ghost_x[i] + 1, ghost_y[i] + 1);

            //if pacman touches an enemy, a life is lost
            if (pacman_x > (ghost_x[i] - 12) && pacman_x < (ghost_x[i] + 12)
                    && pacman_y > (ghost_y[i] - 12) && pacman_y < (ghost_y[i] + 12)
                    && running) {

                dying = true;
            }
        }
    }

    public void drawGhost(Graphics2D g2d, int x, int y) {
        g2d.drawImage(ghost, x, y, this);
    }
    /*public void drawDot(Graphics2D g2d, int x, int y) {
        g2d.drawImage(dot, x, y, this);
    }*/

    //method that checks the movement
    public void movePacman() {
        int pos;
        short ch;

        //determine position of pacman:
        if (pacman_x % BLOCK_SIZE == 0 && pacman_y % BLOCK_SIZE == 0) {
            pos = pacman_x / BLOCK_SIZE + N_OF_BLOCKS * (pacman_y / BLOCK_SIZE);
            ch = screenData[pos];
            if ((ch & 16) != 0) { //if pacman is on a block with a dot:
                screenData[pos] = (short) (ch & 15); //remove dot
                score++; //and increase score
            }
            if (req_dx != 0 || req_dy != 0) {
                //if pacman is on one of the borders:
                if (!((req_dx == -1 && req_dy == 0 && (ch & 1) != 0)
                        || (req_dx == 1 && req_dy == 0 && (ch & 4) != 0)
                        || (req_dx == 0 && req_dy == -1 && (ch & 2) != 0)
                        || (req_dx == 0 && req_dy == 1 && (ch & 8) != 0))) {
                    pacman_dx = req_dx;
                    pacman_dy = req_dy; //then pacman cannot move
                }
            }
            //if pacman is not moving:
            if ((pacman_dx == -1 && pacman_dy == 0 && (ch & 1) != 0)
                    || (pacman_dx == 1 && pacman_dy == 0 && (ch & 4) != 0)
                    || (pacman_dx == 0 && pacman_dy == -1 && (ch & 2) != 0)
                    || (pacman_dx == 0 && pacman_dy == 1 && (ch & 8) != 0)) {
                pacman_dx = 0;
                pacman_dy = 0; //then set variables to 0
            }
        }
        //adjust speed accordingly:
        pacman_x = pacman_x + PACMAN_SPEED * pacman_dx;
        pacman_y = pacman_y + PACMAN_SPEED * pacman_dy;
    }

    //method used to draw corresponding images based on pacman's movement
    public void drawPacman(Graphics2D g2d) {
        if (req_dx == -1) { //check which arrow key is pressed
            //draw corresponding image:
            g2d.drawImage(left, pacman_x + 1, pacman_y + 1, this);
        } else if (req_dx == 1) {
            g2d.drawImage(right, pacman_x + 1, pacman_y + 1, this);
        } else if (req_dy == -1) {
            g2d.drawImage(up, pacman_x + 1, pacman_y + 1, this);
        } else {
            g2d.drawImage(down, pacman_x + 1, pacman_y + 1, this);
        }
    }

    //method used to draw game with 225 position
    private void drawMaze(Graphics2D g2d) {

        short i = 0;
        int x, y;

        for (y = 0; y < WINDOW_SIZE; y += BLOCK_SIZE) {
            for (x = 0; x < WINDOW_SIZE; x += BLOCK_SIZE) { //draws the x and y axis of the array

                g2d.setColor(new Color(0, 72, 251));
                g2d.setStroke(new BasicStroke(5)); //thickness of border

                if ((levelData[i] == 0)) { //obstacles
                    g2d.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
                }

                if ((screenData[i] & 1) != 0) { //left border
                    g2d.drawLine(x, y, x, y + BLOCK_SIZE - 1);
                }

                if ((screenData[i] & 2) != 0) { //top border
                    g2d.drawLine(x, y, x + BLOCK_SIZE - 1, y);
                }

                if ((screenData[i] & 4) != 0) { //right border
                    g2d.drawLine(x + BLOCK_SIZE - 1, y, x + BLOCK_SIZE - 1,
                            y + BLOCK_SIZE - 1);
                }

                if ((screenData[i] & 8) != 0) { //bottom border
                    g2d.drawLine(x, y + BLOCK_SIZE - 1, x + BLOCK_SIZE - 1,
                            y + BLOCK_SIZE - 1);
                }

                if ((screenData[i] & 16) != 0) { //small dots
                    g2d.setColor(new Color(255, 255, 255));
                    g2d.fillOval(x + 10, y + 10, 6, 6);
                }

                if ((screenData[i] & 64) != 0) { //big dots
                    g2d.setColor(new Color(255, 0, 0));
                    g2d.fillOval(x + 10, y + 10, 6, 6);
                }

                i++;
            }
        }
    }

    //method for initialising the game
    private void initGame() {
        hearts = 3;
        score = 0;
        initLevel(); //initialise level
        N_GHOSTS = 6;
        currentSpeed = 3;
    }

    //method for initialising the level
    private void initLevel() {
        int i;
        for (i = 0; i < N_OF_BLOCKS * N_OF_BLOCKS; i++) { //copies levelData array to screenData
            screenData[i] = levelData[i];
        }
        continueLevel();
    }

    //method defines position of enemies
    private void continueLevel() {
        int dx = 1;
        int random;

        for (int i = 0; i < N_GHOSTS; i++) { //assigns a random speed to enemies
            ghost_y[i] = 4 * BLOCK_SIZE;
            ghost_x[i] = 4 * BLOCK_SIZE;
            ghost_dy[i] = 0;
            ghost_dx[i] = dx;
            dx = -dx;
            random = (int) (Math.random() * (currentSpeed + 1));

            if (random > currentSpeed) {
                random = currentSpeed;
            }
            ghost_speed[i] = validSpeed[random]; //the speed may only be one available from the validSpeed array
        }
        //define the starting position of pacman:
        pacman_x = 7 * BLOCK_SIZE;
        pacman_y = 11 * BLOCK_SIZE;
        pacman_dx = 0;
        pacman_dy = 0;
        //controlled by cursor keys:
        req_dx = 0;
        req_dy = 0;
        dying = false;
    }

    //method used to complete graphic components
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, d.width, d.height);

        drawMaze(g2d);
        drawScore(g2d);

        if (running) {
            playGame(g2d);
        } else {
            showIntro(g2d);
        }
        Toolkit.getDefaultToolkit().sync();
        g2d.dispose();
    }

    //arrow key event handler
    class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (running) { //if the game is running pacman is controlled by arrow keys
                if (key == KeyEvent.VK_LEFT) {
                    req_dx = -1; //assign left key
                    req_dy = 0;
                } else if (key == KeyEvent.VK_RIGHT) {
                    req_dx = 1;
                    req_dy = 0; //assign right key
                } else if (key == KeyEvent.VK_UP) {
                    req_dx = 0;
                    req_dy = -1; //assign up key
                } else if (key == KeyEvent.VK_DOWN) {
                    req_dx = 0;
                    req_dy = 1; //assign down key
                } else if (key == KeyEvent.VK_ESCAPE && timer.isRunning()) { //end game when escape is pressed
                    /*if (hiscore.changed) {
                        hiscore.save(hiscoreFile());
                    }*/
                    running = false;
                }
            } else {
                if (key == KeyEvent.VK_SPACE) { //run game when space is pressed
                    running = true;
                    initGame();
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public class Scorer {
        private String name;
        private int score;

        public Scorer(String n, int score) {
            this.name = n;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }

    }

    public class ScoreSorter implements Comparator<Scorer> {
        @Override
        public int compare(Scorer s1, Scorer s2) {
            return s2.getScore() - s1.getScore();
        }
    }
}

class Main extends JFrame {

    public Main() {
        add(new CE203_1906027_Ass2 ());
    }

    public static void main(String[] args) {
        Main pacman = new Main();
        //Game.hiscoreFile();
        pacman.setVisible(true);
        pacman.setTitle("1906027 Pacman");
        pacman.setSize(380, 420);
        pacman.setDefaultCloseOperation(EXIT_ON_CLOSE);
        pacman.setLocationRelativeTo(null);
    }
}
