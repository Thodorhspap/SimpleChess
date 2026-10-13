package main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

// Ο υπολογιστής-αντίπαλος: ψάχνει κινήσεις μπροστά με minimax + alpha-beta κλάδεμα
public class Computer {
    static final int MATE = 100000;
    static final int INF = 1000000;
    static final int QUIESCE_DEPTH = 4;

    private final GamePanel gp;
    private final Random random = new Random();
    // έτοιμες βασίλισσες για τις προαγωγές μέσα στο ψάξιμο (για να μη φορτώνουμε εικόνες συνέχεια)
    private final ArrayDeque<piece> spareWhiteQueens = new ArrayDeque<>();
    private final ArrayDeque<piece> spareBlackQueens = new ArrayDeque<>();

    static class Move {
        piece p;
        int fromCol, fromRow, toCol, toRow;
        piece captured;
        boolean wasMoved;
        piece prevEnPassant;
        piece rook;
        int rookFromCol;
        piece promotedTo;
        int order;
    }

    public Computer(GamePanel gp) {
        this.gp = gp;
        for (int i = 0; i < 2; i++) {
            spareWhiteQueens.push(newSpareQueen(GamePanel.WHITE));
            spareBlackQueens.push(newSpareQueen(GamePanel.BLACK));
        }
    }

    private piece newSpareQueen(int color) {
        piece q = new Queen(color, 0, 0);
        q.moved = true;
        return q;
    }

    // level: 1 = εύκολο (βάθος 1), 2 = μέτριο (βάθος 3), 3 = δύσκολο (βάθος 4)
    public Move findBestMove(int color, int level) {
        int depth = level == 1 ? 1 : level + 1;
        int randomness = level == 1 ? 60 : 8;
        boolean quiesce = level > 1;

        List<Move> moves = generateMoves(color, false);
        if (moves.isEmpty()) {
            return null;
        }
        Collections.shuffle(moves, random);   // για να μην παίζει πάντα το ίδιο παιχνίδι
        sortMoves(moves);

        Move best = null;
        int bestScore = -INF;       // το πραγματικό σκορ της καλύτερης κίνησης
        int bestNoisy = -INF;       // το ίδιο με λίγη τύχη, για να διαλέγει ανάμεσα σε σχεδόν ίσες κινήσεις
        for (Move m : moves) {
            // κινήσεις κοντά στην καλύτερη παίρνουν ακριβές σκορ, οι χειρότερες απλώς απορρίπτονται
            int floor = bestScore == -INF ? -INF : bestScore - 2 * randomness;
            make(m);
            int score = -negamax(depth - 1, -INF, -floor, 1 - color, 1, quiesce);
            undo(m);
            if (score <= floor) {
                continue;
            }
            int noisy = score + random.nextInt(2 * randomness + 1) - randomness;
            if (noisy > bestNoisy) {
                bestNoisy = noisy;
                best = m;
            }
            if (score > bestScore) {
                bestScore = score;
            }
        }
        return best;
    }

