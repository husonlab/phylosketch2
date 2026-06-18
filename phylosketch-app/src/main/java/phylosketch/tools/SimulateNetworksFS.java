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

import jloda.fx.util.ArgsOptions;
import jloda.graph.Edge;
import jloda.phylo.PhyloTree;
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static jloda.phylo.algorithms.RootedNetworkProperties.*;

/**
 * simulate networks for the Fuchs - Steel conjecture on random tree-child networks
 * Daniel Huson, 7.2025
 */
public class SimulateNetworksFS {
	public enum TreeModel {uniform, yule}

	public enum AttachEdges {tree, all}

	public enum OutputFormat {full, newick, stats}

	public static void main(String[] args) {
		try {
			PhyloTree.SUPPORT_RICH_NEWICK = true;
			ProgramProperties.setProgramName("SimulateNetworksFS");
			ProgramProperties.setProgramVersion(phylosketch.main.Version.SHORT_DESCRIPTION);

			PeakMemoryUsageMonitor.start();
			(new SimulateNetworksFS()).run(args);
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
		final ArgsOptions options = new ArgsOptions(args, this, "Simulate networks on trees");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("This is free software, licensed under the terms of the GNU General Public License, Version 3.");
		options.setAuthors("Daniel H. Huson");

		options.comment("Input and output");
		var outputFile = options.getOption("-o", "output", "Output file (stdout, *.gz ok)", "stdout");
		var outputFormat = OutputFormat.valueOf(options.getOption("-f", "format", "Output format", OutputFormat.values(), OutputFormat.stats.name()));
		options.comment("Options");
		var taxaSet = BitSetUtils.valueOf(options.getOptionMandatory("-t", "taxa", "Number of taxa in output network", ""), false);
		var reticulationsString = options.getOptionMandatory("-k", "reticulations", "Number of reticulations to add (range or pow(p,q)", "");
		var randomSeed = options.getOption("-s", "seed", "Random generator seed (0:use random seed)", 42);
		var replicates = options.getOption("-R", "replicates", "Number of replicates per n/k pair", 1);

		var treeModel = TreeModel.valueOf(options.getOption("-m", "treeModel", "Tree generation treeModel", TreeModel.values(), TreeModel.uniform.name()));
		var attachEdges = AttachEdges.valueOf(options.getOption("-a", "attach", "Reticulate attach to tree edges or all edges", AttachEdges.values(), AttachEdges.tree.name()));

		ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-th", "threads", "Set number of threads to use", 8));
		options.done();

		Function<Integer, Collection<Integer>> reticulationValues = taxa -> {
			if (reticulationsString.startsWith("pow")) {
				var pattern = Pattern.compile("^\\s*pow\\(\\s*(-?\\d+)\\s*/\\s*(-?\\d+)\\s*\\)\\s*$");
				var matcher = pattern.matcher(reticulationsString);
				if (!matcher.matches()) {
					throw new IllegalArgumentException("Invalid format: " + reticulationsString);
				}
				var p = Integer.parseInt(matcher.group(1));
				var q = Integer.parseInt(matcher.group(2));
				return List.of((int) Math.round(Math.pow(taxa, (double) p / (double) q)));
			} else
				try {
					return BitSetUtils.asList(BitSetUtils.valueOf(reticulationsString));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
		};

		var random = new Random(randomSeed);

		var jobs = new ArrayList<Job>();
		for (var taxa : BitSetUtils.members(taxaSet)) {
			for (var reticulations : reticulationValues.apply(taxa)) {
				for (var replicate = 1; replicate <= replicates; replicate++) {
					jobs.add(new Job(taxa, reticulations, replicate, random.nextLong(), treeModel, attachEdges));
				}
			}
		}
		if (options.isVerbose())
			System.err.println("Jobs: " + jobs.size());

		var results = new ConcurrentHashMap<Job, Result>();
		try (var progress = new ProgressPercentage("Simulating")) {
			ExecuteInParallel.apply(jobs, job -> {
				var tree = job.createNetwork();
				var isDag = (tree != null && isNonEmptyDAG(tree));
				var isTreeChild = isDag && isTreeChild(tree);
				var isNormal = isTreeChild && isNormal(tree);
				results.put(job, new Result(isDag, isTreeChild, isNormal, isDag ? tree.toBracketString(false) : ""));
			}, ProgramExecutorService.getNumberOfCoresToUse(), progress);
		}

		jobs.stream().filter(job -> results.get(job) == null).forEach(job -> System.err.println("Warning: " + job + ": has no result"));

		if (options.isVerbose() && !outputFile.equals("stdout"))
			System.err.println("Writing output to: " + outputFile);
		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile)) {
			if (outputFormat == OutputFormat.full) {
				for (var job : jobs) {
					w.write(job.parameterString() + "," + results.get(job) + "\n");
				}
			} else if (outputFormat == OutputFormat.newick) {
				for (var job : jobs) {
					var newick = results.get(job).newick();
					if (newick != null) {
						w.write(newick + "\n");
					}
				}
			}
			{
				var taxa = -1;
				var reticulations = -1;

				var countDag = 0;
				var countTreeChild = 0;
				var countNormal = 0;
				var reps = 0;

				for (var job : jobs) {
					if (job.taxa() != taxa || job.reticulations() != reticulations) {
						if (reps > 0) {
							w.write("ntaxa=%d,nRet=%d,dag=%s%%,tree-child=%s%%,normal=%s%%%n".formatted(taxa, reticulations,
									StringUtils.removeTrailingZerosAfterDot(countDag * 100.0 / reps),
									StringUtils.removeTrailingZerosAfterDot(countTreeChild * 100.0 / reps),
									StringUtils.removeTrailingZerosAfterDot(countNormal * 100.0 / reps)));
						}
						taxa = job.taxa();
						reticulations = job.reticulations();
						countDag = 0;
						countTreeChild = 0;
						countNormal = 0;
						reps = 0;
					}
					reps++;
					var result = results.get(job);
					if (result.isDag())
						countDag++;
					if (result.isTreeChild())
						countTreeChild++;
					if (result.isNormal())
						countNormal++;
				}
				if (reps > 0) {
					w.write("ntaxa=%d,nRet=%d,dag=%s%%,tree-child=%s%%,normal=%s%%%n".formatted(taxa, reticulations,
							StringUtils.removeTrailingZerosAfterDot(countDag * 100.0 / reps),
							StringUtils.removeTrailingZerosAfterDot(countTreeChild * 100.0 / reps),
							StringUtils.removeTrailingZerosAfterDot(countNormal * 100.0 / reps)));
				}
			}
		}
	}

