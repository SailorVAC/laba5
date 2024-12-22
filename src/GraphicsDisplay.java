import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import javax.swing.*;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {
    private Double[][] graphicsData;
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean highlightSpecialPoints = false;
    private int hoveredPointIndex = -1; // Индекс точки, на которой находится курсор
    private int startX, startY, endX, endY; // Координаты для области масштабирования
    private boolean isSelecting = false; // Флаг для проверки, что выделение в процессе
    private Double[][] originalData; // Массив для хранения исходных данных графика

    // Масштабирование области (параметры масштаба)
    private double zoomMinX = Double.MAX_VALUE, zoomMaxX = Double.MIN_VALUE;
    private double zoomMinY = Double.MAX_VALUE, zoomMaxY = Double.MIN_VALUE;

    public GraphicsDisplay() {
        // Инициализация массива данных графика (если он не был передан)
        this.graphicsData = new Double[0][0];
        this.originalData = new Double[0][0]; // Инициализация данных для восстановления

        // Обработчик события нажатия мыши для начала выделения
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Проверка, что нажата левая кнопка мыши
                if (SwingUtilities.isLeftMouseButton(e)) {
                    startX = e.getX();
                    startY = e.getY();
                    isSelecting = true; // Начинаем процесс выделения
                }
                // Обработка правой кнопки для восстановления исходного масштаба
                if (SwingUtilities.isRightMouseButton(e)) {
                    resetZoom(); // Восстанавливаем исходный масштаб
                    repaint(); // Обновляем панель
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Завершаем выделение, если левая кнопка была отпущена
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isSelecting = false;
                    endX = e.getX();
                    endY = e.getY();
                    zoomToSelectedArea(); // Масштабирование выбранной области
                    repaint();
                }
            }
        });

        // Обработчик события перемещения мыши для рисования рамки
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Проверка, наведён ли курсор на маркер
                hoveredPointIndex = -1;
                if (graphicsData != null && graphicsData.length > 0) {
                    for (int i = 0; i < graphicsData.length; i++) {
                        int x = translateX(graphicsData[i][0]);
                        int y = translateY(graphicsData[i][1]);
                        if (Math.abs(e.getX() - x) < 10 && Math.abs(e.getY() - y) < 10) {
                            hoveredPointIndex = i;
                            break;
                        }
                    }
                }
                repaint(); // Обновляем изображение при изменении положения курсора
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isSelecting) {
                    endX = e.getX();
                    endY = e.getY();
                    repaint(); // Обновляем изображение при перетаскивании рамки выделения
                }
            }
        });
    }

    public void showGraphics(Double[][] graphicsData) {
        this.graphicsData = graphicsData != null ? graphicsData : new Double[0][0]; // Защищаем от null
        this.originalData = graphicsData != null ? graphicsData : new Double[0][0]; // Сохраняем оригинальные данные для восстановления
        repaint();
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setHighlightSpecialPoints(boolean highlightSpecialPoints) {
        this.highlightSpecialPoints = highlightSpecialPoints;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (graphicsData == null || graphicsData.length == 0) return;

        Graphics2D canvas = (Graphics2D) g;

        // Настройки сглаживания
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Отрисовка осей координат
        if (showAxis) paintAxis(canvas);

        // Отрисовка линий графика с учетом области масштабирования
        paintGraphics(canvas);

        // Отрисовка маркеров точек
        if (showMarkers) paintMarkers(canvas);

        // Отображение координат при наведении на маркер
        if (hoveredPointIndex != -1 && graphicsData != null && graphicsData.length > 0) {
            Double[] point = graphicsData[hoveredPointIndex];
            String coordinates = String.format("X: %.2f, Y: %.2f", point[0], point[1]);
            canvas.setColor(Color.BLACK);
            canvas.setFont(new Font("Arial", Font.PLAIN, 14));
            canvas.drawString(coordinates, translateX(point[0]) + 10, translateY(point[1]) - 10);
        }

        // Рисование рамки выделения, если оно активно
        if (isSelecting) {
            canvas.setColor(Color.GRAY);
            canvas.setStroke(new BasicStroke(1.0f));
            // Пунктирная линия
            float[] dash = {5.0f};
            canvas.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            canvas.drawRect(Math.min(startX, endX), Math.min(startY, endY), Math.abs(startX - endX), Math.abs(startY - endY));
        }
    }

    private void paintAxis(Graphics2D canvas) {
        canvas.setStroke(new BasicStroke(2.0f));
        canvas.setColor(Color.BLACK);

        double minX = getMinX();
        double maxX = getMaxX();
        double minY = getMinY();
        double maxY = getMaxY();

        int zeroX = translateX(0);
        int zeroY = translateY(0);

        // Добавляем отступы для осей
        int padding = 40;

        // Ось X
        int axisY = (zeroY >= padding && zeroY <= getHeight() - padding) ? zeroY : getHeight() - padding;
        canvas.drawLine(translateX(minX), axisY, translateX(maxX), axisY);

        // Ось Y
        int axisX = (zeroX >= padding && zeroX <= getWidth() - padding) ? zeroX : padding;
        canvas.drawLine(axisX, translateY(maxY), axisX, translateY(minY));

        // Подписи осей
        canvas.drawString("X", getWidth() - 20, axisY - 10);
        canvas.drawString("Y", axisX + 10, 20);

        // Подписи значений
        canvas.drawString(String.format("%.2f", minX), translateX(minX) + 5, axisY + 15);
        canvas.drawString(String.format("%.2f", maxX), translateX(maxX) - 35, axisY + 15);
        canvas.drawString(String.format("%.2f", minY), axisX + 5, translateY(minY) - 5);
        canvas.drawString(String.format("%.2f", maxY), axisX + 5, translateY(maxY) + 15);

        // Обозначение точки (0, 0)
        if (minX <= 0 && maxX >= 0 && minY <= 0 && maxY >= 0) {
            canvas.setColor(Color.RED);
            canvas.fill(new Ellipse2D.Double(zeroX - 5, zeroY - 5, 10, 10));
            canvas.drawString("(0,0)", zeroX + 5, zeroY - 5);
        }
    }

    private void paintGraphics(Graphics2D canvas) {
        canvas.setColor(Color.BLUE);
        canvas.setStroke(new BasicStroke(2.0f));

        if (graphicsData != null && graphicsData.length > 1) {
            for (int i = 0; i < graphicsData.length - 1; i++) {
                int x1 = translateX(graphicsData[i][0]);
                int y1 = translateY(graphicsData[i][1]);
                int x2 = translateX(graphicsData[i + 1][0]);
                int y2 = translateY(graphicsData[i + 1][1]);
                canvas.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void paintMarkers(Graphics2D canvas) {
        if (graphicsData != null) {
            for (Double[] point : graphicsData) {
                int x = translateX(point[0]);
                int y = translateY(point[1]);

                // Если включена подсветка особых точек
                if (highlightSpecialPoints && isSpecialPoint(point)) {
                    canvas.setColor(Color.RED); // Красный для особых точек
                } else {
                    canvas.setColor(Color.BLUE); // Синий для обычных маркеров
                }

                // Рисуем маркер в виде круга
                canvas.fill(new Ellipse2D.Double(x - 5, y - 5, 11, 11));

                // Рисуем крест внутри маркера
                canvas.setColor(Color.WHITE); // Цвет креста
                canvas.setStroke(new BasicStroke(2.0f)); // Толщина линии

                // Вертикальная линия креста
                canvas.drawLine(x, y - 5, x, y + 5);
                // Горизонтальная линия креста
                canvas.drawLine(x - 5, y, x + 5, y);
            }
        }
    }

    // Проверка, является ли точка "особой"
    private boolean isSpecialPoint(Double[] point) {
        return Math.floor(point[1]) % 2 == 0; // Чётность целой части значения Y
    }

    // Преобразование координат X в пиксели
    private int translateX(double x) {
        double scale = getWidth() / (zoomMaxX - zoomMinX); // Масштабирование с учетом текущего масштаба
        return (int) ((x - zoomMinX) * scale);
    }

    // Преобразование координат Y в пиксели
    private int translateY(double y) {
        double scale = getHeight() / (zoomMaxY - zoomMinY); // Масштабирование с учетом текущего масштаба
        return getHeight() - (int) ((y - zoomMinY) * scale);
    }

    // Преобразование пикселей в реальные координаты X
    private double translateXInverse(int x) {
        double scale = getWidth() / (zoomMaxX - zoomMinX); // Масштабирование с учетом текущего масштаба
        return (x / scale) + zoomMinX;
    }

    // Преобразование пикселей в реальные координаты Y
    private double translateYInverse(int y) {
        double scale = getHeight() / (zoomMaxY - zoomMinY); // Масштабирование с учетом текущего масштаба
        return zoomMinY + (getHeight() - y) / scale;
    }

    // Масштабирование на выбранную область
    private void zoomToSelectedArea() {
        if (startX == endX || startY == endY) return; // Проверка на допустимость области

        // Преобразование координат пикселей в реальные координаты графика
        double newMinX = translateXInverse(Math.min(startX, endX));
        double newMaxX = translateXInverse(Math.max(startX, endX));
        double newMinY = translateYInverse(Math.max(startY, endY)); // Меняем max и min для оси Y, чтобы избежать инверсии
        double newMaxY = translateYInverse(Math.min(startY, endY)); // Аналогично для maxY

        // Проверка на слишком маленькую выделенную область
        if (Math.abs(newMaxX - newMinX) < 0.0001 || Math.abs(newMaxY - newMinY) < 0.0001) {
            return;
        }

        // Устанавливаем новые границы для масштабирования
        zoomMinX = newMinX;
        zoomMaxX = newMaxX;
        zoomMinY = newMinY;
        zoomMaxY = newMaxY;

        repaint();
    }

    // Сброс масштаба
    private void resetZoom() {
        zoomMinX = getMinX();
        zoomMaxX = getMaxX();
        zoomMinY = getMinY();
        zoomMaxY = getMaxY();
        repaint();
    }

    private double getMinX() {
        double minX = graphicsData[0][0];
        for (Double[] point : graphicsData) {
            if (point[0] < minX) minX = point[0];
        }
        return minX;
    }

    private double getMaxX() {
        double maxX = graphicsData[0][0];
        for (Double[] point : graphicsData) {
            if (point[0] > maxX) maxX = point[0];
        }
        return maxX;
    }

    private double getMinY() {
        double minY = graphicsData[0][1];
        for (Double[] point : graphicsData) {
            if (point[1] < minY) minY = point[1];
        }
        return minY;
    }

    private double getMaxY() {
        double maxY = graphicsData[0][1];
        for (Double[] point : graphicsData) {
            if (point[1] > maxY) maxY = point[1];
        }
        return maxY;
    }
}
