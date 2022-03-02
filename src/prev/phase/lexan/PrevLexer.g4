lexer grammar PrevLexer;

@header {
	package prev.phase.lexan;
	import prev.common.report.*;
	import prev.data.sym.*;
}

@members {
    @Override
	public Token nextToken() {
		return (Token) super.nextToken();
	}
}

/* Constants */
CONST_VOID : 'none' ;
CONST_BOOL : 'true' | 'false' ;

CONST_INT : [1-9][0-9]* | '0' ;
CONST_INT_ERR : '0'[0-9]+ {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Integer constants with leading zeros not allowed"
    );
} ;

CONST_CHAR : '\'' CHAR_CHAR '\'' ;
CONST_CHAR_UNCLOSED : '\'' UNESCAPED_CHAR+  ~['\n\r] {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Unclosed character constant"
    );
} ;
CONST_CHAR_ERR : '\'' UNESCAPED_CHAR UNESCAPED_CHAR+ '\'' {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Invalid character constant"
    );
} ;
CONST_CHAR_EMPTY : '\'\'' {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Empty character constant"
    );
} ;

CONST_STR : '"' STR_CHAR* '"' ;
CONST_STR_UNCLOSED : '"' STR_CHAR* {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Unclosed string constant"
    );
} ; 

CONST_PTR : 'nil' ;

/* Symbols */
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
LSQUARE : '[' ;
RSQUARE : ']' ;
DOT : '.' ;
COMMA : ',' ;
COLON : ':' ;
SEMI : ';' ;
AMP : '&' ;
PIPE : '|' ;
BANG : '!' ;
EQ : '==' ;
NEQ : '!=' ;
LT : '<' ;
GT : '>' ;
LE : '<=' ;
GE : '>=' ;
MULT : '*' ;
DIV : '/' ;
MOD : '%' ;
PLUS : '+' ;
MINUS : '-' ;
CARET : '^' ;
ASSIGN : '=' ;

/* Keywords */
BOOL : 'bool' ;
CHAR : 'char' ;
DEL : 'del' ;
DO : 'do' ;
ELSE : 'else' ;
FUN : 'fun' ;
IF : 'if' ;
INT : 'int' ;
NEW : 'new' ;
THEN : 'then' ;
TYP : 'typ' ;
VAR : 'var' ;
VOID : 'void' ;
WHERE : 'where' ;
WHILE : 'while' ;

/* Identifiers */
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
ID_ERR : [0-9][a-zA-Z_0-9]* {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Identifiers cannot start with a number"
    );
} ;

/* Comments and white space */
COMMENT : '#' ~[\r\n]* ;
WS : [ \t\n\r]+ -> skip ;

UNRECOGNIZED : . {
    String text = getText();
    int line = getLine();
    int col = getCharPositionInLine() - text.length() + 1;
    Location l = new Location(line, col);
    if (true) throw new Report.Error(
        l, text + " : Unexpected symbol"
    );
} ;

fragment CHAR_CHAR : UNESCAPED_CHAR | '\\\'' ; 
fragment UNESCAPED_CHAR : [\u0020-\u0026\u0028-\u007e] ; 
fragment STR_CHAR : [\u0020-\u0021\u0023-\u007e] | '\\"' ;

