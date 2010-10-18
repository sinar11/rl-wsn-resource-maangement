package drcl.inet.sensorsim.drl.demo;

public class DRLDemoFactory {
	static IDRLDemo demo=null;
	
	public static IDRLDemo getDRLDemo(){
		if(demo==null){
			String demoProp= System.getProperty("drl.demo");
			if(demoProp==null)
				demo= new MockDemo();
			else
				demo= new DRLDemo();
		}
		return demo;
	}
}
