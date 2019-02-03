package eu.unitn.disi.db.excode.densesub;

import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.set.hash.HashIntSet;

/**
 *
 * @author bluecopper
 * @param <B>
 */
public class MetaData<B> {
    
    public final HashIntObjMap<HashIntSet> nodeEdgeMap;
    public final B snapInfo;
    
    public MetaData(HashIntObjMap<HashIntSet> nodeEdgeMap, B snapInfo) {
        this.nodeEdgeMap = nodeEdgeMap;
        this.snapInfo = snapInfo;
    }
    
}
