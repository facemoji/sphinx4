package edu.cmu.sphinx.linguist.acoustic.tiedstate;

enum ModelFile {
  MEANS("means"),
  VARIANCES("variances"),
  MIXTURE_WEIGHTS("mixture_weights"),
  TRANSITION_MATRICES("transition_matrices"),
  FEATURE_TRANSFORM("feature_transform"),
  FEAT_PARAMS("feat.params"),
  MDEF("mdef");

  final String filename;

  ModelFile(String filename) {
    this.filename = filename;
  }
}

public interface ModelData {

  byte[] get(ModelFile modelFile);

}