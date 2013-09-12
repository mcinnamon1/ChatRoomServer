import java.net.*;
import java.util.ArrayList;
import java.io.*;



class ChatThread implements Runnable {
    
    /* The client socket and IO we are going to handle in this thread */
    protected Socket         socket;
    protected PrintWriter    out;
    protected BufferedReader in;
    protected User user;

    
    public ChatThread(Socket socket, User user1) { //constructor for the threads (created at bottom)
        /* Assign local variable */
        this.socket = socket;
        user = user1;

        
        /* Create the I/O variables */
        try {
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in  = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            
            /* Some debug */
            synchronized(this)
            {
                ChatServer.threads.add(this);
            	for (int x = 0; x < ChatServer.threads.size(); x++)
            	{
            		ChatThread current = ChatServer.threads.get(x);
            		current.out.println(this.user.getName() + " connected!");
            	}


            }
            
            /* Say hi to the client */
            this.out.println("Welcome to the server!");
            //immediately in constructor
            
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }
    
    public void run() {
        /* Our thread is going to read lines from the client and parrot them back.
           It will continue to do this until an exception occurs or the connection ends
           */
        while (true) {
            try {
                /* Get string from client */
                String fromClient = this.in.readLine();
                
                /* If null, connection is closed, so just finish */
                if (fromClient == null) {
                	 synchronized(this)
                     {
                     	for (int x = 0; x < ChatServer.threads.size(); x++)
                     	{
                     		ChatThread current = ChatServer.threads.get(x);
                     		current.out.println(this.user.getName() + " disconnected.");
                     	}
                     	this.in.close();
                     	this.out.close();
                     	this.socket.close();
                     	int index = ChatServer.threads.indexOf(this.user);
                     	ChatServer.threads.remove(index);
                     }
                    return;
                }
                
                /* If the client said "bye", close the connection */
                
                if (fromClient.equals("bye")) {
                	
                	 synchronized(this)
                     {
                     	for (int x = 0; x < ChatServer.threads.size(); x++)
                     	{
                     		ChatThread current = ChatServer.threads.get(x);
                     		current.out.println(this.user.getName() + ": " + fromClient); 
                     		current.out.println(this.user.getName() + " disconnected.");
                     	}
                     	this.in.close();
                     	this.out.close();
                     	this.socket.close();
                     	int index = ChatServer.threads.indexOf(this.user);
                     	ChatServer.threads.remove(index);
                     }
                    return;
                }
                
                /* Otherwise parrot the text */
                //use "System" so not personal to server?
                synchronized(this)
                {
                	for (int x = 0; x < ChatServer.threads.size(); x++)
                	{
                		ChatThread current = ChatServer.threads.get(x);
                		current.out.println(user.getName() + ": " + fromClient); 
                	}
                }
                //System.out.println("Client said: " + fromClient);
                
            } catch (IOException e) {
                /* On exception, stop the thread */
                System.out.println("IOException: " + e);
                return;
            }
        }
    }
    
}

public class ChatServer {
	public static ArrayList<ChatThread> threads;
    
    public static void main(String [] args) {
    	threads = new ArrayList<ChatThread>();
        
        /* Check port exists */
        if (args.length < 1) {
            System.out.println("Usage: ParrotServerExample <port>");
            System.exit(1);
        }
        
        /* This is the server socket to accept connections */
        ServerSocket serverSocket = null;
        
        /* Create the server socket */
        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        } catch (IOException e) {
            System.out.println("IOException: " + e);
            System.exit(1);
        }
        
        /* In the main thread, continuously listen for new clients and spin off threads for them. */
        while (true) {
            try {
                /* Get a new client */
                Socket clientSocket = serverSocket.accept(); //blocks until something connects --> spins off
                											//new socket for each (clientSocket)
                
                /* Create a thread for it and start! */
                User newUser = new User("User" + (threads.size()+1));
                newUser.assignSocket(clientSocket);
                ChatThread clientThread = new ChatThread(clientSocket, newUser);

                //threads.add(clientThread);

                
                
                new Thread(clientThread).start();
                
            } catch (IOException e) {
                System.out.println("Accept failed: " + e);
                System.exit(1);
            }
        }
    }
    
}
