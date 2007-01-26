/*
 * 本代码所有权归作者所有 但在保持源代码不被破坏以及所有人署名的基础上 任何人可自由无限使用
 */
package com.sohospace.lucene.analysis.xanalyzer;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import com.sohospace.lucene.analysis.xanalyzer.collector.QueryTokenCollector;
import com.sohospace.lucene.analysis.xanalyzer.collector.WriterTokenCollector;
import com.sohospace.paoding.Beef;
import com.sohospace.paoding.Collector;
import com.sohospace.paoding.Knife;
import com.sohospace.paoding.Paoding;

/**
 * XTokenizer是基于“庖丁解牛”框架的TokenStream实现，为XAnalyzer使用。
 * <p>
 * 
 * @author zhiliang.wang@yahoo.com.cn
 * 
 * @see Beef
 * @see Knife
 * @see Paoding
 * @see Tokenizer
 * @see XAnalyzer
 * 
 * @see Collector
 * @see TokenCollector
 * @see QueryTokenCollector
 * @see WriterTokenCollector
 * 
 * @since 1.0
 */
public final class XTokenizer extends TokenStream implements Collector {

	// -------------------------------------------------

	/**
	 * 文本字符源
	 * 
	 * @see #next()
	 */
	private final Reader input;
	
	/**
	 * 
	 */
	private static final int bufferLength = 128;

	/**
	 * 接收来自{@link #input}的文本字符
	 * 
	 * @see #next()
	 */
	private final char[] buffer = new char[bufferLength];

	/**
	 * {@link buffer}[0]在{@link #input}中的偏移
	 * 
	 * @see #collect(String, int, int)
	 * @see #next()
	 */
	private int offset;
	
	/**
	 * 
	 */
	private final Beef beef = new Beef(buffer, 0, 0);
	
	/**
	 * 
	 */
	private int dissected;

	/**
	 * 用于分解beef中的文本字符，由XAnalyzer提供
	 * 
	 * @see #next()
	 */
	private Knife knife;

	/**
	 * 
	 */
	private TokenCollector tokenCollector;

	/**
	 * tokens迭代器，用于next()方法顺序读取tokens中的Token对象
	 * 
	 * @see #tokens
	 * @see #next()
	 */
	private Iterator<Token> tokenIteractor;

	// -------------------------------------------------

	/**
	 * 
	 * @param input
	 * @param knife
	 * @param tokenCollector
	 */
	public XTokenizer(Reader input, Knife knife, TokenCollector tokenCollector) {
		this.input = input;
		this.knife = knife;
		this.tokenCollector = tokenCollector;
	}

	// -------------------------------------------------

	public TokenCollector getTokenCollector() {
		return tokenCollector;
	}

	public void setTokenCollector(TokenCollector tokenCollector) {
		this.tokenCollector = tokenCollector;
	}

	// -------------------------------------------------

	
	public void collect(String word, int offset, int end) {
		tokenCollector.collect(word, this.offset + offset, this.offset + end);
	}

	// -------------------------------------------------
	@Override
	public Token next() throws IOException {
		// 已经穷尽tokensIteractor的Token对象，则继续请求reader流入数据
		while (tokenIteractor == null || !tokenIteractor.hasNext()) {
			int read = 0;
			int remainning = -1;//重新读入字符时，buffer中还剩下的字符数，-1表示当前暂不需要从reader中读入字符
			if (dissected >= beef.length()) {
				remainning = 0;
			}
			else if (dissected < 0){
				remainning = bufferLength + dissected;
			}
			if (remainning != -1) {
				if (remainning > 0) {
					System.arraycopy(buffer, -dissected, buffer, 0, remainning);
				}
				read = input.read(buffer, remainning, bufferLength - remainning);
				int charCount = remainning + read;
				if (charCount < 0) {
					// reader已尽，按接口next()要求返回null.
					return null;
				}
				if (charCount < bufferLength) {
					buffer[charCount ++] = 0;
				}
				// 构造“牛”，并使用knife“解”之
				beef.set(0, charCount);
				offset -= remainning;
				dissected = 0;
			}
			dissected = knife.dissect((Collector)this, beef, dissected);
			offset += read;// !!!
			tokenIteractor = tokenCollector.iterator();
		}
		// 返回tokensIteractor下一个Token对象
		return tokenIteractor.next();
	}

	// -------------------------------------------------

	@Override
	public void close() throws IOException {
		super.close();
		input.close();
	}

}
