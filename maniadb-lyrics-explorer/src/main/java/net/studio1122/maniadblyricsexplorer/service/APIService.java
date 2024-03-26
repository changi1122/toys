package net.studio1122.maniadblyricsexplorer.service;

import net.studio1122.maniadblyricsexplorer.entity.SearchResultItem;
import net.studio1122.maniadblyricsexplorer.utility.AlbumSearchSaxHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class APIService {

    public List<SearchResultItem> albumSearch(String query) throws Exception {
        if (query.isEmpty())
            return new ArrayList<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        String.format(
                                "https://www.maniadb.com/api/search/%s/?sr=album&display=20&key=example&v=0.5",
                                query
                        )))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        AlbumSearchSaxHandler handler = new AlbumSearchSaxHandler();

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();

        saxParser.parse(new InputSource(new ByteArrayInputStream(response.body().getBytes("UTF-8"))), handler);

        return handler.getItems();
    }
}
