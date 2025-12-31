package ninth;

import java.awt.*;
import java.awt.geom.*;

/**
 * 固定サイズの点（ドット）を描画するクラス
 * クリックした位置に小さな円形の点を配置
 */
public class Dot extends Figure {
    double size = 10.0;  // 点の直径（固定サイズ）
    Ellipse2D f;         // 描画用の楕円形オブジェクト

    /**
     * 点を描画
     * 中心座標(x, y)を基準に、固定サイズの円を描く
     */
    @Override public void paint(Graphics2D g){
        // 中心(x, y)から半径分ずらした位置を左上として円を作成
        f = new Ellipse2D.Double(x - size/2, y - size/2, size, size);
        
        // 線のスタイルを設定（線幅、端を丸める、接続部を丸める）
        g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(color);  // 描画色を設定
        
        // 塗りつぶしフラグがtrueなら内部を塗る
        if (filled) g.fill(f);
        // 常に枠線を描画
        g.draw(f);
    }

    /**
     * ヒットテスト：点(px, py)が円の内部にあるかを判定
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 中心からの距離が半径以内ならtrue
     */
    @Override
    public boolean contains(double px, double py) {
        double dx = px - x, dy = py - y;  // 中心からの距離ベクトル
        double r = size / 2.0;             // 半径
        // 距離の2乗が半径の2乗以下なら円内
        return dx*dx + dy*dy <= r*r;
    }

    /**
     * 外接矩形を取得
     * @return 点を囲む正方形の矩形
     */
    @Override
    public java.awt.geom.Rectangle2D getBounds2D() {
        double r = size/2.0;  // 半径
        // 中心から半径分ずらした左上座標と、サイズ×サイズの矩形を返す
        return new java.awt.geom.Rectangle2D.Double(x - r, y - r, size, size);
    }
}
