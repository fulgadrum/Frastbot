package frastbot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import de.daslaboratorium.machinelearning.classifier.Classification;;

/**
 * A simple ping-pong bot.
 */
public class DittoBot
{
	private MarkovMachine markov;
	private DiscordAPI api;
	private boolean mute;
	
    public DittoBot(String token)
    {
        // See "How to get the token" below
    	markov = new MarkovMachine();
        api = Javacord.getApi(token, true);
        mute = false;
        
        // connect
        api.connect(new FutureCallback<DiscordAPI>() {
            
            public void onSuccess(DiscordAPI api)
            {
                // register listener
                api.registerListener(new MessageCreateListener()
                {
					public void onMessageCreate(DiscordAPI api, Message message)
                    {
						String[] tokens = message.getContent().split(" ");

						// Respond to diddo commands
						if (tokens[0].equals("!diddo"))
						{
							// insufficient tokens
							if (tokens.length == 1)
							{
								// help
							}
							
							// transform
							else if (tokens[1].equals("transform"))
							{
								if (!message.getAuthor().getName().equals("forks"))
					            {
					            	message.reply("*ha ha nice try fucker*");
					            	return;
					            }
								
								else if (tokens.length > 2)
									transform(tokens[2], message);
								else
									message.reply("*usage: !diddo transform <username>*");
							}
							
							// status
							else if (tokens[1].equals("status"))
							{
								String status = "*currently imitating: ";
								if (markov.getTarget() != null)
									status += markov.getTarget().getName();
								else
									status += "no one";
								
								message.reply(status + "*");
							}
							
							// speak
							else if (tokens[1].equals("speak"))
							{
								if (markov.target == null)
									message.reply("*diddo ain't transformed, dummy!*");
								else
									message.reply(markov.makeComment());
							}
							
							// help
							else if (tokens[1].equals("help"))
							{
								message.reply("*commands: transform <user>, speak, status, mute, unmute*");
							}
							
							// mute
							else if (tokens[1].equals("mute"))
							{
								mute = true;
							} 
							
							// unmute
							else if (tokens[1].equals("unmute"))
							{
								mute = false;
							}
						}
						
						// Respond to regular messages, if transformed
						else if (markov != null && markov.getTarget() != null && !mute)
				    	{
				    		String[] classifyTokens = message.getContent().split(" ");
				    		double rand = Math.random();
				    		
				    		Classification<String, String> cl = markov.bayes.classify(Arrays.asList(classifyTokens));
				    		double p = cl.getProbability();
				    		
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
		markov.target = null;
		message.reply("*diddo used transform!*");
		
//		Future<MessageHistory> future = c.getMessageHistory(99999999);
		Future<MessageHistory> future = c.getMessageHistory(9999);
		
		
		
		try
		{
			MessageHistory mh = future.get();
			List<Message> allMessages = mh.getMessagesSorted();
			
			// Initialize markov machine and train classifier
			markov.learn(allMessages, username);
			
			if (markov.getTarget() == null)
			{
				message.reply("*but, it failed!*");
				c.getServer().updateNickname(api.getYourself(), "diddo");
			}
			else
			{
				// Create nickname
				StringBuilder nick = new StringBuilder();
				for (int i = 0; i < markov.getTarget().getName().length(); i++)
				{
					if (Math.random() < 0.5)
						nick.append(markov.getTarget().getName().substring(i, i+1).toUpperCase());
					else
						nick.append(markov.getTarget().getName().substring(i, i+1).toLowerCase());
				}
				
				c.getServer().updateNickname(api.getYourself(), nick.toString());
				message.reply("*diddo transformed into " + markov.getTarget().getName() + "!*");
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args)
    {
    	DittoBot ppb = new DittoBot("MzAzODE4MTA5NDg2MjM1NjQ4.C9doPw.FOTkOL_EMrI67O5C4g62O0rw8J0");
    }
}