	public record Job(int taxa, int reticulations, int replicate, long randomSeed, TreeModel treeModel,
					  AttachEdges attachEdges) {
		public String parameterString() {
			return "nTaxa=%d,nRet=%d,rep=%d".formatted(taxa, reticulations, replicate);
		}

		public PhyloTree createNetwork() {
			var tree = new PhyloTree();
			createRandomTree(tree, taxa, randomSeed, treeModel);
			if (addReticulations(tree, reticulations, randomSeed, attachEdges)) {
				return tree;
			} else return null;
		}
	}

	public record Result(boolean isDag, boolean isTreeChild, boolean isNormal, String newick) {
		@Override
		public String toString() {
			return "dag=%s,tree-child=%s,normal=%s,newick=%s".formatted(isDag, isTreeChild, isNormal, newick);
		}
	}

	public static void createRandomTree(PhyloTree tree, int nTaxa, long randomSeed, TreeModel treeModel) {
		var root = tree.newNode();
		tree.setRoot(root);
		var secondRoot = tree.newNode();
		var edges = new ArrayList<Edge>();
		edges.add(tree.newEdge(root, secondRoot));

		for (var t = 1; t <= Math.min(2, nTaxa); t++) {
			var v = tree.newNode();
			edges.add(tree.newEdge(secondRoot, v));
			tree.addTaxon(v, t);
			tree.setLabel(v, "t" + t);
		}

		var random = new Random(randomSeed);
		for (var t = 3; t <= nTaxa; t++) {
			var e = edges.get(random.nextInt(edges.size()));
			var src = e.getSource();
			var tar = e.getTarget();
			var isLeafEdge = tar.isLeaf();
			var v = tree.newNode();
			var sv = tree.newEdge(src, v);
			if (treeModel == TreeModel.uniform) {
				edges.add(sv);
			}
			var vt = tree.newEdge(v, tar);
			if (treeModel == TreeModel.uniform || isLeafEdge) {
				edges.add(vt);
			}
			var w = tree.newNode();
			edges.add(tree.newEdge(v, w));
			tree.addTaxon(w, t);
			tree.setLabel(w, "t" + t);

			tree.deleteEdge(e);
			edges.remove(e);
		}
	}

	private static boolean addReticulations(PhyloTree tree, int reticulations, long randomSeed, AttachEdges attachEdges) {
		var random = new Random(randomSeed);
		var edges = tree.getEdgesAsList();
		for (var k = 0; k < reticulations; k++) {
			var first = edges.get(random.nextInt(edges.size()));
			var second = edges.get(random.nextInt(edges.size()));
			while (second == first)
				second = edges.get(random.nextInt(edges.size()));
			var fSrc = first.getSource();
			var fTar = first.getTarget();
			var v = tree.newNode();
			edges.add(tree.newEdge(fSrc, v));
			edges.add(tree.newEdge(v, fTar));
			var sSrc = second.getSource();
			var sTar = second.getTarget();
			var w = tree.newNode();
			edges.add(tree.newEdge(sSrc, w));
			edges.add(tree.newEdge(w, sTar));

			var vw = tree.newEdge(v, w);
			if (attachEdges == AttachEdges.all) {
				edges.add(vw); // also allow reticulation edges as attachment sets
			}
			edges.remove(first);
			tree.deleteEdge(first);
			edges.remove(second);
			tree.deleteEdge(second);
		}
		for (var e : edges) {
			var target = e.getTarget();
			if (target.getInDegree() > 1) {
				for (var f : target.inEdges()) {
					tree.setReticulate(f, true);
				}
				tree.setTransferAcceptor(e, true);
			}
		}
		return true;
	}
}