    private int negamax(int depth, int alpha, int beta, int color, int ply, boolean quiesce) {
        if (depth == 0) {
            return quiesce ? quiesce(alpha, beta, color, QUIESCE_DEPTH) : evaluate(color);
        }
        List<Move> moves = generateMoves(color, false);
        if (moves.isEmpty()) {
            // ματ (όσο πιο γρήγορο τόσο καλύτερο) ή πατ
            return gp.isKingInCheck(color) ? -MATE + ply : 0;
        }
        sortMoves(moves);
        for (Move m : moves) {
            make(m);
            int score = -negamax(depth - 1, -beta, -alpha, 1 - color, ply + 1, quiesce);
            undo(m);
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    // συνεχίζει μόνο με αλλαγές κομματιών, για να μη σταματάει το ψάξιμο στη μέση μιας ανταλλαγής
    private int quiesce(int alpha, int beta, int color, int qDepth) {
        int standPat = evaluate(color);
        if (standPat >= beta) {
            return beta;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }
        if (qDepth == 0) {
            return alpha;
        }
        List<Move> captures = generateMoves(color, true);
        sortMoves(captures);
        for (Move m : captures) {
            make(m);
            int score = -quiesce(-beta, -alpha, 1 - color, qDepth - 1);
            undo(m);
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    List<Move> generateMoves(int color, boolean capturesOnly) {
        List<Move> moves = new ArrayList<>();
        List<piece> own = new ArrayList<>();
        List<piece> enemies = new ArrayList<>();
        for (piece p : GamePanel.pieces) {
            (p.color == color ? own : enemies).add(p);
        }
        for (piece p : own) {
            if (capturesOnly) {
                for (piece e : enemies) {
                    addIfLegal(moves, p, e.col, e.row);
                }
            } else {
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        addIfLegal(moves, p, col, row);
                    }
                }
            }
        }
        return moves;
    }

    private void addIfLegal(List<Move> moves, piece p, int col, int row) {
        if (!gp.isLegalMove(p, col, row)) {
            return;
        }
        Move m = new Move();
        m.p = p;
        m.fromCol = p.col;
        m.fromRow = p.row;
        m.toCol = col;
        m.toRow = row;
        m.captured = gp.getCapturedPiece(p, col, row);
        // πρώτα οι αλλαγές όπου φτηνό κομμάτι τρώει ακριβό - βοηθάει πολύ το κλάδεμα
        if (m.captured != null) {
            m.order = 10 * value(m.captured.type) - value(p.type);
        }
        if (p.type == Type.PAWN && (row == 0 || row == 7)) {
            m.order += 8000;
        }
        moves.add(m);
    }

    private void sortMoves(List<Move> moves) {
        moves.sort(Comparator.comparingInt((Move m) -> m.order).reversed());
    }

    // κάνει την κίνηση μόνο στη "λογική" του ταμπλό (όχι στις συντεταγμένες της οθόνης)
    void make(Move m) {
        piece p = m.p;
        m.wasMoved = p.moved;
        m.prevEnPassant = GamePanel.enPassantPawn;

        if (m.captured != null) {
            GamePanel.pieces.remove(m.captured);
        }
        m.rook = null;
        if (p.type == Type.KING && Math.abs(m.toCol - m.fromCol) == 2) {
            m.rookFromCol = m.toCol > m.fromCol ? 7 : 0;
            m.rook = GamePanel.getPieceAt(m.rookFromCol, m.fromRow);
            m.rook.col = m.toCol > m.fromCol ? m.toCol - 1 : m.toCol + 1;
            m.rook.moved = true;
        }
        GamePanel.enPassantPawn = (p.type == Type.PAWN && Math.abs(m.toRow - m.fromRow) == 2) ? p : null;

        p.col = m.toCol;
        p.row = m.toRow;
        p.moved = true;

        m.promotedTo = null;
        if (p.type == Type.PAWN && (m.toRow == 0 || m.toRow == 7)) {
            ArrayDeque<piece> spares = p.color == GamePanel.WHITE ? spareWhiteQueens : spareBlackQueens;
            piece q = spares.isEmpty() ? newSpareQueen(p.color) : spares.pop();
            q.col = m.toCol;
            q.row = m.toRow;
            GamePanel.pieces.remove(p);
            GamePanel.pieces.add(q);
            m.promotedTo = q;
        }
    }

    void undo(Move m) {
        piece p = m.p;
        if (m.promotedTo != null) {
            GamePanel.pieces.remove(m.promotedTo);
            (p.color == GamePanel.WHITE ? spareWhiteQueens : spareBlackQueens).push(m.promotedTo);
            GamePanel.pieces.add(p);
        }
        p.col = m.fromCol;
        p.row = m.fromRow;
        p.moved = m.wasMoved;
        if (m.rook != null) {
            m.rook.col = m.rookFromCol;
            m.rook.moved = false;
        }
        GamePanel.enPassantPawn = m.prevEnPassant;
        if (m.captured != null) {
            GamePanel.pieces.add(m.captured);
        }
    }

    // ---------------- Αξιολόγηση θέσης ----------------

    static int value(Type type) {
        switch (type) {
            case PAWN: return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK: return 500;
            case QUEEN: return 900;
            default: return 0;
        }
    }

    // σκορ από τη μεριά του color: θετικό = καλύτερα για αυτόν
    int evaluate(int color) {
        int heavyMaterial = 0;
        for (piece p : GamePanel.pieces) {
            if (p.type != Type.PAWN && p.type != Type.KING) {
                heavyMaterial += value(p.type);
            }
        }
        boolean endgame = heavyMaterial <= 1300;

        int score = 0;
        for (piece p : GamePanel.pieces) {
            // οι πίνακες είναι γραμμένοι για τα λευκά, για τα μαύρα τους καθρεφτίζουμε
            int sq = p.color == GamePanel.WHITE ? p.row * 8 + p.col : (7 - p.row) * 8 + p.col;
            int s = value(p.type) + positionBonus(p.type, sq, endgame);
            score += p.color == GamePanel.WHITE ? s : -s;
        }
        return color == GamePanel.WHITE ? score : -score;
    }

    private int positionBonus(Type type, int sq, boolean endgame) {
        switch (type) {
            case PAWN: return PAWN_TABLE[sq];
            case KNIGHT: return KNIGHT_TABLE[sq];
            case BISHOP: return BISHOP_TABLE[sq];
            case ROOK: return ROOK_TABLE[sq];
            case QUEEN: return QUEEN_TABLE[sq];
            default: return endgame ? KING_END_TABLE[sq] : KING_TABLE[sq];
        }
    }

    // πίνακες θέσης ("Simplified Evaluation Function"), η πρώτη γραμμή είναι η 8η σειρά
    private static final int[] PAWN_TABLE = {
             0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
             5,  5, 10, 25, 25, 10,  5,  5,
             0,  0,  0, 20, 20,  0,  0,  0,
             5, -5,-10,  0,  0,-10, -5,  5,
             5, 10, 10,-20,-20, 10, 10,  5,
             0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] KNIGHT_TABLE = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] BISHOP_TABLE = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] ROOK_TABLE = {
             0,  0,  0,  0,  0,  0,  0,  0,
             5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
             0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] QUEEN_TABLE = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
             -5,  0,  5,  5,  5,  5,  0, -5,
              0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] KING_TABLE = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
             20, 20,  0,  0,  0,  0, 20, 20,
             20, 30, 10,  0,  0, 10, 30, 20
    };
    private static final int[] KING_END_TABLE = {
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };
}
