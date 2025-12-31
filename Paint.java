package ninth;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * ペイントアプリケーションのメインウィンドウクラス
 * JFrame を継承し、UI コンポーネント（タブ、ボタン、ラジオボタン）を管理
 * 実際の描画処理は PaintCanvas に委譲
 */
public class Paint extends JFrame {
    // UI コンポーネント
    JButton clearBtn;    // 全体消去ボタン
    JButton endBtn;      // 終了ボタン
    ButtonGroup bg;      // 描画モードのラジオボタングループ
    
    // 描画モードのラジオボタン
    JRadioButton r0,  // 選択モード
                 r1,  // 丸（ドット）
                 r2,  // 円
                 r3,  // 四角形
                 r4,  // 線
                 r5,  // 楕円
                 r6,  // フリーハンド
                 r7,  // 消しゴム
                 r8;  // 折れ線

    /**
     * メインメソッド：アプリケーションのエントリーポイント
     * @param args コマンドライン引数（ファイル名を指定可能）
     */
    public static void main(String[] args) {
        Paint p = new Paint();
        PaintCanvas canvas;
        String fname = null;

        // コマンドライン引数からファイル名を取得
        if(args.length == 1) fname = args[0];
        p.setTitle("Paint");

        // 描画キャンバスを作成（白背景）
        canvas = new PaintCanvas(p, fname);
        canvas.setBackground(Color.WHITE);

        // === タブ式リボンUIの構築（Microsoft Word風） ===
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // タブバーの作成
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        tabBar.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        JToggleButton tabFile = new JToggleButton("ファイル");
        JToggleButton tabMode = new JToggleButton("モード");
        JToggleButton tabOpt = new JToggleButton("オプション");
        JToggleButton tabEdit = new JToggleButton("編集");
        
        // タブボタンをグループ化（排他選択）
        ButtonGroup tabGroup = new ButtonGroup();
        tabGroup.add(tabFile); tabGroup.add(tabMode); tabGroup.add(tabOpt); tabGroup.add(tabEdit);
        tabBar.add(tabFile); tabBar.add(tabMode); tabBar.add(tabOpt); tabBar.add(tabEdit);

        // タブごとの内容パネルをカード形式で管理
        JPanel ribbonContent = new JPanel(new CardLayout());

        // === ファイルタブのパネル ===
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBorder(BorderFactory.createTitledBorder("ファイル"));
        JButton openBtn = new JButton("開く");           // ファイル読み込み
        JButton saveBtn = new JButton("保存");           // ファイル保存
        JButton exportBtn = new JButton("PNG出力");      // PNG画像として出力
        
        // 保存サイズの設定用スピナー（幅・高さ）
        SpinnerNumberModel wModel = new SpinnerNumberModel(800, 16, 8192, 16);
        SpinnerNumberModel hModel = new SpinnerNumberModel(600, 16, 8192, 16);
        JSpinner widthSpinner = new JSpinner(wModel);
        JSpinner heightSpinner = new JSpinner(hModel);
        JLabel sizeLabel = new JLabel("保存サイズ: 800 x 600");
        
        filePanel.add(openBtn);
        filePanel.add(saveBtn);
        filePanel.add(exportBtn);
        filePanel.add(new JLabel("幅:")); filePanel.add(widthSpinner);
        filePanel.add(new JLabel("高:")); filePanel.add(heightSpinner);
        filePanel.add(sizeLabel);
        ribbonContent.add(filePanel, "file");

        // === モードタブのパネル ===
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.setBorder(BorderFactory.createTitledBorder("モード"));
        
        // 各描画モードのラジオボタンを作成（デフォルト：選択モード）
        p.r0 = new JRadioButton("選択", true);
        p.r1 = new JRadioButton("丸");
        p.r2 = new JRadioButton("円");
        p.r1.setSelected(false);  // 丸の初期選択を解除
        p.r3 = new JRadioButton("四角");
        p.r4 = new JRadioButton("線");
        p.r5 = new JRadioButton("楕円");
        p.r6 = new JRadioButton("フリーハンド");
        p.r7 = new JRadioButton("消しゴム");
        p.r8 = new JRadioButton("折れ線");
        
        // すべてのモードボタンをグループ化（排他選択）
        p.bg = new ButtonGroup();
        p.bg.add(p.r0); p.bg.add(p.r1); p.bg.add(p.r2); p.bg.add(p.r3); 
        p.bg.add(p.r4); p.bg.add(p.r5); p.bg.add(p.r6); p.bg.add(p.r7); p.bg.add(p.r8);
        
        modePanel.add(p.r0); modePanel.add(p.r1); modePanel.add(p.r2); modePanel.add(p.r3); 
        modePanel.add(p.r4); modePanel.add(p.r5); modePanel.add(p.r6); modePanel.add(p.r7); modePanel.add(p.r8);
        ribbonContent.add(modePanel, "mode");

        // === オプションタブのパネル ===
        JPanel optPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optPanel.setBorder(BorderFactory.createTitledBorder("オプション"));
        JButton colorBtn = new JButton("色選択");  // 描画色選択
        optPanel.add(colorBtn);
        ribbonContent.add(optPanel, "opt");

        // === 編集タブのパネル ===
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editPanel.setBorder(BorderFactory.createTitledBorder("編集"));
        p.clearBtn = new JButton("全体消去");  // すべての図形を削除
        p.endBtn = new JButton("終了");        // アプリケーション終了
        editPanel.add(p.clearBtn);
        editPanel.add(p.endBtn);
        ribbonContent.add(editPanel, "edit");

        // タブバーとコンテンツを結合
        topPanel.add(tabBar, BorderLayout.NORTH);
        topPanel.add(ribbonContent, BorderLayout.CENTER);
        p.getContentPane().add(topPanel, BorderLayout.NORTH);

        // === イベントリスナーの登録 ===
        // キャンバスにボタンイベントを接続
        p.clearBtn.addActionListener(canvas);
        p.endBtn.addActionListener(canvas);
        openBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(p) == JFileChooser.APPROVE_OPTION) {
                canvas.load(fc.getSelectedFile().getAbsolutePath());
            }
        });
        saveBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(p) == JFileChooser.APPROVE_OPTION) {
                canvas.save(fc.getSelectedFile().getAbsolutePath());
            }
        });
        ChangeListener sizeChange = ev -> {
            int w = (Integer) widthSpinner.getValue();
            int h = (Integer) heightSpinner.getValue();
            sizeLabel.setText("保存サイズ: " + w + " x " + h);
            canvas.setExportSize(w, h);
        };
        widthSpinner.addChangeListener(sizeChange);
        heightSpinner.addChangeListener(sizeChange);

        tabFile.addActionListener(ev -> ((CardLayout)ribbonContent.getLayout()).show(ribbonContent, "file"));
        tabMode.addActionListener(ev -> ((CardLayout)ribbonContent.getLayout()).show(ribbonContent, "mode"));
        tabOpt.addActionListener(ev -> ((CardLayout)ribbonContent.getLayout()).show(ribbonContent, "opt"));
        tabEdit.addActionListener(ev -> ((CardLayout)ribbonContent.getLayout()).show(ribbonContent, "edit"));
        
        tabFile.setSelected(true);
        ((CardLayout)ribbonContent.getLayout()).show(ribbonContent, "file");

        exportBtn.addActionListener(ev -> {
            JFileChooser fc2 = new JFileChooser();
            if (fc2.showSaveDialog(p) == JFileChooser.APPROVE_OPTION) {
                String path = fc2.getSelectedFile().getAbsolutePath();
                // ensure .png
                if (!path.toLowerCase().endsWith(".png")) path += ".png";
                int w = (Integer) widthSpinner.getValue();
                int h = (Integer) heightSpinner.getValue();
                try {
                    canvas.exportImage(path, w, h);
                    JOptionPane.showMessageDialog(p, "PNG を出力しました: " + path);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(p, "出力に失敗しました: " + ex.getMessage());
                }
            }
        });
        colorBtn.addActionListener(ev -> {
            Color c = JColorChooser.showDialog(p, "色を選択", Color.BLACK);
            if (c != null) canvas.setSelectedColor(c);
        });

        // オプション: 塗り・線幅・Undo/Redo
        JCheckBox fillCheck = new JCheckBox("塗り");
        JSpinner strokeSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.5, 50.0, 0.5));
        optPanel.add(new JLabel("線幅:"));
        optPanel.add(strokeSpinner);
        optPanel.add(fillCheck);
        fillCheck.addActionListener(ev -> canvas.setFilled(fillCheck.isSelected()));
        strokeSpinner.addChangeListener(ev -> {
            double v = (Double) strokeSpinner.getValue();
            canvas.setStrokeWidth((float)v);
        });

        JButton undoBtn = new JButton("Undo");
        JButton redoBtn = new JButton("Redo");
        editPanel.add(undoBtn);
        editPanel.add(redoBtn);
        undoBtn.addActionListener(ev -> canvas.undo());
        redoBtn.addActionListener(ev -> canvas.redo());
        p.getContentPane().add(canvas, BorderLayout.CENTER);

        p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.setSize(900, 600);
        p.setLocationRelativeTo(null);
        p.setVisible(true);
    }
}
