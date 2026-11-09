# Hand string literal folding audit

The scan covered all27,208 original JAR classes. The two classes with malformed annotations were scanned from the annotation-fixed copies; these replace failed parses, not two additional classes. No source files were changed.

## Exact field writes and initialization

All92 audited owner.field pairs have exactly one PUTSTATIC instruction each. Every write is inside its own holder. No field has multiple write sites. The92 fields are public static String fields (access9), not final constants, so this conclusion concerns the observed program references rather than Java access restrictions.

There are47 holders and64 writer methods. Every writer has exactly one direct callsite, in its own holder's <clinit>; none has a method handle. Holder <clinit> methods contain only INVOKESTATIC and RETURN. Writer bodies contain only LDC, INVOKESTATIC, PUTSTATIC, RETURN; every call is to the inspected string decoder. Thus there are no branch-dependent values, settings, I/O or other holder initialization effects to preserve when removing these GETSTATIC reads from the recovered renderer.

No setter field handles exist for these92 fields. All handle constants reachable through LDC, invokedynamic bootstrap arguments and nested ConstantDynamic arguments were checked. No writer exists outside the declared holder initializer helpers.

## Decoder and limits

The decoder owner is wexside/teGM0Rro3NgL0yQBhCP28UZ8n. Its <clinit> installs the16-byte key, subtractor88, rotation5 and initial feedback130. Method eja2ziBWDI(String) performs Base64 decode, feedback XOR, 8-bit rotate, index/subtractor subtraction, key XOR, then skips4 header bytes and constructs big-endian UTF-16 characters. Its only global effect is String.intern; returned content is deterministic for this initialized key state. Original bci0..189 performs decoding/construction, bci189 calls intern. PrepareHand's byte-array decoder matches this arithmetic. An independent Python reconstruction using original bytecode char[] arithmetic matched all92 audit values directly from each original encrypted LDC; encoded and independently decoded values are retained per write. Every one of the92 decoded values is ASCII, so its UTF_16BE string construction has no malformed-surrogate divergence from the original char[] construction for these inputs.

The decoder also has a public key mutation method0Mrb20M4Mu(long), bci0..49: seeded Random overwrites the key array and the3 scalar parameters. It is not sound to call the decoder universally immutable or pure. However the full class scan finds no invocation or handle for that mutator. Key/state fields are private and referenced only by the decoder's own <clinit>, decode method and dormant mutator. No external code obtains the private key array in the inspected bytecode.

610 methods containing reflection/Unsafe/Lookup APIs were inventoried. None contains a target holder/field-name literal or target holder class literal. No targeted reflective writer was found. This static search does not prove absence of arbitrary dynamically computed reflection or native/JNI mutation; literal folding is justified for the retained bytecode initialization path, with that precise scope.

## Reproduction and evidence

StringWriteAudit.java scans the original JAR with ASM9 and records the exact writer instructions, field flags, holder initializer callsites and operations, handles, reflective methods, decoder operations and annotation fallback filenames in string-write-audit.json. string-decoder.javap.txt retains original bytecode offsets. The audit was compiled and executed with JDK25 using ASM9.10.1, ASM-tree9.10.1 and Gson2.13.2 from work/verification-deps; no target code or initializer was executed.
