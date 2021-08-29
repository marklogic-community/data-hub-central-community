/**
 * Given an Envision model , create Entity Services
 * entities for each entity in the model.
 *
 */

const model = require('/envision/model.sjs');
const m = model.enhancedModel();
model.modelToES(m);
