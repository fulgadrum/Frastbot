package frastbot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;

public class MarkovMachine
{
	//1								2							3
	CountingHashtable<String, CountingHashtable<String, CountingHashtable<String, Integer>>> map;
	Classifier<String, String> bayes;
	User target;
	
	public MarkovMachine()
	{
		map = new CountingHashtable<String, CountingHashtable<String, CountingHashtable<String, Integer>>>(1000);
		bayes = new BayesClassifier<String, String>();
		target = null;	
	}
	
	public void learn(List<Message> msgs, String username)
	{
		for (int i = 0; i < msgs.size(); i++)
		{
			Message m = msgs.get(i);
			
			// Populate word map if the target user posted it
			if (msgs.get(i).getAuthor().getName().equals(username))
			{
				if (target == null)
					target = msgs.get(i).getAuthor();
				
				// Tokenize the current message
				String content = "^ " + m.getContent() + " ^";
				String[] tokens = content.split(" ");
				
				tokens = sanitizeContent(tokens);
				addTokens(tokens);
			}

			
			// Train on the current message
			String[] tokens = m.getContent().split(" ");
			if (i < msgs.size() - 1)
			{
				String category;
				if (msgs.get(i+1).getAuthor().getName().equalsIgnoreCase(username))
					category = "reply";
				else
					category = "ignore";
				
				bayes.learn(category, Arrays.asList(tokens));
			}	
		}
	}
	
	public String[] sanitizeContent(String[] tokens)
	{
		ArrayList<String> tokensClean = new ArrayList<String>();
		
		for (String token : tokens)
			if (!token.contains("http"))	// exclude links
			{
				tokensClean.add(token);
			}
		
		return tokensClean.toArray(new String[0]);
	}
	
	public void addTokens(String[] tokens)
	{
		for (int i = 0; i < tokens.length - 2; i++)
		{
			String a = tokens[i];
			String b = tokens[i+1];
			String c = tokens[i+2];
			
			if (map.get(a) == null)
				map.put(a, new CountingHashtable<String, CountingHashtable<String, Integer>>());
			
			if (map.get(a).get(b) == null)
				map.get(a).put(b, new CountingHashtable<String, Integer>());
			
			if (map.get(a).get(b).get(c) == null)
				map.get(a).get(b).put(c, 0);
			
			map.get(a).total++;
			map.get(a).get(b).total++;
			map.get(a).get(b).put(c, new Integer(map.get(a).get(b).get(c) + 1));
		}
	}
	
	public String makeComment()
	{
		StringBuilder comment = new StringBuilder('^');
		String curr = "";
		String next = "^";
		
		// Get beginning
		int rand = (int) (map.get("^").total * Math.random()) + 1;
		for (String key2 : map.get("^").keySet())
		{
			rand -= map.get("^").get(key2).total;
			if (rand <= 0)
			{
				curr = next;
				next = key2;
				comment.append(key2 + " ");
				break;
			}
		}
		
		do
		{
			{
				rand = (int) (map.get(curr).get(next).total * Math.random());
				for (String key3 : map.get(curr).get(next).keySet())
					if (map.get(curr).get(next).total > 0)
					{
						rand -= map.get(curr).get(next).get(key3);
						if (rand <= 0)
						{			
							curr = next;
							next = key3;
							break;
						}
					}
				if (!next.equals("^"))
					comment.append(next + " ");
				
			}
		} while (!next.equals("^"));

		return comment.toString();
	}
	
	public User getTarget()
	{
		return target;
	}
	
//	public void printMap()
//	{
//		for (String key : map.keySet())
//		{
//			System.out.println(">" + key + " [" + map.get(key).total + "]");
//			for (String key2 : map.get(key).keySet())
//			{
//				System.out.println("\t>>" + key2 + " [" + map.get(key).get(key2).total + "]");
//				for (String key3 : map.get(key).get(key2).keySet())
//					System.out.println("\t\t>>>" + key3 + " " + map.get(key).get(key2).get(key3) + " (" + key + " " + key2 + " " + key3 + ")");
//			}
//		}
//	}
	
	class CountingHashtable<K, V> extends Hashtable<K, V>
	{
		int total;
		
		public CountingHashtable(int initialCapacity)
		{
			super(initialCapacity);
			total = 0;
		}
		
		public CountingHashtable()
		{
			super();
			total = 0;
		}
	}
}
