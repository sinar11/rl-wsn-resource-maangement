package visualSensorNet;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
*/

public class Mouseclick extends MouseAdapter
 {
	Placer parent;

    public Mouseclick(Placer parent_) {
	        super();
	        parent = parent_;
	}


    public void mouseClicked(MouseEvent e)
	{
	    //System.out.println("MouseListener:'click' sur ["+e.getX() +","+e.getY() +"]");
	    parent.reactMouseClick( e.getX(), e.getY());
    }
}
