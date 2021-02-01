/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.TimerPool;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the breadth first search. To perform recognition an application should call initialize before recognition
 * begins, and repeatedly call <code> recognize </code> until Result.isFinal() returns true. Once a final result has
 * been obtained, <code> terminate </code> should be called.
 * <p>
 * All scores and probabilities are maintained in the log math log domain.
 * <p>
 * For information about breadth first search please refer to "Spoken Language Processing", X. Huang, PTR
 */
public class SimpleBreadthFirstSearchManager extends TokenSearchManager {

    /**
     * The property that defines the name of the linguist to be used by this search manager.
     */
    private final Pruner pruner; // used to prune the active list

    /**
     * The property that defines the name of the scorer to be used by this search manager.
     */
    private final AcousticScorer scorer; // used to score the active list
    private final Logger logger;
    private final String name;
    /**
     * The property that controls whether or not relative beam pruning will be performed on the entry into a
     * state.
     * (defaultValue = false)
     */
    private final boolean wantEntryPruning;
    /**
     * The property that sets the minimum score relative to the maximum score in the word list for pruning. Words with a
     * score less than relativeBeamWidth * maximumScore will be pruned from the list
     * (defaultValue = 0.0)
     */
    private final float logRelativeWordBeamWidth;
    /**
     * The property that controls the number of frames processed for every time the decode growth step is skipped.
     * Setting this property to zero disables grow skipping. Setting this number to a small integer will increase the
     * speed of the decoder but will also decrease its accuracy. The higher the number, the less often the grow code is
     * skipped.
     * defaultValue = 0
     */
    private final int growSkipInterval;
    /**
     * The property that defines the name of the linguist to be used by this search manager.
     * Provides grammar/language info
     */
    private final Linguist linguist;
    /**
     * The property that defines the name of the active list factory to be used by this search manager.
     */
    private final ActiveListFactory activeListFactory;

    // ------------------------------------
    // monitoring data
    // ------------------------------------

    private Timer scoreTimer; // TODO move these timers out
    private Timer pruneTimer;
    private int currentFrameNumber; // the current frame number
    private StatisticsVariable totalTokensScored;
    private StatisticsVariable tokensPerSecond;
    private StatisticsVariable curTokensScored;
    private StatisticsVariable tokensCreated;
    private StatisticsVariable viterbiPruned;
    private StatisticsVariable beamPruned;

    // ------------------------------------
    // Working data
    // ------------------------------------
    private long currentCollectTime; // the current frame number
    private ActiveList activeList; // the list of active tokens
    private List<Token> resultList; // the current set of results
    private int totalHmms;
    private double startTime;
    private float threshold;
    private float wordThreshold;
    private Timer growTimer;
    private Map<SearchState, Token> bestTokenMap;
    private boolean streamEnd;

    /**
     * Creates a manager for simple search
     *
     * @param linguist linguist to configure search space
     * @param pruner pruner to prune extra paths
     * @param scorer scorer to estimate token probability
     * @param activeListFactory factory for list of tokens
     * @param relativeWordBeamWidth relative pruning beam for lookahead
     * @param growSkipInterval interval to skip growth step
     * @param wantEntryPruning entry pruning
     */
    public SimpleBreadthFirstSearchManager(Linguist linguist, Pruner pruner, AcousticScorer scorer, ActiveListFactory activeListFactory, double relativeWordBeamWidth, int growSkipInterval,
        boolean wantEntryPruning) {
        this(linguist, pruner, scorer, activeListFactory, relativeWordBeamWidth, growSkipInterval, wantEntryPruning, true, true);
    }

    /**
     * Creates a manager for simple search
     *
     * @param linguist linguist to configure search space
     * @param pruner pruner to prune extra paths
     * @param scorer scorer to estimate token probability
     * @param activeListFactory factory for list of tokens
     * @param relativeWordBeamWidth relative pruning beam for lookahead
     * @param growSkipInterval interval to skip growth step
     * @param wantEntryPruning entry pruning
     * @param buildWordLattice The property that specifies whether to build a word lattice. (defaultValue = true)
     * @param keepAllTokens The property that controls whether or not we keep all tokens. If this is set to false, only word tokens are retained, otherwise all tokens are retained. (defaultValue = true)
     */
    public SimpleBreadthFirstSearchManager(Linguist linguist, Pruner pruner, AcousticScorer scorer, ActiveListFactory activeListFactory, double relativeWordBeamWidth, int growSkipInterval,
        boolean wantEntryPruning, boolean buildWordLattice, boolean keepAllTokens) {
        super(buildWordLattice, keepAllTokens);
        this.name = getClass().getName();
        this.logger = Logger.getLogger(name);
        LogMath logMath = LogMath.getLogMath();
        this.linguist = linguist;
        this.pruner = pruner;
        this.scorer = scorer;
        this.activeListFactory = activeListFactory;
        this.growSkipInterval = growSkipInterval;
        this.wantEntryPruning = wantEntryPruning;
        this.logRelativeWordBeamWidth = logMath.linearToLog(relativeWordBeamWidth);
    }


