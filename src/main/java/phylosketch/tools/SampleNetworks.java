/*
 *  SampleTreesFromNetwork.java Copyright (C) 2024 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.tools;

import jloda.fx.phylo.embed.Averaging;
import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
import jloda.fx.util.ArgsOptions;
import jloda.graph.Node;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * samples rooted networks from a given input tree
 * Daniel Huson, 7.2025
 */
public class SampleNetworks {

	public static void main(String[] args) {
		try {
			PhyloTree.SUPPORT_RICH_NEWICK = true;
			ProgramProperties.setProgramName("SampleNetworks");
			ProgramProperties.setProgramVersion(phylosketch.main.Version.SHORT_DESCRIPTION);

			PeakMemoryUsageMonitor.start();
			(new SampleNetworks()).run(args);
			PeakMemoryUsageMonitor.report();
			System.exit(0);
		} catch (Exception ex) {
			Basic.caught(ex);
			System.exit(1);
		}
	}

	/**
	 * run
	 */
	private void run(String[] args) throws Exception {
		final ArgsOptions options = new ArgsOptions(args, this, "Samples rooted networks from a tree");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("This is free software, licensed under the terms of the GNU General Public License, Version 3.");
		options.setAuthors("Daniel H. Huson");

		options.comment("Input and output");
		var inputFile = options.getOptionMandatory("-i", "input", "Input tree file (stdin, *.gz ok)", "");
		var outputFile = options.getOption("-o", "output", "Output file (stdout, *.gz ok)", "stdout");

		options.comment("Options");
		var numberOfTaxaRangeString = options.getOption("-t", "taxa", "Number of taxa in output network", "");
		var requestedReticulateEdges = options.getOption("-a", "add", "Number of reticulate edges to add (value < 1: will use value*number-of-taxa)", 0.1);
		var randomSeed = options.getOption("-s", "seed", "Random generator seed (0:use random seed)", 42);
		var echoInputTree = options.getOption("-e", "echoInput", "Echo the input tree to output", false);

		var timeLayout = options.getOption("-x", "xtraTimeLayout", "Time the optimized layout algorithm", false);

		var replicates = options.getOption("-R", "replicates", "Number replicates per choice of taxon number", 1);

		ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-th", "threads", "Set number of threads to use", 8));
		options.done();

		FileUtils.checkFileReadableNonEmpty(inputFile);
		FileUtils.checkAllFilesDifferent(inputFile, outputFile);

		if (requestedReticulateEdges < 0)
			throw new UsageException("--add must be non-negative, got: " + requestedReticulateEdges);
		if (requestedReticulateEdges > 1)
			requestedReticulateEdges = (int) Math.round(requestedReticulateEdges);

		var random = new Random();

		if (randomSeed != 0) {
			random.setSeed(randomSeed);
		}

		try (var r = new BufferedReader(new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(inputFile)));
			 var w = new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile))) {
			var countInputTrees = 0;
			var countOutputTrees = 0;

			var taxIdMap = new HashMap<String, Integer>();

			while (r.ready()) {
				var line = r.readLine();
				if (line.startsWith("(")) {
					var inputTree0 = NewickIO.valueOf(line);
					addAdhocTaxonIds(inputTree0, taxIdMap);
					var inputTaxa0 = BitSetUtils.asBitSet(inputTree0.getTaxa());

					int[] numberOfTaxaRange;
					if (numberOfTaxaRangeString.isBlank())
						numberOfTaxaRange = new int[]{inputTaxa0.cardinality()};
					else
						numberOfTaxaRange = BitSetUtils.asList(BitSetUtils.valueOf(numberOfTaxaRangeString)).stream().mapToInt(t -> t).toArray();

					for (var numberOfTaxa : numberOfTaxaRange) {
						BitSet inputTaxa;
						PhyloTree inputTree;
						if (numberOfTaxa > 0 && numberOfTaxa < inputTaxa0.cardinality()) {
							var array = new int[BitSetUtils.max(inputTaxa0) + 1];
							var count = 0;
							inputTaxa = new BitSet();
							for (var t : BitSetUtils.members(inputTaxa0)) {
								count++;
								array[t] = count;
								inputTaxa.set(count);
								if (count == numberOfTaxa)
									break;
							}
							inputTree = computeInducedTree(array, inputTree0);
							assert inputTree != null;
							assert BitSetUtils.asBitSet(inputTree.getTaxa()).equals(BitSetUtils.asBitSet(BitSetUtils.range(1, numberOfTaxa + 1)));
						} else {
							inputTaxa = inputTaxa0;
							inputTree = new PhyloTree(inputTree0);
						}

						for (var v : inputTree.nodes()) {
							if (v.getInDegree() == 1 && v.getOutDegree() == 1)
								inputTree.delDivertex(v);
						}
						countInputTrees++;
						if (echoInputTree)
							w.write(inputTree.toBracketString(false) + "[&&NHX:GN=in%d];%n".formatted(countInputTrees));

						var reticulateEdges = Math.round(requestedReticulateEdges >= 1 ? requestedReticulateEdges : requestedReticulateEdges * inputTaxa.cardinality());

						for (var replicate = 0; replicate < replicates; replicate++) {
							var network = new PhyloTree(inputTree);
							var internalNodes = new ArrayList<>(network.nodeStream().filter(v -> v.getInDegree() > 0 && v.getOutDegree() > 0).toList());

							for (var add = 0; add < reticulateEdges; add++) {
								var a = internalNodes.get(random.nextInt(internalNodes.size()));
								var b = internalNodes.get(random.nextInt(internalNodes.size()));
								var count = 0;
								while (ancestors(b).contains(a) || a.isChild(b)) {
									a = internalNodes.get(random.nextInt(internalNodes.size()));
									b = internalNodes.get(random.nextInt(internalNodes.size()));
									if (++count == 1000)
										throw new RuntimeException("Can't add edge");
								}
								network.newEdge(b, a);
								for (var f : a.inEdges()) {
									network.setReticulate(f, true);
								}
							}
							if (true) {
								for (var v : network.nodes()) {
									var list = IteratorUtils.asList(v.adjacentEdges());
									Collections.shuffle(list, new Random(666));
									network.rearrangeAdjacentEdges(v, list);
								}
							}

							var label = "n%d.%d".formatted(countInputTrees, replicate + 1);
							if (options.isVerbose() && !timeLayout) {
								System.out.printf("%s: taxa=%d, h=%d%n", label, IteratorUtils.count(network.leaves()),
										network.nodeStream().filter(v -> v.getInDegree() > 1).mapToInt(v -> v.getInDegree() - 1).sum());
							}

							if (timeLayout) {
								var start = System.currentTimeMillis();

								LayoutRootedPhylogeny.apply(network, LayoutRootedPhylogeny.Layout.Rectangular, LayoutRootedPhylogeny.Scaling.EarlyBranching, Averaging.LeafAverage, true, random, new HashMap<>(), new HashMap<>());
								if (true)
									System.out.printf("%s: taxa=%d, h=%d, time=%ds%n", label, IteratorUtils.count(network.leaves()),
											network.nodeStream().filter(v -> v.getInDegree() > 1).mapToInt(v -> v.getInDegree() - 1).sum(),
											(System.currentTimeMillis() - start) / 1000);

								w.write(network.toBracketString(false) + "[&&NHX:GN=%s];%n".formatted(label));
								w.flush();
							}
						}
					}
				}
			}
			if (options.isVerbose()) {
				System.err.printf("Input trees:    %,10d%n", countInputTrees);
				System.err.printf("Output networks:%,10d%n", countOutputTrees);
			}
		}
	}


	public Set<Node> ancestors(Node b) {
		var set = new HashSet<Node>();
		var stack = new Stack<Node>();
		stack.push(b);
		while (!stack.isEmpty()) {
			b = stack.pop();
			if (!set.contains(b)) {
				set.add(b);
				for (var e : b.inEdges()) {
					stack.push(e.getSource());
				}
			}
		}
		return set;
	}

	/**
	 * computes the induced tree
	 */
	public static PhyloTree computeInducedTree(int[] oldTaxonId2NewTaxonId, PhyloTree originalTree) {
		final var inducedTree = new PhyloTree(originalTree);
		inducedTree.getLSAChildrenMap().clear();

		final var toRemove = new LinkedList<Node>(); // initially, set to all leaves that have lost their taxa

		// change taxa:
		for (var v : inducedTree.nodes()) {
			if (inducedTree.getNumberOfTaxa(v) > 0) {
				var taxa = new BitSet();
				for (var t : inducedTree.getTaxa(v)) {
					if (oldTaxonId2NewTaxonId[t] > 0)
						taxa.set(oldTaxonId2NewTaxonId[t]);
				}
				inducedTree.clearTaxa(v);
				if (taxa.cardinality() == 0)
					toRemove.add(v);
				else {
					for (var t : BitSetUtils.members(taxa)) {
						inducedTree.addTaxon(v, t);
					}
				}
			}
		}

		// delete all nodes that don't belong to the induced tree
		while (!toRemove.isEmpty()) {
			final var v = toRemove.remove(0);
			for (var e : v.inEdges()) {
				final var w = e.getSource();
				if (w.getOutDegree() == 1 && inducedTree.getNumberOfTaxa(w) == 0) {
					toRemove.add(w);
				}
			}
			if (inducedTree.getRoot() == v) {
				inducedTree.deleteNode(v);
				inducedTree.setRoot(null);
				return null; // tree has completely disappeared...
			}
			inducedTree.deleteNode(v);
		}

		// remove path from original root to new root:

		var root = inducedTree.getRoot();
		while (inducedTree.getNumberOfTaxa(root) == 0 && root.getOutDegree() == 1) {
			root = root.getFirstOutEdge().getTarget();
			inducedTree.deleteNode(inducedTree.getRoot());
			inducedTree.setRoot(root);
		}

		// remove all divertices
		final var diVertices = new LinkedList<Node>();
		for (var v : inducedTree.nodes()) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 1)
				diVertices.add(v);
		}
		for (var v : diVertices) {
			inducedTree.delDivertex(v);
		}

		return inducedTree;
	}

	public static void addAdhocTaxonIds(PhyloTree tree, Map<String, Integer> taxaIdMap) {
		tree.nodeStream().filter(v -> tree.getLabel(v) != null).forEach(v -> {
			var taxId = taxaIdMap.getOrDefault(tree.getLabel(v), taxaIdMap.size() + 1);
			tree.addTaxon(v, taxId);
			taxaIdMap.put(tree.getLabel(v), taxId);
		});
	}
}