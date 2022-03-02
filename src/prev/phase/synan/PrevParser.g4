parser grammar PrevParser;

@header {

	package prev.phase.synan;
	
	import java.util.*;
	
	import prev.common.report.*;
	//import prev.data.ast.tree.*;
	//import prev.data.ast.tree.decl.*;
	//import prev.data.ast.tree.expr.*;
	//import prev.data.ast.tree.stmt.*;
	//import prev.data.ast.tree.type.*;
	import prev.phase.lexan.*;
	
}

@members {

	private Location loc(Token     tok) { return new Location((prev.data.sym.Token)tok); }
	private Location loc(Locatable loc) { return new Location(loc                 ); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((prev.data.sym.Token)tok1, (prev.data.sym.Token)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((prev.data.sym.Token)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1,                      (prev.data.sym.Token)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1,                      loc2); }

}

options{
    tokenVocab=PrevLexer;
}


source
  :
  ;

