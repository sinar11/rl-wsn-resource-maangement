package visualSensorNet;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;


 /**
 * @author Soul_of_bat@hotmail.com
 * @version 1.0, 07/24/2005
 *
*/


public class JTextPanel extends Canvas {

	private String[][] texte;
	public int taille_x,taille_y;
	private static final int largeur_ligne=15;
	public static final int largeur_colonne = 600;
	public static final int largeur_fenetre = 800;
	
	private int nblignes;
	private int nbmaxlignes;
	public int frgColor;//Couleur premier plan
	public int bkgColor;//Couleur arriere-plan
	String separator="--------------------------------------------------------------------------------------------------------------" ;
	
	public JTextPanel(String texte_init_col1,String texte_init_col2,int n)
	{
		nbmaxlignes=n;
		this.taille_y=n*largeur_ligne+12;
		texte=new String[2][nbmaxlignes];
		clearText();
		texte[0][0]=texte_init_col1;
		nblignes=1;
		this.taille_x=largeur_fenetre;
	}
	
	public void clearText()
	{
		for(int i=0;i<nbmaxlignes;i++)
		{
			texte[0][i]="";
			texte[1][i]="";
			nblignes=0;
		}
	}
	
	public void rmLigne(int numligne)
	{
		if(numligne<nbmaxlignes&&numligne>=0&&numligne<nblignes)
		{
			int i;
			for(i=numligne-1;i<nblignes;i++)
			{
				texte[i]=texte[i+1];
			}
			texte[0][i]="";
			texte[1][i]="";
		}
	}
	
	public Dimension getPreferredSize() {
        return new Dimension(taille_x,taille_y);
    }
	
	public void addText(String txt_col1, String txt_col2)
	{
		if(nblignes>=nbmaxlignes) return;
		texte[0][nblignes]=txt_col1;
		texte[1][nblignes]=txt_col2;
		nblignes++;
	}
	
	
	public void addSeparator()
	{
		//adds a separator...
		texte[0][nblignes]=separator;
		nblignes++;
	}
	
	
	
	public void paint(Graphics g)
	{
		setForeground(Color.BLUE);

		for(int i=0;i<nblignes;i++)
		{
			g.drawString(texte[0][i],5,12+i*largeur_ligne);
			g.drawString(texte[1][i],5+largeur_colonne,12+i*largeur_ligne);
		}  
	}
}
