import java.util.Random;
import java.util.*;
import java.io.*;
import java.lang.*;

class make_logs{
	public static void main (String args[]) throws FileNotFoundException{
		int file_num = 48;

		Random r1 = new Random();
		Random r2 = new Random();

		String[] file_name = new String[]{"2019-11-12-00", "2019-11-12-01", "2019-11-12-02", "2019-11-12-03",
										"2019-11-12-04", "2019-11-12-05", "2019-11-12-06", "2019-11-12-07", 
										"2019-11-12-08", "2019-11-12-09", "2019-11-12-10", "2019-11-12-11", 
										"2019-11-12-12", "2019-11-12-13", "2019-11-12-14", "2019-11-12-15", 
										"2019-11-12-16", "2019-11-12-17", "2019-11-12-18", "2019-11-12-19", 
										"2019-11-12-20", "2019-11-12-21", "2019-11-12-22", "2019-11-12-23", 
										"2019-11-12-24", "2019-11-12-25", "2019-11-12-26", "2019-11-12-27", 
										"2019-11-12-28", "2019-11-12-29", "2019-11-12-30", "2019-11-12-31", 
										"2019-11-12-32", "2019-11-12-33", "2019-11-12-34", "2019-11-12-35", 
										"2019-11-12-36", "2019-11-12-37", "2019-11-12-38", "2019-11-12-39", 
										"2019-11-12-40", "2019-11-12-41", "2019-11-12-42", "2019-11-12-43", 
										"2019-11-12-44", "2019-11-12-45", "2019-11-12-46", "2019-11-12-47"};
		String file_extension = ".log";

		System.out.println("Writing log");
		try{
			for(int i=0; i<file_num; i++){
				File file = new File("/Users/jeremy/Desktop/test/httpd/"+file_name[i]+file_extension);
				file.createNewFile();
				Scanner sc = new Scanner(file);
		        FileWriter fw = new FileWriter(file, true);
		        BufferedWriter bufw = null;
		        bufw = new BufferedWriter(fw);
		        int line_num = r1.nextInt(10)*1000000+1;
		        for(int j=0; j<line_num; j++){
		         	bufw.write("10.2.3.4 [2018/13/10:14:02:39] GET /api/playeritems?playerId=3 200 "+String.valueOf(r2.nextInt(10000)));
		           	bufw.newLine();
		           	bufw.flush();
		        }
		        bufw.close();
		        System.out.println(file_name[i]+file_extension+" done");
			}
		}catch (IOException e) {
	        System.out.println(e.getMessage());
	    } 	
	}
}







