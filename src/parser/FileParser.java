package parser;
import java.io.BufferedReader;
import java.io.*;


/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
*/
public class FileParser {
	
	 BufferedReader in = null;
	 public boolean parse_succes = false;
	 int nblines = 0;
	 ParseEntry[] sensors;
	 public int nbsens = 0;
	 public double map_xmin,map_xmax,map_ymin,map_ymax,map_dx,map_dy;
	 
	 
	public FileParser(String file_to_parse)
    {

        // parses the current file
		try{
			System.out.println("Parsing file: "+ file_to_parse);
			in = new BufferedReader(new FileReader(file_to_parse));
			
			while(in.ready()){
				String line = in.readLine() ;
				if(line.length()>0)
				if(line.charAt(0) != '#'){
					// this is not a comment line. We count it;
					nblines++;
				}
			}
			
			// we now know the number of sensors we'll have to add...
			// parsing a second time to get data
			sensors = new ParseEntry[nblines];
			System.out.println("Effective Line count : "+ nblines);
			in = new BufferedReader(new FileReader(file_to_parse)); // we re-open the buffer
			
			int ln = 0;
			
			while(in.ready()){
				ln++;		//used to print the bogus line in case of parse error
				String line = in.readLine() ;
				if(line.length()>0)
				if(line.charAt(0) != '#') {

                    // this is not a comment line. We parse it
					if(!parseLine(line)) {
                        System.out.println("Parse error on line "+ln+":"+line);
                        return;
                    }
				}
			}	
			
		} catch(Exception e){
			 System.out.println("Error : file "+file_to_parse+" not found");
			 System.out.println(e.getLocalizedMessage() );
			 return ;
		}
		parse_succes = true;
	}
	
	protected boolean parseLine(String line){
		//parses the line line
		//We first erase the comments at the end of the line
		String regline = line.split("#")[0];
		String[] result = regline.split("[ |\t]+"); // use space and tabs to cut tokens

        //NICHOLAS: DO NOT HAVE THIS METHOD?
        //if(regline.contains("Topo")) return topodef(line);
        if(regline.startsWith("Topo")) return topodef(line);

        //System.out.println(result.length+"  0:"+result[0]+"  1:"+result[1]+"  2:"+result[2] );
        //System.out.println(result.length+"  0:"+result[0].trim() +"  1:"+result[1].trim() +"  2:"+result[2].trim()  );
		if(result.length <4 ) {
            return false; //this line's got arguments missing
        }
		else{
			try{
				//casting Strings to what we want..
				int num = Integer.parseInt( result[0]);			
				double posX = Double.parseDouble( result[1]);			
				double posY = Double.parseDouble( result[2]);	
				double posZ = Double.parseDouble( result[3]);
				
				sensors[nbsens] = new ParseEntry(num,posX,posY,posZ,(result.length>=4)?"":result[4]);
				nbsens++;
				//System.out.println("Parser : adding sensor number "+num+":( "+posX+" , "+posY+" , "+posZ+" )");
				return true;
			}catch(Exception e){
				System.out.println("Number format exception : Please enter numerical values only. Message:");
				System.out.println(e.toString() );
				return false;
			}
		}
			
	}

	public boolean topodef(String line)
    {
		// line used for topology definitions
		String regline = line.split("#")[0];
		String[] result = regline.split("[ |\t]+");
		
		
		if(result.length <7 ) return false;
		else{
			try{
				//casting Strings to what we want..
				
				map_xmin = Double.parseDouble( result[1]);			
				map_xmax = Double.parseDouble( result[2]);	
				map_ymin = Double.parseDouble( result[3]);
				map_ymax = Double.parseDouble( result[4]);			
				map_dx = Double.parseDouble( result[5]);	
				map_dy = Double.parseDouble( result[6]);
				
				System.out.println("Parser : Global topology Set at :( "+map_xmin+"-"+map_xmax+" , "+map_ymin+"-"+map_ymax+
						" ,  dX="+map_dx+" dY = "+map_dy+" )");
				return true;
			}catch(Exception e){
				System.out.println("Topology format exception : Please enter numerical values only. Message:");
				System.out.println(e.toString() );
				return false;
				}
			
			}
	}


	public ParseEntry getSensor(int i)
    {
		//returns data corresponding to the entry i
		if(i<nbsens & parse_succes) return sensors[i];
		else return null;
	}


	public int getsensorNumber()
    {
		return nbsens;
	}

		 
	}

