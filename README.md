Query Learning Algorithm for Residual Symbolic Finite Automata
====

An implementation of ["Query Learning Algorithm for Residual Symbolic Finite Automata"](https://arxiv.org/abs/1902.07417).

How to run
----

1. Install `symbolicautomata` library from [this repository](https://github.com/zaburo-ch/symbolicautomata/tree/abdaca250002d9ae2ced9cce91df1dc9c86b10a0) (we needed to change the library a bit). If you use Eclipse, you can import it as a Maven project and install it by `Maven install`.

2. Build `rsfalearning` project with Maven. If you use Eclipse, you can build with `Maven build`. You will get `rsfalearning-0.0.1-SNAPSHOT.jar` and `rsfalearning-0.0.1-SNAPSHOT-jar-with-dependencies.jar` in `target` directory.

3. Run it as java program. We recommend to allow the program to allocate a large memory. We ran the largest experiment with options as follows.
```
java -Xms200g -Xmx200g -ea -jar rsfalearning-0.0.1-SNAPSHOT-jar-with-dependencies.jar > results
```
