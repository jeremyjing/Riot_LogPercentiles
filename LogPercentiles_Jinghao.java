import java.util.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.lang.*;


class LogPercentiles_Jinghao{
	static int partition_size = 10000000; //allocate 10^7 elements for each partition 
								
	public static void main (String [] args){
		System.out.println("What is the time variance of READ API Logs you want to analyze? (Please enter the date and time of logs you want to analyze).");
		System.out.println("If you enter 2019-11-12-00 2019-11-12-00, then you will get the logs from 00:00 - 00:59 of Nov12, 2019.");
		System.out.println("If you enter 2019-11-12-00 2019-11-12-05, then you will get the logs from 00:00 - 05:59 of Nov12, 2019.");
		//determine the time variance of logs (past two hours/a day?)
		Scanner sc = new Scanner(System.in);
		String time_variance_start = sc.next();
		String time_variance_end = sc.next();

		//System.out.println("Input file: "+time_variance_start+" and "+time_variance_end);
		try{
		LogPercentiles_Jinghao lp = new LogPercentiles_Jinghao();
			//get logs from past time_variance hours, 
			//sort by response time, and write out the response time into temp files
			lp.get_logs(time_variance_start, time_variance_end);

			//merge all sorted temp fiels;
			lp.merge_tmp_files();

			//find the no%, nf%, and nn% response time
			lp.find_response_time();
		}catch(IOException e){
			System.out.println("Getting and merging logs failed because of IO error");
		}
	}

