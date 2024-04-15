package net.studio1122.lyricstool.entity;


import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("music")
public class Music {

    @Id private String id;
    @NonNull
    private String title;
    private String artist;
    private String genre;
    private String release;
    private String nation;
    private String lyric;
}
