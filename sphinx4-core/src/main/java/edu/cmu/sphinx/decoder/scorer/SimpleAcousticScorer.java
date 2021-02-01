package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.ConfigurableAdapter;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements some basic scorer functionality, including a simple default
 * acoustic scoring implementation which scores within the current thread, that
 * can be changed by overriding the {@link #doScoring} method.
 *
 * <p>
 * Note that all scores are maintained in LogMath log base.
 *
 * @author Holger Brandl
 */
public class SimpleAcousticScorer extends ConfigurableAdapter implements AcousticScorer {

    /**
     * Property the defines the frontend to retrieve features from for scoring
     */
    private final BaseDataProcessor frontEnd;
    private final LinkedList<Data> storedData = new LinkedList<>();
    private boolean seenEnd = false;

    /**
     *
     * @param frontEnd Property the defines the frontend to retrieve features from for scoring
     */
    public SimpleAcousticScorer(BaseDataProcessor frontEnd) {
        this.frontEnd = frontEnd;
    }

    /**
     * Scores the given set of states.
     *
     * @param scoreableList A list containing scoreable objects to be scored
     * @return The best scoring scoreable, or <code>null</code> if there are no
     * more features to score
     */
    public Data calculateScores(List<? extends Scoreable> scoreableList) {
        Data data;
        if (storedData.isEmpty()) {
            while ((data = getNextData()) instanceof Signal) {
                if (data instanceof SpeechEndSignal) {
                    seenEnd = true;
                    break;
                }
                if (data instanceof DataEndSignal) {
                    if (seenEnd)
                        return null;
                    else
                        break;
                }
            }
            if (data == null)
                return null;
        } else {
            data = storedData.poll();
        }

        return calculateScoresForData(scoreableList, data);
    }

    protected Data calculateScoresForData(List<? extends Scoreable> scoreableList, Data data) {
        if (data instanceof SpeechEndSignal || data instanceof DataEndSignal) {
            return data;
        }

        if (scoreableList.isEmpty())
            return null;

        // convert the data to FloatData if not yet done
        if (data instanceof DoubleData)
            data = DataUtil.DoubleData2FloatData((DoubleData) data);

        return doScoring(scoreableList, data);
    }

    protected Data getNextData() {
        return frontEnd.getData();
    }

    public void startRecognition() {
        storedData.clear();
    }

    public void stopRecognition() {
        // nothing needs to be done here
    }

    /**
     * Scores a a list of <code>Scoreable</code>s given a <code>Data</code>
     * -object.
     *
     * @param scoreableList The list of Scoreables to be scored
     * @param data The <code>Data</code>-object to be used for scoring.
     * @param <T> type for scorables
     * @return the best scoring <code>Scoreable</code> or <code>null</code> if
     * the list of scoreables was empty.
     */
    protected <T extends Scoreable> T doScoring(List<T> scoreableList, Data data) {

        T best = null;
        float bestScore = -Float.MAX_VALUE;

        for (T item : scoreableList) {
            item.calculateScore(data);
            if (item.getScore() > bestScore) {
                bestScore = item.getScore();
                best = item;
            }
        }
        return best;
    }

    // Even if we don't do any meaningful allocation here, we implement the
    // methods because most extending scorers do need them either.

    public void allocate() {
    }

    public void deallocate() {
    }

}
