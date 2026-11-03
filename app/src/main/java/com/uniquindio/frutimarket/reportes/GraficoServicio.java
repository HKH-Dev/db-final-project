package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.comun.Fmt;
import com.uniquindio.frutimarket.comun.Formato;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

/** Dibuja en el servidor el grafico de un reporte (PNG en base64) para incrustarlo en el PDF. */
@Service
public class GraficoServicio {

    /** Mismos colores que la aplicacion web (static/js/app.js) */
    public static final List<Color> PALETA = List.of(new Color(0x1F7A4D), new Color(0xF28C28), new Color(0xB0306A),
            new Color(0xE8B200), new Color(0x2A9D8F), new Color(0x6C4AB6), new Color(0x8AB833), new Color(0xD1495B),
            new Color(0x3A6EA5), new Color(0x9C6644));
    private static final Color TEXTO = new Color(0x3D4A42);
    private static final Color REJILLA = new Color(0xE3E8E4);

    public String png(Grafico g, List<Map<String, Object>> filas, int ancho, int alto) {
        JFreeChart chart = g.circular() ? torta(g, filas) : categorias(g, filas);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        LegendTitle leyenda = chart.getLegend();
        if (leyenda != null) {
            leyenda.setFrame(org.jfree.chart.block.BlockBorder.NONE);
            leyenda.setItemFont(new Font("SansSerif", Font.PLAIN, 11));
            leyenda.setItemPaint(TEXTO);
            leyenda.setPosition(g.circular() ? RectangleEdge.RIGHT : RectangleEdge.BOTTOM);
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ChartUtils.writeScaledChartAsPNG(os, chart, ancho, alto, 2, 2);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo dibujar el gráfico", e);
        }
    }

    private JFreeChart categorias(Grafico g, List<Map<String, Object>> filas) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map<String, Object> f : filas) {
            String cat = String.valueOf(f.get(g.etiqueta()));
            for (Grafico.Serie s : g.series()) {
                ds.addValue(Fmt.numero(f.get(s.columna())), s.nombre(), cat);
            }
        }
        boolean linea = g.tipo() == Grafico.Tipo.LINEA;
        PlotOrientation o = g.tipo() == Grafico.Tipo.BARRAS_H ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL;
        JFreeChart chart = linea ? ChartFactory.createLineChart(null, null, null, ds, o, g.series().size() > 1, false, false)
                : ChartFactory.createBarChart(null, null, null, ds, o, g.series().size() > 1, false, false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(REJILLA);
        plot.setRangeGridlineStroke(new BasicStroke(1f));
        plot.setDomainGridlinesVisible(false);
        CategoryAxis x = plot.getDomainAxis();
        x.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        x.setTickLabelPaint(TEXTO);
        x.setAxisLineVisible(false);
        x.setTickMarksVisible(false);
        x.setMaximumCategoryLabelLines(2);
        if (o == PlotOrientation.VERTICAL && filas.size() > 8) {
            x.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }
        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        y.setTickLabelPaint(TEXTO);
        y.setAxisLineVisible(false);
        y.setTickMarksVisible(false);
        y.setNumberFormatOverride(formatoEje(g.formato()));
        if (linea) {
            LineAndShapeRenderer r = (LineAndShapeRenderer) plot.getRenderer();
            for (int i = 0; i < g.series().size(); i++) {
                r.setSeriesPaint(i, PALETA.get(i));
                r.setSeriesStroke(i, new BasicStroke(2.5f));
                r.setSeriesShapesVisible(i, true);
            }
        } else {
            BarRenderer r = (BarRenderer) plot.getRenderer();
            r.setBarPainter(new StandardBarPainter());
            r.setShadowVisible(false);
            r.setItemMargin(0.06);
            r.setMaximumBarWidth(0.09);
            for (int i = 0; i < g.series().size(); i++) {
                r.setSeriesPaint(i, PALETA.get(i));
            }
        }
        return chart;
    }

    private JFreeChart torta(Grafico g, List<Map<String, Object>> filas) {
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        String col = g.series().getFirst().columna();
        for (Map<String, Object> f : filas) {
            ds.setValue(String.valueOf(f.get(g.etiqueta())), Fmt.numero(f.get(col)));
        }
        JFreeChart chart = g.tipo() == Grafico.Tipo.DONA ? ChartFactory.createRingChart(null, ds, true, false, false)
                : ChartFactory.createPieChart(null, ds, true, false, false);
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.setLabelBackgroundPaint(Color.WHITE);
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", new DecimalFormat("0"), new DecimalFormat("0.0 %")));
        plot.setSectionOutlinesVisible(true);
        plot.setDefaultSectionOutlinePaint(Color.WHITE);
        plot.setDefaultSectionOutlineStroke(new BasicStroke(2f));
        int i = 0;
        for (Object k : ds.getKeys()) {
            plot.setSectionPaint((String) k, PALETA.get(i++ % PALETA.size()));
        }
        if (plot instanceof RingPlot ring) {
            ring.setSectionDepth(0.42);
            ring.setSeparatorsVisible(false);
        }
        return chart;
    }

    private static DecimalFormat formatoEje(Formato f) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat(f == Formato.MONEDA ? "$ #,##0" : f == Formato.KG ? "#,##0 kg" : "#,##0", s);
    }
}