    /**
     * Called at the start of recognition. Gets the search manager ready to recognize
     */
    public void startRecognition() {
        logger.finer("starting recognition");

        linguist.startRecognition();
        pruner.startRecognition();
        scorer.startRecognition();
        localStart();
        if (startTime == 0.0) {
            startTime = System.currentTimeMillis();
        }
    }


    /**
     * Performs the recognition for the given number of frames.
     *
     * @param nFrames the number of frames to recognize
     * @return the current result or null if there is no Result (due to the lack of frames to recognize)
     */
    public Result recognize(int nFrames) {
        boolean done = false;
        Result result = null;
        streamEnd = false;

        for (int i = 0; i < nFrames && !done; i++) {
            done = recognize();
        }

        // generate a new temporary result if the current token is based on a final search state
        // remark: the first check for not null is necessary in cases that the search space does not contain scoreable tokens.
        if (activeList.getBestToken() != null) {
            // to make the current result as correct as possible we undo the last search graph expansion here
            ActiveList fixedList = undoLastGrowStep();

            // Now create the result using the fixed active-list.
            if (!streamEnd)
                result =
                    new Result(fixedList, resultList, currentFrameNumber, done, linguist.getSearchGraph().getWordTokenFirst());
        }

        return result;
    }


    /**
     * Because the growBranches() is called although no data is left after the last speech frame, the ordering of the
     * active-list might depend on the transition probabilities and (penalty-scores) only. Therefore we need to undo the last
     * grow-step up to final states or the last emitting state in order to fix the list.
     *
     * @return newly created list
     */
    private ActiveList undoLastGrowStep() {
        ActiveList fixedList = activeList.newInstance();

        for (Token token : activeList) {
            Token curToken = token.getPredecessor();

            // remove the final states that are not the real final ones because they're just hide prior final tokens:
            while (curToken.getPredecessor() != null && (
                (curToken.isFinal() && curToken.getPredecessor() != null && !curToken.getPredecessor().isFinal())
                    || (curToken.isEmitting() && curToken.getData() == null) // the so long not scored tokens
                    || (!curToken.isFinal() && !curToken.isEmitting()))) {
                curToken = curToken.getPredecessor();
            }

            fixedList.add(curToken);
        }

        return fixedList;
    }


    /**
     * Terminates a recognition
     */
    public void stopRecognition() {
        localStop();
        scorer.stopRecognition();
        pruner.stopRecognition();
        linguist.stopRecognition();

        logger.finer("recognition stopped");
    }


    /**
     * Performs recognition for one frame. Returns true if recognition has been completed.
     *
     * @return <code>true</code> if recognition is completed.
     */
    private boolean recognize() {
        boolean more = scoreTokens(); // score emitting tokens
        if (more) {
            pruneBranches(); // eliminate poor branches
            currentFrameNumber++;
            if (growSkipInterval == 0 || (currentFrameNumber % growSkipInterval) != 0) {
                growBranches(); // extend remaining branches
            }
        }
        return !more;
    }


    /**
     * Gets the initial grammar node from the linguist and creates a GrammarNodeToken
     */
    private void localStart() {
        currentFrameNumber = 0;
        curTokensScored.value = 0;
        ActiveList newActiveList = activeListFactory.newInstance();
        SearchState state = linguist.getSearchGraph().getInitialState();
        newActiveList.add(new Token(state, -1));
        activeList = newActiveList;

        growBranches();
    }


    /**
     * Local cleanup for this search manager
     */
    private void localStop() {
    }


