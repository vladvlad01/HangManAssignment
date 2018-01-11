/*
 * Vlad Ciobanu
 * C15716369 
 */

package Application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class HGServer 
{
	int port = 2323;
	char currentGame[],word[];
	int mistakes = 0;
	Scanner scan;
	boolean isGameOver = false;
	
	public static void main(String args[])
	{
		try 
		{
			
			new HGServer();
		} catch (IOException e) 
		{
			log("An error has occured. Error codename: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void log(String s)
	{
		System.out.println(s);
	}
	public boolean isLetterOK(char c)
	{
		for (int i=0;i<word.length;i++) //for every letter in our real word
		{
			if (word[i]==c && currentGame[i]=='*') //if the letter in the current position matches the one given 
				//as a parameter and it is still hidden in the real word
			{
				return true; //then the letter is ok
			}
			else if (word[i] == c && currentGame[i]!='*') //if the letter in the current position matches 
				//the one given as a parameter, but it was already found
			{
				return false; // it's not ok
			}
		}
		return false; // it's not ok in any other case but the above
	}
	public char[] getNewWord(char c)
	{
		//This should only be called if letter c is a valid move
		if (isLetterOK(c))//If we somehow called this method with a letter that is not ok, we check it here.
		{
			char toReturn[] = Arrays.copyOf(currentGame, currentGame.length); //we create a copy of our current game status
			for (int i=0;i<toReturn.length;i++) //for each letter in the current game
			{
				if (toReturn[i]=='*' && word[i]==c) //if it's hidden and meant to be shown
				{
					toReturn[i] = c; // we mark it as found
				}
			}
			return toReturn; //return the newly updated game state
		}
		else
		{
			; //We should never get here unless we broke something
			return null;
		}
	}
	private void updateGameDisplay(int mistakes2, char[] currentGame2) 
	{
		log("The progress is  "+new String(currentGame2)+" so far. Player2 has "+mistakes+" mistakes out of 5.");
		
	}
	public HGServer() throws IOException
	{
		scan = new Scanner(System.in);
		initializeNewGame(scan);
		
		
		ServerSocket serversocket = new ServerSocket(port); //we begin to accept new clients
		log("Waiting for Player2");
		Socket clientConnection = serversocket.accept(); //when the client has connected
		//I choose to use Data Output and Input rather than Buffered because its better for transmitting primitives, 
		//length-prefixed strings etc.
		DataOutputStream out = new DataOutputStream(clientConnection.getOutputStream()); //we initialize input&output streams
		DataInputStream in = new DataInputStream(clientConnection.getInputStream());
		log("Player2 has connected.");
		out.writeUTF(new String(currentGame)); //we tell the client our game status
		String data[];
		do
		{
			String message = in.readUTF(); //we keep receiving different messages
			data = message.split(";"); //and we separate them by a separator for ease of processing
			handleMessage(data,in,out); //then we take care of the message
		}
		while(!data[0].equals("-1")); //the "-1" indicates the client wants to disconnect
		log("Player2 has disconnected.");
		in.close(); //we close every connection
		out.close();
		clientConnection.close();
		serversocket.close();
	}
	private void initializeNewGame(Scanner scan) 
	{
		log("Please enter a word to be guessed by Player2");
		String readWord = "";
		do
		{
			readWord = scan.nextLine();
			if (readWord.length() == 0)
			{
				log("Invalid input. Please re-enter the word.");
			}
			else
			{
				break;
			}
		}
		while(true);
		
		this.word = readWord.toCharArray();
		Random rand = new Random();
		int randomPos = rand.nextInt(word.length);
		currentGame = new char[word.length];
		Arrays.fill(currentGame, '*');	//we set the whole world to be guessed as *
		currentGame[randomPos]=word[randomPos]; //except for one random letter that will be shown from the beginning
		mistakes = 0; //we initialize mistakes with 0 because it's a new game
		isGameOver = false;
		updateGameDisplay(mistakes,currentGame); //we show it in the console
	}
	private void handleMessage(String[] data, DataInputStream in, DataOutputStream out) throws IOException //handling received messages and 
	//delivering appropriate responses 
	{
		int messageType = Integer.parseInt(data[0]); //parsing the message ID for ease of processing
		if (messageType == 0) //if the client sent a letter
		{
			if (mistakes<5 && !isGameOver) //if we should do something with it
			{
				out.writeBoolean(true); //inform the client we can
				
				char c = in.readChar(); //then read the character the client wants us to process
				if (isLetterOK(c)) //if it matches anything
				{
					log("Player2 has entered a correct character: "+c);
					String positions="";
					char[] currentGameUpdated = getNewWord(c);//get the new game status
					for (int i=0;i<currentGameUpdated.length;i++)
					{
						if (currentGameUpdated[i]!=currentGame[i])
						{
							positions+=i+";";	//record the differences (their positions)
						}
					}
					currentGame = currentGameUpdated; //update the global game
					
					out.writeBoolean(true); //tell the client the letter is okay
					out.writeUTF(positions); //also tell it where it should be replaced in his copy of the game status
				}
				else
				{
					log("Player2 has entered a wrong character: "+c);
					mistakes++;
					out.writeBoolean(false); //increment mistakes and tell the client the letter was not correct
				}
				out.writeInt(mistakes); //in any case tell the client how many mistakes it has
				
				updateGameDisplay(mistakes,currentGame);
			}
			else
			{
				out.writeBoolean(false); //we could not process the request because the game is over or there are no attempts left
			}
			
		}
		else if (messageType == 1) //if the client sends the whole word
		{
			if (!isGameOver) //if it's worth processing the word
			{
				out.writeBoolean(true); //inform the client
				String guessedFinalWord = in.readUTF(); //read the "final" word
				log("Player2 has entered the word: "+guessedFinalWord);
				if (guessedFinalWord.equals(new String(word))) //if it is correct
				{
					out.writeBoolean(true); //inform the client
				}
				else
				{
					out.writeBoolean(false); //like above
				}

				isGameOver = true; //set the game to be over according to the game rules (one whole word only)
			}
			else
			{
				out.writeBoolean(false); //we cannot process the requset
			}
		}
		else if (messageType == 2) //restarting the game
		{
			initializeNewGame(scan); //initialize a new game
			out.writeUTF(new String(currentGame)); //tell the client the new partially shown word
			out.writeInt(mistakes); //also the mistakes
		}
	}
	
}
