package lexer;

import java.io.IOException;
import java.util.Hashtable;

import symbols.Type;

public class Lexer {
	
	public static int line = 1;//记录行号
	char peek = ' ';//记录下一个读到的char，包括换行
	Hashtable<String, Word> words = new Hashtable<String, Word>();//k-v对

	void reserve(Word w) {
		words.put(w.lexeme, w);
	}

	public Lexer() {//初始化关键字列表
		reserve(new Word("if", Tag.IF));
		reserve(new Word("else", Tag.ELSE));
		reserve(new Word("while", Tag.WHILE));
		reserve(new Word("do", Tag.DO));
		reserve(new Word("break", Tag.BREAK));
		reserve(Word.True); reserve(Word.False);
		reserve(Type.Int); reserve(Type.Char);
		reserve(Type.Bool); reserve(Type.Float);
		reserve(new Word("void", Tag.VOID)) ;
//		reserve(new Word("int", Tag.INT));
		reserve(new Word("double", Tag.DOUBLE));
//		reserve(new Word("bool", Tag.BOOL));
		reserve(new Word("string", Tag.STRING));
		reserve(new Word("class", Tag.CLASS));
		reserve(new Word("null", Tag.NULL));
		reserve(new Word("this", Tag.THIS));
		reserve(new Word("extends", Tag.EXTENDS));
		reserve(new Word("for", Tag.FOR));
		reserve(new Word("return", Tag.RETURN));
		reserve(new Word("new", Tag.NEW));
		reserve(new Word("NewArray", Tag.NEWARRAY));
		reserve(new Word("print", Tag.PRINT));
		reserve(new Word("ReadInteger", Tag.READINTEGER));
		reserve(new Word("ReaderLine", Tag.READERLINE));
		reserve(new Word("static", Tag.STATIC));
	}

	public void readch() throws IOException {
		peek = (char) System.in.read();//peek载入下一char
		
	}

	boolean readch(char c) throws IOException {
		readch();
		if (peek != c) {
			return false;//peek不置空，继续参加之后的token匹配
		}
		peek = ' ';//将peek置空，接下来也不再用到peek
		return true;//peek到的char与参数c相同
	}

	public Token scan() throws IOException {
		for (;; readch()) {
			 if (peek == ' ' || peek == '\t')
				continue;
			else if (peek == '\n') {
				line += 1;
			} else {break;}//此时peek获取第一个有意义char
		}
		if(peek == '@')//检测非法输入@符号
		{
			   System.out.println("@is invalid");
			   Token tok = new Token(peek);
			   peek = ' ';
			   return tok;
		}
		switch (peek) {
		case '&':
			if (readch('&'))//实现关键字token匹配
				return Word.and;
			else
				return new Token('&');
		case '|':
			if (readch('|'))
				return Word.or;
			else
				return new Token('|');
		case '=':
			if (readch('='))
				return Word.eq;
			else
				return new Token('=');
		case '!':
			if (readch('='))
				return Word.ne;
			else
				return new Token('!');
		case '<':
			if (readch('='))
				return Word.le;
			else
				return new Token('<');
		case '>':
			if (readch('='))
				return Word.ge;
			else
				return new Token('>');
		}
		if (Character.isDigit(peek)) {//说明peek到的字符是一串数字的开头
			int v = 0;
			do {
				v = 10 * v + Character.digit(peek, 10);//由一串char转10进制数
				readch();
			} while (Character.isDigit(peek));//下一位还是数字则继续此处理
			if (peek != '.')
				return new Num(v);//说明是整数
			float x = v;
			float d = 10;
			for (;;) {//不断循环直到下一位不是数字
				readch();
				if (!Character.isDigit(peek))
					break;
				x = x + Character.digit(peek, 10) / d;
				d = d * 10;//由小数点后一串char转10进制小数部分
			}
			return new Real(x);//token为Real类型，不属于NUM类型
		}
		if (Character.isLetter(peek)) {
			StringBuffer b = new StringBuffer();//可变长
			do {//直到peek到的不是字母前，peek到的字符都添加到token中
				b.append(peek);
				readch();
			} while (Character.isLetterOrDigit(peek));
			String s = b.toString();
			Word w = (Word) words.get(s);
			if (w != null)
				return w;//返回一个Word类型的token
			w = new Word(s, Tag.ID);
			words.put(s, w);
			return w;
		}
		Token tok = new Token(peek);
		peek = ' ';
		return tok;//枚举关键字匹配不到，返回一个SYM类型的token，peek置空
	}
	
	public void out() {
		System.out.println(words.size());
		
	}

	public char getPeek() {
		return peek;
	}

	public void setPeek(char peek) {
		this.peek = peek;
	}

}
