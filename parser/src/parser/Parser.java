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
	      match('}');  top = savedEnv;//更新环境（给环境“链表”添加新top（位于链表最末尾））
	      return s;
	   }

	   void decls() throws IOException {

	      while( look.tag == Tag.BASIC ) {   // D -> type ID ;
	         Type p = type(); Token tok = look; match(Tag.ID); match(';');
	         Id id = new Id((Word)tok, p, used);
	         top.put( tok, id );//给环境中添加一个已声明变量
	         used = used + p.width;
	      }
	   }

	   Type type() throws IOException {//返回look的类型，基本类型或array类型（形如int[n]）

	      Type p = (Type)look;            // expect look.tag == Tag.BASIC 
	      match(Tag.BASIC);
	      if( look.tag != '[' ) return p; // T -> basic
	      else return dims(p);            // return array type
	   }

	   Type dims(Type p) throws IOException {//生成array类型
	      match('[');  Token tok = look;  match(Tag.NUM);  match(']');
	      if( look.tag == '[' ) {p = dims(p);}
	      return new Array(((Num)tok).value, p);
	   }

	   Stmt stmts() throws IOException {//直接提取以{}为界的一段statements
	      if ( look.tag == '}' ) return Stmt.Null;
	      else return new Seq(stmt(), stmts());
	   }

	   Stmt stmt() throws IOException {//statement匹配结果三种：赋值句；开启新块；检测到关键字并匹配关键字block
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
	         match(Tag.ELSE); //以match代move()
	         s2 = stmt();
	         return new Else(x, s1, s2);

	      case Tag.WHILE:
	         While whilenode = new While();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = whilenode;//进入当前while匹配作业
	         match(Tag.WHILE); match('('); x = bool(); match(')');
	         s1 = stmt();
	         whilenode.init(x, s1);//生成的while节保存了while应有的所有元素（条件和while块内内容）
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
	         //每个assign2()（特殊赋值句）是不需匹配；的assign()（普通赋值句）
	         //此版本只支持一种for格式：for(赋值句（且被赋值变量在for前已声明）；终止条件（boolean）；赋值句）
	         s = stmts();
	         fornode.init(s1,x,s2,s);
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return fornode;
	         //测试：{int a;int b;int c;while(true){if(true) for(a=b;a<b;a=a+1) a=b;}}

	      case Tag.BREAK://检测到break关键字，结束此块
	         match(Tag.BREAK); match(';');
	         return new Break();

	      case '{':
	         return block();

	      default:
	         return assign();//默认认定此statement为赋值句
	      }
	   }
	   
	   Stmt assign() throws IOException {//匹配一个赋值句
	      Stmt stmt;  Token t = look;
	      match(Tag.ID);
	      Id id = top.get(t);
	      if( id == null ) error(t.toString() + " undeclared");//使用变量必须已声明

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

	   Stmt assign2() throws IOException {//匹配一个不以;结尾的赋值句
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


	   //【以下表达式，越下方优先度越高】
	   Expr bool() throws IOException {//匹配一个boolean表达式，优先级：==>&&>||
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

	   Expr rel() throws IOException {//匹配一个不等式，并把所有形如a>b的式子转记为b<a
	      Expr x = expr();
	      switch( look.tag ) {
	      case '<': case Tag.LE: case Tag.GE: 
	      case '>':
	         Token tok = look;  move();  return new Rel(tok, x, expr());
	      default:
	         return x;
	      }
	   }

	   Expr expr() throws IOException {//匹配算术表达式，优先级一元操作符（-、！）>乘除>加减
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

	   Expr factor() throws IOException {//匹配operand，可能为：一个用（）括起的boolean表达式/整数/浮点数/真/假/已声明的变量
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
