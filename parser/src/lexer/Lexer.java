package lexer;

import java.io.IOException;
import java.util.Hashtable;

import symbols.Type;

public class Lexer {
	
	public static int line = 1;//��¼�к�
	char peek = ' ';//��¼��һ��������char����������
	Hashtable<String, Word> words = new Hashtable<String, Word>();//k-v��

	void reserve(Word w) {
		words.put(w.lexeme, w);
	}

	public Lexer() {//��ʼ���ؼ����б�
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
		peek = (char) System.in.read();//peek������һchar
		
	}

	boolean readch(char c) throws IOException {
		readch();
		if (peek != c) {
			return false;//peek���ÿգ������μ�֮���tokenƥ��
		}
		peek = ' ';//��peek�ÿգ�������Ҳ�����õ�peek
		return true;//peek����char�����c��ͬ
	}

	public Token scan() throws IOException {
		for (;; readch()) {
			 if (peek == ' ' || peek == '\t')
				continue;
			else if (peek == '\n') {
				line += 1;
			} else {break;}//��ʱpeek��ȡ��һ��������char
		}
		if(peek == '@')//���Ƿ�����@����
		{
			   System.out.println("@is invalid");
			   Token tok = new Token(peek);
			   peek = ' ';
			   return tok;
		}
		switch (peek) {
		case '&':
			if (readch('&'))//ʵ�ֹؼ���tokenƥ��
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
		if (Character.isDigit(peek)) {//˵��peek�����ַ���һ�����ֵĿ�ͷ
			int v = 0;
			do {
				v = 10 * v + Character.digit(peek, 10);//��һ��charת10������
				readch();
			} while (Character.isDigit(peek));//��һλ��������������˴���
			if (peek != '.')
				return new Num(v);//˵��������
			float x = v;
			float d = 10;
			for (;;) {//����ѭ��ֱ����һλ��������
				readch();
				if (!Character.isDigit(peek))
					break;
				x = x + Character.digit(peek, 10) / d;
				d = d * 10;//��С�����һ��charת10����С������
			}
			return new Real(x);//tokenΪReal���ͣ�������NUM����
		}
		if (Character.isLetter(peek)) {
			StringBuffer b = new StringBuffer();//�ɱ䳤
			do {//ֱ��peek���Ĳ�����ĸǰ��peek�����ַ�����ӵ�token��
				b.append(peek);
				readch();
			} while (Character.isLetterOrDigit(peek));
			String s = b.toString();
			Word w = (Word) words.get(s);
			if (w != null)
				return w;//����һ��Word���͵�token
			w = new Word(s, Tag.ID);
			words.put(s, w);
			return w;
		}
		Token tok = new Token(peek);
		peek = ' ';
		return tok;//ö�ٹؼ���ƥ�䲻��������һ��SYM���͵�token��peek�ÿ�
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
