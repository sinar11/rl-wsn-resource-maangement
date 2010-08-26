package visualSensorNet;

import java.awt.*;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
 * Nicholas: J'ai changer le parametre 'taille' car je voulait avoir
 * des dimension different pour x et y... et pour avoir une image plus
 * "grande" dans drawTab() je multiplie la location des sensors par un
 * chiffre pour que sa ralonge.
 *
*/

public class Plan extends Canvas
{
	public static final int Click_zone_size_x = 25;
    public static final int Click_zone_sizeX1 = 8;
	public static final int Click_zone_size_y = 15;
    public static final int Click_zone_sizeY1 = 8;

	double[][] sensor,sink,targs ;
	int nbsens,nbsink,nbtargs;
	//int taille;
    int width;
    int height;


    /**
     *
     * @param width_
     * @param height_
     * @param max_sens
     * @param max_sinks
     * @param max_targs
    */
	public Plan(int width_, int height_, int max_sens, int max_sinks, int max_targs)
    {
		//taille = taille_;
		width = width_;
        height = height_;
        nbsens = 1; //nicholas: modified this to a 1 so it its the same as the backend numbering
		nbsink = 0;
		nbtargs = 0;
		
		sensor = new  double[max_sens][2];
		sink = new  double[max_sinks][2];
		targs = new  double[max_targs][2];
	}
	
	/**
     *
     * @param x
     * @param y
    */
	public void addSensor(double x, double y)
    {
        if(nbsens>=sensor.length )
            return;
	
		sensor[nbsens][0] = x ;
		sensor[nbsens][1] = y ;
		nbsens++;
	}

    /**
     *
     * @param x
     * @param y
    */
	public void addSink(double x,double y)
    {
		if(nbsink>=sink.length ) return;
		sink[nbsink][0] = x ;
		sink[nbsink][1] = y ;
		nbsink++;
	}

    /**
     *
     * @param x
     * @param y
    */
	public void addTarg(double x,double y)
    {
		if(nbtargs>=targs.length ) return;
		targs[nbtargs][0] = x ;
		targs[nbtargs][1] = y ;
		nbtargs++;
	}

    /**
     *
     * @param i
     * @param x
     * @param y
    */
	public void mvTarg(int i, double x, double y)
    {
		// deplace la cible i
		targs[i][0] = x;
		targs[i][1] = x;
	}


    /**
     * dessine les nb premiers éléments de tab dans g
     * @param tab
     * @param nb
     * @param g
     * @param coul
    */
	protected void drawTab(double[][] tab,int nb, Graphics g, Color coul)
    {
        //Nicholas:
        //Note that in order to make the image look bigger all xCoordinates were multiplied
        //by 6 and all yCoordinates were multiplied by 7. Therefore if x = 1m then that is
        //equivalent to 6 over on the screen (i.e. 6 pixels instead of 1).
		for(int i = 0; i < nb; i++){
			g.setColor(coul);
		   	g.drawArc(((int)tab[i][0])*6,((int)tab[i][1])*7,10,10,0,360);
		   	g.setFont(new Font("Arial",0,9));
		  	//g.drawString(""+tab[i][0],(int)tab[i][0]+10,(int)tab[i][1]+5); //xCoord Label
		    //g.drawString(""+tab[i][1],(int)tab[i][0]+10,(int)tab[i][1]+15); //yCoord Label
		    g.drawString(i+"",((int)tab[i][0])*6+2,((int)tab[i][1])*7+9);
		}
	}

    /**
     *
     * @param pas
     * @param g
    */
	protected void drawGrid(int pas, Graphics g)
    {
		/*for(int i=0; i < taille; i = i + pas){

			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(0,i,taille,i);
			g.drawLine(i,0,i,taille);
		}*/
        for(int i = 0; i < height; i = i + pas){

			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(0, i, width, i);
			//g.drawLine(i,0,i,taille);
		}
        for(int i=0; i < width; i = i + pas){

			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(i,0,i,height);
            //g.drawLine(i,0,i,taille);
		}
	}
	
    /**
     *
     * @return
    */
	public Dimension getPreferredSize()
    {
        return new Dimension(width, height);
    }

    /**
     *  returns the number of the pointed node , used by Placer to know where mouse was clicked
     *  returns -1 if no node matches
     * @param x
     * @param y
     * @return
    */
	public int whichPointedNode(int x, int y)
    {
		int res = -1;

		for(int i = 0; i < nbsens;i++)
        {
            //sensor [sensor#] [0:xCoord, 1:yCoord]
            //System.out.println("----------------------\n Sensor"+i+ " has xCoord: " + sensor[i][0]*6 + " and yCoord: " +sensor[i][1]*9);

			boolean matchx = ( (x >= (sensor[i][0]*6)-Click_zone_sizeX1) & (x <= (sensor[i][0]*6 + Click_zone_size_x)));
			boolean matchy = ( (y >= (sensor[i][1]*7)-Click_zone_sizeY1) & (y <= (sensor[i][1]*7 + Click_zone_size_y)));
			if(matchx & matchy) res = i;
		}
		
		return res;
		
	}
	
	
	public void paint(Graphics g) {
    	
        setBackground(Color.WHITE);
    	//g.clearRect(0,0,taille,taille);
        g.clearRect(0, 0, width, height);
    	drawGrid(50,g);
    	
    	drawTab(sensor,nbsens,g,Color.BLUE);
    	drawTab(sink,nbsink,g,Color.GREEN);
    	drawTab(targs,nbtargs,g,Color.RED );
    
	}
}
