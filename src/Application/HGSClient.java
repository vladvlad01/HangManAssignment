/*
 * Vlad Ciobanu
 * C15716369 
 */

package Application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class HGSClient {

	int port = 2323;
	char currentGame[];
	boolean gameHasEnded=false;
	int mistakes = 0;
	public static void main(String args[])
	{
		try {
			new HGSClient();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void log(String s)
	{
		System.out.println(s);
	}
	public void updateGameDisplay(char[] game,int mistakes)
	{
		log("Your progress is  "+new String(game)+" so far. You have "+mistakes+" mistakes out of 5.");
	}
	public HGSClient() throws Exception
	{
		Scanner scan = new Scanner(System.in);
		log("Connecting to Player1");
		Socket serverConnection = new Socket("localhost",port);
		DataOutputStream out = new DataOutputStream(serverConnection.getOutputStream());
		DataInputStream in = new DataInputStream(serverConnection.getInputStream());
		log("Connected to Player1");
		currentGame = in.readUTF().toCharArray();
		updateGameDisplay(currentGame,mistakes);
		do
		{
			log("Please enter a character to guess, or the whole word if you know it. Enter \"bye\" to disconnect. Enter \"retry\" to restart.");
			String message = scan.nextLine();
			if (message.length() == 0)
			{
				log("Invalid input.");
			}
			else
			{
				handleMessage(message,in,out);
			}
		}
		while(!gameHasEnded);
		in.close();
		out.close();
		serverConnection.close();
	}
	private void handleMessage(String message, DataInputStream in, DataOutputStream out) throws IOException 
	{
		if (message.equalsIgnoreCase("bye"))
		{
			log("You have disconnected.");
			gameHasEnded=true;
			out.writeUTF("-1");
		}
		else if (message.equalsIgnoreCase("retry"))
		{
			log("The game has restarted. Please wait until Player1 enters a new word.");
			out.writeUTF("2");
			currentGame = in.readUTF().toCharArray();
			mistakes = in.readInt();
			updateGameDisplay(currentGame,mistakes);
		}
		else
		{
			if (message.length() == 1)
			{
				out.writeUTF("0");
				boolean canProceed = in.readBoolean();
				if (canProceed)
				{
					out.writeChar(message.charAt(0));
					boolean isOK = in.readBoolean();
					if (isOK)
					{
						log("You are correct!");
						String data[] = in.readUTF().split(";");
						for (String s:data)
						{
							int position = Integer.parseInt(s);
							currentGame[position] = message.charAt(0);
						}
					}
					else
					{
						log("You are wrong!");
					}
					mistakes = in.readInt();
					updateGameDisplay(currentGame,mistakes);
				}
				else
				{
					log("You do not have any attempts remaining or the game might be over!");
				}
			}
			else 
			{
				out.writeUTF("1");
				boolean canProceed = in.readBoolean();
				if (canProceed)
				{
					out.writeUTF(message);
					boolean isOK = in.readBoolean();
					if (isOK)
					{
						log("You got it right! Game Over! The word is: "+message);
					}
					else
					{
						log("You are wrong! Game Over! Type \"bye\" to exit or \"retry\" to restart.");
					}
				}
				else
				{
					log("The game has ended! Please exit or restart using the appropiate commands.");
				}
			}
		}
		
	}
}
