package parser;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
*/

public class ParseEntry {

	public int number;
	public double posX,posY,posZ;
	public String Comment;

    public ParseEntry(int number, double posx, double posy, double posz, String comment)
    {
		
		this.number = number;
		posX = posx;
		posY = posy;
		posZ = posz;
		Comment = comment;
	}
}
