package com.dci.intellij.dbn.common.util;

import java.util.Collection;
import java.util.Map;

public class CollectionUtil {
    public static <T extends Cloneable<T>> void cloneCollectionElements(Collection<T> source, Collection<T> target) {
        for (T cloneable : source) {
            T clone = cloneable.clone();
            target.add(clone);
        }
    }

    public static void clearCollection(Collection collection) {
        if (collection != null) {
            collection.clear();
        }
    }

    public static void clearMap(Map map) {
        if (map != null) {
            map.clear();
        }
    }

}
