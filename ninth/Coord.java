package ninth;

import java.io.*;

/**
 * 座標を管理する基本クラス
 * すべての図形クラスの基底となり、位置情報を保持する
 *
 * 注意: Undo/Redoや保存/読み込みで座標(x,y)を正しく復元するため、
 * Serializable を実装する。
 */
public class Coord implements Serializable {
    private static final long serialVersionUID = 1L;

    double x, y;  // 図形の基準座標（x座標、y座標）

    /**
     * コンストラクタ：座標を原点(0, 0)で初期化
     */
    Coord() { x = y = 0; }

    /**
     * 相対移動：現在位置から指定された量だけ移動
     * @param dx x方向の移動量
     * @param dy y方向の移動量
     */
    public void move(double dx, double dy){
        x += dx;  // x座標に移動量を加算
        y += dy;  // y座標に移動量を加算
    }

    /**
     * 絶対移動：指定された座標に直接移動
     * @param x 新しいx座標
     * @param y 新しいy座標
     */
    public void moveto(double x, double y){
        this.x = x;  // x座標を設定
        this.y = y;  // y座標を設定
    }
}

