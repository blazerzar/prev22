JAVA            = java --enable-preview
ANTLRDIR        = ../lib/antlr-4.9.3-complete.jar
XSLTPROC	= xsltproc
RM              = 'rm' -fr

PHASE		= all
LOG		= all

.PHONY	: %
%	: %.p22
	$(RM) -fr $@.*.xml $@.*.html
	if $(JAVA) -cp ../bin:../src:../lib/antlr-4.9.3-complete.jar prev.Compiler --xsl=../lib/xsl/ --logged-phase=$(LOG) --target-phase=$(PHASE) $< ; then for XML in `'ls' -1 $@.*.xml` ; do $(XSLTPROC) $$XML -o `basename $$XML xml`html; done ; fi

.PHONY  : clean
clean   :
	$(RM) *~
	$(RM) *xml *html

