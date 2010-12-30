/*
 * Copyright 2010 Kevin Seim
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.beanio;

import java.io.*;
import java.util.Locale;

import org.beanio.util.*;

/**
 * A <tt>StreamFactory</tt> manages stream configurations and is used create 
 * <tt>BeanWriter</tt> and <tt>BeanReader</tt> instances.
 * <p>
 * The default BeanIO <tt>StreamFactory</tt> implementation can be safely shared 
 * across multiple threads.
 * 
 * @author Kevin Seim
 * @since 1.0
 * @see BeanReader
 * @see BeanWriter
 */
public abstract class StreamFactory {

	/**
	 * Constructs a new <tt>StreamFactory</tt>.
	 */
	public StreamFactory() { }
	
	/**
	 * Creates a new <tt>BeanReader</tt> for reading from a file.
	 * @param name the stream configuration name to use for reading the stream
	 * @param file the file to read
	 * @return the new BeanReader
	 * @throws BeanIOException if there is no stream configured for the given name
	 */
	public BeanReader createReader(String name, File file) throws BeanIOException {
		validateStreamName(name);
		
		Reader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			return createReader(name, in);
		}
		catch (FileNotFoundException ex) {
			throw new BeanIOException(ex);
		}
		catch (BeanIOException ex) {
			IOUtil.closeQuietly(in);
			throw ex;
		}
	}
	
	/**
	 * Creates a new <tt>BeanReader</tt> for reading from the given input stream.
	 * @param name the stream configuration name to use for reading the stream
	 * @param in the input stream to read from
	 * @return the new BeanReader
	 * @throws BeanIOException if there is no stream configured for the given name
	 */
	public BeanReader createReader(String name, Reader in) throws BeanIOException {
		 return createReader(name, in, Locale.getDefault());
	}
	
	/**
	 * Creates a new <tt>BeanReader</tt> for reading from the given input stream.
	 * @param name the stream configuration name to use for reading the stream
	 * @param in the input stream to read from
	 * @param locale the locale used for 
	 * @return the new BeanReader
	 * @throws BeanIOException if there is no stream configured for the given name
	 */
	public abstract BeanReader createReader(String name, Reader in, Locale locale)
		throws BeanIOException;

	/**
	 * Creates a new <tt>BeanWriter</tt> for writing to the given file.
	 * @param name the stream configuration name to use for writing to the stream
	 * @param file the file to write to
	 * @return the new BeanWriter
	 * @throws BeanIOException if there is no stream configured for the given name
	 */
	public BeanWriter createWriter(String name, File file) throws BeanIOException {
		validateStreamName(name);
		
		Writer out = null;
		try {
			return createWriter(name, new BufferedWriter(new FileWriter(file)));
		}
		catch (IOException ex) {
			throw new BeanIOException(ex);
		}
		finally {
			IOUtil.closeQuietly(out);
		}
	}
	
	/**
	 * Creates a new <tt>BeanWriter</tt> for writing to the given output stream.
	 * @param name the stream configuration name to use for writing to the stream
	 * @param out the output stream to write to
	 * @return the new BeanWriter
	 * @throws BeanIOException if there is no stream configured for the given name
	 */
	public abstract BeanWriter createWriter(String name, Writer out)
		throws BeanIOException;
	
	/**
	 * Loads a BeanIO configuration file and adds the configured streams to this factory.
	 * If the given file name is not found in the file system, the file will be loaded
	 * from the classpath next.
	 * @param filename the name of the BeanIO configuration file to load
	 * @throws BeanIOException if an IOException or other fatal error is caught while
	 *   loading the file
	 * @throws BeanIOConfigurationException if the configuration is invalid
	 */
	public void load(String filename) throws BeanIOException, BeanIOConfigurationException {
		File file = new File(filename);
		if (file.exists()) {
			load(file);
			return;
		}
		InputStream in = null;
		try {
			in = getClass().getResourceAsStream(filename);
			if (in == null)
				throw new BeanIOException("BeanIO configureation not found '" + filename + "'");
				
			load(in);
		}
		catch (IOException ex) {
			throw new BeanIOException("IOException caught reading configuration file: " + file, ex);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) { }
		}
	}
	
	/**
	 * Loads a BeanIO configuration file and adds the configured streams to this factory.
	 * @param file the BeanIO configuration file to load
	 * @throws BeanIOException if an IOException or other fatal error is caught while
	 *   loading the file
	 * @throws BeanIOConfigurationException if the configuration is invalid
	 */
	public void load(File file) throws BeanIOException, BeanIOConfigurationException {
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
			load(in);
		}
		catch (IOException ex) {
			throw new BeanIOException("IOException caught reading configuration file '" + file + "'", ex);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) { }
		}
	}
	
	/**
	 * Loads a BeanIO configuration and adds the configured streams to this factory.
	 * @param in the input stream to read the configuration from
	 * @throws BeanIOException if an IOException or other fatal error is caught while
	 *   reading the input stream
	 * @throws BeanIOConfigurationException if the configuration is invalid
	 */
	public abstract void load(InputStream in) throws IOException, BeanIOConfigurationException;
	
	/**
	 * Returns a new <tt>StreamFactory</tt> instance.  An implementation class is loaded
	 * using the the BeanIO configuration setting <tt>org.beanio.streamFactory</tt>.
	 * @return a new <tt>StreamFactory</tt>
	 * @throws BeanIOException if a <tt>StreamFactory</tt> could not be created
	 * @see Settings
	 */
	public static StreamFactory newInstance() throws BeanIOException {
		
		String className = Settings.getInstance().getProperty(Settings.STREAM_FACTORY_CLASS);
		if (className == null) {
			throw new BeanIOException("Property '" + Settings.STREAM_FACTORY_CLASS + "' not set");
		}
		
		try {
			return (StreamFactory) Class.forName(className).newInstance();
		}
		catch (ClassNotFoundException e) {
			throw new BeanIOException("Failed to load stream factory implementation class '" + 
				className + "'", e);
		} 
		catch (InstantiationException e) {
			throw new BeanIOException("Failed to load stream factory implementation class '" + 
				className + "'", e);
		} 
		catch (IllegalAccessException e) {
			throw new BeanIOException("Failed to load stream factory implementation class '" + 
				className + "'", e);
		}
	}
	
	/**
	 * Validates a stream name.
	 * @param name the stream name to validate
	 * @throws BeanIOException if there is no stream configured with this name
	 */
	protected abstract void validateStreamName(String name) throws BeanIOException;
}