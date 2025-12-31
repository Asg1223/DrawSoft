package ninth;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;

/**
 * 描画キャンバスクラス
 * すべての図形を管理し、マウスイベントに応じて図形を作成・編集・削除する
 * JPanel を継承し、MouseListener/MouseMotionListener/ActionListener を実装
 */
public class PaintCanvas extends JPanel implements MouseListener, MouseMotionListener, ActionListener {
    // 描画中の図形オブジェクト（現在ドラッグ中の図形）
    Figure obj = null;
    // マウス座標の記録用
    double x, y;
    // メインウィンドウ（Paint4）への参照
    Paint4 p4;
    // すべての図形を保持するリスト（描画順）
    ArrayList<Figure> objList;
    // 描画モード（1=点、2=通常図形、3=フリーハンド、4=折れ線）
    int mode = 0;
    
    // 現在選択中の描画色（デフォルト：黒）
    private Color selectedColor = Color.BLACK;
    // 現在の線幅（デフォルト：2.0ピクセル）
    private float strokeWidth = 2.0f;
    // 塗りつぶしフラグ（デフォルト：false）
    private boolean filled = false;
    
    // === 消しゴムモード用 ===
    // 消しゴムモード中かどうか
    private boolean erasing = false;
    // 消しゴムが通った経路の点列
    private java.util.List<java.awt.geom.Point2D.Double> eraserPoints = new ArrayList<>();
    // 消しゴムの半径（ピクセル）
    private double eraserRadius = 8.0;
    
    // === 選択・移動・リサイズモード用 ===
    // 現在選択中の図形
    private Figure selectedFigure = null;
    // 前回のマウス座標（移動量の計算用）
    private double lastMouseX = 0, lastMouseY = 0;
    // リサイズ中かどうか
    private boolean resizing = false;
    // 選択開始時の図形の境界情報（リサイズ計算用）
    private double selInitialX, selInitialY, selInitialW, selInitialH;
    // マウスドラッグ開始座標
    private double selMouseStartX, selMouseStartY;
    // アクティブなハンドルID: 0=なし、1〜8=コーナー/辺（NW,N,NE,E,SE,S,SW,W）
    private int activeHandle = 0;
    // ハンドルの半サイズ（ピクセル）
    private static final int HS = 6;
    
    // === Undo/Redo機能用 ===
    // Undo用スタック（シリアライズされたキャンバス状態のスナップショット）
    private Deque<byte[]> undoStack = new ArrayDeque<>();
    // Redo用スタック
    private Deque<byte[]> redoStack = new ArrayDeque<>();
    // 履歴の最大保持数
    private int maxHistory = 50;

    /**
     * コンストラクタ
     * @param p メインウィンドウへの参照
     * @param fname 起動時に読み込むファイル名（nullなら新規キャンバス）
     */
    PaintCanvas(Paint4 p, String fname) {
        this.p4 = p;
        objList = new ArrayList<Figure>();
        // マウスイベントリスナーを登録
        addMouseListener(this);
        addMouseMotionListener(this);

        // ファイル名が指定されていれば読み込む
        if(fname != null) load(fname);
    }

