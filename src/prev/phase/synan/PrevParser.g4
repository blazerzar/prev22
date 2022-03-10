parser grammar PrevParser;

@header {

	package prev.phase.synan;
	
	import java.util.*;
	
	import prev.common.report.*;
	import prev.data.ast.tree.*;
	import prev.data.ast.tree.decl.*;
	import prev.data.ast.tree.expr.*;
	import prev.data.ast.tree.stmt.*;
	import prev.data.ast.tree.type.*;
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
  returns [AstTrees<AstDecl> ast]
  : prg { $ast = $prg.ast; } 
    EOF
  ;

prg
  returns [AstTrees<AstDecl> ast]
  : { List<AstDecl> decls = new LinkedList<>(); }
    ( decl { decls.add($decl.ast); } )+
    { $ast = new AstTrees<>(decls); }
  ;

/* Declarations */
decl
  returns [AstDecl ast]
  : decl_type { $ast = $decl_type.ast; }
  | decl_var { $ast = $decl_var.ast; }
  | decl_fun { $ast = $decl_fun.ast; }
  ;

id_exp
  : ~ID
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : Identifier expected");
    }
  ;

decl_type
  returns [AstTypeDecl ast]
  : TYP ID ASSIGN type
    { $ast = new AstTypeDecl(loc($TYP, $type.ast), $ID.text, $type.ast); }
  | TYP id_exp
  ;

decl_var
  returns [AstVarDecl ast]
  : VAR ID COLON type
    { $ast = new AstVarDecl(loc($VAR, $type.ast), $ID.text, $type.ast); }
  | VAR id_exp
  ;

decl_fun
  returns [AstFunDecl ast]
  : {
        AstTrees<AstParDecl> pars = null;
        AstExpr ret = null;
    }
    FUN ID LPAREN
    ( params { pars = $params.ast; } )? RPAREN COLON type
    ( returned { ret = $returned.ast; } )?
    { $ast = new AstFunDecl(loc($FUN, ret != null ? ret : $type.ast), $ID.text, pars, $type.ast, ret); }
  | FUN id_exp
  ;

params
  returns [AstTrees<AstParDecl> ast]
  : { List<AstParDecl> pars = new LinkedList<>(); }
    id1=ID COLON t1=type
    { pars.add(new AstParDecl(loc($id1, $t1.ast), $id1.text, $t1.ast)); }
    ( COMMA id2=ID COLON t2=type
      { pars.add(new AstParDecl(loc($id2, $t2.ast), $id2.text, $t2.ast)); }
    )*
    { $ast = new AstTrees<>(pars); }
  | ID COLON type COMMA ( ID COLON type COMMA )* id_exp
  | id_exp
  ;

returned
  returns [AstExpr ast]
  : ASSIGN expr { $ast = $expr.ast; }
  ;

/* Types */
type
  returns [AstType ast]
  : VOID { $ast = new AstAtomType(loc($VOID), AstAtomType.Type.VOID); } 
  | CHAR { $ast = new AstAtomType(loc($CHAR), AstAtomType.Type.CHAR); }
  | INT { $ast = new AstAtomType(loc($INT), AstAtomType.Type.INT); }
  | BOOL { $ast = new AstAtomType(loc($BOOL), AstAtomType.Type.BOOL); }
  | ID { $ast = new AstNameType(loc($ID), $ID.text); }
  | array_type { $ast = $array_type.ast; }
  | ptr_type { $ast = $ptr_type.ast; }
  | record_type { $ast = $record_type.ast; }
  | LPAREN type RPAREN
    {
        $type.ast.relocate(loc($LPAREN, $RPAREN));
        $ast = $type.ast;
    }
  ;

array_type
  returns [AstArrType ast]
  : LSQUARE expr RSQUARE type
    { $ast = new AstArrType(loc($LSQUARE, $type.ast), $type.ast, $expr.ast); } 
  ;

ptr_type
  returns [AstPtrType ast]
  : CARET type
    { $ast = new AstPtrType(loc($CARET, $type.ast), $type.ast); }
  ;

