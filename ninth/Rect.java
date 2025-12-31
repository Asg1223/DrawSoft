package ninth;

import java.awt.*;
import java.awt.geom.*;

/**
 * 矩形を描画するクラス
 * ドラッグ方向に関わらず正しく矩形を描画するため、
 * 負の幅・高さを正の値に正規化する処理を含む
 */
public class Rect extends Figure {
    Rectangle2D f;  // 描画用の矩形オブジェクト

    /**
     * 矩形を描画
     * w, h が負の場合にも対応し、正しい左上座標とサイズを計算
     */
    @Override public void paint(Graphics2D g) {
        // 初期値：始点を左上、wとhをそのまま使用
        double left = x, top = y;
        double width = w, height = h;
        
        // wが負の場合（左方向にドラッグ）
        if (w < 0) {
            left = x + w;    // 左上を調整
            width = -w;      // 幅を正の値に
        }
        // hが負の場合（上方向にドラッグ）
        if (h < 0) {
            top = y + h;     // 左上を調整
            height = -h;     // 高さを正の値に
        }
        
        // 正規化された座標とサイズで矩形を作成
        f = new Rectangle2D.Double(left, top, width, height);
        
        // 線のスタイルを設定
        g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(color);  // 描画色を設定
        
        // 塗りつぶしまたは枠線描画
        if (filled) g.fill(f);   // 塗りつぶし
        else g.draw(f);          // 枠線のみ
    }

    /**
     * ヒットテスト：点(px, py)が矩形内にあるかを判定
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 矩形内ならtrue
     */
    @Override
    public boolean contains(double px, double py) {
        // 座標を正規化（paintと同じ処理）
        double left = x, top = y;
        double width = w, height = h;
        if (w < 0) { left = x + w; width = -w; }
        if (h < 0) { top = y + h; height = -h; }
        
        // 矩形内判定：左端≤px≤右端 AND 上端≤py≤下端
        return px >= left && px <= left + width && py >= top && py <= top + height;
    }

    /**
     * 外接矩形を取得（矩形自身の境界）
     * @return 正規化された矩形
     */
    @Override
    public java.awt.geom.Rectangle2D getBounds2D() {
        // 座標を正規化
        double left = x, top = y;
        double width = w, height = h;
        if (w < 0) { left = x + w; width = -w; }
        if (h < 0) { top = y + h; height = -h; }
        return new java.awt.geom.Rectangle2D.Double(left, top, width, height);
    }
}
