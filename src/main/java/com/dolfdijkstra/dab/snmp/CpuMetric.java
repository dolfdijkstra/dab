package com.dolfdijkstra.dab.snmp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.dolfdijkstra.dab.Dimension;
import com.dolfdijkstra.dab.snmp.DS.Type;

import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraphDef;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class CpuMetric extends AbstractMetric {

    private String[][] map = new String[][] { { "ssCpuRawUser", "1.3.6.1.4.1.2021.11.50.0" },
            { "ssCpuRawNice", "1.3.6.1.4.1.2021.11.51.0" }, { "ssCpuRawSystem", "1.3.6.1.4.1.2021.11.52.0" },
            { "ssCpuRawIdle", "1.3.6.1.4.1.2021.11.53.0" }, { "ssCpuRawWait", "1.3.6.1.4.1.2021.11.54.0" },
            { "ssCpuRawKernel", "1.3.6.1.4.1.2021.11.55.0" }, { "ssCpuRawInterrupt", "1.3.6.1.4.1.2021.11.56.0" },
            { "ssCpuRawSoftIRQ", "1.3.6.1.4.1.2021.11.61.0" } };
    private Color[] color = new Color[] { new Color(255, 0, 0), new Color(255, 140, 0), new Color(240, 128, 128),
            new Color(255, 160, 122), new Color(245, 245, 220), new Color(0, 191, 255),
            new Color(0, 191, 255).brighter() };
    private String[] legend = new String[] { "User", "Nice", "System", "Idle", "Wait", "Kernel", "HW Int", "SW Int" };

    private long[] prev;

    private String rrdPath;

    public CpuMetric(File rrdPath) throws IOException, RrdException {
        super(new SnmpRRD(rrdPath, 5));
        this.rrdPath = rrdPath.getAbsolutePath();
        pdu = Util.toPDU(map);
    }

    @Override
    protected NVP[] toNVP(VariableBinding[] bindings) {
        long[] cpuValues = toCpuValues(bindings);
        System.out.println(Arrays.toString(cpuValues));
        double[] pct = new double[0];
        if (prev != null && prev.length == cpuValues.length) {

            pct = precentage(prev, cpuValues);
            // for (int i = 0; i < pct.length; i++) {
            // System.out.println(map[i][0] + ": " + pct[i]);
            // }
        }
        prev = cpuValues;
        return toNVP(pct);
    }

    /**
     * @param bindings
     * @return
     */
    private long[] toCpuValues(VariableBinding[] bindings) {
        long[] cpuValues = new long[8];
        for (VariableBinding vb : bindings) {
            Variable v = vb.getVariable();
            int i = getNameId(vb.getOid().toString());
            if (i != -1 && i < cpuValues.length) {
                cpuValues[i] = v.toLong();
            }

            /*
             * System.out.println("Snmp Get Response = " + vb.getOid() + ": " +
             * v.getSyntaxString() + ": " + v.toString());
             */
        }
        return cpuValues;
    }

    protected NVP[] toNVP(double[] pct) {
        NVP[] nvp = new NVP[pct.length];
        for (int i = 0; i < pct.length; i++) {
            nvp[i] = new NVP(map[i][0], pct[i]);
        }
        return nvp;
    }

    private double[] precentage(long[] a, long[] b) {
        long[] delta = delta(a, b);
        System.out.println(Arrays.toString(delta));
        double total = add(b) - add(a);
        if (total == 0) {
            System.err.println("Error: No new values " + new Date());
            return new double[0];
        }
        double[] pct = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            pct[i] = total == 0 ? 0 : (delta[i] * 100D) / total;
        }
        System.out.println(Arrays.toString(pct));
        return pct;
    }

    private long[] delta(long[] a, long[] b) {
        long[] d = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            d[i] = b[i] - a[i];
            if (d[i] < 0) {
                d[i] = 0;
            }
        }
        return d;
    }

    private long add(long[] a) {
        long d = 0L;
        for (int i = 0; i < a.length; i++) {
            d += a[i];
        }
        return d;
    }

    private int getNameId(String oid) {
        for (int i = 0; i < map.length; i++) {
            if (map[i][1].equals(oid)) {
                return i;
            }
        }
        return -1;
    }

    public void init(SnmpGet snmp) throws Exception {
        VariableBinding[] bindings = snmp.getVariable(pdu);
        prev = toCpuValues(bindings);
        DS[] ds = new DS[map.length];
        for (int i = 0; i < map.length; i++) {
            ds[i] = new DS(map[i][0], Type.GAUGE);
        }
        collector.createRrdFileIfNecessary(ds);

    }

    @Override
    public BufferedImage createGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException {
        final RrdGraphDef graphDef = new RrdGraphDef();

        graphDef.datasource(map[0][0], rrdPath, map[0][0], "AVERAGE");
        graphDef.datasource(map[1][0], rrdPath, map[1][0], "AVERAGE");
        graphDef.datasource(map[2][0], rrdPath, map[2][0], "AVERAGE");
        // graphDef.datasource(map[3][0], rrdPath, map[3][0], "AVERAGE");
        graphDef.datasource(map[4][0], rrdPath, map[4][0], "AVERAGE");
        graphDef.datasource(map[5][0], rrdPath, map[5][0], "AVERAGE");
        graphDef.datasource(map[6][0], rrdPath, map[6][0], "AVERAGE");
        graphDef.datasource(map[7][0], rrdPath, map[7][0], "AVERAGE");
        color[0] = Color.red;
        color[1] = lighter(Color.red, 0.8f);
        color[2] = Color.green;
        color[3] = lighter(color[2], 0.8f);
        color[4] = lighter(color[3], 0.8f);
        color[2] = Color.red.darker();
        color[3] = color[2].darker();
        color[4] = color[3].darker();

        graphDef.area(map[0][0], color[0], legend[0]);
        graphDef.stack(map[1][0], color[1], legend[1]);
        graphDef.stack(map[2][0], color[2], legend[2]);
        // graphDef.stack(map[3][0], Color.black, legend[3]);
        graphDef.stack(map[4][0], color[3], legend[4]);
        graphDef.stack(map[5][0], color[4], legend[5]);
        graphDef.stack(map[6][0], color[5], legend[6]);
        graphDef.stack(map[7][0], color[6], legend[7]);

        graphDef.setVerticalLabel("CPU %");
        if (title != null)
            graphDef.setTitle(title);
        return produceImage(startTime, endTime, dimension, graphDef);
    }

    static Color lighter(Color o, float factor) {
        // = 0.70F;
        int a = (int) ((factor * o.getRed()) + (255 * (1 - factor)));
        int b = (int) ((factor * o.getGreen()) + (255 * (1 - factor)));
        int c = (int) ((factor * o.getBlue()) + (255 * (1 - factor)));
        return new Color(a, b, c);
    }

    static Color darker(Color o, float factor) {
        if (factor < 0 || factor > 1)
            throw new IllegalArgumentException("Factor is illegal: " + factor);
        return new Color(Math.max((int) (o.getRed() * factor), 0), Math.max((int) (o.getGreen() * factor), 0),
                Math.max((int) (o.getBlue() * factor), 0));
    }

    public static void main(String[] a) throws IOException, RrdException {

        Color color = Color.red;
        System.out.println(color.toString());
        System.out.println(color.brighter().toString());
        System.out.println(lighter(color, 0.7f).toString());
        System.out.println(color.darker().toString());
        System.out.println(color.darker().brighter().toString());

        class AWTRectangle extends JPanel {

            private static final long serialVersionUID = 1L;
            int s = 7;

            public void paint(Graphics g) {
                super.paint(g);
                // s = Math.max(s++, 5);
                System.out.println("paint");
                float factor = 1 - (1F / (s / 2));

                int w = getWidth() / 3;
                int h = getHeight() / s;

                drawColorShadeAndTints(g, factor, createColorPallette(factor, Color.red), 0, 0, w, h);
                drawColorShadeAndTints(g, factor, createColorPallette(factor, Color.orange), w, 0, w, h);
                drawColorShadeAndTints(g, factor, createColorPallette(factor, Color.blue), 2 * w, 0, w, h);

            }

            /**
             * @param g
             * @param factor
             * @param w
             * @param h
             * @param x
             * @param y
             */
            private void drawColorShadeAndTints(Graphics g, float factor, Color[] c, int x, int y, int w, int h) {

                for (int i = 0; i < c.length; i++) {
                    g.setColor(c[i]);
                    g.fillRect(x, y, w, h);
                    g.setColor(getBackground());
                    g.drawString(Integer.toString(i) + " " + c[i].toString(), x + 20, y + (h / 2));
                    y = y + h + 1;
                }
            }

            /**
             * @param factor
             * @param color
             * @return
             */
            private Color[] createColorPallette(float factor, Color color) {
                Color[] c = new Color[s];
                c[s / 2] = color;
                for (int i = s / 2 - 1; i >= 0; i--) {
                    c[i] = lighter(c[i + 1], factor);
                }
                for (int i = s / 2 + 1; i < s; i++) {
                    c[i] = darker(c[i - 1], factor);
                }
                return c;
            }

        }

        JFrame frame = new JFrame();
        frame.getContentPane().add(new AWTRectangle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        System.out.println("before size");
        frame.setSize(300, 300);
        frame.setBackground(Color.white);

        System.out.println("after size");
        frame.setVisible(true);

        // CpuMetric task = new CpuMetric("cpuRaw-2011-12-24T044634.rdd");
        // RrdDbPool rrdPool = RrdDbPool.getInstance();
        // RrdDb rrd = rrdPool.requestRrdDb("cpuRaw-2011-12-24T044634.rdd");
        //
        // long startTime = rrd.getLastUpdateTime() - 31;
        // long endTime = rrd.getLastUpdateTime() + 1;
        // BufferedImage bi = task.createGraph(startTime, endTime, null, new
        // Dimension(800, 300));
        // ImageIO.write(bi, "png", new File("test.png"));

    }
}
