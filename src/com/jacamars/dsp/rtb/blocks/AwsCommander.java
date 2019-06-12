package com.jacamars.dsp.rtb.blocks;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.jacamars.dsp.rtb.common.Configuration;

import redis.clients.jedis.Jedis;

/**
 * A class that handles the actual command received by the subscriber.
 * @author Ben M. Faul
 *
 */
public class AwsCommander {
	
	String msg = null;
	boolean err = false;
	/**
	 * Constructor. Does the actual command.
	 * @param message String. The message sent in the command.
	 */
	public AwsCommander(String message) {
		String[] tokens = message.split(",");
		String[] parts = tokens[0].split(" ");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}

		try {
			switch (parts[0]) {
			case "load":
				msg = load(parts);
				break;
			case "delete":
				delete(parts[1]);
				msg = "Symbol " + parts[1] + " removed";
				break;
			default:
			}
		} catch (Exception error) {
			msg = error.toString();
			err = true;
			error.printStackTrace();
		}
	}

	/**
	 * Return the message from the load
	 * @return String. The message to return to the caller.
	 */
	public String getMessage() {
		return msg;
	}
	
	/**
	 * Return whether the command errored
	 * @return boolean. Returns true if was an error.
	 */
	public boolean errored() {
		return err;
	}
	
	void delete(String key) {
		LookingGlass.symbols.remove(key);
	}
	
	/**
	 * Load the file or s3 object.
	 * Eg: load s3 bloom $name data/object
	 * Note S3 bucket is predefined by the startup. See the docker compose file.
	 * @param parts String[]. An array of tokens.
	 * @return String. The message returned from the load command.
	 * @throws Exception on I/O errirs.
	 */
	String load(String[] parts) throws Exception {
		String otype = null;
		String symbolName = null;
		String name;
		String type = parts[1]; // file or S3
		//for (int i=0;i<parts.length;i++) {
		//	System.out.println("Part[" + i + "] = " + parts[i]);
		//}
		otype = parts[2]; // bloom, cache, cuckoo.
		symbolName = parts[3]; // name of the object
		name = parts[4]; // filename or object
		if (!symbolName.startsWith("@")) {
			if (symbolName.charAt(0) != '$')
				symbolName = "@" + symbolName;
		}

		if (type.equals("file")) {
			return Configuration.getInstance().initObject(symbolName,name,type);
		} 
		
		S3Object object = Configuration.getInstance().s3.getObject(new GetObjectRequest(Configuration.getInstance().s3_bucket,name));
		long size = Configuration.s3.getObjectMetadata(Configuration.s3_bucket, name).getContentLength();
		
		return Configuration.getInstance().readData(otype,symbolName,object, size);
	}
}
