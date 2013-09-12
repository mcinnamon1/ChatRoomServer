import java.net.*; 

public class User 
{
	private String name;
	private Socket socket;
	
	public User(String username)
	{
		name = username;
		socket = null;
	}
	
	public void changeName (String newName)
	{
		name = newName;
	}
	
	public void assignSocket (Socket s)
	{
		socket = s;
	}
	
	public Socket getSocket()
	{
		return socket;
	}
	
	public String getName()
	{
		return name;
	}
	

}