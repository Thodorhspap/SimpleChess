package main;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter {
    private final GamePanel gp;

    public Mouse(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        gp.mousePressed(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        gp.mouseDragged(e.getX(), e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        gp.mouseMoved(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        gp.mouseReleased(e.getX(), e.getY());
    }
}
