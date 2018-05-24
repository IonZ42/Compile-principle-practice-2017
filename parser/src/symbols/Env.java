package symbols;

import inter.Id;

import java.util.Hashtable;

import lexer.Token;

public class Env {
	private Hashtable<Token, Id> table;
	protected Env prev;//向前链表，head元素的prev是null
	
	public Env(Env n) { table = new Hashtable<Token, Id>(); prev=n;}//向后添加新node
	
	public void put(Token w, Id i) {
		table.put(w, i);
	}
	
	public Id get(Token w){//向前遍历所有env的所有table，找与w match的ID
		for(Env e=this; e!=null; e=e.prev){
			Id found = (Id)(e.table.get(w));
			if(found!=null) return found;
		}
		return null;
	}
}
