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

/* Program */
source
  : prg EOF
  ;

prg
  : decl decl*
  ;

/* Declarations */
decl
  : decl_type
  | decl_var
  | decl_fun
  ;

id_exp
  : ~ID { if (true) throw new Report.Error(loc(getContext().start), getContext().getText() + " : Identifier expected"); }
  ;

decl_type
  : TYP ID ASSIGN type
  | TYP id_exp
  ;

decl_var
  : VAR ID COLON type
  | VAR id_exp
  ;

decl_fun
  : FUN ID LPAREN params? RPAREN COLON type returned?
  | FUN id_exp
  ;

params
  : ID COLON type ( COMMA ID COLON type )*
  | ID COLON type COMMA ( ID COLON type COMMA )* id_exp
  | id_exp
  ;

returned
  : ASSIGN expr
  ;

/* Types */
type
  : VOID
  | CHAR
  | INT
  | BOOL
  | ID
  | array_type
  | ptr_type
  | record_type
  | LPAREN type RPAREN
  ;

array_type
  : LSQUARE expr RSQUARE type 
  ;

ptr_type
  : CARET type
  ;

record_type
  : LCURLY ID COLON type ( COMMA ID COLON type )* RCURLY
  | LCURLY RCURLY { if (true) throw new Report.Error(loc(getContext().start), getContext().getText() + " : Empty record type not allowed"); }
  ;

/* Expressions */
expr
  : CONST_VOID
  | CONST_BOOL
  | CONST_INT
  | CONST_CHAR
  | CONST_STR
  | CONST_PTR
  | ID
  | fun_call
  | compound_expr
  | typecast_expr
  | LPAREN expr RPAREN
  | expr ( LSQUARE expr RSQUARE | CARET | DOT ( ID | id_exp ) )
  | ( BANG | PLUS | MINUS | CARET | NEW | DEL ) expr
  | expr ( MULT | DIV | MOD ) expr
  | expr ( PLUS | MINUS ) expr
  | expr ( EQ | NEQ | LT | GT | LE | GE ) expr
  | expr AMP expr
  | expr PIPE expr
  | expr WHERE LCURLY decl decl* RCURLY
  ;

fun_call
  : ID LPAREN args? RPAREN
  ;

args
  : expr ( COMMA expr )*
  ;

compound_expr
  : LCURLY stmt SEMI ( stmt SEMI )* RCURLY
  | LCURLY stmt ( SEMI stmt )* semi_miss 
  | LCURLY RCURLY { if (true) throw new Report.Error(loc(getContext().start), "Empty compound statement not allowed"); }
  ;

semi_miss
  : ~SEMI { if (true) throw new Report.Error(loc(getContext().start), getContext().getText() + " : Semi colon expected"); }
  ;

typecast_expr
  : LPAREN expr COLON type RPAREN
  ;

/* Statements */
stmt
  : expr_stmt
  | assign_stmt
  | if_stmt
  | while_stmt
  ;

expr_stmt
  : expr
  ;

assign_stmt
  : expr ASSIGN expr
  ;

if_stmt
  : IF expr ( THEN | then_exp ) stmt ( ELSE | else_exp ) stmt
  ;
  
then_exp
  : ~THEN { if (true) throw new Report.Error(loc(getContext().start), getContext().getText() + " : then expected"); }
  ;
 
else_exp
  : ~ELSE { if (true) throw new Report.Error(loc(getContext().start), getContext().getText() + " : else expected"); }
  ;

while_stmt
  : WHILE expr ( DO | do_exp ) stmt
  ;

do_exp
  : ~DO { if (true) throw new Report.Error(loc(getContext().start), getContext().getText() + " : do expected"); }
  ;
    
