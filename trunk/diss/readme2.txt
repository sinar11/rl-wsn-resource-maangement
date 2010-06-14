Below are some common formatting problems with solutions.
Submitted by Darin Brezeale, Fri Jan 11 15:36:58 CST 2008

Formatting problems

1.  You are submitting electronically, so you should not include a signature page.
2.  Titles in the front matter and text longer than one line are appearing double-spaced in the front matter, but they should be single spaced.
3.  The bottom margin of all pages is not between 1.25 and 1.5 inches.  The left and right margins are not between 1.25 and 1.5 inches.


Solutions to formatting problems

The LaTeX template consists of a set of .tex files for the various sample chapters as well a style file (utathesis.sty) that sets format values.  There is also a file called utaexample.tex that pulls the individual chapter files together to form the entire dissertation.

1.  Simply remove the reference to the signature page in the file utaexample.tex and move the reference to the title page so that it occurs before the copyright page.

2.  There is a document at the grad school website that explains how to make long captions for figures and tables appear as single-spaced entries in the front matter.  It also mentions how to deal with long section titles.  This document is located at 
http://grad.uta.edu/pdfs/current/ThesisAndDissertation/Manual%20Of%20Style.pdf

3.  When using LaTeX on a Linux operating system, one way to generate PDF documents is to use
	dvipdf utaexample.tex
The problem with this is that dvipdf defaults to a paper size of A4, which is longer and narrower than 8.5" by 11".  This affects the left, right, and bottom margins.  To produce a document with the correct dimensions, in Linux first generate the DVI file.  Convert this file to postscript, then convert the postscript file to PDF using the following two commands:

        dvips -t letter -Ppdf -G0 utaexample.dvi
        ps2pdf utaexample.ps

    If you are using LaTeX on Windows, you may (depending on how your 
    environment is configured) be able to open a command window and
    type 

       dvips -t letter -Ppdf -G0 utaexample.dvi
       ps2pdf utaexample.ps

    to generate a PDF with the correct dimensions.


