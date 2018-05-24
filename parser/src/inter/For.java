package inter;

import symbols.Type;

public class For extends Stmt {

   Expr expr; Stmt stmt1,stmt2,stmt;

   public For() { expr = null; stmt = null; }

   public void init(Stmt s1,Expr x,Stmt s2,Stmt s) {
      expr = x;  stmt1 = s1;stmt2=s2;stmt=s;
      if( expr.type != Type.Bool ) expr.error("boolean required in for");
   }
   public void gen(int b, int a) {}
   
   public void display() {
	   emit("stmt : for begin");
	   stmt.display();
	   emit("stmt : for end");
   }
}
