# Approximate Isomorphism Mining Over Control Data Flow Graphs at Scale PIFAQ

Rhys Braginton Pettee Olsen

Bor-Yuh Evan Chang, Sergio Mover, Sriram Sankaranarayanan

## Why?
 
As part of CUPLV’s MUSE Fixr effort, we need a way to perform relevant code search that meets the following three requirements:

**1: Computationally Tractable at Scale**: our offline notion of similarity between two code snippets must be possible to compute from a corpus of ~100k repositories. Moreover, once generated, it should be possible to explore relevant code in real time or near-real time; the user won’t accept having to wait several minutes for relevant code to be produced.

**2: Precise**: our means of search should generate only results that are truly relevant to the code searched against. We mustn’t generate results that are irrelevant (have nothing to do with our search) or spurious (have something to do with our search, such as using many of the same methods, but don’t implement similar functionality).

**3: Sensitive/Sophisticated**: our means of search should generate results that are relevant in terms of functionality despite significant differences in code structure or even program behavior (or perhaps even because of such differences if such differences give rise to a latent bug condition the user wishes to identify).

Achieving requirement 1 in tandem with the other two requirements, both of which add sigificantly to our costs to compute, requires developing infrastructure that is:

- **Highly parallel** at the computer, processor core and possibly vector-processing unit level.

- **Carefully budgeted** in the use of costly abstractions (eg: garbage collection may constitute unacceptable overhead for much of the analysis).

- **Free of and/or Tolerant to Faults**: if analysis takes several days to compute across many machines, we must either be guaranteed that no machine will fail or that any machine that does fail will not cause the entire process to fail.

All of these represent significant software engineering challenges.

Achieving requirements 2 and 3 requires the ability to reason about code both *syntactially* and *semantically*. Because we are performing relevant code search with respect to one API (the Android API), It is possible to achieve requirement 2's demands against irrelevence by comparing two code modules based only on the methods they both use. In practice, this "bag of methods" approach is insufficiently precise: it has been found to fall short of requirement 2's demands against spurious results and introduces too many false positives. Achieving all of the demands of requirement 2 requires awareness not just of methods used but of program structure, especially control flow. At the same time, it's more challenging to define a notion of program similarity based on control flow, let alone one that is inexpensive enough to compute at scale. Finally, we hypothesize that requirement 3 cannot be achieved without first performing source code transformations on our input programs that reduce common equivalent structures to a single normal form.

It's with this in mind that we propose creating a massively parallel system based on identifying approximate isomorphisms between abstracted control data flow graphs generated from source programs lowered to an intermediate representation.

## Plan

Our long-term plan consists of building the following components:

1. **Abstract graph extraction** that accepts a single Java class as input, transforms this class to a low-level intermediate representation using parsing and syntax-directed rewriting techniques, generates a control data flow graph, and from this generates an *abstract control data flow graph* whose instruction nodes represent only which, if any, methods were invoked and what variables they were assigned to. Finally, the graph extractor serializes the abstract control data flow graph in a space-efficient byte representation for offline processing.

2. **Offline approximate isomorphism mining** that based on an approximate bag-of-methods match between two classes performs intensive approximate graph isomorphism before storing the match and possibly assigning a similarity measure between the two programs.

3. **Relevant code retrieval** that based on the results of the isomorphism mining retrieves and presents matches for a given code snippet.
4. 
In the short term, it is necessary to first generate 
