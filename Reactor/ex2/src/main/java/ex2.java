import datamodels.TripRequest;
import datamodels.Flight;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.math.MathFlux;
import utils.AsyncTaskBarrier;
import utils.CheapestPriceCollector;
import utils.ExchangeRate;
import utils.TestDataFactory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This example demonstrates various reactive algorithms for finding
 * all the minimum values in an unordered list, which is surprisingly
 * not well documented in the literature.  These three algorithms
 * return a Flux that emits the cheapest flight(s) from a Flux of
 * available flights, which is part of an Airline Booking App that
 * we're creating for an upcoming MOOC on Reactive Microservices.
 * <p>
 * This example also shows how to use the AsyncTaskBarrier framework
 * and the zipWith() operation that converts flight prices to the
 * given currency after two asynchronous operations complete.
 */
public class ex2 {
    /**
     * The trip flight leg used for the tests.
     */
    private static final TripRequest sTrip = TripRequest
        .valueOf(LocalDateTime.parse("2025-01-01T07:00:00"),
                 LocalDateTime.parse("2025-02-01T19:00:00"),
                 "LHR",
                 "JFK",
                 "EUR",
                 1);

    /**
     * Maintains a mapping of currency conversions.
     */
    private static final ExchangeRate sExchangeRates =
        new ExchangeRate();

    /**
     * Main entry point into the test program.
     */
    public static void main(String[] argv) {
        // Print the cheapest flights via a two pass algorithm that
        // uses min() and filter().
        AsyncTaskBarrier.register(ex2::printCheapestFlightsMin);

        // Print the cheapest flights via a two-pass algorithm that
        // first calls sort() to order the trips by price and then
        // uses takeWhile() to return the cheapest flight(s).
        // AsyncTaskBarrier.register(ex2::printCheapestFlightsSorted);

        // Print the cheapest flights via a one-pass algorithm and a
        // custom Collector.
        // AsyncTaskBarrier.register(ex2::printCheapestFlightsOnepass);

        @SuppressWarnings("ConstantConditions")
        long testCount = AsyncTaskBarrier
            // Run all the tests.
            .runTasks()

            // Block until all the tests are done to allow future
            // computations to complete running.
            .block();

        // Print the results.
        System.out.println("Completed " + testCount + " tests");
    }

    /**
     * Convert the flight prices according to the relevant exchange rate.
     *
     * @param toCurrency The 3 letter currency to convert to
     * @return A {@link Flux} that emits flights with the correct prices
     */
    private static Flux<Flight> convertFlightPrices(String toCurrency) {
        // Asynchronous get all the exchange rates.
        Mono<ExchangeRate> exchangeRates = Mono
            .fromCallable(() -> sExchangeRates)
            // Run this computation in a thread pool.
            .subscribeOn(Schedulers.parallel());

        // Asynchronously get all the flights.
        Flux<Flight> flights = TestDataFactory
            // Get all the flights.
            .findFlights(sTrip)
            // Run this computation in a thread pool.
            .subscribeOn(Schedulers.parallel());

        // Return a Flux that emits flights with the correct prices.
        return exchangeRates
            // Wait for both the flights and the exchange rates
            // to complete.
            .flatMapMany(rates -> flights.map(flight ->
                         // Convert the flight prices to the appropriate
                         // currency.
                         convertCurrency(toCurrency,
                                         flight,
                                         rates)));
    }

    /**
     * Convert from {@code flight.getCurrency()} to {@code toCurrency} and
     * return an updated {@code flight}.
     *
     * @param toCurrency Current to convert to
     * @param flight     Flight containing the price in the {@code flight.getCurrency()} format
     * @param rates      The exchange rates
     * @return An updated flight whose price reflects the exchange rate conversion
     */
    private static Flight convertCurrency(String toCurrency,
                                          Flight flight,
                                          ExchangeRate rates) {
        // Only convert the currency if necessary.
        if (!flight.getCurrency().equals(toCurrency)) {
            // Update the price by multiplying it by the
            // currency conversion rate.
            flight.setPrice(flight.getPrice()
                            * rates.getRates(flight.getCurrency()).get(toCurrency));
        }
        // Return the flight (which may or may not be updated).
        return flight;
    }

    /**
     * Print the cheapest flights via a two pass algorithm that uses
     * min() and filter().
     */
    private static Mono<Void> printCheapestFlightsMin() {
        Flux<Flight> flights = ex2
            // Convert the flights into the requested currency.
            .convertFlightPrices(sTrip.getCurrency());

        // Find the cheapest flights.
        Flux<Flight> lowestPrices = MathFlux
            // Find the cheapest flight.
            .min(flights,
                 Comparator.comparing(Flight::getPrice))
            // Create a Flux that contains the cheapest flights.
            .flatMapMany(min -> flights
                // Only allow flights that match the cheapest.
                .filter(tr -> tr.getPrice().equals(min.getPrice())));

        // Print the cheapest flights.
        return lowestPrices
            .doOnNext(flight -> System.out.println("printCheapestFlightsMin() = " + flight))
            .then();
    }

    /**
     * Print the cheapest flights via a two-pass algorithm that first
     * calls sort() to order the trips by price and then uses
     * takeWhile() to return the cheapest flight(s).
     */
    private static Mono<Void> printCheapestFlightsSorted() {
        Flux<Flight> sortedFlights = ex2
            // Convert the flights into the requested currency.
            .convertFlightPrices(sTrip.getCurrency())

            // Sort the flights from lowest to highest price.
            .sort(Comparator.comparing(Flight::getPrice));

        // Create a flux containing the cheapest prices.
        Flux<Flight> lowestPrices = sortedFlights
            // Get the cheapest price.
            .elementAt(0)

            // Take all the elements that match the cheapest price.
            .flatMapMany(min -> sortedFlights
                .takeWhile(tr -> tr.getPrice().equals(min.getPrice())));

        // Print the cheapest flights.
        return lowestPrices
            .doOnNext(flight ->
                          System.out.println("printCheapestFlightsSorted() = "
                                                 + flight))
            .then();
    }

    /**
     * Print the cheapest flights via a one-pass algorithm and a
     * custom Collector.
     */
    private static Mono<Void> printCheapestFlightsOnepass() {

        Flux<Flight> lowestPrices = ex2
            // Convert the flights into the requested currency.
            .convertFlightPrices(sTrip.getCurrency())

            // Converts a stream of TripResponses into a Flux that
            // emits the cheapest priced trips(s).
            .collect(CheapestPriceCollector.toFlux())

            // Convert the Mono into a Flux that emits the cheapest
            // priced trip(s).
            .flatMapMany(Function.identity());

        // Print the cheapest flights.
        return lowestPrices
            .doOnNext(flight ->
                          System.out.println("printCheapestFlightsOnepass() = "
                                                 + flight))
            .then();
    }
}
