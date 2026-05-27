package com.furkan.odev_2211012611;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

//**********************************************************
// ADAM ASMACA OYUNU - Ana pencere sinifi
// Ogrenci No: 2211012611
// Bu sinif oyunun tum mantigini icerir
//**********************************************************
public class NewJFrame extends JFrame {

    //-----------------------------------------------------
    // DOSYA YOLLARI (sinif degiskenleri)
    // Windows'ta C:\P2Oyun, Linux'ta ~/P2Oyun kullanilir
    //-----------------------------------------------------
    public static final String BASE_PATH;
    public static final String RESIM_PATH;
    public static final String TXT_PATH;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            BASE_PATH = "C:\\P2Oyun";
        } else {
            BASE_PATH = System.getProperty("user.home") + File.separator + "P2Oyun";
        }
        RESIM_PATH = BASE_PATH + File.separator + "Resimler";
        TXT_PATH = BASE_PATH + File.separator + "TXTDosyalar";
    }

    //-----------------------------------------------------
    // DOSYA ADLARI (sabit)
    //-----------------------------------------------------
    private static final String SIFRE_FILE = "sifre.txt";
    private static final String KELIMELER_FILE = "kelimeler.txt";
    private static final String LOG_FILE = "log.txt";
    private static final String OYUNLAR_FILE = "oyunlar.txt";

    //-----------------------------------------------------
    // MENU BILESENLERI
    //-----------------------------------------------------
    private JMenuBar menuBar;
    private JMenu oyunMenu;
    private JMenuItem oyunaBaslaMenuItem;
    private JMenuItem oyunuYenidenBaslatMenuItem;
    private JMenuItem cikisMenuItem;

    //-----------------------------------------------------
    // TAB PANEL BILESENLERI
    //-----------------------------------------------------
    private JTabbedPane tabbedPane;
    private JPanel oyunPanel;      // Oyun Oyna sekmesi
    private JPanel skorPanel;      // Eski Skorlar sekmesi
    private JPanel logPanel;       // Log Kayitlari sekmesi

    //-----------------------------------------------------
    // OYUN ALANI BILESENLERI (Oyun Oyna sekmesi)
    //-----------------------------------------------------
    private JPanel resimPanel;     // WEST - adam asmaca resimleri
    private JPanel oyunAlanPanel;  // CENTER - oyun alani
    private JPanel tahminPanel;    // SOUTH - tahmin girisi
    private JLabel resimLabel;     // adam asmaca resminin gosterilecegi label
    private JPanel kelimePanel;    // harflerin dinamik olarak eklendigi panel
    private JPanel durumPanel;     // sure ve kalan hak bilgisi
    private JLabel zamanLabel;     // oyun suresi (saniye)
    private JLabel kalanHakLabel;  // kalan yanlis hakki
    private ArrayList<JLabel> harfLabelList;  // her harf icin bir label
    private JTextField harfTahminField;       // harf tahmini girisi
    private JButton harfTahminButon;          // harf tahmin butonu
    private JTextField kelimeTahminField;     // kelime tahmini girisi
    private JButton kelimeTahminButon;        // kelime tahmin butonu
    private JLabel durumLabel;                // alt kisimda durum mesaji

    //-----------------------------------------------------
    // SKOR TABLOSU BILESENLERI (Eski Skorlar sekmesi)
    //-----------------------------------------------------
    private JScrollPane skorScrollPane;
    private JTable skorTablosu;
    private JPanel skorButonPanel;
    private JButton skorTemizleButon;

    //-----------------------------------------------------
    // LOG TABLOSU BILESENLERI (Log Kayitlari sekmesi)
    //-----------------------------------------------------
    private JScrollPane logScrollPane;
    private JTable logTablosu;
    private JPanel logButonPanel;
    private JButton logTemizleButon;

    //-----------------------------------------------------
    // OYUN DURUM DEGISKENLERI
    //-----------------------------------------------------
    private List<String> words;             // kelime listesi
    private String currentWord;             // su anki kelime
    private StringBuilder guessedLetters;   // tahmin edilen harfler
    private int wrongCount;                 // yanlis sayisi
    private static final int MAX_WRONG = 11; // maksimum yanlis hakki
    private boolean gameActive;             // oyun aktif mi?
    private Timer gameTimer;                // zaman sayaci
    private int gameSeconds;                // oyun suresi (saniye)
    private String password;                // dogru sifre
    private int passwordAttempts;           // sifre deneme sayisi
    private ImageIcon[] hangmanImages;      // adam asmaca resimleri

    //===========================================================
    // YAPICI METOD
    //===========================================================
    public NewJFrame() {
        // once gerekli klasorleri olustur
        createDirectories();
        
        // resimleri yukle (1.jpg - 11.jpg arasi)
        hangmanImages = loadHangmanImages();
        
        // arayuz bilesenlerini olustur
        initComponents();
        
        // menu ve buton aksiyonlarini tanimla
        setupMenuActions();

        // sifre kontrolu yap, dogru degilse kapat
        if (!handlePassword()) {
            System.exit(0);
        }

        // kelimeleri yukle ve tablolari doldur
        loadWords();
        loadLogTable();
        loadScoreTable();
    }

    //===========================================================
    // KLASOR VE DOSYA OLUSTURMA
    //===========================================================
    private void createDirectories() {
        try {
            // once klasorleri olustur
            Files.createDirectories(Paths.get(RESIM_PATH));
            Files.createDirectories(Paths.get(TXT_PATH));
            
            // eger kelimeler.txt yoksa olustur
            File kelimelerFile = new File(TXT_PATH, KELIMELER_FILE);
            if (!kelimelerFile.exists()) {
                createKelimelerFile(kelimelerFile);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Dizinler olusturulamadi: " + e.getMessage(),
                "Hata", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // kelimeler.txt dosyasini olusturur
    private void createKelimelerFile(File file) throws IOException {
        String[] words = {
            "BILGISAYAR", "KLAVYE", "PROGRAM", "INTERNET", "YAZILIM",
            "DONANIM", "VERITABANI", "ALGORITMA", "DERLEYICI", "SIFRELEME",
            "GUVENLIK", "ARAYUZ", "ISLETIM", "SISTEM", "MIMARI",
            "SUNUCU", "DEPOLAMA", "ISLEMCI", "BELLEK", "YAZICI",
            "TARAYICI", "BAGLANTI", "YONLENDIRICI", "UYGULAMA", "COZUMLEME",
            "ANAHTAR", "HESAPLAMA", "KODLAMA", "OTOMASYON", "SIMULASYON"
        };
        Files.write(file.toPath(), Arrays.asList(words), StandardCharsets.UTF_8);
    }

    //===========================================================
    // RESIM YUKLEME
    //===========================================================
    private ImageIcon[] loadHangmanImages() {
        ImageIcon[] images = new ImageIcon[MAX_WRONG + 1];
        for (int i = 0; i <= MAX_WRONG; i++) {
            // dosya adi: i=0 icin 1.jpg, i=10 icin 11.jpg, i=11 icin 11.jpg
            int fileIndex = Math.min(i + 1, MAX_WRONG);
            File imgFile = new File(RESIM_PATH, fileIndex + ".jpg");
            if (imgFile.exists()) {
                ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(220, 300, Image.SCALE_SMOOTH);
                images[i] = new ImageIcon(img);
            } else {
                // resim yoksa bos bir resim goster
                images[i] = new ImageIcon();
            }
        }
        return images;
    }

    //===========================================================
    // ARAYUZ BILESENLERI
    //===========================================================
    private void initComponents() {
        // ana pencere ayarlari
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Adam Asmaca Oyunu");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        //-------------------------------------------------------
        // MENU BAR
        //-------------------------------------------------------
        menuBar = new JMenuBar();
        oyunMenu = new JMenu("Oyun");
        oyunaBaslaMenuItem = new JMenuItem("Oyuna Basla");
        oyunuYenidenBaslatMenuItem = new JMenuItem("Oyunu Yeniden Baslat");
        cikisMenuItem = new JMenuItem("Cikis");

        oyunMenu.add(oyunaBaslaMenuItem);
        oyunMenu.add(oyunuYenidenBaslatMenuItem);
        oyunMenu.addSeparator();
        oyunMenu.add(cikisMenuItem);
        menuBar.add(oyunMenu);
        setJMenuBar(menuBar);

        //-------------------------------------------------------
        // TABBEDPANE - 3 sekme
        //-------------------------------------------------------
        tabbedPane = new JTabbedPane();

        // 1. sekme: Oyun Oyna
        oyunPanel = new JPanel(new BorderLayout(10, 10));
        oyunPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildOyunPanel();

        // 2. sekme: Eski Skorlar
        skorPanel = new JPanel(new BorderLayout(10, 10));
        skorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildSkorPanel();

        // 3. sekme: Log Kayitlari
        logPanel = new JPanel(new BorderLayout(10, 10));
        logPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildLogPanel();

        tabbedPane.addTab("Oyun Oyna", oyunPanel);
        tabbedPane.addTab("Eski Skorlar", skorPanel);
        tabbedPane.addTab("Log Kayitlari", logPanel);

        add(tabbedPane, BorderLayout.CENTER);
        setVisible(true);
    }

    //-------------------------------------------------------
    // OYUN OYNA PANELI
    //-------------------------------------------------------
    private void buildOyunPanel() {
        // sol taraf - adam asmaca resimleri
        resimPanel = new JPanel(new BorderLayout());
        resimPanel.setPreferredSize(new Dimension(220, 310));
        resimPanel.setBorder(BorderFactory.createTitledBorder("Adam Asmaca"));
        resimLabel = new JLabel();
        resimLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resimLabel.setPreferredSize(new Dimension(220, 300));
        resimLabel.setIcon(hangmanImages[0]);
        resimPanel.add(resimLabel, BorderLayout.CENTER);

        // orta alan - kelime ve durum bilgileri
        oyunAlanPanel = new JPanel(new BorderLayout(10, 10));

        // kelime harflerinin gosterilecegi panel (dinamik label'lar)
        kelimePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        kelimePanel.setBorder(BorderFactory.createTitledBorder("Kelime"));
        harfLabelList = new ArrayList<>();

        // durum paneli - sure ve kalan hak
        durumPanel = new JPanel(new GridLayout(1, 2));
        zamanLabel = new JLabel("Sure: 0 sn", SwingConstants.CENTER);
        zamanLabel.setFont(new Font("Arial", Font.BOLD, 14));
        kalanHakLabel = new JLabel("Kalan Hak: 11", SwingConstants.CENTER);
        kalanHakLabel.setFont(new Font("Arial", Font.BOLD, 14));
        durumPanel.add(zamanLabel);
        durumPanel.add(kalanHakLabel);

        oyunAlanPanel.add(kelimePanel, BorderLayout.CENTER);
        oyunAlanPanel.add(durumPanel, BorderLayout.SOUTH);

        // alt kisim - tahmin girisi
        tahminPanel = new JPanel(new GridBagLayout());
        tahminPanel.setBorder(BorderFactory.createTitledBorder("Tahmin"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);

        // 1. satir: harf tahmini
        gbc.gridx = 0; gbc.gridy = 0;
        tahminPanel.add(new JLabel("Harf Tahmini:"), gbc);
        gbc.gridx = 1;
        harfTahminField = new JTextField(5);
        harfTahminField.setHorizontalAlignment(JTextField.CENTER);
        tahminPanel.add(harfTahminField, gbc);
        gbc.gridx = 2;
        harfTahminButon = new JButton("Tahmin Et");
        tahminPanel.add(harfTahminButon, gbc);

        // 2. satir: kelime tahmini
        gbc.gridx = 0; gbc.gridy = 1;
        tahminPanel.add(new JLabel("Kelime Tahmini:"), gbc);
        gbc.gridx = 1;
        kelimeTahminField = new JTextField(15);
        kelimeTahminField.setHorizontalAlignment(JTextField.CENTER);
        tahminPanel.add(kelimeTahminField, gbc);
        gbc.gridx = 2;
        kelimeTahminButon = new JButton("Tahmin Et");
        tahminPanel.add(kelimeTahminButon, gbc);

        // 3. satir: durum mesaji
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        durumLabel = new JLabel("Oyuna baslamak icin 'Oyuna Basla' menusunu kullanin.", SwingConstants.CENTER);
        durumLabel.setFont(new Font("Arial", Font.BOLD, 13));
        tahminPanel.add(durumLabel, gbc);

        // paneli birlestir
        oyunPanel.add(resimPanel, BorderLayout.WEST);
        oyunPanel.add(oyunAlanPanel, BorderLayout.CENTER);
        oyunPanel.add(tahminPanel, BorderLayout.SOUTH);
    }

    //-------------------------------------------------------
    // SKOR PANELI (Eski Skorlar)
    //-------------------------------------------------------
    private void buildSkorPanel() {
        skorScrollPane = new JScrollPane();
        skorTablosu = new JTable(new DefaultTableModel(new Object[]{"Tarih", "Sure (sn)", "Sonuc"}, 0));
        skorScrollPane.setViewportView(skorTablosu);
        skorPanel.add(skorScrollPane, BorderLayout.CENTER);

        // temizle butonu
        skorButonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        skorTemizleButon = new JButton("Temizle");
        skorButonPanel.add(skorTemizleButon);
        skorPanel.add(skorButonPanel, BorderLayout.SOUTH);
        skorTemizleButon.addActionListener(e -> temizleSkorlar());
    }

    //-------------------------------------------------------
    // LOG PANELI (Log Kayitlari)
    //-------------------------------------------------------
    private void buildLogPanel() {
        logScrollPane = new JScrollPane();
        logTablosu = new JTable(new DefaultTableModel(new Object[]{"Tarih", "Islem"}, 0));
        logScrollPane.setViewportView(logTablosu);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // temizle butonu
        logButonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logTemizleButon = new JButton("Temizle");
        logButonPanel.add(logTemizleButon);
        logPanel.add(logButonPanel, BorderLayout.SOUTH);
        logTemizleButon.addActionListener(e -> temizleLog());
    }

    //===========================================================
    // MENU VE BUTON AKSIYONLARI
    //===========================================================
    private void setupMenuActions() {
        // menu ogeleri
        oyunaBaslaMenuItem.addActionListener(e -> oyunaBasla());
        oyunuYenidenBaslatMenuItem.addActionListener(e -> oyunuYenidenBaslat());
        cikisMenuItem.addActionListener(e -> {
            stopTimer();
            System.exit(0);
        });

        // harf ve kelime tahmin butonlari
        harfTahminButon.addActionListener(e -> harfTahmin());
        harfTahminField.addActionListener(e -> harfTahmin());
        kelimeTahminButon.addActionListener(e -> kelimeTahmin());
        kelimeTahminField.addActionListener(e -> kelimeTahmin());
    }

    //===========================================================
    // SIFRE ISLEMLERI
    //===========================================================
    // sifre dosyasini kontrol eder, yoksa olusturur, varsa dogrular
    private boolean handlePassword() {
        File sifreFile = new File(TXT_PATH, SIFRE_FILE);
        if (!sifreFile.exists()) {
            return createPassword(sifreFile);
        } else {
            return verifyPassword(sifreFile);
        }
    }

    // yeni sifre belirleme ekrani
    private boolean createPassword(File sifreFile) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JPasswordField pass1 = new JPasswordField(15);
        JPasswordField pass2 = new JPasswordField(15);
        panel.add(new JLabel("Yeni Sifre:"));
        panel.add(pass1);
        panel.add(new JLabel("Sifre Tekrar:"));
        panel.add(pass2);

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, panel,
                "Sifre Belirleme", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
            String p1 = new String(pass1.getPassword());
            String p2 = new String(pass2.getPassword());
            if (p1.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Sifre bos olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
                pass1.setText(""); pass2.setText("");
                continue;
            }
            if (!p1.equals(p2)) {
                JOptionPane.showMessageDialog(this, "Sifreler eslesmiyor!", "Hata", JOptionPane.ERROR_MESSAGE);
                pass1.setText(""); pass2.setText("");
                continue;
            }
            try {
                Files.writeString(sifreFile.toPath(), p1, StandardCharsets.UTF_8);
                logKayit("Sifre olusturuldu");
                return true;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Sifre kaydedilemedi: " + e.getMessage(),
                    "Hata", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }

    // sifre dogrulama ekrani (3 hak)
    private boolean verifyPassword(File sifreFile) {
        try {
            password = Files.readString(sifreFile.toPath(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Sifre dosyasi okunamadi!", "Hata", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        passwordAttempts = 0;
        JPasswordField passField = new JPasswordField(15);

        while (passwordAttempts < 3) {
            JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
            panel.add(new JLabel("Sifre:"));
            panel.add(passField);

            int result = JOptionPane.showConfirmDialog(this, panel,
                "Sifre Girisi (" + (3 - passwordAttempts) + " hakkiniz kaldi)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                logKayit("Giris iptal edildi");
                return false;
            }

            String girilen = new String(passField.getPassword());
            passField.setText("");

            if (girilen.equals(password)) {
                logKayit("Giris basarili");
                return true;
            } else {
                passwordAttempts++;
                logKayit("Hatali sifre denemesi (" + passwordAttempts + "/3)");
                if (passwordAttempts >= 3) {
                    logKayit("3 hatali giris - program sonlandirildi");
                    JOptionPane.showMessageDialog(this,
                        "3 kez hatali sifre girdiniz! Program sonlandiriliyor.",
                        "Hata", JOptionPane.ERROR_MESSAGE);
                    return false;
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Hatali sifre! Kalan hakkiniz: " + (3 - passwordAttempts),
                        "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return false;
    }

    //===========================================================
    // LOG KAYIT
    // Tum girisler tarih/saat ile log.txt dosyasina yazilir
    //===========================================================
    private void logKayit(String islem) {
        try {
            String zaman = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
            String satir = zaman + " - " + islem;
            Files.writeString(Paths.get(TXT_PATH, LOG_FILE),
                satir + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Log yazilamadi: " + e.getMessage());
        }
    }

    //===========================================================
    // KELIME YUKLEME
    // kelimeler.txt dosyasindan kelimeleri okur
    //===========================================================
    private void loadWords() {
        words = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(TXT_PATH, KELIMELER_FILE), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim().toUpperCase(Locale.ENGLISH);
                if (!trimmed.isEmpty() && trimmed.length() >= 6) {
                    words.add(trimmed);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Kelimeler dosyasi okunamadi: " + e.getMessage(),
                "Hata", JOptionPane.ERROR_MESSAGE);
        }
        // eger dosyadan okunamadiysa yedek kelimeler
        if (words.isEmpty()) {
            words.add("BILGISAYAR");
            words.add("PROGRAM");
            words.add("INTERNET");
        }
    }

    //===========================================================
    // OYUN BASLATMA
    //===========================================================
    private void oyunaBasla() {
        if (words.isEmpty()) {
            durumLabel.setText("Kelime listesi bos!");
            return;
        }
        
        // rastgele bir kelime sec
        Random rnd = new Random();
        currentWord = words.get(rnd.nextInt(words.size()));
        guessedLetters = new StringBuilder();
        wrongCount = 0;
        gameSeconds = 0;
        gameActive = true;

        // kelimenin her harfi icin _ koy
        for (int i = 0; i < currentWord.length(); i++) {
            guessedLetters.append('_');
        }

        // dinamik label'lari olustur
        kelimePanel.removeAll();
        harfLabelList.clear();
        for (int i = 0; i < currentWord.length(); i++) {
            JLabel lbl = new JLabel("*", SwingConstants.CENTER);
            lbl.setFont(new Font("Monospaced", Font.BOLD, 28));
            lbl.setPreferredSize(new Dimension(35, 40));
            lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            lbl.setOpaque(true);
            lbl.setBackground(new Color(240, 240, 240));
            harfLabelList.add(lbl);
            kelimePanel.add(lbl);
        }
        kelimePanel.revalidate();
        kelimePanel.repaint();

        // arayuzu sifirla
        resimLabel.setIcon(hangmanImages[0]);
        kalanHakLabel.setText("Kalan Hak: " + (MAX_WRONG - wrongCount));
        zamanLabel.setText("Sure: 0 sn");
        durumLabel.setText("Oyun basladi! Harf veya kelime tahmini yapin.");
        harfTahminField.setEnabled(true);
        harfTahminButon.setEnabled(true);
        kelimeTahminField.setEnabled(true);
        kelimeTahminButon.setEnabled(true);
        harfTahminField.requestFocus();

        // zamanlayiciyi baslat (her saniye calisir)
        stopTimer();
        gameTimer = new Timer("GameTimer", true);
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (gameActive) {
                    gameSeconds++;
                    SwingUtilities.invokeLater(() ->
                        zamanLabel.setText("Sure: " + gameSeconds + " sn"));
                }
            }
        }, 1000, 1000);
    }

    // oyunu yeniden baslat (timer'i durdur ve yeniden baslat)
    private void oyunuYenidenBaslat() {
        stopTimer();
        oyunaBasla();
    }

    // zamanlayiciyi durdur
    private void stopTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
    }

    //===========================================================
    // HARF TAHMINI
    //===========================================================
    private void harfTahmin() {
        if (!gameActive) {
            durumLabel.setText("Oyun aktif degil! 'Oyuna Basla' menusunu kullanin.");
            return;
        }
        
        // kullanicinin girdigi harfi al
        String girdi = harfTahminField.getText().trim().toUpperCase(Locale.ENGLISH);
        harfTahminField.setText("");
        
        // sadece tek harf kontrolu
        if (girdi.length() != 1) {
            durumLabel.setText("Lutfen tek bir harf girin!");
            return;
        }
        char harf = girdi.charAt(0);

        // bu harf daha once tahmin edilmis mi?
        boolean zatenTahmin = false;
        for (int i = 0; i < currentWord.length(); i++) {
            if (guessedLetters.charAt(i) != '_' && guessedLetters.charAt(i) == harf) {
                zatenTahmin = true;
                break;
            }
        }

        // harf kelimede var mi?
        boolean bulundu = false;
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == harf) {
                guessedLetters.setCharAt(i, harf);
                harfLabelList.get(i).setText(String.valueOf(harf));
                bulundu = true;
            }
        }

        // eger zaten tahmin edildiyse uyari ver
        if (zatenTahmin) {
            durumLabel.setText("Bu harfi zaten tahmin ettiniz!");
            return;
        }

        // dogru veya yanlis durumuna gore islem yap
        if (bulundu) {
            durumLabel.setText("Dogru! '" + harf + "' harfi kelimede var.");
            if (kazandiMi()) {
                oyunBitti(true);
            }
        } else {
            wrongCount++;
            durumLabel.setText("Yanlis! '" + harf + "' harfi kelimede yok.");
            resimLabel.setIcon(hangmanImages[Math.min(wrongCount, MAX_WRONG)]);
            kalanHakLabel.setText("Kalan Hak: " + (MAX_WRONG - wrongCount));
            if (wrongCount >= MAX_WRONG) {
                oyunBitti(false);
            }
        }
    }

    //===========================================================
    // KELIME TAHMINI
    //===========================================================
    private void kelimeTahmin() {
        if (!gameActive) {
            durumLabel.setText("Oyun aktif degil! 'Oyuna Basla' menusunu kullanin.");
            return;
        }
        
        // kullanicinin girdigi kelimeyi al
        String tahmin = kelimeTahminField.getText().trim().toUpperCase(Locale.ENGLISH);
        kelimeTahminField.setText("");
        
        if (tahmin.isEmpty()) {
            durumLabel.setText("Lutfen bir kelime girin!");
            return;
        }

        // dogru tahmin mi kontrol et
        if (tahmin.equals(currentWord)) {
            // tum harfleri goster
            for (int i = 0; i < currentWord.length(); i++) {
                guessedLetters.setCharAt(i, currentWord.charAt(i));
                harfLabelList.get(i).setText(String.valueOf(currentWord.charAt(i)));
            }
            durumLabel.setText("Tebrikler! Kelimeyi dogru tahmin ettiniz!");
            oyunBitti(true);
        } else {
            wrongCount++;
            durumLabel.setText("Yanlis! '" + tahmin + "' dogru kelime degil.");
            resimLabel.setIcon(hangmanImages[Math.min(wrongCount, MAX_WRONG)]);
            kalanHakLabel.setText("Kalan Hak: " + (MAX_WRONG - wrongCount));
            if (wrongCount >= MAX_WRONG) {
                oyunBitti(false);
            }
        }
    }

    //===========================================================
    // OYUN BITIS KONTROLLERI
    //===========================================================
    // tum harfler bulundu mu?
    private boolean kazandiMi() {
        return guessedLetters.indexOf("_") == -1;
    }

    // oyun bittiginde yapilacak islemler
    private void oyunBitti(boolean kazandi) {
        gameActive = false;
        stopTimer();
        harfTahminField.setEnabled(false);
        harfTahminButon.setEnabled(false);
        kelimeTahminField.setEnabled(false);
        kelimeTahminButon.setEnabled(false);

        String sonuc = kazandi ? "Kazandi" : "Kaybetti";
        
        if (!kazandi) {
            // kaybedince dogru kelimeyi goster
            for (int i = 0; i < currentWord.length(); i++) {
                harfLabelList.get(i).setText(String.valueOf(currentWord.charAt(i)));
            }
            durumLabel.setText("Kaybettiniz! Dogru kelime: " + currentWord);
        } else {
            durumLabel.setText("Tebrikler! Kazandiniz! Sure: " + gameSeconds + " sn");
        }

        // oyunu dosyaya kaydet ve tabloyu guncelle
        oyunKaydet(sonuc);
        loadScoreTable();
    }

    //===========================================================
    // DOSYA ISLEMLERI
    //===========================================================
    // oyun sonucunu oyunlar.txt dosyasina yaz
    private void oyunKaydet(String sonuc) {
        try {
            String zaman = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
            String satir = zaman + " - Sure: " + gameSeconds + " sn - " + sonuc;
            Files.writeString(Paths.get(TXT_PATH, OYUNLAR_FILE),
                satir + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Oyun kaydedilemedi: " + e.getMessage());
        }
    }

    // skor tablosunu guncelle
    private void loadScoreTable() {
        DefaultTableModel model = (DefaultTableModel) skorTablosu.getModel();
        model.setRowCount(0);
        try {
            File file = new File(TXT_PATH, OYUNLAR_FILE);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(" - ");
                    if (parts.length >= 3) {
                        model.addRow(new Object[]{
                            parts[0],
                            parts[1].replace("Sure: ", "").replace(" sn", ""),
                            parts[2]
                        });
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Skorlar yuklenemedi: " + e.getMessage());
        }
    }

    // log tablosunu guncelle
    private void loadLogTable() {
        DefaultTableModel model = (DefaultTableModel) logTablosu.getModel();
        model.setRowCount(0);
        try {
            File file = new File(TXT_PATH, LOG_FILE);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(" - ", 2);
                    if (parts.length >= 2) {
                        model.addRow(new Object[]{parts[0], parts[1]});
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Loglar yuklenemedi: " + e.getMessage());
        }
    }

    //===========================================================
    // TEMIZLEME ISLEMLERI (sifre korumali)
    //===========================================================
    // skorlari temizle
    private void temizleSkorlar() {
        if (sifreDogrumu()) {
            try {
                Files.writeString(Paths.get(TXT_PATH, OYUNLAR_FILE), "", StandardCharsets.UTF_8);
                loadScoreTable();
                JOptionPane.showMessageDialog(this, "Skorlar temizlendi.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Temizleme hatasi: " + e.getMessage(),
                    "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // loglari temizle
    private void temizleLog() {
        if (sifreDogrumu()) {
            try {
                Files.writeString(Paths.get(TXT_PATH, LOG_FILE), "", StandardCharsets.UTF_8);
                loadLogTable();
                JOptionPane.showMessageDialog(this, "Loglar temizlendi.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Temizleme hatasi: " + e.getMessage(),
                    "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // sifre dogrulama (temizleme islemleri icin)
    private boolean sifreDogrumu() {
        JPasswordField passField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(new JLabel("Sifre:"));
        panel.add(passField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Sifre Dogrulama", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String girilen = new String(passField.getPassword());
            if (girilen.equals(password)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "Hatali sifre!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    //===========================================================
    // MAIN METODU
    //===========================================================
    public static void main(String args[]) {
        // Nimbus tema ayari
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // GUI baslat
        EventQueue.invokeLater(() -> new NewJFrame().setVisible(true));
    }
}
