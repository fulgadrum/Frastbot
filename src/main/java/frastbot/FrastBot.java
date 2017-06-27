package frastbot;

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
	
    public FrastBot(String token)
    {
        // See "How to get the token" below
    	markov = new MarkovMachine();
        api = Javacord.getApi(token, true);
        mute = false;
        chatty = 100;
        
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

						// Respond to frast commands
						if (tokens[0].equals("!frast"))
						{
							// insufficient tokens
							if (tokens.length == 1)
							{
								// help
							}
							
							// transform
							else if (tokens[1].equals("arise"))
							{
								if (markov.getTarget() == null)
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
									chatty = Math.min(100, chatty);
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
						}
						
						// Respond to regular messages, if transformed
						else if (markov != null && markov.getTarget() != null && !mute)
				    	{
							String[] classifyTokens = message.getContent().split(" ");
				    		double rand = Math.random();
				    		
				    		Classification<String, String> cl = markov.bayes.classify(Arrays.asList(classifyTokens));
				    		double p = cl.getProbability();
				    		
				    		if (message.getAuthor() != api.getYourself())
				    			p *= chatty / 100.0;
				    		
				    		
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
		
		Future<MessageHistory> future = c.getMessageHistory(150000);
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
		
		if (markov.getTarget() == null)
			System.err.println("transformation failed!");
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
			
			c.getServer().updateNickname(api.getYourself(), "\"" + markov.getTarget().getName() + "\"");
		}
	 
		
    }
    
    public String getHelpMessage()
    {
    	return "*commands:* chatty <1-100>, mute, unmute";
    }
}