    /**
     * マウスボタンが押された時の処理
     * - 右クリック：既存図形の色/線幅/塗りの編集
     * - 選択モード：図形の選択と移動/リサイズの開始
     * - 各描画モード：新しい図形の作成開始
     */
    @Override public void mousePressed(MouseEvent e) {
        Point2D p = e.getPoint();
        x = p.getX();
        y = p.getY();

        // === 右クリック：既存図形の編集 ===
        if (e.getButton() == MouseEvent.BUTTON3) {
            // 最上層の図形から順に当たり判定
            for (int i = objList.size() - 1; i >= 0; i--) {
                Figure f = objList.get(i);
                if (f.contains(x, y)) {
                    // Undo用スナップショットを保存
                    pushUndo();
                    
                    // 色選択ダイアログを表示
                    Color c = JColorChooser.showDialog(this, "色を選択", f.color != null ? f.color : Color.BLACK);
                    if (c != null) f.color = c;
                    
                    // 線幅入力ダイアログを表示
                    String in = JOptionPane.showInputDialog(this, "線幅を入力:", f.strokeWidth);
                    if (in != null) {
                        try { f.strokeWidth = Float.parseFloat(in); } catch (Exception ex) {}
                    }
                    
                    // 塗りつぶしの確認ダイアログを表示
                    int ans = JOptionPane.showConfirmDialog(this, "塗りにしますか?", "塗り", JOptionPane.YES_NO_OPTION);
                    f.filled = (ans == JOptionPane.YES_OPTION);

                    redoStack.clear();  // 新操作でRedoスタックをクリア
                    repaint();
                    return;
                }
            }
        }

        // === 選択モード：図形の選択と移動/リサイズ開始 ===
        if (p4.r0 != null && p4.r0.isSelected()) {
            // 最上層の図形から順に当たり判定
            for (int i = objList.size() - 1; i >= 0; i--) {
                Figure f = objList.get(i);
                java.awt.geom.Rectangle2D bb = f.getBounds2D();
                // 図形本体またはバウンディングボックスがクリックされたか判定
                if (f.contains(x, y) || (bb != null && bb.contains(x, y))) {
                    selectedFigure = f;  // 選択図形として記録
                    lastMouseX = x; lastMouseY = y;
                    selMouseStartX = x; selMouseStartY = y;
                    
                    // バウンディングボックスを初期選択範囲として記録
                    if (bb != null) {
                        selInitialX = bb.getX(); selInitialY = bb.getY(); 
                        selInitialW = bb.getWidth(); selInitialH = bb.getHeight();
                    } else {
                        selInitialX = f.x; selInitialY = f.y; 
                        selInitialW = f.w; selInitialH = f.h;
                    }
                    
                    // どのハンドルがクリックされたかを判定（0=なし、1〜8=コーナー/辺）
                    activeHandle = getHandleAt(selInitialX, selInitialY, selInitialW, selInitialH, x, y);
                    resizing = (activeHandle != 0);  // ハンドルがクリックされたらリサイズモード
                    
                    // 変更前のスナップショットを保存
                    pushUndo();
                    redoStack.clear();
                    repaint();
                    return;
                }
            }
        }

        // === 各描画モードでの図形作成開始 ===
        if(p4.r1.isSelected()){ 
            // 点（ドット）モード
            mode = 1; obj = new Dot();
        } else if (p4.r8 != null && p4.r8.isSelected()) {
            // 折れ線モード：新規作成または頂点追加（mouseClickedで処理）
            mode = 4;
            if (obj == null) {
                obj = new Polyline(x, y, selectedColor, strokeWidth);
                obj.color = selectedColor;
                obj.strokeWidth = strokeWidth;
                obj.filled = filled;
                pushUndo();  // 開始時のスナップショット
                redoStack.clear();
            }
        } else if(p4.r2.isSelected()){ 
            // 円モード
            mode = 2; obj = new Circle();
        } else if(p4.r3.isSelected()){  
            // 矩形モード
            mode = 2; obj = new Rect();
        } else if(p4.r4.isSelected()){  
            // 直線モード
            mode = 2; obj = new Line();
        } else if(p4.r5 != null && p4.r5.isSelected()) {
            // 楕円モード
            mode = 2; obj = new Ellipse();
        } else if (p4.r6 != null && p4.r6.isSelected()) {
            // フリーハンドモード
            mode = 3;
            obj = new Freehand(x, y, selectedColor, strokeWidth);
        } else if (p4.r7 != null && p4.r7.isSelected()) {
            // 消しゴムモード：経路記録開始
            erasing = true;
            eraserPoints.clear();
            eraserPoints.add(new java.awt.geom.Point2D.Double(x, y));
        }
        
        // 新しく作る図形に開始座標を設定
        if(obj != null) obj.moveto(x, y);
        
        // 新しく作る図形に選択中の色・線幅・塗りを適用
        if (obj != null) {
            if (selectedColor != null) obj.color = selectedColor;
            obj.strokeWidth = strokeWidth;
            obj.filled = filled;
        }
        repaint();
    }

