package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneSequence;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AllphoneLinguist implements Linguist {

    /**
     * The property that defines the acoustic model to use when building the search graph
     */
    private final AcousticModel acousticModel;
    private ArrayList<HMM> ciHMMs;
    private ArrayList<HMM> fillerHMMs;
    private ArrayList<HMM> leftContextSilHMMs;
    private HashMap<SenoneSequence, ArrayList<Unit>> senonesToUnits;
    private HashMap<Unit, HashMap<Unit, ArrayList<HMM>>> cdHMMs;

    /**
     * The property that controls phone insertion probability.
     * Default value for context independent phoneme decoding is 0.05,
     * while for context dependent - 0.01.
     *
     * (defaultValue = 0.05)
     */
    private final float phoneInsertionProbability;

    /**
     * The property that controls whether to use context dependent phones.
     * Changing it for true, don't forget to tune phone insertion probability.
     * (defaultValue = false)
     */
    private final boolean useContextDependentPhones;

    /**
     * @param phoneInsertionProbability The property that controls phone insertion probability. Default value for context independent phoneme decoding is 0.05, while for context dependent - 0.01.
     * @param useContextDependentPhones The property that controls whether to use context dependent phones. Changing it for true, don't forget to tune phone insertion probability. (defaultValue = false)
     */
    public AllphoneLinguist(AcousticModel acousticModel, float phoneInsertionProbability, boolean useContextDependentPhones) {
        this.acousticModel = acousticModel;
        this.phoneInsertionProbability = phoneInsertionProbability;
        this.useContextDependentPhones = useContextDependentPhones;
    }

    public SearchGraph getSearchGraph() {
        return new AllphoneSearchGraph(this);
    }

    public void startRecognition() {
    }

    public void stopRecognition() {
    }

    public void allocate() throws IOException {
        acousticModel.allocate();
        createSuccessors(useContextDependentPhones);
    }

    public void deallocate() throws IOException {
        acousticModel.deallocate();
    }

    private void createSuccessors(boolean useCD) {
        if (useCD)
            createContextDependentSuccessors();
        else
            createContextIndependentSuccessors();
    }

    public AcousticModel getAcousticModel() {
        return acousticModel;
    }

    public float getPhoneInsertionProb() {
        return phoneInsertionProbability;
    }

    public boolean useContextDependentPhones() {
        return useContextDependentPhones;
    }

    public ArrayList<HMM> getCISuccessors() {
        return ciHMMs;
    }

    public ArrayList<HMM> getCDSuccessors(Unit lc, Unit base) {
        if (lc.isFiller())
            return leftContextSilHMMs;
        if (base == UnitManager.SILENCE)
            return fillerHMMs;
        return cdHMMs.get(lc).get(base);
    }

    public ArrayList<Unit> getUnits(SenoneSequence senoneSeq) {
        return senonesToUnits.get(senoneSeq);
    }

    private void createContextIndependentSuccessors() {
        Iterator<HMM> hmmIter = acousticModel.getHMMIterator();
        ciHMMs = new ArrayList<>();
        senonesToUnits = new HashMap<>();
        while (hmmIter.hasNext()) {
            HMM hmm = hmmIter.next();
            if (!hmm.getUnit().isContextDependent()) {
                ArrayList<Unit> sameSenonesUnits;
                SenoneSequence senoneSeq = ((SenoneHMM) hmm).getSenoneSequence();
                if ((sameSenonesUnits = senonesToUnits.get(senoneSeq)) == null) {
                    sameSenonesUnits = new ArrayList<>();
                    senonesToUnits.put(senoneSeq, sameSenonesUnits);
                }
                sameSenonesUnits.add(hmm.getUnit());
                ciHMMs.add(hmm);
            }
        }
    }

    private void createContextDependentSuccessors() {
        cdHMMs = new HashMap<>();
        senonesToUnits = new HashMap<>();
        fillerHMMs = new ArrayList<>();
        leftContextSilHMMs = new ArrayList<>();
        Iterator<HMM> hmmIter = acousticModel.getHMMIterator();
        while (hmmIter.hasNext()) {
            HMM hmm = hmmIter.next();
            ArrayList<Unit> sameSenonesUnits;
            SenoneSequence senoneSeq = ((SenoneHMM) hmm).getSenoneSequence();
            if ((sameSenonesUnits = senonesToUnits.get(senoneSeq)) == null) {
                sameSenonesUnits = new ArrayList<>();
                senonesToUnits.put(senoneSeq, sameSenonesUnits);
            }
            sameSenonesUnits.add(hmm.getUnit());
            if (hmm.getUnit().isFiller()) {
                fillerHMMs.add(hmm);
                continue;
            }
            if (hmm.getUnit().isContextDependent()) {
                LeftRightContext context = (LeftRightContext) hmm.getUnit().getContext();
                Unit lc = context.getLeftContext()[0];
                if (lc == UnitManager.SILENCE) {
                    leftContextSilHMMs.add(hmm);
                    continue;
                }
                Unit base = hmm.getUnit().getBaseUnit();
                HashMap<Unit, ArrayList<HMM>> lcSuccessors;
                if ((lcSuccessors = cdHMMs.get(lc)) == null) {
                    lcSuccessors = new HashMap<>();
                    cdHMMs.put(lc, lcSuccessors);
                }
                ArrayList<HMM> lcBaseSuccessors;
                if ((lcBaseSuccessors = lcSuccessors.get(base)) == null) {
                    lcBaseSuccessors = new ArrayList<>();
                    lcSuccessors.put(base, lcBaseSuccessors);
                }
                lcBaseSuccessors.add(hmm);
            }
        }
        leftContextSilHMMs.addAll(fillerHMMs);
    }

}
