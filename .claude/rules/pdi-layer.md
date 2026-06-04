---
paths:
  - "src/main/java/**/NachaReader*.java"
---

# PDI Integration Layer

- `NachaReaderStepMeta.getFields()` branches on `outputMode`: ENTRY_DETAILS → 28 fields, ALL_RECORDS → 41 fields
- `addField()` must declare `throws KettleStepException` because `RowMetaInterface.addValueMeta()` throws it
- `NachaReaderStep.processRow()` returns `true` after processing one file (PDI calls again for next input row); only returns `false` when `getRow()` returns null
- Use `RowDataUtil.resizeArray(r, outputRowMeta.size())` to copy input fields and allocate NACHA output slots — do NOT mutate the input row array `r` directly
- `environmentSubstitute()` must be called on the file path string to resolve PDI variables
- `filePathFieldIndex` is resolved once on `first == true` and cached in `NachaReaderStepData`
- Dialog: `stepMeta` field does not exist in the stub; find the StepMeta by name via `new StepMeta(); sm.setName(stepname)` when calling `transMeta.getPrevStepFields()`
