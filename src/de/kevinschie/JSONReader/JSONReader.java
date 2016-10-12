package de.kevinschie.JSONReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import org.json.*;

public class JSONReader {	
	private Scanner 				m_scn;
	private ArrayList<JSONObject> 	m_jsonObjects;
	
	public JSONReader(){}
	
	public synchronized ArrayList<JSONObject> ReadJSON(File MyFile,String Encoding) throws FileNotFoundException
	{
		try {
			m_scn = new Scanner(MyFile,Encoding);
			
			m_jsonObjects = new ArrayList<JSONObject>();
		    //Reading and Parsing Strings to Json
		    while(m_scn.hasNext()){
		        JSONObject obj;
				obj = (JSONObject) new JSONTokener(m_scn.nextLine()).nextValue();
				m_jsonObjects.add(obj);
		    }
		    //Here Printing Json Objects
		    for(JSONObject obj : m_jsonObjects){
		        System.out.println(obj.get("name")+" : "+obj.get("value")+" : "+obj.get("timestamp"));
		    }
		    return m_jsonObjects;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			m_scn.close();
		}
	    return null;
	}
}