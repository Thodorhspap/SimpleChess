package main;

public class King extends piece {
    public King(int color,int col,int row) {
        super(color, col, row);
        type = Type.KING;

        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w-king");
        }else{
            image = getImage("/piece/b-king");
        }
    }

    @Override
    public boolean attacks(int targetCol,int targetRow){
        return Math.max(Math.abs(targetCol-col),Math.abs(targetRow-row))==1;
    }

    @Override
    public boolean canMove(int targetCol,int targetRow){
        return super.canMove(targetCol,targetRow) || canCastle(targetCol,targetRow);
    }

    // ροκέ: ο βασιλιάς πάει δύο τετράγωνα προς έναν πύργο που δεν έχει κουνηθεί.
    // Το αν περνάει από σαχ το ελέγχει το GamePanel.
    public boolean canCastle(int targetCol,int targetRow){
        if (moved || targetRow!=row || Math.abs(targetCol-col)!=2){
            return false;
        }
        int rookCol = targetCol>col ? 7 : 0;
        piece rook = GamePanel.getPieceAt(rookCol,row);
        return rook!=null && rook.type==Type.ROOK && rook.color==color && !rook.moved
                && isPathClear(rookCol,row);
    }
}
