package edu.cmu.sphinx.decoder.search;

abstract public class TokenSearchManager implements SearchManager {

  /**
   * The property that specifies whether to build a word lattice. (defaultValue = true)
   */
  private final boolean buildWordLattice;
  /**
   * The property that controls whether or not we keep all tokens. If this is
   * set to false, only word tokens are retained, otherwise all tokens are
   * retained.
   * (defaultValue = false)
   */
  private final boolean keepAllTokens;

  public TokenSearchManager(boolean buildWordLattice, boolean keepAllTokens) {
    this.buildWordLattice = buildWordLattice;
    this.keepAllTokens = keepAllTokens;
  }

  /**
   * Find the token to use as a predecessor in resultList given a candidate
   * predecessor. There are three cases here:
   *
   * <ul>
   * <li>We want to store everything in resultList. In that case
   * {@link #keepAllTokens} is set to true and we just store everything that
   * was built before.
   * <li>We are only interested in sequence of words. In this case we just
   * keep word tokens and ignore everything else. In this case timing and
   * scoring information is lost since we keep scores in emitting tokens.
   * <li>We want to keep words but we want to keep scores to build a lattice
   * from the result list later and {@link #buildWordLattice} is set to true.
   * In this case we want to insert intermediate token to store the score and
   * this token will be used during lattice path collapse to get score on
   * edge. See {@code edu.cmu.sphinx.result.Lattice} for details of resultList
   * compression.
   * </ul>
   *
   * @param token the token of interest
   * @return the immediate successor word token
   */
  public static Token getPredecessorToken(final Token token, boolean keepAllTokens, boolean buildWordLattice) {
    if (keepAllTokens) {
      return token;
    }

    if (!buildWordLattice) {
      if (token.isWord()) {
        return token;
      } else {
        return token.getPredecessor();
      }
    }

    float logAcousticScore = 0.0f;
    float logLanguageScore = 0.0f;
    float logInsertionScore = 0.0f;

    Token predecessor = token;
    while (predecessor != null && !predecessor.isWord()) {
      logAcousticScore += predecessor.getAcousticScore();
      logLanguageScore += predecessor.getLanguageScore();
      logInsertionScore += predecessor.getInsertionScore();
      predecessor = predecessor.getPredecessor();
    }

    return new Token(predecessor, predecessor != null ? predecessor.getScore() : token.getScore(), logInsertionScore, logAcousticScore, logLanguageScore);
  }

  /**
   * Find the token to use as a predecessor in resultList given a candidate
   * predecessor. There are three cases here:
   *
   * <ul>
   * <li>We want to store everything in resultList. In that case
   * {@link #keepAllTokens} is set to true and we just store everything that
   * was built before.
   * <li>We are only interested in sequence of words. In this case we just
   * keep word tokens and ignore everything else. In this case timing and
   * scoring information is lost since we keep scores in emitting tokens.
   * <li>We want to keep words but we want to keep scores to build a lattice
   * from the result list later and {@link #buildWordLattice} is set to true.
   * In this case we want to insert intermediate token to store the score and
   * this token will be used during lattice path collapse to get score on
   * edge. See {@code edu.cmu.sphinx.result.Lattice} for details of resultList
   * compression.
   * </ul>
   *
   * @param token the token of interest
   * @return the immediate successor word token
   */
  protected Token getResultListPredecessor(Token token) {
    return getPredecessorToken(token, keepAllTokens, buildWordLattice);
  }

}
