package parser;

import inter.Access;
import inter.And;
import inter.Arith;
import inter.Break;
import inter.Constant;
import inter.Do;
import inter.Else;
import inter.Expr;
import inter.For;
import inter.Id;
import inter.If;
import inter.Not;
import inter.Or;
import inter.Rel;
import inter.Seq;
import inter.Set;
import inter.SetElem;
import inter.Stmt;
import inter.Unary;
import inter.While;

import java.io.IOException;

import symbols.Array;
import symbols.Env;
import symbols.Type;
import lexer.Lexer;
import lexer.Tag;
import lexer.Token;
import lexer.Word;
import lexer.Num;

public class Parser {

	   private Lexer lex;    // lexical analyzer for this parser
	   private Token look;   // lookahead token
	   Env top = null;       // current or top symbol table
	   int used = 0;         // storage used for declarations

	   public Parser(Lexer l) throws IOException { lex = l; move(); }

	   void move() throws IOException { look = lex.scan(); }

	   void error(String s) { throw new Error("near line "+Lexer.line+": "+s); }

	   void match(int t) throws IOException { //match look.tag with input tag
	      if( look.tag == t ) move();
	      else error("syntax error");
	   }

	   public void program() throws IOException {  // program -> block
		  // build the syntax tree
	      Stmt s = block();
	      // display the syntax tree
	      // only display the stmts, without expr
	      s.display();
	   }

	   Stmt block() throws IOException {  // block -> { decls stmts }
	      match('{');  Env savedEnv = top;  top = new Env(top);
	      decls(); Stmt s = stmts();
	      match('}');  top = savedEnv;//���»����������������������top��λ��������ĩβ����
	      return s;
	   }

	   void decls() throws IOException {

	      while( look.tag == Tag.BASIC ) {   // D -> type ID ;
	         Type p = type(); Token tok = look; match(Tag.ID); match(';');
	         Id id = new Id((Word)tok, p, used);
	         top.put( tok, id );//�����������һ������������
	         used = used + p.width;
	      }
	   }

	   Type type() throws IOException {//����look�����ͣ��������ͻ�array���ͣ�����int[n]��

	      Type p = (Type)look;            // expect look.tag == Tag.BASIC 
	      match(Tag.BASIC);
	      if( look.tag != '[' ) return p; // T -> basic
	      else return dims(p);            // return array type
	   }

	   Type dims(Type p) throws IOException {//����array����
	      match('[');  Token tok = look;  match(Tag.NUM);  match(']');
	      if( look.tag == '[' ) {p = dims(p);}
	      return new Array(((Num)tok).value, p);
	   }

	   Stmt stmts() throws IOException {//ֱ����ȡ��{}Ϊ���һ��statements
	      if ( look.tag == '}' ) return Stmt.Null;
	      else return new Seq(stmt(), stmts());
	   }

	   Stmt stmt() throws IOException {//statementƥ�������֣���ֵ�䣻�����¿飻��⵽�ؼ��ֲ�ƥ��ؼ���block
	      Expr x,x1;  Stmt s, s1, s2;
	      Stmt savedStmt;         // save enclosing loop for breaks

	      switch( look.tag ) {

	      case ';':
	         move();
	         return Stmt.Null;

	      case Tag.IF:
	         match(Tag.IF); match('('); x = bool(); match(')');
	         s1 = stmt();
	         if( look.tag != Tag.ELSE ) return new If(x, s1);
	         match(Tag.ELSE); //��match��move()
	         s2 = stmt();
	         return new Else(x, s1, s2);

	      case Tag.WHILE:
	         While whilenode = new While();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = whilenode;//���뵱ǰwhileƥ����ҵ
	         match(Tag.WHILE); match('('); x = bool(); match(')');
	         s1 = stmt();
	         whilenode.init(x, s1);//���ɵ�while�ڱ�����whileӦ�е�����Ԫ�أ�������while�������ݣ�
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return whilenode;

	      case Tag.DO:
	         Do donode = new Do();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = donode;
	         match(Tag.DO);
	         s1 = stmt();
	         match(Tag.WHILE); match('('); x = bool(); match(')'); match(';');
	         donode.init(s1, x);
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return donode;
	         
	      case Tag.FOR:
	    	 For fornode=new For();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = fornode;
	         match(Tag.FOR); match('('); s1=assign2(); match(';'); x = bool(); match(';'); s2=assign2(); match(')');
	         //ÿ��assign2()�����⸳ֵ�䣩�ǲ���ƥ�䣻��assign()����ͨ��ֵ�䣩
	         //�˰汾ֻ֧��һ��for��ʽ��for(��ֵ�䣨�ұ���ֵ������forǰ������������ֹ������boolean������ֵ�䣩
	         s = stmts();
	         fornode.init(s1,x,s2,s);
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return fornode;
	         //���ԣ�{int a;int b;int c;while(true){if(true) for(a=b;a<b;a=a+1) a=b;}}

	      case Tag.BREAK://��⵽break�ؼ��֣������˿�
	         match(Tag.BREAK); match(';');
	         return new Break();

	      case '{':
	         return block();

	      default:
	         return assign();//Ĭ���϶���statementΪ��ֵ��
	      }
	   }
	   
