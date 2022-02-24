JAVA		= java
JAVAC		= javac
ANTLR           = org.antlr.v4.Tool
ANTLRDIR	= lib/antlr-4.9.3-complete.jar
RM		= 'rm' -fr
FIND		= 'find'

all	:
	if [ -d src/prev/phase/lexan ] ; then $(MAKE) -C src/prev/phase/lexan ; fi
	if [ -d src/prev/phase/synan ] ; then $(MAKE) -C src/prev/phase/synan ; fi
	$(JAVAC) -encoding us-ascii -cp $(ANTLRDIR):src src/prev/Compiler.java	
	@echo ":-) OK"

.PHONY	: clean
clean	:
	if [ -d doc ] ; then $(MAKE) -C doc clean ; fi
	if [ -d src ] ; then $(MAKE) -C prg clean ; fi
	if [ -d src/prev/phase/lexan ] ; then $(MAKE) -C src/prev/phase/lexan clean ; fi
	if [ -d src/prev/phase/synan ] ; then $(MAKE) -C src/prev/phase/synan clean ; fi
	$(FIND) . -type f -iname "*~" -exec $(RM) {} \;
	$(FIND) . -type f -iname "*.class" -exec $(RM) {} \;
	$(RM) bin
