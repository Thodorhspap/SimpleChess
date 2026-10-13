package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Board {
    final int Max_Col = 8;
    final int Max_Row = 8;
    public static final int SquareSize=90;
    public static final int Half_Square_Size=SquareSize/2;

    // δύο υφές ξύλου σε μέγεθος όλου του ταμπλό: κάθε τετράγωνο παίρνει το δικό του κομμάτι,
    // οπότε κανένα τετράγωνο δεν μοιάζει ακριβώς με το άλλο
    private final BufferedImage lightWood;
    private final BufferedImage darkWood;

    public Board(){
        Random random = new Random(42);
        int size = SquareSize*Max_Col;
        lightWood = makeWood(size,new Color(236,203,154),new Color(196,146,88),random);
        darkWood = makeWood(size,new Color(138,86,48),new Color(84,48,24),random);
    }

    public void draw (Graphics2D g2){
        for (int row =0; row<Max_Row;row++){
            for (int col=0 ; col<Max_Col;col++){
                int x = col*SquareSize;
                int y = row*SquareSize;
                BufferedImage wood = (row+col)%2==0 ? lightWood : darkWood;
                g2.drawImage(wood,x,y,x+SquareSize,y+SquareSize,x,y,x+SquareSize,y+SquareSize,null);

                // λεπτή σκιά στις άκρες, σαν να είναι ένθετα κομμάτια ξύλου
                g2.setColor(new Color(255,255,255,28));
                g2.drawLine(x,y,x+SquareSize-1,y);
                g2.drawLine(x,y,x,y+SquareSize-1);
                g2.setColor(new Color(0,0,0,40));
                g2.drawLine(x,y+SquareSize-1,x+SquareSize-1,y+SquareSize-1);
                g2.drawLine(x+SquareSize-1,y,x+SquareSize-1,y+SquareSize-1);
            }
        }
    }

    // φτιάχνει υφή ξύλου: κυματιστά νερά κατά μήκος + λίγο "θόρυβο" για τις ίνες
    private BufferedImage makeWood(int size,Color light,Color dark,Random random){
        BufferedImage img = new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
        Noise warp = new Noise(random,size/80+2,size/14+2);
        Noise streaks = new Noise(random,size/30+2,size/4+2);
        double phase = random.nextDouble()*10;

        for (int y=0; y<size; y++){
            for (int x=0; x<size; x++){
                // τα νερά πάνε οριζόντια και τα στραβώνει λίγο ο θόρυβος
                double w = warp.at(x/80.0,y/14.0);
                double grain = Math.sin(y/5.5+w*7.0+phase)*0.5+0.5;
                grain = Math.pow(grain,3);
                double streak = streaks.at(x/30.0,y/4.0);
                double fiber = random.nextDouble()*0.06;
                double t = grain*0.45+streak*0.35+fiber+w*0.15-0.05;
                t = Math.max(0,Math.min(1,t));
                img.setRGB(x,y,mix(light,dark,t));
            }
        }
        return img;
    }

    private static int mix(Color a,Color b,double t){
        int r = (int) (a.getRed()+(b.getRed()-a.getRed())*t);
        int g = (int) (a.getGreen()+(b.getGreen()-a.getGreen())*t);
        int bl = (int) (a.getBlue()+(b.getBlue()-a.getBlue())*t);
        return (r<<16)|(g<<8)|bl;
    }

    // απλός θόρυβος: τυχαίες τιμές σε πλέγμα, με ομαλή μετάβαση ανάμεσά τους
    private static class Noise {
        final double[][] grid;

        Noise(Random random,int w,int h){
            grid = new double[h+2][w+2];
            for (double[] row : grid){
                for (int i=0; i<row.length; i++){
                    row[i] = random.nextDouble();
                }
            }
        }

        double at(double x,double y){
            int x0 = (int) x;
            int y0 = (int) y;
            double fx = smooth(x-x0);
            double fy = smooth(y-y0);
            double top = grid[y0][x0]+(grid[y0][x0+1]-grid[y0][x0])*fx;
            double bottom = grid[y0+1][x0]+(grid[y0+1][x0+1]-grid[y0+1][x0])*fx;
            return top+(bottom-top)*fy;
        }

        private static double smooth(double t){
            return t*t*(3-2*t);
        }
    }
}