    /**
     * マウスがドラッグされた時の処理
     * - 消しゴムモード：経路を記録
     * - 選択モード：図形の移動またはリサイズ
     * - フリーハンドモード：点を追加
     * - その他の描画モード：図形のサイズを更新
     */
    @Override public void mouseDragged(MouseEvent e) {
        Point2D p = e.getPoint();
        x = p.getX();
        y = p.getY();

        if (erasing) {
            // 消しゴムモード：経路に点を追加
            eraserPoints.add(new java.awt.geom.Point2D.Double(x, y));
        } else if (selectedFigure != null) {
            // 選択モード：移動またはリサイズ
            if (resizing) {
                // === リサイズ処理 ===
                // 選択開始時の境界ボックスを基準に、ハンドルに応じて新しい境界を計算
                double left = selInitialX;
                double top = selInitialY;
                double right = selInitialX + selInitialW;
                double bottom = selInitialY + selInitialH;
                
                // アクティブなハンドルに応じて境界を更新
                switch (activeHandle) {
                    case 1: left = x; top = y; break;       
                    case 2: top = y; break;                 
                    case 3: right = x; top = y; break;    
                    case 4: right = x; break;              
                    case 5: right = x; bottom = y; break;   
                    case 6: bottom = y; break;              
                    case 7: left = x; bottom = y; break;   
                    case 8: left = x; break;                
                }
                
                // 新しいサイズを計算（最小1ピクセル）
                double newW = right - left;
                double newH = bottom - top;
                if (newW < 1) newW = 1;
                if (newH < 1) newH = 1;
                
                // 図形の種類に応じてリサイズを適用
                if (selectedFigure instanceof Freehand) {
                    // フリーハンド：すべての点をスケール変換
                    Freehand fh = (Freehand) selectedFigure;
                    ArrayList<java.awt.geom.Point2D.Double> pts = fh.getPoints();
                    ArrayList<java.awt.geom.Point2D.Double> newPts = new ArrayList<>();
                    double ox = selInitialX, oy = selInitialY, ow = selInitialW, oh = selInitialH;
                    if (ow <= 0) ow = 1; if (oh <= 0) oh = 1;
                    for (java.awt.geom.Point2D.Double pt : pts) {
                        double nx = left + ((pt.x - ox) * newW / ow);
                        double ny = top + ((pt.y - oy) * newH / oh);
                        newPts.add(new java.awt.geom.Point2D.Double(nx, ny));
                    }
                    pts.clear();
                    pts.addAll(newPts);
                } else if (selectedFigure instanceof Circle) {
                    // 円：中心と半径を更新（幅を半径として使用）
                    selectedFigure.x = left + newW / 2.0;
                    selectedFigure.y = top + newH / 2.0;
                    selectedFigure.w = newW / 2.0;
                    selectedFigure.h = 0;
                } else {
                    // その他の図形：境界ボックスを直接更新
                    selectedFigure.x = left;
                    selectedFigure.y = top;
                    selectedFigure.w = newW;
                    selectedFigure.h = newH;
                }
            } else {
                // === 移動処理 ===
                // 前回からの移動量を計算して図形を移動
                double dx = x - lastMouseX;
                double dy = y - lastMouseY;
                selectedFigure.move(dx, dy);
                lastMouseX = x; lastMouseY = y;
            }
        } else if (obj instanceof Freehand) {
            // フリーハンドモード：現在のマウス位置を点として追加
            ((Freehand)obj).addPoint(x, y);
        } else if(mode == 1) {
            // 点モード：現在のマウス位置に移動
            obj.moveto(x, y);
        } else if(mode == 2) {
            // 通常図形モード：ドラッグ量に応じてサイズを更新
            obj.setWH(x - obj.x, y - obj.y);
        }

        repaint();  // 再描画
    }

