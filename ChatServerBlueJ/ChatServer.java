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
                
                /* User commands -- initialized with "/" and a keyword (nick, voteoff, whisper, disconnect)*/
                
                /* /nick command: changes the user's nickname */
                if(fromClient.length() >= 5 && fromClient.substring(0,5).equals("/nick"))
                {
                    synchronized(ChatServer.threads) {
                        if (fromClient.length() < 7)
                            this.out.println("(You forgot to enter a new nickname!)");
                        else {
                            String oldName = this.user.getName();
                            String newName = fromClient.substring(6);
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
                }
                
                else if(fromClient.length()>= 8 && fromClient.substring(0,8).equals("/voteoff"))
                {
                    synchronized(ChatServer.threads) {
                       if (fromClient.length() < 10)
                            this.out.println("(You forgot to enter a victim!)");
                       else {
                           String victim = fromClient.substring(9);
                           boolean warningSent = false;
                            
                           //make while-loop that counts up & checks for sent boolean
                           int r = 0;
                           int numVotes = 1;
                           int j = 0;
                            
                           while(j<ChatServer.threads.size() && warningSent == false)
                           {
                               String name = ChatServer.threads.get(j).user.getName();
                               if (name.equals(victim))
                               {
                                   ChatServer.threads.get(j).out.println("A vote has been issued against you. Please hold to receive your fate.");
                                   warningSent = true;
                               }
                               j++;   
                           }
                                
                           while(r<ChatServer.threads.size())
                           {
                               ChatThread current = ChatServer.threads.get(r);
                               if(!current.equals(this) && !current.user.getName().equals(victim))
                               {
                                   current.out.println("<" + user.getName() + "> wants to vote " + victim + " off the chat. Do you agree (yes or no)? Please hit return before entering your response.");
                                   String response = current.in.readLine();
                                   if(response.equalsIgnoreCase("yes"))
                                   {
                                       numVotes++;
                                   }
                               }
                               r++;
                           }
    
                           if(numVotes == (ChatServer.threads.size()-1))
                           {
                               for(int z = 0; z < ChatServer.threads.size(); z++)
                               {
                                   ChatThread current = ChatServer.threads.get(z);
                                   if(current.user.getName().equals(victim))
                                   {
                                       current.out.println("You have been voted off the chat. :( Goodbye! Please close the chat.");
                                       current.in.close();
                                       current.out.close();
                                       current.socket.close();
                                       int ind = ChatServer.threads.indexOf(current);
                                       ChatServer.threads.remove(ind);
                                   }
                                   else
                                   {
                                       current.out.println(victim + " has been voted off the chat. Way to go.");
                                   }
                               }
                           }
                           else
                           {
                               for(int w = 0; w < ChatServer.threads.size(); w++)
                               {
                                   ChatThread current = ChatServer.threads.get(w);
                                   if(!current.user.getName().equals(victim))
                                   {
                                       current.out.println(victim + " did not receive enough votes against him/her to be voted off the chat. Better luck next time!");
                                   }
                                   else if (current.user.getName().equals(victim))
                                   {
                                       current.out.println("Congratulations - you are safe... for now.");
                                   }
                               }
                           }
                       }
                    }
                }
                    
                /* /whisper command: whispers to someone private, specified by /whisper name: message */
                else if(fromClient.length() >= 8 && fromClient.substring(0,8).equals("/whisper"))
                {
                    synchronized(ChatServer.threads) {
                        if (fromClient.length() < 10)
                            this.out.println("(You forgot to enter a recipient!)");
                            
                        else {
                            
                            int colon = fromClient.indexOf(":");
                            
                            if (colon < 0) //no ':' or forgot about writing recipient's name
                                this.out.println("Make sure to add a ':' after the recipient's username!");
                            
                            else if (fromClient.length() - colon - 1 > 2) //checks for a message first; prevents potential NullPointerException
                            {
                                String recipient = fromClient.substring(9, colon);
                                boolean sent = false;
                                
                                //make while-loop that counts up & checks for sent boolean
                                int x = 0;
                                while(x<ChatServer.threads.size() && sent == false)
                                {
                                    ChatThread current = ChatServer.threads.get(x);
                                    if(current.user.getName().equals(recipient))
                                    {
                                        current.out.println("<" + user.getName() + "> whispered to you: " + fromClient.substring(colon + 2));
                                        sent = true;
                                    }
                                    x++;
        
                                }
                                
                                if (!sent) { 
                                    this.out.println("IOException: user does not exist!");
                                }
                            }
                        }
                    }
                }
                
                /* /disconnect command: disconnects the user and prints the user's parting message */
                else if(fromClient.length() >= 11 && fromClient.substring(0, 11).equals("/disconnect"))
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

                
                /*Outputs the current user's fromClient message on all other clients' screens*/
                else 
                {
                    synchronized(ChatServer.threads)
                    {
                        if (fromClient.length() > 0 && !fromClient.substring(0, 1).equals("/")) 
                        {
                            for (int x = 0; x < ChatServer.threads.size(); x++)
                            {
                                ChatThread current = ChatServer.threads.get(x);
                                if(!current.equals(this))
                                    current.out.println("<" + user.getName() + "> " + fromClient);
                            }
                        }
                    }
                }
                
            }
            catch (IOException e) {
                /* On exception, stop the thread */
                System.out.println("IOException_2Run: " + e);
                return;
            }
        }
    }
    
}

public class ChatServer {
    public static ArrayList<ChatThread> threads;
    public static ArrayList<String> names;
    
    public static void main(String [] args) {
        threads = new ArrayList<ChatThread>();
        names = new ArrayList<String>();
        names.add("James"); names.add("Bob"); names.add("Glenda"); names.add("Lafundah"); names.add("Zeus"); names.add("Athena"); names.add("Zion"); names.add("Moses"); names.add("Jimmy"); names.add("Suzy"); names.add("Lily"); names.add("Aphrodite");
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
                int random = (int)(Math.random()*12);
                User newUser = new User(names.get(random));
                names.remove(random);
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