    /**
     * Goes through the active list of tokens and expands each token, finding the set of successor tokens until all the
     * successor tokens are emitting tokens.
     */
    private void growBranches() {
        int mapSize = activeList.size() * 10;
        if (mapSize == 0) {
            mapSize = 1;
        }
        growTimer.start();
        bestTokenMap = new HashMap<>(mapSize);
        ActiveList oldActiveList = activeList;
        resultList = new LinkedList<>();
        activeList = activeListFactory.newInstance();
        threshold = oldActiveList.getBeamThreshold();
        wordThreshold = oldActiveList.getBestScore() + logRelativeWordBeamWidth;

        for (Token token : oldActiveList) {
            collectSuccessorTokens(token);
        }
        growTimer.stop();
        if (logger.isLoggable(Level.FINE)) {
            int hmms = activeList.size();
            totalHmms += hmms;
            logger.fine("Frame: " + currentFrameNumber + " Hmms: "
                + hmms + "  total " + totalHmms);
        }
    }


    /**
     * Calculate the acoustic scores for the active list. The active list should contain only emitting tokens.
     *
     * @return <code>true</code> if there are more frames to score, otherwise, false
     */
    private boolean scoreTokens() {
        boolean hasMoreFrames = false;

        scoreTimer.start();
        Data data = scorer.calculateScores(activeList.getTokens());
        scoreTimer.stop();

        Token bestToken = null;
        if (data instanceof Token) {
            bestToken = (Token) data;
        } else if (data == null) {
            streamEnd = true;
        }

        if (bestToken != null) {
            hasMoreFrames = true;
            currentCollectTime = bestToken.getCollectTime();
            activeList.setBestToken(bestToken);
        }

        // update statistics
        curTokensScored.value += activeList.size();
        totalTokensScored.value += activeList.size();
        tokensPerSecond.value = totalTokensScored.value / getTotalTime();

//        if (logger.isLoggable(Level.FINE)) {
//            logger.fine(currentFrameNumber + " " + activeList.size()
//                    + " " + curTokensScored.value + " "
//                    + (int) tokensPerSecond.value);
//        }

        return hasMoreFrames;
    }


