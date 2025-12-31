package ninth;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

/**
 * 折れ線を描画するクラス
 * クリックするごとに頂点を追加し、それらを直線で順に接続する
 * Freehandと異なり、クリック単位で頂点が追加されるため、より正確な形状を描ける
 */
public class Polyline extends Figure {
    ArrayList<Point2D.Double> pts = new ArrayList<>();  // 頂点の列

    /**
     * コンストラクタ：開始点、色、線幅を設定
     * @param x 開始点のx座標
     * @param y 開始点のy座標
     * @param c 描画色
     * @param strokeWidth 線幅
     */
    public Polyline(double x, double y, Color c, float strokeWidth) {
        // 基準座標を設定
        this.x = x; this.y = y;
        // 色を設定（nullの場合は黒）
        this.color = (c != null) ? c : Color.BLACK;
        // 線幅を設定
        this.strokeWidth = strokeWidth;
        // 最初の頂点を追加
        pts.add(new Point2D.Double(x, y));
    }

    /**
     * クリックするごとに新しい頂点を追加
     * @param x 追加する頂点のx座標
     * @param y 追加する頂点のy座標
     */
    public void addPoint(double x, double y) {
        pts.add(new Point2D.Double(x, y));
    }

    /** 頂点列を取得 */
    public ArrayList<Point2D.Double> getPoints() { return pts; }

    /**
     * 折れ線を描画
     * 頂点を順に直線で接続して描く
     */
    @Override
    public void paint(Graphics2D g) {
        if (pts.size() == 0) return;  // 頂点がない場合は何も描かない
        
        g.setColor(color);  // 描画色を設定
        // 線のスタイルを設定
        g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // 隣接する頂点ペアを順に線分で接続
        for (int i = 0; i + 1 < pts.size(); i++) {
            Point2D.Double a = pts.get(i);      // i番目の頂点
            Point2D.Double b = pts.get(i+1);    // i+1番目の頂点
            // 頂点aからbまでの直線を描画
            g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
        }
    }

    /**
     * ヒットテスト：点(px, py)が折れ線の近くにあるかを判定
     * 各線分からの距離を計算し、いずれかが近ければtrue
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 折れ線の近くならtrue
     */
    @Override
    public boolean contains(double px, double py) {
        if (pts.size() < 2) return false;  // 頂点が2個未満なら線分がないのでfalse
        
        // 判定用のしきい値（最低2.0または線幅の半分）
        double thresh = Math.max(2.0, strokeWidth/2.0);
        
        // 各線分に対して距離を計算
        for (int i = 0; i + 1 < pts.size(); i++) {
            Point2D.Double a = pts.get(i);      // 線分の始点
            Point2D.Double b = pts.get(i+1);    // 線分の終点
            // 点から線分までの最短距離
            double dist = Line2D.ptSegDist(a.x, a.y, b.x, b.y, px, py);
            if (dist <= thresh) return true;  // しきい値以下なら選択可能
        }
        return false;
    }

    /**
     * 外接矩形を取得
     * すべての頂点を囲む最小の矩形を計算
     * @return 折れ線を囲む矩形（パディング付き）
     */
    @Override
    public Rectangle2D getBounds2D() {
        if (pts.isEmpty()) return null;  // 頂点がない場合はnull
        
        // すべての頂点から最小・最大座標を探す
        double minx = Double.POSITIVE_INFINITY, miny = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY, maxy = Double.NEGATIVE_INFINITY;
        for (Point2D.Double p : pts) {
            if (p.x < minx) minx = p.x;
            if (p.y < miny) miny = p.y;
            if (p.x > maxx) maxx = p.x;
            if (p.y > maxy) maxy = p.y;
        }
        
        // 選択しやすくするためのパディング
        double pad = Math.max(2.0, strokeWidth/2.0);
        // パディングを含めた矩形を返す
        return new Rectangle2D.Double(minx - pad, miny - pad, (maxx - minx) + pad*2, (maxy - miny) + pad*2);
    }
}
