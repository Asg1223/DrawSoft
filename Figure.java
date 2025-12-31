package ninth;

import java.awt.*;
import java.io.*;

/**
 * すべての図形クラスの抽象基底クラス
 * 座標管理(Coord)を継承し、図形の共通属性と描画インターフェースを定義
 * Serializable実装により、図形データの保存・読み込みが可能
 */
public abstract class Figure extends Coord implements Serializable {
    double w = 0, h = 0;              // 図形の幅と高さ（ドラッグ量）
    Color color = Color.BLACK;        // 図形の描画色（デフォルト：黒）
    double strokeWidth = 2.0;         // 線幅（デフォルト：2.0ピクセル）
    boolean filled = false;           // 塗りつぶしフラグ（false=枠線のみ、true=塗りつぶし）

    /**
     * 図形のサイズを設定
     * @param w 幅（または横方向のドラッグ量）
     * @param h 高さ（または縦方向のドラッグ量）
     */
    public void setWH(double w, double h){
        this.w = w; this.h = h;
    }

    /** 線幅を設定 */
    public void setStrokeWidth(double sw) { this.strokeWidth = sw; }
    /** 線幅を取得 */
    public double getStrokeWidth() { return this.strokeWidth; }
    /** 塗りつぶしの有無を設定 */
    public void setFilled(boolean f) { this.filled = f; }
    /** 塗りつぶし状態を取得 */
    public boolean isFilled() { return this.filled; }

    /**
     * 図形を描画する（各サブクラスで実装）
     * @param g 描画コンテキスト
     */
    public abstract void paint(Graphics2D g);
    
    /**
     * ヒットテスト：点(px, py)が図形内に含まれるかを判定
     * 図形の選択に使用される
     * @param px テストするx座標
     * @param py テストするy座標
     * @return 点が図形内にあればtrue
     */
    public abstract boolean contains(double px, double py);
    
    /**
     * 図形の外接矩形（バウンディングボックス）を取得
     * 選択矩形の描画やリサイズハンドルの配置に使用
     * @return 図形を囲む矩形
     */
    public abstract java.awt.geom.Rectangle2D getBounds2D();
}