    /**
     * Returns the total time since we start4ed
     *
     * @return the total time (in seconds)
     */
    private double getTotalTime() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }


    /**
     * Removes unpromising branches from the active list
     */
    private void pruneBranches() {
        int startSize = activeList.size();
        pruneTimer.start();
        activeList = pruner.prune(activeList);
        beamPruned.value += startSize - activeList.size();
        pruneTimer.stop();
    }


    /**
     * Gets the best token for this state
     *
     * @param state the state of interest
     * @return the best token
     */
    private Token getBestToken(SearchState state) {
        Token best = bestTokenMap.get(state);
        if (logger.isLoggable(Level.FINER) && best != null) {
            logger.finer("BT " + best + " for state " + state);
        }
        return best;
    }


    /**
     * Sets the best token for a given state
     *
     * @param token the best token
     * @param state the state
     * @return the previous best token for the given state, or null if no previous best token
     */
    private Token setBestToken(Token token, SearchState state) {
        return bestTokenMap.put(state, token);
    }

    public ActiveList getActiveList() {
        return activeList;
    }

    /**
     * Collects the next set of emitting tokens from a token and accumulates them in the active or result lists
     *
     * @param token the token to collect successors from
     */
    private void collectSuccessorTokens(Token token) {
        SearchState state = token.getSearchState();
        // If this is a final state, add it to the final list
        if (token.isFinal()) {
            resultList.add(token);
        }
        if (token.getScore() < threshold) {
            return;
        }
        if (state instanceof WordSearchState
            && token.getScore() < wordThreshold) {
            return;
        }
        SearchStateArc[] arcs = state.getSuccessors();
        // For each successor
        // calculate the entry score for the token based upon the
        // predecessor token score and the transition probabilities
        // if the score is better than the best score encountered for
        // the SearchState and frame then create a new token, add
        // it to the lattice and the SearchState.
        // If the token is an emitting token add it to the list,
        // otherwise recursively collect the new tokens successors.
        for (SearchStateArc arc : arcs) {
            SearchState nextState = arc.getState();
            // We're actually multiplying the variables, but since
            // these come in log(), multiply gets converted to add
            float logEntryScore = token.getScore() + arc.getProbability();
            if (wantEntryPruning) { // false by default
                if (logEntryScore < threshold) {
                    continue;
                }
                if (nextState instanceof WordSearchState
                    && logEntryScore < wordThreshold) {
                    continue;
                }
            }
            Token predecessor = super.getResultListPredecessor(token);

            // if not emitting, check to see if we've already visited
            // this state during this frame. Expand the token only if we
            // haven't visited it already. This prevents the search
            // from getting stuck in a loop of states with no
            // intervening emitting nodes. This can happen with nasty
            // jsgf grammars such as ((foo*)*)*
            if (!nextState.isEmitting()) {
                Token newToken = new Token(predecessor, nextState, logEntryScore,
                    arc.getInsertionProbability(),
                    arc.getLanguageProbability(),
                    currentCollectTime);
                tokensCreated.value++;
                if (!isVisited(newToken)) {
                    collectSuccessorTokens(newToken);
                }
                continue;
            }

            Token bestToken = getBestToken(nextState);
            if (bestToken == null) {
                Token newToken = new Token(predecessor, nextState, logEntryScore,
                    arc.getInsertionProbability(),
                    arc.getLanguageProbability(),
                    currentFrameNumber);
                tokensCreated.value++;
                setBestToken(newToken, nextState);
                activeList.add(newToken);
            } else {
                if (bestToken.getScore() <= logEntryScore) {
                    bestToken.update(predecessor, nextState, logEntryScore,
                        arc.getInsertionProbability(),
                        arc.getLanguageProbability(),
                        currentCollectTime);
                    viterbiPruned.value++;
                } else {
                    viterbiPruned.value++;
                }
            }
        }
    }


    /**
     * Determines whether or not we've visited the state associated with this token since the previous frame.
     *
     * @param t the token to check
     * @return true if we've visited the search state since the last frame
     */
    private boolean isVisited(Token t) {
        SearchState curState = t.getSearchState();

        t = t.getPredecessor();

        while (t != null && !t.isEmitting()) {
            if (curState.equals(t.getSearchState())) {
                return true;
            }
            t = t.getPredecessor();
        }
        return false;
    }


    /**
     * Returns the best token map.
     *
     * @return the best token map
     */
    private Map<SearchState, Token> getBestTokenMap() {
        return bestTokenMap;
    }


    /**
     * Sets the best token Map.
     *
     * @param bestTokenMap the new best token Map
     */
    private void setBestTokenMap(Map<SearchState, Token> bestTokenMap) {
        this.bestTokenMap = bestTokenMap;
    }


    /**
     * Returns the result list.
     *
     * @return the result list
     */
    public List<Token> getResultList() {
        return resultList;
    }


    /**
     * Returns the current frame number.
     *
     * @return the current frame number
     */
    public int getCurrentFrameNumber() {
        return currentFrameNumber;
    }


    /**
     * Returns the Timer for growing.
     *
     * @return the Timer for growing
     */
    public Timer getGrowTimer() {
        return growTimer;
    }


    /**
     * Returns the tokensCreated StatisticsVariable.
     *
     * @return the tokensCreated StatisticsVariable.
     */
    public StatisticsVariable getTokensCreated() {
        return tokensCreated;
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.decoder.search.SearchManager#allocate()
     */
    public void allocate() {
        totalTokensScored = StatisticsVariable.getStatisticsVariable("totalTokensScored");
        tokensPerSecond = StatisticsVariable.getStatisticsVariable("tokensScoredPerSecond");
        curTokensScored = StatisticsVariable.getStatisticsVariable("curTokensScored");
        tokensCreated = StatisticsVariable.getStatisticsVariable("tokensCreated");
        viterbiPruned = StatisticsVariable.getStatisticsVariable("viterbiPruned");
        beamPruned = StatisticsVariable.getStatisticsVariable("beamPruned");

        try {
            linguist.allocate();
            pruner.allocate();
            scorer.allocate();
        } catch (IOException e) {
            throw new RuntimeException("Allocation of search manager resources failed", e);
        }

        scoreTimer = TimerPool.getTimer(this, "Score");
        pruneTimer = TimerPool.getTimer(this, "Prune");
        growTimer = TimerPool.getTimer(this, "Grow");
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.decoder.search.SearchManager#deallocate()
     */
    public void deallocate() {
        try {
            scorer.deallocate();
            pruner.deallocate();
            linguist.deallocate();
        } catch (IOException e) {
            throw new RuntimeException("Deallocation of search manager resources failed", e);
        }
    }


    @Override
    public String toString() {
        return name;
    }
}
