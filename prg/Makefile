JAVA            = java --enable-preview
ANTLRDIR        = ../lib/antlr-4.9.3-complete.jar
XSLTPROC	= xsltproc
RM              = 'rm' -fr

PHASE		= all
LOG		= all
NREGS = 8

FILE = test

.PHONY	: %
%	: %.p22
	$(RM) -fr $@.*.xml $@.*.html
	if $(JAVA) -cp ../bin:../src:../lib/antlr-4.9.3-complete.jar prev.Compiler --xsl=../lib/xsl/ --logged-phase=$(LOG) --target-phase=$(PHASE) --nregs=$(NREGS) $< ; then for XML in `'ls' -1 $@.*.xml` ; do $(XSLTPROC) $$XML -o `basename $$XML xml`html; done ; fi

.PHONY	: %
run : $(FILE).p22
	$(RM) *~
	$(RM) *xml *html *mms *mmo
	$(JAVA) -cp ../bin:../src:../lib/antlr-4.9.3-complete.jar prev.Compiler --xsl=../lib/xsl/ --logged-phase=$(LOG) --target-phase=$(PHASE) --nregs=$(NREGS) $(FILE).p22 && ./mmixal -x -b256 $(FILE).mms && ./mmix $(FILE).mmo || make clean

.PHONY  : clean
clean   :
	$(RM) *~
	$(RM) *xml *html *mms *mmo

