package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.IntConsumer;

public class GamePanel extends JPanel {
    public static final int Width = 1100;
    public static final int Height = 720;
    public static final int WHITE=0;
    public static final int BLACK=1;
    int currentColor=WHITE;

    public static ArrayList<piece> pieces = new ArrayList<>();
    public static piece enPassantPawn;      // το πιόνι που μόλις έκανε διπλό βήμα (τρώγεται en passant)
    ArrayList<piece> capturedPieces = new ArrayList<>();
    ArrayList<Point> validSquares = new ArrayList<>();
    ArrayList<piece> promotionChoices = new ArrayList<>();
    Board board = new Board();
    Mouse mouse = new Mouse(this);

    piece selectedP;        // το κομμάτι που έχει διαλέξει ο παίκτης
    boolean dragging;
    piece promotionP;       // πιόνι που έφτασε στο τέλος και περιμένει να διαλέξουμε τι θα γίνει
    boolean inCheck;
    int checkCol=-1, checkRow=-1;
    boolean gameOver;
    String statusText="";
    int lastFromCol=-1, lastFromRow=-1, lastToCol=-1, lastToRow=-1;

    // παιχνίδι με τον υπολογιστή
    Computer computer = new Computer(this);
    boolean vsComputer=true;
    int computerColor=BLACK;
    int playerColor=WHITE;
    int level=2;                    // 1 εύκολο, 2 μέτριο, 3 δύσκολο
    volatile boolean aiThinking;
    ArrayList<piece> drawSnapshot = new ArrayList<>();  // τι ζωγραφίζουμε όσο ο υπολογιστής ψάχνει

    // μενού πριν από κάθε παιχνίδι
    boolean inMenu=true;
    int playAs=0;                   // 0 λευκά, 1 μαύρα, 2 τυχαία
    boolean flipped;                // γυρισμένο ταμπλό όταν παίζουμε με τα μαύρα
    int hoverX=-1, hoverY=-1;
    Random random = new Random();

    // τέλος παιχνιδιού
    int winner=-1;                  // -1 = ισοπαλία
    String endReason="";
    ArrayList<int[]> confetti = new ArrayList<>();  // x, y, χρώμα, σχήμα

    static final int BoardSize = Board.SquareSize*8;
    static final int PanelX = BoardSize+22;
    static final int PromoY = 290;
    static final int PromoSize = 80;
    static final int IconSize = 40;

    public GamePanel() {
        setPreferredSize(new Dimension(Width, Height));
        setBackground(new Color(28,26,30));

        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        // R ή Esc = πίσω στο μενού, Enter = ξεκίνα από το μενού
        bindKey(KeyEvent.VK_R,"menu",this::backToMenu);
        bindKey(KeyEvent.VK_ESCAPE,"menuEsc",this::backToMenu);
        bindKey(KeyEvent.VK_ENTER,"start",() -> {
            if (inMenu){
                startGame();
            }
        });

        newGame();
    }

    void bindKey(int key,String name,Runnable action){
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key,0),name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    void startGame(){
        playerColor = playAs==2 ? random.nextInt(2) : playAs;
        computerColor = 1-playerColor;
        flipped = vsComputer && playerColor==BLACK;
        inMenu=false;
        newGame();
    }

    void backToMenu(){
        // περιμένουμε να τελειώσει ο υπολογιστής, γιατί το ψάξιμο αλλάζει τη λίστα με τα κομμάτια
        if (aiThinking || inMenu){
            return;
        }
        inMenu=true;
        selectedP=null;
        dragging=false;
        validSquares.clear();
        repaint();
    }

    public void newGame() {
        if (aiThinking){
            return;
        }
        pieces.clear();
        setPieces();
        capturedPieces.clear();
        validSquares.clear();
        promotionChoices.clear();
        enPassantPawn=null;
        selectedP=null;
        promotionP=null;
        dragging=false;
        currentColor=WHITE;
        inCheck=false;
        checkCol=checkRow=-1;
        gameOver=false;
        winner=-1;
        endReason="";
        confetti.clear();
        statusText="";
        lastFromCol=lastFromRow=lastToCol=lastToRow=-1;
        startComputerMoveIfNeeded();
        repaint();
    }

