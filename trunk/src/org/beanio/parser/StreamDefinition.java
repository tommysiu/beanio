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
package org.beanio.parser;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.beanio.*;
import org.beanio.stream.*;

/**
 * A <tt>StreamDefinition</tt> stores all information needed to parse an input 
 * stream and format an output stream, and is used to create <tt>BeanReader</tt>
 * and <tt>BeanWriter</tt> instances.
 * <p>
 * Internally, a stream definition is a tree structure with <tt>GroupDefinition</tt>
 * instances for branches and <tt>RecordDefinition</tt> instances for leaves.  
 * <p>
 * All classes and subclasses used to define a stream may be shared across multiple 
 * threads and must be thread-safe.
 * 
 * @author Kevin Seim
 * @since 1.0
 */
public abstract class StreamDefinition implements MessageFactory {

	/* resource bundle key prefixes */
	private static final String LABEL_MESSAGE_PREFIX = "label";
	private static final String FIELD_ERROR_MESSAGE_PREFIX = "fielderror";
	private static final String RECORD_ERROR_MESSAGE_PREFIX = "recorderror";
	
	/* configured resource bundle for messages */
	private ResourceBundle resourceBundle;
	/* default resource bundle for messages based on the stream format */
	private ResourceBundle defaultResourceBundle;
	/* cache messages from resource bundles */
	private ConcurrentHashMap<String,String> messageCache = new ConcurrentHashMap<String,String>();
	/* used to flag cache misses */
	private static final String NOT_FOUND = new String();
	
	private String name;
	private String format;
	private GroupDefinition root;
	
	private RecordReaderFactory readerFactory;
	private RecordWriterFactory writerFactory;
	
	/**
	 * Creates a new <code>StreamDefinition</code>.
	 */
	public StreamDefinition(String format) {
		this.format = format;
		
		root = new GroupDefinition();
		root.setOrder(1);
		root.setMinOccurs(0);
		root.setMaxOccurs(1);
		root.setName("<root>");
	}
	
	/**
	 * Returns the name of this stream context.
	 * @return the name of this stream context
	 */
	public String getName() {
		return name;
	}
	
	/**s
	 * Sets the name of this stream context.
	 * @param name the new name of this stream context
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the stream format, one of 'csv', 'delimited' or 'fixedlength'.
	 * @return the stream format
	 */
	public String getFormat() {
		return this.format;
	}
	
	/**
	 * Constructs a new <tt>Record</tt> for this stream to operate on.
	 * @param locale the locale to use for rendering error messages
	 * @return new <tt>Record</tt>
	 */
	public final Record createRecord(Locale locale) {
		Record record = createRecord();
		record.setLocale(locale);
		record.setMessageContext(this);
		return record;
	}
	
