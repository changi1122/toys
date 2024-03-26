package net.studio1122.maniadblyricsexplorer.service;

import net.studio1122.maniadblyricsexplorer.entity.Album;
import net.studio1122.maniadblyricsexplorer.entity.SearchResultItem;
import net.studio1122.maniadblyricsexplorer.entity.Song;
import net.studio1122.maniadblyricsexplorer.utility.AlbumSaxHandler;
import net.studio1122.maniadblyricsexplorer.utility.AlbumSearchSaxHandler;
import net.studio1122.maniadblyricsexplorer.utility.EncodingUtil;
import net.studio1122.maniadblyricsexplorer.utility.Pair;
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
                                EncodingUtil.encodeURIComponent(query)
                        )))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        AlbumSearchSaxHandler handler = new AlbumSearchSaxHandler();

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();

        saxParser.parse(new InputSource(new ByteArrayInputStream(response.body().getBytes("UTF-8"))), handler);

        return handler.getItems();
    }

    public Pair<Album, List<Song>> album(String id) throws Exception {
        if (id.isEmpty())
            return null;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        String.format(
                                "https://www.maniadb.com/api/album/%s/?key=example&v=0.5",
                                id
                        )))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        AlbumSaxHandler handler = new AlbumSaxHandler();

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();

        saxParser.parse(new InputSource(new ByteArrayInputStream(response.body().getBytes("UTF-8"))), handler);

        return new Pair<>(handler.getAlbum(), handler.getSongs());
    }
}
