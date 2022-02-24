JAVA		= java
ANTLR		= org.antlr.v4.Tool
ANTLRDIR	= ../../../../lib/antlr-4.9.3-complete.jar
RM		= 'rm' -fr

lexer	: PrevLexer.java PrevLexer.interp PrevLexer.tokens
PrevLexer.java PrevLexer.interp PrevLexer.tokens : PrevLexer.g4
	$(JAVA) -cp $(ANTLRDIR):$(CLASSPATH) $(ANTLR) $<

.PHONY	: clean
clean	:
	$(RM) *~
	$(RM) Prev*.java *interp *tokens