    public void setPieces() {
        pieces.add(new Pawn(WHITE,0,6));
        pieces.add(new Pawn(WHITE,1,6));
        pieces.add(new Pawn(WHITE,2,6));
        pieces.add(new Pawn(WHITE,3,6));
        pieces.add(new Pawn(WHITE,4,6));
        pieces.add(new Pawn(WHITE,5,6));
        pieces.add(new Pawn(WHITE,6,6));
        pieces.add(new Pawn(WHITE,7,6));
        pieces.add(new Rook(WHITE,0,7));
        pieces.add(new Rook(WHITE,7,7));
        pieces.add(new Knight(WHITE,1,7));
        pieces.add(new Knight(WHITE,6,7));
        pieces.add(new King(WHITE,4,7));
        pieces.add(new Queen(WHITE,3 ,7));
        pieces.add(new Bishop(WHITE,2,7));
        pieces.add(new Bishop(WHITE,5,7));

        pieces.add(new Pawn(BLACK,0,1));
        pieces.add(new Pawn(BLACK,1,1));
        pieces.add(new Pawn(BLACK,2,1));
        pieces.add(new Pawn(BLACK,3,1));
        pieces.add(new Pawn(BLACK,4,1));
        pieces.add(new Pawn(BLACK,5,1));
        pieces.add(new Pawn(BLACK,6,1));
        pieces.add(new Pawn(BLACK,7,1));
        pieces.add(new Rook(BLACK,0,0));
        pieces.add(new Rook(BLACK,7,0));
        pieces.add(new Knight(BLACK,6,0));
        pieces.add(new Knight(BLACK,1,0));
        pieces.add(new King(BLACK,4,0));
        pieces.add(new Queen(BLACK,3 ,0));
        pieces.add(new Bishop(BLACK,5,0));
        pieces.add(new Bishop(BLACK,2,0));
    }

    public static piece getPieceAt(int col,int row){
        for (piece p : pieces){
            if (p.col==col && p.row==row){
                return p;
            }
        }
        return null;
    }

    // ---------------- Ποντίκι ----------------

    // όταν το ταμπλό είναι γυρισμένο, οθόνη <-> ταμπλό είναι απλώς καθρέφτισμα (ισχύει και ανάποδα)
    int toScreen(int v){
        return flipped ? BoardSize-Board.SquareSize-v : v;
    }

    int toBoardSquare(int screenPos){
        int sq = screenPos/Board.SquareSize;
        return flipped ? 7-sq : sq;
    }

    void mouseMoved(int mx,int my){
        hoverX=mx;
        hoverY=my;
        if (inMenu || gameOver){
            repaint();
        }
    }

    void mousePressed(int mx,int my){
        if (inMenu){
            menuClick(mx,my);
            return;
        }
        if (gameOver){
            if (returnButton().contains(mx,my)){
                backToMenu();
            }
            return;
        }
        if (aiThinking || isComputerTurn()){
            return;
        }
        if (promotionP!=null){
            choosePromotion(mx,my);
            return;
        }
        if (mx<0 || my<0 || mx>=BoardSize || my>=BoardSize){
            return;
        }
        int col = toBoardSquare(mx);
        int row = toBoardSquare(my);

        // δεύτερο κλικ σε έγκυρο τετράγωνο -> κίνηση
        if (selectedP!=null && validSquares.contains(new Point(col,row))){
            movePiece(selectedP,col,row);
            repaint();
            return;
        }

        piece p = getPieceAt(col,row);
        if (p!=null && p.color==currentColor){
            selectedP=p;
            dragging=true;
            findValidSquares(p);
            p.x=toScreen(mx-Board.Half_Square_Size);
            p.y=toScreen(my-Board.Half_Square_Size);
        }else{
            selectedP=null;
            validSquares.clear();
        }
        repaint();
    }

    void mouseDragged(int mx,int my){
        if (dragging && selectedP!=null){
            selectedP.x=toScreen(mx-Board.Half_Square_Size);
            selectedP.y=toScreen(my-Board.Half_Square_Size);
            repaint();
        }
    }

    void mouseReleased(int mx,int my){
        if (inMenu || !dragging || selectedP==null){
            return;
        }
        dragging=false;
        int col = toBoardSquare(mx);
        int row = toBoardSquare(my);
        boolean onBoard = mx>=0 && my>=0 && mx<BoardSize && my<BoardSize;

        if (onBoard && validSquares.contains(new Point(col,row))){
            movePiece(selectedP,col,row);
        }else{
            // μένει επιλεγμένο, για να μπορεί να κάνει κίνηση με δεύτερο κλικ
            selectedP.resetPosition();
        }
        repaint();
    }

