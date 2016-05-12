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

It's with this in mind that we propose creating a massively parallel system based on identifying approximate isomorphisms between abstracted control data flow graphs (ACDFGs) generated from source programs lowered to an intermediate representation.

## Plan

Our long-term plan spans at least through late june and will likely run through the end of the summer, if not later. Our short-term plan represents our objectives for approximately the next week.

### Long-Term Plan

Our long-term plan consists of building the following components:

1. **Abstract graph extraction** that accepts a single Java class as input, transforms this class to a low-level intermediate representation using parsing and syntax-directed rewriting techniques, generates a control data flow graph (CDFG), and from this generates an *abstract control data flow graph* (ACDFG) whose instruction nodes represent only which, if any, methods were invoked and what variables they were assigned to. Finally, the graph extractor serializes the ACDFG in a space-efficient byte representation for offline processing.

2. **Offline approximate isomorphism mining** that based on an approximate bag-of-methods match between two classes performs intensive approximate graph isomorphism before storing the match and possibly assigning a similarity measure between the two programs.

3. **Relevant code retrieval** that based on the results of the isomorphism mining retrieves and presents matches for a given code snippet.

### Short-Term Plan

Our short-term plan is focused upon item 1 of the long-term plan and involves the development of the abstract graph extractor.

As of this writing, a Soot-based representation system capable of transforming Java classes into CDFGs has been implemented, as has a means of generating `.dot` files for graph visualization purposes. Additionally, a provisional Protobuff data type for ACDFGs has been defined, giving us a means to automatically generate classes against Java, Scala, C++ and other languages capable of handling ACDFGs. Our immediate task is two-fold: prepare conversion from Soot CDFGs to our own ACDFG representation, and prepare serialization of the ACDFG in the ProtoBuff format.

Our ACDFG format will capture the following features of the CDFG from which a given ACDFG is generated:

- Each individual control node of the original CDFG.

- Each control edge of the original CDFG from one control node to another.

- The triple (`a`, `m`, `g`) for each intermediate representation node of the form `a = m(g1, g2, ..., gn)`, where `a` is the name of the (optional) assignee, `m` is the (full) name of the method being invoked, and `g` is simply the list of argument names for `g1` through `gn` to `m`.
 
- Each data node of the original CDFG.
 
- Each def edge from a control node to a data node.
 
- Each get edge from a data node to a control node.
 
At the present moment, we hypothesize that this abstraction will suffice to capture the program invariants of Java classes using the Android API against which we're posing the relevant code search probem while avoiding needless overhead by omitting features that add costs not expected to improve satisfaction of our three requirements.

Based on our preliminary results with graph inspection, we may experiment with eliding control nodes that are empty or merging all nodes in a given basic block that are connected with the same data nodes. Our goal is to define the abstraction in such a way that it will be easy to extend and possible to modify.

## FAQs

**Why is the bag of methods representation of programs being replaced?**

We intend to use the bag of methods representation as a filter to narrow down the number of candidates for (more expensive) approximate subgraph isomorphism matching using a cutoff in size of the symmetric difference between two prospective matches. If the size of the symmetric difference of the bags for each of two methods is too high, approximate subgraph isomorphism will not be performed on the corresponding ACDFGs. The usefulness of this lower-cost filtering to reduce the number of expensive analyses conducted in program similarity tasks at scale is attested to by Long et al.'s "Automatic Patch Generation by Learning Correct Code". The need to move beyond bag of methods in our analysis is supported by our own research and our inability to meet requirement 2 of precision by using bag of methods alone.

**Won't syntactic differences between programs, especially in the use of control structures, hurt ACDFG isomorphism between semantically similar programs, damaging precision (requirement 2) or especially sensitivity (requirement 3)?**

Soot's reduction to a low-level representation prior to the generation of CDFGs features reduction to a nomal form for Java classes that includes a large variety of common compiler optimizations such as loop flattening and method inlining that we will be able to tune. Besides this, every statement in the low-level representation involves at most three variables. Our prior work (Olsen: "Using High-Level Source Code Transformation to Expose Similar Functionality") attests to the efficacy of using such transformations to expose semantic similarities similar to those of concern in this project, and the transformations so evaluated are less extensive than those supported by Soot.

**Why is so little of the original semantics preserved by ACDFG transformation? Won't this hurt precision (requirement 2) or sensitivity (requirement 3)?**

Because our analysis is only concerned with the use of methods in the Android API, only aspects of the program that interface with the Android API and the control structures around them are relevant to our analysis. It's true that this analysis may miss a substantial amount of data flowing into Android API invocations, but our present hypothesis is that the preserveration of data nodes will suffice to capture use of similar argument and variable patterns. This leaves the ellision of operations on Java primitives and built-in classes, the use of same-app helper classes, and the use of third-party classes.

Except for in a few cases, we do not expect operations on Java primitives and built-in classes, including the use of arithmetic, string and list operations, to have much bearing on similar uses of the Android API. We are at liberty to extend the ACDFG specification to handle more of this behavior as the project matures.

Three things count against better handling of other same-app and third-party classes. First, since we are not able to build a majority of Android applications in our corpus, it's somewhere between very challenging and impossible to acquire third-party classes in general, and the inclusion of other same-app classes requires significantly rethinking how ACDFGs are stored, grouped, and generated. Second, including all functionality in dependencies that may aid in analyzing a class could very easily increase the size of the average representation by a factor of 10, with clearly diminishing returns since most of the extra functionality in class dependencies has nothing to do with what a given class does, especially with the Android API. Finally, even if including these other classes in our analysis improved sensitivity (requirement 3), all similarities to which the analysis would then be sensitive to would be so complex in their dependencies as to diminish the usefulness of the similarity search: useful similarities should be possible for a typical developer to identify and learn from quickly. On balance, confining our analysis to individual classes is entirely necessary.

**If buildability is a problem, how do you intend to run extraction on classes that can't be built?**

There are versions of Soot capable of generating CDFG representations from programs that cannot be built. These CDFGs are as straightforward to convert to ACDFGs as those generated from buildable apps. When we encounter buildability problems, this will come into play.

**Given how lossy ACDFG generation is, how do you intend to recover information to support the similarity of two classes?**

At every step of our design, we will incorporate features to track class provenance, retaining as many intermediates as is feasible. Considering this problem up front will make it significantly easier to support the conclusions of the subgraph approximate isomorphism. Thought must be lent to how found approximate isomorphisms will be used to annotate source code, but this task will be easier to support with provenance information in place. ACDFG generation can be done so as to match each generated node against its original. The use of Soot should make it possible to relate CDFGs back to the source code from which it was generated.
