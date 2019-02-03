# ExCoDE


## Overview

ExCoDE is a general framework to mine diverse dense correlated subgraphs from dynamic networks. The correlation of a subgraph is computed in terms of the minimum pairwise Pearson correlation between its edges. The density of a subgraph is computed either as the minimum average degree among the snapshots of the networks, or as the average average degree among the snapshots of the networks. The similarity between different subgraphs is measured as the Jaccard similarity between the corresponding sets of edges.

The framework includes both an exact and an approximate mining algorithm. 

## Content
	run.sh ...........
	config.cfg ...............
	ExCoDE-release/ ...................

## Requirements
	Java JRE v1.8.0

## Input Format

### Graph File
This file contains the information about the edges in the network, and must comply with the following format:

	<node_id1> <node_id2> <edge_label>(optional) <time_series>
	<node_id1> <node_id2> <edge_label>(optional) <time_series>
	...

In particular, it must list one edge per line, indicating its source node, destination node, label (optional), and a comma separated list of numbers representing either the edge existence or the edge weights over time. 

The datasets used in our experiments are available on Google Drive at 
[this](https://drive.google.com/open?id=1HeueR-JOImhC2TGZ5wwKWvvjk5fS9N-X) link.

## Usage
You can use ExCoDE by running the script *run.sh* included in this package.

### Using the Script

1. **Input**: the name of the graph file must end with the extension *.csv*.

2. **Settings**: the value of each parameter used by ExCoDE must be set in the configuration file *config.cfg*. In particular:
 * General settings:
    * dataFolder: path to the folder that contains the graph file.
    * outputFolder: path to the folder where the results will be saved.
    * maxCCSize: max size of the candidate subgraphs that will be processed by the algorithm.
    * maxJac: max Jaccard similarity allowed between the subgraphs in the result set.
    * hashFuncs: number of hash functions used by the approximate algorithm to compute the pairs of correlated edges.
    * hashRuns: number of runs of the min-hashing procedure.
    * isExact: boolean value. If true, the script will run the approximate algorithm.

 * Dataset-related settings:
    * Dataset names: file names of the datasets to test.
    * Default values: comma-separated list of default values and information about the graph, i.e., default correlation threshold, default density threshold, default edges-per-snapshot threshold, is-weighted, and is-labeled.
    * Densities: comma-separated list of density thresholds to test.
    * Correlations: comma-separated list of correlation thresholds to test.
    * Experimental flags: test to perform among (1) test many density thresholds, (2) test many correlation thresholds.
    
3. **Declaration of the datasets**: the arrays that store the names, the densities, the correlations, and the experimental flags of each dataset to test must be declared at the beginning of the script *run.sh*. For example in the script *run.sh* you can write:
        
        declare -A datasets
        datasets[$twiter_db]=$twitter_defaults
        declare -A test_dens
        test_dens[$twitter_db]=$twitter_dens
        declare -A test_cor
        test_cor[$twitter_db]=$twitter_cor
        declare -A flags
        flags[$twitter_db]=$twitter_flags

  while in the configuration file *config.cfg*:

        twitter_db='twitter'
        twitter_defaults=0.7,2.7,1,false,false
        twitter_dens=2.7,2.6
        twitter_cor=0.7,0.8
        twitter_flags='0,0'