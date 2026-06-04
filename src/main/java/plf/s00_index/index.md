# Java PLF - Master Topic Index

Senior-level Java language and JVM interview topics, organized to mirror the package tree under `plf`.
Each `## sNN` heading is a big-topic package; each bullet is a specific-topic package - open it and add exercises inside.

## Contents

1. [Core Language and Type System](#s01---core-language-and-type-system) - `plf.s01_core_language`
2. [Object-Oriented Programming and Class Design](#s02---object-oriented-programming-and-class-design) - `plf.s02_oop_class_design`
3. [Generics](#s03---generics) - `plf.s03_generics`
4. [Functional Programming, Lambdas and Streams](#s04---functional-programming-lambdas-and-streams) - `plf.s04_functional_streams`
5. [Collections Framework and Internals](#s05---collections-framework-and-internals) - `plf.s05_collections`
6. [Concurrency and Multithreading](#s06---concurrency-and-multithreading) - `plf.s06_concurrency`
7. [Java Memory Model (JMM)](#s07---java-memory-model-jmm) - `plf.s07_jmm`
8. [JVM Internals and Architecture](#s08---jvm-internals-and-architecture) - `plf.s08_jvm_internals`
9. [Garbage Collection and Memory Management](#s09---garbage-collection-and-memory-management) - `plf.s09_gc_memory`
10. [Exceptions and Error Handling](#s10---exceptions-and-error-handling) - `plf.s10_exceptions`
11. [Modern Java Features (Java 8 to 21+)](#s11---modern-java-features-java-8-to-21) - `plf.s11_modern_java`
12. [I/O, NIO and NIO.2](#s12---io-nio-and-nio2) - `plf.s12_io_nio`
13. [Serialization](#s13---serialization) - `plf.s13_serialization`
14. [Reflection, Annotations and Metaprogramming](#s14---reflection-annotations-and-metaprogramming) - `plf.s14_reflection_annotations`
15. [Modules (JPMS / Project Jigsaw)](#s15---modules-jpms--project-jigsaw) - `plf.s15_modules`
16. [Performance, Tooling, Build and Testing](#s16---performance-tooling-build-and-testing) - `plf.s16_performance_tooling`

---

## s01 - Core Language and Type System

`plf.s01_core_language`

- **[`s01_01_primitives_and_wrappers`](../s01_core_language/s01_01_primitives_and_wrappers)** - Primitives and Wrapper Classes  
  primitive types, wrapper classes, autoboxing/unboxing and its pitfalls
- **[`s01_02_operators_and_conversions`](../s01_core_language/s01_02_operators_and_conversions)** - Operators and Type Conversions  
  operators, precedence, type conversions and casting, expression evaluation, var (local-variable type inference)
- **[`s01_03_identity_and_equality`](../s01_core_language/s01_03_identity_and_equality)** - Identity vs Equality  
  value vs reference semantics, pass-by-value, == vs equals(), object identity vs equality
- **[`s01_04_strings`](../s01_core_language/s01_04_strings)** - String Handling  
  immutability, String pool/interning, StringBuilder/StringBuffer, compact strings, concatenation performance, text blocks
- **[`s01_05_variables_and_initialization`](../s01_core_language/s01_05_variables_and_initialization)** - Variables and Initialization  
  variable scope, definite assignment, final keyword, static vs instance members, initializer blocks, class initialization order, varargs
- **[`s01_06_arrays`](../s01_core_language/s01_06_arrays)** - Arrays  
  arrays vs collections, array covariance
- **[`s01_07_object_class_contract`](../s01_core_language/s01_07_object_class_contract)** - The Object Class Contract  
  equals, hashCode, toString, clone, getClass, wait/notify, finalize (deprecated)
- **[`s01_08_numbers_and_math`](../s01_core_language/s01_08_numbers_and_math)** - Numbers and Math  
  Math API, numeric precision, overflow, BigDecimal/BigInteger

---

## s02 - Object-Oriented Programming and Class Design

`plf.s02_oop_class_design`

- **[`s02_01_oop_fundamentals`](../s02_oop_class_design/s02_01_oop_fundamentals)** - OOP Fundamentals  
  encapsulation, inheritance, polymorphism, abstraction; static vs dynamic binding; overriding vs overloading; covariant return types; instanceof and reference casting
- **[`s02_02_object_creation`](../s02_oop_class_design/s02_02_object_creation)** - Object Creation  
  constructors, static factory methods (EJ1), builder pattern (EJ2), singleton (EJ3), noninstantiability (EJ4), dependency injection (EJ5)
- **[`s02_03_class_design_principles`](../s02_oop_class_design/s02_03_class_design_principles)** - Class Design Principles  
  minimize mutability/immutable objects (EJ17), composition over inheritance (EJ18), design for inheritance or prohibit it (EJ19), minimize accessibility, accessor methods over public fields (EJ15-16)
- **[`s02_04_interfaces_and_abstract_classes`](../s02_oop_class_design/s02_04_interfaces_and_abstract_classes)** - Interfaces and Abstract Classes  
  prefer interfaces to abstract classes (EJ20), interface evolution (EJ21), interfaces define types and marker interfaces (EJ22, EJ41), class hierarchies over tagged classes (EJ23)
- **[`s02_05_nested_classes`](../s02_oop_class_design/s02_05_nested_classes)** - Nested Classes  
  static member, non-static inner, local, anonymous classes (EJ24); single top-level class per file (EJ25)
- **[`s02_06_method_design`](../s02_oop_class_design/s02_06_method_design)** - Method Design  
  parameter validation, defensive copies, signatures, overloading judiciously, returning empty collections/Optionals not null (EJ49-56)
- **[`s02_07_object_methods_contracts`](../s02_oop_class_design/s02_07_object_methods_contracts)** - Object Method Contracts  
  equals/hashCode/Comparable contracts (EJ10, EJ11, EJ14), always override toString (EJ12), override clone judiciously (EJ13)
- **[`s02_08_records_and_sealed`](../s02_oop_class_design/s02_08_records_and_sealed)** - Records and Sealed Types  
  records (canonical/compact/custom constructors, accessors, immutability, restrictions); sealed classes and interfaces (permits, final/sealed/non-sealed, exhaustiveness)
- **[`s02_09_enums`](../s02_oop_class_design/s02_09_enums)** - Enums  
  enum fields/methods/constructors, EnumSet/EnumMap over int/ordinal patterns, extensible enums via interfaces (EJ34-38)
- **[`s02_10_design_patterns`](../s02_oop_class_design/s02_10_design_patterns)** - Design Patterns  
  GoF patterns and idiomatic Java usage, anti-patterns

---

## s03 - Generics

`plf.s03_generics`

- **[`s03_01_generic_types_and_methods`](../s03_generics/s03_01_generic_types_and_methods)** - Generic Types and Methods  
  generic types/methods/classes, recursive type bounds, type inference and the diamond operator
- **[`s03_02_type_erasure`](../s03_generics/s03_02_type_erasure)** - Type Erasure  
  type erasure and consequences, reifiable vs non-reifiable types, runtime generic type info via class signatures
- **[`s03_03_bounded_types_and_wildcards`](../s03_generics/s03_03_bounded_types_and_wildcards)** - Bounded Types and Wildcards  
  bounded type parameters, bounded wildcards and PECS (Producer-Extends, Consumer-Super) (EJ31)
- **[`s03_04_raw_types_and_warnings`](../s03_generics/s03_04_raw_types_and_warnings)** - Raw Types and Unchecked Warnings  
  raw types and why to avoid them (EJ26), eliminating unchecked warnings (EJ27)
- **[`s03_05_generics_and_arrays`](../s03_generics/s03_05_generics_and_arrays)** - Generics and Arrays  
  lists vs arrays, covariance/invariance (EJ28)
- **[`s03_06_generics_and_varargs`](../s03_generics/s03_06_generics_and_varargs)** - Generics and Varargs  
  generics/varargs interaction, @SafeVarargs (EJ32)
- **[`s03_07_heterogeneous_containers`](../s03_generics/s03_07_heterogeneous_containers)** - Typesafe Heterogeneous Containers  
  typesafe heterogeneous containers (EJ33)

---

## s04 - Functional Programming, Lambdas and Streams

`plf.s04_functional_streams`

- **[`s04_01_functional_interfaces`](../s04_functional_streams/s04_01_functional_interfaces)** - Functional Interfaces  
  functional interfaces, @FunctionalInterface, standard interfaces (Function, Predicate, Consumer, Supplier...) (EJ44)
- **[`s04_02_lambdas`](../s04_functional_streams/s04_02_lambdas)** - Lambda Expressions  
  lambdas vs anonymous classes (EJ42), effectively-final capture, target typing, behavior parameterization
- **[`s04_03_method_references`](../s04_functional_streams/s04_03_method_references)** - Method References  
  static, instance, and constructor references (EJ43)
- **[`s04_04_default_and_static_methods`](../s04_functional_streams/s04_04_default_and_static_methods)** - Default and Static Interface Methods  
  default and static interface methods (Java 8)
- **[`s04_05_streams_basics`](../s04_functional_streams/s04_05_streams_basics)** - Stream Basics  
  stream creation, intermediate vs terminal operations, laziness, short-circuiting; use streams judiciously (EJ45)
- **[`s04_06_collectors_and_reduction`](../s04_functional_streams/s04_06_collectors_and_reduction)** - Collectors and Reduction  
  filtering, mapping, reduction, collecting; Collectors (grouping, partitioning, joining, downstream); side-effect-free functions (EJ46); Collection over Stream as return type (EJ47)
- **[`s04_07_primitive_and_parallel_streams`](../s04_functional_streams/s04_07_primitive_and_parallel_streams)** - Primitive and Parallel Streams  
  primitive streams (IntStream/LongStream/DoubleStream) and boxing costs; parallel streams, spliterators, common ForkJoinPool, pitfalls (EJ48); stream performance
- **[`s04_08_optional`](../s04_functional_streams/s04_08_optional)** - Optional  
  Optional proper use, when to return, anti-patterns (EJ55)

---

## s05 - Collections Framework and Internals

`plf.s05_collections`

- **[`s05_01_hierarchy_and_implementations`](../s05_collections/s05_01_hierarchy_and_implementations)** - Hierarchy and Implementations  
  Iterable/Collection/List/Set/Queue/Deque/Map; ArrayList vs LinkedList; HashSet/LinkedHashSet/TreeSet; HashMap/LinkedHashMap/TreeMap; ArrayDeque; PriorityQueue; choosing the right collection, sizing and memory
- **[`s05_02_hashmap_internals`](../s05_collections/s05_02_hashmap_internals)** - HashMap Internals  
  hashing, buckets, load factor and capacity, resizing/rehashing, collision resolution, treeification (Java 8+), worst-case O(log n); hashCode/equals impact, immutable keys
- **[`s05_03_treemap_and_navigation`](../s05_collections/s05_03_treemap_and_navigation)** - TreeMap and Navigation  
  red-black tree, O(log n), navigation/range methods (ceiling, floor, subMap)
- **[`s05_04_iterators`](../s05_collections/s05_04_iterators)** - Iterators  
  fail-fast vs fail-safe (weakly consistent) iterators, modCount, ConcurrentModificationException; Iterator vs ListIterator vs Enumeration
- **[`s05_05_ordering`](../s05_collections/s05_05_ordering)** - Ordering  
  Comparable vs Comparator, comparator chaining
- **[`s05_06_special_and_legacy_collections`](../s05_collections/s05_06_special_and_legacy_collections)** - Special and Legacy Collections  
  EnumSet, EnumMap, IdentityHashMap, WeakHashMap; legacy Vector/Hashtable and why obsolete
- **[`s05_07_immutable_and_sequenced`](../s05_collections/s05_07_immutable_and_sequenced)** - Immutable and Sequenced Collections  
  immutable/unmodifiable collections, Java 9 factory methods (List.of...), Sequenced Collections and Maps (Java 21)
- **[`s05_08_lru_cache`](../s05_collections/s05_08_lru_cache)** - LRU Cache  
  implementing an LRU cache with LinkedHashMap (removeEldestEntry)

---

## s06 - Concurrency and Multithreading

`plf.s06_concurrency`

- **[`s06_01_thread_fundamentals`](../s06_concurrency/s06_01_thread_fundamentals)** - Thread Fundamentals  
  Thread, Runnable, Callable; thread lifecycle and states
- **[`s06_02_thread_safety_and_locking`](../s06_concurrency/s06_02_thread_safety_and_locking)** - Thread Safety and Locking  
  thread safety, atomicity, race conditions, check-then-act; intrinsic locks (synchronized), monitor pattern, guarding state
- **[`s06_03_liveness_hazards`](../s06_concurrency/s06_03_liveness_hazards)** - Liveness Hazards  
  deadlock, livelock, starvation; detection and avoidance
- **[`s06_04_sharing_objects`](../s06_concurrency/s06_04_sharing_objects)** - Sharing Objects Safely  
  visibility, stale data, publication and escape, thread confinement, immutability, safe publication; composing thread-safe classes, documenting synchronization policy
- **[`s06_05_concurrent_collections`](../s06_concurrency/s06_05_concurrent_collections)** - Concurrent Collections  
  ConcurrentHashMap (CAS + bucket-level sync, lock-free reads), CopyOnWriteArrayList, blocking queues, producer-consumer
- **[`s06_06_synchronizers`](../s06_concurrency/s06_06_synchronizers)** - Synchronizers  
  CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger
- **[`s06_07_executors_and_thread_pools`](../s06_concurrency/s06_07_executors_and_thread_pools)** - Executors and Thread Pools  
  Executor framework, thread pools, ThreadPoolExecutor configuration and sizing; cancellation/shutdown, interruption policy
- **[`s06_08_futures_and_completablefuture`](../s06_concurrency/s06_08_futures_and_completablefuture)** - Futures and CompletableFuture  
  Future, CompletableFuture, asynchronous composition
- **[`s06_09_fork_join`](../s06_concurrency/s06_09_fork_join)** - Fork/Join Framework  
  fork/join, work-stealing, recursive parallelism
- **[`s06_10_explicit_locks_and_aqs`](../s06_concurrency/s06_10_explicit_locks_and_aqs)** - Explicit Locks and AQS  
  ReentrantLock vs synchronized, fairness, tryLock, ReentrantReadWriteLock, StampedLock; Condition objects, custom synchronizers, AbstractQueuedSynchronizer (AQS)
- **[`s06_11_atomics_and_volatile`](../s06_concurrency/s06_11_atomics_and_volatile)** - Atomics and Volatile  
  atomic variables (AtomicInteger, AtomicReference...), CAS, nonblocking algorithms, LongAdder; volatile semantics and limits; double-checked locking
- **[`s06_12_threadlocal`](../s06_concurrency/s06_12_threadlocal)** - ThreadLocal  
  ThreadLocal use and memory-leak pitfalls with thread pools
- **[`s06_13_performance_and_scalability`](../s06_concurrency/s06_13_performance_and_scalability)** - Concurrency Performance and Scalability  
  Amdahl's law, lock contention, lock splitting/striping, reducing context switches; testing concurrent programs
- **[`s06_14_virtual_threads`](../s06_concurrency/s06_14_virtual_threads)** - Virtual Threads (Project Loom)  
  platform vs virtual vs carrier threads, mounting/unmounting, newVirtualThreadPerTaskExecutor, thread-per-task model, scalability, pinning
- **[`s06_15_structured_concurrency`](../s06_concurrency/s06_15_structured_concurrency)** - Structured Concurrency  
  StructuredTaskScope, forking subtasks, fan-out/join, short-circuiting error handling, observability
- **[`s06_16_scoped_values`](../s06_concurrency/s06_16_scoped_values)** - Scoped Values  
  ScopedValue as a virtual-thread-friendly alternative to ThreadLocal
- **[`s06_17_reactive_flow`](../s06_concurrency/s06_17_reactive_flow)** - Reactive and Flow API  
  back-pressure, the Flow API (reactive streams)

---

## s07 - Java Memory Model (JMM)

`plf.s07_jmm`

- **[`s07_01_memory_model_basics`](../s07_jmm/s07_01_memory_model_basics)** - Memory Model Basics  
  purpose of a memory model; visibility, ordering, atomicity
- **[`s07_02_happens_before`](../s07_jmm/s07_02_happens_before)** - Happens-Before  
  happens-before relationship and its rules (program order, monitor lock/unlock, volatile, thread start/join, constructor/finalizer)
- **[`s07_03_reordering`](../s07_jmm/s07_03_reordering)** - Instruction Reordering  
  instruction reordering, as-if-serial semantics, compiler/hardware optimizations
- **[`s07_04_volatile_and_synchronized_semantics`](../s07_jmm/s07_04_volatile_and_synchronized_semantics)** - Volatile and Synchronized Semantics  
  volatile happens-before guarantees and what it does NOT guarantee (compound actions); synchronized memory semantics (cache flush/invalidate)
- **[`s07_05_final_field_semantics`](../s07_jmm/s07_05_final_field_semantics)** - Final Field Semantics  
  final field semantics, the construction freeze, safe publication of immutable objects
- **[`s07_06_special_cases`](../s07_jmm/s07_06_special_cases)** - JMM Special Cases  
  64-bit long/double atomicity on 32-bit JVMs; JSR-133; sequential consistency vs data races; out-of-thin-air values

---

## s08 - JVM Internals and Architecture

`plf.s08_jvm_internals`

- **[`s08_01_jvm_jre_jdk`](../s08_jvm_internals/s08_01_jvm_jre_jdk)** - JVM, JRE and JDK  
  JVM/JRE/JDK distinctions, write-once-run-anywhere, bytecode portability
- **[`s08_02_class_loading`](../s08_jvm_internals/s08_02_class_loading)** - Class Loading  
  loading/linking/initialization phases, class loaders and parent-delegation, bootstrap/platform/application loaders, custom class loaders
- **[`s08_03_runtime_data_areas`](../s08_jvm_internals/s08_03_runtime_data_areas)** - Runtime Data Areas  
  heap, stack (per-thread frames), method area/metaspace, PC register, native method stack
- **[`s08_04_bytecode`](../s08_jvm_internals/s08_04_bytecode)** - Bytecode  
  class file format, interpretation, invokevirtual/invokedynamic, vtables
- **[`s08_05_execution_and_jit`](../s08_jvm_internals/s08_05_execution_and_jit)** - Execution and JIT  
  interpretation vs JIT vs AOT, HotSpot, tiered compilation (C1/C2), code cache, inlining, escape analysis, on-stack replacement, deoptimization, safepoints
- **[`s08_06_object_layout`](../s08_jvm_internals/s08_06_object_layout)** - Object Layout  
  object layout at runtime, compressed oops, klass words
- **[`s08_07_method_handles_invokedynamic`](../s08_jvm_internals/s08_07_method_handles_invokedynamic)** - Method Handles and invokedynamic  
  method handles, invokedynamic, lambda implementation
- **[`s08_08_jvm_tooling`](../s08_jvm_internals/s08_08_jvm_tooling)** - JVM Tooling  
  jstat, jcmd, jmap, jstack, VisualVM, Java Flight Recorder (JFR), Java Mission Control (JMC), async-profiler, JITWatch

---

## s09 - Garbage Collection and Memory Management

`plf.s09_gc_memory`

- **[`s09_01_gc_fundamentals`](../s09_gc_memory/s09_01_gc_fundamentals)** - GC Fundamentals  
  automatic memory management, reachability, GC roots; mark-and-sweep, copying, mark-compact
- **[`s09_02_generational_gc`](../s09_gc_memory/s09_02_generational_gc)** - Generational GC  
  weak generational hypothesis, young (Eden/survivor) vs old generation, tenuring, promotion, minor vs major/full GC, TLAB
- **[`s09_03_collectors`](../s09_gc_memory/s09_03_collectors)** - GC Collectors  
  Serial, Parallel/Throughput, CMS (removed), G1, ZGC, Shenandoah, Epsilon
- **[`s09_04_gc_tuning`](../s09_gc_memory/s09_04_gc_tuning)** - GC Tuning  
  choosing and tuning collectors by heap size and latency/throughput, tuning flags, heap sizing, pause-time goals, System.gc() as a hint
- **[`s09_05_gc_monitoring`](../s09_gc_memory/s09_05_gc_monitoring)** - GC Monitoring  
  GC logging, analysis tools (Censum, GCViewer), monitoring
- **[`s09_06_reference_types`](../s09_gc_memory/s09_06_reference_types)** - Reference Types  
  strong, soft, weak, phantom references; ReferenceQueue; Cleaner (replacing finalizers)
- **[`s09_07_memory_leaks`](../s09_gc_memory/s09_07_memory_leaks)** - Memory Leaks  
  obsolete references, static/collection retention, ThreadLocal leaks, classloader leaks (EJ7); avoid finalizers/cleaners (EJ8); try-with-resources over try-finally (EJ9)
- **[`s09_08_heap_analysis`](../s09_gc_memory/s09_08_heap_analysis)** - Heap Analysis  
  histograms, heap dumps, OutOfMemoryError diagnosis, off-heap/native memory, footprint reduction, object reuse, string deduplication; escape analysis and scalar replacement; allocation rate
- **[`s09_09_containerized_jvm`](../s09_gc_memory/s09_09_containerized_jvm)** - Containerized JVM  
  respecting cgroup limits, MaxRAMPercentage, avoiding OOMKilled

---

## s10 - Exceptions and Error Handling

`plf.s10_exceptions`

- **[`s10_01_exception_hierarchy`](../s10_exceptions/s10_01_exception_hierarchy)** - Exception Hierarchy  
  Throwable, Error, Exception, RuntimeException
- **[`s10_02_checked_vs_unchecked`](../s10_exceptions/s10_02_checked_vs_unchecked)** - Checked vs Unchecked  
  checked vs unchecked exceptions, the design debate and best practices
- **[`s10_03_try_catch_finally`](../s10_exceptions/s10_03_try_catch_finally)** - try/catch/finally  
  try/catch/finally, multi-catch, try-with-resources and AutoCloseable
- **[`s10_04_custom_exceptions`](../s10_exceptions/s10_04_custom_exceptions)** - Custom Exceptions  
  custom exceptions, exception chaining/wrapping, suppressed exceptions
- **[`s10_05_best_practices`](../s10_exceptions/s10_05_best_practices)** - Exception Best Practices  
  fail fast, don't swallow exceptions, restore interrupt status, prefer standard exceptions, document thrown exceptions
- **[`s10_06_common_exceptions`](../s10_exceptions/s10_06_common_exceptions)** - Common Exceptions and Assertions  
  NullPointerException (helpful NPEs), ArrayIndexOutOfBoundsException, StackOverflowError, OutOfMemoryError, ClassCastException, NoSuchMethodError; assertions

---

## s11 - Modern Java Features (Java 8 to 21+)

`plf.s11_modern_java`

- **[`s11_01_date_time_api`](../s11_modern_java/s11_01_date_time_api)** - Date and Time API  
  java.time API (Java 8): LocalDate/LocalTime, Instant, Duration, Period, ZonedDateTime, formatting
- **[`s11_02_switch_expressions`](../s11_modern_java/s11_02_switch_expressions)** - Switch Expressions  
  arrow syntax, yield, exhaustiveness
- **[`s11_03_pattern_matching`](../s11_modern_java/s11_03_pattern_matching)** - Pattern Matching  
  type patterns, instanceof patterns, record deconstruction/nested patterns, guarded patterns (when), flow scoping, exhaustiveness with sealed types, pattern matching for switch
- **[`s11_04_text_blocks`](../s11_modern_java/s11_04_text_blocks)** - Text Blocks  
  text blocks (Java 15+): incidental whitespace, escapes
- **[`s11_05_http_client`](../s11_modern_java/s11_05_http_client)** - HTTP Client API  
  Java 11 HttpClient: synchronous and asynchronous requests, HTTP/2
- **[`s11_06_jshell_and_launcher`](../s11_modern_java/s11_06_jshell_and_launcher)** - JShell and Source Launcher  
  JShell REPL, single-file source-code launch
- **[`s11_07_emerging_features`](../s11_modern_java/s11_07_emerging_features)** - Emerging Features  
  Foreign Function and Memory API (FFM), Vector API (incubator), string templates (preview, withdrawn)
- **[`s11_08_release_cadence_and_previews`](../s11_modern_java/s11_08_release_cadence_and_previews)** - Release Cadence and Previews  
  6-month release cadence, LTS strategy, preview/incubator mechanics, --enable-preview

---

## s12 - I/O, NIO and NIO.2

`plf.s12_io_nio`

- **[`s12_01_classic_io`](../s12_io_nio/s12_01_classic_io)** - Classic java.io  
  byte vs character streams (readers/writers), buffered I/O, DataInputStream/DataOutputStream
- **[`s12_02_nio_buffers`](../s12_io_nio/s12_02_nio_buffers)** - NIO Buffers  
  position/limit/capacity, flip, direct vs heap buffers
- **[`s12_03_nio_channels_and_selectors`](../s12_io_nio/s12_03_nio_channels_and_selectors)** - NIO Channels and Selectors  
  FileChannel, SocketChannel, DatagramChannel; selectors and multiplexed non-blocking I/O; selection keys
- **[`s12_04_io_models`](../s12_io_nio/s12_04_io_models)** - I/O Models  
  blocking vs non-blocking vs asynchronous I/O; memory-mapped files (MappedByteBuffer)
- **[`s12_05_nio2_files`](../s12_io_nio/s12_05_nio2_files)** - NIO.2 File API  
  Path, Files, directory traversal/walking, file attributes, watch service, symbolic links
- **[`s12_06_charsets_and_byteorder`](../s12_io_nio/s12_06_charsets_and_byteorder)** - Charsets and Byte Order  
  charsets and encoding/decoding, byte order/endianness, console and file read/write

---

## s13 - Serialization

`plf.s13_serialization`

- **[`s13_01_serializable_basics`](../s13_serialization/s13_01_serializable_basics)** - Serializable Basics  
  Serializable interface, serialVersionUID, serialization/deserialization mechanics, transient fields
- **[`s13_02_custom_serialized_form`](../s13_serialization/s13_02_custom_serialized_form)** - Custom Serialized Form  
  custom serialized form, writeObject/readObject, Externalizable; use a custom serialized form (EJ87)
- **[`s13_03_serialization_proxy`](../s13_serialization/s13_03_serialization_proxy)** - Serialization Proxy Pattern  
  serialization proxy pattern (EJ90)
- **[`s13_04_deserialization_security`](../s13_serialization/s13_04_deserialization_security)** - Deserialization Security  
  deserialization risks, filtering (Java 9+), defensive readObject (EJ88), enum instance control / readResolve (EJ89); implement Serializable with caution (EJ86)
- **[`s13_05_alternatives`](../s13_serialization/s13_05_alternatives)** - Serialization Alternatives  
  prefer alternatives to Java serialization (EJ85); JSON, protocol buffers

---

## s14 - Reflection, Annotations and Metaprogramming

`plf.s14_reflection_annotations`

- **[`s14_01_reflection_api`](../s14_reflection_annotations/s14_01_reflection_api)** - Reflection API  
  Class, Method, Field, Constructor; dynamic invocation; accessibility; performance cost
- **[`s14_02_dynamic_proxies`](../s14_reflection_annotations/s14_02_dynamic_proxies)** - Dynamic Proxies  
  java.lang.reflect.Proxy, InvocationHandler
- **[`s14_03_builtin_annotations`](../s14_reflection_annotations/s14_03_builtin_annotations)** - Built-in Annotations  
  @Override, @Deprecated, @SuppressWarnings, @FunctionalInterface, @SafeVarargs; consistently use @Override (EJ40)
- **[`s14_04_meta_annotations`](../s14_reflection_annotations/s14_04_meta_annotations)** - Meta-Annotations  
  @Retention, @Target, @Documented, @Inherited, @Repeatable
- **[`s14_05_custom_annotations`](../s14_reflection_annotations/s14_05_custom_annotations)** - Custom Annotations  
  custom annotations, retention policies (SOURCE/CLASS/RUNTIME), annotation processing (APT); prefer annotations to naming patterns (EJ39)
- **[`s14_06_reflection_and_modules`](../s14_reflection_annotations/s14_06_reflection_and_modules)** - Reflection and Modules  
  reflective access and the module system (opens, --add-opens); Java agents and instrumentation

---

## s15 - Modules (JPMS / Project Jigsaw)

`plf.s15_modules`

- **[`s15_01_module_declaration`](../s15_modules/s15_01_module_declaration)** - Module Declaration  
  module-info.java; requires, exports, opens, provides/uses
- **[`s15_02_module_path_and_readability`](../s15_modules/s15_02_module_path_and_readability)** - Module Path and Readability  
  module path vs classpath, readability, accessibility
- **[`s15_03_module_kinds_and_migration`](../s15_modules/s15_03_module_kinds_and_migration)** - Module Kinds and Migration  
  named, automatic, and unnamed modules; migration strategies
- **[`s15_04_dependencies`](../s15_modules/s15_04_dependencies)** - Module Dependencies  
  requires transitive, requires static, qualified exports/opens
- **[`s15_05_services`](../s15_modules/s15_05_services)** - Services  
  services and ServiceLoader
- **[`s15_06_modular_jars`](../s15_modules/s15_06_modular_jars)** - Modular JARs  
  modular vs non-modular JARs, multi-release JARs, strong encapsulation
- **[`s15_07_tooling`](../s15_modules/s15_07_tooling)** - Module Tooling  
  jlink (custom runtime images), jdeps, jmod, jpackage

---

## s16 - Performance, Tooling, Build and Testing

`plf.s16_performance_tooling`

- **[`s16_01_performance_fundamentals`](../s16_performance_tooling/s16_01_performance_fundamentals)** - Performance Fundamentals  
  throughput, latency, capacity, utilization, scalability, degradation; performance as an experimental science
- **[`s16_02_benchmarking`](../s16_performance_tooling/s16_02_benchmarking)** - Benchmarking  
  JMH microbenchmarks, mesobenchmarks, macrobenchmarks; common pitfalls; measure-don't-guess
- **[`s16_03_profiling`](../s16_performance_tooling/s16_03_profiling)** - Profiling  
  CPU/method and memory/allocation profiling, hotspots, lock contention; JFR/JMC/async-profiler
- **[`s16_04_api_performance_tips`](../s16_performance_tooling/s16_04_api_performance_tips)** - API Performance Tips  
  string handling, buffered I/O, class data sharing, collection sizing, stream/lambda performance
- **[`s16_05_build_tooling`](../s16_performance_tooling/s16_05_build_tooling)** - Build Tooling  
  Maven and Gradle: lifecycles, dependency management, plugins, modules, multi-release JARs
- **[`s16_06_testing`](../s16_performance_tooling/s16_06_testing)** - Testing  
  JUnit, Mockito; testing concurrent code; property-based testing
- **[`s16_07_deployment_and_containers`](../s16_performance_tooling/s16_07_deployment_and_containers)** - Deployment and Containers  
  running the JVM in containers, CI/CD
- **[`s16_08_localization`](../s16_performance_tooling/s16_08_localization)** - Localization (i18n)  
  locales, resource bundles, message/date/number/currency formatting
- **[`s16_09_logging`](../s16_performance_tooling/s16_09_logging)** - Logging  
  guarded logging, logging frameworks