    // ---------------- Κανόνες ----------------

    void findValidSquares(piece p){
        validSquares.clear();
        for (int row=0; row<8; row++){
            for (int col=0; col<8; col++){
                if (isLegalMove(p,col,row)){
                    validSquares.add(new Point(col,row));
                }
            }
        }
    }

    // έγκυρη κίνηση = ακολουθεί τους κανόνες του κομματιού και δεν αφήνει τον βασιλιά μας σε σαχ
    boolean isLegalMove(piece p,int targetCol,int targetRow){
        if (!p.canMove(targetCol,targetRow)){
            return false;
        }

        // στο ροκέ ο βασιλιάς δεν μπορεί να ξεκινάει ή να περνάει από τετράγωνο που απειλείται
        if (p.type==Type.KING && Math.abs(targetCol-p.col)==2){
            int enemy = 1-p.color;
            if (isSquareAttacked(p.col,p.row,enemy) || isSquareAttacked((p.col+targetCol)/2,p.row,enemy)){
                return false;
            }
        }

        // δοκιμάζουμε την κίνηση, ελέγχουμε για σαχ και την αναιρούμε
        int oldCol=p.col;
        int oldRow=p.row;
        piece captured = getCapturedPiece(p,targetCol,targetRow);
        if (captured!=null){
            pieces.remove(captured);
        }
        p.col=targetCol;
        p.row=targetRow;

        boolean safe = !isKingInCheck(p.color);

        p.col=oldCol;
        p.row=oldRow;
        if (captured!=null){
            pieces.add(captured);
        }
        return safe;
    }

    // το κομμάτι που θα φαγωθεί (μαζί με το en passant, όπου το θύμα δεν είναι στο τετράγωνο-στόχο)
    piece getCapturedPiece(piece p,int targetCol,int targetRow){
        piece captured = getPieceAt(targetCol,targetRow);
        if (captured==null && p.type==Type.PAWN && targetCol!=p.col){
            captured = enPassantPawn;
        }
        return captured;
    }

    boolean isSquareAttacked(int col,int row,int byColor){
        for (piece p : pieces){
            if (p.color==byColor && p.attacks(col,row)){
                return true;
            }
        }
        return false;
    }

    piece getKing(int color){
        for (piece p : pieces){
            if (p.type==Type.KING && p.color==color){
                return p;
            }
        }
        return null;
    }

    boolean isKingInCheck(int color){
        piece king = getKing(color);
        return king!=null && isSquareAttacked(king.col,king.row,1-color);
    }

