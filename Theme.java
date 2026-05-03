import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Theme & custom-painted Swing components for CraftLaunch v5.0 */
public class Theme {

    // ── Emerald Cave palette ─────────────────────────────────
    public static final Color BG_DEEP    = new Color(8,  10, 14);   // void
    public static final Color BG_PANEL   = new Color(14, 18, 24);   // panel
    public static final Color BG_CARD    = new Color(20, 26, 34);   // card
    public static final Color BG_INPUT   = new Color(10, 14, 20);   // input
    public static final Color BG_HOVER   = new Color(28, 36, 48);   // hover

    public static final Color EMERALD       = new Color(80, 220, 130);
    public static final Color EMERALD_BRIGHT= new Color(120, 255, 170);
    public static final Color EMERALD_DEEP  = new Color(28, 90, 50);
    public static final Color EMERALD_GLOW  = new Color(80, 220, 130, 55);

    public static final Color DIAMOND     = new Color(120, 200, 230);
    public static final Color DIAMOND_DEEP = new Color(50, 110, 145);

    public static final Color GOLD     = new Color(252, 211, 77);
    public static final Color RUBY     = new Color(248, 113, 113);
    public static final Color AMETHYST = new Color(167, 139, 250);

    public static final Color TEXT     = new Color(232, 240, 252);
    public static final Color TEXT_DIM = new Color(140, 160, 185);
    public static final Color MUTED    = new Color(85,  100, 122);
    public static final Color BORDER   = new Color(36,  46,  62);
    public static final Color BORDER_BR= new Color(56,  72,  92);

    // ── Fonts ────────────────────────────────────────────────
    public static final Font  TITLE   = new Font("Monospaced", Font.BOLD,  22);
    public static final Font  HEADING = new Font("Monospaced", Font.BOLD,  14);
    public static final Font  BODY    = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font  BODY_B  = new Font("Monospaced", Font.BOLD,  12);
    public static final Font  SMALL   = new Font("Monospaced", Font.PLAIN, 10);
    public static final Font  TINY    = new Font("Monospaced", Font.PLAIN, 9);
    public static final Font  LABEL   = new Font("Monospaced", Font.BOLD,  10);

    // ════════════════════════════════════════════════════════
    //  ANIMATED STARFIELD BACKGROUND
    // ════════════════════════════════════════════════════════
    public static class StarfieldPanel extends JPanel {
        private final Random rng = new Random(42);
        private final float[] xs, ys, sp, sz;
        private final int N = 60;
        private float t = 0;
        private final javax.swing.Timer timer;

