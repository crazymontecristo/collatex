package eu.interedition.collatex.collation;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.sd_editions.collatex.permutations.MatchSequence;

public class MatchNonMatch {

  private final List<MatchSequence> matchSequencesForBase;
  private final List<MatchSequence> matchSequencesForWitness;
  private final List<NonMatch> nonMatches;
  private final Set<Match> matches;

  public MatchNonMatch(Set<Match> _matches, List<MatchSequence> _matchSequencesForBase, List<MatchSequence> _matchSequencesForWitness, List<NonMatch> _nonMatches) {
    super();
    this.matches = _matches;
    this.matchSequencesForBase = _matchSequencesForBase;
    this.matchSequencesForWitness = _matchSequencesForWitness;
    this.nonMatches = _nonMatches;
  }

  public Set<Match> getMatches() {
    return matches;
  }

  public List<MatchSequence> getMatchSequencesForBase() {
    return matchSequencesForBase;
  }

  public List<MatchSequence> getMatchSequencesForWitness() {
    return matchSequencesForWitness;
  }

  public List<NonMatch> getNonMatches() {
    return nonMatches;
  }

  public double getVariationMeasure() {
    return 1000.0 * (matchSequencesForBase.size() - 1) + 10.0 * nonMatches.size() + getWordDistanceSum();
  }

  public float getWordDistanceSum() {
    float wordDistanceSum = 0f;
    for (MatchSequence matchSequence : matchSequencesForBase)
      for (Match match : matchSequence.getMatches())
        wordDistanceSum += match.wordDistance;
    return wordDistanceSum;
  }

  public List<NonMatch> getReplacements() {
    List<NonMatch> replacements = Lists.newArrayList();
    for (NonMatch nonMatch : getNonMatches()) {
      if (nonMatch.isReplacement()) {
        replacements.add(nonMatch);
      }
    }
    return replacements;
  }

  public List<NonMatch> getAdditions() {
    List<NonMatch> additions = Lists.newArrayList();
    for (NonMatch nonMatch : nonMatches) {
      if (nonMatch.isAddition()) {
        additions.add(nonMatch);
      }
    }
    return additions;
  }
}