	   Stmt assign() throws IOException {//ƥ��һ����ֵ��
	      Stmt stmt;  Token t = look;
	      match(Tag.ID);
	      Id id = top.get(t);
	      if( id == null ) error(t.toString() + " undeclared");//ʹ�ñ�������������

	      if( look.tag == '=' ) {       // S -> id = E ;
	         move();  stmt = new Set(id, bool());
	      }
	      else {                        // S -> L = E ;
	         Access x = offset(id);
	         match('=');  stmt = new SetElem(x, bool());
	      }
	      match(';');
	      return stmt;
	   }

	   Stmt assign2() throws IOException {//ƥ��һ������;��β�ĸ�ֵ��
	      Stmt stmt;  Token t = look;
	      match(Tag.ID);
	      Id id = top.get(t);
	      if( id == null ) error(t.toString() + " undeclared");

	      if( look.tag == '=' ) {       // S -> id = E ;
	         move();  stmt = new Set(id, bool());
	      }
	      else {                        // S -> L = E ;
	         Access x = offset(id);
	         match('=');  stmt = new SetElem(x, bool());
	      }
	      return stmt;
	   }


	   //�����±��ʽ��Խ�·����ȶ�Խ�ߡ�
	   Expr bool() throws IOException {//ƥ��һ��boolean���ʽ�����ȼ���==>&&>||
	      Expr x = join();
	      while( look.tag == Tag.OR ) {
	         Token tok = look;  move();  x = new Or(tok, x, join());
	      }
	      return x;
	   }

	   Expr join() throws IOException {
	      Expr x = equality();
	      while( look.tag == Tag.AND ) {
	         Token tok = look;  move();  x = new And(tok, x, equality());
	      }
	      return x;
	   }

	   Expr equality() throws IOException {
	      Expr x = rel();
	      while( look.tag == Tag.EQ || look.tag == Tag.NE ) {
	         Token tok = look;  move();  x = new Rel(tok, x, rel());
	      }
	      return x;
	   }

	   Expr rel() throws IOException {//ƥ��һ������ʽ��������������a>b��ʽ��ת��Ϊb<a
	      Expr x = expr();
	      switch( look.tag ) {
	      case '<': case Tag.LE: case Tag.GE: 
	      case '>':
	         Token tok = look;  move();  return new Rel(tok, x, expr());
	      default:
	         return x;
	      }
	   }

	   Expr expr() throws IOException {//ƥ���������ʽ�����ȼ�һԪ��������-������>�˳�>�Ӽ�
	      Expr x = term();
	      while( look.tag == '+' || look.tag == '-' ) {
	         Token tok = look;  move();  x = new Arith(tok, x, term());
	      }
	      return x;
	   }

	   Expr term() throws IOException {
	      Expr x = unary();
	      while(look.tag == '*' || look.tag == '/' ) {
	         Token tok = look;  move();   x = new Arith(tok, x, unary());
	      }
	      return x;
	   }

	   Expr unary() throws IOException {
	      if( look.tag == '-' ) {
	         move();  return new Unary(Word.minus, unary());
	      }
	      else if( look.tag == '!' ) {
	         Token tok = look;  move();  return new Not(tok, unary());
	      }
	      else return factor();
	   }

	   Expr factor() throws IOException {//ƥ��operand������Ϊ��һ���ã��������boolean���ʽ/����/������/��/��/�������ı���
	      Expr x = null;
	      switch( look.tag ) {
	      case '(':
	         move(); x = bool(); match(')');
	         return x;
	      case Tag.NUM:
	         x = new Constant(look, Type.Int);    move(); return x;
	      case Tag.REAL:
	         x = new Constant(look, Type.Float);  move(); return x;
	      case Tag.TRUE:
	         x = Constant.True;                   move(); return x;
	      case Tag.FALSE:
	         x = Constant.False;                  move(); return x;
	      case Tag.ID:
		         String s = look.toString();
		         Id id = top.get(look);
		         if( id == null ) error(look.toString() + " undeclared");
		         move();
		         if( look.tag != '[' ) return id;
		         else return offset(id);
	      default:
	         error("syntax error");
	         return x;
	      }
	   }

	   Access offset(Id a) throws IOException {   // I -> [E] | [E] I
	      Expr i; Expr w; Expr t1, t2; Expr loc;  // inherit id

	      Type type = a.type;
	      match('['); i = bool(); match(']');     // first index, I -> [ E ]
	      type = ((Array)type).of;
	      w = new Constant(type.width);
	      t1 = new Arith(new Token('*'), i, w);
	      loc = t1;
	      while( look.tag == '[' ) {      // multi-dimensional I -> [ E ] I
	         match('['); i = bool(); match(']');
	         type = ((Array)type).of;
	         w = new Constant(type.width);
	         t1 = new Arith(new Token('*'), i, w);
	         t2 = new Arith(new Token('+'), loc, t1);
	         loc = t2;
	      }

	      return new Access(a, loc, type);
	   }
	}
