package ninth;

import java.awt.*;
import java.awt.geom.*;

/**
 * 直線を描画するクラス
 * 始点(x, y)から終点(x+w, y+h)までの線分を描く
 * 選択しやすくするため、線からの距離でヒット判定を行う
 */
public class Line extends Figure {
    Line2D f;  // 描画用の線分オブジェクト

    /**
     * 直線を描画
     * 始点から終点までの線分を描く
     */
    @Override public void paint(Graphics2D g) {
        // 始点(x, y)から終点(x+w, y+h)までの線分を作成
        f = new Line2D.Double(x, y, x + w, y + h);
        
        // 線のスタイルを設定（線幅、端を丸める、接続部を丸める）
        g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(color);  // 描画色を設定
        
        // 直線を描画
        g.draw(f);
    }

    /**
     * ヒットテスト：点(px, py)が線分の近くにあるかを判定
     * 線からの最短距離を計算し、線幅の半分以内なら選択可能と判定
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 線分の近くならtrue
     */
    @Override
    public boolean contains(double px, double py) {
        // 線分を再作成
        Line2D seg = new Line2D.Double(x, y, x + w, y + h);
        // 点から線分までの最短距離を計算
        double dist = seg.ptSegDist(px, py);
        // 距離がしきい値以下なら選択可能（最低2.0または線幅の半分）
        return dist <= Math.max(2.0, strokeWidth/2.0);
    }

    /**
     * 外接矩形を取得
     * 線分を囲む矩形を返す。選択しやすくするためパディングを追加
     * @return 線分を囲む矩形（パディング付き）
     */
    @Override
    public java.awt.geom.Rectangle2D getBounds2D() {
        // 左端と上端を計算（始点と終点の小さい方）
        double left = Math.min(x, x + w);
        double top = Math.min(y, y + h);
        // 幅と高さを計算（絶対値）
        double width = Math.abs(w);
        double height = Math.abs(h);
        
        // 選択しやすくするためのパディング（最低2.0または線幅の半分）
        double pad = Math.max(2.0, strokeWidth/2.0);
        
        // パディングを含めた矩形を返す
        return new java.awt.geom.Rectangle2D.Double(left - pad, top - pad, width + pad*2, height + pad*2);
    }
}
