# clojure-rust-graalvm

An example of Clojure program calling a Rust library, all combined into one executable using GraalVM.
It gets the amount of free memory via the
[heim-rs](https://github.com/heim-rs/heim) library and prints it in EDN format.

This repo is an adapted example of what is described in the README of the Rust
[jni](https://docs.rs/jni/0.14.0/jni/) library.

In `clojure/src-java` there is a Java static method which calls a Rust function
via JNI. We call this static method from Clojure.

## Usage

``` shell
$ time ./clojure-rust megabyte
{:memory/free [:megabyte "1210"]}
./clojure-rust megabyte   0.01s  user 0.01s system 34% cpu 0.027 total
```

Accepted options: `byte`, `megabyte`, `gigabyte`.

## Build

Prerequisites:

- Download [GraalVM](https://www.graalvm.org/downloads/) and set `GRAALVM_HOME`
- Install [lein](https://github.com/technomancy/leiningen)
- Install [cargo](https://doc.rust-lang.org/cargo/getting-started/installation.html)

Run `script/compile` to build the Rust lib, the Clojure uberjar and the GraalVM executable.

Finally, run the executable:

``` shell
$ target/clojure-rust
{:memory/free [:byte "896126976"]}
```

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
