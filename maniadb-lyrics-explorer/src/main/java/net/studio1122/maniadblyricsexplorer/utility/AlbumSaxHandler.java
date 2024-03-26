package net.studio1122.maniadblyricsexplorer.utility;

import lombok.Getter;
import net.studio1122.maniadblyricsexplorer.entity.Album;
import net.studio1122.maniadblyricsexplorer.entity.SearchResultItem;
import net.studio1122.maniadblyricsexplorer.entity.Song;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class AlbumSaxHandler extends DefaultHandler {

    @Getter
    Album album = new Album();
    @Getter
    List<Song> songs = new ArrayList<>();
    Song song = new Song();

    boolean isItemStarted = false;
    boolean isManiadbArtistStarted = false;
    boolean isManiadbTracksStarted = false;

    private String innerText;

    // 시작 태그를 만나면 호출
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("item")) {
            isItemStarted = true;
            album.setId(attributes.getValue("id"));
        }
        if (qName.equals("maniadb:artist")) {
            isManiadbArtistStarted = true;
        }
        if (qName.equals("maniadb:tracks")) {
            isManiadbTracksStarted = true;
        }
        if (qName.equals("song")) {
            song = new Song();
        }
    }

    // 종료 태그를 만나면 호출
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "title":
                if (isItemStarted && album.getTitle() == null)
                    album.setTitle(innerText);
                if (isManiadbTracksStarted)
                    song.setTitle(innerText);
                break;
            case "link":
                album.setLink(innerText);
                break;
            case "releasedate":
                if (isItemStarted && album.getRelease() == null)
                    album.setRelease(innerText);
                break;
            case "name":
                if (isManiadbArtistStarted && album.getArtist() == null)
                    album.setArtist(innerText);
                break;
            case "lyric":
                if (isManiadbTracksStarted)
                    song.setLyric(innerText);
                break;
            case "song":
                songs.add(song);
                break;
        }
    }

    // 태그 사이 텍스트 처리
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.innerText = new String(ch, start, length);
    }

}
