package org.zkoss.zss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Properties;

import org.zkoss.lang.Library;

public class Setup {
	
	static {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("zss.test.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(Iterator<?> it = props.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			Library.setProperty(key, props.getProperty(key));
		}
	}
	
	public static void touch() {};
	
	static private File temp; 
	
	public static synchronized File getTempFile(){
		if(temp==null){
			File tempFolder = new File(System.getProperty("java.io.tmpdir"));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			temp = new File(tempFolder,"zsstest-"+sdf.format(new java.util.Date()));
			
		}
		return temp;
		
	}
}