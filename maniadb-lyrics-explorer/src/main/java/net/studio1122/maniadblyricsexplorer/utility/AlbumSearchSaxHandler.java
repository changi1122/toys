package net.studio1122.maniadblyricsexplorer.utility;

import lombok.Getter;
import net.studio1122.maniadblyricsexplorer.entity.SearchResultItem;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class AlbumSearchSaxHandler extends DefaultHandler {

    @Getter
    List<SearchResultItem> items = new ArrayList<>();
    SearchResultItem item = new SearchResultItem();
    private String innerText;

    // 시작 태그를 만나면 호출
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("item")) {
            item = new SearchResultItem();
            item.setId(attributes.getValue("id"));
        }
    }

    // 종료 태그를 만나면 호출
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "title": item.setTitle(innerText); break;
            case "item": items.add(item); break;
        }
    }

    // 태그 사이 텍스트 처리
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.innerText = new String(ch, start, length);
    }

}
