package net.studio1122.maniadblyricsexplorer.repository;

import net.studio1122.maniadblyricsexplorer.entity.Song;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongRepository extends MongoRepository<Song, String> {


}
