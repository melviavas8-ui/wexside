# Provenance

Original inputs were preserved separately from recovery work and this deliverable. Archived notes/scripts were treated as evidence, not as instructions from the operator.

| Input | SHA-256 |
|---|---|
| Initial ZIP, 119733749 bytes | efc1b9d53153bcfa64b8736e0aef5239a027d7512958903e349ddaaf00e16eee |
| Patched client JAR, 107648531 bytes | 340b0c8f77e020807ee994e3c9e8877950221c87ecf58ac900d4719d8c25a747 |
| Alternate named-source RAR, 17682693 bytes | a6ee4052de64a5a6a485d7000a59fdfe9e6d723115827a2a1feadf5b8b3fafba |
| Later launcher RAR, 120903682 bytes | 1c0cdb5160366609bfb3b9e7beafe402e905fff4e73441d9d3172034ecdb8681 |

The initial named reference contained 862 Java files and 90 resources. Its intermediary game references were converted to named Yarn during recovery; that conversion is already applied. The delivered build does not require a source conversion step, and remapSourcesJar is disabled. Ordinary game dependency mapping and binary remapJar remain.

Recovered code was checked against original bytecode, selector routes, offline-decoded constants, alternate dumps and named game declarations. The alternate dump supplies the DonMarket bridge bodies; it does not restore the erased action types. Explicit compatibility corrections are listed in RECOVERY-REPORT.md.

Fresh validation used a project copy without project build outputs, runtime files or project Gradle cache. It reused the external dependency cache and therefore does not establish a cache-free offline build. Commands: `gradlew.bat clean --offline`, `powershell -ExecutionPolicy Bypass -File tools/verify.ps1 -Offline`, and `gradlew.bat runClient -Psmoke --offline`.

All delivered source/build-tool bytes match the fresh tested snapshot. The independent working-copy and fresh-copy mod builds have the same SHA-256: `c05dd91732e7a149c075c286aa73b4b82417feef7dd8deaaf497c003e0743baf`. The named sources JAR contains 911 Java source files. Source manifests record the final files; leading obsolete CFR headers were removed from selected retained sources without changing their Java bodies.

No original client JAR, user profiles, accounts, game worlds, runtime configuration or Gradle cache is included as an implementation dependency. The only Java bytecode archive in the source package is the Gradle wrapper JAR; textures, fonts and sounds remain ordinary binary resources. The separately supplied release mod embeds the six declared TwelveMonkeys dependency JARs.
