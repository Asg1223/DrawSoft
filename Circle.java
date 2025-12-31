package ninth;

import java.awt.*;
import java.awt.geom.*;

/**
 * 円を描画するクラス
 * ドラッグ量(w, h)から半径を計算し、中心を基準に正円を描く
 * 半径 = √(w² + h²) で計算される
 */
public class Circle extends Figure {
    Ellipse2D f;  // 描画用の楕円形オブジェクト

    /**
     * 円を描画
     * ドラッグ量から半径を算出し、中心(x, y)を基準に円を描く
     */
    @Override public void paint(Graphics2D g) {
        // ドラッグ量w, hから半径を計算（ピタゴラスの定理）
        double size = Math.sqrt((double)(w * w + h * h));
        
        // 中心(x, y)から半径分ずらした位置を左上として、直径×直径の円を作成
        f = new Ellipse2D.Double(x - size, y - size, size * 2, size * 2);
        
        // 線のスタイルを設定（線幅、端を丸める、接続部を丸める）
        g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(color);  // 描画色を設定
        
        // 塗りつぶしまたは枠線描画
        if (filled) g.fill(f);   // 塗りつぶし
        else g.draw(f);          // 枠線のみ
    }

    /**
     * ヒットテスト：点(px, py)が円の内部にあるかを判定
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 中心からの距離が半径以内ならtrue
     */
    @Override
    public boolean contains(double px, double py) {
        // 半径を再計算
        double size = Math.sqrt((double)(w * w + h * h));
        // 中心からの距離ベクトル
        double dx = px - x, dy = py - y;
        // 距離の2乗が半径の2乗以下なら円内
        return dx*dx + dy*dy <= size*size;
    }

    /**
     * 外接矩形を取得
     * @return 円を囲む正方形の矩形
     */
    @Override
    public java.awt.geom.Rectangle2D getBounds2D() {
        // 半径を再計算
        double size = Math.sqrt((double)(w * w + h * h));
        // 中心から半径分ずらした左上座標と、直径×直径の矩形を返す
        return new java.awt.geom.Rectangle2D.Double(x - size, y - size, size*2, size*2);
    }
}
