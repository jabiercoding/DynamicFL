# Getting started (replication)
* Follow the setting up instructions from https://github.com/but4reuse/argouml-spl-benchmark . The ArgoUMLSPLBenchmark project must be in the workspace and the "Original" scenario must be generated.
* Clone this repository and import the Java project ArgoUMLSPLDynamicFL in the workspace.
* Download and unzip the dataset in your computer https://drive.google.com/file/d/1p1Jn47t7a_I81z55vjo5k-S9BJ36XDo7/view?usp=sharing (the dataset will be available at Zenodo if paper gets accepted).
* Open the Java main method in dynamicfl.Main and change the PATH_DATASET and PATH_ARGOUMLSPL_BENCHMARK values to those in your computer
* Run the Java main method in dynamicfl.Main. Results will be generated in a folder called "output" in the ArgoUMLSPLDynamicFL project. Several hours will be needed. You can remove the addition of spectrum-based ranking metrics in dynamicfl.gridSearch.GridSearch (commenting the source code line) if you are interested in one in particular.
* For the performance values, run the Java main method in dynamicfl.performance.MainPerformance