	/**
	 * Constructs a new <tt>Record</tt> for this stream to operate on.
	 * @return new <tt>Record</tt>
	 */
	protected Record createRecord() {
		return new Record();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.beanio.parser.MessageContext#getRecordLabel(java.lang.String)
	 */
	public String getRecordLabel(String recordName) {
		return getLabel(LABEL_MESSAGE_PREFIX + "." + recordName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.beanio.parser.MessageContext#getFieldLabel(java.lang.String, java.lang.String)
	 */
	public String getFieldLabel(String recordName, String fieldName) {
		return getLabel(LABEL_MESSAGE_PREFIX + "." + recordName + "." + fieldName);
	}
	
	/**
	 * Returns a label from the configured resource bundle.
	 * @param key the resource bundle key
	 * @return the label, or null if not found
	 */
	protected String getLabel(String key) {
		String label = messageCache.get(key);
		if (label != null) {
			return label == NOT_FOUND ? null : label;
		}
		 
		if (resourceBundle != null) {
			label = getMessage(resourceBundle, key);
		}
		
		if (label == null) {
			messageCache.putIfAbsent(key, NOT_FOUND);
			return null;
		}
		
		messageCache.putIfAbsent(key, label);
		return label;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.bio.context.MessageContext#getFieldErrorMessage(java.lang.String, java.lang.String, java.lang.String)
	 */
	public String getFieldErrorMessage(String recordName, String fieldName, String rule) {
		String key = FIELD_ERROR_MESSAGE_PREFIX + "." + recordName + "." + fieldName + "." + rule;
		
		String message = messageCache.get(key);
		if (message != null) {
			return message == NOT_FOUND ? key : message;
		}
		
		String k = key;
		if (resourceBundle != null) {
			message = getMessage(resourceBundle, k);
			if (message == null) {
				k = FIELD_ERROR_MESSAGE_PREFIX + "." + recordName + "." + rule;
				message = getMessage(resourceBundle, k);
				if (message == null) {
					k = FIELD_ERROR_MESSAGE_PREFIX + "." + rule;
					message = getMessage(resourceBundle, k);
				}
			}
		}
		
		if (message == null && defaultResourceBundle != null) {
			message = getMessage(defaultResourceBundle, FIELD_ERROR_MESSAGE_PREFIX + "." + rule);
		}
		
		if (message == null) {
			messageCache.putIfAbsent(key, NOT_FOUND);
			return key;
		}
		else {
			messageCache.putIfAbsent(key, message);
			return message;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.bio.context.MessageContext#getRecordErrorMessage(java.lang.String, java.lang.String)
	 */
	public String getRecordErrorMessage(String recordName, String rule) {
		String key = RECORD_ERROR_MESSAGE_PREFIX + "." + recordName + "." + rule;
		
		String message = messageCache.get(key);
		if (message != null) {
			return message == NOT_FOUND ? key : message;
		}
		
		if (resourceBundle != null) {
			message = getMessage(resourceBundle, key);
			if (message == null) {
				message = getMessage(resourceBundle, RECORD_ERROR_MESSAGE_PREFIX + "." + rule);
			}
		}
		
		if (message == null && defaultResourceBundle != null) {
			message = getMessage(defaultResourceBundle, RECORD_ERROR_MESSAGE_PREFIX + "." + rule);
		}
		
		if (message == null) {
			messageCache.putIfAbsent(key, NOT_FOUND);
			return key;
		}
		else {
			messageCache.putIfAbsent(key, message);
			return message;
		}
	}
	
	/**
	 * Returns a message from a resource bundle.
	 * @param bundle the resource bundle to check
	 * @param key the resource bundle key for the message
	 * @return the message or <tt>null</tt> if not found
	 */
	private String getMessage(ResourceBundle bundle, String key) {
		try {
			return bundle.getString(key);
		}
		catch (MissingResourceException ex) {
			return null;
		}
	}
	
	/**
	 * Creates a new <tt>RecordReader</tt> to read from the given input stream.
	 * This method delegates to the configured reader factory, or if null, 
	 * it calls {@link #createDefaultReader(Reader)}.
	 * @param in the input stream to read from
	 * @return a new <tt>RecordReader</tt>
	 */
	public RecordReader createRecordReader(Reader in) {
		if (readerFactory == null)
			return createDefaultReader(in);
		else
			return readerFactory.createReader(in);
	}
	
	/**
	 * Creates a default <tt>RecordReader</tt> to read from the given input stream.
	 * This method is called by {@link #createRecordReader(Reader)} if no record
	 * reader factory was configured.
	 * @param in the input stream to read from
	 * @return a new <tt>RecordReader</tt>
	 */
	protected abstract RecordReader createDefaultReader(Reader in);
	
	/**
	 * Creates a new <tt>RecordWriter</tt> for writing to the given output stream.
	 * This method delegates to the configured record writer factory, or if null, 
	 * it calls {@link #createDefaultWriter(Writer)}.
	 * @param out the output stream to write to
	 * @return a new <tt>RecordWriter</tt>
	 */
	public RecordWriter createRecordWriter(Writer out) {
		if (writerFactory == null)
			return createDefaultWriter(out);
		else
			return writerFactory.createWriter(out);
	}
	
	/**
	 * Creates a default <tt>RecordWriter</tt> for writing to the given output stream.
	 * This method is called by {@link #createRecordWriter(Writer)} if no record
	 * writer factory was configured.
	 * @param out the output stream to write to
	 * @return a new <tt>RecordWriter</tt>
	 */
	protected abstract RecordWriter createDefaultWriter(Writer out);

	/**
	 * Sets the record reader factory to use to create new <tt>RecordReader</tt>'s.
	 * If set to null, this stream context will create a default reader.
	 * @param readerFactory the new record reader factory
	 */
	public void setReaderFactory(RecordReaderFactory readerFactory) {
		this.readerFactory = readerFactory;
	}
	
	/**
	 * Sets the record writer factory to use to create new <tt>RecordWriter</tt>'s.
	 * If set to null, this stream context will create a default writer.
	 * @param writerFactory the new record writer factor
	 */
	public void setWriterFactory(RecordWriterFactory writerFactory) {
		this.writerFactory = writerFactory;
	}
	
	/**
	 * Sets the primary resource bundle to check for messages.
	 * @param resourceBundle the resource bundle
	 */
	public void setResourceBundle(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
	}
	
	/**
	 * Sets the default resource bundle to check of messages not found in the
	 * primary resource bundle.
	 * @param defaultResourceBundle the default resource bundle
	 */
	public void setDefaultResourceBundle(ResourceBundle defaultResourceBundle) {
		this.defaultResourceBundle = defaultResourceBundle;
	}

	/**
	 * Returns the top level or root group context for this stream, to which all
	 * child records and subgroups were added.
	 * @return the root group context
	 */
	public final GroupDefinition getRootGroupContext() {
		return root;
	}

	/**
	 * Sets the minimum number of times the record layout must be read from
	 * in the input stream.  Defaults to 0.
	 * @param minOccurs the minimum number of times the record layout
	 *   must be read from the input stream
	 */
	public void setMinOccurs(int minOccurs) {
		this.root.setMinOccurs(minOccurs);
	}
	
	/**
	 * Sets the maximum number of times the record layout can be read from
	 * the input stream.  Defaults to 1.
	 * @param maxOccurs the maximum number of times the record layout can be
	 *   read from the input straem
	 */
	public void setMaxOccurs(int maxOccurs) {
		this.root.setMaxOccurs(maxOccurs);
	}
	
	/**
	 * Creates a new <tt>BeanReader</tt> for reading from the given input stream.
	 * @param in the input stream to read from
	 * @param locale the locale to use for rendering error messages
	 * @return a new <tt>BeanReader</tt>.
	 */
	public BeanReader createBeanReader(Reader in, Locale locale) {
		if (in == null) {
			throw new NullPointerException();
		}
		RecordReader reader = createRecordReader(in);
		Record record = createRecord(locale);
		return new BeanReaderImpl(reader, record);
	}
	
	/**
	 * Creates a new <tt>BeanWriter</tt> for writing to the given output stream.
	 * @param out the output stream to write to
	 * @return a new <tt>BeanWriter</tt>
	 */
	public BeanWriter createBeanWriter(Writer out) {
		if (out == null) {
			throw new NullPointerException();
		}
		RecordWriter writer = createRecordWriter(out);
		return new BeanWriterImpl(writer);
	}
	
	/*
	 * BeanReader implementation.
	 */
	private class BeanReaderImpl implements BeanReader {
		private Node layout;
		private Record record;
		private RecordReader in;
		
		/**
		 * Constructs a new <tt>BeanReaderImpl</tt>.
		 * @param reader the record reader to read from
		 * @param record the record implementation to operate on
		 */
		public BeanReaderImpl(RecordReader reader, Record record) {
			this.in = reader;
			this.record = record;
			this.layout = buildTree(StreamDefinition.this);
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.beanio.BeanReader#read()
		 */
		public Object read() {
			if (layout == null)
				return null;
			
			Object bean = null;
			while (bean == null) {
				
				RecordNode node = null;
				while (node == null) {
					// clear the last record
					record.clear();
					
					// read the next record
					Object  recordValue;
					try {
						recordValue = in.read();
						record.setRecordText(in.getRecordText());
						record.setLineNumber(in.getRecordLineNumber());
					}
					catch (RecordIOException e) {
						record.setLineNumber(in.getRecordLineNumber());
						record.setRecordText(in.getRecordText());
						record.addRecordError("malformed", in.getRecordLineNumber(), in.getRecordText());
						throw new MalformedRecordException(record.getContext(), 
							"Malformed record at line " + in.getRecordLineNumber());
					}
					catch (IOException e) {
						throw new BeanReaderIOException(record.getContext(),
							"IOException caught reading from input stream", e);
					}
					
					// check for end of file
					if (recordValue == null) {
						try {
							// calling close will determine if all min occurs have been met
							Node unsatisfied = layout.close();
							if (unsatisfied != null) {
								record.addRecordError("sequence", record.getRecordLineNumber(), record.getRecordText());
								throw new UnexpectedRecordException(record.getContext(),
									"End of stream reached at line " + record.getRecordLineNumber() + ", expected record '" + unsatisfied.getName() + "'");
							}
							return null;
						}
						finally {
							layout = null;
						}
					}
					
					// set the value of the record (which is implementation specific) on the record
					record.setValue(recordValue);
					
					try {
						node = (RecordNode) layout.matchNext(record);
					}
					catch (UnexpectedRecordException ex) {
						if (layout.matchAny(record) == null) {
							record.addRecordError("unidentified", record.getRecordLineNumber(), record.getRecordText());
							throw new UnidentifiedRecordException(record.getContext(),
								"Unidentified record type at line " + record.getRecordLineNumber());							
						}
						else {
							record.addRecordError("unexpected", record.getRecordLineNumber(), record.getRecordText());
							throw ex;
						}
					}
					if (node == null) {
						// determine if the record matches any record type in order to throw
						// the correct exception type
						node = (RecordNode) layout.matchAny(record);
						if (node != null) {
							record.addRecordError("unexpected", record.getRecordLineNumber(), record.getRecordText());
							throw new UnexpectedRecordException(record.getContext(),
								"Unexpeced record type '" + node.getName() + "' found at line " + record.getRecordLineNumber());
						}
						else {
							record.addRecordError("unidentified", record.getRecordLineNumber(), record.getRecordText());
							throw new UnidentifiedRecordException(record.getContext(),
								"Unidentified record type at line " + record.getRecordLineNumber());
						}
					}
				}
				
				RecordDefinition recordContext = node.getRecordContext();
				record.setRecordName(recordContext.getName());
				bean = recordContext.parseBean(record);
			}
			return bean;
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.beanio.impl.AbstractBeanReader#close()
		 */
		public void close() {
			record.clear();
			try {
				in.close();
			}
			catch (IOException ex) {
				throw new BeanReaderIOException(record.getContext(),
					"IOException caught closing input stream", ex);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.beanio.BeanReader#getName()
		 */
		public String getName() {
			return record.getRecordName();
		}

		/*
		 * (non-Javadoc)
		 * @see org.beanio.BeanReader#getLineNumber()
		 */
		public int getLineNumber() {
			return record.getRecordLineNumber();
		}
		
		private GroupNode buildTree(StreamDefinition context) {
			GroupNode root = new GroupNode(context.getRootGroupContext());
			buildTree(context.getRootGroupContext(), root);
			return root;
		}
		
		private void buildTree(NodeDefinition context, GroupNode group) {
			for (NodeDefinition childCtx : context.getChildren()) {
				if (childCtx.isRecordDefinition()) {
					RecordNode child = new RecordNode((RecordDefinition)childCtx);
					group.addChild(child);
				}
				else {
					GroupNode child = new GroupNode((GroupDefinition)childCtx);
					group.addChild(child);
					buildTree(childCtx, child);
				}
			}
		}
	}
	
	/*
	 * BeanWriter implementation.
	 */
	private class BeanWriterImpl extends AbstractBeanWriter {

		/**
		 * Constructs a new <tt>BeanWriterImpl</tt>.
		 * @param out the output stream to write to
		 */
		public BeanWriterImpl(RecordWriter out) {
			super(out);
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.beanio.BeanWriter#write(java.lang.Object)
		 */
		public void write(Object bean) {
			// search for a matching record context
			RecordDefinition recordContext = (RecordDefinition) getRootGroupContext().findDefinitionFor(bean);
			if (recordContext == null) {
				throw new BeanWriterIOException("No record mapping found for class '" + bean.getClass() + "'");
			}
			
			try {
				out.write(recordContext.formatBean(bean));
			} catch (IOException e) {
				throw new BeanWriterIOException("IOException caught writing to output stream", e);
			}
		}
	}
}