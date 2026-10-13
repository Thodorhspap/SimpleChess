package main;

import javax.swing.JFrame;

public class main {
    public static void main (String[] args){

        //φτιαχνω το frame
        JFrame window = new JFrame("SimpleChess");
        window.setResizable(false);         // να μην αλλαζει μεγεθος


        //βαζω το πανελ που εφτιαξα μεσα στο jframe
        GamePanel gp =new GamePanel();
        window.add(gp);
        window.pack();


        window.setLocationRelativeTo(null); //να εμφανιζεται στο κεντρο
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //να κλεινει το προγραμμα με το X
        window.setVisible(true);            //να εμφανιζεται
    }
}
