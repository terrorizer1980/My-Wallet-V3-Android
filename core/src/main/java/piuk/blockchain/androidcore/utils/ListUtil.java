package piuk.blockchain.androidcore.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class ListUtil {

    public static <E> void addAllIfNotNull(@NonNull List<E> list,
                                           @Nullable Collection<? extends E> collection) {
        if (collection != null) {
            list.addAll(collection);
        }
    }

}
