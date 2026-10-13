package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class piece {
    public BufferedImage image;
    public int x,y;
    public int col,row,preCol,preRow;
    public int color;
    public Type type;
    public boolean moved;   // αν έχει κουνηθεί ποτέ (χρειάζεται για το ροκέ)

  public piece(int color,int col,int row ){
        this.color=color;
        this.col=col;
        this.row=row;
        x= getX(col);
        y= getY(row);
        preCol=col;
        preRow=row;
    }

    public BufferedImage getImage(String ImagePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getResourceAsStream(ImagePath + ".png"));
        }catch(IOException e){
            e.printStackTrace();
        }
        return image;

    }

    public int getX(int col){
        return col* Board.SquareSize;

    }
    public int getY(int row){

      return row*Board.SquareSize;
    }

    // κλειδώνει τη νέα θέση μετά από κίνηση
    public void updatePosition(){
        x=getX(col);
        y=getY(row);
        preCol=col;
        preRow=row;
        moved=true;
    }

    // γυρνάει το κομμάτι πίσω αν η κίνηση δεν ήταν έγκυρη
    public void resetPosition(){
        col=preCol;
        row=preRow;
        x=getX(col);
        y=getY(row);
    }

    public boolean isWithinBoard(int targetCol,int targetRow){
        return targetCol>=0 && targetCol<=7 && targetRow>=0 && targetRow<=7;
    }

    // τα τετράγωνα που απειλεί το κομμάτι (για το σαχ) - το κάνει override κάθε κομμάτι
    public boolean attacks(int targetCol,int targetRow){
        return false;
    }

    // κίνηση με βάση τους κανόνες του κομματιού, χωρίς να ελέγχει αν αφήνει τον βασιλιά σε σαχ
    public boolean canMove(int targetCol,int targetRow){
        if (!isWithinBoard(targetCol,targetRow) || (targetCol==col && targetRow==row)){
            return false;
        }
        piece target = GamePanel.getPieceAt(targetCol,targetRow);
        if (target!=null && target.color==color){
            return false;
        }
        return attacks(targetCol,targetRow);
    }

    public boolean isStraightLine(int targetCol,int targetRow){
        return (targetCol==col) != (targetRow==row);
    }

    public boolean isDiagonalLine(int targetCol,int targetRow){
        return targetCol!=col && Math.abs(targetCol-col)==Math.abs(targetRow-row);
    }

    // ελέγχει ότι δεν υπάρχει κανένα κομμάτι ανάμεσα σε εμάς και στον στόχο
    public boolean isPathClear(int targetCol,int targetRow){
        int dc = Integer.signum(targetCol-col);
        int dr = Integer.signum(targetRow-row);
        int c = col+dc;
        int r = row+dr;
        while (c!=targetCol || r!=targetRow){
            if (GamePanel.getPieceAt(c,r)!=null){
                return false;
            }
            c+=dc;
            r+=dr;
        }
        return true;
    }

    public void draw(Graphics2D g2){
        draw(g2,x,y,Board.SquareSize);
    }

    // ζωγραφίζει την εικόνα χωρίς να την παραμορφώνει, στο κέντρο του τετραγώνου
    public void draw(Graphics2D g2,int px,int py,int size){
        if (image==null){
            return;
        }
        double scale = Math.min((double) size/image.getWidth(),(double) size/image.getHeight());
        int w = (int) (image.getWidth()*scale);
        int h = (int) (image.getHeight()*scale);
        g2.drawImage(image,px+(size-w)/2,py+(size-h)/2,w,h,null);
    }
}
