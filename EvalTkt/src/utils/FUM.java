package utils;

public class FUM {
	public static String DoubleToStr(Integer num, Integer denum)
	{		
		if(denum==null || denum==0) return "N/A";
		if(num==null) num=0;
		String ds = String.format("%.02f", ((double)num*100)/denum);
		//System.out.printf("\t[Test] '%s'\n", ds);
		while(ds.endsWith(".00")) {
			ds =  ds.substring(0, ds.length()-3);			
		}
		if(ds.contains(".")) 
			while(ds.endsWith("0") || ds.endsWith(".")) {
				ds = ds.substring(0, ds.length()-1);
				//System.out.printf("'%s'\n", ds);
			}
		return String.format("%s%%", ds.length()>0?ds:"0");
	}
	
	public static String DoubleToStr(double d)
	{
		String ds = String.format("%.04f", d);
		while(ds.endsWith("0") || ds.endsWith(".")) ds = ds.substring(0, ds.length()-1);
		return ds.length()>0?ds:"0";
	}
}