	//get logs from target files, sort logs and write into tmp files
	private void get_logs(String time_variance_start, String time_variance_end) throws IOException{		
		String s = ".log";
		String start_file = time_variance_start+s;
	    String end_file = time_variance_end+s;	
		List<String> target_logs = new ArrayList<String>();
		long line_numbers = 0;

		//There are more than one logs file to be analyzed
		if(!time_variance_end.equals(time_variance_start)){ 
			//get time stamp for start file and end file
			try{
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH");
				Date date1 = simpleDateFormat.parse(time_variance_start);
				Date date2 = simpleDateFormat.parse(time_variance_end);
				long ts_s = date1.getTime();
				long ts_e = date2.getTime();
				//System.out.println("ts_s: "+ts_s+" ts_e: "+ts_e);
				//determine target log files based on time stamp of each log file
				//and calculate total line number of logs
				File http_dir = new File("/var/log/httpd"); 
				String[] http_list = http_dir.list();
				for(String file : http_list){
					if(file.length()==17){
						String file_time_s = file.substring(0, file.length()-4);
						Date date = simpleDateFormat.parse(file_time_s);
						long file_time = date.getTime();
						if(file_time>=ts_s && file_time<=ts_e){
							//System.out.println("target file is "+file);
							target_logs.add(file);
							File f = new File("/var/log/httpd/"+file);
							//getting total line number of all files
							LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(f));
							lineNumberReader.skip(Long.MAX_VALUE);
		    	           	int lines = lineNumberReader.getLineNumber()+1;
		    	           	//System.out.println(file+" line: "+lines);
		    	           	line_numbers = line_numbers + lines;
		    	           	//System.out.println("total line: "+line_numbers);
		    	           	lineNumberReader.close();
		    	        }
		    	    }	
				}
			}catch(ParseException ex){
				System.out.println("SimpleDateFormat conversion fail");
			}catch(FileNotFoundException e){
				System.out.println( "File not found");
			}
		}else{ //There are only one log file to be analyzed
			target_logs.add(start_file);
			File f = new File("/var/log/httpd/"+start_file);
			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(f));
    		lineNumberReader.skip(Long.MAX_VALUE);
    	    line_numbers = lineNumberReader.getLineNumber()+1;
    	    //System.out.println("total line: "+line_numbers);
    	    lineNumberReader.close();
		}
		partition_sort(target_logs, line_numbers);
	}

	//store response time from target logs into partitions, sort them and write into output file
	private void partition_sort(List<String> file_names, long log_lines) throws FileNotFoundException{
		int[] rt = new int[partition_size];  //partition that temporally stores response time for each log
		int i=0, a=1;

		File file = new File("/var/log/httpd/tmp/");
		file.mkdirs(); //make a tmp directory to store tmp log files

		for(int k=0; k<file_names.size(); k++){
			String name = file_names.get(k);
			//System.out.println("Scanning file: "+name);
			File f = new File("/var/log/httpd/"+name);
    	    Scanner sc = new Scanner(f);
			while(sc.hasNextLine()){
				//more lines in file to be read, extra space in rt to store
				if(sc.hasNextLine() && i<partition_size){ 
					String tmp = sc.nextLine();
					int p = tmp.length()-1;
					while(p>=0 && tmp.charAt(p)!=' '){
						p--;
					}
					String tmp1 = tmp.substring(p+1, tmp.length());
					int response_time = Integer.parseInt(tmp1);
					//System.out.println("scanning file line: "+tmp+" time: "+tmp1+" int: "+response_time);
					rt[i] = response_time;	
					i++;
				}else if(sc.hasNextLine()){ 
					//no space in partition, write into the output file and start with new partition
					quick_sort(rt, 0, rt.length-1);  //quick sort all time stamps in partitions
					String tmp_file = "/var/log/httpd/tmp/" + a +".log";
					write_to_file(rt, tmp_file);	//write partitions to temp files
					a++;
					i=0;
				}
			}
		}
		//sort partition
		quick_sort(rt, 0, i-1);
		int[] rt_tmp = new int[i];
		for(int m=0; m<i; m++){
			rt_tmp[m]=rt[m];
			//System.out.println("After sort, rt_tmp[i]: "+rt_tmp[m]+" --- "+(m+1));
		}
		String tmp_file = "/var/log/httpd/tmp/" + a +".log";
		write_to_file(rt_tmp, tmp_file);
	}

	//quick sort to sort partitions
	void quick_sort(int[] arr, int start, int end){
    	if(start<end){
    		int pi = partition(arr, start, end);

    		quick_sort(arr, start, pi-1);
    		quick_sort(arr, pi+1, end);
    	}
    }

	int partition(int[] arr, int l, int r){
		int pivot = arr[r];
		int i = l-1; 
		int j = l;

		for(j=l; j<r; j++){
			if(arr[j]>pivot){
				++i;
				int tmp = arr[i];
				arr[i] = arr[j];  
				arr[j] = tmp;
			}
		}
		int tmp = arr[i+1];
		arr[i+1] = arr[r];
		arr[r] = tmp;

		return i+1;
	}

	//write sorted time stamp into new file
	void write_to_file(int[] A, String file_name){
		BufferedWriter bufw = null;
        try {
        	File file = new File (file_name);
        	file.createNewFile();
            FileWriter fw = new FileWriter(file);
            bufw = new BufferedWriter(fw);
            for (int i = 0; i < A.length; i++) {
            	//System.out.println("Writing A[i]: "+A[i]+" --- "+i);
                bufw.write(String.valueOf(A[i]));
                bufw.newLine();
                bufw.flush();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (bufw != null) {
                try {
                    bufw.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
	}

	//merge all tmp log files into a big file
	void merge_tmp_files() throws IOException{
		File tmp_dir = new File("/var/log/httpd/tmp"); 
		String[] tmp_list = tmp_dir.list();
		HashMap<String, Integer> store = new HashMap<String, Integer>();
		HashMap<String, Integer> line_count = new HashMap<String, Integer>();
		
		for(int i=0; i<tmp_list.length; i++){
			File f1 = new File("/var/log/httpd/tmp/"+tmp_list[i]);
			if(!f1.isDirectory()){
	    	    store.put(tmp_list[i], 0);
	    	    line_count.put(tmp_list[i], 1);
	    	    //System.out.println("file: "+tmp_list[i]);
    		}
		}

		compare_and_sort(store, line_count, tmp_list);
	}

	//before merging into the big file, compare each element in each tmp file
	void compare_and_sort(HashMap<String, Integer> map, HashMap<String, Integer> line_count, String[] file_list){
		try{
			File f = new File ("/var/log/httpd/tmp/merged.log");
	        f.createNewFile();
	        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
	        HashMap<String, Integer> sorted_map = new HashMap<String, Integer>();
			while (iterator.hasNext()) { 
				Map.Entry<String, Integer> entry = iterator.next();
	            String key = entry.getKey(); 
	            File file = new File("/var/log/httpd/tmp/"+key);
	            Scanner sc = new Scanner(file);
	            if(sc.hasNext()){
	            	int a = sc.nextInt();
	            	Integer value = new Integer(a);
	            	map.put(key, value);
	            } 
	        } 

	        sorted_map = sort_map_by_value(map);
	        while(sorted_map.size()!=0){
				sorted_map = sort_map_by_value(sorted_map);
				HashMap.Entry<String, Integer> first = sorted_map.entrySet().iterator().next();
				Iterator<Map.Entry<String, Integer>> iterator1 = sorted_map.entrySet().iterator();
				while (iterator1.hasNext()){
					Map.Entry<String, Integer> entry = iterator1.next();
					String key = entry.getKey(); 
					//System.out.println("key: "+key+" value: "+entry.getValue());
				}

				Integer res_tmp = first.getValue();
				int res = res_tmp.intValue();
				//System.out.println("merging res to: "+res);
				merge_to_file(res, f);

				String file_name = first.getKey();
				//System.out.println("file name: "+file_name);
				File fs = new File("/var/log/httpd/tmp/"+file_name);
				Scanner sc = new Scanner(fs);
				Integer line_num = line_count.get(file_name);
				Integer tmp_line_num = line_num;
				//System.out.println("line number: "+line_num);
				String line="";
				while(tmp_line_num>=0){
					if(sc.hasNextLine()){
						line = sc.nextLine();
						tmp_line_num--;
					}else{
						line = null;
						break;
					}
				}
				//System.out.println("line is: "+line);
				if(line!=null){
					Integer i = Integer.valueOf(line);
					line_num = line_num + 1;
					//System.out.println("Integer i is: "+i+" line_num is: "+line_num);
					sorted_map.put(file_name, i);
					line_count.put(file_name, line_num);
				}else{
					sorted_map.remove(file_name);
					line_count.remove(file_name);
				}
			}
		}catch(IOException e){
			System.out.println("IOException error");
		}	
	}

	//helper function sort HashMap
	HashMap<String, Integer> sort_map_by_value(HashMap<String, Integer> hm){
		List<Map.Entry<String, Integer> > list = 
               new LinkedList<Map.Entry<String, Integer> >(hm.entrySet()); 
  
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() { 
            public int compare(Map.Entry<String, Integer> o1,  
                               Map.Entry<String, Integer> o2) 
            { 
                return (o2.getValue()).compareTo(o1.getValue()); 
            } 
        }); 
          
        // put data from sorted list to hashmap  
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>(); 
        for (Map.Entry<String, Integer> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        }
        return temp;
	}

	//write merged response time into one file
	void merge_to_file(int a, File file) throws FileNotFoundException{
		BufferedWriter bufw = null;
		Scanner sc = new Scanner(file);
        try {
            FileWriter fw = new FileWriter(file, true);
            bufw = new BufferedWriter(fw);
            bufw.write(String.valueOf(a));
            bufw.newLine();
            bufw.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (bufw != null) {
                try {
                    bufw.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
	}

	//calculate and print out the result
	void find_response_time(){
		try{
			File merged_log = new File("/var/log/httpd/tmp/merged.log"); 
			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(merged_log));
			lineNumberReader.skip(Long.MAX_VALUE);
			int lines = lineNumberReader.getLineNumber()+1;
			lineNumberReader.close();

			DecimalFormat df = new DecimalFormat("0");

			double nop_time, nfp_time, nnp_time;
			nop_time = 0.9 * lines;
			nfp_time = 0.95 * lines;
			nnp_time = 0.99 * lines;


			String nop_finals, nfp_finals, nnp_finals;
			nop_finals = df.format(nop_time);
			nfp_finals = df.format(nfp_time);
			nnp_finals = df.format(nnp_time);

			int nop_final, nfp_final, nnp_final;
			nop_final = lines - Integer.parseInt(nop_finals);
			nfp_final = lines - Integer.parseInt(nfp_finals);
			nnp_final = lines - Integer.parseInt(nnp_finals);

			File f = new File ("/var/log/httpd/tmp/merged.log");
			Scanner sc = new Scanner(f);
			String res = "";
			String nnp_res = ""; 
			String nfp_res = "";
			String nop_res = "";
			while(nop_final>=0){
				res = sc.nextLine();
				nop_final--;
				nfp_final--;
				nnp_final--;
				if(nnp_final==0){
					nnp_res = res;
				}
				if(nfp_final==0){
					nfp_res = res;
				}
				if(nop_final==0){
					nop_res = res;
				}
			}

			File file = new File ("/var/log/httpd/tmp/merged.log");
			Scanner scan = new Scanner(file);
			if(nnp_res.equals("")){
				nnp_res = scan.nextLine();
			}
			if(nfp_res.equals("")){
				nfp_res = scan.nextLine();
			}
			if(nop_res.equals("")){
				nop_res = scan.nextLine();
			}

			System.out.println("90% of requests return a response within "+ nop_res +" ms");
			System.out.println("95% of requests return a response within "+ nfp_res +" ms");
			System.out.println("99% of requests return a response within "+ nnp_res +" ms");

		}catch(FileNotFoundException e){
			System.out.println("merged.log was not found");
		}catch(IOException e){
			System.out.println("IOE Error");
		}
	}
}







