package de.kevinschie.JSONReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import org.json.*;

public class JSONReader {	
	private Scanner 				m_scn;
	private ArrayList<JSONObject> 	m_jsonObjects;
	private String 					encoding ;
	private InputStream 			in;
	
	public JSONReader()
	{
		try
		{
			encoding = "UTF-8";
			in = getClass().getResourceAsStream("downtown-crosstown.json");
		}
		catch(NullPointerException npe)
		{
			npe.printStackTrace();
		}
	}
	
	public synchronized ArrayList<JSONObject> ReadJSON(String jsonDirectory)
	{
		try
		{
			if(!jsonDirectory.equals("downtown-crosstown.json"))
			{
				try
				{
					File file = new File(jsonDirectory);
					in = new FileInputStream(file);
				}
				catch(Exception ex)
				{
					System.out.println("Exception while reading custom file. Using default file");
					//ex.printStackTrace();
					in = getClass().getResourceAsStream("downtown-crosstown.json");
				}
			}
			m_scn = new Scanner(in, encoding);
			
			m_jsonObjects = new ArrayList<JSONObject>();
		    //Reading and Parsing Strings to Json
		    while(m_scn.hasNext())
		    {
		        JSONObject obj;
				obj = (JSONObject) new JSONTokener(m_scn.nextLine()).nextValue();
				m_jsonObjects.add(obj);
		    }
		    //Here Printing Json Objects
		    for(JSONObject obj : m_jsonObjects)
		    {
		        System.out.println(obj.get("name")+" : "+obj.get("value")+" : "+obj.get("timestamp"));
		    }
		    return m_jsonObjects;
		}
		catch(IllegalArgumentException iae)
		{
			System.out.println("Wrong parameters for Scanner");
			iae.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			m_scn.close();
			try
			{
				in.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				System.out.println("Exception while closing the InputStream.");
			}
		}
	    return null;
	}
}