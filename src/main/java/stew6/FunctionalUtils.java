package stew6;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * An utility for Java functional API in Stew.
 */
public final class FunctionalUtils {

    private FunctionalUtils() { // empty, forbidden
    }

    public static <T, R> List<R> mapAndToList(Iterable<T> it, Function<T, R> f) {
        Stream<T> stream = StreamSupport.stream(it.spliterator(), false);
        return stream.map(x -> f.apply(x)).collect(Collectors.toList());
    }

    public static <T, R> List<R> filterMapAndToList(Iterable<T> it, Function<T, Optional<R>> f) {
        Stream<T> stream = StreamSupport.stream(it.spliterator(), false);
        return stream.flatMap(x -> optionalToStream(f.apply(x))).collect(Collectors.toList());
    }

    private static <T> Stream<T> optionalToStream(Optional<T> opt) {
        return opt.isPresent() ? Stream.of(opt.get()) : Stream.of();
    }

}