        public StarfieldPanel() {
            setBackground(BG_DEEP);
            xs = new float[N]; ys = new float[N]; sp = new float[N]; sz = new float[N];
            for (int i = 0; i < N; i++) {
                xs[i] = rng.nextFloat();
                ys[i] = rng.nextFloat();
                sp[i] = 0.0001f + rng.nextFloat() * 0.0005f;
                sz[i] = 1f + rng.nextFloat() * 2.5f;
            }
            timer = new javax.swing.Timer(60, e -> {
                t += 1;
                for (int i = 0; i < N; i++) {
                    ys[i] += sp[i];
                    if (ys[i] > 1f) { ys[i] = 0f; xs[i] = rng.nextFloat(); }
                }
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Vignette glow at top
            RadialGradientPaint vignette = new RadialGradientPaint(
                w / 2f, -h * 0.2f, w * 0.8f,
                new float[]{0f, 1f},
                new Color[]{new Color(80, 220, 130, 22), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);

            // Stars
            for (int i = 0; i < N; i++) {
                float x = xs[i] * w;
                float y = ys[i] * h;
                float twinkle = (float)(0.4 + 0.6 * Math.sin((t + i * 7) * 0.05));
                int alpha = Math.max(0, Math.min(255, (int)(80 * Math.abs(twinkle))));
                g2.setColor(new Color(232, 240, 252, alpha));
                float s = sz[i];
                g2.fill(new Ellipse2D.Float(x - s/2, y - s/2, s, s));
            }
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CARD (rounded with subtle inner glow)
    // ════════════════════════════════════════════════════════
    public static class Card extends JPanel {
        private final boolean glow;
        public Card() { this(false); }
        public Card(boolean glow) {
            this.glow = glow;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), r = 14;

            if (glow) {
                for (int i = 8; i > 0; i--) {
                    g2.setColor(new Color(80, 220, 130, 4));
                    g2.fill(new RoundRectangle2D.Float(-i, -i, w + 2*i, h + 2*i, r + i, r + i));
                }
            }

            // Card body with subtle gradient
            g2.setPaint(new GradientPaint(
                0, 0, BG_CARD,
                0, h, new Color(BG_CARD.getRed() - 4, BG_CARD.getGreen() - 4, BG_CARD.getBlue() - 4)
            ));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            // Inner highlight (top edge)
            g2.setPaint(new GradientPaint(
                0, 0, new Color(255, 255, 255, 14),
                0, 30, new Color(255, 255, 255, 0)
            ));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, 30, r, r));

            // Border
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, r, r));

            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  PIXEL DIAMOND ICON
    // ════════════════════════════════════════════════════════
    public static class DiamondIcon extends JComponent {
        private final int size;
        private float phase = 0f;
        public DiamondIcon(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
            new javax.swing.Timer(50, e -> { phase += 0.05f; repaint(); }).start();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float pulse = (float)(0.6 + 0.4 * Math.sin(phase));
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int s = size / 2 - 2;

            // Outer glow
            for (int i = 6; i > 0; i--) {
                int alpha = Math.max(0, Math.min(255, (int)(15 * Math.abs(pulse) / i)));
                g2.setColor(new Color(80, 220, 130, alpha));
                g2.fillPolygon(
                    new int[]{cx, cx + s + i, cx, cx - s - i},
                    new int[]{cy - s - i, cy, cy + s + i, cy},
                    4
                );
            }

            // Body
            g2.setPaint(new GradientPaint(
                cx - s, cy - s, EMERALD_BRIGHT,
                cx + s, cy + s, EMERALD_DEEP
            ));
            g2.fillPolygon(
                new int[]{cx, cx + s, cx, cx - s},
                new int[]{cy - s, cy, cy + s, cy},
                4
            );

            // Inner highlight
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillPolygon(
                new int[]{cx, cx + s/2, cx, cx - s/2},
                new int[]{cy - s, cy - s/3, cy - s/4, cy - s/3},
                4
            );

            // Outline
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(EMERALD_BRIGHT);
            g2.drawPolygon(
                new int[]{cx, cx + s, cx, cx - s},
                new int[]{cy - s, cy, cy + s, cy},
                4
            );
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  POLISHED BUTTON
    // ════════════════════════════════════════════════════════
    public static class PolishButton extends JButton {
        private final Color baseColor, textColor;
        private final boolean primary;
        private float hover = 0f;
        private javax.swing.Timer hoverTimer;

        public PolishButton(String text, Color baseColor, Color textColor, boolean primary) {
            super(text);
            this.baseColor = baseColor;
            this.textColor = textColor;
            this.primary = primary;
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(textColor);
            setFont(BODY_B);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { startHover(true); }
                public void mouseExited (java.awt.event.MouseEvent e) { startHover(false); }
            });
        }

        private void startHover(boolean in) {
            if (hoverTimer != null) hoverTimer.stop();
            hoverTimer = new javax.swing.Timer(16, ev -> {
                hover += in ? 0.12f : -0.12f;
                if (hover > 1) { hover = 1; ((javax.swing.Timer)ev.getSource()).stop(); }
                if (hover < 0) { hover = 0; ((javax.swing.Timer)ev.getSource()).stop(); }
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), r = 8;

            Color top, bot;
            if (primary) {
                top = lerp(baseColor.brighter(), baseColor.brighter().brighter(), hover);
                bot = lerp(baseColor.darker(),    baseColor,                       hover);
            } else {
                top = lerp(baseColor, BG_HOVER, hover);
                bot = lerp(baseColor.darker(), baseColor, hover);
            }

            if (primary && hover > 0.1f) {
                int glow = Math.max(0, Math.min(255, (int)(40 * hover)));
                for (int i = 4; i > 0; i--) {
                    int a = Math.max(0, Math.min(255, glow / i));
                    g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), a));
                    g2.fill(new RoundRectangle2D.Float(-i, -i, w + 2*i, h + 2*i, r + i, r + i));
                }
            }

            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            // Top sheen
            g2.setPaint(new GradientPaint(0, 0,
                new Color(255, 255, 255, primary ? 50 : 18),
                0, h / 2,
                new Color(255, 255, 255, 0)));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h / 2, r, r));