    /**
     * マウスボタンが離された時の処理
     * - 消しゴムモード：消しゴム処理を実行
     * - 選択モード：移動/リサイズを確定
     * - 描画モード：図形をリストに追加して確定
     */
    @Override public void mouseReleased(MouseEvent e) {
        Point2D p = e.getPoint();
        x = p.getX();
        y = p.getY();
        
        if (erasing) {
            // === 消しゴムモード終了 ===
            pushUndo();           // スナップショット保存
            applyEraser();        // 消しゴム処理実行
            redoStack.clear();
            erasing = false;
            eraserPoints.clear();
            mode = 0;
            repaint();
            return;
        }

        // 図形のサイズを最終更新
        if(mode == 1) obj.moveto(x, y);
        else if(mode == 2) obj.setWH(x - obj.x, y - obj.y);

        // === 選択モード：移動/リサイズを確定 ===
        if (selectedFigure != null) {
            // mousePressed で既に Undo スナップショットを保存済み
            selectedFigure = null;
            activeHandle = 0;
            resizing = false;
            redoStack.clear();
            mode = 0;
            repaint();
            return;
        }

        // === 描画モード：図形を確定してリストに追加 ===
        // 折れ線モード（mode==4）以外は、リリース時に図形を確定
        if(mode >= 1 && mode != 4){
            pushUndo();           // 変更前のスナップショット保存
            objList.add(obj);     // 図形をリストに追加
            obj = null;           // 描画中オブジェクトをクリア
            redoStack.clear();    // 新操作でRedoスタックをクリア
        }
        mode = 0;
        repaint();
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {
        if (p4.r8 != null && p4.r8.isSelected()) {
            Point2D p = e.getPoint();
            double px = p.getX(), py = p.getY();
            if (obj instanceof Polyline) {
                if (e.getClickCount() == 2) {
                    Polyline pl = (Polyline) obj;

                    java.util.List<java.awt.geom.Point2D.Double> pts = pl.getPoints();
                    if (pts.isEmpty() || Math.hypot(pts.get(pts.size()-1).x - px, pts.get(pts.size()-1).y - py) > 0.5) {
                        pl.addPoint(px, py);
                    }
                    objList.add(obj);
                    obj = null;
                    mode = 0;
                    redoStack.clear();
                    repaint();
                } else {
                
                    Polyline pl = (Polyline) obj;
                    java.util.List<java.awt.geom.Point2D.Double> pts = pl.getPoints();
                    if (pts.isEmpty() || Math.hypot(pts.get(pts.size()-1).x - px, pts.get(pts.size()-1).y - py) > 0.5) {
                        pl.addPoint(px, py);
                    }
                    repaint();
                }
            } else {
            }
        }
    }
    @Override public void mouseMoved(MouseEvent e) {
        Point2D p = e.getPoint();
        x = p.getX();
        y = p.getY();
        if (obj instanceof Polyline) repaint();
    }

    @Override public void actionPerformed(ActionEvent e){
        if(e.getSource() == p4.endBtn) {
            String fname = JOptionPane.showInputDialog(this, "ファイル名を入力してください:", "paint.dat");
            if (fname != null && !fname.trim().isEmpty()) {
                save(fname);
            }
            System.exit(0);
        } else if(e.getSource() == p4.clearBtn){
            pushUndo();
            objList.clear();
            redoStack.clear();
            repaint();
        }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for(Figure f : objList)
            f.paint(g2);

        if (obj != null) obj.paint(g2);

        if (obj instanceof Polyline) {
            Polyline pl = (Polyline) obj;
            java.util.List<java.awt.geom.Point2D.Double> pts = pl.getPoints();
            if (!pts.isEmpty()) {
                java.awt.geom.Point2D.Double last = pts.get(pts.size()-1);
                Color oldc = g2.getColor();
                Stroke olds = g2.getStroke();
                g2.setColor(new Color(0,0,0,128));
                g2.setStroke(new BasicStroke((float)pl.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4f,4f}, 0f));
                g2.draw(new Line2D.Double(last.x, last.y, x, y));
                g2.setStroke(olds);
                g2.setColor(oldc);
            }
        }

        
        if (erasing && !eraserPoints.isEmpty()) {
            Color old = g2.getColor();
            for (java.awt.geom.Point2D.Double pt : eraserPoints) {
                g2.setColor(Color.LIGHT_GRAY);
                double r = eraserRadius;
                g2.fill(new Ellipse2D.Double(pt.x - r, pt.y - r, r * 2, r * 2));
            }
            g2.setColor(old);
        }

        if (selectedFigure != null) {
            java.awt.geom.Rectangle2D bb = selectedFigure.getBounds2D();
            if (bb != null) {
                Color old = g2.getColor();
                Stroke os = g2.getStroke();
                g2.setColor(Color.BLUE);
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[]{4f,4f}, 0f));
                g2.draw(bb);
                g2.setStroke(os);
                double cx = bb.getX() + bb.getWidth()/2.0;
                double cy = bb.getY() + bb.getHeight()/2.0;
                double[][] pts = new double[][]{
                    {bb.getX(), bb.getY()}, 
                    {cx, bb.getY()}, 
                    {bb.getX()+bb.getWidth(), bb.getY()}, 
                    {bb.getX()+bb.getWidth(), cy}, 
                    {bb.getX()+bb.getWidth(), bb.getY()+bb.getHeight()}, 
                    {cx, bb.getY()+bb.getHeight()}, 
                    {bb.getX(), bb.getY()+bb.getHeight()}, 
                    {bb.getX(), cy} 
                };
                for (int i = 0; i < pts.length; i++) {
                    double hx = pts[i][0], hy = pts[i][1];
                    g2.fill(new Rectangle2D.Double(hx - HS, hy - HS, HS*2, HS*2));
                }
                g2.setColor(old);
            }
        }
    }

    

    public void save(String fname){
        try {
            FileOutputStream fos = new FileOutputStream(fname);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(objList);
            oos.close();
            fos.close();
        } catch(IOException e){}
    }

    private void applyEraser() {
        if (eraserPoints.isEmpty()) return;
        ArrayList<Figure> newList = new ArrayList<>();

        for (Figure f : objList) {
            if (f instanceof Freehand) {
                Freehand fh = (Freehand) f;
                ArrayList<java.awt.geom.Point2D.Double> pts = fh.getPoints();
                int n = pts.size();
                if (n == 0) continue;
                // mark points that survive
                boolean[] keep = new boolean[n];
                for (int i = 0; i < n; i++) {
                    java.awt.geom.Point2D.Double p = pts.get(i);
                    boolean erased = false;
                    for (java.awt.geom.Point2D.Double ept : eraserPoints) {
                        double dx = p.x - ept.x;
                        double dy = p.y - ept.y;
                        if (dx*dx + dy*dy <= eraserRadius*eraserRadius) { erased = true; break; }
                    }
                    keep[i] = !erased;
                }
                int i = 0;
                while (i < n) {
                    while (i < n && !keep[i]) i++;
                    int start = i;
                    while (i < n && keep[i]) i++;
                    int end = i; // [start, end)
                    if (end - start >= 2) {
                        java.awt.geom.Point2D.Double p0 = pts.get(start);
                        Freehand nf = new Freehand(p0.x, p0.y, fh.color, (float)fh.strokeWidth);
                        for (int k = start+1; k < end; k++) nf.addPoint(pts.get(k).x, pts.get(k).y);
                        newList.add(nf);
                    }
                }
            } else {
                boolean hit = false;
                for (java.awt.geom.Point2D.Double ept : eraserPoints) {
                    if (f.contains(ept.x, ept.y)) { hit = true; break; }
                    java.awt.geom.Rectangle2D bb = f.getBounds2D();
                    if (bb != null) {
                        double ex = ept.x, ey = ept.y;
                        if (bb.contains(ex, ey)) { hit = true; break; }
                    }
                }
                if (!hit) newList.add(f);
            }
        }

        objList = newList;
    }

    private void pushUndo() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(objList);
            oos.close();
            undoStack.addLast(bos.toByteArray());
            while (undoStack.size() > maxHistory) undoStack.removeFirst();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(objList);
            oos.close();
            redoStack.addLast(bos.toByteArray());

            byte[] prev = undoStack.removeLast();
            ByteArrayInputStream bis = new ByteArrayInputStream(prev);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object o = ois.readObject();
            if (o instanceof ArrayList) objList = (ArrayList<Figure>) o;
            ois.close();
            repaint();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(objList);
            oos.close();
            undoStack.addLast(bos.toByteArray());

            byte[] next = redoStack.removeLast();
            ByteArrayInputStream bis = new ByteArrayInputStream(next);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object o = ois.readObject();
            if (o instanceof ArrayList) objList = (ArrayList<Figure>) o;
            ois.close();
            repaint();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void setSelectedColor(Color c) { if (c != null) this.selectedColor = c; }
    public void setStrokeWidth(float w) { if (w > 0) this.strokeWidth = w; }
    public void setFilled(boolean f) { this.filled = f; }

    private int exportWidth = 800;
    private int exportHeight = 600;
    public void setExportSize(int w, int h) { if (w > 0 && h > 0) { this.exportWidth = w; this.exportHeight = h; } }

    public void exportImage(String fname, int w, int h) throws Exception {
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("invalid size");
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        double minx = Double.POSITIVE_INFINITY, miny = Double.POSITIVE_INFINITY, maxx = Double.NEGATIVE_INFINITY, maxy = Double.NEGATIVE_INFINITY;
        for (Figure f : objList) {
            java.awt.geom.Rectangle2D bb = f.getBounds2D();
            if (bb == null) continue;
            if (bb.getX() < minx) minx = bb.getX();
            if (bb.getY() < miny) miny = bb.getY();
            if (bb.getX() + bb.getWidth() > maxx) maxx = bb.getX() + bb.getWidth();
            if (bb.getY() + bb.getHeight() > maxy) maxy = bb.getY() + bb.getHeight();
        }
        double contentW = (minx==Double.POSITIVE_INFINITY) ? getWidth() : (maxx - minx);
        double contentH = (miny==Double.POSITIVE_INFINITY) ? getHeight() : (maxy - miny);
        if (contentW <= 0) contentW = Math.max(1, getWidth());
        if (contentH <= 0) contentH = Math.max(1, getHeight());

        double scaleX = w / contentW;
        double scaleY = h / contentH;
        double scale = Math.min(scaleX, scaleY);
       
        double tx = 0, ty = 0;
        if (minx != Double.POSITIVE_INFINITY) {
            tx = -minx * scale + (w - contentW * scale) / 2.0;
            ty = -miny * scale + (h - contentH * scale) / 2.0;
        } else {
            tx = (w - getWidth()*scale) / 2.0;
            ty = (h - getHeight()*scale) / 2.0;
        }

        g2.translate(tx, ty);
        g2.scale(scale, scale);
      
        for (Figure f : objList) {
            f.paint(g2);
        }
        g2.dispose();

        javax.imageio.ImageIO.write(bi, "PNG", new java.io.File(fname));
    }

    

    @SuppressWarnings("unchecked")
    public void load(String fname){
        try {
            FileInputStream fis = new FileInputStream(fname);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object o = ois.readObject();
            if (o instanceof ArrayList) objList = (ArrayList<Figure>) o;
            ois.close();
            fis.close();
        } catch(IOException e){
        } catch(ClassNotFoundException e){
        }
        repaint();
    }

    private int getHandleAt(double bx, double by, double bw, double bh, double mx, double my) {
        double cx = bx + bw/2.0;
        double cy = by + bh/2.0;
        double[][] pts = new double[][]{
            {bx, by}, {cx, by}, {bx + bw, by}, {bx + bw, cy}, {bx + bw, by + bh}, {cx, by + bh}, {bx, by + bh}, {bx, cy}
        };
        for (int i = 0; i < pts.length; i++) {
            double hx = pts[i][0], hy = pts[i][1];
            if (Math.abs(mx - hx) <= HS && Math.abs(my - hy) <= HS) return i + 1;
        }
        return 0;
    }
}
