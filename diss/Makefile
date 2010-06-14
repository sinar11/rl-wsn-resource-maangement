# process LaTeX

# based on http://www.sigterm.de/misc/src/Makefile-latex

# Basename for result
TARGET=utaexample
SRC=*.tex *.bib *.eps
LATEX=latex
PDFLATEX=pdflatex


all: ${TARGET}.dvi

pdf: ${TARGET}.pdf

${TARGET}.dvi: ${SRC}
	${LATEX} ${TARGET}
#	makeindex ${TARGET}
	bibtex ${TARGET}
	${LATEX} ${TARGET}
	${LATEX} ${TARGET}

${TARGET}.pdf: ${SRC}
	#dvipdf ${TARGET}
	dvips -t letter -Ppdf -G0 ${TARGET}
	ps2pdf ${TARGET}.ps

objects := $(wildcard *.aux *.bbl *.blg *.out *.ilg *.ind *.log *.toc *.lot *.lof)


clean: $(objects)
	rm $(objects)
	

