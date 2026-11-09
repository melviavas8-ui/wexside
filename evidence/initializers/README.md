# Initializer recovery boundary

Exact input SHA-256: `340b0c8f77e020807ee994e3c9e8877950221c87ecf58ac900d4719d8c25a747`.

AutoWarden and CrystalAura are **not wholly unavailable**. Their feature methods, real event-registration callbacks, helper constructors and runtime constant tables survive. AutoWarden constructor allocates its ArrayDeque before super; CrystalAura calls only super. The superclass invokes virtual register. Constructor shape alone was not used as an absence proof.

Raw ASM inspection covered27208classes,0holes, plus571nested mixinextras entries and every nonclass resource/native blob. It found136target field-write instructions but **zero writes or setter handles to their10setting-reference fields**; zero external target writes; zero orphan target CP strings/member refs after including all BSM handles. Target CP contains no setting-construction/default metadata. Input configs contain only voicechat files; alternate source has no matching feature types. User confirms no other artifact exists.

Recovered exact AutoWarden mode labels/order: `LOW=Низкий`, `MEDIUM=Средний`, `HIGH=Высокий`. They were decoded offline from retained condy bootstrap arguments, without initializing target classes. Its mode getter maps the setting string to this enum. Selected/default mode is not established.

Helper constructor inventory is intact: `helper-constructors.json`. CrystalAura rotation helper has a real no-arg constructor. AutoWarden filter takes Supplier, drops takes(filter,session), route takes(filter), event trackers take EventBus, controllers take(shared helper,helper,BooleanSupplier). Existing consumers permit graph reconstruction; original ambiguous supplier/association choices still require tracing. Arbitrary null/zero/no-op factory callbacks are not recovery evidence.

Narrow missing-data boundary: original initial/default values, original id/caption/description, toggle/keybind/visibility metadata and registration order for AutoWarden's4Boolean+Text+Mode settings and CrystalAura's4Boolean settings remain unestablished. Java-null setting references do not imply false option defaults. AutoWarden's Text initial string/max length and Mode selected/default option are unknown. Fixed runtime numerical constants and feature bodies are present and should continue through offline decoding/porting.

Detailed proof: `initializer-recovery-analysis.md`. Machine summary: `summary.json`. Exact field status: `reference-field-status.json`. Full opcode/descriptor/handle/CP evidence: `raw-target-references.jsonl`. Raw pools/decrypted strings: `target-constant-pools.json`, `*-decoded-strings.json`. Mode decode: `warden-mode-labels.json`, `decode_warden_modes.py`, `warden-loot-levels.verbose.txt`, `condy-bootstrap.javap.txt`. Reproducible scanners: `InitializerAudit.java`, `constant_pool_audit.py`.

Verdict: retained behavior and helper graph are recoverable work, not a blanket external blocker. Exact original setting/default equivalence for the ten listed fields remains INCONCLUSIVE after these artifact checks. No staging source was edited by this audit.
