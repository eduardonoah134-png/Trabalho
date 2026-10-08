import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Animated "Website is Under Construction" Application in Java.
 * Cleaned, robust, zero warnings.
 */
public class UnderConstructionApp extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final int PORT = 8080;

    @SuppressWarnings("this-escape")
    public UnderConstructionApp() {
        super("Website is Under Construction - Java Animated Showcase");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);

        // Main animated canvas
        add(new AnimatedCanvas(), BorderLayout.CENTER);

        // Top toolbar with browser launch button & controls
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        topBar.setBackground(new Color(38, 15, 92));

        JLabel infoLabel = new JLabel("🚀 Servidor Java ativo em http://localhost:" + PORT);
        infoLabel.setForeground(new Color(200, 190, 240));
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnOpenBrowser = new JButton("🌐 Abrir no Navegador (Web)");
        btnOpenBrowser.setFocusPainted(false);
        btnOpenBrowser.setBackground(new Color(108, 92, 231));
        btnOpenBrowser.setForeground(Color.WHITE);
        btnOpenBrowser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenBrowser.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpenBrowser.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btnOpenBrowser.addActionListener(e -> openWebBrowser());

        topBar.add(infoLabel);
        topBar.add(btnOpenBrowser);
        add(topBar, BorderLayout.NORTH);
    }

    private void openWebBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + PORT));
            } else {
                JOptionPane.showMessageDialog(this, "Abra o navegador em: http://localhost:" + PORT);
            }
        } catch (IOException | URISyntaxException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir navegador: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        // Start background Java HTTP Server
        startHttpServer();

        // Launch Swing GUI
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
                // Fallback to default look and feel
            }
            
            UnderConstructionApp app = new UnderConstructionApp();
            app.setVisible(true);
        });
    }

    private static void startHttpServer() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
                server.createContext("/", new StaticFileHandler());
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                System.out.println("Java HTTP Server rodando em: http://localhost:" + PORT);
            } catch (IOException e) {
                System.err.println("Não foi possível iniciar o servidor HTTP: " + e.getMessage());
            }
        });
    }

    /**
     * Handler to serve the HTML/CSS/JS files from the web directory.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /**
     * Custom JPanel with 60 FPS Java2D rendering.
     */
    static class AnimatedCanvas extends JPanel {
        @Serial
        private static final long serialVersionUID = 1L;

        private double time = 0.0;
        private final transient List<Star> stars = new ArrayList<>();
        private final transient List<Sparkle> sparkles = new ArrayList<>();
        private final transient Random rand = new Random();

        public AnimatedCanvas() {
            setBackground(new Color(43, 20, 102));

            // Generate starry background particles
            for (int i = 0; i < 40; i++) {
                stars.add(new Star(rand.nextDouble(), rand.nextDouble(), rand.nextDouble() * 2 + 1, rand.nextDouble() * Math.PI * 2));
            }

            // 60 FPS Animation Timer (~16 ms)
            Timer animTimer = new Timer(16, e -> {
                time += 0.035;
                updateSparkles();
                repaint();
            });
            animTimer.start();
        }

        private void updateSparkles() {
            Iterator<Sparkle> it = sparkles.iterator();
            while (it.hasNext()) {
                Sparkle s = it.next();
                s.update();
                if (s.isDead()) it.remove();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            // High Quality Antialiasing and Rendering Hints
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int width = getWidth();
            int height = getHeight();

            // 1. Radial Dark Purple Background
            Point2D center = new Point2D.Float(width / 2f, height / 2f);
            float radius = (float) Math.max(width, height);
            float[] dist = {0.0f, 0.6f, 1.0f};
            Color[] colors = {new Color(58, 26, 130), new Color(38, 15, 92), new Color(24, 8, 64)};
            RadialGradientPaint radialPaint = new RadialGradientPaint(center, radius, dist, colors);
            g2.setPaint(radialPaint);
            g2.fillRect(0, 0, width, height);

            // 2. Background Stars
            for (Star s : stars) {
                float alpha = (float) (0.3 + 0.5 * Math.sin(time * 2 + s.phase));
                g2.setColor(new Color(1f, 1f, 1f, Math.max(0.1f, Math.min(1f, alpha))));
                g2.fill(new Ellipse2D.Double(s.rx * width, s.ry * height, s.size, s.size));
            }

            // 3. Rotating Background Gears
            drawGear(g2, width * 0.12, height * 0.2, 50, (float) (time * 0.4), new Color(255, 255, 255, 12));
            drawGear(g2, width * 0.88, height * 0.8, 80, (float) (-time * 0.3), new Color(255, 255, 255, 10));
            drawGear(g2, width * 0.82, height * 0.22, 38, (float) (time * 0.5), new Color(255, 255, 255, 14));

            // 4. White Rounded Card
            int cardW = Math.min(940, width - 60);
            int cardH = Math.min(520, height - 70);
            int cardX = (width - cardW) / 2;
            int cardY = (height - cardH) / 2;

            // Card Shadow
            g2.setColor(new Color(12, 4, 35, 90));
            g2.fillRoundRect(cardX + 4, cardY + 12, cardW, cardH, 36, 36);
            g2.setColor(new Color(12, 4, 35, 40));
            g2.fillRoundRect(cardX + 2, cardY + 6, cardW, cardH, 36, 36);

            // Card Body
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 36, 36);

            // 5. Left Column: Text Content
            int leftX = cardX + 50;
            int textY = cardY + cardH / 2 - 40;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g2.setColor(new Color(76, 49, 157));
            g2.drawString("WEBSITE IS", leftX, textY);

            g2.setFont(new Font("Segoe UI Black", Font.BOLD, 36));
            g2.setColor(new Color(45, 20, 112));
            g2.drawString("UNDER CONSTRUCTION", leftX, textY + 45);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            g2.setColor(new Color(121, 108, 153));
            g2.drawString("Estamos trabalhando para trazer uma experiência incrível.", leftX, textY + 90);
            g2.drawString("Em breve, novos recursos, design renovado e muito mais!", leftX, textY + 114);

            // 6. Right Side: Illustration Scene
            int sceneX = cardX + cardW / 2 + 10;
            int sceneY = cardY + 30;
            int sceneW = cardW / 2 - 40;
            int sceneH = cardH - 60;

            drawIllustrationScene(g2, sceneX, sceneY, sceneW, sceneH);

            // 7. Sparkles
            for (Sparkle sp : sparkles) {
                g2.setColor(sp.color);
                g2.fill(new Ellipse2D.Double(sp.x, sp.y, sp.size, sp.size));
            }

            g2.dispose();
        }

        private void drawGear(Graphics2D g2, double cx, double cy, double r, float angle, Color color) {
            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(angle);
            g2.setColor(color);
            int teeth = 8;
            for (int i = 0; i < teeth; i++) {
                g2.rotate(Math.PI * 2 / teeth);
                g2.fillRoundRect((int) (-r * 0.18), (int) (-r * 1.15), (int) (r * 0.36), (int) (r * 0.4), 6, 6);
            }
            g2.fill(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
            g2.setComposite(AlphaComposite.Clear);
            g2.fill(new Ellipse2D.Double(-r * 0.45, -r * 0.45, r * 0.9, r * 0.9));
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setTransform(old);
        }

        private void drawIllustrationScene(Graphics2D g2, int sx, int sy, int sw, int sh) {
            // Ground Shadow Base
            g2.setColor(new Color(228, 224, 253, 180));
            g2.fill(new Ellipse2D.Double(sx + sw * 0.1, sy + sh * 0.88, sw * 0.85, 18));

            // A. Tablet Wireframe Mockup
            int tabW = (int) (sw * 0.58);
            int tabH = (int) (sh * 0.72);
            int tabX = sx + (int) (sw * 0.26);
            int tabY = sy + (int) (sh * 0.12);

            // Tablet frame
            g2.setColor(new Color(115, 93, 232));
            g2.fillRoundRect(tabX - 5, tabY - 5, tabW + 10, tabH + 10, 20, 20);
            g2.setColor(new Color(244, 241, 253));
            g2.fillRoundRect(tabX, tabY, tabW, tabH, 16, 16);

            // Tablet speaker bar
            g2.setColor(new Color(185, 173, 238));
            g2.fillRoundRect(tabX + (tabW - 30) / 2, tabY + 6, 30, 4, 2, 2);

            // Tablet wireframe cards
            int innerX = tabX + 12;
            int innerY = tabY + 18;
            int colW = (tabW - 30) / 2;

            // Left mini card
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(innerX, innerY, colW, (int) (tabH * 0.75), 8, 8);
            g2.setColor(new Color(232, 227, 250));
            g2.fillRoundRect(innerX + 6, innerY + 6, colW - 12, 50, 6, 6);

            // Right mini card with lines
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(innerX + colW + 8, innerY, colW, (int) (tabH * 0.75), 8, 8);
            g2.setColor(new Color(224, 219, 247));
            for (int l = 0; l < 4; l++) {
                int lw = (int) ((colW - 16) * (0.9 - l * 0.12));
                g2.fillRoundRect(innerX + colW + 14, innerY + 12 + l * 12, lw, 5, 2, 2);
            }

            // Fresh paint overlay animation on tablet
            double paintProg = 0.3 + 0.3 * Math.sin(time * 1.5);
            int paintH = (int) (tabH * paintProg);
            g2.setColor(new Color(108, 92, 231, 60));
            g2.fillRoundRect(tabX + 4, tabY + 15, tabW - 8, paintH, 10, 10);
            g2.setColor(new Color(108, 92, 231, 180));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6, 4}, 0));
            g2.drawLine(tabX + 4, tabY + 15 + paintH, tabX + tabW - 4, tabY + 15 + paintH);
            g2.setStroke(new BasicStroke(1f));

            // B. Floating Wireframe Preview Cards
            double float1 = Math.sin(time * 2.2) * 8;
            double float2 = Math.cos(time * 1.8) * 9;

            // Left floating bubble
            int f1X = sx + 20;
            int f1Y = (int) (sy + sh * 0.3 + float1);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(f1X, f1Y, 70, 38, 8, 8);
            g2.setColor(new Color(217, 210, 245));
            g2.fillRoundRect(f1X + 8, f1Y + 10, 54, 5, 2, 2);
            g2.fillRoundRect(f1X + 8, f1Y + 20, 35, 5, 2, 2);

            // Right floating bubble
            int f2X = sx + sw - 85;
            int f2Y = (int) (sy + sh * 0.18 + float2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(f2X, f2Y, 75, 44, 8, 8);
            g2.setColor(new Color(217, 210, 245));
            g2.fillRoundRect(f2X + 8, f2Y + 10, 58, 5, 2, 2);
            g2.fillRoundRect(f2X + 8, f2Y + 20, 58, 5, 2, 2);
            g2.fillRoundRect(f2X + 8, f2Y + 30, 36, 5, 2, 2);

            // C. Ladder
            int ladBaseX = sx + (int) (sw * 0.28);
            int ladTopX = sx + (int) (sw * 0.42);
            int ladBaseY = (int) (sy + sh * 0.9);
            int ladTopY = (int) (sy + sh * 0.35);

            g2.setColor(new Color(55, 32, 117));
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Rails
            g2.drawLine(ladBaseX, ladBaseY, ladTopX, ladTopY);
            g2.drawLine(ladBaseX + 38, ladBaseY, ladTopX + 32, ladTopY);
            // Rungs
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int r = 1; r <= 5; r++) {
                double t = r / 6.0;
                int rx1 = (int) (ladBaseX + t * (ladTopX - ladBaseX));
                int ry1 = (int) (ladBaseY + t * (ladTopY - ladBaseY));
                int rx2 = (int) ((ladBaseX + 38) + t * (ladTopX + 32 - (ladBaseX + 38)));
                int ry2 = ry1;
                g2.drawLine(rx1, ry1, rx2, ry2);
            }

            // D. Worker 1 on Ladder (Painting)
            double ladderBob = Math.sin(time * 2.5) * 2;
            int w1X = ladTopX + 5;
            int w1Y = ladTopY + 50 + (int) ladderBob;

            // Worker 1 Legs
            g2.setColor(new Color(243, 156, 18));
            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w1X - 5, w1Y + 30, w1X - 10, w1Y + 65);
            g2.drawLine(w1X + 12, w1Y + 30, w1X + 12, w1Y + 55);

            // Worker 1 Shoes
            g2.setColor(new Color(44, 62, 80));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w1X - 14, w1Y + 68, w1X - 4, w1Y + 68);
            g2.drawLine(w1X + 8, w1Y + 58, w1X + 18, w1Y + 58);

            // Worker 1 Body / Overalls
            g2.setColor(new Color(243, 156, 18));
            g2.fillRoundRect(w1X - 10, w1Y, 24, 38, 8, 8);
            // Purple Shirt
            g2.setColor(new Color(95, 39, 205));
            g2.fillRoundRect(w1X - 10, w1Y, 24, 12, 4, 4);

            // Head
            g2.setColor(new Color(255, 234, 167));
            g2.fillOval(w1X - 3, w1Y - 22, 18, 18);
            // Yellow Hard Hat
            g2.setColor(new Color(255, 195, 18));
            g2.fillArc(w1X - 6, w1Y - 26, 24, 20, 0, 180);
            g2.fillRect(w1X - 7, w1Y - 18, 26, 3);

            // Worker 1 Animated Painting Arm & Roller
            double armAngle = Math.sin(time * 3) * 0.35;
            AffineTransform armTrans = g2.getTransform();
            g2.translate(w1X + 10, w1Y + 6);
            g2.rotate(armAngle);

            g2.setColor(new Color(255, 234, 167));
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(0, 0, 18, -25);
            g2.drawLine(18, -25, 28, -55);

            // Roller Handle & Head
            g2.setColor(new Color(50, 50, 50));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(28, -55, 32, -75);
            g2.setColor(new Color(108, 92, 231));
            g2.fillRoundRect(15, -88, 32, 12, 4, 4);

            g2.setTransform(armTrans);

            // Paint particle drips
            if (rand.nextDouble() < 0.25) {
                sparkles.add(new Sparkle(w1X + 35 + rand.nextInt(10), w1Y - 60, rand.nextDouble() * 0.5 - 0.25, rand.nextDouble() * 2 + 1, new Color(108, 92, 231)));
            }

            // E. Worker 2 on Ground (Carrying Bucket)
            double groundBob = Math.sin(time * 2.8) * 3;
            int w2X = sx + (int) (sw * 0.72);
            int w2Y = (int) (sy + sh * 0.62 + groundBob);

            // Worker 2 Legs
            g2.setColor(new Color(243, 156, 18));
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w2X - 5, w2Y + 40, w2X - 8, w2Y + 95);
            g2.drawLine(w2X + 14, w2Y + 40, w2X + 18, w2Y + 95);

            // Worker 2 Shoes
            g2.setColor(new Color(44, 62, 80));
            g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w2X - 14, w2Y + 98, w2X - 2, w2Y + 98);
            g2.drawLine(w2X + 12, w2Y + 98, w2X + 24, w2Y + 98);

            // Worker 2 Body / Overalls
            g2.setColor(new Color(243, 156, 18));
            g2.fillRoundRect(w2X - 10, w2Y + 5, 26, 42, 8, 8);
            // Purple Shirt
            g2.setColor(new Color(95, 39, 205));
            g2.fillRoundRect(w2X - 10, w2Y + 5, 26, 14, 4, 4);

            // Head
            g2.setColor(new Color(255, 234, 167));
            g2.fillOval(w2X - 3, w2Y - 18, 18, 18);
            // Yellow Hard Hat
            g2.setColor(new Color(255, 195, 18));
            g2.fillArc(w2X - 6, w2Y - 22, 24, 20, 0, 180);
            g2.fillRect(w2X - 7, w2Y - 14, 26, 3);

            // Worker 2 Arm carrying paint bucket
            double bucketSway = Math.sin(time * 2.8) * 0.2;
            AffineTransform bucketTrans = g2.getTransform();
            g2.translate(w2X - 8, w2Y + 14);
            g2.rotate(bucketSway);

            g2.setColor(new Color(255, 234, 167));
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(0, 0, -10, 24);

            // Paint Bucket
            int bX = -20;
            int bY = 26;
            g2.setColor(new Color(95, 39, 205));
            int[] bpx = {bX + 3, bX + 6, bX + 20, bX + 23};
            int[] bpy = {bY + 6, bY + 24, bY + 24, bY + 6};
            g2.fillPolygon(bpx, bpy, 4);
            g2.setColor(new Color(125, 95, 255));
            g2.fillOval(bX + 4, bY + 4, 18, 5);
            g2.setColor(new Color(150, 150, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(bX + 4, bY - 3, 18, 14, 0, 180);

            g2.setTransform(bucketTrans);

            // F. Safety Cones
            drawCone(g2, sx + (int) (sw * 0.18), (int) (sy + sh * 0.78));
            drawCone(g2, sx + (int) (sw * 0.82), (int) (sy + sh * 0.78));

            // G. Construction Barrier
            int barX = sx + (int) (sw * 0.86);
            int barY = (int) (sy + sh * 0.72);
            drawBarrier(g2, barX, barY);
        }

        private void drawCone(Graphics2D g2, int cx, int cy) {
            // Base
            g2.setColor(new Color(255, 82, 82));
            g2.fillRoundRect(cx - 16, cy + 30, 32, 6, 2, 2);

            // Cone Body
            int[] cpx = {cx, cx - 14, cx + 14};
            int[] cpy = {cy, cy + 30, cy + 30};
            g2.setColor(new Color(255, 121, 63));
            g2.fillPolygon(cpx, cpy, 3);

            // White reflective stripe
            int[] wpx = {cx - 6, cx - 11, cx + 11, cx + 6};
            int[] wpy = {cy + 12, cy + 22, cy + 22, cy + 12};
            g2.setColor(Color.WHITE);
            g2.fillPolygon(wpx, wpy, 4);

            // Glowing light top
            float glow = (float) (0.4 + 0.6 * Math.abs(Math.sin(time * 3)));
            g2.setColor(new Color(1f, 0.95f, 0.4f, glow));
            g2.fill(new Ellipse2D.Double(cx - 3, cy - 2, 6, 6));
        }

        private void drawBarrier(Graphics2D g2, int bx, int by) {
            // Legs
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(bx + 4, by + 12, 4, 38);
            g2.fillRect(bx + 48, by + 12, 4, 38);
            g2.setColor(new Color(44, 62, 80));
            g2.fillRoundRect(bx, by + 48, 12, 5, 2, 2);
            g2.fillRoundRect(bx + 44, by + 48, 12, 5, 2, 2);

            // Yellow Board with Purple Stripes
            g2.setColor(new Color(241, 196, 15));
            g2.fillRoundRect(bx - 2, by, 60, 22, 4, 4);

            Shape oldClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Double(bx - 2, by, 60, 22, 4, 4));

            g2.setColor(new Color(52, 31, 151));
            g2.setStroke(new BasicStroke(8f));
            for (int s = -20; s < 70; s += 16) {
                g2.drawLine(bx + s, by - 5, bx + s + 18, by + 28);
            }

            g2.setClip(oldClip);
            g2.setColor(new Color(44, 62, 80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(bx - 2, by, 60, 22, 4, 4);
        }
    }

    static class Star {
        double rx, ry, size, phase;
        Star(double rx, double ry, double size, double phase) {
            this.rx = rx; this.ry = ry; this.size = size; this.phase = phase;
        }
    }

    static class Sparkle {
        double x, y, vx, vy;
        double size = 5;
        int life = 50;
        int maxLife = 50;
        Color color;

        Sparkle(double x, double y, double vx, double vy, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.12; // gravity
            life--;
            size = Math.max(1, 5 * ((double) life / maxLife));
        }

        boolean isDead() {
            return life <= 0;
        }
    }
}
