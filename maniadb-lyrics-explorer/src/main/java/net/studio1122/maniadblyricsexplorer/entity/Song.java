package net.studio1122.maniadblyricsexplorer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(value = "song")
public class Song {

    @Id private String id;
    private String title;
    private String link;
    private String release;
    private String artist;
    private String lyric;
}
