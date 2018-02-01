# Fixr Graph Extractor


[![Build Status](https://travis-ci.com/cuplv/FixrGraphExtractor.svg?token=8yApKhj2WXmbEPSYZsqa&branch=master)](https://travis-ci.com/cuplv/FixrGraphExtractor)

Sriram Sankaranarayanan, Sergio Mover and Rhys Braginton Pettee Olsen

University of Colorado at Boulder CUPLV Lab

Boulder, Colorado

Part of CUPLV's DARPA MUSE Effort

## Description
The tool processes a a set of java class files or a set of Java source files and extracts an Abstract Control Data Floww Graph (ACDFG) for each method declared in the code.
Here we use the name ACDFG to refer to GROUMS.


The tool takes as input a filter of API packages that are of interest (e.g. "android") and builds a ACDFG that only contains that set of API calls.

The tool may also slice the a control data flow graph only w.r.t. calls to a set of Android APIs (or some other user-defined API).



## Limitations
1. Presently, the extraction is limited to a intraprocedural analysis (one graph per procedure)
2. Limitations of the current slicing technique (the slicing is not sound):
  * No aliasing (due to this, we cannot handle arrays of arrays correctly, since in jimple they are encoded with intermeditate variables)
  * Method calls are opaque, and they do not change anything (e.g. memory content or have side effects on the parameters)



## Building and Execution

### Requirements

Building and execution requires:

- Working versions of the JDK and JRE for Java 1.7

*WARNING*: we explicitly require Java 1.7 when looking at source code (tests use the source code), due to this Soot limitation https://github.com/Sable/soot/issues/465

- A suitable version of SBT (found to work on 0.13.6)
- A suitable version of Scala (found to work on 2.11.4)

- Java Protobuffer. To install protobuffer on linux:
```
wget https://github.com/google/protobuf/releases/download/v2.6.1/protobuf-2.6.1.tar.gz
tar -xzvf protobuf-2.6.1.tar.gz
pushd protobuf-2.6.1 && ./configure --prefix=/usr && make && sudo make install && popd
```


### Build the tool

The tool can be run inside sbt or can run with Java, after creating a single jar file.

We can get a single jar file by running `sbt oneJar`


### Running the tool


An important point with the tool that comes from Soot is that you have to provide the jar files for the java or Android libraries, otherwise Soot will fail to build the Jimple representation.

#### Command line arguments of the tool

- Read source code: `-s [true|false]`
Default is false to read bytecode

- Classpath: `-l <soot-class-path>`

It contains all the library needed by the project (e.g., the RT.jar library)

- Input folder: `-p <path-to-the-project-files>`

Path to the project that contains the files.


- Enable jphantom: `-j [true|false]`
In many cases we do not have all the libraries needed by the project. We use a modified version of jphantom (https://github.com/gbalats/jphantom) to create a stub of the missing classes.
In this way, we can invoke soot.

- Timeout in the construction of the graph: `-t <n>`, `n` is the number of seconds

- Name of the github user: `-n <github-user>`

The github information are not compulsory, but if you provide them you will have a link back to the source code.

- Name of the github repository: `-r <repo-name>`

- Commit hash: `-h <commit-hash>`

- Url of the repository: `-u <repo-url>`

- Filter on the API packages: `-f package_1:package_2...:package_n`

Filter the ACDFG to use only methods from the listed packages. You must define a package, otherwise the tool will not extract any ACDFG.

- Output path: `-o <path-to-the-output-folder>`

The folder that will contains the ACDFGs.

The are created as binary file in this folder.

*Warning*: Now the tool creates a file name like `class_name.method_name`, where `class_name` and `method_name` are the name of the class and declared method, and thus ignore polymorphic methods.

The binary files can be read with protobuffer. The format of the ACDFGs is defined in `src/main/protobuf/proto_acdfg.proto`.


- Output provenance path: `-d <path-to-the-debug-output>`

The folder contains the dot representation of the ACDFG, the original and sliced jimple code and the dot representation of the control flow graph and control data flow graph.

For each graph, there is an HTML page that shows all these information.

The page shows an image of the graph.

*NOTE*: you must use Firefox or use Chrome with the flag `--allow-file-access-from-files`, since the page needs to access the local file system to render the graph.



#### A simple example

Get a Java program to test:

```
git clone https://github.com/innokenty/swing-file-browser
cd swing-file-browser
mvn compile
```

You can run the graph extractor as follows (from the path to the graph extractor):
```
java -jar ./target/scala-2.11/fixrgraphextractor_2.11-0.1-SNAPSHOT-one-jar.jar  -l <path-to-rt.jre> -p <path-to-swing-file-browser>/target/classes -j true -z <tmp-for-phantom>  -s false -n innokenty -r swing-file-browser -h 2465e919b57fad855f702644e5723bc51982eaaf  -u 'https://github.com/innokenty/swing-file-browser' -o <out-path> -d <out-path-provenance> -f java.swing:java.awt
```

Where:

- `<path-to-rt.jre`: is the path to the rt.jre file in the jvm library (e.g., if you have the jdk on Mac it should be similar to this: `/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/rt.jar`)

- `<path-to-swing-file-browser>`: is the path to the repository of swing-file-browser. In practice here you provide the base path to the class files (you can concatenate more path with `:`).

- `<tmp-for-phantom>` is the directory used by jphantom to store temporary class files (they will be removed automatically when not needed anymore).

- `<out-path-provenance>` and `<out-path>` are the path to the output folder that will be created by the extractor.



### Access the generated ACDFGs programmatically

The ACDFG are created in a format defined with Google protobuffer (see the file `src/main/protobuf/proto_acdfg.proto`).

You can create the protobuf libraries in your project or link the extractor libraries (if you use Java).



### Unit Tests
To perform the unit tests, run `sbt test`.


## SOOT issues
We use the commit version fe313b0 of soot to avoid later bugs still not solved (https://github.com/Sable/soot/issues/618)
