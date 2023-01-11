package berraquotes.client;

import berraquotes.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import berraquotes.utils.WebUtils;

import java.util.List;

import static berraquotes.Constants.EndPoint.*;
import static berraquotes.Constants.EndPoint.Params.QUOTE_IDS_PARAM;

/**
 * This class is a proxy to the {@code BerraApplication} microservice
 * and its {@code BerraController}.
 */
@Component
public class BerraQuotesProxy {
    /**
     * This field connects the {@link BerraQuotesProxy} to the {@link
     * RestTemplate} that performs HTTP requests synchronously.
     */
    @Autowired
    private RestTemplate mRestTemplate;


    /**
     * @return An {@link List} containing all {@link
     *         Quote} objects
     */
    public List<Quote> getAllQuotes() {
        // Create the encoded URL.
        var uri = UriComponentsBuilder
            // Create the path for the GET_ALL_QUOTES request,
            // including the 'service'.
            .fromPath(GET_ALL_QUOTES)

            // Build the URI.
            .build()

            // Convert the URI to a String.
            .toUriString();

        return WebUtils
            // Create and send a GET request to the server.
            .makeGetRequestList(mRestTemplate,
                                // Pass the encoded URL.
                                uri,
                                // Return type is a Quote array.
                                Quote[].class);
    }

    /**
     * Get a {@link List} that contains the requested quotes.
     *
     * @param quoteIds A {@link List} containing the given
     *                 {@code quoteIds}
     * @return An {@link List} containing the requested {@link
     *         Quote} objects
     */
    public List<Quote> getQuotes(List<Integer> quoteIds) {
        // Create the encoded URL.
        var uri = UriComponentsBuilder
            // Create the path for the GET_QUOTES request, including
            // the 'service'.
            .fromPath(GET_QUOTES)

            // Create the query param, which encodes the quote ids.
            .queryParam(QUOTE_IDS_PARAM,
                        WebUtils
                        // Convert List to String.
                        .list2String(quoteIds))

            // Build the URI.
            .build()

            // Convert the URI to a String.
            .toUriString();

        return WebUtils
            // Create and send a GET request to the server.
            .makeGetRequestList(mRestTemplate,
                                // Pass the encoded URL.
                                uri,
                                // Return type is a Quote array.
                                Quote[].class);
    }

    /**
     * Get a {@link List} that contains quotes that match the {@code query}.
     *
     * @param query A {@link String} to search for
     * @return An {@link List} containing matching {@link
     *         Quote} objects
     */
    public List<Quote> searchQuotes(String query) {
        var uri = UriComponentsBuilder
            .fromPath(GET_SEARCH
                      + "/"
                      + query)
            .build()
            .toUriString();

        return WebUtils
            // Create and send a GET request to the server.
            .makeGetRequestList(mRestTemplate,
                                uri,
                                // Return type is a Quote array.
                                Quote[].class);


    }
}
