package de.kevinschie.SimulatorListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

public class SimulationListener {

	ObjectInputStream ois;
	Socket socket;
	
	public void listenToSimulator()
	{
		try{
			new Thread("Device Listener") {
	            public void run() {
					try (
						Socket echoSocket = new Socket("0.0.0.0", 50001);
						BufferedReader in =
						        new BufferedReader(
						            new InputStreamReader(echoSocket.getInputStream()));
					)
					{
						System.out.println("TEST");
						String line;
						while ((line = in.readLine()) != null)
						{
						    System.out.println("echo: " + line);
						}
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
	            };
			}.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
		
		
		/*try{
			System.out.println("Listener läuft.........");
			URL oracle = new URL("http://0.0.0.0:50001/");
	        URLConnection yc = oracle.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(
	                                yc.getInputStream()));
	        String inputLine;
	        while ((inputLine = in.readLine()) != null) 
	            System.out.println(inputLine);
	        in.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}*/
		
		/*try {
            final ServerSocket serverSocket = new ServerSocket(50001);
            new Thread("Device Listener") {
                public void run() {
                	try ( 
                		    ServerSocket serverSocket = new ServerSocket(50001);
                		    Socket clientSocket = serverSocket.accept();
                		    PrintWriter out =
                		        new PrintWriter(clientSocket.getOutputStream(), true);
                		    BufferedReader in = new BufferedReader(
                		        new InputStreamReader(clientSocket.getInputStream()));
                		) {
                			System.out.println(in.readLine());
	                    } catch (Exception e) {
	                        e.printStackTrace();
	                    }
                };
            }.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/
	}
}
