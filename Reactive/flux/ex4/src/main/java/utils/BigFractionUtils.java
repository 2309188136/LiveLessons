package utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * A utility class containing helpful methods for manipulating various
 * BigFraction features.
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class BigFractionUtils {
    /**
     * A utility class should always define a private constructor.
     */
    private BigFractionUtils() {
    }

    /**
     * Number of big fractions to process asynchronously in a Reactor
     * flux stream.
     */
    public static final int sMAX_FRACTIONS = 10;

    /**
     * These final strings are used to pass params to various lambdas in the
     * test methods below.
     */
    public static final String sBI1 = "846122553600669882";
    public static final String sBI2 = "188027234133482196";

    /**
     * Represents a test that's completed running when it returns.
     */
    public static final Mono<Void> sVoidM =
            Mono.empty();

    /**
     * A big reduced fraction constant.
     */
    public static final BigFraction sBigReducedFraction =
            BigFraction.valueOf(new BigInteger(sBI1),
                    new BigInteger(sBI2),
                    true);

    /**
     * A factory method that returns a large random BigFraction whose
     * creation is performed synchronously.
     *
     * @param random A random number generator
     * @param reduced A flag indicating whether to reduce the fraction or not
     * @return A large random BigFraction
     */
    public static BigFraction makeBigFraction(Random random,
                                              boolean reduced) {
        // Create a large random big integer.
        BigInteger numerator =
            new BigInteger(150000, random);

        // Create a denominator that's between 1 to 10 times smaller
        // than the numerator.
        BigInteger denominator =
            numerator.divide(BigInteger.valueOf(random.nextInt(10) + 1));

        // Return a big fraction.
        return BigFraction.valueOf(numerator,
                                   denominator,
                                   reduced);
    }

    /**
     * Display the {@code string} after prepending the thread id.
     */
    public static void display(String string) {
        System.out.println("["
                           + Thread.currentThread().getId()
                           + "] "
                           + string);
    }

    /**
     * Convert {@code unreducedFraction} to a mixed string and {@code
     * reducedFraction} to a mixed string and append it to the
     * contents of {@code stringBuilder}.
     */
    public static void logBigFraction(BigFraction unreducedFraction,
                                      BigFraction reducedFraction,
                                      StringBuffer sb) {
        sb.append("     "
                  + unreducedFraction.toMixedString()
                  + " x "
                  + reducedFraction.toMixedString()
                  + "\n");
    }

    /**
     * Convert {@code unreducedFraction} to a mixed string and {@code
     * reducedFraction} to a mixed string and append it to the
     * contents of {@code stringBuilder}.
     */
    public static void logBigFraction(BigFraction unreducedFraction,
                                      BigFraction reducedFraction,
                                      StringBuilder sb) {
        sb.append("     "
                + unreducedFraction.toMixedString()
                + " x "
                + reducedFraction.toMixedString()
                + "\n");
    }

    /**
     * Convert {@code bigFraction} to a mixed string, {@code
     * reducedFraction} to a mixed string, and {@code result} to a
     * mixed string and append it to the contents of {@code
     * stringBuilder}.
     */
    public static void logBigFractionResult(BigFraction bigFraction,
                                            BigFraction reducedFraction,
                                            BigFraction result,
                                            StringBuffer sb) {
        sb.append("     "
                  + bigFraction.toMixedString()
                  + " x "
                  + reducedFraction.toMixedString()
                  + " = "
                  + result.toMixedString()
                  + "\n");
    }

    /**
     * Display {@code bigFraction} and the contents of {@code stringBuffer}.
     */
    public static void displayMixedBigFraction(BigFraction bigFraction,
                                               StringBuffer stringBuffer) {
        stringBuffer.append("     Mixed BigFraction result = "
                            + bigFraction
                            + "\n");
        BigFractionUtils.display(stringBuffer.toString());
    }

    /**
     * This factory method returns a {@link Mono} that's signaled
     * after the {@code unreducedFraction} is reduced/multiplied
     * asynchronously in background threads from the given {@link
     * Scheduler}.
     *
     * @param unreducedFraction An unreduced {@link BigFraction}
     * @param scheduler         The {@link Scheduler} to perform the
     *                          computation in
     * @param sb                The {@link StringBuffer} to store logging messages
     * @return A {@link Mono<BigFraction>} that's signaled when the
     * asynchronous computation completes
     */
    public static Mono<BigFraction> reduceAndMultiplyFraction
        (BigFraction unreducedFraction,
         Scheduler scheduler,
         StringBuffer sb) {
        return Mono
            // Emit one item that performs the reduction.
            .fromSupplier(() -> BigFraction
                          .reduce(unreducedFraction))

            // Perform all processing asynchronously in the scheduler.
            .subscribeOn(scheduler)

            // Return a Mono to a multiplied BigFraction.
            .flatMap(reducedFraction ->
                     multiplyFraction(reducedFraction,
                                      sBigReducedFraction,
                                      scheduler,
                                      sb));
    }

    /**
     * @return A {@link Mono} that's signaled after the {@link
     * BigFraction} is multiplied asynchronously in a background
     * thread from the given {@link Scheduler}
     */
    public static Mono<BigFraction> multiplyFraction(BigFraction bf1,
                                                     BigFraction bf2,
                                                     Scheduler scheduler,
                                                     StringBuffer sb) {
        return Mono
            // Return a Mono to a multiplied big fraction.
            .fromSupplier(() -> bf1
                          // Multiply the big fractions
                          .multiply(bf2))

            // Perform processing asynchronously in a pool of
            // background threads.
            .subscribeOn(scheduler);
    }

    /**
     * This factory method creates a {@link BlockingSubscriber} with
     * the given parameters.
     *
     * @param sb The {@link StringBuffer} to record output
     * @param requestSize The size to pass to {@link Flow.Subscription#request}
     * @return A {@link BlockingSubscriber} that consumes events
     */
    public static BlockingSubscriber<BigFraction> makeBlockingSubscriber
        (StringBuffer sb,
         long requestSize,
         boolean log) {
        return new BlockingSubscriber<>
            (bf -> {
                // Conditionally append the results.
                if (log)
                    // Add fraction to the string buffer.
                    sb.append("Result = " + bf.toMixedString() + "\n");
            },
            t -> {
                // Display results when processing is done.
                BigFractionUtils.display(sb.toString());
            },
            () -> {
                // Display results when processing is done.
                BigFractionUtils.display(sb.toString());
            },

            // Value passed to Subscription#request.
            requestSize,

            // Pass the StringBuffer.
            sb);
    }

    /**
     * Sort the {@link List} in parallel using quicksort and heapsort
     * and then store and print the results in the {@link
     * StringBuffer} parameter.
     */
    public static Mono<Void> sortAndPrintList(List<BigFraction> list,
                                              StringBuffer sb) {
        // Quick sort the list asynchronously.
        var quickSortM = Mono
                // Use the fromCallable() factory method to obtain the
                // results of quick sorting the list.
                .fromCallable(() -> quickSort(list))

                // Use subscribeOn() to run all the processing in the
                // parallel thread pool.
                .subscribeOn(Schedulers.parallel());

        // Heap sort the list asynchronously.
        var heapSortM =  Mono
                // Use the fromCallable() factory method to obtain the
                // results of heap sorting the list.
                .fromCallable(() -> heapSort(list))

                // Use subscribeOn() to run all the processing in the
                // parallel thread pool.
                .subscribeOn(Schedulers.parallel());

        // Display the results as mixed fractions.
        Consumer<List<BigFraction>> displayList = sortedList -> {
            // Iterate through each BigFraction in the sorted list.
            sortedList.forEach(fraction ->
                    sb.append("\n     "
                            + fraction.toMixedString()));
            sb.append("\n");
            display(sb.toString());
        };

        return Mono
                // Use firstWithSignal() to select the result of whichever
                // sort finishes first and use it to print the sorted
                // list.
                .firstWithSignal(quickSortM,
                        heapSortM)

                // Use doOnSuccess() to display the first sorted list.
                .doOnSuccess(displayList)

                // Use then() to return an empty mono to synchronize with
                // the AsyncTaskBarrier framework.
                .then();
    }

    /**
     * Perform a quick sort on the {@code list}.
     */
    public static List<BigFraction> quickSort(List<BigFraction> list) {
        var copy = new ArrayList<>(list);

        // Order the list with quick sort.
        Collections.sort(copy);

        return copy;
    }

    /*
     * Perform a heap sort on the {@code list}.
     */
    public static List<BigFraction> heapSort(List<BigFraction> list) {
        var copy = new ArrayList<>(list);

        // Order the list with heap sort.
        HeapSort.sort(copy);

        return copy;
    }

}
