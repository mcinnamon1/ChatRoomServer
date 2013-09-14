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
            synchronized(ChatServer.threads)
            {
                ChatServer.threads.add(this);
                for (int x = 0; x < ChatServer.threads.size(); x++)
                {
                    ChatThread current = ChatServer.threads.get(x);
                    if(current.equals(this))
                    {
                        current.out.println("Welcome to Molly and Jackie's Chat Server! You are " + this.user.getName() + ". There are " + (ChatServer.threads.size()-1) + " other users in the chatroom.");
                    }
                    else
                    {
                        current.out.println(this.user.getName() + " connected!");
                    }
                }


            }
            
            /* Say hi to the client */
            this.out.println("Welcome to the server!");
            //immediately in constructor
            
        } catch (IOException e) {
            System.out.println("IOException_1Class: " + e);
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
                     synchronized(ChatServer.threads)
                     {
                        for (int x = 0; x < ChatServer.threads.size(); x++)
                        {
                            ChatThread current = ChatServer.threads.get(x);
                            if (current.equals(this))
                            {
                                current.out.println("You disconnected.");
                            }
                            else
                            {
                                current.out.println(this.user.getName() + " has disconnected from the room.");
                            }
                        }
                        this.in.close();
                        this.out.close();
                        this.socket.close();
                        int index = ChatServer.threads.indexOf(this);
                        ChatServer.threads.remove(index);
                     }
                    return;
                }
                
                /* If the client said "bye", close the connection */
                
                if (fromClient.equals("bye")) {
                    
                     synchronized(ChatServer.threads)
                     {
                        for (int x = 0; x < ChatServer.threads.size(); x++)
                        {
                            ChatThread current = ChatServer.threads.get(x);
                            if (current.equals(this))
                            {
                                current.out.println("You disconnected.");
                            }
                            else
                            {
                                current.out.println(this.user.getName() + " has disconnected from the room. " + this.user.getName() + " says bye!");
                            }
                        }
                        this.in.close();
                        this.out.close();
                        this.socket.close();
                        int index = ChatServer.threads.indexOf(this);
                        ChatServer.threads.remove(index);
                     }
                    return;
                }
                
                if (fromClient.length() > 0 && fromClient.substring(0,1).equals("/")) {
                    
                     synchronized(ChatServer.threads)
                     {
                         if(fromClient.length() >= 5 && fromClient.substring(0,5).equals("/nick"))
                         {
                            String oldName = this.user.getName();
                            String newName;
                            if (fromClient.length() < 7) //JR: changed this too, just in case
                                newName = "random";
                            else
                                newName = fromClient.substring(6);
                            this.user.changeName(newName);
                            for (int x = 0; x < ChatServer.threads.size()   ; x++)
                            {
                                ChatThread current = ChatServer.threads.get(x);
                                if(!current.equals(this))
                                    current.out.println(oldName + " is now " + newName); 
                                else
                                {
                                    current.out.println("You are now " + newName);
                                    current.out.println(this.user.getName());
                                }
                            }
                        }
                        
                        if(fromClient.length() >= 11 && fromClient.substring(0, 11).equals("/disconnect"))
                        {
                                for (int x = 0; x < ChatServer.threads.size(); x++)
                            {
                                ChatThread current = ChatServer.threads.get(x);
                                if (current.equals(this))
                                {
                                    current.out.println("You disconnected.");
                                }
                                else //JR: this had a similar problem with the substring -- fixed it, now can disconnect without a message
                                {
                                    current.out.print(this.user.getName() + " has disconnected from the room.");
                                    if (fromClient.length() > 11)
                                        current.out.println(" (" + fromClient.substring(12) + ")");
                                }
                            }
                            this.in.close();
                            this.out.close();
                            this.socket.close();
                            int index = ChatServer.threads.indexOf(this);
                            ChatServer.threads.remove(index);
                        }
                             
                        
                     }
                }
                else 
                {
                    //Current Thoughts (Sep 13, 7:40p / JR): after doing a "/" command, this code also runs (reprinting the user's actual code
                    //as a message in addition to our cleaner explanation) since the "/" commands can't end with a return statement. I tried
                    //an if-else for now, and it fixed the repeat for after disconnecting, but not for after the name change.
                    //
                    //Also, in Fieldman's example on the worksheet, he had < > instead of :, and he also printed <username> before the "this" user
                    //user typed anything as well. I thought that seemed more uniform, so I changed that below for now, but I'm not sure how
                    //he got "this" user's own name to print first -- it would probably be written further up, but even if it does print, when
                    //another user types something first, it would keep showing up as the user's name with a lot of blank lines before/after...
                
                    /*Outputs the current user's fromClient message on all other clients' screens*/
                    synchronized(ChatServer.threads)
                    {
                        for (int x = 0; x < ChatServer.threads.size(); x++)
                        {
                            ChatThread current = ChatServer.threads.get(x);
                            if(!current.equals(this))
                                current.out.println("<" + user.getName() + "> " + fromClient);
                        }
                    }
                }
                //System.out.println("Client said: " + fromClient);
                
            } catch (IOException e) {
                /* On exception, stop the thread */
                System.out.println("IOException_2Run: " + e);
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
            System.out.println("Usage: ChatServer <port>");
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
