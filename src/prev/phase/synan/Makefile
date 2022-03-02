JAVA		= java
ANTLR		= org.antlr.v4.Tool
ANTLRDIR	= ../../../../lib/antlr-4.9.3-complete.jar
RM		= 'rm' -fr

parser	: PrevParser.java PrevParser.interp PrevParser.tokens 
PrevParser.java PrevParser.interp PrevParser.tokens : PrevParser.g4 ../lexan/PrevLexer.java ../lexan/PrevLexer.interp ../lexan/PrevLexer.tokens
	$(JAVA) -cp $(ANTLRDIR) $(ANTLR) -no-listener -no-visitor -lib ../lexan $<

../lexan/PrevLexer.java ../lexan/PrevLexer.interp ../lexan/PrevLexer.tokens :
	$(MAKE) -C ../lexan

.PHONY	: clean
clean	:
	$(RM) *~
	$(RM) Prev*.java *interp *tokens
