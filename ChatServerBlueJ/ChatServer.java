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
        /* Our thread is going to read lines and commands from the client and display the message or change
         * accordingly on each users window.
           It will continue to do this until an exception occurs or the connection ends.
           */
        while (true) {
            try {
                /* Get string from client */
                String fromClient = this.in.readLine();
                
                /* If null, connection is closed, so just finish */
                //JR: the program hasn't actually been doing this -- not recognizing a return as a null
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
                
                /* User commands -- initialized with "/" and a keyword */
                if (fromClient.length() > 0 && fromClient.substring(0,1).equals("/")) {
                    
                    /* /nick command: changes the user's nickname */
                    if(fromClient.length() >= 5 && fromClient.substring(0,5).equals("/nick"))
                    {
                        synchronized(ChatServer.threads) {
                            String oldName = this.user.getName();
                            String newName;
                            if (fromClient.length() < 7)
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
                                }
                            }
                        }
                    }
                    
                    /* /disconnect command: disconnects the user and prints the user's parting message */
                    if(fromClient.length() >= 11 && fromClient.substring(0, 11).equals("/disconnect"))
                    {
                        synchronized(ChatServer.threads) {
                            for (int x = 0; x < ChatServer.threads.size(); x++)
                            {
                                ChatThread current = ChatServer.threads.get(x);
                                if (current.equals(this))
                                {
                                    current.out.println("You disconnected.");
                                }
                                else
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
                        return;
                    }
                }
                else 
                {
                    /*Current Thoughts (Sep 15, 11a / JR): 
                    I'm still not sure how he got "this" user's own name to print first in < > before the input -- 
                    it would probably be written further up, but even if it does print, when another user types
                    something first, it would keep showing up as the user's name with a lot of blank lines before/after...
                    
                    It won't be an issue when we know who's going at what time, but if we're on separate computers,
                    and one user interrupts the other, than the problem above occurs.
                    */
                
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