record_type
  returns [AstRecType ast]
  : { List<AstCompDecl> comps = new LinkedList<>(); } 
    LCURLY id1=ID COLON t1=type
    { comps.add(new AstCompDecl(loc($id1, $t1.ast), $id1.text, $t1.ast)); }
    ( COMMA id2=ID COLON t2=type
      { comps.add(new AstCompDecl(loc($id2, $t2.ast), $id2.text, $t2.ast)); }
    )*
    RCURLY
    { $ast = new AstRecType(loc($LCURLY, $RCURLY), new AstTrees<>(comps)); }
  | LCURLY RCURLY
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : Empty record type not allowed");
    }
  ;

/* Expressions */
expr
  returns [AstExpr ast]
  : CONST_VOID { $ast = new AstAtomExpr(loc($CONST_VOID), AstAtomExpr.Type.VOID, $CONST_VOID.text); }
  | CONST_BOOL { $ast = new AstAtomExpr(loc($CONST_BOOL), AstAtomExpr.Type.BOOL, $CONST_BOOL.text); }
  | CONST_INT { $ast = new AstAtomExpr(loc($CONST_INT), AstAtomExpr.Type.INT, $CONST_INT.text); }
  | CONST_CHAR { $ast = new AstAtomExpr(loc($CONST_CHAR), AstAtomExpr.Type.CHAR, $CONST_CHAR.text); }
  | CONST_STR { $ast = new AstAtomExpr(loc($CONST_STR), AstAtomExpr.Type.STRING, $CONST_STR.text); }
  | CONST_PTR { $ast = new AstAtomExpr(loc($CONST_PTR), AstAtomExpr.Type.POINTER, $CONST_PTR.text); }
  | ID { $ast = new AstNameExpr(loc($ID), $ID.text); }
  | fun_call { $ast = $fun_call.ast; }
  | compound_expr { $ast = $compound_expr.ast; }
  | typecast_expr { $ast = $typecast_expr.ast; }
  | LPAREN expr RPAREN
    {
        $expr.ast.relocate(loc($LPAREN, $RPAREN));
        $ast = $expr.ast;
    }
  | e1=expr
    ( 
        LSQUARE e2=expr RSQUARE
        { $ast = new AstArrExpr(loc($e1.ast, $RSQUARE), $e1.ast, $e2.ast); }
      | CARET
        { $ast = new AstSfxExpr(loc($e1.ast, $CARET), AstSfxExpr.Oper.PTR, $e1.ast ); }
      | DOT ( ID | id_exp )
        { $ast = new AstRecExpr(loc($e1.ast, $ID), $e1.ast, new AstNameExpr(loc($ID), $ID.text)); }
    )
  | { AstPfxExpr.Oper op; Token start; }
    (
        BANG  { op = AstPfxExpr.Oper.NOT; start = $BANG;  }
      | PLUS  { op = AstPfxExpr.Oper.ADD; start = $PLUS;  }
      | MINUS { op = AstPfxExpr.Oper.SUB; start = $MINUS; }
      | CARET { op = AstPfxExpr.Oper.PTR; start = $CARET; }
      | NEW   { op = AstPfxExpr.Oper.NEW; start = $NEW;   }
      | DEL   { op = AstPfxExpr.Oper.DEL; start = $DEL;   }
    )
    expr
    { $ast = new AstPfxExpr(loc(start, $expr.ast), op, $expr.ast); }
  | fst=expr 
    { AstBinExpr.Oper op; }
    (
        MULT { op = AstBinExpr.Oper.MUL; }
      | DIV  { op = AstBinExpr.Oper.DIV; }
      | MOD  { op = AstBinExpr.Oper.MOD; }
    )
    snd=expr
    { $ast = new AstBinExpr(loc($fst.ast, $snd.ast), op, $fst.ast, $snd.ast ); }
  | fst=expr
    { AstBinExpr.Oper op; }
    (
        PLUS  { op = AstBinExpr.Oper.ADD; }
      | MINUS { op = AstBinExpr.Oper.SUB; }
    )
    snd=expr
    { $ast = new AstBinExpr(loc($fst.ast, $snd.ast), op, $fst.ast, $snd.ast ); }
  | fst=expr
    { AstBinExpr.Oper op; }
    (
        EQ  { op = AstBinExpr.Oper.EQU; }
      | NEQ { op = AstBinExpr.Oper.NEQ; }
      | LT  { op = AstBinExpr.Oper.LTH; }
      | GT  { op = AstBinExpr.Oper.GTH; }
      | LE  { op = AstBinExpr.Oper.LEQ; }
      | GE  { op = AstBinExpr.Oper.GEQ; }
    )
    snd=expr
    { $ast = new AstBinExpr(loc($fst.ast, $snd.ast), op, $fst.ast, $snd.ast ); }
  | fst=expr AMP snd=expr
    { $ast = new AstBinExpr(loc($fst.ast, $snd.ast), AstBinExpr.Oper.AND, $fst.ast, $snd.ast ); }
  | fst=expr PIPE snd=expr
    { $ast = new AstBinExpr(loc($fst.ast, $snd.ast), AstBinExpr.Oper.OR, $fst.ast, $snd.ast ); }
  | e=expr WHERE LCURLY prg RCURLY
    { $ast = new AstWhereExpr(loc($e.ast, $RCURLY), $e.ast, $prg.ast); }
  ;

