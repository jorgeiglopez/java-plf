package plf.s05_collections.s05_07_immutable_and_sequenced;

import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

public final class CollectionLens {

    private CollectionLens() {
    }

    public static SequencedCollection<String> audit(List<String> input) {
        return Collections.unmodifiableList(input);   // <-- Task 3: is this a snapshot?
    }
}