    boolean hasAnyLegalMove(int color){
        // αντίγραφο της λίστας, γιατί το isLegalMove αφαιρεί/ξαναβάζει κομμάτια
        for (piece p : new ArrayList<>(pieces)){
            if (p.color!=color){
                continue;
            }
            for (int row=0; row<8; row++){
                for (int col=0; col<8; col++){
                    if (isLegalMove(p,col,row)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // μόνο βασιλιάδες, ή βασιλιάδες και ένας αξιωματικός/ίππος -> κανείς δεν μπορεί να κάνει ματ
    boolean isInsufficientMaterial(){
        ArrayList<piece> others = new ArrayList<>();
        for (piece p : pieces){
            if (p.type!=Type.KING){
                others.add(p);
            }
        }
        if (others.isEmpty()){
            return true;
        }
        return others.size()==1 && (others.get(0).type==Type.BISHOP || others.get(0).type==Type.KNIGHT);
    }

    void movePiece(piece p,int targetCol,int targetRow){
        piece captured = getCapturedPiece(p,targetCol,targetRow);
        if (captured!=null){
            pieces.remove(captured);
            capturedPieces.add(captured);
        }

        // ροκέ: μετακινούμε και τον πύργο
        if (p.type==Type.KING && Math.abs(targetCol-p.col)==2){
            int rookCol = targetCol>p.col ? 7 : 0;
            piece rook = getPieceAt(rookCol,p.row);
            rook.col = targetCol>p.col ? targetCol-1 : targetCol+1;
            rook.updatePosition();
        }

        enPassantPawn = (p.type==Type.PAWN && Math.abs(targetRow-p.row)==2) ? p : null;

        lastFromCol=p.col;
        lastFromRow=p.row;
        lastToCol=targetCol;
        lastToRow=targetRow;

        p.col=targetCol;
        p.row=targetRow;
        p.updatePosition();

        selectedP=null;
        dragging=false;
        validSquares.clear();

        // προαγωγή: περιμένουμε τον παίκτη να διαλέξει κομμάτι
        if (p.type==Type.PAWN && (targetRow==0 || targetRow==7)){
            promotionP=p;
            promotionChoices.clear();
            promotionChoices.add(new Queen(p.color,p.col,p.row));
            promotionChoices.add(new Rook(p.color,p.col,p.row));
            promotionChoices.add(new Bishop(p.color,p.col,p.row));
            promotionChoices.add(new Knight(p.color,p.col,p.row));
            statusText="Choose a piece";
            return;
        }
        endTurn();
    }

    void choosePromotion(int mx,int my){
        for (int i=0; i<promotionChoices.size(); i++){
            Rectangle box = new Rectangle(PanelX+i*(PromoSize+6),PromoY,PromoSize,PromoSize);
            if (box.contains(mx,my)){
                promote(promotionChoices.get(i));
                repaint();
                return;
            }
        }
    }

    void promote(piece newPiece){
        newPiece.updatePosition();
        pieces.remove(promotionP);
        pieces.add(newPiece);
        promotionP=null;
        promotionChoices.clear();
        endTurn();
    }

    boolean isComputerTurn(){
        return vsComputer && currentColor==computerColor;
    }

    void startComputerMoveIfNeeded(){
        if (inMenu || !isComputerTurn() || gameOver || aiThinking || promotionP!=null){
            return;
        }
        aiThinking=true;
        selectedP=null;
        validSquares.clear();
        // το ψάξιμο αλλάζει προσωρινά τη λίστα, οπότε ζωγραφίζουμε ένα αντίγραφο
        drawSnapshot = new ArrayList<>(pieces);

        Thread thinker = new Thread(() -> {
            long start = System.currentTimeMillis();
            Computer.Move best = computer.findBestMove(computerColor,level);
            // μικρή παύση για να μη φαίνεται η κίνηση ακαριαία
            long elapsed = System.currentTimeMillis()-start;
            if (elapsed<400){
                try {
                    Thread.sleep(400-elapsed);
                } catch (InterruptedException ignored) {
                }
            }
            SwingUtilities.invokeLater(() -> {
                aiThinking=false;
                if (best!=null){
                    movePiece(best.p,best.toCol,best.toRow);
                    if (promotionP!=null){
                        promote(new Queen(computerColor,promotionP.col,promotionP.row));
                    }
                }
                repaint();
            });
        });
        thinker.setDaemon(true);
        thinker.start();
        repaint();
    }

    void endTurn(){
        currentColor = 1-currentColor;
        inCheck = isKingInCheck(currentColor);
        piece king = getKing(currentColor);
        checkCol = inCheck && king!=null ? king.col : -1;
        checkRow = inCheck && king!=null ? king.row : -1;

        if (!hasAnyLegalMove(currentColor)){
            if (inCheck){
                endGame(1-currentColor,"by checkmate");
                statusText = "Checkmate! " + winnerName(winner);
            }else{
                endGame(-1,"Stalemate - nobody can move");
                statusText = "Stalemate - draw";
            }
        }else if (isInsufficientMaterial()){
            endGame(-1,"Not enough pieces left to checkmate");
            statusText="Draw - not enough pieces";
        }else{
            statusText = inCheck ? "Check!" : "";
        }
        startComputerMoveIfNeeded();
    }

    // winner = -1 σημαίνει ισοπαλία
    void endGame(int winnerColor,String reason){
        gameOver=true;
        winner=winnerColor;
        endReason=reason;
        confetti.clear();
        if (isCelebration()){
            // κομφετί γύρω από την κάρτα (σταθερά, υπολογίζονται μία φορά)
            for (int i=0; i<90; i++){
                int x = random.nextInt(BoardSize);
                int y = random.nextInt(BoardSize);
                if (cardBox().contains(x,y)){
                    continue;
                }
                confetti.add(new int[]{x,y,random.nextInt(CONFETTI_COLORS.length),random.nextInt(4)});
            }
        }
    }

    // στο παιχνίδι με τον υπολογιστή, συγχαρητήρια μόνο όταν κερδίζει ο παίκτης
    boolean isCelebration(){
        return winner>=0 && (!vsComputer || winner==playerColor);
    }

    String gameOverTitle(){
        if (!isCelebration()){
            return "Good game!";
        }
        if (vsComputer){
            return "Congratulations!";
        }
        return "Congratulations, "+colorName(winner)+"!";
    }

    String gameOverMessage(){
        if (winner<0){
            return endReason;
        }
        if (vsComputer){
            String[] levelNames = {"Easy","Medium","Hard"};
            return winner==playerColor
                    ? "You beat the computer on "+levelNames[level-1]+"!"
                    : "The computer won this time - try again!";
        }
        return colorName(winner)+" wins "+endReason;
    }

    String colorName(int color){
        return color==WHITE ? "White" : "Black";
    }

    String winnerName(int winner){
        if (vsComputer){
            return winner==playerColor ? "You win" : "Computer wins";
        }
        return colorName(winner)+" wins";
    }

    // ---------------- Σχεδίαση ----------------

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)  g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        board.draw(g2);
        if (!inMenu){
            drawHighlights(g2);
        }

        ArrayList<piece> toDraw = aiThinking ? drawSnapshot : pieces;
        for (piece p : toDraw){
            if (!(dragging && p==selectedP)){
                drawPiece(g2,p);
            }
        }
        // το κομμάτι που σέρνουμε ζωγραφίζεται τελευταίο για να φαίνεται από πάνω
        if (dragging && selectedP!=null){
            drawPiece(g2,selectedP);
        }

        if (inMenu){
            drawMenu(g2);
        }else{
            drawSidePanel(g2);
            if (gameOver){
                drawGameOver(g2);
            }
        }
    }

    Rectangle cardBox(){
        return new Rectangle(BoardSize/2-240,BoardSize/2-140,480,280);
    }

    Rectangle returnButton(){
        Rectangle card = cardBox();
        return new Rectangle(card.x+card.width/2-130,card.y+card.height-90,260,60);
    }

    static final Color[] CONFETTI_COLORS = {
            new Color(255,200,0), new Color(230,70,70), new Color(80,170,255),
            new Color(90,200,110), new Color(240,240,240), new Color(200,120,255)
    };

    void drawGameOver(Graphics2D g2){
        // σκουραίνουμε το ταμπλό για να ξεχωρίζει η κάρτα
        g2.setColor(new Color(0,0,0,120));
        g2.fillRect(0,0,BoardSize,BoardSize);

        for (int[] c : confetti){
            g2.setColor(CONFETTI_COLORS[c[2]]);
            switch (c[3]){
                case 0: g2.fillRect(c[0],c[1],10,5); break;
                case 1: g2.fillRect(c[0],c[1],5,10); break;
                case 2: g2.fillOval(c[0],c[1],8,8); break;
                default: g2.fillRect(c[0],c[1],7,7); break;
            }
        }

        Rectangle card = cardBox();
        g2.setColor(new Color(28,26,30,240));
        g2.fillRoundRect(card.x,card.y,card.width,card.height,24,24);
        g2.setColor(isCelebration() ? new Color(255,200,0) : new Color(125,78,40));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(card.x,card.y,card.width,card.height,24,24);
        g2.setStroke(new BasicStroke(1));

        int centerX = card.x+card.width/2;
        g2.setFont(new Font("SansSerif",Font.BOLD,40));
        g2.setColor(isCelebration() ? new Color(255,200,0) : Color.WHITE);
        drawCenteredAt(g2,gameOverTitle(),centerX,card.y+75);

        g2.setFont(new Font("SansSerif",Font.PLAIN,21));
        g2.setColor(Color.LIGHT_GRAY);
        drawCenteredAt(g2,gameOverMessage(),centerX,card.y+125);

        Rectangle b = returnButton();
        boolean hover = b.contains(hoverX,hoverY);
        g2.setColor(hover ? new Color(255,215,60) : new Color(255,200,0));
        g2.fillRoundRect(b.x,b.y,b.width,b.height,16,16);
        g2.setColor(new Color(30,20,10));
        g2.setFont(new Font("SansSerif",Font.BOLD,24));
        FontMetrics fm = g2.getFontMetrics();
        drawCenteredAt(g2,"Return to menu",centerX,b.y+(b.height+fm.getAscent()-fm.getDescent())/2);
    }

    void drawCenteredAt(Graphics2D g2,String text,int centerX,int y){
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text,centerX-fm.stringWidth(text)/2,y);
    }

    void drawPiece(Graphics2D g2,piece p){
        p.draw(g2,toScreen(p.x),toScreen(p.y),Board.SquareSize);
    }

    void fillSquare(Graphics2D g2,int col,int row,Color color){
        g2.setColor(color);
        int s = Board.SquareSize;
        g2.fillRect(toScreen(col*s),toScreen(row*s),s,s);
    }

    void drawHighlights(Graphics2D g2){
        int s = Board.SquareSize;

        if (lastFromCol>=0){
            fillSquare(g2,lastFromCol,lastFromRow,new Color(255,200,0,70));
            fillSquare(g2,lastToCol,lastToRow,new Color(255,200,0,110));
        }

        if (checkCol>=0){
            fillSquare(g2,checkCol,checkRow,new Color(255,40,40,150));
        }

        if (selectedP==null){
            return;
        }
        fillSquare(g2,selectedP.preCol,selectedP.preRow,new Color(255,255,255,60));

        // κουκκίδα για άδειο τετράγωνο, δαχτυλίδι εκεί που τρώμε
        for (Point sq : validSquares){
            int sx = toScreen(sq.x*s);
            int sy = toScreen(sq.y*s);
            int cx = sx+s/2;
            int cy = sy+s/2;
            g2.setColor(new Color(40,20,5,110));
            if (getCapturedPiece(selectedP,sq.x,sq.y)!=null){
                g2.setStroke(new BasicStroke(5));
                g2.drawOval(sx+5,sy+5,s-10,s-10);
                g2.setStroke(new BasicStroke(1));
            }else{
                g2.fillOval(cx-13,cy-13,26,26);
            }
        }
    }

    void drawSidePanel(Graphics2D g2){
        int x = PanelX;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif",Font.BOLD,36));
        g2.drawString("SimpleChess",x,62);

        g2.setColor(new Color(255,200,0));
        g2.setFont(new Font("SansSerif",Font.PLAIN,18));
        String[] levelNames = {"Easy","Medium","Hard"};
        g2.drawString(vsComputer ? "vs Computer  -  "+levelNames[level-1] : "2 players",x,92);

        // ποιος παίζει
        g2.setFont(new Font("SansSerif",Font.PLAIN,24));
        if (!gameOver){
            g2.setColor(currentColor==WHITE ? Color.WHITE : Color.BLACK);
            g2.fillRect(x,118,24,24);
            g2.setColor(Color.GRAY);
            g2.drawRect(x,118,24,24);
            g2.setColor(Color.WHITE);
            String turnText;
            if (isComputerTurn()){
                turnText = "Computer is thinking...";
            }else if (vsComputer){
                turnText = "Your move";
            }else{
                turnText = colorName(currentColor)+" to move";
            }
            g2.drawString(turnText,x+36,139);
        }

        if (!statusText.isEmpty()){
            g2.setFont(new Font("SansSerif",Font.BOLD,26));
            g2.setColor(gameOver ? new Color(255,200,0) : new Color(255,90,90));
            g2.drawString(statusText,x,200);
        }

        if (promotionP!=null){
            for (int i=0; i<promotionChoices.size(); i++){
                int bx = x+i*(PromoSize+6);
                g2.setColor(new Color(125,78,40));
                g2.fillRoundRect(bx,PromoY,PromoSize,PromoSize,12,12);
                promotionChoices.get(i).draw(g2,bx+5,PromoY+5,PromoSize-10);
            }
        }

        drawCaptured(g2,"Captured by White",BLACK,410);
        drawCaptured(g2,"Captured by Black",WHITE,520);

        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif",Font.PLAIN,16));
        g2.drawString("Drag a piece, or click it and then a square",x,680);
        g2.drawString(gameOver ? "R or Esc - menu / play again" : "R or Esc - back to menu",x,702);
    }

    // ---------------- Μενού ----------------

    static class MenuButton {
        final Rectangle box;
        final String label;
        final boolean selected;
        final boolean enabled;
        final Runnable action;

        MenuButton(Rectangle box,String label,boolean selected,boolean enabled,Runnable action){
            this.box=box;
            this.label=label;
            this.selected=selected;
            this.enabled=enabled;
            this.action=action;
        }
    }

    static final int MenuRow1 = 250, MenuRow2 = 370, MenuRow3 = 490, StartY = 600;

    // τα κουμπιά υπολογίζονται κάθε φορά από τις τρέχουσες επιλογές
    ArrayList<MenuButton> menuButtons(){
        ArrayList<MenuButton> list = new ArrayList<>();
        addButtonRow(list,MenuRow1,new String[]{"vs Computer","2 Players"},vsComputer ? 0 : 1,true,i -> vsComputer = i==0);
        addButtonRow(list,MenuRow2,new String[]{"Easy","Medium","Hard"},level-1,vsComputer,i -> level = i+1);
        addButtonRow(list,MenuRow3,new String[]{"White","Black","Random"},playAs,vsComputer,i -> playAs = i);
        list.add(new MenuButton(new Rectangle(Width/2-160,StartY,320,66),"Start game",true,true,this::startGame));
        return list;
    }

    void addButtonRow(ArrayList<MenuButton> list,int y,String[] labels,int selected,boolean enabled,IntConsumer onPick){
        int w=200, gap=20;
        int total = labels.length*w+(labels.length-1)*gap;
        int x0 = Width/2-total/2;
        for (int i=0; i<labels.length; i++){
            int index = i;
            Rectangle box = new Rectangle(x0+i*(w+gap),y,w,56);
            list.add(new MenuButton(box,labels[i],i==selected,enabled,() -> onPick.accept(index)));
        }
    }

    void menuClick(int mx,int my){
        for (MenuButton b : menuButtons()){
            if (b.enabled && b.box.contains(mx,my)){
                b.action.run();
                repaint();
                return;
            }
        }
    }

    void drawMenu(Graphics2D g2){
        // σκουραίνουμε όλο το παράθυρο, το ταμπλό φαίνεται από πίσω
        g2.setColor(new Color(15,12,18,215));
        g2.fillRect(0,0,Width,Height);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif",Font.BOLD,64));
        drawCentered(g2,"SimpleChess",130);
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif",Font.PLAIN,20));
        drawCentered(g2,"Choose how you want to play",170);

        g2.setFont(new Font("SansSerif",Font.PLAIN,18));
        g2.setColor(Color.LIGHT_GRAY);
        drawCentered(g2,"Mode",MenuRow1-14);
        g2.setColor(vsComputer ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        drawCentered(g2,"Difficulty",MenuRow2-14);
        drawCentered(g2,"Play as",MenuRow3-14);

        Color accent = new Color(125,78,40);
        for (MenuButton b : menuButtons()){
            boolean hover = b.enabled && b.box.contains(hoverX,hoverY);
            boolean isStart = b.label.equals("Start game");
            Color fill;
            if (!b.enabled){
                fill = new Color(40,38,44);
            }else if (isStart){
                fill = hover ? new Color(255,215,60) : new Color(255,200,0);
            }else if (b.selected){
                fill = hover ? accent.brighter() : accent;
            }else{
                fill = hover ? new Color(70,66,76) : new Color(50,47,55);
            }
            g2.setColor(fill);
            g2.fillRoundRect(b.box.x,b.box.y,b.box.width,b.box.height,16,16);
            if (b.selected && b.enabled && !isStart){
                g2.setColor(new Color(255,200,0));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(b.box.x,b.box.y,b.box.width,b.box.height,16,16);
                g2.setStroke(new BasicStroke(1));
            }

            g2.setFont(new Font("SansSerif",Font.BOLD,isStart ? 28 : 22));
            if (!b.enabled){
                g2.setColor(Color.GRAY.darker());
            }else{
                g2.setColor(isStart ? new Color(30,20,10) : Color.WHITE);
            }
            FontMetrics fm = g2.getFontMetrics();
            int tx = b.box.x+(b.box.width-fm.stringWidth(b.label))/2;
            int ty = b.box.y+(b.box.height+fm.getAscent()-fm.getDescent())/2;
            g2.drawString(b.label,tx,ty);
        }

        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif",Font.PLAIN,16));
        drawCentered(g2,"Enter - start      R or Esc during a game - back to this menu",700);
    }

    void drawCentered(Graphics2D g2,String text,int y){
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text,(Width-fm.stringWidth(text))/2,y);
    }

    void drawCaptured(Graphics2D g2,String title,int capturedColor,int y){
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("SansSerif",Font.PLAIN,18));
        g2.drawString(title,PanelX,y);
        int i=0;
        for (piece p : capturedPieces){
            if (p.color==capturedColor){
                int ix = PanelX+(i%8)*(IconSize+2);
                int iy = y+10+(i/8)*(IconSize+2);
                p.draw(g2,ix,iy,IconSize);
                i++;
            }
        }
    }
}