fun_call
  returns [AstNameExpr ast]
  : { AstTrees<AstExpr> arguments = null; }
    ID LPAREN
    ( args { arguments = $args.ast; } )? RPAREN
    { $ast = arguments != null
        ? new AstCallExpr(loc($ID, $RPAREN), $ID.text, arguments)
        : new AstNameExpr(loc($ID, $RPAREN), $ID.text); }
  ;

args
  returns [AstTrees<AstExpr> ast]
  : { List<AstExpr> arguments = new LinkedList<>(); }
    e1=expr { arguments.add($e1.ast); }
    ( COMMA e2=expr { arguments.add($e2.ast); } )*
    { $ast = new AstTrees<>(arguments); }
  ;

compound_expr
  returns [AstStmtExpr ast]
  : { List<AstStmt> stmts = new LinkedList<>(); }
    LCURLY s1=stmt { stmts.add($s1.ast); }
    SEMI ( s2=stmt SEMI { stmts.add($s2.ast); } )* RCURLY
    { $ast = new AstStmtExpr(loc($LCURLY, $RCURLY), new AstTrees<>(stmts)); }
  | LCURLY stmt ( SEMI stmt )* semi_miss 
  | LCURLY RCURLY
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : Empty compound statement not allowed");
    }
  ;

semi_miss
  : ~SEMI
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : Semi colon expected");
    }
  ;

typecast_expr
  returns [AstCastExpr ast]
  : LPAREN expr COLON type RPAREN
    { $ast = new AstCastExpr(loc($LPAREN, $RPAREN), $expr.ast, $type.ast); }
  ;

/* Statements */
stmt
  returns [AstStmt ast]
  : expr_stmt { $ast = $expr_stmt.ast; }
  | assign_stmt { $ast = $assign_stmt.ast; }
  | if_stmt { $ast = $if_stmt.ast; }
  | while_stmt { $ast = $while_stmt.ast; }
  ;

expr_stmt
  returns [AstExprStmt ast]
  : expr { $ast = new AstExprStmt($expr.ast.location(), $expr.ast); }
  ;

assign_stmt
  returns [AstAssignStmt ast]
  : dst=expr ASSIGN src=expr
    { $ast = new AstAssignStmt(loc($dst.ast, $src.ast), $dst.ast, $src.ast); }
  ;

if_stmt
  returns [AstIfStmt ast]
  : IF expr ( THEN | then_exp ) t=stmt ( ELSE | else_exp ) e=stmt
    { $ast = new AstIfStmt(loc($IF, $e.ast), $expr.ast, $t.ast, $e.ast); }
  ;
  
then_exp
  : ~THEN
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : then expected");
    }
  ;
 
else_exp
  : ~ELSE
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : else expected");
    }
  ;

while_stmt
  returns [AstWhileStmt ast]
  : WHILE expr ( DO | do_exp ) stmt
    { $ast = new AstWhileStmt(loc($WHILE, $stmt.ast), $expr.ast, $stmt.ast); }
  ;

do_exp
  : ~DO
    {
        if (true)
            throw new Report.Error(
                loc(getContext().start),
                getContext().getText() + " : do expected");
    }
  ;
    