            if (!primary) {
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(BORDER_BR);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, r, r));
            }

            super.paintComponent(g2);
            g2.dispose();
        }

        private static Color lerp(Color a, Color b, float t) {
            t = Math.max(0, Math.min(1, t));
            return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
            );
        }
    }

    // ════════════════════════════════════════════════════════
    //  HERO LAUNCH BUTTON (animated, glowing)
    // ════════════════════════════════════════════════════════
    public static class LaunchButton extends JButton {
        private float phase = 0f;
        private float hover = 0f;
        private javax.swing.Timer hoverTimer;

        public LaunchButton() {
            super("▶  LAUNCH MINECRAFT");
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(new Color(8, 18, 12));
            setFont(new Font("Monospaced", Font.BOLD, 15));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(16, 30, 16, 30));

            new javax.swing.Timer(40, e -> { phase += 0.08f; repaint(); }).start();
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { startHover(true); }
                public void mouseExited (java.awt.event.MouseEvent e) { startHover(false); }
            });
        }

        private void startHover(boolean in) {
            if (hoverTimer != null) hoverTimer.stop();
            hoverTimer = new javax.swing.Timer(16, ev -> {
                hover += in ? 0.1f : -0.1f;
                if (hover > 1) { hover = 1; ((javax.swing.Timer)ev.getSource()).stop(); }
                if (hover < 0) { hover = 0; ((javax.swing.Timer)ev.getSource()).stop(); }
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), r = 12;

            float pulse = (float)(0.5 + 0.5 * Math.sin(phase));

            // Outer glow
            int glowAlpha = (int)(40 + 60 * hover + 20 * pulse);
            glowAlpha = Math.max(0, Math.min(255, glowAlpha));
            if (!isEnabled()) glowAlpha = 0;
            for (int i = 12; i > 0; i--) {
                int a = Math.max(0, Math.min(255, glowAlpha / (i + 2)));
                g2.setColor(new Color(80, 220, 130, a));
                g2.fill(new RoundRectangle2D.Float(-i, -i, w + 2*i, h + 2*i, r + i, r + i));
            }

            Color top, bot;
            if (isEnabled()) {
                top = new Color(140, 255, 180);
                bot = new Color(50, 170, 90);
            } else {
                top = new Color(60, 70, 80);
                bot = new Color(40, 50, 60);
            }

            // Body
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            // Top sheen
            g2.setPaint(new GradientPaint(0, 0,
                new Color(255, 255, 255, 80),
                0, h / 2,
                new Color(255, 255, 255, 0)));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h / 2, r, r));

            // Bottom shadow
            g2.setPaint(new GradientPaint(0, h - 8,
                new Color(0, 0, 0, 0),
                0, h,
                new Color(0, 0, 0, 60)));
            g2.fill(new RoundRectangle2D.Float(0, h - 8, w, 8, r, r));

            // Border
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(isEnabled() ? new Color(180, 255, 200) : new Color(100, 110, 120));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, r, r));

            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  GRADIENT HEADER
    // ════════════════════════════════════════════════════════
    public static class HeaderPanel extends JPanel {
        public HeaderPanel() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Base gradient
            g2.setPaint(new GradientPaint(0, 0, BG_PANEL, 0, h, BG_DEEP));
            g2.fillRect(0, 0, w, h);

            // Emerald glow blob
            RadialGradientPaint blob = new RadialGradientPaint(
                w * 0.15f, h / 2f, w * 0.4f,
                new float[]{0f, 1f},
                new Color[]{new Color(80, 220, 130, 25), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(blob);
            g2.fillRect(0, 0, w, h);

            // Bottom border
            g2.setColor(BORDER);
            g2.fillRect(0, h - 1, w, 1);

            // Bottom accent line
            g2.setPaint(new GradientPaint(
                0, h - 2, new Color(80, 220, 130, 0),
                w/2f, h - 2, new Color(80, 220, 130, 100),
                false));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(0, h - 1, w, h - 1);
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CUSTOM SCROLLBAR
    // ════════════════════════════════════════════════════════
    public static class EmeraldScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            this.thumbColor      = new Color(80, 220, 130, 80);
            this.thumbHighlightColor = EMERALD;
            this.thumbDarkShadowColor = EMERALD_DEEP;
            this.thumbLightShadowColor = EMERALD;
            this.trackColor      = BG_DEEP;
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(BG_DEEP);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(80, 220, 130, 100));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CUSTOM PROGRESS BAR
    // ════════════════════════════════════════════════════════
    public static class EmeraldProgressBarUI extends BasicProgressBarUI {
        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            JProgressBar bar = (JProgressBar) c;
            int w = bar.getWidth(), h = bar.getHeight();

            // Track
            g2.setColor(BG_DEEP);
            g2.fillRoundRect(0, 0, w, h, 6, 6);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

            // Filled portion
            int amountFull = getAmountFull(c.getInsets(), w - 2, h - 2);
            if (amountFull > 0) {
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(120, 255, 170),
                    0, h, new Color(50, 170, 90)
                ));
                g2.fillRoundRect(1, 1, amountFull, h - 2, 5, 5);

                // Inner sheen
                g2.setPaint(new GradientPaint(
                    0, 1, new Color(255, 255, 255, 60),
                    0, h / 2, new Color(255, 255, 255, 0)
                ));
                g2.fillRoundRect(1, 1, amountFull, h / 2, 5, 5);
            }

            // String
            if (bar.isStringPainted()) {
                g2.setColor(TEXT);
                g2.setFont(SMALL);
                FontMetrics fm = g2.getFontMetrics();
                String s = bar.getString();
                int sw = fm.stringWidth(s);
                g2.drawString(s, (w - sw) / 2, h / 2 + fm.getAscent() / 2 - 2);
            }
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CUSTOM SLIDER
    // ════════════════════════════════════════════════════════
    public static class EmeraldSliderUI extends BasicSliderUI {
        public EmeraldSliderUI(JSlider s) { super(s); }
        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle t = trackRect;
            int cy = t.y + t.height / 2 - 3;
            // Track background
            g2.setColor(BG_DEEP);
            g2.fillRoundRect(t.x, cy, t.width, 6, 4, 4);
            g2.setColor(BORDER);
            g2.drawRoundRect(t.x, cy, t.width - 1, 5, 4, 4);
            // Filled portion
            int filled = thumbRect.x + thumbRect.width / 2 - t.x;
            if (filled > 0) {
                g2.setPaint(new GradientPaint(
                    t.x, cy, EMERALD_BRIGHT,
                    t.x + filled, cy, EMERALD
                ));
                g2.fillRoundRect(t.x, cy, filled, 6, 4, 4);
            }
            g2.dispose();
        }
        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle r = thumbRect;
            int cx = r.x + r.width / 2;
            int cy = r.y + r.height / 2;
            int rad = 8;

            // Glow
            for (int i = 6; i > 0; i--) {
                g2.setColor(new Color(80, 220, 130, 25 / i));
                g2.fillOval(cx - rad - i, cy - rad - i, (rad + i) * 2, (rad + i) * 2);
            }
            // Body
            g2.setPaint(new GradientPaint(
                cx - rad, cy - rad, EMERALD_BRIGHT,
                cx + rad, cy + rad, EMERALD_DEEP
            ));
            g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
            // Highlight
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(cx - rad / 2, cy - rad / 2 - 1, rad / 2, rad / 3);
            // Border
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(EMERALD_BRIGHT);
            g2.drawOval(cx - rad, cy - rad, rad * 2 - 1, rad * 2 - 1);
            g2.dispose();
        }
        @Override public void paintFocus(Graphics g) {}
    }

    // ════════════════════════════════════════════════════════
    //  CUSTOM TABBED PANE UI
    // ════════════════════════════════════════════════════════
    public static class EmeraldTabbedPaneUI extends BasicTabbedPaneUI {
        @Override protected Insets getContentBorderInsets(int p) { return new Insets(0, 0, 0, 0); }
        @Override protected Insets getTabAreaInsets(int p) { return new Insets(8, 24, 0, 24); }
        @Override protected Insets getTabInsets(int p, int i) { return new Insets(10, 22, 10, 22); }
        @Override protected int calculateTabAreaHeight(int p, int n, int h) { return 44; }

        @Override
        protected void paintTabBackground(Graphics g, int placement, int idx,
                int x, int y, int w, int h, boolean selected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setPaint(new GradientPaint(0, y, BG_CARD, 0, y + h, BG_PANEL));
                g2.fillRoundRect(x + 2, y + 2, w - 4, h - 2, 8, 8);
                // Top accent
                g2.setColor(EMERALD);
                g2.fillRect(x + 8, y + 2, w - 16, 2);
            } else {
                g2.setColor(BG_DEEP);
                g2.fillRect(x, y, w, h);
            }
            g2.dispose();
        }
        @Override
        protected void paintTabBorder(Graphics g, int placement, int idx,
                int x, int y, int w, int h, boolean selected) {}
        @Override
        protected void paintContentBorder(Graphics g, int placement, int selectedIdx) {}
        @Override
        protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
                int idx, Rectangle iconRect, Rectangle textRect, boolean selected) {}
        @Override
        protected void paintText(Graphics g, int placement, Font font, FontMetrics m,
                int idx, String title, Rectangle textRect, boolean selected) {
            g.setFont(LABEL);
            g.setColor(selected ? EMERALD : TEXT_DIM);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, textRect.x, textRect.y + fm.getAscent());
        }
    }

    // ════════════════════════════════════════════════════════
    //  ROUNDED INPUT BORDER
    // ════════════════════════════════════════════════════════
    public static class InputBorder implements Border {
        private final Color color;
        public InputBorder(Color color) { this.color = color; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, 8, 8));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(8, 12, 8, 12); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    // ════════════════════════════════════════════════════════
    //  ROUNDED TEXT FIELD (paints rounded background)
    // ════════════════════════════════════════════════════════
    public static class RoundedTextField extends JTextField {
        private final Color bg;
        public RoundedTextField(String text, Color bg) {
            super(text);
            this.bg = bg;
            setOpaque(false);
            setBackground(bg);
            setForeground(TEXT);
            setCaretColor(EMERALD);
            setFont(BODY);
            setBorder(BorderFactory.createCompoundBorder(
                new InputBorder(BORDER_BR), new EmptyBorder(0, 4, 0, 4)));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ════════════════════════════════════════════════════════
    //  ROUNDED COMBO BOX UI
    // ════════════════════════════════════════════════════════
    public static class RoundedComboBoxUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.setColor(EMERALD);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(cx - 5, cy - 2, cx, cy + 3);
                    g2.drawLine(cx, cy + 3, cx + 5, cy - 2);
                    g2.dispose();
                }
            };
            b.setBorder(null);
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            return b;
        }
        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_INPUT);
            g2.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), 8, 8));
            g2.setColor(BORDER_BR);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, c.getWidth() - 1, c.getHeight() - 1, 8, 8));
            g2.dispose();
            paintCurrentValue(g, rectangleForCurrentValue(), false);
        }
        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            ListCellRenderer<Object> renderer = comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(
                listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setBackground(BG_INPUT);
            c.setForeground(TEXT);
            c.setFont(BODY);
            // strip the inner border that DefaultListCellRenderer adds
            if (c instanceof JComponent jc) jc.setOpaque(false);
            currentValuePane.paintComponent(g, c, comboBox,
                bounds.x + 4, bounds.y, bounds.width - 4, bounds.height, false);
        }
        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // Drawn by paint()
        }
    }

    // ════════════════════════════════════════════════════════
    //  DIAMOND IMAGE FOR ICON
    // ════════════════════════════════════════════════════════
    public static Image makeAppIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = 32, cy = 32, s = 26;
        g2.setPaint(new GradientPaint(
            cx - s, cy - s, EMERALD_BRIGHT,
            cx + s, cy + s, EMERALD_DEEP));
        g2.fillPolygon(
            new int[]{cx, cx + s, cx, cx - s},
            new int[]{cy - s, cy, cy + s, cy}, 4);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillPolygon(
            new int[]{cx, cx + s/2, cx, cx - s/2},
            new int[]{cy - s, cy - s/3, cy - s/4, cy - s/3}, 4);
        g2.setColor(EMERALD_BRIGHT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(
            new int[]{cx, cx + s, cx, cx - s},
            new int[]{cy - s, cy, cy + s, cy}, 4);
        g2.dispose();
        return img;
    }

    // ════════════════════════════════════════════════════════
    //  NICE LABEL HELPER
    // ════════════════════════════════════════════════════════
    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL);
        l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static JLabel hLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }
}
