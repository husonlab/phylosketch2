Capturing the two Felidae trees of Li et al. (2016) with PhyloSketch

This directory shows the preprocessing that allows semi-automatic capture of
published tree figures with PhyloSketch. The two trees (biparental nuclear
genome and mitogenome, Fig. 1A of the paper) were used in the phylogenetic
parallelograms paper (Huson, Cetinkaya and Zhang). The paper does not provide
the trees as files, only the underlying sequence and genotype data, which is
why they were captured from the figure.

Image source (not included here): Li G, Davis BW, Eizirik E, Murphy WJ.
Phylogenomic evidence for ancient hybridization in the genomes of living cats
(Felidae). Genome Res. 2016 Jan;26(1):1-11. doi: 10.1101/gr.186668.114.
PMID: 26518481; PMCID: PMC4691742. Figure 1A is vector graphics in the
journal PDF; render the page at 600 dpi (for example with Ghostscript,
gs -r600 -sDEVICE=png16m) before cleaning.

Files

  clean_nuclear.py
      Cleans the nuclear tree (left part of Fig. 1A) from the 600-dpi page
      render: crops the tree and its label column, turns the coloured edges
      black, joins the dashed edges into solid lines, bridges the edges
      through the numbered lineage boxes, and removes the map inset, title,
      grey time bands, dashed time lines, asterisks, support values, arrows
      and the symbols right of the labels. Output: cats-nuclear-tree.png.
  clean_tree.py
      Cleans the mitogenome tree (right part of Fig. 1A) from a screenshot
      of the figure: removes the title, legend, lineage boxes, asterisks,
      support values and the symbols left of the labels. With --flip it
      mirrors the tree so that the root is on the left and moves the labels
      to the right of the leaves. Outputs: cats-mitogenome-tree.png,
      cats-mitogenome-tree-root-left.png.
  cats-nuclear-tree.png, cats-mitogenome-tree.png, cats-mitogenome-tree-root-left.png
      The cleaned black-and-white images that were loaded into PhyloSketch.

Steps

  1. Render and clean the figure:
       python3 clean_nuclear.py page-600dpi.png cats-nuclear-tree.png
       python3 clean_tree.py mitogenome-screenshot.png cats-mitogenome-tree.png [--flip]
     The scripts need Python 3 with Pillow and numpy; clean_nuclear.py
     imports helper functions from clean_tree.py, so keep both files
     together. The crop coordinates and the positions of the title, legend
     and lineage boxes are specific to this figure and are set at the top
     of each script.
  2. In PhyloSketch, open a cleaned image with the capture function and
     accept or correct the detected nodes, edges and labels.
  3. Export the captured trees in Newick format. The trees used in the
     paper are in publication-data/figure5 of
     https://github.com/husonlab/phyloparallelograms.
