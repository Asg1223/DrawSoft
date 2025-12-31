package ninth;

import java.awt.*;
import java.awt.geom.*;

/**
 * 楕円を描画するクラス
 * wとhを横軸と縦軸の直径として楕円を描く
 * 負のw, hにも対応するため、Rectと同様の正規化処理を行う
 */
public class Ellipse extends Figure {
    Ellipse2D f;  // 描画用の楕円オブジェクト

    /**
     * 楕円を描画
     * w, hを正規化してから楕円を作成
     */
    @Override public void paint(Graphics2D g) {
        // 座標を正規化（w, hが負の場合に対応）
        double left = x, top = y;
        double width = w, height = h;
        if (w < 0) { left = x + w; width = -w; }   // 左方向ドラッグ対応
        if (h < 0) { top = y + h; height = -h; }   // 上方向ドラッグ対応
        
        // 正規化された座標とサイズで楕円を作成
        f = new Ellipse2D.Double(left, top, width, height);
        
        // 線のスタイルを設定
        g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(color);  // 描画色を設定
        
        // 塗りつぶしまたは枠線描画
        if (filled) g.fill(f);   // 塗りつぶし
        else g.draw(f);          // 枠線のみ
    }
    
    /**
     * ヒットテスト：点(px, py)が楕円内にあるかを判定
     * 楕円の内点判定は、正規化座標系で (x/rx)² + (y/ry)² ≤ 1 で判定
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 楕円内ならtrue
     */
    @Override
    public boolean contains(double px, double py) {
        // 座標を正規化
        double left = x, top = y;
        double width = w, height = h;
        if (w < 0) { left = x + w; width = -w; }
        if (h < 0) { top = y + h; height = -h; }
        
        // 楕円の半径（横軸と縦軸）
        double rx = width/2.0;
        double ry = height/2.0;
        // 楕円の中心座標
        double cx = left + rx;
        double cy = top + ry;
        
        // 楕円のサイズが無効な場合はfalse
        if (rx <= 0 || ry <= 0) return false;
        
        // 中心からの正規化された距離を計算
        double nx = (px - cx) / rx;
        double ny = (py - cy) / ry;
        // 正規化座標系での距離が1以下なら楕円内
        return nx*nx + ny*ny <= 1.0;
    }

    /**
     * 外接矩形を取得
     * @return 楕円を囲む矩形
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
