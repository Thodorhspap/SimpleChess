package main;

public class Pawn extends piece {
    public Pawn(int color,int col,int row){
        super (color,col,row);
        type = Type.PAWN;
        if (color == GamePanel.WHITE){
            image = getImage("/piece/w-pawn");
        }else{
            image = getImage("/piece/b-pawn");
        }}

    // τα λευκά πάνε προς τα πάνω, τα μαύρα προς τα κάτω
    private int direction(){
        return color==GamePanel.WHITE ? -1 : 1;
    }

    @Override
    public boolean attacks(int targetCol,int targetRow){
        return targetRow==row+direction() && Math.abs(targetCol-col)==1;
    }

    @Override
    public boolean canMove(int targetCol,int targetRow){
        if (!isWithinBoard(targetCol,targetRow)){
            return false;
        }
        piece target = GamePanel.getPieceAt(targetCol,targetRow);

        // ευθεία: μόνο σε άδειο τετράγωνο
        if (targetCol==col){
            if (target!=null){
                return false;
            }
            if (targetRow==row+direction()){
                return true;
            }
            int startRow = color==GamePanel.WHITE ? 6 : 1;
            return row==startRow && targetRow==row+2*direction()
                    && GamePanel.getPieceAt(col,row+direction())==null;
        }

        // διαγώνια: μόνο για να φάει
        if (attacks(targetCol,targetRow)){
            if (target!=null){
                return target.color!=color;
            }
            // en passant
            piece ep = GamePanel.enPassantPawn;
            return ep!=null && ep.color!=color && ep.col==targetCol && ep.row==row;
        }
        return false;
    }
}
