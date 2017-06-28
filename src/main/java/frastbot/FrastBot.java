package frastbot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import de.daslaboratorium.machinelearning.classifier.Classification;

/**
 * A simple ping-pong bot.
 */
public class FrastBot
{
	private MarkovMachine markov;
	private DiscordAPI api;
	private boolean mute;
	private int chatty;
	private ArrayList<Channel> channels;
	private long lastSave;
	
    public FrastBot(String token)
    {
        // See "How to get the token" below
    	markov = new MarkovMachine();
        api = Javacord.getApi(token, true);
        mute = false;
        chatty = 5;
        channels = new ArrayList<Channel>();
        
        lastSave = System.currentTimeMillis();
        
        // connect
        api.connect(new FutureCallback<DiscordAPI>() {
            
            public void onSuccess(DiscordAPI api)
            {
                // register listener
                api.registerListener(new MessageCreateListener()
                {
					public void onMessageCreate(DiscordAPI api, Message message)
                    {
						String serverID = message.getChannelReceiver().getServer().getId();
						String[] tokens = message.getContent().split(" ");

						// Respond to frast commands
						if (tokens[0].equals("!frast") && !serverID.equals("125648631100473344"))
						{
							// insufficient tokens
							if (tokens.length == 1)
							{
								// help
							}
							
							// learn
							else if (tokens[1].equals("learn"))
							{
								transform("Kang Frast", message);
							}
							
							// chatty
							else if (tokens[1].equals("chatty"))
							{
								if (tokens.length < 3)
									message.reply("*usage:* !frast chatty <1-100>");
								
								try
								{
									chatty = Integer.parseInt(tokens[2]);
									chatty = Math.max(0, chatty);
									chatty = Math.min(1000, chatty);
								}
								catch (Exception e) {}
							}
							
							// help
							else if (tokens[1].equals("help"))
							{
								message.reply(getHelpMessage());
							}
							
							// mute
							else if (tokens[1].equals("mute"))
							{
								mute = true;
								api.setIdle(true);
							} 
							
							// unmute
							else if (tokens[1].equals("unmute"))
							{
								mute = false;
								api.setIdle(false);
							}
							
							// save
							else if (tokens[1].equals("save"))
							{
								saveData();
							}
							
							// load
							else if (tokens[1].equals("load"))
							{
								ObjectInputStream objectinputstream = null;
								FileInputStream streamIn = null;
								try {
								    streamIn = new FileInputStream("timbrain");
								    objectinputstream = new ObjectInputStream(streamIn);
								    markov = (MarkovMachine) objectinputstream.readObject();
								    completeTransformation(markov.username, message.getChannelReceiver());
								    System.out.println("Loaded timmy's brain!");
								} catch (Exception e) {
								    e.printStackTrace();
								} finally {
								    if(objectinputstream != null)
								    	try
								    	{
								    		objectinputstream .close();
								    	} catch (Exception e) { e.printStackTrace(); }
								}
							}
						}
						
						// Respond to regular messages, if transformed
						else if (markov != null && markov.username != null && !mute)
				    	{
							if (message.getAuthor().getName().equals(markov.username))
							{
								markov.learnMessage(message);
							}
							
							if (serverID.equals("125648631100473344"))
								return;
							
							double rand = Math.random();
				    		double p = chatty / 1000.0;
				    		
				    		
				    		// Guaranteed reply if message mentions frast
				    		if (message.getContent().toLowerCase().contains("frast"))
				    			p = 1.0;
				    		
				    		System.out.println("Considering message: " + message.getContent());
				    		System.out.println("Reply prob: " + p);
				    		System.out.println("Reply roll: " + rand);
				    		
				    		if (rand < p)
				    		{
				    			String reply = markov.makeComment();
				    			System.out.println("Reply confirmed: " + reply);
				    			message.reply(reply);
				    		}
				    	}
						
						else
						{
							// Save if it's been more than a day since the last save
							long time = System.currentTimeMillis(); 
			                System.out.println(time - lastSave);
			                if (time - lastSave >= 86400000 && markov != null)
			                	saveData();
						}

                    }
					
					 
                });
                
               
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void transform(String username, Message message)
    {    		
    	Channel c = message.getChannelReceiver();
    	
    	if (channels.contains(c))
    	{
    		message.reply("*frast already knows this channel*");
    		return;
    	}
    	
		Future<MessageHistory> future = c.getMessageHistory(200000);
		MessageHistory mh = null;
		
		try
		{
			mh = future.get();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
		List<Message> allMessages = mh.getMessagesSorted();
		
		// Initialize markov machine and train classifier
		markov.learn(allMessages, username);
		
		if (markov.username == null)
			System.err.println("transformation failed!");
		else
		{
			completeTransformation(username, c);
			channels.add(c);
		}
	 
		
    }
    
    public void completeTransformation(String username, Channel c)
    {
    	// Create nickname
		StringBuilder nick = new StringBuilder();
		c.getServer().updateNickname(api.getYourself(), "\"" + username + "\"");
    }
    
    public void saveData()
    {
    	FileOutputStream fout = null;
		ObjectOutputStream oos = null;
		try
		{
			fout = new FileOutputStream("timbrain");
			oos = new ObjectOutputStream(fout);
			oos.writeObject(markov);
			
			System.out.println("Saved timmy's brain!");
		} catch (Exception e) { e.printStackTrace(); }
		finally
		{
			if(oos != null)
				try
				{
					oos.close();
				} catch (Exception e) { e.printStackTrace(); }
		}
		
		lastSave = System.currentTimeMillis();
    }
    
    public String getHelpMessage()
    {
    	return "*commands:* chatty <1-100>, mute, unmute";
    }
}
